package github.com.gengyoubo.sscfa.mixin;

import net.onixary.shapeShifterCurseForge.client.codex.CodexData;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.EvolutionComponent;
import github.com.gengyoubo.sscfa.evolution.EvolutionNode;
import github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Forge 闁插秴鍟撻敍姘崇箻閸栨牔濞囨?鏉╂稑瀵茬紘搴ゃ偪閾诲牏娈戞稊锕傘€夋径鏍潎鏉╄棄濮為崝鐘靛仯閸欐瑨鍫敍鍫㈡窗閺嶅洦鏁兼稉?Forge CodexData閿涘鈧?*/
@Mixin(value = CodexData.class, remap = false)
public class SscAddonCodexStatusMixin {

    @Inject(target = @Desc(value = "getContentText", args = {CodexData.ContentType.class, Player.class}, ret = Component.class),
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void appendEvolutionToAppearance(CodexData.ContentType type, Player player,
                                                    CallbackInfoReturnable<Component> cir) {
        if (type != CodexData.ContentType.APPEARANCE) {
            return;
        }
        FormDefinition currentForm = FormManager.current(player);
        if (currentForm == null || currentForm.id() == null) {
            return;
        }

        List<EvolutionNode> nodes;
        String baseNodeId;
        if (FormIdentifiers.UPGRADE_FAMILIAR_FOX.equals(currentForm.id())) {
            nodes = FamiliarFoxTree.nodes();
            baseNodeId = FamiliarFoxTree.NODE_BASE;
        } else if (FormIdentifiers.UPGRADE_AXOLOTL.equals(currentForm.id())) {
            nodes = AxolotlTree.nodes();
            baseNodeId = AxolotlTree.NODE_BASE;
        } else {
            return;
        }
        if (nodes.isEmpty()) {
            return;
        }

        EvolutionComponent comp = RegEvolutionComponent.EVOLUTION.get(player);
        if (comp == null) {
            return;
        }
        Component base = cir.getReturnValue();
        if (base == null) {
            return;
        }

        int unlockableCount = 0;
        int unlockedCount = 0;
        for (EvolutionNode node : nodes) {
            if (baseNodeId.equals(node.id)) {
                continue;
            }
            unlockableCount++;
            if (comp.isUnlocked(node.id)) {
                unlockedCount++;
            }
        }

        MutableComponent dynamic = Component.empty();
        dynamic.append(base);
        dynamic.append(Component.literal("\n\n"));
        dynamic.append(Component.translatable("text.ssc_addon.evolution.book.summary_title"));
        dynamic.append(Component.literal("\n"));
        dynamic.append(Component.translatable("text.ssc_addon.evolution.book.summary_stats",
                unlockedCount, unlockableCount, comp.getPoints(), player.experienceLevel));
        for (EvolutionNode node : nodes) {
            if (baseNodeId.equals(node.id)) {
                continue;
            }
            if (!comp.isUnlocked(node.id)) {
                continue;
            }
            dynamic.append(Component.literal("\n\n\u2022 "));
            dynamic.append(Component.translatable(node.nameKey));
            dynamic.append(Component.literal("\n"));
            String bookKey = node.descKey.substring(0, node.descKey.length() - 5) + ".book";
            dynamic.append(Component.translatable(bookKey));
        }
        cir.setReturnValue(dynamic);
    }
}
