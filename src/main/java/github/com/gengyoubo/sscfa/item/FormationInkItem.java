package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.spell.FormationElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 法阵油墨（jackcooper）。三型：
 * <ul>
 *   <li>{@link Type#NORMAL 普通}：玻璃瓶+墨囊+月尘纯晶合成，是系别油墨的基底；</li>
 *   <li>{@link Type#ICE 冰系}：普通油墨+雪球合成，抄写冰系法阵用；</li>
 *   <li>{@link Type#FIRE 火系}：普通油墨+岩浆膏合成，抄写火系法阵用。</li>
 * </ul>
 * <p>油墨共七型：普通基底 + 六系到（冰/火/月辉/诅咒/召唤/虚无/空间，抄写对应系法阵耗材）。</p>
 * <p>抄写消耗：法阵每级 1 瓶对应系油墨（L1=1 瓶、L3=3 瓶…）。</p>
 */
public class FormationInkItem extends Item {

	public enum Type {
		NORMAL("normal", null),
		ICE("ice", FormationElement.ICE),
		FIRE("fire", FormationElement.FIRE),
		LUNAR("lunar", FormationElement.LUNAR),
		CURSE("curse", FormationElement.CURSE),
		SUMMON("summon", FormationElement.SUMMON),
		VOID("void", FormationElement.VOID),
		SPACE("space", FormationElement.SPACE);

		/** 类型 id（模型 / lang 用）。 */
		public final String id;
		/** 对应法阵系别（NORMAL 为 null）。 */
		public final FormationElement element;

		Type(String id, FormationElement element) {
			this.id = id;
			this.element = element;
		}
	}

	private final Type type;

	public FormationInkItem(Properties settings, Type type) {
		super(settings);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		if (type == Type.NORMAL) {
			tooltip.add(Component.translatable("item.ssc_addon.formation_ink.tip_normal").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			tooltip.add(Component.translatable("item.ssc_addon.formation_ink.tip_element",
					Component.translatable(type.element.getNameKey())).withStyle(type.element.formatting));
			tooltip.add(Component.translatable("item.ssc_addon.formation_ink.tip_hint").withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
