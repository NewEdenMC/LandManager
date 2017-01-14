package co.neweden.LandManager.Commands;

import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ProtectionCommands implements CommandExecutor {

    private Map<Player, CommandCache> cmdCache = new HashMap<>();
    private Collection<Player> persistPlayers = new HashSet<>();

    public ProtectionCommands() {
        LandManager.getPlugin().getCommand("ppersist").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        Player player = (Player) sender;
        if (handlePersist(command, player)) return true; // we are done if the player ran the persist command
        cmdCache.put(player, new CommandCache(command.getName(), args, System.currentTimeMillis()));

        return true;
    }

    private boolean handlePersist(Command command, Player player) {
        if (!command.getName().toLowerCase().equals("ppersist")) return false;
        if (persistPlayers.contains(player)) {
            persistPlayers.remove(player);
            cmdCache.remove(player);
            player.sendMessage(Util.formatString("&bPersist mode for protection commands &cOFF"));
        } else {
            persistPlayers.add(player);
            player.sendMessage(Util.formatString(
                    "&bPersist mode for protection commands &aON\n" +
                    "&bThe next protection command you run will stay active until you run /" + command.getName().toLowerCase() + " again"
            ));
        }
        return true;
    }

}
