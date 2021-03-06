package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.LandManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEvents implements Listener {

    public PlayerEvents() {
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        LandManager.updatePlayerCache(event.getPlayer());
    }

}
