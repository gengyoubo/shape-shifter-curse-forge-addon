package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.ArcaneAnalysis;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 澧炲己娉曢樀鐗╁搧锛坖ackcooper锛夈€傜被浼煎嵎杞寸殑绾哥墖锛孨BT 缁戝畾绯诲埆锛堢伀/鍐帮級涓庣瓑绾э紙1-5锛夈€?
 *
 * <p><b>闀挎寜鍙抽敭钃勫姏</b>锛堝悓 waystone 鍥炲煄鍗疯酱/杩涘寲鐭筹紝32t=1.6 绉掞級=銆岃褰曢瓟娉曘€嶏細钃勬弧鍚庣墿鍝佹秷澶便€?
 * 娉曢樀璁板綍杩涚帺瀹舵暟鎹紙{@link FormationKnowledgeComponent}锛夛紝涔嬪悗鍙湪娉曟湳鐮旂┒鍙版秷鑰楁湀灏樺涔犮€佹妱鍐欍€?
 * 宸茶褰曡繃鍚岀郴鍚岀骇鏃?b>璧锋墜鍗虫彁绀?/b>銆佷笉杩涘叆钃勫姏銆佺墿鍝佷笉娑堣€楋紙闃茶鎹燂紝涔熶笉鐢ㄥ偦绛?1.6 绉掞級锛?
 * 楂樹綆绛夌骇浜掍笉鍐茬獊锛堝悇绛夌骇鐙珛璁板綍锛夈€傝搫鍔涗腑閫旀澗寮€/琚墦鏂?= 鍙栨秷锛屼笉璁板綍涓嶆秷鑰椼€?/p>
 */
public class FormationItem extends Item {


	public FormationItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack=user.getItemInHand(hand);FormationElement element=FormationData.getElement(stack);
		if(ArcaneAnalysis.isUnanalyzed(stack)){
			if(!world.isClientSide)user.displayClientMessage(Component.translatable("message.ssc_addon.analysis.required").withStyle(ChatFormatting.RED),true);
			return InteractionResultHolder.fail(stack);
		}
		if(element==null)return InteractionResultHolder.pass(stack);
		String variant=FormationData.getVariant(stack);int level=FormationData.getLevel(stack);
		if(FormationKnowledgeComponent.get(user).hasRecorded(element,variant,level)){
			if(world.isClientSide)user.displayClientMessage(Component.translatable("message.ssc_addon.formation.already_recorded",displayName(element,variant),level).withStyle(ChatFormatting.YELLOW),true);
			return InteractionResultHolder.fail(stack);
		}
		user.startUsingItem(hand);return InteractionResultHolder.consume(stack);
	}

	/** 鍙樹綋鎰熺煡鏄剧ず鍚嶏紙閫氱敤绯绘樉绀哄彉浣撳悕锛屽叾瀹冪郴鏄剧ず绯诲埆鍚嶏級銆?*/
	private static Component displayName(FormationElement element, String variant) {
		return element == FormationElement.UNIVERSAL
				? Component.translatable(variantNameKey(variant))
				: Component.translatable(element.getNameKey());
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if(ArcaneAnalysis.isUnanalyzed(stack))return stack;
		if(!world.isClientSide&&user instanceof ServerPlayer player){
			FormationElement element=FormationData.getElement(stack);
			if(element!=null){
				int level=FormationData.getLevel(stack);String variant=FormationData.getVariant(stack);
				var knowledge=FormationKnowledgeComponent.get(player);
				if(!knowledge.hasRecorded(element,variant,level)){
					knowledge.record(element,variant,level);FormationKnowledgeComponent.sync(player);
					if(!player.getAbilities().instabuild)stack.shrink(1);
					player.displayClientMessage(Component.translatable("message.ssc_addon.formation.recorded",displayName(element,variant),level).withStyle(ChatFormatting.GREEN),true);
					world.playSound(null,player.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,.4f,1.6f);
				}
			}
		}
		return stack;
	}

	@Override
	public Component getName(ItemStack stack) {
		if(ArcaneAnalysis.isUnanalyzed(stack))return Component.translatable("item.ssc_addon.unfamiliar_spell");
		FormationElement element = FormationData.getElement(stack);
		if (element != null) {
			int level = FormationData.getLevel(stack);
			// 鍚嶇О鎸夊搧璐ㄨ壊锛堢櫧/缁?钃?绱?姗欙紝涓庡嵎杞翠竴鑷达級锛涢€氱敤绯绘樉绀哄彉浣撳悕锛屽叾瀹冪郴鏄剧ず绯诲埆鍚?
			return Component.translatable(element == FormationElement.UNIVERSAL
					? "item.ssc_addon.formation.named_universal" : "item.ssc_addon.formation.named",
					displayName(element, FormationData.getVariant(stack)), level)
					.withStyle(FormationData.getRarity(level).color);
		}
		return super.getName(stack);
	}

	/** 閫氱敤绯诲彉浣撳悕 lang key銆?*/
	private static String variantNameKey(String variant) {
		String v = FormationData.normalizeVariant(variant);
		return "formation.ssc_addon.variant." + (v != null ? v : FormationData.VARIANT_REGEN);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		if(ArcaneAnalysis.isUnanalyzed(stack)){
			tooltip.add(Component.translatable("message.ssc_addon.analysis.required").withStyle(ChatFormatting.GRAY));
			return;
		}
		FormationElement element = FormationData.getElement(stack);
		if (element == null) {
			tooltip.add(Component.translatable("item.ssc_addon.formation.tip_empty").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		int level = FormationData.getLevel(stack);
		// 閫氱敤绯伙細鎸夊彉浣撳垎涓夊鏂囨锛堝洖鑳?鑳介噺杞寲 / 澧炶兘=娉曞姏涓婇檺 / 缁忛獙=exp 鏁堢巼锛?
		if (element == FormationElement.UNIVERSAL) {
			String variant = FormationData.getVariant(stack);
			switch (variant == null ? FormationData.VARIANT_REGEN : variant) {
				case FormationData.VARIANT_MANA -> {
                    int manaPct = github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.formationSafetyBonus(level);
					tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("formation_mana",
							manaPct).withStyle(ChatFormatting.GRAY));
				}
				case FormationData.VARIANT_EXP -> {
					int expPct = Math.round(FormationData.universalExpPerLevel() * level * 100);
					tooltip.add(Component.translatable("item.ssc_addon.formation.tip_exp",
							expPct).withStyle(ChatFormatting.GRAY));
				}
				case FormationData.VARIANT_RECOVERY -> {
					int recPct = Math.round(0.20f * level * 100);
					tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("formation_recovery",
							recPct).withStyle(ChatFormatting.GRAY));
				}
				default -> {
					int pct = (int) Math.round(FormationData.universalThreshold(level) * 100);
					tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("formation_regen",
							pct,
							(int) FormationData.universalManaDrainPerSec(),
							(int) FormationData.universalBookManaPerSec()).withStyle(ChatFormatting.GRAY));
				}
			}
			tooltip.add(Component.translatable("item.ssc_addon.formation.tip_use").withStyle(ChatFormatting.DARK_GRAY));
			tooltip.add(Component.translatable("item.ssc_addon.formation.tip_hint").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
        if(element==FormationElement.SPACE){
            tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("formation_space",
                    Math.round(FormationData.spaceRangeBonusPerLevel()*level*100),
                    Math.round(FormationData.cooldownReductionPerLevel()*level*100),
                    Math.round(FormationData.manaCostPerLevel()*level*100)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.ssc_addon.formation.tip_use").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.ssc_addon.formation.tip_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int pct = Math.round(FormationData.damageBonusPerLevel() * level * 100);
		int cdPct = Math.round(FormationData.cooldownReductionPerLevel() * level * 100);
		int manaPct = Math.round(FormationData.manaCostPerLevel() * level * 100);
		tooltip.add(github.com.gengyoubo.sscfa.neomagic.NeoMagic.text("formation_effect",
				Component.translatable(element.getNameKey()), pct,
				Component.translatable(element.opponent().getNameKey()), pct,
				cdPct, manaPct).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.formation.tip_use").withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.formation.tip_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
