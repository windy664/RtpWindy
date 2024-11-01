package org.windy.rtpWindy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
public class RtpWindy extends JavaPlugin {
    private int rangeX, rangeZ, maxAttempts;
    private Location center;
    private List<String> commands;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        displayPluginInfo(true); // Display on enable
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        rangeX = config.getInt("Range.x");
        rangeZ = config.getInt("Range.z");
        String[] centerCoords = config.getString("center").split(":");
        center = new Location(
                Bukkit.getWorlds().get(0), // or specify a world if needed
                Double.parseDouble(centerCoords[0]),
                Double.parseDouble(centerCoords[1]),
                Double.parseDouble(centerCoords[2])
        );
        maxAttempts = config.getInt("Times");
        commands = config.getStringList("Commands");
        debug = config.getBoolean("debug",false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("sjtp") && sender instanceof Player) {
            Player player = (Player) sender;

            // Send title to indicate the teleportation process has started
            player.sendTitle("§e正在寻找安全坐标...", "请稍候", 10, 70, 20);

            // Start the teleportation process
            teleportPlayer(player);
            return true;
        }
        return false;
    }


    private void teleportPlayer(Player player) {
        new BukkitRunnable() {
            int attempts = 0;
            Random random = new Random();
            World world = player.getWorld();

            @Override
            public void run() {
                if (attempts >= maxAttempts) {
                    player.sendMessage("§c未能找到安全的传送位置，请稍后再试！");
                    this.cancel();
                    return;
                }

                // Calculate random coordinates
                int x = center.getBlockX() + random.nextInt(2 * rangeX) - rangeX;
                int z = center.getBlockZ() + random.nextInt(2 * rangeZ) - rangeZ;
                int y = world.getHighestBlockYAt(x, z);
                Location targetLocation = new Location(world, x + 0.5, y, z + 0.5);
                Material blockType = targetLocation.getBlock().getType();

                // Check if the block is safe
                if (isSafeBlock(blockType)) {
                    // Schedule the teleport and any subsequent actions on the main thread
                    Bukkit.getScheduler().runTask(RtpWindy.this, () -> {
                        startParachuteDescent(player, targetLocation.add(0, 50, 0)); // Start descent 50 blocks above target
                        executeCommands(player);
                        player.sendMessage("§a你已成功随机传送！");
                    });
                    this.cancel(); // Stop further attempts once teleport is successful
                }

                attempts++;
            }
        }.runTaskTimer(this, 0L, 5L); // Keep this on the main thread
    }

    private void startParachuteDescent(Player player, Location targetLocation) {
        log("开始为玩家 " + player.getName() + " 启动降落伞降落。");

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, false, false));
        log("已为玩家添加慢速下降效果。");

        // 将玩家传送到目标位置上方50格
        player.teleport(targetLocation.add(0, 25, 0));
        log("玩家已传送到目标位置上方50格。");

        // 设置无敌时间来避免落地伤害
        player.setNoDamageTicks(100);  // 5秒无敌时间（100 ticks）
        log("设置玩家无敌状态，防止降落过程中受到坠落伤害。");

// 定时检查玩家是否落地
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    Location playerLocation = player.getLocation();
                    Block blockBelow = playerLocation.subtract(0, 1, 0).getBlock(); // 获取玩家脚下的方块

                    // 检查玩家脚下的方块是否是地面方块
                    if (blockBelow.getType().isSolid()) {
                        log("玩家已落地，移除慢速下降效果，确保平安落地。");
                        player.removePotionEffect(PotionEffectType.SLOW_FALLING); // 移除慢速下降效果
                        this.cancel(); // 停止任务
                    } else {
                        // 如果玩家不在地面上，继续检查
                        log("玩家仍在空中，继续等待。");
                    }
                }
            }
        }.runTaskTimer(this, 1L, 1L);  // 每tick检查一次
    }



    private boolean isSafeBlock(Material blockType) {
        return blockType != Material.WATER && blockType != Material.LAVA;
    }



    private void executeCommands(Player player) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        for (String command : commands) {
            // Use PlaceholderAPI to parse placeholders
            String parsedCommand = PlaceholderAPI.setPlaceholders(player, command);
            Bukkit.dispatchCommand(console, parsedCommand);
        }
    }

    private void displayPluginInfo(boolean isEnabled) {
        String pluginVersion = this.getDescription().getVersion();
        String minecraftVersion = Bukkit.getVersion();
        String serverType = Bukkit.getServer().getName();

        // ASCII Art with Colors and aligned information
        String art = "\n§b  _____      _     §3______   __        __  §b   ____   __     __\n" +
                " §b|  __ \\    | |   §3|  ____|  \\ \\      / /  §b |  _ \\  \\ \\   / /\n" +
                " §b| |__) | __| |_  §3| |__      \\ \\ /\\ / /   §b| |_) |  \\ \\_/ / \n" +
                " §b|  _  / / _` | | §3|  __|      \\ V  V /    §b|  _ <    \\   /  \n" +
                " §b| | \\ \\| (_| | |_| §3| |____     \\_/\\_/     §b| |_) |    | |   \n" +
                " §b|_|  \\_\\\\__,_|\\__,_|______|                 §b|____/     |_|   \n\n" +
                " §a\n"+
                " §a| 插件版本    : §f" + pluginVersion + "\n" +
                " §a| 游戏版本 : §f" + minecraftVersion + "\n" +
                " §a| 服务器核心       : §f" + serverType + "\n" +
                " §a| 状态            : §f" + (isEnabled ? "§2已启用" : "§4已关闭") + "\n" +
                " §a";

        Bukkit.getConsoleSender().sendMessage(art);
    }
    @Override
    public void onDisable() {
        displayPluginInfo(false); // Display on disable
    }

    void log(String message){
        if(debug){
            this.getLogger().info(message);
        }
    }
}