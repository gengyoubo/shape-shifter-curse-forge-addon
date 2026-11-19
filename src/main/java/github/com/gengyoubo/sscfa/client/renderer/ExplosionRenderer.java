package github.com.gengyoubo.sscfa.client.renderer;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * 爆裂魔法客户端表现入口。
 *
 * <p>Forge 端尚未移植爆裂的光柱/法阵几何、主题音频及 {@code ssc_addon:explosion_start} 接收器；
 * 由于客户端未注册该通道，服务端不会下发爆裂序列，因此客户端不持有任何需要中止的爆裂表现。
 * 超距失败时的音效反馈由 {@code ExplosionEffects.playSuppressionCues} 负责，蓄力音由
 * {@code SpellChargeSoundInstance.stopFor} 按施法者停止。移植渲染器时须在此按序列 ID
 * 中止对应表现（只停该序列，不清理其他玩家的爆裂）。</p>
 */
public final class ExplosionRenderer {
    private ExplosionRenderer() {}

    public static void interruptForRange(UUID sequence, ClientLevel level, long now) {
        // 无本地爆裂序列可中止，见类注释。
    }
}
