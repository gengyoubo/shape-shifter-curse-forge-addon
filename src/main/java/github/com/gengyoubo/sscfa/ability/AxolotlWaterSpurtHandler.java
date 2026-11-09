package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 杩涘寲缇庤タ铻堛€屾按娴佸啿鍒恒€嶏紙water_spurt 鑺傜偣锛夆€斺€?鏈嶅姟绔彲闈犲疄鐜帮紝姘撮檰涓ゅ瑙﹀彂褰诲簳闅旂銆?
 *
 * <p><b>姘翠笅鍐插埡</b>锛堝悓 SSC {@code form_axolotl_2_water_spurt}锛夛細鍦ㄦ按涓寜<b>鐤捐窇閿?/b>锛坰print 涓婂崌娌匡級鈫?
 * 娌胯绾垮墠鍐诧紝<b>涓嶆秷鑰楁箍娑﹀害</b>锛堜笌 SSC 涓€鑷达級锛岀嫭绔嬪喎鍗?5 绉掋€?/p>
 * <p><b>闄嗗湴鍐插埡</b>锛氬湪闄嗗湴涓婄柧璺戞椂鎸?b>娼滆閿?/b>锛坰neak 涓婂崌娌夸笖褰撳墠/涓婁竴 tick 澶勪簬鐤捐窇锛夆啋
 * 鍚屾牱鐨勫墠鍐蹭綅绉伙紝<b>娑堣€?{@link #LAND_MOISTURE_COST} 鐐规箍娑﹀害(air)</b>锛岀嫭绔嬪喎鍗?5 绉掋€?/p>
 * <p>鐢?{@code isTouchingWater()} 鍖哄垎姘?闄嗭紝涓ゅ垎鏀簰鏂ワ紙姘撮噷鍙蛋鐤捐窇鍐插埡銆佷笉鍝嶅簲娼滆锛涢檰鍦板彧璧版綔琛屽啿鍒恒€?
 * 涓嶅搷搴旂柧璺戯級锛屼笖鍚勮嚜鐙珛鍐峰嵈锛屼簰涓嶅共鎵般€傚凡瑙ｉ攣 water_spurt 鑺傜偣鎵嶇敓鏁堛€?/p>
 */
public final class AxolotlWaterSpurtHandler {

	/** 闄嗗湴鍐插埡鐨勬湇鍔＄鎵ｈ垂鍜屽鎴风璧勬簮闂ㄦ鍏辩敤鍚屼竴鏁板€笺€?*/
	public static int landAirCostForHud(net.minecraft.world.entity.player.Player player) {
		return github.com.gengyoubo.sscfa.compat.UpgradeAxolotlPerks.landDashAirCost(player, BAL.i("land_moisture_cost", LAND_MOISTURE_COST));
	}

	// 浠ヤ笅鍧囦负榛樿鍊硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.axolotl_water_spurt锛?
	private static final int CD_TICKS = 100;   // 5 绉?
	private static final ResourceLocation WATER_HUD = new ResourceLocation("my_addon", "form_upgrade_axolotl_dash_hud_water");
	private static final ResourceLocation LAND_HUD = new ResourceLocation("my_addon", "form_upgrade_axolotl_dash_hud_land");
	private static final double BURST = 1.6;    // 鍓嶅啿鍔涘害锛堟按闄嗕竴鑷达級
	/** 闄嗗湴鍐插埡娑堣€楃殑婀挎鼎搴︼紙air 鍊硷紱鍙傜収 WaterSpearLeapManager 鐨?18=6%锛屽啿鍒烘洿杞伙紝鍙?12鈮?%锛夈€傛按涓嬪啿鍒哄悓 SSC 鍏嶈垂銆?*/
	private static final int LAND_MOISTURE_COST = 12;

	/** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.axolotl_water_spurt");

	/** 瀹㈡埛绔笂鎶ョ殑銆岀湡姝ｇ柧璺戦敭銆嶆寜浣忕姸鎬侊紙鍖哄垎鍙屽嚮 W / 娓告吵鑷姩鐤捐窇锛岄伩鍏嶈瑙﹀彂鍐插埡锛夈€?*/
	private static final Map<UUID, Boolean> CLIENT_SPRINT = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> WAS_CLIENT_SPRINT = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> WAS_SNEAKING = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> WATER_CD = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> LAND_CD = new ConcurrentHashMap<>();

	/** 瀹㈡埛绔彂銆岀柧璺戦敭鎸変綇鐘舵€併€嶅寘鏃惰皟鐢紙浠呰繘鍖栫編瑗胯煚鏈満浼氬彂锛涘尯鍒嗙湡姝ｇ柧璺戦敭涓庡弻鍑?W/娓告吵鑷姩鐤捐窇锛夈€?*/
	public static void setClientSprintHeld(ServerPlayer player, boolean held) {
		CLIENT_SPRINT.put(player.getUUID(), held);
	}

	private AxolotlWaterSpurtHandler() {
	}

	/** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢ㄣ€?*/
	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		int wcd = WATER_CD.getOrDefault(id, 0);
		if (wcd > 0) WATER_CD.put(id, wcd - 1);
		int lcd = LAND_CD.getOrDefault(id, 0);
		if (lcd > 0) LAND_CD.put(id, lcd - 1);

		if (!FormUtils.isUpgradeAxolotl(player)) {
			CLIENT_SPRINT.remove(id);
			WAS_CLIENT_SPRINT.remove(id);
			WAS_SNEAKING.remove(id);
			return;
		}

		// 鐢ㄥ鎴风涓婃姤鐨勩€岀湡姝ｇ柧璺戦敭銆嶇姸鎬侊紝鑰岄潪鏈嶅姟绔?isSprinting()锛堝悗鑰呬細琚弻鍑?W / 娓告吵鑷姩缃湡 鈫?璇啿锛?
		boolean sprintKey = CLIENT_SPRINT.getOrDefault(id, false);
		// HUD 璧勬簮浠呭湪 CD 娲昏穬鏈熷悓姝ワ紙绌洪棽绋虫€?wcd==lcd==0 闆惰祫婧愯鍐欙級锛欳D 褰掗浂鍚庤ˉ鍙戜竴娆?0锛?
		// 涔嬪悗涓嶅啀瑙︾璧勬簮锛涜Е鍙戝啿鍒烘椂鍦ㄥ垎鏀唴鐩存帴鍐?HUD锛堝€煎繀鍙橈紝鏃犻渶姣斿锛夈€?
		// 褰掗浂琛ュ彂鑻ュ洜鏋佺鏃跺簭琚簲鐢ㄥ眰涓㈠純锛堟崲缁村害/浼犻€佺灛闂村疄浣撴湭杩借釜锛夛紝绌洪棽鏈熸瘡 100t 浣庨瀵硅处
		// 涓€娆¤嚜鎰堬紙鏈嶅姟绔湰鍦拌涓€娆℃瘮瀵癸紝鍊间笉绗︽墠閲嶅彂鈥斺€旀甯告€侀浂棰濆鍖咃紝寮傚父鎬佹渶澶氭畫鐣?5s锛夈€?
		boolean waterCooling = wcd > 0;
		boolean landCooling = lcd > 0;
		boolean idleAudit = !waterCooling && !landCooling && wcd != 1 && lcd != 1 && player.tickCount % 100 == 0;
		if (waterCooling || wcd == 1 || (idleAudit
				&& PowerUtils.getResourceValue(player, WATER_HUD) != 0)) {
			// 姣?tick 鎭板ソ -1 鐨?CD 鍊掕鏃讹細閫掑噺鏈熻蛋鎵归噺棰勬祴閫氶亾锛屽綊闆跺抚/瀵硅处璧扮珛鍗冲悓姝ワ紙2026-09-24锛?
			if (waterCooling) PowerUtils.countDownAndSync(player, WATER_HUD, Math.max(0, wcd - 1));
			else PowerUtils.setResourceValueAndSync(player, WATER_HUD, Math.max(0, wcd - 1));
		}
		if (landCooling || lcd == 1 || (idleAudit
				&& PowerUtils.getResourceValue(player, LAND_HUD) != 0)) {
			if (landCooling) PowerUtils.countDownAndSync(player, LAND_HUD, Math.max(0, lcd - 1));
			else PowerUtils.setResourceValueAndSync(player, LAND_HUD, Math.max(0, lcd - 1));
		}
		boolean sneaking = player.isShiftKeyDown();
		boolean wasSprintKey = WAS_CLIENT_SPRINT.getOrDefault(id, false);
		boolean wasSneaking = WAS_SNEAKING.getOrDefault(id, false);
		WAS_CLIENT_SPRINT.put(id, sprintKey);
		WAS_SNEAKING.put(id, sneaking);

		// 鏈В閿?water_spurt 鑺傜偣锛氬彧鏇存柊鐘舵€併€佷笉瑙﹀彂浠讳綍鍐插埡
		if (!RegEvolutionComponent.EVOLUTION.get(player).isUnlocked(AxolotlTree.NODE_WATER_SPURT)) {
			return;
		}

		boolean inWater = player.isInWater();
		int cdTicks = BAL.i("cd_ticks", CD_TICKS);

		if (inWater) {
			// ===== 姘翠笅鍐插埡锛氭按閲?+ 鎸変笅鐪熸鐤捐窇閿紙涓婂崌娌匡級鈫?鍓嶅啿锛屽厤璐癸紙鍙屽嚮 W/娓告吵鑷姩鐤捐窇涓嶈Е鍙戯級=====
			if (sprintKey && !wasSprintKey && WATER_CD.getOrDefault(id, 0) <= 0) {
				doDash(player);
				WATER_CD.put(id, cdTicks);
				PowerUtils.setResourceValueAndSync(player, WATER_HUD, cdTicks);
			}
			// 姘撮噷鍙搷搴旂柧璺戦敭鍒嗘敮锛岄殧绂婚檰鍦版綔琛岄€昏緫
			return;
		}

		// ===== 闄嗗湴鍐插埡锛氶檰鍦?+ 鎸変綇鐪熸鐤捐窇閿椂鎸夋綔琛岋紙sneak 涓婂崌娌匡級鈫?鍓嶅啿锛屾秷鑰楁箍娑﹀害 =====
		if (sneaking && !wasSneaking && sprintKey && LAND_CD.getOrDefault(id, 0) <= 0) {
			int landCost = landAirCostForHud(player);
			if (player.getAirSupply() < landCost) {
				return; // 婀挎鼎搴︿笉瓒筹紝涓嶈Е鍙?
			}
			player.setAirSupply(player.getAirSupply() - landCost);
			doDash(player);
			LAND_CD.put(id, cdTicks);
			PowerUtils.setResourceValueAndSync(player, LAND_HUD, cdTicks);
		}
	}

	/** 娌胯绾垮墠鍐蹭竴娆★紙姘撮檰涓€鑷寸殑浣嶇Щ涓庤鍚弽棣堬級銆?*/
	private static void doDash(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		double burst = BAL.d("burst", BURST);
		player.push(look.x * burst, look.y * burst, look.z * burst);
		player.hurtMarked = true;

		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2f, 1.0f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1.3f, 0.7f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SPLASH, player.getX(), player.getY() + 0.6, player.getZ(),
				60, 0.6, 0.4, 0.6, 0.6);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.5, player.getZ(),
				40, 0.5, 0.4, 0.5, 0.5);
		github.com.gengyoubo.sscfa.util.ParticleUtils.spawnWaterBurst(
				sw, player, player.getX(), player.getY() + 0.6, player.getZ(), 1.2);
	}

	public static void onPlayerDisconnect(UUID id) {
		CLIENT_SPRINT.remove(id);
		WAS_CLIENT_SPRINT.remove(id);
		WAS_SNEAKING.remove(id);
		WATER_CD.remove(id);
		LAND_CD.remove(id);
	}
}
