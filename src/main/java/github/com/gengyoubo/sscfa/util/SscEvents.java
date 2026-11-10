package github.com.gengyoubo.sscfa.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.function.Consumer;

/** Forge 事件适配层：???Fabric 风格回调注册服务端事???*/
public final class SscEvents {
    private SscEvents() {}

    public static void onServerTick(Consumer<MinecraftServer> cb) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) cb.accept(e.getServer());
        });
    }

    public static void onWorldTick(Consumer<ServerLevel> cb) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel sl) cb.accept(sl);
        });
    }

    public static void onServerStarting(Consumer<MinecraftServer> cb) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> cb.accept(e.getServer()));
    }

    public static void onServerStarted(Consumer<MinecraftServer> cb) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> cb.accept(e.getServer()));
    }

    public static void onServerStopping(Consumer<MinecraftServer> cb) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> cb.accept(e.getServer()));
    }

    public static void onServerStopped(Consumer<MinecraftServer> cb) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> cb.accept(e.getServer()));
    }

    @FunctionalInterface
    public interface DamagePredicate {
        boolean allow(LivingEntity entity, DamageSource source, float amount);
    }

    public static void allowDamage(DamagePredicate cb) {
        MinecraftForge.EVENT_BUS.addListener((LivingAttackEvent e) -> {
            if (!cb.allow(e.getEntity(), e.getSource(), e.getAmount())) e.setCanceled(true);
        });
    }

    @FunctionalInterface
    public interface RespawnConsumer {
        void accept(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
    }

    public static void afterRespawn(RespawnConsumer cb) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) cb.accept(sp, sp, !e.isEndConquered());
        });
    }

    @FunctionalInterface
    public interface EntityLoadConsumer {
        void accept(Entity entity, Level world);
    }

    public static void onEntityLoad(EntityLoadConsumer cb) {
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> cb.accept(e.getEntity(), e.getLevel()));
    }

    @FunctionalInterface
    public interface ChunkLoadConsumer {
        void accept(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk);
    }

    public static void onChunkLoad(ChunkLoadConsumer cb) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.level.ChunkEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel sl && e.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk lc) {
                cb.accept(sl, lc);
            }
        });
    }

    @FunctionalInterface
    public interface UseBlockCallback {
        InteractionResult use(Player player, Level world, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult);
    }

    public static void useBlock(UseBlockCallback cb) {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            InteractionResult r = cb.use(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
            if (r != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(r);
            }
        });
    }

    @FunctionalInterface
    public interface EntityUnloadConsumer {
        void accept(Entity entity, Level world);
    }

    public static void onEntityUnload(EntityUnloadConsumer cb) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.EntityLeaveLevelEvent e) -> cb.accept(e.getEntity(), e.getLevel()));
    }

    public static void allowResettingTime(java.util.function.Predicate<Player> cb) {
        // no-op: sleeping time reset is not gated in the Forge port
    }

    @FunctionalInterface
    public interface DeathConsumer {
        void accept(LivingEntity entity, DamageSource source);
    }

    public static void onLivingDeath(DeathConsumer cb) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.living.LivingDeathEvent e) -> cb.accept(e.getEntity(), e.getSource()));
    }

    public static void onPlayerJoin(Consumer<ServerPlayer> cb) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) cb.accept(sp);
        });
    }

    @FunctionalInterface
    public interface UseItemCallback {
        net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Player player, Level world, InteractionHand hand);
    }

    public static void useItem(UseItemCallback cb) {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> r = cb.use(e.getEntity(), e.getLevel(), e.getHand());
            if (r.getResult() != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(r.getResult());
            }
        });
    }

    @FunctionalInterface
    public interface AttackCallback {
        InteractionResult attack(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult);
    }

    public static void attackEntity(AttackCallback cb) {
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            InteractionResult r = cb.attack(e.getEntity(), e.getEntity().level(), InteractionHand.MAIN_HAND, e.getTarget(), null);
            if (r != InteractionResult.PASS) e.setCanceled(true);
        });
    }
}
