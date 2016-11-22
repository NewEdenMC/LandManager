package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Listeners.MenuEvents;
import co.neweden.LandManager.Util;
import co.neweden.menugui.menu.MenuInstance;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
        LandManager.getPlugin().getCommand("unclaim").setExecutor(this);
        LandManager.getPlugin().getCommand("linfo").setExecutor(this);
        LandManager.getPlugin().getCommand("ltransfer").setExecutor(this);
        LandManager.getPlugin().getCommand("ladd").setExecutor(this);
        LandManager.getPlugin().getCommand("lremove").setExecutor(this);
        LandManager.getPlugin().getCommand("lpublic").setExecutor(this);
        LandManager.getPlugin().getCommand("lprivate").setExecutor(this);
        LandManager.getPlugin().getCommand("lrename").setExecutor(this);
        LandManager.getPlugin().getCommand("licon").setExecutor(this);
        LandManager.getPlugin().getCommand("listland").setExecutor(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        Player player = (Player) sender;
        boolean end = true;

        switch (command.getName().toLowerCase()) {
            case "claim": claimCommand(player, args); break;
            case "newclaim": newClaimCommand(player, args); break;
            case "linfo": infoCommand(player); break;
            case "listland": listCommand(commandLabel, player, args); break;
            default: end = false;
        }

        if (end) return true; // to prevent the below code from executing after any of the above commands have executed

        if (!LandManager.isChunkClaimed(player.getLocation().getChunk())) {
            player.sendMessage(Util.formatString("&cThis chunk is not claimed, this command can only be used on claimed land."));
            return true;
        }

        LandClaim land = LandManager.getLandClaim(player.getLocation().getChunk());

        switch (command.getName().toLowerCase()) {
            case "unclaim": unClaimCommand(land, player); break;
            case "ltransfer": transferCommand(land, player, args); break;
            case "ladd": addCommand(land, player, args); break;
            case "lremove": removeCommand(land, player, args); break;
            case "lpublic": publicCommand(land, player, args); break;
            case "lprivate": privateCommand(land, player); break;
            case "lrename": renameCommand(land, player, args); break;
            case "licon": iconCommand(land, player, args); break;
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
                try {
                    if (land.claimChunk(player.getLocation().getChunk())) // access check already performed
                        player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                    else
                        player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
                    return;
                } catch (RestrictedWorldException e) {
                    player.sendMessage(Util.formatString("&c" + e.getMessage()));
                }
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
            try {
                if (land.claimChunk(player.getLocation().getChunk())) // access check already performed
                    player.sendMessage(Util.formatString("&aThis chunk has been claimed and added to the land claim: &e" + land.getDisplayName()));
                else
                    player.sendMessage(Util.formatString("&cAn internal error has occurred while trying to claim chunk for existing claim, please contact a staff member."));
                return;
            } catch (RestrictedWorldException e) {
                player.sendMessage(Util.formatString("&c" + e.getMessage()));
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
        try {
            claim.claimChunk(player.getLocation().getChunk());
        } catch (RestrictedWorldException e) {
            player.sendMessage(Util.formatString("&c" + e.getMessage()));
        }
        player.sendMessage(Util.formatString("&aThis chunk has been claimed and a new land claim was setup with the name: &e" + claim.getDisplayName()));
    }

    private void unClaimCommand(LandClaim land, Player player) {
        if (!land.testAccessLevel(player, ACL.Level.FULL_ACCESS, "landmanager.unclaim.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to unclaim this chunk.")); return;
        }

        LandClaim.UnClaimResult unclaimResult = land.unclaimChunk(player.getLocation().getChunk());

        String message = "";
        switch (unclaimResult) {
            case FAILED: message = "&cAn internal error has occurred while trying to un-claim this chunk, please contact a staff member."; break;
            case SUCCESS: message = "&aChunk successfully un-claimed"; break;
            case SUCCESS_LAST:
                if (LandManager.deleteClaim(land))
                    message = "&aChunk successfully un-claimed, &eAs this was the last chunk in the land claim the land claim has been deleted";
                else
                    message = "&cChunk was successfully un-claimed, as it was the last chunk in the Land Claim should have been deleted however an internal error occurred, please contact a staff members.";
                break;
        }

        player.sendMessage(Util.formatString(message));
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

    public void transferCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.FULL_ACCESS, "landmanager.ltransfer.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to transfer this Land to another player.")); return;
        }

        if (args.length == 0) {
            player.sendMessage(Util.formatString("&cYou did not specify a player to transfer this land to.")); return;
        }

        OfflinePlayer toPlayer = Util.getOfflinePlayer(args[0]);

        if (toPlayer == null) {
            player.sendMessage(Util.formatString("&cPlayer \"" + args[0] + "\"not found.")); return;
        }

        if (!land.setOwner(toPlayer.getUniqueId()) || !land.setAccess(player.getUniqueId(), ACL.Level.MODIFY))
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to transfer ownership of the Land, please contact a staff member."));

        player.sendMessage(Util.formatString("&aYou have successfully transferred ownership of Land &e" + land.getDisplayName() + "&a to &e" + toPlayer.getName()));
    }

    private void addCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.ladd.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to add another player to this land.")); return;
        }

        if (args.length == 0) {
            player.sendMessage(Util.formatString("&cYou did not specify a player to add to this land.")); return;
        }

        OfflinePlayer addPlayer = Util.getOfflinePlayer(args[0]);

        if (addPlayer == null) {
            player.sendMessage(Util.formatString("&cPlayer \"" + args[0] + "\" not found.")); return;
        }

        ACL.Level level = ACL.Level.MODIFY;
        if (args.length >= 2) {
            try {
                level = ACL.Level.valueOf(args[1].toUpperCase());
                if (!level.equals(ACL.Level.INTERACT) && !level.equals(ACL.Level.MODIFY))
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                player.sendMessage(Util.formatString("&cThe ACL Level \"" + args[1] + "\" is not valid or is not allowed, allowed levels are: INTERACT, MODIFY")); return;
            }
        }

        if (land.setAccess(addPlayer.getUniqueId(), level))
            player.sendMessage(Util.formatString("&aPlayer &e" + addPlayer.getName() + "&a added to Land with access level &e" + level));
        else
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to update the ACL, please contact a staff member."));
    }

    private void removeCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lremove.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to remove another player from this land.")); return;
        }

        if (args.length == 0) {
            player.sendMessage(Util.formatString("&cYou did not specify a player to remove from this land.")); return;
        }

        OfflinePlayer removePlayer = Util.getOfflinePlayer(args[0]);

        if (removePlayer == null) {
            player.sendMessage(Util.formatString("&cPlayer \"" + args[0] + "\" not found.")); return;
        }

        if (land.getACL().entrySet().stream().filter(e -> removePlayer.getUniqueId().equals(e.getKey())).count() == 0) {
            player.sendMessage(Util.formatString("&cPlayer " + removePlayer.getName() + " cannot be removed from this Land as they are not on the Access List.")); return;
        }

        if (land.setAccess(removePlayer.getUniqueId(), null))
            player.sendMessage(Util.formatString("&aPlayer &e" + removePlayer.getName() + "&a remove from Land"));
        else
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to update the ACL, please contact a staff member."));
    }

    private void publicCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lpublic.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to set this land as public.")); return;
        }

        ACL.Level level = ACL.Level.INTERACT;
        if (args.length >= 1) {
            try {
                level = ACL.Level.valueOf(args[0].toUpperCase());
                if (!level.equals(ACL.Level.VIEW) && !level.equals(ACL.Level.INTERACT))
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                player.sendMessage(Util.formatString("&cThe ACL Level \"" + args[0] + "\" is not valid or is not allowed, allowed levels are: VIEW, INTERACT")); return;
            }
        }

        if (land.setEveryoneAccessLevel(level))
            player.sendMessage(Util.formatString("&aLand set to public, access level for EVERYONE set to &e" + level));
        else
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member."));
    }

    private void privateCommand(LandClaim land, Player player) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lprivate.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to set this land as private.")); return;
        }

        if (land.setEveryoneAccessLevel(null))
            player.sendMessage(Util.formatString("&aLand set to private, access level for EVERYONE has been reset to default."));
        else
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member."));
    }

    private void renameCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.lrename.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to rename this Land."));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(Util.formatString("&cYou did not specify a name for this Land, use \"/lrename clear\" to reset the name to the default.")); return;
        }

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
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to set the name for this Land, please contact a staff member."));
    }

    private void iconCommand(LandClaim land, Player player, String[] args) {
        if (!land.testAccessLevel(player, ACL.Level.MODIFY, "landmanager.licon.any")) {
            player.sendMessage(Util.formatString("&cYou do not have permission to set the icon for this Land."));
            return;
        }

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
                player.sendMessage(Util.formatString("&cMaterial \"" + args[0] + "\" is not a valid Minecraft Material Name, \"/itemdb\" might help you."));
                return;
            }
        }

        if (land.setIconMaterial(material))
            player.sendMessage(Util.formatString(success));
        else
            player.sendMessage(Util.formatString("&cAn internal error occurred while trying to set the Icon for this Land, please contact a staff member."));
    }

    private void listCommand(String commandLabel, Player player, String[] args) {
        OfflinePlayer forPlayer = player;
        if (player.hasPermission("landmanager.listland.others") && args.length > 0 && commandLabel.equalsIgnoreCase("listland")) {
            forPlayer = Util.getOfflinePlayer(args[0]);
            if (forPlayer == null) {
                player.sendMessage(Util.formatString("&cPlayer \"" + args[0] + "\" not found")); return;
            }
        }
        MenuEvents.landListMenuSubjects.put(player.getUniqueId(), forPlayer);
        LandManager.getLandListMenu().openMenu(player);
    }

}
