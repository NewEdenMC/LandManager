package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.Events.PlayerEnterLandClaimEvent;
import co.neweden.LandManager.Events.PlayerExitLandClaimEvent;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class LocationEvents implements Listener {

    public LocationEvents() {
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
    }

    private enum EventResponse { NO_ACTION, SUCCESS, CANCELED }

    private EventResponse handleEvent(PlayerEvent callingEvent, Chunk fromChunk, Chunk toChunk, boolean onCancelGoToWorldSpawn) {
        if (callingEvent instanceof PlayerMoveEvent) {
            PlayerMoveEvent moveEvent = (PlayerMoveEvent) callingEvent;
            if (moveEvent.getFrom().getChunk().equals(moveEvent.getTo().getChunk()))
                return EventResponse.NO_ACTION;
        }

        LandClaim fromLand = (fromChunk != null) ? LandManager.getLandClaim(fromChunk) : null;
        LandClaim toLand = (toChunk != null) ? LandManager.getLandClaim(toChunk) : null;

        if ((fromLand == null && toLand == null) || fromLand == toLand)
            return EventResponse.NO_ACTION;

        if (fromLand != null) {
            PlayerExitLandClaimEvent exitEvent = new PlayerExitLandClaimEvent(fromLand, callingEvent.getPlayer());
            Bukkit.getServer().getPluginManager().callEvent(exitEvent);
            callingEvent.getPlayer().sendMessage(Util.formatString("You have left the Land Claim: &e" + fromLand.getDisplayName()));
        }

        if (toLand != null) {
            PlayerEnterLandClaimEvent enterEvent = new PlayerEnterLandClaimEvent(toLand, callingEvent.getPlayer());

            if (!toLand.testAccessLevel(callingEvent.getPlayer().getUniqueId(), ACL.Level.VIEW) && !callingEvent.getPlayer().hasPermission("landmanager.enterany")) {
                enterEvent.setCancelled(true);
            }

            Bukkit.getServer().getPluginManager().callEvent(enterEvent);

            if (enterEvent.isCancelled()) {
                if (onCancelGoToWorldSpawn) {
                    callingEvent.getPlayer().teleport(toChunk.getWorld().getSpawnLocation());
                }

                if (callingEvent instanceof Cancellable)
                    ((Cancellable) callingEvent).setCancelled(true);

                callingEvent.getPlayer().sendMessage(Util.formatString(enterEvent.getDenyMessage()));
                return EventResponse.CANCELED;
            }

            callingEvent.getPlayer().sendMessage(Util.formatString("You have entered the Land Claim: &e" + toLand.getDisplayName()));
            return EventResponse.SUCCESS;
        }

        return EventResponse.SUCCESS;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleEvent(event, null, event.getPlayer().getLocation().getChunk(), true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        handleEvent(event, event.getFrom().getChunk(), event.getTo().getChunk(), false);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        handleEvent(event, event.getFrom().getChunk(), event.getTo().getChunk(), false);
    }

}
