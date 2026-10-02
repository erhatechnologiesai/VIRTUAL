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
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

rootProject.name = "VIRTUAL"
include(":app")
include(":core")
include(":camera")
include(":webrtc")
include(":pairing")
include(":viewer")
include(":auth")
include(":settings")
