package client;

import activities.*;
import core.*;
import database.SQL;
import io.Message;
import io.Session;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import map.Map;
import map.Mob;
import map.f;
import template.EffTemplate;
import template.ItemBoat;
import template.ItemTemplate4;

public class MessageHandler {

    private Session conn;

    public MessageHandler(Session session) {
        this.conn = session;
    }

    public void process_msg(Message m) throws IOException {
        switch (m.cmd) {
            case -100: // Keepalive Ping from WebGL
                break;
            case -89: {
                if (conn.p != null) {
                    LenhTruyNa.process(conn.p, m);
                }
                break;
            }
            case 60: {
                if (conn.p != null) {
                    DiemDanh.process(conn.p, m);
                }
                break;
            }
            case -96: {
                if (conn.p != null) {
                    ListTichTieu.process(conn.p, m);
                }
                break;
            }
            case -90: {
                if (conn.p != null) {
                    ListTichNap.process(conn.p, m);
                }
                break;
            }
            case 37: {
                if (conn.p != null) {
                    ArchiDaily.process(conn.p, m);
                }
                break;
            }
            case -86: {
                if (conn.p != null) {
                    Wanted_Chest.process(conn.p, m);
                }
                break;
            }
            case -85: {
                if (conn.p != null) {
                    Wanted.process(conn.p, m);
                }
                break;
            }
            case -35: {
                if (conn.p != null) {
                    Fight.process(conn.p, m);
                }
                break;
            }
            case -80: {
                if (conn.p != null) {
                    // Pet.process(conn.p, m);
                }
                break;
            }
            case 79: {
                if (conn.p != null) {
                    HanhTrinh.process(conn.p, m);
                }
                break;
            }
            case -94: {
                if (conn.p != null) {
                    UpgradeDial.process(conn.p, m);
                }
                break;
            }
            case 81: {
                if (conn.p != null) {
                    Upgrade_Skin.process(conn.p, m);
                }
                break;
            }
            case -53: {
                if (conn.p != null) {
                    Ship.process(conn.p, m);
                }
                break;
            }
            case 74: {
                byte type = m.reader().readByte();
                short id = m.reader().readShort();
                if (conn.p != null && type == 0) {
                    try {
                        Message m2 = new Message(76);
                        m2.writer().writeByte(0);
                        m2.writer().writeShort(id);
                        byte[] data1 = Util.loadfile("data/template/Eff_Data/" + id);
                        byte[] data2 = Util.loadfile("data/template/Eff_Img/x" + conn.zoomlv + "/" + id + ".png");
                        m2.writer().writeShort(data1.length);
                        m2.writer().write(data1);
                        m2.writer().write(data2);
                        conn.addmsg(m2);
                        m2.cleanup();
                    } catch (Exception e) {
                        GameLogger.error("MessageHandler case 74: Error loading Eff_Data/Img for ID " + id, e);
                    }
                }
                break;
            }
            case 43: {
                if (conn.p != null) {
                    byte type = m.reader().readByte();
                    byte value = m.reader().readByte();
                    if (type == 0) {
                        if (value == 1 || value == 0) {
                            conn.p.is_show_hat = !conn.p.is_show_hat;
                            Service.charWearing(conn.p, conn.p, false);
                            Service.update_PK(conn.p, conn.p, false);
                            conn.p.update_info_to_all();
                            Service.send_box_ThongBao_OK(conn.p,
                                    conn.p.is_show_hat ? "Đã bật hiển thị nón"
                                            : "Đã tắt hiển thị nón");
                        }
                    } else if (type == 1) {
                        if (value == 1 || value == 0) {
                            conn.p.is_show_weapon = !conn.p.is_show_weapon;
                            Service.charWearing(conn.p, conn.p, false);
                            Service.update_PK(conn.p, conn.p, false);
                            conn.p.update_info_to_all();
                            Service.send_box_ThongBao_OK(conn.p,
                                    conn.p.is_show_weapon ? "Đã bật hiển thị vũ khí thời trang"
                                            : "Đã tắt hiển thị vũ khí thời trang");
                        }
                    }
                }
                break;
            }
            case -91: {
                if (conn.p != null) { // dau gia
                    DauGia.process(conn.p, m);
                }
                break;
            }
            case -63: {
                if (conn.p != null) {
                    Pvp.process(conn.p, m);
                }
                break;
            }
            case 68: {
                if (conn.p != null && conn.p.map != null) {
                    conn.p.tocSuper++;
                    if (conn.p.tocSuper > 2) {
                        conn.p.tocSuper = 0;
                    }
                    List<Player> snapshot181;
                    synchronized (conn.p.map.players) {
                        snapshot181 = new ArrayList<>(conn.p.map.players);
                    }
                    for (Player p0 : snapshot181) {
                        Service.charWearing(conn.p, p0, false);
                    }
                }
                break;
            }
            case -95: {
                break;
            }
            case -36: {
                int id = m.reader().readInt();
                if (conn.p != null) {
                    Player p0 = null;
                    for (int i = 0; i < conn.p.friend_list.size(); i++) {
                        if (conn.p.friend_list.get(i).id == id) {
                            p0 = Map.get_player_by_name_allmap(conn.p.friend_list.get(i).name);
                            break;
                        }
                    }
                    if (p0 == null) {
                        for (int i = 0; i < conn.p.enemy_list.size(); i++) {
                            if (conn.p.enemy_list.get(i).id == id) {
                                p0 = Map.get_player_by_name_allmap(conn.p.enemy_list.get(i).name);
                                break;
                            }
                        }
                    }
                    boolean check = false;
                    if (p0 != null) {
                        if (Map.map_cant_save_site(p0.map.template.id)) {
                            Service.send_box_ThongBao_OK(conn.p,
                                    "Không thể dịch chuyển đến lúc này");
                            return;
                        }
                        for (int i = 0; i < p0.friend_list.size(); i++) {
                            if (p0.friend_list.get(i).name.equals(conn.p.name)) {
                                check = true;
                                break;
                            }
                        }
                        if (!check) {
                            for (int i = 0; i < conn.p.enemy_list.size(); i++) {
                                if (conn.p.enemy_list.get(i).name.equals(p0.name)) {
                                    check = true;
                                    break;
                                }
                            }
                        }
                        if (check) {
                            conn.p.data_yesno = new int[] { id };
                            Service.send_box_yesno(conn.p, 43, "Thông báo",
                                    ("Dịch chuyển đến người này mất 100 Ruby, xác nhận dịch chuyển?"),
                                    new String[] { "100", "Không" }, new byte[] { 7, -1 });
                        }
                    }
                    if (!check) {
                        Service.send_box_ThongBao_OK(conn.p,
                                "Đối phương không online hoặc không có trong danh sách");
                    }
                }
                break;
            }
            case -52: {
                if (conn.p != null) {
                    byte type = m.reader().readByte();
                    if (conn.p.clan != null || type == 11 || type == 12) { // type 11 xin vao clan,
                        // type 12 dong y moi
                        // vao clan
                        Clan.process(conn.p, m, type);
                    }
                }
                break;
            }
            case 49: {
                if (conn.p != null) {
                    Max_Level.process(conn.p, m);
                }
                break;
            }
            case 80: { // event
                if (conn.p != null) {
                }
                break;
            }
            case -71: { // auto revive
                if (conn.p != null && conn.p.map != null && conn.p.map.map_pvp == null
                        && conn.p.map.map_little_garden == null) {
                    if (conn.p.map.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
                        Service.send_box_ThongBao_OK(conn.p, "Vé Hồi Sinh không có tác dụng tại đây!");
                        break;
                    }
                    if (conn.p.type_pk == -1 && conn.p.typePirate == -1) {
                        if (m.reader().readByte() == 1) {
                            if (conn.p.item.total_item_bag_by_id(4, 89) > 0) {
                                conn.p.item.remove_item47(4, 89, 1);
                                conn.p.item.update_Inventory(-1, false);
                                conn.p.isdie = false;
                                Service.use_potion(conn.p, 0, conn.p.body.get_hp_max(true));
                                Service.use_potion(conn.p, 1, conn.p.body.get_mp_max(true));
                                Service.send_revive_player(conn.p);
                                //
                                Message m2 = new Message(-71);
                                m2.writer().writeByte(1);
                                m2.writer().writeShort(conn.p.index_map);
                                m2.writer().writeByte(0);
                                m2.writer().writeInt(60 * 30);
                                conn.p.map.send_msg_all_p(m2, conn.p, true);
                                m2.cleanup();
                                EffTemplate eff = conn.p.get_eff(7);
                                if (eff != null) {
                                    eff.time = System.currentTimeMillis() + 60_000L * 15;
                                } else {
                                    conn.p.add_new_eff(7, 1, 60_000L * 15);
                                }
                            }
                        }
                    }
                }
                break;
            }
            case -74: {
                if (conn.p != null) {
                    TableTickOption.process(conn.p, m);
                }
                break;
            }
            case 44: {
                if (conn.p != null) {
                    Market.process(conn.p, m);
                }
                break;
            }
            case -23: {
                if (conn.p != null) {
                    Quest.process(conn.p, m);
                }
                break;
            }
            case -72: {
                break;
            }
            case 66: {
                if (conn.p != null) {
                    UpgradeSuperItem.process(conn.p, m);
                }
                break;
            }
            case -30: {
                if (conn.p != null) {
                    BXH.process(conn.p, m);
                }
                break;
            }
            case -50: {
                if (conn.p != null) {
                    byte type = m.reader().readByte();
                    if (type == 0) {
                        Split_Item.process(conn.p, m);
                    } else if (type == 1) {
                        Join_Item.process(conn.p, m);
                    }
                }
                break;
            }
            case -62: {
                if (conn.p != null) {
                    ItemBoat.update_part_boat_when_shopping(conn.p);
                }
                break;
            }
            case -28: {
                if (conn.p != null) {
                    Learn_Skill.process(conn.p, m);
                }
                break;
            }
            case 45: {
                if (conn.p != null) {
                    UpgradeDevil.process(conn.p, m);
                }
                break;
            }
            case -25: {
                if (conn.p != null) {
                    Party.process(conn.p, m);
                }
                break;
            }
            case 20: {
                if (conn.p != null && conn.p.map != null && !conn.p.isdie
                        && conn.p.get_eff(201) == null) {
                    Buff.process(conn.p, m);
                }
                break;
            }
            case -32: {
                if (conn.p != null) {
                    PlayerChest.process(conn.p, m);
                }
                break;
            }
            case 54: {
                if (conn.p != null) {
                    VongQuay.process(conn.p, m);
                }
                break;
            }
            case 18: {
                if (conn.p != null) {
                    Chat.process(conn.p, m, 0);
                }
                break;
            }
            case -29: {
                if (conn.p != null) {
                    Friend.process(conn.p, m);
                }
                break;
            }
            case -49: {
                Trade.process(conn.p, m);
                break;
            }
            case -67: {
                if (conn.p != null) {
                    Rebuild_Item.process(conn.p, m);
                }
                break;
            }
            case -77: {
                if (conn.p != null) {
                    ChuyenHoa.process(conn.p, m);
                }
                break;
            }
            case -16: {
                if (conn.p != null) {
                    conn.p.plus_point(m);
                }
                break;
            }
            case -13: { // use potion
                if (conn.p != null) {
                    Player p = conn.p;
                    int id = m.reader().readShort();
                    int totalItem = p.item.total_item_bag_by_id(4, id);
                    String itemName = ItemTemplate4.get_it_by_id(id).name;
                    Set<Integer> specialItems = Set.of(
                            414, 592, 593, 594, 595, 582, 596,
                            1037, 1038, 349, 1039, 1022, 1023,
                            1024, 1018, 1019, 455, 1051, 1052, 1053, 1054, 1055, 1060, 1061, 1062, 1080, 1081);
                    if (specialItems.contains(id) && totalItem >= 5) {
                        if (p.tempIndexMapping == null)
                            p.tempIndexMapping = new ArrayList<>();
                        p.tempIndexMapping.clear();
                        p.tempIndexMapping.add(id);
                        Service.input_text(p, 25, itemName + " (" + totalItem + ")",
                                new String[] { "Nhập số lượng sử dụng" });
                    } else if (totalItem > 0) {
                        UseItem.use_item_potion(p, id);
                    }
                }
                break;
            }

            case -11: {
                if (conn.p != null) {
                    ClientYesNo.process(conn.p, m);
                }
                break;
            }
            case -58: {
                if (conn.p != null) {
                    ClientInput.process(conn.p, m);
                }
                break;
            }
            case -46: {
                if (conn.p != null) {
                    byte type = m.reader().readByte();
                    String text = m.reader().readUTF();
                    if (type == 0) {
                        if (conn.p.get_ngoc() < 5) {
                            Service.send_box_ThongBao_OK(conn.p, "Không đủ 5 ruby để chat KTG");
                            return;
                        }
                        conn.p.update_ngoc(-5);
                        conn.p.update_money();
                        Manager.gI().chatKTG(conn.p, conn.p.getNameShow() + ": " + text);
                    } else if (type == 1 && conn.p.clan != null) {
                        boolean check = false;
                        for (int i = 0; i < conn.p.clan.members.size(); i++) {
                            if (conn.p.clan.members.get(i).name.equals(conn.p.name)
                                    && (conn.p.clan.members.get(i).levelInclan == 1
                                            || conn.p.clan.members.get(i).levelInclan == 0)) {
                                check = true;
                                break;
                            }
                        }
                        if (check) {
                            if (conn.p.clan.get_ngoc() < 15) {
                                Service.send_box_ThongBao_OK(conn.p,
                                        "Không đủ 15 ruby băng để chat KTG");
                                return;
                            }
                            conn.p.clan.update_ruby(-15);
                            for (int i = 0; i < conn.p.clan.members.size(); i++) {
                                Player p0 = Map
                                        .get_player_by_name_allmap(conn.p.clan.members.get(i).name);
                                if (p0 != null) {
                                    Clan.send_money(p0, false);
                                }
                            }
                            Message m23 = new Message(-31);
                            m23.writer().writeByte(type);
                            m23.writer().writeUTF(conn.p.clan.name + ": " + text);
                            m23.writer().writeByte(0);
                            m23.writer().writeShort(conn.p.clan.icon);
                            for (Map[] mapall : Map.ENTRYS) {
                                for (Map map : mapall) {
                                    List<Player> snapshot500;
                                    synchronized (map.players) {
                                        snapshot500 = new ArrayList<>(map.players);
                                    }
                                    for (Player p0 : snapshot500) {
                                        if (p0 != null && p0.conn != null && !p0.isHideKTG) {
                                            p0.addmsg(m23);
                                        }
                                    }
                                }
                            }
                            m23.cleanup();
                        }
                    }
                }
                break;
            }
            case -48: {
                if (conn.p != null) {
                    UpgradeItem.process(conn.p, m);
                }
                break;
            }
            case -22: {
                if (conn.p != null) {
                    UseItem.process(conn.p, m);
                }
                break;
            }
            case -105: {
                if (conn.p != null) {
                    Service.request_item4_info(conn.p, m);
                }
                break;
            }
            case -21: {
                if (conn.p != null) {
                    Service.sell_item(conn.p, m);
                }
                break;
            }
            case 12: {
                if (conn.p != null) {
                    conn.p.map.pick_item(conn.p, m);
                }
                break;
            }
            case -42: {
                if (conn.p != null) {
                    String name = m.reader().readUTF();
                    Player p0 = Map.get_player_by_name_allmap(name);
                    if (p0 != null) {
                        conn.p.tempPlayer = p0; // Lưu đối tượng Player tạm thời vào tempPlayer
                        // Service.send_view_other_player(p0, conn.p);
                        MenuController.send_dynamic_menu(conn.p, 9997, "Thông Tin",
                                new String[] { "Trang bị 1", "Trang bị 2", "Trang bị 3" }, null);
                    }
                }
                break;
            }
            case 59: {
                if (conn.p != null) {
                    try {
                        byte action = m.reader().readByte();
                        short id = m.reader().readShort();
                        byte cat = m.reader().readByte();
                        List<Player> snapshot564;
                        synchronized (conn.p.map.players) {
                            snapshot564 = new ArrayList<>(conn.p.map.players);
                        }
                        Player target = null;
                        for (Player p0 : snapshot564) {
                            if (p0.index_map == id) {
                                target = p0;
                                break;
                            }
                        }
                        if (target != null && target != conn.p) {
                            if (conn.p.getTongNap() < 500000) {
                                Service.send_box_ThongBao_OK(conn.p,
                                        "Yêu cầu nạp tối thiểu 500.000 Coin để sử dụng chức năng tặng Ruby.");
                                return;
                            }
                            conn.p.tempRubyP0 = target;
                            Service.input_text(conn.p, 16, "Tặng Ruby (-30%)", new String[] {
                                    "Nhập Ruby tặng " + target.name
                            });
                        } else {
                            Service.send_box_ThongBao_OK(conn.p, "Không tìm thấy người chơi");
                        }
                    } catch (Exception e) {
                        GameLogger.error("MessageHandler case 59: Error in Ruby donation logic forUser: "
                                + (conn.p != null ? conn.p.name : "unknown"), e);
                    }
                }
                break;
            }

            case 14: {
                if (conn.p != null) {
                    byte type = m.reader().readByte();
                    byte act = m.reader().readByte();
                    if (act == 0) {
                        conn.p.map.change_flag(conn.p, type);
                    }
                }
                break;
            }
            case 6: {
                if (conn.p != null) {
                    conn.p.request_live_from_die(m);
                }
                break;
            }
            case -18: {
                if (conn.p != null) {
                    Service.buy_item(conn.p, m);
                }
                break;
            }
            case -5: {
                if (conn.p != null) {
                    short id = m.reader().readShort();
                    conn.p.map.send_char_in4_inmap(conn.p, id);
                }
                break;
            }
            case 46: {
                if (conn.p != null) {
                    Service.checkPlayInMap(conn.p, m);
                }
                break;
            }
            case 0: {
                if (conn.p != null) {
                    // int id_map_change =
                    m.reader().readShort();
                    // byte action_change =
                    m.reader().readByte();
                    while (conn.p.list_msg_cache.size() > 0) {
                        try {
                            Message m_send = conn.p.list_msg_cache.take();
                            conn.addmsg(m_send);
                        } catch (InterruptedException e) {
                            GameLogger.warn("MessageHandler case 0: Cache message taking interrupted", e);
                        }
                    }
                    Service.getThanhTich(conn.p, conn.p);
                    conn.p.map.send_in4_obj_inmap(conn.p);
                    conn.p.wait_change_map = false;
                    //
                    if (conn.p.map.template.id == 1000) {// map wait pvp
                        Pvp.show_table(conn.p);
                    }
                    if (conn.p.map.map_pvp != null) {// map pvp
                        Pvp.show_info(conn.p, 0, 0, 0, 3);
                    }
                    conn.p.map.change_flag(conn.p, conn.p.type_pk);
                    Service.update_PK(conn.p, conn.p, false);
                    // weather
                    conn.p.map.send_weather(conn.p);
                    // ship pet
                    if (conn.p.ship_pet != null && conn.p.ship_pet.map == null) {
                        conn.p.ship_pet.map = conn.p.map;
                        conn.p.ship_pet.id_map_save = conn.p.map.template.id;
                        conn.p.ship_pet.x = conn.p.x;
                        conn.p.ship_pet.y = conn.p.y;
                        Message m_local = new Message(1);
                        m_local.writer().writeByte(0);
                        m_local.writer().writeShort(conn.p.ship_pet.index_map);
                        m_local.writer().writeShort(conn.p.ship_pet.x);
                        m_local.writer().writeShort(conn.p.ship_pet.y);
                        List<Player> snapshot664;
                        synchronized (conn.p.map.players) {
                            snapshot664 = new ArrayList<>(conn.p.map.players);
                        }
                        for (Player p0 : snapshot664) {
                            if (p0 != null && p0.conn != null) {
                                p0.addmsg(m_local);
                            }
                        }
                        m_local.cleanup();
                    }
                    if (Map.is_map_dungeon(conn.p.map.template.id) && conn.p.dungeon != null) {
                        Service.send_time_cool_down(conn.p, conn.p.dungeon.time, "Thời gian", 2);
                    } else if (conn.p.map.template.id == 9999 && conn.p.map.clan_resource != null) {
                        Service.send_time_cool_down(conn.p, conn.p.map.clan_resource.time,
                                "Thời gian", 2);
                    } else if (conn.p.map.template.id == 81
                            && conn.p.map.map_little_garden != null) {
                        LittleGarden.send_info(conn.p);
                        Service.send_time_cool_down(conn.p, conn.p.map.map_little_garden.time,
                                "Thời gian", 2);
                    } else if (conn.p.map.map_pvp != null && conn.p.map.map_pvp.type_map == 1) {
                        conn.p.update_info_to_all();
                    } else if (conn.p.map.template.id == 119) { // phong cho truy na
                        Wanted.show_table(conn.p);
                        Service.Wanted(conn.p, false);
                        Wanted_Chest.send_box(conn.p);
                    } else if (conn.p.map.map_ThuThachVeThan != null) { // ttvt
                        for (int i = 0; i < conn.p.map.map_ThuThachVeThan.mobs.size(); i++) {
                            Mob mob = conn.p.map.map_ThuThachVeThan.mobs.get(i);
                            if (mob != null && !mob.isdie) {
                                Message m_local = new Message(1);
                                m_local.writer().writeByte(1);
                                m_local.writer().writeShort(mob.index);
                                m_local.writer().writeShort(mob.x);
                                m_local.writer().writeShort(mob.y);
                                if (conn.p != null && conn.p.conn != null) {
                                    conn.p.addmsg(m_local);
                                }
                                m_local.cleanup();
                            }
                        }
                        Service.send_time_cool_down(conn.p, conn.p.map.map_ThuThachVeThan.time_state,
                                "Tầng " + (conn.p.map.map_ThuThachVeThan.level), 0);
                        if (conn.p.isdie) {
                            Service.use_potion(conn.p, 0, conn.p.body.get_hp_max(true));
                            Service.use_potion(conn.p, 1, conn.p.body.get_mp_max(true));
                            conn.p.isdie = false;
                        }
                    } else if (conn.p.map.map_Hang != null) { // hang dong
                        for (int i = 0; i < conn.p.map.map_Hang.mobs.size(); i++) {
                            Mob mob = conn.p.map.map_Hang.mobs.get(i);
                            if (mob != null && !mob.isdie) {
                                Message m_local = new Message(1);
                                m_local.writer().writeByte(1);
                                m_local.writer().writeShort(mob.index);
                                m_local.writer().writeShort(mob.x);
                                m_local.writer().writeShort(mob.y);
                                if (conn.p != null && conn.p.conn != null) {
                                    conn.p.addmsg(m_local);
                                }
                                m_local.cleanup();
                            }
                        }
                        Service.send_time_cool_down(conn.p, conn.p.map.map_Hang.time,
                                "Hang " + (conn.p.map.map_Hang.level), 0);
                        if (conn.p.isdie) {
                            Service.use_potion(conn.p, 0, conn.p.body.get_hp_max(true));
                            Service.use_potion(conn.p, 1, conn.p.body.get_mp_max(true));
                            conn.p.isdie = false;
                        }
                    }
                    Clan clanTop = ChiemDao.getClanTop(conn.p.map.template.id);
                    if (clanTop != null) {
                        Message mm = new Message(63);
                        mm.writer().writeShort(clanTop.id);
                        mm.writer().writeShort(clanTop.icon);
                        mm.writer().writeUTF(clanTop.name);
                        mm.writer().writeUTF(clanTop.members.get(0).name);
                        mm.writer().writeShort(clanTop.level);
                        mm.writer().writeByte(clanTop.members.size());
                        mm.writer().writeByte(Clan.get_mem_max(clanTop.level, clanTop.trungsinh));
                        mm.writer().writeInt(1);
                        conn.addmsg(mm);
                        mm.cleanup();
                    }

                    if (conn.p.map.template.id == 303 || conn.p.map.template.id == 304) {
                        conn.p.map.change_flag(conn.p, -1);
                    }
                }
                break;
            }
            case 23: {
                if (conn.p != null && !conn.p.isdie) {
                    if (conn.p.ship_pet != null) {
                        Service.send_box_ThongBao_OK(conn.p,
                                "Không thể chuyến khu khi bạn đang vận chuyển hàng");
                    } else {
                        Service.area_select(conn.p, m);
                    }
                }
                break;
            }
            case 17: {
                if (conn.p != null) {
                    conn.p.map.send_chat(conn.p, m);
                }
                break;
            }
            case -20: {
                if (conn.p != null) {
                    MenuController.process_menu(conn.p, m);
                }
                break;
            }
            case -19: {
                if (conn.p != null) {
                    MenuController.send_menu(conn.p, m);
                }
                break;
            }
            case 2: {
                if (conn.p != null && conn.p.map != null) {
                    short idSkill = m.reader().readShort();
                    byte CatBeFire = m.reader().readByte();
                    byte size_target = m.reader().readByte();
                    List<Integer> targetIds = new ArrayList<>();
                    for (int i = 0; i < size_target; i++) {
                        int id_targett = m.reader().readShort();
                        targetIds.add(id_targett);
                    }
                    conn.p.map.use_skill(conn.p, idSkill, CatBeFire, size_target, targetIds);
                }
                break;
            }

            case -70: {
                if (conn.p != null) {
                    conn.p.map.update_num_player_in_map(conn.p);
                    Service.charWearing(conn.p, conn.p, false);
                }
                break;
            }
            case -45: {// update pk point
                if (conn.p != null) {
                    conn.p.update_point_pk(0);
                    Service.CountDown_Ticket(conn.p);
                    //
                    Service.charWearing(conn.p, conn.p, false);
                }
                break;
            }
            case -33: {
                if (conn.p != null) {
                    Service.rms_process(conn.p, m);
                }
                break;
            }
            case 1: {
                if (conn.p != null && conn.p.map != null) {
                    conn.p.map.send_move(conn.p, m.reader().readShort(), m.reader().readShort());
                }
                break;
            }
            case 4: {
                if (conn.p != null) {
                    Service.request_mob_in4(conn.p, m);
                }
                break;
            }
            case 48: {
                if (conn.p != null) {
                    Service.send_obj_template(conn.p, m);
                }
                break;
            }
            case -9: {
                if (conn.p == null) {
                    login(m);
                }
                break;
            }
            case -8: {
                conn.create_char(m);
                break;
            }
            case -51: {
                Service.send_icon(m, conn);
                break;
            }
            case -82: {// msg read art
                conn.ReadPartNew(m);
                break;
            }
            case -38: {
                conn.send_data_from_server(m);
                break;
            }
            case -2: {
                if (conn.user == null && conn.pass == null) {
                    conn.login(m);
                }
                break;
            }
            case -6: {
                conn.Check_Data_Ver();
                break;
            }
            case -7: {
                conn.request_data_update(m);
                break;
            }
        }
    }

    public void login(Message m2) throws IOException {
        short id = m2.reader().readShort();
        // byte type =
        m2.reader().readByte();
        // short idsupport =
        m2.reader().readShort();
        login_into_char_select(id);
    }

    public void login_into_char_select(short id) throws IOException {
        try {
            if (conn.list_char != null && id < conn.list_char.size()) {
                String charName = conn.list_char.get(id);
                Player p_online = Map.get_player_by_name_allmap(charName);

                // [HIJACK LOGIC] Nếu nhân vật đang treo offline
                if (p_online != null && p_online.isOfflineTraining) {
                    // 1. Swap Session & Stop AI AI (Dùng lock tối giản)
                    synchronized (p_online) {
                        p_online.conn = this.conn; 
                        this.conn.p = p_online; 
                        p_online.isOfflineTraining = false; 
                        p_online.targetMobOffline = null; 
                    }
                    
                    // 2. Các thao tác I/O & Sync thực hiện ngoài Lock để tránh Deadlock
                    Player.flush(p_online, true); 
                    sendLoginSyncPackets(p_online);

                    String timeRemain = core.Util.get_time_str_by_sec2(p_online.offlineTrainingEndTime - System.currentTimeMillis());
                    Service.send_box_ThongBao_OK(p_online,
                            "Chào mừng bạn quay trở lại!\nAI Ủy thác đã tạm dừng. Thời gian treo máy còn lại: " + timeRemain);

                    // 3. Sử dụng rejoin_map thay vì goto_map để tránh duplicate entry
                    p_online.map.rejoin_map(p_online); 
                    return;
                }

                Player p0 = new Player(conn, charName);
                if (!p0.setup()) {
                    conn.disconnect();
                    return;
                }
                p0.setin4();
                if (Map.get_player_by_name_allmap(p0.name) != null) {
                    conn.disconnect();
                    return;
                }
                conn.p = p0;
                io.SessionManager.registerPlayer(p0);
                Message m = new Message(-7);
                m.writer().writeByte(17);
                m.writer().writeLong(System.currentTimeMillis());
                conn.addmsg(m);
                m.cleanup();

                // Đồng bộ dữ liệu khởi tạo
                sendLoginSyncPackets(conn.p);

                if (conn.p.map == null) {
                    Map[] maplang = Map.get_map_by_id(1);
                    if (maplang != null && maplang.length > 0) {
                        conn.p.map = maplang[0];
                    }
                }
                if (conn.p.map != null) {
                    conn.p.map.goto_map(conn.p);
                } else {
                    conn.disconnect();
                    return;
                }
                sendInitialNotices(conn.p);
            } else {
                conn.disconnect();
            }
        } catch (Exception e) {
            GameLogger.error("MessageHandler: Error during login selection for ID " + id, e);
            conn.disconnect();
        }
    }

    private void sendInitialNotices(Player p) throws IOException {
        String notice = "Cảnh Báo Lừa Đảo : Hãy luôn nhớ không cung cấp thông tin tài khoản mật khẩu cho bất kì ai";
        Message ch = new Message(-31);
        ch.writer().writeByte(1);
        ch.writer().writeUTF(notice);
        ch.writer().writeByte(5);
        p.list_msg_cache.add(ch);
        ch.cleanup();
        String notice2 = "Truy cập trang chủ " + Manager.gI().server_domain + " để nạp tiền ủng hộ cho game phát triển";
        Message ch2 = new Message(-31);
        ch2.writer().writeByte(1);
        ch2.writer().writeUTF(notice2);
        ch2.writer().writeByte(1);
        p.list_msg_cache.add(ch2);
        ch2.cleanup();
        int khuyenmai = Manager.gI().xnap;
        if (khuyenmai > 0) {
            String notice3 = "Đang diễn ra khuyến mãi nạp X" + khuyenmai + " hãy nhanh tay nạp để được ưu đãi";
            Message ch3 = new Message(-31);
            ch3.writer().writeByte(1);
            ch3.writer().writeUTF(notice3);
            ch3.writer().writeByte(3);
            p.list_msg_cache.add(ch3);
            ch3.cleanup();
        }
        sendGiftCodeNotice(p);
    }

    private void sendGiftCodeNotice(Player p) throws IOException {
        StringBuilder giftCodes = new StringBuilder("Danh sách GiftCode hiện tại :\n");
        int count = 0;
        for (int i = 0; i < template.GiftTemplate.ENTRY.size(); i++) {
            if (template.GiftTemplate.ENTRY.get(i).visible) {
                count++;
                giftCodes.append(count).append(". ").append(template.GiftTemplate.ENTRY.get(i).giftname).append("\n");
            }
        }
        Chat.send_chat(p, "Thông báo", giftCodes.toString(), true);
    }
    private void sendLoginSyncPackets(Player p) throws IOException {
        Service.UpdateInfoMaincharInfo(p);
        Service.Main_char_Info(p);
        Service.UpdatePvpPoint(p);
        Service.update_PK(p, p, false);
        Service.getThanhTich(p, p);
        p.item.send_maxbag_Inventory();
        Service.update_PK(p, p, true);
        Service.pet(p, p, true);
        p.item.update_Inventory(-1, true);
        p.item.update_assets_Inventory(true);
        Service.ChestWanted(p, true);
        p.item.send_maxbox_Inventory();
        p.item.update_assets_Box(true);
        p.item.update_Inventory_box(-1, true);
        Quest.send_List_Quest(p, true);
        Quest.update_map_have_side_quest(p, true);
        Service.Weapon_fashion(p, p, true);
        Service.charWearing(p, p, true);
        p.map.send_boat(p, true);
        p.map.update_boat(p, p, true);
        Service.login_ok(p, true);
        Service.Wanted(p, true);
        Clan.send_info(p, true);
        p.item.update_assets_Inventory(true);
    }

}