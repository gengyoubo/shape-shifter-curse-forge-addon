package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.spell.ExplosionRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** 爆裂/施法超距失败的本地反馈音。 */
public final class ExplosionEffects {
    private ExplosionEffects() {}

    /**
     * 播放一对取消提示音（玻璃碎裂 + 熄火）。音量沿用爆裂的受众距离曲线；以听者相对方式播放，
     * 领域的世界声屏障不会吞掉这条通知。
     */
    public static void playSuppressionCues(Vec3 position) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || position == null) return;
        float volume = ExplosionRules.soundVolume(Math.sqrt(client.player.distanceToSqr(position)));
        if (volume <= 0) return;
        for (var sound : new net.minecraft.sounds.SoundEvent[]{SoundEvents.GLASS_BREAK, SoundEvents.FIRE_EXTINGUISH}) {
            client.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.PLAYERS,
                    volume, 1f, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        }
    }
}
