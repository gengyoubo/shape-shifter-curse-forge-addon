package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * ?SSC Forge 的玩家状态变化转?Forge 事件：SSC Forge 本身不发「形态改变」「诅咒之月开始」事件，
 * 这里在服务端每个玩家 tick 末比对上一 tick 的状态，变化时发 {@link FormChangedEvent} / {@link CursedMoonEvent}? * 只在服务端主线程访问? */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class SscStateEvents {
    private record State(@Nullable ResourceLocation form, boolean cursedMoon) {}

    private static final Map<UUID, State> STATES = new HashMap<>();

    static {
        PlayerSessionCleanup.register((connection, server) -> STATES.remove(connection.player.getUUID()));
    }

    private SscStateEvents() {}

    private static State snapshot(ServerPlayer player) {
        FormDefinition form = FormManager.current(player);
        boolean cursedMoon = SscApi.currentForm(player).map(PlayerFormData::isCursedMoonApplied).orElse(false);
        return new State(form == null ? null : form.id(), cursedMoon);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.put(player.getUUID(), snapshot(player));
            github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player)) return;
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.tick(player);
        }
        State now = snapshot(player);
        State before = STATES.put(player.getUUID(), now);
        if (before == null) return;
        if (!Objects.equals(before.form(), now.form())) {
            MinecraftForge.EVENT_BUS.post(new FormChangedEvent(player, before.form(), now.form()));
        }
        if (!before.cursedMoon() && now.cursedMoon()) {
            MinecraftForge.EVENT_BUS.post(new CursedMoonEvent.Begin(player));
        } else if (before.cursedMoon() && !now.cursedMoon()) {
            MinecraftForge.EVENT_BUS.post(new CursedMoonEvent.End(player));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }
}
