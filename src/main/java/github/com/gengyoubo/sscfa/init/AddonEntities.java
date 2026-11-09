package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.entity.GhostCatEntity;
import github.com.gengyoubo.sscfa.entity.ShellMob;
import github.com.gengyoubo.sscfa.entity.ShellProjectile;
import github.com.gengyoubo.sscfa.entity.BeepSheepEntity;
import github.com.gengyoubo.sscfa.entity.FrostArrayEntity;
import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import github.com.gengyoubo.sscfa.entity.LaserBeamEntity;
import github.com.gengyoubo.sscfa.entity.TidalOrbEntity;
import github.com.gengyoubo.sscfa.entity.FrostStormEntity;
import github.com.gengyoubo.sscfa.entity.SpellCurseMarkEntity;
import github.com.gengyoubo.sscfa.entity.SpellFireBoltEntity;
import github.com.gengyoubo.sscfa.entity.SpellFrostSpikeEntity;
import github.com.gengyoubo.sscfa.entity.SpellMeteorEntity;
import github.com.gengyoubo.sscfa.entity.SpellMoonlightArrowEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SSCA 实体注册（对?Fabric ??SscAddon / RegAddonEntities）。尺寸与追踪参数照搬原版?? * 追踪范围由方块换算为区块（向上取整）??ghost_cat 外均为空壳，?{@link ShellProjectile} / {@link ShellMob}?? */
public final class AddonEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SscAddonForge.MOD_ID);
    private static final List<RegistryObject<EntityType<ShellProjectile>>> SHELL_PROJECTILES = new ArrayList<>();

    // ===== 投射?/ 场地实体（空??=====
    public static final RegistryObject<EntityType<ShellProjectile>> FROST_BALL = projectile("frost_ball", 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<SpellFrostSpikeEntity>> SPELL_FROST_SPIKE = entity("spell_frost_spike", SpellFrostSpikeEntity::new, 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<SpellFireBoltEntity>> SPELL_FIRE_BOLT = entity("spell_fire_bolt", SpellFireBoltEntity::new, 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<SpellMeteorEntity>> SPELL_METEOR = entity("spell_meteor", SpellMeteorEntity::new, 0.5f, 0.5f, 4, 10);
    public static final RegistryObject<EntityType<SpellMoonlightArrowEntity>> SPELL_MOONLIGHT_ARROW = entity("spell_moonlight_arrow", SpellMoonlightArrowEntity::new, 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<SpellCurseMarkEntity>> SPELL_CURSE_MARK = entity("spell_curse_mark", SpellCurseMarkEntity::new, 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<BeepSheepEntity>> BEEP_SHEEP = entity("beep_sheep", BeepSheepEntity::new, 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<ShellProjectile>> LUNAR_SPIRIT_BOLT = projectile("lunar_spirit_bolt", 0.2f, 0.2f, 4, 10);
    public static final RegistryObject<EntityType<ShellProjectile>> THROWN_WATER_SPEAR = projectile("thrown_water_spear", 0.4f, 0.4f, 4, 10);
    public static final RegistryObject<EntityType<FrostThornEntity>> FROST_THORN = entity("frost_thorn", FrostThornEntity::new, 0.3f, 0.3f, 8, 1);
    public static final RegistryObject<EntityType<FrostArrayEntity>> FROST_ARRAY = entity("frost_array", FrostArrayEntity::new, 0.3f, 0.3f, 4, 1);
    public static final RegistryObject<EntityType<ShellProjectile>> FOX_FIREBALL = projectile("fox_fireball", 0.25f, 0.25f, 4, 2);
    public static final RegistryObject<EntityType<ShellProjectile>> INFECTION_SPORE_BOMB = projectile("infection_spore_bomb", 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<ShellProjectile>> PARASITIC_SEED = projectile("parasitic_seed", 0.25f, 0.25f, 4, 10);
    public static final RegistryObject<EntityType<FrostStormEntity>> FROST_STORM = entity("frost_storm", FrostStormEntity::new, 1.0f, 2.0f, 4, 10);
    public static final RegistryObject<EntityType<TidalOrbEntity>> TIDAL_ORB = entity("tidal_orb", TidalOrbEntity::new, 0.5f, 0.5f, 4, 1);
    public static final RegistryObject<EntityType<LaserBeamEntity>> LASER_BEAM = entity("laser_beam", LaserBeamEntity::new, 0.5f, 0.5f, 6, 1);
    public static final RegistryObject<EntityType<ShellProjectile>> WATER_SPEAR = projectile("water_spear", 0.5f, 0.5f, 1, 20);
    public static final RegistryObject<EntityType<ShellProjectile>> FRIEND_MARKER = projectile("friend_marker", 0.25f, 0.25f, 1, 10);
    public static final RegistryObject<EntityType<ShellProjectile>> CLEAR_FRIEND_MARKER = projectile("clear_friend_marker", 0.25f, 0.25f, 1, 10);
    public static final RegistryObject<EntityType<ShellProjectile>> BRIDGE_WEB_BULLET = projectile("bridge_web_bullet", 0.5f, 0.5f, 10, 1);
    public static final RegistryObject<EntityType<ShellProjectile>> WEB_MEMBRANE_BULLET = projectile("web_membrane_bullet", 0.5f, 0.5f, 10, 1);
    public static final RegistryObject<EntityType<ShellProjectile>> SPIDER_SWING_BULLET = projectile("spider_swing_bullet", 0.4f, 0.4f, 10, 1);

    // ===== 鐢??=====
    public static final RegistryObject<EntityType<ShellMob>> LUNAR_SPIRIT = ENTITIES.register("lunar_spirit", () ->
            EntityType.Builder.<ShellMob>of(ShellMob::new, MobCategory.MISC).sized(0.4f, 0.6f)
                    .clientTrackingRange(4).updateInterval(1).build("lunar_spirit"));
    public static final RegistryObject<EntityType<GhostCatEntity>> GHOST_CAT = ENTITIES.register("ghost_cat", () ->
            EntityType.Builder.<GhostCatEntity>of(GhostCatEntity::new, MobCategory.MISC).sized(0.6f, 0.8f)
                    .clientTrackingRange(1).updateInterval(2).build("ghost_cat"));
    public static final RegistryObject<EntityType<ShellMob>> WITCH_FAMILIAR = ENTITIES.register("witch_familiar", () ->
            EntityType.Builder.<ShellMob>of(ShellMob::new, MobCategory.MONSTER).sized(0.5f, 0.7f)
                    .clientTrackingRange(4).updateInterval(3).build("witch_familiar"));
    public static final RegistryObject<EntityType<ShellMob>> AXOLOTL_SHIFTER = ENTITIES.register("axolotl_shifter", () ->
            EntityType.Builder.<ShellMob>of(ShellMob::new, MobCategory.AXOLOTLS).sized(0.6f, 1.8f)
                    .clientTrackingRange(4).updateInterval(3).build("axolotl_shifter"));

    private AddonEntities() {}

    public static List<RegistryObject<EntityType<ShellProjectile>>> shellProjectiles() {
        return Collections.unmodifiableList(SHELL_PROJECTILES);
    }

    /** 模组总线：为生物注册默认属性（缺失会在生成时崩溃）?*/
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LUNAR_SPIRIT.get(), PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FLYING_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 32.0).build());
        event.put(GHOST_CAT.get(), GhostCatEntity.createGhostCatAttributes().build());
        event.put(WITCH_FAMILIAR.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 20.0).build());
        event.put(AXOLOTL_SHIFTER.get(), PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ARMOR, 5.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ATTACK_KNOCKBACK, 0.0).build());
    }

    private static <T extends Entity> RegistryObject<EntityType<T>> entity(String id, EntityType.EntityFactory<T> factory,
                                                                          float width, float height, int trackingChunks, int interval) {
        return ENTITIES.register(id, () ->
                EntityType.Builder.<T>of(factory, MobCategory.MISC).sized(width, height)
                        .clientTrackingRange(trackingChunks).updateInterval(interval).build(id));
    }

    private static RegistryObject<EntityType<ShellProjectile>> projectile(String id, float width, float height, int trackingChunks, int interval) {
        RegistryObject<EntityType<ShellProjectile>> entry = ENTITIES.register(id, () ->
                EntityType.Builder.<ShellProjectile>of(ShellProjectile::new, MobCategory.MISC).sized(width, height)
                        .clientTrackingRange(trackingChunks).updateInterval(interval).build(id));
        SHELL_PROJECTILES.add(entry);
        return entry;
    }

}
