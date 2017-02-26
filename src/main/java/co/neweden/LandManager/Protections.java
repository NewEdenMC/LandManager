package co.neweden.LandManager;

import co.neweden.LandManager.Exceptions.ProtectionAlreadyExistsException;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Protections implements Listener {

    protected List<BlockProtection> blockProtections = new CopyOnWriteArrayList<>();

    public Protections() {
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
    }

    public boolean isWorldRestricted(World world) { return LandManager.isWorldRestricted("protections", world); }

    public BlockProtection get(Block block) {
        // Setup b1 and b2, in general b1 and b2 will be the same except where "block" is e.g. a Chest or Door
        final Block b1 = block;
        Block joining = Util.getJoiningBlock(block);
        final Block b2 = (joining != null) ? joining : block;

        // Search currently loaded protections to see if protection for "block" is already loaded, if so return it
        Optional<BlockProtection> bpOption = blockProtections.stream()
                .filter(p -> p.getBlock().equals(b1) || p.getBlock().equals(b2)).findFirst();
        if (bpOption.isPresent())
            return bpOption.get();

        // If we couldn't find the relevant protection query the database to see if it's there
        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("SELECT * FROM protections WHERE (world=? AND x=? AND y=? AND z=?) OR (world=? AND x=? AND y=? AND z=?)");

            st.setString(1, b1.getWorld().getName());
            st.setInt(2, b1.getX()); st.setInt(3, b1.getY()); st.setInt(4, b1.getZ());

            st.setString(5, b2.getWorld().getName());
            st.setInt(6, b2.getX()); st.setInt(7, b2.getY()); st.setInt(8, b2.getZ());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                BlockProtection p = generate(rs);
                if (p == null) return null; // If an error occurred while generating, skip this one, console output already sent
                blockProtections.add(p);
                return p;
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get Protection data for block at: x=" + block.getX() + " y=" + block.getType() + " z=" + block.getZ(), e);
            return null;
        }

        return null;
    }

    private BlockProtection generate(ResultSet rs) throws SQLException {
        BlockProtection p = new BlockProtection(rs.getInt("protection_id"));

        try {
            p.owner = UUID.fromString(rs.getString("owner"));
        } catch (IllegalArgumentException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "Protection ID #" + rs.getInt("protection_id") + ": UUID \"" + rs.getString("owner") + "\" is not valid, Protection will not be loaded.");
            return null;
        }

        try {
            if (rs.getString("everyone_acl_level") != null)
                p.everyoneAccessLevel = ACL.Level.valueOf(rs.getString("everyone_acl_level"));
        } catch (IllegalArgumentException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "Protection ID #" + rs.getInt("protection_id") + ": Value \"" + rs.getString("everyone_acl_level") + "\" is not valid, default Level will be used instead.");
        }

        World w = Bukkit.getWorld(rs.getString("world"));
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        p.block = new Location(w, x, y, z).getBlock();

        return p;
    }

    public BlockProtection create(UUID owner, Block block) throws RestrictedWorldException, ProtectionAlreadyExistsException {
        if (isWorldRestricted(block.getWorld()))
            throw new RestrictedWorldException(block.getWorld(), "Unable to create Protection in this World as it is restricted by the configuration.", "You are not allowed to create a protection in this world.");

        ACL parent = LandManager.getFirstACL(block.getLocation());
        if (parent != null) {
            if (parent instanceof Protection)
                throw new ProtectionAlreadyExistsException((Protection) parent, "Unable to create Protection as a Protection already exists for the Block at the Location: x=" + block.getX() + " y=" + block.getType() + " z=" + block.getZ(), "Cannot protect this block as it is already protected.");
        }

        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("INSERT INTO `protections` (`world`, `x`, `y`, `z`, `owner`) VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());
            st.setString(5, owner.toString());
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();

            int pID = rs.getInt(1);
            BlockProtection p = new BlockProtection(pID);
            p.owner = owner;
            p.block = block;
            blockProtections.add(p);

            return p;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to create a new Protection.", e);
            return null;
        }
    }

    public boolean delete(Protection protection) {
        try {
            blockProtections.removeIf(p -> p.equals(protection));

            PreparedStatement st = LandManager.getDB().prepareStatement("DELETE FROM protections_acl WHERE protection_id=?");
            st.setInt(1, protection.getID());
            st.executeUpdate();

            st = LandManager.getDB().prepareStatement("DELETE FROM protections WHERE protection_id=?");
            st.setInt(1, protection.getID());
            st.executeUpdate();
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to delete Protection #" + protection.getID(), e);
            return false;
        }
        return true;
    }

    public boolean canBlockBeProtected(Material material) {
        if (!LandManager.getPlugin().getConfig().isConfigurationSection("protections.blocks") || // if no blocks have been configured any blocks can be protected
                LandManager.getPlugin().getConfig().isConfigurationSection("protections.blocks." + material.toString().toLowerCase())) return true;
        else
            return false;
    }

    public boolean canBlockAutoProtect(Material material) {
        return LandManager.getPlugin().getConfig().getBoolean("protections.blocks." + material.toString().toLowerCase() + ".auto_protect", false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Collection<BlockProtection> bp = blockProtections.stream()
                .filter(e -> e.getBlock().getChunk().equals(event.getChunk()))
                .collect(Collectors.toList());
        blockProtections.removeAll(bp);
    }

}
