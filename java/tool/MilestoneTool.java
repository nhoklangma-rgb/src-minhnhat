package tool;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.*;
import java.util.*;

public class MilestoneTool {

    private static final String NAP_FILE = "data/tich_nap.json";
    private static final String TIEU_FILE = "data/tich_tieu.json";
    private static final String GIFT_FILE = "data/gift_codes.json";
    private static final String ITEM_LIST_FILE = "data/item_list.txt";
    private static final String SQL_FILE = "htth.sql";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            core.GameLogger.info("\n========== MILESTONE TOOL ==========");
            core.GameLogger.info("1. Quản lý mốc TÍCH NẠP");
            core.GameLogger.info("2. Quản lý mốc TÍCH TIÊU");
            core.GameLogger.info("3. Quản lý GIFTCODE");
            core.GameLogger.info("4. Tìm kiếm ID Vật phẩm");
            core.GameLogger.info("5. Cập nhật danh sách vật phẩm từ SQL (Chạy lần đầu)");
            core.GameLogger.info("0. Thoát");
            System.out.print("Chọn: ");
            int rootChoice = safeReadInt(sc);

            if (rootChoice == 0)
                break;
            if (rootChoice == 1)
                manageFile(sc, NAP_FILE, "Tích Nạp");
            else if (rootChoice == 2)
                manageFile(sc, TIEU_FILE, "Tích Tiêu");
            else if (rootChoice == 3)
                manageGiftcodes(sc);
            else if (rootChoice == 4)
                searchItem(sc);
            else if (rootChoice == 5)
                exportItemList();
        }
        core.GameLogger.info("Bye!");
    }

    private static void manageFile(Scanner sc, String filePath, String label) {
        while (true) {
            core.GameLogger.info("\n--- Quản lý " + label + " ---");
            core.GameLogger.info("1. Liệt kê mốc hiện có");
            core.GameLogger.info("2. Thêm mốc mới");
            core.GameLogger.info("0. Quay lại");
            System.out.print("Chọn: ");
            int choice = safeReadInt(sc);

            if (choice == 0)
                break;
            if (choice == 1)
                listMilestones(filePath);
            else if (choice == 2)
                addMilestone(sc, filePath);
        }
    }

    private static void listMilestones(String filePath) {
        JSONArray arr = loadJson(filePath);
        if (arr == null || arr.isEmpty()) {
            core.GameLogger.info("Trống!");
            return;
        }
        core.GameLogger.info("Danh sách mốc:");
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = (JSONObject) arr.get(i);
            core.GameLogger.info("[" + i + "] Mốc: " + obj.get("num") + " | Items: " + obj.get("items"));
        }
    }

    private static void addMilestone(Scanner sc, String filePath) {
        JSONArray arr = loadJson(filePath);
        if (arr == null)
            arr = new JSONArray();

        System.out.print("Nhập giá trị mốc (num): ");
        int num = safeReadInt(sc);

        JSONArray items = new JSONArray();
        while (true) {
            core.GameLogger.info("Thêm vật phẩm thưởng (Nhập id = 0 để dừng):");
            System.out.print("ID: ");
            int id = safeReadInt(sc);
            if (id == 0)
                break;

            System.out.print("Loại (Cat) [3: TB, 4: Potion, 7: Material, 105: Fashion]: ");
            int cat = safeReadInt(sc);

            System.out.print("Số lượng (Quant): ");
            int quant = safeReadInt(sc);

            JSONObject item = new JSONObject();
            item.put("cat", cat);
            item.put("id", id);
            item.put("quant", quant);
            items.add(item);
        }

        JSONObject newMilestone = new JSONObject();
        newMilestone.put("num", num);
        newMilestone.put("items", items);
        arr.add(newMilestone);

        // Sort by num
        arr.sort((a, b) -> {
            int n1 = Integer.parseInt(((JSONObject) a).get("num").toString());
            int n2 = Integer.parseInt(((JSONObject) b).get("num").toString());
            return Integer.compare(n1, n2);
        });

        saveJson(filePath, arr);
        core.GameLogger.info("Đã thêm và lưu mốc mới!");
    }

    private static JSONArray loadJson(String path) {
        File file = new File(path);
        if (!file.exists())
            return new JSONArray();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            return (JSONArray) JSONValue.parse(sb.toString());
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] MilestoneTool.loadJson error: " + e.getMessage());
            return null;
        }
    }

    private static void saveJson(String path, JSONArray arr) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(arr.toJSONString());
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] MilestoneTool.saveJson error: " + e.getMessage());
        }
    }

    private static void manageGiftcodes(Scanner sc) {
        while (true) {
            core.GameLogger.info("\n--- Quản lý GIFTCODE ---");
            core.GameLogger.info("1. Liệt kê Giftcode");
            core.GameLogger.info("2. Thêm Giftcode mới");
            core.GameLogger.info("3. Xóa Giftcode");
            core.GameLogger.info("0. Quay lại");
            System.out.print("Chọn: ");
            int choice = safeReadInt(sc);
            if (choice == 0)
                break;
            if (choice == 1)
                listGiftcodes();
            else if (choice == 2)
                addGiftcode(sc);
            else if (choice == 3)
                deleteGiftcode(sc);
        }
    }

    private static void listGiftcodes() {
        JSONArray arr = loadJson(GIFT_FILE);
        if (arr == null || arr.isEmpty()) {
            core.GameLogger.info("Trống!");
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = (JSONObject) arr.get(i);
            System.out.println("[" + i + "] Code: " + obj.get("name") + " | Beri: " + obj.get("beri") + " | Ruby: "
                    + obj.get("ruby") + " | Items: " + obj.get("items"));
        }
    }

    private static void addGiftcode(Scanner sc) {
        JSONArray arr = loadJson(GIFT_FILE);
        if (arr == null)
            arr = new JSONArray();
        System.out.print("Nhập mã Giftcode: ");
        String name = sc.nextLine();
        System.out.print("Nhập Beri: ");
        int beri = safeReadInt(sc);
        System.out.print("Nhập Ruby: ");
        int ruby = safeReadInt(sc);
        System.out.print("Giới hạn lượt nhập: ");
        int limit = safeReadInt(sc);

        JSONArray items = new JSONArray();
        // Tái sử dụng logic thêm item tương tự mốc nạp
        while (true) {
            core.GameLogger.info("Thêm vật phẩm thưởng (Nhập id = 0 để dừng):");
            System.out.print("ID: ");
            int id = safeReadInt(sc);
            if (id == 0)
                break;
            System.out.print("Loại: ");
            int cat = safeReadInt(sc);
            System.out.print("Số lượng: ");
            int quant = safeReadInt(sc);
            JSONArray item = new JSONArray();
            item.add(cat);
            item.add(id);
            item.add(quant);
            items.add(item);
        }

        JSONObject gift = new JSONObject();
        gift.put("name", name);
        gift.put("beri", beri);
        gift.put("ruby", ruby);
        gift.put("limit", limit);
        gift.put("used", "");
        gift.put("items", items.toJSONString());
        gift.put("luotnhap", 0);
        arr.add(gift);
        saveJson(GIFT_FILE, arr);
        core.GameLogger.info("Đã lưu Giftcode!");
    }

    private static void deleteGiftcode(Scanner sc) {
        JSONArray arr = loadJson(GIFT_FILE);
        listGiftcodes();
        System.out.print("Nhập STT muốn xóa: ");
        int idx = safeReadInt(sc);
        if (idx >= 0 && idx < arr.size()) {
            arr.remove(idx);
            saveJson(GIFT_FILE, arr);
            core.GameLogger.info("Đã xóa!");
        }
    }

    private static void searchItem(Scanner sc) {
        System.out.print("Nhập tên vật phẩm muốn tìm: ");
        String key = sc.nextLine().toLowerCase();
        File f = new File(ITEM_LIST_FILE);
        if (!f.exists()) {
            core.GameLogger.info("Chưa có danh sách tra cứu, hãy chọn mục 5 ở menu chính.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(key))
                    core.GameLogger.info(line);
            }
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] MilestoneTool.searchItem error: " + e.getMessage());
        }
    }

    public static void exportItemList() {
        core.GameLogger.info("Đang quét SQL... Vui lòng đợi...");
        try (BufferedReader reader = new BufferedReader(new FileReader(SQL_FILE));
                BufferedWriter writer = new BufferedWriter(new FileWriter(ITEM_LIST_FILE))) {
            String line;
            String currentCat = "";
            while ((line = reader.readLine()) != null) {
                if (line.contains("INSERT INTO `item3`"))
                    currentCat = "3";
                else if (line.contains("INSERT INTO `item4`"))
                    currentCat = "4";
                else if (line.contains("INSERT INTO `item7`"))
                    currentCat = "7";

                if (!currentCat.isEmpty() && line.trim().startsWith("(")) {
                    // Extract ID and Name: (ID, 'Name', ...
                    int firstComma = line.indexOf(",");
                    if (firstComma > 0) {
                        String idStr = line.substring(1, firstComma).trim();
                        int firstQuote = line.indexOf("'", firstComma);
                        int secondQuote = line.indexOf("'", firstQuote + 1);
                        if (firstQuote > 0 && secondQuote > firstQuote) {
                            String name = line.substring(firstQuote + 1, secondQuote);
                            writer.write("Loại " + currentCat + " | ID: " + idStr + " | Tên: " + name);
                            writer.newLine();
                        }
                    }
                }
                if (line.endsWith(";"))
                    currentCat = "";
            }
            core.GameLogger.info("Xong! Đã tạo " + ITEM_LIST_FILE);
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] MilestoneTool.exportItemList error: " + e.getMessage());
        }
    }

    private static int safeReadInt(Scanner sc) {
        try {
            return Integer.parseInt(sc.nextLine());
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] MilestoneTool.safeReadInt error: " + e.getMessage());
            return -1;
        }
    }
}
