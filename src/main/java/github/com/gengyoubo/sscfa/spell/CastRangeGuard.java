package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Accepted locks use a frozen effective range; this guard never owns released effects. */
public final class CastRangeGuard {
    public static final double MULTIPLIER=1.5;
    private CastRangeGuard() {}
    public record Lock(Vec3 point,LivingEntity entity,double range) {
        public boolean exceeded(ServerPlayer caster) {
            if(entity!=null&&(entity.isRemoved()||!entity.isAlive()||entity.level()!=caster.level()))return false;
            return exceeds(caster.position(),entity==null?point:entity.position(),range);
        }
    }
    public static Lock point(Vec3 point,double range){return point!=null&&validRange(range)?new Lock(point,null,range):null;}
    public static Lock entity(LivingEntity entity,double range){return entity!=null&&validRange(range)?new Lock(entity.position(),entity,range):null;}
    private static boolean validRange(double range){return Double.isFinite(range)&&range>0;}
    public static boolean exceeds(Vec3 caster,Vec3 target,double range) {
        if(caster==null||target==null||!validRange(range))return false;
        double limit=range*MULTIPLIER;
        return caster.distanceToSqr(target)>limit*limit;
    }
}
