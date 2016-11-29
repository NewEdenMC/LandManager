package co.neweden.LandManager.Exceptions;

import org.bukkit.World;

public class RestrictedWorldException extends UserException {

    private World world;

    public RestrictedWorldException(World world, String consoleMessage, String userMessage) {
        super(consoleMessage, userMessage);
        this.world = world;
    }

    public RestrictedWorldException(World world, String message) { this(world, message, message); }

    public RestrictedWorldException(RestrictedWorldException cause) { super(cause); }

    public World getWorld() { return world; }

}
