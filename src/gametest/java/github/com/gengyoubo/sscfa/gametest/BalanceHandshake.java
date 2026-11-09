package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.balance.ServerBalanceState;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;

/**
 * 假玩家没有真实连接，不会确认 balance 同步，依赖 {@link BalanceIntegration#isPlayerReady} 的技能对它不可用。
 * 这里在测试里走一遍真实客户端的握手（登记加入 → 生成发送序列 → 按当前版本确认），不在正式代码里留后门。
 */
final class BalanceHandshake {
    private BalanceHandshake() {}

    static void complete(ServerPlayer player) {
        try {
            ServerBalanceState state = (ServerBalanceState) field("server").get(null);
            String tree = (String) field("serverTreeJson").get(null);
            state.playerJoined(player.getUUID());
            state.buildSendSequence(player.getUUID(), tree);
            state.playerAcked(player.getUUID(), state.revision());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BalanceIntegration fields changed; update the game test handshake", e);
        }
        if (!BalanceIntegration.isPlayerReady(player)) throw new IllegalStateException("balance handshake did not mark the player ready");
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = BalanceIntegration.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
