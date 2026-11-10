/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * 瀵勭敓鏋滆潬娆¤鎶€鑳斤細鎰熸煋瀛㈠瓙鐐稿脊鐨勬姇鎺风墿銆?
 * 椋炶鏃舵嫋鍑虹豢鑹茬矑瀛愶紝鍛戒腑鏂瑰潡鎴栫敓鐗╂椂鏃犱激瀹崇垎鐐革紝鍚?EXPLOSION_RADIUS 鑼冨洿鍐?
 * 闈炵櫧鍚嶅崟鐢熺墿鏂藉姞鎰熸煋瀛㈠瓙鐘舵€併€?
 */
public class InfectionSporeBombEntity extends ThrowableItemProjectile {
    // 浠ヤ笅鍧囦负榛樿鍊硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.parasitic_spore_bomb锛?
    /** 鐖嗙偢鐗规晥浣滅敤鍗婂緞 */
    public static final double EXPLOSION_RADIUS = 4.0;
    /** 榛樿鎰熸煋/娌荤枟鏃堕暱锛?0s锛夛細鍛戒腑鐢熺墿涓庤惤鍦版瘨闆句簯缁熶竴 */
    public static final int DEFAULT_INFECTION_TICKS = 200;
    /** 澧ㄧ豢鑹蹭腑姣掔矑瀛愶紙涓?StatusEffects.POISON 瑙嗚鑹茶皟涓€鑷达級 */
    private static final DustParticleOptions POISON_DUST = new DustParticleOptions(new Vector3f(0.30f, 0.50f, 0.10f), 1.0f);

    /** balance 蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_spore_bomb");

    public InfectionSporeBombEntity(EntityType<? extends InfectionSporeBombEntity> entityType, Level world) {
        super(entityType, world);
    }

    public InfectionSporeBombEntity(Level world, LivingEntity owner) {
        super(github.com.gengyoubo.sscfa.SscAddon.INFECTION_SPORE_BOMB_ENTITY, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        // 瑙嗚妯″瀷锛氬彶鑾卞鐞?
        return Items.SLIME_BALL;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // 椋炶灏捐抗锛氬ⅷ缁胯壊涓瘨绮掑瓙锛堣交閲忥紝姣?tick 2 涓級
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(POISON_DUST,
                    this.getX(), this.getY() + 0.1, this.getZ(),
                    2, 0.06, 0.06, 0.06, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level().isClientSide) return;
        if (this.isRemoved()) return;

        explode();
        // 钀藉湴锛堝懡涓柟鍧楋級鏃堕澶栫敓鎴愪竴鍥㈡粸鐣欐瘨闆句簯锛氳繘鍏ヨ€呮寜浜戝墿浣欏鍛借鎰熸煋/娌荤枟
        if (hitResult.getType() == HitResult.Type.BLOCK
                && this.level() instanceof ServerLevel sw
                && this.getOwner() instanceof ServerPlayer caster) {
            // 钀藉湴棰濆鐗规晥锛氳嵂姘寸矑瀛愬悜涓婇鏁?
            spawnRisingPotionParticles(sw, hitResult.getLocation());
            InfectionSporeManager.spawnCloud(caster, sw, hitResult.getLocation(),
                    InfectionSporeManager.cloudRadius(), BAL.i("default_infection_ticks", DEFAULT_INFECTION_TICKS));
        }
        this.discard();
    }

    /** 閫氱敤纰版挒棰勮繃婊わ細蹇界暐鍙戝皠鑰呮湰浜恒€?*/
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel world)) return;
        Vec3 pos = this.position();

        // 鐖嗙偢鐗规晥锛氱矑瀛?+ 闊虫晥锛堟棤浠讳綍瀹炰綋浼ゅ锛?
        // 寮瑰嚭鑺傚涓嶅彉锛氱墿鍝佺鐗囨暎灏勶紙璐村浘鏀逛负鍙茶幈濮嗙悆锛?+ 涓讳綋绮掑瓙鏀逛负澧ㄧ豢鑹蹭腑姣掕壊
        ItemParticleOption itemFx = new ItemParticleOption(ParticleTypes.ITEM, Items.SLIME_BALL.getDefaultInstance());
        world.sendParticles(itemFx, pos.x, pos.y, pos.z, 18, 0.4, 0.4, 0.4, 0.15);
        world.sendParticles(POISON_DUST, pos.x, pos.y + 0.2, pos.z, 30, 0.6, 0.4, 0.6, 0.05);
        world.sendParticles(POISON_DUST, pos.x, pos.y + 0.2, pos.z, 25, 1.5, 1.0, 1.5, 0.0);
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);

        // 浠呭綋鍙戝皠鑰呮槸鏈嶅姟绔帺瀹舵椂杩涜鐧藉悕鍗曞垽瀹氫笌鎰熸煋
        ServerPlayer caster = (this.getOwner() instanceof ServerPlayer sp) ? sp : null;
        if (caster == null) return;

        double explosionRadius = BAL.d("explosion_radius", EXPLOSION_RADIUS);
        int infectionTicks = BAL.i("default_infection_ticks", DEFAULT_INFECTION_TICKS);
        AABB box = new AABB(pos.subtract(explosionRadius, explosionRadius, explosionRadius),
                pos.add(explosionRadius, explosionRadius, explosionRadius));
        double sqRadius = explosionRadius * explosionRadius;
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.distanceToSqr(pos) <= sqRadius && e != caster);

        for (LivingEntity target : targets) {
            // 鍙楃櫧鍚嶅崟淇濇姢鐨勫弸鏂癸紙鐜╁銆佸疇鐗╃瓑锛夆啋 鎺ュ彈娌荤枟瀛㈠瓙锛堟瘡 3s +1HP锛屾寔缁?15s锛?
            // 闈炵櫧鍚嶅崟鐩爣 鈫?鎺ュ彈鎰熸煋瀛㈠瓙
            if (WhitelistUtils.isProtected(caster, target)) {
                InfectionSporeManager.applyFriendHeal(caster, target, infectionTicks);
            } else {
                InfectionSporeManager.infect(caster, target, infectionTicks);
            }
        }
    }

    /** 钀藉湴棰濆鐗规晥锛氳嵂姘寸矑瀛愬悜涓婇鏁ｏ紙count=0 鏃跺悗涓変綅涓洪€熷害锛寁y>0 鍗充笂椋橈級銆?*/
    private void spawnRisingPotionParticles(ServerLevel w, Vec3 pos) {
        for (int i = 0; i < 14; i++) {
            double vx = (this.random.nextDouble() - 0.5) * 0.05;
            double vy = 0.08 + this.random.nextDouble() * 0.08;   // 姝ｅ悜涓?鈫?涓婇
            double vz = (this.random.nextDouble() - 0.5) * 0.05;
            w.sendParticles(ParticleTypes.EFFECT,
                    pos.x + (this.random.nextDouble() - 0.5) * 0.8,
                    pos.y + 0.1,
                    pos.z + (this.random.nextDouble() - 0.5) * 0.8,
                    0, vx, vy, vz, 1.0);
        }
    }
}
