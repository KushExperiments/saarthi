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

rootProject.name = "lifeos-android"

include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:navigation")
include(":core:ui")
include(":core:testing")
include(":core:security")
include(":core:data")
include(":core:ai")
include(":core:memory")
include(":core:cognitive")
include(":core:interaction")
include(":feature:placeholder")
include(":feature:medicines")
include(":feature:contacts")
include(":feature:voice")
