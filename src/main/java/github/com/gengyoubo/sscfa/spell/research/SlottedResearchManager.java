package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class SlottedResearchManager {
    public static final int OPEN = 20, SAVE = 21, TEST = 22, COMPLETE = 23, TRAIN = 24, INSPECT = 25;

    private SlottedResearchManager() {}

    public static Component text(String key, Object... args) {
        return Component.translatable("research.ssc_addon.slotted." + key, args);
    }

    public static SpellResearchTableBlockEntity context(ServerPlayer player, int syncId) {
        return player.containerMenu instanceof SpellResearchTableScreenHandler handler
                && handler.containerId == syncId && handler.stillValid(player)
                && handler.getInventory() instanceof SpellResearchTableBlockEntity table ? table : null;
    }

    public static void handle(ServerPlayer player, int syncId, CompoundTag request) {
        SpellResearchTableBlockEntity table = context(player, syncId);
        if (table == null) return;
        WorldRuneState world = WorldRuneState.get(player.getServer());
        FormationKnowledgeComponent knowledge = FormationKnowledgeComponent.get(player);
        ResearchProgress progress = knowledge.research();
        progress.bind(world.worldId());
        migrateLegacyData(player, table, progress, world.language());

        int action = request.getInt("Action");
        int level = request.getInt("Level");
        if (level < 1 || level > 5) return;
        if (action != OPEN && (!request.hasUUID("World") || !world.worldId().equals(request.getUUID("World"))
                || request.getInt("Version") != RuneLayout.VERSION)) return;

        int[] slots = request.getIntArray("Slots");
        String key = "ready";
        Object[] arguments = {};
        int[] diagnosis = new int[0];
        if (action == OPEN) {
            slots = progress.runeDrafts.getOrDefault(level, RuneLayout.empty(level));
        } else if (!RuneLayout.validDraft(level, slots)) {
            key = "invalid";
        } else {
            progress.runeDrafts.put(level, slots.clone());
            RuneBuildEvaluator.Result evaluation = RuneBuildEvaluator.evaluate(
                    level, slots, world.language(), SlottedSpellRecipes.all());
            SlottedSpellRecipes.Recipe recipe = evaluation.valid() ? evaluation.recipe() : null;
            switch (action) {
                case SAVE -> key = "saved";
                case INSPECT -> {
                    int glyph = request.getInt("Glyph");
                    if (glyph < 0 || glyph > 17) return;
                    progress.reveal(glyph);
                    if (progress.knows(world.language().glyph(RuneRole.SOURCE))
                            && progress.knows(world.language().glyph(RuneRole.GAIN))
                            && progress.knows(world.language().glyph(RuneRole.STABLE))) {
                        progress.rank = Math.max(1, progress.rank);
                    }
                    key = "identified";
                    arguments = new Object[]{Component.translatable(
                            "research.ssc_addon.slotted.role." + world.language().meaning(glyph).ordinal())};
                }
                case TEST -> {
                    key = recipe == null ? "invalid" : "valid";
                    if (recipe != null) {
                        arguments = new Object[]{Component.translatable(SpellRegistry.get(recipe.spell()).getNameKey()), level};
                    }
                    diagnosis = FormationDiagnosis.diagnose(level, slots, world.language());
                    if (!evaluation.problems().isEmpty()) {
                        RuneBuildEvaluator.Problem problem = evaluation.problems().get(0);
                        key = "rune_" + problem.rule();
                        arguments = problem.rule().equals("stability")
                                ? new Object[]{problem.amount()}
                                : new Object[]{problem.first() + 1, problem.second() + 1, problem.amount()};
                    }
                }
                case TRAIN -> {
                    if (recipe == null || level != progress.rank + 1 || progress.rank < 1
                            || (progress.slotCompleted.isEmpty() && progress.runeCompleted.isEmpty())) {
                        key = "training";
                    } else {
                        progress.rank = level;
                        key = "promoted";
                    }
                }
                case COMPLETE -> {
                    if (!request.hasUUID("Operation")) return;
                    SlottedFormationTransaction.Result result = request.getBoolean("Upgrade")
                            ? SlottedFormationTransaction.upgradeEnhanced(table, player.getInventory(), knowledge, world, level, slots, request.getUUID("Operation"))
                            : SlottedFormationTransaction.completeEnhanced(table, player.getInventory(), knowledge, world, level, slots, request.getUUID("Operation"));
                    key = result.key();
                    if (result.success()) {
                        player.containerMenu.sendAllDataToRemote();
                        player.serverLevel().playSound(null, table.getBlockPos(),
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, .7f, .9f);
                        player.serverLevel().playSound(null, table.getBlockPos(),
                                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, .8f, 1.15f);
                    }
                }
                default -> {
                    return;
                }
            }
        }

        FormationKnowledgeComponent.sync(player);
        CompoundTag state = new CompoundTag();
        state.putBoolean("Slotted", true);
        state.putInt("Action", action);
        state.putInt("Level", level);
        if (request.hasUUID("Request")) state.putUUID("Request", request.getUUID("Request"));
        state.putInt("Revision", request.getInt("Revision"));
        state.putUUID("World", world.worldId());
        state.putInt("Version", RuneLayout.VERSION);
        state.putUUID("Operation", progress.completionToken);
        state.put("Progress", progress.write());
        state.putIntArray("Slots", slots);
        if (diagnosis.length > 0) state.putIntArray("Diagnosis", diagnosis);

        int[] meanings = new int[18], schools = new int[18];
        Arrays.fill(meanings, -1);
        for (int glyph = 0; glyph < 18; glyph++) {
            if (progress.knows(glyph)) meanings[glyph] = world.language().meaning(glyph).ordinal();
            schools[glyph] = SlottedFormation.school(world.language(), glyph);
        }
        if (RuneLayout.validDraft(level, slots)) {
            RuneBuildEvaluator.Result evaluated = RuneBuildEvaluator.evaluate(
                    level, slots, world.language(), SlottedSpellRecipes.all());
            state.putInt("Stability", evaluated.stability());
            state.putBoolean("RuneValid", evaluated.valid());
            state.put("Modifiers", evaluated.modifiers().write());
            state.put("RuneTooltipState", RuneSlotTooltips.write(evaluated));
            state.put("RuneSummary", RuneSummary.preview(evaluated, level, player));
            state.putIntArray("Inactive", evaluated.inactive().stream().toArray());
            state.putIntArray("Synergy", evaluated.synergy().stream().toArray());
            state.putIntArray("Suppressed", evaluated.suppressed().stream().toArray());
            state.putIntArray("Problems", evaluated.problems().stream()
                    .flatMapToInt(problem -> java.util.stream.IntStream.of(problem.first(), problem.second()))
                    .filter(value -> value >= 0).distinct().toArray());
        }
        state.putIntArray("Meanings", meanings);
        state.putIntArray("Schools", schools);
        if (action == OPEN || action == COMPLETE) {
            state.put("UnlockedReferences", UnlockedSpellReferences.write(knowledge, world, level));
        }

        if (!AddonNetwork.canSend(player, FormationResearchNetworking.STATE)) return;
        var packet = AddonNetwork.buffer();
        packet.writeVarInt(syncId);
        packet.writeNbt(state);
        packet.writeComponent(text(key, arguments));
        packet.writeBoolean(key.equals("valid") || key.equals("completed"));
        packet.writeVarInt(-1);
        packet.writeVarInt(-1);
        packet.writeVarInt(0);
        AddonNetwork.sendToPlayer(player, FormationResearchNetworking.STATE, packet);
    }

    private static void migrateLegacyData(ServerPlayer player, SpellResearchTableBlockEntity table,
                                          ResearchProgress progress, RuneLanguage language) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            migrateDiagramStack(player.getInventory().getItem(slot), language);
        }
        for (int slot = 0; slot < table.getContainerSize(); slot++) {
            migrateDiagramStack(table.getItem(slot), language);
        }
        for (var entry : progress.slotReferences.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            int[] migrated = SlottedSpellRecipes.migrateIfLegacy(parts[0], parseIntSafe(parts[1]), entry.getValue(), language);
            if (migrated != null) entry.setValue(migrated);
        }
    }

    private static void migrateDiagramStack(ItemStack stack, RuneLanguage language) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getInt("Version") != 1 || !stack.is(SscAddon.ANALYZED_SPELL_DIAGRAM)) return;
        int[] migrated = SlottedSpellRecipes.migrateIfLegacy(tag.getString("Spell"), tag.getInt("Level"),
                tag.getIntArray("Slots"), language);
        if (migrated != null) tag.putIntArray("Slots", migrated);
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
