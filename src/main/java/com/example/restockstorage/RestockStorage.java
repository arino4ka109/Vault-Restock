package com.example.restockstorage;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Vault;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RestockStorage extends JavaPlugin implements Listener {

    private long cooldownMillis;
    private String cooldownStr;
    private final Set<Location> ejectingVaults = new HashSet<>();
    private final Map<Location, Long> activeCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cooldownStr = getConfig().getString("cooldown", "1min");
        cooldownMillis = parseTimeToMillis(cooldownStr);
        getServer().getPluginManager().registerEvents(this, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    "restock",
                    "Основная команда управления перезарядкой хранилищ.",
                    new RestockCommand()
            );
        });
        getLogger().info("RestockStorage Done");
    }

    @EventHandler
    public void onVaultStateChange(VaultChangeStateEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        Vault.State newState = event.getNewState();
        if (newState == Vault.State.EJECTING) {
            ejectingVaults.add(loc);
        }
        else if ((newState == Vault.State.ACTIVE || newState == Vault.State.INACTIVE) && ejectingVaults.contains(loc)) {
            ejectingVaults.remove(loc);
            activeCooldowns.put(loc, System.currentTimeMillis() + cooldownMillis);
            long ticks = cooldownMillis / 50L;
            if (ticks <= 0) ticks = 1L;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (activeCooldowns.containsKey(loc)) {
                    resetVault(block);
                    activeCooldowns.remove(loc);
                }
            }, ticks);
        }
    }

    private void resetVault(Block block) {
        if (block.getType() != Material.VAULT) return;
        if (block.getState() instanceof org.bukkit.block.Vault) {
            org.bukkit.block.Vault vaultState = (org.bukkit.block.Vault) block.getState();
            List<UUID> rewarded = new ArrayList<>(vaultState.getRewardedPlayers());
            for (UUID uuid : rewarded) {
                vaultState.removeRewardedPlayer(uuid);
            }
            vaultState.update(true, false);
        }
    }

    private long parseTimeToMillis(String input) {
        try {
            if (input.endsWith("tick")) return Long.parseLong(input.replace("tick", "")) * 50L;
            if (input.endsWith("sec")) return Long.parseLong(input.replace("sec", "")) * 1000L;
            if (input.endsWith("min")) return Long.parseLong(input.replace("min", "")) * 60L * 1000L;
            if (input.endsWith("day")) return Long.parseLong(input.replace("day", "")) * 24L * 60L * 60L * 1000L;
            if (input.endsWith("week")) return Long.parseLong(input.replace("week", "")) * 7L * 24L * 60L * 60L * 1000L;
            if (input.endsWith("mon")) return Long.parseLong(input.replace("mon", "")) * 30L * 24L * 60L * 60L * 1000L;
        } catch (NumberFormatException e) {
            return -1;
        }
        return -1;
    }

    private class RestockCommand implements BasicCommand {
        @Override
        public void execute(CommandSourceStack source, String[] args) {
            CommandSender sender = source.getSender();
            if (args.length == 0) {
                sender.sendMessage("§b[Vault time: " + cooldownStr + "] §eДоступные команды:\n" +
                        "§7/restock time <время> §8- Изменить время отката\n" +
                        "§7/restock storage §8- Обновить хранилище, на которое вы смотрите");
                return;
            }
            if (args[0].equalsIgnoreCase("time")) {
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /restock time 1tick 1sec 1min 1day 1week 1mon");
                    return;
                }
                String input = args[1].toLowerCase();
                long parsedTime = parseTimeToMillis(input);
                if (parsedTime == -1) {
                    sender.sendMessage("§cОшибка: Неверный формат времени!");
                    return;
                }
                cooldownMillis = parsedTime;
                cooldownStr = input;
                getConfig().set("cooldown", input);
                saveConfig();
                sender.sendMessage("§bТекущее время перезарядки Vault: " + input);
                return;
            }
            if (args[0].equalsIgnoreCase("storage")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cТолько игрок может использовать данную команду!.");
                    return;
                }
                Player player = (Player) sender;
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || targetBlock.getType() != Material.VAULT) {
                    player.sendMessage("§cОшибка: Вы должны смотреть на Хранилище!");
                    return;
                }
                resetVault(targetBlock);
                activeCooldowns.remove(targetBlock.getLocation());
                ejectingVaults.remove(targetBlock.getLocation());
                player.sendMessage("§bVault обновлён");
            }
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            List<String> suggestions = new ArrayList<>();
            if (args.length == 1) {
                String current = args[0].toLowerCase();
                if (current.isEmpty()) {
                    suggestions.add("time");
                    suggestions.add("storage");
                } else {
                    if ("time".startsWith(current)) suggestions.add("time");
                    if ("storage".startsWith(current)) suggestions.add("storage");
                }
            }
            return suggestions;
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return sender.hasPermission("restockstorage.admin");
        }
    }
}