package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 寒棘狐被动「寒棘护体」：棘甲 + 反刺（服务端权威，多人一致）?
 *
 * <p><b>棘??/b>：每根环绕冰锥使受到的近战伤害减?4%（??5 ??20%）——冰锥既是矛也是盾??
 * 发射后防御随之下跌，攻防自抉择。冰锥数实时?{@link FrostSpikeManager}??/p>
 *
 * <p><b>反??/b>：被生物近战命中时，攻击者??1 层「寒棘」（缓??I 2.5 ??+ 雪花粒子）??
 * ??3 层触??b>棘??/b>：攻击者被冻结（FROST_FREEZE 1 秒?? ??2 点反伤，层数清零??
 * 内??0.5 秒防连击刷层（同一攻击??10 tick 内的连续命中只算 1 层）?/p>
 *
 * <p>白名单：默认白名单——白名单内个体（玩家及宠??召唤物）不受反刺与棘爆影响??/p>
 */
public final class FrostArmorManager {

	// 以下常量均为默认值；运行时??balance 快照读取（scope：abilities.frost_armor_manager??
	/** 每根环绕冰锥的近战减伤比例??*/
	private static final float PER_THORN_REDUCTION = 0.04f;
	/** 反刺叠层上限：达到即触发棘爆并清零?*/
	private static final int BURST_AT = 3;
	/** 寒棘层有效期（tick）：超时未叠新层则清零重计?*/
	private static final int LAYER_EXPIRE_TICKS = 50;
	/** 同一攻击者两次命中叠层的最小间隔（tick），防连击快速刷满?*/
	private static final int RE_LAYER_GAP_TICKS = 10;
	/** 棘爆冻结时长（tick）??*/
	private static final int BURST_FREEZE_TICKS = 20;
	/** 棘爆反伤??*/
	private static final float BURST_DAMAGE = 2.0f;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_armor_manager");

	/** attackerUuid ??叠层状态?*/
	private static final Map<UUID, Layers> LAYERS = new HashMap<>();

	private static final class Layers {
		int count;
		long lastLayerGameTime = Long.MIN_VALUE; // 上次叠层的游戏时??
		long lastHitGameTime = Long.MIN_VALUE;   // 上次命中的游戏时刻（防连击门?
	}

	private FrostArmorManager() {}

	/** 是否为「生物近战命中」：攻击者是活体且伤害源为近战系（玩家普?/ 生物近战）??*/
	public static boolean isMeleeHit(DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity) || !(attacker instanceof net.minecraft.world.entity.Mob)
				&& !(attacker instanceof net.minecraft.world.entity.player.Player)) {
			return false;
		}
		return source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
	}

	/**
	 * 棘甲：受害者是寒棘狐且受近战伤????按环绕冰锥数减伤??
	 * ??LivingEntity.damage ??applyDamage 调用点（ModifyArgs），不吞事件、不吞击退?
	 *
	 * @return 修正后的伤害?
	 */
	public static float applyArmor(ServerPlayer victim, DamageSource source, float amount) {
		if (!isMeleeHit(source)) return amount;
		int thorns = FrostSpikeManager.getHoverCount(victim);
		if (thorns <= 0) return amount;
		float reduction = Math.min(0.2f, thorns * (float) BAL.d("per_thorn_reduction", PER_THORN_REDUCTION));
		return amount * (1.0f - reduction);
	}

	/**
	 * 反刺入口：寒棘狐被近战命中（伤害已实际生效后调用）??攻击者叠?/ ??3 层棘爆?
	 * ??RETURN 注入里调用，天然避??damage 栈内重入；反伤经 server.execute 再延迟一帧双保险?
	 */
	public static void onFrostspineMeleeHit(ServerPlayer victim, DamageSource source) {
		if (!isMeleeHit(source)) return;
		Entity raw = source.getEntity();
		if (!(raw instanceof LivingEntity attacker) || attacker == victim) return;
		// 白名单：默认白名单内个体不受反刺
		if (WhitelistUtils.isProtected(victim, attacker)) return;
		if (!(victim.level() instanceof ServerLevel sw)) return;
		long now = sw.getGameTime();

		Layers l = LAYERS.computeIfAbsent(attacker.getUUID(), k -> new Layers());
		// 防连击门：同一攻击??10t 内重复命中不叠层??
		// 哨兵判断：首击必须放行，防??now - Long.MIN_VALUE 下溢为负导致反刺永远无法叠??
		if (l.lastHitGameTime != Long.MIN_VALUE && now - l.lastHitGameTime < BAL.i("re_layer_gap_ticks", RE_LAYER_GAP_TICKS)) return;
		l.lastHitGameTime = now;
		l.count++;
		l.lastLayerGameTime = now;

		if (l.count >= BAL.i("burst_at", BURST_AT)) {
			// 棘爆：冻??+ 反伤 + 清??
			l.count = 0;
			final int freezeTicks = BAL.i("burst_freeze_ticks", BURST_FREEZE_TICKS);
			final float burstDamage = (float) BAL.d("burst_damage", BURST_DAMAGE);
			final LivingEntity fAttacker = attacker;
			sw.getServer().execute(() -> {
				if (!fAttacker.isAlive() || fAttacker.isRemoved() || WhitelistUtils.isProtected(victim, fAttacker)) return;
				fAttacker.addEffect(new MobEffectInstance(
						github.com.gengyoubo.sscfa.init.AddonEffects.FROST_FREEZE.get(), freezeTicks, 0, false, true, true), victim);
				// 独??thorn_burst 源保留寒棘狐归属，??NPC 伤害加成读取；专属类型阻止反刺递归??
				fAttacker.hurt(new net.minecraft.world.damagesource.DamageSource(fAttacker.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(
							net.minecraft.core.registries.Registries.DAMAGE_TYPE,
							new net.minecraft.resources.ResourceLocation("my_addon", "thorn_burst"))), victim), burstDamage);
			});
			// 棘爆反馈：碎冰音 + 雪花爆裂粒子（攻击者处?
			sw.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
			sw.sendParticles(ParticleTypes.SNOWFLAKE,
					attacker.getX(), attacker.getY(0.5), attacker.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
			sw.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.7f, 1.0f);
		} else {
			// 叠层反馈：缓?I 2.5s（随层递增显示? 少量雪花粒??
			attacker.addEffect(new MobEffectInstance(
					net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true), victim);
			sw.sendParticles(ParticleTypes.SNOWFLAKE,
					attacker.getX(), attacker.getY(0.5), attacker.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
		}
	}

	/** 每服务端 tick：过期层清理（超时未叠新层清零）。??SscAddonServerEvents 世??tick??*/
	public static void tick(ServerLevel world) {
		if (LAYERS.isEmpty()) return;
		long now = world.getGameTime();
		Iterator<Map.Entry<UUID, Layers>> it = LAYERS.entrySet().iterator();
		while (it.hasNext()) {
			Layers l = it.next().getValue();
			// 哨兵判断??count>0 共同成立才比较，防止下溢为负使清??移除永不触??
			if (l.count > 0 && l.lastLayerGameTime != Long.MIN_VALUE
					&& now - l.lastLayerGameTime > BAL.i("layer_expire_ticks", LAYER_EXPIRE_TICKS)) {
				l.count = 0;
			}
			// 全冷层且长期无命???直接移除??map 膨胀（同理先排除哨兵：从未命中过的条目按已过期处理??
			if (l.count == 0 && (l.lastHitGameTime == Long.MIN_VALUE || now - l.lastHitGameTime > 1200)) {
				it.remove();
			}
		}
	}

	/** 攻击者断?/ 世界卸载清理?*/
	public static void clearPlayer(UUID uuid) {
		LAYERS.remove(uuid);
	}

	/** 服务器停机清理?*/
	public static void clearAll() {
		LAYERS.clear();
	}
}
