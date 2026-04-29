import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        // Note: Platform test framework requires special IDE environment setup
        // testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4) // Required by IntelliJ Platform test classpath
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
        vendor {
            name = "BerryCrush"
            url = "https://berrycrush.org"
        }
    }

    signing {
        // Configure signing for JetBrains Marketplace publishing (optional)
    }

    publishing {
        // Configure publishing to JetBrains Marketplace (optional)
    }

    instrumentCode = false
}

tasks {
    wrapper {
        gradleVersion = "8.11"
    }

    test {
        useJUnitPlatform()
        // Exclude IntelliJ Platform test classes from regular tests
        jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=false")
    }
}
