package co.neweden.LandManager;

import java.util.UUID;

public class ACLEntry implements Comparable<ACLEntry> {

    public final UUID uuid;
    public final ACL.Level level;
    public final boolean inherited;

    ACLEntry(UUID uuid, ACL.Level level, boolean inherited) {
        this.uuid = uuid; this.level = level; this.inherited = inherited;
    }

    @Override
    public int compareTo(ACLEntry another) {
        return another.level.compareTo(level);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof ACLEntry)) return false;
        ACLEntry e = (ACLEntry) obj;
        if (uuid != null && e.uuid != null) {
            if (!uuid.equals(e.uuid)) return false;
        } else return false;
        return level == e.level && inherited == e.inherited;
    }

    public boolean equalsUUID(UUID other) {
        if (uuid == null && other == null)
            return true; // if both are null they are equal
        if (uuid == null || other == null)
            return false; // if one is null but the other isn't they are not equal
        return uuid.equals(other); // neither are null, refer to built in method to provide answer
    }

}
