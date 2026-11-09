package github.com.gengyoubo.sscfa.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 魔法卷轴??NBT 数据读写工具（jackcooper）。卷?NBT??
 * <ul>
 *   <li>{@code Spell}（String）：魔??id ??path（命名空间??ssc_addon）??/li>
 *   <li>{@code Uses}（int）：单独使用剩余次数（放入魔法书后不消耗，仅决定书内效果的耐久缩放比）??/li>
 *   <li>{@code Cd}（long）：单独使用时的冷却结束世界时间（放书内不用，书?cd 存在?NBT）??/li>
 *   <li>{@code Level}（int）：魔法等级??-5，固定不可升级，只能开箱获得更高等级卷轴；缺省 1）??/li>
 * </ul>
 */
public final class ScrollData {
	public static final String NBT_SPELL = "Spell";
	public static final String NBT_USES = "Uses";
	public static final String NBT_CD = "Cd";
	public static final String NBT_LEVEL = "Level";
	/** 低阶选档施放档位（阶??C / 计划????.4）?? = 未设置（用卷轴等级）??-5 ????Level??*/
	public static final String NBT_CAST_LEVEL = "CastLevel";

	/** 魔法等级上限?*/
	public static final int MAX_SPELL_LEVEL = 5;

	private ScrollData() {
	}

	/** 读取卷轴绑定的魔法（无绑定或未注册返?null）??*/
	public static Spell getSpell(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt == null || !nbt.contains(NBT_SPELL)) {
			return null;
		}
		return SpellRegistry.get(nbt.getString(NBT_SPELL));
	}

	/** 剩余单独使用次数（未初始化时按等级对应品质的上限）?*/
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

	/** 该卷轴等级对应品质的单独使用次数上限??*/
	public static int getMaxUses(ItemStack stack) {
		Spell spell = getSpell(stack);
		return spell == null ? 0 : spell.getRarity(getLevel(stack)).soloUses;
	}

	/**
	 * 耐久比（0~1），用于装书内时的效果缩放：满次?1（正常），用过则按剩余比例衰减?
	 * 红色（不可单独使用、上?0）??1（书内满效果）?
	 */
	public static float getDurabilityRatio(ItemStack stack) {
		int max = getMaxUses(stack);
		if (max <= 0) {
			return 1.0f; // 红色恒满
		}
		int uses = Math.min(getUses(stack), max);
		return Math.max(0f, Math.min(1f, uses / (float) max));
	}

	/** 消??1 次单独使用次数，返回消耗后是否已耗尽（应销毁卷轴）??*/
	public static boolean consumeSoloUse(ItemStack stack) {
		int uses = getUses(stack) - 1;
		setUses(stack, uses);
		return uses <= 0;
	}

	// ---- 单独使用冷却（卷轴自?NBT 时间戳，双端一致）----

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

	/** 是否为魔法卷轴（绑定了有效魔法）??*/
	public static boolean isScroll(ItemStack stack) {
		return getSpell(stack) != null;
	}

	/** 魔法等级??-5；无字段或缺省时?1，与旧存档卷轴兼容）?*/
	public static int getLevel(ItemStack stack) {
		Spell spell = getSpell(stack);
		int maxLevel = spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel();
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(NBT_LEVEL)) {
			return Math.max(1, Math.min(maxLevel, nbt.getInt(NBT_LEVEL)));
		}
		return 1;
	}

	/** 写入魔法等级（内部工??战利品生成用；等级固定不可升级，正常游玩无升级途径）?*/
	public static void setLevel(ItemStack stack, int level) {
		Spell spell = getSpell(stack);
		int maxLevel = spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel();
		stack.getOrCreateTag().putInt(NBT_LEVEL, Math.max(1, Math.min(maxLevel, level)));
	}

	// ---- 低阶选档施放（阶?C / 计划????.4??----

	/** 施放档位??/缺??= 用卷轴等级；1-5 ????Level）??*/
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

	/** 写入施放档位? = 重置为跟随卷轴等级；自动夹到 [0, Level]）??*/
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

	/** 新建一张绑定指定魔法的卷轴（次数按品质上限；可选指定等级??/缺??1）??*/
	public static ItemStack create(String spellPath, int level) {
		ItemStack stack = new ItemStack(github.com.gengyoubo.sscfa.init.AddonItems.MAGIC_SCROLL.get());
		Spell spell = SpellRegistry.get(spellPath);
		stack.getOrCreateTag().putString(NBT_SPELL, spellPath);
		int lv = Math.max(1, Math.min(spell == null ? MAX_SPELL_LEVEL : spell.getMaxLevel(), level));
		stack.getOrCreateTag().putInt(NBT_LEVEL, lv);
		stack.getOrCreateTag().putInt(NBT_USES, spell == null ? 0 : spell.getRarity(lv).soloUses);
		return stack;
	}

	/** 新建一张绑定指定魔法的卷轴? 级，次数按品质上限）??*/
	public static ItemStack create(String spellPath) {
		return create(spellPath, 1);
	}
}
