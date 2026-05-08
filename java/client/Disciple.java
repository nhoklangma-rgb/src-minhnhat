package client;

import core.Service;
import core.Util;
import core.GameLogger;
import io.Message;
import io.Session;
import java.io.IOException;
import map.Mob;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import template.Option;
import template.ItemFashionP;
import template.ItemFashionP2;

public class Disciple extends Player {
    public Player master;
    public byte status; // 0: Nghi (Hien tren map, chay theo), 1: Tan cong quai, 2: Thu hoi (Mat khoi
                        // map), 3: Bao ve
    // public byte clazz; // 1: Vo si, 2: Kiem khach, 3: Dau bep, 4: Hoa tieu, 5: Xa
    // thu
    public Mob targetMob;
    public Player targetPlayer;
    public long lastUpdateAI = 0;
    private long deadTime = 0;
    private final java.util.Queue<Runnable> attackQueue = new java.util.ArrayDeque<>();
    private long nextAttackTime = 0;
    public long timeTanSat = 0;

    // JSON Template mặc định cho các phái (trích xuất từ Session.java)
    private static final String[] DEFAULT_BODY_WEAR_JSON = {
            "[[0,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[5,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[10,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]", // Luffy
            "[[1,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[6,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[11,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]", // Zoro
            "[[2,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[7,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[12,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]", // Sanji
            "[[3,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[8,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[13,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]", // Nami
            "[[4,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[9,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[14,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]" // Usopp
    };

    public Disciple(Player master) throws IOException {
        super(new Session(true), master.name + "'s Đệ");
        this.master = master;
        this.clone = true;
        this.isPetVisible = true;
        this.status = 2; // Mat Dinh: o nha
        this.id = -master.id; // phan biet ID am (Internal ID)
        // index_map will be assigned by Map.enter_map using getNextCloneId()
        setupBase();
    }

    @Override
    public boolean isDisciple() {
        return true;
    }

    public void chat(String msg) {
        try {
            Message m = new Message(17);
            m.writer().writeShort(this.index_map);
            m.writer().writeByte(0);
            m.writer().writeUTF(msg);
            if (this.map != null) {
                this.map.send_msg_all_p_distance(m, this.x, this.y, false, 6, -1, this.master.index_map);
            }
            m.cleanup();
        } catch (Exception e) {
            GameLogger.warn("Disciple.chat: Error sending message '" + msg + "' for disciple of " + master.name, e);
        }
    }

    public void stopFighting() {
        this.targetMob = null;
        this.targetPlayer = null;
        this.attackQueue.clear();
    }

    public void resetAfterRitual() {
        this.isdie = false;
        this.invalidateStatCache(); // [CORE] Tái tính toán lại các chỉ số Max HP/MP dựa trên level mới
        this.hp = this.body.get_hp_max(true);
        this.mp = this.body.get_mp_max(true);
        this.stopFighting(); // [SAFETY] Xóa sạch hàng đợi combo cũ và mục tiêu
        try {
            this.update_info_to_all();
            // [SYNC] Cập nhật ngoại trang (level, trang bị) cho tất cả người chơi xung
            // quanh
            if (this.map != null) {
                for (Player p0 : this.map.players) {
                    if (p0 != null && p0.conn != null) {
                        Service.charWearing(this, p0, false);
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("Disciple.resetAfterRitual: Error syncing info", e);
        }
    }

    public boolean isMapCamDeTu(map.Map checkMap) {
        if (checkMap == null || checkMap.template == null)
            return false;
        if (checkMap.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID)
            return true;
        if (checkMap.template.id == 119)
            return true; // Truy na
        if (checkMap.map_pvp != null || checkMap.map_dungeon != null || checkMap.clan_resource != null
                || checkMap.map_little_garden != null || checkMap.map_LienTang != null
                || checkMap.map_Hang != null || checkMap.vuonCam != null || checkMap.pvpBang != null
                || checkMap.pvpBangMapFight != null || checkMap.map_SieuLienTang != null
                || checkMap.map_LanhDiaBang != null || checkMap.isMazeMap || checkMap.map_VoHanLienTang != null
                || checkMap.map_ThuThachVeThan != null) {
            return true;
        }
        return false;
    }

    private void setupBase() {
        this.level = 1;
        this.exp = 0;
        this.thongthao = 0;
        this.is_show_weapon = true;
        this.is_show_hat = true;
        this.item = new Item(this);
        this.body = new Body(this);

        // [CHUẨN] Khởi tạo mảng chống crash khi gọi skill
        if (this.daHanhTrinh == null) {
            this.daHanhTrinh = new java.util.ArrayList<>();
        }

        if (this.clazz == 0) {
            this.clazz = (byte) Util.random(1, 6);
        }

        // Đảm bảo đệ tử luôn có tối thiểu 1 điểm tiềm năng ban đầu để tránh lỗi logic
        this.point1 = 1;
        this.point2 = 1;
        this.point3 = 1;
        this.point4 = 1;

        refreshExterior();
        initDefaultGear();
        learnDefaultSkills();
        this.hp = this.body.get_hp_max(true);
        this.mp = this.body.get_mp_max(true);
        this.type_pk = -1;
        this.typePirate = -1;
        generateDFRequirements(); // Đảm bảo đệ tử mới luôn có 6 trái yêu cầu
    }

    public void refreshExterior() {
        // Khoi tao ngoai hinh mac dinh theo phai
        switch (this.clazz) {
            case 1:
                this.hair = 1; // toc luffy
                break;
            case 2:
                this.hair = 24; // toc zoro
                break;
            case 3:
                this.hair = 28; // toc sanji
                break;
            case 4:
                this.hair = 32; // toc nami
                break;
            case 5:
                this.hair = 36; // toc usopp
                break;
            default:
                this.hair = 1;
                break;
        }
    }

    private void initDefaultGear() {
        this.item.it_body = new template.Item_wear[8];
        this.item.second_body = new template.Item_wear[8];
        this.item.third_body = new template.Item_wear[8];
        if (this.clazz >= 1 && this.clazz <= 5) {
            try {
                org.json.simple.JSONArray js_body = (org.json.simple.JSONArray) org.json.simple.JSONValue
                        .parse(DEFAULT_BODY_WEAR_JSON[this.clazz - 1]);
                if (js_body != null) {
                    for (int i = 0; i < js_body.size(); i++) {
                        org.json.simple.JSONArray js_it = (org.json.simple.JSONArray) js_body.get(i);
                        template.Item_wear it = new template.Item_wear();
                        Item.readUpdateItem(js_it.toJSONString(), it);
                        if (it.index < this.item.it_body.length) {
                            this.item.it_body[it.index] = it;
                        }
                    }
                }
            } catch (Exception e) {
                GameLogger.error("Disciple.initDefaultGear: Error initializing gear for disciple of " + master.name, e);
            }
        }
    }

    private void loadFashionFromJSON(JSONArray js_fas) {
        if (js_fas == null)
            return;
        this.fashion = new ArrayList<>();
        for (int i = 0; i < js_fas.size(); i++) {
            JSONArray js_temp = (JSONArray) js_fas.get(i);
            ItemFashionP2 tempf = new ItemFashionP2();
            tempf.id = Short.parseShort(js_temp.get(0).toString());
            tempf.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
            tempf.level = Byte.parseByte(js_temp.get(2).toString());
            tempf.expirationTime = js_temp.size() > 3 ? Long.parseLong(js_temp.get(3).toString()) : -1;

            JSONArray js_op = js_temp.size() > 4 ? (JSONArray) js_temp.get(4) : new JSONArray();
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
    }

    private void loadItFashionPFromJSON(JSONArray js_itfas) {
        if (js_itfas == null)
            return;
        this.itfashionP = new ArrayList<>();
        for (int i = 0; i < js_itfas.size(); i++) {
            JSONArray js_temp = (JSONArray) js_itfas.get(i);
            ItemFashionP tempf = new ItemFashionP();
            tempf.category = Byte.parseByte(js_temp.get(0).toString());
            tempf.id = Short.parseShort(js_temp.get(1).toString());
            tempf.icon = Short.parseShort(js_temp.get(2).toString());
            tempf.is_use = Byte.parseByte(js_temp.get(3).toString()) == 1;
            this.itfashionP.add(tempf);
        }
    }

    @SuppressWarnings("unchecked")
    private JSONArray saveFashionToJSON() {
        JSONArray js_fas = new JSONArray();
        for (ItemFashionP2 fas : this.fashion) {
            JSONArray js_entry = new JSONArray();
            js_entry.add(fas.id);
            js_entry.add(fas.is_use ? 1 : 0);
            js_entry.add(fas.level);
            js_entry.add(fas.expirationTime);

            JSONArray js_op = new JSONArray();
            if (fas.op != null) {
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
            js_entry.add(js_op);
            js_fas.add(js_entry);
        }
        return js_fas;
    }

    @SuppressWarnings("unchecked")
    private JSONArray saveItFashionPToJSON() {
        JSONArray js_itfas = new JSONArray();
        for (ItemFashionP itfas : this.itfashionP) {
            JSONArray js_entry = new JSONArray();
            js_entry.add(itfas.category);
            js_entry.add(itfas.id);
            js_entry.add(itfas.icon);
            js_entry.add(itfas.is_use ? 1 : 0);
            js_itfas.add(js_entry);
        }
        return js_itfas;
    }

    private void learnDefaultSkills() {
        this.skill_point.clear();

        // Bảng Icon chuẩn cho 3 chiêu cơ bản (Xác định tuyệt đối qua Icon ID)
        int[][] SKILL_ICONS = {
                { 0, 3, 6, 9, 12 }, // Slot 1 (id_2=0)
                { 1, 4, 7, 10, 13 }, // Slot 2 (id_2=1)
                { 2, 5, 8, 11, 14 } // Slot 3 (id_2=2)
        };

        if (this.clazz >= 1 && this.clazz <= 5) {
            for (int i = 0; i < SKILL_ICONS.length; i++) {
                int targetIcon = SKILL_ICONS[i][this.clazz - 1];
                for (template.Skill_Template temp : template.Skill_Template.ENTRYS) {
                    // Tìm đúng chiêu theo Icon và yêu cầu cấp 1 (Lv_RQ = 1)
                    if (temp.idIcon == targetIcon && temp.Lv_RQ == 1) {
                        template.Skill_info sk = new template.Skill_info();
                        sk.temp = temp;
                        sk.realLevel = 1;
                        sk.exp = 0;
                        this.skill_point.add(sk);
                        break;
                    }
                }
            }
        }
    }

    public void loadData(String jsonString) {
        if (jsonString == null || jsonString.equals("null")) {
            return;
        }
        try {
            JSONObject js = (JSONObject) new JSONParser().parse(jsonString);
            if (js.get("level") != null) {
                this.level = Short.parseShort(js.get("level").toString());
            }
            if (js.get("exp") != null) {
                this.exp = Long.parseLong(js.get("exp").toString());
            }
            if (js.get("hp") != null) {
                this.hp = Long.parseLong(js.get("hp").toString());
            }
            if (js.get("mp") != null) {
                this.mp = Long.parseLong(js.get("mp").toString());
            }
            if (js.get("status") != null) {
                this.status = Byte.parseByte(js.get("status").toString());
            }
            if (js.get("clazz") != null) {
                this.clazz = Byte.parseByte(js.get("clazz").toString());
                refreshExterior();
            }

            if (js.get("head") != null) {
                this.head = Short.parseShort(js.get("head").toString());
            }
            if (js.get("hair") != null) {
                this.hair = Short.parseShort(js.get("hair").toString());
            }

            if (this.clazz == 0) {
                this.clazz = (byte) Util.random(1, 6);
                refreshExterior();
            }

            if (js.get("reincarnation") != null) {
                this.reincarnation = Integer.parseInt(js.get("reincarnation").toString());
            }
            if (js.get("levelAwaken") != null) {
                this.levelAwaken = Byte.parseByte(js.get("levelAwaken").toString());
            }

            // Load Gear with strict set validation (tb2: 0=Main, 1=Set 2, 2=Set 3)
            if (js.get("gear") != null) {
                loadGearFromJSON((org.json.simple.JSONArray) js.get("gear"), this.item.it_body, 0);
            } else {
                initDefaultGear();
            }

            // Load Gear Set 2
            if (js.get("gear2") != null) {
                loadGearFromJSON((org.json.simple.JSONArray) js.get("gear2"), this.item.second_body, 1);
            }

            // Load Gear Set 3
            if (js.get("gear3") != null) {
                loadGearFromJSON((org.json.simple.JSONArray) js.get("gear3"), this.item.third_body, 2);
            }

            // Load Attribute Points (+ Enforce 1 point min for critical stats)
            if (js.get("p1") != null)
                this.point1 = (short) Math.max(1, Short.parseShort(js.get("p1").toString()));
            if (js.get("p2") != null)
                this.point2 = (short) Math.max(1, Short.parseShort(js.get("p2").toString()));
            if (js.get("p3") != null)
                this.point3 = (short) Math.max(1, Short.parseShort(js.get("p3").toString()));
            if (js.get("p4") != null)
                this.point4 = (short) Math.max(1, Short.parseShort(js.get("p4").toString()));
            if (js.get("p5") != null)
                this.point5 = Short.parseShort(js.get("p5").toString());
            if (js.get("pointAttribute") != null)
                this.pointAttribute = Integer.parseInt(js.get("pointAttribute").toString());

            // Load Skills if exists
            if (js.get("skills") != null) {
                loadSkillsFromJSON((org.json.simple.JSONArray) js.get("skills"));
            } else {
                learnDefaultSkills();
            }

            // Load Fashion
            if (js.get("fashion") != null) {
                loadFashionFromJSON((JSONArray) js.get("fashion"));
            }
            if (js.get("itfashionP") != null) {
                loadItFashionPFromJSON((JSONArray) js.get("itfashionP"));
            }

            if (js.get("idDF_req") != null) {
                JSONArray js_df = (JSONArray) js.get("idDF_req");
                for (int i = 0; i < 6 && i < js_df.size(); i++) {
                    this.idDF_required[i] = Short.parseShort(js_df.get(i).toString());
                }
            }
            if (js.get("timeTanSat") != null) {
                this.timeTanSat = Long.parseLong(js.get("timeTanSat").toString());
            }
            if (this.idDF_required[5] == -1) { // Đệ tử cần đủ 6 trái
                generateDFRequirements();
            }
        } catch (Exception e) {
            GameLogger.error(
                    "Disciple.loadData: JSON parse error for disciple of " + (master != null ? master.name : "unknown"),
                    e); // Loi doc JSON
        }
    }

    @SuppressWarnings("unchecked")
    public String saveData() {
        JSONObject js = new JSONObject();
        js.put("level", this.level);
        js.put("exp", this.exp);
        js.put("hp", this.hp);
        js.put("mp", this.mp);
        js.put("status", this.status);
        js.put("clazz", this.clazz);
        js.put("pointAttribute", this.pointAttribute);
        js.put("p1", this.point1);
        js.put("p2", this.point2);
        js.put("p3", this.point3);
        js.put("p4", this.point4);
        js.put("p5", this.point5);
        js.put("head", this.head);
        js.put("hair", this.hair);
        js.put("reincarnation", this.reincarnation);
        js.put("levelAwaken", this.levelAwaken);

        // Save Fashion
        js.put("fashion", saveFashionToJSON());
        js.put("itfashionP", saveItFashionPToJSON());

        // Save Gear sets
        js.put("gear", saveGearToJSON(this.item.it_body));
        js.put("gear2", saveGearToJSON(this.item.second_body));
        js.put("gear3", saveGearToJSON(this.item.third_body));

        // Save Skills
        js.put("skills", saveSkillsToJSON());

        // Save Awakening requirements
        // Save Awakening requirements
        JSONArray js_df = new JSONArray();
        for (short id : this.idDF_required) {
            js_df.add(id);
        }
        js.put("idDF_req", js_df);
        js.put("timeTanSat", this.timeTanSat);

        return js.toJSONString();
    }

    private void loadSkillsFromJSON(org.json.simple.JSONArray js_skills) {
        if (js_skills == null)
            return;
        this.skill_point.clear();
        for (int i = 0; i < js_skills.size(); i++) {
            org.json.simple.JSONArray js_sk = (org.json.simple.JSONArray) js_skills.get(i);
            template.Skill_info sk = new template.Skill_info();
            sk.exp = Long.parseLong(js_sk.get(1).toString());
            sk.temp = template.Skill_Template.get_temp(Short.parseShort(js_sk.get(0).toString()), sk.exp);
            if (sk.temp != null) {
                sk.lvdevil = Byte.parseByte(js_sk.get(2).toString());
                sk.devilpercent = Byte.parseByte(js_sk.get(3).toString());
                if (js_sk.size() > 4) {
                    sk.fruitId = Short.parseShort(js_sk.get(4).toString());
                }
                sk.realLevel = sk.temp.Lv_RQ; // Cập nhật level thực tế từ template sau khi nạp exp
                this.skill_point.add(sk);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private org.json.simple.JSONArray saveSkillsToJSON() {
        org.json.simple.JSONArray js_skills = new org.json.simple.JSONArray();
        for (template.Skill_info sk : this.skill_point) {
            org.json.simple.JSONArray js_sk = new org.json.simple.JSONArray();
            js_sk.add(sk.temp.indexSkillInServer);
            js_sk.add(sk.exp);
            js_sk.add(sk.lvdevil);
            js_sk.add(sk.devilpercent);
            js_sk.add(sk.fruitId);
            js_skills.add(js_sk);
        }
        return js_skills;
    }

    private void loadGearFromJSON(org.json.simple.JSONArray js_gear, template.Item_wear[] target_body,
            int targetSet) {
        if (js_gear == null || target_body == null)
            return;
        for (int i = 0; i < js_gear.size(); i++) {
            org.json.simple.JSONArray js_it = (org.json.simple.JSONArray) js_gear.get(i);
            template.Item_wear it = new template.Item_wear();
            Item.readUpdateItem(js_it.toJSONString(), it);

            // [VALIDATION] Only load item if it matches the target gear set (tb2)
            // tb2: 0 = Main, 1 = Set 2, 2 = Set 3
            if (it.template != null && it.template.tb2 != targetSet) {
                core.GameLogger.warn("[Gear Load] item id=" + it.template.id
                        + " tb2=" + it.template.tb2
                        + " targetSet=" + targetSet
                        + " -> SKIPPED for " + master.name);
                continue;
            }

            if (it.index >= 0 && it.index < target_body.length) {
                target_body[it.index] = it;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private org.json.simple.JSONArray saveGearToJSON(template.Item_wear[] target_body) {
        org.json.simple.JSONArray js_gear = new org.json.simple.JSONArray();
        if (target_body != null) {
            for (int i = 0; i < target_body.length; i++) {
                if (target_body[i] != null && target_body[i].template != null) {
                    js_gear.add(Item.it_data_to_json(target_body[i]));
                }
            }
        }
        return js_gear;
    }

    public void wear_item_disciple(template.Item_wear it, Player master) throws IOException {
        if (this.level < it.template.level) {
            core.Service.send_box_ThongBao_OK(master, "Đệ tử chưa đủ cấp độ để mặc vật phẩm này");
            master.use_item_3 = -1;
            return;
        }
        if (it.template.clazz != 0 && this.clazz != it.template.clazz) {
            core.Service.send_box_ThongBao_OK(master, "Hệ phái đệ tử không phù hợp với vật phẩm này");
            master.use_item_3 = -1;
            return;
        }

        // Tự động khóa trang bị khi mặc
        if (it.valueKichAn != 12) {
            it.typelock = 1;
        }

        short id_fashion = it.template.id;
        boolean is_fashion = false;

        // Kiểm tra nếu là thời trang mới (FashionP2)
        if (it.template != null && it.template.typeEquip == -1) {
            is_fashion = true;
            // Tháo bộ FashionP2 cũ nếu đang dùng
            for (ItemFashionP2 f : this.fashion) {
                if (f.is_use) {
                    f.is_use = false;
                }
            }

            // Kiểm tra xem đã có bộ này chưa
            ItemFashionP2 f_found = null;
            for (ItemFashionP2 f : this.fashion) {
                if (f.id == id_fashion) {
                    f_found = f;
                    break;
                }
            }

            if (f_found != null) {
                f_found.is_use = true;
            } else {
                ItemFashionP2 newF = new ItemFashionP2();
                newF.id = id_fashion;
                newF.is_use = true;
                newF.level = 1;
                newF.op = ItemFashionP2.randomOptionsForFashion(id_fashion);
                this.fashion.add(newF);
            }
            // Xóa khỏi túi đồ sư phụ
            if (it.index >= 0 && it.index < master.item.bag3.length) {
                master.item.bag3[it.index] = null;
            }
        }

        if (!is_fashion) {
            if (it.template == null) {
                core.Service.send_box_ThongBao_OK(master, "Dữ liệu trang bị lỗi!");
                master.use_item_3 = -1;
                return;
            }

            // [FIX] Access typeEquip directly from template to avoid potential NPE
            byte index_wear = it.template.typeEquip;
            if (index_wear != -1) {
                // Lấy vật phẩm từ túi đồ sư phụ
                if (it.index < 0 || it.index >= master.item.bag3.length || master.item.bag3[it.index] == null
                        || master.item.bag3[it.index] != it) {
                    core.Service.send_box_ThongBao_OK(master, "Vật phẩm không hợp lệ!");
                    master.use_item_3 = -1;
                    return;
                }
                int emptySlotInBag = it.index; // Lưu lại vị trí ô vừa lấy đồ ra
                master.item.bag3[emptySlotInBag] = null;

                template.Item_wear[] target_body;
                if (it.template.tb2 == 1) {
                    target_body = this.item.second_body;
                } else if (it.template.tb2 == 2) {
                    target_body = this.item.third_body;
                } else {
                    target_body = this.item.it_body;
                }

                // Xử lý đổi đồ: Gán thẳng món cũ vào ô trống vừa tạo ra trong túi
                if (index_wear >= 0 && index_wear < target_body.length && target_body[index_wear] != null) {
                    master.item.bag3[emptySlotInBag] = target_body[index_wear];
                    target_body[index_wear].index = (byte) emptySlotInBag; // Cập nhật lại index hành trang cho món đồ
                                                                           // cũ
                    target_body[index_wear] = null;
                }

                // Mặc món đồ mới cho đệ tử
                if (index_wear >= 0 && index_wear < target_body.length) {
                    target_body[index_wear] = it;
                    it.index = index_wear;
                } else {
                    core.Service.send_box_ThongBao_OK(master, "Vị trí trang bị không hợp lệ!");
                    master.item.bag3[emptySlotInBag] = it; // Trả lại túi
                }
            }
        }

        // Cập nhật hành trang cho sư phụ
        master.item.update_Inventory(-1, false);

        // Cập nhật thông tin và ngoại hình đệ tử cho mọi người xung quanh
        this.invalidateStatCache();
        this.update_info_to_all();
        if (this.map != null) {
            for (Player p0 : this.map.players) {
                core.Service.charWearing(this, p0, false);
            }
        }

        master.use_item_3 = -1;
        core.Service.send_box_ThongBao_OK(master, "Đệ tử đã mặc trang bị thành công!");
    }

    /**
     * Quy tắc 2: Tách riêng hàm xử lý Trái Ác Quỷ an toàn (Data update only).
     */
    public void wear_fashion_disciple(int id, int cat) throws IOException {
        if (this.master != null) {
            if (cat == 105) {
                ItemFashionP2 f_mas = this.master.check_fashion(id);
                if (f_mas != null && f_mas.is_use) {
                    f_mas.is_use = false;
                    this.master.update_info_to_all();
                    if (this.master.map != null) {
                        for (int i = 0; i < this.master.map.players.size(); i++) {
                            Service.charWearing(this.master, this.master.map.players.get(i), false);
                        }
                    }
                }
            } else {
                ItemFashionP f_mas = this.master.check_itfashionP(id, cat);
                if (f_mas != null && f_mas.is_use) {
                    f_mas.is_use = false;
                    this.master.update_info_to_all();
                    if (this.master.map != null) {
                        for (int i = 0; i < this.master.map.players.size(); i++) {
                            Service.charWearing(this.master, this.master.map.players.get(i), false);
                        }
                    }
                }
            }
        }
        if (cat == 105) { // Fashion P2
            boolean found = false;
            for (ItemFashionP2 f : this.fashion) {
                if (f.id == id) {
                    f.is_use = true;
                    found = true;
                } else {
                    f.is_use = false;
                }
            }
            if (!found) {
                ItemFashionP2 newF = new ItemFashionP2();
                newF.id = (short) id;
                newF.is_use = true;
                newF.level = 1;
                newF.op = ItemFashionP2.randomOptionsForFashion((short) id);
                this.fashion.add(newF);
            }
        } else { // Fashion P (103, 108...)
            boolean found = false;
            for (ItemFashionP f : this.itfashionP) {
                if (f.category == cat && f.id == id) {
                    f.is_use = true;
                    found = true;
                } else if (f.category == cat) {
                    f.is_use = false;
                }
            }
            if (!found) {
                ItemFashionP f = new ItemFashionP();
                f.category = (byte) cat;
                f.id = (short) id;
                f.is_use = true;
                template.ItemHair it = template.ItemHair.get_item(id, cat);
                if (it != null)
                    f.icon = it.idIcon;
                this.itfashionP.add(f);
            }
        }

        this.invalidateStatCache();
        this.update_info_to_all();
        if (this.map != null) {
            for (Player p0 : this.map.players) {
                core.Service.charWearing(this, p0, false);
            }
        }
    }

    public void eat_devil_fruit(int fruitId) throws IOException {
        if (fruitId > 4000) {
            // Already handled or direct item ID
        } else if (fruitId > 0 && fruitId < 1000) {
            fruitId += 4000;
        }

        // 1. Identify current fruits (including legacy ones)
        java.util.List<Integer> fruitList = new java.util.ArrayList<>();
        for (template.Skill_info sk : this.skill_point) {
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
                if (!fruitList.contains(-1)) {
                    fruitList.add(-1);
                }
            }
        }

        // 2. Manage limit and removal
        if (fruitList.contains(fruitId)) {
            if (this.master != null) {
                Service.send_box_ThongBao_OK(this.master, "Đệ tử đã sở hữu Trái Ác Quỷ này rồi!");
            }
            return;
        }

        java.util.List<template.Skill_info> toRemove = new java.util.ArrayList<>();
        while (fruitList.size() >= (this.levelAwaken + 1)) {
            int fruitToRemove = fruitList.remove(0);
            for (template.Skill_info sk : this.skill_point) {
                if (sk.temp == null)
                    continue;
                int skFruitId = sk.fruitId;
                if (skFruitId <= 0) {
                    skFruitId = Player.getFruitIdBySkillIdx(sk.temp.indexSkillInServer);
                }

                if (fruitToRemove == -1) {
                    if (skFruitId <= 0 && sk.temp.ID > 2000
                            && !(sk.temp.indexSkillInServer >= 660 && sk.temp.indexSkillInServer <= 685)
                            && !(sk.temp.indexSkillInServer >= 800 && sk.temp.indexSkillInServer <= 870)) {
                        try {
                            activities.Learn_Skill.remove_skill(this, sk);
                        } catch (Exception e) {
                        }
                        toRemove.add(sk);
                    }
                } else if (skFruitId == fruitToRemove) {
                    try {
                        activities.Learn_Skill.remove_skill(this, sk);
                    } catch (Exception e) {
                    }
                    toRemove.add(sk);
                }
            }
        }
        this.skill_point.removeAll(toRemove);

        // 3. Add 3 new skills from the fruit
        int[] skillIds = core.DevilFruitManager.getSkillsByFruit(fruitId);
        for (int id : skillIds) {
            template.Skill_Template temp = template.Skill_Template.get_temp(id, 0L);
            if (temp != null) {
                template.Skill_info sk = new template.Skill_info();
                sk.temp = temp;
                sk.realLevel = 1;
                sk.exp = 0;
                sk.fruitId = (short) fruitId;
                this.skill_point.add(sk);
            }
        }

        // 4. Update stats safely using centralized logic
        this.invalidateStatCache();
        try {
            this.update_info_to_all();

            // 5. Notify master
            if (this.master != null) {
                int baseId = fruitId >= 4000 ? fruitId - 4000 : fruitId;
                template.ItemTemplate4 temp = template.ItemTemplate4.get_it_by_id(baseId);
                String name = (temp != null) ? temp.name : ("Trái Ác Quỷ #" + baseId);
                Service.send_box_ThongBao_OK(this.master, "Đệ tử đã hấp thụ thành công " + name + "!");
            }
        } catch (IOException e) {
            GameLogger.error("Disciple.eat_devil_fruit: Error notifying master " + master.name, e);
        }
    }

    public synchronized void updateDiscipleAI() {
        if (this.master == null || this.master.map != this.map || this.master.fusionType != 0) {
            return;
        }
        fixNegativeStats(); // [RECOVERY] Sửa ngay nếu bị âm do tràn số (overflow)
        if (this.isdie) {
            if (System.currentTimeMillis() - deadTime > 60 * 1000) { // 1 min
                this.isdie = false;
                this.hp = this.body.get_hp_max(true);
                this.mp = this.body.get_mp_max(true);
                this.chat("Con đã sống lại rồi thưa sư phụ!");
            } else {
                return;
            }
        }

        if (this.status == 2) {
            return;
        }

        if (master == null || master.map == null) {
            return;
        }

        // 1. Map Synchronization
        if (this.map == null || !master.map.equals(this.map)) {
            if (isMapCamDeTu(master.map)) {
                map.Map oldMap = this.map;
                this.map = null;
                try {
                    if (oldMap != null) {
                        oldMap.leave_map(this, 0);
                    }
                } catch (Exception e) {
                    GameLogger.warn("Disciple.AI: leave_map error (Cam Map)", e);
                }
                this.status = 2; // Auto-Recall
                return;
            }
            map.Map oldMap = this.map;
            this.map = null;
            try {
                if (oldMap != null) {
                    oldMap.leave_map(this, 0);
                }
            } catch (Exception e) {
                GameLogger.warn("Disciple.AI: leave_map error", e);
            }
            this.map = master.map;
            this.x = master.x;
            this.y = master.y;
            try {
                this.map.enter_map(this);
                // Broadcast spawn position to everyone
                Message m = new Message(1);
                m.writer().writeByte(0);
                m.writer().writeShort(this.index_map);
                m.writer().writeShort(this.x);
                m.writer().writeShort(this.y);
                this.map.send_msg_all_p(m, null, true);
                m.cleanup();

                // Broadcast full info to everyone
                this.update_info_to_all();
            } catch (IOException e) {
                GameLogger.error("Disciple.AI: Error entering map " + master.map.template.name, e);
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdateAI < 200) {
            return;
        }
        lastUpdateAI = now;

        // Kiểm tra hết hạn chế độ Tàn Sát
        if (this.status == 4 && now > timeTanSat) {
            this.status = 1;
            try {
                this.chat("Sư phụ ơi, con mệt quá!");
                if (master != null) {
                    Service.send_box_ThongBao_OK(master, "Chế độ Tàn Sát của đệ tử đã hết hiệu lực.");
                }
            } catch (java.io.IOException e) {
                core.GameLogger.error("Disciple.AI: Error sending Tan Sat expiry notification", e);
            }
        }

        // 2. Recovery & Sync
        if (this.hp > 0 && this.hp < this.body.get_hp_max(true)) {
            this.hp += this.body.get_hp_max(true) / 10;
        }
        if (this.mp < this.body.get_mp_max(true)) {
            this.mp += this.body.get_mp_max(true) / 10;
        }
        this.type_pk = master.type_pk;
        this.typePirate = master.typePirate;

        autoAddPoint();

        // 3. Movement Decision
        int distToMasterX = Math.abs(this.x - master.x);
        int distToMasterY = Math.abs(this.y - master.y);
        int totalDistToMaster = distToMasterX + distToMasterY;

        // Force follow if too far (even in combat)
        if (totalDistToMaster > 600) { // Tăng lên 600 để không cản trở đệ tử đang đánh quái
            this.x = (short) (master.x + Util.random(-40, 40));
            this.y = master.y;
            try {
                // Ép client render lại đệ tử ở vị trí mới
                this.update_info_to_all();
            } catch (IOException e) {
                GameLogger.error("Disciple.AI: Error teleporting to master " + master.name, e);
            }
            return;
        }

        if (this.status == 0) {
            // FOLLOW MODE
            handleMovement(master.x, master.y, 60, 250);
        } else if (this.status == 1) {
            // [ENHANCED] ATTACK MODE - Dịch chuyển và đánh như Tàn sát
            updateTanSatMode();
        } else if (this.status == 3) {
            // PROTECT MODE
            if (targetPlayer != null && !targetPlayer.isdie && targetPlayer.map == this.map) {
                int distToPlayer = Math.abs(this.x - targetPlayer.x) + Math.abs(this.y - targetPlayer.y);
                if (distToPlayer > 60) {
                    handleMovement(targetPlayer.x, targetPlayer.y, 50, 400);
                } else {
                    attackPlayer(targetPlayer);
                }
            } else {
                handleMovement(master.x, master.y, 60, 250);
            }
        } else if (this.status == 4) {
            // TÀN SÁT MODE
            updateTanSatMode();
        }
    }

    private void updateTanSatMode() {
        if (this.map == null)
            return;

        // 1. Tìm quái gần nhất trên toàn map (không giới hạn leash)
        if (targetMob == null || targetMob.isdie || targetMob.map == null) {
            targetMob = null;
            long minDis = 5000; // Quét cực rộng
            for (Mob m : this.map.mobs_cache) {
                if (!m.isdie) {
                    int curDis = Math.abs(m.x - this.x) + Math.abs(m.y - this.y);
                    if (curDis < minDis) {
                        minDis = curDis;
                        targetMob = m;
                    }
                }
            }
        }

        if (targetMob != null) {
            // 2. Dịch chuyển tức thời tới quái
            if (this.x != targetMob.x || this.y != targetMob.y) {
                this.x = targetMob.x;
                this.y = targetMob.y;
                try {
                    // Gửi gói tin di chuyển tức thời
                    this.map.send_move(this, this.x, this.y);
                } catch (java.io.IOException e) {
                }
            }
            // 3. Tấn công
            attackMob(targetMob);
        } else {
            // Không có quái thì bay về master
            handleMovement(master.x, master.y, 60, 250);
        }
    }

    private void handleMovement(int targetX, int targetY, int minStopDist, int maxTeleportDist) {
        int dx = Math.abs(this.x - targetX);
        int dy = Math.abs(this.y - targetY);
        int dist = dx + dy;

        if (dist <= minStopDist) {
            return; // Already close enough
        }

        if (dist > maxTeleportDist) {
            // Pha dịch chuyển bắt kịp mục tiêu
            this.x = (short) (targetX + Util.random(-30, 30)); // (Hoặc master.x tùy vị trí bạn dán)
            this.y = (short) targetY;

            try {
                // Tạo gói tin Di chuyển/Xuất hiện (Message 1)
                io.Message m = new io.Message(1);
                m.writer().writeByte(0); // 0: Type Player/Disciple
                m.writer().writeShort(this.index_map);
                m.writer().writeShort(this.x);
                m.writer().writeShort(this.y);

                // [CHUẨN BÀI TẠI ĐÂY]
                // Truyền Message, tọa độ x, y của Đệ tử, và false (để kích hoạt check khoảng
                // cách)
                this.map.send_msg_all_p_distance(m, this.x, this.y, false, 6, this.index_map, master.index_map);

                m.cleanup();
            } catch (Exception e) {
                core.GameLogger.warn("Disciple.AI: Teleport send move error for " + master.name, e);
            }
        } else {
            // Bước đi mượt (Không vượt quá target)
            int step = 50;
            int dxMove = 0;
            int dyMove = 0;

            if (this.x < targetX) {
                dxMove = Math.min(step, targetX - this.x);
            } else if (this.x > targetX) {
                dxMove = -Math.min(step, this.x - targetX);
            }

            if (this.y < targetY) {
                dyMove = Math.min(step, targetY - this.y);
            } else if (this.y > targetY) {
                dyMove = -Math.min(step, this.y - targetY);
            }

            this.x += dxMove;
            this.y += dyMove;

            try {
                this.map.send_move(this, this.x, this.y);
            } catch (IOException e) {
                GameLogger.warn("Disciple.AI: Move error for " + master.name, e);
            }
        }
    }

    public void autoAddPoint() {
        if (this.pointAttribute <= 0)
            return;

        int pointsToAdd = Util.random(1, 6);
        if (pointsToAdd > this.pointAttribute)
            pointsToAdd = this.pointAttribute;

        int maxStat = Math.min(template.Skill_info.STAT_POINT_MAX, 32000); // Giới hạn an toàn cho kiểu short
        int totalAdded = 0;

        for (int i = 0; i < pointsToAdd; i++) {
            boolean added = false;
            int rand = Util.random(1, 101);
            switch (this.clazz) {
                case 1: // Luffy - Vo si: The luc > Suc manh > Phong thu > Nhanh nhen
                    if (rand <= 45 && this.point3 < maxStat) {
                        this.point3++;
                        added = true;
                    } else if (rand <= 75 && this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    } else if (rand <= 95 && this.point2 < maxStat) {
                        this.point2++;
                        added = true;
                    } else if (this.point5 < maxStat) {
                        this.point5++;
                        added = true;
                    }
                    break;
                case 2: // Zoro - Kiem khach: Suc manh > Phong thu > The luc > Nhanh nhen > Tinh than
                    if (rand <= 45 && this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    } else if (rand <= 70 && this.point2 < maxStat) {
                        this.point2++;
                        added = true;
                    } else if (rand <= 90 && this.point3 < maxStat) {
                        this.point3++;
                        added = true;
                    } else if (rand <= 95 && this.point5 < maxStat) {
                        this.point5++;
                        added = true;
                    } else if (this.point4 < maxStat) {
                        this.point4++;
                        added = true;
                    }
                    break;
                case 3: // Sanji - Dau bep: Nhanh nhen > Suc manh > Tinh than > The luc > Phong thu
                    if (rand <= 40 && this.point5 < maxStat) {
                        this.point5++;
                        added = true;
                    } else if (rand <= 70 && this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    } else if (rand <= 85 && this.point4 < maxStat) {
                        this.point4++;
                        added = true;
                    } else if (rand <= 95 && this.point3 < maxStat) {
                        this.point3++;
                        added = true;
                    } else if (this.point2 < maxStat) {
                        this.point2++;
                        added = true;
                    }
                    break;
                case 4: // Nami - Hoa tieu: Tinh than > Nhanh nhen > The luc > Phong thu > Suc manh
                    if (rand <= 45 && this.point4 < maxStat) {
                        this.point4++;
                        added = true;
                    } else if (rand <= 75 && this.point5 < maxStat) {
                        this.point5++;
                        added = true;
                    } else if (rand <= 90 && this.point3 < maxStat) {
                        this.point3++;
                        added = true;
                    } else if (rand <= 95 && this.point2 < maxStat) {
                        this.point2++;
                        added = true;
                    } else if (this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    }
                    break;
                case 5: // Usopp - Xa thu: Tinh than > Suc manh > Nhanh nhen > The luc > Phong thu
                    if (rand <= 40 && this.point4 < maxStat) {
                        this.point4++;
                        added = true;
                    } else if (rand <= 70 && this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    } else if (rand <= 85 && this.point5 < maxStat) {
                        this.point5++;
                        added = true;
                    } else if (rand <= 95 && this.point3 < maxStat) {
                        this.point3++;
                        added = true;
                    } else if (this.point2 < maxStat) {
                        this.point2++;
                        added = true;
                    }
                    break;
                default:
                    if (this.point1 < maxStat) {
                        this.point1++;
                        added = true;
                    }
                    break;
            }

            // [SMART FALLBACK] Nếu các mục ưu tiên đã đầy, tìm bất kỳ mục nào còn trống
            if (!added) {
                if (this.point1 < maxStat) {
                    this.point1++;
                    added = true;
                } else if (this.point2 < maxStat) {
                    this.point2++;
                    added = true;
                } else if (this.point3 < maxStat) {
                    this.point3++;
                    added = true;
                } else if (this.point4 < maxStat) {
                    this.point4++;
                    added = true;
                } else if (this.point5 < maxStat) {
                    this.point5++;
                    added = true;
                }
            }

            if (added) {
                totalAdded++;
            } else {
                // Toàn bộ 5 chỉ số đã đạt maxStat, dừng vòng lặp sớm
                break;
            }
        }

        // Chỉ trừ đi số điểm thực tế đã cộng được (thay vì cố định pointsToAdd)
        this.pointAttribute -= totalAdded;
        if (totalAdded > 0) {
            this.invalidateStatCache();
        }
    }

    private template.Skill_info pickRandomSkill() {
        if (this.skill_point.isEmpty())
            return null;
        return this.skill_point.get(Util.random(0, this.skill_point.size()));
    }

    private void attackMob(Mob mob) {
        if (mob == null || mob.isdie || this.skill_point.isEmpty())
            return;

        long now = System.currentTimeMillis();
        int attackDelay = (this.status == 4) ? 100 : 300; // 200ms (dùng 190 để trừ hao AI lag)

        // Drain 1 action từ queue nếu đến lượt
        if (!attackQueue.isEmpty() || now < nextAttackTime) {
            if (now >= nextAttackTime && !attackQueue.isEmpty()) {
                Runnable action = attackQueue.poll();
                if (action != null)
                    action.run();

                if (attackQueue.isEmpty()) {
                    nextAttackTime = now + attackDelay;
                } else {
                    nextAttackTime = now + attackDelay;
                }
            }
            return;
        }

        // === Nạp combo với sự đa dạng chiêu thức ===
        java.util.List<template.Skill_info> pool = new java.util.ArrayList<>(this.skill_point);
        java.util.Collections.shuffle(pool); // Xáo trộn để combo đa dạng

        int comboSize = 3 + Util.random(0, 3);
        for (int i = 0; i < comboSize; i++) {
            final Mob target = mob;
            final template.Skill_info sk = pool.get(i % pool.size());
            if (sk == null)
                continue;

            attackQueue.add(() -> {
                if (target.isdie || target.map == null || this.map == null) {
                    attackQueue.clear();
                    return;
                }
                try {
                    // Không cần bơm MP tay nữa vì đã có update_mp chặn trừ mana
                    java.util.List<Integer> targets = new java.util.ArrayList<>();
                    targets.add((int) target.index);
                    this.map.use_skill(this, (short) sk.temp.ID, (byte) 1, (byte) 1, targets);
                } catch (Exception e) {
                    GameLogger.error("Disciple.combo: attack error", e);
                    attackQueue.clear();
                    stopFighting();
                }
            });
        }

        // Kích hoạt chiêu đầu tiên ngay lập tức
        if (!attackQueue.isEmpty()) {
            Runnable first = attackQueue.poll();
            if (first != null)
                first.run();

            nextAttackTime = now + attackDelay;
        }
    }

    private void attackPlayer(Player pl) {
        if (pl != null && !pl.isdie && this.skill_point.size() > 0) {
            try {
                template.Skill_info sk = this.skill_point.get(Util.random(0, this.skill_point.size()));
                if (sk == null || sk.temp == null)
                    return;

                java.util.List<Integer> listTargets = new java.util.ArrayList<>();

                // Đã ép kiểu (int) pl.id để tránh lỗi List<Integer> mismatch
                listTargets.add((int) pl.id);

                this.map.use_skill(this, (short) sk.temp.ID, (byte) 0, (byte) 1, listTargets);
                this.lastUpdateAI = System.currentTimeMillis() + 500;
            } catch (Exception e) {
                GameLogger.error("Disciple.attackPlayer: AI error during player attack for disciple of " + master.name,
                        e);
            }
        }
    }

    @Override
    public void update_info_to_all() throws IOException {
        if (this.map == null)
            return;

        // Sử dụng vòng lặp duyệt trực tiếp (Tương thích cực tốt với
        // CopyOnWriteArrayList)
        for (Player p0 : this.map.players) {
            if (p0 == null || p0 == this || p0.conn == null)
                continue;

            // Tính khoảng cách Manhattan từ người chơi đó đến con đệ tử
            int dist = Math.abs(p0.x - this.x) + Math.abs(p0.y - this.y);

            // CHỈ GỬI GÓI TIN NẶNG NẾU ĐỨNG TRONG BÁN KÍNH 800 PIXEL
            if (dist <= 800) {
                this.map.send_char_in4_inmap(p0, this.index_map);
                Service.getThanhTich(this, p0);
                Service.update_PK(this, p0, false);
                Service.pet(this, p0, false);
            }
        }
    }

    @Override
    public void update_mp(long mp_add) {
        if (mp_add < 0) {
            // "Hack" Mana: Không cho phép trừ mana của đệ tử
            return;
        }
        // Chỉ cập nhật chỉ số khi hồi máu/tăng mana, TUYỆT ĐỐI KHÔNG gửi Message
        // (packet)
        this.mp += mp_add;
        if (this.mp < 0)
            this.mp = 0;
        long maxMp = this.body.get_mp_max(true);
        if (this.mp > maxMp)
            this.mp = maxMp;
    }

    @Override
    public void update_exp(long exp_add, boolean show_notice) {
        // Đệ tử đánh quái cũng được cộng EXP, nhưng không gửi packet thông báo
        if (exp_add > 0) {
            this.exp += exp_add;
            // Check level up (Sử dụng hệ thống Level chuẩn của server)
            while (this.level < 100 && this.exp >= template.Level.ENTRYS[this.level - 1].exp) {
                this.exp -= template.Level.ENTRYS[this.level - 1].exp;
                this.level++;
                this.pointAttribute += this.getPotentialPerLevel(); // Cộng điểm theo logic trùng sinh giống sư phụ
                this.invalidateStatCache();
                // Refill on level up - Đã được xử lý trong invalidateStatCache nhưng cần gán
                // cứng max
                this.hp = this.body.get_hp_max(true);
                this.mp = this.body.get_mp_max(true);
            }
        }
    }

    @Override
    public synchronized void update_skill_exp(int index, long exp) throws IOException {
        // Ghi đè để xử lý lên cấp kỹ năng cho đệ tử âm thầm
        if (exp > 0) {
            template.Skill_info sk = this.get_skill_temp((short) index);
            if (sk != null) {
                sk.exp += exp;
                // [LÚA VỀ] Kiểm tra điều kiện lên cấp kỹ năng
                long exp_total = template.Skill_info.EXP[sk.temp.Lv_RQ - 1];
                if (sk.exp >= exp_total) {
                    if (template.Skill_Template.upgrade_skill(sk, (byte) this.clazz)) {
                        sk.exp -= exp_total;
                        // Giới hạn nếu tràn quá nhiều để tránh bug nhảy level ảo
                        if (sk.exp >= exp_total) {
                            sk.exp = 1;
                        }
                        // [NOTIFY] Thông báo cho Sư phụ để họ 'wow' một cái
                        if (master != null && master.conn != null) {
                            Service.send_box_ThongBao_OK(master,
                                    "Đệ tử [" + this.name + "] đã nâng cấp kỹ năng [" + sk.temp.name + "] lên cấp "
                                            + sk.temp.Lv_RQ + "!");
                        }
                    } else {
                        // Nếu max level thì giữ lại lượng EXP tối đa
                        sk.exp = exp_total;
                    }
                }
            }
        }
    }

    @Override
    public void update_hp(long hp_add) {
        // Tương tự, chặn packet khi đệ tử hồi máu / mất máu
        this.hp += hp_add;
        if (this.hp < 0)
            this.hp = 0;
        long maxHp = this.body.get_hp_max(true);
        if (this.hp > maxHp)
            this.hp = maxHp;
    }

    @Override
    public void update_info_all_to_me() throws IOException {
        // Disciple không nhận data về mình (AI), bỏ qua hoàn toàn để tránh crash do
        // dummy session
    }

    @Override
    public void send_skill() throws IOException {
        // Disciple không gửi skill packet cho chính mình (AI)
        // Chỉ cần refresh list_can_combo
        list_can_combo.clear();
    }

    @Override
    public void send_skill_lv_up(template.Skill_info sk_info) throws IOException {
        // Disciple không gửi packet nâng cấp kỹ năng cho chính mình (AI)
    }

    @Override
    public void update_die() {
        this.isdie = true;
        this.deadTime = System.currentTimeMillis();
        this.hp = 0;
        this.targetMob = null;
        this.targetPlayer = null;
        this.stopFighting();
    }

    @Override
    public void send_msg(io.Message m) {
        // Đệ tử không có màn hình thật. Bất kỳ packet nào định gửi cho nó sẽ bị hủy tại
        // đây!
        // Không gọi super.send_msg(m), triệt tiêu hoàn toàn rủi ro crash mạng.
    }

    @Override
    public void add_msg_transaction(io.Message m) throws java.io.IOException {
        // Tương tự, chặn đứng mọi packet giao dịch/hiệu ứng gửi ngầm.
    }

    @Override
    public void invalidateStatCache() {
        super.invalidateStatCache();
        if (this.body != null) {
            // Force recalculate all aggregated stats
            this.body.get_hp_max(true);
            this.body.get_mp_max(true);
            // Cap current HP/MP to new maximums
            if (this.hp > this.body.get_hp_max(false))
                this.hp = this.body.get_hp_max(false);
            if (this.mp > this.body.get_mp_max(false))
                this.mp = this.body.get_mp_max(false);
        }
        // Sync to Master
        syncAttributeToMaster();
    }

    private void syncAttributeToMaster() {
        if (this.master != null && this.master.conn != null) {
            try {
                // TUYỆT ĐỐI KHÔNG gọi Service.Main_char_Info(this, master) vì sẽ làm hoán đổi
                // identity sư phụ
                // Đồng bộ ngoại hình đệ tử lên màn hình sư phụ
                Service.charWearing(this, this.master, false);
                // Cập nhật thông tin cho tất cả người chơi trong map (bao gồm cả master)
                this.update_info_to_all();
            } catch (IOException e) {
                GameLogger.error("Disciple.syncAttributeToMaster fail for " + master.name, e);
            }
        }
    }

    /**
     * Tự động sửa lỗi chỉ số âm do tràn số (overflow) kiểu dữ liệu short.
     */
    private void fixNegativeStats() {
        int maxSafe = 32000;
        boolean fixed = false;
        if (this.point1 < 0) {
            this.point1 = (short) maxSafe;
            fixed = true;
        }
        if (this.point2 < 0) {
            this.point2 = (short) maxSafe;
            fixed = true;
        }
        if (this.point3 < 0) {
            this.point3 = (short) maxSafe;
            fixed = true;
        }
        if (this.point4 < 0) {
            this.point4 = (short) maxSafe;
            fixed = true;
        }
        if (this.point5 < 0) {
            this.point5 = (short) maxSafe;
            fixed = true;
        }
        if (fixed) {
            this.invalidateStatCache();
            GameLogger.warn("[Overflow Fix] Disciple of " + (master != null ? master.name : "unknown")
                    + " had negative stats and was recovered to " + maxSafe);
        }
    }

    /**
     * Tính điểm tiềm năng nhận được mỗi cấp dựa trên cấp Trùng sinh của Đệ tử.
     * Bắt chước logic của Sư phụ trong Player.java.
     */
    @Override
    public int getPotentialPerLevel() {
        int points = 5 + (this.reincarnation * 5);
        return Math.min(points, template.Skill_info.POTENTIAL_PER_LEVEL);
    }

    public String get_detailed_info() {
        StringBuilder sb = new StringBuilder();
        int lvPercent = this.get_level_percent();
        String lvPercentStr = (lvPercent / 10) + "." + (lvPercent % 10) + "%";
        long maxExp = (this.level <= 100) ? template.Level.ENTRYS[this.level - 1].exp
                : template.Level.LEVEL_THONGTHAO[this.thongthao];

        sb.append("--- Đệ tử: ").append(this.name).append(" ---\n");
        sb.append("Level: ").append(this.level).append(" [").append(lvPercentStr).append("]\n");
        sb.append("Trùng sinh: ").append(this.reincarnation).append("\n");
        sb.append("Tiềm năng: ").append(Util.number_format(this.pointAttribute)).append("\n");
        sb.append("HP: ").append(Util.number_format(this.hp)).append(" / ")
                .append(Util.number_format(this.body.get_hp_max(true))).append("\n");
        sb.append("MP: ").append(Util.number_format(this.mp)).append(" / ")
                .append(Util.number_format(this.body.get_mp_max(true))).append("\n");
        sb.append("Sát thương: ").append(Util.number_format(this.body.get_dame(true))).append("\n");
        sb.append("Phòng thủ: ").append(Util.number_format(this.body.get_def(true))).append("\n");

        sb.append("--- Chỉ số chi tiết ---\n");
        sb.append("Chí mạng: ").append(this.body.get_crit(true) / 10).append("%\n");
        sb.append("Sát thương chí mạng: ").append(this.body.get_multi_dame_when_crit(true) / 10).append("%\n");
        sb.append("Xuyên giáp: ").append(this.body.get_pierce(true) / 10).append("%\n");
        sb.append("Né đòn: ").append(this.body.get_miss(true) / 10).append("%\n");
        sb.append("Phản đòn: ").append(this.body.get_dame_react(true) / 10).append("%\n");
        sb.append("Hút HP: ").append(this.body.get_hp_atk_absorb(true) / 10).append("%\n");
        sb.append("Hút MP: ").append(this.body.get_mp_atk_absorb(true) / 10).append("%\n");
        sb.append("Kháng vật lý: ").append(this.body.get_dame_resist(true) / 10).append("%\n");
        sb.append("Kháng phép: ").append(this.body.get_dame_resist_ap(true) / 10).append("%\n");

        if (this.skill_point.size() > 0) {
            sb.append("--- K? nang ---\n");
            for (template.Skill_info sk : this.skill_point) {
                if (sk.temp != null) {
                    int skPercent = sk.get_percent();
                    String skPercentStr = (skPercent / 10) + "." + (skPercent % 10) + "%";
                    sb.append("- ").append(sk.temp.name).append(" (C?p ").append(sk.realLevel).append("): ")
                            .append(skPercentStr).append("\n");
                }
            }
        }
        return sb.toString();
    }
}