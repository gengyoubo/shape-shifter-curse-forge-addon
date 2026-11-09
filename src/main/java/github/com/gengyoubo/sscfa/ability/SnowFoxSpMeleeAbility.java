package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SP闆嫄杩戞垬涓昏鎶€鑳?- 闆埡鍐插埡
 * 鍚戝噯鏄熸柟鍚戝啿鍒?鏍硷紝纰版挒鏁屼汉閫犳垚6鐐归瓟娉曚激瀹冲苟鏂藉姞闇滃嚌鏁堟灉3绉?
 */
public class SnowFoxSpMeleeAbility {

	private static final ConcurrentHashMap<UUID, DashingPlayerData> DASHING_PLAYERS = new ConcurrentHashMap<>();
	private static final double DASH_DISTANCE = 8.0;   // 榛樿锛沚alance 鍙鐩栵紙abilities.snow_fox_sp_melee锛?
	private static final double DASH_SPEED = 1.5;
	private static final float DAMAGE = 8.0f;
	private static final int FROST_FREEZE_DURATION = 60;
	private static final int MANA_COST = 15;
	private static final int MELEE_REGEN_LOCK_TICKS = 100; // 鍥炶兘閿侀粯璁わ紱balance regen_lock_ticks 鍙鐩?
	private static final float DAMAGE_REDUCTION = 1.0f;    // 鍐插埡鏈熼棿鍏嶄激锛?00%锛夛紱balance damage_reduction 鍙鐩?

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.snow_fox_sp_melee");
	// ==== NEW CODE: 浣跨敤FormIdentifiers ====
	private static final ResourceLocation REGEN_COOLDOWN_ID = FormIdentifiers.SNOW_FOX_REGEN_COOLDOWN;

	private SnowFoxSpMeleeAbility() {
	}

	/**
	 * 鎵ц闆埡鍐插埡
	 * 闂ㄧ锛歱ower JSON锛坢y_addon:fail_aware_active_self锛夊師鐢?cooldown 瀛楁绠＄悊锛?
	 * 鏈柟娉曞彧璐熻矗鏁堟灉涓庡け璐ユ爣璁帮細钃濅笉澶?鍐插埡涓?鈫?markFail()锛堟寜 fail_cooldown 杩?CD锛夈€?
	 */
	private static final String SKILL_ID = "my_addon:form_snow_fox_sp_melee_primary";

	public static boolean execute(ServerPlayer player) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return false;
		if (github.com.gengyoubo.sscfa.resource.ResourceBars.get(player,
                github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX) < BAL.i("mana_cost", MANA_COST)) {
			// 娉曞姏涓嶈冻鎻愮ず闊充粎鏂芥硶鑰呰嚜宸卞惉锛坧layer.playSound 鍦ㄦ湇鍔＄浼氭帓闄よ嚜宸憋紝鏁呮敼涓哄畾鍚戝彂鍖咃級
			github.com.gengyoubo.sscfa.ability.MancianimaMarkManager.playSoundToPlayer(
					player, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.0f);
			FailAwareActiveSelfPower.markFail();
			return false;
		}

		if (DASHING_PLAYERS.containsKey(player.getUUID())) {
			FailAwareActiveSelfPower.markFail();
			return false;
		}

        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
        github.com.gengyoubo.sscfa.resource.ResourceBars.consume(player,
                github.com.gengyoubo.sscfa.resource.BarKeys.SNOW_FOX, BAL.i("mana_cost", MANA_COST));
		PowerUtils.setResourceValueAndSync(player, REGEN_COOLDOWN_ID, BAL.i("regen_lock_ticks", MELEE_REGEN_LOCK_TICKS));

		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.released(player, SKILL_ID);

		Vec3 lookDir = player.getLookAngle().normalize();

		DashingPlayerData data = new DashingPlayerData(lookDir, 0);
        data.castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
		DASHING_PLAYERS.put(player.getUUID(), data);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0f, 1.2f);

		return true;
	}

	/**
	 * 姣弔ick鏇存柊鍐插埡鐘舵€?
	 */
	public static void tick(ServerPlayer player) {
		DashingPlayerData data = DASHING_PLAYERS.get(player.getUUID());
		if (data == null) return;
        if (!player.isAlive()) {
            DASHING_PLAYERS.remove(player.getUUID());
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, data.castId);
            return;
        }

		double distanceMoved = data.ticksElapsed * BAL.d("dash_speed", DASH_SPEED);

		// 涓嶈妫€鏌?verticalCollision锛氳俯鍦ㄥ湴闈㈡椂 vanilla Entity.move 浼氬熀浜?閲嶅姏璁?movement.y=-0.08
		// 浣嗚鍦伴潰闃绘尅鍒?0锛宮ovement.y != vec3d.y"鎭掔疆 verticalCollision=true锛屽鑷寸珯绔嬭Е鍙戠殑 dash
		// 鍦ㄧ涓€ tick 灏辫杩欓噷 return 鎺夛紝setVelocity 涓€娆￠兘娌℃墽琛岋紝鐜╁鍘熷湴涓嶅姩涔熺涓嶅埌鏁屼汉銆?
		// 姘村钩鎾炲鎵嶉渶瑕佺粓姝?dash锛屾墍浠ュ彧鐪?horizontalCollision銆?
		if (distanceMoved >= BAL.d("dash_distance", DASH_DISTANCE) || player.horizontalCollision) {
			DASHING_PLAYERS.remove(player.getUUID());
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, SKILL_ID, data.castId);
			return;
		}

		Vec3 velocity = data.direction.scale(BAL.d("dash_speed", DASH_SPEED));
		player.setDeltaMovement(velocity);
		player.hurtMarked = true;
		// 鍏抽敭锛堝浜?瀹㈡満淇锛夛細鐜╁绉诲姩鏄鎴风鏉冨▉锛屽崟闈?velocityModified 涓嶄繚璇佹妸"鑷韩閫熷害"
		// 涓嬪彂缁欐帶鍒剁锛岃繙绔鏈轰細鍘熷湴涓嶅姩銆佸啿鍒烘棤浣嶇Щ銆佷篃纰颁笉鍒版部閫斿疄浣撳鑷存棤浼ゅ銆?
		// 杩欓噷姣?tick 鏄惧紡缁欒鐜╁杩炴帴琛ュ彂閫熷害鍖咃紝寮哄埗瀹㈡満搴旂敤鍐插埡閫熷害锛堜富鏈烘湰鍦扮帺瀹朵笉鍙楀奖鍝嶏級銆?
		if (player.connection != null) {
			player.connection.send(
					new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
		}

		AABB hitbox = player.getBoundingBox().inflate(0.5);
		List<Entity> nearbyEntities = player.level().getEntities(player, hitbox,
				entity -> entity instanceof LivingEntity && !data.hitEntities.contains(entity.getUUID()));

		for (Entity entity : nearbyEntities) {
			if (entity instanceof LivingEntity target) {
				if (WhitelistUtils.isProtected(player, target)) {
					data.hitEntities.add(entity.getUUID());
					continue;
				}
				data.hitEntities.add(entity.getUUID());

				DamageSource source = player.damageSources().playerAttack(player);
				boolean damaged=target.hurt(source, (float) BAL.d("damage", DAMAGE));

				boolean frozen=target.addEffect(new MobEffectInstance(
						github.com.gengyoubo.sscfa.init.AddonEffects.FROST_FREEZE.get(),
						BAL.i("freeze_ticks", FROST_FREEZE_DURATION),
						0,
						false,
						true,
						true
				), player);
				if(damaged||frozen)github.com.gengyoubo.sscfa.spell.FrostSpellLink.onNativeHit(player,target);

				// 浣跨敤ParticleUtils
				if (player.level() instanceof ServerLevel serverWorld) {
					ParticleUtils.spawnHitParticles(serverWorld, new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ()));
				}

				player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
						SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
			}
		}

		// 浣跨敤ParticleUtils
		if (player.level() instanceof ServerLevel serverWorld) {
			ParticleUtils.spawnSnowflakeParticles(serverWorld, new Vec3(player.getX(), player.getY() + 0.5, player.getZ()));
		}

		data.ticksElapsed++;
	}

	/**
	 * 鐜╁鏂嚎/姝讳骸鏃舵竻鐞嗘墍鏈夌姸鎬侊紝闃叉鍐呭瓨娉勬紡鍜岄噸杩炲悗鐘舵€侀敊涔?
	 */
	public static void clearPlayer(java.util.UUID uuid) {
		DASHING_PLAYERS.remove(uuid);
	}

	/**
	 * 娓呯悊鎵€鏈夊啿鍒虹姸鎬侊紝鐢ㄤ簬鏈嶅姟鍣ㄩ噸缃垨鍏ㄥ眬娓呯悊
	 */
	public static void clearAll() {
		DASHING_PLAYERS.clear();
	}

	/**
	 * 妫€鏌ョ帺瀹舵槸鍚︽鍦ㄩ洩鍒哄啿鍒猴紙渚?mixin 鍏嶄激鏌ヨ锛夈€?
	 */
	public static boolean isDashing(ServerPlayer player) {
		return DASHING_PLAYERS.containsKey(player.getUUID());
	}

	/**
	 * 闆埡鍐插埡鏈熼棿鐨勪激瀹冲噺鍏嶇郴鏁帮紙渚?mixin锛涢粯璁?1.0 = 100% 鍏嶄激锛宐alance damage_reduction 鍙皟锛夈€?
	 */
	public static float getDashDamageReduction(ServerPlayer player) {
		if (isDashing(player)) {
			return (float) BAL.d("damage_reduction", DAMAGE_REDUCTION);
		}
		return 0.0f;
	}

	/**
	 * 鍐插埡涓帺瀹舵暟鎹?
	 */
	private static class DashingPlayerData {
        long castId = -1;
		final Vec3 direction;
		final Set<UUID> hitEntities;
		int ticksElapsed;

		DashingPlayerData(Vec3 direction, int ticksElapsed) {
			this.direction = direction;
			this.hitEntities = new HashSet<>();
			this.ticksElapsed = ticksElapsed;
		}
	}
}
