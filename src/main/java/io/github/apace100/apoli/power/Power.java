package io.github.apace100.apoli.power;

/** Apoli Power 兼容占位。 */
public class Power {
    private final PowerType<?> type;

    public Power(PowerType<?> type) {
        this.type = type;
    }

    public PowerType<?> getType() {
        return type;
    }

    public void onRemoved() {
    }
}
