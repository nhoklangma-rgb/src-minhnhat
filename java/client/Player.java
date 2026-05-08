package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import activities.*;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import core.Manager;
import core.PendingRewardManager;
import core.Service;
import core.Util;
import core.GameLogger;
import database.SQL;
import io.Message;
import io.Session;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import map.Map;
import map.Mob;
import map.Vgo;
import map.Npc;
import map.Vgo;
import map.f;
import org.joda.time.DateTimeConstants;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import template.*;

public class Player {

    public playerClone pcl;
    private volatile boolean dirty = false;
    private final Object saveLock = new Object();

    public static Object parseJSON(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return JSONValue.parse(s);
    }

    public static JSONArray parseJSONArray(String s) {
        Object obj = parseJSON(s);
        if (obj instanceof JSONArray) {
            return (JSONArray) obj;
        }
        return new JSONArray();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isDisciple() {
        return false;
    }

    public Object getSaveLock() {
        return saveLock;
    }

    public boolean typePKClone;

    public long timeSetPK, timeSua;
    public long lastEffUpdateTime = 0;
    public long lastSkinUpdateTime = 0;
    public long lastTitleUpdateTime = 0;
    public long time_broadcast_hp = 0;
    public long time_regen = 0;
    public long hp_max_cache = 0, mp_max_cache = 0;
    public boolean hp_max_dirty = true, mp_max_dirty = true;

    public void invalidateStatCache() {
        this.hp_max_dirty = true;
        this.mp_max_dirty = true;
        if (this.body != null) {
            this.body.setDirty();
        }
    }

    public WorldWarInfo worldWarInfo;
    public Session conn;

    public void addmsg(Message m) {
        try {
            if (this.conn != null) {
                this.conn.addmsg(m);
            }
        } catch (Exception e) {
            // [STABILITY] Bỏ qua lỗi gửi tin nhắn khi kết nối đã đóng
        }
    }

    public int getVersion() {
        if (conn == null || conn.version == null) {
            return 129; // Mặc định ver mới nhất nếu không có session (Offline/Bot)
        }
        try {
            return Integer.parseInt(conn.version.replace(".", ""));
        } catch (Exception e) {
            return 129;
        }
    }

    public short index_map;
    public int id;
    public byte clazz;
    public String name;
    public short x;
    public short y;
    public long hp;
    public long mp;
    public short level;
    public long exp;
    public long beri0;
    public long ruby0;
    public long pica0;
    public int tu_tien;
    public int tu_ma;
    public short point1;
    public short point2;
    public short point3;
    public short point4;
    public short point5;
    public short point6;
    public short point7;
    public short point8;
    public short point9;
    public short point10;
    public Map map;
    public int xold;
    public int yold;
    public boolean isdie;
    public byte last_index_join_item;
    public Set<String> id_meet_in_map = new HashSet<>();
    public int[] data_super_upgrade;
    public BlockingQueue<Integer> key_red_line;
    public int time_key_red_line;
    public long time_pick_item_other;
    public long cd_ticket_next;
    public long cd_keyboss_next;
    public long cd_pvp_next;
    public byte[] is_combo;
    public long time_combo;
    public String[] data_yesno_gem = new String[0];
    public int fee_trade;
    public byte danhLaChoang;
    public byte thanhLoc;
    public byte nenDau;
    public byte giaiPhongNangLuong;
    public Clan clan;
    public boolean isclose;
    public Dungeon dungeon;
    public Player pvp_target;
    public boolean pvp_accept;
    public int pvp_win;
    public int pvp_lose;
    public short id_it_select = -1;
    public int id_select_temp = -1;
    public short id_ship_packet = -1;
    public Ship_pet ship_pet;
    public byte time_ship;
    public byte time_can_hs;
    public short offlineX; // Tọa độ X khi treo máy
    public short offlineY; // Tọa độ Y khi treo máy
    public TableTickOption tableTickOption;
    public String[] name_ThoSanHaiTac;
    public Upgrade_Skin_Info upgrade_skin;
    public byte[] tool_dial;
    public List<ItemBag47> daHanhTrinh = new ArrayList<>();
    private long tichLuy;
    private short ticket;
    private long vang;
    private long kimcuong;
    public boolean ischangemap = true;
    public int thongthao;
    public int thongthao2;
    public int reincarnation;
    public int pointPk;
    public int pointAttribute;
    public int typePirate;
    public int indexGhostServer;
    public int pointSkill;
    public short head;
    public short hair;
    public int fusionType = 0; // 0 = Bình thường, >0 = ID của fusion template
    public long fusionExpiry = -1;
    public Item item;
    public Body body;
    public int preFusionStatus = -1;

    // ===== OFFLINE TRAINING SYSTEM =====
    public boolean isOfflineTraining = false;
    public long offlineTrainingEndTime = -1L;
    public int offlineTrainingType = 0; // 0: None, 1: Extol (0.3s), 2: VIP (0.1s)
    public Mob targetMobOffline = null;
    public long lastUpdateOfflineAI = 0L;
    public long lastAtkTimeOffline = 0L;
    public long lastDieTimeOffline = 0L;
    public static final long OFFLINE_TRAINING_DURATION = 30 * 60 * 1000L; // 30 phút
    public static final long OFFLINE_DEATH_RESPAWN_DELAY = 5 * 60 * 1000L; // 5 phút
    // ===================================

    // ===== FUSION TEMPLATE SYSTEM =====
    public static class FusionTemplate {
        public int id;
        public short head; // -1 = giữ nguyên ngoại hình gốc
        public short hair; // -1 = giữ nguyên
        public short[] fashion; // null = giữ nguyên

        public FusionTemplate(int id, short head, short hair, short[] fashion) {
            this.id = id;
            this.head = head;
            this.hair = hair;
            this.fashion = fashion;
        }
    }

    // Registry: id -> FusionTemplate, load từ data/fusion_data.json lúc khởi động
    public static java.util.Map<Integer, FusionTemplate> FUSION_TEMPLATES = new java.util.HashMap<>();
    public BlockingQueue<Message> list_msg_cache = new LinkedBlockingQueue<>();
    public byte type_pk;
    public ItemMap[] it_map = new ItemMap[0];
    private ConcurrentHashMap<Integer, EffTemplate> map_eff = new ConcurrentHashMap<>();
    public long time_chat_ktg;
    public int use_item_3;
    public int index_badge_gacha = -1;
    public List<Integer> locked_options = new ArrayList<>();
    public int[] tool_upgrade = new int[14];
    public byte[][] rms = new byte[15][0];
    public List<Skill_info> skill_point = new ArrayList<>();
    public List<Skill_info> list_can_combo = new ArrayList<>();
    public Item_wear item_chuyenhoa_save_0;
    public Item_wear item_chuyenhoa_save_1;
    public Item_wear item_to_kham_ngoc;
    public short item_to_kham_ngoc_id_ngoc;
    public Player trade_target;
    public List<Item_wear> list_item_trade3 = new ArrayList<>();
    public List<ItemBag47> list_item_trade47 = new ArrayList<>();
    public long money_trade;
    public boolean is_lock_trade;
    public boolean is_accept_trade;
    public boolean isShop;
    public boolean isBox;
    public List<FriendTemp> friend_list = new ArrayList<>();
    public List<FriendTemp> enemy_list = new ArrayList<>();

    public static class BadgeReduction {
        public int attackerId;
        public int type; // 1-5 (SM, PT, TL, TT, NN)
        public int value;
        public long expiry;

        public BadgeReduction(int attackerId, int type, int value, long duration) {
            this.attackerId = attackerId;
            this.type = type;
            this.value = value;
            this.expiry = System.currentTimeMillis() + duration;
        }
    }

    public List<BadgeReduction> list_badge_reduction = new CopyOnWriteArrayList<>();
    public long[] total_badge_reductions = new long[6];

    public void add_badge_reduction(Player attacker, int type, int value, long duration) {
        if (attacker == null || value <= 0 || type < 1 || type > 5)
            return;
        long now = System.currentTimeMillis();
        for (BadgeReduction br : list_badge_reduction) {
            if (br.attackerId == attacker.id && br.type == type) {
                if (value > br.value) {
                    total_badge_reductions[type] += (value - br.value);
                    br.value = value;
                }
                br.expiry = now + duration;
                return;
            }
        }
        list_badge_reduction.add(new BadgeReduction(attacker.id, type, value, duration));
        total_badge_reductions[type] += value;
    }

    public List<ItemFashionP2> fashion = new ArrayList<>();
    public List<ItemFashionP> itfashionP = new ArrayList<>();
    public long time_buff_hp_mp;
    public long lastTimeUseItem;
    public List<QuestP> list_quest = new ArrayList<>();
    public DateTime date;
    public Party party;
    public int[] data_yesno;
    public int[] map_tele;
    public int id_map_save = 1;
    public long lastMazeTeleport;
    public int currentMazeNode;
    public boolean wait_change_map;
    public List<ItemBoatP> itemboat = new ArrayList<>();
    public boolean is_show_hat;
    private long vnd;

    public activities.WishManager.WishStats wishStats;
    public activities.WishManager.HuntTarget huntTarget;
    private int bua;
    public byte percent_da_sieu_cap;
    private short pvp_ticket;
    private short key_boss;
    public MapBossInfo map_boss_info;
    public int pointAttributeThongThao;
    public List<Option> list_op_thongthao = new ArrayList<>();
    public boolean hasUnlockedMastery2 = false;
    public int pointAttributeThongThao2;
    public List<Option> list_op_thongthao2 = new ArrayList<>();
    public int tocSuper;
    private int pvppoint;
    public int time_nvl;
    public long time_hs_little_garden;
    public boolean isTachTB = false;
    public byte time_ttvt;
    public long time_skill_decrease;
    public long time_can_mob_atk;
    public boolean is_show_weapon;
    public Player targetFight;
    private long wanted_price;
    public Wanted_Chest[] wanted_chest = new Wanted_Chest[0];
    public List<MyPet> my_pet = new ArrayList<>();
    public long time_change_map;
    public ConcurrentHashMap<Integer, Long> time_use_skill = new ConcurrentHashMap<>();

    public List<GiftBox> giftTTVT = new ArrayList<>();
    public byte[] tichNapCheck = new byte[10];
    public byte[] tichTieuCheck = new byte[10];
    public long timeDataEff;
    public Player tempPlayer; // Biến tạm thời để lưu đối tượng Player
    public Player tempRubyP0; // Biến tạm thời để lưu đối tượng Player
    public MyArchiDaily[] archiDaily = new MyArchiDaily[0];
    public int typeBXH;
    public int soluongmua;
    // Thêm biến tempIndexMapping để lưu ánh xạ chỉ số
    public List<Integer> tempIndexMapping = new ArrayList<>();
    public java.util.List<?> tempFruitList; // Lưu danh sách trái ác quỷ cho menu xem chi tiết
    public java.util.List<Integer> tempIdList;
    public int pendingFruitItemId = -1; // Biến tạm ổn định cho TAQ
    // clone bot
    public boolean clone;
    public boolean typePVP;
    public boolean isPetVisible = true; // Mặc định pet hiển thị
    public boolean isHideKTG = false; // Mặc định không ẩn KTG
    public byte levelAwaken = 0; // 0: Normal, 1: Stage 1 (2 fruits), 2: Stage 2 (3 fruits)
    public short[] idDF_required = new short[] { -1, -1, -1, -1, -1, -1 };

    public Disciple myDisciple = null;

    public void generateDFRequirements() {
        List<Short> allFruits = new ArrayList<>(devilfruit.DevilFruitManager.FRUITS.keySet());
        if (allFruits.size() < 6)
            return;

        // Get already assigned fruits to avoid duplicates
        Set<Short> assigned = new HashSet<>();
        for (short id : idDF_required) {
            if (id != -1)
                assigned.add(id);
        }

        allFruits.removeAll(assigned);
        Collections.shuffle(allFruits);

        int fruitIdx = 0;
        for (int i = 0; i < 6; i++) {
            if (idDF_required[i] == -1 && fruitIdx < allFruits.size()) {
                idDF_required[i] = allFruits.get(fruitIdx++);
            }
        }
    }

    public List<MyDanhHieu> danh_hieu = new ArrayList<>();
    public int selectedMenuOption;
    public long lastSpinTime = 0; // Thời gian lần quay cuối cùng
    public int solannang = 10; // Số lần auto nâng
    public int timeDecDame;
    public String[] data_topPkOnline = new String[0];
    public int oldMapId = -1;
    public short oldX = -1;
    public short oldY = -1;
    public boolean isDying = false;
    public boolean is_show_gift = false;
    public short skinId = -1;
    public long lastAttackTime = 0;
    public boolean isRight = true;
    public long lastMoveTime = 0;

    public Player(Session conn, String name) {
        this.conn = conn;
        this.name = name;
    }

    public boolean setup() throws ParseException {
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(
                    "SELECT * FROM `players` WHERE `name` = '" + this.name + "' LIMIT 1;");
            if (!rs.next()) {
                return false;
            }
            id = rs.getInt("id");
            index_map = (short) id;
            clazz = rs.getByte("clazz");
            pvppoint = rs.getInt("pvppoint");
            beri0 = rs.getLong("beri0");
            ruby0 = rs.getInt("ruby0");
            pica0 = rs.getLong("pica0");
            tocSuper = rs.getInt("tocSuper");
            //
            String tuHanhJson = rs.getString("tu_hanh");
            if (tuHanhJson != null && !tuHanhJson.isEmpty() && !tuHanhJson.equals("[]")) {
                JSONArray tuHanhArray = (JSONArray) new JSONParser().parse(tuHanhJson);
                if (tuHanhArray != null && tuHanhArray.size() >= 2) {
                    tu_tien = Integer.parseInt(tuHanhArray.get(0).toString());
                    tu_ma = Integer.parseInt(tuHanhArray.get(1).toString());
                }
            }
            // Fusion persistence
            try {
                fusionType = rs.getInt("fusionType");
                fusionExpiry = rs.getLong("fusionExpiry");

                // Tránh trường hợp fusion rác lưu quá hạn
                if (fusionExpiry != -1 && fusionExpiry < System.currentTimeMillis()) {
                    fusionType = 0;
                    fusionExpiry = -1;
                }
            } catch (Exception e) {
                fusionType = 0;
                fusionExpiry = -1;
            }
            //
            JSONArray js = (JSONArray) JSONValue.parse(rs.getString("level"));
            if (js != null && js.size() >= 3) {
                level = Short.parseShort(js.get(0).toString());
                exp = Long.parseLong(js.get(1).toString());
                thongthao = Integer.parseInt(js.get(2).toString());
                if (js.size() > 3) {
                    reincarnation = Integer.parseInt(js.get(3).toString());
                } else {
                    reincarnation = 0;
                }
            } else {
                level = 1;
                exp = 0;
                thongthao = 0;
                reincarnation = 0;
            }
            js.clear();
            date = DateTime.parse(rs.getString("date"));
            js = (JSONArray) JSONValue.parse(rs.getString("body"));
            if (js != null && js.size() >= 2) {
                head = Short.parseShort(js.get(0).toString());
                hair = Short.parseShort(js.get(1).toString());
            } else {
                head = 0;
                hair = 0;
            }
            js.clear();
            //
            try {
                js = parseJSONArray(rs.getString("myarchidaily"));
                this.archiDaily = new MyArchiDaily[js.size()];
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    MyArchiDaily myArchiDaily = new MyArchiDaily();
                    myArchiDaily.id = Integer.parseInt(js_in.get(0).toString());
                    myArchiDaily.num = Long.parseLong(js_in.get(1).toString());
                    myArchiDaily.type = Byte.parseByte(js_in.get(2).toString());
                    this.archiDaily[i] = myArchiDaily;
                }
            } catch (Exception e) {
                core.GameLogger.error("Player.load: Error loading archiDaily", e);
                ArchiDaily.getRd(this);
            } finally {
                if (js != null) {
                    js.clear();
                }
            }
            //
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("potential"));
            if (js != null && js.size() >= 6) {
                pointAttribute = Integer.parseInt(js.get(0).toString());
                point1 = Short.parseShort(js.get(1).toString());
                point2 = Short.parseShort(js.get(2).toString());
                point3 = Short.parseShort(js.get(3).toString());
                point4 = Short.parseShort(js.get(4).toString());
                point5 = Short.parseShort(js.get(5).toString());
            } else {
                pointAttribute = 0;
                point1 = 1;
                point2 = 1;
                point3 = 1;
                point4 = 1;
                point5 = 1;
            }
            pointAttributeThongThao = Integer.parseInt(js.get(6).toString());
            list_op_thongthao = new ArrayList<>();
            JSONArray js_in = (JSONArray) js.get(7);
            for (int i = 0; i < js_in.size(); i++) {
                JSONArray js_in_1 = (JSONArray) js_in.get(i);
                list_op_thongthao.add(new Option(Byte.parseByte(js_in_1.get(0).toString()),
                        Integer.parseInt(js_in_1.get(1).toString())));
            }
            if (js.size() > 8) {
                levelAwaken = Byte.parseByte(js.get(8).toString());
            } else {
                levelAwaken = 0;
            }
            if (js.size() > 9) {
                JSONArray js_df = (JSONArray) js.get(9);
                for (int i = 0; i < 6 && i < js_df.size(); i++) {
                    idDF_required[i] = Short.parseShort(js_df.get(i).toString());
                }
            }
            if (idDF_required[2] == -1) { // Đảm bảo Sư phụ luôn có tối thiểu 3 trái
                generateDFRequirements();
            }
            if (js.size() > 10) {
                hasUnlockedMastery2 = Boolean.parseBoolean(js.get(10).toString());
            }
            if (js.size() > 11) {
                thongthao2 = Integer.parseInt(js.get(11).toString());
            }
            if (js.size() > 12) {
                pointAttributeThongThao2 = Integer.parseInt(js.get(12).toString());
            }
            list_op_thongthao2 = new ArrayList<>();
            if (js.size() > 13) {
                JSONArray js_in_2 = (JSONArray) js.get(13);
                for (int i = 0; i < js_in_2.size(); i++) {
                    JSONArray js_in_1 = (JSONArray) js_in_2.get(i);
                    list_op_thongthao2.add(new Option(Byte.parseByte(js_in_1.get(0).toString()),
                            Integer.parseInt(js_in_1.get(1).toString())));
                }
            }
            if (js.size() > 14) {
                point6 = Short.parseShort(js.get(14).toString());
            }
            if (js.size() > 15) {
                point7 = Short.parseShort(js.get(15).toString());
            }
            if (js.size() > 16) {
                point8 = Short.parseShort(js.get(16).toString());
            }
            if (js.size() > 17) {
                point9 = Short.parseShort(js.get(17).toString());
            }
            if (js.size() > 18) {
                point10 = Short.parseShort(js.get(18).toString());
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("point_inven"));
            vang = Long.parseLong(js.get(0).toString());
            kimcuong = Long.parseLong(js.get(1).toString());
            vnd = Long.parseLong(js.get(2).toString());
            bua = Integer.parseInt(js.get(3).toString());
            tichLuy = Long.parseLong(js.get(4).toString());
            pvp_win = Integer.parseInt(js.get(5).toString());
            pvp_lose = Integer.parseInt(js.get(6).toString());
            time_ship = Byte.parseByte(js.get(7).toString());
            time_can_hs = Byte.parseByte(js.get(8).toString());
            time_nvl = Integer.parseInt(js.get(9).toString());
            time_ttvt = Byte.parseByte(js.get(10).toString());
            wanted_price = Long.parseLong(js.get(11).toString());
            js.clear();
            this.wanted_chest = new Wanted_Chest[2];
            js = (JSONArray) JSONValue.parse(rs.getString("wanted_chest"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_in2 = (JSONArray) js.get(i);
                this.wanted_chest[i] = new Wanted_Chest();
                this.wanted_chest[i].id = Short.parseShort(js_in2.get(0).toString());
                this.wanted_chest[i].timeUse = Long.parseLong(js_in2.get(1).toString());
                this.wanted_chest[i].maxTimeUse = Short.parseShort(js_in2.get(2).toString());
                this.wanted_chest[i].Ruby = Short.parseShort(js_in2.get(3).toString());
            }
            js.clear();

            try {
                String tichluyData = rs.getString("tichluycheck");
                JSONObject jsObject = (JSONObject) parseJSON(tichluyData);
                if (jsObject == null) {
                    jsObject = new JSONObject();
                    jsObject.put("nap", new JSONArray());
                    jsObject.put("tieu", new JSONArray());
                }

                // Xử lý trạng thái "nap"
                JSONArray napArray = (JSONArray) jsObject.get("nap");
                int napSize = ListTichNap.ENTRY.size(); // Số mốc hiện tại
                tichNapCheck = new byte[napSize];
                for (int i = 0; i < napSize; i++) {
                    if (i < napArray.size()) {
                        tichNapCheck[i] = Byte.parseByte(napArray.get(i).toString());
                    } else {
                        tichNapCheck[i] = 0; // Giá trị mặc định cho mốc mới
                    }
                }

                // Xử lý trạng thái "tieu"
                JSONArray tieuArray = (JSONArray) jsObject.get("tieu");
                int tieuSize = ListTichTieu.ENTRY.size(); // Số mốc hiện tại
                tichTieuCheck = new byte[tieuSize];
                for (int i = 0; i < tieuSize; i++) {
                    if (i < tieuArray.size()) {
                        tichTieuCheck[i] = Byte.parseByte(tieuArray.get(i).toString());
                    } else {
                        tichTieuCheck[i] = 0; // Giá trị mặc định cho mốc mới
                    }
                }
            } catch (Exception ex) {
                core.GameLogger.error("Player.load: Error loading tichluycheck", ex);
                // Khởi tạo mặc định nếu không có dữ liệu
                tichNapCheck = new byte[ListTichNap.ENTRY.size()];
                tichTieuCheck = new byte[ListTichTieu.ENTRY.size()];
            } finally {
                js.clear();
            }

            js.clear();
            js = parseJSONArray(rs.getString("mypet"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_in2 = (JSONArray) js.get(i);
                MyPet tempPet = new MyPet();
                tempPet.id = Short.parseShort(js_in2.get(0).toString());
                tempPet.template = Pet.getTemplate(Short.parseShort(js_in2.get(1).toString()));
                tempPet.isUse = Byte.parseByte(js_in2.get(2).toString()) == 1;
                if (tempPet.template != null) {
                    my_pet.add(tempPet);
                }
            }
            js.clear();
            // Xử lý danh_hieu
            danh_hieu = new ArrayList<>();
            js = parseJSONArray(rs.getString("danhhieu"));
            if (js != null) {
                for (Object obj : js) {
                    JSONArray js_in2 = (JSONArray) obj;
                    MyDanhHieu tempDanhHieu = new MyDanhHieu();
                    tempDanhHieu.id = Integer.parseInt(js_in2.get(0).toString()); // ID của danh hiệu
                    tempDanhHieu.isUsed = Byte.parseByte(js_in2.get(1).toString()) == 1; // Trạng thái sử dụng

                    // Kiểm tra nếu template hợp lệ
                    if (DanhHieu.getTemplate(tempDanhHieu.id) != null) {
                        danh_hieu.add(tempDanhHieu);
                    }
                }
            }

            js.clear();
            //
            js = parseJSONArray(rs.getString("site"));
            if (js == null || js.size() < 6) {
                Map[] maplang = Map.get_map_by_id(1);
                this.map = (maplang != null && maplang.length > 0) ? maplang[0] : null;
                this.hp = 100;
                this.mp = 100;
                this.x = (short) (this.map != null ? this.map.template.maxW / 2 : 300);
                this.y = (short) (this.map != null ? this.map.template.maxH / 2 : 300);
                this.is_show_hat = true;
                this.pointPk = 0;
            } else {
                Map[] mapArray = Map.get_map_by_id(Integer.parseInt(js.get(0).toString()));
                if (mapArray == null) {
                    mapArray = Map.get_map_by_id(1);
                }
                byte zone_id = Byte.parseByte(js.get(1).toString());
                int zone_goto = (mapArray != null && zone_id < mapArray.length) ? zone_id : 0;
                if (mapArray != null && zone_goto < mapArray.length && zone_id != 0) {
                    while (zone_goto < (mapArray[zone_goto].template.max_zone - 1)
                            && mapArray[zone_goto].players.size() >= mapArray[zone_goto].template.max_player) {
                        zone_goto++;
                    }
                    if (mapArray[zone_goto].players.size() >= mapArray[zone_goto].template.max_player) {
                        mapArray = Map.get_map_by_id(1);
                        zone_goto = 0;
                    }
                }
                if (mapArray != null && zone_goto < mapArray.length) {
                    this.map = mapArray[zone_goto];
                } else {
                    Map[] maplang = Map.get_map_by_id(1);
                    this.map = (maplang != null && maplang.length > 0) ? maplang[0] : null;
                }
            }
            this.hp = Long.parseLong(js.get(2).toString());
            this.mp = Long.parseLong(js.get(3).toString());

            x = Short.parseShort(js.get(4).toString());
            y = Short.parseShort(js.get(5).toString());
            if (this.x < 0 || this.x > this.map.template.maxW || this.y < 0
                    || this.y > this.map.template.maxH) {
                x = (short) (this.map.template.maxW / 2);
                y = (short) (this.map.template.maxH / 2);
            }
            is_show_hat = Byte.parseByte(js.get(6).toString()) == 1;
            pointPk = Integer.parseInt(js.get(7).toString());
            ticket = Short.parseShort(js.get(8).toString());
            cd_ticket_next = Long.parseLong(js.get(9).toString());
            if (cd_ticket_next == 0 || ticket >= get_ticket_max()) {
                cd_ticket_next = System.currentTimeMillis() + (60_000L * 10); // 10p
            }
            while (ticket < get_ticket_max() && cd_ticket_next < System.currentTimeMillis()) {
                ticket++;
                cd_ticket_next += (60_000L * 10); // 10p
            }
            pvp_ticket = Short.parseShort(js.get(10).toString());
            cd_pvp_next = Long.parseLong(js.get(11).toString());
            if (cd_pvp_next == 0 || pvp_ticket >= get_pvp_ticket_max()) {
                cd_pvp_next = System.currentTimeMillis() + (60_000L * 60 * 2); // 2h
            }
            while (pvp_ticket < get_pvp_ticket_max() && cd_pvp_next < System.currentTimeMillis()) {
                pvp_ticket++;
                cd_pvp_next += (60_000L * 60 * 2); // 2h
            }
            key_boss = Short.parseShort(js.get(12).toString());
            cd_keyboss_next = Long.parseLong(js.get(13).toString());
            if (cd_keyboss_next == 0 || key_boss >= get_key_boss_max()) {
                cd_keyboss_next = System.currentTimeMillis() + (60_000L * 60 * 1); // 1h
            }
            while (key_boss < get_key_boss_max() && cd_keyboss_next < System.currentTimeMillis()) {
                key_boss++;
                cd_keyboss_next += (60_000L * 60 * 1); // 1h
            }
            if (js.size() > 14) {
                is_show_weapon = Byte.parseByte(js.get(14).toString()) == 1;
            } else {
                is_show_weapon = true;
            }
            //
            js.clear();
            list_quest = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("quest"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_q = (JSONArray) js.get(i);
                QuestP temp = new QuestP();
                int id_quest_get = Short.parseShort(js_q.get(0).toString());
                temp.template = Quest.get_quest(id_quest_get);
                JSONArray js_q2 = (JSONArray) js_q.get(1);
                temp.data = new short[js_q2.size()][];
                for (int j = 0; j < temp.data.length; j++) {
                    JSONArray js_q3 = (JSONArray) js_q2.get(j);
                    temp.data[j] = new short[js_q3.size()];
                    for (int k = 0; k < temp.data[j].length; k++) {
                        temp.data[j][k] = Short.parseShort(js_q3.get(k).toString());
                    }
                }
                list_quest.add(temp);
            }
            js.clear();
            //
            item = new Item(this);
            item.bag3 = new Item_wear[item.max_bag];
            item.box3 = new Item_wear[item.max_box];
            item.it_body = new Item_wear[8];
            item.second_body = new Item_wear[8];
            item.third_body = new Item_wear[8];
            item.bag47 = new ArrayList<>();
            item.box47 = new ArrayList<>();
            item.save_item_wear = new ArrayList<>();
            item.save_item_47 = new ArrayList<>();
            daHanhTrinh = new ArrayList<>();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("bag3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.bag3.length) {
                    item.bag3[temp.index] = temp;
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("save_it3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.template != null) {
                    item.save_item_wear.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("box3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.box3.length) {
                    item.box3[temp.index] = temp;
                }
            }
            js.clear();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("it_body"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.it_body.length) {
                    item.it_body[temp.index] = temp;
                }
            }
            js.clear();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("second_body"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.second_body.length) {
                    item.second_body[temp.index] = temp;

                }
            }
            if (js != null) {
                js.clear();
            }
            //
            js = (JSONArray) JSONValue.parse(rs.getString("third_body"));
            if (js != null) {
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                    Item_wear temp = new Item_wear();
                    Item.readUpdateItem(js2.toString(), temp);
                    if (temp.index < item.third_body.length) {
                        item.third_body[temp.index] = temp;
                    }
                }
                js.clear();
            }
            js = (JSONArray) JSONValue.parse(rs.getString("bag47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0) {
                    item.bag47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("box47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0) {
                    item.box47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("save_it47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0
                        && ((temp.category == 4 && temp.id > 28) || (temp.category == 7))) {
                    item.save_item_47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("hanhtrinh"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                daHanhTrinh.add(temp);
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("rms"));
            rms = new byte[11][];
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                rms[i] = new byte[js2.size()];
                for (int j = 0; j < rms[i].length; j++) {
                    rms[i][j] = Byte.parseByte(js2.get(j).toString());
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("skill"));
            skill_point = new ArrayList<>();
            int ver_ = getVersion();
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Skill_info skill_add = new Skill_info();
                skill_add.versionClient = ver_;
                skill_add.exp = Long.parseLong(js2.get(1).toString());
                if (skill_add.exp < 0) {
                    skill_add.exp = 0;
                }
                skill_add.temp = Skill_Template.get_temp(Short.parseShort(js2.get(0).toString()),
                        skill_add.exp);
                skill_add.lvdevil = Byte.parseByte(js2.get(2).toString());
                skill_add.devilpercent = Byte.parseByte(js2.get(3).toString());

                // Load extended fields
                if (js2.size() > 4) {
                    skill_add.realLevel = Integer.parseInt(js2.get(4).toString());
                }
                if (js2.size() > 5) {
                    skill_add.fruitId = Integer.parseInt(js2.get(5).toString());
                }

                int currentSkillIdx = -1;
                if (skill_add.temp != null) {
                    currentSkillIdx = skill_add.temp.indexSkillInServer;
                    // Migration/Guess Logic for fruitId
                    if (skill_add.fruitId <= 0) {
                        skill_add.fruitId = Player.getFruitIdBySkillIdx(currentSkillIdx);
                    }
                }

                skill_point.add(skill_add);
            }

            // REFINED CLEANUP: Now that we have the full list, process it to remove literal
            // duplicates and surplus
            Set<Integer> uniqueSkillIds = new HashSet<>();
            Set<Integer> fruitTypesAllowed = new HashSet<>();
            List<Skill_info> final_skill_point = new ArrayList<>();

            for (Skill_info sk : skill_point) {
                int skillIdx = (sk.temp != null) ? sk.temp.indexSkillInServer : -1;

                if (skillIdx == -1) {
                    final_skill_point.add(sk);
                    continue;
                }

                // Remove exact duplicate skill entries (preventing literal duplicate skills)
                if (uniqueSkillIds.contains(skillIdx)) {
                    continue;
                }

                // Ensure fruitId is set correctly (Legacy/Audit Sync)
                if (sk.fruitId <= 0 && sk.temp != null) {
                    sk.fruitId = Player.getFruitIdBySkillIdx(skillIdx);
                }

                if (sk.fruitId > 0) {
                    // Fruit skill: strictly enforce the limit on login
                    if (fruitTypesAllowed.contains(sk.fruitId) || fruitTypesAllowed.size() < (levelAwaken + 1)) {
                        fruitTypesAllowed.add(sk.fruitId);
                        uniqueSkillIds.add(skillIdx);
                        final_skill_point.add(sk);
                    }
                    // Else: skip this skill (surplus fruit) - will be pruned when player is saved
                } else {
                    // Non-fruit skill: always keep
                    uniqueSkillIds.add(skillIdx);
                    final_skill_point.add(sk);
                }
            }
            this.skill_point = final_skill_point;
            js.clear();
            //
            friend_list = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("friend"));
            for (int i = 0; i < js.size(); i++) {
                FriendTemp temp = new FriendTemp((JSONArray) JSONValue.parse(js.get(i).toString()));
                friend_list.add(temp);
                temp.id = friend_list.indexOf(temp);
            }
            js.clear();
            enemy_list = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("enemy"));
            for (int i = 0; i < js.size(); i++) {
                FriendTemp temp = new FriendTemp((JSONArray) JSONValue.parse(js.get(i).toString()));
                enemy_list.add(temp);
                temp.id = enemy_list.indexOf(temp);
            }
            js.clear();
            //
            this.itfashionP = new ArrayList<>();
            this.fashion = new ArrayList<>();
            this.itemboat = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("fashion"));
            JSONArray js_temp_2 = (JSONArray) JSONValue.parse(js.get(0).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemFashionP tempf = new ItemFashionP();
                tempf.category = Byte.parseByte(js_temp.get(0).toString());
                tempf.id = Short.parseShort(js_temp.get(1).toString());
                tempf.icon = Short.parseShort(js_temp.get(2).toString());
                tempf.is_use = Byte.parseByte(js_temp.get(3).toString()) == 1;
                this.itfashionP.add(tempf);
            }
            js_temp_2.clear();
            js_temp_2 = (JSONArray) JSONValue.parse(js.get(1).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemFashionP2 tempf = new ItemFashionP2();
                tempf.id = Short.parseShort(js_temp.get(0).toString());
                tempf.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
                tempf.level = Byte.parseByte(js_temp.get(2).toString());
                tempf.expirationTime = js_temp.size() > 3 ? Long.parseLong(js_temp.get(3).toString()) : -1;
                JSONArray js_op = js_temp.size() > 4 ? (JSONArray) JSONValue.parse(js_temp.get(4).toString())
                        : new JSONArray();
                List<Option> ops = new ArrayList<>();
                for (int j = 0; j < js_op.size(); j++) {
                    JSONArray js_op_temp = (JSONArray) js_op.get(j);
                    Option op = new Option(
                            Byte.parseByte(js_op_temp.get(0).toString()),
                            Short.parseShort(js_op_temp.get(1).toString()));
                    ops.add(op);
                }
                tempf.op = ops;
                this.fashion.add(tempf);
            }
            js_temp_2 = (JSONArray) JSONValue.parse(js.get(2).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemBoatP tempboat = new ItemBoatP();
                tempboat.id = Byte.parseByte(js_temp.get(0).toString());
                tempboat.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
                this.itemboat.add(tempboat);
            }
            js_temp_2.clear();
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("eff"));
            map_eff = new ConcurrentHashMap<>();
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                int id_eff = Byte.parseByte(js2.get(0).toString());
                map_eff.put(id_eff, new EffTemplate(id_eff,
                        Integer.parseInt(js2.get(1).toString()),
                        (System.currentTimeMillis() + Long.parseLong(js2.get(2).toString()))));
            }
            js.clear();
            //
            body = new Body(this);

            // Đệ tử Load (Di chuyển xuống đây để Sư phụ có đầy đủ body/item trước khi load
            // đệ)
            try {
                // [OFFLINE TRAINING] - load state
                this.isOfflineTraining = rs.getInt("is_offline_training") == 1;
                this.offlineTrainingEndTime = rs.getLong("offline_training_end");
                this.offlineTrainingType = rs.getInt("offline_training_type");
                this.offlineX = rs.getShort("offline_x");
                this.offlineY = rs.getShort("offline_y");

                // [FIX] Kiểm tra và gỡ bỏ AI nếu hết hạn hoặc người chơi Login
                if (this.offlineTrainingEndTime > 0 && System.currentTimeMillis() > this.offlineTrainingEndTime) {
                    this.isOfflineTraining = false;
                    this.offlineTrainingEndTime = -1L;
                    this.offlineTrainingType = 0;
                } else if (this.conn != null && !this.conn.isDummy) {
                    // Nếu vẫn còn thời gian nhưng người chơi thật Login vào thì chỉ tắt AI
                    this.isOfflineTraining = false;
                }

                String discipleData = rs.getString("disciple_data");
                if (discipleData != null && !discipleData.equals("null") && !discipleData.isEmpty()) {
                    myDisciple = new Disciple(this);
                    myDisciple.loadData(discipleData);
                }
            } catch (Exception e) {
                core.GameLogger.error("Player.load: Error loading disciple_data", e);
            }

            ChiemDao.checkAndRewardLogin(this);
            PendingRewardManager.claimAll(this);
            activities.WishManager.gI().loadWishData(this);
        } catch (Exception e) {
            GameLogger.error("Player.setup: Failed to load data for " + this.name, e);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.setup: SQL error closing resources for " + this.name, e);
            }
        }
        return true;
    }

    public int randomID() {
        return Util.random(-10000, -1);
    }

    public boolean setupClone() throws ParseException {
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(
                    "SELECT * FROM `players` WHERE `name` = '" + this.name + "' LIMIT 1;");
            if (!rs.next()) {
                return false;
            }
            id = randomID();
            name = name + " ";
            index_map = (short) id;
            clazz = rs.getByte("clazz");
            pvppoint = rs.getInt("pvppoint");
            beri0 = rs.getLong("beri0");
            ruby0 = rs.getInt("ruby0");
            pica0 = rs.getLong("pica0");
            tocSuper = rs.getInt("tocSuper");
            //
            String tuHanhJson = rs.getString("tu_hanh");
            if (tuHanhJson != null) {
                JSONArray tuHanhArray = (JSONArray) new JSONParser().parse(tuHanhJson);
                tu_tien = Integer.parseInt(tuHanhArray.get(0).toString());
                tu_ma = Integer.parseInt(tuHanhArray.get(1).toString());
            }
            //
            JSONArray js = (JSONArray) JSONValue.parse(rs.getString("level"));
            level = Short.parseShort(js.get(0).toString());
            exp = Long.parseLong(js.get(1).toString());
            thongthao = Short.parseShort(js.get(2).toString());
            js.clear();
            date = DateTime.parse(rs.getString("date"));
            js = (JSONArray) JSONValue.parse(rs.getString("body"));
            head = Short.parseShort(js.get(0).toString());
            hair = Short.parseShort(js.get(1).toString());
            js.clear();
            //
            try {
                js = (JSONArray) JSONValue.parse(rs.getString("myarchidaily"));
                this.archiDaily = new MyArchiDaily[js.size()];
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js_in = (JSONArray) js.get(i);
                    MyArchiDaily myArchiDaily = new MyArchiDaily();
                    myArchiDaily.id = Integer.parseInt(js_in.get(0).toString());
                    myArchiDaily.num = Long.parseLong(js_in.get(1).toString());
                    myArchiDaily.type = Byte.parseByte(js_in.get(2).toString());
                    this.archiDaily[i] = myArchiDaily;
                }
            } catch (Exception e) {
                ArchiDaily.getRd(this);
                GameLogger.warn("Player.setupClone: ArchiDaily data invalid for " + this.name + ", generating new", e);
            } finally {
                if (js != null) {
                    js.clear();
                }
            }
            //
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("potential"));
            pointAttribute = Integer.parseInt(js.get(0).toString());
            point1 = Short.parseShort(js.get(1).toString());
            point2 = Short.parseShort(js.get(2).toString());
            point3 = Short.parseShort(js.get(3).toString());
            point4 = Short.parseShort(js.get(4).toString());
            point5 = Short.parseShort(js.get(5).toString());
            pointAttributeThongThao = Integer.parseInt(js.get(6).toString());
            list_op_thongthao = new ArrayList<>();
            JSONArray js_in = (JSONArray) js.get(7);
            for (int i = 0; i < js_in.size(); i++) {
                JSONArray js_in_1 = (JSONArray) js_in.get(i);
                list_op_thongthao.add(new Option(Byte.parseByte(js_in_1.get(0).toString()),
                        Integer.parseInt(js_in_1.get(1).toString())));
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("point_inven"));
            vang = Long.parseLong(js.get(0).toString());
            kimcuong = Long.parseLong(js.get(1).toString());
            vnd = Long.parseLong(js.get(2).toString());
            bua = Integer.parseInt(js.get(3).toString());
            tichLuy = Long.parseLong(js.get(4).toString());
            pvp_win = Integer.parseInt(js.get(5).toString());
            pvp_lose = Integer.parseInt(js.get(6).toString());
            time_ship = Byte.parseByte(js.get(7).toString());
            time_can_hs = Byte.parseByte(js.get(8).toString());
            time_nvl = Integer.parseInt(js.get(9).toString());
            time_ttvt = Byte.parseByte(js.get(10).toString());
            wanted_price = Long.parseLong(js.get(11).toString());
            js.clear();
            this.wanted_chest = new Wanted_Chest[2];
            js = (JSONArray) JSONValue.parse(rs.getString("wanted_chest"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_in2 = (JSONArray) js.get(i);
                this.wanted_chest[i] = new Wanted_Chest();
                this.wanted_chest[i].id = Short.parseShort(js_in2.get(0).toString());
                this.wanted_chest[i].timeUse = Long.parseLong(js_in2.get(1).toString());
                this.wanted_chest[i].maxTimeUse = Short.parseShort(js_in2.get(2).toString());
                this.wanted_chest[i].Ruby = Short.parseShort(js_in2.get(3).toString());
            }
            js.clear();

            try {
                String tichluyData = rs.getString("tichluycheck");
                JSONObject jsObject = (JSONObject) JSONValue.parse(tichluyData);

                // Xử lý trạng thái "nap"
                JSONArray napArray = (JSONArray) jsObject.get("nap");
                int napSize = ListTichNap.ENTRY.size(); // Số mốc hiện tại
                tichNapCheck = new byte[napSize];
                for (int i = 0; i < napSize; i++) {
                    if (i < napArray.size()) {
                        tichNapCheck[i] = Byte.parseByte(napArray.get(i).toString());
                    } else {
                        tichNapCheck[i] = 0; // Giá trị mặc định cho mốc mới
                    }
                }

                // Xử lý trạng thái "tieu"
                JSONArray tieuArray = (JSONArray) jsObject.get("tieu");
                int tieuSize = ListTichTieu.ENTRY.size(); // Số mốc hiện tại
                tichTieuCheck = new byte[tieuSize];
                for (int i = 0; i < tieuSize; i++) {
                    if (i < tieuArray.size()) {
                        tichTieuCheck[i] = Byte.parseByte(tieuArray.get(i).toString());
                    } else {
                        tichTieuCheck[i] = 0; // Giá trị mặc định cho mốc mới
                    }
                }
            } catch (Exception ex) {
                core.GameLogger.error("Player.load: Error loading tichluycheck", ex);
                // Khởi tạo mặc định nếu không có dữ liệu
                tichNapCheck = new byte[ListTichNap.ENTRY.size()];
                tichTieuCheck = new byte[ListTichTieu.ENTRY.size()];
            } finally {
                js.clear();
            }

            my_pet = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("mypet"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_in2 = (JSONArray) js.get(i);
                MyPet tempPet = new MyPet();
                tempPet.id = Short.parseShort(js_in2.get(0).toString());
                tempPet.template = Pet.getTemplate(Short.parseShort(js_in2.get(1).toString()));
                tempPet.isUse = Byte.parseByte(js_in2.get(2).toString()) == 1;
                if (tempPet.template != null) {
                    my_pet.add(tempPet);
                }
            }
            js.clear();
            // Xử lý danh_hieu
            danh_hieu = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("danhhieu"));
            if (js != null) {
                for (Object obj : js) {
                    JSONArray js_in2 = (JSONArray) obj;
                    MyDanhHieu tempDanhHieu = new MyDanhHieu();
                    tempDanhHieu.id = Integer.parseInt(js_in2.get(0).toString()); // ID của danh hiệu
                    tempDanhHieu.isUsed = Byte.parseByte(js_in2.get(1).toString()) == 1; // Trạng thái sử dụng

                    // Kiểm tra nếu template hợp lệ
                    if (DanhHieu.getTemplate(tempDanhHieu.id) != null) {
                        danh_hieu.add(tempDanhHieu);
                    }
                }
            }

            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("site"));
            //
            Map[] map = Map.get_map_by_id(Integer.parseInt(js.get(0).toString()));
            if (map == null)
                map = Map.get_map_by_id(1);
            byte zone_id = Byte.parseByte(js.get(1).toString());
            int zone_goto = zone_id < map.length ? zone_id : 0;
            if (zone_goto != 0) {
                while (zone_goto < (map[zone_goto].template.max_zone - 1)
                        && map[zone_goto].players.size() >= map[zone_goto].template.max_player) {
                    zone_goto++;
                }
                if (map[zone_goto].players.size() >= map[zone_goto].template.max_player) {
                    map = Map.get_map_by_id(1);
                    zone_goto = 0;
                }
            }
            this.map = map[zone_goto];
            this.hp = Long.parseLong(js.get(2).toString());
            this.mp = Long.parseLong(js.get(3).toString());

            x = Short.parseShort(js.get(4).toString());
            y = Short.parseShort(js.get(5).toString());
            if (this.x < 0 || this.x > this.map.template.maxW || this.y < 0
                    || this.y > this.map.template.maxH) {
                x = (short) (this.map.template.maxW / 2);
                y = (short) (this.map.template.maxH / 2);
            }
            is_show_hat = Byte.parseByte(js.get(6).toString()) == 1;
            pointPk = Integer.parseInt(js.get(7).toString());
            ticket = Short.parseShort(js.get(8).toString());
            cd_ticket_next = Long.parseLong(js.get(9).toString());
            if (cd_ticket_next == 0 || ticket >= get_ticket_max()) {
                cd_ticket_next = System.currentTimeMillis() + (60_000L * 10); // 10p
            }
            while (ticket < get_ticket_max() && cd_ticket_next < System.currentTimeMillis()) {
                ticket++;
                cd_ticket_next += (60_000L * 10); // 10p
            }
            pvp_ticket = Short.parseShort(js.get(10).toString());
            cd_pvp_next = Long.parseLong(js.get(11).toString());
            if (cd_pvp_next == 0 || pvp_ticket >= get_pvp_ticket_max()) {
                cd_pvp_next = System.currentTimeMillis() + (60_000L * 60 * 2); // 2h
            }
            while (pvp_ticket < get_pvp_ticket_max() && cd_pvp_next < System.currentTimeMillis()) {
                pvp_ticket++;
                cd_pvp_next += (60_000L * 60 * 2); // 2h
            }
            key_boss = Short.parseShort(js.get(12).toString());
            cd_keyboss_next = Long.parseLong(js.get(13).toString());
            if (cd_keyboss_next == 0 || key_boss >= get_key_boss_max()) {
                cd_keyboss_next = System.currentTimeMillis() + (60_000L * 60 * 1); // 1h
            }
            while (key_boss < get_key_boss_max() && cd_keyboss_next < System.currentTimeMillis()) {
                key_boss++;
                cd_keyboss_next += (60_000L * 60 * 1); // 1h
            }
            if (js.size() > 14) {
                is_show_weapon = Byte.parseByte(js.get(14).toString()) == 1;
            } else {
                is_show_weapon = true;
            }
            //
            js.clear();
            list_quest = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("quest"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js_q = (JSONArray) js.get(i);
                QuestP temp = new QuestP();
                int id_quest_get = Short.parseShort(js_q.get(0).toString());
                temp.template = Quest.get_quest(id_quest_get);
                JSONArray js_q2 = (JSONArray) js_q.get(1);
                temp.data = new short[js_q2.size()][];
                for (int j = 0; j < temp.data.length; j++) {
                    JSONArray js_q3 = (JSONArray) js_q2.get(j);
                    temp.data[j] = new short[js_q3.size()];
                    for (int k = 0; k < temp.data[j].length; k++) {
                        temp.data[j][k] = Short.parseShort(js_q3.get(k).toString());
                    }
                }
                list_quest.add(temp);
            }
            js.clear();
            //
            item = new Item(this);
            item.bag3 = new Item_wear[item.max_bag];
            item.box3 = new Item_wear[item.max_box];
            item.it_body = new Item_wear[8];
            item.second_body = new Item_wear[8];
            item.third_body = new Item_wear[8];
            item.bag47 = new ArrayList<>();
            item.box47 = new ArrayList<>();
            item.save_item_wear = new ArrayList<>();
            item.save_item_47 = new ArrayList<>();
            daHanhTrinh = new ArrayList<>();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("bag3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.bag3.length) {
                    item.bag3[temp.index] = temp;
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("save_it3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.template != null) {
                    item.save_item_wear.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("box3"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.box3.length) {
                    item.box3[temp.index] = temp;
                }
            }
            js.clear();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("it_body"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.it_body.length) {
                    item.it_body[temp.index] = temp;
                }
            }
            js.clear();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("second_body"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Item_wear temp = new Item_wear();
                Item.readUpdateItem(js2.toString(), temp);
                if (temp.index < item.second_body.length) {
                    item.second_body[temp.index] = temp;

                }
            }

            js.clear();
            //
            js = (JSONArray) JSONValue.parse(rs.getString("third_body"));
            if (js != null) {
                for (int i = 0; i < js.size(); i++) {
                    JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                    Item_wear temp = new Item_wear();
                    Item.readUpdateItem(js2.toString(), temp);
                    if (temp.index < item.third_body.length) {
                        item.third_body[temp.index] = temp;
                    }
                }
                js.clear();
            }
            js = (JSONArray) JSONValue.parse(rs.getString("bag47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0) {
                    item.bag47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("box47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0) {
                    item.box47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("save_it47"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                if (temp.quant > 0
                        && ((temp.category == 4 && temp.id > 28) || (temp.category == 7))) {
                    item.save_item_47.add(temp);
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("hanhtrinh"));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                ItemBag47 temp = new ItemBag47();
                temp.category = Byte.parseByte(js2.get(0).toString());
                temp.id = Short.parseShort(js2.get(1).toString());
                temp.quant = Short.parseShort(js2.get(2).toString());
                daHanhTrinh.add(temp);
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("rms"));
            rms = new byte[11][];
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                rms[i] = new byte[js2.size()];
                for (int j = 0; j < rms[i].length; j++) {
                    rms[i][j] = Byte.parseByte(js2.get(j).toString());
                }
            }
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("skill"));
            skill_point = new ArrayList<>();
            // int ver_ = Integer.parseInt(this.conn.version.replace(".", ""));
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                Skill_info skill_add = new Skill_info();
                // skill_add.versionClient = ver_;
                skill_add.exp = Long.parseLong(js2.get(1).toString());
                if (skill_add.exp < 0) { // ✅ tự động reset exp âm khi load
                    skill_add.exp = 0;
                }
                skill_add.temp = Skill_Template.get_temp(Short.parseShort(js2.get(0).toString()),
                        skill_add.exp);
                skill_add.lvdevil = Byte.parseByte(js2.get(2).toString());
                skill_add.devilpercent = Byte.parseByte(js2.get(3).toString());

                if (skill_add.temp != null) {
                    short id = (short) skill_add.temp.ID;
                    short lv = skill_add.temp.Lv_RQ;
                    boolean needFix = false;
                    short newLv = lv;

                    if (id <= 2) {
                        if (lv > 1000)
                            newLv = 1000;
                        else if (lv <= 0)
                            newLv = 1;
                    } else if (id >= 2003 && id <= 2063) {
                        if (lv > 100)
                            newLv = 100;
                        else if (lv < 0)
                            newLv = 0;
                    }

                    if (newLv != lv) {
                        System.out.println(
                                "[FIX] Account: " + this.name + " skill_id=" + id + " level " + lv + " -> " + newLv);
                        for (Skill_Template t : Skill_Template.ENTRYS) {
                            if (t.ID == id && t.name.equals(skill_add.temp.name) && t.Lv_RQ == newLv) {
                                skill_add.temp = t;
                                break;
                            }
                        }
                    }
                }

                skill_point.add(skill_add);
            }
            js.clear();
            //
            friend_list = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("friend"));
            for (int i = 0; i < js.size(); i++) {
                FriendTemp temp = new FriendTemp((JSONArray) JSONValue.parse(js.get(i).toString()));
                friend_list.add(temp);
                temp.id = friend_list.indexOf(temp);
            }
            js.clear();
            enemy_list = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("enemy"));
            for (int i = 0; i < js.size(); i++) {
                FriendTemp temp = new FriendTemp((JSONArray) JSONValue.parse(js.get(i).toString()));
                enemy_list.add(temp);
                temp.id = enemy_list.indexOf(temp);
            }
            js.clear();
            //
            this.itfashionP = new ArrayList<>();
            this.fashion = new ArrayList<>();
            this.itemboat = new ArrayList<>();
            js = (JSONArray) JSONValue.parse(rs.getString("fashion"));
            JSONArray js_temp_2 = (JSONArray) JSONValue.parse(js.get(0).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemFashionP tempf = new ItemFashionP();
                tempf.category = Byte.parseByte(js_temp.get(0).toString());
                tempf.id = Short.parseShort(js_temp.get(1).toString());
                tempf.icon = Short.parseShort(js_temp.get(2).toString());
                tempf.is_use = Byte.parseByte(js_temp.get(3).toString()) == 1;
                this.itfashionP.add(tempf);
            }
            js_temp_2.clear();
            js_temp_2 = (JSONArray) JSONValue.parse(js.get(1).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemFashionP2 tempf = new ItemFashionP2();
                tempf.id = Short.parseShort(js_temp.get(0).toString());
                tempf.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
                tempf.level = Byte.parseByte(js_temp.get(2).toString());
                tempf.expirationTime = js_temp.size() > 3 ? Long.parseLong(js_temp.get(3).toString()) : -1;
                JSONArray js_op = js_temp.size() > 4 ? (JSONArray) JSONValue.parse(js_temp.get(4).toString())
                        : new JSONArray();
                List<Option> ops = new ArrayList<>();
                for (int j = 0; j < js_op.size(); j++) {
                    JSONArray js_op_temp = (JSONArray) js_op.get(j);
                    Option op = new Option(
                            Byte.parseByte(js_op_temp.get(0).toString()),
                            Short.parseShort(js_op_temp.get(1).toString()));
                    ops.add(op);
                }
                tempf.op = ops;
                this.fashion.add(tempf);
            }
            js_temp_2 = (JSONArray) JSONValue.parse(js.get(2).toString());
            for (int i = 0; i < js_temp_2.size(); i++) {
                JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i).toString());
                ItemBoatP tempboat = new ItemBoatP();
                tempboat.id = Byte.parseByte(js_temp.get(0).toString());
                tempboat.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
                this.itemboat.add(tempboat);
            }
            js_temp_2.clear();
            js.clear();
            js = (JSONArray) JSONValue.parse(rs.getString("eff"));
            map_eff = new ConcurrentHashMap<>();
            for (int i = 0; i < js.size(); i++) {
                JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
                int id_eff = Byte.parseByte(js2.get(0).toString());
                map_eff.put(id_eff, new EffTemplate(id_eff,
                        Integer.parseInt(js2.get(1).toString()),
                        (System.currentTimeMillis() + Long.parseLong(js2.get(2).toString()))));
            }
            js.clear();
            //
            body = new Body(this);

            // Đệ tử Load (Di chuyển xuống đây để Sư phụ có đầy đủ body/item trước khi load
            // đệ)
            try {
                String discipleData = rs.getString("disciple_data");
                if (discipleData != null && !discipleData.equals("null") && !discipleData.isEmpty()) {
                    myDisciple = new Disciple(this);
                    myDisciple.loadData(discipleData);
                }
            } catch (Exception e) {
                core.GameLogger.error("Player.setupClone: Error loading disciple data for " + this.name, e);
            }
        } catch (SQLException e) {
            GameLogger.error("Player.setupClone: Failed to load data (SQLException) for " + this.name, e);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.setupClone: SQL error closing resources for " + this.name, e);
            }
        }
        return true;
    }

    public void setin4() throws IOException {
        // if (this.thongthao > 200) {
        // this.thongthao = 200;
        // this.list_op_thongthao.clear();
        // }
        // // Tính lại tổng tiềm năng thông thạo từ tất cả nguồn
        // {
        // int totalPoints = this.thongthao; // 1 điểm / cấp thông thạo (từ EXP)
        // // Cộng điểm từ Tu Tiên
        // for (int i = 0; i < this.tu_tien; i++) {
        // totalPoints += ItemDropTool.getDynamicPointGain(i);
        // }
        // // Cộng điểm từ Tu Ma
        // for (int i = 0; i < this.tu_ma; i++) {
        // totalPoints += ItemDropTool.getDynamicPointGain(i);
        // }

        // // Trừ đi số điểm đã cộng vào thuộc tính
        // int spentPoints = 0;
        // if (this.list_op_thongthao != null) {
        // for (Option op : this.list_op_thongthao) {
        // spentPoints += op.getParam();
        // }
        // }

        // this.pointAttributeThongThao = totalPoints - spentPoints;
        // if (this.pointAttributeThongThao < 0) {
        // this.pointAttributeThongThao = 0;
        // }
        // }
        xold = x;
        yold = y;
        // rankWanted = -1;
        typePirate = -1;
        indexGhostServer = -1;
        pointSkill = 20;
        type_pk = (byte) -1;
        //
        long hp_max = this.body.get_hp_max(true);
        if (this.hp == -1) {
            this.hp = hp_max;
        }
        if (this.hp <= 0 && Map.isMapLang(this.map.template.id)) {
            Service.use_potion(this, 0, this.body.get_hp_max(true));
            Service.use_potion(this, 1, this.body.get_mp_max(true));
        }
        if (this.hp <= 0) {
            this.hp = 0;
            isdie = true;
            this.map.die_player(this, this);
        } else {
            if (this.hp > hp_max) {
                this.hp = hp_max;
            }
            isdie = false;
        }
        if (this.mp == -1) {
            this.mp = this.body.get_mp_max(true);
        }
        ischangemap = false;
        list_msg_cache = new LinkedBlockingQueue<>();
        it_map = new ItemMap[3];
        tool_upgrade = new int[] { -1, -1 };
        item_chuyenhoa_save_0 = null;
        item_chuyenhoa_save_1 = null;
        item_to_kham_ngoc = null;
        item_to_kham_ngoc_id_ngoc = -1;
        trade_target = null;
        list_item_trade3 = null;
        list_item_trade47 = null;
        money_trade = 0;
        fee_trade = 0;
        is_lock_trade = false;
        is_accept_trade = false;
        use_item_3 = -1;
        time_buff_hp_mp = System.currentTimeMillis() + 5000L;
        party = null;
        wait_change_map = true;
        id_meet_in_map = new HashSet<>();
        percent_da_sieu_cap = 50;
        key_red_line = new LinkedBlockingQueue<>();
        time_key_red_line = -1;
        time_pick_item_other = 0;
        is_combo = null;
        list_can_combo = new ArrayList<>();
        map_boss_info = null;
        danhLaChoang = 0;
        thanhLoc = 0;
        nenDau = 0;
        giaiPhongNangLuong = 0;
        clan = Clan.get_my_clan(this.name);
        ship_pet = Ship_pet.get_pet(this);
    }

    public int get_level_percent() {
        if (level == 100) {
            // int index = this.thongthao;
            // if (index < 0)
            // index = 0;
            // if (index >= Level.LEVEL_THONGTHAO.length)
            // index = Level.LEVEL_THONGTHAO.length - 1;
            // return (int) ((exp * 1000) / Level.LEVEL_THONGTHAO[index]);
            return (int) ((exp * 1000) / Level.LEVEL_THONGTHAO[this.thongthao]);
        } else {
            // int index = level - 1;
            // if (index < 0)
            // index = 0;
            // if (index >= Level.ENTRYS.length)
            // index = Level.ENTRYS.length - 1;
            // return (int) ((exp * 1000) / Level.ENTRYS[index].exp);
            return (int) ((exp * 1000) / Level.ENTRYS[level - 1].exp);
        }
    }

    public int getPotentialPerLevel() {
        int points = 5 + (this.reincarnation * 5);
        return Math.min(points, core.Manager.gI().potential_per_level);
    }

    /**
     * Tiềm năng thông thạo nhận được từ Tu Hành / Bội.
     * Base 2 + thêm 2 mỗi 10 cấp trùng sinh.
     */
    public int getMasteryPotentialGain() {
        return 10;
    }

    @SuppressWarnings("unchecked")
    public static int flush(Player p, boolean print) {
        if (p == null || p.isDisciple() || p.conn == null || p.body == null || p.item == null || p.map == null) {
            if (print) {
                System.err.println(
                        "[Flush] Null or incomplete initialization for player: " + (p != null ? p.name : "null"));
            }
            return -1;
        }
        synchronized (p) { // Chỉ khóa đúng nhân vật đang được lưu
            int result = 0;
            String query = "UPDATE `players` SET `level` = ?, `date` = ?, `site` = ?, `point_inven` = ?, "
                    + "`bag3` = ?, `it_body` = ?, `potential` = ?, `bag47` = ?, "
                    + "`rms` = ?, `skill` = ?, `friend` = ?, `enemy` = ?, `fashion` = ?, `eff` = ?, `box47` = ?, `box3` = ?, `quest` = ?, "
                    + "`exp` = ?, `pvppoint` = ?, `save_it3` = ?, `save_it47` = ?, "
                    + "`hanhtrinh` = ?, `wanted_point` = ?, `wanted_chest` = ?, `mypet` = ?, `tichluycheck` = ?, `beri0` = ?, `ruby0` = ? , `second_body` = ?, `pica0` = ?, `myarchidaily` = ?, `danhhieu` = ? , `tu_hanh` = ?, `tocSuper` = ?, `third_body` = ?, `disciple_data` = ?, `clazz` = ?, `fusionType` = ?, `fusionExpiry` = ?, `is_offline_training` = ?, `offline_training_end` = ?, `offline_training_type` = ?, `offline_x` = ?, `offline_y` = ? WHERE `id` = "
                    + p.id + ";";
            Connection connection = null;
            PreparedStatement ps = null;
            try {
                connection = SQL.gI().getCon();
                ps = connection.prepareStatement(query);
                JSONArray js = new JSONArray();
                js.add(p.level);
                js.add(p.exp);
                js.add(p.thongthao);
                js.add(p.reincarnation);
                ps.setNString(1, js.toJSONString());
                js.clear();
                ps.setNString(2, p.date.toString());
                js = new JSONArray();
                if (Map.map_cant_save_site(p.map.template.id)) {
                    //
                    int x_save = -1, y_save = -1;
                    Map[] map_get = Map.get_map_by_id(p.id_map_save);
                    for (Npc npc_temp : map_get[0].template.npcs) {
                        if (npc_temp.namegt.equals("Bản đồ")) {
                            x_save = npc_temp.x;
                            y_save = (npc_temp.y < 250) ? npc_temp.y + 20 : npc_temp.y - 40;
                            break;
                        }
                    }
                    //
                    if (x_save != -1 && y_save != -1) {
                        js.add(p.id_map_save);
                        js.add(0);
                        js.add(p.hp);
                        js.add(p.mp);
                        js.add(x_save);
                        js.add(y_save);
                    } else {
                        js.add(1);
                        js.add(0);
                        js.add(p.hp);
                        js.add(p.mp);
                        js.add(830);
                        js.add(203);
                    }
                } else {
                    js.add(p.map.template.id);
                    js.add(p.map.zone_id);
                    js.add(p.hp);
                    js.add(p.mp);
                    js.add(p.x);
                    js.add(p.y);
                }
                js.add(p.is_show_hat ? 1 : 0);
                js.add(p.pointPk);
                js.add(p.ticket);
                js.add(p.cd_ticket_next);
                js.add(p.pvp_ticket);
                js.add(p.cd_pvp_next);
                js.add(p.key_boss);
                js.add(p.cd_keyboss_next);
                js.add(p.is_show_weapon ? 1 : 0);
                //
                ps.setNString(3, js.toJSONString());
                js.clear();
                js = new JSONArray();
                //
                js.add(p.vang);
                js.add(p.kimcuong);
                js.add(p.vnd);
                js.add(p.bua);
                js.add(p.tichLuy);
                js.add(p.pvp_win);
                js.add(p.pvp_lose);
                js.add(p.time_ship);
                js.add(p.time_can_hs);
                js.add(p.time_nvl);
                js.add(p.time_ttvt);
                js.add(p.wanted_price);
                ps.setNString(4, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.bag3.length; i++) {
                    if (p.item.bag3[i] != null && p.item.bag3[i].template != null) {
                        JSONArray js_temp = Item.it_data_to_json(p.item.bag3[i]);
                        if (js_temp.size() > 0) {
                            js.add(js_temp);
                        }
                    }
                }
                ps.setNString(5, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.it_body.length; i++) {
                    if (p.item.it_body[i] != null && p.item.it_body[i].template != null) {
                        JSONArray js_temp = Item.it_data_to_json(p.item.it_body[i]);
                        if (js_temp.size() > 0) {
                            js.add(js_temp);
                        }
                    }
                }
                ps.setNString(6, js.toJSONString());
                js.clear();
                js = new JSONArray();
                js.add(p.pointAttribute);
                js.add(p.point1);
                js.add(p.point2);
                js.add(p.point3);
                js.add(p.point4);
                js.add(p.point5);
                js.add(p.pointAttributeThongThao);
                JSONArray js_in = new JSONArray();
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    JSONArray js_in1 = new JSONArray();
                    js_in1.add(p.list_op_thongthao.get(i).id);
                    js_in1.add(p.list_op_thongthao.get(i).getParam());
                    js_in.add(js_in1);
                }
                js.add(js_in);
                js.add(p.levelAwaken); // index 8
                JSONArray js_df = new JSONArray();
                for (short id : p.idDF_required) {
                    js_df.add(id);
                }
                js.add(js_df); // index 9
                js.add(p.hasUnlockedMastery2); // index 10
                js.add(p.thongthao2); // index 11
                js.add(p.pointAttributeThongThao2); // index 12
                JSONArray js_in_2 = new JSONArray();
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    JSONArray js_in1 = new JSONArray();
                    js_in1.add(p.list_op_thongthao2.get(i).id);
                    js_in1.add(p.list_op_thongthao2.get(i).getParam());
                    js_in_2.add(js_in1);
                }
                js.add(js_in_2); // index 13
                js.add(p.point6); // index 14
                js.add(p.point7); // index 15
                js.add(p.point8); // index 16
                js.add(p.point9); // index 17
                js.add(p.point10); // index 18
                ps.setNString(7, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.bag47.size(); i++) {
                    JSONArray js_temp = new JSONArray();
                    js_temp.add(p.item.bag47.get(i).category);
                    js_temp.add(p.item.bag47.get(i).id);
                    js_temp.add(p.item.bag47.get(i).quant);
                    js.add(js_temp);
                }
                ps.setNString(8, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.rms.length; i++) {
                    JSONArray js_temp = new JSONArray();
                    for (int j = 0; j < p.rms[i].length; j++) {
                        js_temp.add(p.rms[i][j]);
                    }
                    js.add(js_temp);
                }
                ps.setNString(9, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.skill_point.size(); i++) {
                    JSONArray js_temp = new JSONArray();
                    js_temp.add(p.skill_point.get(i).temp.indexSkillInServer);
                    js_temp.add(p.skill_point.get(i).exp);
                    js_temp.add(p.skill_point.get(i).lvdevil);
                    js_temp.add(p.skill_point.get(i).devilpercent);
                    js_temp.add(p.skill_point.get(i).realLevel); // Add realLevel to JSON
                    js_temp.add(p.skill_point.get(i).fruitId); // Add fruitId to JSON
                    js.add(js_temp);
                }
                ps.setNString(10, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.friend_list.size(); i++) {
                    js.add(p.friend_list.get(i).toJSONArray());
                }
                ps.setNString(11, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.enemy_list.size(); i++) {
                    js.add(p.enemy_list.get(i).toJSONArray());
                }
                ps.setNString(12, js.toJSONString());
                js.clear();
                js = new JSONArray();
                JSONArray js_temp_2 = new JSONArray();
                for (int i = 0; i < p.itfashionP.size(); i++) {
                    JSONArray js14 = new JSONArray();
                    js14.add(p.itfashionP.get(i).category);
                    js14.add(p.itfashionP.get(i).id);
                    js14.add(p.itfashionP.get(i).icon);
                    js14.add(p.itfashionP.get(i).is_use ? 1 : 0);
                    js_temp_2.add(js14);
                }
                js.add(js_temp_2);
                //
                JSONArray js_temp_22 = new JSONArray();
                for (int i = 0; i < p.fashion.size(); i++) {
                    ItemFashionP2 fas = p.fashion.get(i);
                    JSONArray js14 = new JSONArray();
                    js14.add(fas.id);
                    js14.add(fas.is_use ? 1 : 0);
                    js14.add(fas.level);
                    js14.add(fas.expirationTime);
                    // Thêm option
                    JSONArray js_op = new JSONArray();
                    if (fas.op != null && fas.op instanceof List<?>) {
                        for (Object obj : (List<?>) fas.op) {
                            if (obj instanceof Option) {
                                Option op = (Option) obj;
                                JSONArray js_op_entry = new JSONArray();
                                js_op_entry.add(op.id);
                                js_op_entry.add(op.getParam());
                                js_op.add(js_op_entry);
                            }
                        }
                    }
                    js14.add(js_op);
                    js_temp_22.add(js14);
                }
                js.add(js_temp_22);
                //
                JSONArray js_temp_23 = new JSONArray();
                for (int i = 0; i < p.itemboat.size(); i++) {
                    JSONArray js14 = new JSONArray();
                    js14.add(p.itemboat.get(i).id);
                    js14.add(p.itemboat.get(i).is_use ? 1 : 0);
                    js_temp_23.add(js14);
                }
                js.add(js_temp_23);
                //
                ps.setNString(13, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (EffTemplate eff_temp : p.map_eff.values()) {
                    if (eff_temp != null && EffTemplate.check_eff_can_save(eff_temp.id)) {
                        JSONArray js_temp = new JSONArray();
                        js_temp.add(eff_temp.id);
                        js_temp.add(eff_temp.param);
                        js_temp.add(eff_temp.time - System.currentTimeMillis());
                        js.add(js_temp);
                    }
                }
                ps.setNString(14, js.toJSONString()); // eff
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.box47.size(); i++) {
                    JSONArray js_temp = new JSONArray();
                    js_temp.add(p.item.box47.get(i).category);
                    js_temp.add(p.item.box47.get(i).id);
                    js_temp.add(p.item.box47.get(i).quant);
                    js.add(js_temp);
                }
                ps.setNString(15, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.box3.length; i++) {
                    if (p.item.box3[i] != null && p.item.box3[i].template != null) {
                        JSONArray js_temp = Item.it_data_to_json(p.item.box3[i]);
                        if (js_temp.size() > 0) {
                            js.add(js_temp);
                        }
                    }
                }
                ps.setNString(16, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.list_quest.size(); i++) {
                    QuestP temp = p.list_quest.get(i);
                    JSONArray js_p = new JSONArray();
                    js_p.add(temp.template.id);
                    JSONArray js_p2 = new JSONArray();
                    for (int j = 0; j < temp.data.length; j++) {
                        JSONArray js_p3 = new JSONArray();
                        for (int k = 0; k < temp.data[j].length; k++) {
                            js_p3.add(temp.data[j][k]);
                        }
                        js_p2.add(js_p3);
                    }
                    js_p.add(js_p2);
                    js.add(js_p);
                }
                ps.setNString(17, js.toJSONString());
                js.clear();
                long exp = (long) p.reincarnation * 1000000000000000L; // Ưu tiên trùng sinh cao nhất
                exp += p.exp;
                for (int i = 0; i < (p.level - 1); i++) {
                    if (i < Level.ENTRYS.length) {
                        exp += Level.ENTRYS[i].exp;
                    }
                }
                for (int i = 0; i < p.thongthao; i++) {
                    if (i < Level.LEVEL_THONGTHAO.length) {
                        exp += Level.LEVEL_THONGTHAO[i];
                        // } else {
                        // exp += Level.LEVEL_THONGTHAO[Level.LEVEL_THONGTHAO.length - 1];
                    }
                }
                ps.setLong(18, exp);
                ps.setInt(19, p.pvppoint);
                js = new JSONArray();
                for (int i = 0; i < p.item.save_item_wear.size(); i++) {
                    JSONArray js_temp = Item.it_data_to_json(p.item.save_item_wear.get(i));
                    if (js_temp.size() > 0) {
                        js.add(js_temp);
                    }
                }
                ps.setNString(20, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.item.save_item_47.size(); i++) {
                    JSONArray js_temp = new JSONArray();
                    js_temp.add(p.item.save_item_47.get(i).category);
                    js_temp.add(p.item.save_item_47.get(i).id);
                    js_temp.add(p.item.save_item_47.get(i).quant);
                    js.add(js_temp);
                }
                ps.setNString(21, js.toJSONString());
                js.clear();
                js = new JSONArray();
                for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                    JSONArray js_temp = new JSONArray();
                    js_temp.add(p.daHanhTrinh.get(i).category);
                    js_temp.add(p.daHanhTrinh.get(i).id);
                    js_temp.add(p.daHanhTrinh.get(i).quant);
                    js.add(js_temp);
                }
                ps.setNString(22, js.toJSONString());
                js.clear();
                ps.setLong(23, p.wanted_price);
                js = new JSONArray();
                for (int i = 0; i < p.wanted_chest.length; i++) {
                    if (p.wanted_chest[i] != null) {
                        JSONArray js_in2 = new JSONArray();
                        js_in2.add(p.wanted_chest[i].id);
                        js_in2.add(p.wanted_chest[i].timeUse);
                        js_in2.add(p.wanted_chest[i].maxTimeUse);
                        js_in2.add(p.wanted_chest[i].Ruby);
                        js.add(js_in2);
                    }
                }
                ps.setNString(24, js.toJSONString());
                js.clear();
                if (p.my_pet != null) {
                    for (int i = 0; i < p.my_pet.size(); i++) {
                        MyPet pet = p.my_pet.get(i);
                        if (pet != null && pet.template != null) {
                            JSONArray js_in2 = new JSONArray();
                            js_in2.add(pet.id);
                            js_in2.add(pet.template.id);
                            js_in2.add(pet.isUse ? 1 : 0);
                            js.add(js_in2);
                        }
                    }
                }
                ps.setNString(25, js.toJSONString());
                js.clear();
                // Khởi tạo JSON Object để lưu trữ cả "nap" và "tieu"
                JSONObject jsObject = new JSONObject();

                // Thêm trạng thái "nap" vào JSON
                JSONArray napArray = new JSONArray();
                if (p.tichNapCheck != null) {
                    for (int i = 0; i < p.tichNapCheck.length; i++) {
                        napArray.add(p.tichNapCheck[i]);
                    }
                }
                jsObject.put("nap", napArray);

                // Thêm trạng thái "tieu" vào JSON
                JSONArray tieuArray = new JSONArray();
                if (p.tichTieuCheck != null) {
                    for (int i = 0; i < p.tichTieuCheck.length; i++) {
                        tieuArray.add(p.tichTieuCheck[i]);
                    }
                }
                jsObject.put("tieu", tieuArray);

                // Lưu vào SQL trong cột tichluycheck
                ps.setNString(26, jsObject.toJSONString());

                ps.setLong(27, p.beri0);
                ps.setLong(28, p.ruby0);
                //
                js.clear();
                js = new JSONArray();
                if (p.item != null && p.item.second_body != null) {
                    for (int i = 0; i < p.item.second_body.length; i++) {
                        if (p.item.second_body[i] != null && p.item.second_body[i].template != null) {
                            JSONArray js_temp = Item.it_data_to_json(p.item.second_body[i]);
                            if (js_temp != null && js_temp.size() > 0) {
                                js.add(js_temp);
                            }
                        }
                    }
                }
                ps.setNString(29, js.toJSONString());
                // Ghi đè pica0 = dame hiện tại
                p.pica0 = p.body.get_dame(true);
                ps.setLong(30, p.pica0);
                js.clear();
                js = new JSONArray();
                if (p.archiDaily != null) {
                    for (int i = 0; i < p.archiDaily.length; i++) {
                        if (p.archiDaily[i] != null) {
                            JSONArray js_in2 = new JSONArray();
                            js_in2.add(p.archiDaily[i].id);
                            js_in2.add(p.archiDaily[i].num);
                            js_in2.add(p.archiDaily[i].type);
                            js.add(js_in2);
                        }
                    }
                }
                ps.setString(31, js.toJSONString());
                js.clear();
                // Xử lý danh_hieu (thêm mới)
                js = new JSONArray();
                if (p.danh_hieu != null) {
                    for (int i = 0; i < p.danh_hieu.size(); i++) {
                        JSONArray js_in2 = new JSONArray();
                        MyDanhHieu myDanhHieu = p.danh_hieu.get(i);
                        if (myDanhHieu != null) {
                            // Lưu ID danh hiệu và trạng thái sử dụng
                            js_in2.add(myDanhHieu.id); // ID danh hiệu
                            js_in2.add(myDanhHieu.isUsed ? 1 : 0); // Trạng thái sử dụng (1 = đang sử dụng, 0 = không sử
                                                                   // dụng)
                            js.add(js_in2);
                        }
                    }
                }
                ps.setNString(32, js.toJSONString());
                js.clear();
                //
                JSONArray tuHanh = new JSONArray();
                tuHanh.add(p.tu_tien);
                tuHanh.add(p.tu_ma);
                ps.setString(33, tuHanh.toJSONString());
                ps.setInt(34, p.tocSuper);
                js.clear();
                js = new JSONArray();
                if (p.item != null && p.item.third_body != null) {
                    for (int i = 0; i < p.item.third_body.length; i++) {
                        if (p.item.third_body[i] != null && p.item.third_body[i].template != null) {
                            JSONArray js_temp = Item.it_data_to_json(p.item.third_body[i]);
                            if (js_temp != null && js_temp.size() > 0) {
                                js.add(js_temp);
                            }
                        }
                    }
                }
                ps.setNString(35, js.toJSONString());
                js.clear();
                //

                // Disciple data save
                if (p.myDisciple != null) {
                    ps.setNString(36, p.myDisciple.saveData());
                } else {
                    ps.setNString(36, "null");
                }
                ps.setByte(37, p.clazz);
                ps.setInt(38, p.fusionType);
                ps.setLong(39, p.fusionExpiry);
                ps.setInt(40, p.isOfflineTraining ? 1 : 0);
                ps.setLong(41, p.offlineTrainingEndTime);
                ps.setInt(42, p.offlineTrainingType);
                ps.setShort(43, p.offlineX);
                ps.setShort(44, p.offlineY);

                result = ps.executeUpdate();
                if (result > 0) {
                    p.dirty = false;
                }

            } catch (SQLException e) {
                GameLogger.error("Player.flush: SQL error saving data for " + p.name, e);
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    GameLogger.warn("Player.flush: SQL error closing resources for " + p.name, e);
                }
            }
            return result;
        }
    }

    public void saveOldMap() {
        this.oldMapId = this.map.template.id;
        this.oldX = this.x;
        this.oldY = this.y;
    }

    public void goto_map(Vgo vgo) throws IOException {
        this.ischangemap = false;
        this.xold = this.x;
        this.yold = this.y;
        Map[] map_go = vgo.map_go;

        // --- Ocean Maze Global Redirection ---
        if (map_go != null && map_go.length > 0 && map_go[0].template.id == 314) {
            Map entranceNode = core.OceanMazeManager.getMazeNode(999);
            if (entranceNode != null) {
                map_go = new Map[] { entranceNode };
                vgo.xnew = 400;
                vgo.ynew = 400;
                this.lastMazeTeleport = System.currentTimeMillis();
            }
        }
        // ------------------------------------

        if (map_go == null) {
            Service.send_box_ThongBao_OK(this, "Chưa thể đi đến map này");
            return;
        }

        // ENTRY REQUIREMENT FOR MAP 320
        if (map_go[0].template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
            long remaining = BossEvent.BossEvent.getInstance().getRemainingCooldown();
            if (remaining > 0) {
                Service.send_box_ThongBao_OK(this, "Kaido đang hồi phục sức mạnh... Quay lại sau "
                        + (remaining / 60000 + 1) + " phút nữa!");
                return;
            }
            if (this.item.total_item_bag_by_id(4, 196) < 1) {
                Service.send_box_ThongBao_OK(this, "Bạn cần Cánh Hoa Phượng để vào khu vực này!");
                return;
            }
            // Consume 1 Cánh Hoa Phượng
            this.item.remove_item47(4, 196, 1);
            this.item.update_Inventory(-1, false);
            Service.send_box_ThongBao_OK(this, "Bạn đã tiêu tốn 1 Cánh Hoa Phượng để vào Boss Event!");
        }

        // ENTRY REQUIREMENT FOR MAP 316 (Bãi Farm Đá 2)
        if (map_go[0].template.id == 316) {
            boolean has0 = false, has1 = false, has2 = false;
            for (Skill_info sk : this.skill_point) {
                if (sk != null && sk.temp != null && sk.temp.Lv_RQ >= 500) {
                    if (sk.temp.ID == 0)
                        has0 = true;
                    else if (sk.temp.ID == 1)
                        has1 = true;
                    else if (sk.temp.ID == 2)
                        has2 = true;
                }
            }
            if (!has0 || !has1 || !has2) {
                Service.send_box_ThongBao_OK(this, "Bạn cần cả 3 kỹ năng đạt cấp 500 trở lên để vào đây!");
                return;
            }
        }

        // --- Ocean Maze Entry Requirement ---
        if (map_go[0].isMazeMap && !this.map.isMazeMap) {
            if (this.party == null || this.party.list.isEmpty() ||
                    !this.party.list.get(0).equals(this)) {
                Service.send_box_ThongBao_OK(this, "Bạn cần đi cùng đồng đội để vào đây!");
                return;
            }
        }
        // ------------------------------------
        if (this.ship_pet != null) {
            int nextMapId = map_go[0].template.id;
            // [MERCHANT] Cho phép di chuyển trong lộ trình từ Map 22 về Map 0 (Bãi biển
            // hoang sơ)
            if (nextMapId > 22 && nextMapId != 0 && nextMapId != 1 && nextMapId != 2) {
                Service.send_box_ThongBao_OK(this, "Bạn đang vận chuyển hàng\nHãy đưa hàng đến Bãi biển hoang sơ");
                return;
            }
        }
        this.wait_change_map = true;
        if (this.hp > 0 && this.ship_pet != null && this.map.equals(this.ship_pet.map)
                && Math.abs(this.x - this.ship_pet.x) < 200
                && Math.abs(this.y - this.ship_pet.y) < 200) {
            this.ship_pet.map = null;
        }
        int zone_into = 0;
        while (zone_into < (map_go.length - 1)
                && map_go[zone_into].players.size() >= map_go[zone_into].template.max_player) {
            zone_into++;
        }
        if (map_go[zone_into].players.size() >= map_go[zone_into].template.max_player) {
            Service.send_box_ThongBao_OK(this, "Khu vực đã đầy, không thể vào");
            return;
        }
        //
        boolean send_boat = false;
        for (int i = 0; i < DataTemplate.mSea.length; i++) {
            if (DataTemplate.mSea[i][0] == map_go[zone_into].template.id
                    && DataTemplate.mSea[i][1] != this.map.template.id) {
                send_boat = true;
                break;
            }
        }
        if (zone_into >= map_go.length) {
            zone_into = map_go.length - 1;
        }
        byte oldSpecMap = this.map.template.specMap;
        this.map.leave_map(this, 2);
        this.map = map_go[zone_into];
        this.x = vgo.xnew;
        this.y = vgo.ynew;
        this.xold = this.x;
        this.yold = this.y;
        this.map.goto_map(this);
        this.time_change_map = System.currentTimeMillis() + 3000L;
        Service.update_PK(this, this, true);
        Service.pet(this, this, true);
        this.map.send_boat(this, send_boat);
        Quest.update_map_have_side_quest(this, true);
        this.map.update_boat(this, this, true);
        if (oldSpecMap != this.map.template.specMap) {
            try {
                this.send_skill();
            } catch (IOException e) {
                GameLogger.error("Player.goto_map: Error sending skills after map change for " + this.name, e);
            }
        }

        // [STABILITY] Sync disciple map transition after master moves
        if (this.myDisciple != null) {
            // 1. Ensure disciple leaves its current map if master moved
            if (this.myDisciple.map != null) {
                this.myDisciple.map.leave_map(this.myDisciple, 1);
            }

            // 2. Sync map reference and coordinates
            this.myDisciple.map = this.map;
            this.myDisciple.x = (short) (this.x + Util.random(-20, 21));
            this.myDisciple.y = this.y;

            if (this.myDisciple.isMapCamDeTu(this.map)) {
                this.myDisciple.status = 2; // Auto-recall if map is restricted
            } else if (!this.myDisciple.isdie && this.myDisciple.status != 2) {
                // 3. Enter new map if alive and active
                this.myDisciple.lastUpdateAI = System.currentTimeMillis() + 500; // Debounce AI update to prevent double
                                                                                 // entry
                this.map.enter_map(this.myDisciple);
                try {
                    Message m = new Message(1);
                    m.writer().writeByte(0); // Player type
                    m.writer().writeShort(this.myDisciple.index_map);
                    m.writer().writeShort(this.myDisciple.x);
                    m.writer().writeShort(this.myDisciple.y);
                    this.map.send_msg_all_p(m, null, true);
                    m.cleanup();

                    // Broadcast full info to all players in map (including master)
                    this.myDisciple.update_info_to_all();
                } catch (IOException e) {
                    core.GameLogger.error("Player.goto_map: Error sending disciple map transition", e);
                    // Fail silently
                }
            }
        }
    }

    public int checkQuest() {
        return 9999;
    }

    public void change_map(Vgo vgo) throws IOException {
        this.ischangemap = false;
        this.xold = this.x;
        this.yold = this.y;
        Map[] map_go = vgo.map_go;
        if (map_go == null) {
            Service.send_box_ThongBao_OK(this, "Chưa thể đi đến map này!");
            return;
        }
        Message m = new Message(30);
        this.send_msg(m);
        m.cleanup();
        core.GameLogger.info("send msg 30");
        this.map.leave_map(this, 2);

        int zone_into = 0;
        while (zone_into < (map_go[zone_into].template.max_zone - 1)
                && map_go[zone_into].players.size() >= map_go[zone_into].template.max_player) {
            zone_into++;
        }
        this.map = map_go[zone_into];
        this.x = (short) vgo.xnew;
        this.y = (short) vgo.ynew;
        this.xold = this.x;
        this.yold = this.y;
    }

    public synchronized void update_exp(long exp_up, boolean multi) throws IOException {
        markDirty();

        // Chặn EXP khi đạt max trùng sinh — không reset, không tụt
        if (this.level >= 100 && this.thongthao >= 200) {
            return;
        }
        if (get_eff(8) != null) {
            return;
        }
        if (multi) {
            exp_up *= Manager.gI().exp;
        }
        int divisor = Math.max(1, thongthao / 2);
        exp_up /= divisor;
        this.exp += exp_up;
        //
        // Cấp thực: lên đến 100 rồi dừng, logic trùng sinh diễn ra ở phần dưới
        if (this.level < 100 && this.exp >= Level.ENTRYS[this.level - 1].exp) {
            while (this.level < 100 && this.exp >= Level.ENTRYS[this.level - 1].exp) {
                this.exp -= Level.ENTRYS[this.level - 1].exp;
                this.level++;
                this.pointAttribute += getPotentialPerLevel();
            }
            Service.send_eff(this, 0, 0);
            this.invalidateStatCache();
            this.update_info_to_all();
            this.update_money();
            Service.CountDown_Ticket(this);
        }
        // Cấp trùng sinh (thongthao): bắt đầu khi level = 100, tối đa 200
        if (this.level > 100) {
            this.level = 100;
            this.update_info_to_all();
        }
        if (level >= 100) {
            if (this.exp >= Level.LEVEL_THONGTHAO[this.thongthao]) {
                while (this.exp >= Level.LEVEL_THONGTHAO[this.thongthao]) {
                    this.exp -= Level.LEVEL_THONGTHAO[this.thongthao];
                    this.thongthao++;
                    this.pointAttributeThongThao++;
                }
                Service.send_eff(this, 0, 0);
                this.invalidateStatCache();
                this.update_info_to_all();
                this.update_money();
                Service.CountDown_Ticket(this);
            }
        }
        // Khi đạt đỉnh trùng sinh: clamp lại, không để vượt
        if (this.thongthao >= 200) {
            this.thongthao = 200;
            if (Level.LEVEL_THONGTHAO.length >= 200) {
                this.exp = Level.LEVEL_THONGTHAO[199] - 1;
            } else {
                this.exp = 0;
            }
            this.update_info_to_all();
        }

        Message m = new Message(10);
        m.writer().writeShort(this.index_map);
        m.writer().writeShort(get_level_percent());
        m.writer().writeInt(f.setInteger(exp_up));
        this.send_msg(m);
        m.cleanup();
    }

    public void request_live_from_die(Message m2) throws IOException {
        if (this.map.template.id == 81 && this.map.map_little_garden != null) {
            return;
        }
        byte type = m2.reader().readByte();
        if (type == 1) {
            Service.send_box_yesno(this, 14, "Thông báo",
                    ("Hồi sinh tại chỗ mất 500 Beri, bạn có muốn hồi sinh không?"),
                    new String[] { "500", "Hủy" }, new byte[] { 6, -1 });
        } else { // ve lang
            revive_to_village();
        }
    }

    public void revive_to_village() throws IOException {
        if (this.isdie) {
            this.isdie = false;
            this.hp = this.body.get_hp_max(true);
            this.mp = this.body.get_mp_max(true);
            if (this.myDisciple != null) {
                this.myDisciple.isdie = false;
                this.myDisciple.hp = this.myDisciple.body.get_hp_max(true);
                this.myDisciple.mp = this.myDisciple.body.get_mp_max(true);
            }
            Vgo vgo = new Vgo();
            vgo.map_go = Map.get_map_by_id(this.id_map_save);
            for (int i = 0; i < vgo.map_go[0].template.npcs.size(); i++) {
                Npc npc_temp = vgo.map_go[0].template.npcs.get(i);
                if (npc_temp.namegt.equals("Bản đồ")) {
                    vgo.xnew = npc_temp.x;
                    if (npc_temp.y < 250) {
                        vgo.ynew = (short) (npc_temp.y + 20);
                    } else {
                        vgo.ynew = (short) (npc_temp.y - 40);
                    }
                    break;
                }
            }
            this.goto_map(vgo);
            Service.use_potion(this, 0, this.hp);
            Service.use_potion(this, 1, this.mp);
            Service.send_revive_player(this);
            if (this.myDisciple != null) {
                Service.send_revive_player(this.myDisciple);
            }
            this.time_can_mob_atk = System.currentTimeMillis() + 1200L;
        }
    }

    public void updateOfflineTrainingAI() {
        // Double-check: thoát ngay nếu đã bị hijack
        if (!this.isOfflineTraining)
            return;
        long now = System.currentTimeMillis();
        // Tốc độ đánh dựa trên loại vé (VIP type 2: 100ms, Thường type 1: 300ms)
        long attackDelay = (this.offlineTrainingType == 2) ? 10L : 100L;
        if (now - this.lastUpdateOfflineAI < attackDelay)
            return;
        this.lastUpdateOfflineAI = now;

        // Auto regen MP để AI không bị đứng hình
        if (this.mp < this.body.get_mp_max(true)) {
            this.mp = this.body.get_mp_max(true);
        }

        // ── BƯỚC 1: Tìm mob target hợp lệ ──────────────────────────────
        if (this.targetMobOffline == null || this.targetMobOffline.isdie
                || this.targetMobOffline.map != this.map) {
            this.targetMobOffline = null;
            int minDis = 5000;
            if (this.map == null || this.map.mobs_cache == null)
                return;
            for (Mob mob : this.map.mobs_cache) {
                if (mob == null || mob.isdie)
                    continue;
                int dx = this.x - mob.x;
                int dy = this.y - mob.y;
                int dis = Math.abs(dx) + Math.abs(dy);
                if (dis < minDis) {
                    minDis = dis;
                    this.targetMobOffline = mob;
                }
            }
        }
        if (this.targetMobOffline == null)
            return;

        // ── BƯỚC 2: Teleport đến mob ────────────────────────────────────
        this.x = (short) this.targetMobOffline.x;
        this.y = (short) this.targetMobOffline.y;
        try {
            this.map.send_move(this, this.x, this.y);
        } catch (java.io.IOException e) {
            // Dummy session ignore
        }

        // ── BƯỚC 3: Chọn ngẫu nhiên một skill đã học (Không tốn MP, không hồi chiêu)
        // ──
        Skill_info sk = null;
        if (this.skill_point != null && !this.skill_point.isEmpty()) {
            List<Skill_info> activeSkills = new ArrayList<>();
            for (Skill_info s : this.skill_point) {
                if (s != null && s.temp != null && s.temp.typeSkill != 3) { // Bỏ qua Passive
                    activeSkills.add(s);
                }
            }
            if (!activeSkills.isEmpty()) {
                sk = activeSkills.get(core.Util.random(activeSkills.size()));
            }
        }

        // ── BƯỚC 4: Tấn công ───────────────────────────────────────────
        if (sk != null) {
            if (!this.isOfflineTraining)
                return; // ← thêm dòng này
            List<Integer> targets = new ArrayList<>();
            targets.add((int) this.targetMobOffline.index);
            try {
                // Tấn công trực tiếp, bỏ qua mọi giới hạn MP/Cooldown
                this.map.use_skill(this, (short) sk.temp.ID, (byte) 1, (byte) 1, targets);
            } catch (Exception e) {
                // Ignore error
            }
        }
    }

    public void update_money() throws IOException {
        this.item.update_assets_Inventory(false);
    }

    public long get_vang() {
        return this.vang;
    }

    public long get_ngoc() {
        return this.kimcuong;
    }

    public long getBeri0() {
        return this.beri0;
    }

    public long getRuby0() {
        return this.ruby0;
    }

    public int getTichNap() {
        return 0;
    }

    public long get_vnd() {
        return this.vnd;
    }

    public synchronized void update_vnd(long par) {
        this.vnd += par;
        if (this.vnd < 0) {
            this.vnd = 0;
        }
    }

    public synchronized void refresh_vnd() {
        if (conn == null) {
            return;
        }
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery("SELECT `point_inven` FROM `players` WHERE `id` = " + this.id + " LIMIT 1;");
            if (rs.next()) {
                String point_inven = rs.getString("point_inven");
                org.json.simple.JSONArray js = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(point_inven);
                this.vnd = Long.parseLong(js.get(2).toString());
                this.update_money();
            }
        } catch (Exception e) {
            GameLogger.error("Player.refresh_vnd: SQL error refreshing VND for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.refresh_vnd: SQL error closing resources for " + this.name, e);
            }
        }
    }

    public synchronized void update_vang(long par) {
        if ((((long) par) + this.vang) < 2_000_000_000_000_000L) {
            this.vang += par;
        }
    }

    public synchronized void updateBeri0(long par) {
        if ((((long) par) + this.beri0) < 2_000_000_000_000_000L) {
            this.beri0 += par;
        }
    }

    public synchronized void update_ngoc(long par) throws IOException {
        if ((((long) par) + this.kimcuong) < 2_000_000_000_000_000L) {
            this.kimcuong += par;
            if (par < 0) {
                update_tong_tieu(-par);
            }
        }
    }

    public synchronized void updateNgocForGift(long par) throws IOException {
        if ((((long) par) + this.kimcuong) < 2_000_000_000_000_000L) {
            this.kimcuong += par;
        }
    }

    public synchronized void updateRuby0(long par) {
        if ((((long) par) + this.ruby0) < 2_000_000_000_000_000L) {
            this.ruby0 += par;
        }
    }

    public synchronized void updatePica0(long par) {
        if ((((long) par) + this.pica0) < 2_000_000_000_000_000L) {
            this.pica0 += par;
        }
    }

    public synchronized void updateTuTien(long par) {
        if ((((long) par) + this.tu_tien) < 2_000_000_000L) {
            this.tu_tien += par;
        }
    }

    public synchronized void updateTuMa(long par) {
        if ((((long) par) + this.tu_ma) < 2_000_000_000L) {
            this.tu_ma += par;
        }
    }

    public synchronized void update_extol(long par) throws IOException {
        if ((((long) par) + this.vnd) < 2_000_000_000_000_000L) {
            this.vnd += par;
            this.update_money();
        }
    }

    public static void update_extol_offline(int playerId, long amount) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            ps = connection.prepareStatement("SELECT `point_inven` FROM `players` WHERE `id` = ? LIMIT 1;",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, playerId);
            rs = ps.executeQuery();
            if (rs.next()) {
                String pointInvenStr = rs.getString("point_inven");
                JSONArray js = (JSONArray) JSONValue.parse(pointInvenStr);
                if (js != null && js.size() > 2) {
                    long currentExtol = Long.parseLong(js.get(2).toString());
                    js.set(2, currentExtol + amount);

                    ps.close();
                    ps = connection.prepareStatement("UPDATE `players` SET `point_inven` = ? WHERE `id` = ?;");
                    ps.setString(1, js.toJSONString());
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            GameLogger.error("Player.update_extol_offline: SQL error updating offline extol for playerID " + playerId,
                    e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                GameLogger.warn("Player.update_extol_offline: SQL error closing resources for playerID " + playerId, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void updateDameBossSQL(int bossId, String playerName, long dame) {
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement psSelect = conn.prepareStatement("SELECT `data` FROM `damebosssave` WHERE `id` = ?");
            psSelect.setInt(1, bossId);
            ResultSet rs = psSelect.executeQuery();
            JSONArray dameList = new JSONArray();
            boolean found = false;
            if (rs.next()) {
                String dataStr = rs.getString("data");
                if (dataStr != null && !dataStr.isEmpty()) {
                    dameList = (JSONArray) new JSONParser().parse(dataStr);
                    for (Object obj : dameList) {
                        JSONArray entry = (JSONArray) obj;
                        if (entry.get(0).equals(playerName)) {
                            long currentDame = (long) entry.get(1);
                            entry.set(1, currentDame + dame);
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                JSONArray newEntry = new JSONArray();
                newEntry.add(playerName);
                newEntry.add(dame);
                dameList.add(newEntry);
            }
            PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE `damebosssave` SET `data` = ? WHERE `id` = ?");
            psUpdate.setString(1, dameList.toJSONString());
            psUpdate.setInt(2, bossId);
            psUpdate.executeUpdate();
            psSelect.close();
            psUpdate.close();
        } catch (Exception e) {
            GameLogger.error("Player.updateDameBossSQL: SQL error updating boss damage for bossID " + bossId
                    + " player: " + playerName, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void ProcessBossRewardsAndReset() {
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement ps = conn.prepareStatement("SELECT `id`, `data`, `gift` FROM `damebosssave`");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int bossId = rs.getInt("id");
                String dataStr = rs.getString("data");
                String giftStr = rs.getString("gift");
                JSONArray dameList = (dataStr == null || dataStr.isEmpty()) ? new JSONArray()
                        : (JSONArray) new JSONParser().parse(dataStr);
                List<JSONArray> sorted = new ArrayList<>();
                for (Object obj : dameList) {
                    sorted.add((JSONArray) obj);
                }
                sorted.sort((a, b) -> Long.compare((long) ((JSONArray) b).get(1), (long) ((JSONArray) a).get(1)));
                JSONArray oldGifts = (giftStr == null || giftStr.isEmpty()) ? new JSONArray()
                        : (JSONArray) new JSONParser().parse(giftStr);
                for (int i = 0; i < Math.min(10, sorted.size()); i++) {
                    JSONArray topPlayer = sorted.get(i);
                    String name = topPlayer.get(0).toString();
                    List<GiftBox> gifts = getGiftsForRank(i, bossId);
                    JSONArray playerGift = null;
                    for (Object obj : oldGifts) {
                        JSONArray giftEntry = (JSONArray) obj;
                        if (giftEntry.get(0).toString().equalsIgnoreCase(name)) {
                            playerGift = giftEntry;
                            break;
                        }
                    }
                    if (playerGift == null) {
                        playerGift = new JSONArray();
                        playerGift.add(name);
                        playerGift.add(new JSONArray());
                        oldGifts.add(playerGift);
                    }
                    JSONArray giftList = (JSONArray) playerGift.get(1);
                    for (GiftBox g : gifts) {
                        boolean merged = false;
                        for (Object obj : giftList) {
                            JSONArray existing = (JSONArray) obj;
                            if ((long) existing.get(0) == g.id &&
                                    (long) existing.get(1) == g.type &&
                                    existing.get(2).toString().equalsIgnoreCase(g.name) &&
                                    (long) existing.get(3) == g.icon &&
                                    (long) existing.get(5) == g.color) {
                                long currentNum = (long) existing.get(4);
                                existing.set(4, currentNum + g.num);
                                merged = true;
                                break;
                            }
                        }
                        if (!merged) {
                            JSONArray giftEntry = new JSONArray();
                            giftEntry.add((long) g.id);
                            giftEntry.add((long) g.type);
                            giftEntry.add(g.name);
                            giftEntry.add((long) g.icon);
                            giftEntry.add((long) g.num);
                            giftEntry.add((long) g.color);
                            giftList.add(giftEntry);
                        }
                    }
                }
                PreparedStatement update = conn
                        .prepareStatement("UPDATE `damebosssave` SET `gift` = ?, `data` = '[]' WHERE `id` = ?");
                update.setString(1, oldGifts.toJSONString());
                update.setInt(2, bossId);
                update.executeUpdate();
                update.close();
            }
            ps.close();
        } catch (Exception e) {
            GameLogger.error("Player.ProcessBossRewardsAndReset: SQL error processing boss rewards", e);
        }
    }

    public static List<GiftBox> getGiftsForRank(int rank, int bossId) {
        List<GiftBox> list = new ArrayList<>();
        int numReward = switch (rank) {
            case 0 -> 10; // top 1
            case 1 -> 5;
            case 2 -> 3;
            default -> 1; // top 10
        };
        GiftBox beri = new GiftBox();
        ItemTemplate4 beriTemplate = ItemTemplate4.get_it_by_id(0);
        beri.id = beriTemplate.id;
        beri.type = 4;
        beri.name = beriTemplate.name;
        beri.icon = beriTemplate.icon;
        int beriAmount = numReward * 100_000_000;
        beri.num = beriAmount;
        beri.color = 0;
        list.add(beri);
        //
        GiftBox ruongtb = new GiftBox();
        ItemTemplate4 it4v = ItemTemplate4.get_it_by_id(1053);
        ruongtb.id = it4v.id;
        ruongtb.type = 4;
        ruongtb.name = it4v.name;
        ruongtb.icon = it4v.icon;
        ruongtb.num = numReward;
        ruongtb.color = 0;
        list.add(ruongtb);
        return list;
    }

    public static void NhanQuaTopBoss(Player p) {
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement ps = conn.prepareStatement("SELECT `id`, `gift` FROM `damebosssave`");
            ResultSet rs = ps.executeQuery();
            List<Integer> bossGiftToClear = new ArrayList<>();
            List<GiftBox> totalGifts = new ArrayList<>();
            while (rs.next()) {
                int bossId = rs.getInt("id");
                String giftStr = rs.getString("gift");
                if (giftStr == null || giftStr.isEmpty())
                    continue;
                JSONArray giftList = (JSONArray) new JSONParser().parse(giftStr);
                JSONArray newGiftList = new JSONArray();
                for (Object obj : giftList) {
                    JSONArray giftEntry = (JSONArray) obj;
                    String name = (String) giftEntry.get(0);
                    if (name.equals(p.name)) {
                        JSONArray gifts = (JSONArray) giftEntry.get(1);
                        for (Object gObj : gifts) {
                            JSONArray g = (JSONArray) gObj;
                            GiftBox gb = new GiftBox();
                            gb.id = ((Long) g.get(0)).shortValue();
                            gb.type = ((Long) g.get(1)).byteValue();
                            gb.name = (String) g.get(2);
                            gb.icon = ((Long) g.get(3)).shortValue();
                            gb.num = ((Long) g.get(4)).intValue();
                            gb.color = ((Long) g.get(5)).byteValue();
                            boolean merged = false;
                            for (GiftBox existing : totalGifts) {
                                if (existing.id == gb.id &&
                                        existing.type == gb.type &&
                                        existing.name.equalsIgnoreCase(gb.name) &&
                                        existing.icon == gb.icon &&
                                        existing.color == gb.color) {
                                    existing.num += gb.num;
                                    merged = true;
                                    break;
                                }
                            }
                            if (!merged) {
                                totalGifts.add(gb);
                            }
                        }
                        bossGiftToClear.add(bossId);
                    } else {
                        newGiftList.add(giftEntry);
                    }
                }
                if (bossGiftToClear.contains(bossId)) {
                    PreparedStatement update = conn
                            .prepareStatement("UPDATE `damebosssave` SET `gift` = ? WHERE `id` = ?");
                    update.setString(1, newGiftList.toJSONString());
                    update.setInt(2, bossId);
                    update.executeUpdate();
                    update.close();
                }
            }
            rs.close();
            ps.close();
            if (!totalGifts.isEmpty()) {
                Service.send_gift(p, 1, "Quà TOP Săn Boss", "", totalGifts, true);
                p.addDanhHieu(12);
            } else {
                Service.send_box_ThongBao_OK(p, "Bạn không có quà hoặc đã nhận rồi\n" +
                        "Hằng ngày sẽ trao quà vào lúc 12h đêm\n"
                        + "Quà sẽ được lưu trữ mỗi ngày cho bạn");
            }
        } catch (Exception e) {
            GameLogger.error("Player.NhanQuaTopBoss: SQL error receiving boss top gifts for " + p.name, e);
        }
    }

    public static long getTotalDamePlayers() {
        long total = 0;
        try (Connection conn = SQL.gI().getCon();
                Statement ps = conn.createStatement();
                ResultSet rs = ps.executeQuery("SELECT SUM(pica0) as total FROM players")) {
            if (rs.next()) {
                total = rs.getLong("total");
            }
        } catch (Exception e) {
            GameLogger.error("Player.getTotalDamePlayers: SQL error calculating total damage", e);
        }
        return total;
    }

    public void addDanhHieu(int id) throws IOException {
        for (MyDanhHieu myDanhHieu : danh_hieu) {
            if (myDanhHieu.id == id) {
                return;
            }
        }
        DanhHieu template = DanhHieu.getTemplate(id);
        if (template != null) {
            MyDanhHieu newDanhHieu = new MyDanhHieu();
            newDanhHieu.id = id;
            newDanhHieu.isUsed = false;
            danh_hieu.add(newDanhHieu);
            String notice = template.name + " (Đã mở khóa)";
            Chat.send_chat(this, "Danh hiệu", notice, false);
        }
    }

    private static final int COOLDOWN_MINUTES = 15;

    public boolean canReceiveKillPoint(int targetId) {
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT last_kill_time FROM kill_cooldown WHERE killer_id = ? AND target_id = ?");
            ps.setInt(1, this.id);
            ps.setInt(2, targetId);
            ResultSet rs = ps.executeQuery();
            LocalDateTime now = LocalDateTime.now();
            if (rs.next()) {
                Timestamp lastKillTimeTs = rs.getTimestamp("last_kill_time");
                LocalDateTime lastKillTime = lastKillTimeTs.toLocalDateTime();
                Duration diff = Duration.between(lastKillTime, now);
                if (diff.toMinutes() < COOLDOWN_MINUTES) {
                    rs.close();
                    ps.close();
                    return false;
                }
            }
            rs.close();
            ps.close();
            return true;
        } catch (Exception e) {
            GameLogger.error("Player.canReceiveKillPoint: SQL error checking kill cooldown for " + this.name
                    + " target: " + targetId, e);
            return false;
        }
    }

    public void updateKillCooldown(int targetId) {
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement psUpdate = conn.prepareStatement(
                    "INSERT INTO kill_cooldown (killer_id, target_id, last_kill_time) VALUES (?, ?, NOW()) " +
                            "ON DUPLICATE KEY UPDATE last_kill_time = NOW()");
            psUpdate.setInt(1, this.id);
            psUpdate.setInt(2, targetId);
            psUpdate.executeUpdate();
            psUpdate.close();
        } catch (Exception e) {
            GameLogger.error("Player.updateKillCooldown: SQL error updating kill cooldown for " + this.name
                    + " target: " + targetId, e);
        }
    }

    public synchronized int get_coin() throws IOException {
        if (conn == null) {
            return 0;
        }
        String query = "SELECT `coin` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        int coin = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                coin = rs.getInt("coin");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.get_coin: SQL error getting coins for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.get_coin: SQL error closing resources for " + this.name, e);
            }
        }
        return coin;
    }

    public synchronized int getTongNap() throws IOException {
        if (conn == null) {
            return 0;
        }
        String query = "SELECT `tongnap` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        int tongnap = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                tongnap = rs.getInt("tongnap");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.getTongNap: SQL error getting total recharge for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.getTongNap: SQL error closing resources for " + this.name, e);
            }
        }
        return tongnap;
    }

    public synchronized long getTongTieu() throws IOException {
        if (conn == null) {
            return 0;
        }
        String query = "SELECT `tongtieu` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        long tongtieu = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                tongtieu = rs.getLong("tongtieu");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.getTongTieu: SQL error getting total spent for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.getTongTieu: SQL error closing resources for " + this.name, e);
            }
        }
        return tongtieu;
    }

    public synchronized int get_bua() throws IOException {
        if (conn == null) {
            return 0;
        }
        String query = "SELECT `bua` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        int bua = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                bua = rs.getInt("bua");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.get_bua: SQL error getting charms for " + this.name, e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (st != null)
                    st.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                GameLogger.warn("Player.get_bua: SQL error closing resources for " + this.name, e);
            }
        }
        return bua;
    }

    public int get_conn_id() {
        int accId = -1;
        String sql = "SELECT id FROM accounts WHERE BINARY user = ?";
        try (Connection con = SQL.gI().getCon();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, conn.user);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accId = rs.getInt("id");
                }
            }
        } catch (Exception e) {
            GameLogger.error("Player.get_conn_id: SQL error getting account ID for " + this.name, e);
        }
        return accId;
    }

    public synchronized int getAdmin() throws IOException {
        String query = "SELECT `admin` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        int admin = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                admin = rs.getInt("admin");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.getAdmin: SQL error getting admin status for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.getAdmin: SQL error closing resources for " + this.name, e);
            }
        }
        return admin;
    }

    public synchronized int getKichHoat() throws IOException {
        String query = "SELECT `kichhoat` FROM `accounts` WHERE BINARY `user` = '" + conn.user + "' LIMIT 1;";
        int kichhoat = 0;
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                kichhoat = rs.getInt("kichhoat");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.getKichHoat: SQL error getting activation status for " + this.name, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                GameLogger.warn("Player.getKichHoat: SQL error closing resources for " + this.name, e);
            }
        }
        return kichhoat;
    }

    public synchronized boolean update_coin(int coin_exchange) throws IOException {
        String query = "UPDATE accounts " +
                "SET coin = coin + ? " +
                "WHERE BINARY user = ? " +
                "AND coin + ? >= 0 " +
                "AND coin + ? <= 2000000000";
        try (Connection connection = SQL.gI().getCon();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, coin_exchange);
            ps.setString(2, conn.user);
            ps.setInt(3, coin_exchange);
            ps.setInt(4, coin_exchange);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            GameLogger.error("Player.update_coin: SQL error updating coins for " + this.name, e);
            return false;
        }
    }

    public synchronized boolean update_tong_tieu(long tongtieu) throws IOException {
        String query = "UPDATE accounts " +
                "SET tongtieu = tongtieu + ? " +
                "WHERE BINARY user = ? " +
                "AND tongtieu + ? >= 0";
        try (Connection connection = SQL.gI().getCon();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, tongtieu);
            ps.setString(2, conn.user);
            ps.setLong(3, tongtieu);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            GameLogger.error("Player.update_tong_tieu: SQL error updating total spent for " + this.name, e);
            return false;
        }
    }

    public synchronized boolean update_bua(int value) throws IOException {
        String query = "UPDATE accounts " +
                "SET bua = bua + ? " +
                "WHERE BINARY user = ? " +
                "AND bua + ? >= 0 " +
                "AND bua + ? <= 2000000000";
        try (Connection connection = SQL.gI().getCon();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, value);
            ps.setString(2, conn.user);
            ps.setInt(3, value);
            ps.setInt(4, value);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            GameLogger.error("Player.update_bua: SQL error updating charms for " + this.name, e);
            return false;
        }
    }

    public synchronized void update_TichLuy(long par) {
        if ((((long) par) + this.tichLuy) < 2_000_000_000_000_000L) {
            this.tichLuy += par;
        }
    }

    public synchronized boolean payVnd(long amount) {
        if (amount < 0)
            return false;
        if (this.vnd >= amount) {
            this.vnd -= amount;
            return true;
        }
        return false;
    }

    public synchronized boolean payVang(long amount) {
        if (amount < 0)
            return false;
        if (this.vang >= amount) {
            this.vang -= amount;
            return true;
        }
        return false;
    }

    public synchronized boolean payNgoc(long amount) {
        if (amount < 0)
            return false;
        if (this.kimcuong >= amount) {
            this.kimcuong -= amount;
            return true;
        }
        return false;
    }

    public synchronized boolean payTicket(int amount) {
        if (amount < 0)
            return false;
        if (this.ticket >= amount) {
            this.ticket -= amount;
            return true;
        }
        return false;
    }

    public EffTemplate get_eff(int id) {
        return this.map_eff.get(id);
    }

    public void wear_item(Item_wear it) throws IOException {
        if (this.level < it.template.level) {
            Service.send_box_ThongBao_OK(this, "Chưa đủ level");
            this.use_item_3 = -1;
            return;
        }
        if (it.template.clazz != 0 && this.clazz != it.template.clazz) {
            Service.send_box_ThongBao_OK(this, "Không thể mặc vật phẩm này");
            this.use_item_3 = -1;
            return;
        }
        if (it.valueKichAn != 12) {
            it.typelock = 1;
        }
        byte index_wear = ItemTemplate3.get_it_by_id(it.template.id).typeEquip;
        if (index_wear != -1) {
            Item_wear it_inbag = it;
            item.bag3[it_inbag.index] = null;

            if (it.template.tb2 == 0) {
                if (item.it_body[index_wear] != null) {
                    item.add_item_bag3(item.it_body[index_wear]);
                    item.it_body[index_wear] = null;
                }
                item.it_body[index_wear] = it_inbag;
                it_inbag.index = index_wear;
            } else if (it.template.tb2 == 1) { // trang bi 2
                if (item.second_body[index_wear] != null) {
                    item.add_item_bag3(item.second_body[index_wear]);
                    item.second_body[index_wear] = null;
                }
                item.second_body[index_wear] = it_inbag;
                it_inbag.index = index_wear;
            } else if (it.template.tb2 == 2) { // trang bi 3
                if (item.third_body[index_wear] != null) {
                    item.add_item_bag3(item.third_body[index_wear]);
                    item.third_body[index_wear] = null;
                }
                item.third_body[index_wear] = it_inbag;
                it_inbag.index = index_wear;
            }
        }
        item.update_Inventory(-1, false);
        //
        Service.pet(this, this, false);
        Service.resetAndShowPet(this);
        Service.UpdateInfoMaincharInfo(this);
        //
        this.update_info_to_all();
        this.invalidateStatCache();
        Service.UpdatePvpPoint(this);
        Service.update_PK(this, this, false);
        Service.getThanhTich(this, this);
        Service.Weapon_fashion(this, this, false);
        Service.charWearing(this, this, false);
        this.use_item_3 = -1;
    }

    public void remove_all_second_body_items() throws IOException {
        for (int i = 0; i < item.second_body.length; i++) {
            if (item.second_body[i] != null) {
                Item_wear it = item.second_body[i];
                if (item.able_bag() >= 1) {
                    item.add_item_bag3(it);
                    item.second_body[i] = null;
                } else {
                    Service.send_box_ThongBao_OK(this, "Hành trang không đủ chỗ trống");
                    break;
                }
            }
        }
        item.update_Inventory(-1, false);
        this.invalidateStatCache();
        Service.UpdateInfoMaincharInfo(this);
        Service.charWearing(this, this, false);
        this.update_info_to_all();
    }

    public void remove_all_third_body_items() throws IOException {
        for (int i = 0; i < item.third_body.length; i++) {
            if (item.third_body[i] != null) {
                Item_wear it = item.third_body[i];
                if (item.able_bag() >= 1) {
                    item.add_item_bag3(it);
                    item.third_body[i] = null;
                } else {
                    Service.send_box_ThongBao_OK(this, "Hành trang không đủ chỗ trống");
                    break;
                }
            }
        }
        item.update_Inventory(-1, false);
        this.invalidateStatCache();
        Service.UpdateInfoMaincharInfo(this);
        Service.charWearing(this, this, false);
        this.update_info_to_all();
    }

    public void update_eff() throws IOException {
        if (list_badge_reduction.size() > 0) {
            long now = System.currentTimeMillis();
            boolean changed = false;
            for (BadgeReduction br : list_badge_reduction) {
                if (now > br.expiry) {
                    total_badge_reductions[br.type] -= br.value;
                    if (total_badge_reductions[br.type] < 0)
                        total_badge_reductions[br.type] = 0;
                    list_badge_reduction.remove(br);
                    changed = true;
                }
            }
            if (changed) {
                this.hp_max_dirty = true;
            }
        }
        try {
            if (map_eff == null || map_eff.isEmpty()) {
                return;
            }
            List<EffTemplate> list_temp = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (EffTemplate eff : map_eff.values()) {
                if (eff != null && eff.time < now) {
                    list_temp.add(eff);
                }
            }
            for (EffTemplate eff : list_temp) {
                map_eff.remove(eff.id);
            }
            for (int i = 0; i < list_temp.size(); i++) {
                EffTemplate eff = list_temp.get(i);
                if (eff == null)
                    continue;
                if (eff.id == 7) {
                    if (conn != null && conn.p != null && conn.p.map != null) {
                        Message m2 = new Message(-71);
                        m2.writer().writeByte(1);
                        m2.writer().writeShort(conn.p.index_map);
                        m2.writer().writeByte(0);
                        m2.writer().writeInt(1); // time
                        conn.p.map.send_msg_all_p(m2, conn.p, true);
                        m2.cleanup();
                        this.update_info_to_all();
                    }
                } else if (eff.id == 19) {
                    if (this.map != null && this.map.template != null && this.map.template.id == 1000) {
                        if (this.pvp_target != null && !this.pvp_target.equals(this) && !this.pvp_accept) {
                            Vgo vgo = new Vgo();
                            vgo.map_go = Map.get_map_by_id(this.id_map_save);
                            if (vgo.map_go != null && vgo.map_go.length > 0 && vgo.map_go[0] != null
                                    && vgo.map_go[0].template != null) {
                                for (int i1 = 0; i1 < vgo.map_go[0].template.npcs.size(); i1++) {
                                    Npc npc_temp = vgo.map_go[0].template.npcs.get(i1);
                                    if (npc_temp != null && "Bản đồ".equals(npc_temp.namegt)) {
                                        vgo.xnew = npc_temp.x;
                                        if (npc_temp.y < 250) {
                                            vgo.ynew = (short) (npc_temp.y + 20);
                                        } else {
                                            vgo.ynew = (short) (npc_temp.y - 40);
                                        }
                                        break;
                                    }
                                }
                                if (vgo.xnew == 0 || vgo.ynew == 0) {
                                    vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                                    vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                                }
                                this.goto_map(vgo);
                            }
                        } else if (this.pvp_target != null && !this.pvp_target.equals(this) && this.pvp_accept) {
                            Pvp.show_table(this);
                            Pvp.start_find(this);
                            Service.send_box_ThongBao_OK(this, "Đối thủ rời đi, bạn quay lại hàng chờ");
                        }
                    }
                } else if (eff.id == 21) {
                    this.update_info_to_all();
                    if (this.map != null && this.map.players != null) {
                        for (int j = 0; j < this.map.players.size(); j++) {
                            Player target = this.map.players.get(j);
                            if (target != null) {
                                Service.charWearing(this, target, false);
                            }
                        }
                    }
                } else if (eff.id == 13) {
                    this.update_info_to_all();
                }
            }
            if (!list_temp.isEmpty()) {
                this.send_skill();
                this.update_info_to_all();
                this.invalidateStatCache();
                list_temp.clear();
            }
        } catch (Exception e) {
            core.GameLogger.error("[ERROR][update_eff] Player: " + this.name + " - " + e);
        }
    }

    public void add_new_eff(int id, int param, long time) {
        this.map_eff.put(id, new EffTemplate(id, param, (time + System.currentTimeMillis())));
        this.invalidateStatCache();
    }

    public void update_die() {
        for (EffTemplate eff : map_eff.values()) {
            if (eff != null && EffTemplate.check_eff_remove_when_die(eff.id)) {
                eff.time = System.currentTimeMillis();
            }
        }
    }

    public void plus_point(Message m2) throws IOException {
        byte index = m2.reader().readByte();
        short value = m2.reader().readShort();
        plus_point(index, value);
    }

    public int plus_point(int index, int value) throws IOException {
        int added = 0;
        if (value > this.pointAttribute) {
            value = this.pointAttribute;
        }
        if (value > 0) {
            int max = Skill_info.STAT_POINT_MAX;
            switch (index) {
                case 0: {
                    if ((this.point1 + value) > max) {
                        value = max - this.point1;
                    }
                    if (value <= 0)
                        return 0;
                    this.point1 += value;
                    added = value;
                    break;
                }
                case 1: {
                    if ((this.point2 + value) > max) {
                        value = max - this.point2;
                    }
                    if (value <= 0)
                        return 0;
                    this.point2 += value;
                    added = value;
                    break;
                }
                case 2: {
                    if ((this.point3 + value) > max) {
                        value = max - this.point3;
                    }
                    if (value <= 0)
                        return 0;
                    this.point3 += value;
                    added = value;
                    break;
                }
                case 3: {
                    if ((this.point4 + value) > max) {
                        value = max - this.point4;
                    }
                    if (value <= 0)
                        return 0;
                    this.point4 += value;
                    added = value;
                    break;
                }
                case 4: {
                    if ((this.point5 + value) > max) {
                        value = max - this.point5;
                    }
                    if (value <= 0)
                        return 0;
                    this.point5 += value;
                    added = value;
                    break;
                }
            }
            this.pointAttribute -= added;
            this.invalidateStatCache();
            this.update_info_to_all();
        } else {
            Service.send_box_ThongBao_OK(this, "Không đủ điểm tiềm năng");
        }
        return added;
    }

    public void reset_point(int type) throws IOException {
        switch (type) {
            case 0: {
                // this.pointAttribute = (short) Level.get_total_point_by_level(this.level);
                // this.point1 = 1;
                // this.point2 = 1;
                // this.point3 = 1;
                // this.point4 = 1;
                // this.point5 = 0;
                // // reset thong thao
                // // int thongThaoTotal = this.pointAttributeThongThao;
                // // for (Option op : this.list_op_thongthao) {
                // // thongThaoTotal += op.getParam();
                // // }
                // int thongThaoTotal = this.thongthao + (this.tu_tien * 10) + (this.tu_ma *
                // 10);
                // this.pointAttributeThongThao = thongThaoTotal;
                // Sửa lỗi mất điểm Bội: Tính tổng điểm hiện có + điểm đã cộng
                int currentThongThaoPool = this.pointAttributeThongThao;
                int spentThongThao = 0;
                if (this.list_op_thongthao != null) {
                    for (Option op : this.list_op_thongthao) {
                        spentThongThao += op.getParam();
                    }
                    this.list_op_thongthao.clear();
                }

                // Công thức tính điểm gốc từ Cấp độ (như ban đầu người chơi yêu cầu)
                int thongThaoBase = this.thongthao + (this.tu_tien * 10) + (this.tu_ma * 10);

                // Đảm bảo trả về tổng điểm lớn nhất (bao gồm cả điểm Bội đã dùng trước đó) và
                // giới hạn ở 160,000 (32,000 * 5)
                this.pointAttributeThongThao = Math.min(160000,
                        Math.max(thongThaoBase, currentThongThaoPool + spentThongThao));
                break;
            }
            case 1: {
                // Hoàn trả điểm tiềm năng dựa trên cấp độ và cấp trùng sinh
                // this.pointAttribute = (this.level - 1) * 10 + (this.reincarnation * 100);
                // this.pointAttribute = (int) (this.level * getPotentialPerLevel()) - 4;
                this.pointAttribute = ((this.level - 1) * getPotentialPerLevel());
                this.point1 = 1;
                this.point2 = 1;
                this.point3 = 1;
                this.point4 = 1;
                this.point5 = 0;
                this.setin4();
                break;
            }
        }
        this.invalidateStatCache();
        this.update_info_to_all();
    }

    public void send_skill() throws IOException {
        list_can_combo.clear();
        Message m = new Message(-7);
        m.writer().writeByte(3);
        m.writer().writeByte(this.skill_point.size());
        for (int i = 0; i < this.skill_point.size(); i++) {
            Skill_info sk_info = this.skill_point.get(i);
            write_data_skill(m.writer(), sk_info);
            if (sk_info.temp.ID < 3 && sk_info.temp.typeSkill == 1 && sk_info.temp.Lv_RQ > -1) {
                list_can_combo.add(sk_info);
            }
        }
        this.send_msg(m);
        m.cleanup();
    }

    public void write_data_skill(DataOutputStream dos, Skill_info sk_info) throws IOException {
        // SPOOFING: Client gốc chỉ hỗ trợ đến Lv30 (hiển thị "Cấp độ tối đa" khi
        // Lv_RQ==30)
        // Với skill Lv30+: gửi Lv_RQ=29 để client hiển thị thanh tiến trình
        // indexSkillInServer và ID giữ nguyên để client nhận diện đúng skill
        short realLv = sk_info.temp.Lv_RQ;
        String nameToSend = sk_info.temp.name;
        byte levelToClient = (byte) realLv;

        if (sk_info.temp.ID >= 2003 && sk_info.temp.ID <= 2063 && sk_info.temp.ID != 2059 && sk_info.temp.ID != 2006
                && sk_info.temp.ID != 2007 && sk_info.temp.ID != 2011 && sk_info.temp.ID != 2012
                && sk_info.temp.ID != 2015 && sk_info.temp.ID != 2021 && sk_info.temp.ID != 2022
                && sk_info.temp.ID != 2027 && sk_info.temp.ID != 2031 && sk_info.temp.ID != 2036
                && sk_info.temp.ID != 2039 && sk_info.temp.ID != 2043 && sk_info.temp.ID != 2048
                && sk_info.temp.ID != 2051 && sk_info.temp.ID != 2054 && sk_info.temp.ID != 2063
                && sk_info.temp.ID != 2055) {
            float pct = sk_info.get_percent() / 10.0f;
            nameToSend = sk_info.temp.name + " (" + realLv + " - " + String.format("%.1f", pct) + "%)";
        } else if (realLv > 25) {
            levelToClient = 1;
            nameToSend = sk_info.temp.name + " (Lv." + realLv + ")";
        }

        dos.writeShort(sk_info.temp.indexSkillInServer);
        dos.writeShort(sk_info.temp.ID);
        dos.writeShort(sk_info.temp.idIcon);
        dos.writeByte(this.map.template.specMap == 4 ? 4 : sk_info.temp.typeSkill);
        dos.writeByte(sk_info.temp.typeBuff);
        dos.writeUTF(nameToSend);
        dos.writeShort(sk_info.get_eff_skill());
        dos.writeShort(sk_info.temp.range);
        //
        dos.writeByte(sk_info.temp.nTarget);
        dos.writeShort(sk_info.temp.rangeLan);
        String info_sk = sk_info.temp.getInfo(sk_info.lvdevil, this.clazz);
        dos.writeInt(f.setInteger(sk_info.get_dame(this)));
        dos.writeShort(sk_info.temp.manaLost);
        dos.writeInt(sk_info.temp.timeDelay);
        dos.writeByte(sk_info.temp.nKick);
        dos.writeUTF(info_sk);
        dos.writeByte(levelToClient); // Gửi level đã spoof (1 thay vì 30+)
        dos.writeShort(sk_info.get_percent());
        dos.writeByte(sk_info.temp.typeDevil);
        //
        dos.writeByte(sk_info.temp.op.size());
        for (int j = 0; j < sk_info.temp.op.size(); j++) {
            dos.writeByte(sk_info.temp.op.get(j).id);
            dos.writeShort(sk_info.temp.op.get(j).getParam());
        }
        dos.writeByte(sk_info.temp.idEffSpec);
        if (sk_info.temp.idEffSpec > 0) {
            dos.writeShort(sk_info.temp.perEffSpec);
            dos.writeShort(sk_info.temp.timeEffSpec);
        }
        dos.writeByte(sk_info.lvdevil);
        dos.writeByte(sk_info.devilpercent);
    }

    public synchronized void update_skill_exp(int index, long exp) throws IOException {
        markDirty();

        // Nếu không phải kỹ năng Haki, áp dụng hệ số nhân EXP
        if (index != 5001 && index != 5002 && index != 5003) {
            exp = (long) (exp * Manager.gI().exp * Skill_info.SKILL_EXP_RATE); // Nhân EXP với hệ số từ Manager
        }
        if (index < 4 || index == 5000 || index == 5001 || index == 5002 || index == 5003
                || (index >= 2003 && index <= 2063 && index != 2059)) {
            Skill_info sk_info = null;
            for (int i = 0; i < this.skill_point.size(); i++) {
                Skill_info temp = this.skill_point.get(i);
                if (temp.temp.ID == index) {
                    sk_info = temp;
                    break;
                }
            }
            if (sk_info != null) {
                if (sk_info.exp > -1) {
                    sk_info.exp += exp;
                    long exp_total = Skill_info.EXP[sk_info.temp.Lv_RQ - 1];
                    if (sk_info.exp >= exp_total && sk_info.temp.Lv_RQ >= this.level
                            && (index < 2003 || index > 2063 || index == 2059)) {
                        // Capped at 100% until player level increases
                        sk_info.exp = exp_total;
                    }
                    if (sk_info.exp >= exp_total) {
                        if (Skill_Template.upgrade_skill(sk_info, this.clazz)) {
                            sk_info.exp -= exp_total;
                            if (sk_info.exp >= exp_total) {
                                sk_info.exp = 1;
                            }
                            this.send_skill_lv_up(sk_info);
                            this.send_skill();
                            this.update_info_to_all();
                        } else {
                            // Max level reached for current data
                            sk_info.exp = exp_total;
                            if (index >= 2003 && index <= 2063 && index != 2059) {
                                this.send_skill();
                            } else {
                                Learn_Skill.send_skill_percent(this, sk_info);
                            }
                        }
                    } else {
                        // Chỉ gửi phần trăm nếu chưa đủ exp lên cấp
                        if (index >= 2003 && index <= 2063 && index != 2059) {
                            this.send_skill();
                        } else {
                            Learn_Skill.send_skill_percent(this, sk_info);
                        }
                    }
                }
            }
        }
    }

    public int get_skill_Lv_RQ(int index) throws IOException {
        Skill_info sk_info = null;
        for (int i = 0; i < this.skill_point.size(); i++) {
            Skill_info temp = this.skill_point.get(i);
            if (temp.temp.ID == index) {
                sk_info = temp;
                break;
            }
        }
        if (sk_info != null) {
            return sk_info.temp.Lv_RQ;
        }
        return -1; // Trả về -1 nếu không tìm thấy kỹ năng
    }

    public void send_skill_lv_up(Skill_info sk_info) throws IOException {
        Message m = new Message(-28);
        m.writer().writeByte(1);
        write_data_skill(m.writer(), sk_info);
        this.send_msg(m);
        m.cleanup();
    }

    public int get_head() {
        if (this.fusionType != 0) {
            FusionTemplate tmpl = FUSION_TEMPLATES.get(this.fusionType);
            if (tmpl != null && tmpl.head != -1) {
                return tmpl.head;
            }
        }
        if (get_eff(21) != null) { // zoombi
            return 765;
        }
        for (int i = 0; i < this.fashion.size(); i++) {
            if (this.fashion.get(i).is_use) {
                ItemFashion temp = ItemFashion.get_item(this.fashion.get(i).id);
                if (temp != null && temp.mWearing[6] != -1) {
                    return temp.mWearing[6];
                }
            }
        }
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == 108 && this.itfashionP.get(i).is_use) {
                return this.itfashionP.get(i).icon;
            }
        }
        return this.head;
    }

    public int get_hair() {
        if (this.fusionType != 0) {
            FusionTemplate tmpl = FUSION_TEMPLATES.get(this.fusionType);
            if (tmpl != null && tmpl.hair != -1) {
                return tmpl.hair;
            }
        }
        for (int i = 0; i < this.fashion.size(); i++) {
            if (this.fashion.get(i).is_use) {
                ItemFashion temp = ItemFashion.get_item(this.fashion.get(i).id);
                if (temp != null && (temp.mWearing[7] != -1)) {
                    return temp.mWearing[7];
                }
            }
        }
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == 103 && this.itfashionP.get(i).is_use) {
                if (this.itfashionP.get(i).icon == 772) {
                    return (this.itfashionP.get(i).icon + this.tocSuper);
                } else {
                    return this.itfashionP.get(i).icon;
                }
            }
        }
        return this.hair;
    }

    public short getEffHair() {
        int hairId = get_hair();
        switch (hairId) {
            case 714:
                return 200;
            case 716:
                return 202;
            case 717:
                return 203;
            case 771:
                return 205;
            case 775:
                return 209;
            case 776:
                return 210;
            case 777:
                return 211;
            case 772:
                return 206;
            case 773:
                return 207;
            case 774:
                return 208;
            case 1008:
                return 212;
            case 1012:
                return 213;
            case 1016:
                return 214;
            case 1019:
                return 215;
            case 1026:
                return 217;
            case 1023:
                return 216;
            case 1064:
                return 218;
            default:
                return -1;
        }
    }

    public short getEffFashion() {
        for (int i = 0; i < this.fashion.size(); i++) {
            if (this.fashion.get(i).is_use) {
                int fashionId = this.fashion.get(i).id;
                switch (fashionId) {
                    case 112:
                        return 313;
                    case 113:
                        return 314;
                    case 114:
                        return 315;
                    case 115:
                        return 316;
                    case 116:
                        return 317;
                    case 117:
                        return 318;
                    case 109:
                        return 312;
                    case 64:
                        return 319;
                    case 124:
                        return 304;
                    case 17:
                        return 323;
                    case 80:
                        return 308;
                    case 102:
                        return 324;
                    case 103:
                        return 329;
                }
                break;
            }
        }
        return -1; // Nếu không tìm thấy fashion đang được sử dụng
    }

    public void update_itfashionP(ItemFashionP temp_new, int category) throws IOException {
        temp_new.is_use = true;
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == category
                    && !this.itfashionP.get(i).equals(temp_new)) {
                this.itfashionP.get(i).is_use = false;
            }
        }
        if (this.myDisciple != null) {
            for (int i = 0; i < this.myDisciple.itfashionP.size(); i++) {
                ItemFashionP f = this.myDisciple.itfashionP.get(i);
                if (f.category == category && f.id == temp_new.id && f.is_use) {
                    f.is_use = false;
                    for (int j = 0; j < map.players.size(); j++) {
                        Service.charWearing(this.myDisciple, map.players.get(j), false);
                    }
                    break;
                }
            }
        }
        update_info_to_all();
    }

    public ItemFashionP check_itfashionP(int id, int type) {
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == type && this.itfashionP.get(i).id == id) {
                return this.itfashionP.get(i);
            }
        }
        return null;
    }

    public ItemFashionP2 check_fashion(int id) {
        for (int i = 0; i < this.fashion.size(); i++) {
            if (this.fashion.get(i).id == id) {
                return this.fashion.get(i);
            }
        }
        return null;
    }

    public void update_fashionP2(ItemFashionP2 temp_new) throws IOException {
        temp_new.is_use = true;
        for (int i = 0; i < this.fashion.size(); i++) {
            if (!this.fashion.get(i).equals(temp_new)) {
                this.fashion.get(i).is_use = false;
            }
        }
        if (this.myDisciple != null) {
            for (int i = 0; i < this.myDisciple.fashion.size(); i++) {
                ItemFashionP2 f = this.myDisciple.fashion.get(i);
                if (f.id == temp_new.id && f.is_use) {
                    f.is_use = false;
                    for (int j = 0; j < map.players.size(); j++) {
                        Service.charWearing(this.myDisciple, map.players.get(j), false);
                    }
                    break;
                }
            }
        }
        this.update_info_to_all();
    }

    public short[] get_fashion() {
        if (get_eff(21) != null) {
            return new short[] { -1, -2, -1, 766, -1, 767, 765, -2 };
        }

        short[] result = null;
        for (int i = 0; i < this.fashion.size(); i++) {
            if (this.fashion.get(i).is_use) {
                ItemFashion temp = ItemFashion.get_item(this.fashion.get(i).id);
                if (temp != null) {
                    result = new short[temp.mWearing.length];
                    for (int j = 0; j < temp.mWearing.length; j++) {
                        result[j] = temp.mWearing[j];
                    }

                    // Kiểm tra id của trang phục và thiết lập phần tử đầu tiên của result dựa trên
                    // clazz
                    if (this.fashion.get(i).id == 127) {
                        switch (this.clazz) {
                            case 1:
                                result[0] = -1;
                                break;
                            case 2:
                                result[0] = 804;
                                break;
                            case 3:
                                result[0] = 805;
                                break;
                            case 4:
                                result[0] = 803;
                                break;
                            case 5:
                                result[0] = 806;
                                break;
                        }
                    }
                    break;
                }
            }
        }

        if (result != null && result[0] == -1) {
            Item_wear weapon = this.item.it_body[0];
            if (weapon == null) {
                weapon = this.item.second_body[0];
            }
            if (weapon == null) {
                weapon = this.item.third_body[0];
            }

            if (weapon != null && weapon.template != null) {
                result[0] = weapon.template.part;
            }
        }

        // [FUSION PRIORITY] Phủ các part từ Fusion Template lên trên lớp thời trang
        // hiện tại
        if (this.fusionType != 0) {
            FusionTemplate tmpl = FUSION_TEMPLATES.get(this.fusionType);
            if (tmpl != null && tmpl.fashion != null) {
                if (result == null) {
                    result = new short[tmpl.fashion.length];
                    for (int i = 0; i < tmpl.fashion.length; i++) {
                        result[i] = tmpl.fashion[i];
                    }
                } else {
                    for (int i = 0; i < tmpl.fashion.length && i < result.length; i++) {
                        if (tmpl.fashion[i] != -1) {
                            result[i] = tmpl.fashion[i];
                        }
                    }
                }
            }
        }

        return result;
    }

    public void check_and_remove_expired_fashion() throws IOException {
        long currentTime = System.currentTimeMillis();

        // Danh sách chứa các thời trang hết hạn
        List<ItemFashionP2> expiredFashions = new ArrayList<>();

        // Duyệt qua tất cả thời trang
        for (int i = 0; i < this.fashion.size(); i++) {
            ItemFashionP2 fashionItem = this.fashion.get(i);
            // Kiểm tra xem thời trang có hết hạn không
            if (fashionItem.expirationTime != -1 && fashionItem.expirationTime < currentTime) {
                expiredFashions.add(fashionItem); // Thêm thời trang hết hạn vào danh sách
            }
        }

        // Xóa các thời trang hết hạn
        for (int i = 0; i < expiredFashions.size(); i++) {
            ItemFashionP2 expired = expiredFashions.get(i);
            expired.is_use = false;
            this.fashion.remove(expired); // Xóa khỏi danh sách
        }

        // Kiểm tra xem có thực sự xóa được không
        // core.GameLogger.info("Expired Fashions removed: " + expiredFashions.size());
        // Cập nhật lại trang phục cho tất cả người chơi
        for (int j = 0; j < map.players.size(); j++) {
            Player p0 = map.players.get(j);
            Service.charWearing(this, p0, false); // Cập nhật lại trang phục cho người chơi
        }
    }

    public void remove_hairf() throws IOException {
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == 103) {
                this.itfashionP.get(i).is_use = false;
            }
        }
        for (int i = 0; i < map.players.size(); i++) {
            Player p0 = map.players.get(i);
            Service.charWearing(this, p0, false);
        }
    }

    public void remove_fashion() throws IOException {
        for (int i = 0; i < this.fashion.size(); i++) {
            this.fashion.get(i).is_use = false;
        }
        for (int i = 0; i < map.players.size(); i++) {
            Player p0 = map.players.get(i);
            Service.charWearing(this, p0, false);
        }
    }

    public void remove_headf() throws IOException {
        for (int i = 0; i < this.itfashionP.size(); i++) {
            if (this.itfashionP.get(i).category == 108) {
                this.itfashionP.get(i).is_use = false;
            }
        }
        for (int i = 0; i < map.players.size(); i++) {
            Player p0 = map.players.get(i);
            Service.charWearing(this, p0, false);
        }
    }

    public void change_new_date() {
        if (this.isdie || this.clone) {
            return;
        }
        DateTime now = DateTime.now();
        if (!Util.is_same_day(now, date)) {
            date = now;
            time_ship = 0;
            time_nvl = 0;
            ArchiDaily.getRd(this);

            // Reset dữ liệu tuần cá nhân (chỉ khi chủ nhật 0h)
            if (now.getDayOfWeek() == DateTimeConstants.SUNDAY && now.getHourOfDay() == 0) {
                this.wanted_price = 0;
                this.pvppoint = 0;
                this.beri0 = 0;
            }
        }
    }

    public static void resetAllPlayersPoints() {
        String query = "UPDATE `players` SET `pvppoint` = 0, `wanted_point` = 0, `beri0` = 0;";
        try (Connection connection = SQL.gI().getCon();
                PreparedStatement ps = connection.prepareStatement(query)) {
            int result = ps.executeUpdate();
            if (result > 0) {
                core.GameLogger.info("Reset điểm tuần Chủ Nhật (DB).");
            }
        } catch (SQLException e) {
            GameLogger.error("Player.resetAllPlayersPoints: SQL error resetting Sunday points", e);
        }
    }

    public Skill_info get_skill_temp(int idSkill) {
        for (int i = 0; i < this.skill_point.size(); i++) {
            if (this.skill_point.get(i).temp.ID == idSkill) {
                return this.skill_point.get(i);
            }
        }
        return null;
    }

    public static int getFruitIdBySkillIdx(int index) {
        switch (index) {
            // Mera (4032)
            case 475:
            case 476:
            case 478:
                return 4032;
            // Gomu (4033)
            case 477:
            case 479:
            case 480:
                return 4033;
            // Ushi/Chopper (4034)
            case 481:
            case 482:
            case 483:
                return 4034;
            // Smoke (4088)
            case 484:
            case 485:
            case 486:
                return 4088;
            // Ushi/Bulls (4090)
            case 512:
            case 513:
            case 514:
                return 4090;
            // Paint (4091)
            case 515:
            case 516:
            case 517:
                return 4091;
            // Suna (4093)
            case 518:
            case 520:
            case 521:
                return 4093;
            // Hie (4092)
            case 519:
            case 522:
            case 523:
                return 4092;
            // Goro (4160)
            case 524:
            case 525:
            case 526:
            case 527:
                return 4160;
            // Magu (4161)
            case 528:
            case 529:
            case 530:
            case 531:
                return 4161;
            // Falcon (4220)
            case 533:
            case 534:
            case 535:
                return 4220;
            // Leopa/Luci (4219)
            case 536:
            case 537:
            case 538:
                return 4219;
            // Gura (4240)
            case 539:
            case 540:
            case 541:
            case 542:
                return 4240;
            // Dice (4317)
            case 543:
            case 544:
            case 545:
                return 4317;
            // Wax (4316)
            case 546:
            case 547:
            case 548:
                return 4316;
            // Kilo (4318)
            case 549:
            case 550:
            case 551:
                return 4318;
            // Dark (4427)
            case 656:
            case 657:
            case 658:
            case 659:
                return 4427;
            // Light (4684)
            case 761:
            case 762:
            case 763:
            case 764:
                return 4684;
            default:
                return 0;
        }
    }

    public void get_skill_taq_new(int id) throws IOException {
        if (id > 0 && id < 1000) {
            id += 4000;
        }
        // 1. Identify current fruits (including legacy ones)
        List<Integer> fruitList = new ArrayList<>();
        for (Skill_info sk : this.skill_point) {
            if (sk.temp == null)
                continue;
            int skFruitId = sk.fruitId;
            if (skFruitId <= 0) {
                skFruitId = Player.getFruitIdBySkillIdx(sk.temp.indexSkillInServer);
            }

            if (skFruitId > 0) {
                if (!fruitList.contains(skFruitId)) {
                    fruitList.add(skFruitId);
                }
            } else if (sk.temp.ID > 2000
                    && !(sk.temp.indexSkillInServer >= 660 && sk.temp.indexSkillInServer <= 685)
                    && !(sk.temp.indexSkillInServer >= 800 && sk.temp.indexSkillInServer <= 870)) {
                // Legacy fruit logic: identifying skills that act like fruits but aren't mapped
                if (!fruitList.contains(-1)) {
                    fruitList.add(-1);
                }
            }
        }

        // 2. Manage limit and removal
        List<Skill_info> list_to_remove = new ArrayList<>();
        if (fruitList.contains(id)) {
            // Strict duplicate check: always block if already have this fruit
            Service.send_box_ThongBao_OK(this, "Bạn đã sở hữu trái ác quỷ này rồi!");
            return;
        }

        while (fruitList.size() >= (this.levelAwaken + 1)) {
            // Over limit or limit reached, remove oldest fruit until we are under the limit
            int fruitToRemove = fruitList.remove(0);
            for (Skill_info sk : this.skill_point) {
                if (sk.temp == null)
                    continue;
                int skFruitId = sk.fruitId;
                if (skFruitId <= 0) {
                    skFruitId = Player.getFruitIdBySkillIdx(sk.temp.indexSkillInServer);
                }

                if (fruitToRemove == -1) {
                    // Remove legacy skills
                    if (skFruitId <= 0 && sk.temp.ID > 2000
                            && !(sk.temp.indexSkillInServer >= 660 && sk.temp.indexSkillInServer <= 685)
                            && !(sk.temp.indexSkillInServer >= 800 && sk.temp.indexSkillInServer <= 870)) {
                        Learn_Skill.remove_skill(this, sk);
                        list_to_remove.add(sk);
                    }
                } else if (skFruitId == fruitToRemove) {
                    // Remove skills belonging to the target fruit
                    Learn_Skill.remove_skill(this, sk);
                    list_to_remove.add(sk);
                }
            }
        }
        this.skill_point.removeAll(list_to_remove);
        list_to_remove.clear(); // Will reuse this list for adding new skills
        switch (id) {
            case 4032: {
                int[] id_ = new int[] { 478, 476, 475 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4033: {
                int[] id_ = new int[] { 480, 479, 477 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4034: {
                int[] id_ = new int[] { 483, 482, 481 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4088: {
                int[] id_ = new int[] { 484, 485, 486 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4090: {
                int[] id_ = new int[] { 514, 513, 512 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4091: {
                int[] id_ = new int[] { 517, 516, 515 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4092: {
                int[] id_ = new int[] { 523, 522, 519 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4093: {
                int[] id_ = new int[] { 521, 520, 518 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4160: {
                int[] id_ = new int[] { 527, 526, 525, 524 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4161: {
                int[] id_ = new int[] { 531, 530, 529, 528 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4219: {
                int[] id_ = new int[] { 538, 537, 536 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4220: {
                int[] id_ = new int[] { 535, 534, 533 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4240: {
                int[] id_ = new int[] { 542, 541, 539, 540 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4316: {
                int[] id_ = new int[] { 548, 547, 546 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4317: {
                int[] id_ = new int[] { 545, 544, 543 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4318: {
                // String[] name_ = new String[]{"Thần hộ thể", "Tăng trọng", "Sức nặng ngàn
                // cân"};
                int[] id_ = new int[] { 551, 550, 549 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4427: {
                // String[] name_ = new String[]{"Dòng chảy ma pháp", "Vòng xoáy ma pháp", "Giải
                // phóng", "Xoáy đen"};
                int[] id_ = new int[] { 656, 657, 658, 659 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
            case 4684: { // trai anh sang
                // String[] name_ = new String[] { "Thiên quang phá diệt", "Cực quang phong ấn",
                // "Quang vũ thực tâm", "Quang minh hoá thân" };
                int[] id_ = new int[] { 761, 762, 763, 764 };
                for (int i = 0; i < id_.length; i++) {
                    Skill_info sk_add = new Skill_info();
                    sk_add.exp = 0;
                    sk_add.temp = Skill_Template.get_temp(id_[i], sk_add.exp);
                    if (sk_add.temp != null) {
                        list_to_remove.add(sk_add);
                    }
                }
                break;
            }
        }
        for (Skill_info sk : list_to_remove) {
            sk.fruitId = id;
        }
        this.skill_point.addAll(list_to_remove);
        list_to_remove.clear();
        this.send_skill();
        this.update_info_to_all();
    }

    public void update_info_to_all() throws IOException {
        if (this.map == null) {
            return;
        }
        Service.Main_char_Info(this);
        Service.getThanhTich(this, this);
        Service.update_PK(this, this, false);
        Service.pet(this, this, false);
        for (int i = 0; i < this.map.players.size(); i++) {
            Player p0 = this.map.players.get(i);
            if (p0 == null || p0.conn == null) {
                continue;
            }
            if (p0.index_map != this.index_map) {
                this.map.send_char_in4_inmap(p0, this.index_map);
                Service.getThanhTich(this, p0);
                Service.update_PK(this, p0, false);
                Service.pet(this, p0, false);
            }
        }
    }

    public void update_info_all_to_me() throws IOException {
        if (this.map == null) {
            return;
        }
        // Cập nhật thông tin của tất cả người chơi (p0) cho người chơi hiện tại (this)
        for (int i = 0; i < this.map.players.size(); i++) {
            Player p0 = this.map.players.get(i);
            if (p0 == null || p0.conn == null) {
                continue;
            }
            if (p0.index_map != this.index_map) {
                // Gửi thông tin của p0 cho người chơi hiện tại
                this.map.send_char_in4_inmap(this, p0.index_map);
                Service.getThanhTich(p0, this);
                Service.update_PK(p0, this, false);
                Service.pet(p0, this, false);
            }
        }
    }

    public int getNumPassive() {
        return 6;
    }

    public ItemBoatP check_itboat(int id) {
        for (int i = 0; i < this.itemboat.size(); i++) {
            if (this.itemboat.get(i).id == id) {
                return this.itemboat.get(i);
            }
        }
        return null;
    }

    public void update_new_part_boat(ItemBoatP temp_new) {
        byte type_boat_new = ItemBoat.get_item(temp_new.id).type;
        for (int i = 0; i < this.itemboat.size(); i++) {
            if (!this.itemboat.get(i).equals(temp_new)
                    && type_boat_new == ItemBoat.get_item(this.itemboat.get(i).id).type) {
                this.itemboat.get(i).is_use = false;
            }
        }
    }

    public short[] get_part_boat() {
        short[] result = new short[] { 0, 1, 2, 3 };
        for (int i = 0; i < this.itemboat.size(); i++) {
            if (this.itemboat.get(i).is_use) {
                ItemBoat temp = ItemBoat.get_item(this.itemboat.get(i).id);
                result[temp.type] = temp.idimg;
            }
        }
        return result;
    }

    public short get_hat() {
        short[] fashion = this.get_fashion();
        Item_wear it_w = this.item.it_body[1];
        if (!this.is_show_hat || it_w == null) {
            return -1;
        } else if (fashion != null && fashion[1] != -1) {
            return fashion[1];
        } else {
            return ItemTemplate3.get_it_by_id(it_w.template.id).part;
        }
    }

    public byte get_index_full_set() {
        return get_index_full_set(this.item != null ? this.item.it_body : null);
    }

    public byte get_index_full_set(Item_wear[] items) {
        if (items == null) {
            return 0;
        }
        int[] milestones = { 11, 20, 40, 60, 80, 100 };
        for (int i = milestones.length - 1; i >= 0; i--) {
            int count = 0;
            for (int j = 0; j < 6; j++) {
                if (j >= items.length)
                    break;
                Item_wear it = items[j];
                if (it != null && it.levelup >= milestones[i]) {
                    count++;
                }
            }
            if (count >= 6) {
                return (byte) i;
            }
        }
        return 0;
    }

    public String getNameShow() {
        if (this.name == null)
            return "";
        String rawName = core.Util.getRawName(this.name);
        String suffix = this.reincarnation > 0 ? " " + core.Util.toRoman(this.reincarnation) : "";
        int index1 = get_index_full_set(this.item != null ? this.item.it_body : null);
        int index2 = get_index_full_set(this.item != null ? this.item.second_body : null);
        int index3 = get_index_full_set(this.item != null ? this.item.third_body : null);

        int countFullSet = 0;
        if (index1 == 5)
            countFullSet++;
        if (index2 == 5)
            countFullSet++;
        if (index3 == 5)
            countFullSet++;

        if (countFullSet == 3) {
            return "「Độc Chủ」" + rawName + suffix;
        } else if (countFullSet == 2) {
            return "「Chí Tôn」" + rawName + suffix;
        } else if (countFullSet == 1) {
            if (index1 == 5)
                return "「Bá Vương」" + rawName + suffix;
            if (index2 == 5)
                return "「Chiến Thần」" + rawName + suffix;
            if (index3 == 5)
                return "「Vô Song」" + rawName + suffix;
        }
        return rawName + suffix;
    }

    public boolean hasFullSet16() {
        int count = 0;
        for (int i = 0; i < 6; i++) {
            Item_wear it = this.item.it_body[i];
            if (it != null && it.levelup >= 16) {
                count++;
            }
        }
        return count >= 6;
    }

    public int getFullSetBonusPercent() {
        int[] bonuses = { 5, 10, 15, 20, 30 }; // U, SS, SR, SSS, SSR
        int totalBonus = 0;
        for (int color = bonuses.length - 1; color >= 0; color--) {
            int count = 0;
            for (Item_wear it : item.it_body) {
                if (it != null && it.template != null && it.template.color == color) {
                    count++;
                }
            }
            if (count >= 8) {
                totalBonus += bonuses[color];
                break;
            }
        }
        int countSecond = 0;
        for (Item_wear it : item.second_body) {
            if (it != null && it.template != null && it.template.color == 8) {
                countSecond++;
            }
        }
        if (countSecond >= 8) {
            totalBonus += 10;
        }

        int countGR = 0;
        int countNewTier = 0;
        for (Item_wear it : item.it_body) {
            if (it != null && it.template != null && it.template.color == 8 && it.template.tb2 == 0) {
                // Tách biệt hoàn toàn: Cái nào ra cái đó
                if (it.template.id >= 144 && it.template.id <= 153) {
                    countNewTier++;
                } else {
                    countGR++;
                }
            }
        }

        // Xét Bonus New Tier trước
        if (countNewTier >= 8) {
            totalBonus += 75;
        } else if (countNewTier >= 6) {
            totalBonus += 55;
        }

        // Xét Bonus GR cũ riêng biệt
        if (countGR >= 8) {
            totalBonus += 45;
        } else if (countGR >= 6) {
            totalBonus += 35;
        }
        return totalBonus;
    }

    public short get_percent_mana_use_skill() {
        return 0;
    }

    public void send_kich_an() throws IOException {
        Message m = new Message(57);
        m.writer().writeByte(9); // reset cooldown skill
        m.writer().writeShort(0);
        m.writer().writeShort(this.index_map);
        m.writer().writeByte(0);
        m.writer().writeByte(0);
        m.writer().writeInt(0);
        this.send_msg(m);
        m.cleanup();
    }

    public QuestP get_quest(int id) {
        for (int i = 0; i < this.list_quest.size(); i++) {
            if (this.list_quest.get(i).template.index == id) {
                return this.list_quest.get(i);
            }
        }
        return null;
    }

    public void update_point_pk(int point) throws IOException {
        this.pointPk += point;
        if (this.pointPk < 0) {
            this.pointPk = 0;
        }
        if (this.pointPk > 2_100_000_000) {
            this.pointPk = 2_100_000_000;
        }
        Message m = new Message(-45);
        m.writer().writeInt(this.pointPk);
        m.writer().writeByte(-1); // fake
        this.send_msg(m);
        m.cleanup();
    }

    public void update_num_item_quest(int type, int id_mob, int value) throws IOException {
        QuestP questCheck = null;
        for (int j = this.list_quest.size() - 1; j >= 0; j--) {
            QuestP tempP = this.list_quest.get(j);
            if (tempP.template.statusQuest == 1) {
                for (int k = 0; k < tempP.data.length; k++) {
                    if (tempP.data[k].length == 4 && tempP.data[k][0] == type
                            && tempP.data[k][1] == id_mob && tempP.data[k][3] < tempP.data[k][2]) {
                        questCheck = tempP;
                        break;
                    }
                }
            }
            if (questCheck != null) {
                break;
            }
        }
        if (questCheck != null) {
            boolean finished = true;
            for (int j = 0; j < questCheck.data.length; j++) {
                if (questCheck.data[j][0] == type && questCheck.data[j][1] == id_mob
                        && questCheck.data[j][3] < questCheck.data[j][2]) {
                    questCheck.data[j][3] += value;
                    if (questCheck.data[j][3] >= questCheck.data[j][2]) {
                        questCheck.data[j][3] = questCheck.data[j][2];
                    } else {
                        finished = false;
                    }
                    // update notice quest progress
                    Message mq = new Message(25);
                    mq.writer().writeShort(id_mob);
                    mq.writer().writeByte(questCheck.data[j][0] == 1 ? 1 : 5);
                    mq.writer().writeShort(questCheck.data[j][3]);
                    mq.writer().writeShort(questCheck.data[j][2]);
                    this.send_msg(mq);
                    mq.cleanup();
                } else {
                    if (questCheck.data[j][3] < questCheck.data[j][2]) {
                        finished = false;
                    }
                }
            }
            if (finished) {
                Quest.remove_old_and_send_next(this, questCheck);
                // send notice quest finish
                Message mnext = new Message(-31);
                mnext.writer().writeByte(0);
                mnext.writer().writeUTF("Nhiệm vụ hoàn thành");
                mnext.writer().writeByte(5);
                mnext.writer().writeShort(-1);
                this.send_msg(mnext);
                mnext.cleanup();
            }
        } else {
            for (int j = 0; j < this.list_quest.size(); j++) {
                QuestP tempP = this.list_quest.get(j);
                if (tempP.template.statusQuest == 2) {
                    Quest temp_old_quest = Quest.get_quest(tempP.template.id - 1);
                    if (temp_old_quest.statusQuest == 1 && temp_old_quest.data_quest.length > 0) {
                        for (int i = 0; i < temp_old_quest.data_quest.length; i++) {
                            if (temp_old_quest.data_quest[i][0] == type
                                    && temp_old_quest.data_quest[i][1] == id_mob) {
                                Message mq = new Message(25);
                                mq.writer().writeShort(id_mob);
                                mq.writer().writeByte(temp_old_quest.data_quest[i][0] == 1 ? 1 : 5);
                                mq.writer().writeShort(temp_old_quest.data_quest[i][2]);
                                mq.writer().writeShort(temp_old_quest.data_quest[i][2]);
                                this.send_msg(mq);
                                mq.cleanup();
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    public int get_ticket() {
        return ticket;
    }

    public void update_ticket(int i) {
        this.ticket += i;
        if (this.ticket >= this.get_ticket_max()) {
            this.ticket = (short) this.get_ticket_max();
        }
    }

    public void update_pvp_ticket(int i) {
        this.pvp_ticket += i;
        if (this.pvp_ticket >= this.get_pvp_ticket_max()) {
            this.pvp_ticket = (byte) this.get_pvp_ticket_max();
        }
    }

    public void update_key_boss(int i) {
        this.key_boss += i;
        if (this.key_boss >= this.get_key_boss_max()) {
            this.key_boss = (byte) this.get_key_boss_max();
        }
    }

    public int get_ticket_max() {
        return 100;
    }

    public int get_pvp_ticket_max() {
        return 100;
    }

    public int get_key_boss_max() {
        return 100;
    }

    public int get_pvp_ticket() {
        return this.pvp_ticket;
    }

    public int get_key_boss() {
        return this.key_boss;
    }

    public long getTichLuy() {
        return tichLuy;
    }

    public void update_pvpPoint(int i) {
        this.pvppoint += i;
        if (this.pvppoint < 0) {
            this.pvppoint = 0;
        }
    }

    public int get_pvpPoint() {
        return pvppoint;
    }

    public ItemBag47 get_daHanhTrinh(int cat) {
        int index = -1;
        for (int i = 0; i < HanhTrinh.LANG.length; i++) {
            if (HanhTrinh.LANG[i] == cat) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            for (int i = 0; i < this.daHanhTrinh.size(); i++) {
                if (this.daHanhTrinh.get(i).category == index
                        && this.daHanhTrinh.get(i).quant == 1) {
                    return this.daHanhTrinh.get(i);
                }
            }
        }
        return null;
    }

    public int get_icon_daHanhTrinh(int cat) {
        for (int i = 0; i < this.daHanhTrinh.size(); i++) {
            if (this.daHanhTrinh.get(i).category == cat && this.daHanhTrinh.get(i).quant == 1) {
                return ItemTemplate4.get_it_by_id(this.daHanhTrinh.get(i).id).icon;
            }
        }
        return -1;
    }

    public List<ItemBag47> get_list_daHanhTrinh_total(int cat) {
        int index = -1;
        for (int i = 0; i < HanhTrinh.LANG.length; i++) {
            if (HanhTrinh.LANG[i] == cat) {
                index = i;
                break;
            }
        }
        List<ItemBag47> result = new ArrayList<>();
        if (index > -1) {
            for (int i = 0; i < this.daHanhTrinh.size(); i++) {
                if (this.daHanhTrinh.get(i).category == index
                        && this.daHanhTrinh.get(i).quant == 0) {
                    result.add(this.daHanhTrinh.get(i));
                }
            }
        }
        return result;
    }

    public void update_wanted_point(int i) {
        long value = this.wanted_price;
        if ((value + i) <= 9_999_999_999L) {
            this.wanted_price += i;
            if (this.wanted_price < 0) {
                this.wanted_price = 0;
            }
        }
    }

    public long get_wanted_point() {
        return this.wanted_price;
    }

    public int get_pet() {
        Item_wear item = this.item.second_body[6];
        if (item != null) {
            switch (item.template.id) {
                case 11010:
                    return 7;
                case 11011:
                    return 8;
                case 11012:
                    return 9;
                case 11013:
                    return 10;
                case 11014:
                    return 11;
                case 11015:
                    return 4;
                case 11002:
                    return 12;
                case 11003:
                    return 13;
                case 11004:
                    return 14;
                case 11005:
                    return 15;
                case 11006:
                    return 16;
                case 11007:
                    return 17;
                case 11008:
                    return 18;
                case 11016:
                    return 19;
                case 11017:
                    return 20;
                case 11018:
                    return 21;
                case 11019:
                    return 22;
                case 11080:
                    return 23;
                case 11081:
                    return 24;
                case 11082:
                    return 25;
                default:
                    return -1; // Trả về -1 nếu không khớp ID
            }
        }
        return -1; // Trả về -1 nếu không có item
    }

    public void updateArchiDaily(int id) {
        for (int i = 0; i < this.archiDaily.length; i++) {
            if ((this.archiDaily[i].type == 0 || this.archiDaily[i].type == 1)
                    && this.archiDaily[i].id == id) {
                this.archiDaily[i].num++;
            }
        }
        boolean allCompleted = true;
        for (int i = 0; i < this.archiDaily.length; i++) {
            if (this.archiDaily[i].type != -1) {
                if (this.archiDaily[i].type != 1 && this.archiDaily[i].type != 2) {
                    allCompleted = false;
                    break;
                }
            }
        }
        if (allCompleted) {
            for (int i = 0; i < this.archiDaily.length; i++) {
                if (this.archiDaily[i].type == -1) {
                    this.archiDaily[i].type = 0;
                }
            }
        }
    }

    public MyArchiDaily getArchiDaily(int id) {
        for (int i = 0; i < this.archiDaily.length; i++) {
            if ((this.archiDaily[i].type == 0 || this.archiDaily[i].type == 1)
                    && this.archiDaily[i].id == id) {
                return this.archiDaily[i];
            }
        }
        return null;
    }

    public Skill_info getSkillById(int slot) {
        int index = slot - 1;
        if (this.skill_point != null && index >= 0 && index < this.skill_point.size()) {
            return this.skill_point.get(index);
        }
        return null;
    }

    public synchronized void update_hp(long par) {
        markDirty();

        if (this.isclose) {
            return;
        }
        long hp_max = this.body.get_hp_max(true);
        this.hp += par;
        if (this.hp > hp_max) {
            this.hp = hp_max;
        }
        if (this.hp <= 0) {
            this.hp = 0;
            if (!this.isdie) {
                this.isdie = true;
            }
        }
        if (this.map != null) {
            Message m = new Message(-83);
            try {
                m.writer().writeShort(this.index_map);
                m.writer().writeByte(0);
                m.writer().writeInt(f.setInteger(hp_max));
                m.writer().writeInt(f.setInteger(this.hp));
                m.writer().writeInt(f.setInteger(par));
                m.writer().writeInt(f.setInteger(this.body.get_mp_max(true)));
                m.writer().writeInt(f.setInteger(this.mp));
                m.writer().writeInt(0);
                this.map.send_msg_all_p(m, this, true);
            } catch (IOException e) {
                core.GameLogger.error("Player.update_hp: Error sending HP update message", e);
            }
        }
    }

    public synchronized void update_mp(long par) {
        markDirty();

        if (this.isclose) {
            return;
        }
        long mp_max = this.body.get_mp_max(true);
        this.mp += par;
        if (this.mp > mp_max) {
            this.mp = mp_max;
        }
        if (this.mp < 0) {
            this.mp = 0;
        }
        if (this.map != null) {
            Message m = new Message(-83);
            try {
                m.writer().writeShort(this.index_map);
                m.writer().writeByte(0);
                m.writer().writeInt(f.setInteger(this.body.get_hp_max(true)));
                m.writer().writeInt(f.setInteger(this.hp));
                m.writer().writeInt(0);
                m.writer().writeInt(f.setInteger(mp_max));
                m.writer().writeInt(f.setInteger(this.mp));
                m.writer().writeInt(f.setInteger(par));
                this.map.send_msg_all_p(m, this, true);
            } catch (IOException e) {
                core.GameLogger.error("Player.update_mp: Error sending MP update message", e);
            }
        }
    }

    public void send_msg(Message m) {
        if (this.conn != null) {
            try {
                this.conn.addmsg(m);
            } catch (Exception e) {
                core.GameLogger.error("Player.send_msg: Error sending message", e);
                // "Fire and Forget": Giao diện không được làm crash logic người chơi
            }
        }
    }

    /**
     * Gửi tin mang tính GIAO DỊCH (Mua bán, Trừ tiền).
     * KHÔNG dùng try-catch để logic gọi hàm có thể bắt được lỗi mạng nếu cần.
     */
    public void add_msg_transaction(Message m) throws IOException {
        if (this.conn != null) {
            this.conn.addmsg(m);
        }
    }

    public void startFusion(int durationSeconds, int type) {
        this.fusionType = type;
        this.fusionExpiry = (durationSeconds == -1) ? -1 : (System.currentTimeMillis() + durationSeconds * 1000L);
        if (this.myDisciple != null) {
            this.preFusionStatus = this.myDisciple.status;
            this.myDisciple.status = 2; // [FIX] Set to 2 (Home) to prevent AI from reappearing
            if (this.myDisciple.map != null) {
                this.myDisciple.map.leave_map(this.myDisciple, 0);
            }
        }
        this.invalidateStatCache();
        try {
            Service.charWearing(this, this, false);
            this.update_info_to_all(); // [CRITICAL] Update stats to client UI
        } catch (Exception e) {
            GameLogger.error("Player.startFusion: Error sending charWearing message", e);
        }
    }

    public void stopFusion() {
        this.fusionType = 0;
        this.fusionExpiry = -1;
        if (this.myDisciple != null && this.preFusionStatus != -1) {
            this.myDisciple.status = (byte) this.preFusionStatus;
            this.preFusionStatus = -1;
        }
        this.invalidateStatCache();
        try {
            Service.charWearing(this, this, false);
            this.update_info_to_all(); // [CRITICAL] Update stats tracking down
        } catch (Exception e) {
            GameLogger.error("Player.stopFusion: Error sending charWearing message", e);
        }
    }

}
