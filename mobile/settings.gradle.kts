pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // DJI Mobile SDK v5 ships through Maven Central; no extra repo needed.
    }
}

rootProject.name = "RoofRecon"
include(":app")
