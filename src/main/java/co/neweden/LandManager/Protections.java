package co.neweden.LandManager;

import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
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
        Optional<BlockProtection> bpOption = blockProtections.stream().filter(p -> p.getBlock().equals(block)).findFirst();
        return bpOption.orElse(null);
    }

    public BlockProtection create(UUID owner, Block block) throws RestrictedWorldException {
        if (isWorldRestricted(block.getWorld()))
            throw new RestrictedWorldException(block.getWorld(), "Unable to create Protection in this World as it is restricted by the configuration.", "You are not allowed to create a protection in this world.");

        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("INSERT INTO `protections` (`world`, `chunk_loc`, `x`, `y`, `z`, `owner`) VALUES (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            st.setString(1, block.getWorld().getName());
            st.setString(2, block.getChunk().getX() + ":" + block.getChunk().getZ());
            st.setInt(3, block.getX());
            st.setInt(4, block.getY());
            st.setInt(5, block.getZ());
            st.setString(6, owner.toString());
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

    public boolean loadAllForChunk(Chunk chunk) {
        String cloc = chunk.getX() + ":" + chunk.getZ();
        try {
            PreparedStatement st = LandManager.getDB().prepareStatement("SELECT * FROM protections WHERE world=? AND chunk_loc=?");
            st.setString(1, chunk.getWorld().getName());
            st.setString(2, cloc);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                int protection_id = rs.getInt("protection_id");
                if (blockProtections.stream().filter(e -> e.getID() == protection_id).count() > 0)
                    continue; // If the current protection is already loaded skip it

                BlockProtection p = generate(rs);
                if (p == null) continue; // If an error occurred while generating, skip this one, console output already sent

                blockProtections.add(p);
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get Protection data for chunk_loc: " + cloc, e);
            return false;
        }
        return true;
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
    public void onChunkLoad(ChunkLoadEvent event) {
        loadAllForChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Collection<BlockProtection> bp = blockProtections.stream()
                .filter(e -> e.getBlock().getChunk().equals(event.getChunk()))
                .collect(Collectors.toList());
        blockProtections.removeAll(bp);
    }

}
