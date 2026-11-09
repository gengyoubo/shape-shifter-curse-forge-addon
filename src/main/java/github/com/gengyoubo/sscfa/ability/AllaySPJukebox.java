package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.item.AllayJukeboxItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.SkillBlocker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.UUID;

/**
 * SP闁诡噯濡囨导鎺楀船鏉堚晛顣婚柡鍫濇惈鐏忣垶宕洪悢鍛婃珡闁哄缈籭ck闂侇偅妲掔欢?
 * - 闁搞儳鍋犻、鍛熼垾宕囩闁挎稒纰嶉惁?缂佸甯炵划?0闁哄秶鍘ч崬鎾儌閽樺鍊抽柛妤佹礈閺佹捇鎮ч埡浣圭1HP
 * - 闁告梻濞€閳ь剛鍠愯啯鐎殿喖楠忕槐浼村箰娴ｈ櫣鏁剧紓?0闁哄秶鍘ч崬鎾儌閽樺鍊抽柛妤佹礈閺佹捇鎮?10%缂佸顭烽埀?
 * - 婵絽绻掗～妤€鈽夐崼锝傚亾?闁绘劗鎳撻崢鏍嚄?
 * - 闁圭虎鍘介弬渚€鎳涢鍕毎濞戞柨顦甸悡鑸电▕?
 */
public class AllaySPJukebox {
    private AllaySPJukebox() {
        // Utility class
    }

    // 闁哥偠浜晶鏍嫉閸濆嫬妫橀柡浣稿簻缁辩増顪€濡鍚囧☉鎾冲閺佺偤鏌屾繝浣侯伇闁肩柉鎻槐杈ㄦ交閹邦垼鏀介柡鍐╂构缁?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柨娑樻綂bilities.allay_sp_jukebox闁?
    public static final double RANGE = 20.0;
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a3b4c5d6-e7f8-9012-3456-789abcdef012");
    private static final String SPEED_MODIFIER_NAME = "allay_jukebox_speed";
    private static final double SPEED_BONUS = 0.10; // 濮掓稒顭堥濠氭晬?0% speed

    // 濠⒀呭仧濞?status effect 闁告帡鏀遍弻濠囧籍閸洘姣愰柨娑樻緟ick闁挎稑顧€缁辨澘袙?5 tick 閻炴稏鍎茬€垫洘绋夐埀顒€鈻庨埥鍛20t 閻熸洖妫涘ú濠囧礆闁垮鐓€闂傚倹鎸冲▓褎绌卞┑濠勬闁搞儳鍋撻悥锝囨暜闂€鎰槰濞戞挸绉瑰Λ顖炲棘椤撱劎骞?
    // 濮掓稑瀚缓?ABSORPTION)闁活潿鍔嶉婵嬪籍閸洘姣愰柨娑欏哺閳ь兘鍋撻柛鎴濇惈濞叉牜鎮伴埀顒€螣閳ュ磭纭€闁告艾楠告禒鐘差潰閵忕姴鐓曢柡鍌氬簻缁辨繈宕滈埡鈧紞?effect 濞村吋鑹惧﹢?闁?0t 闁告劕鎳撶换鍐嫉閻曞倻绀?
    // AbsorptionStatusEffect.onRemoved 濞村吋淇洪崵婊堝礉閵婏箑鈷忛梻鍕╁€曢悾鐘诲棘閽樺顫ｉ柣銊ュ婵垽鎯勯幘鍛濞戞挸绉烽銈夊礆閻橀潧璐熼悗纭呮硾閸欏墽鈧懓鍟闈涒攦閹邦厼袘闁烩晙鍕橀埀?
    private static final int BUFF_REFRESH_DURATION = 20;

    // 闂傚啳鍩栭?5闁挎稒纰嶅﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲绠涢銈呭季閻犲洩顕цぐ鍥晬閸繃褰ラ柣鎾楀嫭寮撻柛鎺撶箓椤劙宕犻弽褎绀€闂侇偀鍋撳娑欘焾椤撹崵鏁幖浣告闁?
    private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
            new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_jukebox");

    // Track per-player: -1 = not playing, 0 = speed music, 1 = heal music
    private static final java.util.concurrent.ConcurrentHashMap<UUID, Integer> playerMusicState = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<net.minecraft.server.MinecraftServer, SourceLeaseBook<LivingEntity, UUID>> SPEED_LEASES = new java.util.IdentityHashMap<>();

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> { if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return; var server = event.getServer();
            var leases = SPEED_LEASES.get(server);
            if (leases != null) leases.expire(server.overworld().getGameTime()).forEach(AllaySPJukebox::removeSpeedModifier);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> { var server = event.getServer();
            var leases = SPEED_LEASES.remove(server);
            if (leases != null) leases.clear().forEach(AllaySPJukebox::removeSpeedModifier);
            playerMusicState.clear();
        });
    }

    private static SourceLeaseBook<LivingEntity, UUID> leases(ServerPlayer player) {
        return SPEED_LEASES.computeIfAbsent(player.getServer(), ignored -> new SourceLeaseBook<>());
    }

    /**
     * 闁绘壕鏅涢宥夊棘椤撶姴娈犻柡鍐煐缁斿鎮堕崱娑氬従濞戞梹鍔楁慨鎼佸箑娴ｆ瓕瀚欑紒澶婎煼濞呭酣鏌呴悢宄邦唺闁告梻濮甸崹姘舵晬瀹€鍕╂慨婵勫灩閸炲鈧稒蓱绾姴顭宠箛鎾村闁稿繑婀圭划顒勬偝閳轰緡鍟€婵﹢鏅茬粻娆愮┍濠靛牊娈岄梺顐ゅ枎鐎圭牗uff
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        removeSpeedFromAll(player);
        Integer currentState = playerMusicState.remove(player.getUUID());
        if (currentState != null && currentState != -1) {
            stopAllMusic(player);
            playerMusicState.remove(player.getUUID());
            removeSpeedFromAll(player);
        }
    }

    /**
     * 闁衡偓閸洘鑲犻幖瀛樻煥缂嶅宕ラ鈧崺宀€鎷犻妷銉︽殸闁绘娲﹀┃鈧梻濠傚帠缁犱即鎯冮崟顓炶礋閻庣顔愮槐浼村箰娴ｈ绠掗柤鏉挎噺濠€鐗堢?+ 閺夆晛鈧喖鍤嬮柟闀愮劍濠€渚€鎳撻崨顖涚暠闁圭鍋撻柡鍫濐槺鐢櫣鈧顔愮槐娆撳础閹惰姤顎嶉弶鈺傚灱閸忔﹢鎯囩€ｎ亜鐓傚ù鐘崇墱濞堟垿鎮抽埡渚囧晙闁挎稑顦埀?
     * PlayerLookup.tracking 濞戞挸绉撮幆鍫ユ偝閳轰緡鍟€闁煎浜滅换渚€鏁嶇仦鐐珚闁归潧顑呮慨鈺呭礉閻樻彃寮抽柟闀愮劍濠€渚€鎳撻崨顐熷亾?
     * 濞ｅ浂鍠栭ˇ鏌ユ晬濮橆剦鍚傞柡鍫㈠劋閹搁亶寮ㄩ悙顒侇槯闁稿繑婀圭划顒佺閸濆嫭鍎斿☉鎾崇Х椤棝鏁嶉崼婵嗘枾闁稿繐鐗嗚ぐ褔宕ｉ幋鐐茬槷闁哄牆顦抽埀顒€鎷戠槐姘跺灳閺傝　鍋撻弮鈧弫鍏肩▔閸濆嫭鍊婚梻鍕缁诲酣鎮抽埡渚囧晙闁肩厧鍟ú鍧楃嵁閹稿孩灏￠柨娑樺鐎靛瞼鈧箍鍨哄┃鈧☉鎾亾闁奸攱鐣埀?
     */
    private static java.util.Set<ServerPlayer> getAudience(ServerPlayer player) {
        java.util.Set<ServerPlayer> audience = new java.util.HashSet<>(player.serverLevel().players());
        audience.add(player);
        return audience;
    }

    /**
     * Stop all jukebox music for a player by sending StopSoundS2CPacket
     */
    public static void stopAllMusic(ServerPlayer player) {
        // 闁告碍鍨剁€垫棃寮垫径搴樺亾?+ 闂傚嫬瀚换搴ㄥ箥閳ь剟寮垫径灞借礋閻庣娉涜ぐ鍌炲磻濠婂嫷鍓鹃柛鏍ф嫅缁辨繃绌卞┑濠勬闁肩厧鍟ú鍧楃嵁閹稿孩灏￠柣銊ュ閻撹埖绋婇幇顒夊殸闁圭鍋撻柡鍫濐槸閹瀵煎Δ鍛幋闁煎疇妫勬禒鐘崇▔鐎ｅ墎绀勫☉鎾诡嚙椤撳綊寮垫潪鎵伇闁肩柉鎻槐?
        for (ServerPlayer audience : getAudience(player)) {
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.getId(), SoundSource.RECORDS));
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.getId(), SoundSource.RECORDS));
        }
        playerMusicState.put(player.getUUID(), -1);
    }

    /**
     * Stop old music and immediately play the new mode's music from the beginning
     */
    private static void switchMusic(ServerPlayer player, int newMode) {
        SoundEvent newSound = (newMode == AllayJukeboxItem.MODE_SPEED) ? github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.get() : github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.get();
        Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(newSound);
        // 闁圭鍋撻柡鍫濐槸閹瀵煎Δ鍐╂殢闁告艾濂旂粩鎾⒕韫囨梹绨氱紒澶婄Т閻℃瑩鏁嶇仦鑲╃閻犲洣娴囩€垫牠宕堕弶鎴濇暥闁绘壕鏅涢宥夊触椤掆偓閸╁矂鎯冮崟顖滃従濞戞梹鍔橀幑锝夊箻椤撴繄顏遍柤?
        long seed = player.getRandom().nextLong();
        // 闁告碍鍨剁€垫棃寮垫径搴樺亾閸涱厽瀚查弶鈺佲偓鐔煎殝闁兼澘鎳庣粻宥夊箻椤撱劎绐楅柛娆樹簻娴犵姴顫㈤姀鈩冩嫳闁告梻鍠曢崗妯荤▔閵堝嫰鍤嬮柡鍕捣閳ユ﹢鎯冮崟顖滃従濞?ID闁挎稑鑻崯鈧柟缁㈠幗閺備線寮悧鍩礁顕ｈ箛瀣у亾?
        for (ServerPlayer audience : getAudience(player)) {
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_HEAL_MUSIC.getId(), SoundSource.RECORDS));
            audience.connection.send(new ClientboundStopSoundPacket(github.com.gengyoubo.sscfa.init.AddonSounds.ALLAY_SPEED_MUSIC.getId(), SoundSource.RECORDS));
            // 闂傚﹥濞婇崳?0.12闁挎稑鐗嗙敮?0.06 闁?2 闁稿﹤绋勭槐姘舵晬濞戞碍鏆忛悗鍦仒缂嶅鎹勯悢濠氬殝闂傚﹨娅曢弲銉╁礌閸滃啰绀夐悹浣叉櫊閻撹埖绋婇幇顒婄矗婵犙勫姌缁愶繝姊捐箛鏂跨槷闁哄牆顦抽埀顒€鎳愪簺闁告棏鐓夌槐娆撴嚀瀹€鍕闂佽棄顦﹢顏堟煂婵犲啯鏉瑰ù锝呯Ф閻ゅ棝鏁?
            audience.connection.send(new ClientboundSoundEntityPacket(entry, SoundSource.RECORDS,
                    player, 0.25f, 1.0f, seed));
        }

        playerMusicState.put(player.getUUID(), newMode);
    }


    /**
     * Called every tick for each allay_sp player from the server tick event
     */
    public static void tick(ServerPlayer player) {
        // 鐎点倕顦悳顖濄亹閵忊檧鍋撴笟鈧Λ顒勫箳瑜嶆晶鐘电磾椤曞棛绐楅柛蹇撶墕閸ㄥ€熴亹閵忊檧鍋撴担绋挎櫃闁?SkillBlocker闁挎稑鐗嗛悺褏绮敂鑳洬闁瑰嘲鍚嬬敮?+ commandTags 缂佺偓瀵ч埀顑嫬顥囬柟璇查獜缁辨岸鏁?
        // 闂傚牏鍋為崑濠囨倶?SP 闁绘壕鏅涢宥呅?tick 闁告瑯浜ｆ慨铏▔閳ь剙鈻?CCA 闁哄被鍎撮妤呮晬閸繂鏂ч弶鈺傤焾椤╋箓宕楅崼锝囩畺 SkillBlocker 闁归潧绉撮崹鍊熴亹閵忊檧鍋撴笟濠勭
        net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
        boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
        if (!isAllaySp) {
            Integer currentState = playerMusicState.getOrDefault(player.getUUID(), -1);
            if (currentState != -1) {
                stopAllMusic(player);
                removeSpeedFromAll(player);
            }
            return;
        }
        if (SkillBlocker.isSkillBlocked(player, "allay", "jukebox_charge")) {
            return;
        }

        // Find jukebox item in inventory (should be in slot 1)
        ItemStack jukeboxStack = player.getInventory().getItem(1);
        
        // If item is missing, treat as inactive -> cleanup
        if (!jukeboxStack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get())) {
            Integer currentState = playerMusicState.getOrDefault(player.getUUID(), -1);
            if (currentState != -1) {
                stopAllMusic(player);
                removeSpeedFromAll(player);
            }
            return;
        }

        boolean isActive = AllayJukeboxItem.isActive(jukeboxStack);

        if (!isActive) {
            // Remove speed modifiers from all nearby entities when deactivated
            removeSpeedFromAll(player);
            // Stop music if it was playing
            Integer lastState = playerMusicState.get(player.getUUID());
            if (lastState != null && lastState != -1) {
                stopAllMusic(player);
            }
            return;
        }

        int charge = AllayJukeboxItem.getCharge(jukeboxStack);
        if (charge <= 0) {
            // Out of charge, deactivate
            AllayJukeboxItem.setActive(jukeboxStack, false);
            removeSpeedFromAll(player);
            stopAllMusic(player);
            return;
        }

        // Consume 1 charge per second (every 20 ticks)
        if (player.tickCount % 20 == 0) {
            AllayJukeboxItem.setCharge(jukeboxStack, charge - 1);
        }

        int mode = AllayJukeboxItem.getMode(jukeboxStack);

        // ===== Music: 婵?tick 婵☆偀鍋撴繛?mode 闁告瑦锚鐎垫煡鏁嶉崼婊呯憹濞撴碍绻嗙粋鍡欌偓鍦仒缂嶅骞嶉锝呬紟闁挎稑濂旂换姘辨嫚娴ｇ鐎兼俊顖椻偓鍐茬ギ闁哄啳顔愮槐?=====
        Integer lastMusicMode = playerMusicState.getOrDefault(player.getUUID(), -1);
        if (lastMusicMode != mode) {
            // Mode changed or first time: stop old, play new immediately
            switchMusic(player, mode);
        }

        // 闁诡儸鍡楀幋濞村吋锚鐎垫煡鏁嶅鍐茬槺闁搞儲娼欓悿鍕媴閹炬潙顥囬柟?getEntitiesByClass) + buff 濠靛倹顨堥弫?缂佸顭峰▍?闂傚嫬绉归。鍫曞礆閻楀牏妲?5 tick 濞戞挴鍋撴繛鍡忔缁辨瑩宕㈤悢鍝ユЖ tick闁挎稑顦埀?
        // applySpeedModifier 妤犵偛鍊婚悺鎴︽晬閸繂鍤掗柡鍫濐槸閸垳鎹勭€圭姷绠栭柨娑橆檧缁辨繈宕堕悙棰濇敤闁哄牜鍓欏銊バ?60t闁靛棔鎭璴eanup 闁哄牜鍓欏銊バ?40t闁挎稑鐗嗗搴㈢▔?5 闁汇劌瀚埀顒€绉甸弳鐔兼晬鐏炶姤鍤掑☉鎿冨幒缁楀宕ｅ鍫㈢闁?
        // 闁哄倹濯界换姗€鎳犻崘銊︾函闁兼澘鎳忓〒鑸靛緞濮橆厽鐝?5t 闁兼儳鍢叉慨鐐烘焻?閻炴凹鍋勫ú鏍偘閳ь剟鏁嶅畝鍐ㄢ偓婵嬫儑闂傚鐟濋柛娆樺灠閻у倿濡?
        if (player.tickCount % 5 == 0) {
            List<LivingEntity> nearbyEntities = getNearbyWhitelistEntities(player);

            if (mode == AllayJukeboxItem.MODE_HEAL) {
                // Heal mode: every 3 seconds (60 ticks), heal 1 HP
                if (player.tickCount % 60 == 0) {
                    for (LivingEntity entity : nearbyEntities) {
                        entity.heal(1.0f);
                    }
                }
                // 濡増绻傞ˇ缁樻櫠閻愬灚鎶勯柨娑欑婵兘骞?I闁?0% 闁告垵绻嬪┑鈧柨娑樼灱閻擃參寮捄鍝勭厱闁哄倸搴滅槐? 濮掓稑瀚缓楣冩晬閸︽弮SORPTION闁挎稑鐣甿plifier 0 = 4HP = 2 濡増顨愮槐婵嬪触鐏炵伕浣割嚕韫囨挸鐓曢柡鍌氬簻缁?
                // 闂侇偀鍋撻柛鎴濇惈濞叉牜鎮伴埀顒€螣閳ュ磭纭€闁告艾楠告禒鐘差潰閵忕姴鐓曢柡鍌氬簻缁辨繈宕滈埡鈧紞?effect 闁?闁?0t 闁告劕鎳撶换鍐嫉閻曞倻绀塷nRemoved 闁煎浜滄慨鈺呭箥閿濆鐝熼悗鐟板暞閺岋箓宕濋悩鍨暠闁硅翰鍊楀ù姗€鏁嶇仦鑲╃憹閻犲浂鍨伴崹褰掓偝閳轰緡鍟€闁稿繗娉涢悾鐘诲箮閵堝洦绂?
                int buffRefreshDuration = BAL.i("buff_refresh_duration", BUFF_REFRESH_DURATION);
                for (LivingEntity entity : nearbyEntities) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffRefreshDuration, 0, false, false, true));
                    entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, buffRefreshDuration, 0, false, false, true));
                }
                // Remove speed modifier when in heal mode
                removeSpeedFromAll(player);
            } else {
                // Speed mode: apply 10% speed modifier
                int buffRefreshDuration = BAL.i("buff_refresh_duration", BUFF_REFRESH_DURATION);
                for (LivingEntity entity : nearbyEntities) {
                    leases(player).renew(entity, player.getUUID(), player.getServer().overworld().getGameTime() + 20);
                    applySpeedModifier(entity);
                    // 濡増绻傞ˇ缁樻櫠閻愬灚鎶勯柨娑欑閳ь兙鍎撮幓?I闁挎稑鐗忛悡顓㈠籍鐠哄搫鐓曢柡鍌氬簻缁辨繂袙?5 tick 閻炴稏鍎茬€垫洟鏁?
                    entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, buffRefreshDuration, 0, false, false, true));
                }
                // Clean up speed modifiers from entities that moved out of range
                cleanupOutOfRangeEntities(player, nearbyEntities);
            }
        }
    }

    private static List<LivingEntity> getNearbyWhitelistEntities(ServerPlayer player) {
        double range = BAL.d("range", RANGE);
        AABB box = new AABB(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range
        );

        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            double dist = entity.distanceToSqr(player);
            if (dist > range * range) return false;
            if (entity == player) return true;
            // Use the allay whitelist
            return AllaySPGroupHeal.isInWhitelist(player, entity);
        });
    }

    private static void applySpeedModifier(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
        if (existing == null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_UUID, SPEED_MODIFIER_NAME,
                    BAL.d("speed_bonus", SPEED_BONUS), AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        speedAttr.removeModifier(SPEED_MODIFIER_UUID);
    }

    private static void removeSpeedFromAll(ServerPlayer player) {
        var book = SPEED_LEASES.get(player.getServer());
        if (book != null) book.release(player.getUUID()).forEach(AllaySPJukebox::removeSpeedModifier);
    }

    private static void cleanupOutOfRangeEntities(ServerPlayer player, List<LivingEntity> inRange) {
        leases(player).retain(player.getUUID(), new java.util.HashSet<>(inRange))
                .forEach(AllaySPJukebox::removeSpeedModifier);
    }
}
