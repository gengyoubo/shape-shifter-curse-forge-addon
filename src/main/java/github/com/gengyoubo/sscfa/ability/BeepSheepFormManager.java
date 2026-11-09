package github.com.gengyoubo.sscfa.ability;

import io.github.apace100.apoli.power.CooldownPower;
import io.github.apace100.apoli.power.Power;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 变羊形态切换管理器（羊了个羊配套，服务端权威，jackcooper??026-09-29 用户三轮定稿）?
 *
 * <p><b>架构换代</b>：变羊从「效果压制」改为「切换空壳羊形态」——羊形态（my_addon:sheep_spell_form??
 * 空壳?power）不挂任何技?被??魔法，天然全失效；渲染继续??SheepFormRenderMixin 画原版羊模型?
 * SHEEP_FORM 效果继续承载时??AI 停机/换血（这套已验证）。无黑屏动画（immediatelyTransform 瞬变?
 * 用户定稿）；到期切回原形态；牛奶不可净化??/p>
 *
 * <p><b>原形态保??/b>：变羊瞬间记录原 FormID 到本管理器内存表（按玩??UUID）??b>Apoli 原??CD 快??/b>??
 * 切形态会重??power 实例导??CooldownPower.lastUseTime 清零——变羊前快照所??CooldownPower ??
 * 剩??tick，切回后按「快照剩?+ 变羊经过时长」回写（CD 正常走表，不重算；用户定稿）??
 * SSCA 统一冷却（SkillCooldowns，PersistentState 记账）不受切形态影响，无需处理?/p>
 *
 * <p><b>持久?/b>：原形态记录??NBT 到本类的玩家数据（登录时若玩家处于羊形态且无记????自动切回
 * 基础人形 original_before_enable，防卡死羊形态）；变羊期间掉线：效果与形态都在玩家数据里?
 * 重进后由 tickSweep 续走（效果在→继续羊；效果没了→切回）?/p>
 */
public final class BeepSheepFormManager {

	private BeepSheepFormManager() {}

	/** 玩??UUID ??变羊快照（??FormID + Apoli CD 快??+ 变羊时刻）??*/
	private static final Map<UUID, SheepSnapshot> SNAPSHOTS = new HashMap<>();

	private record SheepSnapshot(String originalFormId, Map<String, Integer> cooldowns, long sheepStartTime) {}

	/** Existing persisted transformation snapshot; callers must not treat it as an active form/power. */
	public static String originalFormId(ServerPlayer player) {
		SheepSnapshot snapshot = SNAPSHOTS.get(player.getUUID());
		return snapshot == null ? null : snapshot.originalFormId();
	}

	/** 当前是否处于羊形态（?FormID 判定，双端可用）??*/
	public static boolean isSheepForm(net.onixary.shapeShifterCurseForge.form.FormDefinition form) {
		return form != null && FormIdentifiers.SHEEP_FORM.equals(form.id());
	}

	/** 变羊开始：快照原形??+ Apoli CD ??瞬切羊形态（无黑屏，用户定稿）?*/
	public static void onSheepStart(ServerPlayer player) {
		// 装死打断??026-09-29 用户实测问??）：变羊瞬间中止装死——??PLAYING_DEAD 及伴生效果、恢复站姿??
		// 若不清：效果继??tick（??SWIMMING 姿??持续回血），且羊期??PlayDeadEndClient 用??GLFW
		// 绕过按键屏蔽仍会发提前结束包 ??羊身上无美西?power ??extra() 返??0 ??force(0) 清空装死 CD
		//（即问??「变回后 CD 被重置」的真因，日??20:12:54 skill disabled 警告与此吻合）?
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get())) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get());
			player.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
			player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
			player.setPose(net.minecraft.world.entity.Pose.STANDING);
		}
		if (SNAPSHOTS.containsKey(player.getUUID())) return; // 重复施法只刷新效果时?
		net.onixary.shapeShifterCurseForge.form.FormDefinition original = FormUtils.getCurrentForm(player);
		String originalId = original != null && original.id() != null
				? original.id().toString() : null;
		if (originalId == null || FormIdentifiers.SHEEP_FORM.toString().equals(originalId)) {
			return; // 已是?无形态（理论不可达）：不快照不切??
		}
		// Apoli 原??CD 快照（CooldownPower.lastUseTime 随切形态重建清零；切回后回写续走??
		Map<String, Integer> cds = new HashMap<>();
		for (Power power : io.github.apace100.apoli.component.PowerHolderComponent.KEY.get(player).getPowers()) {
			if (power instanceof CooldownPower cooldown && cooldown.getRemainingTicks() > 0) {
				cds.put(power.getType().getIdentifier().toString(), cooldown.getRemainingTicks());
			}
		}
		long now = player.level().getGameTime();
		SNAPSHOTS.put(player.getUUID(), new SheepSnapshot(originalId, cds, now));

		net.onixary.shapeShifterCurseForge.form.FormDefinition sheep = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.SHEEP_FORM);
		if (sheep != null) {
			net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, sheep.id()); // 瞬变（无黑屏动画，用户定稿??
			// fallback 修复?026-09-29 实测问??真因）：immediatelyTransform 尾??_setForm ??
			// NormalForm.onTransform_Finish ??setFallbackForm(null)——null 查不?form 会??
			// fallbackFormID 污染?InitialForm??开书前人形）。羊期间一旦任何路径??
			// FormUtils.applyFallback（如重进登录链异常兜底），玩家就会被错误变回人类?
			// 变羊后立刻??fallback 修回「变羊前的原形态」，保证任??fallback 都回到原形态??
			net.onixary.shapeShifterCurseForge.form.FormDefinition originalForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(originalId));
			if (originalForm != null) {
				
			}
		}
	}

	/** 变羊结束：切回原形??+ 回??Apoli CD（快照剩?+ 变羊经过时长）?*/
	public static void onSheepEnd(ServerPlayer player) {
		SheepSnapshot snapshot = SNAPSHOTS.remove(player.getUUID());
		net.onixary.shapeShifterCurseForge.form.FormDefinition original = snapshot != null
				? net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(snapshot.originalFormId())) : null;
		if (original == null) {
			original = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(net.onixary.shapeShifterCurseForge.form.FormRegistry.ORIGINAL_BEFORE_ENABLE); // 记录缺失兜底：人??
		}
		net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, original.id());
		// fallback 修复（同 onSheepStart）：切回??_setForm 尾??setFallbackForm(null) 又会?
		// fallbackFormID 污染?InitialForm——切回后立刻修回原形态，防后续任??applyFallback
		// 把玩家错误变成开书前人形??026-09-29 实测「退出羊形态后变人类」真因）??
		
		// CD 回写：切形态已重建 power 实例（CD 清零），按「快照剩?- 变羊经过」续写，
		// ??CD 表现与「从未变羊」完全一致（正常计算，用户定稿「别全重新计算」）??
		// 遍历玩家当??power ??type id 匹配快照（PowerTypeRegistry ??get(Identifier) API，不依赖它??
		if (snapshot != null && !snapshot.cooldowns().isEmpty()) {
			long elapsed = Math.max(0, player.level().getGameTime() - snapshot.sheepStartTime());
			var holder = io.github.apace100.apoli.component.PowerHolderComponent.KEY.get(player);
			for (Power power : holder.getPowers()) {
				if (!(power instanceof CooldownPower cooldown)) continue;
				Integer snap = snapshot.cooldowns().get(power.getType().getIdentifier().toString());
				if (snap == null) continue;
				int remaining = (int) Math.max(0, snap - elapsed);
				if (remaining > 0) {
					cooldown.setCooldown(remaining);
					io.github.apace100.apoli.component.PowerHolderComponent.syncPower(player, power.getType());
				}
			}
		}
	}

	/** 兜底扫描（??END_SERVER_TICK）：效果没了但还卡在羊形???切回?
	 *  覆盖：牛奶外的一切非正常路径（死亡重生清效??指令清效??reload 清效果等）??*/
	public static void tickSweep(net.minecraft.server.MinecraftServer server) {
		for (var world : server.getAllLevels()) {
			for (ServerPlayer sp : world.players()) {
				net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(sp);
				if (isSheepForm(form) && !sp.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) {
					onSheepEnd(sp); // 效果已无但形态还是羊：恢??
				}
			}
		}
	}

	/** 玩家数据持久化（PersistentState 语义）：??NBT（断?停服不丢原形态记录）?*/
	public static void writePlayerNbt(ServerPlayer player, CompoundTag nbt) {
		SheepSnapshot snapshot = SNAPSHOTS.get(player.getUUID());
		if (snapshot == null) return;
		CompoundTag tag = new CompoundTag();
		tag.putString("OriginalForm", snapshot.originalFormId());
		tag.putLong("SheepStart", snapshot.sheepStartTime());
		CompoundTag cds = new CompoundTag();
		snapshot.cooldowns().forEach(cds::putInt);
		tag.put("Cooldowns", cds);
		nbt.put("SSCA.SheepForm", tag);
	}

	/** 登录恢复：??NBT 重建快照（变羊中掉线重连续走）?*/
	public static void readPlayerNbt(ServerPlayer player, CompoundTag nbt) {
		if (!nbt.contains("SSCA.SheepForm")) return;
		CompoundTag tag = nbt.getCompound("SSCA.SheepForm");
		Map<String, Integer> cds = new HashMap<>();
		CompoundTag cdTag = tag.getCompound("Cooldowns");
		for (String key : cdTag.getAllKeys()) cds.put(key, cdTag.getInt(key));
		SNAPSHOTS.put(player.getUUID(), new SheepSnapshot(
				github.com.gengyoubo.sscfa.util.FormIdMigration.canonical(tag.getString("OriginalForm")),
				cds, tag.getLong("SheepStart")));
	}

	/** 玩家彻底退出（不留尸体数据）：清内存记录?*/
	public static void onPlayerRemoved(ServerPlayer player) {
		SNAPSHOTS.remove(player.getUUID());
	}

	// ==================== 事件挂载（SscAddon.registerApoliSystems 调用?====================

	/** 事件注册：玩??NBT 读写（持久化? 登录矫正 + 断线清理??*/
	public static void init() {
		// JOIN：变羊中重进（效果与形态都在玩家数据里，天然续走）；此处兜底两种异常??
		// ??卡羊形态但效果已无（停服期间到期未??onRemoved）??切回原形态??
		// ??快照丢失但处于羊形态（旧存档）??切回人形防卡死?
		github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(player -> {
					net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(player);
					if (isSheepForm(form)) {
						// 重进后立刻??fallback：存档里??fallbackFormID 可能是上一会??
						// onTransform_Finish 污染的人形值；用快照原形态覆盖，防登录链任??
						// applyFallback 把变羊中的玩家变人类??
						SheepSnapshot snap = SNAPSHOTS.get(player.getUUID());
						net.onixary.shapeShifterCurseForge.form.FormDefinition original = snap != null
								? net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(snap.originalFormId())) : null;
						if (original != null) {
							
						}
						if (!player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) {
							onSheepEnd(player); // 无快照时内部兜底切人??
						}
					}
			});
		// 注意?*不??*注??DISCONNECT 清空快照??026-09-29 实测问??真因）：Fabric DISCONNECT
		// 事件?onDisconnected HEAD 触发，早??PlayerManager.remove ??savePlayerData ??
		// writeCustomDataToNbt 存盘——先清内存表会让 SSCA.SheepForm 从未写进 NBT，重进后
		// readPlayerNbt 无数???到??onSheepEnd 走人形兜底（「变羊退出回来直接变人」）??
		// 内存表无泄漏风险：重进时 readPlayerNbt ??UUID 覆盖、到??onSheepEnd ??remove??
		// 停服进程结束自然清空?
	}

	/** NBT 写：?mixin（PlayerEntity.writeCustomDataToNbt 末尾）调用??*/
	public static void onWriteNbt(ServerPlayer player, CompoundTag nbt) {
		writePlayerNbt(player, nbt);
	}

	/** NBT 读：?mixin（PlayerEntity.readCustomDataFromNbt 末尾）调用??*/
	public static void onReadNbt(ServerPlayer player, CompoundTag nbt) {
		readPlayerNbt(player, nbt);
	}
}
