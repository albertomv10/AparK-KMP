package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // En KMP, signInWithEmailAndPassword ya es una suspend function
            firebaseAuth.signInWithEmailAndPassword(email, password)

            if (isUserEmailVerified()) {
                Result.success(Unit)
            } else {
                val exception = Exception("Email_verification_required")
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password)

            // Enviamos verificación inmediatamente después de crear
            sendEmailVerification()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun isUserEmailVerified(): Boolean {
        return firebaseAuth.currentUser?.isEmailVerified ?: false
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Lanzamos una corrutina en segundo plano o asumimos que se llama desde contexto suspendido.
                // Nota: En GitLive sendEmailVerification es suspend.
                // Si tu interfaz no es suspend, esto es un pequeño problema de diseño heredado.
                // Como solución temporal para KMP si la interfaz no es suspend:
                // No podemos llamar a funciones suspend aquí sin un scope.
                // RECOMENDACIÓN: Cambiar sendEmailVerification a 'suspend' en la interfaz.
                // Por ahora, asumiremos que cambiarás la interfaz o usaremos un hack (no recomendado).

                // Opción A (Correcta): Cambia la interfaz a 'suspend fun sendEmailVerification()'
                // Opción B (Si no cambias interfaz): No podemos ejecutarlo aquí.

                // *Asumo que cambiarás la interfaz a suspend, mira la nota abajo*
                user.sendEmailVerification()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun getUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override suspend fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            println("Error al cerrar sesión: ${e.message}")
        }
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        accessToken: String?
    ): Result<FirebaseUser?> {
        return try {

            val credential = GoogleAuthProvider.credential(idToken, accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential)

            println("AuthRepository: Usuario autenticado con Google")
            Result.success(authResult.user)
        } catch (e: Exception) {
            println("AuthRepository: Error al autenticar con Google: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun loginWithApple(
        idToken: String,
        nonce: String
    ): Result<FirebaseUser?> {
        return try {
            // Usamos el companion object directamente con sus parámetros correctos
            val credential = OAuthProvider.credential(
                providerId = "apple.com",
                idToken = idToken,
                rawNonce = nonce
            )
            val authResult = firebaseAuth.signInWithCredential(credential)
            Result.success(authResult.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email)
            println("AuthRepository: Email de restablecimiento enviado a: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            println("AuthRepository: Error al enviar email: ${e.message}")
            Result.failure(e)
        }
    }
}