# Forge 移植待办

对照上游 Fabric 仓库 `main` 提交 `481b1a6`。已注册或能编译不代表游戏功能已验证；各项完成后应检查服务端、客户端和存档行为。

历史自动验证记录（2026-11-14）：`runGameTestServer` 35 项全过（含新增的「附属 JSON 类型全部已注册」与「饰品战利品池已注入」），`balanceTest` 491 项 pin 0 待移植全过，`neoModelTest`、`energyNetworkTest` 通过，`runClient` 可进入标题界面。本次未重跑这些测试；通过注册检查不代表下列占位功能已经实现。

## 2026-11-19 代码审查：空实现与未接通逻辑

范围：当前工作区 `src/main/java` 的 855 个 Java 文件，结合资源注册和调用链静态检查。这里的“确认”指代码中可见的缺口，尚未通过游戏复现；未重新对比上游。P1 优先处理核心功能阻断，P2 处理加成、反馈与兼容缺失，P3 清理遗留入口。下文源码位置均相对于 `src/main/java/github/com/gengyoubo/sscfa/`。

### P1：核心玩法阻断

- [ ] **接回注入祭坛的真实方块。** `init/AddonBlocks.java:43` 仍注册 `FacingShellBlock`，该类没有创建方块实体、tick 或打开菜单的实现。已有的 `block/InfusionAltarBlock.java:33` 才包含这些入口，单独注册实体类型和客户端菜单不能代替接线。改用真实方块，并核对旧存档兼容。验收：实际放置的祭坛具有 `InfusionAltarBlockEntity`，右键可打开菜单，处理逻辑推进，重载后库存保留。
  - 代码接线已完成（2026-11-19）：`AddonBlocks.INFUSION_ALTAR` 改用 `InfusionAltarBlock`；对旧存档中无方块实体的祭坛，首次右键时补建 `InfusionAltarBlockEntity`。待实机验证放置、打开菜单、tick 与重载后库存保留。

- [ ] **实现增强符文方案与卷轴拓印。** `spell/research/RuneScheme.java:12–27` 的查询固定返回 `false` / `true` / `null` / `RuneModifiers.NONE`，`create` 只生成空 NBT，两个 `imprint` 都返回 `0`，归一化方法为空。`SlottedFormationTransaction.completeEnhanced` 已消耗材料并调用 `SpellFormationItem.createEnhanced`，因此不能只补 UI。补齐世界权威方案存储、归属和版本校验、修饰参数、拓印及旧数据迁移。验收：增强研究产物有可验证的方案；祭坛拓印成功且只扣一次材料；增强影响实际施法、预览和冷却；错误世界或损坏方案被拒绝；存档重载保持一致。
  - 代码已完成（2026-11-19）：按上游移植 `RuneScheme`——世界 ID + 方案 ID 双重授权、镜像与登记内容逐字段比对、法术/等级校验、修饰参数（服务端只信世界登记）、拓印（全部校验先于修改，仅扣一张法阵）、旧「仅基础」方案剥除。`PlaceholderPortGameTests.runeSchemeRequiresWorldAuthority` 覆盖授权/篡改/他世界。待实机验证：增强研究产物、祭坛拓印、增强对施法/预览/冷却的影响、存档重载。

- [ ] **统一玩家法术熟练度和书本经验。** `spell/PlayerSpellMastery.java:30–33` 的 `clientExpTen` 恒为 `0`，`grant` 为空；旧法术和 `SpellExpGrant` 的经验入口因此失效。注意 `neomagic/NeoRuntime.java:278–284` 已单独累加 `spellbookMasteryExpTen`，补实现时必须去重，不能再累加两次。接回持久化、书本经验、倍率、等级阈值、加成及客户端快照，核对 `thresholdTen` 当前忽略等级参数是否符合规则。验收：旧法术、蓝图施法及命中经验按各自规则发放一次，祭坛和物品提示显示同一数值，重登保留。
  - 代码已完成（2026-11-19）：`PlayerSpellMastery` 接回 `grant`（未满级书加书内经验，玩家熟练度按 `boostedExpTen` 累加一次）、满级旧书经验迁移 `migrate`、客户端快照（HUD 同步写入、退出清零）；删除 `NeoRuntime.grantExperience` 中的重复累加。`thresholdTen` 与上游一致固定 6000（等级参数仅保留接口形状）。`NeoLegacyCasting.profile` 改为身体档案 + 熟练度安全值 + 增能法阵安全值，与上游一致。待实机验证：三类经验来源各发一次、祭坛/物品提示一致、重登保留。

- [ ] **接通崩解的服务端阶段惩罚。** `spell/CollapsePenalties.java:17–30` 只有分档、减速数值与身体转向辅助；`NeoRuntime.tick` 没有按负荷应用/撤销对应惩罚，`backlash` 当前只发消息和标脏。`neomagic/CollapseEffect.java` 虽已实现减速和周期扣血，尚未找到按崩解阶段施加这些效果的链路；其减速 UUID 也与身体转向辅助读取的 UUID 不同。验收：超过安全值后各档实际生效，恢复、变形、死亡和退出后正确清理，阶段 HUD 与服务端效果一致，失败结算不重复扣血。
  - 代码已完成（2026-11-19）：`CollapsePenalties.tick` 由 `NeoRuntime` 每 tick 驱动：5 档减速修饰符（UUID 与身体转向辅助一致）、3 档起每 20/10 tick 直接扣 1/2 点生命（跳过护甲/吸收，保留图腾与九命复活）；变形、死亡、退出、关服时清理；首次接管时移除旧版崩解药水效果。`backlash` 仍只提示，连续惩罚统一按 tick 结算，不重复扣血。`collapsePenaltyFollowsBurdenAndClears` 覆盖分档与撤销。待实机验证：HUD 阶段与服务端一致、多人。

### P2：已进入调用链的占位与部分实现

- [ ] **补天赋映射和仍返回默认值的加成。** `compat/AxolotlSpPerks.java:9–14` 不应用水魔法效率及湿润度折扣；`FluorescentAxolotlPerks.java:8–13` 的伤害、蓄力、飞行速度及命中回调均未实现；`FamiliarFoxSpPerks.java:11–16` 不保留效果时长或转换等级；`NovaPerks.hungerDiscount`、`WindSpiritPerks.hungerDiscount` 恒为 `0`。这些入口已有漩涡、激光、潮汐、效果 mixin 或饥饿 mixin 调用。另为已实现 power 查询的冥狼、金沙岚、跳蛛、月织蛛、升级美西螈接通可购买的 perk 树/映射。验收：购买、洗点、切换形态和多人同步都能改变相应行为。**保留 `SscPerkIds` 中按既有要求停用的远距扑袭及其强化，不自动恢复 `hasLongPounce`。**
  - 代码已完成（2026-11-19）：新增 `compat/AddonPerkTrees`，在 SSC 内置树注册后为 17 个附属形态镜像对应原版树（节点/连线/花费/图标原样，节点 ID `my_addon:<形态>/<原版 perk>`，增减能力按上游 `loadMapped` 规则映射；金沙岚只加载附属能力）；语言键从 SSC 原文生成。接回美西螈 SP 水魔法效率与漩涡空气折扣、荧光幼灵伤害/蓄力/飞行速度/命中回湿（每秒一次）、使魔渗透场保留时长并降为 I 级、新星/风灵代谢饥饿折扣。远距扑袭节点即使出现也标记未实现，`hasLongPounce` 仍恒为 false。`addonPerkTreesMirrorSscTrees` 校验树形与能力 ID。
  - 与 Fabric 的差异：附属形态各有独立形态组，购买记录按附属形态独立保存，不与原版形态的购买共享；旧 Fabric 存档中原版树的购买不会自动迁移到附属树。阿澪（`axolotl_aling`）未注册为形态，未建树。
  - 待实机验证：形态调谐器中显示/购买、洗点、切换形态、多人同步。

- [ ] **补旧法术命中的联动与经验结算。** `spell/SpellHitHelper.java:25–48` 只处理白名单、伤害和异常命中，忽略 `element`、`refundCastId`、`expBounty`。火弹、冰刺、月光箭、AOE 和死亡终曲仍调用它；核对并接回形态命中联动、退款标识和命中经验，同时避免与弹体/AOE 调用方已有结算重复。验收：有效命中触发对应联动并只发一次经验，保护/免疫目标不结算。
  - 代码已完成（2026-11-19）：`SpellHitHelper` 按上游补回：弹体解析到主人（此前弹体命中跳过白名单且无攻击者）、王庭伤害联动、命中经验 `SpellExpGrant`（仅 HIT 时发放，调用方随后清零）、悦灵伤害联动、`projectileHit` 的形态流派钩子（FIRE 聚合钩子仍由调用方在循环外调用，已核对无重复）。待实机验证。

- [ ] **补寄生果实的施法中减益加成。** `ability/NpcCombatEnhancements.java:80–86` 的 `enhanceFruitDebuff` 只加固定 NPC 奖励；注释仍称 `BatSpellLink` 未移植，但该类现已存在。接回当前蝙蝠施法快照中应参与的加成，校正过时注释。验收：同一减益在施法联动开启/关闭、NPC/非 NPC 目标下按规则结算一次。
  - 代码已完成（2026-11-19）：扩散传递中的效果不再叠加；否则取 `BatSpellLink.current()` 快照的 `npcBonus`，无施法快照时用默认加成；过时注释已更正。待实机验证。

- [ ] **实现通用法阵充能状态。** `spell/UniversalFormationManager.java:9–10` 的 `isCharging` 恒为 `false`；`condition/SscAddonConditions.java:48` 和 `ability/MancianimaMarkManager.java:359` 已依赖它判定施法/充能互斥。补实际状态、生命周期和所需法力回复桥接。验收：开始、取消、结束、死亡及切换形态时查询准确，互斥条件有效。
  - 代码已完成（2026-11-19）：移植上游每秒结算（书内回能法阵、负荷高于阈值、形态有能量条时消耗形态能量降低负荷），本秒转化即标记充能，至下次结算；退出、死亡、变形、关服清理。附属形态的回能 power 已用 `not_universal_formation_charging` 条件暂停回复；SSC 法力池（1.10.0.11 起）通过取消 `SscManaRecoveryEvent` 暂停回复，扣除、直接设值与补满不受影响。附属的法力写入（`ManaUtils`、`ResourceBars`）改走 `SscManaApi`：回复经事件，恢复旧档/补满/命令设值用 `setMana` 绕过事件，上限读取含加成的 `maximumMana`。待实机验证。

- [ ] **接通蛛网弹的毒纺锤饰品判定。** `compat/TrinketsConditionAction.java:11–13` 恒为 `false`，`compat/RegCustomItem.java:13–14` 的 `VENOM_SPINDLE` / `VENOM_SPINDLE_PLUS` 都为 `null`；`entity/WebMembraneBullet.java:90–132` 的额外伤害、减益及升级版缠身因此不可达。对接 SSC Forge 实际物品和饰品槽查询。验收：基础/升级饰品分别产生预期加成，卸下或未安装可选饰品模组时正确回退。
  - 代码已完成（2026-11-19）：`TrinketsConditionAction` 通过 SSC Forge `AccessoryUtils.getEntitySlot` 查询（index < 0 查全部槽位，与 SSC 自身蛛网弹一致），饰品模组缺失时按未佩戴处理；`VENOM_SPINDLE` 取 SSC 注册物品，`VENOM_SPINDLE_PLUS` 按 ID 查找（缺失为 null）。待实机验证。

- [ ] **实现饰品客户端冷却查询。** `item/LifesavingCatTailClient.java:6–11` 固定返回 `0` / `false`，救命猫尾与幻影铃都在调用。接回客户端实际冷却与剩余 tick，处理无玩家/退出世界。验收：触发、倒计时、到期和重新同步时的提示与服务端一致。
  - 代码已完成（2026-11-19）：读取本地玩家物品冷却（服务端冷却经原版包同步），无玩家时为 0/false。待实机验证。

- [ ] **补技能 HUD 的两个状态来源。** `client/ClientTickCache.java:27,45` 的 `frostForgeProgress` 恒为 `-1f`，`tidalTetherFraction` 恒为 `-1.0`；`SkillCooldownBarRenderer.java:122,184` 因此无法显示凝棘进度和潮汐牵引倒计时。接已有实体/客户端快照并清理失效状态。验收：技能开始、推进、结束及换维度时正确显示和消失。
  - 代码已完成（2026-11-19）：`ClientTickCache` 每客户端 tick 扫描一次寒棘法阵进度与潮汐球拴人剩余，切换世界/维度或退出时清空。待实机验证。

- [ ] **实现蓄力音效及超距打断反馈。** `client/sound/SpellChargeSoundInstance.java:8–9` 的开始/停止方法为空；`client/ExplosionEffects.java:8` 与 `client/renderer/ExplosionRenderer.java:9` 也为空。`SpellCastHud` 和 `CastingVisualState` 已调用这些入口。验收：蓄力音效按施法者管理，取消/完成/退出时停止；超距失败产生反馈并中止对应爆裂表现，其他玩家的表现不受误清理。
  - 代码已完成（2026-11-19）：蓄力循环音（≥3 秒才播放、跟随施法者、音调随进度上升，按施法者停止，退出世界全部停止）与超距取消提示音。
  - 爆裂客户端表现已移植（2026-11-19）：`ExplosionRenderer` 接收 `ssc_addon:explosion_start`（公共代码声明通道，独立服务端也会下发），绘制落点/脚下法阵、光柱、七层空中法阵、红白球、冲击波与终章，以及目标定位指针；`ExplosionEffects` 负责警示圈/爆炸粒子（分帧）、光影预热与距离衰减的主题音频。超距中止只停对应序列。待实机验证画面与音频。

- [ ] **补新魔法蓝图的书本/法阵倍率桥接。** `neomagic/NeoLegacyCasting.java:133–147` 的带书本 `blueprintQuote` 忽略书本，`blueprintPower` 和 `blueprintExperienceMultiplier` 恒为 `1`；`NeoRuntime` 已读取后两者。对照现有设计确认各项适用范围并接回，特别核对 `FormationData.universalExpMultiplier`，避免和熟练度修复重复乘算。验收：有/无法阵时成本、威力和经验的服务端结算与预览一致。
  - 代码已完成（2026-11-19）：按上游接回带书报价（书内法阵按输入核心折算费用 × 潮汐 × 形态崩解倍率）、威力（伤害法阵 × 形态亲和）、冷却（最低 20% 且 ≥10 tick）、空间射程、经验法阵倍率。经验法阵倍率与 `boostedExpTen` 是两个独立倍率（与旧法术一致），不重复。射程不再额外乘形态弹体倍率（运行时已乘 `style.projectileRange`）。HUD 预览与服务端共用 `blueprintQuote`。待实机验证。

- [ ] **补法力类型查询与旧档恢复匹配。** `compat/ManaUtils.java:36–37` 的 `getPlayerManaTypeID` 恒为空串，`event/LoginResourceRestoreHandler.java:64` 却用它与旧档 `ManaTypeID` 比较。实现 Forge 类型映射或明确迁移规则。验收：旧档非空且类型匹配的法力可恢复，类型不匹配时不会错误套用；资源诊断命令显示真实类型。
  - 代码已完成（2026-11-19）：按 SSC Forge 规则取激活的 `mana_type_power` 的 `mana_type`，否则为 `shape-shifter-curse:generic_mana`；与 Fabric 版 ID 相同，可直接比较旧档 `ManaTypeID`。待实机验证旧档恢复。

- [ ] **实现 CTP 药水形态识别。** `compat/CTPUtils.java:10` 恒为 `null`，使 `item/PotionTubeDose.java:31` 的“形态药水不可分装”检查失效。确认 Forge 的 NBT 格式并实现识别。验收：普通药水可正常分装，带形态数据的药水被拒绝，空/损坏 NBT 安全处理。
  - 代码已完成（2026-11-19）：读取 SSC Forge 写入的字符串 `targetForm`；缺失、类型不符或无法解析时为 null（SSC 同样不会据此变形）。`ctpAndManaTypeLookups` 覆盖。待实机验证分装。

- [ ] **接回客户端配置保存和 HUD 缩放。** `config/SSCAddonConfig.java` 只创建内存配置对象，`SSCAddonClientConfig.java` 没有加载/保存链路；`client/hud/HudScaling.java:8` 忽略 `scaleId`，仅执行绘制，`hudScalePercent` 未被读取。接配置读写、变更通知与各 HUD 的缩放/锚点。验收：不同 HUD 可独立缩放，位置正确，重启后保留设置，教程已读记录不丢失。
  - 代码已完成（2026-11-19）：客户端配置以 JSON 保存在 `config/ssc_addon-client.json`（首次访问加载、缺省补全、损坏备份为 `.broken`）（`saveClient` 不触发 `ConfigChangeManager`，其监听者是服务端的故事书配置）；颜色教程已读记录写入后立即保存。`HudScaling` 对附属绘制的 mana/cooldown/spellbook/charge 四块按各自 `hudScalePercent` 缩放（50–200%，左上或右缘锚定并钳制在屏幕内）。
  - 仍缺：Forge 端没有 HUD 布局/缩放的游戏内编辑界面，目前只能改 JSON；SSC 自身 HUD（本能、魔法、物品栏、稳定度）不由附属绘制，不受缩放。

- [x] **移植 `ssc_addon:conditional_mana_attribute`。** 2026-11-19：SSC Forge 1.10.0.12 提供 `SscManaMaximumEvent` 后，新增 `power/ConditionalManaAttributePower`（注册为 power 类型并监听上限事件），每次计算上限时对条件成立的该类能力按 Fabric 顺序计算（全部 add → 全部 multiply → 全部 add_total），无持久状态；获得/失去能力时调用 `SscManaApi.synchronizeMana`。从 `JsonTypeRegistrationGameTests` 的待移植名单移除；`conditionalManaAttributeRaisesMaximum` 验证升级使魔解锁 spark 天赋后上限 +10。`regen_mana_modifier` 未支持（附属 JSON 未使用）。待实机验证 HUD 上限与天赋/perk 切换。

### P3：遗留接口、注释与待核对功能

- [x] **清理无调用的占位桥接，避免误认为已接通。** 包括 `util/CustomEdibleUtils.ReloadPlayerCustomEdible`、`compat/CarryOnCompat.isInCarryingAnimation`、`entity/RegAddonEntities` 的三个 `null` 类型、`SscAddon` 中空注册方法/`null` 常量、`RegFormationKnowledgeComponent.Holder.sync`。先确认保留兼容 API 的必要性，再删除或转发真实入口；自定义饮食和 Carry On 若仍需支持，应从真实事件入口接回，而非只填方法体。
  - 已完成（2026-11-19）：删除 `CustomEdibleUtils`、`CarryOnCompat`、`RegAddonEntities`、`RegFormationKnowledgeComponent`，以及 `SscAddon` 中 `null` 常量与空注册方法（编译源中均无调用，只剩 `src/pending` 的 Fabric 引用；`ON_TRANSFORM_ADDON_FORM` 实际入口为 `AddonCriteria`）；更正 `AddonEntities` 的「空壳」注释。自定义饮食与 Carry On 若需支持，应从真实事件入口另行接回。

- [x] **核对领域异常脉冲并修正文档。** `spell/DomainManager.java:26` 明确写着异常压力脉冲待接回，目前 tick 只处理边界、时限、音效和同步。对照当前玩法决定补接还是确认移除；不能因领域本体可用就认为此联动已完成。若保留，验证脉冲频率、白名单和多人负载。
  - 已完成（2026-11-19）：按上游补接——完全展开时对内圈合法目标施加一次空间异常压力，之后每 `domainPulseTicks` 脉冲一次（只查内圈），移除/关服清理。待实机验证频率、白名单与多人负载。

- [x] **核对并清理异常伤害观察钩子。** `anomaly/AnomalyManager.java:50` 的 `damageObserved` 为空，`mixin/FrozenEntityMixin.java:42` 每次有效伤害仍调用它。确认是否有独立观察职责；碎冰已由 `FrozenControl.breakFromHit`、伤后效果已由 `AddonAnomalyEffects.afterDamage` 处理，避免重复实现这些逻辑。
  - 已完成（2026-11-19）：上游 `damageObserved` 只用于在延迟异常队列中保持「碎冰先于同 tick 后续命中」的顺序；Forge 端异常命中同步分发、没有该队列，碎冰与伤后效果已在 `FrozenEntityMixin` 内直接处理，故删除该空方法及其调用。

**本次排除的正常空实现：** 工具类私有构造器、`Spell` 的可选生命周期默认方法、`AddonCriteria.init`（触发静态注册）、`PocketSpaceSpell` 的三参 `cast`（实际五参重载会进入空间）、已改客户端本地粒子的 `FoxFireballEntity.spawnTrail` / `TidalOrbEntity.spawnHoverParticles`、已明确移除观测靶的 `NeoEffects.measureBlock`、Forge 不接入 AsyncParticles 的 `prepare`，以及由 mixin 覆盖的粒子标记默认方法。`ShellMob` / `ShellProjectile` 和 `AddonEntities` 的旧“全为空壳”注释也不能代表当前实体注册情况，当前注册已使用真实实体类。

## 优先：核心玩法

- [ ] 验证法术研究台的客户端操作、多人同步和旧存档中已放置方块的升级。
- [ ] 完成上述祭坛方块接线、符文拓印和经验逻辑后，实机验证配置/维护两种模式切换、书本升级、卷轴修复/拆解/拓印及多人同步（客户端界面已注册）。
- [ ] 实机验证定形锚在诅咒之月、重生、跨维度、客户端提示及特殊变形物品下的表现。
- [ ] 实机验证月织蛛投射物织网（旧法术与新魔法弹体）、跳蛛跳杀触发诅咒法术及其消耗和冷却。
- [ ] 实机验证阿努比斯死亡领域：展开、方块还原（掉线、重载、关服）、领域内冥狼增益。
- [ ] 核对红狐其他玩法：诅咒之月自动变形、形态物品、客户端演出，以及重生和跨维度时限处理。
- [ ] 实机验证各形态技能：技能 JSON 动作/条件与每 tick 驱动已全部接通，需逐形态按键试用并核对冷却。
- [x] 完成上述 perk 映射和占位加成后，再对照上游核对尚无同名实现的 `BatDesmodusPerks`、`BatParasiticFruitPerks`、`SnowFoxSpPerks`、`UpgradeFamiliarFoxPerks`：确认功能是否已被其他服务实现，补齐实际缺失项；继续保留既有远距扑袭停用决定。
  - 2026-11-19 核对：两个蝙蝠类与升级使魔的上游内容只有 `loadMapped`（已由 `AddonPerkTrees` 覆盖）。缺的是三个上游 mixin 的行为，已补：使魔虹吸（`LivingDamageEvent` 吸收后实际扣血 >0.1 且目标为 SSC `enemy` 时回 2/4 点法力，升级使魔用其独立 perk）、药水符放行附属使魔形态（升级使魔需炼金天赋；`FamiliarFoxSpPotionCharmMixin` 以 `@Redirect` 改 SSC `PotionCharmItem.use` 的形态判断，同时列出 `use`/`m_7203_`）、雪狐轻踏方块触及 ×1.5（`forge:block_reach` 修饰符，每 tick 对账）。`elusivePawsScalesBlockReach`、`addonFamiliarCanUsePotionCharm`、`siphonRestoresManaOnHealthDamage` 覆盖。SSC Forge 原版轻踏引用的 `reach-entity-attributes:reach` 在 Forge 上不存在，原版雪狐的该 perk 不生效（属 SSC Forge 问题）。
- [ ] 完成上述通用法阵、崩解、旧法术命中、蝙蝠减益及蓝图倍率的联动待办，再做跨形态回归。

## 物品、效果与方块

- [ ] 实机验证凋零药水的制作入口、多人投掷效果及客户端显示。
- [ ] 实机验证状态效果（狐火、蓝火环、装死、预隐身、霜冻、净化、潮汐减速、诅咒印记、霜碎、变羊、蛛网缠身、月尘试管）在多人下的表现。
- [ ] 实机做 Curios 佩戴和卸下测试，核对各饰品技能效果。
- [ ] 实机验证便携冰箱（给雪球发射器装弹、自回充）与便携加湿器（回复湿润度、淋雨回充、三级 +15% 伤害）。
- [ ] 实机核对法术抗性在附魔台、铁砧和多人战斗中的表现。

## 兼容、界面与验证

- [ ] 实机核对异常反应的粒子、音效和提示，以及束界传送限制。
- [ ] 对照上游检查寄生果实选择、稳定度 HUD、教程和进化界面，补齐所需文本及素材。
  - 2026-11-19：补齐上游有而本地缺的 35 张贴图（进化界面图标/皮肤、寄生果实与月织蛛模式技能图标、升级美西螈水冲刺图标、`ssc_addon:textures/gui/slot/spellbook.png` 饰品槽图标）和 323 条语言键（其中 290 条为教程课程文本，本地教程界面按同一键名读取）。
  - 寄生果实选择已移植（2026-11-19）：`ability/fruit/`（`FruitSelection`、`FruitLayerLedger`、`ParasiticFruitLayers`、`FruitStatusOverlay`、`ParasiticFruitAbility`）、重写的 `ParasiticFruitSeedPower`、`ParasiticFruitStatusMixin`、投掷物与种子圈携带所选果实、`client/ParasiticFruitClient`。手势改由服务端识别：SSC 会把技能键按下/松开分派给 `SscPowerType`，按住 ≥0.5 秒松开投掷（蓄力决定速度，满 1.5 秒），两次短按切换果实；所选果实存于玩家持久数据（跨死亡/重登保留），换形态重置为自动。技能栏第三槽显示所选果实，主技能条显示蓄力，达到最小/满蓄力各一声提示；月织蛛织网模式改为服务端同步后显示模式图标（此前客户端只在单人时碰巧读到）；升级美西螈水冲刺换用专用图标。能力 JSON 描述同步上游。`ParasiticFruitGameTests` 覆盖账本、双击切换、换形态重置、种植结果与叠加层撤销。待实机验证手感（服务端计时受网络延迟影响）、多人共享层与描边。
  - 仍缺：上游进化界面使用的 `EvolutionArtwork`/`EvolutionInk` 皮肤未移植（贴图已就位）。
- [ ] 核对 JEI、其他可选模组兼容及平台差异。
- [ ] 完成单机、独立服务端、多人同步、存档重载和客户端视觉测试。
- [x] 补 `temp_web_bridge` 方块状态 JSON 缺少的 `facing` 变体模型（客户端加载时报 4 条警告）。2026-11-19：新增 `assets/ssc_addon/blockstates/temp_web_bridge.json`，四个朝向复用 SSC 的 `web_bridge_0` 模型。

## 已完成的基础工作

- [x] 接通异常反应的元素粒子、圆环、音效、提示及密集场景限频。
- [x] 接通束界的主动传送守卫和空间法术施法前拦截，解除效果后恢复传送。
- [x] 注册法术抗性附魔，接通法术和异常反应伤害减免及保护类附魔互斥。
- [x] 修正旧形态 ID 的注册查询，并注册进化石目标和进化树起点。
- [x] 接入进化石基本使用、目标选择、分支检查和成功消耗。
- [x] 接回灵能宝珠及配方，以及一批现有物品和饰品的注册入口。
- [x] 将法术研究台接入已有方块实体和菜单，并实现研究知识的世界存档与客户端同步。
- [x] 实现定形锚和解锚符的基本锁定、解除、按玩家存档与变形拦截。
- [x] 接通凋零药水的饮用、喷溅和滞留投掷及两步酿造配方。
- [x] 接通月髓环的进化树分支门控、临时红狐到期回退和剧情红狐免费变回入口。
- [x] 注入祭坛：移植客户端界面并注册到菜单类型（仅此步骤完成；真实方块接线与拓印仍见 P1）。
- [x] 技能 JSON：移植全部自定义动作（`SscAddonActions`）与条件（`SscAddonConditions`）；SSC Forge 的双实体动作不查 Java 注册表，由 `FormPowerRuntimeBiEntityMixin` 补分派；失败冷却 `markFail` 不再是空操作。
- [x] 技能驱动：移植 `SscAddonServerEvents`（各形态技能每 tick 推进、生命周期清理、STUN 残留修正、水矛上限），并接回 Forge 入口遗漏的二十余个服务端管理器初始化。
- [x] 移植阿努比斯死亡领域、冥狼被动召唤与魂召回；修正法术召唤冥狼时每只额外多生成两只的问题。
- [x] 月织蛛：蛛网膜铺设（`coatArea`）、触碰减速/缠身/高亮、可燃；投射物织网（旧法术与新魔法弹体）；跳蛛跳杀触发诅咒法术（`triggerSpiderCurse`、触发式施法通道）及跳杀/毒液按 perk 结算冷却。
- [x] 形态×法术联动：移植 `FormSpellStyle` 与狐/豹猫/悦灵/蝙蝠/冥裁/梦魇各联动类、`FormCastingStyle`，新魔法命中接入织网、冥裁、悦灵连锁、蝙蝠扩散和湿润治疗批次；施法取消时结束真隐身掩护。
- [x] 空壳状态效果全部换回真实效果类，删除 `ShellEffect`。
- [x] 便携冰箱、便携加湿器改为 SSC Forge 饰品并移植佩戴逻辑。
- [x] 战利品：饰品宝箱/猫掉落、进化物品、法阵、魔法卷轴、故事书均通过 `LootTableLoadEvent` 注入。
- [x] 物品染色与模型谓词（2026-11-19）：此前 Forge 端没有任何物品颜色处理器，卷轴/法阵图案、药水试管、月尘舒缓剂与凋零药水液体均显示为未染色。新增 `client/AddonItemColors`：卷轴与增强法阵按法术系别染图案层、法阵按 NBT 系别染色、试管取药水色、月尘舒缓剂默认月尘色（或 CustomPotionColor）、凋零药水液体色；补注册法阵 `universal` 与水矛 `held`/`throwing` 模型谓词（通用法阵独立模型、手持 3D 水矛此前不会切换）。待实机确认外观。
