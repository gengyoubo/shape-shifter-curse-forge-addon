package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyHits;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.visual.ParametricVisuals;
import github.com.gengyoubo.sscfa.spell.SpellDamageSource;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 月灵光弹：带曲线追踪、变体伤害与元素减益的投射物。 */
public class LunarSpiritBoltEntity extends Projectile implements SpellAnomalyHits.ProjectileData {
    private static final double SPEED = 0.7;
    private static final double TURN_RATE = 0.14;
    private static final int MAX_TICKS = 80;
    private static final BalanceReader BAL = new BalanceReader("abilities.lunar_spirit_bolt");

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(LunarSpiritBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(LunarSpiritBoltEntity.class, EntityDataSerializers.INT);

    private int ticksAlive;
    private SpellAnomalyContext anomaly;

    public LunarSpiritBoltEntity(EntityType<? extends LunarSpiritBoltEntity> type, Level level) {
        super(type, level);
    }

    public LunarSpiritBoltEntity(Level level, LivingEntity owner) {
        super(SscAddon.LUNAR_SPIRIT_BOLT_ENTITY, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(VARIANT, 2);
        entityData.define(TARGET_ID, 0);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, Math.max(0, Math.min(2, variant)));
    }

    public void setTargetEntity(LivingEntity target) {
        entityData.set(TARGET_ID, target == null ? 0 : target.getId());
    }

    private LivingEntity resolveTarget() {
        int id = entityData.get(TARGET_ID);
        if (id == 0) return null;
        Entity entity = level().getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        Vec3 velocity = getDeltaMovement();
        LivingEntity target = resolveTarget();
        if (target != null) {
            double speed = BAL.d("speed", SPEED);
            double turnRate = BAL.d("turn_rate", TURN_RATE);
            Vec3 desired = target.position().add(0, target.getBbHeight() * 0.6, 0)
                    .subtract(position()).normalize();
            Vec3 current = velocity.normalize();
            velocity = current.scale(1.0 - turnRate).add(desired.scale(turnRate)).normalize().scale(speed);
            setDeltaMovement(velocity);
        }
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (isRemoved()) return;
        if (!level().isClientSide && ticksAlive > BAL.i("max_ticks", MAX_TICKS)) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel server) {
            ParametricVisuals.touch(this, getUUID(), ParametricVisuals.Kind.LUNAR_BOLT_TRAIL, null,
                    position(), true, server.getGameTime() - tickCount, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity living) || level().isClientSide) return;
        ServerPlayer ownerPlayer = resolveOwnerPlayer();
        if (ownerPlayer != null && WhitelistUtils.isProtected(ownerPlayer, living)) {
            discard();
            return;
        }
        float damage = 3.0f;
        int variant = getVariant();
        if (variant == 0) damage *= 1.5f;
        else if (variant == 1) damage *= 0.7f;
        boolean damaged = ownerPlayer != null
                ? living.hurt(SpellDamageSource.of(damageSources(), this, ownerPlayer), damage)
                : living.hurt(SpellDamageSource.of(damageSources()), damage);
        if (variant == 0) living.setSecondsOnFire(3);
        else if (variant == 1) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        else living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.6f);
        if (damaged) SpellAnomalyHits.hit(this, living);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (result.getType() == HitResult.Type.BLOCK && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.04);
        }
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner() || entity instanceof LunarSpiritEntity || entity instanceof ArmorStand) return false;
        if (!(entity instanceof LivingEntity living) || !super.canHitEntity(entity)) return false;
        ServerPlayer owner = resolveOwnerPlayer();
        return owner == null || !WhitelistUtils.isProtected(owner, living);
    }

    private ServerPlayer resolveOwnerPlayer() {
        if (anomaly != null && level() instanceof ServerLevel server) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(anomaly.casterUUID());
            if (player != null) return player;
        }
        if (getOwner() instanceof ServerPlayer player) return player;
        if (getOwner() instanceof LunarSpiritEntity spirit && spirit.getOwnerUuid() != null
                && level() instanceof ServerLevel server) {
            return server.getServer().getPlayerList().getPlayer(spirit.getOwnerUuid());
        }
        return null;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("BoltTicks", ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) setVariant(tag.getInt("Variant"));
        ticksAlive = tag.getInt("BoltTicks");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public SpellAnomalyContext ssca$anomaly() {
        return anomaly;
    }

    @Override
    public void ssca$anomaly(SpellAnomalyContext context) {
        anomaly = context;
    }
}
