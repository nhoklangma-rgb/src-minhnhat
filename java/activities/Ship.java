package activities;

import java.util.Timer;
import java.util.TimerTask;
import client.Player;
import core.Service;
import core.Util;
import io.Message;
import template.ItemTemplate4;
import template.Ship_pet;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ship {

    public static void show_table(Player p) throws IOException {
        if ((p.map.template.id == 1 || p.map.template.id == 22) && p.typePirate == 0) {
            Message m = new Message(-19);
            m.writer().writeByte(101);
            m.writer().writeUTF("Lái buôn");
            m.writer().writeByte(4);
            m.writer().writeShort(4);
            //
            m.writer().writeShort(39);
            m.writer().writeShort(1);
            m.writer().writeShort(38);
            m.writer().writeShort(1);
            m.writer().writeShort(37);
            m.writer().writeShort(1);
            m.writer().writeShort(36);
            m.writer().writeShort(1);
            p.addmsg(m);
            m.cleanup();
            notice_ship_packet(p, 36);
        }
    }

    private static void notice_ship_packet(Player p, int type) throws IOException {
        if ((p.map.template.id == 1 || p.map.template.id == 22) && p.typePirate == 0) {
            Message m = new Message(-53);
            m.writer().writeByte(0);
            m.writer()
                    .writeUTF("Gói hàng hiện tại của bạn là " + ItemTemplate4.get_item_name(type));
            m.writer().writeShort(type);
            p.addmsg(m);
            m.cleanup();
            //
            p.id_ship_packet = (short) type;
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        if ((p.map.template.id == 1 || p.map.template.id == 22) && p.typePirate == 0) {
            byte act = m2.reader().readByte();
            // core.GameLogger.info(act);
            switch (act) {
                case 0: {
                    if (p.item.total_item_bag_by_id(4, 361) > 0) {
                        if (80 > Util.random(120)) {
                            Ship.notice_ship_packet(p, 36);
                        } else if (90 > Util.random(120)) {
                            Ship.notice_ship_packet(p, 37);
                        } else if (95 > Util.random(120)) {
                            Ship.notice_ship_packet(p, 38);
                        } else {
                            Ship.notice_ship_packet(p, 39);
                        }
                        p.item.remove_item47(4, 361, 1);
                        p.item.update_Inventory(-1, false);
                    } else {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ 1 " + ItemTemplate4.get_item_name(361));
                    }
                    break;
                }
                case 1: {
                    if (p.id_ship_packet != -1) {
                        int giahang = 0; // Khởi tạo giá trị mặc định của giahang
                        switch (p.id_ship_packet) {
                            case 36:
                                giahang = 250_000;
                                break;
                            case 37:
                                giahang = 500_000;
                                break;
                            case 38:
                                giahang = 1_000_000;
                                break;
                            case 39:
                                giahang = 5_000_000;
                                break;
                            default:
                                core.GameLogger.error("Unknown ship packet ID: " + p.id_ship_packet);
                                break;
                        }
                        String formattedGiahang = NumberFormat.getInstance().format(giahang);
                        Service.send_box_yesno(p, 50, "Thông báo",
                                "Để tham gia lái buôn, bạn phải mất " + formattedGiahang + " Beri, bạn có "
                                        + "muốn tham gia?",
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 6, -1 });
                    }
                    break;
                }

            }
        }
    }

    public static void notice_start_shipping(Player p) throws IOException {
        if (p.typePirate == 0) {
            Message m = new Message(-53);
            m.writer().writeByte(1);
            m.writer().writeUTF("Gói hàng của bạn đây, bạn có 60 giây để chuyển hàng, lên đường may mắn");
            p.addmsg(m);
            m.cleanup();
        }
    }
}
