package activities;

import client.Player;
import client.Quest;
import core.Service;
import io.Message;
import map.Map;
import map.Npc;
import map.Vgo;
import template.EffTemplate;
import template.Map_pvp;
import java.io.IOException;

public class Pvp {
    public static void show_table(Player p) throws IOException {
        Service.UpdatePvpPoint(p);
        //
        Message m = new Message(-63);
        m.writer().writeByte(0);
        m.writer().writeShort(p.map.players.size());
        p.addmsg(m);
        m.cleanup();
        //
        p.pvp_target = null;
        p.pvp_accept = false;
    }

    public static void pvp_notice(Player p, int type) throws IOException {
        Message m = new Message(36);
        m.writer().writeByte(type);
        p.addmsg(m);
        m.cleanup();
    }

    public static void show_info(Player p, int countDown, int left, int right, int maxWin)
            throws IOException {
        Message m = new Message(-73);
        m.writer().writeByte(5);
        m.writer().writeShort(countDown);
        m.writer().writeByte(left);
        m.writer().writeByte(right);
        m.writer().writeByte(maxWin);
        p.addmsg(m);
        m.cleanup();
    }

    public static void find_out_other(Player p, Player p0) throws IOException {
        Message m = new Message(-63);
        m.writer().writeByte(3);
        m.writer().writeUTF("Ẩn danh");
        m.writer().writeByte(0); // clazz
        p.addmsg(m);
        m.cleanup();
        //
        p.pvp_accept = false;
        if (p.clone) {
            p.pvp_accept = true;
        }
        if (p0.clone) {
            p0.pvp_accept = true;
        }
        EffTemplate ef = p.get_eff(19);
        if (ef != null) {
            ef.time = System.currentTimeMillis() + 30_000;
        } else {
            p.add_new_eff(19, 1, 30_000);
        }
    }

    public synchronized static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        switch (act) {
            case 2: {
                if (p.pvp_target != null && p.pvp_target.equals(p)) {
                    stop_find(p);
                }
                break;
            }
            case 1: {
                start_find(p);
                break;
            }
            case 4: {
                if (p.pvp_target != null && !p.pvp_target.equals(p) && p.pvp_target.conn != null
                        && p.map.equals(p.pvp_target.map) && p.map.template.id == 1000) {
                    Message m = new Message(-63);
                    m.writer().writeByte(4);
                    m.writer().writeByte(0);
                    p.addmsg(m);
                    m.cleanup();
                    m = new Message(-63);
                    m.writer().writeByte(4);
                    m.writer().writeByte(1);
                    p.pvp_target.addmsg(m);
                    m.cleanup();
                    p.pvp_accept = true;
                    if (p.pvp_accept) {
                        p.map.leave_map(p, 2);
                        p.type_pk = -1;
                        // create map
                        Map maptemp = Map.get_map_by_id(58)[0];
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
                        p.pvp_target.map = map_create;
                        p.pvp_target.x = 380;
                        p.pvp_target.y = 240;
                        p.pvp_target.xold = p.x;
                        p.pvp_target.yold = p.y;
                        p.pvp_target.map.goto_map(p.pvp_target);
                        Service.update_PK(p.pvp_target, p.pvp_target, true);
                        Service.pet(p.pvp_target, p.pvp_target, true);
                        Quest.update_map_have_side_quest(p.pvp_target, true);
                        //
                        map_create.map_pvp = new Map_pvp();
                        map_create.map_pvp.time_pvp = 5;
                        map_create.map_pvp.status_pvp = 0;
                        map_create.map_pvp.num_win_p1 = 0;
                        map_create.map_pvp.num_win_p2 = 0;
                        map_create.map_pvp.type_map = 0;// map pvp
                        map_create.start_map();
                        Map.add_map_plus(map_create);
                    }
                }
                break;
            }
            case 5: {
                if (p.pvp_target != null && !p.pvp_target.equals(p) && p.pvp_target.conn != null) {
                    show_table(p.pvp_target);
                    start_find(p.pvp_target);
                    Service.send_box_ThongBao_OK(p.pvp_target,
                            "Đối thủ rời đi, bạn quay lại hàng chờ");
                }
                p.pvp_target = null;
                //
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
        }
    }

    private static void stop_find(Player p) throws IOException {
        Message m = new Message(-63);
        m.writer().writeByte(2);
        p.addmsg(m);
        m.cleanup();
        //
        p.pvp_target = null;
    }

    public static void start_find(Player p) throws IOException {
        Message m = new Message(-63);
        m.writer().writeByte(1);
        p.addmsg(m);
        m.cleanup();
        //
        p.pvp_target = p;
        p.pvp_accept = false;
    }
}
