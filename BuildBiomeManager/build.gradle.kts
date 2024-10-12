import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP

group = "me.cjcrafter"
version = "3.7.4"

plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.3"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

// See https://github.com/Minecrell/plugin-yml
bukkit {
    main = "me.cjcrafter.biomemanager.BiomeManager"
    name = "BiomeManager"
    apiVersion = "1.16"
    load = STARTUP // required to register biomes before world load
    foliaSupported = true

    authors = listOf("CJCrafter")
    depend = listOf("ProtocolLib", "MechanicsCore")
    softDepend = listOf("TerraformGenerator")  // softdepend on plugins that register custom biomes so we can modify them
    loadBefore = listOf("WorldEdit")
}

repositories {
    mavenCentral() // shade bStats
}

dependencies {
    implementation(project(":"))
    implementation(project(":Biome_1_19_R3", "reobf"))
    implementation(project(":Biome_1_20_R1", "reobf"))
    implementation(project(":Biome_1_20_R2", "reobf"))
    implementation(project(":Biome_1_20_R3", "reobf"))
    implementation(project(":Biome_1_20_R4", "reobf"))
    implementation(project(":Biome_1_21_R1", "reobf"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.shadowJar {
    destinationDirectory.set(file("../build"))
    archiveFileName.set("BiomeManager-${project.version}.jar")

    dependencies {
        include(project(":"))
        include(project(":Biome_1_19_R3"))
        include(project(":Biome_1_20_R1"))
        include(project(":Biome_1_20_R2"))
        include(project(":Biome_1_20_R3"))
        include(project(":Biome_1_20_R4"))
        include(project(":Biome_1_21_R1"))

        relocate("org.bstats", "com.cjcrafter.biomemanager.lib.bstats") {
            include(dependency("org.bstats:"))
        }
    }

    relocate("com.cjcrafter.foliascheduler", "me.deecaad.core.lib.scheduler")

    // This doesn't actually include any dependencies, this relocates all references
    // to the mechanics core lib.
    relocate("net.kyori", "me.deecaad.core.lib")
}