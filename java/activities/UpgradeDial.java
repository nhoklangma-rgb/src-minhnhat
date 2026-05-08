package activities;

import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import template.Item_wear;
import template.Option;

public class UpgradeDial {
    public static void show_table(Player p) throws IOException {
        Message m = new Message(-94);
        m.writer().writeByte(7);
        p.addmsg(m);
        m.cleanup();
        p.data_yesno = null;
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte beri_gem = m2.reader().readByte();
        byte num = m2.reader().readByte();
        if (type == 4 && beri_gem == 0 && num == 0) { // select item
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null) {
                if ((it_select.template.tb2 == 0
                        && (it_select.template.typeEquip == 6 || it_select.template.typeEquip == 7))
                        || it_select.template.tb2 == 1) {
                    Message m = new Message(-94);
                    m.writer().writeByte(4);
                    m.writer().writeShort(id);
                    m.writer().writeByte(6);
                    m.writer().writeShort(18);
                    m.writer().writeShort(get_botCH(it_select.levelup));
                    m.writer().writeShort(19);
                    m.writer().writeShort(get_botvang(it_select.levelup));
                    m.writer().writeShort(13);
                    m.writer().writeShort(get_longvu(it_select.levelup));
                    m.writer().writeShort(11);
                    m.writer().writeShort(get_thienthach(it_select.levelup));
                    m.writer().writeShort(6);
                    m.writer().writeShort(get_mairua(it_select.levelup));
                    m.writer().writeShort(10);
                    m.writer().writeShort(get_khien(it_select.levelup));
                    m.writer().writeInt(get_beri_up(it_select.levelup));
                    m.writer().writeInt(get_ruby_up(it_select.levelup));
                    m.writer().writeInt(get_extol_up(it_select.levelup));
                    m.writer().writeByte(45);
                    p.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_box_ThongBao_OK(p, "Trang bị cường hoá phải đúng loại\n Dial, Quả Tim, Trang Bị 2");
                }
            }
        } else if (type == 1 && beri_gem == 0 && num == 0) { // start
            Item_wear it_select = p.item.bag3[id];
            if (it_select != null) {
                if (it_select.levelup < 100) {
                    int beri_req = get_beri_up(it_select.levelup);
                    int extol_req = get_extol_up(it_select.levelup);
                    int botCH_req = get_botCH(it_select.levelup);
                    int botvang_req = get_botvang(it_select.levelup);
                    int longvu_req = get_longvu(it_select.levelup);
                    int thienThach_req = get_thienthach(it_select.levelup);
                    int maiRua_req = get_mairua(it_select.levelup);
                    int khien_req = get_khien(it_select.levelup);
                    if (p.get_vang() < beri_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(beri_req) + " Beri");
                        return;
                    }
                    if (p.get_vnd() < extol_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(extol_req) + " Extol");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(7, 18) < botCH_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + botCH_req + " Bột siêu cấp");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(7, 19) < botvang_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + botvang_req + " Tinh thể vàng");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(7, 13) < longvu_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + longvu_req + " Lông vũ");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(7, 11) < thienThach_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + thienThach_req + " Thiên thạch may mắn");
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
                    Service.send_box_yesno(p, 68, "Thông báo",
                            ("Bạn có muốn cường hoá\n" + it_select.template.name + " lên cấp "
                                    + (it_select.levelup + 1)),
                            new String[] { Util.number_format(extol_req), "Auto", "Đóng" }, new byte[] { 69, 3, -1 });
                    p.data_yesno = new int[] { id };
                } else {
                    Service.send_box_ThongBao_OK(p, "Vật phẩm đã đạt cấp tối đa");
                }

            }
        }
    }

    public static void normalUpgrade(Player p, int id) throws IOException {
        Item_wear it_select = p.item.bag3[id];
        int beri_req = get_beri_up(it_select.levelup);
        int extol_req = get_extol_up(it_select.levelup);
        int botCH_req = get_botCH(it_select.levelup);
        int botvang_req = get_botvang(it_select.levelup);
        int longvu_req = get_longvu(it_select.levelup);
        int thienThach_req = get_thienthach(it_select.levelup);
        int maiRua_req = get_mairua(it_select.levelup);
        int khien_req = get_khien(it_select.levelup);
        p.update_vang(-beri_req);
        p.update_vnd(-extol_req);
        p.update_money();
        p.item.remove_item47(7, 18, botCH_req);
        p.item.remove_item47(7, 19, botvang_req);
        p.item.remove_item47(7, 13, longvu_req);
        p.item.remove_item47(7, 11, thienThach_req);
        p.item.remove_item47(7, 6, maiRua_req);
        p.item.remove_item47(7, 10, khien_req);
        p.item.update_Inventory(-1, false);
        int successChance = Math.max(1, 100 - it_select.levelup);
        boolean suc = successChance > Util.random(150);
        if (suc) {
            it_select.levelup++;
            congOptions(p, id);
            sendChatKTG(p, it_select, 1);
            Message m = new Message(-94);
            m.writer().writeByte(2);
            m.writer().writeUTF("Cường hoá thành công\n"
                    + it_select.template.name + " lên cấp " + it_select.levelup);
            p.addmsg(m);
            m.cleanup();
        } else {
            Message m = new Message(-94);
            m.writer().writeByte(3);
            m.writer().writeUTF("Cường hoá thất bại\n"
                    + it_select.template.name + " không bị rớt cấp");
            p.addmsg(m);
            m.cleanup();
        }
        p.item.update_Inventory(-1, false);
    }

    public static void autoUpgrade(Player p, int id) throws IOException {
        Item_wear it_select = p.item.bag3[id];
        int totalUpgrades = 0;
        long totalBeriSpent = 0;
        int totalExtolSpent = 0;
        int totalBotCHUsed = 0;
        int totalBotVangUsed = 0;
        int totalLongVuUsed = 0;
        int totalMaiRuaUsed = 0;
        int totalKhienUsed = 0;
        int totalThienThachUsed = 0;
        int targetLevel = 100;
        boolean success = false;
        while (it_select.levelup < targetLevel) {
            if (totalUpgrades >= p.solannang) {
                break;
            }
            int beri_req = get_beri_up(it_select.levelup);
            int extol_req = get_extol_up(it_select.levelup);
            int botCH_req = get_botCH(it_select.levelup);
            int botvang_req = get_botvang(it_select.levelup);
            int longvu_req = get_longvu(it_select.levelup);
            int thienThach_req = get_thienthach(it_select.levelup);
            int maiRua_req = get_mairua(it_select.levelup);
            int khien_req = get_khien(it_select.levelup);
            if (p.get_vang() < beri_req || p.get_vnd() < extol_req ||
                    p.item.total_item_bag_by_id(7, 18) < botCH_req ||
                    p.item.total_item_bag_by_id(7, 19) < botvang_req ||
                    p.item.total_item_bag_by_id(7, 13) < longvu_req ||
                    p.item.total_item_bag_by_id(7, 11) < thienThach_req ||
                    p.item.total_item_bag_by_id(7, 6) < maiRua_req ||
                    p.item.total_item_bag_by_id(7, 10) < khien_req) {
                break;
            }
            p.update_vang(-beri_req);
            p.update_vnd(-extol_req);
            // p.update_money(); // MOVED OUTSIDE
            p.item.remove_item47(7, 18, botCH_req);
            p.item.remove_item47(7, 19, botvang_req);
            p.item.remove_item47(7, 13, longvu_req);
            p.item.remove_item47(7, 11, thienThach_req);
            p.item.remove_item47(7, 6, maiRua_req);
            p.item.remove_item47(7, 10, khien_req);

            totalBeriSpent += beri_req;
            totalExtolSpent += extol_req;
            totalBotCHUsed += botCH_req;
            totalBotVangUsed += botvang_req;
            totalLongVuUsed += longvu_req;
            totalThienThachUsed += thienThach_req;
            totalMaiRuaUsed += maiRua_req;
            totalKhienUsed += khien_req;
            // p.item.update_Inventory(-1, false); // MOVED OUTSIDE
            totalUpgrades++;
            int successChance = Math.max(1, 100 - it_select.levelup);
            boolean suc = successChance > Util.random(150);
            if (suc) {
                it_select.levelup++;
                success = true;
                congOptions(p, id);
                sendChatKTG(p, it_select, totalUpgrades);
            }
            if (it_select.levelup >= targetLevel) {
                break;
            }
        }
        p.update_money(); // MOVED HERE
        p.item.update_Inventory(-1, false); // MOVED HERE
        Message m = new Message(-94);
        m.writer().writeByte(success ? (byte) 2 : (byte) 3);
        String result = success ? "Thành công" : "Thất bại";
        String summaryMessage = "== Auto Nâng Dial ==\n"
                + "- Kết quả: " + result + "\n"
                + "- Cấp độ hiện tại: +" + it_select.levelup + "\n"
                + "------------------------" + "\n"
                + "- Số lần nâng cấp: " + totalUpgrades + "\n"
                + "- Beri : " + String.format("%,d", totalBeriSpent) + "\n"
                + "- Extol : " + String.format("%,d", totalExtolSpent) + "\n"
                + "------------------------" + "\n"
                + "- Nguyên liệu : " + String.format("%,d", totalBotCHUsed) + " Bột siêu cấp, "
                + String.format("%,d", totalBotVangUsed) + " Tinh thể vàng, "
                + totalLongVuUsed + " Lông vũ, "
                + totalThienThachUsed + " Thiên thạch may mắn, "
                + totalMaiRuaUsed + " Mai rùa, "
                + totalKhienUsed + " Khiên";
        m.writer().writeUTF(summaryMessage);
        p.addmsg(m);
        m.cleanup();
        p.update_money();
        p.item.update_Inventory(-1, false);
    }

    public static void congOptions(Player p, int id) throws IOException {
        Item_wear it_select = p.item.bag3[id];
        int multiplier = 1;
        if (it_select.template.id == 11082 || it_select.template.id == 11038 || it_select.template.id == 11039
                || it_select.template.id == 11040) {
            multiplier = 2;
        }
        int colorBonus = (it_select.template.id > 12000) ? it_select.template.color * 20 : 0;
        if (it_select.levelup == 3 || it_select.levelup == 5 || it_select.levelup == 8 || it_select.levelup == 10) {
            int[] POSSIBLE_OPTION_IDS = { 1, 2, 10, 11, 12, 13, 14, 17, 49, 50, 51, 52, 53, 56, 63 };
            int optionId = POSSIBLE_OPTION_IDS[Util.random(POSSIBLE_OPTION_IDS.length)];
            int param = Util.random(50, 151) * multiplier + colorBonus;
            it_select.option_item.add(new Option(optionId, param));
        }
        p.item.update_Inventory(-1, false);
        p.data_yesno = null;
    }

    public static void sendChatKTG(Player p, Item_wear item, int totalUpgrades) throws IOException {
        if (item.levelup >= 8) {
            String msg = "Chúc mừng " + p.name + " đã cường hoá thành công "
                    + item.template.name + " lên cấp +" + item.levelup
                    + " với " + totalUpgrades + " lần nâng";
            Manager.gI().chatKTG(0, msg, 5);
        }
    }

    public static void sendChatKTG_KichAn(Player p, Item_wear item, int totalUpgrades) throws IOException {
        String msg = "Chúc mừng " + p.name + " đã kích ẩn thành công "
                + item.template.name + " với " + totalUpgrades + " lần kích ẩn";
        Manager.gI().chatKTG(0, msg, 5);
    }

    public static void sendChatKTG_HoanMy(Player p, Item_wear item, int totalUpgrades) throws IOException {
        String msg = "Chúc mừng " + p.name + " đã hoàn mỹ thành công "
                + item.template.name + " với " + totalUpgrades + " lần hoàn mỹ";
        Manager.gI().chatKTG(0, msg, 5);
    }

    public static void sendChatKTG_KichKhuon(Player p, Item_wear item) throws IOException {
        String msg = "Chúc mừng " + p.name + " đã kích khuôn thành công "
                + item.template.name + "!";
        Manager.gI().chatKTG(0, msg, 5);
    }

    private static int get_longvu(byte levelup) {
        return (levelup + 1) * 2;
    }

    private static int get_botCH(byte levelup) {
        return (levelup + 1) * 6;
    }

    private static int get_botvang(byte levelup) {
        return (levelup + 1) * 4;
    }

    private static int get_thienthach(byte levelup) {
        return (levelup + 1) * 5;
    }

    private static int get_mairua(byte levelup) {
        return (levelup + 1) * 5;
    }

    private static int get_khien(byte levelup) {
        return (levelup - 1) / 3 + 1;
    }

    private static int get_beri_up(byte levelup) {
        return (levelup + 1) * 30_000;
    }

    private static int get_extol_up(byte levelup) {
        return (levelup + 1) * 3_000;
    }

    private static int get_ruby_up(byte levelup) {
        return (levelup + 1) * 3_000;
    }

}
