package co.neweden.LandManager.Exceptions;

import co.neweden.LandManager.LandClaim;

import java.util.UUID;

public class LandClaimLimitReachedException extends UserException {

    private LandClaim landClaim;
    private UUID playerUUID;

    public LandClaimLimitReachedException(LandClaim landClaim, UUID playerUUID, String consoleMessage, String userMessage) {
        super(consoleMessage, userMessage);
        this.playerUUID = playerUUID;
        this.landClaim = landClaim;
    }

    public LandClaimLimitReachedException(LandClaim landClaim, UUID playerUUID, String message) { this(landClaim, playerUUID, message, message); }

    public LandClaimLimitReachedException(LandClaimLimitReachedException cause) { super(cause); }

    public LandClaim getLandClaim() { return landClaim; }

    public UUID getPlayerUUID() { return playerUUID; }

}
