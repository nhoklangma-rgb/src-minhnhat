package template;

import client.Player;
import core.Util;
import java.util.ArrayList;
import java.util.List;

public class GiftBox {
    public byte type;
    public String name;
    public short icon;
    public long num;
    public byte color;
    public short id;

    public static List<GiftBox> get_gift_boss_pica(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(20_000_000, 50_000_000));
        addGiftBox(listGift, 1, 4, Util.random(10000, 30000));
        addGiftBox(listGift, 2, 4, Util.random(300, 500)); // Extol

        if (80 > Util.random(120)) { // manh trang bi
            addGiftBox(listGift, 1057, 4, Util.random(20, 50));
        } else {
            addGiftBox(listGift, 1058, 4, Util.random(10, 20));
        }
        if (20 > Util.random(120)) { // exp haki quan sat
            addGiftBox(listGift, 1022, 4, 1);
        }
        if (15 > Util.random(120)) { // exp haki vu trang
            addGiftBox(listGift, 1024, 4, 1);
        }
        if (10 > Util.random(120)) { // exp haki ba vuong
            addGiftBox(listGift, 1023, 4, 1);
        }
        if (50 > Util.random(120)) { // khien xanh
            addGiftBox(listGift, 10, 7, Util.random(1, 5));
        }
        if (50 > Util.random(120)) { // bua duc
            addGiftBox(listGift, 457, 4, Util.random(1, 5));
        }

        // SSR Equipment Reward Logic
        int plus = 0;
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

        if (!validIds.isEmpty()) {
            int id_add = validIds.get(Util.random(validIds.size()));
            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
            if (temp != null) {
                // Tạo item thực tế
                Item_wear it_ssr = new Item_wear();
                it_ssr.setup_template_by_id(temp);
                it_ssr.levelup = (byte) plus;

                // Thêm 5 dòng chỉ số ngẫu nhiên
                int[] randomStats = { 5, 6, 7, 8, 9 };
                int colorBonus = temp.color * 15; // color 6 → +60
                for (int j = 0; j < 5; j++) {
                    int optId = randomStats[Util.random(randomStats.length)];
                    int optVal = Util.random(100, 201) + colorBonus + (plus / 2);
                    it_ssr.option_item.add(new Option(optId, optVal));
                }

                if (p.item.add_item_bag3(it_ssr)) {
                    // Hiển thị trong GiftBox UI
                    GiftBox gb = new GiftBox();
                    gb.id = -1; // -1 to prevent adding again via send_gift
                    gb.type = 3;
                    gb.name = temp.name + " +" + plus;
                    gb.icon = temp.icon;
                    gb.num = 1;
                    gb.color = temp.color;
                    listGift.add(gb);
                }
            }
        }

        return listGift;
    }

    public static List<GiftBox> get_gift_boss_pica_other(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 1057, 4, Util.random(5, 10));
        addGiftBox(listGift, 0, 4, Util.random(2_000_000, 5_000_000));
        addGiftBox(listGift, 1, 4, Util.random(500, 1000));

        return listGift;
    }

    public static List<GiftBox> get_gift_boss_clan(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(20_000_000, 30_000_000));
        addGiftBox(listGift, 1, 4, Util.random(1000, 2000));

        addGiftBox(listGift, 579, 4, Util.random(10, 20));
        if (50 > Util.random(120)) { // khien xanh
            addGiftBox(listGift, 10, 7, 1);
        }
        if (50 > Util.random(120)) { // bua duc
            addGiftBox(listGift, 457, 4, 1);
        }
        addGiftBoxClan(listGift, -10, Util.random(100, 200));
        addGiftBoxClan(listGift, -11, Util.random(20000, 50000));
        addGiftBoxClan(listGift, -12, Util.random(3000, 5000));
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_clan_other(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(2_000_000, 5_000_000));
        addGiftBox(listGift, 1, 4, Util.random(500, 1000));

        addGiftBox(listGift, 579, 4, Util.random(1, 5));
        addGiftBoxClan(listGift, -10, Util.random(100, 200));
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_xa_nu(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(20_000_000, 30_000_000));
        addGiftBox(listGift, 2, 4, Util.random(100, 300)); // Extol

        // addGiftBox(listGift, new int[] { 1041, 1042 }[Util.random(2)], 4,
        // Util.random(50, 100));
        addGiftBox(listGift, new int[] { 1037, 1038 }[Util.random(2)], 4, Util.random(1, 4));
        if (50 > Util.random(120)) { // khien xanh
            addGiftBox(listGift, 10, 7, 1);
        }
        if (50 > Util.random(120)) { // bua duc
            addGiftBox(listGift, 457, 4, 1);
        }
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_xa_nu_other(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(2_000_000, 5_000_000));

        addGiftBox(listGift, new int[] { 1041, 1042 }[Util.random(2)], 4, Util.random(10, 20));
        return listGift;
    }

    public static List<GiftBox> get_gift_farm_da(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(200_000, 500_000));
        addGiftBox(listGift, 1, 4, Util.random(20, 50));
        addGiftBox(listGift, 2, 4, Util.random(500, 600)); // Extol

        addGiftBox(listGift, new int[] { 1041, 1042 }[Util.random(2)], 4, 1);

        if (Util.random(100) < 5) {
            addGiftBox(listGift, 196, 4, 1);
        }
        return listGift;
    }

    public static List<GiftBox> get_gift_farm_da2(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(400_000, 1_000_000));
        addGiftBox(listGift, 1, 4, Util.random(200, 500));
        addGiftBox(listGift, 2, 4, Util.random(800, 1000)); // Extol

        if (Util.random(100) < 10) {

            // Bước 2: Trúng 10% rồi, giờ chọn ngẫu nhiên 1 trong 2 món (1037 hoặc 1038)
            // Util.random(2) sẽ trả về ngẫu nhiên số 0 hoặc 1 (tương ứng vị trí trong mảng)
            int randomId = new int[] { 1037, 1038 }[Util.random(2)];

            // Bước 3: Thêm món đồ vừa chọn vào hộp quà
            // Số lượng đang được random từ 1 đến 3 (nếu hàm của bạn là lấy số cuối - 1)
            addGiftBox(listGift, randomId, 4, Util.random(1, 4));
        }

        if (Util.random(100) < 5) {
            addGiftBox(listGift, 196, 4, 1);
        }
        return listGift;
    }

    public static List<GiftBox> get_gift_farm_ran(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(200_000, 500_000));
        addGiftBox(listGift, 1, 4, Util.random(20, 50));
        addGiftBox(listGift, 13, 7, 1);
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_banh_kem(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(100_000, 200_000));
        addGiftBox(listGift, 1, 4, Util.random(200, 500));
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_banh_kem_other(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(200_000, 300_000));
        addGiftBox(listGift, 1, 4, Util.random(100, 200));
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_sieu_trum(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 0, 4, Util.random(10_000_000, 20_000_000));
        addGiftBox(listGift, 1, 4, Util.random(1000, 2000));
        addGiftBox(listGift, 2, 4, Util.random(5200, 8200)); // Extol

        addGiftBox(listGift, new int[] { 1055, 1055 }[Util.random(2)], 4, Util.random(1, 3));

        if (80 > Util.random(120)) { // manh trang bi
            addGiftBox(listGift, 1056, 4, Util.random(20, 50));
        } else {
            addGiftBox(listGift, 1057, 4, Util.random(10, 20));
        }
        if (50 > Util.random(120)) { // khien xanh
            addGiftBox(listGift, 10, 7, 1);
        }
        if (50 > Util.random(120)) { // bua duc
            addGiftBox(listGift, 457, 4, 1);
        }
        return listGift;
    }

    public static List<GiftBox> get_gift_boss_sieu_trum_other(Player p) {
        List<GiftBox> listGift = new ArrayList<>();
        addGiftBox(listGift, 1056, 4, Util.random(1, 20));
        addGiftBox(listGift, 0, 4, Util.random(2_000_000, 5_000_000));
        addGiftBox(listGift, 1, 4, Util.random(500, 1000));

        return listGift;
    }

    public static void addGiftBox(List<GiftBox> listGift, int itemId, int type, long num) {
        GiftBox gb_ = new GiftBox();
        gb_.type = (byte) type;
        gb_.num = num;
        gb_.color = 0;

        if (type == 4) {
            if (itemId == 2) { // Extol reward
                gb_.id = 2;
                gb_.name = "Extol";
                gb_.icon = 170; // Map with Extol symbol
                gb_.color = 1;
                listGift.add(gb_);
                return;
            }
            ItemTemplate4 it_temp4 = ItemTemplate4.get_it_by_id(itemId);
            if (it_temp4 != null) {
                gb_.id = it_temp4.id;
                gb_.name = it_temp4.name;
                gb_.icon = it_temp4.icon;
                listGift.add(gb_);
            }
        } else if (type == 7) {
            ItemTemplate7 it_temp7 = ItemTemplate7.get_it_by_id(itemId);
            if (it_temp7 != null) {
                gb_.id = it_temp7.id;
                gb_.name = it_temp7.name;
                gb_.icon = it_temp7.icon;
                listGift.add(gb_);
            }
        }
    }

    public static void addGiftBoxClan(List<GiftBox> listGift, int idSpecial, long num) {
        GiftBox gb_ = new GiftBox();
        gb_.type = 4;
        gb_.num = num;
        switch (idSpecial) {
            case -10:
                gb_.id = -10;
                gb_.name = "XP Clan";
                gb_.icon = 91;
                gb_.color = 1;
                break;
            case -11:
                gb_.id = -11;
                gb_.name = "Bery Clan";
                gb_.icon = 93;
                gb_.color = 5;
                break;
            case -12:
                gb_.id = -12;
                gb_.name = "Ruby Clan";
                gb_.icon = 92;
                gb_.color = 6;
                break;
            default:
                return;
        }

        listGift.add(gb_);
    }

}
