package me.cjcrafter.biomemanager.compatibility;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;

public interface BiomeCompatibility {

    BiomeWrapper createBiome(NamespacedKey key, BiomeWrapper base);

    BiomeWrapper getBiomeAt(Block block);

    void handleChunkBiomesPacket(PacketEvent event);
}
