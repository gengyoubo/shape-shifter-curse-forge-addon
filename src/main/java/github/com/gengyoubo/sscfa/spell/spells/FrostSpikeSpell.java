package github.com.gengyoubo.sscfa.spell.spells;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.entity.SpellFrostSpikeEntity;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;

/**
 * 冰锥（白色基底，jackcooper）：朝准星射出一枚冰锥，命中造成魔法伤害。
 *
 * <p><b>本类只含行为</b>（投射物生成、音效、等级缩放应用）；全部数值已外移到
 * {@code data/ssc_addon/spells/frost_spike.json}（数据包可覆盖）：</p>
 * <ul>
 *   <li>基准：6 伤 / cd 10 秒（L5 3 秒）/ 无前摇 / 耗书法力 15 / 单独使用惩罚 0.5×伤 2×cd 2×施法时间；</li>
 *   <li>levels[5]：L2 +25% 伤速 -20% cd、L3 再 +25% 伤 -20% cd、L4 再 +12.5% 伤 -15% cd、L5 再 +10% 伤 -30% cd（合计伤 +83% cd -70%）；
 *       品质 1白/2绿/3蓝/4紫/5橙（决定单独使用次数 8/6/4/2/1）；L4+ 投射物换 3D 冰锥模型。</li>
 * </ul>
 */
public class FrostSpikeSpell extends Spell {

	@Override public double getProjectileRange(int level) { return SpellFrostSpikeEntity.maxDistance(); }

	public FrostSpikeSpell() {
		super(new ResourceLocation("ssc_addon", "frost_spike"), SpellRarity.WHITE);
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {
		cast(caster, power, solo, 1);
	}

	/** 带等级施法：速度与投射物外观（L4+ 换 3D 冰锥模型）按卷轴等级缩放。 */
	public void cast(ServerPlayer caster, float power, boolean solo, int level) {
		SpellFrostSpikeEntity spike = new SpellFrostSpikeEntity(caster.level(), caster);
		spike.setDamage(power);
		spike.setLevel(level);
		spike.setExpBountyTen(ssc_addon$takePendingExp()); // exp_mode 1/2 挂起经验随弹射物走
		spike.setRefundCastId(solo ? null : ssc_addon$getRefundCastId());
		Vec3 look = caster.getViewVector(1.0F);
		spike.setDirection(look,(float)RuneCastContext.current().scale(getSpeedMultiplier(level),RuneModifiers.Stat.SPEED));
		caster.level().addFreshEntity(spike);
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 0.8f);
	}
}
