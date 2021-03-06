package co.neweden.LandManager;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getPlayer;

public class Util {

    public static String formatString(String text) {
        text = text.replaceAll("&0", "\u00A70"); // Black
        text = text.replaceAll("&1", "\u00A71"); // Dark Blue
        text = text.replaceAll("&2", "\u00A72"); // Dark Green
        text = text.replaceAll("&3", "\u00A73"); // Dark Aqua
        text = text.replaceAll("&4", "\u00A74"); // Dark Red
        text = text.replaceAll("&5", "\u00A75"); // Dark Purple
        text = text.replaceAll("&6", "\u00A76"); // Gold
        text = text.replaceAll("&7", "\u00A77"); // Gray
        text = text.replaceAll("&8", "\u00A78"); // Dark Gray
        text = text.replaceAll("&9", "\u00A79"); // Blue
        text = text.replaceAll("&a", "\u00A7a"); // Green
        text = text.replaceAll("&b", "\u00A7b"); // Aqua
        text = text.replaceAll("&c", "\u00A7c"); // Red
        text = text.replaceAll("&d", "\u00A7d"); // Light Purple
        text = text.replaceAll("&e", "\u00A7e"); // Yellow
        text = text.replaceAll("&f", "\u00A7f"); // White

        text = text.replaceAll("&k", "\u00A7k"); // Obfuscated
        text = text.replaceAll("&l", "\u00A7l"); // Bold
        text = text.replaceAll("&m", "\u00A7m"); // Strikethrough
        text = text.replaceAll("&o", "\u00A7o"); // Italic
        text = text.replaceAll("&r", "\u00A7r"); // Reset

        return text;
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public static Location locationFromString(String location) {
        String[] locArray = location.split(",");

        if (locArray.length < 4) {
            LandManager.getPlugin().getLogger().log(Level.WARNING, "Tried to parse String \"" + location + "\" to org.bukkit.Location, however String is not in a valid format.");
            return null;
        }

        World world = Bukkit.getWorld(locArray[0]);
        if (world == null) {
            LandManager.getPlugin().getLogger().log(Level.WARNING, "Tried to parse String \"" + location + "\" to org.bukkit.Location, however World \"" + locArray[0] + "\" is not loaded.");
            return null;
        }

        double x; double y; double z;
        try {
            x = Double.parseDouble(locArray[1]);
            y = Double.parseDouble(locArray[2]);
            z = Double.parseDouble(locArray[3]);
        } catch (NumberFormatException e) {
            LandManager.getPlugin().getLogger().log(Level.WARNING, "Tried to parse String \"" + location + "\" to org.bukkit.Location, however some of \"" + locArray[1] + "," + locArray[2] + "," + locArray[3] + "\" are not numbers.");
            return null;
        }

        return new Location(world, x, y, z);
    }

    public static OfflinePlayer getOfflinePlayer(String name) {
        Player player = getPlayer(name);
        if (player != null) return player;

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName().equals(name))
                return offlinePlayer;
        }

        HttpProfileRepository hpr = new HttpProfileRepository("minecraft");
        Profile[] p = hpr.findProfilesByNames(name);
        if (p.length > 0) {
            String uuid = p[0].getId().replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
            return Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        } else
            return null;
    }

    public static Block getJoiningBlock(Block block) {
        if (getHorizontalJoiningBlockType().contains(block.getType())) {
            BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : faces) {
                Block faceBlock = block.getRelative(face);
                if (!getHorizontalJoiningBlockType().contains(faceBlock.getType())) continue;
                return faceBlock;
            }
        }

        if (block.getState().getData() instanceof Door) {
            Door door = (Door) block.getState().getData();
            Block joining;
            if (door.isTopHalf()) // Get the joining Block to "block", as in get the top or bottom part of the Door
                joining = block.getRelative(BlockFace.DOWN);
            else
                joining = block.getRelative(BlockFace.UP);
            // Make sure the joining Block is actually a Door (in the odd case where we only have half a Door)
            // if so return the joining Block, else return null
            return (joining.getState().getData() instanceof Door) ? joining : null;
        }

        return null;
    }

    public static Collection<Material> getHorizontalJoiningBlockType() {
        Collection<Material> list = new ArrayList<>();
        list.add(Material.CHEST); list.add(Material.TRAPPED_CHEST);
        return list;
    }

    public static Location getHighestAirBlockAt(World world, long x, long z) {
        // We want to do the least amount of iterations, so first we check the Y section where the ground is most
        // likely to be Y 50 to Y 100
        Location loc = evalYSection(new Location(world, x, 50, z), 100);
        if (loc != null) return loc;

        // Next we check below that from Y 0 to Y 49
        loc = evalYSection(new Location(world, x, 0, z), 49);
        if (loc != null) return loc;

        // Finally we check the least likely place Y 101 too the world height (probably Y 256) - 1, we limit to 1 block
        // below the world height because the top block won't have enough room for a player anyway if that is the top
        // air block
        loc = evalYSection(new Location(world, x, 101, z), world.getMaxHeight() - 1);
        if (loc != null) return loc;

        // Return null in the unlikely event this x and z location has no double air blocks
        return null;
    }

    private static Location evalYSection(Location start, int to) {
        Location below = start.clone().subtract(0, 1, 0);
        Location current = start;
        Location above = start.clone().add(0, 1, 0);
        Location use = null;

        for (int i = current.getBlockY(); i <= to; i++) {
            Material belowB = below.getBlock().getType();
            Material currentB = current.getBlock().getType();
            Material aboveB = above.getBlock().getType();

            if (belowB.isSolid() && !currentB.isSolid() && !aboveB.isSolid())
                use = current.clone();

            // increment each location for next pass
            below.add(0, 1, 0);
            current.add(0, 1, 0);
            above.add(0, 1, 0);
        }

        if (use != null) use.add(0.5, 0, 0.5); // put player in the centre of the block, avoids damage by being half in a wall
        return use;
    }

}
