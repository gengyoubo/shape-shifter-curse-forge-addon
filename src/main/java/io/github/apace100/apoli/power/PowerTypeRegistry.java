package io.github.apace100.apoli.power;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Apoli PowerTypeRegistry 兼容占位。 */
public final class PowerTypeRegistry {
    private static final Map<ResourceLocation, PowerType<?>> TYPES = new HashMap<>();

    private PowerTypeRegistry() {}

    public static boolean contains(ResourceLocation id) {
        return TYPES.containsKey(id);
    }

    public static PowerType<?> get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static void register(ResourceLocation id, PowerType<?> type) {
        TYPES.put(id, type);
    }
}
