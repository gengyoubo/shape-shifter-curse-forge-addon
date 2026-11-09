package io.github.apace100.apoli.power;

import net.minecraft.resources.ResourceLocation;

/** Apoli PowerType 兼容占位。 */
public class PowerType<T extends Power> {
    private final ResourceLocation id;

    public PowerType(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getIdentifier() {
        return id;
    }
}
