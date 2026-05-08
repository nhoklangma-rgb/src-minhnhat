package activities;

import client.Item;
import client.Player;
import core.GameLogger;
import core.Manager;
import core.Service;
import core.Util;
import io.Message;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import template.*;

public class Trade {
    private static final String LOG_FOLDER = "trade_log";

    public static void process(Player p, Message m2) throws IOException {
        byte action = m2.reader().readByte();
        int id = -1;
        byte cat = -1;
        int num = -1;
        String str = "";
        if (action == 1 || action == 6) {
            id = m2.reader().readShort();
            cat = m2.reader().readByte();
            num = m2.reader().readInt();
        }
        if (action == 2) {
            str = m2.reader().readUTF();
        }
        switch (action) {
            case 6: {
                if (num == 1) { // accept
                    if (p.getTongNap() == 0 && !Manager.gI().server_test) {
                        Service.send_box_ThongBao_OK(p, "Bạn chưa nạp lần đầu.");
                        return;
                    }
                    Player p0 = p.map.get_player_by_id_inmap(id);
                    if (p0 != null) {
                        if (p0.trade_target == null || p0.trade_target.id != p.id) {
                            Service.send_box_ThongBao_OK(p,
                                    "Đối phương đang giao dịch với người khác hoặc lời mời đã hết hạn");
                            p.fee_trade = 0;
                            p.money_trade = 0;
                            p.is_lock_trade = false;
                            p.is_accept_trade = false;
                            p.list_item_trade3 = null;
                            p.list_item_trade47 = null;
                            p.trade_target = null;
                            return;
                        }
                        // Đảm bảo cả 2 bên đều nhận diện đúng mục tiêu giao dịch
                        p.trade_target = p0;
                        p0.trade_target = p;

                        Trade.show_table(p, p0.name);
                        Trade.show_table(p0, p.name);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Đối phương không online");
                    }
                } else if (num == 0) { // request
                    if (p.getTongNap() == 0 && !Manager.gI().server_test) {
                        Service.send_box_ThongBao_OK(p, "Bạn chưa nạp lần đầu.");
                        return;
                    }
                    Player p0 = p.map.get_player_by_id_inmap(id);
                    if (p0 != null) {
                        if (p0.getTongNap() == 0 && !Manager.gI().server_test) {
                            Service.send_box_ThongBao_OK(p, "Đối phương chưa nạp lần đầu.");
                            return;
                        }
                        if (p0.trade_target != null) {
                            Service.send_box_ThongBao_OK(p, "Đối phương đang có giao dịch");
                            return;
                        }
                        if (p.trade_target != null) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn đang đợi đối phương chấp nhận lời mời giao dịch");
                            return;
                        }
                        p0.trade_target = p;
                        p.trade_target = p0;
                        //
                        Message m = new Message(-49);
                        m.writer().writeByte(6);
                        m.writer().writeByte(1);
                        m.writer().writeShort(p.index_map);
                        m.writer().writeUTF(p.name);
                        p0.addmsg(m);
                        m.cleanup();
                    } else {
                        Service.send_box_ThongBao_OK(p, "Đối phương không online");
                    }
                }
                break;
            }
            case 1: {
                if (p.trade_target != null && p.is_lock_trade) {
                    Service.send_box_ThongBao_OK(p, "Không thể thực hiện khi đã khóa giao dịch");
                    return;
                }
                if (num == 1 && cat == 3 && p.trade_target != null) { // add item vao
                    Item_wear it_select = p.item.bag3[id];
                    if (it_select != null) {
                        boolean send_or_remove = true;
                        for (int i = 0; i < p.list_item_trade3.size(); i++) {
                            if (it_select.equals(p.list_item_trade3.get(i))) {
                                p.list_item_trade3.remove(it_select);
                                send_or_remove = false;
                                break;
                            }
                        }
                        if ((p.trade_target.item.able_bag() - p.list_item_trade3.size()
                                - p.list_item_trade47.size()) < 1) {
                            Service.send_box_ThongBao_OK(p,
                                    "Hành trang đối phương không đủ chỗ trống");
                            return;
                        }
                        if (send_or_remove) {
                            if (Trade.can_add_item_trade(p)) {
                                if (it_select.typelock == 1) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Trang bị đã khóa không thể giao dịch!");
                                    return;
                                }
                                Message m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(1);
                                m.writer().writeByte(3);
                                m.writer().writeByte(1);
                                Item.readUpdateItem(m.writer(), it_select, p);
                                p.trade_target.addmsg(m);
                                m.cleanup();
                                //
                                m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(0);
                                m.writer().writeByte(3);
                                m.writer().writeByte(1);
                                Item.readUpdateItem(m.writer(), it_select, p);
                                p.addmsg(m);
                                m.cleanup();
                                p.list_item_trade3.add(it_select);
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không thể thêm vật phẩm");
                            }
                        } else {
                            Message m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(1);
                            m.writer().writeByte(3);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.trade_target.addmsg(m);
                            m.cleanup();
                            //
                            m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(0);
                            m.writer().writeByte(3);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                } else if (num > 0 && cat == 6 && id == 0 && p.trade_target != null) { // add beri
                    if (num > 2_000_000_000) {
                        Service.send_box_ThongBao_OK(p, "Giao dịch tối đa 2 tỷ");
                    }
                    long beri_quant = (long) num * 130L / 100L;
                    if (p.get_vang() < beri_quant) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(beri_quant)
                                + " beri, phí giao dịch beri 30%");
                        return;
                    }
                    //
                    Message m = new Message(-49);
                    m.writer().writeByte(1);
                    m.writer().writeByte(1);
                    m.writer().writeByte(6);
                    m.writer().writeInt(num);
                    p.trade_target.addmsg(m);
                    m.cleanup();
                    //
                    m = new Message(-49);
                    m.writer().writeByte(1);
                    m.writer().writeByte(0);
                    m.writer().writeByte(6);
                    m.writer().writeInt(num);
                    p.addmsg(m);
                    m.cleanup();
                    //
                    p.money_trade = num;
                } else if (num > 0 && cat == 7 && p.trade_target != null) { // add item7
                    ItemTemplate7 it_temp = ItemTemplate7.get_it_by_id(id);
                    if (it_temp != null) {
                        boolean send_or_remove = true;
                        for (int i = 0; i < p.list_item_trade47.size(); i++) {
                            if (p.list_item_trade47.get(i).category == 7
                                    && p.list_item_trade47.get(i).id == id) {
                                send_or_remove = false;
                                p.list_item_trade47.remove(p.list_item_trade47.get(i));
                                break;
                            }
                        }
                        if (send_or_remove) {
                            if (p.item.total_item_bag_by_id(7, id) < num) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không đủ " + num + " " + it_temp.name);
                                return;
                            }
                            if (it_temp.istrade == 1) {
                                Service.send_box_ThongBao_OK(p,
                                        "Vật phẩm này không thể giao dịch!");
                                return;
                            }
                            int num_in_target_bag = p.trade_target.item.total_item_bag_by_id(7, id);
                            if (num > DataTemplate.MAX_ITEM_IN_BAG
                                    || (num_in_target_bag + num) > DataTemplate.MAX_ITEM_IN_BAG
                                    || (num_in_target_bag == 0 && (p.trade_target.item.able_bag()
                                            - p.list_item_trade3.size()
                                            - p.list_item_trade47.size()) < 1)) {
                                Service.send_box_ThongBao_OK(p,
                                        "Hành trang đối phương không đủ chỗ trống");
                                return;
                            }
                            if (it_temp.istrade == 0 && Trade.can_add_item_trade(p)) {
                                for (int i = 0; i < p.item.bag47.size(); i++) {
                                    if (p.item.bag47.get(i).category == 7
                                            && p.item.bag47.get(i).id == id) {
                                        ItemBag47 it_add = new ItemBag47();
                                        it_add.category = 7;
                                        it_add.id = (short) id;
                                        it_add.quant = (short) num;
                                        p.list_item_trade47.add(it_add);
                                        break;
                                    }
                                }
                                Message m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(1);
                                m.writer().writeByte(7);
                                m.writer().writeByte(1);
                                m.writer().writeByte(id);
                                m.writer().writeShort(num);
                                p.trade_target.addmsg(m);
                                m.cleanup();
                                //
                                m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(0);
                                m.writer().writeByte(7);
                                m.writer().writeByte(1);
                                m.writer().writeByte(id);
                                m.writer().writeShort(num);
                                p.addmsg(m);
                                m.cleanup();
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không thể thêm vật phẩm");
                            }
                        } else {
                            Message m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(1);
                            m.writer().writeByte(7);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.trade_target.addmsg(m);
                            m.cleanup();
                            //
                            m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(0);
                            m.writer().writeByte(7);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                } else if (num > 0 && cat == 4 && p.trade_target != null) { // add item4
                    ItemTemplate4 it_temp = ItemTemplate4.get_it_by_id(id);
                    if (it_temp != null) {
                        boolean send_or_remove = true;
                        for (int i = 0; i < p.list_item_trade47.size(); i++) {
                            if (p.list_item_trade47.get(i).category == 4
                                    && p.list_item_trade47.get(i).id == id) {
                                send_or_remove = false;
                                p.list_item_trade47.remove(p.list_item_trade47.get(i));
                                break;
                            }
                        }
                        int num_in_target_bag = p.trade_target.item.total_item_bag_by_id(4, id);
                        if (num > DataTemplate.MAX_ITEM_IN_BAG
                                || (num_in_target_bag + num) > DataTemplate.MAX_ITEM_IN_BAG
                                || (num_in_target_bag == 0 && (p.trade_target.item.able_bag()
                                        - p.list_item_trade3.size()
                                        - p.list_item_trade47.size()) < 1)) {
                            Service.send_box_ThongBao_OK(p,
                                    "Hành trang đối phương không đủ chỗ trống");
                            return;
                        }
                        if (send_or_remove) {
                            if (p.item.total_item_bag_by_id(4, id) < num) {
                                Service.send_box_ThongBao_OK(p,
                                        "Không đủ " + num + " " + it_temp.name);
                                return;
                            }
                            if (it_temp.istrade == 1) {
                                Service.send_box_ThongBao_OK(p,
                                        "Vật phẩm này không thể giao dịch!");
                                return;
                            }
                            if (it_temp.istrade == 0 && Trade.can_add_item_trade(p)) {
                                for (int i = 0; i < p.item.bag47.size(); i++) {
                                    if (p.item.bag47.get(i).category == 4
                                            && p.item.bag47.get(i).id == id) {
                                        ItemBag47 it_add = new ItemBag47();
                                        it_add.category = 4;
                                        it_add.id = (short) id;
                                        it_add.quant = (short) num;
                                        p.list_item_trade47.add(it_add);
                                        break;
                                    }
                                }
                                Message m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(1);
                                m.writer().writeByte(4);
                                m.writer().writeByte(1);
                                m.writer().writeShort(id);
                                m.writer().writeShort(num);
                                p.trade_target.addmsg(m);
                                m.cleanup();
                                //
                                m = new Message(-49);
                                m.writer().writeByte(1);
                                m.writer().writeByte(0);
                                m.writer().writeByte(4);
                                m.writer().writeByte(1);
                                m.writer().writeShort(id);
                                m.writer().writeShort(num);
                                p.addmsg(m);
                                m.cleanup();
                            } else {
                                Service.send_box_ThongBao_OK(p, "Không thể thêm vật phẩm");
                            }
                        } else {
                            Message m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(1);
                            m.writer().writeByte(4);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.trade_target.addmsg(m);
                            m.cleanup();
                            //
                            m = new Message(-49);
                            m.writer().writeByte(1);
                            m.writer().writeByte(0);
                            m.writer().writeByte(4);
                            m.writer().writeByte(0);
                            m.writer().writeShort(id);
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                }
                break;
            }
            case 5: { // thoat trade
                if (id == -1 && cat == -1 && num == -1 && p.trade_target != null) {
                    end_trade_by_disconnect(p.trade_target, p, 0, "");
                    end_trade_by_disconnect(p, p.trade_target, 0, "");
                }
                break;
            }
            case 2: { // chat popup
                if (id == -1 && cat == -1 && num == -1 && !str.isEmpty()
                        && p.trade_target != null) {
                    Message m = new Message(-49);
                    m.writer().writeByte(2);
                    m.writer().writeByte(1);
                    m.writer().writeUTF(str);
                    p.trade_target.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 3: { // lock
                if (id == -1 && cat == -1 && num == -1 && p.trade_target != null
                        && !p.is_lock_trade) {
                    Message m = new Message(-49);
                    m.writer().writeByte(3);
                    m.writer().writeByte(1);
                    p.trade_target.addmsg(m);
                    m.cleanup();
                    //
                    m = new Message(-49);
                    m.writer().writeByte(3);
                    m.writer().writeByte(0);
                    p.addmsg(m);
                    m.cleanup();
                    //
                    p.is_lock_trade = true;
                    if (p.money_trade > 0) {
                        p.fee_trade += (50 + (p.money_trade / 50_000));
                    }
                    for (int i = 0; i < p.list_item_trade3.size(); i++) {
                        Item_wear it_select = p.list_item_trade3.get(i);
                        p.fee_trade += it_select.template.color * 50;
                        p.fee_trade += (50 * it_select.numLoKham);
                        p.fee_trade += (50 * it_select.mdakham.length);
                        p.fee_trade += (50 * it_select.levelup);
                        if (it_select.valueKichAn > -1) {
                            p.fee_trade += 100;
                        }
                        p.fee_trade += (100 * it_select.isHoanMy);
                    }
                    //
                    int fee_item4 = 0;
                    for (int i = 0; i < p.list_item_trade47.size(); i++) {
                        fee_item4 += p.list_item_trade47.get(i).quant * 10; // Mỗi `quant` có phí là 10
                    }
                    p.fee_trade += fee_item4;
                    //
                    Service.send_box_ThongBao_OK(p,
                            "Bạn đã khóa giao dịch \n == Phí giao dịch == \n" + String.format("%,d", p.fee_trade)
                                    + " Ruby");

                }
                break;
            }
            case 4: { // Accept trade
                synchronized (p) {
                    if (id != -1 || cat != -1 || num != -1)
                        break;
                    if (p.trade_target == null || p.trade_target == p || !p.is_lock_trade
                            || !p.trade_target.is_lock_trade
                            || p.is_accept_trade)
                        break;
                    Player target = p.trade_target;
                    synchronized (target) {
                        if (p.get_ngoc() < p.fee_trade) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + String.format("%,d", p.fee_trade) + " Ruby để trả phí giao dịch");
                            return;
                        }
                        if (p.trade_target == p) {
                            Service.send_box_ThongBao_OK(p, "Không thể giao dịch với chính mình.");
                            break;
                        }
                        if (p.conn.user.equals(p.trade_target.conn.user)) {
                            Service.send_box_ThongBao_OK(p, "Không thể giao dịch giữa 2 nhân vật cùng tài khoản.");
                            break;
                        }

                        p.is_accept_trade = true;
                        if (!target.is_accept_trade) {
                            Message m = new Message(-49);
                            m.writer().writeByte(4);
                            m.writer().writeByte(1);
                            target.addmsg(m);
                            m.cleanup();

                            m = new Message(-49);
                            m.writer().writeByte(4);
                            m.writer().writeByte(0);
                            p.addmsg(m);
                            m.cleanup();
                            return;
                        }
                        // Cả 2 đã accept → bắt đầu xử lý giao dịch
                        if (p.conn == null || !p.conn.connected || target.conn == null || !target.conn.connected) {
                            Service.send_box_ThongBao_OK(p, "Giao dịch bị huỷ do mất kết nối.");
                            if (target.conn != null && target.conn.connected)
                                Service.send_box_ThongBao_OK(target, "Giao dịch bị huỷ do đối phương mất kết nối.");
                            end_trade_by_disconnect(p, target, 0, p.name);
                            end_trade_by_disconnect(target, p, 0, target.name);
                            return;
                        }
                        // Trừ phí và vàng
                        p.update_ngoc(-p.fee_trade);
                        target.update_ngoc(-target.fee_trade);
                        p.update_vang(-(p.money_trade * 130L) / 100L);
                        target.update_vang(-(target.money_trade * 130L) / 100L);
                        p.update_vang(target.money_trade);
                        target.update_vang(p.money_trade);
                        // Nếu âm vàng → rollback
                        if (p.get_vang() < 0 || target.get_vang() < 0) {
                            long adjustP = Math.max(-p.get_vang(), 0);
                            long adjustT = Math.max(-target.get_vang(), 0);
                            p.update_vang(adjustP);
                            target.update_vang(-adjustP);
                            target.update_vang(adjustT);
                            p.update_vang(-adjustT);
                        }
                        // Trao đổi vật phẩm
                        for (Item_wear it : p.list_item_trade3)
                            p.item.remove_item_wear(it);
                        for (Item_wear it : target.list_item_trade3)
                            target.item.remove_item_wear(it);
                        for (Item_wear it : target.list_item_trade3)
                            p.item.add_item_bag3(it.clone_obj());
                        for (Item_wear it : p.list_item_trade3)
                            target.item.add_item_bag3(it.clone_obj());

                        for (ItemBag47 it : p.list_item_trade47)
                            p.item.remove_item47(it.category, it.id, it.quant);
                        for (ItemBag47 it : target.list_item_trade47)
                            target.item.remove_item47(it.category, it.id, it.quant);
                        for (ItemBag47 it : target.list_item_trade47)
                            p.item.add_item_bag47(it.category, it.id, it.quant);
                        for (ItemBag47 it : p.list_item_trade47)
                            target.item.add_item_bag47(it.category, it.id, it.quant);
                        p.item.update_Inventory(-1, false);
                        target.item.update_Inventory(-1, false);
                        p.update_money();
                        target.update_money();
                        logTrade(p, target);
                        Player.flush(p, true);
                        Player.flush(target, true);
                        end_trade_by_disconnect(p, target, 1, "");
                        end_trade_by_disconnect(target, p, 1, "");
                    }
                }
                break;
            }
        }
    }

    // Ghi log vào file
    public static void logTrade(Player p, Player tradeTarget) {
        File logDir = new File(LOG_FOLDER);
        if (!logDir.exists()) {
            logDir.mkdir();
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        File logFile = new File(logDir, date + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write("Trade Date: " + date);
            writer.newLine();
            writer.write("Trade Time: " + time);
            writer.newLine();

            writer.write("Trader: " + p.name + " | Account: " + (p.conn != null ? p.conn.user : "null"));
            writer.newLine();
            writer.write("Trade Target: " + tradeTarget.name + " | Account: "
                    + (tradeTarget.conn != null ? tradeTarget.conn.user : "null"));
            writer.newLine();

            writer.write("Money traded:");
            writer.newLine();
            writer.write(" P1 -> " + p.money_trade);
            writer.newLine();
            writer.write(" P2 -> " + tradeTarget.money_trade);
            writer.newLine();

            writer.write("Items traded (Wear) by P1 :");
            writer.newLine();
            if (p.list_item_trade3 != null) {
                for (Item_wear item : p.list_item_trade3) {
                    writer.write("  " + (item != null ? item.toString() : "Unknown Item"));
                    writer.newLine();
                }
            }

            writer.write("Items traded (Wear) by P2 :");
            writer.newLine();
            if (tradeTarget.list_item_trade3 != null) {
                for (Item_wear item : tradeTarget.list_item_trade3) {
                    writer.write("  " + (item != null ? item.toString() : "Unknown Item"));
                    writer.newLine();
                }
            }

            writer.write("Items traded (47) by P1 :");
            writer.newLine();
            if (p.list_item_trade47 != null) {
                for (ItemBag47 item : p.list_item_trade47) {
                    writer.write("  " + (item != null ? item.toString() : "Unknown Item"));
                    writer.newLine();
                }
            }

            writer.write("Items traded (47) by P2 :");
            writer.newLine();
            if (tradeTarget.list_item_trade47 != null) {
                for (ItemBag47 item : tradeTarget.list_item_trade47) {
                    writer.write("  " + (item != null ? item.toString() : "Unknown Item"));
                    writer.newLine();
                }
            }

            writer.write("--------------------------------------------------");
            writer.newLine();
        } catch (IOException e) {
            GameLogger.error("Trade.logTrade: Error writing trade log for player " + p.name, e);
        }
    }

    private static boolean can_add_item_trade(Player p) {
        return (p.list_item_trade3.size() + p.list_item_trade47.size() < 4);
    }

    public static void end_trade_by_disconnect(Player p_mine, Player p_target, int type,
            String name_exit) throws IOException {
        Message m = new Message(-49);
        m.writer().writeByte(5);
        m.writer().writeByte(0);
        if (type == 1) {
            m.writer().writeUTF("Giao dịch với " + p_target.name + " hoàn tất");
        } else if (type == 0) {
            m.writer().writeUTF(p_target.name + " hủy giao dịch");
        } else if (type == 2) {
            m.writer().writeUTF("Giao dịch bị hủy bỏ vì " + name_exit
                    + " không đủ khả năng để trả phí cho giao dịch này");
        }
        if (p_mine.conn != null) {
            p_mine.addmsg(m);
        }
        m.cleanup();
        //
        Player.flush(p_mine, true);
        Player.flush(p_mine, true);
        p_mine.fee_trade = 0;
        p_mine.money_trade = 0;
        p_mine.is_lock_trade = false;
        p_mine.is_accept_trade = false;
        p_mine.list_item_trade3 = null;
        p_mine.list_item_trade47 = null;
        p_mine.trade_target = null;
    }

    public static void show_table(Player p, String name) throws IOException {
        Message m = new Message(-49);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeUTF(name);
        p.addmsg(m);
        m.cleanup();
        p.list_item_trade3 = new ArrayList<>();
        p.list_item_trade47 = new ArrayList<>();
        p.fee_trade = 0;
        p.money_trade = 0;
        p.is_lock_trade = false;
        p.is_accept_trade = false;
    }
}
