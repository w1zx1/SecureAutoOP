package com.w1zx1.autoop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoOpPlugin extends JavaPlugin implements Listener {

    private List<String> blockedCommands;
    private List<String> allowedPlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("allowplayer").setExecutor(this::onCommand);
        getLogger().info("AutoOP with restrictions enabled!");
    }

    private void loadConfigValues() {
        blockedCommands = getConfig().getStringList("blocked-commands");
        allowedPlayers = getConfig().getStringList("allowed-players")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        
        if (blockedCommands.isEmpty()) {
            blockedCommands = new ArrayList<>(Arrays.asList(
                "ban", "ban-ip", "banlist", "kick", "stop", 
                "op", "deop", "pardon", "pardon-ip", "whitelist",
                "reload", "restart", "save-all", "save-off", "save-on",
                "timings", "kill", "minecraft:kill"
            ));
            getConfig().set("blocked-commands", blockedCommands);
        }
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) {
            player.setOp(true);
            player.sendMessage(ChatColor.GOLD + "Вы получили OP-статус с ограниченными правами!");
        }
    }

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isAllowed(player)) return;

        String message = event.getMessage().toLowerCase().trim();
        String baseCommand = message.split(" ")[0]
                .replace("/", "")
                .replace("minecraft:", "")
                .replace("bukkit:", "");

        if (blockedCommands.contains(baseCommand)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Эта команда заблокирована!");
        }
    }

    private boolean isAllowed(Player player) {
        return allowedPlayers.contains(player.getName().toLowerCase()) 
            || !player.isOp();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("allowplayer")) return false;
        
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Использование: /allowplayer <ник>");
            return true;
        }

        if (!hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "Недостаточно прав!");
            return true;
        }

        String nickname = args[0].toLowerCase();
        
        if (allowedPlayers.contains(nickname)) {
            sender.sendMessage(ChatColor.YELLOW + "Игрок уже в белом списке!");
            return true;
        }

        allowedPlayers.add(nickname);
        getConfig().set("allowed-players", allowedPlayers);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Игрок " + args[0] + " добавлен в белый список!");
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return allowedPlayers.contains(player.getName().toLowerCase());
        }
        return true; // Консоль всегда имеет права
    }

    @Override
    public void onDisable() {
        getLogger().info("AutoOP with restrictions disabled!");
    }
}