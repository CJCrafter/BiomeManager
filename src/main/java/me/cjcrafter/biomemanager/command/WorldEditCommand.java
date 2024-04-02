package me.cjcrafter.biomemanager.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import me.cjcrafter.biomemanager.BiomeRegistry;
import me.cjcrafter.biomemanager.compatibility.BiomeCompatibilityAPI;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import me.deecaad.core.commands.Argument;
import me.deecaad.core.commands.CommandBuilder;
import me.deecaad.core.commands.CommandExecutor;
import me.deecaad.core.commands.arguments.BiomeArgumentType;
import me.deecaad.core.commands.wrappers.BiomeHolder;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;

public class WorldEditCommand {

    public static void register() {
        CommandBuilder builder = new CommandBuilder("/setcustombiome")
                .withPermission("biomemanager.commands.worldedit.setcustombiome")
                .withDescription("Uses WorldEdit to fill custom biomes")
                .withArguments(new Argument<>("biome", new BiomeArgumentType()))
                .executes(CommandExecutor.player((sender, args) -> {
                    LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(sender));
                    Region region;
                    try {
                        region = session.getSelection();
                        if (region.getWorld() == null)
                            throw new IncompleteRegionException();
                    } catch (IncompleteRegionException e) {
                        sender.sendMessage(ChatColor.RED + "Please make a region selection first");
                        return;
                    }

                    // Set each block in the region to the new biome
                    World world = BukkitAdapter.adapt(region.getWorld());
                    BiomeHolder holder = (BiomeHolder) args[0];
                    BiomeWrapper wrapper = BiomeRegistry.getInstance().get(holder.key());
                    if (wrapper == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown biome '" + holder.key() + "'");
                        return;
                    }

                    int count = 0;
                    for (BlockVector3 pos : region) {
                        Block block = world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
                        wrapper.setBiome(block);
                        count++;
                    }

                    sender.sendMessage(ChatColor.GREEN + "" + count + " blocks were effected");
                }));

        builder.register();
    }
}
