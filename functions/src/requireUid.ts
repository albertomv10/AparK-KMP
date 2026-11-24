import { CallableRequest, HttpsError } from "firebase-functions/v2/https";

/**
 * The caller's uid, or `unauthenticated`. Every callable here reveals or changes something that
 * belongs to someone, so none of them has anything to say to an anonymous request.
 */
export function requireUid(request: CallableRequest): string {
    const uid = request.auth?.uid;
    if (!uid) {
        throw new HttpsError("unauthenticated", "You must be signed in.");
    }
    return uid;
}
