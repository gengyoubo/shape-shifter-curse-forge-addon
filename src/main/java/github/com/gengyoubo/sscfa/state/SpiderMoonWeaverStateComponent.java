package github.com.gengyoubo.sscfa.state;

/** 月织蛛模式状态组件（Forge 简化）。 */
public final class SpiderMoonWeaverStateComponent {
    public static final int MODE_BRIDGE = 0;
    public static final int MODE_ATTACK = 1;

    private int mode = MODE_BRIDGE;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
