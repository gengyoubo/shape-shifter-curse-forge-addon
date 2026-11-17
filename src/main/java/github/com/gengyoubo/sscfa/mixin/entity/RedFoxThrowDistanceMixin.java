package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ThrowablePotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrowablePotionItem.class)
public class RedFoxThrowDistanceMixin {

	@Unique
	private static final ResourceLocation RED_FOX_MANA_POWER = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_MANA;

	@Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrownPotion;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"), require = 0)
	private void modifyThrowVelocity(ThrownPotion instance, Entity entity, float pitch, float yaw, float roll, float speed, float divergence) {
		if (entity instanceof Player player) {
			// Check if player has the specific power indicating Red Fox form
			boolean isRedFox = FormPowerRegistry.has(player, RED_FOX_MANA_POWER);

			if (isRedFox) {
				speed *= 1.5F; // Increase by 50%
			}
		}
		instance.shootFromRotation(entity, pitch, yaw, roll, speed, divergence);
	}
}
