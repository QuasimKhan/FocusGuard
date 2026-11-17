plugins {
    id("com.android.application") version "8.7.2"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

fun releaseValue(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseValue("RELEASE_STORE_FILE")
val releaseStorePassword = releaseValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseValue("RELEASE_KEY_PASSWORD")
val releaseSigningValues = linkedMapOf(
    "RELEASE_STORE_FILE" to releaseStoreFile,
    "RELEASE_STORE_PASSWORD" to releaseStorePassword,
    "RELEASE_KEY_ALIAS" to releaseKeyAlias,
    "RELEASE_KEY_PASSWORD" to releaseKeyPassword
)
val missingReleaseSigningValues = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys
val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':').contains("release", ignoreCase = true)
}
if (releaseBuildRequested && missingReleaseSigningValues.isNotEmpty()) {
    throw GradleException(
        "Release signing configuration is missing: ${missingReleaseSigningValues.joinToString()}. " +
            "Supply these as Gradle properties or environment variables; debug builds do not require them."
    )
}
if (releaseBuildRequested && releaseStoreFile != null && !rootProject.file(releaseStoreFile).isFile) {
    throw GradleException("Release keystore file configured by RELEASE_STORE_FILE does not exist.")
}

android {
    namespace = "com.focusguard.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.focusguard.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10400
        versionName = "1.5.0"
    }

    signingConfigs {
        create("release") {
            if (missingReleaseSigningValues.isEmpty()) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
