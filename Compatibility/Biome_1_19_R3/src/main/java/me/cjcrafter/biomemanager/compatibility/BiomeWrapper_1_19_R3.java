package me.cjcrafter.biomemanager.compatibility;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Lifecycle;
import me.cjcrafter.biomemanager.BiomeManager;
import me.cjcrafter.biomemanager.BiomeRegistry;
import me.cjcrafter.biomemanager.SpecialEffectsBuilder;
import me.deecaad.core.utils.EnumUtil;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.ReflectionUtil;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static me.deecaad.core.utils.ReflectionUtil.*;

public class BiomeWrapper_1_19_R3 implements BiomeWrapper {

    private static final Field climateSettingsField;
    private static final Field temperatureAdjustmentField;
    private static final Field generationSettingsField;
    private static final Field mobSettingsField;
    private static final Field particleDensityField;
    private static final Field specialEffectsField;

    static {
        // https://nms.screamingsandals.org/1.19.4/net/minecraft/world/level/biome/Biome$ClimateSettings.html
        Class<?> clazz = getNMSClass("world.level.biome", "BiomeBase$ClimateSettings");
        climateSettingsField = getField(Biome.class, clazz);
        temperatureAdjustmentField = getField(clazz, Biome.TemperatureModifier.class);
        generationSettingsField = getField(Biome.class, BiomeGenerationSettings.class);
        mobSettingsField = getField(Biome.class, MobSpawnSettings.class);
        particleDensityField = getField(AmbientParticleSettings.class, float.class);
        specialEffectsField = getField(Biome.class, BiomeSpecialEffects.class);
    }

    private final NamespacedKey key;
    private Biome base;
    private Biome biome;
    private boolean isVanilla;
    private boolean isExternalPlugin;
    private boolean isDirty; // true for custom and modified vanilla biomes

    public BiomeWrapper_1_19_R3(Biome biome) {
        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();

        this.key = NamespacedKey.fromString(biomes.getKey(biome).toString());
        this.base = biome;

        // Swap so reset() will work in the future (keep a copy of original biome).
        reset();
        Biome temp = this.base;
        this.base = this.biome;
        this.biome = temp;

        isDirty = false;
        isVanilla = true;
        isExternalPlugin = !key.getKey().equals(NamespacedKey.MINECRAFT);
    }

    public BiomeWrapper_1_19_R3(NamespacedKey key, BiomeWrapper_1_19_R3 base) {
        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();

        this.key = key;
        this.base = biomes.get(new ResourceLocation(base.getKey().getNamespace(), base.getKey().getKey()));
        reset();

        // This is true by default, since custom biomes are always "dirty".
        isDirty = true;

        // When these keys are equal, we are overriding a vanilla biome. In
        // this case, we want to modify the raw biome object instead of
        // modifying the copied biome object. We still need to create that
        // copy for the reset() method to work for future calls, though.
        if (key.equals(base.getKey())) {
            Biome temp = this.biome;
            this.biome = this.base;
            this.base = temp;

            isVanilla = true;
            isDirty = false;
        }
    }

    @Override
    public void reset() {
        if (isVanilla)
            isDirty = false;

        Biome temp = new Biome.BiomeBuilder()
                .hasPrecipitation(base.hasPrecipitation())
                .temperature(base.getBaseTemperature())
                .downfall(base.climateSettings.downfall())
                .specialEffects(base.getSpecialEffects())
                .mobSpawnSettings(base.getMobSettings())
                .generationSettings(base.getGenerationSettings())
                .temperatureAdjustment((Biome.TemperatureModifier) invokeField(temperatureAdjustmentField, invokeField(climateSettingsField, base)))
                .build();

        // If we don't have a biome already, just set
        if (biome == null) {
            biome = temp;
            return;
        }

        // When the biome already exists, we have to use reflection since we
        // want to modify the biome that is in the NMS registry.
        setField(climateSettingsField, biome, invokeField(climateSettingsField, temp));
        setField(generationSettingsField, biome, invokeField(generationSettingsField, temp));
        setField(mobSettingsField, biome, invokeField(mobSettingsField, temp));
        setField(specialEffectsField, biome, invokeField(specialEffectsField, temp));
    }

    @Override
    public org.bukkit.block.Biome getBase() {
        if (isVanilla)
            return getBukkitBiome();

        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();
        ResourceKey<Biome> key = biomes.getResourceKey(base).orElseThrow();

        if (!key.location().getNamespace().equals(NamespacedKey.MINECRAFT))
            return org.bukkit.block.Biome.CUSTOM;

        return EnumUtil.getIfPresent(org.bukkit.block.Biome.class, key.location().getPath()).orElse(org.bukkit.block.Biome.CUSTOM);
    }

    @Override
    public void setBase(org.bukkit.block.Biome biome) {
        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();
        base = biomes.get(new ResourceLocation(biome.getKey().getNamespace(), biome.getKey().getKey()));

        if (base == null)
            throw new IllegalArgumentException("Invalid biome: " + biome);
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public int getId() {
        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();
        return biomes.getId(biome);
    }

    @Override
    public SpecialEffectsBuilder getSpecialEffects() {
        BiomeSpecialEffects effects = biome.getSpecialEffects();
        SpecialEffectsBuilder builder = new SpecialEffectsBuilder();
        builder.setFogColor(effects.getFogColor())
                .setWaterColor(effects.getWaterColor())
                .setWaterFogColor(effects.getWaterFogColor())
                .setSkyColor(effects.getSkyColor())
                .setGrassColorModifier(effects.getGrassColorModifier().name());

        effects.getGrassColorOverride().ifPresent(builder::setGrassColorOverride);
        effects.getFoliageColorOverride().ifPresent(builder::setFoliageColorOverride);
        effects.getAmbientLoopSoundEvent().ifPresent(holder -> builder.setAmbientSound(holder.value().getLocation().toString()));

        if (effects.getAmbientParticleSettings().isPresent()) {
            AmbientParticleSettings particle = effects.getAmbientParticleSettings().get();
            builder.setAmbientParticle(particle.getOptions().writeToString())
                    .setParticleProbability((float) ReflectionUtil.invokeField(particleDensityField, particle));
        }

        if (effects.getAmbientMoodSettings().isPresent()) {
            AmbientMoodSettings settings = effects.getAmbientMoodSettings().get();

            builder.setCaveSound(settings.getSoundEvent().value().getLocation().toString())
                    .setCaveTickDelay(settings.getTickDelay())
                    .setCaveSearchDistance(settings.getBlockSearchExtent())
                    .setCaveSoundOffset(settings.getSoundPositionOffset());
        }

        if (effects.getAmbientAdditionsSettings().isPresent()) {
            AmbientAdditionsSettings settings = effects.getAmbientAdditionsSettings().get();

            builder.setRandomSound(settings.getSoundEvent().value().getLocation().toString())
                    .setRandomTickChance(settings.getTickChance());
        }

        if (effects.getBackgroundMusic().isPresent()) {
            Music music = effects.getBackgroundMusic().get();

            builder.setMusicSound(music.getEvent().value().getLocation().toString())
                    .setMusicMinDelay(music.getMinDelay())
                    .setMusicMaxDelay(music.getMaxDelay())
                    .setMusicOverride(music.replaceCurrentMusic());
        }

        return builder;
    }

    @Override
    public void setSpecialEffects(SpecialEffectsBuilder builder) {
        isDirty = true;

        SpecialEffectsBuilder.ParticleData particle = builder.getParticle();
        SpecialEffectsBuilder.MusicData music = builder.getMusic();
        SpecialEffectsBuilder.CaveSoundData caveSettings = builder.getCaveSoundSettings();
        SpecialEffectsBuilder.RandomSoundData cave = builder.getRandomSound();

        BiomeSpecialEffects.Builder a = new BiomeSpecialEffects.Builder()
                .fogColor(builder.getFogColor())
                .waterColor(builder.getWaterColor())
                .waterFogColor(builder.getWaterFogColor())
                .skyColor(builder.getSkyColor())
                .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.valueOf(builder.getGrassColorModifier().trim().toUpperCase()));

        if (builder.getGrassColorOverride() != -1) {
            a.grassColorOverride(builder.getGrassColorOverride());
        }
        if (builder.getFoliageColorOverride() != -1) {
            a.foliageColorOverride(builder.getFoliageColorOverride());
        }
        if (builder.getAmbientSound() != null) {
            a.ambientLoopSound(getSound(builder.getAmbientSound()));
        }
        if (particle.particle() != null) {
            try {
                RegistryAccess access = ((CraftServer) Bukkit.getServer()).getServer().registryAccess();
                ParticleOptions nmsParticle = ParticleArgument.readParticle(new StringReader(particle.particle()), access.lookupOrThrow(Registries.PARTICLE_TYPE));
                a.ambientParticle(new AmbientParticleSettings(nmsParticle, particle.density()));
            } catch (CommandSyntaxException ex) {
                BiomeManager.inst().debug.log(LogLevel.ERROR, "Could not set particle: " + particle, ex);
            }
        }
        if (caveSettings.sound() != null) {
            a.ambientMoodSound(new AmbientMoodSettings(getSound(caveSettings.sound()), caveSettings.tickDelay(), caveSettings.searchOffset(), caveSettings.soundOffset()));
        }
        if (cave.sound() != null) {
            a.ambientAdditionsSound(new AmbientAdditionsSettings(getSound(cave.sound()), cave.tickChance()));
        }
        if (music.sound() != null) {
            a.backgroundMusic(new Music(getSound(music.sound()), music.minDelay(), music.maxDelay(), music.isOverride()));
        }

        ReflectionUtil.setField(specialEffectsField, biome, a.build());
    }

    @Override
    public boolean setBiome(Block block) {
        ServerLevel world = ((CraftWorld) block.getWorld()).getHandle();

        // Don't attempt to load a chunk! This will lag the server.
        LevelChunk chunk = world.getChunkIfLoaded(new BlockPos(block.getX(), block.getY(), block.getZ()));
        if (chunk == null)
            return false;

        // Biomes are stored in 4x4 chunks.
        int x = QuartPos.toSection(block.getX());
        int y = QuartPos.toSection(block.getY());
        int z = QuartPos.toSection(block.getZ());

        chunk.setBiome(x, y, z, Holder.direct(biome));
        return true;
    }

    @Override
    public void register(boolean isCustom) {
        Registry<Biome> biomes = MinecraftServer.getServer().registryAccess().registry(Registries.BIOME).orElseThrow();
        ResourceKey<Biome> resource = ResourceKey.create(biomes.key(), new ResourceLocation(key.getNamespace(), key.getKey()));

        // In order to add biomes, we need to use a writable registry.
        if (!(biomes instanceof WritableRegistry<Biome> writable))
            throw new InternalError(biomes + " was not a writable registry???");

        // Register the biome to BiomeManager's registry, and to the vanilla registry
        if (isCustom) {
            Field freezeField = ReflectionUtil.getField(MappedRegistry.class, boolean.class);
            ReflectionUtil.setField(freezeField, biomes, false);

            Field intrusiveHoldersField = ReflectionUtil.getField(MappedRegistry.class, "m");
            ReflectionUtil.setField(intrusiveHoldersField, biomes, new HashMap<>());

            writable.createIntrusiveHolder(biome);
            writable.register(resource, biome, Lifecycle.stable());

            ReflectionUtil.setField(intrusiveHoldersField, biomes, null);
            ReflectionUtil.setField(freezeField, biomes, true);
        }
        BiomeRegistry.getInstance().add(key, this);
    }

    @Override
    public boolean isExternalPlugin() {
        return isExternalPlugin;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public String toString() {
        return key.toString();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BiomeWrapper biome)) return false;
        return key.equals(biome.getKey());
    }

    private static Holder<SoundEvent> getSound(String sound) {
        ResourceLocation key = ResourceLocation.tryParse(sound);
        return Holder.direct(BuiltInRegistries.SOUND_EVENT.get(key));
    }

    private static ParticleType<?> getParticle(String particle) {
        ResourceLocation key = ResourceLocation.tryParse(particle);
        return BuiltInRegistries.PARTICLE_TYPE.get(key);
    }
}
