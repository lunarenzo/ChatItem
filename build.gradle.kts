plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.plugin.yml.paper)
}

group = "io.github.lunatech"
version = "1.0.0-SNAPSHOT"
description = "A lightweight inventory and item showcase plugin."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.annotations)
    compileOnly(libs.placeholderapi)

    implementation(libs.morepaperlib)
    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        // Relocate libraries to avoid conflicts
        val prefix = "io.github.lunatech.chatitem.libs"
        relocate("space.arim.morepaperlib", "$prefix.morepaperlib")
        relocate("org.spongepowered.configurate", "$prefix.configurate")
        relocate("org.snakeyaml", "$prefix.snakeyaml")
        relocate("io.leangen.geantyref", "$prefix.geantyref")
        
        // Exclude Google/Guava packages provided by the server to reduce size
        exclude("com/google/**")
        
        mergeServiceFiles()
    }
}

paper {
    main = "io.github.lunatech.chatitem.ChatItem"
    name = "ChatItem"
    version = "${project.version}"
    description = "${project.description}"
    authors = listOf("lunarenzo")
    apiVersion = "1.21"
    foliaSupported = true

    serverDependencies {
        register("PlaceholderAPI") {
            required = false
        }
    }
}