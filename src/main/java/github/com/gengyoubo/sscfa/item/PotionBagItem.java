package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.screen.PotionBagScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public class PotionBagItem extends Item {

	/** 蹇嵎鎶曟斁鏍忔Ы浣嶇储寮曪紙鑽按鍖呮渶宸︿晶妲戒綅锛夈€?*/
	private static final int QUICK_SLOT = 0;
	/**
	 * 鏅€氭姇鎺疯嵂姘寸殑鎶曟幏闂撮殧鍐峰嵈锛? 绉掞紙100 tick锛夈€傝褰曞湪鑽按琚?NBT锛坽@link #NBT_THROW_END}锛夛紝
	 * 涓嶇洿鎺ュ崰鐢?ItemCooldownManager 鈥斺€?鐧借壊閬僵缁熶竴鐢?{@link #inventoryTick} 鎸夈€屽揩鎹锋爮鑽按鑷韩鍓╀綑鍐峰嵈銆?
	 * 鍚屾锛屼娇閬僵闀垮害涓庢墍鎶曡嵂姘翠竴鑷达紙鏅€氳嵂姘?鎶曟幏闂撮殧锛涙棤闄愯嵂姘?鍚勫舰鎬佸厖鑳芥椂闀匡級锛屾崲鑽按鏃惰嚜鍔ㄩ噸缃€?
	 */
	private static final int THROW_COOLDOWN = 100;
	/** 楗敤鍨嬭嵂姘寸殑楗敤璇绘潯鏃堕暱锛氭瘮鍘熺増鐩存帴鍠濓紙32 tick锛夐暱 15% 鈮?37 tick銆?*/
	private static final int DRINK_TIME = 37;
	/** 鑽按琚?NBT锛氭櫘閫氭姇鎺疯嵂姘寸殑鎶曟幏鍐峰嵈缁撴潫涓栫晫鏃堕棿锛坓ame time锛夈€?*/
	private static final String NBT_THROW_END = "ThrowEndTime";
	/** 鑽按琚?NBT锛氬喎鍗撮伄缃╁悓姝ヤ护鐗岋紙= 褰撳墠蹇嵎鏍忚嵂姘村喎鍗寸粨鏉熺殑涓栫晫鏃堕棿锛? 琛ㄧず鏃犲喎鍗达級銆?*/
	private static final String NBT_CD_TOKEN = "CdToken";

	public PotionBagItem(Properties settings) {
		super(settings.stacksTo(1));
	}

	/** 鏄惁涓烘棤闄愬帇缂╄兘閲忚嵂姘达紙浠绘剰褰㈡€侊級銆?*/

	private static boolean isInfinite(ItemStack stack) {
		return stack.getItem() instanceof InfiniteEnergyPotionItem;
	}

	/** 鎶曟幏鍨嬶紙婧呭皠/婊炵暀锛夛紝鍚棤闄愯嵂姘寸殑鍠锋簠/婊炵暀褰㈡€併€?*/
	private static boolean isThrowable(ItemStack stack) {
		if (stack.is(MoonDustReliefItem.SPLASH)) return true;
		if (stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof LingeringPotionItem) {
			return true;
		}
		return stack.getItem() instanceof InfiniteEnergyPotionItem inf
				&& inf.getType() != InfiniteEnergyPotionItem.Type.DRINK;
	}

	/** 楗敤鍨嬶紙鏅€氳嵂姘存垨鏃犻檺鑽按鐨勯ギ鐢ㄥ舰鎬侊紝鎺掗櫎婧呭皠/婊炵暀锛夈€?*/
	private static boolean isDrinkable(ItemStack stack) {
		if (PotionTestTubes.isFilledTube(stack) || stack.is(MoonDustReliefItem.BOTTLE)) return true;
		if (stack.getItem() instanceof InfiniteEnergyPotionItem inf) {
			return inf.getType() == InfiniteEnergyPotionItem.Type.DRINK;
		}
		return !stack.isEmpty() && stack.getItem() instanceof PotionItem && !isThrowable(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);

		// 娼滆 + 鍙抽敭锛氭墦寮€鑽按鍖?GUI
		if (user.isShiftKeyDown()) {
			if (!world.isClientSide) {
				user.openMenu(new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.translatable("item.ssc_addon.potion_bag");
					}

					@Override
					public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
						return new PotionBagScreenHandler(syncId, inv, stack);
					}
				});
			}
			return InteractionResultHolder.success(stack);
		}

		// 鏅€氬彸閿細蹇嵎鎶曟斁鏍忥紙鏈€宸︿晶妲戒綅锛夊揩閫熶娇鐢ㄤ竴鐡惰嵂姘?
		ItemStack potion = PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT);
		if (potion.isEmpty()) {
			// 蹇嵎鏍忓凡绌猴紝鏃犳硶缁х画浣跨敤锛堜笉鎽嗘墜銆佷笉娑堣€楋級
			return InteractionResultHolder.pass(stack);
		}

		if (isThrowable(potion)) {
			if (isInfinite(potion)) {
				// 鏃犻檺鑽按锛氫粎鐢辫嚜韬┖鐡跺厖鑳介棬鎺э紱鐧借壊閬僵鐢?inventoryTick 鎸夊叾鍏呰兘鏃堕暱鍚屾
				if (InfiniteEnergyPotionItem.isRecharging(potion, world)) {
					return InteractionResultHolder.fail(stack);
				}
				if (!world.isClientSide) {
					if (!throwPotion(world, user, stack, potion)) return InteractionResultHolder.fail(stack);
				}
				return InteractionResultHolder.success(stack);
			}
			// 鏅€氭姇鎺疯嵂姘达細鑽按琚嬭嚜韬姇鎺烽棿闅斿喎鍗达紙璁板綍鍦ㄨ嵂姘磋 NBT锛屽弻绔竴鑷达級
			if (isThrowCoolingDown(stack, world)) {
				return InteractionResultHolder.fail(stack);
			}
			if (!world.isClientSide) {
				if (!throwPotion(world, user, stack, potion)) return InteractionResultHolder.fail(stack);
			}
			// 鍙岀璁板綍鎶曟幏鍐峰嵈缁撴潫鏃堕棿锛坵orld.getTime 鍙岀鍚屾锛夛紱鐧借壊閬僵鐢?inventoryTick 鍚屾
			stack.getOrCreateTag().putLong(NBT_THROW_END, world.getGameTime() + THROW_COOLDOWN);
			return InteractionResultHolder.success(stack);
		}

		// 楗敤鍨嬶細鏃犻檺鑽按绌虹摱鍏呰兘涓垯涓嶅彲楗敤
		if (isInfinite(potion) && InfiniteEnergyPotionItem.isRecharging(potion, world)) {
			return InteractionResultHolder.fail(stack);
		}
		if (!isDrinkable(potion)) return InteractionResultHolder.pass(stack);
		// 寮€濮嬮ギ鐢ㄨ鏉★紙DRINK 鍔ㄤ綔 + getMaxUseTime锛夛紝鏃犲喎鍗?
		user.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * 楗敤璇绘潯瀹屾垚锛氭柦鍔犺嵂姘存晥鏋滃苟娑堣€?1 鐡讹紝杩旇繕绌虹幓鐠冪摱锛涜嵂姘磋鏈韩涓嶈娑堣€椼€?
	 * 浠呮湇鍔＄缁撶畻锛屽浜轰富瀹㈡満涓€鑷淬€?
	 */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (!world.isClientSide && user instanceof Player player) {
			ItemStack potion = PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT);
			// 鏃犻檺鍘嬬缉鑳介噺鑽按锛堥ギ鐢ㄥ瀷锛夛細鏂藉姞 feed_effect锛屾爣璁扮┖鐡跺厖鑳斤紝涓嶆秷鑰楁暟閲忋€佷笉杩旇繕鐜荤拑鐡?
			if (potion.getItem() instanceof InfiniteEnergyPotionItem inf
					&& inf.getType() == InfiniteEnergyPotionItem.Type.DRINK) {
				if (!InfiniteEnergyPotionItem.isRecharging(potion, world)) {
					net.onixary.shapeShifterCurseForge.registry.ModEffects.FEED.get().applyInstantenousEffect(player, player, player, 0, 1.0);
					inf.markUsed(potion, world);
					PotionBagScreenHandler.setStoredStack(stack, QUICK_SLOT, potion);
				}
			} else if (PotionTestTubes.isFilledTube(potion) || potion.is(MoonDustReliefItem.BOTTLE)) {
				// 鍗曞墏鍓湰璧扮墿鍝佽嚜韬叆鍙ｏ細璇曠鎸?/3鑽晥锛屾湀灏樼摱瑁呮寜瀹屾暣鎭㈠閲忋€?
				ItemStack one = potion.copyWithCount(1);
				ItemStack remainder = one.finishUsingItem(world, player);
				if (!player.getAbilities().instabuild) {
					potion.shrink(1);
					PotionBagScreenHandler.setStoredStack(stack, QUICK_SLOT, potion);
					if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
				}
			} else if (isDrinkable(potion)) {
				// 鏂藉姞鑽按鏁堟灉锛堢灛鏃舵晥鏋滅珛鍗崇粨绠楋紝鎸佺画鏁堟灉鍔犱负鐘舵€侊級
				for (MobEffectInstance effect : PotionUtils.getMobEffects(potion)) {
					if (effect.getEffect().isInstantenous()) {
						effect.getEffect().applyInstantenousEffect(player, player, player, effect.getAmplifier(), 1.0);
					} else {
						player.addEffect(new MobEffectInstance(effect));
					}
				}
				// 姝ｅ父楗敤杩旇繕绌虹幓鐠冪摱锛堝垱閫犳ā寮忎笉杩旇繕锛涜儗鍖呮弧鍒欐帀钀斤級
				if (!player.getAbilities().instabuild) {
					ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
					if (!player.getInventory().add(bottle)) {
						player.drop(bottle, false);
					}
					// 鍒涢€犳ā寮忎笉娑堣€楄鍐呰嵂姘达紝涔熶笉鐢熸垚棰濆瀹瑰櫒銆?
					potion.shrink(1);
					PotionBagScreenHandler.setStoredStack(stack, QUICK_SLOT, potion);
				}
			}
		}
		return stack; // 鑽按琚嬫湰韬笉琚秷鑰?
	}

	/** 楗敤璇绘潯鏃堕暱锛氫粎褰撳揩鎹锋爮涓洪ギ鐢ㄥ瀷鑽按鏃惰繑鍥?{@link #DRINK_TIME}锛屽惁鍒?0锛堟姇鎺峰瀷/绌轰负鍗虫椂/鏃犲姩浣滐級銆?*/
	@Override
	public int getUseDuration(ItemStack stack) {
		ItemStack potion = PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT);
		if (PotionTestTubes.isFilledTube(potion) || potion.is(MoonDustReliefItem.BOTTLE)) return (int) Math.ceil(potion.getUseDuration() * 1.15);
		return isDrinkable(potion) ? DRINK_TIME : 0;
	}

	/** 楗敤鍨嬭嵂姘存樉绀哄枬鑽姩浣滐紙鍚師鐗堥ギ鐢ㄧ矑瀛愪笌闊虫晥锛夛紝鍚﹀垯鏃犲姩浣溿€?*/
	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return isDrinkable(PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT)) ? UseAnim.DRINK : UseAnim.NONE;
	}

	/**
	 * 鎶曟幏涓€鐡惰嵂姘达細鐢熸垚鎶曟幏鑽按寮瑰皠鐗╋紙setItem 璁╁脊灏勭墿娓叉煋浣跨敤瀵瑰簲鑽按鐨勭汗鐞嗭級锛?
	 * 骞舵秷鑰?1 鐡跺啓鍥炶嵂姘村寘瀛樺偍銆備粎鏈嶅姟绔皟鐢ㄣ€?
	 */
	private boolean throwPotion(Level world, Player user, ItemStack bagStack, ItemStack potion) {
		if (potion.is(MoonDustReliefItem.SPLASH)) {
			if (!MoonDustReliefItem.SPLASH.spawnThrownPotion(world, user, potion)) return false;
			if (!user.getAbilities().instabuild) potion.shrink(1);
			PotionBagScreenHandler.setStoredStack(bagStack, QUICK_SLOT, potion);
			user.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(MoonDustReliefItem.SPLASH));
			return true;
		}
		// 鏃犻檺鍘嬬缉鑳介噺鑽按锛堝柗婧?婊炵暀鍨嬶級锛氬鐢ㄥ叾鑷韩鎶曟幏閫昏緫锛屾爣璁扮┖鐡跺厖鑳借€岄潪娑堣€楁暟閲?
		if (potion.getItem() instanceof InfiniteEnergyPotionItem inf) {
			InfiniteEnergyPotionItem.playThrowSound(world, user);
			inf.spawnThrownPotion(world, user);
			inf.markUsed(potion, world);
			PotionBagScreenHandler.setStoredStack(bagStack, QUICK_SLOT, potion);
			return true;
		}
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		ThrownPotion potionEntity = new ThrownPotion(world, user);
		potionEntity.setItem(potion.copyWithCount(1));
		potionEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
		world.addFreshEntity(potionEntity);
		potion.shrink(1);
		PotionBagScreenHandler.setStoredStack(bagStack, QUICK_SLOT, potion);
		return true;
	}

	/** 鏅€氭姇鎺疯嵂姘存槸鍚︿粛鍦ㄦ姇鎺烽棿闅斿喎鍗翠腑锛堝熀浜庤嵂姘磋 NBT 璁板綍鐨勭粨鏉熶笘鐣屾椂闂达級銆?*/
	private static boolean isThrowCoolingDown(ItemStack bag, Level world) {
		CompoundTag nbt = bag.getTag();
		return nbt != null && nbt.contains(NBT_THROW_END) && world.getGameTime() < nbt.getLong(NBT_THROW_END);
	}

	/**
	 * 姣?tick 鎶娿€屽揩鎹锋爮妲戒綅鑽按鑷韩鐨勫墿浣欏喎鍗淬€嶅悓姝ュ埌鑽按琚嬬殑 ItemCooldownManager 鐧借壊閬僵锛?
	 * <ul>
	 *   <li>鏃犻檺鑽按 鈫?鍏剁┖鐡跺厖鑳界粨鏉熸椂闂达紙鍚勫舰鎬佹椂闀夸笉鍚岋細楗敤 10s / 鍠锋簠 15s / 婊炵暀 20s锛?/li>
	 *   <li>鏅€氭姇鎺疯嵂姘?鈫?鑽按琚嬭褰曠殑鎶曟幏闂撮殧鍐峰嵈缁撴潫鏃堕棿锛?s锛?/li>
	 *   <li>鍏跺畠锛堟櫘閫氬彲楗敤鑽按/绌猴級鈫?鏃犲喎鍗?/li>
	 * </ul>
	 * 浠ャ€屽喎鍗寸粨鏉熺殑涓栫晫鏃堕棿銆嶄綔涓哄悓姝ヤ护鐗岋紙{@link #NBT_CD_TOKEN}锛夛紝浠呭綋浠ょ墝鍙樺寲锛堟崲鑽按 / 鍐嶆浣跨敤锛夋椂
	 * 鎵嶉噸鏂?set/remove 閬僵锛岄伩鍏嶆瘡 tick 閲嶇疆瀵艰嚧閬僵鍗℃弧銆備粎鏈嶅姟绔墽琛岋紝set 浼氳嚜鍔ㄥ悓姝ュ埌瀹㈡埛绔伄缃┿€?
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
		if (!world.isClientSide && entity instanceof Player player) {
			ItemStack potion = PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT);
			long time = world.getGameTime();
			long endTime = 0L;
			if (potion.getItem() instanceof InfiniteEnergyPotionItem) {
				endTime = InfiniteEnergyPotionItem.getRechargeEndTime(potion);
			} else if (isThrowable(potion)) {
				CompoundTag nbt = stack.getTag();
				if (nbt != null && nbt.contains(NBT_THROW_END)) {
					endTime = nbt.getLong(NBT_THROW_END);
				}
			}
			long token = endTime > time ? endTime : 0L; // 宸茶繃鏈熻涓烘棤鍐峰嵈
			long lastToken = stack.getTag() != null ? stack.getTag().getLong(NBT_CD_TOKEN) : 0L;
			if (token != lastToken) {
				stack.getOrCreateTag().putLong(NBT_CD_TOKEN, token);
				if (token > 0L) {
					player.getCooldowns().addCooldown(this, (int) (token - time));
				} else {
					player.getCooldowns().removeCooldown(this);
				}
			}
		}
		super.inventoryTick(stack, world, entity, slot, selected);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.potion_bag.tooltip").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.test_tube_stack").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.potion_bag.tooltip.controls").withStyle(ChatFormatting.DARK_GRAY));
		super.appendHoverText(stack, world, tooltip, context);
	}

	/**
	 * 鐗╁搧鍚嶅疄鏃跺弽鏄犲揩鎹锋姇鏀炬爮锛堟Ы浣?0锛夌殑鑽按锛氱┖鏃舵樉绀恒€岃嵂姘村寘锛堢┖锛夈€嶏紝鏈夎嵂姘存椂鏄剧ず銆岃嵂姘村寘锛堣嵂姘村悕锛夈€嶃€?
	 * 鍚嶇О鍙栬嚜蹇嵎鏍忚嵂姘磋嚜韬殑 {@link ItemStack#getHoverName()}锛堟櫘閫氳嵂姘村惈鑽晥鍚庣紑锛屾棤闄愯嵂姘翠负鍏舵湰鍦板悕锛夈€?
	 */
	@Override
	public Component getName(ItemStack stack) {
		ItemStack potion = PotionBagScreenHandler.getStoredStack(stack, QUICK_SLOT);
		if (potion.isEmpty()) {
			return Component.translatable("item.ssc_addon.potion_bag.empty");
		}
		return Component.translatable("item.ssc_addon.potion_bag.named", potion.getHoverName());
	}

	// ====== 蹇嵎鏀剁撼锛氬湪鍏跺畠鐣岄潰鎶婅嵂姘寸洿鎺ユ斁鍏ヨ嵂姘磋锛堜氦浜掗€昏緫涓庡師鐗堟敹绾宠涓€鑷达紝浼樺厛鏀惧叆闈炲揩鎹锋秷鑰楁爮锛?======

	/** 鑽按琚嬫Ы浣嶆€绘暟锛堜笌 {@link PotionBagScreenHandler} 涓€鑷达細妲戒綅 0 涓哄揩鎹锋姇鏀炬爮锛?-8 涓洪潪蹇嵎娑堣€楁爮锛夈€?*/
	private static final int BAG_SLOTS = 9;

	/** 鏄惁涓哄彲鏀惧叆鑽按琚嬬殑鑽按绫荤墿鍝侊紙涓庤嵂姘村寘 GUI 鐨勫彲鏀惧叆瑙勫垯涓€鑷达級銆?*/
	public static boolean isStorable(ItemStack stack) {
		return PotionTestTubes.hasCustomStackLimit(stack) || stack.getItem() instanceof PotionItem
				|| stack.getItem() instanceof SplashPotionItem
				|| stack.getItem() instanceof LingeringPotionItem
				|| stack.getItem() instanceof InfiniteEnergyPotionItem;
	}

	/** 鎵€鏈夊叆琚嬭矾寰勫叡鐢ㄥ閲忥細鏃犻檺1锛岃瘯绠?/16锛屾湀灏樼摱瑁?/power锛屾櫘閫氳嵂姘?銆?*/
	public static int maxPerSlot(ItemStack stack, @Nullable Player player) {
		if (stack.getItem() instanceof InfiniteEnergyPotionItem) return 1;
		if (PotionTestTubes.hasCustomStackLimit(stack)) return PotionTestTubes.stackLimit(stack, player);
		return 8;
	}

	/**
	 * 鎶?{@code source} 涓殑鑽按灏介噺鏀捐繘鑽按琚嬪苟鎵ｅ噺鍏舵暟閲忥紝杩斿洖瀹為檯鏀惧叆鐨勬暟閲忋€?
	 * 鏀剧疆浼樺厛绾э細鍏堝～闈炲揩鎹锋秷鑰楁爮锛堟Ы浣?1-8锛夛紝蹇嵎鎶曟斁鏍忥紙妲戒綅 0锛変粎鍦ㄥ墠鑰呮斁涓嶄笅鏃跺厹搴曪紱
	 * 鍚勫尯闂村唴鍧囧厛鍚堝苟鍒板悓绫绘湭婊″爢鍙狅紝鍐嶅崰鐢ㄧ┖妲戒綅銆傚爢鍙犺鍒欎笌鑽按鍖?GUI 涓€鑷达紙鏅€氳嵂姘存瘡鏍?8銆佹棤闄愯兘閲忚嵂姘存瘡鏍?1锛夈€?
	 */
	public static int insertIntoBag(ItemStack bag, ItemStack source) {
		return insertIntoBag(bag, source, null);
	}

	public static int insertIntoBag(ItemStack bag, ItemStack source, @Nullable Player player) {
		if (source.isEmpty() || !isStorable(source)) {
			return 0;
		}
		int perSlot = maxPerSlot(source, player);
		ItemStack[] slots = new ItemStack[BAG_SLOTS];
		for (int i = 0; i < BAG_SLOTS; ++i) {
			slots[i] = PotionBagScreenHandler.getStoredStack(bag, i);
		}
		// 浼樺厛闈炲揩鎹锋秷鑰楁爮锛堟Ы浣?1-8锛夛紝蹇嵎鎶曟斁鏍忥紙妲戒綅 0锛夊厹搴?
		int inserted = fillRange(slots, source, perSlot, 1, BAG_SLOTS);
		inserted += fillRange(slots, source, perSlot, QUICK_SLOT, QUICK_SLOT + 1);
		if (inserted > 0) {
			for (int i = 0; i < BAG_SLOTS; ++i) {
				PotionBagScreenHandler.setStoredStack(bag, i, slots[i]);
			}
		}
		return inserted;
	}

	/** 鍦?{@code [from, to)} 妲戒綅鍖洪棿鍐呮斁鍏ヨ嵂姘达細鍏堝悎骞跺埌鍚岀被鏈弧鍫嗗彔銆佸啀鍗犵敤绌烘Ы浣嶏紱鎵ｅ噺 {@code source} 骞惰繑鍥炴斁鍏ユ暟閲忋€?*/
	private static int fillRange(ItemStack[] slots, ItemStack source, int perSlot, int from, int to) {
		int inserted = 0;
		for (int i = from; i < to && !source.isEmpty(); ++i) {
			ItemStack slot = slots[i];
			if (!slot.isEmpty() && slot.getCount() < perSlot && ItemStack.isSameItemSameTags(slot, source)) {
				int add = Math.min(perSlot - slot.getCount(), source.getCount());
				slot.grow(add);
				source.shrink(add);
				inserted += add;
			}
		}
		for (int i = from; i < to && !source.isEmpty(); ++i) {
			if (slots[i].isEmpty()) {
				int add = Math.min(perSlot, source.getCount());
				slots[i] = source.copyWithCount(add);
				source.shrink(add);
				inserted += add;
			}
		}
		return inserted;
	}
}
