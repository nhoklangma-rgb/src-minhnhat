package activities;

import java.io.IOException;
import client.Player;
import client.Quest;
import core.Service;
import core.Util;
import io.Message;
import map.Map;
import template.Map_pvp;

public class Fight {
    public synchronized static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte typeFight = m2.reader().readByte();
//        if (type == 0 && typeFight == 0) {
//            return;
//        }
        if (type == 0) {
            Player p0 = p.map.get_player_by_id_inmap(id);
            if (p0 != null && id != p.index_map) {
                if (p0.targetFight != null) {
                    Service.send_box_ThongBao_OK(p, "Đối phương đang nhận lời mời từ người khác");
                } else {
                    Message m = new Message(-35);
                    m.writer().writeByte(0);
                    m.writer().writeShort(id);
                    m.writer().writeUTF(p.name);
                    m.writer().writeShort(99);
                    m.writer().writeByte(0);
                    p0.addmsg(m);
                    m.cleanup();
                    p0.targetFight = p;
                }
            } else {
                Service.send_box_ThongBao_OK(p, "Đối phương offline");
            }
        } else if (type == 1 && id == p.index_map && p.targetFight != null
                && p.targetFight.map.equals(p.map)) {
            if (p.targetFight.targetFight != null) {
                p.targetFight.targetFight = null;
                p.targetFight = null;
                Service.send_box_ThongBao_OK(p,
                        "Đối phương đang nhận lời mời từ người khác, hãy thử lại sau");
            } else {
                p.targetFight.targetFight = p;
                //
                p.map.leave_map(p, 2);
                p.targetFight.map.leave_map(p.targetFight, 2);
                p.type_pk = -1;
                p.targetFight.type_pk = -1;
                //
                // create map
                short[] mapID = new short[] {120, 122, 123};
                Map maptemp = Map.get_map_by_id(mapID[Util.random(mapID.length)])[0];
                Map map_create = new Map();
                map_create.template = maptemp.template;
                map_create.zone_id = (byte) 0;
                map_create.list_mob = new int[0];
                //
                p.map = map_create;
                p.x = 320;
                p.y = 240;
                p.xold = p.x;
                p.yold = p.y;
                p.map.goto_map(p);
                Service.update_PK(p, p, true);
                Service.pet(p, p, true);
                Quest.update_map_have_side_quest(p, true);
                //
                p.targetFight.map = map_create;
                p.targetFight.x = 380;
                p.targetFight.y = 240;
                p.targetFight.xold = p.x;
                p.targetFight.yold = p.y;
                p.targetFight.map.goto_map(p.targetFight);
                Service.update_PK(p.targetFight, p.targetFight, true);
                Service.pet(p.targetFight, p.targetFight, true);
                Quest.update_map_have_side_quest(p.targetFight, true);
                //
                map_create.map_pvp = new Map_pvp();
                map_create.map_pvp.time_pvp = 5;
                map_create.map_pvp.status_pvp = 0;
                map_create.map_pvp.num_win_p1 = 0;
                map_create.map_pvp.num_win_p2 = 0;
                map_create.map_pvp.type_map = 1; // map fight giao huu
                map_create.start_map();
                Map.add_map_plus(map_create);
            }
        }
    }
}
