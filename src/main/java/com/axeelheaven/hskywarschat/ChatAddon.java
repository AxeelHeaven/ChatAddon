package com.axeelheaven.hskywarschat;

import com.axeelheaven.hskywars.api.HSkyWarsAPI;
import com.axeelheaven.hskywars.arenas.Arena;
import com.axeelheaven.hskywars.arenas.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class ChatAddon extends JavaPlugin implements Listener {

    private HSkyWarsAPI skyWarsAPI;
    private HashMap<Player, Long> cooldown;
    private boolean placeholderAPI = false;
    private ConsoleCommandSender commandSender;

    @Override
    public void onEnable() {
        if(!this.getServer().getPluginManager().isPluginEnabled("HSkyWars")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "The HSkyWars plugin was not detected.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if(this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderAPI = true;
        }
        this.commandSender = Bukkit.getConsoleSender();
        this.cooldown = new HashMap<Player, Long>();
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
        this.skyWarsAPI = HSkyWarsAPI.getInstance();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if(event.isCancelled()) return;
        if (this.cooldown.containsKey(player) && System.currentTimeMillis() < this.cooldown.get(player) && !player.hasPermission("addon.cooldown")) {
            this.getConfig().getStringList("messages.interval").forEach(strings -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', strings.replace("<time>", this.seconds(Math.max(this.cooldown.get(player) - System.currentTimeMillis(), 0)))));
            });
            event.setCancelled(true);
        } else {
            String message = ChatColor.translateAlternateColorCodes('&', event.getMessage());
            if(!player.hasPermission("addon.chatcolor")) {
                message = ChatColor.stripColor(message);
            }
            int lowerCase = 0;
            int upperCase = 0;
            for (int i = 0; i < message.length(); i++) {
                if(Character.isUpperCase(message.charAt(i))) {
                    upperCase++;
                }
                if(Character.isLowerCase(message.charAt(i))) {
                    lowerCase++;
                }
            }
            if(upperCase > lowerCase && message.length() > this.getConfig().getInt("chat_settings.min_caps")) {
                message = String.valueOf(message.charAt(0) + message.substring(1).toLowerCase());
            }
            event.setMessage(message);
            final String format;
            if (this.skyWarsAPI.isArena(player)) {
                final Arena arena = this.skyWarsAPI.getArena(player);
                if(arena.getSpectators().contains(player)) {
                    format = this.skyWarsAPI.replace(player, this.getConfig().getString("messages.spectator"));
                } else {
                    if(arena.isArenaType(ArenaType.SOLO)) {
                        format = this.skyWarsAPI.replace(player, this.getConfig().getString("messages.solo"));
                    } else if(arena.isArenaType(ArenaType.TEAM)) {
                        format = this.skyWarsAPI.replace(player, this.getConfig().getString("messages.team"));
                    } else {
                        format = this.skyWarsAPI.replace(player, this.getConfig().getString("messages.ranked"));
                    }
                }
            } else {
                format = this.skyWarsAPI.replace(player, this.getConfig().getString("messages.lobby"));
            }
            event.getRecipients().forEach(players -> players.sendMessage(ChatColor.translateAlternateColorCodes('&', format.replace("<message>", event.getMessage()).replace("<name>", player.getName()))));
            this.commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', format.replace("<message>", event.getMessage()).replace("<name>", player.getName())));
            event.setCancelled(true);
            this.cooldown.put(player, System.currentTimeMillis() + this.getConfig().getInt("chat_settings.interval") * 1000);
        }
    }

    private String seconds(final Long time) {
        final float seconds = (time + 0.0f) / 1000.0f;
        return String.format("%1$.1f", seconds);
    }
}
