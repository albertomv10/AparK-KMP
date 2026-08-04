import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { onDocumentDeleted, FirestoreEvent, QueryDocumentSnapshot } from "firebase-functions/v2/firestore";
// Imported from its own entry point rather than the `firebase-functions/v2` barrel: that barrel
// pulls in every provider, including Realtime Database, whose transitive @firebase/app peer
// dependency npm does not install — which breaks the deploy-time analysis of this codebase.
import * as logger from "firebase-functions/logger";

initializeApp();

/**
 * Both Firestore databases live in `eur3`, and a v2 Firestore trigger has to be deployed in a
 * region matching its database's location — the default `us-central1` would be rejected.
 */
const REGION = "europe-west4";

const PROD_DATABASE = "(default)";
const DEBUG_DATABASE = "apark-at";

const USERS_COLLECTION = "users";
const USER_VEHICLES_FIELD = "userVehicles";

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
    event: FirestoreEvent<QueryDocumentSnapshot | undefined, { vehicleId: string }>,
    databaseId: string
): Promise<void> {
    const vehicleId = event.params.vehicleId;
    const vehicle = event.data?.data();

    if (!vehicle) {
        logger.warn("Deleted vehicle carried no data; nothing to clean up", { vehicleId, databaseId });
        return;
    }

    const ownerId: string = vehicle.ownerId ?? "";
    const sharedUsers: string[] = vehicle.sharedUsers ?? [];

    // The owner is not part of sharedUsers, and a member could in principle appear twice.
    const memberIds = [...new Set([ownerId, ...sharedUsers])].filter((id) => id.length > 0);

    if (memberIds.length === 0) {
        logger.info("Vehicle had no members", { vehicleId, databaseId });
        return;
    }

    // Writes must land in the database that raised the event: cleaning the wrong one would fail
    // silently, leaving the ids behind while the function reports success.
    const firestore = getFirestore(databaseId);
    const userRefs = memberIds.map((id) => firestore.collection(USERS_COLLECTION).doc(id));

    // A batched update against a missing document rejects the *whole* batch, and a member may
    // well have deleted their account, so only touch the documents that are actually there.
    const snapshots = await firestore.getAll(...userRefs);
    const existing = snapshots.filter((snapshot) => snapshot.exists);

    if (existing.length === 0) {
        logger.info("No member documents left to clean", { vehicleId, databaseId });
        return;
    }

    const batch = firestore.batch();
    for (const snapshot of existing) {
        batch.update(snapshot.ref, { [USER_VEHICLES_FIELD]: FieldValue.arrayRemove(vehicleId) });
    }
    await batch.commit();

    logger.info("Cleaned up vehicle references", {
        vehicleId,
        databaseId,
        members: memberIds.length,
        cleaned: existing.length,
    });
}

export const cleanupVehicleReferences = onDocumentDeleted(
    { document: "vehicles/{vehicleId}", database: PROD_DATABASE, region: REGION },
    (event) => removeVehicleFromMembers(event, PROD_DATABASE)
);

export const cleanupVehicleReferencesDebug = onDocumentDeleted(
    { document: "vehicles/{vehicleId}", database: DEBUG_DATABASE, region: REGION },
    (event) => removeVehicleFromMembers(event, DEBUG_DATABASE)
);
