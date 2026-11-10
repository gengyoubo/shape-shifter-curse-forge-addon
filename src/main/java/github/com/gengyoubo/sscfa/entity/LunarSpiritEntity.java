package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** 閺堝牏浼掔€圭偘缍嬮敍鍦榦rge 缁犫偓閸栨牭绱氶敍姘箽閻ｆ瑧娲伴弽鍥粓閸斻劍澧嶉棁鈧幒銉ュ經閵?*/
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

    public void ssca$summonAnomaly(Object anomaly) {}

    public void setLifeTicks(int ticks) {}

    public void setVariant(int variant) {}

    public void setFormationSlot(int slot) {}

    public void offerTarget(Entity target, int tier) {
    }
}
