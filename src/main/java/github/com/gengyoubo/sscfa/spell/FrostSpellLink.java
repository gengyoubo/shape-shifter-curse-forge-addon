package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.ability.FrostSpikeManager;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Ice-only native-form collaboration. Quotes never consume a buff or a hovering thorn. */
public final class FrostSpellLink {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private record Ready(ServerPlayer player,ServerLevel world,long until) {}
    private static final Map<UUID,Ready> READY=new HashMap<>();
    public static java.util.function.ToDoubleFunction<Player> clientMultiplier=player->1;
    private FrostSpellLink() {}

    public static void onNativeHit(ServerPlayer caster,LivingEntity target) {
        if(caster==null||target==null||!caster.isAlive()||caster.isRemoved()||target instanceof ArmorStand
                ||target.isSpectator()||target.level()!=caster.level()
                ||!FormUtils.isForm(caster,FormIdentifiers.SNOW_FOX_SP)||WhitelistUtils.isProtected(caster,target))return;
        long until=caster.level().getGameTime()+BAL.i("snow_fox_ready_ticks",60);
        Ready previous=ready(caster);
        if(previous!=null&&previous.until==until)return; // An area hit refreshes once per server tick.
        READY.put(caster.getUUID(),new Ready(caster,caster.serverLevel(),until));
    }
    private static Ready ready(ServerPlayer player) {
        Ready value=READY.get(player.getUUID());
        if(value!=null&&value.player!=player)return null;
        if(value!=null&&(value.world!=player.level()||!player.isAlive()
                ||!FormUtils.isForm(player,FormIdentifiers.SNOW_FOX_SP)||player.level().getGameTime()>=value.until)) {
            READY.remove(player.getUUID(),value);return null;
        }
        return value;
    }
    public static long readyUntil(ServerPlayer player){Ready value=ready(player);return value==null?0:value.until;}
    public static double multiplier(Player player,boolean ice) {
        if(!ice||player==null)return 1;
        if(!(player instanceof ServerPlayer server))return clientMultiplier.applyAsDouble(player);
        if(FormUtils.isForm(server,FormIdentifiers.SNOW_FOX_SP))return ready(server)==null?1:1-BAL.d("snow_fox_collapse_reduction",.15);
        if(FormUtils.isForm(server,FormIdentifiers.SNOW_FOX_FROSTSPINE))
            return Math.max(0,1-FrostSpikeManager.hoveringThorns(server).size()*BAL.d("frostspine_reduction_per_thorn",.05));
        return 1;
    }
    public static Payment plan(ServerPlayer player,boolean ice) {
        if(!ice)return null;
        if(FormUtils.isForm(player,FormIdentifiers.SNOW_FOX_SP)) {
            Ready buff=ready(player);
            return buff==null?null:new Payment(player,buff,List.of(),1-BAL.d("snow_fox_collapse_reduction",.15),0);
        }
        if(!FormUtils.isForm(player,FormIdentifiers.SNOW_FOX_FROSTSPINE))return null;
        var thorns=FrostSpikeManager.hoveringThorns(player);
        return thorns.isEmpty()?null:new Payment(player,null,thorns,
                Math.max(0,1-thorns.size()*BAL.d("frostspine_reduction_per_thorn",.05)),BAL.i("frostspine_lifetime_cost_ticks",360));
    }
    public static final class Payment {
        private final ServerPlayer player;
        private final Ready buff;
        private final List<FrostThornEntity> thorns;
        private final int lifetimeCost;
        public final double multiplier;
        private boolean paid;
        private int lifetimeSpent;
        private Payment(ServerPlayer player,Ready buff,List<FrostThornEntity> thorns,double multiplier,int lifetimeCost) {
            this.player=player;this.buff=buff;this.thorns=thorns;this.multiplier=multiplier;this.lifetimeCost=lifetimeCost;
        }
        public boolean pay() {
            if(paid)return true;
            if(!player.isAlive()||player.isRemoved()||!thorns.isEmpty()&&!FormUtils.isForm(player,FormIdentifiers.SNOW_FOX_FROSTSPINE))return false;
            if(buff!=null&&ready(player)!=buff)return false;
            if(thorns.stream().anyMatch(t->t.isRemoved()||!t.isHover()||t.level()!=player.level()))return false;
            if(buff!=null)READY.remove(player.getUUID(),buff);
            for(var thorn:thorns)lifetimeSpent+=thorn.spendHoverLifetime(lifetimeCost);
            paid=true;return true;
        }
        public void record(Object record) {
        }
    }
    public static void clear(ServerPlayer player) {
        Ready value=READY.get(player.getUUID());if(value!=null&&value.player==player)READY.remove(player.getUUID(),value);
    }
    public static void register() {
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler,server)->clear(handler.player));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) clear(player);
                });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.server.ServerStoppedEvent event) -> READY.clear());
    }
}
