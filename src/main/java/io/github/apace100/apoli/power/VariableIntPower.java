package io.github.apace100.apoli.power;

/** Apoli VariableIntPower 兼容占位。 */
public class VariableIntPower extends Power {
    private int value;
    private int min;
    private int max = 100;

    public VariableIntPower(PowerType<?> type) { super(type); }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = Math.max(min, Math.min(max, value)); }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public void modify(int delta) { setValue(value + delta); }
}
