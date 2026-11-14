package github.com.gengyoubo.sscfa.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

/** 护甲上的法术抗性：四件护甲等级相加，最高按 5 级计算。 */
public final class SpellResistanceEnchantment extends Enchantment {
    public static final int MAX_LEVEL = 5;
    public static final float REDUCTION_PER_LEVEL = 0.13f;

    public SpellResistanceEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR,
                new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override public int getMaxLevel() { return MAX_LEVEL; }
    @Override public int getMinCost(int level) { return 1 + (level - 1) * 10; }
    @Override public int getMaxCost(int level) { return super.getMinCost(level) + 50; }

    @Override protected boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.ALL_DAMAGE_PROTECTION
                && other != Enchantments.FIRE_PROTECTION && other != Enchantments.BLAST_PROTECTION
                && other != Enchantments.PROJECTILE_PROTECTION;
    }
}
