package github.com.gengyoubo.sscfa.gametest;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.anomaly.AnomalyReactionFeedback;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnomalyFeedbackGameTests {
    @GameTest(template = "gametest_floor")
    public static void baseAndReactionHaveSeparateSoundLimits(GameTestHelper helper) {
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        var sounds = new ArrayList<SoundEvent>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == helper.getLevel()) sounds.add(event.getSound().value());
        };
        AnomalyReactionFeedback.clear(helper.getLevel());
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            AnomalyReactionFeedback.showBase("fire", target);
            AnomalyReactionFeedback.showBase("fire", target);
            AnomalyReactionFeedback.show("fire", "ice", target, null);
            AnomalyReactionFeedback.show("ice", "fire", target, null);
            helper.assertTrue(sounds.size() == 2, "each sound should play once per cell and window: " + sounds);
            helper.assertTrue(sounds.get(0) == SoundEvents.BLAZE_SHOOT && sounds.get(1) == SoundEvents.FIRE_EXTINGUISH,
                    "base and reaction should use distinct sounds");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
            AnomalyReactionFeedback.clear(helper.getLevel());
        }
        helper.succeed();
    }
}
