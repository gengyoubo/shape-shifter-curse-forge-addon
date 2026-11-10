/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/**
 * 瀵勭敓鏋滆潬涓绘妧鑳姐€岀伒鏋滃瘎鐢熴€嶇殑鎶曟幏鐗╋紙鎶涚墿绾匡紝閫熷害鍚屽瀛愮偢寮癸級銆?
 * 鍛戒腑鐢熺墿 鈫?璋冪敤 {@link ParasiticFruitSeedPower#plantSeedFrom} 鍦ㄥ涓昏韩涓婄涓嬬伒鏋滅瀛愶紙淇濈暀鍘熺敓鏍圭粨鏋滄晥鏋滐級銆?
 * 鍛戒腑鏂瑰潡锛堣惤鍦帮級鈫?闃舵2 灏嗙敓鎴愬湴闈㈢瀛愬湀锛涢樁娈? 鏆備粎钀藉湴绮掑瓙 + 闊虫晥銆?
 * 鍏ㄩ儴鍒ゅ畾鍦ㄦ湇鍔＄鎵ц锛岀‘淇濆浜虹幆澧冧富瀹㈡満涓€鑷淬€?
 */
public class ParasiticSeedProjectile extends ThrowableItemProjectile {
    /** 椋炶灏捐抗锛氱伒鏋滅豢鑹茬矑瀛?*/
    private static final DustParticleOptions SEED_TRAIL = new DustParticleOptions(new Vector3f(0.35f, 0.95f, 0.30f), 1.0f);
    /** 钀藉湴绉嶅瓙鍦堝鍛斤紙tick锛屽尮閰嶄富鎶€鑳?duration 榛樿 240=12s锛?*/
    private static final int DEFAULT_FIELD_LIFE = 240;

    // 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_projectile");
    /** 鏄惁瑁呭鍙岀敓绉嶈嵎锛堝懡涓?钀藉湴鏃跺惎鐢ㄦ墿鏁ｏ細棰濆瀵勭敓 1 浜猴紝鏃犱汉鍒欏彔 2 灞傦級銆?*/
    private boolean twinPod = false;

    public void setTwinPod(boolean twinPod) {
        this.twinPod = twinPod;
    }

    public ParasiticSeedProjectile(EntityType<? extends ParasiticSeedProjectile> entityType, Level world) {
        super(entityType, world);
    }

    public ParasiticSeedProjectile(Level world, LivingEntity owner) {
        super(github.com.gengyoubo.sscfa.SscAddon.PARASITIC_SEED_ENTITY, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        // 瑙嗚妯″瀷锛氬皬楹︾瀛?
        return Items.WHEAT_SEEDS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // 椋炶灏捐抗锛氱豢鑹茬伒鏋滅矑瀛愶紙杞婚噺锛屾瘡 tick 2 涓級
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(SEED_TRAIL, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level().isClientSide) return;
        if (this.isRemoved()) return;

        ServerPlayer caster = (this.getOwner() instanceof ServerPlayer sp) ? sp : null;

        if (hitResult.getType() == HitResult.Type.ENTITY
                && hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity host
                && host.isAlive() && caster != null) {
            // 鍛戒腑鐢熺墿锛氬湪瀹夸富韬笂绉嶄笅鐏垫灉绉嶅瓙锛堝弻鐢熺鑽锋椂鎵╂暎棰濆 1 浜?鏃犱汉鍙?2 灞傦級
            ParasiticFruitSeedPower.plantSeedSpread(caster, host, twinPod);
        } else if (hitResult.getType() == HitResult.Type.BLOCK
                && this.level() instanceof ServerLevel sw && caster != null) {
            // 钀藉湴锛氱敓鎴愮伒鏋滅瀛愬湀锛堢豢鑹叉不鐤楃幆 + 鏍稿績鍥炬爣锛岃繘鍦堢敓鐗╄Е鍙戯紱鍙岀敓绉嶈嵎鏃舵牳蹇冧负鍙岀敓绉嶈嵎锛?
            github.com.gengyoubo.sscfa.ability.ParasiticSeedFieldManager
                    .spawnField(caster, sw, hitResult.getLocation().add(0, 0.05, 0), BAL.i("default_field_life", DEFAULT_FIELD_LIFE), twinPod);
        }
        this.discard();
    }

    /** 閫氱敤纰版挒棰勮繃婊わ細蹇界暐鍙戝皠鑰呮湰浜恒€?*/
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner()
                && !entity.isSpectator() && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand);
    }
}
