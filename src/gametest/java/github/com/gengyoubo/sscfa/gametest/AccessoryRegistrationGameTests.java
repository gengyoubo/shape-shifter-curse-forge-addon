package github.com.gengyoubo.sscfa.gametest;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.AddonAccessoryGuard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;

import java.util.List;
import java.util.UUID;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccessoryRegistrationGameTests {
    @GameTest(template = "gametest_floor")
    public static void accessoriesEnforceFormAndLoginGrace(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ssca_accessory_test"));
        var slot = new AccessoryItem.SlotData(new ResourceLocation("curios", "ring"), 0);
        List<Item> items = List.of(AddonItems.BLUE_FIRE_AMULET.get(), AddonItems.FROST_AMULET.get(),
                AddonItems.INVISIBILITY_CLOAK.get(), AddonItems.LIFESAVING_CAT_TAIL.get(),
                AddonItems.PHANTOM_BELL.get(), AddonItems.BLOOD_GARNET.get(), AddonItems.BLOODLUST_RING.get(),
                AddonItems.HUMUS_RING.get(), AddonItems.TWIN_POD.get(), AddonItems.ACTIVE_CORAL_NECKLACE.get(),
                AddonItems.SEA_CRYSTAL_PENDANT.get(), AddonItems.WIND_SPIRIT_STAMINA_NECKLACE.get(),
                AddonItems.NOVA_REVIVE_NECKLACE.get(), AddonItems.ANUBIS_CRYSTAL.get(), AddonItems.ANKH_STONE.get(),
                AddonItems.BINDING_ANKLET.get(), AddonItems.EROSION_SAND_PRISM.get(), AddonItems.WITHERED_SAND_RING.get(),
                AddonItems.NIGHTMARE_RING.get(), AddonItems.FROST_SPINE_COLLAR.get(), AddonItems.VENOM_GLAND.get());
        for (Item item : items) {
            helper.assertTrue(item instanceof AccessoryItem, item + " must use the SSC accessory bridge");
            AccessoryItem accessory = (AccessoryItem) item;
            player.tickCount = 0;
            helper.assertTrue(accessory.canEquip(new ItemStack(item), player, slot), item + " must allow login restoration");
            player.tickCount = 21;
            helper.assertTrue(!accessory.canEquip(new ItemStack(item), player, slot), item + " must reject an unrelated form");
            helper.assertTrue(!AddonAccessoryGuard.canEquipMappedAccessory(player, accessory), item + " must be checked after login");
        }
        helper.succeed();
    }
}
