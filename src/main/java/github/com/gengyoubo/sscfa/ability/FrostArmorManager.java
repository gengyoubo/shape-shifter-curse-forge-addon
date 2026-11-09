package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 鐎垫帗顥濋悪鎰潶閸斻劊鈧苯鐦ㄥΛ妯诲Б娴ｆ挶鈧稄绱板Λ妯兼暢 + 閸欏秴鍩￠敍鍫熸箛閸旓紕顏弶鍐ㄢ枆閿涘苯顦挎禍杞扮閼疯揪绱氶妴?
 *
 * <p><b>濡鏁?/b>閿涙碍鐦￠弽鍦箚缂佹洖鍟洪柨銉ゅ▏閸欐鍩岄惃鍕箮閹存ü婵€鐎瑰啿鍣虹亸?4%閿涘牊寮?5 閺?20%閿涘鈧柡鈧柨鍟洪柨銉︽＆閺勵垳鐓曟稊鐔告Ц閻╂拝绱?
 * 閸欐垵鐨犻崥搴ㄦЩ瀵帮繝娈㈡稊瀣╃瑓鐠哄矉绱濋弨濠氭Щ閼奉亝濡烽幏鈹库偓鍌氬暫闁裤儲鏆熺€圭偞妞傜拠?{@link FrostSpikeManager}閵?/p>
 *
 * <p><b>閸欏秴鍩?/b>閿涙俺顫﹂悽鐔哄⒖鏉╂垶鍨崨鎴掕厬閺冭绱濋弨璇插毊閼板懎褰?1 鐏炲倶鈧苯鐦ㄥΛ妯糕偓宥忕礄缂傛挻鍙?I 2.5 缁?+ 闂嗩亣濮崇划鎺戠摍閿涘绱?
 * 濠?3 鐏炲倽袝閸?b>濡鍨?/b>閿涙碍鏁鹃崙鏄忊偓鍛邦潶閸愯崵绮ㄩ敍鍦楻OST_FREEZE 1 缁夋帪绱? 閸?2 閻愮懓寮芥导銈忕礉鐏炲倹鏆熷〒鍛存祩閵?
 * 閸愬懐鐤?0.5 缁夋帡妲绘潻鐐插毊閸掑嘲鐪伴敍鍫濇倱娑撯偓閺€璇插毊閼?10 tick 閸愬懐娈戞潻鐐电敾閸涙垝鑵戦崣顏嗙暬 1 鐏炲偊绱氶妴?/p>
 *
 * <p>閻ц棄鎮曢崡鏇窗姒涙顓婚惂钘夋倳閸楁洍鈧柡鈧梻娅ч崥宥呭礋閸愬懍閲滄担鎿勭礄閻溾晛顔嶉崣濠傜枃閻?閸欘剙鏁滈悧鈺嬬礆娑撳秴褰堥崣宥呭煛娑撳孩顥濋悥鍡楀閸濆秲鈧?/p>
 */
public final class FrostArmorManager {

	// 娴犮儰绗呯敮鎼佸櫤閸у洣璐熸妯款吇閸婄》绱辨潻鎰攽閺冩湹绮?balance 韫囶偆鍙庣拠璇插絿閿涘澃cope閿涙瓫bilities.frost_armor_manager閿?
	/** 濮ｅ繑鐗撮悳顖滅搏閸愪即鏁熼惃鍕箮閹存ê鍣烘导銈嗙槷娓氬鈧?*/
	private static final float PER_THORN_REDUCTION = 0.04f;
	/** 閸欏秴鍩￠崣鐘茬湴娑撳﹪妾洪敍姘虫彧閸掓澘宓嗙憴锕€褰傚Λ妯煎瀻楠炶埖绔婚梿韬测偓?*/
	private static final int BURST_AT = 3;
	/** 鐎垫帗顥濈仦鍌涙箒閺佸牊婀￠敍鍧眎ck閿涘绱扮搾鍛閺堫亜褰旈弬鏉跨湴閸掓瑦绔婚梿鍫曞櫢鐠伮扳偓?*/
	private static final int LAYER_EXPIRE_TICKS = 50;
	/** 閸氬奔绔撮弨璇插毊閼板懍琚卞▎鈥虫嚒娑擃厼褰旂仦鍌滄畱閺堚偓鐏忓繘妫块梾鏃撶礄tick閿涘绱濋梼鑼剁箾閸戣鎻╅柅鐔峰煕濠娐扳偓?*/
	private static final int RE_LAYER_GAP_TICKS = 10;
	/** 濡鍨庨崘鑽ょ波閺冨爼鏆遍敍鍧眎ck閿涘鈧?*/
	private static final int BURST_FREEZE_TICKS = 20;
	/** 濡鍨庨崣宥勬縺閵?*/
	private static final float BURST_DAMAGE = 2.0f;

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_armor_manager");

	/** attackerUuid 閳?閸欑姴鐪伴悩鑸碘偓浣碘偓?*/
	private static final Map<UUID, Layers> LAYERS = new HashMap<>();

	private static final class Layers {
		int count;
		long lastLayerGameTime = Long.MIN_VALUE; // 娑撳﹥顐奸崣鐘茬湴閻ㄥ嫭鐖堕幋蹇旀閸?
		long lastHitGameTime = Long.MIN_VALUE;   // 娑撳﹥顐奸崨鎴掕厬閻ㄥ嫭鐖堕幋蹇旀閸掍紮绱欓梼鑼剁箾閸戝妫敍?
	}

	private FrostArmorManager() {}

	/** 閺勵垰鎯佹稉鎭掆偓宀€鏁撻悧鈺勭箮閹存ê鎳℃稉顓溾偓宥忕窗閺€璇插毊閼板懏妲稿ú璁崇秼娑撴柧婵€鐎硅櫕绨稉楦跨箮閹存閮撮敍鍫㈠负鐎硅埖娅橀弨?/ 閻㈢喓澧挎潻鎴炲灛閿涘鈧?*/
	public static boolean isMeleeHit(DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity) || !(attacker instanceof net.minecraft.world.entity.Mob)
				&& !(attacker instanceof net.minecraft.world.entity.player.Player)) {
			return false;
		}
		return source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
	}

	/**
	 * 濡鏁抽敍姘綀鐎瑰疇鈧懏妲哥€垫帗顥濋悪鎰瑬閸欐绻庨幋妯规縺鐎?閳?閹稿骞嗙紒鏇炲暫闁裤儲鏆熼崙蹇庢縺閵?
	 * 閹?LivingEntity.damage 閻?applyDamage 鐠嬪啰鏁ら悙鐧哥礄ModifyArgs閿涘绱濇稉宥呮偠娴滃娆㈤妴浣风瑝閸氱偛鍤柅鈧妴?
	 *
	 * @return 娣囶喗顒滈崥搴ｆ畱娴笺倕顔婇崐?
	 */
	public static float applyArmor(ServerPlayer victim, DamageSource source, float amount) {
		if (!isMeleeHit(source)) return amount;
		int thorns = FrostSpikeManager.getHoverCount(victim);
		if (thorns <= 0) return amount;
		float reduction = Math.min(0.2f, thorns * (float) BAL.d("per_thorn_reduction", PER_THORN_REDUCTION));
		return amount * (1.0f - reduction);
	}

	/**
	 * 閸欏秴鍩￠崗銉ュ經閿涙艾鐦ㄥΛ妯煎珓鐞氼偉绻庨幋妯烘嚒娑擃叏绱欐导銈咁唺瀹告彃鐤勯梽鍛晸閺佸牆鎮楃拫鍐暏閿涘鍟?閺€璇插毊閼板懎褰旂仦?/ 濠?3 鐏炲倹顥濋悥鍡愨偓?
	 * 閸?RETURN 濞夈劌鍙嗛柌宀冪殶閻㈩煉绱濇径鈺冨姧闁灝绱?damage 閺嶅牆鍞撮柌宥呭弳閿涙稑寮芥导銈囩病 server.execute 閸愬秴娆㈡潻鐔剁鐢冨蓟娣囨繈娅撻妴?
	 */
	public static void onFrostspineMeleeHit(ServerPlayer victim, DamageSource source) {
		if (!isMeleeHit(source)) return;
		Entity raw = source.getEntity();
		if (!(raw instanceof LivingEntity attacker) || attacker == victim) return;
		// 閻ц棄鎮曢崡鏇窗姒涙顓婚惂钘夋倳閸楁洖鍞存稉顏冪秼娑撳秴褰堥崣宥呭煛
		if (WhitelistUtils.isProtected(victim, attacker)) return;
		if (!(victim.level() instanceof ServerLevel sw)) return;
		long now = sw.getGameTime();

		Layers l = LAYERS.computeIfAbsent(attacker.getUUID(), k -> new Layers());
		// 闂冭尪绻涢崙濠氭，閿涙艾鎮撴稉鈧弨璇插毊閼?10t 閸愬懘鍣告径宥呮嚒娑擃厺绗夐崣鐘茬湴閿?
		// 閸濄劌鍙洪崚銈嗘焽閿涙岸顩婚崙璇茬箑妞ょ粯鏂佺悰宀嬬礉闂冨弶顒?now - Long.MIN_VALUE 娑撳瀛╂稉楦跨鐎佃壈鍤ч崣宥呭煛濮樻瓕绻欓弮鐘崇《閸欑姴鐪?
		if (l.lastHitGameTime != Long.MIN_VALUE && now - l.lastHitGameTime < BAL.i("re_layer_gap_ticks", RE_LAYER_GAP_TICKS)) return;
		l.lastHitGameTime = now;
		l.count++;
		l.lastLayerGameTime = now;

		if (l.count >= BAL.i("burst_at", BURST_AT)) {
			// 濡鍨庨敍姘枙缂?+ 閸欏秳婵€ + 濞撳懎鐪?
			l.count = 0;
			final int freezeTicks = BAL.i("burst_freeze_ticks", BURST_FREEZE_TICKS);
			final float burstDamage = (float) BAL.d("burst_damage", BURST_DAMAGE);
			final LivingEntity fAttacker = attacker;
			sw.getServer().execute(() -> {
				if (!fAttacker.isAlive() || fAttacker.isRemoved() || WhitelistUtils.isProtected(victim, fAttacker)) return;
				fAttacker.addEffect(new MobEffectInstance(
						github.com.gengyoubo.sscfa.init.AddonEffects.FROST_FREEZE.get(), freezeTicks, 0, false, true, true), victim);
				// 閻欘剛鐝?thorn_burst 濠ф劒绻氶悾娆忕槰濡瀚勮ぐ鎺戠潣閿涘奔绶?NPC 娴笺倕顔婇崝鐘冲灇鐠囪褰囬敍娑楃瑩鐏炵偟琚崹瀣▎濮濄垹寮介崚娲偓鎺戠秺閵?
				fAttacker.hurt(new net.minecraft.world.damagesource.DamageSource(fAttacker.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(
							net.minecraft.core.registries.Registries.DAMAGE_TYPE,
							new net.minecraft.resources.ResourceLocation("my_addon", "thorn_burst"))), victim), burstDamage);
			});
			// 濡鍨庨崣宥夘洯閿涙氨顣抽崘浼寸叾 + 闂嗩亣濮抽悥鍡氼棁缁帒鐡欓敍鍫熸暰閸戞槒鈧懎顦╅敍?
			sw.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
			sw.sendParticles(ParticleTypes.SNOWFLAKE,
					attacker.getX(), attacker.getY(0.5), attacker.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
			sw.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.7f, 1.0f);
		} else {
			// 閸欑姴鐪伴崣宥夘洯閿涙氨绱﹂幈?I 2.5s閿涘牓娈㈢仦鍌炩偓鎺戭杻閺勫墽銇氶敍? 鐏忔垿鍣洪梿顏囧С缁帒鐡?
			attacker.addEffect(new MobEffectInstance(
					net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true), victim);
			sw.sendParticles(ParticleTypes.SNOWFLAKE,
					attacker.getX(), attacker.getY(0.5), attacker.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
		}
	}

	/** 濮ｅ繑婀囬崝锛勵伂 tick閿涙俺绻冮張鐔风湴濞撳懐鎮婇敍鍫ｇТ閺冭埖婀崣鐘虫煀鐏炲倹绔婚梿璁圭礆閵嗗倹瀵?SscAddonServerEvents 娑撴牜鏅?tick閵?*/
	public static void tick(ServerLevel world) {
		if (LAYERS.isEmpty()) return;
		long now = world.getGameTime();
		Iterator<Map.Entry<UUID, Layers>> it = LAYERS.entrySet().iterator();
		while (it.hasNext()) {
			Layers l = it.next().getValue();
			// 閸濄劌鍙洪崚銈嗘焽娑?count>0 閸忓崬鎮撻幋鎰彌閹靛秵鐦潏鍐跨礉闂冨弶顒涙稉瀣娑撻缚绀嬫担鎸庣鐏?缁夊娅庡闀愮瑝鐟欙箑褰?
			if (l.count > 0 && l.lastLayerGameTime != Long.MIN_VALUE
					&& now - l.lastLayerGameTime > BAL.i("layer_expire_ticks", LAYER_EXPIRE_TICKS)) {
				l.count = 0;
			}
			// 閸忋劌鍠庣仦鍌欑瑬闂€鎸庢埂閺冪姴鎳℃稉?閳?閻╁瓨甯寸粔濠氭珟闂?map 閼躲劏鍎夐敍鍫濇倱閻炲棗鍘涢幒鎺楁珟閸濄劌鍙洪敍姘矤閺堫亜鎳℃稉顓＄箖閻ㄥ嫭娼惄顔藉瘻瀹歌尪绻冮張鐔奉槱閻炲棴绱?
			if (l.count == 0 && (l.lastHitGameTime == Long.MIN_VALUE || now - l.lastHitGameTime > 1200)) {
				it.remove();
			}
		}
	}

	/** 閺€璇插毊閼板懏鏌囩痪?/ 娑撴牜鏅崡姝屾祰濞撳懐鎮婇妴?*/
	public static void clearPlayer(UUID uuid) {
		LAYERS.remove(uuid);
	}

	/** 閺堝秴濮熼崳銊ヤ粻閺堢儤绔婚悶鍡愨偓?*/
	public static void clearAll() {
		LAYERS.clear();
	}
}
