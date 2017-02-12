package co.neweden.LandManager;

import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import org.bukkit.block.Block;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BlockProtection extends Protection {

    protected Block block;

    public BlockProtection(int id) { super(id); }

    public ACL getParentACL() { return getParent(); }

    public LandClaim getParent() {
        return LandManager.getLandClaim(block.getChunk());
    }

    public Block getBlock() { return block; }

    public boolean setBlock(Block block) throws RestrictedWorldException {
        if (LandManager.isWorldRestrictedForProtections(block.getWorld()))
            throw new RestrictedWorldException(block.getWorld(), "Cannot change the World of protection #" + getID() + " to World " + block.getWorld().getName() + " as this world is restricted.", "Cannot change the world of this protection as the new world is restricted.");

        if (LandManager.getProtection(block) != null)
            return false; // prevent this protection from being set to a block which is already protected

        try {
            PreparedStatement st = LandManager.db.prepareStatement("UPDATE protections SET world=?,x=?,y=?,z=? WHERE protection_id=?;");
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());
            st.setInt(5, getID());
            st.executeUpdate();
            this.block = block;
            return true;
        } catch (SQLException e) {
            LandManager.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Protection #" + getID() + ": An SQL Exception occurred while trying to update the block location", e);
            return false;
        }
    }

}
