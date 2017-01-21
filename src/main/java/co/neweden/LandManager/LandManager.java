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
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LandManager {

    protected static Main plugin;
    protected static Connection db;
    protected static Map<Integer, LandClaim> landClaims = new HashMap<>();
    private static Map<Location, Protection> blockProtections = new HashMap<>();
    protected static Menu landListMenu;

    public static Main getPlugin() { return plugin; }

    public static Connection getDB() { return db; }

    public static Protection getProtection(Block block) {
        if (blockProtections.containsKey(block.getLocation()))
            return blockProtections.get(block.getLocation());

        try {
            PreparedStatement st = getDB().prepareStatement("SELECT protection_id, owner, everyone_acl_level FROM protections WHERE world=? AND x=? AND y=? AND z=?");
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                Protection p = new Protection(rs.getInt("protection_id"));

                try {
                    p.owner = UUID.fromString(rs.getString("owner"));
                } catch (IllegalArgumentException e) {
                    getPlugin().getLogger().log(Level.SEVERE, "Protection ID #" + rs.getInt("protection_id") + ": UUID \"" + rs.getString("owner") + "\" is not valid, Protection will not be loaded.");
                    return null;
                }

                try {
                    if (rs.getString("everyone_acl_level") != null)
                        p.everyoneAccessLevel = ACL.Level.valueOf(rs.getString("everyone_acl_level"));
                } catch (IllegalArgumentException e) {
                    getPlugin().getLogger().log(Level.SEVERE, "Protection ID #" + rs.getInt("protection_id") + ": Value \"" + rs.getString("everyone_acl_level") + "\" is not valid, default Level will be used instead.");
                }

                blockProtections.put(block.getLocation(), p);
                return p;
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get block protection information.", e);
        }
        return null;
    }

    public static Protection createProtection(UUID owner, Block block) throws RestrictedWorldException {
        try {
            PreparedStatement st = getDB().prepareStatement("INSERT INTO `protections` (`world`, `x`, `y`, `z`, `owner`) VALUES (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());
            st.setString(5, owner.toString());
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();

            if (LandManager.isWorldRestrictedForProtections(block.getWorld()))
                throw new RestrictedWorldException(block.getWorld(), "Unable to create Protection in this World as it is restricted by the configuration.", "You are not allowed to create a protection in this world.");

            int pID = rs.getInt(1);
            Protection p = new Protection(pID);
            p.owner = owner;
            blockProtections.put(block.getLocation(), p);

            return p;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to create a new Protection.", e);
            return null;
        }
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
        try {
            PreparedStatement st = LandManager.db.prepareStatement("INSERT INTO `landclaims` (`owner`, `home_location`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, owner.toString());
            st.setString(2, Util.locationToString(homeLocation));
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();

            if (!LandManager.canPlayerClaimMoreLand(owner))
                throw new LandClaimLimitReachedException(null, owner, "Cannot create a new Land Claim for Player " + owner + " as the Player has reached their Land Claim Limit, limit: " + getLandClaimLimit(owner), "Unable to create a new Land Claim as you have reached your Land Claim Limit of " + getLandClaimLimit(owner) + " Land Claims.");

            if (LandManager.isWorldRestrictedForClaims(homeLocation.getWorld()))
                throw new RestrictedWorldException(homeLocation.getWorld(), "Unable to create Land Claim in this World as it is restricted by the configuration.", "You are not allowed to create a Land Claim in this world.");

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

    public static boolean isWorldRestrictedForProtections(World world) { return isWorldRestricted("protections", world); }
    public static boolean isWorldRestrictedForClaims(World world) { return isWorldRestricted("land_claims", world); }

    private static boolean isWorldRestricted(String type, World world) {
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

    public static boolean canBlockBeProtected(Material material) {
        if (!getPlugin().getConfig().isConfigurationSection("protections.blocks") || // if no blocks have been configured any blocks can be protected
                getPlugin().getConfig().isConfigurationSection("protections.blocks." + material.toString().toLowerCase())) return true;
        else
            return false;
    }

    public static boolean canBlockAutoProtect(Material material) {
        return getPlugin().getConfig().getBoolean("protections.blocks." + material.toString().toLowerCase() + ".autoProtect", false);
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
