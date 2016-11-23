package co.neweden.LandManager.Exceptions;

public class UserException extends Exception {

    private String userMessage;

    public UserException(String consoleMessage, String userMessage) {
        super(consoleMessage);
        this.userMessage = userMessage;
    }

    public UserException(String message) { this(message, message); }

    public UserException(UserException cause) { super(cause); }

    public String getUserMessage() { return userMessage; }

    public String getConsoleMessage() { return getMessage(); }

}
