package github.com.gengyoubo.sscfa.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 月尘魔法书键位（Forge 版）：默认未绑定，玩家在「控制」中自定义?*/
public final class SpellcastKeybindings {
	public static final String CATEGORY = "key.categories.ssc_addon";

	public static KeyMapping KEY_SWITCH;
	public static KeyMapping KEY_CAST;
	public static KeyMapping KEY_EVOLUTION;
	public static final KeyMapping[] KEY_DIRECT = new KeyMapping[7];

	private SpellcastKeybindings() {}

	public static void init() {
		KEY_SWITCH = new KeyMapping("key.ssc_addon.spell_switch", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		KEY_CAST = new KeyMapping("key.ssc_addon.spell_cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		KEY_EVOLUTION = new KeyMapping("key.ssc_addon.evolution", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		for (int i = 0; i < 7; i++) {
			KEY_DIRECT[i] = new KeyMapping("key.ssc_addon.spell_cast_" + (i + 1),
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
		}
	}

	public static void register(RegisterKeyMappingsEvent event) {
		event.register(KEY_SWITCH);
		event.register(KEY_CAST);
		event.register(KEY_EVOLUTION);
		for (KeyMapping mapping : KEY_DIRECT) event.register(mapping);
	}
}
