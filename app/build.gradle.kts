plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

val versionMajor = (project.findProperty("VERSION_MAJOR") as? String)?.toInt() ?: 0
val versionMinor = (project.findProperty("VERSION_MINOR") as? String)?.toInt() ?: 1
val versionPatch = (project.findProperty("VERSION_PATCH") as? String)?.toInt() ?: 0
val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch

android {
  namespace = "io.github.elli0t0.phone"
  compileSdk = 35

  defaultConfig {
    applicationId = "io.github.elli0t0.phone"
    minSdk = 29
    targetSdk = 35
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("PHONE_KEYSTORE_PATH")
        ?: (project.findProperty("PHONE_KEYSTORE_PATH") as? String)
        ?: System.getenv("KEYSTORE_PATH")
      val keystorePassword = System.getenv("PHONE_KEYSTORE_PASSWORD")
        ?: (project.findProperty("PHONE_KEYSTORE_PASSWORD") as? String)
        ?: System.getenv("STORE_PASSWORD")
      val keyAliasVal = System.getenv("PHONE_KEY_ALIAS")
        ?: (project.findProperty("PHONE_KEY_ALIAS") as? String)
        ?: "phone-release"
      val keyPasswordVal = System.getenv("PHONE_KEY_PASSWORD")
        ?: (project.findProperty("PHONE_KEY_PASSWORD") as? String)
        ?: System.getenv("KEY_PASSWORD")

      if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists() &&
          !keystorePassword.isNullOrEmpty() && !keyAliasVal.isNullOrEmpty() && !keyPasswordVal.isNullOrEmpty()) {
        storeFile = file(keystorePath)
        storePassword = keystorePassword
        keyAlias = keyAliasVal
        keyPassword = keyPasswordVal
      }
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
      val releaseKeystore = signingConfigs.getByName("release")
      if (releaseKeystore.storeFile != null) {
        signingConfig = releaseKeystore
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
    }
  }

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
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  testImplementation(libs.junit)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
