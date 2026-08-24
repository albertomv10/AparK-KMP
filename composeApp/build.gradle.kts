import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Las credenciales de firma se leen de fuera del repositorio: primero de `local.properties`, que
// esta gitignorado, y si no de variables de entorno, que es como las recibe CI. Nunca se commitean.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingSecret(name: String): String? =
    (localProperties.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingSecret("RELEASE_STORE_FILE")?.let(::file)?.takeIf { it.exists() }
val releaseStorePassword = signingSecret("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingSecret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingSecret("RELEASE_KEY_PASSWORD")

// Si no hay credenciales, la build de release no se firma en lugar de fallar: compilar el proyecto
// tiene que seguir funcionando en cualquier maquina, incluida la de alguien que solo quiere ver si
// arranca. Pero una configuracion *a medias* casi siempre es un descuido, y ahi si conviene gritar.
val releaseSigningReady = releaseStoreFile != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null

if (!releaseSigningReady && signingSecret("RELEASE_STORE_FILE") != null) {
    logger.warn(
        "AVISO: hay credenciales de firma a medias, asi que la build de release saldra SIN FIRMAR. " +
            "Revisa RELEASE_STORE_FILE (y que el fichero exista), RELEASE_STORE_PASSWORD, " +
            "RELEASE_KEY_ALIAS y RELEASE_KEY_PASSWORD."
    )
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.secretsGradlePlugin)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            linkerOpts.add("-lsqlite3")
            binaryOption("bundleId", "com.albertomedina.apark.shared")
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.splash.screen)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)

            implementation(libs.koin.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            implementation(libs.maps.compose)
            implementation(libs.maps.compose.utils)

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            // Common BackHandler (androidx.compose.ui.backhandler); not pulled in by compose.ui
            implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.composeMultiplatform.get()}")
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation(compose.materialIconsExtended)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Navigation 3 (KMP)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)

            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.crashlytics)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences.core)

            implementation(libs.compottie)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.albertomedina.apark"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.albertomedina.apark"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.app.version.code.get().toInt()
        versionName = libs.versions.app.version.name.get()
    }
    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            // Nulo cuando no hay credenciales: AGP marca la salida como `-unsigned`, que es una
            // senal mucho mas clara que un artefacto firmado con la clave de depuracion.
            signingConfig = if (releaseSigningReady) signingConfigs.getByName("release") else null
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    debugImplementation(compose.uiTooling)
}
