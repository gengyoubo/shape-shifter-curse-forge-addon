package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** 法力组件兼容层：包装 Forge SSC 的法力资源。 */
public final class ManaComponent {
    private final Player player;

    private ManaComponent(Player player) {
        this.player = player;
    }

    public static ManaComponent of(Player player) {
        return new ManaComponent(player);
    }

    public double getMana() {
        return ManaUtils.getPlayerMana(player);
    }

    public double getMaxMana() {
        return ManaUtils.getPlayerMaxMana(player);
    }

    public void setMana(double value) {
        ManaUtils.setPlayerMana(player, value);
    }

    public void consumeMana(double amount) {
        ManaUtils.setPlayerMana(player, Math.max(0.0, getMana() - amount));
    }

    public void addMana(double amount) {
        ManaUtils.gainPlayerMana(player, amount);
    }
}
