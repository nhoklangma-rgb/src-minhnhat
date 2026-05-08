package activities;

import client.Player;
import core.Service;
import io.Message;
import java.io.IOException;
import template.ItemTemplate4;
import template.Item_wear;

public class ChuyenHoa {
    public static void show_table(Player p) throws IOException {
        Message m = new Message(-77);
        m.writer().writeByte(0);
        p.addmsg(m);
        m.cleanup();
        p.item_chuyenhoa_save_0 = null;
        p.item_chuyenhoa_save_1 = null;
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short idLeft = m2.reader().readShort();
        short idRight = -1;
        if (type == 2 || type == 3) {
            idRight = m2.reader().readShort();
        }
        switch (type) {
            case 1: {
                if (idRight == -1 && idLeft < p.item.bag3.length) {
                    Item_wear it_select = p.item.bag3[idLeft];
                    if (it_select != null) {
                        Message m = new Message(-77);
                        m.writer().writeByte(1);
                        if (it_select.levelup <= 5) {
                            m.writer().writeByte(1);
                            p.item_chuyenhoa_save_1 = it_select;
                        } else {
                            m.writer().writeByte(0);
                            p.item_chuyenhoa_save_0 = it_select;
                        }
                        m.writer().writeShort(idLeft);
                        p.addmsg(m);
                        m.cleanup();
                    }
                }
                break;
            }
            case 2: {
                if (p.item_chuyenhoa_save_0 != null && p.item_chuyenhoa_save_1 != null
                        && p.item_chuyenhoa_save_0.levelup > p.item_chuyenhoa_save_1.levelup) {

                    if (p.item_chuyenhoa_save_0.template.typeEquip == 7
                            || p.item_chuyenhoa_save_1.template.typeEquip == 7) {
                        Service.send_box_ThongBao_OK(p,
                                "Không thể chuyển hoá đối với Dial");
                        return;
                    }
                    if (p.item_chuyenhoa_save_0.template.tb2 == 1
                            || p.item_chuyenhoa_save_1.template.tb2 == 1) {
                        Service.send_box_ThongBao_OK(p,
                                "Không thể chuyển hoá đối với Trang bị 2");
                        return;
                    }

                    if (p.item_chuyenhoa_save_0.levelup >= 10) {
                        ItemTemplate4 it_bh = null;
                        switch (p.item_chuyenhoa_save_0.levelup) {
                            case 15: {
                                it_bh = ItemTemplate4.get_it_by_id(551);
                                p.data_yesno = new int[] { 551 };
                                break;
                            }
                            case 13:
                            case 14: {
                                it_bh = ItemTemplate4.get_it_by_id(550);
                                p.data_yesno = new int[] { 550 };
                                break;
                            }
                            default: { // 10, 11, 12
                                it_bh = ItemTemplate4.get_it_by_id(549);
                                p.data_yesno = new int[] { 549 };
                                break;
                            }
                        }
                        Service.send_box_yesno(p, 8, "Thông báo",
                                ("Cần phải sử dụng : \n" + it_bh.name),
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                    } else {
                        Service.send_box_yesno(p, 6, "Thông báo",
                                ("Bạn có muốn thực hiện chuyển hóa với mức phí là 800 Ruby?"),
                                new String[] { "800", "Đóng" }, new byte[] { 7, -1 });
                    }
                }
                break;
            }
        }
    }

    public static void show_result(Player p, String s, int lv) throws IOException {
        Message m = new Message(-77);
        m.writer().writeByte(3);
        m.writer().writeUTF(s);
        m.writer().writeByte(lv);
        p.addmsg(m);
        m.cleanup();
    }
}
