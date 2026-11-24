/**
 * Tests de las reglas de `vehicles` tras la spec 008.
 *
 * Se prueban contra el emulador, no contra un proyecto real. Importa tanto lo que debe **permitirse**
 * como lo que debe **denegarse**: una regla que solo se prueba por el lado bueno no demuestra nada,
 * porque `allow read: if true` pasaria igual.
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

const OWNER = "uid_owner";
const MEMBER = "uid_member";
const STRANGER = "uid_stranger";
const VEHICLE = "vehicle_1";

const OWNER_EMAIL = "owner@apark.test";
const MEMBER_EMAIL = "member@apark.test";

let env: RulesTestEnvironment;

beforeAll(async () => {
    env = await initializeTestEnvironment({
        projectId: "apark-rules-test",
        firestore: { rules: readFileSync("../firestore.rules", "utf8") },
    });
});

afterAll(async () => env?.cleanup());

beforeEach(async () => {
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (context) => {
        await setDoc(doc(context.firestore(), "vehicles", VEHICLE), {
            name: "Coche",
            ownerId: OWNER,
            memberIds: [OWNER, MEMBER],
            lastLocation: null,
        });
    });
});

const as = (uid: string, email: string) =>
    env.authenticatedContext(uid, { email, email_verified: true }).firestore();

describe("lectura", () => {
    it("el dueño lee su vehículo", async () => {
        await assertSucceeds(getDoc(doc(as(OWNER, OWNER_EMAIL), "vehicles", VEHICLE)));
    });

    it("un miembro compartido lo lee", async () => {
        await assertSucceeds(getDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE)));
    });

    it("un extraño NO lo lee", async () => {
        await assertFails(getDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", VEHICLE)));
    });

    it("sin autenticar NO se lee", async () => {
        await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "vehicles", VEHICLE)));
    });
});

describe("creación", () => {
    const valid = { name: "Nuevo", ownerId: STRANGER, memberIds: [STRANGER], lastLocation: null };

    it("crear siendo el único miembro y dueño", async () => {
        await assertSucceeds(setDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", "v_ok"), valid));
    });

    it("NO se puede crear con otro como dueño", async () => {
        await assertFails(
            setDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", "v_bad"), { ...valid, ownerId: OWNER })
        );
    });

    it("NO se puede crear colando a alguien más en memberIds", async () => {
        await assertFails(
            setDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", "v_bad"), {
                ...valid,
                memberIds: [STRANGER, OWNER],
            })
        );
    });

    it("NO se puede crear sin memberIds", async () => {
        await assertFails(
            setDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", "v_bad"), {
                name: "Nuevo",
                ownerId: STRANGER,
                lastLocation: null,
            })
        );
    });
});

describe("aparcar", () => {
    const location = {
        latitude: 40.4,
        longitude: -3.7,
        timestamp: 1,
        user: { uid: MEMBER, name: "Miembro", email: MEMBER_EMAIL },
    };

    it("un miembro escribe lastLocation con su propio email", async () => {
        await assertSucceeds(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), { lastLocation: location })
        );
    });

    it("NO puede firmar la ubicación con el email de otro", async () => {
        await assertFails(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), {
                lastLocation: { ...location, user: { ...location.user, email: OWNER_EMAIL } },
            })
        );
    });

    it("NO puede aprovechar para cambiar el nombre de paso", async () => {
        await assertFails(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), {
                lastLocation: location,
                name: "Secuestrado",
            })
        );
    });

    it("un extraño NO puede aparcarlo", async () => {
        await assertFails(
            updateDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", VEHICLE), { lastLocation: location })
        );
    });
});

describe("salirse", () => {
    it("un miembro se quita a sí mismo", async () => {
        await assertSucceeds(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), { memberIds: [OWNER] })
        );
    });

    it("NO puede echar a otro", async () => {
        await assertFails(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), { memberIds: [MEMBER] })
        );
    });

    it("NO puede quitarse y colar a un tercero a la vez", async () => {
        await assertFails(
            updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), { memberIds: [OWNER, STRANGER] })
        );
    });

    it("el DUEÑO no puede usar esta vía: dejaría el vehículo sin dueño legible", async () => {
        await assertFails(
            updateDoc(doc(as(OWNER, OWNER_EMAIL), "vehicles", VEHICLE), { memberIds: [MEMBER] })
        );
    });
});

describe("gestión de miembros (spec 014)", () => {
    const update = (uid: string, email: string, data: Record<string, unknown>) =>
        updateDoc(doc(as(uid, email), "vehicles", VEHICLE), data);

    it("el dueño pasa la propiedad a un miembro, y sigue siendo miembro", async () => {
        await assertSucceeds(update(OWNER, OWNER_EMAIL, { ownerId: MEMBER }));
    });

    it("NO a alguien que no es miembro: el vehículo tendría un dueño que no puede leerlo", async () => {
        await assertFails(update(OWNER, OWNER_EMAIL, { ownerId: STRANGER }));
    });

    it("NO puede pasar la propiedad y quitarse en la misma escritura", async () => {
        await assertFails(update(OWNER, OWNER_EMAIL, { ownerId: MEMBER, memberIds: [MEMBER] }));
    });

    it("el dueño quita a un miembro", async () => {
        await assertSucceeds(update(OWNER, OWNER_EMAIL, { memberIds: [OWNER] }));
    });

    it("el dueño NO cuela a nadie: se entra solo con un código", async () => {
        await assertFails(update(OWNER, OWNER_EMAIL, { memberIds: [OWNER, MEMBER, STRANGER] }));
    });

    it("el dueño NO cambia a un miembro por otro", async () => {
        await assertFails(update(OWNER, OWNER_EMAIL, { memberIds: [OWNER, STRANGER] }));
    });

    it("un miembro NO pasa la propiedad, ni a sí mismo", async () => {
        await assertFails(update(MEMBER, MEMBER_EMAIL, { ownerId: MEMBER }));
    });

    it("el dueño sigue pudiendo aparcar: la app 1.1.0 escribe por esta rama", async () => {
        await assertSucceeds(
            update(OWNER, OWNER_EMAIL, {
                lastLocation: { latitude: 1, longitude: 2, timestamp: 3, user: { uid: OWNER, name: "", email: OWNER_EMAIL } },
            })
        );
    });

    it("tras pasar la propiedad, el antiguo dueño ya no puede quitar a nadie", async () => {
        await assertSucceeds(update(OWNER, OWNER_EMAIL, { ownerId: MEMBER }));
        await assertFails(update(OWNER, OWNER_EMAIL, { memberIds: [OWNER] }));
    });
});

describe("datos del vehículo (spec 015)", () => {
    const fresh = { name: "Octavia", ownerId: STRANGER, memberIds: [STRANGER], lastLocation: null };
    const data = {
        type: "car",
        powertrain: "hybrid",
        fuel: "petrol",
        make: "Škoda",
        model: "Octavia",
        color: "red",
        emissionsLabel: "eco",
    };
    const create = (extra: Record<string, unknown>) =>
        setDoc(doc(as(STRANGER, "x@apark.test"), "vehicles", "v_data"), { ...fresh, ...extra });
    const ownerUpdate = (extra: Record<string, unknown>) =>
        updateDoc(doc(as(OWNER, OWNER_EMAIL), "vehicles", VEHICLE), extra);

    it("se crea con todos sus datos", async () => {
        await assertSucceeds(create(data));
    });

    it("se crea con los datos vacíos: «sin dato» es la cadena vacía", async () => {
        await assertSucceeds(create({ type: "", powertrain: "", fuel: "", make: "", model: "", color: "", emissionsLabel: "" }));
    });

    it("NO se crea con un código que no existe", async () => {
        await assertFails(create({ ...data, type: "submarine" }));
        await assertFails(create({ ...data, fuel: "Diésel" }));
        await assertFails(create({ ...data, emissionsLabel: "A" }));
    });

    it("NO se crea con una marca o un modelo de más de 40", async () => {
        await assertFails(create({ ...data, make: "x".repeat(41) }));
        await assertFails(create({ ...data, model: "x".repeat(41) }));
    });

    it("el dueño edita los datos", async () => {
        await assertSucceeds(ownerUpdate({ ...data, name: "Coche nuevo", licensePlate: "1234 ABC" }));
    });

    it("el dueño NO pone un color fuera de la paleta", async () => {
        await assertFails(ownerUpdate({ color: "granate" }));
    });

    it("el dueño NO deja el nombre vacío ni lo pasa de 60", async () => {
        await assertFails(ownerUpdate({ name: "" }));
        await assertFails(ownerUpdate({ name: "x".repeat(61) }));
    });

    it("un miembro NO edita los datos", async () => {
        await assertFails(updateDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE), { type: "car" }));
    });

    it("un valor antiguo inválido no impide aparcar ni editar otro campo: solo se valida lo que cambia", async () => {
        await env.withSecurityRulesDisabled(async (context) => {
            await updateDoc(doc(context.firestore(), "vehicles", VEHICLE), { color: "granate metalizado" });
        });
        await assertSucceeds(
            ownerUpdate({ lastLocation: { latitude: 1, longitude: 2, timestamp: 3, user: { uid: OWNER, name: "", email: OWNER_EMAIL } } })
        );
        await assertSucceeds(ownerUpdate({ type: "van" }));
    });
});

describe("borrado", () => {
    it("el dueño borra", async () => {
        const { deleteDoc } = await import("firebase/firestore");
        await assertSucceeds(deleteDoc(doc(as(OWNER, OWNER_EMAIL), "vehicles", VEHICLE)));
    });

    it("un miembro compartido NO borra", async () => {
        const { deleteDoc } = await import("firebase/firestore");
        await assertFails(deleteDoc(doc(as(MEMBER, MEMBER_EMAIL), "vehicles", VEHICLE)));
    });
});

describe("invites: cerrada a cal y canto", () => {
    it("ni el dueño puede leerlas", async () => {
        await assertFails(getDoc(doc(as(OWNER, OWNER_EMAIL), "invites", "ABC123")));
    });

    it("nadie puede escribirlas", async () => {
        await assertFails(setDoc(doc(as(OWNER, OWNER_EMAIL), "invites", "ABC123"), { vehicleId: VEHICLE }));
    });
});
