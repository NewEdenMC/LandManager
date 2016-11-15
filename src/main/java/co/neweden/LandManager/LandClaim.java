package co.neweden.LandManager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
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
    }

    private boolean setDBValue(String key, String value) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("UPDATE landclaims SET " + key + "=? WHERE land_id=?;");
            st.setString(1, value);
            st.setInt(2, id);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Land Claim #" + id + ": An SQL Exception occurred while trying to update " + key + " to \"" + value + "\"");
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

    public boolean setAccess(UUID uuid, Level level) {

        return true;
    }

}
