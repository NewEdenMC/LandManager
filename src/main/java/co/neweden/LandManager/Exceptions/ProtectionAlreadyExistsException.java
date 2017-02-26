package co.neweden.LandManager.Exceptions;

import co.neweden.LandManager.Protection;

public class ProtectionAlreadyExistsException extends UserException {

    private Protection protection;

    public ProtectionAlreadyExistsException(Protection protection, String consoleMessage, String userMessage) {
        super(consoleMessage, userMessage);
        this.protection = protection;
    }

    public ProtectionAlreadyExistsException(Protection protection, String message) { this(protection, message, message); }

    public ProtectionAlreadyExistsException(ProtectionAlreadyExistsException cause) { super(cause); }

    public Protection getProtection() { return protection; }

}
