package core;

import activities.Friend;
import client.Clan;
import client.Item;
import client.Player;
import database.SQL;
import io.Message;
import map.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.*;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BXH {
    public static List<InfoMemList> CAOTHU = new ArrayList<>();
    public static List<InfoMemList> PVP = new ArrayList<>();
    public static List<InfoMemList> WANTED = new ArrayList<>();
    public static List<InfoMemList> HANG = new ArrayList<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void send(Player p, int type, int page) throws IOException {
        if (page < 0) {
            page = 0;
        }
        Message m = new Message(-30);
        switch (type) {
            case 8: {
                String title = "TOP Săn Boss";
                List<InfoMemList> listShow = getTopDameFromSQL(1);
                int bound1 = 0;
                int bound2 = listShow.size();
                m.writer().writeByte(8);
                m.writer().writeUTF(title);
                m.writer().writeByte(0);
                m.writer().writeByte(bound2 - bound1);
                for (int i = bound1; i < bound2; i++) {
                    InfoMemList temp = listShow.get(i);
                    InfoMemList.WriteInfoMemList(m.writer(), temp);
                }
                break;
            }

            case 10: {
                writeBXHToMessage(m, 10, "Top Hang Động", BXH.HANG, page);
                break;
            }

            case 7: {
                writeBXHToMessage(m, 7, "Top PVP", BXH.PVP, page);
                break;
            }
            case 4: {
                writeBXHToMessage(m, 4, "Cao Thủ", BXH.CAOTHU, page);
                break;
            }
            case 6: {
                int bound1 = 0;
                int bound2 = Clan.BXH.size();
                if (Clan.BXH.size() > 10) {
                    if (((page + 1) * 10) > Clan.BXH.size()) {
                        bound1 = 10 * page;
                        bound2 = Clan.BXH.size();
                        while (bound1 >= bound2) {
                            bound1 -= 10;
                            page--;
                        }
                    } else {
                        bound1 = 10 * page;
                        bound2 = bound1 + 10;
                    }
                } else {
                    page = 0;
                }
                m.writer().writeByte(6);
                m.writer().writeUTF("Băng Hải Tặc");
                m.writer().writeByte(page);
                m.writer().writeByte(bound2 - bound1);
                for (int i = bound1; i < bound2; i++) {
                    String clan_name = Clan.BXH.get(i);
                    Clan clan = Clan.get_clan_by_name(clan_name);
                    m.writer().writeShort(clan.id);
                    m.writer().writeUTF(clan.name);
                    String info = "TS: %s - Lv: %s + %s";
                    float percent = (clan.xp * 100f) / Clan.get_xp_max(clan.level, clan.trungsinh);
                    if (percent > 100f) {
                        percent = 100f;
                    }
                    m.writer().writeUTF(String.format(info, clan.trungsinh, clan.level,
                            String.format("%.2f", percent)) + "%");
                    m.writer().writeShort(clan.icon); // clan icon
                    m.writer().writeShort(i);
                }
                break;
            }
            case 9: {
                int bound1 = 0;
                int bound2 = WANTED.size() > 10 ? 10 : WANTED.size();
                page = 0;
                m.writer().writeByte(9);
                m.writer().writeUTF("Truy nã");
                m.writer().writeByte(page);
                m.writer().writeByte(bound2 - bound1);
                for (int i = bound1; i < bound2; i++) {
                    InfoMemList temp = BXH.WANTED.get(i);
                    m.writer().writeInt(temp.id);
                    m.writer().writeUTF(temp.name);
                    m.writer().writeShort(temp.head);
                    m.writer().writeShort(temp.hair);
                    m.writer().writeShort(temp.hat);
                    m.writer().writeShort(temp.part[0]); // body
                    m.writer().writeShort(temp.part[1]); // leg
                    m.writer().writeShort(temp.part[2]); // weapon
                    m.writer().writeInt(i); // rank
                    m.writer().writeInt((int) temp.thongthao); // wanted point
                }
                break;
            }
        }
        p.addmsg(m);
        m.cleanup();
    }

    private static void writeBXHToMessage(Message m, int type, String title, List<InfoMemList> list, int page)
            throws IOException {
        int bound1 = 0;
        int bound2 = list.size();
        if (type == 4 || type == 7) {
            if (list.size() > 10) {
                if (((page + 1) * 10) > list.size()) {
                    bound1 = 10 * page;
                    bound2 = list.size();
                    while (bound1 >= bound2) {
                        bound1 -= 10;
                        page--;
                    }
                } else {
                    bound1 = 10 * page;
                    bound2 = bound1 + 10;
                }
            } else {
                page = 0;
            }
        }
        m.writer().writeByte(type);
        m.writer().writeUTF(title);
        m.writer().writeByte(page);
        m.writer().writeByte(bound2 - bound1);

        for (int i = bound1; i < bound2; i++) {
            InfoMemList temp = list.get(i);
            InfoMemList.WriteInfoMemList(m.writer(), temp);
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        byte idlist = m2.reader().readByte();
        byte page = m2.reader().readByte();
        switch (type) {
            case 2: {
                if (idlist == 2 && page == 0) { // dsach den
                    Message m = new Message(-30);
                    m.writer().writeByte(2);
                    m.writer().writeUTF("Kẻ Thù");
                    m.writer().writeByte(0);
                    m.writer().writeByte(p.enemy_list.size());
                    for (int i = 0; i < p.enemy_list.size(); i++) {
                        Friend.ReadInfoMemList(m.writer(), p.enemy_list.get(i));
                    }
                    p.addmsg(m);
                    m.cleanup();
                }
                break;
            }
            case 3: {
                BXH.send(p, idlist, page);
                break;
            }
        }
    }

    public static void update() {
        final Runnable updateCaoThuTask = () -> {
            try {
                updateCaoThu();
            } catch (Exception e) {
                GameLogger.error("BXH.update: updateCaoThuTask error", e);
            }
        };
        final Runnable updatePVPTask = () -> {
            try {
                updatePVP();
            } catch (Exception e) {
                GameLogger.error("BXH.update: updatePVPTask error", e);
            }
        };
        final Runnable updateWantedTask = () -> {
            try {
                updateWanted();
            } catch (Exception e) {
                GameLogger.error("BXH.update: updateWantedTask error", e);
            }
        };
        final Runnable updateTOPHangTask = () -> {
            try {
                updateTOPHang();
            } catch (Exception e) {
                GameLogger.error("BXH.update: updateTOPHangTask error", e);
            }
        };
        scheduler.schedule(updateCaoThuTask, 0, TimeUnit.SECONDS);
        scheduler.schedule(updatePVPTask, 5, TimeUnit.SECONDS);
        scheduler.schedule(updateWantedTask, 10, TimeUnit.SECONDS);
        scheduler.schedule(updateTOPHangTask, 15, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            GameLogger.warn("BXH scheduler shutdown interrupted", e);
            scheduler.shutdownNow();
        }
    }

    public static List<InfoMemList> getTopDameFromSQL(int bossId) {
        List<InfoMemList> result = new ArrayList<>();
        try (Connection conn = SQL.gI().getCon()) {
            PreparedStatement ps = conn.prepareStatement("SELECT `data` FROM `damebosssave` WHERE `id` = ?");
            ps.setInt(1, bossId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dataStr = rs.getString("data");
                if (dataStr != null && !dataStr.isEmpty()) {
                    JSONArray jsonArr = (JSONArray) new JSONParser().parse(dataStr);
                    List<Top_Dame> topList = new ArrayList<>();
                    for (Object obj : jsonArr) {
                        JSONArray arr = (JSONArray) obj;
                        String name = (String) arr.get(0);
                        long dame = (long) arr.get(1);
                        Top_Dame top = new Top_Dame();
                        top.name = name;
                        top.dame = dame;
                        topList.add(top);
                    }
                    topList.sort((a, b) -> Long.compare(b.dame, a.dame));
                    for (int i = 0; i < Math.min(50, topList.size()); i++) {
                        Top_Dame top = topList.get(i);
                        InfoMemList infoAdd = new InfoMemList();
                        infoAdd.id = i;
                        infoAdd.name = top.name;
                        infoAdd.thongthao = top.dame;
                        infoAdd.info = "Boss : " + Util.number_format(top.dame);
                        infoAdd.rank = (short) i;
                        try (PreparedStatement ps2 = conn.prepareStatement(
                                "SELECT `body`, `it_body`, `fashion`, `site`, `tocSuper` FROM `players` WHERE name = ?")) {
                            ps2.setString(1, top.name);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    setCharacterAppearance(infoAdd, rs2);
                                } else {
                                    infoAdd.head = -1;
                                    infoAdd.hair = -1;
                                    infoAdd.hat = -1;
                                    infoAdd.part = new short[] { -1, -1, -1 };
                                }
                            }
                        } catch (Exception e) {
                            GameLogger.error("BXH.getTopDameFromSQL: error setCharacterAppearance for " + top.name, e);
                        }

                        result.add(infoAdd);
                    }
                }
            }
            ps.close();
        } catch (Exception e) {
            GameLogger.error("BXH.getTopDameFromSQL: SQL error", e);
        }
        return result;
    }

    // Hàm truy vấn SQL chung cho các hàm update
    private static List<InfoMemList> queryTopPlayers(String column, String orderBy, int limit, String extraInfo) {
        List<InfoMemList> listAdd = new ArrayList<>();
        String sql = String.format(
                "SELECT `id`, `name`, `clazz`, `%s`, `body`, `it_body`, `fashion`, `site`, `tocSuper` FROM `players` ORDER BY `%s` DESC LIMIT %d",
                column, orderBy, limit);

        try (Connection connection = SQL.gI().getCon()) {
            if (connection == null) {
                return listAdd;
            }
            try (PreparedStatement ps = connection.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    InfoMemList info = new InfoMemList();
                    info.id = rs.getInt("id");
                    info.name = rs.getString("name");
                    info.thongthao = rs.getInt(column);
                    setCharacterAppearance(info, rs);
                    info.info = String.format(extraInfo, info.thongthao);
                    listAdd.add(info);
                }
            }
        } catch (Exception e) {
            GameLogger.error("BXH.queryTopPlayers: SQL error", e);
            listAdd.clear();
        }
        if (!listAdd.isEmpty()) {
            for (int i = 0; i < listAdd.size(); i++) {
                listAdd.get(i).rank = (short) i;
            }
        }

        return listAdd;
    }

    // Xử lý dữ liệu hiển thị nhân vật từ JSON
    private static void setCharacterAppearance(InfoMemList info, ResultSet rs) throws SQLException {
        try {
            String fashionStr = rs.getString("fashion");
            String bodyStr = rs.getString("body");
            String itemStr = rs.getString("it_body");
            String siteStr = rs.getString("site");
            short defaultHead = 1;
            short defaultHair = 1;
            if (bodyStr != null && !bodyStr.isEmpty()) {
                JSONArray bodyJson = (JSONArray) JSONValue.parse(bodyStr);
                defaultHead = Short.parseShort(bodyJson.get(0).toString());
                defaultHair = Short.parseShort(bodyJson.get(1).toString());
            }
            List<ItemFashionP2> fashion = (fashionStr != null && !fashionStr.isEmpty()) ? parseFashionP2(fashionStr)
                    : new ArrayList<>();
            List<ItemFashionP> itemFashionP = (fashionStr != null && !fashionStr.isEmpty()) ? parseFashionP(fashionStr)
                    : new ArrayList<>();
            short[] wearingFashion = getFashionWearing(fashion);
            Item_wear[] items = (itemStr != null && !itemStr.isEmpty()) ? parseItemWear(itemStr) : new Item_wear[8];
            if (wearingFashion != null && wearingFashion.length > 7 && wearingFashion[7] == -2) {
                info.part = new short[3];
                info.part[0] = wearingFashion.length > 3 ? wearingFashion[3] : -1; // body
                info.part[1] = wearingFashion.length > 5 ? wearingFashion[5] : -1; // leg
                info.part[2] = wearingFashion.length > 0 ? wearingFashion[0] : -1; // weapon
                info.head = wearingFashion.length > 6 ? wearingFashion[6] : defaultHead;
                info.hair = -2;
                info.hat = (wearingFashion.length > 1 && wearingFashion[1] != -1) ? wearingFashion[1]
                        : -1;
                return;
            }
            short headFromFashion = -1;
            short hairFromFashion = -1;
            if (wearingFashion != null && wearingFashion.length > 6 && wearingFashion[6] != -1) {
                headFromFashion = wearingFashion[6];
                hairFromFashion = -2;
            } else {
                for (ItemFashionP it : itemFashionP) {
                    if (it.category == 103 && it.is_use)
                        hairFromFashion = it.icon;
                    if (it.category == 108 && it.is_use)
                        headFromFashion = it.icon;
                }
            }
            if (hairFromFashion == 772) {
                hairFromFashion += rs.getInt("tocSuper");
            }
            info.head = (headFromFashion != -1) ? headFromFashion : defaultHead;
            if (hairFromFashion == -2) {
                info.hair = -2;
            } else if (hairFromFashion != -1) {
                info.hair = hairFromFashion;
            } else {
                info.hair = defaultHair;
            }
            info.hat = -1;
            if (siteStr != null && !siteStr.isEmpty() && items != null && items.length > 1 && items[1] != null) {
                JSONArray site = (JSONArray) JSONValue.parse(siteStr);
                boolean isShowHat = Byte.parseByte(site.get(6).toString()) == 1;
                if (isShowHat) {
                    if (wearingFashion != null && wearingFashion.length > 1 && wearingFashion[1] != -1) {
                        info.hat = wearingFashion[1];
                    } else {
                        info.hat = ItemTemplate3.get_it_by_id(items[1].template.id).part;
                    }
                }
            }
            info.part = new short[3];
            info.part[0] = (wearingFashion != null && wearingFashion.length > 3 && wearingFashion[3] != -1)
                    ? wearingFashion[3]
                    : (items[3] != null ? items[3].template.part : -1); // body
            info.part[1] = (wearingFashion != null && wearingFashion.length > 5 && wearingFashion[5] != -1)
                    ? wearingFashion[5]
                    : (items[5] != null ? items[5].template.part : -1); // leg
            info.part[2] = (wearingFashion != null && wearingFashion.length > 0 && wearingFashion[0] != -1)
                    ? wearingFashion[0]
                    : (items[0] != null ? items[0].template.part : -1); // weapon

        } catch (Exception e) {
            GameLogger.warn("[BXH] Lỗi khi set ngoại hình nhân vật: " + e.getMessage());
            info.head = 1;
            info.hair = 1;
            info.hat = -1;
            info.part = new short[] { -1, -1, -1 };
        }
    }

    private static void updateWanted() {
        List<InfoMemList> listWanted = queryTopPlayers("wanted_point", "wanted_point", 50, "Điểm: %d");
        if (!listWanted.isEmpty()) {
            BXH.WANTED.clear();
            BXH.WANTED.addAll(listWanted);
        }
    }

    private static void updatePVP() {
        List<InfoMemList> listPVP = queryTopPlayers("pvppoint", "pvppoint", 50, "Điểm: %,d");
        if (!listPVP.isEmpty()) {
            BXH.PVP.clear();
            BXH.PVP.addAll(listPVP);
        }
    }

    private static void updateTOPHang() {
        List<InfoMemList> listHang = queryTopPlayers("beri0", "beri0", 50, "Hang: %s");
        if (!listHang.isEmpty()) {
            BXH.HANG.clear();
            BXH.HANG.addAll(listHang);
        }
    }

    private static void updateCaoThu() {
        List<InfoMemList> listCaoThu = new ArrayList<>();
        try (Connection connection = SQL.gI().getCon();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT `id`, `name`, `clazz`, `level`, `body`, `it_body`, `fashion`, `site`, `tocSuper` FROM `players` ORDER BY `exp` DESC LIMIT 50;");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InfoMemList temp = new InfoMemList();
                temp.id = rs.getInt("id");
                temp.name = rs.getString("name");
                JSONArray levelData = (JSONArray) JSONValue.parse(rs.getString("level"));
                temp.level = Short.parseShort(levelData.get(0).toString());
                temp.thongthao = Short.parseShort(levelData.get(2).toString());
                int reincarnation = 0;
                if (levelData.size() > 3) {
                    reincarnation = Integer.parseInt(levelData.get(3).toString());
                }
                setCharacterAppearance(temp, rs);
                String romanNumeral = reincarnation > 0 ? " [" + Util.toRoman(reincarnation) + "]" : "";
                temp.info = String.format("Cấp %s - TT: %s%s", temp.level, temp.thongthao, romanNumeral);
                listCaoThu.add(temp);
            }
            if (!listCaoThu.isEmpty()) {
                for (int i = 0; i < listCaoThu.size(); i++) {
                    listCaoThu.get(i).rank = (short) i;
                }
                BXH.CAOTHU.clear();
                BXH.CAOTHU.addAll(listCaoThu);
            }
        } catch (Exception e) {
            GameLogger.error("BXH.updateCaoThu: error", e);
        }
    }

    public static int get_Thanh_tich_level(Player p) {
        return getPlayerRankInTop(p.name, BXH.CAOTHU, 9);
    }

    public static int get_Thanh_tich_pvp(Player p) {
        return getPlayerRankInTop(p.name, BXH.PVP, 9);
    }

    public static int get_rank_hang_dong(Player p) {
        return getPlayerRankInTop(p.name, BXH.HANG, 9);
    }

    // Hàm chung để lấy thành tích của người chơi
    private static int getPlayerRankInTop(String playerName, List<InfoMemList> list, int maxRank) {
        try {
            for (int i = 0; i < list.size() && i <= maxRank; i++) {
                if (list.get(i).name.equals(playerName)) {
                    if (i <= 2) {
                        return i; // Top 3 được trả về thứ hạng chính xác
                    } else {
                        return 3; // Ngoài top 3 đều trả về 3
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.warn("[BXH] Lỗi lọc thứ hạng người chơi '" + playerName + "': " + e.getMessage());
        }
        return -1;
    }

    public static int get_rank_wanted(String name) {
        try {
            for (int i = 0; i < BXH.WANTED.size(); i++) {
                if (BXH.WANTED.get(i).name.equals(name)) {
                    return i;
                }
            }
        } catch (Exception e) {
            GameLogger.warn("[BXH] Lỗi lọc thứ hạng truy nã '" + name + "': " + e.getMessage());
        }
        return -1;
    }

    // Helper methods for parsing JSON data
    private static List<ItemFashionP2> parseFashionP2(String fashionStr) {
        List<ItemFashionP2> list = new ArrayList<>();
        JSONArray js = (JSONArray) JSONValue.parse(fashionStr);
        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(1).toString());

        for (Object o : js2) {
            JSONArray item = (JSONArray) JSONValue.parse(o.toString());
            ItemFashionP2 temp = new ItemFashionP2();
            temp.id = Short.parseShort(item.get(0).toString());
            temp.is_use = Byte.parseByte(item.get(1).toString()) == 1;
            list.add(temp);
        }
        return list;
    }

    private static List<ItemFashionP> parseFashionP(String fashionStr) {
        List<ItemFashionP> list = new ArrayList<>();
        JSONArray js = (JSONArray) JSONValue.parse(fashionStr);
        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(0).toString());

        for (Object o : js2) {
            JSONArray item = (JSONArray) JSONValue.parse(o.toString());
            ItemFashionP temp = new ItemFashionP();
            temp.category = Byte.parseByte(item.get(0).toString());
            temp.id = Short.parseShort(item.get(1).toString());
            temp.icon = Short.parseShort(item.get(2).toString());
            temp.is_use = Byte.parseByte(item.get(3).toString()) == 1;
            list.add(temp);
        }
        return list;
    }

    private static short[] getFashionWearing(List<ItemFashionP2> fashionList) {
        for (ItemFashionP2 f : fashionList) {
            if (f.is_use) {
                ItemFashion item = ItemFashion.get_item(f.id);
                if (item != null) {
                    return item.mWearing;
                }
            }
        }
        return null;
    }

    private static Item_wear[] parseItemWear(String jsonStr) {
        Item_wear[] items = new Item_wear[8];
        JSONArray js = (JSONArray) JSONValue.parse(jsonStr);
        for (Object o : js) {
            JSONArray arr = (JSONArray) JSONValue.parse(o.toString());
            Item_wear item = new Item_wear();
            Item.readUpdateItem(arr.toString(), item);
            items[item.index] = item;
        }
        return items;
    }

    // Class hỗ trợ cho getTopDameFromSQL
    private static class Top_Dame {
        String name;
        long dame;
    }
}
