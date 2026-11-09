package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 閵嗗苯鍤弶鈧挧瀣厴閵嗗秶娈戦柌濠冩杹娓氀嶇礄SP娴ｅ潡鐡?/ 缁俱垹鐖奸拃鎴掑▏姒勬棑绱濋張宥呭缁旑垽绱氶妴?
 *
 * <p>鐠у鍏樺┑鈧ú缁樻埂闂傚鏁遍崢鐔烘晸娑撹濮╅幎鈧懗钘夊弳閸欙絽婀張宥呭缁旑垶鈧瀚ㄦ稉?濞嗏剝濡ч懗鏂ょ窗
 * <ul>
 *   <li>娑撶粯濡ч懗?= 鐠у鍏橀悘顐ゅ箚閿涙艾鍙忛崨銊︽埂閸忓秷鎽戦敍灞肩瑢濮濓絽鐖堕悘顐ゅ箚娴滄帗鏋奸敍鍫㈠Ц閹焦婧€鐟?{@link #tryCastRing}閿涘鈧?/li>
 *   <li>濞嗏剝濡ч懗?= SP 閸氭劖浼呴幋?Red 閻忣偆鎮嗛敍姘帳閽冩縿鈧礁鍘ら弲顕€鈧艾鍠庨崡鎾櫞閺€鍙ョ濞喡扳偓?/li>
 * </ul>
 * 闁插﹥鏂侀崡铏Х閼版ぞ绔村▎陇绁撮懗鑺ユ簚娴兼熬绱濋悘顐ゅ箚閹镐胶鐢婚弮鍫曟？閻欘剛鐝涚拋锛勭暬閵?
 */
public final class KillEmpowerCast {

	private KillEmpowerCast() {}

	// 鐠у鍏橀崥鎰紖閸欏倹鏆熼敍姘剁帛鐠併倓绗?power JSON 鐢瓕顫夐崥鎰紖娑撯偓閼疯揪绱? 閺?/ 6 娴?鑴抽棃鐐靛负鐎硅泛鈧秶宸?/ 閻忚偐鍎?5s閿涘绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.kill_empower_cast閿?
	private static final float BREATH_DISTANCE = 8.0f;
	private static final float BREATH_DAMAGE = 6.0f;
	private static final float BREATH_NON_PLAYER_MULT_SP = 3.0f;   // 姒涙顓婚敍姹糚娴ｅ潡鐡?閸氭劖浼呯€佃鈧亜鈧秶宸奸敍鍫滅瑢濮濓絽鐖堕悧?JSON 鑴? 鐎靛綊缍堥敍?
	private static final float BREATH_NON_PLAYER_MULT_RED = 1.5f;  // 姒涙顓婚敍娑氬閸洝鎯?閸氭劖浼呯€佃鈧亜鈧秶宸奸敍鍫濇倱濠?鑴?.5閿?
	private static final int BURN_DURATION = 100;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.kill_empower_cast");

	/** 鐠у鍏橀悘顐ゅ箚娴笺倕顔婄捄绛圭礄KillEmpowerManager 濮?16t 鐠嬪啰鏁ゆ稉鈧▎鈽呯幢娑撳孩顒滅敮鍝ュ箚 effects_loop 閸氬矁濡總蹇撴倱閸楀﹤绶為敍澶堚偓?*/
	public static void tickEmpowerRing(ServerPlayer player) {
		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// 閽冩繄浼€閹躲倗顑佹穱顔筋劀閿涘牊鐦＄捄鍐茬杽閺冩儼顕伴崣鏍电礉閹?閹芥ê宓嗛弮鍓佹晸閺佸牞绱氶敍姘縺鐎?+4閵嗕浇瀵栭崶?-40%
		boolean amulet = TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.BLUE_FIRE_AMULET.get());
		final float base;
		if (redForm) base = amulet ? 9.0f : 5.0f; else base = amulet ? 8.0f : 4.0f;	// Red 閻滎垰鐔€娴?5閳?閿涙奔P 閻滎垰鐔€娴?4閳?閿涘牊濮㈢粭?+4閿?
		final double ringRadius = amulet ? 3.6 : 6.0;
		AABB box = player.getBoundingBox().inflate(ringRadius);
		ResourceKey<DamageType> genericKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "generic"));
		world.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator()).forEach(e -> {
			if (WhitelistUtils.isProtected(player, e)) return;
			if (player.distanceToSqr(e) > ringRadius * ringRadius) return;
			float dmg = e instanceof net.minecraft.world.entity.player.Player ? base : base * 2.0f;	// 鐎靛綊娼悳鈺侇啀閿涘湤PC/閹亞澧块敍澹?
			e.invulnerableTime = 0;	// 閸涙垝鑵戦崜宥嗙閿涙矮绗夌悮顐ｆ珮閺€缁樻￥閺佸苯鎶氶崥?
			if (e.hurt(new net.minecraft.world.damagesource.DamageSource(e.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(genericKey), player, player), dmg)) {
				e.invulnerableTime = 0;	// 閸涙垝鑵戦崥搴㈢閿涙矮绗夐悾娆愭￥閺佸苯鎶氶崣宥呮偠閺咁喗鏁?
				e.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get(), redForm ? 80 : 60, 0), player);
				KillEmpowerManager.trackBurn(player, e, redForm ? 80 : 60, false);
				e.setRemainingFireTicks(redForm ? 80 : 60);
			}
		});
	}

	/** 閸掋倖鏌囧锝呯埗閻忣偆骞嗛敍鍫ユ姜鐠у鍏橀悳顖ょ礆閺勵垰鎯佸锝呮躬闁插﹥鏂侀敍姘劀鐢摜骞嗗鈧弮?toggle_state=1閿涙稖绁撮懗鐣屽箚娑撳秶鐤?toggle閵嗗倸顓归幋椋庮伂閸欘垵顕伴妴?*/
	public static boolean isNormalRingActive(net.minecraft.world.entity.player.Player player) {
		boolean red = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// 4 娑?Identifier 娑撴椽娼ら幀浣哥埗闁?
		ResourceLocation toggleId = red ? TOGGLE_RED : TOGGLE_SP;
		ResourceLocation amuletToggleId = red ? AMULET_TOGGLE_RED : AMULET_TOGGLE_SP;
		return PowerUtils.getClientResourceValue(player, toggleId) == 1
				|| PowerUtils.getClientResourceValue(player, amuletToggleId) == 1;
	}

	/** 閻忣偆骞?toggle 鐠у嫭绨?id 鐢悂鍣洪敍鍫㈠/SP 鑴?閺咁噣鈧?閹躲倗顑侀敍宀勪缉閸?HUD 濮ｅ繐鎶?new Identifier閿涘鈧?*/
	private static final ResourceLocation TOGGLE_SP = new ResourceLocation("my_addon", "form_familiar_fox_sp_blue_fire_ring_toggle_state");
	private static final ResourceLocation TOGGLE_RED = new ResourceLocation("my_addon", "form_familiar_fox_red_blue_fire_ring_toggle_state");
	private static final ResourceLocation AMULET_TOGGLE_SP = new ResourceLocation("my_addon", "form_familiar_fox_sp_blue_fire_ring_amulet_toggle_state");
	private static final ResourceLocation AMULET_TOGGLE_RED = new ResourceLocation("my_addon", "form_familiar_fox_red_blue_fire_ring_amulet_toggle_state");

	/** 鐎广垺鍩涚粩顖涘瘻娑撶粯濡ч懗浠嬫暛閿涘牐绁撮懗鑺モ偓渚婄礆閵嗗倽绻戦崶?true 鐞涖劎銇氬鎻掝槱閻炲棎鈧?*/
	public static boolean tryCastRing(ServerPlayer player) {
		if (!KillEmpowerManager.isEmpowerForm(player)) return false;
		KillEmpowerState state = KillEmpowerManager.readState(player);
		if (!state.hasReady() && !state.hasRing()) return false;

		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);

		if (state.hasRing()) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			KillEmpowerManager.writeState(player, state.stopRing());
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			return true;
		}

		// STATE_READY閿涙碍顒滅敮鍝ュ箚閿涘牆鎯堥幎銈囶儊閻滎垽绱氬鈧惈鈧?閳?閸忓牆鍙ч悳顖濈箻鐎电懓绨?CD閿涘牐绁撮懗鎴掔箽閻ｆ瑱绱濋柆顔惧兊娴犲秹娈ｉ挊蹇ョ礆閿涙稑鎯侀崚娆戞纯閹恒儱绱戠挧瀣厴閻?
		boolean normalRingOn = player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
		if (normalRingOn) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
			String ringPrefix = (redForm ? "form_familiar_fox_red" : "form_familiar_fox_sp") + "_blue_fire_ring";
			boolean amuletRingOn = PowerUtils.getResourceValue(player, new ResourceLocation("my_addon", ringPrefix + "_amulet_toggle_state")) == 1;
			String ringPower = ringPrefix + (amuletRingOn ? "_amulet" : "");
			PowerUtils.setResourceValueAndSync(player, new ResourceLocation("my_addon", ringPower + "_toggle_state"), 0);
			// 閸忓啿鎽㈡總妤冨箚鐏忚鲸瀵滅拠銉т紑閻?JSON 閻?cooldown 鐠ч鐣?
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, "my_addon:" + ringPower + "_key_activation");
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
			return true; // 閺堫剚顐奸幐澶愭暛閸欘亜鍙ч悳顖ょ幢鐠у鍏樻禒宥呮躬閿涘牅绗呭▎鈩冨瘻闁款喖绱戠挧瀣厴閻滎垽绱?
		}

		// 閽冩繄浼€閹躲倗顑佹穱顔筋劀閿涙碍瀵旂紒顓熸闂?-15%閿涘牓鍣撮弨鍓х仜闂傜顕伴崣鏍电幢SP 232閳?97閵嗕阜ed 280閳?38閿?
		boolean amulet = TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.BLUE_FIRE_AMULET.get());
		int fullRingTicks = redForm ? KillEmpowerManager.ringTicksRed() : KillEmpowerManager.ringTicksSp();
		if (amulet) fullRingTicks = Math.round(fullRingTicks * 0.85f);
		player.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get(), fullRingTicks, 0, true, false, true));
		MobEffectInstance ringEffect = player.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BLUE_FIRE_RING.get());
		if (ringEffect == null || ringEffect.getDuration() <= 0) return true;
		PowerUtils.setResourceValueAndSync(player, FormIdentifiers.EMPOWER_RING_DURATION, ringEffect.getDuration());
		KillEmpowerManager.writeState(player, state.startRing(ringEffect.getDuration()));
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.0f);
		return true;
	}

	/** 鐎广垺鍩涚粩顖涘瘻濞嗏剝濡ч懗浠嬫暛閿涘牐绁撮懗鑺モ偓渚婄礆閵嗗倽绻戦崶?true 鐞涖劎銇氬鎻掝槱閻炲棴绱欑挧瀣厴閸氭劖浼呭鏌ュ櫞閺€鎾呯礆閵?*/
	public static boolean tryCastBreath(ServerPlayer player) {
		if (!KillEmpowerManager.isEmpowerForm(player)) return false;
		KillEmpowerState state = KillEmpowerManager.readState(player);
		if (!state.hasReady()) return false;

		ServerLevel world = (ServerLevel) player.level();
		boolean redForm = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
		// balance 鏉╂劘顢戦弮璺哄棘閺佸府绱欓崥灞炬煙濞夋洖顦垮▎鈥插▏閻㈩煉绱濋崗鍫濆絿鐏炩偓闁劌褰夐柌蹇ョ礆
		float nonPlayerMult = redForm
				? (float) BAL.d("breath_mult_red", BREATH_NON_PLAYER_MULT_RED)
				: (float) BAL.d("breath_mult_sp", BREATH_NON_PLAYER_MULT_SP);
		double breathDist = BAL.d("breath_distance", BREATH_DISTANCE);
		double breathDistSq = breathDist * breathDist;
		float breathDamage = (float) BAL.d("breath_damage", BREATH_DAMAGE);
		int burnDuration = BAL.i("burn_duration", BURN_DURATION);

		// 濞戝牐鈧绁撮懗鏂ょ礄閺嶅洩顔囩挧瀣厴閸戠粯娼?閳?閺堫剚顐奸崥鎰紖閻ㄥ嫬鍤弶鈧稉宥埿曢崣鎴炴煀鐠у鍏橀敍?
		KillEmpowerManager.writeState(player, state.consume());

		if (redForm) {
			github.com.gengyoubo.sscfa.action.SscAddonActions.launchFoxFireball(player, true);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 1.0f, 1.0f);
			return true;
		}

		// 閸忓秷鍨傞柌濠冩杹閿涙矮绗夐幍?mana閵嗕椒绗夊Λ鈧弻?CD/mana 閺夆€叉閿涙稑甯?CD 閼汇儱婀挧棰佺瑝閸欐濂栭崫宥忕礄娑撳秹鍣哥純顕嗙礆
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		ResourceKey<DamageType> magicKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "magic"));
		AABB box = player.getBoundingBox().inflate(breathDist).expandTowards(look.scale(breathDist));
		world.getEntitiesOfClass(LivingEntity.class, box, t -> t != player && t.isAlive() && !t.isSpectator()).forEach(t -> {
			if (WhitelistUtils.isProtected(player, t)) return;
			Vec3 toT = t.position().add(0, t.getBbHeight() / 2.0, 0).subtract(eye).normalize();
			if (look.dot(toT) > 0.8 && player.distanceToSqr(t) < breathDistSq) {
				float dmg = t instanceof net.minecraft.world.entity.player.Player ? breathDamage : breathDamage * nonPlayerMult;
				t.invulnerableTime = 0;	// 閸涙垝鑵戦崜宥嗙閿涙矮绗夌悮顐ｆ珮閺€缁樻￥閺佸苯鎶氶崥?
				if (t.hurt(new net.minecraft.world.damagesource.DamageSource(t.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(magicKey), player, player), dmg)) {
					t.invulnerableTime = 0;	// 閸涙垝鑵戦崥搴㈢閿涙矮绗夐悾娆愭￥閺佸苯鎶氶崣宥呮偠閺咁喗鏁?
				t.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FOX_FIRE_BURN.get(), burnDuration, 0), player);
				KillEmpowerManager.trackBurn(player, t, burnDuration, false);
					if (t instanceof github.com.gengyoubo.sscfa.util.SscIgnitedEntityAccessor acc) {
						acc.sscAddon$setIgniterUuid(player.getUUID());
					}
				}
			}
		});
		// 鐞涖劎骞囨径宥呭煝 power JSON閿涙艾澧犻弬鐟邦樋鐏炲倿鐡婇悘顐ょ煈鐎?+ 閻忣偆鍔欓崗鍛板厴/閻欐劗瀚洪崣顐︾叾閺?
		for (int d = 1; d <= 5; d++) {
			double spread = 0.2 * d;
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.SOUL_FIRE_FLAME,
					player.getX() + look.x * d, player.getY() + 1.2, player.getZ() + look.z * d,
					4 + d * 3, spread, 0.4 + spread * 0.5, spread, 0.04);
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 1.0f, 1.0f);
		return true;
	}
}
