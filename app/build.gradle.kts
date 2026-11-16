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
        versionCode = 2
        versionName = "0.2.1-oauth"

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

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
