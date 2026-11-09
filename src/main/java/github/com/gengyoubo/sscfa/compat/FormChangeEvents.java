package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.util.SscEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** 形态变化事件：轮询 FormManager 并在形态切换时回调。 */
public final class FormChangeEvents {
    public interface Listener {
        void onChange(ServerPlayer player, FormDefinition previous, FormDefinition current);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, ResourceLocation> LAST = new ConcurrentHashMap<>();
    private static boolean initialized;

    private FormChangeEvents() {}

    public static void register(Listener listener) {
        ensureInit();
        LISTENERS.add(listener);
    }

    private static void ensureInit() {
        if (initialized) return;
        initialized = true;
        SscEvents.onServerTick(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ResourceLocation current = FormManager.current(player).id();
                ResourceLocation previous = LAST.put(player.getUUID(), current);
                if (previous != null && !previous.equals(current)) {
                    FormDefinition previousDef = FormRegistry.get(previous);
                    FormDefinition currentDef = FormRegistry.get(current);
                    for (Listener listener : LISTENERS) {
                        listener.onChange(player, previousDef, currentDef);
                    }
                }
            }
        });
    }
}
