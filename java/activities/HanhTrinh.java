package activities;

import client.Player;
import core.Service;
import io.Message;
import java.io.IOException;
import java.util.List;
import template.ItemBag47;
import template.ItemTemplate4;
import template.ItemTemplate4_Info;

public class HanhTrinh {
    public static final String[] NAME = new String[] { "Làng Foosha", "TT. Vỏ Sò", "TT. Orange",
            "Làng Sirup", "Baratie", "Làng hạt dẻ", "TT. Khởi Đầu", "TT. Whiskey", "TT. HORN",
            "TT. Nanohana", "Đảo trời", "Water 7" };
    public static final short[] ICON = new short[] { 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814 };
    public static final short[] LANG = new short[] { 1, 9, 17, 25, 33, 41, 49, 69, 83, 93, 113, 191 };

    public static void show_table(Player p, int type) throws IOException {
        if (type == 0) {
            Message m = new Message(79);
            m.writer().writeByte(4);
            m.writer().writeUTF("Bản đồ hành trình");
            m.writer().writeByte(NAME.length);
            for (int i = 0; i < NAME.length; i++) {
                m.writer().writeUTF(NAME[i]);
                m.writer().writeShort(p.get_icon_daHanhTrinh(i));
            }
            p.addmsg(m);
            m.cleanup();
        } else if (type == 1) {
            Message m = new Message(79);
            m.writer().writeByte(0);
            m.writer().writeShort(get_map(p));
            m.writer().writeUTF(p.map.template.name);
            List<ItemBag47> list_DaHanhTrinh = p.get_list_daHanhTrinh_total(p.map.template.id);
            m.writer().writeByte(list_DaHanhTrinh.size());
            for (int i = 0; i < list_DaHanhTrinh.size(); i++) {
                ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(list_DaHanhTrinh.get(i).id);
                m.writer().writeUTF(itemTemplate4.name);
                m.writer().writeByte(4);
                m.writer().writeShort(itemTemplate4.id);
                m.writer().writeByte(1);
                m.writer().writeShort(itemTemplate4.icon);
                ItemTemplate4_Info temp_info = ItemTemplate4_Info.get_by_id(itemTemplate4.indexInfoPotion);
                if (temp_info != null) {
                    m.writer().writeUTF(temp_info.info);
                } else {
                    m.writer().writeUTF("Chưa có thông tin");
                }
            }
            p.addmsg(m);
            m.cleanup();
            //
            update_da_kham(p);
        }
    }

    public static int get_map(Player p) {
        for (int i = 0; i < LANG.length; i++) {
            if (p.map.template.id == LANG[i]) {
                return ICON[i];
            }
        }
        return ICON[0];
    }

    public static void update_da_kham(Player p) throws IOException {
        Message m = new Message(79);
        m.writer().writeByte(1);
        ItemBag47 it_select = p.get_daHanhTrinh(p.map.template.id);
        if (it_select != null) {
            m.writer().writeByte(1);
            //
            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(it_select.id);
            m.writer().writeUTF(itemTemplate4.name);
            m.writer().writeByte(4);
            m.writer().writeShort(itemTemplate4.id);
            m.writer().writeShort(itemTemplate4.icon);
            ItemTemplate4_Info temp_info = ItemTemplate4_Info.get_by_id(itemTemplate4.indexInfoPotion);
            if (temp_info != null) {
                m.writer().writeUTF(temp_info.info);
            } else {
                m.writer().writeUTF("Chưa có thông tin");
            }
        } else {
            m.writer().writeByte(0);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        if (act == 2) {
            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id);
            if (itemTemplate4 != null) {
                ItemBag47 it_select = null;
                for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                    if (p.daHanhTrinh.get(i).category > -1
                            && p.map.template.id == LANG[p.daHanhTrinh.get(i).category]
                            && p.daHanhTrinh.get(i).id == id && p.daHanhTrinh.get(i).quant == 0) {
                        it_select = p.daHanhTrinh.get(i);
                    }
                }
                if (it_select != null) {
                    p.data_yesno = new int[] { id };
                    Service.send_box_yesno(p, 58, "Thông báo",
                            "Bạn có muốn khảm " + itemTemplate4.name + "?",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, -1 });
                } else {
                    Service.send_box_ThongBao_OK(p, "Không đủ 1 " + itemTemplate4.name);
                }
            }
        } else if (act == 3) {
            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id);
            if (itemTemplate4 != null) {
                ItemBag47 it_select = null;
                for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                    if (p.daHanhTrinh.get(i).category > -1
                            && p.map.template.id == LANG[p.daHanhTrinh.get(i).category]
                            && p.daHanhTrinh.get(i).id == id && p.daHanhTrinh.get(i).quant == 1) {
                        it_select = p.daHanhTrinh.get(i);
                    }
                }
                if (it_select != null) {
                    p.data_yesno = new int[] { id };
                    Service.send_box_yesno(p, 59, "Thông báo",
                            "Bạn muốn tách " + itemTemplate4.name + " với phí 100 Ruby?",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, -1 });
                } else {
                    Service.send_box_ThongBao_OK(p, "Không đủ 1 " + itemTemplate4.name);
                }
            }
        }
    }
}
