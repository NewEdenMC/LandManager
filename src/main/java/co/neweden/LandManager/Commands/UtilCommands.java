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
        LandManager.getPlugin().getCommand("rtp").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }
        Player player = (Player) sender;

        try {
            switch (command.getName().toLowerCase()) {
                case "rtp": rtpCommand(player); break;
            }
        } catch (CommandException e) {
            sender.sendMessage(Util.formatString(e.getMessage()));
        }

        return true;
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
                x = rand.nextInt(); z = rand.nextInt();
            } else {
                x = rand.nextInt(worldRadius); z = rand.nextInt(worldRadius);
            }
            Location loc = new Location(player.getWorld(), x, player.getWorld().getHighestBlockYAt(x, z), z);
            Material belowBlock = new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ()).getBlock().getType();
            if (
                    !belowBlock.equals(Material.WATER) &&
                    !belowBlock.equals(Material.STATIONARY_WATER) &&
                    !belowBlock.equals(Material.LAVA) &&
                    !belowBlock.equals(Material.STATIONARY_LAVA) &&
                    LandManager.getLandClaim(loc.getChunk()) == null)
            {
                player.teleport(loc);
                return;
            } else
                passes++;
        }
        player.sendMessage(Util.formatString("&cTried to teleport you to a random location that isn't on water, lava or a land claim but after trying " + passLimit + " I was unable to find a location to send you to, contact a staff member who can help you find some empty land, please also report this issue."));
    }

}
