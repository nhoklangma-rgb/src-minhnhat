package client;

import core.Service;
import io.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Quest {
    private static List<Quest> ENTRY = new ArrayList<>();
    public static Quest QUEST_FINISH;
    public short id;
    public short index;
    public byte statusQuest;
    public byte typeMainSub;
    public byte typeActionQuest;
    public String name;
    public short idNpc;
    public String talk;
    public byte typeQ;
    public String strNpcMap;
    public short lvRequest;
    public String strNhacNho;
    public String showDialog;
    public short[][] data_quest;
    public short idMapHelp;
    public short idNpcSub;
    public List<GiftBox> gift;
    public String infoFinish;

    public static void add(ResultSet rs) throws SQLException {
        Quest t = new Quest();
        t.index = rs.getShort("index_server");
        t.statusQuest = rs.getByte("statusQuest");
        t.typeMainSub = rs.getByte("typeMainSub");
        t.typeActionQuest = rs.getByte("typeActionQuest");
        t.name = rs.getString("name");
        t.idNpc = rs.getShort("idNpc");
        t.talk = rs.getString("talk");
        t.typeQ = rs.getByte("typeQ");
        t.strNpcMap = rs.getString("strNpcMap");
        t.lvRequest = rs.getShort("lvRequest");
        t.strNhacNho = rs.getString("strNhacNho");
        t.showDialog = rs.getString("showDialog");
        JSONArray js = (JSONArray) JSONValue.parse(rs.getString("data_quest"));
        t.data_quest = new short[js.size()][];
        for (int i = 0; i < t.data_quest.length; i++) {
            JSONArray js2 = (JSONArray) js.get(i);
            t.data_quest[i] = new short[js2.size()];
            for (int j = 0; j < t.data_quest[i].length; j++) {
                t.data_quest[i][j] = Short.parseShort(js2.get(j).toString());
            }
        }
        js.clear();
        t.idMapHelp = rs.getShort("idMapHelp");
        t.idNpcSub = rs.getShort("idNpcSub");
        t.id = rs.getShort("id");
        t.gift = new ArrayList<>();
        js = (JSONArray) JSONValue.parse(rs.getString("gift"));
        for (int i = 0; i < js.size(); i++) {
            JSONArray js2 = (JSONArray) js.get(i);
            GiftBox temp = new GiftBox();
            temp.type = Byte.parseByte(js2.get(0).toString());
            temp.name = js2.get(1).toString();
            temp.icon = Short.parseShort(js2.get(2).toString());
            temp.num = Integer.parseInt(js2.get(3).toString());
            temp.color = Byte.parseByte(js2.get(4).toString());
            if (temp.type == 4) {
                if (temp.name.equals("XP Clan")) {
                    temp.id = -10;
                } else if (temp.name.equals("Bery Clan")) {
                    temp.id = -11;
                } else if (temp.name.equals("Ruby Clan")) {
                    temp.id = -12;
                } else {
                    ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_name(temp.name);
                    if (itemTemplate4 != null && itemTemplate4.icon == temp.icon) {
                        temp.id = itemTemplate4.id;
                    } else {
                        core.GameLogger.info("load gift quest err " + temp.name + " id " + t.id);
                        System.exit(0);
                    }
                }
            } else if (temp.type == 7) {
                ItemTemplate7 itemTemplate7 = ItemTemplate7.get_it_by_name(temp.name);
                if (itemTemplate7 != null && itemTemplate7.icon == temp.icon) {
                    temp.id = itemTemplate7.id;
                } else {
                    core.GameLogger.info("load gift quest err " + temp.name + " id " + t.id);
                    System.exit(0);
                }
            } else if (temp.type == 99) {
                temp.id = -1;
            } else {
                core.GameLogger.info("load gift quest err " + temp.name + " id " + t.id);
                System.exit(0);
            }
            t.gift.add(temp);
        }
        t.infoFinish = rs.getString("infoFinish");
        Quest.ENTRY.add(t);
    }

    public static Quest get_quest(int id_quest) {
        for (int i = 0; i < Quest.ENTRY.size(); i++) {
            if (Quest.ENTRY.get(i).id == id_quest) {
                return Quest.ENTRY.get(i);
            }
        }
        return Quest.QUEST_FINISH;
    }

    public static void write_Quest(DataOutputStream dos, QuestP q) throws IOException {
        switch (q.template.statusQuest) {
            case 0: {
                dos.writeShort(q.template.index);
                dos.writeByte(q.template.typeMainSub);
                dos.writeByte(q.template.typeActionQuest);
                dos.writeUTF(q.template.name);
                dos.writeShort(q.template.idNpc);
                dos.writeUTF(q.template.talk);
                dos.writeByte(q.template.typeQ);
                dos.writeUTF(q.template.showDialog);
                dos.writeUTF(q.template.strNpcMap);
                dos.writeShort(q.template.lvRequest);
                break;
            }
            case 1: {
                dos.writeShort(q.template.index);
                dos.writeByte(q.template.typeMainSub);
                dos.writeByte(q.template.typeActionQuest);
                dos.writeUTF(q.template.name);
                dos.writeShort(q.template.idNpc);
                dos.writeUTF(q.template.strNhacNho);
                dos.writeUTF(q.template.showDialog);
                dos.writeByte(q.template.data_quest.length);
                for (int i = 0; i < q.data.length; i++) {
                    dos.writeByte(q.data[i][0]);
                    if (q.data[i][0] == 1 || q.data[i][0] == 2) {
                        dos.writeShort(q.data[i][1]);
                        dos.writeShort(q.data[i][2]);
                        dos.writeShort(q.data[i][3]);
                    }
                }
                dos.writeUTF(q.template.strNpcMap);
                dos.writeShort(q.template.idMapHelp);
                break;
            }
            case 2: {
                dos.writeShort(q.template.index);
                dos.writeByte(q.template.typeMainSub);
                dos.writeByte(q.template.typeActionQuest);
                dos.writeUTF(q.template.name);
                dos.writeShort(q.template.idNpc);
                dos.writeShort(q.template.idNpcSub);
                dos.writeUTF(q.template.talk);
                dos.writeByte(q.template.typeQ);
                dos.writeUTF(q.template.showDialog);
                dos.writeUTF(q.template.strNpcMap);
                dos.writeUTF(q.template.strNhacNho);
                break;
            }
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        QuestP temp = p.get_quest(id);
        switch (act) {
            case 1: { // do quest
                if (temp != null && temp.template.equals(Quest.QUEST_FINISH)) {
                    Service.send_box_ThongBao_OK(p, "Bạn đã hoàn thành hết nhiệm vụ hiện tại");
                    return;
                }
                if (temp != null && temp.template.statusQuest == 0) {
                    // check side quest
                    if (temp.template.id < 0) {
                        if (p.list_quest.get(0).template.id < 11) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn phải hoàn thành nhiệm vụ Bài tập đầu tiên mới có "
                                            + "thể nhận nhiệm vụ lặp");
                        } else {
                            p.data_yesno = new int[] { id };
                            Service.send_box_yesno(p, 15, "Thông báo",
                                    ("Nhận nhiệm vụ lặp mất 3 bánh mì, bạn có muốn"
                                            + " nhận không?"),
                                    new String[] { "3", "Hủy" }, new byte[] { 8, -1 });
                        }
                    } else {
                        // remove quest now
                        Quest.remove_old_and_send_next(p, temp);
                        // send dialog quest new
                        Message m = new Message(-23);
                        m.writer().writeByte(5);
                        m.writer().writeShort(temp.template.index);
                        p.addmsg(m);
                        m.cleanup();
                    }
                }
                break;
            }
            case 4: { // finish quest
                if (temp != null && temp.template.equals(Quest.QUEST_FINISH)) {
                    Service.send_box_ThongBao_OK(p, "Bạn đã hoàn thành hết nhiệm vụ hiện tại");
                    return;
                }
                if (temp != null && temp.template.statusQuest == 2) {
                    Quest.remove_old_and_send_next(p, temp);
                    // remove item quest
                    List<ItemBag47> list_remove = new ArrayList<>();
                    for (int i = 0; i < p.item.bag47.size(); i++) {
                        if (p.item.bag47.get(i).category == 5) {
                            list_remove.add(p.item.bag47.get(i));
                        }
                    }
                    p.item.bag47.removeAll(list_remove);
                    p.item.update_Inventory(-1, false);
                }
                break;
            }
            case 2: {
                break;
            }
        }
    }

    public static void remove_old_and_send_next(Player p, QuestP temp) throws IOException {
        Message m = new Message(-23);
        m.writer().writeByte(2);
        m.writer().writeShort(temp.template.index);
        p.addmsg(m);
        m.cleanup();
        //
        if (temp.template.id < 0 && temp.template.statusQuest == 2) {
            Quest.remove_side_quest(p, temp);
        } else {
            Quest.next_quest(p, temp);
            m = new Message(-23);
            m.writer().writeByte(1);
            m.writer().writeByte(temp.template.statusQuest);
            Quest.write_Quest(m.writer(), temp);
            p.addmsg(m);
            m.cleanup();
        }
    }

    private static void remove_side_quest(Player p, QuestP questP) throws IOException {
        if (questP.template.gift.size() > 0) {
            Quest.send_gift(p, questP);
        }
        p.list_quest.remove(questP);
        Quest.update_map_have_side_quest(p, false);
    }

    public static void next_quest(Player p, QuestP questP) throws IOException {
        if (questP.template.gift.size() > 0) {
            Quest.send_gift(p, questP);
        }
        questP.template = Quest.get_quest(questP.template.id + 1);
        questP.data = new short[questP.template.data_quest.length][];
        for (int i = 0; i < questP.data.length; i++) {
            questP.data[i] = new short[questP.template.data_quest[i].length];
            for (int j = 0; j < questP.data[i].length; j++) {
                questP.data[i][j] = questP.template.data_quest[i][j];
            }
        }
    }

    private static void send_gift(Player p, QuestP questP) throws IOException {
        List<GiftBox> result = new ArrayList<>();
        for (int i = 0; i < questP.template.gift.size(); i++) {
            GiftBox temp = new GiftBox();
            temp.type = questP.template.gift.get(i).type;
            temp.name = questP.template.gift.get(i).name;
            temp.icon = questP.template.gift.get(i).icon;
            temp.num = questP.template.gift.get(i).num;
            temp.color = questP.template.gift.get(i).color;
            temp.id = questP.template.gift.get(i).id;
            result.add(temp);
        }
        Service.send_gift(p, 0, "Nhiệm vụ hoàn thành", questP.template.infoFinish, result, true);
    }

    public static void send_List_Quest(Player p, boolean save_cache) throws IOException {
        List<QuestP> q0 = new ArrayList<>();
        List<QuestP> q1 = new ArrayList<>();
        List<QuestP> q2 = new ArrayList<>();
        for (int i = 0; i < p.list_quest.size(); i++) {
            QuestP temp = p.list_quest.get(i);
            switch (temp.template.statusQuest) {
                case 0: {
                    q0.add(temp);
                    break;
                }
                case 1: {
                    q1.add(temp);
                    break;
                }
                case 2: {
                    q2.add(temp);
                    break;
                }
            }
        }
        Message m = new Message(-23);
        m.writer().writeByte(0);
        m.writer().writeByte(q0.size());
        for (int i = 0; i < q0.size(); i++) {
            Quest.write_Quest(m.writer(), q0.get(i));
        }
        m.writer().writeByte(q1.size());
        for (int i = 0; i < q1.size(); i++) {
            Quest.write_Quest(m.writer(), q1.get(i));
        }
        m.writer().writeByte(q2.size());
        for (int i = 0; i < q2.size(); i++) {
            Quest.write_Quest(m.writer(), q2.get(i));
        }
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void update_map_have_side_quest(Player p, boolean save_cache) throws IOException {
        // remove job repeat status 0
        List<QuestP> list_remove = new ArrayList<>();
        for (int i = 0; i < p.list_quest.size(); i++) {
            if (p.list_quest.get(i).template.id < 0
                    && p.list_quest.get(i).template.statusQuest == 0) {
                list_remove.add(p.list_quest.get(i));
            }
        }
        if (list_remove.size() > 0) {
            for (int i = 0; i < list_remove.size(); i++) {
                Message m = new Message(-23);
                m.writer().writeByte(2);
                m.writer().writeShort(list_remove.get(i).template.index);
                if (save_cache) {
                    p.list_msg_cache.add(m);
                } else {
                    p.addmsg(m);
                }
                m.cleanup();
                p.list_quest.remove(list_remove.get(i));
            }
            list_remove.clear();
        }
    }

    public static void add_finish_quest() {
        if (Quest.ENTRY.size() > 0) {
            Quest last = Quest.ENTRY.get(Quest.ENTRY.size() - 1);
            QUEST_FINISH = new Quest();
            QUEST_FINISH.index = (short) (last.index + 1);
            QUEST_FINISH.statusQuest = -1;
            QUEST_FINISH.typeMainSub = 0;
            QUEST_FINISH.typeActionQuest = 0;
            QUEST_FINISH.name = "Làng Cối Xay Gió";
            QUEST_FINISH.idNpc = -1;
            QUEST_FINISH.talk = "1 Chào mừng bạn đến server HTTH, chúc các bạn có một trải nghiệm tuyệt vời, nhiệm vụ hiện tại đã hết";
            QUEST_FINISH.typeQ = 0;
            QUEST_FINISH.strNpcMap = "Trưởng làng - Làng Cối Xay Gió";
            QUEST_FINISH.lvRequest = 1;
            QUEST_FINISH.strNhacNho = "nhacnho";
            QUEST_FINISH.showDialog = "Hãy tiếp tục nói chuyện với trưởng làng.";
            QUEST_FINISH.data_quest = new short[0][];
            QUEST_FINISH.idMapHelp = -1;
            QUEST_FINISH.idNpcSub = -1;
            QUEST_FINISH.id = (short) (last.id + 1);
            QUEST_FINISH.gift = new ArrayList<>();
            QUEST_FINISH.infoFinish = "info finish";
        }
    }
}
