package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import github.com.gengyoubo.sscfa.balance.BalanceReader;

import java.util.List;

/**
 * 闁哄牆鐗忕划鎰版憘濞戞粠娼堕柛鏃戠厐闁垮绠戞慨锝嗗笂闂€婊堟憖閳ь剟鏁嶉崷?闁挎稑顧€缁变即寮甸崼銏㈢煆闁炬梹绋撶敮铏光偓纭呮硾閹冲棝宕?8 闁哄秶鍘ч崬鎾Υ娴ｇ瓔娼堕柕鍡楃焷濠€妤冪磾閹寸姷鐐婇棅顒夊亖閳ь剙绉甸崹銊╁Υ瀹€鍐跨串闁间匠浣插亾瀹ュ洤笑闁诡兛绀佸ú鑸垫媴韫囨洘鐣遍柣銏㈠枔婢у潡鏁?
 * 闁归晲鑳堕悽鑽ゆ偖椤愶絾鐓㈤柛鏃傚С閼垫垵袙?I闁挎稑鐗婇惁锟犳⒕閺傚墽顏辨繛鍫濈仛濡炲倿姊婚弶鎴濈厱闁哄倸搴滅槐婵囩┍濠靛﹦妲堥悷鏇炴濞插﹥绋夊鍡樼劷婵℃缍囩槐姘跺Υ?
 *
 * <p>閻犱焦宕橀鍝ユ啺娴ｅ搫浠柨?
 * <ul>
 *   <li>闁哄牆绉存慨鐔虹博椤栨稒缍€濠电偘绶ょ槐浼村箥椤愶絽浼庨柡鍫濈Т婵喓绮╅姘辨澖濞达絾鎸堕埀顑跨劍濠€鍥礉閿涘嫷浼?addStatusEffect闁挎稑鑻褰掑箣妞嬪寒浼傞柣銏犲船鐢偊鎮ч崼婵囧€辨慨婵勫劵缁辨繄鈧箍鍨哄┃鈧慨婵撶悼閳ユ﹢濡?/li>
 *   <li>闁规鍋呭鎸庯紣閹寸姴鑺?{@link #SCAN_INTERVAL}闁挎稑鐗婇惁?60t 闁规鍋傜粩鏉戔枎閳藉懐绀嗛柨娑樺閼垫垵袙閹烘挻顦ч梻鈧?{@link #POISON_DURATION}闁?0t闁?
 *       &gt; 闁规鍋呭鍧楁⒒閹绢喗顓鹃柨娑樺缁绘氨鎷犳担绛嬫蕉缂傚啯鍨靛ú鑸垫媴韫囨梹鍩傞梻鍌滅節閼垫垵袙閹烘挸鐦紓渚囧幒缁楀绋夐鐔哥劷闁?/li>
 *   <li>闁谎嗘閹洟宕￠弴顏嗙獥濮掓稒顭堥濠氭儌閽樺鍊抽柛妤佹穿缁辨瑩鎯傞挊澶嬪€抽柛妤佹礀閸炴挳鎮抽埡渚囧晙/閻庡湱濮锋晶鎸庣▔瀹ュ懎缍€鐟滄澘宕幖鐑芥晬婢舵稓骞㈤柧姘焾濞叉挾鐚惧Ч鍥ｅ亾娴ｇ懓浜伴柣蹇曟暩闁挳宕?{@code isBoundImmune}
 *       闁哄秷顫夊﹢鐗堢▔瀹ュ嫮绐楅悶姘煎亗缁楀倿鎽傚☉姘辩Ч闁绘鍩栭埀顑跨筏缁辨繈寮崨顓滀函闁绘帟娉涢崢銈夋偪椤愵剛绀夋慨婵勫€曢ˇ鈺呭籍閻樼粯浠樺Λ鐗堢箓椤﹀宕氶妶鍡樼劷闁?/li>
 *   <li>闁诡儸鍡楀幋闁挎稒宀稿娲礆閼搁潧顥囬柟璇茬箰瀹曟劕顕?{@link #SCAN_RADIUS} + 婵?60t 闁归潧绉垫竟鍌涚▔閳ь剙鈻庢幊閳?/li>
 * </ul>
 */
public final class SpiderMoonWeaverMoonPoisonManager {

    // 濞寸姰鍎扮粭鍛村锤閸ワ箒绀嬪娑欘焾椤撳宕愮涵椋庡耿閺夆晜鍔橀、鎴﹀籍閺堢數鐭?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柨娑樻綂bilities.moon_weaver_moon_poison闁?
    /** 闁规鍋呭鍧楀础婵犲倻绐為柨娑樼墛閻楁悂鏁嶆径娑氱獥閻庨潧缍婄紞鍫ユ憘濞戞顐剧€?AURA_RADIUS闁?*/
    private static final double SCAN_RADIUS = 8.0;
    /** 闁规鍋呭鍧楁⒒閹绢喗顓鹃柨娑樻緟ick闁挎稑顧€缁辨澘袙?3 缂佸甯楁竟鍌涚▔閳ь剙鈻庢幊閳?*/
    private static final int SCAN_INTERVAL = 60;
    /** 闁哄倽妫勬慨鐐寸▔椤撶喓妲?I 闁汇劌瀚€垫梻绱掗鐔割槯闂傗偓閸栵紕绀則ick闁挎稑顧€缁? 缂佸甯槐婵囧緞瑜岀花顒勫箥椤愶絽浼庨梻鍌涙尦濞堁勭┍濠靛﹦妲堝☉鎾崇У閺屽洤顩奸敐鍐ｅ亾?*/
    private static final int POISON_DURATION = 80;

    // 闂傚啳鍩栭?5闁挎稒纰嶅﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲绠涢銈呭季閻犲洩顕цぐ鍥晬閸繃褰ラ柣鎾楀嫭寮撻柛鎺撶箓椤劙宕犻弽褎绀€闂侇偀鍋撳娑欘焾椤撹崵鏁幖浣告闁?
    private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_moon_poison");

    private SpiderMoonWeaverMoonPoisonManager() {}

    /**
     * 婵絽绻戝﹢鍥礉閿涘嫷浼?tick 閻庣敻鈧稓顏卞☉鎿冧簼濠€鈧紓浣告穿濞叉捇鎮抽埡渚囧晙闁圭瑳鍡╂斀闁挎稑鐗忛弫?{@code SscAddonServerEvents} 闁汇劌瀚敮铏光偓纭呮硾閹﹪鎮抽婵堟闁汇埄鐓夌槐姘跺Υ?
     * 闁?{@link #SCAN_INTERVAL} 闁煎搫鍊圭粊锕傚箥椤愶絽浼庨柛娑栧妼濞插潡宕ｅΔ鈧ú鍫曟偨閻斿搫鈷栨鐐跺煐閺岋箓宕濋悩杈幀婵?I闁?
     */
    public static void tick(ServerPlayer player) {
        // 闁告稏鍔嶅﹢锟犳⒒閵婏箑浠橀柛鎾崇Ф閻ゅ棝鏁嶅顒€甯ラ梻鍕Ч椤ｅ爼宕樺鍛驳鐟滆埇鍨洪埀?閻庡湱鍋樼紞瀣箥椤愶絽浼庨柨娑樼焸濞碱亪寮甸崼銏㈢煆闁炬梹绋撶敮铏光偓纭呭煐閻?tick 闁告瑯浜ｆ慨铏▔閳ь剙鈻庨垾鍐茬悼濞达絾鐟辩槐娆撳储閻斿摜妲?tick 闂侇喖鈧喓绠栧☉鎾亾婵?CCA 鐟滆埇鍨洪埀顑跨劍閻擄紕鎷犻～顔剧
        if (player.tickCount % BAL.i("scan_interval", SCAN_INTERVAL) != 0) return;
        if (!FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER)) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        double scanRadius = BAL.d("scan_radius", SCAN_RADIUS);
        AABB box = new AABB(player.blockPosition()).inflate(scanRadius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, e -> true);
        for (LivingEntity target : targets) {
            // 闁圭儤甯″▍搴ㄥ棘閼恒儳銆婇柤鏉挎噺濠€鐗堢?
            if (target.getUUID().equals(player.getUUID())) continue;
            // 闁谎嗘閹洟宕￠弴顏嗙獥濮掓稒顭堥濠氭儌閽樺鍊抽柛妤佹穿缁辨瑩鎯傞挊澶嬪€抽柛妤佹礀閸炴挳鎮抽埡渚囧晙/閻庡湱濮锋晶鎸庣▔瀹ュ懎缍€鐟滄澘宕幖鐑芥晬?
            if (WhitelistUtils.isProtected(player, target)) continue;
            // 濞寸姴鎳庨顔炬暜閿旇姤绠掗柕鍡楃焷濠€妤冪磾閹寸姷鐐婇棅顒夊亖閳ь剙绉甸崹銊╁Υ瀹€鍐跨串闁间匠浣插亾瀹ュ洤笑闁诡兛鑳跺▓鎴︽偨閻斿搫鈷栭柣銏㈠枑閺?
            boolean webBound = target.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get()) != null;
            boolean cocooned = target.getEffect(
                    github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get()) != null;
            if (!webBound && !cocooned) continue;
            // 闁哄倽妫勬慨鐐寸▔椤撶喓妲?I闁挎稑鐗呯粭澶愬及閸撗佷粵缂侇喗甯掗悺娆撴偝椤栨凹鏆旈柡浣哥墛閻忓鏁嶇仦鐐枖缂佲偓閾忓湱鐓堥悗娑欏姃娴滄帞鎷嬮埡鍐ㄨ礋閻庣娉涜ぐ鑼喆娓氬﹦绀嗛柨娑欑☉閻㈩偊寮懞銉с€婇柤?source 濞撴碍绋戦崣鍡楊潖閿旇棄顏￠柟鎼簻缂嶅﹪宕?
            target.addEffect(new MobEffectInstance(
                    MobEffects.POISON, BAL.i("poison_duration", POISON_DURATION), 0, false, true, true), player);
        }
    }
}
