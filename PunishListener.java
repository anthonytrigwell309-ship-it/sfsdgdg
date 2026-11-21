package com.example.punish;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PunishListener implements Listener {

    private final PunishPlugin plugin;
    private final PunishManager manager;

    public PunishListener(PunishPlugin plugin, PunishManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PunishMenu.DurationHolder) && !(holder instanceof PunishMenu.MethodHolder)) {
            return;
        }
        event.setCancelled(true); // never let staff take the menu items

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Player staff = (Player) event.getWhoClicked();
        String name = meta.getDisplayName();

        if (holder instanceof PunishMenu.DurationHolder dh) {
            PunishManager.Duration duration = switch (stripColor(name)) {
                case "5 Minutes" -> PunishManager.Duration.FIVE_MIN;
                case "15 Minutes" -> PunishManager.Duration.FIFTEEN_MIN;
                case "1 Hour" -> PunishManager.Duration.ONE_HOUR;
                case "24 Hours" -> PunishManager.Duration.ONE_DAY;
                case "Indefinite" -> PunishManager.Duration.INDEFINITE;
                default -> null;
            };
            if (duration == null) return;
            staff.openInventory(PunishMenu.methodMenu(dh.target, duration));
            return;
        }

        if (holder instanceof PunishMenu.MethodHolder mh) {
            PunishManager.Method method = switch (stripColor(name)) {
                case "Knocked Out" -> PunishManager.Method.KNOCKED_OUT;
                case "UFO Abduction" -> PunishManager.Method.UFO_ABDUCTION;
                case "Trapdoor" -> PunishManager.Method.TRAPDOOR;
                case "Yeeted" -> PunishManager.Method.YEETED;
                case "Portal" -> PunishManager.Method.PORTAL;
                case "Exiled" -> PunishManager.Method.EXILED;
                case "Poof" -> PunishManager.Method.POOF;
                case "Arrested" -> PunishManager.Method.ARRESTED;
                case "Shrunk" -> PunishManager.Method.SHRUNK;
                case "Random" -> PunishManager.Method.random();
                default -> null;
            };
            if (method == null) return;

            staff.closeInventory();
            manager.punish(staff, mh.target, mh.duration, method);
        }
    }

    private String stripColor(String s) {
        return s.replaceAll("§.", "");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (manager.isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou're currently punished and can't chat.");
        }
    }
}
