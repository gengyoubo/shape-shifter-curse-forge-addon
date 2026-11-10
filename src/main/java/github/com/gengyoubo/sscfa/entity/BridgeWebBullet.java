package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.ability.AddonWebBridgeAction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import github.com.gengyoubo.sscfa.ability.AddonWebBridgeAction;
import github.com.gengyoubo.sscfa.block.RegAddonBlocks;

/**
 * 鏈堢粐铔涖€屾惌璺ā寮忋€嶈洓涓濆脊锛坖ackcooper锛夛細澶嶇敤鍘熺増 {@link WebBullet} 鐨勯琛?/ 绮掑瓙 /
 * 鍙戝皠闊虫晥 / 鍛戒腑瀹炰綋缂犵粫锛堝惈绠瘨绾洪敜鍔犳垚锛夛紝浠呮妸鍛戒腑鏂瑰潡鐨勬惌澶╂閫昏緫鎹㈡垚闄勫睘
 * {@link AddonWebBridgeAction#BuildWebLadder}鈥斺€旀鍧楄矾寰勪笂閬囧埌闄勫睘鍑忛€熻洓缃戣啘
 * 锛坵eb_membrane锛夋椂鍏堟媶缃戣啘鍐嶆斁缃紝瀹炵幇銆屾惌璺柟鍧楁浛浠ｇ綉鑶溿€嶃€?
 *
 * <p>澶╂鍙傛暟鐩存帴寮曠敤鍘熺増 {@code WebBullet} 鐨?tier1/2/3 闈欐€侀厤缃紝淇濊瘉妗ｄ綅鏁板€?
 * 涓庡師鐗堣湗铔涙惌璺畬鍏ㄤ竴鑷淬€?/p>
 */
public class BridgeWebBullet extends WebBulletBase {

	public BridgeWebBullet(EntityType<? extends BridgeWebBullet> type, Level world) {
		super(type, world);
	}

	public BridgeWebBullet(LivingEntity owner, int tier) {
		super(RegAddonEntities.BRIDGE_WEB_BULLET, owner.level());
		this.owner = owner;
		this.Tier = tier;
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.10000000149011612, owner.getZ());
	}

	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		// 妗ｄ綅 鈫?鍘熺増澶╂閰嶇疆锛堟暟鍊间笌鍘熺増铚樿洓鎼矾涓€鑷达級
		AddonWebBridgeAction.WebLadderConfig src = switch (this.Tier) {
			case 3 -> ladderConfigTier3;
			case 2 -> ladderConfigTier2;
			default -> ladderConfigTier1;
		};
		if (!this.EnableTopBlockBuild) {
			// 涓庡師鐗堝榻愶細绂侀《寤烘椂鎶?TopBlockNum 娓呴浂
			src = new AddonWebBridgeAction.WebLadderConfig(src.SideBlockNum(), src.BottomBlockNum(), 0,
					src.LargerLadder(), src.LargerLadderCountPercent());
		}
		AddonWebBridgeAction.WebLadderConfig config = new AddonWebBridgeAction.WebLadderConfig(
				src.SideBlockNum(), src.BottomBlockNum(), src.TopBlockNum(),
				src.LargerLadder(), src.LargerLadderCountPercent());
		AddonWebBridgeAction.BuildWebLadder(this.level(), blockHitResult, config, RegAddonBlocks.TEMP_WEB_BRIDGE);
		playHitEffects();
		this.discard();
	}

	private void playHitEffects() {
		if (this.level() instanceof ServerLevel serverWorld) {
			serverWorld.sendParticles(ParticleTypes.CLOUD,
					this.getX(), this.getY(), this.getZ(),
					20, 0.3, 0.3, 0.3, 0.05);
			serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.WET_GRASS_BREAK, SoundSource.NEUTRAL, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
		}
	}
}
