package github.com.gengyoubo.sscfa.spell;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * 法术伤害源：SSCA 魔法体系专用独立伤害类型 {@code ssc_addon:spell_damage}。
 *
 * <p>Forge 1.20.1 的 {@code DamageSources.source(ResourceKey)} 为私有，这里在服务器启动时缓存
 * {@link Holder}，再用公开的 {@link DamageSource} 构造器创建；未绑定前回退原版魔法伤害。</p>
 */
public final class SpellDamageSource {

	public static final ResourceKey<DamageType> SPELL_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("ssc_addon", "spell_damage"));

	private static volatile Holder<DamageType> holder;

	private SpellDamageSource() {
	}

	/** 服务器启动时调用，绑定数据包注册表中的伤害类型。 */
	public static void bind(MinecraftServer server) {
		try {
			holder = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPELL_DAMAGE);
		} catch (RuntimeException ignored) {
			holder = null;
		}
	}

	public static DamageSource of(DamageSources ds) {
		Holder<DamageType> h = holder;
		return h == null ? ds.magic() : new DamageSource(h);
	}

	public static DamageSource of(DamageSources ds, Entity attacker) {
		Holder<DamageType> h = holder;
		return h == null ? ds.indirectMagic(attacker, attacker) : new DamageSource(h, attacker, attacker);
	}

	public static DamageSource of(DamageSources ds, Entity source, Entity attacker) {
		Holder<DamageType> h = holder;
		return h == null ? ds.indirectMagic(source, attacker) : new DamageSource(h, source, attacker);
	}
}
