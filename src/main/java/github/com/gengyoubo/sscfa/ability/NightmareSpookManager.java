package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.GhostCatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 濡炲鍠愰埅鍨洨閺冩挾绀凬ightmare闁挎稑顦伴鑲╂啺娴ｇ懓螚闁煎啿顫曢埀顒€鏈崕鏇㈠触閹炬墎鍋撳蹇曠Spook闁挎稑顦埀顒佹煛閳?闁哄牆绉存慨鐔虹博椤栨稒缍€濠电偘闄嶉埀?
 *
 * <p>闁圭顦甸弫顓㈡晬閸х殐_secondary闁挎稑鐡擠 8s闁挎稑顦顕€骞嶉埀顒勫嫉婢跺﹤鍤掗柛蹇嬪劜閳敻鎯勯鐣屽灱闁汇垻鍠愰弲銉╂晬?/p>
 * <ul>
 *   <li><b>妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€?/b>闁挎稒姘ㄥú浼村冀閸ヮ亪鐓╅柛姘捣閳规牕顫濋弬鍓хТ闁汇垻鍠愰崹姘舵儑閻旈鏉?{@link Creeper}闁挎稑婀AI/闁哄啰濮甸弲?閻庣敻鈧稓閾傚ù?
 *       闂傚懏鍔橀棅鈺呮晬婢舵稓绀夊ù鐘叉噽濞蹭即寮介崶褜鍚傞柟鎾棑椤忣剟宕ｉ婵愭綄闁挎稑娼癷xin 閻忕偐鍋撻梺顔哄妽濡銇愰～顔剧闁挎稒绋戠槐鈺傜┍閿熺姷鍙鹃柡浣哥墔缁楀矂宕氶悧鍫熷焸闁绘牕妫涢崑銏＄珶?缂侇喗甯掗崣蹇涙焾?
 *       <b>S2C 闁烩晜娼欒ぐ鍌炴儎椤旂晫鍨?/b>闁挎稑鐗呯划顒佺濞差亝绁╅柟鎵枔閻擄繝鏁嶆径娑氬耿3 缂佸甯掗幃妤呭籍閻樿尙鏉藉ù绗哄€楅崹搴ㄦ⒔閵堝啠鍋?/li>
 *   <li><b>妤犵偟鏅导鎺楁煂鎼达絽鐨?/b>闁挎稒姘ㄥú浼村冀閸ャ劍鏆伴柛鎴炴鐎氥倝宕濆☉娆屽亾閺囩偞鍊甸柨娑樺缁娀鎳熼敃鈧慨蹇涘箑閺囩喓啸闁轰緤绲介ˇ鈺呮偨閻旂鐏囬柣顏嗗枎閻?
 *       {@link GhostCatEntity}闁挎稑鐗撻崳褰掓偖椤愩垼鍩岄柟?geo 婵☆垪鈧磭鈧兘鏁嶇€涒暙AI/闁哄啰濮甸弲?閻庣敻鈧稓閾傚ù婊勬そ濞堬綁鐓顒傜濞寸姴鎳愬ú浼村冀閸パ冭閻熸瑤绶ょ槐姘舵晬鐏炶棄甯?
 *       <b>闁绘凹鍋勯幖鍗烆潩?/b>闁挎稑鐗嗛悾楣冨触閹达妇鍙鹃柡浣哥墳缁辨岸鏁嶇仦鐐瘋闁烩晩鍠楅悥锝嗙附閺冨洨鐛撻柨娑樼墛濠€鍥礉閿涘嫷浼傞柟闈涱儏婵晜銇欓崡鐐残楀ù锝呯Ф浜涢柨娑橆檧缁辨繄鎹勫┑鍫熺獥闁?4 闁?b>閻犙呮焿閻戯箓骞嶉幋锝呭П</b>闁?
 *       閻犳劗顥愰棅鈺呭箰閵壯冪劵闁?2 闁绘劕缍婇悺鐔封枖閺囨碍绺洪悗鍦劜缁辨岸鏁嶅畝鍕吂闁告艾娴烽崕顐︽⒖閻愵剛啸闁?+ 濠㈣埖鎸抽妴濠冪閸涱垱绐楅柡宥呮搐瑜拌尙鎲存担鐑樼暠闁绘垹鍠曟慨鎶藉Υ?/li>
 * </ul>
 *
 * <p>閻庤鑹鹃幃婊勭珶閹殿喗鏆伴柛妯煎枎閸垶鏁嶅顓烆暡闁哄牆顦敍鎰版?缂侇喗甯掗悺娆撳矗椤忓棙鏆?{@code PlaySoundS2CPacket}/{@code ParticleS2CPacket}
 * 闁烩晜娼欒ぐ鍌滅磼濞嗘垶绐楅柡宥呮处濠€鐗堢閻氬绀勯柡鍫濈Т婵喓绮╅娑欑秬濠?+ 闁告瑯浜濆﹢渚€鎯勯鐣屽灱闁告凹鍓欑欢閬嶅礆?闁活亜顑呯欢閬嶅礆?+ 閻㈩垽濡囬埞鏍⒒鐎涙ɑ鐓欏ù锝呯▌缁辨岸鏁嶅畝鈧划閿嬬▔?world.playSound
 * 妤犵偞瀵ч幐閬嶆晬閸儱浜濆ù鍏肩婵℃悂鐛悾灞肩触婵炲瀚槐锛勭磼濞嗗繑鍣柛銉ョ摠婢у秹寮垫径澶嬬溄闁挎稑顦埀?/p>
 */
public final class NightmareSpookManager {

	// ==== 濞寸姰鍎扮粭鍛暜閹间礁娅ゅ☉鎾存そ缁垳鎷嬮妶鍛亾绾骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柨娑樻緝cope: abilities.nightmare_spook闁挎稑鏈弳鐔煎箲椤旂厧鐦堕柛娆樺灥椤╊偊鎯勯弽鐢电====
	/** 闁瑰灈鍋撻柤?CD闁挎稑婢卛ck闁? 缂佸甯槐姘跺Υ閸岀偟甯涢悹浣靛€х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final int SPOOK_COOLDOWN_TICKS = 160;
	/** 妤犵偟鏅导鎺楁煂鎼达絽鐨搁柡鈧拠鎻掓瘖濞寸鍊曢濠囨晬閸儳鎽曟繛澶嬫穿缁辨岸濡撮崒鐐靛笡閻犱降鍊х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final float CLONE_DAMAGE = 12.0f;
	/** 妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€閺囩偟鎽犳繛韫串缁辨獩ick闁? 缂佸甯槐姘跺Υ閸岀偟甯涢悹浣靛€х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final int CREEPER_LIFE_TICKS = 60;
	/** 妤犵偟鏅导鎺楁煂鎼达絽鐨搁悗娑櫳戝鎸庣▔婵犲洦顎欓柨娑樻緟ick闁挎稑鑻崢瑙勬償閺囶亞骞㈡慨婵撶到閻?~1.5-2.5 缂佸甯掗悾顒勫箣閹邦厼鈪抽柛婊庡墾缁辨岸濡撮崒鐐靛笡閻犱降鍊х槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final int CAT_LIFE_TICKS = 90;
	/** 闂佹彃娴风亸妤佺附閺冨洨鐛撴慨婵勫劦閳ь剛鍣︾槐娆撳冀?tick闁挎稑顦埀顒€鍊跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final double CAT_RUN_SPEED = 0.32;
	/** 闂佹彃娴风亸妤冩崉濠靛牊绐楅柡宥呮搐椤︽寧娼诲鍡樺闯閻犺櫣顒茬槐娆撳冀绾绀嗛柕鍡楀€跨划顖滄媼閵堝繒骞㈤弶鈺傚姌椤㈡垿寮張鐢电煠 balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柕?*/
	public static final double CAT_LEAP_DISTANCE = 4.0;

	/** 闁哄牆绉存慨鐔虹博椤栨稒缍€濠?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柨娑樼墕閹烩晠鎮¤濠€顓㈠礆濠靛棭娼楅柛鏍ㄧ墬濡炲倿宕堕悙琛″亾閳ь剚绋夋繝鍐╃厵濮掓稒顭堥鑽ゆ暜閹间礁娅ら柨娑橆槶閳?*/
	private static final BalanceReader BAL = new BalanceReader("abilities.nightmare_spook");

	/** 婵炲弶妲掔粚顒勭嵁閻ｅ奔绱ラ柤浼欑畱婵繘骞€閺囶亞绐楅悗鍦仒缂?UUID -> 闁烩晩鍠楅悥锝夋偝閳轰緡鍟€ UUID闁?*/
	private record CreeperState(UUID target, UUID caster) {}
	private static final Map<UUID, CreeperState> CREEPERS = new ConcurrentHashMap<>();
	/** 婵炲弶妲掔粚顒勭嵁閻ｅ奔绱ラ梺鎻掓捣鐏忔鏁嶅顒傛澖濞?UUID -> CatState闁?*/
	private static final Map<UUID, CatState> CATS = new ConcurrentHashMap<>();

	/** 妤犵偟鏅导鎺楁煂鎼达絽鐨搁弶鈺傚姌椤㈡垿骞€娴ｇ鍋?*/
	private static final class CatState {
		final ServerPlayer target;
		final UUID casterUuid;
		final long bornTick;
		/** 闂傚啳鍩栭宀勬晬?=閺夆晛鈧喓鐛?1=閻犲搫鐤囩粚顒佺▔?2=鐎圭寮剁亸宀勬偉?鐎垫澘鎳忕粔鐑藉极?闁?*/
		int phase;
		/** 閻犲搫鐤囩粚顒傛導妞嬪骸浠柨娑樼墛瑜板啴宕愰崗鐓庮潝闁绘せ鏅濋崵搴ㄦ偨椤帞绀嗛柕?*/
		double jumpFromX, jumpFromY, jumpFromZ;
		/** 閻犲搫鐤囩粚顒€顔忛懠鍓佺閻?tick闁?*/
		int jumpTicks;

		CatState(ServerPlayer target, UUID casterUuid, long bornTick) {
			this.target = target;
			this.casterUuid = casterUuid;
			this.bornTick = bornTick;
		}
	}

	private NightmareSpookManager() {
	}

	/** 婵炲棌鍓濇俊褔鎳楅挊澶婂汲闁告瑱缍囩槐妾杘wer action 閻犲鍟伴弫銈夋晬鐏炶偐鐭岄柡鍫濈Т婵喓绮╅銈囩闁靛棗鍊瑰Λ銈夊礂閵夛腹鍙洪柣鈺婂枟閻栵絾娼婚弬鎸庣 false闁挎稑鐗呯粭澶嬫交?CD闁挎稑顦埀?*/
	public static boolean execute(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel world)) return false;
		// CD 闂傚倶鍔庨々锕€顔忛懠顒佹殸 power 閻?begin 缂備胶鍠嶇粩鎾箯閿旇棄鐒婚柨娑樼墔閹?P1-1闁?

		long now = world.getGameTime();
		List<LivingEntity> targets = NightmareDreamManager.collectDreamTargets(player, now);
		if (targets.isEmpty()) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}

		boolean any = false;
		for (LivingEntity target : targets) {
			if (target instanceof ServerPlayer sp) {
				// 闁绘壕鏅涢宥夋儎椤旂晫鍨奸柨娑欒壘閼垮洭鎮樻担鍐仭闁告梹绋掗埀顒佹礀婢剁喓鎸掗垾鍐插伎濠靛倹顨愮槐娆忣嚕閺囨矮绻?闁?闁搞儳鍋涢崵?闁?妤犵偟鏅导鎺楁煂鎼达絽鐨搁柟鍨灱閸旑垶鏁?
				if (spawnGhostCreeper(world, player, sp)) any = true;
			} else {
				// 闂傚牏鍋熺敮铏光偓鐟板濞蹭即寮介崶椋庣闁活潿鍔嶉崺娑氣偓瑙勬皑椤牓鏁嶆径娑氱獥闁哄啰濮存径鐔烘寬閳藉懐绀夐柣鈺佺摠鐢鎮銏ｅ即闁诲繒鏁婚崳褰掓偖椤愶絾瀵楅柛婊庡墾缁?2 闁绘劕缍婇悺鐔封枖閺囨碍绺洪悗鍦劜缁辨岸鏁?
				// 闁稿繈鍎查埅鐢稿籍閸洘锛熼梺鎻掔Ф閻ゅ棙绋?20 缂佸甯楀褎锛愬┑鎾剁濞戞挸绉村閬嶅礆閸洍鍋撻埀顒勫礄閻氬绀勯柡鍐У娴滃綊骞嗚娴犲牓宕濋妸褎鐣辩€殿喖鎼崺妤呮嚐韫囨稑鏅柨娑橆槶閳?
				if (directMobBite(world, player, target, now)) any = true;
			}
		}
		if (!any) {
			github.com.gengyoubo.sscfa.power.FailAwareActiveSelfPower.markFail();
			return false;
		}
		return true;
	}

	/** 闂傚牏鍋熺敮铏光偓鐟板濞蹭即寮介崶椋庣獥闁哄啰濮撮懜浼存儍閸曨偉寮撮柣蹇曟暬閸ｅ綊鎮銈嗙函闁规亽鍎查幐鍕椽椤掑﹦绀勯悗瑙勮壘閹粍绔熼悧鍫熸珡 + 濞寸鍊曢?+ 闁稿繈鍎查埅鐢告煂瀹ュ洨鏋?20s闁挎稑顦埀?*/
	private static boolean directMobBite(ServerLevel world, ServerPlayer caster, LivingEntity target, long now) {
		if (!target.isAlive() || target.level() != world) return false;
		// 闁谎嗘閹洟宕￠弴姘辩闁硅翰鍊х槐浼存儌閽樺鍊抽柛妤佹礀閸炴挳鎮介悢鍝勨挅濞戞挸绉磋ぐ鍫ュ箚婵犲倹鍋傚ù绗哄€曢?
		if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(caster, target)) return false;
		// 妤犵偟鏅导鎺楁煂鎼达絽鐨搁悹鎰棎闂娾晜绋夐埀顒勫礄娴兼瑧绐?2 闁绘劕缍婇悺鐔封枖閺囨碍绺洪悗鍦劜缁辨瑩宕?completeCatAttack 闁汇劌瀚┑鈧悗纭呮珪缁噣鏁嶇仦钘夎鐟滅増甯掑ú婊勵槹閻旂硶鍙哄鍕缁?
		target.hurt(caster.damageSources().indirectMagic(caster, caster), (float) BAL.d("clone_damage", CLONE_DAMAGE));
		// 闁稿繈鍎查埅鐢稿籍閸洘锛熼梺鎻掔Ф閻ゅ棙绋?20 缂佸甯楀褎锛愬┑鎾剁濞戞挸绉村閬嶅礆閸洍鍋撻埀顒勫礄閻氬骞㈠☉鎾冲娴滃綊骞嗚濞堟垿鏌屽鍥╂瀭闁告艾鐭傞·鍌炴晬?
		NightmareDreamManager.resetDream(caster.getUUID(), target.getUUID(), now);
		// 濠㈠湱澧楅弲銉︾▔鎼达絿鐓堥悗娑欏姧缁辨瑩鎮介悢鍝勨挅闁哄啰濮撮褰掑箣妞嬪寒浼傞悘鐐茬箰缁犵兘鏁嶅顒€寮块柛娑櫭ぐ鏌ユ⒒閼姐倖鐣卞ù锝呭閻洭鎮銏℃ + 闁烩晩鍠楅悥锝嗘媴瀹ュ洨鏋傞柣鎴犲枛濞存﹢鏁?
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.CAT_HISS, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.85f);
		world.sendParticles(ParticleTypes.CLOUD,
				target.getX(), target.getY(0.5), target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
		return true;
	}

	// ==================== 閻庤鑹鹃幃婊勭珶閹殿喗鏆伴柨娑樼墔缁酣鎯勯鐣屽灱闁哄牜鍏涘Ч澶愭晬?====================

	/** 閻庤鑹鹃幃婊堟閾忣偅娅忛柨娑欑煯缁酣鎯勯鐣屽灱闁告凹鍓欓崺宀勬晬閸?C 闁烩晜娼欒ぐ鍌炴晬鐏炵晫鏁ㄧ紒灞炬そ濡潡寮柅娑氱Т閻炴稒婢橀崳娲晬婢跺牃鍋?*/
	private static void playSoundAt(ServerPlayer target, SoundEvent sound, double x, double y, double z,
	                                float volume, float pitch) {
		Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		target.connection.send(new ClientboundSoundPacket(
				entry, SoundSource.HOSTILE, x, y, z, volume, pitch, target.getRandom().nextLong()));
	}

	/** 閻庤鑹鹃幃婊呭垝閹烘垹鎽嶉柨娑欑煯缁酣鎯勯鐣屽灱闁活亜顑呴崺宀勬晬閸?C 闁烩晜娼欒ぐ鍌炴晬婢跺牃鍋?*/
	private static void spawnParticlesAt(ServerPlayer target, ParticleOptions particle,
	                                     double x, double y, double z, int count, double ox, double oy, double oz) {
		target.connection.send(new ClientboundLevelParticlesPacket(particle, true, x, y, z,
				(float) ox, (float) oy, (float) oz, 0.0f, count));
	}

	// ==================== 妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€?====================

	/** 闁革负鍔庡ú浼村冀閸ヮ亪鐓╅柛?180閹?闂傚懎绻戝┃鈧柡鍌滄嚀閹?2~3 闁哄秷鍋愬▓鎴犵矚閻戞姣滃ù锝呯Ф閻ゅ棝鎮介悢绋跨亣妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€閺囶亞绀勯柣顏嗗枎閻ゅ嫭鎷呴幙鍕濞寸姴鎳愬ú浼村冀閸パ冭閻熸瑤绶ょ槐姘跺Υ?*/
	private static boolean spawnGhostCreeper(ServerLevel world, ServerPlayer caster, LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) return false;
		Vec3 pos = findAirPosBehind(world, target, 2.0, 3.0, 16, 0.9, 1.5);
		if (pos == null) return false;

		Creeper creeper = EntityType.CREEPER.create(world);
		creeper.moveTo(pos.x, pos.y, pos.z, target.getYHeadRot() + 180.0f, 0.0f);
		// 妤犵偟鏅导鎺楀礌閺嶃劍娈堕柟璇″櫙缁辨壆绮?AI闁挎稑鐗忛悵顖氼浖閳哄绀嗛柕鍡曠劍濡倝寮仦鐑╁亾娴ｅ壊鍤犲ù鐘崇墧濮瑰姊鹃幇顖炵叐闁靛棔鐒﹀Λ銈夋煂瀹ュ懎顫旈柕鍡曠劍鐎垫梹绋婇崨顓烆嚙
		creeper.setNoAi(true);
		creeper.setInvulnerable(true);
		creeper.setInvisible(true);
		creeper.setNoGravity(true);
		creeper.setPersistenceRequired();
		creeper.addTag("ssca_ghost_creeper");
        CREEPERS.put(creeper.getUUID(), new CreeperState(sp.getUUID(),caster.getUUID()));
        if (!world.addFreshEntity(creeper)) { CREEPERS.remove(creeper.getUUID()); return false; }
		// 妤犵偟鏅导鎺楀冀閸ヮ亶鍞堕柛鏍ф嫅缁辨瑦绂掗崨顖涚獥闁哄秴娴勭槐姘舵晬濮橆剦鍚傞柟鎾棑椤忣剛鈧數顢婇姘扁偓鍦仒缂嶅浠﹂埀顒勬焾閵娿儱绲挎繛?invisible
		SscAddonNetworking.sendSpookGhost(sp, creeper.getUUID(), BAL.i("creeper_life_ticks", CREEPER_LIFE_TICKS));
		// 鐎殿喗娲戞穱濠囧床鐠虹儤鈻夊鐟板簻缁辨瑧鈧鑹鹃幃婊堟晬鐏炶偐鐭岄柣鈺婂枟閻栵綁宕ラ鈧崺宀勬晬鐏炶偐鐭ゆ鐐垫櫕娴兼帡寮悷鐗堝€诲ù鑲╁У濞肩敻鏁?
		playSoundAt(sp, SoundEvents.CREEPER_PRIMED, pos.x, pos.y, pos.z, 1.0f, 1.0f);
		return true;
	}

	/** 闁烩晩鍠楅悥锝夊绩鐠囨彃姣婃鐐垫櫕娴兼帡鎳熼敃鈧慨蹇涘箑閺囶亞绀勯柡鍫濈Т婵喓绮?AttackEntityCallback闁挎稑顧€缁变即宕欑紒妯荤祷闁间紮绠戞慨蹇涘箑?+ 闁告瑯鍓欓崵顓㈢嵁閻ｅ奔绱ラ梺鎻掓捣鐏忔濡?*/
	public static void onGhostCreeperAttacked(ServerPlayer attacker, Creeper creeper) {
		CreeperState ghostState = CREEPERS.get(creeper.getUUID());
		if (ghostState == null) return;
		if (!attacker.getUUID().equals(ghostState.target())) return; // 闁告瑯浜濆﹢渚€鎯勯鐣屽灱闁哄牜鍏涘Ч澶愭儍閸曨剚鏆伴柛鎴犵帛濠€渚€寮?
		CREEPERS.remove(creeper.getUUID());
		ServerLevel world = (ServerLevel) attacker.level();
		// 闁告垹绮鍐閾忣偅娅?闁绘垹鍠栧ù姗€鏁嶉崼婵堟毎闁告碍鍩婄槐婵囩閸涱垱绐楅柡宥呮祫缁?
		playSoundAt(attacker, SoundEvents.CREEPER_DEATH, creeper.getX(), creeper.getY(), creeper.getZ(), 0.8f, 1.0f);
		spawnParticlesAt(attacker, ParticleTypes.CLOUD, creeper.getX(), creeper.getY(0.5), creeper.getZ(), 8, 0.3, 0.3, 0.3);
		Vec3 creeperPos = creeper.position();
		creeper.discard();
		spawnGhostCat(world, attacker, creeperPos,ghostState.caster());
	}

	// ==================== 妤犵偟鏅导鎺楁煂鎼达絽鐨?====================

	/** 濞寸姴姘︾€氥倝宕濆☉娆屽亾閺囩喓啸闁轰緤绲介ˇ鈺呮偨閻旂鐏囨鐐垫櫕娴兼帡鏌屾惔锝呯毟闁挎稑鐗撻崳褰掓偖椤愩垼鍩岄柟顑跨劍鑶╅柛銊ヮ儑濠€锛勨偓鍦仒缂嶅鏁嶇仦鑲╃煂闁烩晩鍠楅悥锝夊矗椤栨繍娼岄柨娑橆槴閸?濠靛倹妫佺粣?闁?閻犲搫鐤囬崝顖炲绩鐠囨彃姣婇柕?*/
	private static void spawnGhostCat(ServerLevel world, ServerPlayer target, Vec3 nearPos,UUID casterId) {
		// 闁汇垻鍠愰崹姘舵倷閻у摜绐楀ù鍏济崢娑㈡嚐閿曗偓婵繘骞€閺囩偛鏂уù锝呯Ф閻ゅ棝姊介崟顔剧闁挎稑鐗撻崳褰掓偖椤愮姭鍋撶仦鑲╃煠闁间紮绠戞慨蹇涘箑閺囩喓啸闁轰緤绲介ˇ鈺呭箥閹存繂姣夐柕鍡楃▌缁辨岸鏁嶇仦濮愪杭閻犳劑鍎甸埀顑藉亾闁烩晩鍠楅悥锝夌叕椤愩垺鍊?
		Vec3 pos = findAirPosNear(world, nearPos, 1.5, 8, 0.9, 1.0);
		if (pos == null) pos = findAirPosBehind(world, target, 2.0, 3.0, 12, 0.9, 1.0);
		if (pos == null) return;

		GhostCatEntity cat = github.com.gengyoubo.sscfa.init.AddonEntities.GHOST_CAT.get().create(world);
		cat.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
		// 妤犵偟鏅导鎺楀礌閺嶇數绐桸oAI + 闁哄啰濮甸弲?+ 闂傚懏鍔橀棅鈺呮晬閸粎鐭岄柣鈺婂枟閻栵絿鈧箍鍨洪崺娑氱博椤栨稒鈻旂憸閭︽緛缁辨岸鏁嶅☉妤冪闁伙絾鐟╅崳鎼佸礉濞戝磭绀勯悷鏇氭祰閸掓盯宕烽幏宀€鐛?閻犺櫣顒茬槐?
		cat.setNoAi(true);
		cat.setInvulnerable(true);
		cat.setInvisible(true);
		cat.setPersistenceRequired();
		cat.addTag("ssca_ghost_cat");
        long now = world.getGameTime();
        CATS.put(cat.getUUID(), new CatState(target, casterId, now));
        if (!world.addFreshEntity(cat)) { CATS.remove(cat.getUUID()); return; }
		// 闁哄嫭鍎抽懜浼村冀閸ヮ亶鍞堕柛鏍ф嫅缁辨瑦绂掗崨顖涚獥闁哄秴娴勭槐?
		SscAddonNetworking.sendSpookGhost(target, cat.getUUID(), BAL.i("cat_life_ticks", CAT_LIFE_TICKS));
		// 闁绘凹鍋勯幖鍗烆潩閺冨牏鍙鹃柡浣哥墳缁辨瑧鈧鑹鹃幃婊堟晬鐏炶偐鐭岄柣鈺婂枟閻栵綁宕ラ鈧崺宀勬晬?
		playSoundAt(target, SoundEvents.CAT_HISS, pos.x, pos.y, pos.z, 1.0f, 1.0f);
		// 闁告垵鎼┃鈧悘蹇撶箳閸庮偊姊块幘鍛閻庤鑹鹃幃婊呭垝閹烘垹鎽嶉柨娑樺缁酣鎯勯鐣屽灱闁?
		spawnParticlesAt(target, ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 6, 0.25, 0.25, 0.25);
	}

	/** 婵?tick 闁规亽鍔忕换姗€鐛悾灞肩触闂佹彃娴风亸妤呮晬濮樿櫕瀚归悹?闁?閻犙呮焿閻?闁?閻犳劗顥愰棅鈺呭箰閵壯冪劵闁?2 濮掑嫭姊圭涵鑸靛閵堝繒绀嗛柕?*/
	private static void tickGhostCat(ServerLevel world, CatState st, GhostCatEntity cat) {
		ServerPlayer target = st.target;
		long now = world.getGameTime();
		long age = now - st.bornTick;
		// 闁稿繑绮岀花鎶芥晬濮樺磭孝闁哄啳鍩栭惀鍛村椽椤掆偓閸?/ 闁烩晩鍠楅悥锝咁啅閸欏鍔?闁?闁绘垹鍠栧ù妯衡槈閸喐娈?
		if (age >= BAL.i("cat_life_ticks", CAT_LIFE_TICKS) || !target.isAlive() || target.level() != world) {
			spawnParticlesAt(target, ParticleTypes.CLOUD, cat.getX(), cat.getY(0.5), cat.getZ(), 8, 0.3, 0.3, 0.3);
			cat.discard();
			CATS.remove(cat.getUUID());
			return;
		}
		Vec3 catPos = cat.position();
		Vec3 eyePos = new Vec3(target.getX(), target.getY(0.5), target.getZ());
		Vec3 toEye = eyePos.subtract(catPos);
		double dist = toEye.length();
		// 闂傚牄鍨归幃婊堟儎椤旂晫鍨?
		float yaw = (float) Math.toDegrees(Math.atan2(toEye.x, toEye.z)) - 90.0f;
		cat.setYRot(yaw);
		cat.yHeadRot = yaw;
		cat.yBodyRot = yaw;

		if (st.phase == 0) {
			// 閺夆晛鈧喓鐛撻柨娑欑濠€鐐烘儎椤旂晫鍨肩紒澶庮嚙婵晠鏁嶉崷顣旳I 濞戞挸顑嗘晶婊堝礉閵娾斁鏀抽柛鏃戠厜缁辩湋imbAnimator 闁汇垹褰夌紞鍛磾椤斿吋鈻曞鐟板船婵晠鎳欓崸妤€鍔ラ柛鏂诲妿閺侀箖鏁?
			Vec3 dir = toEye.normalize();
			Vec3 next = catPos.add(dir.scale(BAL.d("cat_run_speed", CAT_RUN_SPEED)));
			cat.setPos(next.x, next.y, next.z);
			// 閻犵儤绻勯‖?4 闁?闁?閻犙呮焿閻戯箓骞嶉幋锝呭П
			if (dist <= BAL.d("cat_leap_distance", CAT_LEAP_DISTANCE)) {
				st.phase = 1;
				st.jumpFromX = catPos.x;
				st.jumpFromY = catPos.y;
				st.jumpFromZ = catPos.z;
				st.jumpTicks = 0;
				// 闁瑰灚鍨甸崵顕€宕滃鍕У闁告氨銆嬬槐娆撴偖椤愩垹寤洪梻鍕Х閻ㄧ喖鏁嶇仦鐣屾毎闁告碍鍩婄槐?
				playSoundAt(target, SoundEvents.CAT_AMBIENT, catPos.x, catPos.y, catPos.z, 0.9f, 0.75f);
			}
		} else if (st.phase == 1) {
			// 閻犲搫鐤囩粚顒勬晬濮橆厼绲婚柛濠傚悑婵繘鎮ч埡鍐ㄦ疇闁瑰灚鍨甸幃婊堟儎椤旂晫鍨奸柤瀛樻倐閸庢挳鏁?t / 0.4s闁?
			st.jumpTicks++;
			float t = Math.min(1.0f, st.jumpTicks / 8.0f);
			double nx = st.jumpFromX + (target.getX() - st.jumpFromX) * t;
			double ny = st.jumpFromY + (target.getY(0.5) - 1.4 - st.jumpFromY) * t
					+ Math.sin(t * Math.PI) * 0.9; // 鐎殿啫鍥モ偓?+0.9 闁?
			double nz = st.jumpFromZ + (target.getZ() - st.jumpFromZ) * t;
			// 闂傚啯褰冨畷杈ㄦ櫠濞嗘瑧绐楅悹鍝勭枃缁岊剛鎹勯姘辩獮闁告挸绉甸弻鐔煎嫉婢跺鐓欓柛?闁?闁烩晛鐡ㄧ敮瀵哥磼閹惧墎鏆悹鎰棎闂娾晠寮ㄧ拠鎻掓瘖
			BlockPos front = BlockPos.containing(nx, ny, nz);
			if (!world.getBlockState(front).isAir()) {
				completeCatAttack(world, st, cat, target);
				return;
			}
			cat.setPos(nx, ny, nz);
			if (t >= 1.0f) {
				completeCatAttack(world, st, cat, target);
			}
		}
	}

	/** 闂佹彃娴风亸妤冩嫻绾惧鐓╅柟鎼炲劤閸╁懘鏁?2 濮掑嫭姊圭涵鑸靛?+ 闁告稒鍨濋懙鎴︽閾忣偅娅?缂侇喗甯掗悺娆撴晬瀹€鍕吂闁告艾瀛╃粔鐑藉极閿濆啠鍋?*/
	private static void completeCatAttack(ServerLevel world, CatState st, GhostCatEntity cat, ServerPlayer target) {
		if (st.phase == 2) return;
		st.phase = 2;
		ServerPlayer caster = world.getServer().getPlayerList().getPlayer(st.casterUuid);
		if (caster != null && target.isAlive() && target.level() == world
                &&!github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(caster,target)) {
			target.hurt(caster.damageSources().indirectMagic(caster, caster), (float) BAL.d("clone_damage", CLONE_DAMAGE));
			// 闁告稒鍨濋懙鎴︽閾忣偅娅忛柨娑欒壘閹煎崬顫?+ 闁规悶鍎遍崵顕€鏁嶉崼婵堟毎闁告碍鍩婄槐婵囩閸涱垱绐楅柡宥呮祫缁?
			playSoundAt(target, SoundEvents.CAT_HISS, target.getX(), target.getY(), target.getZ(), 1.2f, 0.9f);
			playSoundAt(target, SoundEvents.PLAYER_ATTACK_SWEEP, target.getX(), target.getY(), target.getZ(), 0.7f, 1.2f);
		}
		// 婵炴垵鐗婇弳搴ㄦ晬濮樺崬鍔旈梻?+ 闁烩晩鍠楅悥锝嗗緞閹绢喓鈧﹪鎮滈悢璇残￠柨娑樼墕閻ｉ箖宕ラ幋鐘电厛閻庢稒鍔х槐婵囩閸涱垱绐楅柡宥呮祫缁?
		spawnParticlesAt(target, ParticleTypes.CLOUD, cat.getX(), cat.getY(0.5), cat.getZ(), 10, 0.35, 0.35, 0.35);
		spawnParticlesAt(target, ParticleTypes.FIREWORK, cat.getX(), cat.getY(1.8), cat.getZ(), 3, 0.25, 0.1, 0.25);
		cat.discard();
		CATS.remove(cat.getUUID());
	}

	// ==================== 闁汇垻鍠愰崹姘舵倷瑜版帒娅氶柡?====================

	/** 闁烩晩鍠楅悥锝夌叕椤愩垺鍊?180閹?闁归潧娲ょ亸顖炲箥閸撗€鏁勬慨妯绘閹倝鎮欓惂鍝ョ缁炬壆澧楅幐鎺楁儎閹烘挻锟ラ柡鍌滄嚀濞?+ 闁告碍鍨崇粭?闁? 闁哄秷澹堥崚娑㈠捶鐢喚绀嗛柕?*/
	private static Vec3 findAirPosBehind(ServerLevel world, LivingEntity target, double minDist, double maxDist,
	                                      int tries, double width, double height) {
		float behindYaw = target.getYHeadRot() + 180.0f;
		for (int i = 0; i < tries; i++) {
			float yaw = behindYaw + (world.random.nextFloat() - 0.5f) * 180.0f;
			double dist = minDist + world.random.nextDouble() * (maxDist - minDist);
			double cx = target.getX() - Math.sin(Math.toRadians(yaw)) * dist;
			double cz = target.getZ() + Math.cos(Math.toRadians(yaw)) * dist;
			Vec3 pos = tryGroundedAir(world, cx, target.getY(), cz, width, height);
			if (pos != null) return pos;
		}
		return null;
	}

	/** 闁哄本鍔楅崑锝夊川閵娿儲绾柟闈涘⒔閳规牕顫濋弮鍥ㄥ劙闁绘劗娅㈢槐娆撴偝椤栨俺鍩岄梻鍛箲濠р偓闂佹彃娲﹂悧閬嶆晬婢跺牃鍋?*/
	private static Vec3 findAirPosNear(ServerLevel world, Vec3 center, double radius, int tries,
	                                    double width, double height) {
		for (int i = 0; i < tries; i++) {
			float ang = world.random.nextFloat() * (float) (Math.PI * 2);
			double r = 0.8 + world.random.nextDouble() * radius;
			double cx = center.x + Math.cos(ang) * r;
			double cz = center.z + Math.sin(ang) * r;
			Vec3 pos = tryGroundedAir(world, cx, center.y, cz, width, height);
			if (pos != null) return pos;
		}
		return null;
	}

	/** 闁哄稄绻濋悰?(cx, cy, cz)闁挎稒姘ㄩ～顐﹀箻閻愬灚纾紒灞炬そ濡潡寮悩铏厵闁?+ 闁告碍鍨崇粭?闁? 闁哄秷澹堥崚娑㈠捶鐢喚绀夐弶鈺傛煥濞叉牠寮甸埀顒傜磼閸絾鍎伴柣鎰畭閳?*/
	private static Vec3 tryGroundedAir(ServerLevel world, double cx, double cy, double cz,
	                                    double width, double height) {
		AABB box = new AABB(cx - width / 2, cy, cz - width / 2, cx + width / 2, cy + height, cz + width / 2);
		if (!world.noCollision(null, box)) return null;
		// 闁告碍鍨崇粭鍛村嫉閳ь剚寰?2 闁哄秴鍚嬫竟妯兼嫻閺夋垶鍕鹃柨娑樼墦娴尖晠宕楀鍡椾壕缂佸瞼灏ㄧ槐?
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(Math.floor(cx), (int) Math.floor(cy), Math.floor(cz));
		int drop = 0;
		while (drop < 2 && world.getBlockState(mpos.below(drop + 1)).isAir()) {
			drop++;
		}
		if (drop >= 2) return null; // 闁诡噮鍓涢埞鏍惥閸涙壆绠?2 闁?闁?闁衡偓閹呯＞閻犲洢鍎甸崳浼村冀妞嬪骸浠?
		return new Vec3(cx, cy - drop, cz);
	}

	// ==================== 闁哄被鍎撮?/ 闁汇垻鍠庨幊锟犲川閵婏附鍩?====================

	/** 闁哄牆绉存慨鐔虹博椤栨稓鍙€閻犲浂婢佺槐鎵偖椤愶絾鏆伴柛鎴ｎ嚙閻ゅ嫭鎷呴幘瀛樞﹂柛姘剧畱閼垮洭鎮樻担鍐仭闁告梹绋掗埀顒佹穿缁辨獩ag 闊浂鍋夐惌鎯ь嚗閸曞墎绀嗛柕?*/
	public static boolean isGhostCreeperEntity(Entity e) {
		return e instanceof Creeper && e.getTags().contains("ssca_ghost_creeper");
	}

	/** 婵?tick 闁规亽鍔忕换姗€鏁嶉崼銏℃殸 SscAddonServerEvents 閻犲鍟伴弫銈夋晬婢舵稓绐楅柤浼欑畱婵繘骞€閺囩偟绌垮ǎ?闁告帞澧楀﹢锟犲Υ娓氣偓閸ｅ綊鎮銉﹀闁?闁瑰灚鍨甸幐浼村Υ?*/
	public static void tick(ServerPlayer player) {
		if (CREEPERS.isEmpty() && CATS.isEmpty()) return;
		if (!(player.level() instanceof ServerLevel world)) return;

		// 妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€閺囶亞绐楅柛娆樹簽閺侀亶鎯勯鐣屽灱闁哄牜鍏涘Ч澶愭儍?tick 闁规亽鍔忕换姗€鏁嶉崼婵嗙闂佹彃绋勭槐?
		int creeperLife = BAL.i("creeper_life_ticks", CREEPER_LIFE_TICKS);
		Iterator<Map.Entry<UUID, CreeperState>> cit = CREEPERS.entrySet().iterator();
		while (cit.hasNext()) {
			Map.Entry<UUID, CreeperState> e = cit.next();
			if (!e.getValue().target().equals(player.getUUID())) continue;
			Entity creeper = world.getEntity(e.getKey());
			if (!(creeper instanceof Creeper gc)) {
				cit.remove();
				continue;
			}
			if (gc.tickCount >= creeperLife) {
				// 闁告帞澧楀﹢锟犳晬濮橆厽锟ラ悗鍦仒濠碘偓闁绘牕妫濆▍搴ㄦ晬閸繐绱?缂侇喗甯炲ú鍧楀矗閹寸姵绐楅柡宥呮祫缁辨繃绋夊鍛暛闁圭虎鍙忕槐?
				cit.remove();
				playSoundAt(player, SoundEvents.GENERIC_EXPLODE, gc.getX(), gc.getY(), gc.getZ(), 1.0f, 1.0f);
				spawnParticlesAt(player, ParticleTypes.EXPLOSION_EMITTER, gc.getX(), gc.getY(0.5), gc.getZ(), 1, 0, 0, 0);
				gc.discard();
				continue;
			}
			// 闁哄牃鍋撻柛?1.2 缂佸甯槐浼存倷閸︻厼娅戠€殿喗娲戞穱濠囨晬閸綆娼掗悷娆忣樀濡垶鎯傞弬銈囩
			if (gc.tickCount >= creeperLife - 24) {
				gc.setSwellDir(1);
			}
		}

		// 妤犵偟鏅导鎺楁煂鎼达絽鐨搁柨娑欒壘瑜把囨偨鏉堚晜绐楅柡宥呮处濠€鐗堢閾忚鐣?tick 闁规亽鍔忕换?
		Iterator<Map.Entry<UUID, CatState>> it2 = CATS.entrySet().iterator();
		while (it2.hasNext()) {
			Map.Entry<UUID, CatState> e = it2.next();
			CatState st = e.getValue();
			if (!st.target.getUUID().equals(player.getUUID())) continue;
			Entity cat = world.getEntity(e.getKey());
			if (!(cat instanceof GhostCatEntity oc)) {
				it2.remove();
				continue;
			}
			tickGhostCat(world, st, oc);
		}
	}

	/** 闁烩晩鍠楅悥锝夊棘椤撶姴娈?闁告垹鍎ら埅闈涖€掗崨顖涘€為柨娑欑缁斿宕楃捄鐑樺€冲☉鎾愁儏閼垮洭鎮橀悽鍨仮閻犱浇鍩囬埀?*/
    public static void clearFor(UUID targetUuid, MinecraftServer server) {
        java.util.Set<UUID> ghosts = new java.util.HashSet<>();
        CREEPERS.forEach((id, state) -> { if (targetUuid.equals(state.target())) ghosts.add(id); });
        CATS.forEach((id, state) -> { if (targetUuid.equals(state.target.getUUID())) ghosts.add(id); });
        for (UUID id : ghosts) {
            for (ServerLevel world : server.getAllLevels()) {
                Entity ghost = world.getEntity(id);
                if (ghost != null) ghost.discard();
            }
            CREEPERS.remove(id);
            CATS.remove(id);
        }
    }

    public static boolean ownsGhost(UUID id) {
        return CREEPERS.containsKey(id) || CATS.containsKey(id);
    }

	/** 闁绘壕鏅涢宥夊箳婢跺苯娈犻柨娑欑缁斿鎮╅懜纰樺亾娓氬﹦绀勯悗鍦仒缂嶅鎮?tick 闁哄被鍎扮粭澶愬礆閻楀牊顦ч柤濂変簽閸斞呯矓婵犳碍鐝熼柨娑欑煘learAll 闁稿繑绮岀花鎶藉箥?tag闁挎稑顦埀?*/
	public static void onDisconnect(ServerPlayer player) {
        clearFor(player.getUUID(), player.getServer());
	}

	/** 闁哄牆绉存慨鐔煎闯閵娿儲鍎欓柛?闁轰胶澧楀畵渚€宕犻崨鏉戞閺夌偞鏋荤槐鏉裤€掗崨顖楁晞闁稿繈鍔戦崕鎾偐閼哥鍋撴担姝屽珯闁告垹绮鍐ㄢ枔鐎ｎ剚娈岄柣銊ュ閼垮洭鎮橀棃娑氭澖濞达絾鎸堕埀?*/
	public static void clearAll(MinecraftServer server) {
		for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
			for (Entity e : world.getAllEntities()) {
				if (e instanceof Creeper && e.getTags().contains("ssca_ghost_creeper")) {
					e.discard();
				}
				if (e instanceof GhostCatEntity && e.getTags().contains("ssca_ghost_cat")) {
					e.discard();
				}
			}
		}
		CREEPERS.clear();
		CATS.clear();
	}

	/** 闁哄牆绉存慨鐔虹博椤栨瑧鐨戝ù鐘哄煐閺佺偤宕樺畝瀣闁?SscAddonServerEvents 閻犲鍟伴弫銈夋晬婢舵稓绐楅柣鈺婂枟閻栵綁寮ㄧ拠鎻掓瘖妤犵偟鏅导鎺楁嚐閿曗偓婵繘骞€閺囩姵鐣遍柛鎺嬪€曢悾楣冨Υ?*/
	public static void registerEvents() {
		github.com.gengyoubo.sscfa.util.SscEvents.attackEntity((player, world, hand, entity, hitResult) -> {
			// AttackEntityCallback 婵炲鍔岄崣?PlayerEntity.attack闁挎稑鏈﹢鍥礉閿涘嫷浼傞悷娆欑畱瑜板倿鏁嶉崷鐜verPlayerEntityMixin 鐎圭寮堕悧宕団偓鍦剳缁?
			if (world.isClientSide || !(player instanceof ServerPlayer sp)) return net.minecraft.world.InteractionResult.PASS;
			if (isGhostCreeperEntity(entity)) {
				onGhostCreeperAttacked(sp, (Creeper) entity);
				return net.minecraft.world.InteractionResult.FAIL;
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
	}
}
