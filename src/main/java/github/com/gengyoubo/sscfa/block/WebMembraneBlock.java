package github.com.gengyoubo.sscfa.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铔涚綉鑶滐細澶氶潰璐撮檮鐨勮杽灞傝洓缃戯紙鏈哄埗鍚屽彂鍏夊湴琛ｏ級锛屾斁缃悗 60~90 绉掗殢鏈烘秷澶便€? *
 * <p>杩佺Щ鍗犱綅锛氫繚鐣欐柟鍧楃姸鎬併€佽创闄勪笌瀹氭椂娑堟暎锛涙帴瑙﹀噺閫熴€併€岃湗缃戠紶韬€嶃€佹柦娉曡€呯櫧鍚嶅崟涓庨珮浜? * 渚濊禆褰㈡€?鐧藉悕鍗?缃戠粶绯荤粺锛屽緟鍏惰縼绉诲悗浠?Fabric 鐗?WebMembraneBlock 琛ュ洖銆?/p>
 */
@SuppressWarnings("deprecation")
public class WebMembraneBlock extends MultifaceBlock {
	public static final int LIFESPAN_MIN = 1200;
	public static final int LIFESPAN_MAX = 1800;
	private final MultifaceSpreader spreader = new MultifaceSpreader(this);

	public WebMembraneBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MultifaceSpreader getSpreader() {
		return spreader;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		super.onPlace(state, level, pos, oldState, moved);
		if (!level.isClientSide) {
			level.scheduleTick(pos, this, LIFESPAN_MIN + level.getRandom().nextInt(LIFESPAN_MAX - LIFESPAN_MIN + 1));
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!level.isClientSide && !newState.is(this)) WebMembraneOwners.remove(level, pos);
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.is(this)) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			WebMembraneOwners.remove(level, pos);
		}
	}

    public static void coatArea(net.minecraft.server.level.ServerLevel world, net.minecraft.core.BlockPos pos, double radius, java.util.UUID owner) {}
}
