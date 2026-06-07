# --- Reglas Generales de Apark ---

# Mantener los modelos de dominio para que la serialización funcione correctamente
-keep class com.albertomedina.apark.domain.model.** { *; }
-keep class com.albertomedina.apark.domain.model.**$$serializer { *; }
-keepclassmembers class com.albertomedina.apark.domain.model.** {
    *** Companion;
}

# --- Koin (Inyección de dependencias) ---
-keep class org.koin.** { *; }
-keep class com.albertomedina.apark.di.** { *; }

# --- Firebase & Google Play Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class dev.gitlive.firebase.** { *; }

# --- Corrutinas ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# --- Compose Multiplatform Resources ---
-keep class apark.composeapp.generated.resources.** { *; }
