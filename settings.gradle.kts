pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Kadb's SPAKE2 dependency (com.github.Flyfish233:spake2-java) is
    // published only on JitPack, not on Maven Central or Google Maven.
    // Scoped to that group so no other dependency resolves from JitPack.
    maven("https://jitpack.io") {
      content {
        includeGroup("com.github.Flyfish233")
      }
    }
  }
}

rootProject.name = "Phone"

include(":app")
