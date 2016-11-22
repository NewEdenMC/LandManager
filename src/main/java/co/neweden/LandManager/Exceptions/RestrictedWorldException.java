package co.neweden.LandManager.Exceptions;

import org.bukkit.World;

public class RestrictedWorldException extends Exception {

    private World world;

    public RestrictedWorldException(World world, String message) {
        super(message);
        this.world = world;
    }

    public RestrictedWorldException(Throwable cause) {
        super(cause);
    }

}
