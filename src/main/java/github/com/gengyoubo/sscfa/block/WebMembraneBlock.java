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
 * 蛛网膜：多面贴附的薄层蛛网（机制同发光地衣），放置后 60~90 秒随机消失。
 *
 * <p>迁移占位：保留方块状态、贴附与定时消散；接触减速、「蜘网缠身」、施法者白名单与高亮
 * 依赖形态/白名单/网络系统，待其迁移后从 Fabric 版 WebMembraneBlock 补回。</p>
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
}
