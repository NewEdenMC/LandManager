package co.neweden.LandManager.Commands;

public class CommandCache {

    public CommandCache(String command, String[] args, long runTime) {
        this.command = command;
        this.args = args;
        this.runTime = runTime;
    }

    public String command;
    public String[] args;
    public long runTime;

}
