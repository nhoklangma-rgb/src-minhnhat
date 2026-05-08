package activities;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import client.Player;
import core.Service;
import core.Util;
import io.Session;
import io.SessionManager;
import map.Map;
import map.Mob;
import template.MobTemplate;

public class WishManager {

    private static WishManager instance;
    private final AtomicLong globalCooldown = new AtomicLong(0);
    private static final String DATA_DIR = "data/wish/";

    private static final int[][] MOB_WHITELIST = {
            { 2, 0 }, { 2, 1 }, // Rừng làng
            { 3, 2 }, // Đường ra biển
            { 4, 2 }, { 4, 3 }, // Bãi san hô
            { 5, 4 }, // Khu vực nghỉ mát
            { 7, 45 }, // Bến tàu Fosha
            { 8, 46 }, // Biển đông 1
            { 10, 5 }, { 10, 6 }, // Đảo vỏ sò
            { 11, 6 }, { 11, 7 }, { 11, 8 }, // Bãi đất trống
            { 12, 6 }, { 12, 7 }, { 12, 9 }, // Bãi đất trống 2
            { 13, 10 }, // Bãi đất trống 3
            { 15, 47 }, // Biển đông 2
            { 16, 48 }, // Đảo Orange
            { 34, 26 }, { 34, 25 }, // Sau nhà hàng
            { 192, 156 }, // Nhà Franky
            { 194, 165 }, { 194, 166 }, { 194, 157 }, // Enies Lobby
            { 302, 179 }, // Lãnh địa bang
            { 310, 184 }, // Bãi Farm Đá
            { 307, 127 }, // Đảo bánh kem
            { 308, 175 }, // Map Siêu Trùm
            { 300, 182 }, // Map Thế Giới
            { 309, 173 } // Cao nguyên
    };

    public static WishManager gI() {
        if (instance == null) {
            instance = new WishManager();
        }
        return instance;
    }

    public WishManager() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadEventData();
    }

    // public boolean isOnCooldown() {
    // return false;
    // }

    public long getRemainingCooldown() {
        return Math.max(0, globalCooldown.get() - System.currentTimeMillis());
    }

    public void setGlobalCooldown() {
        globalCooldown.set(System.currentTimeMillis() + 3 * 60 * 1000); // 3 minutes
        saveEventData();
    }

    private void saveEventData() {
        JSONObject json = new JSONObject();
        json.put("nextOpenTime", globalCooldown.get());
        try (FileWriter writer = new FileWriter(DATA_DIR + "event.json")) {
            writer.write(json.toJSONString());
        } catch (IOException e) {
            core.GameLogger.error("WishManager.saveEventData: Error saving event data", e);
        }
    }

    private void loadEventData() {
        File file = new File(DATA_DIR + "event.json");
        if (!file.exists())
            return;
        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            globalCooldown.set((long) json.getOrDefault("nextOpenTime", 0L));
        } catch (Exception e) {
            core.GameLogger.error("WishManager.loadEventData: Error loading event data", e);
        }
    }

    // --- JSON Persistence ---

    @SuppressWarnings("unchecked")
    public void loadWishData(Player p) {
        File file = new File(DATA_DIR + p.id + ".json");
        // Fallback for old name-based files
        if (!file.exists()) {
            File oldFile = new File(DATA_DIR + p.name + ".json");
            if (oldFile.exists()) {
                oldFile.renameTo(file);
            }
        }

        if (!file.exists()) {
            p.wishStats = new WishStats();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            p.wishStats = new WishStats();
            p.wishStats.extol = (long) json.getOrDefault("extol", 0L);
            p.wishStats.dodge = (long) json.getOrDefault("dodge", 0L);
            p.wishStats.reduction = (long) json.getOrDefault("reduction", 0L);
            p.wishStats.react = (long) json.getOrDefault("react", 0L);
            p.wishStats.pierce = (long) json.getOrDefault("pierce", 0L);
            p.wishStats.decDodge = (long) json.getOrDefault("decDodge", 0L);
            p.wishStats.decReact = (long) json.getOrDefault("decReact", 0L);
            p.wishStats.decReduc = (long) json.getOrDefault("decReduc", 0L);
            p.wishStats.decPierce = (long) json.getOrDefault("decPierce", 0L);
            p.wishStats.finalDame = (long) json.getOrDefault("finalDame", 0L);
            p.wishStats.finalHp = (long) json.getOrDefault("finalHp", 0L);
            p.wishStats.critDame = (long) json.getOrDefault("critDame", 0L);

            // Defensive check for legacy scaled values in JSON
            // if (p.wishStats.dodge > 500)
            // p.wishStats.dodge /= 10;
            // if (p.wishStats.reduction > 500)
            // p.wishStats.reduction /= 10;
            // if (p.wishStats.react > 500)
            // p.wishStats.react /= 10;
            // if (p.wishStats.pierce > 500)
            // p.wishStats.pierce /= 10;
            // if (p.wishStats.decDodge > 500)
            // p.wishStats.decDodge /= 10;
            // if (p.wishStats.decReact > 500)
            // p.wishStats.decReact /= 10;
            // if (p.wishStats.decReduc > 500)
            // p.wishStats.decReduc /= 10;
            // if (p.wishStats.decPierce > 500)
            // p.wishStats.decPierce /= 10;
            // if (p.wishStats.finalDame > 500)
            // p.wishStats.finalDame /= 10;
            // if (p.wishStats.finalHp > 500)
            // p.wishStats.finalHp /= 10;
            // if (p.wishStats.critDame > 500)
            // p.wishStats.critDame /= 10;

            if (json.containsKey("huntTarget")) {
                JSONObject targetJson = (JSONObject) json.get("huntTarget");
                p.huntTarget = new HuntTarget(
                        (String) targetJson.get("name"),
                        (String) targetJson.get("mapName"),
                        ((Long) targetJson.get("zone")).intValue(),
                        ((Long) targetJson.get("itemId")).intValue(),
                        (boolean) targetJson.get("isMob"),
                        ((Long) targetJson.get("targetTemplateId")).intValue(),
                        ((Long) targetJson.get("mapId")).intValue());

                // Check if player target is still online
                if (!p.huntTarget.isMob) {
                    boolean online = false;
                    synchronized (SessionManager.CLIENT_ENTRYS) {
                        for (Session ss : SessionManager.CLIENT_ENTRYS) {
                            if (ss != null && ss.p != null && ss.p.id == p.huntTarget.targetTemplateId) {
                                online = true;
                                break;
                            }
                        }
                    }
                    if (!online) {
                        p.huntTarget = null;
                        // Silently clear so hunter can re-scan
                        saveWishData(p);
                    }
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("WishManager.loadWishData: Error loading wish data", e);
            p.wishStats = new WishStats();
        }
    }

    @SuppressWarnings("unchecked")
    public void saveWishData(Player p) {
        if (p.wishStats == null)
            return;
        JSONObject json = new JSONObject();
        json.put("extol", p.wishStats.extol);
        json.put("dodge", p.wishStats.dodge);
        json.put("reduction", p.wishStats.reduction);
        json.put("react", p.wishStats.react);
        json.put("pierce", p.wishStats.pierce);
        json.put("decDodge", p.wishStats.decDodge);
        json.put("decReact", p.wishStats.decReact);
        json.put("decReduc", p.wishStats.decReduc);
        json.put("decPierce", p.wishStats.decPierce);
        json.put("finalDame", p.wishStats.finalDame);
        json.put("finalHp", p.wishStats.finalHp);
        json.put("critDame", p.wishStats.critDame);

        if (p.huntTarget != null) {
            JSONObject targetJson = new JSONObject();
            targetJson.put("name", p.huntTarget.name);
            targetJson.put("mapName", p.huntTarget.mapName);
            targetJson.put("zone", p.huntTarget.zone);
            targetJson.put("itemId", p.huntTarget.itemId);
            targetJson.put("isMob", p.huntTarget.isMob);
            targetJson.put("targetTemplateId", p.huntTarget.targetTemplateId);
            targetJson.put("mapId", p.huntTarget.mapId);
            json.put("huntTarget", targetJson);
        }

        try (FileWriter writer = new FileWriter(DATA_DIR + p.id + ".json")) {
            writer.write(json.toJSONString());
        } catch (IOException e) {
            core.GameLogger.error("WishManager.saveWishData: Error saving wish data", e);
        }
    }

    // --- Hunting Logic ---

    public boolean startHunting(Player p) {
        if (p.huntTarget != null) {
            try {
                Service.send_box_ThongBao_OK(p, "Bạn đang có mục tiêu rồi!");
            } catch (IOException e) {
                core.GameLogger.error("WishManager.startHunting: Error sending message to player", e);
            }
            return false;
        }

        // Tỉ lệ thất bại 40% — không dò được ngọc
        if (Util.random(0, 99) < 40) {
            try {
                Service.send_box_ThongBao_OK(p, "Không phát hiện tín hiệu Ngọc rồng vô cực nào...");
            } catch (IOException e) {
                core.GameLogger.error("WishManager.startHunting: Error sending message to player", e);
            }
            // Vẫn tiêu hao máy dò khi fail scan
            return true;
        }

        // Rarity priority (Weight): 7★ > 6★ > 5★ > 3★ > 2★ > 1★ > 4★
        int[] itemIds = { 1093, 1092, 1091, 1089, 1088, 1087, 1090 };
        int[] weights = { 400, 250, 200, 95, 30, 15, 10 };

        int selectedItemId = 1093;
        int rand = Util.random(0, 999);
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            if (rand < sum) {
                selectedItemId = itemIds[i];
                break;
            }
        }

        // Tỉ lệ player target tăng theo độ hiếm
        int playerTargetChance = switch (selectedItemId) {
            case 1090 -> 70; // ★4 khó nhất
            case 1087 -> 60; // ★1
            case 1088 -> 50; // ★2
            case 1089 -> 40; // ★3
            default -> 30; // ★5, ★6, ★7
        };

        boolean res = false;
        if (Util.random(0, 99) < playerTargetChance) {
            res = assignPlayerTarget(p, selectedItemId);
        } else {
            res = assignMobTarget(p, selectedItemId);
        }

        if (res) {
            saveWishData(p);
            // Tiêu hao máy dò sau khi dò thành công
        }
        return res;
    }

    private boolean assignMobTarget(Player p, int itemId) {
        return assignMobTarget(p, itemId, false);
    }

    private boolean assignMobTarget(Player p, int itemId, boolean fromFallback) {
        int[] config = MOB_WHITELIST[Util.random(MOB_WHITELIST.length)];
        int mapId = config[0];
        int mobTemplateId = config[1];

        Map[] maps = Map.get_map_by_id(mapId);
        if (maps == null || maps.length == 0) {
            // Fallback: If map is not loaded and not already in a fallback chain, try
            // players
            if (fromFallback)
                return false;
            return assignPlayerTarget(p, itemId, true);
        }

        MobTemplate mobTemp = MobTemplate.get_it_by_id(mobTemplateId);
        if (mobTemp == null) {
            // Fallback: If mob template is not loaded and not already in a fallback chain,
            // try players
            if (fromFallback)
                return false;
            return assignPlayerTarget(p, itemId, true);
        }

        // Pick a random zone
        int zone = Util.random(maps.length);
        p.huntTarget = new HuntTarget(mobTemp.name, maps[0].template.name, zone, itemId, true, mobTemplateId, mapId);
        sendHuntNotice(p);
        return true;
    }

    private boolean assignPlayerTarget(Player p, int itemId) {
        return assignPlayerTarget(p, itemId, false);
    }

    private boolean assignPlayerTarget(Player p, int itemId, boolean fromFallback) {
        boolean requireDonor = switch (itemId) {
            case 1089, 1087, 1090 -> true; // ★3, ★1, ★4
            default -> false;
        };

        List<Player> activePlayers = new ArrayList<>();
        synchronized (SessionManager.CLIENT_ENTRYS) {
            for (Session ss : SessionManager.CLIENT_ENTRYS) {
                if (ss != null && ss.p != null && ss.p.conn != null && ss.p.id != p.id && ss.p.map != null
                        && ss.p.hp > 0) {
                    try {
                        boolean eligible = !requireDonor || ss.p.getTongNap() > 0;
                        if (eligible)
                            activePlayers.add(ss.p);
                    } catch (IOException e) {
                        core.GameLogger.error("WishManager.assignPlayerTarget: Error checking player eligibility", e);
                    }
                }
            }
        }

        if (activePlayers.isEmpty()) {
            if (requireDonor) {
                // If it's a donor-required ball and no donor is available, try fallback to mobs
                // (unless already in fallback)
                if (fromFallback)
                    return false;
                return assignMobTarget(p, itemId, true);
            }
            // For other balls, if no one is online and it's a primary hunt or fallback, we
            // just fail gracefully
            try {
                Service.send_box_ThongBao_OK(p,
                        "Máy dò hiện không tìm thấy mục tiêu nào khả dụng. Hãy thử lại sau (Không mất Máy dò).");
            } catch (IOException e) {
                core.GameLogger.error("WishManager.assignPlayerTarget: Error sending message to player", e);
            }
            return false;
        }

        Player targetPlayer = activePlayers.get(Util.random(activePlayers.size()));
        p.huntTarget = new HuntTarget(targetPlayer.name, targetPlayer.map.template.name, targetPlayer.map.zone_id,
                itemId, false, targetPlayer.id, targetPlayer.map.template.id);
        sendHuntNotice(p);
        return true;
    }

    public void sendHuntNotice(Player p) {
        if (p.huntTarget == null)
            return;
        String msg = "Máy dò phát hiện Ngọc rồng vô cực " + (p.huntTarget.itemId - 1086) + " Sao đang được giữ bởi "
                + p.huntTarget.name + " tại " + p.huntTarget.mapName + " - Khu " + (p.huntTarget.zone + 1) + ".";
        try {
            Service.send_box_ThongBao_OK(p, msg);
        } catch (IOException e) {
            core.GameLogger.error("WishManager.sendHuntNotice: Error sending message to player", e);
        }
    }

    public void checkReward(Player p, Object target) {
        if (p.huntTarget == null)
            return;

        boolean success = false;
        if (target instanceof Mob) {
            Mob m = (Mob) target;
            if (p.huntTarget.isMob && m.mob_template.mob_id == p.huntTarget.targetTemplateId) {
                if (p.map.template.id != p.huntTarget.mapId) {
                    try {
                        Service.send_box_ThongBao_OK(p, "Ngọc không ở đây, hãy đến " + p.huntTarget.mapName + "!");
                    } catch (IOException e) {
                        core.GameLogger.error("WishManager.checkReward: Error sending message to player", e);
                    }
                    return;
                }

                int dropChance = switch (p.huntTarget.itemId) {
                    case 1093 -> 40; // ★7
                    case 1092 -> 30; // ★6
                    case 1091 -> 20; // ★5
                    case 1089 -> 15; // ★3
                    case 1088 -> 10; // ★2
                    case 1087 -> 5; // ★1
                    case 1090 -> 2; // ★4 (Rarest)
                    default -> 50;
                };

                if (Util.random(0, 99) < dropChance) {
                    success = true;
                } else {
                    try {
                        Service.send_box_ThongBao_OK(p, "Con quái này không chứa ngọc, hãy tìm con khác!");
                    } catch (IOException e) {
                        core.GameLogger.error("WishManager.checkReward: Error sending message to player", e);
                    }
                }
            }
        } else if (target instanceof Player) {
            Player targetP = (Player) target;
            if (!p.huntTarget.isMob && targetP.id == p.huntTarget.targetTemplateId) {
                success = true; // PvP remains 100% on kill for now as requested
            }
        }

        if (success) {
            int ballId = p.huntTarget.itemId;
            boolean added = p.item.add_item_bag47(4, ballId, 1);
            if (!added) {
                try {
                    Service.send_box_ThongBao_OK(p, "Hành trang đầy! Hãy dọn chỗ trống rồi thử lại.");
                } catch (IOException e) {
                    core.GameLogger.error("WishManager.checkReward: Error sending message to player", e);
                }
                return; // giữ nguyên huntTarget, player thử lại sau
            }
            try {
                p.item.update_Inventory(-1, false);
                Service.send_box_ThongBao_OK(p,
                        "Bạn đã nhận được Ngọc rồng vô cực " + (ballId - 1086) + " Sao từ " + p.huntTarget.name + "!");
            } catch (IOException e) {
                core.GameLogger.error("WishManager.checkReward: Error sending message to player", e);
            }
            p.huntTarget = null;
            saveWishData(p);
        }
    }

    // Gọi khi player logout — notify tất cả người đang target player này
    public void onPlayerLogout(Player logoutPlayer) {
        List<Player> affectedHunters = new ArrayList<>();

        synchronized (SessionManager.CLIENT_ENTRYS) {
            for (Session ss : SessionManager.CLIENT_ENTRYS) {
                if (ss == null || ss.p == null)
                    continue;
                Player hunter = ss.p;
                if (hunter.huntTarget == null)
                    continue;
                if (hunter.huntTarget.isMob)
                    continue;
                if (hunter.huntTarget.targetTemplateId != logoutPlayer.id)
                    continue;
                affectedHunters.add(hunter);
            }
        }

        // Xử lý ngoài synchronized — tránh block session list
        for (Player hunter : affectedHunters) {
            hunter.huntTarget = null;
            saveWishData(hunter);
            try {
                Service.send_box_ThongBao_OK(hunter,
                        "Mục tiêu " + logoutPlayer.name + " đã thoát game! Bạn có thể dò lại.");
            } catch (IOException e) {
                core.GameLogger.error("WishManager.onPlayerLogout: Error sending message to player", e);
            }
        }
    }

    public static class WishStats {
        public long extol = 0;
        public long dodge = 0; // points
        public long reduction = 0; // points
        public long react = 0; // points
        public long pierce = 0; // points
        public long decDodge = 0; // points
        public long decReact = 0; // points
        public long decReduc = 0; // points
        public long decPierce = 0; // points
        public long finalDame = 0; // points
        public long finalHp = 0; // points
        public long critDame = 0; // points

        // Coefficients (multiplier to get internal percentage representation)
        // 1 point = 10% (internally 100) or 5% (internally 50)
        public static final int COEFF_10 = 100;
        public static final int COEFF_5 = 50;

        public long getBonus(int id) {
            switch (id) {
                case 11:
                    return critDame * COEFF_10;
                case 12:
                    return dodge * COEFF_10;
                case 13:
                    return pierce * COEFF_10;
                case 14:
                    return react * COEFF_10;
                case 69:
                    return reduction * COEFF_10;
                case 46:
                    return finalDame * COEFF_5;
                case 50:
                    return decPierce * COEFF_10;
                case 51:
                    return decDodge * COEFF_10;
                case 52:
                    return decReact * COEFF_10;
                case 56:
                    return finalHp * COEFF_5;
                case 63:
                    return decReduc * COEFF_10;
                default:
                    return 0;
            }
        }
    }

    public static class HuntTarget {
        public String name;
        public String mapName;
        public int zone;
        public int itemId;
        public boolean isMob;
        public int targetTemplateId;
        public int mapId;

        public HuntTarget(String name, String mapName, int zone, int itemId, boolean isMob, int targetTemplateId,
                int mapId) {
            this.name = name;
            this.mapName = mapName;
            this.zone = zone;
            this.itemId = itemId;
            this.isMob = isMob;
            this.targetTemplateId = targetTemplateId;
            this.mapId = mapId;
        }
    }
}
