import { getFirestore } from "firebase-admin/firestore";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { memberList, MemberEntry, MemberProfile } from "./membersPlan";
import { requireUid } from "./requireUid";

const VEHICLES_COLLECTION = "vehicles";
const USERS_COLLECTION = "users";

export type GetVehicleMembersResult =
    | { status: "ok"; members: MemberEntry[] }
    | { status: "not-member" };

/**
 * The members of a vehicle, by name (spec 014).
 *
 * This lives on the server because nobody may read anybody else's `users/{uid}`: that document
 * carries `userVehicles`, the ids of all their vehicles. The Admin SDK reads them and only a label
 * per member comes back. A `members` map inside the vehicle would avoid the call, but would have to
 * be migrated and kept in sync from six places; members are read rarely and not in real time.
 *
 * Not being a member — the vehicle is gone, or they were just removed — is an expected outcome, so
 * it comes back as a status, the same convention as `joinVehicleWithCode`.
 */
export async function getVehicleMembersHandler(
    request: CallableRequest<{ vehicleId?: string }>
): Promise<GetVehicleMembersResult> {
    const uid = requireUid(request);
    const vehicleId = request.data?.vehicleId;

    if (typeof vehicleId !== "string" || vehicleId.length === 0) {
        throw new HttpsError("invalid-argument", "A vehicleId is required.");
    }

    const firestore = getFirestore();
    const snapshot = await firestore.collection(VEHICLES_COLLECTION).doc(vehicleId).get();
    const vehicle = snapshot.data();
    const memberIds: unknown[] = Array.isArray(vehicle?.memberIds) ? vehicle.memberIds : [];

    if (!snapshot.exists || !memberIds.includes(uid)) {
        return { status: "not-member" };
    }

    const ids = [...new Set(memberIds.filter((id): id is string => typeof id === "string" && id.length > 0))];
    const users = await firestore.getAll(...ids.map((id) => firestore.collection(USERS_COLLECTION).doc(id)));

    const profiles: Record<string, MemberProfile> = {};
    for (const user of users) {
        if (user.exists) profiles[user.id] = user.data() as MemberProfile;
    }

    return { status: "ok", members: memberList(memberIds, vehicle?.ownerId, profiles) };
}
