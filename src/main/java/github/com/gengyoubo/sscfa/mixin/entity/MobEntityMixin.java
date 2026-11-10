package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.MancianimaAggroTracker;
import github.com.gengyoubo.sscfa.ability.NightmareFearManager;
import github.com.gengyoubo.sscfa.entity.WitchFamiliarEntity;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.UndeadNeutralState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 鐗堬細Mob 鐩爣閫夋嫨鐨勪腑绔?鎭愭儳/闅愯韩绛夎鍒欙紙渚濇嵁涓婃父 Fabric 婧愮爜閲嶅啓涓?Mojang 鏄犲皠锛夈€?*/
@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Unique
    private long ssc_addon$lastSawProvokedTarget = -1;

    @Unique
    private boolean ssc_addon$isNeutralMobPair(Mob mob, Player player) {
        if (FormUtils.isForm(player, FormIdentifiers.ANUBIS_WOLF_SP)) {
            return mob.getMobType() == MobType.UNDEAD;
        }
        if (FormUtils.isForm(player, FormIdentifiers.GOLDEN_SANDSTORM_SP)) {
            return mob instanceof Husk || FormUtils.isTransformativeWolf(mob);
        }
        return false;
    }

    @Unique
    private boolean ssc_addon$shouldUndeadIgnore(Mob mob, Player player) {
        if (!ssc_addon$isNeutralMobPair(mob, player)) return false;
        return !UndeadNeutralState.isPlayerProvoked(player.getUUID(), mob.level().getGameTime());
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target == null) {
            LivingEntity prev = self.getTarget();
            if (prev instanceof Player pp && FormUtils.isForm(pp, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) {
                MancianimaAggroTracker.forget(self.getUUID());
            }
        }
        if (target != null) {
            if (self instanceof IronGolem golem
                    && target instanceof Player player
                    && FormUtils.isAllaySP(player)) {
                golem.setTarget(null);
                ci.cancel();
                return;
            }
            if (target.hasEffect(SscAddon.PLAYING_DEAD)) {
                ci.cancel();
                return;
            }
            if (target.hasEffect(SscAddon.TRUE_INVISIBILITY)) {
                ci.cancel();
                return;
            }
            if (self instanceof Phantom
                    && target instanceof Player phantomTarget
                    && FormUtils.isForm(phantomTarget, FormIdentifiers.WILD_CAT_NIGHTMARE)) {
                ci.cancel();
                return;
            }
            if (!NightmareFearManager.isMobAggroAllowed(self, target)) {
                ci.cancel();
                return;
            }
            if ((self instanceof Raider || self instanceof Vex || self instanceof Witch)
                    && target instanceof WitchFamiliarEntity) {
                ci.cancel();
                return;
            }
            if (self instanceof WitchFamiliarEntity
                    && (target instanceof Raider || target instanceof Vex || target instanceof Witch || target instanceof WitchFamiliarEntity)) {
                ci.cancel();
                return;
            }
            if ((self instanceof Raider || self instanceof Vex)
                    && target instanceof Player player
                    && player.getTags().contains("ssc_raid_friend")) {
                ci.cancel();
                return;
            }
            if (target instanceof Player player
                    && ssc_addon$shouldUndeadIgnore(self, player)) {
                ci.cancel();
                return;
            }
            if (target instanceof Player player
                    && FormUtils.isForm(player, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)
                    && !(self instanceof Warden)
                    && !(self instanceof IronGolem)) {
                if (!MancianimaAggroTracker.isAngered(self.getUUID(), player.getUUID())) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void ssc_addon$onMobTick(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!mob.level().isClientSide) {
            NightmareFearManager.tickFearedMobAggro(mob);
        }

        if (mob.hasEffect(SscAddon.STUN)) {
            ci.cancel();
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null) {
            ssc_addon$lastSawProvokedTarget = -1;
            return;
        }

        if (target.hasEffect(SscAddon.TRUE_INVISIBILITY)) {
            mob.setTarget(null);
            return;
        }

        if ((mob instanceof Raider || mob instanceof Vex)
                && target instanceof Player player
                && player.getTags().contains("ssc_raid_friend")) {
            mob.setTarget(null);
            return;
        }

        if (target instanceof Player player
                && ssc_addon$isNeutralMobPair(mob, player)) {
            if (!UndeadNeutralState.isPlayerProvoked(player.getUUID(), mob.level().getGameTime())) {
                mob.setTarget(null);
                ssc_addon$lastSawProvokedTarget = -1;
                return;
            }
            long worldTime = mob.level().getGameTime();
            if (mob.tickCount % 5 == 0 ? mob.hasLineOfSight(target) : ssc_addon$lastSawProvokedTarget >= 0) {
                ssc_addon$lastSawProvokedTarget = worldTime;
                UndeadNeutralState.PROVOKE_TIMESTAMPS.put(player.getUUID(), worldTime);
            } else {
                if (ssc_addon$lastSawProvokedTarget < 0) {
                    ssc_addon$lastSawProvokedTarget = worldTime;
                }
                if (worldTime - ssc_addon$lastSawProvokedTarget > UndeadNeutralState.SIGHT_TIMEOUT) {
                    mob.setTarget(null);
                    ssc_addon$lastSawProvokedTarget = -1;
                }
            }
        }
    }
}
