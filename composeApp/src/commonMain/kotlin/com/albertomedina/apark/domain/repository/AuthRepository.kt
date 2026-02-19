package com.albertomedina.apark.domain.repository

import dev.gitlive.firebase.auth.FirebaseUser


interface AuthRepository {
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
    fun sendEmailVerification(): Result<Unit>

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
    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser?>

    /**
     * Attempts to reset the password of a given email address.
     * @return A Result containing Unit on success, or an error on failure.
     */
    suspend fun resetPassword(email: String): Result<Unit>
}