plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0"
description = "Universal custom join and leave messages for Velocity/BungeeCord + Paper/Spigot/Folia"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper + Velocity
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // BungeeCord
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
}

dependencies {
    // 代理端依赖 (compileOnly to avoid conflicts)
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    
    // 后端依赖 (compileOnly to avoid conflicts)  
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    
    // 注解处理器
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    
    // 配置文件处理
    implementation("org.yaml:snakeyaml:2.0")
    
    // 工具库
    implementation("com.google.guava:guava:31.1-jre")
    
    // JSON处理 (用于持久化玩家数据)
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    jar {
        archiveBaseName.set("CustomJoinMessage")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
    
    processResources {
        val props = mapOf(
            "version" to version,
            "description" to description
        )
        inputs.properties(props)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }
}