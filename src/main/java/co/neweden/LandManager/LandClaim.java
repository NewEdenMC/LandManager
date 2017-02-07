package co.neweden.LandManager;

import co.neweden.LandManager.Exceptions.LandClaimLimitReachedException;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import co.neweden.LandManager.Exceptions.UnclaimChunkException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

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
                    list.add(new ACLEntry(UUID.fromString(rs.getString("uuid")), Level.valueOf(rs.getString("level")), false));
                } catch (IllegalArgumentException e) {
                    LandManager.getPlugin().getLogger().warning("Land Claim #" + id + ": Invalid data for ACL Entry, UUID or Level are not valid");
                }
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while loading Land Claim ACL Data", e);
        }
    }

    public ACL getParentACL() { return null; } // there is never a parent ACL for a Land Claim (yet)

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
        if (setDBValue("displayName", (name != null) ? name : null)) {
            displayName = name;
            return true;
        } else
            return false;
    }

    public int getID() { return id; }

    public UUID getOwner() { return owner; }

    public boolean setOwner(UUID uuid) throws LandClaimLimitReachedException {
        if (!LandManager.canPlayerClaimMoreLand(uuid))
            throw new LandClaimLimitReachedException(this, uuid, "Cannot set owner for Land Claim #" + id + " to Player " + uuid + " as the Player has reached their Land Claim Limit, limit: " + LandManager.getLandClaimLimit(getOwner()), "Unable to set the owner for this Land Claim as they have reached their Land Claim Limit.");

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
        if (setDBValue("everyone_acl_level", (level != null) ? level.toString() : null)) {
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
        if (setDBValue("icon_material", (material != null) ? material.toString() : null)) {
            iconMaterial = material;
            return true;
        } else
            return false;
    }

    public boolean claimChunk(Chunk chunk) throws RestrictedWorldException {
        if (LandManager.isChunkClaimed(chunk))
            return false;

        if (LandManager.isWorldRestrictedForClaims(chunk.getWorld()))
            throw new RestrictedWorldException(chunk.getWorld(), "Unable to claim chunk as the World is restricted by the configuration.", "It is not possible to claim a chunk in this world.");

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

    public boolean unclaimChunk(Chunk chunk) throws UnclaimChunkException {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("SELECT COUNT(*) FROM chunks WHERE land_id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            rs.next();
            if (rs.getInt(1) <= 1) {
                String consoleMessage = "Land Claim #" + id + ": Tried to unclaim chunk " + chunk + " but it is the last chunk in this Land Claim, Land Claim cannot contain no chunks, Land Claim should be deleted or another chunk claimed before  this one is unclaimed.";
                String userMessage = "This chunk can't be unclaimed as it is the last chunk in this Land Claim, either claim a new chunk for this Claim then try again, or delete this Land Claim.";
                throw new UnclaimChunkException(this, chunk, UnclaimChunkException.Reason.LAST_CHUNK, consoleMessage, userMessage);
            }

            if (getHomeLocation().getChunk().equals(chunk)) {
                String consoleMessage = "Land Claim #" + id + ": Tried to unclaim chunk " + chunk + " but it contains home location, home location must not be in chunk unless it is the only chunk left in the claim.";
                String userMessage = "This chunk can't be unclaimed as the Home Block for this Land Claim is in this chunk, to unclaim this chunk set the Home Block to another chunk in this Land Claim, then try again.";
                throw new UnclaimChunkException(this, chunk, UnclaimChunkException.Reason.HOME_IN_CHUNK, consoleMessage, userMessage);
            }

            st = LandManager.db.prepareStatement("DELETE FROM chunks WHERE land_id=? AND x=? AND z=?;");
            st.setInt(1, id);
            st.setInt(2, chunk.getX());
            st.setInt(3, chunk.getZ());
            st.executeUpdate();
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "An SQL Exception occurred while trying to un-claim chunk \"" + chunk + "\" for land claim " + id + " (\"" + displayName + "\").", e);
            return false;
        }
        return true;
    }

    public boolean setAccess(UUID uuid, Level level) {
        try {
            PreparedStatement st;
            if (level == null) {
                st = LandManager.getDB().prepareStatement("DELETE FROM landclaims_acl WHERE land_id=? AND uuid=?");
                st.setInt(1, getID());
                st.setString(2, uuid.toString());
                list.remove(uuid);
                st.executeUpdate();
                return true;
            }

            if (list.contains(uuid)) {
                st = LandManager.getDB().prepareStatement("UPDATE landclaims_acl SET level=? WHERE land_id=? AND uuid=?");
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
            list.add(new ACLEntry(uuid, level, false));
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while trying to set access level for UUID \"" + uuid.toString() + "\" ", e);
            return false;
        }
        return true;
    }

}
