/**
 * Tests del script de migración de la spec 008.
 *
 * Corre contra el emulador, así que no hacen falta credenciales — que es justo lo que permite
 * probar de verdad un script que acabará escribiendo en producción.
 */
import { deleteApp, initializeApp } from "firebase-admin/app";
import { getFirestore, type Firestore } from "firebase-admin/firestore";
import { afterAll, beforeEach, describe, expect, it } from "vitest";
import { membersOf, migrate } from "../migrate-member-ids.js";

const app = initializeApp({ projectId: "apark-migrate-test" }, "migrate-test");
const db: Firestore = getFirestore(app);

afterAll(async () => deleteApp(app));

async function reset() {
    const docs = await db.collection("vehicles").get();
    await Promise.all(docs.docs.map((d) => d.ref.delete()));
}

const options = { project: "apark-migrate-test", dryRun: false, dropSharedUsers: false };

beforeEach(reset);

describe("membersOf", () => {
    it("incluye al dueño y a los compartidos, sin repetir", () => {
        expect(membersOf({ ownerId: "a", sharedUsers: ["b", "a"] })).toEqual(["a", "b"]);
    });

    it("aguanta un documento sin sharedUsers", () => {
        expect(membersOf({ ownerId: "a" })).toEqual(["a"]);
    });

    it("descarta un documento sin dueño en vez de inventárselo", () => {
        expect(membersOf({ sharedUsers: [] })).toEqual([]);
    });
});

describe("backfill", () => {
    it("escribe memberIds a partir de ownerId y sharedUsers", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"] });
        await migrate(options, db);
        expect((await db.collection("vehicles").doc("v1").get()).data()?.memberIds).toEqual(["a", "b"]);
    });

    it("--dry-run no escribe nada", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"] });
        await migrate({ ...options, dryRun: true }, db);
        expect((await db.collection("vehicles").doc("v1").get()).data()?.memberIds).toBeUndefined();
    });

    it("es idempotente: la segunda pasada no cambia nada", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"] });
        await migrate(options, db);
        const first = (await db.collection("vehicles").doc("v1").get()).updateTime;
        await migrate(options, db);
        const second = (await db.collection("vehicles").doc("v1").get()).updateTime;
        expect(second?.isEqual(first!)).toBe(true);
    });

    it("no toca sharedUsers durante el backfill", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"] });
        await migrate(options, db);
        expect((await db.collection("vehicles").doc("v1").get()).data()?.sharedUsers).toEqual(["b"]);
    });

    it("deja intacto un vehículo sin dueño en vez de dejarlo invisible", async () => {
        await db.collection("vehicles").doc("huerfano").set({ name: "Sin dueño" });
        await migrate(options, db);
        expect((await db.collection("vehicles").doc("huerfano").get()).data()?.memberIds).toBeUndefined();
    });
});

describe("limpieza", () => {
    it("borra sharedUsers cuando ya existe memberIds", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"], memberIds: ["a", "b"] });
        await migrate({ ...options, dropSharedUsers: true }, db);
        const data = (await db.collection("vehicles").doc("v1").get()).data();
        expect(data?.sharedUsers).toBeUndefined();
        expect(data?.memberIds).toEqual(["a", "b"]);
    });

    it("hace las dos cosas de una si el documento venía sin migrar", async () => {
        await db.collection("vehicles").doc("v1").set({ ownerId: "a", sharedUsers: ["b"] });
        await migrate({ ...options, dropSharedUsers: true }, db);
        const data = (await db.collection("vehicles").doc("v1").get()).data();
        expect(data?.memberIds).toEqual(["a", "b"]);
        expect(data?.sharedUsers).toBeUndefined();
    });

    it("un vehículo sin dueño conserva el acceso de sus compartidos", async () => {
        // Está corrupto (sin ownerId no hay quien lo borre), pero `b` sigue pudiendo leerlo:
        // la pertenencia se deriva igual, así que borrar sharedUsers no le quita nada.
        await db.collection("vehicles").doc("huerfano").set({ sharedUsers: ["b"] });
        await migrate({ ...options, dropSharedUsers: true }, db);
        const data = (await db.collection("vehicles").doc("huerfano").get()).data();
        expect(data?.memberIds).toEqual(["b"]);
        expect(data?.sharedUsers).toBeUndefined();
    });

    it("nunca deja un documento sin ninguna forma de pertenencia", async () => {
        await db.collection("vehicles").doc("vacio").set({ name: "Ni dueño ni compartidos" });
        await migrate({ ...options, dropSharedUsers: true }, db);
        const data = (await db.collection("vehicles").doc("vacio").get()).data();
        expect(data?.memberIds).toBeUndefined();
        expect(data?.name).toBe("Ni dueño ni compartidos");
    });
});
