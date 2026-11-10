package github.com.gengyoubo.sscfa.item;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;
import github.com.gengyoubo.sscfa.compat.CTPUtils;
import net.onixary.shapeShifterCurseForge.effect.FeedEffect;
import org.jetbrains.annotations.Nullable;

/** 鐠囨洜顓搁惃鍕仜閺冭泛澧忛柌蹇撳弳閸欙綇绱辨穱婵堟殌閸樼喓澧楅崣鏍ㄦ殻閸滃SC閹垹顦查柅鏄忕帆閿涘苯褰ф稉鐑樻拱濞嗏€冲竾缂傗晞鍏橀柌蹇氱槸缁狅繝妾虹€规氨缂夐弨鍙ョ瑐娑撳鏋冮妴?*/
public final class PotionTubeDose {
    private static final ThreadLocal<FeedDose> FEED_DOSE = new ThreadLocal<>();

    private PotionTubeDose() {}

    /** 閺咁噣鈧碍瀵旂紒顓熸櫏閺嬫粌褰查幐澶嬫闂€鎸庡閸掑棴绱遍惉顒佹閺佸牊鐏夋禒鍛磻閺€鎯у嚒閺嶇顕崜鍌炲櫤鐠囶厺绠熼惃鍕笉閻ゆぜ鈧椒婵€鐎瑰啿寮烽崢瀣級閼充粙鍣洪妴?*/
    public static boolean isDivisible(MobEffect effect) {
        if (effect instanceof TransformativeStatusEffect) return false;
        return !effect.isInstantenous() || effect == MobEffects.HEAL
                || effect == MobEffects.HARM || effect instanceof FeedEffect;
    }

    /** 閸欐ê鑸伴惄顔界垼NBT閸忛攱婀佹稉鈧▎鈩冣偓褑顕㈡稊澶涚礉閸楀厖濞囬懡顖涙寜閺佸牊鐏夐崚妤勩€冩稉铏光敄娑旂喍绗夐懛顏勫З鐠併倓缍旈弲顕€鈧俺宓傚娣偓?*/
    public static boolean isDivisible(ItemStack stack) {
        if (stack.isEmpty() || CTPUtils.getCTPFormIDFromNBT(stack.getTag()) != null) return false;
        return PotionUtils.getMobEffects(stack).stream().allMatch(effect -> isDivisible(effect.getEffect()));
    }

    public static void applyInstant(MobEffectInstance effect, @Nullable Player player,
                                    LivingEntity user, double dose) {
        applyInstant(effect, player, player, user, dose);
    }

    public static void applyInstant(MobEffectInstance effect, @Nullable Entity source,
                                    @Nullable Entity attacker, LivingEntity target, double dose) {
        if (!Double.isFinite(dose) || dose <= 0) return;
        double fraction = Math.min(1, dose);
        MobEffect type = effect.getEffect();
        if (!(type instanceof FeedEffect)) {
            // 閸樼喓澧楀▽鑽ゆ灍/娴笺倕顔婇崷銊︻劃濞岃法鏁?int(閸╄櫣顢呴崐娴庢澧忛柌蹇ョ礈0.5) 閻ㄥ嫬褰囬弫鏉戝挤閹碘偓閺堝甯張澶婅埌閹椒鎱ㄥ锝冣偓?
            type.applyInstantenousEffect(source, attacker, target, effect.getAmplifier(), fraction);
            return;
        }
        FeedDose previous = FEED_DOSE.get();
        FEED_DOSE.set(new FeedDose(target, fraction));
        try {
            type.applyInstantenousEffect(source, attacker, target, effect.getAmplifier(), fraction);
        } finally {
            if (previous == null) FEED_DOSE.remove();
            else FEED_DOSE.set(previous);
        }
    }

    /** 閸欘亝娴涢幑銏℃拱濞嗩搶eed鐠嬪啰鏁ら惃鍕煑濠у懓绐涚粋璁崇瑓闂勬劧绱遍弲顕€鈧氨鎽遍妴浣告煑濠у懎鎷板鐐垫殌濞屸剝婀佺拠銉ょ瑐娑撳鏋冮妴?*/
    public static double feedFraction(LivingEntity target, double original) {
        FeedDose context = FEED_DOSE.get();
        if (context == null || context.target != target || context.claimed) return original;
        context.claimed = true;
        return context.fraction;
    }

    /** 閹规洝骞忛崢鐔烘晸ceil閸撳秶娈戞鐔哄⒖閸撳倿鍣洪敍宀勪缉閸忓秴顦查崚绂⊿C閻ㄥ嫬鐔€绾偓閺佹澘鈧吋鍨ㄩ崣鏍ㄦ殻缁犳纭堕妴?*/
    public static void captureFeedFood(LivingEntity target, double unroundedFood) {
        FeedDose context = FEED_DOSE.get();
        if (context != null && context.target == target && context.claimed && !context.foodCaptured) {
            context.unroundedFood = unroundedFood;
            context.foodCaptured = true;
        }
    }

    /** 妞嬬喓澧挎禒宥呭絿閸樼喓鏁揷eil閿涙盯銈遍崪灞藉閹稿鏆ｉ悺鍓佸殠閹冨瀻閸撳倿鍣洪敍宀勪缉閸忓秹顥ら悧鈺佹嫲modifier閸欏矂鍣哥紓鈺傛杹閵?*/
    public static float feedSaturation(LivingEntity target, int food, float original) {
        FeedDose context = FEED_DOSE.get();
        if (context == null || context.target != target || !context.foodCaptured || context.saturationClaimed || food <= 0) return original;
        context.saturationClaimed = true;
        double adjusted = context.unroundedFood * original / (food * context.fraction);
        return Double.isFinite(adjusted) ? (float) adjusted : original;
    }

    private static final class FeedDose {
        private final LivingEntity target;
        private final double fraction;
        private boolean claimed, foodCaptured, saturationClaimed;
        private double unroundedFood;
        private FeedDose(LivingEntity target, double fraction) {
            this.target = target;
            this.fraction = fraction;
        }
    }
}
