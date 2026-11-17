package github.com.gengyoubo.sscfa.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.ability.AnubisWolfSpDeathDomain;
import github.com.gengyoubo.sscfa.ability.ErosionBrandClientState;
import github.com.gengyoubo.sscfa.ability.GoldenSandstormErosionBrand;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkClientState;
import github.com.gengyoubo.sscfa.ability.MancianimaMarkManager;
import github.com.gengyoubo.sscfa.compat.ManaUtils;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.spell.UniversalFormationManager;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.onixary.shapeShifterCurseForge.api.registry.SscCondition;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;

/**
 * The addon's custom JSON condition types, registered with SSC Forge by their {@code "type"} id.
 *
 * <p>Port of the Fabric {@code SscAddonConditions}. SSC Forge evaluates unknown condition types as
 * false, so before this port every power gated on one of these (for example the 64 talent-gated
 * powers using {@code ssc_addon:has_talent}) was permanently off. SSC Forge uses one call for entity
 * and bi-entity conditions: {@code actor} is the power holder, {@code target} the other entity or
 * {@code null}. SSC applies {@code inverted} itself.</p>
 */
public final class SscAddonConditions {
	private static boolean registered;

	private SscAddonConditions() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		entity("ssc_addon", "not_universal_formation_charging", (player, data) ->
				!(player instanceof ServerPlayer sp) || !UniversalFormationManager.isCharging(sp));
		entity("ssc_addon", "has_reverse_thermometer", (player, data) -> {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("shape-shifter-curse", "charm_of_reverse_thermometer"));
			return item != null && TrinketUtils.isWearing(player, item);
		});
		entity("ssc_addon", "has_trinket", (player, data) -> {
			Item item = itemOf(data, "item");
			return item != null && TrinketUtils.isWearing(player, item);
		});
		entity("ssc_addon", "item_on_cooldown", (player, data) -> {
			Item item = itemOf(data, "item");
			return item != null && player.getCooldowns().isOnCooldown(item);
		});
		entity("ssc_addon", "has_blue_fire_amulet", (player, data) ->
				TrinketUtils.isWearing(player, AddonItems.BLUE_FIRE_AMULET.get()));
		entity("ssc_addon", "has_mana_percent_safe", (player, data) -> {
			double max = ManaUtils.getPlayerMaxMana(player);
			if (max <= 0) return false;
			return compare(ManaUtils.getPlayerMana(player) / max, stringOf(data, "comparison", ">="),
					doubleOf(data, "mana_percent", 0));
		});

		register("my_addon", "not_actor_whitelisted", (actor, target, data) ->
				!(actor instanceof ServerPlayer player) || !(target instanceof LivingEntity living)
						|| !WhitelistUtils.isProtected(player, living));
		// Nightmare "dream" gate for JSON-applied effects: false when the actor (the one applying the
		// effect) is dreamed by the target. Same rule as the server-side addEffect interceptor.
		register("my_addon", "not_dream_blocked", (actor, target, data) ->
				!(target instanceof LivingEntity living) || actor.level().isClientSide()
						|| !github.com.gengyoubo.sscfa.ability.NightmareDreamManager.isBlocked(actor, living));

		// Erosion brand colour (yellow / orange / red / green) for entity_glow: the server reads the
		// brand table, the client its synced cache.
		register("ssc_addon", "erosion_brand_state", (actor, target, data) -> {
			if (target == null) return false;
			String color = stringOf(data, "color", "");
			if (actor instanceof ServerPlayer) return GoldenSandstormErosionBrand.hasColor(actor.getUUID(), target.getUUID(), color);
			return actor.level().isClientSide() && ErosionBrandClientState.hasColor(target.getUUID(), color);
		});
		// Mancianima mark colour (yellow / orange / red) for entity_glow.
		register("my_addon", "mancianima_mark_color", (actor, target, data) -> {
			if (target == null) return false;
			String color = stringOf(data, "color", "");
			if (actor instanceof ServerPlayer) {
				String c = MancianimaMarkManager.getColorString(actor.getUUID(), target.getUUID());
				return c != null && c.equals(color);
			}
			return actor.level().isClientSide() && MancianimaMarkClientState.hasColor(target.getUUID(), color);
		});
		// Mancianima crosshair target (client only): the green highlight override.
		register("my_addon", "mancianima_crosshair_target", (actor, target, data) -> {
			if (target == null || !actor.level().isClientSide()) return false;
			try {
				return github.com.gengyoubo.sscfa.client.MancianimaCrosshairTracker.isCurrent(target.getUUID());
			} catch (Throwable t) {
				return false;
			}
		});

		// True while the skill is NOT blocked; action_over_time powers carry it so they stop when disabled.
		entity("ssc_addon", "skill_disabled", (player, data) ->
				!(player instanceof ServerPlayer sp)
						|| !SkillBlocker.isSkillBlocked(sp, stringOf(data, "form", ""), stringOf(data, "skill", "")));
		entity("ssc_addon", "not_empowered", (player, data) -> {
			int state = PowerUtils.getResourceValue(player, FormIdentifiers.EMPOWER_STATE);
			return github.com.gengyoubo.sscfa.ability.KillEmpowerState.allowsNormalSkill(state, boolOf(data, "primary", false));
		});
		entity("ssc_addon", "in_own_death_domain", (player, data) ->
				player instanceof ServerPlayer && AnubisWolfSpDeathDomain.isInActiveDomain(player.getUUID(), player.blockPosition()));
		// Evolution talent node unlocked: talent-gated powers stay off until the node is bought.
		entity("ssc_addon", "has_talent", (player, data) -> {
			String talent = stringOf(data, "talent_id", null);
			return talent != null && RegEvolutionComponent.EVOLUTION.get(player).isUnlocked(talent);
		});
		// Skill neither casting nor cooling down; server-authoritative, always true on the client.
		entity("my_addon", "skill_ready", (player, data) -> {
			String power = stringOf(data, "power", null);
			return !(player instanceof ServerPlayer sp) || power == null
					|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(sp, power);
		});
	}

	@FunctionalInterface
	private interface EntityCondition {
		boolean test(Player player, JsonObject data);
	}

	/**
	 * Entity condition on the power holder. SSC Forge evaluates entity conditions on the actor, and
	 * passes the other entity as {@code target} inside {@code apoli:actor_condition} too, so the
	 * target is ignored here. (The addon's data uses these only at top level and in actor_condition.)
	 */
	private static void entity(String namespace, String path, EntityCondition condition) {
		register(namespace, path, (actor, target, data) -> actor != null && condition.test(actor, data));
	}

	private static void register(String namespace, String path, SscCondition condition) {
		SscJavaRegistries.registerCondition(new ResourceLocation(namespace, path), condition);
	}

	/** Apoli comparison strings. */
	static boolean compare(double value, String comparison, double compareTo) {
		return switch (comparison) {
			case ">" -> value > compareTo;
			case ">=" -> value >= compareTo;
			case "<" -> value < compareTo;
			case "<=" -> value <= compareTo;
			case "==" -> value == compareTo;
			case "!=" -> value != compareTo;
			default -> false;
		};
	}

	private static String stringOf(JsonObject data, String key, String fallback) {
		JsonElement e = data.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
	}

	private static double doubleOf(JsonObject data, String key, double fallback) {
		JsonElement e = data.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsDouble() : fallback;
	}

	private static boolean boolOf(JsonObject data, String key, boolean fallback) {
		JsonElement e = data.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : fallback;
	}

	private static Item itemOf(JsonObject data, String key) {
		JsonElement e = data.get(key);
		if (e != null && e.isJsonObject()) e = e.getAsJsonObject().get("item");
		if (e == null || !e.isJsonPrimitive()) return null;
		ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
		return id == null || !ForgeRegistries.ITEMS.containsKey(id) ? null : ForgeRegistries.ITEMS.getValue(id);
	}
}
