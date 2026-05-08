package activities;

import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import template.ItemBag47;
import template.ItemTemplate4;
import template.ItemTemplate7;

public class VongQuay {

    public static short[] ID_ITEM = new short[] { 34, 88, 90, 91, 92, 219, 220, 240, //
            32, 316, 161, 93, 317, 160, 33, 318, 684, 427 };

    private static ItemBag47 get_random(Player p) {
        if (94 > Util.random(120)) {
            return null;
        }
        short[] rareIds = { 160, 161, 240, 684, 427 };
        int[] rareWeights = { 1, 2, 3, 1, 1 };
        short[] commonIds = { 93, 219, 92, 32, 34, 88, 90, 91, 220, 316, 317, 33, 318 };
        int commonWeight = 46;
        int total = 0;
        for (int w : rareWeights)
            total += w;
        total += commonIds.length * commonWeight;
        int r = Util.random(total);
        int sum = 0;
        short chosenId = commonIds[commonIds.length - 1];
        for (int i = 0; i < rareIds.length; i++) {
            sum += rareWeights[i];
            if (r < sum) {
                chosenId = rareIds[i];
                break;
            }
        }
        if (r >= sum) {
            for (int i = 0; i < commonIds.length; i++) {
                sum += commonWeight;
                if (r < sum) {
                    chosenId = commonIds[i];
                    break;
                }
            }
        }
        ItemBag47 result = new ItemBag47();
        result.id = chosenId;
        result.category = 4;
        result.quant = 1;
        return result;
    }

    public static void show_table(Player p) throws IOException {
        Message m = new Message(54);
        m.writer().writeByte(0);
        p.addmsg(m);
        m.cleanup();
    }

    public static void process(Player p, Message m2) throws IOException {
        byte action = m2.reader().readByte();

        switch (action) {
            case 3: {
                Message m = new Message(54);
                m.writer().writeByte(3);
                m.writer().writeByte(VongQuay.ID_ITEM.length);
                for (int i = 0; i < VongQuay.ID_ITEM.length; i++) {
                    m.writer().writeByte(4);
                    m.writer().writeShort(ItemTemplate4.get_it_by_id(VongQuay.ID_ITEM[i]).icon);
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 4: {
                break;
            }
            case 2:
            case 1: {
                int quant_reward = 0;
                if (action == 1) {
                    if (p.item.total_item_bag_by_id(4, 232) < 1) {
                        Service.send_box_ThongBao_OK(p, "Không đủ vé quay");
                        return;
                    }
                    p.item.remove_item47(4, 232, 1);
                    quant_reward = 3;
                } else {
                    if (p.item.total_item_bag_by_id(4, 232) < 3) {
                        Service.send_box_ThongBao_OK(p, "Không đủ vé quay");
                        return;
                    }
                    p.item.remove_item47(4, 232, 3);
                    quant_reward = 9;
                }
                ItemBag47[] list_reward = new ItemBag47[quant_reward];
                for (int i = 0; i < quant_reward; i++) {
                    list_reward[i] = VongQuay.get_random(p);
                }
                if (list_reward[0] == null && list_reward[1] == null && list_reward[2] == null) {
                    ItemBag47 it = new ItemBag47();
                    it.id = (short) Util.random(2, 6);
                    it.category = 4;
                    it.quant = (short) Util.random(1, 6);
                    list_reward[Util.random(3)] = it;
                }
                boolean add_vang = false;
                Message m = new Message(54);
                m.writer().writeByte(action);
                m.writer().writeByte(list_reward.length);
                for (int i = 0; i < list_reward.length; i++) {
                    if (list_reward[i] == null) { // lose
                        if (!add_vang && 35 > Util.random(120)) {
                            int vang_receiv = (10 > Util.random(120)) ? Util.random(100_000, 200_000)
                                    : Util.random(10_000, 20_000);
                            m.writer().writeByte(4);
                            m.writer().writeUTF("Beri");
                            m.writer().writeShort(0);
                            m.writer().writeInt(vang_receiv);
                            m.writer().writeByte(0);
                            p.update_vang(vang_receiv);
                            p.update_money();
                            add_vang = true;
                        } else {
                            m.writer().writeByte(0);
                            m.writer().writeUTF("");
                            m.writer().writeShort(-1);
                            m.writer().writeInt(0);
                            m.writer().writeByte(0);
                        }
                    } else {
                        if (list_reward[i].category == 4) {
                            ItemTemplate4 template4 = ItemTemplate4.get_it_by_id(list_reward[i].id);
                            m.writer().writeByte(4); // type
                            m.writer().writeUTF(template4.name);
                            m.writer().writeShort(template4.icon);
                            m.writer().writeInt(1); // quant
                            m.writer().writeByte(0); // color
                        } else {
                            return;
                        }
                        //
                        if (!p.item.add_item_bag47(list_reward[i].category, list_reward[i].id, 1)) {
                        }
                    }
                }
                p.addmsg(m);
                m.cleanup();
                p.item.update_Inventory(-1, false);
                break;
            }
        }
    }

    public static void performQuickSpin(Player p, int numTurns) throws IOException {
        if (numTurns <= 0 || numTurns > 1000) {
            Service.send_box_ThongBao_OK(p, "Số lượng không hợp lệ (1-1000)");
            return;
        }

        int ticketsNeeded = numTurns * 3;
        if (p.item.total_item_bag_by_id(4, 232) < ticketsNeeded) {
            Service.send_box_ThongBao_OK(p, "Không đủ " + ticketsNeeded + " vé quay");
            return;
        }

        // Ước tính chỗ trống cần thiết (mỗi lượt x3 tiêu tốn 3 vé và nhận 9 phần quà)
        // Tuy nhiên nhiều quà là Beri hoặc null, nên check tối thiểu khoảng 10%
        // numTurns * 9?
        // Thôi cứ check khoảng 20 ô trống là được, hoặc check theo tiến độ.
        if (p.item.able_bag() < 20) {
            Service.send_box_ThongBao_OK(p, "Hành trang cần ít nhất 20 ô trống để quay nhanh");
            return;
        }

        p.item.remove_item47(4, 232, ticketsNeeded);

        long totalBeri = 0;
        Map<Short, Integer> rewards = new HashMap<>();
        int totalItemsReceived = 0;

        for (int i = 0; i < numTurns * 9; i++) {
            ItemBag47 it = get_random(p);
            if (it == null) {
                // Logic rơi Beri từ process()
                if (35 > Util.random(120)) {
                    int vang_receiv = (10 > Util.random(120)) ? Util.random(100_000, 200_000)
                            : Util.random(10_000, 20_000);
                    totalBeri += vang_receiv;
                }
            } else {
                rewards.put(it.id, rewards.getOrDefault(it.id, 0) + 1);
                totalItemsReceived++;
            }
        }

        // Nếu quay quá đen (không có gì), cho ít đá cho player đỡ buồn (giống logic
        // process)
        if (totalItemsReceived == 0 && totalBeri < 100_000) {
            short stoneId = (short) Util.random(2, 6);
            rewards.put(stoneId, Util.random(1, 6));
        }

        StringBuilder sbPopup = new StringBuilder("Quay nhanh " + numTurns + " lần thành công!\n");
        StringBuilder sbChat = new StringBuilder("Chi tiết vật phẩm quay nhanh (" + numTurns + " lần):\n");

        if (totalBeri > 0) {
            p.update_vang(totalBeri);
            p.update_money();
            String beriStr = "- Beri: +" + Util.number_format(totalBeri);
            sbPopup.append(beriStr).append("\n");
            sbChat.append(beriStr).append("\n");
        }

        int itemTypeCount = 0;
        for (Map.Entry<Short, Integer> entry : rewards.entrySet()) {
            short id = entry.getKey();
            int quant = entry.getValue();
            ItemTemplate4 temp = ItemTemplate4.get_it_by_id(id);
            if (temp != null) {
                p.item.add_item_bag47(4, id, quant);
                String itemDetail = "- " + temp.name + ": x" + quant;
                sbChat.append(itemDetail).append("\n");
                itemTypeCount++;
            }
        }

        if (itemTypeCount > 0) {
            sbPopup.append("- Nhận được ").append(itemTypeCount).append(" loại vật phẩm.\n");
            sbPopup.append("(Xem chi tiết tại khung Chat)");
            Chat.send_chat(p, "Hệ thống", sbChat.toString(), false);
        }

        p.item.update_Inventory(-1, false);
        Service.send_box_ThongBao_OK(p, sbPopup.toString());
    }
}
