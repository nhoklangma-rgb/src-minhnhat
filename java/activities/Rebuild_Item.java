package activities;

import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import template.*;

public class Rebuild_Item {
    public static short[] ID_SELL = new short[] { 46, 52, 58, 64, 70, 76 };
    public static byte[] PERCENT_HOP_NGOC = new byte[] { 120, 85, 70, 55, 40, 0 };
    public static int[] PRICE_THAO_NGOC = new int[] { 2, 6, 18, 54, 162, 300 };
    public static short[][] ITEM_NGOC_SIEU_CAP;
    public static final long COST_BERRY_KICH_KHUON = 300_000_000L;
    public static final int COST_RUBY_KICH_KHUON = 1_000_000;
    public static final int COST_EXTOL_KICH_KHUON = 100_000;
    static {
        short[] id_ = new short[] { 49, 55, 61, 67, 73, 79 };
        short a = 0, b = 1;
        Rebuild_Item.ITEM_NGOC_SIEU_CAP = new short[30][];
        for (int i = 241; i < 271; i++) {
            Rebuild_Item.ITEM_NGOC_SIEU_CAP[i - 241] = new short[] { (short) i, id_[a], id_[b] };
            if (b < (id_.length - 1)) {
                b++;
                if (b == a && b < (id_.length - 1)) {
                    b++;
                }
            } else if (a < (id_.length - 1)) {
                a++;
                b = 0;
            }
        }
    }

    public static void show_table(Player p, int type) throws IOException {
        Message m = new Message(-67);
        m.writer().writeByte(0);
        switch (type) {
            case 1: {
                m.writer().writeByte(4);
                break;
            }
            case 2: {
                m.writer().writeByte(19);
                break;
            }
            case 3: {
                m.writer().writeByte(1);
                break;
            }
            case 4: {
                m.writer().writeByte(3);
                break;
            }
            case 5: {
                m.writer().writeByte(13);
                break;
            }
            case 6: { //
                m.writer().writeByte(10);
                break;
            }
            case 7: { //
                m.writer().writeByte(11);
                break;
            }
            case 8: { //
                m.writer().writeByte(12);
                break;
            }
            case 9: { // ghep manh trang bi
                m.writer().writeByte(14);
                break;
            }
            case 10: { // duc lo dial
                m.writer().writeByte(19);
                break;
            }
            case 11: { // kich khuon
                java.util.List<Item_wear> list = new java.util.ArrayList<>();
                java.util.List<Integer> list_index = new java.util.ArrayList<>();
                for (int i = 0; i < p.item.bag3.length; i++) {
                    Item_wear it = p.item.bag3[i];
                    if (it != null && it.template.typeEquip <= 7
                            && it.numLoKham == 12) {
                        list.add(it);
                        list_index.add(i);
                    }
                }
                if (list.isEmpty()) {
                    Service.send_box_ThongBao_OK(p,
                            "Bạn không có trang bị nào đủ 12 lỗ để Kích Khuôn!\nHãy đục đủ 12 lỗ trước.");
                    return;
                }
                String[] name = new String[list.size()];
                short[] icon = new short[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    name[i] = list.get(i).template.name;
                    icon[i] = list.get(i).template.icon;
                }
                p.tempIndexMapping = list_index;
                core.MenuController.send_dynamic_menu(p, 9878, "Chọn trang bị Kích khuôn", name, icon, 7);
                return;
            }
        }
        p.addmsg(m);
        m.cleanup();
        p.item_to_kham_ngoc = null;
        p.item_to_kham_ngoc_id_ngoc = -1;
        p.data_yesno = null;
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        byte action = m2.reader().readByte();
        short idItem = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        short num = m2.reader().readShort();
        m2.reader().readShort();
        // System.out.println("type=" + type + ", action=" + action + ", idItem=" +
        // idItem
        // + ", cat=" + cat + ", num=" + num);
        if (cat == 4 && num > 0 && type == 4 && action == 1) { // bo da kham vao de hop
            if (num < 3) {
                Service.send_box_ThongBao_OK(p,
                        "Số lượng nhập vào của bạn không được nhỏ hơn 3 viên");
                return;
            }
            if (p.item.total_item_bag_by_id(4, idItem) < num) {
                Service.send_box_ThongBao_OK(p, "Không đủ vật phẩm trong hành trang");
                return;
            }
            if ((idItem >= 44 && idItem <= 78 && idItem != 73 && idItem != 67 && idItem != 61
                    && idItem != 55 && idItem != 49) || (idItem >= 221 && idItem <= 225)
                    || (idItem >= 362 && idItem <= 366)) {
                Message m = new Message(-67);
                m.writer().writeByte(1);
                m.writer().writeShort(idItem);
                m.writer().writeByte(4);
                m.writer().writeShort(num);
                p.addmsg(m);
                m.cleanup();
            } else {
                Service.send_box_ThongBao_OK(p, "Vật phẩm không hợp lệ");
            }
        } else if (cat == 4 && num > 0 && type == 4 && action == 5) { // hop da kham
            if ((idItem >= 44 && idItem <= 78 && idItem != 73 && idItem != 67 && idItem != 61
                    && idItem != 55 && idItem != 49) || (idItem >= 221 && idItem <= 225)
                    || (idItem >= 362 && idItem <= 366)) {
                if (p.item.total_item_bag_by_id(4, idItem) < num) {
                    Service.send_box_ThongBao_OK(p, "Không đủ vật phẩm trong hành trang!");
                    return;
                }
                int time_success = 0;
                int time_lose = 0;
                int percent = Rebuild_Item.PERCENT_HOP_NGOC[Rebuild_Item.get_percent_hop_ngoc(idItem)];
                while (num >= 3) {
                    if (percent > Util.random(120)) {
                        time_success++;
                    } else {
                        time_lose++;
                    }
                    num -= 3;
                }
                p.item.remove_item47(4, idItem, ((2 * time_lose) + (3 * time_success)));
                if (time_success > 0) {
                    p.item.add_item_bag47(4, (idItem + 1), time_success);
                }
                //
                p.item.update_Inventory(-1, false);
                Message m = new Message(-67);
                m.writer().writeByte(5);
                m.writer()
                        .writeUTF("Sử dụng " + (3 * (time_lose + time_success)) + " "
                                + ItemTemplate4.get_item_name(idItem) + " để nâng cấp. Thành công "
                                + time_success + " lần, thất bại " + time_lose + " lần.");
                m.writer().writeShort(idItem);
                m.writer().writeShort((idItem + 1));
                m.writer().writeShort(time_success);
                m.writer().writeByte(4);
                p.addmsg(m);
                m.cleanup();
            } else {
                Rebuild_Item.show_table(p, 1);
                Service.send_box_ThongBao_OK(p, "Vật phẩm không hợp lệ!");
            }
        } else if (cat == 3 && num == 1 && type == 1 && action == 1) { // bo item kham ngoc vao
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null && it_select.template.typeEquip <= 8) {
                Message m = new Message(-67);
                m.writer().writeByte(1);
                m.writer().writeShort(idItem);
                m.writer().writeByte(3);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
                p.item_to_kham_ngoc = it_select;
            } else {
                Service.send_box_ThongBao_OK(p, "Vật phẩm không hợp lệ!");
            }
        } else if (cat == 4 && num == 1 && type == 1 && action == 1) { // bo ngoc kham vao
            if (p.item.total_item_bag_by_id(4, idItem) < num) {
                Service.send_box_ThongBao_OK(p, "Không đủ vật phẩm trong hành trang");
                return;
            }
            Message m = new Message(-67);
            m.writer().writeByte(1);
            m.writer().writeShort(idItem);
            m.writer().writeByte(4);
            m.writer().writeShort(1);
            p.addmsg(m);
            m.cleanup();
            p.item_to_kham_ngoc_id_ngoc = idItem;
        } else if (cat == 0 && num == 0 && type == 1 && action == 4) { // bat dau kham ngoc len item
            Item_wear it_select = p.item_to_kham_ngoc;
            if (it_select != null && p.item_to_kham_ngoc_id_ngoc != -1) {
                ItemTemplate4 temp4 = ItemTemplate4.get_it_by_id(p.item_to_kham_ngoc_id_ngoc);
                if (it_select.numLoKham <= it_select.mdakham.length && temp4 != null) {
                    Rebuild_Item.show_table(p, 3);
                    Service.send_box_ThongBao_OK(p, "Vật phẩm này không có lỗ trống để khảm");
                    return;
                }
                if (p.item_to_kham_ngoc_id_ngoc >= 221 && p.item_to_kham_ngoc_id_ngoc <= 226) {
                    Rebuild_Item.show_table(p, 3);
                    Service.send_box_ThongBao_OK(p,
                            "Đá Hải Thạch không thể khảm lên trang bị");
                    return;
                }
                if (!check_can_kham_len_item(it_select, p.item_to_kham_ngoc_id_ngoc)) {
                    Rebuild_Item.show_table(p, 3);
                    Service.send_box_ThongBao_OK(p,
                            "Không thể khảm \n" + temp4.name + "\n lên loại trang bị này");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, p.item_to_kham_ngoc_id_ngoc) < 1) {
                    Rebuild_Item.show_table(p, 3);
                    Service.send_box_ThongBao_OK(p, "Không đủ vật phẩm trong hành trang");
                    return;
                }
                p.item.remove_item47(4, p.item_to_kham_ngoc_id_ngoc, 1);
                //
                add_op_ngoc_kham_new(p, it_select, p.item_to_kham_ngoc_id_ngoc);
                //
                p.item.update_Inventory(-1, false);
                Service.Main_char_Info(p);
                Message m = new Message(-67);
                m.writer().writeByte(4);
                m.writer()
                        .writeUTF("Khảm thành công \n"
                                + ItemTemplate4.get_it_by_id(p.item_to_kham_ngoc_id_ngoc).name
                                + "\n lên " + it_select.template.name);
                p.addmsg(m);
                m.cleanup();
            } else {
                Rebuild_Item.show_table(p, 3);
                Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, hãy thử lại!");
            }
        } else if (cat == 3 && num == 1 && type == 3 && action == 1) { // bo item thao ngoc kham
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null) {
                if (it_select.mdakham.length > 0) {
                    Message m = new Message(-67);
                    m.writer().writeByte(1);
                    m.writer().writeShort(idItem);
                    m.writer().writeByte(3);
                    m.writer().writeShort(1);
                    p.addmsg(m);
                    m.cleanup();
                    p.item_to_kham_ngoc = it_select;
                } else {
                    Service.send_box_ThongBao_OK(p, "Vật phẩm chưa có đá khảm!");
                }
            }
        } else if (cat == 3 && num == 1 && type == 3 && action == 6) { // bat dau thao ngoc kham
            if (p.item_to_kham_ngoc != null) {
                int vang_req = 0;
                for (int i = 0; i < p.item_to_kham_ngoc.mdakham.length; i++) {
                    if (p.item_to_kham_ngoc.mdakham[i] >= 44
                            && p.item_to_kham_ngoc.mdakham[i] <= 79) {
                        vang_req += Rebuild_Item.PRICE_THAO_NGOC[Rebuild_Item
                                .get_percent_hop_ngoc(p.item_to_kham_ngoc.mdakham[i])];
                    } else if (p.item_to_kham_ngoc.mdakham[i] >= 241
                            && p.item_to_kham_ngoc.mdakham[i] <= 270
                            || p.item_to_kham_ngoc.mdakham[i] >= 362
                                    && p.item_to_kham_ngoc.mdakham[i] <= 373) {
                        vang_req += 3000;
                    } else {
                        vang_req += 3500;
                    }
                }
                if (vang_req > 0) {
                    Service.send_box_yesno(p, 1, "Thông báo",
                            "Xác nhận tháo tất cả ngọc khảm với giá " + Util.number_format(vang_req) + " Ruby?",
                            new String[] { "Có", "Không" }, new byte[] { -1, -1 });
                }
            }
        } else if (cat == 4 && num == 0 && type == 13 && (action == 28 || action == 29)) { // bo
                                                                                           // item
                                                                                           // da
                                                                                           // sieu
                                                                                           // cap
            if (!Rebuild_Item.check_it_can_upgrade_da_sieu_cap(idItem)) {
                Rebuild_Item.show_table(p, 5);
                Service.send_box_ThongBao_OK(p, "Vật phẩm không hợp lệ!");
                p.data_yesno = null;
                return;
            }
            if (idItem == 367 && action == 29) {
                Rebuild_Item.show_table(p, 5);
                Service.send_box_ThongBao_OK(p,
                        "Không thể sử dụng hổ phách để làm đá nguyên liệu!");
                p.data_yesno = null;
                return;
            }
            if (p.item.total_item_bag_by_id(4, idItem) < (action == 28 ? 1 : 2)) {
                Rebuild_Item.show_table(p, 5);
                Service.send_box_ThongBao_OK(p, "Không đủ " + (action == 28 ? 1 : 2) + " "
                        + ItemTemplate4.get_item_name(idItem));
                p.data_yesno = null;
                return;
            }
            if (p.data_yesno == null || p.data_yesno.length != 2) {
                p.data_yesno = new int[] { -1, -1 };
            }
            if (action == 28) {
                p.data_yesno[0] = idItem;
            } else if (action == 29) {
                p.data_yesno[1] = idItem;
            }
            int num_material = action == 28 ? 1 : 2;
            Message m = new Message(-67);
            m.writer().writeByte(action);
            m.writer().writeShort(idItem);
            m.writer().writeByte(4);
            m.writer().writeShort(num_material);
            m.writer().writeByte(p.percent_da_sieu_cap);
            p.addmsg(m);
            m.cleanup();
        } else if (idItem == 0 & cat == 0 && num == 0 && type == 13 && action == 26
                && p.data_yesno != null && p.data_yesno.length == 2) { // bat dau tao thanh da sieu
                                                                       // cap
            Service.send_box_yesno(p, 13, "Thông báo",
                    "Bạn sẽ mất 1 đá nguyên liệu nếu thất bại. Bạn có muốn tiếp " + "tục nâng cấp?",
                    new String[] { "Có", "Không" }, new byte[] { 2, 1 });
        } else if (type == 10 && action == 1 && cat == 3 && num == 1) { // bo item vao de hoan my / kich khuon
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null && it_select.template.typeEquip <= 7) {
                Message m = new Message(-67);
                m.writer().writeByte(1);
                m.writer().writeShort(idItem);
                m.writer().writeByte(3);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
                p.item_to_kham_ngoc = it_select;

                // Hoan my requires Da Hai Thach 6
                if (p.item.total_item_bag_by_id(4, 226) >= 1) {
                    m = new Message(-67);
                    m.writer().writeByte(1);
                    m.writer().writeShort(226);
                    m.writer().writeByte(4);
                    m.writer().writeShort(1);
                    p.addmsg(m);
                    m.cleanup();
                }
            }
        } else if (type == 10 && action == 20 && cat == 0 && idItem == 0 && num == 0) { // hoan my
            if (p.item_to_kham_ngoc != null) {
                Item_wear it_select = p.item_to_kham_ngoc;
                Service.send_box_yesno(p, 30, "Thông báo",
                        "Bạn có muốn thực hiện \nHoàn mỹ " + it_select.template.name,
                        new String[] { "5.000", "Auto", "Đóng" }, new byte[] { 69, 3, -1 });
            }
        } else if (type == 11 && action == 1 && cat == 3 && num == 1) { // bo item kich an
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null && it_select.template.typeEquip <= 7) {
                Message m = new Message(-67);
                m.writer().writeByte(1);
                m.writer().writeShort(idItem);
                m.writer().writeByte(3);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
                p.item_to_kham_ngoc = it_select;

                if (p.item.total_item_bag_by_id(4, 226) >= 1) {
                    m = new Message(-67);
                    m.writer().writeByte(1);
                    m.writer().writeShort(226);
                    m.writer().writeByte(4);
                    m.writer().writeShort(1);
                    p.addmsg(m);
                    m.cleanup();
                }
            }
        } else if (type == 11 && action == 22 && cat == 0 && idItem == 0 && num == 0) { // kich an
            if (p.item_to_kham_ngoc != null) {
                Item_wear it_select = p.item_to_kham_ngoc;
                Service.send_box_yesno(p, 32, "Thông báo",
                        "Bạn có muốn thực hiện \nKích ẩn " + it_select.template.name,
                        new String[] { "5.000", "Auto", "Đóng" }, new byte[] { 69, 3, -1 });
            }
        } else if (type == 14 && action == 1 && cat == 4 && idItem >= 0 && num == 1) { // bo manh
                                                                                       // trang bi
                                                                                       // vao
            if (p.item.total_item_bag_by_id(cat, idItem) < num) {
                Service.send_box_ThongBao_OK(p, "Không đủ trong hành trang");
                return;
            }
            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(idItem);
            if (itemTemplate4 != null) {
                if (itemTemplate4.type == 74) {
                    Message m = new Message(-67);
                    m.writer().writeByte(1);
                    m.writer().writeShort(idItem);
                    m.writer().writeByte(4);
                    m.writer().writeShort(1);
                    p.addmsg(m);
                    m.cleanup();
                    p.item_to_kham_ngoc_id_ngoc = idItem;
                } else {
                    Service.send_box_ThongBao_OK(p, "Chỉ có thể bỏ mảnh trang bị vào");
                }
            }
        } else if (type == 14 && action == 31 && cat == 0 && idItem == 0 && num == 0) { // bat dau
                                                                                        // ghep
                                                                                        // trang bi
            if (p.item.total_item_bag_by_id(cat, p.item_to_kham_ngoc_id_ngoc) < num) {
                Service.send_box_ThongBao_OK(p, "Không đủ trong hành trang");
                return;
            }
            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(p.item_to_kham_ngoc_id_ngoc);
            if (itemTemplate4 != null) {
                if (itemTemplate4.type == 74) {
                    p.data_yesno = new int[] { itemTemplate4.id };
                    switch (itemTemplate4.id) {
                        case 304:
                        case 305:
                        case 306: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị trắng 9x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 307:
                        case 308:
                        case 309: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị xanh 9x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 310:
                        case 311:
                        case 312: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị tím 9x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 313:
                        case 314:
                        case 315: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị cam 9x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 536:
                        case 537:
                        case 538: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị trắng 10x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 539:
                        case 540:
                        case 541: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị xanh 10x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 542:
                        case 543:
                        case 544: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị tím 10x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        case 545:
                        case 546:
                        case 547: {
                            Service.send_box_yesno(p, 40, "Thông báo",
                                    "Bạn có muốn ghép 18 " + itemTemplate4.name
                                            + " thành 1 trang bị cam 10x?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                            break;
                        }
                        default: {
                            p.data_yesno = null;
                            Service.send_box_ThongBao_OK(p,
                                    "Mảnh trang bị loại này hiện tại chưa ghép được");
                            break;
                        }
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Chỉ có thể bỏ mảnh trang bị vào");
                }
            }
        } else if (cat == 3 && num == 1 && type == 19 && action == 1) { // bo item vao de duc lo
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null) {
                Message m = new Message(-67);
                m.writer().writeByte(1);
                m.writer().writeShort(idItem);
                m.writer().writeByte(3);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
            }
        } else if (cat == 3 && num == 1 && type == 19 && action == 7) { // bat dau duc lo
            Item_wear it_select = p.item.bag3[idItem];
            if (it_select != null) {
                if (p.item.total_item_bag_by_id(4, 457) < 1) {
                    Service.send_box_ThongBao_OK(p, "Bạn không có Búa Đục Lỗ");
                    return;
                }
                if (it_select.numLoKham >= 12) {
                    Service.send_box_ThongBao_OK(p, "Đã đạt tối đa 12 lỗ khảm");
                    return;
                }
                int beri_req = 5 * (it_select.numLoKham + 1);
                p.data_yesno = new int[] { idItem };
                Service.send_box_yesno(p, 57, "Thông báo",
                        "Bạn có muốn thực hiện \nĐục lỗ " + it_select.template.name,
                        new String[] { beri_req + "M Beri", "Auto", "Đóng" }, new byte[] { 6, 3, -1 });
            }
        } else if (type == 10 && action == 10 && cat == 0 && idItem == 0 && num == 0) { // bat dau kich khuon
            if (p.item_to_kham_ngoc != null) {
                Item_wear it_select = p.item_to_kham_ngoc;
                Service.send_box_yesno(p, 9877, "Thông báo",
                        "Xác nhận Kích khuôn " + it_select.template.name
                                + "?\nChi phí: " + Util.number_format(COST_BERRY_KICH_KHUON) + " Beri, "
                                + Util.number_format(COST_RUBY_KICH_KHUON) + " Ruby, "
                                + Util.number_format(COST_EXTOL_KICH_KHUON) + " Extol",
                        new String[] { "Có", "Không" }, new byte[] { -1, -1 });
            }
        }
    }

    private static boolean check_it_can_upgrade_da_sieu_cap(short idItem) {
        return Rebuild_Item.get_percent_hop_ngoc(idItem) == 5;
    }

    public static int get_percent_hop_ngoc(short idItem) {
        if (idItem == 221) {
            return 0;
        } else if (idItem == 222) {
            return 1;
        } else if (idItem == 223) {
            return 2;
        } else if (idItem == 224 || idItem == 362) {
            return 3;
        } else if (idItem == 225 || idItem == 363) {
            return 4;
        } else if (idItem == 226) {
            return 6; // 5
        } else {
            int index = idItem - 44;
            while (index >= 6) {
                index -= 6;
            }
            return index;
        }
    }

    private static void add_op_ngoc_kham_new(Player p, Item_wear it_select, short id) throws IOException {
        if (it_select.mdakham.length > 0) {
            short[] temp = new short[it_select.mdakham.length + 1];
            for (int i = 0; i < it_select.mdakham.length; i++) {
                temp[i] = it_select.mdakham[i];
            }
            temp[temp.length - 1] = id;
            it_select.mdakham = temp;
        } else {
            it_select.mdakham = new short[] { id };
        }
        int[] id_add = null;
        int[] par_add = null;
        if (devilfruit.DevilFruitManager.isDevilFruit(id)) {
            id_add = devilfruit.DevilFruitManager.getOptionIds(id);
            par_add = devilfruit.DevilFruitManager.getOptionValues(id);
        } else {
            switch (id) {
                case 44: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 20 };
                    break;
                }
                case 45: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 30 };
                    break;
                }
                case 46: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 40 };
                    break;
                }
                case 47: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 60 };
                    break;
                }
                case 48: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 90 };
                    break;
                }
                case 49: {
                    id_add = new int[] { 4 };
                    par_add = new int[] { 140 };
                    break;
                }
                case 50: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 50 };
                    break;
                }
                case 51: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 70 };
                    break;
                }
                case 52: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 90 };
                    break;
                }
                case 53: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 120 };
                    break;
                }
                case 54: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 160 };
                    break;
                }
                case 55: {
                    id_add = new int[] { 1 };
                    par_add = new int[] { 220 };
                    break;
                }
                case 56: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 10 };
                    break;
                }
                case 57: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 20 };
                    break;
                }
                case 58: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 30 };
                    break;
                }
                case 59: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 40 };
                    break;
                }
                case 60: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 50 };
                    break;
                }
                case 61: {
                    id_add = new int[] { 10 };
                    par_add = new int[] { 60 };
                    break;
                }
                case 62: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 10 };
                    break;
                }
                case 63: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 20 };
                    break;
                }
                case 64: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 30 };
                    break;
                }
                case 65: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 40 };
                    break;
                }
                case 66: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 60 };
                    break;
                }
                case 67: {
                    id_add = new int[] { 13 };
                    par_add = new int[] { 90 };
                    break;
                }
                case 68: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 10, 10 };
                    break;
                }
                case 69: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 20, 20 };
                    break;
                }
                case 70: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 30, 30 };
                    break;
                }
                case 71: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 40, 40 };
                    break;
                }
                case 72: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 60, 60 };
                    break;
                }
                case 73: {
                    id_add = new int[] { 26, 27 };
                    par_add = new int[] { 90, 90 };
                    break;
                }
                case 74: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 10 };
                    break;
                }
                case 75: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 20 };
                    break;
                }
                case 76: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 30 };
                    break;
                }
                case 77: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 40 };
                    break;
                }
                case 78: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 60 };
                    break;
                }
                case 79: {
                    id_add = new int[] { 14 };
                    par_add = new int[] { 90 };
                    break;
                }
                case 241: {
                    id_add = new int[] { 4, 48 };
                    par_add = new int[] { 140, 20 };
                    break;
                }
                case 242: {
                    id_add = new int[] { 4, 49 };
                    par_add = new int[] { 140, 40 };
                    break;
                }
                case 243: {
                    id_add = new int[] { 4, 50 };
                    par_add = new int[] { 140, 40 };
                    break;
                }
                case 244: {
                    id_add = new int[] { 4, 51 };
                    par_add = new int[] { 140, 40 };
                    break;
                }
                case 245: {
                    id_add = new int[] { 4, 52 };
                    par_add = new int[] { 140, 40 };
                    break;
                }
                case 246: {
                    id_add = new int[] { 1, 47 };
                    par_add = new int[] { 220, 40 };
                    break;
                }
                case 247: {
                    id_add = new int[] { 1, 49 };
                    par_add = new int[] { 220, 40 };
                    break;
                }
                case 248: {
                    id_add = new int[] { 1, 50 };
                    par_add = new int[] { 220, 40 };
                    break;
                }
                case 249: {
                    id_add = new int[] { 1, 51 };
                    par_add = new int[] { 220, 40 };
                    break;
                }
                case 250: {
                    id_add = new int[] { 1, 52 };
                    par_add = new int[] { 220, 40 };
                    break;
                }
                case 251: {
                    id_add = new int[] { 10, 47 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 252: {
                    id_add = new int[] { 10, 48 };
                    par_add = new int[] { 60, 20 };
                    break;
                }
                case 253: {
                    id_add = new int[] { 10, 50 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 254: {
                    id_add = new int[] { 10, 51 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 255: {
                    id_add = new int[] { 10, 52 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 256: {
                    id_add = new int[] { 13, 47 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 257: {
                    id_add = new int[] { 13, 48 };
                    par_add = new int[] { 90, 20 };
                    break;
                }
                case 258: {
                    id_add = new int[] { 13, 49 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 259: {
                    id_add = new int[] { 13, 51 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 260: {
                    id_add = new int[] { 13, 52 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 261: {
                    id_add = new int[] { 26, 27, 47 };
                    par_add = new int[] { 90, 90, 40 };
                    break;
                }
                case 262: {
                    id_add = new int[] { 26, 27, 48 };
                    par_add = new int[] { 90, 90, 20 };
                    break;
                }
                case 263: {
                    id_add = new int[] { 26, 27, 49 };
                    par_add = new int[] { 90, 90, 40 };
                    break;
                }
                case 264: {
                    id_add = new int[] { 26, 27, 50 };
                    par_add = new int[] { 90, 90, 40 };
                    break;
                }
                case 265: {
                    id_add = new int[] { 26, 27, 52 };
                    par_add = new int[] { 90, 90, 40 };
                    break;
                }
                case 266: {
                    id_add = new int[] { 14, 47 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 267: {
                    id_add = new int[] { 14, 48 };
                    par_add = new int[] { 90, 20 };
                    break;
                }
                case 268: {
                    id_add = new int[] { 14, 49 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 269: {
                    id_add = new int[] { 14, 50 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 270: {
                    id_add = new int[] { 14, 51 };
                    par_add = new int[] { 90, 40 };
                    break;
                }
                case 362: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 10 };
                    break;
                }
                case 363: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 20 };
                    break;
                }
                case 364: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 30 };
                    break;
                }
                case 365: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 40 };
                    break;
                }
                case 366: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 50 };
                    break;
                }
                case 367: {
                    id_add = new int[] { 12 };
                    par_add = new int[] { 60 };
                    break;
                }
                case 368: {
                    id_add = new int[] { 12, 47 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 369: {
                    id_add = new int[] { 12, 48 };
                    par_add = new int[] { 60, 20 };
                    break;
                }
                case 370: {
                    id_add = new int[] { 12, 49 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 371: {
                    id_add = new int[] { 12, 50 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 372: {
                    id_add = new int[] { 12, 51 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 373: {
                    id_add = new int[] { 12, 52 };
                    par_add = new int[] { 60, 40 };
                    break;
                }
                case 324: {
                    id_add = new int[] { 1, 4, 10, 13, 14, 26, 27 };
                    par_add = new int[] { 100, 100, 50, 50, 50, 50, 50 };
                    break;
                }
                case 325: {
                    id_add = new int[] { 49, 50, 52, 51, 63 };
                    par_add = new int[] { 100, 100, 100, 100, 100 };
                    break;
                }
                case 326: {
                    id_add = new int[] { 1, 10, 13, 57, 48 };
                    par_add = new int[] { 200, 100, 100, 100, 100 };
                    break;
                }
                //
                case 647: {
                    id_add = new int[] { 4, 48 };
                    par_add = new int[] { 250, 30 };
                    break;
                }
                case 648: {
                    id_add = new int[] { 4, 49 };
                    par_add = new int[] { 250, 60 };
                    break;
                }
                case 649: {
                    id_add = new int[] { 4, 50 };
                    par_add = new int[] { 250, 60 };
                    break;
                }
                case 650: {
                    id_add = new int[] { 4, 51 };
                    par_add = new int[] { 250, 60 };
                    break;
                }
                case 651: {
                    id_add = new int[] { 4, 52 };
                    par_add = new int[] { 250, 60 };
                    break;
                }
                case 652: {
                    id_add = new int[] { 1, 47 };
                    par_add = new int[] { 320, 60 };
                    break;
                }
                case 653: {
                    id_add = new int[] { 1, 49 };
                    par_add = new int[] { 320, 60 };
                    break;
                }
                case 654: {
                    id_add = new int[] { 1, 50 };
                    par_add = new int[] { 320, 60 };
                    break;
                }
                case 655: {
                    id_add = new int[] { 1, 51 };
                    par_add = new int[] { 320, 60 };
                    break;
                }
                case 656: {
                    id_add = new int[] { 1, 52 };
                    par_add = new int[] { 320, 60 };
                    break;
                }
                case 657: {
                    id_add = new int[] { 10, 47 };
                    par_add = new int[] { 100, 60 };
                    break;
                }
                case 658: {
                    id_add = new int[] { 10, 48 };
                    par_add = new int[] { 100, 30 };
                    break;
                }
                case 659: {
                    id_add = new int[] { 10, 50 };
                    par_add = new int[] { 100, 60 };
                    break;
                }
                case 660: {
                    id_add = new int[] { 10, 51 };
                    par_add = new int[] { 100, 60 };
                    break;
                }
                case 661: {
                    id_add = new int[] { 10, 52 };
                    par_add = new int[] { 100, 60 };
                    break;
                }
                case 662: {
                    id_add = new int[] { 13, 47 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 663: {
                    id_add = new int[] { 13, 48 };
                    par_add = new int[] { 150, 30 };
                    break;
                }
                case 664: {
                    id_add = new int[] { 13, 49 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 665: {
                    id_add = new int[] { 13, 51 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 666: {
                    id_add = new int[] { 13, 52 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 667: {
                    id_add = new int[] { 26, 27, 47 };
                    par_add = new int[] { 150, 150, 60 };
                    break;
                }
                case 668: {
                    id_add = new int[] { 26, 27, 48 };
                    par_add = new int[] { 150, 150, 30 };
                    break;
                }
                case 669: {
                    id_add = new int[] { 26, 27, 49 };
                    par_add = new int[] { 150, 150, 60 };
                    break;
                }
                case 670: {
                    id_add = new int[] { 26, 27, 50 };
                    par_add = new int[] { 150, 150, 60 };
                    break;
                }
                case 671: {
                    id_add = new int[] { 26, 27, 52 };
                    par_add = new int[] { 150, 150, 60 };
                    break;
                }
                case 672: {
                    id_add = new int[] { 14, 47 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 673: {
                    id_add = new int[] { 14, 48 };
                    par_add = new int[] { 150, 30 };
                    break;
                }
                case 674: {
                    id_add = new int[] { 14, 49 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 675: {
                    id_add = new int[] { 14, 50 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 676: {
                    id_add = new int[] { 14, 51 };
                    par_add = new int[] { 150, 60 };
                    break;
                }
                case 677: {
                    id_add = new int[] { 12, 47 };
                    par_add = new int[] { 90, 60 };
                    break;
                }
                case 678: {
                    id_add = new int[] { 12, 48 };
                    par_add = new int[] { 90, 30 };
                    break;
                }
                case 679: {
                    id_add = new int[] { 12, 49 };
                    par_add = new int[] { 90, 60 };
                    break;
                }
                case 680: {
                    id_add = new int[] { 12, 50 };
                    par_add = new int[] { 90, 60 };
                    break;
                }
                case 681: {
                    id_add = new int[] { 12, 51 };
                    par_add = new int[] { 90, 60 };
                    break;
                }
                case 682: {
                    id_add = new int[] { 12, 52 };
                    par_add = new int[] { 90, 60 };
                    break;
                }
                case 1032: {
                    id_add = new int[] { 26 };
                    par_add = new int[] { 150 };
                    break;
                }
            }
        }
        if (id_add != null && par_add != null) {
            for (int i = 0; i < id_add.length; i++) {
                Option op_new = null;
                for (int j = 0; j < it_select.option_item_2.size(); j++) {
                    if (it_select.option_item_2.get(j).id == id_add[i]) {
                        op_new = it_select.option_item_2.get(j);
                        break;
                    }
                }
                if (op_new != null) {
                    int par_old = op_new.getParam();
                    op_new.setParam(par_old + par_add[i]);
                } else {
                    op_new = new Option(id_add[i], par_add[i]);
                    it_select.option_item_2.add(op_new);
                }
            }
        }
        p.update_info_to_all();
    }

    private static boolean check_can_kham_len_item(Item_wear it_select, int id) {
        if (devilfruit.DevilFruitManager.isDevilFruit((short) id)) {
            return devilfruit.DevilFruitManager.canSocketToItem(it_select, (short) id);
        }
        if (it_select.template.typeEquip == 7 || (id >= 324 && id <= 326) || it_select.template.tb2 == 2) {
            return true;
        }
        boolean result = false;
        switch (it_select.template.typeEquip) {
            case 0: {
                if (id >= 50 && id <= 55 || id >= 246 && id <= 250 || id >= 652 && id <= 656) {
                    result = true;
                }
                break;
            }
            case 1:
            case 3:
            case 5: {
                if (id >= 68 && id <= 73 || id >= 44 && id <= 49 || id >= 241 && id <= 245
                        || id >= 261 && id <= 265 || id >= 362 && id <= 373
                        || id >= 647 && id <= 651 || id >= 667 && id <= 671
                        || id >= 677 && id <= 682) {
                    result = true;
                }
                break;
            }
            case 2:
            case 4: {
                if (id >= 74 && id <= 79 || id >= 56 && id <= 67 || id >= 266 && id <= 270
                        || id >= 251 && id <= 260 || id >= 672 && id <= 676
                        || id >= 657 && id <= 666) {
                    result = true;
                }
                break;
            }
        }
        return result;
    }

    public static short get_id_ngoc_sieu_cap(int id1, int id2) {
        for (int i = 0; i < Rebuild_Item.ITEM_NGOC_SIEU_CAP.length; i++) {
            if (Rebuild_Item.ITEM_NGOC_SIEU_CAP[i][1] == id1
                    && Rebuild_Item.ITEM_NGOC_SIEU_CAP[i][2] == id2) {
                return Rebuild_Item.ITEM_NGOC_SIEU_CAP[i][0];
            }
        }
        return 2;
    }
}
