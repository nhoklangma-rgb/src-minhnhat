package map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import activities.ChiemDao;
import activities.Dungeon;
import activities.LenhTruyNa;
import activities.LittleGarden;
import activities.Pvp;
import activities.Trade;
import activities.Wanted;
import activities.WorldWar;
import client.Buff;
import client.Clan;
import client.DanhHieu;
import client.Disciple;
import client.MyDanhHieu;
import client.Player;
import client.Quest;
import client.UseItem;
import client.Wanted_Chest;
import client.playerClone;
import core.BXH;
import core.GameLogger;
import core.Manager;
import core.MenuController;
import core.OceanMazeManager;
import core.Service;
import core.Util;
import io.Message;
import template.DataTemplate;
import template.EffTemplate;
import template.FriendTemp;
import template.GiftBox;
import template.ItemFashionP2;
import template.ItemMap;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.Item_wear;
import template.Map_Hang_Dong;
import template.Map_Lanh_Dia_Bang;
import template.Map_Lien_Tang;
import template.Map_Little_Garden;
import template.Map_Sieu_Lien_Tang;
import template.Map_ThuThachVeThan;
import template.Map_clan_resource;
import template.Map_pvp;
import template.Option_Dame_Msg;
import template.PvpBang;
import template.PvpBangMapFight;
import template.Ship_pet;
import template.Skill_info;
import template.VuonCam;
import tool.ItemDropTool;

public class Map implements Runnable {

    public static final List<Map[]> ENTRYS = new ArrayList<>();
    public static final java.util.Map<Integer, Map[]> MAP_BY_ID = new ConcurrentHashMap<>();
    public List<Mob> mobs_cache = new CopyOnWriteArrayList<>();
    private List<Integer> mob132Indices = new ArrayList<>();
    public static final List<Map> MAP_PLUS = java.util.Collections.synchronizedList(new ArrayList<>());
    // public static int id_eff = 0;
    public static byte weather = -1;
    public static byte weather_level = 1;
    public MapTemplate template;
    public Map_ThuThachVeThan map_ThuThachVeThan;
    public boolean running;
    public CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    public int[] list_mob;
    public byte zone_id;
    public Map_pvp map_pvp;
    public Dungeon map_dungeon;
    public Map_clan_resource clan_resource;
    public Map_Little_Garden map_little_garden;
    public ItemMap[] list_it_map = new ItemMap[1_000];
    public boolean can_PK = true;
    public Map_Lien_Tang map_LienTang;
    public Map_Hang_Dong map_Hang;
    public VuonCam vuonCam;
    public PvpBang pvpBang;
    public PvpBangMapFight pvpBangMapFight;
    public Map_Sieu_Lien_Tang map_SieuLienTang;
    public Map_Lanh_Dia_Bang map_LanhDiaBang;
    public java.util.concurrent.ScheduledFuture<?> future;
    public boolean is_map_plus = false;
    private int empty_tick = 0;
    private int respawn_tick = 0;
    public boolean has_dead_mob = true;
    public boolean isMazeMap = false;
    public String customName = null;
    public java.util.List<MazePortal> mazePortals = new java.util.ArrayList<>();
    public template.Map_Vo_Han_Lien_Tang map_VoHanLienTang;
    private short next_clone_id = -10000;
    public static final int VIEW_DISTANCE_MANHATTAN = 800;

    public Map() {
        this.running = false;
    }

    public synchronized short getNextCloneId() {
        this.next_clone_id--;
        if (this.next_clone_id < -30000) {
            this.next_clone_id = -10000;
        }
        return this.next_clone_id;
    }

    public static boolean is_map_lang(int id) {
        return id == 191 || id == 113 || id == 93 || id == 83 || id == 69 || id == 49 || id == 41
                || id == 33 || id == 25 || id == 17 || id == 9 || id == 1;
    }

    public static boolean is_map_banh_kem(int id) {
        return id == 307 || id == 310 || id == 800;
    }

    public static boolean is_map_treo(int id) {
        return id == 300 || id == 302 || id == 308 || id == 311;
    }

    public static boolean is_boss(int id) {
        return id == 182 || id == 179 || id == 185 || id == 186 || id == 127 || id == 175 || id == 184 || id == 120;
    }

    private void update_mapHang() throws IOException {
        if (this.map_Hang != null) {
            try {
                java.util.Iterator<java.util.Map.Entry<Mob, Long>> iterator = this.map_Hang.listRemoveMap.entrySet()
                        .iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry<Mob, Long> en = iterator.next();
                    if (en.getValue() < System.currentTimeMillis()) {
                        remove_obj(en.getKey().index, 1);
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] Map.update_mapHang error: " + e.getMessage());
            }
            Player p = null;
            synchronized (players) {
                if (players.size() > 0) {
                    p = this.players.get(0);
                }
            }
            if (p != null && p.isdie && !p.isDying) {
                p.isDying = true;
                this.map_Hang.time = System.currentTimeMillis() + 6000L;
                Service.send_time_cool_down(p, this.map_Hang.time, "Hang " + (this.map_Hang.level), 0);
            }
            if (this.map_Hang.time < System.currentTimeMillis()) {
                if (this.map_Hang.level == 20 || !this.map_Hang.gifted) {
                    Vgo vgo = new Vgo();
                    try {
                        Player pFirst;
                        synchronized (players) {
                            pFirst = players.isEmpty() ? null : players.get(0);
                        }
                        if (pFirst != null) {
                            vgo.map_go = Map.get_map_by_id(pFirst.id_map_save);
                        } else {
                            vgo.map_go = Map.get_map_by_id(1);
                        }
                    } catch (Exception ex) {
                        core.GameLogger.error("[Disconnect] Map.update_mapHang error: " + ex.getMessage());
                        vgo.map_go = Map.get_map_by_id(1);
                    }
                    if (vgo.map_go != null && vgo.map_go.length > 0) {
                        vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                        vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                        List<Player> playerList = new ArrayList<>();
                        synchronized (players) {
                            for (int i = 0; i < players.size(); i++) {
                                playerList.add(players.get(i));
                            }
                        }
                        playerList.forEach(l -> {
                            try {
                                l.isDying = false;
                                l.goto_map(vgo);
                            } catch (IOException e) {
                                core.GameLogger.error("[Disconnect] Map.update_mapHang error: " + e.getMessage());
                            }
                        });
                    } else {
                        core.GameLogger.error("Không tìm thấy Map để chuyển về trong update_mapHang");
                    }
                } else {
                    List<Player> listP = new ArrayList<>();
                    listP.addAll(this.players);
                    //
                    int index_mob = -2;
                    // create map
                    Map[] mapTemplateArr = Map.get_map_by_id(301);
                    if (mapTemplateArr == null || mapTemplateArr.length == 0) {
                        core.GameLogger.error("Không tìm thấy Map Template 301 cho update_mapHang");
                        return;
                    }
                    Map mapTemplate = mapTemplateArr[0];
                    Map map_boss = new Map();
                    map_boss.template = mapTemplate.template;
                    map_boss.zone_id = (byte) 0;
                    map_boss.list_mob = new int[0];
                    map_boss.map_Hang = new Map_Hang_Dong();
                    map_boss.map_Hang.time = System.currentTimeMillis() + (60_000L * 10);
                    map_boss.map_Hang.mobs = new ArrayList<>();
                    map_boss.map_Hang.listRemoveMap = new HashMap<>();
                    map_boss.map_Hang.gifted = false;
                    map_boss.map_Hang.levelMob = this.map_Hang.levelMob;
                    map_boss.map_Hang.hpScale = this.map_Hang.hpScale;
                    map_boss.map_Hang.level = this.map_Hang.level + 1;
                    for (int i = 0; i < mapTemplate.list_mob.length; i++) {
                        Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
                        Mob mob_add = new Mob();
                        mob_add.mob_template = temp.mob_template;
                        mob_add.x = temp.x;
                        mob_add.y = temp.y;
                        long hpNow = (long) (this.map_Hang.hpScale * (1 + 0.5 * (this.map_Hang.level - 1)));
                        mob_add.hp_max = hpNow;
                        mob_add.hp = mob_add.hp_max;
                        mob_add.level = this.map_Hang.levelMob;
                        mob_add.isdie = false;
                        mob_add.id_target = -1;
                        mob_add.index = index_mob--;
                        mob_add.map = map_boss;
                        //
                        map_boss.map_Hang.mobs.add(mob_add);
                    }
                    //
                    listP.forEach(p0 -> p0.map.leave_map(p0, 2));
                    listP.forEach(p0 -> {
                        try {
                            p0.map = map_boss;
                            p0.x = 100;
                            p0.y = 300;
                            p0.xold = p0.x;
                            p0.yold = p0.y;
                            p0.map.goto_map(p0);
                            Service.update_PK(p0, p0, true);
                            Service.pet(p0, p0, true);
                            Quest.update_map_have_side_quest(p0, true);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mapHang error: " + e.getMessage());
                        }
                    });
                    map_boss.start_map();
                    Map.add_map_plus(map_boss);
                }
                Map.remove_map_plus(this);
            } else {
                if (!this.map_Hang.gifted && this.map_Hang.mobs.isEmpty()) {
                    this.map_Hang.gifted = true;
                    this.map_Hang.time = System.currentTimeMillis() + 6000L;
                    List<GiftBox> listGift = new ArrayList<>();
                    GiftBox.addGiftBox(listGift, 0, 4, Util.random(1_000_000, 2_000_000)); // beri
                    if (this.map_Hang.level == 10 || this.map_Hang.level == 20) {
                        GiftBox.addGiftBox(listGift, 1018, 4, 1); // trung pet
                    }
                    if (this.map_Hang.level <= 10) {
                        GiftBox.addGiftBox(listGift, 1056, 4, 1); // manh trang bi u
                    } else if (this.map_Hang.level <= 20) {
                        GiftBox.addGiftBox(listGift, 1057, 4, 1); // manh trang bi ss
                    }
                    GiftBox.addGiftBox(listGift, 1021, 4, 1); // da tinh tu
                    GiftBox.addGiftBox(listGift, 1020, 4, 1); // nuoc tinh tu

                    synchronized (players) {
                        for (int i = 0; i < this.players.size(); i++) {
                            Service.send_time_cool_down(this.players.get(i),
                                    this.players.get(i).map.map_Hang.time, "Hang " + (this.map_Hang.level), 0);
                            //
                            Service.send_gift(this.players.get(i), 1, "Phó bản hang động",
                                    "Hoàn thành Hang " + (this.map_Hang.level), listGift, true);
                            this.players.get(i).updateBeri0(1);
                        }
                    }
                }
            }
        }
    }

    private void update_map_ThuThachVeThan() throws IOException {
        if (this.map_ThuThachVeThan != null) {
            try {
                java.util.Iterator<java.util.Map.Entry<Mob, Long>> iterator = this.map_ThuThachVeThan.listRemoveMap
                        .entrySet().iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry<Mob, Long> en = iterator.next();
                    if (en.getValue() < System.currentTimeMillis()) {
                        remove_obj(en.getKey().index, 1);
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] Map.update_mapThuThachVeThan error: " + e.getMessage());
            }
            Player p = null;
            synchronized (players) {
                if (players.size() > 0) {
                    p = this.players.get(0);
                }
            }
            if (p != null && p.isdie && !p.isDying) {
                p.isDying = true;
                this.map_ThuThachVeThan.time_state = System.currentTimeMillis() + 6000L;
                Service.send_time_cool_down(p, this.map_ThuThachVeThan.time_state,
                        "Tầng " + (this.map_ThuThachVeThan.level), 0);
            }
            if (this.map_ThuThachVeThan.time_state < System.currentTimeMillis()) {
                if (this.map_ThuThachVeThan.level == 10 || !this.map_ThuThachVeThan.gifted) {
                    Vgo vgo = new Vgo();
                    vgo.map_go = Map.get_map_by_id(189);
                    if (vgo.map_go != null && vgo.map_go.length > 0) {
                        vgo.xnew = (short) 367;
                        vgo.ynew = (short) 223;
                        List<Player> playerList = new ArrayList<>();
                        synchronized (players) {
                            for (int i = 0; i < players.size(); i++) {
                                playerList.add(players.get(i));
                            }
                        }
                        playerList.forEach(l -> {
                            try {
                                l.isDying = false;
                                l.goto_map(vgo);
                            } catch (IOException e) {
                                core.GameLogger
                                        .error("[Disconnect] Map.update_mapThuThachVeThan error: " + e.getMessage());
                            }
                        });
                    } else {
                        core.GameLogger.error("Không tìm thấy Map ID 189 để chuyển về trong update_mapThuThachVeThan");
                    }
                } else {
                    List<Player> listP = new ArrayList<>();
                    listP.addAll(this.players);
                    //
                    int index_mob = -2;
                    // create map
                    Map[] mapTemplateArr = Map.get_map_by_id(900);
                    if (mapTemplateArr == null || mapTemplateArr.length == 0) {
                        core.GameLogger.error("Không tìm thấy Map Template 900 cho update_mapThuThachVeThan");
                        return;
                    }
                    Map mapTemplate = mapTemplateArr[0];
                    Map map_boss = new Map();
                    map_boss.template = mapTemplate.template;
                    map_boss.zone_id = (byte) 0;
                    map_boss.list_mob = new int[0];
                    map_boss.map_ThuThachVeThan = new Map_ThuThachVeThan();
                    map_boss.map_ThuThachVeThan.time_state = System.currentTimeMillis() + (60_000L * 10);
                    map_boss.map_ThuThachVeThan.mobs = new ArrayList<>();
                    map_boss.map_ThuThachVeThan.listRemoveMap = new HashMap<>();
                    map_boss.map_ThuThachVeThan.gifted = false;
                    map_boss.map_ThuThachVeThan.levelMob = this.map_ThuThachVeThan.levelMob;
                    map_boss.map_ThuThachVeThan.hpScale = this.map_ThuThachVeThan.hpScale;
                    map_boss.map_ThuThachVeThan.level = this.map_ThuThachVeThan.level + 1;
                    for (int i = 0; i < mapTemplate.list_mob.length; i++) {
                        Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
                        Mob mob_add = new Mob();
                        mob_add.mob_template = temp.mob_template;
                        mob_add.x = temp.x;
                        mob_add.y = temp.y;
                        long hpNow = (long) (this.map_ThuThachVeThan.hpScale
                                * (1 + 0.5 * (this.map_ThuThachVeThan.level - 1)));
                        mob_add.hp_max = hpNow;
                        mob_add.hp = mob_add.hp_max;
                        mob_add.level = this.map_ThuThachVeThan.levelMob;
                        mob_add.isdie = false;
                        mob_add.id_target = -1;
                        mob_add.index = index_mob--;
                        mob_add.map = map_boss;
                        //
                        map_boss.map_ThuThachVeThan.mobs.add(mob_add);
                    }
                    //
                    listP.forEach(p0 -> p0.map.leave_map(p0, 2));
                    listP.forEach(p0 -> {
                        try {
                            p0.map = map_boss;
                            p0.x = 100;
                            p0.y = 300;
                            p0.xold = p0.x;
                            p0.yold = p0.y;
                            p0.map.goto_map(p0);
                            Service.update_PK(p0, p0, true);
                            Service.pet(p0, p0, true);
                            Quest.update_map_have_side_quest(p0, true);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mapThuThachVeThan error: " + e.getMessage());
                        }
                    });
                    map_boss.start_map();
                    Map.add_map_plus(map_boss);
                }
                Map.remove_map_plus(this);
            } else {
                if (!this.map_ThuThachVeThan.gifted && this.map_ThuThachVeThan.mobs.isEmpty()) {
                    this.map_ThuThachVeThan.gifted = true;
                    this.map_ThuThachVeThan.time_state = System.currentTimeMillis() + 6000L;
                    List<GiftBox> listGift = new ArrayList<>();
                    GiftBox.addGiftBox(listGift, 0, 4, Util.random(1_000_000, 2_000_000)); // beri
                    GiftBox.addGiftBox(listGift, 452, 4, 1); // sách
                    GiftBox.addGiftBox(listGift, 453, 4, 1); // vỏ ốc
                    GiftBox.addGiftBox(listGift, 13, 7, Util.random(10, 20)); // lông vũ
                    synchronized (players) {
                        for (int i = 0; i < this.players.size(); i++) {
                            Service.send_time_cool_down(this.players.get(i),
                                    this.players.get(i).map.map_ThuThachVeThan.time_state,
                                    "Tầng " + (this.map_ThuThachVeThan.level), 0);
                            //
                            Service.send_gift(this.players.get(i), 1, "Thử thách vệ thần",
                                    "Hoàn thành Tầng " + (this.map_ThuThachVeThan.level), listGift, true);
                        }
                    }
                }
            }
        }
    }

    public static boolean is_map_dungeon(int id) {
        return id >= 167 && id <= 176;
    }

    public static void add_map_plus(Map map_boss) {
        if (!Map.MAP_PLUS.contains(map_boss)) {
            map_boss.is_map_plus = true;
            Map.MAP_PLUS.add(map_boss);
        }
    }

    public static void remove_map_plus(Map map) {
        Map.MAP_PLUS.remove(map);
        map.stop_map();
    }

    public static List<Map> get_map_plus() {
        return Map.MAP_PLUS;
    }

    public static boolean isMapLang(int id) {
        for (int i = 0; i < MenuController.ID_MAP_LANG.length; i++) {
            if (id == MenuController.ID_MAP_LANG[i]) {
                return true;
            }
        }
        return false;
    }

    public void start_map() {
        buildSpecialMobIndices();
        this.running = true;
        this.empty_tick = 0;
        if (this.future != null && !this.future.isCancelled()) {
            this.future.cancel(false);
        }
        this.future = Manager.MAP_POOL.scheduleWithFixedDelay(
                this,
                0,
                1000,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void buildSpecialMobIndices() {
        if (list_mob == null)
            return;
        mob132Indices.clear();
        mobs_cache.clear();
        for (int index : list_mob) {
            Mob m = Mob.ENTRYS.get(index);
            if (m != null) {
                mobs_cache.add(m);
                if (m.mob_template.mob_id == 132) {
                    mob132Indices.add(index);
                }
            }
        }
    }

    public void stop_map() {
        this.running = false;
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
    }

    @Override
    public void run() {
        if (!this.running)
            return;
        try {
            update();

            // Auto-cleanup leaked dynamic maps
            if (this.is_map_plus) {
                if (this.players.isEmpty()) {
                    this.empty_tick++;
                    // Safety: Wait 5 minutes (300 seconds) before deleting an empty dynamic map
                    // to give players time to walk back if they died in a dungeon.
                    if (this.empty_tick > 300) {
                        Map.remove_map_plus(this);
                    }
                } else {
                    this.empty_tick = 0;
                }
            }
        } catch (Throwable t) {
            System.err.println("[Map Error] map=" + (template != null ? template.id : "null") + " zone=" + zone_id
                    + " : " + t.getMessage());
            t.printStackTrace(); // [DEBUG] In chi tiết lỗi để truy vết NPE
            core.GameLogger.error("[Disconnect] Map.update error: " + t.getMessage());
        }
    }

    private void update() throws IOException {
        // 1. LUÔN CẬP NHẬT CÁC LOGIC TOÀN CỤC & DỌN DẸP
        try {
            update_item_map();
            update_map_pvp();
            update_map_Wanted();
        } catch (Exception e) {
            core.GameLogger.error("Map.update Global Logic Error: " + e.getMessage());
        }

        // Cập nhật tiến độ của các phó bản/map động
        try {
            update_map_little_garden();
            update_map_ThuThachVeThan();
            update_mapHang();
            update_mapPvpBangFight();
            update_mapVoHanLienTang();
        } catch (Exception e) {
            core.GameLogger.error("Map.update Dungeon Logic Error: " + e.getMessage());
        }

        if (this.template != null && this.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
            try {
                BossEvent.BossEvent.getInstance().update(this);
            } catch (Exception e) {
                core.GameLogger.error("BossEvent Update Error: " + e.getMessage());
            }
        }

        // 2. CHẾ ĐỘ NGỦ ĐÔNG (SLEEP MAP)
        if (players.isEmpty()) {
            if (!has_dead_mob) {
                return;
            }
            this.respawn_tick++;
            if (this.respawn_tick >= 10) {
                this.respawn_tick = 0;
                try {
                    update_mob();
                } catch (Exception e) {
                    core.GameLogger.error("Map.update_mob Sleep Error: " + e.getMessage());
                }
            }
            return;
        }

        // 3. MAP ĐANG HOẠT ĐỘNG BÌNH THƯỜNG
        this.respawn_tick = 0;
        try {
            update_mob();
        } catch (Exception e) {
            core.GameLogger.error("Map.update_mob Error: " + e.getMessage());
        }
        
        try {
            update_player();
        } catch (Exception e) {
            core.GameLogger.error("Map.update_player Error: " + e.getMessage());
        }
    }

    private void update_map_Wanted() throws IOException {
        if (this.template.id == 119) {
            Player[] p0 = Wanted.get_p_random_waiting();
            if (p0 != null && p0[0] != null && p0[1] != null) {
                p0[0].map.leave_map(p0[0], 2);
                p0[1].map.leave_map(p0[1], 2);
                p0[0].type_pk = -1;
                p0[1].type_pk = -1;
                //
                // create map
                short[] mapID = new short[] { 120, 122, 123 };
                Map[] maptempArr = Map.get_map_by_id(mapID[Util.random(mapID.length)]);
                if (maptempArr == null || maptempArr.length == 0) {
                    core.GameLogger.error("Không tìm thấy Map Template cho update_map_Wanted");
                    return;
                }
                Map maptemp = maptempArr[0];
                Map map_create = new Map();
                map_create.template = maptemp.template;
                map_create.zone_id = (byte) 0;
                map_create.list_mob = new int[0];
                //
                p0[0].map = map_create;
                p0[0].x = 320;
                p0[0].y = 300;
                p0[0].xold = p0[0].x;
                p0[0].yold = p0[0].y;
                p0[0].map.goto_map(p0[0]);
                Service.update_PK(p0[0], p0[0], true);
                Service.pet(p0[0], p0[0], true);
                Quest.update_map_have_side_quest(p0[0], true);
                //
                p0[1].map = map_create;
                p0[1].x = 380;
                p0[1].y = 300;
                p0[1].xold = p0[1].x;
                p0[1].yold = p0[1].y;
                p0[1].map.goto_map(p0[1]);
                Service.update_PK(p0[1], p0[1], true);
                Service.pet(p0[1], p0[1], true);
                Quest.update_map_have_side_quest(p0[1], true);
                //
                map_create.map_pvp = new Map_pvp();
                map_create.map_pvp.time_pvp = 5;
                map_create.map_pvp.status_pvp = 0;
                map_create.map_pvp.num_win_p1 = 0;
                map_create.map_pvp.num_win_p2 = 0;
                map_create.map_pvp.type_map = 2; // map fight truy na

                map_create.start_map();
                Map.add_map_plus(map_create);
            }
        }
    }

    private void update_mapPvpBangFight() throws IOException {
        if (this.pvpBangMapFight != null) {
            this.pvpBangMapFight.time_pvp--;
            if (this.pvpBangMapFight.status_pvp == 0 && this.pvpBangMapFight.time_pvp <= 0) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 0);
                    }
                }
                this.pvpBangMapFight.time_pvp = 5;
                this.pvpBangMapFight.status_pvp = 1;
            } else if (this.pvpBangMapFight.status_pvp == 1 && this.pvpBangMapFight.time_pvp <= 0) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 1);
                        Service.use_potion(players.get(i), 0, players.get(i).body.get_hp_max(true));
                        Service.use_potion(players.get(i), 1, players.get(i).body.get_mp_max(true));
                        players.get(i).isdie = false;
                        Service.send_revive_player(players.get(i));
                    }
                }
                this.pvpBangMapFight.time_pvp = 4;
                this.pvpBangMapFight.status_pvp = 2;
            } else if (this.pvpBangMapFight.status_pvp == 2 && this.pvpBangMapFight.time_pvp <= 0) {
                this.pvpBangMapFight.time_pvp = 60 * 2 + 59; // time
                this.pvpBangMapFight.status_pvp = 3;
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 2);
                        change_flag(players.get(i), -1);
                        Pvp.show_info(players.get(i), this.pvpBangMapFight.time_pvp, 0, 0, 1);
                    }
                }
            }
            if (this.pvpBangMapFight.status_pvp == 3) {
                int numAliveClan1 = 0;
                int numAliveClan2 = 0;
                synchronized (players) {
                    for (int j = 0; j < players.size(); j++) {
                        if (!players.get(j).isdie) {
                            if (players.get(j).clan.equals(this.pvpBangMapFight.clan1)) {
                                numAliveClan1++;
                            } else {
                                numAliveClan2++;
                            }
                        }
                    }
                }
                if (numAliveClan1 == 0 || numAliveClan2 == 0) {
                    this.pvpBangMapFight.status_pvp = 4;
                    this.pvpBangMapFight.time_pvp = 4;
                    synchronized (players) {
                        for (int i = 0; i < players.size(); i++) {
                            change_flag(players.get(i), -1);
                        }
                    }
                    if (numAliveClan1 == 0) {
                        synchronized (players) {
                            for (int j = 0; j < players.size(); j++) {
                                if (players.get(j).clan.equals(this.pvpBangMapFight.clan1)) {
                                    Pvp.pvp_notice(players.get(j), 4); // thua
                                } else {
                                    Pvp.pvp_notice(players.get(j), 3); // thắng
                                }
                            }
                        }
                        PvpBang.notice(this, "Kết thúc - Băng chiến thắng : " + this.pvpBangMapFight.clan2.name
                                + ". Bạn sẽ được đưa về map chờ");
                        if (this.pvpBangMapFight.clan1.map_create != null
                                && this.pvpBangMapFight.clan1.map_create.pvpBang != null) {
                            this.pvpBangMapFight.clan1.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                                    + this.pvpBangMapFight.clan2.name + " - Thua.";
                            this.pvpBangMapFight.clan1.map_create.pvpBang.isWin = false;
                        }
                        if (this.pvpBangMapFight.clan2.map_create != null
                                && this.pvpBangMapFight.clan2.map_create.pvpBang != null) {
                            this.pvpBangMapFight.clan2.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                                    + this.pvpBangMapFight.clan1.name + " - Thắng.";
                            this.pvpBangMapFight.clan2.map_create.pvpBang.isWin = true;
                        }
                    } else {
                        synchronized (players) {
                            for (int j = 0; j < players.size(); j++) {
                                if (players.get(j).clan.equals(this.pvpBangMapFight.clan1)) {
                                    Pvp.pvp_notice(players.get(j), 3); // thắng
                                } else {
                                    Pvp.pvp_notice(players.get(j), 4); // thua
                                }
                            }
                        }
                        PvpBang.notice(this, "Kết thúc - Băng chiến thắng: " + this.pvpBangMapFight.clan1.name
                                + ". Bạn sẽ được đưa về map chờ");
                        if (this.pvpBangMapFight.clan1.map_create != null
                                && this.pvpBangMapFight.clan1.map_create.pvpBang != null) {
                            this.pvpBangMapFight.clan1.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                                    + this.pvpBangMapFight.clan2.name + " - Thắng.";
                            this.pvpBangMapFight.clan1.map_create.pvpBang.isWin = true;
                        }
                        if (this.pvpBangMapFight.clan2.map_create != null
                                && this.pvpBangMapFight.clan2.map_create.pvpBang != null) {
                            this.pvpBangMapFight.clan2.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                                    + this.pvpBangMapFight.clan1.name + " - Thua.";
                            this.pvpBangMapFight.clan2.map_create.pvpBang.isWin = false;
                        }
                    }
                }
            }
            if (this.pvpBangMapFight.status_pvp == 3 && this.pvpBangMapFight.time_pvp <= 0) {
                this.pvpBangMapFight.status_pvp = 4;
                this.pvpBangMapFight.time_pvp = 4;
                PvpBang.notice(this, "Hết thời gian, kết quả hòa, bạn sẽ được đưa về map chờ");
                if (this.pvpBangMapFight.clan1.map_create != null
                        && this.pvpBangMapFight.clan1.map_create.pvpBang != null) {
                    this.pvpBangMapFight.clan1.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                            + this.pvpBangMapFight.clan2.name + " - Hoà.";
                    this.pvpBangMapFight.clan1.map_create.pvpBang.isWin = false;
                }
                if (this.pvpBangMapFight.clan2.map_create != null
                        && this.pvpBangMapFight.clan2.map_create.pvpBang != null) {
                    this.pvpBangMapFight.clan2.map_create.pvpBang.noticeFinished = "Kết Quả pvp với băng "
                            + this.pvpBangMapFight.clan1.name + " - Hoà.";
                    this.pvpBangMapFight.clan2.map_create.pvpBang.isWin = false;
                }
            } else if (this.pvpBangMapFight.status_pvp == 4 && this.pvpBangMapFight.time_pvp == 1) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Service.use_potion(players.get(i), 0, players.get(i).body.get_hp_max(true));
                        Service.use_potion(players.get(i), 1, players.get(i).body.get_mp_max(true));
                        players.get(i).isdie = false;
                        Service.send_revive_player(players.get(i));
                    }
                }
            } else if (this.pvpBangMapFight.status_pvp == 4 && this.pvpBangMapFight.time_pvp <= 0) {
                Vgo vgo = new Vgo();
                Map[] map_go_arr = Map.get_map_by_id(260);
                if (map_go_arr != null && map_go_arr.length > 0) {
                    vgo.map_go = new Map[1];
                    vgo.map_go[0] = map_go_arr[0];
                    vgo.xnew = 530;
                    vgo.ynew = 260;
                    List<Player> playerList = new ArrayList<>();
                    synchronized (players) {
                        for (int i = 0; i < players.size(); i++) {
                            playerList.add(players.get(i));
                        }
                    }
                    playerList.forEach(l -> {
                        try {
                            l.goto_map(vgo);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mapPvpBang error: " + e.getMessage());
                        }
                    });
                } else {
                    core.GameLogger.error("Không tìm thấy Map ID 260 cho update_mapPvpBang");
                }
                this.pvpBangMapFight.status_pvp = 99;
                if (this.pvpBangMapFight.clan1.map_create != null
                        && this.pvpBangMapFight.clan1.map_create.pvpBang != null) {
                    this.pvpBangMapFight.clan1.map_create.pvpBang.state = 3;
                    this.pvpBangMapFight.clan1.map_create.pvpBang.timeState = System.currentTimeMillis() + 5_000;
                }
                if (this.pvpBangMapFight.clan2.map_create != null
                        && this.pvpBangMapFight.clan2.map_create.pvpBang != null) {
                    this.pvpBangMapFight.clan2.map_create.pvpBang.state = 3;
                    this.pvpBangMapFight.clan2.map_create.pvpBang.timeState = System.currentTimeMillis() + 5_000;
                }
            } else if (this.pvpBangMapFight.status_pvp == 99) {
                Map.remove_map_plus(this);
                this.pvpBangMapFight = null;
            }
        }
    }

    private void update_map_little_garden() {
        if (this.map_little_garden != null) {
            for (int i = 0; i < this.map_little_garden.mobs.size(); i++) {
                Mob mob = this.map_little_garden.mobs.get(i);
                if (mob != null) {
                    if (mob.isdie) {
                        if (mob.time_refresh < System.currentTimeMillis()) {
                            mob.isdie = false;
                            mob.hp = mob.hp_max;
                            mob.id_target = -1;
                            //
                            try {
                                Message m_local = new Message(1);
                                m_local.writer().writeByte(1);
                                m_local.writer().writeShort(mob.index);
                                m_local.writer().writeShort(mob.x);
                                m_local.writer().writeShort(mob.y);
                                this.send_msg_all_p_distance(m_local, mob.x, mob.y, false, 15, -1);
                                m_local.cleanup();
                            } catch (IOException e) {
                                core.GameLogger
                                        .error("[Disconnect] Map.update_mapLittleGarden error: " + e.getMessage());
                            }
                        }
                    }
                }
            }
            //
            if ((this.map_little_garden.is_finish || this.map_little_garden.time < System.currentTimeMillis())
                    && !map_little_garden.rewardGiven) {
                int xpWin, xpLose, beriWin, beriLose, rubyWin, rubyLose;
                Clan winnerClan, loserClan;
                if (this.map_little_garden.hp_1 <= 0) {
                    winnerClan = this.map_little_garden.clan2;
                    loserClan = this.map_little_garden.clan1;
                    xpWin = 2000;
                    xpLose = 1000;
                    beriWin = 5000;
                    beriLose = 2000;
                    rubyWin = 500;
                    rubyLose = 200;
                } else if (this.map_little_garden.hp_2 <= 0) {
                    winnerClan = this.map_little_garden.clan1;
                    loserClan = this.map_little_garden.clan2;
                    xpWin = 2000;
                    xpLose = 1000;
                    beriWin = 5000;
                    beriLose = 2000;
                    rubyWin = 500;
                    rubyLose = 200;
                } else {
                    winnerClan = null;
                    loserClan = null;
                    xpWin = xpLose = 1000;
                    beriWin = beriLose = 1000;
                    rubyWin = rubyLose = 50;
                }
                List<GiftBox> giftsWinner = new ArrayList<>();
                List<GiftBox> giftsLoser = new ArrayList<>();
                if (winnerClan != null) {
                    GiftBox.addGiftBoxClan(giftsWinner, -10, xpWin);
                    GiftBox.addGiftBoxClan(giftsWinner, -11, beriWin);
                    GiftBox.addGiftBoxClan(giftsWinner, -12, rubyWin);

                    GiftBox.addGiftBoxClan(giftsLoser, -10, xpLose);
                    GiftBox.addGiftBoxClan(giftsLoser, -11, beriLose);
                    GiftBox.addGiftBoxClan(giftsLoser, -12, rubyLose);
                } else {
                    // Hòa
                    GiftBox.addGiftBoxClan(giftsWinner, -10, xpWin);
                    GiftBox.addGiftBoxClan(giftsWinner, -11, beriWin);
                    GiftBox.addGiftBoxClan(giftsWinner, -12, rubyWin);
                    giftsLoser = giftsWinner;
                }
                List<Player> winnerPlayers = (winnerClan != null)
                        ? winnerClan.members.stream()
                                .map(m -> Map.get_player_by_name_allmap(m.name))
                                .filter(Objects::nonNull)
                                .toList()
                        : new ArrayList<>();

                List<Player> loserPlayers = (loserClan != null)
                        ? loserClan.members.stream()
                                .map(m -> Map.get_player_by_name_allmap(m.name))
                                .filter(Objects::nonNull)
                                .toList()
                        : new ArrayList<>();

                // Gửi quà
                if (winnerClan != null) {
                    Service.distributeClanGiftOnce(winnerClan, winnerPlayers, giftsWinner,
                            "Phó Bản Khổng Lồ", "Phần thưởng thắng cuộc");
                    Service.distributeClanGiftOnce(loserClan, loserPlayers, giftsLoser,
                            "Phó Bản Khổng Lồ", "Phần thưởng thua cuộc");
                } else {
                    // Hòa
                    Service.distributeClanGiftOnce(this.map_little_garden.clan1, winnerPlayers, giftsWinner,
                            "Phó Bản Khổng Lồ", "Phần thưởng hoà cuộc");
                    Service.distributeClanGiftOnce(this.map_little_garden.clan2, loserPlayers, giftsLoser,
                            "Phó Bản Khổng Lồ", "Phần thưởng hoà cuộc");
                }
                map_little_garden.rewardGiven = true;
                this.map_little_garden.time = System.currentTimeMillis() + 10000; // delay 10 giây
            }
            if (map_little_garden.rewardGiven && this.map_little_garden.time <= System.currentTimeMillis()) {
                this.map_little_garden.clan1.map_create = null;
                this.map_little_garden.clan2.map_create = null;
                Vgo vgo = new Vgo();
                vgo.map_go = Map.get_map_by_id(33);
                vgo.xnew = 710;
                vgo.ynew = 320;
                List<Player> playerList = new ArrayList<>();
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        playerList.add(players.get(i));
                    }
                }
                playerList.forEach(l -> {
                    try {
                        l.goto_map(vgo);
                    } catch (IOException e) {
                        core.GameLogger.error("[Disconnect] Map.update_mapLittleGarden error: " + e.getMessage());
                    }
                });
                map_little_garden.rewardGiven = false;
                Map.remove_map_plus(this);
            }
        }
    }

    static long timeUpdateMapPVP = System.currentTimeMillis();

    private void update_map_pvp() throws IOException {
        try {
            if (this.template.id == 1000 && System.currentTimeMillis() - timeUpdateMapPVP > 20000) {
                List<Player> list_pvp_wait = new ArrayList<>();
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Player p0 = players.get(i);
                        if (p0.pvp_target != null && p0.pvp_target.equals(p0)) {
                            list_pvp_wait.add(p0);
                        }
                    }
                }
                if (list_pvp_wait.size() > 0) {
                    Player p_select = list_pvp_wait.get(0);
                    if (!p_select.clone) {
                        Player p_select_2 = playerClone.findPlayerPvP(p_select);

                        list_pvp_wait.remove(0);
                        if (p_select_2 != null) {
                            p_select.pvp_target = p_select_2;
                            p_select_2.pvp_target = p_select;
                            //
                            if (!(p_select.name + " ").equals(p_select_2.name)) {
                                Pvp.find_out_other(p_select, p_select_2);
                                Pvp.find_out_other(p_select_2, p_select);
                            } else {
                                if (p_select_2.pcl != null) {
                                    p_select_2.pcl.dis();
                                }
                                p_select.pvp_target = p_select;
                            }
                        }
                    } else {
                        if (p_select.pcl != null) {
                            p_select.pcl.dis();
                        }
                    }
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] Map.update_mapWaitPvp error: " + e.getMessage());
        }
        if (this.map_pvp != null) { // map pvp

            this.map_pvp.time_pvp--;
            if (this.map_pvp.status_pvp == 0 && this.map_pvp.time_pvp <= 0) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 0);
                        if (players.get(i).pcl != null) {
                            players.get(i).pcl.batDau();
                        }
                    }
                }
                this.map_pvp.time_pvp = 5;
                this.map_pvp.status_pvp = 1;
            } else if (this.map_pvp.status_pvp == 1 && this.map_pvp.time_pvp <= 0) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 1);
                        Service.use_potion(players.get(i), 0, players.get(i).body.get_hp_max(true));
                        Service.use_potion(players.get(i), 1, players.get(i).body.get_mp_max(true));
                    }
                }
                this.map_pvp.time_pvp = 4;
                this.map_pvp.status_pvp = 2;
            } else if (this.map_pvp.status_pvp == 2 && this.map_pvp.time_pvp <= 0) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 2);
                        //
                        Pvp.show_info(players.get(i), 180, 0, 0, 3);
                        change_flag(players.get(i), (i == 0 ? 14 : 15));
                    }
                }
                this.map_pvp.time_pvp = 180;
                this.map_pvp.status_pvp = 3;
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        if (players.get(i).pcl != null) {
                            players.get(i).pcl.batDau();
                        }
                    }
                }
            }
            if (this.map_pvp.status_pvp == 3 && players.size() == 2) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        if (players.get(i).isdie) {
                            this.map_pvp.status_pvp = 91;
                            break;
                        }
                    }
                }
            } else if (this.map_pvp.status_pvp == 91 && players.size() == 2) {
                this.map_pvp.status_pvp = 90;
            } else if (this.map_pvp.status_pvp == 90 && players.size() == 2) {
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        if (this.map_pvp.num_win_p1 == 3 || this.map_pvp.num_win_p2 == 3) {
                            change_flag(players.get(i), -1);
                        }
                        // if (players.get(i).isdie) {
                        players.get(i).isdie = false;
                        Service.send_revive_player(players.get(i));
                        Service.use_potion(players.get(i), 0, players.get(i).body.get_hp_max(true));
                        Service.use_potion(players.get(i), 1, players.get(i).body.get_mp_max(true));
                        // }
                    }
                }
                this.map_pvp.status_pvp = 3;
            } else if (this.map_pvp.status_pvp == 90) {
                this.map_pvp.status_pvp = 3;
            }
            if (this.map_pvp.status_pvp == 3
                    && (this.map_pvp.num_win_p1 == 3 || this.map_pvp.num_win_p2 == 3)) {
                this.map_pvp.status_pvp = 4;
                this.map_pvp.time_pvp = 4;
                //
                try {
                    if (this.map_pvp.type_map == 0) { // la map pvp
                        if (this.map_pvp.num_win_p1 == 3) {
                            Pvp.pvp_notice(players.get(0), 3);
                            Pvp.pvp_notice(players.get(1), 4);
                            players.get(0).pvp_win++;
                            players.get(1).pvp_lose++;
                            //
                            int chenhLech = players.get(1).get_pvpPoint() - players.get(0).get_pvpPoint();
                            if (chenhLech > 15) {
                                chenhLech = 15;
                            } else if (chenhLech < -15) {
                                chenhLech = -15;
                            }
                            chenhLech += 30;
                            int diemwin = chenhLech;
                            players.get(0).update_pvpPoint(diemwin);
                            players.get(1).update_pvpPoint(-chenhLech);
                        } else {
                            Pvp.pvp_notice(players.get(1), 3);
                            Pvp.pvp_notice(players.get(0), 4);
                            players.get(1).pvp_win++;
                            players.get(0).pvp_lose++;
                            //
                            int chenhLech = players.get(0).get_pvpPoint() - players.get(1).get_pvpPoint();
                            if (chenhLech > 15) {
                                chenhLech = 15;
                            } else if (chenhLech < -15) {
                                chenhLech = -15;
                            }
                            chenhLech += 30;
                            int diemwin = chenhLech;
                            players.get(1).update_pvpPoint(diemwin);
                            players.get(0).update_pvpPoint(-chenhLech);
                        }
                    } else if (this.map_pvp.type_map == 2) { // la map truy na
                        if (this.map_pvp.num_win_p1 == 3) {
                            Pvp.pvp_notice(players.get(0), 3);
                            Pvp.pvp_notice(players.get(1), 4);
                            //
                            long beri_win = (10_000L + (long) players.get(1).get_wanted_point()) / 100L;
                            long beri_lose = (5_000L + (long) players.get(1).get_wanted_point()) / 100L;
                            players.get(0).update_wanted_point((int) beri_win);
                            players.get(1).update_wanted_point((int) -beri_lose);
                            //
                            Wanted_Chest.receiv_ruong(players.get(0));
                        } else {
                            Pvp.pvp_notice(players.get(1), 3);
                            Pvp.pvp_notice(players.get(0), 4);
                            //
                            long beri_win = (10_000L + (long) players.get(0).get_wanted_point()) / 100L;
                            long beri_lose = (5_000L + (long) players.get(0).get_wanted_point()) / 100L;
                            players.get(1).update_wanted_point((int) beri_win);
                            players.get(0).update_wanted_point((int) -beri_lose);
                            //
                            Wanted_Chest.receiv_ruong(players.get(1));
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    core.GameLogger.error("[Disconnect] Map.update_mapPvp error: " + e.getMessage());
                    this.map_pvp.status_pvp = 3;
                }
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        change_flag(players.get(i), -1);
                    }
                }
            }
            if (this.map_pvp.status_pvp == 3 && players.size() < 2) {
                this.map_pvp.status_pvp = 4;
                this.map_pvp.time_pvp = 4;
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Pvp.pvp_notice(players.get(i), 3);
                        //
                        Pvp.show_info(players.get(i), 4, 3, 0, 3);
                        change_flag(players.get(i), -1);
                        //
                        if (this.map_pvp.type_map == 0) { // la map pvp
                            players.get(i).update_pvpPoint(20);
                        }
                    }
                }
            } else if (this.map_pvp.status_pvp == 3 && this.map_pvp.time_pvp <= 0) {
                //
                try {
                    if (this.map_pvp.type_map == 0) { // la map pvp
                        Player p1 = players.get(0);
                        Player p2 = players.get(1);
                        if (p1 != null && p2 != null && !p1.equals(p2)) {
                            if (this.map_pvp.num_win_p1 > this.map_pvp.num_win_p2) {
                                p1.pvp_win++;
                                p1.update_pvpPoint(15);
                                p2.pvp_lose++;
                                p2.update_pvpPoint(-15);
                            } else if (this.map_pvp.num_win_p1 < this.map_pvp.num_win_p2) {
                                p1.pvp_lose++;
                                p1.update_pvpPoint(-15);
                                p2.pvp_win++;
                                p2.update_pvpPoint(15);
                            }
                        }
                    }
                } catch (Exception e) {
                    core.GameLogger.error("[Disconnect] Map.update_mapPvp error: " + e.getMessage());
                }
                //
                this.map_pvp.status_pvp = 4;
                this.map_pvp.time_pvp = 4;
                for (int i = 0; i < players.size(); i++) {
                    if (this.map_pvp.type_map == 0) { // la map pvp
                        Service.send_box_ThongBao_OK(players.get(i),
                                "Hết thời gian, kết quả hòa, bạn sẽ được đưa về map chờ");
                    } else {
                        Service.send_box_ThongBao_OK(players.get(i),
                                "Đối thủ xứng tầm không thể phân biệt thắng thua");
                    }
                }
            } else if (this.map_pvp.status_pvp == 4 && this.map_pvp.time_pvp <= 0) {
                Vgo vgo = new Vgo();
                if (this.map_pvp.type_map == 0) { // la map pvp
                    vgo.map_go = Map.get_map_by_id(1000);
                } else if (this.map_pvp.type_map == 2) { // la map truy na
                    vgo.map_go = Map.get_map_by_id(119);
                } else {
                    vgo.map_go = Map.get_map_by_id(1);
                }
                if (vgo.map_go != null && vgo.map_go.length > 0) {
                    vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                    vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                    List<Player> playerList = new ArrayList<>();
                    synchronized (players) {
                        for (int i = 0; i < players.size(); i++) {
                            playerList.add(players.get(i));
                        }
                    }
                    playerList.forEach(l -> {
                        try {
                            l.targetFight = null;
                            change_flag(l, -1);
                            l.goto_map(vgo);
                            // Giải phóng bot khi trận đấu kết thúc
                            if (l.pcl != null) {
                                l.pcl.dis();
                            }
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mapPvp error: " + e.getMessage());
                        }
                    });
                } else {
                    core.GameLogger.error("Không tìm thấy Map để thoát trong update_map_pvp");
                }
                this.map_pvp.status_pvp = 99;
            } else if (this.map_pvp.status_pvp == 99) {
                running = false;
                this.map_pvp = null;
            }
        }
    }

    private void update_item_map() throws IOException {
        for (int i = 0; i < this.list_it_map.length; i++) {
            ItemMap it = this.list_it_map[i];
            if (it != null && it.time_exist < System.currentTimeMillis()) {
                this.remove_obj(it.index, it.category);
                this.list_it_map[i] = null;
            }
            if (it != null && (it.time_exist - 10_000L) < System.currentTimeMillis()) {
                it.id_master = -1;
            }
        }
    }

    public void remove_obj(int index, int category) throws IOException {
        Message m = new Message(13);
        m.writer().writeShort(index);
        m.writer().writeByte(category);
        this.send_msg_all_p(m, null, true);
        m.cleanup();
    }

    private void update_player() throws IOException {
        // 1. Không cần tạo snapshot, duyệt trực tiếp trên CopyOnWriteArrayList cực an
        // toàn
        List<Player> list_remove = new ArrayList<>();
        for (Player p0 : players) {
            Player p0_local = p0; // [SHADOWING]
            if (p0_local == null)
                continue;

            io.Session localConn = p0_local.conn; // [SHADOWING SESSION]

            // [STABILITY] Wrap disciple AI in try-catch to prevent Map Thread crash
            try {
                if (p0_local.myDisciple != null && p0_local.map != null && p0_local.map.equals(this)) {
                    p0_local.myDisciple.updateDiscipleAI();
                }
            } catch (Exception e) {
                core.GameLogger.error("[Disciple AI Error] Master: " + p0_local.name + " - " + e.getMessage());
            }

            // [FUSION] Expiration check
            long now = System.currentTimeMillis();
            if (p0_local.fusionType != 0 && p0_local.fusionExpiry != -1 && now > p0_local.fusionExpiry) {
                p0_local.stopFusion();
                try {
                    Service.send_box_ThongBao_OK(p0_local, "Hợp thể đã kết thúc!");
                } catch (IOException e) {
                    // Mạng lỗi không làm crash bản đồ
                }
            }

            if ((p0_local.isclose || localConn == null) && !p0_local.isOfflineTraining) {
                // [SANITY CHECK] Xóa đệ tử ma (Đệ tử không có sư phụ trong map hoặc sư phụ đang
                // dung hợp)
                if (p0_local.isDisciple()) {
                    client.Disciple d = (client.Disciple) p0_local;
                    if (d.master == null || d.master.map != this || d.master.fusionType != 0) {
                        this.leave_map(p0_local, 0);
                        list_remove.add(p0_local); // XÓA CỨNG KHỎI DANH SÁCH
                        core.GameLogger.info("[Sanity] Hard-removed ghost disciple: " + d.name);
                        continue;
                    }
                }

                if (p0_local.conn != null) {
                    p0_local.pcl.dis();
                }
                list_remove.add(p0_local);
                continue;
            }

            // ── OFFLINE TRAINING AI TICK ────────────────────────────────────
            if (p0_local.isOfflineTraining) {
                // 1. Kiểm tra hết hạn
                if (now > p0_local.offlineTrainingEndTime) {
                    p0_local.isOfflineTraining = false;
                    p0_local.offlineTrainingEndTime = -1L;
                    client.Player.flush(p0_local, true);
                    this.leave_map(p0_local, 0);
                    io.SessionManager.unregisterPlayer(p0_local);
                    continue;
                }

                // 2. Kiểm tra tử trận (Chỉ xử lý Sư phụ, Đệ tử sẽ đi theo Sư phụ)
                if (p0_local.isdie) {
                    if (p0_local.isDisciple()) {
                        continue;
                    }
                    if (p0_local.lastDieTimeOffline == 0L) {
                        p0_local.lastDieTimeOffline = now;
                    }
                    long deadTime = now - p0_local.lastDieTimeOffline;

                    // Thử hồi sinh tại chỗ bằng vé (ID 89) sau 30 giây
                    if (deadTime > 30_000L && p0_local.item.total_item_bag_by_id(4, 89) > 0) {
                        p0_local.item.remove_item47(4, 89, 1);
                        p0_local.item.update_Inventory(-1, false);
                        p0_local.isdie = false;
                        p0_local.hp = p0_local.body.get_hp_max(true);
                        p0_local.mp = p0_local.body.get_mp_max(true);
                        p0_local.lastDieTimeOffline = 0L;
                        Service.use_potion(p0_local, 0, p0_local.hp);
                        Service.use_potion(p0_local, 1, p0_local.mp);
                        Service.send_revive_player(p0_local);
                        core.GameLogger.info("[Offline] " + p0_local.name + " used Item 89.");
                        continue;
                    }

                    // CHỈ VÉ VIP (Type 2) MỚI TỰ HỒI SINH VỀ LÀNG VÀ QUAY LẠI
                    // Vé thường (Type 1) sẽ nằm im tại chỗ cho đến khi hết thời gian hoặc người
                    // chơi log vào
                    if (p0_local.offlineTrainingType == 2 && deadTime > 60_000L) { // Đợi 60 giây để hồi sinh về làng
                        Map trainingMap = p0_local.map;
                        p0_local.lastDieTimeOffline = 0L;

                        try {
                            p0_local.revive_to_village();

                            if (trainingMap != null) {
                                final int mapId = trainingMap.template.id;
                                final short targetX = p0_local.offlineX;
                                final short targetY = p0_local.offlineY;

                                new Thread(() -> {
                                    try {
                                        Thread.sleep(3000);
                                        // Sử dụng logic Vgo để dịch chuyển chuẩn xác
                                        Vgo vgo = new Vgo();
                                        vgo.map_go = Map.get_map_by_id(mapId);
                                        vgo.xnew = targetX;
                                        vgo.ynew = targetY;
                                        p0_local.goto_map(vgo);

                                        core.GameLogger.info("[Offline] VIP " + p0_local.name + " returned to map "
                                                + mapId + " via Vgo at (" + targetX + "," + targetY + ")");
                                    } catch (Exception e) {
                                    }
                                }).start();
                            }
                        } catch (Exception e) {
                            core.GameLogger.error("Offline AI: return logic failed for " + p0_local.name, e);
                        }
                        continue;
                    }
                    continue;
                } else {
                    p0_local.lastDieTimeOffline = 0L;
                }

                // 3. Chạy AI
                p0_local.updateOfflineTrainingAI();
            }
            // ────────────────────────────────────────────────────────────────
            // Logic Buff HP/MP và Hồi phục duy trì
            int hp_buff = 0;
            int mp_buff = 0;
            EffTemplate eff = p0.get_eff(0);
            if (!p0.isdie && eff != null) {
                hp_buff = eff.param;
                if (p0.clan != null) {
                    int buff_percent = 100;
                    if (p0.clan.check_buff(2))
                        buff_percent += 25;
                    if (p0.clan.check_buff(4))
                        buff_percent += 25;
                    hp_buff = (hp_buff * buff_percent) / 100;
                }
            }
            eff = p0.get_eff(1);
            if (!p0.isdie && eff != null) {
                mp_buff = eff.param;
                if (p0.clan != null) {
                    int buff_percent = 100;
                    if (p0.clan.check_buff(2))
                        buff_percent += 25;
                    if (p0.clan.check_buff(4))
                        buff_percent += 25;
                    mp_buff = (mp_buff * buff_percent) / 100;
                }
            }
            now = System.currentTimeMillis();
            if (p0.time_buff_hp_mp < now) {
                p0.time_buff_hp_mp = now + 5000L;
                hp_buff += p0.body.get_hp_auto_buff(true);
                mp_buff += p0.body.get_mp_auto_buff(true);
            }
            long hp_max = p0.body.get_hp_max(true);
            long mp_max = p0.body.get_mp_max(true);
            if (!p0.isdie && p0.hp < hp_max && hp_buff > 0 && p0.get_eff(202) == null) {
                p0.update_hp(hp_buff);
            }
            if (!p0.isdie && p0.mp < mp_max && mp_buff > 0) {
                p0.update_mp(mp_buff);
            }
            if (now - p0.time_regen >= 1000) {
                if (!p0.isdie && p0.mp < mp_max) {
                    long mpRegen = Math.max(1, mp_max * 5 / 100);
                    long mpAdd = Math.min(mpRegen, mp_max - p0.mp);
                    if (mpAdd > 0)
                        p0.update_mp(mpAdd);
                }
                if (p0.get_eff(207) != null) {
                    long hp_decrease = hp_max / 100;
                    if (p0.hp - hp_decrease > 0)
                        p0.update_hp(-hp_decrease);
                }
                if (Map.isMapLang(this.template.id) && !p0.isdie) {
                    long hpAdd = Math.min(hp_max * 20 / 100, hp_max - p0.hp);
                    long mpAdd = Math.min(mp_max * 20 / 100, mp_max - p0.mp);
                    if (hpAdd > 0)
                        p0.update_hp(hpAdd);
                    if (mpAdd > 0)
                        p0.update_mp(mpAdd);
                }
                p0.time_regen = now;
            }
            // --- Viewport Optimized Broadcasts (Cosmetics) ---
            p0.time_broadcast_hp = now;

            // Hiệu ứng vật phẩm
            Item_wear item37 = p0.item.second_body[7];
            if (item37 != null) {
                int effIndex = switch (item37.template.id) {
                    case 11033 -> 16;
                    case 11034 -> 17;
                    case 11035 -> 18;
                    case 11036 -> 19;
                    case 11037 -> 15;
                    case 11038 -> 23;
                    case 11039 -> 24;
                    case 11040 -> 33;
                    default -> -1;
                };
                if (effIndex != -1 && (now - p0.lastEffUpdateTime >= 500)) {
                    p0_local.lastEffUpdateTime = now;
                    Message m3 = new Message(74);
                    m3.writer().writeByte(1);
                    m3.writer().writeShort(p0_local.index_map);
                    m3.writer().writeShort(effIndex);
                    m3.writer().writeInt(2_000);
                    m3.writer().writeByte(0);
                    m3.writer().writeByte(20);
                    this.send_msg_all_p_distance(m3, p0_local.x, p0_local.y, false, 3, -1, p0_local.index_map);
                    m3.cleanup();
                }
            }

            // Hiệu ứng Skin
            int skinEffId = core.SkinTitleManager.getActiveSkinEffId(p0_local);
            if (skinEffId != -1 && (now - p0_local.lastSkinUpdateTime >= 500)) {
                p0_local.lastSkinUpdateTime = now;
                Message m3 = new Message(74);
                try {
                    m3.writer().writeByte(1);
                    m3.writer().writeShort(p0_local.index_map);
                    m3.writer().writeShort(skinEffId);
                    m3.writer().writeInt(2_000);
                    m3.writer().writeByte(p0_local.isRight ? 0 : 2);
                    m3.writer().writeByte(20);
                    this.send_msg_all_p_distance(m3, p0_local.x, p0_local.y, false, 3, -1, p0_local.index_map);
                } finally {
                    m3.cleanup();
                }
            }

            // Hiệu ứng Danh hiệu
            for (MyDanhHieu myDanhHieu : p0_local.danh_hieu) {
                if (myDanhHieu.isUsed) {
                    DanhHieu danhHieu = DanhHieu.getTemplate(myDanhHieu.id);
                    if (danhHieu != null && danhHieu.eff != -1 && (now - p0_local.lastTitleUpdateTime >= 500)) {
                        p0_local.lastTitleUpdateTime = now;
                        Message m3 = new Message(74);
                        try {
                            m3.writer().writeByte(1);
                            m3.writer().writeShort(p0_local.index_map);
                            m3.writer().writeShort(danhHieu.eff);
                            m3.writer().writeInt(2_000);
                            m3.writer().writeByte(0);
                            m3.writer().writeByte(20);
                            this.send_msg_all_p_distance(m3, p0_local.x, p0_local.y, false, 3, -1, p0_local.index_map);
                        } finally {
                            m3.cleanup();
                        }
                    }
                }
            }
            // Hồi sinh Little Garden
            if (this.template.id == 81 && this.map_little_garden != null && p0_local.isdie
                    && p0_local.time_hs_little_garden <= System.currentTimeMillis()) {
                p0_local.isdie = false;
                Service.use_potion(p0_local, 0, p0_local.body.get_hp_max(true));
                Service.use_potion(p0_local, 1, p0_local.body.get_mp_max(true));
            }
        }
        // 2. Xóa bỏ synchronized ở removeAll, CopyOnWriteArrayList xử lý cực tốt việc
        // này
        if (!list_remove.isEmpty()) {
            players.removeAll(list_remove);
        }
        for (Player p0 : list_remove) {
            Message m = new Message(3);
            m.writer().writeShort(p0.index_map);
            m.writer().writeByte(0);
            // Khi người chơi thoát map, cũng dùng viewport để đỡ lag map
            this.send_msg_all_p(m, p0, false);
            m.cleanup();
        }
    }

    private void update_mob() {
        boolean current_has_dead = false;
        boolean noPlayers = players.isEmpty();
        if (this.can_PK) {
            for (int i = 0; i < mobs_cache.size(); i++) {
                Mob mob = mobs_cache.get(i);
                if (mob != null) {
                    if (!mob.isdie) {
                        if (!noPlayers && mob.id_target != -1) {
                            try {
                                mob_fire(mob, mob.id_target);
                            } catch (IOException e) {
                                core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                            }
                        }
                    } else {
                        current_has_dead = true;
                        if (mob.hp == 0 && mob.time_refresh < System.currentTimeMillis()) {
                            if (mob.isDynamic) {
                                try {
                                    this.remove_obj(mob.index, 1);
                                    this.mobs_cache.remove(mob);
                                    Mob.ENTRYS.remove(mob.index);
                                } catch (Exception e) {
                                }
                                continue;
                            }
                            if (mob.mob_template.mob_id == 132) {
                                continue;
                            }
                            mob.isdie = false;
                            mob.hp = mob.hp_max;
                            try {
                                if (mob.mob_template.mob_id == 133) {
                                    Message m = new Message(4);
                                    m.writer().writeShort(mob.index);
                                    m.writer().writeShort(mob.mob_template.mob_id); // id mob
                                    m.writer().writeShort(mob.x);
                                    m.writer().writeShort(mob.y);
                                    m.writer().writeShort(mob.level); // lv
                                    m.writer().writeInt(f.setInteger(mob.hp));
                                    m.writer().writeInt(f.setInteger(mob.hp_max));
                                    // short effId = mob.mob_template.skill[0];
                                    // if (effId <= 0)
                                    // effId = 15;
                                    // m.writer().writeShort(effId);
                                    m.writer().writeShort(mob.mob_template.skill[0]);
                                    m.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME); // tgian hs
                                    m.writer().writeByte(mob.mob_template.typemonster); // type mons
                                    m.writer().writeByte(0); // lvthongthao
                                    this.send_msg_all_p_distance(m, mob.x, mob.y);
                                    m.cleanup();

                                    for (int childIndex : mob132Indices) {
                                        Mob mobSub = Mob.ENTRYS.get(childIndex);
                                        if (mobSub != null) {
                                            mobSub.isdie = false;
                                            mobSub.hp = mobSub.hp_max;
                                            try {
                                                Message mSub = new Message(4);
                                                mSub.writer().writeShort(mobSub.index);
                                                mSub.writer().writeShort(mobSub.mob_template.mob_id);
                                                mSub.writer().writeShort(mobSub.x);
                                                mSub.writer().writeShort(mobSub.y);
                                                mSub.writer().writeShort(mobSub.level);
                                                mSub.writer().writeInt(f.setInteger(mobSub.hp));
                                                mSub.writer().writeInt(f.setInteger(mobSub.hp_max));
                                                mSub.writer().writeShort(mobSub.mob_template.skill[0]);
                                                mSub.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME);
                                                mSub.writer().writeByte(mobSub.mob_template.typemonster);
                                                mSub.writer().writeByte(0);
                                                this.send_msg_all_p_distance(mSub, mobSub.x, mobSub.y);
                                                mSub.cleanup();
                                            } catch (Exception e) {
                                                core.GameLogger
                                                        .error("[Disconnect] Map.update_mob error: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                            }
                            mob.id_target = -1;

                            // try {
                            // Message m_local = new Message(1);
                            // m_local.writer().writeByte(1);
                            // m_local.writer().writeShort(mob.index);
                            // m_local.writer().writeShort(mob.x);
                            // m_local.writer().writeShort(mob.y);
                            // this.send_msg_all_p(m_local, null, true);
                            // m_local.cleanup();
                            // } catch (IOException e) {
                            // core.GameLogger.printStackTrace(e);
                            // }
                            if (mob.mob_template.mob_id != 133) {
                                try {
                                    Message m_local = new Message(4);
                                    try {
                                        m_local.writer().writeShort(mob.index);
                                        m_local.writer().writeShort(mob.mob_template.mob_id);
                                        m_local.writer().writeShort(mob.x);
                                        m_local.writer().writeShort(mob.y);
                                        m_local.writer().writeShort(mob.level);
                                        m_local.writer().writeInt(f.setInteger(mob.hp));
                                        m_local.writer().writeInt(f.setInteger(mob.hp_max));
                                        short effId = mob.mob_template.skill[0];
                                        m_local.writer().writeShort(effId);
                                        int respawnTime = Skill_info.GLOBAL_MOB_RESPAWN_TIME;
                                        if (Map.is_map_treo(mob.map.template.id)) {
                                            respawnTime = 3;
                                        } else if (Map.is_map_banh_kem(mob.map.template.id)) {
                                            respawnTime = 3;
                                        }
                                        m_local.writer().writeShort(respawnTime);
                                        m_local.writer().writeByte(mob.mob_template.typemonster);
                                        m_local.writer().writeByte(0);
                                        this.send_msg_all_p(m_local, null, true);
                                    } finally {
                                        m_local.cleanup();
                                    }
                                } catch (IOException e) {
                                    core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!noPlayers && this.map_Hang != null) {
            for (int i = 0; i < this.map_Hang.mobs.size(); i++) {
                Mob mob = this.map_Hang.mobs.get(i);
                if (mob.map.equals(this) && !mob.isdie) {
                    if (mob.id_target != -1) {
                        try {
                            mob_fire(mob, mob.id_target);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                        }
                    }
                } else if (mob.map.equals(this) && mob.isdie) {
                    current_has_dead = true;
                }
            }
        }
        if (!noPlayers && this.map_ThuThachVeThan != null) {
            for (int i = 0; i < this.map_ThuThachVeThan.mobs.size(); i++) {
                Mob mob = this.map_ThuThachVeThan.mobs.get(i);
                if (mob.map.equals(this) && !mob.isdie) {
                    if (mob.id_target != -1) {
                        try {
                            mob_fire(mob, mob.id_target);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                        }
                    }
                } else if (mob.map.equals(this) && mob.isdie) {
                    current_has_dead = true;
                }
            }
        }
        if (!noPlayers && this.map_VoHanLienTang != null) {
            for (int i = 0; i < this.map_VoHanLienTang.mobs.size(); i++) {
                Mob mob = this.map_VoHanLienTang.mobs.get(i);
                if (mob.map.equals(this) && !mob.isdie) {
                    if (mob.id_target != -1) {
                        try {
                            mob_fire(mob, mob.id_target);
                        } catch (IOException e) {
                            core.GameLogger.error("[Disconnect] Map.update_mob error: " + e.getMessage());
                        }
                    }
                } else if (mob.map.equals(this) && mob.isdie) {
                    current_has_dead = true;
                }
            }
        }
        this.has_dead_mob = current_has_dead;
    }

    private void mob_fire(Mob mob, int id_target) throws IOException {
        if (mob.mob_template.mob_id == 132 || mob.mob_template.mob_id == 133 || mob.mob_template.mob_id == 184
                || mob.mob_template.mob_id == 127 || mob.mob_template.mob_id == 150 || mob.mob_template.mob_id == 152) {
            return;
        }
        if (!mob.isdie && mob.time_skill < System.currentTimeMillis()) {
            mob.time_skill = System.currentTimeMillis() + 1800L;
            Player p0 = this.get_player_by_id_inmap(id_target);
            if (p0 != null && p0.map != null) {
                if (!mob.isdie && !p0.wait_change_map && !p0.isdie
                        && p0.time_can_mob_atk < System.currentTimeMillis()) {
                    long dame = mob.hp / 200;
                    long dame_mine = 0; // Fix: Khai báo biến dame_mine cho logic phản đòn
                    long def = p0.body.get_def(true);
                    def = (def * (1000 + p0.body.get_def_percent(true))) / 1000;
                    if (this.map_VoHanLienTang != null) {
                        int giamDef = this.map_VoHanLienTang.getOptionValue(15, this.map_VoHanLienTang.floor);
                        if (giamDef > 0) {
                            def = def * Math.max(0, 1000 - (giamDef * 10)) / 1000;
                        }
                    }
                    dame = Math.max(1, dame * 1000 / (1000 + def));
                    // --- BỔ SUNG LOGIC VÔ HẠN LIÊN TẦNG & DIFFERENTIAL SCALING ---
                    long raw_miss = p0.body.get_miss(true);
                    long raw_mien_thuong = p0.body.get_dame_skip(true);
                    long raw_phan_don = p0.body.get_dame_react(true);

                    // Mặc định quái không có chỉ số phá, trừ khi là trong VHLT
                    long mob_pha_ne = 0;
                    long mob_pha_mien = 0;
                    long mob_pha_phan = 0;

                    if (this.map_VoHanLienTang != null) {
                        mob_pha_ne = this.map_VoHanLienTang.getOptionValue(51, this.map_VoHanLienTang.floor) * 10;
                        mob_pha_mien = this.map_VoHanLienTang.getOptionValue(63, this.map_VoHanLienTang.floor) * 10;
                        mob_pha_phan = this.map_VoHanLienTang.getOptionValue(64, this.map_VoHanLienTang.floor) * 10;
                    }

                    // Tỉ lệ Né tránh (Differential)
                    long diff_miss = core.Util.getStatDiminishing(Math.max(0, raw_miss - mob_pha_ne));
                    boolean miss = (diff_miss > Util.random(1000));

                    if (this.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID
                            && (mob.mob_template.mob_id == BossEvent.BossEvent.BOSS_MAIN_ID
                                    || BossEvent.BossEvent.isGuardian(mob.mob_template.mob_id))) {
                        miss = false; // Boss không bao giờ đánh hụt
                    }

                    if (miss) {
                        dame = 0; // 0 to show Miss/0 on client
                    } else {
                        // Tỉ lệ Miễn thương (Differential)
                        long diff_mien = core.Util.getStatDiminishing(Math.max(0, raw_mien_thuong - mob_pha_mien));
                        if (diff_mien > 0) {
                            dame = (dame * Math.max(0, 1000 - diff_mien)) / 1000;
                        }
                        if (dame < 0) {
                            dame = 0;
                        }

                        // Sát thương phản đòn (Differential & Amount Split)
                        long diff_phan = core.Util.getStatDiminishing(Math.max(0, raw_phan_don - mob_pha_phan));
                        if (diff_phan > Util.random(1000)) {
                            // Tỉ lệ kích hoạt thành công, tính lượng phản tỉ lệ thuận
                            dame_mine = (dame * diff_phan) / 1000;
                        }
                    }

                    // Boss DAMAGE OVERRIDE (10% maxHP)
                    if (this.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID
                            && (mob.mob_template.mob_id == BossEvent.BossEvent.BOSS_MAIN_ID
                                    || BossEvent.BossEvent.isGuardian(mob.mob_template.mob_id))) {
                        dame = p0.body.get_hp_max(true) * 3 / 100;
                    }

                    // update hp target
                    if (p0.hp == p0.body.get_hp_max(true) && dame >= p0.hp) {
                        p0.update_hp(1 - p0.hp);
                    } else {
                        p0.update_hp(-dame);
                    }
                    if (p0.hp <= 0) {
                        mob.id_target = -1;
                        mob_non_focus(mob);
                    }
                    //
                    // Phản đòn đã được tính toán ở phần dame
                    if (dame_mine > 0) {
                        mob.hp -= dame_mine;
                        if (mob.hp <= 0) {
                            mob.hp = 1;
                        }
                        this.update_hp_mp_eff(null, mob, 1, -dame_mine);
                    }
                    //
                    Message m = new Message(100);
                    m.writer().writeShort(mob.index);
                    m.writer().writeByte(1);
                    m.writer().writeInt(f.setInteger(mob.hp)); // hp
                    m.writer().writeInt(f.setInteger(mob.hp)); // mp
                    // short effId =
                    // mob.mob_template.skill[Util.random(mob.mob_template.skill.length)];
                    // if (effId <= 0)
                    // effId = 28;
                    // m.writer().writeShort(effId);
                    m.writer().writeShort(mob.mob_template.skill[Util.random(mob.mob_template.skill.length)]);
                    m.writer().writeByte(1); // size target
                    m.writer().writeShort(id_target);
                    m.writer().writeByte(0);
                    m.writer().writeInt(f.setInteger(dame));
                    m.writer().writeInt(0); // dame plus
                    m.writer().writeInt(f.setInteger(p0.hp));
                    m.writer().writeByte(0);
                    this.send_msg_all_p_distance(m, mob.x, mob.y, false, 3, -1, p0.index_map);
                    m.cleanup();
                    //
                    if (p0.hp <= 0) {
                        die_player(p0, p0);
                    }
                    if (mob.id_target != -1
                            && !(Math.abs(mob.x - p0.x) < 200 && Math.abs(mob.y - p0.y) < 200)) {
                        mob.id_target = -1;
                        mob_non_focus(mob);
                    }
                }
            }
        }
    }

    private void mob_non_focus(Mob mob) throws IOException {
        Message m2 = new Message(5);
        try {
            m2.writer().writeShort(mob.index);

            // Xài hàm Overload siêu ngắn: Ép 6 người xem gần nhất, giải tán đám đông!
            this.send_msg_all_p_distance(m2, mob.x, mob.y);

        } finally {
            // Thói quen tốt của Senior: Luôn bọc finally để dọn rác RAM
            m2.cleanup();
        }
    }

    public void die_player(Player p0, Player p) throws IOException {
        p0.isdie = true;
        p0.update_die();
        //
        if (this.template.id == 6) {
            Map[] map_go = Map.get_map_by_id(1);
            if (map_go != null && map_go.length > 0) {
                Vgo vgo = new Vgo();
                vgo.map_go = map_go;
                vgo.xnew = 350;
                vgo.ynew = 250;
                p0.goto_map(vgo);
            } else {
                core.GameLogger.error("Không tìm thấy Map ID 1 để chuyển về trong die_player");
            }
        }
        //
        Message m = new Message(7);
        try {
            m.writer().writeShort(p.index_map);
            m.writer().writeByte(0);
            m.writer().writeShort(p0.index_map);
            m.writer().writeByte(0);
            m.writer().writeShort(p.pointPk); // point pk

            // GIỮ NGUYÊN: Gửi toàn Map để đồng bộ trạng thái tử trận, tránh lỗi bóng ma
            this.send_msg_all_p(m, p0, true);

        } catch (IOException e) {
            GameLogger.error("[Disconnect] Map.die_player error: " + e.getMessage());
        } finally {
            // Đảm bảo dọn dẹp Message
            m.cleanup();
        }
        //
        if (p0.is_combo != null) {
            p0.is_combo = null;
            Service.start_combo(p0, 0);
        }
        //
        if (this.map_pvp != null && !p.equals(p0)) {
            if (players.indexOf(p0) == 0) {
                this.map_pvp.num_win_p2++;
                Pvp.show_info(p, this.map_pvp.time_pvp, this.map_pvp.num_win_p1,
                        this.map_pvp.num_win_p2, 3);
                Pvp.show_info(p0, this.map_pvp.time_pvp, this.map_pvp.num_win_p2,
                        this.map_pvp.num_win_p1, 3);
            } else {
                this.map_pvp.num_win_p1++;
                Pvp.show_info(p, this.map_pvp.time_pvp, this.map_pvp.num_win_p2,
                        this.map_pvp.num_win_p1, 3);
                Pvp.show_info(p0, this.map_pvp.time_pvp, this.map_pvp.num_win_p1,
                        this.map_pvp.num_win_p2, 3);
            }
        }
    }

    public void enter_map(Player p) throws IOException {
        if (p == null)
            return;
        // [SECURITY] Assign map-unique ID to all clones to prevent index collision
        if (p.clone) {
            p.index_map = this.getNextCloneId();
        }
        // [SECURITY] Chặn duplicate bằng addIfAbsent (Atomic)
        if (!this.players.addIfAbsent(p)) {
            core.GameLogger
                    .warn("[enter_map] Duplicate registration blocked for: " + p.name + " in Map " + this.template.id);
            return;
        }
        core.GameLogger
                .info("[enter_map] SUCCESS: " + p.name + " | Map: " + this.template.id + " | Total: " + players.size());
        // Đồng bộ quái phó bản VHLT khi người chơi vào map
        // if (this.map_VoHanLienTang != null && !this.map_VoHanLienTang.mobs.isEmpty())
        // {
        // for (Mob mob : this.map_VoHanLienTang.mobs) {
        // if (mob != null && !mob.isdie) {
        // Message m = new Message(4);
        // m.writer().writeShort(mob.index);
        // m.writer().writeShort(mob.mob_template.mob_id);
        // m.writer().writeShort(mob.x);
        // m.writer().writeShort(mob.y);
        // m.writer().writeByte(0);
        // // m.writer().writeShort(mob.level);
        // m.writer().writeInt(f.setInteger(mob.hp));
        // m.writer().writeInt(f.setInteger(mob.hp_max));
        // // m.writer().writeShort(mob.mob_template.skill[0]);
        // // m.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME);
        // // m.writer().writeByte(mob.mob_template.typemonster);
        // m.writer().writeByte(0);
        // p.addmsg(m);
        // m.cleanup();
        // }
        // }
        // }
        // Đồng bộ quái phó bản VHLT khi người chơi vào map (BẢN 1 — FIX)
        if (this.map_VoHanLienTang != null && !this.map_VoHanLienTang.mobs.isEmpty()) {
            for (Mob mob : this.map_VoHanLienTang.mobs) {
                if (mob != null && !mob.isdie) {
                    Message m = new Message(4);
                    m.writer().writeShort(mob.index);
                    m.writer().writeShort(mob.mob_template.mob_id);
                    m.writer().writeShort(mob.x);
                    m.writer().writeShort(mob.y);
                    m.writer().writeShort(mob.level); // FIX: writeShort thay vì writeByte
                    m.writer().writeInt(f.setInteger(mob.hp));
                    m.writer().writeInt(f.setInteger(mob.hp_max));
                    m.writer().writeShort(mob.mob_template.skill[0]); // FIX: bỏ comment
                    m.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME); // FIX: bỏ comment
                    m.writer().writeByte(mob.mob_template.typemonster); // FIX: bỏ comment
                    m.writer().writeByte(0);
                    p.addmsg(m);
                    m.cleanup();
                }
            }
        }
    }

    public void leave_map(Player p, int type) {
        if (p == null)
            return;
        // Clean up Disciple if it's currently in this map to prevent "ghosting"
        try {
            if (!(p instanceof client.Disciple) && p.myDisciple != null && p.myDisciple.map != null
                    && p.myDisciple.map.equals(this)) {
                this.leave_map(p.myDisciple, type);
            }
        } catch (Exception e) {
            core.GameLogger.error("[Ghost Fix] Disciple cleanup error in leave_map: " + e.getMessage());
        }
        // [CLEANUP] Xóa sạch mọi occurrence để triệt tiêu ghosting
        int sizeBefore = players.size();
        players.removeIf(existing -> existing == p);
        core.GameLogger.info("[leave_map] CLEAN: " + p.name + " | Map: " + this.template.id + " | Removed: "
                + (sizeBefore - players.size()));
        if (p.pcl != null) {
            playerClone pclTemp = p.pcl;
            p.pcl = null; // Ngắt liên kết trước khi dọn dẹp để tránh đệ quy
            try {
                pclTemp.dis();
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
            }
        }

        // [STABILITY] Clear mob targets referencing this player to prevent ghosts
        for (Mob mob : mobs_cache) {
            if (mob.id_target == p.index_map) {
                mob.id_target = -1;
            }
        }

        p.is_combo = null;
        p.time_combo = 0;
        p.id_meet_in_map.clear();
        p.id_meet_in_map.add("" + p.index_map);
        //
        if (Map.get_map_plus().contains(this)) {
            if (this.players.size() < 1) {
                this.stop_map();
                Map.get_map_plus().remove(this);
            }
        }
        try {
            Message m = new Message(3);
            m.writer().writeShort(p.index_map);
            // 2: next map, 1: tele, 0: exit game
            m.writer().writeByte(type);
            synchronized (players) {
                for (int i = 0; i < players.size(); i++) {
                    Player p0_local = players.get(i);
                    if (p0_local == null)
                        continue;
                    io.Session localConn = p0_local.conn;
                    if (localConn != null) {
                        localConn.addmsg(m);
                    }
                    p0_local.id_meet_in_map.remove("" + p.index_map);
                }
            }
            m.cleanup();
            //
            p.id_select_temp = -1;
            p.trade_target = null;
            p.is_lock_trade = false;
            p.is_accept_trade = false;
            p.isShop = false;
            p.isBox = false;
            //
            if (p.ship_pet != null && p.ship_pet.map == null) {
                m = new Message(3);
                m.writer().writeShort(p.ship_pet.index_map);
                m.writer().writeByte(type);
                synchronized (players) {
                    for (int i = 0; i < players.size(); i++) {
                        Player p0 = players.get(i);
                        if (p0 == null)
                            continue;
                        if (p0.conn != null) {
                            p0.addmsg(m);
                        }
                    }
                }
                m.cleanup();
            }
            //
            if (p.trade_target != null) {
                Trade.end_trade_by_disconnect(p.trade_target, p, 0, "");
                p.fee_trade = 0;
                p.money_trade = 0;
                p.is_lock_trade = false;
                p.is_accept_trade = false;
                p.list_item_trade3 = null;
                p.list_item_trade47 = null;
                p.trade_target = null;
            }
            int pet_select = p.get_pet();
            if (pet_select != -1) {
                Message m22 = new Message(-80);
                try {
                    m22.writer().writeByte(1);
                    m22.writer().writeShort(-1);
                    m22.writer().writeShort(p.index_map);

                    // GIỮ NGUYÊN: Phải để 'true' để xóa sạch Pet trên máy của tất cả mọi người
                    // Mẹo nhỏ: Bạn có thể đổi 'null' thành 'p' để không gửi ngược lại cho chính
                    // người đang rời map
                    this.send_msg_all_p(m22, p, true);

                } catch (IOException e) {
                    GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
                } finally {
                    // Luôn dọn dẹp Message để tránh treo Server sau vài ngày chạy
                    m22.cleanup();
                }
            }
        } catch (IOException e) {
            core.GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
        } catch (NullPointerException e) {
            core.GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
        }
    }

    public static Map[] get_map_by_id(int id) {
        return MAP_BY_ID.get(id);
    }

    public static Map createMazeMap(int templateId, String customName, int zoneId, List<MazePortal> portals) {
        Map[] templates = get_map_by_id(templateId);
        if (templates == null || templates.length == 0)
            return null;

        Map original = templates[0];
        Map mazeMap = new Map();
        mazeMap.template = original.template;
        mazeMap.isMazeMap = true;
        mazeMap.customName = customName;
        mazeMap.zone_id = 0; // Set to 0 to avoid byte overflow and negative numbers in UI
        mazeMap.is_map_plus = true;
        // Ensure maze maps have player capacity
        if (mazeMap.template.max_player <= 0) {
            mazeMap.template.max_player = 100;
        }
        mazeMap.running = true;
        mazeMap.mazePortals = portals;

        // Inherit mobs if needed, or leave empty for sea maps
        mazeMap.list_mob = original.list_mob;

        // Start the map thread using the existing MAP_POOL
        mazeMap.future = core.Manager.MAP_POOL.scheduleAtFixedRate(mazeMap, 0, 200,
                java.util.concurrent.TimeUnit.MILLISECONDS);
        MAP_PLUS.add(mazeMap);

        return mazeMap;
    }

    public static List<Player> get_all_players() {
        List<Player> result = new ArrayList<>();
        try {
            // Lấy player ở các map tĩnh
            for (int i = 0; i < Map.ENTRYS.size(); i++) {
                for (int j = 0; j < Map.ENTRYS.get(i).length; j++) {
                    Map map = Map.ENTRYS.get(i)[j];
                    if (map != null) {
                        synchronized (map.players) {
                            result.addAll(map.players);
                        }
                    }
                }
            }

            // Lấy player ở các map động (ĐÃ KHÓA ĐỒNG BỘ TRÁNH LỖI VĂNG EXCEPTION)
            synchronized (Map.MAP_PLUS) {
                for (int i = 0; i < MAP_PLUS.size(); i++) {
                    Map map = MAP_PLUS.get(i);
                    if (map != null) {
                        synchronized (map.players) {
                            result.addAll(map.players);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Bắt lỗi dự phòng, nhưng với synchronized thì 99% sẽ không bao giờ văng lỗi
            // nhảy CPU nữa
            core.GameLogger.error("[Disconnect] Map.get_all_players error: " + e.getMessage());
        }
        return result;
    }

    public static Player get_player_by_name_allmap(String name) {
        if (name == null) {
            return null;
        }
        Player p = io.SessionManager.PLAYER_BY_NAME.get(name.toLowerCase());
        if (p == null) {
            // Fallback: try stripping decorations
            String rawName = core.Util.getRawName(name);
            if (rawName != null && !rawName.equals(name)) {
                p = io.SessionManager.PLAYER_BY_NAME.get(rawName.toLowerCase());
            }
        }
        return p;
    }

    public static Player get_player_by_id_allmap(int id) {
        return io.SessionManager.PLAYER_BY_ID.get(id);
    }

    public void send_move(Player p, short x, short y) throws IOException {
        if (p.isdie || p.wait_change_map) {
            return;
        }

        if (!p.isdie) {
            if (x != p.x) {
                p.isRight = (x > p.x);
            }
            p.x = x;
            p.y = y;
            p.lastMoveTime = System.currentTimeMillis();

            // --- Ocean Maze Portal Collision ---
            if (this.isMazeMap && !this.mazePortals.isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - p.lastMazeTeleport > 2500) { // 2.5s cooldown
                    for (MazePortal portal : this.mazePortals) {
                        if (Math.abs(p.x - portal.x) < 60 && Math.abs(p.y - portal.y) < 60) {
                            Map targetMap = OceanMazeManager.getMazeNode(portal.targetNode);
                            if (targetMap != null) {
                                p.lastMazeTeleport = now;
                                Vgo vgo = new Vgo();
                                vgo.map_go = new Map[] { targetMap };
                                vgo.xnew = portal.teleX;
                                vgo.ynew = portal.teleY;
                                p.goto_map(vgo);
                                return; // Exit after teleport to prevent further processing
                            }
                        }
                    }
                }
            }
            // ------------------------------------

            if (p.map.map_pvp != null) {
                if (p.x < 5) {
                    p.x = 5;
                } else if (p.x > 1070) {
                    p.x = 1070;
                }
                if (p.y > 330) {
                    p.y = 330;
                } else if (p.y < 170) {
                    p.y = 170;
                }
            }
            //
            if (!Map.is_map_dont_show_other_info(this.template.id)) {
                Message mmove = new Message(1);
                mmove.source_index = p.index_map;
                mmove.writer().writeByte(0);
                mmove.writer().writeShort(p.index_map);
                mmove.writer().writeShort(p.x);
                mmove.writer().writeShort(p.y);
                this.send_msg_all_p_distance(mmove, p.x, p.y, false, 6, p.index_map);
                mmove.cleanup();
                //
                if (p.ship_pet != null && p.ship_pet.map != null && p.ship_pet.map.equals(p.map)) {
                    if (p.ship_pet.time < System.currentTimeMillis()) {
                        p.ship_pet.time = System.currentTimeMillis() + 1000L;
                        if (Math.abs(p.x - p.ship_pet.x) < 300
                                && Math.abs(p.y - p.ship_pet.y) < 300) {
                            p.ship_pet.x = p.x;
                            p.ship_pet.y = p.y;
                        }
                    }
                    //
                    mmove = new Message(1);
                    try {
                        mmove.writer().writeByte(0);
                        mmove.writer().writeShort(p.ship_pet.index_map);
                        mmove.writer().writeShort(p.ship_pet.x);
                        mmove.writer().writeShort(p.ship_pet.y);

                        // THAM SỐ CHUẨN:
                        // false: Không gửi toàn map.
                        // 6: Cho 6 ông hàng xóm thấy pet của bạn cho vui mắt.
                        // -1: Không chặn ai cả.
                        // p.index_map: CHỦ NHÂN LÀ VIP. Phải luôn thấy pet mình di chuyển.
                        this.send_msg_all_p_distance(mmove, p.ship_pet.x, p.ship_pet.y, false, 6, -1, p.index_map);

                    } catch (Exception e) {
                        GameLogger.error("[Disconnect] Map.leave_map error: " + e.getMessage());
                    } finally {
                        // "Bảo hiểm" RAM cho server của bạn
                        mmove.cleanup();
                    }
                }
            }
            // mob
            for (int i = 0; i < list_mob.length; i++) {
                Mob mob = Mob.ENTRYS.get(Integer.valueOf(list_mob[i]));
                if (mob != null && !mob.isdie && !p.isdie && Math.abs(mob.x - p.x) < 70
                        && Math.abs(mob.y - p.y) < 70 && mob.id_target == -1) {
                    mob.id_target = p.index_map;
                }
            }
            if (this.map_Hang != null) {
                for (int i = 0; i < this.map_Hang.mobs.size(); i++) {
                    Mob mob = this.map_Hang.mobs.get(i);
                    if (mob.map.equals(this) && !mob.isdie && !p.isdie && mob.id_target == -1
                            && Math.abs(mob.x - p.x) < 70 && Math.abs(mob.y - p.y) < 70) {
                        mob.id_target = p.index_map;
                    }
                }
            }
            if (this.map_ThuThachVeThan != null) {
                for (int i = 0; i < this.map_ThuThachVeThan.mobs.size(); i++) {
                    Mob mob = this.map_ThuThachVeThan.mobs.get(i);
                    if (mob.map.equals(this) && !mob.isdie && !p.isdie && mob.id_target == -1
                            && Math.abs(mob.x - p.x) < 70 && Math.abs(mob.y - p.y) < 70) {
                        mob.id_target = p.index_map;
                    }
                }
            }

            if (p.ischangemap) {
                for (Vgo vgo : this.template.vgos) {
                    if (Math.abs(vgo.xold - p.x) < 60 && Math.abs(vgo.yold - p.y) < 60) {
                        p.time_change_map = System.currentTimeMillis() + 5000L;
                        p.goto_map(vgo);
                        break;
                    }
                }
            } else if (p.time_change_map < System.currentTimeMillis()) {
                p.ischangemap = true;
            }
        }
    }

    private static boolean is_map_dont_show_other_info(int id) {
        return id == 64;
    }

    public void update_num_player_in_map(Player p) throws IOException {
        Message m = new Message(-70);
        m.writer().writeByte((byte) p.map.players.size());
        m.writer().writeByte(15);
        p.addmsg(m);
        m.cleanup();
    }

    public boolean use_skill(Player p, short idSkill, byte CatBeFire, byte size_target,
            List<Integer> targetIds) throws IOException { // su dung skill
        // [SAFETY] Bọc thép ngay tại đầu luồng tung chiêu
        if (p == null || p.isdie || p.skill_point == null) {
            return false;
        }
        core.SkinTitleManager.onAttack(p);
        if (!p.isdie && size_target > 0) {
            if (!p.time_use_skill.containsKey(((int) idSkill))) {
                p.time_use_skill.put(((int) idSkill), 1L);
            }
            long time_ = p.time_use_skill.get(((int) idSkill));
            if (!p.isDisciple() && time_ > System.nanoTime()) {
                p.timeDecDame++;
                if (p.timeDecDame > 100) {
                    p.timeDecDame = 100;
                }
                return false;
            }
            p.timeDecDame -= 10;
            if (p.timeDecDame < 0) {
                p.timeDecDame = 0;
            }
            Skill_info sk_temp = p.get_skill_temp(idSkill);
            if (sk_temp == null || (sk_temp.temp.typeSkill != 1 && sk_temp.temp.typeSkill != 4)) {
                return false;
            }
            if (!p.isDisciple() && (p.mp - sk_temp.temp.manaLost) < 0) {
                Service.send_box_ThongBao_OK(p, "MP không đủ!");
                return false;
            }
            p.time_use_skill.put(((int) idSkill), (System.nanoTime()
                    + (sk_temp.temp.timeDelay * 1_000L * (1000 - p.body.get_agility(true)))));
            long oldMp = p.mp;
            long mp_calc = p.mp - sk_temp.temp.manaLost + p.body.get_mp_atk_absorb(true);
            int mpPlus = 0;
            for (int i = 0; i < p.daHanhTrinh.size(); i++) {
                if (p.daHanhTrinh.get(i).quant == 1 && p.daHanhTrinh.get(i).id == 515) {
                    mpPlus++;
                }
            }
            mp_calc = (long) ((double) mp_calc * (100.0 + mpPlus) / 100.0);
            p.update_mp(mp_calc - oldMp);
            long dame = p.body.get_dame(true);
            dame = (long) ((double) dame * p.body.get_dame_devil_percent() / 100.0);
            EffTemplate eff = p.get_eff(5); // combo
            if (eff != null) {
                dame *= 2;
            }
            eff = p.get_eff(18); // skill boc pha
            if (eff != null) {
                dame = (long) ((double) dame * eff.param / 100.0);
            }
            if (dame > 2 && p.get_eff(21) != null) { // zoombie
                dame /= 2;
            }
            if (sk_temp.temp.ID == 2057 || sk_temp.temp.ID == 2058) { // buff trai bong toi
                dame = (long) ((double) dame * 12.0 / 10.0);
            }
            dame = (long) ((double) dame * (100.0 - p.timeDecDame) / 100.0);
            // kich an danh la choang
            if (p.get_eff(407) == null && p.body.get_kich_an(7) > 0) {
                p.danhLaChoang++;
            }
            // kich an thanh loc
            if (p.get_eff(408) == null && p.body.get_kich_an(8) > 0) {
                p.thanhLoc++;
            }

            //
            if (sk_temp.temp.nTarget > 0 && sk_temp.temp.nTarget < size_target) {
                size_target = sk_temp.temp.nTarget;
            }
            size_target = (byte) Math.min(size_target, targetIds.size());
            // [FIX CORE - CỨU TINH MỤC TIÊU CHÍNH]
            // Client Hải Tặc thường nhét con quái BỊ NHẮM vào CUỐI mảng.
            // Ta phải bốc con cuối cùng ra nhét lên đầu danh sách để không bao giờ bị
            // trượt.
            List<Integer> smartTargets = new ArrayList<>();
            if (!targetIds.isEmpty()) {
                // Bắt buộc lấy con cuối cùng (Mục tiêu chính)
                smartTargets.add(targetIds.get(targetIds.size() - 1));
            }
            // Nhồi thêm các con vạ lây ở đầu mảng vào cho đủ quota của chiêu thức
            for (int id : targetIds) {
                if (smartTargets.size() >= size_target)
                    break;
                if (!smartTargets.contains(id)) {
                    smartTargets.add(id);
                }
            }
            // [CẬP NHẬT QUAN TRỌNG] Phải tính lại size_target dựa trên số lượng thực tế sau khi lọc trùng
            size_target = (byte) smartTargets.size();
            
            Player[] p_target = new Player[size_target];
            Mob[] mob_target = new Mob[size_target];
            Ship_pet spet = null;

            // 1. VÒNG LẶP CHỈ DÙNG ĐỂ GOM MỤC TIÊU VÀO MẢNG
            for (int i = 0; i < size_target; i++) {
                // int id_target = targetIds.get(i); // Lấy id_target từ danh sách
                int id_target = smartTargets.get(i);
                switch (CatBeFire) {
                    case 0: {
                        p_target[i] = this.get_player_by_id_inmap(id_target);
                        if (i == 0 && p_target[i] == null) {
                            spet = Ship_pet.get_pet(id_target);
                        }
                        break;
                    }
                    case 1: {
                        mob_target[i] = Mob.ENTRYS.get(id_target);
                        if (mob_target[i] == null && this.template.id == 81
                                && this.map_little_garden != null) {
                            mob_target[i] = this.get_mobs(id_target, 0);
                        }
                        if (mob_target[i] == null && this.map_ThuThachVeThan != null) {
                            for (int j = 0; j < this.map_ThuThachVeThan.mobs.size(); j++) {
                                if (this.map_ThuThachVeThan.mobs.get(j).index == id_target) {
                                    mob_target[i] = this.map_ThuThachVeThan.mobs.get(j);
                                    break;
                                }
                            }
                            if (mob_target[i] == null) {
                                remove_obj(id_target, 1);
                            }
                        }
                        if (mob_target[i] == null && this.map_Hang != null) {
                            for (int j = 0; j < this.map_Hang.mobs.size(); j++) {
                                if (this.map_Hang.mobs.get(j).index == id_target) {
                                    mob_target[i] = this.map_Hang.mobs.get(j);
                                    break;
                                }
                            }
                            if (mob_target[i] == null) {
                                remove_obj(id_target, 1);
                            }
                        }
                        if (mob_target[i] == null && this.map_VoHanLienTang != null) {
                            for (int j = 0; j < this.map_VoHanLienTang.mobs.size(); j++) {
                                if (this.map_VoHanLienTang.mobs.get(j).index == id_target) {
                                    mob_target[i] = this.map_VoHanLienTang.mobs.get(j);
                                    break;
                                }
                            }
                            if (mob_target[i] == null) {
                                remove_obj(id_target, 1);
                            }
                        }
                        // Bỏ return false đi để 1 con null không làm tịt luôn cả chiêu đánh
                        break;
                    }
                }
            } // <---- ĐÃ ĐÓNG VÒNG LẶP FOR TẠI ĐÂY!

            // 2. GỌI HÀM GÂY DAMAGE 1 LẦN DUY NHẤT CHO CẢ MẢNG
            long[] exp_up = null;
            switch (CatBeFire) {
                case 0: {
                    eff = p.get_eff(12);
                    if (eff != null && p_target[0] != null) { // skill buff zoro
                        Service.send_eff_sword_splash(p_target[0].index_map, p);
                    }
                    if (p_target.length > 0 && p_target[0] == null && spet != null) {
                        atk_ship_pet(spet, p, idSkill);
                    } else {
                        Fire_Player(p_target, p, idSkill, dame);
                    }
                    break;
                }
                case 1: {
                    if (mob_target.length > 0 && mob_target[0] != null) {
                        eff = p.get_eff(12);
                        if (eff != null) { // skill buff zoro
                            Service.send_eff_sword_splash(mob_target[0].index, p);
                        }
                    }
                    exp_up = Fire_Monster(mob_target, p, idSkill, dame);
                    break;
                }
            }

            // 3. CỘNG EXP VÀ HIỆU ỨNG (Giữ nguyên)
            if (exp_up != null) {
                if (exp_up[0] > 0) {
                    exp_up[0] = (exp_up[0] * (1000 + p.body.get_xp_more())) / 1000;
                    p.update_exp(exp_up[0], true);
                }
                if (exp_up[1] > 0) {
                    exp_up[1] = (exp_up[1] * (1000l + p.body.get_xp_skill_more())) / 1000l;
                    p.update_skill_exp(idSkill, exp_up[1]);
                }
            }
            if (idSkill == 2) {
                Item_wear item37 = p.item.second_body[7];
                if (item37 != null) {
                    switch (item37.template.id) {
                        case 11038: {
                            sendOneHitEff(p, 25);
                            sendOneHitEff(p, 26);
                            sendOneHitEff(p, 27);
                            sendOneHitEff(p, 28);
                            break;
                        }
                        case 11039: {
                            sendOneHitEff(p, 29);
                            sendOneHitEff(p, 30);
                            sendOneHitEff(p, 31);
                            sendOneHitEff(p, 32);
                            break;
                        }
                        case 11040: {
                            sendOneHitEff(p, 34);
                            sendOneHitEff(p, 35);
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    public void sendOneHitEff(Player p, int effClientId) {
        try {
            Message m = new Message(74);
            m.writer().writeByte(1);
            m.writer().writeShort(p.index_map);
            m.writer().writeShort(effClientId);
            m.writer().writeInt(700);
            m.writer().writeByte(0);
            m.writer().writeByte(1);
            // - p.x, p.y: Lấy vị trí người chơi làm tâm.
            // - 6: Chỉ cho tối đa 6 người xung quanh xem.
            // - p.index_map: Vé VIP cho chính chủ (phải thấy hiệu ứng mình đánh).
            this.send_msg_all_p_distance(m, p.x, p.y, false, 6, -1, p.index_map);
            m.cleanup();
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] Map.sendOneHitEff error: " + e.getMessage());
        }
    }

    private void atk_ship_pet(Ship_pet spet, Player p, short idSkill) throws IOException {
        if (p == null || Map.isMapLang(this.template.id) || spet == null || spet.main_ship == null
                || spet.main_ship.index_map == p.index_map
                || !(p.typePirate == 0 || p.typePirate == 2)
                || (p.typePirate == 0 && spet.main_ship.typePirate == 0)) {
            return;
        }
        Skill_info sk_temp = p.get_skill_temp(idSkill);
        if (sk_temp != null) {
            Message m = new Message(100);
            try {
                m.writer().writeShort(p.index_map);
                m.writer().writeByte(0);
                m.writer().writeInt(f.setInteger(p.hp));
                m.writer().writeInt(f.setInteger(p.mp));
                m.writer().writeShort(sk_temp.get_eff_skill());
                m.writer().writeByte(1);
                //
                m.writer().writeShort(spet.index_map);
                m.writer().writeByte(0);
                int dame_ship_pet = 50;
                m.writer().writeInt(dame_ship_pet);
                //
                spet.hp -= dame_ship_pet;
                if (spet.hp <= 0) {
                    spet.hp = 0;
                    spet.main_ship.ship_pet = null;
                    Ship_pet.remv(spet);
                    remove_obj(spet.index_map, 0);
                    List<GiftBox> listGiftP = new ArrayList<>();
                    GiftBox.addGiftBox(listGiftP, 0, 4, 5_000_000);
                    Service.send_gift(p, 0, "Cướp Hàng", "Cướp Hàng " + spet.main_ship.name, listGiftP, true);
                    p.updateRuby0(5);
                    Service.send_box_ThongBao_OK(spet.main_ship, "Vận chuyển thất bại\nHàng của bạn đã bị cướp");
                }
                //
                m.writer().writeInt(0); // dame plus
                m.writer().writeInt(spet.hp);
                //
                m.writer().writeByte(0);
                send_msg_all_p_distance(m, spet.x, spet.y, false, 6, -1, p.index_map);
            } finally {
                // LUÔN LUÔN dọn rác, dù sống hay chết, dù lỗi hay không
                if (m != null) {
                    m.cleanup();
                }
            }
        }
    }

    private void Fire_Player(Player[] list_target, Player p, int idSkill, long dame)
            throws IOException {
        // [SAFETY] Bọc thép: Kiểm tra người tung chiêu
        if (p == null || p.isdie || p.skill_point == null || p.skill_point.isEmpty()) {
            return;
        }
        Skill_info sk_temp = p.get_skill_temp(idSkill);
        if (!this.can_PK || sk_temp == null
                || (this.map_pvp != null && (this.map_pvp.num_win_p1 == 3
                        || this.map_pvp.num_win_p2 == 3 || this.map_pvp.status_pvp != 3))) {
            return;
        }
        int dame_plus_percent = 0;
        long dame_magic_plus_percent = p.body.get_dame_ap();
        long crit_skill = p.body.get_crit(true);
        long multi_dame_skill = p.body.get_multi_dame_when_crit(true);
        boolean crit = false;
        //
        List<Dame_Msg> list = new ArrayList<>();
        long dame_mine_all = 0;
        long damebefore = dame;
        long dame2;
        EffTemplate eff;
        //
        for (int i = 0; i < list_target.length; i++) {
            Player p_target = list_target[i];
            // [SAFETY] Kiểm tra mục tiêu và người tung chiêu (p) một lần nữa trong vòng lặp
            if (p_target != null && p != null && p_target.index_map != p.index_map && !p_target.isdie && !p.isdie
                    && (p_target.time_can_mob_atk - 1000) < System.currentTimeMillis()) {
                if (!((p.typePirate == 0 && p_target.typePirate == 2)
                        || (p.typePirate == 2 && p_target.typePirate == 0)
                        || (p.typePirate == 1 && p_target.typePirate == 2)
                        || (p.typePirate == 2 && p_target.typePirate == 1)
                        || (p.type_pk == 14 && p_target.type_pk == 15)
                        || (p.type_pk == 15 && p_target.type_pk == 14)
                        || (p.typePirate == 2 && p_target.typePirate == 2) || (p.type_pk == 0)
                        || (p_target.type_pk == 1) || (p.type_pk == 3 && p_target.type_pk == 3)
                        || (p_target.type_pk == 0)
                        || (p.type_pk == 3 && p_target.type_pk >= 4 && p_target.type_pk <= 8)
                        || (p_target.type_pk == 3 && p.type_pk >= 4 && p.type_pk <= 8)
                        || (p.type_pk >= 4 && p.type_pk <= 8 && p_target.type_pk >= 4
                                && p_target.type_pk <= 8 && p.type_pk != p_target.type_pk)
                        || (p.type_pk == 11 && (p_target.type_pk == 12 || p_target.type_pk == 13))
                        || (p.type_pk == 12 && (p_target.type_pk == 11 || p_target.type_pk == 13))
                        || (p.type_pk == 13 && (p_target.type_pk == 11 || p_target.type_pk == 12))
                        || ((this.template.id == 303) && p.clan != null
                                && p_target.clan != null && p.clan.equals(p_target.clan))
                        || BossEvent.BossEvent.getInstance().checkFriendlyFire(p, p_target))) {
                    continue;
                }
                ItemFashionP2 checkF = p.check_fashion(17);
                if (checkF != null && checkF.is_use && i == 0 && p_target.get_eff(21) == null) { // tt
                    // zombie
                    if (10 > Util.random(100)) {
                        p_target.add_new_eff(21, Util.random(28), 5000);
                        p_target.update_info_to_all();
                        //
                        for (int j = 0; j < players.size(); j++) {
                            Service.charWearing(p_target, players.get(j), false);
                        }
                    }
                }
                for (int type_red = 1; type_red <= 5; type_red++) {
                    int val_red = (int) p.body.view_in4(90 + type_red);
                    if (val_red > 0) {
                        p_target.add_badge_reduction(p, type_red, val_red, 5000L);
                    }
                }
                dame2 = damebefore;
                // [SAFETY] Dame + % trang bị dùng double
                dame2 = (long) ((double) dame2 * (1000.0 + dame_plus_percent) / 1000.0);
                long baseDame = (long) p.skill_point.get(0).get_dame(p);
                if (baseDame <= 0) {
                    baseDame = 1;
                }
                // [SAFETY] Phép tính gây lỗi nặng nhất: (A * B) / C -> Chế ngự bằng double
                // trung gian
                dame2 = (long) ((double) dame2 * sk_temp.get_dame(p) / (double) baseDame);

                long def = p_target.body.get_def(true);
                def = (long) ((double) def * (1000.0 + p_target.body.get_def_percent(true)) / 1000.0);
                long raw_pierce = p.body.get_pierce(true) - p_target.body.get_pierce_reduce();
                long pierce = core.Util.getStatDiminishing(Math.max(0, raw_pierce));
                def = (long) ((double) def * Math.max(0.0, 1000.0 - (double) pierce) / 1000.0);
                dame2 = Math.max(0, dame2 - def);
                long raw_crit = crit_skill - p_target.body.get_crit_reduce();
                crit = core.Util.getStatDiminishing(Math.max(0, raw_crit)) > Util.random(1000);
                //
                long dame_mine = 0;
                Dame_Msg dame_inf = new Dame_Msg();
                dame_inf.data = new ArrayList<>();
                dame_inf.targetP = p_target;
                if (dame2 > 0 && idSkill != 0) {
                    // [SAFETY] Nhân ma pháp % bằng double
                    dame_inf.dameM = (long) ((double) p.get_skill_temp(idSkill).get_dame(p) * dame_magic_plus_percent
                            / 1000.0);
                }
                if (dame_inf.dameM < 0) {
                    dame_inf.dameM = 0;
                }
                if (dame2 > 0 && (idSkill == 2038 || idSkill == 2041)) {
                    if (p.get_eff(6) != null) {
                        dame2 = (dame2 * 115) / 100;
                    }
                    // fashion bao dom + chim ung
                    for (int i12 = 0; i12 < p.fashion.size(); i12++) {
                        if ((p.fashion.get(i12).id == 33 || p.fashion.get(i12).id == 34)
                                && p.fashion.get(i12).is_use) {
                            dame2 = (dame2 * 115) / 100;
                            break;
                        }
                    }
                }
                // --- BỔ SUNG LOGIC BIẾN THIÊN (DIFFERENTIAL SCALING) ---
                long raw_get_miss = p_target.body.get_miss(true) - p.body.get_miss_reduce();
                long diff_miss = core.Util.getStatDiminishing(Math.max(0, raw_get_miss));

                long raw_MienThuong = p_target.body.get_dame_skip(true) - p.body.get_dame_skip_reduce();
                long diff_mien = core.Util.getStatDiminishing(Math.max(0, raw_MienThuong));

                long raw_react_dame = p_target.body.get_dame_react(true) - p.body.get_dame_react_reduce();
                long diff_phan = core.Util.getStatDiminishing(Math.max(0, raw_react_dame));

                boolean miss = (p.get_eff(205) != null || diff_miss > Util.random(1000));

                if (miss) {
                    dame2 = 0; // 0 to show Miss/0 on client
                    dame_inf.dameM = 0; // Miss means no damage
                } else {
                    // 1. Miễn thương (Skip - Pierce) dùng double để tinh chỉnh
                    if (diff_mien > 0) {
                        dame2 = (long) ((double) dame2 * Math.max(0.0, 1000.0 - (double) diff_mien) / 1000.0);
                    }

                    // 2. Phản đòn (Reflect - CounterReflect)
                    if (diff_phan > Util.random(1000)) {
                        // Tỉ lệ phản đòn tỉ lệ thuận với hiệu số chỉ số
                        dame_mine = (long) ((double) dame2 * (double) diff_phan / 1000.0);
                    }
                }
                int kich_an;
                // --- BÁO CÁO DAME CHO HỆ THỐNG PK ---
                long dame_to_p_system = Math.max(0, dame2) + Math.max(0, dame_inf.dameM);
                if (dame_to_p_system > 0 && p_target.body.get_hp_max(true) >= 2_000_000_000) {
                    String message = "PK HP : " + Util.number_format(p_target.hp) + "\nDame : "
                            + Util.number_format(dame_to_p_system);
                    Message m = new Message(17);
                    m.writer().writeShort(p_target.index_map);
                    m.writer().writeByte(1);
                    m.writer().writeUTF(message);
                    p.addmsg(m);
                    m.cleanup();
                }
                if (dame2 > 0) {
                    // eff kich an
                    kich_an = p_target.body.get_kich_an(0);
                    if (kich_an > 0) { // bat tu
                        eff = p_target.get_eff(300);
                        if (eff != null) {
                            dame2 = 0;
                            dame_inf.dameM = 0;
                        } else {
                            eff = p_target.get_eff(400);
                            if (eff == null) {
                                int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                                if (per > Util.random(120)) {
                                    dame2 = 0;
                                    dame_inf.dameM = 0;
                                    int time_eff = 5;

                                    p_target.add_new_eff(300, 1, (time_eff * 1_000));
                                    Service.send_kich_an(p, p_target, time_eff, 0, 0, 0);
                                    time_eff = 60_000;

                                    p_target.add_new_eff(400, 1, time_eff);
                                }
                            }
                        }
                    }
                    kich_an = p_target.body.get_kich_an(1);
                    if (kich_an > 0) { // loi cam on
                        eff = p_target.get_eff(401);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                            if (per > Util.random(120)) {
                                p_target.hp += dame2 / 5;
                                int time_eff = 60_000;

                                p_target.add_new_eff(401, 1, time_eff);
                                Service.send_kich_an(p, p_target, 1, 1, 0, f.setInteger(dame2 / 5));
                                dame2 = 0;
                                dame_inf.dameM = 0;
                            }
                        }
                    }
                    kich_an = p_target.body.get_kich_an(2);
                    if (kich_an > 0) { // la chan
                        eff = p_target.get_eff(402);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                            if (per > Util.random(120)) {
                                int time_eff = 60_000;

                                p_target.add_new_eff(402, 1, time_eff);
                                Service.send_kich_an(p, p_target, 1, 2, 5, 50);
                                //
                                time_eff = 5_000;

                                //
                                eff = p.get_eff(205);
                                if (eff == null) {
                                    p.add_new_eff(205, 1, time_eff);
                                } else {
                                    eff.time = System.currentTimeMillis() + time_eff;
                                }
                                Buff.send_choang(p_target, p, time_eff);
                                dame2 = 0;
                                dame_inf.dameM = 0;
                            }
                        }
                    }
                    kich_an = p_target.body.get_kich_an(3);
                    if (kich_an > 0) { // khoa nang luong
                        eff = p_target.get_eff(403);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                            if (per > Util.random(120)) {
                                // p_target.add_new_eff(303, 1, 5_000);
                                int time_eff = 60_000;

                                p_target.add_new_eff(403, 1, time_eff);
                                Service.send_kich_an(p, p_target, 5, 3, 0, f.setInteger(p.mp));
                                p.mp = 0;
                                dame2 = 0;
                                dame_inf.dameM = 0;
                            }
                        }
                    }
                    kich_an = p.body.get_kich_an(4);
                    if (kich_an > 0) { // boc pha
                        eff = p.get_eff(404);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                            if (per > Util.random(120)) {
                                int time_eff = 60_000;

                                p.add_new_eff(404, 1, time_eff);
                                Service.send_kich_an(p_target, p, 1, 4, 0, f.setInteger(dame2 * 2));
                                dame2 *= 2;
                                dame_inf.dameM *= 2;
                            }
                        }
                    }
                    kich_an = p.body.get_kich_an(5);
                    if (kich_an > 0) { // tap trung cao do
                        eff = p.get_eff(305);
                        if (eff != null) {
                            crit = true;
                        } else {
                            eff = p.get_eff(405);
                            if (eff == null) {
                                int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                                if (per > Util.random(120)) {
                                    int time_eff = 10;

                                    p.add_new_eff(305, 1, time_eff * 1_000);
                                    Service.send_kich_an(p_target, p, time_eff, 5, 0, 0);
                                    time_eff = 60_000;

                                    p.add_new_eff(405, 1, time_eff);
                                    crit = true;
                                }
                            }
                        }
                    }
                    kich_an = p.body.get_kich_an(6);
                    if (kich_an > 0) { // ma ca rong
                        eff = p.get_eff(406);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 8 : 5);

                            if (per > Util.random(120)) {
                                int time_eff = 60_000;

                                p.add_new_eff(406, 1, time_eff);
                                Service.send_kich_an(p_target, p, 1, 6, 0, f.setInteger(dame2 / 5));
                                p.update_hp(dame2 / 5);
                            }
                        }
                    }
                    kich_an = p.body.get_kich_an(7);
                    if (kich_an > 0) { // danh la choang
                        eff = p.get_eff(407);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 15 : 20);

                            if (per == p.danhLaChoang) {
                                p.danhLaChoang = 0;
                                int time_eff = 60_000;

                                p.add_new_eff(407, 1, time_eff);
                                Service.send_kich_an(p_target, p, 1, 7, 5, 50);
                                // dame_inf.data.add(new Option_Dame_Msg(5, 1, 50));
                                time_eff = 5_000;

                                eff = p_target.get_eff(205);
                                if (eff == null) {
                                    p_target.add_new_eff(205, 1, time_eff);
                                } else {
                                    eff.time = System.currentTimeMillis() + time_eff;
                                }
                                Buff.send_choang(p, p_target, time_eff);
                            }
                        }
                    }
                    kich_an = p.body.get_kich_an(8);
                    if (kich_an > 0) { // thanh loc
                        eff = p.get_eff(408);
                        if (eff == null) {
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 15 : 20);

                            if (per == p.thanhLoc) {
                                p.thanhLoc = 0;
                                int time_eff = 60_000;

                                p.add_new_eff(408, 1, time_eff);
                                Service.send_kich_an(p_target, p, 1, 8, 0, 0);
                            }
                        }
                    }
                    //
                    kich_an = p_target.body.get_kich_an(9);
                    if (kich_an > 0) { // nen dau
                        eff = p_target.get_eff(409);
                        if (eff == null) {
                            p_target.nenDau++;
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 15 : 20);

                            if (per == p_target.nenDau) {
                                p_target.nenDau = 0;
                                int time_eff = 60_000;

                                p_target.add_new_eff(409, 1, time_eff);
                                Service.send_kich_an(p, p_target, 1, 9, 0, 0);
                            }
                        }
                    }
                    kich_an = p_target.body.get_kich_an(10);
                    if (kich_an > 0) { // giai phong nang luong
                        eff = p_target.get_eff(410);
                        if (eff == null) {
                            p_target.giaiPhongNangLuong++;
                            int per = kich_an == 3 ? 10 : (kich_an == 2 ? 15 : 20);

                            if (per == p_target.giaiPhongNangLuong) {
                                p_target.giaiPhongNangLuong = 0;
                                int time_eff = 60_000;

                                p_target.add_new_eff(410, 1, time_eff);
                                Service.send_kich_an(p, p_target, 1, 10, 5, 50);
                                // dame_inf.data.add(new Option_Dame_Msg(5, 1, 50));
                                time_eff = 5_000;

                                eff = p.get_eff(205);
                                if (eff == null) {
                                    p_target.add_new_eff(205, 1, time_eff);
                                } else {
                                    eff.time = System.currentTimeMillis() + time_eff;
                                }
                                Buff.send_choang(p_target, p, time_eff);
                            }
                        }
                    }
                    //
                    if (dame2 > 0) {
                        dame2 = (dame2 * (1000L + p.body.get_percent_final_dame())) / 1000L;

                        // [SAFETY] Áp dụng hệ số Chí mạng bằng double
                        long multi = 1000L + multi_dame_skill;
                        dame2 = (long) ((double) dame2 * multi / 1000.0);

                        long dame_crit_decrease = Math.min(1000,
                                Math.max(0, p_target.body.get_multi_dame_decrease()));
                        dame2 = (long) ((double) dame2 * (1000.0 - dame_crit_decrease) / 1000.0);
                        if (dame2 < 1) {
                            dame2 = 1;
                        }
                        //
                        long percent_hp_target = p.body.get_dame_percent_hp_target();
                        if (damebefore > 0 && percent_hp_target > 0) {
                            long hp_target = p_target.hp;
                            // Overflow check for % HP
                            if (percent_hp_target > 0 && hp_target > Long.MAX_VALUE / percent_hp_target) {
                                hp_target = Long.MAX_VALUE;
                            } else {
                                hp_target = hp_target * (percent_hp_target) / 1000L;
                            }

                            long mien_reduction = (diff_mien * 3) / 5;
                            hp_target = (hp_target * (1000 - mien_reduction)) / 1000;

                            if (Long.MAX_VALUE - dame2 < hp_target) {
                                dame2 = Long.MAX_VALUE;
                            } else {
                                dame2 += hp_target;
                            }
                        }
                        //
                        dame2 = (dame2 * (1000 - diff_mien)) / 1000;
                        if (dame2 < 0)
                            dame2 = 0;
                    }
                }
                kich_an = p.body.get_kich_an(11);
                if (kich_an > 0 && p.get_eff(202) == null) { // nguoi bat tu
                    long hp_max = p.body.get_hp_max(true);
                    int per = kich_an == 3 ? 20 : (kich_an == 2 ? 15 : 10);

                    long hp_absorb_kichan = (hp_max / 1000) * per;
                    if (hp_absorb_kichan > 0) {
                        Service.send_kich_an(p_target, p, f.setInteger(hp_absorb_kichan) / 10, 11, 0, 0);
                        p.hp += hp_absorb_kichan;
                        if (p.hp > hp_max) {
                            p.hp = hp_max;
                        }
                    }
                }
                //
                if ((p_target.get_eff(7) != null && p_target.type_pk == -1 && this.template.id != 6)
                        || p_target.get_eff(9) != null || damebefore == 0) {
                    dame2 = 0;
                    dame_inf.dameM = 0;
                    dame_mine = 0;
                }
                dame_inf.dameP = dame2;
                //
                if (dame_inf.dameP < -1) {
                    dame_inf.dameP = -1;
                }
                if (dame_inf.dameM < 0) {
                    dame_inf.dameM = 0;
                }
                eff = p_target.get_eff(24);
                if (eff != null) { // kaido thoi trang
                    dame2 /= 2;
                    dame_inf.dameM /= 2;
                }
                //
                long dame_to_target = Math.max(0, dame2) + Math.max(0, dame_inf.dameM);
                if (dame_to_target < 0) {
                    dame_to_target = 0;
                }

                // Cổng 2: Balance Gateway - Kiểm soát sát thương cuối (PvP Scaling)
                dame_to_target = core.BalanceGateway.applyCombatScaling(p, p_target, dame_to_target);

                // Cộng Sát thương chuẩn (ID 57) - Bỏ qua mọi lớp phòng thủ
                // [FIX] Nếu có khiên bảo vệ (7) hoặc bất tử (9), chặn cả sát thương chuẩn
                if (p_target.get_eff(7) == null && p_target.get_eff(9) == null) {
                    dame_to_target += (damebefore * p.body.get_true_dame() / 100);
                } else {
                    dame_to_target = 0;
                }

                p_target.update_hp(-dame_to_target);
                // Disciple Revenge Logic
                if (dame_to_target > 0 && p_target.myDisciple != null && p_target.myDisciple.status == 3
                        && !p_target.myDisciple.isdie && !p.isdie) {
                    p_target.myDisciple.targetPlayer = p;
                }
                if (p_target.body.get_hp_max(true) >= 2_000_000_000L) {
                    String message = "HP : " + Util.number_format(p_target.hp) + "\nDame : "
                            + Util.number_format(dame_to_target);
                    Message m = new Message(17);
                    m.writer().writeShort(p_target.id);
                    m.writer().writeByte(0);
                    m.writer().writeUTF(message);
                    p.addmsg(m);
                    m.cleanup();
                }

                // hut mau trai bong toi
                long HapThuHP = 0;
                long percent_HapThu = p_target.body.get_HapThu_Hp();
                if (dame2 > 1 && percent_HapThu > Util.random(1000) && p.get_eff(202) == null
                        && p_target.hp > 0) {
                    HapThuHP = (dame2 * percent_HapThu) / 1_000L;
                    long hp_max_target = p_target.body.get_hp_max(true);
                    if (HapThuHP > (hp_max_target / 2)) {
                        HapThuHP = (hp_max_target / 2);
                    }
                    Service.use_potion(p_target, 0, f.setInteger(HapThuHP));
                }
                // tu choi tu than
                if (p_target.hp <= 0 && p_target.get_eff(10) == null
                        && p_target.body.get_TuChoiTuThan() > 0) {
                    int time_eff = 10; // thời gian bất tử
                    p_target.add_new_eff(9, 1, time_eff * 1_000);
                    int time_eff_cooldown = 150_000;
                    p_target.add_new_eff(10, 1, time_eff_cooldown);
                    Service.send_eff(p_target, 21, 50);
                    p_target.update_hp(p_target.body.get_hp_max(true) / 10 - p_target.hp);
                }
                //
                if (p_target.hp <= 0) {
                    activities.WishManager.gI().checkReward(p, p_target);

                    if (p.typePirate == 1 && p_target.typePirate == 2) {
                        List<GiftBox> listGift = new ArrayList<>();
                        {
                            ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                            GiftBox giftBox = new GiftBox();
                            giftBox.id = (short) 0;
                            giftBox.type = 4;
                            giftBox.name = itemTemplate4.name;
                            giftBox.icon = itemTemplate4.icon;
                            giftBox.num = f.setInteger(50_000L);
                            giftBox.color = 0;
                            listGift.add(giftBox);
                        }
                        Service.send_gift(p, 0, "Đánh Bại Hải Tặc", "Giết " + p_target.name,
                                listGift, true);
                    }
                    if (p.type_pk == 0) {
                        int totalReward = p_target.thongthao + p_target.tu_tien + p_target.tu_ma;
                        if (totalReward > 0 && !map_cant_save_site(p_target.map.template.id)) {
                            if (p.canReceiveKillPoint(p_target.id)) {
                                int honTuMa = totalReward / 10;
                                if (honTuMa < 1) {
                                    honTuMa = 1;
                                }
                                p.update_point_pk(totalReward);
                                p.item.add_item_bag47(4, 1042, honTuMa); // hồn tu ma
                                p.item.update_Inventory(-1, false);
                                p.updateKillCooldown(p_target.id);
                            }
                        }
                        //
                        LenhTruyNa.completeWantedByTarget(p_target.id, p.id);
                        //
                        if (p_target.type_pk == -1 && p_target.typePirate == -1) {
                            // p_target.enemy_list
                            while (p_target.enemy_list.size() > 50) {
                                p_target.enemy_list.remove(0);
                            }
                            FriendTemp enemy_add = null;
                            for (int j = 0; j < p_target.enemy_list.size(); j++) {
                                if (p_target.enemy_list.get(j).name.equals(p.name)) {
                                    enemy_add = p_target.enemy_list.get(j);
                                    break;
                                }
                            }
                            if (enemy_add != null) {
                                int save_index = p_target.enemy_list.indexOf(enemy_add);
                                FriendTemp save = p_target.enemy_list.get(0);
                                p_target.enemy_list.set(0, enemy_add);
                                p_target.enemy_list.set(save_index, save);
                            } else {
                                enemy_add = new FriendTemp(p);
                                p_target.enemy_list.add(enemy_add);
                                if (p_target.enemy_list.size() >= 2) {
                                    enemy_add.id = p_target.enemy_list
                                            .get(p_target.enemy_list.size() - 2).id + 1;
                                } else {
                                    enemy_add.id = 0;
                                }
                            }
                        }
                    }

                    if (this.template.id == 81 && this.map_little_garden != null) {
                        p_target.time_hs_little_garden = System.currentTimeMillis() + 10_000L;
                        Service.send_time_cool_down(p_target, p_target.time_hs_little_garden,
                                "Hồi sinh", 3);
                    }
                }
                if (dame_inf.dameP > 0 && sk_temp.temp.idEffSpec > 0
                        && sk_temp.temp.idEffSpec < 17) {
                    eff = p_target.get_eff(200 + sk_temp.temp.idEffSpec);
                    if (eff == null) {
                        long reduce_Eff = p_target.body.get_reduce_Eff();
                        long percent = sk_temp.temp.perEffSpec;
                        percent = (percent * (1000 - reduce_Eff)) / 1000;
                        if (percent > Util.random(1000)) {
                            long time = sk_temp.temp.timeEffSpec;
                            time = (time * (1000 - reduce_Eff)) / 1000;
                            p_target.add_new_eff((200 + sk_temp.temp.idEffSpec), 1, (time * 100));
                            dame_inf.data.add(new Option_Dame_Msg(sk_temp.temp.idEffSpec, 1, f.setInteger(time)));
                            // }
                            //
                            if (sk_temp.temp.idEffSpec == 16) {
                                Message m = new Message(74);
                                try {
                                    m.writer().writeByte(1);
                                    m.writer().writeShort(p_target.index_map);
                                    m.writer().writeShort(5);
                                    m.writer().writeInt((f.setInteger(time * 100)));
                                    m.writer().writeByte(1);
                                    m.writer().writeByte(10);
                                    this.send_msg_all_p_distance(m, p.x, p.y, false, 3, -1, p_target.index_map);
                                } finally {
                                    // LUÔN LUÔN dọn rác Message để RAM luôn xanh tươi
                                    if (m != null) {
                                        m.cleanup();
                                    }
                                }
                            }
                        }
                    }
                }
                if (crit) {
                    dame_inf.data.add(new Option_Dame_Msg(1010, f.setInteger(dame_inf.dameP), 0));
                }
                if (HapThuHP > 0) {
                    dame_inf.data.add(new Option_Dame_Msg(1058, f.setInteger(HapThuHP), 0));
                }
                if (dame_mine > 0) {
                    dame_inf.data.add(new Option_Dame_Msg(1014, f.setInteger(dame_mine), 0));
                    dame_mine_all += dame_mine;
                }
                list.add(dame_inf);
            }
        }
        if (dame_mine_all > 0) {
            p.update_hp(-dame_mine_all);
            update_hp_mp_eff(p, null, 1, -dame_mine_all);
        }
        if (list.size() > 0) {
            this.send_dame_msg(p, sk_temp.get_eff_skill(), list);
        }
        if (p.hp <= 0) {
            //
            if (this.map_pvp != null) {
                try {
                    Player p_in_pvp = null;
                    synchronized (players) {
                        for (int i = 0; i < players.size(); i++) {
                            p_in_pvp = players.get(i);
                            if (!p_in_pvp.equals(p)) {
                                break;
                            }
                        }
                    }
                    if (p_in_pvp != null && !p_in_pvp.equals(p)) {
                        die_player(p, p_in_pvp);
                    }
                } catch (Exception e) {
                    core.GameLogger.error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                }
            } else {
                die_player(p, p);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            Player pTarget = list.get(i).targetP;
            if (pTarget != null && pTarget.isdie) {
                die_player(pTarget, p);
            }
        }
    }

    private void update_hp_mp_eff(Player p, Mob mob, int type, long dame) throws IOException {
        Message m = new Message(55);
        try {
            int broadcastX, broadcastY;
            int forcedIndex = -1;

            if (mob != null) {
                m.writer().writeShort(mob.index);
                m.writer().writeByte(1);
                m.writer().writeByte(type);
                m.writer().writeInt(f.setInteger(mob.hp_max));
                m.writer().writeInt(f.setInteger(mob.hp));
                m.writer().writeInt(f.setInteger(dame));
                m.writer().writeInt(f.setInteger(mob.hp_max));
                m.writer().writeInt(f.setInteger(mob.hp));
                m.writer().writeInt(0);
                broadcastX = mob.x;
                broadcastY = mob.y;
            } else if (p != null) {
                m.writer().writeShort(p.index_map);
                m.writer().writeByte(0);
                m.writer().writeByte(1);
                m.writer().writeInt(f.setInteger(p.body.get_hp_max(true)));
                m.writer().writeInt(f.setInteger(p.hp));
                m.writer().writeInt(f.setInteger(dame));
                m.writer().writeInt(f.setInteger(p.body.get_mp_max(true)));
                m.writer().writeInt(f.setInteger(p.mp));
                m.writer().writeInt(0);
                broadcastX = p.x;
                broadcastY = p.y;
                forcedIndex = p.index_map;
            } else {
                // Cả hai đều null — không có gì để gửi
                return;
            }

            this.send_msg_all_p_distance(m, broadcastX, broadcastY, false, 6, -1, forcedIndex);
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    private long[] Fire_Monster(Mob[] list_target, Player p, int idSkill, long dame)
            throws IOException {
        long[] exp_up = new long[] { 0, 0 };
        // [SAFETY] Bọc thép: Kiểm tra người tung chiêu
        if (p == null || p.isdie || p.skill_point == null || p.skill_point.isEmpty()) {
            return exp_up;
        }
        Skill_info sk_temp = p.get_skill_temp(idSkill);
        if (sk_temp == null) {
            return exp_up;
        }
        int dame_plus_percent = 0;
        long dame_magic_plus_percent = p.body.get_dame_ap();
        long crit_skill = p.body.get_crit(true);
        long multi_dame_skill = p.body.get_multi_dame_when_crit(true);
        boolean crit = (crit_skill) > Util.random(1000);
        List<Dame_Msg> list = new ArrayList<>();
        HashMap<Integer, Integer> id_mob_die = new HashMap<>(); // quest relative to mob
        //
        final long damebefore = dame;
        long dame2;
        for (int i = 0; i < list_target.length; i++) {
            Mob mob_target = list_target[i];
            if (mob_target != null && !mob_target.isdie && !p.isdie) {
                if (Math.abs(mob_target.x - p.x) > sk_temp.temp.rangeLan
                        || Math.abs(mob_target.y - p.y) > sk_temp.temp.rangeLan) {
                    // continue;
                }
                dame2 = damebefore;
                // [SAFETY] Dame quái dùng double
                dame2 = (long) ((double) dame2 * (1000.0 + dame_plus_percent) / 1000.0);
                // [SAFETY] p và skill_point đã được check ở đầu hàm, nhưng check thêm để tuyệt
                // đối an toàn
                if (p == null || p.skill_point == null || p.skill_point.isEmpty()) {
                    continue;
                }
                long baseDame = p.skill_point.get(0).get_dame(p);
                if (baseDame > 0) {
                    dame2 = (long) ((double) dame2 * sk_temp.get_dame(p) / (double) baseDame);
                }
                crit = core.Util.getStatDiminishing(Math.max(0, crit_skill)) > Util.random(1000);
                // long dame_exp = dame2;
                // [SAFETY] Chí mạng quái
                if (dame2 > 1 && crit) {
                    long multi = 1000L + multi_dame_skill;
                    dame2 = (long) ((double) dame2 * multi / 1000.0);
                }
                Dame_Msg dame_inf = new Dame_Msg();
                dame_inf.data = new ArrayList<>();
                dame_inf.targetM = mob_target;
                if (dame2 > 0 && idSkill != 0) {
                    // [SAFETY] Dame ma pháp quái
                    dame_inf.dameM = (long) ((double) p.get_skill_temp(idSkill).get_dame(p) * dame_magic_plus_percent
                            / 1000.0);
                }
                dame2 = (long) ((double) dame2 * (1000.0 + p.body.get_percent_final_dame()) / 1000.0);
                if (idSkill == 2038 || idSkill == 2041) {
                    // skill bien hinh bao dom, chim ung
                    if (p.get_eff(6) != null) {
                        dame2 = (dame2 * 115) / 100;
                    }
                    // fashion bao dom + chim ung
                    for (int i12 = 0; i12 < p.fashion.size(); i12++) {
                        if ((p.fashion.get(i12).id == 33 || p.fashion.get(i12).id == 34)
                                && p.fashion.get(i12).is_use) {
                            dame2 = (dame2 * 115) / 100;
                            break;
                        }
                    }
                }
                // --- BỔ SUNG LOGIC BIẾN THIÊN (DIFFERENTIAL SCALING) ---
                long raw_mob_miss = (5 + mob_target.level / 10) * 10; // Chuyển sang phần nghìn
                long raw_mob_mien = 0;
                long raw_mob_phan = 0;

                if (this.map_VoHanLienTang != null) {
                    raw_mob_miss = this.map_VoHanLienTang.getOptionValue(12, this.map_VoHanLienTang.floor) * 10;
                    raw_mob_mien = this.map_VoHanLienTang.getOptionValue(53, this.map_VoHanLienTang.floor) * 10 / 10; // Hồi
                                                                                                                      // đó
                                                                                                                      // chia
                                                                                                                      // 10,
                                                                                                                      // giờ
                                                                                                                      // nhân
                                                                                                                      // 10
                                                                                                                      // ->
                                                                                                                      // giữ
                                                                                                                      // nguyên
                    raw_mob_phan = this.map_VoHanLienTang.getOptionValue(14, this.map_VoHanLienTang.floor) * 10;
                }

                // 1. Né tránh (Dodge - Accuracy)
                long p_pha_ne = p.body.get_miss_reduce();
                long diff_miss = core.Util.getStatDiminishing(Math.max(0, raw_mob_miss - p_pha_ne));
                boolean miss = (diff_miss > Util.random(1000));

                if (this.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID
                        && (mob_target.mob_template.mob_id == BossEvent.BossEvent.BOSS_MAIN_ID
                                || BossEvent.BossEvent.isGuardian(mob_target.mob_template.mob_id))) {
                    miss = false;
                }

                if (miss) {
                    dame2 = 0; // 0 to show Miss/0 on client
                    dame_inf.dameM = 0; // Miss means no damage
                } else {
                    // 2. Miễn thương (Monster Skip)
                    long p_pha_mien = p.body.get_pierce_reduce();
                    long diff_mien = core.Util.getStatDiminishing(Math.max(0, raw_mob_mien - p_pha_mien));
                    if (diff_mien > 0) {
                        dame2 = (long) ((double) dame2 * Math.max(0.0, 1000.0 - (double) diff_mien) / 1000.0);
                    }

                    // 3. Phản đòn (Monster Reflect)
                    long p_pha_phan = p.body.get_dame_react_reduce();
                    long diff_phan = core.Util.getStatDiminishing(Math.max(0, raw_mob_phan - p_pha_phan));
                    if (diff_phan > Util.random(1000)) {
                        long reflectDame = (long) ((double) dame2 * (double) diff_phan / 1000.0);
                        p.hp -= reflectDame;
                        if (p.hp <= 0) {
                            p.hp = 1;
                        }
                        this.update_hp_mp_eff(p, null, 1, -reflectDame);
                    }
                }

                if (dame2 > 0) {
                    dame2 -= (dame2 * Util.random(10)) / 100;
                }

                if (!miss && ((mob_target.mob_template.mob_id >= 79 && mob_target.mob_template.mob_id <= 81)
                        || mob_target.mob_template.mob_id == 184 || mob_target.mob_template.mob_id == 154
                        || mob_target.mob_template.mob_id == 127)) {
                    dame2 = 1;
                    dame_inf.dameM = 0;
                }
                if (mob_target.mob_template.mob_id == 133) {
                    int destroyedSubTowers = 0;
                    for (int j = 0; j < this.list_mob.length; j++) {
                        Mob mobSelect = Mob.ENTRYS.get(this.list_mob[j]);
                        if (mobSelect.mob_template.mob_id == 132 && mobSelect.isdie) {
                            destroyedSubTowers++;
                        }
                    }
                    int maxDame = 1000 + destroyedSubTowers * 1000;
                    dame2 = Math.min(dame2, maxDame);
                    dame_inf.dameM = Math.min(dame_inf.dameM, maxDame);
                }

                if (mob_target.mob_template.mob_id == 133 || mob_target.mob_template.mob_id == 132) {
                    if (this.template.id == 303) {
                        if (p.clan == null) {
                            dame2 = 0;
                            dame_inf.dameM = 0;
                        } else {
                            Clan clanTop = ChiemDao.getClanTop(this.template.id);
                            if (clanTop != null && p.clan.equals(clanTop)) {
                                dame2 = 0;
                                dame_inf.dameM = 0;
                                Message m = new Message(17);
                                m.writer().writeShort(mob_target.index);
                                m.writer().writeByte(1);
                                m.writer().writeUTF("Đã chiếm đảo này rồi");
                                p.addmsg(m);
                                m.cleanup();
                            }
                        }
                    } else if (this.template.id == 304) {
                        if (ChiemDao.cachedHolderId304 == p.id) {
                            dame2 = 0;
                            dame_inf.dameM = 0;
                            Message m = new Message(17);
                            m.writer().writeShort(mob_target.index);
                            m.writer().writeByte(1);
                            m.writer().writeUTF("Đã chiếm đảo này rồi");
                            p.addmsg(m);
                            m.cleanup();
                        }
                    }
                }

                if (mob_target.mob_template.mob_id == 175) {
                    int maxDame = 50_000_000;
                    dame2 = Math.min(dame2, maxDame);
                    dame_inf.dameM = Math.min(dame_inf.dameM, maxDame);
                }

                if (mob_target.mob_template.mob_id == 132) {
                    int maxDame = 20;
                    dame2 = Math.min(dame2, maxDame);
                    dame_inf.dameM = Math.min(dame_inf.dameM, maxDame);
                }

                if (mob_target.mob_template.mob_id == 112) {
                    int maxDame = 500000;
                    dame2 = Math.min(dame2, maxDame);
                    dame_inf.dameM = Math.min(dame_inf.dameM, maxDame);
                }

                if (mob_target.mob_template.mob_id == 186) {
                    int maxDame = 1;
                    dame2 = Math.min(dame2, maxDame);
                    dame_inf.dameM = Math.min(dame_inf.dameM, maxDame);
                }

                long dame_to_target = Math.max(0, dame2) + Math.max(0, dame_inf.dameM);

                // Cổng 2: Balance Gateway - Kiểm soát sát thương cuối (hiển thị)
                dame_to_target = core.BalanceGateway.applyCombatScaling(p, mob_target, dame_to_target);

                if (mob_target.hp_max >= 2_000_000_000) {
                    String message = "HP : " + Util.number_format(mob_target.hp) + "\nDame : "
                            + Util.number_format(dame_to_target);
                    Message m = new Message(17);
                    m.writer().writeShort(mob_target.index);
                    m.writer().writeByte(1);
                    m.writer().writeUTF(message);
                    p.addmsg(m);
                    m.cleanup();
                }
                // BossEvent immunity check TRƯỚC, anti-1-hit SAU
                if (this.clan_resource != null) {
                    this.clan_resource.dame += dame_to_target;
                }
                dame_to_target = BossEvent.BossEvent.getInstance().onPlayerAttackBoss(p, mob_target,
                        dame_to_target);

                // Anti-1-hit: chỉ áp dụng cho mob KHÔNG phải boss event
                if (mob_target.hp == mob_target.hp_max
                        && dame_to_target >= mob_target.hp
                        && mob_target.hp_max > 1
                        && mob_target.mob_template.mob_id != BossEvent.BossEvent.BOSS_MAIN_ID
                        && !BossEvent.BossEvent.isGuardian(mob_target.mob_template.mob_id)) {
                    mob_target.hp = 1;
                } else {
                    if (dame2 <= 0 && dame_inf.dameM <= 0) {
                        dame_to_target = 0;
                    } else {
                        dame_to_target = Math.max(0, dame2) + Math.max(0, dame_inf.dameM);
                    }

                    // Cổng 2: Balance Gateway - Kiểm soát sát thương cuối (thực trừ)
                    dame_to_target = core.BalanceGateway.applyCombatScaling(p, mob_target, dame_to_target);

                    mob_target.hp -= dame_to_target;
                }

                // Force death cho boss event mobs bị giữ ở HP=1 bởi engine floor
                if (mob_target.hp <= 1
                        && dame_to_target > 0
                        && (mob_target.mob_template.mob_id == BossEvent.BossEvent.BOSS_MAIN_ID
                                || BossEvent.BossEvent.isGuardian(mob_target.mob_template.mob_id))) {
                    mob_target.hp = 0;
                }

                dame_inf.dameP = dame2;
                if (!p.isdie) {
                    mob_target.id_target = p.index_map;
                }
                if (mob_target.hp <= 0 && !mob_target.isdie) {
                    mob_target.hp = 0;
                    mob_target.isdie = true;

                    // [SECURITY] Transfer killer ownership from Clone to Master for rewards
                    Player p_killer = p;
                    if (p_killer instanceof Disciple) {
                        p_killer = ((Disciple) p_killer).master;
                    }

                    activities.WishManager.gI().checkReward(p_killer, mob_target);
                    if (mob_target.mob_template.mob_id == BossEvent.BossEvent.BOSS_MAIN_ID) {
                        BossEvent.BossEvent.getInstance().onBossDeath(p_killer, this);
                    } else {
                        BossEvent.BossEvent.getInstance().onGuardianDeath(mob_target, this);
                    }
                    int respawnTime;

                    if (mob_target.isDynamic) {
                        respawnTime = 0;
                    } else if (is_boss(mob_target.mob_template.mob_id)) {
                        respawnTime = 5; // Nhanh: 5 giây cho Boss
                        if (Map.is_map_treo(mob_target.map.template.id)) {
                            Player.updateDameBossSQL(1, p_killer.name, 1);
                        }
                    } else if (Map.is_map_treo(mob_target.map.template.id)) {
                        Player.updateDameBossSQL(1, p_killer.name, 1);
                        respawnTime = 60;
                    } else if (Map.is_map_banh_kem(mob_target.map.template.id)) {
                        respawnTime = 30;
                    } else {
                        respawnTime = Skill_info.GLOBAL_MOB_RESPAWN_TIME;
                    }

                    mob_target.id_target = -1;

                    mob_target.time_refresh = System.currentTimeMillis() + respawnTime * 1000;
                    exp_up[1] += (long) ((double) mob_target.level * 2 * Skill_info.SKILL_EXP_RATE);
                    //
                    if (this.map_ThuThachVeThan != null) {
                        this.map_ThuThachVeThan.mobs.remove(mob_target);
                        this.map_ThuThachVeThan.listRemoveMap.put(mob_target,
                                (System.currentTimeMillis() + 1_500));
                    }
                    if (this.map_Hang != null) {
                        this.map_Hang.mobs.remove(mob_target);
                        this.map_Hang.listRemoveMap.put(mob_target, (System.currentTimeMillis() + 1_500));
                    }
                    // update quest relative to
                    if (!id_mob_die.containsKey((int) mob_target.mob_template.mob_id)) {
                        id_mob_die.put((int) mob_target.mob_template.mob_id, 1);
                    } else {
                        int oldvalue = id_mob_die.get((int) mob_target.mob_template.mob_id);
                        id_mob_die.replace((int) mob_target.mob_template.mob_id, oldvalue,
                                oldvalue + 1);
                    }
                    if (this.map_little_garden != null && !this.map_little_garden.is_finish
                            && (p_killer.type_pk == 4 || p_killer.type_pk == 5)) {
                        LeaveItemMap.leave_item4_little_garden(this, mob_target, p_killer);
                    }
                    // boss
                    int mobId = mob_target.mob_template.mob_id;
                    String mobName = mob_target.mob_template.name;
                    List<GiftBox> giftForKiller = null;
                    List<GiftBox> giftForOthers = null;

                    // NEW: Check ItemDropTool for customized rewards
                    List<GiftBox> customRewards = ItemDropTool.getCustomBossRewards(p_killer, mobId);
                    if (customRewards != null) {
                        if (ItemDropTool.isSharedToArea(mobId)) {
                            // Share with everyone in area
                            List<Player> snapshotCustom;
                            synchronized (mob_target.map.players) {
                                snapshotCustom = new ArrayList<>(mob_target.map.players);
                            }
                            for (Player p0 : snapshotCustom) {
                                if (p0 == null || p0.conn == null || p0.isdie)
                                    continue;
                                Service.send_gift(p0, 0, "Phần thưởng " + mobName, "Phần thưởng", customRewards, true);
                            }
                        } else {
                            // Last hit only
                            Service.send_gift(p_killer, 0, "Tiêu Diệt " + mobName, "Phần thưởng", customRewards, true);
                        }
                    } else {
                        // FALLBACK: boss the gioi (Old Hardcoded Logic)
                        if (mobId == 182) { // Jinbei
                            giftForKiller = GiftBox.get_gift_boss_pica(p_killer);
                        } else if (mobId == 179) { // Chúa Tể Bóng Đêm
                            giftForKiller = GiftBox.get_gift_boss_clan(p_killer);
                        } else if (mobId == 185) { // Xà Nữ
                            giftForKiller = GiftBox.get_gift_boss_xa_nu(p_killer);
                        } else if (mobId == 127) { // Bánh sinh nhật
                            giftForKiller = GiftBox.get_gift_boss_banh_kem(p_killer);
                        } else if (mobId == 175) { // Siêu Lucci
                            giftForKiller = GiftBox.get_gift_boss_sieu_trum(p_killer);
                        } else if (mobId == 184 || (this.template.id == 310 && !is_boss(mobId))) { // Đá linh hồn
                            giftForKiller = GiftBox.get_gift_farm_da(p_killer);
                        } else if (mobId == 186) { // Đá linh hồn 2
                            giftForKiller = GiftBox.get_gift_farm_da2(p_killer);
                        } else if (mobId == 120) { // Rắn trời
                            giftForKiller = GiftBox.get_gift_farm_ran(p_killer);
                        }

                        if (giftForKiller != null) {
                            Service.send_gift(p_killer, 0, "Tiêu Diệt " + mobName, "Phần thưởng", giftForKiller, true);

                            try {
                                List<Player> snapshot;
                                synchronized (mob_target.map.players) {
                                    snapshot = new ArrayList<>(mob_target.map.players);
                                }
                                for (Player p0 : snapshot) {
                                    if (p0 == null || p0.conn == null || p0.isdie
                                            || (p_killer != null && p0.id == p_killer.id)) {
                                        continue;
                                    }

                                    if (mobId == 182) {
                                        giftForOthers = GiftBox.get_gift_boss_pica_other(p0);
                                    } else if (mobId == 179) {
                                        giftForOthers = GiftBox.get_gift_boss_clan_other(p0);
                                    } else if (mobId == 185) {
                                        giftForOthers = GiftBox.get_gift_boss_xa_nu_other(p0);
                                    } else if (mobId == 127) {
                                        giftForOthers = GiftBox.get_gift_boss_banh_kem_other(p0);
                                    } else if (mobId == 175) {
                                        giftForOthers = GiftBox.get_gift_boss_sieu_trum_other(p0);
                                    }

                                    if (giftForOthers != null) {
                                        Service.send_gift(p0, 0, "Phần thưởng Săn " + mobName,
                                                "Phần thưởng", giftForOthers, true);
                                    }
                                }
                            } catch (Exception e) {
                                core.GameLogger.error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                            }
                        }
                    }

                    if (mob_target.mob_template.mob_id == 133) {
                        if (this.template.id == 303 && p.clan != null) {
                            ChiemDao.setClanTop(this.template.id, p.clan);
                            Clan clanTop = ChiemDao.getClanTop(this.template.id);
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
                                this.send_msg_all_p(mm, null, true);
                                mm.cleanup();
                                if (!ChiemDao.isRewardTaskRunning()) {
                                    ChiemDao.startRewardTask();
                                }
                                String mapName = "Đảo";
                                Map[] mapT = Map.get_map_by_id(this.template.id);
                                if (mapT != null && mapT.length > 0) {
                                    mapName = mapT[0].template.name;
                                }
                                Manager.gI().chatKTG(0, "Băng " + p.clan.name + " đã chiếm được "
                                        + mapName, 5);
                                try {
                                    for (int j = 0; j < this.players.size(); j++) {
                                        Player p0 = this.players.get(j);
                                        if (p0 != null && p0.map == this) {
                                            this.change_flag(p0, -1);
                                        }
                                    }
                                } catch (Exception e) {
                                    core.GameLogger
                                            .error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                                }
                            }
                        } else if (this.template.id == 304) {
                            ChiemDao.onPlayerCapture304(p);
                            if (!ChiemDao.isRewardTaskRunning()) {
                                ChiemDao.startRewardTask();
                            }
                        }
                    }

                }
                // update exxp
                // Injected by StatsTool
                long exp_up_add = (long) ((20) * Skill_info.GLOBAL_EXP_RATE);
                exp_up_add += (long) (dame_inf.dameP * 0.0001); // Thêm 0.01% Dame vào EXP theo yêu cầu
                exp_up[0] += exp_up_add;
                if (crit) {
                    dame_inf.data.add(new Option_Dame_Msg(1010, (int) dame_inf.dameP, 0));
                }
                list.add(dame_inf);
            }
        }
        if (list.size() > 0)

        {
            this.send_dame_msg(p, sk_temp.get_eff_skill(), list);
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).targetM.isdie) {
                this.die_mob(list.get(i).targetM, p);
                for (int ix = 1; ix <= 6; ix++) {
                    p.updateArchiDaily(ix);
                }
            }
        }
        //
        EffTemplate eff = p.get_eff(2);
        if (eff != null) {
            exp_up[0] *= 2;
        }
        eff = p.get_eff(17);
        if (eff != null) {
            exp_up[0] *= 2;
        }
        // update quest
        if (id_mob_die.size() > 0) {
            for (java.util.Map.Entry<Integer, Integer> en : id_mob_die.entrySet()) {
                int id_mob = en.getKey();
                p.update_num_item_quest(1, id_mob, en.getValue());
            }
        }
        return exp_up;
    }

    private void die_mob(Mob targetM, Player p_killer) throws IOException {
        Message m = new Message(7);
        try {
            m.writer().writeShort(p_killer.index_map);
            m.writer().writeByte(0);
            m.writer().writeShort(targetM.index);
            m.writer().writeByte(1);
            m.writer().writeShort(0); // point pk
            this.send_msg_all_p_distance(m, targetM.x, targetM.y, false, 6, -1, p_killer.index_map);
        } finally {
            // "Bảo hiểm" RAM: Đảm bảo dọn dẹp Message
            if (m != null) {
                m.cleanup();
            }
        }
    }

    private void send_dame_msg(Player p, short typeEffSkill, List<Dame_Msg> list) throws IOException {
        Message m = new Message(100);
        try {
            m.writer().writeShort(p.index_map);
            m.writer().writeByte(0);
            m.writer().writeInt(f.setInteger(p.hp));
            m.writer().writeInt(f.setInteger(p.mp));
            // typeEffSkill = (short) Map.id_eff;
            m.writer().writeShort(typeEffSkill);
            m.writer().writeByte(list.size());

            // [BƯỚC 1]: Đếm xem có bao nhiêu người chơi bị trúng đòn để chuẩn bị vé VIP
            int playerTargetCount = 0;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).targetP != null) {
                    playerTargetCount++;
                }
            }

            // [BƯỚC 2]: Tạo danh sách VIP (Kích thước = 1 người đánh + số người bị đánh)
            int[] forced_indices = new int[1 + playerTargetCount];
            forced_indices[0] = p.index_map; // VIP đầu tiên luôn là người ra đòn
            int forced_idx = 1;

            // [BƯỚC 3]: Xử lý ghi dữ liệu gói tin và nhét nạn nhân vào danh sách VIP
            for (int j = 0; j < list.size(); j++) {
                Dame_Msg temp = list.get(j);
                if (temp.targetM != null) {
                    // ĐÁNH TRÚNG QUÁI (Quái là AI nên không cần thêm vào VIP)
                    m.writer().writeShort(temp.targetM.index);
                    m.writer().writeByte(1);
                    m.writer().writeInt(f.setInteger(temp.dameP));
                    m.writer().writeInt(f.setInteger(temp.dameM)); // dame plus
                    m.writer().writeInt(f.setInteger(temp.targetM.hp));
                    m.writer().writeByte(temp.data.size());
                    for (int i = 0; i < temp.data.size(); i++) {
                        m.writer().writeShort(temp.data.get(i).type);
                        m.writer().writeShort(temp.data.get(i).hp);
                        m.writer().writeShort(temp.data.get(i).time);
                    }
                } else {
                    // ĐÁNH TRÚNG NGƯỜI
                    m.writer().writeShort(temp.targetP.index_map);
                    m.writer().writeByte(0);
                    m.writer().writeInt(f.setInteger(temp.dameP));
                    m.writer().writeInt(f.setInteger(temp.dameM)); // dame plus
                    m.writer().writeInt(f.setInteger(temp.targetP.hp));
                    //
                    m.writer().writeByte(temp.data.size());
                    for (int i = 0; i < temp.data.size(); i++) {
                        m.writer().writeShort(temp.data.get(i).type);
                        m.writer().writeShort(temp.data.get(i).hp);
                        m.writer().writeShort(temp.data.get(i).time);
                    }

                    // Thêm nạn nhân vào danh sách VIP để họ BẮT BUỘC thấy sát thương
                    forced_indices[forced_idx++] = temp.targetP.index_map;
                }
            }

            // [BƯỚC 4: XUẤT GÓI TIN ĐỈNH CAO]
            // - p.x, p.y: Tâm điểm vụ nổ
            // - ignore_index = -1: Không bỏ qua ai cả (Cho phép mọi người xem nếu đứng gần)
            // - VIP = Mảng forced_indices vừa gom ở trên
            this.send_msg_all_p_distance(m, p.x, p.y, false, 6, -1, forced_indices);
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    // public void send_msg_all_p(Message m, Player p, boolean all) throws
    // IOException {
    // if (players.isEmpty())
    // return;
    // Player[] array;
    // synchronized (players) {
    // array = players.toArray(new Player[0]);
    // }
    // for (Player p0 : array) {
    // if (p0 != null && p0.conn != null) {
    // if (all || (p != null && p0.index_map != p.index_map)) {
    // p0.addmsg(m);
    // }
    // }
    // }
    // }

    public void send_msg_all_p(Message m, Player p, boolean all) throws IOException {
        for (Player p0 : players) {
            Player p0_local = p0; // [SHADOWING PLAYER]
            if (p0_local == null)
                continue;

            io.Session localConn = p0_local.conn; // [SHADOWING SESSION]
            if (localConn != null) {
                if (all || (p != null && p0_local.index_map != p.index_map)) {
                    localConn.addmsg(m);
                }
            }
        }
    }

    public void send_chat(Player p, Message m2) throws IOException {
        String s = m2.reader().readUTF();

        // [ADMIN COMMANDS] Boss Summoning
        if (p.getAdmin() >= 1 || core.Manager.gI().server_admin) {
            String cmd_raw = s.trim().toLowerCase();
            if (cmd_raw.startsWith("boss ")) {
                String[] prm = cmd_raw.split("\\s+");
                if (prm.length >= 3) {
                    try {
                        int idBoss = Integer.parseInt(prm[1]);
                        long hpBoss = Long.parseLong(prm[2]);
                        core.BossManager.callBoss(p.map, idBoss, hpBoss, p.x, p.y);
                        Service.send_box_ThongBao_OK(p,
                                "Triệu hồi Boss (ID: " + idBoss + ") thành công tại khu vực này.");
                    } catch (Exception e) {
                        Service.send_box_ThongBao_OK(p, "Lỗi cú pháp: boss [id] [hp]");
                    }
                    return;
                }
            } else if (cmd_raw.startsWith("bossmap ")) {
                String[] prm = cmd_raw.split("\\s+");
                if (prm.length >= 4) {
                    try {
                        int mapTarget = Integer.parseInt(prm[1]);
                        int idBoss = Integer.parseInt(prm[2]);
                        long hpBoss = Long.parseLong(prm[3]);

                        // Kiểm tra map tồn tại
                        Map[] maps = Map.get_map_by_id(mapTarget);
                        if (maps == null || maps.length == 0) {
                            Service.send_box_ThongBao_OK(p, "Không tìm thấy Map ID: " + mapTarget);
                            return;
                        }

                        short xSummon = (short) (prm.length >= 6 ? Integer.parseInt(prm[4])
                                : maps[0].template.maxW / 2);
                        short ySummon = (short) (prm.length >= 6 ? Integer.parseInt(prm[5])
                                : maps[0].template.maxH / 2);

                        core.BossManager.callBoss(mapTarget, idBoss, hpBoss, xSummon, ySummon);
                        Service.send_box_ThongBao_OK(p,
                                "Triệu hồi Boss (ID: " + idBoss + ") tại Map " + maps[0].template.name + " ("
                                        + mapTarget
                                        + ") thành công tại tọa độ (" + xSummon + "," + ySummon + ")");
                    } catch (Exception e) {
                        Service.send_box_ThongBao_OK(p, "Lỗi cú pháp: bossmap [mapId] [bossId] [hp] [x] [y]");
                    }
                    return;
                }
            } else if (cmd_raw.equals("autospawn")) {
                core.Manager.gI().isAutoSpawn = !core.Manager.gI().isAutoSpawn;
                Service.send_box_ThongBao_OK(p, "Chế độ tự động spawn Boss: "
                        + (core.Manager.gI().isAutoSpawn ? "BẬT" : "TẮT"));
                return;
            }
        }
        // Chat Commands for Disciple (Tối ưu: trim và ignoreCase)
        String cmd = s.trim().toLowerCase();
        if (p.myDisciple != null && (cmd.equals("trieuhoi") || cmd.equals("venha") || cmd.equals("tancong")
                || cmd.equals("ditheo") || cmd.equals("baove") || cmd.equals("info")
                || cmd.equals("hsd") || cmd.equals("dd") || cmd.equals("fusion"))) {
            if (p.fusionType != 0 && !cmd.equals("fusion")) {
                Service.send_box_ThongBao_OK(p, "Bạn đang trong trạng thái Hợp thể, không thể điều khiển đệ tử!");
                return;
            }
            if (cmd.equals("trieuhoi")) {
                if (p.myDisciple.isMapCamDeTu(p.map)) {
                    Service.send_box_ThongBao_OK(p, "Khu vực này không thể triệu hồi đệ tử.");
                } else if (p.myDisciple.isdie) {
                    Service.send_box_ThongBao_OK(p, "Đệ tử đang trọng thương, cần nghỉ ngơi thêm.");
                } else if (p.myDisciple.status == 2) {
                    p.myDisciple.status = 0;
                    p.myDisciple.x = p.x;
                    p.myDisciple.y = p.y;
                    p.myDisciple.map = p.map;
                    Message m = new Message(1);
                    m.writer().writeByte(0); // là người
                    m.writer().writeShort(p.myDisciple.index_map);
                    m.writer().writeShort(p.myDisciple.x);
                    m.writer().writeShort(p.myDisciple.y);
                    this.send_msg_all_p(m, null, true);
                    m.cleanup();
                    // Gửi thông tin chi tiết đệ tử cho tất cả mọi người trong map
                    for (Player p0 : players) {
                        if (p0 != null && p0.conn != null) {
                            this.send_char_in4_inmap(p0, p.myDisciple.index_map);
                        }
                    }
                    Service.send_box_ThongBao_OK(p, "Đệ tử đã xuất hiện!");
                    p.myDisciple.chat("Con có mặt thưa sư phụ!");
                } else {
                    // Nếu đã ở ngoài thì Teleport về cạnh chủ
                    p.myDisciple.x = p.x;
                    p.myDisciple.y = p.y;
                    try {
                        this.send_move(p.myDisciple, p.x, p.y);
                    } catch (Exception e) {
                        core.GameLogger.error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                    }
                    Service.send_box_ThongBao_OK(p, "Đệ tử đã được gọi về cạnh bạn.");
                    p.myDisciple.chat("Con có mặt thưa sư phụ!");
                }
            } else if (cmd.equals("fusion")) {
                if (p.fusionType == 1) {
                    Service.send_box_ThongBao_OK(p, "Bạn đang trong trạng thái hợp thể tạm thời rồi!");
                } else if (p.fusionType == 2) {
                    Service.send_box_ThongBao_OK(p,
                            "Bạn đang trong trạng thái hợp thể vĩnh viễn, hãy dùng vật phẩm để tách!");
                } else {
                    p.startFusion(1800, 1); // Type 1 = Fusion tạm thời (30 phút)
                    Service.send_box_ThongBao_OK(p, "Dung hợp thành công! Hiệu lực trong 30 phút.");
                }
            } else if (cmd.equals("venha")) {
                if (p.myDisciple.status != 2) {
                    p.myDisciple.status = 2; // ve
                    p.myDisciple.stopFighting();
                    Message m = new Message(3);
                    m.writer().writeShort(p.myDisciple.index_map);
                    m.writer().writeByte(0);
                    this.send_msg_all_p(m, null, true);
                    m.cleanup();
                    Service.send_box_ThongBao_OK(p, "Đệ tử đã về nhà nghỉ ngơi.");
                    p.myDisciple.chat("Oki con về, bibi sư phụ!");
                }
            } else if (cmd.equals("tancong")) {
                p.myDisciple.status = 1;
                p.myDisciple.stopFighting();
                Service.send_box_ThongBao_OK(p, "Đệ tử bắt đầu đánh quái.");
                p.myDisciple.chat("Sư phụ cứ để con lo!");
            } else if (cmd.equals("ditheo")) {
                p.myDisciple.status = 0;
                p.myDisciple.stopFighting();
                Service.send_box_ThongBao_OK(p, "Đệ tử sẽ đi theo bạn.");
                p.myDisciple.chat("Con sẽ bảo vệ sư phụ!");
            } else if (cmd.equals("baove")) {
                p.myDisciple.status = 3;
                p.myDisciple.stopFighting();
                Service.send_box_ThongBao_OK(p, "Đệ tử đã sẵn sàng bảo vệ bạn.");
                p.myDisciple.chat("Thằng nào dám động đến sư phụ tao?");
            } else if (cmd.equals("info")) {
                core.MenuController.send_disciple_menu(p);
            } else if (cmd.equals("hsd")) {
                if (p.getAdmin() > 0) {
                    if (p.myDisciple.isMapCamDeTu(p.map)) {
                        Service.send_box_ThongBao_OK(p, "Khu vực này không thể triệu hồi đệ tử.");
                        return;
                    }
                    p.myDisciple.isdie = false;
                    p.myDisciple.hp = p.myDisciple.body.get_hp_max(true);
                    p.myDisciple.mp = p.myDisciple.body.get_mp_max(true);
                    p.myDisciple.x = p.x;
                    p.myDisciple.y = p.y;
                    p.myDisciple.map = p.map;
                    p.myDisciple.status = 0; // Đi theo
                    // Gửi gói tin xuất hiện
                    Message m = new Message(1);
                    m.writer().writeByte(0); // là người
                    m.writer().writeShort(p.myDisciple.index_map);
                    m.writer().writeShort(p.myDisciple.x);
                    m.writer().writeShort(p.myDisciple.y);
                    this.send_msg_all_p(m, null, true);
                    m.cleanup();
                    for (Player p0 : players) {
                        if (p0 != null && p0.conn != null) {
                            this.send_char_in4_inmap(p0, p.myDisciple.index_map);
                        }
                    }
                    Service.send_box_ThongBao_OK(p, "Admin đã hồi sinh nhanh đệ tử!");
                    p.myDisciple.chat("Sao không chat sớm?");
                } else {
                    Service.send_box_ThongBao_OK(p, "Lệnh này chỉ dành cho Admin.");
                }
            } else if (cmd.equals("dd")) {
                if (p.getAdmin() > 0) {
                    if (p.myDisciple != null && p.myDisciple.isMapCamDeTu(p.map)) {
                        Service.send_box_ThongBao_OK(p, "Khu vực này không thể triệu hồi đệ tử.");
                        return;
                    }
                    // Loại bỏ đệ tử cũ khỏi map và gửi packet xóa cho mọi người
                    if (p.myDisciple != null) {
                        try {
                            Message m_out = new Message(3);
                            m_out.writer().writeShort(p.myDisciple.index_map);
                            this.send_msg_all_p(m_out, null, true);
                            m_out.cleanup();
                        } catch (Exception e) {
                            core.GameLogger.error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                        }
                        this.leave_map(p.myDisciple, 0);
                    }

                    // Khởi tạo đệ tử mới hoàn toàn
                    p.myDisciple = new Disciple(p);
                    p.myDisciple.x = (short) (p.x + 30);
                    p.myDisciple.y = p.y;
                    p.myDisciple.map = p.map;
                    p.myDisciple.status = 0; // Theo đuôi

                    // Thêm đệ tử mới vào danh sách người chơi trong map
                    if (!players.contains(p.myDisciple)) {
                        players.add(p.myDisciple);
                    }

                    // Gửi gói tin xuất hiện cho mọi người (Message 1)
                    try {
                        Message m_appear = new Message(1);
                        m_appear.writer().writeByte(0); // là người
                        m_appear.writer().writeShort(p.myDisciple.index_map);
                        m_appear.writer().writeShort(p.myDisciple.x);
                        m_appear.writer().writeShort(p.myDisciple.y);
                        this.send_msg_all_p(m_appear, null, true);
                        m_appear.cleanup();
                    } catch (Exception e) {
                        core.GameLogger.error("[Disconnect] Map.update_skill_player error: " + e.getMessage());
                    }

                    // Đồng bộ hiển thị đệ tử mới cho tất cả người chơi
                    for (Player p0 : players) {
                        if (p0 != null && p0.conn != null) {
                            this.send_char_in4_inmap(p0, p.myDisciple.index_map);
                        }
                    }
                    Service.send_box_ThongBao_OK(p, "Đã hủy đệ cũ và tạo đệ mới (Phái: " + p.myDisciple.clazz + ")");
                } else {
                    Service.send_box_ThongBao_OK(p, "Lệnh này chỉ dành cho Admin.");
                }
            }
            return;
        }

        if (s.startsWith("weather ")) {
            if (p.getAdmin() == 1 || Manager.gI().server_test) {
                try {
                    String[] arr = s.split(" ");
                    byte type = Byte.parseByte(arr[1]);
                    byte level = arr.length > 2 ? Byte.parseByte(arr[2]) : 1;
                    Map.weather = type;
                    Map.weather_level = level;
                    for (Map[] maps : Map.ENTRYS) {
                        for (Map map : maps) {
                            List<Player> snapshot;
                            synchronized (map.players) {
                                snapshot = new ArrayList<>(map.players);
                            }
                            for (Player p0 : snapshot) {
                                map.send_weather(p0);
                            }
                        }
                    }
                    Service.send_box_ThongBao_OK(p, "Đổi thời tiết thành " + type + ", level " + level);
                } catch (Exception e) {
                    Service.send_box_ThongBao_OK(p, "Sai cú pháp! Ví dụ: weather 3 2 (type 3, level 2)");
                }
            }
            return;
        }

        if (s.equals("congtien")) {
            if (p.getAdmin() == 1 || Manager.gI().server_test) {
                if (p.get_vang() < 100_000_000_000L) {
                    p.update_vang(50_000_000_000L);
                }
                p.update_ngoc(1_000_000_000);
                p.update_vnd(1_000_000_000);
                p.update_coin(1_000_000_000);
                p.update_money();
            }
        } else if (s.equals("reload")) {
            if (p.getAdmin() == 1) {
                Manager.gI().reloadConfig();
                Manager.gI().load_parts();
                Service.send_box_ThongBao_OK(p, "Đã nạp lại cấu hình Balance thành công!");
            }
        } else if (s.equalsIgnoreCase("taixiu")) {
            taixiu.TaiXiuManager.xuLyLenh(p);
        } else if (s.equalsIgnoreCase("blackjack") || s.equalsIgnoreCase("bj")) {
            taixiu.BlackjackManager.showMainMenu(p);
        } else if (s.equalsIgnoreCase("luattaixiu")) {
            taixiu.TaiXiuManager.guiHuongDan(p);
        } else if (s.equalsIgnoreCase("baucua")) {
            taixiu.BauCuaManager.xuLyLenh(p);
        } else if (s.equalsIgnoreCase("luatbaucua")) {
            taixiu.BauCuaManager.guiHuongDan(p);
        } else if (s.equalsIgnoreCase("but oi")) {
            butoi.ButOi.processChat(p);
        } else if (s.equalsIgnoreCase("tt")) {
            String info = "--- Thông số " + p.name + " ---\n"
                    + "HP: " + Util.number_format(p.hp) + " / " + Util.number_format(p.body.get_hp_max(true)) + "\n"
                    + "MP: " + Util.number_format(p.mp) + " / " + Util.number_format(p.body.get_mp_max(true)) + "\n"
                    + "Sát thương: " + Util.number_format(p.body.get_dame(true));
            Service.send_box_ThongBao_OK(p, info);
        } else if (s.equalsIgnoreCase("ktg")) {
            p.isHideKTG = !p.isHideKTG;
            Service.send_box_ThongBao_OK(p,
                    p.isHideKTG ? "Đã tắt thông báo kênh thế giới" : "Đã bật thông báo kênh thế giới");
        } else if (s.equalsIgnoreCase("check")) {
            if (p.item.total_item_bag_by_id(4, 194) > 0) {
                Service.send_box_yesno(p, 194, "Thông báo", "Bạn có muốn sử dụng Chim để chỉ hướng không?",
                        new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
            } else {
                Service.send_box_ThongBao_OK(p, "Bạn không có Chim!");
            }
        } else if (s.startsWith("e") && s.length() < 6 && s.matches("e\\d+")) {
            // Chỉ nhận khi bắt đầu bằng 'e' và theo sau là các con số (VD: e12, e999)
            try {
                short id = Short.parseShort(s.replace("e", ""));
                sendEffect(p, id);
            } catch (NumberFormatException e) {
                core.GameLogger.error("[Disconnect] Map.handleChat error: " + e.getMessage());
            }
        } else if (p.getAdmin() == 1 && s.startsWith("eff")) {
            String[] parts = s.split(" ");
            if (parts.length == 2) {
                try {
                    int effId = Integer.parseInt(parts[1]);
                    Message m3 = new Message(74);
                    m3.writer().writeByte(1);
                    m3.writer().writeShort(p.index_map);
                    m3.writer().writeShort(effId); // id Eff
                    m3.writer().writeInt(10_000); // time, giữ nguyên
                    m3.writer().writeByte(0); // type move, giữ nguyên
                    m3.writer().writeByte(20); // loop, giữ nguyên
                    this.send_msg_all_p(m3, null, true);
                    m3.cleanup();
                } catch (NumberFormatException e) {
                    core.GameLogger.info("Invalid effId. Use format: eff <id>");
                }
            } else {
                core.GameLogger.info("Invalid eff command. Use format: eff <id>");
            }
        } else if (s.equalsIgnoreCase("bua")) {
            UseItem.use_item_7(p, 12);
        } else if (s.equals("tb2")) {

            Service.send_view_second_body(p, p);
        } else if (s.equals("thongtin")) {
            long dame = p.body.get_dame(true);
            long hp = p.hp;
            long hpMax = p.body.get_hp_max(true);
            long mp = p.mp;
            long mpMax = p.body.get_mp_max(true);
            long def = p.body.get_def(true);
            long def_percent = p.body.get_def_percent(true);
            String info = "== Thông Tin ==\n__" + p.name
                    + "__\nDame: " + Util.number_format(dame)
                    + "\nHP: " + Util.number_format(hp) + " / " + Util.number_format(hpMax)
                    + "\nMP: " + Util.number_format(mp) + " / " + Util.number_format(mpMax)
                    + "\nDef: " + Util.number_format(def) + " (" + Util.number_format(def_percent) + ")";
            Service.send_box_ThongBao_OK(p, info);

        } else if (p.getAdmin() == 1 && s.startsWith("af")) {
            String[] parts = s.split(" ");
            if (parts.length == 3) {
                String account = parts[1];
                try {
                    short fashionId = Short.parseShort(parts[2]);
                    Player target = null;
                    synchronized (io.SessionManager.CLIENT_ENTRYS) {
                        for (io.Session ss : io.SessionManager.CLIENT_ENTRYS) {
                            if (ss != null && ss.user != null && ss.user.equalsIgnoreCase(account)) {
                                target = ss.p;
                                break;
                            }
                        }
                    }
                    if (target != null) {
                        if (target.check_fashion(fashionId) == null) {
                            ItemFashionP2 newFashion = new ItemFashionP2();
                            newFashion.id = fashionId;
                            newFashion.is_use = false;
                            newFashion.level = 0;
                            newFashion.expirationTime = -1; // Permanent
                            newFashion.op = ItemFashionP2.randomOptionsForFashion(fashionId);
                            target.fashion.add(newFashion);

                            Service.send_box_ThongBao_OK(p,
                                    "Đã trao thời trang " + fashionId + " cho tài khoản " + account);
                            Service.send_box_ThongBao_OK(target, "Bạn nhận được thời trang mới từ Admin!");
                            Player.flush(target, false);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Tài khoản " + account + " đã sở hữu thời trang này.");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Không tìm thấy tài khoản " + account + " đang trực tuyến.");
                    }
                } catch (NumberFormatException e) {
                    Service.send_box_ThongBao_OK(p, "ID thời trang phải là số.");
                }
            }
        } else if (p.getAdmin() == 1 && s.startsWith("rf")) {
            String[] parts = s.split(" ");
            if (parts.length == 3) {
                String account = parts[1];
                try {
                    short fashionId = Short.parseShort(parts[2]);
                    Player target = null;
                    synchronized (io.SessionManager.CLIENT_ENTRYS) {
                        for (io.Session ss : io.SessionManager.CLIENT_ENTRYS) {
                            if (ss != null && ss.user != null && ss.user.equalsIgnoreCase(account)) {
                                target = ss.p;
                                break;
                            }
                        }
                    }
                    if (target != null) {
                        ItemFashionP2 fas = target.check_fashion(fashionId);
                        if (fas != null) {
                            if (fas.is_use) {
                                fas.is_use = false;
                                target.setin4(); // Refresh stats
                            }
                            target.fashion.remove(fas);
                            Service.send_box_ThongBao_OK(p,
                                    "Đã thu hồi thời trang " + fashionId + " từ tài khoản " + account);
                            Service.send_box_ThongBao_OK(target,
                                    "Admin đã thu hồi thời trang " + fashionId + " của bạn.");
                            Player.flush(target, false);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Tài khoản " + account + " không có thời trang này.");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Không tìm thấy tài khoản " + account + " đang trực tuyến.");
                    }
                } catch (NumberFormatException e) {
                    Service.send_box_ThongBao_OK(p, "ID thời trang phải là số.");
                }
            }
        } else if (p.getAdmin() == 1 && s.startsWith("i")) {
            String[] parts = s.trim().split(" ");
            if (parts.length >= 3) {
                try {
                    int type = Integer.parseInt(parts[0].substring(1));
                    int amount = Integer.parseInt(parts[parts.length - 1]);
                    if (amount <= 0 || amount > 32000) {
                        amount = 1;
                    }
                    List<Integer> validIds = new ArrayList<>();
                    List<Integer> invalidIds = new ArrayList<>();
                    for (int i = 1; i < parts.length - 1; i++) {
                        int id = Integer.parseInt(parts[i]);
                        boolean exists = false;
                        switch (type) {
                            case 3: {
                                ItemTemplate3 temp = ItemTemplate3.get_it_by_id(id);
                                if (temp != null) {
                                    exists = true;
                                    Item_wear it_add = new Item_wear();
                                    it_add.setup_template_by_id(temp);
                                    if (it_add.template != null) {
                                        p.item.add_item_bag3(it_add);
                                    }
                                }
                                break;
                            }
                            case 4: {
                                ItemTemplate4 temp = ItemTemplate4.get_it_by_id(id);
                                if (temp != null) {
                                    exists = true;
                                    p.item.add_item_bag47(4, temp.id, amount);
                                }
                                break;
                            }
                            case 7: {
                                ItemTemplate7 temp = ItemTemplate7.get_it_by_id(id);
                                if (temp != null) {
                                    exists = true;
                                    p.item.add_item_bag47(7, temp.id, amount);
                                }
                                break;
                            }
                        }
                        if (exists) {
                            validIds.add(id);
                        } else {
                            invalidIds.add(id);
                        }
                    }
                    p.item.update_Inventory(-1, false);
                    String msg = "";
                    if (!validIds.isEmpty()) {
                        msg += "Lấy thành công IDs: " + validIds.toString() + ", số lượng: " + amount;
                    }
                    if (!invalidIds.isEmpty()) {
                        if (!msg.isEmpty()) {
                            msg += "\n";
                        }
                        msg += "Các ID không tồn tại: " + invalidIds.toString();
                    }
                    Service.send_box_ThongBao_OK(p, msg);
                } catch (Exception e) {
                    Service.send_box_ThongBao_OK(p, "Cú pháp: i<type> <id1> <id2> ... <số lượng>");
                }
            } else {
                Service.send_box_ThongBao_OK(p, "Cú pháp sai. Ví dụ: i4 427 512 500");
            }
        } else if (p.getAdmin() == 1 && s.startsWith("map")) {
            String[] parts = s.split(" ");
            if (parts.length > 1) {
                try {
                    int id = Integer.parseInt(parts[1]);
                    Map[] map_go = Map.get_map_by_id(id);

                    Vgo vgo = new Vgo();
                    vgo.map_go = map_go;
                    vgo.xnew = 350;
                    vgo.ynew = 250;
                    p.goto_map(vgo);
                } catch (NumberFormatException e) {
                    Service.send_box_ThongBao_OK(p, "Map không hợp lệ. Vui lòng nhập một số.");
                }
            } else {
                Service.send_box_ThongBao_OK(p, "Vui lòng nhập ID Map sau 'map'.");
            }
        } else if (s.startsWith("velang")) {
            Map[] map_go = Map.get_map_by_id(1);
            Vgo vgo = new Vgo();
            vgo.map_go = map_go;
            vgo.xnew = 350;
            vgo.ynew = 250;
            p.goto_map(vgo);
        } else if (s.equalsIgnoreCase("skin")) {
            if (p.skinId == -1) {
                p.skinId = 1503;
                Service.send_box_ThongBao_OK(p, "Đã bật Skin (Tàng hình nhân vật)");
            } else {
                p.skinId = -1;
                Service.send_box_ThongBao_OK(p, "Đã tắt Skin");
            }
            Service.charWearing(p, p, false);
            p.update_info_to_all();
        } else if (s.toLowerCase().trim().startsWith("a")) {
            try {
                String cmdd = s.toLowerCase().trim();
                int index = -1;
                int sizePrefix = 0;
                if (cmdd.startsWith("asm")) {
                    index = 0;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("apt")) {
                    index = 1;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("atl")) {
                    index = 2;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("att")) {
                    index = 3;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("ann")) {
                    index = 4;
                    sizePrefix = 3;
                }

                if (index != -1) {
                    String numStr = cmdd.substring(sizePrefix).trim();
                    int value;
                    try {
                        if (numStr.isEmpty()) {
                            value = p.pointAttribute;
                        } else {
                            long valLong = Long.parseLong(numStr);
                            value = (int) Math.min(valLong, Integer.MAX_VALUE);
                        }
                    } catch (NumberFormatException e) {
                        value = p.pointAttribute;
                    }
                    if (value > 0) {
                        int added = p.plus_point(index, value);
                        if (added > 0) {
                            String namePoint = "";
                            switch (index) {
                                case 0:
                                    namePoint = "Sức mạnh";
                                    break;
                                case 1:
                                    namePoint = "Phòng thủ";
                                    break;
                                case 2:
                                    namePoint = "Thể lực";
                                    break;
                                case 3:
                                    namePoint = "Tinh thần";
                                    break;
                                case 4:
                                    namePoint = "Nhanh nhẹn";
                                    break;
                            }
                            Service.send_box_ThongBao_OK(p, "Đã cộng " + Util.number_format(added) + " điểm vào "
                                    + namePoint + ".\nĐiểm tiềm năng còn lại: " + Util.number_format(p.pointAttribute));
                        } else {
                            Service.send_box_ThongBao_OK(p, "Chỉ số này đã đạt mức tối đa!");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Bạn không có điểm tiềm năng để cộng.");
                    }
                }
            } catch (Exception e) {
                Service.send_box_ThongBao_OK(p, "Cú pháp: a<tên_tắt><số_điểm>. VD: asm 10");
            }
        } else if (s.toLowerCase().trim().startsWith("c")
                && (s.toLowerCase().trim().length() >= 3 && !"congtien".equalsIgnoreCase(s.toLowerCase().trim()))) {
            try {
                String cmdd = s.toLowerCase().trim();
                int index = -1;
                int sizePrefix = 0;
                if (cmdd.startsWith("csm")) {
                    index = 0;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("cpt")) {
                    index = 1;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("ctl")) {
                    index = 2;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("ctt")) {
                    index = 3;
                    sizePrefix = 3;
                } else if (cmdd.startsWith("cnn")) {
                    index = 4;
                    sizePrefix = 3;
                }

                if (index != -1) {
                    if (p.clan != null) {
                        if (p.clan.members.get(0).name.equals(p.name)) {
                            String numStr = cmd.substring(sizePrefix).trim();
                            int value;
                            try {
                                if (numStr.isEmpty()) {
                                    value = p.clan.pointAttri;
                                } else {
                                    long valLong = Long.parseLong(numStr);
                                    value = (int) Math.min(valLong, Integer.MAX_VALUE);
                                }
                            } catch (NumberFormatException e) {
                                value = p.clan.pointAttri;
                            }
                            if (value > 0) {
                                int added = p.clan.plus_point(index, value);
                                if (added > 0) {
                                    String namePoint = "";
                                    switch (index) {
                                        case 0:
                                            namePoint = "Sức mạnh Băng";
                                            break;
                                        case 1:
                                            namePoint = "Phòng thủ Băng";
                                            break;
                                        case 2:
                                            namePoint = "Thể lực Băng";
                                            break;
                                        case 3:
                                            namePoint = "Tinh thần Băng";
                                            break;
                                        case 4:
                                            namePoint = "Nhanh nhẹn Băng";
                                            break;
                                    }
                                    Service.send_box_ThongBao_OK(p,
                                            "Đã cộng " + Util.number_format(added) + " điểm vào "
                                                    + namePoint + ".\nĐiểm tiềm năng băng còn lại: "
                                                    + Util.number_format(p.clan.pointAttri));
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Chỉ số băng này đã đạt mức tối đa!");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p, "Băng không có điểm tiềm năng để cộng.");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Chỉ thuyền trưởng mới có thể cộng tiềm năng băng!");
                        }
                    } else {
                        Service.send_box_ThongBao_OK(p, "Bạn không ở trong băng nào!");
                    }
                }
            } catch (Exception e) {
                Service.send_box_ThongBao_OK(p, "Cú pháp: c<tên_tắt><số_điểm>. VD: csm 10");
            }
            // } else if (s.toLowerCase().trim().equals("toi bi gay")) {
            // if (p.getTongNap() < 10000) {
            // try (java.sql.Connection conn = database.SQL.gI().getCon();
            // java.sql.PreparedStatement ps = conn.prepareStatement(
            // "UPDATE `accounts` SET `tongnap` = `tongnap` + 10000 WHERE BINARY `user` =
            // ?")) {
            // ps.setString(1, p.conn.user);
            // int rs = ps.executeUpdate();
            // if (rs > 0) {
            // Service.send_box_ThongBao_OK(p,
            // "Bạn đã dũng cảm thừa nhận! Chúc mừng bạn đã được mở Thành Viên.");
            // }
            // } catch (java.sql.SQLException e) {
            // core.GameLogger.printStackTrace(e);
            // Service.send_box_ThongBao_OK(p, "Lỗi kết nối cơ sở dữ liệu khi mở thành
            // viên.");
            // }
            // } else {
            // Service.send_box_ThongBao_OK(p, "Thừa nhận là tốt, nhưng tôi biết bạn là Gay
            // rồi ahihi!");
            // }
        } else if (s.toLowerCase().trim().equals("outclan")) {
            if (p.clan != null) {
                if (p.clan.members.size() > 0 && p.clan.members.get(0).name.equals(p.name)) {
                    Service.send_box_ThongBao_OK(p, "Thuyền trưởng không thể rời băng!");
                } else {
                    Service.send_box_yesno(p, 999, "Rời băng",
                            "Bạn có chắc muốn rời khỏi băng " + p.clan.name + "?",
                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 7, -1 });
                }
            } else {
                Service.send_box_ThongBao_OK(p, "Bạn không ở trong băng nào!");
            }
        } else {
            this.send_chat_popup(0, p.index_map, s);
        }
    }

    public void send_chat_popup(int type, int id_p, String s) throws IOException {
        Message m = new Message(17);
        switch (type) {
            case 0 -> {
                // Kéo người chơi ra TRƯỚC khi ghi dữ liệu vào gói tin
                Player p0 = this.get_player_by_id_inmap(id_p);

                // [LỚP PHÒNG NGỰ] Đảm bảo người đó thực sự còn trong Map
                if (p0 != null) {
                    m.writer().writeShort(id_p);
                    m.writer().writeByte(0);
                    m.writer().writeUTF(s);

                    // [BỘ LỌC ĐỈNH CAO]
                    // Tâm điểm: p0.x, p0.y
                    // ignore_index = -1 (Không chặn khán giả)
                    // VIP = p0.index_map (Chính chủ BẮT BUỘC phải thấy bong bóng chat của mình)
                    this.send_msg_all_p_distance(m, p0.x, p0.y, false, 6, -1, p0.index_map);
                }
            }
        }
        m.cleanup();
    }

    public static void sendChatNpc(short npcId, String message, int mapid) throws IOException {
        Message m3 = new Message(17);
        m3.writer().writeShort(npcId);
        m3.writer().writeByte(2);
        m3.writer().writeUTF(message);
        Map[] mapArrSelect = Map.get_map_by_id(mapid);
        for (int i = 0; i < mapArrSelect.length; i++) {
            if (mapArrSelect[i] != null) {
                mapArrSelect[i].send_msg_all_p(m3, null, true);
            }
        }
        m3.cleanup();
    }

    public void send_in4_obj_inmap(Player p) throws IOException {
        // send npc
        if (this.template.npcs.size() > 0) {
            Message mnpc = new Message(16);
            mnpc.writer().writeByte(this.template.npcs.size());
            for (int i = 0; i < this.template.npcs.size(); i++) {
                Npc npc = this.template.npcs.get(i);
                mnpc.writer().writeShort(npc.iditem);
                mnpc.writer().writeUTF(npc.name);
                mnpc.writer().writeUTF(npc.namegt);
                mnpc.writer().writeUTF(npc.chat);
                mnpc.writer().writeShort(npc.x);
                mnpc.writer().writeShort(npc.y);
                mnpc.writer().writeByte(npc.isPerson);
                mnpc.writer().writeByte(npc.typeIcon);
                mnpc.writer().writeByte(npc.wBlock);
                mnpc.writer().writeByte(npc.hBlock);
                mnpc.writer().writeByte(npc.b3);
                if (npc.b3 == 0) {
                    mnpc.writer().writeByte(npc.dataFrame[0]);
                    mnpc.writer().writeByte(npc.dataFrame[1]);
                } else {
                    mnpc.writer().writeShort(npc.head);
                    mnpc.writer().writeShort(npc.hair);
                    mnpc.writer().writeByte(npc.wearing.length);
                    for (int j = 0; j < npc.wearing.length; j++) {
                        if (npc.wearing[j] == -1) {
                            mnpc.writer().writeByte(-1);
                        } else {
                            mnpc.writer().writeByte(1);
                            mnpc.writer().writeShort(npc.wearing[j]);
                        }
                    }
                }
            }
            p.addmsg(mnpc);
            mnpc.cleanup();
        }
        if (this.template.id == 81 && this.map_little_garden != null) {
            for (int i = 0; i < this.map_little_garden.mobs.size(); i++) {
                Mob mob = this.map_little_garden.mobs.get(i);
                if (mob != null && mob.map.equals(this)) {
                    Message m_local = new Message(1);
                    m_local.writer().writeByte(1);
                    m_local.writer().writeShort(mob.index);
                    m_local.writer().writeShort(mob.x);
                    m_local.writer().writeShort(mob.y);
                    p.addmsg(m_local);
                    m_local.cleanup();
                }
            }
        }
        // send mob (Standardized to iterate over live instances in mobs_cache)
        for (int i = 0; i < this.mobs_cache.size(); i++) {
            Mob mob = this.mobs_cache.get(i);
            if (mob != null && !mob.isdie) {
                Service.send_mob_info(p, mob);
            }
        }
        //
        if (!Map.is_map_dont_show_other_info(this.template.id)) {
            // send player
            for (int i = 0; i < players.size(); i++) {
                Player p0 = players.get(i);
                // skip self and skip entities without real connection if we handle them via
                // master
                // OR just process them as standard entities but skip master's redundant
                // sub-call
                if (p.index_map != p0.index_map) {
                    Message m_local = new Message(1);
                    m_local.writer().writeByte(0);
                    m_local.writer().writeShort(p0.index_map);
                    m_local.writer().writeShort(p0.x);
                    m_local.writer().writeShort(p0.y);
                    p.addmsg(m_local);
                    m_local.cleanup();
                    //
                    p.id_meet_in_map.add("" + p0.index_map);
                    // Restore ship_pet visibility as it is not an independent entity in players
                    // list
                    if (p0.ship_pet != null && p0.ship_pet.map != null
                            && p0.ship_pet.map.equals(p.map)) {
                        m_local = new Message(1);
                        m_local.writer().writeByte(0);
                        m_local.writer().writeShort(p0.ship_pet.index_map);
                        m_local.writer().writeShort(p0.ship_pet.x);
                        m_local.writer().writeShort(p0.ship_pet.y);
                        p.addmsg(m_local);
                        m_local.cleanup();
                    }
                }
            }

            Message m_to_other = new Message(1);
            m_to_other.writer().writeByte(0);
            m_to_other.writer().writeShort(p.index_map);
            m_to_other.writer().writeShort(p.x);
            m_to_other.writer().writeShort(p.y);
            // LUÔN LUÔN tối ưu Viewport cho gói tin In4 (Hồ sơ nhân vật)
            // - p.x, p.y: Tâm điểm là người sở hữu bộ trang bị.
            // - false: Không gửi toàn map để tránh lag spike.
            // - 6: Chỉ cho 6 người đứng gần thấy sự thay đổi trang bị/ngoại trang.
            // - p.index_map: Sổ đen (Ignore). Chính chủ p thường đã có gói tin in4 riêng
            // cho máy mình rồi,
            // không cần server gửi ngược lại gói "in4 dành cho người khác" này.
            this.send_msg_all_p_distance(m_to_other, p.x, p.y, false, 6, p.index_map);
            m_to_other.cleanup();
        }
        if (p.party != null) {
            p.party.send_info();
        }
        if (p.conn != null) {
            p.change_new_date();
        }
        p.update_info_to_all();
    }

    public void send_char_in4_inmap(Player p, short id) throws IOException {
        Player p0 = get_player_by_id_inmap(id);
        if (p0 != null) {
            if (p0.map == null || p.map == null || !p0.map.equals(p.map)) {
                return;
            }
            boolean new_enter = false;
            if (!p.id_meet_in_map.contains("" + p0.index_map)) {
                p.id_meet_in_map.add("" + p0.index_map);
                new_enter = true;
            }
            int dir_ = 1;
            //
            // for (int i12 = 0; i12 < p0.fashion.size(); i12++) {
            // if ((p0.fashion.get(i12).id == 126) && p0.fashion.get(i12).is_use) {
            // Message m3 = new Message(-47);
            // m3.writer().writeByte(8);
            // m3.writer().writeByte(4);
            // p.addmsg(m3);
            // m3.cleanup();
            // break;
            // }
            // }
            //
            Message m = new Message(-5);
            m.writer().writeShort(p0.index_map);
            m.writer().writeByte(0);
            m.writer().writeByte((p0 instanceof client.Disciple) ? 1 : 0); // typePlayer: 0=Player, 1=NPC/NoInteract
            m.writer().writeByte(p0.typePirate); // typePirate
            m.writer().writeByte(p0.type_pk); // typePk
            m.writer().writeByte(new_enter ? dir_ : 0); // eff dir new
            m.writer().writeByte(-1); // index team
            m.writer().writeUTF(p0.getNameShow());
            m.writer().writeShort(p0.level);
            m.writer().writeInt(f.setInteger(p0.body.get_hp_max(true)));
            m.writer().writeInt(f.setInteger(p0.hp));
            m.writer().writeShort(p0.thongthao);
            m.writer().writeInt(BXH.get_rank_wanted(p0.name));
            m.writer().writeByte(p0.body.get_level_perfect());
            m.writer().writeByte(p0.clazz);
            m.writer().writeByte(-1); // dir new
            int[] colorMap = { 0, 10, 20, 30, 70 };
            int colorValue = 0;
            if (p0.item.it_body.length > 6 && p0.item.it_body[6] != null && p0.item.it_body[6].template != null) {
                int color = p0.item.it_body[6].template.color;
                if (color >= 0 && color < colorMap.length) {
                    colorValue = colorMap[color];
                }
            }
            m.writer().writeByte(colorValue);
            //
            m.writer().writeShort(p0.getEffFashion()); // body bay
            m.writer().writeShort(-1); // leg bay
            m.writer().writeShort(-1); // weapon bay
            m.writer().writeShort(p0.getEffHair()); // hair bay
            //
            p.addmsg(m);
            m.cleanup();
            //
            Service.pet(p0, p, false);
            Service.update_PK(p0, p, false);
            Service.update_PK(p, p0, false);
            Service.Weapon_fashion(p0, p, false);
            Service.getThanhTich(p0, p);
            Service.charWearing(p0, p, false);
            //
            this.update_boat(p0, p, false);
            this.update_boat(p, p0, false);
            //
            EffTemplate eff = p0.get_eff(7);
            if (eff != null) {
                Message m2 = new Message(-71);
                m2.writer().writeByte(1);
                m2.writer().writeShort(p0.index_map);
                m2.writer().writeByte(0);
                m2.writer().writeInt(f.setInteger((eff.time - System.currentTimeMillis()) / 1000));
                p.addmsg(m2);
                m2.cleanup();
            }
            // clan
            if (p0.clan != null) {
                Clan.send_me_to_other(p0, p, false);
                if (p0.worldWarInfo != null) {
                    Message m123 = new Message(53);
                    m123.writer().writeShort(p0.index_map);
                    m123.writer().writeByte(p0.worldWarInfo.skPoint);
                    m123.writer().writeShort(p0.worldWarInfo.kill);
                    m123.writer().writeShort(p0.worldWarInfo.dead);
                    p.addmsg(m123);
                }
            }
        } else {
            Ship_pet spet = Ship_pet.get_pet(id);
            if (spet == null) {
                spet = p.ship_pet;
            }
            if (spet != null && spet.map != null && spet.map.equals(p.map)) {
                Message m = new Message(-5);
                m.writer().writeShort(spet.index_map);
                m.writer().writeByte(0);
                m.writer().writeByte(2); // typePlayer
                m.writer().writeByte(spet.main_ship.typePirate); // typePirate
                m.writer().writeByte(-1); // typePk
                m.writer().writeByte(1);
                m.writer().writeByte(-1); // index team
                m.writer().writeUTF(spet.name);
                m.writer().writeShort(1); // level
                m.writer().writeInt(spet.hp_max);
                m.writer().writeInt(spet.hp);
                m.writer().writeShort(0);
                m.writer().writeInt(-1);
                m.writer().writeByte(0);
                //
                m.writer().writeShort(996);
                m.writer().writeByte(1);
                m.writer().writeShort(spet.main_ship.index_map);
                m.writer().writeByte(spet.main_ship.typePirate);
                //
                m.writer().writeShort(-1); // body bay
                m.writer().writeShort(-1); // leg bay
                m.writer().writeShort(-1); // weapon bay

                //
                p.addmsg(m);
                m.cleanup();
                //
            }
        }
    }

    public Player get_player_by_id_inmap(int id) {
        for (Player p01 : players) { // Bỏ synchronized (players)
            if (p01 != null && p01.index_map == id) {
                return p01;
            }
            if (p01 != null && p01.myDisciple != null && p01.myDisciple.index_map == id) {
                return p01.myDisciple;
            }
        }
        return null;
    }

    public static boolean map_cant_save_site(int id) {
        boolean check = false;
        // for (int i = 0; i < DataTemplate.mSea.length; i++) {
        // if (DataTemplate.mSea[i][1] == id) {
        // check = true;
        // break;
        // }
        // }
        return check || id == 1000 || id == 81 || id == 120 || id == 122 || id == 123 || id == 119 || id == 58
                || id == 301 || id == 303 || id == 900 || id == 260
                || id == 500 || id == 62 || id == 250 || id == 121;
    }

    public static boolean is_map_sea(int id) {
        return id == 7;
    }

    public void change_flag(Player p, int type) throws IOException {
        if (!(this.map_pvp != null || this.template.id == 1000)) {
            if (p.type_pk == 1 && type == -1) {
                return;
            }
        } else if (type == 1) {
            type = -1;
        }
        if (p.clan != null) {
            if (p.worldWarInfo != null) {
                type = p.worldWarInfo.flagType;
                Service.showWorldWarPoint(p);
                WorldWar.updateWorldWarPoint(p, p.worldWarInfo.skPoint, p.worldWarInfo.kill,
                        p.worldWarInfo.dead);
            }
            if (p.clan != null && p.map.map_little_garden != null) {
                if (p.clan.equals(p.map.map_little_garden.clan1)) {
                    type = 4;
                } else {
                    type = 5;
                }
            }
            if (p.map.pvpBangMapFight != null && p.map.pvpBangMapFight.status_pvp == 3) {
                if (p.clan.equals(p.map.pvpBangMapFight.clan1)) {
                    type = 4;
                } else {
                    type = 5;
                }
            }
            if (p.map.template.id == 303) {
                type = 3;
            }
            if (ChiemDao.getClanTop(this.template.id) != null
                    && ChiemDao.getClanTop(this.template.id).equals(p.clan)) {
                type = 4;
            }
        }
        if (this.template.id == 304) {
            type = 3;
            if (ChiemDao.cachedHolderId304 == p.id) {
                type = 4;
            }
        }
        p.type_pk = (byte) type;
        List<Player> snapshot;
        synchronized (players) {
            snapshot = new ArrayList<>(this.players);
        }
        for (Player p0 : snapshot) {
            Service.update_PK(p, p0, false);
        }
    }

    public synchronized void pick_item(Player p, Message m2) throws IOException {
        if (p.isdie || p.rms.length > 2 && p.rms[2].length > 0 && p.rms[2][0] == 0) {
            // return;
        }
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        byte code_response = -1;
        //
        switch (cat) {
            case 3: {
                for (int i = 0; i < list_it_map.length; i++) {
                    if (list_it_map[i] != null && list_it_map[i].category == cat
                            && list_it_map[i].index == id) {
                        if (list_it_map[i].id_master == -1 || (list_it_map[i].id_master != -1
                                && list_it_map[i].id_master == p.index_map)) {
                            ItemTemplate3 temp3 = ItemTemplate3.get_it_by_id(list_it_map[i].id);
                            if (temp3 != null && p.rms.length > 2 && p.rms[2].length > 3) {
                                // core.GameLogger.info(p.rms[2][1] + " " + temp3.color);
                                if (p.rms[2][1] == 1 && temp3.color < 2) {
                                    // return;
                                }
                                if (p.rms[2][1] == 2 && temp3.color < 3) {
                                    // return;
                                }
                            }
                            //
                            if (temp3 != null) {
                                Item_wear it_add = new Item_wear();
                                it_add.setup_template_by_id(temp3);
                                if (it_add.template != null) {
                                    if (!p.item.add_item_bag3(it_add)) {
                                        // Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                                        return;
                                    }
                                    p.item.update_Inventory(-1, false);
                                }
                            }
                            list_it_map[i] = null;
                            code_response = 0;
                        } else {
                            code_response = 1;
                        }
                        break;
                    }
                }
                break;
            }
            case 5: { // quest
                for (int i = 0; i < list_it_map.length; i++) {
                    if (list_it_map[i] != null && list_it_map[i].category == cat
                            && list_it_map[i].index == id) {
                        if (list_it_map[i].id < DataTemplate.NamePotionquest.length) {
                            if (list_it_map[i].id_master == -1 || (list_it_map[i].id_master != -1
                                    && list_it_map[i].id_master == p.index_map)) {
                                if (!p.item.add_item_bag47(5, list_it_map[i].id,
                                        list_it_map[i].quant)) {
                                    // Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                                    return;
                                }
                                p.item.update_Inventory(-1, false);
                                p.update_num_item_quest(2, list_it_map[i].id, list_it_map[i].quant);
                                list_it_map[i] = null;
                                code_response = 0;
                            } else {
                                code_response = 1;
                            }
                        }
                        break;
                    }
                }
                break;
            }
            case 4: {
                for (int i = 0; i < list_it_map.length; i++) {
                    if (list_it_map[i] != null && list_it_map[i].category == cat
                            && list_it_map[i].index == id) {
                        if (this.template.id == 81 && this.map_little_garden != null) {
                            if (list_it_map[i].id_master == -1 || (list_it_map[i].id_master != -1
                                    && list_it_map[i].id_master == p.index_map)) {
                                //
                                switch (list_it_map[i].id) {
                                    case 94: {
                                        if (p.type_pk == 4) {
                                            for (int j = 0; j < this.players.size(); j++) {
                                                Player p0 = this.players.get(j);
                                                if (p0 != null && p0.conn != null && p0.type_pk == 5
                                                        && !p0.isdie) {
                                                    die_player(p0, p);
                                                    p0.time_hs_little_garden = System.currentTimeMillis() + 10_000L;
                                                    Service.send_time_cool_down(p0,
                                                            p0.time_hs_little_garden, "Hồi sinh",
                                                            3);
                                                }
                                            }
                                        } else {
                                            for (int j = 0; j < this.players.size(); j++) {
                                                Player p0 = this.players.get(j);
                                                if (p0 != null && p0.conn != null && p0.type_pk == 4
                                                        && !p0.isdie) {
                                                    die_player(p0, p);
                                                    p0.time_hs_little_garden = System.currentTimeMillis() + 10_000L;
                                                    Service.send_time_cool_down(p0,
                                                            p0.time_hs_little_garden, "Hồi sinh",
                                                            3);
                                                }
                                            }
                                        }
                                        break;
                                    }
                                    case 95: {
                                        for (int j = 0; j < this.players.size(); j++) {
                                            Player p0 = this.players.get(j);
                                            if (p0 != null && p0.conn != null
                                                    && p0.type_pk == p.type_pk && p0.isdie) {
                                                p0.time_hs_little_garden = 0;
                                            }
                                        }
                                        break;
                                    }
                                    case 96: {
                                        LittleGarden.update_mp(this, p.type_pk, 0);
                                        break;
                                    }
                                    case 97: {
                                        LittleGarden.update_mp(this, p.type_pk, 2);
                                        break;
                                    }
                                    case 98: {
                                        LittleGarden.update_hp(this, p.type_pk, 2);
                                        break;
                                    }
                                    case 99: {
                                        LittleGarden.update_mp(this, p.type_pk, 1);
                                        break;
                                    }
                                    case 100: {
                                        LittleGarden.update_hp(this, p.type_pk, 1);
                                        break;
                                    }
                                }
                                //
                                list_it_map[i] = null;
                                code_response = 2;
                            } else {
                                code_response = 1;
                            }
                        } else {
                            for (int i2 = 0; i2 < LeaveItemMap.ITEM_POTION.length; i2++) {
                                if (LeaveItemMap.ITEM_POTION[i2] == list_it_map[i].id
                                        || (list_it_map[i].id >= 7 && list_it_map[i].id <= 17)) {
                                    if (list_it_map[i].id_master == -1
                                            || (list_it_map[i].id_master != -1
                                                    && list_it_map[i].id_master == p.index_map)) {
                                        if (list_it_map[i].id == 0) { // beri
                                            if (p.rms.length > 2 && p.rms[2].length > 3
                                                    && p.rms[2][3] == 1) {
                                                return;
                                            }
                                            p.update_vang(list_it_map[i].quant);
                                            p.update_money();
                                        } else if (list_it_map[i].id == 1) { // ruby
                                            if (p.rms.length > 2 && p.rms[2].length > 3
                                                    && p.rms[2][3] == 1) {
                                                return;
                                            }
                                            // p.update_ngoc(list_it_map[i].quant);
                                            // p.update_money();
                                        } else {
                                            if (p.rms.length > 2 && p.rms[2].length > 3) {
                                                ItemTemplate4 itemTemplate4 = ItemTemplate4
                                                        .get_it_by_id(list_it_map[i].id);
                                                // itemTemplate4.type);
                                                if (p.rms[2][2] == 1 && itemTemplate4.type != 1) {
                                                    return;
                                                }
                                                if (p.rms[2][2] == 2 && itemTemplate4.type != 2) {
                                                    return;
                                                }
                                            }
                                            if (!p.item.add_item_bag47(4, list_it_map[i].id,
                                                    list_it_map[i].quant)) {
                                                // Service.send_box_ThongBao_OK(p, "Hành trang
                                                // đầy");
                                                return;
                                            }
                                            p.item.update_Inventory(-1, false);
                                        }
                                        list_it_map[i] = null;
                                        code_response = 0;
                                    } else {
                                        code_response = 1;
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
                break;
            }
            case 7: {
                for (int i = 0; i < list_it_map.length; i++) {
                    if (list_it_map[i] != null && list_it_map[i].category == cat
                            && list_it_map[i].index == id) {
                        if (list_it_map[i].id_master == -1 || (list_it_map[i].id_master != -1
                                && list_it_map[i].id_master == p.index_map)) {
                            if (!p.item.add_item_bag47(7, list_it_map[i].id,
                                    list_it_map[i].quant)) {
                                // Service.send_box_ThongBao_OK(p, "Hành trang đầy");
                                return;
                            }
                            p.item.update_Inventory(-1, false);
                            list_it_map[i] = null;
                            code_response = 0;
                        } else {
                            code_response = 1;
                        }
                        break;
                    }
                }
                break;
            }
        }
        switch (code_response) {
            case -1: {
                remove_obj(id, cat);
                break;
            }
            case 0: { // ok
                Message m = new Message(12);
                m.writer().writeShort(id);
                m.writer().writeByte(cat);
                m.writer().writeShort(p.index_map);
                p.addmsg(m);
                m.cleanup();
                remove_obj(id, cat);
                break;
            }
            case 1: {
                if (p.time_pick_item_other < System.currentTimeMillis()) {
                    p.time_pick_item_other = System.currentTimeMillis() + 7_000L;
                    Message mnext = new Message(-31);
                    mnext.writer().writeByte(0);
                    mnext.writer().writeUTF("Vật phẩm của người khác");
                    mnext.writer().writeByte(0);
                    mnext.writer().writeShort(-1);
                    p.addmsg(mnext);
                    mnext.cleanup();
                }
                break;
            }
            case 2: { // little garden
                if (this.map_little_garden != null && !this.map_little_garden.is_finish
                        && (p.type_pk == 4 || p.type_pk == 5)) {
                    //
                    Message m = new Message(33);
                    m.writer().writeShort(id);
                    m.writer().writeByte(cat);
                    m.writer().writeByte(p.type_pk == 4 ? 0 : 1);
                    p.addmsg(m);
                    m.cleanup();
                    // remove_obj(id, cat);
                }
                break;
            }
        }
    }

    public void send_data(Player p) throws IOException {
        Message m = new Message(0);
        m.writer().writeShort(this.template.id);
        m.writer().writeByte(this.zone_id);
        m.writer().writeByte(this.template.type_view_p);
        m.writer().writeShort(p.x);
        m.writer().writeShort(p.y);
        m.writer().writeInt(f.setInteger(p.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(p.hp));
        m.writer().writeInt(f.setInteger(p.body.get_mp_max(true)));
        m.writer().writeInt(f.setInteger(p.mp));
        m.writer().writeByte(this.template.b);
        m.writer().writeByte(this.template.specMap);
        if (this.template.b == 1) {
            m.writer().writeInt(this.template.data[0].length);
            m.writer().write(this.template.data[0]);
            m.writer().writeInt(this.template.data[1].length);
            m.writer().write(this.template.data[1]);

            if (this.isMazeMap) {
                m.writer().writeByte(this.mazePortals.size());
                for (int i = 0; i < this.mazePortals.size(); i++) {
                    MazePortal portal = this.mazePortals.get(i);
                    String targetName = core.OceanMazeManager.getNodeName(portal.targetNode);
                    m.writer().writeUTF(targetName);
                    m.writer().writeShort(portal.x);
                    m.writer().writeShort(portal.y);
                }
            } else {
                m.writer().writeByte(this.template.vgos.size());
                for (int i = 0; i < this.template.vgos.size(); i++) {
                    m.writer().writeUTF(this.template.vgos.get(i).map_go[0].template.name);
                    m.writer().writeShort(this.template.vgos.get(i).xold);
                    m.writer().writeShort(this.template.vgos.get(i).yold);
                }
            }
        }
        m.writer().writeByte(this.template.IDBack);
        m.writer().writeShort(this.template.HBack);
        m.writer().writeByte(this.template.id_eff_map);
        m.writer().writeByte(this.template.level);
        m.writer().writeByte(this.template.typeChangeMap);
        if (this.template.specMap == 3) {
            m.writer().writeByte(this.template.mPosMapTrain.length);
            for (int i = 0; i < this.template.mPosMapTrain.length; i++) {
                for (int j = 0; j < this.template.mPosMapTrain[i].length; j++) {
                    m.writer().writeByte(this.template.mPosMapTrain[i][j]);
                }
            }
            m.writer().writeUTF(this.template.strTimeChange);
        }
        m.writer().writeUTF(customName != null ? customName : this.template.name);
        p.addmsg(m);
        m.cleanup();
    }

    public void goto_map(Player p) throws IOException {
        if (p.conn != null) {
            this.enter_map(p);
            this.send_data(p);
            //
            boolean send_move = true;
            for (int i = 0; i < DataTemplate.mSea.length; i++) {
                if (DataTemplate.mSea[i][0] == this.template.id) {
                    send_move = false;
                    break;
                }
            }
            if (send_move) {
                Message mmove = new Message(1);
                mmove.writer().writeByte(0);
                mmove.writer().writeShort(p.index_map);
                mmove.writer().writeShort(p.x);
                mmove.writer().writeShort(p.y);
                p.list_msg_cache.add(mmove);
                mmove.cleanup();
            }
            // conn.p.map.enter_zone(conn.p);
            this.enter_zone(p);
            if (Map.is_map_save_revival(this.template.id)) {
                p.id_map_save = this.template.id;
                p.time_can_hs = 7;
            }

            // Đồng bộ quái phó bản VHLT khi người chơi vào map
            if (this.map_VoHanLienTang != null && !this.map_VoHanLienTang.mobs.isEmpty()) {
                for (Mob mob : this.map_VoHanLienTang.mobs) {
                    if (mob != null && !mob.isdie) {
                        Message m = new Message(4);
                        m.writer().writeShort(mob.index);
                        m.writer().writeShort(mob.mob_template.mob_id);
                        m.writer().writeShort(mob.x);
                        m.writer().writeShort(mob.y);
                        m.writer().writeShort(mob.level);
                        m.writer().writeInt(f.setInteger(mob.hp));
                        m.writer().writeInt(f.setInteger(mob.hp_max));
                        m.writer().writeShort(mob.mob_template.skill[0]);
                        m.writer().writeShort(Skill_info.GLOBAL_MOB_RESPAWN_TIME);
                        m.writer().writeByte(mob.mob_template.typemonster);
                        m.writer().writeByte(0);
                        p.addmsg(m);
                        m.cleanup();
                    }
                }
            }
        }
    }

    private static boolean is_map_save_revival(int id) {
        for (int i = 0; i < MenuController.ID_MAP_LANG.length; i++) {
            if (id == MenuController.ID_MAP_LANG[i] && id != 113 && id != 79 && id != 191) {
                return true;
            }
        }
        return false;
    }

    public void send_boat(Player p, boolean is_have_my_boat) throws IOException {
        if (p.conn == null || !p.conn.connected) {
            return;
        }
        for (int i = 0; i < DataTemplate.mSea.length; i++) {
            if (DataTemplate.mSea[i][0] == this.template.id) {
                Message m = new Message(-56);
                int size = is_have_my_boat ? this.template.list_boat.size()
                        : (this.template.list_boat.size() - 1);
                m.writer().writeByte(size);
                if (is_have_my_boat) {
                    m.writer().writeShort(p.index_map);
                    m.writer().writeShort(this.template.list_boat.get(0).x);
                    m.writer().writeShort(this.template.list_boat.get(0).y);
                    m.writer().writeByte(4);
                    m.writer().writeShort(0);
                    m.writer().writeShort(1);
                    m.writer().writeShort(2);
                    m.writer().writeShort(3);
                }
                for (int j = 1; j < this.template.list_boat.size(); j++) {
                    m.writer().writeShort(-1);
                    m.writer().writeShort(this.template.list_boat.get(j).x);
                    m.writer().writeShort(this.template.list_boat.get(j).y);
                    m.writer().writeByte(0);
                }
                p.list_msg_cache.add(m);
                m.cleanup();
                break;
            }
        }
    }

    public void enter_zone(Player p) throws IOException {
        if (p.map == null || p.conn == null) {
            return;
        }
        p.ischangemap = false;
        p.xold = p.x;
        p.yold = p.y;
        Message m = new Message(21);
        m.writer().writeByte(this.zone_id);
        m.writer().writeByte(0);
        m.writer().writeShort(p.x);
        m.writer().writeShort(p.y);
        m.writer().writeInt(f.setInteger(p.body.get_hp_max(true)));
        m.writer().writeInt(f.setInteger(p.hp));
        m.writer().writeInt(f.setInteger(p.body.get_mp_max(true)));
        m.writer().writeInt(f.setInteger(p.mp));
        m.writer().writeByte(p.map.template.IDBack);
        m.writer().writeShort(p.map.template.HBack);
        p.addmsg(m);
        m.cleanup();
        //
        Service.update_PK(p, p, true);
        Service.pet(p, p, true);
        // Service.send_Quest(p,true);
        this.send_boat(p, true);
        this.update_boat(p, p, true);
        this.send_in4_obj_inmap(p);
    }

    public void update_boat(Player p0, Player p, boolean cache) throws IOException {
        if (p == null || p.conn == null || !p.conn.connected) {
            return;
        }
        for (int i = 0; i < DataTemplate.mSea.length; i++) {
            if (DataTemplate.mSea[i][1] == this.template.id) {
                break;
            }
        }
        Message m = new Message(-33);
        m.writer().writeByte(0);
        m.writer().writeShort(p.rms[0].length);
        if (p.rms[0].length > 0) {
            m.writer().write(p.rms[0]);
        }
        if (cache) {
            p.list_msg_cache.add(m);
        } else {
            p.addmsg(m);
        }
        m.cleanup();

    }

    public int get_index_item_map() {
        for (int i = 0; i < this.list_it_map.length; i++) {
            if (this.list_it_map[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public void send_weather(Player p) throws IOException {
        // Các loại thời tiết client hỗ trợ (/bg/*.png):
        // -1 = Tắt thời tiết
        // 0 = Lá/Gió (leaf.png)
        // 1 = Tuyết (snow.png) → đổi background AE()
        // 2 = Hoa anh đào (flower.png)
        // 3 = Mưa (rain.png)
        // 4 = Sóng biển (sea1/sea2.png)
        // 5 = Hoa mai (flowermai.png)
        // 6 = Hoa pháo (flowerphao.png) → đổi background AF()
        // 7 = Hoa đào (flowerdao.png)
        // 8 = Mưa nặng (rain.png, mật độ cao) → đổi background AF()
        // 9 = Super Boss → đổi background AG() + khóa isSuperBoss
        // weather_level: 0=rất thưa, 1=thưa, 2=bình thường, 3=dày, 4=rất dày
        if (p.conn != null && p.conn.connected && p.map.template.id_eff_map == -1) {
            Message m = new Message(-47);
            m.writer().writeByte(Map.weather);
            m.writer().writeByte(Map.weather_level);
            p.addmsg(m);
            m.cleanup();
        }
    }

    public Mob get_mobs(int id, int type) {
        switch (type) {
            case 0: {
                if (this.map_little_garden != null) {
                    for (int i = 0; i < this.map_little_garden.mobs.size(); i++) {
                        if (this.map_little_garden.mobs.get(i).index == id) {
                            return this.map_little_garden.mobs.get(i);
                        }
                    }
                }
                if (this.map_VoHanLienTang != null) {
                    for (int i = 0; i < this.map_VoHanLienTang.mobs.size(); i++) {
                        if (this.map_VoHanLienTang.mobs.get(i).index == id) {
                            return this.map_VoHanLienTang.mobs.get(i);
                        }
                    }
                }
                break;
            }
        }
        return null;
    }

    private void sendEffect(Player p, short idEffect) {
        Message m = null;
        try {
            m = new Message(74);
            m.writer().writeByte(1);
            m.writer().writeShort(p.index_map);
            m.writer().writeShort(idEffect);
            m.writer().writeInt(9999);
            m.writer().writeByte(0);
            m.writer().writeByte(0);
            this.send_msg_all_p_distance(m, p.x, p.y, false, 6, -1, p.index_map);
            m.cleanup();
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] Map.sendEffect error: " + e.getMessage());
        }
    }

    private void update_mapVoHanLienTang() throws IOException {
        if (this.map_VoHanLienTang != null) {
            // 1. Kiểm tra hết giờ hoặc người chơi tử trận
            synchronized (players) {
                for (int i = players.size() - 1; i >= 0; i--) {
                    Player p = players.get(i);
                    boolean isTimeout = System.currentTimeMillis() > this.map_VoHanLienTang.time;
                    if (p != null && (p.isdie || isTimeout)) {
                        String msg = isTimeout ? "Đã hết thời gian!" : "Bạn đã bị hạ gục!";
                        Service.send_box_ThongBao_OK(p, msg);

                        // Đưa về làng
                        p.map.leave_map(p, 2);
                        Map[] mapLàngArr = Map.get_map_by_id(1);
                        if (mapLàngArr != null && mapLàngArr.length > 0) {
                            p.map = mapLàngArr[0]; // Làng Windmill
                        } else {
                            core.GameLogger
                                    .error("Không tìm thấy Làng Windmill (Map ID 1) trong update_mapVoHanLienTang");
                            continue;
                        }
                        p.x = 422;
                        p.y = 398;
                        p.map.goto_map(p);
                        continue;
                    }
                }
            }

            // 2. Kiểm tra và xử lý chuyển tầng
            if (this.map_VoHanLienTang.time_next_floor != -1
                    && this.map_VoHanLienTang.time_next_floor < System.currentTimeMillis()) {
                this.map_VoHanLienTang.time = System.currentTimeMillis() + (60_000L * 2); // 2 Phút mỗi tầng
                this.map_VoHanLienTang.time_next_floor = -1;
                this.map_VoHanLienTang.floor++;
                this.map_VoHanLienTang.floor_cleared = false;
                this.map_VoHanLienTang.mobs.clear();

                Map[] mapTemplateArr = Map.get_map_by_id(301);
                if (mapTemplateArr == null || mapTemplateArr.length == 0) {
                    core.GameLogger.error("Không tìm thấy Map Template 301 cho update_mapVoHanLienTang");
                    return;
                }
                Map mapTemplate = mapTemplateArr[0]; // Template Đảo Sét
                Mob firstMob = Mob.ENTRYS.get(mapTemplate.list_mob[0]);
                int mLevel = (int) (firstMob.level * (1.0 + (this.map_VoHanLienTang.floor * 0.1)));
                long mHp = (long) (firstMob.hp_max * (1.0 + (this.map_VoHanLienTang.floor * 0.1)));

                int ne = this.map_VoHanLienTang.getOptionValue(12, this.map_VoHanLienTang.floor);
                int phan = this.map_VoHanLienTang.getOptionValue(14, this.map_VoHanLienTang.floor);
                int mien = this.map_VoHanLienTang.getOptionValue(53, this.map_VoHanLienTang.floor) / 10;
                int phaNe = this.map_VoHanLienTang.getOptionValue(51, this.map_VoHanLienTang.floor) / 10;
                int phaMien = this.map_VoHanLienTang.getOptionValue(63, this.map_VoHanLienTang.floor) / 10;
                int phaPhan = this.map_VoHanLienTang.getOptionValue(64, this.map_VoHanLienTang.floor);
                int phaDef = this.map_VoHanLienTang.getOptionValue(15, this.map_VoHanLienTang.floor);

                String stats = "\nQuái cấp: " + mLevel
                        + "\nHP: " + Util.number_format(mHp)
                        + "\nNé tránh: " + ne + "%"
                        + "\nPhản đòn: " + phan + "%"
                        + "\nMiễn thương: " + mien + "%"
                        + "\nGiảm né tránh: " + phaNe + "%"
                        + "\nGiảm miễn thương: " + phaMien + "%"
                        + "\nGiảm phản đòn: " + phaPhan + "%"
                        + "\nGiảm phòng thủ: " + phaDef + "%";

                synchronized (players) {
                    for (Player p : players) {
                        p.update_hp(p.body.get_hp_max(true) - p.hp);
                        p.update_mp(p.body.get_mp_max(true) - p.mp);
                        Service.use_potion(p, 0, p.hp);
                        Service.use_potion(p, 1, p.mp);

                        // Dịch chuyển về (100, 300)
                        p.x = 100;
                        p.y = 300;
                        p.xold = p.x;
                        p.yold = p.y;
                        Service.Main_char_Info(p); // Cập nhật tọa độ và HP/MP cho client

                        // Gửi đồng hồ đếm ngược 2 phút
                        Service.send_time_cool_down(p, this.map_VoHanLienTang.time,
                                "VHT Tầng " + this.map_VoHanLienTang.floor, 0);

                        Service.send_box_ThongBao_OK(p, "Bắt đầu tầng " + this.map_VoHanLienTang.floor + stats);

                        if (this.map_VoHanLienTang.floor > 1) {
                            p.ruby0 += 1000;
                            p.update_money();
                        }
                    }
                }

                // Sinh quái mới (Sử dụng ID Mob âm giống Vệ Thần)
                int index_mob = -2;
                for (int i = 0; i < mapTemplate.list_mob.length; i++) {
                    Mob temp = Mob.ENTRYS.get(mapTemplate.list_mob[i]);
                    Mob mob_add = new Mob();
                    mob_add.mob_template = temp.mob_template;
                    mob_add.index = index_mob--;
                    mob_add.x = temp.x;
                    mob_add.y = temp.y;
                    mob_add.level = (int) (temp.level * (1.0 + (this.map_VoHanLienTang.floor * 0.1)));
                    mob_add.hp_max = (long) (temp.hp_max * (1.0 + (this.map_VoHanLienTang.floor * 0.1)));
                    mob_add.hp = mob_add.hp_max;
                    mob_add.isdie = false;
                    mob_add.map = this;
                    this.map_VoHanLienTang.mobs.add(mob_add);

                    // Gửi thông tin quái mới xuất hiện cho toàn map
                    Message m = new Message(4);
                    m.writer().writeShort(mob_add.index);
                    m.writer().writeShort(mob_add.mob_template.mob_id);
                    m.writer().writeShort(mob_add.x);
                    m.writer().writeShort(mob_add.y);
                    m.writer().writeShort(mob_add.level);
                    m.writer().writeInt(f.setInteger(mob_add.hp));
                    m.writer().writeInt(f.setInteger(mob_add.hp_max));
                    m.writer().writeShort(mob_add.mob_template.skill[0]);
                    m.writer().writeShort(-1);
                    m.writer().writeByte(mob_add.mob_template.typemonster);
                    m.writer().writeByte(0);
                    send_msg_all_p(m, null, true);
                    m.cleanup();
                }
                return;
            }

            // Kiểm tra trạng thái quái để dọn map
            if (!this.map_VoHanLienTang.floor_cleared) {
                boolean allDead = true;
                if (this.map_VoHanLienTang.mobs.isEmpty()) {
                    allDead = false; // Đang chờ quái tầng 1 sinh ra
                } else {
                    for (Mob mob : this.map_VoHanLienTang.mobs) {
                        if (mob != null && !mob.isdie) {
                            allDead = false;
                            break;
                        }
                    }
                }

                if (allDead && !this.map_VoHanLienTang.mobs.isEmpty()) {
                    this.map_VoHanLienTang.floor_cleared = true;
                    this.map_VoHanLienTang.time_next_floor = System.currentTimeMillis() + 3000L; // Giảm xuống 3s để chờ
                                                                                                 // cho nhanh
                    synchronized (players) {
                        for (Player p : players) {
                            Service.send_box_ThongBao_OK(p, "Tầng " + this.map_VoHanLienTang.floor + " Hoàn tất!");
                            Service.send_time_cool_down(p, this.map_VoHanLienTang.time_next_floor, "Chuẩn bị", 0);
                            // Cập nhật BXH ngay khi qua tầng
                            activities.VoHanLienTangRanking.update(p, this.map_VoHanLienTang.floor);
                        }
                    }
                }
            }
        }
    }

    // Kiểm tra các map diện tích nhỏ để tắt Viewport (tiết kiệm CPU)
    public boolean is_small_map() {
        int id = this.template.id;
        return id == 1000 || id == 120 || id == 123 || id == 58; // Lôi đài, Đấu trường, vv.
    }

    // =========================================================================
    // BỘ HÀM OVERLOAD (GỌI LINH HOẠT - TỰ ĐỘNG ĐIỀN THAM SỐ)
    // =========================================================================

    // 1. SIÊU NGẮN (Dành cho Quái vật nhúc nhích, Rớt đồ dưới đất)
    // -> Tự động: max_p = 6, không chặn ai (-1), không có VIP
    public void send_msg_all_p_distance(Message m, int x, int y) throws IOException {
        this.send_msg_all_p_distance(m, x, y, false, 6, -1);
    }

    // 2. CHỈNH KHÁN GIẢ (Dành cho Sự kiện, Boss xuất hiện, Nổ AoE to)
    // -> Tự động: không chặn ai (-1), không có VIP. Bạn tự quyết định max_p.
    public void send_msg_all_p_distance(Message m, int x, int y, int max_p) throws IOException {
        this.send_msg_all_p_distance(m, x, y, false, max_p, -1);
    }

    // 3. HỆ NGƯỜI CHƠI (Dành cho Di chuyển, Đánh quái lẻ, Bật hiệu ứng)
    // -> Tự động: max_p = 6. Bắt buộc bạn phải truyền sổ đen (ignore) và VIP
    // (forced).
    public void send_msg_all_p_distance(Message m, int x, int y, int ignore_index, int... forced_indices)
            throws IOException {
        this.send_msg_all_p_distance(m, x, y, false, 6, ignore_index, forced_indices);
    }

    // 4. FULL OPTION MÀ KHÔNG CẦN CHỮ "FALSE" (Tối đa linh hoạt)
    public void send_msg_all_p_distance(Message m, int x, int y, int max_p, int ignore_index, int... forced_indices)
            throws IOException {
        this.send_msg_all_p_distance(m, x, y, false, max_p, ignore_index, forced_indices);
    }

    // =========================================================================
    // HÀM LÕI (MASTER FUNCTION) - NƠI XỬ LÝ LOGIC CHÍNH
    // =========================================================================
    public void send_msg_all_p_distance(Message m, int x, int y, boolean all, int max_p, int ignore_index,
            int... forced_indices) throws IOException {
        if (players.isEmpty())
            return;

        if (all || is_small_map()) {
            send_msg_all_p(m, null, true);
            return;
        }

        int count = 0; // Đổi byte thành int để chứa được max_p lớn hơn 127

        for (Player p0 : players) {
            Player p0_local = p0; // [SHADOWING PLAYER]
            if (p0_local == null)
                continue;

            io.Session localConn = p0_local.conn; // [SHADOWING SESSION]
            if (localConn != null) {
                try {
                    // 1. KIỂM TRA QUYỀN VIP (Mối liên kết Sư phụ - Đệ tử)
                    boolean isForced = false;
                    if (forced_indices != null && forced_indices.length > 0) {
                        for (int forcedId : forced_indices) {
                            // Trường hợp 1: Chính chủ (người tạo ra hiệu ứng/skill)
                            if (p0_local.index_map == forcedId) {
                                isForced = true;
                                break;
                            }
                            // Trường hợp 2: Tôi là Sư phụ của người trong danh sách VIP
                            if (p0_local.myDisciple != null && p0_local.myDisciple.index_map == forcedId) {
                                isForced = true;
                                break;
                            }
                            // Trường hợp 3: Tôi là Đệ tử của người trong danh sách VIP
                            if (p0_local instanceof client.Disciple) {
                                client.Disciple d = (client.Disciple) p0_local;
                                if (d.master != null && d.master.index_map == forcedId) {
                                    isForced = true;
                                    break;
                                }
                            }
                        }
                    }

                    // 2. SỔ ĐEN
                    if (!isForced && p0_local.index_map == ignore_index) {
                        continue;
                    }

                    // 3. XUẤT GÓI TIN VỚI MAX_P LINH HOẠT
                    if (isForced) {
                        // VIP luôn nhận tin, không tính vào count và không check distance
                        localConn.addmsg(m);
                    } else if (count < max_p) {
                        // int distance = Math.abs(p0_local.x - x) + Math.abs(p0_local.y - y);
                        // if (distance < VIEW_DISTANCE_MANHATTAN) {
                        // localConn.addmsg(m);
                        // count++;
                        // }
                        localConn.addmsg(m);
                        count++;
                    }
                } catch (Exception e) {
                    core.GameLogger.error("[send_msg_all_p_distance] Lỗi gửi tin cho player " + p0_local.name, e);
                }
            }
        }
    }

    public void rejoin_map(Player p) throws IOException {
        // [GUARD] Đảm bảo player vẫn đang được track bởi map thread này
        if (!players.contains(p)) {
            core.GameLogger.warn("[rejoin_map] Desync detected for " + p.name + ". Redirecting to town.");
            Vgo vgo = new Vgo();
            vgo.map_go = Map.get_map_by_id(1); // Làng Windmill
            vgo.xnew = (short) 422;
            vgo.ynew = (short) 398;
            p.goto_map(vgo);
            return;
        }

        if (p.conn != null) {
            this.send_data(p); // Sync tiles/map info
            this.enter_zone(p); // Sync người chơi xung quanh
            Service.Main_char_Info(p); // Sync chỉ số cá nhân

            // Ép client cập nhật tọa độ thực tế
            Message m = new Message(1);
            m.writer().writeByte(0);
            m.writer().writeShort(p.index_map);
            m.writer().writeShort(p.x);
            m.writer().writeShort(p.y);
            p.addmsg(m);
            m.cleanup();
        }
    }
}
