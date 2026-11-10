package github.com.gengyoubo.sscfa.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 閸欘亪娈ｉ挊蹇撳斧閻楀牊膩閺夊じ鍚夌憗鍛畱閸愬懘鍎撮弫鍫熺亯鏉炴垝缍嬮敍灞肩箽閻ｆ瑧婀″锝囧缁斿娈戦張鍫濈毠閵嗕線鈧氨鏁ら懗浠嬪櫤缁涘妾仦鐐跺祩濮樻番鈧?*/
public final class PotionCarrierVisibility {
    private static boolean registered;

    private PotionCarrierVisibility() {}

    public static void register() {
        if (registered) return;
        registered = true;
        // 娑撱倗顫掗崣顖欐叏閺€鐟板灙鐞涖劌娼庨棁鈧潻鍥ㄦ姢閿涙碍鐓￠惇瀣珤閸欘垵鍏橀弨鍫曟肠鐏炴洜銇氶弽蹇斿灗閹兼粎鍌ㄩ弽蹇庢崲閹板繋绔存禒钘夊敶鐎瑰箍鈧?
        /* creative tab entries omitted */
    }

    public static boolean isInternalVanillaCarrier(ItemStack stack) {
        if (!(stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION) || stack.is(Items.TIPPED_ARROW)) || !stack.hasTag()) {
            return false;
        }
        // 濮ｆ棁绶濇穱婵嗙摠閻ㄥ嫮琚崹濠璂閿涘奔绗夌憰浣圭湴瀹告彃浠犻悽銊ф畱閺堝牆鐨筆otion缂佈呯敾濞夈劌鍞介敍灞肩瘍娑撳秷袝閸欐垶鏅ラ弸婊勬暈閸愬瞼娈戦棃娆愨偓浣稿灥婵瀵查妴?
        return switch (stack.getTag().getString("Potion")) {
            case "ssc_addon:moon_dust_tube", "ssca_neomagic:moon_dust_tube", "ssc_addon:universal_energy" -> true;
            default -> false;
        };
    }
}
