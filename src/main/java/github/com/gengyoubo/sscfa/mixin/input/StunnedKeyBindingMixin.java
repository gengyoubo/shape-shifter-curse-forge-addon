package github.com.gengyoubo.sscfa.mixin.input;

import net.onixary.shapeShifterCurseForge.client.TransformClientState;
import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class StunnedKeyBindingMixin {

	@Shadow
	private int clickCount;

	@Shadow
	private boolean isDown;

	@Unique
	private boolean isTargetKey(String key) {
		return key.equals("key.shape-shifter-curse.active_skill_1") ||
				key.equals("key.shape-shifter-curse.active_skill_2") ||
				key.equals("key.ssc_addon.sp_primary") ||
				key.equals("key.ssc_addon.sp_secondary") ||
				key.equals("key.use") ||
				key.equals("key.attack") ||
				// 移动 / 跳跃 / 潜行 / 疾跑：装死(PLAYING_DEAD)期间必须彻底屏蔽，否则客户端
				// ClientPlayerEntity 仍会读到 movementInput 非零，travel(input) 内 updateVelocity
				// 重新给玩家加水平速度，导致 PlayingDeadEffect 服务端每 tick 清零的 velocity
				// 又被客户端推回去，玩家仍能 WASD 移动。STUN 已用 -100% 移速属性兜底，多一层
				// keybinding 屏蔽不会有副作用且能消除客户端预测性滑步。
				key.equals("key.forward") ||
				key.equals("key.back") ||
				key.equals("key.left") ||
				key.equals("key.right") ||
				key.equals("key.jump") ||
				key.equals("key.sneak") ||
				key.equals("key.sprint") ||
				key.equals("key.tacz.shoot.desc") ||
				key.equals("key.tacz.aim.desc") ||
				key.equals("key.tacz.inspect.desc") ||
				key.equals("key.tacz.reload.desc") ||
				key.equals("key.tacz.fire_select.desc") ||
				key.equals("key.tacz.crawl.desc") ||
				key.equals("key.tacz.refit.desc") ||
				key.equals("key.tacz.zoom.desc") ||
				key.equals("key.tacz.melee.desc");
	}

	@Unique
	private boolean isPlayerStunned() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return false;
		}
		return client.player.hasEffect(SscAddon.STUN)
				|| client.player.hasEffect(SscAddon.PLAYING_DEAD)
				|| TransformClientState.isTransforming();
	}

	@Unique
	private boolean isPlayerSheep() {
		Minecraft client = Minecraft.getInstance();
		return client != null && client.player != null
				&& client.player.hasEffect(SscAddon.SHEEP_FORM);
	}

	/** 变羊期间屏蔽的键（2026-09-29 用户定稿「羊能走但不能打/放技能」）：技能/攻击/使用/TACZ 枪械链，
	 *  **不含移动键**——移动键屏蔽会让 movementInput 拿不到按键直接完全不能走（首轮实机踩坑：
	 *  SHEEP_FORM 挂进 isPlayerStunned 后 isTargetKey 里的移动键全被 isPressed/setPressed 屏蔽），
	 *  限速由服务端移速上限修饰符负责（SheepFormRestoreHandler）。 */
	@Unique
	private boolean isSheepBlockedKey(String key) {
		return key.equals("key.shape-shifter-curse.active_skill_1") ||
				key.equals("key.shape-shifter-curse.active_skill_2") ||
				key.equals("key.ssc_addon.sp_primary") ||
				key.equals("key.ssc_addon.sp_secondary") ||
				key.equals("key.use") ||
				key.equals("key.attack") ||
				key.equals("key.tacz.shoot.desc") ||
				key.equals("key.tacz.aim.desc") ||
				key.equals("key.tacz.inspect.desc") ||
				key.equals("key.tacz.reload.desc") ||
				key.equals("key.tacz.fire_select.desc") ||
				key.equals("key.tacz.crawl.desc") ||
				key.equals("key.tacz.refit.desc") ||
				key.equals("key.tacz.zoom.desc") ||
				key.equals("key.tacz.melee.desc");
	}

	@Unique
	private boolean isPlayerRooted() {
		// ROOTED：只锁移动/跳跃（不锁视角/技能键），荧光幼灵法阵激光蓄力/释放期使用
		Minecraft client = Minecraft.getInstance();
		return client != null && client.player != null && client.player.hasEffect(SscAddon.ROOTED);
	}

	@Unique
	private boolean isMovementKey(String key) {
		return key.equals("key.forward") || key.equals("key.back")
				|| key.equals("key.left") || key.equals("key.right")
				|| key.equals("key.jump") || key.equals("key.sneak") || key.equals("key.sprint");
	}

	@Unique
	private boolean shouldBlock(String key) {
		Minecraft client = Minecraft.getInstance();
		if (client != null && github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked(client.player)) {
			return !java.util.Set.of("key.chat", "key.command", "key.screenshot", "key.fullscreen",
					"key.togglePerspective", "key.playerlist").contains(key);
		}
		return (isTargetKey(key) && isPlayerStunned())
				|| (isMovementKey(key) && isPlayerRooted())
				|| (isSheepBlockedKey(key) && isPlayerSheep());
	}

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	private void onWasPressed(CallbackInfoReturnable<Boolean> cir) {
		KeyMapping binding = (KeyMapping) (Object) this;
		String key = binding.getName();

		if (shouldBlock(key)) {
			// Clear any buffered input so it doesn't trigger later
			this.clickCount = 0;
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
		KeyMapping binding = (KeyMapping) (Object) this;
		String key = binding.getName();

		if (shouldBlock(key)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
	private void onSetPressed(boolean isDown, CallbackInfo ci) {
		KeyMapping binding = (KeyMapping) (Object) this;
		String key = binding.getName();

		// If trying to press the key (isDown=true) while stunned, block it AND ensure internal state is false
		if (isDown && shouldBlock(key)) {
			this.isDown = false;
			ci.cancel();
		}
	}

	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private void onMatchesKey(int key, int scancode, CallbackInfoReturnable<Boolean> cir) {
		KeyMapping binding = (KeyMapping) (Object) this;
		String translationKey = binding.getName();

		if (shouldBlock(translationKey)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
	private void onMatchesMouse(int button, CallbackInfoReturnable<Boolean> cir) {
		KeyMapping binding = (KeyMapping) (Object) this;
		String translationKey = binding.getName();

		if (shouldBlock(translationKey)) {
			cir.setReturnValue(false);
		}
	}
}
