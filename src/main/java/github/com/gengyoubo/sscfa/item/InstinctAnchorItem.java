package github.com.gengyoubo.sscfa.item;

import java.util.List;
import java.util.Locale;
import github.com.gengyoubo.sscfa.util.InstinctAnchors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 定形锚与解锚符共用入口，扣除和反馈只在服务端执行一次?*/
public final class InstinctAnchorItem extends Item {
    private final boolean fix;

    public InstinctAnchorItem(Properties settings, boolean fix) { super(settings); this.fix = fix; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer player)) return InteractionResultHolder.fail(stack);
        InstinctAnchors.Result result = InstinctAnchors.apply(player, fix);
        boolean success = result == InstinctAnchors.Result.FIXED || result == InstinctAnchors.Result.RELEASED;
        player.displayClientMessage(Component.translatable("message.ssc_addon.instinct_anchor." + result.name().toLowerCase(Locale.ROOT))
                .withStyle(success ? ChatFormatting.AQUA : ChatFormatting.YELLOW), true);
        if (!success) return InteractionResultHolder.fail(stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 10);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                fix ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, .7f, 1.2f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tip").withStyle(ChatFormatting.GRAY));
        if (fix) {
            tooltip.add(Component.translatable("item.ssc_addon.instinct_anchor.lifetime_tip").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.ssc_addon.instinct_anchor.condition_tip").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.ssc_addon.instinct_release.native_tip").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.ssc_addon.instinct_anchor.moon_tip").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
