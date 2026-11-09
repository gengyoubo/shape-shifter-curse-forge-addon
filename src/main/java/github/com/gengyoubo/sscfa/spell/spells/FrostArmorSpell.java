package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.FrostArmorAccess;

/**
 * 闂囨粎鏁抽張顖ょ礄閸愭壆閮撮敍宀冩憫閼规彃鐔€鎼存洩绱漥ackcooper閿涘绱扮紒娆掑殰瀹歌鲸鏌﹂崝鐘偓灞兼縺鐎瑰啿鎯涢弨韬测偓宥忕礄姒涘嫬绺鹃敍澶嗏偓鏂衡偓?
 * 閺冪姳婵€鐎规娊鐡熷▔鏇礉power 鐠囶厺绠?= 閺堢喐婀滈崥鍛婃暪閻愯鏆熼妴?
 *
 * <p>閻欘剛鐝涢棁婊呮暢閺佸牊鐏夌拋鈩冩閿涘苯鎯涢弨鑸电潨鐞涖儴鍤?{@code max(current, power)}閿涘矂鍣告径宥嗘煢閺€鍙ョ瑝閸欑姴濮為妴?
 * 缁墽鈥樼拋鏉跨秿閺堫剚纭堕張顖澦夐崗銉ф畱娴犱粙顤傞敍灞藉綀娴笺倕鍘涘☉鍫ｂ偓妤呮節閻㈣绱遍崚鐗堟埂閹存牗鏅ラ弸婊嗩潶缁夊娅庨弮璺哄涧缁夊娅庨棁婊呮暢閸撯晙缍戦惃鍕敜妫版縿鈧?
 * 娑撳秵鏁奸崘娆撳櫨閼昏鐏夌粵澶婂従娴犳牕鎯涢弨鑸垫櫏閺嬫粎娈戠粵澶岄獓閸滃本瀵旂紒顓熸闂傛番鈧?/p>
 *
 * <p>閺佹澘鈧厧顦荤純?{@code data/ssc_addon/spells/frost_armor.json}閿?
 * 閸╁搫鍣?8 閸氬憡鏁归敍? 姒涘嫬绺鹃敍? 閹镐胶鐢?20s / cd 25s 濮ｅ繒楠?-2.5s / 閼版鎽?25 濮ｅ繒楠?鑴?.2閿涘牆顦查崚鈺嬬礉娑撳孩婀€鏉堝鐦忛崪鎺旈兇 buff 缁绔撮懛杈剧礆閿?
 * L1-L5 = 閸氬憡鏁?8/12/16/20/24 閻愮櫢绱?/6/8/10/12 姒涘嫬绺鹃敍澶涚礉閼版鎽?25/30/36/43/52閿涘畱d 25/22.5/20/17.5/15s閵?
 * 闁插秴顦查弬鑺ユ杹閸掗攱鏌婇弮鍫曟毐閿涘牆鎯涢弨鍫曞櫤閸?max閿涘奔绗夐崣鐘插閿涘鈧?/p>
 */
public class FrostArmorSpell extends Spell {

	/** 閸氬憡鏁归幐浣虹敾閺冨爼妫块敍鍧眎ck閿涘绱?0s閵嗗倿绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閵?*/
	private static final int DURATION_TICKS = 400;

	// 闂冭埖顔?5閿涙俺绻嶇悰灞炬韫囶偆鍙庣拠璇插絿閿涘澃pells.frost_armor閿涙稑鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閵?
	// 閹镐胶鐢婚弮鍫曟？閸滃苯鎯涢弨鍫曞櫤閸у洣绗夐崐鐔烘暏閸樼喓澧楅崥鍛婃暪閺佸牊鐏夐惃鍕殻閺佺増銆傛担宥忕礆
	private static final BalanceReader BAL = new BalanceReader("spells.frost_armor");

	public FrostArmorSpell() {
		super(new ResourceLocation("ssc_addon", "frost_armor"), SpellRarity.BLUE);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		caster.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.FROST_ARMOR.get(),
				RuneCastContext.current().duration(BAL.i("duration_ticks", DURATION_TICKS),RuneModifiers.Stat.SHIELD_DURATION), 0));
		if (caster.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.FROST_ARMOR.get())) ((FrostArmorAccess) caster).ssca$frostArmor().grant(caster, power);
		// 濠曟柨鍤敍姘槰濮樻梻绱剁紒?+ 閸愮増娅犻惄鍓ь暢鐟佸倿鐓堕弫?
		if (caster.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(serverWorld, caster, ParticleTypes.SNOWFLAKE,
					caster.getX(), caster.getY() + 1.0, caster.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
			serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.6f);
			serverWorld.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.8f, 1.4f);
		}
	}

}
