import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { onDocumentDeleted, FirestoreEvent, QueryDocumentSnapshot } from "firebase-functions/v2/firestore";
// Imported from its own entry point rather than the `firebase-functions/v2` barrel: that barrel
// pulls in every provider, including Realtime Database, whose transitive @firebase/app peer
// dependency npm does not install — which breaks the deploy-time analysis of this codebase.
import * as logger from "firebase-functions/logger";
import { onCall } from "firebase-functions/v2/https";
import { createInviteHandler, joinWithCodeHandler } from "./invites";

initializeApp();

/**
 * Firestore lives in `eur3` in both projects, and a v2 Firestore trigger has to be deployed in a
 * region matching its database's location — the default `us-central1` would be rejected.
 */
const REGION = "europe-west4";

const USERS_COLLECTION = "users";
const USER_VEHICLES_FIELD = "userVehicles";
const INVITES_COLLECTION = "invites";
const VEHICLE_ID_FIELD = "vehicleId";

/**
 * Drops a deleted vehicle's id from the `userVehicles` array of every member.
 *
 * The client can only clean its own document — security rules forbid writing to anybody else's —
 * so without this the id lingers in the other members' lists. The app already tolerates those
 * dangling ids, but each one costs retries on their next start.
 *
 * Safe to run more than once: `arrayRemove` on an id that is already gone does nothing, so a
 * retried invocation is harmless.
 */
async function removeVehicleFromMembers(
    event: FirestoreEvent<QueryDocumentSnapshot | undefined, { vehicleId: string }>
): Promise<void> {
    const vehicleId = event.params.vehicleId;
    const vehicle = event.data?.data();

    if (!vehicle) {
        logger.warn("Deleted vehicle carried no data; nothing to clean up", { vehicleId });
        return;
    }

    const ownerId: string = vehicle.ownerId ?? "";
    const sharedUsers: string[] = vehicle.sharedUsers ?? [];

    // The owner is not part of sharedUsers, and a member could in principle appear twice.
    const memberIds = [...new Set([ownerId, ...sharedUsers])].filter((id) => id.length > 0);

    if (memberIds.length === 0) {
        logger.info("Vehicle had no members", { vehicleId });
        return;
    }

    const firestore = getFirestore();
    const userRefs = memberIds.map((id) => firestore.collection(USERS_COLLECTION).doc(id));

    // A batched update against a missing document rejects the *whole* batch, and a member may
    // well have deleted their account, so only touch the documents that are actually there.
    const snapshots = await firestore.getAll(...userRefs);
    const existing = snapshots.filter((snapshot) => snapshot.exists);

    if (existing.length === 0) {
        logger.info("No member documents left to clean", { vehicleId });
        return;
    }

    const batch = firestore.batch();
    for (const snapshot of existing) {
        batch.update(snapshot.ref, { [USER_VEHICLES_FIELD]: FieldValue.arrayRemove(vehicleId) });
    }
    await batch.commit();

    logger.info("Cleaned up vehicle references", {
        vehicleId,
        members: memberIds.length,
        cleaned: existing.length,
    });
}

/**
 * Deletes the invitations pointing at a vehicle that no longer exists.
 *
 * The TTL policy on `expiresAt` would sweep them up eventually, but until then they are live codes
 * for something that is gone. This needs nothing but the id from the event parameters, which is why
 * it does not read the deleted document.
 *
 * Safe to run more than once: deleting an already-deleted document is a no-op.
 */
async function deleteVehicleInvites(vehicleId: string): Promise<void> {
    const firestore = getFirestore();
    const invites = await firestore
        .collection(INVITES_COLLECTION)
        .where(VEHICLE_ID_FIELD, "==", vehicleId)
        .get();

    if (invites.empty) {
        return;
    }

    // One batch is enough: a vehicle holds at most one live invitation plus whatever used ones the
    // TTL policy has not swept yet, which is nowhere near the 500-write limit.
    const batch = firestore.batch();
    invites.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    logger.info("Deleted invitations for a removed vehicle", {
        vehicleId,
        deleted: invites.size,
    });
}

/**
 * Both cleanups are attempted even if one fails, and neither depends on the other: the member
 * cleanup needs the deleted document's data, while the invitation cleanup only needs its id.
 */
export const cleanupVehicleReferences = onDocumentDeleted(
    { document: "vehicles/{vehicleId}", region: REGION },
    async (event) => {
        await Promise.all([
            removeVehicleFromMembers(event),
            deleteVehicleInvites(event.params.vehicleId),
        ]);
    }
);

export const createVehicleInvite = onCall({ region: REGION }, createInviteHandler);
export const joinVehicleWithCode = onCall({ region: REGION }, joinWithCodeHandler);
