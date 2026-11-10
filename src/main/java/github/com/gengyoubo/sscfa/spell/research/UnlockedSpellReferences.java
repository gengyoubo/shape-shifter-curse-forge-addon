package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Only the player's unlocked, research-rank-accessible foundations are sent as references. */
public final class UnlockedSpellReferences {
    private UnlockedSpellReferences() {}

    public static ListTag write(FormationKnowledgeComponent knowledge, WorldRuneState world, int level) {
        ListTag entries = new ListTag();
        ResearchProgress progress = knowledge.research();
        if (!world.worldId().equals(progress.world) || level < 1 || level > progress.rank) return entries;
        for (var recipe : SlottedSpellRecipes.all()) {
            if (!knowledge.hasSpell(recipe.spell()) || !SlottedSpellRecipes.available(recipe, level)) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("Spell", recipe.spell());
            entry.putInt("Level", level);
            entry.putIntArray("Slots", RuneLayout.foundation(recipe, level, world.language()));
            entries.add(entry);
        }
        return entries;
    }
}
