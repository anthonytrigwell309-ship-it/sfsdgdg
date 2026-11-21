package com.example.punish;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PunishManager {

    public enum Duration {
        FIVE_MIN(5 * 60), FIFTEEN_MIN(15 * 60), ONE_HOUR(60 * 60), ONE_DAY(24 * 60 * 60), INDEFINITE(-1);
        public final int seconds;
        Duration(int seconds) { this.seconds = seconds; }
    }

    public enum Method {
        KNOCKED_OUT, UFO_ABDUCTION, TRAPDOOR, YEETED, PORTAL, EXILED, POOF, ARRESTED, SHRUNK;

        private static final Random RNG = new Random();
        public static Method random() {
            Method[] vals = values();
            return vals[RNG.nextInt(vals.length)];
        }
    }

    private final PunishPlugin plugin;
    private final Map<UUID, BukkitTask> restoreTasks = new HashMap<>();
    private final Map<UUID, Boolean> muted = new HashMap<>();
    private final Map<UUID, GameMode> originalGameMode = new HashMap<>();

    public PunishManager(PunishPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isMuted(UUID uuid) {
        return muted.getOrDefault(uuid, false);
    }

    public void punish(Player staff, Player target, Duration duration, Method method) {
        Bukkit.broadcastMessage("§4§l[PUNISH] §c" + target.getName() + " §7is about to face judgement... (§e"
                + methodLabel(method) + "§7, by " + staff.getName() + ")");

        playAnimation(target, method);
        muted.put(target.getUniqueId(), true);
        originalGameMode.putIfAbsent(target.getUniqueId(), target.getGameMode());

        Location hold = plugin.getHoldingLocation();
        if (hold == null) {
            staff.sendMessage("§eNo holding location set — run §f/punish sethold §ewhile standing"
                    + " where punished players should be sent. They'll just be locked to Adventure"
                    + " mode in place for now.");
        }

        // let the animation play for a moment before locking them away
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline()) return;
                if (hold != null) target.teleport(hold);
                target.setGameMode(GameMode.ADVENTURE);
            }
        }.runTaskLater(plugin, 30L);

        // cancel any previous pending restore for this player
        BukkitTask existing = restoreTasks.remove(target.getUniqueId());
        if (existing != null) existing.cancel();

        if (duration != Duration.INDEFINITE) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    restore(target);
                    target.sendMessage("§aYour punishment has ended. Behave.");
                }
            }.runTaskLater(plugin, (long) duration.seconds * 20L);
            restoreTasks.put(target.getUniqueId(), task);
        } else {
            target.sendMessage("§cYou are indefinitely punished. A staff member must run §e/pardon " + target.getName());
        }
    }

    public void pardon(Player target) {
        BukkitTask task = restoreTasks.remove(target.getUniqueId());
        if (task != null) task.cancel();
        restore(target);
    }

    public void restoreAll() {
        for (UUID uuid : new java.util.ArrayList<>(restoreTasks.keySet())) {
            BukkitTask task = restoreTasks.remove(uuid);
            if (task != null) task.cancel();
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) restore(p);
        }
    }

    private void restore(Player target) {
        UUID id = target.getUniqueId();
        if (target.isOnline()) {
            GameMode original = originalGameMode.get(id);
            target.setGameMode(original != null ? original : GameMode.SURVIVAL);
            target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            target.removePotionEffect(PotionEffectType.WEAKNESS);
            target.removePotionEffect(PotionEffectType.LEVITATION);
            target.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        originalGameMode.remove(id);
        muted.remove(id);
    }

    private String methodLabel(Method m) {
        return switch (m) {
            case KNOCKED_OUT -> "Knocked Out";
            case UFO_ABDUCTION -> "UFO Abduction";
            case TRAPDOOR -> "Trapdoor";
            case YEETED -> "Yeeted";
            case PORTAL -> "Portal";
            case EXILED -> "Exiled";
            case POOF -> "Poof";
            case ARRESTED -> "Arrested";
            case SHRUNK -> "Shrunk";
        };
    }

    // ---------- Animations ----------

    private void playAnimation(Player target, Method method) {
        switch (method) {
            case KNOCKED_OUT -> animateKnockedOut(target);
            case UFO_ABDUCTION -> animateUfo(target);
            case TRAPDOOR -> animateTrapdoor(target);
            case YEETED -> animateYeet(target);
            case PORTAL -> animatePortal(target);
            case EXILED -> animateExiled(target);
            case POOF -> animatePoof(target);
            case ARRESTED -> animateArrested(target);
            case SHRUNK -> animateShrunk(target);
        }
    }

    private void animateKnockedOut(Player target) {
        Location loc = target.getLocation();
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        target.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        target.sendTitle("§c§lKNOCKED OUT", "§7Better luck next time.", 5, 40, 10);
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step++ >= 6) { cancel(); return; }
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.7f, 0.8f);
                target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    private void animateUfo(Player target) {
        target.sendTitle("§d§l[ZAP]", "§7You've been abducted...", 5, 50, 15);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 70, 1, false, true));
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step++ >= 14) { cancel(); return; }
                Location l = target.getLocation();
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, l, 10, 0.4, 0.2, 0.4, 0);
                target.getWorld().spawnParticle(Particle.END_ROD, l, 5, 0.2, 0.1, 0.2, 0.01);
                if (step % 4 == 0) target.getWorld().playSound(l, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 2f);
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void animateTrapdoor(Player target) {
        target.sendTitle("§6§lTRAPDOOR", "§7The floor had enough of you.", 3, 30, 8);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1f, 0.7f);
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 40, 0.6, 0.1, 0.6, 0.02);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, true));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SMALL_FALL, 1f, 1f);
    }

    private void animateYeet(Player target) {
        target.sendTitle("§a§lYEETED", "§7Gone. Reduced to atoms.", 3, 30, 8);
        target.setVelocity(target.getLocation().getDirection().multiply(-2).setY(1.8));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.3f);
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step++ >= 8) { cancel(); return; }
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void animatePortal(Player target) {
        target.sendTitle("§5§lPORTAL", "§7Sucked into the void.", 3, 30, 8);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step++ >= 10) { cancel(); return; }
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 1);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void animateExiled(Player target) {
        target.sendTitle("§7§lEXILED", "§7Escorted off the premises.", 5, 40, 10);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 0.8f);
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step++ >= 4) { cancel(); return; }
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WOOD_STEP, 1f, 0.5f);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void animatePoof(Player target) {
        target.sendTitle("§8§lPOOF", "§7...and they were gone.", 3, 25, 8);
        target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.5f);
    }

    private void animateArrested(Player target) {
        target.sendTitle("§e§lARRESTED", "§7You have the right to remain quiet.", 5, 40, 10);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
    }

    private void animateShrunk(Player target) {
        target.sendTitle("§b§lSHRUNK", "§7Tiny and humbled.", 3, 30, 8);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1f, 1.8f);
        target.getWorld().spawnParticle(Particle.POOF, target.getLocation(), 25, 0.3, 0.3, 0.3, 0.02);
        // Note: actual model scaling needs the SCALE attribute (Paper 1.20.5+); omitted here for
        // broader version compatibility. Swap in Attribute.GENERIC_SCALE if your server supports it.
    }
}
