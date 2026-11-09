package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.DomainManager;

/**
 * 婵傛垹浼?- 娑撴槒顩﹂幎鈧懗鏂ょ窗娑撳顔岄弽鍥唶閵?
 *
 * 濞堝吀缍呴敍?
 * <ol>
 *   <li>閺冪姵鐖ｇ拋?/ 閺堝绮?濮楁瑦鐖ｇ拋棰佺稻閸戝棙妲︽稉宥呮躬鐠囥儳娲伴弽?閳?閺嶅洩顔囬弬鎵窗閺嶅浄绱欐鍕剁礆閵?
 *       濞戝牐鈧?15 mana閿涙稑鍣弰鐔告￥閻㈢喓澧?閳?5 mana fizzle閿涙稖袝閸?5s CD閿涘牅绮庢＃鏍偧閿涘鈧?/li>
 *   <li>濮楁瑦鐖ｉ惃鍕倱娑撯偓閻╊喗鐖ｉ崷銊ュ櫙閺勭喍绗?閳?閸楀洨楠囨稉铏瑰閼硅绱欓幁鎰劤 + 閸戝娴傞崣顐礉1s 鐟欙箑褰傞梻鎾閿涘奔绗夌粻?CD閿涘鈧?/li>
 *   <li>瀹稿弶婀佺痪銏＄垼閿涘本妫ょ拋鍝勫櫙閺勭喎婀崫?閳?閸氼垰濮?2s 瀵洖顕遍敍灞界穿鐎靛吋鍨氶崝鐔奉嚠缁俱垺鐖ｉ惄顔界垼闁姵鍨?
 *       20% 閸撯晙缍戦悽鐔锋嚒閻喍婵€閿涘牅绗傞梽?20閿? 鐟欏棜顫庨悥鍡欏仮閿涙保D +15s閵?/li>
 * </ol>
 *
 * 閻ц棄鎮曢崡鏇窗姒涙顓婚張鍝勫煑閿涘澖@link WhitelistUtils#isProtected}閿涘绱濋惂钘夋倳閸楁洘鍨氶崨?鐞氼偊鈹嬮張宥呯枃閻椻晙绗夐懗鍊燁潶閺嶅洩顔囬妴?
 * 閸氬本妞傜粋浣诡剾閺嶅洩顔囬崝顐ｅ脯闂冧絻鎯€閿涚噦绱欑拋鎯х暰閺傚洣娆㈢拠?閺冪姵纭剁€电懓濮幒鐘绘█閽€銉ф晸閻椻晙濞囬悽?閳ユ柡鈧柨鐫橀崺铏诡攨鐞氼偄濮╅敍灞炬拱閺堢喐娈忔稉宥呬粵閹凤附鍩呴妴鍌︾礆
 */
public final class MancianimaPrimary {

	private MancianimaPrimary() {}

	private static float channelDamage(ServerPlayer caster, LivingEntity target, float remainingHealth) {
		float cap = NpcCombatEnhancements.primaryCap(caster, target, (float) BAL.d("damage_cap", DAMAGE_CAP));
		return (float) Math.max(BAL.d("damage_min", DAMAGE_MIN), Math.min(cap, remainingHealth * BAL.d("damage_percent", DAMAGE_PERCENT)));
	}

	public static final int MARK_MANA_COST = 15;
	public static final int FIZZLE_MANA_COST = 5;
	public static final String SKILL_ID = "my_addon:form_familiar_fox_mancianima_contract_mark";
	public static final int RED_TRIGGER_INTERVAL = 20;       // 1s 鐟欙箑褰傞梻鎾閿涘牓妲绘潻鐐靛仯閸楀洨瀛╅敍?
	public static final double MARK_RANGE = 32.0;
	public static final double RED_LOCK_RANGE = 24.0;
	public static final int CHANNEL_DAMAGE_TICKS = 40;       // 2s 瀵洖顕?
	public static final double DAMAGE_PERCENT = 0.20;
	public static final float DAMAGE_MIN = 2.0f;
	public static final float DAMAGE_CAP = 37.0f;
	public static final int MANA_REGEN_PAUSE_TICKS = 100;    // 5s
	private static final net.minecraft.resources.ResourceLocation MANA_REGEN_PAUSE_RES =
			new net.minecraft.resources.ResourceLocation("my_addon", "form_familiar_fox_sp_mana_regen_regen_pause_timer");
	// 闂冭埖顔?5閿涙俺绻嶇悰灞炬韫囶偆鍙庣拠璇插絿閿涘潊bilities.mancianima_primary閿涙稑鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧妯款吇鐢悂鍣洪敍?
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_primary");
	/** 鐎广垺鍩涚粩顖涘瘻闁款喛鐨熼悽銊ф畱閸忋儱褰涢妴?*/
	public static void execute(ServerPlayer player) {
		if (!isMancianima(player)) return;
		ServerLevel world = (ServerLevel) player.level();
		long now = world.getGameTime();

		// 瀵洖顕辨稉顓ㄧ吹閸愬秵顐奸幐澶愭暛 = 閸欐牗绉烽敍鍫ｎ啎鐎规碍婀弰搴ゎ嚛閿涘矂绮拋銈堫攽娑撶尨绱版稉宥呮惙鎼存棑绱辨潻娆撳櫡娣囨繃瀵旀稉宥呮惙鎼存棑绱?
		if (MancianimaMarkManager.CHANNELING.containsKey(player.getUUID())) return;

		MancianimaMarkManager.Mark mark = MancianimaMarkManager.getMark(player.getUUID());

		// 濞?3閿涙艾鍑￠張澶屽閺?閳?瀵洖顕?2s 閻喍婵€
		if (mark != null && mark.color == MancianimaMarkManager.MarkColor.RED) {
			LivingEntity tgt = findLivingByUuid(world, mark.targetUuid);
			if (tgt == null || !tgt.isAlive() || DomainManager.blocksTargeting(player, tgt)) return;
			// 闂冭埖顔岄崘宄板祱閿涙氨瀛╅弽鍥у灠閸楀洨楠囬崥搴ㄦ付缁?3s 閹靛秷鍏樺鏇犲瀻閿涘牐顕?MarkManager 娓氀冩倱 scope 閸欏倹鏆熸禒銉ょ箽閹镐礁鎮撴稉鈧崐纭风礆
			int stageGate = MancianimaMarkManager.stageGateTicks();
			if (now - mark.colorSetTick < stageGate) {
				int secLeft = (int) Math.max(1L, (long) Math.ceil((stageGate - (now - mark.colorSetTick)) / 20.0));
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.stage_locked_seconds", secLeft), true);
				playMarkFailSound(player);
				return;
			}
			// 娑撳秵绉烽懓?mana 缁斿宓嗗鈧慨瀣穿鐎电》绱盋D 閸︺劌绱╃€靛吋鍨氶崝鐔锋倵閸?15s
			MancianimaMarkManager.CHANNELING.put(player.getUUID(),
					new MancianimaMarkManager.ChannelState(mark.targetUuid, now + BAL.i("channel_damage_ticks", CHANNEL_DAMAGE_TICKS), 1));
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.channeling"), true);
			// 閸忋劌婧€娴ｅ骸鎼烽拑鍕閸★繝绂忛敍鍫ｎ唨缁楊兛绗侀弬瑙勬箒閹碘偓閹扮喓鐓￠敍?
			MancianimaMarkManager.broadcastSoundAtEntity(world, player, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.5f);
			// marker 缁変浇浜伴敍姘閺勫墽娈?閸氼垰濮?閸欏秹顩敍鍫滅矌閼奉亜绻侀懗钘夋儔鐟欎緤绱?
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
			return;
		}

		// 濞?1/2閿涙aycast 閹靛墽娲伴弽?
		LivingEntity target = raycastLiving(player, BAL.d("mark_range", MARK_RANGE));

		// 濞?2閿涙碍顭堕弽鍥╂畱閸氬奔绔撮惄顔界垼
		if (mark != null && mark.color == MancianimaMarkManager.MarkColor.ORANGE
				&& target != null && target.getUUID().equals(mark.targetUuid)) {
			// 闂冭埖顔岄崘宄板祱閿涙岸顩诲▎鈩冪垼鐠佹澘鎮楅棁鈧粵?3s 閹靛秷鍏橀崡鍥╁閿涘牐顕?MarkManager 娓氀冩倱 scope 閸欏倹鏆熸禒銉ょ箽閹镐礁鎮撴稉鈧崐纭风礆
			int stageGate = MancianimaMarkManager.stageGateTicks();
			if (now - mark.colorSetTick < stageGate) {
				int secLeft = (int) Math.max(1L, (long) Math.ceil((stageGate - (now - mark.colorSetTick)) / 20.0));
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.stage_locked_seconds", secLeft), true);
				playMarkFailSound(player);
				return;
			}
			// 1s 鐟欙箑褰傞梻鎾闁俺绻?SP_PRIMARY_CD 閺勵垰鎯?0 閺夈儲顥呭ù瀣剁窗娴ｅ棙顒濆▓鍏哥瑝鎼存梻鏁?CD 閳?閻劌宕熼悪顒冪カ濠ф劒绗夋径顏勫灊缁犳ぜ鈧?
			// 缁犫偓閸栨牭绱伴懟銉ょ瑐濞嗭紕瀛╅崡鍥ㄦ闂傚瓨婀崚甯礄閻?RED_LOCKOUT閿涘绱濋悽?upgradeToRed 閸愬懘鍎撮崚銈呯暰閵?
			boolean ok = MancianimaMarkManager.upgradeToRed(player, target);
			if (!ok) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.primary.red_locked"), true);
				return;
			}
			// 娴笺倕顔婃稉搴ㄧ叾閺?
			// 缁俱垹宕岄幋鎰 - marker 婢舵艾鐪伴崣鐘插閸欏秹顩棅绛圭礄娑擃亙姹夐崥顒冾潌閿涘本绔婚弲鐗堝絹缁€铏诡儑娴滃本顐奸崨鎴掕厬閿?
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.7f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.8f, 0.9f);
			MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.4f);
			// 閻╊喗鐖ｆ径鍕瘍楠炴寧鎸辨稉鈧稉瀣剁礉鐠佲晛褰堥崙鏄忊偓鍛挤閸涖劌娲块悳鈺侇啀閼宠棄鎯夌憴浣风癌濞堥潧宕岀痪?
			MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.4f);
			if (target instanceof ServerPlayer sp) {
				MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.0f);
				MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.WITHER_AMBIENT, 0.7f, 1.0f);
			} else {
				MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.WITHER_AMBIENT, 0.7f, 1.0f);
			}
			// 濞?2 娑撳秵绉烽懓?CD閵嗕椒绗夊☉鍫ｂ偓?mana
			return;
		}

		// 濞?1閿涙碍鐖ｇ拋鐗堟煀閻╊喗鐖ｉ敍鍫熷灗瑜版挸澧犲▽鈩冩箒娴犺缍嶉弽鍥唶閿?
		// 濡偓閺?CD閿涘牆顩ч弸婊冪秼閸撳秴婀?CD 娑擃叏绱濈粭顑跨濞堢數娲块幒銉﹀珕缂佹繐绱?
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) {
			playMarkFailSound(player);
			return;
		}

		if (target == null) {
			// 閺冪姷娲伴弽?fizzle
			int fizzleCost = BAL.i("fizzle_mana_cost", FIZZLE_MANA_COST);
			github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
			if (mana == null || mana.getMana() < fizzleCost) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.no_mana"), true);
				playMarkFailSound(player);
				return;
			}
			mana.setMana(mana.getMana() - fizzleCost);
			pauseManaRegen(player);
			playMarkFailSound(player);
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
			return;
		}

		// 閻ц棄鎮曢崡?/ 娑撳秷鍏橀弽鍥唶閼奉亜绻?
		if (target == player) { playMarkFailSound(player); return; }
		if (WhitelistUtils.isProtected(player, target)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.target_protected"), true);
			playMarkFailSound(player);
			return;
		}

		github.com.gengyoubo.sscfa.compat.ManaComponent mana = github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
		int markCost = BAL.i("mark_mana_cost", MARK_MANA_COST);
		if (mana == null || mana.getMana() < markCost) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.mancianima.no_mana"), true);
			playMarkFailSound(player);
			return;
		}
		mana.setMana(mana.getMana() - markCost);
		pauseManaRegen(player);

		MancianimaMarkManager.setMark(player, target, MancianimaMarkManager.MarkColor.YELLOW);
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);

		// 閺嶅洩顔囬幋鎰 - 娴?marker 閼奉亜绻侀懗钘夋儔閸掓壆娈戦崣宥夘洯闂婄绱欐径姘湴閸欑姴濮為敍灞筋杻瀵搫褰叉潏銊ㄧ槕鎼达讣绱?
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f, 1.8f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.6f, 1.4f);

		if (target instanceof ServerPlayer sp) {
			MancianimaMarkManager.playSoundToPlayer(sp, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 2.0f);
		} else {
			MancianimaMarkManager.broadcastSoundAtEntity(world, target, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 2.0f);
		}
	}

	/** 閻?MancianimaMarkManager 瀵洖顕?tick 閺堫偄鐔拫鍐暏閿涙碍澧界悰宀€婀℃导銈勭瑢閻栧棛鍋㈤悧瑙勬櫏閵?*/
	public static void executeChannelComplete(ServerPlayer marker, LivingEntity target) {
		if (target == null || !target.isAlive() || WhitelistUtils.isProtected(marker, target) || DomainManager.blocksTargeting(marker, target)) return;
		ServerLevel world = (ServerLevel) marker.level();
		// 閻喍婵€鐠侊紕鐣?
		float remainingHealth = target.getHealth();
		float dmg = channelDamage(marker, target, remainingHealth);
		// 娴笺倕顔婅ぐ鎺戠潣閿涙矮濞囬悽?indirectMagic 娴?marker 娑撶儤鏁鹃崙鏄忊偓鍜冪礄閻㈢喓澧挎导姘矆7楠炶泛鐢?tag bypasses_armor娑撳秷顫﹂幎銈囨暢閸戝繐鍘ら敍?
		DamageSource src = world.damageSources().indirectMagic(marker, marker);
		target.hurt(src, dmg);

		// AOE閿涙矮浜?target 娑撹桨鑵戣箛?3 閺嶇厧宕愬鍕敶閻ㄥ嫬鍙炬禒鏍晸閻椻晛褰堥崚?50% 娴笺倕顔婇敍鍫ｇ儲鏉?marker 閼奉亜绻?+ 娑撹崵娲伴弽?+ 閻ц棄鎮曢崡鏇礆
		double aoeRadius = 3.0;
		AABB aoeBox = new AABB(
				target.getX() - aoeRadius, target.getY() - aoeRadius, target.getZ() - aoeRadius,
				target.getX() + aoeRadius, target.getY() + aoeRadius, target.getZ() + aoeRadius);
		for (Entity e : world.getEntities((net.minecraft.world.entity.Entity) null, aoeBox, EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
			if (le == target || le == marker) continue;
			if (e.distanceToSqr(target) > aoeRadius * aoeRadius) continue;
			if (WhitelistUtils.isProtected(marker, le)) continue;
			// Per-recipient cap: an NPC primary target never increases the player splash.
			le.hurt(src, channelDamage(marker, le, remainingHealth) * 0.5f);
		}

		// 閻栧棛鍋㈢憴鍡氼潕閿涘牅绗夐柅鐘冲灇鐎圭偤妾导銈咁唺閿?
		double cx = target.getX(), cy = target.getY() + target.getBbHeight() / 2.0, cz = target.getZ();
		// 娑撹崵鍨庨悙闈╃礄婢堆冪€烽敍?
		world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 2, 0.3, 0.3, 0.3, 0);
		// 閻忕敻鐡婇悜鍫㈠姍閻炲喛绱欑槐顐︾拨婵傛垹浼掓稉濠氼暯閿?
		world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 80, 1.5, 1.5, 1.5, 0.15);
		world.sendParticles(ParticleTypes.SCULK_SOUL, cx, cy, cz, 30, 1.2, 1.2, 1.2, 0.1);
		world.sendParticles(ParticleTypes.DRAGON_BREATH, cx, cy, cz, 40, 1.5, 0.8, 1.5, 0.05);
		// AOE 鏉堝湱绱幐鍥┿仛閿? 閺嶇厧宕愬鍕噯閸ュ骞嗚ぐ銏㈢煈鐎涙劧绱?
		double aoeR = 3.0;
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI * 2.0 / 32.0;
			double px = cx + Math.cos(a) * aoeR;
			double pz = cz + Math.sin(a) * aoeR;
			world.sendParticles(ParticleTypes.LARGE_SMOKE, px, cy - 0.5, pz, 1, 0, 0.1, 0, 0);
			world.sendParticles(ParticleTypes.FLAME, px, cy, pz, 1, 0, 0.05, 0, 0);
		}
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
		world.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8f, 0.8f);
		// 姒瑦灏熺紙鍛寸叾閺佸牞绱版禒?marker 娑撹桨鑵戣箛鍐ㄧ畭閹绢叏绱濆〒鍙夌厠閳ユ粍鏌€閺夆偓閳ユ繃鐨靛?
		world.playSound(null, marker.getX(), marker.getY(), marker.getZ(),
				SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 0.8f);
		// 閹存劕濮?CD +15s
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(marker, SKILL_ID,
				github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.remaining(marker, SKILL_ID)
						+ github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.extra(marker, SKILL_ID, "channel_success"));
		// 閺傗晜娼冪€瑰本鍨氶敍姘乘夊?mana 閼充粙鍣?
		github.com.gengyoubo.sscfa.compat.ManaUtils.setPlayerMana(marker, github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(marker));
		// 缁俱垺鐖ｆ担鍨嚒鏉堢偓鍨?閳?濞撳懘娅庨敍鍫濆嚒闁姵鍨氭导銈咁唺閿涘矁绻橀崗銉ょ瑓娑撯偓鏉烆噯绱?
		MancianimaMarkManager.clearMark(world.getServer(), marker.getUUID());
	}

	// ============== Helpers ==============

	private static boolean isMancianima(Player player) {
		net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm((ServerPlayer) player);
		return form != null && FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id());
	}

	private static void pauseManaRegen(ServerPlayer player) {
		PowerUtils.setResourceValueAndSync(player, MANA_REGEN_PAUSE_RES, BAL.i("mana_regen_pause_ticks", MANA_REGEN_PAUSE_TICKS));
	}

	/** 閺嶅洩顔囨径杈Е閹绘劗銇氶棅绛圭礄娴?marker 閼奉亜绻侀崥顒€鍩岄敍宀€绮烘稉鈧弽鐓庣础閿?*/
	private static void playMarkFailSound(ServerPlayer player) {
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.0f);
		MancianimaMarkManager.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.6f);
	}

	/** 閸戝棙妲﹂柨銉ヨ埌缁便垺鏅敍姘躬閸撳秵鏌?5鎼达箒瀵栭崶鏉戝敶鐎电粯澹橀張鈧潻鎴犳畱閸欘垱鐖ｇ拋鎵窗閺嶅洢鈧?*/
	private static LivingEntity raycastLiving(ServerPlayer player, double maxRange) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		ServerLevel world = (ServerLevel) player.level();

		// 15鎺?闁裤儱鑸伴崡濠咁潡閻ㄥ嫪缍戝锕傛閸婄》绱檆os(7.5鎺?閿?
		double cosHalfAngle = Math.cos(Math.toRadians(7.5));

		// 闁裤儱鑸版潻婊咁伂閻愮櫢绱欓悽銊ょ艾閺嬪嫬缂撻幖婊呭偍閸栧懎娲块惄鎺炵礆
		Vec3 end = eye.add(look.scale(maxRange));
		double r = Math.max(0.5, maxRange * Math.tan(Math.toRadians(7.5)));
		AABB searchBox = new AABB(eye, end).inflate(r);

		double bestDist = Double.MAX_VALUE;
		LivingEntity best = null;

		for (Entity e : world.getEntities(player, searchBox, EntitySelector.NO_SPECTATORS)) {
			if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
			if (DomainManager.blocksTargeting(player, le)) continue;

			// 閸欐牕鐤勬担鎾茶厬閻?
			Vec3 entityCenter = le.position().add(0, le.getBbHeight() / 2.0, 0);
			Vec3 toEntity = entityCenter.subtract(eye);
			double dist = toEntity.length();
			if (dist > maxRange) continue;

			// 15鎼达箓鏁熻ぐ銏ｅ瘱閸ユ潙鍨界€规熬绱伴弬鐟版倻閸氭垿鍣烘稉搴ゎ潒缁惧灝銇欑憴?閳?7.5鎺?
			Vec3 toEntityNorm = toEntity.normalize();
			double dot = look.dot(toEntityNorm);
			if (dot < cosHalfAngle) continue;

			// 鐟欏棝鍣归柆顔藉皡濡偓閺屻儻绱版禒搴ｆ簜閻稑鍩岀€圭偘缍嬫稉顓犲仯閸嬫艾鐨犵痪鍖＄礉閻妲搁崥锕侇潶閺傜懓娼￠幐鈥虫躬鐎圭偘缍嬮崜?
			BlockHitResult blockHit = world.clip(new ClipContext(
					eye, entityCenter,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					player));
			if (blockHit.getType() != HitResult.Type.MISS) {
				double blockDistSq = eye.distanceToSqr(blockHit.getLocation());
				// 閺傜懓娼￠崨鎴掕厬閻愯鐦€圭偘缍嬫潻?閳?鐞氼偅鏌熼崸妤呬紕閹?
				if (blockDistSq + 0.5 < dist * dist) continue;
			}

			// 鐠哄疇绻冮惂钘夋倳閸?
			if (WhitelistUtils.isProtected(player, le)) continue;

			if (dist < bestDist) {
				bestDist = dist;
				best = le;
			}
		}
		return best;
	}

	private static LivingEntity findLivingByUuid(ServerLevel world, java.util.UUID uuid) {
		Entity e = world.getEntity(uuid);
		if (e instanceof LivingEntity le) return le;
		// 闁秴宸婚幍鈧張澶屾樊鎼?
		if (world.getServer() != null) {
			for (ServerLevel w : world.getServer().getAllLevels()) {
				Entity en = w.getEntity(uuid);
				if (en instanceof LivingEntity le2) return le2;
			}
		}
		return null;
	}
}
