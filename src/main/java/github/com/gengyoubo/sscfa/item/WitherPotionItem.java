package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * 鍑嬮浂鑽按锛堣嚜瀹氫箟鍙爢鍙犺嵂姘达紝楗敤 / 鍠锋簠 / 婊炵暀 涓夊瀷锛夈€?
 *
 * 璁捐锛堢敤鎴?2026-07-04 纭锛夛細
 * - 浠讳綍浜洪兘鑳介ギ鐢?/ 鎶曟幏锛涘埗浣滃熀纭€楗敤鍨嬩粛闇€ SP闃垮姫姣旀柉锛坕tem_on_item锛氬噵闆剁帿鐟?+ 鑽按 鈫?1 鐡讹級銆?
 * - 鏁堟灉锛氬浐瀹氬噵闆?II锛坅mplifier 1锛夛紝20 绉掋€傞ギ鐢ㄥ瀷浣滅敤浜庤嚜宸憋紱鍠锋簠 / 婊炵暀鍨嬫姇鎺峰悗瀵硅寖鍥村唴鐢熺墿 AOE銆?
 * - 閰块€狅細楗敤鍨?+ 鐏嵂 鈫?鍠锋簠鍨嬶紱鍠锋簠鍨?+ 榫欐伅 鈫?婊炵暀鍨嬶紙瑙?BrewingRegistryInfiniteMixin锛夈€?
 * - 鐡惰韩甯﹂檮榄斿厜鏁堬紙hasGlint锛夈€傚彲鍙?3 鐡讹紙maxCount 鐢辨敞鍐屽鎺у埗锛夈€?
 * - 涓嶇户鎵?PotionItem锛岄伩鍏嶈"鑽按鍙爢鍙?绫绘ā缁勮浼ゃ€?
 */
public class WitherPotionItem extends Item {

	/** 鍑嬮浂绛夌骇锛坅mplifier 1 = 鍑嬮浂 II锛?*/
	private static final int WITHER_AMPLIFIER = 1;
	/** 鍑嬮浂鏃堕暱锛坱ick锛? 20绉?*/
	private static final int WITHER_DURATION = 400;
	/** 楗敤璇绘潯鏃堕暱锛屼笌鍘熺増鑽按涓€鑷达紙32 tick锛?*/
	private static final int DRINK_TIME = 32;
	/** 鐡惰韩 / AOE 鐫€鑹诧紙鍑嬮浂鏆楄锛?*/
	private static final int POTION_COLOR = 0x4A403A;

	public enum Type {
		DRINK, SPLASH, LINGERING
	}

	private final Type type;

	public WitherPotionItem(Properties settings, Type type) {
		super(settings);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	/**
	 * 鎸夌帺瀹跺舰鎬佽繑鍥炲噵闆惰嵂姘村爢鍙犱笂闄愶細浣块瓟绯?8 / SP闃垮姫姣旀柉 3 / 鍏跺畠 1銆?
	 * 渚?Slot.getMaxItemCount / 鍒涢€犱腑閿鍒?/ 鍙屽嚮鍚堝苟 绛夊澶勭粺涓€璋冪敤銆?
	 */
	public static int getStackLimitFor(Player player) {
		if (player == null) {
			return 1;
		}
		if (FormUtils.isFamiliarFoxFamily(player)) {
			return 8;
		}
		if (FormUtils.isForm(player, FormIdentifiers.ANUBIS_WOLF_SP)) {
			return 3;
		}
		return 1;
	}

	/** 鐡惰韩闄勯瓟鍏夋晥銆?*/
	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return type == Type.DRINK ? DRINK_TIME : 0;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return type == Type.DRINK ? UseAnim.DRINK : UseAnim.NONE;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		if (type == Type.DRINK) {
			// 楗敤鍨嬶細璧锋墜璇绘潯锛屾晥鏋滃湪 finishUsing 鏂藉姞
			user.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		// 鍠锋簠 / 婊炵暀鍨嬶細鎶曟幏鍘熺増鑽按寮瑰皠鐗╋紙鎼哄甫鍑嬮浂鏁堟灉锛夛紝澶嶇敤鍘熺増 AOE / 鍦伴潰浜?
		if (!world.isClientSide) {
			spawnThrownPotion(world, user);
		}
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!user.getAbilities().instabuild) {
			stack.shrink(1);
		}
		user.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (type != Type.DRINK || !(user instanceof Player player)) {
			return super.finishUsingItem(stack, world, user);
		}
		if (!world.isClientSide) {
			// 鍥哄畾鍑嬮浂 II锛坅mplifier 1锛夛紝20 绉掞紙400t锛変綔鐢ㄤ簬楗敤鑰呰嚜宸?
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, WITHER_AMPLIFIER));
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 0.6f, 1.0f);
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return stack;
	}

	/** 鐢熸垚鎼哄甫鍑嬮浂鏁堟灉鐨勫師鐗堟姇鎺疯嵂姘达紙鍠锋簠 / 婊炵暀锛夛紝澶嶇敤鍘熺増 AOE / 鍦伴潰浜戞満鍒躲€備粎鏈嶅姟绔皟鐢ㄣ€?*/
	private void spawnThrownPotion(Level world, Player user) {
		ItemStack thrown = new ItemStack(type == Type.LINGERING ? Items.LINGERING_POTION : Items.SPLASH_POTION);
		PotionUtils.setCustomEffects(thrown, List.of(
				new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, WITHER_AMPLIFIER)));
		thrown.getOrCreateTag().putInt("CustomPotionColor", POTION_COLOR);
		ThrownPotion entity = new ThrownPotion(world, user);
		entity.setItem(thrown);
		entity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
		world.addFreshEntity(entity);
	}
}
