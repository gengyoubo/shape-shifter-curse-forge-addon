package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 妫板棗鐓欓悥鍡欏仮闂呮梻顬囬敍鍧朼ckcooper閿?026-09-22 闂団偓濮瑰倹鐗崇€圭儑绱氶敍姘卞瀻閻愮鐨犵痪澶哥瑝鐠併倛鐦戞鎴ｅ閻炲啫锛撻敍鍫濓紦闂堢偞鏌熼崸妤嬬礆閿?
 * 閻栧棛鍋㈠▔銏犲挤閼煎啫娲块崘鍛畱鐠恒劎鏅弬鐟版健娴兼俺顫﹂悡褍鐖堕悙鍛婄槈閳ユ柡鈧柨顦婚悾?TNT 閼崇晫鍋㈤幒澶愵暙閸╃喎鍞撮弬鐟版健閵嗕礁鍞撮柈銊у瀻閻愰晲绡冮懗鐣屽仮婢舵牜鏅妴?
 *
 * <p>鐎圭偘缍嬫导銈咁唺瀹歌尙鏁?{@code SscAddonLivingEntityMixin.damage} 閻ㄥ嫯娉曢悾灞惧閹搭亣顩惄鏍电礄閻栧棛鍋?
 * DamageSource.getPosition 閺冪姵妯夊蹇撴綏閺嶅洦妞傞崶鐐衡偓鈧悥鍡樼爱鐎圭偘缍嬮崸鎰垼閿涘苯鍑￠崣宥囩椽鐠囨垶鐗崇€圭儑绱氶敍?
 * 閺?mixin 閸欘亣藟閺傜懓娼￠棃顫窗{@code collectBlocksAndDamageEntities} TAIL 鏉╁洦鎶?
 * {@code affectedBlocks} 娑擃厺绗岄悥鍡楃妇閸掑棗鐫樻竟鍐插敶婢舵牜娈戦弬鐟版健閿涘牆顦查悽?{@link
 * DomainManager#blocksCrossBoundary}閿涘苯锛撻梻瀵哥暬閸愬拑绱濇稉搴濈炊闁?婢逛即鐓剁拠顓濈疅娑撯偓閼疯揪绱氶妴?/p>
 *
 * <p>娴犲懏婀囬崝锛勵伂閿涘牊鏌熼崸妤冪壃閸у繐褰ч崷銊︽箛閸旓紕顏紒鎾剁暬閿涘绱辩悮顐ョ箖濠娿倗娈戦弬鐟版健娑撳秶鍋㈡稉宥嗗竴閽€鏂ょ礉閻栧棛鍋㈢憴鍡氼潕缁帒鐡?
 * 閻撗冪埗閿涘牆顓归幋椋庮伂鐞涖劎骞囬敍灞炬￥閻潙娼栫拠顓濈疅閿涘鈧?/p>
 */
@Mixin(Explosion.class)
public abstract class DomainExplosionBoundaryMixin {
	@Shadow @Final private double x;
	@Shadow @Final private double y;
	@Shadow @Final private double z;
	@Shadow @Final private Level level;

	@Inject(method = "explode", at = @At("TAIL"))
	private void ssca$filterCrossBoundaryBlocks(CallbackInfo ci) {
		if (!(level instanceof ServerLevel serverWorld)) return;
		Vec3 center = new Vec3(x, y, z);
		((Explosion) (Object) this).getToBlow()
				.removeIf(pos -> DomainManager.blocksCrossBoundary(serverWorld, center, Vec3.atCenterOf(pos)));
	}
}
