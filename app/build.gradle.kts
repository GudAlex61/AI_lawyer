import java.util.Properties
import org.gradle.api.Project

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            buildConfigString(getLocalProperty(project, "OPENROUTER_API_KEY"))
        )

        buildConfigField(
            "String",
            "S3_ENABLED",
            buildConfigString(getLocalProperty(project, "S3_ENABLED", "1"))
        )

        buildConfigField(
            "String",
            "S3_ENDPOINT_URL",
            buildConfigString(getLocalProperty(project, "S3_ENDPOINT_URL"))
        )

        buildConfigField(
            "String",
            "S3_REGION",
            buildConfigString(getLocalProperty(project, "S3_REGION", "us-east-005"))
        )

        buildConfigField(
            "String",
            "S3_ACCESS_KEY",
            buildConfigString(getLocalProperty(project, "S3_ACCESS_KEY"))
        )

        buildConfigField(
            "String",
            "S3_SECRET_KEY",
            buildConfigString(getLocalProperty(project, "S3_SECRET_KEY"))
        )

        buildConfigField(
            "String",
            "S3_BUCKET",
            buildConfigString(getLocalProperty(project, "S3_BUCKET"))
        )

        buildConfigField(
            "String",
            "S3_PRESIGNED_EXPIRATION",
            buildConfigString(getLocalProperty(project, "S3_PRESIGNED_EXPIRATION", "600"))
        )

        buildConfigField(
            "String",
            "S3_AUTO_DELETE_AFTER",
            buildConfigString(getLocalProperty(project, "S3_AUTO_DELETE_AFTER", "0"))
        )

        buildConfigField(
            "String",
            "IMAGE_MAX_SIZE",
            buildConfigString(getLocalProperty(project, "IMAGE_MAX_SIZE", "1600"))
        )

        buildConfigField(
            "String",
            "IMAGE_QUALITY",
            buildConfigString(getLocalProperty(project, "IMAGE_QUALITY", "90"))
        )

        buildConfigField(
            "String",
            "OPENROUTER_MODEL",
            buildConfigString(
                getLocalProperty(
                    project,
                    "OPENROUTER_MODEL",
                    "openai/gpt-4o-mini"
                )
            )
        )

        buildConfigField(
            "String",
            "OPENROUTER_VISION_MODEL",
            buildConfigString(
                getLocalProperty(
                    project,
                    "OPENROUTER_VISION_MODEL",
                    "google/gemini-flash-1.5"
                )
            )
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

fun getLocalProperty(project: Project, key: String, defaultValue: String = ""): String {
    val propertiesFile = project.rootProject.file("local.properties")

    if (!propertiesFile.exists()) {
        return defaultValue
    }

    val properties = Properties()
    propertiesFile.inputStream().use { inputStream ->
        properties.load(inputStream)
    }

    return properties.getProperty(key, defaultValue)
}

fun buildConfigString(value: String): String {
    val escapedValue = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    return "\"$escapedValue\""
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}