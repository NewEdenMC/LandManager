package co.neweden.LandManager.Exceptions;

import org.bukkit.World;

public class RestrictedWorldException extends Exception {

    private World world;
    private String userMessage;

    public RestrictedWorldException(World world, String consoleMessage, String userMessage) {
        super(consoleMessage);
        this.world = world;
        this.userMessage = userMessage;
    }

    public RestrictedWorldException(World world, String message) { this(world, message, message); }

    public RestrictedWorldException(Throwable cause) {
        super(cause);
    }

    public String getUserMessage() { return userMessage; }

    public String getConsoleMessage() { return getMessage(); }

    public World getWorld() { return world; }

}
