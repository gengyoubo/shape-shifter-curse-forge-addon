package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.MoistureWaterRecovery;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.function.DoubleConsumer;

/** 一次施法拥有一份湿润催化账本；只在实际治疗结算时工作，不增加常驻tick或网络轮询。 */
public final class MoistureHealing {
    private static final BalanceReader BAL=new BalanceReader("systems.form_casting");
    private final ServerPlayer caster;
    private final int baseCost,otherCost;
    private final double multiplier;
    private final Set<UUID> enhanced=new HashSet<>();
    private final ArrayList<Pending> pending=new ArrayList<>();
    private int depth;
    private boolean paidBase,unavailable;
    public int spent;
    private java.util.function.IntConsumer payment=amount->{};
    private record Pending(LivingEntity target,double amount,DoubleConsumer apply) {}

    private MoistureHealing(ServerPlayer caster) {
        this.caster=caster;baseCost=BAL.i("healing_moisture",10);otherCost=BAL.i("healing_other_moisture",5);
        multiplier=BAL.d("healing_multiplier",1.5);
    }
    public static MoistureHealing create(ServerPlayer caster) {
        return FormUtils.isForm(caster,FormIdentifiers.AXOLOTL_SP)?new MoistureHealing(caster):null;
    }
    public void onPayment(java.util.function.IntConsumer listener) {payment=listener;}
    public void begin() {depth++;}
    public void end() {
        if(depth<=0)throw new IllegalStateException("湿润催化批次未开始");
        if(--depth==0)flush();
    }
    public void abort() {pending.clear();depth=0;}
    /** apply接收最终治疗量；范围治疗在本批次汇总后统一支付，随后立刻执行。 */
    public void heal(LivingEntity target,double amount,DoubleConsumer apply) {
        if(!Double.isFinite(amount)||amount<=0||!eligible(target)) {apply.accept(amount);return;}
        pending.add(new Pending(target,amount,apply));
        if(depth==0)flush();
    }
    private boolean eligible(LivingEntity target) {
        return target!=null&&caster.isAlive()&&!caster.isRemoved()
                &&FormUtils.isForm(caster,FormIdentifiers.AXOLOTL_SP)
                &&target.isAlive()&&!target.isRemoved()&&target.level()==caster.level()
                &&target.getHealth()<target.getMaxHealth()&&WhitelistUtils.isBuffTarget(caster,target);
    }
    private void flush() {
        if(pending.isEmpty())return;
        var requests=new ArrayList<>(pending);pending.clear();
        Set<UUID> newcomers=new HashSet<>();int others=0;
        for(var request:requests)if(eligible(request.target)&&!enhanced.contains(request.target.getUUID())
                &&newcomers.add(request.target.getUUID())&&request.target!=caster)others++;
        if(!newcomers.isEmpty()&&!unavailable) {
            long price=(paidBase?0L:baseCost)+(long)otherCost*others;
            if(price<=Integer.MAX_VALUE&&MoistureWaterRecovery.consume(caster,(int)price)) {
                paidBase=true;enhanced.addAll(newcomers);spent+=(int)price;payment.accept((int)price);
            } else unavailable=true;
        }
        for(var request:requests)request.apply.accept(request.amount*(eligible(request.target)&&enhanced.contains(request.target.getUUID())?multiplier:1));
    }
}
