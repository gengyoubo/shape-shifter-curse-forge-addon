package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 法阵知识组件注册占位：按玩家 UUID 保存。 */
public final class RegFormationKnowledgeComponent {
    public static final Holder FORMATION_KNOWLEDGE = new Holder();

    private RegFormationKnowledgeComponent() {}

    public static final class Holder {
        public FormationKnowledgeComponent get(Player player) {
            return FormationKnowledgeComponent.get(player);
        }

        public void sync(ServerPlayer player) {
        }
    }
}
