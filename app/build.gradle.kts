import java.util.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

// Llave de la cuenta de servicio con rol "Firebase Cloud Messaging API Admin" (T17).
// No se sube a git (ver app/.gitignore). Sin el archivo la app compila y funciona, pero no envía pushes.
val fcmKeyFile = file("fcm-service-account.json")
val fcmKeyBase64 = if (fcmKeyFile.exists()) Base64.getEncoder().encodeToString(fcmKeyFile.readBytes()) else ""

android {
    namespace = "com.example.chat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.chat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // En Base64 para no tener que escapar las comillas y saltos de línea del JSON
        buildConfigField("String", "FCM_SERVICE_ACCOUNT", "\"$fcmKeyBase64\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        // Metadatos repetidos en los jars de google-auth; no se usan en Android
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services) // .await() sobre las Task de Firebase
    implementation(libs.google.auth) // firma las llamadas a la API de FCM con la cuenta de servicio (T17)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
