package me.cjcrafter.biomemanager.events;

import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiomeCreateEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    private final BiomeWrapper biome;

    public BiomeCreateEvent(BiomeWrapper biome) {
        this.biome = biome;
    }

    public BiomeWrapper getBiome() {
        return biome;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
