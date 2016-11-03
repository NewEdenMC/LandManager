package co.neweden.LandManager.commands;

import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LandCommands implements CommandExecutor {

    public LandCommands() {
        LandManager.getPlugin().getCommand("claim").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        switch (commandLabel.toLowerCase()) {
            case "claim": claimCommand(sender); break;
        }

        return true;
    }

    private void claimCommand(CommandSender sender) {
        Player player = (Player) sender;

        if (LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            sender.sendMessage(Util.formatString("&cYou can't claim this chunk as it has already been claimed."));
            return;
        }

        LandClaim claim = LandManager.createClaim(player.getUniqueId(), player.getLocation());

        if (claim == null) {
            sender.sendMessage(Util.formatString("&cAn internal error has occurred while trying to create a new Land Claim, please contact a staff member."));
            return;
        }

        claim.claimChunk(player.getLocation().getChunk());

        sender.sendMessage(Util.formatString("&aThis chunk has been claimed and a new land claim was setup"));
    }

}
