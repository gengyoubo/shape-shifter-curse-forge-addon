package github.com.gengyoubo.sscfa.moonmarrow;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.onixary.shapeShifterCurseForge.form.TransformManager;

import java.util.List;

/** 仅处理月髓环附带时限的红狐；剧情红狐没有到期标签，不受此逻辑影响。 */
public final class TemporaryRedForm {
    private static final String PREFIX = "ssc_addon_red_expire:";
    private TemporaryRedForm() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tick(player);
        });
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        boolean red = FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_RED);
        boolean transforming = TransformManager.isTransforming(player);
        for (String tag : List.copyOf(player.getTags())) {
            if (!tag.startsWith(PREFIX)) continue;
            final long expires;
            try { expires = Long.parseLong(tag.substring(PREFIX.length())); }
            catch (NumberFormatException invalid) { player.removeTag(tag); continue; }
            if (transforming) continue;
            if (!red) { player.removeTag(tag); continue; }
            if (player.level().getGameTime() < expires) continue;
            // Only remove the deadline once the change succeeded; retry if another system blocks it.
            if (!TransformManager.immediatelyTransform(player, MoonMarrowRing.FAMILIAR_FOX_SP)) continue;
            player.removeTag(tag);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.JUMP);
            player.displayClientMessage(Component.translatable("message.ssc_addon.red_revert_timeout").withStyle(ChatFormatting.GREEN), false);
            break;
        }
    }
}
