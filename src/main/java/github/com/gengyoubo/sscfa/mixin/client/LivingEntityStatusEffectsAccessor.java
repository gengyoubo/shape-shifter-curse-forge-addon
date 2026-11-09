package github.com.gengyoubo.sscfa.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 恐惧视野限制用：直接暴露 {@code LivingEntity.activeStatusEffects}（效果实例 Map）。
 *
 * <p>背景：恐惧的雾需要「失明」作为载体（vanilla 雾管线 + 光影 blindness uniform 都认它），
 * 但受害者形态的 Apoli 效果免疫拦死 addStatusEffect 全部路径，故直接写 Map 绕过施加链
 * （免疫管不到 Map 写入，而雾管线每帧只查 Map）。光影下失明雾被 IrisShaderPackMixin
 * 内存替换为「粉色 + 12→16 格」版本；无光影时由 FearFogMixin 覆盖雾距/雾色。</p>
 */
@Mixin(LivingEntity.class)
public interface LivingEntityStatusEffectsAccessor {

	@Accessor("activeEffects")
	Map<MobEffect, MobEffectInstance> sscAddon$getActiveStatusEffects();
}
