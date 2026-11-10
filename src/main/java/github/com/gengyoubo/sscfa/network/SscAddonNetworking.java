package github.com.gengyoubo.sscfa.network;

import github.com.gengyoubo.sscfa.ability.AllaySPGroupHeal;
import github.com.gengyoubo.sscfa.ability.ControlledAbilityInterruption;
import github.com.gengyoubo.sscfa.ability.FluorescentLaserManager;
import github.com.gengyoubo.sscfa.ability.FluorescentTidalManager;
import github.com.gengyoubo.sscfa.ability.FrostSpikeManager;
import github.com.gengyoubo.sscfa.ability.JumpKillManager;
import github.com.gengyoubo.sscfa.ability.MancianimaPrimary;
import github.com.gengyoubo.sscfa.ability.MancianimaTeleport;
import github.com.gengyoubo.sscfa.ability.NightmareDreamManager;
import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverDoubleJumpManager;
import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager;
import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager;
import github.com.gengyoubo.sscfa.ability.VenomSkillManager;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.ability.VortexGuideManager;
import github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager;
import github.com.gengyoubo.sscfa.ability.WindDashManager;
import github.com.gengyoubo.sscfa.ability.WindSpiritClawManager;
import github.com.gengyoubo.sscfa.ability.AxolotlWaterSpurtHandler;
import github.com.gengyoubo.sscfa.action.SscAddonActions;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.block.InfusionAltarBlockEntity;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.evolution.EvolutionManager;
import github.com.gengyoubo.sscfa.evolution.EvolutionRegistry;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.screen.InfusionAltarScreenHandler;
import github.com.gengyoubo.sscfa.spell.ResearchTableManager;
import github.com.gengyoubo.sscfa.spell.ScrollWorkshopManager;
import github.com.gengyoubo.sscfa.spell.SpellCastManager;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 附属模组自己的网络包：通道 ID、服务端接收器（客户端按键 → 技能入口）和服务端发给客户端的表现同步。
 *
 * <p>全部走 {@link AddonNetwork}。接收器在服务端主线程执行；发往客户端的包只在对方客户端声明了该通道
 * （{@link AddonNetwork#canSend}）时才发，所以客户端表现层还没移植的通道不会产生流量。</p>
 *
 * <p>Fabric 版里的形态/皮肤广播（{@code broadcast_forms}）没有移植：SSC Forge 自己同步形态。
 * 客户端的 {@code request_form_sync} 请求只补发进化路线与法术配置。</p>
 */
public final class SscAddonNetworking {
	/** 契灵 - 次要技能：瞬移。payload: byte mode (0=RAYCAST, 1=PLATFORM) */
	public static final ResourceLocation PACKET_MANCIANIMA_TELEPORT = id("mancianima_teleport");
	/** 契灵 - 主要技能：三段标记。无 payload，服务端根据当前状态分支。 */
	public static final ResourceLocation PACKET_MANCIANIMA_PRIMARY = id("mancianima_primary");
	/** 风灵「疾风连爪」：C2S 上报左键按住(boolean)；S2C 同步爪击阶段(int)+准星条进度(float)。 */
	public static final ResourceLocation PACKET_CLAW_HOLD = id("claw_hold");
	public static final ResourceLocation PACKET_CLAW_STATE = id("claw_state");
	/** 风灵副技能：C2S 按 sp_secondary 触发 +50% 增伤 buff。无 payload。 */
	public static final ResourceLocation PACKET_CLAW_BUFF = id("claw_buff");
	/** 风灵「风之冲刺」：C2S 按主技能键（无 payload，服务端按阶段分支）；S2C 同步阶段(int)+targetY(double)。 */
	public static final ResourceLocation PACKET_WIND_DASH = id("wind_dash");
	public static final ResourceLocation PACKET_DASH_STATE = id("dash_state");
	/** 朔望「灵跃闪身」：S2C 同步第 2 段窗口时长给施法者。payload: varint windowTicks（>0 开启倒数，0 关闭）。 */
	public static final ResourceLocation PACKET_NOVA_LEAP_WINDOW = id("nova_leap_window");
	/** 堕落悦灵「召唤恼鬼」：S2C 同步召唤物存活窗口给施法者。payload: varint windowTicks（>0 开启倒数，0 关闭）。 */
	public static final ResourceLocation PACKET_FALLEN_ALLAY_VEX_WINDOW = id("fallen_allay_vex_window");

	// ===== 白名单 GUI =====
	/** S2C：把调用者当前白名单推给客户端，用于打开/刷新 GUI。 */
	public static final ResourceLocation PACKET_WHITELIST_GUI_SYNC = id("whitelist_gui_sync");
	/** C2S：把某 UUID 加入自己的白名单。payload: UUID */
	public static final ResourceLocation PACKET_WHITELIST_GUI_ADD = id("whitelist_gui_add");
	/** C2S：把某 UUID 从自己的白名单移除。payload: UUID */
	public static final ResourceLocation PACKET_WHITELIST_GUI_REMOVE = id("whitelist_gui_remove");
	/** C2S：切换模式。payload: byte (0=默认, 1=自定义) */
	public static final ResourceLocation PACKET_WHITELIST_GUI_MODE = id("whitelist_gui_mode");
	/** C2S：从生物白名单中移除一个 UUID。payload: UUID */
	public static final ResourceLocation PACKET_WHITELIST_GUI_MOB_REMOVE = id("whitelist_gui_mob_remove");

	/** C2S：美西螈装死期间按技能键请求提前结束装死。无 payload。 */
	public static final ResourceLocation PACKET_PLAY_DEAD_END = id("play_dead_end");
	/** C2S：美西螈漩涡开始蓄力 / 释放。无 payload。 */
	public static final ResourceLocation PACKET_VORTEX_START = id("vortex_start");
	public static final ResourceLocation PACKET_VORTEX_RELEASE = id("vortex_release");

	// ===== 月尘魔法书 =====
	/** C2S：释放书内指定槽魔法。payload: varint slot + varint token。 */
	public static final ResourceLocation PACKET_SPELL_CAST = id("spell_cast");
	/** C2S：更新当前选中槽（滚轮切换）。payload: varint slot。 */
	public static final ResourceLocation PACKET_SPELL_SELECT = id("spell_select");
	/** C2S：设置卷轴施放档位。payload: varint slot + varint castLevel(0=跟随卷轴等级)。 */
	public static final ResourceLocation PACKET_SPELL_SET_CAST_LEVEL = id("spell_set_cast_level");
	/** C2S：法力不足三连击触发的临时降档施放。payload: varint slot + varint token。 */
	public static final ResourceLocation PACKET_SPELL_CAST_DOWNGRADED = id("spell_cast_downgraded");
	/** S2C：把服务端的法术数值配置（JSON）同步给客户端。 */
	public static final ResourceLocation PACKET_SPELL_CONFIG_SYNC = id("spell_config_sync");

	// ===== 月织蛛 =====
	/** C2S：织网术 - 潜行双击主键切换 搭路/攻击 模式。无 payload。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_TOGGLE = id("spider_moon_weaver_toggle");
	/** C2S：织网术 - 主键按下开始蓄力 /「平铺搭桥」蓄力 / 松开释放。无 payload。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_CHARGE_START = id("spider_moon_weaver_charge_start");
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_CHARGE_START_FLAT = id("spider_moon_weaver_charge_start_flat");
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_CHARGE_RELEASE = id("spider_moon_weaver_charge_release");
	/** C2S：二段跳 - 空中按跳跃键。无 payload。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_DOUBLE_JUMP = id("spider_moon_weaver_double_jump");
	/** C2S：蛛丝荡漾 - 次键按下（发射/断丝切换）。无 payload。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_SWING_PRESS = id("spider_moon_weaver_swing_press");
	/** C2S：蛛丝荡漾 - 摆荡中上报当前绳长 + 收放意图。payload: double ropeLen + varint reel(>0收/<0放/0无)。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_SWING_SYNC = id("spider_moon_weaver_swing_sync");
	/** S2C：蛛丝荡漾 - 状态同步给附近玩家。payload: UUID + boolean active + 3×double anchor + double ropeLen + varint state + boolean canExtend + int tetherId。 */
	public static final ResourceLocation PACKET_SPIDER_MOON_WEAVER_SWING_STATE = id("spider_moon_weaver_swing_state");

	// ===== 寒棘狐 =====
	/** C2S：冰刺 - 长按开始蓄力 / 松开停止蓄力 / 点按发射。无 payload。 */
	public static final ResourceLocation PACKET_FROST_SPIKE_CHARGE_START = id("frost_spike_charge_start");
	public static final ResourceLocation PACKET_FROST_SPIKE_CHARGE_RELEASE = id("frost_spike_charge_release");
	public static final ResourceLocation PACKET_FROST_SPIKE_FIRE = id("frost_spike_fire");
	/** C2S：凝棘（次技能） - 长按开始蓄力 / 松开发射。无 payload。 */
	public static final ResourceLocation PACKET_FROST_SPIKE_SECONDARY_START = id("frost_spike_secondary_start");
	public static final ResourceLocation PACKET_FROST_SPIKE_SECONDARY_RELEASE = id("frost_spike_secondary_release");
	/** S2C：主技能蓄力状态（开始/结束各 1 包），客户端据此本地画汇聚流。payload: UUID player + boolean charging */
	public static final ResourceLocation PACKET_FROST_SPIKE_CHARGE_STATE = id("frost_spike_charge_state");

	// ===== 跳蛛 =====
	/** C2S：跳杀 - 主键按下开始蓄力 / 松开跳扑。无 payload。 */
	public static final ResourceLocation PACKET_JUMP_KILL_CHARGE_START = id("jump_kill_charge_start");
	public static final ResourceLocation PACKET_JUMP_KILL_CHARGE_RELEASE = id("jump_kill_charge_release");
	/** C2S：毒液（次技能） - 次键按下。无 payload。 */
	public static final ResourceLocation PACKET_VENOM_SKILL_PRESS = id("venom_skill_press");
	/** S2C：安全丝锚点状态广播。payload: UUID + boolean + 3×double 锚点 + varint window + long 服务端 tick。 */
	public static final ResourceLocation PACKET_JUMP_KILL_SILK_STATE = id("jump_kill_silk_state");

	/** C2S：进化美西螈上报「真正疾跑键」按住状态。payload: boolean held。 */
	public static final ResourceLocation PACKET_AXOLOTL_SPRINT_KEY = id("axolotl_sprint_key");
	/** S2C：仅向施法者发送，令其客户端给实体描边。payload: varint entityId + varint duration + int color。 */
	public static final ResourceLocation PACKET_WEB_HIGHLIGHT = id("web_highlight");

	// ===== 食梦魔 =====
	/** S2C：入梦目标屏幕粉色晕影。payload: varint durationTicks（<=0 = 结束）+ UUID nightmareUuid。 */
	public static final ResourceLocation PACKET_DREAM_VEIL = id("dream_veil");
	/** S2C：恐惧状态，仅发给恐惧目标本人。payload: varint durationTicks（<=0 = 结束）。 */
	public static final ResourceLocation PACKET_FEAR_STATE = id("fear_state");
	/** S2C：恐惧目标「看不见梦魔」窗口。payload: UUID nightmareUuid + varint hideTicks。 */
	public static final ResourceLocation PACKET_FEAR_HIDE = id("fear_hide");
	/** S2C：恐惧目标「梦魇显形」。payload: UUID nightmareUuid + varint revealTicks。 */
	public static final ResourceLocation PACKET_FEAR_REVEAL = id("fear_reveal");
	/** S2C：「惊吓」幽灵实体标记，仅目标本人。payload: UUID ghostUuid + varint lifeTicks。 */
	public static final ResourceLocation PACKET_SPOOK_GHOST = id("spook_ghost");

	// ===== 工作台 =====
	/** C2S：注魔台 - 点击「升级」按钮。无 payload，服务端重验条件后扣材料。 */
	public static final ResourceLocation PACKET_INFUSION_ALTAR_UPGRADE = id("infusion_altar_upgrade");
	/** C2S：法术研究台 - 抄写 / 学习法阵。payload: String element + String variant + varint level。 */
	public static final ResourceLocation PACKET_FORMATION_SCRIBE = id("formation_scribe");
	public static final ResourceLocation PACKET_FORMATION_LEARN = id("formation_learn");
	/** C2S：卷轴工坊 - 制作 / 升级。payload: String spellPath + varint level。 */
	public static final ResourceLocation PACKET_SCROLL_CRAFT = id("scroll_craft");
	public static final ResourceLocation PACKET_SCROLL_UPGRADE = id("scroll_upgrade");
	/** C2S：卷轴工坊 - 修复 / 铭刻 / 拆解。无 payload。 */
	public static final ResourceLocation PACKET_SCROLL_REPAIR = id("scroll_repair");
	public static final ResourceLocation PACKET_SCROLL_IMPRINT = id("scroll_imprint");
	public static final ResourceLocation PACKET_SCROLL_SALVAGE = id("scroll_salvage");

	// ===== 进化美西螈 / 荧光幼灵 =====
	/** C2S：进化美西螈主技能「投掷水矛」/ 次技能「涡流引导」按键。无 payload。 */
	public static final ResourceLocation PACKET_UPGRADE_AXOLOTL_SPEAR = id("upgrade_axolotl_spear");
	public static final ResourceLocation PACKET_UPGRADE_AXOLOTL_VORTEX = id("upgrade_axolotl_vortex");
	/** S2C：「投掷水矛」蓄力期手持水矛渲染状态（对追踪者+自身广播）。payload: UUID + boolean charging */
	public static final ResourceLocation PACKET_SPEAR_CHARGE_STATE = id("spear_charge_state");
	/** C2S：荧光幼灵主要技能（法阵激光）/ 次要技能（潮汐波动）按键。无 payload。 */
	public static final ResourceLocation PACKET_FLUO_LASER = id("fluo_laser_key");
	public static final ResourceLocation PACKET_FLUO_TIDAL = id("fluo_tidal_key");
	/** S2C：「潮汐束缚」被拴目标的 entityId，用于渲染守卫者激光。payload: varint orbId + varint count + count*varint entityId */
	public static final ResourceLocation PACKET_TIDAL_TETHER = id("tidal_tether");

	/** S2C：动画调试记录开关切换（/ssc_addon debug anim）。无 payload。 */
	public static final ResourceLocation PACKET_ANIM_DEBUG_TOGGLE = id("anim_debug_toggle");

	// ===== 进化加点系统 =====
	/** C2S：选择进化路线 / SP 分支 / 解锁一个天赋节点。payload: String id */
	public static final ResourceLocation PACKET_EVO_SELECT_ROUTE = id("evo_select_route");
	public static final ResourceLocation PACKET_EVO_SELECT_BRANCH = id("evo_select_branch");
	public static final ResourceLocation PACKET_EVO_UNLOCK = id("evo_unlock");
	/** C2S：一次性提交多个待确认节点（按点击顺序）。payload: int count + count*String */
	public static final ResourceLocation PACKET_EVO_UNLOCK_BATCH = id("evo_unlock_batch");
	/** C2S：开局选形态界面选定一个 SSCA 进化形态。payload: String formId */
	public static final ResourceLocation PACKET_SSCA_START_ROUTE = id("ssca_start_route");
	/** C2S：客户端加入后请求补发同步数据（两代协议共用）。无 payload */
	public static final ResourceLocation PACKET_REQUEST_ALL_FORM_SYNC = id("request_all_form_sync");
	public static final ResourceLocation PACKET_REQUEST_FORM_SYNC_V2 = id("request_form_sync_v2");
	/** S2C（Fabric 版的形态广播，Forge 版不发）。 */
	public static final ResourceLocation PACKET_BROADCAST_FORMS = id("broadcast_forms");
	/** S2C：把所有进化路线定义（JSON）同步给客户端，供进化树 UI 渲染。 */
	public static final ResourceLocation PACKET_EVO_ROUTES_SYNC = id("evo_routes_sync");
	/** S2C：让客户端打开「转职选择形态」界面。无 payload。 */
	public static final ResourceLocation PACKET_OPEN_JOB_CHANGE = id("open_job_change");
	/** C2S：在转职界面选定目标进化形态并确认。payload: String formId */
	public static final ResourceLocation PACKET_JOB_CHANGE_CONFIRM = id("job_change_confirm");
	/** C2S：打开进化加点界面（触发旧存档迁移检测）。无 payload。 */
	public static final ResourceLocation PACKET_EVO_OPEN = id("evo_open");

	/** 界面类请求的限频：每玩家上一次被接受的时间。 */
	private static final Map<UUID, Long> LAST_GUI_PACKET_MS = new ConcurrentHashMap<>();
	/** 同一玩家两次界面请求的最小间隔（毫秒）。 */
	private static final long GUI_PACKET_MIN_INTERVAL_MS = 100L;

	private SscAddonNetworking() {}

	private static ResourceLocation id(String path) {
		return new ResourceLocation("my_addon", path);
	}

	/** 该玩家是否在限频间隔内重复发包；返回 true 时丢弃这个包。 */
	private static boolean isRateLimited(ServerPlayer player) {
		long now = System.currentTimeMillis();
		Long last = LAST_GUI_PACKET_MS.put(player.getUUID(), now);
		return last != null && (now - last) < GUI_PACKET_MIN_INTERVAL_MS;
	}

	/**
	 * 技能键入口：被控制或平衡配置未同步时不响应；正在施法读条时先按法术自身的打断规则取消施法，技能随后正常生效。
	 * 只包技能触发类的包；施法系统自己的 cast/select 包不走这里。
	 */
	private static void runSkillAction(ServerPlayer player, Runnable action) {
		if (AbilityUseGuard.blocked(player)) return;
		if (!BalanceIntegration.isPlayerReady(player)) return;
		runSkillCleanup(player, action);
	}

	/** 松键/收尾类的包：即使平衡配置未同步也要执行，否则蓄力状态会卡住；被控制时改为中断当前技能。 */
	private static void runSkillCleanup(ServerPlayer player, Runnable action) {
		if (AbilityUseGuard.blocked(player)) {
			ControlledAbilityInterruption.interrupt(player);
			return;
		}
		if (!SpellChannelManager.onSkillAction(player)) return;
		action.run();
	}

	/** 玩家退服时清理限频时间戳。 */
	public static void onPlayerDisconnect(UUID uuid) {
		LAST_GUI_PACKET_MS.remove(uuid);
	}

	// ===== 客户端 → 服务端 =====

	/** 客户端：发送无 payload 的空包。 */
	public static void sendEmpty(ResourceLocation id) {
		AddonNetwork.sendToServer(id, AddonNetwork.buffer());
	}

	// ===== 服务端 → 客户端 =====

	/** 对方客户端声明了该通道才发。 */
	private static void send(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
		if (AddonNetwork.canSend(player, id)) AddonNetwork.sendToPlayer(player, id, buf);
	}

	/** 发给追踪该玩家的客户端和玩家自己。 */
	private static void broadcast(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
		if (AddonNetwork.canSend(player, id)) AddonNetwork.sendToTrackingAndSelf(player, id, buf);
	}

	/** 风灵「疾风连爪」：同步爪击阶段与准星条进度。 */
	public static void syncClawState(ServerPlayer player, int phase, float crosshairProgress) {
		syncClawState(player, phase, crosshairProgress, 0);
	}

	public static void syncClawState(ServerPlayer player, int phase, float crosshairProgress, float recoveryStep) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeInt(phase);
		buf.writeFloat(crosshairProgress);
		buf.writeFloat(recoveryStep);
		buf.writeLong(player.level().getGameTime());
		buf.writeResourceLocation(player.level().dimension().location());
		send(player, PACKET_CLAW_STATE, buf);
	}

	/** 风灵「风之冲刺」：同步阶段与目标悬浮 Y（驱动落点预览）。 */
	public static void syncDashState(ServerPlayer player, int phase, double targetY) {
		syncDashState(player, phase, targetY, 16.0, 3.0);
	}

	public static void syncDashState(ServerPlayer player, int phase, double targetY, double range, double radius) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeInt(phase);
		buf.writeDouble(targetY);
		buf.writeDouble(range);
		buf.writeDouble(radius);
		send(player, PACKET_DASH_STATE, buf);
	}

	/** 朔望「灵跃闪身」：同步第 2 段灵跃窗口给施法者（>0 = 开启倒数，0 = 关闭）。 */
	public static void syncNovaLeapWindow(ServerPlayer player, int windowTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeVarInt(windowTicks);
		send(player, PACKET_NOVA_LEAP_WINDOW, buf);
	}

	/** 堕落悦灵「召唤恼鬼」：同步召唤物存活窗口给施法者（>0 = 开启倒数，0 = 关闭）。 */
	public static void syncFallenAllayVexWindow(ServerPlayer player, int windowTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeVarInt(windowTicks);
		send(player, PACKET_FALLEN_ALLAY_VEX_WINDOW, buf);
	}

	/** 进化美西螈「投掷水矛」：广播蓄力期手持水矛的渲染状态。 */
	public static void syncSpearChargeState(ServerPlayer player, boolean charging) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(charging);
		broadcast(player, PACKET_SPEAR_CHARGE_STATE, buf);
	}

	/** 寒棘狐主技能蓄力状态广播（蓄力开始/结束各 1 包）。 */
	public static void syncFrostSpikeChargeState(ServerPlayer player, boolean charging) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(charging);
		broadcast(player, PACKET_FROST_SPIKE_CHARGE_STATE, buf);
	}

	/** 向施法者发「高亮」（默认蓝色），令其客户端把实体描边 duration tick。 */
	public static void sendWebHighlight(ServerPlayer owner, int entityId, int durationTicks) {
		sendWebHighlight(owner, entityId, durationTicks, 0x3AA0FF);
	}

	/** 向施法者发「高亮」并指定描边颜色（RGB），仅施法者可见。施法者被该目标入梦时不发（入梦者看不到食梦魔的描边）。 */
	public static void sendWebHighlight(ServerPlayer owner, int entityId, int durationTicks, int color) {
		if (owner.level() instanceof ServerLevel world) {
			Entity entity = world.getEntity(entityId);
			if (entity instanceof LivingEntity living && NightmareDreamManager.isBlocked(owner, living)) return;
		}
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeVarInt(entityId);
		buf.writeVarInt(durationTicks);
		buf.writeInt(color);
		send(owner, PACKET_WEB_HIGHLIGHT, buf);
	}

	/** 向入梦目标本人发「粉色晕影 + 入梦你的食梦魔」（duration<=0 = 这段入梦关系结束）。 */
	public static void sendDreamVeil(ServerPlayer target, int durationTicks, UUID nightmareUuid) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeVarInt(durationTicks);
		buf.writeUUID(nightmareUuid);
		send(target, PACKET_DREAM_VEIL, buf);
	}

	/** 向恐惧目标本人发「恐惧」状态（duration<=0 = 结束）。 */
	public static void sendFearState(ServerPlayer target, int durationTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeVarInt(durationTicks);
		send(target, PACKET_FEAR_STATE, buf);
	}

	/** 向恐惧目标本人发「看不见该梦魔」窗口。 */
	public static void sendFearHide(ServerPlayer target, UUID nightmareUuid, int hideTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(nightmareUuid);
		buf.writeVarInt(hideTicks);
		send(target, PACKET_FEAR_HIDE, buf);
	}

	/** 向恐惧目标本人发「梦魇显形 N tick」（清除该梦魔的隐匿窗口，显形期内不触发新隐匿）。 */
	public static void sendFearReveal(ServerPlayer target, UUID nightmareUuid, int revealTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(nightmareUuid);
		buf.writeVarInt(revealTicks);
		send(target, PACKET_FEAR_REVEAL, buf);
	}

	/** 向目标本人发「幽灵实体」标记（客户端对该实体局部取消隐身，仅目标可见）。 */
	public static void sendSpookGhost(ServerPlayer target, UUID ghostUuid, int lifeTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(ghostUuid);
		buf.writeVarInt(lifeTicks);
		send(target, PACKET_SPOOK_GHOST, buf);
	}

	/** 月织蛛蛛丝荡漾：广播摆荡状态（锚点/绳长/状态/能否放长/被拴实体）。 */
	public static void syncSwingState(ServerPlayer player, boolean active, double ax, double ay, double az,
	                                  double ropeLen, int state, boolean canExtend, int tetherEntityId) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(active);
		buf.writeDouble(ax);
		buf.writeDouble(ay);
		buf.writeDouble(az);
		buf.writeDouble(ropeLen);
		buf.writeVarInt(state);
		buf.writeBoolean(canExtend);
		buf.writeInt(tetherEntityId);
		broadcast(player, PACKET_SPIDER_MOON_WEAVER_SWING_STATE, buf);
	}

	/** 向执行者发「动画调试记录开关切换」。 */
	public static void sendAnimDebugToggle(ServerPlayer player) {
		send(player, PACKET_ANIM_DEBUG_TOGGLE, AddonNetwork.buffer());
	}

	/** 跳蛛安全丝锚点状态广播（active=false = 断丝/结束，锚点坐标忽略）。 */
	public static void syncJumpKillSilk(ServerPlayer player, boolean active, double ax, double ay, double az) {
		syncJumpKillSilk(player, active, ax, ay, az, 0);
	}

	/** @param windowTicks 拉回窗口总长（HUD 倒数的分母，仅 active=true 时有意义） */
	public static void syncJumpKillSilk(ServerPlayer player, boolean active, double ax, double ay, double az, int windowTicks) {
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(active);
		buf.writeDouble(ax);
		buf.writeDouble(ay);
		buf.writeDouble(az);
		buf.writeVarInt(windowTicks);
		// 服务端世界 tick：客户端锚点到期 = 此值 + window
		buf.writeLong(player.level().getGameTime());
		broadcast(player, PACKET_JUMP_KILL_SILK_STATE, buf);
	}

	/** 通知客户端打开灵能宝珠「转职选择形态」界面。 */
	public static void sendOpenJobChange(ServerPlayer player) {
		send(player, PACKET_OPEN_JOB_CHANGE, AddonNetwork.buffer());
	}

	/** 把指定玩家当前白名单推送到其客户端，用于打开/刷新白名单 GUI。 */
	public static void sendWhitelistSync(ServerPlayer player) {
		List<UUID> mobs = WhitelistUtils.getWhitelistedMobUuids(player);
		// 生物 UUID 同时存于玩家前缀时（双写机制产生），不要出现在玩家页
		Set<UUID> mobSet = new HashSet<>(mobs);
		List<UUID> players = new ArrayList<>();
		for (UUID uuid : AllaySPGroupHeal.getWhitelistUuids(player)) if (!mobSet.contains(uuid)) players.add(uuid);
		FriendlyByteBuf buf = AddonNetwork.buffer();
		buf.writeBoolean(WhitelistUtils.isCustomMode(player));
		buf.writeInt(players.size());
		for (UUID uuid : players) buf.writeUUID(uuid);
		// 生物列表：int n + n * (UUID + 类型 ID 或 "")
		buf.writeInt(mobs.size());
		for (UUID uuid : mobs) {
			buf.writeUUID(uuid);
			String typeId = WhitelistUtils.getMobTypeId(player, uuid);
			buf.writeUtf(typeId != null ? typeId : "");
		}
		send(player, PACKET_WHITELIST_GUI_SYNC, buf);
	}

	public static void sendEvolutionRoutes(ServerPlayer player) {
		sendEvolutionRoutes(player, false);
	}

	private static void sendEvolutionRoutes(ServerPlayer player, boolean force) {
		JsonSnapshotSync.send(player, PACKET_EVO_ROUTES_SYNC, EvolutionRegistry.INSTANCE.getRawJson(), force);
	}

	/** 玩家加入和数据包重载成功后都发送最新进化路线，在线客户端无需重连。 */
	private static void registerEvolutionRouteSync() {
		MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() != null) sendEvolutionRoutes(event.getPlayer(), false);
			else event.getPlayerList().getPlayers().forEach(player -> sendEvolutionRoutes(player, true));
		});
	}

	// ===== 接收器 =====

	/** 在模组构造时调用一次。 */
	public static void registerServerReceivers() {
		registerEvolutionRouteSync();
		PlayerSessionCleanup.register((handler, server) -> LAST_GUI_PACKET_MS.remove(handler.player.getUUID()));
		MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> LAST_GUI_PACKET_MS.clear());

		// 契灵
		AddonNetwork.registerServerReceiver(PACKET_MANCIANIMA_TELEPORT, (player, buf) -> {
			byte mode = buf.readByte();
			runSkillAction(player, () -> MancianimaTeleport.execute(player, mode));
		});
		skill(PACKET_MANCIANIMA_PRIMARY, MancianimaPrimary::execute);

		// 白名单 GUI
		AddonNetwork.registerServerReceiver(PACKET_WHITELIST_GUI_ADD, (player, buf) -> {
			UUID target = buf.readUUID();
			if (isRateLimited(player)) return;
			// 不允许把自己加入自己的白名单
			if (target.equals(player.getUUID())) return;
			// 每人最多 256 个，防止恶意客户端用持久化 tag 撑爆存档
			String tag = AllaySPGroupHeal.WHITELIST_TAG_PREFIX + target;
			long existing = player.getTags().stream().filter(t -> t.startsWith(AllaySPGroupHeal.WHITELIST_TAG_PREFIX)).count();
			if (!player.getTags().contains(tag) && existing >= 256L) return;
			player.getTags().add(tag);
			sendWhitelistSync(player);
		});
		AddonNetwork.registerServerReceiver(PACKET_WHITELIST_GUI_REMOVE, (player, buf) -> {
			UUID target = buf.readUUID();
			if (isRateLimited(player)) return;
			player.getTags().remove(AllaySPGroupHeal.WHITELIST_TAG_PREFIX + target);
			sendWhitelistSync(player);
		});
		AddonNetwork.registerServerReceiver(PACKET_WHITELIST_GUI_MODE, (player, buf) -> {
			byte mode = buf.readByte();
			if (isRateLimited(player)) return;
			WhitelistUtils.setCustomMode(player, mode == 1);
			sendWhitelistSync(player);
		});
		AddonNetwork.registerServerReceiver(PACKET_WHITELIST_GUI_MOB_REMOVE, (player, buf) -> {
			UUID mobUuid = buf.readUUID();
			if (isRateLimited(player)) return;
			WhitelistUtils.removeMobFromWhitelist(player, mobUuid);
			// 同时清掉友军标记双写时写入的玩家前缀 tag，避免残留导致计数不一致
			AllaySPGroupHeal.removeFromWhitelistByUuid(player, mobUuid);
			sendWhitelistSync(player);
		});

		// 美西螈装死 - 提前结束（装死期间按 sp_secondary）
		AddonNetwork.registerServerReceiver(PACKET_PLAY_DEAD_END, (player, buf) -> {
			if (!player.hasEffect(AddonEffects.PLAYING_DEAD.get())) return;
			player.removeEffect(AddonEffects.PLAYING_DEAD.get());
			player.removeEffect(MobEffects.BLINDNESS);
			player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			player.setPose(Pose.STANDING);
			// 提前结束：冷却从此刻起按 power JSON 的 extra_cooldowns.early_end 重新起算。
			// 身上没有该技能（如羊形态期间）时 extra() 返回 0，不能拿 0 去覆盖正在走的装死冷却。
			int earlyEnd = SkillCooldowns.extra(player, SscAddonActions.PLAY_DEAD_SKILL_ID, "early_end");
			if (earlyEnd > 0) SkillCooldowns.force(player, SscAddonActions.PLAY_DEAD_SKILL_ID, earlyEnd);
		});

		// 美西螈漩涡蓄力
		skill(PACKET_VORTEX_START, VortexChargeManager::start);
		cleanup(PACKET_VORTEX_RELEASE, VortexChargeManager::release);

		// 月尘魔法书
		AddonNetwork.registerServerReceiver(PACKET_SPELL_CAST, (player, buf) -> {
			int slot = buf.readVarInt();
			int token = buf.readVarInt();
			SpellCastManager.cast(player, slot, token);
		});
		AddonNetwork.registerServerReceiver(PACKET_SPELL_SELECT, (player, buf) ->
				SpellCastManager.setSelected(player, buf.readVarInt()));
		AddonNetwork.registerServerReceiver(PACKET_SPELL_SET_CAST_LEVEL, (player, buf) -> {
			int slot = buf.readVarInt();
			int castLevel = buf.readVarInt();
			if (!isRateLimited(player)) SpellCastManager.setCastLevel(player, slot, castLevel);
		});
		AddonNetwork.registerServerReceiver(PACKET_SPELL_CAST_DOWNGRADED, (player, buf) -> {
			int slot = buf.readVarInt();
			int token = buf.readVarInt();
			if (!isRateLimited(player)) SpellCastManager.castDowngraded(player, slot, token);
		});

		// 注魔台 - 「升级」按钮（服务端重验：书可升级 + 催化物 + 燃料）
		AddonNetwork.registerServerReceiver(PACKET_INFUSION_ALTAR_UPGRADE, (player, buf) -> {
			if (player.containerMenu instanceof InfusionAltarScreenHandler menu && !menu.isMaintenance()
					&& menu.stillValid(player) && menu.getInventory() instanceof InfusionAltarBlockEntity altar) {
				altar.tryUpgrade(player);
			}
		});

		// 法术研究台
		AddonNetwork.registerServerReceiver(PACKET_FORMATION_SCRIBE, (player, buf) -> {
			String element = buf.readUtf(64), variant = buf.readUtf(64);
			int level = buf.readVarInt();
			if (!isRateLimited(player)) ResearchTableManager.scribe(player, element, variant, level);
		});
		AddonNetwork.registerServerReceiver(PACKET_FORMATION_LEARN, (player, buf) -> {
			String element = buf.readUtf(64), variant = buf.readUtf(64);
			int level = buf.readVarInt();
			if (!isRateLimited(player)) ResearchTableManager.learn(player, element, variant, level, 0);
		});

		// 卷轴工坊
		AddonNetwork.registerServerReceiver(PACKET_SCROLL_CRAFT, (player, buf) -> {
			String spellPath = buf.readUtf(64);
			int level = buf.readVarInt();
			if (!isRateLimited(player)) ScrollWorkshopManager.craft(player, spellPath, level);
		});
		AddonNetwork.registerServerReceiver(PACKET_SCROLL_UPGRADE, (player, buf) -> {
			String spellPath = buf.readUtf(64);
			int targetLevel = buf.readVarInt();
			if (!isRateLimited(player)) ScrollWorkshopManager.upgrade(player, spellPath, targetLevel);
		});
		gui(PACKET_SCROLL_REPAIR, ScrollWorkshopManager::repair);
		gui(PACKET_SCROLL_IMPRINT, ScrollWorkshopManager::imprint);
		gui(PACKET_SCROLL_SALVAGE, ScrollWorkshopManager::salvage);

		// 月织蛛：织网术 / 二段跳 / 蛛丝荡漾
		skill(PACKET_SPIDER_MOON_WEAVER_TOGGLE, SpiderMoonWeaverWebManager::toggleMode);
		skill(PACKET_SPIDER_MOON_WEAVER_CHARGE_START, SpiderMoonWeaverWebManager::start);
		skill(PACKET_SPIDER_MOON_WEAVER_CHARGE_START_FLAT, SpiderMoonWeaverWebManager::startFlat);
		cleanup(PACKET_SPIDER_MOON_WEAVER_CHARGE_RELEASE, SpiderMoonWeaverWebManager::release);
		// 二段跳本身由客户端原版 jump() 完成，服务端只广播音效粒子
		skill(PACKET_SPIDER_MOON_WEAVER_DOUBLE_JUMP, SpiderMoonWeaverDoubleJumpManager::onDoubleJump);
		skill(PACKET_SPIDER_MOON_WEAVER_SWING_PRESS, SpiderMoonWeaverSwingManager::onSecondaryPress);
		AddonNetwork.registerServerReceiver(PACKET_SPIDER_MOON_WEAVER_SWING_SYNC, (player, buf) -> {
			double ropeLen = buf.readDouble();
			int reel = buf.readVarInt();
			SpiderMoonWeaverSwingManager.onReelSync(player, ropeLen, reel);
		});

		// 寒棘狐：冰刺 / 凝棘
		skill(PACKET_FROST_SPIKE_CHARGE_START, FrostSpikeManager::startCharge);
		cleanup(PACKET_FROST_SPIKE_CHARGE_RELEASE, FrostSpikeManager::stopCharge);
		skill(PACKET_FROST_SPIKE_FIRE, FrostSpikeManager::fire);
		skill(PACKET_FROST_SPIKE_SECONDARY_START, FrostSpikeManager::startSecondaryCharge);
		skill(PACKET_FROST_SPIKE_SECONDARY_RELEASE, FrostSpikeManager::releaseSecondary);

		// 跳蛛：跳杀 / 毒液
		skill(PACKET_JUMP_KILL_CHARGE_START, JumpKillManager::startCharge);
		cleanup(PACKET_JUMP_KILL_CHARGE_RELEASE, JumpKillManager::release);
		skill(PACKET_VENOM_SKILL_PRESS, VenomSkillManager::onPress);

		// 进化美西螈：疾跑键状态 / 投掷水矛 / 涡流引导
		AddonNetwork.registerServerReceiver(PACKET_AXOLOTL_SPRINT_KEY, (player, buf) ->
				AxolotlWaterSpurtHandler.setClientSprintHeld(player, buf.readBoolean()));
		skill(PACKET_UPGRADE_AXOLOTL_SPEAR, WaterSpearLeapManager::onKeyPress);
		skill(PACKET_UPGRADE_AXOLOTL_VORTEX, VortexGuideManager::onKeyPress);

		// 风灵：疾风连爪按住状态 / 增伤 buff / 风之冲刺
		AddonNetwork.registerServerReceiver(PACKET_CLAW_HOLD, (player, buf) -> {
			boolean hold = buf.readBoolean();
			// 松开总是接受；按下要求平衡配置已同步，并先打断正在读条的法术
			if (hold && !BalanceIntegration.isPlayerReady(player)) return;
			if (hold && !SpellChannelManager.onSkillAction(player)) return;
			WindSpiritClawManager.setHolding(player, hold);
		});
		skill(PACKET_CLAW_BUFF, WindSpiritClawManager::activateSecondaryBuff);
		skill(PACKET_WIND_DASH, WindDashManager::onKeyPress);

		// 荧光幼灵：法阵激光 / 潮汐波动
		skill(PACKET_FLUO_LASER, FluorescentLaserManager::onKeyPress);
		skill(PACKET_FLUO_TIDAL, FluorescentTidalManager::onKeyPress);

		// 进化加点系统
		AddonNetwork.registerServerReceiver(PACKET_EVO_SELECT_ROUTE, (player, buf) -> {
			String routeId = buf.readUtf(256);
			if (!isRateLimited(player)) EvolutionManager.selectRoute(player, routeId);
		});
		AddonNetwork.registerServerReceiver(PACKET_EVO_SELECT_BRANCH, (player, buf) -> {
			String branchId = buf.readUtf(256);
			if (!isRateLimited(player)) EvolutionManager.selectBranch(player, branchId);
		});
		AddonNetwork.registerServerReceiver(PACKET_EVO_UNLOCK, (player, buf) -> {
			String nodeId = buf.readUtf(256);
			if (!isRateLimited(player)) EvolutionManager.tryUnlock(player, nodeId);
		});
		AddonNetwork.registerServerReceiver(PACKET_EVO_UNLOCK_BATCH, (player, buf) -> {
			int count = Math.max(0, Math.min(64, buf.readInt()));
			List<String> ids = new ArrayList<>(count);
			for (int i = 0; i < count; i++) ids.add(buf.readUtf(256));
			if (!isRateLimited(player)) EvolutionManager.tryUnlockBatch(player, ids);
		});
		AddonNetwork.registerServerReceiver(PACKET_SSCA_START_ROUTE, (player, buf) -> {
			String formId = buf.readUtf(256);
			if (!isRateLimited(player)) EvolutionManager.startSscaRoute(player, formId);
		});
		AddonNetwork.registerServerReceiver(PACKET_JOB_CHANGE_CONFIRM, (player, buf) -> {
			String formId = buf.readUtf(256);
			if (!isRateLimited(player)) EvolutionManager.startJobChange(player, formId);
		});
		gui(PACKET_EVO_OPEN, EvolutionManager::checkAndMigrate);

		// 客户端加入后的补发请求：形态由 SSC Forge 同步，这里只补进化路线
		gui(PACKET_REQUEST_ALL_FORM_SYNC, SscAddonNetworking::sendEvolutionRoutes);
		gui(PACKET_REQUEST_FORM_SYNC_V2, SscAddonNetworking::sendEvolutionRoutes);
	}

	/** 无 payload 的技能键包。 */
	private static void skill(ResourceLocation id, java.util.function.Consumer<ServerPlayer> action) {
		AddonNetwork.registerServerReceiver(id, (player, buf) -> runSkillAction(player, () -> action.accept(player)));
	}

	/** 无 payload 的松键/收尾包。 */
	private static void cleanup(ResourceLocation id, java.util.function.Consumer<ServerPlayer> action) {
		AddonNetwork.registerServerReceiver(id, (player, buf) -> runSkillCleanup(player, () -> action.accept(player)));
	}

	/** 无 payload 的界面请求包（限频）。 */
	private static void gui(ResourceLocation id, java.util.function.Consumer<ServerPlayer> action) {
		AddonNetwork.registerServerReceiver(id, (player, buf) -> {
			if (!isRateLimited(player)) action.accept(player);
		});
	}
}
