package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.*;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockEvents implements Listener {

    public BlockEvents() {
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
    }

    private void handleBlockMove(Cancellable event, Block from, Block to) {
        LandClaim fromLand = LandManager.getLandClaim(from.getChunk());
        LandClaim toLand = LandManager.getLandClaim(to.getChunk());
        if (toLand == null) return;
        if (toLand.equals(fromLand)) return;
        if (LandManager.protections().get(from) != null || LandManager.protections().get(to) != null) return;
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) { handleBlockMove(event, event.getBlock(), event.getToBlock()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromToUpdate(BlockFromToEvent event) {
        RegisteredBlockProtection p = LandManager.protections().get(event.getBlock());
        if (p == null || event.getBlock().isLiquid()) return;

        try {
            if (!p.setBlock(event.getToBlock()))
                event.setCancelled(true);
        } catch (RestrictedWorldException e) {
            event.setCancelled(true);
        }
    }

    // called on block/entity explode and piston extend/retract
    private void handleCheckBlocks(Cancellable event, Collection<Block> blocks) { handleCheckBlocks(event, blocks, null); }
    // called on block multi-place and structure grow (trees)
    private void handleCheckBlocks(Cancellable event, Collection<Block> blocks, Player trigger) {
        Set<RegisteredProtection> pFound = blocks.stream()
                .map(LandManager.protections()::get)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (pFound.size() > 0) {
            // If a block or entity are registered we should not effect it in any way so cancel this action
            event.setCancelled(true);
            if (trigger != null)
                trigger.sendMessage(Util.formatString("&cThe action you just performed couldn't not be completed as it would have effected at least one nearby Protected Blocks or Entities."));
            return;
        }

        // Now we now the effected blocks contain no protections, search for any Land Claims.
        // The resulting Set will also include null if there are any chunks that will be effected which don't have
        // any Land Claims
        Set<LandClaim> landFound = blocks.stream()
                .map(Block::getChunk).map(LandManager::getLandClaim)
                .collect(Collectors.toSet());

        boolean hasNull = false;
        if (landFound.contains(null)) {
            hasNull = true;
            landFound.remove(null);
        }

        if (landFound.size() < 1)
            return; // we use Set.size() instead of the Lambda count() as we can take advantage of Sets ability to group/override duplicate values for us

        if (trigger != null) {
            // if the event we are checking was triggered by a player we have the opportunity to check if the player is
            // allowed to perform any actions to the effected blocks
            int count = 0;
            for (LandClaim e : landFound) {
                if (e.testAccessLevel(trigger, ACL.Level.INTERACT, "landmanager.land.interactany")) count++;
            }
            if (count == landFound.size()) return;
            trigger.sendMessage(Util.formatString("&cThe action you just performed would effect at least one nearby Land Claim and you do not have the Access Level INTERACT on all effected Land Claims."));
            event.setCancelled(true);
            return;
        }

        if (event instanceof BlockPistonEvent) {
            BlockPistonEvent pistonEvent = (BlockPistonEvent) event;
            handleLandBorderCheck(pistonEvent.getBlock().getChunk(), landFound, event);
            return;
        }

        if (event instanceof EntityExplodeEvent) {
            // We should allow TNT to explode in a Land Claim
            EntityExplodeEvent entityEvent = (EntityExplodeEvent) event;
            if (entityEvent.getEntityType() == EntityType.PRIMED_TNT) {
                handleLandBorderCheck(entityEvent.getLocation().getChunk(), landFound, event);
                return;
            }
        }

        event.setCancelled(true);
    }

    private void handleLandBorderCheck(Chunk source, Set<LandClaim> landFound, Cancellable event) {
        // Get the Land Claim of the chunk the event was triggered in
        LandClaim sourceLand = LandManager.getLandClaim(source);
        // Don't cancel the event if the the source and effected blocks are on the same Land Claim
        // In other words the event should not cancel if only one Land Claim was found
        if (landFound.iterator().next().equals(sourceLand) && landFound.size() == 1) return;
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        RegisteredProtection p = LandManager.protections().get(event.getBlock());
        if (p == null) return;

        if (!p.testAccessLevel(event.getPlayer(), ACL.Level.FULL_ACCESS, "landmanager.unlock.any")) {
            event.getPlayer().sendMessage(Util.formatString("&cThis block is protected, you do not have permission to remove the protection by breaking it."));
            event.setCancelled(true);
            return;
        }

        if (processUpdateForMultiBlockProtection(event.getPlayer(), event.getBlock(), p)) return;

        if (LandManager.protections().delete(p))
            event.getPlayer().sendMessage(Util.formatString("&aThis block was protected, the protection has now been removed."));
        else
            event.getPlayer().sendMessage(Util.formatString("&cThis block was protected, there was a problem removing the protection, please contact a staff member."));
    }

    /*
     For protections associated with multiple blocks, returns true if removal of the protection should be stopped,
     false if it should continue
     */
    private boolean processUpdateForMultiBlockProtection(Player player, Block block, RegisteredProtection p) {
        // must be a chest and must be a block protection to swap protection block, otherwise removal should continue
        if (!Util.getHorizontalJoiningBlockType().contains(block.getType()) || !(p instanceof RegisteredBlockProtection)) return false;
        RegisteredBlockProtection bp = (RegisteredBlockProtection) p;
        // If the block relating to the protection is different from the one being broken we don't need to do anything
        // e.g. if the left side of a double chest is being broken but the right side holds the protection, breaking
        // the left side effects nothing, return true to prevent the protection being removed
        if (!block.equals(bp.getBlock())) return true;
        Block adjacent = Util.getJoiningBlock(block);
        // Return if there are no adjacent blocks removal should continue
        if (adjacent == null) return false;

        try {
            // If the block being broken holds the protection switch the block to the adjacent block
            if (!bp.setBlock(adjacent))
                player.sendMessage(Util.formatString("&cThere was a problem moving the protection to the adjacent block, please contact a staff member."));
        } catch (RestrictedWorldException e) {
            return false; // There's no way an adjacent block spans two worlds, that's just crazy talk
        }
        return true;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        handleCheckBlocks(event, event.getReplacedBlockStates().stream().map(BlockState::getBlock).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) { handleCheckBlocks(event, event.blockList()); }

    private void handleEveryoneAccessCheck(Cancellable event, Location loc) {
        ACL acl = LandManager.protections().getACL(loc.getBlock());
        if (!ACL.testAccessLevel(acl.getEveryoneAccessLevel(), ACL.Level.INTERACT))
            event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleCheckBlocks(event, event.blockList());
        if (event.getEntity() instanceof LivingEntity) // aka check for creepers or other exploding entities which can move
            handleEveryoneAccessCheck(event, event.getLocation());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) { handleCheckBlocks(event, event.getBlocks()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) { handleCheckBlocks(event, event.getBlocks()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
        if (!(event.getEntity() instanceof Player)) // e.g. prevent zombies breaking village doors
            handleEveryoneAccessCheck(event, event.getBlock().getLocation());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        handleCheckBlocks(event, event.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) { handleEveryoneAccessCheck(event, event.getBlock().getLocation()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause().equals(BlockIgniteEvent.IgniteCause.SPREAD))
            handleEveryoneAccessCheck(event, event.getBlock().getLocation());
    }

}
