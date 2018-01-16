package co.neweden.LandManager.Commands;

import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class UtilCommands implements CommandExecutor {

    public UtilCommands() {
        LandManager.getPlugin().getCommand("landmanager").setExecutor(this);
        LandManager.getPlugin().getCommand("rtp").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        try {
            switch (command.getName().toLowerCase()) {
                case "landmanager": adminCommand(sender, args); return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
                return true;
            }
            Player player = (Player) sender;

            switch (command.getName().toLowerCase()) {
                case "rtp": rtpCommand(player); break;
            }
        } catch (CommandException e) {
            sender.sendMessage(Util.formatString(e.getMessage()));
        }

        return true;
    }

    private void adminCommand(CommandSender sender, String[] args) {
        if (args.length == 0)
            throw new CommandException(adminCommandHelp());

        switch (args[0].toLowerCase()) {
            case "reload": reloadCommand(sender); break;
            case "reloadconfig": reloadConfigCommand(sender); break;
        }
    }

    private static String adminCommandHelp() {
        return Util.formatString(
                "&fLandManager version " + LandManager.getPlugin().getDescription().getVersion() + "\n" +
                "&fAvailable sub-commands are:\n" +
                "&f- &breload&f: reload all configurations from file, Protection and Land data from the database\n" +
                "&f- &breloadconfig&f: reload the configuration from file"
        );
    }

    private static void reloadCommand(CommandSender sender) {
        sender.sendMessage(Util.formatString("&bReloading all data"));
        if (LandManager.getPlugin().reload())
            sender.sendMessage(Util.formatString("&aReload successful"));
        else
            throw new CommandException("&cReload failed");
    }

    private static void reloadConfigCommand(CommandSender sender) {
        sender.sendMessage(Util.formatString("&bReloading config"));
        LandManager.getPlugin().reloadConfig();
        sender.sendMessage(Util.formatString("&aReload successful"));
    }

    private void rtpCommand(Player player) {
        int defRadius = LandManager.getPlugin().getConfig().getInt("random_tp.default_radius", -1);
        int worldRadius = LandManager.getPlugin().getConfig().getInt("random_tp.worlds." + player.getWorld().getName(), defRadius);
        int passLimit = LandManager.getPlugin().getConfig().getInt("random_tp.pass_limit", 100);
        int x; int z;
        Random rand = new Random();
        player.sendMessage(Util.formatString("&eTeleporting..."));

        int passes = 0;
        while (passes < passLimit) { // We don't want this running indefinitely
            if (worldRadius == -1) {
                x = rand.nextInt(); x = x - (x / 2);
                z = rand.nextInt(); z = z - (z / 2);
            } else {
                x = rand.nextInt(worldRadius * 2) - worldRadius;
                z = rand.nextInt(worldRadius * 2) - worldRadius;
            }
            Location loc = Util.getHighestAirBlockAt(player.getWorld(), x, z);
            if (loc == null) continue;
            Material belowBlock = loc.subtract(0, 1, 0).getBlock().getType();
            if (
                    !belowBlock.equals(Material.WATER) &&
                    !belowBlock.equals(Material.STATIONARY_WATER) &&
                    !belowBlock.equals(Material.LAVA) &&
                    !belowBlock.equals(Material.STATIONARY_LAVA) &&
                    LandManager.getLandClaim(loc.getChunk()) == null)
            {
                player.teleport(loc.add(0, 1, 0));
                return;
            } else
                passes++;
        }
        player.sendMessage(Util.formatString("&cTried to teleport you to a random location that isn't on water, lava or a land claim but after trying " + passLimit + " I was unable to find a location to send you to, contact a staff member who can help you find some empty land, please also report this issue."));
    }

}
