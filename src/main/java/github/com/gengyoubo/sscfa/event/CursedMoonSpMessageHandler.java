package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;

/**
 * 诅咒之月开始时，给 SP 形态（{@value #SPECIAL_FORM} 标记）的玩家额外提示「形态比较特殊」? * 管理员关闭了诅咒之月变形时所有人都不会变形，此时不再提示，避免误导? */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class CursedMoonSpMessageHandler {
    private static final String SPECIAL_FORM = "special_form";

    private CursedMoonSpMessageHandler() {}

    @SubscribeEvent
    public static void onCursedMoonBegin(CursedMoonEvent.Begin event) {
        ServerPlayer player = event.getServerPlayer();
        FormDefinition form = FormManager.current(player);
        if (form == null || !form.hasFlag(SPECIAL_FORM)) return;
        if (!SscCommonConfig.ENABLE_CURSED_MOON_TRANSFORM.get()) return;
        player.displayClientMessage(Component.translatable("message.ssc_addon.cursed_moon_sp_special")
                .withStyle(ChatFormatting.YELLOW), false);
    }
}
