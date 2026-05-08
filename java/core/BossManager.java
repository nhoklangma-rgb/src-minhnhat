package core;

import java.io.IOException;
import client.Player;
import map.Map;
import map.Mob;
import map.f;
import template.MobTemplate;
import template.Skill_info;

public class BossManager {
    private static int current_index = 20000;

    public static synchronized void callBoss(int mapId, int templateId, long hp, short x, short y) {
        Map[] maps = Map.get_map_by_id(mapId);
        if (maps == null || maps.length == 0) {
            core.GameLogger.error("BossManager.callBoss: Map not found ID=" + mapId);
            return;
        }

        MobTemplate temp = MobTemplate.get_it_by_id(templateId);
        if (temp == null)
            return;

        // Tự động lấy trung tâm nếu truyền -1
        short xSummon = (x == -1) ? (short) (maps[0].template.maxW / 2) : x;
        short ySummon = (y == -1) ? (short) (maps[0].template.maxH / 2) : y;

        // Chỉ spawn tại 1 khu ngẫu nhiên thay vì tất cả các khu
        callBossInternal(maps[Util.random(maps.length)], temp, hp, xSummon, ySummon);

        announceBoss(temp, maps[0].template.name);
    }

    // --- AUTOMATIC SPAWN LOGIC ---
    private static final int[] AUTO_BOSS_TEMPLATES = { 153 };
    // Maps từ 0-34, bỏ làng (1, 9, 17, 25, 33)
    private static final int[] AUTO_BOSS_MAPS = {
            0, 2, 3, 4, 5, 6, 7, 8,
            10, 11, 12, 13, 14, 15, 16,
            18, 19, 20, 21, 22, 23, 24,
            26, 27, 28, 29, 30, 31, 32, 34
    };
    private static long lastAutoSpawn = 0;

    public static void update() {
        if (!Manager.gI().isAutoSpawn)
            return;

        long now = System.currentTimeMillis();
        // Mỗi 5 phút tự động spawn 1 Boss ngẫu nhiên
        if (lastAutoSpawn == 0 || now - lastAutoSpawn >= 30 * 60_000L) {
            lastAutoSpawn = now;
            spawnRandomBoss();
        }
    }

    private static void spawnRandomBoss() {
        try {
            int templateId = AUTO_BOSS_TEMPLATES[Util.random(AUTO_BOSS_TEMPLATES.length)];
            int mapId = AUTO_BOSS_MAPS[Util.random(AUTO_BOSS_MAPS.length)];
            long hp = 100000000L; // HP mặc định cho Boss tự động

            callBoss(mapId, templateId, hp, (short) -1, (short) -1);
            core.GameLogger.info("[AutoSpawn] Boss " + templateId + " spawned at Map " + mapId);
        } catch (Exception e) {
            core.GameLogger.error("BossManager.spawnRandomBoss error", e);
        }
    }

    public static synchronized void callBoss(Map map, int templateId, long hp, short x, short y) {
        if (map == null)
            return;

        MobTemplate temp = MobTemplate.get_it_by_id(templateId);
        if (temp == null)
            return;

        callBossInternal(map, temp, hp, x, y);
        announceBoss(temp, map.template.name);
    }

    private static void callBossInternal(Map map, MobTemplate temp, long hp, short x, short y) {
        Mob boss = new Mob();
        boss.index = current_index++;
        if (current_index > 32000) {
            current_index = 20000;
        }

        boss.mob_template = temp;
        boss.hp_max = hp;
        boss.hp = hp;
        boss.x = x;
        boss.y = y;
        boss.level = temp.level;
        boss.isdie = false;
        boss.id_target = -1;
        boss.map = map;
        boss.isDynamic = true;

        // Thêm vào Map (Xử lý mảng int[])
        if (map.list_mob == null) {
            map.list_mob = new int[] { boss.index };
        } else {
            int[] new_list = new int[map.list_mob.length + 1];
            System.arraycopy(map.list_mob, 0, new_list, 0, map.list_mob.length);
            new_list[map.list_mob.length] = boss.index;
            map.list_mob = new_list;
        }
        map.mobs_cache.add(boss);
        Mob.ENTRYS.put(boss.index, boss);

        sendMobSpawn(boss, map);
    }

    private static void announceBoss(MobTemplate temp, String mapName) {
        try {
            String msg = "CẢNH BÁO: Boss " + temp.name + " đã xuất hiện tại " + mapName + "!";
            Manager.gI().chatKTG(0, msg, 5);
            // core.GameLogger.info("World Chat: " + msg);
        } catch (Exception e) {
            core.GameLogger.error("Failed to send World Chat for Boss: " + temp.name, e);
        }
    }

    private static void sendMobSpawn(Mob m, Map map) {
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
            core.GameLogger.error("BossManager.sendMobSpawn error", e);
        }
    }
}
