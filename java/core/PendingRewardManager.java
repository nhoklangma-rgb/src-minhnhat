package core;

import client.Player;
import database.SQL;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import core.Util;
import core.Service;
import map.Map;

/**
 * PendingRewardManager.java
 * Quản lý phần thưởng chờ (Tài Xỉu, Bầu Cua, ...)
 */
public class PendingRewardManager {

    public static void addReward(int playerId, String gameType, long amount) {
        if (amount <= 0) return;

        // 1. Lưu DB trước để đảm bảo an toàn
        insertDB(playerId, gameType, amount);

        // 2. Check online và claim ngay nếu có thể
        Player p = Map.get_player_by_id_allmap(playerId);
        if (p != null && p.conn != null) {
            try {
                claimAll(p);
            } catch (Exception e) {
                // Nếu claim ngay bị lỗi, player vẫn nhận được khi login lại (vì đã có trong DB)
                GameLogger.error("PendingRewardManager: Failed to claim rewards for online player " + p.name, e);
            }
        }
    }

    private static void insertDB(int playerId, String gameType, long amount) {
        String sql = "INSERT INTO `pending_rewards` (`player_id`, `game_type`, `amount`, `created_at`) VALUES (?, ?, ?, ?)";
        try (Connection conn = SQL.gI().getCon();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setString(2, gameType);
            ps.setLong(3, amount);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            GameLogger.error("PendingRewardManager: SQL error inserting reward for player ID " + playerId, e);
        }
    }

    public static void claimAll(Player player) throws IOException {
        String query = "SELECT `id`, `game_type`, `amount` FROM `pending_rewards` WHERE `player_id` = ?";
        List<Integer> idsToDel = new ArrayList<>();
        long totalAmount = 0;
        StringBuilder detail = new StringBuilder();

        try (Connection conn = SQL.gI().getCon();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, player.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("game_type");
                    long amount = rs.getLong("amount");

                    idsToDel.add(id);
                    totalAmount += amount;

                    String gameName = type.equalsIgnoreCase("taixiu") ? "Tài Xỉu"
                            : (type.equalsIgnoreCase("baucua") ? "Bầu Cua"
                                    : (type.equalsIgnoreCase("blackjack") ? "Blackjack" : type));
                    detail.append("- ").append(gameName).append(": +").append(Util.number_format(amount)).append(" Extol\n");
                }
            }
        } catch (SQLException e) {
            GameLogger.error("PendingRewardManager: SQL error querying rewards for player " + player.name, e);
            return;
        }

        if (totalAmount > 0) {
            // Xóa khỏi DB trước
            deleteRewards(idsToDel);

            // Cộng tiền vào in-memory
            player.update_vnd(totalAmount);
            player.update_money();

            // Thông báo
            String msg = "Bạn nhận được phần thưởng chờ:\n" + detail.toString() + "Tổng cộng: +" + Util.number_format(totalAmount) + " Extol đã được cộng vào hành trang.";
            Service.send_box_ThongBao_OK(player, msg);
        }
    }

    private static void deleteRewards(List<Integer> ids) {
        if (ids.isEmpty()) return;
        StringBuilder sql = new StringBuilder("DELETE FROM `pending_rewards` WHERE `id` IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append("?");
            if (i < ids.size() - 1) sql.append(",");
        }
        sql.append(")");

        try (Connection conn = SQL.gI().getCon();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            GameLogger.error("PendingRewardManager: SQL error deleting rewards", e);
        }
    }
}
