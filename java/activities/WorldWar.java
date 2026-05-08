package activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.joda.time.LocalTime;
import client.Clan;
import client.Player;
import core.Manager;
import core.Util;
import io.Message;
import map.Map;
import map.Mob;
import template.WorldWarInfo;

public class WorldWar {
  public static final String[] CLAN_FLAG_NAME = new String[] { "Phe Hải tặc", "Phe Hải quân", "Phe Cách mạng" };
  public static final int[] CLAN_FLAG_TYPE = new int[] { 13, 11, 12 };
  public static List<WorldWarInfo> listPlayer = new ArrayList<>();
  public static HashMap<Clan, Integer> mapClan = new HashMap<>();
  private static Map[] listMap;
  static {
    listMap = new Map[4];
    for (int i = 250; i <= 253; i++) {
      // create map
      Map mapTemplate = Map.get_map_by_id(i)[0];
      Map map_dungeon = new Map();
      map_dungeon.template = mapTemplate.template;
      map_dungeon.zone_id = (byte) 0;
      map_dungeon.list_mob = new int[0];
      map_dungeon.start_map();
      Map.add_map_plus(map_dungeon);
      listMap[i - 250] = map_dungeon;
    }
  }

  public synchronized static boolean isRegisted(Clan clan) {
    return mapClan.containsKey(clan);
  }

  public synchronized static void register(Clan clan, int type) {
    mapClan.put(clan, type);
  }

  public synchronized static int getClanFlag(Clan clan) {
    return mapClan.get(clan);
  }

  public synchronized static String getFlagName(Clan clan) {
    int type = WorldWar.getClanFlag(clan);
    for (int i = 0; i < CLAN_FLAG_NAME.length; i++) {
      if (CLAN_FLAG_TYPE[i] == type) {
        return CLAN_FLAG_NAME[i];
      }
    }
    return CLAN_FLAG_NAME[0];
  }

  public static void updateWorldWarPoint(Player p, int skill, int kill, int dead)
      throws IOException {
    Message m = new Message(53);
    m.writer().writeShort(p.index_map);
    m.writer().writeByte(skill);
    m.writer().writeShort(kill);
    m.writer().writeShort(dead);
    p.map.send_msg_all_p(m, null, true);
  }

  public synchronized static void registerP(String name, int flagType) {
    WorldWarInfo t = new WorldWarInfo();
    t.name = name;
    t.skPoint = 5;
    t.kill = 0;
    t.dead = 0;
    t.flagType = flagType;
    t.timeRefreshSkill = System.currentTimeMillis();
    listPlayer.add(t);
  }

  public synchronized static WorldWarInfo getInfoP(String name) {
    int time_h = LocalTime.now().getHourOfDay();
    if (time_h == 21) {
      for (int i = 0; i < listPlayer.size(); i++) {
        if (listPlayer.get(i).name.equals(name)) {
          return listPlayer.get(i);
        }
      }
    }
    return null;
  }

  public synchronized static int getFlagRd() {
    byte[] flag = new byte[3];
    for (Entry<Clan, Integer> en : mapClan.entrySet()) {
      switch (en.getValue()) {
        case 11:
          flag[1]++;
          break;
        case 12:
          flag[2]++;
          break;
        default: // 13
          flag[0]++;
          break;
      }
    }
    int max = -1;
    for (int i = 0; i < flag.length; i++) {
      max = Math.max(flag[i], max);
    }
    int rd = Util.random(CLAN_FLAG_TYPE.length);
    int time = 0;
    while (time < 100 && flag[rd] == max) {
      rd = Util.random(CLAN_FLAG_TYPE.length);
      time++;
    }
    return CLAN_FLAG_TYPE[rd];
  }

  public synchronized static void start() throws IOException {
    for (Map[] map_all : Map.ENTRYS) {
      for (Map map : map_all) {
        for (int i = 0; i < map.players.size(); i++) {
          map.players.get(i).worldWarInfo = WorldWar.getInfoP(map.players.get(i).name);
          map.players.get(i).map.change_flag(map.players.get(i), -1);
        }
      }
    }
    List<Map> mapplus = Map.get_map_plus();
    for (int i = 0; i < mapplus.size(); i++) {
      for (int i12 = 0; i12 < mapplus.get(i).players.size(); i12++) {
        Player p0 = mapplus.get(i).players.get(i12);
        p0.worldWarInfo = WorldWar.getInfoP(p0.name);
        p0.map.change_flag(p0, -1);
      }
    }
  }

  public synchronized static void stop() throws IOException {
    for (Map[] map_all : Map.ENTRYS) {
      for (Map map : map_all) {
        for (int i = 0; i < map.players.size(); i++) {
          Player p0 = map.players.get(i);
          if (p0.worldWarInfo != null) {
            long xp = p0.worldWarInfo.kill * 100 * (p0.level / 10);
            p0.update_exp(xp, false);
            p0.worldWarInfo = null;
            p0.map.change_flag(p0, -1);
          }
        }
      }
    }
    List<Map> mapplus = Map.get_map_plus();
    for (int i = 0; i < mapplus.size(); i++) {
      for (int i12 = 0; i12 < mapplus.get(i).players.size(); i12++) {
        Player p0 = mapplus.get(i).players.get(i12);
        if (p0.worldWarInfo != null) {
          long xp = p0.worldWarInfo.kill * 100 * (p0.level / 10);
          p0.update_exp(xp, false);
          if (xp > 10_000) {
            p0.clan.xp += xp / 500;
          } else {
            p0.clan.xp += 20;
          }
          // p0.worldWarInfo = null;
          p0.map.change_flag(p0, -1);
        }
      }
    }
    try {
      for (Entry<Clan, Integer> en : mapClan.entrySet()) {
        Clan clan = en.getKey();
        int xp = 0;
        for (int i = 0; i < clan.members.size(); i++) {
          Player p0 = Map.get_player_by_name_allmap(clan.members.get(i).name);
          if (p0 != null) {
            Clan.set_data(p0, false);
            if (p0.worldWarInfo != null) {
              long xp2 = p0.worldWarInfo.kill * 100 * (p0.level / 10);
              if (xp2 > 10_000) {
                xp += xp2 / 500;
              } else {
                xp += 20;
              }
              p0.worldWarInfo = null;
            }
          }
        }
        //
        clan.chat_on_board(clan.members.get(0).id, clan.members.get(0).name,
            ("Đại chiến thế giới nhận " + xp + " xp băng"), -3);
      }
    } catch (Exception e) {
      core.GameLogger.error("WorldWar.stop: Error processing clan rewards", e);
    }
    int[] kills = new int[] { 0, 0, 0 };
    int[] deads = new int[] { 0, 0, 0 };
    for (int i = 0; i < WorldWar.listPlayer.size(); i++) {
      WorldWarInfo wInfo = WorldWar.listPlayer.get(i);
      switch (wInfo.flagType) {
        case 11:
          kills[1] += wInfo.kill;
          deads[1] += wInfo.dead;
          break;
        case 12:
          kills[2] += wInfo.kill;
          deads[2] += wInfo.dead;
          break;
        default: // 13
          kills[0] += wInfo.kill;
          deads[0] += wInfo.dead;
          break;
      }
    }
    String notice = "Sự kiện Đại chiến thế giới kết thúc. Kết quả: ";
    for (int i = 0; i < WorldWar.CLAN_FLAG_NAME.length; i++) {
      notice += (WorldWar.CLAN_FLAG_NAME[i] + ": (" + kills[i] + " giết - " + deads[i] + " chết), ");
    }
    // notice += ". Phe chiến thắng là ";
    Manager.gI().chatKTG(0, notice, 5);
    mapClan.clear();
    listPlayer.clear();
  }

  public static void notice() throws IOException {
    WorldWarInfo tempInfo = null;
    for (int i = 0; i < listPlayer.size(); i++) {
      if (tempInfo == null || ((tempInfo.kill - tempInfo.dead) < (listPlayer.get(i).kill
          - listPlayer.get(i).dead))) {
        tempInfo = listPlayer.get(i);
      }
    }
    if (tempInfo != null && tempInfo.kill > 0) {
      String notice = " Top 1 Đại chiến thế giới hiện tại: " + tempInfo.name + ": (" + tempInfo.kill
          + " giết - " + tempInfo.dead + " chết)";
      Manager.gI().chatKTG(0, notice, 5);
    }
  }

  public static Map getMap(int level) {
    if (level < 4) {
      return listMap[0];
    } else if (level < 6) {
      return listMap[1];
    } else if (level < 8) {
      return listMap[2];
    } else {
      return listMap[3];
    }
  }
}
