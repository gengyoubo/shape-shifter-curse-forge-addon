/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.ability.SeedEnergyEatingHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 閻庨潧瀚弫鎾诲几濠婂棙娴嗙憸鑸灪閳ь兛绶ょ槐浼村捶閵娧冭礋閻庣娉涢幃鍡欌偓鐟扮灱椤帞鈧稒鍔х槐妾唘stom_edible 閻熸瑱绠戣ぐ鍌炴儍?eatFood 婵炵繝鑳堕埢濂告晬婢跺顦ч柨娑樿嫰濞叉牜鎷?SeedEnergyEatingHandler 闁?1 闁绘劗顢婇崗姗€鏌岃箛鎾瑰珯闁规亽鍔戦崢銈嗭紣濠靛袝闁告帗銇滈埀? * 闂侇偄顦扮€?RETURN inject 闁哄嫷鍨板ú婊勭▔閻氬绐楅柛妯煎枔婢?PlayerEntity.eatFood 閻庣懓鏈崹姘跺触鎼淬垹顤呯紒鐘愁殘濠€鈥愁潰閿濆懏鍊栧☉鎾愁儜缁辫鲸绌卞┑鍫熸畬闁告鍠撴晶妤侇殰閵夆斂鍋柛?缂備胶鍠曢鎼佸矗濡搫顕ч柨娑樺缁酣宕烽妸銉у暡闂侇喓鍔岃ぐ鏃堝礉閻樼粯顎嶉悘鐐靛仱閳ь剚妲掔欢顐﹀Υ? */
@Mixin(LivingEntity.class)
public abstract class SeedEnergyEatFoodMixin {

    @Inject(method = "eat", at = @At("RETURN"))
    private void my_addon$onEatFoodReturn(Level world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClientSide) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;
        if (stack.isEmpty()) return;
        if (!SeedEnergyEatingHandler.EDIBLE_SEEDS.contains(stack.getItem())) return;
        SeedEnergyEatingHandler.onSeedEaten(sp, stack);
    }
}
