package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.client.screen.SlottedFormationScreen;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.research.FormationResearchNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class FormationResearchClient {
    public static void register() {
        AddonNetwork.registerClientReceiver(github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent.SYNC, buf -> {
            var id = buf.readUUID();
            CompoundTag tag = buf.readNbt();
            if (tag != null) github.com.gengyoubo.sscfa.spell.FormationKnowledgeComponent.acceptClient(id, tag);
        });
        AddonNetwork.registerClientReceiver(FormationResearchNetworking.STATE, buf -> {
            int syncId = buf.readVarInt();
            CompoundTag state = buf.readNbt();
            Component message = buf.readComponent();
            boolean valid = buf.readBoolean();
            // 协议尾部 3 个 VarInt（node/family/level，服务端固定写 -1/-1/0）为旧版预留字段，读完丢弃以保持 buf 对齐
            buf.readVarInt();
            buf.readVarInt();
            buf.readVarInt();
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (state != null && client.screen instanceof SlottedFormationScreen screen
                        && screen.getScreenHandler().containerId == syncId) {
                    screen.accept(state, message, valid);
                }
            });
        });
    }
}
