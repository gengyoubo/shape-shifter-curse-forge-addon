package github.com.gengyoubo.sscfa.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** 美西螈幻形者（Forge 简化）：基础游荡 AI，完整 AI/动画待后续移植。 */
public class AxolotlShifterEntity extends ShellMob {
    public AxolotlShifterEntity(EntityType<? extends AxolotlShifterEntity> type, Level level) {
        super(type, level);
    }

    public static boolean canSpawnInWater(EntityType<?> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
    }
}
