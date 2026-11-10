package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.NightmareFearClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 食梦魔「恐惧」1 秒隐匿窗口：恐惧目标本地跳过该梦魔的实体渲染。 */
@Mixin(EntityRenderer.class)
public class FearHideRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private <T extends Entity> void ssca$hideNightmareDuringFear(T entity, Frustum frustum,
                                                                  double x, double y, double z,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player player)) return;
        if (NightmareFearClient.isHidden(player.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
