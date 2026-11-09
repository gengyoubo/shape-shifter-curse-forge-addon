package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.mixin.entity.GoalSelectorGoalsAccessor;
import github.com.gengyoubo.sscfa.mixin.entity.MobEntityGoalAccessor;
import github.com.gengyoubo.sscfa.mixin.entity.PrioritizedGoalAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 变??mob 的「??AI」控制器（羊了个羊，服务端权威，2026-09-29 用户四轮定稿）?
 *
 * <p><b>架构（Goal 集备???替????回放???/b>??
 * <ul>
 *   <li><b>变羊?/b>：先?goalSelector / targetSelector 的原?Goal ??b>逐条备???/b>
 *       （??Goal + 优先级，???accessor ??{@code GoalSelector.goals} ??
 *       {@code PrioritizedGoal.goal/priority}），再整体替换为原版成年??Goal ??+
 *       FleeEntityGoal(逃离施法者，苦力怕怕猫同款机?????/li>
 *   <li><b>变羊结???/b>：清掉???Goal 集??b>按备份逐条回??/b>（原优先级???Goal 实例）???
 *       不??initGoals() 重造——它只覆盖「在该方法里注册」??Goal??b>在构造器里注???Goal
 *       的生物（部分原版??mod 生物）重造后这部分永久丢??/b>??026-09-29 实??
 *       「变回来后部分生??AI 消失」根因）。备??回放对任意注册途径都完整?/li>
 * </ul>
 * Goal 实例本身??mob 绑定（构造时传??mob），回放同一实例即恢复原行为??/p>
 *
 * <p><b>原能力停??/b>（用户定稿）：替换期间原??Goal 全部不在选择器里——苦力怕自爆??
 * 唤魔者召唤恼鬼等一切主动行为彻底无法触发；targetSelector 一并备份并留空（羊不索敌）??/p>
 */
public final class SheepFormAiController {

	private SheepFormAiController() {}

	/** 变羊目???UUID ??施法?UUID（逃离源）??*/
	private static final Map<UUID, UUID> FLEE_FROM = new HashMap<>();

	/** 备份条目：??Goal + 注册优先级（goalSelector ??targetSelector 各一份列表）???
	 *  mobRef：swap 时??mob 实例引用——重?换维度后???UUID 是新实例，旧备份里??Goal
	 *  全绑死在旧实体上（回?把死引用塞进???mob 的选择器，永??tick），用于判备份过期??*/
	private record GoalBackup(Mob mobRef, List<GoalEntry> goals, List<GoalEntry> targets) {}
	private record GoalEntry(int priority, Goal goal) {}

	/** 变羊目???UUID ??Goal 备份?*/
	private static final Map<UUID, GoalBackup> BACKUPS = new HashMap<>();

	/** 咩弹命中时登记（目??mob 与施法者）??*/
	public static void setFleeFrom(UUID target, UUID caster) {
		if (target != null && caster != null) {
			FLEE_FROM.put(target, caster);
		}
	}

	/** 变羊结束：清登记（Goal 回放???{@link #restoreGoals} 承担，须?clear 之前调用）??*/
	public static void clear(UUID target) {
		if (target != null) {
			FLEE_FROM.remove(target);
			BACKUPS.remove(target);
		}
	}

	/**
	 * ??AI ??tick 推进（SheepFormEffect.applyUpdateEffect 服务端调用，???MobEntity）??
	 * 首??tick 执行「备?+ 替换」（幂等）???
	 */
	public static void tickSheepAi(LivingEntity entity) {
		if (entity.level().isClientSide || !(entity instanceof Mob mob)) {
			return;
		}
		swapGoalsToSheep(mob);
	}

	/** 备份原???Goal ????替换为???Goal 集（幂等，按实例身份判）?*/
	private static void swapGoalsToSheep(Mob mob) {
		GoalBackup prev = BACKUPS.get(mob.getUUID());
		if (prev != null && prev.mobRef() == mob) {
			return; // 同一实例已替换过（幂等挡板）；实例已换则继续走自愈重?
		}
		UUID casterId = FLEE_FROM.get(mob.getUUID());
		if (casterId == null || !(mob.level() instanceof ServerLevel world)) {
			return;
		}
		var caster = world.getEntity(casterId);
		if (!(caster instanceof LivingEntity casterLiving)) {
			return;
		}
		MobEntityGoalAccessor acc = (MobEntityGoalAccessor) mob;
		GoalSelector goalSelector = acc.ssca$getGoalSelector();
		GoalSelector targetSelector = acc.ssca$getTargetSelector();
		// ??备份原???Goal 集（?Goal + 优先级；含构造器注册的——??initGoals 重造完整）??
		//    实例已换（重???换维度）时旧备份??Goal 绑死旧实体不可回放——方法入口挡???
		//    已放行，此处直接用当前选择器内容（重载后构造器重建的原生集）覆盖重新备份??
		GoalBackup backup = new GoalBackup(mob, snapshotGoals(goalSelector), snapshotGoals(targetSelector));
		BACKUPS.put(mob.getUUID(), backup);
		// 诊断日志??026-09-29 实测「AI 消失」定位用；确认修复后可移除）
		org.slf4j.LoggerFactory.getLogger("SSCA/SheepAI").info(
				"swap: {} goals(goalSel={}) + targets(targetSel={})",
				mob.getType().toString(), backup.goals().size(), backup.targets().size());
		// ??清空并替换为???Goal 集（照??SheepEntity.initGoals 注册表与优先级??
		// 0 Swim / 1 EscapeDanger(1.25) / 2 EatGrass(濞?Animal) / 3 WanderFar(1.0)
		// / 4 看玩??6?? / 5 环视；AnimalMate/Tempt/FollowParent 依赖羊专属行为，非羊挂不上，跳过??
		stopAndClear(goalSelector);
		stopAndClear(targetSelector);
		PathfinderMob pathMob = mob instanceof PathfinderMob p ? p : null;
		goalSelector.addGoal(0, new FloatGoal(mob));
		if (pathMob != null) {
			goalSelector.addGoal(1, new PanicGoal(pathMob, 1.25));
		}
		if (mob instanceof net.minecraft.world.entity.animal.Animal animal) {
			goalSelector.addGoal(2, new EatBlockGoal(animal));
		}
		if (pathMob != null) {
			goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(pathMob, 1.0));
		}
		goalSelector.addGoal(4, new LookAtPlayerGoal(mob, Player.class, 6.0F));
		goalSelector.addGoal(5, new RandomLookAroundGoal(mob));
		// ??逃离施法者（苦力怕怕猫同款）：FleeEntityGoal 优先???0（最高档，压过游?吃草）???
		//    12 格内逃离，惊慌速???1.2。两个谓词（selection + includeTarget）都锁定施法者实体??
		//    FleeEntityGoal 构造需???PathAwareEntity——??PathAware 的稀?mob 跳过逃离（??Goal 集其余照挂）???
		if (pathMob != null) {
			AvoidEntityGoal<LivingEntity> flee = new AvoidEntityGoal<>(
					pathMob, LivingEntity.class,
					(LivingEntity target) -> target == casterLiving,
					12.0F, 1.0, 1.2,
					(LivingEntity target) -> target == casterLiving);
			goalSelector.addGoal(0, flee);
		}
		// ??targetSelector 留空：羊不索敌不记??
		// ??清残留仇恨（???Goal 前已被锁定的旧目???旧攻击状态???
		mob.setTarget(null);
		mob.setLastHurtByPlayer(null);
	}

	/** 备份是否绑在当前实例上（sweep 判定重载中间态用：备份在但实例已????需??swap）??*/
	public static boolean hasStaleBackupFor(LivingEntity entity) {
		GoalBackup b = BACKUPS.get(entity.getUUID());
		return b != null && b.mobRef() != entity;
	}

	/** mob 卸载时清全??AI 记录（Goal 集随实体消亡，备份的??Goal 实例已无意义）?*/
	public static void clearOffline(UUID target) {
		if (target != null) {
			FLEE_FROM.remove(target);
			BACKUPS.remove(target);
		}
	}

	/**
	 * 实体重载后的中间态修复（sweep 调用）：效果残留但实例已换（新实例构造器已重建原生集），
	 * 旧备份??Goal 绑死旧实体不可回放——丢弃旧备份并重???swap（用当前原生集重新备份）??
	 * 只在「实例已换」时执行，同实例正常变羊中恒不触发??
	 */
	public static void reswapAfterReload(LivingEntity entity) {
		if (hasStaleBackupFor(entity) && entity instanceof Mob mob) {
			BACKUPS.remove(entity.getUUID());
			swapGoalsToSheep(mob);
		}
	}

	/** 从选择器读出全部（优先级，?Goal）对（快照拷贝）?*/
	private static List<GoalEntry> snapshotGoals(GoalSelector selector) {
		List<GoalEntry> list = new ArrayList<>();
		for (WrappedGoal pg : ((GoalSelectorGoalsAccessor) selector).ssca$getGoals()) {
			PrioritizedGoalAccessor pga = (PrioritizedGoalAccessor) pg;
			list.add(new GoalEntry(pga.ssca$getPriority(), pga.ssca$getGoal()));
		}
		return list;
	}

	/** 停止并清空一个选择器：先对运行中???PrioritizedGoal ??stop() 释??Move/Look 控制??
	 *  （GoalSelector.clear 只??removeIf，不?stop——运行中??Goal 会??goalsByControl ??
	 *  持锁，替换后???Goal 抢不???Move 控制????站桩。stop ??PrioritizedGoal ??public
	 *  方法，反编译已核实签名）??*/
	private static void stopAndClear(GoalSelector selector) {
		for (WrappedGoal pg : new ArrayList<>(((GoalSelectorGoalsAccessor) selector).ssca$getGoals())) {
			if (pg.isRunning()) {
				pg.stop();
			}
		}
		selector.removeAllGoals(goal -> true);
	}

	/**
	 * 变羊结束恢复原生 Goal 集（SheepFormRestoreHandler.restore 调用，须?clear 之前）???
	 * 清??Goal ??按备份逐条回放（原优先级??Goal 实例）。任意注册途径（构造器/initGoals/
	 * mod 注入）???Goal 都完整恢复——根治「部分生?AI 消失」???
	 */
	public static void restoreGoals(LivingEntity entity) {
		if (entity.level().isClientSide || !(entity instanceof Mob mob)) {
			return;
		}
		GoalBackup backup = BACKUPS.get(mob.getUUID());
		if (backup == null) {
			return; // 未替换过（玩家目?重复恢复），no-op
		}
		BACKUPS.remove(mob.getUUID());
		// 实例已换（重???换维度）：旧备份??Goal 绑死旧实体，不回放死引用——新实例构造???
		// 已重建原生集，直接保持即可?
		if (backup.mobRef() != mob) {
			org.slf4j.LoggerFactory.getLogger("SSCA/SheepAI").info(
					"restore: {} instance changed, keep rebuilt native goals",
					mob.getType().toString());
			return;
		}
		MobEntityGoalAccessor acc = (MobEntityGoalAccessor) mob;
		stopAndClear(acc.ssca$getGoalSelector());
		stopAndClear(acc.ssca$getTargetSelector());
		for (GoalEntry entry : backup.goals()) {
			acc.ssca$getGoalSelector().addGoal(entry.priority(), entry.goal());
		}
		for (GoalEntry entry : backup.targets()) {
			acc.ssca$getTargetSelector().addGoal(entry.priority(), entry.goal());
		}
		// 诊断日志??026-09-29 实测「AI 消失」定位用；确认修复后可移除）
		org.slf4j.LoggerFactory.getLogger("SSCA/SheepAI").info(
				"restore: {} replayed {} goals + {} targets",
				mob.getType().toString(), backup.goals().size(), backup.targets().size());
		// 清残留导??仇恨，原??AI 从干净状态启??
		mob.getNavigation().stop();
		mob.setTarget(null);
		mob.setLastHurtByPlayer(null);
	}
}
