package core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import activities.Market;
import client.Clan;
import client.Player;
import map.Map;

public class SaveData {

    // Target ~30% of HikariCP (100) to leave room for other processes
    private static final int SAVE_THREAD_COUNT = 30;

    private static final ExecutorService saveExecutor = Executors.newFixedThreadPool(SAVE_THREAD_COUNT, r -> {
        Thread t = new Thread(r, "SaveWorker");
        t.setDaemon(true); // Don't block JVM shutdown
        return t;
    });

    public static void process() {
        if (Manager.gI().server_admin) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            List<Player> playersToSave = new ArrayList<>();

            // 1. Gather snapshots of all players from all maps
            if (Map.ENTRYS != null) {
                for (Map[] mapArr : Map.ENTRYS) {
                    if (mapArr == null)
                        continue;
                    for (Map map : mapArr) {
                        if (map != null) {
                            synchronized (map.players) {
                                playersToSave.addAll(map.players);
                            }
                        }
                    }
                }
            }

            List<Map> mapPlus = Map.get_map_plus();
            if (mapPlus != null) {
                List<Map> snapshotPlus;
                synchronized (mapPlus) {
                    snapshotPlus = new ArrayList<>(mapPlus);
                }
                for (Map map : snapshotPlus) {
                    if (map != null) {
                        synchronized (map.players) {
                            playersToSave.addAll(map.players);
                        }
                    }
                }
            }

            // 2. Filter dirty players and save in parallel or sync if shutting down
            int savedCount = 0;
            if (saveExecutor.isShutdown()) {
                // Fallback to synchronous save if executor is already closed
                for (Player p : playersToSave) {
                    if (p != null && !p.clone && p.isDirty()) {
                        safeFlush(p);
                        savedCount++;
                    }
                }
            } else {
                List<CompletableFuture<Void>> futures = playersToSave.stream()
                        .filter(p -> p != null && !p.clone && p.isDirty())
                        .map(p -> CompletableFuture.runAsync(() -> safeFlush(p), saveExecutor).exceptionally(ex -> {
                            GameLogger.error("[SAVE] Failed to save player " + p.name, ex);
                            return null;
                        })).collect(Collectors.toList());

                savedCount = futures.size();
                // 3. Wait for all saves in this batch to complete
                if (!futures.isEmpty()) {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            }

            updateData();

            long elapsed = System.currentTimeMillis() - start;
            core.GameLogger.info(String.format("[SAVE] Data OK: %d/%d players saved in %d ms",
                    savedCount, playersToSave.size(), elapsed));
        } catch (Exception e) {
            GameLogger.error("[SAVE][FATAL] Scheduler Error", e);
        }
    }

    private static void safeFlush(Player p) {
        // Granular lock at player level to prevent concurrent flush/disconnect save conflicts
        synchronized (p.getSaveLock()) {
            Player.flush(p, false);
        }
    }

    private static void updateData() {
        try {
            Market.update();
        } catch (Exception e) {
            GameLogger.error("[WARN] Market update failed", e);
        }

        try {
            Clan.update();
        } catch (Exception e) {
            GameLogger.error("[WARN] Clan update failed", e);
        }
    }

    public static void shutdown() {
        if (saveExecutor != null) {
            saveExecutor.shutdown();
        }
    }
}
