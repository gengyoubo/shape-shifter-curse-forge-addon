package github.com.gengyoubo.sscfa.power;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 寄生果种子能力占位实现：待 Apoli power 层移植后替换。 */
public final class ParasiticFruitSeedPower {
    private ParasiticFruitSeedPower() {}

    public static boolean isValidSeedHost(Player caster, Entity entity) {
        return false;
    }

    public static void plantSeedSpread(Player caster, LivingEntity target, boolean first) {
    }
}
