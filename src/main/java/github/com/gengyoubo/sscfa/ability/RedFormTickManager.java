package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashSet;
import java.util.Set;

/**
 * 缁绢澀濞囨?/ SP 瑜般垺鈧礁鎳嗛張鐔尖偓鏄忕帆缁狅紕鎮婇崳顭掔礄jackcooper閿涘鈧?
 *
 * <p>閸樼喍璐?{@code RedFormTickMixin} 鐎?{@code ServerPlayerEntity.tick} 閻?HEAD 濞夈劌鍙嗛敍鍫熺槨閻溾晛顔嶅В?tick閿涘绱?
 * 2026-09-09 鏉╀胶些閸?{@code ServerTickEvents.START_WORLD_TICK} 娑撴牜鏅痪褌绨ㄦ禒鍫曚憾閸樺棌鈧柡鈧?
 * 闁槒绶張顒€姘ㄧ敮?{@code player.age % 20 == 0} 娑撯偓缁夋帡妫幒褝绱濈悰灞艰礋闂嗚泛妯婂鍌︾礉閸掔姵甯€閺堚偓妤傛﹢顣跺▔銊ュ弳閻愬箍鈧?/p>
 *
 * <p>閼卞矁鐭楅敍鍫熺槨缁夋帊绔村▎鈽呯礉闁秴宸婚崥鍕瑯閻ｅ瞼甯虹€硅绱氶敍?/p>
 * <ul>
 *   <li>SP 娴ｅ潡鐡熺拠鍛嫽娑斿婀€ 5% 濮掑倻宸奸崣妯块煩缁绢澀濞囨鏃撶礄10 閸掑棝鎸撶搾鍛閸ョ偤鈧偓閿涘瞼鐭戠€涙劖甯洪幎銈忕礆閿?/li>
 *   <li>SP 閹妇浼掗懛顏勫З閸欐垶鏂?閸ョ偞鏁瑰▽鑽ゆ灍閺?濡?)娑撳骸鏁遍悧鍥ㄦ簚(濡?)閿?/li>
 *   <li>缁绢澀濞囨鏃囧殰閸斻劌褰傞弨?閸ョ偞鏁归懡顖涙寜鐞?濡?)閿涘牆娲栭弨鑸垫閸婃帒鍤崘鍛啇閻椻晪绱氶敍?/li>
 *   <li>缁绢澀濞囨鏃囩Т閺冭埖鐖ｇ粵鐐閻炲棔绗岄崶鐐衡偓鈧妴?/li>
 * </ul>
 */
public final class RedFormTickManager {
	private RedFormTickManager() {
	}

	// 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓婚崐纭风礆
	private static final BalanceReader BAL = new BalanceReader("abilities.red_form");

	public static void init() {
		github.com.gengyoubo.sscfa.compat.FormChangeEvents.register((player, previous, current) -> {
			if (!player.level().isClientSide && previous != null && current != null
					&& previous.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED)
					&& !current.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED)) {
				player.getTags().removeIf(tag -> tag.startsWith("ssc_addon_red_expire:"));
			}
		});
	}

	public static void tick(ServerPlayer player) {
		// 娑撯偓缁夋帡妫幒褝绱欐稉搴″斧 per-player tick 濞夈劌鍙嗘稉鈧懛杈剧礆
		if (player.tickCount % 20 != 0) {
			return;
		}

		boolean isCursedMoon = net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isCursedMoonDay(player.level());

		// 闂堢偠鐦忛崪鎺嶇閺堝牊绔婚梽銈呭綁闊偄鐨剧拠鏇熺垼鐠?
		if (!isCursedMoon && player.getTags().contains("ssc_addon_red_attempted")) {
			player.getTags().remove("ssc_addon_red_attempted");
		}

		// 閼筋垱鎸夌悮瀣偓鏄忕帆閺嶅洩顔?
		net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
		boolean isRedForm = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED);

		// SP 瑜般垺鈧?+ 鐠囧懎鎷绘稊瀣箑閸欐闊╃痪顫▏姒勬棑绱?%閿?
		if (currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_SP) && isCursedMoon && !player.getTags().contains("ssc_addon_red_attempted")) {
			player.addTag("ssc_addon_red_attempted");
			if (player.getRandom().nextFloat() < BAL.d("transform_chance", 0.05)) {
				ResourceLocation redFormId = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED;
				net.onixary.shapeShifterCurseForge.form.FormDefinition redForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(redFormId);
				if (redForm != null) {
					net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, redForm.id());

					// 10 閸掑棝鎸?= 12000 tick閿涘牓绮拋銈忕幢balance 閸欘垵顩惄?duration_ticks閿?
					long expireTime = player.level().getGameTime() + BAL.i("duration_ticks", 12000);
					player.addTag("ssc_addon_red_expire:" + expireTime);

					player.displayClientMessage(Component.translatable("message.ssc_addon.red_transformation_special").withStyle(ChatFormatting.GREEN), false);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
					return; // 閸欐闊╅幋鎰閸氬海娲块幒銉ㄧ箲閸?
				}
			}
		}

		// 娑撱倛鐤嗛悪顒傜彌閼冲苯瀵橀幍顐ｅ伎閸氬牆鑻熸稉鍝勫礋鏉烆噯绱欓崥灞肩濞嗏€虫儕閻滎垰鍞撮幐澶婅埌閹椒绗侀崗鍐槱閻炲棔绗佺猾鑽ゅ⒖閸濅緤绱氶敍?
		// 濮?20t 鐏忔垶澹傛稉鈧弫纾嬬枂 41 閺嶇》绱辩悰灞艰礋缁涘鐜敍鍫滅瑏閻椻晛鎼ф禍鎺嶇瑝闁插秴褰旈敍灞芥礀閺€鍫曗偓鏄忕帆閻撗勬＋閿涘鈧?
		boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
		if (isAllaySp) {
			placeFormItemSafe(player, 0, github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_HEAL_WAND.get());
			placeFormItemSafe(player, 1, github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get());
		} else {
			for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_HEAL_WAND.get())) {
					player.getInventory().setItem(i, ItemStack.EMPTY);
				} else if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get())) {
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
		}

		// 缁绢澀濞囨鏃撶窗閼奉亜濮╅崣鎴炴杹閼筋垱鎸夌悮?8)閿涙盯娼拠銉ヨ埌閹礁鍨崶鐐存暪閿涘牆鈧帒鍤崘鍛啇閻椻晛鎮楃粔濠氭珟閿?
		if (isRedForm) {
			placeFormItemSafe(player, 8, github.com.gengyoubo.sscfa.init.AddonItems.POTION_BAG.get());
		} else {
			for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.POTION_BAG.get())) {
					if (stack.getTag() != null && stack.hasTag() && stack.getTag().contains("Items", 9)) {
						ListTag list = stack.getTag().getList("Items", 10);
						for (int j = 0; j < list.size(); ++j) {
							CompoundTag itemTag = list.getCompound(j);
							ItemStack contentStack = ItemStack.of(itemTag);
							if (!contentStack.isEmpty()) {
								player.drop(contentStack, false, true);
							}
						}
					}
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
		}

		// 缁绢澀濞囨鏃囩Т閺冭泛娲栭柅鈧?
		Set<String> tagsToRemove = new HashSet<>();
		boolean shouldRevert = false;
		long currentTime = player.level().getGameTime();

		for (String tag : player.getTags()) {
			if (tag.startsWith("ssc_addon_red_expire:")) {
				boolean pendingRed = net.onixary.shapeShifterCurseForge.form.TransformManager.isTransforming(player);
				if (!isRedForm) {
					if (!pendingRed) tagsToRemove.add(tag);
					continue;
				}
				try {
					long expireTime = Long.parseLong(tag.split(":")[1]);
					if (currentTime >= expireTime) {
						shouldRevert = true;
						tagsToRemove.add(tag);
					}
				} catch (NumberFormatException ignored) {
					tagsToRemove.add(tag); // 閺冪姵鏅ラ弽鍥╊劮閻╁瓨甯寸粔濠氭珟
				}
			}
		}

		if (!tagsToRemove.isEmpty()) {
			for (String tag : tagsToRemove) {
				player.getTags().remove(tag);
			}
		}

		if (shouldRevert) {
			ResourceLocation spFormId = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_SP;
			net.onixary.shapeShifterCurseForge.form.FormDefinition spForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(spFormId);
			if (spForm != null) {
				// 閻?immediatelyTransform 闁灝鍘ら崝銊ф暰
				net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, spForm.id());

				// 閻у€熷缁帒鐡欐径褔鍣虹憰鍡欐磰閻溾晛顔嶉敍鍧ner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夌粭顑跨娴滆櫣袨闁儻顔€閿涘苯褰夐煬顐ｇ川閸戣桨绮禍楦款潒鐟欐帒甯弽鍑ょ礆
				if (player.level() instanceof ServerLevel serverWorld) {
					github.com.gengyoubo.sscfa.network.DecorationParticles.forced(serverWorld, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 100, 0.5, 1.0, 0.5, 0.1, 512);
					github.com.gengyoubo.sscfa.network.DecorationParticles.forced(serverWorld, player, ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 1.0, 0.5, 0.1, 512);
				}

				player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				player.removeEffect(MobEffects.JUMP);

				player.displayClientMessage(Component.translatable("message.ssc_addon.red_revert_timeout").withStyle(ChatFormatting.GREEN), false);
			}
		}
	}

	/**
	 * 鐎瑰鍙忛崷鐗堝Ω閹稿洤鐣捐ぐ銏♀偓浣哄⒖閸濅焦鏂佹潻娑樻祼鐎规碍蝎娴ｅ稄绱濋柆鍨帳鐟曞棛娲婇悳鈺侇啀閸樼喐婀侀悧鈺佹惂閿?
	 * 閳剁姷娲伴弽鍥ㄐ鍙夋Ц鐠囥儳澧块崫浣稿灟娑撳秴濮╅敍娑掓喗閸樼喓澧块崫浣告値楠炴儼绻橀懗灞藉瘶閸氬瞼顫掗崼鍡幢閳躲垹澧挎担娆愬缁岀儤蝎閿涙稈鎳€娴犲秴澧挎担娆庢丢閸﹂绗傞敍宀€绮锋稉宥夋饯姒涙ê鍨归梽銈冣偓?
	 */
	private static void placeFormItemSafe(ServerPlayer player, int targetSlot, Item formItem) {
		Inventory inv = player.getInventory();
		ItemStack existing = inv.getItem(targetSlot);

		if (existing.is(formItem)) {
			return;
		}

		ItemStack moved = existing.copy();
		inv.setItem(targetSlot, ItemStack.EMPTY);

		if (!moved.isEmpty()) {
			for (int i = 0; i < inv.items.size() && !moved.isEmpty(); ++i) {
				if (i == targetSlot) continue;
				ItemStack slotStack = inv.items.get(i);
				if (slotStack.isEmpty()) continue;
				if (!ItemStack.isSameItemSameTags(slotStack, moved)) continue;
				int room = slotStack.getMaxStackSize() - slotStack.getCount();
				if (room <= 0) continue;
				int merge = Math.min(room, moved.getCount());
				slotStack.grow(merge);
				moved.shrink(merge);
			}

			if (!moved.isEmpty()) {
				for (int i = 0; i < inv.items.size() && !moved.isEmpty(); ++i) {
					if (i == targetSlot) continue;
					if (!inv.items.get(i).isEmpty()) continue;
					inv.items.set(i, moved.copy());
					moved.setCount(0);
				}
			}

			if (!moved.isEmpty()) {
				player.drop(moved, false, true);
			}
		}

		inv.setItem(targetSlot, new ItemStack(formItem));
	}
}
