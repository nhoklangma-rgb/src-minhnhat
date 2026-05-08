package activities;

import database.SQL;
import template.GiftBox;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VongXoayMayManManager implements Runnable {

  private static final int MAX_TIME = 180; // 3 phút, giảm 10 lần so với 30 phút cũ
  private static VongXoayMayManManager instance;
  private int timeLeft = MAX_TIME;
  private Thread thread;
  private boolean running = false;

  public static VongXoayMayManManager gI() {
    if (instance == null) {
      instance = new VongXoayMayManManager();
    }
    return instance;
  }

  public void start() {
    if (running)
      return;
    running = true;
    thread = new Thread(this, "VongXoayMayMan-Thread");
    thread.start();
    core.GameLogger.info("[VONG XOAY] Thread started.");
  }

  public void stop() {
    running = false;
    if (thread != null) {
      thread.interrupt();
    }
    core.GameLogger.info("[VONG XOAY] Thread stopped.");
  }

  public int getTimeLeft() {
    return timeLeft;
  }

  @Override
  public void run() {
    int[] timeMarks = { MAX_TIME - 10, 30, 6 };
    String[] messages = {
        "Vòng Xoay May Mắn đã được bắt đầu - Giải thưởng lên tới %s Extol",
        "Vòng Xoay May Mắn còn 30 giây - Giải thưởng hiện tại: %s Extol",
        "Vòng Xoay May Mắn còn 6 giây - Giải thưởng hiện tại: %s Extol"
    };
    while (running) {
      try {
        Thread.sleep(1000);
        timeLeft--;
        for (int i = 0; i < timeMarks.length; i++) {
          if (timeLeft == timeMarks[i]) {
            if (timeMarks[i] == MAX_TIME - 10) {
              updateExtolSpinData("DRO", 1_000_000L); // Giảm 100 lần (100tr -> 1tr)
            }
            Manager.gI().chatKTG(0, String.format(messages[i], Util.number_format(getTongExtol())), 5);
          }
        }
        if (timeLeft <= 0) {
          timeLeft = MAX_TIME;
          selectWinner();
        }
      } catch (InterruptedException e) {
        running = false;
        core.GameLogger.warn("[VongXoayMayManManager] Thread bị interrupt, dừng vòng lặp");
      } catch (Exception e) {
        core.GameLogger.error("VongXoayMayManManager.run: Error in main loop", e);
      }
    }
  }

  public static void showSpinMenu(Player p) throws IOException {
    int timeLeft = VongXoayMayManManager.gI().getTimeLeft();
    int soNguoi = VongXoayMayManManager.gI().getSoNguoi();
    long tongExtol = VongXoayMayManManager.gI().getTongExtol();
    long extolCaNhan = VongXoayMayManManager.gI().getExtolOfPlayer(p.name);
    double tile = 0;
    if (tongExtol > 0) {
      tile = (extolCaNhan * 100.0) / tongExtol;
    }
    String tileStr = String.format("%.1f", tile) + "%";
    String logStr = "";
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `log` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String json = rs.getString("log");
        if (json != null && !json.isEmpty()) {
          JSONArray logs = (JSONArray) new JSONParser().parse(json);
          if (!logs.isEmpty()) {
            JSONArray lastLog = (JSONArray) logs.get(logs.size() - 1);
            String winnerName = lastLog.get(0).toString();
            long winnerBet = (long) lastLog.get(1);
            long winnerWin = (long) lastLog.get(2);
            logStr = winnerName + " vừa chiến thắng\nvới " + Util.number_format(winnerBet) + " Extol \nnhận "
                + Util.number_format(winnerWin) + " Extol";
          }
        }
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.showSpinMenu: Error loading log data", e);
    }

    String content = "Vòng Xoay May Mắn"
        + "\n________________\n"
        + timeLeft + "s"
        + "\nTỉ lệ thắng " + tileStr
        + "\nTổng " + Util.number_format(tongExtol) + " Extol"
        + "\n " + soNguoi + " người đang tham gia"
        + "\nBạn đặt " + Util.number_format(extolCaNhan) + " Extol"
        + "\n________________\n"
        + (logStr.isEmpty() ? "Chưa có người chiến thắng" : logStr);

    Service.send_box_yesno(p, 84, "Thông báo",
        content,
        new String[] { "Cập Nhật", "Tham Gia", "Nhận Extol", "Đóng" },
        new byte[] { -1, -1, -1, -1 });
  }

  public static void updateExtolSpinData(String playerName, long extol) {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement psSelect = conn.prepareStatement("SELECT `data` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = psSelect.executeQuery();
      JSONArray spinData = new JSONArray();
      boolean found = false;
      if (rs.next()) {
        String dataStr = rs.getString("data");
        if (dataStr != null && !dataStr.isEmpty()) {
          spinData = (JSONArray) new JSONParser().parse(dataStr);
          for (Object obj : spinData) {
            JSONArray entry = (JSONArray) obj;
            if (entry.get(0).equals(playerName)) {
              long currentExtol = ((Number) entry.get(1)).longValue();
              entry.set(1, currentExtol + extol);
              found = true;
              break;
            }
          }
        }
      }
      if (!found) {
        JSONArray newEntry = new JSONArray();
        newEntry.add(playerName);
        newEntry.add(extol);
        spinData.add(newEntry);
      }
      PreparedStatement psUpdate = conn.prepareStatement("UPDATE `vongxoaymayman` SET `data` = ? WHERE `id` = 1");
      psUpdate.setString(1, spinData.toJSONString());
      psUpdate.executeUpdate();
      psSelect.close();
      psUpdate.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.updateExtolSpinData: Error updating spin data", e);
    }
  }

  private void selectWinner() {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `data`, `gift`, `log` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String dataStr = rs.getString("data");
        String giftStr = rs.getString("gift");
        String logStr = rs.getString("log");
        if (dataStr != null && !dataStr.isEmpty()) {
          JSONArray data = (JSONArray) new JSONParser().parse(dataStr);
          if (data.isEmpty())
            return;
          long tongExtol = 0;
          for (Object obj : data) {
            JSONArray entry = (JSONArray) obj;
            tongExtol += (long) entry.get(1);
          }
          if (tongExtol == 0)
            return;
          double r = Math.random() * tongExtol;
          long sum = 0;
          String winnerName = "";
          long winnerBet = 0;
          for (Object obj : data) {
            JSONArray entry = (JSONArray) obj;
            String name = (String) entry.get(0);
            long bet = (long) entry.get(1);
            sum += bet;
            if (r <= sum) {
              winnerName = name;
              winnerBet = bet;
              break;
            }
          }
          boolean isDRO = "DRO".equals(winnerName);
          if (isDRO) {
            long playerExtol = 0;
            long currentDroExtol = 0;
            for (Object obj : data) {
              JSONArray entry = (JSONArray) obj;
              String name = (String) entry.get(0);
              long bet = (long) entry.get(1);
              if ("DRO".equals(name)) {
                currentDroExtol = bet;
              } else {
                playerExtol += bet;
              }
            }
            long newDroExtol = currentDroExtol + playerExtol;
            long MAX_EXTOL = 100_000_000L; // Giảm 100 lần (10 tỷ -> 100tr)
            if (newDroExtol > MAX_EXTOL) {
              newDroExtol = MAX_EXTOL;
            }
            JSONArray newData = new JSONArray();
            JSONArray droEntry = new JSONArray();
            droEntry.add("DRO");
            droEntry.add(newDroExtol);
            newData.add(droEntry);
            if (playerExtol > 0 || newDroExtol != currentDroExtol) {
              PreparedStatement psUpdate = conn
                  .prepareStatement("UPDATE `vongxoaymayman` SET `data` = ? WHERE `id` = 1");
              psUpdate.setString(1, newData.toJSONString());
              psUpdate.executeUpdate();
              psUpdate.close();
            }
          } else {
            JSONArray giftList = (giftStr == null || giftStr.isEmpty()) ? new JSONArray()
                : (JSONArray) new JSONParser().parse(giftStr);
            JSONArray winnerGift = null;
            for (Object obj : giftList) {
              JSONArray entry = (JSONArray) obj;
              if (entry.get(0).toString().equalsIgnoreCase(winnerName)) {
                winnerGift = entry;
                break;
              }
            }
            if (winnerGift == null) {
              winnerGift = new JSONArray();
              winnerGift.add(winnerName);
              winnerGift.add(new JSONArray());
              giftList.add(winnerGift);
            }
            JSONArray listGift = (JSONArray) winnerGift.get(1);
            boolean merged = false;
            for (Object g : listGift) {
              JSONArray gift = (JSONArray) g;
              if ((long) gift.get(0) == 2 && (long) gift.get(1) == 4) {
                long current = (long) gift.get(4);
                gift.set(4, current + tongExtol);
                merged = true;
                break;
              }
            }
            if (!merged) {
              JSONArray extolGift = new JSONArray();
              extolGift.add(2L); // ID extol
              extolGift.add(4L); // type 4
              extolGift.add("extol");
              extolGift.add(170L); // icon extol
              extolGift.add(tongExtol); // số lượng
              extolGift.add(0L); // color
              listGift.add(extolGift);
            }
            JSONArray logList = (logStr == null || logStr.isEmpty()) ? new JSONArray()
                : (JSONArray) new JSONParser().parse(logStr);
            JSONArray logEntry = new JSONArray();
            logEntry.add(winnerName);
            logEntry.add(winnerBet);
            logEntry.add(tongExtol);
            String timeNow = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy"));
            logEntry.add(timeNow);
            logList.add(logEntry);
            PreparedStatement psUpdate = conn
                .prepareStatement("UPDATE `vongxoaymayman` SET `gift` = ?, `log` = ?, `data` = '[]' WHERE `id` = 1");
            psUpdate.setString(1, giftList.toJSONString());
            psUpdate.setString(2, logList.toJSONString());
            psUpdate.executeUpdate();
            psUpdate.close();
            Manager.gI().chatKTG(0, "Chúc mừng " + winnerName + " đã chiến thắng " + Util.number_format(tongExtol)
                + " Extol trong Vòng Xoay May Mắn", 5);
          }
        }
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.selectWinner: Error selecting winner", e);
    }
  }

  public static void nhanQuaVongXoay(Player p) {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `gift` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (!rs.next())
        return;
      String giftStr = rs.getString("gift");
      if (giftStr == null || giftStr.isEmpty())
        return;
      JSONArray giftList = (JSONArray) new JSONParser().parse(giftStr);
      JSONArray newGiftList = new JSONArray();
      GiftBox giftExtol = null;
      for (Object obj : giftList) {
        JSONArray entry = (JSONArray) obj;
        String name = (String) entry.get(0);
        if (!name.equals(p.name)) {
          newGiftList.add(entry);
          continue;
        }
        JSONArray gifts = (JSONArray) entry.get(1);
        for (Object gObj : gifts) {
          JSONArray g = (JSONArray) gObj;
          int id = ((Long) g.get(0)).intValue();
          byte type = ((Long) g.get(1)).byteValue();
          if (type == 4 && id == 2) { // extol
            long amount = (Long) g.get(4);
            long toSend = Math.min(amount, 2_000_000_000L);
            giftExtol = new GiftBox();
            giftExtol.id = (short) id;
            giftExtol.type = type;
            giftExtol.name = (String) g.get(2);
            giftExtol.icon = ((Long) g.get(3)).shortValue();
            giftExtol.num = (int) toSend;
            giftExtol.color = ((Long) g.get(5)).byteValue();
            long remain = amount - toSend;
            if (remain > 0) {
              g.set(4, (long) remain);
              JSONArray newEntry = new JSONArray();
              newEntry.add(name);
              JSONArray newGifts = new JSONArray();
              newGifts.add(g);
              newEntry.add(newGifts);
              newGiftList.add(newEntry);
            }
            break;
          }
        }
        break;
      }
      rs.close();
      ps.close();
      PreparedStatement update = conn.prepareStatement("UPDATE `vongxoaymayman` SET `gift` = ? WHERE `id` = 1");
      update.setString(1, newGiftList.toJSONString());
      update.executeUpdate();
      update.close();
      if (giftExtol != null) {
        List<GiftBox> list = new ArrayList<>();
        list.add(giftExtol);
        Service.send_gift(p, 1, "Vòng Xoay May Mắn", "", list, true);
      } else {
        Service.send_box_ThongBao_OK(p, "Bạn không có phần thưởng nào.");
      }
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.handleReceiveGift: Error receiving gift", e);
    }
  }

  public int getSoNguoi() {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `data` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String dataStr = rs.getString("data");
        if (dataStr != null && !dataStr.isEmpty()) {
          JSONArray data = (JSONArray) new JSONParser().parse(dataStr);
          return data.size();
        }
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.getSoNguoi: Error getting number of players", e);
    }
    return 0;
  }

  public long getTongExtol() {
    long total = 0;
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `data` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String dataStr = rs.getString("data");
        if (dataStr != null && !dataStr.isEmpty()) {
          JSONArray data = (JSONArray) new JSONParser().parse(dataStr);
          for (Object obj : data) {
            List entry = (List) obj;
            long value = Long.parseLong(entry.get(1).toString());
            total += value;
          }
        }
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.getTongExtol: Error getting total extol", e);
    }
    return total;
  }

  public long getExtolOfPlayer(String playerName) {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT `data` FROM `vongxoaymayman` WHERE `id` = 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String dataStr = rs.getString("data");
        if (dataStr != null && !dataStr.isEmpty()) {
          JSONArray data = (JSONArray) new JSONParser().parse(dataStr);
          for (Object obj : data) {
            JSONArray entry = (JSONArray) obj;
            if (entry.get(0).equals(playerName)) {
              return Long.parseLong(entry.get(1).toString());
            }
          }
        }
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      core.GameLogger.error("VongXoayMayManManager.getExtolOfPlayer: Error getting player's extol", e);
    }
    return 0;
  }

}
