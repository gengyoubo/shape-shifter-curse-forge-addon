package github.com.gengyoubo.sscfa.mixin.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.com.gengyoubo.sscfa.item.PotionTestTubes;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

	@Shadow
	public Player player;

	@Shadow
	public abstract ItemStack getItem(int slot);

	@Shadow
	public abstract void setItem(int slot, ItemStack stack);

	@Shadow
	public abstract boolean add(ItemStack stack);

	/** 闂佸搫鐗嗛悧鍡涙儍瑜版帗鍋ら柟顖炲亰濡懘鏌涢埡鍕仩妞ゃ垹鎳忕粙?闂佹寧绋戦張顒€锕㈡稊绔渨er闂佸搫鍟抽崺鏍ㄦ櫠閻樿绀傚ù锝囩摂閸熷懘寮堕埡鍌溾槈闁告瑥妫濆畷銏ゆ偄閸濆嫷鏆梺鐟板帨閸撴繄鈧絺鍋撻梺鍛婅壘閻楀棝鎳熼悢璁胯鎷呯悰鈩冪厾闂?*/
	@WrapOperation(method = "hasRemainingSpaceForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isStackable()Z"))
	private boolean ssc_addon$addonPotionPickupStackable(ItemStack stack, Operation<Boolean> original) {
		return PotionTestTubes.hasCustomStackLimit(stack)
				? PotionTestTubes.stackLimit(stack, this.player) > 1 : original.call(stack);
	}

	/** 闂佺懓鍘滈崜婵堚偓锝傚亾闂佸憡鑹鹃悧鍡涙嚐閻斿摜鈻旈幖杈剧稻濞呮洟姊婚崼娑樼仾鐟滄澘鍟村畷妤呭Ψ閵夈儱缍戦梺娲绘娇閸斿海鏁搹鍏夊亾閻熸澘顣虫繛鍫熷灴閹筹繝顢涘☉娆忕樆婵炴垶鎸搁敃顏勵瀶濞差亜违?*/
	@WrapOperation(method = "hasRemainingSpaceForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
	private int ssc_addon$tubePickupMergeLimit(ItemStack existingStack, Operation<Integer> original) {
		return PotionTestTubes.hasCustomStackLimit(existingStack)
				? PotionTestTubes.stackLimit(existingStack, this.player) : original.call(existingStack);
	}

	/** 闂佸搫鍊瑰妯肩磽閹捐绠抽柕澶涢檮閺嗩亪鏌″鍛闁哄棛鍠栭弻宀冪疀鎼淬倗顦伴梻鍌氭缂堜即鍩€椤戣儻鍏屽褍娼″畷顐ｆ媴鐟欏嫮鏆炴繛鎴炴尵閻︽彂R闂佹寧绋戦懟顖滄崲閳ь剙顪冮妶鍥╁笡妞ゃ儱鎳樺濠氬Ψ閵堝洨鍊為梺绋跨箞閸庢挳鎯侀婊呬笉闁斥晛鍟梽宥夋煕閹邦剙鏆遍柣锝囧亾缁嬪绻濋崶顭戔偓娆撴煏?*/
	@WrapOperation(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
	private int ssc_addon$tubePickupInsertLimit(ItemStack destination, Operation<Integer> original,
			@Local(argsOnly = true) ItemStack incomingStack) {
		return PotionTestTubes.hasCustomStackLimit(incomingStack)
				? PotionTestTubes.stackLimit(incomingStack, this.player) : original.call(destination);
	}

	/** 闁诲骸婀遍幊鎾斥枍閹烘挻浜ら柡鍐ㄦ川缁犵兘鎮硅濞堝穳fer闂佹寧绋戦懟顖炴偩閻樿鎹堕柕濞垮妿濞堝爼鏌ｉ埗鈺傛nsertStack闂佸憡鎸哥粔鏉戭潩閵婏箑顕辨慨姗嗗幖閻﹁鈽夐幘绛硅含婵＄偓妞介獮蹇涘幢濡も偓閻庡鏌?*/
	@WrapOperation(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
	private int ssc_addon$tubeOfferedStackLimit(ItemStack stack, Operation<Integer> original) {
		return PotionTestTubes.hasCustomStackLimit(stack)
				? PotionTestTubes.stackLimit(stack, this.player) : original.call(stack);
	}

	/** 婵犮垼娉涘畷顒勭嵁閹煎朝wer闂佸憡鑹剧花鑲╂崲濮樿埖鍋╂繛鍡樺姈閿涘鎮洪幒鎴炲殗闁革絾妞藉浠嬪醇椤掑倻顦梻鍌氬暙瀵爼顢楀☉姘闁绘柨鍢查埛鏍归敐鍡欐噮闁宠鐗犲濠氬级閹搭厼娈奸梺鐓庡槻椤曨厽鏅堕崸妤€浼犲ù锝呭濡插鏌涢幒鏇炵厫闁搞劊鍔戝畷鎰板箳閹炬枼鎷搁梺?*/
	@Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$guardTubeInsertion(int slot, ItemStack incomingStack, CallbackInfoReturnable<Integer> cir) {
		if (!PotionTestTubes.hasCustomStackLimit(incomingStack)) return;
		ItemStack existingStack = this.getItem(slot);
		if (!existingStack.isEmpty() && (!ItemStack.isSameItemSameTags(existingStack, incomingStack)
				|| existingStack.getCount() >= PotionTestTubes.stackLimit(incomingStack, this.player))) {
			cir.setReturnValue(incomingStack.getCount());
		}
	}

	/**
	 * Helper to check if an item is a locked form-exclusive item in a specific slot
	 */
	@Unique
	private boolean isLockedAllayItem(int slot, ItemStack stack) {
		net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
		boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
		if (!isAllaySp) return false;

		if (slot == 0 && stack.is(SscAddon.ALLAY_HEAL_WAND)) return true;
		return slot == 1 && stack.is(SscAddon.ALLAY_JUKEBOX);
	}

	/**
	 * Prevents removing potion bag from slot 8 if player is Red form
	 * Prevents removing allay items from slots 0/1 if player is Allay SP form
	 */
	@Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void preventLockedItemRemoval(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack stack = this.getItem(slot);

		// Red form: lock potion bag in slot 8
		if (slot == 8 && stack.is(SscAddon.POTION_BAG)) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isRedForm = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED);
			if (isRedForm) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
		}

		// Allay SP form: lock heal wand in slot 0, jukebox in slot 1
		if (isLockedAllayItem(slot, stack)) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}

	/**
	 * Prevents setting locked items to wrong slots
	 */
	@Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
	private void preventLockedItemMisplacement(int slot, ItemStack stack, CallbackInfo ci) {
		// Potion Bag logic (existing)
		if (stack.is(SscAddon.POTION_BAG) && slot != 8) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isRedForm = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED);
			if (isRedForm) {
				ci.cancel();
				ItemStack slot8Stack = this.getItem(8);
				if (!slot8Stack.is(SscAddon.POTION_BAG)) {
					if (!slot8Stack.isEmpty()) {
						this.setItem(slot, slot8Stack);
					}
					this.setItem(8, stack);
				}
				return;
			}
		}

		// Allay Heal Wand: must stay in slot 0
		if (stack.is(SscAddon.ALLAY_HEAL_WAND) && slot != 0) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
			if (isAllaySp) {
				ci.cancel();
				return;
			}
		}

		// Allay Jukebox: must stay in slot 1
		if (stack.is(SscAddon.ALLAY_JUKEBOX) && slot != 1) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
			if (isAllaySp) {
				ci.cancel();
			}
		}
	}

	/**
	 * Prevents inserting locked items outside their designated slots
	 */
	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void preventLockedItemInsert(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(SscAddon.POTION_BAG)) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isRedForm = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED);
			if (!isRedForm) {
				cir.setReturnValue(false);
			} else if (slot != 8 && slot != -1) {
				cir.setReturnValue(false);
			}
		}

		if (stack.is(SscAddon.ALLAY_HEAL_WAND) || stack.is(SscAddon.ALLAY_JUKEBOX)) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
			boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
			if (!isAllaySp) {
				cir.setReturnValue(false);
			}
		}
	}
}
