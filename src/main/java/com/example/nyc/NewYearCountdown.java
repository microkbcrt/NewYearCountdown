package com.example.nyc;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class NewYearCountdown extends JavaPlugin implements CommandExecutor {

    private BossBar bossBar;
    private BukkitRunnable countdownTask;
    private BukkitRunnable fireworkTask;
    private boolean isRunning = false;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        if (getCommand("nyc") != null) {
            getCommand("nyc").setExecutor(this);
        }
        getLogger().info("跨年倒计时插件 (1.21.1) 已加载！");
    }

    @Override
    public void onDisable() {
        stopAll();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nyc.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令。");
            return true;
        }

        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("start")) {
            if (isRunning) {
                sender.sendMessage(ChatColor.RED + "倒计时已经在运行中！请先输入 /nyc stop");
                return true;
            }
            startCountdown();
            sender.sendMessage(ChatColor.GREEN + "跨年倒计时已开始！");
            return true;
        } else if (args[0].equalsIgnoreCase("stop")) {
            stopAll();
            sender.sendMessage(ChatColor.YELLOW + "倒计时/烟花已停止，插件已重置。");
            return true;
        }

        return false;
    }

    private void startCountdown() {
        isRunning = true;

        // 创建 BossBar: 初始绿色，10段
        bossBar = Bukkit.createBossBar("准备开始...", BarColor.GREEN, BarStyle.SEGMENTED_10);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }

        countdownTask = new BukkitRunnable() {
            int timeLeft = 60;

            @Override
            public void run() {
                // 自动把中途进服的玩家加入 BossBar
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(p)) {
                        bossBar.addPlayer(p);
                    }
                }

                // 更新 BossBar 文字和进度
                bossBar.setTitle("距离新春还有 " + timeLeft + " 秒");
                bossBar.setProgress(Math.max(0.0, timeLeft / 60.0));

                // === 阶段控制 ===
                
                // 阶段1: 60秒-31秒 (绿色)
                if (timeLeft > 30) {
                    bossBar.setColor(BarColor.GREEN);
                }
                // 阶段2: 30秒-16秒 (黄色，开始刷屏)
                else if (timeLeft > 15) {
                    bossBar.setColor(BarColor.YELLOW);
                    Bukkit.broadcastMessage(ChatColor.RED + "新春倒计时: " + ChatColor.YELLOW + timeLeft);
                }
                // 阶段3: 15秒-1秒 (红色，大标题，高音钢琴声)
                else if (timeLeft > 0) {
                    bossBar.setColor(BarColor.RED);
                    Bukkit.broadcastMessage(ChatColor.RED + "新春倒计时: " + ChatColor.GOLD + ChatColor.BOLD + timeLeft);
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // Title 显示
                        p.sendTitle(ChatColor.GOLD + "距离新春还剩", ChatColor.RED + String.valueOf(timeLeft) + " 秒", 0, 25, 5);
                        // 播放高音钢琴声 (Pitch 2.0 = F# 高八度，非常清脆)
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 10f, 2.0f);
                    }
                }
                // 阶段4: 0秒 (结束)
                else {
                    startCelebration();
                    this.cancel();
                    return;
                }

                timeLeft--;
            }
        };
        // 0延迟，每20tick(1秒)执行一次
        countdownTask.runTaskTimer(this, 0L, 20L);
    }

    private void startCelebration() {
        // 移除 BossBar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        // 发送全服红色 Title
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.RED + "§l新春快乐！", ChatColor.GOLD + "万事如意，心想事成！", 10, 100, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 10f, 1.0f);
        }

        Bukkit.broadcastMessage(ChatColor.RED + "§l=========================");
        Bukkit.broadcastMessage(ChatColor.RED + "§l      新 春 快 乐！       ");
        Bukkit.broadcastMessage(ChatColor.RED + "§l=========================");

        // 启动无限烟花任务
        fireworkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    spawnRandomFirework(p.getLocation());
                }
            }
        };
        fireworkTask.runTaskTimer(this, 0L, 20L); // 每秒执行一次
    }

    private void spawnRandomFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fm = fw.getFireworkMeta();

        FireworkEffect.Type type = FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)];
        Color c1 = getRandomColor();
        Color c2 = getRandomColor();

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(random.nextBoolean())
                .withColor(c1)
                .withFade(c2)
                .with(type)
                .trail(random.nextBoolean())
                .build();

        fm.addEffect(effect);
        // Power 1 (约0.5秒) 或 Power 2 (约1秒)，让它飞一会儿
        fm.setPower(random.nextInt(2) + 1);

        fw.setFireworkMeta(fm);
    }

    private Color getRandomColor() {
        return Color.fromBGR(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    private void stopAll() {
        isRunning = false;
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        if (fireworkTask != null && !fireworkTask.isCancelled()) {
            fireworkTask.cancel();
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }
}
