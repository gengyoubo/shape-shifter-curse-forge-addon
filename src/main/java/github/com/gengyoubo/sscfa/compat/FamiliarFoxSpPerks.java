package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 使魔 SP / 赤狐 / 契灵与升级使魔的渗透场效果：已购买的渗透场让指定增益保留原时长、降为 I 级，
 * 而不是被形态的增益免疫清除。能力由 {@link AddonPerkTrees} 对应的使魔树授予。
 */
public final class FamiliarFoxSpPerks {
    public static final ResourceLocation FIELD_1 = addon("perk_familiar_fox_sp_permeable_field_1");
    public static final ResourceLocation FIELD_2 = addon("perk_familiar_fox_sp_permeable_field_2");
    public static final ResourceLocation UPGRADE_FIELD_1 = addon("perk_upgrade_familiar_fox_permeable_field_1");
    public static final ResourceLocation UPGRADE_FIELD_2 = addon("perk_upgrade_familiar_fox_permeable_field_2");

    private FamiliarFoxSpPerks() {}

    public static boolean isSupported(LivingEntity entity) {
        return FormUtils.isAnyForm(entity, FormIdentifiers.FAMILIAR_FOX_SP, FormIdentifiers.FAMILIAR_FOX_RED,
                FormIdentifiers.FAMILIAR_FOX_MANCIANIMA);
    }

    public static boolean preservesEffectDuration(LivingEntity entity, MobEffect effect) {
        if (!(entity instanceof Player player) || effect == null) return false;
        boolean second, first;
        if (FormUtils.isForm(player, FormIdentifiers.UPGRADE_FAMILIAR_FOX)) {
            second = ResourceBars.hasPowerId(player, UPGRADE_FIELD_2);
            first = ResourceBars.hasPowerId(player, UPGRADE_FIELD_1);
        } else if (isSupported(player)) {
            second = ResourceBars.hasPowerId(player, FIELD_2);
            first = ResourceBars.hasPowerId(player, FIELD_1);
        } else {
            return false;
        }
        if (!second && !first) return false;
        return effect == MobEffects.MOVEMENT_SPEED || effect == MobEffects.DIG_SPEED || effect == MobEffects.INVISIBILITY
                || second && (effect == MobEffects.DAMAGE_BOOST || effect == MobEffects.WATER_BREATHING
                || effect == MobEffects.NIGHT_VISION);
    }

    public static MobEffectInstance levelOneWithOriginalDuration(MobEffectInstance effect) {
        return new MobEffectInstance(effect.getEffect(), effect.getDuration(), 0, effect.isAmbient(),
                effect.isVisible(), effect.showIcon(), null, effect.getFactorData());
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
