/**
 * Lo que devuelve la callable `getVehicleMembers` (spec 014). Funciones puras: sin emulador.
 */
import { describe, expect, it } from "vitest";
import { displayNameOf, memberList } from "../../functions/src/membersPlan";

describe("displayNameOf", () => {
    it("el nombre, recortado", () => {
        expect(displayNameOf({ name: "  Edu ", email: "edu@test" })).toBe("Edu");
    });

    it("sin nombre, el email: es lo que ya hace «Aparcado por»", () => {
        expect(displayNameOf({ name: "", email: "edu@test" })).toBe("edu@test");
        expect(displayNameOf({ name: "   ", email: "edu@test" })).toBe("edu@test");
        expect(displayNameOf({ email: "edu@test" })).toBe("edu@test");
    });

    it("sin perfil, o con basura en vez de texto: vacío, y la app pone el marcador", () => {
        expect(displayNameOf(undefined)).toBe("");
        expect(displayNameOf({ name: 42, email: null })).toBe("");
    });
});

describe("memberList", () => {
    const profiles = {
        owner: { name: "Alberto", email: "a@test" },
        first: { name: "", email: "first@test" },
        second: { name: "Edu", email: "e@test" },
    };

    it("el dueño primero, y el resto en orden de entrada", () => {
        const list = memberList(["first", "owner", "second"], "owner", profiles);
        expect(list.map((m) => m.uid)).toEqual(["owner", "first", "second"]);
        expect(list.map((m) => m.isOwner)).toEqual([true, false, false]);
    });

    it("solo sale una etiqueta por miembro, nunca nombre y email juntos", () => {
        const list = memberList(["owner", "first"], "owner", profiles);
        expect(list).toEqual([
            { uid: "owner", displayName: "Alberto", isOwner: true },
            { uid: "first", displayName: "first@test", isOwner: false },
        ]);
    });

    it("un miembro sin perfil, como una cuenta borrada desde la consola, sale con la etiqueta vacía", () => {
        expect(memberList(["owner", "ghost"], "owner", profiles)[1]).toEqual({
            uid: "ghost",
            displayName: "",
            isOwner: false,
        });
    });

    it("ignora duplicados, ids vacíos y lo que no es texto", () => {
        const list = memberList(["owner", "owner", "", 7, "second"], "owner", profiles);
        expect(list.map((m) => m.uid)).toEqual(["owner", "second"]);
    });

    it("sin memberIds legible, la lista está vacía", () => {
        expect(memberList(undefined, "owner", profiles)).toEqual([]);
    });
});
