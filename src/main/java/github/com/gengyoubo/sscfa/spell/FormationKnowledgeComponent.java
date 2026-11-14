package github.com.gengyoubo.sscfa.spell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 鐜╁娉曟湳鐭ヨ瘑鏁版嵁缁勪欢锛坖ackcooper锛夛細璁板綍銆屽凡璁板綍鐨勬硶闃点€嶄笌銆屽凡瀛︿範鐨勬硶闃点€嶃€?
 *
 * <p>涓ょ骇鏁版嵁锛堥兘璺熼殢鐜╁銆佸浜哄悇鑷嫭绔嬶級锛?/p>
 * <ul>
 *   <li><b>宸茶褰曪紙recorded锛?/b>锛氬彸閿娇鐢ㄦ硶闃电墿鍝佽幏寰楋紝鏍煎紡 {@code element[/variant]:level}锛?
 *       鍚岀郴鍚勭瓑绾х嫭绔嬭褰曪紙楂樼瓑绾т笉瑕嗙洊浣庣瓑绾э紝瀛︿範鏃惰嚜閫夌瓑绾э級锛涢€氱敤绯讳笁鍙樹綋
 *       锛坮egen/mana/exp锛夊悇鑷嫭绔嬭褰曪紱</li>
 *   <li><b>宸插涔狅紙learned锛?/b>锛氱爺绌跺彴娑堣€楁湀灏樺涔犲悗鑾峰緱锛屽彧瀛樻渶楂樼瓑绾э紙浣庣瓑绾ц嚜鍔ㄥ寘鍚級锛?
 *       瀛︿範鍚庢墠鑳藉湪鐮旂┒鍙版妱鍐欏搴旂瓑绾х殑娉曢樀銆傚瓨鍌ㄩ敭 {@code element[/variant]}銆?/li>
 * </ul>
 *
 * <p><b>鏃ф。杩佺Щ锛?026-09-15 鍙樹綋鎷嗗垎锛?/b>锛氭棫璁板綍閿?{@code universal:N} 褰掍负鍥炶兘鍙樹綋
 * {@code universal/regen:N}锛涙棫瀛︿範閿?{@code learned_universal} 褰掍负
 * {@code learned_universal/regen}鈥斺€旀棫閫氱敤娉曢樀鐗╁搧缂虹渷鍙樹綋鍗冲洖鑳斤紝璇诲啓涓€鑷淬€?/p>
 */
public class FormationKnowledgeComponent {
/** 宸茶褰曠殑娉曢樀锛坋lement[/variant]:level 闆嗗悎锛屽 ice:3銆乽niversal/regen:2锛夈€?*/
private final Set<String> recorded = new HashSet<>();
/** 宸插涔犵殑娉曢樀 鈫?鏈€楂樼瓑绾э紙0 = 鏈涔狅級銆傞敭锛歟lement id 鎴?universal/<variant>銆?*/
private final Map<String, Integer> learned = new HashMap<>();
/**
 * 宸茶褰曠殑娉曟湳鍥捐氨锛堥樁娈?C / 璁″垝涔?搂8.1锛氭硶鏈嵎杞村彸閿褰曪紝瑙ｉ攣瀹氬悜鍒朵綔锛涗笉娑堣€楀嵎杞达級銆?
 * 閿細娉曟湳 id path锛堝 fire_bolt锛夈€傝褰曠煡璇嗕笉鑳藉厤璐瑰鍒跺疄浣撳嵎杞粹€斺€斿畾鍚戝埗浣滀粛闇€鏉愭枡銆?
 */
private final Set<String> spellAtlas = new HashSet<>();
private final github.com.gengyoubo.sscfa.spell.research.ResearchProgress research =
        new github.com.gengyoubo.sscfa.spell.research.ResearchProgress();

public github.com.gengyoubo.sscfa.spell.research.ResearchProgress research() {
    ensureResearchMigration();
    return research;
}

private void ensureResearchMigration() {
    if (research.migrated) return;
    research.legacy.addAll(recorded);
    for (int f = 0; f < 11; f++) {
        String id = github.com.gengyoubo.sscfa.spell.research.ResearchTarget.FAMILIES.get(f);
        int level = learned.getOrDefault(id, 0);
        if (level > 0) { research.rank = Math.max(research.rank, level); research.attuned |= 1 << f; }
    }
    research.migrated = true;
}

public static final ResourceLocation SYNC = new ResourceLocation("ssc_addon", "formation_knowledge_sync");
private static final Map<java.util.UUID, FormationKnowledgeComponent> CLIENT_STORE = new java.util.concurrent.ConcurrentHashMap<>();
public static FormationKnowledgeComponent get(Player player) {
if (!player.level().isClientSide() && player.getServer() != null) {
    return FormationKnowledgeSavedData.get(player.getServer()).getOrCreate(player.getUUID());
}
return CLIENT_STORE.computeIfAbsent(player.getUUID(), ignored -> new FormationKnowledgeComponent());
}

public static void acceptClient(java.util.UUID id, CompoundTag tag) {
    FormationKnowledgeComponent component = new FormationKnowledgeComponent();
    component.readFromNbt(tag);
    CLIENT_STORE.put(id, component);
}

// ---- 瀛樺偍閿?----

/** 瀛︿範绛夌骇瀛樺偍閿細闈為€氱敤绯?= element id锛涢€氱敤绯?= universal/<variant>锛堥潪娉曞彉浣撳綊 regen锛夈€?*/
private static String storageKey(FormationElement element, String variant) {
if (element == FormationElement.UNIVERSAL) {
String v = FormationData.normalizeVariant(variant);
return element.id + "/" + (v != null ? v : FormationData.VARIANT_REGEN);
}
return element.id;
}

/** 璁板綍鏉＄洰閿細element[/variant]:level銆?*/
private static String key(FormationElement element, String variant, int level) {
return storageKey(element, variant) + ":" + level;
}

// ---- 宸茶褰?----

/** 鏄惁宸茶褰曟寚瀹氱郴鍒紙+鍙樹綋锛? 绛夌骇鐨勬硶闃点€倂ariant 浠呴€氱敤绯绘湁鏁堬紝浼?null 褰?regen銆?*/
public boolean hasRecorded(FormationElement element, String variant, int level) {
return recorded.contains(key(element, variant, level));
}

/** 璁板綍鎸囧畾娉曢樀锛堝箓绛夛級銆倂ariant 浠呴€氱敤绯绘湁鏁堬紝浼?null 褰?regen銆?*/
public void record(FormationElement element, String variant, int level) {
recorded.add(key(element, variant, level));
}

// ---- 宸插涔?----

/** 鎸囧畾绯诲埆锛?鍙樹綋锛夊凡瀛︿範鍒扮殑鏈€楂樼瓑绾э紙0 = 鏈涔狅級銆倂ariant 浠呴€氱敤绯绘湁鏁堛€?*/
public int getLearnedLevel(FormationElement element, String variant) {
return learned.getOrDefault(storageKey(element, variant), 0);
}

/** 鏄惁宸插涔犳寚瀹氱郴鍒紙+鍙樹綋锛夎嚦灏戝埌鎸囧畾绛夌骇銆?*/
public boolean hasLearned(FormationElement element, String variant, int level) {
return getLearnedLevel(element, variant) >= level;
}

/** 瀛︿範鎸囧畾绯诲埆锛?鍙樹綋锛夊埌鎸囧畾绛夌骇锛堝彧鍗囦笉闄嶏級銆?*/
public void learn(FormationElement element, String variant, int level) {
String k = storageKey(element, variant);
if (level > learned.getOrDefault(k, 0)) {
learned.put(k, level);
}
}

// ---- 娉曟湳鍥捐氨锛堥樁娈?C / 璁″垝涔?搂8.1锛?----

/** 鏄惁宸茶褰曟煇娉曟湳鍥捐氨锛堥敭锛氭硶鏈?id path锛夈€?*/
public boolean hasSpell(String spellPath) {
return spellAtlas.contains(spellPath);
}

/** 璁板綍娉曟湳鍥捐氨锛堝箓绛夛級銆?*/
public void recordSpell(String spellPath) {
spellAtlas.add(spellPath);
}

/** 宸茶褰曠殑娉曟湳鍥捐氨鏁伴噺銆?*/
public int spellAtlasSize() {
return spellAtlas.size();
}

// ---- 鎸佷箙鍖?/ 鍚屾 ----
public void readFromNbt(CompoundTag nbt) {
recorded.clear();
ListTag list = nbt.getList("recorded", Tag.TAG_STRING);
for (int i = 0; i < list.size(); i++) {
String entry = list.getString(i);
// 鏃ф。杩佺Щ锛歶niversal:N 鈫?universal/regen:N锛堟棫閫氱敤娉曢樀涓€寰嬭涓哄洖鑳藉彉浣擄級
if (entry.startsWith("universal:")) {
entry = "universal/" + FormationData.VARIANT_REGEN + ":" + entry.substring("universal:".length());
}
recorded.add(entry);
}// 娉曟湳鍥捐氨锛堥樁娈?C锛?
spellAtlas.clear();
ListTag atlas = nbt.getList("spell_atlas", Tag.TAG_STRING);
for (int i = 0; i < atlas.size(); i++) {
spellAtlas.add(atlas.getString(i));
}
learned.clear();
for (FormationElement element : FormationElement.values()) {
if (element == FormationElement.UNIVERSAL) {
// 閫氱敤绯讳笁鍙樹綋鐙珛璇诲彇
for (String variant : new String[]{FormationData.VARIANT_REGEN, FormationData.VARIANT_MANA, FormationData.VARIANT_EXP, FormationData.VARIANT_RECOVERY}) {
String nbtKey = "learned_" + storageKey(element, variant);
if (nbt.contains(nbtKey)) {
int lv = clampLevel(nbt.getInt(nbtKey));
if (lv > 0) {
learned.put(storageKey(element, variant), lv);
}
}
}
// 鏃ч敭杩佺Щ锛歭earned_universal 鈫?learned_universal/regen锛堜粎褰撴柊閿笉瀛樺湪鏃讹級
String regenKey = "learned_" + storageKey(element, FormationData.VARIANT_REGEN);
if (!nbt.contains(regenKey) && nbt.contains("learned_universal")) {
int legacy = clampLevel(nbt.getInt("learned_universal"));
if (legacy > 0) {
learned.put(storageKey(element, FormationData.VARIANT_REGEN), legacy);
}
}
} else {
int lv = clampLevel(nbt.getInt("learned_" + element.id));
if (lv > 0) {
learned.put(element.id, lv);
}
}
}
research.read(nbt.getCompound("RuneResearch"));
ensureResearchMigration();
}
public void writeToNbt(CompoundTag nbt) {
ListTag list = new ListTag();
for (String entry : recorded) {
list.add(StringTag.valueOf(entry));
}
nbt.put("recorded", list);
for (Map.Entry<String, Integer> e : learned.entrySet()) {
nbt.putInt("learned_" + e.getKey(), e.getValue());
}
ListTag atlas = new ListTag();
for (String entry : spellAtlas) {
atlas.add(StringTag.valueOf(entry));
}
nbt.put("spell_atlas", atlas);
nbt.put("RuneResearch", research().write());
}

/** 鏈嶅姟绔彉鏇村悗鍚屾缁欏鎴风锛堢爺绌跺彴 GUI 闇€瑕佸疄鏃惰锛夈€?*/
public static void sync(ServerPlayer player) {
    var data = FormationKnowledgeSavedData.get(player.getServer());
    FormationKnowledgeComponent component = data.getOrCreate(player.getUUID());
    data.setDirty();
    if (!github.com.gengyoubo.sscfa.network.AddonNetwork.canSend(player, SYNC)) return;
    CompoundTag tag = new CompoundTag();
    component.writeToNbt(tag);
    var buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
    buf.writeUUID(player.getUUID());
    buf.writeNbt(tag);
    github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, SYNC, buf);
}

private static int clampLevel(int lv) {
return Math.max(0, Math.min(FormationData.MAX_FORMATION_LEVEL, lv));
}
}
