package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.SscAddon;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** 变羊期间锁定第三人称背后视角；结束恢复原本的第一人称。 */
public final class SheepFormCameraController {
    private static CameraType originalPerspective = null;

    private SheepFormCameraController() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            originalPerspective = null;
            return;
        }
        boolean sheep = client.player.hasEffect(SscAddon.SHEEP_FORM);
        if (sheep) {
            if (originalPerspective == null) {
                originalPerspective = client.options.getCameraType();
            }
            if (client.options.getCameraType() == CameraType.FIRST_PERSON) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (originalPerspective != null) {
            if (originalPerspective == CameraType.FIRST_PERSON) {
                client.options.setCameraType(CameraType.FIRST_PERSON);
            }
            originalPerspective = null;
        }
    }
}
