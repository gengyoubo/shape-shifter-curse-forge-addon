package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import github.com.gengyoubo.sscfa.neomagic.NeoNetwork;
import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import java.util.*;

/** Wind movement and Nova's temporary post-nine-lives preparation shortcut. */
public final class OcelotSpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private record Window(ServerPlayer player,ServerLevel world,long until) {}
    private static final Map<UUID,Window> WINDOWS=new HashMap<>();
    public static java.util.function.ToLongFunction<Player> clientQuickUntil=player->0;
    public static java.util.function.Predicate<Player> clientReleaseLocked=player->false;
    private OcelotSpellLink() {}
    public static boolean wind(Player player) {
        return player!=null&&FormUtils.isForm(player,FormIdentifiers.OCELOT_SP);
    }
    public static double walkingMultiplier(){return BAL.d("wind_cast_walking_multiplier",.3);}
    /** Match SSC's actual dry, fed sneak-rush condition rather than treating every crouch as running. */
    public static boolean sneakRush(Player player) {
        return wind(player)&&player.isShiftKeyDown()&&player.getFluidHeight(net.minecraft.tags.FluidTags.WATER)<=0
                &&player.getFoodData().getFoodLevel()>=6;
    }
    public static boolean rushing(Player player){return wind(player)&&(player.isSprinting()||sneakRush(player));}
    public static boolean windLocked(Player player) {
        return wind(player)&&(player instanceof ServerPlayer server
                ?SpellChannelManager.releaseLocked(server):clientReleaseLocked.test(player));
    }
    /** Called only by the successful nine-lives revival path, including its self-detonation use. */
    public static void onNineLivesRevive(ServerPlayer player) {
        if(!player.isAlive()||!FormUtils.isForm(player,FormIdentifiers.OCELOT_NOVA))return;
        NeoRuntime.refreshForm(player);
        WINDOWS.put(player.getUUID(),new Window(player,player.serverLevel(),player.level().getGameTime()+BAL.i("nova_quick_cast_ticks",100)));
        NeoNetwork.hud(player);
    }
    public static long quickUntil(Player player) {
        if(player==null||!player.isAlive()||!FormUtils.isForm(player,FormIdentifiers.OCELOT_NOVA))return 0;
        if(!(player instanceof ServerPlayer server))return clientQuickUntil.applyAsLong(player);
        Window w=WINDOWS.get(player.getUUID());if(w==null||w.player!=player)return 0;
        if(w.world!=player.level()||player.level().getGameTime()>=w.until){WINDOWS.remove(player.getUUID(),w);return 0;}
        return w.until;
    }
    public static boolean quickCast(Player player){return quickUntil(player)>0;}
    public static void clear(ServerPlayer player){Window w=WINDOWS.get(player.getUUID());if(w!=null&&w.player==player)WINDOWS.remove(player.getUUID(),w);}
    public static void register() {
        // A replacement connection must not retain the old player's world/network references.
        github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(joined ->{
            Window old=WINDOWS.get(joined.getUUID());
            if(old!=null&&old.player!=joined)WINDOWS.remove(joined.getUUID(),old);
        });
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler,server)->clear(handler.player));
        github.com.gengyoubo.sscfa.util.SscEvents.onLivingDeath((entity,source)->{if(entity instanceof ServerPlayer player)clear(player);});
        github.com.gengyoubo.sscfa.util.SscEvents.afterRespawn((old,player,alive)->{clear(old);clear(player);});
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server->WINDOWS.clear());
    }
}
