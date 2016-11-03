package co.neweden.LandManager;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class LandManager {

    protected static Main plugin;
    protected static Connection db;
    protected static Collection<LandClaim> landClaims = new HashSet<>();

    public static Main getPlugin() { return plugin; }

    public static boolean isChunkClaimed(Chunk chunk) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("SELECT chunk_id FROM chunks WHERE world=? AND x=? AND z=?;");
            st.setString(1, chunk.getWorld().getName());
            st.setInt(2, chunk.getX());
            st.setInt(3, chunk.getZ());
            ResultSet rs = st.executeQuery();
            if (rs.next())
                return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get claimed chunk information.", e);
            return false;
        }
        return false;
    }

    public static LandClaim createClaim(UUID owner, Location homeLocation) {
        try {
            PreparedStatement st = LandManager.db.prepareStatement("INSERT INTO `landclaims` (`owner`, `home_location`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, owner.toString());
            st.setString(2, Util.locationToString(homeLocation));
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();

            LandClaim claim = new LandClaim(rs.getInt(1));
            claim.owner = owner;
            claim.homeLocation = homeLocation;
            landClaims.add(claim);

            return claim;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to create a new Land Claim.", e);
            return null;
        }
    }

}
