package activities;

import client.Player;
import database.SQL;
import io.Message;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import template.InfoMemList;

public class VoHanLienTangRanking {
    private static final String FILE_PATH = "data/vhlt_ranking.json";
    private static List<RankingEntry> rankingList = new ArrayList<>();

    public static class RankingEntry {
        public String name;
        public int floor;
        public int level;
        public long timestamp;

        public RankingEntry(String name, int floor, int level, long timestamp) {
            this.name = name;
            this.floor = floor;
            this.level = level;
            this.timestamp = timestamp;
        }
    }

    public static void init() {
        load();
    }

    public static synchronized void load() {
        rankingList.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                for (Object item : array) {
                    JSONObject jsonObject = (JSONObject) item;
                    String name = (String) jsonObject.get("name");
                    int floor = ((Long) jsonObject.get("floor")).intValue();
                    int level = ((Long) jsonObject.get("level")).intValue();
                    long timestamp = (Long) jsonObject.get("timestamp");
                    rankingList.add(new RankingEntry(name, floor, level, timestamp));
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("VoHanLienTangRanking.load: Error loading ranking data", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized void save() {
        JSONArray array = new JSONArray();
        for (RankingEntry entry : rankingList) {
            JSONObject obj = new JSONObject();
            obj.put("name", entry.name);
            obj.put("floor", (long) entry.floor);
            obj.put("level", (long) entry.level);
            obj.put("timestamp", entry.timestamp);
            array.add(obj);
        }

        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(array.toJSONString());
        } catch (IOException e) {
            core.GameLogger.error("VoHanLienTangRanking.save: Error saving ranking data", e);
        }
    }

    public static synchronized void update(Player p, int floor) {
        if (floor <= 0) {
            return;
        }

        boolean found = false;
        for (RankingEntry entry : rankingList) {
            if (entry.name.equals(p.name)) {
                if (floor > entry.floor) {
                    entry.floor = floor;
                    entry.level = p.level;
                    entry.timestamp = System.currentTimeMillis();
                }
                found = true;
                break;
            }
        }

        if (!found) {
            rankingList.add(new RankingEntry(p.name, floor, p.level, System.currentTimeMillis()));
        }

        // Sắp xếp: Tầng cao nhất lên đầu, nếu tầng bằng nhau thì Level thấp hơn lên
        // đầu,
        // tiếp theo là ai đạt sớm hơn lên đầu.
        Collections.sort(rankingList, (a, b) -> {
            if (b.floor != a.floor) {
                return Integer.compare(b.floor, a.floor);
            }
            if (a.level != b.level) {
                return Integer.compare(a.level, b.level);
            }
            return Long.compare(a.timestamp, b.timestamp);
        });

        // Chỉ giữ Top 50
        if (rankingList.size() > 50) {
            rankingList.remove(rankingList.size() - 1);
        }
    }

    public static void show(Player p) throws IOException {
        Message m = new Message(-30);
        m.writer().writeByte(167); // Type nhận dạng cho Client
        m.writer().writeUTF("Bảng Xếp Hạng Vô Hạn Liên Tầng");
        m.writer().writeByte(0); // Page (mặc định trang 0)

        List<InfoMemList> showList = new ArrayList<>();
        try (Connection connection = SQL.gI().getCon()) {
            for (int i = 0; i < rankingList.size(); i++) {
                RankingEntry entry = rankingList.get(i);
                InfoMemList info = new InfoMemList();
                info.id = i;
                info.name = entry.name;
                info.level = (short) entry.level;
                info.info = "Đạt tới tầng : " + entry.floor;
                info.rank = (short) i;

                // Lấy nhân hình từ DB cho đồng bộ (do JSON chỉ lưu tên/tầng)
                try (PreparedStatement ps = connection
                        .prepareStatement("SELECT `body` FROM `players` WHERE `name` = ?")) {
                    ps.setString(1, entry.name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String bodyStr = rs.getString("body");
                            if (bodyStr != null && !bodyStr.isEmpty()) {
                                JSONArray bodyJson = (JSONArray) JSONValue.parse(bodyStr);
                                info.head = Short.parseShort(bodyJson.get(0).toString());
                                info.hair = Short.parseShort(bodyJson.get(1).toString());
                            }
                        } else {
                            info.head = -1;
                            info.hair = -1;
                        }
                        info.hat = -1; // Mặc định không nón hoặc lấy từ DB nếu cần
                    }
                } catch (Exception e) {
                    core.GameLogger.error("VoHanLienTangRanking.show: Error loading player body data", e);
                }

                showList.add(info);
            }
        } catch (Exception e) {
            core.GameLogger.error("VoHanLienTangRanking.show: Error loading ranking data", e);
        }

        m.writer().writeByte(showList.size());
        for (int i = 0; i < showList.size(); i++) {
            InfoMemList.WriteInfoMemList(m.writer(), showList.get(i));
        }

        p.addmsg(m);
        m.cleanup();
    }
}
