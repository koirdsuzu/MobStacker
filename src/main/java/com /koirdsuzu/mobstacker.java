# 参考：KUNLab
# 過去バージョンのを勝手にアレンジしたものです
package com.koirdsuzu.mobstacker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MobStackerPlugin extends JavaPlugin implements Listener {

    private int stackLimit;

    @Override
    public void onEnable() {
        // config.ymlの読み込み
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        stackLimit = config.getInt("stack-limit", 50);

        // イベントリスナーを登録
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // Mobがスポーンしたときのイベント
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity newMob = event.getEntity();
            stackNearbyMobs(newMob);
        }
    }

    // Mobが倒されたときのイベント
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        int currentStack = getStackCount(mob);

        if (currentStack > 1) {
            // スタックを減らして新しいMobをスポーンさせる
            spawnStackedMob(mob, currentStack - 1);
            event.getDrops().clear(); // ドロップアイテムを削除
        }
    }

    // 近くの同じ種類のMobをスタック
    private void stackNearbyMobs(LivingEntity mob) {
        EntityType type = mob.getType();
        int stackCount = 1;

        for (LivingEntity nearbyMob : mob.getLocation().getNearbyLivingEntities(5)) {
            if (nearbyMob.getType() == type && nearbyMob != mob) {
                int nearbyStack = getStackCount(nearbyMob);
                stackCount += nearbyStack;

                if (stackCount > stackLimit) {
                    stackCount = stackLimit;
                    break;
                }

                // アイテムを持っている場合は継承
                if (!nearbyMob.getEquipment().getArmorContents().equals(null)) {
                    mob.getEquipment().setArmorContents(nearbyMob.getEquipment().getArmorContents());
                }
                mob.getEquipment().setItemInMainHand(nearbyMob.getEquipment().getItemInMainHand());
                nearbyMob.remove();
            }
        }

        // 新しい名前を設定
        mob.setCustomName(stackCount + "Stack " + type.name());
        mob.setCustomNameVisible(true);
    }

    // スタック数を減らした新しいMobをスポーン
    private void spawnStackedMob(LivingEntity mob, int newStackCount) {
        LivingEntity newMob = (LivingEntity) mob.getWorld().spawnEntity(mob.getLocation(), mob.getType());

        // アイテムを継承
        newMob.getEquipment().setArmorContents(mob.getEquipment().getArmorContents());
        newMob.getEquipment().setItemInMainHand(mob.getEquipment().getItemInMainHand());

        // 新しい名前を設定
        newMob.setCustomName(newStackCount + "Stack " + mob.getType().name());
        newMob.setCustomNameVisible(true);
    }

    // Mobの名前からスタック数を取得
    private int getStackCount(LivingEntity mob) {
        String name = mob.getCustomName();
        if (name != null && name.contains("Stack")) {
            try {
                String[] parts = name.split("Stack");
                return Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}
