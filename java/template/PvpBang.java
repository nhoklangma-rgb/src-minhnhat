package template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import client.Clan;
import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.util.Iterator;
import java.util.Objects;
import map.Map;
import map.Vgo;

public class PvpBang {
  public static List<Map> ENTRY = new ArrayList<>();
  public static int typeFight = 0;
  public int state;
  public long timeState;
  public Clan clan;
  public Map mapWait;
  public Map mapFight;
  public HashMap<String, String> mapOpponent;
  public byte win, lose;
  public String noticeFinished;
  public boolean isWin;

  public synchronized static void addMap(Map map_dungeon) {
    ENTRY.add(map_dungeon);
  }

  public synchronized static void update() {
    try {
      List<Map> listMapStartFight = new ArrayList<>();
      List<Map> listMapFinished = new ArrayList<>();
      Map map260 = Map.get_map_by_id(260)[0];
      Set<Clan> clansInMap260 = new HashSet<>();
      for (Player p : map260.players) {
        if (p.clan != null &&
            (p.clan.map_create == null ||
                p.clan.map_create.pvpBang == null ||
                p.clan.map_create.pvpBang.state == 0)) {
          clansInMap260.add(p.clan);
        }
      }
      List<Clan> listClanSelect = new ArrayList<>(clansInMap260);
      while (listClanSelect.size() > 1) {
        Clan clan1 = listClanSelect.remove(Util.random(listClanSelect.size()));
        Clan clan2 = listClanSelect.remove(Util.random(listClanSelect.size()));
        if (!clan1.equals(clan2)) {
          PvpBang pvpBang1 = new PvpBang();
          PvpBang pvpBang2 = new PvpBang();
          pvpBang1.clan = clan1;
          pvpBang2.clan = clan2;
          pvpBang1.mapWait = map260;
          pvpBang2.mapWait = map260;
          pvpBang1.state = 1;
          pvpBang2.state = 1;
          pvpBang1.timeState = System.currentTimeMillis() + 14_500;
          pvpBang2.timeState = System.currentTimeMillis() + 14_500;
          pvpBang1.mapOpponent = new HashMap<>();
          pvpBang2.mapOpponent = new HashMap<>();
          pvpBang1.win = 0;
          pvpBang2.win = 0;
          pvpBang1.lose = 0;
          pvpBang2.lose = 0;
          pvpBang1.noticeFinished = "";
          pvpBang2.noticeFinished = "";
          Map mapTemplate = Map.get_map_by_id(121)[0];
          Map map_dungeon = new Map();
          map_dungeon.template = mapTemplate.template;
          map_dungeon.zone_id = (byte) 0;
          map_dungeon.list_mob = new int[0];
          map_dungeon.pvpBangMapFight = new PvpBangMapFight();
          map_dungeon.pvpBangMapFight.clan1 = clan1;
          map_dungeon.pvpBangMapFight.clan2 = clan2;
          Map.add_map_plus(map_dungeon);
          pvpBang1.mapFight = map_dungeon;
          pvpBang2.mapFight = map_dungeon;
          if (clan1.map_create == null) {
            clan1.map_create = new Map();
          }
          if (clan2.map_create == null) {
            clan2.map_create = new Map();
          }
          clan1.map_create.pvpBang = pvpBang1;
          clan2.map_create.pvpBang = pvpBang2;
          notice(map260, "Ghép thành công Băng " + clan1.name + " vs Băng " + clan2.name
              + ".Trận đấu sẽ bắt đầu sau 15s nữa");
          Message m33 = new Message(-73);
          m33.writer().writeByte(4);
          m33.writer().writeShort(15);
          m33.writer().writeByte(pvpBang1.win);
          m33.writer().writeByte(pvpBang1.lose);
          map260.send_msg_all_p(m33, null, true);
          m33.cleanup();
          ENTRY.add(clan1.map_create);
          ENTRY.add(clan2.map_create);
        }
      }
      for (Map map : ENTRY) {
        if (map.pvpBang != null) {
          if (map.pvpBang.state == 2 && map.pvpBang.timeState < System.currentTimeMillis()) {
            map.pvpBang.state = 3;
          } else if (map.pvpBang.state == 3) {
            listMapFinished.add(map);
          } else if (map.pvpBang.state == 1 && map.pvpBang.timeState < System.currentTimeMillis()
              && map.pvpBang.mapFight != null) {
            listMapStartFight.add(map);
          }
        }
      }
      Set<Map> mapToStart = new HashSet<>();
      for (Map map : listMapStartFight) {
        Vgo vgo = new Vgo();
        vgo.map_go = new Map[1];
        vgo.map_go[0] = map.pvpBang.mapFight;
        vgo.ynew = (short) Util.random(240, 280);
        List<Player> listPlayer = new ArrayList<>();
        for (Player p : map260.players) {
          if (p.clan != null && p.clan.equals(map.pvpBang.clan)) {
            listPlayer.add(p);
          }
        }
        if (map.pvpBang.mapFight.pvpBangMapFight.clan1.equals(map.pvpBang.clan)) {
          vgo.xnew = 200;
        } else {
          vgo.xnew = 1600;
        }
        listPlayer.forEach(l -> {
          try {
            l.goto_map(vgo);
          } catch (IOException e) {
            core.GameLogger.error("[Disconnect] PvpBang.update error: " + e.getMessage());
          }
        });
        mapToStart.add(map.pvpBang.mapFight);
        map.pvpBang.mapFight.pvpBangMapFight.time_pvp = 3;
        map.pvpBang.state = 2;
        map.pvpBang.timeState = System.currentTimeMillis() + 60_000 * 3;
      }
      mapToStart.forEach(Map::start_map);
      Iterator<Map> finishedIterator = listMapFinished.iterator();
      while (finishedIterator.hasNext()) {
        Map map = finishedIterator.next();
        if (!map.pvpBang.noticeFinished.isEmpty()
            && map.pvpBang.timeState < System.currentTimeMillis()) {
          List<Player> playerList = map.pvpBang.clan.members.stream()
              .map(member -> Map.get_player_by_name_allmap(member.name))
              .filter(Objects::nonNull)
              .toList();
          int xpAmount = map.pvpBang.isWin ? 2000 : 1000;
          int beriAmount = map.pvpBang.isWin ? 5000 : 2000;
          int rubyAmount = map.pvpBang.isWin ? 500 : 200;
          List<GiftBox> gifts = new ArrayList<>();
          GiftBox.addGiftBoxClan(gifts, -10, xpAmount);
          GiftBox.addGiftBoxClan(gifts, -11, beriAmount);
          GiftBox.addGiftBoxClan(gifts, -12, rubyAmount);
          String winText = map.pvpBang.isWin ? "thắng" : "thua";
          Service.distributeClanGiftOnce(map.pvpBang.clan, playerList, gifts, "Phó Bản PVP Băng",
              "Phần thưởng " + winText + " cuộc");
          map.pvpBang.state = 0;
          map.pvpBang.timeState = System.currentTimeMillis() + 5_000;
          map.pvpBang.noticeFinished = "";
          map.pvpBang.mapFight = null;
          map.pvpBang.isWin = false;
          PvpBang.notice(map260,
              "Băng " + map.pvpBang.clan.name + " đang tìm kiếm đối thủ");
          finishedIterator.remove();
        }
      }

    } catch (Exception ex) {
      core.GameLogger.error("[Disconnect] PvpBang.update error: " + ex.getMessage());
    }
  }

  public static void notice(Map map, String notice) throws IOException {
    Message m = new Message(-31);
    m.writer().writeByte(0);
    m.writer().writeUTF(notice);
    m.writer().writeByte(0); // color
    m.writer().writeShort(-1); // icon clan
    map.send_msg_all_p(m, null, true);
    m.cleanup();
  }
}
