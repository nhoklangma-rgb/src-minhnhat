package activities;

import client.Clan;
import client.Player;
import core.GameLogger;
import core.Manager;
import core.Service;
import database.SQL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ChiemDao {
  // Map 303 (Clan)
  public static Clan conqueringClan;
  private static long startTime = 0;
  private static long lastRewardTime = 0;
  private static int currentClanId = -1;
  private static final Set<Integer> announcedMilestones = new HashSet<>();

  // Map 304 (Individual) - Cached from DB
  public static int cachedHolderId304 = -1;
  public static String cachedHolderName304 = "";
  public static long cachedCaptureTime304 = 0;
  public static long cachedLastRewardTime304 = 0;
  private static final Set<Integer> announcedMilestones304 = new HashSet<>();

  private static Timer rewardTimer = null;

  static {
    loadIslandStatus304();
  }

  // ═══════════════════════════════════════════════════════
  // DB PERSISTENCE (Map 304)
  // ═══════════════════════════════════════════════════════

  public static void loadIslandStatus304() {
    Connection connection = null;
    Statement st = null;
    ResultSet rs = null;
    try {
      connection = SQL.gI().getCon();
      st = connection.createStatement();
      st.execute("CREATE TABLE IF NOT EXISTS `chiem_dao_304` (" +
          "`id` INT PRIMARY KEY DEFAULT 1," +
          "`player_id` INT DEFAULT -1," +
          "`player_name` VARCHAR(255) DEFAULT ''," +
          "`capture_time` BIGINT DEFAULT 0," +
          "`last_reward_time` BIGINT DEFAULT 0" +
          ");");

      rs = st.executeQuery("SELECT * FROM `chiem_dao_304` WHERE `id` = 1 LIMIT 1;");
      if (rs.next()) {
        cachedHolderId304 = rs.getInt("player_id");
        cachedHolderName304 = rs.getString("player_name");
        cachedCaptureTime304 = rs.getLong("capture_time");
        cachedLastRewardTime304 = rs.getLong("last_reward_time");
      } else {
        st.execute(
            "INSERT INTO `chiem_dao_304` (`id`, `player_id`, `player_name`, `capture_time`, `last_reward_time`) VALUES (1, -1, '', 0, 0);");
      }
    } catch (Exception e) {
      GameLogger.error("ChiemDao.loadIslandStatus304: SQL error loading island status", e);
    } finally {
      closeDB(connection, st, rs);
    }
  }

  public static void saveIslandStatus304() {
    Connection connection = null;
    PreparedStatement ps = null;
    try {
      connection = SQL.gI().getCon();
      ps = connection.prepareStatement(
          "UPDATE `chiem_dao_304` SET `player_id` = ?, `player_name` = ?, `capture_time` = ?, `last_reward_time` = ? WHERE `id` = 1;");
      ps.setInt(1, cachedHolderId304);
      ps.setString(2, cachedHolderName304);
      ps.setLong(3, cachedCaptureTime304);
      ps.setLong(4, cachedLastRewardTime304);
      ps.executeUpdate();
    } catch (Exception e) {
      GameLogger.error("ChiemDao.saveIslandStatus304: SQL error saving island status", e);
    } finally {
      closeDB(connection, ps, null);
    }
  }

  private static void closeDB(Connection conn, Statement st, ResultSet rs) {
    try {
      if (rs != null)
        rs.close();
      if (st != null)
        st.close();
      if (conn != null)
        conn.close();
    } catch (Exception e) {
      GameLogger.error("ChiemDao.closeDB: Error closing database resources", e);
    }
  }

  // ═══════════════════════════════════════════════════════
  // TIMER
  // ═══════════════════════════════════════════════════════

  public static void startRewardTask() {
    if (rewardTimer != null)
      return;

    rewardTimer = new Timer();
    rewardTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          // Map 303: Clan
          if (conqueringClan != null) {
            rewardClanMembers(303, conqueringClan);
          }
          // Map 304: Individual — chỉ cần online, không cần ở map 304
          if (cachedHolderId304 != -1) {
            Player p = Manager.gI().getPlayer(cachedHolderId304);
            if (p != null && p.conn != null) { // FIX 2: bỏ check map.id == 304
              rewardPlayer(p);
            }
          }
        } catch (Exception e) {
          GameLogger.error("ChiemDao.rewardTask: Error in reward task", e);
        }
      }
    }, 60 * 1000, 60 * 1000);
  }

  public static void stopRewardTask() {
    if (rewardTimer != null) {
      rewardTimer.cancel();
      rewardTimer = null;
    }
  }

  public static boolean isRewardTaskRunning() {
    return rewardTimer != null;
  }

  // ═══════════════════════════════════════════════════════
  // LOGIC CHÍNH
  // ═══════════════════════════════════════════════════════

  private static void rewardClanMembers(int mapId, Clan clan) throws IOException {
    long now = System.currentTimeMillis();

    if (currentClanId != clan.id) {
      startTime = now;
      lastRewardTime = now;
      currentClanId = clan.id;
      announcedMilestones.clear();
      Manager.gI().chatKTG(0, "Băng " + clan.name + " vừa chiếm đảo!", 5);
      return;
    }

    long elapsedMillis = now - startTime;
    long totalMinutes = elapsedMillis / (60 * 1000);

    if (now - lastRewardTime >= 10 * 60 * 1000) {
      int xp = calculateXP(totalMinutes);
      clan.update_xp(xp);
      clan.chat_on_board(clan.members.get(0).id, clan.members.get(0).name,
          "Giữ đảo " + formatTime(totalMinutes) + " → +" + xp + " XP Băng", -3);
      lastRewardTime = now;
    }

    int[] milestones = { 30, 60, 120, 240 };
    for (int m : milestones) {
      if (totalMinutes >= m && !announcedMilestones.contains(m)) {
        announcedMilestones.add(m);
        Manager.gI().chatKTG(0, "Băng " + clan.name + " đã giữ đảo " + formatTime(m) + "! Ai dám cướp?", 5);
      }
    }
  }

  private static void rewardPlayer(Player p) throws IOException {
    long now = System.currentTimeMillis();
    long elapsedMillis = now - cachedCaptureTime304;
    long totalMinutes = elapsedMillis / (60 * 1000);

    if (now - cachedLastRewardTime304 >= 10 * 60 * 1000) {
      int extol = calculateExtol(totalMinutes);
      p.update_extol(extol);
      Service.send_box_ThongBao_OK(p, "Bạn nhận được " + extol + " Extol từ việc giữ đảo " + formatTime(totalMinutes));

      cachedLastRewardTime304 = now;
      saveIslandStatus304();
    }

    int[] milestones = { 30, 60, 120, 240 };
    for (int m : milestones) {
      if (totalMinutes >= m && !announcedMilestones304.contains(m)) {
        announcedMilestones304.add(m);
        Manager.gI().chatKTG(0, p.name + " đã giữ Đảo Extol " + formatTime(m) + "! Ai dám cướp?", 5);
      }
    }
  }

  public static void onPlayerCapture304(Player p) {
    long now = System.currentTimeMillis();

    // 1. Thưởng cho người giữ cũ (chỉ tính block 10 phút tròn)
    if (cachedHolderId304 != -1 && cachedHolderId304 != p.id) {
      long rewardMinutes = (now - cachedLastRewardTime304) / (60 * 1000);
      if (rewardMinutes >= 10) {
        int blocks = (int) (rewardMinutes / 10);
        long totalMinutes = (now - cachedCaptureTime304) / (60 * 1000);
        int extolPerBlock = calculateExtol(totalMinutes);
        int totalExtol = extolPerBlock * blocks;

        Player oldP = Manager.gI().getPlayer(cachedHolderId304);
        if (oldP != null && oldP.conn != null) {
          try {
            oldP.update_extol(totalExtol);
          } catch (IOException e) {
            GameLogger.error("ChiemDao.onPlayerCapture304: Error updating old holder's extol", e);
          }
        } else {
          Player.update_extol_offline(cachedHolderId304, totalExtol);
        }
      }
    }

    // 2. Set người giữ mới
    cachedHolderId304 = p.id;
    cachedHolderName304 = p.name;
    cachedCaptureTime304 = now;
    cachedLastRewardTime304 = now;
    announcedMilestones304.clear();
    saveIslandStatus304();

    try {
      Manager.gI().chatKTG(0, p.name + " đã chiếm được Đảo Extol!", 5);
    } catch (Exception e) {
      GameLogger.error("ChiemDao.onPlayerCapture304: Error sending capture message", e);
    }

    // FIX 1: Khởi động timer nếu chưa chạy
    if (!isRewardTaskRunning()) {
      startRewardTask();
    }
  }

  // FIX 3: Thêm setPlayerTop để Map.java gọi được
  public static void setPlayerTop(int id, Player p) {
    if (id == 304) {
      onPlayerCapture304(p);
    }
  }

  public static void checkAndRewardLogin(Player p) {
    if (cachedHolderId304 == p.id) {
      long now = System.currentTimeMillis();
      long rewardMinutes = (now - cachedLastRewardTime304) / (60 * 1000);
      if (rewardMinutes >= 10) {
        int blocks = (int) (rewardMinutes / 10);
        long totalMinutes = (now - cachedCaptureTime304) / (60 * 1000);
        int extol = calculateExtol(totalMinutes) * blocks;
        try {
          p.update_extol(extol);
          Service.send_box_ThongBao_OK(p, "Bạn nhận được " + extol + " Extol tích lũy từ Đảo Extol!");
          cachedLastRewardTime304 = now;
          saveIslandStatus304();
        } catch (IOException e) {
          GameLogger.error("ChiemDao.checkAndRewardLogin: Error updating login reward", e);
        }
      }
    }
  }

  // ═══════════════════════════════════════════════════════
  // HELPER
  // ═══════════════════════════════════════════════════════

  private static int calculateXP(long totalMinutes) {
    if (totalMinutes < 30)
      return 100_000;
    if (totalMinutes < 60)
      return 250_000;
    if (totalMinutes < 120)
      return 500_000;
    if (totalMinutes < 240)
      return 800_000;
    return 1_000_000;
  }

  private static int calculateExtol(long totalMinutes) {
    if (totalMinutes < 5)
      return 5_000;
    if (totalMinutes < 10)
      return 10_000;
    if (totalMinutes < 15)
      return 15_000;
    if (totalMinutes < 20)
      return 20_000;
    return 25_000;
  }

  public static Clan getClanTop(int id) {
    if (id == 303)
      return conqueringClan;
    return null;
  }

  public static void setClanTop(int id, Clan clan) {
    if (id == 303) {
      if (conqueringClan == null || !conqueringClan.equals(clan)) {
        if (conqueringClan != null && !conqueringClan.members.isEmpty()) {
          try {
            conqueringClan.chat_on_board(
                conqueringClan.members.get(0).id,
                conqueringClan.members.get(0).name,
                "Đảo đã bị băng " + clan.name + " cướp mất!",
                -3);
          } catch (IOException e) {
            GameLogger.error("ChiemDao.setClanTop: Error sending loss message to old clan", e);
          }
        }
        conqueringClan = clan;
        startTime = System.currentTimeMillis();
        lastRewardTime = startTime;
        currentClanId = clan.id;
        announcedMilestones.clear();
      }
    }
  }

  private static String formatTime(long totalMinutes) {
    if (totalMinutes >= 60) {
      long h = totalMinutes / 60;
      long m = totalMinutes % 60;
      return m > 0 ? h + " giờ " + m + " phút" : h + " giờ";
    }
    return totalMinutes + " phút";
  }
}