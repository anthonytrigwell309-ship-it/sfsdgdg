package com.example.punish;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PunishPlugin extends JavaPlugin {

    private PunishManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.manager = new PunishManager(this);
        getServer().getPluginManager().registerEvents(new PunishListener(this, manager), this);
        getLogger().info("PunishPlugin enabled.");
    }

    /** Where punished players get teleported to while serving their time. Null if not set yet. */
    public Location getHoldingLocation() {
        String worldName = getConfig().getString("hold.world", "");
        if (worldName == null || worldName.isEmpty()) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                getConfig().getDouble("hold.x"),
                getConfig().getDouble("hold.y"),
                getConfig().getDouble("hold.z"),
                (float) getConfig().getDouble("hold.yaw"),
                (float) getConfig().getDouble("hold.pitch"));
    }

    public void setHoldingLocation(Location loc) {
        getConfig().set("hold.world", loc.getWorld().getName());
        getConfig().set("hold.x", loc.getX());
        getConfig().set("hold.y", loc.getY());
        getConfig().set("hold.z", loc.getZ());
        getConfig().set("hold.yaw", loc.getYaw());
        getConfig().set("hold.pitch", loc.getPitch());
        saveConfig();
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.restoreAll();
        getLogger().info("PunishPlugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!staff.hasPermission("punish.use")) {
            staff.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length < 1) {
            staff.sendMessage("§eUsage: /" + label + " <player>"
                    + (label.equalsIgnoreCase("punish") ? "  (or /punish sethold)" : ""));
            return true;
        }

        if (label.equalsIgnoreCase("punish") && args[0].equalsIgnoreCase("sethold")) {
            setHoldingLocation(staff.getLocation());
            staff.sendMessage("§aHolding location set to your current position §7("
                    + staff.getLocation().getBlockX() + ", " + staff.getLocation().getBlockY() + ", "
                    + staff.getLocation().getBlockZ() + ")§a. Punished players will be sent here.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            staff.sendMessage("§cPlayer not found (must be online).");
            return true;
        }

        if (label.equalsIgnoreCase("pardon")) {
            manager.pardon(target);
            staff.sendMessage("§a" + target.getName() + " has been pardoned.");
            target.sendMessage("§aYou have been pardoned by " + staff.getName() + ".");
            return true;
        }

        // /punish from here
        if (target.equals(staff)) {
            staff.sendMessage("§cYou can't punish yourself... probably.");
            return true;
        }
        staff.openInventory(PunishMenu.durationMenu(target));
        return true;
    }
}
