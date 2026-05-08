package activities;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import core.Service;
import io.Message;
import map.Map;
import template.FriendTemp;

public class Friend {
    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = (short) m2.reader().readInt();
        switch (type) {
            case 0: {
                Player p0 = p.map.get_player_by_id_inmap(id);
                if (p0 != null) {
                    if (p.friend_list.size() < 100) {
                        for (int i = 0; i < p.friend_list.size(); i++) {
                            if (p.friend_list.get(i).name.equals(p0.name)) {
                                Service.send_box_ThongBao_OK(p,
                                        "Đối phương đã có trong danh sách bạn bè");
                                return;
                            }
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Đầy danh sách");
                        return;
                    }
                }
                if (p0 != null && p0.index_map != p.index_map) {
                    Message m = new Message(-29);
                    m.writer().writeByte(0);
                    m.writer().writeShort(p.index_map);
                    m.writer().writeUTF(p.name);
                    p0.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_box_ThongBao_OK(p, "Đối phương offline");
                }
                break;
            }
            case 3: {
                Player p0 = p.map.get_player_by_id_inmap(id);
                if (p0 == null) {
                    Service.send_box_ThongBao_OK(p, "Đối phương không có mặt trong map");
                    return;
                }
                if (p.friend_list.size() < 100) {
                    for (int i = 0; i < p.friend_list.size(); i++) {
                        if (p.friend_list.get(i).name.equals(p0.name)) {
                            Service.send_box_ThongBao_OK(p,
                                    "Đối phương đã có trong danh sách bạn bè");
                            return;
                        }
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Danh sách bạn bè đã đầy, không thể thêm nữa");
                    return;
                }
                if (p0.index_map != p.index_map) {
                    p.friend_list.add(new FriendTemp(p0));
                    p.friend_list.get(p.friend_list.size() - 1).id =
                            p.friend_list.indexOf(p.friend_list.get(p.friend_list.size() - 1));
                    p0.friend_list.add(new FriendTemp(p));
                    p0.friend_list.get(p0.friend_list.size() - 1).id =
                            p0.friend_list.indexOf(p0.friend_list.get(p0.friend_list.size() - 1));
                    //
                    Message m = new Message(-29);
                    m.writer().writeByte(3);
                    ReadInfoMemList(m.writer(), p.friend_list.get(p.friend_list.size() - 1));
                    p.addmsg(m);
                    m.cleanup();
                    m = new Message(-29);
                    m.writer().writeByte(3);
                    ReadInfoMemList(m.writer(), p0.friend_list.get(p0.friend_list.size() - 1));
                    p0.addmsg(m);
                    m.cleanup();
                    Service.send_box_ThongBao_OK(p, "Bạn trở thành bạn bè với " + p0.name);
                    Service.send_box_ThongBao_OK(p0, "Bạn trở thành bạn bè với " + p.name);
                }
                break;
            }
            case 2: {
                if (id == 0) {
                    update_list(p);
                }
                break;
            }
            case 1: {
                if (id < p.friend_list.size()) {
                    p.friend_list.remove(id);
                    update_list(p);
                }
                break;
            }
        }
    }

    private static void update_list(Player p) throws IOException {
        List<FriendTemp> list_remove = new ArrayList<>();
        for (int i = 0; i < p.friend_list.size(); i++) {
            Player p0 = Map.get_player_by_name_allmap(p.friend_list.get(i).name);
            boolean can_remove = true;
            if (p0 != null) {
                for (int j = 0; j < p0.friend_list.size(); j++) {
                    if (p0.friend_list.get(j).name.equals(p.name)) {
                        can_remove = false;
                        break;
                    }
                }
                if (can_remove) {
                    list_remove.add(p.friend_list.get(i));
                }
            }
        }
        p.friend_list.removeAll(list_remove);
        Message m = new Message(-29);
        m.writer().writeByte(2);
        m.writer().writeByte(p.friend_list.size());
        for (int i = 0; i < p.friend_list.size(); i++) {
            ReadInfoMemList(m.writer(), p.friend_list.get(i));
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void ReadInfoMemList(DataOutputStream dos, FriendTemp temp) throws IOException {
        Player p0 = Map.get_player_by_name_allmap(temp.name);
        if (p0 != null) {
            temp.level = p0.level;
            temp.head = p0.head;
            temp.hair = p0.hair;
        }
        dos.writeInt(temp.id);
        dos.writeUTF(temp.name);
        dos.writeShort(temp.level);
        dos.writeShort(temp.head);
        dos.writeShort(temp.hair);
        dos.writeShort(temp.hat);
        dos.writeByte((p0 != null) ? 1 : 0);
        String info =
                (p0 == null) ? "Offline" : (p0.map.template.name + " khu " + (p0.map.zone_id + 1));
        dos.writeUTF(info);
        dos.writeShort(temp.rank);
    }
}
