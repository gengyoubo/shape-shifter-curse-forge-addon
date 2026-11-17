package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.block.RegAddonBlocks;
import github.com.gengyoubo.sscfa.block.WebMembraneOwners;
import github.com.gengyoubo.sscfa.resource.BarKeys;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
// [forge-port] removed yarn wildcard import net.minecraft.util.math.*
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.compat.EntangledEffectUtils;

import java.util.*;

/** 蜘蛛施法联动：仅起手/实际碰撞/跳杀命中工作，不添加每tick扫描或网络消息。 */
public final class SpiderSpellLink {
    private static final BalanceReader BAL = new BalanceReader("systems.form_casting");
    private SpiderSpellLink() {}
    public static int webCost() { return BAL.i("moon_weaver_projectile_mana",8); }
    public static boolean canWeave(ServerPlayer p) {
        return FormUtils.isForm(p,FormIdentifiers.SPIDER_MOON_WEAVER)
                &&ResourceBars.get(p,BarKeys.VANILLA_MANA)>=webCost();
    }
    public interface WebProjectile { Impact ssca$weave(); }
    public static final class RollGroup {
        public final UUID id=UUID.randomUUID();
        public final Set<UUID> targets=new HashSet<>();
    }
    /** Only casts with extant saved projectile entities occupy this ledger; unloading is not destruction. */
    private static final class WeaveLedger extends SavedData {
        private static final String KEY="ssca_spell_weave_hits";
        private static final class Entry {
            final Set<UUID> projectiles=new HashSet<>(),targets=new HashSet<>();
        }
        private final Map<UUID,Entry> casts=new HashMap<>();
        static WeaveLedger get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(WeaveLedger::read,WeaveLedger::new,KEY);
        }
        static WeaveLedger read(CompoundTag nbt) {
            WeaveLedger result=new WeaveLedger();
            for(String key:nbt.getAllKeys())try {
                Entry entry=new Entry();CompoundTag data=nbt.getCompound(key);
                readSet(data,"Projectiles",entry.projectiles);readSet(data,"Targets",entry.targets);
                if(!entry.projectiles.isEmpty())result.casts.put(UUID.fromString(key),entry);
            }catch(IllegalArgumentException ignored){}
            return result;
        }
        static void readSet(CompoundTag data,String key,Set<UUID> into) {
            ListTag list=data.getList(key,Tag.TAG_STRING);
            for(int i=0;i<list.size();i++)try{into.add(UUID.fromString(list.getString(i)));}catch(IllegalArgumentException ignored){}
        }
        static ListTag writeSet(Set<UUID> values) {ListTag list=new ListTag();values.forEach(id->list.add(StringTag.valueOf(id.toString())));return list;}
        @Override public CompoundTag save(CompoundTag nbt) {
            casts.forEach((id,entry)->{CompoundTag data=new CompoundTag();data.put("Projectiles",writeSet(entry.projectiles));data.put("Targets",writeSet(entry.targets));nbt.put(id.toString(),data);});return nbt;
        }
        void join(UUID cast,UUID projectile) {
            if(casts.computeIfAbsent(cast,k->new Entry()).projectiles.add(projectile))setDirty();
        }
        void leave(UUID cast,UUID projectile) {
            Entry entry=casts.get(cast);if(entry==null||!entry.projectiles.remove(projectile))return;
            if(entry.projectiles.isEmpty())casts.remove(cast);setDirty();
        }
        boolean draw(UUID cast,UUID target) {
            Entry entry=casts.get(cast);
            if(entry==null||entry.targets.size()>=256||!entry.targets.add(target))return false;
            setDirty();return true;
        }
    }
    public static void register() {
        github.com.gengyoubo.sscfa.util.SscEvents.onEntityUnload((entity,world)->{
            var reason=entity.getRemovalReason();
            if((reason==net.minecraft.world.entity.Entity.RemovalReason.DISCARDED||reason==net.minecraft.world.entity.Entity.RemovalReason.KILLED)
                    &&entity instanceof WebProjectile projectile&&world instanceof ServerLevel server)projectile.ssca$weave().destroyed(server,entity.getUUID());
        });
    }
    /** 每颗弹体的服务端快照；穿刺曾直击实体后，后续落地也不铺网。 */
    public static final class Impact {
        private boolean enabled,direct;
        private Set<UUID> rolled;
        private UUID cast;
        public void enable() {enabled=true;}
        public void enable(UUID group) {enabled=true;cast=group;}
        public void enable(Set<UUID> group) {enabled=true;rolled=group;}
        public void loaded(ServerLevel world,UUID projectile) {if(cast!=null)WeaveLedger.get(world.getServer()).join(cast,projectile);}
        public void destroyed(ServerLevel world,UUID projectile) {if(cast!=null)WeaveLedger.get(world.getServer()).leave(cast,projectile);}
        public boolean enabled() {return enabled;}
        public void direct() {if(enabled)direct=true;}
        public void hit(ServerPlayer caster,LivingEntity target) {
            if(!enabled)return;
            direct=true;
            if(!FormSpellStyle.legalTarget(caster,target))return;
            if(cast!=null){if(!WeaveLedger.get(caster.getServer()).draw(cast,target.getUUID()))return;}
            else {
                if(rolled==null)rolled=new HashSet<>();
                if(rolled.size()>=256||!rolled.add(target.getUUID()))return;
            }
            if(caster.getRandom().nextDouble()<BAL.d("moon_weaver_cocoon_chance",.15))
                EntangledEffectUtils.applyEntangledEffect(caster,target,
                        EntangledEffectUtils.ENTANGLED_DURATION_PER_LEVEL*(EntangledEffectUtils.ENTANGLED_MAX_LEVEL+1));
        }
        public void ground(ServerPlayer caster,BlockHitResult hit) {
            if(!enabled||direct||hit.getDirection()!=Direction.UP)return;
            // 一次碰撞只处理一次；不影响已存在的蛛网膜及其寿命/归属。
            enabled=false;
            ServerLevel world=caster.serverLevel();BlockPos center=hit.getBlockPos().above();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
                BlockPos pos=center.offset(x,0,z),floor=pos.below();
                if(!world.isInWorldBounds(pos)||!world.hasChunkAt(pos)||!caster.mayBuild()
                        ||!world.mayInteract(caster,pos)||!world.getBlockState(pos).isAir()
                        ||!world.getBlockState(floor).isFaceSturdy(world,floor,Direction.UP))continue;
                boolean wet=false;
                for(Direction d:Direction.values())if(!world.getFluidState(pos.relative(d)).isEmpty()){wet=true;break;}
                if(wet)continue;
                var state=RegAddonBlocks.WEB_MEMBRANE.defaultBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN),true);
                if(world.setBlockAndUpdate(pos,state))WebMembraneOwners.set(world,pos,caster.getUUID());
            }
        }
        public void write(CompoundTag nbt) {
            if(!enabled&&!direct)return;
            nbt.putBoolean("SscaWeaveEnabled",enabled);nbt.putBoolean("SscaWeaveDirect",direct);
            if(cast!=null)nbt.putUUID("SscaWeaveCast",cast);
            if(rolled!=null&&!rolled.isEmpty()){ListTag list=new ListTag();rolled.forEach(id->list.add(StringTag.valueOf(id.toString())));nbt.put("SscaWeaveRolled",list);}
        }
        public void read(CompoundTag nbt) {
            enabled=nbt.getBoolean("SscaWeaveEnabled");direct=nbt.getBoolean("SscaWeaveDirect");rolled=null;
            cast=nbt.hasUUID("SscaWeaveCast")?nbt.getUUID("SscaWeaveCast"):null;
            ListTag list=nbt.getList("SscaWeaveRolled",Tag.TAG_STRING);
            if(!list.isEmpty())rolled=new HashSet<>();
            for(int i=0;i<Math.min(256,list.size());i++)try{rolled.add(UUID.fromString(list.getString(i)));}catch(IllegalArgumentException ignored){}
        }
    }
    public static void directHit(Projectile projectile,LivingEntity target) {
        if(projectile.getOwner() instanceof ServerPlayer caster)
            ((WebProjectile)projectile).ssca$weave().hit(caster,target);
    }
    /** 本次被动的冷却和消耗乘区；aim仅在构造效果时临时使用，不移动玩家视角。 */
    public record Trigger(Vec3 aim,boolean free) {
        public double growth() {return free?0:BAL.d("salticidae_curse_collapse_multiplier",.2);}
        public int cooldown(int normal) {return Math.max(1,(int)Math.ceil(normal*BAL.d("salticidae_curse_cooldown_multiplier",.1)));}
        public void effect(ServerPlayer p,Runnable action) {
            float yaw=p.getYRot(),pitch=p.getXRot();Vec3 delta=aim.subtract(p.getEyePosition());
            if(delta.lengthSqr()>1e-8){p.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));}
            try{action.run();}finally{p.setYRot(yaw);p.setXRot(pitch);}
        }
    }
    /** selected可用优先；否则随机一个书内非红色诅咒槽；无可用槽返回-1走白色兜底。 */
    public static int chooseSlot(ServerPlayer p,net.minecraft.world.item.ItemStack book) {
        if(book==null||book.isEmpty())return -1;
        List<Integer> ready=new ArrayList<>();int selected=SpellbookData.getSelectedSlot(book);
        for(int slot=0;slot<SpellbookData.getSlotCount(book);slot++) {
            var scroll=SpellbookData.getScroll(book,slot);var spell=ScrollData.getSpell(scroll);
            if(spell==null||ArcaneAnalysis.isUnanalyzed(scroll)||spell.getElement()!=FormationElement.CURSE
                    ||spell.getRarity(ScrollData.getCastLevel(scroll))==SpellRarity.RED
                    ||SharedSpellCooldowns.getEffectiveCooldownEnd(p,scroll)>p.level().getGameTime())continue;
            if(slot==selected)return slot;ready.add(slot);
        }
        return ready.isEmpty()?-1:ready.get(p.getRandom().nextInt(ready.size()));
    }
    public static void onJumpHit(ServerPlayer p,LivingEntity target) {
        if(!FormUtils.isForm(p,FormIdentifiers.SPIDER_SALTICIDAE)||target.level()!=p.level()
                ||target==p||target.isRemoved()||target.isSpectator()
                ||github.com.gengyoubo.sscfa.util.WhitelistUtils.isProtected(p,target)
                ||DomainManager.blocksTargeting(p,target))return;
        var book=SpellCastManager.getEquippedBook(p);int slot=chooseSlot(p,book);
        if(slot>=0){SpellCastManager.triggerSpiderCurse(p,slot,null,target);return;}
        List<Spell> white=SpellRegistry.all().stream().filter(s->s.getElement()==FormationElement.CURSE&&s.getRarity(1)==SpellRarity.WHITE).toList();
        if(!white.isEmpty())SpellCastManager.triggerSpiderCurse(p,-1,white.get(p.getRandom().nextInt(white.size())),target);
    }
}
