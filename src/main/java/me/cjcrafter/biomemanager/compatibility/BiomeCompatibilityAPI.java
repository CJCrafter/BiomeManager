package me.cjcrafter.biomemanager.compatibility;

import me.deecaad.core.compatibility.CompatibilitySetup;

public class BiomeCompatibilityAPI {

    private static final BiomeCompatibility BIOME_COMPATIBILITY;

    static {
        BIOME_COMPATIBILITY = new CompatibilitySetup()
                .getCompatibleVersion(BiomeCompatibility.class, "me.cjcrafter.biomemanager.compatibility");
    }

    public static BiomeCompatibility getBiomeCompatibility() {
        return BIOME_COMPATIBILITY;
    }
}
