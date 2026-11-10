package io.github.apace100.apoli.power;

/** Apoli ModifyPotionStackPower 兼容占位。 */
public class ModifyPotionStackPower extends Power {
    private final boolean onlyWaterPotion;
    private final int count;

    public ModifyPotionStackPower(PowerType<?> type, boolean onlyWaterPotion, int count) {
        super(type);
        this.onlyWaterPotion = onlyWaterPotion;
        this.count = count;
    }

    public boolean isOnlyWaterPotion() {
        return onlyWaterPotion;
    }

    public int getCount() {
        return count;
    }
}
