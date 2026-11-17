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
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_MOON_TETHER = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.moon_tether");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_MANCIANIMA_EXTRA = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.mancianima_extra");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_EFFECT_REDUCTION = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.effect_reduction");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_ANUBIS_WITHER_EXTRA = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.anubis_wither_extra");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_ALLAY_SP_EXTRA = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_extra");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_NIGHTMARE_FEAR_EXTRA = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.nightmare_fear_extra");
    @Unique private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL_UPGRADE_FOX_NODES = new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.upgrade_fox_nodes");
    @Unique private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> ssca$TETHER_SACRIFICE_KEY =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    new net.minecraft.resources.ResourceLocation("my_addon", "tether_sacrifice"));
    /** Jumping spider eating a fluid cocoon: its own poison passes the form's poison immunity. Server thread only. */
    @Unique private static final ThreadLocal<Boolean> ssca$salticidaeCocoonBypass = ThreadLocal.withInitial(() -> Boolean.FALSE);
    @Unique private static final net.minecraft.resources.ResourceLocation ssca$SPIDER_FLUID_COCOON =
            new net.minecraft.resources.ResourceLocation("shape-shifter-curse", "spider_fluid_cocoon");

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

    /** Level-3 portable moisturizer: every hit its wearer (an axolotl-line player) deals is +15%. */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$moisturizerLevel3DamageBoost(float amount, DamageSource source) {
        if (amount <= 0.0F) return amount;
        if (!(source.getEntity() instanceof net.minecraft.world.entity.player.Player attacker)) return amount;
        if (!github.com.gengyoubo.sscfa.util.FormUtils.isMoistureDependent(attacker)) return amount;
        if (!github.com.gengyoubo.sscfa.item.PortableMoisturizerItem.isLevel3Equipped(attacker)) return amount;
        return amount * 1.15F;
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
                && FormUtils.isForm(sp, FormIdentifiers.SPIDER_SALTICIDAE)
                && !ssca$salticidaeCocoonBypass.get()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"))
    private void ssca$salticidaeCocoonEatHead(net.minecraft.world.level.Level world, net.minecraft.world.item.ItemStack stack,
                                              CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (!world.isClientSide && (Object) this instanceof ServerPlayer sp
                && FormUtils.isForm(sp, FormIdentifiers.SPIDER_SALTICIDAE)
                && ssca$SPIDER_FLUID_COCOON.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
            ssca$salticidaeCocoonBypass.set(Boolean.TRUE);
        }
    }

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void ssca$salticidaeCocoonEatReturn(net.minecraft.world.level.Level world, net.minecraft.world.item.ItemStack stack,
                                                CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (ssca$salticidaeCocoonBypass.get()) ssca$salticidaeCocoonBypass.set(Boolean.FALSE);
    }

    /**
     * Save-data snapshots read after load: power resources (restored once the login form re-attach has
     * reset them) and the sheep spell's original-form snapshot. The health snapshot lives in
     * LivingEntityLoginHealthMixin.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ssca$snapshotLoginResources(CompoundTag nbt, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            github.com.gengyoubo.sscfa.event.LoginResourceRestoreHandler.recordSnapshot(player.getUUID(), nbt);
            github.com.gengyoubo.sscfa.ability.BeepSheepFormManager.onReadNbt(player, nbt);
        }
    }

    /** Sheep spell: the original form, cooldown snapshot and start time survive logout and restarts. */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ssca$writeSheepFormSnapshot(CompoundTag nbt, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            github.com.gengyoubo.sscfa.ability.BeepSheepFormManager.onWriteNbt(player, nbt);
        }
    }

    /** Axolotl SP vortex charge: a charging player takes no part in entity pushing, so pulled mobs cannot shove them. */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$vortexChargingNoPush(Entity entity, CallbackInfo ci) {
        if (ssc_addon$isVortexChargingPlayer((LivingEntity) (Object) this) || ssc_addon$isVortexChargingPlayer(entity)) ci.cancel();
    }

    @Unique
    private static boolean ssc_addon$isVortexChargingPlayer(Entity e) {
        if (!(e instanceof Player)) return false;
        if (e instanceof ServerPlayer sp) return github.com.gengyoubo.sscfa.ability.VortexChargeManager.isCharging(sp);
        // Local client player: per-tick cached flag instead of reading resources on every collision.
        return e.level().isClientSide() && github.com.gengyoubo.sscfa.ability.VortexChargeManager.isClientLocalCharging();
    }

    /**
     * Moon weaver tether: the owner deals +25% (players) / +15% (NPCs) to a tethered enemy and takes
     * -25% / -40% from it; a tethered ally takes half and the owner shoulders the other half on the next
     * server task (deferred so the transfer never re-enters this damage call).
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc_addon$spiderTetherDamage(float amount, DamageSource source) {
        if (amount <= 0.0F || source == null) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return amount;
        Entity attacker = source.getEntity();
        ServerPlayer owner = github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.getTetheringPlayer(self);
        if (owner != null && owner != self) {
            if (github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(owner, self)) {
                float transfer = amount * (float) BAL_MOON_TETHER.d("ally_transfer_share", 0.5);
                if (transfer > 0.0F) {
                    final Entity killer = attacker;
                    owner.getServer().execute(() -> {
                        if (!owner.isAlive()) return;
                        var registry = owner.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
                        owner.hurt(new DamageSource(registry.getHolderOrThrow(ssca$TETHER_SACRIFICE_KEY), killer, killer), transfer);
                    });
                }
                return amount * (float) BAL_MOON_TETHER.d("ally_taken_share", 0.5);
            } else if (attacker == owner) {
                return amount * (self instanceof Player ? (float) BAL_MOON_TETHER.d("owner_vs_enemy_mul", 1.25)
                        : (float) NpcCombatEnhancements.value("moon_tether_dealt", 1.15));
            }
        }
        if (self instanceof ServerPlayer vp && attacker instanceof LivingEntity la
                && github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverSwingManager.isTethering(vp, la)
                && !github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(vp, la)) {
            return amount * (la instanceof Player ? (float) BAL_MOON_TETHER.d("enemy_vs_owner_mul", 0.75)
                    : (float) NpcCombatEnhancements.value("moon_tether_taken", 0.6));
        }
        return amount;
    }

    /**
     * Wither applied without a source (wolf bites and the like): attribute it to the single nearest
     * Netherjudge player in range, for soul energy.
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("RETURN"))
    private void ssc_addon$registerAnubisWitherSourceNoSource(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || effect.getEffect() != MobEffects.WITHER) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        ServerPlayer best = null;
        double radius = BAL_ANUBIS_WITHER_EXTRA.d("attrib_radius", 24.0);
        double bestDist = radius * radius;
        for (Player p : self.level().players()) {
            if (!(p instanceof ServerPlayer sp) || !FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP)) continue;
            double d = p.distanceToSqr(self);
            if (d < bestDist) {
                bestDist = d;
                best = sp;
            }
        }
        if (best != null) github.com.gengyoubo.sscfa.ability.AnubisWolfSpSoulEnergy.registerWitherSource(self, best, effect.getDuration());
    }

    /**
     * Mancianima resistance charges and i-frames: while i-frames run, hits from living attackers are
     * cancelled; otherwise a resistance charge cancels the hit (and its knockback) and starts 4 ticks of
     * i-frames. Any damage resets the 15 s recharge timer. Also the evolved familiar fox's combat marker.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$mancianimaResistance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (source.getEntity() instanceof ServerPlayer atkFox && FormUtils.isForm(atkFox, FormIdentifiers.UPGRADE_FAMILIAR_FOX)) {
            MancianimaMarkManager.markCombat(atkFox.getUUID(), atkFox.serverLevel().getGameTime());
        }
        if (self instanceof ServerPlayer defFox && source.getEntity() != null && source.getEntity() != defFox
                && FormUtils.isForm(defFox, FormIdentifiers.UPGRADE_FAMILIAR_FOX)) {
            MancianimaMarkManager.markCombat(defFox.getUUID(), defFox.serverLevel().getGameTime());
        }
        if (!(self instanceof ServerPlayer sp) || !FormUtils.isForm(sp, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) return;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) return;
        MancianimaMarkManager.markCombat(sp.getUUID(), sp.serverLevel().getGameTime());
        // Only damage from another living entity; environmental damage is not blocked.
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity) || attacker == sp) return;
        if (PowerUtils.getResourceValue(sp, FormIdentifiers.MANCIANIMA_IFRAMES) > 0) {
            cir.setReturnValue(false);
            return;
        }
        int resist = PowerUtils.getResourceValue(sp, FormIdentifiers.MANCIANIMA_RESISTANCE);
        if (resist > 0) {
            PowerUtils.setResourceValueAndSync(sp, FormIdentifiers.MANCIANIMA_RESISTANCE, resist - BAL_MANCIANIMA_EXTRA.i("resist_cost", 1));
            PowerUtils.setResourceValueAndSync(sp, FormIdentifiers.MANCIANIMA_IFRAMES, BAL_MANCIANIMA_EXTRA.i("iframes_ticks", 4));
            sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.6f);
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
        if (self.level().isClientSide) return;
        // Frostspine counter-thorns: a melee hit on the frostspine stacks thorns on the attacker.
        if (self instanceof ServerPlayer frostspine && FormUtils.isForm(frostspine, FormIdentifiers.SNOW_FOX_FROSTSPINE)) {
            github.com.gengyoubo.sscfa.ability.FrostArmorManager.onFrostspineMeleeHit(frostspine, source);
        }
        // Netherjudge wither infection: the player's (or their wolf's) hit hands wither time to the target.
        ServerPlayer witherOwner = ssc_addon$anubisAttacker(self, source);
        if (witherOwner != null) WitherFrenzyManager.tryWitherInfect(witherOwner, self);
    }

    /** The Netherjudge player behind this hit: the player themself, or the owner of the wolf that bit. */
    @Unique
    private static ServerPlayer ssc_addon$anubisAttacker(LivingEntity self, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer sp) return FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP) ? sp : null;
        if (attacker instanceof net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity wolf
                && wolf.getOwnerUUID() != null && self.level() instanceof ServerLevel level
                && level.getPlayerByUUID(wolf.getOwnerUUID()) instanceof ServerPlayer owner
                && FormUtils.isForm(owner, FormIdentifiers.ANUBIS_WOLF_SP)) return owner;
        return null;
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
        Entity attacker = source.getEntity();

        // Allay SP: no single hit takes more than a quarter of max health.
        if (FormUtils.isAllaySP(self) && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
            amount = Math.min(amount, self.getMaxHealth() * (float) BAL_ALLAY_SP_EXTRA.d("damage_cap_ratio", 0.25));
        }
        // Frostspine thorn armour: melee damage reduced 4% per orbiting spike.
        if (self instanceof ServerPlayer frost && FormUtils.isForm(frost, FormIdentifiers.SNOW_FOX_FROSTSPINE)) {
            amount = github.com.gengyoubo.sscfa.ability.FrostArmorManager.applyArmor(frost, source, amount);
        }
        // Infection spores: an infected attacker deals 15% less.
        if (attacker instanceof LivingEntity living && InfectionSporeManager.isInfected(living.getUUID())) {
            amount = InfectionSporeManager.reduceDamageIfInfected(living, amount);
        }
        // Netherjudge wither frenzy: the player and their wolves hit harder the longer the player is withered.
        ServerPlayer frenzy = ssc_addon$anubisAttacker(self, source);
        if (frenzy != null) amount *= WitherFrenzyManager.getDamageMultiplier(frenzy);
        // Netherjudge wither resistance: wither ticks are weaker and some are skipped.
        if (self instanceof ServerPlayer sp && FormUtils.isForm(sp, FormIdentifiers.ANUBIS_WOLF_SP) && source.is(DamageTypes.WITHER)) {
            float scale = WitherFrenzyManager.getWitherDamageScale(sp);
            amount = scale <= 0f ? 0.0f : amount * scale;
        }
        // Nightmare fear: the first hit from a nightmare (or a whitelisted ally) on a feared target is doubled,
        // and every such hit reveals the nightmare to that target.
        if (attacker instanceof ServerPlayer nightmare && self.level() instanceof ServerLevel level
                && NightmareFearManager.isFeared(self.getUUID(), level.getGameTime())
                && NightmareFearManager.isDoubleDamageBeneficiary(nightmare, level)) {
            if (NightmareFearManager.tryConsumeDoubleDamage(self.getUUID(), level.getGameTime())) {
                amount *= (float) BAL_NIGHTMARE_FEAR_EXTRA.d("first_hit_mul", 2.0);
            }
            if (self instanceof ServerPlayer fearedPlayer) NightmareFearManager.onNightmareAttackFeared(fearedPlayer, nightmare);
        }
        // Mancianima red mark: the marker deals +25% to their red-marked victim, who deals -25% back.
        if (attacker instanceof ServerPlayer ap && MancianimaMarkManager.isRedMarkedBy(ap.getUUID(), self.getUUID())) {
            amount *= (float) BAL_MANCIANIMA_EXTRA.d("redmark_attacker_mul", 1.25);
        } else if (self instanceof ServerPlayer marker && attacker != null) {
            java.util.UUID markerOf = MancianimaMarkManager.getMarkerOf(attacker.getUUID());
            if (marker.getUUID().equals(markerOf)) {
                MancianimaMarkManager.Mark mark = MancianimaMarkManager.getMark(marker.getUUID());
                if (mark != null && mark.color == MancianimaMarkManager.MarkColor.RED) {
                    amount *= (float) BAL_MANCIANIMA_EXTRA.d("redmark_victim_mul", 0.75);
                }
            }
        }
        // (Vampire bat blood-thirst scaling runs from BatDesmodusEvents on LivingHurtEvent.)
        // Evolved familiar fox: magic and potion damage -25% per unlocked node (buff immunity, alchemy), added.
        if (self instanceof ServerPlayer fox && FormUtils.isForm(fox, FormIdentifiers.UPGRADE_FAMILIAR_FOX)
                && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))) {
            var evolution = github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.EVOLUTION.get(fox);
            if (evolution.isOnSscaRoute() && evolution.hasStarted()) {
                float reduction = 0f;
                double perNode = BAL_UPGRADE_FOX_NODES.d("potion_node_reduction", 0.25);
                if (evolution.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_BUFF_IMMUNITY)) reduction += (float) perNode;
                if (evolution.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_ALCHEMY)) reduction += (float) perNode;
                if (reduction > 0f) amount *= 1f - reduction;
            }
        }
        return amount;
    }

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance modifyStatusEffect(MobEffectInstance effect) {
        if (!effect.getEffect().isInstantenous()
                && github.com.gengyoubo.sscfa.compat.FamiliarFoxSpPerks.preservesEffectDuration((LivingEntity) (Object) this, effect.getEffect())) {
            return github.com.gengyoubo.sscfa.compat.FamiliarFoxSpPerks.levelOneWithOriginalDuration(effect);
        }
        if (!effect.getEffect().isInstantenous()
                && EffectEfficiencyReductionPower.isActive((LivingEntity) (Object) this)) {
            int originalAmp = effect.getAmplifier();
            int newDuration;
            if (originalAmp == 0) newDuration = (int) (effect.getDuration() * BAL_EFFECT_REDUCTION.d("dur_l1", 0.4));
            else if (originalAmp == 1) newDuration = (int) (effect.getDuration() * BAL_EFFECT_REDUCTION.d("dur_l2", 0.6));
            else if (originalAmp == 2) newDuration = (int) (effect.getDuration() * BAL_EFFECT_REDUCTION.d("dur_l3", 0.8));
            else newDuration = effect.getDuration();
            return new MobEffectInstance(effect.getEffect(), newDuration, 0, effect.isAmbient(), effect.isVisible(), effect.showIcon());
        }
        return effect;
    }
}
