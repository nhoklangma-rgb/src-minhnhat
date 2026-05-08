package io;

import activities.DauGia;
import activities.Trade;
import activities.Wanted;
import client.Player;
import core.Manager;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import map.Map;

public class SessionManager {

    public static final List<Session> CLIENT_ENTRYS = new LinkedList<>();
    public static final ConcurrentHashMap<String, Long> CLIENT_LOGIN_TIME = new ConcurrentHashMap<>();
    private static final int MAX_TIME_LOGIN_CACHE = 500;
    public static final java.util.Map<String, Long> time_login = java.util.Collections
            .synchronizedMap(new java.util.LinkedHashMap<String, Long>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
                    return size() > MAX_TIME_LOGIN_CACHE;
                }
            });
    public static final ConcurrentHashMap<Integer, Player> PLAYER_BY_ID = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Player> PLAYER_BY_NAME = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> IP_COUNT = new ConcurrentHashMap<>();
    public final static long TIME_LOGIN_AGAIN = Manager.gI().server_admin ? 0 : 5_000L;
    private static final Object[] USER_LOCKS = new Object[1024];
    private static final AtomicInteger SESSION_ID_COUNTER = new AtomicInteger(1000);

    static {
        for (int i = 0; i < USER_LOCKS.length; i++) {
            USER_LOCKS[i] = new Object();
        }
    }

    public static Object getUserLock(String user) {
        if (user == null) {
            return new Object();
        }
        return USER_LOCKS[user.toLowerCase().hashCode() & (USER_LOCKS.length - 1)];
    }

    public static void registerPlayer(Player p) {
        if (p == null || p.name == null) {
            return;
        }
        PLAYER_BY_ID.put(p.id, p);
        PLAYER_BY_NAME.put(p.name.toLowerCase(), p);
    }

    public static void refreshAllPlayers() {
        for (client.Player p : PLAYER_BY_ID.values()) {
            if (p != null && p.conn != null && p.body != null) {
                try {
                    p.invalidateStatCache();
                    p.body.refreshStats();
                    p.update_info_to_all();
                } catch (Exception e) {
                }
            }
        }
    }

    public static void unregisterPlayer(Player p) {
        if (p == null || p.name == null) {
            return;
        }
        PLAYER_BY_ID.remove(p.id, p); // Safe removal (only if it matches the object)
        PLAYER_BY_NAME.remove(p.name.toLowerCase(), p);
    }

    public static boolean client_connect(Session ss) {
        if (ss == null) {
            return false;
        }
        boolean added = false;
        synchronized (CLIENT_ENTRYS) {
            if (!CLIENT_ENTRYS.contains(ss)) {
                if (ss.ipAddress != null) {
                    int count = IP_COUNT.getOrDefault(ss.ipAddress, 0);
                    if (count >= 17 && !Manager.gI().server_admin) {
                        return false;
                    }
                    IP_COUNT.merge(ss.ipAddress, 1, Integer::sum);
                }
                CLIENT_ENTRYS.add(ss);
                added = true;
            }
        }
        if (added) {
            // [TỐI ƯU] Khởi tạo mạng ngoài Global Lock
            ss.id = SESSION_ID_COUNTER.getAndIncrement();
            ss.init();
            SessionTracker.logConnect(ss);
            return true;
        }
        return false;
    }

    public static void client_disconnect(Session ss) {
        if (ss == null) {
            return;
        }

        // ── OFFLINE TRAINING GUARD ──
        if (ss.p != null) {
            synchronized (ss.p) {
                // Nếu vẫn còn thời gian treo máy, tự động chuyển sang chế độ AI khi logout
                if (ss.p.offlineTrainingEndTime > System.currentTimeMillis()
                        && ss.p.conn == ss) {

                    final client.Player p_off = ss.p;
                    p_off.isOfflineTraining = true; // [QUAN TRỌNG] Bật lại AI khi logout
                    p_off.type_pk = -1;
                    p_off.typePirate = -1;

                    Session dummy = new Session(true);
                    dummy.p = p_off;
                    p_off.conn = dummy; // an toàn, đang trong synchronized(p_off)

                    ss.connected = false;
                    synchronized (CLIENT_ENTRYS) {
                        CLIENT_ENTRYS.remove(ss);
                    }
                    if (ss.ipAddress != null) {
                        IP_COUNT.computeIfPresent(ss.ipAddress, (k, v) -> v > 1 ? v - 1 : null);
                    }

                    new Thread(() -> {
                        try {
                            client.Player.flush(p_off, true);
                        } catch (Exception e) {
                            core.GameLogger.error("[OfflineTraining] Lỗi lưu DB khi disconnect", e);
                        }
                    }).start();

                    SessionTracker.logDisconnect(ss, "offline_transition");
                    return;
                }
            }
        }
        // ────────────────────────────────────────────────────────────────
        try {
            ss.connected = false;

            // [PHASE 1] Quick remove from Global Session List
            synchronized (CLIENT_ENTRYS) {
                if (!CLIENT_ENTRYS.remove(ss)) {
                    // Tránh xử lý thừa nếu đã bị xóa nơi khác
                }
            }
            if (ss.ipAddress != null) {
                IP_COUNT.computeIfPresent(ss.ipAddress, (k, v) -> v > 1 ? v - 1 : null);
            }

            // [PHASE 2] Account-specific cleanup (Thread-Safe per User)
            String lockUser = (ss.user != null) ? ss.user : "";
            synchronized (getUserLock(lockUser)) {
                if (ss.user != null && !ss.user.isEmpty()) {
                    time_login.put(ss.user, System.currentTimeMillis() + TIME_LOGIN_AGAIN);
                    CLIENT_LOGIN_TIME.remove(ss.user);
                }

                if (ss.p != null) {
                    Player p = ss.p;
                    try {
                        // [Tác vụ dọn dẹp có rủi ro cao]
                        try {
                            if (p.map != null) {
                                p.map.leave_map(p, 0);
                                handleMapSpecialCases(p);
                            }
                        } catch (Exception e) {
                            core.GameLogger.error(
                                    "[Ghost Fix] Emergency Map.leave_map failed for " + p.name + ": " + e.getMessage());
                        }

                        // High-priority cleanup (WishManager/Trade/TaiXiu can be noisy, so we do them
                        // after map removal)
                        try {
                            activities.WishManager.gI().onPlayerLogout(p);
                        } catch (Exception e) {
                            core.GameLogger.warn("[Disconnect] WishManager logout error: " + e.getMessage());
                        }
                        try {
                            taixiu.SlotMachineManager.clearPlayerSession(p.id);
                            taixiu.BlackjackManager.clearPlayerSession(p.id);
                        } catch (Exception e) {
                            core.GameLogger.warn("[Disconnect] TaiXiu/SlotMachine cleanup error: " + e.getMessage());
                        }
                        try {
                            if (!p.clone) {
                                Player.flush(p, true);
                            }
                        } catch (Exception e) {
                            core.GameLogger
                                    .error("[Disconnect] Player.flush error for " + ss.user + ": " + e.getMessage());
                        }
                        try {
                            if (p.ship_pet != null && p.ship_pet.map == null && p.map != null) {
                                p.ship_pet.map = p.map;
                            }
                            if (p.dungeon != null) {
                                for (int i = 0; i < p.dungeon.maps.size(); i++) {
                                    p.dungeon.maps.get(i).map_dungeon.time = 0;
                                }
                            }
                        } catch (Exception e) {
                            core.GameLogger
                                    .warn("[Disconnect] ShipPet/Dungeon specific cleanup error: " + e.getMessage());
                        }
                        try {
                            if (p.trade_target != null) {
                                Trade.end_trade_by_disconnect(p.trade_target, p, 0, "");
                                Trade.end_trade_by_disconnect(p, p.trade_target, 0, "");
                            }
                        } catch (Exception e) {
                            core.GameLogger.warn("[Disconnect] Trade cleanup error: " + e.getMessage());
                        }
                        try {
                            if (p.party != null) {
                                p.party.remove_mem(p);
                            }
                            DauGia.player_leave(p);
                            handleTableTickOption(p);
                            Wanted.remove_player_wait(p);
                            p.check_and_remove_expired_fashion();
                        } catch (Exception e) {
                            core.GameLogger.warn("[Disconnect] Party/DauGia/Wanted cleanup error: " + e.getMessage());
                        }
                    } finally {
                        // [CHỐT CHẶN CUỐI CÙNG] Đảm bảo giải phóng Session để có thể vào lại được
                        unregisterPlayer(p);
                        p.conn = null;
                        try {
                            ss.update_onl(0);
                        } catch (Exception e) {
                            core.GameLogger.warn("[Disconnect] SQL update_onl(0) error: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] Global error for " + ss.user + ": " + e.getMessage());
        } finally {
            // [PHASE 3] Final Network cleanup
            SessionTracker.logDisconnect(ss, "client_closed");
            try {
                ss.clear_network(ss);
            } catch (Exception e) {
                core.GameLogger.warn("[Disconnect] Network cleanup error: " + e.getMessage());
            }
        }
    }

    private static void handleMapSpecialCases(Player p) {
        if (p == null || p.map == null) {
            return;
        }
        Player p_select = null;
        boolean hasPvp;
        synchronized (p.map.players) {
            hasPvp = p.map.map_pvp != null && p.map.players.size() > 0;
            if (hasPvp && p.map.map_pvp.type_map == 2 && !p.map.players.isEmpty()) {
                p_select = p.map.players.get(0);
            }
        }
        if (hasPvp) {
            if (p.map.map_pvp.type_map == 0) {
                p.pvp_lose++;
                p.update_pvpPoint(-45);
            } else if (p.map.map_pvp.type_map == 2) {
                try {
                    if (p_select != null) {
                        long beri_win = (10_000L + (long) p.get_wanted_point()) / 100L;
                        long beri_lose = (5_000L + (long) p.get_wanted_point()) / 100L;
                        p_select.update_wanted_point((int) beri_win);
                        p.update_wanted_point((int) -beri_lose);
                    }
                } catch (Exception e) {
                    core.GameLogger.error("[Disconnect] HandleMapSpecialCases error: " + e.getMessage());
                }
            }
        }
    }

    private static void handleTableTickOption(Player p) throws IOException {
        if (p == null) {
            return;
        }
        if (p.tableTickOption != null) {
            Message m = new Message(-74);
            m.writer().writeByte(3);
            m.writer().writeShort(p.tableTickOption.idDialog); // id dialog
            m.writer().writeShort(p.index_map);

            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                Player p0 = Map.get_player_by_name_allmap(p.tableTickOption.listP.get(i).name);
                if (p0 != null && p0.conn != null) {
                    p0.addmsg(m);
                }
                if (p.name.equals(p.tableTickOption.listP.get(i).name)) {
                    p.tableTickOption.list_check[i] = -1;
                }
            }
            m.cleanup();
        }
    }
}