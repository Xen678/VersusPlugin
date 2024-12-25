package de.nms.versusPluginSandbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathEvent implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player killer = e.getPlayer().getKiller();
        Player killed = e.getPlayer();
        killer.sendMessage("§7[§aVersus§7] §aDu hast §e" + killed.getName() + " §agekillt!");
        killed.sendMessage("§7[§aVersus§7] §cDu wurdest von §e" + killer.getName() + " §cgekillt!");
        World w = Bukkit.getWorld("world");
        if (w != null) {
            killed.teleport(w.getSpawnLocation());
        }
    }
}
