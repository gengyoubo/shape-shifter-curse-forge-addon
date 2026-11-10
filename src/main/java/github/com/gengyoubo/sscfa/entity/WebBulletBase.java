package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.ability.AddonWebBridgeAction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** 铔涗笣寮瑰熀绫伙紙Forge 鐗堬級锛氭彁渚?owner/Tier/澶╂閰嶇疆绛変緵瀛愮被澶嶇敤銆?*/
public class WebBulletBase extends ThrowableItemProjectile {
    public LivingEntity owner;
    public int Tier = 1;
    public boolean EnableTopBlockBuild = true;
    public boolean EnableVenomSpindle = true;
    public int Tier1BuffTime = 200;
    public int Tier2BuffTime = 400;
    public int Tier3BuffTime = 600;

    public static final AddonWebBridgeAction.WebLadderConfig ladderConfigTier1 =
            new AddonWebBridgeAction.WebLadderConfig(0, 4, 0, false, 0.0f);
    public static final AddonWebBridgeAction.WebLadderConfig ladderConfigTier2 =
            new AddonWebBridgeAction.WebLadderConfig(1, 6, 1, false, 0.0f);
    public static final AddonWebBridgeAction.WebLadderConfig ladderConfigTier3 =
            new AddonWebBridgeAction.WebLadderConfig(1, 8, 1, true, 0.25f);

    public WebBulletBase(EntityType<? extends WebBulletBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STRING;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
    }
}
