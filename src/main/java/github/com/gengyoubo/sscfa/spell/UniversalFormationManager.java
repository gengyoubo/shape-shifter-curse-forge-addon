package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.event.FormChangedEvent;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting;
import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBarDef;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * 通用增强法阵的「形态能量 → 书法术值」转化，服务端每秒结算一次。
 *
 * <p>魔法书内装有<b>回能变体</b>通用法阵（regen）时生效：当前形态持有能量条且负荷高于阈值，
 * 每秒消耗形态能量以降低崩解负荷。阈值取书内等级最高的回能法阵（多张不叠加）。</p>
 *
 * <p>本秒发生转化的玩家标记为「充能中」，直到下一次结算。充能期间：附属形态的能量回复 power 通过
 * {@code ssc_addon:not_universal_formation_charging} 条件暂停；SSC 法力池的回复通过取消
 * {@code SscManaRecoveryEvent} 暂停（扣除、直接设值与补满不受影响）。</p>
 */
public final class UniversalFormationManager {
    private static final Set<UUID> CHARGING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static boolean registered;

    private UniversalFormationManager() {}

    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                clearCharging(player);
                tickPlayer(player);
            }
        });
        PlayerSessionCleanup.register((handler, server) -> clearCharging(handler.player));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            clearCharging(event.getOriginal());
            clearCharging(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((FormChangedEvent event) -> clearCharging(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CHARGING_PLAYERS.clear());
        // 充能期间暂停 SSC 法力池的一切回复（gain_mana 动作、月夜回复、蛛丝自然回复、储能体等）。
        MinecraftForge.EVENT_BUS.addListener((net.onixary.shapeShifterCurseForge.api.SscManaRecoveryEvent event) -> {
            if (isCharging(event.getEntity())) event.setCanceled(true);
        });
    }

    public static boolean isCharging(Player player) {
        return player != null && CHARGING_PLAYERS.contains(player.getUUID());
    }

    private static void clearCharging(Player player) {
        if (player != null) CHARGING_PLAYERS.remove(player.getUUID());
    }

    private static void tickPlayer(ServerPlayer player) {
        // 施法期间不转化：起手已一次性结清，读条中回能会吃掉净消耗。
        if (!player.isAlive() || player.isSpectator() || NeoLegacyCasting.isCasting(player)) return;
        ItemStack book = TrinketUtils.findFirstEquipped(player,
                stack -> stack.getItem() == AddonItems.MOON_DUST_SPELLBOOK.get());
        if (book == null || book.isEmpty()) return;
        int bestLevel = FormationData.getBestUniversalVariantLevel(book, FormationData.VARIANT_REGEN);
        if (bestLevel <= 0) return;
        double collapse = NeoRuntime.data(player).burden;
        double maxMana = NeoRuntime.profile(player).safety();
        double threshold = FormationData.universalThreshold(bestLevel);
        if (maxMana <= 0 || collapse <= maxMana * (1 - threshold)) return;
        ResourceBarDef bar = findEnergyBar(player);
        if (bar == null) return;
        int drain = (int) FormationData.universalManaDrainPerSec();
        int restore = (int) FormationData.universalBookManaPerSec();
        if (!ResourceBars.consume(player, bar, drain)) return;
        NeoLegacyCasting.restore(player, restore);
        CHARGING_PLAYERS.add(player.getUUID());
    }

    /** 与 {@code UniversalEnergyPotionItem.canRestore} 同源：先查附属资源条（种子条除外），再以 SSC mana 兜底。 */
    private static ResourceBarDef findEnergyBar(ServerPlayer player) {
        // 朔望无能量体系；果蝠的种子量不是 mana。
        if (FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA)
                || FormUtils.isForm(player, FormIdentifiers.BAT_PARASITIC_FRUIT)) return null;
        for (ResourceBarDef bar : BarKeys.ALL) {
            if (bar == BarKeys.SEED) continue;
            if (ResourceBars.has(player, bar)) return bar;
        }
        return ResourceBars.has(player, BarKeys.VANILLA_MANA) ? BarKeys.VANILLA_MANA : null;
    }
}
