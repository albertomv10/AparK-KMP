package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.auth.SignInProvider
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow


interface AuthRepository {
    val authStateChanges: Flow<FirebaseUser?>
    /**
     * Attempts to log in a user with the provided email and password.
     * @param email The user's email address.
     * @param password The user's password.
     * @return A Result containing Unit on success, or an error on failure.
     */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Attempts to register a new user with the provided email and password.
     * @param email The user's email address.
     * @param password The user's password.
     * @return A Result containing Unit on success, or an error on failure.
     */
    suspend fun register(email: String, password: String): Result<Unit>

    /**
     * Checks if a user is currently logged in.
     * @return True if the user is logged in, false otherwise.
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Checks if the currently logged-in user's email is verified.
     * @return True if the user's email is verified, false otherwise.
     */
    fun isUserEmailVerified(): Boolean

    /**
     * Sends a verification email to the currently logged-in user.
     * This should be called after registration to ensure the user verifies their email.
     */
    suspend fun sendEmailVerification(): Result<Unit>

    /**
     * Retrieves the currently logged-in user.
     * @return The FirebaseUser object if logged in, null otherwise.
     */
    fun getCurrentUser(): FirebaseUser?

    /**
     * Retrieves the email of the currently logged-in user.
     * @return The user's email if logged in, null otherwise.
     */
    fun getUserEmail(): String?

    /**
     * Logs out the currently logged-in user.
     */
    suspend fun logout()

    /**
     * Attempts to log in a user using Google authentication.
     * @return A Result containing Unit on success, or an error on failure.
     */
    suspend fun loginWithGoogle(idToken: String, accessToken: String? = null): Result<FirebaseUser?>

    suspend fun loginWithApple(idToken: String, nonce: String): Result<FirebaseUser?>

    /**
     * Attempts to reset the password of a given email address.
     * @return A Result containing Unit on success, or an error on failure.
     */
    suspend fun resetPassword(email: String): Result<Unit>

    /** How the current user signed in, which decides what they can change from the app. */
    fun signInProvider(): SignInProvider

    /**
     * Proves again that the person holding the phone knows the password. Firebase demands a recent
     * sign-in before changing the email or the password — and, in 1.4, before deleting the account.
     * Fails with [com.albertomedina.apark.domain.auth.AuthFailureReason.INVALID_CREDENTIALS] when
     * the password is wrong.
     */
    suspend fun reauthenticate(currentPassword: String): Result<Unit>

    /**
     * Sends a verification link to [newEmail]. The sign-in email **does not change** until that
     * link is opened, and opening it ends every session of the account.
     */
    suspend fun requestEmailChange(newEmail: String): Result<Unit>

    /** Changes the password. This device keeps its session; the others lose theirs. */
    suspend fun changePassword(newPassword: String): Result<Unit>

    /**
     * Refreshes the current user from the server, **and its ID token with it**, so that
     * [getUserEmail] and [tokenEmail] cannot disagree afterwards. A revoked session fails with
     * [com.albertomedina.apark.domain.auth.AuthFailureReason.SESSION_EXPIRED].
     */
    suspend fun reloadUser(): Result<Unit>

    /**
     * The email inside the current ID token — exactly what the security rules see as
     * `request.auth.token.email`. It can differ from [getUserEmail] for a while after the email
     * changes, because refreshing the profile does not refresh the token.
     */
    suspend fun tokenEmail(): String?
}