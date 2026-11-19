package github.com.gengyoubo.sscfa.compat;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 使魔 SP / 赤狐 / 契灵与升级使魔的 perk 效果：渗透场让指定增益保留原时长并降为 I 级（而不是被形态的
 * 增益免疫清除）；虹吸在造成实际伤害时回复法力；药水符使用权限。能力由 {@link AddonPerkTrees} 对应的使魔树授予。
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

    public static final ResourceLocation SIPHON_1 = addon("perk_familiar_fox_sp_siphon_1");
    public static final ResourceLocation SIPHON_2 = addon("perk_familiar_fox_sp_siphon_2");
    public static final ResourceLocation UPGRADE_SIPHON_1 = addon("perk_upgrade_familiar_fox_siphon_1");
    public static final ResourceLocation UPGRADE_SIPHON_2 = addon("perk_upgrade_familiar_fox_siphon_2");
    private static final JsonObject ENEMY = enemyCondition();
    private static boolean registered;

    /** 虹吸每次实际扣血回复的法力：II 级 4，I 级 2。 */
    public static int siphonAmount(Player player) {
        if (player == null) return 0;
        if (FormUtils.isForm(player, FormIdentifiers.UPGRADE_FAMILIAR_FOX)) {
            return ResourceBars.hasPowerId(player, UPGRADE_SIPHON_2) ? 4
                    : ResourceBars.hasPowerId(player, UPGRADE_SIPHON_1) ? 2 : 0;
        }
        if (!isSupported(player)) return 0;
        return ResourceBars.hasPowerId(player, SIPHON_2) ? 4 : ResourceBars.hasPowerId(player, SIPHON_1) ? 2 : 0;
    }

    /**
     * 药水符：SSC 只允许原版使魔 3 阶使用；附属的 SP / 赤狐 / 契灵同样可用，升级使魔需要「炼金」天赋。
     * 由 {@code FamiliarFoxSpPotionCharmMixin} 在 SSC 的形态判断处调用。
     */
    public static boolean canUsePotionCharm(Player player) {
        if (player == null) return false;
        if (isSupported(player)) return true;
        return FormUtils.isForm(player, FormIdentifiers.UPGRADE_FAMILIAR_FOX)
                && RegEvolutionComponent.EVOLUTION.get(player).isUnlocked("alchemy");
    }

    /**
     * 虹吸：攻击者对敌对目标（SSC {@code shape-shifter-curse:enemy} 条件）造成的实际扣血超过 0.1 时回复法力。
     * Forge 的 {@code LivingDamageEvent} 在护甲、抗性与吸收之后、扣血之前发布，对应 Fabric 的 DamageTracker 落点；
     * 超出目标剩余生命的部分不计。
     */
    public static void registerEvents() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDamageEvent event) -> {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide || !(Math.min(event.getAmount(), victim.getHealth()) > 0.1f)
                    || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
            int refund = siphonAmount(player);
            if (refund > 0 && FormPowerRuntime.test(player, victim, ENEMY)) ManaUtils.gainPlayerMana(player, refund);
        });
    }

    private static JsonObject enemyCondition() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "shape-shifter-curse:enemy");
        return json;
    }

    public static MobEffectInstance levelOneWithOriginalDuration(MobEffectInstance effect) {
        return new MobEffectInstance(effect.getEffect(), effect.getDuration(), 0, effect.isAmbient(),
                effect.isVisible(), effect.showIcon(), null, effect.getFactorData());
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
