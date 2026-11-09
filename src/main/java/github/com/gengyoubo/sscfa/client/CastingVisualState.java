package github.com.gengyoubo.sscfa.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 施法视觉状态客户端镜像（Forge 版）：接收服务端 cast_visual / suppression 包，维护「谁在施法」表。 */
public final class CastingVisualState {
	private static final int EXPIRE_TICKS = 45;
	public static final float RAISE_TICKS = 5.0F;

	private static final Map<UUID, Entry> ACTIVE = new HashMap<>();
	private static final Map<UUID, Cancellation> CANCELLATIONS = new HashMap<>();
	public record Cancellation(Vec3 position, int color, long started) {}
	public record Suppression(ResourceLocation dimension, UUID caster, Vec3 position, int color, UUID explosionSequence) {}

	public static java.util.Collection<Cancellation> cancellations() { return CANCELLATIONS.values(); }

	public static Suppression readSuppression(FriendlyByteBuf buf) {
		var dimension = buf.readResourceLocation();
		var caster = buf.readUUID();
		var position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		int color = buf.readInt();
		return new Suppression(dimension, caster, position, color, buf.readBoolean() ? buf.readUUID() : null);
	}

	private static ClientLevel visualWorld;

	private CastingVisualState() {}

	private static final class Entry {
		final long receivedAt;
		long expiresAt;
		boolean active = true;
		boolean armPose;
		int schoolColor;
		int rarityColor;
		boolean explosion;
		boolean deathFinale;
		Entry(long receivedAt, long expiresAt) { this.receivedAt = receivedAt; this.expiresAt = expiresAt; }
	}

	public static void register() {
		AddonNetwork.registerClientReceiver(SpellChannelManager.SUPPRESSION, buf -> {
			Suppression failure = readSuppression(buf);
			Minecraft client = Minecraft.getInstance();
			if (client.level == null || !client.level.dimension().location().equals(failure.dimension())) return;
			checkWorld(client);
			long now = client.level.getGameTime();
			Entry old = ACTIVE.remove(failure.caster());
			if (old != null && !old.explosion && !old.deathFinale)
				CANCELLATIONS.put(failure.caster(), new Cancellation(failure.position(), failure.color(), now));
			if (failure.explosionSequence() != null)
				github.com.gengyoubo.sscfa.client.renderer.ExplosionRenderer.interruptForRange(failure.explosionSequence(), client.level, now);
			github.com.gengyoubo.sscfa.client.sound.SpellChargeSoundInstance.stopFor(failure.caster());
			ExplosionEffects.playSuppressionCues(failure.position());
		});
		AddonNetwork.registerClientReceiver(SpellChannelManager.CAST_VISUAL, buf -> {
			UUID uuid = buf.readUUID();
			boolean active = buf.readBoolean();
			boolean armPose = active && buf.readBoolean();
			int schoolColor = active ? buf.readInt() : 0;
			int rarityColor = active ? buf.readInt() : 0;
			ResourceLocation spellId = active ? buf.readResourceLocation() : null;
			boolean explosion = spellId != null && spellId.equals(new ResourceLocation("ssc_addon", "explosion"));
			boolean deathFinale = spellId != null && spellId.equals(new ResourceLocation("ssc_addon", "death_finale"));
			Minecraft client = Minecraft.getInstance();
			checkWorld(client);
			if (client.level == null) return;
			long now = client.level.getGameTime();
			if (active) {
				Entry old = ACTIVE.get(uuid);
				if (old != null && old.active && now <= old.expiresAt) {
					old.expiresAt = now + EXPIRE_TICKS;
				} else {
					old = new Entry(now, now + EXPIRE_TICKS);
					ACTIVE.put(uuid, old);
				}
				old.armPose = armPose;
				old.schoolColor = schoolColor;
				old.rarityColor = rarityColor;
				old.explosion = explosion;
				old.deathFinale = deathFinale;
			} else {
				Entry old = ACTIVE.get(uuid);
				if (old != null && old.active) {
					old.active = false;
					old.expiresAt = now + 6;
				}
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
			ACTIVE.clear();
			CANCELLATIONS.clear();
			visualWorld = null;
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
		});
		MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
				github.com.gengyoubo.sscfa.client.renderer.CastingCircleRenderer.render(event);
				Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
			}
		});
	}

	private static void checkWorld(Minecraft client) {
		if (visualWorld != client.level) {
			ACTIVE.clear();
			CANCELLATIONS.clear();
			visualWorld = client.level;
		}
	}

	private static void tick(Minecraft client) {
		checkWorld(client);
		if (client.level == null) return;
		long now = client.level.getGameTime();
		ACTIVE.values().removeIf(entry -> now > entry.expiresAt);
		if (!CANCELLATIONS.isEmpty()) CANCELLATIONS.values().removeIf(entry -> now - entry.started() > github.com.gengyoubo.sscfa.spell.ExplosionRules.AFTER_GLOW_TICKS);
		if (now % 2 != 0) return;
		for (var player : client.level.players()) {
			Entry entry = ACTIVE.get(player.getUUID());
			if (entry == null || !entry.active || !entry.armPose || !player.isAlive()
					|| player.isSpectator() || player.isInvisible()) continue;
			if (github.com.gengyoubo.sscfa.client.renderer.DomainRenderer.isCharging(player.getUUID())) continue;
			if (client.getCameraEntity() == null || player.distanceToSqr(client.getCameraEntity()) > 32 * 32) continue;
			var random = client.level.random;
			for (int count = 0; count < 2; count++) {
				var particle = client.particleEngine.createParticle(ParticleTypes.END_ROD,
						player.getX() + (random.nextDouble() - 0.5) * 0.65,
						player.getY() + player.getBbHeight() + 0.12 + random.nextDouble() * 0.12,
						player.getZ() + (random.nextDouble() - 0.5) * 0.65, 0, 0.04, 0);
				if (particle == null) continue;
				if (particle instanceof github.com.gengyoubo.sscfa.client.particle.OwnedDecoration decoration) {
					decoration.ssca$setDecorationOwner(player.getUUID());
				}
				particle.setColor(((entry.schoolColor >> 16) & 255) / 255F,
						((entry.schoolColor >> 8) & 255) / 255F, (entry.schoolColor & 255) / 255F);
				if (particle instanceof net.minecraft.client.particle.EndRodParticle endRod) {
					endRod.setFadeColor(entry.schoolColor);
				}
				particle.setParticleSpeed(0, 0.035 + random.nextDouble() * 0.02, 0);
				particle.setLifetime(20);
				particle.scale(1.2F);
			}
		}
	}

	private static Entry entryOf(UUID uuid) {
		Entry entry = ACTIVE.get(uuid);
		if (entry == null) return null;
		Minecraft client = Minecraft.getInstance();
		if (client.level != visualWorld) return null;
		long now = client.level == null ? 0 : client.level.getGameTime();
		if (now > entry.expiresAt) { ACTIVE.remove(uuid); return null; }
		return entry;
	}

	public static boolean isCasting(UUID uuid) {
		Entry entry = entryOf(uuid);
		return entry != null && entry.active && entry.armPose;
	}
	public static boolean hasCircle(UUID uuid) { return entryOf(uuid) != null; }
	public static boolean isExplosion(UUID uuid) {
		Entry entry = entryOf(uuid);
		return entry != null && entry.explosion;
	}
	public static boolean isDeathFinale(UUID uuid) {
		Entry entry = entryOf(uuid);
		return entry != null && entry.deathFinale;
	}
	public static int circleColor(UUID uuid) {
		Entry entry = entryOf(uuid);
		return entry == null ? 0xFFFFFF : entry.rarityColor;
	}
	public static float circleAlpha(UUID uuid) {
		Entry entry = entryOf(uuid);
		if (entry == null) return 0;
		if (entry.active) return 1;
		long now = Minecraft.getInstance().level.getGameTime();
		return Mth.clamp((entry.expiresAt - now) / 6F, 0, 1);
	}
	public static float raiseProgress(UUID uuid) {
		Entry entry = entryOf(uuid);
		if (entry == null) return 0.0F;
		Minecraft client = Minecraft.getInstance();
		long now = client.level == null ? entry.receivedAt : client.level.getGameTime();
		return Mth.clamp((now - entry.receivedAt) / RAISE_TICKS, 0.0F, 1.0F);
	}
	public static float armSway(float age, boolean right) {
		return Mth.sin(age * 0.18F) * (right ? 1.0F : -1.0F);
	}
	public static boolean localPlayerCasting() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && isCasting(player.getUUID());
	}
}
