package github.com.gengyoubo.sscfa.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Target-bound charge visuals. Casting/payment stays in SpellChannelManager.
 * Each cast has its own ID; interruption removes all pending visual state.
 * Clients receive elapsed ticks every 10 ticks and generate visuals/audio locally;
 * damage, ignition and knockback are settled only on the server. */
public final class ExplosionManager {
	public static final ResourceLocation START = new ResourceLocation("ssc_addon", "explosion_start");
	private static final Map<UUID, Sequence> SEQUENCES = new LinkedHashMap<>();
    private static final github.com.gengyoubo.sscfa.network.SnapshotAudience AUDIENCE =
            new github.com.gengyoubo.sscfa.network.SnapshotAudience();

	private static final class Sequence {
        final FormSpellStyle.Timing timing;
		final UUID id = UUID.randomUUID();
		final ServerLevel world;
		final Vec3 center;
		final UUID owner;
		/** 閽冨嫬濮忛柨姘卞仯閿涙碍鏌﹀▔鏇炵磻婵妞傞悳鈺侇啀娴ｅ秶鐤嗛敍灞肩秴缁夋槒绉?3 閺嶅吋澧﹂弬顓ㄧ礄2026-09-22 閻劍鍩涚€规氨顭堥敍灞芥倱妫板棗鐓欓敍澶堚偓?*/
		final Vec3 chargeAnchor;
		UUID refundCastId;
        AllaySpellLink.Cast allay;
        CourtSpellLink.Cast court;
        github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext anomaly;
		final long startTime;
		float power;
		// 鐠ч攱澧滆箛顐ゅ弾閿?026-09-27 鐎孤ゎ吀娣囶喖顦查敍澶涚窗700t 閽冨嫬濮忛崣顖濆厴鐠恒劍鏆熼幑顔煎瘶 reload閿涘瞼绮ㄧ粻妤佹埂閼汇儱宓嗛弮鎯邦嚢
		// ExplosionRules閿涘潌alance 閸欘垰褰夐敍澶夌窗鐠佲晛鎮撴稉鈧▎锛勫瀻閻愮澧犻崥搴濇縺鐎硅櫕澹掗悽銊ょ瑝閸氬苯寮弫鎵斥偓鏂衡偓?
		// 閺嬪嫰鈧媴绱檅eginCharge閿涘妞傞幑鏇″箯娑撯偓濞嗏槄绱漝amageBatch 缂佹挾鐣绘稉?START 閸栧懎鎮撳銉ュ弿缁嬪顕拌箛顐ゅ弾閵?
		final double coreRadius;
		final double outerRadius;
		final double igniteNear;
		final double igniteFar;
		final int fireTicksNear;
		final int fireTicksFar;
		boolean exploded;
		/** Physical elapsed tick of domain suppression; retained only for the non-damaging circle finale. */
		int suppressedAt = -1;
		/** 瀵板懐绮ㄧ粻妤冩窗閺嶅洭妲﹂崚妤嬬礄2026-09-24 娴笺倕顔婇崚鍡樺閿涘绱板鏇犲瀻閺冭泛鎻╅悡褍鍙忛柈銊ф窗閺嶅浄绱濆В?tick 閸欘亞绮ㄦ稉鈧幍鐧哥礉
		 * 闁灝鍘ょ€靛棝娉﹂崷鐑樻珯閻у彞缍戠€圭偘缍嬮崥灞芥姎鏉?damage/濮濊楠搁幒澶庢儰闁姵鍨氶張宥呭缁旑垰鐨峰畡甯礄閵嗗矂鈧姵鍨氭导銈咁唺閸氬骸宕辨稉鈧稉瀣ㄢ偓宥忕礆閵?*/
		java.util.List<LivingEntity> pendingTargets = java.util.List.of();

		Sequence(ServerPlayer owner, Vec3 center, float power, UUID refundCastId) {
			this.world = owner.serverLevel();
			this.owner = owner.getUUID();
			this.center = center;
			this.chargeAnchor = owner.position();
            var castTiming=SpellChannelManager.animationTiming(owner);
            this.timing=castTiming==null?FormSpellStyle.Timing.of(ExplosionRules.CHARGE_TICKS,ExplosionRules.CHARGE_TICKS,ExplosionRules.SOUND_START_TICKS,1):castTiming;
			this.power = power;
			this.refundCastId = refundCastId;
			this.startTime = world.getGameTime();
			// 鐠ч攱澧滈幑鏇″箯缂佹挾鐣婚崣鍌涙殶韫囶偆鍙庨敍鍫ユЩ reload 閹靛綊妫块幑銏犲棘閿涘矁顫嗙€涙顔屽▔銊╁櫞閿?
			this.coreRadius = ExplosionRules.coreRadius();
			this.outerRadius = ExplosionRules.outerRadius();
			this.igniteNear = ExplosionRules.igniteNear();
			this.igniteFar = ExplosionRules.igniteFar();
			this.fireTicksNear = ExplosionRules.fireTicksNear();
			this.fireTicksFar = ExplosionRules.fireTicksFar();
		}

		int realElapsed() { return (int)Math.max(0,world.getGameTime()-startTime); }
		int elapsed() { return timing.logical(realElapsed()); }

		/** 閺勵垰鎯佸鑼剁箻閸忋儳瀛╅惂鐣屾倖闂冭埖顔岄敍鍫氬⒑ BALL_START_TICKS閿涘绱板銈呮倵閺傝姤纭堕柨浣哥暰娑撳秴褰查幍鎾存焽閵?*/
		/** 闁夸礁鐣鹃梼鍫濃偓纭风礄2026-09-24 閻劍鍩涚€规氨顭堥敍澶涚窗娑撳顣介棅鎶筋暥鐠ч攱鎸遍敍鍦?8s / 540t閿涘宓嗛柨浣哥暰娑撳秴褰查幍鎾存焽閳ユ柡鈧?
		 * 闂婃娊顣堕崫宥堟崳 = 閺傝姤纭跺韫瑝閸欘垱灏抽崶鐑囩礉娴笺倕顔?娑撹濮╅崣鏍ㄧХ/闂€鎸庡瘻閸欐牗绉?娴ｅ秶些閸忋劑鍎存径杈ㄦ櫏閻╂潙鍩岄柌濠冩杹閵?
		 * 閸樼喍璐熺痪銏㈡閻炲啰鏁撻幋鎰剁礄682t / 34.1s閿涘绱濋悽銊﹀煕鐟曚焦鐪伴幓鎰閸掍即鐓舵０鎴ｆ崳閹绢厹鈧?*/
		boolean ballCharging() { return elapsed() >= ExplosionRules.SOUND_START_TICKS; }

		/** 閺傝姤纭堕懓鍛Ц閸氾缚绮涢崷銊╂晪閻?3 閺嶇厧鍞撮敍鍫濇倱妫板棗鐓?MAX_CAST_DISPLACEMENT閿涙稓顬囬柨姘卞仯閸楄櫕澧﹂弬顓℃惈閸旀冻绱氶妴?*/
		boolean ownerWithinAnchor(MinecraftServer server) {
			ServerPlayer player = server.getPlayerList().getPlayer(owner);
			return player != null && player.level() == world
					&& player.position().distanceToSqr(chargeAnchor) <= 3.0 * 3.0;
		}
	}

	private ExplosionManager() {}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(ExplosionManager::tick);
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) ->
                AUDIENCE.disconnect(handler.player.getUUID(), handler));
		github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> { SEQUENCES.clear(); AUDIENCE.clear(); });
	}

	public static void detonate(ServerPlayer owner, Vec3 center, float power, UUID refundCastId) {
		Sequence sequence = SEQUENCES.values().stream().filter(value -> !value.exploded && value.suppressedAt < 0
				&& value.owner.equals(owner.getUUID()) && value.world == owner.level()
				&& value.center.equals(center)).findFirst().orElse(null);
		if (sequence == null) return;
		sequence.power = power;
        sequence.allay=FormSpellStyle.currentAllay();
        sequence.court=FormSpellStyle.currentCourt();
        sequence.anomaly=github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.current();
		sequence.refundCastId = refundCastId;
		sequence.exploded = true;
		// 娴笺倕顔婇崚鍡樺閿?026-09-24 娣囶喖宕辨い鍖＄礆閿涙艾绱╅悥鍡樻閸欘亜鎻╅悡褏娲伴弽鍥у灙鐞?+ 閺嶅洩顔囧鑼瀻閿涘牐顫嬬憴?闂婃娊顣剁粩瀣祮閻㈢喐鏅ラ敍澶涚礉
		// 鐎圭偤妾导銈咁唺閻?tick() 濮?tick 缂佹挷绔撮幍鐧哥礉鐎靛棝娉﹂崷鐑樻珯娴笺倕顔?濮濊楠搁幒澶庢儰鐏忔牕鍢茬悮顐ｆ啞楠炵绱欓幀璁虫縺鐎瑰厖绗夐崣姗堢礆閵?
		double diameter = sequence.outerRadius * 2;
		sequence.pendingTargets = sequence.world.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(center, diameter, diameter, diameter),
				entity -> entity.isAlive() && !entity.isSpectator());
		syncNow(owner.getServer());
	}

	public static void beginCharge(ServerPlayer owner, Vec3 target) {
		cancelCharge(owner);
		Sequence sequence = new Sequence(owner, target, 0, null);
		SEQUENCES.put(sequence.id, sequence);
		syncNow(owner.getServer());
	}

	public static void cancelCharge(ServerPlayer owner) {
		if (SEQUENCES.values().removeIf(value -> !value.exploded && value.suppressedAt < 0 && value.owner.equals(owner.getUUID())))
			syncNow(owner.getServer());
	}

	/** Target-center coverage overrides the channel's ordinary interrupt lock, including expanding domains. */
	public static boolean suppressIfDomainCovered(ServerPlayer owner) {
		return suppress(owner,true);
	}
	/** Same inert circle finale as domain suppression, with no damage or theme restart. */
	public static boolean suppressCharge(ServerPlayer owner){return suppress(owner,false);}
	public static UUID chargingSequence(ServerPlayer owner) {
		return SEQUENCES.values().stream().filter(s->!s.exploded&&s.suppressedAt<0&&s.owner.equals(owner.getUUID())&&s.world==owner.level())
				.map(s->s.id).findFirst().orElse(null);
	}
	private static boolean suppress(ServerPlayer owner,boolean requireDomain) {
		boolean suppressed = false;
		for (Sequence sequence : SEQUENCES.values()) {
			if (sequence.exploded || sequence.suppressedAt >= 0 || !sequence.owner.equals(owner.getUUID())
					|| sequence.world != owner.level() || requireDomain&&!DomainManager.enclosedTrapping(sequence.world, sequence.center)) continue;
			sequence.suppressedAt = sequence.realElapsed();
			sequence.pendingTargets = java.util.List.of();
			suppressed = true;
		}
		if (suppressed) syncNow(owner.getServer());
		return suppressed;
	}

/** 閺傝姤纭堕懓鍛Ц閸氾缚绮涢崷銊ㄦ惈閸旀盯鏁嬮悙?3 閺嶇厧鍞撮敍鍫熸￥鏉╂稖顢戞稉顓炵碍閸掓妞傜憴鍕娑?true閿涘奔绗夐梼缁樻焽閸忚泛鐣犻弽锟犵崣閿涘鈧?
	 * 缁俱垻娅ч悶鍐ㄧ磻婵鏁撻幋鎰倵鏉╂稑鍙嗛柨浣哥暰閹緤绱?026-09-23 閻劍鍩涚€规氨顭堥敍澶涚窗娴ｅ秶些娑撳秴鍟€閹垫挻鏌囬敍灞界箑妞ゅ鍣撮弨淇扁偓?*/
	public static boolean isWithinChargeAnchor(ServerPlayer owner) {
		return SEQUENCES.values().stream()
				.filter(value -> !value.exploded && value.suppressedAt < 0 && value.owner.equals(owner.getUUID()))
				.allMatch(value -> value.ballCharging() || OcelotSpellLink.wind(owner) || value.ownerWithinAnchor(owner.getServer()));
	}

	/** 閺傝姤纭堕懓鍛畱鏉╂稖顢戞稉顓炵碍閸掓妲搁崥锕€鍑℃潻娑樺弳缁俱垻娅ч悶鍐▉濞堢绱欓埉?82t / 缁?34.1 缁夋帪绱氶敍?
	 * 闁夸礁鐣炬稉宥呭讲閹垫挻鏌囬敍鍫濇倱妫板棗鐓欐竟铏⒖瀵娀鏀ｇ€规熬绱?026-09-23 閻劍鍩涚€规氨顭堥敍澶堚偓?*/
	public static boolean isBallCharging(ServerPlayer owner) {
		return SEQUENCES.values().stream()
				.anyMatch(value -> !value.exploded && value.suppressedAt < 0 && value.owner.equals(owner.getUUID())
						&& value.ballCharging());
	}

	private static void tick(MinecraftServer server) {
		if (SEQUENCES.isEmpty()) return;
		boolean removed = false;
		var iterator = SEQUENCES.values().iterator();
		while (iterator.hasNext()) {
			Sequence sequence = iterator.next();
			if (sequence.suppressedAt >= 0) {
				if (sequence.realElapsed() - sequence.suppressedAt > ExplosionRules.AFTER_GLOW_TICKS) {
					iterator.remove();
					removed = true;
				}
				continue;
			}
			int elapsed = sequence.elapsed();
			// 閸掑棙澹掓导銈咁唺閹恒劏绻橀敍姘槨 tick 閳?DAMAGE_BATCH 娑擃亞娲伴弽鍥风礄濮ｅ繒娲伴弽鍥╃波缁犳妞傛?isAlive閿涘矁鐑︽潻鍥ㄥ闂傚瓨顒存禍?閸楁瓕娴囬敍澶堚偓?
			if (!sequence.pendingTargets.isEmpty()) damageBatch(sequence);
			// Only the paid channel's successful completion may trigger damage.
			if ((!sequence.exploded && !isChargingOwner(server, sequence))
					|| (sequence.exploded && sequence.pendingTargets.isEmpty()
					&& elapsed > ExplosionRules.EXPLODE_TICKS + ExplosionRules.AFTER_GLOW_TICKS)) {
				iterator.remove();
				removed = true;
			}
		}
		// Also send an empty snapshot when the final sequence expires.
		if (removed || server.getTickCount() % 10 == 0) syncNow(server);
	}

	private static boolean isChargingOwner(MinecraftServer server, Sequence sequence) {
		ServerPlayer owner = server.getPlayerList().getPlayer(sequence.owner);
		return owner != null && owner.isAlive() && owner.level() == sequence.world && SpellChannelManager.isCasting(owner);
	}

	/** 閸掑棙澹掓导銈咁唺濮?tick 缂佹挾鐣婚惄顔界垼閺侀绗傞梽鎰剁窗40 娑?閹电櫢绱?00+ 鐎圭偘缍嬮崷鐑樻珯 2-3 tick 閹藉﹤鐣敍鍫氬⒐150ms 閻溾晛顔嶆稉宥呭讲閹扮喓鐓￠敍澶堚偓?*/
	private static final int DAMAGE_BATCH = 40;

	/** 閸掑棙澹掔紒鎾剁暬娑撯偓閹甸€涙縺鐎圭绱?026-09-24 娣囶喖宕辨い鍖＄礆閿涙矮绮?pendingTargets 婢舵挳鍎撮崣?閳棯AMAGE_BATCH 娑擃亪鈧劒閲滅紒鎾剁暬閿?
	 * 闁槒绶稉搴″斧 damageArea 閸楁洜娲伴弽鍥х暚閸忋劋绔撮懛杈剧礄鐠烘繄顬囩悰鏉垮櫤/閺冪姴妯婇崚顐℃縺鐎?閻愬湱鍣?閸戝鈧偓閿涘绱遍幍褰掓？瀹稿弶顒存禍?閸楁瓕娴囬惃鍕窗閺?
	 * 閻?isAlive 鐠哄疇绻冮妴鍌炲櫨濞屾瑥鐭氭潻鐐插毊缂佺喕顓搁敍鍧檃stHit/killed/hitBurning閿涘娉曢幍鍦柈鐠佲槄绱濋張鈧崥搴濈閹电懓鐣幋鎰缂佺喍绔?
	 * 鐠?onSpellHit閳ユ柡鈧棁顕㈡稊澶夌瑢閸樼喆鈧苯鍙忛崷杞扮濞嗭紕绮ㄧ粻妞尖偓宥勭閼锋番鈧?*/
	private static void damageBatch(Sequence sequence) {
		ServerLevel world = sequence.world;
		Vec3 center = sequence.center;
		ServerPlayer owner = world.getServer().getPlayerList().getPlayer(sequence.owner);
		if (owner != null && owner.level() != world) owner = null;
		var source = owner == null ? SpellDamageSource.of(world.damageSources())
				: SpellDamageSource.of(world.damageSources(), owner);
		LivingEntity lastHit = null, killed = null;
		boolean hitBurning = false;
		int settle = Math.min(DAMAGE_BATCH, sequence.pendingTargets.size());
		var batch = sequence.pendingTargets.subList(0, settle);
		for (LivingEntity target : batch) {
			if (!target.isAlive() || target.isRemoved()) continue; // 閹靛綊妫垮璁抽/閸楁瓕娴囩捄瀹犵箖
			double distance = target.position().distanceTo(center);
			double factor = damageFactor(sequence, distance);
			if (factor <= 0) continue;
			boolean burning = target.getRemainingFireTicks() > 0;
			// Deliberately no ally whitelist or caster exclusion (confirmed friendly fire).
            float healthBefore=target.getHealth();
			if (!CourtSpellLink.damage(sequence.court,owner,target,()->target.hurt(source, (float) (sequence.power * factor)))) continue;
            if(owner!=null&&sequence.allay!=null)sequence.allay.damage(owner,target,Math.max(0,healthBefore-target.getHealth()));
            github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits.hit(sequence.anomaly,target);
			if (target != owner && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
				lastHit = target;
				hitBurning |= burning;
				if (!target.isAlive()) killed = target;
			}
			int fire = fireTicks(sequence, distance);
			if (fire > 0) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fire));
			Vec3 direction = target.position().subtract(center).multiply(1, 0, 1).normalize();
			double strength = knockbackStrength(sequence, distance);
			target.push(direction.x * strength, 0.35 + strength * 0.25, direction.z * strength);
			target.hurtMarked = true;
		}
		batch.clear(); // 瀹歌尙绮ㄧ粻妤冩窗閺嶅洤鍤梼鐕傜礄subList.clear 閸樼喍缍呯粔濠氭珟閿?
		if (owner != null && lastHit != null) {
			FormCastingStyle.onSpellHit(owner, killed != null ? killed : lastHit,
					FormationElement.FIRE, sequence.refundCastId, hitBurning);
		}
	}

	/** 鐠烘繄鍨庤箛?distance 閺嶈偐娈戞导銈咁唺缁粯鏆熼敍姘愁嚢 {@link Sequence} 鐠ч攱澧滆箛顐ゅ弾閿涘牓鈧槒绶崥?
	 * ExplosionRules.damageFactor閿涘矂妲?reload 閹靛綊妫块幑銏犲棘閿涘鈧柡鈧?-閺嶇绺?1.0閳?.4閿涙稒鐗宠箛?婢舵牗閮?0.1閳?.0閵?*/
	private static double damageFactor(Sequence sequence, double distance) {
		if (!Double.isFinite(distance) || distance < 0) return 0;
		if (distance <= sequence.coreRadius) {
			return 1.0 - 0.6 * (distance / sequence.coreRadius);
		}
		if (distance <= sequence.outerRadius) {
			return 0.1 * (1.0 - (distance - sequence.coreRadius) / (sequence.outerRadius - sequence.coreRadius));
		}
		return 0;
	}

	/** 鐠烘繄鍨庤箛?distance 閺嶈偐娈戦悙鍦櫑閺冨爼鏆遍敍鍧遍敍澶涚窗鐠?Sequence 鐠ч攱澧滆箛顐ゅ弾閳ユ柡鈧梹鐗宠箛?igniteNear 閸愬懎婀€閺冨爼鏆遍妴?
	 * igniteNear-igniteFar 婢舵牕婀€閺冨爼鏆遍妴浣稿従娴?0閿涘牓鈧槒绶崥?ExplosionRules.fireTicks閿涘鈧?*/
	private static int fireTicks(Sequence sequence, double distance) {
		if (distance > sequence.coreRadius && distance <= sequence.igniteNear) return sequence.fireTicksNear;
		if (distance > sequence.igniteNear && distance <= sequence.igniteFar) return sequence.fireTicksFar;
		return 0;
	}

	/** 閸戝鈧偓濮樻潙閽╁鍝勫閿涙俺顕?Sequence 鐠ч攱澧滆箛顐ゅ弾婢舵牗閮ㄩ垾鏂衡偓鏃囩箮婢?1.8 缁炬寧鈧嗏€滈崙蹇撳煂婢舵牗閮?0.2
	 * 閿涘牓鈧槒绶崥?ExplosionRules.knockbackStrength閿涘鈧?*/
	private static double knockbackStrength(Sequence sequence, double distance) {
		double t = Math.min(1.0, distance / sequence.outerRadius);
		return 1.6 * (1.0 - t) + 0.2;
	}

	private static void syncNow(MinecraftServer server) {
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (!github.com.gengyoubo.sscfa.network.AddonNetwork.canSend(viewer, START)) continue;
			var visible = SEQUENCES.values().stream().filter(sequence -> sequence.world == viewer.level()
					&& viewer.distanceToSqr(sequence.center) < ExplosionRules.audienceRange() * ExplosionRules.audienceRange()).toList();
            if (!AUDIENCE.shouldSend(viewer.getUUID(), viewer.connection, !visible.isEmpty())) continue;
			var buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
			buf.writeResourceLocation(viewer.level().dimension().location());
			buf.writeVarInt(visible.size());
			for (Sequence sequence : visible) writeSequence(buf, sequence, sequence.realElapsed());
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(viewer, START, buf);
            AUDIENCE.sent(viewer.getUUID(), viewer.connection, !visible.isEmpty());
		}
	}

	private static void writeSequence(net.minecraft.network.FriendlyByteBuf buf, Sequence sequence, int elapsed) {
		buf.writeUUID(sequence.id);
		buf.writeDouble(sequence.center.x);
		buf.writeDouble(sequence.center.y);
		buf.writeDouble(sequence.center.z);
		buf.writeVarInt(elapsed); // Duration rather than server uptime, including late joins.
		buf.writeBoolean(sequence.exploded);
		buf.writeDouble(sequence.coreRadius);
		buf.writeDouble(sequence.outerRadius);
		buf.writeVarInt(sequence.timing.originalPre());
		buf.writeVarInt(sequence.timing.preparation());
		buf.writeVarInt(sequence.suppressedAt);
	}
}
