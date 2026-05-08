package activities;

import client.Player;
import core.GameLogger;
import core.Manager;
import core.Service;
import core.Util;
import database.SQL;
import io.Message;
import template.GiftBox;
import template.ItemTemplate4;
import template.ItemTemplate7;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DauGia {
  private static final int PRICE_STEP = 100;
  public static List<Player> listP = new ArrayList<>();
  public static final byte STATUS_ACTIVE = 0;
  public static final byte STATUS_ENDED = 1;
  public static final byte STATUS_CLAIMED = 2;

  public static void show_table(Player p) throws IOException {
    player_join(p);
    Message m = new Message(-91);
    m.writer().writeByte(0);
    List<AuctionData> auctions = getActiveAuctions();
    m.writer().writeByte(auctions.size());
    long now = System.currentTimeMillis();
    for (int i = 0; i < auctions.size(); i++) {
      AuctionData auction = auctions.get(i);
      long start = auction.startTime.getTime();
      long end = auction.endTime.getTime();
      int timeRemain = (now < start) ? -1 : (now > end ? 0 : (int) ((end - now) / 1000));
      m.writer().writeByte(i);
      m.writer().writeInt(auction.price);
      m.writer().writeInt(timeRemain);
      m.writer().writeInt(-1);
      m.writer().writeByte(auction.itemType);
      m.writer().writeUTF(getItemName(auction.itemType, auction.itemId));
      m.writer().writeShort(getItemIcon(auction.itemType, auction.itemId));
      m.writer().writeShort(auction.quantity);
      m.writer().writeByte(0);
      m.writer().writeByte(auction.playerId == p.id ? 1 : 0);
    }
    p.addmsg(m);
    m.cleanup();
  }

  public static void process(Player p, Message m) throws IOException {
    byte type = m.reader().readByte();
    byte id = -1;
    if (m.reader().available() > 0) {
      id = m.reader().readByte();
    }
    switch (type) {
      case 0: // Hiển thị
        if (id == -1)
          show_table(p);
        break;
      case 1: // Đấu giá
        if (id >= 0)
          placeBid(p, id);
        break;
      case 2: // Nhận thưởng
        if (id >= 0)
          claimReward(p);
        break;
      case 4: // Đóng bảng đấu giá
        if (id == -1) {
          player_leave(p);
        }
        break;
    }
  }

  private static void placeBid(Player p, int index) throws IOException {
    List<AuctionData> auctions = getActiveAuctions();
    if (index >= auctions.size()) {
      return;
    }
    AuctionData auction = auctions.get(index);
    synchronized (auction) {
      int newPrice = auction.price + PRICE_STEP;
      long now = System.currentTimeMillis();
      if (now > auction.endTime.getTime()) {
        return;
      }
      if (auction.playerId == p.id) {
        return;
      }
      if (p.get_bua() < newPrice) {
        Service.send_box_ThongBao_OK(p, "Bạn không đủ " + Util.number_format(newPrice) + " Búa");
        return;
      }
      try (Connection conn = SQL.gI().getCon()) {
        conn.setAutoCommit(false);
        if (auction.accountId > 0) {
          PreparedStatement ps1 = conn.prepareStatement(
              "UPDATE accounts SET bua = bua + ? WHERE id = ?");
          ps1.setInt(1, auction.price);
          ps1.setInt(2, auction.accountId);
          ps1.executeUpdate();
          ps1.close();
        }
        PreparedStatement ps3 = conn.prepareStatement(
            "UPDATE auction_items SET price_bua = ?, player_id = ?, account_id = ? WHERE id = ?");
        ps3.setInt(1, newPrice);
        ps3.setInt(2, p.id);
        ps3.setInt(3, p.get_conn_id());
        ps3.setInt(4, auction.id);
        ps3.executeUpdate();
        ps3.close();
        long timeRemain = auction.endTime.getTime() - now;
        if (timeRemain < 60_000) { // dưới 60 giây
          auction.endTime = new Timestamp(now + 60_000);
          PreparedStatement psTime = conn.prepareStatement(
              "UPDATE auction_items SET time_end = ? WHERE id = ?");
          psTime.setTimestamp(1, auction.endTime);
          psTime.setInt(2, auction.id);
          psTime.executeUpdate();
          psTime.close();
        }
        conn.commit();
        p.update_bua(-newPrice);
        p.update_money();
        broadcastUpdate(index, p, newPrice);
      } catch (Exception e) {
        GameLogger.error("DauGia.placeBid: Error placing bid for player " + p.name, e);
      }
    }
  }

  private static void claimReward(Player p) throws IOException {
    try (Connection conn = SQL.gI().getCon()) {
      String query = "SELECT * FROM auction_items WHERE player_id = ? AND status = ? LIMIT 1";
      PreparedStatement ps = conn.prepareStatement(query);
      ps.setInt(1, p.id);
      ps.setByte(2, STATUS_ENDED);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int auctionId = rs.getInt("id");
        byte itemType = rs.getByte("item_type");
        short itemId = rs.getShort("item_id");
        short quantity = rs.getShort("item_quantity");
        PreparedStatement ps2 = conn.prepareStatement(
            "UPDATE auction_items SET status = ? WHERE id = ?");
        ps2.setByte(1, STATUS_CLAIMED);
        ps2.setInt(2, auctionId);
        ps2.executeUpdate();
        ps2.close();
        List<GiftBox> gifts = new ArrayList<>();
        GiftBox gift = new GiftBox();
        if (itemType == 4) {
          ItemTemplate4 item = ItemTemplate4.get_it_by_id(itemId);
          gift.id = item.id;
          gift.type = 4;
          gift.name = item.name;
          gift.icon = item.icon;
        } else if (itemType == 7) {
          ItemTemplate7 item = ItemTemplate7.get_it_by_id(itemId);
          gift.id = item.id;
          gift.type = 7;
          gift.name = item.name;
          gift.icon = item.icon;
        }
        gift.num = quantity;
        gift.color = 0;
        gifts.add(gift);
        Service.send_gift(p, 0, "Phần Thưởng Đấu Giá", "", gifts, true);
      }
      rs.close();
      ps.close();
    } catch (Exception e) {
      GameLogger.error("DauGia.claimReward: Error claiming reward for player " + p.name, e);
    }
  }

  public static void createAuction(byte itemType, short itemId, short quantity, int startPrice) {
    try (Connection conn = SQL.gI().getCon()) {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime start = now.withHour(19).withMinute(0).withSecond(0).withNano(0);
      if (now.getHour() >= 19) {
        start = start.plusDays(1);
      }
      LocalDateTime end = start.withHour(20).withMinute(30).withSecond(0).withNano(0);
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO auction_items (item_type, item_id, item_quantity, price_bua, time_start, time_end, status) VALUES (?, ?, ?, ?, ?, ?, ?)");
      ps.setByte(1, itemType);
      ps.setShort(2, itemId);
      ps.setShort(3, quantity);
      ps.setInt(4, startPrice);
      ps.setTimestamp(5, Timestamp.valueOf(start));
      ps.setTimestamp(6, Timestamp.valueOf(end));
      ps.setByte(7, STATUS_ACTIVE);
      ps.executeUpdate();
      ps.close();
    } catch (Exception e) {
      GameLogger.error("DauGia.createAuction: Error creating auction", e);
    }
  }

  public static void createDaily() {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement psCheck = conn.prepareStatement(
          "SELECT time_start FROM auction_items ORDER BY id DESC LIMIT 1");
      ResultSet rs = psCheck.executeQuery();
      LocalDate lastAuctionDate = null;
      if (rs.next()) {
        lastAuctionDate = rs.getTimestamp("time_start").toLocalDateTime().toLocalDate();
      }
      rs.close();
      psCheck.close();
      LocalDate today = LocalDate.now();
      boolean canCreate = false;
      if (lastAuctionDate == null) {
        canCreate = true;
      } else if (lastAuctionDate.plusDays(2).isBefore(today) || lastAuctionDate.plusDays(2).isEqual(today)) {
        canCreate = true;
      }
      if (!canCreate) {
        return;
      }
      createAuction((byte) 4, (short) 427, (short) 1, 100);
      createAuction((byte) 4, (short) 1026, (short) 1, 100);
      createAuction((byte) 4, (short) 1044, (short) 1, 100);
    } catch (Exception e) {
      GameLogger.error("DauGia.createDaily: Error creating daily auctions", e);
    }
  }

  public static void update() {
    try (Connection conn = SQL.gI().getCon()) {
      PreparedStatement ps = conn.prepareStatement(
          "UPDATE auction_items SET status = ? WHERE status = ? AND time_end < NOW()");
      ps.setByte(1, STATUS_ENDED);
      ps.setByte(2, STATUS_ACTIVE);
      ps.executeUpdate();
      ps.close();
    } catch (Exception e) {
      GameLogger.error("DauGia.update: Error updating auction status", e);
    }
  }

  private static List<AuctionData> getActiveAuctions() {
    List<AuctionData> list = new ArrayList<>();
    String sql = "SELECT id, price_bua, player_id, account_id, item_type, item_id, item_quantity, time_start, time_end "
        + "FROM auction_items " + "WHERE status = CASE "
        + "  WHEN EXISTS (SELECT 1 FROM auction_items WHERE status = 0) THEN 0 " +
        "  ELSE 1 " + "END " + "ORDER BY id ASC";
    try (Connection conn = SQL.gI().getCon();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        AuctionData ad = new AuctionData();
        ad.id = rs.getInt("id");
        ad.price = rs.getInt("price_bua");
        ad.playerId = rs.getInt("player_id");
        ad.accountId = rs.getInt("account_id");
        ad.itemType = rs.getByte("item_type");
        ad.itemId = rs.getShort("item_id");
        ad.quantity = rs.getShort("item_quantity");
        ad.startTime = rs.getTimestamp("time_start");
        ad.endTime = rs.getTimestamp("time_end");
        list.add(ad);
      }
    } catch (SQLException e) {
      GameLogger.error("DauGia.getActiveAuctions: SQL error loading active auctions", e);
    }
    return list;
  }

  private static void player_join(Player p) {
    if (!listP.contains(p)) {
      listP.add(p);
    }
  }

  public static void player_leave(Player p) {
    listP.remove(p);
  }

  private static void broadcastUpdate(int index, Player bidder, int price) {
    try {
      Message m = new Message(-91);
      m.writer().writeByte(1);
      m.writer().writeByte(index);
      m.writer().writeShort(bidder.index_map);
      m.writer().writeInt(price);
      List<AuctionData> auctions = getActiveAuctions();
      int timeRemain = 0;
      if (index < auctions.size()) {
        AuctionData auction = auctions.get(index);
        timeRemain = (int) ((auction.endTime.getTime() - System.currentTimeMillis()) / 1000);
        if (timeRemain < 0)
          timeRemain = 0;
      }
      m.writer().writeInt(timeRemain);
      for (Player p0 : listP) {
        if (p0 != null && p0.conn != null) {
          p0.update_money();
          p0.addmsg(m);
        }
      }
      m.cleanup();
    } catch (Exception e) {
      GameLogger.error("DauGia.broadcastUpdate: Error broadcasting auction update", e);
    }
  }

  private static class AuctionData {
    int id, price, playerId, accountId;
    byte itemType;
    short itemId, quantity;
    Timestamp startTime, endTime;
  }

  private static String getItemName(byte type, short id) {
    if (type == 4) {
      ItemTemplate4 item4 = ItemTemplate4.get_it_by_id(id);
      return item4 != null ? item4.name : "";
    } else if (type == 7) {
      ItemTemplate7 item7 = ItemTemplate7.get_it_by_id(id);
      return item7 != null ? item7.name : "";
    }
    return "";
  }

  private static short getItemIcon(byte type, short id) {
    if (type == 4) {
      ItemTemplate4 item4 = ItemTemplate4.get_it_by_id(id);
      return item4 != null ? item4.icon : 0;
    } else if (type == 7) {
      ItemTemplate7 item7 = ItemTemplate7.get_it_by_id(id);
      return item7 != null ? item7.icon : 0;
    }
    return 0;
  }

}