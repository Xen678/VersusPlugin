package de.nms.versusPluginSandbox;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VersusPlugin extends JavaPlugin {

    // Eine einfache Methode zur Verwaltung von Duellanfragen
    private final HashMap<UUID, List<Request>> requestMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Registrierung der Kommandos
        getCommand("vs").setExecutor(new VersusCommand());
        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("decline").setExecutor(new DeclineCommand());
        getLogger().info("VersusPlugin aktiviert!");
        Bukkit.getPluginManager().registerEvents(new DeathEvent(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("VersusPlugin deaktiviert!");
    }

    // --- Request- und RequestManager Logik ---
    private List<Request> getRequests(UUID uuid) {
        return requestMap.getOrDefault(uuid, new ArrayList<>());
    }

    private void addRequest(UUID uuid, Request request) {
        requestMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(request);
    }

    private void removeRequest(UUID uuid, Request request) {
        List<Request> requests = requestMap.get(uuid);
        if (requests != null) {
            requests.remove(request);
            if (requests.isEmpty()) {
                requestMap.remove(uuid);
            }
        }
    }

    // --- VersusCommand Logik ---
    public class VersusCommand implements CommandExecutor {

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cNur Spieler dürfen das!");
                return false;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage("§cBitte benutze §e/vs <player>");
                return false;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cZielspieler nicht gefunden!");
                return false;
            }

            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            // Überprüfen, ob bereits eine Anfrage existiert
            List<Request> playerRequests = getRequests(playerUUID);
            List<Request> targetRequests = getRequests(targetUUID);

            if (playerRequests.stream().anyMatch(request -> request.getReceiverUUID().equals(targetUUID) && request.getStatus() == RequestStatus.SEND)) {
                player.sendMessage("§cDu hast bereits eine Anfrage an diesen Spieler gesendet.");
                return false;
            }

            // Neue Anfrage erstellen und hinzufügen
            Request request = new Request(playerUUID, targetUUID, RequestStatus.SEND);
            addRequest(playerUUID, request);
            addRequest(targetUUID, request);

            // Zielspieler benachrichtigen
            target.sendMessage("§e" + player.getName() + " hat dich zu einem 1vs1-Duell herausgefordert! Benutze §e/accept §7oder §e/decline §7, um zu antworten.");
            player.sendMessage("§aDu hast " + target.getName() + " zu einem 1vs1-Duell herausgefordert!");

            return true;
        }
    }

    // --- AcceptCommand Logik ---
    public class AcceptCommand implements CommandExecutor {

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cNur Spieler dürfen das!");
                return false;
            }

            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();

            // Holen der Anfragen des Spielers
            List<Request> requests = getRequests(playerUUID);
            if (requests.isEmpty()) {
                player.sendMessage("§cDu hast keine offenen Duellanfragen.");
                return false;
            }

            Request request = requests.get(0);
            if (request.getStatus() != RequestStatus.SEND) {
                player.sendMessage("§cKeine gültige Anfrage zum Akzeptieren.");
                return false;
            }

            Player opponent = Bukkit.getPlayer(request.getRequesterUUID());
            if (opponent == null) {
                player.sendMessage("§cDer Anfragende Spieler ist nicht mehr online.");
                return false;
            }

            // Anfrage akzeptieren
            request.setStatus(RequestStatus.ACCEPTED);

            // Beide Spieler benachrichtigen
            opponent.sendMessage("§aDein Duell gegen " + player.getName() + " wurde akzeptiert!");
            player.sendMessage("§aDu hast das Duell gegen " + opponent.getName() + " akzeptiert!");

            // Welt für das Duell erstellen
            World duelWorld = createDuelWorld(player.getName(), opponent.getName());
            List<Player> bothPlayers = List.of(opponent, player);
            // Beide Spieler in die Duellwelt teleportieren
            opponent.teleport(duelWorld.getSpawnLocation());
            player.teleport(duelWorld.getSpawnLocation());
            bothPlayers.forEach(player1 -> {
                Inventory inv = player1.getInventory();
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
                ItemStack echant_gap = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
                ItemStack gap = new ItemStack(Material.GOLDEN_APPLE, 3);
                ItemStack ender_pearls = new ItemStack(Material.ENDER_PEARL, 3);
                ItemStack obsidian = new ItemStack(Material.OBSIDIAN);
                ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
                ItemStack leggins = new ItemStack(Material.DIAMOND_LEGGINGS);
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
                ItemStack crystal = new ItemStack(Material.END_CRYSTAL);

                inv.setItem(36, boots);
                inv.setItem(37, leggins);
                inv.setItem(38, chestplate);
                inv.setItem(39, helmet);

                inv.setItem(0, sword);
                inv.setItem(1, axe);
                inv.setItem(2, echant_gap);
                inv.setItem(3, ender_pearls);
                inv.setItem(4, gap);
                inv.setItem(5, obsidian);
                inv.setItem(40, totem);
                inv.setItem(6, crystal);
            });

            // Anfragen löschen
            removeRequest(playerUUID, request);
            removeRequest(request.getRequesterUUID(), request);

            return true;
        }
    }

    // --- DeclineCommand Logik ---
    public class DeclineCommand implements CommandExecutor {

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cNur Spieler dürfen das!");
                return false;
            }

            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();

            // Holen der Anfragen des Spielers
            List<Request> requests = getRequests(playerUUID);
            if (requests.isEmpty()) {
                player.sendMessage("§cDu hast keine offenen Duellanfragen.");
                return false;
            }

            Request request = requests.get(0);
            if (request.getStatus() != RequestStatus.SEND) {
                player.sendMessage("§cKeine gültige Anfrage zum Ablehnen.");
                return false;
            }

            Player opponent = Bukkit.getPlayer(request.getRequesterUUID());
            if (opponent == null) {
                player.sendMessage("§cDer Anfragende Spieler ist nicht mehr online.");
                return false;
            }

            // Anfrage ablehnen
            request.setStatus(RequestStatus.DECLINED);

            // Beide Spieler benachrichtigen
            opponent.sendMessage("§cDein Duell gegen " + player.getName() + " wurde abgelehnt.");
            player.sendMessage("§cDu hast das Duell gegen " + opponent.getName() + " abgelehnt.");

            // Anfragen löschen
            removeRequest(playerUUID, request);
            removeRequest(request.getRequesterUUID(), request);

            return true;
        }
    }

    // --- Chunk- und Weltlogik für das Duell ---
    public class Chunk extends ChunkGenerator {

        @Override
        public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
            ChunkData chunk = createChunkData(world);

            Material surface = Material.STONE_BRICKS;
            Material fill = Material.STONE;
            Material bottom = Material.BEDROCK;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 128; y++) {
                        if (y == 0) {
                            chunk.setBlock(x, y, z, bottom);
                        } else if (y == 127) {
                            chunk.setBlock(x, y, z, surface);
                        } else {
                            chunk.setBlock(x, y, z, fill);
                        }
                    }
                }
            }

            return chunk;
        }
    }

    private World createDuelWorld(String player1, String player2) {
        String worldName = player1 + "_vs_" + player2 + "_" + System.currentTimeMillis();
        return new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .generator(new Chunk())
                .createWorld();
    }

    // --- Request und RequestStatus Klassen ---
    public static class Request {

        private final UUID requesterUUID;
        private final UUID receiverUUID;
        private RequestStatus status;

        public Request(UUID requesterUUID, UUID receiverUUID, RequestStatus status) {
            this.requesterUUID = requesterUUID;
            this.receiverUUID = receiverUUID;
            this.status = status;
        }

        public UUID getRequesterUUID() {
            return requesterUUID;
        }

        public UUID getReceiverUUID() {
            return receiverUUID;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public void setStatus(RequestStatus status) {
            this.status = status;
        }
    }

    public enum RequestStatus {
        SEND, ACCEPTED, DECLINED, ERROR
    }
}
