package co.neweden.LandManager.Commands;

import co.neweden.LandManager.*;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import co.neweden.LandManager.Exceptions.UnclaimChunkException;
import co.neweden.LandManager.Exceptions.UserException;
import co.neweden.LandManager.Listeners.MenuEvents;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class LandCommands implements CommandExecutor {

    public LandCommands() {
        LandManager.getPlugin().getCommand("claim").setExecutor(this);
        LandManager.getPlugin().getCommand("newclaim").setExecutor(this);
        LandManager.getPlugin().getCommand("unclaim").setExecutor(this);
        LandManager.getPlugin().getCommand("linfo").setExecutor(this);
        LandManager.getPlugin().getCommand("ltransfer").setExecutor(this);
        LandManager.getPlugin().getCommand("laccess").setExecutor(this);
        LandManager.getPlugin().getCommand("lpublic").setExecutor(this);
        LandManager.getPlugin().getCommand("lprivate").setExecutor(this);
        LandManager.getPlugin().getCommand("lrename").setExecutor(this);
        LandManager.getPlugin().getCommand("lseticon").setExecutor(this);
        LandManager.getPlugin().getCommand("lsethomeblock").setExecutor(this);
        LandManager.getPlugin().getCommand("listland").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        Player player = (Player) sender;
        boolean end = true;

        try {

            switch (command.getName().toLowerCase()) {
                case "claim": claimCommand(player, args); break;
                case "newclaim": newClaimCommand(player, args); break;
                case "linfo": infoCommand(player); break;
                case "listland": listCommand(commandLabel, player, args); break;
                default: end = false;
            }

            if (end)
                return true; // to prevent the below code from executing after any of the above commands have executed

            if (!LandManager.isChunkClaimed(player.getLocation().getChunk())) {
                player.sendMessage(Util.formatString("&cThis chunk is not claimed, this command can only be used on claimed land."));
                return true;
            }

            LandClaim land = LandManager.getLandClaim(player.getLocation().getChunk());

            switch (command.getName().toLowerCase()) {
                case "unclaim": unClaimCommand(land, player); break;
                case "ltransfer": transferCommand(land, player, args); break;
                case "laccess": ACLCommandHandlers.accessCommand(land, "Land Claim", player, args, "landmanager.laccess.any"); break;
                case "lpublic": ACLCommandHandlers.publicCommand(land, "Land Claim", player, args, "landmanager.lpublic.any"); break;
                case "lprivate": ACLCommandHandlers.privateCommand(land, "Land Claim", player, "landmanager.lprivate.any"); break;
                case "lrename": renameCommand(land, player, args); break;
                case "lseticon": setIconCommand(land, player, args); break;
                case "lsethomeblock": setHomeBlockCommand(land, player); break;
            }

        } catch (CommandException e) {
            sender.sendMessage(Util.formatString(e.getMessage()));
        }

        return true;
    }

    private void claimCommand(Player player, String[] args) {
        if (LandManager.isChunkClaimed(player.getLocation().getChunk()))
            throw new CommandException("&cYou can't claim this chunk as it has already been claimed.");

        // Get claims that are directly next to this chunk
        // Proper access checks performed in the getAdjacentClaims methods
        Collection<LandClaim> nearClaims = LandManager.getAdjacentClaims(player.getLocation().getChunk(), player.getUniqueId());
        if (nearClaims == null)
            throw new CommandException("&cAn internal error has occurred while trying to identify land, please contact a staff member.");

        // if command is run with an argument (which should be the Land ID that the player selected) do this
        if (args.length == 1) {
            int landIDfromArgs;
            try {
                landIDfromArgs = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new CommandException("&cWrong input format for first argument, expected integer, got: " + args[0]);
            }
            for (LandClaim land : nearClaims) {
                if (land.getID() != landIDfromArgs) continue;
                try {
                    if (!land.claimChunk(player.getLocation().getChunk())) // access check already performed
                        throw new CommandException("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member.");

                    player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                    return;
                } catch (RestrictedWorldException e) {
                    throw new CommandException("&c" + e.getUserMessage());
                }
            }
            throw new CommandException("&cLand ID provided is not an an adjacent land claim");
        }

        // If there are no claims nearby do this
        if (nearClaims.size() == 0) {
            createNewClaim(player, null);
            return;
        }

        // If there is just one claim nearby do this
        if (nearClaims.size() == 1) {
            LandClaim land = nearClaims.iterator().next();
            try {
                if (!land.claimChunk(player.getLocation().getChunk())) // access check already performed
                    throw new CommandException("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member.");

                player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                return;
            } catch (RestrictedWorldException e) {
                throw new CommandException("&c" + e.getUserMessage());
            }
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
            if (args.length > 0) {
                name = "";
                for (int i = 0; i < args.length; i++) {
                    name += args[i] + " ";
                }
                if (!name.isEmpty())
                    name = name.substring(0, name.length() - 1);
            }
        }

        try {
            LandClaim claim = LandManager.createClaim(player.getUniqueId(), player.getLocation());
            if (claim == null)
                throw new CommandException("&cAn internal error has occurred while trying to create a new Land Claim, please contact a staff member.");

            claim.setDisplayName(name);
            claim.claimChunk(player.getLocation().getChunk());
            player.sendMessage(Util.formatString("&aThis chunk has been claimed and a new land claim was setup with the name: &e" + claim.getDisplayName()));
        } catch (UserException e) {
            throw new CommandException("&c" + e.getUserMessage());
        }
    }

    private void unClaimCommand(LandClaim land, Player player) {
        if (!land.testAccessLevel(player, ACL.Level.FULL_ACCESS, "landmanager.unclaim.any"))
            throw new CommandException("&cYou do not have permission to unclaim this chunk.");

        String message = "&aChunk successfully un-claimed";

        try {
            if (!land.unclaimChunk(player.getLocation().getChunk()))
                throw new CommandException("&cAn internal error has occurred while trying to un-claim this chunk, please contact a staff member.");

        } catch (UnclaimChunkException e) {

            if (!e.getReason().equals(UnclaimChunkException.Reason.LAST_CHUNK))
                throw new CommandException("&c" + e.getUserMessage());

            if (!LandManager.deleteClaim(land))
                throw new CommandException("&cChunk was un-claimed and as it was the last chunk in the Land Claim, the Claim should have been deleted however an internal error occurred, please contact a staff members.");

            message += "&e as this was the last chunk in the land claim the land claim has been deleted";
        }

        player.sendMessage(Util.formatString(message));
    }

    private void infoCommand(Player player) {
        String status = "&7Not Claimed";
        String name = "";
        String owner = "";
        ACLSet acl = null;

        if (LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            LandClaim land = LandManager.getLandClaim(player.getLocation().getChunk());

            if (!land.testAccessLevel(player, ACL.Level.VIEW, "landmanager.linfo.any"))
                throw new CommandException("&cThis land is claimed but you do not have permission to view the Land Info.");

            status = "&aClaimed";
            name = land.getDisplayName();
            owner = Bukkit.getOfflinePlayer(land.getOwner()).getName();
            acl = land.getACL();
        }

        player.sendMessage(Util.formatString(
                "Chunk status: " + status + "&r\n" +
                "Land Name: " + name + "&r\n" +
                "Land owned by: " + owner + "&r\n\n" +
                "Access Control List:\n" + ACLCommandHandlers.renderACL(acl)
        ));
    }

    private void transferCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.FULL_ACCESS, "landmanager.ltransfer.any"))
            throw new CommandException("&cYou do not have permission to transfer this Land to another player.");

        if (args.length == 0)
            throw new CommandException("&cYou did not specify a player to transfer this land to.");

        OfflinePlayer toPlayer = Util.getOfflinePlayer(args[0]);

        if (toPlayer == null)
            throw new CommandException("&cPlayer \"" + args[0] + "\" not found.");

        try {
            if (!land.setAccess(land.getOwner(), ACL.Level.MODIFY) || !land.setOwner(toPlayer.getUniqueId()))
                throw new CommandException("&cAn internal error occurred while trying to transfer ownership of the Land, please contact a staff member.");
        } catch (UserException e) { player.sendMessage(Util.formatString("&c" + e.getUserMessage())); }

        player.sendMessage(Util.formatString("&aYou have successfully transferred ownership of Land &e" + land.getDisplayName() + "&a to &e" + toPlayer.getName()));
    }

    private void renameCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lrename.any"))
            throw new CommandException("&cYou do not have permission to rename this Land.");

        if (args.length == 0)
            throw new CommandException("&cYou did not specify a name for this Land, use \"/lrename clear\" to reset the name to the default.");

        String name = null;
        String success = "&aLand name cleared and reset to the default.";
        if (!args[0].equalsIgnoreCase("clear")) {
            name = "";
            for (int i = 0; i < args.length; i++) {
                name += args[i] + " ";
            }
            name = name.substring(0, name.length() - 1);
            success = "&aLand name has been changed to &e" + name;
        }

        if (land.setDisplayName(name))
            player.sendMessage(Util.formatString(success));
        else
            throw new CommandException("&cAn internal error occurred while trying to set the name for this Land, please contact a staff member.");
    }

    private void setIconCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lseticon.any"))
            throw new CommandException("&cYou do not have permission to set the icon for this Land.");

        if (args.length == 0) {
            player.sendMessage(Util.formatString("&cYou did not specify a Material for this Land's Icon, use \"/licon clear\" to reset the icon to the default.")); return;
        }

        Material material = null;
        String success = "&aLand Icon cleared and reset to the default.";
        if (!args[0].equalsIgnoreCase("clear")) {
            try {
                material = Material.valueOf(args[0].toUpperCase());
                success = "&aLand Icon has been set to the Material &e" + material;
            } catch (IllegalArgumentException e) {
                throw new CommandException("&cMaterial \"" + args[0] + "\" is not a valid Minecraft Material Name, \"/itemdb\" might help you.");
            }
        }

        if (land.setIconMaterial(material))
            player.sendMessage(Util.formatString(success));
        else
            throw new CommandException("&cAn internal error occurred while trying to set the Icon for this Land, please contact a staff member.");
    }

    private void setHomeBlockCommand(LandClaim land, Player player) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lsethomeblock.any"))
            throw new CommandException("&cYou do not have permission to set the Home Block for this Land.");

        if (land.setHomeLocation(player.getLocation()))
            player.sendMessage(Util.formatString("&aThe Home Block for this Land Claim has been changed to the location you are stinging in, now when using /myland players will be taken here instead of the old location."));
        else
            throw new CommandException("&cAn internal error occurred while trying to set the Home Block for this Land, please contact a staff member.");
    }

    private void listCommand(String commandLabel, Player player, String[] args) {
        OfflinePlayer forPlayer = player;
        if (player.hasPermission("landmanager.listland.others") && args.length > 0 && commandLabel.equalsIgnoreCase("listland")) {
            forPlayer = Util.getOfflinePlayer(args[0]);
            if (forPlayer == null)
                throw new CommandException("&cPlayer \"" + args[0] + "\" not found");
        }
        MenuEvents.landListMenuSubjects.put(player.getUniqueId(), forPlayer);
        LandManager.getLandListMenu().openMenu(player);
    }

}
