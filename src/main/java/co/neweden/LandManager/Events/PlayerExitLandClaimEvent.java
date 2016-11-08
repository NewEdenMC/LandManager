package co.neweden.LandManager.Events;

import co.neweden.LandManager.LandClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerExitLandClaimEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private LandClaim land;
    private Player player;

    public PlayerExitLandClaimEvent(LandClaim land, Player player) {
        this.land = land;
        this.player = player;
    }

    public LandClaim getLandClaim() { return land; }

    public Player getPlayer() { return player; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
