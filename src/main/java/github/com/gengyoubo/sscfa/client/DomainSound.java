package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.DomainRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.UUID;

/** 领域音效：跟随施法者位置并按壳内距离曲线调节音量。 */
public final class DomainSound extends AbstractTickableSoundInstance {
    private final ClientLevel world;
    private final UUID owner;
    private final float volumeScale;
    private final double innerRadius;
    private final double outerRadius;

    private DomainSound(ClientLevel world, UUID owner, SoundEvent sound, double x, double y, double z,
                        float pitch, float volumeScale, long seed, double innerRadius, double outerRadius) {
        super(sound, SoundSource.PLAYERS, RandomSource.create(seed));
        this.world = world;
        this.owner = owner;
        this.volumeScale = volumeScale;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.attenuation = SoundInstance.Attenuation.NONE;
        tick();
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(DomainManager.SOUND, buf -> {
            ResourceLocation dimension = buf.readResourceLocation();
            UUID owner = buf.readUUID();
            ResourceLocation soundId = buf.readResourceLocation();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float pitch = buf.readFloat();
            float volumeScale = buf.readFloat();
            long seed = buf.readLong();
            double inner = buf.isReadable() ? buf.readDouble() : DomainRules.innerRadius();
            double outer = buf.isReadable() ? buf.readDouble() : DomainRules.outerRadius();

            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            if (level == null || client.player == null || !level.dimension().location().equals(dimension)) return;
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (sound != null) {
                client.getSoundManager().play(new DomainSound(level, owner, sound, x, y, z,
                        pitch, volumeScale, seed, inner, outer));
            }
        });
    }

    @Override
    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != world || client.player == null) {
            this.volume = 0;
            stop();
            return;
        }
        var caster = world.getPlayerByUUID(owner);
        if (caster != null && !caster.isRemoved()) {
            this.x = caster.getX();
            this.y = caster.getY();
            this.z = caster.getZ();
        }
        this.volume = DomainRules.soundVolume(
                Math.sqrt(client.player.distanceToSqr(this.x, this.y, this.z)), innerRadius, outerRadius)
                * volumeScale;
    }
}
