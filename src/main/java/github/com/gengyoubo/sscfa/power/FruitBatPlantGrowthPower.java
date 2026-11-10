package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 寄生果蝠被动 {@code my_addon:fruit_bat_plant_growth}：每 {@code tick_interval} tick 给身周 {@code radius} 格内的
 * 农作物各按 {@code chance} 概率催生一次 randomTick。
 *
 * <p>跨玩家不叠加：同一坐标在一个周期内只会被任一玩家判定一次。</p>
 */
public final class FruitBatPlantGrowthPower implements SscPowerType {
    public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "fruit_bat_plant_growth");

    /** 维度 -> (方块坐标 -> 上次判定时的世界 tick) */
    private static final Map<ServerLevel, Map<Long, Long>> RECENT_PROCESSED = new WeakHashMap<>();

    @Override
    public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
        int tickInterval = Math.max(1, FormPowerRuntime.intValue(data, "tick_interval", 20));
        if (player.tickCount % tickInterval != 0) return;
        if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) return;
        int radius = Math.max(0, FormPowerRuntime.intValue(data, "radius", 5));
        float chance = Math.max(0f, Math.min(1f, FormPowerRuntime.floatValue(data, "chance", 0.05f)));

        ServerLevel world = player.serverLevel();
        long now = world.getGameTime();
        Map<Long, Long> dimMap = RECENT_PROCESSED.computeIfAbsent(world, k -> new ConcurrentHashMap<>());

        // 顺手清理过老条目（5 倍周期之外）防止长期累积内存
        long expire = now - (long) tickInterval * 5L;
        dimMap.entrySet().removeIf(e -> e.getValue() < expire);

        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(cursor);
                    if (!isFarmable(state)) continue;

                    long packed = cursor.asLong();
                    Long last = dimMap.get(packed);
                    // 同一周期内已被任一玩家判定过 -> 跳过，避免叠加
                    if (last != null && now - last < tickInterval) continue;

                    // 无论概率是否命中都标记本格，否则其他玩家会再判一次造成叠加
                    dimMap.put(packed, now);

                    if (world.random.nextFloat() >= chance) continue;
                    state.randomTick(world, cursor.immutable(), world.random);
                }
            }
        }
    }

    /** 涵盖 1.20.1 中的农作物方块类型。 */
    private static boolean isFarmable(BlockState state) {
        Block b = state.getBlock();
        return b instanceof CropBlock           // 小麦/胡萝卜/土豆/甜菜/火把花/瓶子草
                || b instanceof StemBlock        // 西瓜/南瓜茎
                || b instanceof CocoaBlock       // 可可豆
                || b instanceof SugarCaneBlock   // 甘蔗
                || b instanceof BambooStalkBlock // 竹子
                || b instanceof BambooSaplingBlock
                || b instanceof NetherWartBlock  // 地狱疣
                || b instanceof SaplingBlock     // 树苗
                || b instanceof SweetBerryBushBlock // 甜浆果丛
                || b instanceof CaveVinesPlantBlock // 洞穴藤蔓主体
                || b instanceof CaveVinesBlock;  // 洞穴藤蔓头（发光浆果生长）
    }
}
