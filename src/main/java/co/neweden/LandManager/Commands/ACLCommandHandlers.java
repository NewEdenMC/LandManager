package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import org.bukkit.Bukkit;

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

}
