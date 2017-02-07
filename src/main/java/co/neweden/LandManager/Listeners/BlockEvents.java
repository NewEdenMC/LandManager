package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Chunk;
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
import java.util.stream.Stream;

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
            Collection<String> bperms = new HashSet<>();
            bperms.add("landmanager.land.interactany");
            bperms.add("landmanager.protection.interactany");
            if (found.stream().filter(e -> e.testAccessLevel(trigger, ACL.Level.INTERACT, bperms)).count() == found.size()) return;
            trigger.sendMessage(Util.formatString("&cIt is not possible to perform this action, you are either to close to a Land border or this will effect a protection that you do not have access to."));
        }
        event.setCancelled(true);
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
