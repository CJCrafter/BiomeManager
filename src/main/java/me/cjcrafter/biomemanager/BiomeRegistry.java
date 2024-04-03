package me.cjcrafter.biomemanager;

import com.comphenix.protocol.collections.IntegerMap;
import me.cjcrafter.biomemanager.compatibility.BiomeCompatibilityAPI;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class BiomeRegistry {

    // Singleton design pattern
    private static final BiomeRegistry INSTANCE = new BiomeRegistry();

    public static BiomeRegistry getInstance() {
        return INSTANCE;
    }


    private final Map<NamespacedKey, BiomeWrapper> map;
    private final IntegerMap<BiomeWrapper> byId;
    private final Set<NamespacedKey> removedBiomes;

    public BiomeRegistry() {
        map = new LinkedHashMap<>();
        byId = new IntegerMap<>();
        removedBiomes = new HashSet<>();
    }

    public void add(Plugin plugin, String key, BiomeWrapper biome) {
        add(new NamespacedKey(plugin, key), biome);
    }

    public void add(NamespacedKey key, BiomeWrapper biome) {
        if (map.containsKey(key))
            throw new IllegalArgumentException("Duplicate key '" + key + "'");
        if (removedBiomes.contains(key))
            throw new IllegalArgumentException("Tries to add deleted biome '" + key + "' without restarting the server");

        map.put(key, biome);
        byId.put(biome.getId().orElseThrow(), biome);
    }

    public BiomeWrapper remove(NamespacedKey key) {
        BiomeWrapper removed = map.remove(key);
        if (removed != null)
            removedBiomes.add(key);
        return removed;
    }

    public BiomeWrapper get(NamespacedKey key) {
        return map.get(key);
    }

    public BiomeWrapper getById(int id) {
        return byId.get(id);
    }

    public BiomeWrapper getBukkit(Biome biome) {
        if (biome == Biome.CUSTOM)
            throw new IllegalArgumentException("Cannot use Biome.CUSTOM");

        BiomeWrapper wrapper = map.get(biome.getKey());
        if (wrapper == null)
            throw new IllegalStateException("BiomeManager failed to wrap vanilla biomes... map: " + map);

        return wrapper;
    }

    public BiomeWrapper getOrCreate(NamespacedKey key) {
        return getOrCreate(key, Biome.PLAINS);
    }

    public BiomeWrapper getOrCreate(NamespacedKey key, Biome base) {
        return getOrCreate(key, getBukkit(base));
    }

    public BiomeWrapper getOrCreate(NamespacedKey key, BiomeWrapper base) {
        BiomeWrapper biome = get(key);
        if (biome == null) {
            biome = BiomeCompatibilityAPI.getBiomeCompatibility().createBiome(key, base);
            add(key, biome);
        }
        return biome;
    }

    public Set<NamespacedKey> getKeys(boolean dirty) {
        if (dirty) {
            Set<NamespacedKey> temp = new LinkedHashSet<>();
            for (Map.Entry<NamespacedKey, BiomeWrapper> entry : map.entrySet()) {
                if (entry.getValue().isDirty())
                    temp.add(entry.getKey());
            }
            return temp;
        }

        return Collections.unmodifiableSet(map.keySet());
    }
}
