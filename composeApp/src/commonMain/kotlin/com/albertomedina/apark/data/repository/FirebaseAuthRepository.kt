package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.auth.EmailLoginFailure
import com.albertomedina.apark.domain.auth.EmailLoginReason
import com.albertomedina.apark.domain.repository.AuthRepository
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.Flow

/** `Code=17009` dentro de la descripcion del NSError. */
private val AUTH_ERROR_CODE = Regex("Code=(\\d+)")

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {
    override val authStateChanges: Flow<FirebaseUser?> = firebaseAuth.authStateChanged

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // En KMP, signInWithEmailAndPassword ya es una suspend function
            firebaseAuth.signInWithEmailAndPassword(email, password)

            if (isUserEmailVerified()) {
                Result.success(Unit)
            } else {
                Result.failure(EmailLoginFailure(EmailLoginReason.EMAIL_NOT_VERIFIED))
            }
        } catch (e: Exception) {
            Result.failure(e.toEmailLoginFailure())
        }
    }

    /**
     * Traduce la excepción de Firebase a un motivo, aquí y no en la UI: esta es la única capa que
     * conoce el SDK. Se clasifica **por tipo**, nunca por el texto del mensaje, que cambia con el
     * idioma del dispositivo y con la versión del SDK.
     *
     * El detalle se conserva para diagnóstico; quien lo reciba decide qué hacer con él, pero no se
     * enseña.
     */
    private fun Throwable.toEmailLoginFailure(): EmailLoginFailure {
        if (this is EmailLoginFailure) return this
        val reason = when (this) {
            // Credenciales mal, usuario inexistente o cuenta deshabilitada: para quien lo escribe
            // son el mismo problema, y distinguirlos en pantalla además revelaría qué emails
            // existen.
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> EmailLoginReason.INVALID_CREDENTIALS
            is FirebaseNetworkException -> EmailLoginReason.NETWORK
            is FirebaseTooManyRequestsException -> EmailLoginReason.TOO_MANY_ATTEMPTS
            // En Android los tipos de arriba son alias del SDK nativo y bastan. En iOS no: gitlive
            // traduce el NSError a tipo mirando su codigo, y **no cubre los dos mas comunes de un
            // login** -17009 contrasena incorrecta y 17011 usuario inexistente-, de modo que caen
            // en un `FirebaseAuthException` generico. De ahi el segundo intento.
            else -> firebaseAuthErrorCode()?.toLoginReason() ?: EmailLoginReason.UNKNOWN
        }
        return EmailLoginFailure(reason, detail = "${this::class.simpleName}: $message")
    }

    /**
     * El codigo de error de Firebase que viene dentro del mensaje del `NSError` de iOS.
     *
     * Esto **no** es volver a decidir por el texto. Lo que se busca es un numero que forma parte
     * del contrato de la API, igual en todos los idiomas y estable entre versiones; lo que se hacia
     * antes era buscar la palabra "credential" en una frase escrita para que la lea una persona, y
     * eso se rompe con cambiar el idioma del dispositivo.
     */
    private fun Throwable.firebaseAuthErrorCode(): Long? =
        message?.let { AUTH_ERROR_CODE.find(it)?.groupValues?.get(1)?.toLongOrNull() }

    /** Codigos de `AuthErrors.swift` del SDK de iOS. Solo los que cambian lo que ve el usuario. */
    private fun Long.toLoginReason(): EmailLoginReason? = when (this) {
        17009L, // wrongPassword
        17011L, // userNotFound
        17005L, // userDisabled
        -> EmailLoginReason.INVALID_CREDENTIALS
        17010L -> EmailLoginReason.TOO_MANY_ATTEMPTS // tooManyRequests
        17020L -> EmailLoginReason.NETWORK // networkError
        else -> null
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