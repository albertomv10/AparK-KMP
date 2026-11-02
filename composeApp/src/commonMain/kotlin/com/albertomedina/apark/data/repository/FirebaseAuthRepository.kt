package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.auth.SignInProvider
import com.albertomedina.apark.domain.repository.AuthRepository
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/** `Code=17009` dentro de la descripcion del NSError. */
private val AUTH_ERROR_CODE = Regex("Code=(\\d+)")

/**
 * Codigos de `AuthErrors.swift` del SDK de iOS que dicen que **la cuenta ya no vale**: no existe,
 * esta deshabilitada, o su sesion se revoco. gitlive tipa el 17021 como `InvalidCredentials`, igual
 * que una contrasena mal escrita, asi que estos se miran por codigo **antes** que por tipo.
 */
private val ACCOUNT_GONE_CODES = setOf(
    17011L, // userNotFound
    17005L, // userDisabled
    17017L, // invalidUserToken
    17021L, // userTokenExpired
)

/** La operacion que fallo. El mismo error de Firebase no significa lo mismo en todas. */
private enum class AuthOperation {
    LOGIN, REGISTER, REAUTHENTICATE, CHANGE_EMAIL, CHANGE_PASSWORD, RELOAD;

    /** Se hace sobre una sesion abierta: si la cuenta ya no vale, es que la sesion termino. */
    val needsSession: Boolean get() = this != LOGIN && this != REGISTER
}

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
                Result.failure(AuthFailure(AuthFailureReason.EMAIL_NOT_VERIFIED))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAuthFailure(AuthOperation.LOGIN))
        }
    }

    /**
     * Traduce la excepción de Firebase a un motivo, aquí y no en la UI: esta es la única capa que
     * conoce el SDK. Se clasifica **por tipo**, nunca por el texto del mensaje, que cambia con el
     * idioma del dispositivo y con la versión del SDK.
     *
     * En Android los tipos bastan, porque son alias del SDK nativo. En iOS no: gitlive traduce el
     * NSError a tipo mirando su codigo, y **no cubre algunos de los mas comunes** —17009 contrasena
     * incorrecta, 17011 usuario inexistente—, que caen en un `FirebaseAuthException` generico. De
     * ahi que cada rama mire tambien el codigo.
     *
     * [operation] desambigua lo que el tipo no dice. En Android un email mal formado llega como
     * `InvalidCredentials`, igual que una contrasena incorrecta, y una sesion revocada llega como
     * `InvalidUser`, igual que una cuenta inexistente al entrar.
     *
     * El detalle se conserva para diagnóstico; quien lo reciba decide qué hacer con él, pero no se
     * enseña.
     */
    private fun Throwable.toAuthFailure(operation: AuthOperation): AuthFailure {
        if (this is AuthFailure) return this
        val code = firebaseAuthErrorCode()
        val reason = when {
            // Al entrar, cuenta inexistente o deshabilitada es lo mismo que credenciales mal para
            // quien lo escribe, y distinguirlo en pantalla revelaria que emails existen.
            this is FirebaseAuthInvalidUserException || code in ACCOUNT_GONE_CODES ->
                if (operation.needsSession) AuthFailureReason.SESSION_EXPIRED
                else AuthFailureReason.INVALID_CREDENTIALS

            // Antes que `InvalidCredentials`, que es su clase madre.
            this is FirebaseAuthWeakPasswordException || code == 17026L -> AuthFailureReason.WEAK_PASSWORD
            this is FirebaseAuthRecentLoginRequiredException || code == 17014L ->
                AuthFailureReason.REQUIRES_RECENT_LOGIN
            this is FirebaseAuthUserCollisionException || code == 17007L -> AuthFailureReason.EMAIL_ALREADY_IN_USE
            code == 17008L -> AuthFailureReason.INVALID_EMAIL // invalidEmail

            this is FirebaseAuthInvalidCredentialsException || code == 17009L -> when (operation) {
                // Aqui no hay contrasena que pueda estar mal: solo el email que se ha escrito.
                AuthOperation.REGISTER, AuthOperation.CHANGE_EMAIL -> AuthFailureReason.INVALID_EMAIL
                AuthOperation.CHANGE_PASSWORD, AuthOperation.RELOAD -> AuthFailureReason.UNKNOWN
                AuthOperation.LOGIN, AuthOperation.REAUTHENTICATE -> AuthFailureReason.INVALID_CREDENTIALS
            }

            this is FirebaseNetworkException || code == 17020L -> AuthFailureReason.NETWORK
            this is FirebaseTooManyRequestsException || code == 17010L -> AuthFailureReason.TOO_MANY_ATTEMPTS
            else -> AuthFailureReason.UNKNOWN
        }
        return AuthFailure(reason, detail = "${this::class.simpleName}: $message")
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

    /**
     * Una operacion sobre la sesion abierta, con su fallo ya traducido.
     *
     * Si Firebase dice que la sesion ya no vale, se cierra **aqui**, que es el unico sitio que se
     * entera. Sin esto la app seguiria con un usuario que el servidor ya no reconoce hasta que el
     * SDK lo descubriera por su cuenta, al renovar el token. Cerrarla hace que `authStateChanges`
     * emita `null`, y la sesion de las pestanas lleva al login (spec 011).
     */
    private suspend fun onCurrentUser(
        operation: AuthOperation,
        block: suspend (FirebaseUser) -> Unit
    ): Result<Unit> = try {
        val user = firebaseAuth.currentUser ?: throw AuthFailure(AuthFailureReason.SESSION_EXPIRED)
        block(user)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val failure = e.toAuthFailure(operation)
        if (failure.reason == AuthFailureReason.SESSION_EXPIRED) logout()
        Result.failure(failure)
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password)

            // Enviamos verificación inmediatamente después de crear
            sendEmailVerification()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAuthFailure(AuthOperation.REGISTER))
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

    override fun signInProvider(): SignInProvider =
        SignInProvider.from(firebaseAuth.currentUser?.providerData?.map { it.providerId }.orEmpty())

    override suspend fun reauthenticate(currentPassword: String): Result<Unit> =
        onCurrentUser(AuthOperation.REAUTHENTICATE) { user ->
            val email = user.email ?: throw AuthFailure(AuthFailureReason.SESSION_EXPIRED)
            user.reauthenticate(EmailAuthProvider.credential(email, currentPassword))
        }

    override suspend fun requestEmailChange(newEmail: String): Result<Unit> =
        onCurrentUser(AuthOperation.CHANGE_EMAIL) { it.verifyBeforeUpdateEmail(newEmail) }

    override suspend fun changePassword(newPassword: String): Result<Unit> =
        onCurrentUser(AuthOperation.CHANGE_PASSWORD) { it.updatePassword(newPassword) }

    override suspend fun reloadUser(): Result<Unit> =
        onCurrentUser(AuthOperation.RELOAD) {
            it.reload()
            // `reload` pone al dia el perfil, pero no el token: sin esto el email de la cuenta y el
            // del token pueden ser distintos, y aparcar firmaria con uno que la regla no acepta.
            // Renovar el token es ademas lo que de verdad descubre una sesion revocada.
            it.getIdToken(forceRefresh = true)
        }

    override suspend fun tokenEmail(): String? = try {
        firebaseAuth.currentUser?.getIdTokenResult(forceRefresh = false)?.claims?.get("email") as? String
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
