package activities;

import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.*;

public class Upgrade_Skin {
    public static List<ItemFashionP2> permanentFashion = new ArrayList<>();

    public static void show_table(Player p) throws IOException {
        permanentFashion.clear();
        for (int i = 0; i < p.fashion.size(); i++) {
            ItemFashionP2 currentFashion = p.fashion.get(i);
            if (currentFashion.expirationTime == -1) {
                permanentFashion.add(currentFashion);
            }
        }
        p.upgrade_skin = new Upgrade_Skin_Info();
        p.upgrade_skin.upgrade_skin_data = new short[] { -1, -1, -1, -1, -1, -1 };
        //
        Message m = new Message(81);
        m.writer().writeByte(0);
        m.writer().writeByte(105);
        m.writer().writeByte(permanentFashion.size()); // size skin
        for (ItemFashionP2 fashion : permanentFashion) {
            ItemFashion temp = ItemFashion.get_item(fashion.id);
            if (temp != null) {
                m.writer().writeShort(temp.ID);
                m.writer().writeUTF("");
                m.writer().writeUTF("");
                m.writer().writeShort(temp.idIcon);
                m.writer().writeByte(fashion.level);
            }
        }
        //
        List<ItemBag47> list_da_kham = new ArrayList<>();
        for (int i = 0; i < p.item.bag47.size(); i++) {
            ItemBag47 it47 = p.item.bag47.get(i);
            if (it47.category == 4) {
                ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(it47.id);
                if (itemTemplate4.type == 12
                        && (itemTemplate4.id == 49 || itemTemplate4.id == 55 || itemTemplate4.id == 61
                                || itemTemplate4.id == 67 || itemTemplate4.id == 73 || itemTemplate4.id == 79)) {
                    list_da_kham.add(it47);
                }
            }
        }
        m.writer().writeByte(list_da_kham.size()); // size da kham
        for (int i = 0; i < list_da_kham.size(); i++) {
            if (list_da_kham.get(i).category == 4) {
                ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(list_da_kham.get(i).id);
                m.writer().writeByte(4);
                m.writer().writeShort(itemTemplate4.id);
                m.writer().writeShort(list_da_kham.get(i).quant);
                m.writer().writeUTF(itemTemplate4.name);
                m.writer().writeShort(itemTemplate4.icon);
            }
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void process(Player p, Message m2) throws IOException {
        permanentFashion.clear();
        if (m2.reader().available() == 6) {
            byte type = m2.reader().readByte();
            byte cat = m2.reader().readByte();
            short id = m2.reader().readShort();
            byte pos = m2.reader().readByte();
            byte bovao = m2.reader().readByte();
            // core.GameLogger.info(type + " " + cat + " " + id + " " + pos + " " + bovao);
            if (type == 1 && cat == 105 && pos == 0 && bovao == 1) {
                ItemFashionP2 myFashion = p.check_fashion(id);
                //
                for (int i = 0; i < p.fashion.size(); i++) {
                    ItemFashionP2 currentFashion = p.fashion.get(i);
                    if (currentFashion.expirationTime == -1) {
                        permanentFashion.add(currentFashion);
                    }
                }
                //
                if (myFashion != null && p.upgrade_skin != null) { // update lai sau khi upgrade
                    Message m3 = new Message(81);
                    m3.writer().writeByte(5);
                    m3.writer().writeByte(105);
                    m3.writer().writeByte(permanentFashion.size()); // size skin
                    for (ItemFashionP2 fashion : permanentFashion) {
                        ItemFashion temp = ItemFashion.get_item(fashion.id);
                        if (temp != null) {
                            m3.writer().writeShort(temp.ID);
                            m3.writer().writeUTF("");
                            m3.writer().writeUTF("");
                            m3.writer().writeShort(temp.idIcon);
                            m3.writer().writeByte(fashion.level);
                        }
                    }
                    //
                    List<ItemBag47> list_da_kham = new ArrayList<>();
                    for (int i = 0; i < p.item.bag47.size(); i++) {
                        ItemBag47 it47 = p.item.bag47.get(i);
                        if (it47.category == 4) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(it47.id);
                            if (itemTemplate4.type == 12
                                    && (itemTemplate4.id == 49 || itemTemplate4.id == 55 || itemTemplate4.id == 61
                                            || itemTemplate4.id == 67 || itemTemplate4.id == 73
                                            || itemTemplate4.id == 79)) {
                                list_da_kham.add(it47);
                            }
                        }
                    }
                    m3.writer().writeByte(list_da_kham.size()); // size da kham
                    for (int i = 0; i < list_da_kham.size(); i++) {
                        if (list_da_kham.get(i).category == 4) {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(list_da_kham.get(i).id);
                            m3.writer().writeByte(4);
                            m3.writer().writeShort(itemTemplate4.id);
                            m3.writer().writeShort(list_da_kham.get(i).quant);
                            m3.writer().writeUTF(itemTemplate4.name);
                            m3.writer().writeShort(itemTemplate4.icon);
                        }
                    }
                    p.addmsg(m3);
                    m3.cleanup();
                }
                if (myFashion != null && p.upgrade_skin != null) { // bo vao hoac update
                    Message m = new Message(81);
                    m.writer().writeByte(1);
                    m.writer().writeByte(105);
                    m.writer().writeShort(id);
                    m.writer().writeByte(0);
                    m.writer().writeByte(0);
                    m.writer().writeByte(1);
                    //
                    m.writer().writeInt(get_beri_up(myFashion.level));
                    m.writer().writeShort(get_ruby_up(myFashion.level));
                    m.writer().writeInt(get_extol_up(myFashion.level));
                    p.addmsg(m);
                    m.cleanup();
                    p.upgrade_skin.skin = myFashion;
                }
            } else if (type == 1 && cat == 4) {
                if (p.upgrade_skin != null && p.upgrade_skin.skin != null) {
                    int targetPos = -1;
                    if (bovao == 1) { // lắp gem
                        for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
                            if (p.upgrade_skin.upgrade_skin_data[i] == -1) {
                                targetPos = i;
                                break;
                            }
                        }
                        if (targetPos == -1)
                            return;
                        int gemsUsed = 0;
                        for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
                            if (p.upgrade_skin.upgrade_skin_data[i] != -1)
                                gemsUsed++;
                        }
                        int totalPercent = get_percent(p.upgrade_skin.skin.level);
                        int percentPerGem = Math.max(1, (totalPercent + 4) / 5); // chia ceil, tối thiểu 1% mỗi gem
                        Message m = new Message(81);
                        m.writer().writeByte(1);
                        m.writer().writeByte(4);
                        m.writer().writeShort(id);
                        m.writer().writeByte(percentPerGem);
                        m.writer().writeByte(targetPos);
                        m.writer().writeByte(1);
                        m.writer().writeInt(get_beri_up(p.upgrade_skin.skin.level));
                        m.writer().writeShort(get_ruby_up(p.upgrade_skin.skin.level));
                        m.writer().writeInt(get_extol_up(p.upgrade_skin.skin.level));
                        p.addmsg(m);
                        m.cleanup();
                        p.upgrade_skin.upgrade_skin_data[targetPos] = id;
                    } else if (bovao == 0) { // tháo gem
                        if (pos >= 1 && pos < p.upgrade_skin.upgrade_skin_data.length) {
                            short gemId = p.upgrade_skin.upgrade_skin_data[pos];
                            if (gemId != -1) {
                                p.upgrade_skin.upgrade_skin_data[pos] = -1;
                                int totalPercent2 = get_percent(p.upgrade_skin.skin.level);
                                int percentPerGem = Math.max(1, (totalPercent2 + 4) / 5);
                                Message m = new Message(81);
                                m.writer().writeByte(1);
                                m.writer().writeByte(4);
                                m.writer().writeShort(gemId);
                                m.writer().writeByte(0);
                                m.writer().writeByte(pos);
                                m.writer().writeByte(0);
                                m.writer().writeInt(get_beri_up(p.upgrade_skin.skin.level));
                                m.writer().writeShort(get_ruby_up(p.upgrade_skin.skin.level));
                                m.writer().writeInt(get_extol_up(p.upgrade_skin.skin.level));
                                p.addmsg(m);
                                m.cleanup();
                            }
                        }
                    }
                }
            }
        } else if (m2.reader().available() == 3) {
            byte type = m2.reader().readByte();
            short id = m2.reader().readShort();
            // core.GameLogger.info(type + " " + id);
            if (type == 4) {
                ItemFashionP2 myFashion = p.check_fashion(id);
                if (myFashion.expirationTime != -1) {
                    Service.send_box_ThongBao_OK(p, "Chỉ thời trang vĩnh viễn mới có thể nâng cấp");
                    return;
                }
                if (myFashion != null && p.upgrade_skin != null && p.upgrade_skin.skin != null
                        && p.upgrade_skin.skin.id == myFashion.id) {
                    boolean check_da_kham = false;
                    for (int i = 0; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
                        if (p.upgrade_skin.upgrade_skin_data[i] != -1) {
                            check_da_kham = true;
                            break;
                        }
                    }
                    if (p.upgrade_skin != null && p.upgrade_skin.skin != null
                            && p.upgrade_skin.skin.level >= 42) {
                        Service.send_box_ThongBao_OK(p, "Đã đạt nâng cấp tối đa");
                        return;
                    }
                    int beri_req = get_beri_up(p.upgrade_skin.skin.level);
                    int ruby_req = get_ruby_up(p.upgrade_skin.skin.level);
                    int extol_req = get_extol_up(p.upgrade_skin.skin.level);
                    if (p.get_vang() < beri_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(beri_req) + " Beri");
                        return;
                    }
                    if (p.get_ngoc() < ruby_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(ruby_req) + " Ruby");
                        return;
                    }
                    if (p.get_vnd() < extol_req) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(extol_req) + " Extol");
                        return;
                    }
                    if (check_da_kham) {
                        ItemFashion itF = ItemFashion.get_item(myFashion.id);
                        Service.send_box_yesno(p, 70, "Thông báo",
                                ("Bạn có muốn nâng cấp\n" + itF.name + " lên +" + (p.upgrade_skin.skin.level + 1)),
                                new String[] { Util.number_format(extol_req), "Auto", "Đóng" },
                                new byte[] { 69, 3, -1 });
                    } else {
                        Service.send_box_ThongBao_OK(p, "Hãy bỏ đá khảm trước");
                    }
                }
            }
        }
    }

    public static void normalUpgrade(Player p) throws IOException {
        if (p.upgrade_skin == null || p.upgrade_skin.skin == null)
            return;
        int beri_req = get_beri_up(p.upgrade_skin.skin.level);
        int ruby_req = get_ruby_up(p.upgrade_skin.skin.level);
        int extol_req = get_extol_up(p.upgrade_skin.skin.level);
        boolean hasAllGems = true;
        int percent = 0;
        for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
            int gemId = p.upgrade_skin.upgrade_skin_data[i];
            if (gemId != -1) {
                if (p.item.total_item_bag_by_id(4, gemId) < 1) {
                    hasAllGems = false;
                    break;
                }
            }
        }
        if (!hasAllGems) {
            Service.send_box_ThongBao_OK(p, "Không đủ đá nâng cấp");
            return;
        }
        for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
            int gemId = p.upgrade_skin.upgrade_skin_data[i];
            if (gemId != -1) {
                int totalPct = get_percent(p.upgrade_skin.skin.level);
                int percentPerGem = Math.max(1, (totalPct + 4) / 5);
                percent += percentPerGem;
                p.item.remove_item47(4, gemId, 1);
            }
        }
        if (percent < 0)
            percent = 0;
        p.update_vang(-beri_req);
        p.update_ngoc(-ruby_req);
        p.update_vnd(-extol_req);
        p.update_money();
        boolean success = percent > Util.random(120);
        if (success) {
            p.upgrade_skin.skin.level++;
        }
        Message m = new Message(81);
        m.writer().writeByte(3);
        m.writer().writeByte(success ? 0 : 1);
        m.writer().writeShort(p.upgrade_skin.skin.id);
        m.writer().writeByte(105);
        ItemFashion itF = ItemFashion.get_item(p.upgrade_skin.skin.id);
        m.writer().writeUTF("Nâng cấp thời trang " + (success ? "thành công" : "thất bại"));
        p.addmsg(m);
        m.cleanup();
        p.upgrade_skin.upgrade_skin_data = new short[] { -1, -1, -1, -1, -1, -1 };
        p.item.update_Inventory(-1, false);
        p.update_info_to_all();
    }

    public static void autoUpgrade(Player p) throws IOException {
        if (p.upgrade_skin == null || p.upgrade_skin.skin == null)
            return;
        int upgradeCount = 0;
        int totalBeri = 0;
        int totalRuby = 0;
        int totalExtol = 0;
        boolean isSuccess = false;
        int beri_req = get_beri_up(p.upgrade_skin.skin.level);
        int ruby_req = get_ruby_up(p.upgrade_skin.skin.level);
        int extol_req = get_extol_up(p.upgrade_skin.skin.level);
        while (true) {
            if (upgradeCount >= p.solannang) {
                break;
            }
            boolean hasRequiredItems = true;
            StringBuilder requiredItemsInfo = new StringBuilder();
            if (p.get_vang() < beri_req || p.get_ngoc() < ruby_req || p.get_vnd() < extol_req) {
                break;
            }
            boolean hasAllGems = true;
            if (p.upgrade_skin != null && p.upgrade_skin.skin != null) {
                for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
                    int gemId = p.upgrade_skin.upgrade_skin_data[i];
                    if (gemId != -1) {
                        if (p.item.total_item_bag_by_id(4, gemId) < 1) {
                            hasAllGems = false;
                            break;
                        }
                    }
                }
            }
            if (!hasAllGems) {
                break;
            }
            int percent = 0;
            if (p.upgrade_skin != null && p.upgrade_skin.skin != null) {
                for (int i = 1; i < p.upgrade_skin.upgrade_skin_data.length; i++) {
                    int gemId = p.upgrade_skin.upgrade_skin_data[i];
                    if (gemId != -1) {
                        int totalPct2 = get_percent(p.upgrade_skin.skin.level);
                        int percentPerGem = Math.max(1, (totalPct2 + 4) / 5);
                        percent += percentPerGem;
                        p.item.remove_item47(4, gemId, 1);
                    }
                }
            }
            if (percent < 0)
                percent = 0;
            p.update_vang(-beri_req);
            p.update_ngoc(-ruby_req);
            p.update_vnd(-extol_req);
            totalBeri += beri_req;
            totalRuby += ruby_req;
            totalExtol += extol_req;
            upgradeCount++;
            isSuccess = percent > Util.random(120);
            if (isSuccess) {
                p.upgrade_skin.skin.level++;
                break;
            }
        }
        p.update_money(); // MOVED HERE
        Message m = new Message(81);
        m.writer().writeByte(3);
        m.writer().writeByte(isSuccess ? 0 : 1);
        m.writer().writeShort(p.upgrade_skin.skin.id);
        m.writer().writeByte(105);
        ItemFashion itF = ItemFashion.get_item(p.upgrade_skin.skin.id);
        String upgradeResult = "== Auto Nâng Thời Trang ==\n" +
                itF.name + " +" + p.upgrade_skin.skin.level + "\n" +
                "- Kết quả: " + (isSuccess ? "Thành công" : "Thất bại") + "\n" +
                "- Số lần nâng : " + upgradeCount + "\n" +
                "------------------------\n" +
                "- Beri: " + String.format("%,d", totalBeri) + "\n" +
                "- Ruby: " + String.format("%,d", totalRuby) + "\n" +
                "- Extol: " + String.format("%,d", totalExtol) + "\n";
        m.writer().writeUTF(upgradeResult);
        p.addmsg(m);
        m.cleanup();
        p.upgrade_skin.upgrade_skin_data = new short[] { -1, -1, -1, -1, -1, -1 };
        p.item.update_Inventory(-1, false);
        p.update_info_to_all();
    }

    private static int get_percent(byte level) {
        int nextLevel = level + 1;
        // Exponential decay: giảm nhanh lúc đầu, chậm dần về sau, không có điểm nhảy
        // Level +1 ≈ 80%, Level +10 ≈ 42%, Level +20 ≈ 20%, Level +42 → tối thiểu 5%
        int percent = (int) (80 * Math.pow(0.93, nextLevel - 1));
        return Math.max(5, percent);
    }

    private static int get_beri_up(int level) {
        long beri = (long) (level + 1) * 100_000_000L;
        if (beri > 2_000_000_000L)
            beri = 2_000_000_000L;
        return (int) beri;
    }

    private static int get_ruby_up(int level) {
        return 30_000;
    }

    private static int get_extol_up(int level) {
        return (level + 1) * 10_000; // 10k mỗi cấp
    }

}
