package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 空间归途传送结算（jackcooper，服务端权威）。
 * <p>
 * 读条生命周期完全由统一施法管理器 {@code SpellChannelManager}（特殊一级档）负责：
 * 读条期间禁止主动走动/跳跃，转身/视角不受限（用原版 MC 机制）。打断方式只有两种——受到伤害（外部位）或
 * 自己再次长按施法键 1 秒（自身位）；<b>移动不算打断</b>（旧版位移打断逻辑已随
 * 统一施法重构废弃删除）。
 * <p>
 * 本类只保留「读条完成瞬间的传送与演出」这一最小职责。
 */
public final class SpaceRecallManager {
	private SpaceRecallManager() {
	}

	/** 读条完成：传送回重生点（绑定床/重生锚）并播出发/收两端演出。
	 *  <p>落点复用原版 {@link PlayerEntity#findRespawnPosition}（与死亡重生/起床同源算法，
	 *  含床占位时的周围安全位回退——同床同朝向落点固定，周围堵时顺位下移；2026-09-19 用户定稿）。
	 *  找不到合法落点（床被完全围死且无回退位）时回退床中心，绝不传送失败。 */
	public static void completeNow(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return;
		if (!(player.level() instanceof ServerLevel world)) {
			return;
		}
		BlockPos dest = player.getRespawnPosition();
		if (dest == null) {
			return; // canCast 已前置校验；此处双保险
		}
		// 起点消散
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.PORTAL,
				player.getX(), player.getY(0.5), player.getZ(), 24, 0.3, 0.5, 0.3, 0.1);
		// 目的地维度（重生点可能在其它维度，如重生锚在下界）：跨维度走原版传送链路
		net.minecraft.server.level.ServerLevel destWorld = world.getServer().getLevel(player.getRespawnDimension());
		if (destWorld == null) {
			destWorld = world; // 极端兜底：当前维度
		}
		// 原版起床/重生同款落点搜索：床占位 → 周围偏移表回退（同床同朝向落点固定）
		java.util.Optional<net.minecraft.world.phys.Vec3> landing =
				net.minecraft.world.entity.player.Player.findRespawnPositionAndUseSpawnBlock(
						destWorld, dest, player.getRespawnAngle(), false, true);
		double tx;
		double ty;
		double tz;
		if (landing.isPresent()) {
			net.minecraft.world.phys.Vec3 v = landing.get();
			tx = v.x;
			ty = v.y;
			tz = v.z;
		} else {
			// 全堵兜底：床中心（宁可挤在床里也不让传送失败）
			tx = dest.getX() + 0.5;
			ty = dest.getY() + 0.2;
			tz = dest.getZ() + 0.5;
		}
		// 传送到起床落点（同维度直接挪；跨维度走原版 teleport 全状态迁移）
		if (destWorld == world) {
			player.teleportToWithTicket(tx, ty, tz);
		} else {
			player.teleportTo(destWorld, tx, ty, tz, java.util.Collections.emptySet(),
					player.getYRot(), player.getXRot());
		}
		// 终点光柱与音效（落点在 destWorld；跨维度时粒子必须发到落点维度，修复此前发进起点维度的预存在 bug）
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(destWorld, player, ParticleTypes.END_ROD,
				tx, ty + 1.0, tz, 20, 0.4, 0.6, 0.4, 0.05);
		destWorld.playSound(null, tx, ty, tz,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
		destWorld.playSound(null, tx, ty, tz,
				SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.4f);
	}
}
