package dev.letsgoaway.geyserextras.menus;

import dev.letsgoaway.geyserextras.BedrockPlayer;
import dev.letsgoaway.geyserextras.PlayerDevice;
import dev.letsgoaway.geyserextras.PlayerPlatform;
import dev.letsgoaway.geyserextras.TabListCommand;
import dev.letsgoaway.geyserextras.form.BedrockContextMenu;
import dev.letsgoaway.geyserextras.form.elements.Button;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.util.FormImage;

import java.util.UUID;

public class TabListPlayerDetails extends BedrockContextMenu {
    private static String createFooter(Player player) {
        StringBuilder footer = new StringBuilder();
        footer.append("Name: "+ player.getName()+"\n");
        footer.append("Device: "+ PlayerDevice.getPlayerDevice(player).displayName+"\n");
        footer.append("Platform: "+ PlayerPlatform.getPlayerPlatform(player).displayName+"\n");
        return footer.toString();
    }
    public TabListPlayerDetails(BedrockPlayer bplayer, Player player) {
        super(player.getPlayerListName(),createFooter(player));
        onClose = ()->{
            new TabList(bplayer);
        };
        add(new Button("Message", FormImage.Type.PATH,
                "textures/ui/chat_send.png", ()->{
            if (Bukkit.getOfflinePlayer(player.getUniqueId()).isOnline()) {
                new TabListMessageUI(player,bplayer).show(bplayer);
            }
        }));
    }
}
