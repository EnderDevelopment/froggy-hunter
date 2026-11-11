package com.enderdevelopment.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class Main extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey frogKey;
    private NamespacedKey tongueCooldownKey;
    private NamespacedKey biteCooldownKey;
    private NamespacedKey knockoutKey;

    @Override
    public void onEnable() {
        frogKey = new NamespacedKey(this, "froggy");
        tongueCooldownKey = new NamespacedKey(this, "tongue_cooldown");
        biteCooldownKey = new NamespacedKey(this, "bite_cooldown");
        knockoutKey = new NamespacedKey(this, "knocked_out");
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("froggy") == null) {
            throw new IllegalStateException("The froggy command is missing from plugin.yml");
        }
        getCommand("froggy").setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::tickFrogs, 20L, 5L);
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Slime slime && isFroggy(slime)) {
                    slime.remove();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(Component.text("Use /froggy spawn to summon the hunter.", NamedTextColor.GREEN));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("A player must run /froggy spawn.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("spawn")) {
            sender.sendMessage(Component.text("Usage: /froggy spawn", NamedTextColor.YELLOW));
            return true;
        }
        spawnFroggy(player.getLocation());
        player.sendMessage(Component.text("A hungry Froggy is on the hunt.", NamedTextColor.GREEN));
        return true;
    }

    private void spawnFroggy(Location location) {
        Entity entity = location.getWorld().spawnEntity(location, EntityType.SLIME);
        if (!(entity instanceof Slime slime)) {
            entity.remove();
            return;
        }
        slime.setSize(3);
        slime.setCustomName("Froggy");
        slime.setCustomNameVisible(true);
        slime.setPersistent(true);
        slime.setRemoveWhenFarAway(false);
        slime.setAI(false);
        slime.getPersistentDataContainer().set(frogKey, PersistentDataType.BYTE, (byte) 1);
        slime.getPersistentDataContainer().set(tongueCooldownKey, PersistentDataType.INTEGER, 0);
        slime.getPersistentDataContainer().set(biteCooldownKey, PersistentDataType.INTEGER, 0);
    }

    private void tickFrogs() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Slime slime) || !isFroggy(slime)) {
                    continue;
                }
                Player target = world.getPlayers().stream()
                        .filter(player -> !player.isDead() && !isKnockedOut(player))
                        .filter(player -> player.getLocation().distanceSquared(slime.getLocation()) <= 30 * 30)
                        .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(slime.getLocation())))
                        .orElse(null);
                if (target == null) {
                    slime.setVelocity(new Vector(0, 0, 0));
                    continue;
                }
                steerFroggy(slime, target);
                decrementCooldown(slime, tongueCooldownKey);
                decrementCooldown(slime, biteCooldownKey);
                double distanceSquared = slime.getLocation().distanceSquared(target.getLocation());
                if (distanceSquared <= 144 && cooldown(slime, tongueCooldownKey) == 0) {
                    useTongue(slime, target);
                    slime.getPersistentDataContainer().set(tongueCooldownKey, PersistentDataType.INTEGER, 100);
                }
                if (distanceSquared <= 3.24 && cooldown(slime, biteCooldownKey) == 0) {
                    bite(slime, target);
                    slime.getPersistentDataContainer().set(biteCooldownKey, PersistentDataType.INTEGER, 20);
                }
            }
        }
    }

    private void steerFroggy(Slime slime, Player target) {
        Location frogLocation = slime.getLocation();
        Location targetLocation = target.getLocation();
        Vector direction = targetLocation.toVector().subtract(frogLocation.toVector());
        direction.setY(0);
        if (direction.lengthSquared() > 0.001) {
            direction.normalize();
        }
        Vector current = slime.getVelocity();
        double speed = 0.28;
        double vertical = current.getY();
        if (slime.isOnGround()) {
            vertical = 0.52;
        }

        Location below = frogLocation.clone().subtract(0, 0.1, 0);
        Material underfoot = below.getBlock().getType();
        Material inBlock = frogLocation.getBlock().getType();
        if (underfoot.isWater() || inBlock.isWater()) {
            int waterY = inBlock.isWater() ? frogLocation.getBlockY() : below.getBlockY();
            double surfaceY = waterY + 1.02;
            vertical = Math.max(-0.18, Math.min(0.18, surfaceY - frogLocation.getY()));
        }
        slime.setVelocity(new Vector(direction.getX() * speed, vertical, direction.getZ() * speed));
    }

    private void useTongue(Slime slime, Player target) {
        Vector pull = slime.getLocation().toVector().subtract(target.getLocation().toVector());
        if (pull.lengthSquared() > 0.01) {
            pull.normalize().multiply(0.72);
            pull.setY(Math.max(0.18, pull.getY() + 0.22));
            target.setVelocity(target.getVelocity().add(pull));
        }
        target.sendActionBar(Component.text("Froggy lashes its tongue around you!", NamedTextColor.GREEN));
        slime.getWorld().playSound(slime.getLocation(), org.bukkit.Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.7f);
    }

    private void bite(Slime slime, Player target) {
        target.damage(3.0, slime);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
        target.sendActionBar(Component.text("Froggy pins you down and starts biting!", NamedTextColor.DARK_GREEN));
        slime.getWorld().playSound(slime.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.8f, 0.65f);
        if (Math.random() < 0.18 && !isKnockedOut(target)) {
            int duration = 60;
            target.getPersistentDataContainer().set(knockoutKey, PersistentDataType.INTEGER, duration);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 8, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1, false, true, true));
            target.sendMessage(Component.text("Froggy knocks you out!", NamedTextColor.RED));
        }
        target.getPersistentDataContainer().set(knockoutKey, PersistentDataType.INTEGER,
                Math.max(0, target.getPersistentDataContainer().getOrDefault(knockoutKey, PersistentDataType.INTEGER, 0) - 5));
    }

    @EventHandler
    public void onFroggyHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Slime slime) || !isFroggy(slime)
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (Math.random() < 0.18) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 8, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, true, true));
            player.sendMessage(Component.text("Froggy knocks you out!", NamedTextColor.RED));
        }
    }

    private boolean isFroggy(Slime slime) {
        return slime.getPersistentDataContainer().has(frogKey, PersistentDataType.BYTE);
    }

    private int cooldown(Slime slime, NamespacedKey key) {
        return slime.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private void decrementCooldown(Slime slime, NamespacedKey key) {
        int remaining = cooldown(slime, key);
        if (remaining > 0) {
            slime.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.max(0, remaining - 5));
        }
    }

    private boolean isKnockedOut(Player player) {
        int remaining = player.getPersistentDataContainer().getOrDefault(knockoutKey, PersistentDataType.INTEGER, 0);
        if (remaining <= 0) {
            return false;
        }
        int updated = Math.max(0, remaining - 5);
        player.getPersistentDataContainer().set(knockoutKey, PersistentDataType.INTEGER, updated);
        return true;
    }
}
