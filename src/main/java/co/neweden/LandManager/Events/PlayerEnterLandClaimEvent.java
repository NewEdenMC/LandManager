package co.neweden.LandManager.Events;

import co.neweden.LandManager.LandClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerEnterLandClaimEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private LandClaim land;
    private Player player;
    private boolean cancelled = false;
    private String denyMessage = "&cYou are not permitted to enter this land claim.";

    public PlayerEnterLandClaimEvent(LandClaim land, Player player) {
        this.land = land;
        this.player = player;
    }

    public LandClaim getLandClaim() { return land; }

    public Player getPlayer() { return player; }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public void setDenyMessage(String message) { denyMessage = message; }

    public String getDenyMessage() { return denyMessage; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
