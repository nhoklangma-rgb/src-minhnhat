package client;

import core.Manager;
import core.MenuController;
import core.Service;
import core.Util;
import core.GameLogger;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import map.Map;
import map.Vgo;
import map.f;
import tool.ItemDropTool;
import activities.Mastery2;
import template.*;

public class UseItem {

    public static void process(Player p, Message m2) throws IOException {
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        switch (cat) {
            case 3: {
                use_item_3(p, id);
                break;
            }
            case 7: {
                use_item_7(p, id);
                break;
            }
            case 105: {
                ItemFashionP2 temp = p.check_fashion(id);
                if (temp != null) {
                    if (temp.is_use) {
                        temp.is_use = false;
                        if (temp.id == 122) {
                            p.tocSuper = 0;
                        }
                        p.update_info_to_all();
                        if (p.map != null) {
                            for (int i = 0; i < p.map.players.size(); i++) {
                                Player p0 = p.map.players.get(i);
                                Service.charWearing(p, p0, false);
                            }
                        }
                        p.body.setDirty();
                        Service.Main_char_Info(p);
                        p.item.update_Inventory(-1, false);
                        ItemFashionP.show_table(p, 105);
                        Service.send_box_ThongBao_OK(p,
                                "Tháo thành công " + ItemFashion.get_item(temp.id).name);
                        return;
                    }
                    if (p.myDisciple != null && p.myDisciple.status != 2) {
                        p.data_yesno = new int[] { id, 105 };
                        Service.Main_char_Info(p);
                        p.item.update_Inventory(-1, false);
                        MenuController.send_dynamic_menu(p, 8889, "Sử dụng cho ai?",
                                new String[] { "Sư phụ (Bản thân)", "Đệ tử" }, null);
                        return;
                    }
                    p.data_yesno = new int[] { id };
                    Service.Main_char_Info(p);
                    p.item.update_Inventory(-1, false);
                    Service.send_box_yesno(
                            p, 81, "", ItemFashion.get_item(temp.id).name
                                    + "\n Bạn muốn mặc hay đổi chỉ số ?",
                            new String[] { "Mặc", "Đổi CS", "Đóng" },
                            new byte[] { -1, -1, -1 });
                } else {
                    Service.send_box_ThongBao_OK(p, "Chưa mua vật phẩm này!");
                }
                break;
            }
            case 102: {
                ItemBoatP temp = p.check_itboat(id);
                if (temp != null) {
                    temp.is_use = true;
                    p.update_new_part_boat(temp);
                    ItemBoat.update_part_boat_when_shopping(p);
                    ItemFashionP.show_table(p, 102);
                    Service.send_box_ThongBao_OK(p, "Sử dụng " + ItemBoat.get_item(temp.id).name);
                } else {
                    Service.send_box_ThongBao_OK(p, "Chưa mua vật phẩm này!");
                }
                break;
            }
            case 108:
            case 103: {
                ItemFashionP temp = p.check_itfashionP(id, cat);
                if (temp != null) {
                    if (temp.is_use) {
                        temp.is_use = false;
                        p.update_info_to_all();
                        if (p.map != null) {
                            for (int i = 0; i < p.map.players.size(); i++) {
                                Player p0 = p.map.players.get(i);
                                Service.charWearing(p, p0, false);
                            }
                        }
                        Service.Main_char_Info(p);
                        p.item.update_Inventory(-1, false);
                        ItemFashionP.show_table(p, cat);
                        Service.send_box_ThongBao_OK(p,
                                "Tháo thành công " + template.ItemHair.get_item(temp.id, cat).name);
                        return;
                    }
                    if (p.myDisciple != null && p.myDisciple.status != 2) {
                        p.data_yesno = new int[] { id, cat };
                        MenuController.send_dynamic_menu(p, 8889, "Sử dụng cho ai?",
                                new String[] { "Sư phụ (Bản thân)", "Đệ tử" }, null);
                        return;
                    }
                    p.update_itfashionP(temp, cat);
                    for (int i = 0; i < p.map.players.size(); i++) {
                        Player p0 = p.map.players.get(i);
                        Service.charWearing(p, p0, false);
                    }
                    Service.UpdateInfoMaincharInfo(p);
                    ItemFashionP.show_table(p, cat);
                    Service.send_box_ThongBao_OK(p,
                            "Sử dụng " + template.ItemHair.get_item(temp.id, cat).name);
                } else {
                    Service.send_box_ThongBao_OK(p, "Chưa mua vật phẩm này!");
                }
                break;
            }
        }
    }

    public static void use_item_7(Player p, int id) throws IOException {
        if (id == 12) {
            List<Item_wear> list = new ArrayList<>();
            List<Integer> list_index = new ArrayList<>();
            for (int i = 0; i < p.item.bag3.length; i++) {
                Item_wear it = p.item.bag3[i];
                if (it != null && it.template != null && it.template.typeEquip <= 7) {
                    list.add(it);
                    list_index.add(i);
                }
            }
            if (list.isEmpty()) {
                Service.send_box_ThongBao_OK(p, "Bạn không có trang bị nào để nâng cấp!");
                return;
            }
            String[] name = new String[list.size()];
            short[] icon = new short[list.size()];
            for (int i = 0; i < list.size(); i++) {
                name[i] = list.get(i).template.name + " (+" + list.get(i).levelup + ")";
                icon[i] = list.get(i).template.icon;
            }
            p.tempIndexMapping = list_index;
            MenuController.send_dynamic_menu(p, 8888, "Chọn trang bị cần nâng cấp", name, icon, 3);
        }
    }

    public static boolean use_item_4(Player p, int id) throws IOException {
        boolean used = true;
        ItemTemplate4 it_temp = ItemTemplate4.get_it_by_id(id);
        if (it_temp != null) {
            if (it_temp.type == 1 || it_temp.type == 2) { // item hp mp
                if (it_temp.type == 1 && p.type_pk != 0) {
                    EffTemplate eff = p.get_eff(0);
                    if (eff == null || (eff.time - System.currentTimeMillis()) < 1_000) {
                        long par = it_temp.value;
                        if (it_temp.id == 173) { // com hop hai tac
                            par = p.body.get_hp_max(true) / 20;
                        }
                        par = (par * (100 + p.body.get_hp_potion_use_percent(true) / 10)) / 100;
                        if (par < 0) {
                            par = 0;
                        }
                        if (it_temp.id == 173 && par > 100_000) { // com hop hai tac
                            par = 100_000;
                        }
                        p.add_new_eff(0, (int) par, it_temp.timedelay);
                    }
                } else if (it_temp.type == 2 && p.type_pk != 0) {
                    EffTemplate eff = p.get_eff(1);
                    if (eff == null || (eff.time - System.currentTimeMillis()) < 1_000) {
                        long par = it_temp.value;
                        par = (par * (100 + p.body.get_mp_potion_use_percent(true) / 10)) / 100;
                        p.add_new_eff(1, f.setInteger(par), it_temp.timedelay);
                    }
                }
            } else {
                switch (id) {
                    case 89: {
                        if (p.map.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
                            Service.send_box_ThongBao_OK(p, "Vé Hồi Sinh không có tác dụng tại đây!");
                            return false;
                        }
                        Service.send_box_ThongBao_OK(p,
                                "Vật phẩm sẽ tự dùng khi chết. Vui lòng vào Cài đặt -> Tự động đánh -> Bật tự dùng hồi sinh để tự động hồi sinh khi chết");
                        used = false;
                        break;
                    }
                    case 194: { // Chim Phương Nam
                        Service.send_box_yesno(p, 194, "Thông báo",
                                "Bạn có muốn sử dụng Chim Phương Nam để chỉ hướng không?",
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                        return false;
                    }
                    case 196: { // Cánh hoa phượng
                        Vgo vgo = new Vgo();
                        vgo.map_go = Map.get_map_by_id(BossEvent.BossEvent.BOSS_EVENT_MAP_ID);
                        vgo.xnew = 350;
                        vgo.ynew = 250;
                        p.goto_map(vgo);
                        break;
                    }
                    case 1047: {
                        if (p.level < 80) {
                            p.level = 80;
                            p.exp = Level.ENTRYS[p.level - 1].exp - 1;
                            p.update_exp(1, false);
                            p.reset_point(0);
                            Service.send_box_ThongBao_OK(p, "Thành công tiến hoá lên level 80");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Dành cho cấp độ dưới level 80");
                            return false;
                        }
                        break;
                    }

                    case 1048: {
                        if (p.level < 30) {
                            Service.send_box_ThongBao_OK(p, "Cần đạt level 30 để tiến hoá kỹ năng");
                            return false;
                        }
                        int countUpgraded = 0;
                        for (int i = 0; i < p.skill_point.size(); i++) {
                            Skill_info skill = p.skill_point.get(i);
                            if (skill.temp.ID >= 0 && skill.temp.ID <= 2 && skill.exp > -1) {
                                while (skill.temp.Lv_RQ < 30) {
                                    long expReq = Skill_info.EXP[skill.temp.Lv_RQ - 1] - skill.exp;
                                    p.update_skill_exp(skill.temp.ID, expReq);
                                    countUpgraded++;
                                }
                            }
                        }
                        if (countUpgraded > 0) {
                            Service.send_box_ThongBao_OK(p, "Thành công tiến hoá kỹ năng lên cấp 30");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Kỹ năng đã đạt cấp tối đa");
                            return false;
                        }
                        break;
                    }

                    case 414: {
                        int countUpgraded = 0;
                        for (int i = 0; i < p.skill_point.size(); i++) {
                            Skill_info skill = p.skill_point.get(i);
                            if (skill.exp > -1) {
                                if (skill.temp.ID >= 0 && skill.temp.ID <= 2) {
                                    if (skill.temp.Lv_RQ < 1000) {
                                        long expReq = Skill_info.EXP[skill.temp.Lv_RQ] - skill.exp;
                                        if (expReq <= 0) { // ✅ guard: không cho cộng số âm
                                            skill.exp = 0; // reset về 0 cho an toàn
                                            continue;
                                        }
                                        p.update_skill_exp(skill.temp.ID, expReq);
                                        countUpgraded++;
                                    }
                                } else if (skill.temp.ID >= 2003 && skill.temp.ID <= 2063) {
                                    if (skill.temp.Lv_RQ < 100) {
                                        long expReq = Skill_info.EXP[skill.temp.Lv_RQ] - skill.exp;
                                        if (expReq <= 0) { // ✅ guard
                                            skill.exp = 0;
                                            continue;
                                        }
                                        p.update_skill_exp(skill.temp.ID, expReq);
                                        countUpgraded++;
                                    }
                                }
                            }
                        }
                        if (countUpgraded > 0) {
                            Service.send_box_ThongBao_OK(p, "Thành công nâng kỹ năng lên thêm 1 cấp");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Kỹ năng đã đạt cấp tối đa");
                            return false;
                        }
                        break;
                    }

                    case 1049:
                        return addDanhHieuIfNotOwned(p, 4, 16, 19);

                    case 1050:
                        return addDanhHieuIfNotOwned(p, 5, 17);

                    case 1063:
                        return addDanhHieuIfNotOwned(p, 9);

                    case 1064:
                        return addDanhHieuIfNotOwned(p, 7, 8, 10);

                    case 1065:
                        return addDanhHieuIfNotOwned(p, 1);

                    case 1066:
                        return addDanhHieuIfNotOwned(p, 2);

                    case 1067:
                        return addDanhHieuIfNotOwned(p, 3);

                    case 1068:
                        return addDanhHieuIfNotOwned(p, 6, 18, 20);

                    case 1069:
                        return addDanhHieuIfNotOwned(p, 21, 22);

                    case 1070:
                    case 1071:
                    case 1072: {
                        Service.send_box_ThongBao_OK(p, "Liên Hệ Admin Để Nhận");
                        break;
                    }

                    case 29: {
                        if (p.level < 20) {
                            Service.send_box_ThongBao_OK(p, "Yêu cầu đạt level 20 để mở rương");
                            return false;
                        }
                        // Danh sách các trái sơ cấp
                        short[] possibleIds = { 88, 318, 90, 34, 91 };
                        short id_add = possibleIds[Util.random(possibleIds.length)];
                        if (!p.item.add_item_bag47(4, id_add, 1)) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        open_taq_random(p, id_add, "Rương ác quỷ", "Nhận ngẫu nhiên");
                        break;
                    }

                    case 31: { // Bùa xóa tội
                        if (p.pointPk <= 0) {
                            Service.send_box_ThongBao_OK(p, "Bạn không có điểm PK để xóa!");
                            return false;
                        }
                        p.pointPk -= 10;
                        if (p.pointPk < 0) {
                            p.pointPk = 0;
                        }
                        Service.Main_char_Info(p);
                        p.update_info_to_all();
                        Service.send_box_ThongBao_OK(p, "Sử dụng Bùa xóa tội thành công. Giảm 10 điểm PK!");
                        break;
                    }

                    case 158: {
                        if (p.level < 20) {
                            Service.send_box_ThongBao_OK(p, "Yêu cầu đạt level 20 để mở rương");
                            return false;
                        }
                        // Danh sách các trái trung cấp
                        short[] midFruits = { 316, 32, 93, 317, 92, 219, 220, 33 };
                        short id_add = midFruits[Util.random(midFruits.length)];
                        if (!p.item.add_item_bag47(4, id_add, 1)) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        open_taq_random(p, id_add, "Rương đại ác quỷ", "Nhận ngẫu nhiên");
                        break;
                    }

                    case 1045: {
                        if (p.level < 20) {
                            Service.send_box_ThongBao_OK(p, "Yêu cầu đạt level 20 để mở rương");
                            return false;
                        }
                        // Danh sách trái siêu cấp
                        short[] superFruits = { 240, 161, 160 };
                        short id_add = superFruits[Util.random(superFruits.length)];
                        if (!p.item.add_item_bag47(4, id_add, 1)) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        open_taq_random(p, id_add, "Rương siêu ác quỷ", "Nhận ngẫu nhiên");
                        break;
                    }

                    case 32:
                    case 33:
                    case 34:
                    case 88:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                    case 160:
                    case 161:
                    case 219:
                    case 220:
                    case 240:
                    case 316:
                    case 317:
                    case 318:
                    case 427:
                    case 684: {
                        if (p.myDisciple != null) {
                            p.data_yesno = new int[] { id };
                            p.pendingFruitItemId = id;
                            MenuController.send_dynamic_menu(p, 8890, "Sử dụng cho ai?",
                                    new String[] { "Sư phụ (Bản thân)", "Đệ tử" }, null);
                        } else {
                            Service.send_box_yesno(p, (id + 4000), "Thông báo",
                                    "Bạn có muốn sử dụng " + ItemTemplate4.get_it_by_id(id).name,
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                        }
                        return false;
                    }
                    case 80: {
                        EffTemplate eff = p.get_eff(2);
                        if (eff != null && (eff.time > (System.currentTimeMillis() + 3000L))) {
                            if ((eff.time - System.currentTimeMillis()) < (1000L * 60 * 60 * 24
                                    * 7)) {
                                eff.time += (1000L * 60 * 60 * 2);
                            }
                        } else {
                            p.add_new_eff(2, 2, (60_000L * 60 * 2));
                        }
                        Service.CountDown_Ticket(p);
                        Service.send_box_ThongBao_OK(p,
                                "Dùng x2 exp thành công, lưu ý thời gian cộng dồn tối đa 7 ngày");
                        break;
                    }
                    case 86: {
                        if (p.myDisciple != null) {
                            p.data_yesno = new int[] { id };
                            p.pendingFruitItemId = id;
                            MenuController.send_dynamic_menu(p, 8890, "Sử dụng cho ai?",
                                    new String[] { "Sư phụ (Bản thân)", "Đệ tử" }, null);
                        } else {
                            Service.send_box_yesno(p, 35, "Thông báo",
                                    "Bạn có muốn sử dụng Trái Ác Quỷ?", new String[] { "Đồng ý", "Hủy" },
                                    new byte[] { -1, -1 });
                        }
                        return false;
                    }
                    case 87: {
                        if (p.myDisciple != null) {
                            p.data_yesno = new int[] { id };
                            p.pendingFruitItemId = id;
                            MenuController.send_dynamic_menu(p, 8890, "Sử dụng cho ai?",
                                    new String[] { "Sư phụ (Bản thân)", "Đệ tử" }, null);
                        } else {
                            Service.send_box_yesno(p, 37, "Thông báo",
                                    "Bạn có muốn sử dụng Trái Ác Quỷ trung cấp?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                        }
                        return false;
                    }

                    case 159: {
                        EffTemplate eff = p.get_eff(3);
                        if (eff != null && (eff.time > (System.currentTimeMillis() + 3000L))) {
                            if ((eff.time - System.currentTimeMillis()) < (1000L * 60 * 60 * 24
                                    * 7)) {
                                eff.time += (1000L * 60 * 60 * 2);
                            }
                        } else {
                            p.add_new_eff(3, 3, (60_000L * 60 * 2));
                        }
                        eff = p.get_eff(3);
                        Service.send_box_ThongBao_OK(p, "Thời gian x2 kỹ năng EXP còn lại "
                                + Util.get_time_str_by_sec2(eff.time - System.currentTimeMillis())
                                + "\nCó thể xem lại ở npc Robin, Lưu ý thời gian cộng dồn tối đa 7 ngày");
                        break;
                    }
                    case 179: {
                        EffTemplate eff = p.get_eff(4);
                        if (eff != null && (eff.time > (System.currentTimeMillis() + 3000L))) {
                            if ((eff.time - System.currentTimeMillis()) < (1000L * 60 * 60 * 24
                                    * 7)) {
                                eff.time += (1000L * 60 * 5);
                            }
                        } else {
                            p.add_new_eff(4, 50, (60_000L * 5));
                            //
                            p.update_info_to_all();
                        }
                        break;
                    }

                    case 592: // Hộp quà Huy Hiệu Cỏ
                    case 593: // Hộp quà Huy Hiệu Lửa
                    case 594: // Hộp quà Huy Hiệu Nước
                    case 595: // Hộp quà Huy Hiệu Đá
                    case 582: // Hộp quà Huy Hiệu Điện
                    case 596: // Hộp quà Huy Hiệu Đặc Biệt
                    {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int typeOffset = 0;
                        switch (id) {
                            case 592:
                                typeOffset = 0;
                                break;
                            case 593:
                                typeOffset = 1;
                                break;
                            case 594:
                                typeOffset = 2;
                                break;
                            case 595:
                                typeOffset = 3;
                                break;
                            case 582:
                                typeOffset = 4;
                                break;
                            case 596:
                                typeOffset = 5;
                                break;
                        }

                        int roll = Util.random(100);
                        int tierIndex = 0;
                        int minLines = 1, maxLines = 2, minVal = 10, maxVal = 50;
                        String tierName = "U";

                        if (roll < 65) { // [U]
                            tierIndex = 0;
                            tierName = "U";
                            minLines = 1;
                            maxLines = 2;
                            minVal = 100;
                            maxVal = 250;
                        } else if (roll < 85) { // [SS]
                            tierIndex = 1;
                            tierName = "SS";
                            minLines = 2;
                            maxLines = 3;
                            minVal = 300;
                            maxVal = 450;
                        } else if (roll < 95) { // [SR]
                            tierIndex = 2;
                            tierName = "SR";
                            minLines = 3;
                            maxLines = 4;
                            minVal = 450;
                            maxVal = 600;
                        } else if (roll < 99) { // [SSS]
                            tierIndex = 3;
                            tierName = "SSS";
                            minLines = 4;
                            maxLines = 5;
                            minVal = 600;
                            maxVal = 750;
                        } else { // [SSR]
                            tierIndex = 4;
                            tierName = "SSR";
                            minLines = 5;
                            maxLines = 6;
                            minVal = 900;
                            maxVal = 1000;
                        }

                        int badgeId = 15001 + (tierIndex * 6) + typeOffset;
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(badgeId);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomDebuffStatsScaled(it_new, temp.color, minLines, maxLines, minVal,
                                    maxVal);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Huy Hiệu [" + tierName + "]",
                                        "Chúc mừng bạn nhận được " + temp.name, list, true);
                            }
                        }
                        break;
                    }

                    case 1051: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 0 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Rương trống rỗng!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomStatsScaled(it_new, temp.color, 1, 2, 10, 50);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1; // -1 to prevent adding again via send_gift
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị U", list, true);
                            }
                        }
                        break;
                    }

                    case 1052: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 1 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Rương trống rỗng!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomStatsScaled(it_new, temp.color, 2, 3, 50, 60);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SS", list, true);
                            }
                        }
                        break;
                    }

                    case 1053: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 2 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Rương trống rỗng!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomStatsScaled(it_new, temp.color, 3, 4, 70, 80);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SR", list, true);
                            }
                        }
                        break;
                    }

                    case 1054: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 3 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Rương trống rỗng!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomStatsScaled(it_new, temp.color, 4, 4, 80, 90);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SSS", list, true);
                            }
                        }
                        break;
                    }

                    case 1055: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 4 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Rương trống rỗng!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomStatsScaled(it_new, temp.color, 4, 4, 90, 100);
                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SSR", list, true);
                            }
                        }
                        break;
                    }

                    case 1085: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int plus = 0;
                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null && it.color == 4 && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }

                        if (!validIds.isEmpty()) {
                            int id_add = validIds.get(Util.random(validIds.size()));
                            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                            if (temp != null) {
                                Item_wear it_ssr = new Item_wear();
                                it_ssr.setup_template_by_id(temp);
                                it_ssr.levelup = (byte) plus;

                                int[] randomStats = { 5, 6, 7, 8, 9 };
                                int colorBonus = temp.color * 15;
                                for (int j = 0; j < 5; j++) {
                                    int optId = randomStats[Util.random(randomStats.length)];
                                    int optVal = Util.random(100, 301) + colorBonus + (plus / 2);
                                    it_ssr.option_item.add(new Option(optId, optVal));
                                }

                                if (p.item.add_item_bag3(it_ssr)) {
                                    List<GiftBox> listGift = new ArrayList<>();
                                    GiftBox gb = new GiftBox();
                                    gb.id = -1;
                                    gb.type = 3;
                                    gb.name = temp.name + " +" + plus;
                                    gb.icon = temp.icon;
                                    gb.num = 1;
                                    gb.color = temp.color;
                                    listGift.add(gb);
                                    Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SSR", listGift, true);
                                }
                            }
                        }
                        break;
                    }

                    case 1086: {
                        if (p.huntTarget != null) {
                            Service.send_box_yesno(p, 104, "Máy dò ngọc rồng",
                                    "Máy dò trước đã dò ra một mục tiêu, bạn có muốn dùng máy khác dò lại không?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, 1 });
                            return false; // ← thêm dòng này, không trừ item
                        } else {
                            boolean success = activities.WishManager.gI().startHunting(p);
                            if (!success)
                                return false; // không trừ item nếu fail vì không có target
                        }
                        break; // chỉ đến đây khi startHunting thành công → use_item_potion trừ item
                    }

                    case 1094: {
                        // 1. Kiểm tra hành trang
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }

                        int plus = 0;

                        // 2. Thuật toán quay tỉ lệ Gacha (0 đến 99)
                        int rate = Util.random(100);

                        // Khai báo sẵn các biến chứa ID, số dòng và giới hạn chỉ số
                        int finalId;
                        int numLines;
                        int minVal;
                        int maxVal;

                        // Xét tỉ lệ và phân bổ sức mạnh tương ứng
                        if (rate < 5) {
                            finalId = 13005; // 5% (từ 0 đến 4) - Rất hiếm
                            numLines = 5;
                            minVal = 200;
                            maxVal = 301;
                        } else if (rate < 15) {
                            finalId = 13004; // 10% (từ 5 đến 14) - Hiếm
                            numLines = 4;
                            minVal = 150;
                            maxVal = 251;
                        } else if (rate < 35) {
                            finalId = 13003; // 20% (từ 15 đến 34) - Khá
                            numLines = 3;
                            minVal = 100;
                            maxVal = 201;
                        } else if (rate < 60) {
                            finalId = 13002; // 25% (từ 35 đến 59) - Thường
                            numLines = 2;
                            minVal = 50;
                            maxVal = 151;
                        } else {
                            finalId = 13001; // 40% (từ 60 đến 99) - Cùi nhất
                            numLines = 1;
                            minVal = 20;
                            maxVal = 101;
                        }

                        // 3. Lấy Template của ID vừa quay trúng
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(finalId);

                        // 4. Kiểm tra và thêm item
                        if (temp != null) {
                            Item_wear it_ssr = new Item_wear();
                            it_ssr.setup_template_by_id(temp);
                            it_ssr.levelup = (byte) plus;

                            int[] randomStats = { 5, 6, 7, 8, 9 };
                            int colorBonus = temp.color * 15;

                            // Vòng lặp đập chỉ số: chạy theo numLines thay vì fix cứng là 5
                            for (int j = 0; j < numLines; j++) {
                                int optId = randomStats[Util.random(randomStats.length)];
                                // Random optVal theo minVal và maxVal đã được set ở trên
                                int optVal = Util.random(minVal, maxVal) + colorBonus + (plus / 2);
                                it_ssr.option_item.add(new Option(optId, optVal));
                            }

                            // Thêm vào túi và gửi thông báo
                            if (p.item.add_item_bag3(it_ssr)) {
                                p.item.update_Inventory(-1, false);

                                List<GiftBox> listGift = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name + (plus > 0 ? " +" + plus : "");
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                listGift.add(gb);

                                Service.send_gift(p, 1, "Mở Khóa Rương", "Nhận được: " + temp.name, listGift, true);
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lỗi: Không tìm thấy vật phẩm " + finalId + "!");
                        }
                        break;
                    }

                    case 1095: {
                        short fashionId = 1; // Thay ID thời trang bạn muốn trao vào đây

                        // 1. Kiểm tra xem người chơi đã sở hữu thời trang này chưa
                        if (p.check_fashion(fashionId) != null) {
                            // 2. Nếu ĐÃ CÓ RỒI thì thông báo
                            Service.send_box_ThongBao_OK(p, "Bạn đã sở hữu thời trang này rồi, không thể nhận thêm.");
                            // Ngắt luồng ngay lập tức, trả về false để hệ thống (use_item_potion) KHÔNG TRỪ
                            // vật phẩm
                            return false;

                        } else {
                            // 3. Nếu CHƯA CÓ thì khởi tạo và add vào list
                            ItemFashionP2 newFashion = new ItemFashionP2();
                            newFashion.id = fashionId;
                            newFashion.is_use = false;
                            newFashion.level = 0;
                            newFashion.expirationTime = -1; // Vĩnh viễn
                            newFashion.op = ItemFashionP2.randomOptionsForFashion(fashionId);

                            p.fashion.add(newFashion);

                            // 4. Thông báo và lưu dữ liệu
                            Service.send_box_ThongBao_OK(p, "Chúc mừng! Bạn đã nhận được thời trang Thần Mặt Trời!");
                            Player.flush(p, false);

                            // Code chạy tới đây sẽ lọt xuống break, thoát vòng switch và trả về true ở cuối
                            // hàm use_item_4.
                            // Phễu use_item_potion nhận được true sẽ tự động trừ đi 1 vật phẩm dùng để mở
                            // và update lại Inventory.
                        }
                        break;
                    }

                    case 1096: {
                        Service.send_box_yesno(p, 1096, "Thuốc chuyển giới",
                                "Bạn muốn đổi thành phái nào? Trang bị đang mặc sẽ tự động chuyển đổi. Lỗ khảm, chỉ số, cấp độ giữ nguyên.",
                                new String[] { "Võ sĩ", "Kiếm khách", "Đầu bếp", "Hoa tiêu", "Xạ thủ", "Hủy" },
                                new byte[] { -1, -1, -1, -1, -1, -1 });
                        return false;
                    }

                    case 1097: {
                        String msg = (p.myDisciple == null)
                                ? "Bạn muốn triệu hồi một đệ tử ngẫu nhiên không?"
                                : "Bạn muốn đổi đệ tử hiện tại không?\n(Lưu ý: Mọi cấp độ và trang bị của đệ cũ sẽ mất, đệ mới sẽ có phái ngẫu nhiên)";
                        Service.send_box_yesno(p, 1097, "Lõi Đệ Tử", msg, new String[] { "Đồng ý", "Hủy" },
                                new byte[] { -1, -1 });
                        return false;
                    }

                    case 1098: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }

                        int rate = Util.random(100);
                        int finalId;
                        int numLines;
                        int minV, maxV;

                        if (rate < 5) { // 5% - Hiếm nhất
                            finalId = 11081;
                            numLines = 10;
                            minV = 200;
                            maxV = 351;
                        } else if (rate < 15) { // 10% - Hiểm nhì
                            finalId = 11080;
                            numLines = 7;
                            minV = 150;
                            maxV = 251;
                        } else if (rate < 35) { // 20% - Hiếm thứ ba
                            finalId = 11019;
                            numLines = 5;
                            minV = 100;
                            maxV = 201;
                        } else { // 65% - Ít hiếm nhất
                            finalId = 11018;
                            numLines = 3;
                            minV = 50;
                            maxV = 151;
                        }

                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(finalId);
                        if (temp != null) {
                            Item_wear it = new Item_wear();
                            it.setup_template_by_id(temp);
                            it.levelup = 0;
                            it.typelock = 1;

                            // Ngẫu nhiên hóa số dòng Thường và Debuff
                            int normalLines = Util.random(0, numLines + 1);
                            int debuffLines = numLines - normalLines;

                            if (normalLines > 0) {
                                ItemDropTool.addRandomStatsScaled(it, temp.color, normalLines, normalLines, minV, maxV);
                            }
                            if (debuffLines > 0) {
                                ItemDropTool.addRandomDebuffStatsScaled(it, temp.color, debuffLines, debuffLines, minV,
                                        maxV);
                            }

                            if (p.item.add_item_bag3(it)) {
                                p.item.update_Inventory(-1, false);

                                List<GiftBox> listGift = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                listGift.add(gb);

                                Service.send_gift(p, 1, "Mở Rương",
                                        "Bạn nhận được: " + temp.name + " (" + numLines + " dòng)", listGift, true);
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lỗi: Không tìm thấy vật phẩm " + finalId);
                            return false;
                        }
                        break;
                    }

                    case 1099: {
                        if (p.myDisciple == null) {
                            Service.send_box_ThongBao_OK(p, "Bạn không có đệ tử để dung hợp!");
                            break;
                        }
                        if (p.fusionType == 2) {
                            // Đang hợp thể vĩnh viễn → Tách
                            p.stopFusion();
                            Service.send_box_ThongBao_OK(p, "Đã tách dung hợp thành công!");
                        } else if (p.fusionType != 0) {
                            // Đang hợp thể loại khác (vd: chat command type 1) → Không cho dùng
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn đang trong trạng thái hợp thể tạm thời, hãy đợi hết hiệu lực!");
                        } else {
                            // Chưa hợp thể → Kích hoạt vĩnh viễn type 2
                            p.startFusion(-1, 2);
                            Service.send_box_ThongBao_OK(p, "Dung hợp vĩnh viễn thành công!");
                        }
                        return false;
                    }

                    case 1100: {
                        if (p.myDisciple == null) {
                            Service.send_box_ThongBao_OK(p, "Bạn không có đệ tử để thực hiện lệnh này!");
                            return false;
                        }
                        long now = System.currentTimeMillis();
                        long duration = 15 * 60 * 1000L; // 15 phút
                        if (p.myDisciple.timeTanSat < now) {
                            p.myDisciple.timeTanSat = now + duration;
                        } else {
                            p.myDisciple.timeTanSat += duration;
                        }
                        p.myDisciple.status = (byte) 4;
                        String timeStr = core.Util.get_time_str_by_sec2(p.myDisciple.timeTanSat - now);
                        Service.send_box_ThongBao_OK(p,
                                "Đã kích hoạt chế độ Tàn Sát cho đệ tử!\nTổng thời gian còn lại: " + timeStr);
                        p.myDisciple.chat("Tàn sát thôi sư phụ ơi!!!");
                        return true;
                    }

                    case 1103: {
                        short f1 = 31;
                        short f2 = 32;
                        boolean hasF1 = p.check_fashion(f1) != null;
                        boolean hasF2 = p.check_fashion(f2) != null;
                        short fashionId;
                        if (hasF1 && hasF2) {
                            Service.send_box_ThongBao_OK(p, "Bạn đã sở hữu đầy đủ bộ thời trang trong rương rồi.");
                            return false;
                        } else if (hasF1) {
                            fashionId = f2;
                        } else if (hasF2) {
                            fashionId = f1;
                        } else {
                            fashionId = (short) (Util.random(100) < 50 ? f1 : f2);
                        }

                        ItemFashionP2 newFashion = new ItemFashionP2();
                        newFashion.id = fashionId;
                        newFashion.is_use = false;
                        newFashion.level = 0;
                        newFashion.expirationTime = -1;
                        newFashion.op = ItemFashionP2.randomOptionsForFashion(fashionId);

                        p.fashion.add(newFashion);
                        Service.send_box_ThongBao_OK(p, "Chúc mừng! Bạn đã nhận được một bộ thời trang mới!");
                        Player.flush(p, false);
                        break;
                    }

                    case 1104: // Hộp quà Huy Hiệu Siêu Cấp (SSR & GR)
                    {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }

                        int mainRoll = Util.random(100);
                        int badgeId = 0;
                        String tierName = "";
                        int minLines = 1, maxLines = 2, minVal = 10, maxVal = 50;

                        // 1. Tỉ lệ 5 (50%): 6 Huy hiệu bình thường (SSR hoặc GR)
                        if (mainRoll < 50) {
                            int normalTypeOffset = Util.random(6);
                            int rarityRoll = Util.random(10);

                            if (rarityRoll < 8) { // SSR Thường
                                tierName = "SSR";
                                badgeId = 15025 + normalTypeOffset;
                                minLines = 5;
                                maxLines = 6;
                                minVal = 900;
                                maxVal = 1000; // Chỉ số SSR thường
                            } else { // GR Thường
                                tierName = "GR";
                                badgeId = 15031 + normalTypeOffset;
                                minLines = 6;
                                maxLines = 7;
                                minVal = 1200;
                                maxVal = 1500; // Chỉ số GR thường
                            }
                        }
                        // 2. Tỉ lệ 4 (40%): Huy hiệu Bóng Tối & Ánh Sáng (Phẩm U đổ lên SSR)
                        else if (mainRoll < 90) {
                            boolean isLight = Util.random(2) == 1;
                            int rareTierIndex = Util.random(5);

                            // ĐÃ BUFF: Tăng minVal và maxVal cao hơn so với huy hiệu thường
                            switch (rareTierIndex) {
                                case 0:
                                    tierName = "U";
                                    minLines = 1;
                                    maxLines = 2;
                                    minVal = 150;
                                    maxVal = 300;
                                    break; // Thường là 100-250
                                case 1:
                                    tierName = "SS";
                                    minLines = 2;
                                    maxLines = 3;
                                    minVal = 350;
                                    maxVal = 500;
                                    break; // Thường là 300-450
                                case 2:
                                    tierName = "SR";
                                    minLines = 3;
                                    maxLines = 4;
                                    minVal = 500;
                                    maxVal = 700;
                                    break; // Thường là 450-600
                                case 3:
                                    tierName = "SSS";
                                    minLines = 4;
                                    maxLines = 5;
                                    minVal = 700;
                                    maxVal = 900;
                                    break; // Thường là 600-750
                                case 4:
                                    tierName = "SSR";
                                    minLines = 5;
                                    maxLines = 6;
                                    minVal = 1050;
                                    maxVal = 1200;
                                    break; // Thường là 900-1000
                            }

                            if (!isLight) {
                                badgeId = 15037 + rareTierIndex;
                            } else {
                                badgeId = 15043 + rareTierIndex;
                            }
                        }
                        // 3. Tỉ lệ 1 (10%): Huy hiệu Bóng Tối & Ánh Sáng (Phẩm GR)
                        else {
                            boolean isLight = Util.random(2) == 1;
                            tierName = "GR";
                            // ĐÃ BUFF: Nhỉnh hơn GR thường (1200-1500) và có thể ra 8 dòng
                            minLines = 6;
                            maxLines = 8;
                            minVal = 1400;
                            maxVal = 1800;

                            if (!isLight) {
                                badgeId = 15042;
                            } else {
                                badgeId = 15048;
                            }
                        }

                        // Chốt chặn an toàn
                        if (badgeId > 15048) {
                            badgeId = 15048;
                        }

                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(badgeId);
                        if (temp != null) {
                            Item_wear it_new = new Item_wear();
                            it_new.setup_template_by_id(temp);
                            ItemDropTool.addRandomDebuffStatsScaled(it_new, temp.color, minLines, maxLines, minVal,
                                    maxVal);

                            if (p.item.add_item_bag3(it_new)) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1;
                                gb.type = 3;
                                gb.name = temp.name;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Rương Trang bị 3 Siêu Cấp",
                                        "Chúc mừng bạn nhận được " + temp.name + " phẩm [" + tierName + "]", list,
                                        true);
                            }
                        }
                        break;
                    }

                    case 1106: {
                        // Kiểm tra đá nguyên liệu
                        if (p.item.total_item_bag_by_id(4, 514) < 20 ||
                                p.item.total_item_bag_by_id(4, 515) < 20 ||
                                p.item.total_item_bag_by_id(4, 516) < 20) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn cần có 20 Đá mặt trăng, 20 Đá sao hỏa và 20 Đá khổng tước!");
                            return false;
                        }

                        // Tìm trang bị GR phù hợp (color 8, id < 144, tb2 == 0)
                        List<Integer> listIndex = new ArrayList<>();
                        List<String> listName = new ArrayList<>();
                        for (int i = 0; i < p.item.bag3.length; i++) {
                            Item_wear it = p.item.bag3[i];
                            if (it != null && it.template != null && it.template.color == 8 && it.template.tb2 == 0
                                    && it.template.id < 144) {
                                listIndex.add(i);
                                listName.add(it.template.name + " (+" + it.levelup + ")");
                            }
                        }

                        if (listIndex.isEmpty()) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không có trang bị GR (color 8) nào phù hợp trong hành trang!");
                            return false;
                        }

                        p.tempIndexMapping = listIndex;
                        MenuController.send_dynamic_menu(p, 1106, "Chọn trang bị GR để nâng cấp",
                                listName.toArray(new String[0]));
                        return false;
                    }

                    case 1107: {
                        short fashionId = 41; // ID thời trang Yamato
                        int fragmentId = 1107; // ID mảnh skin Yamato
                        int requiredCount = 1000;

                        // 1. Kiểm tra số lượng mảnh
                        int currentCount = p.item.total_item_bag_by_id(4, fragmentId);
                        if (currentCount < requiredCount) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn cần đủ " + requiredCount + " mảnh Yamato để đổi. Hiện có: " + currentCount);
                            return false;
                        }

                        // 2. Kiểm tra xem đã sở hữu thời trang chưa
                        if (p.check_fashion(fashionId) != null) {
                            Service.send_box_ThongBao_OK(p, "Bạn đã sở hữu thời trang Yamato rồi.");
                            return false;
                        }

                        // 3. Trao thời trang
                        ItemFashionP2 newFashion = new ItemFashionP2();
                        newFashion.id = fashionId;
                        newFashion.is_use = false;
                        newFashion.level = 0;
                        newFashion.expirationTime = -1; // Vĩnh viễn
                        newFashion.op = ItemFashionP2.randomOptionsForFashion(fashionId);

                        p.fashion.add(newFashion);

                        // 4. Trừ mảnh (Hệ thống sẽ tự trừ 1 mảnh khi return true, nên ta trừ thêm 999
                        // mảnh)
                        p.item.remove_item47(4, fragmentId, requiredCount - 1);
                        p.item.update_Inventory(-1, false);

                        // 5. Thông báo và lưu
                        Service.send_box_ThongBao_OK(p,
                                "Chúc mừng! Bạn đã thu thập đủ 1000 mảnh và nhận được thời trang Yamato!");
                        Player.flush(p, false);
                        break;
                    }

                    case 455: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int[] itemIds = { 12001, 12002, 12003, 12004, 12005 };
                        int[] chances = { 68, 17, 8, 5, 2 }; // tổng = 100
                        int rand = Util.random(101);
                        int id_random = itemIds[itemIds.length - 1]; // Mặc định ra item khó nhất
                        int cumulative = 0;
                        for (int i = 0; i < itemIds.length; i++) {
                            cumulative += chances[i];
                            if (rand <= cumulative) {
                                id_random = itemIds[i];
                                break;
                            }
                        }
                        List<GiftBox> list = new ArrayList<>();
                        ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(id_random);
                        if (itemTemplate3 != null) {
                            GiftBox gb4 = new GiftBox();
                            gb4.id = (short) id_random;
                            gb4.type = 3;
                            gb4.name = itemTemplate3.name;
                            gb4.icon = itemTemplate3.icon;
                            gb4.num = 1;
                            gb4.color = itemTemplate3.color;
                            list.add(gb4);
                        }

                        if (!list.isEmpty()) {
                            Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Dial", list, true);
                        }
                        break;
                    }

                    case 1018: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        if (p.level < 20) {
                            Service.send_box_ThongBao_OK(p, "Yêu cầu đạt level 20 để mở trứng");
                            return false;
                        }
                        int[] idOptions = { 11002, 11003, 11010, 11011, 11012, 11013, 11014, 11016, 11017 };
                        int id_random = idOptions[Util.random(idOptions.length)];
                        List<GiftBox> list = new ArrayList<>();
                        ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(id_random);
                        if (itemTemplate3 != null) {
                            GiftBox gb4 = new GiftBox();
                            gb4.id = (short) id_random;
                            gb4.type = 3;
                            gb4.name = itemTemplate3.name;
                            gb4.icon = itemTemplate3.icon;
                            gb4.num = 1;
                            gb4.color = itemTemplate3.color;
                            list.add(gb4);
                        }
                        if (list.size() > 0) {
                            Service.send_gift(p, 1, "Mở Trứng", "Trứng Pet", list, true);
                        }
                        break;
                    }

                    case 1040: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
                            return false;
                        }
                        int[] idOptions = { 11082 };
                        int id_random = idOptions[Util.random(idOptions.length)];
                        List<GiftBox> list = new ArrayList<>();
                        ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(id_random);
                        if (itemTemplate3 != null) {
                            GiftBox gb4 = new GiftBox();
                            gb4.id = (short) id_random;
                            gb4.type = 3;
                            gb4.name = itemTemplate3.name;
                            gb4.icon = itemTemplate3.icon;
                            gb4.num = 1;
                            gb4.color = itemTemplate3.color;
                            list.add(gb4);
                        }
                        if (list.size() > 0) {
                            Service.send_gift(p, 1, "Mở Trứng", "Trứng Pet VIP", list, true);
                        }
                        break;
                    }

                    case 1019: {
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
                            return false;
                        }
                        int[] idOptions = {
                                11020, 11021, 11022, 11023, 11024, 11025, 11026, 11027, 11028, 11029,
                                11030, 11031, 11032, 11033, 11034, 11035, 11036, 11037, 11050, 11051, 11052, 11053
                        };
                        int id_random = idOptions[Util.random(idOptions.length)];

                        List<GiftBox> list = new ArrayList<>();
                        ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(id_random);
                        if (itemTemplate3 != null) {
                            GiftBox gb4 = new GiftBox();
                            gb4.id = (short) id_random;
                            gb4.type = 3;
                            gb4.name = itemTemplate3.name;
                            gb4.icon = itemTemplate3.icon;
                            gb4.num = 1;
                            gb4.color = itemTemplate3.color;
                            list.add(gb4);
                        }

                        if (list.size() > 0) {
                            Service.send_gift(p, 1, "Mở Rương", "Rương tinh tú", list, true);
                        }
                        break;
                    }

                    case 1026: {
                        Skill_info sk_select = null;
                        for (Skill_info skill : p.skill_point) {
                            if (skill.temp.ID == 5004) {
                                sk_select = skill;
                                break;
                            }
                        }
                        if (sk_select == null) {
                            Skill_Template firstSkill = Skill_Template.get_temp(685, 0);
                            if (firstSkill != null) {
                                sk_select = new Skill_info();
                                sk_select.exp = 0;
                                sk_select.temp = firstSkill;
                                sk_select.lvdevil = 0;
                                sk_select.devilpercent = 0;
                                p.skill_point.add(sk_select);
                                p.send_skill();
                                Service.send_box_ThongBao_OK(p, "Kích hoạt Bá Vương thành công");
                            }
                        } else {
                            int currentLv = sk_select.temp.Lv_RQ;
                            Skill_Template nextSkill = null;
                            for (Skill_Template skill : Skill_Template.ENTRYS) {
                                if (skill.ID == 5004 && skill.Lv_RQ == currentLv + 1) {
                                    nextSkill = skill;
                                    break;
                                }
                            }
                            if (nextSkill != null) {
                                sk_select.temp = nextSkill;
                                sk_select.exp = 0;
                                p.send_skill();
                                Service.send_box_ThongBao_OK(p,
                                        "Đã nâng cấp Bá Vương lên cấp " + nextSkill.Lv_RQ);
                            } else {
                                Service.send_box_ThongBao_OK(p, "Bá Vương đã đạt cấp tối đa");
                                return false;
                            }
                        }
                        break;
                    }

                    case 1044: {
                        Skill_info sk_select = null;
                        for (Skill_info skill : p.skill_point) {
                            if (skill.temp.ID == 5005) {
                                sk_select = skill;
                                break;
                            }
                        }
                        if (sk_select == null) {
                            Skill_Template firstSkill = Skill_Template.get_temp(841, 0);
                            if (firstSkill != null) {
                                sk_select = new Skill_info();
                                sk_select.exp = 0;
                                sk_select.temp = firstSkill;
                                sk_select.lvdevil = 0;
                                sk_select.devilpercent = 0;
                                p.skill_point.add(sk_select);
                                p.send_skill();
                                Service.send_box_ThongBao_OK(p, "Kích hoạt Gió Vương thành công");
                            }
                        } else {
                            int currentLv = sk_select.temp.Lv_RQ;
                            Skill_Template nextSkill = null;
                            for (Skill_Template skill : Skill_Template.ENTRYS) {
                                if (skill.ID == 5005 && skill.Lv_RQ == currentLv + 1) {
                                    nextSkill = skill;
                                    break;
                                }
                            }
                            if (nextSkill != null) {
                                sk_select.temp = nextSkill;
                                sk_select.exp = 0;
                                p.send_skill();
                                Service.send_box_ThongBao_OK(p,
                                        "Đã nâng cấp Gió Vương lên cấp " + nextSkill.Lv_RQ);
                            } else {
                                Service.send_box_ThongBao_OK(p, "Gió Vương đã đạt cấp tối đa");
                                return false;
                            }
                        }
                        break;
                    }

                    case ItemDropTool.ITEM_BOI_TU_TIEN:
                    case ItemDropTool.ITEM_BOI_TU_MA:
                    case Mastery2.ITEM_BOI_TU_TIEN_2:
                    case Mastery2.ITEM_BOI_TU_MA_2: {
                        if (System.currentTimeMillis() - p.lastTimeUseItem < Manager.gI().item_usage_cooldown) {
                            return false;
                        }
                        p.lastTimeUseItem = System.currentTimeMillis();

                        if (id == Mastery2.ITEM_BOI_TU_TIEN_2 || id == Mastery2.ITEM_BOI_TU_MA_2) {
                            if (!p.hasUnlockedMastery2) {
                                Service.send_box_ThongBao_OK(p, "Bạn chưa mở khóa Thông Thạo 2 để dùng Bội cấp 2!");
                                return false;
                            }
                            if (Util.random(100) < ItemDropTool.TU_TIEN_BOI_SUCCESS_RATE) {
                                Mastery2.processUseBoi(p, true, id);
                            } else {
                                String itemName = (id == Mastery2.ITEM_BOI_TU_TIEN_2) ? "Bội Tu Tiên Cấp 2"
                                        : "Bội Tu Ma Cấp 2";
                                Service.send_box_ThongBao_OK(p,
                                        "Sử dụng " + itemName + " thất bại! Vật phẩm đã bị phá hủy.");
                                p.item.remove_item47(4, id, 1);
                                p.item.update_Inventory(-1, false);
                            }
                        } else {
                            if (p.hasUnlockedMastery2) {
                                Service.send_box_ThongBao_OK(p, "Bạn đã Thăng Giai, không thể dùng Bội thường!");
                                return false;
                            }
                            // Mastery 1 Bội logic
                            if (Util.random(100) < ItemDropTool.TU_TIEN_BOI_SUCCESS_RATE) {
                                Mastery2.processUseBoi(p, false, id);
                            } else {
                                String itemName = (id == ItemDropTool.ITEM_BOI_TU_TIEN) ? "Bội Tu Tiên" : "Bội Tu Ma";
                                Service.send_box_ThongBao_OK(p,
                                        "Sử dụng " + itemName + " thất bại! Vật phẩm đã bị phá hủy.");
                                p.item.remove_item47(4, id, 1);
                                p.item.update_Inventory(-1, false);
                            }
                        }
                        return false;
                    }

                    case 349: {
                        List<GiftBox> listGift = new ArrayList<>();
                        int soberi = Util.random(100_000_000, 110_000_000);
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                        GiftBox giftBox = new GiftBox();
                        giftBox.id = (short) 0;
                        giftBox.type = 4;
                        giftBox.name = itemTemplate4.name;
                        giftBox.icon = itemTemplate4.icon;
                        giftBox.num = soberi;
                        giftBox.color = 0;
                        listGift.add(giftBox);
                        Service.send_gift(p, 0, "Túi Beri", "", listGift,
                                true);
                        break;
                    }

                    case 1039: {
                        List<GiftBox> listGift = new ArrayList<>();
                        int num = Util.random(10_000, 20_000);
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.id = (short) 1;
                        giftBox.type = 4;
                        giftBox.name = itemTemplate4.name;
                        giftBox.icon = itemTemplate4.icon;
                        giftBox.num = num;
                        giftBox.color = 0;
                        listGift.add(giftBox);
                        Service.send_gift(p, 0, "Bao Ruby", "", listGift,
                                true);
                        break;
                    }
                    case 1046: {
                        if (p.getTongNap() == 0 && !Manager.gI().server_test) {
                            Service.send_box_ThongBao_OK(p, "Bạn chưa nạp lần đầu.");
                            return false;
                        }
                        List<GiftBox> listGiftP = new ArrayList<>();
                        GiftBox.addGiftBox(listGiftP, 0, 4, 200_000_000); // beri
                        GiftBox.addGiftBox(listGiftP, 1, 4, 50_000); // ruby
                        GiftBox.addGiftBox(listGiftP, 1037, 4, 1); // bội tu
                        GiftBox.addGiftBox(listGiftP, 1038, 4, 1); // bội ma
                        GiftBox.addGiftBox(listGiftP, 1047, 4, 1); // level
                        GiftBox.addGiftBox(listGiftP, 1048, 4, 1); // skill
                        Service.send_gift(p, 0, "Quà Nạp Đầu", "", listGiftP, true);
                        break;
                    }

                    case 1060: {
                        List<GiftBox> listGiftP = new ArrayList<>();
                        GiftBox.addGiftBox(listGiftP, 0, 4, Util.random(1_000_000, 5_000_000)); // beri
                        GiftBox.addGiftBox(listGiftP, 232, 4, Util.random(1, 5)); // vé quay
                        int[] ids = { 46, 52, 58, 64, 70, 76, 223, 364 }; // đá khảm
                        GiftBox.addGiftBox(listGiftP, ids[Util.random(ids.length)], 4, Util.random(1, 5));
                        GiftBox.addGiftBox(listGiftP, 1056, 4, Util.random(20, 30)); // mảnh u
                        Service.send_gift(p, 0, "Bóng Sơ Cấp", "", listGiftP, true);
                        break;
                    }

                    case 1061: {
                        List<GiftBox> listGiftP = new ArrayList<>();
                        GiftBox.addGiftBox(listGiftP, 0, 4, Util.random(5_000_000, 10_000_000)); // beri
                        GiftBox.addGiftBox(listGiftP, 232, 4, Util.random(5, 10)); // vé quay
                        int[] ids = { 47, 53, 59, 65, 71, 77, 224, 365 }; // đá khảm
                        GiftBox.addGiftBox(listGiftP, ids[Util.random(ids.length)], 4, Util.random(1, 5));
                        GiftBox.addGiftBox(listGiftP, 1057, 4, Util.random(10, 20)); // mảnh ss
                        Service.send_gift(p, 0, "Bóng Trung Cấp", "", listGiftP, true);
                        break;
                    }

                    case 1062: {
                        List<GiftBox> listGiftP = new ArrayList<>();
                        GiftBox.addGiftBox(listGiftP, 0, 4, Util.random(10_000_000, 20_000_000)); // beri
                        GiftBox.addGiftBox(listGiftP, 232, 4, Util.random(10, 15)); // vé quay
                        int[] ids = { 48, 54, 60, 66, 72, 78, 225, 366 }; // đá khảm
                        GiftBox.addGiftBox(listGiftP, ids[Util.random(ids.length)], 4, Util.random(1, 5));
                        GiftBox.addGiftBox(listGiftP, 1058, 4, Util.random(5, 10)); // mảnh sr
                        Service.send_gift(p, 0, "Bóng Siêu Cấp", "", listGiftP, true);
                        break;
                    }

                    case 350: {
                        List<GiftBox> listGiftP = new ArrayList<>();
                        {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(100_000, 500_000);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(100, 500);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        // Gửi quà cho người chơi
                        Service.send_gift(p, 0, "Bánh chưng", "", listGiftP, true);
                        break;
                    }
                    case 169: {
                        List<GiftBox> listGiftP = new ArrayList<>();
                        {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(50_000, 200_000);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(100, 500);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        // Gửi quà cho người chơi
                        Service.send_gift(p, 0, "Bao Lì Xì", "", listGiftP, true);
                        break;
                    }
                    // case 1055: { // Rương Trang Bị SSR
                    // if (p.item.able_bag() == 0) {
                    // Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                    // return false;
                    // }
                    // List<ItemTemplate3> list_ssr = new ArrayList<>();
                    // for (ItemTemplate3 it : ItemTemplate3.ENTRYS) {
                    // if (it != null && it.name != null && it.name.contains("[SSR]")
                    // && (it.clazz == p.clazz || it.clazz == 0)) {
                    // list_ssr.add(it);
                    // }
                    // }
                    // if (list_ssr.isEmpty()) {
                    // Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị [SSR] phù hợp hệ của
                    // bạn!");
                    // return false;
                    // }
                    // ItemTemplate3 temp = list_ssr.get(Util.random(list_ssr.size()));
                    // Item_wear it_ssr = new Item_wear();
                    // it_ssr.setup_template_by_id(temp);
                    // it_ssr.levelup = (byte) 100;
                    // p.item.add_item_bag3(it_ssr);
                    // p.item.update_Inventory(-1, false);
                    // Service.send_box_ThongBao_OK(p, "Bạn nhận được " + temp.name + " +100");
                    // break;
                    // }
                    case 1005: // Rương SSR (+12)
                    case 1006: { // Rương SSR (+15)
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int plus = (id == 1005) ? 12 : 15;

                        List<Integer> validIds = new ArrayList<>();
                        for (int i = 0; i < 120; i++) {
                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                            if (it != null
                                    && it.color == 4
                                    && it.typeEquip < 6
                                    && (it.clazz == 0 || it.clazz == p.clazz)) {
                                validIds.add(i);
                            }
                        }
                        if (validIds.isEmpty()) {
                            Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị phù hợp!");
                            return false;
                        }
                        int id_add = validIds.get(Util.random(validIds.size()));
                        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);

                        // Tạo item thực tế
                        Item_wear it_ssr = new Item_wear();
                        it_ssr.setup_template_by_id(temp);
                        it_ssr.levelup = (byte) plus;

                        // Thêm 5 dòng chỉ số ngẫu nhiên
                        int[] randomStats = { 5, 6, 7, 8, 9 };
                        int colorBonus = temp.color * 15; // color 4 → +60
                        for (int j = 0; j < 5; j++) {
                            int optId = randomStats[Util.random(randomStats.length)];
                            int optVal = Util.random(100, 201) + colorBonus + (plus / 2);
                            it_ssr.option_item.add(new Option(optId, optVal));
                        }

                        p.item.add_item_bag3(it_ssr);
                        p.item.update_Inventory(-1, false);

                        // Hiển thị popup GiftBox
                        if (temp != null) {
                            List<GiftBox> list = new ArrayList<>();
                            GiftBox gb = new GiftBox();
                            gb.id = -1; // Set to -1 to prevent send_gift from adding item automatically
                            gb.type = 3;
                            gb.name = temp.name + " +" + plus;
                            gb.icon = temp.icon;
                            gb.num = 1;
                            gb.color = temp.color;
                            list.add(gb);
                            Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SSR +" + plus, list, true);
                        }
                        break;
                    }

                    case 1008: // +20
                    case 1009: // +30
                    case 1010: // +40
                    case 1073: // +50
                    case 1074: // +60
                    case 1075: // +70
                    case 1076: // +80
                    case 1077: // +90
                    case 1078: // +95
                    case 1079: { // +100
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int plus = 0;
                        if (id == 1008)
                            plus = 20;
                        else if (id == 1009)
                            plus = 30;
                        else if (id == 1010)
                            plus = 40;
                        else if (id == 1073)
                            plus = 50;
                        else if (id == 1074)
                            plus = 60;
                        else if (id == 1075)
                            plus = 70;
                        else if (id == 1076)
                            plus = 80;
                        else if (id == 1077)
                            plus = 90;
                        else if (id == 1078)
                            plus = 95;
                        else if (id == 1079)
                            plus = 100;

                        if (plus > 0) {
                            List<Integer> validIds = new ArrayList<>();
                            for (int i = 0; i < 120; i++) {
                                ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                                if (it != null
                                        && it.color == 4
                                        && it.typeEquip < 6
                                        && (it.clazz == 0 || it.clazz == p.clazz)) {
                                    validIds.add(i);
                                }
                            }
                            if (validIds.isEmpty()) {
                                Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị phù hợp!");
                                return false;
                            }
                            int id_add = validIds.get(Util.random(validIds.size()));
                            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);

                            // Tạo item thực tế
                            Item_wear it_ssr = new Item_wear();
                            it_ssr.setup_template_by_id(temp);
                            it_ssr.levelup = (byte) plus;

                            // Thêm 5 dòng chỉ số ngẫu nhiên
                            int[] randomStats = { 5, 6, 7, 8, 9 };
                            int colorBonus = temp.color * 15; // color 4 → +60
                            for (int j = 0; j < 5; j++) {
                                int optId = randomStats[Util.random(randomStats.length)];
                                int optVal = Util.random(100, 201) + colorBonus + (plus / 2);
                                it_ssr.option_item.add(new Option(optId, optVal));
                            }

                            p.item.add_item_bag3(it_ssr);
                            p.item.update_Inventory(-1, false);

                            // Hiển thị popup GiftBox
                            if (temp != null) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1; // Set to -1 to prevent send_gift from adding item automatically
                                gb.type = 3;
                                gb.name = temp.name + " +" + plus;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị SSR +" + plus, list, true);
                            }
                        }
                        break;
                    }
                    case 1082: // +10
                    case 1083: // +50
                    case 1084: { // +100
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
                            return false;
                        }
                        int plus = 0;
                        if (id == 1082)
                            plus = 10;
                        else if (id == 1083)
                            plus = 50;
                        else if (id == 1084)
                            plus = 100;

                        if (plus > 0) {
                            List<Integer> validIds = new ArrayList<>();
                            for (int i = 0; i < 144; i++) {
                                ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                                if (it != null
                                        && it.color == 8
                                        && it.typeEquip < 6
                                        && (it.clazz == 0 || it.clazz == p.clazz)) {
                                    validIds.add(i);
                                }
                            }
                            if (validIds.isEmpty()) {
                                Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị phù hợp!");
                                return false;
                            }
                            int id_add = validIds.get(Util.random(validIds.size()));
                            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);

                            // Tạo item thực tế
                            Item_wear it_ssr = new Item_wear();
                            it_ssr.setup_template_by_id(temp);
                            it_ssr.levelup = (byte) plus;
                            it_ssr.typelock = 1; // Khóa trang bị

                            int[] randomStats = { 5, 6, 7, 8, 9 };
                            int colorBonus = temp.color * 15;

                            // Cho vòng lặp chạy tối đa 10 lần
                            for (int j = 0; j < 10; j++) {
                                // Từ dòng thứ 6 (tức là j >= 5) trở đi mới bắt đầu xét tỉ lệ
                                if (j >= 5) {
                                    // Càng nhiều dòng tỉ lệ càng gắt: Dòng 6(50%), Dòng 7(30%), Dòng 8(15%), Dòng
                                    // 9(5%), Dòng 10(1%)
                                    int rate = (j == 5) ? 50 : (j == 6) ? 30 : (j == 7) ? 15 : (j == 8) ? 5 : 1;

                                    // Quay random 0-99, nếu lớn hơn hoặc bằng rate thì xịt -> Dừng vòng lặp luôn
                                    if (Util.random(100) >= rate) {
                                        break;
                                    }
                                }

                                // Nếu qua được ải kiểm tra (hoặc đang ở 5 dòng đầu) thì đập chỉ số vào
                                int optId = randomStats[Util.random(randomStats.length)];
                                int optVal = Util.random(200, 400) + colorBonus + (plus / 2);
                                it_ssr.option_item.add(new Option(optId, optVal));
                            }

                            p.item.add_item_bag3(it_ssr);
                            p.item.update_Inventory(-1, false);

                            // Hiển thị popup GiftBox
                            if (temp != null) {
                                List<GiftBox> list = new ArrayList<>();
                                GiftBox gb = new GiftBox();
                                gb.id = -1; // Set to -1 to prevent send_gift from adding item automatically
                                gb.type = 3;
                                gb.name = temp.name + " +" + plus;
                                gb.icon = temp.icon;
                                gb.num = 1;
                                gb.color = temp.color;
                                list.add(gb);
                                Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị GR +" + plus, list, true);
                            }
                        }
                        break;
                    }
                    case 1004: {
                        if (p.level < 20) {
                            Service.send_box_ThongBao_OK(p, "Yêu cầu đạt level 20 để mở rương");
                            return false;
                        }
                        List<GiftBox> listGift = new ArrayList<>();
                        int id_random = Util.random(647, 682);
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id_random);
                        GiftBox giftBox = new GiftBox();
                        giftBox.id = (short) id_random;
                        giftBox.type = 4;
                        giftBox.name = itemTemplate4.name;
                        giftBox.icon = itemTemplate4.icon;
                        giftBox.num = 1;
                        giftBox.color = 0;
                        listGift.add(giftBox);
                        Service.send_gift(p, 0, "Rương đá thần thoại", "Phần thưởng", listGift,
                                true);
                        break;
                    }

                    case 359: {
                        if (true) {
                            Service.send_box_ThongBao_OK(p, "Không thể bắn pháo hoa");
                            return false;
                        }
                        // Tạo danh sách quà cho người chơi chính 'p'
                        List<GiftBox> listGiftP = new ArrayList<>();
                        if (45 > Util.random(120)) {
                            int[] possibleIds = { 44, 45, 46, 50, 51, 52, 56, 57, 58, 62, 63, 64, 68, 69, 70, 74, 75,
                                    76 };
                            int randomIndex = Util.random(0, possibleIds.length - 1);
                            int id_random = possibleIds[randomIndex]; // Chọn ID ngẫu nhiên từ mảng
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id_random);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) id_random;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (30 > Util.random(120)) {
                            int[] possibleIds = { 4, 5, 9 };
                            int randomIndex = Util.random(0, possibleIds.length - 1);
                            int id_random = possibleIds[randomIndex]; // Chọn ID ngẫu nhiên từ mảng
                            GiftBox giftBox = new GiftBox();
                            ItemTemplate7 it_temp7 = ItemTemplate7.get_it_by_id(id_random);
                            giftBox.id = it_temp7.id;
                            giftBox.type = 7;
                            giftBox.name = it_temp7.name;
                            giftBox.icon = it_temp7.icon;
                            giftBox.num = Util.random(1, 3);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (5 > Util.random(1, 150)) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(158);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(1000, 10000);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        Service.send_eff(p, 23, 50);
                        Service.send_gift(p, 0, "Đốt pháo hoa", p.name, listGiftP, true);

                        // Gửi quà cho các người chơi khác 'p0'
                        for (Player p0 : p.map.players) {
                            if (p0 != p) {
                                List<GiftBox> listGiftP0 = new ArrayList<>();
                                if (45 > Util.random(150)) {
                                    int[] possibleIds = { 44, 45, 46, 50, 51, 52, 56, 57, 58, 62, 63, 64, 68, 69, 70,
                                            74, 75,
                                            76 };
                                    int randomIndex = Util.random(0, possibleIds.length - 1);
                                    int id_random = possibleIds[randomIndex]; // Chọn ID ngẫu nhiên từ mảng
                                    ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id_random);
                                    GiftBox giftBox = new GiftBox();
                                    giftBox.id = (short) id_random;
                                    giftBox.type = 4;
                                    giftBox.name = itemTemplate4.name;
                                    giftBox.icon = itemTemplate4.icon;
                                    giftBox.num = 1;
                                    giftBox.color = 0;
                                    listGiftP0.add(giftBox);
                                } else if (30 > Util.random(150)) {
                                    int[] possibleIds = { 4, 5, 9 };
                                    int randomIndex = Util.random(0, possibleIds.length - 1);
                                    int id_random = possibleIds[randomIndex]; // Chọn ID ngẫu nhiên từ mảng
                                    GiftBox giftBox = new GiftBox();
                                    ItemTemplate7 it_temp7 = ItemTemplate7.get_it_by_id(id_random);
                                    giftBox.id = it_temp7.id;
                                    giftBox.type = 7;
                                    giftBox.name = it_temp7.name;
                                    giftBox.icon = it_temp7.icon;
                                    giftBox.num = 1;
                                    giftBox.color = 0;
                                    listGiftP0.add(giftBox);
                                } else if (5 > Util.random(1, 150)) {
                                    ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(158);
                                    GiftBox giftBox = new GiftBox();
                                    giftBox.id = (short) itemTemplate4.id;
                                    giftBox.type = 4;
                                    giftBox.name = itemTemplate4.name;
                                    giftBox.icon = itemTemplate4.icon;
                                    giftBox.num = 1;
                                    giftBox.color = 0;
                                    listGiftP0.add(giftBox);
                                } else {
                                    ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                                    GiftBox giftBox = new GiftBox();
                                    giftBox.id = (short) itemTemplate4.id;
                                    giftBox.type = 4;
                                    giftBox.name = itemTemplate4.name;
                                    giftBox.icon = itemTemplate4.icon;
                                    giftBox.num = Util.random(1000, 10000);
                                    giftBox.color = 0;
                                    listGiftP0.add(giftBox);
                                }
                                Service.send_gift(p0, 0, "Đốt pháo hoa", p.name, listGiftP0, true);
                            }
                        }
                        break;
                    }

                    case 332: {
                        // Tạo danh sách quà cho người chơi chính 'p'
                        List<GiftBox> listGiftP = new ArrayList<>();
                        if (45 > Util.random(120)) {
                            int[] possibleIds = { 1022, 1023, 1024 };
                            int randomIndex = Util.random(0, possibleIds.length - 1);
                            int id_random = possibleIds[randomIndex]; // Chọn ID ngẫu nhiên từ mảng
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(id_random);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) id_random;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (5 > Util.random(120)) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1018);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (5 > Util.random(120)) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(172);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (5 > Util.random(120)) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1035);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = 1;
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else if (50 > Util.random(120)) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(500, 1000);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        } else {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) itemTemplate4.id;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = Util.random(500_000, 1_000_000);
                            giftBox.color = 0;
                            listGiftP.add(giftBox);
                        }
                        Service.send_eff(p, 20, 50);
                        Service.send_gift(p, 0, "Điều ước đèn trời", p.name, listGiftP, true);
                        break;
                    }

                    case 1007: {
                        String[] name = new String[] {
                                "Trang Bị 2",
                                "Trang Bị 3",
                                p.soluongmua == 0 ? "Số lượng mua: Mặc định" : "Số lượng mua: x" + p.soluongmua,
                                "Auto Nâng : " + p.solannang + " Lần",
                                p.is_show_gift ? "Đã Bật Hiển Thị Quà" : "Đã Tắt Hiển Thị Quà",
                                "Đệ tử"
                        };
                        MenuController.send_dynamic_menu(p, 9996, "Menu Tiện Ích", name, null);
                        return false;
                    }
                    case 1011:
                    case 1012:
                    case 1013:
                    case 1014:
                    case 1015:
                    case 1016:
                    case 1017: {
                        if (p.item.total_item_bag_by_id(4, 1011) < 1 || p.item.total_item_bag_by_id(4, 1012) < 1
                                || p.item.total_item_bag_by_id(4, 1013) < 1 || p.item.total_item_bag_by_id(4, 1014) < 1
                                || p.item.total_item_bag_by_id(4, 1015) < 1 || p.item.total_item_bag_by_id(4, 1016) < 1
                                || p.item.total_item_bag_by_id(4, 1017) < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Hãy sưu tập đủ 7 viên ngọc rồng từ 1 sao đến 7 sao để thực hiện điều ước hỗ trợ tân thủ");
                            return false;
                        }
                        p.item.remove_item47(4, 1011, 1);
                        p.item.remove_item47(4, 1012, 1);
                        p.item.remove_item47(4, 1013, 1);
                        p.item.remove_item47(4, 1014, 1);
                        p.item.remove_item47(4, 1015, 1);
                        p.item.remove_item47(4, 1016, 1);
                        p.item.remove_item47(4, 1017, 1);
                        p.item.update_Inventory(-1, false);

                        List<GiftBox> listGift = new ArrayList<>();
                        GiftBox.addGiftBox(listGift, 0, 4, 50_000_000); // Beri
                        GiftBox.addGiftBox(listGift, 1, 4, 5_000_000); // Ruby
                        GiftBox.addGiftBox(listGift, 2, 4, 1_000_000); // Extol

                        Service.send_eff(p, 20, 50); // Mở rồng thần effect? 20/50 are popular in the source
                        Service.send_gift(p, 0, "Quà Tân Thủ", "Bạn nhận được quà hỗ trợ tân thủ từ Rồng Thần",
                                listGift, true);
                        break;
                    }
                    case 1087:
                    case 1088:
                    case 1089:
                    case 1090:
                    case 1091:
                    case 1092:
                    case 1093: {
                        if (p.map.template.id != 6) {
                            Service.send_box_ThongBao_OK(p, "Chỉ có thể ước rồng thần tại Bến Tàu Fosha");
                            return false;
                        }
                        if (p.item.total_item_bag_by_id(4, 1087) < 1
                                || p.item.total_item_bag_by_id(4, 1088) < 1
                                || p.item.total_item_bag_by_id(4, 1089) < 1 || p.item.total_item_bag_by_id(4,
                                        1090) < 1
                                || p.item.total_item_bag_by_id(4, 1091) < 1 || p.item.total_item_bag_by_id(4,
                                        1092) < 1
                                || p.item.total_item_bag_by_id(4, 1093) < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Hãy sưu tập đủ 7 viên ngọc rồng vô cực từ 1 sao đến 7 sao để thực hiện điều ước");
                            return false;
                        }
                        Service.send_box_yesno(p, 103, "Rồng Thần",
                                "Bạn có muốn triệu hồi Rồng Thần để thực hiện điều ước không?",
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                        return false;
                    }

                    case 271: {
                        Service.input_text(p, 14, "Đổi tên", new String[] { "Nhập tên mới" });
                        return false;
                    }

                    case 1022: { // Haki Quan Sát
                        if (p.get_skill_Lv_RQ(5001) >= 10) {
                            Service.send_box_ThongBao_OK(p, "Haki Quan Sát đã đạt cấp tối đa (10)");
                            return false;
                        }
                        p.update_skill_exp(5001, Util.random(2000, 5000));
                        break;
                    }

                    case 1023: { // Haki Bá Vương
                        if (p.get_skill_Lv_RQ(5003) >= 10) {
                            Service.send_box_ThongBao_OK(p, "Haki Bá Vương đã đạt cấp tối đa (10)");
                            return false;
                        }
                        p.update_skill_exp(5003, Util.random(2000, 5000));
                        break;
                    }

                    case 683:
                    case 1033:
                    case 1034: {
                        Service.send_box_ThongBao_OK(p, "Bạn hãy giữ lại nó, admin sẽ thêm chức năng cho nó sau");
                        return false;
                    }

                    case 1035: { // Rương Dial Vua
                        int total = p.item.total_item_bag_by_id(4, 1035);
                        if (total > 0) {
                            Service.input_text(p, 1035, "Rương Dial Vua (" + total + " cái)",
                                    new String[] { "Nhập số lượng muốn mở" });
                        }
                        return false;
                    }

                    case 1024: { // Haki Vũ Trang
                        if (p.get_skill_Lv_RQ(5002) >= 10) {
                            Service.send_box_ThongBao_OK(p, "Haki Vũ Trang đã đạt cấp tối đa (10)");
                            return false;
                        }
                        p.update_skill_exp(5002, Util.random(2000, 5000));
                        break;
                    }

                    case 171: { // Hộp Quà 8.3
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                            return false;
                        }
                        if (Util.random(100) < 20) { // 20%
                            p.item.add_item_bag47(4, 427, 1);
                            p.item.update_Inventory(-1, false);
                            Service.send_box_ThongBao_OK(p, "Bạn nhận được Trái Bóng Tối từ Hộp Quà 8.3!");
                        } else {
                            Service.send_box_ThongBao_OK(p,
                                    "Rất tiếc, bạn không nhận được Trái Bóng Tối lần này. Chúc bạn may mắn lần sau!");
                        }
                        break;
                    }

                    case 1101: { // Vé Ủy thác Extol (Thường)
                        if (p.map != null && p.map.template != null && map.Map.map_cant_save_site(p.map.template.id)) {
                            Service.send_box_ThongBao_OK(p, "Không thể kích hoạt Ủy thác tại khu vực này!");
                            used = false;
                            break;
                        }
                        int type = 1;
                        long currentTime = System.currentTimeMillis();
                        if (p.offlineTrainingType != type && p.offlineTrainingType != 0) {
                            p.offlineTrainingEndTime = currentTime + Player.OFFLINE_TRAINING_DURATION;
                            Service.send_box_ThongBao_OK(p, "Bạn đã đổi sang vé Thường. Thời gian cũ đã bị hủy.");
                        } else {
                            if (p.offlineTrainingEndTime < currentTime) {
                                p.offlineTrainingEndTime = currentTime + Player.OFFLINE_TRAINING_DURATION;
                            } else {
                                p.offlineTrainingEndTime += Player.OFFLINE_TRAINING_DURATION;
                            }
                        }
                        p.offlineTrainingType = type;
                        p.offlineX = (short) p.x;
                        p.offlineY = (short) p.y;
                        p.isOfflineTraining = true;
                        Player.flush(p, true);
                        String timeRemain = core.Util.get_time_str_by_sec2(p.offlineTrainingEndTime - currentTime);
                        Service.send_box_ThongBao_OK(p, "Kích hoạt Ủy thác Extol (Thường) thành công!\n"
                                + "Thời gian còn lại: " + timeRemain + "\nTốc độ: 0.3s.");
                        used = true;
                        break;
                    }

                    case 1102: { // Vé Ủy thác Coin (VIP)
                        if (p.map != null && p.map.template != null && map.Map.map_cant_save_site(p.map.template.id)) {
                            Service.send_box_ThongBao_OK(p, "Không thể kích hoạt Ủy thác tại khu vực này!");
                            used = false;
                            break;
                        }
                        int type = 2;
                        long currentTime = System.currentTimeMillis();
                        if (p.offlineTrainingType != type && p.isOfflineTraining) {
                            p.offlineTrainingEndTime = currentTime + Player.OFFLINE_TRAINING_DURATION;
                        } else {
                            if (p.offlineTrainingEndTime < currentTime) {
                                p.offlineTrainingEndTime = currentTime + Player.OFFLINE_TRAINING_DURATION;
                            } else {
                                p.offlineTrainingEndTime += Player.OFFLINE_TRAINING_DURATION;
                            }
                        }
                        p.offlineTrainingType = type;
                        p.offlineX = (short) p.x;
                        p.offlineY = (short) p.y;
                        p.isOfflineTraining = true;
                        Player.flush(p, true);
                        String timeRemain = core.Util.get_time_str_by_sec2(p.offlineTrainingEndTime - currentTime);
                        Service.send_box_ThongBao_OK(p, "Kích hoạt Ủy thác VIP (Siêu tốc) thành công!\n"
                                + "Thời gian còn lại: " + timeRemain
                                + "\nƯu đãi VIP: Tốc độ 0.1s + Tự quay lại Map khi chết.");
                        used = true;
                        break;
                    }

                    default: {
                        Service.send_box_ThongBao_OK(p, "Hiện tại "
                                + ItemTemplate4.get_item_name(id) + " chưa sử dụng được");
                        used = false;
                        break;
                    }
                }
            }
        } else {
            Service.send_box_ThongBao_OK(p, "Vật phẩm lỗi, hãy báo cho admin");
            used = false;
        }
        return used;
    }

    private static void open_taq_random(Player p, int id, String name1, String name2)
            throws IOException {
        Message m = new Message(-34);
        m.writer().writeByte(21);
        m.writer().writeShort(-1);
        m.writer().writeUTF(name1);
        m.writer().writeUTF(name2);
        m.writer().writeByte(1);
        ItemTemplate4 it_temp = ItemTemplate4.get_it_by_id(id);
        m.writer().writeByte(4);
        m.writer().writeUTF(it_temp.name);
        m.writer().writeShort(it_temp.icon);
        m.writer().writeInt(1);
        m.writer().writeByte(0);
        p.addmsg(m);
        m.cleanup();
    }

    private static boolean addDanhHieuIfNotOwned(Player p, int... ids) {
        try {
            for (MyDanhHieu dh : p.danh_hieu) {
                for (int id : ids) {
                    if (dh.id == id) {
                        Service.send_box_ThongBao_OK(p, "Bạn đã sở hữu danh hiệu này rồi");
                        return false;
                    }
                }
            }
            for (int id : ids) {
                p.addDanhHieu(id);
            }
            return true;
        } catch (IOException e) {
            GameLogger.error("UseItem.addDanhHieuIfNotOwned: Error adding titles for " + p.name, e);
            return false;
        }
    }

    private static void use_item_3(Player p, int id) throws IOException {
        if (p.use_item_3 == -1) {
            if (p.item.able_bag() < 1) {
                Service.send_box_ThongBao_OK(p,
                        "Cần 1 ô trống trong hành trang để thực hiện");
                return;
            }
            Item_wear it = p.item.bag3[id];
            if (it != null) {
                if (check_it_can_wear(it.template.typeEquip)) {
                    // Kiểm tra nếu là Rương SGR giả trang bị
                    if (it.template.id == 1105) {
                        handleOpenSGRChest(p, it);
                        return;
                    }
                    p.use_item_3 = id;
                    if (p.myDisciple != null) {
                        // Chuyển sang Dynamic Menu để ổn định gói tin và dễ lựa chọn hơn
                        MenuController.send_dynamic_menu(p, 9999, "Trang bị cho ai?",
                                new String[] { "Bản thân", "Đệ tử" });
                    } else {
                        // Luồng cũ khi không có đệ tử
                        if (it.typelock == 1 || it.valueKichAn == 12) {
                            p.wear_item(it);
                        } else {
                            Service.send_box_yesno(p, 0, "Thông báo",
                                    "Khi trang bị lên người vật phẩm "
                                            + ItemTemplate3.get_it_by_id(it.template.id).name
                                            + " sẽ chuyển sang trạng thái khóa không thể giao dịch. "
                                            + "Bạn có muốn trang bị?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                        }
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Chưa có chức năng");
                }
            }
        }
    }

    private static void handleOpenSGRChest(Player p, Item_wear itChest) throws IOException {
        if (p.item.able_bag() < 1) {
            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
            return;
        }

        List<Integer> validIds = new ArrayList<>();
        // Quét dải ID đồ mới từ 144 đến 153
        for (int i = 144; i <= 153; i++) {
            ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
            if (it != null && it.color == 8 && it.typeEquip < 6 && (it.clazz == 0 || it.clazz == p.clazz)) {
                validIds.add(i);
            }
        }

        if (validIds.isEmpty()) {
            Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị phù hợp!");
            return;
        }

        int id_add = validIds.get(Util.random(validIds.size()));
        ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);

        // Lấy tỉ lệ buff từ cấp độ của Rương
        int boostRate = itChest.levelup;

        // Xóa Rương
        p.item.bag3[itChest.index] = null;

        // Tạo item thực tế
        Item_wear it_ssr = new Item_wear();
        it_ssr.setup_template_by_id(temp);
        it_ssr.typelock = 1;
        it_ssr.levelup = 0;

        int[] randomStats = { 5, 6, 7, 8, 9 };
        int colorBonus = temp.color * 15;

        for (int j = 0; j < 20; j++) {
            if (j >= 6) { // Tăng số dòng cơ bản lên 6 dòng
                int finalRate = 0;

                if (j < 10) {
                    // GIỮ NGUYÊN LOGIC CŨ (Dòng 7 - 10)
                    int baseRate = (j == 6) ? 80 : (j == 7) ? 60 : (j == 8) ? 40 : 20;
                    finalRate = baseRate + boostRate;
                } else {
                    // LOGIC MỚI (Dòng 11 - 20): Càng cao càng khó
                    // Công thức: Bắt đầu từ 15% và giảm dần theo mỗi dòng.
                    // Bạn có thể chỉnh số 15 (tỉ lệ khởi đầu) hoặc số 1.5 (độ dốc) để cân bằng lại.
                    double difficultRate = 15.0 - ((j - 10) * 1.5);

                    // Đảm bảo tỉ lệ không bị âm, tối thiểu là 1% cho các dòng cuối
                    if (difficultRate < 1)
                        difficultRate = 1;

                    // boostRate vẫn có tác dụng nhưng nhẹ hơn ở các dòng cao
                    finalRate = (int) difficultRate + (boostRate / 2);
                }

                // Kiểm tra nhân phẩm
                if (Util.random(100) >= finalRate) {
                    break;
                }
            }
            int optId = randomStats[Util.random(randomStats.length)];
            int optVal = Util.random(400, 600) + colorBonus;
            it_ssr.option_item.add(new Option(optId, optVal));
        }

        p.item.add_item_bag3(it_ssr);
        p.item.update_Inventory(-1, false);

        // Hiển thị GiftBox
        List<GiftBox> list = new ArrayList<>();
        GiftBox gb = new GiftBox();
        gb.id = -1;
        gb.type = 3;
        gb.name = temp.name;
        gb.icon = temp.icon;
        gb.num = 1;
        gb.color = temp.color;
        list.add(gb);
        Service.send_gift(p, 1, "Mở Khóa Rương", "Rương Trang Bị Mới", list, true);
    }

    public static boolean check_it_can_wear(byte type) {
        return type >= 0 && type <= 5 || type == 7 || type == 6;
    }

    public static void use_item_potion(Player p, int id) throws IOException {
        int numInBag = p.item.total_item_bag_by_id(4, id);
        if (numInBag > 0) {
            if (use_item_4(p, id)) {
                if (id != 271) {
                    p.item.remove_item47(4, id, 1);
                }
                Message m2 = new Message(-13);
                m2.writer().writeShort(id);
                m2.writer().writeShort(p.item.total_item_bag_by_id(4, id));
                p.addmsg(m2);
                m2.cleanup();
                //
                p.item.update_Inventory(-1, false);
            }
        }
    }
}
