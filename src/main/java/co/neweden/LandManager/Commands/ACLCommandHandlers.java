package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ACLCommandHandlers {

    protected static String renderACL(ACL acl) {
        String render = "";
        for (Map.Entry<UUID, ACL.Level> entry : acl.getACL().entrySet()) {
            if (entry.getKey() != null)
                render += "- " + Bukkit.getOfflinePlayer(entry.getKey()).getName() + " (" + entry.getValue() + ")\n";
            else
                render += "- &7EVERYONE (" + entry.getValue() + ")&r\n";
        }
        return render;
    }

    protected static void addCommand(ACL acl, Player player, String[] args, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission to add a player to the Access List.");

        if (args.length == 0)
            throw new CommandException("&cYou did not specify a player to add to the Access List.");

        OfflinePlayer addPlayer = Util.getOfflinePlayer(args[0]);

        if (addPlayer == null)
            throw new CommandException("&cPlayer \"" + args[0] + "\" not found.");

        ACL.Level level = ACL.Level.MODIFY;
        if (args.length >= 2) {
            level = ACL.Level.valueOf(args[1].toUpperCase());
            if (!level.equals(ACL.Level.INTERACT) && !level.equals(ACL.Level.MODIFY))
                throw new CommandException("&cThe ACL Level \"" + args[1] + "\" is not valid or is not allowed, allowed levels are: INTERACT, MODIFY");
        }

        if (acl.setAccess(addPlayer.getUniqueId(), level))
            player.sendMessage(Util.formatString("&aPlayer &e" + addPlayer.getName() + "&a added to the Access List with access level &e" + level));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL, please contact a staff member.");
    }

    protected static void removeCommand(ACL acl, Player player, String[] args, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission to remove a player from the Access List.");

        if (args.length == 0)
            throw new CommandException("&cYou did not specify a player to remove from the Access List.");

        OfflinePlayer removePlayer = Util.getOfflinePlayer(args[0]);

        if (removePlayer == null)
            throw new CommandException("&cPlayer \"" + args[0] + "\" not found.");

        if (acl.getACL().entrySet().stream().filter(e -> removePlayer.getUniqueId().equals(e.getKey())).count() == 0)
            throw new CommandException("&cPlayer " + removePlayer.getName() + " cannot be removed as they are not on the Access List.");

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
            player.sendMessage(Util.formatString("&a" + typeName + " set to public, access level for EVERYONE set to &e" + level));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member.");
    }

    protected static void privateCommand(ACL acl, String typeName, Player player, String bypassPerm) {
        if (!acl.testAccessLevel(player, ACL.Level.MODIFY, bypassPerm))
            throw new CommandException("&cYou do not have permission to set this " + typeName + " as private.");

        if (acl.setEveryoneAccessLevel(null))
            player.sendMessage(Util.formatString("&a" + typeName + " set to private, access level for EVERYONE has been reset to default."));
        else
            throw new CommandException("&cAn internal error occurred while trying to update the ACL level for EVERYONE, please contact a staff member.");
    }

}
