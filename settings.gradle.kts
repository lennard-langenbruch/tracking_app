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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maps SDK (v11 needs no secret download token anymore)
        maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") }
    }
}

rootProject.name = "FHMobileTracking"
include(":app")
