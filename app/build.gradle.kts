import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}
val hfApiKey: String by lazy {
    val props = Properties()

    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        props.load(localProperties.inputStream())
        props.getProperty("HF_API_KEY") ?: error("HF_API_KEY not found in local.properties")
    } else {
        error("local.properties file not found")
    }
}

println("Loaded HF_API_KEY: $hfApiKey")
val OPENAI_API_KEY: String by project

android {
    namespace = "com.example.theaterapp"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.theaterapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_API_KEY", "\"$OPENAI_API_KEY\"")
        buildConfigField("String", "HF_API_KEY", "\"$hfApiKey\"")
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
