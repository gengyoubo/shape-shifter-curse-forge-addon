package github.com.gengyoubo.sscfa.mixin.item;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.item.WaterSpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

	protected ItemEntityMixin(EntityType<?> type, Level world) {
		super(type, world);
	}

	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	public abstract int getAge();

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		if (!this.level().isClientSide) {
			ItemStack stack = this.getItem();
			if (github.com.gengyoubo.sscfa.ability.AllaySPPortableBeacon.isActiveStack(stack)) {
				stack = stack.copy();
				github.com.gengyoubo.sscfa.ability.AllaySPPortableBeacon.clearStack(stack);
				((ItemEntity) (Object) this).setItem(stack);
			}
			// 濠电姷鏁告慨鐑姐€傛禒瀣；闁规儳顕粻楣冩煠閼圭増纭鹃柛姘愁潐缁绘盯宕奸悢椋庝桓缂備礁顑呴ˇ顖烆敇婵傜骞㈤柍杞扮琚氶梻鍌欑劍閹爼宕曢幓鎺旀殾婵せ鍋撶€规洏鍎遍～婊堝焵椤掑嫮宓侀柟鐑樻尭缁剁偤骞栧ǎ顒€鐏╅柛鏃€鐟╁娲閳哄嫮顦伴梺鍛婃煥閼活垶鏁冮姀銈呯妞ゆ棁鍋愰、鍛存⒑閸涘﹤濮囬柣顏嗗缁傚秵绺介崨濠勫幈濠碘槅鍨甸崑鎰暜濞戞氨纾奸柛婵嗗閹牓鎮￠妶鍚ゅ綊宕楅懖鈺傚櫘缂備讲鍋撻柛?闂傚倷鐒︾€笛呯矙閹烘埈娼╅柕濞炬杹閳ь剚鐗犲畷鎯邦檨闁搞倖娲熼弻?SP 闂傚倷娴囬鏍闯椤栨稐绻嗛柛銉ｅ妼椤曢亶鏌熼悜姗嗘當閻庢艾顦伴妵鍕箣閻愬樊鍤嬮梺娲讳簷缁瑩寮诲☉妯滄棃鍩€椤掑嫬鐤柛褎顨呮闂佺鎻粻鎴﹀几娓氣偓閺屾盯顢曢銏狀潾缂備讲鍋?			
			if (github.com.gengyoubo.sscfa.ability.AllaySPTotem.isMarkedActiveTotem(stack)) {
				stack = stack.copy();
				github.com.gengyoubo.sscfa.ability.AllaySPTotem.clearTotemMark(stack);
				((ItemEntity) (Object) this).setItem(stack);
			}
			if (stack.getItem() instanceof WaterSpearItem && this.getAge() >= 20) {
				this.discard();
			}
			if (stack.is(SscAddon.ALLAY_HEAL_WAND) || stack.is(SscAddon.ALLAY_JUKEBOX) || stack.is(SscAddon.POTION_BAG)) {
				this.discard();
			}
		}
	}
}
