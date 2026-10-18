/**
 * Siembra los emuladores de Firebase con un estado de partida reproducible (spec 010).
 *
 * El objetivo es que la app se pueda recorrer entera sin tocar ningún proyecto real: dos cuentas,
 * tres vehículos, y uno de ellos **compartido entre las dos** — que es el caso que distingue a
 * AparK y el que más cuesta montar a mano.
 *
 * Se escribe con el Admin SDK, que **se salta las reglas**. Eso es cómodo y es un riesgo: podría
 * sembrar estados que `firestore.rules` jamás permitiría, y entonces lo que se pruebe encima no se
 * parecería a producción. Por eso el script termina comprobando sus propios invariantes y aborta
 * si no se cumplen — ver `assertInvariants`.
 *
 * Frente a `--import/--export` del propio emulador: un volcado binario no lo revisa nadie y se
 * pudre en silencio cuando cambia el esquema. Esto se lee, se versiona y falla a la vista.
 *
 * Uso (con los emuladores ya levantados):
 *   npm run seed
 */
import { pathToFileURL } from "node:url";
import { initializeApp } from "firebase-admin/app";
import { getAuth, type Auth } from "firebase-admin/auth";
import { getFirestore, type Firestore } from "firebase-admin/firestore";

const PROJECT = "apark-dev";
const USERS = "users";
const VEHICLES = "vehicles";

/** La contraseña es la misma para los dos: esto no sale de tu máquina ni toca ninguna cuenta real. */
const PASSWORD = "Emulador1234";

export interface SeedUser {
    uid: string;
    email: string;
    name: string;
}

export const ALBERTO: SeedUser = { uid: "seed-alberto", email: "alberto@emulador.test", name: "Alberto" };
export const EDU: SeedUser = { uid: "seed-edu", email: "edu@emulador.test", name: "Edu" };

/** Fijo, no `Date.now()`: dos siembras seguidas tienen que dar exactamente lo mismo. */
const T0 = Date.UTC(2026, 0, 1, 12, 0, 0);

export interface SeedVehicle {
    id: string;
    name: string;
    model: string;
    licensePlate: string;
    color: string;
    ownerId: string;
    memberIds: string[];
    lastLocation: {
        latitude: number;
        longitude: number;
        timestamp: number;
        user: { uid: string; name: string; email: string } | null;
    } | null;
    createdAt: number;
    updatedAt: number;
}

export function buildVehicles(): SeedVehicle[] {
    return [
        {
            // Sin aparcar: es el estado en el que nace un vehículo y el que enseña el caso vacío.
            id: "seed-vehicle-sin-aparcar",
            name: "Furgoneta",
            model: "Transporter",
            licensePlate: "1234 ABC",
            color: "#3F51B5",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid],
            lastLocation: null,
            createdAt: T0,
            updatedAt: T0,
        },
        {
            // Aparcado en Madrid, por su dueño.
            id: "seed-vehicle-aparcado",
            name: "Coche de Alberto",
            model: "Civic",
            licensePlate: "5678 DEF",
            color: "#E91E63",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid],
            lastLocation: {
                latitude: 40.4168,
                longitude: -3.7038,
                timestamp: T0,
                user: { uid: ALBERTO.uid, name: ALBERTO.name, email: ALBERTO.email },
            },
            createdAt: T0,
            updatedAt: T0,
        },
        {
            // El interesante: dos miembros, y lo aparcó el que NO es el dueño.
            id: "seed-vehicle-compartido",
            name: "Coche compartido",
            model: "Ibiza",
            licensePlate: "9012 GHI",
            color: "#009688",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid, EDU.uid],
            lastLocation: {
                latitude: 40.4155,
                longitude: -3.7074,
                timestamp: T0,
                user: { uid: EDU.uid, name: EDU.name, email: EDU.email },
            },
            createdAt: T0,
            updatedAt: T0,
        },
    ];
}

/**
 * Lo que `firestore.rules` da por cierto y el Admin SDK no obliga a cumplir.
 *
 * No pretende reimplementar las reglas —para eso están los tests de `tools/test/`— sino evitar
 * sembrar un estado imposible, que es el error que haría que todo lo probado encima no valiese.
 */
export function assertInvariants(vehicles: SeedVehicle[], users: SeedUser[]): void {
    const known = new Set(users.map((u) => u.uid));

    for (const v of vehicles) {
        if (!v.memberIds.includes(v.ownerId)) {
            throw new Error(`${v.id}: el dueño tiene que estar en memberIds, o no podría leer su propio vehículo`);
        }
        if (new Set(v.memberIds).size !== v.memberIds.length) {
            throw new Error(`${v.id}: memberIds tiene repetidos`);
        }
        for (const uid of v.memberIds) {
            if (!known.has(uid)) throw new Error(`${v.id}: el miembro ${uid} no es ninguno de los usuarios sembrados`);
        }
        // La regla de aparcar exige que el email de quien aparca sea el del token, así que un
        // `lastLocation` cuyo autor no sea miembro describe algo que no pudo pasar.
        const author = v.lastLocation?.user;
        if (author && !v.memberIds.includes(author.uid)) {
            throw new Error(`${v.id}: lo aparcó ${author.uid}, que no es miembro`);
        }
        if (v.name.length === 0 || v.name.length > 60) {
            throw new Error(`${v.id}: el nombre no cumple lo que exige la regla de creación`);
        }
    }
}

function connect(): { db: Firestore; auth: Auth } {
    // Sin estas dos, el Admin SDK iría al proyecto de verdad y pediría credenciales. Exigirlas es
    // lo que hace imposible sembrar `apark-dev` por accidente.
    const firestore = process.env.FIRESTORE_EMULATOR_HOST;
    const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
    if (!firestore || !authHost) {
        throw new Error(
            "Faltan FIRESTORE_EMULATOR_HOST y/o FIREBASE_AUTH_EMULATOR_HOST. Este script solo siembra " +
                "emuladores: sin ellas escribiría contra el proyecto real. Levántalos primero."
        );
    }

    initializeApp({ projectId: PROJECT });
    console.log(`Emuladores: Firestore en ${firestore}, Auth en ${authHost}`);
    return { db: getFirestore(), auth: getAuth() };
}

/** Deja la colección como si fuera la primera vez: sembrar dos veces tiene que dar lo mismo. */
async function wipe(db: Firestore, collection: string): Promise<void> {
    const snapshot = await db.collection(collection).get();
    await Promise.all(snapshot.docs.map((d) => d.ref.delete()));
}

export async function seed(): Promise<void> {
    const { db, auth } = connect();
    const users = [ALBERTO, EDU];
    const vehicles = buildVehicles();

    // Antes de tocar nada: si el estado que vamos a sembrar es imposible, mejor no sembrarlo.
    assertInvariants(vehicles, users);

    await Promise.all([wipe(db, USERS), wipe(db, VEHICLES)]);
    const existing = await auth.listUsers();
    await Promise.all(existing.users.map((u) => auth.deleteUser(u.uid)));

    for (const u of users) {
        await auth.createUser({ uid: u.uid, email: u.email, password: PASSWORD, displayName: u.name, emailVerified: true });
        await db.collection(USERS).doc(u.uid).set({
            id: u.uid,
            email: u.email,
            name: u.name,
            // `userVehicles` es solo la pista de ordenación del carrusel (spec 008).
            userVehicles: vehicles.filter((v) => v.memberIds.includes(u.uid)).map((v) => v.id),
            createdAt: T0,
            updatedAt: T0,
        });
    }

    for (const v of vehicles) {
        const { id, ...data } = v;
        await db.collection(VEHICLES).doc(id).set(data);
    }

    console.log(`\nSembrado: ${users.length} usuarios, ${vehicles.length} vehículos.`);
    console.log(`Entra con cualquiera de estos y la contraseña ${PASSWORD}:`);
    for (const u of users) console.log(`  · ${u.email}`);
    console.log("\nEl email ya está verificado, así que la app va directa al mapa.");
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
    seed().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exit(1);
    });
}
