package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager;
import github.com.gengyoubo.sscfa.ability.fruit.FruitSelection;
import github.com.gengyoubo.sscfa.ability.fruit.ParasiticFruitAbility;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * 寄生果蝠与月织蛛的客户端状态镜像：所选果实、蓄力进度（技能栏图标与蓄力条）与月织蛛织网模式。
 * 每次被接受的蓄力只播两次提示音：达到最小蓄力与满蓄力。
 */
public final class ParasiticFruitClient {
    private static FruitSelection selection = FruitSelection.AUTO;
    private static boolean charging;
    private static long chargeStart;
    private static int chargeCues;
    private static int moonWeaverMode;
    private static boolean registered;

    private ParasiticFruitClient() {}

    public static void register() {
        if (registered) return;
        registered = true;
        AddonNetwork.registerClientReceiver(ParasiticFruitAbility.STATE, buf -> {
            FruitSelection nextSelection = FruitSelection.fromId(buf.readVarInt());
            boolean nextCharging = buf.readBoolean();
            int ticks = buf.readVarInt();
            var level = Minecraft.getInstance().level;
            long now = level == null ? 0 : level.getGameTime();
            selection = nextSelection;
            if (nextCharging) {
                // 只用服务端值校准起点，避免每 5 tick 的校准包把提示音重置。
                long start = now - ticks;
                if (!charging || Math.abs(start - chargeStart) > 3) chargeStart = start;
            } else {
                chargeCues = 0;
            }
            charging = nextCharging;
        });
        AddonNetwork.registerClientReceiver(SpiderMoonWeaverWebManager.MODE_STATE, buf -> moonWeaverMode = buf.readVarInt());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> reset());
    }

    public static FruitSelection selection() { return selection; }

    public static int moonWeaverMode() { return moonWeaverMode; }

    /** 蓄力占满 30 tick 的比例；未蓄力为 -1。 */
    public static double chargeFraction() {
        var level = Minecraft.getInstance().level;
        if (!charging || level == null) return -1;
        return Math.max(0, Math.min(1, (level.getGameTime() - chargeStart) / (double) FruitSelection.FULL_CHARGE));
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            reset();
            return;
        }
        if (!charging) return;
        long held = client.level.getGameTime() - chargeStart;
        if (chargeCues == 0 && held >= FruitSelection.MIN_CHARGE) {
            cue(client, false);
            chargeCues = 1;
        }
        if (chargeCues == 1 && held >= FruitSelection.FULL_CHARGE) {
            cue(client, true);
            chargeCues = 2;
        }
    }

    private static void cue(Minecraft client, boolean full) {
        client.getSoundManager().play(new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_PLING.value().getLocation(),
                SoundSource.PLAYERS, 0.45f, full ? 1.2f : 0.8f, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }

    private static void reset() {
        selection = FruitSelection.AUTO;
        charging = false;
        chargeCues = 0;
        moonWeaverMode = 0;
    }
}
