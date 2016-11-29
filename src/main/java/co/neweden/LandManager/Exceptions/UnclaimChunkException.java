package co.neweden.LandManager.Exceptions;

import co.neweden.LandManager.LandClaim;
import org.bukkit.Chunk;

public class UnclaimChunkException extends UserException {

    private LandClaim land;
    private Chunk chunk;
    public enum Reason { LAST_CHUNK, HOME_IN_CHUNK }
    private Reason cause;

    public UnclaimChunkException(LandClaim land, Chunk chunk, Reason cause, String consoleMessage, String userMessage) {
        super(consoleMessage, userMessage);
        this.land = land;
        this.chunk = chunk;
        this.cause = cause;
    }

    public UnclaimChunkException(LandClaim land, Chunk chunk, Reason cause, String message) { this(land, chunk, cause, message, message); }

    public UnclaimChunkException(UnclaimChunkException cause) { super(cause); }

    public LandClaim getLandClaim() { return land; }

    public Chunk getChunk() { return chunk; }

    public Reason getReason() { return cause; }

}
