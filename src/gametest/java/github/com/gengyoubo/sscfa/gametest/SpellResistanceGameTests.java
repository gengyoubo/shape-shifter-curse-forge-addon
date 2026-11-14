package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import github.com.gengyoubo.sscfa.enchantment.RegAddonEnchantments;
import github.com.gengyoubo.sscfa.spell.SpellDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpellResistanceGameTests {
    @GameTest(template = "gametest_floor")
    public static void enchantmentHasArmorRulesAndProtectionConflicts(GameTestHelper helper) {
        var enchantment = RegAddonEnchantments.SPELL_RESISTANCE.get();
        helper.assertTrue(enchantment.getMaxLevel() == 5 && enchantment.canEnchant(new ItemStack(Items.IRON_CHESTPLATE)),
                "spell resistance must support armor and reach level five");
        helper.assertTrue(!enchantment.canEnchant(new ItemStack(Items.IRON_SWORD)), "spell resistance must not apply to swords");
        for (var protection : new net.minecraft.world.item.enchantment.Enchantment[]{Enchantments.ALL_DAMAGE_PROTECTION,
                Enchantments.FIRE_PROTECTION, Enchantments.BLAST_PROTECTION, Enchantments.PROJECTILE_PROTECTION}) {
            helper.assertTrue(!enchantment.isCompatibleWith(protection) && !protection.isCompatibleWith(enchantment),
                    "protection enchantments must be incompatible in both orders");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void spellAndAnomalyDamageUseCappedArmorLevels(GameTestHelper helper) {
        var world = helper.getLevel();
        var spellTarget = armored(helper, 3, 3);
        helper.assertTrue(spellTarget.hurt(SpellDamageSource.of(world.damageSources()), 10), "spell damage should apply");
        helper.assertTrue(Math.abs(spellTarget.getHealth() - 16.5f) < 0.02f, "six armor levels must cap at 65 percent reduction");
        var reactionTarget = armored(helper, 2, 0);
        var registry = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var reaction = new DamageSource(registry.getHolderOrThrow(AnomalyEffects.MAGIC_DAMAGE));
        helper.assertTrue(reactionTarget.hurt(reaction, 10), "anomaly damage should apply");
        helper.assertTrue(Math.abs(reactionTarget.getHealth() - 12.6f) < 0.02f, "two levels should reduce anomaly damage by 26 percent");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void ordinaryMagicIsNotReduced(GameTestHelper helper) {
        var target = armored(helper, 5, 0);
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().magic(), 10), "ordinary magic damage should apply");
        helper.assertTrue(Math.abs(target.getHealth() - 10.0f) < 0.02f, "ordinary vanilla magic must not receive the spell reduction");
        helper.succeed();
    }

    private static Zombie armored(GameTestHelper helper, int helmetLevel, int chestLevel) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        var helmet = new ItemStack(Items.LEATHER_HELMET);
        helmet.enchant(RegAddonEnchantments.SPELL_RESISTANCE.get(), helmetLevel);
        zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
        if (chestLevel > 0) {
            var chest = new ItemStack(Items.LEATHER_CHESTPLATE);
            chest.enchant(RegAddonEnchantments.SPELL_RESISTANCE.get(), chestLevel);
            zombie.setItemSlot(EquipmentSlot.CHEST, chest);
        }
        return zombie;
    }
}
