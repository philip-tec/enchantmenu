package me.enchant;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.UUID;

public class PressurePlateLauncher implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    private FileConfiguration getConfig() {
        return EnchantPlugin.getInstance().getConfig();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!getConfig().getBoolean("launcher.enabled", true))
            return;

        Location loc = player.getLocation();
        Block blockAtFeet = loc.getBlock();
        Block blockBelow = loc.clone().subtract(0, 1, 0).getBlock();

        
        if (blockAtFeet.getType() != Material.IRON_PLATE && blockBelow.getType() != Material.IRON_PLATE)
            return;

        UUID playerId = player.getUniqueId();
        long cooldownTime = getConfig().getLong("launcher.cooldown", 0); 

        if (cooldowns.containsKey(playerId)) {
            long timeSinceLastLaunch = System.currentTimeMillis() - cooldowns.get(playerId);
            if (timeSinceLastLaunch < cooldownTime)
                return;
        }

        double forwardMultiplier = getConfig().getDouble("launcher.forward-multiplier", 2.5);
        double upwardMultiplier = getConfig().getDouble("launcher.upward-multiplier", 0.5);

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(forwardMultiplier);
        velocity.setY(velocity.getY() + upwardMultiplier);
        player.setVelocity(velocity);

        if (getConfig().getBoolean("launcher.effects.enabled", true)) {
            if (getConfig().getBoolean("launcher.effects.sound", true)) {
                String soundName = getConfig().getString("launcher.effects.sound-type", "ENDERDRAGON_WINGS");
                try {
                    Sound sound = Sound.valueOf(soundName);
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 1.0f);
                }
            }

            if (getConfig().getBoolean("launcher.effects.particles", true)) {
                player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }
        }

        if (getConfig().getBoolean("launcher.send-message", false)) {
            String message = getConfig().getString("launcher.launch-message", "&7Whoosh!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public void cleanupPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
