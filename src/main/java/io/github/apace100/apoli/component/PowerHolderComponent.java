package io.github.apace100.apoli.component;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

/** Apoli PowerHolderComponent 兼容占位：Forge 侧暂无对?Power 实例?*/
public final class PowerHolderComponent {
    public static final Key KEY = new Key();

    private PowerHolderComponent() {}

    public static final class Key {
        public Holder get(net.minecraft.world.entity.Entity player) {
            return new Holder();
        }
    }

    public static final class Holder {
        public Collection<Power> getPowers() {
            return List.of();
        }

        public boolean hasPower(PowerType<?> type) {
            return false;
        }

        public Power getPower(PowerType<?> type) {
            return null;
        }
    }

    public static <T extends Power> java.util.List<T> getPowers(net.minecraft.world.entity.Entity entity, Class<T> type) {
        return List.of();
    }

    public static void syncPower(Player player, PowerType<?> type) {
    }
}
