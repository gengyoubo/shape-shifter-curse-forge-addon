package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.item.AllayJukeboxItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.UUID;

/**
 * SP悦灵唱片机区域效果Tick逻??
 * - 回血模式：??秒??0格内白名单生物回1HP
 * - 加速模式：持续??0格内白名单生???10%移??
 * - 每秒消?点充?
 * - 播放自定义音?
 */
public class AllaySPJukebox {
    private AllaySPJukebox() {
        // Utility class
    }

    // 唱片机参数：默认与注释一致；运行时??balance 快照读取（abilities.allay_sp_jukebox??
    public static final double RANGE = 20.0;
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a3b4c5d6-e7f8-9012-3456-789abcdef012");
    private static final String SPEED_MODIFIER_NAME = "allay_jukebox_speed";
    private static final double SPEED_BONUS = 0.10; // 默认?0% speed

    // 增??status effect 刷新时长（tick）：?5 tick 补挂一次，20t 覆盖刷新间隔保证图标常亮不闪断???
    // 黄??ABSORPTION)用此时长：退出回血模式后停止刷新，剩??effect 会????0t 内过期???
    // AbsorptionStatusEffect.onRemoved 会自动扣除它施加的护盾，不误删玩家其它来源护盾??
    private static final int BUFF_REFRESH_DURATION = 20;

    // 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
    private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
            new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_jukebox");

    // Track per-player: -1 = not playing, 0 = speed music, 1 = heal music
    private static final java.util.concurrent.ConcurrentHashMap<UUID, Integer> playerMusicState = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<net.minecraft.server.MinecraftServer, SourceLeaseBook<LivingEntity, UUID>> SPEED_LEASES = new java.util.IdentityHashMap<>();

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> { if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return; var server = event.getServer();
            var leases = SPEED_LEASES.get(server);
            if (leases != null) leases.expire(server.overworld().getGameTime()).forEach(AllaySPJukebox::removeSpeedModifier);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> { var server = event.getServer();
            var leases = SPEED_LEASES.remove(server);
            if (leases != null) leases.clear().forEach(AllaySPJukebox::removeSpeedModifier);
            playerMusicState.clear();
        });
    }

    private static SourceLeaseBook<LivingEntity, UUID> leases(ServerPlayer player) {
        return SPEED_LEASES.computeIfAbsent(player.getServer(), ignored -> new SourceLeaseBook<>());
    }

    /**
     * 玩家断线时清理音乐状态并移除速度加成，防止内存泄漏和其他玩家永久保留速度buff
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        removeSpeedFromAll(player);
        Integer currentState = playerMusicState.remove(player.getUUID());
        if (currentState != null && currentState != -1) {
            stopAllMusic(player);
            playerMusicState.remove(player.getUUID());
            removeSpeedFromAll(player);
        }
    }

    /**
     * 收集应当听到该唱片机音乐的玩家：持有者本?+ 追踪持有者的所有玩家（即附近能看到他的玩家）??
     * PlayerLookup.tracking 不含玩家自己，故手动加入持有者?
     * 修复：客机播放时其他人听不见（原先只发持有者）——改为向附近玩家范围广播，主客机一致??
     */
    private static java.util.Set<ServerPlayer> getAudience(ServerPlayer player) {
        java.util.Set<ServerPlayer> audience = new java.util.HashSet<>(player.serverLevel().players());
        audience.add(player);
        return audience;
    }

    /**
     * Stop all jukebox music for a player by sending StopSoundS2CPacket
     */
    public static void stopAllMusic(ServerPlayer player) {
        // 向持有?+ 附近所有玩家发停止包，保证范围广播的音乐对所有听众都能停下（主客机一致??
        for (ServerPlayer audience : getAudience(player)) {
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.getId(), SoundSource.RECORDS));
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.getId(), SoundSource.RECORDS));
        }
        playerMusicState.put(player.getUUID(), -1);
    }

    /**
     * Stop old music and immediately play the new mode's music from the beginning
     */
    private static void switchMusic(ServerPlayer player, int newMode) {
        SoundEvent newSound = (newMode == AllayJukeboxItem.MODE_SPEED) ? github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.get() : github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.get();
        Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(newSound);
        // 所有听众用同一随机种子，保证范围内玩家听到的音乐起播一??
        long seed = player.getRandom().nextLong();
        // 向持有者和追踪者广播：只停止本功能两个明确的音??ID，再播放新模式?
        for (ServerPlayer audience : getAudience(player)) {
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.getId(), SoundSource.RECORDS));
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.getId(), SoundSource.RECORDS));
            // 音??0.12（??0.06 ??2 倍）；用实体跟踪音效包，让音乐声源跟随持有者移动（而非钉在释放位置???
            audience.connection.send(new ClientboundSoundEntityPacket(entry, SoundSource.RECORDS,
                    player, 0.25f, 1.0f, seed));
        }

        playerMusicState.put(player.getUUID(), newMode);
    }


    /**
     * Called every tick for each allay_sp player from the server tick event
     */
    public static void tick(ServerPlayer player) {
        // 廉价形态门控前置：先判形态再??SkillBlocker（字符串拼??+ commandTags 线性扫描）???
        // 非悦?SP 玩家???tick 只花一???CCA 查询（原还要先过 SkillBlocker 才判形态）
        net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
        boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
        if (!isAllaySp) {
            Integer currentState = playerMusicState.getOrDefault(player.getUUID(), -1);
            if (currentState != -1) {
                stopAllMusic(player);
                removeSpeedFromAll(player);
            }
            return;
        }
        if (SkillBlocker.isSkillBlocked(player, "allay", "jukebox_charge")) {
            return;
        }

        // Find jukebox item in inventory (should be in slot 1)
        ItemStack jukeboxStack = player.getInventory().getItem(1);
        
        // If item is missing, treat as inactive -> cleanup
        if (!jukeboxStack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get())) {
            Integer currentState = playerMusicState.getOrDefault(player.getUUID(), -1);
            if (currentState != -1) {
                stopAllMusic(player);
                removeSpeedFromAll(player);
            }
            return;
        }

        boolean isActive = AllayJukeboxItem.isActive(jukeboxStack);

        if (!isActive) {
            // Remove speed modifiers from all nearby entities when deactivated
            removeSpeedFromAll(player);
            // Stop music if it was playing
            Integer lastState = playerMusicState.get(player.getUUID());
            if (lastState != null && lastState != -1) {
                stopAllMusic(player);
            }
            return;
        }

        int charge = AllayJukeboxItem.getCharge(jukeboxStack);
        if (charge <= 0) {
            // Out of charge, deactivate
            AllayJukeboxItem.setActive(jukeboxStack, false);
            removeSpeedFromAll(player);
            stopAllMusic(player);
            return;
        }

        // Consume 1 charge per second (every 20 ticks)
        if (player.tickCount % 20 == 0) {
            AllayJukeboxItem.setCharge(jukeboxStack, charge - 1);
        }

        int mode = AllayJukeboxItem.getMode(jukeboxStack);

        // ===== Music: ??tick 检??mode 变化（不依赖实体扫描，保证切模即时??=====
        Integer lastMusicMode = playerMusicState.getOrDefault(player.getUUID(), -1);
        if (lastMusicMode != mode) {
            // Mode changed or first time: stop old, play new immediately
            switchMusic(player, mode);
        }

        // 性能优化：范围实体扫??getEntitiesByClass) + buff 套??移??降频到???5 tick 一次（原每 tick）??
        // applySpeedModifier 幂等（已有则跳过），回血本就???60t、cleanup 本就???40t（均?5 的倍数，命中不变）??
        // 新进范围者最多???5t 获加?被回血，肉眼不可察???
        if (player.tickCount % 5 == 0) {
            List<LivingEntity> nearbyEntities = getNearbyWhitelistEntities(player);

            if (mode == AllayJukeboxItem.MODE_HEAL) {
                // Heal mode: every 3 seconds (60 ticks), heal 1 HP
                if (player.tickCount % 60 == 0) {
                    for (LivingEntity entity : nearbyEntities) {
                        entity.heal(1.0f);
                    }
                }
                // 额外增益：抵???I??0% 减伤，短时刷新?? 黄心（ABSORPTION，amplifier 0 = 4HP = 2 颗，同模式刷新??
                // 退出回血模式后停止刷新，剩??effect ????0t 内过期，onRemoved 自动扣除它施加的护盾，不误删玩家其它护???
                int buffRefreshDuration = BAL.i("buff_refresh_duration", BUFF_REFRESH_DURATION);
                for (LivingEntity entity : nearbyEntities) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffRefreshDuration, 0, false, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, buffRefreshDuration, 0, false, false, true));
                }
                // Remove speed modifier when in heal mode
                removeSpeedFromAll(player);
            } else {
                // Speed mode: apply 10% speed modifier
                int buffRefreshDuration = BAL.i("buff_refresh_duration", BUFF_REFRESH_DURATION);
                for (LivingEntity entity : nearbyEntities) {
                    leases(player).renew(entity, player.getUUID(), player.getServer().overworld().getGameTime() + 20);
                    applySpeedModifier(entity);
                    // 额外增益：急??I（短时刷新，?5 tick 补挂???
                    entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, buffRefreshDuration, 0, false, false, true));
                }
                // Clean up speed modifiers from entities that moved out of range
                cleanupOutOfRangeEntities(player, nearbyEntities);
            }
        }
    }

    private static List<LivingEntity> getNearbyWhitelistEntities(ServerPlayer player) {
        double range = BAL.d("range", RANGE);
        AABB box = new AABB(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range
        );

        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            double dist = entity.distanceToSqr(player);
            if (dist > range * range) return false;
            if (entity == player) return true;
            // Use the allay whitelist
            return AllaySPGroupHeal.isInWhitelist(player, entity);
        });
    }

    private static void applySpeedModifier(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
        if (existing == null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_UUID, SPEED_MODIFIER_NAME,
                    BAL.d("speed_bonus", SPEED_BONUS), AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        speedAttr.removeModifier(SPEED_MODIFIER_UUID);
    }

    private static void removeSpeedFromAll(ServerPlayer player) {
        var book = SPEED_LEASES.get(player.getServer());
        if (book != null) book.release(player.getUUID()).forEach(AllaySPJukebox::removeSpeedModifier);
    }

    private static void cleanupOutOfRangeEntities(ServerPlayer player, List<LivingEntity> inRange) {
        leases(player).retain(player.getUUID(), new java.util.HashSet<>(inRange))
                .forEach(AllaySPJukebox::removeSpeedModifier);
    }
}
