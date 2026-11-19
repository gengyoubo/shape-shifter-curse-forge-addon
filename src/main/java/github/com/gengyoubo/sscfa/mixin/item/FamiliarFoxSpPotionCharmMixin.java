package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.compat.FamiliarFoxSpPerks;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.items.PotionCharmItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * SSC 的药水符只认原版使魔 3 阶（{@code currentForm(...).map(...).orElse(false)}），附属使魔形态也放行。
 * SSC 发布包已重混淆为 SRG，{@code use} 在发布环境中名为 {@code m_7203_}，两个名字都列出且不经 refmap；
 * 被重定向的 {@code Optional.orElse} 是 JDK 方法，方法内只有这一处调用。
 */
@Mixin(value = PotionCharmItem.class, remap = false)
public abstract class FamiliarFoxSpPotionCharmMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = {"use", "m_7203_"}, require = 1,
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ssca$allowAddonFamiliar(Optional optional, Object other, Level level, Player player, InteractionHand hand) {
        Object original = optional.orElse(other);
        return Boolean.TRUE.equals(original) || FamiliarFoxSpPerks.canUsePotionCharm(player) ? Boolean.TRUE : original;
    }
}
