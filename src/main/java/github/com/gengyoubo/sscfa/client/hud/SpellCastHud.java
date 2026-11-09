package github.com.gengyoubo.sscfa.client.hud;

import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;

/** 施法 HUD（Forge 占位）：状态读取接口保留；HUD 绘制待客户端渲染层移植。 */
public final class SpellCastHud {
    private SpellCastHud() {}

    public record State(Spell spell, int level, int token, SpellCastingRules.Mode mode,
                        boolean solo, boolean locked, boolean released) {}

    public static void register() {}
    public static void clear() {}
    public static State getState() { return null; }
    public static void markLocalCancelling() {}
    public static void clearLocalCancelling() {}
}
