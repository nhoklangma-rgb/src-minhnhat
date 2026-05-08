package core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import map.*;
import map.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import database.SQL;
import io.Message;
import template.*;
import client.*;
import activities.*;

public class Manager {

    private static Manager instance;
    public static String[] NAME_ITEM_SELL_TEMP = new String[] { "Shop Trang Bị Võ Sĩ", "Shop Trang Bị Kiếm Khách",
            "Shop Trang Bị Đầu Bếp", "Shop Trang Bị Hoa Tiêu", "Shop Trang Bị Xạ Thủ" };
    public boolean debug;
    public String mysql_host;
    public String mysql_database;
    public String mysql_user;
    public String mysql_pass;
    public int server_port;
    public int exp;
    public boolean server_admin;
    public boolean server_test;
    public boolean isAutoSpawn = true;
    private int index_mob;
    private static int a = 0;
    public String server_domain;
    public int xnap;
    public boolean enable_websocket;
    public int websocket_port;
    public int http_data_port = 8181;
    private HashMap<String, String> configMap = new HashMap<>();
    public volatile int[] statCaps = new int[200];
    public volatile int[] statRates = new int[200];
    public volatile java.util.List<SoftCapStage>[] statSoftCaps;
    public volatile float pvpRate;

    public static class SoftCapStage {
        public long threshold;
        public double factor;

        public SoftCapStage(long t, double f) {
            this.threshold = t;
            this.factor = f;
        }
    }

    public static final int MAP_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    public static ScheduledExecutorService MAP_POOL = Executors.newScheduledThreadPool(MAP_POOL_SIZE);

    // --- New Game Configurations ---
    public float global_dame_rate;
    public float mob_hp_rate;
    public float global_exp_rate;
    public float global_gold_rate;
    public float global_drop_rate;
    public float mana_cost_rate;
    public float cooldown_rate;
    public int mob_respawn_time;
    public int potential_per_level;
    public int stat_point_max;
    public float spirit_extra_mana;
    public int max_skill_level;
    public float skill_exp_rate;
    public int tu_tien_success_rate;
    public int tu_tien_boi_success_rate;
    public long tu_tien_base_beri_cost;
    public int tu_tien_base_hon_cost;
    public int tu_tien_point_gain;
    public int spam_icon_limit;
    public int item_usage_cooldown;
    public float atk_scaling;
    public float def_scaling;
    public float hp_scaling;
    public float mp_scaling;
    public boolean write_log_to_file;
    public boolean enable_system_log = true;
    public boolean enable_warn_log = true;
    public boolean enable_game_log = true;
    public boolean enable_session_log = true;

    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public void init() {
        index_mob = 1;
        try {
            this.load_config();
            this.reloadConfig();
            load_fusion_data();
            devilfruit.DevilFruitManager.load();
            // load msg data
            ByteArrayInputStream bais = new ByteArrayInputStream(Util.loadfile("data/msg/hair"));
            DataInputStream dis = new DataInputStream(bais);
            // load_hair(dis, 103);
            dis.close();
            bais.close();
            //
            bais = new ByteArrayInputStream(Util.loadfile("data/msg/head"));
            dis = new DataInputStream(bais);
            load_hair(dis, 108);
            dis.close();
            bais.close();
            // load da than thoai
            DaThanThoai.data_shop = Util.loadfile("data/msg/dathanthoaishop");
        } catch (Exception e) {
            GameLogger.error("config load err!", e);
            System.exit(0);
        }
        load_database();
        core.OceanMazeManager.init();
        start_service();
    }

    public void logDDOS(String message) {
        try (java.io.FileWriter fw = new java.io.FileWriter("ddos_log.txt", true);
                java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
                java.io.PrintWriter out = new java.io.PrintWriter(bw)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss");
            out.println("[" + now.format(formatter) + "] " + message);
        } catch (IOException e) {
            GameLogger.error("Failed to write to ddos_log.txt", e);
        }
    }

    private void start_service() {
        for (Map[] mapall : Map.ENTRYS) {
            for (Map map : mapall) {
                map.start_map();
            }
        }
        // load static class
        a = Rebuild_Item.ID_SELL.length;
        a = UpgradeItem.DATA.size();
        a = Body.Point3_Template_hp.length;
        a = ItemBoat.ENTRYS.size();
        a = ItemSell.ENTRYS.size();
        a = VongQuay.ID_ITEM.length;
        a = Level.ENTRYS.length;
        a = Skill_info.EXP.length;
        //
        GameLogger.info("Start Service OK, " + a);

        // [CẢI TIẾN] Khôi phục người chơi ủy thác offline sau bảo trì
        restoreOfflinePlayers();
    }

    private void stop_service() {
        MAP_POOL.shutdown();
        try {
            if (!MAP_POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                MAP_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            GameLogger.warn("Manager.stop_service: Interrupted", e);
            MAP_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
        for (int i = 0; i < Map.get_map_plus().size(); i++) {
            Map.get_map_plus().get(i).stop_map();
        }
    }

    private void load_hair(DataInputStream dis, int type) throws IOException {
        dis.readByte();
        dis.readUTF();
        dis.readByte();
        int n = dis.readShort();
        for (int i = 0; i < n; i++) {
            ItemHair temp = ItemHair.readUpdateItemHair(dis);
            temp.type = (byte) type;
            ItemHair.ENTRYS.add(temp);
        }
    }

    public void load_parts() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = SQL.gI().getCon();
            ps = conn.prepareStatement("SELECT * FROM `parts`;");
            rs = ps.executeQuery();
            List<Part> tempParts = new ArrayList<>();
            while (rs.next()) {
                byte type = rs.getByte("type");
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("data"));
                Part part = new Part(type);
                part.id = rs.getShort("id");
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    part.pi[i] = new PartImg();
                    part.pi[i].id = Short.parseShort(js_in.get(0).toString());
                    part.pi[i].dx = Byte.parseByte(js_in.get(1).toString());
                    part.pi[i].dy = Byte.parseByte(js_in.get(2).toString());
                }
                tempParts.add(part);
            }
            Part.ENTRY = tempParts;
            GameLogger.info("Load parts ok (" + Part.ENTRY.size() + ")");
            syncPartsToAll();
        } catch (Exception e) {
            GameLogger.error("Load parts error", e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }
    }

    public void syncPartsToAll() {
        synchronized (io.SessionManager.CLIENT_ENTRYS) {
            for (io.Session ss : io.SessionManager.CLIENT_ENTRYS) {
                if (ss != null && ss.p != null) {
                    for (Part part : Part.ENTRY) {
                        try {
                            Message m = new Message(-82);
                            m.writer().writeShort(part.id);
                            m.writer().writeByte(part.type);
                            for (int i = 0; i < part.pi.length; i++) {
                                m.writer().writeShort(part.pi[i].id);
                                m.writer().writeByte(part.pi[i].dx);
                                m.writer().writeByte(part.pi[i].dy);
                            }
                            ss.addmsg(m);
                            m.cleanup();
                        } catch (Exception e) {
                        }
                    }
                    try {
                        ss.p.setin4();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void load_database() {
        Connection conn = null;
        Statement ps = null;
        ResultSet rs = null;
        try {
            conn = SQL.gI().getCon();
            ps = conn.createStatement();
            // reset online status
            ps.executeUpdate("UPDATE `accounts` SET `onl` = 0;");
            // load mobs
            String query = "SELECT * FROM `mobs`;";
            MobTemplate.MAP.clear();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                MobTemplate temp = new MobTemplate();
                temp.mob_id = Short.parseShort(rs.getString("id"));
                temp.name = rs.getString("name");
                temp.level = Short.parseShort(rs.getString("level"));
                // Injected by StatsTool
                temp.hp_max = (long) (Long.parseLong(rs.getString("hp")) * Skill_info.GLOBAL_MOB_HP_RATE);
                temp.hOne = Short.parseShort(rs.getString("hOne"));
                temp.typemove = Byte.parseByte(rs.getString("typemove"));
                temp.ishuman = Byte.parseByte(rs.getString("ishuman"));
                temp.typemonster = Byte.parseByte(rs.getString("typemonster"));
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("idicon"));
                if (temp.ishuman == 0) {
                    temp.icon = Short.parseShort(js.get(1).toString());
                } else if (temp.ishuman == 1) {
                    temp.head = Short.parseShort(js.get(1).toString());
                    temp.hair = Short.parseShort(js.get(2).toString());
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(3).toString());
                    temp.wearing = new short[js2.size()];
                    for (int i = 0; i < temp.wearing.length; i++) {
                        temp.wearing[i] = Short.parseShort(js2.get(i).toString());
                    }
                }
                js.clear();
                js = (JSONArray) JSONValue.parse(rs.getString("skill"));
                temp.skill = new short[js.size()];
                for (int i = 0; i < temp.skill.length; i++) {
                    temp.skill[i] = Short.parseShort(js.get(i).toString());
                }
                js.clear();
                MobTemplate.ENTRYS.add(temp);
                MobTemplate.MAP.put((int) temp.mob_id, temp);
            }
            rs.close();
            GameLogger.info("load mob ok");
            query = "SELECT * FROM `shoptichluy` ORDER BY `point` ASC;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ShopTichLuy temp = new ShopTichLuy();
                temp.id = rs.getShort("id");
                temp.type = rs.getByte("type");
                temp.point = rs.getInt("point");
                temp.info = rs.getString("info");
                temp.limit = rs.getInt("limit");
                temp.limit_data = new HashMap<>();
                JSONArray jsar = (JSONArray) JSONValue.parse(rs.getString("limit_data"));
                for (int i = 0; i < jsar.size(); i++) {
                    JSONArray js_in = (JSONArray) jsar.get(i);
                    int value = Integer.parseInt(js_in.get(1).toString());
                    temp.limit_data.put(js_in.get(0).toString(), value);
                }
                ShopTichLuy.ENTRY.add(temp);
            }
            rs.close();
            // load map
            query = "SELECT * FROM `maps`;";
            rs = ps.executeQuery(query);
            MapTemplate.ENTRYS = new ArrayList<>();
            while (rs.next()) {
                short id_map = rs.getShort("id");
                //
                MapTemplate map_temp = new MapTemplate();
                map_temp.id = id_map;
                map_temp.name = rs.getString("name");
                map_temp.max_zone = rs.getByte("maxzone");
                map_temp.max_player = rs.getByte("maxplayer");
                // npc
                JSONArray js_npc = (JSONArray) JSONValue.parse(rs.getString("npcs"));
                map_temp.npcs = new ArrayList<>();
                for (int i = 0; i < js_npc.size(); i++) {
                    JSONArray js_npc_temp = (JSONArray) JSONValue.parse(js_npc.get(i).toString());
                    Npc npc = new Npc();
                    npc.iditem = Short.parseShort(js_npc_temp.get(0).toString());
                    npc.name = js_npc_temp.get(1).toString();
                    npc.namegt = js_npc_temp.get(2).toString();
                    npc.chat = js_npc_temp.get(3).toString();
                    npc.x = Short.parseShort(js_npc_temp.get(4).toString());
                    npc.y = Short.parseShort(js_npc_temp.get(5).toString());
                    npc.isPerson = Byte.parseByte(js_npc_temp.get(6).toString());
                    npc.typeIcon = Byte.parseByte(js_npc_temp.get(7).toString());
                    npc.wBlock = Byte.parseByte(js_npc_temp.get(8).toString());
                    npc.hBlock = Byte.parseByte(js_npc_temp.get(9).toString());
                    npc.b3 = Byte.parseByte(js_npc_temp.get(10).toString());
                    JSONArray js_npc_temp_2 = (JSONArray) JSONValue.parse(js_npc_temp.get(11).toString());
                    npc.dataFrame = new byte[js_npc_temp_2.size()];
                    for (int j = 0; j < npc.dataFrame.length; j++) {
                        npc.dataFrame[j] = Byte.parseByte(js_npc_temp_2.get(j).toString());
                    }
                    npc.head = Short.parseShort(js_npc_temp.get(12).toString());
                    npc.hair = Short.parseShort(js_npc_temp.get(13).toString());
                    JSONArray js_npc_temp_3 = (JSONArray) JSONValue.parse(js_npc_temp.get(14).toString());
                    npc.wearing = new short[js_npc_temp_3.size()];
                    for (int k = 0; k < npc.wearing.length; k++) {
                        npc.wearing[k] = Short.parseShort(js_npc_temp_3.get(k).toString());
                    }
                    map_temp.npcs.add(npc);
                }
                js_npc.clear();
                js_npc = (JSONArray) JSONValue.parse(rs.getString("boat"));
                map_temp.list_boat = new ArrayList<>();
                for (int i = 0; i < js_npc.size(); i++) {
                    JSONArray js_temp = (JSONArray) js_npc.get(i);
                    Boat_In_Map temp_boat = new Boat_In_Map();
                    temp_boat.x = Short.parseShort(js_temp.get(0).toString());
                    temp_boat.y = Short.parseShort(js_temp.get(1).toString());
                    map_temp.list_boat.add(temp_boat);
                }
                js_npc.clear();
                map_temp.vgos = new ArrayList<>();
                js_npc = (JSONArray) JSONValue.parse(rs.getString("vgos"));
                for (int i = 0; i < js_npc.size(); i++) {
                    JSONArray js_0 = (JSONArray) js_npc.get(i);
                    Vgo vgo_temp = new Vgo();
                    vgo_temp.id_map_go = Short.parseShort(js_0.get(0).toString());
                    vgo_temp.xold = Short.parseShort(js_0.get(1).toString());
                    vgo_temp.yold = Short.parseShort(js_0.get(2).toString());
                    vgo_temp.xnew = Short.parseShort(js_0.get(3).toString());
                    vgo_temp.ynew = Short.parseShort(js_0.get(4).toString());
                    if (vgo_temp.id_map_go != -1) {
                        map_temp.vgos.add(vgo_temp);
                    }
                }
                js_npc.clear();
                map_temp.type_view_p = rs.getByte("typeViewPlayer");
                map_temp.b = rs.getByte("b");
                map_temp.specMap = rs.getByte("specMap");
                js_npc = (JSONArray) JSONValue.parse(rs.getString("data"));
                map_temp.data = new byte[2][];
                for (int i = 0; i < js_npc.size(); i++) {
                    JSONArray js_in = (JSONArray) js_npc.get(i);
                    map_temp.data[i] = new byte[js_in.size()];
                    for (int j = 0; j < map_temp.data[i].length; j++) {
                        map_temp.data[i][j] = Byte.parseByte(js_in.get(j).toString());
                    }
                }
                js_npc.clear();
                js_npc = (JSONArray) JSONValue.parse(rs.getString("MapBack"));
                map_temp.IDBack = Byte.parseByte(js_npc.get(0).toString());
                map_temp.HBack = Short.parseShort(js_npc.get(1).toString());
                map_temp.maxW = Short.parseShort(js_npc.get(2).toString());
                map_temp.maxH = Short.parseShort(js_npc.get(3).toString());
                js_npc.clear();
                map_temp.id_eff_map = rs.getByte("id_eff_map");
                map_temp.level = rs.getByte("level");
                map_temp.typeChangeMap = rs.getByte("typeChangeMap");
                js_npc = (JSONArray) JSONValue.parse(rs.getString("mPosMapTrain"));
                map_temp.mPosMapTrain = new byte[js_npc.size()][];
                for (int i = 0; i < js_npc.size(); i++) {
                    JSONArray js_in = (JSONArray) js_npc.get(i);
                    map_temp.mPosMapTrain[i] = new byte[js_in.size()];
                    for (int j = 0; j < map_temp.mPosMapTrain[i].length; j++) {
                        map_temp.mPosMapTrain[i][j] = Byte.parseByte(js_in.get(j).toString());
                    }
                }
                js_npc.clear();
                map_temp.strTimeChange = rs.getString("strTimeChange");
                MapTemplate.ENTRYS.add(map_temp);
                //
                String mob_json = rs.getString("mobs");
                Map[] m_temp = new Map[map_temp.max_zone];
                for (int i2 = 0; i2 < m_temp.length; i2++) {
                    m_temp[i2] = new Map();
                    m_temp[i2].zone_id = (byte) i2;
                    m_temp[i2].template = map_temp;
                    JSONArray js = (JSONArray) JSONValue.parse(mob_json);
                    m_temp[i2].list_mob = new int[js.size()];
                    for (int i = 0; i < js.size(); i++) {
                        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                        Mob temp = new Mob();
                        temp.mob_template = MobTemplate.get_it_by_id(Integer.parseInt(js2.get(0).toString()));
                        temp.x = Short.parseShort(js2.get(1).toString());
                        temp.y = Short.parseShort(js2.get(2).toString());
                        temp.hp_max = temp.mob_template.hp_max;
                        temp.hp = temp.hp_max;
                        temp.level = temp.mob_template.level;
                        temp.isdie = false;
                        temp.id_target = -1;
                        temp.index = this.index_mob;
                        temp.map = m_temp[i2];
                        Mob.ENTRYS.put(this.index_mob, temp);
                        m_temp[i2].list_mob[i] = this.index_mob;
                        this.index_mob++;
                    }
                }
                Map.ENTRYS.add(m_temp);
                Map.MAP_BY_ID.put((int) id_map, m_temp);
            }
            rs.close();
            for (int i = 0; i < MapTemplate.ENTRYS.size(); i++) {
                for (int j = 0; j < MapTemplate.ENTRYS.get(i).vgos.size(); j++) {
                    Vgo vgo = MapTemplate.ENTRYS.get(i).vgos.get(j);
                    vgo.map_go = Map.get_map_by_id(vgo.id_map_go);
                    if (vgo.map_go == null) {
                        vgo.map_go = Map.get_map_by_id(1);
                    }
                }
            }
            GameLogger.info("load map ok");
            load_parts();
            // load item 3
            query = "SELECT * FROM `item3`;";
            ItemTemplate3.MAP.clear();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemTemplate3 temp = new ItemTemplate3();
                temp.id = rs.getShort("id");
                temp.name = rs.getString("name");
                temp.clazz = rs.getByte("clazz");
                temp.typeEquip = rs.getByte("typeequip");
                temp.icon = rs.getShort("icon");
                temp.level = rs.getShort("level");
                temp.color = rs.getByte("color");
                temp.typelock = rs.getByte("typelock");
                temp.numHoleDaDuc = rs.getByte("numHoleDaDuc");
                // temp.valueChetac = rs.getShort("chetac");
                temp.valueChetac = (short) (100);
                temp.isHoanMy = rs.getByte("ishoanmy");
                temp.valueKichAn = rs.getByte("valuekichan");
                // core.GameLogger.info(temp.id);
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("op_1"));
                temp.option_item = new ArrayList<>();
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                    temp.option_item.add(new Option(Byte.parseByte(js2.get(0).toString()),
                            Short.parseShort(js2.get(1).toString())));
                }
                js.clear();
                temp.option_item_2 = new ArrayList<>();
                js = (JSONArray) JSONValue.parse(rs.getString("op_2"));
                for (int k = 0; k < js.size(); k++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(k).toString());
                    temp.option_item_2.add(new Option(Byte.parseByte(js2.get(0).toString()),
                            Short.parseShort(js2.get(1).toString())));
                }
                js.clear();
                temp.numLoKham = rs.getByte("numlokham");
                js = (JSONArray) JSONValue.parse(rs.getString("mdakham"));
                temp.mdakham = new short[js.size()];
                for (int l = 0; l < temp.mdakham.length; l++) {
                    temp.mdakham[l] = Short.parseShort(js.get(l).toString());
                }
                temp.part = rs.getShort("part");
                temp.beri = rs.getInt("beri");
                temp.ruby = rs.getInt("ruby");
                temp.tb2 = rs.getInt("tb2");
                // Part.get_part(temp.id);;
                ItemTemplate3.ENTRYS.add(temp);
                ItemTemplate3.MAP.put((int) temp.id, temp);
            }
            rs.close();
            // load info item4
            query = "SELECT * FROM `item4_info`;";
            ItemTemplate4_Info.ENTRY.clear();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemTemplate4_Info temp = new ItemTemplate4_Info();
                temp.id = rs.getShort("id");
                temp.info = rs.getString("info");
                ItemTemplate4_Info.ENTRY.put(temp.id, temp);
            }
            rs.close();
            // load item temp 4
            query = "SELECT * FROM `item4`;";
            ItemTemplate4.MAP.clear();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemTemplate4 temp = new ItemTemplate4();
                temp.id = rs.getShort("id");
                temp.name = rs.getString("name");
                temp.icon = rs.getShort("icon");
                temp.indexInfoPotion = rs.getShort("indexInfoPotion");
                temp.beri = rs.getInt("price");
                temp.ruby = rs.getShort("priceruby");
                temp.istrade = rs.getByte("istrade");
                temp.type = rs.getByte("hpmpother");
                temp.timedelay = rs.getShort("timedelay");
                temp.value = rs.getShort("value");
                temp.timeactive = rs.getShort("timeactive");
                temp.nameuse = rs.getString("nameuse");
                ItemTemplate4.ENTRYS.add(temp);
                ItemTemplate4.MAP.put((int) temp.id, temp);
            }
            rs.close();
            // load item temp 7
            query = "SELECT * FROM `item7`;";
            ItemTemplate7.MAP.clear();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemTemplate7 temp = new ItemTemplate7();
                temp.id = rs.getShort("id");
                temp.name = rs.getString("name");
                temp.type = rs.getByte("type");
                temp.icon = rs.getByte("icon");
                temp.price = rs.getInt("price");
                temp.priceruby = rs.getShort("priceruby");
                temp.istrade = rs.getByte("istrade");
                ItemTemplate7.ENTRYS.add(temp);
                ItemTemplate7.MAP.put((int) temp.id, temp);
            }
            rs.close();
            GameLogger.info("load item ok");
            // load skill temp
            Skill_Template.ENTRYS = new ArrayList<>();
            query = "SELECT * FROM `skill` ORDER BY `id_index`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                // int id = rs.getInt("id");
                Skill_Template temp_add = new Skill_Template(rs.getInt("id_index"),
                        rs.getInt("id_2"), rs.getShort("icon"), rs.getByte("typeSkill"),
                        rs.getByte("typeBuff"), rs.getString("name"), rs.getShort("typeEffSkill"),
                        rs.getShort("range"));
                temp_add.getData(rs.getByte("nTarget"), rs.getShort("rangeLan"),
                        rs.getInt("damage"), rs.getShort("manaLost"), rs.getInt("timeDelay"),
                        rs.getByte("nKick"), rs.getString("info"), rs.getShort("Lv_RQ"),
                        rs.getByte("typeDevil"));
                // Injected by StatsTool
                temp_add.manaLost = (short) (temp_add.manaLost * Skill_info.GLOBAL_MANA_RATE);
                temp_add.timeDelay = (int) (temp_add.timeDelay * Skill_info.GLOBAL_COOLDOWN_RATE);
                temp_add.op = new ArrayList<>();
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("option"));
                for (int j = 0; j < js.size(); j++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(j).toString());
                    temp_add.op.add(new Option(Byte.parseByte(js2.get(0).toString()),
                            Integer.parseInt(js2.get(1).toString())));
                }
                js.clear();
                js = (JSONArray) JSONValue.parse(rs.getString("EffSpec"));
                temp_add.idEffSpec = Byte.parseByte(js.get(0).toString());
                temp_add.perEffSpec = Short.parseShort(js.get(1).toString());
                temp_add.timeEffSpec = Short.parseShort(js.get(2).toString());
                js.clear();
                Skill_Template.ENTRYS.add(temp_add);
            }
            rs.close();
            GameLogger.info("load skill ok");
            // load item option temp
            ItemOptionTemplate.ENTRYS = new ArrayList<>();
            query = "SELECT * FROM `itemoption`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemOptionTemplate temp = new ItemOptionTemplate();
                temp.id = rs.getShort("id");
                temp.name = rs.getString("name");
                temp.color = rs.getByte("color");
                temp.percent = rs.getByte("percent");
                ItemOptionTemplate.ENTRYS.add(temp);
            }
            rs.close();
            GameLogger.info("load item op temp ok");
            // load item fashion info
            ItemFashion.ENTRYS = new ArrayList<>();
            query = "SELECT * FROM `fashiontemplate`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                byte id = rs.getByte("id");
                short icon = rs.getShort("icon");
                String name = rs.getString("name");
                String info = rs.getString("info");
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("mwear"));
                short[] wear = new short[js.size()];
                for (int i = 0; i < wear.length; i++) {
                    wear[i] = Short.parseShort(js.get(i).toString());
                }
                js.clear();
                js = (JSONArray) JSONValue.parse(rs.getString("op"));
                List<Option> op = new ArrayList<>();
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                    op.add(new Option(Byte.parseByte(js2.get(0).toString()),
                            Integer.parseInt(js2.get(1).toString())));
                }
                ItemFashion.ENTRYS
                        .add(new ItemFashion(id, icon, name, info, wear, op, rs.getInt("price")));
            }
            rs.close();
            core.GameLogger.info("load fashion temp ok");

            // load hair
            query = "SELECT * FROM `itemhair`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemHair.ENTRYS.add(ItemHair.read_json_it_hair(rs));
            }
            rs.close();
            GameLogger.info("load item hair ok");
            ItemTemplate8.ENTRYS = new ArrayList<>();
            ItemTemplate8.MAP.clear();
            query = "SELECT * FROM `item8`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                ItemTemplate8 temp = new ItemTemplate8();
                temp.id = rs.getShort("id");
                temp.name = rs.getString("name");
                temp.icon = rs.getShort("icon");
                temp.info = rs.getString("info");
                temp.beri = rs.getInt("price");
                temp.ruby = rs.getShort("priceruby");
                temp.istrade = rs.getByte("istrade");
                temp.type = rs.getByte("hpmpother");
                temp.timedelay = rs.getShort("timedelay");
                temp.value = rs.getShort("value");
                temp.timeactive = rs.getShort("timeactive");
                temp.nameuse = rs.getString("nameuse");
                ItemTemplate8.ENTRYS.add(temp);
                ItemTemplate8.MAP.put((int) temp.id, temp);
            }
            rs.close();
            GameLogger.info("load item clan ok");
            Clan.ENTRY = new ArrayList<>();
            Clan.BXH = new ArrayList<>();
            query = "SELECT * FROM `clan`;";
            Set<String> name_check = new HashSet<>();
            rs = ps.executeQuery(query);
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getShort("id");
                clan.name = rs.getString("name");
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("info"));
                clan.icon = Short.parseShort(js.get(0).toString());
                clan.level = Short.parseShort(js.get(1).toString());
                clan.xp = Long.parseLong(js.get(2).toString());
                clan.maxAttri = Short.parseShort(js.get(3).toString());
                clan.pointAttri = Short.parseShort(js.get(4).toString());
                clan.trungsinh = Byte.parseByte(js.get(5).toString());
                clan.countAction = Integer.parseInt(js.get(6).toString());
                clan.ruby = Long.parseLong(js.get(7).toString());
                clan.beri = Long.parseLong(js.get(8).toString());
                clan.allowRequest = Byte.parseByte(js.get(9).toString());
                clan.opAttri = new short[] { 0, 0, 0, 0, 0 };
                JSONArray js2 = (JSONArray) js.get(10);
                for (int i = 0; i < clan.opAttri.length; i++) {
                    clan.opAttri[i] = Short.parseShort(js2.get(i).toString());
                }
                clan.thongbao = rs.getString("notice");
                js.clear();
                clan.chat = new ArrayList<>();
                clan.mem_request = new ArrayList<>();
                clan.members = new ArrayList<>();
                js = (JSONArray) JSONValue.parse(rs.getString("member"));
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    Clan_member mem = new Clan_member();
                    mem.id = (short) i;
                    mem.name = Util.getRawName(js_in.get(0).toString());
                    mem.level = Short.parseShort(js_in.get(1).toString());
                    mem.levelInclan = Byte.parseByte(js_in.get(2).toString());
                    mem.donate = Short.parseShort(js_in.get(3).toString());
                    mem.gopRuby = Short.parseShort(js_in.get(4).toString());
                    mem.numquest = Short.parseShort(js_in.get(5).toString());
                    mem.conghien = Integer.parseInt(js_in.get(6).toString());
                    mem.head = Short.parseShort(js_in.get(7).toString());
                    mem.hair = Short.parseShort(js_in.get(8).toString());
                    mem.hat = Short.parseShort(js_in.get(9).toString());
                    mem.clazz = Byte.parseByte(js_in.get(10).toString());
                    //
                    boolean add = true;
                    // int num_clazz = 0;
                    // for (int j = 0; j < clan.members.size(); j++) {
                    // if (clan.members.get(j).clazz == mem.clazz) {
                    // num_clazz++;
                    // }
                    // }
                    // if (num_clazz >= 4) {
                    // core.GameLogger.info("err load clan >=4 " + clan.name + " " + mem.name);
                    // add = false;
                    // }
                    if (add && !name_check.contains(mem.name)) {
                        name_check.add(mem.name);
                    } else {
                        add = false;
                    }
                    if (add) {
                        clan.members.add(mem);
                    }
                }
                js.clear();
                clan.list_it = new ArrayList<>();
                js = (JSONArray) JSONValue.parse(rs.getString("item"));
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    ItemBag47 itemBag47 = new ItemBag47();
                    itemBag47.category = 4;
                    itemBag47.id = Short.parseShort(js_in.get(0).toString());
                    itemBag47.quant = Short.parseShort(js_in.get(1).toString());
                    clan.list_it.add(itemBag47);
                }
                clan.buff = new ArrayList<>();
                js = (JSONArray) JSONValue.parse(rs.getString("buff"));
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    clan.buff.add(new EffTemplate(Byte.parseByte(js_in.get(0).toString()),
                            Integer.parseInt(js_in.get(1).toString()),
                            Long.parseLong(js_in.get(2).toString())));
                }
                Clan.add_new_clan(clan);
            }
            rs.close();
            GameLogger.info("load clan ok");
            // load quest
            query = "SELECT * FROM `quests`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                Quest.add(rs);
            }
            Quest.add_finish_quest();
            rs.close();
            GameLogger.info("load quest ok");
            // load pet template
            query = "SELECT * FROM `pet_template`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                Pet tempPet = new Pet();
                tempPet.id = rs.getShort("id");
                tempPet.name = rs.getString("name");
                tempPet.type = rs.getByte("type");
                tempPet.icon = rs.getShort("icon");
                tempPet.frame = rs.getShort("frame");
                Pet.ENTRY.add(tempPet);
            }
            rs.close();
            GameLogger.info("load pet ok");
            // load danhhieu template
            query = "SELECT * FROM `danhhieu_template`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                DanhHieu tempDanhHieu = new DanhHieu();
                tempDanhHieu.id = rs.getShort("id");
                tempDanhHieu.name = rs.getString("name");
                tempDanhHieu.eff = rs.getShort("eff");
                // Load options column
                try {
                    String op_data = rs.getString("options");
                    if (op_data != null && !op_data.isEmpty()) {
                        JSONArray js_op = (JSONArray) JSONValue.parse(op_data);
                        for (int i = 0; i < js_op.size(); i++) {
                            JSONArray js_in = (JSONArray) js_op.get(i);
                            tempDanhHieu.options.add(new Option(Byte.parseByte(js_in.get(0).toString()),
                                    Integer.parseInt(js_in.get(1).toString())));
                        }
                    }
                } catch (Exception e) {
                    GameLogger.warn("[Manager] Lỗi đọc options danh hiệu: " + e.getMessage());
                }
                DanhHieu.ENTRY.add(tempDanhHieu);
            }
            rs.close();
            core.GameLogger.info("load danhhieu ok");

            // load shop extol
            query = "SELECT * FROM `shop_extol`;";
            try {
                rs = ps.executeQuery(query);
                while (rs.next()) {
                    ShopExtol temp = new ShopExtol();
                    temp.id = rs.getInt("id");
                    temp.id_title = rs.getShort("id_title");
                    temp.price = rs.getLong("price");
                    temp.icon = rs.getShort("icon");
                    temp.info = rs.getString("info");
                    ShopExtol.ENTRY.add(temp);
                }
                rs.close();
                GameLogger.info("load shop extol ok");
            } catch (Exception e) {
                core.GameLogger.error("Lỗi load shop_extol: " + e.getMessage());
            }
            SkinTitleManager.init(); // Khởi tạo cache Skin sau khi DanhHieu load xong
            query = "SELECT * FROM `market`;";
            rs = ps.executeQuery(query);
            while (rs.next()) {
                Market tempMarket = new Market();
                tempMarket.type = rs.getByte("id");
                JSONObject jsob = (JSONObject) JSONValue.parse(rs.getString("data"));
                tempMarket.item3 = new ArrayList<>();
                JSONArray js = (JSONArray) JSONValue.parse(jsob.get("item3").toString());
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) js.get(i);
                    ItemMarket itemMarket = new ItemMarket();
                    itemMarket.load_json(js2);
                    if (itemMarket.index != -1) {
                        tempMarket.item3.add(itemMarket);
                    }
                }
                js.clear();
                tempMarket.item47 = new ArrayList<>();
                js = (JSONArray) JSONValue.parse(jsob.get("item47").toString());
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) js.get(i);
                    PotionMarket potionMarket = new PotionMarket();
                    potionMarket.load_json(js2);
                    if (potionMarket.index != -1) {
                        tempMarket.item47.add(potionMarket);
                    }
                }
                js.clear();
                Market.ENTRY.add(tempMarket);
            }
            // rs.close();
            GameLogger.info("load market ok");
            GiftTemplate.load();
        } catch (SQLException e) {
            GameLogger.error("Manager.load_database: SQL error", e);
            System.exit(0);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Manager.load_database: SQL resource close error", e);
            }
        }
    }

    private void load_config() throws IOException {
        final byte[] ab = Util.loadfile("htth.conf");
        if (ab == null) {
            core.GameLogger.info("Config file not found!");
            System.exit(0);
        }
        final String data = new String(ab);
        this.configMap.clear();
        final StringBuilder sbd = new StringBuilder();
        boolean bo = false;
        for (int i = 0; i <= data.length(); ++i) {
            final char es;
            if (i == data.length() || (es = data.charAt(i)) == '\n') {
                bo = false;
                final String sbf = sbd.toString().trim();
                if (sbf != null && !sbf.equals("") && sbf.charAt(0) != '#') {
                    final int j = sbf.indexOf(58);
                    if (j > 0) {
                        final String key = sbf.substring(0, j).trim();
                        final String value = sbf.substring(j + 1).trim();
                        this.configMap.put(key, value);
                        core.GameLogger.info("config: " + key + ": " + value);
                    }
                }
                sbd.setLength(0);
            } else {
                if (es == '#') {
                    bo = true;
                }
                if (!bo) {
                    sbd.append(es);
                }
            }
        }
        if (configMap.containsKey("port")) {
            this.server_port = Integer.parseInt(configMap.get("port"));
        } else {
            this.server_port = 2239;
        }
        if (configMap.containsKey("debug")) {
            this.debug = Boolean.parseBoolean(configMap.get("debug"));
        } else {
            this.debug = false;
        }
        if (configMap.containsKey("mysql-host")) {
            this.mysql_host = configMap.get("mysql-host");
        } else {
            this.mysql_host = "127.0.0.1";
        }
        if (configMap.containsKey("mysql-user")) {
            this.mysql_user = configMap.get("mysql-user");
        } else {
            this.mysql_user = "root";
        }
        if (configMap.containsKey("mysql-password")) {
            this.mysql_pass = configMap.get("mysql-password");
        } else {
            this.mysql_pass = "12345678";
        }
        if (configMap.containsKey("mysql-database")) {
            this.mysql_database = configMap.get("mysql-database");
        } else {
            this.mysql_database = "database";
        }
        if (configMap.containsKey("exp")) {
            this.exp = Integer.parseInt(configMap.get("exp"));
        } else {
            this.exp = 1;
        }
        if (configMap.containsKey("serveradmin")) {
            this.server_admin = Boolean.parseBoolean(configMap.get("serveradmin"));
        } else {
            this.server_admin = false;
        }
        if (configMap.containsKey("servertest")) {
            this.server_test = Boolean.parseBoolean(configMap.get("servertest"));
        } else {
            this.server_test = false;
        }
        if (configMap.containsKey("serverdomain")) {
            this.server_domain = configMap.get("serverdomain");
        } else {
            this.server_domain = "default-domain.com"; // Giá trị mặc định nếu không có trong file cấu hình
        }

        if (configMap.containsKey("xnap")) {
            try {
                this.xnap = Integer.parseInt(configMap.get("xnap"));
            } catch (NumberFormatException e) {
                this.xnap = 0; // Giá trị mặc định nếu không đọc được từ cấu hình
            }
        } else {
            this.xnap = 0;
        }

        this.enable_websocket = configMap.containsKey("enable-websocket")
                ? Boolean.parseBoolean(configMap.get("enable-websocket"))
                : false;
        this.websocket_port = configMap.containsKey("websocket-port")
                ? Integer.parseInt(configMap.get("websocket-port"))
                : 2240;

        // --- Load Game Configurations ---
        this.global_dame_rate = configMap.containsKey("global-dame-rate")
                ? Float.parseFloat(configMap.get("global-dame-rate"))
                : 0.1f;
        this.mob_hp_rate = configMap.containsKey("mob-hp-rate") ? Float.parseFloat(configMap.get("mob-hp-rate")) : 0.1f;
        this.global_exp_rate = configMap.containsKey("global-exp-rate")
                ? Float.parseFloat(configMap.get("global-exp-rate"))
                : 5.0f;
        this.global_gold_rate = configMap.containsKey("global-gold-rate")
                ? Float.parseFloat(configMap.get("global-gold-rate"))
                : 2.0f;
        this.global_drop_rate = configMap.containsKey("global-drop-rate")
                ? Float.parseFloat(configMap.get("global-drop-rate"))
                : 2.0f;
        this.mana_cost_rate = configMap.containsKey("mana-cost-rate")
                ? Float.parseFloat(configMap.get("mana-cost-rate"))
                : 0.05f;
        this.cooldown_rate = configMap.containsKey("cooldown-rate") ? Float.parseFloat(configMap.get("cooldown-rate"))
                : 0.0f;
        this.mob_respawn_time = configMap.containsKey("mob-respawn-time")
                ? Integer.parseInt(configMap.get("mob-respawn-time"))
                : 7;
        this.potential_per_level = configMap.containsKey("potential-per-level")
                ? Integer.parseInt(configMap.get("potential-per-level"))
                : 2;
        this.stat_point_max = configMap.containsKey("stat-point-max")
                ? Integer.parseInt(configMap.get("stat-point-max"))
                : 80;
        this.spirit_extra_mana = configMap.containsKey("spirit-extra-mana")
                ? Float.parseFloat(configMap.get("spirit-extra-mana"))
                : 50.0f;
        this.max_skill_level = configMap.containsKey("max-skill-level")
                ? Integer.parseInt(configMap.get("max-skill-level"))
                : 1000;
        this.skill_exp_rate = configMap.containsKey("skill-exp-rate")
                ? Float.parseFloat(configMap.get("skill-exp-rate"))
                : 100.0f;

        this.tu_tien_success_rate = configMap.containsKey("tu-tien-success-rate")
                ? Integer.parseInt(configMap.get("tu-tien-success-rate"))
                : 100;
        this.tu_tien_boi_success_rate = configMap.containsKey("tu-tien-boi-success-rate")
                ? Integer.parseInt(configMap.get("tu-tien-boi-success-rate"))
                : 50;
        this.tu_tien_base_beri_cost = configMap.containsKey("tu-tien-base-beri-cost")
                ? Long.parseLong(configMap.get("tu-tien-base-beri-cost"))
                : 20000000L;
        this.tu_tien_base_hon_cost = configMap.containsKey("tu-tien-base-hon-cost")
                ? Integer.parseInt(configMap.get("tu-tien-base-hon-cost"))
                : 20;
        this.tu_tien_point_gain = configMap.containsKey("tu-tien-point-gain")
                ? Integer.parseInt(configMap.get("tu-tien-point-gain"))
                : 10;
        this.spam_icon_limit = configMap.containsKey("spam-icon-limit")
                ? Integer.parseInt(configMap.get("spam-icon-limit"))
                : 1000;
        this.item_usage_cooldown = configMap.containsKey("item-usage-cooldown")
                ? Integer.parseInt(configMap.get("item-usage-cooldown"))
                : 500;
        this.atk_scaling = configMap.containsKey("atk-scaling")
                ? Float.parseFloat(configMap.get("atk-scaling"))
                : 1.0f;
        this.def_scaling = configMap.containsKey("def-scaling")
                ? Float.parseFloat(configMap.get("def-scaling"))
                : 1.0f;
        this.hp_scaling = configMap.containsKey("hp-scaling")
                ? Float.parseFloat(configMap.get("hp-scaling"))
                : 1.0f;
        this.mp_scaling = configMap.containsKey("mp-scaling")
                ? Float.parseFloat(configMap.get("mp-scaling"))
                : 1.0f;
        this.write_log_to_file = configMap.containsKey("write-log-to-file")
                ? Boolean.parseBoolean(configMap.get("write-log-to-file"))
                : false;
        this.enable_system_log = configMap.containsKey("enable-system-log")
                ? Boolean.parseBoolean(configMap.get("enable-system-log"))
                : true;
        this.enable_warn_log = configMap.containsKey("enable-warn-log")
                ? Boolean.parseBoolean(configMap.get("enable-warn-log"))
                : true;
        this.enable_game_log = configMap.containsKey("enable-game-log")
                ? Boolean.parseBoolean(configMap.get("enable-game-log"))
                : true;
        this.enable_session_log = configMap.containsKey("enable-session-log")
                ? Boolean.parseBoolean(configMap.get("enable-session-log"))
                : true;

        Skill_info.updateConfig();
    }

    public void reloadConfig() {
        try {
            core.GameLogger.info("Balance: Reloading configuration from htth.conf...");
            HashMap<String, String> newConfigMap = new HashMap<>();

            // 1. Read file to local map
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("htth.conf"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#") || !line.contains(":")) {
                    continue;
                }
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts[1].split("#")[0].trim();
                newConfigMap.put(key, value);
            }
            br.close();

            // 2. Parse to local variables (Double Buffering)
            int[] newStatCaps = new int[200];
            int[] newStatRates = new int[200];
            java.util.List<SoftCapStage>[] newSoftCaps = new java.util.ArrayList[200];
            java.util.Arrays.fill(newStatCaps, -1);
            java.util.Arrays.fill(newStatRates, 100);
            for (int i = 0; i < 200; i++) {
                newSoftCaps[i] = new java.util.ArrayList<>();
            }

            float newPvpRate = newConfigMap.containsKey("pvp-rate")
                    ? Float.parseFloat(newConfigMap.get("pvp-rate"))
                    : 1.0f;
            
            this.enable_system_log = newConfigMap.containsKey("enable-system-log")
                    ? Boolean.parseBoolean(newConfigMap.get("enable-system-log"))
                    : true;
            this.enable_warn_log = newConfigMap.containsKey("enable-warn-log")
                    ? Boolean.parseBoolean(newConfigMap.get("enable-warn-log"))
                    : true;
            this.enable_game_log = newConfigMap.containsKey("enable-game-log")
                    ? Boolean.parseBoolean(newConfigMap.get("enable-game-log"))
                    : true;
            this.enable_session_log = newConfigMap.containsKey("enable-session-log")
                    ? Boolean.parseBoolean(newConfigMap.get("enable-session-log"))
                    : true;

            for (String key : newConfigMap.keySet()) {
                if (key.startsWith("cap-")) {
                    try {
                        int id = Integer.parseInt(key.substring(4));
                        if (id >= 0 && id < 200) {
                            newStatCaps[id] = Integer.parseInt(newConfigMap.get(key));
                        }
                    } catch (Exception e) {
                    }
                } else if (key.startsWith("rate-")) {
                    try {
                        int id = Integer.parseInt(key.substring(5));
                        if (id >= 0 && id < 200) {
                            newStatRates[id] = Integer.parseInt(newConfigMap.get(key));
                        }
                    } catch (Exception e) {
                    }
                } else if (key.startsWith("soft-cap-")) {
                    try {
                        int id = Integer.parseInt(key.substring(9));
                        if (id >= 0 && id < 200) {
                            String[] stages = newConfigMap.get(key).split(",");
                            for (String s : stages) {
                                String[] sub = s.trim().split("-");
                                if (sub.length == 2) {
                                    long t = Long.parseLong(sub[0].trim());
                                    double f = Double.parseDouble(sub[1].trim());
                                    newSoftCaps[id].add(new SoftCapStage(t, f));
                                }
                            }
                            // Sắp xếp theo ngưỡng tăng dần để logic apply đúng thứ tự
                            newSoftCaps[id].sort((a, b) -> Long.compare(a.threshold, b.threshold));
                        }
                    } catch (Exception e) {
                    }
                }
            }

            // 3. Atomic Swap
            this.configMap = newConfigMap;
            this.statCaps = newStatCaps;
            this.statRates = newStatRates;
            this.statSoftCaps = newSoftCaps;
            this.pvpRate = newPvpRate;

            // Đồng bộ hóa ngay lập tức cho toàn bộ người chơi đang online
            io.SessionManager.refreshAllPlayers();

            core.GameLogger.info("Balance: Hot Reload successful. PvP Rate: " + this.pvpRate);
        } catch (Exception e) {
            core.GameLogger.error("Balance: Hot Reload failed!", e);
        }
    }

    public void close() {
        stop_service();
    }

    public void chatKTG(Player p, String text) throws IOException {
        if (p.getAdmin() == 1 || p.time_chat_ktg < System.currentTimeMillis()) {
            p.time_chat_ktg = System.currentTimeMillis() + 30_000L;
            chatKTG(1, text, 0);
            Service.send_box_ThongBao_OK(p, "Chat KTG thành công với nội dung: " + text);
        } else {
            Service.send_box_ThongBao_OK(p,
                    "Chờ " + (p.time_chat_ktg - System.currentTimeMillis()) / 1000L + "s");
        }
    }

    public void chatKTG(int type, String text, int color) throws IOException {
        Message m = new Message(-31);
        m.writer().writeByte(type);
        m.writer().writeUTF(text);
        m.writer().writeByte(color);
        m.writer().writeShort(-1);
        for (Map[] mapall : Map.ENTRYS) {
            if (mapall == null)
                continue;
            for (Map map : mapall) {
                if (map == null || map.players == null)
                    continue;
                synchronized (map.players) {
                    for (int i = 0; i < map.players.size(); i++) {
                        Player p0 = map.players.get(i);
                        if (p0 != null && p0.conn != null && !p0.isHideKTG) {
                            p0.addmsg(m);
                        }
                    }
                }
            }
        }
        List<Map> mapplus = Map.get_map_plus();
        for (int i = 0; i < mapplus.size(); i++) {
            Map map = mapplus.get(i);
            if (map == null || map.players == null)
                continue;
            synchronized (map.players) {
                for (int i12 = 0; i12 < map.players.size(); i12++) {
                    Player p0 = map.players.get(i12);
                    if (p0 != null && p0.conn != null && !p0.isHideKTG) {
                        p0.addmsg(m);
                    }
                }
            }
        }
        m.cleanup();
    }

    public Player getPlayer(int id) {
        return io.SessionManager.PLAYER_BY_ID.get(id);
    }

    public static void load_fusion_data() {
        try {
            File f = new File("data/fusion_data.json");
            if (!f.exists()) {
                GameLogger.info("[Fusion] data/fusion_data.json not found, skipping.");
                return;
            }
            byte[] data = Util.loadfile("data/fusion_data.json");
            String jsonStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            JSONObject root = (JSONObject) JSONValue.parse(jsonStr);
            if (root != null && root.containsKey("templates")) {
                Player.FUSION_TEMPLATES.clear();
                JSONArray templates = (JSONArray) root.get("templates");
                for (Object obj : templates) {
                    JSONObject tmpl = (JSONObject) obj;
                    int id = Integer.parseInt(tmpl.get("id").toString());
                    short head = Short.parseShort(tmpl.get("head").toString());
                    short hair = Short.parseShort(tmpl.get("hair").toString());
                    JSONArray fashionArr = (JSONArray) tmpl.get("fashion");
                    short[] fashion = null;
                    if (fashionArr != null) {
                        fashion = new short[fashionArr.size()];
                        for (int i = 0; i < fashionArr.size(); i++) {
                            fashion[i] = Short.parseShort(fashionArr.get(i).toString());
                        }
                    }
                    Player.FUSION_TEMPLATES.put(id, new Player.FusionTemplate(id, head, hair, fashion));
                    GameLogger.info("[Fusion] Loaded template id=" + id + " head=" + head + " hair=" + hair);
                }
                GameLogger.info("[Fusion] Total templates loaded: " + Player.FUSION_TEMPLATES.size());
            }
        } catch (Exception e) {
            GameLogger.error("[Fusion] Load fusion_data.json error!", e);
        }
    }

    public String getConfig(String key) {
        return this.configMap.get(key);
    }

    public int getConfigInt(String key, int defaultVal) {
        String val = this.configMap.get(key);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (Exception e) {
            }
        }
        return defaultVal;
    }

    public float getConfigFloat(String key, float defaultVal) {
        String val = this.configMap.get(key);
        if (val != null) {
            try {
                return Float.parseFloat(val);
            } catch (Exception e) {
            }
        }
        return defaultVal;
    }

    private static class PlayerRecoveryData {
        String name;
        int accountId;
        String user;
        String pass;
        int isAdmin;
        int isLock;
        long tongnap;

        PlayerRecoveryData(String name, int accountId, String user, String pass, int isAdmin, int isLock,
                long tongnap) {
            this.name = name;
            this.accountId = accountId;
            this.user = user;
            this.pass = pass;
            this.isAdmin = isAdmin;
            this.isLock = isLock;
            this.tongnap = tongnap;
        }
    }

    private void restoreOfflinePlayers() {
        new Thread(() -> {
            GameLogger.info("[Offline] Background recovery started...");
            Connection conn = null;
            java.sql.PreparedStatement ps = null;
            ResultSet rs = null;
            List<PlayerRecoveryData> recoveryList = new ArrayList<>();

            try {
                conn = SQL.gI().getCon();
                ps = conn.prepareStatement(
                        "SELECT p.name, a.id, a.user, a.pass, a.admin, a.lock, a.tongnap " +
                                "FROM `players` p " +
                                "JOIN `accounts` a ON a.char LIKE CONCAT('%\"', p.name, '\"%') " +
                                "WHERE p.is_offline_training = 1 AND p.offline_training_end > ? LIMIT 500");
                ps.setLong(1, System.currentTimeMillis());

                rs = ps.executeQuery();
                while (rs.next()) {
                    recoveryList.add(new PlayerRecoveryData(
                            rs.getString("name"),
                            rs.getInt("id"),
                            rs.getString("user"),
                            rs.getString("pass"),
                            rs.getInt("admin"),
                            rs.getInt("lock"),
                            rs.getLong("tongnap")));
                }
            } catch (Exception e) {
                GameLogger.error("[Offline] Error querying names", e);
            } finally {
                try {
                    if (rs != null)
                        rs.close();
                    if (ps != null)
                        ps.close();
                    if (conn != null)
                        conn.close();
                } catch (SQLException e) {
                }
            }

            if (recoveryList.isEmpty()) {
                return;
            }

            GameLogger.info("[Offline] Restoring " + recoveryList.size() + " characters in background...");
            int count = 0;
            for (PlayerRecoveryData data : recoveryList) {
                String name = data.name;
                try {
                    if (Map.get_player_by_name_allmap(name) != null)
                        continue;

                    io.Session dummy = new io.Session(true);
                    dummy.version = "1.2.9";
                    dummy.id = data.accountId;
                    dummy.user = data.user;
                    dummy.pass = data.pass;
                    dummy.lock = (byte) data.isLock;
                    dummy.tong_nap = data.tongnap;

                    Player p = new Player(dummy, name);
                    if (p.setup()) {
                        if (!p.isOfflineTraining || p.offlineTrainingEndTime <= System.currentTimeMillis()) {
                            continue;
                        }
                        p.setin4();
                        io.SessionManager.registerPlayer(p);
                        p.type_pk = -1;
                        p.typePirate = -1;

                        if (p.map != null) {
                            p.map.goto_map(p);
                            count++;
                            if (count % 20 == 0) {
                                GameLogger.info(
                                        "[Offline] Restored " + count + "/" + recoveryList.size() + " characters...");
                            }
                        }
                    }
                } catch (Exception e) {
                    GameLogger.error("[Offline] Failed to restore character: " + name, e);
                } finally {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                    }
                }
            }
            GameLogger.info("[Offline] Successfully restored " + count + " characters to maps.");
        }, "OfflineRecovery").start();
    }
}
