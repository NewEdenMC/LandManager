package co.neweden.LandManager.Commands;

import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

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
            case "claim": claimCommand(sender, args); break;
        }

        return true;
    }

    private void claimCommand(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            sender.sendMessage(Util.formatString("&cYou can't claim this chunk as it has already been claimed."));
            return;
        }

        Collection<LandClaim> nearClaims = LandManager.getAdjacentClaims(player.getLocation().getChunk(), player.getUniqueId());
        if (nearClaims == null) {
            player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to identify land, please contact a staff member."));
            return;
        }

        if (args.length == 1) {
            int landIDfromArgs;
            try {
                landIDfromArgs = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Util.formatString("&cWrong input format for first argument, expected integer, got: " + args[0]));
                return;
            }
            for (LandClaim land : nearClaims) {
                if (land.getID() != landIDfromArgs) continue;
                if (land.claimChunk(player.getLocation().getChunk()))
                    sender.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                else
                    sender.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
                return;
            }
            sender.sendMessage(Util.formatString("&cLand ID provided is not an an adjacent land claim"));
            return;
        }

        if (nearClaims.size() == 0) {
            LandClaim claim = LandManager.createClaim(player.getUniqueId(), player.getLocation());
            if (claim == null) {
                sender.sendMessage(Util.formatString("&cAn internal error has occurred while trying to create a new Land Claim, please contact a staff member."));
                return;
            }
            claim.claimChunk(player.getLocation().getChunk());
            sender.sendMessage(Util.formatString("&aThis chunk has been claimed and a new land claim was setup"));
            return;
        }

        if (nearClaims.size() == 1) {
            LandClaim land = nearClaims.iterator().next();
            if (land.claimChunk(player.getLocation().getChunk()))
                sender.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
            else
                sender.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
            return;
        }

        sender.sendMessage(Util.formatString("Select which claim you would like to add this chunk to"));
        for (LandClaim land : nearClaims) {
            player.spigot().sendMessage(new ComponentBuilder(land.getDisplayName()).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim " + land.getID())).create());
        }

    }

}
