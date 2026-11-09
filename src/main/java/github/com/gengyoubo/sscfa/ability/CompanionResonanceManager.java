package github.com.gengyoubo.sscfa.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 娴兼瑤鍗抽崗閬嶇娴笺倕顔婃晶鐐垫抄缁狅紕鎮婇崳顭掔礄jackcooper閿涘本婀囬崝锛勵伂閺夊啫鈻夐敍澶涚窗缂佹瑥鐤囬悧?閸欘剙鏁滈悧鈺傚瘯
 * 閵?2鑴崇粵澶岄獓 閺€璇插毊娴笺倕顔婇妴宥呯潣閹傛叏妤楁壆顑侀敍鍫濇祼鐎?UUID閿涘苯鎮撻惄顔界垼闁插秴顦查弬鑺ユ杹鐟曞棛娲婄仦鍌氬綌閸婄》绱氶敍?
 * 閸掔増婀￠懛顏勫З缁夊娅庨敍娑氭窗閺嶅洦顒存禍?閸楁瓕娴囬弮鍫曟鐎圭偘缍嬬仦鐐粹偓褌绔撮獮鑸电Х娴溾槄绱濋弮鐘崇暙閻ｆ瑣鈧?
 *
 * <p>閸ュ搫鐣?UUID 娣囶噣銈扮粭?+ 閸掔増婀?tick 濞撳懐鎮婇垾鏂衡偓鏂挎倱 StunEffect 閻ㄥ嫬顒濋崕澶告叏濮濓絽鍘规惔鏇熲偓婵婄熅閿?
 * 瀹曗晜婀?閺傤厾鏁搁弸浣侯伂閹懎鍠屾稉瀣畱濞堝鏆€閻㈤亶鍣告径宥嗘煢閺€鎹愵洬閻╂牔绗岀€圭偘缍嬮柌宥呯紦閼奉亞鍔у☉鍫Ｐ掗妴?/p>
 */
public final class CompanionResonanceManager {
	/** 娴笺倕顔婃晶鐐垫抄娣囶噣銈扮粭锕€娴愮€?UUID閿涘牆鎮撴稉鈧€圭偘缍嬫稉濠傜畵缁涘绱伴柌宥咁槻閺傝姤鏂佺憰鍡欐磰閺佹澘鈧》绱氶妴?*/
	public static final UUID DAMAGE_BONUS_UUID = UUID.fromString("8d2f6a34-1b5c-4e7a-9d3f-0a1b2c3d4e5f");

	private static final List<Entry> ACTIVE = new ArrayList<>();

	private static final class Entry {
		final LivingEntity target;
		int ticksRemaining;

		Entry(LivingEntity target, int durationTicks) {
			this.target = target;
			this.ticksRemaining = durationTicks;
		}
	}

	private CompanionResonanceManager() {
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
			Iterator<Entry> it = ACTIVE.iterator();
			while (it.hasNext()) {
				Entry entry = it.next();
				if (!entry.target.isAlive() || entry.target.isRemoved()) {
					removeModifier(entry.target);
					it.remove();
					continue;
				}
				if (--entry.ticksRemaining <= 0) {
					removeModifier(entry.target);
					it.remove();
				}
			}
		});
	}

	/** 閺傝棄濮?閸掗攱鏌婃导銈咁唺婢х偟娉敍鍫濇倱閻╊喗鐖ｉ弮褑顓搁弮鏈电稊鎼寸噦绱濇穱顕€銈扮粭锔芥殶閸婅壈顩惄鏍电礆閵?*/
	public static void apply(LivingEntity target, int level, int durationTicks) {
		// 閸ュ搫鐣?UUID閿涙艾鍘涚粔濠氭珟閺冄冪湴閿涘矂浼╅崗宥咁樋閺傝姤纭堕崣鐘插鐡掑懎鍤拋鎹愵吀
		removeModifier(target);
		ACTIVE.removeIf(e -> e.target == target);
		double amount = 2.0 * level;
		var attr = target.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attr == null) {
			return; // 閺冪姵鏁鹃崙璁虫縺鐎瑰啿鐫橀幀褏娈戠€圭偘缍嬮敍鍫ュ劥閸掑棜顫﹂崝銊ф晸閻椻晪绱氱捄瀹犵箖
		}
		attr.addTransientModifier(new AttributeModifier(DAMAGE_BONUS_UUID,
				"ssc_addon.companion_resonance_damage", amount,
				AttributeModifier.Operation.ADDITION));
		ACTIVE.add(new Entry(target, durationTicks));
	}

	private static void removeModifier(LivingEntity target) {
		var attr = target.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attr != null && attr.getModifier(DAMAGE_BONUS_UUID) != null) {
			attr.removeModifier(DAMAGE_BONUS_UUID);
		}
	}
    public static void removeLegacyModifier(LivingEntity target) {
        if (ACTIVE.stream().noneMatch(entry -> entry.target == target)) removeModifier(target);
    }
    public static void clearAll() {
        for (Entry entry : ACTIVE) removeModifier(entry.target);
        ACTIVE.clear();
    }

}
