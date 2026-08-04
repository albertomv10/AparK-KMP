import { randomInt } from "crypto";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";

const INVITES_COLLECTION = "invites";
const VEHICLES_COLLECTION = "vehicles";
const USERS_COLLECTION = "users";
const USER_VEHICLES_FIELD = "userVehicles";
const SHARED_USERS_FIELD = "sharedUsers";

/** No I, L, O, 0 or 1: the code is typed by hand and those are read wrong. */
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 8;
const CODE_ATTEMPTS = 5;
const INVITE_TTL_MS = 24 * 60 * 60 * 1000;

function generateCode(): string {
    let code = "";
    for (let i = 0; i < CODE_LENGTH; i++) {
        code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
    }
    return code;
}

/** Codes are shared as text and retyped, so accept lowercase, spaces and dashes. */
function normalizeCode(raw: unknown): string {
    if (typeof raw !== "string") return "";
    return raw.toUpperCase().replace(/[^A-Z0-9]/g, "");
}

function requireUid(request: CallableRequest): string {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new HttpsError("unauthenticated", "You must be signed in.");
    }
    return uid;
}

/**
 * Creates a single-use invitation for a vehicle the caller owns.
 *
 * Invitations are created here rather than on the client for two reasons: the client cannot read
 * the `invites` collection at all (rules deny it, which is what stops codes being enumerated),
 * and a tampered client could otherwise mint itself an invitation that never expires.
 */
export function createInviteHandler(databaseId: string) {
    return async (request: CallableRequest<{ vehicleId?: string }>) => {
        const uid = requireUid(request);
        const vehicleId = request.data?.vehicleId;

        if (typeof vehicleId !== "string" || vehicleId.length === 0) {
            throw new HttpsError("invalid-argument", "A vehicleId is required.");
        }

        const firestore = getFirestore(databaseId);
        const vehicleSnapshot = await firestore.collection(VEHICLES_COLLECTION).doc(vehicleId).get();

        if (!vehicleSnapshot.exists) {
            throw new HttpsError("not-found", "That vehicle no longer exists.");
        }
        if (vehicleSnapshot.data()?.ownerId !== uid) {
            throw new HttpsError("permission-denied", "Only the owner can share a vehicle.");
        }

        // At most one live code per vehicle: sharing again revokes whatever was handed out
        // before, which is what gives the owner a way to take an invitation back.
        // Filtered in memory rather than with a second `where`, to avoid needing a composite index.
        const existing = await firestore
            .collection(INVITES_COLLECTION)
            .where("vehicleId", "==", vehicleId)
            .get();

        const superseded = existing.docs.filter((doc) => !doc.data().usedBy);
        if (superseded.length > 0) {
            const batch = firestore.batch();
            superseded.forEach((doc) => batch.delete(doc.ref));
            await batch.commit();
        }

        const expiresAt = Timestamp.fromMillis(Date.now() + INVITE_TTL_MS);

        for (let attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            const code = generateCode();
            try {
                // `create` fails if the id is taken, so a collision cannot overwrite a live invite.
                await firestore.collection(INVITES_COLLECTION).doc(code).create({
                    vehicleId,
                    createdBy: uid,
                    createdAt: FieldValue.serverTimestamp(),
                    expiresAt,
                    usedBy: null,
                });
                return { code, expiresAt: expiresAt.toMillis() };
            } catch (error) {
                // ALREADY_EXISTS: try another code. Anything else is a real failure.
                if ((error as { code?: number }).code !== 6) throw error;
            }
        }

        throw new HttpsError("internal", "Could not generate an invitation code.");
    };
}

/**
 * Joins the caller to the vehicle an invitation points at.
 *
 * This has to run server-side: rules stop a client from finding a vehicle it is not a member of,
 * and equally from adding itself to that vehicle's `sharedUsers`.
 */
export function joinWithCodeHandler(databaseId: string) {
    return async (request: CallableRequest<{ code?: string }>) => {
        const uid = requireUid(request);
        const code = normalizeCode(request.data?.code);

        if (code.length === 0) {
            throw new HttpsError("invalid-argument", "A code is required.");
        }

        const firestore = getFirestore(databaseId);
        const inviteRef = firestore.collection(INVITES_COLLECTION).doc(code);
        const inviteSnapshot = await inviteRef.get();

        if (!inviteSnapshot.exists) {
            return { status: "invalid" as const };
        }

        const invite = inviteSnapshot.data()!;

        // Expected outcomes are returned as a status rather than thrown: an HttpsError arrives
        // at the client as a platform-specific exception whose code is awkward to inspect from
        // shared Kotlin, and the wording would end up untranslated in front of the user.
        if (invite.usedBy) {
            return { status: "used" as const };
        }
        if ((invite.expiresAt as Timestamp).toMillis() <= Date.now()) {
            return { status: "expired" as const };
        }

        const vehicleRef = firestore.collection(VEHICLES_COLLECTION).doc(invite.vehicleId);
        const vehicleSnapshot = await vehicleRef.get();

        if (!vehicleSnapshot.exists) {
            return { status: "invalid" as const };
        }

        const vehicle = vehicleSnapshot.data()!;
        const alreadyMember = vehicle.ownerId === uid || (vehicle.sharedUsers ?? []).includes(uid);

        // Deliberately does not consume the invitation: telling someone they already have the
        // vehicle should not burn the code they may still need to pass on.
        if (alreadyMember) {
            return { status: "already_member" as const };
        }

        const userRef = firestore.collection(USERS_COLLECTION).doc(uid);

        await firestore.runTransaction(async (transaction) => {
            const freshInvite = await transaction.get(inviteRef);
            // Re-checked inside the transaction: two people racing on the same code must not
            // both get in.
            if (freshInvite.data()?.usedBy) {
                throw new HttpsError("aborted", "used");
            }

            transaction.update(vehicleRef, { [SHARED_USERS_FIELD]: FieldValue.arrayUnion(uid) });
            // `set` with merge rather than `update`: the user document is created on sign-up, but
            // this must not fail if it is somehow missing.
            transaction.set(
                userRef,
                { [USER_VEHICLES_FIELD]: FieldValue.arrayUnion(invite.vehicleId) },
                { merge: true }
            );
            transaction.update(inviteRef, { usedBy: uid, usedAt: FieldValue.serverTimestamp() });
        });

        return { status: "ok" as const, vehicleName: vehicle.name ?? "" };
    };
}
