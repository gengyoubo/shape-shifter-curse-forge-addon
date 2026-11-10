package github.com.gengyoubo.sscfa.client.hud;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.ErosionBrandClientState;
import github.com.gengyoubo.sscfa.ability.KillEmpowerCast;
import github.com.gengyoubo.sscfa.ability.KillEmpowerManager;
import github.com.gengyoubo.sscfa.ability.KillEmpowerState;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkClientState;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.client.ClientTickCache;
import github.com.gengyoubo.sscfa.client.DashClientState;
import github.com.gengyoubo.sscfa.client.FallenAllayVexClientState;
import github.com.gengyoubo.sscfa.client.JumpKillSilkClient;
import github.com.gengyoubo.sscfa.client.NovaLeapClientState;
import github.com.gengyoubo.sscfa.client.SkillCooldownClient;
import github.com.gengyoubo.sscfa.config.SSCAddonClientConfig;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.UIPositionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;

/** 技能冷却主面板 + 辅助进度条 HUD。 */
public final class SkillCooldownBarRenderer {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final ResourceLocation TEX_PANEL = new ResourceLocation("my_addon", "textures/gui/skill_cd_panel.png");
    private static final ResourceLocation TEX_PANEL_RIGHT = new ResourceLocation("my_addon", "textures/gui/skill_cd_panel_right.png");
    private static final ResourceLocation[] PANELS = {
            panelTexture("skill_cd_panel_2"), panelTexture("skill_cd_panel_3"), panelTexture("skill_cd_panel_4")};
    private static final ResourceLocation[] PANELS_RIGHT = {
            panelTexture("skill_cd_panel_2_right"), panelTexture("skill_cd_panel_3_right"), panelTexture("skill_cd_panel_4_right")};

    private static final String SSCA_FORM_NAMESPACE = "my_addon";
    public static final int ICON_SIZE = 20;
    public static final int SLOT_WIDTH = 33;
    public static final int SLOT_HEIGHT = 34;
    public static final int SLOT_STEP = 34;
    public static final int PANEL_HEIGHT = 68;
    private static final int INTERNAL_HEIGHT = 24;

    private final Map<String, Integer> trackedMaxValues = new HashMap<>();
    private long novaChargeAnchor = Long.MIN_VALUE;
    private final Map<String, Double> internalReadyCache = new HashMap<>();
    private long internalCacheTick = Long.MIN_VALUE;
    private ResourceLocation lastFormId;
    private Player lastPlayer;
    private Object lastWorld;

    private SkillCooldownBarRenderer() {
    }

    private static ResourceLocation panelTexture(String name) {
        return new ResourceLocation("my_addon", "textures/gui/" + name + ".png");
    }

    public static void register() {
        SkillCooldownBarRenderer renderer = new SkillCooldownBarRenderer();
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                HudScaling.render(event.getGuiGraphics(), 2,
                        () -> renderer.renderUnscaled(event.getGuiGraphics(), event.getPartialTick())));
    }

    private void renderUnscaled(GuiGraphics context, float tickDelta) {
        Player player = MC.player;
        if (player != lastPlayer || MC.level != lastWorld) {
            trackedMaxValues.clear();
            lastFormId = null;
            lastPlayer = player;
            lastWorld = MC.level;
        }
        if (player == null || MC.level == null) return;
        ResourceLocation formId = FormUtils.currentFormId(player);
        if (!java.util.Objects.equals(formId, lastFormId)) {
            trackedMaxValues.clear();
            lastFormId = formId;
            novaChargeAnchor = Long.MIN_VALUE;
        }
        if (formId == null || !SSCA_FORM_NAMESPACE.equals(formId.getNamespace())) return;
        SSCAddonClientConfig config = SSCAddonConfig.client();
        if (MC.options.hideGui || !config.showCdBar) return;
        config.migrateSkillHudLayout();
        var skills = SkillHudCatalog.entriesForHud(formId, player);
        if (skills.isEmpty()) return;

        int extras = SkillHudLayout.extraCount(skills);
        var anchor = UIPositionUtils.getCorrectPosition(config.cdBarPosType, 0, 0);
        int width = MC.getWindow().getGuiScaledWidth();
        int height = MC.getWindow().getGuiScaledHeight();
        Layout layout = panelLayout(anchor.getA() + config.cdBarPosOffsetX,
                anchor.getB() + config.cdBarPosOffsetY,
                config.cdMirrorRight, width, height, extras);
        drawPanel(context, layout.primaryX(), layout.primaryY(), config.cdMirrorRight, extras);

        for (var entry : skills) {
            SkillHudCatalog.Skill skill = entry.skill();
            int x = skill.primary() ? layout.primaryX() : layout.secondaryX();
            int y = skill.primary() ? layout.primaryY() : layout.secondaryY();
            Cooldown cooldown = readCooldown(player, skill.cooldown(), 0);
            boolean conditionBlocked = skill.condition() != null && !skill.condition().test(player);

            if (entry.slot().extra()) {
                drawExtraSlot(context, skill.resolveIcon(player),
                        layout.primaryX() + SkillHudLayout.extraX(entry.slot(), config.cdMirrorRight),
                        layout.primaryY() + SkillHudLayout.extraY(entry.slot()), cooldown,
                        config.showCdSeconds, conditionBlocked);
                continue;
            }

            double internalReady = readInternalReady(player, skill);
            if (FormIdentifiers.SNOW_FOX_FROSTSPINE.equals(formId) && !skill.primary()) {
                internalReady = ClientTickCache.frostForgeProgress();
            }
            if (FormIdentifiers.SNOW_FOX_FROSTSPINE.equals(formId) && skill.primary()) {
                int thorns = ClientTickCache.hoverThornCount();
                internalReady = thorns > 0 ? thorns / 5.0 : -1;
            }
            if (FormIdentifiers.AXOLOTL_FLUORESCENT.equals(formId) && skill.primary()
                    && !ClientTickCache.isWearing(SscAddon.SEA_CRYSTAL_PENDANT)) {
                internalReady = -1;
            }
            if (FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(formId) && skill.primary()) {
                double locked = Math.max(0, MancianimaMarkClientState.getStageEndTick() - MC.level.getGameTime())
                        / (double) MancianimaMarkManager.STAGE_GATE_TICKS;
                internalReady = 1 - Math.min(1, locked);
            }
            double cooldownShade = cooldown.fraction();
            int cooldownSeconds = (int) Math.ceil(cooldown.remaining() / 20.0);
            boolean countdown = false;
            if (FormIdentifiers.OCELOT_SP.equals(formId) && skill.primary()) {
                internalReady = DashClientState.hoverRemainingFraction(
                        SkillHudCatalog.balanceInt("abilities.wind_dash", "hover_ticks", 60));
                countdown = internalReady >= 0;
            }
            if (FormIdentifiers.SPIDER_SALTICIDAE.equals(formId) && skill.primary()) {
                double silkFraction = JumpKillSilkClient.recallWindowFraction();
                if (silkFraction >= 0) {
                    internalReady = silkFraction;
                    countdown = true;
                }
            }
            if (FormIdentifiers.OCELOT_NOVA.equals(formId) && !skill.primary()) {
                internalReady = NovaLeapClientState.remainingFraction(MC.level.getGameTime());
                countdown = internalReady >= 0;
            }
            if (FormIdentifiers.OCELOT_NOVA.equals(formId) && skill.primary()) {
                double chargeFraction = novaChargeFraction(player);
                if (chargeFraction >= 0) {
                    internalReady = chargeFraction;
                    countdown = false;
                }
            }
            if (FormIdentifiers.AXOLOTL_SP.equals(formId) && skill.primary()
                    && skill.cooldown() != null && "form_axolotl_sp_vortex_charge".equals(skill.cooldown().getPath())) {
                int vortexTicks = PowerUtils.getClientResourceValue(player, VortexChargeManager.VORTEX_STATE);
                if (vortexTicks > 0) {
                    internalReady = Math.min(1.0, vortexTicks
                            / (double) Math.max(1, VortexChargeManager.maxTicksForHud()));
                    countdown = false;
                }
            }
            if (FormIdentifiers.AXOLOTL_SP.equals(formId) && !skill.primary()
                    && skill.cooldown() != null && "form_axolotl_sp_play_dead_activate".equals(skill.cooldown().getPath())) {
                var deadEffect = player.getEffect(SscAddon.PLAYING_DEAD);
                if (deadEffect != null) {
                    int duration = SkillHudCatalog.balanceInt("abilities.playing_dead", "duration", 60);
                    internalReady = Math.min(1.0, Math.max(1, deadEffect.getDuration()) / (double) Math.max(1, duration));
                    countdown = true;
                }
            }
            if ((FormIdentifiers.AXOLOTL_FLUORESCENT.equals(formId) || FormIdentifiers.AXOLOTL_ALING.equals(formId))
                    && !skill.primary() && skill.cooldown() != null
                    && "form_axolotl_fluorescent_tidal".equals(skill.cooldown().getPath())) {
                double tetherFraction = ClientTickCache.tidalTetherFraction();
                if (tetherFraction >= 0) {
                    internalReady = tetherFraction;
                    countdown = true;
                }
            }
            if (FormIdentifiers.FALLEN_ALLAY_SP.equals(formId) && skill.primary()) {
                double vexFraction = FallenAllayVexClientState.remainingFraction(MC.level.getGameTime());
                if (vexFraction >= 0) {
                    internalReady = vexFraction;
                    countdown = true;
                }
            }
            if (FormIdentifiers.GOLDEN_SANDSTORM_SP.equals(formId) && !skill.primary()
                    && !ErosionBrandClientState.hasAnyDetonatable()) {
                conditionBlocked = true;
            }
            boolean empowerForm = FormIdentifiers.FAMILIAR_FOX_SP.equals(formId)
                    || FormIdentifiers.FAMILIAR_FOX_RED.equals(formId);
            if (empowerForm) {
                int empowerState = PowerUtils.getClientResourceValue(player, FormIdentifiers.EMPOWER_STATE);
                boolean empowered = (empowerState & KillEmpowerManager.STATE_READY) != 0;
                boolean empowerRing = (empowerState & KillEmpowerManager.STATE_RING) != 0;
                boolean ringActive = skill.primary()
                        && (KillEmpowerCast.isNormalRingActive(player) || empowerRing);
                if (skill.primary()) {
                    Cooldown activation = readCooldown(player, skill.internalCooldown(), 0);
                    if (activation.remaining() > cooldown.remaining()) {
                        cooldownShade = activation.fraction();
                        cooldownSeconds = (int) Math.ceil(activation.remaining() / 20.0);
                    }
                }
                if (ringActive || empowered) conditionBlocked = false;
                if (empowered) {
                    countdown = true;
                    int empowerTicks = PowerUtils.getClientResourceValue(player, FormIdentifiers.EMPOWER_TICKS);
                    cooldownShade = 0;
                    cooldownSeconds = 0;
                    internalReady = KillEmpowerState.countdownFraction(empowerTicks, KillEmpowerManager.EMPOWER_TICKS);
                }
                if (skill.primary() && empowerRing) {
                    countdown = true;
                    int ringTicks = PowerUtils.getClientResourceValue(player, FormIdentifiers.EMPOWER_RING_TICKS);
                    int ringMax = PowerUtils.getClientResourceValue(player, FormIdentifiers.EMPOWER_RING_DURATION);
                    internalReady = KillEmpowerState.countdownFraction(ringTicks, ringMax);
                }
                if (ringActive) {
                    cooldownShade = 1;
                    cooldownSeconds = 0;
                }
            }
            drawSkillSlot(context, skill.resolveIcon(player), x, y,
                    conditionBlocked ? 0 : cooldownShade,
                    cooldownSeconds, internalReady, skill.primary(),
                    config.showCdSeconds, config.cdMirrorRight, conditionBlocked, countdown);
        }
    }

    private double novaChargeFraction(Player player) {
        if (PowerUtils.getClientResourceValue(player, FormIdentifiers.OCELOT_NOVA_CHARGING) <= 0) {
            novaChargeAnchor = Long.MIN_VALUE;
            return -1;
        }
        long now = MC.level.getGameTime();
        if (novaChargeAnchor == Long.MIN_VALUE) novaChargeAnchor = now;
        int chargeTime = SkillHudCatalog.balanceInt("abilities.nova", "charge_time", 100);
        return Math.min(1.0, (now - novaChargeAnchor) / (double) Math.max(1, chargeTime));
    }

    private double readInternalReady(Player player, SkillHudCatalog.Skill skill) {
        ResourceLocation id = skill.internalCooldown();
        if (id == null || skill.internalTicks() <= 0) return -1;
        long tick = player.level().getGameTime();
        if (tick != internalCacheTick) {
            internalCacheTick = tick;
            internalReadyCache.clear();
        }
        String key = id.toString();
        Double hit = internalReadyCache.get(key);
        if (hit != null) return hit;
        var authoritative = SkillCooldownClient.get(key);
        double value;
        if (authoritative != null) {
            int remaining = authoritative.remainingNow(SkillCooldownClient.now());
            value = 1.0 - remaining / (double) skill.internalTicks();
        } else {
            int remaining = PowerUtils.getClientResourceValue(player, id);
            value = remaining <= 0 ? 1.0 : 1.0 - Math.min(1.0, remaining / (double) skill.internalTicks());
        }
        internalReadyCache.put(key, value);
        return value;
    }

    public record Layout(int primaryX, int primaryY, int secondaryX, int secondaryY) {}

    public static Layout panelLayout(int x, int y, boolean mirrorRight, int width, int height) {
        return panelLayout(x, y, mirrorRight, width, height, 0);
    }

    public static Layout panelLayout(int x, int y, boolean mirrorRight, int width, int height, int extras) {
        var placed = SkillHudLayout.place(x, y, mirrorRight, width, height, extras);
        return new Layout(placed.x(), placed.y(), placed.x(), placed.y() + SLOT_STEP);
    }

    public static int currentExtraSkillCount() {
        if (MC.player == null || MC.level == null) return 0;
        ResourceLocation formId = FormUtils.currentFormId(MC.player);
        if (formId == null || !SSCA_FORM_NAMESPACE.equals(formId.getNamespace())) return 0;
        return SkillHudLayout.extraCount(SkillHudCatalog.entriesForHud(formId, MC.player));
    }

    private record Cooldown(int remaining, double fraction) {}

    private Cooldown readCooldown(Player player, ResourceLocation id, int authoritativeTotal) {
        if (id == null) return new Cooldown(0, 0);
        var authoritative = SkillCooldownClient.get(id.toString());
        if (authoritative != null) {
            int remaining = authoritative.remainingNow(SkillCooldownClient.now());
            return new Cooldown(remaining, Math.min(1.0, remaining / (double) Math.max(1, authoritative.total())));
        }
        int remaining = Math.max(0, PowerUtils.getClientResourceValue(player, id));
        if (remaining == 0) {
            trackedMaxValues.remove(id.toString());
            return new Cooldown(0, 0);
        }
        int maximum = authoritativeTotal > 0
                ? Math.max(authoritativeTotal, remaining)
                : trackedMaxValues.merge(id.toString(), remaining, Math::max);
        return new Cooldown(remaining, remaining / (double) Math.max(1, maximum));
    }

    public static void drawPanel(GuiGraphics context, int x, int y, boolean mirrorRight) {
        drawPanel(context, x, y, mirrorRight, 0);
    }

    public static void drawPanel(GuiGraphics context, int x, int y, boolean mirrorRight, int extras) {
        var bounds = SkillHudLayout.bounds(extras, mirrorRight);
        ResourceLocation texture = (mirrorRight ? PANELS_RIGHT : PANELS)[Math.max(0, Math.min(2, extras))];
        context.blit(texture, x + bounds.x(), y + bounds.y(), 0, 0,
                bounds.width(), bounds.height(), bounds.width(), bounds.height());
        int barX = x + (mirrorRight ? 29 : 2);
        context.fill(barX, y + 8, barX + 2, y + 32, 0xFF8B8B8B);
        context.fill(barX, y + 36, barX + 2, y + 60, 0xFF8B8B8B);
    }

    private static void drawExtraSlot(GuiGraphics context, ResourceLocation icon, int x, int y, Cooldown cooldown,
                                      boolean showSeconds, boolean blocked) {
        int iconSize = SkillHudLayout.EXTRA_ICON_SIZE;
        int iconX = x + 2;
        int iconY = y + 2;
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF8B8B8B);
        context.pose().pushPose();
        context.pose().translate(iconX, iconY, 0);
        context.pose().scale(iconSize / 32.0f, iconSize / 32.0f, 1);
        context.blit(icon, 0, 0, 0, 0, 32, 32, 32, 32);
        context.pose().popPose();
        if (blocked) {
            context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x99000000);
        } else {
            int shade = (int) Math.ceil(iconSize * Math.max(0, Math.min(1, cooldown.fraction())));
            if (shade > 0) context.fill(iconX, iconY, iconX + iconSize, iconY + shade, 0xB0000000);
        }
        int seconds = (int) Math.ceil(cooldown.remaining() / 20.0);
        if (showSeconds && seconds > 0) {
            String number = Integer.toString(seconds);
            int textWidth = MC.font.width(number);
            float scale = Math.min(1, (iconSize - 2.0f) / Math.max(1, textWidth));
            context.pose().pushPose();
            context.pose().translate(iconX + (iconSize - textWidth * scale) / 2,
                    iconY + (iconSize - MC.font.lineHeight * scale) / 2, 0);
            context.pose().scale(scale, scale, 1);
            context.drawString(MC.font, number, 0, 0, 0xFFFFFFFF);
            context.pose().popPose();
        }
    }

    public static void drawSkillSlot(GuiGraphics context, ResourceLocation icon, int x, int y,
                                     double cooldownFraction, int seconds, double internalReadyFraction,
                                     boolean primary, boolean showSeconds, boolean mirrorRight) {
        drawSkillSlot(context, icon, x, y, cooldownFraction, seconds, internalReadyFraction,
                primary, showSeconds, mirrorRight, false);
    }

    public static void drawSkillSlot(GuiGraphics context, ResourceLocation icon, int x, int y,
                                     double cooldownFraction, int seconds, double internalReadyFraction,
                                     boolean primary, boolean showSeconds, boolean mirrorRight,
                                     boolean conditionBlocked) {
        drawSkillSlot(context, icon, x, y, cooldownFraction, seconds, internalReadyFraction,
                primary, showSeconds, mirrorRight, conditionBlocked, false);
    }

    private static void drawSkillSlot(GuiGraphics context, ResourceLocation icon, int x, int y,
                                      double cooldownFraction, int seconds, double internalReadyFraction,
                                      boolean primary, boolean showSeconds, boolean mirrorRight,
                                      boolean conditionBlocked, boolean countdown) {
        int iconX = x + (mirrorRight ? 3 : 10);
        int iconY = y + (primary ? 10 : 4);
        context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xFF8B8B8B);
        context.pose().pushPose();
        context.pose().translate(iconX, iconY, 0);
        context.pose().scale(ICON_SIZE / 32.0f, ICON_SIZE / 32.0f, 1.0f);
        context.blit(icon, 0, 0, 0, 0, 32, 32, 32, 32);
        context.pose().popPose();
        int shadeHeight = (int) Math.ceil(ICON_SIZE * Math.max(0, Math.min(1, cooldownFraction)));
        if (shadeHeight > 0) {
            context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + shadeHeight, 0xB0000000);
        }
        if (conditionBlocked) {
            context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0x99000000);
        }
        if (internalReadyFraction >= 0) {
            int readyHeight = countdown
                    ? KillEmpowerState.countdownHeight(internalReadyFraction, INTERNAL_HEIGHT)
                    : (int) Math.floor(INTERNAL_HEIGHT * Math.max(0, Math.min(1, internalReadyFraction)));
            if (readyHeight > 0) {
                int barBottom = primary ? 32 : 26;
                int textureBottom = primary ? 32 : 60;
                int barOffset = mirrorRight ? 29 : 2;
                context.blit(mirrorRight ? TEX_PANEL_RIGHT : TEX_PANEL,
                        x + barOffset, y + barBottom - readyHeight,
                        barOffset, textureBottom - readyHeight,
                        2, readyHeight, SLOT_WIDTH, PANEL_HEIGHT);
            }
        }
        if (showSeconds && seconds > 0) {
            String number = Integer.toString(seconds);
            int textWidth = MC.font.width(number);
            float scale = Math.min(1.0f, (ICON_SIZE - 2.0f) / Math.max(1, textWidth));
            context.pose().pushPose();
            context.pose().translate(iconX + (ICON_SIZE - textWidth * scale) / 2,
                    iconY + (ICON_SIZE - MC.font.lineHeight * scale) / 2, 0);
            context.pose().scale(scale, scale, 1.0f);
            context.drawString(MC.font, number, 0, 0, 0xFFFFFFFF);
            context.pose().popPose();
        }
    }
}
