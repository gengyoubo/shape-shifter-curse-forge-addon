package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 空间归途（空间系，蓝色基底，jackcooper）：特殊一级，统一读条 8 秒后传送回绑定床（重生点）。
 * 施法前置校验：必须有重生点（canCast），否则拒绝（不耗法力不进 CD）。
 * 读条生命周期由统一管理器负责：禁主动走动/跳跃，转身/视角不受限（原版机制）；打断仅限受伤或自己再次长按施法键 1 秒，移动不算打断。
 * 读条完成 → {@code SpaceRecallManager.completeNow} 传送结算。
 *
 * <p>数值外置 {@code data/ssc_addon/spells/space_recall.json}：
 * 读条 8s / cd 60s（L5 30s，每级 -7.5s）/ 耗蓝 60（每级 +20%）。</p>
 */
public class SpaceRecallSpell extends Spell {

	public SpaceRecallSpell() {
		super(new ResourceLocation("ssc_addon", "space_recall"), SpellRarity.BLUE);
	}

	@Override
	public boolean canCast(ServerPlayer caster) {
		// 必须有绑定重生点（床/重生锚）
		BlockPos spawnPos = caster.getRespawnPosition();
		return spawnPos != null && !github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(caster);
	}

	@Override
	public net.minecraft.network.chat.Component getCastFailureMessage(ServerPlayer caster) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(caster))
            return net.minecraft.network.chat.Component.translatable("message.ssc_addon.anomaly.teleport_blocked");
		return net.minecraft.network.chat.Component.translatable("message.ssc_addon.spellbook.no_spawn");
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(caster)) {
            github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.miss(); return;
        }
		if (!(caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		BlockPos spawnPos = caster.getRespawnPosition();
		if (spawnPos == null) {
			return; // 双保险
		}
		github.com.gengyoubo.sscfa.ability.SpaceRecallManager.completeNow(caster);
		// 起手演出：星门环绕
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.PORTAL,
				caster.getX(), caster.getY(0.8), caster.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
	}

    @Override public boolean canContinueCasting(ServerPlayer caster, net.minecraft.world.item.ItemStack scroll) {
        return canCast(caster);
    }
}
