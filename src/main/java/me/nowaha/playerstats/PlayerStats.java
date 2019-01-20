package me.nowaha.playerstats;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public final class PlayerStats extends PluginBase {

    private static Economy economy;

    public static PlayerStats instance;

    FileConfiguration playerStats;

    boolean useBalance = false;
    LuckPermsApi LPAPI;
    boolean useLuckPerms = false;
    boolean usePex = false;

    String message = "";

    @Override
    public void enable() {
        // Enable logic

        instance = this;

        getServer().getPluginManager().registerEvents(new Events(), this);

        if (setupEconomy()) {
            getLogger().info("Hooked to Vault!");
            useBalance = true;
        } else {
            getLogger().info("Failed to hook to Vault: not using Balance.");
            useBalance = false;
        }

        if (getServer().getPluginManager().getPlugin("PermissionsEx") != null) {
            getLogger().info("Hooked to PermissionsEx!");
            usePex = true;
            useLuckPerms = false;
        } else if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPermsApi> provider = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class);
            if (provider != null) {
                LPAPI = provider.getProvider();

                getLogger().info("Hooked to LuckPerms!");
                usePex = false;
                useLuckPerms = true;
            } else {
                getLogger().info("Failed to hook to ranking plugin: not using Rank.");
            }
        } else {
            getLogger().info("Failed to hook to ranking plugin: not using Rank.");
            usePex = false;
            useLuckPerms = false;
        }

        message = getLocale("divider") +
                "\n" + getLocale("player") + " %s";

        if (usePex || useLuckPerms) {
            message += "\n" + getLocale("rank") + " %s";
        }

        if (useBalance) {
            message += "\n" + getLocale("balance.balance") + " " + getLocale("balance.currencytype") + "%s";
        }

        message += "\n" + getLocale("kills") + " %s" +
                "\n" + getLocale("deaths") + " %s" +
                "\n" + getLocale("divider");


        playerStats = new FileConfiguration(this, "killsdeaths.yml");
        if (playerStats.exists()) {
            playerStats.load(); // Load the configuration from the file
        } else {
            playerStats.setHeader("These are the player's kills and deaths.");
            playerStats.save();
        }
    }

    @Override
    public void disable() {
        // Disable logic
    }

    public User loadUser(Player player) {
        // assert that the player is online
        if (!player.isOnline()) {
            throw new IllegalStateException("Player is offline");
        }

        return LPAPI.getUserManager().getUser(player.getUniqueId());
    }

    @Override
    public boolean command(CommandSender sender, String command, String[] args) {
        if (command.equalsIgnoreCase("stats")) {
            if (args.length < 1) {
                if (sender instanceof Player) {
                    if (sender.hasPermission("stats.self")) {
                        getPlayerStatsFor((Player) sender, sender);
                    } else {
                        sender.sendMessage(getLocale("errors.noperms"));
                    }
                } else {
                    sender.sendMessage(getLocale("errors.playeronly"));
                }
            } else {
                if (!sender.hasPermission("stats.other")) {
                    sender.sendMessage(getLocale("errors.noperms"));
                    return true;
                }

                if (Bukkit.getPlayer(args[0]) != null) {
                    getPlayerStatsFor(Bukkit.getPlayer(args[0]), sender);
                } else {
                    sender.sendMessage(getLocale("errors.playernotfound"));
                }
            }
        }
        return true;
    }

    void getPlayerStatsFor(Player player, CommandSender forPlayer) {
        String username = player.getName();
        String rank = "NONE";
        if (usePex) {
            PermissionUser user = PermissionsEx.getUser(player);
            rank = user.getParentIdentifiers().get(0);
        } else if (useLuckPerms) {
            User user = loadUser(player);
            rank = user.getPrimaryGroup();
        }
        String balance = "NONE";

        if (useBalance) {
            balance = economy.getBalance(player) + "";
        }
        String kills;

        try {
            kills = playerStats.get(player.getUniqueId() + ".kills").toString();
        } catch (Exception ex) {
            playerStats.set(player.getUniqueId() + ".kills", 0);
            kills = "0";

            playerStats.save();
        }

        String deaths;

        try {
            deaths = playerStats.get(player.getUniqueId() + ".deaths").toString();
        } catch (Exception ex) {
            playerStats.set(player.getUniqueId() + ".deaths", 1);
            deaths = "1";

            playerStats.save();
        }

        if (usePex || useLuckPerms) {
            if (useBalance) {
                forPlayer.sendMessage(String.format(message, username, rank, balance, kills, deaths));
            } else {
                forPlayer.sendMessage(String.format(message, username, rank, kills, deaths));
            }
        } else if (useBalance) {
            forPlayer.sendMessage(String.format(message, username, balance, kills, deaths));
        } else {
            forPlayer.sendMessage(String.format(message, username, kills, deaths));
        }
    }

    @Override
    public int getMinimumLibVersion() {
        return Common.VERSION;
    }

    @Override
    public void localization() {

        loadLocale("divider", ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "------------------------------------------");
        loadLocale("player", ChatColor.GREEN + "Player" + ChatColor.WHITE + ":");
        loadLocale("rank", ChatColor.BLUE + "Rank" + ChatColor.WHITE + ":");
        loadLocale("balance.balance", ChatColor.YELLOW + "Balance" + ChatColor.WHITE + ":");
        loadLocale("balance.currencytype", "$");
        loadLocale("kills", ChatColor.GREEN + "Kills" + ChatColor.WHITE + ":");
        loadLocale("deaths", ChatColor.RED + "Deaths" + ChatColor.WHITE + ":");

        loadLocale("errors.playernotfound", ChatColor.RED + "Player not found.");
        loadLocale("errors.playeronly", ChatColor.RED + "The console cannot use this command.");
        loadLocale("errors.noperms", ChatColor.RED + "You cannot do that.");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if(economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

}
