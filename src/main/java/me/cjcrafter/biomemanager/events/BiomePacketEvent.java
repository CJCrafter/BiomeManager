package me.cjcrafter.biomemanager.events;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Arrays;

/**
 * Called whenever a chunk packet is sent to a player. This event lets you
 * make "fake" biome changes for players.
 *
 * <p>This event is <i>per-player</i>, meaning that you can change the biomes
 * seen by different players. This also means that any calculations you do will
 * be done for EVERY player, so consider caching results if possible.
 *
 * <p>We use ProtocolLib to intercept the {@link PacketType.Play.Server#MAP_CHUNK}
 * packet which contains the biome information. You can modify this packet manually
 * using {@link #getPacketEvent()}.
 */
public class BiomePacketEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final PacketEvent event;
    private final BiomeWrapper[] biomes;

    private boolean isChunkCache = false;
    private int chunkX;
    private int chunkZ;

    public BiomePacketEvent(PacketEvent event, BiomeWrapper[] biomes) {
        this.event = event;
        this.biomes = biomes;
    }

    private void setChunkCache() {
        if (isChunkCache)
            return;

        isChunkCache = true;
        chunkX = event.getPacket().getIntegers().read(0);
        chunkZ = event.getPacket().getIntegers().read(1);
    }

    /**
     * Returns the ProtocolLib packet event that caused this event.
     *
     * @return ProtocolLib packet event wrapper.
     */
    public PacketEvent getPacketEvent() {
        return event;
    }

    /**
     * Returns the full array of biomes, if you would like to modify it yourself.
     *
     * @return All biomes in this chunk.
     */
    public BiomeWrapper[] getBiomes() {
        return biomes;
    }

    /**
     * The player receiving the changes. Biome changes are per-player, so only
     * this player will be able to see the biomes you changed.
     *
     * @return The player who receives the biome packet.
     */
    public Player getReceivingPlayer() {
        return event.getPlayer();
    }

    /**
     * Returns the world that the biome changes are occurring in.
     *
     * @return The world.
     */
    public World getWorld() {
        return getReceivingPlayer().getWorld();
    }

    /**
     * Returns the chunk x coordinates (multiply by 16 or use {@link #getBlockX()}
     * to get block coordinates).
     *
     * @return The x chunk coordinate.
     * @see #getChunk()
     */
    public int getChunkX() {
        setChunkCache();
        return chunkX;
    }

    /**
     * Returns the chunk z coordinates (multiply by 16 or use {@link #getBlockZ()}
     * to get block coordinates).
     *
     * @return The z chunk coordinate.
     * @see #getChunk()
     */
    public int getChunkZ() {
        setChunkCache();
        return chunkZ;
    }

    /**
     * Returns the x coordinate of the chunk, but in blocks instead of in chunks.
     *
     * @return The x block coordinate.
     */
    public int getBlockX() {
        setChunkCache();
        return chunkX << 4;
    }

    /**
     * Returns the z coordinate of the chunk, but in blocks instead of chunks.
     *
     * @return The z block coordinate.
     */
    public int getBlockZ() {
        setChunkCache();
        return chunkZ << 4;
    }

    /**
     * Returns the chunk we are modifying for the player.
     *
     * @return The chunk involved.
     */
    public Chunk getChunk() {
        setChunkCache();
        return getWorld().getChunkAt(chunkX,  chunkZ);
    }

    /**
     * Converts block coordinates into a biome index.
     *
     * <p>The biome storage format in MC is broken first into 16x16x16 subchunks,
     * then is further broken down into 4x4x4 subchunks. To optimize the process
     * of changing biomes, we use this indexing system.
     *
     * @param block The block coordinates to convert.
     * @return The biome index for that location.
     * @see #getBiomeAtIndex(int)
     * @see #setBiomeAtIndex(int, BiomeWrapper)
     */
    public int getBiomeIndex(Block block) {
        int x = (block.getX() & 15) << 2;
        int y = (block.getY() & 15) << 2;
        int z = (block.getZ() & 15) << 2;

        int ySection = (block.getY() - block.getWorld().getMinHeight()) >> 4;
        return ySection << 6 + x << 5 + y << 4 + z;
    }

    public BiomeWrapper getBiomeAtIndex(int index) {
        return biomes[index];
    }

    public void setBiomeAtIndex(int index, BiomeWrapper biome) {
        biomes[index] = biome;
    }

    public void replaceBiome(BiomeWrapper old, BiomeWrapper replacement) {
        for (int i = 0; i < biomes.length; i++) {
            if (biomes[i].getKey().equals(old.getKey()))
                biomes[i] = replacement;
        }
    }

    public void fillBiome(BiomeWrapper replacement) {
        Arrays.fill(biomes, replacement);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
