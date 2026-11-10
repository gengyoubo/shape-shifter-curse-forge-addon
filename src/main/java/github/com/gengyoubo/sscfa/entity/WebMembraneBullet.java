package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.block.WebMembraneBlock;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 閺堝牏绮愰摂娑栤偓灞炬暰閸戠粯膩瀵繈鈧秷娲撴稉婵嗚剨閿涙艾顦查悽銊ュ斧閻?{@link WebBullet} 閻ㄥ嫰顥ｇ悰?/ 缁帒鐡?/ 閸欐垵鐨犻棅铏櫏閿?
 * 娴ｅ棙鏁奸崘娆忔嚒娑擃厽鏅ラ弸婧锯偓鏂衡偓鏂挎嚒娑擃厽鏌熼崸?/ 鐎圭偘缍嬮崥搴㈠瘻閽冨嫬濮忓锝呮躬閸楀﹤绶為崘鍛板垱闂堛垽鎽甸崙蹇涒偓鐔绘磽缂冩埊绱檣@link WebMembraneBlock}閿涘绱?
 * 閼板矂娼崢鐔哄閻ㄥ嫪澶嶉弮璺恒亯濮婎垬鈧€€ier1/2/3 閳?閸楀﹤绶?3 / 5 / 6 閺嶇鈧?
 */
public class WebMembraneBullet extends WebBulletBase {

	public WebMembraneBullet(EntityType<? extends WebMembraneBullet> type, Level world) {
		super(type, world);
	}

	public WebMembraneBullet(LivingEntity owner, int tier) {
		super(RegAddonEntities.WEB_MEMBRANE_BULLET, owner.level());
		this.owner = owner;
		this.Tier = tier;
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.10000000149011612, owner.getZ());
	}

	// 瑜版帒鐫樼拠瀛樻閿涙艾甯悧?WebBullet.owner 鐎涙顔屾稉宥呭晸閸?NBT閿涘牆鑴婃担鎾崇摠濡楋絾浠径宥呮倵閹帊璐?null閿涘绱?
	// 閼?ProjectileEntity.getOwner() 鐠?owner UUID 閹垹顦查柧鎹愮熅閵嗕線顥ｇ悰灞炬埂閸愬懍绡冮張澶岀处鐎涙ê绱╅悽銊ｂ偓?
	// 閸ョ姵顒濋張顒傝閹碘偓閺堝鎳℃稉顓犵波缁犳ぜ鈧線鎽电純鎴濈秺鐏炵偑鈧胶娅ч崥宥呭礋閸掋倖鏌囨稉鈧瀣暏 this.getOwner()閿?
	// 缁備焦顒涚拠?this.owner閿?026-10-15 鐎圭偞绁存穱顔碱槻閿涙瓊BT 閹垹顦插閫涙丢缁炬椽鏁滈崝鐘冲灇/闁捐櫣缍夎ぐ鎺戠潣閿涘鈧?

	/** 閾旀稓缍夊纭呮儰閸︽壆鍨庨悙鍝ユ畱閾氭缍夌紓鐘洪煩閼煎啫娲块崡濠傜窞閿涘牊鐗搁敍澶涚窗娑撳氦鎼崝娑欍€傞弮鐘插彠閿涘苯娴愮€?8 閺嶇鈧?*/
	private static final double AURA_RADIUS = 8.0;
	/** 閾旀稓缍夌紓鐘洪煩閼煎啫娲块弬钘夊閹镐胶鐢婚弮鍫曟毐閿涘澅ick閿涘绱? 缁夋帪绱欐稉搴や刊缂冩垶鏌﹂崝鐘辩閼疯揪绱氶妴?*/
	private static final int AURA_DURATION = 100;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.web_membrane");

	/** 閽冨嫬濮忓?閳?閸戝繘鈧喓缍夐崡濠傜窞閿涙ier3=6 / tier2=5 / 閸忔湹缍?3閿涘牊娓舵担?3 閺嶇》绱氶妴?*/
	private static double radiusForTier(int tier) {
		if (tier >= 3) return 6.0;
		if (tier >= 2) return 5.0;
		return 3.0;
	}

	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		if (this.level() instanceof ServerLevel world) {
			BlockPos hit = blockHitResult.getBlockPos();
			WebMembraneBlock.coatArea(world, hit, radiusForTier(this.Tier), this.getOwner() != null ? this.getOwner().getUUID() : null);
			applyBoundAura(world, hit); // 閻栧棛鍋㈤懠鍐ㄦ纯閻╁瓨甯撮弬钘夊閾氭缍夌紓鐘洪煩
			playHit(world);
		}
		this.discard();
	}

	@Override
	public void onHitEntity(EntityHitResult entityHitResult) {
		Entity entity = entityHitResult.getEntity();
		if (this.level() instanceof ServerLevel world) {
			BlockPos hit = entity.blockPosition();
			WebMembraneBlock.coatArea(world, hit, radiusForTier(this.Tier), this.getOwner() != null ? this.getOwner().getUUID() : null);
			applyBoundAura(world, hit); // 閻栧棛鍋㈤懠鍐ㄦ纯鐟佺灏?/ 閺傝棄濮為摎妯肩秹缂傜姾闊?
			// 閻╁瓨甯撮崨鎴掕厬閿涙氨绮ㄩ懠褑铔嬮崢鐔哄闁槒绶垾鏂衡偓鏃傗€樼€规碍鈧冨綌閸旂姷绱堕煬顐ｆ闂€鍖＄礄200/400/600t閿涘绱濈槐顖溞濆?500t 鏉烆剝灏庨敍鍫滅瑢閸樼喓澧?SSC 娑撯偓閼疯揪绱?
			if (entity instanceof LivingEntity living && !isProtected(living)) {
				int buffTime = this.Tier >= 3 ? 600 : (this.Tier >= 2 ? 400 : 200);
				github.com.gengyoubo.sscfa.compat.EntangledEffectUtils.applyEntangledEffect(this.getOwner(), living, buffTime);
				// 缁狀厽鐦ㄧ痪娲暅濡偓濞村绱欐稉搴″斧閻?WebBullet 娑撯偓閼疯揪绱氶敍姘閹靛蝎鐟佸懐姹夐柨銈嗘閿涘苯鎳℃稉顓℃嫹閸旂姵濮囬幒铚傛縺鐎?閾忔艾鎬?娑擃厽鐦ㄩ埅顫幢
				// plus 閻楀牓顤傛径鏍ㄦ煢閸旂姷绱堕煬顐礄Tier1/2/3BuffTime = 200/400/600t閿?
				applyVenomSpindleBonus(living);
			}
			playHit(world);
		}
		this.discard();
	}

	/** 缁狀厽鐦ㄧ痪娲暅閸涙垝鑵戦崝鐘冲灇閿涘牊鏆熼崐鑲╁弾閹跺嫬甯悧?WebBullet#onEntityHit 娑撳銆傞敍澶堚偓?*/
	private void applyVenomSpindleBonus(LivingEntity target) {
		if (!(this.getOwner() instanceof net.minecraft.world.entity.player.Player player)) return;
		if (!this.EnableVenomSpindle) return;
		boolean upgraded = isExtraHandVenomSpindleEquipped(player,
				github.com.gengyoubo.sscfa.compat.RegCustomItem.VENOM_SPINDLE_PLUS);
		if (!upgraded && !isExtraHandVenomSpindleEquipped(player,
				github.com.gengyoubo.sscfa.compat.RegCustomItem.VENOM_SPINDLE)) return;
		switch (this.Tier) {
			case 2 -> {
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 2));
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 60, 2));
				target.hurt(this.damageSources().thrown(this, this.getOwner()), 6.0F);
			}
			case 3 -> {
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 160, 3));
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 100, 2));
				target.hurt(this.damageSources().thrown(this, this.getOwner()), 8.0F);
			}
			default -> {
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 1));
				target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 20, 2));
				target.hurt(this.damageSources().thrown(this, this.getOwner()), 5.0F);
			}
		}
		if (upgraded) {
			int duration = switch (this.Tier) {
				case 2 -> Tier2BuffTime;
				case 3 -> Tier3BuffTime;
				default -> Tier1BuffTime;
			};
			github.com.gengyoubo.sscfa.compat.EntangledEffectUtils.applyEntangledEffect(this.getOwner(), target, duration);
		}
	}

	/** 婢跺秴鍩㈤崢鐔哄 WebBullet 閻ㄥ嫬澹囬幍瀣眽闁裤倖顥呭ù瀣剁礄閸樼喐鏌熷▔?private閿涘苯鐡欑猾璁崇瑝閸欘垵顫嗛敍澶堚偓?*/
	private boolean isExtraHandVenomSpindleEquipped(net.minecraft.world.entity.player.Player player,
													net.minecraft.world.item.Item item) {
		return github.com.gengyoubo.sscfa.compat.TrinketsConditionAction.CheckEquipped(
				player, "auto", "hand", "extra_hand", 0,
				stack -> stack.is(item),
				false
		);
	}

	/**
	 * 閾旀稓缍夊纭呮儰閸︽壆鍨庨悙闈╃窗閸楀﹤绶?8 閺嶇厧鍞撮幍鈧張澶愭姜閸忓秶鏌呴悽鐔哄⒖閳ユ柡鈧?
	 * <ul>
	 *   <li>閹稿顩ч悳鍥紮閼佳嶇礄鐠ф澘甯悧?applyEntangledEffect 娑撯偓濞嗏€冲綌濠娐ゆ祮 ENTANGLED_FULL_EFFECT閿涘苯鎯堥懠褎膩閸?閻溾晛顔?s/閹亞澧?5s/濮濊楠搁摂娑氱秹/閹哄绁︽鐔锋抄閿涘绱?/li>
	 *   <li>閺堫亙鑵戠粵鎹愶紮閼佳嗏偓鍛煢閸旂姾婀楃純鎴犵炊闊偓绱欓崙蹇涒偓?閹告牗甯㈤悿鎻掑У+閾忔艾鎬ラ敍澶堚偓?/li>
	 * </ul>
	 * 鐟佺灏庡鍌滃芳閿涙矮鑵戣箛鍐跨礄0閺嶇》绱氶埆?鏉堝湱绱敍?閺嶇》绱氱痪鎸庘偓褑鈥滈崙蹇ョ礉闂呭繗鎼崝娑欍€傞柅鎺戭杻閿涘澅1:10%閳?%閵嗕辜2:35%閳?%閵嗕辜3:60%閳?0%閿涘绱?
	 * 闊晝缍夐崝鐘冲灇閿涙氨娲伴弽鍥煩娑撳﹨婀楃純鎴犵炊闊偅鐦￠崜?20t 鐟佺灏庡鍌滃芳 +5%閿涘牅绗傞梽?+30%閿涘苯宓嗛惌顓熸闂傛潙鍞撮煪鈺冪秹鏉╁洤顦块弴瀛樻鐞氼偉锛欓敍澶堚偓?
	 */
	private void applyBoundAura(ServerLevel world, BlockPos center) {
		// 闂冭埖顔?5閿涙艾鎮撻崙鑺ユ殶婢舵碍顐肩拠璇插絿鐏炩偓闁劌褰夐柌蹇撳
		double auraRadius = BAL.d("aura_radius", AURA_RADIUS);
		double r2 = auraRadius * auraRadius;
		double cx = center.getX() + 0.5, cy = center.getY() + 0.5, cz = center.getZ() + 0.5;
		// 娑擃厼绺鹃埆鎺曠珶缂傛锛欓懠褎顩ч悳鍥风礄闂呭繗鎼崝娑欍€傞柅鎺戭杻閿涘绱皌1 10%閳?%閵嗕辜2 35%閳?%閵嗕辜3 60%閳?0%
		double base = this.Tier >= 3 ? 0.60 : (this.Tier >= 2 ? 0.35 : 0.10);
		double edge = this.Tier >= 3 ? 0.10 : (this.Tier >= 2 ? 0.05 : 0.00);
		for (LivingEntity living : world.getEntitiesOfClass(LivingEntity.class,
				new net.minecraft.world.phys.AABB(center).inflate(auraRadius), e -> !isBoundImmune((LivingEntity) e))) {
			double distSq = living.distanceToSqr(cx, cy, cz);
			if (distSq > r2) continue;
			// 鐠烘繄顬囩痪鎸庘偓褑鈥滈崙?+ 闊晝缍夐崝鐘冲灇閿涘牏鏁?sqrt 鏉╂ê甯痪鎸庘偓褑绐涚粋璇插棘娑撳氦鈥滈崙蹇旂槷娓氬顓哥粻妤嬬礆
			double dist = Math.sqrt(distSq);
			double prob = base + (edge - base) * (dist / auraRadius);
			MobEffectInstance bound = living.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get());
			int boundLeft = bound != null ? bound.getDuration() : 0;
			prob += Math.min(0.30, (boundLeft / 20.0) * 0.05);
			if (world.getRandom().nextDouble() < prob) {
				// 鐠ф澘甯悧鍫㈢炊闊偂绔村▎鈥冲綌濠娐ゆ祮閼佳団偓鏄忕帆閿?00t = 5鑴?00閿涘矁鎻?ENTANGLED_DURATION_PER_LEVEL鑴?MAX_LEVEL+1) 闂冨牆鈧》绱?
				github.com.gengyoubo.sscfa.compat.EntangledEffectUtils.applyEntangledEffect(this.getOwner(), living, 500);
			} else {
			// 閺堫亣锛欓懠褝绱伴弬钘夊閾氭缍夌紓鐘洪煩閿涘牆鍣洪柅?閹告牗甯㈤悿鎻掑У+閾忔艾鎬ラ敍澶涚礉娑撳搫鎮楃紒顓′刊缂?閸愬秵顐奸崨鎴掕厬缁鳖垳袧鐟佺灏庡鍌滃芳
				// 鐢附鏌﹀▔鏇♀偓?source閿涘奔绶垫鐔糕叺姒勬柣鈧苯鍙嗗锔衡偓宄漞buff 閹凤附鍩呰ぐ鎺戞礈
				living.addEffect(new MobEffectInstance(
						github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get(),
						BAL.i("aura_duration", AURA_DURATION), 0, false, false, true), this.getOwner());
			}
		}
	}

	/** 閾旀稓缍夌紓鐘洪煩閼煎啫娲块崗宥囨焻閿涙俺婀楅摂娑氳 / 閹妇浼掔化?/ 閺傝姤纭堕懓鍛閸氬秴宕熸稉顏冪秼鐠炰礁鍘ら妴?*/
	private boolean isBoundImmune(LivingEntity target) {
		if (target instanceof net.minecraft.world.entity.monster.Spider) return true;
		if (target instanceof net.minecraft.world.entity.player.Player p
				&& (github.com.gengyoubo.sscfa.util.FormUtils.isForm(p, github.com.gengyoubo.sscfa.util.FormIdentifiers.SPIDER_MOON_WEAVER)
				|| github.com.gengyoubo.sscfa.util.FormUtils.isForm(p, github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP)
				|| github.com.gengyoubo.sscfa.util.FormUtils.isForm(p, github.com.gengyoubo.sscfa.util.FormIdentifiers.FALLEN_ALLAY_SP))) {
			return true;
		}
		return isProtected(target);
	}

	private boolean isProtected(LivingEntity target) {
		return this.getOwner() instanceof ServerPlayer sp && WhitelistUtils.isProtected(sp, target);
	}

	private void playHit(ServerLevel world) {
		world.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(),
				24, 0.35, 0.35, 0.35, 0.05);
		world.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.WET_GRASS_BREAK, SoundSource.NEUTRAL, 1.0f, 0.7f + this.random.nextFloat() * 0.3f);
	}
}
