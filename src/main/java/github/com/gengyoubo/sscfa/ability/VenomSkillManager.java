package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.SpiderSalticidaePerks;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 璺宠洓娆¤鎶€鑳姐€屾瘨娑层€嶏紙sp_secondary锛夆€斺€?鏈嶅姟绔姸鎬佹満銆?
 *
 * <p><b>鍩虹褰㈡€?/b>锛堟寜涓嬫閿級锛氬鍓嶆柟 2脳2脳2 鏍煎尯鍩熷唴鍏ㄩ儴鐢熺墿閫犳垚 4 鐐归瓟娉曚激瀹?+
 * 涓瘨 I 15 绉掋€?/p>
 *
 * <p><b>涓濈嚎寮哄寲褰㈡€?/b>锛堜富鎶€鑳借烦鏉€鍚庢湁瀹夊叏涓濊繛鐫€鑷繁鏃舵寜娆￠敭锛夛細棰濆鑾峰緱 6 鏍煎墠鍐测€斺€?
 * 娌垮噯鏄熸柟鍚戝啿鍒猴紝閫斾腑鎾炲埌鏁屾柟鐢熺墿閫犳垚棰濆 2 鐐归瓟娉曚激瀹冲苟鍋滀笅锛涘啿鍒虹粨鏉熷悗浠ヨ嚜韬负鍦嗗績
 * 3 鏍煎崐寰?AOE锛? 鐐归瓟娉曚激瀹?+ 涓瘨 II 15 绉掋€?/p>
 *
 * <p>CD 鐢?power form_spider_salticidae_venom 鐨?JSON 瀛楁閰嶇疆銆傜櫧鍚嶅崟锛氶粯璁ょ櫧鍚嶅崟锛堟姢鐜╁ + 瀹犵墿/鍙敜鐗╋級銆傚叏鍒ゅ畾鏈嶅姟绔€?/p>
 */
public final class VenomSkillManager {

	// 浠ヤ笅宸茬櫥璁板父閲忓潎涓洪粯璁ゅ€硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.venom_skill锛?
	private static final float BASE_DAMAGE = 4.0f;        // 鍩虹锛氬墠鏂瑰尯鍩?4 榄旀硶
	private static final int BASE_POISON_DURATION = 300; // 涓瘨 I 15 绉?
	private static final double AREA_SIZE = 3.5;         // 鍓嶆柟 3.5脳3.5脳3.5 鏍硷紙2026-09-29 鑼冨洿 +1.5锛?
	private static final double DASH_DISTANCE = 6.0;     // 涓濈嚎寮哄寲锛氬啿鍒?6 鏍?
	private static final double DASH_SPEED = 1.2;        // 鍐插埡閫熷害
	private static final float DASH_HIT_DAMAGE = 2.0f;   // 鍐插埡纰版挒 2 榄旀硶
	private static final float BURST_DAMAGE = 6.0f;      // 鍐插埡鍚?AOE 6 榄旀硶
	private static final double BURST_RADIUS = 4.5;      // AOE 鍗婂緞 4.5 鏍硷紙2026-09-29 鑼冨洿 +1.5锛?
	private static final int BURST_POISON_DURATION = 300;// AOE 涓瘨 II 15 绉?
	// CD 鐢?power JSON 閰嶇疆锛堣绫绘敞閲婏級锛屽師 CD_TICKS 甯搁噺宸插垹
	private static final int DASH_TIMEOUT = 20;          // 鍐插埡瓒呮椂 1 绉掞紙6 鏍?/ 1.2 姣弔 鈮?5t锛屼綑閲忓厖瓒筹級

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final BalanceReader BAL = new BalanceReader("abilities.venom_skill");
	/** 缁熶竴鍐峰嵈鏈嶅姟鐨勭ǔ瀹氭妧鑳?ID锛堢綉缁滃寘瑙﹀彂鍨嬶級銆?*/
	private static final String SKILL_ID = "my_addon:form_spider_salticidae_venom";

	private static final Map<UUID, DashState> DASHING = new ConcurrentHashMap<>();

	private static final class DashState {
        long castId = -1;
		double damageMultiplier = 1.0;
		double traveled = 0.0;
		int ticks = 0;
		Vec3 dir;
		boolean hitDone;
	}

	private VenomSkillManager() {}

	private static boolean isSalticidae(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE);
	}

	/** 娆￠敭鎸変笅锛氬熀纭€姣掓恫鍖哄煙 / 涓濈嚎寮哄寲鍐插埡銆?*/
	public static void onPress(ServerPlayer player) {
		if (!isSalticidae(player)) return;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return;
		if (DASHING.containsKey(player.getUUID())) return; // 鍐插埡涓笉鍙噸鍏?
		if (!(player.level() instanceof ServerLevel sw)) return;

		// 涓濈嚎寮哄寲鍒ゅ畾锛氬畨鍏ㄤ笣閿氱偣瀛樺湪涓斿湪鏈夋晥绐楀彛鍐?= 銆屾湁涓濈嚎杩炵潃鑷繁銆?
		boolean silkActive = JumpKillManager.hasActiveSilk(player);
		if (!SpiderSalticidaePerks.beginSkill(player, SKILL_ID, true)) return;
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID);
		if (silkActive) {
			startDash(player, sw);
		} else {
			venomArea(player, sw);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID);
		}
	}

	/** 鍩虹褰㈡€侊細鍓嶆柟 2脳2脳2 鍖哄煙姣掓恫鈥斺€? 榄旀硶 + 涓瘨 I 15s锛堟瘨娑茶吅浣擄細绛夌骇+1 / 鏃堕暱脳70%锛夈€?*/
	private static void venomArea(ServerPlayer player, ServerLevel sw) {
		Vec3 look = player.getLookAngle().normalize();
		double areaSize = BAL.d("area_size", AREA_SIZE);
		Vec3 center = player.getEyePosition().add(look.scale(areaSize * 0.75)); // 鍖哄煙涓績鍦ㄨ韩鍓?
		AABB box = new AABB(center.add(-areaSize / 2, -areaSize / 2, -areaSize / 2),
				center.add(areaSize / 2, areaSize / 2, areaSize / 2));
		List<LivingEntity> targets = sw.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !WhitelistUtils.isProtected(player, e));
		boolean gland = github.com.gengyoubo.sscfa.item.VenomGlandItem.isWearingBy(player);
		int amp = gland ? 1 : 0;
		int baseDur = BAL.i("base_poison_duration", BASE_POISON_DURATION);
		int dur = gland ? Math.round(baseDur * github.com.gengyoubo.sscfa.item.VenomGlandItem.DURATION_SCALE) : baseDur;
		for (LivingEntity t : targets) {
			t.hurt(t.damageSources().indirectMagic(player, player), NpcCombatEnhancements.scale(player, t, (float) (BAL.d("base_damage", BASE_DAMAGE) * SpiderSalticidaePerks.venomMultiplier(player)), "salticidae_secondary_damage", 1.5));
			t.addEffect(new MobEffectInstance(MobEffects.POISON, dur, amp, false, true, true), player);
		}
		// 鍙嶉锛氭瘨娑插柗婧呯矑瀛愶紙鍖哄煙涓績锛? 鍠峰悙闊虫晥
		sw.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z,
				24, areaSize / 2, areaSize / 2, areaSize / 2, 0.1);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.5f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 0.8f, 0.6f);
	}

	/** 涓濈嚎寮哄寲锛? 鏍煎啿鍒猴紙鎾炴晫 2 榄旀硶鍋滀笅 鈫?缁撴潫鍚?3 鏍?AOE 6 榄旀硶 + 涓瘨 II 15s锛夈€?*/
	private static void startDash(ServerPlayer player, ServerLevel sw) {
		DashState d = new DashState();
		d.damageMultiplier = SpiderSalticidaePerks.venomMultiplier(player);
        d.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		Vec3 look = player.getLookAngle().normalize();
		d.dir = look;
		DASHING.put(player.getUUID(), d);
		player.fallDistance = 0.0f;
		// 鍐插埡璧锋墜锛氳洓楦?+ 鐮寸┖
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.9f, 1.4f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.2f);
		// 涓濈嚎寮哄寲鎻愮ず锛氭秷鑰椾笣绾匡紙鍐插埡娌夸笣鑽″嚭锛岄敋鐐规竻闄わ級
		JumpKillManager.consumeSilk(player);
	}

	/** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢紙鎺ㄨ繘鍐插埡锛夈€?*/
	public static void tick(ServerPlayer player) {
		DashState d = DASHING.get(player.getUUID());
		if (d == null) return;
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.fullyControlled(player)
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != d.castId) {
			interruptControl(player);
			return;
		}
		if (player.isDeadOrDying() || !isSalticidae(player)) {
            DASHING.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, d.castId);
            return;
        }
		if (!(player.level() instanceof ServerLevel sw)) return;

		d.ticks++;
		// 缁撴潫鏉′欢锛氳窇婊?6 鏍?/ 瓒呮椂 / 鎾炲 / 纰板埌鏁屾柟锛堢鎾炰激瀹冲悗鍋滀笅锛?
		if (d.traveled >= BAL.d("dash_distance", DASH_DISTANCE) || d.ticks > BAL.i("dash_timeout", DASH_TIMEOUT)
				|| player.horizontalCollision || d.hitDone) {
			finishDash(player, sw);
			return;
		}

		// 鍐插埡鎺ㄨ繘锛氭部鍑嗘槦鏂瑰悜锛堣捣璺崇灛闂撮攣瀹氭柟鍚戯紝閫斾腑涓嶈浆鍚戔€斺€旂洿绾垮啿鍒猴級
		double dashSpeed = BAL.d("dash_speed", DASH_SPEED);
		Vec3 v = d.dir.scale(dashSpeed);
		player.setDeltaMovement(v.x, Math.min(0.1, v.y), v.z); // 绔栫洿闄愬箙闃插啿澶?
		player.hurtMarked = true;
		player.fallDistance = 0.0f;
		if (player.connection != null) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		d.traveled += dashSpeed;

		// 閫斾腑纰版挒鍒ゅ畾锛氱鍒版晫鏂圭敓鐗?鈫?2 榄旀硶 + 鍋滀笅
		AABB hitbox = player.getBoundingBox().inflate(0.5);
		List<LivingEntity> hits = sw.getEntitiesOfClass(LivingEntity.class, hitbox,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !WhitelistUtils.isProtected(player, e));
		if (!hits.isEmpty()) {
			for (LivingEntity t : hits) {
				t.hurt(t.damageSources().indirectMagic(player, player),
						NpcCombatEnhancements.scale(player, t, (float) (BAL.d("dash_hit_damage", DASH_HIT_DAMAGE) * d.damageMultiplier), "salticidae_secondary_damage", 1.5));
			}
			d.hitDone = true; // 鏈?tick 缁撴潫鍚庡仠涓?
		}

		// 鍐插埡鎷栧熬锛坥wner 鎵撴爣锛氫粎鏈汉绗竴浜虹О閬胯锛?
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.WITCH, player.getX(), player.getY(0.5), player.getZ(),
				3, 0.15, 0.2, 0.15, 0.05);
	}

	/** 鍐插埡缁撴潫锛氫互鑷韩涓哄渾蹇?3 鏍?AOE鈥斺€? 榄旀硶 + 涓瘨 II 15s锛堟瘨娑茶吅浣擄細绛夌骇+1 / 鏃堕暱脳70%锛夈€?*/
	private static void finishDash(ServerPlayer player, ServerLevel sw) {
        DashState d = DASHING.get(player.getUUID());
        if (d == null) return;
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, d.castId);
		DASHING.remove(player.getUUID());
		Vec3 c = player.position();
		double burstRadius = BAL.d("burst_radius", BURST_RADIUS);
		AABB box = player.getBoundingBox().inflate(burstRadius);
		List<LivingEntity> targets = sw.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& !WhitelistUtils.isProtected(player, e)
						&& e.position().distanceTo(c) <= burstRadius);
		boolean gland = github.com.gengyoubo.sscfa.item.VenomGlandItem.isWearingBy(player);
		int amp = (gland ? 1 : 0) + 1; // 鍩虹涓瘨 II锛岃吅浣?+1
		int baseDur = BAL.i("burst_poison_duration", BURST_POISON_DURATION);
		int dur = gland ? Math.round(baseDur * github.com.gengyoubo.sscfa.item.VenomGlandItem.DURATION_SCALE) : baseDur;
		for (LivingEntity t : targets) {
			t.hurt(t.damageSources().indirectMagic(player, player), NpcCombatEnhancements.scale(player, t, (float) (BAL.d("burst_damage", BURST_DAMAGE) * d.damageMultiplier), "salticidae_secondary_damage", 1.5));
			t.addEffect(new MobEffectInstance(MobEffects.POISON, dur, amp, false, true, true), player);
		}
		// AOE 鍙嶉锛氭瘨鐖嗙矑瀛愮幆 + 濂冲帆娉兼簠
		sw.sendParticles(ParticleTypes.WITCH, c.x, c.y + 0.5, c.z,
				60, burstRadius * 0.7, 0.6, burstRadius * 0.7, 0.2);
		sw.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z,
				16, 0.6, 0.3, 0.6, 0.05);
		sw.playSound(null, c.x, c.y, c.z,
				SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 0.9f);
		sw.playSound(null, c.x, c.y, c.z,
				SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.7f);
	}

	/** 鏂嚎/鍋滄湇娓呯悊銆?*/
	public static void interruptControl(ServerPlayer player) {
		DashState data = DASHING.remove(player.getUUID());
		if (data == null) return;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
	}

	public static void clearPlayer(UUID uuid) {
		DASHING.remove(uuid);
	}

	public static void clearAll() {
		DASHING.clear();
	}
}
