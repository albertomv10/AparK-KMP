package com.albertomedina.apark.data.logging

import com.albertomedina.apark.domain.logging.CrashReporter
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

class FirebaseCrashReporter : CrashReporter {

    private val crashlytics = Firebase.crashlytics

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordError(error: Throwable, context: String?) {
        if (context != null) crashlytics.log(context)
        crashlytics.recordException(error)
    }

    override fun recordFailure(context: String, detail: String?) {
        // Crashlytics agrupa los non-fatal por tipo y mensaje de la excepción. Como aquí no hay
        // excepción real, se fabrica una con el contexto por mensaje: así los informes del mismo
        // problema caen juntos en vez de dispersarse por el detalle, que varía.
        if (detail != null) crashlytics.log(detail)
        crashlytics.recordException(ReportedFailure(context))
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId ?: "")
    }
}

/** Marca los informes que no vienen de una excepción real, para no confundirlos con un crash. */
private class ReportedFailure(message: String) : Exception(message)
