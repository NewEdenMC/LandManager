package co.neweden.LandManager;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class LandManager {

    protected static Main plugin;
    protected static Connection db;
    protected static Map<Integer, LandClaim> landClaims = new HashMap<>();

    public static Main getPlugin() { return plugin; }

    public static Connection getDB() { return db; }

    public static LandClaim getLandClaim(int landID) { return landClaims.get(landID); }

    public static LandClaim getLandClaim(Chunk chunk) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("SELECT land_id FROM chunks WHERE world=? AND x=? AND z=?;");
            st.setString(1, chunk.getWorld().getName());
            st.setInt(2, chunk.getX());
            st.setInt(3, chunk.getZ());
            ResultSet rs = st.executeQuery();
            if (rs.next())
                return landClaims.get(rs.getInt("land_id"));
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get chunk information.", e);
        }
        return null;
    }

    public static boolean isChunkClaimed(Chunk chunk) {
        return (getLandClaim(chunk) != null);
    }

    public static LandClaim createClaim(UUID owner, Location homeLocation) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("INSERT INTO `landclaims` (`owner`, `home_location`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, owner.toString());
            st.setString(2, Util.locationToString(homeLocation));
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();

            int landID = rs.getInt(1);
            LandClaim claim = new LandClaim(landID);
            claim.owner = owner;
            claim.homeLocation = homeLocation;
            landClaims.put(landID, claim);

            return claim;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to create a new Land Claim.", e);
            return null;
        }
    }

    public static Collection<LandClaim> getAdjacentClaims(Chunk chunk) { return getAdjacentClaims(chunk, null); }
    public static Collection<LandClaim> getAdjacentClaims(Chunk chunk, UUID uuid) {
        Collection<LandClaim> adjacentLand = new HashSet<>();
        try {
            PreparedStatement st = LandManager.getDB().prepareStatement(
                    "SELECT * FROM chunks WHERE\n" +
                    " (x = ? - 1 AND z = ?) OR\n" +
                    " (x = ? + 1 AND z = ?) OR\n" +
                    " (x = ? AND z = ? - 1) OR\n" +
                    " (x = ? AND z = ? + 1);"
            );
            st.setInt(1, chunk.getX()); st.setInt(2, chunk.getZ());
            st.setInt(3, chunk.getX()); st.setInt(4, chunk.getZ());
            st.setInt(5, chunk.getX()); st.setInt(6, chunk.getZ());
            st.setInt(7, chunk.getX()); st.setInt(8, chunk.getZ());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                LandClaim land = LandManager.getLandClaim(rs.getInt("land_id"));
                if (land == null) continue;
                if (uuid != null) {
                    if (!land.testAccessLevel(uuid, ACL.Level.MODIFY)) continue;
                }
                adjacentLand.add(land);
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get nearby chunks.", e);
            return null;
        }
        return adjacentLand;
    }

}
