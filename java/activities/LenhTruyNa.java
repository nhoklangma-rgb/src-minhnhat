package activities;

import client.Item;
import client.Player;
import core.GameLogger;
import core.Service;
import core.Util;
import database.SQL;
import io.Message;
import java.sql.*;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import map.Map;
import template.GiftBox;
import template.ItemFashion;
import template.ItemFashionP;
import template.ItemFashionP2;
import template.ItemTemplate3;
import template.Item_wear;

public class LenhTruyNa {

    // Trạng thái lệnh truy nã
    public static final byte STATUS_AVAILABLE = 0; // Chưa ai nhận
    public static final byte STATUS_ACCEPTED = 1; // Đã có người nhận
    public static final byte STATUS_COMPLETED = 2; // Hoàn thành
    public static final byte STATUS_REWARDED = 3; // Đã nhận thưởng
    public static final byte STATUS_CANCELLED = 4; // Đã hủy
    public static final byte STATUS_DELETED = 5; // Đã xoá
    public static final long AUTO_CANCEL_TIME = 24 * 60 * 60 * 1000;

    public static class WantedInfo {

        public int id;
        public String name;
        public String nameIssuer;
        public byte level;
        public int wantedMoney;
        public byte typeOnline;
        public String namePlayerNhan;
        public byte isWantedSuccess = 0;
        public byte isReceiveGift = 0;
        public short head = 1;
        public short hair = 1;
        public short hat = -1;
        public short[] parts = new short[] { -1, -1, -1 };
    }

    public static void createWantedPoster(Player issuer, String targetName, int rewardMoney) {
        try (Connection conn = SQL.gI().getCon()) {
            String countQuery = "SELECT COUNT(*) FROM wanted_posters WHERE issuer_player_id = ? AND status IN (0,1,2,3)";
            try (PreparedStatement ps = conn.prepareStatement(countQuery)) {
                ps.setInt(1, issuer.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= 5) {
                        Service.send_box_ThongBao_OK(issuer, "Bạn đã phát tối đa 5 lệnh truy nã");
                        return;
                    }
                }
            }
            String getTargetQuery = "SELECT id FROM players WHERE name = ?";
            int targetId = -1;
            try (PreparedStatement ps = conn.prepareStatement(getTargetQuery)) {
                ps.setString(1, targetName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        targetId = rs.getInt("id");
                    } else {
                        Service.send_box_ThongBao_OK(issuer, "Không tìm thấy " + targetName);
                        return;
                    }
                }
            }
            if (issuer.get_vang() < rewardMoney) {
                Service.send_box_ThongBao_OK(issuer, "Bạn không đủ " + Util.number_format(rewardMoney) + " Beri");
                return;
            }
            issuer.update_vang(-rewardMoney);
            issuer.update_money();
            String insertQuery = "INSERT INTO wanted_posters (target_player_id, target_name, issuer_player_id, issuer_name, reward_money, status, created_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                ps.setInt(1, targetId);
                ps.setString(2, targetName);
                ps.setInt(3, issuer.id);
                ps.setString(4, issuer.name);
                ps.setInt(5, rewardMoney);
                ps.setByte(6, STATUS_AVAILABLE);
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
                Service.send_box_ThongBao_OK(issuer,
                        "Đã phát lệnh truy nã " + targetName + " với " + Util.number_format(rewardMoney) + " Beri");
            }

        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.createWantedPoster: Error creating wanted poster for player " + issuer.name,
                    e);
        }
    }

    private static void deleteRewardedWanted(Player player, int wantedId) {
        try (Connection conn = SQL.gI().getCon()) {
            String query = "UPDATE wanted_posters SET status = ? WHERE id = ? AND issuer_player_id = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setByte(1, STATUS_DELETED);
                ps.setInt(2, wantedId);
                ps.setInt(3, player.id);
                ps.setByte(4, STATUS_REWARDED);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    sendList(player, (byte) 3);
                    Service.send_box_ThongBao_OK(player, "Xoá thành công lệnh truy nã");
                }
            }
        } catch (Exception e) {
            GameLogger.error(
                    "LenhTruyNa.deleteRewardedWanted: Error deleting rewarded wanted for player " + player.name, e);
        }
    }

    public static void showTargetInfo(Player hunter) {
        try (Connection conn = SQL.gI().getCon()) {
            String query = "SELECT target_player_id, target_name, accepted_time FROM wanted_posters "
                    + "WHERE hunter_player_id = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, hunter.id);
                ps.setByte(2, STATUS_ACCEPTED);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        Service.send_box_ThongBao_OK(hunter, "Bạn chưa nhận lệnh truy nã nào");
                        return;
                    }
                    int targetPlayerId = rs.getInt("target_player_id");
                    String targetName = rs.getString("target_name");
                    StringBuilder info = new StringBuilder();
                    info.append("Thông Tin Truy Nã").append("\n");
                    info.append("Tên : ").append(targetName).append("\n");
                    Player targetOnline = Map.get_player_by_name_allmap(targetName);
                    if (targetOnline != null) {
                        info.append("Trạng thái : Online\n");
                        info.append("Vị trí : ").append(targetOnline.map.template.name)
                                .append(" - Khu ").append(targetOnline.map.zone_id + 1).append("\n");
                    } else {
                        info.append("Trạng thái : Offline\n");
                    }
                    hunter.data_yesno = new int[] { targetPlayerId };
                    Service.send_box_yesno(hunter, 85, "Thông tin lệnh truy nã",
                            info.toString(),
                            new String[] { "Dịch chuyển", "Đóng" },
                            new byte[] { -1, -1 });
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.showTargetInfo: Error showing target info for player " + hunter.name, e);
        }
    }

    private static List<WantedInfo> getMainList() {
        List<WantedInfo> result = new ArrayList<>();
        try (Connection conn = SQL.gI().getCon()) {
            String query = "SELECT w.*, p.body, p.it_body, p.fashion, p.site, p.tocSuper FROM wanted_posters w LEFT JOIN players p ON w.target_player_id = p.id WHERE w.status = ? ORDER BY w.reward_money DESC LIMIT 50";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setByte(1, STATUS_AVAILABLE);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(createWantedInfoFromRS(rs));
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.getMainList: SQL error loading main list", e);
        }
        return result;
    }

    private static List<WantedInfo> getIssuedList(int issuerId) {
        List<WantedInfo> result = new ArrayList<>();
        try (Connection conn = SQL.gI().getCon()) {
            String query = "SELECT w.*, p.body, p.it_body, p.fashion, p.site, p.tocSuper "
                    + "FROM wanted_posters w "
                    + "LEFT JOIN players p ON w.target_player_id = p.id "
                    + "WHERE w.issuer_player_id = ? "
                    + "AND w.status != ? "
                    + "AND w.status != ? "
                    + "ORDER BY w.created_time DESC";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, issuerId);
                ps.setByte(2, STATUS_CANCELLED);
                ps.setByte(3, STATUS_DELETED);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WantedInfo wi = createWantedInfoFromRS(rs);
                        wi.namePlayerNhan = rs.getString("hunter_name") != null ? rs.getString("hunter_name")
                                : ". . . . .";
                        wi.isWantedSuccess = (byte) (rs.getByte("status") >= STATUS_COMPLETED ? 1 : 0);
                        wi.isReceiveGift = (byte) (rs.getByte("status") == STATUS_REWARDED ? 1 : 0);
                        result.add(wi);
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.getIssuedList: SQL error loading issued list for player " + issuerId, e);
        }
        return result;
    }

    private static WantedInfo getLatestReceived(int hunterId) {
        WantedInfo wi = null;
        try (Connection conn = SQL.gI().getCon()) {
            String query = "SELECT w.*, p.body, p.it_body, p.fashion, p.site, p.tocSuper "
                    + "FROM wanted_posters w "
                    + "LEFT JOIN players p ON w.target_player_id = p.id "
                    + "WHERE w.hunter_player_id = ? "
                    + "AND w.status IN (?,?) "
                    + "ORDER BY w.accepted_time DESC "
                    + "LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, hunterId);
                ps.setByte(2, STATUS_ACCEPTED);
                ps.setByte(3, STATUS_COMPLETED);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        wi = createWantedInfoFromRS(rs);
                        wi.namePlayerNhan = rs.getString("hunter_name");
                        wi.isWantedSuccess = (byte) (rs.getByte("status") >= STATUS_COMPLETED ? 1 : 0);
                        wi.isReceiveGift = (byte) (rs.getByte("status") == STATUS_REWARDED ? 1 : 0);
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error(
                    "LenhTruyNa.getLatestReceived: SQL error loading latest received wanted for player " + hunterId, e);
        }
        return wi;
    }

    private static WantedInfo createWantedInfoFromRS(ResultSet rs) throws SQLException {
        WantedInfo wi = new WantedInfo();
        wi.id = rs.getInt("id");
        wi.name = rs.getString("target_name");
        wi.nameIssuer = rs.getString("issuer_name");
        wi.wantedMoney = rs.getInt("reward_money");
        Player targetOnline = Map.get_player_by_name_allmap(wi.name);
        wi.typeOnline = (byte) (targetOnline != null ? 1 : 0);
        wi.level = targetOnline != null ? (byte) targetOnline.level : 1;
        setCharacterAppearance(wi, rs);
        return wi;
    }

    private static void setCharacterAppearance(WantedInfo info, ResultSet rs) throws SQLException {
        try {
            String fashionStr = rs.getString("fashion");
            String bodyStr = rs.getString("body");
            String itemStr = rs.getString("it_body");
            String siteStr = rs.getString("site");
            if (bodyStr != null && !bodyStr.isEmpty()) {
                JSONArray bodyJson = (JSONArray) JSONValue.parse(bodyStr);
                info.head = Short.parseShort(bodyJson.get(0).toString());
                info.hair = Short.parseShort(bodyJson.get(1).toString());
            }
            short hair_ = -1;
            short head_ = -1;
            short[] fashion_ = null;
            List<ItemFashionP2> fashionList = (fashionStr != null && !fashionStr.isEmpty()) ? parseFashionP2(fashionStr)
                    : null;
            List<ItemFashionP> itFashionP = (fashionStr != null && !fashionStr.isEmpty()) ? parseFashionP(fashionStr)
                    : null;
            Item_wear[] items = (itemStr != null && !itemStr.isEmpty()) ? parseItemWear(itemStr) : null;

            if (fashionList != null) {
                for (ItemFashionP2 f : fashionList) {
                    if (f.is_use) {
                        ItemFashion temp = ItemFashion.get_item(f.id);
                        if (temp != null) {
                            fashion_ = temp.mWearing;
                            break;
                        }
                    }
                }
            }
            if (fashion_ != null && fashion_[6] != -1) {
                head_ = fashion_[6];
                hair_ = fashion_[7]; // -1 hoặc số, -1 = ẩn hair
            } else {
                if (itFashionP != null) {
                    for (ItemFashionP it : itFashionP) {
                        if (it.is_use && it.category == 103) {
                            hair_ = it.icon; // hair 105
                        }
                        if (it.is_use && it.category == 108) {
                            head_ = it.icon; // head 108
                        }
                    }
                }
            }
            if (hair_ == 772) {
                hair_ += rs.getInt("tocSuper");
            }
            if (fashion_ != null) {
                info.parts[0] = fashion_[3] != -1 ? fashion_[3]
                        : (items != null && items[3] != null ? items[3].template.part : -1);
                info.parts[1] = fashion_[5] != -1 ? fashion_[5]
                        : (items != null && items[5] != null ? items[5].template.part : -1);
                info.parts[2] = fashion_[0] != -1 ? fashion_[0]
                        : (items != null && items[0] != null ? items[0].template.part : -1);
            } else {
                info.parts[0] = (items != null && items[3] != null) ? items[3].template.part : -1;
                info.parts[1] = (items != null && items[5] != null) ? items[5].template.part : -1;
                info.parts[2] = (items != null && items[0] != null) ? items[0].template.part : -1;
            }
            info.head = head_ != -1 ? head_ : info.head;
            info.hair = hair_ != -1 ? hair_ : info.hair;
            boolean isShowHat = false;
            if (siteStr != null && !siteStr.isEmpty()) {
                JSONArray site = (JSONArray) JSONValue.parse(siteStr);
                isShowHat = Byte.parseByte(site.get(6).toString()) == 1;
            }
            if (isShowHat) {
                if (fashion_ != null && fashion_[1] != -1) {
                    info.hat = fashion_[1]; // ưu tiên hat fashion
                } else if (items != null && items[1] != null) {
                    info.hat = ItemTemplate3.get_it_by_id(items[1].template.id).part; // hat item
                } else {
                    info.hat = -1;
                }
            } else {
                info.hat = -1; // tắt hat
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.getWantedInfo: Error loading wanted info for player " + info.name, e);
            // fallback
            info.head = 1;
            info.hair = 1;
            info.hat = -1;
            info.parts = new short[] { -1, -1, -1 };
        }
    }

    public static void acceptWanted(Player hunter, int wantedId) {
        try (Connection conn = SQL.gI().getCon()) {
            String checkQuery = "SELECT COUNT(*) FROM wanted_posters WHERE hunter_player_id = ? AND status IN (1,2)";
            try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                ps.setInt(1, hunter.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        Service.send_box_ThongBao_OK(hunter,
                                "Bạn đang nhận 1 lệnh truy nã rồi\nKhông thể nhận thêm lệnh nữa");
                        return;
                    }
                }
            }
            String targetCheckQuery = "SELECT issuer_player_id, target_player_id FROM wanted_posters WHERE id = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(targetCheckQuery)) {
                ps.setInt(1, wantedId);
                ps.setByte(2, STATUS_AVAILABLE);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        Service.send_box_ThongBao_OK(hunter, "Lệnh truy nã không tồn tại hoặc đã được nhận.");
                        return;
                    }
                    int issuerId = rs.getInt("issuer_player_id");
                    int targetId = rs.getInt("target_player_id");
                    if (issuerId == hunter.id) {
                        Service.send_box_ThongBao_OK(hunter, "Không thể nhận lệnh mà bạn đã phát.");
                        return;
                    }
                    if (targetId == hunter.id) {
                        Service.send_box_ThongBao_OK(hunter, "Không thể nhận lệnh truy nã bản thân.");
                        return;
                    }
                }
            }
            String updateQuery = "UPDATE wanted_posters SET status = ?, hunter_player_id = ?, hunter_name = ?, accepted_time = ? WHERE id = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                ps.setByte(1, STATUS_ACCEPTED);
                ps.setInt(2, hunter.id);
                ps.setString(3, hunter.name);
                ps.setLong(4, System.currentTimeMillis());
                ps.setInt(5, wantedId);
                ps.setByte(6, STATUS_AVAILABLE);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    Service.send_box_ThongBao_OK(hunter,
                            "Nhận lệnh truy nã thành công, hãy đồ sát tiêu diệt đối tượng, bạn có 24 tiếng để hoàn thành nhiệm vụ.");
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.acceptWanted: Error accepting wanted for player " + hunter.name, e);
        }
    }

    public static void completeWantedByTarget(int targetPlayerId, int hunterId) {
        try (Connection conn = SQL.gI().getCon()) {
            String query = "UPDATE wanted_posters SET status = ?, completed_time = ? "
                    + "WHERE target_player_id = ? AND hunter_player_id = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setByte(1, STATUS_COMPLETED);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, targetPlayerId);
                ps.setInt(4, hunterId);
                ps.setByte(5, STATUS_ACCEPTED);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.completeWantedByTarget: Error completing wanted for target " + targetPlayerId,
                    e);
        }
    }

    public static void cancelWanted(Player player, int wantedId, boolean isIssuer) {
        try (Connection conn = SQL.gI().getCon()) {
            String query;
            if (isIssuer) {
                query = "UPDATE wanted_posters SET status = ? WHERE id = ? AND issuer_player_id = ? AND status IN (0,1)";
            } else {
                query = "UPDATE wanted_posters SET status = 0, hunter_player_id = NULL, hunter_name = NULL, accepted_time = NULL WHERE id = ? AND hunter_player_id = ? AND status = 1";
            }
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                if (isIssuer) {
                    ps.setByte(1, STATUS_CANCELLED);
                    ps.setInt(2, wantedId);
                    ps.setInt(3, player.id);
                } else {
                    ps.setInt(1, wantedId);
                    ps.setInt(2, player.id);
                }
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    Service.send_box_ThongBao_OK(player, (isIssuer ? "Huỷ phát" : "Huỷ nhận") + " lệnh truy nã");
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.cancelWanted: Error cancelling wanted for player " + player.name, e);
        }
    }

    public static void claimReward(Player hunter, int wantedId) {
        try (Connection conn = SQL.gI().getCon()) {
            String getQuery = "SELECT reward_money FROM wanted_posters WHERE id = ? AND hunter_player_id = ? AND status = ?";
            int reward = 0;
            try (PreparedStatement ps = conn.prepareStatement(getQuery)) {
                ps.setInt(1, wantedId);
                ps.setInt(2, hunter.id);
                ps.setByte(3, STATUS_COMPLETED);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reward = rs.getInt("reward_money");
                    } else {
                        return;
                    }
                }
            }
            String updateQuery = "UPDATE wanted_posters SET status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                ps.setByte(1, STATUS_REWARDED);
                ps.setInt(2, wantedId);
                ps.executeUpdate();
            }
            List<GiftBox> listGiftP = new ArrayList<>();
            GiftBox.addGiftBox(listGiftP, 0, 4, reward);
            Service.send_gift(hunter, 2, "Lệnh truy nã", "", listGiftP, true);
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.claimReward: Error claiming reward for player " + hunter.name, e);
        }
    }

    public static void autoCancel() {
        try (Connection conn = SQL.gI().getCon()) {
            long expireTimeHunter = System.currentTimeMillis() - AUTO_CANCEL_TIME; // 24h
            long expireTimeAvailable = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L); // 2 ngày
            // Huỷ những lệnh đã có người nhận nhưng quá hạn 24h
            String queryHunter = "UPDATE wanted_posters "
                    + "SET status = 0, hunter_player_id = NULL, hunter_name = NULL, accepted_time = NULL "
                    + "WHERE status = 1 AND accepted_time < ?";
            try (PreparedStatement ps = conn.prepareStatement(queryHunter)) {
                ps.setLong(1, expireTimeHunter);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) {
                    GameLogger.info(">> Auto cancelled " + cancelled + " expired 24h posters");
                }
            }
            // Huỷ những lệnh đã phát nhưng 2 ngày không ai nhận
            String queryAvailable = "UPDATE wanted_posters "
                    + "SET status = ? "
                    + "WHERE status = 0 AND created_time < ?";
            try (PreparedStatement ps = conn.prepareStatement(queryAvailable)) {
                ps.setByte(1, STATUS_CANCELLED);
                ps.setLong(2, expireTimeAvailable);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) {
                    GameLogger.info(">> Auto cancelled " + cancelled + " posters after 2 day");
                }
            }

        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.autoCancel: Error auto cancelling wanted posters", e);
        }
    }

    // ===== Helper methods =====
    private static List<ItemFashionP2> parseFashionP2(String fashionStr) {
        List<ItemFashionP2> list = new ArrayList<>();
        try {
            JSONArray js = (JSONArray) JSONValue.parse(fashionStr);
            JSONArray js2 = (JSONArray) JSONValue.parse(js.get(1).toString());
            for (Object o : js2) {
                JSONArray item = (JSONArray) JSONValue.parse(o.toString());
                ItemFashionP2 temp = new ItemFashionP2();
                temp.id = Short.parseShort(item.get(0).toString());
                temp.is_use = Byte.parseByte(item.get(1).toString()) == 1;
                list.add(temp);
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.parseFashionP2: Error parsing fashion string", e);
        }
        return list;
    }

    private static List<ItemFashionP> parseFashionP(String fashionStr) {
        List<ItemFashionP> list = new ArrayList<>();
        try {
            JSONArray js = (JSONArray) JSONValue.parse(fashionStr);
            JSONArray js2 = (JSONArray) JSONValue.parse(js.get(0).toString());
            for (Object o : js2) {
                JSONArray item = (JSONArray) JSONValue.parse(o.toString());
                ItemFashionP temp = new ItemFashionP();
                temp.category = Byte.parseByte(item.get(0).toString());
                temp.id = Short.parseShort(item.get(1).toString());
                temp.icon = Short.parseShort(item.get(2).toString());
                temp.is_use = Byte.parseByte(item.get(3).toString()) == 1;
                list.add(temp);
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.parseFashionP: Error parsing fashion string", e);
        }
        return list;
    }

    private static Item_wear[] parseItemWear(String jsonStr) {
        Item_wear[] items = new Item_wear[8];
        try {
            JSONArray js = (JSONArray) JSONValue.parse(jsonStr);
            for (Object o : js) {
                JSONArray arr = (JSONArray) JSONValue.parse(o.toString());
                Item_wear item = new Item_wear();
                Item.readUpdateItem(arr.toString(), item);
                items[item.index] = item;
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.parseItemWear: Error parsing item wear string", e);
        }
        return items;
    }

    public static void sendList(Player p) {
        sendList(p, (byte) 0);
        autoCancel();
    }

    private static void sendList(Player p, byte type) {
        try {
            Message m = new Message(-89);
            m.writer().writeByte(type);
            List<WantedInfo> list;
            switch (type) {
                case 0:
                    list = getMainList();
                    break;
                case 3:
                    list = getIssuedList(p.id);
                    break;
                default:
                    list = getMainList();
                    break;
            }
            m.writer().writeShort(list.size());
            for (WantedInfo wi : list) {
                m.writer().writeShort(wi.id);
                m.writer().writeUTF(wi.nameIssuer);
                m.writer().writeInt(wi.wantedMoney);
                m.writer().writeUTF(wi.name);
                m.writer().writeShort(wi.level);
                m.writer().writeShort(wi.head);
                m.writer().writeShort(wi.hair);
                m.writer().writeShort(wi.hat);

                if (type == 0) {
                    m.writer().writeByte(wi.typeOnline);
                } else if (type == 3) {
                    m.writer().writeUTF(wi.namePlayerNhan);
                    m.writer().writeByte(wi.isWantedSuccess);
                    m.writer().writeByte(wi.isReceiveGift);
                }
            }

            p.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.sendList: Error sending list to player " + p.name, e);
        }
    }

    private static void sendDetail(Player p, byte type, int id) {
        try {
            WantedInfo wi = findWanted(type, id, p.id);
            if (wi == null) {
                return;
            }

            Message m = new Message(-89);
            m.writer().writeByte(type);
            m.writer().writeShort(wi.id);
            m.writer().writeUTF(wi.name);
            m.writer().writeInt(wi.wantedMoney);
            m.writer().writeShort(wi.level);
            m.writer().writeShort(wi.head);
            m.writer().writeShort(wi.hair);

            m.writer().writeByte(6);
            m.writer().writeShort(wi.parts[2]);
            m.writer().writeShort(wi.hat);
            m.writer().writeShort(-1);
            m.writer().writeShort(wi.parts[0]);
            m.writer().writeShort(-1);
            m.writer().writeShort(wi.parts[1]);

            if (type == 4 || type == 5) {
                m.writer().writeByte(wi.isWantedSuccess);
            }
            if (type == 5) {
                m.writer().writeByte(wi.isReceiveGift);
            }

            p.addmsg(m);
            m.cleanup();
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.sendDetail: Error sending detail to player " + p.name, e);
        }
    }

    private static WantedInfo findWanted(byte type, int id, int playerId) {
        try (Connection conn = SQL.gI().getCon()) {
            String query = "SELECT w.*, p.body, p.it_body, p.fashion, p.site, p.tocSuper FROM wanted_posters w LEFT JOIN players p ON w.target_player_id = p.id WHERE w.id = ?";

            if (type == 4) {
                query += " AND w.hunter_player_id = ?";
            } else if (type == 5) {
                query += " AND w.issuer_player_id = ?";
            }

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, id);
                if (type == 4 || type == 5) {
                    ps.setInt(2, playerId);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        WantedInfo wi = createWantedInfoFromRS(rs);
                        wi.isWantedSuccess = (byte) (rs.getByte("status") >= STATUS_COMPLETED ? 1 : 0);
                        wi.isReceiveGift = (byte) (rs.getByte("status") == STATUS_REWARDED ? 1 : 0);
                        return wi;
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.findWanted: Error finding wanted for player " + playerId, e);
        }
        return null;
    }

    public static void process(Player p, Message msg) {
        try {
            byte type = msg.reader().readByte();
            int id = msg.reader().readShort();
            byte action = -1;
            if (msg.reader().available() > 0) {
                action = msg.reader().readByte();
            }
            switch (type) {
                case 0:
                    sendList(p, (byte) 0);
                    break;
                case 1:
                    if (action == 0) {
                        sendDetail(p, (byte) 1, id);
                    }
                    break;
                case 2:
                    acceptWanted(p, id);
                    break;
                case 3:
                    sendList(p, (byte) 3);
                    break;
                case 4:
                    if (action == 0 && id == -1) {
                        WantedInfo wi = getLatestReceived(p.id);
                        if (wi != null) {
                            sendDetail(p, (byte) 4, wi.id);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Bạn chưa nhận lệnh truy nã");
                        }
                    } else if (action == 1) {
                        cancelWanted(p, id, false);
                    } else if (action == 2) {
                        claimReward(p, id);
                    }
                    break;
                case 5:
                    if (action == 0) {
                        sendDetail(p, (byte) 5, id);
                    } else if (action == 1) {
                        cancelWanted(p, id, true);
                    } else if (action == 3) {
                        deleteRewardedWanted(p, id);
                    }
                    break;
            }
        } catch (Exception e) {
            GameLogger.error("LenhTruyNa.process: Error processing message for player " + p.name, e);
        }
    }
}
