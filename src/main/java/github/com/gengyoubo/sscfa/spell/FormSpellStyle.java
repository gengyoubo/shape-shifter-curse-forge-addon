package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery;
import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 本次施法的形态协同快照；原生mana与身体崩解度分别支付。 */
public final class FormSpellStyle {
    private static final BalanceReader BAL = new BalanceReader("systems.form_casting");
    private static final ThreadLocal<FormSpellStyle> EFFECT = new ThreadLocal<>();
    public AllaySpellLink.Cast allay;
    public BatSpellLink.Cast bat;
    public CourtSpellLink.Cast court;
    public static CourtSpellLink.Cast currentCourt(){var style=EFFECT.get();return style==null?null:style.court;}
    public static BatSpellLink.Cast currentBat(){var style=EFFECT.get();return style==null?null:style.bat;}
    public static ServerPlayer currentCaster(){var style=EFFECT.get();return style==null?null:style.player;}
    public void bindBat(github.com.gengyoubo.sscfa.neomagic.NeoState.Experiment record){if(spreading)bat=new BatSpellLink.Cast(player,record);record.bloodSpent=blood.spent();court=CourtSpellLink.create(player,record);}
    public static AllaySpellLink.Cast currentAllay(){var style=EFFECT.get();return style==null?null:style.allay;}
    public void bindAllay(double actual,github.com.gengyoubo.sscfa.neomagic.NeoState.Experiment record,boolean directCurse){allay=AllaySpellLink.create(player,actual,record,directCurse);}
    /** Client wiring supplies the server-authoritative active-cast snapshot without loading client classes here. */
    public static java.util.function.ToDoubleFunction<Player> clientRangeSnapshot = player -> Double.NaN;
    private final ServerPlayer player;
    public final Timing timing;
    public final boolean accelerated;
    public final boolean novaQuickCast;
    public final boolean moonWeaver;
    public final SpiderSpellLink.RollGroup weave;
    public final double collapseMultiplier, range;
    public final double projectileSpeed,projectileRange;
    public final LivingEntity target;
    public final MoistureHealing healing;
    public final FrostSpellLink.Payment frost;
    public final BatSpellLink.Blood blood;
    public final boolean spreading;
    private final int mana;
    private final int moisture;
    private boolean paid;
    private boolean cloak;
    private long cloakCast=-1;

    /** originalPre之前按比例推进演出，之后严格每真实tick推进一tick。 */
    public record Timing(int originalDuration, int originalPre, int preparation, int duration, int lockTick) {
        public static Timing of(int original, int adjusted, int lock, double multiplier) {
            original = Math.max(0, original);
            int before = lock < 0 ? original : Math.min(original, Math.max(0, lock));
            int tail = original - before;
            int pre = (int)Math.ceil(Math.max(0, adjusted - tail) * multiplier);
            return new Timing(original, before, pre, pre + tail, lock < 0 ? -1 : pre);
        }
        public int logical(int elapsed) { return (int)logical(elapsed, originalPre, preparation); }
        public static double logical(double elapsed, int originalPre, int preparation) {
            double t = Math.max(0, elapsed);
            return preparation > 0 && t < preparation ? t * originalPre / preparation
                    : originalPre + Math.max(0, t - preparation);
        }
    }

    private FormSpellStyle(ServerPlayer player, Timing timing, boolean accelerated,
                           double collapseMultiplier, LivingEntity target, double range, int mana,boolean projectile,boolean ice,boolean debuff) {
        this.player=player;this.timing=timing;this.accelerated=accelerated;
        this.novaQuickCast=OcelotSpellLink.quickCast(player);
        boolean enhanced=projectile&&canEnhanceProjectiles(player);
        this.projectileSpeed=enhanced?BAL.d("fluorescent_projectile_speed",1.5):1;
        this.projectileRange=enhanced?BAL.d("fluorescent_projectile_range",1.5):1;
        this.moisture=enhanced?BAL.i("fluorescent_projectile_moisture",10):0;
        this.frost=FrostSpellLink.plan(player,ice);
        this.blood=BatSpellLink.planBlood(player);
        this.spreading=BatSpellLink.spreading(player,debuff);
        this.collapseMultiplier=collapseMultiplier*(enhanced?BAL.d("fluorescent_projectile_collapse_multiplier",1.05):1)
                *(frost==null?1:frost.multiplier)*blood.multiplier*BatSpellLink.spreadMultiplier(player,debuff);
        this.moonWeaver=projectile&&SpiderSpellLink.canWeave(player);
        this.weave=moonWeaver?new SpiderSpellLink.RollGroup():null;
        this.target=target;this.range=range*this.projectileRange;this.mana=mana+(moonWeaver?SpiderSpellLink.webCost():0);
        this.healing=MoistureHealing.create(player);
    }
    public static boolean canAccelerate(Player player) {
        return player!=null&&FormUtils.isAnyForm(player,FormIdentifiers.FAMILIAR_FOX_SP,FormIdentifiers.FAMILIAR_FOX_RED)
                && ResourceBars.get(player,BarKeys.VANILLA_MANA)>=BAL.i("acceleration_mana",10);
    }
    public static double collapseMultiplier(Player player) {
        return (canAccelerate(player)?BAL.d("acceleration_collapse_multiplier",1.05):1)*BatSpellLink.bloodMultiplier(player);
    }
    public static double collapseMultiplier(Player player,boolean projectile) {
        return collapseMultiplier(player)*(projectile?projectileCollapseMultiplier(player):1);
    }
    public static double collapseMultiplier(Player player,boolean projectile,boolean ice) {
        return collapseMultiplier(player,projectile)*FrostSpellLink.multiplier(player,ice);
    }
    public static double collapseMultiplier(Player player,boolean projectile,boolean ice,boolean debuff) {
        return collapseMultiplier(player,projectile,ice)*BatSpellLink.spreadMultiplier(player,debuff);
    }
    public static double sheepProjectileRange() {return BAL.d("sheep_projectile_range",40);}
    private static boolean fluorescent(Player player) {
        return player!=null&&FormUtils.isAnyForm(player,FormIdentifiers.AXOLOTL_FLUORESCENT,FormIdentifiers.AXOLOTL_ALING);
    }
    private static boolean canEnhanceProjectiles(Player player) {
        return fluorescent(player)&&player.getAirSupply()>=BAL.i("fluorescent_projectile_moisture",10);
    }
    public static double projectileCollapseMultiplier(Player player) {
        return canEnhanceProjectiles(player)?BAL.d("fluorescent_projectile_collapse_multiplier",1.05):1;
    }
    public static double projectileSpeedMultiplier(Player player) {
        return canEnhanceProjectiles(player)?BAL.d("fluorescent_projectile_speed",1.5):1;
    }
    public static double projectileRangeMultiplier(Player player) {
        var effect=EFFECT.get();
        if(effect!=null&&effect.player==player)return effect.projectileRange;
        double snapshot=player instanceof ServerPlayer server?SpellChannelManager.projectileRangeSnapshot(server)
                :player==null?Double.NaN:clientRangeSnapshot.applyAsDouble(player);
        if(Double.isFinite(snapshot))return snapshot;
        return canEnhanceProjectiles(player)?BAL.d("fluorescent_projectile_range",1.5):1;
    }
    public static Timing timing(ServerPlayer player,int original,int adjusted,int lock) {
        return Timing.of(original,adjusted,lock,OcelotSpellLink.quickCast(player)?0:canAccelerate(player)?BAL.d("preparation_multiplier",.75):1);
    }
    public static FormSpellStyle plan(ServerPlayer player, Spell spell, int level, int original, int adjusted) {
        return plan(player,spell.getConfig().projectile,spell.getProjectileRange(level),original,adjusted,spell.getLockInTick(),spell.getElement()==FormationElement.ICE,spell.getConfig().debuff&&spell.getRarity(level)!=SpellRarity.RED);
    }
    public static FormSpellStyle planTriggered(ServerPlayer player,Spell spell,int level,int original,int adjusted) {
        return new FormSpellStyle(player,Timing.of(original,adjusted,spell.getLockInTick(),0),false,1,null,
                spell.getProjectileRange(level),0,spell.getConfig().projectile,spell.getElement()==FormationElement.ICE,spell.getConfig().debuff&&spell.getRarity(level)!=SpellRarity.RED);
    }
    /** Non-projectile plan (range only); kept for callers written against the earlier Forge stub. */
    public static FormSpellStyle plan(ServerPlayer player,double range,int original,int adjusted,int lock) {
        return plan(player,false,range,original,adjusted,lock,false,false);
    }
    public static FormSpellStyle plan(ServerPlayer player,boolean projectile,double range,int original,int adjusted,int lock) {
        return plan(player,projectile,range,original,adjusted,lock,false);
    }
    public static FormSpellStyle plan(ServerPlayer player,boolean projectile,double range,int original,int adjusted,int lock,boolean ice) {
        return plan(player,projectile,range,original,adjusted,lock,ice,false);
    }
    public static FormSpellStyle plan(ServerPlayer player,boolean projectile,double range,int original,int adjusted,int lock,boolean ice,boolean debuff) {
        boolean quick=canAccelerate(player);
        LivingEntity target=projectile&&FormUtils.isForm(player,FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)
                ?findTarget(player,range):null;
        return new FormSpellStyle(player,timing(player,original,adjusted,lock),
                quick,quick?BAL.d("acceleration_collapse_multiplier",1.05):1,target,range,
                quick?BAL.i("acceleration_mana",10):target==null?0:BAL.i("homing_mana",10),projectile,ice,debuff);
    }
    /** 所有起手预检成功后调用一次，不足不扣，不生成施法/崩解记录。 */
    public boolean pay() {
        if(paid)return true;
        if(moisture>0&&player.getAirSupply()<moisture)return false;
        if(mana>0&&!ResourceBars.consume(player,BarKeys.VANILLA_MANA,mana)) {
            SpellChannelManager.playNoManaSound(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.spell.homing_mana",mana),true);
            return false;
        }
        if(moisture>0&&!MoistureWaterRecovery.consume(player,moisture))return false;
        if(frost!=null&&!frost.pay())return false;
        if(!blood.pay())return false;
        // True invisibility keeps covering the caster while the spell is prepared.
        long cover=github.com.gengyoubo.sscfa.power.TrueInvisibilityAbilityPower.beginSpellCover(player);
        if(cover>=0){cloak=true;cloakCast=cover;}
        paid=true;return true;
    }
    public int paidMoisture(){return paid?moisture:0;}
    public void endCloak(){if(cloak){cloak=false;github.com.gengyoubo.sscfa.power.TrueInvisibilityAbilityPower.endSpellCover(player,cloakCast);}}
    public void effect(Runnable action) {
        endCloak();
        var previous=EFFECT.get();EFFECT.set(this);
        try{action.run();}finally{if(previous==null)EFFECT.remove();else EFFECT.set(previous);}
    }
    /** 仅法术的生命回复进入此处；原生装死、自然回血和反转伤害沿用原结算。 */
    public static void heal(ServerPlayer caster,LivingEntity target,float amount) {
        var cast=EFFECT.get();
        if(cast==null||cast.player!=caster||cast.healing==null)target.heal(amount);
        else cast.healing.heal(target,amount,value->target.heal((float)value));
    }
    public static boolean legalTarget(ServerPlayer player,LivingEntity target) {
        return target!=player&&target.isAlive()&&!target.isRemoved()&&!target.isSpectator()
                &&!(target instanceof ArmorStand)&&target.level()==player.level()
                &&!player.isAlliedTo(target)&&!WhitelistUtils.isProtected(player,target)
                &&(!(target instanceof ServerPlayer other)||player.canHarmPlayer(other))
                &&!DomainManager.blocksTargeting(player,target);
    }
    /** 一次施法只搜一次；用夹角余弦排序，距离只用于并列时决胜。 */
    public static LivingEntity findTarget(ServerPlayer player,double range) {
        if(!Double.isFinite(range)||range<=0)return null;
        Vec3 eye=player.getEyePosition(),look=player.getViewVector(1).normalize();
        double bestCos=Math.cos(Math.toRadians(BAL.d("homing_cone_degrees",15))),bestDistance=Double.MAX_VALUE;
        LivingEntity best=null;
        for(var candidate:player.level().getEntitiesOfClass(LivingEntity.class,new AABB(eye,eye).inflate(range),
                e->legalTarget(player,e))) {
            Vec3 delta=candidate.getBoundingBox().getCenter().subtract(eye);
            double distance=delta.lengthSqr();
            if(distance<1e-8||distance>range*range)continue;
            double cosine=look.dot(delta)/Math.sqrt(distance);
            if(cosine+1e-10<bestCos||Math.abs(cosine-bestCos)<1e-10&&distance>=bestDistance)continue;
            if(!player.hasLineOfSight(candidate))continue;
            bestCos=cosine;bestDistance=distance;best=candidate;
        }
        return best;
    }
    public static void register() {
        FrostSpellLink.register();
        OcelotSpellLink.register();
        SpiderSpellLink.register();
        AllaySpellLink.register();
        BatSpellLink.register();
        CourtSpellLink.register();
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityLoad((entity,world)->{
            var cast=EFFECT.get();
            if(cast!=null&&entity instanceof Projectile projectile
                    &&projectile.getOwner()==cast.player&&entity instanceof HomingProjectile homing) {
                if(cast.moonWeaver)((SpiderSpellLink.WebProjectile)projectile).ssca$weave().enable(cast.weave.id);
                if(cast.projectileSpeed!=1||cast.projectileRange!=1)homing.ssca$flight(cast.projectileSpeed,cast.range);
                if(cast.target!=null)homing.ssca$home(cast.target,cast.range);
            }
            if(entity instanceof SpiderSpellLink.WebProjectile projectile&&world instanceof net.minecraft.server.level.ServerLevel server)projectile.ssca$weave().loaded(server,entity.getUUID());
        });
    }
    /** 由原版投射体混入提供；只给本次带标签的法术实例启用。 */
    public interface HomingProjectile {
        void ssca$home(LivingEntity target,double range);
        Vec3 ssca$guidedVelocity(Vec3 ordinary);
        boolean ssca$isHoming();
        boolean ssca$hasRangeLimit();
        void ssca$flight(double speedMultiplier,double maximumRange);
        double ssca$flightSpeedMultiplier();
        void ssca$syncFlightVelocity();
    }
}
