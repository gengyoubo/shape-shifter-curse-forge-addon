package github.com.gengyoubo.sscfa.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 服务端玩家的 SSC 形态发生变化（主动变形、诅咒之月变形、治愈等）后，在 {@code MinecraftForge.EVENT_BUS} 上发出? * 登录时只记录初始形态，不发事件；只用于观察，不可取消? *
 * <p>形?ID ?SSC Forge 的实?ID（附属形态沿用旧路径，如 {@code my_addon:axolotl_sp}）；
 * 需要和改名后的 ID 比较时用 {@link github.com.gengyoubo.sscfa.util.FormIdMigration#canonical}?/p>
 */
public class FormChangedEvent extends PlayerEvent {
    @Nullable private final ResourceLocation previousForm;
    @Nullable private final ResourceLocation newForm;

    public FormChangedEvent(ServerPlayer player, @Nullable ResourceLocation previousForm, @Nullable ResourceLocation newForm) {
        super(player);
        this.previousForm = previousForm;
        this.newForm = newForm;
    }

    public ServerPlayer getServerPlayer() {
        return (ServerPlayer) getEntity();
    }

    @Nullable
    public ResourceLocation getPreviousForm() {
        return previousForm;
    }

    @Nullable
    public ResourceLocation getNewForm() {
        return newForm;
    }
}
