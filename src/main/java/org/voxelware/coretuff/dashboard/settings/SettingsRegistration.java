package org.voxelware.coretuff.dashboard.settings;

import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingDefinition;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.BooleanType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.DoubleType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.IntegerType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.StringType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.LongType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.EnumType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.ListType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.MapType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.PasswordType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.ColorType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.DurationType;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType.JsonType;

import java.util.List;

public final class SettingsRegistration {

    private static final SettingType STR = new StringType();
    private static final SettingType INT = new IntegerType();
    private static final SettingType DBL = new DoubleType();
    private static final SettingType LNG = new LongType();
    private static final SettingType BOOL = new BooleanType();
    private static final SettingType ENUM = new EnumType();
    private static final SettingType LIST = new ListType();
    private static final SettingType MAP = new MapType();
    private static final SettingType CLR = new ColorType();
    private static final SettingType DUR = new DurationType();
    private static final SettingType JSON = new JsonType();
    private static final SettingType PASS = new PasswordType();

    private SettingsRegistration() {}

    public static void registerAll(SettingsRegistry registry) {
        registerCore(registry);
        registerEconomy(registry);
        registerTeleport(registry);
        registerUtility(registry);
        registerKits(registry);
        registerLand(registry);
        registerModeration(registry);
        registerSecurity(registry);
    }

    private static void registerCore(SettingsRegistry registry) {
        registry.register("core", null, List.of(
            def("prefix", STR, "&f&l[&b&lCoreTuff&f&l]:&r ", "general", "Message prefix", null, null, null),
            def("postfix", STR, "", "general", "Message postfix", null, null, null),
            def("nopermission", STR, "&c You dont have permission to use this command!", "general", "No permission message", null, null, null),
            def("storage-method", ENUM, "h2", "database", "Storage method for dump command", null, null, List.of("h2", "mysql", "mariadb", "postgresql", "mongodb")),
            def("data.address", STR, "localhost", "database", "Database server address", null, null, null),
            def("data.database", STR, "minecraft", "database", "Database name", null, null, null),
            def("data.username", STR, "root", "database", "Database username", null, null, null),
            def("data.password", PASS, "", "database", "Database password", null, null, null)
        ));
    }

    private static void registerEconomy(SettingsRegistry registry) {
        registry.register("economy", null, List.of(
            def("currency.name", STR, "Dollar", "general", "Singular currency name", null, null, null),
            def("currency.name-plural", STR, "Dollars", "general", "Plural currency name", null, null, null),
            def("currency.symbol", STR, "$", "general", "Currency symbol", null, null, null),
            def("currency.decimal-places", INT, 2, "general", "Number of decimal places", 0, 10, null),
            def("defaults.starting-balance", DBL, 0.0, "balances", "Starting balance for new players", 0.0, null, null),
            def("defaults.max-balance", DBL, 999999999.99, "balances", "Maximum balance a player can hold", 0.0, null, null),
            def("transactions.pay-tax-percent", DBL, 0.0, "transactions", "Tax percentage deducted from payments", 0.0, 100.0, null),
            def("transactions.minimum-transfer", DBL, 0.01, "transactions", "Minimum transfer amount", 0.0, null, null),
            def("transactions.pay-tax-bypass-permission", STR, "coretuff.economy.bypass-tax", "transactions", "Permission that bypasses payment tax", null, null, null),
            def("interest.enabled", BOOL, false, "interest", "Enable interest accumulation", null, null, null),
            def("interest.rate-percent", DBL, 0.5, "interest", "Interest rate percentage per interval", 0.0, 100.0, null),
            def("interest.interval-hours", INT, 24, "interest", "Hours between interest applications", 1, null, null),
            def("interest.max-balance", DBL, 100000.0, "interest", "Maximum balance that earns interest", 0.0, null, null),
            def("interest.interest-bypass-permission", STR, "coretuff.economy.bypass-interest", "interest", "Permission that bypasses interest", null, null, null),
            def("sounds.pay-sent.enabled", BOOL, true, "sounds", "Pay sent sound enabled", null, null, null),
            def("sounds.pay-sent.sound", STR, "ENTITY_ITEM_PICKUP", "sounds", "Pay sent sound effect", null, null, null),
            def("sounds.pay-sent.volume", DBL, 0.5, "sounds", "Pay sent sound volume", 0.0, null, null),
            def("sounds.pay-sent.pitch", DBL, 0.8, "sounds", "Pay sent sound pitch", 0.0, null, null),
            def("sounds.pay-received.enabled", BOOL, true, "sounds", "Pay received sound enabled", null, null, null),
            def("sounds.pay-received.sound", STR, "ENTITY_EXPERIENCE_ORB_PICKUP", "sounds", "Pay received sound effect", null, null, null),
            def("sounds.pay-received.volume", DBL, 0.7, "sounds", "Pay received sound volume", 0.0, null, null),
            def("sounds.pay-received.pitch", DBL, 1.2, "sounds", "Pay received sound pitch", 0.0, null, null),
            def("sounds.balance-check.enabled", BOOL, true, "sounds", "Balance check sound enabled", null, null, null),
            def("sounds.balance-check.sound", STR, "BLOCK_NOTE_BLOCK_CHIME", "sounds", "Balance check sound effect", null, null, null),
            def("sounds.balance-check.volume", DBL, 0.5, "sounds", "Balance check sound volume", 0.0, null, null),
            def("sounds.balance-check.pitch", DBL, 1.0, "sounds", "Balance check sound pitch", 0.0, null, null),
            def("sounds.eco-give.enabled", BOOL, true, "sounds", "Eco give sound enabled", null, null, null),
            def("sounds.eco-give.sound", STR, "ENTITY_PLAYER_LEVELUP", "sounds", "Eco give sound effect", null, null, null),
            def("sounds.eco-give.volume", DBL, 0.7, "sounds", "Eco give sound volume", 0.0, null, null),
            def("sounds.eco-give.pitch", DBL, 1.0, "sounds", "Eco give sound pitch", 0.0, null, null),
            def("sounds.eco-take.enabled", BOOL, true, "sounds", "Eco take sound enabled", null, null, null),
            def("sounds.eco-take.sound", STR, "BLOCK_ANVIL_LAND", "sounds", "Eco take sound effect", null, null, null),
            def("sounds.eco-take.volume", DBL, 0.5, "sounds", "Eco take sound volume", 0.0, null, null),
            def("sounds.eco-take.pitch", DBL, 1.5, "sounds", "Eco take sound pitch", 0.0, null, null),
            def("sounds.eco-set.enabled", BOOL, true, "sounds", "Eco set sound enabled", null, null, null),
            def("sounds.eco-set.sound", STR, "BLOCK_ANVIL_USE", "sounds", "Eco set sound effect", null, null, null),
            def("sounds.eco-set.volume", DBL, 0.5, "sounds", "Eco set sound volume", 0.0, null, null),
            def("sounds.eco-set.pitch", DBL, 1.0, "sounds", "Eco set sound pitch", 0.0, null, null),
            def("sounds.eco-reset.enabled", BOOL, true, "sounds", "Eco reset sound enabled", null, null, null),
            def("sounds.eco-reset.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "sounds", "Eco reset sound effect", null, null, null),
            def("sounds.eco-reset.volume", DBL, 0.6, "sounds", "Eco reset sound volume", 0.0, null, null),
            def("sounds.eco-reset.pitch", DBL, 0.5, "sounds", "Eco reset sound pitch", 0.0, null, null),
            def("sounds.insufficient-funds.enabled", BOOL, true, "sounds", "Insufficient funds sound enabled", null, null, null),
            def("sounds.insufficient-funds.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "sounds", "Insufficient funds sound effect", null, null, null),
            def("sounds.insufficient-funds.volume", DBL, 0.6, "sounds", "Insufficient funds sound volume", 0.0, null, null),
            def("sounds.insufficient-funds.pitch", DBL, 0.3, "sounds", "Insufficient funds sound pitch", 0.0, null, null)
        ));
    }

    private static void registerTeleport(SettingsRegistry registry) {
        registry.register("teleport", null, List.of(
            def("Teleportation.tpSelf", STR, "&cYou cannot teleport to yourself.", "messages", "Self-teleport message", null, null, null),
            def("Teleportation.tpsenderMessage", STR, "teleportation request sent to {player}", "messages", "Request sent confirmation", null, null, null),
            def("Teleportation.tpasktargetMessage", LIST, List.of("{player} wants to teleport to you.", "You have {time} seconds to accept this request.", "You can either accept this request with /tpaccept or decline with /tpdeny."), "messages", "TPA request target message", null, null, null),
            def("Teleportation.tpheretargetMessage", LIST, List.of("{player} wants you to teleport to them.", "You have {time} seconds to accept this request.", "You can either accept this request with /tpaccept or decline with /tpdeny."), "messages", "TPHere request target message", null, null, null),
            def("Teleportation.tpCooldown", INT, 200, "cooldowns", "TPA cooldown in seconds", 0, null, null),
            def("Teleportation.tpExpire", INT, 120, "cooldowns", "Request expiry in seconds", 0, null, null),
            def("Teleportation.tpNoPendingReq", STR, "&cNo pending request.", "messages", "No pending request", null, null, null),
            def("Teleportation.tpReqExpired", STR, "&cRequest expired.", "messages", "Request expired", null, null, null),
            def("Teleportation.tpNoReqDeny", STR, "&cNo request to deny.", "messages", "Nothing to deny", null, null, null),
            def("Teleportation.tpReqDenied", STR, "&cYour teleport request was denied.", "messages", "Sender notified of denial", null, null, null),
            def("Teleportation.tpDenied", STR, "&eDenied teleport request.", "messages", "Target confirm denial", null, null, null),
            def("Teleportation.rtpCooldown", INT, 80, "cooldowns", "RTP cooldown in seconds", 0, null, null),
            def("Teleportation.rtpOnCooldown", STR, "&cOn cooldown!", "messages", "RTP cooldown message", null, null, null),
            def("Teleportation.rtpLocSearch", STR, "&7Searching for location...", "messages", "RTP searching message", null, null, null),
            def("Teleportation.rtpNoSafeLoc", STR, "&cNo Safe location found.", "messages", "No safe location", null, null, null),
            def("Teleportation.rtpTeleported", STR, "&aTeleported!", "messages", "RTP success message", null, null, null),
            def("Teleportation.rtpTeleportFailed", STR, "&cTeleport failed.", "messages", "RTP failure message", null, null, null),
            def("Teleportation.backCooldown", INT, 50, "cooldowns", "Back cooldown in seconds", 0, null, null),
            def("Teleportation.backNoPrevLocation", STR, "&cNo previous location.", "messages", "No previous location", null, null, null),
            def("Teleportation.backOnCooldown", STR, "&cYou must wait {time} seconds to use /back again", "messages", "Back cooldown message", null, null, null),
            def("Teleportation.playerIgnore", STR, "&cThis player is ignoring you.", "ignore", "Player ignoring you message", null, null, null),
            def("Teleportation.ignoreYourself", STR, "&cYou cannot ignore yourself.", "ignore", "Self-ignore prevention message", null, null, null),
            def("Teleportation.alreadlyIgnoring", STR, "&cAlready ignoring this player.", "ignore", "Already ignoring message", null, null, null),
            def("Teleportation.ignoreAdd", STR, "&aYou are now ignoring {player}", "ignore", "Ignore added message", null, null, null),
            def("Teleportation.notIgnoring", STR, "&cYou are not ignoring this player.", "ignore", "Not ignoring message", null, null, null),
            def("Teleportation.ignoreRevoke", STR, "&aYou stopped ignoring {player}", "ignore", "Ignore removed message", null, null, null),
            def("Teleportation.tpCountMove", STR, "&aTeleporting in {time} seconds. Don't move!", "countdown", "Countdown movement warning", null, null, null),
            def("Teleportation.tpCountdown", STR, "&7Teleporting in {time}...", "countdown", "Countdown tick message", null, null, null),
            def("Teleportation.tpCancMoved", STR, "&cTeleport cancelled (you moved)", "countdown", "Cancelled by movement", null, null, null),
            def("Teleportation.tpCancDamage", STR, "&cTeleport cancelled (damage taken)", "countdown", "Cancelled by damage", null, null, null),
            def("Teleportation.rtpGeneratedChunksOnly", BOOL, true, "general", "Only RTP to generated chunks", null, null, null),
            def("Sounds.request-send.enabled", BOOL, true, "sounds", "Request send sound enabled", null, null, null),
            def("Sounds.request-send.sound", STR, "BLOCK_NOTE_BLOCK_PLING", "sounds", "Request send sound effect", null, null, null),
            def("Sounds.request-send.volume", DBL, 0.8, "sounds", "Request send sound volume", 0.0, null, null),
            def("Sounds.request-send.pitch", DBL, 1.2, "sounds", "Request send sound pitch", 0.0, null, null),
            def("Sounds.request-receive.enabled", BOOL, true, "sounds", "Request receive sound enabled", null, null, null),
            def("Sounds.request-receive.sound", STR, "BLOCK_AMETHYST_BLOCK_CHIME", "sounds", "Request receive sound effect", null, null, null),
            def("Sounds.request-receive.volume", DBL, 0.8, "sounds", "Request receive sound volume", 0.0, null, null),
            def("Sounds.request-receive.pitch", DBL, 1.1, "sounds", "Request receive sound pitch", 0.0, null, null),
            def("Sounds.request-accept.enabled", BOOL, true, "sounds", "Request accept sound enabled", null, null, null),
            def("Sounds.request-accept.sound", STR, "ENTITY_ENDERMAN_TELEPORT", "sounds", "Request accept sound effect", null, null, null),
            def("Sounds.request-accept.volume", DBL, 0.7, "sounds", "Request accept sound volume", 0.0, null, null),
            def("Sounds.request-accept.pitch", DBL, 1.25, "sounds", "Request accept sound pitch", 0.0, null, null),
            def("Sounds.request-denied.enabled", BOOL, true, "sounds", "Request denied sound enabled", null, null, null),
            def("Sounds.request-denied.sound", STR, "BLOCK_ANVIL_LAND", "sounds", "Request denied sound effect", null, null, null),
            def("Sounds.request-denied.volume", DBL, 0.5, "sounds", "Request denied sound volume", 0.0, null, null),
            def("Sounds.request-denied.pitch", DBL, 1.5, "sounds", "Request denied sound pitch", 0.0, null, null),
            def("Sounds.rtp-search.enabled", BOOL, true, "sounds", "RTP search sound enabled", null, null, null),
            def("Sounds.rtp-search.sound", STR, "BLOCK_RESPAWN_ANCHOR_CHARGE", "sounds", "RTP search sound effect", null, null, null),
            def("Sounds.rtp-search.volume", DBL, 0.5, "sounds", "RTP search sound volume", 0.0, null, null),
            def("Sounds.rtp-search.pitch", DBL, 1.3, "sounds", "RTP search sound pitch", 0.0, null, null),
            def("Sounds.rtp-success.enabled", BOOL, true, "sounds", "RTP success sound enabled", null, null, null),
            def("Sounds.rtp-success.sound", STR, "ENTITY_ENDERMAN_TELEPORT", "sounds", "RTP success sound effect", null, null, null),
            def("Sounds.rtp-success.volume", DBL, 0.8, "sounds", "RTP success sound volume", 0.0, null, null),
            def("Sounds.rtp-success.pitch", DBL, 1.0, "sounds", "RTP success sound pitch", 0.0, null, null),
            def("Sounds.rtp-countdown.enabled", BOOL, true, "sounds", "RTP countdown sound enabled", null, null, null),
            def("Sounds.rtp-countdown.sound", STR, "BLOCK_NOTE_BLOCK_HAT", "sounds", "RTP countdown sound effect", null, null, null),
            def("Sounds.rtp-countdown.volume", DBL, 0.45, "sounds", "RTP countdown sound volume", 0.0, null, null),
            def("Sounds.rtp-countdown.pitch", DBL, 1.4, "sounds", "RTP countdown sound pitch", 0.0, null, null),
            def("Sounds.rtp-fail.enabled", BOOL, true, "sounds", "RTP fail sound enabled", null, null, null),
            def("Sounds.rtp-fail.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "sounds", "RTP fail sound effect", null, null, null),
            def("Sounds.rtp-fail.volume", DBL, 0.8, "sounds", "RTP fail sound volume", 0.0, null, null),
            def("Sounds.rtp-fail.pitch", DBL, 0.7, "sounds", "RTP fail sound pitch", 0.0, null, null),
            def("Sounds.back-fail.enabled", BOOL, true, "sounds", "Back fail sound enabled", null, null, null),
            def("Sounds.back-fail.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "sounds", "Back fail sound effect", null, null, null),
            def("Sounds.back-fail.volume", DBL, 0.8, "sounds", "Back fail sound volume", 0.0, null, null),
            def("Sounds.back-fail.pitch", DBL, 0.7, "sounds", "Back fail sound pitch", 0.0, null, null)
        ));
    }

    private static void registerUtility(SettingsRegistry registry) {
        registry.register("utility", null, List.of(
            def("playerOffline", STR, "&cPlayer is offline.", "messages", "Target offline message", null, null, null),
            def("cmdUse1", STR, "&c USage: /{command} <player>", "messages", "Generic command usage", null, null, null),
            def("Gamemode_Shorthand.creative", STR, "Your gamemode has been set to Creative!", "gamemode", "Creative mode set message", null, null, null),
            def("Gamemode_Shorthand.survival", STR, "Your gamemode has been set to Survival!", "gamemode", "Survival mode set message", null, null, null),
            def("Gamemode_Shorthand.spectator", STR, "Your gamemode has been set to Spectator!", "gamemode", "Spectator mode set message", null, null, null),
            def("Gamemode_Shorthand.adventure", STR, "Your gamemode has been set to Adventure!", "gamemode", "Adventure mode set message", null, null, null),
            def("Gamemode_Shorthand.gamemodeSet", STR, "Changed gamemode of {player} to {gamemode}", "gamemode", "Gamemode set by other message", null, null, null),
            def("Gamemode_Shorthand.gamemodeChange", STR, "Your gamemode changed to {gamemode} by {player}", "gamemode", "Gamemode target notification", null, null, null),
            def("Fly.cooldown", STR, "&cWait {time} minuets before using this again.", "fly", "Fly cooldown message", null, null, null),
            def("Fly.infly", STR, "You are alreadly in fly mode", "fly", "Already flying message", null, null, null),
            def("Fly.flyenable", STR, "Fly enabled for {time} minuets.", "fly", "Fly enabled message", null, null, null),
            def("Fly.flyexpire", STR, "Fly expired.", "fly", "Fly expired message", null, null, null),
            def("Fly.flyduration", INT, 2, "fly", "Flight duration in minutes", 0, null, null),
            def("Fly.flycooldown", INT, 5, "fly", "Fly cooldown in minutes", 0, null, null),
            def("Heal.healed", STR, "You have been healed", "heal", "Self-heal message", null, null, null),
            def("Heal.healedOther", STR, "You have healed {player}", "heal", "Heal other sender message", null, null, null),
            def("Heal.healedTarget", STR, "You have been healed by {player}", "heal", "Heal target message", null, null, null),
            def("Feed.filled", STR, "Your have been fed", "feed", "Self-feed message", null, null, null),
            def("Feed.feedOther", STR, "You have fed {player}", "feed", "Feed other sender message", null, null, null),
            def("Feed.feedTarget", STR, "You have been fed by {player}", "feed", "Feed target message", null, null, null),
            def("Sounds.fly-enable.enabled", BOOL, true, "sounds", "Fly enable sound enabled", null, null, null),
            def("Sounds.fly-enable.sound", STR, "ENTITY_PHANTOM_FLAP", "sounds", "Fly enable sound effect", null, null, null),
            def("Sounds.fly-enable.volume", DBL, 0.8, "sounds", "Fly enable sound volume", 0.0, null, null),
            def("Sounds.fly-enable.pitch", DBL, 1.15, "sounds", "Fly enable sound pitch", 0.0, null, null),
            def("Sounds.fly-expire.enabled", BOOL, true, "sounds", "Fly expire sound enabled", null, null, null),
            def("Sounds.fly-expire.sound", STR, "BLOCK_BEACON_DEACTIVATE", "sounds", "Fly expire sound effect", null, null, null),
            def("Sounds.fly-expire.volume", DBL, 0.8, "sounds", "Fly expire sound volume", 0.0, null, null),
            def("Sounds.fly-expire.pitch", DBL, 1.0, "sounds", "Fly expire sound pitch", 0.0, null, null),
            def("Sounds.heal.enabled", BOOL, true, "sounds", "Heal sound enabled", null, null, null),
            def("Sounds.heal.sound", STR, "ENTITY_PLAYER_LEVELUP", "sounds", "Heal sound effect", null, null, null),
            def("Sounds.heal.volume", DBL, 0.7, "sounds", "Heal sound volume", 0.0, null, null),
            def("Sounds.heal.pitch", DBL, 1.4, "sounds", "Heal sound pitch", 0.0, null, null),
            def("Sounds.feed.enabled", BOOL, true, "sounds", "Feed sound enabled", null, null, null),
            def("Sounds.feed.sound", STR, "ENTITY_GENERIC_EAT", "sounds", "Feed sound effect", null, null, null),
            def("Sounds.feed.volume", DBL, 0.7, "sounds", "Feed sound volume", 0.0, null, null),
            def("Sounds.feed.pitch", DBL, 1.1, "sounds", "Feed sound pitch", 0.0, null, null)
        ));
    }

    private static void registerKits(SettingsRegistry registry) {
        registry.register("kits", null, List.of(
            def("kit-teleport-delay-seconds", INT, 0, "general", "Delay before kit is given (seconds)", 0, null, null),
            def("default-cooldown-seconds", INT, 0, "general", "Default cooldown for kits without one", 0, null, null),
            def("kits.starter.display-name", STR, "&aStarter Kit", "kits", "Starter kit display name", null, null, null),
            def("kits.starter.permission", STR, "coretuff.utility.kit.starter", "kits", "Starter kit permission", null, null, null),
            def("kits.starter.cooldown-seconds", INT, 86400, "kits", "Starter kit cooldown in seconds", 0, null, null),
            def("kits.starter.items", JSON, "[{\"material\":\"DIAMOND_SWORD\",\"amount\":1},{\"material\":\"COOKED_BEEF\",\"amount\":16},{\"material\":\"IRON_PICKAXE\",\"amount\":1},{\"material\":\"IRON_AXE\",\"amount\":1}]", "kits", "Starter kit items (JSON array)", null, null, null)
        ));
    }

    private static void registerLand(SettingsRegistry registry) {
        registry.register("lands", null, List.of(
            def("homes.teleport-delay-seconds", INT, 3, "homes", "Home teleport delay in seconds", 0, null, null),
            def("homes.default-home", STR, "home", "homes", "Default home name", null, null, null),
            def("homes.max-homes-permission-base", STR, "coretuff.land.sethome.", "homes", "Max homes permission base", null, null, null),
            def("homes.max-homes-default", INT, 1, "homes", "Default max homes", 1, null, null),
            def("homes.messages.set", STR, "&aHome &f{name} &ahas been set.", "homes", "Home set message", null, null, null),
            def("homes.messages.deleted", STR, "&cHome &f{name} &cdeleted.", "homes", "Home deleted message", null, null, null),
            def("homes.messages.teleported", STR, "&aTeleported to home &f{name}&a.", "homes", "Home teleport message", null, null, null),
            def("homes.messages.does-not-exist", STR, "&cHome &f{name} &cdoes not exist.", "homes", "Home not found message", null, null, null),
            def("homes.messages.no-homes", STR, "&cYou have no homes set.", "homes", "No homes message", null, null, null),
            def("homes.messages.list-header", STR, "&aYour homes:", "homes", "Home list header", null, null, null),
            def("homes.messages.list-entry", STR, "&8- &f{name}", "homes", "Home list entry format", null, null, null),
            def("homes.messages.limit", STR, "&cYou can only set up to {max} homes.", "homes", "Home limit message", null, null, null),
            def("homes.messages.invalid-name", STR, "&cHome name must be 1-32 alphanumeric characters.", "homes", "Invalid home name message", null, null, null),
            def("homes.messages.others-list-header", STR, "&a{player}'s homes:", "homes", "Other player's home list header", null, null, null),
            def("homes.sounds.set.enabled", BOOL, true, "homes", "Home set sound enabled", null, null, null),
            def("homes.sounds.set.sound", STR, "BLOCK_AMETHYST_BLOCK_CHIME", "homes", "Home set sound effect", null, null, null),
            def("homes.sounds.set.volume", DBL, 0.7, "homes", "Home set sound volume", 0.0, null, null),
            def("homes.sounds.set.pitch", DBL, 1.2, "homes", "Home set sound pitch", 0.0, null, null),
            def("homes.sounds.teleport.enabled", BOOL, true, "homes", "Home teleport sound enabled", null, null, null),
            def("homes.sounds.teleport.sound", STR, "ENTITY_ENDERMAN_TELEPORT", "homes", "Home teleport sound effect", null, null, null),
            def("homes.sounds.teleport.volume", DBL, 0.7, "homes", "Home teleport sound volume", 0.0, null, null),
            def("homes.sounds.teleport.pitch", DBL, 1.25, "homes", "Home teleport sound pitch", 0.0, null, null),
            def("homes.sounds.delete.enabled", BOOL, true, "homes", "Home delete sound enabled", null, null, null),
            def("homes.sounds.delete.sound", STR, "BLOCK_ANVIL_LAND", "homes", "Home delete sound effect", null, null, null),
            def("homes.sounds.delete.volume", DBL, 0.5, "homes", "Home delete sound volume", 0.0, null, null),
            def("homes.sounds.delete.pitch", DBL, 1.5, "homes", "Home delete sound pitch", 0.0, null, null),
            def("homes.sounds.list.enabled", BOOL, true, "homes", "Home list sound enabled", null, null, null),
            def("homes.sounds.list.sound", STR, "BLOCK_NOTE_BLOCK_CHIME", "homes", "Home list sound effect", null, null, null),
            def("homes.sounds.list.volume", DBL, 0.5, "homes", "Home list sound volume", 0.0, null, null),
            def("homes.sounds.list.pitch", DBL, 1.0, "homes", "Home list sound pitch", 0.0, null, null),
            def("warps.teleport-delay-seconds", INT, 3, "warps", "Warp teleport delay in seconds", 0, null, null),
            def("warps.economy-cost", DBL, 0.0, "warps", "Economy cost to use a warp", 0.0, null, null),
            def("warps.economy-cost-permission-bypass", STR, "coretuff.land.warps.bypass-cost", "warps", "Warp cost bypass permission", null, null, null),
            def("warps.max-warps-permission-base", STR, "coretuff.land.warps.setwarp.", "warps", "Max warps permission base", null, null, null),
            def("warps.messages.set", STR, "&aWarp &f{name} &ahas been created.", "warps", "Warp set message", null, null, null),
            def("warps.messages.deleted", STR, "&cWarp &f{name} &cdeleted.", "warps", "Warp deleted message", null, null, null),
            def("warps.messages.teleported", STR, "&aTeleported to warp &f{name}&a.", "warps", "Warp teleport message", null, null, null),
            def("warps.messages.does-not-exist", STR, "&cWarp &f{name} &cdoes not exist.", "warps", "Warp not found message", null, null, null),
            def("warps.messages.no-warps", STR, "&cNo warps exist on this server.", "warps", "No warps message", null, null, null),
            def("warps.messages.list-header", STR, "&6Warps:", "warps", "Warp list header", null, null, null),
            def("warps.messages.list-entry", STR, "&8- &f{name}", "warps", "Warp list entry format", null, null, null),
            def("warps.messages.invalid-name", STR, "&cWarp name must be 1-32 alphanumeric characters.", "warps", "Invalid warp name message", null, null, null),
            def("warps.messages.cost", STR, "&aTeleporting to &f{name} &acosts {cost}.", "warps", "Warp cost message", null, null, null),
            def("warps.messages.insufficient-funds", STR, "&cYou need {cost} to use this warp.", "warps", "Warp insufficient funds message", null, null, null),
            def("warps.messages.limit", STR, "&cYou can only set up to {max} warps.", "warps", "Warp limit message", null, null, null),
            def("warps.messages.others-teleported", STR, "&aTeleported {player} to warp &f{name}&a.", "warps", "Teleport other player to warp message", null, null, null),
            def("warps.sounds.set.enabled", BOOL, true, "warps", "Warp set sound enabled", null, null, null),
            def("warps.sounds.set.sound", STR, "BLOCK_END_PORTAL_FRAME_FILL", "warps", "Warp set sound effect", null, null, null),
            def("warps.sounds.set.volume", DBL, 0.7, "warps", "Warp set sound volume", 0.0, null, null),
            def("warps.sounds.set.pitch", DBL, 1.0, "warps", "Warp set sound pitch", 0.0, null, null),
            def("warps.sounds.teleport.enabled", BOOL, true, "warps", "Warp teleport sound enabled", null, null, null),
            def("warps.sounds.teleport.sound", STR, "ENTITY_ENDERMAN_TELEPORT", "warps", "Warp teleport sound effect", null, null, null),
            def("warps.sounds.teleport.volume", DBL, 0.7, "warps", "Warp teleport sound volume", 0.0, null, null),
            def("warps.sounds.teleport.pitch", DBL, 1.25, "warps", "Warp teleport sound pitch", 0.0, null, null),
            def("warps.sounds.delete.enabled", BOOL, true, "warps", "Warp delete sound enabled", null, null, null),
            def("warps.sounds.delete.sound", STR, "ENTITY_ITEM_BREAK", "warps", "Warp delete sound effect", null, null, null),
            def("warps.sounds.delete.volume", DBL, 0.5, "warps", "Warp delete sound volume", 0.0, null, null),
            def("warps.sounds.delete.pitch", DBL, 1.2, "warps", "Warp delete sound pitch", 0.0, null, null),
            def("warps.sounds.list.enabled", BOOL, true, "warps", "Warp list sound enabled", null, null, null),
            def("warps.sounds.list.sound", STR, "BLOCK_NOTE_BLOCK_CHIME", "warps", "Warp list sound effect", null, null, null),
            def("warps.sounds.list.volume", DBL, 0.5, "warps", "Warp list sound volume", 0.0, null, null),
            def("warps.sounds.list.pitch", DBL, 1.0, "warps", "Warp list sound pitch", 0.0, null, null),
            def("warps.sounds.no-permission.enabled", BOOL, true, "warps", "Warp no-permission sound enabled", null, null, null),
            def("warps.sounds.no-permission.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "warps", "Warp no-permission sound effect", null, null, null),
            def("warps.sounds.no-permission.volume", DBL, 0.5, "warps", "Warp no-permission sound volume", 0.0, null, null),
            def("warps.sounds.no-permission.pitch", DBL, 0.5, "warps", "Warp no-permission sound pitch", 0.0, null, null),
            def("warps.sounds.insufficient-funds.enabled", BOOL, true, "warps", "Warp insufficient funds sound enabled", null, null, null),
            def("warps.sounds.insufficient-funds.sound", STR, "BLOCK_NOTE_BLOCK_BASS", "warps", "Warp insufficient funds sound effect", null, null, null),
            def("warps.sounds.insufficient-funds.volume", DBL, 0.6, "warps", "Warp insufficient funds sound volume", 0.0, null, null),
            def("warps.sounds.insufficient-funds.pitch", DBL, 0.3, "warps", "Warp insufficient funds sound pitch", 0.0, null, null)
        ));
    }

    private static void registerModeration(SettingsRegistry registry) {
        registry.register("moderation", null, List.of(
            def("moderation.messages.no-permission", STR, "&cYou do not have permission to use this moderation command.", "messages", "No permission for moderation command", null, null, null),
            def("moderation.messages.player-not-found", STR, "&cPlayer not found.", "messages", "Player not found", null, null, null),
            def("moderation.messages.player-not-found-or-no-ip", STR, "&cPlayer not found or no IP.", "messages", "Player not found or no IP", null, null, null),
            def("moderation.messages.cannot-punish-self", STR, "&cYou cannot punish yourself.", "messages", "Self-punish prevention message", null, null, null),
            def("moderation.messages.cannot-punish-op", STR, "&cYou cannot punish an operator.", "messages", "Cannot punish operator message", null, null, null),
            def("moderation.commands.ban.usage", STR, "&cUsage: /ban <player> <reason>", "commands", "Ban command usage", null, null, null),
            def("moderation.commands.ban.success", STR, "&aBanned {player}", "commands", "Ban success message", null, null, null),
            def("moderation.commands.tempban.usage", STR, "&cUsage: /tempban <player> <time> <reason>", "commands", "Tempban command usage", null, null, null),
            def("moderation.commands.tempban.success", STR, "&aTemp banned {player}", "commands", "Tempban success message", null, null, null),
            def("moderation.commands.tempban.invalid-duration", STR, "&c{message}", "commands", "Invalid duration message", null, null, null),
            def("moderation.commands.ban-ip.usage", STR, "&cUsage: /ban-ip <player> <reason>", "commands", "Ban-IP command usage", null, null, null),
            def("moderation.commands.ban-ip.success", STR, "&aIP banned {player}", "commands", "Ban-IP success message", null, null, null),
            def("moderation.commands.kick.usage", STR, "&cUsage: /kick <player> <reason>", "commands", "Kick command usage", null, null, null),
            def("moderation.commands.kick.success", STR, "&aKicked {player}", "commands", "Kick success message", null, null, null),
            def("moderation.commands.kick.kick-message", STR, "&cKicked: {reason}", "commands", "Kick message sent to player", null, null, null),
            def("moderation.commands.pardon.usage", STR, "&cUsage: /pardon <player> [reason]", "commands", "Pardon command usage", null, null, null),
            def("moderation.commands.pardon.default-reason", STR, "Pardoned", "commands", "Default pardon reason", null, null, null),
            def("moderation.commands.pardon.success", STR, "&aPardoned {player}", "commands", "Pardon success message", null, null, null),
            def("moderation.commands.warn.usage", STR, "&cUsage: /warn <player> <low|medium|high> <reason>", "commands", "Warn command usage", null, null, null),
            def("moderation.commands.warn.invalid-severity", STR, "&cInvalid severity. Use: low, medium, high", "commands", "Invalid severity message", null, null, null),
            def("moderation.commands.warn.sender-success", STR, "&aWarn added. Total points: {points}", "commands", "Warn success message", null, null, null),
            def("moderation.commands.warn.target-message", LIST, List.of("&cYou have been warned.", "&7Reason: &f{reason}", "&7Severity: &f{severity}", "&7Total points: &f{points}"), "commands", "Warn target message", null, null, null),
            def("moderation.commands.pid.usage", STR, "&cUsage: /pid [page] or /pid <view|pardon> <id>", "commands", "PID command usage", null, null, null),
            def("moderation.commands.pid.pardon-usage", STR, "&cUsage: /pid pardon <id> <reason>", "commands", "PID pardon usage", null, null, null),
            def("moderation.commands.pid.invalid-page", STR, "&cInvalid page number.", "commands", "Invalid page message", null, null, null),
            def("moderation.commands.pid.no-punishment-found", STR, "&cNo punishment found.", "commands", "No punishment found", null, null, null),
            def("moderation.commands.pid.unknown-subcommand", STR, "&cUnknown subcommand.", "commands", "Unknown PID subcommand", null, null, null),
            def("moderation.commands.pid.recent-title", STR, "&6Recent Punishments: &7(Page {page})", "commands", "Recent punishments title", null, null, null),
            def("moderation.commands.pid.recent-entry", STR, "&e#{id} &7- &f{player} &7- &6{type} &7- {status}", "commands", "Recent punishment entry format", null, null, null),
            def("moderation.commands.pid.pardon-success", STR, "&aPunishment {id} pardoned.", "commands", "PID pardon success", null, null, null),
            def("moderation.commands.pid.status-active", STR, "&aACTIVE", "commands", "Active punishment status", null, null, null),
            def("moderation.commands.pid.status-pardoned", STR, "&cPARDONED", "commands", "Pardoned punishment status", null, null, null),
            def("moderation.commands.pid.view.title", STR, "&6Punishment #{id}", "commands", "Punishment view title", null, null, null),
            def("moderation.commands.pid.view.type", STR, "&eType: &f{type}", "commands", "Punishment type label", null, null, null),
            def("moderation.commands.pid.view.player", STR, "&ePlayer: &f{player}", "commands", "Punishment player label", null, null, null),
            def("moderation.commands.pid.view.moderator", STR, "&eModerator: &f{moderator}", "commands", "Punishment moderator label", null, null, null),
            def("moderation.commands.pid.view.reason", STR, "&eReason: &f{reason}", "commands", "Punishment reason label", null, null, null),
            def("moderation.commands.pid.view.ip", STR, "&eIP: &f{ip}", "commands", "Punishment IP label", null, null, null),
            def("moderation.commands.pid.view.status", STR, "&eStatus: &f{status}", "commands", "Punishment status label", null, null, null),
            def("moderation.commands.pid.view.remaining", STR, "&eRemaining: &f{remaining}", "commands", "Punishment remaining time label", null, null, null),
            def("moderation.commands.pid.view.duration", STR, "&eDuration: &f{duration}", "commands", "Punishment duration label", null, null, null),
            def("moderation.commands.pid.view.pardon-reason", STR, "&ePardon Reason: &f{pardonReason}", "commands", "Punishment pardon reason label", null, null, null),
            def("moderation.commands.pid.view.pardoned-by", STR, "&ePardoned By: &f{pardonedBy}", "commands", "Punishment pardoned-by label", null, null, null),
            def("moderation.warn-points.low", INT, 1, "warnings", "Warn points for low severity", 0, null, null),
            def("moderation.warn-points.medium", INT, 2, "warnings", "Warn points for medium severity", 0, null, null),
            def("moderation.warn-points.high", INT, 3, "warnings", "Warn points for high severity", 0, null, null),
            def("moderation.warn-decay.amount", INT, 3, "warnings", "Points decayed per interval", 0, null, null),
            def("moderation.warn-decay.interval", STR, "1mo", "warnings", "Warn decay interval (e.g. 7d, 1mo)", null, null, null),
            def("moderation.auto-punishments.tempban-points", INT, 3, "auto-punishments", "Warning points threshold for auto tempban", 0, null, null),
            def("moderation.auto-punishments.tempban-duration", STR, "1d", "auto-punishments", "Auto tempban duration", null, null, null),
            def("moderation.auto-punishments.tempban-reason", STR, "Too many warnings", "auto-punishments", "Auto tempban reason", null, null, null),
            def("moderation.auto-punishments.ban-points", INT, 6, "auto-punishments", "Warning points threshold for auto ban", 0, null, null),
            def("moderation.auto-punishments.ban-reason", STR, "Too many warnings", "auto-punishments", "Auto permanent ban reason", null, null, null),
            def("moderation.punishment-history.enabled", BOOL, true, "punishment-history", "Enable punishment history retention", null, null, null),
            def("moderation.punishment-history.retention-days", INT, 90, "punishment-history", "Days to keep punishment records", 1, null, null),
            def("moderation.ban-screen.disconnect", STR, "You are banned.", "ban-screen", "Ban screen disconnect message", null, null, null),
            def("moderation.ban-screen.reason-label", STR, "Reason", "ban-screen", "Ban screen reason label", null, null, null),
            def("moderation.ban-screen.moderator-label", STR, "Moderator", "ban-screen", "Ban screen moderator label", null, null, null),
            def("moderation.ban-screen.ban-id-label", STR, "Ban ID", "ban-screen", "Ban screen ID label", null, null, null),
            def("moderation.ban-screen.expires-label", STR, "Expires", "ban-screen", "Ban screen expires label", null, null, null),
            def("moderation.ban-screen.remaining-label", STR, "Remaining", "ban-screen", "Ban screen remaining label", null, null, null),
            def("moderation.ban-screen.duration-label", STR, "Duration", "ban-screen", "Ban screen duration label", null, null, null),
            def("moderation.ban-screen.permanent-text", STR, "Permanent", "ban-screen", "Ban screen permanent text", null, null, null),
            def("moderation.ban-screen.appeal-label", STR, "Appeal", "ban-screen", "Ban screen appeal label", null, null, null),
            def("moderation.ban-screen.appeal-text", STR, "Contact server staff", "ban-screen", "Ban screen appeal text", null, null, null),
            def("moderation.bedrock-ban-screen.disconnect", STR, "You are banned.", "ban-screen", "Bedrock ban screen disconnect message", null, null, null),
            def("moderation.bedrock-ban-screen.reason-label", STR, "Reason", "ban-screen", "Bedrock ban screen reason label", null, null, null),
            def("moderation.bedrock-ban-screen.id-label", STR, "ID", "ban-screen", "Bedrock ban screen ID label", null, null, null),
            def("moderation.sounds.warn.enabled", BOOL, true, "sounds", "Warn sound enabled", null, null, null),
            def("moderation.sounds.warn.sound", STR, "BLOCK_NOTE_BLOCK_CHIME", "sounds", "Warn sound effect", null, null, null),
            def("moderation.sounds.warn.volume", DBL, 0.8, "sounds", "Warn sound volume", 0.0, null, null),
            def("moderation.sounds.warn.pitch", DBL, 1.15, "sounds", "Warn sound pitch", 0.0, null, null),
            def("moderation.sounds.ban.enabled", BOOL, true, "sounds", "Ban sound enabled", null, null, null),
            def("moderation.sounds.ban.sound", STR, "ENTITY_WITHER_DEATH", "sounds", "Ban sound effect", null, null, null),
            def("moderation.sounds.ban.volume", DBL, 0.7, "sounds", "Ban sound volume", 0.0, null, null),
            def("moderation.sounds.ban.pitch", DBL, 1.4, "sounds", "Ban sound pitch", 0.0, null, null),
            def("moderation.sounds.tempban.enabled", BOOL, true, "sounds", "Tempban sound enabled", null, null, null),
            def("moderation.sounds.tempban.sound", STR, "ENTITY_LIGHTNING_BOLT_THUNDER", "sounds", "Tempban sound effect", null, null, null),
            def("moderation.sounds.tempban.volume", DBL, 0.6, "sounds", "Tempban sound volume", 0.0, null, null),
            def("moderation.sounds.tempban.pitch", DBL, 1.5, "sounds", "Tempban sound pitch", 0.0, null, null),
            def("moderation.jail.teleport-delay-seconds", INT, 3, "jail", "Jail teleport delay in seconds", 0, null, null),
            def("moderation.jail.release-on-logout", BOOL, true, "jail", "Release jailed players on logout", null, null, null),
            def("moderation.jail.messages.jailed", STR, "&cYou have been jailed in &f{jail}&c.", "jail", "Jail message", null, null, null),
            def("moderation.jail.messages.released", STR, "&aYou have been released from jail.", "jail", "Jail release message", null, null, null),
            def("moderation.jail.messages.already-jailed", STR, "&c{player} is already jailed.", "jail", "Already jailed message", null, null, null),
            def("moderation.jail.messages.not-jailed", STR, "&c{player} is not jailed.", "jail", "Not jailed message", null, null, null),
            def("moderation.jail.messages.no-jail-exists", STR, "&cJail &f{jail} &cdoes not exist.", "jail", "No jail exists message", null, null, null),
            def("moderation.jail.messages.jail-set", STR, "&aJail &f{jail} &aset.", "jail", "Jail set message", null, null, null),
            def("moderation.jail.messages.jail-deleted", STR, "&cJail &f{jail} &cdeleted.", "jail", "Jail deleted message", null, null, null),
            def("moderation.jail.messages.jail-list-header", STR, "&6Jails:", "jail", "Jail list header", null, null, null),
            def("moderation.jail.messages.jail-list-entry", STR, "&8- &f{name}", "jail", "Jail list entry format", null, null, null),
            def("moderation.jail.messages.jails-none", STR, "&cNo jails exist.", "jail", "No jails message", null, null, null),
            def("moderation.jail.messages.usage-jail", STR, "&cUsage: /jail <name> [player]", "jail", "Jail command usage", null, null, null),
            def("moderation.jail.messages.usage-setjail", STR, "&cUsage: /setjail <name>", "jail", "Setjail command usage", null, null, null),
            def("moderation.jail.messages.usage-deljail", STR, "&cUsage: /deljail <name>", "jail", "Deljail command usage", null, null, null),
            def("moderation.jail.sounds.jail.enabled", BOOL, true, "jail", "Jail sound enabled", null, null, null),
            def("moderation.jail.sounds.jail.sound", STR, "BLOCK_IRON_DOOR_CLOSE", "jail", "Jail sound effect", null, null, null),
            def("moderation.jail.sounds.jail.volume", DBL, 0.8, "jail", "Jail sound volume", 0.0, null, null),
            def("moderation.jail.sounds.jail.pitch", DBL, 1.0, "jail", "Jail sound pitch", 0.0, null, null),
            def("moderation.jail.sounds.unjail.enabled", BOOL, true, "jail", "Unjail sound enabled", null, null, null),
            def("moderation.jail.sounds.unjail.sound", STR, "BLOCK_IRON_DOOR_OPEN", "jail", "Unjail sound effect", null, null, null),
            def("moderation.jail.sounds.unjail.volume", DBL, 0.8, "jail", "Unjail sound volume", 0.0, null, null),
            def("moderation.jail.sounds.unjail.pitch", DBL, 1.0, "jail", "Unjail sound pitch", 0.0, null, null),
            def("moderation.jail.sounds.set.enabled", BOOL, true, "jail", "Jail set sound enabled", null, null, null),
            def("moderation.jail.sounds.set.sound", STR, "ENTITY_PLAYER_LEVELUP", "jail", "Jail set sound effect", null, null, null),
            def("moderation.jail.sounds.set.volume", DBL, 0.7, "jail", "Jail set sound volume", 0.0, null, null),
            def("moderation.jail.sounds.set.pitch", DBL, 1.2, "jail", "Jail set sound pitch", 0.0, null, null),
            def("moderation.jail.sounds.delete.enabled", BOOL, true, "jail", "Jail delete sound enabled", null, null, null),
            def("moderation.jail.sounds.delete.sound", STR, "ENTITY_ITEM_BREAK", "jail", "Jail delete sound effect", null, null, null),
            def("moderation.jail.sounds.delete.volume", DBL, 0.6, "jail", "Jail delete sound volume", 0.0, null, null),
            def("moderation.jail.sounds.delete.pitch", DBL, 0.8, "jail", "Jail delete sound pitch", 0.0, null, null)
        ));
    }

    private static void registerSecurity(SettingsRegistry registry) {
        registry.register("security", null, List.of(
            def("plugin-hide", BOOL, false, "general", "Hide CoreTuff from /pl", null, null, null),
            def("messages.hidden-command", STR, "&cUnknown command.", "messages", "Message for hidden commands", null, null, null)
        ));
    }

    private static SettingDefinition def(String key, SettingType type, Object defaultValue, String category, String description, Number min, Number max, List<String> options) {
        return new SettingDefinition(key, type, defaultValue, min != null ? min.doubleValue() : null, max != null ? max.doubleValue() : null, description, options, category);
    }
}
