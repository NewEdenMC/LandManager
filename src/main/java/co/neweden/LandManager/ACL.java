package co.neweden.LandManager;

import org.bukkit.entity.HumanEntity;

import java.util.*;

public abstract class ACL {

    protected ACLSet list = new ACLSet();

    public enum Level { NO_ACCESS, ENTER, VIEW, INTERACT, MODIFY, FULL_ACCESS }

    public boolean testAccessLevel(UUID uuid, Level needed) { return testAccessLevel(getAccessLevel(uuid).level, needed); }

    public boolean testAccessLevel(HumanEntity human, Level needed, Collection<String> bypassPermissions) {
        boolean hasall = true;
        for (String p : bypassPermissions) {
            if (!human.hasPermission(p)) hasall = false;
        }
        return (testAccessLevel(human.getUniqueId(), needed) || hasall);
    }

    public boolean testAccessLevel(HumanEntity human, Level needed, String bypassPermission) {
        return (testAccessLevel(human.getUniqueId(), needed) || human.hasPermission(bypassPermission));
    }

    public static boolean testAccessLevel(Level hasLevel, Level needed) {
        // Convert "hasLevel" to integers for comparison
        int hasNum = 0;
        if (hasLevel.equals(Level.ENTER)) hasNum = 1;
        if (hasLevel.equals(Level.VIEW)) hasNum = 2;
        if (hasLevel.equals(Level.INTERACT)) hasNum = 3;
        if (hasLevel.equals(Level.MODIFY)) hasNum = 4;
        if (hasLevel.equals(Level.FULL_ACCESS)) hasNum = 5;

        // check the minimum access level required, and pass if "needed" equals or is greater than "hasLevel"
        switch (needed) {
            case FULL_ACCESS: return (hasNum >= 5);
            case MODIFY: return (hasNum >= 4);
            case INTERACT: return (hasNum >= 3);
            case VIEW: return (hasNum >= 2);
            case ENTER: return (hasNum >= 1);
            case NO_ACCESS: return true;
            default: return false;
        }
    }

    /*
     Overridden in FallbackACL class
     */
    public ACLSet getACL() {
        ACLSet acl = new ACLSet();
        if (getParentACL() != null) {
            // we add ACL entries from the parent ACL first as any entries for this ACL should override the parent
            getParentACL().getACL().forEach(e -> {
                // anyone with FULL_ACCESS on parent ACL should only have MODIFY on this one
                Level level = (e.level == Level.FULL_ACCESS) ? Level.MODIFY : e.level;
                acl.add(new ACLEntry(e.uuid, level, true));
            });
        }
        // now we add all ACL entries from this ACL, if a UUID has a value in the parent ACL and a value in this ACL
        // they will now be overridden so this ACL takes priority over the parent
        acl.addAll(list);
        // force the owner of this ACL to have full access
        acl.add(new ACLEntry(getOwner(), Level.FULL_ACCESS, false));
        acl.add(new ACLEntry(null, getEveryoneAccessLevel(), false));
        return acl;
    }

    public abstract ACL getParentACL();

    /*
     Gets the ACL Entry for a given UUID, if the UUID is on the access list a complete ACLEntry
     object will be returned, otherwise an ACLEntry object will be returned with a null UUID and
     the access level for everyone obtained through ACL.getEveryoneAccessLevel()
     */
    public ACLEntry getAccessLevel(UUID uuid) {
        if (uuid == null) return new ACLEntry(null, getEveryoneAccessLevel(), false);
        Optional<ACLEntry> opt = getACL().stream().filter(e -> uuid.equals(e.uuid)).findFirst();
        return (opt.isPresent()) ? opt.get() : new ACLEntry(null, getEveryoneAccessLevel(), false);
    }

    public abstract UUID getOwner();

    public abstract Level getEveryoneAccessLevel();

    public abstract boolean setEveryoneAccessLevel(Level level);

    public abstract boolean setAccess(UUID uuid, Level level);

}
