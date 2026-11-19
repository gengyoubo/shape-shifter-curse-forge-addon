package github.com.gengyoubo.sscfa.client.hud;

import static github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.Box;
import static github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.DEFAULT;
import static github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.View;
import static github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.percent;
import static github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.place;

import github.com.gengyoubo.sscfa.config.SSCAddonClientConfig;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import github.com.gengyoubo.sscfa.util.ManaBarPos;
import github.com.gengyoubo.sscfa.util.UIPositionUtils;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 每个 HUD 独立的本地缩放。缩放以该 HUD 的原始包围盒为基准：左/上对齐的 HUD 保持左上角，
 * 贴右镜像的 HUD 保持右缘，并钳制在屏幕内；所有元素（含文字）共用同一矩阵。
 * 配置键见 {@link #KEYS}，值为百分比（50–200，默认 100）。附属只绘制
 * mana / cooldown / spellbook / charge 四块，其余键仅为与 Fabric 版配置兼容而保留。
 */
public final class HudScaling {
    public static final String[] KEYS = {"instinct", "mana", "cooldown", "spellbook", "charge", "ssc_magic", "stability", "item_store"};
    private static final ThreadLocal<Integer> ACTIVE = ThreadLocal.withInitial(() -> -1);

    private HudScaling() {}

    public static int get(SSCAddonClientConfig config, int section) {
        if (section < 0 || section >= KEYS.length || config.hudScalePercent == null) return DEFAULT;
        Integer value = config.hudScalePercent.get(KEYS[section]);
        return value == null ? DEFAULT : percent(value);
    }

    public static void set(SSCAddonClientConfig config, int section, int value) {
        if (section < 0 || section >= KEYS.length) throw new IllegalArgumentException("Unknown HUD section " + section);
        if (config.hudScalePercent == null) config.hudScalePercent = new HashMap<>();
        config.hudScalePercent.put(KEYS[section], percent(value));
    }

    public static boolean isVisible(SSCAddonClientConfig config, int section) {
        return switch (KEYS[section]) {
            case "instinct" -> config.showInstinctBar;
            case "mana" -> config.showManaBar;
            case "cooldown" -> config.showCdBar;
            case "spellbook" -> config.showSpellbookHud;
            case "charge" -> config.showChargeBar;
            case "ssc_magic" -> config.showSscMagicHud;
            case "item_store" -> config.showSscItemStoreHud;
            case "stability" -> config.showNeoStabilityBar;
            default -> throw new IllegalArgumentException("Unknown HUD section " + section);
        };
    }

    /** 未缩放时的屏幕包围盒。 */
    public static Box raw(int section) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        SSCAddonClientConfig c = SSCAddonConfig.client();
        switch (section) {
            case 1: {
                int[] args = ManaBarPos.get(8, 100, -17);
                var p = UIPositionUtils.getCorrectPosition(args[0], args[1], args[2]);
                return new Box(p.getA(), p.getB(), 80, 5);
            }
            case 2: {
                c.migrateSkillHudLayout();
                var p = UIPositionUtils.getCorrectPosition(c.cdBarPosType, 0, 0);
                int extras = SkillCooldownBarRenderer.currentExtraSkillCount();
                var b = SkillCooldownBarRenderer.panelLayout(p.getA() + c.cdBarPosOffsetX, p.getB() + c.cdBarPosOffsetY,
                        c.cdMirrorRight, w, h, extras);
                var extent = SkillHudLayout.bounds(extras, c.cdMirrorRight);
                return new Box(b.primaryX() + extent.x(), b.primaryY() + extent.y(), extent.width(), extent.height());
            }
            case 3:
                return SpellbookHudRenderer.scaleBounds();
            case 4:
                c.migrateSkillHudLayout();
                return SpellCastHud.scaleBounds(w, h);
            default:
                throw new IllegalArgumentException("HUD section " + section + " is not drawn by the addon");
        }
    }

    public static boolean rightAligned(int section) {
        SSCAddonClientConfig c = SSCAddonConfig.client();
        return section == 2 ? c.cdMirrorRight : section == 4 && c.chargeMirrorRight;
    }

    public static View view(int section) {
        Minecraft mc = Minecraft.getInstance();
        Box source = raw(section);
        int scale = get(SSCAddonConfig.client(), section);
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        if (section == 2) {
            return SkillHudLayout.scaledView(source, scale, w, h, rightAligned(section),
                    SkillCooldownBarRenderer.currentExtraSkillCount());
        }
        return place(source, scale, w, h, rightAligned(section));
    }

    public static Box bounds(int section) { return view(section).bounds(); }

    public static boolean hit(int section, double x, double y) { return bounds(section).contains(x, y, 2); }

    public static void render(GuiGraphics context, int section, Runnable draw) {
        // 蓄力条在绘制中同时推进退场看门狗，其显示开关由自身在清理后判断。
        if (section != 4 && section >= 0 && section < KEYS.length && !isVisible(SSCAddonConfig.client(), section)) return;
        // 共享渲染器的重入调用不得重复缩放。
        if (ACTIVE.get() == section) {
            draw.run();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || get(SSCAddonConfig.client(), section) == DEFAULT) {
            draw.run();
            return;
        }
        View v;
        try {
            v = view(section);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException unavailable) {
            draw.run();
            return;
        }
        Box source = v.source(), target = v.bounds();
        if (v.scale() == 1 && source.x() == target.x() && source.y() == target.y()) {
            draw.run();
            return;
        }
        context.flush();
        var pose = context.pose();
        pose.pushPose();
        int previous = ACTIVE.get();
        ACTIVE.set(section);
        try {
            pose.translate(target.x(), target.y(), 0);
            pose.scale(v.scale(), v.scale(), 1);
            pose.translate(-source.x(), -source.y(), 0);
            draw.run();
        } finally {
            try {
                context.flush();
            } finally {
                pose.popPose();
                ACTIVE.set(previous);
            }
        }
    }
}
