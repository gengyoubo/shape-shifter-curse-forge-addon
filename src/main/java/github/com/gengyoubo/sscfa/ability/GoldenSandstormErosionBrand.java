package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.GoldenSandstormPerks;
import github.com.gengyoubo.sscfa.item.ErosionSandPrismItem;
import github.com.gengyoubo.sscfa.item.WitheredSandRingItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.ParticleUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 闂佸弶鍨堕惌娆忕暆濮濆└ - 濞撴矮绲诲▓宀勬倻濞嗗繐绁柨娑樼墣椤箓宕濋妸锔惧灱閻犱焦澹嗛柈瀵哥磼閻曞倻绀?
 * <p>
 * 閺夆晜鍨堕崹顒勫绩鐠囨彃姣婇柡鍐╁劶閸ゆ粓宕濋妸銉ョ秾闁告梻濮抽梹婊堟憖閳ь剟鎮滃▎蹇撶オ闁挎稑鏈惁锛勭矓閹烘挻浠樺鑸佃壘瑜?閻忕偛鍋婄槐婵喰掕箛搴ㄥ殝闁烩晩鍠楅悥锝夊嫉閳ь剚寰?閻忕偛鍊堕埀?
 * 闁绘垶鐟ュ畵鍐箰娴ｈ櫣鏁?0缂佸甯槐娆徯掕箛鏃戝仹闁告瑧濮存慨鐐哄礆闁垮鐓€閻犱讲鍓濆鍌炲闯椤帞绀嗛柕?
 * <p>
 * 閻忕偛鍊归弳鐔革紣濠婂棗顥忛悗鐢垫嚀缁ㄦ煡鏁嶉崼銏℃殢濞存粌绌瞡tity_glow闁挎稑顧€缁?
 * - 1閻?闁?濮掓稑瀚竟?
 * - 2閻?闁?婵鐟ㄦ竟?
 * - 3閻?闁?缂佷勘鍨兼竟?
 * - 缂備浇鍎绘竟濠囨偐閼哥鍋?闁?鐎规瓕灏闁告瑦鍨奸～锕傚礉閵娧冪€婚柛娆愬煀缁?缂佸甯掗崬瀛樼▔瀹ュ懎璁查柛鎰У椤愬ジ宕ｉ悩鑼勾闁挎稑鐗忕挒銏ゆ嚌閸欏鍨奸煫鍥殕鐎垫梻绱?0缂佸甯槐?
 * <p>
 * 缂?婵炲棌鍓濋弫楣冨礄娴兼瑧绀?閻忕偛鍊瑰褔宕ユ惔锝嗗煕缂備緡鍘介弫楣冨礄娴兼瑧绀嗛悷娆欑畱瑜板倻鎮銏犘楅柣鏍ф瑜板倿鏁?
 * - 闂侇偆濮甸崹姘舵儎椤旂晫鍨肩憸鐗堟尭婢х娀鎮介悢閿嬪殥闁?0%闁汇劌瀚┑鈧悗鍦劜缁辨瑦绋夋繝鍥€?0闁?
 * - 闁煎浜ｉ棅鈺呭炊閻愬樊妲?0%闁哄牃鍋撳鍫嗗懏鏅搁柛娑滄閳?
 * - 婵炴挸鎳樺▍搴ㄥ箥閳ь剟寮垫径濠勬勾闁轰礁搴滅槐婵囨交濞戞ê寮崇紓浣藉劵婢瑰﹪鎮╅懜纰樺亾娓氬﹦绀?缂佸甯婄粭澶愬矗椤栨艾缍岄悘鐐插亰缁辨繄绱掗懗顖氼棌闁哄秴娲ょ换鏃堝箰娴ｈ櫣鏁?0缂佸甯槐?
 * - 缂備浇鍎绘竟濠囨偐閼哥鍋?缂佸甯掗崰搴ㄥ础鐎涙ɑ鍩傞柛姘瑜版煡鏌屽鍡樼厐闁告瑧濮撮惇?
 * <p>
 * 鐎殿喗娲滈崹搴ㄦ晬閸噥鍋ч悷鏇氱劍婵⊙囨嚄閺傘倗绀嗛柨?
 * - 闂侇偆濮甸崹姘舵儎椤旂晫鍨肩憸鐗堟尭婢х娀鎮介悢閿嬪殥闁?0%闁汇劌瀚┑鈧悗鍦劜缁辨瑦绋夋繝鍥€?0闁?
 * - 闁煎浜ｉ棅鈺呭箰婢跺﹤鍤掗柟鍦枔閺佹捇宕ㄩ挊澶嗗亾閼测晜鐣?0%闁搞儳鍋涢ˇ鍙夌▔閳ь剙鈻?
 */
public class GoldenSandstormErosionBrand {

	// ==================== 閻㈩垱鎮傞崳?====================
	// 閻㈩垽璐熼埀顒€鐭傜划顖滄媼閵堝啠鍋撳鍡樻殘闂佹彃锕﹀▓鎴犳暜閹间礁娅ょ€规瓕灏欏▍銉ф媼?balance闁挎稑婢僣ope闁挎稒鐡玝ilities.golden_sandstorm_erosion_brand闁挎稑顧€缁辨繃娼婚幇顖ｆ斀闁哄啯婀圭划鐘虹疀椤愩倕寮鹃悹鍥嚙瑜板洭鏁?
	// 闁稿繑婀圭紞鎴︽晬閸︾帄ACK_COOLDOWN* / BURST_DAMAGE_CAP_WITH_RING / PRISM_SPLASH_PERCENT闁挎稑顦板﹢顓㈡儌閺勫浚鍞堕柨娑樺缁绘岸骞愭担铏规そ閻犲洦鍨跺﹢锛勬暜閹间礁娅ら柕?
	/** 闁绘垶鐟ュ畵鍐箰娴ｈ櫣鏁鹃柡鍐ㄧ埣濡潡鏁嶉崸鐪巆k闁挎稑顧€缁辨繂袙韫囨棏鍋ч柛娆戝Т婵偤宕氶柨瀣厐闁靛棗鍊跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final int BRAND_DURATION = 200; // 10缂?
	/** 缂備浇鍎绘竟濠囨偐閼哥鍋撴担鐟扮槷缂備緡鍘藉鍌炴⒒鏉堝墽绀則ick闁挎稑顧€缁辨繃绂掗崨顖涙殢濞存粌姘﹂～瀣喆婢跺鍨奸悹浣藉焽閳ь剙鍊跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final int GREEN_DURATION = 200; // 10缂?
	/** 闁绘牕妫楄ぐ鍌炲触鎼粹€崇秾閻忕偛鍊搁崰搴ㄥ础鐎涙ɑ顦ч梻鍌濇彧缁辨獩ick闁挎稑顧€缁辨繈宕樺畡鏉跨ケ缂備焦鎸诲顐﹀触鎼粹€宠闂佹彃绉甸弻濠囧矗閻樿尙婀撮柕鍡楀€跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final int BRAND_STACK_COOLDOWN = 100; // 5缂?
	/** 闁哄牃鍋撳鍫嗗啫缍岄柛鏃傚Т閻即寮懜顑藉亾閸岀偟甯涢悹浣靛€х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final int MAX_STACKS = 3;
	/** 閻炴凹鍋勬慨鈺呮偉閸℃绲洪柣銏㈠枎閹筹繝鎯傞幆褍鐎绘慨锝嗘煣濠碘偓閻庡湱鍋ㄩ埀顒€鍊跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final float BURST_HP_PERCENT = 0.20f;
	/** 閻炴凹鍋勬慨鈺呮偉閸℃绲哄ù绗哄€曢濠冪▔婵犲洦顎欓柕鍡楀€跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final float BURST_DAMAGE_CAP = 20.0f;
	/** 閻炴凹鍋勬慨鈺呮偉閸℃绲洪柤濂変簼閸撱倝鎯傞幆褍鐎绘慨锝嗘煟閳ь剙鍊跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final float BURST_HEAL_PERCENT = 0.10f;
	/** 闁告瑧濮撮惇浼村礃瀹勬澘绁遍柨娑樻緟ick闁挎稑顧€缁辨繈宕ョ仦鑲╊伇闁烩晩鍠楅悥?缂佸甯掗崬鎾嫉閳ь剚寰勫顒€缍?閻?*/
	private static final long STACK_COOLDOWN = 20L;
	/** 闁炬挴鍋撴繛灞剧懄閿涙垿寮查幆閭︽濠㈣泛娲﹀鍌炴儍閸曨偄缍岄悘鐐插€搁崰搴ㄥ础鏉堝墽绀則ick闁挎稑顧€缁?.3缂?*/
	private static final long STACK_COOLDOWN_WITH_PRISM = 26L;
	/** 闁哄鍨遍惌娆撳箰閸モ晛绠氶悷浣告噹椤︻剟寮崜浣圭暠閻炴凹鍋勬慨鈺呮偉閸℃绲哄ù绗哄€曢濠冪▔婵犲洦顎欓柨?30%闁?*/
	private static final float BURST_DAMAGE_CAP_WITH_RING = 26.0f;
	/** 闁炬挴鍋撴繛灞剧懄閿涙垿寮查懜闈涒挅闁轰緤绲煎┑鈧悗鐟扮枃鐎垫牠宕舵潏鍓х闁哄秶銆嬬槐姘跺Υ閸岀偟甯涢悹浣靛€х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final double SPREAD_RANGE = 5.0;
	/** 闁圭鏅滈弳搴ㄥ矗閻樺磭鍨奸悹浣规緲瀹曟劕顕ラ崟鍓佺闁哄秶銆嬬槐姘跺Υ閸岀偟甯涢悹浣靛€х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final double SPLASH_BRAND_RANGE = 4.0;
	/** 鐎殿喗娲滈崹搴ㄦ嚊椤忓懎澧归柨娑欒壘濞叉牗寰勫鍛殥闁瑰湱鍠撻弫鎾诲川閽樺鍋撻懖鈺傜暠闁谎勫劤閸ㄥ骸袙閺傜儵鍋撻崒鐐靛笡閻犱降鍊х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲?*/
	private static final float DETONATE_HEAL_LOST_PERCENT = 0.20f;
	/** 闁炬挴鍋撴繛灞剧懄閿涙垿寮茬拋鍦獥闁圭鏅滈弳搴㈠閵堝拋鍞洪柛妤冨Т缁扁晠鎮ラ崱鏇熺负閻庣懓纾▓鎴澬掗弬鍓т紣闁?0%闁?*/
	private static final float PRISM_SPLASH_PERCENT = 0.40f;

	// ==================== 闁煎浜滈悾鐐▕婢跺绺洪悗鐟扮－鐞氼偊宕?====================
	/** 閻炴凹鍋勬慨鈺呮偉閸℃绲哄ù绗哄€曢濠勭尵鐠囪尙鈧兘鏁嶉崼銏㈡悘閺夆晛娲╃换搴ㄥ箣濡粯鏆伴柛鎴ｅ吹濞堟垶瀵奸妶鍜佸敽闁稿繐绉堕弻鍛暜瑜濈槐?*/
	private static final ResourceKey<DamageType> CURSED_BURST_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_burst"));
	/** 鐎殿喗娲滈崹搴㈠閵堝拋鍞虹紒顐ヮ嚙閻庣兘鏁嶉崼銏㈡悘閺夆晛娲╅～锕傚礉閵娧冪€婚柛娆愬灩濞堟垶瀵奸妶鍜佸敽闁稿繐绉堕弻鍛暜瑜濈槐?*/
	private static final ResourceKey<DamageType> CURSED_EROSION_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("my_addon", "cursed_erosion"));

	// ==================== 缂傚啯鍨圭划鍫曞触鐏炵虎鍔?====================
	/** S2C 闁告牕灏嘍闁挎稒鑹鹃幃鎾愁潰閵壯冨妼闁告ぞ鍗抽·渚€鎳濋崣澶嬫闁硅鍠栭崺宀€鈧箍鍨洪崺娑氱博?*/
	public static final ResourceLocation PACKET_BRAND_SYNC = new ResourceLocation("ssc_addon", "erosion_brand_sync");
	/** 闂傚洠鍋撻悷鏇氱閹挸顫㈤妷褎鐣遍柣婧炬櫅椤斿秹寮介崶顏嶅敹闂傚棗妫楅幃?*/
	private static final Set<UUID> DIRTY_PLAYERS = ConcurrentHashMap.newKeySet();

	// ==================== 闁绘鍩栭埀顑挎祰閹风兘鐓?====================
	/** 闁绘壕鏅涢宄孶ID -> { 闁烩晩鍠楅悥顤沀ID -> 闁绘垶鐟ュ畵鍐偐閼哥鍋?} */
	private static final ConcurrentHashMap<UUID, Map<UUID, BrandState>> ACTIVE_BRANDS = new ConcurrentHashMap<>();

	// 闂傚啳鍩栭?5闁挎稒纰嶅﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲绠涢銈呭季閻犲洩顕цぐ鍥晬閸繃褰ラ柣鎾楀嫭寮撻柛鎺撶箓椤劙宕犻弽褎绀€闂侇偀鍋撳娑欘焾椤撹崵鏁幖浣告闁?
	private static final BalanceReader BAL = new BalanceReader("abilities.golden_sandstorm_erosion_brand");
	private static final BalanceReader CAST_BAL = new BalanceReader("systems.form_casting");

	private GoldenSandstormErosionBrand() {
	}

	// ==================== 濡ゆ婢橀幖褍螞閳ь剟寮?====================

	/** 婵☆偀鍋撻柡灞诲劤鐢櫣鈧鍩栧Σ鎼佸触閿曚緡妫呭璺烘矗缁繝鎽€閳ь剙鈻藉▎鎰剁搐闁哄懓顔愮槐娆撳矗瀹€鍕婵☆偀鍋撴繛鏉戭儜缁辩殞ick閺夆晛鈧喖鍤?+ 闁硅泛鈧喕鏉介悘鐐插€归悡锛勬嫚椤喚绀?*/
	private static boolean hasErosionPrism(ServerPlayer player) {
		// 闁哄倽顫夌涵?闁挎稒顒甶ck闁搞儳鍋犻惃鐔告交閸婄喖鍤?
		if (ErosionSandPrismItem.isEquippedBy(player)) return true;
		// 闁哄倽顫夌涵?闁挎稒淇洪摂瀣浖閸℃浠搁柡鍐Т閸櫻囧箮閸婄喕鏉介悘鐐插亰缁辨瑩宕ユ惔鈽嗘У闁哄倽顫夐、宥夋晬瀹€鍐ㄦ闁告柣鍔戦埀顒€鍊块崢?Trinkets/Curios闁?
		return TrinketUtils.isWearing(player, stack -> stack.getItem() instanceof ErosionSandPrismItem);
	}

	/** 婵☆偀鍋撻柡灞诲劤鐢櫣鈧鍩栧Σ鎼佸触閿曚緡妫呭璺烘矗缁繝寮搁娑氱厯闁圭娲ㄩ獮鍡涙晬閸繂钃熼梺鎻掔У椤ュ懎霉鐎ｅ墎绐梩ick閺夆晛鈧喖鍤?+ 闁硅泛鈧喕鏉介悘鐐插€归悡锛勬嫚椤喚绀?*/
	private static boolean hasWitheredRing(ServerPlayer player) {
		// 闁哄倽顫夌涵?闁挎稒顒甶ck闁搞儳鍋犻惃鐔告交閸婄喖鍤?
		if (WitheredSandRingItem.isEquippedBy(player)) return true;
		// 闁哄倽顫夌涵?闁挎稒淇洪摂瀣浖閸℃浠搁柡鍐Т閸櫻囧箮閸婄喕鏉介悘鐐插亰缁辨瑩宕ユ惔鈽嗘У闁哄倽顫夐、宥夋晬瀹€鍐ㄦ闁告柣鍔戦埀顒€鍊块崢?Trinkets/Curios闁?
		return TrinketUtils.isWearing(player, stack -> stack.getItem() instanceof WitheredSandRingItem);
	}

	/**
	 * 閺夆晜鍨堕崹顒勫绩鐠囨彃姣婇柡鍐╁劶閻ㄧ喖鎮?- 闁煎浜滄慨鈺呭矗閻樻彃顫ｉ柣鎴炵懃瀹?
	 * 閻犲洢鍎查弻鐔封枖閺囩姵鏆?SscAddonActions 闁?erosion_brand_hit biEntity action 閻熸瑱绠戣ぐ?
	 */
	public static void onPlayerAttack(ServerPlayer player, LivingEntity target) {
		if (github.com.gengyoubo.sscfa.spell.CourtSpellLink.suppressLegacy()) return;
		applyBrand(player, target, 1);
	}
	public static void onSpellHit(ServerPlayer player, LivingEntity target) {
		if (WhitelistUtils.isProtected(player,target)) return;
		applyBrand(player,target,1);
	}

	/** A sandstorm adds its captured layer bonus in one operation, not two attacks. */
	public static void onSandstormHit(ServerPlayer player, LivingEntity target, int layers) {
		applyBrand(player, target, Math.max(1, layers));
	}

	private static void applyBrand(ServerPlayer player, LivingEntity target, int layers) {
		if (!FormUtils.isGoldenSandstormSP(player) || !target.isAlive()) return;
		if (!(player.level() instanceof ServerLevel serverWorld)) return;
		if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) return;
		// 閺夆晜鍨堕崹顒勫绩鐠囨彃姣婂☉鎾崇У椤ュ懘寮婚妷褎顏ら柛姘Т瀹曠喖鏁嶉崼婵嗗笒閻犱胶顭堥顕€鎯傞挊澶嬪€抽柛妤佹礈濞蹭即寮介崶褍缍岄柡宥呮穿椤斿洭鏁?
		// 闁谎嗘閹洟宕￠弴姘辩煂闂傚嫭鍔曢崺妤呭箥閳哄倹娈犻柛婊冭嫰缁扁晠鎮ラ崱娆愮暠闁告瑧濮甸悥锝囨媼?

		UUID playerUuid = player.getUUID();
		UUID targetUuid = target.getUUID();
		long currentTick = serverWorld.getGameTime();

		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.computeIfAbsent(playerUuid, k -> new HashMap<>());
		BrandState state = playerBrands.get(targetUuid);
		if (state != null && currentTick >= state.expiryTick) {playerBrands.remove(targetUuid);state=null;}
		long effectiveCooldown = (hasErosionPrism(player) ? STACK_COOLDOWN_WITH_PRISM : STACK_COOLDOWN)
				* CAST_BAL.i("sandstorm_stack_cooldown_multiplier",2);
		// One shared gate includes the hit that bursts a full stack; no rapid fourth-hit bypass.
		if (state != null && !state.greenState && currentTick-state.lastStackTick<effectiveCooldown) {
			state.expiryTick=currentTick+BAL.i("brand_duration",BRAND_DURATION);markDirty(playerUuid);updateTargetMarker(target,state,currentTick,player);return;
		}

		if (state != null && state.greenState) {
			// 闁告瑧濮撮惇浼村礃瀹勬澘绁遍柡鍫㈠枛濡潡鏁嶉崼婵嗩枀5缂佸甯槐姘舵晬濮橆偆鐟濋柛娆戝Т閻即鏁嶇仦鑲╃憹閻熸瑱绠戣ぐ鍌滄偖椤愩垹袟闁绘牕妫楄ぐ?
			if (currentTick < state.stackCooldownExpiryTick) {
				return;
			}
			// 闁告劕鍢插畵鍫㈢磼閹惧瓨灏嗛柨?-10缂佸甯槐姘舵晬鐏炶棄绲挎繛鎴濈墢鐠炪垽鎳濋懠顒€笑闁诡兛绶ょ槐婵囩?閻忕偛鍊哥槐鎴炴叏鐎ｎ偒鍔€閻㈩垳顭堣ぐ鏃備沪?
			state.greenState = false;
			state.stacks = 0;
		}
		// 闁哄秴娲╅鍥閳ь剛鎲版担鍛婂€辨慨?
		markDirty(playerUuid);

		if (state != null && state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
			// 鐎圭寮跺?閻忕偛鍋婄槐婵堢箔?婵炲棌鍓濋弫楣冨礄閺勫警鏇㈠矗閹达綆娼堕柛鏂诲妿閸ㄥ酣宕?
			// !! 闊洤鎳橀妴蹇涘礂閸儱娅㈢紓鍐惧枤婵悂骞€娴ｇ鏅欓梺顐ゅУ閸ㄦ碍瀵奸妶鍜佸敽闁挎稑鑻幆渚€宕?target.damage() 濞村吋宀搁埀顒佷亢缁?Apoli action 闂侇偅甯掔紞濠勬嫬閸愵亝鏆?onPlayerAttack
			state.stacks = 0;
			state.greenState = true;
			int greenDuration = BAL.i("green_duration", GREEN_DURATION);
			int stackCooldown = BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
			state.greenExpiryTick = currentTick + greenDuration;			state.stackCooldownExpiryTick = currentTick + stackCooldown;			state.expiryTick = currentTick + greenDuration;

			// 闁告艾鏈鐐哄冀閸ヮ亶鍞堕柡浣哥墛閻忓宕氶弶娆惧悅闁规挳顥撻顒勬晬閸喐鏉归柛锔哄妺濠碘偓閻庣懓鍟挎晶鐘垫兜椤旇崵绠介柛妤勬珪濡炲倿鎮介悢鍛婃珡闁?
			updateTargetMarker(target, state, currentTick, player);

			// 闁绘粍婢樺﹢顏嗏偓鐟邦槸閸欏繘宕锋导绮瑰亾閻樺啿鐏囧ù绗哄€曢?
			triggerPassiveBurst(player, target, serverWorld);

			// 闁炬挴鍋撴繛灞剧懄閿涙垿寮茬拋鍦獥閻炴凹鍋勬慨鈺呮偉閸℃绲哄☉鏃傚枙琚濋柛娆愬灦婢у潡寮敐蹇曠闁谎嗘閹洟宕￠弴鐘崇獥闁哄秴娲ｇ粭澶屾喆閿曗偓瑜板倿骞嶉埡鍌涙疇闁?
			if (hasErosionPrism(player) && !WhitelistUtils.isProtected(player, target)) {
				float burstDmg = Math.min(target.getHealth() * damagePercent(player), damageCap(player, hasWitheredRing(player)));
				if (burstDmg < 1.0f) burstDmg = 1.0f;
				float splashDmg = burstDmg * PRISM_SPLASH_PERCENT;
				splashDamageToNearby(player, serverWorld, target.position(), splashDmg, target);
			}

			// 闁绘牕妫楄ぐ鍌炴閾忣偅娅忛柛婊冪灱閻垹鈧?
			serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.38f, 1.2f);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					30, 0.5, 0.5, 0.5, 0.1);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					1, 0, 0, 0, 0);
			return;
		}

		// 婵繐绲介悥鍫曞矗閻樿尙婀?
		if (state == null) {
			state = new BrandState();
			playerBrands.put(targetUuid, state);
		}

		// 婵☆偀鍋撻柡灞诲劚瑜版梻浠﹂崒姘瀻闁告鎻槐娆撴憖閳ь剙鈻藉▎鎰剁搐闁哄懓顔愮槐?.3缂佸甯槐婵囶渶濡鍚囬柨?缂佸甯槐?
		if (currentTick - state.lastStackTick < effectiveCooldown) {
			// 闁告劕鍢插畵鍫熺▔椤撱劎绀夐柛鎺楁敱閺屽﹪骞愭担铏规暰闁哄啫鐖煎Λ鎸庢媴閸℃洜鐟濋柛娆戝Т閻?
			state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);
			updateTargetMarker(target, state, currentTick, player); // 闁告帡鏀遍弻濠囧冀閸ヮ亶鍞堕柟闀愯兌閻㈠寮崼鏇燂紵
			return;
		}

		state.stacks = Math.min(state.stacks + layers, BAL.i("max_stacks", MAX_STACKS));
		state.lastStackTick = currentTick;
		state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);

		// 闁告艾鏈鐐哄冀閸ヮ亶鍞堕柡浣哥墛閻忓宕氶弶娆惧悅闁规挳顥撻?
		updateTargetMarker(target, state, currentTick, player);

		// 闁告瑧濮存慨鐐靛垝閹烘垹鎽嶉柟缁樺姉閵?
		ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
				target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				5 * state.stacks, 0.2, 0.2, 0.2, 0.05);

		// 闁告瑧濮存慨鐐烘閾忣偅娅?
		serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 0.8f + state.stacks * 0.2f);
	}

	/**
	 * 閻熸瑱绠戣ぐ鍌滄偖椤愩垹袟闁绘牕妫楄ぐ?- 闂侇偆濮甸崹?0%鐟滅増鎸告晶鐘绘偨閻旈攱鍤掑ù绗哄€曢濠囨晬閸粎鐟愰梻?0闁挎稑顧€缁辨繈鎳涢鍛亯闁搞儳鍋涢ˇ?0%闁哄牃鍋撳鍫嗗懏鏅搁柛?
	 */
	private static void triggerPassiveBurst(ServerPlayer player, LivingEntity target, ServerLevel serverWorld) {
		// 閻犱緤绱曢悾缁樺閵堝拋鍞洪柨娑欐皑濞蹭即寮介崶褏绉奸柛鎾崇Ф閺佹捇宕ㄩ挊澶嗗亾閼测晜鐣?0%闁挎稑鐗婇悘鐔封柦濞嗘劕鐦归柣婊庡灲缁辩増绋夋繝鍥€?6闁挎稑鐭傜划顖滄媼閵堝繒绐楀☉鎾筹躬濡?0闁?
		boolean hasRing = hasWitheredRing(player);
		float cap = damageCap(player, hasRing);
		float rawDamage = target.getHealth() * damagePercent(player);
		float damage = Math.min(rawDamage, cap);
		if (damage < 1.0f) damage = 1.0f;

		// 闂侇偆濮甸崹姘舵嚊椤忓嫮鏆板☉鏂款槷濠碘偓閻庡湱顒茬槐娆戠磼閺囷紕绠栭弶鈺傚灦閸剟寮ㄧ拠鎻掓瘖闁汇劌瀚┑鈧悗鐟板暱閸樸倝鎮块銏″闁?
		Vec3 oldVelocity = target.getDeltaMovement();
		target.invulnerableTime = 0; // 闂佹彃绉堕悿鍡樺閵堝拋鍞洪柛蹇撶Ф閺屽懐鎷嬮埄鍐╊槯闁?
		boolean damaged = target.hurt(new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_BURST_KEY), player), damage);
		if (damaged) {
			target.setDeltaMovement(oldVelocity); // 濞戞挸绉撮崵顕€鏌呴埀?
		}

		// 闁煎浜濋崹婊堝炊閻愬樊妲?0%闁哄牃鍋撳鍫嗗懏鏅搁柛娑滄閳?
		float healAmount = player.getMaxHealth() * (float) BAL.d("burst_heal_percent", BURST_HEAL_PERCENT);
		player.heal(healAmount);
	}

	/**
	 * 婵炲枴銈庢矗闁瑰灈鍋撻柤瀹犳缁扁晠鎮?- 鐎殿喗娲滈崹搴ㄥ箥閳ь剟寮垫径鎰缂備浇鍎绘竟濠囧冀閸ヮ亶鍞堕柣銊ュ濞蹭即寮?
	 * @param player 闁哄倽濮ょ涵鍫曟嚀?
	 * @return 鐎殿喗娲滈崹搴㈢┍閳╁啩绱栭柨娑欑摕鐎殿喗娲滈崹搴ㄦ儎椤旂晫鍨奸柡? 闁诡剝顕ч惇浼村极閻?
	 */
	public static int[] detonateAll(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player)) return new int[]{0, 0};
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		if (playerBrands == null || playerBrands.isEmpty()) return new int[]{0, 0};

		if (!(player.level() instanceof ServerLevel serverWorld)) return new int[]{0, 0};

		// 濡澘瀚崢娑樜涢埀顒勫蓟閵夆斂鍋橀柛婵呰兌婵悂骞€娓氬﹦绀勯梺顒€鐏濋崢銈咁嚗椤忓棗绠氶柛鎰嚇閸ｅ憡寰勫鍡╂⒕闁哄被鍎荤槐?
		boolean hasRing = hasWitheredRing(player);
		boolean hasPrism = hasErosionPrism(player);
		float cap = damageCap(player, hasRing);
		int totalTargets = 0;
		int totalStacks = 0;

		// Prism splash can create more entries. Only detonate the accepted target snapshot.
		for (Map.Entry<UUID, BrandState> entry : new ArrayList<>(playerBrands.entrySet())) {
			if (playerBrands.get(entry.getKey()) != entry.getValue()) continue;
			BrandState state = entry.getValue();

			// 缂備浇鍎绘竟濠囨偐閼哥鍋撴担鐑樼暠闁烩晩鍠楅悥锝嗙▔瀹ュ懎妫樺☉鎾抽缁扁晠鎮?
			if (state.greenState || state.stacks <= 0) {
				continue;
			}

			LivingEntity target = findEntity(serverWorld, entry.getKey());
			if (target == null || !target.isAlive()) {
				playerBrands.remove(entry.getKey(), state);
				continue;
			}
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) continue;

			// 濠碘€冲€归悘澶愭儎椤旂晫鍨奸柡?閻忕偛鍋婄槐婵嬪礂閸及鏇㈠矗閹达綆娼堕柛鏂诲妿閸ㄥ酣宕ｉ幋顖滅濞戞挸绉电粔鐑芥嚀濡も偓閻即寮敮顔剧
			if (state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
				triggerPassiveBurst(player, target, serverWorld);
				// 閻炴凹鍋勬慨鈺呮偉閸℃绲洪柣銊ュ椤ゅ倹寰勯弽顐ょ厛閻?
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
						target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
						1, 0, 0, 0, 0);
			}

			totalTargets++;
			totalStacks += state.stacks;

			// 鐎殿喗娲滈崹搴㈠閵堝拋鍞?= 闁烩晩鍠楅悥锝堛亹閹惧啿顤呴柣銏㈠枎閹筹繝宕?0%闁挎稑鐗呯粭鍌炴⒔?0/26闁?
			float rawDetonateDmg = target.getHealth() * damagePercent(player);
			float detonateDamage = Math.min(rawDetonateDmg, cap);
			if (detonateDamage < 1.0f) detonateDamage = 1.0f;

			// 闁炬挴鍋撴繛灞剧懄閿涙垿寮茬拋鍦獥濞戞捁宕靛ú浼村冀閸ャ劌顥為柛?0%闁?0%闁圭鏅滈弳搴ｇ磼濞嗗繑鍣柛?
			float mainDamage = hasPrism ? detonateDamage * (1.0f - PRISM_SPLASH_PERCENT) : detonateDamage;
			Vec3 oldVelocity = target.getDeltaMovement();
			target.invulnerableTime = 0; // 闂佹彃绉堕悿鍡樺閵堝拋鍞洪柛蹇撶Ф閺屽懐鎷嬮埄鍐╊槯闁?
			boolean detonateResult = target.hurt(new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_EROSION_KEY), player), mainDamage);
			if (detonateResult) {
				target.setDeltaMovement(oldVelocity);
			}

			// 闁炬挴鍋撴繛灞剧懄閿涙垿寮茬拋鍦獥40%鐎殿喗娲滈崹搴㈠閵堝拋鍞洪柟纰樻櫆閺嗗海绱掑▎蹇斿櫙闁?闁哄秶鍘ч崬鎾儍閸曨厽鏅搁柣?
			if (hasPrism) {
				float splashDamage = detonateDamage * PRISM_SPLASH_PERCENT;
				splashDamageToNearby(player, serverWorld, target.position(), splashDamage, target);
			}

			// 鐎殿喗娲滈崹搴ｅ垝閹烘垹鎽?
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					20 * state.stacks, 0.5, 0.5, 0.5, 0.1);

			// 閺夆晜绋戦崣鍡欑磼閼愁垰顥忛柣妯垮煐閳?
			long currentTick = serverWorld.getGameTime();
			int greenDuration = BAL.i("green_duration", GREEN_DURATION);
			state.stacks = 0;
			state.greenState = true;
			state.greenExpiryTick = currentTick + greenDuration;
			state.stackCooldownExpiryTick = currentTick + BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
			state.expiryTick = currentTick + greenDuration;

			// 闁告艾鏈鐐哄冀閸ヮ亶鍞堕柡浣哥墛閻忓宕氶弶娆惧悅闁规挳顥撻?
			updateTargetMarker(target, state, currentTick, player);
		}

		// 闁煎浜濋崹婊堝炊閻愬樊妲婚柨娑欒壘閸戯繝骞戦悢鐑樻櫢闁告稖妫勯埀顒冨亹濞?0%
		if (totalTargets > 0) {
			float lostHealth = player.getMaxHealth() - player.getHealth();
			if (lostHealth > 0) {
				float healAmount = lostHealth * (float) BAL.d("detonate_heal_lost_percent", DETONATE_HEAL_LOST_PERCENT);
				player.heal(healAmount);
			}
			markDirty(player.getUUID());
		}

		return new int[]{totalTargets, totalStacks};
	}

	private static float damagePercent(ServerPlayer player) {
		return GoldenSandstormPerks.damagePercent(player, (float) BAL.d("burst_hp_percent", BURST_HP_PERCENT));
	}

	private static float damageCap(ServerPlayer player, boolean ring) {
		return GoldenSandstormPerks.damageCap(player, ring ? BURST_DAMAGE_CAP_WITH_RING : (float) BAL.d("burst_damage_cap", BURST_DAMAGE_CAP));
	}

	/** Consume this player's valid, loaded, non-green layers without running any damage path. */
	public static int recallAll(ServerPlayer player) {
		if (!FormUtils.isGoldenSandstormSP(player) || github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return 0;
		var brands = ACTIVE_BRANDS.get(player.getUUID());
		if (brands == null || brands.isEmpty()) return 0;
		var world = player.serverLevel();
		long now = world.getGameTime();
		int layers = 0;
		boolean changed = false;
		for (var entry : new ArrayList<>(brands.entrySet())) {
			var state = entry.getValue();
			if (state.greenState || state.stacks <= 0) continue;
			var target = findEntity(world, entry.getKey());
			// An unloaded target or a target in another dimension cannot fund healing.
			if (target == null) continue;
			if (state.expiryTick <= now || !target.isAlive()) {
				if (brands.remove(entry.getKey(), state)) {
					refreshOtherMarkers(target, player.getUUID());
					changed = true;
				}
				continue;
			}
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, target)) continue;
			if (brands.remove(entry.getKey(), state)) {
				layers += state.stacks;
				refreshOtherMarkers(target, player.getUUID());
				changed = true;
			}
		}
		if (changed) {
			if (brands.isEmpty()) ACTIVE_BRANDS.remove(player.getUUID(), brands);
			markDirty(player.getUUID());
			syncIfDirty(player);
		}
		if (layers > 0) player.heal(GoldenSandstormPerks.recallHealing(player.getMaxHealth(), layers));
		return layers;
	}

	/** Shared status icons must not disappear when another owner's layers are removed. */
	private static void refreshOtherMarkers(LivingEntity target, UUID removedOwner) {
		if (!(target.level() instanceof ServerLevel world)) return;
		BrandState selected = null;
		ServerPlayer owner = null;
		long now = world.getGameTime();
		for (var entry : ACTIVE_BRANDS.entrySet()) {
			if (entry.getKey().equals(removedOwner)) continue;
			var candidate = entry.getValue().get(target.getUUID());
			if (candidate == null || candidate.greenState || candidate.stacks <= 0 || candidate.expiryTick <= now) continue;
			var other = world.getPlayerByUUID(entry.getKey());
			if (!(other instanceof ServerPlayer player) || !FormUtils.isGoldenSandstormSP(player)) continue;
			if (selected == null || candidate.lastStackTick > selected.lastStackTick) { selected = candidate; owner = player; }
		}
		if (selected == null) removeAllBrandMarkers(target);
		else updateTargetMarker(target, selected, now, owner);
	}

	/**
	 * 闁炬挴鍋撴繛灞剧懄閿涙垿寮查懜鍨珡闁?- 閻庣數鎳撶槐鈺呮偉?闁绘牕妫楄ぐ鍌炴儎椤旂晫鍨奸柛娑栧妼濞?闁哄秶鍘ч崬鎾儍閸曨垱濮滈柣褑妫勯幃鏇㈠础閺囩姵鏅搁柣妞绘櫊閳ь剛濮甸崹姘跺箥閳哄倹娈犲ù绗哄€曢濠囨晬?
	 * 妤犵偠娉涢?闁哄秶鍘у畷鎰嚗閸曨厽鍊栭柛鎰噽濞堟垿鎯勯鐣屽灱濡増绻傞ˇ濠氬矗?閻忕偛鍊婚崕銉╁础閼割兘鍋撻崒婵嗩仧闁告瑧濮撮崺灞筋煥?閻忕偛鍊搁崹顖滄喆閿曗偓瑜板倻鎮銏犘楅柣鏍ф瑜板倿鐛崼鏇炴缂傚喚鍠栭惇浼村极閼割兘鍋?
	 */
	private static void splashDamageToNearby(ServerPlayer player, ServerLevel serverWorld, Vec3 center, float damage, LivingEntity excludeTarget) {
		UUID playerUuid = player.getUUID();
		UUID excludeUuid = excludeTarget != null ? excludeTarget.getUUID() : null;

		// 缁绢収鍠曠换姘跺嫉閳ь剚鎷呮惔婵囩负閻庣懓鍘栫拹?
		if (damage < 1.0f) damage = 1.0f;

		// 濞达綀娉曢弫銈嗘綇閸愩劊浜ｉ柣銊ュ鐎垫牠宕剁€涙ɑ鍋濈紒槌栨緛缁辨瑩宕ｉ弽锔界负閻庣懓鐤囩€垫牠宕堕弶鎴炲闁告瑧濮甸悥锝囨媼閹峰苯鐦遍柛銉ヮ嚟濞堟垶娼忛崘銊ｄ海闁稿﹦銆嬬槐?
		double spreadRange = BAL.d("spread_range", SPREAD_RANGE);
		double splashBrandRange = BAL.d("splash_brand_range", SPLASH_BRAND_RANGE);
		double maxRange = Math.max(spreadRange, splashBrandRange);
		AABB searchBox = new AABB(
				center.x - maxRange, center.y - maxRange, center.z - maxRange,
				center.x + maxRange, center.y + maxRange, center.z + maxRange
		);

		List<Entity> allEntities = serverWorld.getEntities(player, searchBox);

		DamageSource splashDamageSource = new net.minecraft.world.damagesource.DamageSource(player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_EROSION_KEY), player);
		long currentTick = serverWorld.getGameTime();
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.computeIfAbsent(playerUuid, k -> new HashMap<>());
		int hitCount = 0;

		// 闁衡偓閸洘鑲犻梻鍥ｅ亾閻熸洑娴囪闁告瑦鍨归崹搴ㄥ矗閹寸姵鐣遍柣鈺婂枟閻栵綁鏁嶉崼銉ょ級闁稿繐绉村﹢顏堟焼瀹ュ懎鍧婂☉鎿冨幒閹便劑寮ㄨぐ鎺撹偁闁告艾鐗炵槐?
		List<LivingEntity> burstTargets = new ArrayList<>();

		for (Entity entity : allEntities) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (!living.isAlive()) continue;
			if (living.getUUID().equals(playerUuid) || living.getUUID().equals(excludeUuid)) continue;
			if (WhitelistUtils.isProtected(player, living)) continue;
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(player, living)) continue;

			double distSq = living.distanceToSqr(center);

			// 濞寸鍊曢濠囨嚑閸愩劍绾俊顐熷亾闁哄被鍎荤槐?闁哄秶銆嬬槐?
			if (distSq <= spreadRange * spreadRange) {
				Vec3 oldVelocity = living.getDeltaMovement();
				living.invulnerableTime = 0;
				boolean damaged = living.hurt(splashDamageSource, damage);
				if (damaged) {
					living.setDeltaMovement(oldVelocity);
					hitCount++;
				}
				ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
						living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
						8, 0.3, 0.3, 0.3, 0.05);
			}

			// 闁告瑧濮甸悥锝囨媼閹峰苯鐦遍柛銉ョ摠椤ュ懘寮婚妷顖滅4闁哄秶鍘у畷鎰嚗閸曞墎绀?
			if (distSq <= splashBrandRange * splashBrandRange) {
				UUID livingUuid = living.getUUID();
				BrandState state = playerBrands.get(livingUuid);

				// 缂備浇鍎绘竟濠囨偐閼哥鍋撴担鐑樼暠闁烩晩鍠楅悥锝夋晬濮橆収姊鹃柡灞诲劚瑜版梻浠﹂崒姘瀻闁告鐡ㄥΣ鎼佸触閿斿墽娉㈤柡?
				if (state != null && state.greenState) {
					if (currentTick < state.stackCooldownExpiryTick) {
						continue;
					}
					// 闁告劕鍢插畵鍫㈢磼閹惧瓨灏嗛柨娑樿嫰瑜板洤鈽夐崼銏ｉ浌闁肩灏欐慨鎼佸箑娓氬﹦绀夊ù?鐎殿喒鍋撳┑顔碱儏瑜版梻浠?
					state.greenState = false;
					state.stacks = 0;
				}

				if (state == null) {
					state = new BrandState();
					playerBrands.put(livingUuid, state);
				}

				state.stacks = Math.min(state.stacks + 1, BAL.i("max_stacks", MAX_STACKS));
				state.lastStackTick = currentTick;
				state.expiryTick = currentTick + BAL.i("brand_duration", BRAND_DURATION);

				// 婵?閻忕偛鍊借闁告瑦鍨归崹搴ㄥ矗?
				if (state.stacks >= BAL.i("max_stacks", MAX_STACKS)) {
					state.stacks = 0;
					state.greenState = true;
					int greenDuration = BAL.i("green_duration", GREEN_DURATION);
					state.greenExpiryTick = currentTick + greenDuration;
					state.stackCooldownExpiryTick = currentTick + BAL.i("brand_stack_cooldown", BRAND_STACK_COOLDOWN);
					state.expiryTick = currentTick + greenDuration;
					updateTargetMarker(living, state, currentTick, player);
					burstTargets.add(living);
				} else {
					updateTargetMarker(living, state, currentTick, player);
				}
			}
		}

		// 鐎点倖鍎肩换婊勫緞閸曨厽鍊炴繝?閻忕偛鍊诲▓鎴︽偉閸℃绲洪柨娑樼墦娴尖晠宕楀澶夋喚闁告ê妫旈懙鎴炵┍椤旇姤鏆柣妯垮煐閳ь兛绀侀崯璺ㄧ玻娓氬﹦绀?
		for (LivingEntity burstTarget : burstTargets) {
			triggerPassiveBurst(player, burstTarget, serverWorld);
			serverWorld.playSound(null, burstTarget.getX(), burstTarget.getY(), burstTarget.getZ(),
					SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.38f, 1.2f);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME,
					burstTarget.getX(), burstTarget.getY() + burstTarget.getBbHeight() * 0.5, burstTarget.getZ(),
					30, 0.5, 0.5, 0.5, 0.1);
			ParticleUtils.spawnParticles(serverWorld, ParticleTypes.FLASH,
					burstTarget.getX(), burstTarget.getY() + burstTarget.getBbHeight() * 0.5, burstTarget.getZ(),
					1, 0, 0, 0, 0);
		}

		markDirty(playerUuid);

		if (hitCount > 0) {
			serverWorld.playSound(null, center.x, center.y, center.z,
					SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.6f, 0.6f);
		}
	}

	/**
	 * 闁兼儳鍢茶ぐ鍥箰閸パ呮毎闁绘壕鏅涢宥団偓浣冾潐鐎垫氨鈧姘ㄥú浼村冀閸モ晜鐣遍柣鎴炵懃瀹撳啯锛愬鍡楊棌
	 * @return "yellow"/"orange"/"red"/"green"/null
	 */
	public static String getBrandColor(UUID playerUuid, UUID targetUuid) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(playerUuid);
		if (playerBrands == null) return null;
		return getColorFromState(playerBrands.get(targetUuid));
	}

	/**
	 * 婵☆偀鍋撻柡灞诲劜鐎垫氨鈧姘ㄧ敮铏光偓纭呮硾椤曨噣骞愰崶褏鏆伴柣鈺婂枟閻栵綁寮伴姘剨闁哄牆顦伴悡鍥紣濠婂棗顥忛柣銊ュ閸庛儵宕?
	 * 闁圭粯鍔掔欢鐢电磼?BiEntity 闁哄鈧弶顐藉ù锝堟硶閺?
	 */
	public static boolean hasColor(UUID playerUuid, UUID targetUuid, String color) {
		String currentColor = getBrandColor(playerUuid, targetUuid);
		return color.equals(currentColor);
	}

	/**
	 * 婵絽绱╥ck闁哄洤鐡ㄩ弻?- 婵炴挸鎳愰幃濠冩交閸ャ劍鍩傞柣鎴炵懃瀹?
	 */
	public static void tick(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		if (playerBrands == null) return;

		// 鐟滆埇鍨洪埀顑跨劍椤ュ懘寮?
		if (!FormUtils.isGoldenSandstormSP(player)) {
			// 闁肩浜‖鍥亹閵忊檧鍋撴笟濠勭婵炴挸鎳愰幃濠囧箥閳ь剟寮垫径灞剧獥闁哄秴娲ㄥ▓鎴﹀冀閸ヮ亶鍞堕柡浣哥墛閻?
			if (player.level() instanceof ServerLevel serverWorld) {
				for (UUID targetUuid : playerBrands.keySet()) {
					LivingEntity target = findEntity(serverWorld, targetUuid);
					if (target != null) refreshOtherMarkers(target, player.getUUID());
				}
			}
			ACTIVE_BRANDS.remove(player.getUUID());
			DIRTY_PLAYERS.remove(player.getUUID());
			// 闁告瑦鍨块埀顑胯兌閳规牠宕ョ仦缁㈠妱闁告牕鎳庨崺宀€鈧箍鍨洪崺娑氱博椤栥倗绀夋繛鎾虫嚇濞呭酣宕ｉ幋婵嗗辅缂傚倹鎸搁悺銊╂晬閸儲些婵縿鍨归崹蹇涘炊閻愯尪鍩岄柟顑跨劍濡炲倸鈻撶€ｎ剚娈岄柡鍐勫嫭娈堕柟璇″櫙缁?
			try {
				FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
				buf.writeInt(0);
				github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
			} catch (Exception ignored) {
				// 闁告瑦鍨块埀顑跨閵囨垹鎷归妷锔筋槯闊洨鏅弳?
			}
			return;
		}

		long currentTick = player.serverLevel().getGameTime();
		ServerLevel serverWorld = player.serverLevel();
		boolean[] hadExpiry = {false};
		playerBrands.entrySet().removeIf(entry -> {
			BrandState state = entry.getValue();
			boolean expired = state.greenState && currentTick >= state.greenExpiryTick; // 缂備浇鍎绘竟濠囨偐閼哥鍋撴担鐣岀畺闁哄牏鍠愰ˉ鍛村蓟?
            // 闁哄拋鍣ｉ埀顒佹皑閸庛儵宕￠幏宀€绠栭柡鍫㈠枑椤ュ懘寮?
			if (!state.greenState && currentTick >= state.expiryTick) {
				expired = true;
			}
			if (expired) {
				hadExpiry[0] = true;
				// 缂佸顭峰▍搴ㄦ儎椤旂晫鍨奸柣銊ュ閻栵絿鎷嬮悧鍫熸珡闁?
				LivingEntity target = findEntity(serverWorld, entry.getKey());
				if (target != null) refreshOtherMarkers(target, player.getUUID());
			}
			return expired;
		});
		if (hadExpiry[0]) {
			markDirty(player.getUUID());
		}

		// 濠碘€冲€归悘澶屾嫚閵壯冭礋閻庣鍩栭惀鍛村嫉婢跺宕插ù锝嗘礃濡炶法鎹勯崘顏勫妼闁告顣︾花锟犳晬鐏炲墽顏搁梻鍕╁€栭弳锝嗙▔閻氱櫘p
		if (playerBrands.isEmpty()) {
			ACTIVE_BRANDS.remove(player.getUUID());
		}

		// 闁告艾鏈鐐烘倻濞嗗繐绁柣妯垮煐閳ь兛绀侀崺宀€鈧箍鍨洪崺娑氱博椤栥倗绀勫ù鐘叉噹濠€顏堟偉鐠哄搫缍侀柡鍐硾瑜板倿鏌呮笟濠勭
		syncIfDirty(player);
	}

	/**
	 * 闁绘壕鏅涢宥夊棘椤撶姴娈?闁告瑦锚閼镐即寮懜鐢殿伕闁荤偛妫寸槐婵嬪触鐏炵偓顦х紒澶婎煼濞呭酣鎯勯鐣屽灱濞戞挸锕﹀▓鎴﹀冀閸ヮ亶鍞堕柡浣哥墛閻忓鐛捄鍝勭岛闂侇偂鑳堕埞鏍触鐏炵虎鍔勯柛?
	 */
	public static void clearPlayer(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.remove(player.getUUID());
		if (playerBrands != null && player.level() instanceof ServerLevel serverWorld) {
			for (UUID targetUuid : playerBrands.keySet()) {
				LivingEntity target = findEntity(serverWorld, targetUuid);
				if (target != null) refreshOtherMarkers(target, player.getUUID());
			}
		}
		DIRTY_PLAYERS.remove(player.getUUID());
		// 闁告瑦鍨块埀顑胯兌閳规牠宕ョ仦缁㈠妱闁告牕鎳庨崺宀€鈧箍鍨洪崺娑氱博?
		try {
			FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
			buf.writeInt(0);
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
		} catch (Exception ignored) {
			// 闁绘壕鏅涢宥夊矗椤栨繂鍘寸€圭寮堕弻鍥╃棯閸栵紕绀夐煫鍥╂櫕閺嗘劙宕ｉ幋锔瑰亾娴ｆ悶浜奸悹?
		}
	}

	/**
	 * 闁哄牆绉存慨鐔煎闯閵娾晛娅㈤柛?闁绘埈鍙冮崳鍛婃姜閼恒儲顦ф繛鎾虫噽閹﹪骞嶉埀顒勫嫉婢跺苯笑闁?
	 */
	public static void clearAll() {
		ACTIVE_BRANDS.clear();
		DIRTY_PLAYERS.clear();
	}

	/** 闂佹彃绉风换?闁瑰箍鍨诲ǎ顔芥償閿曗偓閹顕ｉ崫鍕厬闁硅泛锕ょ紞瀣礈瀹ュ洤鍔岄柛妤佸婵悂骞€娴ｅ憡鍊辨慨婵勫劤缁壆鎷犻妷褍璐熼悗纭咁啇缁辨瑩寮悩渚綊 dirty 闁哄秴娲╅鍥晬?*/
	public static void resyncToPlayer(ServerPlayer player) {
		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		if (playerBrands == null || playerBrands.isEmpty()) {
			buf.writeInt(0);
		} else {
			List<Map.Entry<UUID, String>> entries = new ArrayList<>();
			for (Map.Entry<UUID, BrandState> entry : playerBrands.entrySet()) {
				String color = getColorFromState(entry.getValue());
				if (color != null) entries.add(Map.entry(entry.getKey(), color));
			}
			buf.writeInt(entries.size());
			for (Map.Entry<UUID, String> entry : entries) {
				buf.writeUUID(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
		}
		try {
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
		} catch (Exception ignored) {}
		DIRTY_PLAYERS.remove(player.getUUID());
	}

	// ==================== 缂傚啯鍨圭划鍫曞触鐏炵虎鍔勯柡鍌濐潐绾?====================

	/**
	 * 闁哄秴娲╅鍥偝閳轰緡鍟€闁汇劌瀚崕銉╁础閻楀牊娈堕柟璇″櫍濞撳墎鎲版担鍛婂€辨慨婵勫劚閸╁瞼鈧箍鍨洪崺娑氱博?
	 */
	private static void markDirty(UUID playerUuid) {
		DIRTY_PLAYERS.add(playerUuid);
	}

	/**
	 * 濠碘€冲€归悘澶愭偝閳轰緡鍟€闁汇劌瀚崕銉╁础閻楀牊娈堕柟璇″枛閸戯繝宕ｅΟ鍝勵嚙闁挎稑鑻幃鎾愁潰閵夈儱鐓傞柛蹇氭硾椤撳綊骞嬫搴紓
	 * 闁?tick 缂備焦鎸诲顐﹀籍閹壆娈堕柣銏╃厜缁辨繈鏌嗛崹顔煎赋婵絽绻戦濂稿矗濡搫顕ч梺顔挎瑜板倿宕?
	 */
	private static void syncIfDirty(ServerPlayer player) {
		if (!DIRTY_PLAYERS.remove(player.getUUID())) return;

		Map<UUID, BrandState> playerBrands = ACTIVE_BRANDS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();

		if (playerBrands == null || playerBrands.isEmpty()) {
			buf.writeInt(0);
		} else {
			// 闁衡偓閸洘鑲犻柡鍫濐槹閺呫儲锛愬鍡楊棌闁哄绱曞ú?
			List<Map.Entry<UUID, String>> entries = new ArrayList<>();
			for (Map.Entry<UUID, BrandState> entry : playerBrands.entrySet()) {
				String color = getColorFromState(entry.getValue());
				if (color != null) {
					entries.add(Map.entry(entry.getKey(), color));
				}
			}
			buf.writeInt(entries.size());
			for (Map.Entry<UUID, String> entry : entries) {
				buf.writeUUID(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
		}

		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_BRAND_SYNC, buf);
	}

	/**
	 * 濞?BrandState 闁兼儳鍢茶ぐ鍥紣濠婂棗顥忛悗娑欘殘椤戜焦绋?
	 */
	private static String getColorFromState(BrandState state) {
		if (state == null) return null;
		if (state.greenState) return "green";
		return switch (state.stacks) {
			case 1 -> "yellow";
			case 2 -> "orange";
			case 3 -> "red";
			default -> null;
		};
	}

	/**
	 * 闁告艾鏈鐐烘倻濞嗗繐绁柣妯垮煐閳ь兛绀侀崺宀€鈧箍鍨洪崺娑氱博?- 闂侇偅淇虹换鍐偐閼哥鍋撴担瑙勬珡闁哄绮岄悿鍕偝?
	 * 闁瑰嘲妫楅崹搴㈢▔?濞戞搩浜炵€氼厾绮╃€ｎ偅娅忛柡瀣矆娴滄帡寮ㄩ娑樼槷濞戞挸绉撮幃鎾朵沪閸屾稒娈堕柡鍕⒔閵囨碍绋夊鍛€遍柛銉у亾閻栵綁鏁?
	 * erosion_brand_marker_1: 1閻?濮掓稑瀚竟?, erosion_brand_marker_2: 2閻?婵鐟ㄦ竟?, erosion_brand_marker_3: 3閻?缂佷勘鍨兼竟?
	 * 缂備浇鍎绘竟濠囧礃瀹勬澘绁遍柣妯垮煐閳ь兛妞掔粭澶愬及閸撗佷粵闁搞儳鍋撻悥锝夊Υ?
	 */
	/** 闁告艾鏈鐐哄冀閸ヮ亶鍞堕柛鎺撳濞蹭即寮介崶椋庣owner 濞达絾绮堢拹鐔煎极閸喓浜?source闁挎稑濂旂欢鍨槹閻旂硶鍙哄鍕煟閳ь剙鑻崣鍡楊潖閿旇　鍋撳畡婕瀊uff/濡ゅ倹锚閸樻粓骞忛敂钘夌劵鐟滅増甯掑ú婊堟晬婢跺牃鍋?*/
	private static void updateTargetMarker(LivingEntity target, BrandState state, long currentTick,
	                                       net.minecraft.server.level.ServerPlayer owner) {
		if (state == null || (state.stacks <= 0 && !state.greenState)) {
			removeAllBrandMarkers(target);
			return;
		}
		int duration;
		if (state.greenState) {
			// 缂備浇鍎绘竟濠囧礃瀹勬澘绁遍柣妯垮煐閳ь兛绶ょ槐鐗堢▔瀹ュ棙鈻旂紒鈧崫鍕闁哄秴娴勭槐婵堢矓婵犳碍鐝熼柟纰樺亾闁哄牆顦伴悥锝囨媼?
			removeAllBrandMarkers(target);
			return;
		} else {
			duration = (int) (state.expiryTick - currentTick);
		}
		if (duration <= 0) {
			removeAllBrandMarkers(target);
			return;
		}
		// 闁稿繐鐗忎簺闂傚嫨鍊栨晶宥夊嫉婢跺锛嬮柡宥呮穿椤斿洭鏁嶇仦钘夋櫃闁哄倽妫勬慨鐐躲亹閹惧啿顤呴悘鐐插€归弳鐔衡偓鐢垫嚀缁ㄦ煡鎯冮崟顒傚灱閻?
		removeAllBrandMarkers(target);
		net.minecraft.world.effect.MobEffect markerEffect = switch (state.stacks) {
			case 1 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_1.get();
			case 2 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_2.get();
			case 3 -> github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_3.get();
			default -> null;
		};
		if (markerEffect != null) {
			// ambient=false, showParticles=false, showIcon=true闁挎稑鐗婂Ο澶岀矆閸濆嫷鍤犻幖瀛樻煥閻即寮弶鎸庣闁哄秴娴勭槐?
			target.addEffect(new MobEffectInstance(
					markerEffect, duration, 0, false, false, true
			), owner);
		}
	}

	/**
	 * 缂佸顭峰▍搴ㄦ儎椤旂晫鍨煎☉鎾筹功濞堟垿骞嶉埀顒勫嫉婢跺苯鍔岄柛妤冨閻栵絿鎷嬮悧鍫熸珡闁?
	 */
	private static void removeAllBrandMarkers(LivingEntity target) {
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_1.get());
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_2.get());
		target.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.EROSION_BRAND_MARKER_3.get());
	}

	/**
	 * 闁革负鍔嬬粭姗€鎮剧仦鑹板幀闁哄被鍎叉竟姗€鎯勯鐣屽灱閻庡湱鍋樼紞?
	 */
	private static LivingEntity findEntity(ServerLevel world, UUID uuid) {
		var entity = world.getEntity(uuid);
		return entity instanceof LivingEntity living ? living : null;
	}

	// ==================== 闁告劕鎳橀崕鎾极閻楀牆绁︾紒?====================
	private static class BrandState {
		int stacks = 0;
		boolean greenState = false;
		long lastStackTick = -100; // 濞戞挸锕ラ濂稿矗閻樿尙婀撮柡鍐ㄧ埣濡?
		long expiryTick = 0; // 闁绘垶鐟ュ畵鍐╂交閸ャ劍鍩傞柡鍐ㄧ埣濡?
		long greenExpiryTick = 0; // 缂備浇鍎绘竟濠囨偐閼哥鍋撴担鐣岀畺闁哄牏鍠愬鍌炴⒒鏉堝墽绀勯悷娆忔椤酣寮介崶顏嶅敹闁?0缂佸甯槐?
		long stackCooldownExpiryTick = 0; // 闁告瑧濮撮惇浼村礃瀹勬澘绁遍弶鈺佹处濠€锟犲籍閸洘锛熼柨?缂佸甯槐?
	}
}
