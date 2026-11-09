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
 * 瀵勭敓鏋滆潬涓绘妧鑳姐€岀伒鏋滃瘎鐢熴€嶈惤鍦扮瀛愬湀绠＄悊鍣ㄣ€?
 * <p>
 * 鎶曟幏鐗╂湭鍛戒腑鐢熺墿銆佽惤鍦版椂鐢熸垚涓€涓€岀伒鏋滅瀛愬湀銆嶏細鍦伴潰缁胯壊娣峰嚌鍦熸牳蹇冿紙block_marker 绮掑瓙锛屼笉鏀逛笘鐣屾柟鍧楋級
 * + 缁胯壊娌荤枟鐜矑瀛愶紙瀛?RC4 healing_crystal 骞剁豢鑹插寲锛夛紱鍗婂緞 1 鍐呬换鎰忕帺瀹惰蛋鍏ュ嵆鎷惧彇锛?
 * 鑾峰緱瀵勭敓鏁堟灉锛堟椂闀?= 鏈€澶ф椂闀?鈭?宸茶惤鍦版椂闀匡紝鐢辨柦娉曡€呮寔鏈夌殑 power 鏂藉姞锛屽弸/鏁屾灉瀹炶嚜鍔ㄥ垽瀹氾級锛?
 * 瀵垮懡鑰楀敖鎴栬鎷惧彇鍚庢秷澶憋紝娑堝け鏃跺柗缁胯壊绮掑瓙銆?
 * <p>
 * 鎷惧彇鐢辨湇鍔＄鍒ゅ畾锛涙寔缁矑瀛愪笌鏍稿績鑷浆鐢卞鎴风鎸夋湇鍔＄鏃堕棿杞寸敓鎴愩€?
 */
public final class ParasiticSeedFieldManager {
    /** 鎷惧彇 / 娌荤枟鐜崐寰勶紙鏍硷級锛氶粯璁ゅ€硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.parasitic_seed_system锛?*/
    public static final double FIELD_RADIUS = 1.0;

    /** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
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

    /** 鎶曟幏鐗╄惤鍦版椂璋冪敤锛氬湪钀界偣鐢熸垚涓€涓伒鏋滅瀛愬湀锛屽鍛?= lifeTicks銆傚弻鐢熺鑽锋椂鏍稿績鍥炬爣涓哄弻鐢熺鑽枫€?*/
    public static void spawnField(ServerPlayer caster, ServerLevel world, Vec3 pos, int lifeTicks, boolean twinPod) {
        long now = world.getGameTime();
        // 鎮诞鐨勬牳蹇冨浘鏍囷紙small armor_stand 澶存埓锛屼豢 RC4 healing_crystal锛夛細鍙岀敓绉嶈嵎鏃朵负鍙岀敓绉嶈嵎锛屽惁鍒欑伀鎶婅姳绉嶅瓙
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

            // 鍒版湡娑堝け + 缁胯壊绮掑瓙
            if (now >= f.endTick) {
                killStand(world, f);
                spawnDisappearParticles(world, f.pos);
                FIELDS.remove(f);
                continue;
            }

            // 瑙嗚锛氱豢鑹叉贩鍑濆湡鏍稿績 + 鏃嬭浆缁胯壊娌荤枟鐜?
            drawField(world, f, now);

            // 鍗婂緞 1 鍐呬换鎰忓瓨娲荤敓鐗╄Е鍙戯紙鐜╁+鎬墿+鍔ㄧ墿锛屽弸/鏁屾灉鐢辩櫧鍚嶅崟鍒ゅ畾锛夛紱鎺掗櫎绉嶅瓙鍦?armor_stand銆?
            // 鍏佽鏂芥硶鑰呮湰浜鸿Е鍙戯紙瀹㈡満鍙悆鑷繁绉嶅瓙锛夛紝浣嗛渶绉嶅瓙钀藉湴 鈮?0t 鍚庯紝閬垮厤鍒氭墧鍑鸿鑷繁绔嬪嵆韪╁彂
            ServerPlayer caster = server.getPlayerList().getPlayer(f.casterUuid);
            if (caster == null) continue;   // 鏂芥硶鑰呯绾垮垯淇濈暀绉嶅瓙鍦堬紝绛夊叾涓婄嚎鎴栬嚜鐒跺埌鏈?
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

    /** 缁胯壊娣峰嚌鍦熸牳蹇?+ 鏃嬭浆缁胯壊娌荤枟鐜紙瀛?RC4 healing_crystal 骞剁豢鑹插寲锛屽崐寰?1锛夈€?*/
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

    /** 绉婚櫎绉嶅瓙鍦堢殑鎮诞鏂瑰潡 armor_stand銆?*/
    private static void killStand(ServerLevel world, SeedField f) {
        Entity stand = world.getEntity(f.standUuid);
        if (stand != null) stand.discard();
    }
}
