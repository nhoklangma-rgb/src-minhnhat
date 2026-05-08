package client;

import core.Service;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.Option;

public class Party {
    public List<Player> list;
    public List<Option> list_op;

    public Party(Player p) {
        list_op = new ArrayList<>();
        list = new ArrayList<>();
        this.list.add(p);
        //
        add_buff_party(p.clazz);
    }

    private synchronized void add_buff_party(byte clazz) {
        Option op_select = null;
        switch (clazz) {
            case 1: {
                for (int i = 0; i < this.list_op.size(); i++) {
                    if (this.list_op.get(i).id == 17) {
                        op_select = this.list_op.get(i);
                        break;
                    }
                }
                if (op_select == null) {
                    op_select = new Option(17, 250);
                    this.list_op.add(op_select);
                }
                break;
            }
            case 2: {
                for (int i = 0; i < this.list_op.size(); i++) {
                    if (this.list_op.get(i).id == 1) {
                        op_select = this.list_op.get(i);
                        break;
                    }
                }
                if (op_select == null) {
                    op_select = new Option(1, 150);
                    this.list_op.add(op_select);
                }
                break;
            }
            case 3: {
                for (int i = 0; i < this.list_op.size(); i++) {
                    if (this.list_op.get(i).id == 4) {
                        op_select = this.list_op.get(i);
                        break;
                    }
                }
                if (op_select == null) {
                    op_select = new Option(4, 250);
                    this.list_op.add(op_select);
                }
                break;
            }
            case 4: {
                for (int i = 0; i < this.list_op.size(); i++) {
                    if (this.list_op.get(i).id == 23) {
                        op_select = this.list_op.get(i);
                        break;
                    }
                }
                if (op_select == null) {
                    op_select = new Option(23, 250);
                    this.list_op.add(op_select);
                }
                break;
            }
            case 5: {
                for (int i = 0; i < this.list_op.size(); i++) {
                    if (this.list_op.get(i).id == 25) {
                        op_select = this.list_op.get(i);
                        break;
                    }
                }
                if (op_select == null) {
                    op_select = new Option(25, 100);
                    this.list_op.add(op_select);
                }
                break;
            }
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = -1;
        if (type == 0 || type == 2 || type == 4 || type == 6) {
            id = m2.reader().readShort();
        }
        switch (type) {
            case 0: { // request
                Player p0 = p.map.get_player_by_id_inmap(id);
                if (p0 != null) {
                    if (p0.party != null) {
                        Service.send_box_ThongBao_OK(p, "Đối phương đang ở trong nhóm khác!");
                    } else {
                        Message m = new Message(-25);
                        m.writer().writeByte(0);
                        m.writer().writeShort(p.index_map);
                        m.writer().writeUTF(p.getNameShow());
                        p0.addmsg(m);
                        m.cleanup();
                        //
                        if (p.party == null) {
                            p.party = new Party(p);
                            p.party.send_info();
                        }
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Đối phương offline");
                }
                break;
            }
            case 4: { // accept
                Player p0 = p.map.get_player_by_id_inmap(id);
                if (p0 != null) {
                    if (p0.party != null) {
                        p0.party.add_new_mem(p);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Đối phương đã hủy nhóm");
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Đối phương offline");
                }
                break;
            }
            case 3: { // delete
                if (p.party != null && p.party.list.get(0).equals(p)) {
                    p.party.delete();
                }
                break;
            }
            case 2: { // leave
                if (p.party != null) {
                    Player p0 = p.map.get_player_by_id_inmap(id);
                    if (p0 != null) {
                        p.party.remove_mem(p0);
                        if (p0.index_map == p.index_map) {
                            Service.send_box_ThongBao_OK(p0, "Bạn rời khỏi nhóm");
                        } else {
                            Service.send_box_ThongBao_OK(p0, "Bạn bị đuổi khỏi nhóm");
                        }
                    }
                }
                break;
            }
        }
    }

    public synchronized void remove_mem(Player p) throws IOException {
        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            if (p0.equals(p)) {
                Message m = new Message(-25);
                m.writer().writeByte(3);
                p0.addmsg(m);
                m.cleanup();
                p0.party = null;
                list.remove(p);
                this.send_info();
                // Kiểm tra số lượng thành viên sau khi remove
                if (list.size() < 2) {
                    // Hủy nhóm vì không đủ thành viên
                    delete();
                }
                break;
            }
        }
    }

    private synchronized void delete() throws IOException {
        Message m = new Message(-25);
        m.writer().writeByte(3);
        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            p0.party = null;
            if (p0.conn != null) {
                p0.addmsg(m);
                Service.send_box_ThongBao_OK(p0, "Nhóm đã giải tán");
            }
        }
        m.cleanup();
        this.list.clear();
    }

    private synchronized void add_new_mem(Player p) throws IOException {
        if (this.list.size() < 5) {
            list.add(p);
            p.party = this;
            add_buff_party(p.clazz);
            this.send_info();
            Service.send_box_ThongBao_OK(p, "Vào nhóm thành công!");
        } else {
            Service.send_box_ThongBao_OK(list.get(0), "Nhóm đầy!");
        }
    }

    public void send_info() throws IOException {

        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            if (p0.conn == null) {
                list.remove(p0);
                i--;
            }
        }
        Message m = new Message(-25);
        m.writer().writeByte(5);
        m.writer().writeByte(list.size());
        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            m.writer().writeShort(p0.index_map);
            m.writer().writeUTF(p0.getNameShow());
            m.writer().writeShort(p0.map.template.id);
            m.writer().writeByte(i == 0 ? 1 : 0);
            m.writer().writeByte(p0.map.zone_id);
        }
        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            p0.addmsg(m);
        }
        m.cleanup();
        // party buff
        //
        for (int i = 0; i < list.size(); i++) {
            Player p0 = list.get(i);
            List<Option> op_select = get_list_buff_now(p0);
            m = new Message(32);
            if (op_select.size() > 0) {
                m.writer().writeByte(op_select.size());
                //
                for (int i4 = 0; i4 < op_select.size(); i4++) {
                    m.writer().writeByte(op_select.get(i4).id);
                    m.writer().writeShort(op_select.get(i4).getParam());
                }
            } else {
                m.writer().writeByte(1);
                m.writer().writeByte(1);
                m.writer().writeShort(0);
            }
            //
            p0.addmsg(m);
            m.cleanup();
            p0.update_info_to_all();
        }
    }

    public synchronized List<Option> get_list_buff_now(Player p0) {
        int num_P = 0;
        List<Option> result = new ArrayList<>();
        for (int i2 = 0; i2 < list.size(); i2++) {
            Player p0_2 = list.get(i2);
            Option op_select = null;
            if (p0.map.equals(p0_2.map)) {
                switch (p0_2.clazz) {
                    case 1: {
                        for (int i3 = 0; i3 < this.list_op.size(); i3++) {
                            if (this.list_op.get(i3).id == 17) {
                                op_select = this.list_op.get(i3);
                                break;
                            }
                        }
                        break;
                    }
                    case 2: {
                        for (int i3 = 0; i3 < this.list_op.size(); i3++) {
                            if (this.list_op.get(i3).id == 1) {
                                op_select = this.list_op.get(i3);
                                break;
                            }
                        }
                        break;
                    }
                    case 3: {
                        for (int i3 = 0; i3 < this.list_op.size(); i3++) {
                            if (this.list_op.get(i3).id == 4) {
                                op_select = this.list_op.get(i3);
                                break;
                            }
                        }
                        break;
                    }
                    case 4: {
                        for (int i3 = 0; i3 < this.list_op.size(); i3++) {
                            if (this.list_op.get(i3).id == 23) {
                                op_select = this.list_op.get(i3);
                                break;
                            }
                        }
                        break;
                    }
                    case 5: {
                        for (int i3 = 0; i3 < this.list_op.size(); i3++) {
                            if (this.list_op.get(i3).id == 25) {
                                op_select = this.list_op.get(i3);
                                break;
                            }
                        }
                        break;
                    }
                }
                num_P++;
            }
            if (op_select != null) {
                Option op_can_add = null;
                for (int i = 0; i < result.size(); i++) {
                    if (op_select.id == result.get(i).id) {
                        op_can_add = result.get(i);
                        break;
                    }
                }
                if (op_can_add == null) {
                    result.add(op_select);
                }
            }
        }
        if (num_P < 2 && result.size() < 2) {
            result.clear();
        }
        return result;
    }
}
