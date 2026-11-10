package github.com.gengyoubo.sscfa.client;

import net.minecraft.client.KeyMapping;

/** 键位系统占位（Forge）：返回 null，客户端按键检测将安全跳过。 */
public final class SscAddonKeybindings {
    private SscAddonKeybindings() {}

    public static KeyMapping getPrimaryKey() {
        return null;
    }

    public static KeyMapping getSecondaryKey() {
        return null;
    }

    public static boolean isSecondaryRawPressed() {
        return false;
    }

    public static void register() {
    }
}
