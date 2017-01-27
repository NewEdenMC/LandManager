package co.neweden.LandManager.Commands;

import co.neweden.LandManager.ACL;
import co.neweden.LandManager.Exceptions.UserException;
import co.neweden.LandManager.LandManager;
import co.neweden.LandManager.Protection;
import co.neweden.LandManager.Util;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class ProtectionCommands implements CommandExecutor, Listener {

    private Map<Player, CommandCache> cmdCache = new HashMap<>();
    private Collection<Player> persistPlayers = new HashSet<>();

    public ProtectionCommands() {
        LandManager.getPlugin().getCommand("ppersist").setExecutor(this);
        LandManager.getPlugin().getCommand("pprotect").setExecutor(this);
        LandManager.getPlugin().getCommand("unlock").setExecutor(this);
        LandManager.getPlugin().getCommand("pinfo").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, LandManager.getPlugin());
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Util.formatString("&cYou must be a player to run this command"));
            return true;
        }

        Player player = (Player) sender;
        if (handlePersist(command, player)) return true; // we are done if the player ran the persist command
        cmdCache.put(player, new CommandCache(command.getName(), args, System.currentTimeMillis()));
        sender.sendMessage(Util.formatString("&bThe command is ready, &eleft click&b a block to run this command!"));

        return true;
    }

    private boolean handlePersist(Command command, Player player) {
        if (!command.getName().toLowerCase().equals("ppersist")) return false;
        if (persistPlayers.contains(player)) {
            persistPlayers.remove(player);
            cmdCache.remove(player);
            player.sendMessage(Util.formatString("&bPersist mode for protection commands &cOFF"));
        } else {
            persistPlayers.add(player);
            player.sendMessage(Util.formatString(
                    "&bPersist mode for protection commands &aON\n" +
                    "&bThe next protection command you run will stay active until you run /" + command.getName().toLowerCase() + " again"
            ));
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK) ||
                !cmdCache.containsKey(player)) return;

        event.setCancelled(true);

        CommandCache cmd = cmdCache.get(player);
        if (!persistPlayers.contains(player))
            cmdCache.remove(player);

        boolean end = true;

        try {
            switch (cmd.command.toLowerCase()) {
                case "pprotect": protectCommand(player, event.getClickedBlock()); break;
                case "pinfo": infoCommand(player, event.getClickedBlock()); break;
                default: end = false;
            }

            if (end)
                return; // to prevent the below code from executing after any of the above commands have executed

            Protection protection = LandManager.getProtection(event.getClickedBlock());

            if (protection == null)
                throw new CommandException("&cThis " + event.getClickedBlock().getType().toString().toLowerCase() + " is not registered therefor you can't use this command on it.");

            switch (cmd.command.toLowerCase()) {
                case "unlock": unlockCommand(player, protection);
            }
        } catch (CommandException e) {
            event.getPlayer().sendMessage(Util.formatString(e.getMessage()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!LandManager.isWorldRestrictedForProtections(block.getWorld()) &&
                LandManager.canBlockAutoProtect(block.getType()))
            protectCommand(event.getPlayer(), block);
    }

    private void protectCommand(Player player, Block block) {
        if (LandManager.getProtection(block) != null)
            throw new CommandException("&cYou can't create a new protection as this " + block.getType().toString().toLowerCase() + " is already protected.");

        try {
            LandManager.createProtection(player.getUniqueId(), block);
        } catch (UserException e) {
            throw new CommandException("&c" + e.getUserMessage());
        }

        player.sendMessage(Util.formatString("&aProtection has been created."));
    }

    private void unlockCommand(Player player, Protection protection) {
        if (!protection.testAccessLevel(player, ACL.Level.FULL_ACCESS, "landmanager.unlock.any"))
            throw new CommandException("&cYou do not have permission to remove this protection.");

        if (!LandManager.deleteProtection(protection))
            throw new CommandException("&cUnable to remove protection as an internal error occurred, please contact a staff member.");

        player.sendMessage(Util.formatString("&aProtection successfully removed."));
    }

    private void infoCommand(Player player, Block block) {
        String status = "&7Not Protected";
        String owner = "";
        String canProtect = "&ayes";
        String acl = "- &7EVERYONE (FULL_ACCESS)";

        Protection protection = LandManager.getProtection(block);
        if (protection != null) {
            if (!protection.testAccessLevel(player, ACL.Level.VIEW, "landmanager.pinfo.any"))
                throw new CommandException("&cThis is protected but you do not have permission to view the protection Info.");

            status = "&aProtected";
            owner = Bukkit.getOfflinePlayer(protection.getOwner()).getName();
            acl = ACLCommandHandlers.renderACL(protection);
        }

        if (!LandManager.canBlockBeProtected(block.getType()))
            canProtect = "&cno";

        player.sendMessage(Util.formatString(
                "Protection status: " + status + "&r\n" +
                "Protection owned by: " + owner + "&r\n" +
                "Can block type " + block.getType().toString().toLowerCase() + " be protected: " + canProtect + "&r\n" +
                "Access Control List:\n" + acl
        ));
    }

}
