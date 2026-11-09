package io.github.apace100.apoli.power;

/** Apoli CooldownPower 兼容占位。 */
public class CooldownPower extends Power {
    private int remainingTicks;

    public CooldownPower(PowerType<?> type) {
        super(type);
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setCooldown(int ticks) {
        this.remainingTicks = Math.max(0, ticks);
    }
}
