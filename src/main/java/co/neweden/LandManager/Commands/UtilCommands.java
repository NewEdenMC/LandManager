package co.neweden.LandManager.Commands;

import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class UtilCommands implements CommandExecutor {

    private Set<Material> rtpBlocksBlacklist = new HashSet<>();
    private int rtpDefRadius;
    private Map<String, Integer> rtpWorldRadius = new HashMap<>();
    private int rtpPassLimit;

    public UtilCommands() {
        LandManager.getPlugin().getCommand("landmanager").setExecutor(this);
        LandManager.getPlugin().getCommand("rtp").setExecutor(this);

        rtpDefRadius = LandManager.getPlugin().getConfig().getInt("random_tp.default_radius", -1);
        rtpPassLimit = LandManager.getPlugin().getConfig().getInt("random_tp.pass_limit", 100);

        rtpWorldRadius.clear();
        ConfigurationSection rtpConfigWorldRadius = LandManager.getPlugin().getConfig().getConfigurationSection("random_tp.worlds.");
        for (String key : rtpConfigWorldRadius.getKeys(false)) {
            rtpWorldRadius.put(key, rtpConfigWorldRadius.getInt(key));
        }

        rtpBlocksBlacklist.clear();
        List<String> rtpConfigBlocksBlacklist = LandManager.getPlugin().getConfig().getStringList("random_tp.blocks_blacklist");
        for (String key : rtpConfigBlocksBlacklist) {
            try {
                rtpBlocksBlacklist.add(Material.valueOf(key));
            } catch (IllegalArgumentException e) {
                LandManager.getPlugin().getLogger().log(Level.WARNING, "Could not add '" + key + "' to Random TP Blocks Blacklist as it is not a valid Block Type, skipping, check 'blocks_blacklist' in 'rtp' section of the config.yml file.");
            }
        }
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
        Integer worldRadius = rtpWorldRadius.get(player.getWorld().getName());
        if (worldRadius == null) worldRadius = rtpDefRadius;
        int x; int z;
        Random rand = new Random();
        player.sendMessage(Util.formatString("&eTeleporting..."));

        for (int passes = 0; passes < rtpPassLimit; passes++) { // We don't want this running indefinitely
            if (worldRadius == -1) {
                x = rand.nextInt(); x = x - (x / 2);
                z = rand.nextInt(); z = z - (z / 2);
            } else {
                x = rand.nextInt(worldRadius * 2) - worldRadius;
                z = rand.nextInt(worldRadius * 2) - worldRadius;
            }
            Location loc = Util.getHighestAirBlockAt(player.getWorld(), x, z);
            if (loc == null || LandManager.getLandClaim(loc.getChunk()) != null) continue;

            Material belowB = loc.subtract(0, 1, 0).getBlock().getType();
            Material feetB = loc.add(0, 1, 0).getBlock().getType();
            Material headB = loc.add(0, 1, 0).getBlock().getType();

            if (!rtpBlocksBlacklist.contains(belowB) && !rtpBlocksBlacklist.contains(feetB) && !rtpBlocksBlacklist.contains(headB)) {
                player.teleport(loc.subtract(0, 1, 0));
                return;
            }
        }
        player.sendMessage(Util.formatString("&cTried to teleport you to a random location that isn't on a Blacklisted Block nor a Land Claim but after trying " + rtpPassLimit + " times I was unable to find a suitable location to send you to, contact a staff member who can help you find some empty land, please also report this issue."));
    }

}
