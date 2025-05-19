package com.w1zx1.autoop;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AutoOpPlugin extends JavaPlugin implements Listener {

    private List<String> blockedCommands;
    private List<String> allowedPlayers;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Map<String, String> messages = new HashMap<>();
    private String language;
    private boolean blockCommandBlocks;
    private boolean logToConsole;
    private boolean logToFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("allowplayer").setExecutor(this);
        getLogger().info("[AutoOP] Plugin loaded!");
    }

    private void loadConfigValues() {
        language = getConfig().getString("language", "ru").toLowerCase();
        blockCommandBlocks = getConfig().getBoolean("settings.block-command-blocks", true);
        logToConsole = getConfig().getBoolean("logging.console", true);
        logToFile = getConfig().getBoolean("logging.file", true);

        blockedCommands = getConfig().getStringList("blocked-commands");
        allowedPlayers = getConfig().getStringList("allowed-players")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Map<String, String> defaultMessages = new HashMap<String, String>() {{
            put("op-grant", "&6Вы получили OP-статус с ограниченными правами!");
            put("command-blocked", "&cЭта команда заблокирована!");
            put("player-added", "&aИгрок %s добавлен в белый список!");
            put("player-exists", "&eИгрок уже в белом списке!");
            put("no-permission", "&cНедостаточно прав!");
            put("usage", "&cИспользование: /allowplayer <ник>");
        }};

        String messagesPath = "messages." + language + ".";
        for (String key : defaultMessages.keySet()) {
            messages.put(key, getConfig().getString(messagesPath + key, defaultMessages.get(key)));
        }

        if (blockedCommands.isEmpty()) {
            blockedCommands = Arrays.asList(
                "ban", "ban-ip", "banlist", "kick", "stop", 
                "op", "deop", "pardon", "pardon-ip", "whitelist",
                "reload", "restart", "save-all", "save-off", "save-on",
                "timings", "kill"
            );
            getConfig().set("blocked-commands", blockedCommands);
        }
        
        saveConfig();
    }

    private String getMessage(String key, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', 
            String.format(messages.getOrDefault(key, ""), args));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) {
            player.setOp(true);
            logAction("OP_GRANT", player.getName(), "");
            player.sendMessage(getMessage("op-grant"));
        }
    }

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isAllowed(player)) return;

        String baseCommand = normalizeCommand(event.getMessage().split(" ")[0]);
        if (isBlockedCommand(baseCommand)) {
            event.setCancelled(true);
            player.sendMessage(getMessage("command-blocked"));
            logAction("BLOCKED_CMD", player.getName(), event.getMessage());
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (!blockCommandBlocks) return;
        
        if (event.getSender() instanceof BlockCommandSender) {
            String baseCommand = normalizeCommand(event.getCommand().split(" ")[0]);
            if (isBlockedCommand(baseCommand)) {
                event.setCancelled(true);
                logAction("BLOCKED_CMD_BLOCK", "CommandBlock", event.getCommand());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("allowplayer")) return false;
        
        if (args.length != 1) {
            sender.sendMessage(getMessage("usage"));
            return true;
        }

        if (!hasPermission(sender)) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        String nickname = args[0].toLowerCase();
        if (allowedPlayers.contains(nickname)) {
            sender.sendMessage(getMessage("player-exists"));
            return true;
        }

        allowedPlayers.add(nickname);
        getConfig().set("allowed-players", allowedPlayers);
        saveConfig();
        sender.sendMessage(getMessage("player-added", args[0]));
        return true;
    }

    private boolean isAllowed(Player player) {
        return allowedPlayers.contains(player.getName().toLowerCase());
    }

    private boolean hasPermission(CommandSender sender) {
        return !(sender instanceof Player) || 
               allowedPlayers.contains(((Player) sender).getName().toLowerCase());
    }

    private String normalizeCommand(String command) {
        return command.replace("/", "")
                      .replace("minecraft:", "")
                      .replace("bukkit:", "")
                      .toLowerCase();
    }

    private boolean isBlockedCommand(String baseCommand) {
        return blockedCommands.contains(baseCommand);
    }

    private void logAction(String type, String source, String command) {
        String log = String.format("[%s] %s | Источник: %s | Команда: %s",
            dtf.format(LocalDateTime.now()),
            type,
            source,
            command);

        if (logToConsole) {
            switch (type) {
                case "BLOCKED_CMD":
                case "BLOCKED_CMD_BLOCK":
                    getLogger().warning(log);
                    break;
                default:
                    getLogger().info(log);
            }
        }

        if (logToFile) {
            try {
                Files.write(Paths.get(getDataFolder().toString(), "logs.txt"),
                    (log + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            } catch (Exception e) {
                getLogger().severe("Ошибка записи лога: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[AutoOP] Plugin unloaded!");
    }
}