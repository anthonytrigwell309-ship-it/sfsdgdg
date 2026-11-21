package com.example.punish;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PunishMenu {

    /** Marks an inventory as the duration-select menu, remembers who it's for. */
    public static class DurationHolder implements InventoryHolder {
        public final Player target;
        public DurationHolder(Player target) { this.target = target; }
        @Override public Inventory getInventory() { return null; }
    }

    /** Marks an inventory as the method-select menu, remembers target + chosen duration. */
    public static class MethodHolder implements InventoryHolder {
        public final Player target;
        public final PunishManager.Duration duration;
        public MethodHolder(Player target, PunishManager.Duration duration) {
            this.target = target;
            this.duration = duration;
        }
        @Override public Inventory getInventory() { return null; }
    }

    public static Inventory durationMenu(Player target) {
        DurationHolder holder = new DurationHolder(target);
        Inventory inv = org.bukkit.Bukkit.createInventory(holder, 27,
                "§4Punish " + target.getName() + " §7- Duration");

        inv.setItem(10, item(Material.CLOCK, "§e5 Minutes", "§7A quick lesson."));
        inv.setItem(12, item(Material.CLOCK, "§615 Minutes", "§7A short stay."));
        inv.setItem(14, item(Material.CLOCK, "§61 Hour", "§7Standard sentence."));
        inv.setItem(16, item(Material.CLOCK, "§c24 Hours", "§7They really earned this."));
        inv.setItem(22, item(Material.BARRIER, "§4Indefinite", "§7Until a staff member pardons them."));
        return inv;
    }

    public static Inventory methodMenu(Player target, PunishManager.Duration duration) {
        MethodHolder holder = new MethodHolder(target, duration);
        Inventory inv = org.bukkit.Bukkit.createInventory(holder, 27,
                "§4Punish " + target.getName() + " §7- Method");

        inv.setItem(1, item(Material.IRON_SWORD, "§fKnocked Out", "§7Loses a dramatic fight."));
        inv.setItem(3, item(Material.END_CRYSTAL, "§dUFO Abduction", "§7Beamed up in a green light."));
        inv.setItem(5, item(Material.OAK_TRAPDOOR, "§6Trapdoor", "§7The floor opens beneath them."));
        inv.setItem(7, item(Material.SLIME_BALL, "§aYeeted", "§7Launched off into the distance."));
        inv.setItem(10, item(Material.ENDER_PEARL, "§5Portal", "§7Sucked into a swirling vortex."));
        inv.setItem(12, item(Material.IRON_BARS, "§7Exiled", "§7Marched off by invisible guards."));
        inv.setItem(14, item(Material.GUNPOWDER, "§8Poof", "§7Vanishes in a puff of smoke."));
        inv.setItem(16, item(Material.CHAIN, "§eArrested", "§7Cuffed and hauled away."));
        inv.setItem(19, item(Material.POTION, "§bShrunk", "§7Shrinks down and scurries off."));
        inv.setItem(22, item(Material.NETHER_STAR, "§cRandom", "§7Let fate decide."));
        return inv;
    }

    private static ItemStack item(Material mat, String name, String lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        stack.setItemMeta(meta);
        return stack;
    }
}
