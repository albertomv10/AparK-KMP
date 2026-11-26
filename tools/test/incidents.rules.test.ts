/**
 * Tests de las reglas de `vehicles/{id}/incidents` (spec 019).
 *
 * Igual que los de `vehicles`: importa tanto lo que se permite como lo que se deniega. Las fechas
 * van con `serverTimestamp()`, que es lo que manda la app, porque las reglas exigen la hora del
 * servidor.
 */
import { readFileSync } from "node:fs";
import {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment,
    type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import {
    collection,
    deleteDoc,
    deleteField,
    doc,
    getDoc,
    getDocs,
    serverTimestamp,
    setDoc,
    Timestamp,
    updateDoc,
} from "firebase/firestore";

const OWNER = "uid_owner";
const MEMBER = "uid_member";
const OTHER_MEMBER = "uid_other_member";
const STRANGER = "uid_stranger";
const VEHICLE = "vehicle_1";
const INCIDENT = "incident_1";

let env: RulesTestEnvironment;

beforeAll(async () => {
    env = await initializeTestEnvironment({
        // Un proyecto propio: vitest ejecuta los ficheros en paralelo, y con el mismo proyecto el
        // `clearFirestore` de uno borraba los datos de los otros a mitad de sus tests.
        projectId: "apark-rules-incidents-test",
        firestore: { rules: readFileSync("../firestore.rules", "utf8") },
    });
});

afterAll(async () => env?.cleanup());

beforeEach(async () => {
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, "vehicles", VEHICLE), {
            name: "Coche",
            ownerId: OWNER,
            memberIds: [OWNER, MEMBER, OTHER_MEMBER],
            lastLocation: null,
        });
        // Una incidencia abierta, reportada por MEMBER.
        await setDoc(doc(db, "vehicles", VEHICLE, "incidents", INCIDENT), {
            type: "low_fuel",
            title: "",
            note: "",
            status: "open",
            reportedBy: MEMBER,
            createdAt: Timestamp.now(),
        });
    });
});

const db = (uid: string) => env.authenticatedContext(uid, { email_verified: true }).firestore();
const incidents = (uid: string) => collection(db(uid), "vehicles", VEHICLE, "incidents");
const incident = (uid: string, id = INCIDENT) => doc(db(uid), "vehicles", VEHICLE, "incidents", id);

const report = (uid: string, extra: Record<string, unknown> = {}) =>
    setDoc(doc(incidents(uid)), {
        type: "cleaning",
        title: "",
        note: "Hay arena en los asientos",
        status: "open",
        reportedBy: uid,
        createdAt: serverTimestamp(),
        ...extra,
    });

const inDays = (days: number) => Timestamp.fromMillis(Date.now() + days * 24 * 60 * 60 * 1000);

const resolve = (uid: string, extra: Record<string, unknown> = {}) =>
    updateDoc(incident(uid), {
        status: "resolved",
        resolvedBy: uid,
        resolvedAt: serverTimestamp(),
        expiresAt: inDays(30),
        ...extra,
    });

describe("leer", () => {
    it("un miembro lee las incidencias de su vehículo", async () => {
        await assertSucceeds(getDocs(incidents(MEMBER)));
        await assertSucceeds(getDoc(incident(OWNER)));
    });

    it("quien no es miembro NO las lee", async () => {
        await assertFails(getDocs(incidents(STRANGER)));
        await assertFails(getDoc(incident(STRANGER)));
    });
});

describe("reportar", () => {
    it("un miembro reporta una incidencia completa", async () => {
        await assertSucceeds(report(MEMBER));
    });

    it("sin nota también vale", async () => {
        await assertSucceeds(report(OWNER, { note: "" }));
    });

    it("una personalizada, con su nombre", async () => {
        await assertSucceeds(report(MEMBER, { type: "custom", title: "Silla del bebé desmontada" }));
        await assertSucceeds(report(MEMBER, { type: "custom", title: "x".repeat(40) }));
    });

    it("quien no es miembro NO reporta", async () => {
        await assertFails(report(STRANGER));
    });

    it("NO con un tipo que no existe", async () => {
        await assertFails(report(MEMBER, { type: "engine" }));
        await assertFails(report(MEMBER, { type: "" }));
    });

    it("NO una personalizada sin nombre, o solo con espacios", async () => {
        await assertFails(report(MEMBER, { type: "custom", title: "" }));
        await assertFails(report(MEMBER, { type: "custom", title: "   " }));
    });

    it("NO con un nombre de más de 40", async () => {
        await assertFails(report(MEMBER, { type: "custom", title: "x".repeat(41) }));
    });

    it("NO con nombre en un tipo de la lista: ese lo traduce la app", async () => {
        await assertFails(report(MEMBER, { title: "Limpieza a fondo" }));
    });

    it("NO sin el campo del nombre", async () => {
        await assertFails(
            setDoc(doc(incidents(MEMBER)), {
                type: "cleaning",
                note: "",
                status: "open",
                reportedBy: MEMBER,
                createdAt: serverTimestamp(),
            })
        );
    });

    it("NO con los campos de antes del cambio (category y tag)", async () => {
        await assertFails(report(MEMBER, { category: "body" }));
        await assertFails(report(MEMBER, { tag: "cleaning" }));
    });

    it("NO con una nota de más de 280", async () => {
        await assertSucceeds(report(MEMBER, { note: "x".repeat(280) }));
        await assertFails(report(MEMBER, { note: "x".repeat(281) }));
    });

    it("NO en nombre de otro", async () => {
        await assertFails(report(MEMBER, { reportedBy: OWNER }));
    });

    it("NO ya resuelta, ni con una fecha que no sea la del servidor", async () => {
        await assertFails(report(MEMBER, { status: "resolved" }));
        await assertFails(report(MEMBER, { createdAt: Timestamp.fromMillis(Date.now()) }));
    });

    it("NO con campos de más", async () => {
        await assertFails(report(MEMBER, { priority: "high" }));
    });
});

describe("resolver y deshacer", () => {
    it("cualquier miembro la resuelve, no solo quien la reportó", async () => {
        await assertSucceeds(resolve(OTHER_MEMBER));
    });

    it("quien no es miembro NO la resuelve", async () => {
        await assertFails(resolve(STRANGER));
    });

    it("NO en nombre de otro, ni con una fecha que no sea la del servidor", async () => {
        await assertFails(resolve(OTHER_MEMBER, { resolvedBy: MEMBER }));
        await assertFails(resolve(OTHER_MEMBER, { resolvedAt: Timestamp.now() }));
    });

    it("NO con una caducidad de más de 31 días", async () => {
        await assertFails(resolve(OTHER_MEMBER, { expiresAt: inDays(40) }));
    });

    it("NO cambia nada más: una incidencia no se edita", async () => {
        await assertFails(updateDoc(incident(MEMBER), { note: "Otra cosa" }));
        await assertFails(resolve(MEMBER, { type: "damage" }));
    });

    it("Deshacer la vuelve a abrir y quita la resolución", async () => {
        await assertSucceeds(resolve(OTHER_MEMBER));
        await assertSucceeds(
            updateDoc(incident(MEMBER), {
                status: "open",
                resolvedBy: deleteField(),
                resolvedAt: deleteField(),
                expiresAt: deleteField(),
            })
        );
    });

    it("NO se vuelve a abrir dejando la caducidad puesta", async () => {
        await assertSucceeds(resolve(OTHER_MEMBER));
        await assertFails(updateDoc(incident(MEMBER), { status: "open" }));
    });
});

describe("borrar", () => {
    it("quien la reportó la borra", async () => {
        await assertSucceeds(deleteDoc(incident(MEMBER)));
    });

    it("el dueño del vehículo la borra aunque no la reportara", async () => {
        await assertSucceeds(deleteDoc(incident(OWNER)));
    });

    it("otro miembro NO la borra", async () => {
        await assertFails(deleteDoc(incident(OTHER_MEMBER)));
    });

    it("quien no es miembro NO la borra", async () => {
        await assertFails(deleteDoc(incident(STRANGER)));
    });
});
