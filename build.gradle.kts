group = "io.github.sokrato"
version = "0.1.3"

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.12.0"
    id("io.github.sokrato.gmate")
}

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(platform("io.github.sokrato:bom:0.1.0-SNAPSHOT"))
    testImplementation("org.testng:testng")
}

val test by tasks.getting(Test::class) {
    useTestNG()
}

gradlePlugin {
    plugins {
        create("gmate") {
            id = "io.github.sokrato.gmate"
            implementationClass = "io.github.sokrato.gradle.plugin.GmatePlugin"
        }
    }
}

pluginBundle {
    // These settings are set for the whole plugin bundle
    website = "http://github.com/sokrato"
    vcsUrl = "https://github.com/sokrato/gapp"

    // tags and description can be set for the whole bundle here, but can also
    // be set / overridden in the config for specific plugins
    description = "Gradle Mate!"

    // The plugins block can contain multiple plugin entries.
    //
    // The name for each plugin block below (greetingsPlugin, goodbyePlugin)
    // does not affect the plugin configuration, but they need to be unique
    // for each plugin.

    // Plugin config blocks can set the id, displayName, version, description
    // and tags for each plugin.

    // id and displayName are mandatory.
    // If no version is set, the project version will be used.
    // If no tags or description are set, the tags or description from the
    // pluginBundle block will be used, but they must be set in one of the
    // two places.

    (plugins) {
        // first plugin
        "gmate" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Your gradle-mate"
            tags = listOf("git")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("gmate") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri("file:///~/.m2/repository")
        }
    }
}
