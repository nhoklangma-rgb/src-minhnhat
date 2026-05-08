package activities;

import client.Player;
import core.Service;
import io.Message;
import template.ItemTemplate7;
import template.Item_wear;
import java.io.IOException;

public class Split_Item {
    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        short num = m2.reader().readShort();
        if (act == 0 && cat == 3 && num == 1) { // add item
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null) {
                Message m = new Message(-50);
                m.writer().writeByte(0);
                m.writer().writeByte(0);
                m.writer().writeShort(id);
                m.writer().writeByte(3);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
            }
        } else if (act == 1 && cat == 3 && num == 1) { // process
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null && p.data_yesno == null && !p.isTachTB) {
                p.data_yesno = new int[] {id};
                byte id_7 = 2;
                if (it_select.template.color == 2) {
                    id_7 = 3;
                } else if (it_select.template.color == 3) {
                    id_7 = 4;
                }
                p.isTachTB = true;
                Service.send_box_yesno(p, 11, "Thông báo",
                        "Bạn có thật sự muốn phá hủy vật phẩm " + it_select.template.name
                                + " và nhận 1 " + ItemTemplate7.get_it_by_id(id_7).name + " không?",
                        new String[] {"Đồng ý", "Hủy"}, new byte[] {2, 1});
            }
        }
    }

    public static void show_table(Player p) throws IOException {
        Message m = new Message(-50);
        m.writer().writeByte(0);
        m.writer().writeByte(3);
        p.addmsg(m);
        m.cleanup();
    }
}
