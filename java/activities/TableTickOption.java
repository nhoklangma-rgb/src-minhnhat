package activities;

import client.Player;
import client.Quest;
import core.Service;
import io.Message;
import map.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import map.Mob;
import map.Vgo;
import template.PvpBang;

public class TableTickOption {
    public List<Player> listP;
    public byte[] list_check;
    public boolean is_finish = false;
    public short idDialog;

    public static void show_table(Player p, String title) throws IOException {
        if (p.tableTickOption != null) {
            Message m = new Message(-74);
            m.writer().writeByte(0);
            m.writer().writeShort(p.tableTickOption.idDialog); // id dialog
            m.writer().writeUTF(title);
            m.writer().writeByte(p.tableTickOption.listP.size());
            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                Player p0 = Map.get_player_by_name_allmap(p.tableTickOption.listP.get(i).name);
                if (p0 != null) {
                    m.writer().writeShort(p0.index_map);
                    m.writer().writeUTF(p0.name);
                    m.writer().writeShort(p0.map.template.id);
                }
            }
            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                Player p0 = Map.get_player_by_name_allmap(p.tableTickOption.listP.get(i).name);
                if (p0 != null) {
                    p0.tableTickOption = p.tableTickOption;
                    p0.addmsg(m);
                }
            }
            m.cleanup();
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        if (p.tableTickOption != null && !p.tableTickOption.is_finish) {
            byte type = m2.reader().readByte();
            short idDialog = m2.reader().readShort();
            // core.GameLogger.info(type + " " + idDialog);
            if (p.tableTickOption.idDialog == idDialog) {
                switch (idDialog) {
                    case 0: { // pho ban khong lo
                        if (type == 1) { // accept
                            if (p.tableTickOption.listP.get(0).name.equals(p.name)) {
                                for (int i = 0; i < p.tableTickOption.list_check.length; i++) {
                                    if (p.tableTickOption.list_check[i] == 0) {
                                        Service.send_box_ThongBao_OK(p,
                                                p.tableTickOption.listP.get(i).name
                                                        + " chưa tick chọn");
                                        return;
                                    }
                                }
                                //
                                String name_ok = "";
                                for (int i = 0; i < p.tableTickOption.list_check.length; i++) {
                                    if (p.tableTickOption.list_check[i] == 1) {
                                        name_ok += p.tableTickOption.listP.get(i).name + ", ";
                                    }
                                }
                                Service.send_box_yesno(p, 51, "Thông báo",
                                        ("Xác nhận tham gia phó bản khổng lồ với thành viên sau:\n"
                                                + name_ok),
                                        new String[] { "Đồng ý", "Huỷ" }, new byte[] { 2, 1 });
                            } else {
                                Message m = new Message(-74);
                                m.writer().writeByte(1);
                                m.writer().writeShort(0); // id dialog
                                m.writer().writeShort(p.index_map);
                                for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                    Player p0 = Map.get_player_by_name_allmap(
                                            p.tableTickOption.listP.get(i).name);
                                    if (p0 != null) {
                                        p0.addmsg(m);
                                    }
                                    if (p.name.equals(p.tableTickOption.listP.get(i).name)) {
                                        p.tableTickOption.list_check[i] = 1;
                                    }
                                }
                                m.cleanup();
                            }
                        } else if (type == 2) { // huy
                            Message m = new Message(-74);
                            m.writer().writeByte(3);
                            m.writer().writeShort(0); // id dialog
                            m.writer().writeShort(p.index_map);
                            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                Player p0 = Map.get_player_by_name_allmap(
                                        p.tableTickOption.listP.get(i).name);
                                if (p0 != null) {
                                    p0.addmsg(m);
                                }
                                if (p.name.equals(p.tableTickOption.listP.get(i).name)) {
                                    p.tableTickOption.list_check[i] = -1;
                                }
                            }
                            m.cleanup();
                            p.tableTickOption = null;
                        }
                        break;
                    }
                    case 1: { // pvp bang
                        if (type == 1) { // accept
                            if (p.tableTickOption.listP.get(0).name.equals(p.name)) {
                                int dem = 0;
                                for (int i = 0; i < p.tableTickOption.list_check.length; i++) {
                                    if (p.tableTickOption.list_check[i] == 0) {
                                        Service.send_box_ThongBao_OK(p,
                                                p.tableTickOption.listP.get(i).name + " chưa tick chọn");
                                        return;
                                    }
                                    if (p.tableTickOption.list_check[i] == 1) {
                                        dem++;
                                    }
                                }
                                Vgo vgo = new Vgo();
                                vgo.map_go = Map.get_map_by_id(260);
                                vgo.xnew = 530;
                                vgo.ynew = 260;
                                List<Player> list_remove_table_tick = new ArrayList<>();
                                for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                    Player p0 = p.tableTickOption.listP.get(i);
                                    if (p0 != null && p0.conn != null && p.tableTickOption.list_check[i] == 1) {
                                        p0.goto_map(vgo);
                                    }
                                    list_remove_table_tick.add(p0);
                                }
                                list_remove_table_tick.forEach(l -> l.tableTickOption = null);
                            } else {
                                Message m = new Message(-74);
                                m.writer().writeByte(1);
                                m.writer().writeShort(1); // id dialog
                                m.writer().writeShort(p.index_map);
                                for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                    Player p0 = Map.get_player_by_name_allmap(p.tableTickOption.listP.get(i).name);
                                    if (p0 != null) {
                                        p0.addmsg(m);
                                    }
                                    if (p.name.equals(p.tableTickOption.listP.get(i).name)) {
                                        p.tableTickOption.list_check[i] = 1;
                                    }
                                }
                                m.cleanup();
                            }
                        } else if (type == 2) { // huy
                            Message m = new Message(-74);
                            m.writer().writeByte(3);
                            m.writer().writeShort(1); // id dialog
                            m.writer().writeShort(p.index_map);
                            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                Player p0 = Map.get_player_by_name_allmap(p.tableTickOption.listP.get(i).name);
                                if (p0 != null) {
                                    p0.addmsg(m);
                                }
                                if (p.name.equals(p.tableTickOption.listP.get(i).name)) {
                                    p.tableTickOption.list_check[i] = -1;
                                }
                            }
                            m.cleanup();
                            p.tableTickOption = null;
                        }
                        break;
                    }
                }
            }
        }
    }
}
