package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.renderer.DomainRenderer;
import github.com.gengyoubo.sscfa.spell.DomainCollision;
import github.com.gengyoubo.sscfa.spell.DomainRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** 客户端领域壳移动预测，与服务端共用脚部锚点/半宽余量与球壳滑行算法。 */
@Mixin(Entity.class)
public abstract class DomainClientWallMixin {

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 ssca$domainClientWall(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        if (!entity.level().isClientSide() || movement.lengthSqr() == 0) return movement;
        List<DomainRules.Shell> shells = DomainRenderer.clientShells();
        if (shells == null) return movement;
        return DomainRules.limitMovement(entity.position(), movement, entity.getBbWidth() * 0.5, shells);
    }

    @ModifyVariable(method = "move", at = @At("STORE"), index = 3)
    private Vec3 ssca$domainClientFinalMovement(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        if (!entity.level().isClientSide()) return movement;
        List<DomainRules.Shell> shells = DomainRenderer.clientShells();
        return shells == null ? movement : DomainCollision.finishMovement(entity, movement, shells);
    }
}
