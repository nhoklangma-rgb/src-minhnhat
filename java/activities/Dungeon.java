package activities;

import client.Player;
import map.Map;
import map.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Dungeon {
  public List<Map> maps;
  public List<Mob> mobs;
  public HashMap<Mob, Long> hm;
  public long time;
  public byte mode;
  public boolean[] canChangeMap;

  public void create() {
    this.time = System.currentTimeMillis() + 60_000L * 15;
    maps = new ArrayList<>();
    mobs = new ArrayList<>();
    hm = new HashMap<>();
    canChangeMap = new boolean[10];
    int index = -2;
    for (int j = 168; j < 176; j++) {
      // create map
      Map mapTemplate = Map.get_map_by_id(j)[0];
      Map map_dungeon = new Map();
      map_dungeon.template = mapTemplate.template;
      map_dungeon.zone_id = (byte) 0;
      map_dungeon.list_mob = new int[0];
      for (int i = 0; i < mapTemplate.list_mob.length; i++) {
        Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
        Mob mob_add = new Mob();
        mob_add.mob_template = temp.mob_template;
        mob_add.x = temp.x;
        mob_add.y = temp.y;
        mob_add.hp = 10000;
        mob_add.hp = (mob_add.hp * (100 + this.mode * 5000)) / 100;
        mob_add.hp_max = mob_add.hp;
        mob_add.level = 100;
        //
        mob_add.isdie = false;
        mob_add.id_target = -1;
        mob_add.index = index--;
        mob_add.map = map_dungeon;
        mobs.add(mob_add);
      }
      map_dungeon.start_map();
      map_dungeon.map_dungeon = this;
      Map.add_map_plus(map_dungeon);
      maps.add(map_dungeon);
    }
  }

  public Mob get_mob(Player p, int id) {
    for (int i = 0; i < mobs.size(); i++) {
      Mob mob = mobs.get(i);
      if (p.map.equals(mob.map) && mob.index == id) {
        return mob;
      }
    }
    return null;
  }
}
