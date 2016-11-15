package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class LandCommands implements CommandExecutor {

    public LandCommands() {
        LandManager.getPlugin().getCommand("claim").setExecutor(this);
        LandManager.getPlugin().getCommand("newclaim").setExecutor(this);
        LandManager.getPlugin().getCommand("linfo").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        Player player = (Player) sender;
        boolean end = true;

        switch (commandLabel.toLowerCase()) {
            case "claim": claimCommand(player, args); break;
            case "newclaim": newClaimCommand(player, args); break;
            case "linfo": infoCommand(player); break;
            default: end = false;
        }

        if (end) return true; // to prevent the below code from executing after any of the above commands have executed

        if (!LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            player.sendMessage(Util.formatString("&cThis chunk is not claimed, this command can only be used on claimed land."));
            return true;
        }

        switch (commandLabel.toLowerCase()) {
            // for later
        }

        return true;
    }

    private void claimCommand(Player player, String[] args) {
        if (LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            player.sendMessage(Util.formatString("&cYou can't claim this chunk as it has already been claimed."));
            return;
        }

        // Get claims that are directly next to this chunk
        // Proper access checks performed in the getAdjacentClaims methods
        Collection<LandClaim> nearClaims = LandManager.getAdjacentClaims(player.getLocation().getChunk(), player.getUniqueId());
        if (nearClaims == null) {
            player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to identify land, please contact a staff member."));
            return;
        }

        // if command is run with an argument (which should be the Land ID that the player selected) do this
        if (args.length == 1) {
            int landIDfromArgs;
            try {
                landIDfromArgs = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Util.formatString("&cWrong input format for first argument, expected integer, got: " + args[0]));
                return;
            }
            for (LandClaim land : nearClaims) {
                if (land.getID() != landIDfromArgs) continue;
                if (land.claimChunk(player.getLocation().getChunk())) // access check already performed
                    player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                else
                    player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
                return;
            }
            player.sendMessage(Util.formatString("&cLand ID provided is not an an adjacent land claim"));
            return;
        }

        // If there are no claims nearby do this
        if (nearClaims.size() == 0) {
            createNewClaim(player, null);
            return;
        }

        // If there is just one claim nearby do this
        if (nearClaims.size() == 1) {
            LandClaim land = nearClaims.iterator().next();
            if (land.claimChunk(player.getLocation().getChunk())) // access check already performed
                player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
            else
                player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
            return;
        }

        // If there are multiple claims nearby do this
        player.sendMessage(Util.formatString("Multiple adjacent Land Claims found, select which claim you would like to add this chunk to:"));
        for (LandClaim land : nearClaims) {
            player.spigot().sendMessage(new ComponentBuilder(land.getDisplayName()).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim " + land.getID())).create());
        }
        player.spigot().sendMessage(new ComponentBuilder("Or click here to create a new Land Claim").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/newclaim")).create());

    }

    private void newClaimCommand(Player player, String[] args) {
        if (!LandManager.isChunkClaimed(player.getLocation().getChunk()))
            createNewClaim(player, args);
        else
            player.sendMessage(Util.formatString("&cYou can't claim this chunk as it has already been claimed."));
    }

    private void createNewClaim(Player player, String[] args) {
        String name = null;
        if (args != null) {
            name = "";
            for (int i = 0; i < args.length; i++) {
                name += args[i] + " ";
            }
            name = name.substring(0, name.length() - 1);
        }

        LandClaim claim = LandManager.createClaim(player.getUniqueId(), player.getLocation());
        if (claim == null) {
            player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to create a new Land Claim, please contact a staff member."));
            return;
        }
        player.sendMessage(name);
        claim.setDisplayName(name);
        claim.claimChunk(player.getLocation().getChunk());
        player.sendMessage(Util.formatString("&aThis chunk has been claimed and a new land claim was setup with the name: &e" + claim.getDisplayName()));
    }

    private void infoCommand(Player player) {
        String status = "&7Not Claimed";
        String name = "";
        String owner = "";
        String acl = "- &7EVERYONE (FULL_ACCESS)";

        if (LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            LandClaim land = LandManager.getLandClaim(player.getLocation().getChunk());

            if (!land.testAccessLevel(player.getUniqueId(), ACL.Level.VIEW) && !player.hasPermission("landmanager.linfo.any")) {
                player.sendMessage(Util.formatString("&cThis land is claimed but you do not have permission to view the Land Info.")); return;
            }

            status = "&aClaimed";
            name = land.getDisplayName();
            owner = Bukkit.getOfflinePlayer(land.getOwner()).getName();
            acl = "";
            for (Map.Entry<UUID, ACL.Level> entry : land.getACL().entrySet()) {
                if (entry.getKey() != null)
                    acl += "- " + Bukkit.getOfflinePlayer(entry.getKey()).getName() + " (" + entry.getValue() + ")\n";
                else
                    acl += "- &7EVERYONE (" + entry.getValue() + ")&r\n";
            }
        }

        player.sendMessage(Util.formatString(
                "Chunk status: " + status + "&r\n" +
                "Land Name: " + name + "&r\n" +
                "Land owned by: " + owner + "&r\n\n" +
                "Access Control List:\n" + acl
        ));
    }

}
