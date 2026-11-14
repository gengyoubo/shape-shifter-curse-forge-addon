package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.AllaySPRangedHitPassive;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormRegen;
import github.com.gengyoubo.sscfa.ability.InfectionSporeManager;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.ability.NightmareDreamManager;
import github.com.gengyoubo.sscfa.ability.NightmareFearManager;
import github.com.gengyoubo.sscfa.ability.NineLivesManager;
import github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements;
import github.com.gengyoubo.sscfa.ability.NpcCombatState;
import github.com.gengyoubo.sscfa.ability.NpcCombatStateAccess;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpMeleeAbility;
import github.com.gengyoubo.sscfa.ability.SnowFoxSpTeleportAttack;
import github.com.gengyoubo.sscfa.ability.WindSpiritClawManager;
import github.com.gengyoubo.sscfa.ability.WitherFrenzyManager;
import github.com.gengyoubo.sscfa.anomaly.AnomalyEffects;
import github.com.gengyoubo.sscfa.effect.FrostFreezeEffect;
import github.com.gengyoubo.sscfa.item.BindingAnkletItem;
import github.com.gengyoubo.sscfa.power.EffectEfficiencyReductionPower;
import github.com.gengyoubo.sscfa.power.ParasiticFruitSeedPower;
import github.com.gengyoubo.sscfa.spell.DomainManager;
import github.com.gengyoubo.sscfa.spell.FrostArmorState;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.UndeadNeutralState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge 閻楀牞绱欐笟婵囧祦娑撳﹥鐖跺┃鎰垳闁插秴鍟撻敍澶涚窗LivingEntity 閻ㄥ嫪婵€鐎?閺佸牊鐏?閸ョ偠顢呯紒鐓庢値缂佹挾鐣婚妴?*/
@Mixin(LivingEntity.class)
public abstract class SscAddonLivingEntityMixin implements NpcCombatStateAccess {
    @Unique
    private static final net.minecraft.world.entity.EquipmentSlot[] ssca$armorSlots = {
            net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
    @Unique
    private NpcCombatState ssca$npcCombatState;

    @Override
    public NpcCombatState ssca$getNpcCombatState() {
        if (ssca$npcCombatState == null) ssca$npcCombatState = new NpcCombatState();
        return ssca$npcCombatState;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$saveNpcCombat(CompoundTag nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) && ssca$npcCombatState != null) {
            CompoundTag state = ssca$npcCombatState.write(self.level().getGameTime());
            if (!state.getCompound("Fallen").isEmpty() || !state.getCompound("WildCat").isEmpty()) nbt.put("SSCA.NpcCombat", state);
            else nbt.remove("SSCA.NpcCombat");
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$loadNpcCombat(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("SSCA.NpcCombat", Tag.TAG_COMPOUND)) ssca$getNpcCombatState().read(nbt.getCompound("SSCA.NpcCombat"));
        else ssca$npcCombatState = null;
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssca$npcCombatDamage(float amount, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;
        return AnomalyEffects.modifyDamage(target, source, NpcCombatEnhancements.modifyDamage(target, source, amount));
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void ssca$clearNpcCombatEffect(MobEffectInstance effect, CallbackInfo ci) {
        if (effect.getEffect() == SscAddon.FROST_ARMOR) {
            FrostArmorState.removeFrom((LivingEntity) (Object) this);
        }
        if (ssca$npcCombatState == null) return;
        if (effect.getEffect() == MobEffects.GLOWING) ssca$npcCombatState.clearFallen();
        if (effect.getEffect() == SscAddon.STUN) ssca$npcCombatState.clearWildCat();
    }

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MobEffectInstance ssca$npcFruitDebuff(MobEffectInstance effect, MobEffectInstance original, Entity source) {
        return NpcCombatEnhancements.enhanceFruitDebuff((LivingEntity) (Object) this, effect, source);
    }

    @Inject(method = "isImmobile", at = @At("RETURN"), cancellable = true)
    private void ssca$stunImmobile(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (((LivingEntity) (Object) this).hasEffect(SscAddon.STUN)
                || github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked((LivingEntity) (Object) this))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ssca$spellCastingBlockJump(CallbackInfo ci) {
        if (SpellChannelManager.isImmobile((LivingEntity) (Object) this)
                || github.com.gengyoubo.sscfa.anomaly.FrozenControl.blocked((LivingEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void ssca$salticidaePoisonImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect() == MobEffects.POISON
                && (Object) this instanceof ServerPlayer sp
                && FormUtils.isForm(sp, FormIdentifiers.SPIDER_SALTICIDAE)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float ssc_addon$reduceHealWhenParasitized(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || amount <= 0.0f) return amount;
        if (ParasiticFruitSeedPower.isParasitizedByEnemyFruit(self.getUUID(), self.level().getGameTime())) {
            return amount * 0.5f;
        }
        return amount;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ssca$domainRejectDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (DomainManager.blocksDamage((LivingEntity) (Object) this, source)) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssca$domainDamage(float amount, DamageSource source) {
        return DomainManager.modifyDamage((LivingEntity) (Object) this, source, amount);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void ssca$domainRejectEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (DomainManager.blocksEffect((LivingEntity) (Object) this, source)) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$bindingAnkletBoost(float amount, DamageSource source) {
        if (amount <= 0.0F) return amount;
        Entity raw = source.getEntity();
        if (!(raw instanceof LivingEntity attacker)) return amount;
        if (!BindingAnkletItem.isRaiderFaction(attacker)) return amount;
        if (!BindingAnkletItem.hasAnkletAuraNearby(attacker)) return amount;
        return amount * BindingAnkletItem.DAMAGE_MULTIPLIER;
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssca$spellResistance(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || amount <= 0 || source == null
                || (!source.is(github.com.gengyoubo.sscfa.spell.SpellDamageSource.SPELL_DAMAGE)
                && !AnomalyEffects.isReaction(source))) return amount;
        int level = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : ssca$armorSlots) {
            level += net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    github.com.gengyoubo.sscfa.enchantment.RegAddonEnchantments.SPELL_RESISTANCE.get(), self.getItemBySlot(slot));
        }
        level = Math.min(level, github.com.gengyoubo.sscfa.enchantment.SpellResistanceEnchantment.MAX_LEVEL);
        return amount * (1.0f - github.com.gengyoubo.sscfa.enchantment.SpellResistanceEnchantment.REDUCTION_PER_LEVEL * level);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$curseMarkDamageTaken(float amount, DamageSource source) {
        if (amount <= 0.0F || source == null) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;
        MobEffectInstance mark = self.getEffect(SscAddon.CURSE_MARK);
        if (mark == null) return amount;
        float multiplier = github.com.gengyoubo.sscfa.effect.CurseMarkEffect.takenBase() + 1.0f
                + github.com.gengyoubo.sscfa.effect.CurseMarkEffect.takenPerLevel() * mark.getAmplifier();
        return amount * multiplier;
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$curseMarkWeakenOutput(float amount, DamageSource source) {
        if (amount <= 0.0F || source == null) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return amount;
        if (attacker == self) return amount;
        MobEffectInstance mark = attacker.getEffect(SscAddon.CURSE_MARK);
        if (mark == null) return amount;
        float weaken = github.com.gengyoubo.sscfa.effect.CurseMarkEffect.outputWeakenBase()
                + github.com.gengyoubo.sscfa.effect.CurseMarkEffect.outputWeakenPerLevel() * mark.getAmplifier();
        return amount * (1.0f - weaken);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$modifyDamageForFrostEffects(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        float modifiedAmount = amount;
        if (self instanceof ServerPlayer serverPlayer) {
            float reduction = SnowFoxSpTeleportAttack.getDamageReduction(serverPlayer);
            if (reduction > 0) modifiedAmount = modifiedAmount * (1.0f - reduction);
        }
        if (self instanceof ServerPlayer dashingPlayer) {
            float dashReduction = SnowFoxSpMeleeAbility.getDashDamageReduction(dashingPlayer);
            if (dashReduction > 0) modifiedAmount = modifiedAmount * (1.0f - dashReduction);
        }
        MobEffectInstance frostFreezeEffect = self.getEffect(SscAddon.FROST_FREEZE);
        if (frostFreezeEffect != null && FrostFreezeEffect.isPhysicalOrMagicDamage(source)) {
            modifiedAmount = modifiedAmount * 1.35f;
        }
        return modifiedAmount;
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$scaleWindSpiritMelee(float value, DamageSource source, float amount) {
        if (source != null
                && source.getEntity() instanceof ServerPlayer p
                && source.is(DamageTypes.PLAYER_ATTACK)
                && FormUtils.isOcelotSP(p)
                && !WindSpiritClawManager.isHoldingWeapon(p)) {
            float mult = WindSpiritClawManager.getNormalMeleeMultiplier(p, (LivingEntity) (Object) this);
            if (mult != 1.0f) return value * mult;
        }
        return value;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$onUndeadDamaged(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) {
            if (self instanceof ServerPlayer nova && FormUtils.isForm(nova, FormIdentifiers.OCELOT_NOVA)) {
                NineLivesManager.markCombat(nova);
                if (!"outOfWorld".equals(source.getMsgId()) && amount >= nova.getHealth() + nova.getAbsorptionAmount()) {
                    if (NineLivesManager.tryRevive(nova)) {
                        Entity kbSource = source.getDirectEntity();
                        if (kbSource != null) {
                            nova.knockback(0.4, kbSource.getX() - nova.getX(), kbSource.getZ() - nova.getZ());
                            nova.hurtMarked = true;
                        }
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
            if (source.getEntity() instanceof ServerPlayer attacker && FormUtils.isForm(attacker, FormIdentifiers.OCELOT_NOVA)) {
                NineLivesManager.markCombat(attacker);
            }
        }
        if (self instanceof Mob mob && source.getEntity() instanceof Player player) {
            if (mob.getMobType() == MobType.UNDEAD && FormUtils.isForm(player, FormIdentifiers.ANUBIS_WOLF_SP)) {
                UndeadNeutralState.PROVOKE_TIMESTAMPS.put(player.getUUID(), mob.level().getGameTime());
            }
            if ((mob instanceof Husk || FormUtils.isTransformativeWolf(mob))
                    && FormUtils.isForm(player, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
                UndeadNeutralState.PROVOKE_TIMESTAMPS.put(player.getUUID(), mob.level().getGameTime());
            }
        }
        if (!self.level().isClientSide) {
            if (source.getEntity() instanceof ServerPlayer attacker
                    && FormUtils.isForm(attacker, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
                GoldenSandstormRegen.markCombat(attacker);
            }
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void ssc_addon$healAfterSuccessfulWither(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && Boolean.TRUE.equals(cir.getReturnValue()) && source.is(DamageTypes.WITHER)) {
            GoldenSandstormRegen.onWitherTickDamage(self);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void ssc_addon$registerGoldenSandstormWitherSource(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (effect.getEffect() != MobEffects.WITHER) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (source instanceof ServerPlayer sp) {
            if (FormUtils.isForm(sp, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
                GoldenSandstormRegen.registerWitherSource(self, sp, effect.getDuration());
            }
            if (FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP)) {
                github.com.gengyoubo.sscfa.ability.AnubisWolfSpSoulEnergy.registerWitherSource(self, sp, effect.getDuration());
            }
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$blockDreamingDebuff(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        MobEffect type = effect.getEffect();
        boolean harmful = type.getCategory() == MobEffectCategory.HARMFUL;
        boolean glowing = type == MobEffects.GLOWING;
        if (!harmful && !glowing) return;
        if (!(source instanceof LivingEntity livingSource)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (NightmareDreamManager.isBlocked(livingSource, self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void ssc_addon$onAllayRangedHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (amount > 0 && self instanceof ServerPlayer player) {
            SpellChannelManager.onDamaged(player);
        }
        AllaySPRangedHitPassive.onDamageApplied(self, source);
        if (!self.level().isClientSide
                && source.getEntity() instanceof ServerPlayer nightmareAttacker
                && NightmareDreamManager.isNightmare(nightmareAttacker)) {
            NightmareDreamManager.onNightmareDealtDamage(nightmareAttacker, self, amount);
        }
    }

    /**
     * 进入 actuallyHurt 前按顺序修正伤害：悦灵 SP 单次伤害封顶 → 被孢子感染的攻击者减伤 →
     * 阿努比斯狼凋零狂热增伤 → 阿努比斯狼凋零抗性。Forge 上按参数组修改的注入器不可用（合成的参数类加载不到），
     * 所以合并成一个 @ModifyArg。
     */
    @ModifyArg(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"), index = 1)
    private float ssc_addon$modifyIncomingDamage(DamageSource source, float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;

        if (FormUtils.isAllaySP(self) && !"outOfWorld".equals(source.getMsgId()) && !"genericKill".equals(source.getMsgId())) {
            amount = Math.min(amount, self.getMaxHealth() * 0.25f);
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && InfectionSporeManager.isInfected(living.getUUID())) {
            amount = InfectionSporeManager.reduceDamageIfInfected(living, amount);
        }

        if (attacker instanceof ServerPlayer sp && FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP)) {
            amount *= WitherFrenzyManager.getDamageMultiplier(sp);
        }

        if (self instanceof ServerPlayer sp && FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP) && source.is(DamageTypes.WITHER)) {
            float scale = WitherFrenzyManager.getWitherDamageScale(sp);
            amount = scale <= 0f ? 0.0f : amount * scale;
        }
        return amount;
    }

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance modifyStatusEffect(MobEffectInstance effect) {
        if (!effect.getEffect().isInstantenous()
                && EffectEfficiencyReductionPower.isActive((LivingEntity) (Object) this)) {
            int originalAmp = effect.getAmplifier();
            int newDuration;
            if (originalAmp == 0) newDuration = (int) (effect.getDuration() * 0.4);
            else if (originalAmp == 1) newDuration = (int) (effect.getDuration() * 0.6);
            else if (originalAmp == 2) newDuration = (int) (effect.getDuration() * 0.8);
            else newDuration = effect.getDuration();
            return new MobEffectInstance(effect.getEffect(), newDuration, 0, effect.isAmbient(), effect.isVisible(), effect.showIcon());
        }
        return effect;
    }
}
