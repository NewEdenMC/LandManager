package co.neweden.LandManager.Listeners;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.LandClaim;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Util;
import co.neweden.menugui.menu.InventorySlot;
import co.neweden.menugui.menu.MenuInstance;
import co.neweden.menugui.menu.MenuPopulateEvent;
import co.neweden.menugui.menu.MenuRunnable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;

public class MenuEvents implements Listener {

    public static Map<UUID, OfflinePlayer> landListMenuSubjects = new HashMap<>();

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onMenuPopulate(MenuPopulateEvent event) {
        MenuInstance instance = event.getMenuInstance();
        if (!instance.getMenu().equals(LandManager.getLandListMenu()))
            return;

        OfflinePlayer forPlayer = landListMenuSubjects.get(event.getOpener().getUniqueId());
        if (forPlayer == null) forPlayer = event.getOpener();

        List<LandClaim> landList = new ArrayList<>();
        for (LandClaim land : LandManager.getLandClaims()) {
            if (land.getACL().containsKey(forPlayer.getUniqueId()) &&
                    !land.getACL().get(forPlayer.getUniqueId()).equals(ACL.Level.NO_ACCESS)) {
                landList.add(land);
            }
        }

        for (int i = 0; i < landList.size(); i++) {
            InventorySlot slot = event.getMenuInstance().getSlot(i);
            LandClaim land = landList.get(i);

            slot.setMaterial(land.getIconMaterial());
            slot.setDisplayName(land.getDisplayName());
            slot.addHoverText("&7Owned by: " + Bukkit.getOfflinePlayer(land.getOwner()).getName());
            slot.addHoverText("&eClick to teleport to this Land Claim.");
            slot.runOnClick(new MenuRunnable() {
                @Override
                public void run() {
                    event.getMenuInstance().closeMenu();
                    event.getOpener().teleport(land.getHomeLocation());
                }
            });
        }
    }

}
