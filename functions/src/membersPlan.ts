/**
 * What `getVehicleMembers` (spec 014) hands back, with no imports on purpose: `tools/test/` tests
 * this file, and that CI job installs `tools/` alone. See the note in `AGENTS.md`.
 */

/** The fields of `users/{uid}` this needs. Untyped on purpose: it is whatever the document holds. */
export type MemberProfile = { name?: unknown; email?: unknown };

export type MemberEntry = { uid: string; displayName: string; isOwner: boolean };

/**
 * How a member is called in front of the others: their name, or their email if they never set one,
 * which is what the vehicle card already does with «Aparcado por». Signing up only asks for an email
 * and a password, so plenty of accounts have no name. `""` when there is nothing at all — an account
 * deleted from the console leaves its uid behind with no profile — and the app shows a placeholder.
 */
export function displayNameOf(profile: MemberProfile | undefined): string {
    const name = typeof profile?.name === "string" ? profile.name.trim() : "";
    if (name.length > 0) return name;
    return typeof profile?.email === "string" ? profile.email.trim() : "";
}

/**
 * The members, owner first and the rest in the order they joined — `memberIds` is in joining order,
 * see `accountPlan.ts`. Only a label leaves the server, never the name and the email together.
 */
export function memberList(
    memberIds: unknown,
    ownerId: unknown,
    profiles: Record<string, MemberProfile | undefined>
): MemberEntry[] {
    const ids = Array.isArray(memberIds)
        ? memberIds.filter((id): id is string => typeof id === "string" && id.length > 0)
        : [];
    const entries = [...new Set(ids)].map((uid) => ({
        uid,
        displayName: displayNameOf(profiles[uid]),
        isOwner: uid === ownerId,
    }));
    return [...entries.filter((entry) => entry.isOwner), ...entries.filter((entry) => !entry.isOwner)];
}
