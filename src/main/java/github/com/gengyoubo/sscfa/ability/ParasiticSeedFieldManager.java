/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 寄生果蝠主技能「灵果寄生」落地种子圈管理器?
 * <p>
 * 投掷物未命中生物、落地时生成一个「灵果种子圈」：地面绿色混凝土核心（block_marker 粒子，不改世界方块）
 * + 绿色治疗环粒子（?RC4 healing_crystal 并绿色化）；半径 1 内任意玩家走入即拾取?
 * 获得寄生效果（时?= 最大时??已落地时长，由施法者持有的 power 施加，友/敌果实自动判定）?
 * 寿命耗尽或被拾取后消失，消失时喷绿色粒子?
 * <p>
 * 拾取由服务端判定；持续粒子与核心自转由客户端按服务端时间轴生成?
 */
public final class ParasiticSeedFieldManager {
    /** 拾取 / 治疗环半径（格）：默认值；运行时从 balance 快照读取（abilities.parasitic_seed_system?*/
    public static final double FIELD_RADIUS = 1.0;

    /** balance 快照读取（快照未初始化回退默认常量?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    private static final CopyOnWriteArrayList<SeedField> FIELDS = new CopyOnWriteArrayList<>();

    private ParasiticSeedFieldManager() {
    }

    private static final class SeedField {
        final UUID casterUuid;
        final ResourceKey<Level> worldKey;
        final Vec3 pos;
        final long spawnTick;
        final long endTick;
        final UUID standUuid;
        final boolean twinPod;

        SeedField(UUID casterUuid, ResourceKey<Level> worldKey, Vec3 pos, long spawnTick, long endTick, UUID standUuid, boolean twinPod) {
            this.casterUuid = casterUuid;
            this.worldKey = worldKey;
            this.pos = pos;
            this.spawnTick = spawnTick;
            this.endTick = endTick;
            this.standUuid = standUuid;
            this.twinPod = twinPod;
        }
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onWorldTick(ParasiticSeedFieldManager::onWorldTick);
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopping(server -> {
            for (SeedField field : FIELDS) {
                ServerLevel world = server.getLevel(field.worldKey);
                if (world != null) killStand(world, field);
            }
            FIELDS.clear();
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityLoad((entity, world) -> {
            if (entity.getTags().contains("ssca_seed_field")
                    && FIELDS.stream().noneMatch(field -> field.standUuid.equals(entity.getUUID()))) entity.discard();
        });
    }

    /** 投掷物落地时调用：在落点生成一个灵果种子圈，寿?= lifeTicks。双生种荷时核心图标为双生种荷?*/
    public static void spawnField(ServerPlayer caster, ServerLevel world, Vec3 pos, int lifeTicks, boolean twinPod) {
        long now = world.getGameTime();
        // 悬浮的核心图标（small armor_stand 头戴，仿 RC4 healing_crystal）：双生种荷时为双生种荷，否则火把花种子
        ArmorStand stand = new ArmorStand(world, pos.x, pos.y, pos.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setSilent(true);
        stand.setInvulnerable(true);
        CompoundTag nbt = new CompoundTag();
        stand.addAdditionalSaveData(nbt);
        nbt.putBoolean("Small", true);
        nbt.putBoolean("Marker", true);
        nbt.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(nbt);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(twinPod
                ? github.com.gengyoubo.sscfa.init.AddonItems.TWIN_POD.get()
                : Items.TORCHFLOWER_SEEDS));
        stand.addTag("ssca_seed_field");
        SeedField field = new SeedField(caster.getUUID(), world.dimension(), pos, now,
                now + Math.max(20, lifeTicks), stand.getUUID(), twinPod);
        FIELDS.add(field);
        if (!world.addFreshEntity(stand)) { FIELDS.remove(field); return; }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        world.sendParticles(ParticleTypes.WARPED_SPORE, pos.x, pos.y + 0.2, pos.z, 30, 0.5, 0.3, 0.5, 0.02);
    }

    private static void onWorldTick(ServerLevel world) {
        if (FIELDS.isEmpty()) return;
        long now = world.getGameTime();
        MinecraftServer server = world.getServer();
        for (SeedField f : FIELDS) {
            if (!f.worldKey.equals(world.dimension())) continue;

            // 到期消失 + 绿色粒子
            if (now >= f.endTick) {
                killStand(world, f);
                spawnDisappearParticles(world, f.pos);
                FIELDS.remove(f);
                continue;
            }

            // 视觉：绿色混凝土核心 + 旋转绿色治疗?
            drawField(world, f, now);

            // 半径 1 内任意存活生物触发（玩家+怪物+动物，友/敌果由白名单判定）；排除种子?armor_stand?
            // 允许施法者本人触发（客机可吃自己种子），但需种子落地 ?0t 后，避免刚扔出被自己立即踩发
            ServerPlayer caster = server.getPlayerList().getPlayer(f.casterUuid);
            if (caster == null) continue;   // 施法者离线则保留种子圈，等其上线或自然到?
            boolean casterArmed = now - f.spawnTick >= 10;
            double fieldRadius = BAL.d("field_radius", FIELD_RADIUS);
            AABB box = new AABB(f.pos.subtract(fieldRadius, fieldRadius, fieldRadius),
                    f.pos.add(fieldRadius, fieldRadius, fieldRadius));
            double sqRadius = fieldRadius * fieldRadius;
            List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box,
                    e -> ParasiticFruitSeedPower.isValidSeedHost(caster, e)
                            && (casterArmed || e != caster)
                            && e.distanceToSqr(f.pos) <= sqRadius);
            if (!nearby.isEmpty()) {
                if (f.twinPod) {
                    // Spread once: overlapping pickups must not plant the same twin pair twice.
                    LivingEntity picker = nearby.stream().min(java.util.Comparator.comparingDouble(
                            e -> e.distanceToSqr(f.pos))).orElseThrow();
                    ParasiticFruitSeedPower.plantSeedSpread(caster, picker, true);
                } else {
                    // Preserve the ordinary field's simultaneous-pickup behavior.
                    for (LivingEntity picker : nearby) {
                        ParasiticFruitSeedPower.plantSeedSpread(caster, picker, false);
                    }
                }
                spawnPickupParticles(world, f.pos);
                killStand(world, f);
                FIELDS.remove(f);
            }
        }
    }

    /** 绿色混凝土核?+ 旋转绿色治疗环（?RC4 healing_crystal 并绿色化，半?1）?*/
    private static void drawField(ServerLevel world, SeedField f, long now) {
        Entity stand = world.getEntity(f.standUuid);
        if (stand != null) {
            github.com.gengyoubo.sscfa.network.SustainedVisuals.touch(stand,
                    github.com.gengyoubo.sscfa.network.VisualRecipe.Kind.SEED_FIELD,
                    (int) (now - f.spawnTick), (int) (f.endTick - f.spawnTick), BAL.d("field_radius", FIELD_RADIUS), 0);
        }
    }

    private static void spawnPickupParticles(ServerLevel world, Vec3 pos) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.9f, 1.3f);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.3, pos.z, 18, 0.4, 0.5, 0.4, 0.0);
        world.sendParticles(ParticleTypes.WARPED_SPORE, pos.x, pos.y + 0.3, pos.z, 20, 0.5, 0.4, 0.5, 0.02);
    }

    private static void spawnDisappearParticles(ServerLevel world, Vec3 pos) {
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.6f, 0.9f);
        world.sendParticles(ParticleTypes.WARPED_SPORE, pos.x, pos.y + 0.2, pos.z, 24, 0.5, 0.3, 0.5, 0.03);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.2, pos.z, 10, 0.4, 0.3, 0.4, 0.0);
    }

    /** 移除种子圈的悬浮方块 armor_stand?*/
    private static void killStand(ServerLevel world, SeedField f) {
        Entity stand = world.getEntity(f.standUuid);
        if (stand != null) stand.discard();
    }
}
