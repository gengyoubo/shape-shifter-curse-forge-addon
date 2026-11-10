package github.com.gengyoubo.sscfa.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.onixary.shapeShifterCurseForge.entity.WebBulletEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Forge 闂佹彃绉撮崯鎾绘晬濮橆偆鐭岄柡鍥ㄥ瀹曟煡鎽傚☉妤冾偩鐎殿喛顫夐惁?tick 闁汇劌瀚鐑樻交閸︻厾鐓堥悗娑欏姧缁辨瑩鎯勯鐣屽灱闁衡偓闁稖绀?Forge WebBulletEntity闁挎稑顦埀?*/
@Mixin(value = WebBulletEntity.class, remap = false)
public abstract class SscWebBulletTrailMixin {
    @Shadow private int tier;

    @WrapOperation(method = "tick", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"),
            require = 0)
    private <T extends ParticleOptions> int ssca$trail(ServerLevel world, T particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed,
            Operation<Integer> original) {
        WebBulletEntity bullet = (WebBulletEntity) (Object) this;
        if (bullet.isRemoved()) return 0;
        ParametricVisuals.touch(bullet, bullet.getUUID(), ParametricVisuals.Kind.SSC_WEB_TRAIL,
                null, bullet.position(), true, world.getGameTime() - bullet.tickCount, 0, 0, this.tier);
        return 0;
    }
}
