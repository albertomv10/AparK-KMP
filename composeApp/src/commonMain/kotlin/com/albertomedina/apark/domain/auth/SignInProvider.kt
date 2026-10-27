package com.albertomedina.apark.domain.auth

/**
 * Con qué entró la persona, que decide lo que puede cambiar desde la app: quien entró con Google o
 * Apple tiene el email y la contraseña en manos de ese proveedor.
 */
enum class SignInProvider {
    PASSWORD,
    GOOGLE,
    APPLE;

    /** Solo una cuenta con contraseña puede cambiar desde aquí su email y su contraseña. */
    val canChangeCredentials: Boolean get() = this == PASSWORD

    companion object {
        /**
         * A partir de los `providerId` de Firebase. Si está `password`, manda: aunque la cuenta
         * tuviera también Google enlazado, tiene una contraseña que se puede cambiar. Lo desconocido
         * cae en [PASSWORD] solo si no hay nada mejor, para no esconder opciones por un proveedor
         * que la app no conoce.
         */
        fun from(providerIds: List<String>): SignInProvider = when {
            "password" in providerIds -> PASSWORD
            "google.com" in providerIds -> GOOGLE
            "apple.com" in providerIds -> APPLE
            else -> PASSWORD
        }
    }
}
