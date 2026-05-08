package activities;

import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import template.Item_wear;

public class UpgradeSuperItem {

    public static void show_table(Player p) throws IOException {
        Message m = new Message(66);
        m.writer().writeByte(7);
        p.addmsg(m);
        m.cleanup();
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte beri_gem = m2.reader().readByte();
        byte num = m2.reader().readByte();
        if (type == 4 && beri_gem == 0 && num == 0) { // add item3
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null) {
                if (!UpgradeSuperItem.check_it_can_upgrade_super(p, it_select)) {
                    return;
                }
                Message m = new Message(66);
                m.writer().writeByte(4);
                m.writer().writeShort(id);
                m.writer().writeShort(19);
                m.writer().writeShort(get_botvang(it_select.levelup));
                m.writer().writeShort(18);
                m.writer().writeShort(get_botCH(it_select.levelup));
                p.addmsg(m);
                m.cleanup();
            }
            {
                Message m = new Message(66);
                m.writer().writeByte(6);
                m.writer().writeByte(1);
                m.writer().writeShort(6);
                m.writer().writeByte(get_mairua(it_select.levelup));
                p.addmsg(m);
                m.cleanup();
            }
            {
                Message m = new Message(66);
                m.writer().writeByte(14);
                m.writer().writeByte(1);
                m.writer().writeShort(10);
                p.addmsg(m);
                m.cleanup();
            }
            {
                Message m = new Message(66);
                m.writer().writeByte(5);
                m.writer().writeByte(1);
                m.writer().writeShort(11);
                p.addmsg(m);
                m.cleanup();
            }
        } else if (type == 1 && beri_gem == 0 && num == 0) { // request
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null) {
                int vang_req = get_extol_up(it_select.levelup);
                int material_0 = get_botCH(it_select.levelup);
                int material_1 = get_botvang(it_select.levelup);
                int maiRua_req = get_mairua(it_select.levelup);
                int khien_req = 1;
                int thienThach_req = 1;
                if (!UpgradeSuperItem.check_it_can_upgrade_super(p, it_select)) {
                    return;
                }
                if (p.item.total_item_bag_by_id(7, 18) < material_0) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + material_0 + " Bột siêu cấp");
                    return;
                }
                if (p.item.total_item_bag_by_id(7, 19) < material_1) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + material_1 + " Tinh thể vàng");
                    return;
                }
                if (p.item.total_item_bag_by_id(7, 6) < maiRua_req) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + maiRua_req + " Mai rùa");
                    return;
                }
                if (p.item.total_item_bag_by_id(7, 10) < khien_req) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + khien_req + " Khiên");
                    return;
                }
                if (p.item.total_item_bag_by_id(7, 11) < thienThach_req) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + thienThach_req + " Thiên thạch may mắn");
                    return;
                }
                if (p.get_vnd() < vang_req) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Extol");
                    return;
                }
                Service.send_box_yesno(p, 69, "Thông báo",
                        ("Bạn có muốn cường hoá\n" + it_select.template.name + " lên cấp "
                                + (it_select.levelup + 1)),
                        new String[] { Util.number_format(vang_req), "Auto", "Đóng" }, new byte[] { 69, 3, -1 });
                p.data_yesno = new int[] { id };
            }
        } else if (type == 7 && id == 0 && beri_gem == 0 && num == 0) { // shop open upgrade
            UpgradeSuperItem.show_table(p);
        }
    }

    public static void normalUpgrade(Player p, int id) throws IOException {
        Item_wear it_select = p.item.bag3[id];
        if (it_select == null)
            return;
        int vang_req = get_extol_up(it_select.levelup);
        int material_0 = get_botCH(it_select.levelup);
        int material_1 = get_botvang(it_select.levelup);
        int maiRua_req = get_mairua(it_select.levelup);
        int khien_req = 1;
        int thienThach_req = 1;
        p.update_vnd(-vang_req);
        p.update_money();
        p.item.remove_item47(7, 18, material_0);
        p.item.remove_item47(7, 19, material_1);
        p.item.remove_item47(7, 6, maiRua_req);
        p.item.remove_item47(7, 10, khien_req);
        p.item.remove_item47(7, 11, thienThach_req);
        p.item.update_Inventory(-1, false);
        int successChance = Math.max(1, 100 - it_select.levelup);
        boolean suc = successChance > Util.random(150);
        Message m = new Message(66);
        if (suc) {
            it_select.levelup++;
            UpgradeDial.sendChatKTG(p, it_select, 1);
            m.writer().writeByte(2);
            m.writer().writeUTF("Cường hóa thành công\n"
                    + it_select.template.name + " lên cấp " + it_select.levelup);
        } else {
            m.writer().writeByte(3);
            m.writer().writeUTF("Cường hóa thất bại\n"
                    + it_select.template.name + " không bị rớt cấp");
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void autoUpgrade(Player p, int id) throws IOException {
        Item_wear it_select = p.item.bag3[id];
        if (it_select == null)
            return;
        int count_upgrade_attempts = 0;
        int total_material_0_used = 0;
        int total_material_1_used = 0;
        int total_MaiRua = 0;
        int total_Khien = 0;
        int total_ThienThach = 0;
        long total_vang_used = 0;
        boolean success = false;
        while (it_select.levelup < 100 && count_upgrade_attempts < p.solannang) {
            int vang_req = get_extol_up(it_select.levelup);
            int material_0 = get_botCH(it_select.levelup);
            int material_1 = get_botvang(it_select.levelup);
            int maiRua_req = get_mairua(it_select.levelup);
            int khien_req = 1;
            int thienThach_req = 1;
            if (p.item.total_item_bag_by_id(7, 18) < material_0 ||
                    p.item.total_item_bag_by_id(7, 19) < material_1 ||
                    p.item.total_item_bag_by_id(7, 6) < maiRua_req ||
                    p.item.total_item_bag_by_id(7, 10) < khien_req ||
                    p.item.total_item_bag_by_id(7, 11) < thienThach_req ||
                    p.get_vnd() < vang_req) {
                break;
            }
            p.update_vnd(-vang_req);
            total_vang_used += vang_req;
            // p.update_money(); // MOVED OUTSIDE LOOP
            p.item.remove_item47(7, 18, material_0);
            p.item.remove_item47(7, 19, material_1);
            total_material_0_used += material_0;
            total_material_1_used += material_1;
            p.item.remove_item47(7, 6, maiRua_req);
            total_MaiRua += maiRua_req;
            p.item.remove_item47(7, 10, khien_req);
            total_Khien += khien_req;
            p.item.remove_item47(7, 11, thienThach_req);
            total_ThienThach += thienThach_req;
            count_upgrade_attempts++;
            int successChance = Math.max(1, 100 - it_select.levelup);
            boolean suc = successChance > Util.random(150);
            if (suc) {
                it_select.levelup++;
                success = true;
                UpgradeDial.sendChatKTG(p, it_select, count_upgrade_attempts);
                break;
            }
        }
        p.update_money(); // MOVED HERE
        p.item.update_Inventory(-1, false); // MOVED HERE
        Message m = new Message(66);
        m.writer().writeByte(success ? 2 : 3);
        String summaryMessage = "== Auto Siêu Cường Hóa ==\n"
                + it_select.template.name + " +" + it_select.levelup + "\n"
                + "- Kết quả : " + (success ? "Thành công" : "Thất bại") + "\n"
                + "------------------------\n"
                + "- Số lần cường hóa : " + count_upgrade_attempts + "\n"
                + "- Extol : " + String.format("%,d", total_vang_used) + "\n"
                + "------------------------\n"
                + "Nguyên liệu : "
                + total_MaiRua + " Mai rùa, "
                + total_Khien + " Khiên, "
                + total_ThienThach + " Thiên thạch may mắn, "
                + total_material_0_used + " Bột siêu cấp, "
                + total_material_1_used + " Tinh thể vàng.";
        m.writer().writeUTF(summaryMessage);
        p.addmsg(m);
        m.cleanup();
        p.item.update_Inventory(-1, false);
    }

    private static int getStep(byte levelup) {
        return Math.max(0, levelup - 9);
    }

    private static int get_botCH(byte levelup) {
        return getStep(levelup) * 20;
    }

    private static int get_botvang(byte levelup) {
        return getStep(levelup) * 10;
    }

    private static int get_mairua(byte levelup) {
        return getStep(levelup) * 5;
    }

    private static int get_extol_up(byte levelup) {
        return getStep(levelup) * 3_000;
    }

    private static boolean check_it_can_upgrade_super(Player p, Item_wear it_select)
            throws IOException {
        if (it_select.template.typeEquip >= 6 || it_select.template.tb2 == 1) {
            Service.send_box_ThongBao_OK(p, "Không thể siêu cường hoá với \nDial, Quả Tim, Trang Bị 2");
            return false;
        }
        if (it_select.levelup < 10) {
            Service.send_box_ThongBao_OK(p, "Yêu cầu trang bị cấp +10 trở lên");
            return false;
        }
        if (it_select.levelup >= 100) {
            Service.send_box_ThongBao_OK(p, "Trang bị này đã đạt cấp tối đa +100");
            return false;
        }
        return true;
    }
}
