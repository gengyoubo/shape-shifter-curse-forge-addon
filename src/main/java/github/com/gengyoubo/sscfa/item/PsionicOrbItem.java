package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.evolution.EvolutionRegistry;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * 閻忎絻鍏樼€规繄褰旈敍姘矌鏉╂稑瀵茶ぐ銏♀偓渚婄礄閺佺増宓佹す鍗炲З 閳ユ柡鈧?閺屾劘绻橀崠鏍熅缁捐法娈戠挧椋庡仯瑜般垺鈧緤绱氶崣顖滄暏閵?
 *
 * <p>閸欐娊鏁梹鎸庡瘻閸氬孩澧﹀鈧妴宀勨偓澶嬪瑜般垺鈧降鈧秶鏅棃顫礄婢跺秶鏁?{@code SscaFormSelectScreen} 鏉烆剝浜村Ο鈥崇础閿涘绱濋悳鈺侇啀闁鐣炬稉鈧稉顏囩箻閸栨牕鑸伴幀浣衡€樼拋銈呮倵
 * 鐟欙箑褰傚锝呯埗閸欐闊╅崝銊ф暰濞翠胶鈻兼潪顒冧捍閸掓媽顕氳ぐ銏♀偓浣碘偓鍌氱秼閸撳秵澧嶆径鍕埌閹礁婀悾宀勬桨闁插瞼浼嗛弰鍙ョ瑝閸欘垶鈧鈧?/p>
 *
 * <p>鏉烆剝浜撮棁鈧?閳?{@code JOB_CHANGE_COST}(=3) 娑擃亣绻橀崠鏍у閻愬湱鍋ｉ弫甯礉閸婃帡鈧偓 3 娑擃亪鍣风粙瀣暥濡楋綇绱遍崗铚傜秼閺嶏繝鐛?/ 閹碉絿鍋?/ 閸欐闊?/ 濞戝牐鈧浜鹃崗?
 * 閸忋劌婀張宥呭缁?{@code EvolutionManager.startJobChange}閿涘牓妲绘担婊冪礇閿涘鈧倹婀伴柆鎾冲徔閸欘亣绀嬬拹锝冣偓宀勬毐閹稿澧﹀鈧悾宀勬桨閵嗗秷绻栨稉鈧銉ｂ偓?/p>
 */
public class PsionicOrbItem extends Item {

    public PsionicOrbItem(Properties settings) {
        super(settings);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        user.startUsingItem(hand);
        return InteractionResultHolder.consume(user.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!world.isClientSide && user instanceof ServerPlayer player) {
            net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
            ResourceLocation formId = currentForm != null ? currentForm.id() : null;
            // 娴犲懓绻橀崠鏍ц埌閹緤绱欓弻鎰箻閸栨牞鐭剧痪璺ㄦ畱鐠ч鍋ｈぐ銏♀偓渚婄礆閸欘垳鏁ら敍娑欐殶閹诡噣鈹嶉崝顭掔礉閺堫亝娼甸弬鏉款杻鏉╂稑瀵茶ぐ銏♀偓浣藉殰閸斻劍甯撮崗?
            if (formId != null && EvolutionRegistry.INSTANCE.getRouteByStartForm(formId) != null) {
                // 閺堝秴濮熺粩顖氬絺 S2C 閸栧懓顔€鐎广垺鍩涚粩顖涘ⅵ瀵偓鏉烆剝浜撮柅澶嬪閻ｅ矂娼伴敍鍫燁劃婢跺嫪绗夊☉鍫ｂ偓妤呬壕閸忓嚖绱濋悳鈺侇啀绾喛顓绘潪顒冧捍閹存劕濮涢弮鑸靛濞戝牐鈧绱?
                SscAddonNetworking.sendOpenJobChange(player);
            } else {
                player.displayClientMessage(Component.translatable("message.ssc_addon.job_change.fail.not_evolution").withStyle(ChatFormatting.RED), true);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        String key = "item.ssc_addon.psionic_orb.tooltip";
        if (Language.getInstance().has(key)) {
            for (String line : Language.getInstance().getOrDefault(key).split("\n")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
