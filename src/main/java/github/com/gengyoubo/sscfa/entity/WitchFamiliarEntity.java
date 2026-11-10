package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** 女巫使魔（Forge 简化）：基础游荡 AI，完整 AI/动画待后续移植。 */
public class WitchFamiliarEntity extends ShellMob {
    public WitchFamiliarEntity(EntityType<? extends WitchFamiliarEntity> type, Level level) {
        super(type, level);
    }
}
