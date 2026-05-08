package tool;

import client.Player;
import core.Service;
import core.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.GiftBox;
import template.ItemTemplate3;
import template.Item_wear;
import template.Option;

/**
 * ItemDropTool - Manages boss rewards and drop logic.
 * Centered on flexibility for boss drops and sharing behavior.
 */
public class ItemDropTool {

    // Configurable values from htth.conf
    public static int TU_TIEN_SUCCESS_RATE = core.Manager.gI().tu_tien_success_rate;
    public static int TU_TIEN_BOI_SUCCESS_RATE = core.Manager.gI().tu_tien_boi_success_rate;
    public static int MAX_THONG_THAO_POINT = 32000;
    public static int STAT_ID_TU_HANH = 100;
    public static int STAT_ID_KHANG_TU_HANH = 101;
    public static long TU_TIEN_BASE_BERI_COST = core.Manager.gI().tu_tien_base_beri_cost;
    public static int TU_TIEN_BASE_HON_COST = core.Manager.gI().tu_tien_base_hon_cost;
    public static int TU_TIEN_POINT_GAIN = core.Manager.gI().tu_tien_point_gain;

    // Item IDs
    public static final int ITEM_HON_TU_TIEN = 1041;
    public static final int ITEM_HON_TU_MA = 1042;
    public static final int ITEM_BOI_TU_TIEN = 1037;
    public static final int ITEM_BOI_TU_MA = 1038;

    /**
     * Shows the Tu Hành (Tu Tiên/Tu Ma) menu.
     */
    public static void showTuTienMenu(Player p) throws IOException {
        int tuTien = p.tu_tien;
        int tuMa = p.tu_ma;

        int requiredHonTi = getTuTienHonCost(tuTien);
        int requiredHonMa = getTuTienHonCost(tuMa);
        long requiredBeriTi = getTuTienBeriCost(tuTien);
        long requiredBeriMa = getTuTienBeriCost(tuMa);

        String msg = "┏━━━━━ Tu Hành ━━━━━┓\n"
                + " Tu Tiên: Cấp " + tuTien + "\n"
                + "   ┗ Chi phí: " + Util.number_format(requiredBeriTi) + " Beri + " + requiredHonTi + " Hồn\n"
                + " Tu Ma: Cấp " + tuMa + "\n"
                + "   ┗ Chi phí: " + Util.number_format(requiredBeriMa) + " Beri + " + requiredHonMa + " Hồn\n"
                + "━━━━━━━━━━━━━━━━━━━\n"
                + " Tỉ lệ thành công: " + TU_TIEN_SUCCESS_RATE + "%\n"
                + " Phần thưởng: +" + p.getMasteryPotentialGain() + " tiềm năng/lượt.";

        String[] menuOptions = { "Tu Tiên", "Tu Ma", "Ghép Bội", "Đóng" };
        Service.send_box_yesno(p, 78, "Tiên Ma Lộ", msg, menuOptions, new byte[] { 65, 64, -1, 1 });
    }

    /**
     * Calculates the Beri cost for the next Tu Tiên/Ma level.
     */
    public static long getTuTienBeriCost(int currentLevel) {
        return TU_TIEN_BASE_BERI_COST * (currentLevel + 1);
    }

    /**
     * Calculates the Hồn item cost for the next Tu Tiên/Ma level.
     */
    public static int getTuTienHonCost(int currentLevel) {
        return TU_TIEN_BASE_HON_COST * (currentLevel + 1);
    }

    /**
     * Processes the upgrade for Tu Tiên or Tu Ma.
     * 
     * @param p      The player
     * @param isTuMa true if upgrading Tu Ma, false for Tu Tiên
     */
    public static void handleUpgradeTuTien(Player p, boolean isTuMa) throws IOException {
        int currentLevel = isTuMa ? p.tu_ma : p.tu_tien;
        int honId = isTuMa ? ITEM_HON_TU_MA : ITEM_HON_TU_TIEN;
        String typeName = isTuMa ? "Tu Ma" : "Tu Tiên";

        long requiredBeri = getTuTienBeriCost(currentLevel);
        int requiredHon = getTuTienHonCost(currentLevel);
        int pointGain = p.getMasteryPotentialGain();

        if (p.get_vang() < requiredBeri) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ Beri để nâng cấp!");
            return;
        }

        if (p.item.total_item_bag_by_id(4, honId) < requiredHon) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ " + (isTuMa ? "Hồn Tu Ma" : "Hồn Tu Tiên") + "!");
            return;
        }

        // Deduct resources
        p.update_vang(-requiredBeri);
        p.update_money();
        p.item.remove_item47(4, honId, requiredHon);
        p.item.update_Inventory(-1, false);

        // Success check
        if (Util.random(100) < TU_TIEN_SUCCESS_RATE) {
            if (isTuMa) {
                p.tu_ma++;
            } else {
                p.tu_tien++;
            }
            // Chỉ tăng điểm tiềm năng, KHÔNG tăng cấp thông thạo
            p.pointAttributeThongThao += pointGain;
            Service.send_box_ThongBao_OK(p, "Nâng cấp " + typeName + " thành công lên cấp " + (currentLevel + 1)
                    + "!\nBạn nhận được " + pointGain + " điểm tiềm năng thông thạo.");
        } else {
            Service.send_box_ThongBao_OK(p, "Nâng cấp " + typeName + " thất bại! Bạn đã mất nguyên liệu.");
        }
    }

    public static void updateTuBoi(Player p) throws IOException {
        int pointGain = p.getMasteryPotentialGain();
        p.pointAttributeThongThao += pointGain;
        Service.send_box_ThongBao_OK(p, "Sử dụng Bội thành công!\nBạn nhận được " + pointGain
                + " điểm tiềm năng thông thạo.");
    }

    /**
     * Configuration for boss rewards.
     */
    public static class BossRewardConfig {
        public int mobId;
        public boolean isShareArea; // true = everyone in area gets it, false = last hit only
        public List<RewardItem> rewards;

        public BossRewardConfig(int mobId, boolean isShareArea) {
            this.mobId = mobId;
            this.isShareArea = isShareArea;
            this.rewards = new ArrayList<>();
        }

        public void addReward(int itemId, int type, int minNum, int maxNum, int rate) {
            this.rewards.add(new RewardItem(itemId, type, minNum, maxNum, rate));
        }
    }

    public static class RewardItem {
        public int itemId;
        public int type; // 4 or 7
        public int minNum;
        public int maxNum;
        public int rate; // base 1000 (e.g., 500 = 50%)

        public RewardItem(int itemId, int type, int minNum, int maxNum, int rate) {
            this.itemId = itemId;
            this.type = type;
            this.minNum = minNum;
            this.maxNum = maxNum;
            this.rate = rate;
        }
    }

    private static List<BossRewardConfig> bossConfigs = new ArrayList<>();

    static {
        // Initialized with real rewards for BigMom (Mob 127)
        BossRewardConfig bigMom = new BossRewardConfig(127, false); // Default: Last hit only

        // Rewards: Beri, Ruby, Bội Tu Tiên (1037), Bội Tu Ma (1038)
        bigMom.addReward(0, 4, 100_000, 200_000, 1000); // 100% Beri
        bigMom.addReward(1, 4, 1000, 2000, 1000); // 100% Ruby
        bigMom.addReward(1037, 4, 1, 1, 250); // 25% Bội Tu Tiên
        bigMom.addReward(1038, 4, 1, 1, 250); // 25% Bội Tu Ma

        bossConfigs.add(bigMom);

        // Initialized with real rewards for BigMom (Mob 127)
        BossRewardConfig bigMom2 = new BossRewardConfig(126, false); // Default: Last hit only

        // Rewards: Beri, Ruby, Bội Tu Tiên (1037), Bội Tu Ma (1038)
        bigMom2.addReward(0, 4, 300_000, 600_000, 1000); // 100% Beri
        bigMom2.addReward(1, 4, 5000, 10000, 1000); // 100% Ruby
        bigMom2.addReward(1037, 4, 1, 1, 350); // 35% Bội Tu Tiên
        bigMom2.addReward(1038, 4, 1, 1, 350); // 35% Bội Tu Ma

        bossConfigs.add(bigMom2);

        // --- Custom Mob Drops ---

        // Sói rừng (0) & Heo mọi (1)
        int[] beginnerMobs = { 0, 1 };
        for (int id : beginnerMobs) {
            BossRewardConfig config = new BossRewardConfig(id, false);
            config.addReward(0, 7, 1, 1, 1000); // 100% Đá ngũ sắc
            config.addReward(1, 7, 1, 1, 500); // 50% Bột cường hóa
            config.addReward(1056, 4, 1, 1, 1000); // 10% Mảnh Trang Bị U
            config.addReward(1107, 4, 1, 1, 10); // Mảnh skin Yamato
            bossConfigs.add(config);
        }

        // Hải tặc khát (26) & Hải tặc đói (25)
        int[] pirateMobs = { 25, 26 };
        for (int id : pirateMobs) {
            BossRewardConfig config = new BossRewardConfig(id, false);
            config.addReward(2, 7, 1, 1, 800); // 8% Bột than
            config.addReward(3, 7, 1, 1, 600); // 6% Bột tím
            config.addReward(4, 7, 1, 1, 600); // 4% Bột vàng
            bossConfigs.add(config);
        }

        // Kiếm sĩ hải quân (38) & Xạ thủ hải quân (39)
        int[] marineMobs = { 38, 39 };
        for (int id : marineMobs) {
            BossRewardConfig config = new BossRewardConfig(id, false);
            config.addReward(5, 7, 1, 1, 500); // 5% Ngôi sao may mắn
            config.addReward(10, 7, 1, 1, 300); // 3% Khiên
            config.addReward(11, 7, 1, 1, 200); // 2% Thiên thạch may mắn
            bossConfigs.add(config);
        }

        // Đàn em Franky (156) & Blueno (157)
        int[] frankyBluenoMobs = { 156, 157 };
        for (int id : frankyBluenoMobs) {
            BossRewardConfig config = new BossRewardConfig(id, false);
            config.addReward(18, 7, 1, 1, 500); // 5% Bột siêu cấp
            config.addReward(19, 7, 1, 1, 300); // 3% Tinh thể vàng
            config.addReward(8, 7, 1, 1, 200); // 2% Tinh thể đá ác quỷ
            config.addReward(9, 7, 1, 1, 100); // 1% Đá ác quỷ
            bossConfigs.add(config);
        }

        // Rắn trời (120) - Event Mid-Autumn items
        BossRewardConfig ranTroi = new BossRewardConfig(120, false); // Last hit only
        ranTroi.addReward(0, 4, 200_000, 500_000, 1000); // 100% Beri
        ranTroi.addReward(1, 4, 20, 50, 1000); // 100% Ruby
        ranTroi.addReward(13, 7, 1, 1, 1000); // 100% Lông vũ
        ranTroi.addReward(1035, 4, 1, 1, 100); // 10% Rương Dial Vua
        ranTroi.addReward(452, 4, 1, 1, 500); // 50% Sách công thức
        ranTroi.addReward(453, 4, 1, 1, 500); // 50% Vỏ ốc
        bossConfigs.add(ranTroi);

        // Mob 112 - Gồm dãy ID: 592, 593, 594, 595, 582, 596 (Hiếm nhất)
        BossRewardConfig mob112 = new BossRewardConfig(112, false);
        mob112.addReward(592, 4, 1, 1, 600); // 50%
        mob112.addReward(593, 4, 1, 1, 500); // 30%
        mob112.addReward(594, 4, 1, 1, 400); // 20%
        mob112.addReward(595, 4, 1, 1, 300); // 10%
        mob112.addReward(582, 4, 1, 1, 200); // 5%
        mob112.addReward(596, 4, 1, 1, 100); // 1% (Hiếm nhất)
        bossConfigs.add(mob112);

        BossRewardConfig mob153 = new BossRewardConfig(153, false);
        mob153.addReward(1101, 4, 1, 1, 900); // 90%
        // mob153.addReward(1102, 4, 1, 1, 50); // 5%
        bossConfigs.add(mob153);

    }

    /**
     * Gets customized rewards for a specific boss.
     * 
     * @param mobId The ID of the boss.
     * @return List of GiftBox items or null if no custom config.
     */
    public static List<GiftBox> getCustomBossRewards(Player p, int mobId) {
        if (mobId == 153) {
            try {
                String msg = "CẢNH BÁO: Boss " + template.MobTemplate.get_it_by_id(mobId).name
                        + " đã bị tiêu diệt bởi " + p.name + " tại " + p.map.template.name + "!";
                core.Manager.gI().chatKTG(0, msg, 5);
            } catch (Exception e) {
            }
        }

        if (mobId == 181 || mobId == 173 || mobId == 183 || mobId == 168) {
            List<GiftBox> listGift = new ArrayList<>();
            if (mobId == 181) {
                // Kaido 1M Extol
                GiftBox.addGiftBox(listGift, 2, 4, 100_000);
                GiftBox.addGiftBox(listGift, 1097, 4, 1);
            } else if (mobId == 183) {
                // Mob 183: Material Drops
                // Đá sao hỏa (514): 20-30
                GiftBox.addGiftBox(listGift, 514, 4, Util.random(20, 31));
                // Đá mặt trăng (515): 5-10
                GiftBox.addGiftBox(listGift, 515, 4, Util.random(20, 31));
                // Đá khổng tước (516): 1-3
                GiftBox.addGiftBox(listGift, 516, 4, Util.random(20, 31));
            } else if (mobId == 168) {
                // Mob 168 (Map 313): Mặc định rớt cả 2 vật phẩm
                // Ngọc đế quang (517)
                GiftBox.addGiftBox(listGift, 517, 4, Util.random(1, 11));
                // Khoáng thạch vàng (513)
                GiftBox.addGiftBox(listGift, 513, 4, Util.random(1, 11));
            } else if (mobId == 173) {
                GiftBox.addGiftBox(listGift, 0, 4, Util.random(200_000_000, 300_000_000));
                GiftBox.addGiftBox(listGift, 1, 4, Util.random(100_000, 150_000));
                GiftBox.addGiftBox(listGift, 1085, 4, 1);
            }
            return listGift;
        }
        BossRewardConfig config = getConfig(mobId);
        if (config == null)
            return null;

        List<GiftBox> result = new ArrayList<>();
        for (RewardItem item : config.rewards) {
            if (Util.random(1000) < item.rate) {
                int num = Util.random(item.minNum, item.maxNum + 1);
                GiftBox.addGiftBox(result, item.itemId, item.type, num);
            }
        }
        return result;
    }

    /**
     * Check if rewards for this boss should be shared with the whole area.
     */
    public static boolean isSharedToArea(int mobId) {
        BossRewardConfig config = getConfig(mobId);
        return config != null && config.isShareArea;
    }

    private static void addRandomStatsGR(Item_wear it, int color) {
        int[] randomStats = { 5, 6, 7, 8, 9 };
        int colorBonus = color * 15;
        for (int j = 0; j < 10; j++) {
            if (j >= 5) {
                int rate = (j == 5) ? 50 : (j == 6) ? 30 : (j == 7) ? 15 : (j == 8) ? 5 : 1;
                if (Util.random(100) >= rate)
                    break;
            }
            int optId = randomStats[Util.random(randomStats.length)];
            int optVal = Util.random(200, 401) + colorBonus;
            it.option_item.add(new Option(optId, optVal));
        }
    }

    public static void addRandomStatsScaled(Item_wear it, int color, int minLines, int maxLines, int minVal,
            int maxVal) {
        int[] randomStats = { 5, 6, 7, 8, 9 };
        int numLines = Util.random(minLines, maxLines + 1);
        int colorBonus = color * 10;
        for (int j = 0; j < numLines; j++) {
            int optId = randomStats[Util.random(randomStats.length)];
            int optVal = Util.random(minVal, maxVal + 1) + colorBonus;
            it.option_item.add(new Option(optId, optVal));
        }
    }

    public static void addRandomDebuffStatsScaled(Item_wear it, int color, int minLines, int maxLines, int minVal,
            int maxVal) {
        int[] randomStats = { 91, 92, 93, 94, 95 };
        int numLines = Util.random(minLines, maxLines + 1);
        int colorBonus = color * 10;
        for (int j = 0; j < numLines; j++) {
            int optId = randomStats[Util.random(randomStats.length)];
            int optVal = Util.random(minVal, maxVal + 1) + colorBonus;
            it.option_item.add(new Option(optId, optVal));
        }
    }

    public static void handleCraftEquipGR(Player p) throws IOException {
        if (p.item.able_bag() < 1) {
            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
            return;
        }
        if (p.item.total_item_bag_by_id(4, 514) < 100 || p.item.total_item_bag_by_id(4, 515) < 100
                || p.item.total_item_bag_by_id(4, 516) < 100) {
            Service.send_box_ThongBao_OK(p,
                    "Bạn thiếu nguyên liệu (Cần 100 Đá sao hỏa, 100 Đá mặt trăng, 100 Đá khổng tước)");
            return;
        }
        if (p.get_vnd() < 1_000_000) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ 1,000,000 Extol");
            return;
        }

        p.payVnd(1_000_000);
        p.item.remove_item47(4, 514, 100);
        p.item.remove_item47(4, 515, 100);
        p.item.remove_item47(4, 516, 100);

        if (true) {
            // Success 100%
            List<Integer> validIds = new ArrayList<>();
            for (int i = 0; i < 144; i++) {
                ItemTemplate3 it = ItemTemplate3.get_it_by_id(i);
                if (it != null && it.color == 8 && it.typeEquip < 6 && (it.clazz == 0 || it.clazz == p.clazz)) {
                    validIds.add(i);
                }
            }
            if (validIds.isEmpty()) {
                Service.send_box_ThongBao_OK(p, "Lỗi hệ thống: Không tìm thấy trang bị GR!");
                return;
            }
            int id_add = validIds.get(Util.random(validIds.size()));
            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id_add);
            Item_wear it_new = new Item_wear();
            it_new.setup_template_by_id(temp);
            it_new.levelup = 0;
            it_new.typelock = 1;

            addRandomStatsGR(it_new, temp.color);

            p.item.add_item_bag3(it_new);
            p.item.update_Inventory(-1, false);
            Service.send_box_ThongBao_OK(p, "Chế tạo thành công " + temp.name);
        }
        p.item.update_Inventory(-1, false);
    }

    public static void handleCraftDialGR(Player p) throws IOException {
        if (p.item.able_bag() < 1) {
            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
            return;
        }
        if (p.item.total_item_bag_by_id(4, 513) < 100) {
            Service.send_box_ThongBao_OK(p, "Bạn thiếu 100 Khoáng thạch vàng");
            return;
        }
        if (p.get_vnd() < 5_000_000) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ 5,000,000 Extol");
            return;
        }

        p.payVnd(5_000_000);
        p.item.remove_item47(4, 513, 100);

        if (true) {
            // Success 100%
            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(12006);
            if (temp != null) {
                Item_wear it = new Item_wear();
                it.setup_template_by_id(temp);
                it.levelup = 0;
                it.typelock = 1;

                addRandomStatsGR(it, temp.color);

                p.item.add_item_bag3(it);
                Service.send_box_ThongBao_OK(p, "Ghép thành công " + temp.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Lỗi: Không tìm thấy vật phẩm Dial GR trong dữ liệu!");
            }
        }
        p.item.update_Inventory(-1, false);
    }

    public static void handleCraftTimGR(Player p) throws IOException {
        if (p.item.able_bag() < 1) {
            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống");
            return;
        }
        if (p.item.total_item_bag_by_id(4, 517) < 100) {
            Service.send_box_ThongBao_OK(p, "Bạn thiếu 100 Ngọc đế quang");
            return;
        }
        if (p.get_vnd() < 5_000_000) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ 5,000,000 Extol");
            return;
        }

        p.payVnd(5_000_000);
        p.item.remove_item47(4, 517, 100);

        if (true) {
            // Success 100%
            ItemTemplate3 temp = ItemTemplate3.get_it_by_id(13006);
            if (temp != null) {
                Item_wear it = new Item_wear();
                it.setup_template_by_id(temp);
                it.levelup = 0;
                it.typelock = 1;

                addRandomStatsGR(it, temp.color);

                p.item.add_item_bag3(it);
                Service.send_box_ThongBao_OK(p, "Ghép thành công " + temp.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Lỗi: Không tìm thấy vật phẩm Tim GR trong dữ liệu!");
            }
        }
        p.item.update_Inventory(-1, false);
    }

    private static BossRewardConfig getConfig(int mobId) {
        for (BossRewardConfig config : bossConfigs) {
            if (config.mobId == mobId)
                return config;
        }
        return null;
    }

    public static void main(String[] args) {
        core.GameLogger.info("=== ItemDropTool: Current Configuration ===");
        core.GameLogger.info("Tu Tiên Success Rate:      " + TU_TIEN_SUCCESS_RATE + "%");
        core.GameLogger.info("Bội Success Rate:         " + TU_TIEN_BOI_SUCCESS_RATE + "%");
        core.GameLogger.info("Max Thống Thạo Point:      " + MAX_THONG_THAO_POINT);
        core.GameLogger.info("Tu Tiên Base Cost (Beri): " + Util.number_format(TU_TIEN_BASE_BERI_COST));
        core.GameLogger.info("Tu Tiên Base Cost (Hồn):  " + TU_TIEN_BASE_HON_COST);
        core.GameLogger.info("-------------------------------------------");
        core.GameLogger.info("Total Boss Configs:        " + bossConfigs.size());
        for (BossRewardConfig config : bossConfigs) {
            System.out.println("Boss ID " + config.mobId + ": " + config.rewards.size() + " reward items (Shared: "
                    + config.isShareArea + ")");
        }
        core.GameLogger.info("===========================================");
    }
}
