package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SP鎮︾伒缇や綋娌荤枟 - 鐧藉悕鍗曟不鐤楃郴缁?
 * <p>
 * 褰揓SON钃勫姏瀹屾垚鍚庤缃?heal_execute 璧勬簮涓?锛?
 * 姝ょ被鐨則ick妫€娴嬪埌鍚庢墽琛岀櫧鍚嶅崟杩囨护娌荤枟锛岀劧鍚庨噸缃负0銆?
 * <p>
 * 鐧藉悕鍗曞瓨鍌ㄥ湪SP鎮︾伒鐜╁鐨?command tags 涓紝
 * 鏍煎紡: "ssc_allay_wl:<鐩爣鐜╁UUID>"
 */
public class AllaySPGroupHeal {

	public static final String WHITELIST_TAG_PREFIX = "ssc_allay_wl:";
	private static final ResourceLocation HEAL_EXECUTE_ID = FormIdentifiers.ALLAY_GROUP_HEAL_EXECUTE;
	private static final ResourceLocation CHARGING_STATE_ID = new ResourceLocation("my_addon", "form_allay_sp_group_heal_charging_state");
	private static final ResourceLocation CHARGE_TIMER_ID = new ResourceLocation("my_addon", "form_allay_sp_group_heal_charge_timer");
	private static final String SKILL_ID = "my_addon:form_allay_sp_group_heal_key_activation";
	private static final ResourceLocation SOLO_DAMAGE_TIMER_ID = FormIdentifiers.ALLAY_GROUP_HEAL_SOLO_DAMAGE_TIMER;
	// 缇ょ枟鍙傛暟锛氶粯璁や笌娉ㄩ噴涓€鑷达紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.allay_sp_group_heal锛?
	private static final double HEAL_RADIUS = 20.0;
	private static final int RESISTANCE_TICKS = 200;
	private static final int SOLO_BLESSING_TICKS = 400;
	// 缇や綋娌荤枟锛氭瘡涓洰鏍囧洖琛€ = 鏈€澶х敓鍛矫?5%锛岄澶栫粰 鏈€澶х敓鍛矫?0% 鐨勯粍蹇冿紝鎸佺画30绉?
	private static final float HEAL_PERCENT = 0.75f;
	private static final float ABSORPTION_PERCENT = 0.5f;
	private static final int HEAL_ABSORPTION_TICKS = 600;

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_group_heal");

	private AllaySPGroupHeal() {
		throw new UnsupportedOperationException("This class cannot be instantiated.");
	}

	/**
	 * 姣弔ick妫€鏌ユ槸鍚﹂渶瑕佹墽琛屾不鐤?
	 */
	public static void tick(ServerPlayer player) {
		// 寤変环褰㈡€侀棬鎺у墠缃細闈?SP 鎮︾伒鐜╁鐩存帴璺宠繃锛岄伩鍏嶅鍏ㄥ憳姣?tick 鍋?Apoli 璧勬簮鏌ヨ
		// 锛坓etResourceValue 姣忔閮借閬嶅巻鐜╁鍏ㄩ儴 power 鍒楄〃锛岄潪鎸佹湁鑰?100% 娴垂锛?
		if (!github.com.gengyoubo.sscfa.util.FormUtils.isForm(player, FormIdentifiers.ALLAY_SP)) {
			return;
		}
		if (SkillBlocker.isSkillBlocked(player, "allay", "group_heal")) {
			return;
		}
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			onPurified(player);
			return;
		}
		int healExecute = getResourceValue(player, HEAL_EXECUTE_ID);
		if (healExecute != 1) return;

		// 绔嬪嵆閲嶇疆瑙﹀彂鍣紙涓嶅悓姝ワ紝閬垮厤閲嶇疆椋樻诞璁℃椂鍣級
		setResourceValueNoSync(player, HEAL_EXECUTE_ID, 0);

		// 鎵ц鐧藉悕鍗曡繃婊ゆ不鐤?
		executeWhitelistHeal(player);
	}

	/** 鍑€鍖栧彧涓柇缇や綋娌荤枟锛涗笉鏀瑰彉鍏跺畠鎮︾伒缇や綋鍑€鍖栫殑钃勫姏璧勬簮銆?*/
	public static void onPurified(ServerPlayer player) {
		long castId = stopCharge(player);
		if (castId >= 0) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.interruptedFull(player, SKILL_ID, castId);
	}

	public static void interruptControl(ServerPlayer player) {
		long castId = stopCharge(player);
		if (castId >= 0) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, castId);
	}

	private static long stopCharge(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.util.FormUtils.isForm(player, FormIdentifiers.ALLAY_SP)) return -1;
		if (getResourceValue(player, CHARGING_STATE_ID) <= 0 && getResourceValue(player, HEAL_EXECUTE_ID) <= 0) return -1;
		long castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		PowerUtils.setResourceValueAndSync(player, CHARGING_STATE_ID, 0);
		PowerUtils.setResourceValueAndSync(player, CHARGE_TIMER_ID, 0);
		PowerUtils.setResourceValueAndSync(player, HEAL_EXECUTE_ID, 0);
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
		return castId;
	}

	/**
	 * 鎵ц鐧藉悕鍗曡繃婊ょ殑缇や綋娌荤枟
	 * 鐧藉悕鍗曚负绌烘椂娌荤枟鎵€鏈夋椿浣擄紙鍘熺増琛屼负锛夛紱鏈夌櫧鍚嶅崟鏃跺彧娌荤枟鐧藉悕鍗曞唴鐨勭帺瀹跺強鍏跺彫鍞ょ墿
	 */
	private static void executeWhitelistHeal(ServerPlayer allayPlayer) {
		ServerLevel world = (ServerLevel) allayPlayer.level();
		boolean soloHeal = !hasOtherHealablePlayer(allayPlayer, world);
		// balance 杩愯鏃跺弬鏁帮紙鍚屾柟娉曞娆′娇鐢紝鍏堝彇灞€閮ㄥ彉閲忥級
		double healRadius = BAL.d("heal_radius", HEAL_RADIUS);
		double healRadiusSq = healRadius * healRadius;
		int resistanceTicks = BAL.i("resistance_ticks", RESISTANCE_TICKS);

		// 娌荤枟鑷韩
		if (soloHeal) {
			applySoloBlessing(allayPlayer);
		} else {
			healTarget(allayPlayer);
			allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceTicks, 0, false, true, true));
		}
		spawnHealParticles(world, allayPlayer);
		// 鍙湁SP鎮︾伒鑷繁鑳藉惉瑙?
		allayPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);

		// 鑾峰彇鑼冨洿鍐呮墍鏈夋椿浣?
		AABB box = allayPlayer.getBoundingBox().inflate(healRadius);
		List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box,
					e -> e != allayPlayer && e.isAlive() && e.distanceToSqr(allayPlayer) <= healRadiusSq);

		for (LivingEntity entity : entities) {
			// 缁熶竴璧?WhitelistUtils.isBuffTarget锛屽悓鏃堕伒浠庢湇鍔＄ whitelistEnabled 鎬诲紑鍏?
			if (WhitelistUtils.isBuffTarget(allayPlayer, entity)) {
				healTarget(entity);
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistanceTicks, 0, false, true, true));
				spawnHealParticles(world, entity);
				// 鎾斁澹伴煶锛氭不鐤楄€呭惉瑙佺鏈夊０闊筹紝鍏朵粬浜哄惉瑙佺┖闂村０闊?
				allayPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
				world.playSound(allayPlayer, entity.getX(), entity.getY(), entity.getZ(),
						SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
			}
		}
	}

	private static boolean hasOtherHealablePlayer(ServerPlayer allayPlayer, ServerLevel world) {
		double healRadius = BAL.d("heal_radius", HEAL_RADIUS);
		double healRadiusSq = healRadius * healRadius;
		AABB box = allayPlayer.getBoundingBox().inflate(healRadius);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, box,
				player -> player != allayPlayer
						&& player.isAlive()
						&& !player.isSpectator()
						&& player.distanceToSqr(allayPlayer) <= healRadiusSq
						&& WhitelistUtils.isBuffTarget(allayPlayer, player));
		return !players.isEmpty();
	}

	private static void applySoloBlessing(ServerPlayer allayPlayer) {
		int soloBlessingTicks = BAL.i("solo_blessing_ticks", SOLO_BLESSING_TICKS);
		healTarget(allayPlayer);
		allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, soloBlessingTicks, 1, false, true, true));
		allayPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, soloBlessingTicks, 1, false, true, true));
		setResourceValue(allayPlayer, SOLO_DAMAGE_TIMER_ID, soloBlessingTicks);
	}

	/** 娌荤枟鍗曚釜鐩爣锛氬洖琛€=鏈€澶х敓鍛矫?5%锛屽苟缁欎簣 鏈€澶х敓鍛矫?0% 鐨勯粍蹇冿紙鎸佺画30绉掞級銆?*/
	private static void healTarget(LivingEntity entity) {
		float maxHp = entity.getMaxHealth();
		entity.heal(maxHp * (float) BAL.d("heal_percent", HEAL_PERCENT));
        float absorption = maxHp * (float) BAL.d("absorption_percent", ABSORPTION_PERCENT);
        if (absorption > 0) {
            // Vanilla absorption grants 4 HP per amplifier level; round to its available granularity.
            int amplifier = Math.max(0, Math.round(absorption / 4.0f) - 1);
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    BAL.i("heal_absorption_ticks", HEAL_ABSORPTION_TICKS), amplifier, false, true, true));
        }
	}

	/**
	 * 灏嗙洰鏍囧疄浣撶殑鎵€鏈夎€咃紙濡傛灉鏄疇鐗╋級鎴栧疄浣撴湰韬紙濡傛灉鏄櫘閫氱敓鐗?鐜╁锛夊姞鍏ョ櫧鍚嶅崟
	 */
	public static void addToWhitelist(ServerPlayer allayPlayer, LivingEntity target) {
		if (target instanceof net.minecraft.world.entity.player.Player) {
			// 鐜╁锛氬彧鍐?player 鍓嶇紑
			allayPlayer.getTags().add(WHITELIST_TAG_PREFIX + target.getUUID().toString());
		} else {
			// 浠讳綍闈炵帺瀹剁敓鐗╋紙鍚疇鐗╋級锛氬弻鍐?player 鍓嶇紑锛堝吋瀹?isInWhitelist 鍒ゅ畾锛? mob 鍓嶇紑锛圙UI 鐢紝甯︾墿绉嶄俊鎭級
			// 娉ㄦ剰锛氱櫧鍚嶅崟鐜╁鐨勬墍鏈夊疇鐗╁凡閫氳繃"涓讳汉 UUID 鍦?player 鐧藉悕鍗?璺緞榛樿鍙椾繚鎶わ紝
			// 杩欓噷鍐欏疇鐗╄嚜韬?UUID 浠呭湪璇ュ疇鐗╄鏄惧紡鏍囪鏃剁敓鏁堬紝涓嶅奖鍝嶄富浜虹殑鍏跺畠瀹犵墿銆?
			String uuidStr = target.getUUID().toString();
			allayPlayer.getTags().add(WHITELIST_TAG_PREFIX + uuidStr);
			net.minecraft.resources.ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
			String mobTag = github.com.gengyoubo.sscfa.util.WhitelistUtils.WHITELIST_MOB_TAG_PREFIX
					+ uuidStr + ":" + typeId.getNamespace() + ":" + typeId.getPath();
			allayPlayer.getTags().add(mobTag);
		}
	}

	/**
	 * 灏嗙洰鏍囧疄浣撶殑鎵€鏈夎€咃紙濡傛灉鏄疇鐗╋級鎴栧疄浣撴湰韬紙濡傛灉鏄櫘閫氱敓鐗?鐜╁锛変粠鐧藉悕鍗曠Щ闄?
	 */
	public static void removeFromWhitelist(ServerPlayer allayPlayer, LivingEntity target) {
		// 濮嬬粓鎸?target 鑷韩 UUID 绉婚櫎锛堜笌 addToWhitelist 瀵圭О锛?
		allayPlayer.getTags().remove(WHITELIST_TAG_PREFIX + target.getUUID().toString());
		// 鍚屾椂娓呴櫎鐢熺墿鐧藉悕鍗曚笂鐨?mob 鍓嶇紑 tag锛堝寘鍚墿浣欑殑 :ns:path 鍙樹綋锛?
		github.com.gengyoubo.sscfa.util.WhitelistUtils.removeMobFromWhitelist(allayPlayer, target.getUUID());
	}

	/**
	 * 妫€鏌UID鏄惁鍦ㄧ櫧鍚嶅崟涓?
	 */
	public static boolean isInWhitelist(Set<String> allayTags, UUID targetUuid) {
		return allayTags.contains(WHITELIST_TAG_PREFIX + targetUuid.toString());
	}

	/**
	 * 渚挎嵎鏂规硶锛氭鏌ヤ竴涓疄浣撴槸鍚﹀湪鐜╁鐨勭櫧鍚嶅崟涓紙渚涘敱鐗囨満绛夊閮ㄧ郴缁熶娇鐢級
	 * 鐧藉悕鍗曚负绌烘椂杩斿洖true锛堝厑璁告墍鏈夌帺瀹跺拰鍙嬪ソ鐢熺墿锛?
	 */
	public static boolean isInWhitelist(ServerPlayer allayPlayer, LivingEntity entity) {
		// 缁熶竴璧?WhitelistUtils.isBuffTarget锛岄伒浠?whitelistEnabled 鎬诲紑鍏?
		return WhitelistUtils.isBuffTarget(allayPlayer, entity);
	}

	/**
	 * 鍦ㄥ疄浣撲綅缃敓鎴愮埍蹇冪矑瀛?
	 */
	private static void spawnHealParticles(ServerLevel world, LivingEntity entity) {
		github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(world, ParticleTypes.HEART,
				entity.getX(), entity.getY() + entity.getBbHeight() + 0.5, entity.getZ(),
				5, 0.3, 0.3, 0.3, 0.01);
	}

	// ===== 鐧藉悕鍗曠鐞嗘柟娉曪紙渚?GUI / 缃戠粶灞傝皟鐢級 =====
	// 鏃х殑 SP-only 鐜╁閲嶈浇锛坅dd/remove/clear锛夊凡闅?/ssc_addon whitelist 鎸囦护涓€骞跺垹闄ゃ€?

	/**
	 * 浠庣櫧鍚嶅崟鎸塙UID绉婚櫎
	 */
	public static boolean removeFromWhitelistByUuid(ServerPlayer allayPlayer, UUID targetUuid) {
		String tag = WHITELIST_TAG_PREFIX + targetUuid.toString();
		return allayPlayer.getTags().remove(tag);
	}

	/**
	 * 鑾峰彇鐧藉悕鍗曚腑鐨勬墍鏈塙UID
	 */
	public static java.util.List<UUID> getWhitelistUuids(ServerPlayer allayPlayer) {
		java.util.List<UUID> uuids = new java.util.ArrayList<>();
		for (String tag : allayPlayer.getTags()) {
			if (tag.startsWith(WHITELIST_TAG_PREFIX)) {
				try {
					uuids.add(UUID.fromString(tag.substring(WHITELIST_TAG_PREFIX.length())));
				} catch (IllegalArgumentException e) {
					// 鏃犳晥UUID锛岃烦杩?
				}
			}
		}
		return uuids;
	}

		// ===== Apoli璧勬簮璇诲啓宸ュ叿 =====

	private static int getResourceValue(ServerPlayer player, ResourceLocation resourceId) {
		return PowerUtils.getResourceValue(player, resourceId);
	}

	private static void setResourceValue(ServerPlayer player, ResourceLocation resourceId, int value) {
		PowerUtils.setResourceValueAndSync(player, resourceId, value);
	}

		/**
	 * 璁剧疆璧勬簮鍊间絾涓嶅悓姝ュ埌瀹㈡埛绔紝閬垮厤閲嶇疆椋樻诞绛塸ower鐨勫唴閮ㄨ鏃跺櫒
	 * 浠呯敤浜庝笉闇€瑕佸鎴风鎰熺煡鐨勫唴閮ㄨЕ鍙戝櫒锛堝 heal_execute锛?
	 */
	private static void setResourceValueNoSync(ServerPlayer player, ResourceLocation resourceId, int value) {
		PowerUtils.setResourceValue(player, resourceId, value);
	}
}
