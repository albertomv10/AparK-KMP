/**
 * Migración de la spec 008: la pertenencia como consulta.
 *
 * Dos operaciones, en dos momentos distintos del despliegue:
 *
 *   1. **Backfill** (por defecto): escribe `memberIds` en los vehículos que no lo tengan, a partir
 *      de `ownerId` + `sharedUsers`. Es aditivo y no toca nada más, así que ningún cliente se
 *      entera. Va ANTES de desplegar las reglas nuevas.
 *
 *   2. **Limpieza** (`--drop-shared-users`): borra `sharedUsers`, que a esas alturas ya no lo lee
 *      nadie. Va DESPUÉS de actualizar las apps: mientras queden reglas viejas desplegadas en algún
 *      sitio, ese campo sigue dando acceso de lectura y borrarlo echaría fuera a los compartidos.
 *
 * Vive en el repositorio y no como Cloud Function a propósito: se ejecuta un puñado de veces y no
 * tiene por qué vivir para siempre. Pero sí tiene que quedar escrito qué se hizo — es la lección de
 * la política TTL, que no está versionada en ningún sitio.
 *
 * Uso:
 *   npm run migrate -- --project apark-dev --dry-run
 *   npm run migrate -- --project apark-dev
 *   npm run migrate -- --project apark-dev --drop-shared-users
 */
import { pathToFileURL } from "node:url";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, type Firestore } from "firebase-admin/firestore";

const VEHICLES = "vehicles";
/** Un batch de Firestore admite 500 escrituras. */
const BATCH_LIMIT = 400;

export interface Options {
    project: string;
    dryRun: boolean;
    dropSharedUsers: boolean;
}

export function parseArgs(argv: string[]): Options {
    const project = argv[argv.indexOf("--project") + 1];

    // Exigir el proyecto por nombre, siempre. Sin esto, el valor por defecto del entorno decide
    // contra qué base se escribe, y el dia que ese defecto sea produccion nadie se entera.
    if (!argv.includes("--project") || !project || project.startsWith("--")) {
        throw new Error("Falta --project <id>. Es obligatorio: sin él no se sabe contra qué se escribe.");
    }

    return {
        project,
        dryRun: argv.includes("--dry-run"),
        dropSharedUsers: argv.includes("--drop-shared-users"),
    };
}

function connect(project: string): Firestore {
    // Contra el emulador no hacen falta credenciales, que es lo que permite probar este script de
    // verdad sin crear una cuenta de servicio — la credencial más peligrosa que puede tener el
    // proyecto, porque se salta todas las reglas.
    const emulator = process.env.FIRESTORE_EMULATOR_HOST;
    initializeApp(
        emulator ? { projectId: project } : { credential: applicationDefault(), projectId: project }
    );
    console.log(emulator ? `Emulador en ${emulator}` : "Proyecto real (credenciales por defecto)");
    return getFirestore();
}

/** `memberIds` son todos los miembros, dueño incluido. Un uid podría repetirse; el Set lo evita. */
export function membersOf(data: FirebaseFirestore.DocumentData): string[] {
    const shared: string[] = Array.isArray(data.sharedUsers) ? data.sharedUsers : [];
    const owner: string = typeof data.ownerId === "string" ? data.ownerId : "";
    return [...new Set([owner, ...shared])].filter((id) => id.length > 0);
}

export async function migrate(options: Options, existing?: Firestore): Promise<void> {
    const db = existing ?? connect(options.project);
    const snapshot = await db.collection(VEHICLES).get();

    console.log(`\n${snapshot.size} vehículo(s) en ${options.project}\n`);

    const pending: { id: string; updates: FirebaseFirestore.UpdateData<unknown> }[] = [];
    let alreadyDone = 0;
    let skipped = 0;

    for (const doc of snapshot.docs) {
        const data = doc.data();
        const updates: Record<string, unknown> = {};

        const hasMembers = Array.isArray(data.memberIds) && data.memberIds.length > 0;

        if (typeof data.ownerId !== "string" || data.ownerId.length === 0) {
            // Corrupcion preexistente: un vehiculo sin dueño no se puede borrar (la regla de
            // delete compara con ownerId). El script no lo empeora, pero tiene que verse.
            console.warn(`  ! ${doc.id} no tiene ownerId`);
        }

        if (!hasMembers) {
            const members = membersOf(data);
            if (members.length === 0) {
                // Un vehículo sin dueño no deberia existir. Dejarlo intacto y avisar es mejor que
                // escribirle un array vacio, que lo haria invisible para todo el mundo.
                console.warn(`  ! ${doc.id} no tiene ownerId ni sharedUsers; se deja intacto`);
                skipped++;
                continue;
            }
            updates.memberIds = members;
        } else {
            alreadyDone++;
        }

        if (options.dropSharedUsers && data.sharedUsers !== undefined) {
            // Seguro aqui: los documentos de los que no se pudo derivar pertenencia ya salieron
            // por el `continue` de arriba, asi que a estas alturas el documento tiene memberIds,
            // o los va a tener en esta misma escritura. Nadie se queda sin acceso.
            updates.sharedUsers = FieldValue.delete();
        }

        if (Object.keys(updates).length > 0) {
            pending.push({ id: doc.id, updates });
        }
    }

    for (const { id, updates } of pending) {
        const what = Object.entries(updates)
            .map(([k, v]) => (k === "sharedUsers" ? "-sharedUsers" : `+${k}=[${(v as string[]).join(", ")}]`))
            .join("  ");
        console.log(`  ${options.dryRun ? "(simulado)" : "escribir "} ${id}  ${what}`);
    }

    if (!options.dryRun && pending.length > 0) {
        for (let i = 0; i < pending.length; i += BATCH_LIMIT) {
            const batch = db.batch();
            for (const { id, updates } of pending.slice(i, i + BATCH_LIMIT)) {
                batch.update(db.collection(VEHICLES).doc(id), updates);
            }
            await batch.commit();
        }
    }

    console.log(
        `\n${options.dryRun ? "Simulacion" : "Hecho"}: ${pending.length} por cambiar, ` +
            `${alreadyDone} ya estaban, ${skipped} omitido(s).`
    );
    if (skipped > 0) process.exitCode = 1;
}

// Solo se ejecuta al invocarlo como comando. Importarlo desde un test no debe disparar nada.
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    migrate(parseArgs(process.argv.slice(2))).catch((error) => {
        console.error(`\nError: ${error instanceof Error ? error.message : String(error)}`);
        process.exit(1);
    });
}
