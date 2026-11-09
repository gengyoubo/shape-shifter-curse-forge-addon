/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 朔望专属项链：强化九命复活（回血更多、无敌更久、复活瞬间震退并减速周围敌人）。效果在 NineLivesManager 生效?*/
public class NovaReviveNecklaceItem extends AccessoryItem {
    public NovaReviveNecklaceItem(Properties settings) {
        super(settings);
    }

    @Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
        // 朔望专属；登录装载瞬间（age==0）宽容放行，?AddonAccessoryGuard.tick 兜底卸下
        return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity,
                e -> FormUtils.isForm(e, FormIdentifiers.OCELOT_NOVA));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.translatable("item.ssc_addon.nova_revive_necklace.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.ssc_addon.nova_revive_necklace.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, world, tooltip, context);
    }
}
