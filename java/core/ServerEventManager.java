package core;

import activities.*;
import client.Clan;
import client.Player;
import io.Session;
import io.SessionManager;
import java.io.IOException;
import map.Map;
import map.Mob;
import map.Vgo;
import org.joda.time.LocalTime;
import template.Map_Little_Garden;
import template.PvpBang;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTimeConstants;

public class ServerEventManager {

    private Thread thread_cal_time;
    private Thread thread_update_effect;
    private volatile boolean running;

    public ServerEventManager() {
        this.running = false;
        core.GameLogger.info("ServerEventManager instance created.");
    }

    public void close() {
        running = false;
        if (this.thread_cal_time != null)
            this.thread_cal_time.interrupt();
        if (this.thread_update_effect != null)
            this.thread_update_effect.interrupt();
        core.GameLogger.info("ServerEventManager stopped.");
    }

    public void init() {
        core.GameLogger.info("Initializing ServerEventManager...");
        this.running = true;
        this.thread_cal_time = new Thread(this::runCalTime, "Thread-Server-CalTime");
        this.thread_cal_time.start();
        this.thread_update_effect = new Thread(this::runUpdateEffect, "Thread-Update-Effect");
        this.thread_update_effect.start();
    }

    private void runCalTime() {
        core.GameLogger.info("Thread thread_cal_time started.");
        while (this.running) {
            long start = System.currentTimeMillis();
            try {
                LocalTime now = LocalTime.now();
                int hour = now.getHourOfDay();
                int min = now.getMinuteOfHour();
                int sec = now.getSecondOfMinute();

                // Đấu Giá - Optimized to run every 30 seconds
                long startAuction = System.nanoTime();
                if (sec % 30 == 0) {
                    DauGia.update();
                }
                
                // Auto Spawn Boss
                BossManager.update();

                if (hour == 18 && min == 0 && sec == 0) {
                    DauGia.createDaily();
                }
                long elapsedAuction = (System.nanoTime() - startAuction) / 1_000_000L;

                // Reset 0h00
                long startReset = System.nanoTime();
                if (hour == 0 && min == 0 && sec == 0) {
                    doMidnightReset();
                }
                long elapsedReset = (System.nanoTime() - startReset) / 1_000_000L;

                // PBKL ghép clan mỗi 5 giây
                long startLG = System.nanoTime();
                if (sec % 5 == 0 && (hour == 20 || hour == 21)) {
                    doLittleGardenMatch();
                    Map.sendChatNpc((short) -84, "Phó Bản Khổng Lồ đang diễn ra, "
                            + " các băng hãy đến tham gia nào", 33);
                }
                long elapsedLG = (System.nanoTime() - startLG) / 1_000_000L;

                // PVPBANG ghép clan mỗi 10 giây
                long startPvp = System.nanoTime();
                if (sec % 10 == 0 && (hour == 22 || hour == 23)) {
                    PvpBang.update();
                    Map.sendChatNpc((short) -84, "Phó Bản PVP Băng đang diễn ra, "
                            + " các băng hãy đến tham gia nào", 33);
                }
                long elapsedPvp = (System.nanoTime() - startPvp) / 1_000_000L;

                long elapsedTotal = System.currentTimeMillis() - start;
                if (elapsedTotal > 100) {
                    System.err.println(String.format(
                            "[PERF] ServerEventManager tick spike: total=%dms (Auction:%dms, Reset:%dms, LG:%dms, Pvp:%dms)",
                            elapsedTotal, elapsedAuction, elapsedReset, elapsedLG, elapsedPvp));
                }

                long sleep = 1000 - elapsedTotal;
                if (sleep < 0) {
                    sleep = 0;
                }
                Thread.sleep(sleep);

            } catch (InterruptedException e) {
                core.GameLogger.info("Thread thread_cal_time interrupted.");
            } catch (Exception e) {
                GameLogger.error("ServerEventManager: Exception in thread_cal_time", e);
            }
        }
    }

    private void runUpdateEffect() {
        core.GameLogger.info("Thread thread_update_effect started.");
        while (this.running) {
            long start = System.currentTimeMillis();
            try {
                updateAllPlayersEffect();
                long elapsed = System.currentTimeMillis() - start;
                long sleep = 1000 - elapsed;
                if (sleep < 0)
                    sleep = 0;
                Thread.sleep(sleep);

            } catch (InterruptedException e) {
                core.GameLogger.info("Thread thread_update_effect interrupted.");
            } catch (Exception e) {
                GameLogger.error("ServerEventManager: Exception in thread_update_effect", e);
            }
        }
    }

    /** Reset nửa đêm */
    private void doMidnightReset() {
        try {
            core.GameLogger.info("Midnight reset starting...");
            if (Map.ENTRYS != null) {
                for (Map[] map_all : Map.ENTRYS) {
                    if (map_all == null)
                        continue;
                    for (Map map : map_all) {
                        if (map == null || map.players == null || map.players.isEmpty())
                            continue;
                        List<Player> snapshot;
                        synchronized (map.players) {
                            snapshot = new ArrayList<>(map.players);
                        }
                        for (Player p : snapshot) {
                            if (p != null)
                                p.change_new_date();
                        }
                    }
                }
            }
            for (Map map : Map.get_map_plus()) {
                if (map.players == null || map.players.isEmpty())
                    continue;
                List<Player> snapshot;
                synchronized (map.players) {
                    snapshot = new ArrayList<>(map.players);
                }
                for (Player p : snapshot) {
                    if (p != null)
                        p.change_new_date();
                }
            }
            // Reset dữ liệu tuần global (DB)
            LocalTime now = LocalTime.now();
            if (now.getHourOfDay() == 0 && now.getMinuteOfHour() == 0 &&
                    org.joda.time.DateTime.now().getDayOfWeek() == DateTimeConstants.SUNDAY) {
                Player.resetAllPlayersPoints();
            }
            Clan.reset_day();
            LittleGarden.LIST.clear();
            Player.ProcessBossRewardsAndReset();
            core.GameLogger.info("Midnight reset completed.");
        } catch (Exception e) {
            GameLogger.error("ServerEventManager: Midnight reset failed", e);
        }
    }

    /** Ghép clan Little Garden */
    private void doLittleGardenMatch() {
        try {
            if (LittleGarden.LIST.size() > 1) {
                Clan clan1 = LittleGarden.LIST.remove(Util.random(LittleGarden.LIST.size()));
                Clan clan2 = LittleGarden.LIST.remove(Util.random(LittleGarden.LIST.size()));
                if (clan1 == null || clan2 == null)
                    return;
                Player p1 = getLeaderPlayer(clan1);
                Player p2 = getLeaderPlayer(clan2);
                if (p1 == null && p2 == null)
                    return;
                if (p1 == null) {
                    LittleGarden.add_clan_wait(clan2);
                    return;
                }
                if (p2 == null) {
                    LittleGarden.add_clan_wait(clan1);
                    return;
                }
                if (p1.name.equals(p2.name)) {
                    LittleGarden.add_clan_wait(clan1);
                    return;
                }
                Map map_dungeon = createLittleGardenMap(clan1, clan2);
                sendClanToMap(p1, map_dungeon, (byte) 4);
                sendClanToMap(p2, map_dungeon, (byte) 5);
            }
        } catch (Exception e) {
            GameLogger.error("ServerEventManager: doLittleGardenMatch failed", e);
        }
    }

    private Player getLeaderPlayer(Clan clan) {
        if (clan == null || clan.members == null)
            return null;
        for (var member : clan.members) {
            if (member.levelInclan == 0 || member.levelInclan == 1) {
                Player p = Map.get_player_by_name_allmap(member.name);
                if (p != null)
                    return p;
            }
        }
        return null;
    }

    private Map createLittleGardenMap(Clan clan1, Clan clan2) {
        Map mapTemplate = Map.get_map_by_id(81)[0];
        Map map_dungeon = new Map();
        map_dungeon.template = mapTemplate.template;
        map_dungeon.zone_id = (byte) 0;
        map_dungeon.list_mob = new int[0];
        map_dungeon.map_little_garden = new Map_Little_Garden();
        map_dungeon.map_little_garden.mobs = new ArrayList<>();
        map_dungeon.map_little_garden.time = System.currentTimeMillis() + 60_000L * 15;
        map_dungeon.map_little_garden.clan1 = clan1;
        map_dungeon.map_little_garden.clan2 = clan2;
        clan1.map_create = map_dungeon;
        clan2.map_create = map_dungeon;
        int index_mob = -2;
        for (int mobId : mapTemplate.list_mob) {
            Mob temp = Mob.ENTRYS.get(mobId);
            if (temp == null)
                continue;
            Mob mob_add = new Mob();
            mob_add.mob_template = temp.mob_template;
            mob_add.x = temp.x;
            mob_add.y = temp.y;
            mob_add.hp_max = temp.mob_template.hp_max;
            mob_add.hp = mob_add.hp_max;
            mob_add.level = 100;
            mob_add.isdie = false;
            mob_add.id_target = -1;
            mob_add.index = index_mob--;
            mob_add.map = map_dungeon;
            map_dungeon.map_little_garden.mobs.add(mob_add);
        }
        map_dungeon.start_map();
        Map.add_map_plus(map_dungeon);
        return map_dungeon;
    }

    private void sendClanToMap(Player leader, Map map_dungeon, byte pkType) throws IOException {
        if (leader == null || leader.tableTickOption == null || leader.tableTickOption.listP == null)
            return;
        List<Player> toRemove = new ArrayList<>();
        Vgo vgo = new Vgo();
        vgo.map_go = new Map[] { map_dungeon };
        vgo.xnew = 350;
        vgo.ynew = 260;
        for (int i = 0; i < leader.tableTickOption.listP.size(); i++) {
            Player p = leader.tableTickOption.listP.get(i);
            if (p != null && p.conn != null && leader.tableTickOption.list_check[i] == 1) {
                toRemove.add(p);
                p.type_pk = pkType;
                p.goto_map(vgo);
            }
        }
        toRemove.forEach(p -> p.tableTickOption = null);
    }

    private void updateAllPlayersEffect() {
        try {
            Session[] sessions;
            synchronized (SessionManager.CLIENT_ENTRYS) {
                sessions = SessionManager.CLIENT_ENTRYS.toArray(new Session[0]);
            }
            for (Session ss : sessions) {
                if (ss != null && ss.p != null && ss.p.conn != null) {
                    try {
                        ss.p.update_eff();
                    } catch (Exception ex) {
                        GameLogger.warn("ServerEventManager: update_eff failed for player " + ss.p.name, ex);
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("ServerEventManager: updateAllPlayersEffect failed", e);
        }
    }

}
