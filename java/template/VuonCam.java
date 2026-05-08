package template;

import client.Party;
import client.Player;
import map.Map;
import map.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VuonCam {
  private static List<Map> ENTRY = new ArrayList<>();

  private Set<String> nameP = new HashSet<>();
  public List<Mob> mobs;
  public int level;
  public int hp, hpMax, indexMob;
  public byte dir;
  public int state;
  public long time_state;
  public int mobCanInit;
  public long timeHpDecrease;
  public long timeChat;
  public HashMap<Mob, Long> listRemoveMap;

  public int hpMob;

  public synchronized static void add_map(Map map_boss) {
    ENTRY.add(map_boss);
  }

  public synchronized static Map get_map(Player p) {
    List<Map> listRemove = new ArrayList<>();
    for (int i = 0; i < ENTRY.size(); i++) {
      Map map = ENTRY.get(i);
      if (map.vuonCam != null && map.vuonCam.nameP.contains(p.name)) {
        return map;
      }
      if (!map.running) {
        listRemove.add(map);
      }
    }
    ENTRY.removeAll(listRemove);
    return null;
  }
    public int lvMob;

  public void add_name(Party party) {
    for (int i = 0; i < party.list.size(); i++) {
      nameP.add(party.list.get(i).name);
    }
  }
}
