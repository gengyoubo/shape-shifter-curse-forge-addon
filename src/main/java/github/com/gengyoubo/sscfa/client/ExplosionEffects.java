package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.init.AddonSounds;
import github.com.gengyoubo.sscfa.spell.ExplosionRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 一个服务端授权的爆裂序列在本地的粒子与 21 秒主题音频（移植自 Fabric 版）。
 * 主题音频是唯一音源（3 秒渐入，第 8 秒爆炸），音量按距离曲线（128 格内满，至 328 格线性降到 0）乘渐入。
 * 爆炸粒子分 6 帧发放以避免单帧尖峰；蓄力早期在爆心地下预热爆炸专用粒子，避免光影着色器在爆炸那一帧即时编译。
 */
public final class ExplosionEffects {
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1, 0.12f, 0.08f), 2.5f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1, 0.95f, 0.9f), 2.5f);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.55f, 0.12f, 0.85f), 2.5f);
    private static final int BLAST_BATCHES = 6;
    private static final int SPHERE_PARTICLES = 1024;

    private boolean exploded;
    private boolean interrupted;
    private ThemeSound theme;
    private int pendingBlastBatches;

    private static double coreRadius(Double packetCore) {
        return packetCore != null && packetCore > 0 ? packetCore : ExplosionRules.coreRadius();
    }

    public void tick(ClientLevel world, Vec3 center, int elapsed, boolean detonated, Double packetCoreRadius, Double packetOuterRadius) {
        if (interrupted) return;
        // 音频受众更大；几何与粒子仍只在原有可视范围内。
        if (!detonated && elapsed >= ExplosionRules.SOUND_START_TICKS && theme == null) {
            theme = new ThemeSound(world, center);
            Minecraft.getInstance().getSoundManager().play(theme);
        }
        var listener = Minecraft.getInstance().player;
        if (listener == null || !ExplosionRules.visualsInRange(listener.distanceToSqr(center))) {
            if (detonated) exploded = true;
            pendingBlastBatches = 0;
            return;
        }
        if (detonated) {
            if (!exploded) {
                exploded = true;
                particle(ParticleTypes.EXPLOSION_EMITTER, center, Vec3.ZERO, 8);
                pendingBlastBatches = BLAST_BATCHES;
                spawnBlastBatch(world, center, packetCoreRadius);
            } else {
                if (pendingBlastBatches > 0) spawnBlastBatch(world, center, packetCoreRadius);
                sparks(world, center, 12, packetCoreRadius);
            }
            return;
        }
        if (elapsed >= 40 && elapsed <= 200 && elapsed % 20 == 0) {
            Vec3 hidden = center.add(0, -8, 0);
            particle(ParticleTypes.EXPLOSION_EMITTER, hidden, Vec3.ZERO, 1);
            particle(ParticleTypes.CAMPFIRE_COSY_SMOKE, hidden, Vec3.ZERO, 1);
            particle(ParticleTypes.FLAME, hidden, Vec3.ZERO, 1);
            particle(ParticleTypes.LARGE_SMOKE, hidden, Vec3.ZERO, 1);
        }
        if (elapsed >= ExplosionRules.SOUND_START_TICKS) warnRing(world, center, elapsed, packetCoreRadius);
        double height = ExplosionRules.beamHeight(elapsed);
        for (int i = 0; i < 6 && height > 0; i++) {
            particle(RED, center.add((world.random.nextDouble() - 0.5) * 0.5,
                    world.random.nextDouble() * height, (world.random.nextDouble() - 0.5) * 0.5), Vec3.ZERO, 14);
        }
    }

    /** 核心伤害半径上的红/黑/紫警示圈，每 2 tick 一圈并缓慢旋转。 */
    private static void warnRing(ClientLevel world, Vec3 center, int elapsed, Double packetCore) {
        if (world.getGameTime() % 2 != 0) return;
        double radius = coreRadius(packetCore);
        int points = 120;
        double offset = (elapsed - ExplosionRules.SOUND_START_TICKS) * 0.02;
        for (int i = 0; i < points; i++) {
            double angle = offset + i * 2 * Math.PI / points;
            Vec3 pos = center.add(Math.cos(angle) * radius, 0.25, Math.sin(angle) * radius);
            Vec3 drift = new Vec3(-Math.sin(angle) * 0.05, 0.04, Math.cos(angle) * 0.05);
            switch (i % 3) {
                case 0 -> particle(RED, pos, drift, 40);
                case 1 -> particle(ParticleTypes.LARGE_SMOKE, pos, drift.scale(0.6), 36);
                default -> particle(PURPLE, pos, drift, 40);
            }
        }
    }

    /** 按全局下标整数区间分帧：每个粒子的方向/颜色/半径仍由下标决定，只改变提交时机。 */
    private void spawnBlastBatch(ClientLevel world, Vec3 center, Double packetCore) {
        int batch = BLAST_BATCHES - pendingBlastBatches;
        pendingBlastBatches--;
        for (int i = batch * 18 / BLAST_BATCHES; i < (batch + 1) * 18 / BLAST_BATCHES; i++) {
            boolean outer = i >= 6;
            int n = outer ? i - 6 : i;
            double angle = outer ? n * Math.PI / 6 : n * Math.PI / 3 + 0.4;
            double ringRadius = outer ? 18 : 8;
            particle(ParticleTypes.EXPLOSION_EMITTER,
                    center.add(Math.cos(angle) * ringRadius,
                            world.random.nextDouble() * (outer ? 16 : 6) - (outer ? 8 : 2),
                            Math.sin(angle) * ringRadius), Vec3.ZERO, 8);
        }
        final double burstRadius = 28.0;
        int from = batch * SPHERE_PARTICLES / BLAST_BATCHES;
        int to = (batch + 1) * SPHERE_PARTICLES / BLAST_BATCHES;
        for (int i = from; i < to; i++) {
            double y = 1 - 2 * (i + 0.5) / (double) SPHERE_PARTICLES;
            double radial = Math.sqrt(1 - y * y);
            double angle = i * Math.PI * (3 - Math.sqrt(5));
            Vec3 direction = new Vec3(radial * Math.cos(angle), y, radial * Math.sin(angle));
            double radius = i % 3 == 0 ? burstRadius : burstRadius * Math.cbrt(world.random.nextDouble());
            particle(i % 2 == 0 ? RED : WHITE, center.add(direction.scale(radius)), direction.scale(0.25), 30);
            if (i % 8 == 0) particle(ParticleTypes.FLAME, center.add(direction.scale(radius)), direction.scale(0.5), 24);
        }
        sparks(world, center, 40, packetCore);
        for (int i = 0; i < 8; i++) {
            particle(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.add((world.random.nextDouble() - 0.5) * 28,
                    world.random.nextDouble() * 20, (world.random.nextDouble() - 0.5) * 28), new Vec3(0, 0.3, 0), 45);
        }
    }

    private static void sparks(ClientLevel world, Vec3 center, int count, Double packetCore) {
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = world.random.nextDouble() * coreRadius(packetCore);
            Vec3 pos = center.add(Math.cos(angle) * distance, world.random.nextDouble() * 2, Math.sin(angle) * distance);
            Vec3 velocity = new Vec3((world.random.nextDouble() - 0.5) * 0.4,
                    0.2 + world.random.nextDouble() * 0.5, (world.random.nextDouble() - 0.5) * 0.4);
            particle(ParticleTypes.FLAME, pos, velocity, 30);
        }
    }

    private static void particle(ParticleOptions effect, Vec3 position, Vec3 velocity, int life) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(effect,
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        if (particle != null) particle.setLifetime(life);
    }

    public void stop() {
        if (theme != null) Minecraft.getInstance().getSoundManager().stop(theme);
        theme = null;
    }

    /** 先停主题音频，再按需播放一对本地取消提示音；不再发放爆炸粒子。 */
    public void interrupt(Vec3 center, boolean playCue) {
        if (interrupted) return;
        interrupted = true;
        pendingBlastBatches = 0;
        stop();
        if (playCue) playSuppressionCues(center);
    }

    /**
     * 播放一对取消提示音（玻璃碎裂 + 熄火）。音量沿用爆裂的受众距离曲线；以听者相对方式播放，
     * 领域的世界声屏障不会吞掉这条通知。
     */
    public static void playSuppressionCues(Vec3 position) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || position == null) return;
        float volume = ExplosionRules.soundVolume(Math.sqrt(client.player.distanceToSqr(position)));
        if (volume <= 0) return;
        for (var sound : new net.minecraft.sounds.SoundEvent[]{SoundEvents.GLASS_BREAK, SoundEvents.FIRE_EXTINGUISH}) {
            client.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.PLAYERS,
                    volume, 1f, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        }
    }

    /** 主题音频：不使用原版距离衰减，音量逐 tick 按渐入 × 距离曲线重算。 */
    private static final class ThemeSound extends AbstractTickableSoundInstance {
        private final ClientLevel world;
        private final Vec3 center;
        private final long startedAt;

        ThemeSound(ClientLevel world, Vec3 center) {
            super(AddonSounds.EXPLOSION_THEME.get(), SoundSource.PLAYERS, RandomSource.create());
            this.world = world;
            this.center = center;
            this.startedAt = world.getGameTime();
            this.x = center.x;
            this.y = center.y;
            this.z = center.z;
            this.pitch = 1.0f;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.looping = false;
            tick();
        }

        @Override
        public void tick() {
            var client = Minecraft.getInstance();
            if (client.level != world || client.player == null) {
                volume = 0;
                stop();
                return;
            }
            int since = (int) Math.max(0, world.getGameTime() - startedAt);
            volume = ExplosionRules.themeVolume(ExplosionRules.SOUND_START_TICKS + since,
                    Math.sqrt(client.player.distanceToSqr(center)));
            if (volume <= 0f && since > ExplosionRules.SOUND_FADE_TICKS) stop();
        }

        @Override
        public boolean canStartSilent() { return true; }
    }
}
