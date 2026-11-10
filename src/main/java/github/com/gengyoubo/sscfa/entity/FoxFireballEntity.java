package github.com.gengyoubo.sscfa.entity;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.KillEmpowerManager;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * red閿涘牏瀛╅崼鏇℃儰娴ｅ潡鐡熼敍澶嬵偧鐟曚焦濡ч懗?- 閻欐劗浼€閻忣偆鎮嗛妴?
 * 閻╁鍤庢鐐额攽閿涘牊妫ら幎娑氬⒖缁捐￥鈧椒绗夌粚鍨暰閿涘绱?~12 閺嶅ジ鈧喎瀹?20閳? b/s 缁炬寧鈧嗏€滈崙蹇ョ礄缁?1.5 缁夋帪绱濋張鐔兼？缁岃儻绻冮悽鐔哄⒖闁姵鍨?8 姒勬梹纭剁粚鍧椻偓?+ 妫版繂顦婚悥鍡欑壃閿涘绱?
 * 12 閺嶇厧鎮楅崶鍝勭暰 2 閺?s 妞嬬偠顢戦懛鍐差樋 3 缁夋帪绱欐潏?18 閺嶉棿绗傞梽鎰剁礆閿涙稒鎸掓晶娆戠彌閸楀磭鍨庨悙闈╃礉12 閺嶇厧鎮楃喊宥囨晸閻椻晙绡冮悥鍡欏仮閿涘本婀崨鎴掕厬閸掓瑦鍦哄▔陇顥囧鈧☉鍫熸殠閵?
 * 閻栧棛鍋㈤敍? 閺嶈偐鎮嗛懠鍐ㄦ纯 6 閻椻晝鎮婇敍鍫滅瑝缁屽灝顣鹃敍?+ 娴犲懐娲块幒銉ь潾閸掓壆浼€閻炲啳鈧懓鍙為柈銊т紑閻滎垰鎮?2 閺嶇厧鍞存潻鐐烘敚 4 閻椻晝鎮婇敍鍫滅瑝閸愬秳绨╁▎陇绻涢柨渚婄礆閵?
 */
public class FoxFireballEntity extends Projectile implements net.minecraft.world.entity.projectile.ItemSupplier {

    // 閻欐劗浼€閻忣偆鎮嗛崣鍌涙殶閿涙岸绮拋銈勭瑢濞夈劑鍣存稉鈧懛杈剧幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.fox_fireball閿?
    private static final double ARM_DISTANCE = 12.0;   // 姒涙顓婚敍?2 閺嶇厧鎮楅幍宥堢箻閸忋儲娼冩导銈忕礄绾版澘顣?閻㈢喓澧块悥鍡欏仮閿?
    private static final double HIT_RADIUS = 2.5;      // 姒涙顓婚敍娑樻嚒娑?缁屽潡鈧繐鍨界€规艾宕愬鍕剁礄閻╊喗鐖ｇ喊鐗堟寬缁?expand閿涘奔绗岀憴鍡氼潕缁帒鐡欓懠鍐ㄦ纯閸栧綊鍘ら敍?
    private static final double EXPLODE_RADIUS = 6.0;  // 姒涙顓婚敍娑氬瀻閻愬摜鎮嗛崡濠傜窞
    private static final double CHAIN_RADIUS = 2.0;    // 姒涙顓婚敍娑滅箾闁夸礁宕愬?
    private static final float PIERCE_DAMAGE = 8.0f;   // 姒涙顓婚敍娑樺 12 閺嶈偐鈹涢柅蹇ョ礄姒勬梹纭堕敍?
    private static final float EXPLODE_DAMAGE = 6.0f;  // 姒涙顓婚敍娑氬瀻閻愰潻绱欓悧鈺冩倞閿?
    private static final float CHAIN_DAMAGE = 4.0f;    // 姒涙顓婚敍娑滅箾闁夸緤绱欓悧鈺冩倞閿?
    /** 闂堢偟甯虹€瑰墎娲伴弽鍥︽縺鐎瑰啿鈧秶宸奸敍鍫㈡暏閹村嘲鐣剧粙?鑴?.5閿涘绱癛ed 閻忣偆鎮嗙€佃鈧亞澧挎稉澶嬵唽閸忋劑鍎?鑴?.5閿涘苯顕悳鈺侇啀娣囨繃瀵旈崢鐔封偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿 */
    private static final float NON_PLAYER_MULT = 1.5f;
    private static final int RING_DURATION = 7;        // 姒涙顓婚敍娑滃彏闁劎浼€閻滎垱澧块弫锝呭З閻㈣鎶氶弫甯礄閸楀﹤绶?0閳?閿?
    private static final double PHASE2_SPEED = 2.0;    // 姒涙顓婚敍?2 閺嶇厧鎮楅崶鍝勭暰 2 閺?s
    private static final int PHASE2_DURATION = 60;     // 姒涙顓婚敍?2 閺嶇厧鎮楅張鈧径姘额棧 3 缁夋帪绱?0 tick 閳?18 閺嶉棿绗傞梽鎰剁礆

    // 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濐吂閹撮顏?韫囶偆鍙庨張顏勫灥婵瀵查崶鐐衡偓鈧妯款吇鐢悂鍣洪敍娑橆吂閹撮顏０鍕ゴ閸掑棙鏁稉搴″斧缂傛牞鐦ч張鐔风埗闁插繗顢戞稉杞扮閼疯揪绱?
    private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
            new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.fox_fireball");

    private static final EntityDataAccessor<Boolean> EXPLODED = SynchedEntityData.defineId(FoxFireballEntity.class, EntityDataSerializers.BOOLEAN);
    /** 閺傝姤纭堕懓?UUID 閸氬本顒炵紒娆忣吂閹撮顏敍姘珛鐏忓墽鐭戠€涙劖澧?owner 閺嶅浄绱欐禒鍛拱娴滆櫣顑囨稉鈧禍铏剐為柆鑳唨閿涘鏁ら敍?026-09-26 */
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> OWNER_UUID =
            SynchedEntityData.defineId(FoxFireballEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private Vec3 direction = new Vec3(0, 0, 1);
    private double distanceTraveled = 0;
    private int ticksAlive = 0;
    private int phase2Tick = 0;                  // 12 閺嶇厧鎮楀鏌ヮ棧 tick 閺?
    private boolean exploded = false;
    private boolean empowered = false;
    private boolean exploding = false;           // 閻栧棛鍋㈤崥搴′粻濮濄垻些閸斻劊鈧胶鐡戝鍛閺堝浼€閻滎垱鎸辩€瑰苯宓嗗☉鍫濄亼
    private final java.util.List<float[]> activeRings = new java.util.ArrayList<>();   // 濞叉槒绌悘顐ゅ箚 [cx,cy,cz,瀹稿弶鎸遍弨绶廼ck]
    private final java.util.Set<java.util.UUID> piercedEntities = new java.util.HashSet<>();

    public FoxFireballEntity(EntityType<? extends FoxFireballEntity> type, Level world) {
        super(type, world);
    }

    public FoxFireballEntity(Level world, LivingEntity owner) {
        super(github.com.gengyoubo.sscfa.SscAddon.FOX_FIREBALL_ENTITY, world);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        // 閸氬本顒為弬鑺ョ《閼?UUID 缂佹瑥顓归幋椋庮伂閿涙碍瀚嬬亸鍓х煈鐎?owner 閹垫挻鐖ｉ悽?
        if (owner != null) this.entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
    }

    public void setDirection(Vec3 dir) {
        this.direction = dir.normalize();
        this.setDeltaMovement(this.direction);   // 閻劑鈧喎瀹抽幎濠冩煙閸氭垹绱潻?spawn 閸栧拑绱濇笟娑橆吂閹撮顏０鍕ゴ缁夎濮?
    }

    public void setEmpowered(boolean empowered) {
        this.empowered = empowered;
    }

    /** 闁喎瀹抽弴鑼殠閿涘牊鐗?tick = b/s 姊?20閿涘绱?~12 閺嶅吋瀵滅捄婵堫瀲 20閳? 缁炬寧鈧団偓鎺戝櫤閿涘牏瀹?1.5 缁夋帟铔嬬€瑰矉绱氶敍?2 閺嶇厧鎮楅崶鍝勭暰 2 閺?s閵嗕繘rm 鐠烘繄顬囨潻鎰攽閺冩湹绮?balance 鐠囪褰囬敍鍫濐吂閹撮顏崶鐐衡偓鈧妯款吇閿涘鈧?*/
    private double speedPerTick(double d) {
        double bps;
        double armDistance = BAL.d("arm_distance", ARM_DISTANCE);
        if (d < armDistance) {
            bps = 20.0 - (20.0 - 2.0) * (d / armDistance);  // 20 閳?2 缁炬寧鈧団偓鎺戝櫤閿涘牊瀵滅捄婵堫瀲閿?
        } else {
            bps = BAL.d("phase2_speed", PHASE2_SPEED);      // 12 閺嶇厧鎮楅崶鍝勭暰 2 閺?s
        }
        return bps / 20.0;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EXPLODED, false);
        this.entityData.define(OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        if (this.level().isClientSide) {
            // 瀹歌尙鍨庨悙闈╃礄DataTracker 閸氬本顒為敍澶涚窗閸愯崵绮ㄦ担宥囩枂閵嗕椒绗夐崣鎴炲珛鐏忛敮鈧柡鈧柧绗岄張宥呭缁?exploding 閸掑棙鏁崑婊冨絺閺冭埖婧€缁墽鈥樼€靛綊缍?
            if (this.entityData.get(EXPLODED)) {
                return;
            }
            // 鐎广垺鍩涚粩顖溾€樼€规碍鈧囶暕濞村些閸旑煉绱欐稉搴㈡箛閸旓紕顏崥灞肩闁喎瀹抽弴鑼殠閿涘绱濆〒鍙夌厠閹绘帒鈧厧閽╁鎴礉闁灝鍘ら崣顏堟浆 tracker 閸氬本顒為惃鍕幢妞?
            Vec3 v = this.getDeltaMovement();
            Vec3 dir = v.lengthSqr() > 1.0e-6 ? v.normalize() : direction;
            double speed = speedPerTick(distanceTraveled);
            this.setPos(this.getX() + dir.x * speed, this.getY() + dir.y * speed, this.getZ() + dir.z * speed);
            distanceTraveled += speed;
            // 閹锋牕鐔划鎺戠摍鐎广垺鍩涚粩顖濆殰缂佹﹫绱欓梿鍓佺秹缂佹粌瀵橀敍娑樺棘閺佷即鈧劘顢戦悡褎濡遍張宥呭缁?spawnTrail 閸樼喓澧楅敍宀冾潌閺傝纭跺▔銊╁櫞閿?
            spawnTrailParticlesClient();
            return;
        }
        if (!(this.level() instanceof ServerLevel sw)) return;

        // 濮ｅ繐鎶氶幒銊ㄧ箻楠炲墎绮崚鑸靛閺堝妞跨捄鍐紑閻滎垽绱欑粚鍧椻偓?閻栧棛鍋㈢憴锕€褰傞惃鍕杺婢舵牜鍨庨悙绋垮З閻紮绱?
        updateActiveRings(sw);

        if (exploding) {
            // 瀹歌尙鍨庨悙闈╃窗閸樼喎婀寸粵澶婄窡閹碘偓閺堝浼€閻滎垱鎸辩€瑰苯宓嗗☉鍫濄亼
            if (activeRings.isEmpty()) this.discard();
            return;
        }

        boolean armed = distanceTraveled >= BAL.d("arm_distance", ARM_DISTANCE);  // 12 閺嶇厧鎮楅幍宥嗘箒閺夆偓娴?
        double speed = speedPerTick(distanceTraveled);
        Vec3 velocity = direction.scale(speed);
        Vec3 from = this.position();
        Vec3 to = from.add(velocity);

        // 婢ф瑥顥嗙喊鐗堟寬閿涙氨浼€閻炲啩绗夌粚鍨暰閿涘本鎸掓晶娆戠彌閸楀疇袝閸欐垹鍨庨惍杈剧礄娴犺缍嶇捄婵堫瀲閿?
        BlockHitResult blockHit = sw.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            Vec3 hp = blockHit.getLocation();
            this.setPos(hp.x, hp.y, hp.z);
            explode(sw, null);
            return;
        }

        this.setPos(to.x, to.y, to.z);
        distanceTraveled += speed;

        if (armed) {
            // 12 閺嶇厧鎮楅敍? 閺嶈偐鎮嗛崨鎴掕厬闂堢偟娅ч崥宥呭礋閻㈢喓澧块崡宕囧瀻閻?
            LivingEntity target = findTarget(sw);
            if (target != null) {
                explode(sw, target);
                return;
            }
        } else {
            // 閸?12 閺嶇》绱扮粚鑳箖閻㈢喓澧块柅鐘冲灇缁屽潡鈧繋婵€鐎?+ 妫版繂顦婚悥鍡欏仮閿涘牏浼€閻炲啩绗夐悘顓犳埛缂侇參顥ｉ敍?
            pierceTargets(sw);
        }
        spawnTrail(sw);

        // 12 閺嶇厧鎮楅張鈧径姘晙妞?3 缁夋帪绱欐潏?18 閺嶇》绱氶敍灞炬弓閸涙垝鑵戦崚娆忓斧閸︽媽袝閸欐垳绔村▎锛勫瀻閻愮鎮楀☉鍫濄亼
        if (distanceTraveled >= BAL.d("arm_distance", ARM_DISTANCE)) {
            phase2Tick++;
            if (phase2Tick >= BAL.i("phase2_duration", PHASE2_DURATION)) {
                explode(sw, null);   // 閺堫亜鎳℃稉顓㈩棧閸掓澘鐨犵粙瀣╃瑐闂勬劧绱伴崢鐔锋勾鐟欙箑褰傛稉鈧▎锛勫瀻閻愰潻绱欓弮鐘虹箾闁夸緤绱氶崥搴㈢Х婢?
                return;
            }
        }
        if (ticksAlive > 200) this.discard();
    }

    private LivingEntity findTarget(ServerLevel world) {
        // 閸掋倕鐣鹃崺鍝勫櫙 = 閻╊喗鐖ｇ喊鐗堟寬缁犳唻绱欓懓宀勬姜閼存艾绨抽崸鎰垼閻愮櫢绱氶敍姘剁彯婢堆呮窗閺嶅浄绱欓柧浣界吙閸庯繝鐝?2.7 閺嶇》绱氶惃鍕壖鎼存洜鍋?
        // 娑撳酣顥ｇ悰灞艰厬閻ㄥ嫮浼€閻炲啩鑵戣箛鍐ㄧ€惄纾嬬獩缁傝鐖?>1.5 閺嶇》绱濋弮褑鍓兼惔鏇犲仯閸掋倕鐣炬导姘Ω鏉╂瑤绨洪垾婊嗩潒鐟欏鎳℃稉顓涒偓婵婎嚖閸掋倓璐熼張顏勬嚒娑?
        double hitRadius = BAL.d("hit_radius", HIT_RADIUS);
        AABB box = this.getBoundingBox().inflate(hitRadius);
        Vec3 c = this.position();
        List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this.getOwner() && e.isAlive() && !e.isSpectator()
                        && e.getBoundingBox().inflate(hitRadius).contains(c));
        for (LivingEntity e : list) {
            if (this.getOwner() instanceof ServerPlayer op && WhitelistUtils.isProtected(op, e)) continue;
            return e;
        }
        return null;
    }

    /** 閸?12 閺嶈偐鈹涢柅蹇ョ窗鐎电懓鍨界€规氨鎮嗛崘鍛槨娑擃亪娼惂钘夋倳閸楁洜鏁撻悧鈺呪偓鐘冲灇娑撯偓濞?8 姒勬梹纭剁粚鍧椻偓蹇庢縺鐎圭绱欓崢濠氬櫢閿涘瞼浼€閻炲啩绗夐悘顓ㄧ礆閵嗗倸鍨界€规艾鐔€娴滃海娲伴弽鍥╊潾閹剧偟顔堥妴?*/
    private void pierceTargets(ServerLevel world) {
        double hitRadius = BAL.d("hit_radius", HIT_RADIUS);
        AABB box = this.getBoundingBox().inflate(hitRadius);
        Vec3 c = this.position();
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this.getOwner() && e.isAlive() && !e.isSpectator()
                        && e.getBoundingBox().inflate(hitRadius).contains(c)
                        && !piercedEntities.contains(e.getUUID()));
        for (LivingEntity e : list) {
            if (this.getOwner() instanceof ServerPlayer op && WhitelistUtils.isProtected(op, e)) {
                github.com.gengyoubo.sscfa.util.FireballDebugLog.log("PIERCE skipped (whitelist) target=" + e.getType().toString());
                continue;
            }
            float hpBefore = e.getHealth();
            // 缁屽潡鈧繑顔岄崥灞剧壉缂佹洝绻冮崣妤€鍤弮鐘虫櫕鐢嶇窗閺咁喗鏁?鏉╂垼闊╅棁鏉垮毊閸忓牆鎳℃稉顓熸娴兼氨鏆€娑撳妫ら弫灞芥姎 + lastDamageTaken閿?
            // 閼汇儱鍙?閳?8 娴笺倕鍨張顒侇唽鐞氼偄甯悧鍫涒偓瀹巑ount <= lastDamageTaken閵嗗秷顫夐崚娆愭殻閸欙絽鎮堕幒澶涚礄閸嬭泛褰傜粚鍧椻偓蹇旀￥娴笺倖鐗撮崶鐙呯礆
            e.invulnerableTime = 0;
            float pierceDmg = (float) BAL.d("pierce_damage", PIERCE_DAMAGE);
            float dmg = scaleFor(e, pierceDmg);
            boolean dmgOk = KillEmpowerManager.damage(e, magicSource(e, owner), dmg, !empowered);
            if (dmgOk) e.invulnerableTime = 0;   // 閸涙垝鑵戦崥搴㈢閿涙矮绗夐悾娆愭￥閺佸苯鎶氶崣宥呮偠閺咁喗鏁?
            github.com.gengyoubo.sscfa.util.FireballDebugLog.log("PIERCE target=" + e.getType().toString()
                    + " amount=" + pierceDmg + " returned=" + dmgOk
                    + " hp " + hpBefore + " -> " + e.getHealth());
            piercedEntities.add(e.getUUID());
            applyFoxFireBurn(e);
            double ex = e.getX(), ey = e.getY() + e.getBbHeight() * 0.5, ez = e.getZ();
            // 2026-09-26 閻劍鍩涚€规氨顭堥敍姘变紑閻炲啫鎳℃稉顓犵煈鐎涙劕寮稉搴ㄤ缉鐠佲晪绱檕wner 娴ｆ粎鏁ら崺鐕傜礉娴犲懏婀版禍铏诡儑娑撯偓娴滆櫣袨濞ｂ€冲閿涘奔绮禍鍝勫斧閺嶅嚖绱?
            // 瀵綊浜炬担婊呮暏閸╃噦绱扮挒浣稿帳閸戝棙妲﹂柨銉ュ竾閸掕绱欓幏鏍х啲/閻栧棛鍋?閻忣偆骞嗗▽鑳潒缁惧潡顥ｇ悰宀嬬礉閸愬秴褰旈柨銉ょ窗閹跺﹥鏆ｉ弶鈥宠剨闁挸甯囬崚?10%閿?
            github.com.gengyoubo.sscfa.network.DecorationParticleScope.runProjectile(owner, () -> {
                world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ex, ey, ez, 8, 0.3, 0.3, 0.3, 0.02);
                world.sendParticles(ParticleTypes.FLAME, ex, ey, ez, 6, 0.25, 0.25, 0.25, 0.02);
            });
            world.playSound(null, ex, ey, ez, SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 0.4f, 1.6f);
            triggerExtraExplosion(world, owner, e);   // 缁屽潡鈧繑顔屾稊鐔恍曢崣鎴︻杺婢舵牜鍨庨悙闈╃礄閼典即鍎撮悘顐ゅ箚 + 鏉╃偤鏀ｉ敍?
        }
    }

    /**
     * 閹锋牕鐔划鎺戠摍瀹歌尙些閼峰啿顓归幋椋庮伂閿涙俺顫?tick() 鐎广垺鍩涚粩顖氬瀻閺€?spawnTrailParticlesClient閿涘牓娴傜純鎴犵捕閸栧拑绱濋崣鍌涙殶闁劘顢戦悡褎濡遍張顒佹煙濞夋洖甯悧鍫礆閵?
     * 閺堝秴濮熺粩顖欑箽閻ｆ瑧鈹栨竟鍐插涧娴ｆ粌宸婚崣鎻掑棘閼板喛绱遍悥鍡欏仮/缁屽潡鈧?閻忣偆骞嗙粵澶夌濞嗏剝鈧傜皑娴犲墎鐭戠€涙劒绮涢悽杈ㄦ箛閸旓紕顏獮鎸庢尡閵?
     */
    private void spawnTrail(ServerLevel w) {
    }

    /**
     * 閹锋牕鐔划鎺戠摍鐎广垺鍩涚粩顖濆殰缂佹﹫绱?b>闁劘顢戦悡褎濡遍張宥呭缁?spawnTrail 閸樼喓澧?/b>閿涘牊鐦?tick 娑撯偓濞嗏槄绱濋悽鍗炵杽娴?tick 婢垛晝鍔ч梻銊﹀付娑撳秷绉存０鎴礆閵?
     * 娴ｅ秶鐤嗛崗銊х翱绾噯绱遍柅鐔峰閹稿甯悧?gaussian鑴硈peed 閻ㄥ嫰鍣虹痪褍褰囨潻鎴滄妧鐢鎮滈柌蹇ョ礄娑撳孩鐤嗗Ч鎰倖閹锋牕鐔崥灞剧垼閸戝棴绱氶妴?
     */
    private void spawnTrailParticlesClient() {
        // 2026-09-26 閻劍鍩涚€规氨顭堥敍姘珛鐏忔儳寮稉搴ㄤ缉鐠佲斁鈧柡鈧梻鏁?DataTracker 閸氬本顒為惃?owner UUID 閹垫挻鐖ｉ敍鍫滅矌閺堫兛姹夌粭顑跨娴滆櫣袨濞ｂ€冲閿?
        java.util.Optional<java.util.UUID> ownerUuid = this.entityData.get(OWNER_UUID);
        Runnable spawn = () -> {
            double x = this.getX(), y = this.getY(), z = this.getZ();
            Level w = this.level();
            RandomSource rnd = this.random;
            // 閻忣偆鍔欓悶鍐х秼 鑴?0閿涘牆甯悧鍫窗count=1閵嗕礁浜哥粔?0閵嗕够peed=0.01 閳?娴ｅ秶鐤嗙划鍓р€橀妴浣镐簳闁喍绗傛姗堢礆
            for (int i = 0; i < 10; i++) {
                Vec3 p = randomInSphere(1.0, rnd);
                w.addParticle(ParticleTypes.FLAME, x + p.x, y + p.y, z + p.z, 0, 0.01, 0);
            }
            // 姒勫倻浼€閻炲啩缍?鑴?
            for (int i = 0; i < 7; i++) {
                Vec3 p = randomInSphere(1.0, rnd);
                w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + p.x, y + p.y, z + p.z, 0, 0.01, 0);
            }
            // 瀹€鈺傜ギ閻忣偅妲?30%閿涘牆甯悧鍫濅焊缁?鍗?0.003,0.008,0.003)閳?閵嗕線鈧喎瀹?0閿?
            if (rnd.nextFloat() < 0.3f) {
                Vec3 p = randomInSphere(0.4, rnd);
                w.addParticle(ParticleTypes.LAVA, x + p.x, y + p.y, z + p.z, 0, 0, 0);
            }
            // 閻旀柨鍗氬纾嬫儰 7%
            if (rnd.nextFloat() < 0.07f) {
                w.addParticle(ParticleTypes.FALLING_LAVA,
                        x + (rnd.nextDouble() - 0.5) * 1.0,
                        y - 0.6 + (rnd.nextDouble() - 0.5) * 0.8,
                        z + (rnd.nextDouble() - 0.5) * 1.0,
                        0, 0, 0);
            }
            // 鐏忛箖鍎撮崣宥呮倻閿涙氨浼€閻?鑴? / 姒勫倻浼€ 鑴? / 閻戠喖娴?鑴?閿涘牓鐝弬顖氫焊缁夎绗岄張宥呭缁?spawnParticles count>0 閸掑棙鏁崥灞藉瀻鐢喛绱?
            Vec3 v = this.getDeltaMovement();
            Vec3 dir = v.lengthSqr() > 1.0e-6 ? v.normalize() : this.direction;
            Vec3 back = dir.scale(-0.5);
            for (int i = 0; i < 2; i++) {
                w.addParticle(ParticleTypes.FLAME,
                        x + back.x + rnd.nextGaussian() * 0.15,
                        y + back.y + rnd.nextGaussian() * 0.15,
                        z + back.z + rnd.nextGaussian() * 0.15, 0, 0, 0);
            }
            w.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    x + back.x + rnd.nextGaussian() * 0.12,
                    y + back.y + rnd.nextGaussian() * 0.12,
                    z + back.z + rnd.nextGaussian() * 0.12, 0, 0, 0);
            w.addParticle(ParticleTypes.SMOKE,
                    x + back.x * 1.5 + rnd.nextGaussian() * 0.1,
                    y + back.y * 1.5 + rnd.nextGaussian() * 0.1,
                    z + back.z * 1.5 + rnd.nextGaussian() * 0.1, 0, 0, 0);
        };
        if (ownerUuid.isPresent()) {
            github.com.gengyoubo.sscfa.client.particle.FirstPersonParticles.emitProjectile(ownerUuid.get(), spawn);
        } else {
            spawn.run();
        }
    }

    private void explode(ServerLevel w, LivingEntity directTarget) {
        if (exploded) return;
        exploded = true;
        this.entityData.set(EXPLODED, true);   // 閸氬本顒炵€广垺鍩涚粩顖ょ窗閸愯崵绮ㄦ０鍕ゴ缁夎濮╅妴浣镐粻閸欐垶瀚嬬亸?
        double x = this.getX(), y = this.getY(), z = this.getZ();
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;

        // 閻栧棛鍋㈢划鎺戠摍 + 闂婅櫕鏅?
        spawnExplosionParticles(w, x, y, z);
        w.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.4f);

        // 6 閺嶈偐鎮嗛懠鍐ㄦ纯閻椻晝鎮婃导銈咁唺閿涙稖顫嬬痪鑳潶閺傜懓娼￠梼缁樺皡閻ㄥ嫮娲伴弽鍥︾瑝閸欐ぞ婵€閿涘牏鍨庨悙闀愮瑝缁屽灝顣鹃敍?
        double explodeRadius = BAL.d("explode_radius", EXPLODE_RADIUS);
        double explodeRadiusSq = explodeRadius * explodeRadius;
        AABB box = new AABB(x - explodeRadius, y - explodeRadius, z - explodeRadius,
                x + explodeRadius, y + explodeRadius, z + explodeRadius);
        List<LivingEntity> affected = w.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && e.isAlive() && !e.isSpectator()
                        && e.distanceToSqr(x, y, z) <= explodeRadiusSq
                        && !(owner instanceof ServerPlayer op && WhitelistUtils.isProtected(op, e))
                        && hasLineOfSight(w, x, y, z, e));
        float explodeDamageBase = (float) BAL.d("explode_damage", EXPLODE_DAMAGE);
        for (LivingEntity e : affected) {
            // 閻栧棛鐗导銈咁唺閹稿绐涚粋鏄忊€滈崙蹇ョ窗閳? 閺嶇厧鍙忔０婵撶礉2~6 閺嶈偐鍤庨幀褑鈥滈崙蹇氬殾閺堚偓娴?1
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            float dmg;
            if (dist <= 2.0) {
                dmg = explodeDamageBase;
            } else {
                dmg = (float) (explodeDamageBase - (explodeDamageBase - 1.0) * (dist - 2.0) / (explodeRadius - 2.0));
                if (dmg < 1.0f) dmg = 1.0f;
            }
            float hpBefore = e.getHealth();
            // 閹垛偓閼宠棄顦垮▓鍏告縺鐎瑰磭绮潻鍥у斧閻楀牆褰堥崙缁樻￥閺佸苯鎶氶敍姘扁敍闁繑顔?8 娴笺倕鎳℃稉顓炴倵閻╊喗鐖ｆ潻娑樺弳 10t 閺冪姵鏅粣妤€褰涢敍?
            // 閸氬海鐢婚悥鍡欏仮 6 / 鏉╃偤鏀?4 閳?lastDamageTaken 娴兼俺顫?damage() 閻╁瓨甯?return false 閸氱偞甯€閿涘牅娑导銈咁唺閺嶇懓娲滈敍澶堚偓?
            // 濞撳懘娴傞弮鐘虫櫕鐠佲剝妞傜拋鈺傛拱閹垛偓閼冲€熷殰瀹歌京娈戦崥搴ｇ敾濞堥潧鍙忔０婵堟晸閺佸牞绱欐稉宥呭閸濆秴鍙剧€瑰啩婵€鐎硅櫕绨敍澶堚偓?
            e.invulnerableTime = 0;
            float scaledExplode = scaleFor(e, dmg);
            boolean dmgOk = KillEmpowerManager.damage(e, physicalSource(e, owner), scaledExplode, !empowered);
            if (dmgOk) e.invulnerableTime = 0;   // 閸涙垝鑵戦崥搴㈢閿涙矮绗夐悾娆愭￥閺佸苯鎶氶崣宥呮偠閺咁喗鏁?
            github.com.gengyoubo.sscfa.util.FireballDebugLog.log("EXPLODE target=" + e.getType().toString()
                    + " amount=" + dmg + " returned=" + dmgOk
                    + " hp " + hpBefore + " -> " + e.getHealth());
            applyFoxFireBurn(e);
        }
        // 娴犲應鈧粎娲块幒銉ь潾閸掓壆浼€閻炲啠鈧繄娈戦悽鐔哄⒖鐟欙箑褰傛０婵嗩樆閻栧棛鐗敍鍫ｅ彏闁劎浼€閻?+ 2 閺嶈壈绻涢柨渚婄礆閿涙稐瀵岄悥鍡欏仮閼煎啫娲块崘鍛従娴犳牜鏁撻悧鈺€绗夌憴锕€褰傞敍娑氼潾婢ф瑦妫ら惄瀛樺复閻╊喗鐖ｉ崚娆庣瑝鐟欙箑褰?
        if (directTarget != null && directTarget.isAlive()) {
            triggerExtraExplosion(w, owner, directTarget);
        }
        // 閻栧棛鍋㈤崥搴′粻濮濄垻些閸旑煉绱濋悽?tick 缁涘澧嶉張澶屼紑閻滎垱鎸辩€瑰苯宓嗗☉鍫濄亼
        exploding = true;
    }

    /** 閻栧棛鍋㈢憴鍡欏殠濡偓濞村绱伴悥鍡欏仮娑擃厼绺鹃敍鍫㈡殣濞屾寧娼甸崥鎴濇礀闁偓闁灝鍘ょ拹鏉戭暰鐠囶垰鍨介敍澶婂煂閻╊喗鐖ｉ懟銉潶閺傜懓娼＄喊鐗堟寬缁犻亶妯嗛幐鈥冲灟娑撳秴褰叉潏鎾呯礄閻栧棛鍋㈡稉宥団敍婢ф瑱绱氶妴?*/
    private boolean hasLineOfSight(ServerLevel w, double x, double y, double z, LivingEntity e) {
        Vec3 from = new Vec3(x, y, z).subtract(direction.scale(0.3));
        Vec3 to = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
        BlockHitResult hit = w.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** 妫版繂顦婚悥鍡欏仮閿涙俺顫﹁ぐ鍗炴惙閼板懓鍙為柈銊т紑閻滎垱澧块弫?+ 2 閺嶇厧鍞存潻鐐烘敚娴笺倕顔婇敍鍫滅瑝閸愬秳绨╁▎陇绻涢柨渚婄礆閿涙稓鈹涢柅蹇旑唽娑撳海鍨庨悙鍛婎唽閸忚京鏁ら妴?*/
    private void triggerExtraExplosion(ServerLevel w, LivingEntity owner, LivingEntity center) {
        double cx = center.getX(), cy = center.getY() + center.getBbHeight() * 0.5, cz = center.getZ();
        activeRings.add(new float[]{(float) cx, (float) cy, (float) cz, 0f});   // 閼典即鍎撮悘顐ゅ箚閿涘瞼鏁?tick 闁劕鎶氶幘顓熸杹
        double chainRadius = BAL.d("chain_radius", CHAIN_RADIUS);
        double chainRadiusSq = chainRadius * chainRadius;
        AABB box = new AABB(cx - chainRadius, cy - chainRadius, cz - chainRadius,
                cx + chainRadius, cy + chainRadius, cz + chainRadius);
        List<LivingEntity> chained = w.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && e != center && e.isAlive() && !e.isSpectator()
                        && e.distanceToSqr(cx, cy, cz) <= chainRadiusSq
                        && !(owner instanceof ServerPlayer op && WhitelistUtils.isProtected(op, e)));
        float chainDamage = (float) BAL.d("chain_damage", CHAIN_DAMAGE);
        for (LivingEntity e : chained) {
            // 鏉╃偤鏀ｅ▓鐢垫彛鐠虹喓鈹涢柅蹇旑唽閿? 娴笺倧绱氭稊瀣倵閿涘苯鎮撻弽鐑芥付缂佹洝绻冮崣妤€鍤弮鐘虫櫕鐢嶇礉閸氾箑鍨?4 閳?8 鐞氼偄鎮?
            e.invulnerableTime = 0;
            boolean chainDmgOk = KillEmpowerManager.damage(e, physicalSource(e, owner), scaleFor(e, chainDamage), !empowered);
            if (chainDmgOk) e.invulnerableTime = 0;   // 閸涙垝鑵戦崥搴㈢閿涙矮绗夐悾娆愭￥閺佸苯鎶氶崣宥呮偠閺咁喗鏁?
            applyFoxFireBurn(e);
        }
    }

    /** 閻忣偆鎮嗛崨鎴掕厬闂勫嫬濮為悪鎰紑閻忚偐鍎?5 缁夋帪绱欏В蹇曨潡閹哄顢呴敍澶涚礉楠炴儼顔囪ぐ鏇熸煢濞夋洝鈧懎缍婄仦鐑囩礄娓氭稓浼￠悜?DoT 瑜版帒娲滈敍澶堚偓?*/
    private void applyFoxFireBurn(LivingEntity target) {
        // 鐢附鏌﹀▔鏇♀偓?source 娓氭盯顥ゅ锕傜摕閵嗗苯鍙嗗锔衡偓宄漞buff 閹凤附鍩呰ぐ鎺戞礈
        target.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.SscAddon.FOX_FIRE_BURN, 100, 0, false, true, true), this.getOwner());
        if (this.getOwner() instanceof ServerPlayer player) {
            KillEmpowerManager.trackBurn(player, target, 100, !empowered);
        }
        github.com.gengyoubo.sscfa.effect.FoxFireBurnOwnership.record(target,
                this.getOwner() == null ? null : this.getOwner().getUUID());
    }

    /** 閹恒劏绻橀獮鍓佺帛閸掕埖澧嶉張澶嬫た鐠哄啰浼€閻滎垽绱欑粚鍧椻偓?閻栧棛鍋㈢憴锕€褰傞敍澶涚礉閹绢厼鐣粔濠氭珟閵?*/
    private void updateActiveRings(ServerLevel w) {
        int ringDuration = BAL.i("ring_duration", RING_DURATION);
        java.util.Iterator<float[]> it = activeRings.iterator();
        while (it.hasNext()) {
            float[] r = it.next();
            r[3] += 1f;
            float progress = r[3] / (float) ringDuration;
            spawnWaistRing(w, r[0], r[1], r[2], progress);
            if (r[3] >= ringDuration) it.remove();
        }
    }

    /** 6 閺嶈偐鎮嗛悥鍡欏仮缁帒鐡欓敍?0% 缁?dust + 20% 閻欐劗浼€閿涘矂妾崝鐘茬毌闁?lava 姒涙垶绂忛妴淇箇ner 娴ｆ粎鏁ら崺鐕傜窗娴犲懏婀版禍铏诡儑娑撯偓娴滆櫣袨闁儻顔€閵?*/
    private void spawnExplosionParticles(ServerLevel w, double x, double y, double z) {
        double explodeRadius = BAL.d("explode_radius", EXPLODE_RADIUS);
        github.com.gengyoubo.sscfa.network.DecorationParticleScope.runProjectile(getOwner() instanceof LivingEntity le ? le : null, () -> {
            RandomSource rnd = this.random;
            DustParticleOptions red = new DustParticleOptions(new Vector3f(0.85f, 0.1f, 0.05f), 1.3f);
            for (int i = 0; i < 130; i++) {
                Vec3 p = randomInSphere(explodeRadius, rnd);
                double px = x + p.x, py = y + p.y, pz = z + p.z;
                if (rnd.nextDouble() < 0.8) {
                    w.sendParticles(red, px, py, pz, 1, 0, 0, 0, 0);
                } else {
                    w.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0, 0, 0, 0.01);
                }
            }
            for (int i = 0; i < 16; i++) {
                Vec3 p = randomInSphere(explodeRadius * 0.6, rnd);
                w.sendParticles(ParticleTypes.LAVA, x + p.x, y + p.y + 1.0, z + p.z, 1, 0, 0, 0, 0);
            }
            // === RC4 婵傘儲婀抽幍瀣祫閻栧棛鍋㈤悧瑙勬櫏閿涘牊鏂佹径?1.5 閸婂秶澧楅敍姘⒖閺侊綀瀵栭崶缈犵瑢 dust 鐏忓搫顕?鑴?.5閿涘本鏆熼柌蹇庣瑢闁喎瀹虫稉宥呭綁閿?===
            w.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.3, z, 3, 0.3, 0.3, 0.3, 1.0);
            w.sendParticles(ParticleTypes.LAVA, x, y + 0.8, z, 20, 0.45, 0.45, 0.45, 0.2);
            // 缁鳖偆瀛╅埆鎺撴缁俱垺绗庨崣妯肩焽鐏忔﹫绱檇ust 鐏忓搫顕?1閳?.5閿?
            DustColorTransitionOptions arcaneDust = new DustColorTransitionOptions(
                    new Vector3f(0.322f, 0.0f, 0.149f), new Vector3f(0.149f, 0.012f, 0.039f), 1.5f);
            w.sendParticles(arcaneDust, x, y + 0.3, z, 300, 1.05, 1.8, 1.05, 0.01);
            w.sendParticles(ParticleTypes.FLAME, x, y + 0.3, z, 80, 0.75, 1.2, 0.75, 0.1);
            w.sendParticles(ParticleTypes.SQUID_INK, x, y + 0.3, z, 5, 0.45, 0.45, 0.45, 0.1);
            w.sendParticles(ParticleTypes.FALLING_LAVA, x, y + 0.1, z, 125, 1.5, 0.75, 1.5, 0.2);
        });
    }

    /** 閼典即鍎撮悘顐ゅ箚閹碘晜鏆庨崝銊ф暰閿涙碍鐦＄敮褏鏁鹃崡濠傜窞闁帒顤冮惃鍕箚閿?閳帟绻涢柨浣稿磹瀵板嫸绱氶敍灞肩矌閻忣偆鍔?+ 閻忕敻鐡婇悘顐ょ煈鐎涙劧绱欓弮鐘靛閼硅尙鐭囩亸姗堢礆閵嗕抗wner 娴ｆ粎鏁ら崺鐕傜窗娴犲懏婀版禍娲缉鐠佲斂鈧?*/
    private void spawnWaistRing(ServerLevel w, double x, double y, double z, float progress) {
        github.com.gengyoubo.sscfa.network.DecorationParticleScope.runProjectile(getOwner() instanceof LivingEntity le ? le : null, () -> {
            double chainRadius = BAL.d("chain_radius", CHAIN_RADIUS);
            double radius = Math.max(0.1, chainRadius * progress);
            int pts = Math.max(2, (int) ((12 + 26 * progress) * 0.2));  // 缁帒鐡欓柌蹇撳櫤閼峰啿甯?20%
            double rot = progress * 0.6;                                // 鏉炶浜曢弮瀣祮閺囧浼掗崝?
            double upward = 0.02 + progress * 0.04;                     // 閻忣偉瀚嶉崥鎴滅瑐妞?
            for (int i = 0; i < pts; i++) {
                double a = 2 * Math.PI * i / pts + rot;
                double px = x + radius * Math.cos(a);
                double pz = z + radius * Math.sin(a);
                w.sendParticles(ParticleTypes.FLAME, px, y, pz, 1, 0, upward, 0, 0.0);
                if (i % 2 == 0) {
                    w.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, y + 0.05, pz, 1, 0, upward, 0, 0.01);
                }
            }
        });
    }

    private Vec3 randomInSphere(double r, RandomSource rnd) {
        double rr = r * Math.cbrt(rnd.nextDouble());
        double theta = rnd.nextDouble() * 2 * Math.PI;
        double phi = Math.acos(2 * rnd.nextDouble() - 1);
        double sinPhi = Math.sin(phi);
        return new Vec3(rr * sinPhi * Math.cos(theta), rr * Math.cos(phi), rr * sinPhi * Math.sin(theta));
    }

    private DamageSource magicSource(LivingEntity target, LivingEntity owner) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "magic"));
        return new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key), owner, owner);
    }

    /** 閹稿娲伴弽鍥╄閸ㄥ缂夐弨鍙ユ縺鐎圭绱伴棃鐐靛负鐎瑰墎鏁撻悧?鑴?NON_PLAYER_MULT閿涘潌alance 韫囶偆鍙庨崣顖濐洬閻╂牭绱辩€靛湱甯虹€规湹绻氶幐浣稿斧閸婄》绱氶妴?*/
    private static float scaleFor(LivingEntity target, float base) {
        if (target instanceof net.minecraft.world.entity.player.Player) return base;
        return base * (float) BAL.d("non_player_mult", NON_PLAYER_MULT);
    }

    private DamageSource physicalSource(LivingEntity target, LivingEntity owner) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "mob_attack"));
        return new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key), owner, owner);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner() && entity instanceof LivingEntity;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("DirX")) {
            this.direction = new Vec3(nbt.getDouble("DirX"), nbt.getDouble("DirY"), nbt.getDouble("DirZ"));
        }
        this.distanceTraveled = nbt.getDouble("Dist");
        this.exploded = nbt.getBoolean("Exploded");
        this.empowered = nbt.getBoolean("Empowered");
        // 2026-09-26 閻樿埖鈧焦浠径宥埶夋鎰剁窗濮濄倕澧犻柌宥堟祰閸?exploded=true 娴?exploding=false 閳?tick 缂佈呯敾妞嬬偠顢戞担?
        // explode() 鐞?if(exploded) 閹凤附鍩呴敍宀€浼€閻炲啫褰夐妴灞芥惣瀵湱鐢绘鐐偓宥冣偓鍌氬嚒閻栧棛娈戦悘顐ゆ倖闁插秷娴囬崥搴ｆ纯閹恒儲浠径宥呭枙缂佹挻鈧胶鐡戝鍛閻炲棎鈧?
        if (this.exploded) {
            this.exploding = true;
            this.entityData.set(EXPLODED, true);
        }
        // OWNER_UUID 閺堫亝瀵旀稊鍛閺冩湹绮?ProjectileEntity 瀹稿弶浠径宥囨畱 owner UUID 閸ョ偛锝為敍灞惧珛鐏忕偓澧﹂弽鍥︾瑝閺傤參鎽?
        if (this.entityData.get(OWNER_UUID).isEmpty() && super.getOwner() != null) {
            this.entityData.set(OWNER_UUID, java.util.Optional.of(super.getOwner().getUUID()));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putDouble("DirX", direction.x);
        nbt.putDouble("DirY", direction.y);
        nbt.putDouble("DirZ", direction.z);
        nbt.putDouble("Dist", distanceTraveled);
        nbt.putBoolean("Exploded", exploded);
        nbt.putBoolean("Empowered", empowered);
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIRE_CHARGE);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
