package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 閼垫劘娈屾稊瀣禈閹镐胶鐢婚崠鍝勭厵缂佹挾鐣婚敍鍧朼ckcooper閿涘本婀囬崝锛勵伂閺夊啫鈻夐敍澶堚偓鍌涚槨娑擃亝鏌﹀▔鏇♀偓鍛板殾婢舵矮绔撮崶銏ゆ禈
 * 閿涘牓鍣告径宥嗘煢閺€鎯у煕閺傛媽顩惄鏍电礆閿涘矂娴橀梾蹇旀煢濞夋洝鈧懐些閸旑煉绱欐禒銉︽煢濞夋洝鈧懍璐熼崷鍡楃妇閿涘鈧?
 *
 * <p>濮?INTERVAL tick 鐎靛綊娴橀崘鍛櫕娴滅尨绱版稉顓熺槰 I閿涘湧OISON_TICKS閿? 缂傛捇鈧?I閿?s閿涘绱?
 * 閻ц棄鎮曢崡鏇炲綀娣囨繃濮㈤惄顔界垼閸忓秴褰堥妴鍌涙焽缁炬寧绔婚悶鍡愨偓?/p>
 */
public final class CorruptMistManager {
	private static final List<Mist> MISTS = new ArrayList<>();

	private static final class Mist {
		final UUID casterId;
		final ServerPlayer caster;
		final double radius;
		final UUID visualId = UUID.randomUUID();
		final int durationTicks;
		final double visualExtent;
		final double[] visualParams;
		int ticksRemaining;
		final int intervalTicks;
		int ticksToNextPulse;
		final int level;
		final UUID castId;
		final int poisonTicks;
		final int slownessTicks;
        final github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast bat;
        final github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast court;
        final github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly;

		Mist(ServerPlayer caster, double radius, int durationTicks, int intervalTicks, int level, UUID castId) {
			this.casterId = caster.getUUID();
            this.bat=github.com.gengyoubo.sscfa.spell.BatSpellLink.current();
            this.court=github.com.gengyoubo.sscfa.spell.CourtSpellLink.current();
            this.anomaly=github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.current();
			this.caster = caster;
			this.radius = radius;
			this.durationTicks = durationTicks;
			this.visualExtent = Math.hypot(radius, 1);
			this.visualParams = new double[]{radius, durationTicks};
			this.ticksRemaining = durationTicks;
			this.intervalTicks = intervalTicks;
			this.ticksToNextPulse = intervalTicks;
			this.level = level;
			this.castId = castId;
			int basePoisonTicks = github.com.gengyoubo.sscfa.spell.spells.CorruptMistSpell.poisonTicks(); // 鏉╂劘顢戦弮璺烘彥閻撗嗩嚢閸欐牭绱檚pells.corrupt_mist.poison_ticks閿?
			boolean venomAffinity = castId != null && github.com.gengyoubo.sscfa.util.FormUtils.isForm(
					caster, github.com.gengyoubo.sscfa.util.FormIdentifiers.SPIDER_SALTICIDAE);
			this.poisonTicks = github.com.gengyoubo.sscfa.spell.research.RuneCastContext.current().duration((castId == null ? basePoisonTicks
					: github.com.gengyoubo.sscfa.spell.FormAffinity.curseDurationTicks(caster, basePoisonTicks))
					+ (venomAffinity ? 60 : 0),github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.NEGATIVE);
			this.slownessTicks = github.com.gengyoubo.sscfa.spell.research.RuneCastContext.current().duration(castId == null ? 40
					: github.com.gengyoubo.sscfa.spell.FormAffinity.curseDurationTicks(caster, 40),github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.NEGATIVE);
		}
	}

	private CorruptMistManager() {
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
			Iterator<Mist> it = MISTS.iterator();
			while (it.hasNext()) {
				Mist mist = it.next();
				// 閺傝姤纭堕懓鍛瀲缁?鐞氼偆些闂?閳?闂嗙偓绉烽弫锝忕礄DISCONNECT 瀹告彃鍘规惔鏇礉鏉╂瑩鍣烽梼鍙夊壈婢舵牕绱╅悽顭掔礆
				if (mist.caster.isRemoved() || !mist.caster.isAlive()) {
					ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
					it.remove();
					continue;
				}
				if (--mist.ticksRemaining <= 0) {
					ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
					it.remove();
					continue;
				}
				// Send the disk recipe once; every viewer samples the same event/tick on its client.
				ParametricVisuals.touch(mist.caster, mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST,
						mist.casterId, mist.caster.position(), true,
						mist.caster.level().getGameTime() - (mist.durationTicks - mist.ticksRemaining),
						mist.durationTicks, mist.visualExtent, mist.visualParams);
				// 濮ｅ繗鐑︾紒鎾剁暬
				if (--mist.ticksToNextPulse <= 0) {
					mist.ticksToNextPulse = mist.intervalTicks;
					pulse(mist);
				}
			}
		});
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) ->
				removeMist(handler.player.getUUID()));
	}

	/** 閺傝姤鏂?閸掗攱鏌婃稉鈧崶銏ゆ禈閿涘牆鎮撻弬鑺ョ《閼板懏妫梿鎹愵潶鐟曞棛娲婇敍澶堚偓?*/
	public static void start(ServerPlayer caster, double radius, int durationTicks,
	                         int intervalTicks, int level, UUID castId) {
		removeMist(caster.getUUID());
		MISTS.add(new Mist(caster, radius, durationTicks, intervalTicks, level, castId));
	}

	private static void removeMist(UUID casterId) {
		MISTS.removeIf(mist -> {
			if (!mist.casterId.equals(casterId)) return false;
			ParametricVisuals.stop(mist.visualId, ParametricVisuals.Kind.CORRUPT_MIST);
			return true;
		});
	}

	/** 閸楁洝鐑︾紒鎾剁暬閿涙岸娴橀崘鍛櫕娴滆桨鑵戝В?+ 缂傛捇鈧噦绱欓惂钘夋倳閸楁洖鍘ら崣妤嬬幢L4+ 娑擃厽鐦?II閿涘鈧?*/
	private static void pulse(Mist mist) {
		if (!(mist.caster.level() instanceof ServerLevel serverWorld)) {
			return;
		}
		// 妤傛鐡戠痪褎鐦ㄩ弴瀛樼箒閿涙瓈4+ 娑擃厽鐦?II閿涘牏浼?濮楁瑥宓庢潪杈剧礆
		int poisonAmplifier = mist.level >= 4 ? 1 : 0;
		// 闂嗘崘鐑︾紓鎾烩偓鐕傜窗L5 閸?II 缁狙嶇礄2026-09-17 閻劍鍩涚€规氨顭堥敍?
		int slownessAmplifier = mist.level >= 5 ? 1 : 0;
		List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class,
				mist.caster.getBoundingBox().inflate(mist.radius), e -> e != mist.caster && e.isAlive());
		for (LivingEntity target : targets) {
			if (target.distanceTo(mist.caster) > mist.radius) {
				continue;
			}
			// 妫板棗鐓欓梾鏃傤瀲閿涙氨娲伴弽鍥潶娴犺绔存０鍡楃厵婢规娊娈у鈧敍鍫滅瑢閺傝姤纭堕懓鍛瀻鐏炵偛鍞存径鏍电礆閳?娑撳秳鑵戝В鎺嶇瑝缂傛捇鈧?
			if (github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(mist.caster, target)) {
				continue;
			}
			if (WhitelistUtils.isProtected(mist.caster, target)) {
				continue;
			}
			boolean hadHarmfulEffect = github.com.gengyoubo.sscfa.spell.FormCastingStyle.hasHarmfulEffect(target);
            boolean applied=github.com.gengyoubo.sscfa.spell.CourtSpellLink.with(mist.court,()->github.com.gengyoubo.sscfa.spell.NightmareSpellLink.with(mist.caster,()->github.com.gengyoubo.sscfa.spell.BatSpellLink.with(mist.bat,()->target.addEffect(new MobEffectInstance(MobEffects.POISON,mist.poisonTicks,poisonAmplifier)))));
            boolean slowed=github.com.gengyoubo.sscfa.spell.CourtSpellLink.with(mist.court,()->github.com.gengyoubo.sscfa.spell.NightmareSpellLink.with(mist.caster,()->github.com.gengyoubo.sscfa.spell.BatSpellLink.with(mist.bat,()->target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,mist.slownessTicks,slownessAmplifier)))));
            applied|=slowed;
            if (applied || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.POISON,mist.poisonTicks,poisonAmplifier,false)
                    || github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.acceptedStatus(target,MobEffects.MOVEMENT_SLOWDOWN,mist.slownessTicks,slownessAmplifier,false))
                github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(mist.anomaly,target);
			if (applied) {
				github.com.gengyoubo.sscfa.spell.FormCastingStyle.onSpellHit(mist.caster, target,
						github.com.gengyoubo.sscfa.spell.FormationElement.CURSE, mist.castId, hadHarmfulEffect);
			}
		}
	}
}
