package template;

import client.Player;
import map.Map;
import java.util.ArrayList;
import java.util.List;

public class Ship_pet {
    private static List<Ship_pet> LIST = new ArrayList<>();
    public short index_map, x, y;
    public String name;
    public int hp_max, hp;
    public long time;
    public Player main_ship;
    public Map map;
    public long time_buff_hp;
    public String mainBaoVe = "";
    public int id_map_save;
    private static short current_index_map = 10000;

    public static synchronized short getNewIndexMap() {
        return current_index_map++;
    }

    public synchronized static void add(Ship_pet t) {
        LIST.add(t);
    }

    public synchronized static void remv(Ship_pet t) {
        LIST.remove(t);
    }

    public synchronized static Ship_pet get_pet(int id) {
        for (int i = 0; i < LIST.size(); i++) {
            if (LIST.get(i).index_map == id) {
                return LIST.get(i);
            }
        }
        return null;
    }

    public synchronized static Ship_pet get_pet(Player p) {
        Ship_pet result = null;
        // List<Ship_pet> list_remove = new ArrayList<>();
        for (int i = 0; i < LIST.size(); i++) {
            if (LIST.get(i).main_ship != null) {
                if (LIST.get(i).main_ship.name.equals(p.name)) {
                    result = LIST.get(i);
                    p.typePirate = result.main_ship.typePirate;
                    p.id_ship_packet = result.main_ship.id_ship_packet;
                    result.main_ship = p;
                    break;
                }
            } else {
                // list_remove.add(LIST.get(i));
            }
        }
        // LIST.removeAll(list_remove);
        return result;
    }
}
