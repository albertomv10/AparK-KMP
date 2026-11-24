/**
 * The decisions behind `deleteAccount`, with no imports on purpose.
 *
 * `tools/test/account.test.ts` tests this file, and that job installs only `tools/`, not
 * `functions/`: anything imported here has to resolve without `functions/node_modules`. Keeping the
 * pure logic apart from the handler is what makes that true. Firestore, Auth and logging stay in
 * `account.ts`.
 */

/** A Firestore document's data, as `firebase-admin` hands it over (`DocumentData`). */
export type VehicleData = { [field: string]: any };

/**
 * How recent the sign-in has to be. The client re-authenticates right before calling, so this only
 * has to cover the round trip; it exists so the server does not take the client's word for it.
 */
export const RECENT_LOGIN_SECONDS = 5 * 60;

export function isRecentLogin(authTimeSeconds: unknown, nowSeconds: number): boolean {
    return typeof authTimeSeconds === "number" && nowSeconds - authTimeSeconds <= RECENT_LOGIN_SECONDS;
}

/**
 * Whether `lastLocation.user` names this person. Since spec 008 it is `{uid, name, email}`; vehicles
 * nobody has parked since then still carry the whole old `User`, keyed by `id`.
 */
function isParkedBy(vehicle: VehicleData, uid: string): boolean {
    const parkedBy = vehicle.lastLocation?.user;
    return !!parkedBy && (parkedBy.uid === uid || parkedBy.id === uid);
}

export type VehiclePlan =
    | { action: "none" }
    /** Not a member, but their name and email are still on the card. */
    | { action: "anonymize" }
    /** Nobody else is left: the vehicle goes, and `cleanupVehicleReferences` sweeps up after it. */
    | { action: "delete" }
    /** Others remain: drop this person, hand over ownership if it was theirs. */
    | { action: "leave"; memberIds: string[]; newOwnerId?: string; anonymize: boolean };

/**
 * What deleting `uid` means for one vehicle.
 *
 * Ownership goes to the member who has been there longest — the first of `memberIds` once this
 * person is out. Creating a vehicle writes `[owner]`, joining appends with `arrayUnion`, and the
 * spec 008 migration wrote `[owner, ...sharedUsers]`, so the array's order is joining order.
 */
export function planVehicle(vehicle: VehicleData, uid: string): VehiclePlan {
    const members: string[] = Array.isArray(vehicle.memberIds) ? vehicle.memberIds : [];
    const isOwner = vehicle.ownerId === uid;
    const parked = isParkedBy(vehicle, uid);

    if (!members.includes(uid) && !isOwner) {
        return parked ? { action: "anonymize" } : { action: "none" };
    }

    const others = members.filter((member) => member !== uid);
    if (others.length === 0) {
        return { action: "delete" };
    }
    return { action: "leave", memberIds: others, newOwnerId: isOwner ? others[0] : undefined, anonymize: parked };
}
