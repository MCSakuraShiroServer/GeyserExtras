package dev.letsgoaway.geyserextras.api;

import dev.letsgoaway.geyserextras.*;
import dev.letsgoaway.geyserextras.menus.OptionalPacks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Filter;

public class GeyserBedrockAPI extends dev.letsgoaway.geyserextras.api.BedrockPluginAPI implements org.geysermc.geyser.api.event.EventRegistrar {
    private final org.geysermc.geyser.api.GeyserApi api = org.geysermc.geyser.api.GeyserApi.api();
    private final HashMap<UUID, ResourcePack> resourcePackHashMap = new HashMap<>();
    private final HashMap<UUID, Path> resourcePackPathMap = new HashMap<>();

    public GeyserBedrockAPI() {
        super();
        tryRegisterEventBus();
    }

    @Override
    public void onConfigLoad() {
        GeyserExtras.logger.info("Loading optional packs...");
        loadResources();
        GeyserExtras.logger.info("Optional packs loaded!");
    }

    private void loadResources() {
        Plugin geyserSpigot = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyserSpigot == null) {
            return;
        }
        /* geyser has an annoying message where it says that paths are too long,
        so i disable the logger for it temporarily here */
        Filter oldFilter = geyserSpigot.getLogger().getFilter();
        geyserSpigot.getLogger().setFilter(record -> false);
        for (File rp : Config.packsArray) {
            ResourcePack resourcePack = ResourcePack.create(PackCodec.path(rp.toPath()));
            resourcePackHashMap.put(resourcePack.manifest().header().uuid(), resourcePack);
            resourcePackPathMap.put(resourcePack.manifest().header().uuid(), rp.toPath());
            GeyserExtras.logger.info("Pack '"+resourcePack.manifest().header().name()+"' loaded succesfully!");
        }
        /* and reenable it here */
        geyserSpigot.getLogger().setFilter(oldFilter);
    }

    private void tryRegisterEventBus() {
        api.eventBus().subscribe(this, org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent.class, this::onClientEmoteEvent);
        api.eventBus().subscribe(this, org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent.class, this::onResourcePackLoadEvent);
        api.eventBus().subscribe(this, org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent.class, (ev) -> {
            if ((ev.disconnectReason().equals("disconnectionScreen.resourcePack")) || (ev.disconnectReason().equals("Bedrock client disconnected") && ev.connection().javaUuid() == null)) {
                OptionalPacks.loadingResourcePacks.remove(ev.connection().xuid());
            }
        });
    }

    @Subscribe
    public void onClientEmoteEvent(org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent ev) {
        ev.setCancelled(GeyserExtras.bplayers.get(ev.connection().javaUuid()).onPlayerEmoteEvent(ev.emoteId()));
    }

    @Subscribe
    public void onResourcePackLoadEvent(org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent ev) {
        if (OptionalPacks.loadingResourcePacks.containsKey(ev.connection().xuid())) {
            for (String id : OptionalPacks.loadingResourcePacks.get(ev.connection().xuid())) {
                ev.register(resourcePackHashMap.get(UUID.fromString(id)));
            }
            // OptionalPacks.loadingResourcePacks.remove(ev.connection().xuid());
        }
    }

    public void reconnect(UUID uuid) {
        api.transfer(uuid, Config.externalAddress, Config.externalPort);
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return api.isBedrockPlayer(uuid);
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return api.sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(UUID uuid, FormBuilder<?, ?, ?> form) {
        return this.sendForm(uuid, form.build());
    }

    @Override
    public PlayerDevice getPlayerDevice(UUID uuid) {
        return switch (Objects.requireNonNull(api.connectionByUuid(uuid)).platform()) {
            case GOOGLE -> PlayerDevice.ANDROID;
            case IOS -> PlayerDevice.IOS;
            case OSX -> PlayerDevice.OSX;
            case AMAZON -> PlayerDevice.AMAZON;
            case GEARVR -> PlayerDevice.GEARVR;
            case HOLOLENS -> PlayerDevice.HOLOLENS;
            case UWP, WIN32 -> PlayerDevice.WINDOWS;
            case DEDICATED -> PlayerDevice.DEDICATED;
            case TVOS -> PlayerDevice.TVOS;
            case PS4 -> PlayerDevice.PLAYSTATION;
            case NX -> PlayerDevice.SWITCH;
            case XBOX -> PlayerDevice.XBOX;
            case WINDOWS_PHONE -> PlayerDevice.WINDOWS_PHONE;
            default -> PlayerDevice.UNKNOWN;
        };
    }

    @Override
    public PlayerInputType getPlayerInputType(UUID uuid) {
        return switch (Objects.requireNonNull(api.connectionByUuid(uuid)).inputMode()) {
            case CONTROLLER -> PlayerInputType.CONTROLLER;
            case TOUCH -> PlayerInputType.TOUCH;
            case KEYBOARD_MOUSE -> PlayerInputType.KEYBOARD_MOUSE;
            case VR -> PlayerInputType.VR;
            default -> PlayerInputType.UNKNOWN;
        };
    }

    @Override
    public PlayerUIProfile getPlayerUIProfile(UUID uuid) {
        return switch (Objects.requireNonNull(api.connectionByUuid(uuid)).uiProfile()) {
            case CLASSIC -> PlayerUIProfile.CLASSIC;
            case POCKET -> PlayerUIProfile.POCKET;
        };
    }

    @Override
    public String getXboxUsername(UUID uuid) {
        return Objects.requireNonNull(api.connectionByUuid(uuid)).bedrockUsername();
    }

    @Override
    public String getPlayerXUID(UUID uuid) {
        return Objects.requireNonNull(api.connectionByUuid(uuid)).xuid();
    }

    @Override
    public boolean isLinked(UUID uuid) {
        return Objects.requireNonNull(api.connectionByUuid(uuid)).isLinked();
    }

    @Override
    public void sendFog(UUID uuid, String fog) {
        org.geysermc.geyser.api.connection.GeyserConnection connection = api.connectionByUuid(uuid);
        if (connection == null) {
            return;
        }
        connection.camera().sendFog(fog);
    }

    @Override
    public void removeFog(UUID uuid, String fog) {
        org.geysermc.geyser.api.connection.GeyserConnection connection = api.connectionByUuid(uuid);
        if (connection == null) {
            return;
        }
        connection.camera().removeFog(fog);
    }

    @Override
    public UUID getPackID(Path path) {
        for (Map.Entry<UUID, Path> id : resourcePackPathMap.entrySet()) {
            if (id.getValue().equals(path)) {
                return id.getKey();
            }
        }
        return UUID.randomUUID();
    }

    @Override
    public String getPackName(String id) {
        return resourcePackHashMap.get(UUID.fromString(id)).manifest().header().name();
    }


    @Override
    public String getPackDescription(String id) {
        return resourcePackHashMap.get(UUID.fromString(id)).manifest().header().description();
    }

    @Override
    public String getPackVersion(String id) {
        return resourcePackHashMap.get(UUID.fromString(id)).manifest().header().version().toString();
    }

    @Override
    @Nullable
    public Path getPackPath(String id) {
        return resourcePackPathMap.get(UUID.fromString(id));
    }

    @Override
    public boolean getPackExists(String id) {
        try {
            return resourcePackPathMap.containsKey(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}