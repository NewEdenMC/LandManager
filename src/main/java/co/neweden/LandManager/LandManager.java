package co.neweden.LandManager;

import co.neweden.LandManager.Exceptions.LandClaimLimitReachedException;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import co.neweden.menugui.menu.Menu;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class LandManager {

    protected static Main plugin;
    protected static Connection db;
    protected static Map<Integer, LandClaim> landClaims = new HashMap<>();
    protected static Protections protections;
    protected static Menu landListMenu;

    public static Main getPlugin() { return plugin; }

    public static Connection getDB() { return db; }

    public static Protections protections() { return protections; }

    /*
     Returns the first aka most significant ACL that can be found for the location
     Starting by checking for a protection, then if none looking for a land claim
     */
    public static ACL getFirstACL(Location loc) {
        // Check and return a protection if not null
        Protection p = protections().get(loc.getBlock());
        if (p != null) return p;
        // Check and return LandClaim if not null
        LandClaim l = getLandClaim(loc.getChunk());
        if (l != null) return l; else return null;
    }

    public static Menu getLandListMenu() { return landListMenu; }

    public static Collection<LandClaim> getLandClaims() { return new HashSet<>(landClaims.values()); }

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

    public static LandClaim createClaim(UUID owner, Location homeLocation) throws RestrictedWorldException, LandClaimLimitReachedException {
        if (!LandManager.canPlayerClaimMoreLand(owner))
            throw new LandClaimLimitReachedException(null, owner, "Cannot create a new Land Claim for Player " + owner + " as the Player has reached their Land Claim Limit, limit: " + getLandClaimLimit(owner), "Unable to create a new Land Claim as you have reached your Land Claim Limit of " + getLandClaimLimit(owner) + " Land Claims.");

        if (LandManager.isWorldRestrictedForClaims(homeLocation.getWorld()))
            throw new RestrictedWorldException(homeLocation.getWorld(), "Unable to create Land Claim in this World as it is restricted by the configuration.", "You are not allowed to create a Land Claim in this world.");

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

    public static boolean isWorldRestrictedForClaims(World world) { return isWorldRestricted("land_claims", world); }

    protected static boolean isWorldRestricted(String type, World world) {
        String restrictedMode = LandManager.getPlugin().getConfig().getString(type + ".restricted_worlds_mode", "");
        Collection<String> restrictedWorlds = LandManager.getPlugin().getConfig().getStringList(type + ".restricted_worlds_list");

        if (restrictedMode.equalsIgnoreCase("whitelist")) {
            if (!restrictedWorlds.contains(world.getName()))
                return true;
        } else if (restrictedMode.equalsIgnoreCase("blacklist")) {
            if (restrictedWorlds.contains(world.getName()))
                return true;
        }

        return false;
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

    public static boolean deleteClaim(LandClaim land) {
        try {
            landClaims.remove(land.getID());

            PreparedStatement st = getDB().prepareStatement("DELETE FROM chunks WHERE land_id=?");
            st.setInt(1, land.getID());
            st.executeUpdate();

            st = getDB().prepareStatement("DELETE FROM landclaims_acl WHERE land_id=?");
            st.setInt(1, land.getID());
            st.executeUpdate();

            st = getDB().prepareStatement("DELETE FROM landclaims WHERE land_id=?");
            st.setInt(1, land.getID());
            st.executeUpdate();
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to remove chunks to delete Land Claim #" + land.getID());
            return false;
        }
        return true;
    }

    public static boolean canPlayerClaimMoreLand(UUID playerUUID) {
        long claims = LandManager.getLandClaims().stream().filter(e -> e.getOwner().equals(playerUUID)).count();
        int limit = LandManager.getLandClaimLimit(playerUUID);
        return (limit == -1 || claims <= limit);
    }

    public static int getLandClaimLimit(UUID playerUUID) {
        try {
            PreparedStatement st = getDB().prepareStatement("SELECT claim_limit_cache FROM players WHERE uuid=?");
            st.setString(1, playerUUID.toString());
            ResultSet rs = st.executeQuery();
            if (rs.next())
                return rs.getInt("claim_limit_cache");
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get claim_limit_cache value for UUID: " + playerUUID);
        }
        return getPlugin().getConfig().getInt("land_claims.default_claim_limit_per_player", -1);
    }

    public static void updatePlayerCache(Player player) {
        int limit = -2;
        if (player.hasPermission("landmanager.claimlimit.unlimited"))
            limit = -1;
        else {
            for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
                String perm = permInfo.getPermission();

                if (perm.length() < 23) continue;
                System.out.print(perm);
                if (!perm.substring(0, 23).equalsIgnoreCase("landmanager.claimlimit.")) continue;
                System.out.print(perm);

                try {
                    int plimit = Integer.parseInt(perm.substring(23));
                    if (plimit > limit) limit = plimit;
                } catch (NumberFormatException ex) { continue; }
            }
        }

        try {
            PreparedStatement st;
            if (limit == -2)
                st = LandManager.getDB().prepareStatement("DELETE FROM players WHERE uuid = ?");
            else {
                st = LandManager.getDB().prepareStatement("INSERT INTO players (uuid, claim_limit_cache) VALUES (?,?) ON DUPLICATE KEY UPDATE claim_limit_cache=?");
                st.setInt(2, limit); st.setInt(3, limit);
            }
            st.setString(1, player.getUniqueId().toString());
            st.executeUpdate();
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to insert/update player claim_limit_cache for uuid: " + player.getUniqueId(), e);
        }
    }

}
