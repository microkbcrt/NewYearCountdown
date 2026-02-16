package com.example.nyc;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class NewYearPlugin extends JavaPlugin implements CommandExecutor {

    private BukkitRunnable task;
    private BossBar bossBar;
    private boolean isRunning = false;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // 注册指令
        if (getCommand("ny") != null) {
            getCommand("ny").setExecutor(this);
        }
        getLogger().info("跨年倒计时插件 (适配 1.21.11) 已加载！");
    }

    @Override
    public void onDisable() {
        stopEvent();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ny.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令。");
            return true;
        }

        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("start")) {
            startCountdown();
            return true;
        } else if (args[0].equalsIgnoreCase("stop")) {
            stopEvent();
            sender.sendMessage(ChatColor.YELLOW + "跨年活动已停止。");
            return true;
        }

        return false;
    }

    private void startCountdown() {
        if (isRunning) {
            stopEvent();
        }
        isRunning = true;

        // 创建 BossBar
        bossBar = Bukkit.createBossBar("跨年倒计时", BarColor.BLUE, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }

        // 倒计时任务
        task = new BukkitRunnable() {
            int timeLeft = 60;

            @Override
            public void run() {
                // 如果中途有玩家加入，确保他们能看到 BossBar
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(p)) {
                        bossBar.addPlayer(p);
                    }
                }

                // 更新 BossBar
                bossBar.setTitle(ChatColor.GOLD + "跨年倒计时: " + ChatColor.AQUA + timeLeft + "秒");
                bossBar.setProgress(Math.max(0.0, timeLeft / 60.0));

                // 流程逻辑
                if (timeLeft > 30) {
                    // 前30秒：聊天栏通知
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&e&l跨年倒计时: &b" + timeLeft + "秒"));
                    playTickSound();
                } else if (timeLeft > 15) {
                    // 30-16秒：中间留白，只更新 BossBar，增加紧张感
                    // 也可以选择继续发消息，这里保持安静
                } else if (timeLeft > 0) {
                    // 最后15秒：屏幕标题 + 音效
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.RED + String.valueOf(timeLeft), "", 0, 25, 5);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2.0f); // 高音
                    }
                } else {
                    // 0秒：倒计时结束，开始烟花
                    startFireworks();
                    this.cancel(); // 结束当前倒计时任务
                    return;
                }

                timeLeft--;
            }
        };
        task.runTaskTimer(this, 0L, 20L); // 每秒执行一次
    }

    private void startFireworks() {
        // 1. 发送全服标题
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.RED + "新年快乐！", ChatColor.GOLD + "Happy New Year!", 10, 100, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1f);
        }

        // 2. 锁定时间为午夜
        if (Bukkit.getWorlds().size() > 0) {
            Bukkit.getWorlds().get(0).setTime(18000); // 18000 = 午夜
            Bukkit.getWorlds().get(0).setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // 锁定时间
        }
        
        // 移除 BossBar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        // 3. 开启烟花循环任务
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    this.cancel();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    spawnFirework(p.getLocation());
                }
            }
        };
        task.runTaskTimer(this, 0L, 20L); // 每秒 (20 ticks) 放一次
    }

    private void stopEvent() {
        isRunning = false;
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        // 恢复时间流动（可选，如果需要的话）
        if (Bukkit.getWorlds().size() > 0) {
            Bukkit.getWorlds().get(0).setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }
    }

    private void playTickSound() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
        }
    }

    private void spawnFirework(Location location) {
        Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        // 随机颜色生成
        Color c1 = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        Color c2 = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));

        FireworkEffect.Type type = FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)];

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true) // 强制闪烁
                .withColor(c1)
                .withFade(c2)
                .with(type)
                .trail(random.nextBoolean())
                .build();

        fwm.addEffect(effect);
        fwm.setPower(1 + random.nextInt(2)); // 飞行高度 1-2
        fw.setFireworkMeta(fwm);
    }
}
