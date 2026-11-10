package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.init.AddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 婵炴垶鎸撮崑鎾绘煟閳╁啫鍔剁€规挸鍊搁～婵嬪级閹存繄鈧鎮锋担鍛婂櫢閻犳劗鍠愮粙澶嬪緞鐎ｎ偅娈伴柣鐘叉处濞叉粓顢橀幖浣规櫖婵﹩鍓欓弬褔鏌ょ粵瀣仭闁?NBT 闂佸憡顭囬崰鎰版偋鏉堛劎鈹嶆繝闈涚墛濞堝矂鏌ㄥ☉妯肩劯闁靛牜鍣ｉ幃浠嬪Ω閿旂瓔妲梺?1/3 缂傚倸鍊甸弲婊堝棘娓氣偓閺佸秹宕奸悢椋庢Х缂傚倷绶￠崢鍓ф偖椤愶箑绫嶉柛顐ｆ礃濮ｆ劙鏌曢崱鏇″厡闁诲繑鐩顕€宕滄担绛嬫蕉閻庢鍠栭幖顐も偓瑙勫▕閺佸秵寰勯崼姘壕? * 闂佸搫鐗滄禍锝囩紦閼测晝鐭撳┑鐘宠壘閸斻儵鏌涢幒鎴烇紨缂佽京娈恛wer 闂佸湱绮崝鏇炵暦瀹€鍕唶闁糕剝顨呯紞灞解槈閹剧韬俊鐐そ閺佸秹宕烽　绀礽fyPotionStackPower闂佹寧绋戦ˇ顓㈠焵椤戣法顦︾憸鏉款槼閵囨劙濮€閿濆懐銈╁┑顔矫顓灻烘导鏉戝唨闁搞儜鍡楃厒闂佽鍏涢梽宥夊焵椤戣法鍔嶆繝鈧埀顒勬倶韫囨棏鐒界紒鎲嬪閹叉挳鏁愰崱妯活啀闂佺锕﹂妴瀣濮楃惈oRuntime闂佹寧绋戦ˇ顓㈠焵? * 闂佸摜濮撮崐鍧椝夐妶鍥╁暗缂備焦蓱濞呭繘鏌＄€ｎ偆鐭屾繛鍫熷灴瀹曟粓宕掗悙鍙夘仭 mixin闂佹寧绋掔粙鏍崲閺嶃劎顩叉繛鎴烆殘鐠愨晠鎮?Apoli/闂佸憡鎸嗛崟顐㈠/濠电偛顦板ú妯猴耿瀹曞洤瀵查柤濮愬€楅崺鐘绘煥濞戞鐒风紒楦垮亹缁梻鎷犻崣澶娾偓鐢告偠濞戞牕鍔氭繛鍙夌墵婵? */
public final class PotionTestTubes {
	public static final int PORTIONS = 3;
	public static final int DEFAULT_STACK_LIMIT = 3;
	public static final Item EMPTY = AddonItems.TEST_TUBE.get();
	private static final String BOTTLE_TICKS = "TestTubeBottleUseTicks";
	/** 闂佸憡鐟﹂敋闁奸晲鍗抽幊锟狀敍濞戞瑥鐦婚梺姹囧妼鐎氼喖煤娴兼潙鍐€闁搞儜鍡楃厒闂?NBT闂佹寧绋掗惌顔剧博閺夋垟鏋庨柍鈺佸暊閸嬫捁顦舵い鏇樺灪缁嬪﹥寰勫☉姘鳖槷婵炴垶鎸哥粔纾嬨亹閺屻儱绀嗛柛鈩冨嚬濡懘鏌?*/
	private static final String TARGET_FORM = "targetForm";
	/** 闂佸憡顭囬崰鎾存櫠濡ゅ懎瀚夐柛顐ｇ箘濮ｇ娀鏌ｉ垾鍐差暢婵炲牊鍨堕〃婊堫敊閻愵剚娈㈤梺鍝勫暙閻栧ジ寮抽柆宥呂?*/
	private static final int MOON_DUST_BOTTLE_TICKS = 32;

	private PotionTestTubes() {}

	public static boolean isDivisible(MobEffect effect) {
		if (effect instanceof TransformativeStatusEffect) return false;
		return !effect.isInstantenous() || effect == MobEffects.HEAL || effect == MobEffects.HARM;
	}

	public static boolean isDivisible(ItemStack stack) {
		if (stack.isEmpty() || stack.getTag() != null && stack.getTag().contains(TARGET_FORM)) return false;
		return PotionUtils.getMobEffects(stack).stream().allMatch(effect -> isDivisible(effect.getEffect()));
	}

	public static boolean isBottle(ItemStack stack) {
		if (stack.is(AddonItems.MOON_DUST_BOTTLE.get())) return true;
		if (!(stack.getItem() instanceof PotionItem) || stack.getItem() instanceof SplashPotionItem
				|| stack.getItem() instanceof LingeringPotionItem || stack.is(AddonItems.TEST_TUBE_POTION.get())) return false;
		var potion = PotionUtils.getPotion(stack);
		return potion != AddonEffects.MOON_DUST_TUBE.get() && potion != AddonEffects.UNIVERSAL_ENERGY_POTION.get()
				&& isDivisible(stack);
	}

	public static ItemStack divide(ItemStack bottle) {
		if (!isBottle(bottle)) return ItemStack.EMPTY;
		boolean moon = bottle.is(AddonItems.MOON_DUST_BOTTLE.get());
		ItemStack out = new ItemStack(moon ? AddonItems.MOON_DUST_VIAL.get() : AddonItems.TEST_TUBE_POTION.get(), PORTIONS);
		if (bottle.hasTag()) out.setTag(bottle.getTag().copy());
		out.getOrCreateTag().putInt(BOTTLE_TICKS, moon ? MOON_DUST_BOTTLE_TICKS : bottle.getUseDuration());
		if (moon) {
			out.getOrCreateTag().remove("Potion");
			// 闂佸搫鍞查崟顒傜暠闁诲繐绻戦…鍥箺閸楃偐鏌﹂柍鈺佸暞缁犳帡鏌￠崪浣告珝闁逞屽墯娣囧搫鐣甸崒姘兼僵缁炬儳顑嗙粊浼存煠閽樺娈╃紒杈ㄧ箖缁嬪顓奸崼婵嗗箣闂佸憡鐟ф慨瀛樻叏閻愬鈻旈柕鍫濆閺佹粎绱撻崒娑欏磩鐞氭瑩鏌?			out.getOrCreateTag().remove("CustomPotionEffects");
			if (!out.getTag().contains("CustomPotionColor")) out.getTag().putInt("CustomPotionColor", AddonEffects.MOON_DUST_COLOR);
		}
		return out;
	}

	public static int drinkTicks(int bottleTicks) {
		return Math.max(1, (int) Math.ceil(Math.max(1, bottleTicks) * .4));
	}

	/** 闂佸綊鏅查懗鍫曟偨閼姐倗灏电紓浣股戝▍蹇涙煛鐎ｎ偆鐭嬫俊鐐插€垮褰掑箰鎼达紕妯嗘繛瀵稿Ь婵倝寮抽悢鐓庤Е闁瑰瓨绮庨悷鎰版煕濞嗘劗澧柡鍡悼缁辨捇鍩￠崒娑欐緬闂佹寧绋戦悧鍡涘箚閸儲鈷曢柟閭﹀灡椤ユ垿鏌ｉ妸銉ヮ伂缂佽埖绻傞锝嗘綇閵婏附鐝￠梺鍝勵儐缁捇骞楁總鍛婃櫖濠㈣埖绋撻獮銏ゆ煟椤掑澧俊鐐插€婚悮鍓ф嫚閹绘帗鏋鹃梺鍝勭Ф閺屽鎹㈤幋锕€鐐婇柣鎴濇川缁€澶愭煟閵忋倓鎲鹃柕鍫櫍閹粙濡搁敂绛嬫Н闂佸湱顭堥ˇ顖涙櫠韫囨稒鐓傞煫鍥ㄦ礈缁辨岸鏌￠埀顒佺┍閹典礁浜?*/
	public static List<MobEffectInstance> doseEffects(ItemStack stack) {
		List<MobEffectInstance> result = new ArrayList<>();
		for (var effect : PotionUtils.getMobEffects(stack)) {
			if (effect.getEffect().isInstantenous()) {
				result.add(new MobEffectInstance(effect));
				continue;
			}
			CompoundTag nbt = effect.save(new CompoundTag());
			for (CompoundTag part = nbt; ; part = part.getCompound("HiddenEffect")) {
				int duration = part.getInt("Duration");
				if (duration > 0) part.putInt("Duration", Math.max(1, (int) (((long) duration + PORTIONS - 1) / PORTIONS)));
				if (!part.contains("HiddenEffect", 10)) break;
			}
			var scaled = MobEffectInstance.load(nbt);
			if (scaled != null) result.add(scaled);
		}
		return result;
	}

	public static boolean isFilledTube(ItemStack stack) { return stack.getItem() instanceof FilledTube; }

	public static boolean hasCustomStackLimit(ItemStack stack) { return isFilledTube(stack) || MoonDustReliefItem.isBottled(stack); }

	public static int stackLimit(ItemStack stack, @Nullable Player player) {
		if (!hasCustomStackLimit(stack)) return stack.getMaxStackSize();
		return isFilledTube(stack) ? DEFAULT_STACK_LIMIT : 1;
	}

	public static final class FilledTube extends PotionItem {
		public FilledTube(Item.Properties properties) {
			super(properties);
		}

		@Override
		public ItemStack getDefaultInstance() {
			var result = divide(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING));
			result.setCount(1);
			return result;
		}

		@Override
		public int getUseDuration(ItemStack stack) {
			int ticks = stack.hasTag() && stack.getTag().contains(BOTTLE_TICKS) ? stack.getTag().getInt(BOTTLE_TICKS) : 32;
			return drinkTicks(ticks);
		}

		@Override
		public Component getName(ItemStack stack) {
			var potion = PotionUtils.getPotion(stack);
			if (potion == AddonEffects.MOON_DUST_TUBE.get()) return Component.translatable("item.ssc_addon.moon_dust_vial");
			if (potion == Potions.WATER) return Component.translatable("item.ssc_addon.test_tube_water");
			if (potion == Potions.EMPTY) return Component.translatable("item.ssc_addon.test_tube_potion_empty");
			ItemStack bottle = new ItemStack(Items.POTION);
			if (stack.hasTag()) bottle.setTag(stack.getTag().copy());
			return Component.translatable("item.ssc_addon.test_tube_potion", bottle.getHoverName());
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			// 闂佸憡顭囬崰鎾存櫠濡ゅ懏鍤掓い鏍ㄧ⊕鐎垫粓鏌ｉ檱濞夋盯顢氶灏栨灃闁靛鍎查崺鍌毭归敐鍫熺《闁轰降鍊栫€电厧顫濋鈧紞渚€鏌熺€涙ê濮堥柛瀣仢椤斿繘寮堕幋锝呯彲闁哄鏅滃Λ浣烘崲閺囥垺鍋濋柤濮愬€栫€氬懘鏌ｉ檱椤旀劗妲愭潏鈺傚珰闁哄洨濯崥鈧梺鍛婄懐娴滐綁宕楀鈧畷鏉库攽閸曘劌浜?
			return InteractionResult.PASS;
		}

		@Override
		public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
			if (stack.isEmpty() || !stack.is(this)) return stack;
			Player player = user instanceof Player p ? p : null;
			if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
			if (!world.isClientSide) {
				for (var effect : doseEffects(stack)) {
					if (effect.getEffect().isInstantenous()) {
						effect.getEffect().applyInstantenousEffect(player, player, user, effect.getAmplifier(), 1.0 / PORTIONS);
					} else {
						user.addEffect(effect);
					}
				}
			}
			if (player != null) player.awardStat(Stats.ITEM_USED.get(this));
			user.gameEvent(GameEvent.DRINK);
			if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
				if (stack.isEmpty()) return new ItemStack(AddonItems.TEST_TUBE.get());
				if (player != null) player.getInventory().placeItemBackInInventory(new ItemStack(AddonItems.TEST_TUBE.get()));
			}
			return stack;
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> lines, TooltipFlag context) {
			ItemStack shown = new ItemStack(Items.POTION);
			PotionUtils.setCustomEffects(shown, doseEffects(stack));
			PotionUtils.addPotionTooltip(shown, lines, 1f);
			lines.add(Component.translatable("tooltip.ssc_addon.test_tube_dose").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("tooltip.ssc_addon.test_tube_stack").withStyle(ChatFormatting.GRAY));
			if (PotionUtils.getPotion(stack) == Potions.WATER && PotionUtils.getMobEffects(stack).isEmpty()) {
				lines.add(Component.translatable("tooltip.ssc_addon.test_tube_water").withStyle(ChatFormatting.GRAY));
			}
		}
	}
}
