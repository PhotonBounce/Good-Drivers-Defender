import java.util.Properties
import java.io.FileInputStream
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

// apply(from = "publish.gradle.kts")  // removed: file not present


android {
  namespace = "com.aistudio.driverrecorder.gpxrt"
  compileSdk = 36

  // Load signing secrets from key.properties (not committed to git)
  val keyPropertiesFile = rootProject.file("key.properties")
  val keyProperties = Properties()
  if (keyPropertiesFile.exists()) {
    keyProperties.load(FileInputStream(keyPropertiesFile))
  }

  defaultConfig {
    applicationId = "com.aistudio.driverrecorder.gpxrt"
    minSdk = 24
    targetSdk = 36          // Play Store requires targetSdk 36+ (Android 16) — enforced from Aug 30, 2026
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
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8 code + resource shrinking for a smaller release AAB. Keep rules for the
      // serialization/reflection libraries live in proguard-rules.pro. NOTE: smoke-test
      // a release build on a device (internal testing track) before production rollout.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Only assign signing config when a keystore is actually available (CI or local).
      // Note: CI surfaces signing secrets as job-level env, so an *unset* secret arrives as
      // an empty string (not null) — isNullOrEmpty() treats that as "no keystore" so the
      // release still builds UNSIGNED (and validates R8) instead of failing validateSigningRelease.
      val hasKeystore = keyPropertiesFile.exists() || !System.getenv("STORE_PASSWORD").isNullOrEmpty()
      if (hasKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      // Use AGP's auto-managed debug signing key — no external keystore required
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Replaces the deprecated android.kotlinOptions {} block (removal slated for AGP 9 / Kotlin 2.3)
kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
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
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.play.services.location)
  implementation("com.android.billingclient:billing-ktx:9.1.0")
  // NOTE: the Retrofit/OkHttp/Moshi/Firebase/Guava stack was removed — a repo-wide
  // audit found zero usages (the app has no network code by design), and the
  // accompanying proguard keep rules were pinning the unused libraries into every
  // release build. CameraX's ListenableFuture comes transitively (listenablefuture:1.0);
  // if Guava is ever re-added, remember it needs the 9999.0-empty listenablefuture
  // exclusion trick again.
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
}

tasks.register<Zip>("zipApk") {
    dependsOn(":app:assembleDebug")
    archiveFileName.set("DefenderApp-ReadMyRights.zip")
    destinationDirectory.set(file("${rootDir}"))
    from("${projectDir}/build/outputs/apk/debug") {
        include("app-debug.apk")
    }
}




