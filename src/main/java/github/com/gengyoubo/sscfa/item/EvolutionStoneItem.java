package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.evolution.EvolutionManager;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.moonmarrow.MoonMarrowForms;
import github.com.gengyoubo.sscfa.util.AdvancementUtils;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** 进化石的原版形态分支；进化树起点还需解锁全部分支。 */
public final class EvolutionStoneItem extends Item {
    private static final Map<ResourceLocation, ResourceLocation> TARGETS = Map.ofEntries(
            Map.entry(ssc("feral_cat_sp"), FormIdentifiers.WILD_CAT_SP),
            Map.entry(ssc("snow_fox_3"), FormIdentifiers.SNOW_FOX_SP),
            Map.entry(ssc("allay_sp"), FormIdentifiers.ALLAY_SP),
            Map.entry(ssc("anubis_wolf_3"), FormIdentifiers.GOLDEN_SANDSTORM_SP),
            Map.entry(ssc("familiar_fox_3"), FormIdentifiers.FAMILIAR_FOX_MANCIANIMA),
            Map.entry(FormIdentifiers.UPGRADE_FAMILIAR_FOX, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA),
            Map.entry(ssc("bat_3"), FormIdentifiers.BAT_PARASITIC_FRUIT),
            Map.entry(ssc("axolotl_3"), FormIdentifiers.AXOLOTL_FLUORESCENT),
            Map.entry(FormIdentifiers.UPGRADE_AXOLOTL, FormIdentifiers.AXOLOTL_FLUORESCENT),
            Map.entry(ssc("ocelot_3"), FormIdentifiers.OCELOT_NOVA),
            Map.entry(ssc("spider_3"), FormIdentifiers.SPIDER_SALTICIDAE));
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("ssc_addon", "player_form_shock");

    public EvolutionStoneItem(Properties properties) { super(properties); }

    private static ResourceLocation ssc(String path) { return new ResourceLocation("shape-shifter-curse", path); }

    @Nullable
    public static ResourceLocation targetOf(@Nullable ResourceLocation current) {
        return current == null ? null : TARGETS.get(FormIdMigration.canonical(current));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || MoonMarrowForms.isTransforming(player)) return stack;
        ResourceLocation target = targetOf(FormUtils.currentFormId(player));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.ssc_addon.evolution_stone.no_response").withStyle(ChatFormatting.RED), true);
            failSound(level, player);
            return stack;
        }
        if (!EvolutionManager.canUpgradeFoxEvolve(player)) {
            player.displayClientMessage(Component.translatable("message.ssc_addon.evolution.fail.branches_locked").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), false);
            failSound(level, player);
            return stack;
        }
        ResourceLocation registeredTarget = FormIdMigration.legacy(target);
        if (!MoonMarrowForms.isRegistered(registeredTarget)) {
            player.displayClientMessage(Component.translatable("message.ssc_addon.evolution_stone.target_not_found", target).withStyle(ChatFormatting.RED), false);
            return stack;
        }
        if (!MoonMarrowForms.startTransform(player, registeredTarget)) return stack;
        int duration = TransformManager.TRANSFORM_FX_DURATION_IN + TransformManager.TRANSFORM_FX_DURATION_OUT;
        player.addEffect(new MobEffectInstance(AddonEffects.STUN.get(), duration, 0, false, false, false));
        player.displayClientMessage(Component.translatable("message.ssc_addon.evolution_stone.success").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        AdvancementUtils.grant(player, ADVANCEMENT);
        return stack;
    }

    private static void failSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.ssc_addon.evolution_stone.tooltip";
        if (Language.getInstance().has(key)) {
            for (String line : Language.getInstance().getOrDefault(key).split("\n")) tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
