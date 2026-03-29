# --- Reglas para Koin (Inyección de dependencias) ---
-keep class org.koin.** { *; }

# --- Reglas para Firebase / Google Play Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- Reglas para Corrutinas ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Si usas Kotlinx Serialization para llamadas a red/JSON ---
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.albertomedina.apark.domain.model.**$$serializer { *; }
-keepclassmembers class com.albertomedina.apark.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.albertomedina.apark.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}