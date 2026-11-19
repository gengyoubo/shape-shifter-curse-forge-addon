package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.MagicScrollItem;
import github.com.gengyoubo.sscfa.spell.ArcaneAnalysis;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.Spell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 增强符文方案。物品上的 NBT 只是展示镜像，只有世界内不可变的方案登记（{@link WorldRuneState}）
 * 才能授权一个方案：镜像必须带有本世界 ID、已登记的方案 ID，且其余字段与登记内容完全一致。
 */
public final class RuneScheme {
    public static final String KEY = "RuneScheme";

    private RuneScheme() {}

    public static boolean hasEnhancements(CompoundTag scheme) {
        return scheme != null && RuneLayout.hasEnhancements(scheme.getInt("Level"),
                scheme.getIntArray("Slots"), scheme.getInt("Version"));
    }

    public static boolean isModified(ItemStack stack) {
        return stack != null && stack.getTag() != null && stack.getTag().contains(KEY, Tag.TAG_COMPOUND)
                && hasEnhancements(stack.getTag().getCompound(KEY));
    }

    /** 登记一个新方案并返回物品镜像（含方案 ID）。 */
    public static CompoundTag create(WorldRuneState world, int level, int[] slots, RuneBuildEvaluator.Result result) {
        if (!result.valid()) throw new IllegalArgumentException("Invalid rune scheme");
        CompoundTag n = new CompoundTag();
        n.putUUID("World", world.worldId());
        n.putInt("Version", RuneLayout.VERSION);
        n.putInt("Rules", RuneBuildEvaluator.RULE_VERSION);
        n.putString("Spell", result.recipe().spell());
        n.putInt("Level", level);
        n.putIntArray("Slots", slots);
        n.putInt("Stability", result.stability());
        n.put("Modifiers", result.modifiers().write());
        n.put("Summary", RuneSummary.write(result));
        n.putUUID("Id", world.registerScheme(n));
        return n;
    }

    /** 世界不符、未登记或镜像被改动时返回 null。 */
    public static CompoundTag authoritative(WorldRuneState world, CompoundTag mirror) {
        if (mirror == null || !mirror.hasUUID("Id") || !mirror.hasUUID("World")
                || !world.worldId().equals(mirror.getUUID("World"))) return null;
        CompoundTag saved = world.scheme(mirror.getUUID("Id"));
        if (saved == null) return null;
        CompoundTag expected = mirror.copy();
        expected.remove("Id");
        return expected.equals(saved) ? saved : null;
    }

    /** 无方案的普通卷轴始终有效；有方案的卷轴必须得到本世界授权且法术、等级匹配。 */
    public static boolean validScroll(ServerPlayer player, ItemStack scroll, Spell spell) {
        if (scroll.getTag() == null || !scroll.getTag().contains(KEY)) return true;
        CompoundTag saved = authoritative(WorldRuneState.get(player.getServer()), scroll.getTag().getCompound(KEY));
        boolean valid = saved != null && spell != null && saved.getString("Spell").equals(spell.getId().getPath())
                && saved.getInt("Level") >= 1 && saved.getInt("Level") <= ScrollData.getLevel(scroll);
        if (valid) normalizeUnmodified(scroll, saved);
        else player.displayClientMessage(Component.translatable("research.ssc_addon.runes.invalid_scheme"), true);
        return valid;
    }

    public static CompoundTag authoritativeScroll(WorldRuneState world, ItemStack scroll) {
        if (scroll.getTag() == null || !scroll.getTag().contains(KEY)) return null;
        CompoundTag saved = authoritative(world, scroll.getTag().getCompound(KEY));
        Spell spell = ScrollData.getSpell(scroll);
        return saved != null && spell != null && saved.getString("Spell").equals(spell.getId().getPath())
                && saved.getInt("Level") >= 1 && saved.getInt("Level") <= ScrollData.getLevel(scroll) ? saved : null;
    }

    /** 只剥除已授权的旧版「仅基础」方案，保留卷轴其余字段。 */
    public static boolean normalizeUnmodified(WorldRuneState world, ItemStack scroll) {
        if (scroll.getTag() == null || !scroll.getTag().contains(KEY) || isModified(scroll)) return false;
        return normalizeUnmodified(scroll, authoritativeScroll(world, scroll));
    }

    private static boolean normalizeUnmodified(ItemStack scroll, CompoundTag saved) {
        if (saved == null || hasEnhancements(saved)) return false;
        scroll.getTag().remove(KEY);
        return true;
    }

    /**
     * 施法修饰参数。服务端只信任世界登记内容；客户端预览读取镜像。
     * 选择的施法等级低于方案等级（或为 1 级）时不生效。
     */
    public static RuneModifiers modifiers(Player player, ItemStack scroll, int selectedLevel) {
        if (scroll == null || scroll.getTag() == null || !scroll.getTag().contains(KEY)) return RuneModifiers.NONE;
        CompoundTag n = scroll.getTag().getCompound(KEY);
        if (player instanceof ServerPlayer serverPlayer) {
            n = authoritative(WorldRuneState.get(serverPlayer.getServer()), n);
            if (n == null) return RuneModifiers.NONE;
        }
        if (!hasEnhancements(n)) return RuneModifiers.NONE;
        if (selectedLevel <= 1 || selectedLevel < n.getInt("Level")) return RuneModifiers.NONE;
        return RuneModifiers.read(n.getCompound("Modifiers"));
    }

    public static int imprint(Container table, WorldRuneState world) {
        return imprint(table, world, 0, 3);
    }

    /**
     * 将法阵上的方案拓印到卷轴。返回码：0 成功；1 物品不符；2 方案未授权或卷轴无法术；3 法术/等级不匹配。
     * 全部校验在任何修改之前完成，成功时仅消耗一张法阵；卷轴的次数、所选等级、冷却与空间绑定不变。
     */
    public static int imprint(Container table, WorldRuneState world, int schemeSlot, int scrollSlot) {
        ItemStack scheme = table.getItem(schemeSlot), scroll = table.getItem(scrollSlot);
        if (!scheme.is(AddonItems.SPELL_FORMATION.get()) || scheme.getTag() == null
                || !(scroll.getItem() instanceof MagicScrollItem) || ArcaneAnalysis.isUnanalyzed(scroll)) return 1;
        CompoundTag mirror = scheme.getTag().getCompound(KEY), saved = authoritative(world, mirror);
        Spell spell = ScrollData.getSpell(scroll);
        if (saved == null || spell == null) return 2;
        if (!spell.getId().getPath().equals(saved.getString("Spell"))
                || ScrollData.getLevel(scroll) != saved.getInt("Level")) return 3;
        if (hasEnhancements(saved)) scroll.getOrCreateTag().put(KEY, mirror.copy());
        else scroll.getOrCreateTag().remove(KEY);
        scheme.shrink(1);
        table.setChanged();
        return 0;
    }
}
