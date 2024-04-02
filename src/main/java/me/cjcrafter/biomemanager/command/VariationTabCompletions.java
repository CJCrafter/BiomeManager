package me.cjcrafter.biomemanager.command;

import me.cjcrafter.biomemanager.BiomeRegistry;
import me.deecaad.core.commands.CommandData;
import me.deecaad.core.commands.Tooltip;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class VariationTabCompletions {

    public static Tooltip[] suggestions(CommandData data) {
        String current = data.current().toLowerCase(Locale.ROOT); // keys are all lowercase anyway
        List<Tooltip> tooltips = new ArrayList<>();

        // Check if the current string contains a number without a percentage (e.g., "75")
        if (isIncompletePercentage(current)) {
            tooltips.add(Tooltip.of(current + "%"));
        }

        // Check if the current string ends with a biome (for comma suggestion)
        else if (listBiomes().anyMatch(current::endsWith)) {
            tooltips.add(Tooltip.of(current + ","));
            tooltips.add(Tooltip.of(current + " "));
        }

        // By default, return all biomes as options
        else {
            // trace back to the last comma, space, or percent sign.
            int lastComma = current.lastIndexOf(',');
            int lastSpace = current.lastIndexOf(' ');
            int lastPercent = current.lastIndexOf('%');
            int lastPosition = Math.max(lastComma, Math.max(lastSpace, lastPercent));
            String currentWithoutBiome = (lastPosition == -1) ? "" : current.substring(0, lastPosition + 1);

            listBiomes().forEach(biome -> tooltips.add(Tooltip.of(currentWithoutBiome + biome)));
        }

        return tooltips.toArray(new Tooltip[0]);
    }

    public static boolean isIncompletePercentage(String current) {
        String[] split = current.split("[, ]");
        try {
            Double.parseDouble(split[split.length - 1]);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static Stream<String> listBiomes() {
        // So technically we should use NMS since it is *possible* that a biome
        // is not included here. But, I also like to think I have a life (I don't,
        // but let me dream). Next best thing is to list the keys in our registry.
        return BiomeRegistry.getInstance().getKeys(false).stream()
                .flatMap(biome -> {
                        if (NamespacedKey.MINECRAFT.equals(biome.getNamespace())) {
                            return Stream.of(biome.getKey(), biome.toString());
                        } else {
                            return Stream.of(biome.toString());
                        }
                });
    }
}
