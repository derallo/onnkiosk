import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProps = Properties().apply {
    val content = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
    if (!content.isNullOrBlank()) {
        load(StringReader(content))
    }
}

fun quoteBuildConfigString(value: String?): String {
    val clean = (value ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$clean\""
}

android {
    namespace = "com.nernal.frame"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nernal.frame.oauth"
        minSdk = 33
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1-oauth"

        buildConfigField(
            "String",
            "DEFAULT_OAUTH_CLIENT_ID",
            quoteBuildConfigString(localProps.getProperty("oauth.clientId")),
        )
        buildConfigField(
            "String",
            "DEFAULT_OAUTH_CLIENT_SECRET",
            quoteBuildConfigString(localProps.getProperty("oauth.clientSecret")),
        )
        buildConfigField("boolean", "IS_NERNAL_FAMILY_BUILD", "false")
        buildConfigField("String", "APP_DISPLAY_NAME", "\"Onnkiosk\"")
        buildConfigField("String", "AUTH_SCHEME", "\"onnkiosk\"")
        buildConfigField("String", "PREFS_NAME", "\"nernal_oauth_frame_prefs\"")
        buildConfigField("String", "LIBRARY_CACHE_FILE", "\"oauth_album_library_cache.json\"")
        buildConfigField("String", "REMINDERS_PREF_KEY", "\"reminders_v3_oauth\"")
        buildConfigField("String", "PENDING_REM_OPS_KEY", "\"pending_rem_ops_v2_oauth\"")
        buildConfigField("boolean", "DEFAULT_PI_SYNC_ENABLED", "false")
        buildConfigField("String", "DEFAULT_LOCATION_NAME", "\"WASHINGTON, DC\"")
        buildConfigField("String", "DEFAULT_LAT", "\"38.8951\"")
        buildConfigField("String", "DEFAULT_LON", "\"-77.0364\"")
        buildConfigField("String", "DEFAULT_ALBUM_URLS_CSV", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
