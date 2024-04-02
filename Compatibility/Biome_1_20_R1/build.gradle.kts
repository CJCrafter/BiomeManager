plugins {
    java
    id("io.papermc.paperweight.userdev")
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
}

dependencies {
    paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    implementation(project(":"))
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.cjcrafter:mechanicscore:3.3.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(17)
    }
}