package activities;

import client.Player;
import core.GameLogger;
import core.Service;
import io.Message;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import template.GiftBox;
import template.ItemFashion;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;

public class DiemDanh {
  public static final DailyGift[] DAILY_GIFT = new DailyGift[] {
      new DailyGift((byte) 3, (short) 11015, (short) 1),
      new DailyGift((byte) 4, (short) 349, (short) 5),
      new DailyGift((byte) 4, (short) 1039, (short) 5),
      new DailyGift((byte) 4, (short) 232, (short) 20),
      new DailyGift((byte) 4, (short) 226, (short) 10),
      new DailyGift((byte) 4, (short) 89, (short) 20),
      new DailyGift((byte) 4, (short) 1018, (short) 10),
      new DailyGift((byte) 4, (short) 349, (short) 10),
      new DailyGift((byte) 4, (short) 1039, (short) 10),
      new DailyGift((byte) 4, (short) 1052, (short) 10),
      new DailyGift((byte) 4, (short) 226, (short) 20),
      new DailyGift((byte) 7, (short) 10, (short) 30),
      new DailyGift((byte) 4, (short) 1018, (short) 20),
      new DailyGift((byte) 4, (short) 89, (short) 50),
      new DailyGift((byte) 4, (short) 232, (short) 50),
      new DailyGift((byte) 4, (short) 349, (short) 20),
      new DailyGift((byte) 4, (short) 1039, (short) 20),
      new DailyGift((byte) 4, (short) 226, (short) 30),
      new DailyGift((byte) 4, (short) 457, (short) 30),
      new DailyGift((byte) 7, (short) 10, (short) 50),
      new DailyGift((byte) 4, (short) 1053, (short) 10),
      new DailyGift((byte) 4, (short) 89, (short) 80),
      new DailyGift((byte) 4, (short) 349, (short) 50),
      new DailyGift((byte) 4, (short) 1039, (short) 50),
      new DailyGift((byte) 4, (short) 367, (short) 20),
      new DailyGift((byte) 4, (short) 324, (short) 1),
      new DailyGift((byte) 4, (short) 325, (short) 1),
      new DailyGift((byte) 4, (short) 326, (short) 1),
      new DailyGift((byte) 4, (short) 1054, (short) 10),
      new DailyGift((byte) 3, (short) 11082, (short) 1)
      // thêm
  };

  public static class DailyGift {
    public byte type;
    public short id;
    public short quantity;

    public DailyGift(byte type, short id, short quantity) {
      this.type = type;
      this.id = id;
      this.quantity = quantity;
    }
  }

  public static class DailyCheckin {
    public int[] dailyData;
    public LocalDate lastCheckinDate;
    public int nextDayIndex;
    public LocalDate startDate;

    public DailyCheckin(int[] data, LocalDate lastDate, int nextIndex, LocalDate startDate) {
      this.dailyData = data;
      this.lastCheckinDate = lastDate;
      this.nextDayIndex = nextIndex;
      this.startDate = startDate;
    }
  }

  private static int getMaxDay() {
    return DAILY_GIFT.length;
  }

  public static DailyCheckin getOrCreate(Player player) {
    int maxDay = DAILY_GIFT.length; // số ngày thực tế
    try (Connection conn = database.SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM daily_checkin WHERE player_id=?");
      ps.setLong(1, player.id);
      ResultSet rs = ps.executeQuery();
      LocalDate today = LocalDate.now();
      if (rs.next()) {
        String json = rs.getString("daily_data");
        JSONArray arr = (JSONArray) new JSONParser().parse(json);
        int[] dailyData = new int[maxDay];
        for (int i = 0; i < maxDay; i++) {
          dailyData[i] = i < arr.size() ? ((Long) arr.get(i)).intValue() : 0;
        }
        LocalDate lastDate = rs.getDate("last_checkin_date").toLocalDate();
        int nextIndex = rs.getInt("next_day_index");
        LocalDate startDate = rs.getDate("start_date").toLocalDate();
        if (nextIndex > maxDay)
          nextIndex = maxDay;
        JSONArray newArr = new JSONArray();
        for (int v : dailyData)
          newArr.add(v);
        PreparedStatement ps2 = conn.prepareStatement(
            "UPDATE daily_checkin SET daily_data=?, next_day_index=? WHERE player_id=?");
        ps2.setString(1, newArr.toJSONString());
        ps2.setInt(2, nextIndex);
        ps2.setLong(3, player.id);
        ps2.executeUpdate();
        return new DailyCheckin(dailyData, lastDate, nextIndex, startDate);
      } else {
        int[] dailyData = new int[maxDay];
        for (int i = 0; i < maxDay; i++)
          dailyData[i] = 0;
        DailyCheckin dd = new DailyCheckin(dailyData, today.minusDays(1), 0, today);
        JSONArray arr = new JSONArray();
        for (int i = 0; i < maxDay; i++)
          arr.add(0);
        ps = conn.prepareStatement(
            "INSERT INTO daily_checkin(player_id,daily_data,start_date,last_checkin_date,next_day_index) VALUES(?,?,?,?,?)");
        ps.setLong(1, player.id);
        ps.setString(2, arr.toJSONString());
        ps.setDate(3, Date.valueOf(dd.startDate));
        ps.setDate(4, Date.valueOf(dd.lastCheckinDate));
        ps.setInt(5, dd.nextDayIndex);
        ps.executeUpdate();
        return dd;
      }
    } catch (Exception e) {
      GameLogger.error("DiemDanh.getOrCreate: Error getting or creating daily checkin for player " + player.name, e);
      return null;
    }
  }

  public static void showTable(Player player) {
    int MAX_DAY = getMaxDay();
    try {
      DailyCheckin dd = getOrCreate(player);
      Message m = new Message(60);
      m.writer().writeUTF("Quà Điểm Danh");
      m.writer().writeUTF("Nhận Quà");
      m.writer().writeByte(MAX_DAY);

      LocalDate today = LocalDate.now();
      for (int i = 0; i < MAX_DAY; i++) {
        byte typeGiftDaily = 0;
        if (i < dd.nextDayIndex)
          typeGiftDaily = 2;
        else if (i == dd.nextDayIndex && !dd.lastCheckinDate.isEqual(today))
          typeGiftDaily = 1;

        DailyGift gift = DAILY_GIFT[i];
        String itemName = "ngày " + (i + 1);
        short iconId = getItemIcon(gift);
        short quantity = gift.quantity;
        byte type = gift.type;
        byte color = 0;

        m.writer().writeByte(type);
        m.writer().writeUTF(itemName);
        m.writer().writeShort(iconId);
        m.writer().writeByte(color);
        m.writer().writeShort(quantity);
        m.writer().writeByte(typeGiftDaily);
      }

      player.addmsg(m);
      m.cleanup();
    } catch (Exception e) {
      GameLogger.error("DiemDanh.showTable: Error showing table for player " + player.name, e);
    }
  }

  public static void process(Player player, Message msg) {
    int MAX_DAY = getMaxDay();
    try {
      byte actionType = msg.reader().readByte();
      if (actionType != 0)
        return;

      DailyCheckin dd = getOrCreate(player);
      LocalDate today = LocalDate.now();
      if (dd.lastCheckinDate.isEqual(today) || dd.nextDayIndex >= MAX_DAY)
        return;

      dd.dailyData[dd.nextDayIndex] = 2;
      dd.lastCheckinDate = today;
      dd.nextDayIndex += 1;

      try (Connection conn = database.SQL.gI().getCon()) {
        JSONArray arr = new JSONArray();
        for (int v : dd.dailyData)
          arr.add(v);
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE daily_checkin SET daily_data=?, last_checkin_date=?, next_day_index=? WHERE player_id=?");
        ps.setString(1, arr.toJSONString());
        ps.setDate(2, Date.valueOf(dd.lastCheckinDate));
        ps.setInt(3, dd.nextDayIndex);
        ps.setLong(4, player.id);
        ps.executeUpdate();
      }

      DailyGift gift = DAILY_GIFT[dd.nextDayIndex - 1];
      List<GiftBox> gifts = new ArrayList<>();
      GiftBox gb = new GiftBox();
      gb.id = gift.id;
      gb.type = gift.type;
      gb.name = getItemName(gift);
      gb.icon = getItemIcon(gift);
      gb.num = gift.quantity;
      gb.color = 0;
      gifts.add(gb);
      Service.send_gift(player, 1, "Quà điểm danh", "", gifts, true);

    } catch (Exception e) {
      GameLogger.error("DiemDanh.process: Error processing daily checkin for player " + player.name, e);
    }
  }

  private static String getItemName(DailyGift gift) {
    switch (gift.type) {
      case 4:
        return ItemTemplate4.get_it_by_id(gift.id).name;
      case 3:
        return ItemTemplate3.get_it_by_id(gift.id).name;
      case 7:
        return ItemTemplate7.get_it_by_id(gift.id).name;
      case 105:
        return ItemFashion.get_item(gift.id).name;
      default:
        return "Item";
    }
  }

  private static short getItemIcon(DailyGift gift) {
    switch (gift.type) {
      case 4:
        return ItemTemplate4.get_it_by_id(gift.id).icon;
      case 3:
        return ItemTemplate3.get_it_by_id(gift.id).icon;
      case 7:
        return ItemTemplate7.get_it_by_id(gift.id).icon;
      case 105:
        return ItemFashion.get_item(gift.id).idIcon;
      default:
        return 0;
    }
  }
}
