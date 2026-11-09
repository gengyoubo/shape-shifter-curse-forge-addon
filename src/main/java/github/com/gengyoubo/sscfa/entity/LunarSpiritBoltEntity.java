package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** 月灵弹（Forge 简化）：占位投射物类型。 */
public class LunarSpiritBoltEntity extends ShellProjectile {
    public LunarSpiritBoltEntity(EntityType<? extends LunarSpiritBoltEntity> type, Level level) {
        super(type, level);
    }
}
