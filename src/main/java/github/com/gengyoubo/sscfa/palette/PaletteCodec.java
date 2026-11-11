package github.com.gengyoubo.sscfa.palette;

/**
 * 形态配色「分享码」编解码工具：5 个 RGBA 颜色 + 3 个 greyReverse 布尔打包为固定长度 ASCII 文本。
 */
public final class PaletteCodec {
    private PaletteCodec() {
    }

    public static final String PREFIX = "SSCA-";
    public static final int EXPECTED_LENGTH = 56;

    public static class DecodeException extends RuntimeException {
        public final String langKey;
        public final Object[] args;

        public DecodeException(String langKey, Object... args) {
            super(langKey);
            this.langKey = langKey;
            this.args = args;
        }
    }

    public static String encode(int primaryRGBA, int accent1RGBA, int accent2RGBA, int eyeARGBA, int eyeBRGBA,
                                boolean primaryGreyReverse, boolean accent1GreyReverse, boolean accent2GreyReverse) {
        int flags = (primaryGreyReverse ? 1 : 0) | (accent1GreyReverse ? 2 : 0) | (accent2GreyReverse ? 4 : 0);
        return PREFIX
                + hex8(primaryRGBA) + "-"
                + hex8(accent1RGBA) + "-"
                + hex8(accent2RGBA) + "-"
                + hex8(eyeARGBA) + "-"
                + hex8(eyeBRGBA) + "-"
                + Integer.toHexString(flags).toUpperCase();
    }

    public static PaletteData decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) throw new DecodeException("ssc_addon.palette.error.empty");
        String code = raw.trim().toUpperCase();
        if (!code.startsWith(PREFIX)) throw new DecodeException("ssc_addon.palette.error.bad_prefix");
        String[] parts = code.substring(PREFIX.length()).split("-");
        if (parts.length != 6) throw new DecodeException("ssc_addon.palette.error.bad_segments");
        for (int i = 0; i < 5; i++) {
            if (parts[i].length() != 8) {
                throw new DecodeException("ssc_addon.palette.error.bad_color_length", i + 1);
            }
        }
        if (parts[5].length() != 1) throw new DecodeException("ssc_addon.palette.error.bad_flag_length");
        try {
            int primary = parseHex8(parts[0]);
            int accent1 = parseHex8(parts[1]);
            int accent2 = parseHex8(parts[2]);
            int eyeA = parseHex8(parts[3]);
            int eyeB = parseHex8(parts[4]);
            int flags = Integer.parseInt(parts[5], 16);
            if (flags < 0 || flags > 7) throw new DecodeException("ssc_addon.palette.error.bad_flag_range");
            return new PaletteData(primary, accent1, accent2, eyeA, eyeB,
                    (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0);
        } catch (NumberFormatException e) {
            throw new DecodeException("ssc_addon.palette.error.hex_parse", e.getMessage());
        }
    }

    private static String hex8(int v) {
        return String.format("%08X", v);
    }

    private static int parseHex8(String s) {
        return Integer.parseUnsignedInt(s, 16);
    }

    public record PaletteData(int primaryRGBA, int accent1RGBA, int accent2RGBA, int eyeARGBA, int eyeBRGBA,
                              boolean primaryGreyReverse, boolean accent1GreyReverse, boolean accent2GreyReverse) {}
}
