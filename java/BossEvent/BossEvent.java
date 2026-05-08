package BossEvent;

import client.Player;
import core.Manager;
import map.Map;
import map.Mob;
import map.Vgo;
import map.f;
import template.Skill_info;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

public class BossEvent {

    // Constants
    public static final int BOSS_EVENT_MAP_ID = 320;
    private static final int VILLAGE_MAP_ID = 1;
    public static final int BOSS_MAIN_ID = 181; // Kaido
    public static final int[] BOSS_GUARDIAN_IDS = { 172, 163, 162, 161, 160, 159 };

    private static final int GUARDIAN_RESPAWN_MS = 3000;
    private static final long BOSS_COOLDOWN_MS = 30 * 60 * 1000; // 30 minutes

    // Stat debuffs (1000% reduction)
    public static final int MISS_REDUCE = 1000;
    public static final int REACT_REDUCE = 1000;

    private static final BossEvent instance = new BossEvent();

    private final AtomicInteger teleportCountdown = new AtomicInteger(-1);
    private volatile long lastCountdownTime = 0;
    private volatile long lastBossDeathTime = 0;

    private volatile Mob[] guardians;
    private final AtomicInteger aliveGuardiansCount = new AtomicInteger(BOSS_GUARDIAN_IDS.length);
    private final AtomicLongArray guardianDeathTimes = new AtomicLongArray(BOSS_GUARDIAN_IDS.length);

    private BossEvent() {
    }

    public static BossEvent getInstance() {
        return instance;
    }

    public void resetAll() {
        synchronized (this) {
            this.guardians = null;
            this.teleportCountdown.set(-1);
            this.lastCountdownTime = 0;
            this.aliveGuardiansCount.set(BOSS_GUARDIAN_IDS.length);
            for (int i = 0; i < BOSS_GUARDIAN_IDS.length; i++) {
                guardianDeathTimes.set(i, 0L);
            }
        }
    }

    public void update(Map map) {
        // Optimized check: Map.java already filters by ID, but we double check for
        // zone_id
        if (map.template.id != BOSS_EVENT_MAP_ID || map.zone_id != 0)
            return;

        long now = System.currentTimeMillis();
        long monitorStart = now;

        if (lastBossDeathTime != 0 && teleportCountdown.get() <= 0
                && now - lastBossDeathTime < BOSS_COOLDOWN_MS) {
            return;
        }

        // Send world chat when cooldown ends
        if (lastBossDeathTime != 0 && now - lastBossDeathTime >= BOSS_COOLDOWN_MS) {
            lastBossDeathTime = 0;
            try {
                Manager.gI().chatKTG(0, "Kaido Bách Thú đã hồi phục sức mạnh và thách thức mọi đối thủ!", 5);
            } catch (IOException e) {
                core.GameLogger.error("BossEvent.update: Error sending message to player", e);
            }
        }

        // 0. Teleport Countdown Logic
        int currentCountdown = teleportCountdown.get();
        if (currentCountdown > 0) {
            if (now - lastCountdownTime >= 1000) {
                if (map.players != null && !map.players.isEmpty()) {
                    teleportCountdown.decrementAndGet();
                    lastCountdownTime = now;
                    if (teleportCountdown.get() <= 0) {
                        executeTeleport(map);
                    }
                }
            }
            return;
        }

        // 1. Guardian Respawn Logic
        Mob[] localGuardians = getOrInitGuardians(map);
        if (localGuardians != null) {
            for (int i = 0; i < BOSS_GUARDIAN_IDS.length; i++) {
                Mob m = localGuardians[i];
                if (m != null && m.isdie) {
                    long deathTime = guardianDeathTimes.get(i);
                    if (deathTime != 0 && now - deathTime >= GUARDIAN_RESPAWN_MS) {
                        // Use CAS to ensure only one thread (though zone 0 is specific, better safe)
                        if (guardianDeathTimes.compareAndSet(i, deathTime, 0L)) {
                            respawnMob(m, map);
                        }
                    }
                }
            }
        }

        // 2. Boss Skill Logic
        // Logic removed for performance optimization.

        long monitorElapsed = System.currentTimeMillis() - monitorStart;
        if (monitorElapsed > 5) {
            core.GameLogger.info("[BossEvent] update logic took " + monitorElapsed + "ms");
        }
    }

    private Mob[] getOrInitGuardians(Map map) {
        if (this.guardians == null) {
            synchronized (this) {
                if (this.guardians == null) {
                    Mob[] temp = new Mob[BOSS_GUARDIAN_IDS.length];
                    for (int i = 0; i < BOSS_GUARDIAN_IDS.length; i++) {
                        temp[i] = findMob(map, BOSS_GUARDIAN_IDS[i]);
                    }
                    this.guardians = temp;
                }
            }
        }
        return this.guardians;
    }

    private int getGuardianIndex(int mobId) {
        for (int i = 0; i < BOSS_GUARDIAN_IDS.length; i++) {
            if (BOSS_GUARDIAN_IDS[i] == mobId)
                return i;
        }
        return -1;
    }

    public boolean checkFriendlyFire(Player p, Player target) {
        if (p.map.template.id == BOSS_EVENT_MAP_ID) {
            if (p.clan != null && target.clan != null && p.clan.id == target.clan.id) {
                return true;
            }
        }
        return false;
    }

    public long onPlayerAttackBoss(Player p, Mob mob, long damage) {
        int mobId = mob.mob_template.mob_id;

        if (mobId == BOSS_MAIN_ID) {
            if (aliveGuardiansCount.get() > 0) {
                p.hp -= damage;
                if (p.hp < 1)
                    p.hp = 1;
                mob.hp += damage;
                if (mob.hp > mob.hp_max)
                    mob.hp = mob.hp_max;
                return 0;
            }
        } else if (isGuardian(mobId)) {
            // Guardian specific logic remains here (if any)
        }
        return damage;
    }

    public void onGuardianDeath(Mob mob, Map map) {
        int id = mob.mob_template.mob_id;
        int index = getGuardianIndex(id);
        if (index == -1)
            return;

        guardianDeathTimes.set(index, System.currentTimeMillis());
        aliveGuardiansCount.decrementAndGet();
    }

    public void onBossDeath(Player killer, Map map) {
        try {
            String msg = "Chúc mừng Băng " + (killer.clan != null ? killer.clan.name : "Vô bang")
                    + " đã hạ gục Kaido Bách Thú!";
            Manager.gI().chatKTG(0, msg, 5);

            this.teleportCountdown.set(5);
            this.lastCountdownTime = System.currentTimeMillis();
            this.lastBossDeathTime = System.currentTimeMillis();
        } catch (Exception e) {
            core.GameLogger.error("BossEvent.onBossDeath: Error processing boss death", e);
        }
    }

    private Mob findMob(Map map, int templateId) {
        if (map.list_mob == null)
            return null;
        for (int index : map.list_mob) {
            Mob m = Mob.ENTRYS.get(index);
            if (m != null && m.mob_template.mob_id == templateId)
                return m;
        }
        return null;
    }

    private void respawnMob(Mob m, Map map) {
        m.isdie = false;
        m.hp = m.hp_max;
        m.id_target = -1;
        aliveGuardiansCount.incrementAndGet(); // Tăng lại số lượng hộ vệ còn sống
        try {
            io.Message msg = new io.Message(4);
            msg.writer().writeShort(m.index);
            msg.writer().writeShort(m.mob_template.mob_id);
            msg.writer().writeShort(m.x);
            msg.writer().writeShort(m.y);
            msg.writer().writeShort(m.level);
            msg.writer().writeInt(f.setInteger(m.hp));
            msg.writer().writeInt(f.setInteger(m.hp_max));
            msg.writer().writeShort(m.mob_template.skill[0]);
            msg.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME);
            msg.writer().writeByte(m.mob_template.typemonster);
            msg.writer().writeByte(0);
            map.send_msg_all_p(msg, null, true);
            msg.cleanup();
        } catch (IOException e) {
            core.GameLogger.error("BossEvent.respawnMob: Error respawning mob", e);
        }
    }

    private void executeTeleport(Map map) {
        if (map.players == null || map.players.isEmpty()) {
            resetAll();
            return;
        }
        try {
            Vgo vgo = new Vgo();
            vgo.map_go = Map.get_map_by_id(VILLAGE_MAP_ID);
            vgo.xnew = 500;
            vgo.ynew = 500;

            Player[] playersSnapshot;
            synchronized (map.players) {
                playersSnapshot = map.players.toArray(new Player[0]);
            }

            for (Player p : playersSnapshot) {
                if (p != null) {
                    p.goto_map(vgo);
                }
            }
        } catch (IOException e) {
            core.GameLogger.error("BossEvent.executeTeleport: Error teleporting players", e);
        } finally {
            resetAll();
        }
    }

    public static boolean isGuardian(int templateId) {
        for (int id : BOSS_GUARDIAN_IDS) {
            if (id == templateId)
                return true;
        }
        return false;
    }

    public long getRemainingCooldown() {
        if (lastBossDeathTime == 0)
            return 0;
        long elapsed = System.currentTimeMillis() - lastBossDeathTime;
        return elapsed < BOSS_COOLDOWN_MS ? (BOSS_COOLDOWN_MS - elapsed) : 0;
    }
}