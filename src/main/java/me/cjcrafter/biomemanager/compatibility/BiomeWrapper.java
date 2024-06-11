package me.cjcrafter.biomemanager.compatibility;

import me.cjcrafter.biomemanager.BiomeManager;
import me.cjcrafter.biomemanager.BiomeRegistry;
import me.cjcrafter.biomemanager.SpecialEffectsBuilder;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.EnumUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.OptionalInt;

public interface BiomeWrapper {

    /**
     * Resets this biome's settings to its default values. For custom biomes,
     * this resets the biome to the base biome's values.
     */
    void reset();

    /**
     * Gets the "base" biome used for default values.
     */
    Biome getBase();

    /**
     * Sets the "base" biome used for default values. Make sure to call
     * {@link #reset()} for the changes to take effect.
     *
     * @param biome The non-null biome to use for default values.
     */
    void setBase(Biome biome);

    /**
     * Returns the key for this biome. For vanilla biomes, the namespace will
     * be <code>"minecraft:"</code>. For custom biomes, the namespace will be
     * the lowercase name of the plugin, like <code>"biomemanager:"</code>.
     *
     * @return The non-null key storing the name of the biome.
     */
    NamespacedKey getKey();

    /**
     * Returns the unique numerical ID for this biome, as set by the server.
     * This value is not guaranteed to be the same across server restarts.
     *
     * @return The unique numerical ID for this biome.
     */
    default OptionalInt getId() {
        return OptionalInt.empty();
    }

    /**
     * Returns the name of this biome. Biome names <i>may</i> not be unique.
     * This method simply used {@link NamespacedKey#getKey()}.
     *
     * @return The non-null name of this biome.
     */
    default String getName() {
        return getKey().getKey();
    }

    /**
     * Gets the special effects of this biome. The returned effects are cloned,
     * so make sure to use {@link #setSpecialEffects(SpecialEffectsBuilder)} after any
     * changes.
     *
     * @return The non-null fog.
     */
    SpecialEffectsBuilder getSpecialEffects();

    /**
     * Sets the special effects of this biome. The changes do not take effect
     * until the players rejoins the server.
     *
     * @param builder The non-null special effects builder.
     */
    void setSpecialEffects(SpecialEffectsBuilder builder);

    /**
     * Sets this biome wrapper to the given block, similar to
     * {@link Block#setBiome(Biome)}.
     *
     * @param block The non-null block to set.
     * @return true if the biome was changed.
     */
    boolean setBiome(Block block);

    /**
     * Registers this biome wrapper to the BiomeManager's biome registry. If
     * <code>isCustom = true</code>, then the biome will also be registered to
     * the Minecraft Server's biome registry. It is important to register
     * custom biomes BEFORE worlds load. Add <code>load: STARTUP</code> to your
     * plugin.yml file.
     *
     * @param isCustom true if this biome wraps a custom biome.
     */
    void register(boolean isCustom);

    /**
     * If this wraps a <b>vanilla biome</b> (any biome listed in the
     * {@link Biome} enum), then this will return that enum. Otherwise this
     * method will return {@link Biome#CUSTOM} (meaning that this wraps a
     * custom biome).
     *
     * @return The {@link Biome} associated with this wrapper, or {@link Biome#CUSTOM}.
     */
    default Biome getBukkitBiome() {
        if (!getKey().getNamespace().equals(NamespacedKey.MINECRAFT))
            return Biome.CUSTOM;

        return EnumUtil.getIfPresent(Biome.class, getName()).orElse(Biome.CUSTOM);
    }

    /**
     * Returns <code>true</code> if this biome wraps a custom biome.
     *
     * @return true if this biome is custom.
     */
    default boolean isCustom() {
        return getBukkitBiome() == Biome.CUSTOM;
    }

    /**
     * Returns <code>true</code> if this biome wraps a custom biome registered
     * by another plugin.
     *
     * @return true if the biome is from an external plugin.
     */
    boolean isExternalPlugin();

    /**
     * Returns <code>true</code> if this biome has been modified from its
     * default state. For custom biomes, this method will always return
     * <code>true</code>.
     *
     * @return true if the biome has been modified.
     */
    boolean isDirty();

    static BiomeWrapper serialize(SerializeData data) throws SerializerException {

        // Make sure we have initialized the compatibility layer. This ensures that
        // we have already added the vanilla biomes (and any custom biomes from other
        // plugins) into our BiomeRegistry class
        BiomeCompatibility _ignore = BiomeCompatibilityAPI.getBiomeCompatibility();

        String keyStr = data.of("Key").assertExists().get();
        boolean isCustom = data.of("Custom").assertExists().getBool();
        boolean isExternalPlugin = data.of("External_Plugin").getBool(false);
        SpecialEffectsBuilder specialEffects = data.of("Special_Effects").assertExists().serialize(SpecialEffectsBuilder.class);
        Biome base = data.of("Base").assertExists().getEnum(Biome.class);

        NamespacedKey key = NamespacedKey.fromString(keyStr);

        // In order for reset() to work on custom biomes registered by other plugins,
        // we need to let them register the plugin. That way we can just create a wrapper later
        if (isExternalPlugin) {
            BiomeWrapper wrapper = BiomeRegistry.getInstance().get(key);
            if (wrapper == null) {
                BiomeManager.inst().debug.error("An externally added biome '" + key + "' doesn't exist anymore... Removing data.");
                return null;
            }

            wrapper.setSpecialEffects(specialEffects);
            return null;
        }

        BiomeWrapper baseWrapper = BiomeRegistry.getInstance().getBukkit(base);
        BiomeWrapper wrapper = BiomeCompatibilityAPI.getBiomeCompatibility().createBiome(key, baseWrapper);
        wrapper.setSpecialEffects(specialEffects);

        // Only register custom biomes, vanilla ones have already been injected
        if (isCustom)
            wrapper.register(true);

        return wrapper;
    }

    default void deserialize(ConfigurationSection config) {
        config.set("Key", getKey().toString());
        config.set("Custom", isCustom());
        config.set("External_Plugin", isExternalPlugin());
        getSpecialEffects().deserialize(config.createSection("Special_Effects"));
        config.set("Base", getBase().name());
    }
}