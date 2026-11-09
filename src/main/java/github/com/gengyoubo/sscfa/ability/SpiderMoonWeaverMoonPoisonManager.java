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
 * 月织蛛被动·月毒侵蚀??）：月织蛛玩家周???8 格内、被「蜘网缠身」或「裹茧」状态困住的生物???
 * 持续被施加中?I（每隔一段时间刷新，保证覆盖不断档）?
 *
 * <p>设计要点??
 * <ul>
 *   <li>服务端权威：扫描服务端实体、服务???addStatusEffect，客户端由原版同步，客机正确???/li>
 *   <li>扫描频???{@link #SCAN_INTERVAL}（??60t 扫一次），中毒时???{@link #POISON_DURATION}??0t??
 *       &gt; 扫描间隔，保证被网困住期间中毒持续不中断??/li>
 *   <li>白名单：默认白名单（白名单内玩家/宠物不受影响）；蜘蛛类、悦灵系???{@code isBoundImmune}
 *       根本不会被上蛛网状态，故天然免疫，此处无需额外判断??/li>
 *   <li>性能：限制扫描半???{@link #SCAN_RADIUS} + ??60t 才扫一次??/li>
 * </ul>
 */
public final class SpiderMoonWeaverMoonPoisonManager {

    // 以下均为默认值；运行时???balance 快照读取（abilities.moon_weaver_moon_poison??
    /** 扫描半径（格）：对齐蛛丝??AURA_RADIUS??*/
    private static final double SCAN_RADIUS = 8.0;
    /** 扫描间隔（tick）：?3 秒扫一次??*/
    private static final int SCAN_INTERVAL = 60;
    /** 施加中???I 的持续时长（tick）?? 秒，大于扫描间隔保证不断档?*/
    private static final int POISON_DURATION = 80;

    // 阶??5：服务端权威快照读取（快照未初始化回退默认常量??
    private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_moon_poison");

    private SpiderMoonWeaverMoonPoisonManager() {}

    /**
     * 每服务???tick 对一个月织蛛玩家执行（??{@code SscAddonServerEvents} 的玩家循环调用）?
     * ??{@link #SCAN_INTERVAL} 节流扫描周围受困生物并施加中??I??
     */
    public static void tick(ServerPlayer player) {
        // 周期门控前置：先降频再做形??实体扫描，非月织蛛玩家??tick 只花一次取余（原???tick 都过一??CCA 形态查询）
        if (player.tickCount % BAL.i("scan_interval", SCAN_INTERVAL) != 0) return;
        if (!FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER)) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        double scanRadius = BAL.d("scan_radius", SCAN_RADIUS);
        AABB box = new AABB(player.blockPosition()).inflate(scanRadius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, e -> true);
        for (LivingEntity target : targets) {
            // 排除施法者本?
            if (target.getUUID().equals(player.getUUID())) continue;
            // 白名单：默认白名单（白名单内玩家/宠物不受影响?
            if (WhitelistUtils.isProtected(player, target)) continue;
            // 仅对带有「蜘网缠身」或「裹茧」状态的生物生??
            boolean webBound = target.getEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get()) != null;
            boolean cocooned = target.getEffect(
                    github.com.gengyoubo.sscfa.init.AddonEffects.SPIDER_WEB_BOUND.get()) != null;
            if (!webBound && !cocooned) continue;
            // 施加中???I（不显示粒子环境效果，显示粒子以让玩家可见）；带施法??source 供入梦拦截归???
            target.addEffect(new MobEffectInstance(
                    MobEffects.POISON, BAL.i("poison_duration", POISON_DURATION), 0, false, true, true), player);
        }
    }
}
