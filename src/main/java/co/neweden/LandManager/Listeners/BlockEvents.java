package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
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
        event.setCancelled(true);
        if (feedbackToPlayer != null) {
            feedbackToPlayer.sendMessage(Util.formatString("&cIt is not possible to perform this action, you are to close to a Land border."));
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        handleCheckLandBorders(event, event.getReplacedBlockStates().stream().map(BlockState::getChunk).collect(Collectors.toSet()), event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) { handleCheckLandBorders(event, event.blockList().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    private void handleEntityExplosions(Cancellable event, Entity explodingEntity) {
        if (event.isCancelled() || !(explodingEntity instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) explodingEntity;
        LandClaim land = LandManager.getLandClaim(entity.getLocation().getChunk());
        if (land == null) return;
        if (!ACL.testAccessLevel(land.getEveryoneAccessLevel(), ACL.Level.INTERACT))
            event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleCheckLandBorders(event, event.blockList().stream().map(Block::getChunk).collect(Collectors.toSet()));
        handleEntityExplosions(event, event.getEntity());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) { handleCheckLandBorders(event, event.getBlocks().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) { handleCheckLandBorders(event, event.getBlocks().stream().map(Block::getChunk).collect(Collectors.toSet())); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
        if (event.getEntity() instanceof Player) return;
        LandClaim land = LandManager.getLandClaim(event.getBlock().getLocation().getChunk());
        if (land == null) return;
        if (!ACL.testAccessLevel(land.getEveryoneAccessLevel(), ACL.Level.INTERACT))
            event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        handleCheckLandBorders(event, event.getBlocks().stream().map(BlockState::getChunk).collect(Collectors.toSet()), event.getPlayer());
    }

}
