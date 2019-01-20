package me.nowaha.playerstats;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
public class Events implements Listener {

    @EventHandler
    void onPlayerDeath(PlayerDeathEvent e) {

        FileConfiguration playerStats = PlayerStats.instance.playerStats;

        String path = ((Player) e.getEntity()).getUniqueId() + ".deaths";
        try {
            int old = (int) playerStats.get(path);
            playerStats.set(path, old + 1);
        } catch (Exception ex) {
            PlayerStats.instance.getLogger().info(path);
            PlayerStats.instance.getLogger().info(PlayerStats.instance.toString());
            playerStats.set(path, 1);
        }

        playerStats.save();
    }

    @EventHandler
    void EntityDamageByEntityEvent(EntityDamageByEntityEvent e) {

        try {
            Player hitPlayer = (Player) e.getDamager();

            if (hitPlayer.getHealth() - e.getFinalDamage() <= 0) {
                Player killer = null;

                try {
                    killer = (Player) e.getDamager();
                } catch (Exception ex) {
                    try { killer = (Player) ((Arrow) e.getDamager()).getShooter(); } catch (Exception ignored) { }
                }

                if (killer == null) {
                    return;
                }

                FileConfiguration playerStats = PlayerStats.instance.playerStats;

                String path = killer.getUniqueId() + ".kills";
                try {
                    int old = (int) playerStats.get(path);
                    playerStats.set(path, old + 1);
                } catch (Exception ex) {
                    playerStats.set(path, 1);
                }

                playerStats.save();
            }
        } catch (Exception ignored) {

        }
    }
}
