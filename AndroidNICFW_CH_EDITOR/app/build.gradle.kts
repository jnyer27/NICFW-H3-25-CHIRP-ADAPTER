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

fun rejectRepeaterBookCredential(property: String, value: String?) {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return
    val token = Regex("(?i)(?<![A-Za-z0-9])(?:rbuapp_|app_)[A-Za-z0-9_\\-]{8,}")
    if (token.containsMatchIn(text)) {
        throw GradleException(
            "$property contains a RepeaterBook token. Remove it from local.properties. " +
                "This app does not embed credentials. Users enter a personal token at runtime.",
        )
    }
}

val embeddedToken = localProps.getProperty("REPEATERBOOK_APP_TOKEN")?.trim().orEmpty()
if (embeddedToken.isNotEmpty()) {
    throw GradleException(
        "REPEATERBOOK_APP_TOKEN is set in local.properties. Do not compile a RepeaterBook credential into the app. " +
            "Delete that property. Each user enters a personal rbuapp_ token in the app.",
    )
}
val legacyAuthMode = localProps.getProperty("REPEATERBOOK_AUTH_MODE")?.trim().orEmpty()
if (legacyAuthMode.isNotEmpty()) {
    throw GradleException(
        "REPEATERBOOK_AUTH_MODE is no longer supported, including query-string authentication. " +
            "Remove it from local.properties. Export requests send the X-RB-App-Token header only.",
    )
}

val repeaterBookEmail = localProps.getProperty("REPEATERBOOK_CONTACT_EMAIL", "")
val repeaterBookUrl = localProps.getProperty("REPEATERBOOK_APP_URL", "")
/** If set, sent as User-Agent verbatim. This is the approved public identity, not a credential. */
val repeaterBookUserAgent = localProps.getProperty("REPEATERBOOK_USER_AGENT", "")
rejectRepeaterBookCredential("REPEATERBOOK_CONTACT_EMAIL", repeaterBookEmail)
rejectRepeaterBookCredential("REPEATERBOOK_APP_URL", repeaterBookUrl)
rejectRepeaterBookCredential("REPEATERBOOK_USER_AGENT", repeaterBookUserAgent)

android {
    namespace = "com.nicfw.tdh3editor"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.nicfw.tdh3editor"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 10
        versionName = "2.4.1"
        buildConfigField("String", "REPEATERBOOK_CONTACT_EMAIL", "\"${repeaterBookEmail.forBuildConfig()}\"")
        buildConfigField("String", "REPEATERBOOK_APP_URL", "\"${repeaterBookUrl.forBuildConfig()}\"")
        buildConfigField("String", "REPEATERBOOK_USER_AGENT", "\"${repeaterBookUserAgent.forBuildConfig()}\"")
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
    implementation(libs.jsoup)
    implementation(libs.security.crypto)
}

