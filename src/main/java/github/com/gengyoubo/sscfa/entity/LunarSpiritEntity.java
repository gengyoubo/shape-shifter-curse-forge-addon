package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** 月灵实体（Forge 简化）：保留目标联动所需接口。 */
public class LunarSpiritEntity extends ShellMob {
    public static final int TIER_OWNER_ATTACKED = 1;
    public static final int TIER_DEFENSE = 2;

    private UUID ownerUuid;

    public LunarSpiritEntity(EntityType<? extends LunarSpiritEntity> type, Level level) {
        super(type, level);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void offerTarget(Entity target, int tier) {
    }
}
