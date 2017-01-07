package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Chunk;
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
import java.util.stream.Collectors;

public class BlockEvents implements Listener {

    private void handleBlockMove(Cancellable event, Block from, Block to) {
        LandClaim fromLand = LandManager.getLandClaim(from.getChunk());
        LandClaim toLand = LandManager.getLandClaim(to.getChunk());
        if (toLand == null) return;
        if (toLand.equals(fromLand)) return;
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) { handleBlockMove(event, event.getBlock(), event.getToBlock()); }

    private void handleCheckLandBorders(Cancellable event, Collection<Chunk> chunks) { handleCheckLandBorders(event, chunks, null);}
    private void handleCheckLandBorders(Cancellable event, Collection<Chunk> chunks, Player feedbackToPlayer) {
        Collection<LandClaim> found = chunks.stream().map(LandManager::getLandClaim).collect(Collectors.toSet());
        if (found.size() <= 1) return; // we use Set.size() instead of the Lambda count() as we can take advantage of Sets ability to group/override duplicate values for us
        if (feedbackToPlayer != null) {
            if (found.contains(null)) found.remove(null); // chunks with no land claims are group under null, this prevents an NPE on next line
            if (found.stream().filter(e -> e.testAccessLevel(feedbackToPlayer, ACL.Level.INTERACT, "landmanager.land.interactany")).count() == found.size()) return;
            feedbackToPlayer.sendMessage(Util.formatString("&cIt is not possible to perform this action, you are to close to a Land border."));
        }
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        handleCheckLandBorders(event, event.getReplacedBlockStates().stream().map(BlockState::getChunk).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) { handleCheckLandBorders(event, event.blockList().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    private void handleEveryoneAccessCheck(Cancellable event, Chunk chunk) {
        LandClaim land = LandManager.getLandClaim(chunk);
        if (land == null) return;
        if (!ACL.testAccessLevel(land.getEveryoneAccessLevel(), ACL.Level.INTERACT))
            event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleCheckLandBorders(event, event.blockList().stream().map(Block::getChunk).collect(Collectors.toSet()));
        if (event.getEntity() instanceof LivingEntity) // aka check for creepers or other exploding entities which can move
            handleEveryoneAccessCheck(event, event.getLocation().getChunk());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) { handleCheckLandBorders(event, event.getBlocks().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) { handleCheckLandBorders(event, event.getBlocks().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
        if (!(event.getEntity() instanceof Player)) // e.g. prevent zombies breaking village doors
            handleEveryoneAccessCheck(event, event.getBlock().getChunk());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        handleCheckLandBorders(event, event.getBlocks().stream().map(BlockState::getChunk).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) { handleEveryoneAccessCheck(event, event.getBlock().getChunk()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause().equals(BlockIgniteEvent.IgniteCause.SPREAD))
            handleEveryoneAccessCheck(event, event.getBlock().getChunk());
    }

}
