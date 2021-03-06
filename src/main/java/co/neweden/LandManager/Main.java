package co.neweden.LandManager;

import co.neweden.LandManager.Commands.LandCommands;
import co.neweden.LandManager.Commands.ProtectionCommands;
import co.neweden.LandManager.Commands.UtilCommands;
import co.neweden.LandManager.Listeners.*;
import co.neweden.menugui.MenuGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        LandManager.plugin = this;
        new UtilCommands(); new LandCommands(); new ProtectionCommands();
        new LocationEvents(); new InteractEvents(); new BlockEvents(); new MenuEvents(); new PlayerEvents();
        LandManager.protections = new Protections();
        if (!startup()) getServer().getPluginManager().disablePlugin(this);
    }

    private boolean startup() {
        saveDefaultConfig();
        if (!loadDBConnection() || !setupDB() || !loadClaims() || !setupMenu())
            return false;
        getServer().getOnlinePlayers().forEach(LandManager::updatePlayerCache);
        return true;
    }

    public boolean reload() {
        getLogger().info("Reloading LandManager");
        reloadConfig();
        LandManager.landClaims.clear();
        if (!LandManager.landClaims.isEmpty()) return false;
        LandManager.protections().blockProtections.clear();
        if (!LandManager.protections().blockProtections.isEmpty()) return false;
        boolean umenu = MenuGUI.unloadMenu(LandManager.landListMenu);
        if (!umenu) return false;
        try {
            LandManager.db.close();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Unable to close database connection", e);
            return false;
        }
        return startup();
    }

    private boolean loadDBConnection() {
        String host = getConfig().getString("mysql.host", null);
        String port = getConfig().getString("mysql.port", null);
        String database = getConfig().getString("mysql.database", null);
        if (host == null || port == null || database == null) {
            getLogger().log(Level.INFO, "No database information received from config.");
            return false;
        }

        String url = String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, database);

        try {
            LandManager.db = DriverManager.getConnection(url, getConfig().getString("mysql.user", ""), getConfig().getString("mysql.password", ""));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An SQLException occurred while trying to connect to the database.", e);
            return false;
        }
        getLogger().log(Level.INFO, "Connected to MySQL Database");
        return true;
    }

    private boolean setupDB() {
        try {
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `protections` (\n" +
                    "  `protection_id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `world` VARCHAR(128) NULL,\n" +
                    "  `x` INT NULL,\n" +
                    "  `y` INT NULL,\n" +
                    "  `z` INT NULL,\n" +
                    "  `owner` VARCHAR(36) NOT NULL,\n" +
                    "  `everyone_acl_level` VARCHAR(32) NULL,\n" +
                    "  PRIMARY KEY (`protection_id`)\n" +
                    ");\n"
            );
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `protections_acl` (\n" +
                    "  `protection_id` INT NOT NULL,\n" +
                    "  `uuid` VARCHAR(36) NOT NULL,\n" +
                    "  `level` VARCHAR(32) NOT NULL\n" +
                    ");"
            );
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `landclaims` (\n" +
                    "  `land_id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `displayName` VARCHAR(128),\n" +
                    "  `owner` VARCHAR(36) NOT NULL,\n" +
                    "  `everyone_acl_level` VARCHAR(32) NULL,\n" +
                    "  `home_location` VARCHAR(64) NOT NULL,\n" +
                    "  `icon_material` VARCHAR(128) NULL,\n" +
                    "  PRIMARY KEY (`land_id`)\n" +
                    ");\n"
            );
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `landclaims_acl` (\n" +
                    "  `land_id` INT NOT NULL,\n" +
                    "  `uuid` VARCHAR(36) NOT NULL,\n" +
                    "  `level` VARCHAR(32) NOT NULL\n" +
                    ");"
            );
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `chunks` (\n" +
                    "  `chunk_id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `land_id` INT NOT NULL,\n" +
                    "  `world` VARCHAR(128) NOT NULL,\n" +
                    "  `x` INT NOT NULL,\n" +
                    "  `z` INT NOT NULL,\n" +
                    "  PRIMARY KEY (`chunk_id`)\n" +
                    ");\n"
            );
            LandManager.db.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `players` (\n" +
                    "  `uuid` VARCHAR(36) NOT NULL,\n" +
                    "  `claim_limit_cache` INT NOT NULL,\n" +
                    "  PRIMARY KEY (`uuid`)\n" +
                    ");\n"
            );
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Unable to setup database", e);
            return false;
        }
        return true;
    }

    private boolean loadClaims() {
        try {
            ResultSet rs = LandManager.db.createStatement().executeQuery("SELECT * FROM landclaims;");
            while (rs.next()) {
                int land_id = rs.getInt("land_id");
                LandClaim claim = new LandClaim(land_id);
                claim.displayName = rs.getString("displayName");

                try {
                    claim.owner = UUID.fromString(rs.getString("owner"));
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Land Claim #" + land_id + ": UUID \"" + rs.getString("owner") + "\" is not valid, Land Claim will not be loaded.");
                    continue;
                }

                try {
                    if (rs.getString("everyone_acl_level") != null)
                        claim.everyoneAccessLevel = ACL.Level.valueOf(rs.getString("everyone_acl_level"));
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Land Claim #" + land_id + ": Value \"" + rs.getString("everyone_acl_level") + "\" is not valid, default Level will be used instead.");
                }

                Location homeLocation = Util.locationFromString(rs.getString("home_location"));
                if (homeLocation == null) {
                    getLogger().log(Level.SEVERE, "Land Claim #" + land_id + ": Home Location is not valid, Land Claim will not be loaded.");
                    continue;
                }
                claim.homeLocation = homeLocation;

                try {
                    if (rs.getString("icon_material") != null)
                        claim.setIconMaterial(Material.valueOf(rs.getString("icon_material")));
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Land Claim #" + land_id + ": Icon Material \"" + rs.getString("icon_material") + "\" is not valid, default value will be used instead.");
                }

                LandManager.landClaims.put(land_id, claim);
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to load Land Claims from the Database.", e);
            return false;
        }
        getLogger().log(Level.INFO, "Loaded " + LandManager.landClaims.size() + " Land Claims from the Database.");
        return true;
    }

    private boolean setupMenu() {
        LandManager.landListMenu = MenuGUI.newMenu("landlist");
        LandManager.landListMenu.setNumRows(6);
        LandManager.landListMenu.setTitle("Select a Land Claim to teleport to it");
        return true;
    }

}
