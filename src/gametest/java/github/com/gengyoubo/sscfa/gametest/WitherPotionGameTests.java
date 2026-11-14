package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.WitherPotionItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WitherPotionGameTests {
    @GameTest(template = "gametest_floor")
    public static void drinkingAppliesWitherAndConsumesPotion(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_wither_drink"));
        var stack = new ItemStack(AddonItems.WITHER_POTION.get());
        stack.finishUsingItem(helper.getLevel(), player);
        var effect = player.getEffect(MobEffects.WITHER);
        helper.assertTrue(stack.isEmpty(), "drinking should consume the potion");
        helper.assertTrue(effect != null && effect.getAmplifier() == 1 && effect.getDuration() == 400,
                "drinking should apply Wither II for 400 ticks");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void thrownPotionsCarryWither(GameTestHelper helper) {
        var world = helper.getLevel();
        var player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "ssca_wither_throw"));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (var item : new WitherPotionItem[]{(WitherPotionItem) AddonItems.WITHER_POTION_SPLASH.get(),
                (WitherPotionItem) AddonItems.WITHER_POTION_LINGERING.get()}) {
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            item.use(world, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.isEmpty(), "throwing should consume the potion");
            var projectiles = world.getEntitiesOfClass(ThrownPotion.class, new AABB(pos).inflate(5), entity -> entity.getOwner() == player);
            helper.assertTrue(!projectiles.isEmpty(), "throwing should spawn a potion projectile");
            var projectile = projectiles.get(projectiles.size() - 1);
            var carried = projectile.getItem();
            helper.assertTrue(carried.is(item.getType() == WitherPotionItem.Type.SPLASH ? Items.SPLASH_POTION : Items.LINGERING_POTION),
                    "projectile must use the correct vanilla potion type");
            var effects = PotionUtils.getMobEffects(carried);
            helper.assertTrue(effects.stream().anyMatch(e -> e.getEffect() == MobEffects.WITHER && e.getAmplifier() == 1 && e.getDuration() == 400),
                    "projectile must carry Wither II for 400 ticks");
            projectile.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void brewingStandConvertsBothPotionTypes(GameTestHelper helper) {
        var world = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        world.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
        var stand = (BrewingStandBlockEntity) world.getBlockEntity(pos);
        helper.assertTrue(BrewingRecipeRegistry.isValidInput(new ItemStack(AddonItems.WITHER_POTION.get())), "drinkable wither potion must be a valid brewing input");
        brew(world, pos, stand, new ItemStack(AddonItems.WITHER_POTION.get()), new ItemStack(Items.GUNPOWDER));
        helper.assertTrue(stand.getItem(0).is(AddonItems.WITHER_POTION_SPLASH.get()), "gunpowder must produce splash wither potion");
        brew(world, pos, stand, stand.getItem(0).copy(), new ItemStack(Items.DRAGON_BREATH));
        helper.assertTrue(stand.getItem(0).is(AddonItems.WITHER_POTION_LINGERING.get()), "dragon breath must produce lingering wither potion");
        helper.succeed();
    }

    @GameTest(template = "gametest_floor")
    public static void inventoryStackLimitFollowsForm(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_wither_stack"));
        var slot = new Slot(player.getInventory(), 0, 0, 0);
        var potion = new ItemStack(AddonItems.WITHER_POTION.get());
        helper.assertTrue(slot.getMaxStackSize(potion) == 1, "ordinary players should have a limit of one");
        helper.assertTrue(FormManager.setForm(player, new ResourceLocation("my_addon", "anubis_wolf_sp")), "Anubis form must exist");
        helper.assertTrue(slot.getMaxStackSize(potion) == 3, "Anubis should have a limit of three");
        helper.assertTrue(FormManager.setForm(player, new ResourceLocation("my_addon", "familiar_fox_sp")), "familiar form must exist");
        helper.assertTrue(slot.getMaxStackSize(potion) == 8, "familiar fox should have a limit of eight");
        helper.succeed();
    }

    private static void brew(net.minecraft.server.level.ServerLevel world, BlockPos pos, BrewingStandBlockEntity stand,
                             ItemStack input, ItemStack ingredient) {
        stand.setItem(0, input);
        stand.setItem(3, ingredient);
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        for (int i = 0; i < 405; i++) BrewingStandBlockEntity.serverTick(world, pos, world.getBlockState(pos), stand);
    }
}
