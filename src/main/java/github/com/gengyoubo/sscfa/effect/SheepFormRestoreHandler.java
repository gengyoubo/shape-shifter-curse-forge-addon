package github.com.gengyoubo.sscfa.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 变羊属性换血与恢复状态机（羊了个羊配套，服务端权威，jackcooper??026-09-29 用户定稿）?
 *
 * <p><b>变羊瞬间（onSheepStart??/b>：记录原最大生??????8 心上限修饰????按血量百分??
 * 重设当前血（满血变???8 心满、半???4 心）；挂移速统一修饰符（动态求商把任何基础移??
 * 压到羊的 0.23）??/p>
 *
 * <p><b>恢复（onSheepEnd / sweep 兜底?/b>：移除两枚修饰符（还原原上限与原移速）????
 * 「变羊期间血量百分比」逆向换算当前血（变羊时被打掉的血保留，不白嫖不亏）???/p>
 *
 * <p><b>边??/b>??/p>
 * <ul>
 *   <li>重复施法（效果刷新）：{@code onApplied} 会再次触发——状态表已有记录时只刷新时???
 *       不重复换血（百分比已按羊上限对齐，直接沿用）??/li>
 *   <li>死亡：效果随实体清除不??onRemoved——sweep 扫描兜???+ 修饰符本就不???NBT??
 *       重??重进存档后原??clamp 回原属性，状态表清掉即可?/li>
 *   <li>断线（玩家）：同上，sweep（??5t）发现效果已无即恢复；修饰??addTemporary 不??
 *       NBT，重进存档天然回原上限???/li>
 *   <li>牛奶清除：走原???onRemoved 正常恢复??/li>
 * </ul>
 */
public final class SheepFormRestoreHandler {

	private SheepFormRestoreHandler() {}

	/** 移速统一修饰???UUID（独立于攻击修饰符，动态系数）??*/
	public static final UUID SPEED_MODIFIER_UUID = UUID.fromString("B7F29D41-2E85-4C7A-9D3B-6E84C1A2F0B3");
	/** 血量上限修饰??UUID??*/
	public static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("C3A68E20-1D97-4B5F-AE2C-7F91D0B3E4A5");

	/** 变羊期间移速上限（??秒??026-09-29 用户定稿「最??5 ??s」）?
	 *  原版速度属性→?s 换算：实际??s ??属性?????43.17??.20.1 玩家??0.1??.317 ??s 实测口径）??
	 *  5 ??s 对应属性????0.1159??*/
	public static final double SHEEP_MAX_SPEED_UNITS = 5.0;
	/** 血量公式基数：?0 血固??8 颗心?6.0 HP）??*/
	public static final double SHEEP_BASE_MAX_HEALTH = 16.0;
	/** 血量公式阈值??0 血（含）以下为基数档??*/
	public static final double HEALTH_FORMULA_THRESHOLD = 20.0;
	/** 血量公式步进：每多 4 点原????羊上?+1 颗心（???+2.0 HP）??
	 *  用户例???026-09-29）??4 血生????8??+ (24-20)/4 = 9 颗??= 18.0 HP??*/
	public static final double HEALTH_PER_STEP = 4.0;

	/** 按用户公式算变羊后的最大生命：??0 血固??16.0?? 心）?20 ??+4 ???+1 颗心?2.0 HP）??
	 *  例??4 ?????8 + 1 = 9 颗??= 18.0??4 ?????8 + 6 = 14 颗??= 28.0??*/
	public static double sheepMaxHealthFor(double originalMax) {
		if (originalMax <= HEALTH_FORMULA_THRESHOLD) return SHEEP_BASE_MAX_HEALTH;
		double hearts = SHEEP_BASE_MAX_HEALTH / 2.0 + Math.floor((originalMax - HEALTH_FORMULA_THRESHOLD) / HEALTH_PER_STEP);
		return hearts * 2.0;
	}

	/** 换血记录：实???UUID ??变羊前的原最大生命（恢复时用变羊期间百分???此值）?*/
	private static final Map<UUID, Double> ORIGINAL_MAX_HEALTH = new HashMap<>();
	/** 上??sweep 的世界时间??t 节流）???*/
	private static long lastSweep = -1;

	/** 变羊开始（SheepFormEffect.onApplied 服务端调用）???*/
	public static void onSheepStart(LivingEntity entity) {
		if (ORIGINAL_MAX_HEALTH.containsKey(entity.getUUID())) {
			return; // 效果刷新（重复施法）：百分比已按羊上限对齐，不重复换??
		}
		double originalMax = entity.getMaxHealth();
		double ratio = originalMax > 0 ? entity.getHealth() / originalMax : 1.0;
		double sheepMax = sheepMaxHealthFor(originalMax);

		AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
		if (healthAttr != null) {
			// 按公式上限压缩：MULTIPLY_TOTAL 系??= 目标上???原??- 1（??24 ?????18/24-1??
			double factor = sheepMax / Math.max(0.01, originalMax) - 1.0;
			healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
			healthAttr.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_UUID,
					"SSCA Sheep Form Health", factor, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		// 按百分比重设当前血（挂完上限修饰符???setHealth 自??clamp 到羊上限??
		entity.setHealth((float) (sheepMax * Math.max(0.0, Math.min(1.0, ratio))));
		// 记录原值（恢复基准??
		ORIGINAL_MAX_HEALTH.put(entity.getUUID(), originalMax);

		applySpeedUniform(entity);
	}

	/** 变羊结束（onRemoved 服务端调用）???*/
	public static void onSheepEnd(LivingEntity entity) {
		restore(entity);
	}

	/** 登录或异常清效果后，恢复记录可能已丢失；只移除无效果对应的变羊攻击修正??*/
	public static void clearOrphanedAttackModifier(LivingEntity entity) {
		if (!entity.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) removeAttackModifier(entity);
	}

	private static void removeAttackModifier(LivingEntity entity) {
		AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) attack.removeModifier(SheepFormEffect.ATTACK_MODIFIER_UUID);
	}

	/**
	 * 重追?重进后的修饰符重挂（sweep 的实例已换分支调用??026-09-30 伴生修复）??
	 * 临时修饰符不??NBT，??JVM 重进（退出到标题再进存档）后效果还在但修饰符已随实??
	 * 重建消失——血?移速回到原生值。此方法按记录的原上限重算羊上限并重挂两枚修饰符??
	 * 按当前血量百分比压回羊血（??onSheepStart 同公式；移速只压快的不加速）??
	 * 记录不存在（完整重启游戏）时??no-op——那种场??AI/外观随效??NBT 恢复，数值??
	 * 原生属性（可接受降级，恢复时也按无记录路径只清修饰符）??
	 */
	public static void reapplyModifiers(LivingEntity entity) {
		Double originalMax = ORIGINAL_MAX_HEALTH.get(entity.getUUID());
		if (originalMax == null) return;
		double sheepMax = sheepMaxHealthFor(originalMax);
		AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
		if (healthAttr != null) {
			double factor = sheepMax / Math.max(0.01, originalMax) - 1.0;
			healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
			healthAttr.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_UUID,
					"SSCA Sheep Form Health", factor, AttributeModifier.Operation.MULTIPLY_TOTAL));
			if (entity.getHealth() > sheepMax) {
				entity.setHealth((float) sheepMax);
			}
		}
		applySpeedUniform(entity);
	}

	/** 移速上限：属性压????.1159（?? ??s；MULTIPLY_TOTAL 求商系数按当前基础值动态算）??
	 *  用户定稿「移动速度最大??5 ??s」——快者被压到上限；慢者（基础属???< 0.1159 的少数实体??
	 *  不加速（上限语义，不是拉平语义）??*/
	private static void applySpeedUniform(LivingEntity entity) {
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr == null) return;
		double base = speedAttr.getBaseValue();
		if (base <= 0.001) return; // 无移速属性实体（如静?boss）跳?
		double target = SHEEP_MAX_SPEED_UNITS / 43.17; // 5 ??s 对应属性????0.1159
		if (base <= target) return; // 本来就慢于上限：不加?
		double factor = target / base - 1.0; // 任意基?????目标的等价乘???
		speedAttr.removeModifier(SPEED_MODIFIER_UUID);
		speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_UUID,
				"SSCA Sheep Form Speed", factor, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/** 恢复原属性（幂等）：移除修饰???按变羊期间百分比 ??原上限重设当前血??*/
	private static void restore(LivingEntity entity) {
		Double originalMax = ORIGINAL_MAX_HEALTH.remove(entity.getUUID());
		// 变羊结束：恢???mob 原??Goal 集（initGoals 重造），再清「受惊逃离施法者」记录??
		// ??顺序不能反??026-09-29 实测「变回来不恢复??AI」根因）：clear 会先?SWAPPED 表项?
		// restoreGoals 里??SWAPPED.remove 判断是否执???initGoals——??clear ??restore 恒??
		// false ??initGoals 永不调???????Goal 集永久残留（苦力怕不自??唤魔者不召唤）?
		SheepFormAiController.restoreGoals(entity);
		SheepFormAiController.clear(entity.getUUID());
		AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
		if (healthAttr != null) healthAttr.removeModifier(HEALTH_MODIFIER_UUID);
		AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) speedAttr.removeModifier(SPEED_MODIFIER_UUID);
		removeAttackModifier(entity); // sweep 不经?StatusEffect.onRemoved，也必须恢复攻击??
		if (originalMax == null) return; // 没换过血（理论不可达）只清修饰符

		if (entity.isAlive()) {
			// 修饰符移除???getMaxHealth 已回原值；按「变羊期间血量百分比」逆向换算当前???
			// （isAlive 已涵盖活体判定；羊上限按记录的原值重算，与变羊时同一公式???
			double sheepMax = originalMax != null ? sheepMaxHealthFor(originalMax) : SHEEP_BASE_MAX_HEALTH;
			double sheepRatio = sheepMax > 0 ? entity.getHealth() / sheepMax : 1.0;
			entity.setHealth((float) (entity.getMaxHealth() * Math.max(0.0, Math.min(1.0, sheepRatio))));
		}
	}

	/**
	 * 兜底扫描的唯一入口（??ServerTickEvents.END_SERVER_TICK??t 节流）???
	 * 效果已不在但状态表仍有记录的实????恢复?
	 * 覆盖：死亡清效果（不?onRemoved）、换形???指令强清状态、异常路径??
	 * 实体不在线（断线/区块卸载）：只清记录——修饰??addTemporary 不??NBT??
	 * 重进存档原???clamp 回原属性，无残留??
	 */
	public static void tickSweep(net.minecraft.server.MinecraftServer server) {
		if (ORIGINAL_MAX_HEALTH.isEmpty()) return;
		long now = server.overworld() != null ? server.overworld().getGameTime() : 0;
		if (now == lastSweep || Math.floorMod(now, 5) != 0) return;
		lastSweep = now;
		// restore 会删除恢复记录；快照遍历保证多个目标同时异常结束时仍能全部恢复???
		for (UUID id : java.util.List.copyOf(ORIGINAL_MAX_HEALTH.keySet())) {
			LivingEntity found = null;
			for (ServerLevel world : server.getAllLevels()) {
				var e = world.getEntity(id);
				if (e instanceof LivingEntity living && living.isAlive()) {
					found = living;
					break;
				}
			}
			if (found == null) {
				ORIGINAL_MAX_HEALTH.remove(id); // 不在线：修饰符不??NBT，重进自动回原属??
				// 同步???AI 备份?026-09-29「部分生??AI 消失」根因之一）：mob 卸载?
				// Goal 集随实体消亡（Goal 不??NBT），残???BACKUPS 会让重载后（效果还在??
				// Goal 已是构造器新注册的原生集）被误判「已替换」跳???swap / 或错误回放旧实例???
				SheepFormAiController.clearOffline(id);
				continue;
			}
			if (!found.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) {
				// 效果已无但记录还在（非正常路径清除）?立即恢???
				restore(found);
			} else if (found instanceof net.minecraft.world.entity.Mob
					&& SheepFormAiController.hasStaleBackupFor(found)) {
				// 效果还在但备份实例已换（实体重载??Goal 重建、效果残留的中间态）??
				// 重新执??swap 到??Goal 集（用重载后的原??Goal 重新备份）??
				// ??只在「实例已换」时触发——同实例正常变羊中备份恒在，恒不触发???026-09-29
				// 实测「AI 消失」根因：旧写法只??hasBackup，??5t 丢弃重备，把正在运行的???
				// Goal 集错存成备份，到期回放出??Goal 而非原生集）???
				SheepFormAiController.reswapAfterReload(found);
				// 重进场景修饰符已随实体重建消失（addTemporary 不??NBT）——重挂血??移??
				// 修饰符，数值恢复「标准羊」???026-09-30 伴生修复）??
				reapplyModifiers(found);
			}
		}
	}
}
