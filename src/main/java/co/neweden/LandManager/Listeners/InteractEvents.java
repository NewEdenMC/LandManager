package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Collection;
import java.util.HashSet;

public class InteractEvents implements Listener {

    private void handleEvent(Location targetLocation, Cancellable event, Entity callingEntity) {
        ACL acl = LandManager.getFirstACL(targetLocation);
        if (acl == null || callingEntity == null) return; // callingEntity may sometimes be null
        if (!(callingEntity instanceof Player)) return;
        Player player = (Player) callingEntity;

        String bperm = "";
        String typeName = "";
        if (acl instanceof Protection) {
            bperm = "landmanager.protection.interactany";
            typeName = "Protection";
        }
        if (acl instanceof LandClaim) {
            bperm = "landmanager.land.interactany";
            typeName = "Land Claim";
        }

        if (acl.testAccessLevel(player, ACL.Level.INTERACT, bperm))
            return;

        event.setCancelled(true);
        player.sendMessage(Util.formatString("&cYou do not have permission to interact with this " + typeName));
        player.updateInventory();
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        handleEvent(event.getClickedBlock().getLocation(), event, event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onVehicleEnter(VehicleEnterEvent event) {
        handleEvent(event.getVehicle().getLocation(), event, event.getEntered());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        handleEvent(event.getVehicle().getLocation(), event, event.getAttacker()); // event.getAttacker may sometimes be null
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        handleEvent(event.getEntity().getLocation(), event, event.getPlayer());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        handleEvent(event.getEntity().getLocation(), event, event.getDamager());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        handleEvent(event.getBlock().getLocation(), event, event.getPlayer());
    }

}
