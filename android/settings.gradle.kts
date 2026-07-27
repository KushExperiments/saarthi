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

rootProject.name = "saarthi-android"

include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:navigation")
include(":core:ui")
include(":core:testing")
include(":core:security")
include(":feature:placeholder")
