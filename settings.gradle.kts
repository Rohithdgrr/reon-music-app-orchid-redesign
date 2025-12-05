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
    }
}

rootProject.name = "REON"

// Core modules
include(":app")
include(":core:common")
include(":core:model")
include(":core:ui")

// Data layer
include(":data:network")
include(":data:database")
include(":data:repository")

// Media playback
include(":media:playback")

// Feature modules
include(":feature:home")
include(":feature:search")
include(":feature:player")
include(":feature:library")
include(":feature:settings")
