package me.cjcrafter.biomemanager.listeners;

import me.cjcrafter.biomemanager.BiomeManager;
import me.cjcrafter.biomemanager.BiomeRegistry;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import me.cjcrafter.biomemanager.events.BiomePacketEvent;
import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.FileUtil;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.ProbabilityMap;
import me.deecaad.core.utils.StringUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class BiomeRandomizer implements Listener {

    private final Map<String, Map<BiomeWrapper, ProbabilityMap<BiomeWrapper>>> variationsMap;
    
    public BiomeRandomizer() {
        variationsMap = new HashMap<>();

        // Load variations from file
        try {
            File folder = new File(BiomeManager.inst().getDataFolder(), "variations");
            folder.mkdirs();

            final FileUtil.PathReference pathReference = FileUtil.PathReference.of(folder.toURI());
            Files.walkFileTree(pathReference.path(), new SimpleFileVisitor<>() {
                String world = null;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    world = dir.getFileName().toString();
                    if ("variations".equals(world))
                        world = null;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    BiomeManager.inst().debug.info("Loaded " + world + " variations");
                    world = null;
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
                    SerializeData data = new SerializeData("Variations", file.toFile(), null, new BukkitConfig(config));

                    String biomeFile = file.getFileName().toString();
                    BiomeWrapper key = BiomeRegistry.getInstance().get(NamespacedKey.fromString(biomeFile.substring(0, biomeFile.length() - ".yml".length()).replace(';', ':')));

                    try {
                        ProbabilityMap<BiomeWrapper> variations = new ProbabilityMap<>();

                        List<String> lines = data.of("Variations").assertExists().assertType(List.class).get();
                        for (String line : lines) {
                            List<String> split = StringUtil.split(line);
                            NamespacedKey variationKey = NamespacedKey.fromString(split.get(0));
                            double weight = Double.parseDouble(split.get(1));

                            BiomeWrapper variation = BiomeRegistry.getInstance().get(variationKey);
                            if (variation == null) {
                                BiomeManager.inst().debug.warn("The biome '" + key + "' doesn't exist anymore... Did you delete it? We'll remove it to avoid errors");
                                continue;
                            }

                            variations.add(variation, weight);
                        }

                        if (!variations.isEmpty()) {
                            addVariation(world, key, variations);
                        }

                    } catch (SerializerException ex) {
                        ex.log(BiomeManager.inst().getDebug());
                    } catch (NumberFormatException ex) {
                        BiomeManager.inst().getDebug().log(LogLevel.ERROR, "There was a number format error in: " + file, ex);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable var3) {
            throw new InternalError(var3);
        }
    }
    
    public void save() {
        File folder = new File(BiomeManager.inst().getDataFolder(), "variations");
        BiomeManager.inst().deleteRecursively(folder);
        folder.mkdirs();

        for (Map.Entry<String, Map<BiomeWrapper, ProbabilityMap<BiomeWrapper>>> entry : variationsMap.entrySet()) {
            File nestedFolder = folder;
            if (entry.getKey() != null) {
                nestedFolder = new File(nestedFolder, entry.getKey());
                nestedFolder.mkdirs();
            }

            for (Map.Entry<BiomeWrapper, ProbabilityMap<BiomeWrapper>> entry1 : entry.getValue().entrySet()) {
                File biomeFile = new File(nestedFolder, entry1.getKey().getKey().toString().replace(':', ';') + ".yml");
                try {
                    biomeFile.createNewFile();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(biomeFile);
                    List<String> variationStrings = new ArrayList<>();
                    entry1.getValue().forEach(node -> {
                        variationStrings.add(node.getValue().getKey() + " " + node.getChance());
                    });

                    config.set("Variations", variationStrings);
                    config.save(biomeFile);
                } catch (IOException ex) {
                    BiomeManager.inst().debug.log(LogLevel.ERROR, "Failed to save variations into " + biomeFile + ex);
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onBiomes(BiomePacketEvent event) {
        BiomeWrapper[] biomes = event.getBiomes();

        for (int i = 0; i < biomes.length; i++) {
            ProbabilityMap<BiomeWrapper> variations = getVariations(event.getWorld().getName(), biomes[i]);
            if (variations != null)
                biomes[i] = variations.get();
        }
    }

    public ProbabilityMap<BiomeWrapper> getVariations(String world, BiomeWrapper base) {
        ProbabilityMap<BiomeWrapper> variations = variationsMap.getOrDefault(world, Collections.emptyMap()).get(base);
        if (variations == null) {
            variations = variationsMap.getOrDefault(null, Collections.emptyMap()).get(base);
        }

        return variations;
    }

    public void addVariation(String world, BiomeWrapper base, ProbabilityMap<BiomeWrapper> variations) {
        Map<BiomeWrapper, ProbabilityMap<BiomeWrapper>> localVariations = variationsMap.computeIfAbsent(world, k -> new HashMap<>());
        localVariations.put(base, variations);
    }

    public boolean deleteVariation(String world, BiomeWrapper base) {
        Map<BiomeWrapper, ProbabilityMap<BiomeWrapper>> localVariation = variationsMap.get(world);
        if (localVariation == null)
            return false;

        return localVariation.remove(base) != null;
    }
}
