import java.util.Properties
import java.io.FileInputStream
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
// Play Publisher plugin removed (temporarily) // plugin block commented out
  // Play Publisher plugin for Google Play automation
// id("com.github.triplet.play") version "3.10.0" // moved to apply later
}

// apply(from = "publish.gradle.kts")  // removed: file not present


android {
  namespace = "com.aistudio.driverrecorder.gpxrt"
  compileSdk = 35

  // Load signing secrets from key.properties (not committed to git)
  val keyPropertiesFile = rootProject.file("key.properties")
  val keyProperties = Properties()
  if (keyPropertiesFile.exists()) {
    keyProperties.load(FileInputStream(keyPropertiesFile))
  }

  defaultConfig {
    applicationId = "com.aistudio.driverrecorder.gpxrt"
    minSdk = 24
    targetSdk = 35          // Play Store requires targetSdk 35+ as of Aug 2025
    versionCode = 4
    versionName = "2.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // Prefer key.properties file; fall back to env vars for CI
      storeFile = if (keyPropertiesFile.exists()) {
        file(keyProperties["storeFile"] as? String ?: "${rootDir}/my-upload-key.jks")
      } else {
        file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
      }
      storePassword = (keyProperties["storePassword"] as? String)
        ?: System.getenv("STORE_PASSWORD")
      keyAlias = (keyProperties["keyAlias"] as? String) ?: "upload"
      keyPassword = (keyProperties["keyPassword"] as? String)
        ?: System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Play Publisher configuration removed (temporarily) // play block disabled

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.video)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation("com.android.billingclient:billing-ktx:7.1.1")
  implementation(libs.play.services.ads)
  implementation(libs.retrofit)
  implementation("com.google.guava:guava:31.1-android")
  implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register<Zip>("zipApk") {
    dependsOn(":app:assembleDebug")
    archiveFileName.set("DefenderApp-ReadMyRights.zip")
    destinationDirectory.set(file("${rootDir}"))
    from("${projectDir}/build/outputs/apk/debug") {
        include("app-debug.apk")
    }
}




