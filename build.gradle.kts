import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
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
        bundledPlugins("com.intellij.java")
        instrumentationTools()
        // Note: Platform test framework requires special IDE environment setup
        // testFramework(TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
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
            url = "https://github.com/berrycrush/berrycrush"
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
