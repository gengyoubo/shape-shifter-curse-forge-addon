package github.com.gengyoubo.sscfa.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 姒勬梹纭堕崡鐤叡閻?NBT 閺佺増宓佺拠璇插晸瀹搞儱鍙块敍鍧朼ckcooper閿涘鈧倸宓庢潪?NBT閿?
 * <ul>
 *   <li>{@code Spell}閿涘湯tring閿涘绱版鏃€纭?id 閻?path閿涘牆鎳￠崥宥団敄闂傚瓨浜?ssc_addon閿涘绱?/li>
 *   <li>{@code Uses}閿涘潟nt閿涘绱伴崡鏇犲娴ｈ法鏁ら崜鈺€缍戝▎鈩冩殶閿涘牊鏂侀崗銉╃摕濞夋洑鍔熼崥搴濈瑝濞戝牐鈧绱濇禒鍛枀鐎规矮鍔熼崘鍛櫏閺嬫粎娈戦懓鎰畽缂傗晜鏂佸В鏃撶礆閿?/li>
 *   <li>{@code Cd}閿涘潤ong閿涘绱伴崡鏇犲娴ｈ法鏁ら弮鍓佹畱閸愬嘲宓堢紒鎾存将娑撴牜鏅弮鍫曟？閿涘牊鏂佹稊锕€鍞存稉宥囨暏閿涘奔鍔熼崘?cd 鐎涙ê婀稊?NBT閿涘鈧?/li>
 *   <li>{@code Level}閿涘潟nt閿涘绱版鏃€纭剁粵澶岄獓閿?-5閿涘苯娴愮€规矮绗夐崣顖氬磳缁狙嶇礉閸欘亣鍏樺鈧粻杈箯瀵版娲挎妯肩搼缁狙冨祹鏉炶揪绱辩紓铏规阜 1閿涘鈧?/li>
 * </ul>
 */
public final class ScrollData {
	public static final String NBT_SPELL = "Spell";
	public static final String NBT_USES = "Uses";
	public static final String NBT_CD = "Cd";
	public static final String NBT_LEVEL = "Level";
	/** 娴ｅ酣妯侀柅澶嬨€傞弬鑺ユ杹濡楋絼缍呴敍鍫ユ▉濞?C / 鐠佲€冲灊娑?鎼?.4閿涘绱? = 閺堫亣顔曠純顕嗙礄閻劌宓庢潪瀵哥搼缁狙嶇礆閿?-5 妞?閳?Level閵?*/
	public static final String NBT_CAST_LEVEL = "CastLevel";

	/** 姒勬梹纭剁粵澶岄獓娑撳﹪妾洪妴?*/
	public static final int MAX_SPELL_LEVEL = 5;

	private ScrollData() {
	}

	/** 鐠囪褰囬崡鐤叡缂佹垵鐣鹃惃鍕摕濞夋洩绱欓弮鐘电拨鐎规碍鍨ㄩ張顏呮暈閸愬矁绻戦崶?null閿涘鈧?*/
	public static Spell getSpell(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt == null || !nbt.contains(NBT_SPELL)) {
			return null;
		}
		return SpellRegistry.get(nbt.getString(NBT_SPELL));
	}

	/** 閸撯晙缍戦崡鏇犲娴ｈ法鏁ゅ▎鈩冩殶閿涘牊婀崚婵嗩潗閸栨牗妞傞幐澶岀搼缁狙冾嚠鎼存柨鎼х拹銊ф畱娑撳﹪妾洪敍澶堚偓?*/
	public static int getUses(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(NBT_USES)) {
			return nbt.getInt(NBT_USES);
		}
		Spell spell = getSpell(stack);
		return spell == null ? 0 : spell.getRarity(getLevel(stack)).soloUses;
	}

	public static void setUses(ItemStack stack, int uses) {
		stack.getOrCreateTag().putInt(NBT_USES, Math.max(0, uses));
	}

	/** 鐠囥儱宓庢潪瀵哥搼缁狙冾嚠鎼存柨鎼х拹銊ф畱閸楁洜瀚担璺ㄦ暏濞嗏剝鏆熸稉濠囨閵?*/
	public static int getMaxUses(ItemStack stack) {
		Spell spell = getSpell(stack);
		return spell == null ? 0 : spell.getRarity(getLevel(stack)).soloUses;
	}

	/**
	 * 閼版劒绠欏В鏃撶礄0~1閿涘绱濋悽銊ょ艾鐟佸懍鍔熼崘鍛閻ㄥ嫭鏅ラ弸婊呯級閺€鎾呯窗濠娾剝顐奸弫?1閿涘牊顒滅敮闈╃礆閿涘瞼鏁ゆ潻鍥у灟閹稿澧挎担娆愮槷娓氬鈥滈崙蹇嬧偓?
	 * 缁俱垼澹婇敍鍫滅瑝閸欘垰宕熼悪顑垮▏閻劊鈧椒绗傞梽?0閿涘浜?1閿涘牅鍔熼崘鍛姬閺佸牊鐏夐敍澶堚偓?
	 */
	public static float getDurabilityRatio(ItemStack stack) {
		int max = getMaxUses(stack);
		if (max <= 0) {
			return 1.0f; // 缁俱垼澹婇幁鎺撳姬
		}
		int uses = Math.min(getUses(stack), max);
		return Math.max(0f, Math.min(1f, uses / (float) max));
	}

	/** 濞戝牐鈧?1 濞嗏€冲礋閻欘兛濞囬悽銊︻偧閺佸府绱濇潻鏂挎礀濞戝牐鈧鎮楅弰顖氭儊瀹歌尪鈧鏁栭敍鍫濈安闁库偓濮ｄ礁宓庢潪杈剧礆閵?*/
	public static boolean consumeSoloUse(ItemStack stack) {
		int uses = getUses(stack) - 1;
		setUses(stack, uses);
		return uses <= 0;
	}

	// ---- 閸楁洜瀚担璺ㄦ暏閸愬嘲宓堥敍鍫濆祹鏉炵鍤滈煬?NBT 閺冨爼妫块幋绛圭礉閸欏瞼顏稉鈧懛杈剧礆----

	public static long getCooldownEnd(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		return (nbt != null && nbt.contains(NBT_CD)) ? nbt.getLong(NBT_CD) : 0L;
	}

	public static void setCooldownEnd(ItemStack stack, long endTime) {
		stack.getOrCreateTag().putLong(NBT_CD, endTime);
	}

	public static boolean isOnCooldown(ItemStack stack, Level world) {
		return world.getGameTime() < getCooldownEnd(stack);
	}

	/** 閺勵垰鎯佹稉娲摕濞夋洖宓庢潪杈剧礄缂佹垵鐣炬禍鍡樻箒閺佸牓鐡熷▔鏇礆閵?*/
	public static boolean isScroll(ItemStack stack) {
		return getSpell(stack) != null;
	}

	/** 姒勬梹纭剁粵澶岄獓閿?-5閿涙稒妫ょ€涙顔岄幋鏍繁閻焦妞傛稉?1閿涘奔绗岄弮褍鐡ㄥ锝呭祹鏉炴潙鍚嬬€圭櫢绱氶妴?*/
	public static int getLevel(ItemStack stack) {
		Spell spell = getSpell(stack);
		int maxLevel = spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel();
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(NBT_LEVEL)) {
			return Math.max(1, Math.min(maxLevel, nbt.getInt(NBT_LEVEL)));
		}
		return 1;
	}

	/** 閸愭瑥鍙嗘鏃€纭剁粵澶岄獓閿涘牆鍞撮柈銊ヤ紣閸?閹存ê鍩勯崫浣烘晸閹存劗鏁ら敍娑氱搼缁狙冩祼鐎规矮绗夐崣顖氬磳缁狙嶇礉濮濓絽鐖跺〒鍝ュ负閺冪姴宕岀痪褔鈧柨绶為敍澶堚偓?*/
	public static void setLevel(ItemStack stack, int level) {
		Spell spell = getSpell(stack);
		int maxLevel = spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel();
		stack.getOrCreateTag().putInt(NBT_LEVEL, Math.max(1, Math.min(maxLevel, level)));
	}

	// ---- 娴ｅ酣妯侀柅澶嬨€傞弬鑺ユ杹閿涘牓妯佸▓?C / 鐠佲€冲灊娑?鎼?.4閿?----

	/** 閺傝姤鏂佸锝勭秴閿?/缂傝櫣娓?= 閻劌宓庢潪瀵哥搼缁狙嶇幢1-5 娑?閳?Level閿涘鈧?*/
	public static int getCastLevel(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(NBT_CAST_LEVEL)) {
			int cl = nbt.getInt(NBT_CAST_LEVEL);
			if (cl >= 1 && cl <= getLevel(stack)) {
				return cl;
			}
		}
		return getLevel(stack);
	}

	/** 閸愭瑥鍙嗛弬鑺ユ杹濡楋絼缍呴敍? = 闁插秶鐤嗘稉楦跨闂呭繐宓庢潪瀵哥搼缁狙嶇幢閼奉亜濮╂径鐟板煂 [0, Level]閿涘鈧?*/
	public static void setCastLevel(ItemStack stack, int castLevel) {
		int cl = Math.max(0, Math.min(getLevel(stack), castLevel));
		if (cl == 0) {
			stack.getOrCreateTag().remove(NBT_CAST_LEVEL);
		} else if (cl != getLevel(stack)) {
			stack.getOrCreateTag().putInt(NBT_CAST_LEVEL, cl);
		} else {
			stack.getOrCreateTag().remove(NBT_CAST_LEVEL);
		}
	}

	/** 閺傛澘缂撴稉鈧鐘电拨鐎规碍瀵氱€规岸鐡熷▔鏇犳畱閸楃柉閰遍敍鍫燁偧閺佺増瀵滈崫浣藉窛娑撳﹪妾洪敍娑樺讲闁瀵氱€规氨鐡戠痪褝绱?/缂傝櫣娓?1閿涘鈧?*/
	public static ItemStack create(String spellPath, int level) {
		ItemStack stack = new ItemStack(github.com.gengyoubo.sscfa.init.AddonItems.MAGIC_SCROLL.get());
		Spell spell = SpellRegistry.get(spellPath);
		stack.getOrCreateTag().putString(NBT_SPELL, spellPath);
		int lv = Math.max(1, Math.min(spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel(), level));
		stack.getOrCreateTag().putInt(NBT_LEVEL, lv);
		stack.getOrCreateTag().putInt(NBT_USES, spell == null ? 0 : spell.getRarity(lv).soloUses);
		return stack;
	}

	/** 閺傛澘缂撴稉鈧鐘电拨鐎规碍瀵氱€规岸鐡熷▔鏇犳畱閸楃柉閰遍敍? 缁狙嶇礉濞嗏剝鏆熼幐澶婃惂鐠愩劋绗傞梽鎰剁礆閵?*/
	public static ItemStack create(String spellPath) {
		return create(spellPath, 1);
	}
}
