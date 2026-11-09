package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.AxolotlSpPerks;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP 缇庤タ铻?- 婕╂丁钃勫姏鎶€鑳斤紙鏈嶅姟绔姸鎬佹満锛夈€?
 *
 * 鎸?sp_primary 寮€濮嬭搫鍔涳紱姣?10 tick 鎵?8 婀挎鼎鍊?air) 涓斾激瀹?+2锛堝熀纭€ 0锛夛紱
 * 鏈€澶?4 绉?80t / 8 娆?銆佹渶澶ф秷鑰?60 婀挎鼎鍊硷紱涓€斿啀鎸?sp_primary 绔嬪嵆閲婃斁銆佹弧 4 绉掕嚜鍔ㄩ噴鏀俱€?
 * 閲婃斁锛氬崐寰?3 鑼冨洿鐗╃悊浼ゅ = 宸叉墸娆℃暟 脳 2 + 缂撴參 III + 鍑婚€€锛汣D 15 绉?300t) 閲婃斁鍚庢墠璧风畻銆?
 *
 * vortex_state 璧勬簮锛坢y_addon:form_axolotl_sp_vortex_impact_vortex_state锛夌敤浣?钃勫姏涓?鏍囪骞跺悓姝ュ鎴风锛?
 * 0 = 鏈搫鍔涳紝>0 = 钃勫姏涓紙鍊?宸茶搫鍔?tick 鏁帮級銆傚鎴风鎹鍐冲畾鎸夐敭鏄?寮€濮?杩樻槸"閲婃斁"銆?
 */
public final class VortexChargeManager {
	public static final ResourceLocation VORTEX_STATE =
			new ResourceLocation("my_addon", "form_axolotl_sp_vortex_impact_vortex_state");

	/**
	 * 娑℃祦銆岀Щ鍔ㄥ厤鐤€嶅疄浣撶被鍨?tag锛氬懡涓殑瀹炰綋涓嶈娑℃祦鍚搁檮 / 鍑婚€€锛堜粛姝ｅ父鍙椾激锛夈€?
	 * <p>鏁版嵁鍖呰矾寰?{@code data/my_addon/tags/entity_types/vortex_immune.json}锛岄粯璁ゅ惈鍘熺増 6 绫?
	 * boss / 閲嶇敳锛堥搧鍌€鍎÷峰噵鐏德锋湯褰遍緳路娼滃奖璐澛风洃瀹堣€吢疯繙鍙ゅ畧鍗€咃級锛泏@code "replace": false} 鍏佽
	 * 鍏跺畠鏁版嵁鍖?/ 妯＄粍鎶婅嚜瀹?boss 杩藉姞杩涙潵锛屾棤闇€鏀逛唬鐮佸嵆鍙墿灞曞吋瀹广€?
	 */
	private static final TagKey<EntityType<?>> VORTEX_IMMUNE =
			TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("my_addon", "vortex_immune"));

	// 浠ヤ笅鍧囦负榛樿鍊硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.vortex_charge锛?
	private static final int AIR_PER_HIT = 8;
	private static final int MAX_AIR_SPENT = 60;
	private static final int MAX_TICKS = 80;     // 4 绉?
	private static final int HIT_INTERVAL = 10;  // 姣?10 tick 鎵ｄ竴娆?
	private static final int DAMAGE_PER_HIT = 2;
	// CD锛?5 绉掞紝閲婃斁鍚庤捣绠楋級鐢辩粺涓€鍐峰嵈鏈嶅姟 + power JSON 閰嶇疆绠＄悊锛屽師 CD_TICKS 甯搁噺宸插垹
	private static final double RADIUS = 3.0;
	// ===== 钃勫姏鏈熷惛闄勶紙鍞竴鍚搁檮婧愶細鍘?JSON pull_effect 鐨勫浐瀹氬姏搴﹀惛闄勫凡鍒狅紝鏀圭敱姝ゅ鎸夊嚮閫€鎶楁€у垎妗ｇ壍寮曪級=====
	/** 鍚搁檮浣滅敤鍗婂緞锛堜笌鍘?JSON 鍚搁檮瑙﹀強鑼冨洿涓€鑷达級 */
	private static final double PULL_RADIUS = 6.0;
	/** 鍩虹鍚搁檮鍔涘害锛堟湞鍚戠帺瀹剁殑姘村钩閫熷害鍒嗛噺锛涚ǔ鎬侀€熷害鈮?2脳鏈€济楀垎妗ｇ郴鏁帮紝濡傚お寮?澶急璋冩鍊硷級 */
	private static final double PULL_FORCE = 0.6;

	/** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.vortex_charge");
	/** 缁熶竴鍐峰嵈鏈嶅姟鐨勭ǔ瀹氭妧鑳?ID锛堢綉缁滃寘瑙﹀彂鍨嬶級銆?*/
	private static final String SKILL_ID = "my_addon:form_axolotl_sp_vortex_charge";

	/** HUD 闂ㄦ鍚屾簮锛氭瘡娆″懡涓箍娑﹀害娑堣€楋紙balance 鍙皟锛汬UD 灞曠ず鐢級銆?*/
	public static int airPerHitForHud(net.minecraft.world.entity.player.Player player) {
		return AxolotlSpPerks.vortexAirCost(BAL.i("air_per_hit", AIR_PER_HIT), AxolotlSpPerks.hasWaterMagicEfficiency(player));
	}

	/** HUD 杈呭姪鏍忓悓婧愶細钃勫姏鎬绘椂闀夸笂闄愶紙balance 鍙皟锛汬UD 娑ㄦ潯鍒嗘瘝锛屼粠灏忓埌澶э級銆?*/
	public static int maxTicksForHud() { return BAL.i("max_ticks", MAX_TICKS); }

	// ===== 鍔ㄦ€佺矑瀛愶紙闈掕摑/鐧斤細钃勫姏鍚搁檮 + 閲婃斁鎶涚墿绾匡紝鍏ㄩ儴鏈嶅姟绔敓鎴愬苟骞挎挱缁欐墍鏈夊鎴风锛?=====
	/** 闈掕摑鑹插皹鍩冿紙婕傛诞锛屽惛闄勪笌鎵╂暎鐫€鑹茬敤锛?*/
	private static final net.minecraft.core.particles.DustParticleOptions CYAN_DUST =
			new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.20f, 0.62f, 0.92f), 1.6f);
	/** 鐧借壊灏樺焹锛堟紓娴紝鍚搁檮鐢級 */
	private static final net.minecraft.core.particles.DustParticleOptions WHITE_DUST =
			new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.92f, 0.96f, 1.0f), 1.3f);

	private static final Map<UUID, ChargeState> CHARGING = new ConcurrentHashMap<>();

	private static final class ChargeState {
        long castId = -1;
		int ticks = 0;
		int hits = 0;
		int airSpent = 0;
		int baseAirSpent = 0;
		boolean discounted;
	}

	private VortexChargeManager() {
	}

	public static boolean isCharging(ServerPlayer player) {
		return CHARGING.containsKey(player.getUUID());
	}

	/** Only an enemy NPC currently inside the actual pull area loses its damage against this caster. */
	public static boolean suppressesNpcAttack(ServerPlayer player, Entity attacker) {
		return isCharging(player) && player.isAlive() && FormUtils.isAxolotlSP(player)
				&& !player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get()) && attacker instanceof LivingEntity npc
				&& NpcCombatEnhancements.isEnemyNpc(player, npc) && getMovementForceScale(npc) > 0.0
				&& player.getBoundingBox().inflate(BAL.d("pull_radius", PULL_RADIUS)).intersects(npc.getBoundingBox());
	}

	/**
	 * 瀹㈡埛绔湰鍦扮帺瀹躲€屾丁娴佽搫鍔涗腑銆嶇紦瀛樻爣璁帮細鐢?VortexChargeClient 姣忓鎴风 tick 鏇存柊涓€娆★紝
	 * 渚涚鎾炴帹鎸?mixin锛圫scAddonLivingEntityMixin.pushAwayFrom锛夊揩閫熻鍙栵紝閬垮厤姣忔瀹炰綋纰版挒閮借 Apoli 璧勬簮銆?
	 * 浠呭鎴风鏈夋剰涔夛紙鏈嶅姟绔亽 false锛屾湇鍔＄璧?{@link #isCharging} 鏌ヨ〃锛夈€?
	 */
	private static volatile boolean clientLocalCharging = false;

	/** 瀹㈡埛绔瘡 tick 鏇存柊鏈湴鐜╁钃勫姏鏍囪銆?*/
	public static void setClientLocalCharging(boolean charging) {
		clientLocalCharging = charging;
	}

	/** 纰版挒鎺ㄦ尋 mixin 璇诲彇锛氬鎴风鏈湴鐜╁鏄惁澶勪簬娑℃祦钃勫姏銆?*/
	public static boolean isClientLocalCharging() {
		return clientLocalCharging;
	}

	/** 瀹㈡埛绔彂銆屽紑濮嬭搫鍔涖€嶅寘鏃惰皟鐢ㄣ€?*/
	public static void start(ServerPlayer player) {
		if (CHARGING.containsKey(player.getUUID())) return;
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) return;
		if (!FormUtils.isAxolotlSP(player)) return;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return;
		if (player.getAirSupply() < airPerHitForHud(player)) return; // 涓嶩UD浣跨敤鍚屼竴瀹為檯璧锋墜璐圭敤
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return;
		ChargeState s = new ChargeState();
		s.discounted = AxolotlSpPerks.hasWaterMagicEfficiency(player);
        s.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
        CHARGING.put(player.getUUID(), s);
		PowerUtils.setResourceValueAndSync(player, VORTEX_STATE, 1); // 鏍囪钃勫姏涓紙瀹㈡埛绔 >0锛?
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.5f, 0.5f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6f, 1.5f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.6, 0.6, 0.6);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
		// 闈?鐧界矑瀛愬悜涓績鍚搁檮锛堟缉娑¤捣鎵嬶級
		spawnAbsorbRing(sw, player, player.getX(), player.getY() + 1, player.getZ(), 16, 0.0);
	}

	/** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢ㄣ€?*/
	public static void tick(ServerPlayer player) {
		ChargeState s = CHARGING.get(player.getUUID());
		if (s == null) return;
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			onPurified(player);
			return;
		}
		if (player.isDeadOrDying() || !FormUtils.isAxolotlSP(player)) {
			cancel(player); // 褰㈡€佷涪澶?姝讳骸 鈫?鍙栨秷锛屼笉缁撶畻
			return;
		}
		s.ticks++;
		int hitInterval = BAL.i("hit_interval", HIT_INTERVAL);
		int maxTicks = BAL.i("max_ticks", MAX_TICKS);
		// 鎸佺画鍚搁檮婕╂丁锛堟瘡 2 tick 涓€鍦堬紝鐩镐綅闅忔椂闂存棆杞?鈫?鍔ㄦ€佹敹鏉燂級
		if (s.ticks % 2 == 0) {
			spawnAbsorbRing((ServerLevel) player.level(), player,
					player.getX(), player.getY() + 1, player.getZ(), 8, s.ticks * 0.35);
			// 钃勫姏鏈熷疄浣撳惛闄勶細鎶婅寖鍥村唴鎬墿鏈濈帺瀹剁壍寮曪紝鍔涘害闅忓嚮閫€鎶楁€ц“鍑忥紙姣忕骇 -20%锛屽厤鐤殑鍚镐笉鍔級
			pullEntitiesDuringCharge((ServerLevel) player.level(), player);
		}
		if (s.ticks % hitInterval == 0) {
			int airPerHit = BAL.i("air_per_hit", AIR_PER_HIT);
			int maxAirSpent = BAL.i("max_air_spent", MAX_AIR_SPENT);
			int baseSpend = Math.min(airPerHit, Math.max(0, maxAirSpent - s.baseAirSpent));
			int spend = AxolotlSpPerks.vortexAirCost(s.baseAirSpent + baseSpend, s.discounted) - s.airSpent;
			// 鏈姞Perk鏃朵繚鐣欏師闂ㄦ锛涘姞Perk鏃朵互鏈绱璐﹀崟鐨勫疄闄呭閲忔牎楠屻€?
			int required = s.discounted ? spend : airPerHit;
			if (s.baseAirSpent < maxAirSpent && player.getAirSupply() >= required) {
				player.setAirSupply(player.getAirSupply() - spend);
				s.airSpent += spend;
				s.baseAirSpent += baseSpend;
				s.hits++;
				// 钃勫姏杩涘害鍚屾锛堝€?宸茶搫鍔?tick 鏁帮紱HUD 涓绘妧鑳借緟鍔╂爮鎹浠庡皬鍒板ぇ娑ㄦ潯锛?
				PowerUtils.setResourceValueAndSync(player, VORTEX_STATE, s.ticks);
				ServerLevel sw = (ServerLevel) player.level();
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE,
						player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.6, 0.6, 0.6);
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.8f, 0.6f);
			} else {
				release(player); // air 涓嶈冻鎴栧凡鎵ｆ弧 60 鈫?鑷姩閲婃斁
				return;
			}
		}
		// 娉細vortex_state 涓嶅啀閫?tick 鍚屾鈥斺€擩SON 鏉′欢鍙銆?0銆嶏紱
		// start(1)/姣忔钃勫姏璺冲彉(杩涘害鍊?/release(0)/cancel(0) 鍚屾宸茶冻澶燂紝HUD 杈呭姪鏍忚杩涘害鍊间粠灏忓埌澶ф定鏉°€?
		if (s.ticks >= maxTicks) {
			release(player); // 婊?4 绉掕嚜鍔ㄩ噴鏀?
		}
	}

	/** 瀹㈡埛绔彂銆岄噴鏀俱€嶅寘 鎴?鑷姩閲婃斁鏃惰皟鐢ㄣ€?*/
	public static void release(ServerPlayer player) {
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			onPurified(player);
			return;
		}
		ChargeState s = CHARGING.remove(player.getUUID());
		PowerUtils.setResourceValueAndSync(player, VORTEX_STATE, 0);
		if (s == null) return;
		// 0 钃勫姏鏉炬墜鎸夊彇娑堝鐞嗭細涓嶈繘 CD
		if (s.hits <= 0) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
			return;
		}
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, s.castId);
		double radius = BAL.d("radius", RADIUS);
		int damage = s.hits * BAL.i("damage_per_hit", DAMAGE_PER_HIT);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1.5f, 0.5f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(),
				150, radius, 1.0, radius, 1.0);
		// owner 鎵撴爣锛氫粎鏈汉绗竴浜虹О閬胯锛屼粬浜鸿瑙掑師鏍?
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(),
				8, radius * 0.5, 0.5, radius * 0.5, 0.1);
		// 浠?RC-4 鑽按鐮寸鐨勬按鑺辩垎寮€锛堜笌姘寸煕钀藉湴鍚屾锛?
		github.com.gengyoubo.sscfa.util.ParticleUtils.spawnWaterBurst(sw, player, player.getX(), player.getY() + 1, player.getZ(), 1.3);
		if (damage <= 0) return; // 涓€娆￠兘娌¤搫鍒帮紝浠呭彇娑?
		AABB box = player.getBoundingBox().inflate(radius);
		for (Entity e : sw.getEntities(player, box)) {
			if (!(e instanceof LivingEntity living)) continue;
			// 榛樿鐧藉悕鍗曪細璞佸厤鐜╁/瀹犵墿/鐧藉悕鍗曚釜浣擄紝涓嶅彈娑℃祦鍐插嚮浼ゅ涓庢帶鍒?
			if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(player, living)) continue;

			// 浼ゅ锛氭墍鏈夊彈褰卞搷鐢熺墿涓€寰嬫弧棰濓紙鐢ㄦ埛瀹氱銆屼激瀹虫甯搞€嶏紝boss 涔熺収甯稿彈浼わ級
			living.hurt(player.damageSources().mobAttack(player), (float) damage);

			// 鍑婚€€鍔涘害 = f(鍑婚€€鎶楁€х瓑绾? 鏄惁涓篵oss)锛歜oss 鈫?0锛堜笉鍑婚€€涓嶇紦鎱級锛屾櫘閫氭€寜鎶楁€у垎妗?
			double scale = getMovementForceScale(living);
			if (scale <= 0.0) continue;              // boss / 鏋侀珮鎶楁€э細鍙彈浼わ紝涓嶈绉诲姩
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), player);
			Vec3 push = living.position().subtract(player.position());
			if (push.lengthSqr() < 1.0e-4) push = new Vec3(0, 1, 0);
			push = push.normalize().scale(0.8 * scale);
			living.setDeltaMovement(push.x, 0.6 * scale, push.z);
			living.hurtMarked = true;
		}
	}

	/** 鍑€鍖栫珛鍗虫挙閿€钃勫姏锛屼笉浜х敓鍐插嚮锛屽苟浠庡綋鍓嶆椂鍒昏捣杩涘叆瀹屾暣鎶€鑳?CD銆?*/
	public static void onPurified(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
		if (s == null) return;
		PowerUtils.setResourceValueAndSync(player, VORTEX_STATE, 0);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.interruptedFull(player, SKILL_ID, s.castId);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
	}

	/** 鍙栨秷钃勫姏锛堜笉缁撶畻浼ゅ銆佷笉杩?CD锛夈€?*/
	public static void cancel(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
        if (s != null) {
			PowerUtils.setResourceValueAndSync(player, VORTEX_STATE, 0);
		}
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
	}

	// ==================== Boss 鍒ゅ畾 + 绉诲姩鍔涘害绯绘暟 ====================

	/**
	 * 鍒ゅ畾涓€涓敓鐗╂槸鍚︺€屾丁娴佺Щ鍔ㄥ厤鐤€嶏紙涓嶈鍚搁檮 / 鍑婚€€锛屼絾浠嶆甯稿彈浼わ級銆?
	 * <p>鍙岄噸鍒ゅ畾锛?
	 * <ol>
	 *   <li><b>鍑婚€€鎶楁€?鈮?0.99</b>锛氳嚜鍔ㄥ懡涓搧鍌€鍎÷风洃瀹堣€吢蜂互鍙婅浜嗛珮鎶楁€х殑妯＄粍 boss锛屾棤闇€鐧昏銆?/li>
	 *   <li><b>瀹炰綋绫诲瀷 tag {@code my_addon:vortex_immune}</b>锛氳ˉ浣忓師鐗堝嚮閫€鎶楁€?0.0 鍗村簲鍏嶇柅鐨?
	 *       鍑嬬伒路鏈奖榫櫬锋綔褰辫礉路杩滃彜瀹堝崼鑰咃紙榛樿宸插啓杩?tag锛夛紝骞跺厑璁告ā缁勬妸鑷 boss 杩藉姞杩?tag 鎵╁睍鍏煎銆?/li>
	 * </ol>
	 */
	private static boolean isVortexImmune(LivingEntity living) {
		return getKnockbackResistance(living) >= 0.99 || living.getType().is(VORTEX_IMMUNE);
	}

	/**
	 * 璁＄畻鐩爣鍙楁丁娴併€岀Щ鍔ㄧ被鏁堟灉銆嶏紙钃勫姏鍚搁檮 / 閲婃斁鍑婚€€锛夌殑鍔涘害绯绘暟 [0.0, 1.0]銆?
	 * <p>渚濇嵁鐢ㄦ埛瀹氱鐨勪袱椤瑰垽鎹細
	 * <ul>
	 *   <li><b>鏄惁涓?boss</b>锛歿@link #isVortexImmune} 鍛戒腑 鈫?杩斿洖 0.0锛堝畬鍏ㄤ笉琚Щ鍔紝鍔涘害褰掗浂锛夈€?/li>
	 *   <li><b>鍑婚€€鎶楁€х瓑绾?/b>锛氭櫘閫氭€寜銆屾瘡 0.2 鐐规姉鎬т竴绾с€佹瘡绾?-20% 鍔涘害銆嶅垎妗?
	 *       鈥斺€?0.0鈫?.0銆?.2鈫?.8銆?.4鈫?.6銆?.6鈫?.4銆?.8鈫?.2銆佲墺1.0鈫?.0銆?/li>
	 * </ul>
	 * 鍚搁檮涓庡嚮閫€鍏辩敤姝ょ郴鏁帮紝淇濊瘉涓ゅ鍔涘害鍙ｅ緞涓€鑷淬€備激瀹充笉璧版绯绘暟锛堢敱璋冪敤鏂规寜銆屼激瀹虫甯搞€嶆弧棰濈粨绠楋級銆?
	 */
	private static double getMovementForceScale(LivingEntity living) {
		if (isVortexImmune(living)) return 0.0;
		int level = (int) Math.ceil(getKnockbackResistance(living) / 0.2); // 0鈫?, 0.2鈫? ... 1.0鈫?
		if (level > 5) level = 5;
		return Math.max(0.0, 1.0 - level * 0.2);
	}

	// ==================== 钃勫姏鏈熷疄浣撳惛闄?====================

	/**
	 * 钃勫姏鏈熸妸鑼冨洿鍐呮€墿鏈濈帺瀹剁壍寮曘€?
	 * <p>鍚搁檮鍔涘害渚濇嵁 {@link #getMovementForceScale}锛堝嚮閫€鎶楁€х瓑绾?+ 鏄惁涓?boss锛夛細
	 * <ul>
	 *   <li>Boss / 閲嶇敳瀹炰綋锛堥搧鍌€鍎÷峰噵鐏德锋湯褰遍緳路鐩戝畧鑰吢锋綔褰辫礉路杩滃彜瀹堝崼鑰吢烽珮鎶楁€фā缁?boss锛夆啋 鍔涘害 0锛屼笉琚惛闄勩€?/li>
	 *   <li>鏅€氭€寜鍑婚€€鎶楁€с€屾瘡 0.2 鐐逛竴绾с€佹瘡绾?-20% 鍔涘害銆嶅垎妗ｈ“鍑忋€?
	 *       娉細setVelocity 鐩存帴瑕嗙洊閫熷害涓嶇粡杩?takeKnockback锛屾晠鍑婚€€鎶楁€т笉鑳借嚜鐒跺厬搴曪紝闈犲姏搴︾郴鏁版樉寮忚“鍑忋€?/li>
	 *   <li>鐜╁ / 椹湇瀹犵墿 / 鐧藉悕鍗曚釜浣撹眮鍏嶏紙{@link WhitelistUtils#isProtected}锛夈€?/li>
	 * </ul>
	 * 姣?2 tick 鏂藉姞涓€娆℃湞鍚戠帺瀹剁殑姘村钩閫熷害锛岃创鑴搁槇鍊煎唴涓嶅啀鎷夎繎锛堥槻闇囪崱锛夈€?
	 */
	private static void pullEntitiesDuringCharge(ServerLevel sw, ServerPlayer player) {
		double pullRadius = BAL.d("pull_radius", PULL_RADIUS);
		double pullForce = BAL.d("pull_force", PULL_FORCE);
		AABB box = player.getBoundingBox().inflate(pullRadius);
		Vec3 playerPos = player.position();
		for (Entity e : sw.getEntities(player, box)) {
			if (!(e instanceof LivingEntity living)) continue;
			// 鐧藉悕鍗?/ 鐜╁ / 瀹犵墿璞佸厤
			if (WhitelistUtils.isProtected(player, living)) continue;

			// 鍚搁檮鍔涘害 = f(鍑婚€€鎶楁€х瓑绾? 鏄惁涓篵oss)锛歜oss 鈫?0锛堜笉鍚革級锛屾櫘閫氭€寜鎶楁€у垎妗?
			double scale = getMovementForceScale(living);
			if (scale <= 0.0) continue;              // 鍔涘害褰掗浂锛歜oss 鎴栨瀬楂樻姉鎬э紝鍚镐笉鍔?

			Vec3 toPlayer = playerPos.subtract(living.position());
			// 涓嶈璐磋劯闃堝€硷細鍏佽鎬墿琚惛鍒扮帺瀹惰韩涓婂悗鍙嶅闇囪崱锛堢壒鑹叉晥鏋滐紝鐢ㄦ埛瀹氱淇濈暀锛?
			// 鏈濆悜鐜╁鐨勬按骞虫柟鍚戯紙蹇界暐 Y锛岄伩鍏嶆妸鎬惛鍒板ぉ涓?/ 鍦颁笅锛夛紱normalize 瀵归浂鍚戦噺杩斿洖 ZERO锛屾棤 NaN 椋庨櫓
			Vec3 dir = new Vec3(toPlayer.x, 0, toPlayer.z).normalize();
			double force = pullForce * scale;
			// 鍙犲姞鏈濆悜鐜╁鐨勬按骞抽€熷害锛堜笉瑕嗙洊鍘熸湁 Y锛屼繚鐣欓噸鍔?/ 璺宠穬锛?
			living.setDeltaMovement(living.getDeltaMovement().x * 0.5 + dir.x * force,
					living.getDeltaMovement().y,
					living.getDeltaMovement().z * 0.5 + dir.z * force);
			living.hurtMarked = true;
		}
	}

	public static void onPlayerDisconnect(UUID uuid) {
		CHARGING.remove(uuid);
	}

	// ==================== 鍑婚€€鎶楁€ц鍙?====================

	/**
	 * 璇诲彇涓€涓敓鐗╃殑鍑婚€€鎶楁€э紙0.0~1.0锛夈€?
	 * <p>1.20.1 鐨?LivingEntity 鏃?getKnockbackResistance() 鏂规硶锛岄』璧板睘鎬у疄渚嬭鍙栥€?
	 * 瀹炰綋鏈敞鍐岃灞炴€ф椂杩斿洖 0.0锛堝彲姝ｅ父琚嚮閫€/鍚搁檮锛夈€?
	 */
	private static double getKnockbackResistance(LivingEntity living) {
		if (living.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) {
			net.minecraft.world.entity.ai.attributes.AttributeInstance krInst =
					living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
			if (krInst != null) return krInst.getValue();
		}
		return 0.0;
	}

	// ==================== 绮掑瓙杈呭姪 ====================

	/** 鐢熸垚涓€涓甫閫熷害鐨勬湁鍚戠矑瀛愶紙count=0 鏃?delta 鍗充负閫熷害鍚戦噺锛宻peed=1锛夈€?*/
	private static void spawnDirected(ServerLevel sw, ServerPlayer owner, net.minecraft.core.particles.ParticleOptions particle,
			double x, double y, double z, double vx, double vy, double vz) {
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, owner, particle, x, y, z, 0, vx, vy, vz, 1.0);
	}

	/** 钃勫姏鏈燂細鍦ㄥ鍦堢敓鎴愰潚/鐧界矑瀛愶紝閫熷害鎸囧悜涓績骞跺甫鍒囧悜鍒嗛噺 鈫?鍚戝唴鍚搁檮 + 鏃嬭浆婕╂丁銆?*/
	private static void spawnAbsorbRing(ServerLevel sw, ServerPlayer owner, double cx, double cy, double cz, int count, double phase) {
		double r = 2.6;
		for (int i = 0; i < count; i++) {
			double ang = (Math.PI * 2 / count) * i + phase;
			double px = cx + Math.cos(ang) * r;
			double pz = cz + Math.sin(ang) * r;
			double py = cy + 0.2 + (i % 4) * 0.28;
			double inX = (cx - px) * 0.20;
			double inZ = (cz - pz) * 0.20;
			double tanX = -Math.sin(ang) * 0.10;
			double tanZ = Math.cos(ang) * 0.10;
			spawnDirected(sw, owner, (i & 1) == 0 ? CYAN_DUST : WHITE_DUST,
					px, py, pz, inX + tanX, 0.04, inZ + tanZ);
		}
	}
}
