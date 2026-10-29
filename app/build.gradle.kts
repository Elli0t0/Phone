plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

val versionMajor = (project.findProperty("VERSION_MAJOR") as? String)?.toInt() ?: 0
val versionMinor = (project.findProperty("VERSION_MINOR") as? String)?.toInt() ?: 1
val versionPatch = (project.findProperty("VERSION_PATCH") as? String)?.toInt() ?: 0
val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch

android {
  namespace = "io.github.elli0t0.phone"
  compileSdk = 37

  defaultConfig {
    applicationId = "io.github.elli0t0.phone"
    minSdk = 29
    targetSdk = 35
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Native libraries exist for four ABIs (graphics.path from Compose
    // and DataStore's shared counter); ship arm64-v8a only, which
    // covers current phones. Emulator (x86/x86_64) and old 32-bit
    // devices can no longer install this APK.
    ndk {
      abiFilters += "arm64-v8a"
    }

    // Keep only English and Persian resources from the app and its libraries.
    resourceConfigurations += setOf("en", "fa")
  }

  // Drop metadata and metadata-like files that ride along in library
  // jars but nothing at runtime reads.
  packaging {
    resources {
      excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "DebugProbesKt.bin",
        "kotlin-tooling-metadata.json",
        "META-INF/*.version",
        "**/*.kotlin_builtins"
      )
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = (System.getenv("PHONE_KEYSTORE_PATH")
        ?: (project.findProperty("PHONE_KEYSTORE_PATH") as? String))?.trim()
      val keystorePassword = (System.getenv("PHONE_KEYSTORE_PASSWORD")
        ?: (project.findProperty("PHONE_KEYSTORE_PASSWORD") as? String))?.trim()
      val keyAliasVal = (System.getenv("PHONE_KEY_ALIAS")
        ?: (project.findProperty("PHONE_KEY_ALIAS") as? String))?.trim()
      val keyPasswordVal = (System.getenv("PHONE_KEY_PASSWORD")
        ?: (project.findProperty("PHONE_KEY_PASSWORD") as? String))?.trim()

      val hasSigningConfig = !keystorePath.isNullOrEmpty() &&
          !keystorePassword.isNullOrEmpty() &&
          !keyAliasVal.isNullOrEmpty() &&
          !keyPasswordVal.isNullOrEmpty()

      if (hasSigningConfig) {
        storeFile = file(keystorePath)
        storePassword = keystorePassword
        keyAlias = keyAliasVal
        keyPassword = keyPasswordVal
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseKeystore = signingConfigs.getByName("release")
      if (releaseKeystore.storeFile != null) {
        signingConfig = releaseKeystore
      }
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
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

base {
  archivesName.set("Phone-$appVersionName")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.material3)
  implementation(libs.haze)
  implementation(libs.backdrop)
  implementation(libs.shizuku.api)
  implementation(libs.shizuku.provider)
  implementation(libs.kadb)
  implementation(libs.coil.compose)
  implementation(libs.androidx.datastore.preferences)
  // Installs the shipped baseline profile on first launch (via the
  // library's App Startup initializer); no manual call is needed.
  implementation(libs.androidx.profileinstaller)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
