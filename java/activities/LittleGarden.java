package activities;

import client.Clan;
import client.Player;
import io.Message;
import map.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LittleGarden {
    public static List<Clan> LIST = new ArrayList<>();

    public static void send_info(Player p) throws IOException {
        if (p.map.map_little_garden != null) {
            Message m = new Message(-79);
            m.writer().writeByte(0);
            m.writer().writeByte(0); // Dorry
            m.writer().writeInt(10); // hp max
            m.writer().writeInt(10); // mp max
            p.addmsg(m);
            m.cleanup();
            //
            m = new Message(-79);
            m.writer().writeByte(0);
            m.writer().writeByte(1); // Brogy
            m.writer().writeInt(10); // hp max
            m.writer().writeInt(10); // mp max
            p.addmsg(m);
            m.cleanup();
            //
            update_info(p);
        }
    }

    private static void update_info(Player p) throws IOException {
        if (p.map.map_little_garden != null) {
            Message m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(0); // Dorry
            m.writer().writeInt(p.map.map_little_garden.hp_1); // hp max
            m.writer().writeInt(p.map.map_little_garden.mp_1); // mp max
            p.addmsg(m);
            m.cleanup();
            //
            m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(1); // Brogy
            m.writer().writeInt(p.map.map_little_garden.hp_2); // hp max
            m.writer().writeInt(p.map.map_little_garden.mp_2); // mp max
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void update_mp(Map map, int type, int quant) throws IOException {
        if (type == 4) {
            Message m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(0); // Dorry
            m.writer().writeInt(map.map_little_garden.hp_1); // hp
            map.map_little_garden.mp_1 += quant;
            if (quant == 0) {
                map.map_little_garden.mp_2 = 0;
            }
            m.writer().writeInt(map.map_little_garden.mp_1); // mp
            map.send_msg_all_p(m, null, true);
            m.cleanup();
            if (map.map_little_garden.mp_1 >= 10) {
                map.map_little_garden.mp_1 = 0;
                update_dame(map, type);
            }
        } else if (type == 5) {
            Message m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(1); // brogy
            m.writer().writeInt(map.map_little_garden.hp_2); // hp
            map.map_little_garden.mp_2 += quant;
            if (quant == 0) {
                map.map_little_garden.mp_1 = 0;
            }
            m.writer().writeInt(map.map_little_garden.mp_2); // mp
            map.send_msg_all_p(m, null, true);
            m.cleanup();
            if (map.map_little_garden.mp_2 >= 10) {
                update_dame(map, type);
                map.map_little_garden.mp_2 = 0;
            }
        }
        //
        if (quant == 0) {
            if (type == 5) {
                Message m = new Message(-79);
                m.writer().writeByte(1);
                m.writer().writeByte(0); // Dorry
                m.writer().writeInt(map.map_little_garden.hp_1); // hp
                m.writer().writeInt(map.map_little_garden.mp_1); // mp
                map.send_msg_all_p(m, null, true);
                m.cleanup();
            } else if (type == 4) {
                Message m = new Message(-79);
                m.writer().writeByte(1);
                m.writer().writeByte(1); // brogy
                m.writer().writeInt(map.map_little_garden.hp_2); // hp
                m.writer().writeInt(map.map_little_garden.mp_2); // mp
                map.send_msg_all_p(m, null, true);
                m.cleanup();
            }
        }
    }

    public static void update_hp(Map map, int type, int quant) throws IOException {
        if (type == 4) {
            Message m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(0); // Dorry
            map.map_little_garden.hp_1 += quant;
            if (map.map_little_garden.hp_1 > 10) {
                map.map_little_garden.hp_1 = 10;
            }
            m.writer().writeInt(map.map_little_garden.hp_1); // hp
            m.writer().writeInt(map.map_little_garden.mp_1); // mp
            map.send_msg_all_p(m, null, true);
            m.cleanup();
        } else if (type == 5) {
            Message m = new Message(-79);
            m.writer().writeByte(1);
            m.writer().writeByte(1); // brogy
            map.map_little_garden.hp_2 += quant;
            if (map.map_little_garden.hp_2 > 10) {
                map.map_little_garden.hp_2 = 10;
            }
            m.writer().writeInt(map.map_little_garden.hp_2); // hp
            m.writer().writeInt(map.map_little_garden.mp_2); // mp
            map.send_msg_all_p(m, null, true);
            m.cleanup();
        }
    }

    private static void update_dame(Map map, int type) throws IOException {
        Message m = null;
        if (type == 4) {
            m = new Message(-79);
            m.writer().writeByte(2);
            m.writer().writeByte(0); // Dorry
            m.writer().writeInt(1);
            map.map_little_garden.hp_2 -= 1;
            map.send_msg_all_p(m, null, true);
            m.cleanup();
        } else if (type == 5) {
            m = new Message(-79);
            m.writer().writeByte(2);
            m.writer().writeByte(1); // brogy
            m.writer().writeInt(1);
            map.map_little_garden.hp_1 -= 1;
            map.send_msg_all_p(m, null, true);
            m.cleanup();
        }
        //
        m = new Message(-79);
        m.writer().writeByte(1);
        m.writer().writeByte(0); // Dorry
        m.writer().writeInt(map.map_little_garden.hp_1); // hp max
        m.writer().writeInt(map.map_little_garden.mp_1); // mp max
        map.send_msg_all_p(m, null, true);
        m.cleanup();
        //
        m = new Message(-79);
        m.writer().writeByte(1);
        m.writer().writeByte(1); // Brogy
        m.writer().writeInt(map.map_little_garden.hp_2); // hp max
        m.writer().writeInt(map.map_little_garden.mp_2); // mp max
        map.send_msg_all_p(m, null, true);
        m.cleanup();
        if (map.map_little_garden.hp_1 <= 0) {
            m = new Message(-79);
            m.writer().writeByte(3);
            m.writer().writeByte(0); // Brogy
            map.send_msg_all_p(m, null, true);
            m.cleanup();
            map.map_little_garden.is_finish = true;
        } else if (map.map_little_garden.hp_2 <= 0) {
            m = new Message(-79);
            m.writer().writeByte(3);
            m.writer().writeByte(1); // Brogy
            map.send_msg_all_p(m, null, true);
            m.cleanup();
            map.map_little_garden.is_finish = true;
        }
    }

    public synchronized static void add_clan_wait(Clan clan) {
        LIST.add(clan);
    }

    public synchronized static void remove_clan_wait(Clan clan) {
        LIST.remove(clan);
    }
}
