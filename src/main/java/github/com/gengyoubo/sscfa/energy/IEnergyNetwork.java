package github.com.gengyoubo.sscfa.energy;

import java.util.List;

/** 已发现的一组导通节点；网络对象不保存能量，能量由储能节点保存。 */
public interface IEnergyNetwork {
    /** 发现网络时的节点快照。拓扑改变后应重新获取网络。 */
    List<IEnergyNode> nodes();
    int getStoredEnergy();
    int getCapacity();
    /** 返回实际存入的能量；非正数请求返回零。 */
    int insert(int amount);
    /** 返回实际取出的能量；非正数请求返回零。 */
    int extract(int amount);
}
