package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;

/**
 * 婵傛垹浼?- 濞喡ゎ洣閹垛偓閼虫枻绱伴惉顒傂?
 *
 * 娑撱倗顫掑Ο鈥崇础閿涘牏鏁辩€广垺鍩涚粩顖炲帳缂?mancianimaTeleportMode 閸愬啿鐣鹃敍澶涚窗
 * - RAYCAST閿涙碍婀為崙鍡樻Е閺傜懓鎮滈惄瀛樺复娴肩娀鈧緤绱濋張鈧潻?8 閺嶇》绱濋柆鍥у煂閺傜懓娼￠崑婊勵剾
 * - PLATFORM閿涙岸鏀ｇ€规艾鍣弰鐔告煙閸氭垶娓舵潻鎴犳畱閸欘垳鐝粩瀣挬閸欏府绱濋拃鐣屽仯缁鳖偉澹婄划鎺戠摍娴犲懓鍤滃鍗炲讲鐟欎緤绱?
 *   閹稿绗呴弮鑸垫▔缁€娲暕鐟欏牞绱濋弶鎯х磻閹稿鏁弮鏈电炊闁緤绱遍崜宥嗘煙閺冪姴褰叉导鐘烩偓浣搁挬閸欐澘鍨幏鎺旂卜閿涘牅绗夊☉鍫ｂ偓妗烡/濞夋洖濮忛敍?
 *
 * CD: 3.5 缁夋帪绱?0 tick閿?  濞夋洖濮忓☉鍫ｂ偓? 5
 * 娴犲懓鍤滈煬顐ょ仜缁変紮绱濇稉宥夋付鐟曚胶娅ч崥宥呭礋閵?
 */
public final class MancianimaTeleport {

	public static final int COOLDOWN_TICKS = 70; // 3.5s
	/** 閼辨柨濮╅弨璇插毊閿涘牏瀛╅弽鍥／閻滅増鏌€閺夆偓閿涘鎳℃稉顓炴倵閻ㄥ嫮瀚粩瀣粓閸?CD閿?0s 閸愬懍绗夐懗钘夊晙閸欐垵濮╅懕鏂垮З閿涘本娅橀柅姘舵／閻滈绮涢崣顏囪泲 3.5s 鐢瓕顫?CD */
	public static final int RED_LINK_CD_TICKS = 200; // 10s 閼辨柨濮╅弨璇插毊CD
	public static final int RED_FAIL_CD_TICKS = 70; // 3.5s 婢惰精瑙D閿涘牆绱╃€甸棿鑵戦弬?鐎瑰本鍨氶弮鍓佹窗閺嶅洤鍑″浼欑礆
	public static final int MANA_COST = 5;
	public static final int RED_MARK_MANA_COST = 20;
	public static final double MAX_RANGE = 8.0;
	public static final double RED_MARK_TARGET_RANGE = 32.0;
	public static final int RED_MARK_CHANNEL_TICKS = 20; // 1s
	public static final float RED_MARK_DAMAGE_CAP = 37.0f;
	public static final float RED_MARK_DAMAGE_MIN = 2.0f;
	public static final double RED_MARK_DAMAGE_PERCENT = 0.50;
	/** 娴肩娀鈧礁鎮楅崘鑽ょ波閼奉亞鍔ч崶鐐舵憫閻?tick 閺佸府绱? 缁夋帪绱?*/
	public static final int MANA_REGEN_PAUSE_TICKS = 100;
	/** sp_mana_regen 閻ㄥ嫭娈忛崑婊嗩吀閺冭泛鐡欑挧鍕爱閿涘潊poli:multiple 鐎涙劙鏁?閳?power_id + "_" + sub_key閿?*/
	private static final net.minecraft.resources.ResourceLocation MANA_REGEN_PAUSE_RES =
			new net.minecraft.resources.ResourceLocation("my_addon", "form_familiar_fox_sp_mana_regen_regen_pause_timer");
	// 闂冭埖顔?5閿涙俺绻嶇悰灞炬韫囶偆鍙庣拠璇插絿閿涘潊bilities.mancianima_teleport閿涙稑鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧妯款吇鐢悂鍣洪敍澶堚偓?
	// MANA_COST=5 娑?RED_LINK_CD_TICKS=200 鐞?client/hud/SkillHudCatalog 瀵洜鏁ら垾鏂衡偓鎿D 閺勵垰鐫嶇粈娲閸婄》绱?
	// 闁絼琚辨径鍕箽閹镐礁鐖堕柌蹇撶穿閻劋绗夐弨鐧哥幢閺堝秴濮熺粩顖涚Х鐠愰€涚瀵板铔?BAL閵?
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_teleport");
	/** 缂佺喍绔撮崘宄板祱閺堝秴濮熼惃鍕旂€规碍濡ч懗?ID閿? 姒勫倽绌?power 濞夈劌鍞界捄顖氱窞閿涘鈧?*/
	public static final String SKILL_ID = "my_addon:form_familiar_fox_mancianima_soul_teleport";

	/** 娴肩娀鈧焦娓舵径褑绐涚粋浼欑礄閸欏瞼顏稉鈧懛杈剧礆閿涙艾顓归幋椋庮伂妫板嫯顫嶉敍鍧坥mputePlatformLanding閿涘绗岄張宥呭缁旑垱澧界悰灞藉彙閻劊鈧?
	 *  鐎广垺鍩涚粩顖濐嚢鐎广垺鍩涚粩顖炴殔閸嶅繐鎻╅悡褝绱濋張宥呭缁旑垵顕伴弶鍐ㄢ枆韫囶偆鍙庨敍鍫濇倱 MeteorSpell.maxRange 濡€崇础閿涘鈧?*/
	private static double maxRange() {
		if (github.com.gengyoubo.sscfa.balance.BalanceIntegration.isClientThread()) {
			var s = github.com.gengyoubo.sscfa.balance.BalanceIntegration.clientSnapshot();
			if (s != null) return s.getDouble("abilities.mancianima_teleport", "max_range");
		}
		return BAL.d("max_range", MAX_RANGE);
	}

	private MancianimaTeleport() {
	}

	/** 濡€崇础 0 = RAYCAST閿? = PLATFORM閵嗗倸顓归幋椋庮伂閸欐垿鈧緤绱濋張宥呭缁旑垰褰ф穱鈥叉崲濡€崇础閿涘牏婀呴崥鎴犵厺闁插繗鍤滅悰灞肩矤閻溾晛顔嶉悩鑸碘偓浣筋嚢閸欐牭绱氶妴?*/
	public static boolean execute(ServerPlayer player, byte mode) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(player)) return false;
		if (!isMancianima(player)) return false;
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return false;

		// 缁俱垺鐖ｉ懕鏂垮З閿涙艾顩ч弸婊冨櫙閺勭喎婀弻鎰嚋鐞氼偅婀伴悳鈺侇啀缁俱垺鐖ｉ惃鍕晸閻椻晙绗傞敍灞芥儙閸?1s 瀵洖顕?
		net.minecraft.world.entity.LivingEntity redTarget = tryFindRedMarkedInCrosshair(player);
		if (redTarget != null) {
			// 閼辨柨濮╅弨璇插毊閻欘剛鐝?CD閿?0s 閸愬懍绗夐懗钘夊晙閸欐垵濮╅懕鏂垮З閿涘牆鍣弰鐔稿瘹缁俱垺鐖ｉ弮鑸靛絹缁€鍝勮嫙娑撳秹妫悳甯幢娑撳秵瀵氱痪銏＄垼娑撳秴褰堝銈夋閿涘本娅橀柅姘舵／閻滄壆鍙庣敮闈╃礆
			int linkCd = PowerUtils.getResourceValue(player, FormIdentifiers.MANCIANIMA_LINK_CD);
			if (linkCd > 0) {
				int sec = (int) Math.ceil(linkCd / 20.0);
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.link_locked", sec), true);
				return false;
			}
			if (github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(player) < BAL.i("red_mark_mana_cost", RED_MARK_MANA_COST)) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_mana"), true);
				return false;
			}
			if (MancianimaMarkManager.CHANNELING.containsKey(player.getUUID())) return false;
            if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID)) return false;
			long now = ((ServerLevel) player.level()).getGameTime();
			MancianimaMarkManager.CHANNELING.put(player.getUUID(),
					new MancianimaMarkManager.ChannelState(redTarget.getUUID(), now + BAL.i("red_mark_channel_ticks", RED_MARK_CHANNEL_TICKS), 2));
            MancianimaMarkManager.CHANNELING.get(player.getUUID()).castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.channeling"), true);
			return true;
		}

		int manaCost = BAL.i("mana_cost", MANA_COST);
		if (github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(player) < manaCost) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_mana"), true);
			return false;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		ServerLevel world = (ServerLevel) player.level();

		Vec3 targetFeet;
		if (mode == 1) {
			targetFeet = computePlatformLanding(world, eye, look, player);
			if (targetFeet == null) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_platform"), true);
				return false; // 閺冪姴閽╅崣?閳?閹锋帞绮锋导鐘烩偓浣碘偓浣风瑝濞戝牐鈧D/濞夋洖濮?
			}
		} else {
			targetFeet = computeRaycastLanding(world, eye, look, player);
			if (targetFeet == null) return false;
		}

		// 娴肩娀鈧浇鎯ら悙鐟板灲鐎规熬绱伴埗?閽€鐣屽仯閺堫剝闊╅懗钘夘啇缁惧磭甯虹€硅绱遍埗?楠炲啿褰村Ο鈥崇础鐟曚焦鐪伴拃鐣屽仯閸︺劎甯虹€规儼顫嬮柌搴″敶閿涘牏婧傞惈娑掑晪閽€鐣屽仯閼典即鍎撮弮鐘虫煙閸ф浼勯幐鈽呯礆閵?
		// 閻劊鈧矁顫嬮柌搴″敶閵嗗秵娴涙禒锝嗘＋閻ㄥ嫭鏆ｉ弶陇鐭惧鍕潾閹剧偤鍣伴弽灏佲偓鏂衡偓鏂挎倵閼板懍绱伴幎?娴肩姳绗傚В鏃囧殰瀹搁亶鐝惃鍕挬閸?鐠囶垰鍨芥稉铏光敍婢ф瑱绱欓懘姘ｅ晪閸欎即銆婇惄瀵稿殠韫囧懐绮￠獮鍐插酱鐎圭偘缍嬮敍澶庘偓灞惧珕缂佹縿鈧?
		// 鐟欏棝鍣归崚銈呯暰閺冦垼鍏橀幐锟犳閺嶅懏鐖?闂呮柨顣炬导鐘烩偓渚婄礄閽€鐣屽仯閼典即鍎寸悮顐︿紕閹糕槄绱氶敍灞藉嫉閸忎浇顔忓锝呯埗閻у鐝?鐠哄疇绌拃鐣屽仯閿?6閿涘鈧?
		if (!isSafeLanding(world, player, targetFeet)
				|| (mode == 1 && !isLandingVisible(world, player, targetFeet))) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.no_platform"), true);
			return false;
		}

		// 閸戝搫褰傞悙鍦煈鐎?+ 闂婅櫕鏅ラ敍鍫濐嚠閸涖劌娲块幍鈧張澶屽负鐎硅泛褰茬憴?閸欘垰鎯夐敍?
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.PORTAL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.3, 0.8, 0.3, 0.6);
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.REVERSE_PORTAL,
				player.getX(), player.getY() + 1.0, player.getZ(),
				20, 0.3, 0.5, 0.3, 0.05);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);

		// 閹笛嗩攽娴肩娀鈧緤绱欐穱婵堟殌鐟欏棜顫楅弬鐟版倻閿?
		player.teleportToWithTicket(targetFeet.x, targetFeet.y, targetFeet.z);

		// 閽€鐣屽仯缁帒鐡?+ 闂婅櫕鏅ラ敍鍫濐嚠閸涖劌娲块幍鈧張澶屽负鐎硅泛褰茬憴?閸欘垰鎯夐敍?
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.PORTAL,
				targetFeet.x, targetFeet.y + 1.0, targetFeet.z,
				40, 0.3, 0.8, 0.3, 0.6);
		ParticleUtils.spawnDecorationParticles(world, player, ParticleTypes.REVERSE_PORTAL,
				targetFeet.x, targetFeet.y + 1.0, targetFeet.z,
				20, 0.3, 0.5, 0.3, 0.05);
		world.playSound(null, targetFeet.x, targetFeet.y, targetFeet.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

		// 閹碉綁娅庡▔鏇炲 + 鐠佸墽鐤咰D
		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
		if (mana != null) {
			mana.setMana(Math.max(0.0, mana.getMana() - manaCost));
		}
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
		// 娴肩娀鈧礁鎮楅崘鑽ょ波閼奉亞鍔ч崶鐐舵憫 3 缁夋帪绱欐稉宥呭閸濆秳瀵岄崝銊ユ礀閽冩繃濡ч懗?濞戝牐鈧绱?
		PowerUtils.setResourceValueAndSync(player, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
		return true;
	}

	private static boolean isMancianima(Player player) {
		net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(player);
		return form != null && FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id());
	}

	/** 閸戝棙妲︽稉濠冩Ц閸氾附婀佺悮顐ｆ拱閻溾晛顔嶇痪銏＄垼閻ㄥ嫮娲伴弽鍥风吹鏉╂柨娲栫拠銉ф窗閺嶅浄绱濋崥锕€鍨?null閵?*/
	private static net.minecraft.world.entity.LivingEntity tryFindRedMarkedInCrosshair(ServerPlayer player) {
		MancianimaMarkManager.Mark m = MancianimaMarkManager.getMark(player.getUUID());
		if (m == null || m.color != MancianimaMarkManager.MarkColor.RED) return null;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = eye.add(look.scale(BAL.d("red_mark_target_range", RED_MARK_TARGET_RANGE)));
		ServerLevel world = (ServerLevel) player.level();
		net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(eye, end).inflate(2.0);
		double bestDist = Double.MAX_VALUE;
		net.minecraft.world.entity.LivingEntity best = null;
		for (net.minecraft.world.entity.Entity e : world.getEntities(player, searchBox,
				net.minecraft.world.entity.EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive()) continue;
			if (!le.getUUID().equals(m.targetUuid)) continue;
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, le)) continue;
			net.minecraft.world.phys.AABB box = e.getBoundingBox().inflate(1.0); // "婢堆嗗毀鐎电懓鍣?閺€鎯ь啍
			java.util.Optional<Vec3> hit = box.clip(eye, end);
			if (hit.isEmpty()) continue;
			double d = eye.distanceToSqr(hit.get());
			if (d < bestDist) { bestDist = d; best = le; }
		}
		return best;
	}

	/** MancianimaMarkManager 瀵洖顕?tick 閺堫偄鐔拫鍐暏閿涙碍澧界悰宀€瀛╅弽鍥╃仜缁夌粯鏌€閺夆偓閵?*/
	public static void executeRedMarkChannelComplete(ServerPlayer marker, net.minecraft.world.entity.LivingEntity target, long castId) {
        if (github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(marker, SKILL_ID) != castId || castId < 0) return;
        if (github.com.gengyoubo.sscfa.anomaly.AnomalyTeleportGuard.reject(marker)) {
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, SKILL_ID, castId);
            return;
        }
		if (target == null || !target.isAlive()
				|| WhitelistUtils.isProtected(marker, target)
				|| github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(marker, target)) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, SKILL_ID, castId);
			return;
		}
		// 閹?mana
		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(marker);
		if (mana != null) mana.setMana(Math.max(0.0, mana.getMana() - BAL.i("red_mark_mana_cost", RED_MARK_MANA_COST)));
		ServerLevel world = (ServerLevel) marker.level();
		// 鐠侊紕鐣婚拃鐣屽仯閿涙氨娲伴弽鍥煩閸?閺嶇厧婀撮棃?
		Vec3 targetPos = target.position();
		Vec3 targetLook = target.getLookAngle().normalize();
		Vec3 behind = targetPos.subtract(targetLook.x, 0, targetLook.z).add(0, 0, 0); // 1 閺嶇厧鎮楅弬?
		Vec3 landing = adjustBehindTarget(world, target, behind);
		// 閸戝搫褰傜划鎺戠摍
		ParticleUtils.spawnParticles(world, ParticleTypes.PORTAL,
				marker.getX(), marker.getY() + 1.0, marker.getZ(),
				40, 0.3, 0.8, 0.3, 0.6);
		// 娴肩娀鈧?
		marker.teleportToWithTicket(landing.x, landing.y, landing.z);
		// 鐠佲晛顨栭悘鐢垫箙閸氭垹娲伴弽?
		double dx = target.getX() - landing.x;
		double dz = target.getZ() - landing.z;
		float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		marker.setYRot(yaw);
		marker.setXRot(0);
		marker.connection.teleport(landing.x, landing.y, landing.z, yaw, 0);
		// 閽€鐣屽仯缁帒鐡?
		ParticleUtils.spawnParticles(world, ParticleTypes.PORTAL,
				landing.x, landing.y + 1.0, landing.z,
				40, 0.3, 0.8, 0.3, 0.6);
		// 50% 缂傚搫銇戠悰鈧导銈咁唺閿涙艾甯稉濠囨姒涙顓?37閿涘PC 娑撳﹪妾虹紙璇测偓宥忕幢濞岃法鏁ら崢鐔告箒 playerAttack 娴笺倕顔婂┃鎰┾偓?
		float missing = target.getMaxHealth() - target.getHealth();
		float cap = NpcCombatEnhancements.scale(marker, target, (float) BAL.d("red_mark_damage_cap", RED_MARK_DAMAGE_CAP), "mancianima_link_cap_mul", 2.0);
		float dmg = (float) Math.max(BAL.d("red_mark_damage_min", RED_MARK_DAMAGE_MIN), Math.min(cap, missing * BAL.d("red_mark_damage_percent", RED_MARK_DAMAGE_PERCENT)));
		boolean wasAlive = target.isAlive();
		target.hurt(world.damageSources().playerAttack(marker), dmg);
		// 楠炴寧鎸遍弳鏉戝毊闂婅櫕鏅?
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
		// 鐠佸墽鐤?CD + 閺嗗倸浠犻崶鐐舵憫閿涙碍娅橀柅姘舵／閻滄澘褰ф潻?3.5s 鐢瓕顫?CD閿涙稖浠堥崝銊︽暰閸戞槒绻橀悪顒傜彌閻?10s 閼辨柨濮?CD閿涘牊婀￠梻鏉戝讲閺咁噣鈧岸妫悳鑸偓浣风瑝閸欘垰鍟€閼辨柨濮╅敍?
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(marker, SKILL_ID, castId);
		PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_LINK_CD,
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(marker, SKILL_ID, "link"));
		PowerUtils.setResourceValueAndSync(marker, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
		// 閸戠粯娼冩總鏍уС閿涙艾鍩涢弬棰佽⒈娑?CD + 閹舵ぞ婵€鐞涖儲寮?
		if (wasAlive && !target.isAlive()) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.reset(marker, MancianimaPrimary.SKILL_ID);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.reset(marker, SKILL_ID);
			// 閸戠粯娼冩總鏍уС閸氬本鐗卞〒鍛敄閼辨柨濮?CD閿涘苯鍘戠拋鍝ョ彌閸掕鍟€閸欐垵濮╂稉瀣╃濞喡や粓閸斻劍鏁鹃崙?
			PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_LINK_CD, 0);
			int max = PowerUtils.getResourceMax(marker, FormIdentifiers.MANCIANIMA_RESISTANCE);
			if (max <= 0) max = 2;
			PowerUtils.setResourceValueAndSync(marker, FormIdentifiers.MANCIANIMA_RESISTANCE, max);
			// 閺傗晜娼冮崶鐐村姬姒勬柨濮忛敍灞肩瑢娑撶粯濡ч懗鎴掔箽閹镐椒绔撮懛?
			github.com.gengyoubo.sscfa.compat.ManaUtils.setPlayerMana(marker, github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(marker));
			marker.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.teleport.kill_bonus"), true);
		}
		// 缁俱垺鐖ｆ担鍨嚒鏉堢偓鍨?閳?濞撳懘娅?
		MancianimaMarkManager.clearMark(world.getServer(), marker.getUUID());
	}

	/** 鐠侊紕鐣荤痪銏＄垼閻剛些閽€鐣屽仯閿涙矮绱崗鍫㈡窗閺嶅洤鎮楅弬?閺嶇厧婀撮棃顫幢閸氬孩鏌?2閺嶅吋鍋撳畷鏍ゅ晪閺冧椒鏅堕敍娑欐⒑娓氀勬￥閸︽壋鍟嬮崥搴㈡煙1閺嶅吋鍋撶粚鎭掆偓?*/
	private static Vec3 adjustBehindTarget(ServerLevel world, net.minecraft.world.entity.LivingEntity target, Vec3 desired) {
		// 閽€鐣屽仯 Y閿涙矮绮?desired Y 閸氭垳绗呴幍顐ｆ付婢?3 閺嶇厧顕伴幍鎯ф勾闂?
		double baseY = target.getY();
		BlockPos basePos = BlockPos.containing(desired.x, baseY, desired.z);
		// 濡偓閺屻儱鎮楅弬?閺嶇厧婀撮棃銏℃Ц閸氾箑鐡ㄩ崷顭掔礄閼存矮绗呴弬鐟版健鐎圭偛绺鹃敍?
		BlockPos floorBelow = basePos.below();
		boolean hasGround = !world.getBlockState(floorBelow).getCollisionShape(world, floorBelow).isEmpty();
		if (hasGround) return new Vec3(desired.x, baseY, desired.z);
		// 濞屸€虫勾闂?閳?濡偓閺屻儳娲伴弽鍥ㄦ⒑娓?
		Vec3 targetLook = target.getLookAngle().normalize();
		Vec3 sideRight = new Vec3(-targetLook.z, 0, targetLook.x).scale(1.0);
		Vec3 sidePos = target.position().add(sideRight);
		BlockPos sideBelow = BlockPos.containing(sidePos.x, baseY, sidePos.z).below();
		if (!world.getBlockState(sideBelow).getCollisionShape(world, sideBelow).isEmpty()) {
			return new Vec3(sidePos.x, baseY, sidePos.z);
		}
		// 闁姤鐥呴張?閳?娴犲秳绱堕崥搴㈡煙1閺嶇》绱欑粚杞拌厬閿?
		return new Vec3(desired.x, baseY, desired.z);
	}

	/**
	 * RAYCAST 濡€崇础閿涙碍閮ㄧ憴鍡欏殠閺堚偓鏉?8 閺嶇》绱濈喊鏉垮煂閺傜懓娼￠弮璺烘礀闁偓娑撯偓閻愬綊妲婚崡鈥愁暰閵?
	 * 閸忓彉闊╃粻妤佺《閿涘牆顓归幋椋庮伂閸欘垵鐨熼悽銊ユ倱娑撯偓娴犱粙鈧槒绶崑姘额暕鐟欏牞绱氶妴?
	 */
	public static Vec3 computeRaycastLanding(Level world, Vec3 eye, Vec3 look, Player player) {
		Vec3 end = eye.add(look.scale(maxRange()));
		BlockHitResult hit = world.clip(new ClipContext(
				eye, end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));
		Vec3 landingEye;
		if (hit.getType() == HitResult.Type.MISS) {
			landingEye = end;
		} else {
			// 娴犲海顫幘鐐靛仯閸ョ偤鈧偓 0.4 閺嶇》绱濋柆鍨帳閼存岸鍎村畵灞藉弳婢ф瑤缍?
			landingEye = hit.getLocation().add(look.scale(-0.4));
		}
		// 鏉烆剚宕叉稉楦垮壖闁劌娼楅弽鍥风礄閻溾晛顔嶇憴鍡涚彯缁?1.62閿?
		double feetY = landingEye.y - (player.getEyeY() - player.getY());
		// 闂冨弶顒?feetY 瀹撳苯鍙嗛崷浼存桨閿涙俺瀚㈤懘姘瑓閺傜懓娼℃稉鍝勭杽韫囧啫鍨崥鎴滅瑐閹额兛绔撮弽?
		BlockPos feetPos = BlockPos.containing(landingEye.x, feetY, landingEye.z);
		if (!world.getBlockState(feetPos).getCollisionShape(world, feetPos).isEmpty()) {
			feetY = feetPos.getY() + 1.0;
		}
		return new Vec3(landingEye.x, feetY, landingEye.z);
	}

	/**
	 * PLATFORM 濡€崇础閿涙矮姹夐幀褍瀵茬粵鏍殣閵?
	 * <ul>
	 *   <li>娴ｈ法鏁?b>缁墽鈥樼亸鍕殠 XZ</b>娴ｆ粈璐熼拃鐣屽仯閿涘牅绗夐崡鈩冪壐娑擃厼绺鹃敍澶涚幢閽€鐣屽仯 Y 閸欐牗鏌熼崸妤冾潾閹剧偟顔堟い鍫曟桨閿涘牊銈煎?閸欎即妯侀柅鍌炲帳閿涘鈧?/li>
	 *   <li>鐏忓嫮鍤庨崗鍫濅粵娑撯偓濞嗭紕绨跨涵?raycast閿涙艾鎳℃稉?閳?閽€钘夊煂閸涙垝鑵戦悙鐧哥礄妞ゅ爼娼伴崨鎴掕厬=閻╁瓨甯寸粩娆欑幢娓?鎼存洖鎳℃稉?鐠囥儱鍨崥鎴滅瑓閹殿偓绱氶妴?/li>
	 *   <li>鐏忓嫮鍤庨張顏勬嚒娑?閳?濞屽灝鐨犵痪?march閿涘本澧嶉張澶婂灙閸愬懏婀侀弫鍫濋挬閸欓鑵戦崣?b>3D 鐠烘繄顬囩粋璇茬殸缁捐法绮撻悙瑙勬付鏉?/b>閻ㄥ嫪绔存稉顏庣礉
	 *       娴ｅ灝绶辨禒鎷岊潡鐡掑﹤閽╅埆鎺曟儰閻愮绉烘潻婊愮幢娴犳媽顫楅崥鎴滅瑐閳帊绱崗鍫モ偓澶愮彯婢跺嫬顥嗛弸鎯扳偓宀勬姜閼存矮绗呴崷浼存桨閵?/li>
	 *   <li>濮ｅ繐鍨幍顐ｅ伎缁愭褰?[refY-3, refY+1]閿涘瞼绮锋稉宥勭窗娴犲骸鐨犵痪鍨发娴ｅ骸顦╅惃鍕灙閸欐牕鍩岄崷浼存桨閿涘奔绮犻懓宀勪缉閸忓秳缍嗛惉鍕彯閺冩儼鎯ら崚鎷屽壖娑撳鈧?/li>
	 * </ul>
	 * 缁旀瑧鐝涢弶鈥叉閸欘亣顩﹀Ч鍌︾窗閺傜懓娼℃い鍫曟桨闂堢偟鈹?+ 娑撳﹥鏌熼張澶屽 1.8 閺嶈偐鈹栧鏃撶幢娑撳秹妾洪崚璺洪挬閸欐澘鏄傜€垫悶鈧?
	 */
	public static Vec3 computePlatformLanding(Level world, Vec3 eye, Vec3 look, Player player) {
		Vec3 end = eye.add(look.scale(maxRange()));
		BlockHitResult hit = world.clip(new ClipContext(
				eye, end,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));

		if (hit.getType() == HitResult.Type.BLOCK) {
			Vec3 hitPos = hit.getLocation();
			// Case 1閿涙艾鎳℃稉顓熸煙閸ф銆婇棃?閳?閻╁瓨甯存禒銉ユ嚒娑擃厾鍋?XZ 閽€鍊熷壖閿涘牏鐎崫顏嗙彲閸濐亷绱?
			if (hit.getDirection() == Direction.UP) {
				BlockPos solid = hit.getBlockPos();
				Double topY = collisionTopY(world, solid);
				if (topY != null && hasHeadroom(world, hitPos.x, topY, hitPos.z)
						&& isSafeLanding(world, player, new Vec3(hitPos.x, topY, hitPos.z))) {
					Vec3 landing = new Vec3(hitPos.x, topY, hitPos.z);
					if (withinRange(eye, landing)) return landing;
				}
			}
			// Case 2閿涙艾鎳℃稉顓濇櫠闂?鎼存洟娼?閳?閸︺劌鎳℃稉顓犲仯閸撳秵鏌熼惃鍕敄濮樻柨鍨柌灞惧閺堚偓鐠愮绻庨崨鎴掕厬 Y 閻ㄥ嫬閽╅崣甯礄閻劌鎳℃稉顓犲仯 XZ閿?
			BlockPos airCol = hit.getBlockPos().relative(hit.getDirection());
			Vec3 landing = findBestInColumn(world, player, hitPos.x, hitPos.z, airCol.getX(), airCol.getZ(), hitPos.y, eye, hitPos);
			if (landing != null) return landing;
		}

		// Case 3閿涙艾鐨犵痪鎸庢￥闂冪粯灏?閳?濞屽灝鐨犵痪?march閿涘苯褰囩粋?鐏忓嫮鍤庣紒鍫㈠仯"3D 鐠烘繄顬囬張鈧潻鎴犳畱楠炲啿褰撮敍鍫ｇ箼閸嬪繒鐤嗛敍?
		Vec3 best = null;
		double bestSqToEnd = Double.MAX_VALUE;
		double step = 0.5;
		for (double d = 1.0; d <= maxRange(); d += step) {
			Vec3 sample = eye.add(look.scale(d));
			Vec3 landing = findBestInColumn(world, player, sample.x, sample.z,
					Mth.floor(sample.x), Mth.floor(sample.z), sample.y, eye, end);
			if (landing == null) continue;
			double sq = landing.distanceToSqr(end);
			if (sq < bestSqToEnd) {
				bestSqToEnd = sq;
				best = landing;
			}
		}
		return best;
	}

	/**
	 * 閸︺劍瀵氱€规碍鏌熼崸妤€鍨?(bx,bz) 閸愬懎顕伴幍鐐付娴ｅ疇鎯ら懘姘挬閸欒埇鈧?
	 * 閽€鐣屽仯 XZ 娴ｈ法鏁?exactX/exactZ閿涘牅绻氶悾娆忕毈閺佹壆绨挎惔锔肩礆閿涙矉 娴ｈ法鏁ら弬鐟版健绾扮増鎸掔粻鍗炵杽闂勫懘銆婇棃銏ょ彯鎼达负鈧?
	 * 閸婃瑩鈧鐛ラ崣锝勮礋 [refY-3, refY+1]閿涘矂浼╅崗宥勭矤鏉╂粌顦╂担搴ㄧ彯鎼达箑鐨犵痪鎸庣壉閺堫剝顕ら崣鏍壖娑撳婀撮棃顫偓?
	 * 閸︺劎鐛ラ崣锝呭敶閸忋劑鍎撮崐娆撯偓澶夎厬閸欐牜顬?anchor閿涘牆鐨犵痪璺ㄧ矒閻?閸涙垝鑵戦悙鐧哥礆3D 鐠烘繄顬囬張鈧潻鎴犳畱娑撯偓娑擃亗鈧?
	 */
	private static Vec3 findBestInColumn(Level world, Player player, double exactX, double exactZ,
	                                       int bx, int bz, double refY, Vec3 eye, Vec3 anchor) {
		int topY = Mth.floor(refY) + 1;
		int bottomY = Mth.floor(refY) - 3;
		Vec3 best = null;
		double bestSq = Double.MAX_VALUE;
		for (int y = topY; y >= bottomY; y--) {
			BlockPos pos = new BlockPos(bx, y, bz);
			Double topYExact = collisionTopY(world, pos);
			if (topYExact == null) continue;
			if (!hasHeadroom(world, exactX, topYExact, exactZ)) continue;
			Vec3 candidate = new Vec3(exactX, topYExact, exactZ);
			if (!isSafeLanding(world, player, candidate)) continue;
			if (!withinRange(eye, candidate)) continue;
			double sq = candidate.distanceToSqr(anchor);
			if (sq < bestSq) {
				bestSq = sq;
				best = candidate;
			}
		}
		return best;
	}

	/** 鏉╂柨娲栭弬鐟版健绾扮増鎸掔粻閬嶃€婇棃顫瑯閻?Y閿涙稓鈹栫喊鐗堟寬缁犳唻绱欑粚鐑樼毜/濡炲秶澧跨粵澶涚礆鏉╂柨娲?null閵嗗倹鏁幐浣广偧濮?閸欎即妯侀妴?*/
	private static Double collisionTopY(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getCollisionShape(world, pos);
		if (shape.isEmpty()) return null;
		double maxY = shape.max(Direction.Axis.Y);
		if (maxY <= 0) return null;
		return pos.getY() + maxY;
	}

	/**
	 * 濡偓閺?(x,z) 婢跺嫯鍓兼担?feetY 娑撳﹥鏌熺痪?1.8 閺嶈偐鈹栧鏃撶礄閻溾晛顔嶉煬顐︾彯閿涘鈧?
	 * 閻?floor(feetY + 0.5) 娴ｆ粈璐熺挧椋庡仯鐠哄疇绻冮獮鍐插酱閺堫剝闊╅敍鍫濆瘶閹奉剙褰撮梼?濡ゅ吋顫い鍫曟桨閿涘鈧?
	 */
	private static boolean hasHeadroom(Level world, double x, double feetY, double z) {
		int bx = Mth.floor(x);
		int bz = Mth.floor(z);
		int startY = Mth.floor(feetY + 0.5);
		for (int dy = 0; dy < 2; dy++) {
			BlockPos p = new BlockPos(bx, startY + dy, bz);
			VoxelShape shape = world.getBlockState(p).getCollisionShape(world, p);
			if (!shape.isEmpty()) return false;
		}
		return true;
	}

	/** 閽€鐣屽仯鐠烘繄顬囩涵顒勬閸掕绱欓惇鑲╂涧閸掓媽鎯ら悙鍦纯缁捐儻绐涚粋浼欑礆閵?*/
	private static boolean withinRange(Vec3 eye, Vec3 landing) {
		double maxRange = maxRange();
		return eye.distanceToSqr(landing) <= (maxRange + 0.5) * (maxRange + 0.5);
	}

	/** 濡偓閺屻儴鎯ら悙鐟邦槱閻溾晛顔嶇€瑰本鏆ｇ喊鐗堟寬缁犺鲸妲搁崥锔胯礋缁屾亽鈧?*/
	private static boolean isSafeLanding(Level world, Player player, Vec3 feet) {
		AABB box = playerBoxAt(player, feet.x, feet.y, feet.z);
		return world.noCollision(player, box);
	}

	/** 閽€鐣屽仯韫囧懘銆忛崷銊у负鐎规儼顫嬮柌搴″敶閿涙氨婧傞惈娑掑晪閽€鐣屽仯閼典即鍎撮惃鍕潒缁惧じ绗夌悮顐ｆ煙閸ф绱欓崥顐ｇ垺閺?婢ф瑱绱氶柆顔藉皡閵嗗倻鏁ゆ禍搴㈡建缂佹繂閽╅崣鐗埬佸蹇涙闂呮粈绱堕柅渚婄礄#6閿涘鈧?*/
	private static boolean isLandingVisible(Level world, Player player, Vec3 targetFeet) {
		Vec3 eye = player.getEyePosition();
		// 閸欐牞鎯ら悙纭呴煩娴ｆ挷鑵戣箛鍐彯鎼达缚缍旀稉鍝勫讲鐟欎焦鈧冨棘閼板啰鍋ｉ敍鍫㈠ 0.5~0.9 閺嶅ジ鐝敍澶涚礉闁灝鍘ら崣顏咁梾閺屻儴鍓兼惔鏇☆潶閸欎即妯佺拠顖氬灲
		Vec3 targetCenter = targetFeet.add(0, Math.min(0.9, Math.max(0.5, player.getBbHeight() / 2.0)), 0);
		BlockHitResult hit = world.clip(new ClipContext(
				eye, targetCenter,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player));
		if (hit.getType() != HitResult.Type.BLOCK) return true;
		// 閸涙垝鑵戦弬鐟版健閻愯鐦拃鐣屽仯閺囩绻庨敍鍫㈡殌 0.25 鐎圭懓妯婇敍澶嗗晪 鐟欏棛鍤庣悮顐︿紕閹?閳?閽€鐣屽仯娑撳秴褰茬憴?閳?閹锋帞绮锋导鐘烩偓?
		return hit.getLocation().distanceToSqr(eye) + 0.25 >= targetCenter.distanceToSqr(eye);
	}

	private static AABB playerBoxAt(Player player, double x, double y, double z) {
		double halfWidth = Math.max(0.3, player.getBbWidth() / 2.0);
		double height = Math.max(1.8, player.getBbHeight());
		return new AABB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth).deflate(1.0E-7);
	}
}
