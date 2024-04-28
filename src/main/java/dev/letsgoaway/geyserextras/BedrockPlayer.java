package dev.letsgoaway.geyserextras;

import dev.letsgoaway.geyserextras.menus.MainMenu;
import dev.letsgoaway.geyserextras.menus.OptionalPacks;
import dev.letsgoaway.geyserextras.menus.QuickMenuSetup;
import dev.letsgoaway.geyserextras.menus.TabList;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class BedrockPlayer {
    public Player player;
    public static Random random = new Random();
    public boolean lookingAtEntity = false;
    private boolean blockLeftClickAir = true;
    private boolean dontUnblockNextLeftClickAir = false;

    public float coolDownThresHold = 0;
    public int reach = 3;
    public String cooldownType = "Crosshair";
    public static List<String> cooldownTypes = Arrays.asList("Crosshair", "Hotbar", "None");
    public boolean enableSneakDropOffhand = false;
    public boolean spaceHotbar = false;
    public int lastCooldown = 0;
    public String lastCooldownText = "";
    public BukkitTask hotbarTask;
    public List<String> quickMenuList = new ArrayList<>(Arrays.asList("", "", "", ""));
    public List<String> quickMenuActions = new ArrayList<>(Arrays.asList("None", "None", "None", "None"));
    public List<String> optionalPacks = new ArrayList<>();

    public boolean enableArrowDelayFix = false;
    public PlayerDevice device;
    public PlayerInputType inputType;
    public PlayerPlatform platform;
    public PlayerUIProfile uiProfile;
    public String bedrockUsername;

    public String xuid;

    BedrockPlayer(Player player) {
        this.player = player;
        this.bedrockUsername = GeyserExtras.bedrockAPI.getXboxUsername(this);
        this.device = PlayerDevice.getPlayerDevice(this);
        this.inputType = PlayerInputType.getPlayerInputType(this);
        this.platform = PlayerPlatform.getPlayerPlatform(this);
        this.uiProfile = PlayerUIProfile.getPlayerUIProfile(this);
        this.xuid = GeyserExtras.bedrockAPI.getPlayerXUID(this);
        if (this.hasData("cooldown") && cooldownTypes.contains(getData("cooldown", PersistentDataType.STRING))) {
            cooldownType = getData("cooldown", PersistentDataType.STRING);
        }
        if (this.hasData("sneakdropoffhand")) {
            enableSneakDropOffhand = getData("sneakdropoffhand", PersistentDataType.BOOLEAN);
        }
        if (this.hasData("quickmenuemotes")) {
            quickMenuList = new ArrayList<>(getData("quickmenuemotes", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)));
        }
        if (this.hasData("quickmenuactions")) {
            quickMenuActions = new ArrayList<>(getData("quickmenuactions", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)));
            quickMenuActions.replaceAll(s -> {
                if (!Config.quickMenuCommands.containsKey(s)) {
                    return "None";
                } else {
                    return s;
                }
            });
        }
        if (this.hasData("optionalpacks")) {
            optionalPacks = new ArrayList<>(getData("optionalpacks", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)));
        }
        optionalPacks.removeIf((s) -> !GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackExists(s));
        if (this.hasData("arrowdelayfix")) {
            enableArrowDelayFix = getData("arrowdelayfix", PersistentDataType.BOOLEAN);
        }
        this.save();
        if (!OptionalPacks.loadingResourcePacks.containsKey(GeyserExtras.bedrockAPI.getPlayerXUID(this)) && !this.optionalPacks.isEmpty()) {
            OptionalPacks.loadingResourcePacks.put(GeyserExtras.bedrockAPI.getPlayerXUID(this), this.optionalPacks.toArray(String[]::new));
            this.save();
            GeyserExtras.bedrockAPI.reconnect(player.getUniqueId());
            Tick.runIn(3L, () -> {
                GeyserExtras.bedrockAPI.reconnect(player.getUniqueId());
            });
        }
        Tick.runOnNext(() -> {
            TabList.precacheSkin(player);
        });
    }


    public PersistentDataContainer playerSaveData() {
        return player.getPersistentDataContainer();
    }

    public boolean hasData(String key) {
        try {
            return playerSaveData().has(NamespacedKey.fromString(key, GeyserExtras.plugin));
        } catch (Exception ignored) {
            return false;
        }
    }

    public <P, C> void setData(String key, PersistentDataType<P, C> type, C value) {
        playerSaveData().set(NamespacedKey.fromString(key, GeyserExtras.plugin), type, value);
    }


    public <P, C> C getData(String key, PersistentDataType<P, C> type) {
        return playerSaveData().get(NamespacedKey.fromString(key, GeyserExtras.plugin), type);
    }

    public void setCooldownType(String cooldownType) {
        if (cooldownType.equals("none")) {
            player.resetTitle();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("\uE0AF"));
        }
        this.cooldownType = cooldownType;
        setData("cooldown", PersistentDataType.STRING, cooldownType);
    }

    public void setEnableSneakDropOffhand(Boolean b) {
        this.enableSneakDropOffhand = b;
        setData("sneakdropoffhand", PersistentDataType.BOOLEAN, b);
    }

    public void setEnableArrowDelayFix(Boolean b) {
        this.enableArrowDelayFix = b;
        setData("arrowdelayfix", PersistentDataType.BOOLEAN, b);
    }

    public void save() {
        setData("quickmenuemotes", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING), quickMenuList);
        setData("quickmenuactions", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING), quickMenuActions);
        setData("optionalpacks", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING), List.of(""));
        if (!optionalPacks.isEmpty()) {
            setData("optionalpacks", PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING), optionalPacks);
        }
        setEnableSneakDropOffhand(this.enableSneakDropOffhand);
        setCooldownType(this.cooldownType);
        setEnableArrowDelayFix(this.enableArrowDelayFix);
    }

    public void update() {
        if (enableArrowDelayFix) {
            for (Entity entity : player.getWorld().getEntitiesByClasses(Arrow.class)) {
                if (128.0 >= entity.getLocation().distance(player.getLocation()) && !entity.equals(player)) {
                    player.hideEntity(GeyserExtras.plugin, entity);
                    player.showEntity(GeyserExtras.plugin, entity);
                }
            }
        }
        if (!Objects.requireNonNull(player.getAddress()).getHostString().equals("127.0.0.1")) {
            calculateAveragePing();
        }
        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            reach = 5;
        } else {
            reach = 3;
        }
        if (Config.customCoolDownEnabled && !cooldownType.equals("None")) {
            if (cooldownType.equals("Crosshair")) {
                checkLookingAtEntity();
            }
            CooldownHandler.updateForPlayer(this);
        }
        if (Config.netherRoofEnabled) {
            updateNetherFog();
        }
        if (Config.blockGhostingFix) {
            Block targetBlock = player.getTargetBlockExact(5, FluidCollisionMode.ALWAYS);
            if (targetBlock != null) {
                player.sendBlockChange(targetBlock.getLocation(), targetBlock.getBlockData());
            }
        }
        if (player.getPlayerProfile().getTextures().getSkin() == null) {
            try {
                player.getPlayerProfile().getTextures().setSkin(new URL(TabList.getSkinURL(player)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String lastFog = "minecraft:fog_hell";

    public void updateNetherFog() {

    }

    public String getNetherFogID() {
        switch (player.getLocation().getBlock().getBiome()) {
            case NETHER_WASTES -> {
                return "minecraft:fog_hell";
            }
            case SOUL_SAND_VALLEY -> {
                return "minecraft:fog_soulsand_valley";
            }
            case CRIMSON_FOREST -> {
                return "minecraft:fog_crimson_forest";
            }
            case WARPED_FOREST -> {
                return "minecraft:fog_warped_forest";
            }
            case BASALT_DELTAS -> {
                return "minecraft:fog_basalt_deltas";
            }
            default -> {
                return "minecraft:fog_hell";
            }
        }
    }

    public void checkLookingAtEntity() {
        Entity lookingEntity = getTargetEntity(player);
        lookingAtEntity = lookingEntity != null;
    }

    public Entity getTargetEntity(Player player) {
        Location loc = player.getEyeLocation();
        RayTraceResult entityCast = player.getWorld().rayTraceEntities(loc, loc.getDirection(), reach, entity -> {
            if (entity instanceof Player player1) {
                return player1.getUniqueId() != player.getUniqueId();
            } else if (entity.isDead()) {
                return false;
            } else {
                return entity.getType().isAlive();
            }
        });
        if (entityCast == null) {
            return null;
        }

        RayTraceResult blockCast = player.getWorld().rayTraceBlocks(loc, loc.getDirection(), reach);

        if (blockCast == null) {
            return entityCast.getHitEntity();
        }
        if (entityCast.getHitEntity() == null) {
            return null;
        }
        if (blockCast.getHitBlock() == null) {
            return entityCast.getHitEntity();
        }
        if (entityCast.getHitPosition().toLocation(player.getWorld()).distance(loc)
                >= blockCast.getHitPosition().toLocation(player.getWorld()).distance(loc)
                && blockCast.getHitBlock().getType() != Material.AIR) {
            return null;
        }
        return entityCast.getHitEntity();
    }

    public boolean isTool() {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType().isItem() && (
                item.getTranslationKey().contains("_axe")
                        || item.getTranslationKey().contains("_pickaxe")
                        || item.getTranslationKey().contains("_shovel")
                        || item.getTranslationKey().contains("_sword")
                // || item.getTranslationKey().contains("_hoe")
                // hoes dont have attack speed for some reason
        );
    }

    public boolean isArmor() {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType().isItem() && (
                item.getTranslationKey().contains("_helmet")
                        || item.getTranslationKey().contains("_chestplate")
                        || item.getTranslationKey().contains("_leggings")
                        || item.getTranslationKey().contains("_boots")
                // || item.getTranslationKey().contains("_hoe")
                // hoes dont have attack speed for some reason
        );
    }

    public void onGeyserExtrasCommand() {
        // yucky formatting!
        new MainMenu(this);
        /*
        CustomForm form = CustomForm.builder()
                .title("GeyserExtras")
                .dropdown("Attack Indicator", Arrays.asList("Crosshair", "Hotbar", "None"), cooldownTypes.indexOf(cooldownType)) // 0
                .validResultHandler((response) -> setCooldownType(cooldownTypes.get(response.asDropdown(0))))
                .build();
        GeyserExtras.bedrockAPI.sendForm(player.getUniqueId(), form);
        */
    }


    public void onPlayerBlockDamage(BlockDamageEvent ev) {
        coolDownThresHold = 1.0f;
        if (cropTypes.contains(ev.getBlock().getType())) {
            EventListener.cropStop(this);
        }
        blockLeftClickAir = true;
    }

    private Boolean alsoSentBreak = false;

    public void onPlayerBlockDamageAbort(BlockDamageAbortEvent ev) {
        if (!alsoSentBreak) {
            coolDownThresHold = 0.0f;
        } else {
            coolDownThresHold = 1.0f;
            alsoSentBreak = false;
        }
        blockLeftClickAir = true;
        dontUnblockNextLeftClickAir = true;
        if (cropTypes.contains(ev.getBlock().getType())) {
            EventListener.cropStop(this);
        }
    }

    public static final List<Material> noCoolDownOnRClick = Arrays.asList(
            Material.FIREWORK_ROCKET,
            Material.LEGACY_FIREWORK,
            Material.BOW,
            Material.CROSSBOW,
            Material.ENDER_PEARL,
            Material.SNOWBALL,
            Material.ENDER_EYE,
            Material.SPLASH_POTION,
            Material.EXPERIENCE_BOTTLE,
            Material.LEGACY_EXP_BOTTLE,
            Material.ELYTRA,
            Material.TURTLE_HELMET
    );

    public void onPlayerInteract(PlayerInteractEvent ev) {
        player.resetTitle();
        //player.sendMessage(ev.getAction().toString());
        if (ev.getAction().equals(Action.LEFT_CLICK_AIR)) {
            if (!blockLeftClickAir) {
                coolDownThresHold = 0.0f;
            } else {
                if (!dontUnblockNextLeftClickAir) {
                    blockLeftClickAir = false;
                }

            }
        } else if (ev.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            if (noCoolDownOnRClick.contains(player.getInventory().getItemInMainHand().getType()) || isArmor()) {
                coolDownThresHold = 1.0f;
                blockLeftClickAir = true;
                dontUnblockNextLeftClickAir = false;
            } else {
                coolDownThresHold = 0.0f;
            }

        } else if (ev.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            coolDownThresHold = 1.0f;
            blockLeftClickAir = true;
            Tick.runIn(2L, () -> {
                blockLeftClickAir = false;
            });
            dontUnblockNextLeftClickAir = false;
        }

    }

    public static final List<EntityType> noCoolDownOnEntityRClick = Arrays.asList(
            EntityType.ITEM_FRAME,
            EntityType.GLOW_ITEM_FRAME,
            EntityType.VILLAGER,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ARMOR_STAND,
            EntityType.MINECART_CHEST,
            EntityType.MINECART,
            EntityType.BOAT,
            EntityType.CHEST_BOAT,
            EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE,
            EntityType.HORSE,
            EntityType.PIG,
            EntityType.LLAMA,
            EntityType.CAMEL,
            EntityType.MINECART_COMMAND,
            EntityType.MINECART_CHEST,
            EntityType.MINECART_FURNACE,
            EntityType.MINECART_HOPPER,
            EntityType.MINECART_MOB_SPAWNER,
            EntityType.MINECART_TNT
    );

    public void onPlayerInteractEntity(PlayerInteractEntityEvent ev) {
        if (noCoolDownOnEntityRClick.contains(ev.getRightClicked().getType())) {
            coolDownThresHold = 1.0f;
            blockLeftClickAir = true; // have to do here because for some reason on next tick left_click_air is sent. why? idfk!
            dontUnblockNextLeftClickAir = false;
        }
    }

    private String parsePlaceholders(String s) {
        return s.replace("%player_name%", player.getName())
                .replace("%xbox_username%", bedrockUsername)
                .replace("%player_device%", device.displayName)
                .replace("%player_platform%", platform.displayName)
                .replace("%player_inputtype%", inputType.displayName)
                .replace("%player_uiprofile%", uiProfile.displayName);
    }


    private int lastPing = 0;

    private final List<Integer> pingValues = new ArrayList<>();

    public float averagePing = 0.0f;

    private void calculateAveragePing() {
        int ping = player.getPing();
        if (lastPing != ping) {
            pingValues.add(ping);
            if (pingValues.size() > 10) {
                pingValues.remove(0);
            }
            int total = 0;
            for (int pingVal : pingValues) {
                total += pingVal;
            }
            averagePing = (float) total / pingValues.size();
            lastPing = ping;
        }
    }

    // is on ground works good enough and bedrock players are unlikely to spoof.
    public void onPlayerDamageEntity(EntityDamageByEntityEvent ev) {
        coolDownThresHold = 0.0f;
        if (isTool()) {
            player.setCooldown(player.getInventory().getItemInMainHand().getType(), Math.round(CombatData.getCooldownPeriod(player)));
        }
    }

    private BukkitTask spaceTask;

    public void onPlayerItemHeldSwitch(PlayerItemHeldEvent ev) {
        coolDownThresHold = 0.0f;
        float textTime = getTextTime();
        ItemStack heldItem = ev.getPlayer().getInventory().getItem(ev.getNewSlot());
        if ((heldItem == null || heldItem.getType().equals(Material.AIR)) && spaceTask != null && spaceTask.isCancelled()) {
            spaceHotbar = false;
        } else {
            spaceHotbar = true;
            if (spaceTask != null) {
                spaceTask.cancel();
            }
            spaceTask = Tick.runIn(Tick.secondsToTicksRounded(textTime), () -> {
                spaceHotbar = false;
                spaceTask.cancel();
            });
            // all of this stuff is to make sure the cooldown doesnt overlap the item text.
            // average ping is calculated to make sure it goes back down some what on time instead of looking delayed and bad
        }
    }

    private float getTextTime() {
        float textTime = 2.5f; // 2.5 seconds is how long the text fade out is
        for (Enchantment enchantment : player.getInventory().getItemInMainHand().getEnchantments().keySet()) {
            // java only so it doesnt show on bedrock ui
            if (!enchantment.equals(Enchantment.SWEEPING_EDGE)) {
                textTime += .75f; // + .75 seconds is added on the bedrock client
                // for each enchantment so you have time to read it
            }
        }
        if (lastPing >= 40) {
            if (textTime - (averagePing / 1000) > 0.0f) {
                textTime -= (averagePing / 1000);
            }
        }
        return textTime;
    }

    public static final List<Material> cropTypes = Arrays.asList(
            Material.WHEAT,
            Material.WHEAT_SEEDS,
            Material.CARROT,
            Material.CARROTS,
            Material.BEETROOT,
            Material.BEETROOT_SEEDS,
            Material.BEETROOTS,
            Material.POTATO,
            Material.POTATOES,
            Material.POISONOUS_POTATO,
            Material.BAKED_POTATO,
            Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS
    );

    public void onPlayerBlockPlace(BlockPlaceEvent ev) {
        coolDownThresHold = 1.0f;
        dontUnblockNextLeftClickAir = false;
        if (Config.javaBlockPlacement) {
            Block blockTrace = player.getTargetBlockExact(reach);

            if (blockTrace != null) {
                ev.setCancelled(!ev.getBlockPlaced().equals(Objects.requireNonNull(player.rayTraceBlocks(reach)).getHitBlock()));
            }
            if (ev.getBlockPlaced().getType().equals(Material.AIR) || ev.getBlockAgainst().getType().equals(Material.AIR)) {
                ev.setCancelled(true);
            }
            if (ev.isCancelled()) {
                ev.getBlockPlaced().setType(Material.AIR);
                player.teleport(player.getLocation());
            }
        }
        player.resetTitle();
    }

    public void onPlayerBlockBreak(BlockBreakEvent ev) {
        coolDownThresHold = 1.0f;
        alsoSentBreak = true;
        blockLeftClickAir = false;
        player.resetTitle();
    }

    public boolean waitingForEmote = false;
    public int waitingEmoteID = 0;

    private static final List<String> emoteWheelUnicodes = Arrays.asList("\uF840", "\uF841", "\uF842", "\uF843");

    public void setWaiting(int waitingEmoteID) {
        int emoteNumber = waitingEmoteID + 1;
        player.sendMessage("Please play the emote for Emote #" + emoteNumber + " (" + emoteWheelUnicodes.get(waitingEmoteID) + ").");
        this.waitingEmoteID = waitingEmoteID;
        waitingForEmote = true;
    }

    private void runCommand(String string) {
        if (Config.proxyMode) {
            player.performCommand(string);
        } else {
            player.performCommand(string);
        }
    }

    public boolean onPlayerEmoteEvent(String emoteUUID) {
        boolean cancelled = false;
        if (waitingForEmote) {
            cancelled = true;
            waitingForEmote = false;
            quickMenuList.set(waitingEmoteID, emoteUUID);
            new QuickMenuSetup(this).show(this);
            this.save();
        } else {
            if (quickMenuList.contains(emoteUUID)) {
                String action = quickMenuActions.get(quickMenuList.indexOf(emoteUUID));
                if (!action.equals("None")) {
                    cancelled = true;
                    Tick.runOnNext(() -> {
                        runCommand(parsePlaceholders(Config.quickMenuCommands.getOrDefault(action, "")));
                    });
                }
            }
        }
        if (cancelled) {
            Tick.runOnNext(() -> {
                player.setSneaking(true);
            });
        }
        return cancelled;
    }

    private void swapOffhand() {
        runCommand("geyser offhand");
    }

    public void onPlayerDrop(PlayerDropItemEvent ev) {
        coolDownThresHold = 1.0f;
        blockLeftClickAir = true; // have to do here because for some reason on next tick left_click_air is sent. why? idfk!
        dontUnblockNextLeftClickAir = false;
        if (enableSneakDropOffhand && player.isSneaking()) {
            Tick.runOnNext(this::swapOffhand);
            ev.setCancelled(true);
        }
    }

    public boolean hasPack(Path path) {
        if (GeyserExtras.bedrockAPI.apiInstances.containsKey(APIType.GEYSER)) {
            UUID packID = GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackID(path);
            return optionalPacks.contains(packID.toString());
        }
        return false;
    }

    public void addPack(Path path) {
        UUID packID = GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackID(path);
        if (!optionalPacks.contains(packID.toString())) {
            optionalPacks.add(0, packID.toString());
        }
    }

    public void removePack(Path path) {
        UUID packID = GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackID(path);
        optionalPacks.remove(packID.toString());
    }

    public void movePackUp(Path path) {
        UUID packID = GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackID(path);
        int lastPos = optionalPacks.indexOf(packID.toString());
        optionalPacks.remove(lastPos);
        if (lastPos - 1 == -1) {
            optionalPacks.add(0, packID.toString());
            return;
        }
        optionalPacks.add(lastPos - 1, packID.toString());
    }

    public void movePackDown(Path path) {
        UUID packID = GeyserExtras.bedrockAPI.apiInstances.get(APIType.GEYSER).getPackID(path);
        int lastPos = optionalPacks.indexOf(packID.toString());
        optionalPacks.remove(lastPos);
        optionalPacks.add(lastPos + 1, packID.toString());
    }

    public void onPlayerMoveEvent(PlayerMoveEvent ev) {

    }

    public void onPlayerLeave(PlayerQuitEvent ev) {
        TabList.bedrockPlayerTextureIDs.remove(xuid);
    }
}