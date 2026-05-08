package core;

import activities.Rebuild_Item;
import client.*;
import io.Message;
import io.Session;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import java.io.File;
import map.Map;
import map.Mob;
import map.f;
import template.*;

public class Service {

    // ConcurrentHashMap: cho phép đọc đồng thời không block, tránh lag do lock
    // contention
    private static final java.util.concurrent.ConcurrentHashMap<String, byte[]> iconCache = new java.util.concurrent.ConcurrentHashMap<>(
            2048, 0.75f, 4);
    private static final int MAX_CACHE_SIZE = 5000; // giới hạn tối đa
    private static final byte[] DUMMY_ICON = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00,
            0x0D, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00,
            0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C,
            0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
            0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82 };

    public static void send_item4_cansell(Session conn) throws IOException {
        if (conn == null) return;
        Message m = new Message(72);
        m.writer().writeByte(1);
        m.writer().writeShort(ItemTemplate4.ENTRYS.size());
        for (int i = 0; i < ItemTemplate4.ENTRYS.size(); i++) {
            m.writer().writeShort(ItemTemplate4.ENTRYS.get(i).id);
        }
        m.writer().writeShort(ItemTemplate7.ENTRYS.size());
        for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
            m.writer().writeShort(ItemTemplate7.ENTRYS.get(i).id);
        }
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_lechhead_data(Session conn) throws IOException {
        if (conn == null) return;
        Message m = new Message(72);
        m.writer().writeByte(0);
        short[] fullList = { 719, 748, 751, 756, 798, 799, 801, 802, 849, 851, 894, 896, 950, 963, 972, 1053, 818 };

        m.writer().writeShort(fullList.length);
        for (short item : fullList) {
            m.writer().writeShort(item);
        }
        conn.addmsg(m);
        m.cleanup();
    }

    public static void send_msg_data(Session conn, int cmd, String path, boolean save_cache)
            throws IOException {
        if (conn == null) return;
        Message m = new Message(cmd);
        m.writer().write(Util.loadfile(path));
        if (save_cache) {
            conn.p.list_msg_cache.add(m);
        } else {
            conn.addmsg(m);
        }
        m.cleanup();
    }

    public static void UpdateInfoMaincharInfo(Player p) throws IOException {
        if (p == null || p.conn == null)
            return;
        Message m = new Message(-75);
        int id_f = -1;
        for (int i = 0; i < p.fashion.size(); i++) {
            if (p.fashion.get(i).is_use) {
                id_f = p.fashion.get(i).id;
                break;
            }
        }
        m.writer().writeShort(id_f); // id fashion
        long par_agility = p.body.get_agility(false);
        m.writer().writeShort(f.setInteger(par_agility)); // giam cooldown skill : % giam hoi chieu
        m.writer().writeShort(p.get_percent_mana_use_skill());
        p.addmsg(m);
        m.cleanup();
    }

    public static void Main_char_Info(Player p) throws IOException {
        Main_char_Info(p, p);
    }

    public static void Main_char_Info(Player p, Player target) throws IOException {

        long hp_max = p.body.get_hp_max(true);
        long mp_max = p.body.get_mp_max(true);
        if (p.hp > hp_max) {
            p.hp = hp_max;
        }
        if (p.mp > mp_max) {
            p.mp = mp_max;
        }
        Message m = new Message(-10);
        m.writer().writeShort(p.index_map);
        m.writer().writeUTF(p.getNameShow());
        m.writer().writeInt(f.setInteger(hp_max));
        m.writer().writeInt(f.setInteger(mp_max));
        m.writer().writeInt(f.setInteger(p.hp));
        m.writer().writeInt(f.setInteger(p.mp));
        m.writer().writeShort(p.level);
        m.writer().writeShort(p.get_level_percent());
        m.writer().writeShort((short) Math.min(32000, p.thongthao));
        if (p.level >= 100) {
            m.writer().writeShort(p.get_level_percent());
        } else {
            m.writer().writeShort(0);
        }
        m.writer().writeInt(BXH.get_rank_wanted(p.name));
        m.writer().writeByte(p.clazz);
        m.writer().writeInt(p.pointPk);
        m.writer().writeShort((short) Math.min(32000, p.pointAttribute));
        m.writer().writeByte(p.typePirate);
        m.writer().writeByte(p.indexGhostServer);
        m.writer().writeByte(p.getNumPassive());
        m.writer().writeByte(p.body.get_level_perfect());
        //
        m.writer().writeByte(Body.NameAttribute.length); // size
        long p_, p_pluss;
        for (int i = 0; i < Body.NameAttribute.length; i++) {
            m.writer().writeUTF(Body.NameAttribute[i]);
            p_ = i == 0 ? p.point1
                    : (i == 1 ? p.point2 : (i == 2 ? p.point3 : (i == 3 ? p.point4 : p.point5)));
            p_pluss = p.body.get_point_plus(i + 1);
            m.writer().writeShort(f.setInteger(p_));
            m.writer().writeShort(f.setInteger(p_pluss));
            int dem = 0;
            long[] par_show = new long[Body.Id[i].length];
            int index = Math.max(0, (int) (p_ + p_pluss - 1));
            for (int j = 0; j < Body.Id[i].length; j++) {
                int attrId = Body.Id[i][j];
                switch (attrId) {
                    case 1:
                        par_show[j] = Body.Point1_Template_atk[index];
                        break;
                    case 10:
                        par_show[j] = Body.Point1_Template_crit[index];
                        break;
                    case 13:
                        par_show[j] = Body.Point1_Template_pierce[index];
                        break;
                    case 4:
                        par_show[j] = Body.Point2_Template_def_percent[index];
                        break;
                    case 3:
                        par_show[j] = Body.Point2_Template_def[index];
                        break;
                    case 26:
                        par_show[j] = Body.Point2_Template_resist_physical[index];
                        break;
                    case 27:
                        par_show[j] = Body.Point2_Template_resist_magic[index];
                        break;
                    case 50:
                        par_show[j] = Body.Point2_Template_reduce_pierce[index];
                        break;
                    case 14:
                        par_show[j] = Body.Point2_Template_counter_attack[index];
                        break;
                    case 15:
                        par_show[j] = Body.Point3_Template_hp[index];
                        break;
                    case 17:
                        par_show[j] = Body.Point3_Template_hp_percent[index];
                        break;
                    case 19:
                        par_show[j] = Body.Point3_Template_hp_regen[index];
                        break;
                    case 21:
                        par_show[j] = Body.Point3_Template_lifesteal[index];
                        break;
                    case 23:
                        par_show[j] = Body.Point3_Template_hp_potion[index];
                        break;
                    case 16:
                        par_show[j] = Body.Point4_Template_mp[index];
                        break;
                    case 18:
                        par_show[j] = Body.Point4_Template_mp_percent[index];
                        break;
                    case 20:
                        par_show[j] = Body.Point4_Template_mp_regen[index];
                        break;
                    case 22:
                        par_show[j] = Body.Point4_Template_mp_steal[index];
                        break;
                    case 24:
                        par_show[j] = Body.Point4_Template_mp_potion[index];
                        break;
                    case 11:
                        par_show[j] = Body.Point4_Template_dame_crit[index];
                        break;
                    case 25:
                        if (index >= 0) {
                            par_show[j] = Body.Point5_Template_cooldown[index];
                        }
                        break;
                    case 12:
                        if (index >= 0) {
                            par_show[j] = Body.Point5_Template_miss[index];
                        }
                        break;
                    case 67:
                        par_show[j] = Body.Point5_Template_exp_bonus[index];
                        break;
                    case 72:
                        par_show[j] = Body.Point5_Template_beri_bonus[index];
                        break;
                }
                if (par_show[j] > 0) {
                    dem++;
                }
            }

            m.writer().writeByte(dem);
            for (int j = 0; j < Body.Id[i].length; j++) {
                if (par_show[j] > 0) {
                    m.writer().writeByte(Body.Id[i][j]);
                    m.writer().writeInt(f.setInteger(par_show[j]));
                }
            }
        }
        //
        m.writer().writeShort(p.pointSkill);
        m.writer().writeByte(10);
        for (int i = 0; i < 10; i++) {
            m.writer().writeByte(1);
            m.writer().writeByte(1);
        } // Nơi hiển thị các chỉ số cho người chơi xem
        byte[] a = new byte[] { 0, 1, 2, 3, 4, 25, 15, 16, 26, 27, 17, 18, 10, 11, 12, 13, 14, 19,
                20, 21, 22, 23, 24, 46, 48, 47, 49, 50, 51, 52, 53, 63, 55, 56, 57, 58, 59, 62, 67,
                68, 69, 70, 71, 72, 73, 74, 75, 76, 91, 92, 93, 94, 95 };
        int[] b = new int[a.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = f.setInteger(p.body.view_in4(a[i]));
        }
        int dem = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i] > 0 || a[i] == 15 || a[i] == 16 || a[i] == 17 || a[i] == 18 || a[i] == 26
                    || a[i] == 27 || a[i] == 53 || a[i] == 10 || a[i] == 11 || a[i] == 12
                    || a[i] == 2) {
                dem++;
            }
        }
        m.writer().writeByte(dem);
        for (int i = 0; i < a.length; i++) {
            if (b[i] > 0 || a[i] == 15 || a[i] == 16 || a[i] == 17 || a[i] == 18 || a[i] == 26
                    || a[i] == 27 || a[i] == 53 || a[i] == 10 || a[i] == 11 || a[i] == 12
                    || a[i] == 2) {
                m.writer().writeByte(a[i]);
                m.writer().writeInt(f.setInteger(b[i]));
            }
        }
        m.writer().writeByte(p.getEffFashion());
        m.writer().writeByte(-1);
        m.writer().writeByte(-1);
        m.writer().writeByte(p.getEffHair());
        // Đây là cho phiên bản 129
        // m.writer().writeShort(-1);
        // m.writer().writeShort(-1);
        // m.writer().writeShort(-1);
        // m.writer().writeShort(-1);
        target.addmsg(m);
        m.cleanup();
    }

    public static void UpdatePvpPoint(Player p) throws IOException {
        Message m = new Message(-66);
        m.writer().writeInt(p.get_pvpPoint());
        m.writer().writeInt(p.pvp_win); // win
        m.writer().writeInt(p.pvp_lose); // lose
        p.addmsg(m);
        m.cleanup();
    }

    public static void update_PK(Player p0, Player p, boolean save_cache) throws IOException {
        if (p == null || p.conn == null)
            return;
        Message m = new Message(14);
        m.writer().writeShort(p0.index_map);
        m.writer().writeByte(p0.type_pk); // type pk
        m.writer().writeByte(p0.typePirate); // type pirate
        m.writer().writeByte(p0.is_show_hat ? 0 : 1); // dont show hat
        m.writer().writeShort(-1);
        m.writer().writeByte(p0.is_show_weapon ? 0 : 1); // dont show weaponF
        m.writer().writeByte(p0.hasFullSet16() ? 1 : 0);

        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void getThanhTich(Player p0, Player p) throws IOException {
        if (p == null || p.conn == null)
            return;
        Message m = new Message(65);
        m.writer().writeShort(p0.index_map);
        m.writer().writeByte(0);
        //
        boolean hasUsedDanhHieu = false;

        // Duyệt qua danh sách danh hiệu của người chơi
        for (int j = 0; j < p0.danh_hieu.size(); j++) {
            MyDanhHieu myDanhHieu = p0.danh_hieu.get(j);

            // Kiểm tra nếu danh hiệu đang được sử dụng
            if (myDanhHieu.isUsed) {
                DanhHieu danhHieu = DanhHieu.getTemplate(myDanhHieu.id);
                if (danhHieu != null && danhHieu.eff != -1) { // Kiểm tra danh hiệu hợp lệ và có hiệu ứng
                    hasUsedDanhHieu = true;
                    break; // Không cần kiểm tra thêm, chỉ cần biết có danh hiệu được bật
                }
            }
        }

        // Nếu có danh hiệu đang được sử dụng, thành tích là -1, nếu không, lấy từ BXH
        m.writer().writeByte(hasUsedDanhHieu ? -1 : BXH.get_Thanh_tich_pvp(p0)); // PVP
        m.writer().writeByte(hasUsedDanhHieu ? -1 : BXH.get_Thanh_tich_level(p0)); // Level
        //
        m.writer().writeByte(p0.get_index_full_set());
        p.addmsg(m);
        m.cleanup();
    }

    public static void Weapon_fashion(Player p0, Player p, boolean save_cache) throws IOException {
        if (p == null || p.conn == null)
            return;
        Message m = new Message(-104);
        m.writer().writeShort(p0.index_map);
        m.writer().writeByte(0);
        m.writer().writeByte(6);
        m.writer().writeShort(p0.head);
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void Send_UI_Shop(Player p, int type) throws IOException {
        p.isShop = true;
        Message m = new Message(-19);
        m.writer().writeByte(type);
        switch (type) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4: {
                m.writer().writeUTF(Manager.NAME_ITEM_SELL_TEMP[type]);
                m.writer().writeByte(3);
                List<ItemSell> list_sell = ItemSell.get_it_sell(p.level, type);
                m.writer().writeShort(list_sell.size());
                for (int i = 0; i < list_sell.size(); i++) {
                    ItemSell it_sell_temp = list_sell.get(i);
                    ItemTemplate3 it_temp = ItemTemplate3.get_it_by_id(it_sell_temp.id);
                    ItemTemplate3.readUpdateItem(m.writer(), it_temp);
                    m.writer().writeByte(0);
                    m.writer().writeInt(it_sell_temp.price);
                }
                break;
            }
            case 6: {
                m.writer().writeUTF("Shop Nguyên liệu");
                m.writer().writeByte(7);
                byte[] id_sell = ItemSell.get_it_sell_material();
                m.writer().writeShort(id_sell.length);
                for (int i = 0; i < id_sell.length; i++) {
                    m.writer().writeByte(id_sell[i]);
                    m.writer().writeShort(1);
                }
                break;
            }
            case 20: {
                m.writer().writeUTF("Quán ăn");
                m.writer().writeByte(4);
                short[] id_sell = ItemSell.get_it_sell_potion(p);
                m.writer().writeShort(id_sell.length);
                for (int i = 0; i < id_sell.length; i++) {
                    m.writer().writeShort(id_sell[i]);
                    m.writer().writeShort(1);
                }
                break;
            }
            case 99: {
                m.writer().writeUTF("Rương đồ");
                m.writer().writeByte(99);
                m.writer().writeShort(0);
                break;
            }
            case 111: {
                m.writer().writeUTF("Shop Đá");
                m.writer().writeByte(4);
                m.writer().writeShort(Rebuild_Item.ID_SELL.length);
                for (int i = 0; i < Rebuild_Item.ID_SELL.length; i++) {
                    m.writer().writeShort(Rebuild_Item.ID_SELL[i]);
                    m.writer().writeShort(1);
                }
                break;
            }
            case 119: {
                m.writer().writeUTF("Thùng Rác");
                m.writer().writeByte(3);
                m.writer().writeShort(p.item.save_item_wear.size());
                for (int i = p.item.save_item_wear.size() - 1; i >= 0; i--) {
                    Item_wear it_select = p.item.save_item_wear.get(i);
                    if (it_select != null) {
                        it_select.index = (byte) i;
                        Item.readUpdateItem(m.writer(), it_select, p);
                        m.writer().writeByte(0);
                        m.writer().writeInt(0);
                    }
                }
                break;
            }
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void charWearing(Player p0, Player p, boolean save_cache) throws IOException {
        if (p == null || p.conn == null)
            return;

        Message m = new Message(19);
        m.writer().writeShort(p0.index_map);
        m.writer().writeByte(0);
        boolean isSkin = p0.skinId != -1;
        m.writer().writeShort(isSkin ? -1 : p0.get_head());
        m.writer().writeShort(isSkin ? -1 : p0.get_hair());
        m.writer().writeByte(8);

        short[] fashion = p0.get_fashion();

        for (int i = 0; i < 8; i++) {
            Item_wear it_w = p0.item.it_body[i];
            // [RECOGNITION] Support Set 2 and Set 3 appearance if Main Set slot is empty
            if (it_w == null && i < p0.item.second_body.length) {
                it_w = p0.item.second_body[i];
            }
            if (it_w == null && i < p0.item.third_body.length) {
                it_w = p0.item.third_body[i];
            }

            if (it_w != null) {
                m.writer().writeByte(1);
                if (p0.index_map == p.index_map) {
                    Item.readUpdateItem(m.writer(), it_w, p0);
                }
                if (isSkin || (i == 1 && !p0.is_show_hat) || (i == 0 && !p0.is_show_weapon)) {
                    m.writer().writeShort(-1);
                } else if (fashion != null && fashion[i] != -1) {
                    m.writer().writeShort(fashion[i]);
                } else {
                    m.writer().writeShort(it_w.template.part);
                }
            } else {
                m.writer().writeByte(0);
                m.writer().writeShort(-1);
            }
        }
        m.writer().writeShort(isSkin ? -1 : p0.getEffFashion()); // eff body
        m.writer().writeShort(-1); // eff leg
        m.writer().writeShort(-1); // eff vu khi
        m.writer().writeShort(isSkin ? -1 : p0.getEffHair()); // eff hair
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void send_icon(Message m, Session conn) {
        if (conn.user == null || conn.pass == null || conn.user.isEmpty() || conn.pass.isEmpty())
            return;
        long now = System.currentTimeMillis();
        if (now - conn.lastIconCountReset > 1000) {
            conn.iconRequestCount = 0;
            conn.lastIconCountReset = now;
        }
        conn.iconRequestCount++;
        int limit = Manager.gI().spam_icon_limit;
        if (limit != -1 && conn.iconRequestCount > limit) {
            System.out.println("Warning " + conn.ipAddress + " - spam icon " +
                    conn.iconRequestCount + "/s");
            conn.disconnect();
            return;
        }

        try {
            short id = m.reader().readShort();
            short id_request = id;
            if (id_request < 0 || id_request > 32000) {
                return;
            }

            // Known problematic icons that cause client to crash or get stuck in a loop
            // NOTE: 0.png is a valid mob sprite (Mob 0 Sói rừng), do NOT override it!
            if (id_request == 23088 || id_request == 23089 || id_request == 24424)
                // || id_request == 28232 ||
                // id_request == 11916 || id_request == 28257 || id_request == 28237 ||
                // id_request == 28221 ||
                // id_request == 28255 || id_request == 28256 || id_request == 28254 ||
                // id_request == 28236 ||
                // id_request == 4517 || id_request == 4518 || id_request == 4543 || id_request
                // == 4559) {
                return;
            if (id_request > 4912 && id_request < 4935)
                id_request = 4912;

            byte zoomLv = (conn.zoomlv == 0) ? 1 : conn.zoomlv;
            String path = "data/icon/x" + zoomLv + "/" + id_request + ".png";

            // ConcurrentHashMap.get() không cần lock, không gây contention
            byte[] dataImg = iconCache.get(path);
            if (dataImg == null) {
                // Đọc file NGOÀI lock, chỉ put vào cache sau khi đọc xong
                File f = new File(path);
                if (f.exists()) {
                    dataImg = Util.loadfile(path);
                } else {
                    String fallbackPath = "data/icon/x" + zoomLv + "/1.png";
                    dataImg = iconCache.get(fallbackPath);
                    if (dataImg == null) {
                        dataImg = Util.loadfile(fallbackPath);
                        if (dataImg != null) {
                            iconCache.putIfAbsent(fallbackPath, dataImg);
                        } else {
                            dataImg = DUMMY_ICON;
                        }
                    }
                }
                if (dataImg != null && dataImg != DUMMY_ICON) {
                    // Nếu cache quá lớn, xóa bớt để tránh OOM
                    if (iconCache.size() > MAX_CACHE_SIZE) {
                        iconCache.clear();
                    }
                    iconCache.putIfAbsent(path, dataImg);
                }
            }

            if (dataImg == null) {
                dataImg = DUMMY_ICON;
            }

            Message m2 = new Message(dataImg.length < 32000 ? (byte) -51 : (byte) -101);
            m2.writer().writeShort(id);
            m2.writer().write(dataImg);
            conn.addmsg(m2);
            m2.cleanup();

        } catch (IOException e) {
            core.GameLogger.error("!!! Exception in send_icon: " + e.getMessage());
        }
    }

    public static void send_obj_template(Player p, Message m) throws IOException {
        byte type = m.reader().readByte();
        short id = m.reader().readShort();
        if (type == 98) {
            Message m2 = new Message(48);
            m2.writer().writeByte(98);
            m2.writer().writeShort(id);
            m2.writer().write(Util.loadfile("data/template/98/" + id));
            p.addmsg(m2);
            m2.cleanup();
        } else if (type == 1) {
            // Message m2 = new Message(48);
            // m2.writer().writeByte(1);
            // m2.writer().writeShort(id);
            // m2.writer().write(Util.loadfile("data/template/1/" + id));
            // p.addmsg(m2);
            // m2.cleanup();
        } else if (type == 97) {
            Message m2 = new Message(48);
            m2.writer().writeByte(97);
            m2.writer().writeShort(id);
            m2.writer().write(Util.loadfile("data/template/97/" + id));
            p.addmsg(m2);
            m2.cleanup();
        } else if (type == 96 && id < DataTemplate.AttriKichAn.length) {
            Message m2 = new Message(48);
            m2.writer().writeByte(96);
            m2.writer().writeByte(id);
            m2.writer().writeUTF(DataTemplate.AttriKichAn[id]);
            p.addmsg(m2);
            m2.cleanup();
        } else if (type == 4) {
            ItemTemplate4 it4 = ItemTemplate4.get_it_by_id(id);
            if (it4 != null) {
                Message m2 = new Message(48);
                m2.writer().writeByte(4);
                m2.writer().writeShort(it4.id);
                m2.writer().writeShort(it4.icon);
                m2.writer().writeUTF(it4.name);
                m2.writer().writeShort(it4.indexInfoPotion);
                m2.writer().writeInt(it4.beri);
                m2.writer().writeShort(it4.ruby);
                m2.writer().writeByte(it4.istrade);
                m2.writer().writeByte(it4.type);
                m2.writer().writeShort(it4.timedelay);
                m2.writer().writeShort(it4.value);
                m2.writer().writeShort(it4.timeactive);
                m2.writer().writeUTF(it4.nameuse);
                p.addmsg(m2);
                m2.cleanup();
            }
        }
    }

    public static void request_mob_in4(Player p, Message m2) throws IOException {
        int id = m2.reader().readShort();
        Mob temp = Mob.ENTRYS.get(id);
        if (temp == null && p.map.map_little_garden != null) {
            temp = p.map.get_mobs(id, 0);
        }
        if (temp == null && p.map.map_LienTang != null) {
            for (int i = 0; i < p.map.map_LienTang.mobs.size(); i++) {
                Mob mob = p.map.map_LienTang.mobs.get(i);
                if (!mob.isdie && mob.index == id) {
                    temp = mob;
                    break;
                }
            }
        }
        if (temp == null && p.map.map_LanhDiaBang != null) {
            for (int i = 0; i < p.map.map_LanhDiaBang.mobs.size(); i++) {
                Mob mob = p.map.map_LanhDiaBang.mobs.get(i);
                if (!mob.isdie && mob.index == id) {
                    temp = mob;
                    break;
                }
            }
        }
        if (temp == null && p.map.map_ThuThachVeThan != null) {
            for (int i = 0; i < p.map.map_ThuThachVeThan.mobs.size(); i++) {
                Mob mob = p.map.map_ThuThachVeThan.mobs.get(i);
                if (!mob.isdie && mob.index == id) {
                    temp = mob;
                    break;
                }
            }
        }
        if (temp == null && p.map.map_Hang != null) {
            for (int i = 0; i < p.map.map_Hang.mobs.size(); i++) {
                Mob mob = p.map.map_Hang.mobs.get(i);
                if (!mob.isdie && mob.index == id) {
                    temp = mob;
                    break;
                }
            }
        }
        if (temp == null && p.map.map_VoHanLienTang != null) {
            for (int i = 0; i < p.map.map_VoHanLienTang.mobs.size(); i++) {
                Mob mob = p.map.map_VoHanLienTang.mobs.get(i);
                if (!mob.isdie && mob.index == id) {
                    temp = mob;
                    break;
                }
            }
        }
        if (temp != null && !temp.isdie && temp.map.equals(p.map)) {
            send_mob_info(p, temp);
        }
    }

    public static void send_mob_info(Player p, Mob temp) throws IOException {
        Message m = new Message(4);
        m.writer().writeShort(temp.index);
        m.writer().writeShort(temp.mob_template.mob_id); // id mob
        m.writer().writeShort(temp.x);
        m.writer().writeShort(temp.y);
        m.writer().writeShort(temp.level); // lv
        m.writer().writeInt(f.setInteger(temp.hp));
        m.writer().writeInt(f.setInteger(temp.hp_max));
        m.writer().writeShort(temp.mob_template.skill[0]);
        int respawnTime;
        if (Map.is_map_treo(temp.map.template.id)) {
            respawnTime = 60;
        } else if (Map.is_map_banh_kem(temp.map.template.id)) {
            respawnTime = 30;
        } else {
            respawnTime = Skill_info.GLOBAL_MOB_RESPAWN_TIME;
        }
        if (temp.mob_template.mob_id == 132) {
            respawnTime = 32000;
        }
        m.writer().writeShort(respawnTime); // tgian hs
        m.writer().writeByte(temp.mob_template.typemonster); // type mons
        m.writer().writeByte(0); // lvthongthao
        p.addmsg(m);
        m.cleanup();
    }

    public static void rms_process(Player p, Message m2) {
        byte type = -1;
        byte id = -1;
        try {
            type = m2.reader().readByte();
            id = m2.reader().readByte();
            if (id < 0 || id >= p.rms.length) {
                core.GameLogger.error("[RMS] Invalid ID " + id + " for player " + p.name);
                return;
            }
            if (type == 0) {
                if (id == 0) {
                    boolean check = false;
                    for (int i = 0; i < DataTemplate.mSea.length; i++) {
                        if (DataTemplate.mSea[i][1] == p.map.template.id) {
                            check = true;
                            break;
                        }
                    }
                    if (check) {
                        return;
                    }
                }
                Message m = new Message(-33);
                m.writer().writeByte(id);
                m.writer().writeShort(p.rms[id].length);
                m.writer().write(p.rms[id]);
                p.addmsg(m);
                m.cleanup();
            } else if (type == 1) {
                int size = m2.reader().readShort();
                if (size > 0) {
                    p.rms[id] = new byte[size];
                    for (int i = 0; i < p.rms[id].length; i++) {
                        p.rms[id][i] = m2.reader().readByte();
                    }
                }
            }
        } catch (IOException e) {
            String idStr = (id != -1) ? String.valueOf(id) : "unknown";
            GameLogger.warn("[RMS] Lỗi xử lý bản ghi #" + idStr + " (type " + type + ") cho player " + p.name + ": "
                    + e.getMessage());
        }
    }

    public static void area_select(Player p, Message m2) throws IOException {
        // byte type =
        m2.reader().readByte();
        byte select = m2.reader().readByte();
        Map[] map_enter = Map.get_map_by_id(p.map.template.id);
        if (select > -1 && select < map_enter.length) {
            if (map_enter[select].equals(p.map)) {
                Service.send_box_ThongBao_OK(p, "Hiện tại đang ở khu này");
            } else {
                if (map_enter[select].players.size() >= map_enter[select].template.max_player) {
                    Service.send_box_ThongBao_OK(p, "Hiện tại khu vực đã đầy, hãy thử lại sau!");
                } else {
                    if (select >= 0) {
                        // Tiến hành chuyển map
                        p.map.leave_map(p, 1);
                        p.map = map_enter[select];
                        p.map.enter_map(p);
                        p.map.enter_zone(p);
                    }

                }
            }
        }
    }

    public static void pet(Player p0, Player p, boolean save_cache) throws IOException {
        if (p == null || p.conn == null)
            return;
        int petId = p0.get_pet();
        if (petId != -1 && p.isPetVisible) {
            Pet petTemplate = Pet.getTemplate(petId);
            if (petTemplate != null) {
                Message m = new Message(-80);
                m.writer().writeByte(0); // Thông báo hiển thị pet
                m.writer().writeShort(0); // Hiệu ứng
                m.writer().writeShort(p0.index_map); // Vị trí trên bản đồ
                m.writer().writeShort(petTemplate.frame); // Frame của pet
                m.writer().writeByte(petTemplate.type); // Loại pet
                if (save_cache) {
                    p.list_msg_cache.add(m);
                } else {
                    p.addmsg(m);
                }
                m.cleanup();
            }
        } else {
            Message m = new Message(-80);
            m.writer().writeByte(1);
            m.writer().writeShort(-1);
            m.writer().writeShort(p0.index_map);
            if (save_cache) {
                p.list_msg_cache.add(m);
            } else {
                p.addmsg(m);
            }
            m.cleanup();
        }
    }

    public static void resetAndShowPet(Player p) throws IOException {
        int petId = p.get_pet();
        if (!p.isPetVisible || petId == -1)
            return;
        Pet petTemplate = Pet.getTemplate(petId);
        if (petTemplate != null && petTemplate.type != 4) {
            Message m_hide = new Message(-80);
            m_hide.writer().writeByte(1);
            m_hide.writer().writeShort(-1);
            m_hide.writer().writeShort(p.index_map);
            p.list_msg_cache.add(m_hide);
            m_hide.cleanup();
        }
    }

    public static void login_ok(Player p, boolean save_cache) throws IOException {
        Message m = new Message(-2);
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void checkPlayInMap(Player p, Message m2) {
        try {
            m2.reader().readShort();
        } catch (IOException e) {
            GameLogger.warn("checkPlayInMap fail for map " + p.map.template.id, e);
        }
    }

    public static void buy_item(Player p, Message m2) throws IOException {
        byte TypeShop = m2.reader().readByte();
        short id = m2.reader().readShort();
        short value = m2.reader().readShort();
        if (TypeShop == 121) {
            ShopExtol temp_shop = ShopExtol.get_temp_id(id);
            if (temp_shop != null) {
                p.id_select_temp = id;
                Service.send_box_yesno(p, 121, "Thông báo",
                        "Bạn có muốn mua danh hiệu " + temp_shop.info + " với giá "
                                + Util.number_format(temp_shop.price) + " Extol không?",
                        new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
            }
            return;
        }
        byte cat = -1;
        if (TypeShop == 116 || TypeShop == 118) {
            cat = m2.reader().readByte();
        }
        if (TypeShop != 116 && TypeShop != 118 && (value == 1 || value == 20 || value == 500) && p.soluongmua > 0) {
            value = (short) p.soluongmua;
        }
        if (value <= 0 || value > DataTemplate.MAX_ITEM_IN_BAG) {
            Service.send_box_ThongBao_OK(p, "Số lượng không hợp lệ!");
            return;
        }
        boolean check = false;
        if (cat == -1 && TypeShop >= 0 && TypeShop < 5) {
            List<ItemSell> list_sell = ItemSell.get_it_sell(p.level, TypeShop);
            for (int i = 0; i < list_sell.size(); i++) {
                ItemSell temp_sell = list_sell.get(i);
                if (temp_sell != null && temp_sell.id == id) {
                    if (p.item.able_bag() > 0) {
                        if (p.get_vang() < temp_sell.price) {
                            Service.send_box_ThongBao_OK(p,
                                    "Bạn không đủ " + temp_sell.price + " beri!");
                            return;
                        }
                        p.update_vang(-temp_sell.price);
                        p.update_money();
                        Item_wear it_add = new Item_wear();
                        it_add.setup_template_by_id(temp_sell.id);
                        if (it_add.template != null) {
                            p.item.add_item_bag3(it_add);
                        }
                        p.item.update_Inventory(-1, false);
                        check = true;
                    } else {
                        Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
                        return;
                    }
                    break;
                }
            }
            //
            if (check) {
                Service.send_box_ThongBao_OK(p,
                        "Mua thành công " + ItemTemplate3.get_it_by_id(id).name);
            } else {
                Service.send_box_ThongBao_OK(p, "Mua thất bại, hãy thử lại!");
            }
        } else if (cat == -1 && TypeShop == 20) {
            if (ItemSell.check_item_sell_potion(id)) {
                if ((p.item.able_bag() < 1 && p.item.total_item_bag_by_id(4, id) == 0)
                        || ((p.item.total_item_bag_by_id(4, id)
                                + value) > DataTemplate.MAX_ITEM_IN_BAG)) {
                    Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
                    return;
                }
                ItemTemplate4 it_template = ItemTemplate4.get_it_by_id(id);
                if (it_template != null) {
                    int vang_req = it_template.ruby * value;
                    if (vang_req > 0) {
                        if (p.get_ngoc() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Ruby");
                            return;
                        }
                        p.update_ngoc(-vang_req);
                    } else {
                        vang_req = it_template.beri * value;
                        if (vang_req <= 0) {
                            return;
                        }
                        if (p.get_vang() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Beri");
                            return;
                        }
                        p.update_vang(-vang_req);
                    }
                    p.item.add_item_bag47(4, id, value);
                    Message m22 = new Message(-64);
                    m22.writer().writeUTF("Mua " + value);
                    p.addmsg(m22);
                    m22.cleanup();
                    p.item.update_Inventory(-1, false);
                    p.update_money();
                }
            }
        } else if (cat == -1 && TypeShop == 6) {
            if (ItemSell.check_item_sell_material(id)) {
                if ((p.item.able_bag() < 1 && p.item.total_item_bag_by_id(7, id) == 0)
                        || ((p.item.total_item_bag_by_id(7, id)
                                + value) > DataTemplate.MAX_ITEM_IN_BAG)) {
                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                    return;
                }
                ItemTemplate7 it_template = ItemTemplate7.get_it_by_id(id);
                if (it_template != null) {
                    int vang_req = it_template.priceruby * value;
                    if (vang_req > 0) {
                        if (p.get_ngoc() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Ruby");
                            return;
                        }
                        p.update_ngoc(-vang_req);
                    } else {
                        vang_req = it_template.price * value;
                        if (vang_req <= 0) {
                            return;
                        }
                        if (p.get_vang() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Beri");
                            return;
                        }
                        p.update_vang(-vang_req);
                    }
                    p.update_money();
                    p.item.add_item_bag47(7, id, value);
                    Message m22 = new Message(-64);
                    m22.writer().writeUTF("Mua " + value);
                    p.addmsg(m22);
                    m22.cleanup();
                    p.item.update_Inventory(-1, false);
                }
            }
        } else if (cat == -1 && TypeShop == 103) {
            ItemHair ith = ItemHair.get_item(id, 103);
            if (ith != null) {
                if (p.check_itfashionP(ith.ID, 103) != null) {
                    Service.send_box_ThongBao_OK(p, "Đã mua rồi!");
                    return;
                }
                if (p.get_vang() < ith.beri) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + ith.beri + " Beri");
                    return;
                }
                if (p.get_ngoc() < ith.ruby) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + ith.ruby + " Ruby");
                    return;
                }
                p.update_vang(-ith.beri);
                p.update_ngoc(-ith.ruby);
                p.update_money();
                ItemFashionP temp_new = new ItemFashionP();
                temp_new.category = 103;
                temp_new.id = ith.ID;
                temp_new.icon = ith.idIcon;
                p.itfashionP.add(temp_new);
                p.update_itfashionP(temp_new, 103);
                for (int i = 0; i < p.map.players.size(); i++) {
                    Player p0 = p.map.players.get(i);
                    Service.charWearing(p, p0, false);
                }
                ItemFashionP.show_table(p, 103);
                Service.send_box_ThongBao_OK(p, "Mua thành công " + ith.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Mua thất bại, hãy thử lại!");
            }
        } else if (cat == -1 && TypeShop == 112) {
            ItemHair ith = ItemHair.get_item(id, 108);
            if (ith != null) {
                if (p.check_itfashionP(ith.ID, 108) != null) {
                    Service.send_box_ThongBao_OK(p, "Đã mua rồi!");
                    return;
                }
                if (p.get_ngoc() < 500) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 500 Ruby!");
                    return;
                }
                p.update_ngoc(-500);
                p.update_money();
                ItemFashionP temp_new = new ItemFashionP();
                temp_new.category = 108;
                temp_new.id = ith.ID;
                temp_new.icon = ith.idIcon;
                p.itfashionP.add(temp_new);
                p.update_itfashionP(temp_new, 108);
                for (int i = 0; i < p.map.players.size(); i++) {
                    Player p0 = p.map.players.get(i);
                    Service.charWearing(p, p0, false);
                }
                ItemFashionP.show_table(p, 108);
                Service.send_box_ThongBao_OK(p, "Mua thành công " + ith.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Mua thất bại, hãy thử lại!");
            }
        } else if (cat == -1 && TypeShop == 105) {
            ItemFashion itf = ItemFashion.get_item(id);
            if (itf != null) {
                if (itf.price == -1) {
                    Service.send_box_ThongBao_OK(p,
                            "Thời trang " + itf.name + " hiện tại chưa được bán");
                    return;
                }
                if (p.get_ngoc() < itf.price) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + String.format("%,d", itf.price) + " Ruby");
                    return;
                }
                if (p.check_fashion(itf.ID) != null) {
                    Service.send_box_ThongBao_OK(p, "Đã mua rồi!");
                    return;
                }
                p.update_ngoc(-itf.price);
                p.update_money();
                ItemFashionP2 temp2 = new ItemFashionP2();
                temp2.id = itf.ID;
                temp2.expirationTime = -1;
                temp2.op = ItemFashionP2.randomOptionsForFashion(id);
                p.fashion.add(temp2);
                p.update_fashionP2(temp2);
                for (int i = 0; i < p.map.players.size(); i++) {
                    Player p0 = p.map.players.get(i);
                    Service.charWearing(p, p0, false);
                }
                Service.UpdateInfoMaincharInfo(p);
                ItemFashionP.show_table(p, 105);
                Service.send_box_ThongBao_OK(p, "Mua thành công " + itf.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Mua thất bại, hãy thử lại!");
            }
        } else if (cat == -1 && TypeShop == 111) {
            if ((p.item.able_bag() < 1 && p.item.total_item_bag_by_id(4, id) == 0)
                    || ((p.item.total_item_bag_by_id(4, id)
                            + value) > DataTemplate.MAX_ITEM_IN_BAG)) {
                Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                return;
            }
            for (int i = 0; i < Rebuild_Item.ID_SELL.length; i++) {
                if (Rebuild_Item.ID_SELL[i] == id) {
                    ItemTemplate4 it_temp = ItemTemplate4.get_it_by_id(id);
                    if (it_temp != null) {
                        int vang_req = it_temp.ruby * value;
                        if (vang_req > 0) {
                            if (p.get_ngoc() < vang_req) {
                                Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Ruby");
                                return;
                            }
                            p.update_ngoc(-vang_req);
                        } else {
                            vang_req = it_temp.beri * value;
                            if (vang_req <= 0) {
                                return;
                            }
                            if (p.get_vang() < vang_req) {
                                Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Beri");
                                return;
                            }
                            p.update_vang(-vang_req);
                        }
                        ItemBag47 it = new ItemBag47();
                        switch (id) {
                            case 272: {
                                if (p.item.total_item_bag_by_id(4,
                                        46) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 46;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            case 273: {
                                if (p.item.total_item_bag_by_id(4,
                                        52) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 52;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            case 274: {
                                if (p.item.total_item_bag_by_id(4,
                                        58) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 58;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            case 275: {
                                if (p.item.total_item_bag_by_id(4,
                                        64) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 64;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            case 276: {
                                if (p.item.total_item_bag_by_id(4,
                                        70) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 70;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            case 277: {
                                if (p.item.total_item_bag_by_id(4,
                                        76) >= DataTemplate.MAX_ITEM_IN_BAG) {
                                    p.update_ngoc(vang_req);
                                    Service.send_box_ThongBao_OK(p, "Hành trang đầy!");
                                    return;
                                }
                                it.id = 76;
                                it.category = 4;
                                it.quant = (short) (100 * value);
                                break;
                            }
                            default: {
                                it.id = id;
                                it.category = 4;
                                it.quant = value;
                                break;
                            }
                        }
                        //
                        if (p.item.add_item_bag47(it.category, it.id, it.quant)) {
                            Message m22 = new Message(-64);
                            m22.writer().writeUTF("Mua " + value);
                            p.addmsg(m22);
                            m22.cleanup();
                        } else {
                            Service.send_box_ThongBao_OK(p, "Không thể mua với số lượng này");
                            p.update_ngoc(vang_req);
                        }
                        p.item.update_Inventory(-1, false);
                        p.update_money();
                    } else {
                        Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, hãy báo cho admin!");
                    }
                    break;
                }
            }
        } else if (cat == -1 && TypeShop == 102) {
            ItemBoat itb = ItemBoat.get_item(id);
            if (itb != null) {
                if (p.check_itboat(itb.id) != null) {
                    Service.send_box_ThongBao_OK(p, "Đã mua rồi!");
                    return;
                }
                if (p.get_ngoc() < 5) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5 ruby!");
                    return;
                }
                p.update_ngoc(-5);
                p.update_money();
                ItemBoatP temp_new = new ItemBoatP();
                temp_new.id = itb.id;
                temp_new.is_use = true;
                p.itemboat.add(temp_new);
                p.update_new_part_boat(temp_new);
                ItemBoat.update_part_boat_when_shopping(p);
                ItemFashionP.show_table(p, 102);
                Service.send_box_ThongBao_OK(p, "Mua thành công " + itb.name);
            } else {
                Service.send_box_ThongBao_OK(p, "Mua thất bại, hãy thử lại!");
            }
        } else if (p.clan != null && TypeShop == 98 && value == 1 && cat == -1 && id >= 0
                && id < 10) {
            p.clan.icon = id;
            Clan.send_info(p, false);
            for (int i = 0; i < p.map.players.size(); i++) {
                if (!p.map.players.get(i).equals(p)) {
                    Clan.send_me_to_other(p, p.map.players.get(i), false);
                }
            }
            Message m = new Message(-52);
            m.writer().writeByte(21);
            m.writer().writeUTF("Đăng ký băng hải tặc " + p.clan.name + " thành công");
            p.addmsg(m);
            m.cleanup();
        } else if (p.clan != null && TypeShop == 97 && value == 1 && cat == -1 && id >= 0) {
            if (id < 10) {
                p.clan.icon = id;
                for (int i2 = 0; i2 < p.clan.members.size(); i2++) {
                    Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i2).name);
                    if (p0 != null) {
                        Clan.send_info(p0, false);
                        for (int i = 0; i < p0.map.players.size(); i++) {
                            if (!p0.map.players.get(i).equals(p0)) {
                                Clan.send_me_to_other(p0, p.map.players.get(i), false);
                            }
                        }
                    }
                }
                Message m = new Message(-52);
                m.writer().writeByte(21);
                m.writer().writeUTF("Đổi icon băng thành công");
                p.addmsg(m);
                m.cleanup();
            } else {
                int ngoc_quant = Clan.get_ngoc_icon(id);
                if (p.clan.get_ngoc() < ngoc_quant) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(ngoc_quant) + " Ruby Băng");
                } else {

                    int requiredXu = 0;
                    if (ngoc_quant >= 30000) {
                        requiredXu = 3; // Cần 3 xu nếu ngoc_quant >= 30000
                    } else if (ngoc_quant >= 10000) {
                        requiredXu = 1; // Cần 1 xu nếu ngoc_quant >= 10000 và < 30000
                    }
                    if (requiredXu > 0) {
                        if (p.item.total_item_bag_by_id(4, 580) < requiredXu) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + requiredXu + " Xu hành trình");
                            return;
                        } else {
                            p.item.remove_item47(4, 580, requiredXu);
                            p.item.update_Inventory(-1, false);
                        }
                    }
                    p.clan.update_ruby(-ngoc_quant);
                    p.clan.icon = id;
                    Clan.mark_icon_as_sold(id);
                    for (int i2 = 0; i2 < p.clan.members.size(); i2++) {
                        Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i2).name);
                        if (p0 != null) {
                            Clan.send_info(p0, false);
                            for (int i = 0; i < p0.map.players.size(); i++) {
                                if (!p0.map.players.get(i).equals(p0)) {
                                    Clan.send_me_to_other(p0, p0.map.players.get(i), false);
                                }
                            }
                        }
                    }
                    Message m = new Message(-52);
                    m.writer().writeByte(21);
                    m.writer().writeUTF("Đổi icon băng thành công");
                    p.addmsg(m);
                    m.cleanup();
                }
            }
        } else if (p.clan != null && p.clan.members.get(0).name.equals(p.name) && TypeShop == 110
                && value <= 20 && value > 0 && cat == -1) {
            check = false;
            for (int i = 0; i < ItemTemplate8.ENTRYS.size(); i++) {
                if (ItemTemplate8.ENTRYS.get(i).id == id) {
                    if (ItemTemplate8.ENTRYS.get(i).ruby > 0) {
                        int vang_req = ItemTemplate8.ENTRYS.get(i).ruby * value;
                        if (p.clan.get_ngoc() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Ruby băng");
                            return;
                        }
                        p.clan.update_ruby(-vang_req);
                        ItemBag47 it_add = null;
                        for (int j = 0; j < p.clan.list_it.size(); j++) {
                            if (p.clan.list_it.get(j).id == id) {
                                it_add = p.clan.list_it.get(j);
                                break;
                            }
                        }
                        if (it_add == null) {
                            it_add = new ItemBag47();
                            it_add.category = 4;
                            it_add.id = id;
                            it_add.quant = 0;
                            p.clan.list_it.add(it_add);
                        }
                        it_add.quant += value;
                        check = true;
                    } else if (ItemTemplate8.ENTRYS.get(i).beri > 0) {
                        int vang_req = ItemTemplate8.ENTRYS.get(i).beri * value;
                        if (p.clan.get_vang() < vang_req) {
                            Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(vang_req) + " Beri băng");
                            return;
                        }
                        p.clan.update_beri(-vang_req);
                        ItemBag47 it_add = null;
                        for (int j = 0; j < p.clan.list_it.size(); j++) {
                            if (p.clan.list_it.get(j).id == id) {
                                it_add = p.clan.list_it.get(j);
                                break;
                            }
                        }
                        if (it_add == null) {
                            it_add = new ItemBag47();
                            it_add.category = 4;
                            it_add.id = id;
                            it_add.quant = 0;
                            p.clan.list_it.add(it_add);
                        }
                        it_add.quant += value;
                        check = true;
                    } else {
                        Service.send_box_ThongBao_OK(p, "Vật phẩm chưa bán");
                    }
                    break;
                }
            }
            if (check) {
                for (int i = 0; i < p.clan.members.size(); i++) {
                    Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i).name);
                    if (p0 != null) {
                        Clan.send_money(p0, false);
                        p0.clan.send_inventory(p0, false);
                    }
                }
                Message m22 = new Message(-64);
                m22.writer().writeUTF("Mua " + value);
                p.addmsg(m22);
                m22.cleanup();
            }
        } else if (TypeShop == 118 && value == 1) {
            synchronized (p) { // Chống spam mua
                ShopTichLuy temp_shop = ShopTichLuy.get_temp_id(cat, id);
                if (temp_shop != null) {
                    int coin = temp_shop.point;
                    if (p.get_coin() < coin) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(coin) + " Coin");
                        return;
                    }
                    boolean success = false;
                    String itemName = "";
                    if (cat == 4) {
                        ItemTemplate4 template4 = ItemTemplate4.get_it_by_id(id);
                        if (template4 != null) {
                            itemName = template4.name;
                            p.update_coin(-coin);
                            if (id == 221) {
                                p.item.add_item_bag47(4, id, 10);
                            } else {
                                p.item.add_item_bag47(4, id, 1);
                            }
                            success = true;
                        }
                    } else if (cat == 7) {
                        ItemTemplate7 template7 = ItemTemplate7.get_it_by_id(id);
                        if (template7 != null) {
                            itemName = template7.name;
                            p.update_coin(-coin);
                            p.item.add_item_bag47(7, id, 1);
                            success = true;
                        }
                    }
                    if (success) {
                        p.item.update_Inventory(-1, false);
                        p.update_money();
                        Service.send_box_ThongBao_OK(p, "Mua thành công " + itemName);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, vui lòng thử lại.");
                    }
                }
            }
        } else if (TypeShop == 121 && value == 1) { // Mua Danh Hiệu (Extol)
            synchronized (p) {
                ShopExtol temp_shop = ShopExtol.get_temp_id(id); // 'id' là ID bản ghi trong shop_extol
                if (temp_shop != null) {
                    Service.send_box_ThongBao_OK(p, "Mua danh hiệu thành công!");
                }
            }
        } else if (TypeShop == 119 && value == 1 && cat == -1 && id >= 0
                && id < p.item.save_item_wear.size()) {
            Item_wear it_select = p.item.save_item_wear.get(id);
            if (it_select != null) {
                if (p.get_ngoc() < 5) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5 ruby");
                    return;
                }
                if (p.item.able_bag() > 0) {
                    if (p.item.add_item_bag3(it_select)) {
                        p.item.save_item_wear.remove(id);
                        p.update_ngoc(-5);
                        p.update_money();
                        p.item.update_Inventory(-1, false);
                        Service.Send_UI_Shop(p, 119);
                        Service.send_box_ThongBao_OK(p,
                                "Lấy " + it_select.template.name + " về thành công, phí 5 ruby");
                    } else {
                        p.item.remove_item_wear(it_select);
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                }
            }
        } else if (TypeShop == 116 && id >= 647 && id <= 682 && value == 1 && cat == 4) { // da than thoai
            ItemTemplate4 temp4 = null;
            ItemTemplate4 temp4_2 = ItemTemplate4.get_it_by_id(id);
            if (id >= 677 && id <= 682) {
                temp4 = ItemTemplate4.get_it_by_id(id - 309);
            } else {
                temp4 = ItemTemplate4.get_it_by_id(id - 406);
            }
            if (temp4 != null && temp4_2 != null) {
                if (p.item.total_item_bag_by_id(4, temp4.id) < 3) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 3 " + temp4.name);
                    return;
                }
                int beri_can = 1_000_000;
                int ruby_can = 40;
                if (p.get_vang() < beri_can) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + String.format("%,d", beri_can) + " Beri");
                    return;
                }
                if (p.get_ngoc() < ruby_can) {
                    Service.send_box_ThongBao_OK(p, "Không đủ " + Util.number_format(ruby_can) + " Ruby");
                    return;
                }
                int soLanNangCap = 0;
                long tongBeriTieuTon = 0;
                int tongRubyTieuTon = 0;
                boolean thanhCong = false;
                while (p.item.total_item_bag_by_id(4, temp4.id) >= 3 && p.get_vang() >= beri_can
                        && p.get_ngoc() >= ruby_can) {
                    if (soLanNangCap >= p.solannang) {
                        break;
                    }
                    soLanNangCap++;
                    tongBeriTieuTon += beri_can;
                    tongRubyTieuTon += ruby_can;
                    p.update_ngoc(-ruby_can);
                    p.update_vang(-beri_can);
                    p.update_money();
                    boolean suc = Util.random(800) < 1;
                    if (suc) {
                        p.item.remove_item47(4, temp4.id, 3);
                        p.item.add_item_bag47(4, id, 1);
                        thanhCong = true;
                        break;
                    }
                    p.item.update_Inventory(-1, false);
                }
                StringBuilder resultMessage = new StringBuilder();
                resultMessage.append("== Auto Đá Thần Thoại ==\n");
                if (thanhCong) {
                    resultMessage.append("- Thành công -\n");
                    resultMessage.append("Nhận : 1 " + temp4_2.name + "\n");
                } else {
                    resultMessage.append("- Thất bại -\n");
                }
                resultMessage.append("--------------------\n");
                resultMessage.append("- Số lần nâng : " + soLanNangCap + "\n");
                resultMessage.append("- Tổng Beri : " + String.format("%,d", tongBeriTieuTon) + "\n");
                resultMessage.append("- Tổng Ruby : " + String.format("%,d", tongRubyTieuTon) + "\n");
                Service.send_box_ThongBao_OK(p, resultMessage.toString());
            }
        }

    }

    public static void send_box_ThongBao_OK(Player p, String notice) throws IOException {
        if (p == null || p.conn == null)
            return;
        Message m = new Message(-11);
        m.writer().writeShort(0);
        m.writer().writeByte(0);
        m.writer().writeUTF("Thông Báo");
        m.writer().writeUTF(notice);
        m.writer().writeByte(0);
        p.addmsg(m);
        m.cleanup();
    }

    public static void ChestWanted(Player p, boolean save_cache) throws IOException {
        Message m = new Message(-86);
        m.writer().writeByte(1);
        m.writer().writeByte(0);
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void use_potion(Player p, int type, long par) throws IOException {
        switch (type) {
            case 0: {
                p.update_hp(par);
                break;
            }
            case 1: {
                p.update_mp(par);
                break;
            }
        }
    }

    public static void send_revive_player(Player p) throws IOException {
        Message m = new Message(6);
        m.writer().writeShort(p.index_map);
        m.writer().writeByte(0); // Player category
        m.writer().writeInt(f.setInteger(p.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(p.body.get_mp_max(true)));
        p.map.send_msg_all_p(m, p, true);
        m.cleanup();
    }

    public static void send_view_other_player(Player p0, Player p) throws IOException {
        Message m = new Message(-42);
        m.writer().writeUTF(p0.getNameShow());
        m.writer().writeInt(f.setInteger(p0.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(p0.body.get_mp_max(true)));
        m.writer().writeInt(f.setInteger(p0.hp));
        m.writer().writeInt(f.setInteger(p0.mp));
        m.writer().writeShort(p0.level);
        m.writer().writeShort(p0.get_level_percent());
        m.writer().writeShort(p0.get_head());
        m.writer().writeShort(p0.get_hair());
        if (p0.clan != null) {
            m.writer().writeShort(p0.clan.id);
            m.writer().writeShort(p0.clan.icon);
            m.writer().writeUTF(p0.clan.name);
        } else {
            m.writer().writeShort(-1);
        }
        m.writer().writeByte(8);
        short[] fashion = p0.get_fashion();
        for (int i = 0; i < 8; i++) {
            Item_wear it_w = p0.item.it_body[i];
            if (it_w != null) {
                m.writer().writeByte(1);
                Item.readUpdateItem(m.writer(), it_w, p0);
                if ((i == 1 && !p0.is_show_hat) || (i == 0 && !p0.is_show_weapon)) {
                    m.writer().writeShort(-1);
                } else if (fashion != null && fashion[i] != -1) {
                    m.writer().writeShort(fashion[i]);
                } else {
                    m.writer().writeShort(it_w.template.part);
                }
            } else {
                m.writer().writeByte(0);
            }
        }
        m.writer().writeByte(0);
        m.writer().writeShort(-1);
        m.writer().writeByte(p0.get_index_full_set());
        p.addmsg(m);
        m.cleanup();
    }

    public static void send_view_second_body(Player p0, Player p) throws IOException {
        Player target = (p0 != null) ? p0 : p;
        Message m = new Message(-42);
        m.writer().writeUTF(target.getNameShow());
        m.writer().writeInt(f.setInteger(target.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(target.body.get_mp_max(true)));
        m.writer().writeInt(f.setInteger(target.hp));
        m.writer().writeInt(f.setInteger(target.mp));
        m.writer().writeShort(target.level);
        m.writer().writeShort(target.get_level_percent());
        m.writer().writeShort(target.get_head());
        m.writer().writeShort(target.get_hair());
        if (target.clan != null) {
            m.writer().writeShort(target.clan.id);
            m.writer().writeShort(target.clan.icon);
            m.writer().writeUTF(target.clan.name);
        } else {
            m.writer().writeShort(-1);
        }
        m.writer().writeByte(8);
        short[] fashion = target.get_fashion();
        for (int i = 0; i < 8; i++) {
            Item_wear it_w = target.item.second_body[i];
            Item_wear main_item = target.item.it_body[i];
            if (it_w != null) {
                m.writer().writeByte(1);
                Item.readUpdateItem(m.writer(), it_w, target);
                if ((i == 1 && !target.is_show_hat) || (i == 0 && !target.is_show_weapon)) {
                    m.writer().writeShort(-1);
                } else if (fashion != null && fashion[i] != -1) {
                    m.writer().writeShort(fashion[i]);
                } else if (main_item != null) {
                    m.writer().writeShort(main_item.template.part);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeByte(0);
            }
        }
        m.writer().writeByte(0); // 0 bat -1 tat
        m.writer().writeShort(-1); // id part vu khi
        m.writer().writeByte(p0.get_index_full_set());
        p.addmsg(m);
        m.cleanup();
    }

    public static void send_view_third_body(Player p0, Player p) throws IOException {
        Player target = (p0 != null) ? p0 : p;
        Message m = new Message(-42);
        m.writer().writeUTF(target.getNameShow());
        m.writer().writeInt(f.setInteger(target.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(target.body.get_mp_max(true)));
        m.writer().writeInt(f.setInteger(target.hp));
        m.writer().writeInt(f.setInteger(target.mp));
        m.writer().writeShort(target.level);
        m.writer().writeShort(target.get_level_percent());
        m.writer().writeShort(target.get_head());
        m.writer().writeShort(target.get_hair());
        if (target.clan != null) {
            m.writer().writeShort(target.clan.id);
            m.writer().writeShort(target.clan.icon);
            m.writer().writeUTF(target.clan.name);
        } else {
            m.writer().writeShort(-1);
        }
        m.writer().writeByte(8);
        short[] fashion = target.get_fashion();
        for (int i = 0; i < 8; i++) {
            Item_wear it_w = target.item.third_body[i];
            Item_wear main_item = target.item.it_body[i];
            if (it_w != null) {
                m.writer().writeByte(1);
                Item.readUpdateItem(m.writer(), it_w, target);
                if ((i == 1 && !target.is_show_hat) || (i == 0 && !target.is_show_weapon)) {
                    m.writer().writeShort(-1);
                } else if (fashion != null && fashion[i] != -1) {
                    m.writer().writeShort(fashion[i]);
                } else if (main_item != null) {
                    m.writer().writeShort(main_item.template.part);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeByte(0);
            }
        }
        m.writer().writeByte(0); // 0 bat -1 tat
        m.writer().writeShort(-1); // id part vu khi
        m.writer().writeByte(p0.get_index_full_set());
        p.addmsg(m);
        m.cleanup();
    }

    public static void sell_item(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        short num = m2.reader().readShort();
        if (num <= 0 || num > DataTemplate.MAX_ITEM_IN_BAG) {
            return;
        }
        switch (type) {
            case 1: // drop item in bag
            case 0: { // sell item in bag
                switch (cat) {
                    case 3: {
                        if (p.item.bag3[id] != null) {
                            int vang_recive = 0;
                            if (type == 0) {
                                vang_recive = 30 + (2 * p.item.bag3[id].template.color
                                        + (p.item.bag3[id].template.level / 10) + 1)
                                        * DataTemplate.TabInventory_ItemSell[0];
                                if (vang_recive > DataTemplate.TabInventory_ItemSell[1]) {
                                    vang_recive = DataTemplate.TabInventory_ItemSell[1];
                                }
                                p.update_vang(vang_recive * num);
                                p.update_money();
                            }
                            //
                            p.item.add_item_save(p.item.bag3[id]);
                            p.item.bag3[id] = null;
                            p.item.update_Inventory(-1, false);
                        }
                        break;
                    }
                    case 4:
                    case 7: {
                        if (id == 1007) {
                            Service.send_box_ThongBao_OK(p,
                                    "Không thể vứt hoặc bán vật phẩm này");
                            return;
                        }
                        p.item.remove_item47(cat, id, num);
                        p.item.update_Inventory(-1, false);
                        int vang_receiv = 0;
                        if (type == 0) {
                            vang_receiv = DataTemplate.TabInventory_ItemSell[2] * num;
                            p.update_vang(vang_receiv);
                            p.update_money();
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    public static void request_item4_info(Player p, Message m2) throws IOException {
        if (p == null || p.conn == null)
            return;
        short id = m2.reader().readShort();
        ItemTemplate4_Info temp = ItemTemplate4_Info.get_by_id(id);
        if (temp != null) {
            Message m = new Message(-105);
            m.writer().writeShort(id);
            m.writer().writeUTF(temp.info != null ? temp.info : "");
            p.addmsg(m);
            m.cleanup();
        } else {
            core.GameLogger.info("infopotion fail id=" + id + " | user=" + p.conn.user + " | ip=" + p.conn.ipAddress);
            // Gửi thông báo mặc định thay vì kick người chơi
            Message m = new Message(-105);
            m.writer().writeShort(id);
            m.writer().writeUTF("Vui lòng xóa dữ liệu game và tải lại...");
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void input_text(Player p, int id, String s, String[] s2) throws IOException {
        Message m = new Message(-81);
        m.writer().writeShort(id);
        m.writer().writeUTF(s);
        m.writer().writeByte(s2.length);
        for (int i = 0; i < s2.length; i++) {
            m.writer().writeUTF(s2[i]);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void send_box_yesno(Player p, int type, String title, String text,
            String[] name_cmd, byte[] icon) throws IOException {
        Message m = new Message(-11);
        m.writer().writeShort(type);
        m.writer().writeByte(2);
        m.writer().writeUTF(title);
        m.writer().writeUTF(text);
        m.writer().writeByte(name_cmd.length);
        for (int i = 0; i < name_cmd.length; i++) {
            m.writer().writeUTF(name_cmd[i]);
            m.writer().writeByte(i);
            m.writer().writeByte(icon[i]);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void open_box_item3_orange(Player p, List<Item_wear> list, int id_chest,
            String s1, String s2) throws IOException {
        Message m = new Message(-34);
        m.writer().writeByte(21);
        m.writer().writeShort(id_chest);
        m.writer().writeUTF(s1);
        m.writer().writeUTF(s2);
        m.writer().writeByte(list.size());
        for (int i = 0; i < list.size(); i++) {
            Item_wear temp = list.get(i);
            m.writer().writeByte(3);
            m.writer().writeUTF(temp.template.name);
            m.writer().writeShort(temp.template.icon);
            m.writer().writeInt(1);
            m.writer().writeByte(3);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void open_box_item3_vua(Player p, List<Item_wear> list, int id_chest,
            String s1, String s2) throws IOException {
        Message m = new Message(-34);
        m.writer().writeByte(21);
        m.writer().writeShort(id_chest);
        m.writer().writeUTF(s1);
        m.writer().writeUTF(s2);
        m.writer().writeByte(list.size());
        for (int i = 0; i < list.size(); i++) {
            Item_wear temp = list.get(i);
            m.writer().writeByte(3);
            m.writer().writeUTF(temp.template.name);
            m.writer().writeShort(temp.template.icon);
            m.writer().writeInt(1);
            m.writer().writeByte(4);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void CountDown_Ticket(Player p) throws IOException {
        Message m = new Message(-61);
        long cd = (p.cd_ticket_next - System.currentTimeMillis()) / 1000;
        m.writer().writeByte(3);
        EffTemplate eff = p.get_eff(2);
        if (eff != null) {
            cd = (eff.time - System.currentTimeMillis()) / 1000;
            if (cd < 0) {
                cd = 0;
            }
        } else {
            cd = 0;
        }
        m.writer().writeInt((int) cd);
        p.addmsg(m);
        m.cleanup();
    }

    public static void NewDialog_eat_taq(Player p, String[] name_, int[] icon_, int id)
            throws IOException {
        Message m = new Message(40);
        m.writer().writeByte(1);
        m.writer().writeUTF("");
        m.writer().writeByte(name_.length + 1);
        ItemTemplate4 it_temp = ItemTemplate4.get_it_by_id(id);
        m.writer().writeByte(4);
        m.writer().writeUTF(it_temp.name);
        m.writer().writeShort(it_temp.icon);
        for (int i = 0; i < name_.length; i++) {
            m.writer().writeByte(104);
            m.writer().writeUTF(name_[i]);
            m.writer().writeShort(icon_[i]);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void Help_From_Server(Player p, int num, String text) throws IOException {
        Message m = new Message(-54);
        m.writer().writeShort(num);
        m.writer().writeUTF(text);
        p.addmsg(m);
        m.cleanup();
    }

    public static void Wanted(Player p, boolean save_cache) throws IOException {
        Message m = new Message(-85);
        m.writer().writeByte(4);
        m.writer().writeInt((int) p.get_wanted_point());
        if (save_cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void start_combo(Player p, int type) throws IOException {
        Message m = new Message(29);
        int size0 = p.list_can_combo.size();
        if (type == 1 && size0 > 0) {
            p.time_combo = System.currentTimeMillis() + 30_000L;
            int size = Util.random(4, 10);
            p.is_combo = new byte[size];
            m.writer().writeByte(p.is_combo.length);
            for (int i = 0; i < p.is_combo.length; i++) {
                Skill_info get_skill = p.list_can_combo.get(Util.random(size0));
                p.is_combo[i] = (byte) get_skill.temp.ID;
                m.writer().writeShort(get_skill.temp.idIcon + 94);
            }
        } else {
            m.writer().writeByte(0);
        }
        p.addmsg(m);
        m.cleanup();
    }

    public static void distributeClanGiftOnce(Clan clan, List<Player> players, List<GiftBox> gifts, String title,
            String notice) {
        try {
            for (GiftBox gb : gifts) {
                switch (gb.id) {
                    case -10:
                        clan.update_xp(gb.num);
                        break;
                    case -11:
                        clan.update_beri(gb.num);
                        break;
                    case -12:
                        clan.update_ruby(gb.num);
                        break;
                }
            }
            for (Player p : players) {
                Clan.set_data(p, false);
                Clan.send_money(p, false);
                Message m = new Message(-34);
                m.writer().writeByte(0);
                m.writer().writeUTF(title);
                m.writer().writeUTF(notice);
                m.writer().writeByte(gifts.size());
                for (GiftBox gb : gifts) {
                    m.writer().writeByte(gb.type);
                    m.writer().writeUTF(gb.name != null ? gb.name : "");
                    m.writer().writeShort(gb.icon);
                    m.writer().writeInt(f.setInteger(gb.num));
                    m.writer().writeByte(gb.color);
                }
                p.addmsg(m);
                m.cleanup();
            }
        } catch (Exception e) {
            GameLogger.error("Service.distributeClanGiftOnce error", e);
        }
    }

    public static void send_gift(Player p, int type, String title, String notice,
            List<GiftBox> gift, boolean show_table) throws IOException {
        Message m = new Message(-34);
        m.writer().writeByte(type);
        m.writer().writeUTF(title != null ? title : "");
        m.writer().writeUTF(notice != null ? notice : "");
        m.writer().writeByte(gift.size());
        for (int i = 0; i < gift.size(); i++) {
            GiftBox temp = gift.get(i);
            m.writer().writeByte(temp.type);
            m.writer().writeUTF(temp.name != null ? temp.name : "");
            m.writer().writeShort(temp.icon);
            long num_item = temp.num;
            m.writer().writeInt(f.setInteger(num_item));
            m.writer().writeByte(temp.color);
            //
            switch (temp.type) {
                case 99: { // xp
                    long exp_receiv = num_item;
                    int buff_percent = 100;
                    if (p.clan != null && p.clan.check_buff(0)) {
                        buff_percent += 50;
                    }
                    if (p.clan != null && p.clan.check_buff(1)) {
                        buff_percent += 50;
                    }
                    if (p.get_eff(2) != null) {
                        buff_percent += 100;
                    }
                    if (p.get_eff(17) != null) {
                        buff_percent += 100;
                    }
                    exp_receiv = (exp_receiv * buff_percent) / 100;
                    p.update_exp(exp_receiv, false);
                    break;
                }
                case 3: {
                    ItemTemplate3 template3 = ItemTemplate3.get_it_by_id(temp.id);
                    if (template3 != null) {
                        Item_wear it_add = new Item_wear();
                        it_add.setup_template_by_id(template3);
                        if (it_add.template != null) {
                            int r = Util.random(100);
                            int numLoKham = (r < 50) ? 0 : (r < 80 ? 1 : 2);
                            it_add.numLoKham = (byte) numLoKham;
                            p.item.add_item_bag3(it_add);
                            if (it_add.template.id > 12000 || it_add.template.tb2 == 1) {
                                int[] POSSIBLE_OPTION_IDS = { 1, 2, 10, 11, 12, 13, 14, 17,
                                        49, 50, 51, 52, 53, 56, 63 };
                                int numDongs;
                                int multiplier = 1;
                                if (it_add.template.id == 11082 || it_add.template.id == 11038
                                        || it_add.template.id == 11039 || it_add.template.id == 11040) {
                                    numDongs = Util.random(4, 6); // 4 - 5 dòng
                                    multiplier = 2;
                                } else {
                                    numDongs = Util.random(3, 5); // 3 - 4 dòng
                                }
                                int colorBonus = (it_add.template.id > 12000) ? it_add.template.color * 20 : 0;
                                for (int j = 0; j < numDongs; j++) {
                                    int optionId = POSSIBLE_OPTION_IDS[Util.random(POSSIBLE_OPTION_IDS.length)];
                                    int param = Util.random(50, 151) * multiplier + colorBonus;
                                    it_add.option_item.add(new Option(optionId, param));
                                }
                            } else if (it_add.template.tb2 == 0) {
                                int color = it_add.template.color;
                                int[] randomIds = { 5, 6, 7, 8, 9 };
                                int minVal = 10 + color * 10;
                                int maxVal = minVal + 11;
                                for (int j = 0; j < 3; j++) {
                                    int optId = randomIds[Util.random(randomIds.length)];
                                    int optVal = Util.random(minVal, maxVal);
                                    it_add.option_item.add(new Option(optId, optVal));
                                }
                            }
                        }
                    }
                    break;
                }

                case 4: {
                    ItemTemplate4 template4 = ItemTemplate4.get_it_by_id(temp.id);
                    if (template4 != null) {
                        if (template4.id == 0) { // beri
                            p.update_vang(num_item);
                            p.update_money();
                        } else if (template4.id == 1) { // ruby
                            p.update_ngoc(num_item);
                            p.update_money();
                        } else if (template4.id == 2) { // extol
                            p.update_vnd(num_item);
                            p.update_money();
                        } else if (template4.id == 6) { // bread
                            p.update_ticket((int) num_item);
                            p.update_money();
                        } else {
                            p.item.add_item_bag47(4, template4.id, (int) num_item);
                        }
                    } else if (temp.id == -10) { // xp clan
                        if (p.clan != null) {
                            p.clan.update_xp(num_item);
                            for (int i1 = 0; i1 < p.clan.members.size(); i1++) {
                                Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i1).name);
                                if (p0 != null) {
                                    Clan.set_data(p0, false);
                                }
                            }
                        }
                    } else if (temp.id == -11) { // beri clan
                        if (p.clan != null) {
                            p.clan.update_beri(num_item);
                            for (int i1 = 0; i1 < p.clan.members.size(); i1++) {
                                Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i1).name);
                                if (p0 != null) {
                                    Clan.send_money(p0, false);
                                }
                            }
                        }
                    } else if (temp.id == -12) { // ruby clan
                        if (p.clan != null) {
                            p.clan.update_ruby(num_item);
                            for (int i1 = 0; i1 < p.clan.members.size(); i1++) {
                                Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i1).name);
                                if (p0 != null) {
                                    Clan.send_money(p0, false);
                                }
                            }
                        }
                    }
                    break;
                }
                case 7: {
                    ItemTemplate7 template7 = ItemTemplate7.get_it_by_id(temp.id);
                    if (template7 != null) {
                        p.item.add_item_bag47(7, template7.id, (int) num_item);
                    }
                    break;
                }
                case 105: {
                    ItemFashion itf = ItemFashion.get_item(temp.id);
                    if (itf != null) {
                        // Kiểm tra thời trang người chơi đang sở hữu
                        ItemFashionP2 existingFashion = p.check_fashion(itf.ID);
                        if (existingFashion == null) {
                            // Chưa sở hữu, thêm mới thời trang
                            ItemFashionP2 temp2 = new ItemFashionP2();
                            temp2.id = itf.ID;
                            temp2.expirationTime = -1; // Vĩnh viễn
                            temp2.op = ItemFashionP2.randomOptionsForFashion(itf.ID);
                            p.fashion.add(temp2);
                            p.update_fashionP2(temp2);
                        } else if (existingFashion.expirationTime != -1) {
                            // Đã sở hữu nhưng có hạn sử dụng, cập nhật thành vĩnh viễn
                            existingFashion.expirationTime = -1; // Vĩnh viễn
                            p.update_fashionP2(existingFashion);
                        }

                        // Cập nhật cho tất cả người chơi trong bản đồ
                        for (int i2 = 0; i2 < p.map.players.size(); i2++) {
                            Player p0 = p.map.players.get(i2);
                            Service.charWearing(p, p0, false);
                        }
                        Service.UpdateInfoMaincharInfo(p);
                    }
                    break;
                }
            }
        }
        if (show_table && p.is_show_gift) {
            p.addmsg(m);
        }
        m.cleanup();
        p.item.update_Inventory(-1, false);
    }

    public static void send_eff(Player p, int b, int num) throws IOException {
        // 0 lv up
        // 1 bien hinh
        // 3 thunder to obj
        // 11: open bi ngo
        // 14, 15 green ball
        // 19: exit obj
        // 20 khinh khi cau
        // 21 deny death
        // 22 red thunder
        // 23 firework
        // 24 room heart
        //
        // 100 = 10s
        if (b == 14 || b == 15) {
            num /= 10;
        }
        Message m = new Message(-15);
        m.writer().writeByte(b);
        m.writer().writeShort(p.index_map);
        m.writer().writeByte(0);
        m.writer().writeShort(num);
        p.map.send_msg_all_p(m, p, true);
        m.cleanup();
    }

    public static void send_eff_sword_splash(int id, Player p) throws IOException {
        Player p0 = p.map.get_player_by_id_inmap(id);
        Mob mob = null;
        if (p0 == null) {
            mob = Mob.ENTRYS.get(id);
        }
        if (p0 != null || mob != null) {
            Message m = new Message(-15);
            m.writer().writeByte(5);
            m.writer().writeShort(p.index_map);
            m.writer().writeByte(0);
            m.writer().writeShort(2000);
            //
            m.writer().writeShort(id);
            if (mob != null) {
                m.writer().writeByte(1);
            } else {
                m.writer().writeByte(0);
            }
            //
            p.map.send_msg_all_p(m, p, true);
            m.cleanup();
        }
    }

    public static void send_kich_an(Player p0, Player p, int time_buff, int type, int type_eff,
            int par) throws IOException {
        Message m = new Message(57);
        m.writer().writeByte(type);
        m.writer().writeShort(time_buff);
        m.writer().writeShort(p.index_map);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeInt(time_buff * 10);
        //
        m.writer().writeShort(p0.index_map);
        m.writer().writeByte(0);
        m.writer().writeByte(type_eff);
        m.writer().writeInt(f.setInteger(par));
        //
        p.map.send_msg_all_p(m, p, true);
        m.cleanup();
    }

    public static void DonotAutoReconnect(Session ss) throws IOException {
        Message m = new Message(-88);
        ss.addmsg(m);
        m.cleanup();
    }

    public static void send_time_cool_down(Player p, long t, String title, int type)
            throws IOException {
        if (type == 0 || type == 2 || type == 3) {
            Message m = new Message(-73);
            m.writer().writeByte(type); // time type
            long time_remain = t - System.currentTimeMillis();
            m.writer().writeShort((short) (time_remain / 1000));
            m.writer().writeUTF(title);
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_hp_map(Player p, int hp, int hpMax) throws IOException {
        send_hp_map(p, p, hp, hpMax);
    }

    public static void send_hp_map(Player p, Player target, int hp, int hpMax) throws IOException {
        Message m = new Message(-73);
        m.writer().writeByte(1);
        m.writer().writeInt(hpMax);
        m.writer().writeInt(hp);
        target.addmsg(m);
        m.cleanup();
    }

    public static void showWorldWarPoint(Player p) throws IOException {
        Message m = new Message(-7);
        m.writer().writeByte(22);
        m.writer().writeByte(2);
        p.addmsg(m);
    }
}
