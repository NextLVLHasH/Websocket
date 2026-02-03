plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.nextlvlhash"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    
    // Discord bot library (JDA - Java Discord API)
    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module = "opus-java")
    }
    
    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
    }
}

// Configure source sets to include the capitalized Resources directory
sourceSets {
    main {
        resources {
            srcDir("src/main/Resources")
        }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}

// Build task to create the plugin JAR
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Implementation-Title" to "WebSocket Notification Mod",
            "Implementation-Version" to project.version
        )
    }
    
    archiveBaseName.set("WebsocketNotificationMod")
    archiveVersion.set(project.version.toString())
}

tasks.shadowJar {
    archiveBaseName.set("WebsocketNotificationMod")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    
    // Exclude Hytale server classes if they accidentally get included
    dependencies {
        exclude(dependency("com.hypixel:.*"))
    }
    
    // Exclude signature files to prevent security errors and build issues
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Task to copy the built JAR to a plugins directory (optional)
tasks.register<Copy>("copyToPlugins") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)
    into("$projectDir/plugins")
}
