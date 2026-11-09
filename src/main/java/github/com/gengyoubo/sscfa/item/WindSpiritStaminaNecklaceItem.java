/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 风灵专属项链：加快「疾风连爪」过热后的耐力回复速度（效果在 WindSpiritClawManager 生效）?*/
public class WindSpiritStaminaNecklaceItem extends AccessoryItem {
    public WindSpiritStaminaNecklaceItem(Properties settings) {
        super(settings);
    }

    @Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isOcelotSP);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.translatable("item.ssc_addon.wind_spirit_stamina_necklace.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.ssc_addon.wind_spirit_stamina_necklace.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, world, tooltip, context);
    }
}
