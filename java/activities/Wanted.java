package activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import client.playerClone;
import io.Message;
import map.Map;
import map.Npc;
import map.Vgo;

public class Wanted {
    private static List<Player> LIST = new ArrayList<>();

    public static void show_table(Player p) throws IOException {
        Message m = new Message(-85);
        m.writer().writeByte(0);
        p.addmsg(m);
        m.cleanup();
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        // core.GameLogger.info(act);
        switch (act) {
            case -1: { // exit map
                Vgo vgo = new Vgo();
                vgo.map_go = Map.get_map_by_id(p.id_map_save);
                for (int i = 0; i < vgo.map_go[0].template.npcs.size(); i++) {
                    Npc npc_temp = vgo.map_go[0].template.npcs.get(i);
                    if (npc_temp.namegt.equals("Bản đồ")) {
                        vgo.xnew = npc_temp.x;
                        if (npc_temp.y < 250) {
                            vgo.ynew = (short) (npc_temp.y + 20);
                        } else {
                            vgo.ynew = (short) (npc_temp.y - 40);
                        }
                        break;
                    }
                }
                if (vgo.xnew == 0 || vgo.ynew == 0) {
                    vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                    vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                }
                p.goto_map(vgo);
                break;
            }
            case 1: { // find
                Wanted.add_player_wait(p);
                Message m = new Message(-85);
                m.writer().writeByte(1);
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 3: { // stop find
                Wanted.remove_player_wait(p);
                Message m = new Message(-85);
                m.writer().writeByte(3);
                p.addmsg(m);
                m.cleanup();
                break;
            }
        }
    }

    public synchronized static void add_player_wait(Player p) {
        if (!LIST.contains(p)) {
            LIST.add(p);
        }
    }

    public synchronized static void remove_player_wait(Player p) {
        LIST.remove(p);
    }

    public synchronized static Player[] get_p_random_waiting() throws IOException {
        Player[] result = new Player[] { null, null };
        if (LIST.size() > 0) {
            Player p0 = LIST.get(0);
            if (!p0.clone) {
                Player p1 = playerClone.findPlayerTruyNa(p0);
                if (p1 != null) {

                    p0.pvp_target = p1;
                    p1.pvp_target = p0;
                    //
                    if (!(p0.name + " ").equals(p1.name)) {
                        Wanted.wait_to_enter_round(p0);
                        Wanted.wait_to_enter_round(p1);
                        result[0] = p0;
                        result[1] = p1;
                    } else {
                        if (p1.pcl != null) {
                            p1.pcl.dis();
                        }
                        p0.pvp_target = p0;
                    }

                }
            } else {
                if (p0.pcl != null) {
                    p0.pcl.dis();
                }
            }
        }
        return result;
    }

    private static void wait_to_enter_round(Player p) throws IOException {
        LIST.remove(p);
        Message m = new Message(-85);
        m.writer().writeByte(2);
        p.addmsg(m);
        m.cleanup();
    }
}
