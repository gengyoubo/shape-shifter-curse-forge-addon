package github.com.gengyoubo.sscfa.client.sound;

import github.com.gengyoubo.sscfa.client.hud.SpellCastHud;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * 施法蓄力循环音（客户端）。只由服务端 STATE 包的活跃沿启动（被拒绝的施法不发 STATE），
 * 总蓄力不足 3 秒时不播放；HUD 状态清除、权威结束包、超距失败或退出世界时立即停止。
 * 音源跟随施法者，音调随蓄力进度从 0.65 升到 0.90。同一时刻只保留一个实例，停止时按施法者匹配，
 * 不会误停其他玩家的表现。
 */
public final class SpellChargeSoundInstance extends AbstractTickableSoundInstance {
    private static SpellChargeSoundInstance current;

    private static final float BASE_VOLUME = 1.2F;
    private static final float RANGE_COMPENSATION = 0.75F;
    private static final float PITCH_FROM = 0.65F;
    private static final float PITCH_TO = 0.90F;

    private final Entity caster;

    private SpellChargeSoundInstance(Entity caster) {
        super(SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, RandomSource.create());
        this.caster = caster;
        this.looping = true;
        this.relative = false;
        this.volume = BASE_VOLUME * RANGE_COMPENSATION;
        this.pitch = PITCH_FROM;
        this.x = caster.getX();
        this.y = caster.getY();
        this.z = caster.getZ();
    }

    public static void onChannelStart(Entity caster, int durationTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null || caster == null) return;
        if (current != null) client.getSoundManager().stop(current);
        current = null;
        if (!SpellCastingRules.playsChargeSound(durationTicks)) return;
        current = new SpellChargeSoundInstance(caster);
        client.getSoundManager().play(current);
    }

    /** 权威结束包到达即停止，与 HUD 的退场动画无关。 */
    public static void stopFor(UUID caster) {
        if (current == null || caster == null || !current.caster.getUUID().equals(caster)) return;
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    /** 退出世界时无条件停止。 */
    public static void stopAll() {
        if (current == null) return;
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    @Override
    public void tick() {
        SpellCastHud.State state = SpellCastHud.getState();
        if (state == null || caster.isRemoved() || Minecraft.getInstance().level != caster.level()) {
            stop();
            if (current == this) current = null;
            return;
        }
        this.x = caster.getX();
        this.y = caster.getY();
        this.z = caster.getZ();
        if (state.duration() > 0) {
            float progress = Math.min(1.0F, (float) SpellCastHud.getEffectiveElapsed(state) / state.duration());
            this.pitch = PITCH_FROM + (PITCH_TO - PITCH_FROM) * progress;
        }
    }
}
