package me.cjcrafter.biomemanager.command;

import me.cjcrafter.biomemanager.BiomeManager;
import me.cjcrafter.biomemanager.BiomeRegistry;
import me.cjcrafter.biomemanager.SpecialEffectsBuilder;
import me.cjcrafter.biomemanager.compatibility.BiomeCompatibilityAPI;
import me.cjcrafter.biomemanager.compatibility.BiomeWrapper;
import me.cjcrafter.biomemanager.listeners.BiomeRandomizer;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.commands.*;
import me.deecaad.core.commands.arguments.*;
import me.deecaad.core.commands.wrappers.BiomeHolder;
import me.deecaad.core.commands.wrappers.ParticleHolder;
import me.deecaad.core.commands.wrappers.SoundHolder;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.core.utils.ProbabilityMap;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.core.utils.TableBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

public class Command {

    public static void register() {

        BiomeRegistry biomes = BiomeRegistry.getInstance();
        Argument<?> biomeArg = new Argument<>("biome", new BiomeArgumentType()).withDesc("Which biome to choose");

        CommandBuilder command = new CommandBuilder("biomemanager")
                .withAliases("bm", "biome")
                .withPermission("biomemanager.admin")
                .withDescription("BiomeManager main command")

                .withSubcommand(new CommandBuilder("reset")
                        .withPermission("biomemanager.commands.reset")
                        .withDescription("Reset config of a specific biome")
                        .withArgument(biomeArg)
                        .executes(CommandExecutor.any((sender, args) -> {

                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            biome.reset();
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("randomize")
                        .withPermission("biomemanager.commands.randomize")
                        .withDescription("Randomize fog of a biome")
                        .withArgument(biomeArg)
                        .executes(CommandExecutor.any((sender, args) -> {

                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            effects.setFogColor(randomColor());
                            effects.setWaterColor(randomColor());
                            effects.setWaterFogColor(randomColor());
                            effects.setSkyColor(randomColor());
                            effects.setFoliageColorOverride(randomColor());
                            effects.setGrassColorOverride(randomColor());

                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("menu")
                        .withPermission("biomemanager.commands.menu")
                        .withDescription("Shows the colors of a biome fog")
                        .withArgument(biomeArg)
                        .executes(CommandExecutor.any((sender, args) -> {
                            menu(sender, (BiomeHolder) args[0]);
                        })))

                .withSubcommand(new CommandBuilder("editor")
                        .withAliases("edit")
                        .withPermission("biomemanager.commands.debug")
                        .withDescription("Toggle edit mode to see biome information")
                        .withArgument(new Argument<>("enable", new BooleanArgumentType(), null).withDesc("true to enable, false to disable"))
                        .executes(CommandExecutor.player((sender, args) -> {

                            // If the user did not specify to enable/disable, treat it like a toggle
                            boolean enable;
                            if (args[0] == null)
                                enable = !BiomeManager.inst().editModeListener.isEnabled(sender);
                            else
                                enable = (boolean) args[0];

                            BiomeManager.inst().editModeListener.toggle(sender, enable);
                            Component component = text(enable
                                    ? "You entered Biome Editor mode, move around and watch chat."
                                    : "You exited Biome Editor mode")
                                    .color(enable ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                                    .hoverEvent(text("Click to " + (enable ? "Disable" : "Enable")))
                                    .clickEvent(ClickEvent.runCommand("/biomemanager editor " + !enable));
                            MechanicsCore.getPlugin().adventure.player(sender).sendMessage(component);
                        })))

                .withSubcommand(new CommandBuilder("create")
                        .withPermission("biomemanager.commands.create")
                        .withDescription("Create a new custom biome")
                        .withArgument(new Argument<>("name", new StringArgumentType()).withDesc("The name of the new biome"))
                        .withArgument(new Argument<>("namespace", new StringArgumentType(), "biomemanager").withDesc("The namespace, or 'folder' storing the biomes").replace(SuggestionsBuilder.from("biomemanager")))
                        .withArgument(new Argument<>("base", new EnumArgumentType<>(Biome.class), Biome.PLAINS).withDesc("The base values of the biome"))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = new NamespacedKey(((String) args[1]).toLowerCase(Locale.ROOT), ((String) args[0]).toLowerCase(Locale.ROOT));

                            BiomeWrapper existing = biomes.get(key);
                            if (existing != null) {
                                sender.sendMessage(ChatColor.RED + "The biome '" + key + "' already exists!");
                                return;
                            }

                            BiomeWrapper base = BiomeRegistry.getInstance().getBukkit((Biome) args[2]);
                            BiomeWrapper wrapper = BiomeCompatibilityAPI.getBiomeCompatibility().createBiome(key, base);
                            wrapper.register(true);

                            Component component = text("Created new custom biome '" + key + "'")
                                    .color(NamedTextColor.GREEN)
                                    .hoverEvent(text("Click to modify fog colors"))
                                    .clickEvent(ClickEvent.runCommand("/biomemanager menu " + key));
                            MechanicsCore.getPlugin().adventure.sender(sender).sendMessage(component);
                        })))

                .withSubcommand(new CommandBuilder("fill")
                        .withPermission("biomemanager.commands.fill")
                        .withDescription("Fill an area with a biome (may need to rejoin)")
                        .withArgument(new Argument<>("pos1", new LocationArgumentType()).withDesc("The first point of the volume"))
                        .withArgument(new Argument<>("pos2", new LocationArgumentType()).withDesc("The second point of the volume"))
                        .withArgument(new Argument<>("biome", new BiomeArgumentType()).withDesc("Which biome to fill"))
                        .executes(CommandExecutor.any((sender, args) -> {
                            Block pos1 = ((Location) args[0]).getBlock();
                            Block pos2 = ((Location) args[1]).getBlock();

                            NamespacedKey key = ((BiomeHolder) args[2]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            fillBiome(pos1, pos2, biome);
                            sender.sendMessage(ChatColor.GREEN + "Success! You may need to rejoin to see the changes.");
                        })))

                .withSubcommand(new CommandBuilder("delete")
                        .withPermission("biomemanager.commands.delete")
                        .withDescription("Deletes a custom biome")
                        .withArgument(new Argument<>("biome", new BiomeArgumentType()).withDesc("Which biome to remove"))
                        .executes(CommandExecutor.any((sender, args) -> {
                            try {
                                biomes.remove(((BiomeHolder) args[0]).key());
                                sender.sendMessage(ChatColor.GREEN + "Success! Restart your server for the biome to be deleted.");
                            } catch (Exception ex) {
                                sender.sendMessage(ChatColor.RED + "Failed for reason: " + ex.getMessage());
                            }
                        })))

                .withSubcommand(new CommandBuilder("particle")
                        .withPermission("biomemanager.commands.particle")
                        .withDescription("Change the particle of biome fog")
                        .withArgument(biomeArg)
                        .withArguments(new Argument<>("particle", new ParticleArgumentType()).withDesc("Which particle to spawn"))
                        .withArguments(new Argument<>("density", new DoubleArgumentType(0.0, 1.0), Double.NaN).withDesc("Chance to spawn in each block each tick").append(suggestDensity()))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            ParticleHolder particle = ((ParticleHolder) args[1]);

                            effects.setAmbientParticle(particle.asString());
                            if (!Double.isNaN((double) args[2]))
                                effects.setParticleProbability((float) (double) args[2]);
                            else if (effects.getParticle().density() == -1.0f)
                                effects.setParticleProbability(0.0f);

                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("cave")
                        .withPermission("biomemanager.commands.cave")
                        .withDescription("Sounds only heard in Cave Air)")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("sound", new SoundArgumentType()).withDesc("Which sound to play").append(suggestConfig(b -> b.getCaveSoundSettings().sound())))
                        .withArgument(new Argument<>("tick-delay", new IntegerArgumentType(1)).withDesc("Delay between sounds").append(suggestConfig(b -> b.getCaveSoundSettings().tickDelay())))
                        .withArgument(new Argument<>("search-distance", new IntegerArgumentType(1)).withDesc("*Unknown* Distance to search for cave air").append(suggestConfig(b -> b.getCaveSoundSettings().searchOffset())))
                        .withArgument(new Argument<>("sound-offset", new DoubleArgumentType(0.0)).withDesc("How far away from player to play sound").append(suggestConfig(b -> b.getCaveSoundSettings().soundOffset())))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            String sound = ((SoundHolder) args[1]).key().toString();

                            effects.setCaveSound(sound);
                            effects.setCaveTickDelay((int) args[2]);
                            effects.setCaveSearchDistance((int) args[3]);
                            effects.setCaveSoundOffset((double) args[4]);

                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("music")
                        .withPermission("biomemanager.commands.music")
                        .withDescription("Change the music")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("sound", new SoundArgumentType()).withDesc("Which sound the play").append(suggestConfig(b -> b.getMusic().sound())))
                        .withArgument(new Argument<>("min-delay", new IntegerArgumentType(1)).withDesc("The minimum time between sound tracks").append(suggestConfig(b -> b.getMusic().minDelay())))
                        .withArgument(new Argument<>("max-delay", new IntegerArgumentType(1)).withDesc("The maximum time between sound tracks").append(suggestConfig(b -> b.getMusic().maxDelay())))
                        .withArgument(new Argument<>("override-music", new BooleanArgumentType()).append(suggestConfig(b -> b.getMusic().isOverride())))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            String sound = ((SoundHolder) args[1]).key().toString();

                            if ((int) args[2] > (int) args[3]) {
                                sender.sendMessage(ChatColor.RED + "Make sure min-delay < max-delay");
                                return;
                            }

                            effects.setMusicSound(sound);
                            effects.setMusicMinDelay((int) args[2]);
                            effects.setMusicMaxDelay((int) args[3]);
                            effects.setMusicOverride((boolean) args[4]);

                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("random")
                        .withPermission("biomemanager.commands.random")
                        .withDescription("Sound that has a chance to play every tick")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("sound", new SoundArgumentType()).withDesc("Which sound to play").append(suggestConfig(b -> b.getRandomSound().sound())))
                        .withArgument(new Argument<>("chance", new DoubleArgumentType(0.0, 1.0)).withDesc("Chance to play each tick").append(suggestConfig(b -> b.getRandomSound().tickChance())))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            String sound = ((SoundHolder) args[1]).key().toString();

                            effects.setRandomSound(sound);
                            effects.setRandomTickChance((double) args[2]);

                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("ambient")
                        .withPermission("biomemanager.commands.ambient")
                        .withDescription("Change the ambient sound")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("sound", new SoundArgumentType()).withDesc("Which sound to play").append(suggestConfig(SpecialEffectsBuilder::getAmbientSound)))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            BiomeWrapper biome = biomes.get(key);
                            if (biome == null) {
                                sender.sendMessage(ChatColor.RED + "Failed to find biome '" + key + "'");
                                return;
                            }

                            String sound = ((SoundHolder) args[1]).key().toString();

                            SpecialEffectsBuilder effects = biome.getSpecialEffects();
                            effects.setAmbientSound(sound);
                            biome.setSpecialEffects(effects);
                            changes(sender);
                        })))

                .withSubcommand(new CommandBuilder("color")
                        .withPermission("biomemanager.commands.color")
                        .withDescription("Change the colors of a biome (grass/fog/sky/etc.)")

                        .withSubcommand(new CommandBuilder("fog_color")
                                .withDescription("Overworld=lower sky color, Nether=biome fog")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getFogColor)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setFogColor, (BiomeHolder) args[0], (Color) args[1]);
                                })))

                        .withSubcommand(new CommandBuilder("water_color")
                                .withDescription("Change the color of water")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getWaterColor)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setWaterColor, (BiomeHolder) args[0], (Color) args[1]);
                                })))

                        .withSubcommand(new CommandBuilder("water_fog_color")
                                .withDescription("Change the fog present when you are underwater")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getWaterFogColor)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setWaterFogColor, (BiomeHolder) args[0], (Color) args[1]);
                                })))

                        .withSubcommand(new CommandBuilder("sky_color")
                                .withDescription("Change the color of the sky")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getSkyColor)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setSkyColor, (BiomeHolder) args[0], (Color) args[1]);
                                })))

                        .withSubcommand(new CommandBuilder("foliage_color")
                                .withDescription("Change the color of *most* leaves")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getFoliageColorOverride)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setFoliageColorOverride, (BiomeHolder) args[0], (Color) args[1]);
                                })))

                        .withSubcommand(new CommandBuilder("grass_color")
                                .withDescription("Change the color of grass and plants")
                                .withArgument(biomeArg)
                                .withArgument(new Argument<>("color", new ColorArgumentType()).withDesc("The color to use").append(suggestColor(SpecialEffectsBuilder::getGrassColorOverride)))
                                .executes(CommandExecutor.any((sender, args) -> {
                                    setColor(sender, SpecialEffectsBuilder::setGrassColorOverride, (BiomeHolder) args[0], (Color) args[1]);
                                })))
                )

                .withSubcommand(new CommandBuilder("variation")
                        .withAliases("variations")
                        .withDescription("Add variations by adding random biomes")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("world", new StringArgumentType().withLiteral("*")).append(data -> Bukkit.getWorlds().stream().map(World::getName).map(Tooltip::of).toArray(Tooltip[]::new)))
                        .withArgument(new Argument<>("variations", new GreedyArgumentType()).withDesc("Which biomes to add").replace(VariationTabCompletions::suggestions))
                        .executes(CommandExecutor.any((sender, args) -> {
                            setVariations(sender, (BiomeHolder) args[0], (String) args[1], (String) args[2]);
                        }))
                )

                .withSubcommand(new CommandBuilder("deletevariation")
                        .withAliases("deletevariations")
                        .withDescription("Delete random biome variations that you added")
                        .withArgument(biomeArg)
                        .withArgument(new Argument<>("world", new StringArgumentType().withLiteral("*")).append(data -> Bukkit.getWorlds().stream().map(World::getName).map(Tooltip::of).toArray(Tooltip[]::new)))
                        .executes(CommandExecutor.any((sender, args) -> {
                            NamespacedKey key = ((BiomeHolder) args[0]).key();
                            String world = (String) args[1];

                            BiomeWrapper base = BiomeRegistry.getInstance().get(key);
                            if (base == null) {
                                sender.sendMessage(ChatColor.RED + "Could not find biome '" + key + "'");
                                return;
                            }
                            boolean deleted = BiomeManager.inst().biomeRandomizer.deleteVariation("*".equals(world) ? null : world, base);
                            if (deleted)
                                changes(sender);
                            else
                                sender.sendMessage(ChatColor.RED + "You didn't have any variations configured for world '" + world + "' for '" + key + "'");
                        }))
                );

        java.awt.Color primary = new java.awt.Color(85, 255, 85);
        java.awt.Color secondary = new java.awt.Color(255, 85, 170);
        command.registerHelp(new HelpCommandBuilder.HelpColor(Style.style(TextColor.color(primary.getRGB())), Style.style(TextColor.color(secondary.getRGB())), "\u27A2"));
        command.register();
    }

    private static Function<CommandData, Tooltip[]> suggestColor(Function<SpecialEffectsBuilder, Integer> function) {
        return data -> {
            BiomeHolder biome = (BiomeHolder) data.previousArguments()[0];
            BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
            if (wrapper == null)
                return new Tooltip[0];

            SpecialEffectsBuilder effects = wrapper.getSpecialEffects();
            Integer rgb = function.apply(effects);
            if (rgb == -1)
                return new Tooltip[0];

            Color current = Color.fromRGB(rgb);
            java.awt.Color awtColor = new java.awt.Color(current.getRed(), current.getGreen(), current.getBlue());
            String hex = Integer.toHexString(awtColor.getRGB()).substring(2);

            return new Tooltip[]{Tooltip.of(hex, "The current color of " + biome.key().getKey())};
        };
    }

    private static Function<CommandData, Tooltip[]> suggestConfig(Function<SpecialEffectsBuilder, Object> function) {
        return data -> {
            BiomeHolder biome = (BiomeHolder) data.previousArguments()[0];
            BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
            if (wrapper == null)
                return new Tooltip[0];

            SpecialEffectsBuilder effects = wrapper.getSpecialEffects();
            return new Tooltip[]{Tooltip.of(function.apply(effects), "Current value")};
        };
    }

    private static Function<CommandData, Tooltip[]> suggestDensity() {
        return data -> {
            BiomeHolder biome = (BiomeHolder) data.previousArguments()[0];
            BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
            double density = 0.0;
            if (wrapper != null)
                density = wrapper.getSpecialEffects().getParticle().density();

            return new Tooltip[]{
                    Tooltip.of(density, "Current density of " + biome.key().getKey()),
                    Tooltip.of(0.01428, "warped_forest default value"),
                    Tooltip.of(0.025, "crimson_forest default value"),
                    Tooltip.of(0.00625, "soul_sand_valley default value"),
                    Tooltip.of(0.1189334, "basalt_deltas default value")
            };
        };
    }

    public static void menu(CommandSender sender, BiomeHolder biome) {
        TextComponent.Builder builder = text();

        String[] keys = new String[]{"Fog", "Water", "Water_Fog", "Sky", "Foliage", "Grass"};
        List<Function<SpecialEffectsBuilder, Integer>> elements = Arrays.asList(SpecialEffectsBuilder::getFogColor, SpecialEffectsBuilder::getWaterColor, SpecialEffectsBuilder::getWaterFogColor, SpecialEffectsBuilder::getSkyColor, SpecialEffectsBuilder::getFoliageColorOverride, SpecialEffectsBuilder::getGrassColorOverride);

        Style green = Style.style(NamedTextColor.GREEN);
        Style gray = Style.style(NamedTextColor.GRAY);
        TableBuilder table = new TableBuilder()
                .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setPixels(310).setRows(3))
                .withElementCharStyle(green)
                .withElementStyle(gray)
                .withAttemptSinglePixelFix()
                .withFillChar('=')
                .withFillCharStyle(gray.decorate(TextDecoration.STRIKETHROUGH))
                .withHeaderStyle(green)
                .withHeader(StringUtil.snakeToReadable(biome.key().getKey()))
                .withSupplier(i -> {
                    TextComponent.Builder temp = text();
                    showColors(temp, biome, elements.get(i), keys[i]);
                    return temp.build();
                });

        // Add the table and remove the extra newline().
        builder.append(table.build());

        BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
        SpecialEffectsBuilder effects = wrapper.getSpecialEffects();

        // Particle information
        builder.append(text("PARTICLE: ").style(gray));
        builder.append(getComponent(wrapper, effects.getParticle()));
        builder.append(newline());

        // Sound information
        builder.append(text("AMBIENT: ").style(gray));
        builder.append(text(removeNamespace(effects.getAmbientSound())).style(green)
                .hoverEvent(text("Click to modify the ambient sound"))
                .clickEvent(ClickEvent.suggestCommand("/biomemanager ambient " + biome.key() + " " + effects.getAmbientSound())));
        builder.append(newline());

        builder.append(text("RANDOM: ").style(gray));
        builder.append(getComponent(wrapper, effects.getRandomSound()));
        builder.append(newline());

        builder.append(text("CAVE: ").style(gray));
        builder.append(getComponent(wrapper, effects.getCaveSoundSettings()));
        builder.append(newline());

        builder.append(text("MUSIC: ").style(gray));
        builder.append(getComponent(wrapper, effects.getMusic()));
        builder.append(newline());

        // Footer
        StringBuilder footer = new StringBuilder();
        while (TableBuilder.DEFAULT_FONT.getWidth(footer.toString()) < 310)
            footer.append("=");

        footer.setLength(footer.length() - 1);
        builder.append(text(footer.toString()).style(gray.decorate(TextDecoration.STRIKETHROUGH)));

        MechanicsCore.getPlugin().adventure.sender(sender).sendMessage(builder);
    }

    private static void showColors(TextComponent.Builder builder, BiomeHolder biome, Function<SpecialEffectsBuilder, Integer> function, String key) {
        BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
        if (wrapper == null)
            throw new IllegalArgumentException("Biome '" + biome.key() + "' does not exist");

        SpecialEffectsBuilder fog = wrapper.getSpecialEffects();
        int rgb = function.apply(fog);
        Color color = rgb == -1 ? Color.WHITE : Color.fromRGB(function.apply(fog));

        java.awt.Color awtColor = new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue());
        String hex = Integer.toHexString(awtColor.getRGB()).substring(2);

        ClickEvent click = ClickEvent.suggestCommand("/biomemanager color " + key.toLowerCase(Locale.ROOT) + "_color " + biome.key() + " " + hex);
        HoverEvent<?> hover = HoverEvent.showText(text("Click to set color"));

        builder.append(text(key.toUpperCase(Locale.ROOT) + ": ").color(NamedTextColor.GRAY).clickEvent(click).hoverEvent(hover));
        builder.append(text(hex.toUpperCase(Locale.ROOT)).color(TextColor.color(awtColor.getRGB())).clickEvent(click).hoverEvent(hover));
    }

    public static void setColor(CommandSender sender, BiConsumer<SpecialEffectsBuilder, Color> method, BiomeHolder biome, Color color) {
        BiomeWrapper wrapper = BiomeRegistry.getInstance().get(biome.key());
        SpecialEffectsBuilder effects = wrapper.getSpecialEffects();
        method.accept(effects, color);
        wrapper.setSpecialEffects(effects);
        changes(sender);
    }

    public static void changes(CommandSender sender) {
        MechanicsCore.getPlugin().adventure.sender(sender).sendMessage(text("Success! Leave and Rejoin to see your changes.").color(NamedTextColor.GREEN));
    }

    private static Color randomColor() {
        return Color.fromRGB(RandomUtil.range(0, 255), RandomUtil.range(0, 255), RandomUtil.range(0, 255));
    }

    private static TextComponent getComponent(BiomeWrapper wrapper, SpecialEffectsBuilder.ParticleData data) {
        return text(removeNamespace(data.particle()) + " " + round(data.density()))
                .color(NamedTextColor.GREEN)
                .hoverEvent(text("Click to modify particle"))
                .clickEvent(ClickEvent.suggestCommand("/biomemanager particle " + wrapper.getKey() + " " + data.particle() + " " + data.density()));
    }

    private static TextComponent getComponent(BiomeWrapper wrapper, SpecialEffectsBuilder.CaveSoundData data) {
        return text(removeNamespace(data.sound()) + " " + data.tickDelay() + " " + data.searchOffset() + " " + round((float) data.soundOffset()))
                .color(NamedTextColor.GREEN)
                .hoverEvent(text("Click to modify cave sound"))
                .clickEvent(ClickEvent.suggestCommand("/biomemanager cave " + wrapper.getKey() + " " + data.sound() + " " + data.tickDelay() + " " + data.searchOffset() + " " + data.soundOffset()));
    }

    private static TextComponent getComponent(BiomeWrapper wrapper, SpecialEffectsBuilder.RandomSoundData data) {
        return text(removeNamespace(data.sound()) + " " + round((float) data.tickChance()))
                .color(NamedTextColor.GREEN)
                .hoverEvent(text("Click to modify randomized sound"))
                .clickEvent(ClickEvent.suggestCommand("/biomemanager random " + wrapper.getKey() + " " + data.sound() + " " + data.tickChance()));
    }

    private static TextComponent getComponent(BiomeWrapper wrapper, SpecialEffectsBuilder.MusicData data) {
        return text(removeNamespace(data.sound()) + " " + data.minDelay() + " " + data.maxDelay() + " " + data.isOverride())
                .color(NamedTextColor.GREEN)
                .hoverEvent(text("Click to modify the music"))
                .clickEvent(ClickEvent.suggestCommand("/biomemanager music " + wrapper.getKey() + " " + data.sound() + " " + data.minDelay() + " " + data.maxDelay() + " " + data.isOverride()));
    }

    private static String round(float num) {
        BigDecimal bigDecimal = new BigDecimal(num, new MathContext(2, RoundingMode.HALF_UP));
        bigDecimal = bigDecimal.stripTrailingZeros();
        return bigDecimal.toPlainString();
    }

    private static String removeNamespace(String key) {
        if (key == null)
            return "null";

        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    private static void fillBiome(Block pos1, Block pos2, BiomeWrapper biome) {
        if (!pos1.getWorld().equals(pos2.getWorld()))
            throw new IllegalArgumentException("Cannot fill biome between worlds");

        // Make sure the given coords are actual min/max values.
        World world = pos1.getWorld();
        Block min = world.getBlockAt(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        Block max = world.getBlockAt(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));

        // This is technically inefficient since biomes are set in 4x4 areas.
        for (int x = min.getX(); x < max.getX(); x++) {
            for (int y = min.getY(); y < max.getY(); y++) {
                for (int z = min.getZ(); z < max.getZ(); z++) {
                    Block current = world.getBlockAt(x, y, z);
                    biome.setBiome(current);
                }
            }
        }
    }

    private static void setVariations(CommandSender sender, BiomeHolder biome, String world, String variationsString) {
        if (BiomeManager.inst().getConfig().getBoolean("Disable_Biome_Variations")) {
            sender.sendMessage(ChatColor.RED + "Variations are disabled in the config");
            return;
        }

        String[] split = variationsString.split("[, ]");
        ProbabilityMap<BiomeWrapper> variations = new ProbabilityMap<>();

        if ("*".equals(world))
            world = null;
        else if (Bukkit.getWorld(world) == null) {
            sender.sendMessage(ChatColor.RED + "Cannot find world '" + world + "'");
            return;
        }

        for (String biomeVariation : split) {
            String[] variationData = biomeVariation.split("%", 2);
            String key = variationData.length == 2 ? variationData[1] : variationData[0];
            String chanceStr = variationData.length == 2 ? variationData[0] : "1";

            double chance;
            try {
                chance = Double.parseDouble(chanceStr);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "'" + chanceStr + "' is not a valid number");
                return;
            }

            BiomeWrapper replacement = BiomeRegistry.getInstance().get(NamespacedKey.fromString(key));
            if (replacement == null) {
                sender.sendMessage(ChatColor.RED + "Unknown biome '" + key + "'");
                return;
            }

            variations.add(replacement, chance);
        }

        BiomeRandomizer randomizer = BiomeManager.inst().biomeRandomizer;
        BiomeWrapper base = BiomeRegistry.getInstance().get(biome.key());
        randomizer.addVariation(world, base, variations);
        changes(sender);
    }
}
