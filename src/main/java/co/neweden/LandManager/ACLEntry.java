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
        // ACLs should be ordered by Level then UUID
        // First we compare the level and return, returning -1 or 1 if they are different, if the Level matches we
        // continue on to compare based on UUID
        int levelVal = another.level.compareTo(this.level);
        if (levelVal != 0) return levelVal;

        // UUIDs could be null, if both are null return 0 as they are the same
        if (uuid == null && another.uuid == null) return 0;
        // if one is null but not the other return either 1 or -1
        if (uuid == null) return 1;
        if (another.uuid == null) return -1;

        // if both are not nul compare both UUIDs and return
        return uuid.compareTo(another.uuid);
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

    @Override
    public String toString() {
        String inherited = (this.inherited) ? " (inherited)" : "";
        return String.format("%s:%s%s,", uuid, level, inherited);
    }

}
