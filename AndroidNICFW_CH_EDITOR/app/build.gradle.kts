import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Load signing properties from local.properties (never committed to VCS)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun String?.forBuildConfig(): String =
    (this ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

val repeaterBookToken = localProps.getProperty("REPEATERBOOK_APP_TOKEN", "").forBuildConfig()
val repeaterBookEmail = localProps.getProperty("REPEATERBOOK_CONTACT_EMAIL", "").forBuildConfig()
val repeaterBookUrl = localProps.getProperty("REPEATERBOOK_APP_URL", "").forBuildConfig()

android {
    namespace = "com.nicfw.tdh3editor"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.nicfw.tdh3editor"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.2.0"
        buildConfigField("String", "REPEATERBOOK_APP_TOKEN", "\"$repeaterBookToken\"")
        buildConfigField("String", "REPEATERBOOK_CONTACT_EMAIL", "\"$repeaterBookEmail\"")
        buildConfigField("String", "REPEATERBOOK_APP_URL", "\"$repeaterBookUrl\"")
    }

    signingConfigs {
        create("release") {
            val storeFile = localProps.getProperty("RELEASE_STORE_FILE")
            val storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
            val keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
            val keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
                this.storeFile = file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.json)
    testImplementation(libs.junit)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.coordinatorlayout)
    implementation(libs.recyclerview)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
}
