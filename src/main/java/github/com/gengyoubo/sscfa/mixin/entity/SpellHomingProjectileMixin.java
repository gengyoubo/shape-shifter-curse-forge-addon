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

/** 闁烩晩鍠楅悥顤廌闁汇垹宕敮顐︽偋閸︽溂taTracker闁告艾鏈鐐烘晬濞戞ê钃熺紒鏃戝灥濞村棝宕ラ幋顖滅濞戞挸绉归·鍌涘緞閺嶃劎妲╰ick闁告瑦鍨电€垫﹢濡存担椋庣憹闁规鍋呭璺ㄢ偓鍦仒缂嶅濡?*/
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
        // 闂侇偆鍠庣€规娊宕犻崨顔肩樆闁告鍠撴晶妤呮煂韫囨挸顕у☉鎾存濡炬椽鐛崪鍐耿闁告牑鍋撻梺顐ゅ枑绾爼寮甸娆戠煠闁告艾鏈鐐剁疀椤愩倕寮鹃柟顓滃灩椤︽煡鎯囬悢椋庢澖闂侇偆鍠庣€规娊鏁嶇仦鑲╃憹閻犱讲鏅犻悵顕€鏌呴悢鐓庡赋缂傚喚鍠栧﹢顏嗏偓骞垮灪濠р偓闁告瑦蓱閸欏啴濡?
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
        // 濞戞挾顥愮换搴ｄ焊閸曨厸鏌ょ紓浣哥墢閸嬶綁宕ｉ鍥巢闁告挴鏅欑紞鎴犳崉濠靛牜鐎查柨娑樼焸娴尖晠宕楀澶屽蒋闂侇偆鍠庨懘濠冩媴閹炬剚妯嬪瀣仒缁旀挳寮壕姒燾k闁?
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
        // 闁哄牏鎲檌ck缂佸顕ф慨鈺呭捶閳虹椃per.tick濞戞柨顑呴幃妤呭矗閹寸姵鏅搁柨娑樿嫰閻°劌顩奸敐鍡橆槯濞戞梻鍠曢々锔剧磼閹惧顏搁柨娑樼焸濡插宕ｅ鍜佹Щ闁告鐡曞ù鍥礌閸濆嫭鍋ョ€点倕鐖奸弳杈╀焊閸曨厸鏌ら柕?
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
            // 缂佸苯鐏濋崺鈥愁嚕閻熺増鍤掑☉鎿冨弮閺€锝団偓瑙勬皑濞蹭即寮介崶褎鍊电紓浣堝懐鏁鹃柛姘灥婢х娀鏁嶇仦鑲╃憹闁煎疇妫勫ú鏍ㄥ緞绾惧瀚归梺顐ｅ姇閸戯繝宕ㄩ幋鎺曞幀闁汇劌瀚幃鎾寸▔閳ь剚绋夐鍡樼獥闁哄秴娲㈤埀?
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
