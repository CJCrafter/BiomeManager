import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP

group = "me.cjcrafter"
version = "3.7.0"

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    //id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

// See https://github.com/Minecrell/plugin-yml
bukkit {
    main = "me.cjcrafter.biomemanager.BiomeManager"
    name = "BiomeManager"
    apiVersion = "1.16"
    load = STARTUP // required to register biomes before world load

    authors = listOf("CJCrafter")
    depend = listOf("ProtocolLib", "MechanicsCore")
    softDepend = listOf("TerraformGenerator")  // softdepend on plugins that register custom biomes so we can modify them
    loadBefore = listOf("WorldEdit")
}
/*
paper {
    main = "me.cjcrafter.biomemanager.BiomeManager"
    name = "BiomeManager"
    apiVersion = "1.19"
    load = STARTUP // required to register biomes before world load

    authors = listOf("CJCrafter")

    serverDependencies {
        register("MechanicsCore") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("ProtocolLib") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("TerraformGenerator") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
        }
        register("WorldEdit") {
            required = false
        }
    }
}
*/
repositories {
    mavenCentral() // shade bStats
}

dependencies {
    implementation(project(":"))
    implementation(project(":Biome_1_19_R3", "reobf"))
    implementation(project(":Biome_1_20_R1", "reobf"))
    implementation(project(":Biome_1_20_R2", "reobf"))
    implementation(project(":Biome_1_20_R3", "reobf"))
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

        relocate("org.bstats", "me.cjcrafter.biomemanager.lib.bstats") {
            include(dependency("org.bstats:"))
        }
    }

    // This doesn't actually include any dependencies, this relocates all references
    // to the mechanics core lib.
    relocate("net.kyori", "me.deecaad.core.lib")
}