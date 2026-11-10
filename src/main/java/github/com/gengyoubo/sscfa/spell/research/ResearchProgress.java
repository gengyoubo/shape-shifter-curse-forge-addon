package github.com.gengyoubo.sscfa.spell.research;

import net.minecraft.nbt.*;
import java.util.*;

/** Lives inside the existing always-copied player knowledge component, with a separate NBT key. */
public final class ResearchProgress {
    public int rank;
    public int knownGlyphs, attuned, knownRules;
    public boolean migrated;
    public UUID world;
    public final Set<String> legacy = new HashSet<>(), drawn = new HashSet<>();
    public final Map<String, CompoundTag> references = new LinkedHashMap<>();
    public CompoundTag draft = new CompoundTag();
    public CompoundTag spellDraft = new CompoundTag();
    public String spellTarget = "";
    public final Map<Integer,int[]> slotDrafts = new HashMap<>();
    public final Map<String,int[]> slotReferences = new TreeMap<>();
    public final Set<String> slotCompleted = new HashSet<>();
    public final Map<Integer,int[]> runeDrafts = new HashMap<>();
    public final Map<String,int[]> runeReferences = new TreeMap<>();
    public final Set<String> runeCompleted = new HashSet<>();
    public UUID completionToken = UUID.randomUUID();
    public boolean knows(int glyph) { return glyph >= 0 && glyph < 18 && (knownGlyphs & 1 << glyph) != 0; }
    public void reveal(int glyph) { if (glyph >= 0 && glyph < 18) knownGlyphs |= 1 << glyph; }
    public boolean attuned(int family) { return (attuned & 1 << family) != 0; }
    public void bind(UUID id) {
        if(world!=null&&!world.equals(id)){slotDrafts.clear();slotReferences.clear();slotCompleted.clear();runeDrafts.clear();runeReferences.clear();runeCompleted.clear();completionToken=UUID.randomUUID();}
        if (world != null && !world.equals(id)) { knownGlyphs = 0; knownRules = 0; references.clear(); draft = new CompoundTag(); spellDraft = new CompoundTag(); spellTarget = ""; }
        world = id;
    }
    public CompoundTag write() {
        CompoundTag n = new CompoundTag(); n.putInt("Version", 1); n.putInt("Rank", rank); n.putInt("Glyphs", knownGlyphs);
        n.putInt("Attuned", attuned); n.putInt("Rules", knownRules); n.putBoolean("Migrated", migrated);
        if (world != null) n.putUUID("World", world);
        ListTag old = new ListTag(), completed = new ListTag(); legacy.stream().sorted().forEach(v -> old.add(StringTag.valueOf(v))); drawn.stream().sorted().forEach(v -> completed.add(StringTag.valueOf(v)));
        n.put("Legacy", old); n.put("Drawn", completed);
        CompoundTag refs = new CompoundTag(); references.forEach((key, diagram) -> refs.put(key, diagram.copy())); n.put("References", refs); n.put("Draft", draft.copy());
        n.put("SpellDraft", spellDraft.copy());n.putString("SpellTarget", spellTarget);
        CompoundTag slots=new CompoundTag(),patterns=new CompoundTag();
        slotDrafts.forEach((level,values)->slots.putIntArray(String.valueOf(level),values));
        slotReferences.forEach(patterns::putIntArray);n.put("SlotDrafts",slots);n.put("SlotReferences",patterns);
        ListTag done=new ListTag();slotCompleted.stream().sorted().forEach(key->done.add(StringTag.valueOf(key)));n.put("SlotCompleted",done);
        CompoundTag rd=new CompoundTag(),rr=new CompoundTag();
        runeDrafts.forEach((l,v)->rd.putIntArray(String.valueOf(l),v)); runeReferences.forEach(rr::putIntArray);
        n.putInt("RuneLayoutVersion",RuneLayout.VERSION);
        n.put("RuneDrafts",rd);n.put("RuneReferences",rr);ListTag rc=new ListTag();runeCompleted.stream().sorted().forEach(k->rc.add(StringTag.valueOf(k)));n.put("RuneCompleted",rc);
        n.putUUID("CompletionToken",completionToken);return n;
    }
    public void read(CompoundTag n) {
        rank = Math.max(0, Math.min(5, n.getInt("Rank"))); knownGlyphs = n.getInt("Glyphs") & 0x3ffff;
        attuned = n.getInt("Attuned") & 0x7ff; knownRules = n.getInt("Rules") & 0x7ff; migrated = n.getBoolean("Migrated");
        world = n.hasUUID("World") ? n.getUUID("World") : null; legacy.clear(); drawn.clear(); references.clear();
        var old = n.getList("Legacy", Tag.TAG_STRING); for (int i = 0; i < Math.min(55, old.size()); i++) legacy.add(old.getString(i));
        var completed = n.getList("Drawn", Tag.TAG_STRING); for (int i = 0; i < Math.min(55, completed.size()); i++) drawn.add(completed.getString(i));
        var refs = n.getCompound("References");
        for (int f = 0; f < 11; f++) for (int l = 1; l <= 5; l++) { var t = new ResearchTarget(f, l); if (refs.contains(t.id())) {
            references.put(t.id(), refs.getCompound(t.id()).copy());
        } }
        draft = n.getCompound("Draft").copy();spellDraft = n.getCompound("SpellDraft").copy();
        spellTarget = n.getString("SpellTarget");if(spellTarget.length()>128)spellTarget="";
        slotDrafts.clear();slotReferences.clear();slotCompleted.clear();
        for(int level=1;level<=5;level++){int[] values=n.getCompound("SlotDrafts").getIntArray(String.valueOf(level));if(SlottedFormation.validDraft(level,values))slotDrafts.put(level,values);}
        var patterns=n.getCompound("SlotReferences");var done=n.getList("SlotCompleted",Tag.TAG_STRING);
        for(var recipe:SlottedSpellRecipes.all())for(int level=1;level<=5;level++){
            String key=recipe.spell()+":"+level;int[] values=patterns.getIntArray(key);
            if(SlottedFormation.validDraft(level,values))slotReferences.put(key,values);
            for(int index=0;index<Math.min(128,done.size());index++)if(done.getString(index).equals(key))slotCompleted.add(key);
        }
        runeDrafts.clear();runeReferences.clear();runeCompleted.clear();
        int runeVersion=n.contains("RuneLayoutVersion")?n.getInt("RuneLayoutVersion"):RuneLayout.PREVIOUS_VERSION;
        for(int l=1;l<=5;l++){int[] v=RuneLayout.migrateDraft(l,n.getCompound("RuneDrafts").getIntArray(String.valueOf(l)),runeVersion);if(RuneLayout.validDraft(l,v))runeDrafts.put(l,v);}
        var rr=n.getCompound("RuneReferences");var rc=n.getList("RuneCompleted",Tag.TAG_STRING);
        for(var recipe:SlottedSpellRecipes.all())for(int l=1;l<=5;l++){String k=recipe.spell()+":"+l;int[] v=RuneLayout.migrateDraft(l,rr.getIntArray(k),runeVersion);if(RuneLayout.validDraft(l,v))runeReferences.put(k,v);for(int i=0;i<Math.min(128,rc.size());i++)if(k.equals(rc.getString(i)))runeCompleted.add(k);}
        completionToken=n.hasUUID("CompletionToken")?n.getUUID("CompletionToken"):UUID.randomUUID();
    }
}
