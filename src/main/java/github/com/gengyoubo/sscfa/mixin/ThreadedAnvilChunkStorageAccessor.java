package github.com.gengyoubo.sscfa.mixin;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 閺嗘挳婀堕幐澶夌瑯閻ｅ矁顔曠純顔款潎鐎电喕绐涚粋浼欑礄protected閿涘绱伴崣锝堫暟缂佹潙瀹抽崢瀣煂 3閿涘苯褰ч崝鐘烘祰閹村潡妫块崨銊ユ纯鐏忓繐灏崸妤嬬礉閺嶈涓嶆潻娑欏煣閸楋繝銆戦妴?*/
@Mixin(ChunkMap.class)
public interface ThreadedAnvilChunkStorageAccessor {
	@Invoker("setViewDistance")
	void sscAddon$setViewDistance(int watchDistance);
}
