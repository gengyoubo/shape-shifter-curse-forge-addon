package github.com.gengyoubo.sscfa.client.hud;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.config.SSCAddonClientConfig;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;

/**
 * 法术蓄力?HUD（jackcooper?026-09-19 重做）：替换原准星下方横条为
 * CD 条对侧（默认贴屏幕右缘、与左中 CD 条同高镜像对称）的竖向蓄力面板?
 * <ul>
 * <li><b>平时隐藏</b>：开始施法时面板从屏幕侧缘外滑入?.3s ease-out，先快后慢）?/li>
 * <li><b>条旁倒计?/b>：窄条（14px）外侧显示剩余秒数（?2.4），蓄满后显示就绪；数字随条一起滑入；</li>
 * <li><b>进度填充</b>：空框贴图打?+ 满图按进度自下而上裁切显示（注水式）；</li>
 * <li><b>退?/b>：正常释放完成后原地停留 0.5s，再 0.3s ease-in（先慢后快）加速滑出；
 * 被打断时不停留、快速滑出（0.15s），避免误导性残留；</li>
 * <li><b>零读条不显示</b>：instant 档（空间跳跃）无读条过程，不弹条；basic 档正常显示；</li>
 * <li><b>位置可编?/b>：chargeBarPosType/OffsetX/OffsetY/chargeMirrorRight（仿 CD 条左右对换，贴左时材质镜像），位置编辑器可视化调整?/li>
 * </ul>
 * 数据源：服务端起手发的全?{@code spell_channel_state} ?+ ?20t 一次轻量校准包
 * （token+elapsed+标志位），elapsed 由客户端本地?tick 推进（原版弓蓄力同款做法?
 * 施法期间包量较旧版每 tick 全量重发?~95%）?
 */
public final class SpellCastHud {
	/** 面板贴图?4×68 窄条?026-09-19 用户重绘）：?蓄力填充层，?框体层；
	 * ?右两版镜像贴图，?CD ?TEX_PANEL / TEX_PANEL_RIGHT 双贴图选图显示?*/
	private static final ResourceLocation TEX_FULL = new ResourceLocation("ssc_addon", "textures/gui/spell_charge_bar_right.png");
	private static final ResourceLocation TEX_EMPTY = new ResourceLocation("ssc_addon", "textures/gui/spell_charge_bar_right_empty.png");
	private static final ResourceLocation TEX_FULL_LEFT = new ResourceLocation("ssc_addon", "textures/gui/spell_charge_bar_left.png");
	private static final ResourceLocation TEX_EMPTY_LEFT = new ResourceLocation("ssc_addon", "textures/gui/spell_charge_bar_left_empty.png");
	private static final int TEX_W = 14, TEX_H = 68;
	/** 滑入动画时长（tick?.3 ?6t）?*/
	private static final float SLIDE_IN_TICKS = 6.0F;
	/** 释放完成后的停留时长（tick?.5 ?10t）?*/
	private static final float HOLD_TICKS = 10.0F;
	/** 正常滑出时长（tick?.3 ?6t）?*/
	private static final float SLIDE_OUT_TICKS = 6.0F;
	/** 打断快速滑出时长（tick?.15 ?3t）?*/
	private static final float INTERRUPT_SLIDE_OUT_TICKS = 3.0F;
	/** 滑入/滑出距离（px）：一个条?+ 余量，确保完全从屏幕外滑入?*/
	private static final float SLIDE_DIST = 40.0F;
	private static State state;

	public record State(java.util.UUID casterUuid, Spell spell, int token, int elapsed, int duration, SpellCastingRules.Mode mode,
			boolean released, boolean immobilized, boolean solo, boolean continuous, int cancelTicks,
			/** 锁定态（服务?isLockedIn：领域壳扩张?爆裂红白球生成后不可打断）：倒计时红显且忽略取消显示?*/
			boolean locked, int level, int lockTick, float projectileRange) {}

	private SpellCastHud() {}

	public static State getState() { return state; }

	/** 滑入动画起始时刻（客户端世界时间）：null→非 null 的沿触发，state ?tick 重发不重置?*/
	private static Long slideInAt;
	/** 退场起始时刻与退场方式（null = 施法中）?*/
	private static Long endAt;
	private static boolean endedByInterrupt;
	/** 最后一次收?STATE 包的时刻：校准包?20t 一发，静默?45t 视为会话已死（掉包看门狗）?*/
	private static long lastStateAt;
	/** 本地推进锚点：最近一次校准包?(世界 tick, elapsed) 对；两包之间 elapsed 按差值推进?*/
	private static long anchorAt;
	private static int anchorElapsed;
	/** 客户端本地取消标志：施法键再次按下（AUTOMATIC 取消长按）时立即置位，红字即时显示；
	 * 服务?cancelTicks 校准包到达后被权威值覆盖?*/
	private static boolean localCancelling;

	public static void clear() {
		if (state != null) github.com.gengyoubo.sscfa.client.sound.SpellChargeSoundInstance.stopFor(state.casterUuid());
		state = null;
		slideInAt = null;
		endAt = null;
		endedByInterrupt = false;
		lastStateAt = 0;
		anchorAt = 0;
		anchorElapsed = 0;
		localCancelling = false;
		SpellChannelManager.setClientImmobile(null);
	}

	/** 解码活动会话；校准包沿用首包的有效等级，不从当前卷轴等级重新推算?*/
	public static State readActiveState(net.minecraft.network.FriendlyByteBuf buf, State old) {
		if (buf.readBoolean()) {
			Spell spell = SpellRegistry.get(buf.readResourceLocation().getPath());
			java.util.UUID casterUuid = buf.readUUID();
			return new State(casterUuid, spell, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readEnum(SpellCastingRules.Mode.class), buf.readBoolean(), buf.readBoolean(),
					buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(),buf.readVarInt(),buf.readFloat());
		}
		if (old == null) return null;
		return new State(old.casterUuid(), old.spell(), buf.readVarInt(), buf.readVarInt(),
				old.duration(), old.mode(), buf.readBoolean(), old.immobilized(), old.solo(),
				old.continuous(), buf.readVarInt(), buf.readBoolean(), old.level(),old.lockTick(),old.projectileRange());
	}

	public static void register() {
		github.com.gengyoubo.sscfa.spell.OcelotSpellLink.clientReleaseLocked = player ->
				state != null && endAt == null && state.casterUuid().equals(player.getUUID())
				&& (state.locked() || state.lockTick() >= 0 && effectiveElapsed(state) >= state.lockTick());
		github.com.gengyoubo.sscfa.spell.FormSpellStyle.clientRangeSnapshot = player ->
				state != null && endAt == null && state.casterUuid().equals(player.getUUID()) ? state.projectileRange() : Double.NaN;
		AddonNetwork.registerClientReceiver(SpellChannelManager.STATE, buf -> {
			if (!buf.readBoolean()) { onChannelEnd(); return; }
			boolean full = buf.getBoolean(buf.readerIndex());
			State incoming = readActiveState(buf, state);
			if (incoming == null) return;
			final boolean fullPacket = full;
			final State packetState = incoming;
			Minecraft client = Minecraft.getInstance();
			State old = state;
			state = packetState;
			lastStateAt = now();
			anchorAt = lastStateAt;
			anchorElapsed = packetState.elapsed();
			if (fullPacket) localCancelling = false;
			endAt = null;
			endedByInterrupt = false;
			if (old == null && packetState != null && packetState.duration() > 0) slideInAt = now();
			if (old == null && packetState != null && packetState.duration() > 0 && client.player != null) {
				if (packetState.spell() == null
						|| !java.util.Set.of("domain", "death_finale").contains(packetState.spell().getId().getPath())) {
					var caster = client.player.connection.getLevel().getPlayerByUUID(packetState.casterUuid());
					if (caster == null) caster = client.player;
					github.com.gengyoubo.sscfa.client.sound.SpellChargeSoundInstance.onChannelStart(caster, packetState.duration());
				}
			}
			SpellChannelManager.setClientImmobile(packetState.immobilized() && client.player != null
					? client.player.getUUID() : null);
		});
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
			clear();
			slideInAt = null;
			endAt = null;
			endedByInterrupt = false;
		});
		MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
				render(event.getGuiGraphics(), event.getPartialTick()));
	}

	/** 施法键再次按下（取消长按）本地即时置红字：仅在按键按住期间有效，
	 * 松手时由 SpellcastClient ?clearLocalCancelling 立即撤销（不?20t 校准包）?
	 * 与服务端 cancelTicks 权威值叠加：cancelling = localCancelling || cancelTicks > 0?*/
	public static void markLocalCancelling() {
		localCancelling = true;
	}

	/** 取消长按松手：立即撤销本地红字（服务端 cancelTicks 归零最多滞?20t 校准?
	 * 此期间按住状态才是权威的「正在取消」信号）?*/
	public static void clearLocalCancelling() {
		localCancelling = false;
	}

	/** 通道结束（服务端下发 inactive）：冻结本地推进快照 + 记录退场时刻与方式，进入滑出阶段?*/
	private static void onChannelEnd() {
		State current = state;
		if (current != null) {
			github.com.gengyoubo.sscfa.client.sound.SpellChargeSoundInstance.stopFor(current.casterUuid());
			// 打断时进度必然未满（elapsed<duration）；正常结束/蓄满释放 elapsed≥duration
			// （用本地推进的有效值判定，而非可能滞后 ?0t 的包内值）
			endedByInterrupt = effectiveElapsed(current) < current.duration();
			// 冻结：退场动画期间进度停在结束时刻，不再随本地推进增?
			anchorElapsed = effectiveElapsed(current);
			anchorAt = now();
		}
		endAt = current == null ? null : now();
		SpellChannelManager.setClientImmobile(null);
	}

	private static long now() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? 0 : client.level.getGameTime();
	}

	/** 滑出结束后彻底清空状态?*/
	private static void resetExit() {
		state = null;
		slideInAt = null;
		endAt = null;
		endedByInterrupt = false;
		lastStateAt = 0;
		anchorAt = 0;
		anchorElapsed = 0;
		localCancelling = false;
	}

	/** 本地推进的有?elapsed：锚点（最近校准包? 锚点以来?tick 差；蓄满后封顶在 duration?*/
	private static int effectiveElapsed(State current) {
		int advanced = anchorAt <= 0 ? 0 : (int) Math.max(0, now() - anchorAt);
		return Math.min(current.duration(), anchorElapsed + advanced);
	}

	/** 对外暴露本地推进 elapsed（蓄力音 pitch 等帧级消费者用；与 HUD 渲染同源）?*/
	public static int getEffectiveElapsed(State current) {
		return effectiveElapsed(current);
	}

	private static void render(GuiGraphics context, float tickDelta) {
        github.com.gengyoubo.sscfa.client.hud.HudScaling.render(context, 4, () -> renderUnscaled(context, tickDelta));
    }

    private static void renderUnscaled(GuiGraphics context, float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		State current = state;
		// ===== 掉包看门狗：校准包每 20t 一发，静默?45t（且不在退场动画中）→ 会话已死，直接清 =====
		// 否则 inactive 包丢失时 state 永驻，后续书内施法全部被 active!=null 吞掉（同 bug1 锁死链）?
		if (current != null && endAt == null && lastStateAt > 0 && now() - lastStateAt > 45) {
			resetExit();
			return;
		}
		// ===== 退场状态机必须无条件推进（hudHidden/开界面/零读条也不能冻结，否?state 永驻锁死后续施法?====
		if (current != null && endAt != null) {
			long t = now();
			float since = t - endAt;
			float outTicks = endedByInterrupt ? INTERRUPT_SLIDE_OUT_TICKS : HOLD_TICKS + SLIDE_OUT_TICKS;
			if (since >= outTicks) {
				resetExit(); // 彻底清空：state=null，解锁下一次施?
				return;
			}
		}
		if (current == null || current.spell() == null || client.player == null || client.level == null
				|| client.options.hideGui || client.screen != null
				|| !SSCAddonConfig.client().showChargeBar) return;
		// 零读条不绘制（instant 从未滑入；但退场计时已在上方推进）
		if (current.duration() <= 0) return;
		long t = now();
		// ===== 退场阶段：停留 ?加速滑出（打断则跳过停留快速滑出）=====
		if (endAt != null) {
			float since = t - endAt;
			if (endedByInterrupt) {
				drawPanel(context, client, current, easeInCubic(clamp01(since / INTERRUPT_SLIDE_OUT_TICKS)),
						tickDelta, true);
			} else if (since <= HOLD_TICKS) {
				drawPanel(context, client, current, 0.0F, tickDelta, true); // 停留期原位显?
			} else {
				drawPanel(context, client, current, easeInCubic(clamp01((since - HOLD_TICKS) / SLIDE_OUT_TICKS)),
						tickDelta, true);
			}
			return;
		}
		// ===== 施法中：从侧缘滑入（ease-out：初速最快、临就位最慢）=====
		float inP = slideInAt == null ? 1.0F : clamp01((t - slideInAt) / SLIDE_IN_TICKS);
		drawPanel(context, client, current, 1.0F - easeOutCubic(inP), tickDelta, false);
	}

	/**
	 * 绘制蓄力面板?
	 * @param outOffset 0=就位?..1=离场位移比例（ease-in 后）或滑入剩余位移比例（1-ease-out 后）
	 * @param exiting true=退场阶段（进度冻结在结束时刻，不再?tickDelta 推进?
	 */
	private static void drawPanel(GuiGraphics context, Minecraft client, State current,
			float outOffset, float tickDelta, boolean exiting) {
		SSCAddonClientConfig config = SSCAddonConfig.client();
		config.migrateSkillHudLayout(); // 蓄力条可能在不显?CD 条时渲染，这里兜底跑布局迁移
		boolean onLeft = !config.chargeMirrorRight;
		int[] anchor = anchorPosition(config.chargeBarPosType, context.guiWidth(), context.guiHeight());
		int screenW = context.guiWidth();
		// 就位位置（与 CD ?panelLayout 完全同式）：面板左缘贴「锚?偏移? clamp 防出屏；
		// 贴右侧时做镜像换算（x = screenW - x - 条宽）——默认锚?+偏移(0,-34)+贴右 ?条贴屏幕右缘?
		// 与左?CD 条（左缘贴左缘、同 68 高同 Y）几何对称；offsetX 正值向右平移?
		int baseX = clampPosition(anchor[0] + config.chargeBarPosOffsetX, screenW, TEX_W);
		if (config.chargeMirrorRight) baseX = Math.max(0, screenW - baseX - TEX_W);
		int baseY = clampPosition(anchor[1] + config.chargeBarPosOffsetY,
				context.guiHeight(), TEX_H);
		// 位移方向：条在右 ?从右侧屏幕外滑入?X 方向退场）；条在左 ?从左侧屏幕外?X?
		float distance = outOffset * SLIDE_DIST * (onLeft ? -1 : 1);
		float x = baseX + distance;
		// ===== 进度与倒计时（elapsed 用本地推进值：锚点校准 + tick 差推进，蓄满封顶?====
		int elapsed = effectiveElapsed(current);
		float progress = current.duration() <= 0 ? 1
				: clamp01((elapsed + (exiting ? 0 : tickDelta)) / (float) current.duration());
		boolean full = elapsed >= current.duration();
		// 锁定态（2026-09-23 用户定稿）：忽略取消显示（服务端不会取消，避免「取消中」红字永驻）?
		// 倒计时改红提示「不可打断必须释放」?
		// 本地预测兜底?026-09-24）：锁定判定 = 服务端包?locked ?|| 本地 elapsed ?该法术锁定转?tick?
		// 爆裂锁定窗口 682-700t 不在 20t 校准网格上，只靠服务端单点补发一次（时滞/丢包即整窗不红，
		// 领域 200t 恰在网格上由周期包天然冗余携带故从未出问题）——本地预测不依赖网络，红显零时滞?
		// 一致性：预测基于同一 elapsed 推进链，最多早/?1 个校准周期；服务端位到达后即权威覆盖?
		int lockTick = current.lockTick();
		boolean locked = current.locked() || lockTick >= 0 && elapsed >= lockTick;
		boolean cancelling = !locked && (localCancelling || current.cancelTicks() > 0);
		// ===== 绘制：空??满图自下而上裁切（左/右双贴图选图）→ 条旁倒计?=====
		int drawX = (int) x;
		int drawY = baseY;
		ResourceLocation texEmpty = onLeft ? TEX_EMPTY_LEFT : TEX_EMPTY;
		ResourceLocation texFull = onLeft ? TEX_FULL_LEFT : TEX_FULL;
		context.blit(texEmpty, drawX, drawY, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		int fillH = (int) (TEX_H * progress);
		if (fillH > 0) {
			// 源区取满图底?fillH ??目标画在面板底部（注水式自下而上?
			context.blit(texFull, drawX, drawY + TEX_H - fillH, 0, TEX_H - fillH, TEX_W, fillH, TEX_W, TEX_H);
		}
		// 中心倒计时：剩余秒数（一位小数）；蓄满→就绪/待释放；取消长按→红?
		String timeText;
		int color;
		if (cancelling) {
			timeText = Component.translatable("gui.ssc_addon.spell_cast.cancel_short").getString();
			color = 0xFFFF5555;
		} else if (full) {
			timeText = current.mode() == SpellCastingRules.Mode.RELEASE && !current.released()
					? Component.translatable("gui.ssc_addon.spell_cast.release_short").getString()
					: Component.translatable("gui.ssc_addon.spell_cast.ready_short").getString();
			color = 0xFFFFFF66;
		} else {
			float remainTicks = current.duration() - (elapsed + (exiting ? 0 : tickDelta));
			timeText = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, remainTicks) / 20.0F);
			// 锁定态红显（2026-09-23 用户定稿）：不可打断阶段倒计时红色提示必须释?
			color = locked ? 0xFFFF5555 : 0xFFFFFFFF;
		}
		int textW = client.font.width(timeText);
		// 窄条?4px）内放不下文字：倒计时画在条外侧、指向屏幕中心（条在右→文字在左，条在左→文字在右）
		int textX = onLeft ? drawX + TEX_W + 2 : drawX - textW - 2;
		int textY = drawY + TEX_H / 2 - client.font.lineHeight / 2;
		context.drawString(client.font, timeText, textX, textY, color, true);
	}

	private static float clamp01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	/** 防出屏钳制（?CD ?clampPosition 同式）?*/
	/** 9 宫格锚点（1-9）到屏幕坐标；Forge 简化实现。 */
	private static int[] anchorPosition(int type, int screenW, int screenH) {
		int col = (Math.max(1, Math.min(9, type)) - 1) % 3;
		int row = (Math.max(1, Math.min(9, type)) - 1) / 3;
		int x = col == 0 ? 0 : col == 1 ? screenW / 2 : screenW;
		int y = row == 0 ? 0 : row == 1 ? screenH / 2 : screenH;
		return new int[]{x, y};
	}

	private static int clampPosition(int position, int screenSize, int size) {
		return Math.max(0, Math.min(Math.max(0, screenSize - size), position));
	}

	/** ease-out?-(1-t)^3）：初速最快、临近终点最慢——用于滑入?*/
	private static float easeOutCubic(float t) {
		return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
	}

	/** ease-in（t^3）：初速最慢、临近终点最快——用于滑出?*/
	private static float easeInCubic(float t) {
		return t * t * t;
	}
}
