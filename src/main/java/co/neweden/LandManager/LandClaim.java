package co.neweden.LandManager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class LandClaim extends ACL {

    private int id;
    protected String displayName;
    protected UUID owner;
    protected Level everyoneAccessLevel;
    protected Location homeLocation;
    protected Material iconMaterial = Material.GRASS;

    public LandClaim(int id) {
        this.id = id;
        loadACL();
    }

    private void loadACL() {
        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("SELECT uuid,level FROM landclaims_acl WHERE land_id=?");
            st.setInt(1, getID());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                try {
                    list.put(UUID.fromString(rs.getString("uuid")), Level.valueOf(rs.getString("level")));
                } catch (IllegalArgumentException e) {
                    LandManager.getPlugin().getLogger().warning("Land Claim #" + id + ": Invalid data for ACL Entry, UUID or Level are not valid");
                }
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while loading Land Claim ACL Data", e);
        }
    }

    private boolean setDBValue(String key, String value) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("UPDATE landclaims SET " + key + "=? WHERE land_id=?;");
            st.setString(1, value);
            st.setInt(2, id);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while trying to update " + key + " to \"" + value + "\"", e);
            return false;
        }
    }

    public String getDisplayName() {
        if (displayName == null)
            return "Land Claim #" + id;
        else
            return displayName;
    }

    public boolean setDisplayName(String name) {
        if (setDBValue("displayName", name)) {
            displayName = name;
            return true;
        } else
            return false;
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
            return Level.VIEW;
        else
            return everyoneAccessLevel;
    }

    public boolean setEveryoneAccessLevel(Level level) {
        if (setDBValue("everyone_acl_level", level.toString())) {
            everyoneAccessLevel = level;
            return true;
        } else
            return false;
    }

    public Location getHomeLocation() { return homeLocation; }

    public boolean setHomeLocation(Location homeLocation) {
        if (setDBValue("home_location", Util.locationToString(homeLocation))) {
            this.homeLocation = homeLocation;
            return true;
        } else
            return false;
    }

    public Material getIconMaterial() { return iconMaterial; }

    public boolean setIconMaterial(Material material) {
        if (setDBValue("icon_material", material.toString())) {
            iconMaterial = material;
            return true;
        } else
            return false;
    }

    public boolean claimChunk(Chunk chunk) {
        if (LandManager.isChunkClaimed(chunk))
            return false;

        try {
            PreparedStatement st = LandManager.db.prepareStatement("INSERT INTO `chunks` (`land_id`, `world`, `x`, `z`) VALUES (?, ?, ?, ?);");
            st.setInt(1, id);
            st.setString(2, chunk.getWorld().getName());
            st.setInt(3, chunk.getX());
            st.setInt(4, chunk.getZ());
            st.executeUpdate();
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "An SQL Exception occurred while trying to claim chunk \"" + chunk + "\" for land claim " + id + " (\"" + displayName + "\").", e);
            return false;
        }

        return true;
    }

    public enum UnClaimResult { SUCCESS, SUCCESS_LAST, FAILED }

    public UnClaimResult unclaimChunk(Chunk chunk) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("DELETE FROM chunks WHERE land_id=? AND x=? AND z=?;");
            st.setInt(1, id);
            st.setInt(2, chunk.getX());
            st.setInt(3, chunk.getZ());
            st.executeUpdate();

            st = LandManager.db.prepareStatement("SELECT chunk_id FROM chunks WHERE land_id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if (rs.isBeforeFirst())
                return UnClaimResult.SUCCESS;
            else
                return UnClaimResult.SUCCESS_LAST;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "An SQL Exception occurred while trying to un-claim chunk \"" + chunk + "\" for land claim " + id + " (\"" + displayName + "\").", e);
            return UnClaimResult.FAILED;
        }
    }

    public boolean setAccess(UUID uuid, Level level) {
        try {
            PreparedStatement st;
            if (level == null) {
                st = LandManager.getDB().prepareStatement("DELETE FROM landclaims_acl WHERE land_id=? AND uuid=?");
                st.setInt(1, getID());
                st.setString(2, uuid.toString());
                list.remove(uuid);
                return true;
            }

            if (list.containsKey(uuid)) {
                st = LandManager.getDB().prepareStatement("UPDATE landclaim_acl SET level=? WHERE land_id=? AND uuid=?");
                st.setString(1, level.toString());
                st.setInt(2, getID());
                st.setString(3, uuid.toString());
            } else {
                st = LandManager.getDB().prepareStatement("INSERT INTO landclaims_acl (land_id, uuid, level) VALUES (?, ?, ?);");
                st.setInt(1, getID());
                st.setString(2, uuid.toString());
                st.setString(3, level.toString());
            }
            st.executeUpdate();
            list.put(uuid, level);
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while trying to set access level for UUID \"" + uuid.toString() + "\" ", e);
            return false;
        }
        return true;
    }

}
