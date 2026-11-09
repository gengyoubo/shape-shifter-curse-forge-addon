package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import java.util.List;

/**
 * 閸愪即婀濋弬鐗堟Е閿涘牆鍟虹化浼欑礉缂佽儻澹婇崺鍝勭俺閿涘ackcooper閿涘绱版禒銉ㄥ殰闊偂璐熼崷鍡楃妇閻栧棗褰傜€垫帗鐨甸敍宀冨瘱閸ユ潙鍞撮柅鐘冲灇娴笺倕顔?+ 缂傛捇鈧?II 4s閵?
 *
 * <p>閺佹澘鈧厧顦荤純?{@code data/ssc_addon/spells/frost_nova.json}閿?
 * 閸╁搫鍣?3 娴?/ 閸楀﹤绶?4 閺?/ cd 5s / 閼版鎽?15閿涙稑宕愬鍕瘻 speed_multiplier 缂傗晜鏂侀敍鍫熺槨缁?+0.5 閺嶇》绱氶敍?
 * 缁嬧偓閺堝瀹虫稉楦挎憫/濮楁瑦妞傛０婵嗩樆 +25%閵?
 * 缁绢垱甯堕崚璺烘倻閿涙矮绗夐悙鍦櫑閿涘牆顕В鏃傚創閻掔増鏌婇弰鐕傜礆閿涘瞼绱﹂柅鐔告闂€鍧楁缁涘楠囧顔藉畾閿涘湢1 4s 閳?L5 6s閿涘鈧?
 * 閻ц棄鎮曢崡鏇窗娑撹姹夐崷銊у殠娑撴梻娲伴弽鍥у綀娣囨繃濮?閳?閸忓秳婵€閿涙稒鏌﹀▔鏇♀偓鍛拱娴滆桨绗夐崣妤€濂栭崫宥冣偓?/p>
 */
public class FrostNovaSpell extends Spell {

	/** 閸╄櫣顢呴崡濠傜窞閿涘牊鐗搁敍澶愮帛鐠併倧绱濈€圭偤妾崡濠傜窞 = 閸╄櫣顢?鑴?speed_multiplier(level)閿涙稖绻嶇悰灞炬娴?balance 韫囶偆鍙庣拠璇插絿閵?*/
	private static final double BASE_RADIUS = 4.0;

	// 闂冭埖顔?5閿涙俺绻嶇悰灞炬韫囶偆鍙庣拠璇插絿閿涘澃pells.frost_nova閿涙稑鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("spells.frost_nova");

	public FrostNovaSpell() {
		super(new ResourceLocation("ssc_addon", "frost_nova"), SpellRarity.GREEN);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		if (!(caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		double radius = RuneCastContext.current().scale(BAL.d("base_radius", BASE_RADIUS) * getSpeedMultiplier(level),RuneModifiers.Stat.AREA);
		// 缁嬧偓閺堝瀹虫稉楦挎憫/濮楁瑦妞傞敍宀€鏁撻弫鍫ｅ瘱閸ユ挳顤傛径?+25%閿涘牏瀚粩瀣╃艾缁涘楠囩紓鈺傛杹閿涘本鏆熼幑顔煎瘶閺€?rarity 閼奉亜濮╃捄鐔兼閿?
		SpellRarity rarity = getRarity(level);
		if (rarity == SpellRarity.BLUE || rarity == SpellRarity.ORANGE) {
			radius *= 1.25;
		}
		// 缂傛捇鈧?II閿涘本妞傞梹?4s閿涘湢1-2閿? 5s閿涘湢3-4閿? 6s閿涘湢5閿?
		int slowTicks = RuneCastContext.current().duration(80 + (level >= 3 ? 20 : 0) + (level >= 5 ? 20 : 0),RuneModifiers.Stat.NEGATIVE);
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
		int hitExpTen = ssc_addon$takePendingExp();
		for (LivingEntity target : targets) {
			if (target.distanceTo(caster) > radius) {
				continue;
			}
			// 閸忣剙鍙￠崨鎴掕厬缂佹挾鐣婚敍鍫㈡閸氬秴宕熺挒浣稿帳 閳?濞夋洘婀虫导銈咁唺 閳?缂佸繘鐛欑悰銉ュ絺 閳?濞翠焦娣抽柦鈺佺摍闁劗娲伴弽鍥风幢ICE 缁粯妫ら崙缁樻絻閸掑棙鏁敍澶涚窗鐟?SpellHitHelper
			var hit = github.com.gengyoubo.sscfa.spell.SpellHitHelper.projectileHit(
					caster, target, power, github.com.gengyoubo.sscfa.spell.FormationElement.ICE,
					solo ? null : ssc_addon$getRefundCastId(), hitExpTen);
			if (hit != github.com.gengyoubo.sscfa.spell.SpellHitHelper.HitResult.HIT) {
				continue;
			}
			hitExpTen = 0; // Only the first successful hit consumes the cast budget.
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
			// 闂囨粎顣抽敍姘Б閻?-50%閿涘本妞傞梹澶哥瑢缂傛捇鈧喎鎮撳銉礄L1=3s 閳?L5=5s閿?
			target.addEffect(new MobEffectInstance(
					github.com.gengyoubo.sscfa.init.AddonEffects.FROST_SHATTER.get(), slowTicks, 0));
		}
		// 濠曟柨鍤敍姘辨倖瑜般垹鐦ㄥ鏃傜煈鐎涙劧绱欓崣灞界湴闂嗩亣濮抽悶鍐桨 + 閸愬懘鍎存禍鎴︽禈閿? 鐎垫帗鐨甸棅铏櫏閿涘牅绗岄悜鍫㈠姍閺傜増妲﹂崥灞绢儥閻炲啫鑸板鏂垮毉閿?
		github.com.gengyoubo.sscfa.util.SpellFxUtils.sphere(serverWorld, caster,
				caster.getX(), caster.getY() + 1.0, caster.getZ(), radius,
				ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD, 8);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 0.8f);
		serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
	}
}
