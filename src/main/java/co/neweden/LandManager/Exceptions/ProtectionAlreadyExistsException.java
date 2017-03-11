package co.neweden.LandManager.Exceptions;

import co.neweden.LandManager.RegisteredProtection;

public class ProtectionAlreadyExistsException extends UserException {

    private RegisteredProtection protection;

    public ProtectionAlreadyExistsException(RegisteredProtection protection, String consoleMessage, String userMessage) {
        super(consoleMessage, userMessage);
        this.protection = protection;
    }

    public ProtectionAlreadyExistsException(RegisteredProtection protection, String message) { this(protection, message, message); }

    public ProtectionAlreadyExistsException(ProtectionAlreadyExistsException cause) { super(cause); }

    public RegisteredProtection getProtection() { return protection; }

}
