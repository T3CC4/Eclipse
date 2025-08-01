package de.tecca.eclipse.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import de.tecca.eclipse.psp.PSPCommandHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class EclipseCommandManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final Map<String, PSPCommandHandler> pspCommands = new ConcurrentHashMap<>();

    public EclipseCommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        plugin.getCommand("ps").setExecutor(this);
        plugin.getCommand("ps").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ps")) {
            return handlePSCommand(sender, args);
        }

        String commandName = command.getName().toLowerCase();
        PSPCommandHandler handler = pspCommands.get(commandName);
        if (handler != null) {
            return handler.execute(sender, args);
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ps")) {
            return getPSTabComplete(sender, args);
        }

        String commandName = command.getName().toLowerCase();
        PSPCommandHandler handler = pspCommands.get(commandName);
        if (handler != null) {
            return handler.tabComplete(sender, args);
        }

        return new ArrayList<>();
    }

    private boolean handlePSCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("eclipse.admin")) {
            sender.sendMessage("§cNo permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> listPackages(sender);
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /ps info <package-id>");
                    return true;
                }
                showPackageInfo(sender, args[1]);
            }
            case "compile" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /ps compile <project-name>");
                    return true;
                }
                compileProject(sender, args[1]);
            }
            case "package" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /ps package <project-name>");
                    return true;
                }
                createPackage(sender, args[1]);
            }
            case "reload" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /ps reload <package-id>");
                    return true;
                }
                reloadPackage(sender, args[1]);
            }
            case "watch" -> showWatchStatus(sender);
            case "health" -> showHealthStatus(sender);
            case "version" -> showVersion(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    public void registerPSPCommand(String commandName, PSPCommandHandler handler) {
        if (pspCommands.containsKey(commandName.toLowerCase())) {
            plugin.getLogger().warning("Command conflict: /" + commandName);
        }

        pspCommands.put(commandName.toLowerCase(), handler);
        registerBukkitCommand(commandName, handler);
    }

    public void unregisterPSPCommand(String commandName) {
        PSPCommandHandler removed = pspCommands.remove(commandName.toLowerCase());
        if (removed != null) {
            unregisterBukkitCommand(commandName);
        }
    }

    private void registerBukkitCommand(String commandName, PSPCommandHandler handler) {
        plugin.getLogger().info("Registered PSP command: /" + commandName);
    }

    private void unregisterBukkitCommand(String commandName) {
        plugin.getLogger().info("Unregistered PSP command: /" + commandName);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== Eclipse Commands ===");
        sender.sendMessage("§f/ps list §7- List packages");
        sender.sendMessage("§f/ps info <id> §7- Package info");
        sender.sendMessage("§f/ps compile <project> §7- Compile project");
        sender.sendMessage("§f/ps package <project> §7- Create package");
        sender.sendMessage("§f/ps reload <id> §7- Reload package");
        sender.sendMessage("§f/ps watch §7- Watcher status");
        sender.sendMessage("§f/ps health §7- System health");
        sender.sendMessage("§f/ps version §7- Version info");
    }

    private void listPackages(CommandSender sender) {
        try {
            sender.sendMessage("§6=== Packages ===");
            sender.sendMessage("§7Implementation needed");
        } catch (Exception e) {
            sender.sendMessage("§cError: " + e.getMessage());
        }
    }

    private void showPackageInfo(CommandSender sender, String packageId) {
        sender.sendMessage("§6=== Package Info ===");
        sender.sendMessage("§7Package: " + packageId);
        sender.sendMessage("§7Implementation needed");
    }

    private void compileProject(CommandSender sender, String projectName) {
        sender.sendMessage("§eCompiling: " + projectName);
        sender.sendMessage("§7Implementation needed");
    }

    private void createPackage(CommandSender sender, String projectName) {
        sender.sendMessage("§eCreating package: " + projectName);
        sender.sendMessage("§7Implementation needed");
    }

    private void reloadPackage(CommandSender sender, String packageId) {
        sender.sendMessage("§eReloading: " + packageId);
        sender.sendMessage("§7Implementation needed");
    }

    private void showWatchStatus(CommandSender sender) {
        sender.sendMessage("§6=== Watcher Status ===");
        sender.sendMessage("§7Implementation needed");
    }

    private void showHealthStatus(CommandSender sender) {
        sender.sendMessage("§6=== Health Status ===");
        sender.sendMessage("§fEclipse: §aRunning");
        sender.sendMessage("§fVersion: §7" + de.tecca.eclipse.Eclipse.getVersion());
    }

    private void showVersion(CommandSender sender) {
        sender.sendMessage("§6Eclipse Framework v" + de.tecca.eclipse.Eclipse.getVersion());
    }

    private List<String> getPSTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("list", "info", "compile", "package", "reload", "watch", "health", "version");
            String partial = args[0].toLowerCase();

            for (String cmd : subCommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }

    public void shutdown() {
        new ArrayList<>(pspCommands.keySet()).forEach(this::unregisterPSPCommand);
    }
}
