import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import type { Change, FirestoreEvent, QueryDocumentSnapshot } from "firebase-functions/v2/firestore";

const VEHICLES_COLLECTION = "vehicles";
const MEMBER_IDS_FIELD = "memberIds";
const PARKED_BY_NAME = "lastLocation.user.name";
const PARKED_BY_EMAIL = "lastLocation.user.email";

/** Same ceiling as `firestore.rules`; enforced here too because the Admin SDK skips the rules. */
const NAME_MAX_LENGTH = 40;

interface ProfileChange {
    name?: string;
    email?: string;
}

/**
 * `lastLocation.user` names whoever parked last. It used to be a whole `User`, keyed by `id`;
 * spec 008 slimmed it down to `{uid, name, email}`. Vehicles nobody has parked since still carry
 * the old shape, so both keys count.
 */
function isParkedBy(vehicle: FirebaseFirestore.DocumentData | undefined, uid: string): boolean {
    const parkedBy = vehicle?.lastLocation?.user;
    return !!parkedBy && (parkedBy.uid === uid || parkedBy.id === uid);
}

/** `null` when the account is gone: its profile may outlive it for a moment, and that is fine. */
async function authEmailOf(uid: string): Promise<string | null> {
    try {
        return (await getAuth().getUser(uid)).email ?? null;
    } catch (error) {
        if ((error as { code?: string }).code === "auth/user-not-found") return null;
        throw error;
    }
}

/**
 * Works out what actually changed and is safe to copy.
 *
 * The email in `users` is written by its owner, who can put anything there, while the parking rule
 * trusts only the email in the token. So the email is copied only when it matches the account's —
 * the client writes it precisely to catch up with Auth after a change, and in that case it does.
 */
async function profileChange(
    uid: string,
    before: FirebaseFirestore.DocumentData,
    after: FirebaseFirestore.DocumentData
): Promise<ProfileChange> {
    const change: ProfileChange = {};

    const name = typeof after.name === "string" ? after.name.trim().slice(0, NAME_MAX_LENGTH) : "";
    if (after.name !== before.name && name.length > 0) {
        change.name = name;
    }

    if (after.email !== before.email && typeof after.email === "string" && after.email.length > 0) {
        const authEmail = await authEmailOf(uid);
        if (authEmail !== null && authEmail.toLowerCase() === after.email.toLowerCase()) {
            change.email = authEmail;
        } else {
            logger.warn("users.email does not match the account's; not copying it", { uid });
        }
    }

    return change;
}

/**
 * The account's own `displayName` is what Firebase's emails greet the user with, and where a
 * Google sign-in takes its name from. Nothing in the app reads it back, so a failure here is not
 * worth failing the cascade over.
 */
async function syncAuthDisplayName(uid: string, name: string): Promise<void> {
    try {
        await getAuth().updateUser(uid, { displayName: name });
    } catch (error) {
        if ((error as { code?: string }).code === "auth/user-not-found") return;
        logger.error("Could not update the account's displayName", { uid, error });
    }
}

/**
 * Rewrites the copy of the person inside every vehicle they parked last.
 *
 * Each vehicle gets its own transaction that re-reads it before writing: another member may park it
 * between the query and the write, and without that check they would end up wearing this person's
 * name. `timestamp` and `updatedAt` are left alone — the card shows "parked X minutes ago" from the
 * first, and renaming yourself is not parking.
 */
async function rewriteParkedBy(uid: string, change: ProfileChange): Promise<void> {
    const firestore = getFirestore();
    const memberOf = await firestore
        .collection(VEHICLES_COLLECTION)
        .where(MEMBER_IDS_FIELD, "array-contains", uid)
        .get();

    const parked = memberOf.docs.filter((doc) => isParkedBy(doc.data(), uid));
    if (parked.length === 0) {
        logger.info("Profile changed; no vehicle carries a copy of it", { uid });
        return;
    }

    const fields: Record<string, string> = {};
    if (change.name !== undefined) fields[PARKED_BY_NAME] = change.name;
    if (change.email !== undefined) fields[PARKED_BY_EMAIL] = change.email;

    // Settled, not all: one vehicle failing should not leave the others stale.
    const results = await Promise.allSettled(
        parked.map((doc) =>
            firestore.runTransaction(async (transaction) => {
                const fresh = await transaction.get(doc.ref);
                if (!fresh.exists || !isParkedBy(fresh.data(), uid)) return false;
                transaction.update(doc.ref, fields);
                return true;
            })
        )
    );

    const failed = results.filter((result) => result.status === "rejected");
    const rewritten = results.filter((result) => result.status === "fulfilled" && result.value).length;
    logger.info("Copied the profile into the vehicles it parked", {
        uid,
        fields: Object.keys(fields),
        candidates: parked.length,
        rewritten,
        failed: failed.length,
    });

    if (failed.length > 0) {
        throw new Error(`Could not rewrite ${failed.length} of ${parked.length} vehicles for ${uid}`);
    }
}

/**
 * Keeps the copies of a person's name and email in step with `users/{uid}`, which is the only one
 * the client writes. Spec 011.
 *
 * Most writes to `users` are reorders and change neither field, so the first check returns early.
 * Safe to run more than once: it writes the same values again. It never writes to `users`, so it
 * cannot trigger itself.
 */
export async function propagateUserProfileHandler(
    event: FirestoreEvent<Change<QueryDocumentSnapshot> | undefined, { uid: string }>
): Promise<void> {
    const uid = event.params.uid;
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    if (before.name === after.name && before.email === after.email) return;

    const change = await profileChange(uid, before, after);
    if (change.name === undefined && change.email === undefined) return;

    await Promise.all([
        change.name !== undefined ? syncAuthDisplayName(uid, change.name) : Promise.resolve(),
        rewriteParkedBy(uid, change),
    ]);
}
