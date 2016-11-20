package co.neweden.LandManager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class ACL {

    protected Map<UUID, Level> list = new HashMap<>();

    public enum Level { NO_ACCESS, VIEW, INTERACT, MODIFY, FULL_ACCESS }

    public boolean testAccessLevel(UUID uuid, Level needed) { return testAccessLevel(getAccessLevel(uuid), needed); }

    public boolean testAccessLevel(Player player, Level needed, String bypassPermission) {
        if (testAccessLevel(player.getUniqueId(), needed) || player.hasPermission(bypassPermission))
            return true;
        else
            return false;
    }

    public static boolean testAccessLevel(Level hasLevel, Level needed) {
        // Convert "hasLevel" to integers for comparison
        int hasNum = 0;
        if (hasLevel.equals(Level.VIEW)) hasNum = 1;
        if (hasLevel.equals(Level.INTERACT)) hasNum = 2;
        if (hasLevel.equals(Level.MODIFY)) hasNum = 3;
        if (hasLevel.equals(Level.FULL_ACCESS)) hasNum = 4;

        // check the minimum access level required, and pass if "needed" equals or is greater than "hasLevel"
        switch (needed) {
            case FULL_ACCESS: return (hasNum >= 4);
            case MODIFY: return (hasNum >= 3);
            case INTERACT: return (hasNum >= 2);
            case VIEW: return (hasNum >= 1);
            case NO_ACCESS: return true;
            default: return false;
        }
    }

    public Map<UUID, Level> getACL() {
        Map<UUID, Level> acl = new HashMap<>();
        acl.putAll(list);
        acl.put(getOwner(), Level.FULL_ACCESS);
        acl.put(null, getEveryoneAccessLevel());
        return acl;
    }

    public Level getAccessLevel(UUID uuid) {
        return (getACL().containsKey(uuid)) ? getACL().get(uuid) : getEveryoneAccessLevel();
    }

    public abstract UUID getOwner();

    public abstract Level getEveryoneAccessLevel();

    public abstract boolean setAccess(UUID uuid, Level level);

}
