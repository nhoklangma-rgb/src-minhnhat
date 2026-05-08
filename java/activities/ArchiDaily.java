package activities;

import client.MyArchiDaily;
import client.Player;
import core.GameLogger;
import core.Service;
import database.SQL;
import io.Message;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import template.GiftBox;
import template.ItemTemplate4;
import map.f;

public class ArchiDaily {

  public static List<ArchiDaily> ENTRY = new ArrayList<>();

  public String title, info;
  public int id;
  public long num;
  public short icon;
  public byte type;
  public String gift;

  public static void init() {
    try (Connection conn = SQL.gI().getCon();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("select * from `archiDaily`")) {
      while (rs.next()) {
        ArchiDaily temp = new ArchiDaily();
        temp.title = rs.getNString("title");
        temp.info = rs.getNString("info");
        temp.num = rs.getLong("num");
        temp.id = rs.getInt("id");
        temp.icon = rs.getShort("icon");
        temp.type = rs.getByte("type");
        temp.gift = rs.getString("gift");

        ENTRY.add(temp);
      }
    } catch (SQLException e) {
      GameLogger.error("ArchiDaily.init: SQL error loading ArchiDaily data", e);
    }
  }

  public static void show_table(Player p) throws IOException {
    Message m = new Message(37);
    m.writer().writeByte(0);
    m.writer().writeUTF("Nhiệm vụ hàng ngày");
    m.writer().writeByte(p.archiDaily.length);
    for (int i = 0; i < p.archiDaily.length; i++) {
      ArchiDaily template = ArchiDaily.getTemplate(p.archiDaily[i].id);
      if (p.archiDaily[i].type == 0 && p.archiDaily[i].num >= template.num) {
        p.archiDaily[i].type = 1;
      }
      m.writer().writeUTF(template.title);
      m.writer().writeUTF(template.info);
      m.writer().writeInt(f.setInteger(p.archiDaily[i].num));
      m.writer().writeInt(f.setInteger(template.num));
      m.writer().writeShort(template.icon);
      m.writer().writeByte(p.archiDaily[i].type);
    }
    p.addmsg(m);
    m.cleanup();
  }

  private static ArchiDaily getTemplate(int id) {
    for (int i = 0; i < ArchiDaily.ENTRY.size(); i++) {
      if (ArchiDaily.ENTRY.get(i).id == id) {
        return ArchiDaily.ENTRY.get(i);
      }
    }
    return null;
  }

  public static void getRd(Player p) {
    // Phân loại nhiệm vụ
    List<ArchiDaily> listNormal = new ArrayList<>();
    List<ArchiDaily> listSpec = new ArrayList<>();
    for (ArchiDaily entry : ENTRY) {
      if (entry.type == -1) {
        listSpec.add(entry); // Nhiệm vụ đặc biệt
      } else {
        listNormal.add(entry); // Nhiệm vụ thông thường
      }
    }
    listNormal.sort(Comparator.comparingInt(entry -> entry.id));
    listSpec.sort(Comparator.comparingInt(entry -> entry.id));
    int totalTasks = listNormal.size() + listSpec.size();
    p.archiDaily = new MyArchiDaily[totalTasks];
    int index = 0;
    for (ArchiDaily normalTask : listNormal) {
      p.archiDaily[index] = new MyArchiDaily();
      p.archiDaily[index].id = normalTask.id;
      p.archiDaily[index].type = normalTask.type;
      p.archiDaily[index].num = 0;
      index++;
    }
    for (ArchiDaily specTask : listSpec) {
      p.archiDaily[index] = new MyArchiDaily();
      p.archiDaily[index].id = specTask.id;
      p.archiDaily[index].type = specTask.type;
      p.archiDaily[index].num = 0;
      index++;
    }
  }

  public static void process(Player p, Message m2) throws IOException {
    byte type = m2.reader().readByte();
    byte index = m2.reader().readByte();

    if (type == 1 && p.archiDaily != null && p.archiDaily[index] != null) {
      ArchiDaily template = ArchiDaily.getTemplate(p.archiDaily[index].id);
      if (p.archiDaily[index].type == 1 && p.archiDaily[index].num >= template.num) {
        List<GiftBox> listGift = new ArrayList<>();
        try {
          if (template.gift != null && !template.gift.isEmpty()) {
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(template.gift);
            for (Object obj : arr) {
              JSONArray giftEntry = (JSONArray) obj;
              long itemId = (long) giftEntry.get(0);
              long quantity = (long) giftEntry.get(1);

              ItemTemplate4 it_temp4 = ItemTemplate4.get_it_by_id((int) itemId);
              if (it_temp4 != null) {
                GiftBox gb = new GiftBox();
                gb.id = it_temp4.id;
                gb.type = 4;
                gb.name = it_temp4.name;
                gb.icon = it_temp4.icon;
                gb.num = (int) quantity;
                gb.color = 0;
                listGift.add(gb);
              }
            }
          }
        } catch (Exception ex) {
          GameLogger.error("ArchiDaily.process: Error parsing gift JSON for task " + template.id, ex);
        }

        p.archiDaily[index].type = 2;
        ArchiDaily.show_table(p);
        Service.send_gift(p, 1, "Thành tích hằng ngày", template.title, listGift, true);
      }
    }
  }

}
