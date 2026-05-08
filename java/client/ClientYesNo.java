package client;

import activities.*;
import core.Manager;
import core.MenuController;
import core.Service;
import core.Util;
import core.GameLogger;
import io.Message;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import tool.ItemDropTool;
import map.Map;
import map.Mob;
import map.Npc;
import map.Vgo;
import map.f;
import template.*;

public class ClientYesNo {

    private static final String LOG_FOLDER = "market_log";

    public class LogUtil {

        private static final String LOG_FOLDER = "market_log";

        public static void logTransaction(Player p, PotionMarket item, boolean isBeri) {
            File logDir = new File(LOG_FOLDER);
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            File logFile = new File(logDir, date + ".txt");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write("Transaction Date: " + date);
                writer.newLine();
                writer.write("Transaction Time: " + time);
                writer.newLine();

                // Thông tin người chơi
                writer.write("Player: " + p.name);
                writer.newLine();
                writer.write("Player ID: " + p.id);
                writer.newLine();

                // Nếu giao dịch là Beri
                if (isBeri) {
                    writer.write("Transaction Type: Beri Purchase");
                    writer.newLine();
                    writer.write("Amount: " + item.quant + "M Beri");
                    writer.newLine();
                } else {
                    // Kiểm tra loại ItemTemplate
                    if (item.category == 4) { // ItemTemplate4
                        writer.write("Transaction Type: Item Purchase (Category 4 - ItemTemplate4)");
                        writer.newLine();
                        writer.write("Item Name: " + ItemTemplate4.get_item_name(item.id));
                        writer.newLine();
                    } else { // ItemTemplate7
                        writer.write("Transaction Type: Item Purchase (Category 7 - ItemTemplate7)");
                        writer.newLine();
                        writer.write("Item Name: " + ItemTemplate7.get_item_name(item.id));
                        writer.newLine();
                    }
                    writer.write("Quantity: " + item.quant);
                    writer.newLine();
                }

                // Giá giao dịch
                writer.write("Price: " + Util.number_format(item.price_market) + " Extol");
                writer.newLine();
                writer.write("Seller: " + item.seller);
                writer.newLine();

                writer.write("--------------------------------------------------");
                writer.newLine();

            } catch (IOException e) {
                GameLogger.warn("ClientYesNo.LogUtil: Error writing transaction log (beri/item)", e);
            }
        }

        public static void logTransaction(Player p, ItemMarket item) {
            File logDir = new File(LOG_FOLDER);
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            File logFile = new File(logDir, date + ".txt");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write("Transaction Date: " + date);
                writer.newLine();
                writer.write("Transaction Time: " + time);
                writer.newLine();

                // Thông tin người chơi
                writer.write("Player: " + p.name);
                writer.newLine();
                writer.write("Player ID: " + p.id);
                writer.newLine();

                // Giao dịch quần áo (case 25)
                writer.write("Transaction Type: Equipment Purchase");
                writer.newLine();
                writer.write("Item Name: " + item.template.name);
                writer.newLine();
                writer.write("Item ID: " + item.template.id);
                writer.newLine();
                writer.write("Quantity: 1"); // Quần áo luôn có số lượng là 1
                writer.newLine();

                // Giá giao dịch
                writer.write("Price: " + Util.number_format(item.price_market) + " Extol");
                writer.newLine();
                writer.write("Seller: " + item.seller);
                writer.newLine();

                writer.write("--------------------------------------------------");
                writer.newLine();

            } catch (IOException e) {
                GameLogger.warn("ClientYesNo.LogUtil: Error writing equipment log", e);
            }
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        short id = m2.reader().readShort();
        byte value = m2.reader().readByte();
        // core.GameLogger.info("id " + id);
        // core.GameLogger.info("value " + value);

        if (id == 1096) {
            if (value >= 0 && value <= 4) {
                byte newClazz = (byte) (value + 1);

                if (newClazz == p.clazz) {
                    Service.send_box_ThongBao_OK(p, "Bạn đã ở môn phái này rồi!");
                    return;
                }

                if (p.item.total_item_bag_by_id(4, 1096) < 1) {
                    Service.send_box_ThongBao_OK(p, "Không tìm thấy Vé Chuyển Chức!");
                    return;
                }

                // BACKUP DATA FOR ROLLBACK
                byte backupClazz = p.clazz;
                Item_wear[] backupItBody = new Item_wear[p.item.it_body.length];
                for (int i = 0; i < p.item.it_body.length; i++) {
                    if (p.item.it_body[i] != null) {
                        backupItBody[i] = p.item.it_body[i].clone_obj();
                    }
                }

                try {
                    // Đổi class trang bị đang mặc (chỉ giữ lại option, type, levelup)
                    for (int i = 0; i < p.item.it_body.length; i++) {
                        Item_wear it = p.item.it_body[i];
                        if (it != null && it.template != null && it.template.clazz != 0
                                && it.template.clazz != newClazz) {
                            ItemTemplate3 newTemp = null;
                            short bestLevelDiff = 999;
                            boolean isGR = it.template.id >= 120 && it.template.id <= 143;

                            for (ItemTemplate3 temp : ItemTemplate3.ENTRYS) {
                                boolean tempIsGR = temp.id >= 120 && temp.id <= 143;
                                // Phải KHỚP Hệ phái, Loại trang bị, MÀU SẮC, và ĐẶC TÍNH GR (tránh lùi SSR)
                                if (temp.typeEquip == it.template.typeEquip && temp.clazz == newClazz
                                        && temp.color == it.template.color && isGR == tempIsGR) {
                                    short diff = (short) Math.abs(temp.level - it.template.level);
                                    if (temp.level <= it.template.level && diff < bestLevelDiff) {
                                        newTemp = temp;
                                        bestLevelDiff = diff;
                                    }
                                }
                            }
                            if (newTemp == null) {
                                for (ItemTemplate3 temp : ItemTemplate3.ENTRYS) {
                                    boolean tempIsGR = temp.id >= 120 && temp.id <= 143;
                                    if (temp.typeEquip == it.template.typeEquip && temp.clazz == newClazz
                                            && temp.color == it.template.color && isGR == tempIsGR) {
                                        short diff = (short) Math.abs(temp.level - it.template.level);
                                        if (diff < bestLevelDiff) {
                                            newTemp = temp;
                                            bestLevelDiff = diff;
                                        }
                                    }
                                }
                            }
                            // Fallback cấp thấp nhất nếu nhỡ Color có sai biệt Data
                            if (newTemp == null) {
                                for (ItemTemplate3 temp : ItemTemplate3.ENTRYS) {
                                    if (temp.typeEquip == it.template.typeEquip && temp.clazz == newClazz) {
                                        short diff = (short) Math.abs(temp.level - it.template.level);
                                        if (diff < bestLevelDiff) {
                                            newTemp = temp;
                                            bestLevelDiff = diff;
                                        }
                                    }
                                }
                            }
                            if (newTemp != null) {
                                it.template = newTemp;
                            }
                        }
                    }

                    // Set clazz
                    p.clazz = newClazz;
                    // p.reset_point(0); // Tẩy tiềm năng 1
                    // p.reset_point(1); // Tẩy tiềm năng 2

                    // Bảng Icon chuẩn để ánh xạ kỹ năng (Dùng Icon là tuyệt đối chính xác, không bị
                    // lỗi mã hóa)
                    int[][] SKILL_ICONS = {
                            { 0, 3, 6, 9, 12 }, // Slot 1
                            { 1, 4, 7, 10, 13 }, // Slot 2
                            { 2, 5, 8, 11, 14 }, // Slot 3
                            { 41, 42, 43, 44, 45 } // Slot 4 (Buff)
                    };

                    // core.GameLogger.info("--- DEBUG SKILL SWAP (Player: " + p.name + ") ---");
                    // Đổi kỹ năng cơ bản theo đặc tả Phái mới (Giữ nguyên kinh nghiệm, cấp độ)
                    for (int i = 0; i < p.skill_point.size(); i++) {
                        template.Skill_info sk = p.skill_point.get(i);
                        int currentIcon = sk.temp.idIcon;
                        int foundSlot = -1;

                        // 1. Xác định Slot dựa trên Icon (Tìm kiếm toàn bộ bảng để hỗ trợ cả nhân vật
                        // đang bị lệch phái)
                        for (int slot = 0; slot < SKILL_ICONS.length; slot++) {
                            for (int cl = 0; cl < SKILL_ICONS[slot].length; cl++) {
                                if (currentIcon == SKILL_ICONS[slot][cl]) {
                                    foundSlot = slot;
                                    break;
                                }
                            }
                            if (foundSlot != -1) {
                                break;
                            }
                        }

                        if (foundSlot != -1) {
                            int targetIcon = SKILL_ICONS[foundSlot][newClazz - 1];
                            template.Skill_Template newTemp = null;
                            // 2. Tìm chiêu mới cùng Icon mục tiêu và cùng cấp độ (Lv_RQ)
                            for (template.Skill_Template t : template.Skill_Template.ENTRYS) {
                                if (t.idIcon == targetIcon && t.Lv_RQ == sk.temp.Lv_RQ) {
                                    newTemp = t;
                                    break;
                                }
                            }

                            if (newTemp != null) {
                                // System.out.println("[Swap Success] Slot " + (foundSlot + 1) + ": " +
                                // sk.temp.name
                                // + " (Icon " + currentIcon + ") -> " + newTemp.name + " (Icon " + targetIcon
                                // + ")");
                                sk.temp = newTemp;
                            }
                            // else {
                            // System.err.println("[Swap Fail] Slot " + (foundSlot + 1) + ": Not found
                            // template for Icon "
                            // + targetIcon + " Lv_RQ " + sk.temp.Lv_RQ);
                            // }
                        } // else {
                          // System.out.println("[Skip] Skill " + sk.temp.name + " (Icon " + currentIcon
                          // + ") is not a basic class skill.");
                          // }
                    }

                    // CẬP NHẬT TRONG BĂNG HẢI TẶC (Nếu có clan)
                    if (p.clan != null) {
                        for (int i = 0; i < p.clan.members.size(); i++) {
                            if (p.clan.members.get(i).name.equals(p.name)) {
                                p.clan.members.get(i).clazz = newClazz;
                                break;
                            }
                        }
                    }

                    // Trừ item CHỈ KHI MỌI THỨ THÀNH CÔNG (trước khi Save)
                    p.item.remove_item47(4, 1096, 1);

                    // Cập nhật Database & Client
                    Player.flush(p, true);

                    p.setin4();
                    Service.Main_char_Info(p);
                    p.update_info_to_all();
                    p.item.update_Inventory(-1, false);
                    p.send_skill();

                    Service.send_box_ThongBao_OK(p,
                            "Chúc mừng bạn đã chuyển đổi thành công sang môn phái mới!\n Kết nối sẽ được ngắt để hoàn thành đồng bộ.");

                    // Delay nhẹ rồi kick để client nhận được thông báo
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            if (p.conn != null) {
                                p.conn.disconnect();
                            }
                        } catch (Exception e) {
                            GameLogger.warn("ClientYesNo.process: Disconnect thread error for " + p.name, e);
                        }
                    }).start();

                } catch (Exception e) {
                    // ROLLBACK TRƯỜNG HỢP LỖI
                    GameLogger.error("ClientYesNo.process: Error during class change for " + p.name + ", rolled back",
                            e);
                    p.clazz = backupClazz;
                    for (int i = 0; i < backupItBody.length; i++) {
                        p.item.it_body[i] = backupItBody[i];
                    }
                    Service.send_box_ThongBao_OK(p,
                            "Có lỗi từ hệ thống khi thao tác! Dữ liệu đã được bảo toàn (Rollback) và bạn không bị mất item.");
                }
            }
            return;
        }
        if (id == 1097) {
            if (value == 0) { // Đồng ý
                if (p.item.total_item_bag_by_id(4, 1097) < 1) {
                    Service.send_box_ThongBao_OK(p, "Không tìm thấy Lõi Đệ Tử!");
                    return;
                }

                try {
                    // 1. Loại bỏ đệ tử cũ khỏi map nếu có
                    if (p.myDisciple != null) {
                        try {
                            Message m_out = new Message(3);
                            m_out.writer().writeShort(p.myDisciple.index_map);
                            p.map.send_msg_all_p(m_out, null, true);
                            m_out.cleanup();
                        } catch (Exception e) {
                            GameLogger.warn(
                                    "ClientYesNo.process: Error sending exit msg for old disciple for " + p.name, e);
                        }
                        p.map.leave_map(p.myDisciple, 0);
                    }

                    // 2. Khởi tạo đệ tử mới ngẫu nhiên
                    p.myDisciple = new Disciple(p);
                    p.myDisciple.x = (short) (p.x + 30);
                    p.myDisciple.y = p.y;
                    p.myDisciple.map = p.map;
                    p.myDisciple.status = 0; // Đi theo

                    // 3. Đưa đệ tử vào danh sách map (để AI loop xử lý) - Chỉ nạp nếu không phải
                    // map cấm
                    if (p.myDisciple.isMapCamDeTu(p.map)) {
                        p.myDisciple.status = 2; // Tự động cất khi ở map cấm
                    } else {
                        synchronized (p.map.players) {
                            if (!p.map.players.contains(p.myDisciple)) {
                                p.map.players.add(p.myDisciple);
                            }
                        }
                    }

                    // 4. Gửi gói tin xuất hiện cho mọi người trong map
                    Message m_appear = new Message(1);
                    m_appear.writer().writeByte(0); // là người
                    m_appear.writer().writeShort(p.myDisciple.index_map);
                    m_appear.writer().writeShort(p.myDisciple.x);
                    m_appear.writer().writeShort(p.myDisciple.y);
                    p.map.send_msg_all_p(m_appear, null, true);
                    m_appear.cleanup();

                    // 5. Đồng bộ thông tin chi tiết (phái, trang bị, hair...)
                    synchronized (p.map.players) {
                        for (Player p0 : p.map.players) {
                            if (p0 != null && p0.conn != null) {
                                p.map.send_char_in4_inmap(p0, p.myDisciple.index_map);
                            }
                        }
                    }

                    // 6. Tiêu thụ vật phẩm 1097
                    p.item.remove_item47(4, 1097, 1);
                    p.item.update_Inventory(4, false);

                    // 7. Lưu dữ liệu và thông báo
                    Player.flush(p, false);
                    Service.send_box_ThongBao_OK(p,
                            "Chúc mừng! Bạn đã nhận được đệ tử phái " + p.myDisciple.clazz + "!");
                    p.myDisciple.chat("Con chào sư phụ!");

                } catch (Exception e) {
                    GameLogger.error("ClientYesNo.process: Error creating disciple for " + p.name, e);
                    Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra khi tạo đệ tử.");
                }
            }
            return;
        }
        if (id == 38 && p.data_yesno != null && p.data_yesno.length == 1) {
            List<Skill_info> name_skill = new ArrayList<>();
            for (int i = 0; i < p.skill_point.size(); i++) {
                if (p.skill_point.get(i).temp.ID >= 2000
                        && p.skill_point.get(i).temp.typeSkill == 1) {
                    name_skill.add(p.skill_point.get(i));
                }
            }
            if (name_skill.size() > 0 && value < name_skill.size()) {
                Skill_info sk_selsect = name_skill.get(value);
                if (sk_selsect.lvdevil > 4) {
                    Service.send_box_ThongBao_OK(p, "Cấp ác quỷ đã đạt tối đa");
                    p.data_yesno = null;
                    p.map_tele = null;
                    return;
                }
                int exp_devil = 0;
                if (p.item.bag3[p.data_yesno[0]] != null) {
                    if (p.item.bag3[p.data_yesno[0]].option_item.size() > 0) {
                        exp_devil += p.item.bag3[p.data_yesno[0]].option_item.get(0).getParam();
                        p.item.remove_item_wear(p.item.bag3[p.data_yesno[0]]);
                        p.item.update_Inventory(-1, false);
                    }
                }
                if (exp_devil > 0) {
                    sk_selsect.update_exp_devil(exp_devil);
                    p.send_skill();
                    p.update_info_to_all();
                    Service.send_box_ThongBao_OK(p, "Sử dụng năng lượng ác quỷ cho "
                            + sk_selsect.temp.name + " thành công");
                }
            }
            p.data_yesno = null;
            p.map_tele = null;
            return;
        }
        if (id == 39 && value < 4) {
            int vang_total = 0;
            for (int i = 0; i < p.item.bag3.length; i++) {
                if (p.item.bag3[i] != null && p.item.bag3[i].template.typeEquip < 6
                        && p.item.bag3[i].typelock != 1 && p.item.bag3[i].levelup == 0
                        && p.item.bag3[i].template.color <= value) {
                    int vang_recive = 30 + (2 * p.item.bag3[i].template.color
                            + (p.item.bag3[i].template.level / 10) + 1)
                            * DataTemplate.TabInventory_ItemSell[0];
                    if (vang_recive > DataTemplate.TabInventory_ItemSell[1]) {
                        vang_recive = DataTemplate.TabInventory_ItemSell[1];
                    }
                    vang_total += vang_recive;
                    //
                    p.item.add_item_save(p.item.bag3[i]);
                    p.item.bag3[i] = null;
                }
            }
            if (vang_total > 0 && vang_total < 20_000) {
                p.update_vang(vang_total);
                p.update_money();
            } else if (vang_total >= 20_000) {
                core.GameLogger.info("vang ban do " + vang_total);
            }
            p.item.update_Inventory(-1, false);
            p.data_yesno = null;
            p.map_tele = null;
            return;
        }

        process(p, (int) id, value, m2);
    }

    public static void process(Player p, int id, byte value, Message m2) throws IOException {
        if (id == 9998) { // Xác nhận mặc đồ từ rương tinh tú (Hồi đáp từ Menu)
            if (value == 2 || value == 0) { // Đồng ý (Chấp nhận cả giá trị gán và index nút)
                if (p.data_yesno != null && p.data_yesno.length == 1) {
                    int index_bag = p.data_yesno[0];
                    if (index_bag != -1 && index_bag < p.item.bag3.length) {
                        Item_wear it = p.item.bag3[index_bag];
                        if (it != null) {
                            p.wear_item(it);
                        }
                    }
                }
            }
            p.data_yesno = null;
            p.use_item_3 = -1;
            return;
        }

        if (id == butoi.ButOi.YES_NO_ID) {
            butoi.ButOi.processYesNo(p, value);
            return;
        }
        if (id >= 200 && id <= 206) {
            taixiu.TaiXiuManager.xuLyYesNo(p, id, (int) value);
            return;
        }
        if (id == taixiu.BauCuaManager.YESNO_MENU || id == taixiu.BauCuaManager.YESNO_XAC_NHAN
                || id == taixiu.BauCuaManager.YESNO_HUONG_DAN) {
            taixiu.BauCuaManager.xuLyYesNo(p, id, value);
            return;
        }
        if (id >= 210 && id <= 212) {
            taixiu.BlackjackManager.handleYesNo(p, value, id);
            return;
        }
        if (id == taixiu.SlotMachineManager.MENU_ID || id == taixiu.SlotMachineManager.RESULT_MENU_ID) {
            taixiu.SlotMachineManager.handleYesNo(p, (int) value, id);
            return;
        }
        if (id == 3333) {
            if (value == 5) {
                activities.Mastery2.reset(p);
            } else {
                activities.Mastery2.requestAllocation(p, value);
            }
            return;
        }
        if (id == 3335) {
            if (value == 0) { // Ghép Tiên
                activities.Mastery2.craftBoi(p, false);
            } else if (value == 1) { // Ghép Ma
                activities.Mastery2.craftBoi(p, true);
            } else if (value == 2) { // Ghép nhiều Tiên
                Service.input_text(p, 3336, "Ghép Bội Tu Tiên",
                        new String[] { "Nhập số lượng (10 Bội thường = 1 lần ghép):" });
            } else if (value == 3) { // Ghép nhiều Ma
                Service.input_text(p, 3337, "Ghép Bội Tu Ma",
                        new String[] { "Nhập số lượng (10 Bội thường = 1 lần ghép):" });
            }
            return;
        }
        if (id == 78) {
            if (value == 0) {
                ItemDropTool.handleUpgradeTuTien(p, false);
            } else if (value == 1) {
                ItemDropTool.handleUpgradeTuTien(p, true);
            } else if (value == 2) {
                // Ghép Bội
                String[] mGhep = { "Bội Tu Tiên C2", "Bội Tu Ma C2", "Ghép nhiều Tu Tiên", "Ghép nhiều Tu Ma", "Đóng" };
                Service.send_box_yesno(p, 3335, "Ghép Bội",
                        "Bạn muốn ghép loại Bội nào?\n(Yêu cầu: 10 Bội thường, Tỉ lệ: 50%)", mGhep,
                        new byte[] { -1, -1, -1, -1, 1 });
            }
            return;
        }
        if (value == 0) { // ok
            if (id == 881) {
                ItemDropTool.handleCraftEquipGR(p);
                return;
            }
            if (id == 882) {
                ItemDropTool.handleCraftDialGR(p);
                return;
            }
            if (id == 883) {
                ItemDropTool.handleCraftTimGR(p);
                return;
            }
            switch (id) {
                case 1106: { // Xác nhận đổi trang bị GR lấy Rương SGR
                    if (p.data_yesno != null && p.data_yesno.length > 0) {
                        int realIndex = p.data_yesno[0];
                        Item_wear it = p.item.bag3[realIndex];
                        if (it != null) {
                            // Kiểm tra lại nguyên liệu lần cuối
                            if (p.item.total_item_bag_by_id(4, 514) >= 20 && 
                                p.item.total_item_bag_by_id(4, 515) >= 20 && 
                                p.item.total_item_bag_by_id(4, 516) >= 20 &&
                                p.item.total_item_bag_by_id(4, 1106) >= 1) {
                                
                                // Lưu cấp độ đồ cũ để làm buff tỉ lệ ra dòng (0-100)
                                int oldLevel = it.levelup;
                                
                                // Xóa nguyên liệu
                                p.item.remove_item47(4, 514, 20);
                                p.item.remove_item47(4, 515, 20);
                                p.item.remove_item47(4, 516, 20);
                                p.item.remove_item47(4, 1106, 1);
                                
                                // Xóa trang bị GR cũ
                                p.item.bag3[realIndex] = null;
                                
                                // Trao Rương SGR (1105) - Dạng trang bị giả để lưu levelup
                                template.ItemTemplate3 tempChest = template.ItemTemplate3.get_it_by_id(1105);
                                if (tempChest != null) {
                                    Item_wear it_chest = new Item_wear();
                                    it_chest.setup_template_by_id(tempChest);
                                    it_chest.levelup = (byte) oldLevel; // Lưu cấp độ vào rương
                                    p.item.add_item_bag3(it_chest);
                                }
                                
                                p.item.update_Inventory(-1, false);
                                Service.send_box_ThongBao_OK(p, "Đổi thành công! Bạn nhận được Rương SGR (Chứa cấp độ buff +" + oldLevel + ")");
                            } else {
                                Service.send_box_ThongBao_OK(p, "Bạn không đủ nguyên liệu (Đá hoặc Vé nâng cấp)!");
                            }
                        }
                    }
                    return;
                }
                case 121: { // Mua Danh Hiệu (Extol)
                    synchronized (p) {
                        ShopExtol temp_shop = ShopExtol.get_temp_id(p.id_select_temp);
                        if (temp_shop != null) {
                            long price = temp_shop.price;
                            if (p.get_vnd() < price) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không đủ " + Util.number_format(price) + " Extol");
                                return;
                            }
                            // Kiểm tra xem đã có danh hiệu này chưa
                            for (MyDanhHieu myDH : p.danh_hieu) {
                                if (myDH.id == temp_shop.id_title) {
                                    Service.send_box_ThongBao_OK(p, "Bạn đã sở hữu danh hiệu này rồi.");
                                    return;
                                }
                            }

                            // Thực hiện mua
                            p.update_vnd(-price);
                            MyDanhHieu newDH = new MyDanhHieu();
                            newDH.id = temp_shop.id_title;
                            newDH.isUsed = false;
                            p.danh_hieu.add(newDH);

                            p.update_money();
                            p.body.setDirty();
                            p.update_info_to_all();
                            Service.UpdateInfoMaincharInfo(p);
                            Service.send_box_ThongBao_OK(p, "Mua danh hiệu thành công!");
                        }
                    }
                    break;
                }

                case 8887: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int realIndex = p.data_yesno[0];
                        Item_wear it = p.item.bag3[realIndex];
                        if (it != null && it.levelup < 100) {
                            if (p.item.total_item_bag_by_id(7, 12) < 1) {
                                Service.send_box_ThongBao_OK(p, "Bạn không có Bùa cường hóa!");
                                return;
                            }
                            // Nâng cấp thành công 100%
                            it.levelup++;
                            p.item.remove_item47(7, 12, 1);

                            p.item.update_Inventory(-1, false);
                            Service.UpdateInfoMaincharInfo(p);

                            String notice = "Cường hóa thành công " + it.template.name + " lên cấp " + it.levelup;
                            Service.send_box_ThongBao_OK(p, notice);

                            // Thông báo thế giới
                            String textKTG = "[BÙA MAY MẮN] Chúc mừng [" + p.getNameShow()
                                    + "] đã dùng Bùa cường hóa nâng thành công [" + it.template.name + "] lên cấp +"
                                    + it.levelup + "!";
                            Manager.gI().chatKTG(0, textKTG, 0);
                        } else if (it != null && it.levelup >= 100) {
                            Service.send_box_ThongBao_OK(p, "Trang bị đã đạt cấp +100!");
                        }
                    }
                    break;
                }

                case 8892: { // Trùng sinh đệ tử
                    if (p.myDisciple != null) {
                        if (p.myDisciple.level < 100) {
                            Service.send_box_ThongBao_OK(p, "Đệ tử cần đạt cấp 100 để có thể trùng sinh.");
                            break;
                        }
                        // GIẢM 50% CHI PHÍ
                        long costDis = 1_000_000L + (250_000L * p.myDisciple.reincarnation);
                        if (!p.payVnd(costDis)) {
                            Service.send_box_ThongBao_OK(p, "Sư phụ không đủ " + Util.number_format(costDis)
                                    + " Extol để trùng sinh cho đệ tử.");
                            break;
                        }

                        // Thực hiện trùng sinh cho đệ tử
                        p.myDisciple.reincarnation++;
                        p.myDisciple.level = 1;
                        p.myDisciple.exp = 0;
                        p.myDisciple.pointAttribute = 0;
                        p.myDisciple.point1 = 1;
                        p.myDisciple.point2 = 1;
                        p.myDisciple.point3 = 1;
                        p.myDisciple.point4 = 1;
                        p.myDisciple.point5 = 0;
                        p.myDisciple.thongthao = 0;
                        p.myDisciple.pointPk = 0;
                        p.myDisciple.resetAfterRitual();
                        p.update_money();

                        String msgKTG = "[TIN VUI] Đệ tử của [" + p.getNameShow()
                                + "] đã thực hiện nghi thức TRÙNG SINH lần thứ " + p.myDisciple.reincarnation
                                + " thành công!";
                        Manager.gI().chatKTG(0, msgKTG, 1);
                        Service.send_box_ThongBao_OK(p,
                                "Đệ tử trùng sinh thành công lên lần " + p.myDisciple.reincarnation + "!");
                        Player.flush(p, false);
                    }
                    break;
                }

                case 8893: { // Khôi phục tiềm năng đệ tử
                    if (p.myDisciple != null) {
                        try {
                            // 1. Reset các chỉ số về gốc (giống như lúc mới tạo)
                            p.myDisciple.point1 = 1;
                            p.myDisciple.point2 = 1;
                            p.myDisciple.point3 = 1;
                            p.myDisciple.point4 = 1;
                            p.myDisciple.point5 = 0;
                            p.myDisciple.pointAttribute = 0;

                            // 2. Tính lại tổng điểm Đệ tử ĐƯỢC PHÉP có
                            long totalPoints = (long) (p.myDisciple.level - 1) * p.myDisciple.getPotentialPerLevel();
                            long displayTotal = totalPoints;

                            // 3. Cơ chế Nạp điểm chia chặng (Tránh Tràn pointAttribute kiểu short)
                            while (totalPoints > 0) {
                                short chunk = (short) Math.min(totalPoints, 32000);
                                p.myDisciple.pointAttribute += chunk;
                                totalPoints -= chunk;

                                // Bắt đệ tử tiêu thụ cạn kiệt trong 1 giây để dọn kho
                                for (int i = 0; i < 2000 && p.myDisciple.pointAttribute > 0; i++) {
                                    p.myDisciple.autoAddPoint();
                                }
                            }

                            // 4. Bơm full máu
                            p.myDisciple.hp = p.myDisciple.body.get_hp_max(true);
                            p.myDisciple.mp = p.myDisciple.body.get_mp_max(true);

                            // 5. Cập nhật diện mạo
                            p.myDisciple.update_info_to_all();
                            Player.flush(p, false);

                            Service.send_box_ThongBao_OK(p,
                                    "Khôi phục tiềm năng hoàn tất!\nĐệ tử đã được tính toán lại tổng cộng "
                                            + Util.number_format(displayTotal) + " điểm và phân bổ tự động an toàn.");

                            String msgKTG = "[HỒI SINH] Đệ tử của [" + p.getNameShow()
                                    + "] đã hoàn tất Khôi phục tiềm năng, tái cấu trúc lại sức mạnh từ đầu!";
                            Manager.gI().chatKTG(0, msgKTG, 0);

                        } catch (Exception e) {
                            core.GameLogger.error("Error Resetting Disciple Potentials for " + p.name, e);
                            Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra khi khôi phục tiềm năng!");
                        }
                    }
                    break;
                }

                case 8891: { // Thức tỉnh đệ tử
                    if (p.myDisciple != null) {
                        if (p.myDisciple.levelAwaken >= 2) {
                            Service.send_box_ThongBao_OK(p, "Đệ tử đã đạt đỉnh cao Thức tỉnh Giai đoạn 2.");
                            break;
                        }
                        // Kiểm tra đồ đệ
                        int index1 = p.myDisciple
                                .get_index_full_set(p.myDisciple.item != null ? p.myDisciple.item.it_body : null);
                        int index2 = p.myDisciple
                                .get_index_full_set(p.myDisciple.item != null ? p.myDisciple.item.second_body : null);

                        if (p.myDisciple.levelAwaken == 0) {
                            if (index1 < 5 && index2 < 5) {
                                Service.send_box_ThongBao_OK(p, "Đệ tử cần ít nhất 1 bộ trang bị đạt cấp +100!");
                                break;
                            }
                        } else {
                            if (!(index1 >= 5 && index2 >= 5)) {
                                Service.send_box_ThongBao_OK(p, "Đệ tử cần cả 2 bộ trang bị đạt cấp +100!");
                                break;
                            }
                        }

                        // Kiểm tra nguyên liệu (6 trái ác quỷ)
                        boolean hasAll = true;
                        for (int i = 0; i < 6; i++) {
                            if (p.item.total_item_bag_by_id(4, p.myDisciple.idDF_required[i]) < 1) {
                                hasAll = false;
                                break;
                            }
                        }
                        if (!hasAll) {
                            Service.send_box_ThongBao_OK(p,
                                    "Sư phụ cần mang đủ 6 loại Trái ác quỷ yêu cầu trong túi đồ!");
                            break;
                        }

                        // Tiêu tốn (Trừ 6 trái từ Sư phụ lấy theo mảng yêu cầu của Đệ tử)
                        for (int i = 0; i < 6; i++) {
                            p.item.remove_item47(4, p.myDisciple.idDF_required[i], 1);
                        }
                        p.item.update_Inventory(-1, false);

                        // Roll tỉ lệ (Stage 1: 5%, Stage 2: 1%)
                        int rate = (p.myDisciple.levelAwaken == 0) ? 5 : 1;
                        if (Util.random(100) < rate) {
                            p.myDisciple.levelAwaken++;
                            String msg = p.myDisciple.levelAwaken == 1
                                    ? "[THỨC TỈNH] Hào quang rực rỡ đã bao trùm lấy đệ tử của [" + p.getNameShow()
                                            + "], đánh dấu bước thăng hoa đầu tiên!"
                                    : "[CHÍ TÔN] THẾ GIỚI RÚNG ĐỘNG! Đệ tử của [" + p.getNameShow()
                                            + "] đã thức tỉnh sức mạnh tối thượng!";
                            Manager.gI().chatKTG(0, msg, 1);
                            Service.send_box_ThongBao_OK(p,
                                    "Đệ tử Thức tỉnh thành công Giai đoạn " + p.myDisciple.levelAwaken + "!");
                            p.myDisciple.resetAfterRitual();
                        } else {
                            Service.send_box_ThongBao_OK(p, "Đệ tử thức tỉnh thất bại. Nguyên liệu đã tan biến!");
                        }
                        p.myDisciple.generateDFRequirements(); // Đổi yêu cầu của Đệ tử sau mỗi lần thử
                        Player.flush(p, false);
                    }
                    break;
                }

                case 8897: { // Xác nhận mặc thời trang cho đệ tử (Đồng ý=0)
                    if (p.data_yesno != null && p.data_yesno.length == 2 && p.myDisciple != null) {
                        int id_fas = p.data_yesno[0];
                        int cat_fas = p.data_yesno[1];
                        if (value == 0) { // Đồng ý
                            p.myDisciple.wear_fashion_disciple(id_fas, cat_fas);
                            String name = (cat_fas == 105) ? ItemFashion.get_item(id_fas).name
                                    : template.ItemHair.get_item(id_fas, cat_fas).name;
                            Service.send_box_ThongBao_OK(p, "Đệ tử đã mặc " + name + " thành công!");
                        }
                        Service.UpdateInfoMaincharInfo(p);
                    }
                    p.data_yesno = null;
                    break;
                }

                case 8896: { // Xác nhận dạy đệ dùng TAQ từ Shortcut (Đồng ý=0)
                    if (p.data_yesno != null && p.data_yesno.length == 1 && p.myDisciple != null) {
                        int itId = p.data_yesno[0];
                        if (value == 0) { // Đồng ý
                            if (p.item.total_item_bag_by_id(4, itId) > 0) {
                                p.myDisciple.eat_devil_fruit(itId);
                                p.item.remove_item47(4, itId, 1);
                                p.item.update_Inventory(-1, false);
                                Player.flush(p, false);
                                template.ItemTemplate4 temp = template.ItemTemplate4.get_it_by_id(itId);
                                String name = (temp != null) ? temp.name : ("Trái Ác Quỷ #" + itId);
                                Service.send_box_ThongBao_OK(p, "Đệ tử đã được dạy dùng " + name + " thành công!");
                            } else {
                                Service.send_box_ThongBao_OK(p, "Bạn không có vật phẩm này!");
                            }
                        }
                        p.data_yesno = null;
                        p.pendingFruitItemId = -1;
                    }
                    break;
                }

                case 974: {
                    // Awakening Confirmation
                    if (p.levelAwaken >= 2) {
                        break;
                    }
                    // 1. Check if player has all 3 required fruits
                    boolean hasAll = true;
                    for (int i = 0; i < 3; i++) {
                        short id_it = p.idDF_required[i];
                        if (p.item.total_item_bag_by_id(4, id_it) < 1) {
                            hasAll = false;
                            break;
                        }
                    }

                    if (!hasAll) {
                        Service.send_box_ThongBao_OK(p, "Bạn không có đủ 3 loại Trái ác quỷ theo yêu cầu!");
                        break;
                    }

                    // 2. Check gear sets
                    int index1 = p.get_index_full_set(p.item != null ? p.item.it_body : null);
                    int index2 = p.get_index_full_set(p.item != null ? p.item.second_body : null);

                    if (p.levelAwaken == 0) {
                        if (!(index1 == 5 || index2 == 5)) {
                            Service.send_box_ThongBao_OK(p, "Bạn cần ít nhất 1 bộ trang bị đạt cấp +100!");
                            break;
                        }
                    } else if (p.levelAwaken == 1) {
                        if (!(index1 == 5 && index2 == 5)) {
                            Service.send_box_ThongBao_OK(p, "Bạn cần cả 2 bộ trang bị đạt cấp +100!");
                            break;
                        }
                    }

                    // 3. Consume items (only 3 fruits for Master)
                    for (int i = 0; i < 3; i++) {
                        short id_it = p.idDF_required[i];
                        p.item.remove_item47(4, id_it, 1);
                    }
                    p.item.update_Inventory(4, true);

                    // 4. Roll
                    int rate = (p.levelAwaken == 0) ? 5 : 1;
                    if (Util.random(100) < rate) {
                        p.levelAwaken++;
                        String msg = p.levelAwaken == 1
                                ? "[THỨC TỈNH] Hào quang rực rỡ đã bao trùm lấy [" + p.getNameShow()
                                        + "], đánh dấu bước thăng hoa đầu tiên trên hành trình trở thành Huyền Thoại!"
                                : "[CHÍ TÔN] THẾ GIỚI RÚNG ĐỘNG! " + p.getNameShow()
                                        + " đã thức tỉnh sức mạnh tối thượng, chính thức đạt tới Giai đoạn 2 và làm chủ 3 Trái Ác Quỷ!";
                        Manager.gI().chatKTG(0, msg, 1);
                        Service.send_box_ThongBao_OK(p,
                                "Thức tỉnh thành công Giai đoạn " + p.levelAwaken + "!");
                        p.generateDFRequirements();
                    } else {
                        Service.send_box_ThongBao_OK(p, "Thức tỉnh thất bại. Yêu cầu nguyên liệu đã thay đổi!");
                        p.generateDFRequirements();
                    }
                    Player.flush(p, true);
                    break;
                }

                case 975: { // Reincarnation Confirmation
                    if (p.level < 100) {
                        Service.send_box_ThongBao_OK(p, "Bạn cần đạt cấp 100 để trùng sinh.");
                        break;
                    }
                    long cost = 200_000L + (50_000L * p.reincarnation);
                    if (!p.payVnd(cost)) {
                        Service.send_box_ThongBao_OK(p,
                                "Bạn không đủ " + Util.number_format(cost) + " Extol để trùng sinh.");
                        break;
                    }

                    // Deduct cost -> Already done in payVnd

                    // Increment reincarnation
                    p.reincarnation++;

                    // Reset stats
                    p.level = 1;
                    p.exp = 0;
                    p.pointAttribute = 0;
                    p.point1 = 1;
                    p.point2 = 1;
                    p.point3 = 1;
                    p.point4 = 1;
                    p.point5 = 0;

                    // Additional resets for reincarnation
                    p.thongthao = 0; // reset cấp thông thạo
                    p.pointPk = 0; // Reset PK points
                    p.hp = p.body.get_hp_max(true); // Refill HP
                    p.mp = p.body.get_mp_max(true); // Refill MP

                    // Refresh player info
                    p.setin4();
                    p.wait_change_map = false; // CRITICAL: Allow immediate map transition
                    Service.send_eff(p, 0, 0); // Level up effect
                    p.update_info_to_all();
                    p.update_money();

                    String msg = "Người chơi " + p.getNameShow() + " đã thực hiện nghi thức trùng sinh lần thứ "
                            + p.reincarnation + " thành công!";
                    Manager.gI().chatKTG(0, msg, 1);
                    Service.send_box_ThongBao_OK(p, "Trùng sinh thành công lần thứ " + p.reincarnation + "!");

                    Player.flush(p, true);
                    break;
                }
                case 194: { // Chim Phương Nam
                    if (p.item.total_item_bag_by_id(4, 194) > 0) {
                        core.OceanMazeManager.useSouthBird(p);
                        p.item.remove_item47(4, 194, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 103: { // Dragon Ball Summon
                    if (p.item.total_item_bag_by_id(4, 1087) < 1
                            || p.item.total_item_bag_by_id(4, 1088) < 1
                            || p.item.total_item_bag_by_id(4, 1089) < 1
                            || p.item.total_item_bag_by_id(4, 1090) < 1
                            || p.item.total_item_bag_by_id(4, 1091) < 1
                            || p.item.total_item_bag_by_id(4, 1092) < 1
                            || p.item.total_item_bag_by_id(4, 1093) < 1) {
                        Service.send_box_ThongBao_OK(p, "Bạn không đủ 7 viên ngọc rồng vô cực.");
                        return;
                    }
                    String[] wishes = new String[] {
                            "1 triệu Extol",
                            "10% Né đòn",
                            "10% Miễn thương",
                            "10% Phản đòn",
                            "10% Xuyên giáp",
                            "10% Sát thương chí mạng",
                            "10% Giảm né",
                            "10% Giảm phản",
                            "10% Giảm miễn",
                            "10% Giảm xuyên",
                            "5% Sát thương cuối",
                            "5% Máu cuối"
                    };
                    MenuController.send_dynamic_menu(p, 9995, "Điều Ước Rồng Thần", wishes, null);
                    break;
                }
                case 104: { // Dragon Ball Detector Reuse
                    if (value == 0) { // Agree
                        p.huntTarget = null;
                        boolean success = WishManager.gI().startHunting(p);
                        if (success) {
                            // Trừ máy dò thủ công vì không đi qua use_item_potion
                            p.item.remove_item47(4, 1086, 1);
                            try {
                                p.item.update_Inventory(-1, false);
                            } catch (IOException e) {
                                GameLogger.warn(
                                        "ClientYesNo.process: Error updating inventory after using Dragon Ball detector for "
                                                + p.name,
                                        e);
                            }
                        }
                    }
                    p.data_yesno = null;
                    break;
                }

                case 9877: {
                    if (p.item_to_kham_ngoc != null) {
                        Item_wear it = p.item_to_kham_ngoc;

                        long beriCost = Rebuild_Item.COST_BERRY_KICH_KHUON;
                        int rubyCost = Rebuild_Item.COST_RUBY_KICH_KHUON;
                        int extolCost = Rebuild_Item.COST_EXTOL_KICH_KHUON;

                        synchronized (p) {
                            if (p.get_vang() < beriCost || p.get_ngoc() < rubyCost || p.get_vnd() < extolCost) {
                                if (p.get_vang() < beriCost) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Không đủ " + Util.number_format(beriCost) + " Beri.");
                                } else if (p.get_ngoc() < rubyCost) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Không đủ " + Util.number_format(rubyCost) + " Ruby.");
                                } else {
                                    Service.send_box_ThongBao_OK(p,
                                            "Không đủ " + Util.number_format(extolCost) + " Extol.");
                                }
                                return;
                            }

                            // Deduct costs
                            p.update_vang(-beriCost);
                            p.update_ngoc(-rubyCost);
                            p.update_vnd(-extolCost);
                            p.update_money();
                        }

                        // 5% Success Rate
                        if (Util.random(100) < 5) {
                            // Generate random combo pattern summing to 12, each element >= 2
                            List<Integer> pattern = new ArrayList<>();
                            int remaining = 12;
                            while (remaining >= 2) {
                                int[] sizes = { 2, 3, 4, 6 };
                                List<Integer> possible = new ArrayList<>();
                                for (int s : sizes) {
                                    if (s <= remaining && (remaining - s >= 2 || remaining - s == 0)) {
                                        possible.add(s);
                                    }
                                }

                                int size;
                                if (!possible.isEmpty()) {
                                    size = possible.get(Util.random(possible.size()));
                                } else {
                                    size = remaining;
                                }
                                pattern.add(size);
                                remaining -= size;
                            }
                            // If somehow remaining is 1 (shouldn't happen with above logic), add it to the
                            // last element
                            if (remaining > 0 && !pattern.isEmpty()) {
                                pattern.set(pattern.size() - 1, pattern.get(pattern.size() - 1) + remaining);
                            }

                            java.util.Collections.shuffle(pattern);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < pattern.size(); i++) {
                                sb.append(pattern.get(i));
                                if (i < pattern.size() - 1) {
                                    sb.append("-");
                                }
                            }
                            it.combo_pattern = sb.toString();

                            Service.send_box_ThongBao_OK(p, "Kích khuôn thành công!\nTrang bị: " + it.template.name
                                    + "\nKhuôn mẫu: " + it.combo_pattern);
                            p.item.update_Inventory(-1, false);

                            // Broadcast
                            UpgradeDial.sendChatKTG_KichKhuon(p, it);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Kích khuôn thất bại! Liu Liu Ahihi!");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Hãy bỏ trang bị vào ô trước.");
                    }
                    break;
                }

                case 85: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int targetId = p.data_yesno[0];
                        Player target = null;
                        for (Player pl : Map.get_all_players()) {
                            if (pl.id == targetId) {
                                target = pl;
                                break;
                            }
                        }
                        if (target == null) {
                            Service.send_box_ThongBao_OK(p, "Đối tượng đã offline.");
                            return;
                        }
                        if (Map.map_cant_save_site(target.map.template.id)) {
                            Service.send_box_ThongBao_OK(p, "Không thể dịch chuyển đến map hiện tại của đối tượng.");
                            return;
                        }
                        Vgo vgo = new Vgo();
                        vgo.map_go = new Map[] { target.map };
                        vgo.xnew = target.x;
                        vgo.ynew = target.y;
                        p.goto_map(vgo);
                        Service.send_box_ThongBao_OK(target, p.name + " đã dịch chuyển tới vị trí của bạn.");
                    }
                    break;
                }

                case 84: {
                    VongXoayMayManManager.showSpinMenu(p);
                    break;
                }

                case 82: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        if (!p.payVnd(1000)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 1.000 Extol");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.update_money();

                        ItemFashionP2 temp = p.check_fashion(p.data_yesno[0]);
                        temp.op.clear();
                        temp.op = ItemFashionP2.randomOptionsForFashion(temp.id);
                        if (temp.is_use) {
                            p.body.setDirty();
                            Service.Main_char_Info(p);
                        }
                        ItemFashionP.show_table(p, 105);
                        Service.send_box_ThongBao_OK(p,
                                "Đổi Chỉ Số Thành Công\n" + ItemFashion.get_item(temp.id).name);
                    }
                    break;
                }

                case 81: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        ItemFashionP2 temp = p.check_fashion(p.data_yesno[0]);
                        p.update_fashionP2(temp);
                        for (int i = 0; i < p.map.players.size(); i++) {
                            Player p0 = p.map.players.get(i);
                            Service.charWearing(p, p0, false);
                        }
                        p.body.setDirty();
                        Service.Main_char_Info(p);
                        Service.UpdateInfoMaincharInfo(p);
                        ItemFashionP.show_table(p, 105);
                        Service.send_box_ThongBao_OK(p,
                                "Mặc thành công " + ItemFashion.get_item(temp.id).name);
                    }
                    break;
                }

                case 80: {
                    int buaHienTai = p.get_bua();
                    if (buaHienTai > 0) {
                        long extolNhanDuoc = (long) buaHienTai * 1000L;
                        p.update_bua(-buaHienTai);
                        p.update_vnd(extolNhanDuoc);
                        p.update_money();
                        Service.send_box_ThongBao_OK(p, " Đã rút toàn bộ \n"
                                + Util.number_format(buaHienTai) + " Búa về "
                                + Util.number_format(extolNhanDuoc) + " Extol");
                    }
                    break;
                }

                case 79: {
                    if (!p.payNgoc(100_000)) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 100,000 Ruby");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    p.update_money();
                    long oneDayMillis = 24 * 3600 * 1000L;
                    int fashionId = 1; // ID thời trang Wukong
                    ItemFashionP2 fashion = p.check_fashion(fashionId);
                    long now = System.currentTimeMillis();
                    if (fashion == null) {
                        fashion = new ItemFashionP2();
                        fashion.id = (short) fashionId;
                        fashion.is_use = false;
                        fashion.level = 0;
                        fashion.expirationTime = now + oneDayMillis;
                        fashion.op = ItemFashionP2.randomOptionsForFashion((short) id);
                        p.fashion.add(fashion);
                    } else {
                        if (fashion.expirationTime == -1) {
                            return;
                        }
                        if (fashion.expirationTime < now) {
                            fashion.expirationTime = now + oneDayMillis;
                        } else {
                            fashion.expirationTime += oneDayMillis;
                        }
                    }
                    Message m = new Message(-34);
                    m.writer().writeByte(13);
                    m.writer().writeUTF("Thuê Thời Trang");
                    m.writer().writeUTF("");
                    m.writer().writeByte(1);
                    ItemFashion itf = ItemFashion.get_item(fashionId);
                    m.writer().writeByte(105);
                    m.writer().writeUTF("Wukong (1 Ngày)");
                    m.writer().writeShort(itf.idIcon);
                    m.writer().writeInt(1);
                    m.writer().writeByte(0);
                    p.addmsg(m);
                    m.cleanup();
                    break;
                }

                case 77: { // Xóa hết trang bị
                    int count = 0; // Biến đếm số lượng trang bị đã xóa
                    for (int i = 0; i < p.item.bag3.length; i++) {
                        if (p.item.bag3[i] != null) {
                            p.item.remove_item_wear(p.item.bag3[i]);
                            count++; // Tăng biến đếm khi xóa một trang bị
                        }
                    }
                    p.item.update_Inventory(-1, false); // Cập nhật hành trang sau khi xóa
                    Service.send_box_ThongBao_OK(p, "Đã xóa " + count + " trang bị trong hành trang!");
                    break;
                }
                case 76: { // Xóa hết vật phẩm
                    int count = 0; // Biến đếm số lượng vật phẩm đã xóa
                    for (int i = 0; i < p.item.bag47.size(); i++) {
                        ItemBag47 item = p.item.bag47.get(i);
                        if (item != null && (item.category == 4 || item.category == 7)) {
                            if (item.id == 1007) {
                                continue; // Bỏ qua vật phẩm có id là 1007
                            }
                            count += item.quant; // Cộng số lượng vật phẩm đã xóa
                            p.item.remove_item47(item.category, item.id, item.quant);
                        }
                    }
                    p.item.update_Inventory(-1, false); // Cập nhật hành trang sau khi xóa
                    Service.send_box_ThongBao_OK(p, "Đã xóa " + count + " vật phẩm trong hành trang!");
                    break;
                }

                case 75: {
                    if (!p.payVang(80_000_000)) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 80.000.000 Beri");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    p.update_money();

                    if (p.item.able_bag() < 1) {
                        Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    int[] idOptions = { 11054, 11055 };
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
                        Service.send_gift(p, 1, "Vũ khí Noel", "", list, true);
                    }
                    break;
                }

                case 74: {
                    break;
                }
                case 73: {
                    break;
                }

                case 72: {
                    if (p.get_ngoc() < 1_000) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 1.000 Ruby");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    if (p.item.total_item_bag_by_id(4, 441) >= DataTemplate.MAX_ITEM_IN_BAG) {
                        Service.send_box_ThongBao_OK(p,
                                "Số lượng vé đã đạt tối đa trong hành trang");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    p.update_ngoc(-1_000);
                    p.update_money();
                    p.item.add_item_bag47(4, 441, 1);
                    p.item.update_Inventory(-1, false);
                    Service.send_box_ThongBao_OK(p, "Mua 1 Vé quay ốc sên thành công");
                    break;
                }

                case 71: {
                    if (!p.payTicket(10)) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 10 Bánh mì");
                    } else {
                        p.update_money();
                        Service.CountDown_Ticket(p);
                        //
                        Map[] map_go = Map.get_map_by_id(309);

                        Vgo vgo = new Vgo();
                        vgo.map_go = map_go;
                        vgo.xnew = 350;
                        vgo.ynew = 250;
                        p.goto_map(vgo);
                    }
                    break;
                }

                case 70: {
                    Upgrade_Skin.normalUpgrade(p);
                    break;
                }

                case 69: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        UpgradeSuperItem.normalUpgrade(p, p.data_yesno[0]);
                    }
                    break;
                }

                case 68: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        UpgradeDial.normalUpgrade(p, p.data_yesno[0]);
                    }
                    break;
                }

                case 67: {
                    if (p.worldWarInfo != null) {
                        if (p.payNgoc(5)) {
                            p.update_money();
                            p.worldWarInfo.skPoint += 5;
                            if (p.worldWarInfo.skPoint > 10) {
                                p.worldWarInfo.skPoint = 10;
                            }
                            Service.send_box_ThongBao_OK(p,
                                    "Điểm hạ gục hiện tại của bạn là " + p.worldWarInfo.skPoint);
                            WorldWar.updateWorldWarPoint(p, p.worldWarInfo.skPoint, p.worldWarInfo.kill,
                                    p.worldWarInfo.dead);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5 Ruby");
                        }
                    }
                    break;
                }

                case 66: {
                    if (p.item.able_bag() < 1) {
                        Service.send_box_ThongBao_OK(p,
                                "Hành trang phải chừa ít nhất 1 ô trống");
                        return;
                    }

                    if (p.item.total_item_bag_by_id(4, 1021) < 1000) {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ 1000 Đá tinh tú");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(4, 1020) < 50) {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ 50 Nước tinh tú");
                        return;
                    }
                    if (!p.payVnd(200000)) {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ 200.000 Extol");
                        return;
                    }
                    // tru item
                    p.item.remove_item47(4, 1021, 1000);
                    p.item.remove_item47(4, 1020, 50);
                    p.item.update_Inventory(-1, false);

                    p.update_money();
                    // cong ruong tinh tu
                    p.item.add_item_bag47(4, 1019, 1);
                    p.item.update_Inventory(-1, false);
                    Service.send_box_ThongBao_OK(p,
                            "Chế tạo thành công 1 Rương tinh tú");
                    break;
                }

                case 65: {
                    int banh = p.item.total_item_bag_by_id(4, 1025); // Số lượng bánh hiện có
                    int quantyden = banh / 10; // Số lượng đèn trời có thể đổi

                    if (quantyden > 0) {
                        p.item.add_item_bag47(4, 332, quantyden); // Thêm số lượng đèn trời vào túi
                        p.item.remove_item47(4, 1025, quantyden * 10); // Xóa số lượng bánh đã đổi
                        p.item.update_Inventory(-1, false);
                        Service.send_box_ThongBao_OK(p,
                                "Đổi thành công " + (quantyden * 10) + " bánh sang " + quantyden + " đèn trời");
                    } else {
                        Service.send_box_ThongBao_OK(p, "Không đủ bánh để đổi đèn trời.");
                    }

                    break;
                }

                case 64: {
                    int levelMob = p.level;
                    long dame = p.body.get_dame(true);
                    dame = (dame * p.body.get_dame_devil_percent()) / 100;
                    long hpScale = dame * 10;

                    int index_mob = -2;
                    Map mapTemplate = Map.get_map_by_id(301)[0];
                    Map map_boss = new Map();
                    map_boss.template = mapTemplate.template;
                    map_boss.zone_id = (byte) 0;
                    map_boss.list_mob = new int[0];
                    map_boss.map_Hang = new Map_Hang_Dong();
                    map_boss.map_Hang.time = System.currentTimeMillis() + (60_000L * 10);
                    map_boss.map_Hang.mobs = new ArrayList<>();
                    map_boss.map_Hang.listRemoveMap = new HashMap<>();
                    map_boss.map_Hang.gifted = false;
                    map_boss.map_Hang.levelMob = levelMob;
                    map_boss.map_Hang.hpScale = hpScale;
                    map_boss.map_Hang.level = 1;

                    for (int i = 0; i < mapTemplate.list_mob.length; i++) {
                        Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
                        Mob mob_add = new Mob();
                        mob_add.mob_template = temp.mob_template;
                        mob_add.x = temp.x;
                        mob_add.y = temp.y;
                        mob_add.hp_max = hpScale;
                        mob_add.hp = mob_add.hp_max;
                        mob_add.level = levelMob;
                        mob_add.isdie = false;
                        mob_add.id_target = -1;
                        mob_add.index = index_mob--;
                        mob_add.map = map_boss;
                        map_boss.map_Hang.mobs.add(mob_add);
                    }

                    try {
                        p.map.leave_map(p, 2);
                        p.map = map_boss;
                        p.x = 100;
                        p.y = 300;
                        p.xold = p.x;
                        p.yold = p.y;
                        p.map.goto_map(p);
                        Service.update_PK(p, p, true);
                        Service.pet(p, p, true);
                        Quest.update_map_have_side_quest(p, true);
                    } catch (IOException e) {
                        GameLogger.error("ClientYesNo.process: Error entering custom boss map for " + p.name, e);
                    }

                    map_boss.start_map();
                    Map.add_map_plus(map_boss);
                    break;
                }

                case 63: {
                    if (p.clan != null) {
                        boolean isLeader = p.clan.members.get(0).name.equals(p.name);

                        if (isLeader) {
                            if (Clan.delete_clan_from_db(p.clan)) {
                                for (Clan_member member : p.clan.members) {
                                    Player memberPlayer = Map.get_player_by_name_allmap(member.name);
                                    if (memberPlayer != null) {
                                        Clan.send_info(memberPlayer, false);
                                        for (int i = 0; i < p.map.players.size(); i++) {
                                            if (!p.map.players.get(i).equals(memberPlayer)) {
                                                Clan.send_me_to_other(memberPlayer, p.map.players.get(i), false);
                                            }
                                        }
                                        Service.send_box_ThongBao_OK(memberPlayer, "Băng của bạn đã bị xóa");

                                        Message m = new Message(-52);
                                        m.writer().writeByte(10);
                                        m.writer().writeShort(memberPlayer.index_map);
                                        p.map.send_msg_all_p(m, memberPlayer, true);
                                        m.cleanup();

                                        memberPlayer.clan = null;
                                    }
                                }
                                p.clan = null;
                                Service.send_box_ThongBao_OK(p, "Xóa băng thành công");
                            } else {
                                Service.send_box_ThongBao_OK(p, "Xóa băng thất bại");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Chỉ thuyền trưởng mới được phép xóa băng");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Bạn không thuộc bất kỳ băng nào để xóa");
                    }
                    break;
                }

                case 59: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(p.data_yesno[0]);
                        if (itemTemplate4 != null) {
                            ItemBag47 it_select = null;
                            for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                                if (p.daHanhTrinh.get(i).category > -1
                                        && p.map.template.id == HanhTrinh.LANG[p.daHanhTrinh
                                                .get(i).category]
                                        && p.daHanhTrinh.get(i).id == p.data_yesno[0]
                                        && p.daHanhTrinh.get(i).quant == 1) {
                                    it_select = p.daHanhTrinh.get(i);
                                }
                            }
                            if (it_select != null) {
                                if (!p.payNgoc(100)) {
                                    Service.send_box_ThongBao_OK(p, "Không đủ 100 Ruby");
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    return;
                                }
                                p.update_money();
                                //
                                it_select.quant = 0;
                                HanhTrinh.update_da_kham(p);
                                //
                                Message m = new Message(79);
                                m.writer().writeByte(0);
                                m.writer().writeShort(HanhTrinh.get_map(p));
                                m.writer().writeUTF(p.map.template.name);
                                List<ItemBag47> list_DaHanhTrinh = p.get_list_daHanhTrinh_total(p.map.template.id);
                                m.writer().writeByte(list_DaHanhTrinh.size());
                                for (int i = 0; i < list_DaHanhTrinh.size(); i++) {
                                    ItemTemplate4 itemTemplate4_ = ItemTemplate4
                                            .get_it_by_id(list_DaHanhTrinh.get(i).id);
                                    m.writer().writeUTF(itemTemplate4_.name);
                                    m.writer().writeByte(4);
                                    m.writer().writeShort(itemTemplate4_.id);
                                    m.writer().writeByte(1);
                                    m.writer().writeShort(itemTemplate4_.icon);
                                    ItemTemplate4_Info temp_info = ItemTemplate4_Info
                                            .get_by_id(itemTemplate4_.indexInfoPotion);
                                    if (temp_info != null) {
                                        m.writer().writeUTF(temp_info.info);
                                    } else {
                                        m.writer().writeUTF("Chưa có thông tin");
                                    }
                                }
                                p.addmsg(m);
                                m.cleanup();
                                //
                                Service.send_box_ThongBao_OK(p,
                                        "Tách thành công " + itemTemplate4.name);
                                p.update_info_to_all();
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không đủ 1 " + itemTemplate4.name);
                            }
                        }
                    }
                    break;
                }
                case 58: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(p.data_yesno[0]);
                        if (itemTemplate4 != null) {
                            ItemBag47 it_select = null;
                            for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                                if (p.daHanhTrinh.get(i).category > -1
                                        && p.map.template.id == HanhTrinh.LANG[p.daHanhTrinh
                                                .get(i).category]
                                        && p.daHanhTrinh.get(i).id == p.data_yesno[0]
                                        && p.daHanhTrinh.get(i).quant == 0) {
                                    it_select = p.daHanhTrinh.get(i);
                                }
                            }
                            if (it_select != null) {
                                it_select.quant = 1;
                                //
                                Message m = new Message(79);
                                m.writer().writeByte(2);
                                m.writer().writeShort(p.data_yesno[0]);
                                p.addmsg(m);
                                m.cleanup();
                                HanhTrinh.update_da_kham(p);
                                //
                                Service.send_box_ThongBao_OK(p,
                                        "Khảm thành công " + itemTemplate4.name);
                                p.update_info_to_all();
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không đủ 1 " + itemTemplate4.name);
                            }
                        }
                    }
                    break;
                }
                case 57: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Item_wear it_select = p.item.bag3[p.data_yesno[0]];
                        if (it_select != null && it_select.numLoKham < 12) {
                            int beri_req = 5_000_000 * (it_select.numLoKham + 1);
                            if (p.item.total_item_bag_by_id(4, 457) < 1) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 1 Búa Đục Lỗ");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (!p.payVang(beri_req)) {
                                Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(beri_req) + "Beri");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.item.remove_item47(4, 457, 1);

                            p.update_money();
                            boolean suc = 20 > Util.random(100 + it_select.numLoKham * 50);
                            if (suc) {
                                if (it_select.numHoleDaDuc < 0) {
                                    it_select.numHoleDaDuc = 0;
                                }
                                it_select.numLoKham++;
                                it_select.numHoleDaDuc++;
                            }
                            Message m = new Message(-67);
                            m.writer().writeByte(7);
                            m.writer().writeUTF(suc ? ("Đục lỗ thành công " + it_select.template.name)
                                    : "Rất tiếc đục lỗ thất bại");
                            p.addmsg(m);
                            m.cleanup();
                            p.item.update_Inventory(-1, false);
                        }
                    }
                    break;
                }

                case 56: {
                    if (p.map.map_ThuThachVeThan == null) {
                        int levelMob = p.level;
                        long dame = p.body.get_dame(true);
                        dame = (dame * p.body.get_dame_devil_percent()) / 100;
                        long hpScale = dame * 10;
                        int index_mob = -2;
                        Map mapTemplate = Map.get_map_by_id(900)[0];
                        Map map_boss = new Map();
                        map_boss.template = mapTemplate.template;
                        map_boss.zone_id = (byte) 0;
                        map_boss.list_mob = new int[0];
                        map_boss.map_ThuThachVeThan = new Map_ThuThachVeThan();
                        map_boss.map_ThuThachVeThan.time_state = System.currentTimeMillis() + (60_000L * 10);
                        map_boss.map_ThuThachVeThan.mobs = new ArrayList<>();
                        map_boss.map_ThuThachVeThan.listRemoveMap = new HashMap<>();
                        map_boss.map_ThuThachVeThan.gifted = false;
                        map_boss.map_ThuThachVeThan.levelMob = levelMob;
                        map_boss.map_ThuThachVeThan.hpScale = hpScale;
                        map_boss.map_ThuThachVeThan.level = 1;
                        for (int i = 0; i < mapTemplate.list_mob.length; i++) {
                            Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
                            Mob mob_add = new Mob();
                            mob_add.mob_template = temp.mob_template;
                            mob_add.x = temp.x;
                            mob_add.y = temp.y;
                            mob_add.hp_max = hpScale;
                            mob_add.hp = mob_add.hp_max;
                            mob_add.level = levelMob;
                            mob_add.isdie = false;
                            mob_add.id_target = -1;
                            mob_add.index = index_mob--;
                            mob_add.map = map_boss;
                            map_boss.map_ThuThachVeThan.mobs.add(mob_add);
                        }
                        try {
                            p.map.leave_map(p, 2);
                            p.map = map_boss;
                            p.x = 100;
                            p.y = 300;
                            p.xold = p.x;
                            p.yold = p.y;
                            p.map.goto_map(p);
                            Service.update_PK(p, p, true);
                            Service.pet(p, p, true);
                            Quest.update_map_have_side_quest(p, true);
                        } catch (IOException e) {
                            GameLogger.error(
                                    "ClientYesNo.process: Error entering 'Thu Thach Ve Than' map for " + p.name, e);
                        }
                        map_boss.start_map();
                        Map.add_map_plus(map_boss);
                    }
                    break;
                }

                case 55: {
                    Skill_info sk_select = null;
                    for (int i = 0; i < p.skill_point.size(); i++) {
                        if (p.skill_point.get(i).temp.indexSkillInServer >= 661
                                && p.skill_point.get(i).temp.indexSkillInServer <= 666) {
                            sk_select = p.skill_point.get(i);
                            break;
                        }
                    }
                    if (sk_select == null) {
                        if (p.time_ttvt < 50) {
                            if (!p.payNgoc(500)) {
                                Service.send_box_ThongBao_OK(p, "không đủ 500 Ruby để có thể học");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_money();
                        }
                        //
                        sk_select = new Skill_info();
                        sk_select.exp = 0;
                        sk_select.temp = Skill_Template.get_temp(661, 0);
                        sk_select.lvdevil = 0;
                        sk_select.devilpercent = 0;
                        p.skill_point.add(sk_select);
                        p.send_skill();
                        p.update_info_to_all();
                        Service.send_box_ThongBao_OK(p,
                                "Học thành công kỹ năng chế tạo dial cấp 1");
                    } else {
                        Service.send_box_ThongBao_OK(p, "Đã học kỹ năng này rồi");
                    }
                    break;
                }
                case 53: {
                    if (p.name_ThoSanHaiTac != null && p.name_ThoSanHaiTac.length == 1
                            && p.typePirate == 1) {
                        Player p0 = Map.get_player_by_name_allmap(p.name_ThoSanHaiTac[0]);
                        if (p0 != null && p0.map.equals(p.map) && p0.ship_pet != null) {
                            Service.send_box_ThongBao_OK(p0,
                                    p.name + " đồng ý bảo vệ vận hàng, hãy bắt đầu chuyến đi");
                            Service.send_box_ThongBao_OK(p,
                                    "đồng ý bảo vệ vận hàng thành công, hãy bắt đầu chuyến đi");
                            p0.ship_pet.mainBaoVe = p.name;
                        } else {
                            Service.send_box_ThongBao_OK(p, "Đối phương đã rời đi");
                        }
                    }
                    break;
                }
                case 52: {
                    break;
                }
                case 51: {
                    if (p.tableTickOption != null && !p.tableTickOption.is_finish) {
                        if (p.tableTickOption.listP.get(0).name.equals(p.name)) {
                            for (int i = 0; i < p.tableTickOption.listP.size(); i++) {
                                Player p0 = Map.get_player_by_name_allmap(
                                        p.tableTickOption.listP.get(i).name);
                                if (p0 != null) {
                                    Service.send_box_ThongBao_OK(p0,
                                            "Đăng ký tham gia thành công, đợi ghép với băng khác");
                                }
                            }
                            LittleGarden.add_clan_wait(p.clan);
                            p.tableTickOption.is_finish = true;
                        }
                    }
                    break;
                }

                case 50: {
                    if (p.typePirate == 0) {
                        Message m = new Message(17);
                        m.writer().writeShort(-443);
                        m.writer().writeByte(2);
                        m.writer().writeUTF("Chúc bạn lên đường may mắn");
                        p.addmsg(m);
                        m.cleanup();
                        //
                        p.ship_pet = new Ship_pet();
                        p.ship_pet.index_map = Ship_pet.getNewIndexMap();
                        if (p.ship_pet.index_map != -1) {
                            p.ship_pet.main_ship = p;
                            p.ship_pet.map = p.map;
                            p.ship_pet.name = "Hàng " + p.name;
                            p.ship_pet.x = p.x;
                            p.ship_pet.y = p.y;
                            p.ship_pet.hp_max = 1000;
                            p.ship_pet.hp = p.ship_pet.hp_max;
                            p.ship_pet.id_map_save = p.map.template.id;
                            Ship_pet.add(p.ship_pet);
                            Message m_local = new Message(1);
                            m_local.writer().writeByte(0);
                            m_local.writer().writeShort(p.ship_pet.index_map);
                            m_local.writer().writeShort(p.ship_pet.x);
                            m_local.writer().writeShort(p.ship_pet.y);
                            for (int j = 0; j < p.map.players.size(); j++) {
                                Player p0 = p.map.players.get(j);
                                p0.addmsg(m_local);
                            }
                            m_local.cleanup();
                        }
                    }
                    break;
                }

                case 46: {
                    break;
                }
                case 45: {
                    break;
                }
                case 44: {
                    if (p.data_yesno != null && p.data_yesno.length == 1 && p.data_topPkOnline != null) {
                        int index = p.data_yesno[0];
                        if (index >= 0 && index < p.data_topPkOnline.length) {
                            String name = p.data_topPkOnline[index].split(";")[0];
                            Player target = Map.get_player_by_name_allmap(name);

                            if (p.name.equalsIgnoreCase(name)) {
                                Service.send_box_ThongBao_OK(p, "Bạn không thể tự dịch chuyển đến chính mình.");
                                return;
                            }

                            if (target == null) {
                                Service.send_box_ThongBao_OK(p, "Người chơi này đã offline.");
                                return;
                            }

                            if (p.ship_pet != null) {
                                Service.send_box_ThongBao_OK(p, "Không thể dịch chuyển khi đang vận chuyển hàng.");
                                return;
                            }

                            if (Map.map_cant_save_site(target.map.template.id)) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không thể dịch chuyển đến map hiện tại của người chơi này.");
                                return;
                            }

                            // Kiểm tra bổ sung khi đối tượng ở map Bãi Farm Đá 2 (316)
                            if (target.map.template.id == 316) {
                                boolean isValid = true;
                                for (int checkId = 0; checkId <= 2; checkId++) {
                                    boolean hasSkill = false;
                                    // p.skill_point chứa Skill_info, temp.ID tương ứng với id_2
                                    for (template.Skill_info sk : p.skill_point) {
                                        if (sk.temp.ID == checkId && sk.temp.Lv_RQ >= 500) {
                                            hasSkill = true;
                                            break;
                                        }
                                    }
                                    if (!hasSkill) {
                                        isValid = false;
                                        break;
                                    }
                                }
                                if (!isValid) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Yêu cầu 3 Kỹ Năng Cơ Bản đạt cấp 500 trở lên mới có thể theo dõi vị vua này vào Bãi Farm Đá 2.");
                                    return;
                                }
                            }

                            Vgo vgo = new Vgo();
                            vgo.map_go = new Map[] { target.map };
                            vgo.xnew = target.x;
                            vgo.ynew = target.y;
                            p.goto_map(vgo);

                            Service.send_box_ThongBao_OK(target, p.name + " đã dịch chuyển tới vị trí của bạn.");
                        }
                    }
                    break;
                }

                case 43: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Player p0 = Map.get_player_by_id_allmap((int) p.data_yesno[0]);
                        if (p0 != null) {
                            if (p.ship_pet != null) {
                                Service.send_box_ThongBao_OK(p, "Không thể dịch chuyển khi đang vận chuyển hàng");
                            } else if (p.payNgoc(100)) {
                                p.update_money();
                                Vgo vgo = new Vgo();
                                vgo.map_go = new Map[1];
                                vgo.map_go[0] = p0.map;
                                vgo.xnew = p0.x;
                                vgo.ynew = p0.y;
                                p.goto_map(vgo);
                                Service.send_box_ThongBao_OK(p0, p.name + " đã dịch chuyển tới vị trí của bạn");
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không đủ 100 Ruby để thực hiện");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Đối phương không online hoặc không có trong danh sách");
                        }
                    }
                    break;
                }

                case 42: {
                    if (p.clan != null) {
                        Clan_member clan_mem = null;
                        for (int i = 0; i < p.clan.members.size(); i++) {
                            if (p.clan.members.get(i).name.equals(p.name)) {
                                clan_mem = p.clan.members.get(i);
                                break;
                            }
                        }
                        if (clan_mem != null) {
                            if (clan_mem.numquest >= 3) {
                                Service.send_box_ThongBao_OK(p,
                                        "Hôm nay đã hết nhiệm vụ, hãy quay lại vào ngày mai");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            Clan.send_info(p, false);
                            QuestP questP = null;
                            for (int i = 0; i < p.list_quest.size(); i++) {
                                if (p.list_quest.get(i).template.id < -2000) {
                                    questP = p.list_quest.get(i);
                                    break;
                                }
                            }
                            if (questP == null) {
                                clan_mem.numquest++;
                                questP = new QuestP();
                                questP.template = Quest.get_quest(-3000 + ((clan_mem.numquest - 1) * 2));
                                questP.data = new short[questP.template.data_quest.length][];
                                for (int i = 0; i < questP.data.length; i++) {
                                    questP.data[i] = new short[questP.template.data_quest[i].length];
                                    for (int j = 0; j < questP.data[i].length; j++) {
                                        questP.data[i][j] = questP.template.data_quest[i][j];
                                    }
                                }
                                p.list_quest.add(questP);
                                //
                                Message m = new Message(-23);
                                m.writer().writeByte(1);
                                m.writer().writeByte(questP.template.statusQuest);
                                Quest.write_Quest(m.writer(), questP);
                                p.addmsg(m);
                                m.cleanup();
                                m = new Message(-23);
                                m.writer().writeByte(5);
                                m.writer().writeShort(questP.template.index);
                                p.addmsg(m);
                                m.cleanup();
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Nhiệm vụ hiện tại chưa hoàn thành");
                            }
                        }
                    }
                    break;
                }
                case 41: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Max_Level.addThongThao(p, p.data_yesno[0], 100);
                    }
                    break;
                }

                case 40: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(p.data_yesno[0]);
                        if (itemTemplate4 != null) {
                            if (itemTemplate4.type == 74) {
                                if (p.item.total_item_bag_by_id(4, itemTemplate4.id) < 18) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Không đủ 18 " + itemTemplate4.name);
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    return;
                                }
                                if (p.item.able_bag() < 1) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Hành trang phải chừa ít nhất 1 ô trống");
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    return;
                                }
                                p.item.remove_item47(4, itemTemplate4.id, 18);
                                Item_wear it_add = null;
                                int id_b1 = 0;
                                int id_b2 = 0;
                                int color = 0;
                                int clazz = p.clazz;
                                short[] type_equip = new short[] { 0 };
                                switch (itemTemplate4.id) {
                                    case 304: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 0;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 305: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 0;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 306: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 0;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 307: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 1;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 308: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 1;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 309: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 1;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 310: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 2;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 311: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 2;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 312: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 2;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 313: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 3;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 314: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 3;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 315: {
                                        id_b1 = 1728;
                                        id_b2 = 1919;
                                        color = 3;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 536: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 0;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 537: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 0;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 538: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 0;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 539: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 1;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 540: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 1;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 541: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 1;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 542: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 2;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 543: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 2;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 544: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 2;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                    case 545: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 3;
                                        type_equip = new short[] { 1, 3, 5 };
                                        break;
                                    }
                                    case 546: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 3;
                                        type_equip = new short[] { 2, 4 };
                                        clazz = 0;
                                        break;
                                    }
                                    case 547: {
                                        id_b1 = 1920;
                                        id_b2 = 2111;
                                        color = 3;
                                        type_equip = new short[] { 0 };
                                        break;
                                    }
                                }
                                List<ItemTemplate3> list_random = new ArrayList<>();
                                for (int i = 0; i < ItemTemplate3.ENTRYS.size(); i++) {
                                    ItemTemplate3 it_temp = ItemTemplate3.ENTRYS.get(i);
                                    if (it_temp.id >= id_b1 && it_temp.id <= id_b2
                                            && it_temp.color == color && it_temp.clazz == clazz) {
                                        for (int j = 0; j < type_equip.length; j++) {
                                            if (type_equip[j] == it_temp.typeEquip) {
                                                list_random.add(it_temp);
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (list_random.size() > 0) {
                                    it_add = new Item_wear();
                                    it_add.setup_template_by_id(
                                            list_random.get(Util.random(list_random.size())));
                                }
                                if (it_add.template != null) {
                                    int numLoKham = (50 > Util.random(120)) ? 0
                                            : ((70 > Util.random(120)) ? 1 : 2);
                                    it_add.numLoKham = (byte) numLoKham;
                                    p.item.add_item_bag3(it_add);
                                }
                                p.item.update_Inventory(-1, false);
                                if (it_add != null) {
                                    Message m = new Message(-67);
                                    m.writer().writeByte(31);
                                    m.writer().writeUTF(
                                            "Bạn ghép thành công được " + it_add.template.name);
                                    m.writer().writeShort(it_add.index);
                                    p.addmsg(m);
                                    m.cleanup();
                                }
                            }
                        }
                    }
                    break;
                }
                case 39:
                case 38: { // no use
                    break;
                }
                case 37: {
                    // so
                    // 88, 318, 90, 34, 91
                    //
                    // trung
                    // 32,33,92,93,219,220,316,317
                    //
                    // cao
                    // 240, 160, 161, 427
                    if (p.item.total_item_bag_by_id(4, 87) > 0) {
                        int index = Util.random(1000);
                        if (index < 50) { // 316
                            String[] name_ = new String[] { "Giáp sáp", "Đao không kích", "Lao sáp" };
                            int[] icon_ = new int[] { 79, 77, 78 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 316);
                            p.get_skill_taq_new(316);
                        } else if (index < 130) { // 32
                            String[] name_ = new String[] { "Sức mạnh của lửa", "Hỏa quyền", "Nắm đấm lửa" };
                            int[] icon_ = new int[] { 32, 30, 29 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 32);
                            p.get_skill_taq_new(32);
                        } else if (index < 210) { // 93
                            String[] name_ = new String[] { "Cát lưu động", "Bão cát sa mạc",
                                    "Cát linh động" };
                            int[] icon_ = new int[] { 55, 54, 52 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 93);
                            p.get_skill_taq_new(93);
                        } else if (index < 330) { // 317
                            String[] name_ = new String[] { "Thân thể thép", "Ảo ảnh trảm", "Loạn trảm" };
                            int[] icon_ = new int[] { 82, 81, 80 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 317);
                            p.get_skill_taq_new(317);
                        } else if (index < 450) { // 92
                            String[] name_ = new String[] { "Băng vĩnh cửu", "Mưa băng", "Tuyết tê tái" };
                            int[] icon_ = new int[] { 57, 56, 53 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 92);
                            p.get_skill_taq_new(92);
                        } else if (index < 580) { // 219
                            String[] name_ = new String[] { "Sóng âm - Xung kích", "Hóa báo đốm", "Tia chớp" };
                            int[] icon_ = new int[] { 72, 71, 70 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 219);
                            p.get_skill_taq_new(219);
                        } else if (index < 710) { // 220
                            String[] name_ = new String[] { "Cơn lốc - Ưng kích", "Hóa chim ưng",
                                    "Chim săn mồi" };
                            int[] icon_ = new int[] { 69, 68, 67 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 220);
                            p.get_skill_taq_new(220);
                        } else { // 33
                            String[] name_ = new String[] { "Sức sống bất diệt", "Chất bất ổn",
                                    "Súng máy caosu" };
                            int[] icon_ = new int[] { 34, 33, 31 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 33);
                            p.get_skill_taq_new(33);
                        }
                        p.item.remove_item47(4, 87, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 36: {
                    break;
                }
                case 35: {
                    if (p.item.total_item_bag_by_id(4, 86) > 0) {
                        int index = Util.random(1000);
                        if (index < 50) { // 88
                            String[] name_ = new String[] { "Khói bất tử", "Khói tốc độ", "Mưa khói" };
                            int[] icon_ = new int[] { 38, 39, 40 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 88);
                            p.get_skill_taq_new(88);
                        } else if (index < 150) { // 318
                            String[] name_ = new String[] { "Thần hộ thể", "Tăng trọng", "Sức nặng ngàn cân" };
                            int[] icon_ = new int[] { 85, 84, 83 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 318);
                            p.get_skill_taq_new(318);
                        } else if (index < 350) { // 90
                            String[] name_ = new String[] { "Bản năng thủ lĩnh", "Hóa bò tót", "Bất khuất" };
                            int[] icon_ = new int[] { 48, 47, 46 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 90);
                            p.get_skill_taq_new(90);
                        } else if (index < 650) { // 34
                            String[] name_ = new String[] { "Tiến hóa", "Thuốc tăng trưởng", "Hóa tuần lộc" };
                            int[] icon_ = new int[] { 37, 36, 35 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 34);
                            p.get_skill_taq_new(34);
                        } else { // 91
                            String[] name_ = new String[] { "Nét vẽ cường hóa", "Nét vẽ phòng thủ",
                                    "Nét vẽ sức mạnh" };
                            int[] icon_ = new int[] { 51, 50, 49 };
                            Service.NewDialog_eat_taq(p, name_, icon_, 91);
                            p.get_skill_taq_new(91);
                        }
                        p.item.remove_item47(4, 86, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 34: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int idUp = p.data_yesno[0]; // ID rương muốn nâng cấp thành
                        int requiredStones = (idUp == 158) ? 100 : 50;
                        int successRate = (idUp == 158) ? 2 : 5;
                        int newId = idUp;
                        if (p.item.total_item_bag_by_id(7, 9) < requiredStones) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + requiredStones + " " + ItemTemplate7.get_it_by_id(9).name);
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.total_item_bag_by_id(4, idUp) < 1) {
                            Service.send_box_ThongBao_OK(p, "Bạn không có " + ItemTemplate4.get_it_by_id(idUp).name);
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (!p.payNgoc(100)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 100 Ruby");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hãy chừa ít nhất 1 ô trống trong hành trang");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.item.remove_item47(7, 9, requiredStones);
                        p.update_money();
                        boolean suc = Util.random(120) < successRate;
                        if (suc) {
                            p.item.remove_item47(4, idUp, 1);
                            if (idUp == 158) {
                                newId = 1045;
                            } else {
                                newId = 158;
                            }
                            p.item.add_item_bag47(4, newId, 1);
                        }
                        p.item.update_Inventory(-1, false);
                        Message m = new Message(45);
                        m.writer().writeByte(17);
                        m.writer().writeByte(suc ? 1 : 3);
                        m.writer().writeUTF("Nâng cấp " + ItemTemplate4.get_it_by_id(newId).name + " "
                                + (suc ? "thành công" : "thất bại"));
                        p.addmsg(m);
                        m.cleanup();
                    }
                    break;
                }
                case 33: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Skill_info sk_temp = p.get_skill_temp(p.data_yesno[0]);
                        if (sk_temp != null) {
                            if (sk_temp.lvdevil > 4) {
                                Service.send_box_ThongBao_OK(p,
                                        sk_temp.temp.name + " đã được cường hóa tối đa");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            int percent = (sk_temp.lvdevil == 0) ? 10 //
                                    : ((sk_temp.lvdevil == 1) ? 8 //
                                            : ((sk_temp.lvdevil == 2) ? 6 //
                                                    : ((sk_temp.lvdevil == 3) ? 5 : 4)));
                            if (!p.payNgoc(200)) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 200 Ruby");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item.total_item_bag_by_id(7, 9) < 50) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ " + ItemTemplate7.get_it_by_id(9).name);
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.item.remove_item47(7, 9, 50);
                            p.update_money();
                            p.item.update_Inventory(-1, false);
                            //
                            boolean suc = 50 > Util.random(120);
                            if (suc) {
                                if (!p.payVnd(5_000)) {
                                    Service.send_box_ThongBao_OK(p, "Không đủ 5,000 Extol để thực hiện");
                                } else {
                                    p.update_money();
                                    sk_temp.devilpercent += percent;
                                    if (sk_temp.devilpercent >= 100) {
                                        sk_temp.devilpercent = 0;
                                        sk_temp.lvdevil++;
                                    }
                                    p.send_skill();
                                    p.update_info_to_all();
                                }
                            }
                            //
                            Message m = new Message(45);
                            m.writer().writeByte(12);
                            m.writer().writeByte(suc ? 1 : 3);
                            m.writer().writeUTF(
                                    "Cường hóa kỹ năng " + (suc ? "thành công" : "thất bại"));
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                    break;
                }
                case 32: {
                    if (p.item_to_kham_ngoc != null) {
                        Item_wear it_select = p.item_to_kham_ngoc;
                        if (p.item.total_item_bag_by_id(4, 226) < 1) {
                            Service.send_box_ThongBao_OK(p, "Không đủ Đá Hải Thạch cấp 6");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (!p.payVnd(5_000)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.item.remove_item47(4, 226, 1);

                        p.update_money();
                        boolean suc = 25 > Util.random(150);
                        if (suc) {
                            it_select.valueKichAn = (byte) Util.random(13);
                            if (it_select.valueKichAn == 12) {
                                it_select.typelock = -1;
                            }
                            UpgradeDial.sendChatKTG_KichAn(p, it_select, 1);
                        }
                        Message m = new Message(-67);
                        m.writer().writeByte(20);
                        m.writer().writeUTF(suc ? "Kích ẩn thành công" : "Kích ẩn thất bại");
                        p.addmsg(m);
                        m.cleanup();
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 31: {
                    if (p.item_to_kham_ngoc != null && (p.item_to_kham_ngoc.template.typeEquip < 6
                            || p.item_to_kham_ngoc.template.typeEquip == 7)) {
                        if (!p.payVang(100000)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 100,000 beri");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.update_money();
                        p.item_to_kham_ngoc.valueChetac++;
                        if (p.item_to_kham_ngoc.valueChetac >= 100) {
                            p.item_to_kham_ngoc.valueChetac = 100;
                        }
                        //
                        Message m = new Message(-67);
                        m.writer().writeByte(25);
                        m.writer().writeUTF("Bạn phục hồi 1 điểm chế tác thành công");
                        p.addmsg(m);
                        m.cleanup();
                        //
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 30: {
                    if (p.item_to_kham_ngoc != null) {
                        Item_wear it_select = p.item_to_kham_ngoc;
                        if (it_select.isHoanMy == 1) {
                            Service.send_box_ThongBao_OK(p, "Trang bị này đã được hoàn mỹ rồi");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.total_item_bag_by_id(4, 226) < 1) {
                            Service.send_box_ThongBao_OK(p, "Không đủ Đá Hải Thạch cấp 6");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (!p.payVnd(5_000)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.item.remove_item47(4, 226, 1);

                        p.update_money();
                        boolean suc = 25 > Util.random(150);
                        if (suc) {
                            it_select.isHoanMy = 1;
                            UpgradeDial.sendChatKTG_HoanMy(p, it_select, 1);
                        }
                        Message m = new Message(-67);
                        m.writer().writeByte(20);
                        m.writer().writeUTF(suc ? "Hoàn mỹ thành công" : "Hoàn mỹ thất bại");
                        p.addmsg(m);
                        m.cleanup();
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 28: {
                    break;
                }
                case 27: {
                    break;
                }
                case 26: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        PotionMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item47.size(); j++) {
                                // Kiểm tra xem mặt hàng không phải là của người dùng đang mua
                                if (market.item47.get(j).index == p.data_yesno[1]
                                        && market.item47.get(j).seller.equals(p.name)) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Bạn không thể mua vật phẩm mà mình đang đăng bán.");
                                    return; // Thoát khỏi phương thức sau khi gửi thông báo
                                }
                                if (market.item47.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item47.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market > System.currentTimeMillis()) {
                            if (!it_receive.is_processing.compareAndSet(false, true)) {
                                Service.send_box_ThongBao_OK(p, "Vật phẩm đang được xử lý, vui lòng thử lại sau.");
                                return;
                            }
                            try {
                                boolean isBeri = (it_receive.category == 4 && it_receive.id == 0);
                                if (isBeri || (p.item.able_bag() > 0 && (p.item
                                        .total_item_bag_by_id(it_receive.category, it_receive.id)
                                        + it_receive.quant) <= DataTemplate.MAX_ITEM_IN_BAG)) {
                                    if (!p.payVnd(it_receive.price_market)) {
                                        if (it_receive.category == 4) {
                                            if (it_receive.id == 0) {
                                                Service.send_box_ThongBao_OK(p,
                                                        "Bạn không đủ "
                                                                + Util.number_format(
                                                                        it_receive.price_market)
                                                                + " extol để mua " + it_receive.quant
                                                                + " triệu beri");
                                            } else {
                                                Service.send_box_ThongBao_OK(p, "Bạn không đủ "
                                                        + Util.number_format(it_receive.price_market)
                                                        + " extol để mua " + it_receive.quant + " "
                                                        + ItemTemplate4.get_item_name(it_receive.id));
                                            }
                                        } else {
                                            Service.send_box_ThongBao_OK(p,
                                                    "Bạn không đủ "
                                                            + Util.number_format(it_receive.price_market)
                                                            + " extol để mua " + it_receive.quant + " "
                                                            + ItemTemplate7.get_item_name(it_receive.id));
                                        }
                                        p.data_yesno = null;
                                        p.map_tele = null;
                                        return;
                                    }
                                    it_receive.time_market = 0;
                                    it_receive.type_market = 2;
                                    if (!isBeri) {
                                        p.update_money();
                                    }
                                    if (it_receive.category == 4) {
                                        if (it_receive.id == 0) { // beri
                                            long beri_add = 1_000_000L * it_receive.quant;
                                            p.update_vang(beri_add);
                                            p.update_money();
                                            Service.send_box_ThongBao_OK(p,
                                                    "Mua thành công " + it_receive.quant
                                                            + " triệu beri với giá "
                                                            + Util.number_format(
                                                                    it_receive.price_market)
                                                            + " extol");
                                        } else {
                                            p.item.add_item_bag47(it_receive.category, it_receive.id,
                                                    it_receive.quant);
                                            Service.send_box_ThongBao_OK(p,
                                                    "Mua thành công " + it_receive.quant + " "
                                                            + ItemTemplate4.get_item_name(it_receive.id)
                                                            + " với giá "
                                                            + Util.number_format(
                                                                    it_receive.price_market)
                                                            + " extol");
                                        }
                                    } else {
                                        p.item.add_item_bag47(it_receive.category, it_receive.id,
                                                it_receive.quant);
                                        Service.send_box_ThongBao_OK(p,
                                                "Mua thành công " + it_receive.quant + " "
                                                        + ItemTemplate7.get_item_name(it_receive.id)
                                                        + " với giá "
                                                        + Util.number_format(it_receive.price_market)
                                                        + " extol");
                                    }
                                    LogUtil.logTransaction(p, it_receive, isBeri);

                                    p.item.update_Inventory(-1, false);
                                    Market.update_at_market_index(p, market.type);
                                    Market.update_at_market_index(p, 3);
                                    Player.flush(p, true);
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ");
                                }
                            } finally {
                                it_receive.is_processing.set(false);
                            }
                        }
                    }
                    break;
                }
                case 25: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        ItemMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item3.size(); j++) {
                                // Kiểm tra xem mặt hàng không phải là của người dùng đang mua
                                if (market.item3.get(j).index == p.data_yesno[1]
                                        && market.item3.get(j).seller.equals(p.name)) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Bạn không thể mua vật phẩm mà mình đang đăng bán.");
                                    return; // Thoát khỏi phương thức sau khi gửi thông báo
                                }
                                if (market.item3.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item3.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market > System.currentTimeMillis()) {
                            if (!it_receive.is_processing.compareAndSet(false, true)) {
                                Service.send_box_ThongBao_OK(p, "Vật phẩm đang được xử lý, vui lòng thử lại sau.");
                                return;
                            }
                            try {
                                if (p.item.able_bag() > 0) {
                                    if (!p.payVnd(it_receive.price_market)) {
                                        Service.send_box_ThongBao_OK(p,
                                                "Bạn không đủ "
                                                        + Util.number_format(it_receive.price_market)
                                                        + " extol để mua " + it_receive.template.name);
                                        p.data_yesno = null;
                                        p.map_tele = null;
                                        return;
                                    }
                                    Item_wear it_add = new Item_wear();
                                    it_add.clone_obj(it_receive);
                                    if (it_add.template != null) {
                                        it_receive.time_market = 0;
                                        it_receive.type_market = 2;
                                        p.update_money();
                                        //
                                        p.item.add_item_bag3(it_add);
                                        p.item.update_Inventory(-1, false);
                                    }
                                    Service.send_box_ThongBao_OK(p,
                                            "Mua thành công " + it_receive.template.name + " với giá "
                                                    + Util.number_format(it_receive.price_market)
                                                    + " extol");
                                    Market.update_at_market_index(p, market.type);
                                    Market.update_at_market_index(p, 3);
                                    Player.flush(p, true);
                                    //
                                    LogUtil.logTransaction(p, it_receive);

                                } else {
                                    Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ");
                                }
                            } finally {
                                it_receive.is_processing.set(false);
                            }
                        }
                    }
                    break;
                }
                case 24: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        PotionMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item47.size(); j++) {
                                if (market.item47.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item47.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market > System.currentTimeMillis()) {
                            it_receive.time_market = 0;
                            it_receive.type_market = 3;
                            Market.update_at_market_index(p, market.type);
                            Market.update_at_market_index(p, 3);
                            if (it_receive.category == 4) {
                                if (it_receive.id == 0) {
                                    Service.send_box_ThongBao_OK(p, "Hủy bán " + it_receive.quant
                                            + " triệu beri thành công");
                                } else {
                                    Service.send_box_ThongBao_OK(p,
                                            "Hủy bán " + it_receive.quant + " "
                                                    + ItemTemplate4.get_item_name(it_receive.id)
                                                    + " thành công");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Hủy bán " + it_receive.quant + " "
                                                + ItemTemplate7.get_item_name(it_receive.id)
                                                + " thành công");
                            }
                        }
                    }
                    break;
                }
                case 23: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        ItemMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item3.size(); j++) {
                                if (market.item3.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item3.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market < System.currentTimeMillis()) {
                            if (!p.payVnd(1_500)) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ 1.500 extol để đăng bán vật phẩm");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_money();
                            it_receive.time_market = System.currentTimeMillis() + 60_000L * 60 * 24;
                            it_receive.type_market = 1;
                            Service.send_box_ThongBao_OK(p, "Gia hạn thêm 24h cho "
                                    + it_receive.template.name + " thành công");
                            Market.update_at_market_index(p, market.type);
                            Market.update_at_market_index(p, 3);
                            Player.flush(p, true);
                        }
                    }
                    break;
                }
                case 22: {
                    if (p.data_yesno != null && p.data_yesno.length == 4) {
                        if (!p.payVnd(2_000)) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ 2000 extol để đăng bán vật phẩm");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        Market getMarket = null;
                        if (p.data_yesno[0] == 4) {
                            if (p.item.total_item_bag_by_id(p.data_yesno[0],
                                    p.data_yesno[1]) < p.data_yesno[2]) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ " + p.data_yesno[2] + " "
                                                + ItemTemplate4.get_item_name(p.data_yesno[1])
                                                + " để đăng bán");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (Market.check_it_47_cant_sell(4, p.data_yesno[1])) {
                                Service.send_box_ThongBao_OK(p, "Không thể đăng bán vật phẩm này");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            getMarket = Market.get_list_by_type(6);
                        } else if (p.data_yesno[0] == 7) {
                            if (p.item.total_item_bag_by_id(p.data_yesno[0],
                                    p.data_yesno[1]) < p.data_yesno[2]) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ " + p.data_yesno[2] + " "
                                                + ItemTemplate7.get_item_name(p.data_yesno[1])
                                                + " để đăng bán");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (Market.check_it_47_cant_sell(7, p.data_yesno[1])) {
                                Service.send_box_ThongBao_OK(p, "Không thể đăng bán vật phẩm này");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            getMarket = Market.get_list_by_type(5);
                        }
                        if (getMarket != null) {
                            PotionMarket it_add = new PotionMarket();
                            it_add.index = Market.get_index();
                            it_add.id = (short) p.data_yesno[1];
                            it_add.category = (byte) p.data_yesno[0];
                            it_add.quant = (short) p.data_yesno[2];
                            it_add.time_market = System.currentTimeMillis() + 60_000L * 60 * 24;
                            it_add.price_market = p.data_yesno[3];
                            it_add.seller = p.name;
                            it_add.type_market = 1;
                            if (it_add.index != -1) {
                                p.update_money();
                                p.item.remove_item47(p.data_yesno[0], p.data_yesno[1],
                                        p.data_yesno[2]);
                                p.item.update_Inventory(-1, false);
                                //
                                getMarket.item47.add(it_add);
                                Market.update_at_market_index(p, getMarket.type);
                                Market.update_at_market_index(p, 3);
                                Player.flush(p, true);
                                if (p.data_yesno[0] == 4) {
                                    Service.send_box_ThongBao_OK(p,
                                            p.data_yesno[2] + " "
                                                    + ItemTemplate4.get_item_name(p.data_yesno[1])
                                                    + " đã được đăng bán với giá "
                                                    + Util.number_format(p.data_yesno[3])
                                                    + " thành công lên chợ");
                                } else if (p.data_yesno[0] == 7) {
                                    Service.send_box_ThongBao_OK(p,
                                            p.data_yesno[2] + " "
                                                    + ItemTemplate7.get_item_name(p.data_yesno[1])
                                                    + " đã được đăng bán với giá "
                                                    + Util.number_format(p.data_yesno[3])
                                                    + " thành công lên chợ");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Chợ mua bán có quá nhiều vật phẩm, không thể đăng thêm");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra hãy thử lại");
                        }
                    }
                    break;
                }
                case 21: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        PotionMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item47.size(); j++) {
                                if (market.item47.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item47.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market < System.currentTimeMillis()) {
                            if (!it_receive.is_processing.compareAndSet(false, true)) {
                                Service.send_box_ThongBao_OK(p, "Đang xử lý, vui lòng đợi...");
                                break;
                            }
                            try {
                                if (it_receive.type_market == 2) {
                                    p.refresh_vnd(); // Sync RAM with latest DB balance before adding
                                    int price_receive = (int) (((long) it_receive.price_market * 90L) / 100L);
                                    p.update_vnd(price_receive);
                                    p.update_money();
                                    if (it_receive.category == 4) {
                                        Service.send_box_ThongBao_OK(p,
                                                "Nhận " + price_receive + " extol (phí 10%) tiền bán "
                                                        + it_receive.quant + " "
                                                        + ItemTemplate4.get_item_name(it_receive.id));
                                    } else if (it_receive.category == 7) {
                                        Service.send_box_ThongBao_OK(p,
                                                "Nhận " + price_receive + " extol (phí 10%) tiền bán "
                                                        + it_receive.quant + " "
                                                        + ItemTemplate7.get_item_name(it_receive.id));
                                    }
                                    market.item47.remove(it_receive);
                                } else {
                                    if (it_receive.category == 4) {
                                        if (it_receive.id == 0) {
                                            long beri_add = 1_000_000L * it_receive.quant;
                                            p.update_vang(beri_add);
                                            p.update_money();
                                            Service.send_box_ThongBao_OK(p, "Nhận " + it_receive.quant
                                                    + " " + " triệu beri về hành trang");
                                        } else {
                                            if (p.item.add_item_bag47(4, it_receive.id,
                                                    it_receive.quant)) {
                                                p.item.update_Inventory(-1, false);
                                                Service.send_box_ThongBao_OK(p,
                                                        "Nhận " + it_receive.quant + " "
                                                                + ItemTemplate4
                                                                        .get_item_name(it_receive.id)
                                                                + " về hành trang");
                                            } else {
                                                Service.send_box_ThongBao_OK(p,
                                                        "Hành trang không đủ chỗ trống");
                                                p.data_yesno = null;
                                                p.map_tele = null;
                                                return;
                                            }
                                        }
                                    } else if (it_receive.category == 7) {
                                        if (p.item.add_item_bag47(7, it_receive.id, it_receive.quant)) {
                                            p.item.update_Inventory(-1, false);
                                            Service.send_box_ThongBao_OK(p,
                                                    "Nhận " + it_receive.quant + " "
                                                            + ItemTemplate7.get_item_name(it_receive.id)
                                                            + " về hành trang");
                                        } else {
                                            Service.send_box_ThongBao_OK(p,
                                                    "Hành trang không đủ chỗ trống");
                                            p.data_yesno = null;
                                            p.map_tele = null;
                                            return;
                                        }
                                    }
                                    market.item47.remove(it_receive);
                                }
                                Market.update_at_market_index(p, market.type);
                                Market.update_at_market_index(p, 3);
                                Player.flush(p, true);
                            } finally {
                                it_receive.is_processing.set(false); // chống bug
                            }
                        }
                    }
                    break;
                }
                case 20: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        ItemMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item3.size(); j++) {
                                if (market.item3.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item3.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market > System.currentTimeMillis()) {
                            it_receive.time_market = 0;
                            it_receive.type_market = 3;
                            Market.update_at_market_index(p, market.type);
                            Market.update_at_market_index(p, 3);
                            Service.send_box_ThongBao_OK(p,
                                    "Hủy bán " + it_receive.template.name + " thành công");
                        }
                    }
                    break;
                }
                case 19: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        ItemMarket it_receive = null;
                        Market market = Market.get_list_by_type(p.data_yesno[0]);
                        if (market != null) {
                            for (int j = 0; j < market.item3.size(); j++) {
                                if (market.item3.get(j).index == p.data_yesno[1]) {
                                    it_receive = market.item3.get(j);
                                    break;
                                }
                            }
                        }
                        if (it_receive != null
                                && it_receive.time_market < System.currentTimeMillis()) {
                            if (!it_receive.is_processing.compareAndSet(false, true)) {
                                Service.send_box_ThongBao_OK(p, "Đang xử lý, vui lòng đợi...");
                                break;
                            }
                            try {
                                market.item3.remove(it_receive);
                                if (it_receive.type_market == 2) {
                                    p.refresh_vnd(); // Sync RAM with latest DB balance before adding
                                    int price_receive = (int) (((long) it_receive.price_market * 90L) / 100L);
                                    p.update_vnd(price_receive);
                                    p.update_money();
                                    Service.send_box_ThongBao_OK(p, "Nhận " + price_receive
                                            + " extol (phí 10%) tiền bán " + it_receive.template.name);
                                } else {
                                    Item_wear it_add = new Item_wear();
                                    it_add.clone_obj(it_receive);
                                    if (it_add.template != null) {
                                        p.item.add_item_bag3(it_add);
                                        p.item.update_Inventory(-1, false);
                                    }
                                    Service.send_box_ThongBao_OK(p,
                                            "Nhận " + it_receive.template.name + " về hành trang");
                                }
                                Market.update_at_market_index(p, 3);
                                Player.flush(p, true);
                            } finally {
                                it_receive.is_processing.set(false);
                            }
                        }
                    }
                    break;
                }
                case 18: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        if (!p.payVnd(2_000)) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ 2000 extol để đăng bán vật phẩm");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        long beri_add = 1_000_000L * p.data_yesno[0];
                        if (beri_add > 32_000_000_000L || beri_add < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Số beri phải lớn hơn 0 và nhỏ hơn 32000");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        Market getMarket = Market.get_list_by_type(6);
                        if (getMarket != null) {
                            if (!p.payVang(beri_add)) {
                                Service.send_box_ThongBao_OK(p, "Bạn không đủ "
                                        + Util.number_format(beri_add) + " beri để đăng bán");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_money();
                            PotionMarket it_add = new PotionMarket();
                            it_add.index = Market.get_index();
                            it_add.id = 0;
                            it_add.category = 4;
                            it_add.quant = (short) p.data_yesno[0];
                            it_add.time_market = System.currentTimeMillis() + 60_000L * 60 * 24;
                            it_add.price_market = p.data_yesno[1];
                            it_add.seller = p.name;
                            it_add.type_market = 1;
                            if (it_add.index != -1) {
                                getMarket.item47.add(it_add);
                                Market.update_at_market_index(p, 3);
                                Market.update_at_market_index(p, 6);
                                Player.flush(p, true);
                                Service.send_box_ThongBao_OK(p,
                                        p.data_yesno[0] + " triệu beri đã được đăng bán với giá "
                                                + Util.number_format(p.data_yesno[1])
                                                + " thành công lên chợ");
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Chợ mua bán có quá nhiều vật phẩm, không thể đăng thêm");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra hãy thử lại");
                        }
                    }
                    break;
                }
                case 17: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        Item_wear it_select = p.item.bag3[p.data_yesno[0]];
                        if (it_select != null) {
                            if (!p.payVnd(2000)) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ 2000 extol để đăng bán vật phẩm");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            } else {
                                p.update_money();
                            }
                            int type_market = (it_select.template.typeEquip == 0
                                    || it_select.template.typeEquip == 1
                                    || it_select.template.typeEquip == 7)
                                            ? 0
                                            : ((it_select.template.typeEquip == 3
                                                    || it_select.template.typeEquip == 5)
                                                            ? 1
                                                            : 2);
                            Market getMarket = Market.get_list_by_type(type_market);
                            if (getMarket != null) {
                                ItemMarket it_add = new ItemMarket();
                                it_add.clone_from_item_wear(it_select);
                                it_add.time_market = System.currentTimeMillis() + 60_000L * 60 * 24;
                                it_add.price_market = p.data_yesno[1];
                                it_add.seller = p.name;
                                if (it_add.index != -1) {
                                    //
                                    getMarket.item3.add(it_add);
                                    p.item.remove_item_wear(it_select);
                                    p.item.update_Inventory(-1, false);
                                    Market.update_at_market_index(p, 3);
                                    Market.update_at_market_index(p, type_market);
                                    Player.flush(p, true);
                                    Service.send_box_ThongBao_OK(p,
                                            it_select.template.name + " đã được đăng bán với giá "
                                                    + Util.number_format(p.data_yesno[1])
                                                    + " thành công lên chợ");
                                } else {
                                    Service.send_box_ThongBao_OK(p,
                                            "Chợ mua bán có quá nhiều vật phẩm, không thể đăng thêm");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra hãy thử lại");
                            }
                        }
                    }
                    break;
                }
                case 16: {
                    if (p.map_boss_info != null) {
                        if (!p.payTicket(5)) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5 bánh mì");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.update_money();
                        p.map.leave_map(p, 2);
                        // create map boss
                        Map map_boss = new Map();
                        map_boss.template = p.map_boss_info.map.template;
                        map_boss.zone_id = (byte) 0;
                        map_boss.list_mob = new int[0];
                        p.map_boss_info.mob = new ArrayList<>();
                        for (int i = 0; i < p.map_boss_info.map.list_mob.length; i++) {
                            Mob temp = Mob.ENTRYS.get(p.map_boss_info.map.list_mob[i]);
                            Mob mob_add = new Mob();
                            mob_add.mob_template = temp.mob_template;
                            mob_add.x = temp.x;
                            mob_add.y = temp.y;
                            mob_add.hp_max = temp.mob_template.hp_max + Body.Point3_Template_hp[p.level];
                            mob_add.hp = mob_add.hp_max;
                            mob_add.level = p.level;
                            mob_add.isdie = false;
                            mob_add.id_target = -1;
                            mob_add.index = -2;
                            mob_add.map = map_boss;
                            // Mob.ENTRYS.put(this.index_mob, mob_add);
                            p.map_boss_info.mob.add(mob_add);
                            // this.index_mob++;
                        }
                        MapBossInfo.add(p.map_boss_info);
                        //
                        p.map_boss_info.map = map_boss;
                        p.map = p.map_boss_info.map;
                        p.x = p.map_boss_info.x_new;
                        p.y = p.map_boss_info.y_new;
                        p.xold = p.x;
                        p.yold = p.y;
                        p.map.goto_map(p);
                        Service.update_PK(p, p, true);
                        Service.pet(p, p, true);
                        Quest.update_map_have_side_quest(p, true);
                        //
                        map_boss.start_map();
                        Map.add_map_plus(map_boss);
                    }
                    break;
                }
                case 15: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        QuestP temp = p.get_quest(p.data_yesno[0]);
                        if (temp == null || temp.template.equals(Quest.QUEST_FINISH)) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn đã hoàn thành hết nhiệm vụ hiện tại");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (temp != null) {
                            if (p.get_ticket() < 3) {
                                Service.send_box_ThongBao_OK(p, "Bạn không đủ 3 bánh mì để nhận");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_ticket(-3);
                            p.update_money();
                            Service.CountDown_Ticket(p);
                            //
                            // remove quest now
                            Quest.remove_old_and_send_next(p, temp);
                            // send dialog quest new
                            Message m = new Message(-23);
                            m.writer().writeByte(5);
                            m.writer().writeShort(temp.template.index);
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                    break;
                }
                case 14: {
                    if (p.isdie) {
                        if (p.time_can_hs < 1) {
                            Service.send_box_ThongBao_OK(p, "Đã hết số lần hồi sinh tại chỗ!");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.pointPk < 1) {
                            if (p.get_vang() < 500) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 500 Beri");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_vang(-500);
                        } else {
                            if (p.get_ngoc() < 200) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 200 Ruby");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_ngoc(-200);
                        }
                        p.time_can_hs--;
                        p.update_money();
                        p.time_can_mob_atk = System.currentTimeMillis() + 1200L;
                        Service.use_potion(p, 0, p.body.get_hp_max(true));
                        Service.use_potion(p, 1, p.body.get_mp_max(true));
                        p.isdie = false;
                        Service.send_revive_player(p);
                    }
                    break;
                }
                case 13: {
                    if (p.data_yesno != null && p.data_yesno.length == 2) {
                        ItemTemplate4 it_temp1 = ItemTemplate4.get_it_by_id(p.data_yesno[0]);
                        ItemTemplate4 it_temp2 = ItemTemplate4.get_it_by_id(p.data_yesno[1]);
                        if (it_temp1 != null && it_temp2 != null) {
                            if (it_temp1.id == it_temp2.id) {
                                Service.send_box_ThongBao_OK(p,
                                        "Đá siêu cấp và đá nguyên liệu phải khác nhau");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item.able_bag() < 1) {
                                Service.send_box_ThongBao_OK(p,
                                        "Hành trang phải chừa ít nhất 1 ô trống");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item.total_item_bag_by_id(4, it_temp1.id) < 1) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không đủ 1 " + ItemTemplate4.get_item_name(it_temp1.id));
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item.total_item_bag_by_id(4, it_temp2.id) < 2) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không đủ 2 " + ItemTemplate4.get_item_name(it_temp2.id));
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            boolean suc = (p.percent_da_sieu_cap) > Util.random(100);
                            p.item.remove_item47(4, p.data_yesno[1], suc ? 2 : 1);
                            if (suc) {
                                p.item.remove_item47(4, p.data_yesno[0], 1);
                                if (p.data_yesno[0] == 367) { // ho phach
                                    switch (p.data_yesno[1]) {
                                        case 55: {
                                            p.item.add_item_bag47(4, 369, 1);
                                            break;
                                        }
                                        case 61: {
                                            p.item.add_item_bag47(4, 370, 1);
                                            break;
                                        }
                                        case 67: {
                                            p.item.add_item_bag47(4, 371, 1);
                                            break;
                                        }
                                        case 73: {
                                            p.item.add_item_bag47(4, 372, 1);
                                            break;
                                        }
                                        case 79: {
                                            p.item.add_item_bag47(4, 373, 1);
                                            break;
                                        }
                                        default: { // 49
                                            p.item.add_item_bag47(4, 368, 1);
                                            break;
                                        }
                                    }
                                } else {
                                    p.item.add_item_bag47(4, Rebuild_Item.get_id_ngoc_sieu_cap(
                                            p.data_yesno[0], p.data_yesno[1]), 1);
                                }
                                p.percent_da_sieu_cap = 50;
                                //
                                Message m = new Message(-67);
                                m.writer().writeByte(27);
                                m.writer().writeUTF("Nâng cấp thành công");
                                p.addmsg(m);
                                m.cleanup();
                            } else {
                                p.percent_da_sieu_cap += 10;
                                Message m = new Message(-67);
                                m.writer().writeByte(30);
                                m.writer().writeUTF("Quá trình nâng cấp thất bại!");
                                p.addmsg(m);
                                m.cleanup();
                            }
                            p.item.update_Inventory(-1, false);
                        }
                    }
                    break;
                }
                case 12: {
                    break;
                }

                case 11: {
                    if (p.isTachTB && p.data_yesno != null && p.data_yesno.length == 1) {
                        Item_wear it_select = p.item.bag3[p.data_yesno[0]];
                        if (it_select != null) {
                            byte id_7 = 2;
                            if (it_select.template.color == 2) {
                                id_7 = 3;
                            } else if (it_select.template.color == 3) {
                                id_7 = 4;
                            }
                            //
                            p.item.bag3[p.data_yesno[0]] = null;
                            p.item.add_item_bag47(7, id_7, 1);
                            p.item.update_Inventory(-1, false);
                            //
                            Message m = new Message(-50);
                            m.writer().writeByte(0);
                            m.writer().writeByte(1);
                            m.writer().writeUTF("Thành công");
                            m.writer().writeShort(id_7);
                            m.writer().writeByte(7);
                            m.writer().writeShort(1);
                            p.addmsg(m);
                            m.cleanup();
                        }
                        p.isTachTB = false;
                    }
                    break;
                }
                case 10: {
                    int extol = p.data_yesno[0];
                    if (p.get_vnd() < extol) {
                        Service.send_box_ThongBao_OK(p,
                                "Bạn không đủ " + Util.number_format(extol) + " Extol");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    int ruby = extol * 100;
                    p.update_vnd(-extol);
                    p.update_ngoc(ruby);
                    p.update_money();
                    Service.send_box_ThongBao_OK(p,
                            "Bạn đã đổi thành công " + Util.number_format(extol) + " Extol ra "
                                    + Util.number_format(ruby) + " Ruby.");
                    break;
                }
                case 101: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int coin = p.data_yesno[0];
                        if (p.get_coin() < coin) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + Util.number_format(coin) + " Coin");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        long beri = (long) coin * 10_000L;
                        if (p.update_coin(-coin)) {
                            p.update_vang(beri);
                            Player.flush(p, true);
                            p.update_money();
                            Service.send_box_ThongBao_OK(p,
                                    "Đổi thành công \n" + Util.number_format(coin) + " Coin sang "
                                            + Util.number_format(beri) + " Beri.");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lỗi trừ Coin!");
                        }
                    }
                    break;
                }
                case 102: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int coin = p.data_yesno[0];
                        if (p.get_coin() < coin) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + Util.number_format(coin) + " Coin");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        int ruby = coin * 1000;
                        if (p.update_coin(-coin)) {
                            p.update_ngoc(ruby);
                            Player.flush(p, true);
                            p.update_money();
                            Service.send_box_ThongBao_OK(p,
                                    "Đổi thành công \n" + Util.number_format(coin) + " Coin sang "
                                            + Util.number_format(ruby) + " Ruby.");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lỗi trừ Coin!");
                        }
                    }
                    break;
                }
                case 9: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int coin = p.data_yesno[0];
                        if (p.get_coin() < coin) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + Util.number_format(coin) + " Coin");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        int extol = coin * 1;
                        if (p.update_coin(-coin)) {
                            p.update_vnd(extol);
                            Player.flush(p, true);
                            p.update_money();
                            Service.send_box_ThongBao_OK(p,
                                    "Đổi thành công \n" + Util.number_format(coin) + " Coin sang "
                                            + Util.number_format(extol) + " Extol.");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lỗi trừ Coin!");
                        }
                    }
                    break;
                }
                // --- End of Removed Case 103 ---

                case 8: {
                    if (p.item_chuyenhoa_save_0 != null && p.item_chuyenhoa_save_1 != null
                            && p.data_yesno != null && p.data_yesno.length == 1) {
                        if (p.item.total_item_bag_by_id(4, p.data_yesno[0]) < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Không đủ " + ItemTemplate4.get_item_name(p.data_yesno[0]));
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.item_chuyenhoa_save_1.levelup = p.item_chuyenhoa_save_0.levelup;
                        p.item_chuyenhoa_save_0.levelup = 0;

                        p.item.remove_item47(4, p.data_yesno[0], 1);
                        p.item.update_Inventory(-1, false);
                        ChuyenHoa.show_result(p,
                                "== Chuyển Hóa Hoàn Tất ==\n" + p.item_chuyenhoa_save_1.template.name
                                        + "\n Số cường hóa mới là "
                                        + p.item_chuyenhoa_save_1.levelup
                                        + "\n Dùng : " + ItemTemplate4.get_item_name(p.data_yesno[0]),
                                p.item_chuyenhoa_save_1.levelup);
                    }
                    break;
                }
                case 6: {
                    if (p.item_chuyenhoa_save_0 != null && p.item_chuyenhoa_save_1 != null
                            && p.item_chuyenhoa_save_0.levelup > p.item_chuyenhoa_save_1.levelup) {

                        if (p.get_ngoc() < 800) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 800 Ruby");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        int levelBanDau = p.item_chuyenhoa_save_0.levelup; // Lưu trữ cấp độ ban đầu
                        int random = Util.random(1, 120);
                        p.item_chuyenhoa_save_1.levelup = p.item_chuyenhoa_save_0.levelup;
                        p.item_chuyenhoa_save_0.levelup = 0;

                        if (random < 25) {
                            p.item_chuyenhoa_save_1.levelup -= 1;
                        } else if (random < 50) {
                            p.item_chuyenhoa_save_1.levelup -= 2;
                        } else if (random < 85) {
                            p.item_chuyenhoa_save_1.levelup -= 3;
                        }

                        if (p.item_chuyenhoa_save_1.levelup < 0) {
                            p.item_chuyenhoa_save_1.levelup = 0;
                        }

                        int soCuongHoaBiTru = levelBanDau - p.item_chuyenhoa_save_1.levelup; // Tính số cường hóa bị trừ

                        p.update_ngoc(-800);
                        p.update_money();
                        p.item.update_Inventory(-1, false);

                        ChuyenHoa.show_result(p,
                                "== Chuyển Hóa Hoàn Tất ==\n" + p.item_chuyenhoa_save_1.template.name
                                        + "\n Số cường hóa mới là "
                                        + p.item_chuyenhoa_save_1.levelup
                                        + "\n Số cấp bị trừ là " + soCuongHoaBiTru,
                                p.item_chuyenhoa_save_1.levelup);
                    }
                    break;
                }

                case 5: {
                    if (p.ship_pet != null) {
                        Service.send_box_ThongBao_OK(p, "Bạn đang vận chuyển hàng");
                    } else {
                        if (p.map_tele != null && p.data_yesno != null && p.data_yesno.length == 1
                                && p.data_yesno[0] < p.map_tele.length) {
                            Map[] map_go = Map.get_map_by_id(p.map_tele[p.data_yesno[0]]);
                            if (p.map.template.id == map_go[0].template.id) {
                                Service.send_box_ThongBao_OK(p, "Đang ở map này rồi!");
                            } else if (p.get_vang() >= 200) {
                                p.update_vang(-200);
                                p.update_money();
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
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
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không đủ 200 Beri");
                            }
                        }
                    }
                    break;
                }
                case 4: {
                    if (p.map_tele == null && p.data_yesno != null && p.data_yesno.length == 1) {
                        Skill_info temp = p.skill_point.get(p.data_yesno[0]);
                        if (temp.temp.ID >= 1000 && temp.temp.ID < 2000 && temp.temp.Lv_RQ > 0) {
                            if (p.get_ngoc() >= 2) {
                                p.update_ngoc(-2);
                                p.update_money();
                                Skill_Template.reset_skill(temp);
                                p.send_skill();
                                p.update_info_to_all();
                                Service.send_box_ThongBao_OK(p,
                                        "Xóa kỹ năng " + temp.temp.name + " thành công");
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ 2 Ruby. Phí xóa kỹ năng này là 2 ruby!");
                            }
                        }
                    }
                    break;
                }
                case 3: {
                    if (p.map_tele == null && p.data_yesno != null && p.data_yesno.length == 1
                            && p.data_yesno[0] < p.skill_point.size()) {
                        Skill_info temp = p.skill_point.get(p.data_yesno[0]);
                        if (temp != null && temp.temp.ID < 2000) {
                            if (temp.temp.ID >= 1000) {
                                int dem = 0;
                                for (int i2 = 0; i2 < p.skill_point.size(); i2++) {
                                    Skill_info temp2 = p.skill_point.get(i2);
                                    if (temp2.temp.ID >= 1000 && temp2.temp.ID < 2000
                                            && temp2.temp.typeSkill == 3 && temp2.temp.Lv_RQ > 0) {
                                        dem++;
                                    }
                                }
                                if (temp.temp.Lv_RQ == -1 && temp.temp.typeSkill == 3
                                        && dem >= p.getNumPassive()) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Bạn đã học tối đa " + dem + " / " + p.getNumPassive()
                                                    + " chiêu nội tại, hãy up level để mở thêm!");
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    return;
                                } else if (temp.temp.Lv_RQ != -1) {
                                    if (temp.temp.Lv_RQ >= 5) {
                                        Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra");
                                        p.data_yesno = null;
                                        p.map_tele = null;
                                        return;
                                    } else {
                                        int index_new = temp.temp.indexSkillInServer + 1;
                                        temp = new Skill_info();
                                        temp.temp = Skill_Template.get_temp(index_new, 0);
                                        temp.exp = 0;
                                        temp.lvdevil = 0;
                                        temp.devilpercent = 0;
                                    }
                                }
                            }
                            Learn_Skill.request_learn_new_skill(p, temp);
                        }
                    }
                    break;
                }
                case 2: {
                    if (p.get_ngoc() < 500) {
                        Service.send_box_ThongBao_OK(p,
                                "Bạn không đủ 500 Ruby.");
                        p.data_yesno = null;
                        p.map_tele = null;
                        return;
                    }
                    p.update_ngoc(-500);
                    p.update_money();
                    p.reset_point(0);
                    p.reset_point(1);
                    Service.send_box_ThongBao_OK(p, "Tẩy điểm tiềm năng thành công");
                    break;
                }
                case 4032: {
                    if (p.item.total_item_bag_by_id(4, 32) > 0) {
                        String[] name_ = new String[] { "Sức mạnh của lửa", "Hỏa quyền", "Nắm đấm lửa" };
                        int[] icon_ = new int[] { 32, 30, 29 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 32, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4033: {
                    if (p.item.total_item_bag_by_id(4, 33) > 0) {
                        String[] name_ = new String[] { "Sức sống bất diệt", "Chất bất ổn", "Súng máy caosu" };
                        int[] icon_ = new int[] { 34, 33, 31 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 33, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 999: { // outclan via chat command
                    if (p.clan != null) {
                        // Kiểm tra thuyền trưởng
                        if (p.clan.members.size() > 0 && p.clan.members.get(0).name.equals(p.name)) {
                            Service.send_box_ThongBao_OK(p, "Thuyền trưởng không thể rời băng!");
                            break;
                        }
                        Clan.process(p, null, 4);
                    }
                    break;
                }
                case 4034: {
                    if (p.item.total_item_bag_by_id(4, 34) > 0) {
                        String[] name_ = new String[] { "Tiến hóa", "Thuốc tăng trưởng", "Hóa tuần lộc" };
                        int[] icon_ = new int[] { 37, 36, 35 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 34, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4088: {
                    if (p.item.total_item_bag_by_id(4, 88) > 0) {
                        String[] name_ = new String[] { "Khói bất tử", "Khói tốc độ", "Mưa khói" };
                        int[] icon_ = new int[] { 38, 39, 40 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 88, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4090: {
                    if (p.item.total_item_bag_by_id(4, 90) > 0) {
                        String[] name_ = new String[] { "Bản năng thủ lĩnh", "Hóa bò tót", "Bất khuất" };
                        int[] icon_ = new int[] { 48, 47, 46 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 90, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4091: {
                    if (p.item.total_item_bag_by_id(4, 91) > 0) {
                        String[] name_ = new String[] { "Nét vẽ cường hóa", "Nét vẽ phòng thủ",
                                "Nét vẽ sức mạnh" };
                        int[] icon_ = new int[] { 51, 50, 49 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 91, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4092: {
                    if (p.item.total_item_bag_by_id(4, 92) > 0) {
                        String[] name_ = new String[] { "Băng vĩnh cửu", "Mưa băng", "Tuyết tê tái" };
                        int[] icon_ = new int[] { 57, 56, 53 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 92, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4093: {
                    if (p.item.total_item_bag_by_id(4, 93) > 0) {
                        String[] name_ = new String[] { "Cát lưu động", "Bão cát sa mạc", "Cát linh động" };
                        int[] icon_ = new int[] { 55, 54, 52 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 93, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4160: {
                    if (p.item.total_item_bag_by_id(4, 160) > 0) {
                        String[] name_ = new String[] { "Sấm chớp rền vang", "Lôi phạt",
                                "Bùng nổ sức mạnh", "Ý chí thần sấm" };
                        int[] icon_ = new int[] { 61, 60, 59, 58 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 160, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4161: {
                    if (p.item.total_item_bag_by_id(4, 161) > 0) {
                        String[] name_ = new String[] { "Bão nham thạch", "Cột lửa", "Bùng cháy",
                                "Nỗi đau bỏng cháy" };
                        int[] icon_ = new int[] { 65, 64, 63, 62 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 161, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4219: {
                    if (p.item.total_item_bag_by_id(4, 219) > 0) {
                        String[] name_ = new String[] { "Sóng âm - Xung kích", "Hóa báo đốm", "Tia chớp" };
                        int[] icon_ = new int[] { 72, 71, 70 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 219, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4220: {
                    if (p.item.total_item_bag_by_id(4, 220) > 0) {
                        String[] name_ = new String[] { "Cơn lốc - Ưng kích", "Hóa chim ưng", "Chim săn mồi" };
                        int[] icon_ = new int[] { 69, 68, 67 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 220, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4240: {
                    if (p.item.total_item_bag_by_id(4, 240) > 0) {
                        String[] name_ = new String[] { "Bộc phá", "Vết nứt", "Kình lực", "Địa chấn" };
                        int[] icon_ = new int[] { 76, 75, 73, 74 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 240, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4316: {
                    if (p.item.total_item_bag_by_id(4, 316) > 0) {
                        String[] name_ = new String[] { "Giáp sáp", "Đao không kích", "Lao sáp" };
                        int[] icon_ = new int[] { 79, 77, 78 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 316, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4317: {
                    if (p.item.total_item_bag_by_id(4, 317) > 0) {
                        String[] name_ = new String[] { "Thân thể thép", "Ảo ảnh trảm", "Loạn trảm" };
                        int[] icon_ = new int[] { 82, 81, 80 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 317, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4318: {
                    if (p.item.total_item_bag_by_id(4, 318) > 0) {
                        String[] name_ = new String[] { "Thần hộ thể", "Tăng trọng", "Sức nặng ngàn cân" };
                        int[] icon_ = new int[] { 85, 84, 83 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 318, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4427: { // trai bong toi
                    if (p.item.total_item_bag_by_id(4, 427) > 0) {
                        String[] name_ = new String[] { "Dòng chảy ma pháp", "Vòng xoáy ma pháp",
                                "Giải phóng", "Xoáy đen" };
                        int[] icon_ = new int[] { 91, 88, 90, 89 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 427, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 4684: { // trai anh sang
                    if (p.item.total_item_bag_by_id(4, 684) > 0) {
                        String[] name_ = new String[] { "Cực quang phong ấn", "Thiên quang phá diệt",
                                "Quang vũ thực tâm", "Quang minh hoá thân" };
                        int[] icon_ = new int[] { 95, 97, 96, 94 };
                        Service.NewDialog_eat_taq(p, name_, icon_, (id - 4000));
                        p.get_skill_taq_new(id - 4000);
                        p.item.remove_item47(4, 684, 1);
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 1: {
                    if (p.item_to_kham_ngoc != null) {
                        int vang_req = 0;
                        for (int i = 0; i < p.item_to_kham_ngoc.mdakham.length; i++) {
                            if (p.item_to_kham_ngoc.mdakham[i] >= 44
                                    && p.item_to_kham_ngoc.mdakham[i] <= 79) {
                                vang_req += Rebuild_Item.PRICE_THAO_NGOC[Rebuild_Item
                                        .get_percent_hop_ngoc(p.item_to_kham_ngoc.mdakham[i])];
                            } else if (p.item_to_kham_ngoc.mdakham[i] >= 241
                                    && p.item_to_kham_ngoc.mdakham[i] <= 270) {
                                vang_req += 3000;
                            } else {
                                vang_req += 3500;
                            }
                        }
                        if (vang_req > 0) {
                            if (p.get_ngoc() < vang_req) {
                                Service.send_box_ThongBao_OK(p, "Không đủ " + vang_req + " ruby");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item_to_kham_ngoc.mdakham.length > p.item.able_bag()) {
                                Service.send_box_ThongBao_OK(p,
                                        "Hãy chừa ít nhất " + p.item_to_kham_ngoc.mdakham.length
                                                + " ô trống trong hành trang");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            p.update_ngoc(-vang_req);
                            p.update_money();
                            for (int i = 0; i < p.item_to_kham_ngoc.mdakham.length; i++) {
                                p.item.add_item_bag47(4, p.item_to_kham_ngoc.mdakham[i], 1);
                            }
                            p.item_to_kham_ngoc.mdakham = new short[0];
                            p.item_to_kham_ngoc.option_item_2.clear();
                            p.update_info_to_all();
                            p.item.update_Inventory(-1, false);
                            Message m = new Message(-67);
                            m.writer().writeByte(6);
                            m.writer().writeUTF("Tháo ngọc khảm trang bị "
                                    + p.item_to_kham_ngoc.template.name + " thành công");
                            p.addmsg(m);
                            m.cleanup();
                        }
                    } else {
                        Rebuild_Item.show_table(p, 4);
                        Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, hãy thử lại");
                    }
                    break;
                }
                case 0: {
                    // use_item_3
                    if (p.use_item_3 != -1) {
                        Item_wear it = p.item.bag3[p.use_item_3];
                        if (it != null && UseItem.check_it_can_wear(it.template.typeEquip)) {
                            p.wear_item(it);
                        }
                        p.use_item_3 = -1;
                    }
                    break;
                }
            }
        } else if (value == 1) { // hoi ren
            switch (id) {

                case 84: { // vxmm
                    Service.input_text(p, 23, "Vòng Xoay May Mắn", new String[] { "Nhập số Extol" });
                    break;
                }

                case 81: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int idfas = p.data_yesno[0];
                        ItemFashionP2 temp = p.check_fashion(idfas);
                        p.data_yesno = new int[] { idfas };
                        String info = "\n" + ItemFashionP2.getRangeById((short) idfas).getInfoText();
                        Service.send_box_yesno(
                                p, 82, "", ItemFashion.get_item(temp.id).name
                                        + "\n Đổi lại toàn bộ chỉ số (Random) \n Giá 1.000 Extol "
                                        + info,
                                new String[] { "Đổi", "Đóng" },
                                new byte[] { -1, -1 });
                    }
                    break;
                }

                case 78: {
                    ItemDropTool.handleUpgradeTuTien(p, true);
                    break;
                }

                case 41: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Max_Level.addThongThao(p, p.data_yesno[0], 1000);
                    }
                    break;
                }

                case 57: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Item_wear it_select = p.item.bag3[p.data_yesno[0]];
                        if (it_select != null && it_select.numLoKham < 12) {
                            int beri_req = 5_000_000 * (it_select.numLoKham + 1);
                            if (p.item.total_item_bag_by_id(4, 457) < 1) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 1 Búa Đục Lỗ");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.get_vang() < beri_req) {
                                Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(beri_req) + "Beri");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            int attempts = 0;
                            long beriSpent = 0;
                            int buadialUsed = 0;
                            boolean success = false;
                            while (it_select.numLoKham < 12) {
                                if (attempts >= p.solannang) {
                                    break;
                                }
                                if (p.item.total_item_bag_by_id(4, 457) < 1 || p.get_vang() < beri_req) {
                                    break;
                                }
                                p.update_vang(-beri_req);
                                p.item.remove_item47(4, 457, 1);
                                p.update_money();
                                beriSpent += beri_req;
                                buadialUsed++;
                                attempts++;
                                boolean suc = 20 > Util.random(100 + it_select.numLoKham * 50);
                                if (suc) {
                                    if (it_select.numHoleDaDuc < 0) {
                                        it_select.numHoleDaDuc = 0;
                                    }
                                    it_select.numLoKham++;
                                    it_select.numHoleDaDuc++;
                                    success = true;
                                    break;
                                }
                            }
                            String summaryMessage = "== Auto Đục Lỗ ==" + "\n"
                                    + it_select.template.name + " [ " + it_select.numLoKham + " lỗ ]" + "\n"
                                    + (success ? "Thành công" : "Thất bại") + "\n"
                                    + "Số lần đục : " + attempts + "\n"
                                    + "Beri : " + Util.number_format(beriSpent) + "\n"
                                    + "--------------------------\n"
                                    + "Dùng : " + buadialUsed + " Búa Đục Lỗ";
                            Message m = new Message(-67);
                            m.writer().writeByte(7);
                            m.writer().writeUTF(summaryMessage);
                            p.addmsg(m);
                            m.cleanup();
                            p.item.update_Inventory(-1, false);
                        }
                    }
                    break;
                }

                case 70: {
                    Upgrade_Skin.autoUpgrade(p);
                    break;
                }

                case 69: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        UpgradeSuperItem.autoUpgrade(p, p.data_yesno[0]);
                    }
                    break;
                }

                case 68: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        UpgradeDial.autoUpgrade(p, p.data_yesno[0]);
                    }
                    break;
                }

                case 32: {
                    if (p.item_to_kham_ngoc != null) {
                        Item_wear it_select = p.item_to_kham_ngoc;
                        int attempts = 0;
                        long beriSpent = 0;
                        int stoneCount = 0;
                        boolean finalSuccess = false;
                        if (p.item.total_item_bag_by_id(4, 226) < 1) {
                            Service.send_box_ThongBao_OK(p, "Không đủ Đá Hải Thạch cấp 6");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.get_vnd() < 5_000) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        while (true) {
                            if (attempts >= p.solannang) {
                                break;
                            }
                            if (p.item.total_item_bag_by_id(4, 226) < 1) {
                                break;
                            }
                            if (p.get_vnd() < 5_000) {
                                break;
                            }
                            attempts++;
                            p.item.remove_item47(4, 226, 1);

                            p.update_vnd(-5_000);
                            beriSpent += 5_000;
                            stoneCount++;
                            p.update_money();
                            boolean suc = 25 > Util.random(150);
                            if (suc) {
                                it_select.valueKichAn = (byte) Util.random(13);
                                if (it_select.valueKichAn == 12) {
                                    it_select.typelock = -1;
                                }
                                finalSuccess = true;
                                UpgradeDial.sendChatKTG_KichAn(p, it_select, attempts);
                                break;
                            }
                        }
                        String summaryMessage = "== Auto Kích Ẩn ==" + "\n"
                                + (finalSuccess ? "Thành công" : "Thất bại") + "\n"
                                + "Số lần nâng : " + attempts + "\n"
                                + "Extol : " + Util.number_format(beriSpent) + "\n"
                                + "--------------------------\n"
                                + "Dùng : " + stoneCount + " Đá Hải Thạch cấp 6";
                        Message m = new Message(-67);
                        m.writer().writeByte(20);
                        m.writer().writeUTF(summaryMessage);
                        p.addmsg(m);
                        m.cleanup();
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }

                case 30: {
                    if (p.item_to_kham_ngoc != null) {
                        Item_wear it_select = p.item_to_kham_ngoc;
                        int perfectionAttempts = 0;
                        long beriSpent = 0;
                        boolean finalSuccess = false;
                        int stoneCount = 0;
                        if (it_select.isHoanMy == 1) {
                            Service.send_box_ThongBao_OK(p, "Trang bị này đã được hoàn mỹ rồi");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.total_item_bag_by_id(4, 226) < 1) {
                            Service.send_box_ThongBao_OK(p, "Không đủ Đá Hải Thạch cấp 6");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.get_vnd() < 5_000) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        while (true) {
                            if (perfectionAttempts >= p.solannang) {
                                break;
                            }
                            if (p.item.total_item_bag_by_id(4, 226) < 1) {
                                break;
                            }
                            if (p.get_vnd() < 5_000) {
                                break;
                            }
                            perfectionAttempts++;
                            p.item.remove_item47(4, 226, 1);

                            p.update_vnd(-5_000);
                            beriSpent += 5_000;
                            stoneCount++;
                            p.update_money();
                            boolean suc = 25 > Util.random(150);
                            if (suc) {
                                it_select.isHoanMy = 1;
                                finalSuccess = true;
                                UpgradeDial.sendChatKTG_HoanMy(p, it_select, perfectionAttempts);
                                break;
                            }
                        }
                        String summaryMessage = "== Auto Hoàn Mỹ ==" + "\n"
                                + (finalSuccess ? "- Thành công -" : "- Thất bại -") + "\n"
                                + "Số lần nâng : " + perfectionAttempts + "\n"
                                + "Beri : " + Util.number_format(beriSpent) + "\n"
                                + "--------------------------\n"
                                + "Dùng : " + stoneCount + " Đá Hải Thạch cấp 6";
                        Message m = new Message(-67);
                        m.writer().writeByte(20);
                        m.writer().writeUTF(summaryMessage);
                        p.addmsg(m);
                        m.cleanup();
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }

                case 28: {
                    break;
                }

                case 53: {
                    if (p.name_ThoSanHaiTac != null && p.name_ThoSanHaiTac.length == 1
                            && p.typePirate == 1) {
                        Player p0 = Map.get_player_by_name_allmap(p.name_ThoSanHaiTac[0]);
                        if (p0 != null && p0.map.equals(p.map)) {
                            Service.send_box_ThongBao_OK(p0, p.name + " từ chối bảo vệ vận hàng");
                        }
                        Service.send_box_ThongBao_OK(p, "từ chối thành công");
                        p.name_ThoSanHaiTac = null;
                    }
                    break;
                }
                case 34: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        int currentId = p.data_yesno[0]; // ID hiện tại (29 hoặc 158)
                        int targetId;
                        int requiredStones;
                        int successRate;

                        // Xác định targetId, nguyên liệu, tỉ lệ dựa theo currentId
                        if (currentId == 29) {
                            targetId = 158; // từ 29 nâng lên 158
                            requiredStones = 50;
                            successRate = 5;
                        } else if (currentId == 158) {
                            targetId = 1045; // từ 158 nâng lên 1045
                            requiredStones = 100;
                            successRate = 2;
                        } else {
                            Service.send_box_ThongBao_OK(p, "Không thể nâng cấp vật phẩm này!");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }

                        int rubyCostPerTry = 100;

                        // Kiểm tra đủ điều kiện ban đầu
                        if (p.item.total_item_bag_by_id(7, 9) < requiredStones) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + requiredStones + " " + ItemTemplate7.get_it_by_id(9).name);
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.total_item_bag_by_id(4, currentId) < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không có " + ItemTemplate4.get_it_by_id(currentId).name);
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.get_ngoc() < rubyCostPerTry) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 100 Ruby");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        if (p.item.able_bag() < 1) {
                            Service.send_box_ThongBao_OK(p, "Hãy chừa ít nhất 1 ô trống trong hành trang");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }

                        // Biến thống kê
                        int totalAttempts = 0;
                        int totalRubySpent = 0;
                        int totalStoneSpent = 0;
                        boolean success = false;

                        // Auto nâng nhiều lần
                        while (!success && totalAttempts < p.solannang) {
                            // Kiểm tra đủ tài nguyên cho lần nâng này
                            if (p.get_ngoc() < rubyCostPerTry
                                    || p.item.total_item_bag_by_id(7, 9) < requiredStones
                                    || p.item.total_item_bag_by_id(4, currentId) < 1) {
                                break;
                            }

                            // Trừ tài nguyên
                            p.update_ngoc(-rubyCostPerTry);
                            p.item.remove_item47(7, 9, requiredStones);
                            p.update_money();

                            totalAttempts++;
                            totalRubySpent += rubyCostPerTry;
                            totalStoneSpent += requiredStones;

                            // Check thành công
                            if (Util.random(120) < successRate) {
                                p.item.remove_item47(4, currentId, 1);
                                p.item.add_item_bag47(4, targetId, 1);
                                success = true;
                                break;
                            }
                        }

                        // Thông báo kết quả
                        String finalMessage = "== Auto Nâng " + ItemTemplate4.get_it_by_id(currentId).name + " ==\n"
                                + (success ? "- Thành công thành " + ItemTemplate4.get_it_by_id(targetId).name + " -\n"
                                        : "- Thất bại -\n")
                                + "Số lần nâng : " + totalAttempts + "\n"
                                + "-----------------------\n"
                                + "Tổng : " + String.format("%,d", totalRubySpent) + " Ruby\n"
                                + "Nguyên liệu : " + String.format("%,d", totalStoneSpent) + " Đá ác quỷ";

                        Message m = new Message(45);
                        m.writer().writeByte(17);
                        m.writer().writeByte(success ? 1 : 3);
                        m.writer().writeUTF(finalMessage);
                        p.addmsg(m);
                        m.cleanup();

                        // Cập nhật và reset
                        p.item.update_Inventory(-1, false);
                        p.data_yesno = null;
                        p.map_tele = null;
                    }
                    break;
                }

                case 33: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Skill_info sk_temp = p.get_skill_temp(p.data_yesno[0]);
                        if (sk_temp != null) {
                            if (sk_temp.lvdevil > 4) {
                                Service.send_box_ThongBao_OK(p,
                                        sk_temp.temp.name + " đã được cường hóa tối đa");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }

                            if (p.get_ngoc() < 200) {
                                Service.send_box_ThongBao_OK(p, "Không đủ 200 ruby");
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            if (p.item.total_item_bag_by_id(7, 9) < 50) {
                                Service.send_box_ThongBao_OK(p,
                                        "Bạn không đủ " + ItemTemplate7.get_it_by_id(9).name);
                                p.data_yesno = null;
                                p.map_tele = null;
                                return;
                            }
                            int upgradeCount = 0;
                            int totalCost = 0;
                            int totalDevilStoneCost = 0; // Tổng chi phí đá ác quỷ
                            boolean success = false;

                            while (sk_temp.devilpercent < 100) {
                                // Kiểm tra nếu số lần nâng đã đủ
                                if (upgradeCount >= p.solannang) {
                                    break; // Dừng nếu đã nâng đủ số lần
                                }
                                if (p.get_ngoc() < 200) {
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    break;
                                }
                                if (p.item.total_item_bag_by_id(7, 9) < 50) {
                                    p.data_yesno = null;
                                    p.map_tele = null;
                                    break;
                                }
                                upgradeCount++;
                                p.update_ngoc(-200);
                                p.item.remove_item47(7, 9, 50);
                                totalCost += 200;
                                totalDevilStoneCost += 50; // Cộng thêm chi phí đá ác quỷ
                                p.update_money();
                                p.item.update_Inventory(-1, false);

                                int percent = (sk_temp.lvdevil == 0) ? 10
                                        : ((sk_temp.lvdevil == 1) ? 8
                                                : ((sk_temp.lvdevil == 2) ? 6
                                                        : ((sk_temp.lvdevil == 3) ? 5 : 4)));

                                boolean suc = 50 > Util.random(120);
                                if (suc) {
                                    sk_temp.devilpercent += percent;
                                    if (sk_temp.devilpercent >= 100) {
                                        sk_temp.devilpercent = 0;
                                        sk_temp.lvdevil++;
                                        success = true; // Đánh dấu thành công
                                        break;
                                    }
                                    p.send_skill();
                                    p.update_info_to_all();
                                }
                            }
                            // Thông báo kết quả
                            String finalMessage = "== Auto Cường Hóa Kỹ Năng ==" + "\n"
                                    + sk_temp.temp.name + " [" + sk_temp.lvdevil + "]" + " + " + sk_temp.devilpercent
                                    + "%" + "\n"
                                    + "Số lần cường hóa : " + upgradeCount + "\n"
                                    + "-----------------------\n"
                                    + "Tổng : " + String.format("%,d", totalCost) + " Ruby\n"
                                    + "Nguyên liệu : " + String.format("%,d", totalDevilStoneCost) + " Đá ác quỷ";

                            //
                            Message m = new Message(45);
                            m.writer().writeByte(12);
                            m.writer().writeByte(success ? 1 : 3);
                            m.writer().writeUTF(finalMessage);
                            p.addmsg(m);
                            m.cleanup();
                            p.item.update_Inventory(-1, false);
                            p.send_skill();
                            p.update_info_to_all();
                            p.data_yesno = null;
                            p.map_tele = null;
                        }
                    }
                    break;
                }

                case 31: {
                    if (p.item_to_kham_ngoc != null && (p.item_to_kham_ngoc.template.typeEquip < 6
                            || p.item_to_kham_ngoc.template.typeEquip == 7)) {
                        if (p.get_vang() < 1000000) {
                            Service.send_box_ThongBao_OK(p, "Không đủ 1,000,000 beri");
                            p.data_yesno = null;
                            p.map_tele = null;
                            return;
                        }
                        p.update_vang(-1000000);
                        p.update_money();
                        p.item_to_kham_ngoc.valueChetac += 10;
                        if (p.item_to_kham_ngoc.valueChetac >= 100) {
                            p.item_to_kham_ngoc.valueChetac = 100;
                        }
                        //
                        Message m = new Message(-67);
                        m.writer().writeByte(25);
                        m.writer().writeUTF("Bạn phục hồi 10 điểm chế tác thành công");
                        p.addmsg(m);
                        m.cleanup();
                        //
                        p.item.update_Inventory(-1, false);
                    }
                    break;
                }
                case 16: {
                    p.map_boss_info = null;
                    break;
                }
                case 11: {
                    p.isTachTB = false;
                    break;
                }
                case 7: {
                    break;
                }
                case 1: {
                    Rebuild_Item.show_table(p, 4);
                    break;
                }
                case 0: {
                    p.use_item_3 = -1;
                    break;
                }
            }
        } else if (value == 2) { // other
            switch (id) {

                case 84: {
                    VongXoayMayManManager.nhanQuaVongXoay(p);
                    break;
                }

                case 41: {
                    if (p.data_yesno != null && p.data_yesno.length == 1) {
                        Max_Level.addThongThao(p, p.data_yesno[0], p.pointAttributeThongThao);
                    }
                    break;
                }
            }
        }
        if (id != 13 && id != 81) {
            p.data_yesno = null;
            p.map_tele = null;
            p.data_yesno_gem = null;
        }
    }
}
