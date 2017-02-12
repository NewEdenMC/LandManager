package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.*;
import co.neweden.LandManager.Exceptions.RestrictedWorldException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlockEvents implements Listener {

    private void handleBlockMove(Cancellable event, Block from, Block to) {
        LandClaim fromLand = LandManager.getLandClaim(from.getChunk());
        LandClaim toLand = LandManager.getLandClaim(to.getChunk());
        if (toLand == null) return;
        if (toLand.equals(fromLand)) return;
        if (LandManager.getProtection(from) != null || LandManager.getProtection(to) != null) return;
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) { handleBlockMove(event, event.getBlock(), event.getToBlock()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromToUpdate(BlockFromToEvent event) {
        BlockProtection p = LandManager.getProtection(event.getBlock());
        if (p == null || event.getBlock().isLiquid()) return;

        try {
            if (!p.setBlock(event.getToBlock()))
                event.setCancelled(true);
        } catch (RestrictedWorldException e) {
            event.setCancelled(true);
        }
    }

    private void handleCheckBlocks(Cancellable event, Collection<Block> blocks) { handleCheckBlocks(event, blocks, null);}
    private void handleCheckBlocks(Cancellable event, Collection<Block> blocks, Player trigger) {
        Collection<ACL> found = blocks.stream()
                .map(Block::getChunk).map(LandManager::getLandClaim)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        found.addAll(blocks.stream()
                .map(LandManager::getProtection)
                .filter(Objects::nonNull).collect(Collectors.toSet()));

        if (found.size() < 1) return; // we use Set.size() instead of the Lambda count() as we can take advantage of Sets ability to group/override duplicate values for us
        if (trigger != null) {
            // if the event we are checking was triggered by a player we have the opportunity to check if the player is
            // allowed to perform any actions to the effected blocks
            int count = 0;
            for (ACL e : found) {
                String bperm = "";
                if (e instanceof Protection) bperm = "landmanager.protection.interactany";
                if (e instanceof LandClaim) bperm = "landmanager.land.interactany";
                if (e.testAccessLevel(trigger, ACL.Level.INTERACT, bperm)) count++;
            }
            if (count == found.size()) return;
            trigger.sendMessage(Util.formatString("&cIt is not possible to perform this action, you are either to close to a Land border or this will effect a protection that you do not have access to."));
        }
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Protection p = LandManager.getProtection(event.getBlock());
        if (p == null) return;

        if (!p.testAccessLevel(event.getPlayer(), ACL.Level.FULL_ACCESS, "landmanager.unlock.any")) {
            event.getPlayer().sendMessage(Util.formatString("&cThis block is protected, you do not have permission to remove the protection by breaking it."));
            event.setCancelled(true);
            return;
        }

        if (LandManager.deleteProtection(p))
            event.getPlayer().sendMessage(Util.formatString("&aThis block was protected, the protection has now been removed."));
        else
            event.getPlayer().sendMessage(Util.formatString("&cThis block was protected, there was a problem removing the protection, please contact a staff member."));
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        handleCheckBlocks(event, event.getReplacedBlockStates().stream().map(BlockState::getBlock).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) { handleCheckBlocks(event, event.blockList()); }

    private void handleEveryoneAccessCheck(Cancellable event, Location loc) {
        ACL acl = LandManager.getFirstACL(loc);
        if (acl == null) return;
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
