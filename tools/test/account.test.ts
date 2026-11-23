/**
 * Lo que decide la callable `deleteAccount` (spec 012) para cada vehículo, y su comprobación de
 * login reciente. Son funciones puras: no hace falta emulador para probarlas.
 */
import { describe, expect, it } from "vitest";
import { isRecentLogin, planVehicle, RECENT_LOGIN_SECONDS } from "../../functions/src/account";

const ME = "me";
const parkedBy = (uid: string) => ({ latitude: 1, longitude: 2, timestamp: 3, user: { uid, name: "X", email: "x@test" } });

describe("planVehicle", () => {
    it("dueño y único miembro: se borra el vehículo", () => {
        expect(planVehicle({ ownerId: ME, memberIds: [ME] }, ME)).toEqual({ action: "delete" });
    });

    it("dueño de un compartido: pasa al miembro más antiguo, no al último", () => {
        const plan = planVehicle({ ownerId: ME, memberIds: [ME, "a", "b"] }, ME);
        expect(plan).toEqual({ action: "leave", memberIds: ["a", "b"], newOwnerId: "a", anonymize: false });
    });

    it("miembro que no es dueño: sale, y el dueño no cambia", () => {
        const plan = planVehicle({ ownerId: "a", memberIds: ["a", ME] }, ME);
        expect(plan).toEqual({ action: "leave", memberIds: ["a"], newOwnerId: undefined, anonymize: false });
    });

    it("si lo aparcó él, la tarjeta se anonimiza al salir", () => {
        const plan = planVehicle({ ownerId: "a", memberIds: ["a", ME], lastLocation: parkedBy(ME) }, ME);
        expect(plan).toMatchObject({ action: "leave", anonymize: true });
    });

    it("si lo aparcó otro, la tarjeta no se toca", () => {
        const plan = planVehicle({ ownerId: "a", memberIds: ["a", ME], lastLocation: parkedBy("a") }, ME);
        expect(plan).toMatchObject({ action: "leave", anonymize: false });
    });

    it("ya no es miembro pero sigue en la tarjeta: solo se anonimiza", () => {
        expect(planVehicle({ ownerId: "a", memberIds: ["a"], lastLocation: parkedBy(ME) }, ME)).toEqual({ action: "anonymize" });
    });

    it("reconoce la forma de antes de la spec 008, con id en vez de uid", () => {
        const legacy = { latitude: 1, longitude: 2, timestamp: 3, user: { id: ME, name: "X", email: "x@test" } };
        expect(planVehicle({ ownerId: "a", memberIds: ["a"], lastLocation: legacy }, ME)).toEqual({ action: "anonymize" });
    });

    it("un vehículo ajeno y sin rastro suyo: nada", () => {
        expect(planVehicle({ ownerId: "a", memberIds: ["a", "b"] }, ME)).toEqual({ action: "none" });
    });

    it("ya anonimizado: no hay nada que hacer, así que reintentar es inocuo", () => {
        expect(planVehicle({ ownerId: "a", memberIds: ["a"], lastLocation: { latitude: 1, longitude: 2, timestamp: 3, user: null } }, ME))
            .toEqual({ action: "none" });
    });

    it("dueño que no figura como miembro (estado imposible por reglas): igualmente se traspasa", () => {
        expect(planVehicle({ ownerId: ME, memberIds: ["a"] }, ME)).toMatchObject({ action: "leave", newOwnerId: "a" });
    });
});

describe("isRecentLogin", () => {
    const now = 1_000_000;

    it("un login de hace un momento vale", () => {
        expect(isRecentLogin(now - 10, now)).toBe(true);
    });

    it("justo en el límite vale; un segundo después, no", () => {
        expect(isRecentLogin(now - RECENT_LOGIN_SECONDS, now)).toBe(true);
        expect(isRecentLogin(now - RECENT_LOGIN_SECONDS - 1, now)).toBe(false);
    });

    it("sin auth_time no se da por bueno", () => {
        expect(isRecentLogin(undefined, now)).toBe(false);
        expect(isRecentLogin("123", now)).toBe(false);
    });
});
