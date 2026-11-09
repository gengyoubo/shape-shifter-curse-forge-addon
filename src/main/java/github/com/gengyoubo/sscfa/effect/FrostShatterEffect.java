package github.com.gengyoubo.sscfa.effect;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

/**
 * 霜碎效果（寒霜系法术·冰霜新星附??debuff，jackcooper）：护甲 -50%??
 *
 * <p>实现走原??{@code GENERIC_ARMOR} 修饰符（onApplied/onRemoved 对称加减?
 * 到??净??喝奶自动还原，无泄漏、无残留）。时长由冰霜新星施法时决定（与缓速同步，
 * L1=3s ??L5=5s）。破甲是??mod 首个护甲类属?debuff，与缓速分离为独立效果?
 * 便于法术免疫表与提示文本独立控制?/p>
 */
public class FrostShatterEffect extends MobEffect {

	/** 护甲修饰???UUID（固定，防重复叠加）??*/
	private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("a3b7c8d9-e0f1-4a2b-8c3d-4e5f60718293");
	private static final String ARMOR_MODIFIER_NAME = "SSCA Frost Shatter Armor Break";

	/** 护甲减半??*/
	private static final double ARMOR_REDUCTION = -0.5;

	// 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
	private static final BalanceReader BAL = new BalanceReader("abilities.frost_shatter");

	public FrostShatterEffect() {
		super(MobEffectCategory.HARMFUL, 0x9FD8EF); // 浅冰蓝（比霜冻略淡??
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.addAttributeModifiers(entity, attributes, amplifier);
		AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
		if (armorAttr != null) {
			// 先移除旧实例再添加（刷新施放不叠加，???-50%??
			armorAttr.removeModifier(ARMOR_MODIFIER_UUID);
			armorAttr.addTransientModifier(new AttributeModifier(
					ARMOR_MODIFIER_UUID,
					ARMOR_MODIFIER_NAME,
					BAL.d("armor_reduction", ARMOR_REDUCTION),
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);
		AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
		if (armorAttr != null) {
			armorAttr.removeModifier(ARMOR_MODIFIER_UUID);
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 10 == 0;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// 破甲期持续飘细碎冰晶（提示目标护甲已被击碎）
		if (entity.level() instanceof ServerLevel serverWorld) {
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld,
					ParticleTypes.SNOWFLAKE,
					entity.getX(), entity.getY(0.6), entity.getZ(),
					2, entity.getBbWidth() / 3.0, 0.15, entity.getBbWidth() / 3.0, 0.01);
		}
	}
}
