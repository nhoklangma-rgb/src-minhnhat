package client;

import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import map.f;
import template.EffTemplate;
import template.Skill_info;

public class Buff {
    public static void process(Player p, Message m2) throws IOException {
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        byte size = m2.reader().readByte();
        short[] id2 = null;
        if (size > 0) {
            id2 = new short[size];
            for (int i = 0; i < size; i++) {
                id2[i] = m2.reader().readShort();
            }
        }
        int time_buff = 0;
        List<Integer> list_id = new ArrayList<>();
        List<Integer> list_par = new ArrayList<>();
        Skill_info sk_info = null;
        for (int i = 0; i < p.skill_point.size(); i++) {
            if (p.skill_point.get(i).temp.ID == id && p.skill_point.get(i).temp.Lv_RQ > 0) {
                sk_info = p.skill_point.get(i);
                for (int j = 0; j < sk_info.temp.op.size(); j++) {
                    switch (sk_info.temp.op.get(j).id) {
                        case 32: {
                            time_buff = sk_info.temp.op.get(j).getParam() * 100;
                            break;
                        }
                        // default: {
                        // if (sk_info.temp.op.get(j).id == 25
                        // && (sk_info.temp.op.get(j).id < 28
                        // || (sk_info.temp.op.get(j).id >= 40 && sk_info.temp.op.get(j).id <= 63))
                        // && sk_info.temp.op.get(j).id >= 0) {

                        // list_id.add(sk_info.temp.op.get(j).id);
                        // list_par.add(sk_info.temp.op.get(j).getParam());
                        // }
                        // break;
                        // }

                        default: {
                            list_id.add(sk_info.temp.op.get(j).id);
                            list_par.add(sk_info.temp.op.get(j).getParam());
                            break;
                        }

                    }
                }
                break;
            }
        }
        if (sk_info != null && time_buff > 0 && cat == 0 && size == 1) {
            Service.use_potion(p, 1, -sk_info.temp.manaLost);
            Service.pet(p, p, false);
            Service.UpdateInfoMaincharInfo(p);
            if (id >= 2003 && id <= 2063 && id != 2059) {
                p.update_skill_exp(id, 500);
            }
            Message m = new Message(20);
            m.writer().writeByte(1);
            m.writer().writeShort(id);
            m.writer().writeShort(p.index_map);
            m.writer().writeByte(0);
            m.writer().writeShort(sk_info.temp.idIcon);
            m.writer().writeShort(sk_info.get_eff_skill());
            m.writer().writeInt(time_buff);
            m.writer().writeByte(0);
            m.writer().writeByte(1);
            m.writer().writeShort(p.index_map);
            m.writer().writeByte(list_id.size());
            for (int i = 0; i < list_id.size(); i++) {
                m.writer().writeByte(list_id.get(i));
                m.writer().writeShort(list_par.get(i));
                EffTemplate eff = p.get_eff(list_id.get(i));
                if (eff == null) {
                    p.add_new_eff((list_id.get(i) + 100), list_par.get(i), time_buff);
                } else {
                    eff.time += time_buff;
                }
            }
            switch (id) {
                case 2009: {
                    m.writer().writeByte(3);
                    m.writer().writeShort(308);
                    m.writer().writeShort(309);
                    m.writer().writeShort(310);
                    //
                    Message m12 = new Message(20);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(id);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(0);
                    m12.writer().writeShort(sk_info.temp.idIcon);
                    m12.writer().writeShort(sk_info.get_eff_skill());
                    m12.writer().writeInt(time_buff);
                    m12.writer().writeByte(0);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(3);
                    m12.writer().writeShort(308);
                    m12.writer().writeShort(309);
                    m12.writer().writeShort(310);
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (!p.map.players.get(i).equals(p)) {
                            p.map.players.get(i).addmsg(m12);
                        }
                    }
                    m12.cleanup();
                    break;
                }
                case 2016: {
                    m.writer().writeByte(3);
                    m.writer().writeShort(341);
                    m.writer().writeShort(342);
                    m.writer().writeShort(343);
                    //
                    Message m12 = new Message(20);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(id);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(0);
                    m12.writer().writeShort(sk_info.temp.idIcon);
                    m12.writer().writeShort(sk_info.get_eff_skill());
                    m12.writer().writeInt(time_buff);
                    m12.writer().writeByte(0);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(3);
                    m12.writer().writeShort(341);
                    m12.writer().writeShort(342);
                    m12.writer().writeShort(343);
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (!p.map.players.get(i).equals(p)) {
                            p.map.players.get(i).addmsg(m12);
                        }
                    }
                    m12.cleanup();
                    break;
                }
                case 2037: { // chim ung
                    m.writer().writeByte(3);
                    m.writer().writeShort(490);
                    m.writer().writeShort(491);
                    m.writer().writeShort(492);
                    p.add_new_eff(6, 1, time_buff);
                    //
                    Message m12 = new Message(20);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(id);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(0);
                    m12.writer().writeShort(sk_info.temp.idIcon);
                    m12.writer().writeShort(sk_info.get_eff_skill());
                    m12.writer().writeInt(time_buff);
                    m12.writer().writeByte(0);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(3);
                    m12.writer().writeShort(490);
                    m12.writer().writeShort(491);
                    m12.writer().writeShort(492);
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (!p.map.players.get(i).equals(p)) {
                            p.map.players.get(i).addmsg(m12);
                        }
                    }
                    m12.cleanup();
                    break;
                }
                case 2040: { // bao dom
                    m.writer().writeByte(3);
                    m.writer().writeShort(659);
                    m.writer().writeShort(660);
                    m.writer().writeShort(661);
                    p.add_new_eff(6, 1, time_buff);
                    //
                    Message m12 = new Message(20);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(id);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(0);
                    m12.writer().writeShort(sk_info.temp.idIcon);
                    m12.writer().writeShort(sk_info.get_eff_skill());
                    m12.writer().writeInt(time_buff);
                    m12.writer().writeByte(0);
                    m12.writer().writeByte(1);
                    m12.writer().writeShort(p.index_map);
                    m12.writer().writeByte(3);
                    m12.writer().writeShort(659);
                    m12.writer().writeShort(660);
                    m12.writer().writeShort(661);
                    for (int i = 0; i < p.map.players.size(); i++) {
                        if (!p.map.players.get(i).equals(p)) {
                            p.map.players.get(i).addmsg(m12);
                        }
                    }
                    m12.cleanup();
                    break;
                }
                default: {
                    m.writer().writeByte(0);
                    break;
                }
            }
            p.addmsg(m);
            m.cleanup();
            //
            p.send_skill();
            p.update_info_to_all();
            if (id != 2009 && id != 2016 && id != 2037 && id != 2040 && sk_info.temp.typeSkill == 2
                    && sk_info.temp.nTarget > 1 && p.party != null) {
                int num_party_eff = 1;
                for (int j = 0; j < p.party.list.size(); j++) {
                    Player p0 = p.party.list.get(j);
                    if (p0.conn != null && !p0.name.equals(p.name) && p0.map.equals(p.map)) {
                        m = new Message(20);
                        m.writer().writeByte(0);
                        m.writer().writeShort(id);
                        m.writer().writeShort(p0.index_map);
                        m.writer().writeByte(0);
                        m.writer().writeShort(sk_info.temp.idIcon);
                        m.writer().writeShort(sk_info.get_eff_skill());
                        m.writer().writeInt(time_buff);
                        m.writer().writeByte(0);
                        m.writer().writeByte(1);
                        m.writer().writeShort(p0.index_map);
                        m.writer().writeByte(list_id.size());
                        for (int i = 0; i < list_id.size(); i++) {
                            m.writer().writeByte(list_id.get(i));
                            m.writer().writeShort(list_par.get(i));
                            EffTemplate eff = p0.get_eff(list_id.get(i));
                            if (eff == null) {
                                p0.add_new_eff((list_id.get(i) + 100), list_par.get(i), time_buff);
                            }
                        }
                        m.writer().writeByte(0);
                        p0.addmsg(m);
                        m.cleanup();
                        p0.update_info_to_all();
                        num_party_eff++;
                    }
                    if (num_party_eff >= sk_info.temp.nTarget) {
                        break;
                    }
                }
            }
            //
            switch (id) {
                case 1010: { // luffy
                    p.add_new_eff(11, 1, time_buff);
                    break;
                }
                case 1011: { // zoro
                    p.add_new_eff(12, 1, time_buff);
                    break;
                }
                case 1012: { // sanji
                    p.add_new_eff(13, 1, time_buff);
                    Service.Main_char_Info(p);
                    break;
                }
                case 1013: { // nami
                    p.add_new_eff(14, 1, time_buff);
                    break;
                }
                case 1014: { // usop
                    if (id2 != null) {
                        Player p2 = null;
                        for (int i = 0; i < p.map.players.size(); i++) {
                            if (p.map.players.get(i).index_map != p.index_map
                                    && (p2 == null || (Math.abs(p.map.players.get(i).x - p.x) < Math
                                            .abs(p.map.players.get(i).x - p2.x)
                                            && Math.abs(p.map.players.get(i).y - p.y) < Math
                                                    .abs(p.map.players.get(i).y - p2.y)))) {
                                p2 = p.map.players.get(i);
                            }
                        }
                        if (p2 != null) {
                            if ((p.typePirate == 0 && p2.typePirate == 2)
                                    || (p.typePirate == 2 && p2.typePirate == 0)
                                    || (p.typePirate == 1 && p2.typePirate == 2)
                                    || (p.typePirate == 2 && p2.typePirate == 1)
                                    || (p.type_pk == 14 && p2.type_pk == 15)
                                    || (p.type_pk == 15 && p2.type_pk == 14)
                                    || (p.typePirate == 2 && p2.typePirate == 2) || (p.type_pk == 0)
                                    || (p2.type_pk == 1) || (p.type_pk == 3 && p2.type_pk == 3)
                                    || (p2.type_pk == 0)
                                    || (p.type_pk == 3 && p2.type_pk >= 4 && p2.type_pk <= 8)
                                    || (p2.type_pk == 3 && p.type_pk >= 4 && p.type_pk <= 8)
                                    || (p.type_pk >= 4 && p.type_pk <= 8 && p2.type_pk >= 4
                                            && p2.type_pk <= 8 && p.type_pk != p2.type_pk)) {
                                if (Math.abs(p2.x - p.x) < 200 && Math.abs(p2.y - p.y) < 200) {
                                    m = new Message(-15);
                                    m.writer().writeByte(4);
                                    m.writer().writeShort(p.index_map);
                                    m.writer().writeByte(0);
                                    m.writer().writeShort(500);
                                    //
                                    m.writer().writeShort(p2.index_map);
                                    m.writer().writeByte(0);
                                    //
                                    p.map.send_msg_all_p(m, p, true);
                                    m.cleanup();
                                    //
                                    Buff.send_choang(p, p2, 4000);
                                }
                            }
                        }
                    }
                    break;
                }
                case 2059: { // boc pha
                    EffTemplate eff = p.get_eff(18);
                    if (eff == null) {
                        if (3 > Util.random(120)) {
                            p.add_new_eff(18, 280, time_buff);
                        } else {
                            p.add_new_eff(18, 180, time_buff);
                        }
                    } else {
                        eff.time += time_buff;
                        if (3 > Util.random(120)) {
                            eff.param = 280;
                        } else {
                            eff.param = 180;
                        }
                    }
                    break;
                }
                case 2056: { // add eff skill buff trai bong toi
                    Message m2055 = new Message(74);
                    m2055.writer().writeByte(1);
                    m2055.writer().writeShort(p.index_map);
                    m2055.writer().writeShort(3);
                    m2055.writer().writeInt(time_buff);
                    m2055.writer().writeByte(0);
                    m2055.writer().writeByte(10);
                    p.map.send_msg_all_p(m2055, null, true);
                    m2055.cleanup();
                    break;
                }
                case 5004: { // add eff skill buff haki ba vuong
                    Message m2056 = new Message(74);
                    m2056.writer().writeByte(1);
                    m2056.writer().writeShort(p.index_map);
                    m2056.writer().writeShort(20);
                    m2056.writer().writeInt(time_buff);
                    m2056.writer().writeByte(0);
                    m2056.writer().writeByte(10);
                    p.map.send_msg_all_p(m2056, null, true);
                    m2056.cleanup();
                    break;
                }
                case 5005: { // add eff skill buff haki gio vuong
                    Message m2056 = new Message(74);
                    m2056.writer().writeByte(1);
                    m2056.writer().writeShort(p.index_map);
                    m2056.writer().writeShort(21);
                    m2056.writer().writeInt(time_buff);
                    m2056.writer().writeByte(0);
                    m2056.writer().writeByte(10);
                    p.map.send_msg_all_p(m2056, null, true);
                    m2056.cleanup();
                    break;
                }
            }
        }
    }

    public static void send_choang(Player p, Player p2, int time) throws IOException {
        Message m = new Message(28);
        m.writer().writeShort(p2.index_map);
        m.writer().writeByte(0);
        m.writer().writeInt(f.setInteger(p2.hp));
        m.writer().writeInt(f.setInteger(p2.body.get_hp_max(true)));
        m.writer().writeShort(1);
        m.writer().writeShort(time / 100);
        p.map.send_msg_all_p(m, p, true);
        m.cleanup();
    }
}
