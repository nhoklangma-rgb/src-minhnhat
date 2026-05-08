package activities;

import client.Player;
import core.Service;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.GiftBox;
import template.ItemFashion;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ListTichNap {

  public final static List<ListTichNap> ENTRY;

  static {
    ENTRY = new ArrayList<>();
    load();
  }

  public static void load() {
    ENTRY.clear();
    try {
      byte[] ab = core.Util.loadfile("data/tich_nap.json");
      if (ab == null) {
        core.GameLogger.info("data/tich_nap.json not found!");
        return;
      }
      String data = new String(ab, "UTF-8");
      JSONArray arr = (JSONArray) JSONValue.parse(data);
      for (int i = 0; i < arr.size(); i++) {
        JSONObject job = (JSONObject) arr.get(i);
        ListTichNap t = new ListTichNap();
        t.num = Integer.parseInt(job.get("num").toString());
        JSONArray items = (JSONArray) job.get("items");
        t.cat = new byte[items.size()];
        t.id = new short[items.size()];
        t.quant = new short[items.size()];
        for (int j = 0; j < items.size(); j++) {
          JSONObject item = (JSONObject) items.get(j);
          t.cat[j] = Byte.parseByte(item.get("cat").toString());
          t.id[j] = Short.parseShort(item.get("id").toString());
          t.quant[j] = Short.parseShort(item.get("quant").toString());
        }
        ENTRY.add(t);
      }
      core.GameLogger.info("Load " + ENTRY.size() + " mốc tích nạp");
    } catch (Exception e) {
      core.GameLogger.error("ListTichNap.load: Error loading tich nap data", e);
    }
  }

  public byte[] cat;
  public short[] id;
  public short[] quant;
  public int num;

  public static void showTable(Player p) throws IOException {
    int tongnap = p.getTongNap();
    Message m = new Message(-90);
    m.writer().writeByte(0);
    m.writer().writeInt(tongnap);
    m.writer().writeByte(ENTRY.size());
    for (int i = 0; i < ENTRY.size(); i++) {
      ListTichNap t = ENTRY.get(i);
      m.writer().writeByte(i);
      m.writer().writeInt(t.num);
      m.writer().writeByte(p.tichNapCheck[i] == 1 ? 2 : (tongnap >= t.num ? 1 : 0));
      m.writer().writeShort(t.cat.length);
      for (int j = 0; j < t.cat.length; j++) {
        if (t.cat[j] == 4) {
          ItemTemplate4 itTemp4Select = ItemTemplate4.get_it_by_id(t.id[j]);
          if (t.id[j] == -10) {
            int level = p.level / 10;
            if (level == 0)
              level = 1;
            itTemp4Select = ItemTemplate4.get_it_by_id(level + 121);
          }
          if (itTemp4Select.id == 0) {
            if (t.quant[j] == 1000) {
              m.writer().writeUTF("b " + itTemp4Select.name);
            } else {
              m.writer().writeUTF("m " + itTemp4Select.name);
            }
          } else {
            m.writer().writeUTF(itTemp4Select.name);
          }
          m.writer().writeByte(t.cat[j]);
          m.writer().writeShort(itTemp4Select.icon);
          if (itTemp4Select.id == 0 && t.quant[j] == 1000) {
            m.writer().writeShort(1);
          } else {
            m.writer().writeShort(t.quant[j]);
          }
          m.writer().writeByte(0);
        } else if (t.cat[j] == 105) {
          ItemFashion itemFashion = ItemFashion.get_item(t.id[j]);
          m.writer().writeUTF(itemFashion.name);
          m.writer().writeByte(t.cat[j]);
          m.writer().writeShort(itemFashion.idIcon);
          m.writer().writeShort(t.quant[j]);
          m.writer().writeByte(0);
        } else if (t.cat[j] == 7) {
          ItemTemplate7 itTemp7Select = ItemTemplate7.get_it_by_id(t.id[j]);
          m.writer().writeUTF(itTemp7Select.name);
          m.writer().writeByte(t.cat[j]);
          m.writer().writeShort(itTemp7Select.icon);
          m.writer().writeShort(t.quant[j]);
          m.writer().writeByte(0);
        } else if (t.cat[j] == 3) {
          ItemTemplate3 itTemp3Select = ItemTemplate3.get_it_by_id(t.id[j]);
          m.writer().writeUTF(itTemp3Select.name);
          m.writer().writeByte(t.cat[j]);
          m.writer().writeShort(itTemp3Select.icon);
          m.writer().writeShort(t.quant[j]);
          m.writer().writeByte(0);
        } else {
          return;
        }
      }
    }
    p.addmsg(m);
    m.cleanup();
  }

  public static void process(Player p, Message m2) throws IOException {
    byte type = m2.reader().readByte();
    byte id = m2.reader().readByte();
    core.GameLogger.info(type + " " + id);
    if (type == 1 && id < p.tichNapCheck.length && id < ENTRY.size() && p.tichNapCheck[id] != 1) {
      List<GiftBox> list = new ArrayList<>();
      ListTichNap listGet = ENTRY.get(id);
      for (int i = 0; i < listGet.cat.length; i++) {
        if (listGet.cat[i] == 4) {
          ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(listGet.id[i]);
          if (listGet.id[i] == -10) {
            int level = p.level / 10;
            if (level == 0)
              level = 1;
            itemTemplate4 = ItemTemplate4.get_it_by_id(level + 121);
          }
          if (itemTemplate4 != null) {
            GiftBox gb4 = new GiftBox();
            gb4.id = itemTemplate4.id;
            gb4.type = 4;
            gb4.name = itemTemplate4.name;
            gb4.icon = itemTemplate4.icon;
            gb4.num = listGet.quant[i];
            if (gb4.id == 0) {
              gb4.num *= 1_000_000;
            }
            gb4.color = 0;
            list.add(gb4);
          }
        } else if (listGet.cat[i] == 7) {
          ItemTemplate7 itemTemplate7 = ItemTemplate7.get_it_by_id(listGet.id[i]);
          if (itemTemplate7 != null) {
            GiftBox gb4 = new GiftBox();
            gb4.id = itemTemplate7.id;
            gb4.type = 7;
            gb4.name = itemTemplate7.name;
            gb4.icon = itemTemplate7.icon;
            gb4.num = listGet.quant[i];
            gb4.color = 0;
            list.add(gb4);
          }
        } else if (listGet.cat[i] == 105) {
          ItemFashion itemFashion = ItemFashion.get_item(listGet.id[i]);
          if (itemFashion != null) {
            GiftBox gb4 = new GiftBox();
            gb4.id = itemFashion.ID;
            gb4.type = 105;
            gb4.name = itemFashion.name;
            gb4.icon = itemFashion.idIcon;
            gb4.num = listGet.quant[i];
            gb4.color = 0;
            list.add(gb4);
          }
        } else if (listGet.cat[i] == 3) {
          ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(listGet.id[i]);
          if (itemTemplate3 != null) {
            GiftBox gb4 = new GiftBox();
            gb4.id = itemTemplate3.id;
            gb4.type = 3;
            gb4.name = itemTemplate3.name;
            gb4.icon = itemTemplate3.icon;
            gb4.num = listGet.quant[i];
            gb4.color = 0;
            list.add(gb4);
          }
        } else {
          Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, hãy thử lại sau!");
          return;
        }
      }
      p.tichNapCheck[id] = 1;
      if (list.size() > 0) {
        Service.send_gift(p, 1, "Phần thưởng", "Mốc nạp", list, true);
      }
      Message m = new Message(-90);
      m.writer().writeByte(2);
      m.writer().writeByte(id);
      p.addmsg(m);
      m.cleanup();
    }
  }
}
