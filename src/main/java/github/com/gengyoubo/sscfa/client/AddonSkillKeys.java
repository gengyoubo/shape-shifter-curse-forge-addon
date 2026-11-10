package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.platform.InputConstants;
import github.com.gengyoubo.sscfa.power.AddonPowerTypes;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.onixary.shapeShifterCurseForge.client.FormKeyMappings;
import net.onixary.shapeShifterCurseForge.client.SscClientKeys;
import net.onixary.shapeShifterCurseForge.client.render.MagicHudRenderer;

/**
 * SP 主/副技能键（{@code key.ssc_addon.sp_primary / sp_secondary}）：不绑定物理键的虚拟键，按下状态动态计算，
 * 由 SSC Forge 每 tick 轮询并发给服务端（见 {@link SscClientKeys}）。
 *
 * <p>默认跟随 SSC 的第 1/2 个主动技能键（包括 SSC 技能栏选中时的虚拟按下）；打开任意界面、
 * 被完全控制或变羊时不触发。每个形态单独设置技能键等改写通过 {@link #setRouter} 接入。</p>
 */
public final class AddonSkillKeys {
	public static final String CATEGORY = "key.categories.ssc_addon";

	/** 改写某个技能键的按下状态；返回 null 表示不改写，沿用 SSC 技能键。 */
	@FunctionalInterface
	public interface Router {
		Boolean pressed(boolean primary);
	}

	private static volatile Router router = primary -> null;
	private static final VirtualSkillKey PRIMARY = new VirtualSkillKey(AddonPowerTypes.SP_PRIMARY_KEY, true);
	private static final VirtualSkillKey SECONDARY = new VirtualSkillKey(AddonPowerTypes.SP_SECONDARY_KEY, false);

	private AddonSkillKeys() {}

	public static void setRouter(Router value) {
		router = value;
	}

	/** 客户端初始化时调用一次。 */
	public static void init() {
		SscClientKeys.register(PRIMARY);
		SscClientKeys.register(SECONDARY);
	}

	/** 主技能虚拟键（isDown 动态计算）。 */
	public static KeyMapping primaryKey() {
		return PRIMARY;
	}

	/** 副技能虚拟键（isDown 动态计算）。 */
	public static KeyMapping secondaryKey() {
		return SECONDARY;
	}

	/** 技能键当前是否按下。 */
	public static boolean computePressed(boolean primary) {
		Minecraft minecraft = Minecraft.getInstance();
		// 任何界面打开时不触发技能（与原版键位一致）
		if (minecraft.screen != null || minecraft.player == null || AbilityUseGuard.blocked(minecraft.player)) return false;
		if (github.com.gengyoubo.sscfa.client.PlayDeadEndClient.blocksSkillInput(primary)) return false;
		Boolean routed = router.pressed(primary);
		if (routed != null) return routed;
		int slot = primary ? 0 : 1;
		KeyMapping ssc = primary ? FormKeyMappings.ACTIVE_SKILL_1 : FormKeyMappings.ACTIVE_SKILL_2;
		return ssc.isDown() || MagicHudRenderer.virtualPressed(slot);
	}

	/** 虚拟键：名字就是能力 JSON 里的按键 ID，isDown 动态计算。 */
	private static final class VirtualSkillKey extends KeyMapping {
		private final boolean primary;

		private VirtualSkillKey(String name, boolean primary) {
			super(name, InputConstants.UNKNOWN.getValue(), CATEGORY);
			this.primary = primary;
		}

		@Override
		public boolean isDown() {
			return computePressed(primary);
		}
	}
}
