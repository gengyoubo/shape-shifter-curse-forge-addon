package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.client.evolution.EvolutionScreen;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.client.FormKeyMappings;
import net.onixary.shapeShifterCurseForge.client.codex.BookOfShapeShifterScreenV2_P1;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.slf4j.LoggerFactory;

/** Opt-in real-client smoke test; excluded from release jars. */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID, value = Dist.CLIENT)
public final class IssueClientRegression {
    private static int ticks;
    private static float batEyeHeight;
    private static boolean useLeawind;
    private static Object perspectiveOverride;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("ssca.issueRegression") || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null) mc.setScreen(null);
        ticks++;
        if (ticks == 40) {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            registerLeawindPerspectiveOverride();
            setForm(mc, "axolotl_sp");
        }
        if (ticks == 80 || ticks == 140 || ticks == 230 || ticks == 290) FormKeyMappings.ACTIVE_SKILL_1.setDown(true);
        if (ticks == 81 || ticks == 141 || ticks == 231 || ticks == 291) FormKeyMappings.ACTIVE_SKILL_1.setDown(false);
        if (ticks == 100 || ticks == 250) require(
                github.com.gengyoubo.sscfa.util.PowerUtils.getClientResourceValue(mc.player,
                        github.com.gengyoubo.sscfa.ability.VortexChargeManager.VORTEX_STATE) > 0,
                "primary key must actually start the vortex");
        if (ticks == 160 || ticks == 310) {
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> server.getPlayerList().getPlayer(uuid).addEffect(
                    new net.minecraft.world.effect.MobEffectInstance(AddonEffects.PLAYING_DEAD.get(),
                            github.com.gengyoubo.sscfa.action.SscAddonActions.PLAY_DEAD_DURATION_TICKS)));
        }
        if (ticks == 175 || ticks == 325) {
            require(mc.player.hasEffect(AddonEffects.PLAYING_DEAD.get()), "playing dead must be synchronized for the HUD regression");
            LoggerFactory.getLogger("IssueRegression").info("PLAY_DEAD_HUD_PASS: camera={}", mc.options.getCameraType());
        }
        if (ticks == 190) {
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                player.removeAllEffects();
                github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(player,
                        github.com.gengyoubo.sscfa.action.SscAddonActions.PLAY_DEAD_SKILL_ID, 0);
            });
        }
        if (ticks == 200) {
            useLeawind = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(player, "my_addon:form_axolotl_sp_vortex_charge", 0);
            });
        }
        if (ticks == 330) {
            LoggerFactory.getLogger("IssueRegression").info("AXOLOTL_RENDER_PASS: primary key in both perspectives");
            setForm(mc, "bat_desmodus");
        }
        if (ticks == 370 || ticks == 530 || ticks == 580) FormKeyMappings.ACTIVE_SKILL_1.setDown(true);
        if (ticks == 371 || ticks == 531 || ticks == 581) FormKeyMappings.ACTIVE_SKILL_1.setDown(false);
        if (ticks == 360) batEyeHeight = mc.player.getEyeHeight();
        if (ticks == 400) require(mc.player.hasEffect(AddonEffects.MIST_FORM.get()), "bat must enter mist");
        if (ticks == 490 || ticks == 640) {
            require(!mc.player.hasEffect(AddonEffects.MIST_FORM.get()), "mist should have ended");
            var pos = mc.gameRenderer.getMainCamera().getPosition();
            require(Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z), "camera position must remain finite");
            require(Float.isFinite(mc.player.getYRot()) && Float.isFinite(mc.player.getXRot()), "player rotation must remain finite");
            LoggerFactory.getLogger("IssueRegression").info("BAT_CAMERA_PASS: mist ended, camera={} eye={}", pos, mc.player.getEyeHeight());
        }
        if (ticks == 500 || ticks == 650) {
            require(Math.abs(mc.player.getEyeHeight() - batEyeHeight) < 1e-4,
                    "bat eye height must restore to " + batEyeHeight + ", was " + mc.player.getEyeHeight());
            verifyLeawindCameraActive();
        }
        if (ticks == 500) {
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(
                    server.getPlayerList().getPlayer(uuid), "my_addon:form_bat_desmodus_mist_form", 0));
        }
        if (ticks == 650) {
            setForm(mc, "spider_moon_weaver");
        }
        if (ticks == 720) {
            require(!mc.player.hasEffect(MobEffects.NIGHT_VISION), "spider night vision must not use a short potion");
            LoggerFactory.getLogger("IssueRegression").info("NIGHT_VISION_PASS: no flickering potion effect");
            setForm(mc, "upgrade_familiar_fox");
        }
        if (ticks == 760) {
            mc.setScreen(new BookOfShapeShifterScreenV2_P1(mc.player));
            var button = mc.screen.children().stream()
                    .filter(child -> child instanceof net.minecraft.client.gui.components.Button)
                    .map(child -> (net.minecraft.client.gui.components.Button) child)
                    .filter(b -> b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                            && t.getKey().equals("text.ssc_addon.evolution.book.button"))
                    .findFirst().orElseThrow(() -> new AssertionError("missing evolution button"));
            button.onPress();
            require(mc.screen instanceof EvolutionScreen, "button must open the evolution screen");
            LoggerFactory.getLogger("IssueRegression").info("EVOLUTION_ENTRY_PASS");
            mc.setScreen(null);
        }
        if (ticks == 800) {
            LoggerFactory.getLogger("IssueRegression").info("ALL_CLIENT_ISSUE_CHECKS_PASSED");
            mc.stop();
        }
    }

    private static void setForm(Minecraft mc, String form) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            player.removeAllEffects();
            FormManager.setForm(player, ResourceLocation.fromNamespaceAndPath("my_addon", form));
            for (String skill : new String[]{"my_addon:form_axolotl_sp_vortex_charge",
                    "my_addon:form_axolotl_sp_play_dead_activate", "my_addon:form_bat_desmodus_mist_form"}) {
                github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.force(player, skill, 0);
            }
            player.setAirSupply(300);
            player.teleportTo(0.5, -59, 0.5);
        });
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    private static void verifyLeawindCameraActive() {
        try {
            Class<?> api = Class.forName("io.github.leawind.perspectiveapi.api.PerspectiveAPI");
            boolean current = (boolean) api.getMethod("isCurrent", String.class)
                    .invoke(null, "leawind_third_person.third_person");
            require(current, "Leawind third person must actually be active");
            LoggerFactory.getLogger("IssueRegression").info("LEAWIND_3_CAMERA_PASS");
        } catch (ClassNotFoundException absent) {
            // This smoke test also runs without optional camera mods.
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Leawind regression test failed", failure);
        }
    }

    private static void registerLeawindPerspectiveOverride() {
        try {
            Class<?> api = Class.forName("io.github.leawind.perspectiveapi.api.PerspectiveAPI");
            api.getMethod("setEnabled", boolean.class).invoke(null, true);
            Object chain = api.getMethod("getOverrideChain").invoke(null);
            Class<?> chainType = Class.forName("io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain");
            perspectiveOverride = chainType.getMethod("register", int.class, java.util.function.Supplier.class)
                    .invoke(chain, 10000, (java.util.function.Supplier<String>) () ->
                            useLeawind ? "leawind_third_person.third_person" : null);
        } catch (ClassNotFoundException absent) {
            // Optional dependency is only installed by the Leawind test launcher.
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Could not activate Leawind for the client regression", failure);
        }
    }
}
