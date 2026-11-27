/**
 * Siembra los emuladores de Firebase con un estado de partida reproducible (spec 010).
 *
 * El objetivo es que la app se pueda recorrer entera sin tocar ningún proyecto real: dos cuentas,
 * tres vehículos, y uno de ellos **compartido entre las dos** — que es el caso que distingue a
 * AparK y el que más cuesta montar a mano. Y unas incidencias (spec 019): abiertas, resueltas, y
 * una de alguien que ya no es miembro.
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
import { getFirestore, Timestamp, type Firestore } from "firebase-admin/firestore";

const PROJECT = "apark-dev";
const USERS = "users";
const VEHICLES = "vehicles";
const INCIDENTS = "incidents";

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

/**
 * Los datos de la spec 015, como códigos de `docs/DATA-MODEL.md` §9. `""` es «sin dato», que es lo que
 * tiene cualquier vehículo creado antes de esa spec: la app nunca escribió `model` ni `color`.
 */
export interface SeedVehicle {
    id: string;
    name: string;
    licensePlate: string;
    type: string;
    powertrain: string;
    fuel: string;
    make: string;
    model: string;
    color: string;
    emissionsLabel: string;
    ownerId: string;
    memberIds: string[];
    lastLocation: {
        latitude: number;
        longitude: number;
        timestamp: number;
        user: { uid: string; name: string; email: string } | null;
        /** Se aparcó con el botón de su ubicación predeterminada (spec 024). */
        atDefaultPlace?: boolean;
    } | null;
    /** Su ubicación predeterminada (spec 024): el garaje. */
    defaultPlace?: { name: string; latitude: number; longitude: number };
    createdAt: number;
    updatedAt: number;
}

export function buildVehicles(): SeedVehicle[] {
    return [
        {
            // Sin aparcar: es el estado en el que nace un vehículo y el que enseña el caso vacío.
            // Con todos los datos, para ver la ficha completa.
            id: "seed-vehicle-sin-aparcar",
            name: "Furgoneta",
            licensePlate: "1234 ABC",
            type: "van",
            powertrain: "combustion",
            fuel: "diesel",
            make: "Volkswagen",
            model: "Transporter",
            color: "white",
            emissionsLabel: "c",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid],
            lastLocation: null,
            createdAt: T0,
            updatedAt: T0,
        },
        {
            // Aparcado en Madrid, por su dueño, y en su garaje (spec 024): enseña el pin con la casa
            // y «Garaje · …» en la tarjeta. Sin datos, como cualquiera de antes de la spec 015: su
            // dueño ve «Añade los datos del vehículo».
            id: "seed-vehicle-aparcado",
            name: "Coche de Alberto",
            licensePlate: "5678 DEF",
            type: "",
            powertrain: "",
            fuel: "",
            make: "",
            model: "",
            color: "",
            emissionsLabel: "",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid],
            lastLocation: {
                latitude: 40.4168,
                longitude: -3.7038,
                timestamp: T0,
                user: { uid: ALBERTO.uid, name: ALBERTO.name, email: ALBERTO.email },
                atDefaultPlace: true,
            },
            defaultPlace: { name: "Garaje", latitude: 40.4168, longitude: -3.7038 },
            createdAt: T0,
            updatedAt: T0,
        },
        {
            // El interesante: dos miembros, y lo aparcó el que NO es el dueño. Tiene una plaza de
            // garaje cerca, pero no está en ella: enseña los dos botones (spec 024).
            id: "seed-vehicle-compartido",
            name: "Coche compartido",
            licensePlate: "9012 GHI",
            type: "car",
            powertrain: "hybrid",
            fuel: "petrol",
            make: "Seat",
            model: "Ibiza",
            color: "red",
            emissionsLabel: "eco",
            ownerId: ALBERTO.uid,
            memberIds: [ALBERTO.uid, EDU.uid],
            lastLocation: {
                latitude: 40.4155,
                longitude: -3.7074,
                timestamp: T0,
                user: { uid: EDU.uid, name: EDU.name, email: EDU.email },
            },
            defaultPlace: { name: "Plaza 23", latitude: 40.4149, longitude: -3.7065 },
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
/**
 * Las incidencias de la spec 019, en `vehicles/{id}/incidents`.
 *
 * **Sus fechas van respecto a hoy, no a `T0`**: las resueltas se ven 30 días, y con fechas fijas
 * estarían todas ocultas. Así que dos siembras dan las mismas incidencias, pero no las mismas horas.
 */
export interface SeedIncident {
    vehicleId: string;
    id: string;
    /** Un tipo de la lista, o `custom`. */
    type: string;
    /** El nombre de una personalizada; `""` en los demás tipos, como exigen las reglas. */
    title: string;
    note: string;
    reportedBy: string;
    /** Hace cuánto se reportó. */
    reportedAgo: number;
    /** Hace cuánto se resolvió, y quién; sin esto, está abierta. */
    resolved?: { by: string; ago: number };
}

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

/** Alguien que fue miembro y ya no lo es: su incidencia sale sin nombre (criterio 2). */
const FORMER_MEMBER = "seed-antiguo-miembro";

/** La misma lista que `firestore.rules`: la siembra no debe poder saltársela sin que se note. */
const INCIDENT_TYPES = [
    "low_fuel", "low_charge", "oil", "washer_fluid", "adblue",
    "warning_light", "wont_start", "breakdown", "strange_noise", "bulb_out",
    "puncture", "low_pressure", "tyre_wear",
    "damage", "cleaning", "items_inside",
    "custom",
];

export function buildIncidents(): SeedIncident[] {
    const shared = "seed-vehicle-compartido";
    return [
        { vehicleId: shared, id: "seed-incident-gasolina", type: "low_fuel", title: "", note: "Queda para unos 50 km", reportedBy: EDU.uid, reportedAgo: 2 * HOUR },
        { vehicleId: shared, id: "seed-incident-golpe", type: "damage", title: "", note: "Rayón en la puerta trasera derecha", reportedBy: ALBERTO.uid, reportedAgo: 3 * DAY },
        { vehicleId: shared, id: "seed-incident-antiguo", type: "strange_noise", title: "", note: "Chirría al frenar", reportedBy: FORMER_MEMBER, reportedAgo: 12 * DAY },
        // Una personalizada: lo que no encaja en ningún tipo lleva su propio nombre.
        { vehicleId: shared, id: "seed-incident-silla", type: "custom", title: "Silla del bebé desmontada", note: "Está en el maletero", reportedBy: EDU.uid, reportedAgo: 5 * HOUR },
        { vehicleId: shared, id: "seed-incident-presion", type: "low_pressure", title: "", note: "", reportedBy: ALBERTO.uid, reportedAgo: 6 * DAY, resolved: { by: EDU.uid, ago: 5 * DAY } },
        // Resuelta hace más de 30 días: la app ya no la enseña, y en un proyecto real la caducidad
        // de Firestore la habría borrado (criterio 10). El emulador no tiene caducidad.
        { vehicleId: shared, id: "seed-incident-limpieza", type: "cleaning", title: "", note: "", reportedBy: EDU.uid, reportedAgo: 45 * DAY, resolved: { by: ALBERTO.uid, ago: 40 * DAY } },
        { vehicleId: "seed-vehicle-sin-aparcar", id: "seed-incident-adblue", type: "adblue", title: "", note: "Se ha encendido el aviso", reportedBy: ALBERTO.uid, reportedAgo: 1 * DAY },
    ];
}

function incidentDocument(i: SeedIncident, now: number): Record<string, unknown> {
    const document: Record<string, unknown> = {
        type: i.type,
        title: i.title,
        note: i.note,
        status: i.resolved ? "resolved" : "open",
        reportedBy: i.reportedBy,
        createdAt: Timestamp.fromMillis(now - i.reportedAgo),
    };
    if (i.resolved) {
        const resolvedAt = now - i.resolved.ago;
        document.resolvedBy = i.resolved.by;
        document.resolvedAt = Timestamp.fromMillis(resolvedAt);
        document.expiresAt = Timestamp.fromMillis(resolvedAt + 30 * DAY);
    }
    return document;
}

export function assertInvariants(vehicles: SeedVehicle[], users: SeedUser[], incidents: SeedIncident[] = []): void {
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
        const place = v.defaultPlace;
        if (place) {
            const name = place.name.trim();
            if (name === "" || place.name.length > 40) throw new Error(`${v.id}: la ubicación necesita un nombre de 1 a 40`);
            if (Math.abs(place.latitude) > 90 || Math.abs(place.longitude) > 180) {
                throw new Error(`${v.id}: la ubicación está fuera del mapa`);
            }
        }
        if (v.lastLocation?.atDefaultPlace && !place) {
            throw new Error(`${v.id}: está aparcado en su ubicación, pero no tiene`);
        }
    }

    for (const i of incidents) {
        const vehicle = vehicles.find((v) => v.id === i.vehicleId);
        if (!vehicle) throw new Error(`${i.id}: es de ${i.vehicleId}, que no existe`);
        if (!INCIDENT_TYPES.includes(i.type)) throw new Error(`${i.id}: el tipo ${i.type} no es de la lista`);
        const custom = i.type === "custom";
        if (custom && (i.title.trim() === "" || i.title.length > 40)) {
            throw new Error(`${i.id}: una personalizada necesita un nombre de 1 a 40`);
        }
        if (!custom && i.title !== "") throw new Error(`${i.id}: solo las personalizadas llevan nombre`);
        if (i.note.length > 280) throw new Error(`${i.id}: la nota pasa de 280`);
        // Quien la reportó puede haberse ido después; quien la resuelve tiene que estar.
        if (i.resolved && !vehicle.memberIds.includes(i.resolved.by)) {
            throw new Error(`${i.id}: la resolvió ${i.resolved.by}, que no es miembro`);
        }
        if (i.resolved && i.resolved.ago > i.reportedAgo) throw new Error(`${i.id}: se resolvió antes de reportarse`);
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

/**
 * Deja la colección como si fuera la primera vez: sembrar dos veces tiene que dar lo mismo. Con sus
 * subcolecciones, o las incidencias de antes se quedarían colgando de vehículos que ya no existen.
 */
async function wipe(db: Firestore, collection: string): Promise<void> {
    await db.recursiveDelete(db.collection(collection));
}

/**
 * Apaga o enciende los disparadores de Functions en el emulador, a través de su Hub.
 *
 * Sembrar borra los vehículos y los vuelve a crear **con el mismo id**. Cada borrado dispara
 * `cleanupVehicleReferences`, que corre en asíncrono y llega **después** de haberlos recreado:
 * - se llevaba las incidencias recién sembradas;
 * - y quitaba de `userVehicles` los ids que la siembra acababa de escribir.
 *
 * En producción un id no se reutiliza nunca, así que es una carrera solo de la siembra. Sin el
 * emulador de Functions arrancado no hay nada que apagar.
 */
async function setBackgroundTriggers(enabled: boolean): Promise<void> {
    const hub = process.env.FIREBASE_EMULATOR_HUB ?? "127.0.0.1:4400";
    const action = enabled ? "enableBackgroundTriggers" : "disableBackgroundTriggers";
    try {
        const response = await fetch(`http://${hub}/functions/${action}`, { method: "PUT" });
        if (!response.ok) console.warn(`El Hub respondió ${response.status} a ${action}`);
    } catch {
        console.warn(`No se pudo llamar a ${action}: ¿está arrancado el emulador de Functions?`);
    }
}

export async function seed(): Promise<void> {
    await setBackgroundTriggers(false);
    try {
        await seedData();
    } finally {
        await setBackgroundTriggers(true);
    }
}

async function seedData(): Promise<void> {
    const { db, auth } = connect();
    const users = [ALBERTO, EDU];
    const vehicles = buildVehicles();
    const incidents = buildIncidents();

    // Antes de tocar nada: si el estado que vamos a sembrar es imposible, mejor no sembrarlo.
    assertInvariants(vehicles, users, incidents);

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

    const now = Date.now();
    for (const i of incidents) {
        await db.collection(VEHICLES).doc(i.vehicleId).collection(INCIDENTS).doc(i.id).set(incidentDocument(i, now));
    }

    console.log(`\nSembrado: ${users.length} usuarios, ${vehicles.length} vehículos, ${incidents.length} incidencias.`);
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
