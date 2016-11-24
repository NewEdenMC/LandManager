package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    private void handleCheckLandBorders(Cancellable event, Collection<Block> blockList) {
        Collection<LandClaim> land = new HashSet<>();
        blockList.stream().forEach(
                e -> land.add(LandManager.getLandClaim(e.getChunk()))
        );
        if (land.size() > 1)
            event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        Collection<Block> blocks = new HashSet<>();
        event.getReplacedBlockStates().forEach(e -> blocks.add(e.getBlock()));
        handleCheckLandBorders(event, blocks);
        if (event.isCancelled())
            event.getPlayer().sendMessage(Util.formatString("&cYou can't place blocks which cross a land border."));
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) { handleCheckLandBorders(event, event.blockList()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) { handleCheckLandBorders(event, event.blockList()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) { handleCheckLandBorders(event, event.getBlocks()); }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) { handleCheckLandBorders(event, event.getBlocks()); }

}
