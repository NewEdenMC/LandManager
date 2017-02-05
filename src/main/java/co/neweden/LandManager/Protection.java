package co.neweden.LandManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public abstract class Protection extends ACL {

    private int id;
    protected UUID owner;
    protected Level everyoneAccessLevel;

    public Protection(int id) {
        this.id = id;
        loadACL();
    }

    private void loadACL() {
        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("SELECT uuid,level FROM protections_acl WHERE protection_id=?");
            st.setInt(1, getID());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                try {
                    list.put(UUID.fromString(rs.getString("uuid")), Level.valueOf(rs.getString("level")));
                } catch (IllegalArgumentException e) {
                    LandManager.getPlugin().getLogger().warning("Protection #" + id + ": Invalid data for ACL Entry, UUID or Level are not valid");
                }
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Protection #" + id + ": An SQL Exception occurred while loading Land Claim ACL Data", e);
        }
    }

    private boolean setDBValue(String key, String value) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("UPDATE protections SET " + key + "=? WHERE protection_id=?;");
            st.setString(1, value);
            st.setInt(2, id);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Protection #" + id + ": An SQL Exception occurred while trying to update " + key + " to \"" + value + "\"", e);
            return false;
        }
    }

    public int getID() { return id; }

    public UUID getOwner() { return owner; }

    public boolean setOwner(UUID uuid) {
        if (setDBValue("owner", uuid.toString())) {
            owner = uuid;
            return true;
        } else
            return false;
    }

    public Level getEveryoneAccessLevel() {
        if (everyoneAccessLevel == null)
            return Level.NO_ACCESS;
        else
            return everyoneAccessLevel;
    }

    public boolean setEveryoneAccessLevel(Level level) {
        if (setDBValue("everyone_acl_level", (level != null) ? level.toString() : null)) {
            everyoneAccessLevel = level;
            return true;
        } else
            return false;
    }

    public boolean setAccess(UUID uuid, Level level) {
        try {
            PreparedStatement st;
            if (level == null) {
                st = LandManager.getDB().prepareStatement("DELETE FROM protections_acl WHERE protection_id=? AND uuid=?");
                st.setInt(1, getID());
                st.setString(2, uuid.toString());
                list.remove(uuid);
                st.executeUpdate();
                return true;
            }

            if (list.containsKey(uuid)) {
                st = LandManager.getDB().prepareStatement("UPDATE protections_acl SET level=? WHERE protection_id=? AND uuid=?");
                st.setString(1, level.toString());
                st.setInt(2, getID());
                st.setString(3, uuid.toString());
            } else {
                st = LandManager.getDB().prepareStatement("INSERT INTO protections_acl (protection_id, uuid, level) VALUES (?, ?, ?);");
                st.setInt(1, getID());
                st.setString(2, uuid.toString());
                st.setString(3, level.toString());
            }
            st.executeUpdate();
            list.put(uuid, level);
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Protection #" + id + ": An SQL Exception occurred while trying to set access level for UUID \"" + uuid.toString() + "\" ", e);
            return false;
        }
        return true;
    }

}
