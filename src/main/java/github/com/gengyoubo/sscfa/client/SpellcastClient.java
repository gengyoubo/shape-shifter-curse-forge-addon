package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.SpellCastFeedback;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.spell.SpellNumbers;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import github.com.gengyoubo.sscfa.spell.SpellbookData;

/**
 * 月尘魔法书施法客户端检测器（jackcooper）。仅在佩戴魔法书时生效。
 * <p>施法键 / 7 个直达键：上升沿检测 → C2S 施法包（带槽位 index）。切换键 + 滚轮：由
 * {@code SpellcastMouseScrollMixin} 调用 {@link #cycleSelected} 切换当前选中槽并同步服务端。</p>
 * <p><b>法力不足三连击降档</b>（阶段 C §6.4 变体，用户 2026-09-18 定稿）：选中槽为高阶卷轴且
 * 书法力不足时，1 秒内连按三次施法键 → 第三次改为发送「临时降档施放」包（服务端降到付得起的
 * 最高档，本次 CD ×1.2 惩罚）；不写卷轴 NBT、下次施法仍按原档。法力充足时第一次按键即正常施放。</p>
 * <p><b>按住瞄准型法术</b>（{@link Spell#getAimMaxRange()} > 0，如陨火术）：按住施法键时
 * 在准星落点本地粒子圈持续预览（随准星实时移动、半径含等级缩放），松开才发送施法包；
 * 冷却中不预览不发送。直达键仍为按下立即施放。所有伤害/冷却/法力判定在服务端。</p>
 */
public final class SpellcastClient {
	private static int selectedSlot = 0;
	/** ??tick 是否已装备书：无→有沿触发从??NBT 恢复选中槽（重进游戏不丢选择?026-09-24）??*/
	private static boolean bookWasEquipped;
	private static final boolean[] wasDirectPressed = new boolean[7];
	private static boolean wasCastPressed = false;
	private static int gestureToken;
	private static int gestureKey = -1;
	private static int gestureSlot = -1;
	private static boolean selectingTarget;
	private static net.minecraft.world.entity.LivingEntity lunarTarget;
	private static boolean targetingDowngraded;
	private static ItemStack targetingScroll = ItemStack.EMPTY;
	private static int targetingLevel = 1;
	private static net.minecraft.client.multiplayer.ClientLevel targetingWorld;
	private static final github.com.gengyoubo.sscfa.spell.SpellCastingRules.InputGuard inputGuard =
			new github.com.gengyoubo.sscfa.spell.SpellCastingRules.InputGuard();
	private static int cancelToken = -1;
	private static final SpellCastingRules.TriplePress downgradePresses = new SpellCastingRules.TriplePress();
	private record DowngradeContext(int slot, int level, int cost, net.minecraft.nbt.CompoundTag book) {}

	private SpellcastClient() {
	}

	public static void register() {
		github.com.gengyoubo.sscfa.network.AddonNetwork.registerClientReceiver(
				github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.SYNC,
				buf -> github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.applyClientSnapshot(buf.readNbt()));
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
				(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) ->
						github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.applyClientSnapshot(null));
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
				(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
						github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.applyClientSnapshot(null));
		github.com.gengyoubo.sscfa.client.hud.SpellCastHud.register();
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
				(net.minecraftforge.event.TickEvent.ClientTickEvent event) -> {
					if (event.phase == net.minecraftforge.event.TickEvent.Phase.END)
						onClientTick(net.minecraft.client.Minecraft.getInstance());
				});
	}

	/** 客户端当前佩戴的魔法书（未装备返??null；??tick 缓存，tick 检测??HUD 帧渲染共读）??*/
	public static ItemStack getEquippedBook() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		// 每帧全饰品扫?+ Curios 反射兜底链收敛到 ClientTickCache 的??tick 一??
		return ClientTickCache.equippedBook();
	}

	public static boolean hasBookEquipped() {
		ItemStack book = getEquippedBook();
		return book != null && !book.isEmpty();
	}

	public static boolean isSwitchKeyDown() {
		return SpellcastKeybindings.KEY_SWITCH != null && SpellcastKeybindings.KEY_SWITCH.isDown();
	}

	public static int getSelectedSlot() {
		return selectedSlot;
	}

	/** 滚轮切换当前选中槽（首尾相连）。dir=+1 下一个，-1 上一个。由滚??mixin 调用?*/
	public static void cycleSelected(int dir) {
		ItemStack book = getEquippedBook();
		if (book == null || book.isEmpty()) {
			return;
		}
		// 只在已装备卷轴的槽之间循环（跳过空槽）；全空不动?
		int next = SpellbookData.nextFilledSlot(book, selectedSlot, dir);
		if (next < 0 || next == selectedSlot) {
			return;
		}
		selectedSlot = next;
		sendSelect(selectedSlot);
		cancelInputForSelection();
	}

	/**
	 * 循环当前选中槽的施放档位（低阶选档，阶??C ??.4）??
	 * 循环序：满????满??1 ??????1 ??回满档；一级卷轴无意义不动作?
	 * 服务端权威写入，这里只发请求。（保留供潜?施法键等后续入口复用?
	 */
	public static void cycleCastLevel(int slot) {
		ItemStack book = getEquippedBook();
		if (book == null || book.isEmpty()) {
			return;
		}
		ItemStack scroll = SpellbookData.getScroll(book, slot);
		if (ScrollData.getSpell(scroll) == null) {
			return;
		}
		int scrollLevel = ScrollData.getLevel(scroll);
		if (scrollLevel <= 1) {
			return; // 一级卷轴无可降??
		}
		int current = ScrollData.getCastLevel(scroll);
		int next = current <= 1 ? scrollLevel : current - 1;
		sendSetCastLevel(slot, next);
	}

	private static boolean handleTriplePressDowngrade(LocalPlayer player, ItemStack book, int slot, int level, int cost,
			Spell spell, ItemStack scroll, int payable) {
		// 书??经验的正常同步不打断连按；换槽、换卷轴、换法阵或手选等级则重新计数??
		var source = book.getTag() == null ? new net.minecraft.nbt.CompoundTag() : book.getTag().copy();
		source.remove(SpellbookData.NBT_MANA);
		source.remove(SpellbookData.NBT_EXP);
		source.remove("Exp");
		source.remove(SpellbookData.NBT_SELECTED);
		int remaining = downgradePresses.press(new DowngradeContext(slot, level, cost, source), net.minecraft.Util.getMillis());
		if (remaining == 0) return true;
		int lowerCost = SpellNumbers.finalManaCost(spell, book, player, payable, scroll);
		player.displayClientMessage(Component.translatable("message.ssc_addon.spell.downgrade_hint_details",
				cost, SpellbookData.getMana(book), remaining, payable, lowerCost), true);
		return false;
	}

	private static void onClientTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			resetKeys();
			github.com.gengyoubo.sscfa.client.hud.SpellCastHud.clear();
			return;
		}
		boolean castPressed = SpellcastKeybindings.KEY_CAST != null && SpellcastKeybindings.KEY_CAST.isDown();
		boolean anyPressed = castPressed;
		for (KeyMapping key : SpellcastKeybindings.KEY_DIRECT) anyPressed |= key != null && key.isDown();
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) {
			downgradePresses.reset();
			clearTargetSelection();
			gestureKey = -1;
			cancelToken = -1;
			inputGuard.block();
			updatePressedKeys(castPressed);
			return;
		}
		if (client.screen != null) {
			downgradePresses.reset();
			if (gestureKey >= 0 && !selectingTarget) sendRelease(gestureToken);
			clearTargetSelection();
			if (cancelToken >= 0) sendCancelHold(cancelToken, false);
			cancelToken = -1;
			github.com.gengyoubo.sscfa.client.hud.SpellCastHud.clearLocalCancelling();
			gestureKey = -1;
			inputGuard.block();
		}
		if (inputGuard.consume(anyPressed || client.screen != null)) {
			updatePressedKeys(castPressed);
			return;
		}
		// 松手拍：撤销取消??+ 立即撒销本地红字（取消显示跟随按键按住状态，不残留到读条结束??
		if (cancelToken >= 0 && !castPressed) {
			sendCancelHold(cancelToken, false);
			cancelToken = -1;
			github.com.gengyoubo.sscfa.client.hud.SpellCastHud.clearLocalCancelling();
		}
		ItemStack book = getEquippedBook();
		if (book == null || book.isEmpty()) {
			downgradePresses.reset();
			bookWasEquipped = false; // 卸书：下次重新装备时?NBT 初始化选中??
			if (selectingTarget) {
				clearTargetSelection();
				gestureKey = -1;
				inputGuard.block();
			}
			if (gestureKey >= 0 && !keyPressed(gestureKey)) {
				sendRelease(gestureToken);
				gestureKey = -1;
			}
			updatePressedKeys(castPressed);
			return;
		}
		if (!bookWasEquipped) {
			// 重进游戏/新装备书：从?NBT 恢复上次选中的槽（修复「选了第三个法术，重进变回第一个」—?
			// 服务?Selected 一直有存，客户端静态字段却??0 起步从未读过它）?
			bookWasEquipped = true;
			int saved = SpellbookData.getSelectedSlot(book);
			selectedSlot = SpellbookData.hasScroll(book, saved) ? saved : Math.max(0, SpellbookData.firstFilledSlot(book));
		}
		int count = SpellbookData.getSlotCount(book);
		if (selectedSlot >= count) {
			selectedSlot = 0;
		}
		// 选中槽为空（被取走卷?/ 换??/ 初始化）??归位到第一个非空槽，保??HUD 与施法键始终指向有魔法的??
		if (!SpellbookData.hasScroll(book, selectedSlot)) {
			int first = SpellbookData.firstFilledSlot(book);
			if (first >= 0) {
				selectedSlot = first;
			}
		}

		var active = github.com.gengyoubo.sscfa.client.hud.SpellCastHud.getState();
		if (selectingTarget && (active != null || client.level != targetingWorld || selectedSlot != gestureSlot
				|| !ItemStack.matches(SpellbookData.getScroll(book, gestureSlot), targetingScroll)
				|| !player.isAlive() || player.isSpectator())) {
			cancelInputForSelection();
			updatePressedKeys(castPressed);
			return;
		}
		boolean handled = false;
		for (int index = 0; index < 7; index++) {
			if (!keyPressed(index + 1) || wasDirectPressed[index] || index >= count) continue;
			if (index != selectedSlot && (active != null || gestureKey >= 0)) {
				selectedSlot = index;
				sendSelect(index);
				cancelInputForSelection();
			} else if (active == null && gestureKey < 0) {
				selectedSlot = index;
				sendSelect(index);
				startGesture(player, book, index, index + 1);
			}
			handled = true;
			break;
		}
		if (gestureKey >= 0 && !keyPressed(gestureKey)) {
			if (selectingTarget) {
				// First request to the server: it validates and locks its own raycast before charging.
				if (targetingDowngraded) sendCastDowngraded(gestureSlot);
				else sendCast(gestureSlot);
				clearTargetSelection();
			} else sendRelease(gestureToken);
			gestureKey = -1;
		}
		if (!handled && castPressed && !wasCastPressed) {
			if (active != null) {
				// 锁定态（领域壳扩张??爆裂红白球生成后??026-09-23）：不可取消，不发取消包??
				// 不打本地取消标（避免「取消中」红字永驻），HUD 倒计时保持红显?
				if (!active.solo() && !active.locked()
						&& (active.mode() == github.com.gengyoubo.sscfa.spell.SpellCastingRules.Mode.AUTOMATIC
						|| active.mode() == github.com.gengyoubo.sscfa.spell.SpellCastingRules.Mode.RELEASE)) {
					cancelToken = active.token();
					sendCancelHold(cancelToken, true);
					// 本地即时红字反馈（服务端 cancelTicks 校准包最?20t 后才到）
					github.com.gengyoubo.sscfa.client.hud.SpellCastHud.markLocalCancelling();
				}
			} else if (gestureKey < 0) startGesture(player, book, selectedSlot, 0);
		}
		if (gestureKey >= 0 && gestureSlot >= 0 && active != null && active.token() == gestureToken
				&& !active.released() && isAimSpell(book, gestureSlot)) {
			updateAimPreview(client, player, book, gestureSlot);
		}
		updateLunarTarget(player);
		updatePressedKeys(castPressed);
	}

	private static void startGesture(LocalPlayer player, ItemStack book, int slot, int key) {
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PURIFIED.get())) {
			downgradePresses.reset();
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.spell.purified_blocked"), true);
			return;
		}
		ItemStack scroll = SpellbookData.getScroll(book, slot);
		Spell spell = ScrollData.getSpell(scroll);
		if (spell == null) {
			downgradePresses.reset();
			return;
		}
		long cooldown = github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.getEffectiveCooldownEnd(player, scroll)
				- player.level().getGameTime();
		if (cooldown > 0) {
			downgradePresses.reset();
			player.displayClientMessage(SpellCastFeedback.cooldown(cooldown).copy().withStyle(net.minecraft.ChatFormatting.RED), true);
			return;
		}
		int level = ScrollData.getCastLevel(scroll);
		int cost = SpellNumbers.finalManaCost(spell, book, player, level, scroll);
		boolean downgraded = false;
		downgradePresses.reset(); // Collapse has no mana affordability gate.

		// 只有获准正常起手或完成三连按才建立手势；第三次仍须保留松??持续施法链路?
		gestureToken = (gestureToken + 1) & Integer.MAX_VALUE;
		gestureKey = key;
		gestureSlot = slot;
		if (spell.requiresTargetBeforeChannel()) {
			selectingTarget = true;
			targetingDowngraded = downgraded;
			targetingScroll = scroll.copy();
			targetingLevel = level;
			targetingWorld = Minecraft.getInstance().level;
			updateLunarTarget(player);
			return;
		}
		if (downgraded) sendCastDowngraded(slot);
		else sendCast(slot);
	}

	private static boolean keyPressed(int key) {
		KeyMapping binding = key == 0 ? SpellcastKeybindings.KEY_CAST : SpellcastKeybindings.KEY_DIRECT[key - 1];
		return binding != null && binding.isDown();
	}

	private static void updatePressedKeys(boolean castPressed) {
		wasCastPressed = castPressed;
		for (int index = 0; index < 7; index++) wasDirectPressed[index] = keyPressed(index + 1);
	}

	private static void cancelInputForSelection() {
		if (gestureKey >= 0 && !selectingTarget) sendRelease(gestureToken);
		clearTargetSelection();
		if (cancelToken >= 0) {
			sendCancelHold(cancelToken, false);
			github.com.gengyoubo.sscfa.client.hud.SpellCastHud.clearLocalCancelling();
		}
		gestureKey = -1;
		cancelToken = -1;
		downgradePresses.reset();
		inputGuard.block();
	}

	private static void resetKeys() {
		clearTargetSelection();
		gestureKey = -1;
		gestureSlot = -1;
		cancelToken = -1;
		github.com.gengyoubo.sscfa.client.hud.SpellCastHud.clearLocalCancelling();
		inputGuard.reset();
		downgradePresses.reset();
		wasCastPressed = false;
		for (int i = 0; i < 7; i++) {
			wasDirectPressed[i] = false;
		}
	}

	private static void updateLunarTarget(LocalPlayer player) {
		lunarTarget = null;
		if (selectingTarget && gestureKey >= 0 && keyPressed(gestureKey)
				&& ScrollData.getSpell(targetingScroll) instanceof github.com.gengyoubo.sscfa.spell.spells.LunarPhaseSpell) {
			lunarTarget = RuneCastContext.with(RuneScheme.modifiers(player,targetingScroll,ScrollData.getCastLevel(targetingScroll)),()->github.com.gengyoubo.sscfa.spell.spells.LunarPhaseSpell.raycastEntity(player));
		}
	}

	/** Exact current selection, with immediate release/screen/world invalidation. */
	public static boolean isLunarTarget(net.minecraft.world.entity.Entity entity) {
		var client = Minecraft.getInstance();
		return entity != null && entity == lunarTarget && selectingTarget
				&& client.level == targetingWorld && client.screen == null
				&& client.player != null && client.player.isAlive() && entity.isAlive()
				&& gestureKey >= 0 && keyPressed(gestureKey)
				&& !github.com.gengyoubo.sscfa.client.renderer.DomainRenderer.blocksTargetingClient(entity);
	}

	private static void clearTargetSelection() {
		lunarTarget = null;
		selectingTarget = false;
		targetingDowngraded = false;
		targetingScroll = ItemStack.EMPTY;
		targetingLevel = 1;
		targetingWorld = null;
	}

	/** Local-only preview; nothing about this aim stage is broadcast to observers. */
	public static Vec3 targetSelectionPreview() {
		var client = Minecraft.getInstance();
		if (!selectingTarget || client.player == null || client.level == null || client.level != targetingWorld || client.screen != null
				|| gestureKey < 0 || !keyPressed(gestureKey)) return null;
		ItemStack book = getEquippedBook();
		if (book == null || book.isEmpty() || selectedSlot != gestureSlot
				|| !ItemStack.matches(SpellbookData.getScroll(book, gestureSlot), targetingScroll)) return null;
		Spell spell = ScrollData.getSpell(targetingScroll);
		return spell == null ? null : RuneCastContext.with(RuneScheme.modifiers(client.player,targetingScroll,targetingLevel),()->Spell.computeAimImpact(client.player, spell.getAimMaxRange(client.player,targetingLevel)));
	}

	/** 当前槽是否为按住瞄准型法术（getAimMaxRange>0，如陨火术）?*/
	private static boolean isAimSpell(ItemStack book, int slot) {
		Spell spell = ScrollData.getSpell(SpellbookData.getScroll(book, slot));
		return spell != null && spell.getAimMaxRange() > 0;
	}

	/**
	 * 按住瞄准预览（纯客户端本地，零网络开销）：??2t 在准星落点撒一圈火焰粒??
	 * （与服务端陨火预警圈同视觉语言），随准星实时移动?
	 * 月相（实体目标型）：不撒粒子圈，改为对准星射线命中的实体持续紫色描边
	 * （本地高亮表短租约续约，松手停止续约后自然消失；与服务端 raycastEntity 同几何）??
	 * 调用方已保??CD/法??落点均就绪（服务端施法时权威重验），此处无需重复校验??
	 */
	private static void updateAimPreview(Minecraft client, LocalPlayer player, ItemStack book, int slot) {
		ItemStack scroll = SpellbookData.getScroll(book, slot);
		Spell spell = ScrollData.getSpell(scroll);
		if (spell == null) {
			return;
		}
		if (client.level == null || client.level.getGameTime() % 2 != 0) {
			return;
		}
		var active = github.com.gengyoubo.sscfa.client.hud.SpellCastHud.getState();
		int level = active != null && active.spell() == spell ? active.level() : ScrollData.getCastLevel(scroll);
		var runeModifiers=RuneScheme.modifiers(player,scroll,level);
        double radius = RuneCastContext.with(runeModifiers,github.com.gengyoubo.sscfa.spell.FormationData.sumSpaceRangeMultiplier(book,spell.getElement()),()->spell.getAimRadius(level));
		if (radius <= 0) {
			return;
		}
		Vec3 impact = RuneCastContext.with(runeModifiers,github.com.gengyoubo.sscfa.spell.FormationData.sumSpaceRangeMultiplier(book,spell.getElement()),()->Spell.computeAimImpact(player, spell.getAimMaxRange(player,level)));
		if (impact == null) {
			return;
		}
		// 圈粒子按法术系别取色（火=火??/ 虚??传送门?/ 其它默认火焰?
		net.minecraft.core.particles.ParticleOptions ringParticle = ParticleTypes.FLAME;
		if (spell.getElement() == github.com.gengyoubo.sscfa.spell.FormationElement.VOID) {
			ringParticle = ParticleTypes.PORTAL;
		}
		// 火焰圈勾??AOE 范围（旋转角随时间缓慢流动，与服务端预警圈同款动感??
		int ringCount = (int) Math.min(40, Math.max(16, radius * 10));
		double baseAngle = (client.level.getGameTime() / 2) * 0.15;
		for (int i = 0; i < ringCount; i++) {
			double angle = (i * 2 * Math.PI / ringCount) + baseAngle;
			client.level.addParticle(ringParticle,
					impact.x + Math.cos(angle) * radius * 0.95,
					impact.y + 0.1,
					impact.z + Math.sin(angle) * radius * 0.95,
					0.0, 0.0, 0.0);
		}
		// 中心烟柱标记落心
		client.level.addParticle(ParticleTypes.LARGE_SMOKE, impact.x, impact.y + 0.3, impact.z, 0, 0.01, 0);
	}

	private static void sendCast(int slot) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(slot);
		buf.writeVarInt(gestureToken);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SPELL_CAST, buf);
	}

	private static void sendSelect(int slot) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(slot);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SPELL_SELECT, buf);
	}

	private static void sendSetCastLevel(int slot, int castLevel) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(slot);
		buf.writeVarInt(castLevel);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SPELL_SET_CAST_LEVEL, buf);
	}

	/** 三连击触发的临时降档施放（服务端自动选付得起的最高??+ CD ??.2 惩罚）?*/
	private static void sendCastDowngraded(int slot) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(slot);
		buf.writeVarInt(gestureToken);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(SscAddonNetworking.PACKET_SPELL_CAST_DOWNGRADED, buf);
	}

	private static void sendRelease(int token) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(token);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(github.com.gengyoubo.sscfa.spell.SpellChannelManager.RELEASE, buf);
	}

	private static void sendCancelHold(int token, boolean held) {
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		buf.writeVarInt(token);
		buf.writeBoolean(held);
		github.com.gengyoubo.sscfa.network.AddonNetwork.sendToServer(github.com.gengyoubo.sscfa.spell.SpellChannelManager.CANCEL_HOLD, buf);
	}
}
