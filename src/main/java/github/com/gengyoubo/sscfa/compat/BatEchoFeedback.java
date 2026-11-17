package github.com.gengyoubo.sscfa.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.EntityPositionSource;

/** 复用夜间探测本来的目标遍历，声效和振动只发给施术者，每轮反馈数量有上限。 */
public final class BatEchoFeedback {
    private record Burst(long tick, int sent) {}
    private static final Map<UUID, Burst> BURSTS = new HashMap<>();
    private BatEchoFeedback() {}

    public static boolean play(Entity actor, Entity target, int limit, float volume) {
        if (!(actor instanceof ServerPlayer player) || actor == target
                || !(target instanceof LivingEntity) || !target.isAlive() || actor.level() != target.level()) return false;
        int cap = Math.min(32, Math.max(0, limit));
        long tick = player.getServer().getTickCount();
        var previous = BURSTS.get(player.getUUID());
        int sent = previous != null && previous.tick == tick ? previous.sent : 0;
        if (sent >= cap) return false;
        if (sent == 0 && volume > 0) {
            float loudness = Math.min(1, volume);
            MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.SCULK_CLICKING, loudness, 1);
            MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_HAT.value(), loudness, 1);
        }
        var pos = target.getBoundingBox().getCenter();
        var particle = new VibrationParticleOption(new EntityPositionSource(player, player.getEyeHeight()), 10);
        player.serverLevel().sendParticles(player, particle, true, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        BURSTS.put(player.getUUID(), new Burst(tick, sent + 1));
        return true;
    }

    public static void registerLifecycle() {
        PlayerSessionCleanup.register((handler, server) -> BURSTS.remove(handler.player.getUUID()));
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> BURSTS.clear());
    }
}
