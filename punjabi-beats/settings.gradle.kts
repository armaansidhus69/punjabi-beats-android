pluginManagement {
    // Resolve KSP by real artifact coordinates — its plugin marker is only on
    // the (throttled) Gradle Portal; the artifact itself is on the central mirror.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.devtools.ksp") {
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        // Fast mirrors first (sandbox IP is rate-limited on repo.maven.apache.org)
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "PunjabiBeats"
include(":app")
include(":zen-core")
include(":zen-cipher")
include(":zen-potoken")
include(":zen-extractor")
include(":zen-resolver")
include(":zen-datasource")
include(":zenmusic")
