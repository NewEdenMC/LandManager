package co.neweden.LandManager.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

public class BlockEvents implements Listener {

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {

    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {

    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {

    }

}
