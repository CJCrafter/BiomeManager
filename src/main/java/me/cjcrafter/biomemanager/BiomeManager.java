package me.cjcrafter.biomemanager;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.cjcrafter.biomemanager.command.Command;
import me.cjcrafter.biomemanager.command.WorldEditCommand;
import me.cjcrafter.biomemanager.compatibility.BiomeCompatibilityAPI;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import me.cjcrafter.biomemanager.listeners.BiomeRandomizer;
import me.cjcrafter.biomemanager.listeners.EditModeListener;
import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.Debugger;
import me.deecaad.core.utils.FileUtil;
import me.deecaad.core.utils.LogLevel;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static com.comphenix.protocol.PacketType.Play.Server.MAP_CHUNK;

public class BiomeManager extends JavaPlugin {

    private static BiomeManager INSTANCE;
    public Debugger debug;
    public EditModeListener editModeListener;
    public BiomeRandomizer biomeRandomizer;

    public void onLoad() {
        INSTANCE = this;
        debug = new Debugger(getLogger(), 2);
    }

    @Override
    public void onEnable() {
        loadConfig();

        Command.register();
        if (getServer().getPluginManager().getPlugin("WorldEdit") != null)
            WorldEditCommand.register();

        // Register packet listeners
        if (!getConfig().getBoolean("Disable_Biome_Variations", false)) {
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            manager.addPacketListener(new PacketAdapter(this, MAP_CHUNK) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    BiomeCompatibilityAPI.getBiomeCompatibility().handleChunkBiomesPacket(event);
                }
            });
        }

        registerBStats();

        // Register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(editModeListener = new EditModeListener(), this);
        pm.registerEvents(biomeRandomizer = new BiomeRandomizer(), this);
    }

    public void onDisable() {
        saveToConfig();
    }

    public void loadConfig() {
        // Make sure we load NMS onEnable
        BiomeCompatibilityAPI.getBiomeCompatibility();

        if (!getDataFolder().exists() || getDataFolder().listFiles() == null || getDataFolder().listFiles().length == 0) {
            debug.info("Copying files from jar (This process may take up to 30 seconds during the first load!)");
            FileUtil.copyResourcesTo(getClassLoader().getResource("BiomeManager"), getDataFolder().toPath());
        }

        try {
            File biomesFolder = new File(getDataFolder(), "biomes");
            biomesFolder.mkdirs();

            final FileUtil.PathReference pathReference = FileUtil.PathReference.of(biomesFolder.toURI());
            Files.walkFileTree(pathReference.path(), new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(new FileReader(file.toFile()));
                        BiomeWrapper.serialize(new SerializeData("Biome", file.toFile(), null, new BukkitConfig(config)));
                    } catch (SerializerException ex) {
                        ex.log(debug);
                    } catch (Throwable ex) {
                        debug.log(LogLevel.ERROR, "Found an error while serializing " + file, ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            debug.log(LogLevel.ERROR, "Error reading config", ex);
        }
    }

    public void deleteRecursively(File directory) {
        if (!directory.isDirectory())
            throw new InternalError(directory + " is not a directory");

        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                deleteRecursively(file);

            file.delete();
        }
    }

    public void saveToConfig() {
        debug.info("Saving biomes to config");

        // Save biome variations
        biomeRandomizer.save();

        // All custom biomes are stored in this folder, make sure it exists.
        File overridesFolder = new File(getDataFolder(), "biomes");
        if (overridesFolder.exists())
            deleteRecursively(overridesFolder);
        overridesFolder.mkdirs();

        Set<NamespacedKey> keys = BiomeRegistry.getInstance().getKeys(true);
        for (NamespacedKey key : keys) {
            BiomeWrapper wrapper = BiomeRegistry.getInstance().get(key);

            // Each namespace (usually a lowercase plugin name, like
            // "biomemanager") gets their own folder to hold their list of
            // custom biomes.
            File namespaceDirectory = new File(overridesFolder, key.getNamespace());
            namespaceDirectory.mkdirs();

            File configFile = new File(namespaceDirectory, key.getKey() + ".yml");
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    config.getKeys(false).forEach(str -> config.set(str, null));

                    wrapper.deserialize(config);
                    config.save(configFile);
                } catch (IOException ex) {
                    debug.log(LogLevel.ERROR, "Error creating/saving file '" + configFile + "'", ex);
                }
            }
        }
    }

    public void registerBStats() {
        Metrics metrics = new Metrics(this, 17119);
        metrics.addCustomChart(new SingleLineChart("custom_biomes",
                () -> BiomeRegistry.getInstance().getKeys(true).size()));
    }

    public Debugger getDebug() {
        return debug;
    }

    public static BiomeManager inst() {
        return INSTANCE;
    }
}
