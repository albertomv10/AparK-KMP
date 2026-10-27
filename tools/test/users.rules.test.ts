/**
 * Tests de las reglas de `users` tras la spec 011.
 *
 * El nombre pasa a ser texto libre que ven los demás miembros, así que se valida; pero solo cuando
 * cambia, para no dejar bloqueados los documentos que ya traían un nombre largo de Google.
 */
import { readFileSync } from "node:fs";
import {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment,
    type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";

const ME = "uid_me";
const OTHER = "uid_other";

/** 41 caracteres: uno más de lo que se acepta. */
const TOO_LONG = "x".repeat(41);

let env: RulesTestEnvironment;

beforeAll(async () => {
    env = await initializeTestEnvironment({
        // Distinto del de `vehicles.rules.test.ts`: vitest corre los ficheros en paralelo, y los
        // dos limpian la base antes de cada test.
        projectId: "apark-rules-users-test",
        firestore: { rules: readFileSync("../firestore.rules", "utf8") },
    });
});

afterAll(async () => env?.cleanup());

beforeEach(async () => {
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (context) => {
        await setDoc(doc(context.firestore(), "users", ME), {
            email: "me@apark.test",
            name: "Yo",
            userVehicles: ["a", "b"],
        });
        // Un documento de antes de esta regla, sembrado con un displayName de Google largo.
        await setDoc(doc(context.firestore(), "users", OTHER), {
            email: "other@apark.test",
            name: TOO_LONG,
            userVehicles: ["c", "d"],
        });
    });
});

const as = (uid: string) => env.authenticatedContext(uid).firestore();

describe("leer y crear", () => {
    it("cada uno lee el suyo", async () => {
        await assertSucceeds(getDoc(doc(as(ME), "users", ME)));
    });

    it("NO se lee el de otro", async () => {
        await assertFails(getDoc(doc(as(ME), "users", OTHER)));
    });

    it("se crea el propio, aunque traiga el nombre largo de un proveedor", async () => {
        await assertSucceeds(setDoc(doc(as("uid_new"), "users", "uid_new"), { email: "n@apark.test", name: TOO_LONG }));
    });
});

describe("cambiar el nombre", () => {
    it("uno cambia el suyo", async () => {
        await assertSucceeds(updateDoc(doc(as(ME), "users", ME), { name: "Eduardo", updatedAt: 1 }));
    });

    it("con 40 caracteres vale", async () => {
        await assertSucceeds(updateDoc(doc(as(ME), "users", ME), { name: "x".repeat(40) }));
    });

    it("NO con 41 caracteres", async () => {
        await assertFails(updateDoc(doc(as(ME), "users", ME), { name: TOO_LONG }));
    });

    it("NO vacío", async () => {
        await assertFails(updateDoc(doc(as(ME), "users", ME), { name: "" }));
    });

    it("NO algo que no sea texto", async () => {
        await assertFails(updateDoc(doc(as(ME), "users", ME), { name: 42 }));
    });

    it("NO el de otro", async () => {
        await assertFails(updateDoc(doc(as(ME), "users", OTHER), { name: "Secuestrado" }));
    });
});

describe("lo que no toca el nombre", () => {
    it("un documento con nombre antiguo largo puede seguir reordenando", async () => {
        await assertSucceeds(updateDoc(doc(as(OTHER), "users", OTHER), { userVehicles: ["d", "c"] }));
    });

    it("sincronizar el email no se ve afectado", async () => {
        await assertSucceeds(updateDoc(doc(as(ME), "users", ME), { email: "nuevo@apark.test", updatedAt: 2 }));
    });
});
