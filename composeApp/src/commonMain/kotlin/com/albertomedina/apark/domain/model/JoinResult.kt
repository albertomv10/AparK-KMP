package com.albertomedina.apark.domain.model

/**
 * Outcome of redeeming an invitation code.
 *
 * The server reports these as a status rather than an error, so the app can word each case in the
 * user's language instead of surfacing a platform exception.
 */
enum class JoinStatus {
    OK,
    INVALID,
    USED,
    EXPIRED,
    ALREADY_MEMBER,
    UNKNOWN;

    companion object {
        fun from(raw: String): JoinStatus = when (raw) {
            "ok" -> OK
            "invalid" -> INVALID
            "used" -> USED
            "expired" -> EXPIRED
            "already_member" -> ALREADY_MEMBER
            else -> UNKNOWN
        }
    }
}

data class JoinResult(
    val status: JoinStatus,
    val vehicleName: String = ""
)
