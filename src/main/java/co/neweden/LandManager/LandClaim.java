package co.neweden.LandManager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class LandClaim {

    private int id;
    protected String displayName;
    protected UUID owner;
    protected Location homeLocation;
    protected Material iconMaterial = Material.GRASS;

    public LandClaim(int id) {
        this.id = id;
    }

    public String getDisplayName() {
        if (displayName == null)
            return "Land Claim #" + id;
        else
            return displayName;
    }

    public int getID() { return id; }

    public UUID getOwner() { return owner; }

    public Location getHomeLocation() { return homeLocation; }

    public Material getIconMaterial() { return iconMaterial; }

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
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to claim chunk \"" + chunk + "\" for land claim " + id + " (\"" + displayName + "\").", e);
            return false;
        }

        return true;
    }

}
