package github.com.gengyoubo.sscfa.power;

import io.github.apace100.apoli.power.CooldownPower;
import io.github.apace100.apoli.power.PowerType;

/** 虚拟图腾冷却占位实现：待图腾能力层移植后替换。 */
public class VirtualTotemPower extends CooldownPower {
    public VirtualTotemPower(PowerType<?> type) {
        super(type);
    }

    public void modify(int delta) {
        setCooldown(getRemainingTicks() + delta);
    }
}
