package me.cjcrafter.biomemanager;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.ColorSerializer;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;

public class SpecialEffectsBuilder implements Serializer<SpecialEffectsBuilder> {

    /**
     * The data needed to store "ambient particles". In vanilla, only nether
     * biomes use particles. It <i>seems</i> to spawn a particle every block
     * every tick (for a density of 1).
     *
     * @param particle The particle data to parse into an NMS particle
     * @param density  The 0..1 probability to spawn the particle in a block
     */
    public record ParticleData(String particle, float density) {
    }

    /**
     * The data needed to store "cave sounds". Cave sounds are played when a
     * player is in {@link org.bukkit.Material#CAVE_AIR}. It may also play if
     * the game "thinks" the player is underground, but more testing is
     * required.
     *
     * @param sound        The minecraft namespace name of the sound.
     * @param tickDelay    The delay, in ticks, between sounds.
     * @param searchOffset This MIGHT be how far away from the player to look for CAVE_AIR
     * @param soundOffset  How far, in blocks, will the sound play from.
     */
    public record CaveSoundData(String sound, int tickDelay, int searchOffset, double soundOffset) {
    }

    /**
     * Plays a sound on the player every tick (When <code>tickChance</code> is
     * 1.0). So if you want to play a random sound every 1 second (on average),
     * set <code>tickChance</code> to 1.0 / 20.0.
     *
     * @param sound      The minecraft namespace name of the sound.
     * @param tickChance The 0..1 chance to play the sound every tick.
     */
    public record RandomSoundData(String sound, double tickChance) {
    }

    /**
     * Music needs more testing. Seems to be useful for playing sounds back to
     * back without overlap.
     *
     * @param sound      The minecraft namespace name of the sound.
     * @param minDelay   The minimum number of ticks between playing songs.
     * @param maxDelay   The maximum number of ticks between playing songs.
     * @param isOverride When a player enters a new biome, should their music be overridden by this?
     */
    public record MusicData(String sound, int minDelay, int maxDelay, boolean isOverride) {
    }


    private int fogColor;
    private int waterColor;
    private int waterFogColor;
    private int skyColor;
    private int foliageColorOverride = -1;
    private int grassColorOverride = -1;
    private String grassColorModifier = "NONE";
    private ParticleData particle;
    private String ambientSound;
    private CaveSoundData caveSoundSettings;
    private RandomSoundData caveSound;
    private MusicData music;


    public SpecialEffectsBuilder() {
        particle = new ParticleData(null, -1);
        caveSoundSettings = new CaveSoundData(null, -1, -1, -1);
        caveSound = new RandomSoundData(null, -1);
        music = new MusicData(null, -1, -1, false);
    }

    public int getFogColor() {
        return fogColor;
    }

    public SpecialEffectsBuilder setFogColor(int fogColor) {
        this.fogColor = fogColor;
        return this;
    }

    public SpecialEffectsBuilder setFogColor(Color fogColor) {
        this.fogColor = fogColor.asRGB();
        return this;
    }

    public int getWaterColor() {
        return waterColor;
    }

    public SpecialEffectsBuilder setWaterColor(int waterColor) {
        this.waterColor = waterColor;
        return this;
    }

    public SpecialEffectsBuilder setWaterColor(Color waterColor) {
        this.waterColor = waterColor.asRGB();
        return this;
    }

    public int getWaterFogColor() {
        return waterFogColor;
    }

    public SpecialEffectsBuilder setWaterFogColor(int waterFogColor) {
        this.waterFogColor = waterFogColor;
        return this;
    }

    public SpecialEffectsBuilder setWaterFogColor(Color waterFogColor) {
        this.waterFogColor = waterFogColor.asRGB();
        return this;
    }

    public int getSkyColor() {
        return skyColor;
    }

    public SpecialEffectsBuilder setSkyColor(int skyColor) {
        this.skyColor = skyColor;
        return this;
    }

    public SpecialEffectsBuilder setSkyColor(Color skyColor) {
        this.skyColor = skyColor.asRGB();
        return this;
    }

    public int getFoliageColorOverride() {
        return foliageColorOverride;
    }

    public SpecialEffectsBuilder setFoliageColorOverride(int foliageColorOverride) {
        this.foliageColorOverride = foliageColorOverride;
        return this;
    }

    public SpecialEffectsBuilder setFoliageColorOverride(Color foliageColorOverride) {
        this.foliageColorOverride = foliageColorOverride.asRGB();
        return this;
    }

    public int getGrassColorOverride() {
        return grassColorOverride;
    }

    public SpecialEffectsBuilder setGrassColorOverride(int grassColorOverride) {
        this.grassColorOverride = grassColorOverride;
        return this;
    }


    public SpecialEffectsBuilder setGrassColorOverride(Color grassColorOverride) {
        this.grassColorOverride = grassColorOverride.asRGB();
        return this;
    }

    public String getGrassColorModifier() {
        return grassColorModifier;
    }

    public SpecialEffectsBuilder setGrassColorModifier(String grassColorModifier) {
        this.grassColorModifier = grassColorModifier;
        return this;
    }

    public ParticleData getParticle() {
        return particle;
    }

    public SpecialEffectsBuilder setAmbientParticle(String ambientParticle) {
        particle = new ParticleData(ambientParticle, particle.density);
        return this;
    }

    public SpecialEffectsBuilder setParticleProbability(float particleProbability) {
        particle = new ParticleData(particle.particle, particleProbability);
        return this;
    }

    public String getAmbientSound() {
        return ambientSound;
    }

    public void setAmbientSound(String ambientSound) {
        this.ambientSound = ambientSound;
    }

    public CaveSoundData getCaveSoundSettings() {
        return caveSoundSettings;
    }

    public SpecialEffectsBuilder setCaveSound(String sound) {
        caveSoundSettings = new CaveSoundData(sound, caveSoundSettings.tickDelay, caveSoundSettings.searchOffset, caveSoundSettings.soundOffset);
        return this;
    }

    public SpecialEffectsBuilder setCaveTickDelay(int tickDelay) {
        caveSoundSettings = new CaveSoundData(caveSoundSettings.sound, tickDelay, caveSoundSettings.searchOffset, caveSoundSettings.soundOffset);
        return this;
    }

    public SpecialEffectsBuilder setCaveSearchDistance(int searchDistance) {
        caveSoundSettings = new CaveSoundData(caveSoundSettings.sound, caveSoundSettings.tickDelay, searchDistance, caveSoundSettings.soundOffset);
        return this;
    }

    public SpecialEffectsBuilder setCaveSoundOffset(double soundOffset) {
        caveSoundSettings = new CaveSoundData(caveSoundSettings.sound, caveSoundSettings.tickDelay, caveSoundSettings.searchOffset, soundOffset);
        return this;
    }

    public RandomSoundData getRandomSound() {
        return caveSound;
    }

    public SpecialEffectsBuilder setRandomSound(String sound) {
        caveSound = new RandomSoundData(sound, caveSound.tickChance);
        return this;
    }

    public SpecialEffectsBuilder setRandomTickChance(double tickChance) {
        caveSound = new RandomSoundData(caveSound.sound, tickChance);
        return this;
    }

    public MusicData getMusic() {
        return music;
    }

    public SpecialEffectsBuilder setMusicSound(String sound) {
        music = new MusicData(sound, music.minDelay, music.maxDelay, music.isOverride);
        return this;
    }

    public SpecialEffectsBuilder setMusicMinDelay(int minDelay) {
        music = new MusicData(music.sound, minDelay, music.maxDelay, music.isOverride);
        return this;
    }

    public SpecialEffectsBuilder setMusicMaxDelay(int maxDelay) {
        music = new MusicData(music.sound, music.minDelay, maxDelay, music.isOverride);
        return this;
    }

    public SpecialEffectsBuilder setMusicOverride(boolean isOverride) {
        music = new MusicData(music.sound, music.minDelay, music.maxDelay, isOverride);
        return this;
    }

    @Nonnull
    @Override
    public SpecialEffectsBuilder serialize(SerializeData data) throws SerializerException {

        Color fogColor = data.of("Fog_Color").assertExists().serialize(new ColorSerializer()).getColor();
        Color waterColor = data.of("Water_Color").assertExists().serialize(new ColorSerializer()).getColor();
        Color waterFogColor = data.of("Water_Fog_Color").assertExists().serialize(new ColorSerializer()).getColor();
        Color skyColor = data.of("Sky_Color").assertExists().serialize(new ColorSerializer()).getColor();
        ColorSerializer foliageColor = data.of("Foliage_Color").serialize(new ColorSerializer());
        ColorSerializer grassColor = data.of("Grass_Color").serialize(new ColorSerializer());

        SpecialEffectsBuilder builder = new SpecialEffectsBuilder()
                .setFogColor(fogColor)
                .setWaterColor(waterColor)
                .setWaterFogColor(waterFogColor)
                .setSkyColor(skyColor)
                .setGrassColorModifier(data.of("Grass_Modifier").get("NONE").trim().toUpperCase());

        if (foliageColor != null) {
            builder.setFoliageColorOverride(foliageColor.getColor());
        }

        if (grassColor != null) {
            builder.setGrassColorOverride(grassColor.getColor());
        }

        if (data.has("Particle")) {
            builder.setAmbientParticle(data.of("Particle.Type").assertExists().get());
            builder.setParticleProbability((float) data.of("Particle.Density").assertExists().getDouble());
        }

        builder.setAmbientSound(data.of("Ambient_Sound").get(null));

        if (data.has("Cave_Sound")) {
            builder.setCaveSound(data.of("Cave_Sound.Sound").assertExists().get());
            builder.setCaveTickDelay(data.of("Cave_Sound.Tick_Delay").assertExists().assertPositive().getInt());
            builder.setCaveSearchDistance(data.of("Cave_Sound.Search_Distance").assertExists().assertPositive().getInt());
            builder.setCaveSoundOffset(data.of("Cave_Sound.Sound_Offset").assertExists().assertPositive().getDouble());
        }

        if (data.has("Random_Sound")) {
            builder.setRandomSound(data.of("Random_Sound.Sound").assertExists().get());
            builder.setRandomTickChance(data.of("Random_Sound.Tick_Chance").assertExists().assertPositive().getDouble());
        }

        if (data.has("Music")) {
            builder.setMusicSound(data.of("Music.Sound").assertExists().get());
            builder.setMusicMinDelay(data.of("Music.Min_Delay").assertExists().assertPositive().getInt());
            builder.setMusicMaxDelay(data.of("Music.Max_Delay").assertExists().assertPositive().getInt());
            builder.setMusicOverride(data.of("Music.Override_Previous_Music").assertExists().getBool());
        }

        return builder;
    }

    public void deserialize(ConfigurationSection config) {
        config.set("Fog_Color", deserializeColor(fogColor));
        config.set("Water_Color", deserializeColor(waterColor));
        config.set("Water_Fog_Color", deserializeColor(waterFogColor));
        config.set("Sky_Color", deserializeColor(skyColor));

        // Setting to null deleted the option in config
        config.set("Foliage_Color", foliageColorOverride == -1 ? null : deserializeColor(foliageColorOverride));
        config.set("Grass_Color", grassColorOverride == -1 ? null : deserializeColor(grassColorOverride));
        config.set("Grass_Modifier", "NONE".equals(grassColorModifier) ? null : grassColorModifier);

        if (particle.particle != null) {
            config.set("Particle.Type", particle.particle);
            config.set("Particle.Density", particle.density);
        } else {
            config.set("Particle", null);  // else delete section (if present)
        }

        config.set("Ambient_Sound", ambientSound);

        if (caveSoundSettings.sound != null) {
            config.set("Cave_Sound.Sound", caveSoundSettings.sound);
            config.set("Cave_Sound.Tick_Delay", caveSoundSettings.tickDelay);
            config.set("Cave_Sound.Search_Distance", caveSoundSettings.searchOffset);
            config.set("Cave_Sound.Sound_Offset", caveSoundSettings.soundOffset);
        } else {
            config.set("Cave_Sound", null);  // else delete section (if present)
        }

        if (caveSound.sound != null) {
            config.set("Random_Sound.Sound", caveSound.sound);
            config.set("Random_Sound.Tick_Chance", caveSound.tickChance);
        } else {
            config.set("Random_Sound", null);  // else delete section (if present)
        }

        if (music.sound != null) {
            config.set("Music.Sound", music.sound);
            config.set("Music.Min_Delay", music.minDelay);
            config.set("Music.Max_Delay", music.maxDelay);
            config.set("Music.Override_Previous_Music", music.isOverride);
        } else {
            config.set("Music", null);
        }
    }

    private static String deserializeColor(int rgb) {
        Color color = Color.fromRGB(rgb);
        return color.getRed() + "-" + color.getGreen() + "-" + color.getBlue();
    }
}
