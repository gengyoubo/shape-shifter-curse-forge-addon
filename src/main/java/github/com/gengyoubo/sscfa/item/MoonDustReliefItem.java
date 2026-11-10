package github.com.gengyoubo.sscfa.item;

import java.util.List;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
// [forge-port] removed yarn wildcard import net.minecraft.item.*
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import github.com.gengyoubo.sscfa.neomagic.NeoRuntime;
import org.jetbrains.annotations.Nullable;

/** 閺堝牆鐨归懡顖涙寜閸欘亪妾锋担搴ｅ箛閺堝绌跨憴锝呭閿涘奔绗夐弨鐟板綁鐎瑰鍙忛梼鍫濃偓绗衡偓浣歌埌閹浇绁┃鎰灗缁箖鈧哎鈧?*/
public final class MoonDustReliefItem extends Item {
    private static final BalanceReader BAL = new BalanceReader("systems.moon_dust_potions");
    private static final String DOSE_KEY = "MoonDustReliefAmount";
    public static final int COLOR = 0xA7EDF5;
    public static final MoonDustReliefItem VIAL = new MoonDustReliefItem("vial", 20, 16, false);
    public static final MoonDustReliefItem BOTTLE = new MoonDustReliefItem("bottle", 60, 40, false);
    public static final MoonDustReliefItem SPLASH = new MoonDustReliefItem("splash", 35, 0, true);
    // 閸樼喓澧楅懡顖涙寜鐎圭偘缍嬬拹鐔荤煑绾扮増鎸掗妴浣稿磹瀵板嫬娲撻弽鑹扳€滈崙蹇嬧偓浣割吂閹撮顏潻鍊熼嚋閸滃奔绻氱€涙ɑ浠径宥忕幢娑撳秶鏁ら崠鍨倳鐎圭偘缍嬮幋鏍у弿閺堝秶甯虹€硅埖澹傞幓蹇嬧偓?    
    public static final MobEffect RELIEF = new MobEffect(MobEffectCategory.BENEFICIAL, COLOR) {
        @Override public boolean isInstantenous() { return true; }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
        @Override public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity attacker,
                                                LivingEntity target, int amplifier, double proximity) {
            if (!(target instanceof ServerPlayer player)) return;
            double amount = SPLASH.relief();
            if (source instanceof ThrownPotion potion && potion.getItem().hasTag()
                    && potion.getItem().getTag().contains(DOSE_KEY)) {
                amount = potion.getItem().getTag().getDouble(DOSE_KEY);
            }
            // 閹舵洘骞忛弮璺烘彥閻撗冨闁插骏绱遍柌宥堟祰娑撳秳绱伴弨鐟板З瀹歌尙绮℃鐐插毉閻ㄥ嫯宓傚娣偓?            
            if (Double.isFinite(amount) && amount > 0 && Double.isFinite(proximity) && proximity > 0) {
                NeoRuntime.relieve(player, amount * Math.min(1, proximity));
            }
        }
    };

    private final String kind;
    private final double defaultRelief;
    private final int defaultTicks;
    private final boolean splash;

    private MoonDustReliefItem(String kind, double relief, int ticks, boolean splash) {
        super(new Item.Properties().stacksTo("vial".equals(kind) ? PotionTestTubes.DEFAULT_STACK_LIMIT : 1));
        this.kind = kind;
        this.defaultRelief = relief;
        this.defaultTicks = ticks;
        this.splash = splash;
    }

    public double relief() { return this == VIAL ? BOTTLE.relief() / PotionTestTubes.PORTIONS : BAL.d(kind + "_relief", defaultRelief); }
    public static boolean isBottled(ItemStack stack) { return stack.is(BOTTLE) || stack.is(SPLASH); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return splash ? UseAnim.NONE : UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) {
        return splash ? 0 : this == VIAL ? PotionTestTubes.drinkTicks(BOTTLE.getUseDuration(new ItemStack(BOTTLE))) : BAL.i(kind + "_drink_ticks", defaultTicks);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!splash) return ItemUtils.startUsingInstantly(world, player, hand);
        ItemStack held = player.getItemInHand(hand);
        if (!world.isClientSide) {
            if (!spawnThrownPotion(world, player, held)) return InteractionResultHolder.fail(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.getCooldowns().addCooldown(this, BAL.i("throw_cooldown_ticks", 10));
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(held, world.isClientSide);
    }

    /** 閹靛瀵旀稉搴ゅ祩濮樼顣伴崗杈╂暏閸氬奔绔村▎鈥冲闁插繐鎻╅悡褍鎷伴崢鐔哄瀵€涚秼閿涙稒澧搁悧鈺佹惂閵嗕礁鍠庨崡瀵告暠鐠嬪啰鏁ら崗銉ュ經婢跺嫮鎮婇妴?*/
    public boolean spawnThrownPotion(Level world, Player player, ItemStack source) {
        if (world.isClientSide || !splash || source.isEmpty() || !source.is(this)) return false;
        var potion = new ThrownPotion(world, player);
        ItemStack dose = source.copyWithCount(1);
        dose.getOrCreateTag().remove("Potion");
        dose.getTag().remove("CustomPotionEffects");
        PotionUtils.setCustomEffects(dose, List.of(new MobEffectInstance(RELIEF, 1)));
        if (!dose.getTag().contains("CustomPotionColor")) dose.getTag().putInt("CustomPotionColor", COLOR);
        dose.getTag().putDouble(DOSE_KEY, relief());
        potion.setItem(dose);
        potion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20, .5f, 1);
        if (!world.addFreshEntity(potion)) return false;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, .5f,
                .4f / (world.getRandom().nextFloat() * .4f + .8f));
        return true;
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (splash || stack.isEmpty() || !stack.is(this)) return stack;
        if (user instanceof ServerPlayer player) {
            NeoRuntime.relieve(player, relief());
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                Item container = this == VIAL ? PotionTestTubes.EMPTY : Items.GLASS_BOTTLE;
                if (stack.isEmpty()) return new ItemStack(container);
                player.getInventory().placeItemBackInInventory(new ItemStack(container));
            }
        }
        return stack;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level world,
                                        List<Component> lines, TooltipFlag context) {
        lines.add(Component.translatable(splash ? "tooltip.ssc_addon.moon_dust_splash"
                        : "tooltip.ssc_addon.moon_dust_relief", (int) relief()).withStyle(ChatFormatting.AQUA));
        if (this == VIAL) lines.add(Component.translatable("tooltip.ssc_addon.test_tube_stack").withStyle(ChatFormatting.GRAY));
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, new ResourceLocation("ssc_addon", "moon_dust_relief"), RELIEF);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("ssc_addon", "moon_dust_vial"), VIAL);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("ssc_addon", "moon_dust_bottle"), BOTTLE);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("ssc_addon", "moon_dust_splash"), SPLASH);
    }
}
