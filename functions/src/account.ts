import { getAuth } from "firebase-admin/auth";
import { DocumentReference, getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { CallableRequest } from "firebase-functions/v2/https";
import { isRecentLogin, planVehicle, VehiclePlan } from "./accountPlan";
import { requireUid } from "./requireUid";

const VEHICLES_COLLECTION = "vehicles";
const USERS_COLLECTION = "users";
const INVITES_COLLECTION = "invites";
const MEMBER_IDS_FIELD = "memberIds";
const OWNER_ID_FIELD = "ownerId";
const CREATED_BY_FIELD = "createdBy";
const PARKED_BY_FIELD = "lastLocation.user";

/** A batch takes up to 500 writes; stay well below it. */
const BATCH_LIMIT = 400;

export type DeleteAccountStatus = "deleted" | "requires-recent-login";

/**
 * Every vehicle the person touches: as a member, as the owner, or as whoever parked it last. A
 * member who left after parking still has their name on the card, which is why the last two
 * queries exist.
 */
async function vehiclesTouchedBy(uid: string): Promise<DocumentReference[]> {
    const vehicles = getFirestore().collection(VEHICLES_COLLECTION);
    const snapshots = await Promise.all([
        vehicles.where(MEMBER_IDS_FIELD, "array-contains", uid).get(),
        vehicles.where(OWNER_ID_FIELD, "==", uid).get(),
        vehicles.where(`${PARKED_BY_FIELD}.uid`, "==", uid).get(),
        vehicles.where(`${PARKED_BY_FIELD}.id`, "==", uid).get(),
    ]);
    const byPath = new Map<string, DocumentReference>();
    snapshots.forEach((snapshot) => snapshot.docs.forEach((doc) => byPath.set(doc.ref.path, doc.ref)));
    return [...byPath.values()];
}

/**
 * Applies the plan inside a transaction that re-reads the vehicle first: someone may park it or
 * join it between the query and the write, and the plan has to be made on what is there now.
 */
async function settleVehicle(ref: DocumentReference, uid: string): Promise<VehiclePlan["action"]> {
    return getFirestore().runTransaction(async (transaction) => {
        const snapshot = await transaction.get(ref);
        if (!snapshot.exists) return "none";

        const plan = planVehicle(snapshot.data() ?? {}, uid);
        switch (plan.action) {
            case "none":
                break;
            case "delete":
                transaction.delete(ref);
                break;
            case "anonymize":
                // Not `updatedAt`: wiping who parked is not a change anybody made to the vehicle.
                transaction.update(ref, { [PARKED_BY_FIELD]: null });
                break;
            case "leave":
                transaction.update(ref, {
                    [MEMBER_IDS_FIELD]: plan.memberIds,
                    ...(plan.newOwnerId ? { [OWNER_ID_FIELD]: plan.newOwnerId } : {}),
                    ...(plan.anonymize ? { [PARKED_BY_FIELD]: null } : {}),
                    updatedAt: Date.now(),
                });
                break;
        }
        return plan.action;
    });
}

async function deleteInvitesCreatedBy(uid: string): Promise<number> {
    const firestore = getFirestore();
    const invites = await firestore.collection(INVITES_COLLECTION).where(CREATED_BY_FIELD, "==", uid).get();
    for (let i = 0; i < invites.docs.length; i += BATCH_LIMIT) {
        const batch = firestore.batch();
        invites.docs.slice(i, i + BATCH_LIMIT).forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
    }
    return invites.size;
}

/**
 * Deletes the caller's account and everything it left behind. Spec 012.
 *
 * The client can do almost none of this: it cannot write other people's vehicles, and the rules
 * deny it `invites` altogether. The Auth account goes **last**, so a failure halfway leaves an
 * account the person can still sign into and retry from — never data with nobody left to claim it.
 * Every step is idempotent, so a retry only finishes what is missing.
 *
 * A stale sign-in is an expected outcome, not a failure, so it comes back as a status — the same
 * convention as `joinVehicleWithCode`. The client cannot read an `HttpsError`'s code from common
 * code anyway: gitlive does not expose it there.
 */
export async function deleteAccountHandler(request: CallableRequest): Promise<{ status: DeleteAccountStatus }> {
    const uid = requireUid(request);
    if (!isRecentLogin(request.auth?.token.auth_time, Math.floor(Date.now() / 1000))) {
        return { status: "requires-recent-login" };
    }

    const counts: Record<string, number> = { none: 0, anonymize: 0, delete: 0, leave: 0 };
    for (const ref of await vehiclesTouchedBy(uid)) {
        counts[await settleVehicle(ref, uid)]++;
    }
    const invites = await deleteInvitesCreatedBy(uid);

    await getFirestore().collection(USERS_COLLECTION).doc(uid).delete();

    try {
        await getAuth().deleteUser(uid);
    } catch (error) {
        if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
    }

    logger.info("Deleted an account", {
        uid,
        vehiclesDeleted: counts.delete,
        vehiclesLeft: counts.leave,
        vehiclesAnonymized: counts.anonymize,
        invitesDeleted: invites,
    });
    return { status: "deleted" };
}
