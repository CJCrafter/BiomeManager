package me.cjcrafter.biomemanager.listeners;

import me.cjcrafter.biomemanager.BiomeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AutoSaveListener implements Listener {

    private long lastSave = System.currentTimeMillis();

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (!event.getPlayer().isOp())
            return;

        long now = System.currentTimeMillis();
        long minSaveInterval = 1000 * 30;
        if (lastSave + minSaveInterval > now)
            return;

        lastSave = now;

        // When an OP player leaves the server, this MIGHT mean that they are
        // rejoining to see their changes to the biome.
        BiomeManager.inst().getScheduler().async().runNow(() -> BiomeManager.inst().saveToConfig());
    }
}
