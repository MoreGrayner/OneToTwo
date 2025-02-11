package org.targermatch.formarkchase.onetotwo;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OneToTwo extends JavaPlugin implements Listener {
    private final Map<Player, Player> playerGroups = new HashMap<>();
    private final Map<Player, Integer> groupDeathCount = new HashMap<>();
    private final Map<Player, Long> lastUsedTime = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("assigngroups").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.isOp()) {
                    assignRandomGroups();
                    player.sendMessage("팀원이 정상적으로 배정되었습니다.");
                    startActionBarUpdate();
                } else {
                    player.sendMessage(ChatColor.RED + "명령어 사용이 금지됩니다.");
                }
            }
            return true;
        });
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.getInventory().addItem(new ItemStack(Material.COMPASS));
    }

    // 랜덤으로 2명씩 그룹을 묶는 메소드
    public void assignRandomGroups() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() % 2 != 0) {
            players.add(null);
        }

        for (int i = 0; i < players.size(); i += 2) {
            Player player1 = players.get(i);
            Player player2 = players.get(i + 1);

            if (player1 != null && player2 != null) {
                playerGroups.put(player1, player2);
                playerGroups.put(player2, player1);
            }
        }

        for (Player player : playerGroups.keySet()) {
            groupDeathCount.put(player, 0);
        }
    }

    public void startActionBarUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : playerGroups.keySet()) {
                    if (player.isOnline()) {
                        Player groupMate = playerGroups.get(player);
                        String message = groupMate != null ? "지정된 그룹원: " + groupMate.getName() : "지정된 그룹원: " + player.getName();
                        TextComponent actionBarMessage = new TextComponent(ChatColor.GOLD + message); //형 변환을 위해 TextComponent 필요
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBarMessage);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 40L);
    }



    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (playerGroups.containsKey(player)) {
                Player groupMate = playerGroups.get(player);
                double damage = event.getDamage();

                if (groupMate != null && groupMate.isOnline()) {
                    groupMate.damage(damage);
                    event.setCancelled(true);
                }
            }
        }
    }


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (playerGroups.containsKey(player)) {
                Player groupMate = playerGroups.get(player);

                groupDeathCount.put(player, groupDeathCount.get(player) + 1);
                if (groupMate != null && groupMate.isOnline()) {
                    groupDeathCount.put(groupMate, groupDeathCount.get(groupMate) + 1);
                    groupMate.setHealth(0);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            Player player = event.getPlayer();

            long currentTime = System.currentTimeMillis();
            long cooldownTime = 10000;

            if (lastUsedTime.containsKey(player)) {
                long lastTime = lastUsedTime.get(player);
                if (currentTime - lastTime < cooldownTime) {
                    long remainingTime = cooldownTime - (currentTime - lastTime);
                    player.sendMessage(ChatColor.RED + "쿨다운 중입니다... " + (remainingTime / 1000) + "초.");
                    return;
                }
            }

            lastUsedTime.put(player, currentTime);
            if (playerGroups.containsKey(player)) {
                Player groupMate = playerGroups.get(player);
                if (groupMate != null && groupMate.isOnline()) {
                    player.sendMessage(player.getName() + " 님의 그룹원 " + groupMate.getName() + " 님의 좌표: " +
                            groupMate.getLocation().getBlockX() + ", " +
                            groupMate.getLocation().getBlockY() + ", " +
                            groupMate.getLocation().getBlockZ());
                }
            }
        }
    }

}

