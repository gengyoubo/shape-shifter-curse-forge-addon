package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import github.com.gengyoubo.sscfa.spell.SpiderSpellLink;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.AllaySpellLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

/** 目标ID由原版DataTracker同步；双端转向，不额外每tick发包、不扫描实体。 */
@Mixin(Projectile.class)
public abstract class SpellHomingProjectileMixin implements FormSpellStyle.HomingProjectile, SpiderSpellLink.WebProjectile, AllaySpellLink.ProjectileData, github.com.gengyoubo.sscfa.spell.BatSpellLink.ProjectileData, github.com.gengyoubo.sscfa.spell.CourtSpellLink.ProjectileData {
    @Unique private github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast ssca$court;
    @Override public github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast ssca$court(){return ssca$court;}
    @Override public void ssca$court(github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast cast){ssca$court=cast;}
    @Unique private github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast ssca$bat;
    @Override public github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast ssca$bat(){return ssca$bat;}
    @Override public void ssca$bat(github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast cast){ssca$bat=cast;}
    @Inject(method="setOwner",at=@At("TAIL"))
    private void ssca$bindBat(net.minecraft.world.entity.Entity owner,CallbackInfo ci){
        var court=github.com.gengyoubo.sscfa.spell.CourtSpellLink.current();
        if(owner!=null&&court!=null&&owner.getUUID().equals(court.owner))ssca$court=court;
        var cast=github.com.gengyoubo.sscfa.spell.BatSpellLink.current();
        if(owner!=null&&cast!=null&&owner.getUUID().equals(cast.owner))ssca$bat=cast;
    }
    @Unique private AllaySpellLink.Cast ssca$allay;
    @Override public AllaySpellLink.Cast ssca$allay(){return ssca$allay;}
    @Override public void ssca$allay(AllaySpellLink.Cast cast){ssca$allay=cast;}
    @Unique private final SpiderSpellLink.Impact ssca$weave=new SpiderSpellLink.Impact();
    @Override public SpiderSpellLink.Impact ssca$weave(){return ssca$weave;}
    @Unique private static final EntityDataAccessor<Integer> SSCA_TARGET=
            SynchedEntityData.defineId(Projectile.class,EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Float> SSCA_FLIGHT_SPEED=
            SynchedEntityData.defineId(Projectile.class,EntityDataSerializers.FLOAT);
    @Unique private static final EntityDataAccessor<Float> SSCA_LAUNCH_SPEED=
            SynchedEntityData.defineId(Projectile.class,EntityDataSerializers.FLOAT);
    @Unique private UUID ssca$target;
    @Unique private Vec3 ssca$previous;
    @Unique private double ssca$remaining;

    @Inject(method="<init>",at=@At("TAIL"))
    private void ssca$init(EntityType<? extends Projectile> type,Level world,CallbackInfo ci) {
        var data=((Projectile)(Object)this).getEntityData();
        data.define(SSCA_TARGET,-1);data.define(SSCA_FLIGHT_SPEED,1f);data.define(SSCA_LAUNCH_SPEED,0f);
    }
    @Override public void ssca$flight(double speedMultiplier,double maximumRange) {
        var self=(Projectile)(Object)this;
        if(self.level().isClientSide||ssca$previous!=null)return;
        self.getEntityData().set(SSCA_FLIGHT_SPEED,(float)speedMultiplier);
        self.setDeltaMovement(self.getDeltaMovement().scale(speedMultiplier));
        self.getEntityData().set(SSCA_LAUNCH_SPEED,(float)self.getDeltaMovement().length());
        ssca$remaining=maximumRange;ssca$previous=self.position();
    }
    @Override public double ssca$flightSpeedMultiplier() {
        return ((Projectile)(Object)this).getEntityData().get(SSCA_FLIGHT_SPEED);
    }
    @Override public void ssca$syncFlightVelocity() {
        var self=(Projectile)(Object)this;
        if(ssca$flightSpeedMultiplier()==1||!ssca$linear(self))return;
        double speed=self.getEntityData().get(SSCA_LAUNCH_SPEED);
        if(speed>0&&self.getDeltaMovement().lengthSqr()>1e-12)self.setDeltaMovement(self.getDeltaMovement().normalize().scale(speed));
    }
    @Override public void ssca$home(LivingEntity target,double range) {
        var self=(Projectile)(Object)this;
        if(ssca$previous==null){ssca$remaining=range;ssca$previous=self.position();}
        ssca$target=target.getUUID();
        self.getEntityData().set(SSCA_TARGET,target.getId());
    }
    @Override public boolean ssca$isHoming() {
        return ((Projectile)(Object)this).getEntityData().get(SSCA_TARGET)>=0;
    }
    @Override public boolean ssca$hasRangeLimit() {return ssca$previous!=null;}
    @Override public Vec3 ssca$guidedVelocity(Vec3 ordinary) {
        var self=(Projectile)(Object)this;
        var entity=self.level().getEntity(self.getEntityData().get(SSCA_TARGET));
        if(entity instanceof LivingEntity target&&target.isAlive()) {
            Vec3 delta=target.getBoundingBox().getCenter().subtract(self.position());
            if(delta.lengthSqr()>=1e-8)ordinary=delta.normalize().scale(ordinary.length());
        }
        if(!self.level().isClientSide&&ssca$previous!=null&&ordinary.length()>ssca$remaining)
            ordinary=ordinary.normalize().scale(Math.max(0,ssca$remaining));
        return ordinary;
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void ssca$tick(CallbackInfo ci) {
        var self=(Projectile)(Object)this;
        // 速度包按原版量化且限幅；匀速法术从同步快照恢复真实速度，不让高速配置在客机变慢。
        if(self.level().isClientSide)ssca$syncFlightVelocity();
        if(!self.level().isClientSide&&ssca$previous!=null) {
            ssca$remaining-=self.position().distanceTo(ssca$previous);ssca$previous=self.position();
            if(ssca$remaining<=1e-7){self.discard();return;}
            var entity=ssca$target==null?null:((ServerLevel)self.level()).getEntity(ssca$target);
            if(entity instanceof LivingEntity target&&self.getOwner() instanceof ServerPlayer owner
                    &&FormSpellStyle.legalTarget(owner,target))self.getEntityData().set(SSCA_TARGET,target.getId());
            else {ssca$target=null;self.getEntityData().set(SSCA_TARGET,-1);}
        }
        if(!ssca$isHoming()&&ssca$previous==null)return;
        Vec3 velocity=ssca$isHoming()?ssca$guidedVelocity(self.getDeltaMovement()):self.getDeltaMovement();
        // 临近射程终点只走剩余距离，避免高速弹体多飞一整tick。
        if(!self.level().isClientSide&&ssca$previous!=null&&velocity.length()>ssca$remaining)
            velocity=velocity.normalize().scale(ssca$remaining);
        self.setDeltaMovement(velocity);
        double horizontal=Math.sqrt(velocity.x*velocity.x+velocity.z*velocity.z);
        self.setYRot((float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z)));
        self.setXRot((float)Math.toDegrees(Math.atan2(-velocity.y,horizontal)));
    }
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void ssca$write(CompoundTag nbt,CallbackInfo ci) {
        ssca$weave.write(nbt);
        if(ssca$allay!=null)nbt.put("SscaAllayLink",ssca$allay.write());
        if(ssca$bat!=null)nbt.put("SscaBatLink",ssca$bat.write());
        if(ssca$court!=null)nbt.put("SscaCourtLink",ssca$court.write());
        if(ssca$previous==null)return;
        var self=(Projectile)(Object)this;
        // 本tick移动在super.tick之后发生，存档时也要结清，防反复卸载区块延长射程。
        nbt.putDouble("SscaHomingRemaining",Math.max(0,ssca$remaining-self.position().distanceTo(ssca$previous)));
        nbt.putFloat("SscaFlightSpeed",(float)ssca$flightSpeedMultiplier());
        nbt.putFloat("SscaLaunchSpeed",self.getEntityData().get(SSCA_LAUNCH_SPEED));
        if(ssca$target!=null)nbt.putUUID("SscaHomingTarget",ssca$target);
    }
    @Inject(method="onHitEntity",at=@At("HEAD"))
    private void ssca$hit(EntityHitResult hit,CallbackInfo ci) {
        var self=(Projectile)(Object)this;
        if(!self.level().isClientSide)ssca$weave.direct();
        if(!self.level().isClientSide&&hit.getEntity().getUUID().equals(ssca$target)) {
            // 穿刺弹命中锁定目标后继续向前，不能回头追逐已命中的同一个目标。
            ssca$target=null;self.getEntityData().set(SSCA_TARGET,-1);
        }
    }
    @Inject(method="onHitBlock",at=@At("HEAD"))
    private void ssca$ground(net.minecraft.world.phys.BlockHitResult hit,CallbackInfo ci) {
        var self=(Projectile)(Object)this;
        if(self.getOwner() instanceof ServerPlayer caster&&caster.level()==self.level())ssca$weave.ground(caster,hit);
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void ssca$read(CompoundTag nbt,CallbackInfo ci) {
        ssca$weave.read(nbt);
        ssca$allay=nbt.contains("SscaAllayLink")?AllaySpellLink.Cast.read(nbt.getCompound("SscaAllayLink")):null;
        ssca$bat=nbt.contains("SscaBatLink")?github.com.gengyoubo.sscfa.spell.BatSpellLink.Cast.read(nbt.getCompound("SscaBatLink")):null;
        ssca$court=nbt.contains("SscaCourtLink")?github.com.gengyoubo.sscfa.spell.CourtSpellLink.Cast.read(nbt.getCompound("SscaCourtLink")):null;
        if(!nbt.contains("SscaHomingRemaining"))return;
        var self=(Projectile)(Object)this;
        double remaining=nbt.getDouble("SscaHomingRemaining");
        ssca$remaining=Double.isFinite(remaining)?Math.max(0,remaining):0;ssca$previous=self.position();
        ssca$target=nbt.hasUUID("SscaHomingTarget")?nbt.getUUID("SscaHomingTarget"):null;
        float factor=nbt.contains("SscaFlightSpeed")?nbt.getFloat("SscaFlightSpeed"):1;
        float launch=nbt.getFloat("SscaLaunchSpeed");
        self.getEntityData().set(SSCA_FLIGHT_SPEED,Float.isFinite(factor)&&factor>=1&&factor<=5?factor:1);
        self.getEntityData().set(SSCA_LAUNCH_SPEED,Float.isFinite(launch)&&launch>=0?launch:0);
    }
    @Unique private static boolean ssca$linear(Projectile entity) {
        return entity instanceof github.com.gengyoubo.sscfa.entity.SpellFireBoltEntity
                ||entity instanceof github.com.gengyoubo.sscfa.entity.SpellFrostSpikeEntity
                ||entity instanceof github.com.gengyoubo.sscfa.entity.SpellMoonlightArrowEntity
                ||entity instanceof github.com.gengyoubo.sscfa.entity.SpellCurseMarkEntity;
    }
}
