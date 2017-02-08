package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.ACLEntry;
import co.neweden.LandManager.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

public class ACLCommandHandlers {

    protected static String renderACL(ACL acl) {
        String render = "";
        for (ACLEntry entry : acl.getACL()) {
            if (entry.uuid != null)
                render += "- " + Bukkit.getOfflinePlayer(entry.uuid).getName() + " (" + entry.level + ")";
            else
                render += "- &7EVERYONE (" + entry.level + ")";
            render += (entry.inherited) ? " &e*&r\n" : "&r\n";
        }
        return render;
    }

    protected static void accessCommand(ACL acl, String typeName, Player player, String[] args, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission modify the Access List.");

        if (args.length == 0)
            throw new CommandException(accessCommandHelp(typeName));

        switch (args[0].toLowerCase()) {
            case "set":
            case "add": setCommand(acl, player, args, bypassPerm); break;
            case "remove": removeCommand(acl, player, args, bypassPerm); break;
            default:
                player.sendMessage(Util.formatString("&cUnknown sub-command " + args[0] + "\n \n"));
                throw new CommandException(accessCommandHelp(typeName));
        }
    }

    private static String accessCommandHelp(String typeName) {
        return Util.formatString(
                "&fThis command allows you to add/change/remove access for this " + typeName + ", available sub-commands are:\n" +
                "&f- &bset NAME [LEVEL]&f: Give or update a player's access to this " + typeName + "\n" +
                "&f- &badd NAME [LEVEL]&f: Exactly the same as &bset&f just another name for it\n" +
                "&f- &bremove NAME&f: Remove a player's access to this " + typeName + "\n \n" +
                "&fIn the above commands &bNAME&f is the name of the player, and &b[LEVEL]&f the level of access to you want to set, the Level is optional"
        );
    }

    protected static void setCommand(ACL acl, Player player, String[] args, String bypassPerm) {
        if (args.length < 2)
            throw new CommandException("&cYou did not specify a player to add to the Access List.");

        OfflinePlayer addPlayer = Util.getOfflinePlayer(args[1]);

        if (addPlayer == null)
            throw new CommandException("&cPlayer \"" + args[1] + "\" not found.");

        ACL.Level level = ACL.Level.MODIFY;
        if (args.length >= 3) {
            level = ACL.Level.valueOf(args[2].toUpperCase());
            if (!level.equals(ACL.Level.INTERACT) && !level.equals(ACL.Level.MODIFY))
                throw new CommandException("&cThe ACL Level \"" + args[2] + "\" is not valid or is not allowed, allowed levels are: INTERACT, MODIFY");
        }

        if (acl.setAccess(addPlayer.getUniqueId(), level))
            player.sendMessage(Util.formatString("&aPlayer &e" + addPlayer.getName() + "&a added to the Access List with access level &e" + level));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL, please contact a staff member.");
    }

    protected static void removeCommand(ACL acl, Player player, String[] args, String bypassPerm) {
        if (args.length < 2)
            throw new CommandException("&cYou did not specify a player to remove from the Access List.");

        OfflinePlayer removePlayer = Util.getOfflinePlayer(args[1]);

        if (removePlayer == null)
            throw new CommandException("&cPlayer \"" + args[1] + "\" not found.");

        ACLEntry entry = acl.getAccessLevel(removePlayer.getUniqueId());
        if (entry.uuid == null)
            throw new CommandException("&cPlayer " + removePlayer.getName() + " cannot be removed as they are not on the Access List.");

        if (entry.inherited)
            throw new CommandException("&cPlayer " + removePlayer.getName() + " cannot be removed as their access is inherited from the parent ACL (probably a Land Claim), you can either remove them from the parent ACL or set their access to " + acl.getEveryoneAccessLevel());

        if (acl.setAccess(removePlayer.getUniqueId(), null))
            player.sendMessage(Util.formatString("&aPlayer &e" + removePlayer.getName() + "&a remove from the Access List."));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL, please contact a staff member.");
    }

    protected static void publicCommand(ACL acl, String typeName, Player player, String[] args, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission to set this " + typeName + " as public.");

        ACL.Level level = ACL.Level.INTERACT;
        if (args.length >= 1) {
            level = ACL.Level.valueOf(args[0].toUpperCase());
            if (!level.equals(ACL.Level.VIEW) && !level.equals(ACL.Level.INTERACT))
                throw new CommandException("&cThe ACL Level \"" + args[0] + "\" is not valid or is not allowed, allowed levels are: VIEW, INTERACT");
        }

        if (acl.setEveryoneAccessLevel(level))
            player.sendMessage(Util.formatString("&a" + typeName + " set to public, access level for &eEVERYONE&a set to &e" + level));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member.");
    }

    protected static void privateCommand(ACL acl, String typeName, Player player, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission to set this " + typeName + " as private.");

        if (acl.setEveryoneAccessLevel(null))
            player.sendMessage(Util.formatString("&a" + typeName + " set to private, access level for &eEVERYONE&a has been reset to default."));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member.");
    }

}
