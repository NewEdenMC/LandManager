package co.neweden.LandManager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Protections implements Listener {

    public Protections() {
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
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
                if (LandManager.blockProtections.stream().filter(e -> e.getID() == protection_id).count() > 0)
                    continue; // If the current protection is already loaded skip it

                BlockProtection p = generateProtection(rs);
                if (p == null) continue; // If an error occurred while generating, skip this one, console output already sent

                LandManager.blockProtections.add(p);
            }
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to get Protection data for chunk_loc: " + cloc, e);
            return false;
        }
        return true;
    }

    private BlockProtection generateProtection(ResultSet rs) throws SQLException {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        loadAllForChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Collection<BlockProtection> bp = LandManager.blockProtections.stream()
                .filter(e -> e.getBlock().getChunk().equals(event.getChunk()))
                .collect(Collectors.toList());
        bp.forEach(LandManager.blockProtections::remove);
    }

}
