package client;

import activities.DauGia;
import activities.LenhTruyNa;
import activities.Market;
import activities.VongXoayMayManManager;
import core.Manager;
import core.Service;
import core.Util;
import core.GameLogger;
import database.SQL;
import io.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import map.Map;
import template.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ClientInput {

    public static void process(Player p, Message m2) throws IOException {
        short id = m2.reader().readShort();
        String[] name = new String[m2.reader().readByte()];
        for (int i = 0; i < name.length; i++) {
            name[i] = m2.reader().readUTF();
        }
        switch (id) {
            case 3333: {
                if (name.length != 1)
                    return;
                activities.Mastery2.handleAllocation(p, name[0]);
                break;
            }
            case 3336: { // Bulk Craft Tu Tiên
                if (name.length != 1)
                    return;
                if (!Util.isnumber(name[0]))
                    return;
                int quantity = Integer.parseInt(name[0]);
                activities.Mastery2.craftBoiBulk(p, false, quantity);
                break;
            }
            case 3337: { // Bulk Craft Tu Ma
                if (name.length != 1)
                    return;
                if (!Util.isnumber(name[0]))
                    return;
                int quantity = Integer.parseInt(name[0]);
                activities.Mastery2.craftBoiBulk(p, true, quantity);
                break;
            }
            case butoi.ButOi.INPUT_ID: {
                butoi.ButOi.processInput(p, name);
                break;
            }
            case 47: {
                break;
            }
            case taixiu.TaiXiuManager.INPUT_DAT_CUOC:
            case taixiu.TaiXiuManager.INPUT_TIEN_CUOC_UI: {
                taixiu.TaiXiuManager.xuLyInput(p, id, name[0]);
                break;
            }
            case taixiu.BauCuaManager.INPUT_SO_TIEN: {
                taixiu.BauCuaManager.xuLyInput(p, id, name[0]);
                break;
            }
            case taixiu.BlackjackManager.INPUT_BET: {
                taixiu.BlackjackManager.handleInput(p, name[0]);
                break;
            }
            case taixiu.SlotMachineManager.INPUT_ID: {
                taixiu.SlotMachineManager.handleInput(p, name[0]);
                break;
            }

            case 100: {
                if (name.length != 1)
                    return;
                String quantityStr = name[0].trim();
                if (!Util.isnumber(quantityStr)) {
                    Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                    return;
                }
                int turns = Integer.parseInt(quantityStr);
                activities.VongQuay.performQuickSpin(p, turns);
                break;
            }

            case 25: {
                if (name.length != 1)
                    return;
                String quantityStr = name[0].trim();
                if (!Util.isnumber(quantityStr)) {
                    Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                    return;
                }
                int requestedQuantity = Integer.parseInt(quantityStr);
                if (requestedQuantity <= 0) {
                    Service.send_box_ThongBao_OK(p, "Số lượng phải > 0");
                    return;
                }
                if (p.tempIndexMapping == null || p.tempIndexMapping.isEmpty())
                    return;
                for (int itemId : p.tempIndexMapping) {
                    int totalInBag = p.item.total_item_bag_by_id(4, itemId);
                    String itemName = ItemTemplate4.get_it_by_id(itemId).name;
                    if (totalInBag < requestedQuantity) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + requestedQuantity + " " + itemName);
                        return;
                    }
                    if (itemId == tool.ItemDropTool.ITEM_BOI_TU_TIEN || itemId == tool.ItemDropTool.ITEM_BOI_TU_MA) {
                        if (p.hasUnlockedMastery2) {
                            Service.send_box_ThongBao_OK(p, "Bạn đã Thăng Giai, không thể dùng Bội thường!");
                            return;
                        }
                        int successCount = 0;
                        int failCount = 0;
                        int pointsEarned = 0;
                        int pointGainPerUse = p.getMasteryPotentialGain();
                        for (int i = 0; i < requestedQuantity; i++) {
                            if (Util.random(100) < tool.ItemDropTool.TU_TIEN_BOI_SUCCESS_RATE) {
                                pointsEarned += pointGainPerUse;
                                successCount++;
                            } else {
                                failCount++;
                            }
                        }
                        p.pointAttributeThongThao += pointsEarned;
                        p.item.remove_item47(4, itemId, requestedQuantity);
                        p.item.update_Inventory(-1, false);
                        Service.send_box_ThongBao_OK(p,
                                "Sử dụng " + requestedQuantity + " " + itemName + "!\n" +
                                        "Thành công: " + successCount + " lần.\n" +
                                        "Thất bại: " + failCount + " lần.\n" +
                                        "Bạn nhận được " + pointsEarned + " điểm tiềm năng thông thạo.");
                    } else if (itemId == activities.Mastery2.ITEM_BOI_TU_TIEN_2
                            || itemId == activities.Mastery2.ITEM_BOI_TU_MA_2) {
                        if (!p.hasUnlockedMastery2) {
                            Service.send_box_ThongBao_OK(p, "Bạn chưa Thăng Giai, không thể dùng Bội cấp 2!");
                            return;
                        }
                        int successCount = 0;
                        int failCount = 0;
                        int pointsEarned = 0;
                        for (int i = 0; i < requestedQuantity; i++) {
                            if (Util.random(100) < tool.ItemDropTool.TU_TIEN_BOI_SUCCESS_RATE) {
                                pointsEarned += 5;
                                successCount++;
                            } else {
                                failCount++;
                            }
                        }
                        p.pointAttributeThongThao2 += pointsEarned;
                        if (p.pointAttributeThongThao2 > 160000)
                            p.pointAttributeThongThao2 = 160000; // Safety cap at 160k points
                        p.item.remove_item47(4, itemId, requestedQuantity);
                        p.item.update_Inventory(-1, false);
                        Service.send_box_ThongBao_OK(p,
                                "Sử dụng " + requestedQuantity + " " + itemName + "!\n" +
                                        "Thành công: " + successCount + " lần.\n" +
                                        "Thất bại: " + failCount + " lần.\n" +
                                        "Bạn nhận được " + pointsEarned + " điểm tiềm năng Bảng 2.");

                        // Sync client UI for item count
                        Message m = new Message(-13);
                        m.writer().writeShort(itemId);
                        m.writer().writeShort(p.item.total_item_bag_by_id(4, itemId));
                        p.addmsg(m);
                        m.cleanup();
                    } else {
                        int openedCount = 0;
                        for (int i = 0; i < requestedQuantity; i++) {
                            if (p.item.able_bag() < 3) {
                                Service.send_box_ThongBao_OK(p, "Cần ít nhất 3 ô trống trong hành trang");
                                break;
                            }
                            if (!UseItem.use_item_4(p, itemId)) {
                                break;
                            }
                            p.item.remove_item47(4, itemId, 1);
                            openedCount++;
                        }
                        if (openedCount > 0) {
                            p.item.update_Inventory(-1, false);
                            Service.send_box_ThongBao_OK(p, "Done! " + openedCount + " " + itemName);
                        }
                    }
                }
                p.tempIndexMapping.clear();
                break;
            }

            case 1035: {
                if (name.length != 1)
                    return;
                String quantityStr = name[0].trim();
                if (!Util.isnumber(quantityStr)) {
                    Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                    return;
                }
                int requestedQuantity = Integer.parseInt(quantityStr);
                if (requestedQuantity <= 0) {
                    Service.send_box_ThongBao_OK(p, "Số lượng phải > 0");
                    return;
                }
                int totalInBag = p.item.total_item_bag_by_id(4, 1035);
                if (totalInBag < requestedQuantity) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + requestedQuantity + " Rương Dial Vua");
                    return;
                }
                if (p.item.able_bag() < requestedQuantity) {
                    Service.send_box_ThongBao_OK(p,
                            "Hành trang không đủ chỗ trống. Cần " + requestedQuantity + " ô trống.");
                    return;
                }

                int[] dialIds = { 12001, 12002, 12003, 12004, 12005 };
                int[] weights = { 40, 30, 20, 8, 2 }; // tổng = 100
                List<GiftBox> listGifts = new ArrayList<>();

                for (int i = 0; i < requestedQuantity; i++) {
                    int roll = Util.random(100);
                    int cumulative = 0;
                    int id_add = dialIds[0];
                    for (int w = 0; w < weights.length; w++) {
                        cumulative += weights[w];
                        if (roll < cumulative) {
                            id_add = dialIds[w];
                            break;
                        }
                    }

                    ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
                    if (temp != null) {
                        Item_wear it_dial = new Item_wear();
                        it_dial.setup_template_by_id(temp);

                        int[] randomStats = { 5, 6, 7, 8, 9 };
                        for (int j = 0; j < 5; j++) {
                            int optId = randomStats[Util.random(randomStats.length)];
                            int optVal = Util.random(100, 201);
                            it_dial.option_item.add(new Option(optId, optVal));
                        }

                        p.item.add_item_bag3(it_dial);

                        GiftBox gb = new GiftBox();
                        gb.id = -1;
                        gb.type = 3;
                        gb.name = temp.name;
                        gb.icon = temp.icon;
                        gb.num = 1;
                        gb.color = temp.color;
                        listGifts.add(gb);
                    }
                }

                p.item.remove_item47(4, 1035, requestedQuantity);
                Service.send_gift(p, 1, "Mở Rương Dial Vua", "Bạn nhận được " + requestedQuantity + " trang bị Dial",
                        listGifts, true);
                if (!p.is_show_gift) {
                    Service.send_box_ThongBao_OK(p, "Bạn đã mở " + requestedQuantity + " Rương Dial Vua thành công!");
                }
                break;
            }

            case 24: {
                if (name.length == 2) {
                    String targetName = name[0].trim();
                    String rewardStr = name[1].trim();
                    if (targetName.isEmpty()) {
                        Service.send_box_ThongBao_OK(p, "Tên truy nã không được để trống");
                        return;
                    }
                    if (!Util.isnumber(rewardStr)) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int rewardMoney = Integer.parseInt(rewardStr);
                    if (rewardMoney < 100_000 || rewardMoney > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p, "Số Beri từ 100K đến 2 Tỷ");
                        return;
                    }
                    LenhTruyNa.createWantedPoster(p, targetName, rewardMoney);
                }
                break;
            }

            case 23: {
                if (name.length == 1) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    long value = Long.parseLong(name[0]);
                    if (value <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.get_vnd() < value) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(value) + " Extol");
                        return;
                    }
                    int timeLeft = VongXoayMayManManager.gI().getTimeLeft();
                    if (timeLeft <= 5) {
                        Service.send_box_ThongBao_OK(p, "Đã hết thời gian tham gia");
                        return;
                    }
                    p.update_vnd(-value);
                    VongXoayMayManManager.updateExtolSpinData(p.name, value);
                    p.update_money();
                    VongXoayMayManManager.showSpinMenu(p);
                }
                break;
            }

            case 22: {
                if (name.length == 1 && p.data_yesno == null) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.get_vnd() < value) {
                        Service.send_box_ThongBao_OK(p, "Bạn không đủ " + Util.number_format(value) + " Extol");
                        return;
                    }
                    int ruby = value * 1; // Vì 1 ruby = 1 extol
                    p.data_yesno = new int[] { value };
                    Service.send_box_yesno(p, 10, "Thông báo",
                            "Bạn có thật sự muốn đổi " + Util.number_format(value) + " Extol để"
                                    + " đổi lấy " + Util.number_format(ruby) + " Ruby không?",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                    break;
                }
                break;
            }

            case 21: {
                if (name.length == 1) {
                    String newClanName = name[0];
                    if (newClanName.length() < 3 || newClanName.length() > 15) {
                        Service.send_box_ThongBao_OK(p, "Tên băng phải có từ 3 đến 15 ký tự.");
                        return;
                    }
                    if (!newClanName.matches("[a-zA-Z0-9\\p{L}\\s]+")) {
                        Service.send_box_ThongBao_OK(p, "Tên băng không được chứa ký tự đặc biệt.");
                        return;
                    }

                    if (p.get_coin() < 50_000) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 50.000 Coin");
                        return;
                    }
                    if (Clan.get_clan_by_name(newClanName) != null) {
                        Service.send_box_ThongBao_OK(p,
                                "Tên băng này đã được sử dụng, hãy sử dụng tên khác");
                        return;
                    }
                    if (newClanName.toLowerCase().contains("admin")) {
                        Service.send_box_ThongBao_OK(p, "Không được đặt tên băng là admin");
                        return;
                    }

                    if (Clan.renameClan(p.clan, newClanName)) {
                        // Thông báo cho người chơi
                        Service.send_box_ThongBao_OK(p, "Đổi tên băng thành công");
                        p.update_coin(-50_000);
                        p.update_money();
                        // Thông báo cho các thành viên khác trong clan
                        for (Clan_member member : p.clan.members) {
                            Player otherPlayer = Map.get_player_by_name_allmap(member.name);
                            if (otherPlayer != null) {
                                Clan.update_list_member(otherPlayer, false);
                                Clan.send_info(otherPlayer, false);
                            }
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Tên băng đã tồn tại hoặc không hợp lệ.");
                    }
                }
                break;
            }

            case 18: {
                if (name.length == 1) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value < 1000) {
                        Service.send_box_ThongBao_OK(p, "Số nhập ít nhất 1000 Extol");
                        return;
                    }
                    if (p.get_vnd() < value) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(value) + " Extol");
                        return;
                    }
                    int bua = value / 1000;
                    int usedExtol = bua * 1000;
                    p.update_bua(bua);
                    p.update_vnd(-usedExtol);
                    p.update_money();
                    Service.send_box_ThongBao_OK(p,
                            "Đổi thành công \n" + Util.number_format(usedExtol) + " Extol sang "
                                    + Util.number_format(bua) + " Búa");
                }
                break;
            }

            case 16: {
                if (name.length == 1) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int rubyAmount = Integer.parseInt(name[0]);
                    if (rubyAmount <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    // Trừ 30% phí
                    int rubyAfterFee = (int) (rubyAmount * 0.7);
                    if (p.get_ngoc() < rubyAmount) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(rubyAmount) + " Ruby");
                        return;
                    }
                    if (p.tempRubyP0 != null) {
                        if (rubyAfterFee > 2_000_000_000
                                || (rubyAfterFee + (long) p.tempRubyP0.get_ngoc()) > 2_000_000_000) {
                            Service.send_box_ThongBao_OK(p, "Số dư quá lớn, hãy thử lại sau");
                            return;
                        }
                        p.updateNgocForGift(-rubyAmount);
                        p.tempRubyP0.updateNgocForGift(rubyAfterFee);
                        p.update_money();
                        p.tempRubyP0.update_money();
                        Service.send_box_ThongBao_OK(p,
                                "Tặng Ruby (Phí 30%) \n Tặng " + Util.number_format(rubyAmount) + " Ruby cho "
                                        + p.tempRubyP0.getNameShow());
                        Service.send_box_ThongBao_OK(p.tempRubyP0,
                                p.getNameShow() + " đã tặng cho bạn " + Util.number_format(rubyAfterFee) + " Ruby");

                        p.tempRubyP0 = null;
                    }
                }
                break;
            }

            case 15: {
                break;
            }

            case 14: {
                if (name.length == 1) {
                    String newName = name[0].trim();
                    Pattern pattern = Pattern.compile("^[a-zA-Z0-9 ]{5,15}$");
                    if (newName.isEmpty() || !pattern.matcher(newName).matches()) {
                        if (newName.length() < 5 || newName.length() > 15) {
                            Service.send_box_ThongBao_OK(p, "Tên phải từ 5 đến 15 ký tự");
                        } else {
                            Service.send_box_ThongBao_OK(p, "Tên không hợp lệ");
                        }
                        return;
                    }
                    // Kiểm tra còn vật phẩm trong chợ không
                    boolean hasItemInMarket = false;
                    for (Market market : Market.ENTRY) {
                        for (ItemMarket item : market.item3) {
                            if (item.seller.equals(p.name)) {
                                hasItemInMarket = true;
                                break;
                            }
                        }
                        for (PotionMarket potion : market.item47) {
                            if (potion.seller.equals(p.name)) {
                                hasItemInMarket = true;
                                break;
                            }
                        }
                        if (hasItemInMarket)
                            break;
                    }
                    if (hasItemInMarket) {
                        Service.send_box_ThongBao_OK(p,
                                "Không thể đổi tên khi bạn còn có vật phẩm trong chợ kí gửi");
                        return;
                    }

                    try (Connection connection = SQL.gI().getCon()) {
                        connection.setAutoCommit(false);

                        // Kiểm tra trùng lặp tên
                        String checkQuery = "SELECT COUNT(*) FROM players WHERE name = ?";
                        try (PreparedStatement psCheck = connection.prepareStatement(checkQuery)) {
                            psCheck.setString(1, newName);
                            try (ResultSet rs = psCheck.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    Service.send_box_ThongBao_OK(p, "Tên đã tồn tại, vui lòng chọn tên khác");
                                    connection.rollback();
                                    return;
                                }
                            }
                        }

                        // Cập nhật tên người chơi
                        String updatePlayerQuery = "UPDATE players SET name = ? WHERE name = ?";
                        try (PreparedStatement psUpdatePlayer = connection.prepareStatement(updatePlayerQuery)) {
                            psUpdatePlayer.setString(1, newName);
                            psUpdatePlayer.setString(2, p.name);
                            if (psUpdatePlayer.executeUpdate() <= 0) {
                                connection.rollback();
                                Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, vui lòng thử lại");
                                return;
                            }
                        }

                        // Cập nhật tên trong bảng accounts
                        String updateAccountQuery = "UPDATE accounts SET `char` = REPLACE(`char`, ?, ?) WHERE BINARY `user` = ?";
                        try (PreparedStatement psUpdateAccount = connection.prepareStatement(updateAccountQuery)) {
                            psUpdateAccount.setString(1, p.name);
                            psUpdateAccount.setString(2, newName);
                            psUpdateAccount.setString(3, p.conn.user);
                            if (psUpdateAccount.executeUpdate() <= 0) {
                                connection.rollback();
                                Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, vui lòng thử lại");
                                return;
                            }
                        }

                        // Cập nhật tên trong clan
                        if (p.clan != null) {
                            for (Clan_member member : p.clan.members) {
                                if (member.name.equals(p.name)) {
                                    member.name = newName;
                                    break;
                                }
                            }
                            // Save to database immediately
                            String updateClanQuery = "UPDATE `clan` SET `member` = ? WHERE `id` = ?";
                            try (PreparedStatement psUpdateClan = connection.prepareStatement(updateClanQuery)) {
                                org.json.simple.JSONArray js_members = new org.json.simple.JSONArray();
                                for (int i2 = 0; i2 < p.clan.members.size(); i2++) {
                                    org.json.simple.JSONArray js_in = new org.json.simple.JSONArray();
                                    Clan_member mem = p.clan.members.get(i2);
                                    js_in.add(mem.name);
                                    js_in.add(mem.level);
                                    js_in.add(mem.levelInclan);
                                    js_in.add(mem.donate);
                                    js_in.add(mem.gopRuby);
                                    js_in.add(mem.numquest);
                                    js_in.add(mem.conghien);
                                    js_in.add(mem.head);
                                    js_in.add(mem.hair);
                                    js_in.add(mem.hat);
                                    js_in.add(mem.clazz);
                                    js_members.add(js_in);
                                }
                                psUpdateClan.setString(1, js_members.toJSONString());
                                psUpdateClan.setInt(2, p.clan.id);
                                psUpdateClan.executeUpdate();
                            }
                        }

                        // Cập nhật tên trong danh sách yêu cầu vào clan và chat của clan
                        for (Clan clan : Clan.ENTRY) {
                            for (Clan_member member : clan.mem_request) {
                                if (member.name.equals(p.name)) {
                                    member.name = newName;
                                    for (Clan_chat chat : clan.chat) {
                                        chat.str = chat.str.replace(p.name, newName);
                                    }
                                    break;
                                }
                            }
                        }

                        // Cập nhật tên trong danh sách bạn và kẻ thù
                        // friend, enemy
                        for (int i = 0; i < p.friend_list.size(); i++) {
                            Player p0 = Map.get_player_by_name_allmap(p.friend_list.get(i).name);
                            if (p0 != null) {
                                for (int j = 0; j < p0.friend_list.size(); j++) {
                                    if (p0.friend_list.get(j).name.equals(p.name)) {
                                        p0.friend_list.get(j).name = newName;
                                        break;
                                    }
                                }
                            } else {
                                try (Connection conn = SQL.gI().getCon();
                                        PreparedStatement ps = conn.prepareStatement(
                                                "select `id`,`name`,`friend`,`enemy` from `players` where `name` = '"
                                                        + p.friend_list.get(i).name + "'");
                                        ResultSet rs = ps.executeQuery();) {
                                    if (rs.next()) {
                                        int idP = rs.getInt("id");
                                        String friend = rs.getString("friend").replace(p.name, newName);
                                        String enemy = rs.getString("enemy").replace(p.name, newName);
                                        try (Connection conn2 = SQL.gI().getCon();
                                                PreparedStatement ps2 = conn2.prepareStatement(
                                                        "update `players` set `friend` = ?, `enemy` = ? where `id` = ?")) {
                                            ps2.setString(1, friend);
                                            ps2.setString(2, enemy);
                                            ps2.setInt(3, idP);
                                            ps2.executeUpdate();
                                        }
                                    }
                                } catch (SQLException e) {
                                    GameLogger.error("ClientInput: SQL error updating friend/enemy name for " + p.name
                                            + " during rename", e);
                                }
                            }
                        }
                        connection.commit();
                        p.item.remove_item47(4, 271, 1);
                        p.item.update_Inventory(-1, false);
                        String oldName = p.name;
                        p.name = newName;
                        Service.send_box_ThongBao_OK(p,
                                "Đổi tên thành công \n Tên mới : " + newName + " \nVui lòng thoát game vào lại");
                        try (FileWriter fw = new FileWriter("doi_ten_log.txt", true);
                                BufferedWriter bw = new BufferedWriter(fw);
                                PrintWriter out = new PrintWriter(bw)) {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                            String user = (p.conn != null) ? p.conn.user : "UnknownUser";
                            out.println("[" + timestamp + "] User: " + user + " | OldName: " + oldName + " -> NewName: "
                                    + newName);
                        } catch (IOException e) {
                            GameLogger.warn("ClientInput: Error writing name change log for " + p.name, e);
                        }

                    } catch (SQLException e) {
                        GameLogger.error("ClientInput: SQL error during name change for " + p.name, e);
                        Service.send_box_ThongBao_OK(p, "Đã xảy ra lỗi");
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Tên không hợp lệ");
                }
                break;
            }

            case 13: {
                if (name.length == 1) {

                    String xacnhan = name[0];
                    if (xacnhan.equals("xoabang")) {
                        Service.send_box_yesno(p, 63, "Thông báo",
                                ("Bạn có muốn xóa băng "
                                        + p.clan.name + " không?"),
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                    } else {
                        Service.send_box_ThongBao_OK(p,
                                "Bạn đã nhập sai nội dung");
                    }

                }
                break;
            }

            case 12: {
                if (name.length == 1) {
                    // Tìm thành viên mới theo tên nhập vào
                    String newLeaderName = name[0];
                    // Tìm thành viên mới theo tên nhập vào
                    Clan_member newLeader = null;
                    for (Clan_member member : p.clan.members) {
                        if (member.name.equals(newLeaderName)) {
                            newLeader = member;
                            break;
                        }
                    }

                    if (newLeader != null) {
                        // Cập nhật thông tin thuyền trưởng mới
                        Clan_member oldLeader = p.clan.members.get(0);
                        oldLeader.levelInclan = 10; // Cập nhật thuyền trưởng cũ thành thuyền phó hoặc cấp khác
                        newLeader.levelInclan = 0; // Cập nhật thành viên mới thành thuyền trưởng

                        p.clan.members.remove(oldLeader);
                        p.clan.members.remove(newLeader);
                        p.clan.members.add(0, newLeader); // Thêm thành viên mới lên vị trí đầu
                        p.clan.members.add(oldLeader); // Thêm thuyền trưởng cũ vào vị trí cuối

                        // Thông báo cho các thành viên về việc thay đổi thuyền trưởng
                        for (Clan_member member : p.clan.members) {
                            Player p0ther = Map.get_player_by_name_allmap(member.name);
                            if (p0ther != null) {
                                Clan.update_list_member(p0ther, false);
                                Clan.send_info(p0ther, false);

                                //
                                for (int i = 0; i < p.map.players.size(); i++) {
                                    if (!p.map.players.get(i).equals(p0ther)) {
                                        Clan.send_me_to_other(p0ther, p.map.players.get(i), false);
                                    }
                                }
                            }
                        }

                        Service.send_box_ThongBao_OK(p,
                                "Nhường chức thuyền trưởng cho " + newLeaderName + " thành công");
                        // Thông báo cho người nhận chức thuyền trưởng
                        Player newLeaderPlayer = Map.get_player_by_name_allmap(newLeader.name);
                        if (newLeaderPlayer != null) {
                            Service.send_box_ThongBao_OK(newLeaderPlayer,
                                    "Bạn đã trở thành thuyền trưởng của băng " + p.clan.name);
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Không tìm thấy thành viên có tên " + newLeaderName);
                    }
                }
                break;
            }

            case 11: {
                if (name.length == 1) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value <= 0 || value > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.clan != null) {
                        if (p.get_coin() < value) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(value) + " Coin");
                            return;
                        }
                        for (Clan_member mem : p.clan.members) {
                            if (mem.name.equals(p.name)) {
                                mem.gopRuby += value;
                                break;
                            }
                        }
                        p.update_coin(-value);
                        p.update_money();
                        p.clan.update_ruby((int) value);
                        for (int i = 0; i < p.clan.members.size(); i++) {
                            Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i).name);
                            if (p0 != null) {
                                Clan.send_info(p0, false);
                            }
                        }
                        Service.send_box_ThongBao_OK(p, "Đã góp " + value + " Ruby vào Băng");
                        p.clan.chat_on_board((short) p.id, p.name,
                                p.getNameShow() + " Đã góp " + Util.number_format(value) + " Ruby Băng", -3);
                    }
                }
                break;
            }
            case 10: {
                if (name.length == 1) {
                    if (name[0].length() < 3 || name[0].length() > 15) {
                        Service.send_box_ThongBao_OK(p, "Tên băng phải có từ 3 đến 15 ký tự.");
                        return;
                    }

                    if (!name[0].matches("[a-zA-Z0-9\\p{L}\\s]+")) {
                        Service.send_box_ThongBao_OK(p, "Tên băng không được chứa ký tự đặc biệt.");
                        return;
                    }
                    if (p.get_coin() < 50_000) {
                        Service.send_box_ThongBao_OK(p, "Không đủ 50.000 Coin");
                        return;
                    }
                    if (Clan.get_clan_by_name(name[0]) != null) {
                        Service.send_box_ThongBao_OK(p,
                                "Tên băng này đã được sử dụng, hãy sử dụng tên khác");
                        return;
                    }
                    if (name[0].toLowerCase().contains("admin")) {
                        Service.send_box_ThongBao_OK(p, "Không được đặt tên băng là admin");
                        return;
                    }
                    //
                    Clan clan = new Clan();
                    clan.id = (short) Clan.get_clan_id();
                    clan.name = name[0];
                    clan.opAttri = new short[] { 0, 0, 0, 0, 0 };
                    clan.pointAttri = 10;
                    clan.maxAttri = 50;
                    clan.icon = 0;
                    clan.level = 1;
                    clan.xp = 0;
                    clan.thongbao = "";
                    clan.trungsinh = 0;
                    clan.countAction = 0;
                    clan.allowRequest = 1;
                    clan.chat = new ArrayList<>();
                    clan.mem_request = new ArrayList<>();
                    clan.list_it = new ArrayList<>();
                    clan.buff = new ArrayList<>();
                    //
                    clan.members = new ArrayList<>();
                    Clan_member mem = new Clan_member();
                    mem.name = p.name;
                    mem.conghien = 0;
                    mem.donate = 0;
                    mem.gopRuby = 0;
                    mem.numquest = 3;
                    mem.id = 0;
                    mem.hair = (short) p.get_hair();
                    mem.head = (short) p.get_head();
                    mem.hat = p.get_hat();
                    mem.level = p.level;
                    mem.levelInclan = 0;
                    mem.clazz = p.clazz;
                    clan.members.add(mem);
                    //
                    if (Clan.create_new_clan(clan)) {
                        p.update_coin(-50_000);
                        p.update_money();
                        //
                        p.clan = clan;
                        Clan.send_info(p, false);
                        for (int i = 0; i < p.map.players.size(); i++) {
                            if (!p.map.players.get(i).equals(p)) {
                                Clan.send_me_to_other(p, p.map.players.get(i), false);
                            }
                        }
                        Message m = new Message(-19); // show table select icon
                        m.writer().writeByte(98);
                        m.writer().writeUTF("Cửa hàng biểu tượng");
                        m.writer().writeByte(107);
                        m.writer().writeShort(10);
                        for (int i = 0; i < 10; i++) {
                            m.writer().writeShort(i);
                            m.writer().writeShort(i);
                            m.writer().writeUTF("Huy hiệu " + (i + 1));
                            m.writer().writeUTF(
                                    "Được làm từ gì đấy không biết nữa, mua đeo vào rất đẹp");
                            m.writer().writeShort(0);
                        }
                        p.addmsg(m);
                        m.cleanup();
                    } else {
                        Service.send_box_ThongBao_OK(p,
                                "Tên băng này đã được sử dụng, hãy thử lại tên khác");
                    }
                }
                break;
            }
            case 7: {
                if (name.length == 1 && p.data_yesno != null && p.data_yesno.length == 3) {
                    long value;
                    try {
                        value = Long.parseLong(name[0].trim());
                    } catch (NumberFormatException e) {
                        core.GameLogger.error("ClientInput.process: Error parsing long value", e);
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ hoặc quá lớn");
                        return;
                    }

                    if (value < 10_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối thiểu là 10.000 Extol");
                        return;
                    }
                    if (value > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối đa là 2.000.000.000 Extol");
                        return;
                    }
                    int type = p.data_yesno[0];
                    int id_ = p.data_yesno[1];
                    int value_ = p.data_yesno[2];
                    p.data_yesno = new int[] { type, id_, value_, (int) value };
                    if (type == 4) {
                        Service.send_box_yesno(p, 22, "Thông báo",
                                ("Bạn có muốn bán " + value_ + " "
                                        + ItemTemplate4.get_item_name(id_) + " với giá "
                                        + Util.number_format(value)
                                        + " Extol? Phí để đăng bán là 2.000 Extol"),
                                new String[] { "2.000 Extol", "Không" }, new byte[] { -1, -1 });
                    } else if (type == 7) {
                        Service.send_box_yesno(p, 22, "Thông báo",
                                ("Bạn có muốn bán " + value_ + " "
                                        + ItemTemplate7.get_item_name(id_) + " với giá "
                                        + Util.number_format(value)
                                        + " Extol? Phí để đăng bán là 2.000 Extol"),
                                new String[] { "2.000 Extol", "Không" }, new byte[] { -1, -1 });
                    }
                }
                break;
            }
            case 6: {
                if (name.length == 1 && p.data_yesno != null && p.data_yesno.length == 1) {
                    long value;
                    try {
                        value = Long.parseLong(name[0].trim());
                    } catch (NumberFormatException e) {
                        core.GameLogger.error("ClientInput.process: Error parsing long value", e);
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ hoặc quá lớn");
                        return;
                    }

                    if (value < 10_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối thiểu là 10.000 Extol");
                        return;
                    }
                    if (value > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối đa là 2.000.000.000 Extol");
                        return;
                    }
                    int price = p.data_yesno[0];
                    p.data_yesno = new int[] { price, (int) value };
                    Service.send_box_yesno(p, 18, "Thông báo",
                            ("Bạn có muốn bán " + price + " triệu beri với giá "
                                    + Util.number_format(value)
                                    + " Extol? Phí để đăng bán là 2.000 Extol"),
                            new String[] { "2.000 Extol", "Không" }, new byte[] { -1, -1 });
                }
                break;
            }
            case 5: {
                if (name.length == 1 && p.data_yesno != null) {
                    long value;
                    try {
                        value = Long.parseLong(name[0].trim());
                    } catch (NumberFormatException e) {
                        core.GameLogger.error("ClientInput.process: Error parsing long value", e);
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ hoặc quá lớn");
                        return;
                    }

                    if (value < 10_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối thiểu là 10.000 Extol");
                        return;
                    }
                    if (value > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p,
                                "Mức giá bán tối đa là 2.000.000.000 Extol");
                        return;
                    }
                    Item_wear it_select = p.item.bag3[p.data_yesno[0]];
                    if (it_select != null) {
                        p.data_yesno = new int[] { it_select.index, (int) value };
                        Service.send_box_yesno(p, 17, "Thông báo",
                                ("Bạn có muốn bán vật phẩm " + it_select.template.name + " với giá "
                                        + Util.number_format(value)
                                        + " Extol? Phí để đăng bán là 2.000 Extol"),
                                new String[] { "2.000 Extol", "Không" }, new byte[] { -1, -1 });
                    }
                }
                break;
            }
            case 19: {
                if (name.length == 1 && p.data_yesno == null) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.get_coin() < value) {
                        Service.send_box_ThongBao_OK(p, "Bạn không đủ " + Util.number_format(value) + " Coin");
                        return;
                    }
                    long beri = (long) value * 10_000L;
                    p.data_yesno = new int[] { value };
                    Service.send_box_yesno(p, 101, "Thông báo",
                            "Bạn có muốn đổi \n" + Util.number_format(value)
                                    + " Coin sang " + Util.number_format(beri) + " Beri",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                    break;
                }
                break;
            }
            case 20: {
                if (name.length == 1 && p.data_yesno == null) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.get_coin() < value) {
                        Service.send_box_ThongBao_OK(p, "Bạn không đủ " + Util.number_format(value) + " Coin");
                        return;
                    }
                    int ruby = value * 1000;
                    p.data_yesno = new int[] { value };
                    Service.send_box_yesno(p, 102, "Thông báo",
                            "Bạn có muốn đổi \n" + Util.number_format(value)
                                    + " Coin sang " + Util.number_format(ruby) + " Ruby",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                    break;
                }
                break;
            }
            case 4: {
                if (name.length == 1 && p.data_yesno == null) {
                    if (!Util.isnumber(name[0])) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    int value = Integer.parseInt(name[0]);
                    if (value <= 0) {
                        Service.send_box_ThongBao_OK(p, "Số nhập không hợp lệ");
                        return;
                    }
                    if (p.get_coin() < value) {
                        Service.send_box_ThongBao_OK(p, "Bạn không đủ " + Util.number_format(value) + " Coin");
                        return;
                    }
                    int extol = value * 1; // Vì 1 coin = 1 extol
                    p.data_yesno = new int[] { value };
                    Service.send_box_yesno(p, 9, "Thông báo",
                            "Bạn có muốn đổi \n" + Util.number_format(value)
                                    + " Coin sang " + Util.number_format(extol) + " Extol",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                    break;
                }
                break;
            }

            case 2: {
                if (name.length == 2) {
                    name[0] = name[0].replace(" ", "");
                    name[1] = name[1].replace(" ", "");
                    name[0] = name[0].toLowerCase();
                    name[1] = name[1].toLowerCase();
                    if (name[0].contains("admin") || name[1].contains("admin")) {
                        Service.send_box_ThongBao_OK(p,
                                "Tên tài khoản và mật khẩu không được trùng admin!");
                        return;
                    }
                    Pattern pat = Pattern.compile("^[a-zA-Z0-9@.]{1,30}$");
                    if (!pat.matcher(name[0]).matches() || !pat.matcher(name[1]).matches()) {
                        Service.send_box_ThongBao_OK(p,
                                "Tên tài khoản và mật khẩu phải dài hơn 6 và không chứa ký tự đặc biệt!");
                        return;
                    }
                    Connection conn = null;
                    Statement st = null;
                    try {
                        conn = SQL.gI().getCon();
                        st = conn.createStatement();
                        st.executeUpdate(
                                "UPDATE `accounts` SET `user` = '" + name[0] + "', `pass` = '"
                                        + name[1] + "' WHERE BINARY `user` = '" + p.conn.user
                                        + "' AND BINARY `pass` = '" + p.conn.pass + "' LIMIT 1;");
                    } catch (SQLException e) {
                        GameLogger.warn(
                                "ClientInput: Account update failed for " + p.name + " - Username might already exist",
                                e);
                        Service.send_box_ThongBao_OK(p, "Tên đã được sử dụng, hãy thử lại!");
                        return;
                    } finally {
                        try {
                            if (st != null) {
                                st.close();
                            }
                            if (conn != null) {
                                conn.close();
                            }
                        } catch (SQLException e) {
                            GameLogger.warn(
                                    "ClientInput: SQL error closing resources during account update for " + p.name, e);
                        }
                    }
                    p.conn.user = name[0];
                    p.conn.pass = name[1];
                    Message m = new Message(-59);
                    m.writer().writeUTF(name[0]);
                    m.writer().writeUTF(name[1]);
                    p.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 1: {
                if (name.length == 1) {
                    Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{1,20}$");
                    if (!pattern.matcher(name[0]).matches()) {
                        Service.send_box_ThongBao_OK(p, "Ký tự không hợp lệ");
                        return;
                    }
                    Service.send_box_ThongBao_OK(p, "Xin hãy đợi giây lát...");
                    GiftTemplate temp = null;
                    for (GiftTemplate gt : GiftTemplate.ENTRY) {
                        if (gt.giftname.equals(name[0])) {
                            temp = gt;
                            break;
                        }
                    }
                    if (temp == null) {
                        Service.send_box_ThongBao_OK(p, "Giftcode không tồn tại");
                        return;
                    }

                    if (temp != null) {
                        if (temp.gioihan != -1 && temp.luotnhap >= temp.gioihan) {
                            Service.send_box_ThongBao_OK(p, "Giftcode này đã đạt lượt nhập tối đa!");
                            return;
                        }

                        if (!temp.giftname.toLowerCase().startsWith("vip")) {
                            String[] used_ = temp.used.split(",");
                            for (String u : used_) {
                                if (!u.isBlank() && u.equals(p.conn.user)) {
                                    Service.send_box_ThongBao_OK(p, "Bạn đã nhập Giftcode này rồi");
                                    return;
                                }
                            }
                        }

                        if (!temp.special.isEmpty()) {
                            boolean canReceive = false;
                            String[] allow_ = temp.special.split(",");
                            for (String u : allow_) {
                                if (!u.isBlank() && u.equals(p.conn.user)) {
                                    canReceive = true;
                                    break;
                                }
                            }
                            if (!canReceive) {
                                Service.send_box_ThongBao_OK(p, "Bạn không có tên trong danh sách nhận Giftcode này!");
                                return;
                            }
                        }
                        List<String> vipCodes = null;
                        if (temp.giftname.toLowerCase().startsWith("vip")) {
                            String codeSql = null;
                            try (Connection conn2 = SQL.gI().getCon();
                                    PreparedStatement ps = conn2.prepareStatement(
                                            "SELECT `code` FROM `accounts` WHERE `user` = ? LIMIT 1")) {
                                ps.setString(1, p.conn.user);
                                ResultSet rs2 = ps.executeQuery();
                                if (rs2.next()) {
                                    codeSql = rs2.getString("code");
                                }
                                rs2.close();
                            } catch (SQLException e) {
                                GameLogger.error("ClientInput: SQL error getting VIP code for " + p.name, e);
                                return;
                            }

                            vipCodes = new ArrayList<>();
                            if (codeSql != null && !codeSql.isEmpty()) {
                                for (String s : codeSql.split(",")) {
                                    if (!s.trim().isEmpty())
                                        vipCodes.add(s.trim());
                                }
                            }

                            int count = Collections.frequency(vipCodes, temp.giftname);
                            if (count <= 0) {
                                Service.send_box_ThongBao_OK(p, "Bạn không có Giftcode VIP này");
                                return;
                            }
                        }
                        int slotsNeeded = 0;
                        if (temp.type != null) {
                            List<String> uniqueStackables = new ArrayList<>();
                            for (int i = 0; i < temp.type.length; i++) {
                                if (temp.type[i] == 3) {
                                    slotsNeeded++;
                                } else {
                                    String key = temp.type[i] + "_" + temp.id[i];
                                    if (!uniqueStackables.contains(key)) {
                                        if (p.item.total_item_bag_by_id(temp.type[i], temp.id[i]) == 0) {
                                            slotsNeeded++;
                                            uniqueStackables.add(key);
                                        }
                                    }
                                }
                            }
                        }
                        if (slotsNeeded > p.item.able_bag()) {
                            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống. Cần ít nhất "
                                    + slotsNeeded + " ô trống.");
                            return;
                        }
                        List<GiftBox> listGift = new ArrayList<>();
                        if (temp.beri > 0) {
                            ItemTemplate4 it = ItemTemplate4.get_it_by_id(0);
                            if (it != null) {
                                GiftBox gb = new GiftBox();
                                gb.id = it.id;
                                gb.type = 4;
                                gb.name = it.name;
                                gb.icon = it.icon;
                                gb.num = temp.beri;
                                gb.color = 0;
                                listGift.add(gb);
                            }
                        }
                        if (temp.ruby > 0) {
                            ItemTemplate4 it = ItemTemplate4.get_it_by_id(1);
                            if (it != null) {
                                GiftBox gb = new GiftBox();
                                gb.id = it.id;
                                gb.type = 4;
                                gb.name = it.name;
                                gb.icon = it.icon;
                                gb.num = temp.ruby;
                                gb.color = 0;
                                listGift.add(gb);
                            }
                        }
                        if (temp.extol > 0) {
                            ItemTemplate4 it = ItemTemplate4.get_it_by_id(0); // Dùng icon beri tạm hoặc kiếm ID extol
                                                                              // nếu có
                            if (it != null) {
                                GiftBox gb = new GiftBox();
                                gb.id = it.id;
                                gb.type = 4;
                                gb.name = "Extol";
                                gb.icon = 3385; // Icon extol nếu đúng
                                gb.num = temp.extol;
                                gb.color = 0;
                                listGift.add(gb);
                                p.update_vnd(temp.extol);
                            }
                        }
                        if (temp.coin > 0) {
                            ItemTemplate4 it = ItemTemplate4.get_it_by_id(0);
                            if (it != null) {
                                GiftBox gb = new GiftBox();
                                gb.id = it.id;
                                gb.type = 4;
                                gb.name = "Coin";
                                gb.icon = 2951;
                                gb.num = temp.coin;
                                gb.color = 0;
                                listGift.add(gb);
                                p.update_coin((int) temp.coin);
                            }
                        }
                        if (temp.type != null) {
                            for (int i = 0; i < temp.type.length; i++) {
                                GiftBox gb = new GiftBox();
                                if (temp.type[i] == 3) {
                                    ItemTemplate3 it = ItemTemplate3.get_it_by_id(temp.id[i]);
                                    if (it != null) {
                                        gb.id = it.id;
                                        gb.type = 3;
                                        gb.name = it.name;
                                        gb.icon = it.icon;
                                        gb.num = 1;
                                        gb.color = it.color;
                                    }
                                } else if (temp.type[i] == 4) {
                                    ItemTemplate4 it = ItemTemplate4.get_it_by_id(temp.id[i]);
                                    if (it != null) {
                                        gb.id = it.id;
                                        gb.type = 4;
                                        gb.name = it.name;
                                        gb.icon = it.icon;
                                        gb.num = temp.quant[i];
                                        gb.color = 0;
                                    }
                                } else if (temp.type[i] == 7) {
                                    ItemTemplate7 it = ItemTemplate7.get_it_by_id(temp.id[i]);
                                    if (it != null) {
                                        gb.id = it.id;
                                        gb.type = 7;
                                        gb.name = it.name;
                                        gb.icon = it.icon;
                                        gb.num = temp.quant[i];
                                        gb.color = 0;
                                    }
                                }
                                listGift.add(gb);
                            }
                        }
                        Service.send_gift(p, 1, "Giftcode", "", listGift, true);
                        if (!temp.giftname.toLowerCase().startsWith("vip")) {
                            GiftTemplate.update_used(temp, p.conn.user);
                        }
                        if (vipCodes != null) {
                            vipCodes.remove(temp.giftname);
                            String updatedCode = String.join(",", vipCodes);
                            try (Connection conn3 = SQL.gI().getCon();
                                    PreparedStatement ps = conn3.prepareStatement(
                                            "UPDATE `accounts` SET `code` = ? WHERE `user` = ?")) {
                                ps.setString(1, updatedCode);
                                ps.setString(2, p.conn.user);
                                ps.executeUpdate();
                            } catch (SQLException e) {
                                GameLogger.error("ClientInput: SQL error updating VIP code after use for " + p.name, e);
                            }
                        }
                    }
                }
                break;
            }

        }
    }
}
