import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.cpd)
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
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("JUnit")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.vintage) // For JUnit 3/4 style tests (BasePlatformTestCase)
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
    test {
        useJUnitPlatform()
        // Exclude IntelliJ Platform test classes from regular tests
        jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=false")
    }
}

// =============================================================================
// SAST Configuration
// =============================================================================

detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = false
}

spotbugs {
    ignoreFailures.set(false)
    excludeFilter.set(file("config/spotbugs/exclusions.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports {
        create("html") {
            required.set(true)
        }
        create("xml") {
            required.set(false)
        }
    }
}

// CPD configuration
configure<de.aaschmid.gradle.plugins.cpd.CpdExtension> {
    language = "kotlin"
    minimumTokenCount = 120  // Higher threshold to allow similar boilerplate patterns
    isIgnoreAnnotations = true
    isIgnoreLiterals = true
    isIgnoreIdentifiers = true
    toolVersion = "7.24.0"
}

tasks.withType<de.aaschmid.gradle.plugins.cpd.Cpd>().configureEach {
    ignoreFailures = false
}

tasks.register("sast") {
    group = "verification"
    description = "Run all SAST checks (detekt + spotbugs)"
    dependsOn("detekt", "spotbugsMain")
}

tasks.register("sastFull") {
    group = "verification"
    description = "Run all SAST checks including CPD"
    dependsOn("sast", "cpdCheck")
}

// Make check task depend on sastFull
tasks.named("check") {
    dependsOn("sastFull")
}
