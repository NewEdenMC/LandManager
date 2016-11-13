package co.neweden.LandManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public abstract class ACL {

    protected Map<UUID, Level> list = new LinkedHashMap<>();

    public enum Level { NO_ACCESS, VIEW, INTERACT, MODIFY, FULL_ACCESS }

    public Map<UUID, Level> getACL() {
        Map<UUID, Level> acl = new LinkedHashMap<>();
        acl.put(getOwner(), Level.FULL_ACCESS);
        acl.putAll(list);
        acl.put(null, getEveryoneAccessLevel());
        return acl;
    }

    public abstract UUID getOwner();

    public abstract Level getEveryoneAccessLevel();

    public abstract boolean setAccess(UUID uuid, Level level);

}
