package taixiu;

import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.*;
import client.Player;
import core.Manager;
import core.PendingRewardManager;
import core.Service;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * BauCuaManager.java
 * ─────────────────────────────────────────────────────────────────
 * Hệ thống Bầu Cua Multiplayer — Đặt cược bằng Extol
 * ─────────────────────────────────────────────────────────────────
 */
public class BauCuaManager {

    // ══════════════════════════════════════════════════════════════
    // CONSTANTS
    // ══════════════════════════════════════════════════════════════

    public static final int YESNO_MENU = 300;
    public static final int YESNO_XAC_NHAN = 301;
    public static final int INPUT_SO_TIEN = 311;
    public static final int YESNO_HUONG_DAN = 9934;
    public static final int YESNO_HISTORY = 302;
    public static final int YESNO_BXH = 303;

    private static final int THOI_GIAN_DAT = 30;
    private static final int THOI_GIAN_NGHI = 10;
    private static final int MAX_O_DAT = 3;

    public enum O {
        BAU(0, "Bầu"),
        CUA(1, "Cua"),
        CA(2, "Cá"),
        TOM(3, "Tôm"),
        NAI(4, "Nai"),
        GA(5, "Gà");

        public final int id;
        public final String name;

        O(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public static O getById(int id) {
            for (O o : values()) {
                if (o.id == id)
                    return o;
            }
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════════════════

    public static class CuocDat {
        public Player player;
        // Sử dụng ConcurrentHashMap để chống crash khi spam cược nhiều ô cùng lúc
        public Map<O, Long> danhSachO = new ConcurrentHashMap<>();
        public boolean daTraThuong = false;

        public long tongCuoc() {
            return danhSachO.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    public static class Phong {
        public int id;
        public List<CuocDat> danhSachCuoc = new CopyOnWriteArrayList<>();
        public boolean dangDat = true;
        public long thoiGianBatDau;
        public ScheduledFuture<?> timerKetThuc;

        public O[] ketQua = new O[3];
    }

    // ══════════════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════════════

    private static Phong phongHienTai = null;
    private static volatile Phong phongVuaKetThuc = null;
    private static volatile boolean dangNghi = false;
    private static long thoiGianBatDauNghi = 0;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Luồng chuyên dụng để lưu file (Async I/O)
    private static final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private static final Map<Integer, O> pendingO = new ConcurrentHashMap<>();
    private static int idCounter = 1;
    private static JSONArray historyList = new JSONArray();
    private static final int MAX_PLAYER_STATS = 500;
    private static java.util.Map<String, JSONObject> balanceMap = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, JSONObject>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, JSONObject> eldest) {
                    return size() > MAX_PLAYER_STATS;
                }
            });
    private static final String DATA_FILE = "data/baucua_data.json";

    @SuppressWarnings("unchecked")
    public static void init() {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                saveData();
                return;
            }
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(file)) {
                JSONObject data = (JSONObject) parser.parse(reader);
                idCounter = ((Number) data.getOrDefault("idCounter", 1L)).intValue();
                historyList = (JSONArray) data.getOrDefault("history", new JSONArray());
                JSONObject balanceJSON = (JSONObject) data.getOrDefault("balance", new JSONObject());
                balanceMap.clear();
                balanceMap.putAll(balanceJSON);
            }
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] BauCuaManager.init error: " + e.getMessage());
        }
    }

    public static void saveData() {
        final int currentId = idCounter;
        final JSONArray historyClone = new JSONArray();
        synchronized (historyList) {
            historyClone.addAll(historyList);
        }

        final JSONObject balanceClone = new JSONObject();
        synchronized (balanceMap) {
            balanceClone.putAll(balanceMap);
        }

        ioExecutor.submit(() -> {
            JSONObject data = new JSONObject();
            data.put("idCounter", currentId);
            data.put("history", historyClone);
            data.put("balance", balanceClone);

            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write(data.toJSONString());
                writer.flush();
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] BauCuaManager.saveData error: " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ══════════════════════════════════════════════════════════════

    public static void guiHuongDan(Player player) throws IOException {
        Service.send_box_yesno(player, YESNO_HUONG_DAN, "LUẬT BẦU CUA",
                "Chọn linh vật sẽ xuất hiện.\n" +
                        "Ra 1: nhân x2 | Ra 2: x3 | Ra 3: x4\n" +
                        "Tối đa 3 linh vật/ván." +
                        "30s đặt cược, 10s nghỉ. Thoát game giữa chừng sẽ tính là thua",
                new String[] { "Vào chơi", "Lịch sử", "BXH", "Đóng" }, new byte[] { 2, 2, 2, 1 });
    }

    public static void xuLyLenh(Player player) throws IOException {
        synchronized (BauCuaManager.class) {
            if (dangNghi) {
                int conLaiNghi = THOI_GIAN_NGHI - (int) ((System.currentTimeMillis() - thoiGianBatDauNghi) / 1000);
                Service.send_box_ThongBao_OK(player,
                        "Đang trong thời gian nghỉ, vui lòng đợi " + conLaiNghi + "s nữa!");
                return;
            }
            if (phongHienTai == null || !phongHienTai.dangDat) {
                taoPhongMoi();
            }
        }

        if (phongVuaKetThuc != null) {
            CuocDat cuocCu = phongVuaKetThuc.danhSachCuoc.stream()
                    .filter(c -> c.player.id == player.id)
                    .findFirst().orElse(null);
            if (cuocCu != null && !cuocCu.daTraThuong) {
                Service.send_box_ThongBao_OK(player, "Đang xử lý thưởng ván trước, vui lòng chờ!");
                return;
            }
        }

        Phong phong = phongHienTai;
        int conLai = THOI_GIAN_DAT - (int) ((System.currentTimeMillis() - phong.thoiGianBatDau) / 1000);

        CuocDat cuocMinh = phong.danhSachCuoc.stream()
                .filter(c -> c.player.id == player.id)
                .findFirst().orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("VÁN #").append(phong.id)
                .append(" | Còn: ").append(conLai).append("s\n");
        sb.append("Extol: ").append(formatSoLon(player.get_vnd())).append("\n");

        if (cuocMinh != null && !cuocMinh.danhSachO.isEmpty()) {
            sb.append("Đã đặt:\n");
            for (Map.Entry<O, Long> entry : cuocMinh.danhSachO.entrySet()) {
                sb.append("- ").append(entry.getKey().name)
                        .append(": ").append(formatSoLon(entry.getValue())).append(", ");
            }
        }

        String[] menu = new String[] {
                O.TOM.name, O.CUA.name, O.CA.name,
                O.BAU.name, O.NAI.name, O.GA.name,
                "Lịch sử", "BXH", "Xem ván", "Đóng"
        };

        Service.send_box_yesno(player, YESNO_MENU, "BẦU CUA", sb.toString(), menu,
                new byte[] { 2, 2, 2, 2, 2, 2, 2, 2, 2, 1 });
    }

    public static void xuLyYesNo(Player player, int id, int selection) throws IOException {
        if (id == YESNO_MENU) {
            if (selection >= 0 && selection <= 5) {
                // Map selection to O enum
                O selected = null;
                switch (selection) {
                    case 0:
                        selected = O.TOM;
                        break;
                    case 1:
                        selected = O.CUA;
                        break;
                    case 2:
                        selected = O.CA;
                        break;
                    case 3:
                        selected = O.BAU;
                        break;
                    case 4:
                        selected = O.NAI;
                        break;
                    case 5:
                        selected = O.GA;
                        break;
                }

                if (selected != null) {
                    if (phongHienTai == null || !phongHienTai.dangDat) {
                        Service.send_box_ThongBao_OK(player, "Ván đấu đã kết thúc hoặc chưa bắt đầu!");
                        return;
                    }

                    CuocDat cuoc = phongHienTai.danhSachCuoc.stream()
                            .filter(c -> c.player.id == player.id)
                            .findFirst().orElse(null);

                    if (cuoc != null && cuoc.danhSachO.containsKey(selected)) {
                        Service.send_box_ThongBao_OK(player, "Bạn đã đặt linh vật này rồi!");
                        return;
                    }

                    if (cuoc != null && cuoc.danhSachO.size() >= MAX_O_DAT) {
                        Service.send_box_ThongBao_OK(player, "Chỉ được đặt tối đa " + MAX_O_DAT + " linh vật!");
                        return;
                    }

                    pendingO.put(player.id, selected);
                    Service.input_text(player, INPUT_SO_TIEN, "CƯỢC " + selected.name.toUpperCase(),
                            new String[] { "Nhập số Extol muốn cược" });
                }
            } else if (selection == 6) {
                showHistory(player);
            } else if (selection == 7) {
                showBXH(player);
            } else if (selection == 8) {
                xuLyLenh(player);
            }
        } else if (id == YESNO_HUONG_DAN) {
            if (selection == 0) {
                xuLyLenh(player);
            } else if (selection == 1) {
                showHistory(player);
            } else if (selection == 2) {
                showBXH(player);
            }
        }
    }

    public static void xuLyInput(Player player, int id, String text) throws IOException {
        if (id == INPUT_SO_TIEN) {
            O selected = pendingO.remove(player.id);
            if (selected == null) {
                Service.send_box_ThongBao_OK(player, "Ván đấu đã kết thúc, tiền cược không được ghi nhận!");
                return;
            }

            if (phongHienTai == null || !phongHienTai.dangDat) {
                Service.send_box_ThongBao_OK(player, "Ván đấu đã kết thúc, mời bạn đợi ván sau!");
                return;
            }

            long amount;
            try {
                amount = Long.parseLong(text.trim());
            } catch (Exception e) {
                Service.send_box_ThongBao_OK(player, "Số lượng không hợp lệ!");
                return;
            }

            if (amount <= 0) {
                Service.send_box_ThongBao_OK(player, "Tiền cược phải lớn hơn 0!");
                return;
            }

            synchronized (BauCuaManager.class) {
                CuocDat cuoc = phongHienTai.danhSachCuoc.stream()
                        .filter(c -> c.player.id == player.id)
                        .findFirst().orElse(null);

                if (cuoc != null && cuoc.danhSachO.containsKey(selected)) {
                    Service.send_box_ThongBao_OK(player, "Bạn đã đặt linh vật này rồi!");
                    return;
                }

                if (cuoc != null && cuoc.danhSachO.size() >= MAX_O_DAT) {
                    Service.send_box_ThongBao_OK(player, "Chỉ được đặt tối đa " + MAX_O_DAT + " linh vật!");
                    return;
                }
            }

            dangKyCuoc(player, selected, amount);
        }
    }

    private static void dangKyCuoc(Player player, O linhVat, long amount) throws IOException {
        synchronized (BauCuaManager.class) {
            if (phongHienTai == null || !phongHienTai.dangDat) {
                Service.send_box_ThongBao_OK(player, "Ván đấu đã kết thúc, tiền cược không được ghi nhận!");
                return;
            }

            synchronized (player) {
                if (!player.payVnd(amount)) {
                    Service.send_box_ThongBao_OK(player, "Bạn không đủ Extol!");
                    return;
                }
                player.update_money();
            }

            CuocDat cuoc = phongHienTai.danhSachCuoc.stream()
                    .filter(c -> c.player.id == player.id)
                    .findFirst().orElse(null);

            if (cuoc == null) {
                cuoc = new CuocDat();
                cuoc.player = player;
                phongHienTai.danhSachCuoc.add(cuoc);
            }

            long current = cuoc.danhSachO.getOrDefault(linhVat, 0L);
            cuoc.danhSachO.put(linhVat, current + amount);
        }

        Service.send_box_ThongBao_OK(player, "Đã đặt " + formatSoLon(amount) + " Extol vào " + linhVat.name);
        xuLyLenh(player);
    }

    private static void taoPhongMoi() throws IOException {
        Phong phong = new Phong();
        phong.id = idCounter++;
        if (idCounter > 9999)
            idCounter = 1;
        saveData();
        phong.thoiGianBatDau = System.currentTimeMillis();
        phong.dangDat = true;
        phongHienTai = phong;
        phongVuaKetThuc = null;

        Manager.gI().chatKTG(1, "[BẦU CUA] Ván #" + phong.id + " đã mở! Đến NPC Law để tham gia!", 0);

        phong.timerKetThuc = scheduler.schedule(() -> {
            try {
                ketThucVan(phong);
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] BauCuaManager.ketThucVan error: " + e.getMessage());
            }
        }, THOI_GIAN_DAT, TimeUnit.SECONDS);
    }

    private static void ketThucVan(Phong phong) throws IOException {
        phong.dangDat = false;

        synchronized (BauCuaManager.class) {
            dangNghi = true;
            thoiGianBatDauNghi = System.currentTimeMillis();
        }

        try {
            if (phong.danhSachCuoc.isEmpty()) {
                Manager.gI().chatKTG(1, "[BẦU CUA] Không có ai đặt cược, hệ thống tạm dừng chờ người chơi mới.", 0);
                synchronized (BauCuaManager.class) {
                    dangNghi = false;
                }
                return;
            }

            Random rng = new Random();
            phong.ketQua[0] = O.values()[rng.nextInt(6)];
            phong.ketQua[1] = O.values()[rng.nextInt(6)];
            phong.ketQua[2] = O.values()[rng.nextInt(6)];

            String resStr = phong.ketQua[0].name + " — " + phong.ketQua[1].name + " — " + phong.ketQua[2].name;
            Manager.gI().chatKTG(1, "[BẦU CUA] #" + phong.id + " kết quả: " + resStr, 0);

            // Lưu lịch sử
            JSONObject hObj = new JSONObject();
            hObj.put("id", phong.id);
            hObj.put("result", resStr);
            hObj.put("time", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            synchronized (historyList) {
                historyList.add(0, hObj);
                while (historyList.size() > 20)
                    historyList.remove(historyList.size() - 1);
            }

            for (CuocDat cuoc : phong.danhSachCuoc) {
                try {
                    // Tính thưởng (Bao gồm cả người offline và online)
                    long net = chiaThuong(cuoc, phong, resStr);

                    // Cập nhật BXH (Thread-safe)
                    synchronized (balanceMap) {
                        String pKey = String.valueOf(cuoc.player.id);
                        JSONObject pData = (JSONObject) balanceMap.get(pKey);
                        if (pData == null) {
                            pData = new JSONObject();
                            pData.put("name", cuoc.player.name);
                            pData.put("win", 0L);
                            pData.put("loss", 0L);
                        }
                        long currentWin = ((Number) pData.getOrDefault("win", 0L)).longValue();
                        long currentLoss = ((Number) pData.getOrDefault("loss", 0L)).longValue();

                        if (net > 0) {
                            pData.put("win", currentWin + net);
                        } else {
                            pData.put("loss", currentLoss + Math.abs(net));
                        }
                        balanceMap.put(pKey, pData);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi trả thưởng Bầu Cua cho user ID: "
                            + (cuoc.player != null ? cuoc.player.id : "Unknown"));
                    core.GameLogger.error("[Disconnect] BauCuaManager.ketThucVan error: " + e.getMessage());
                }
            } // kết thúc vòng lặp

            saveData();
            moVanMoiSauDelay();

        } catch (Exception e) {
            core.GameLogger.error("Lỗi nghiêm trọng tại ketThucVan Bầu Cua!");
            core.GameLogger.error("[Disconnect] BauCuaManager.ketThucVan error: " + e.getMessage());
            moVanMoiSauDelay();
        } finally {
            // LỚP BẢO VỆ CUỐI CÙNG: DỌN PHÒNG & XÓA RÁC UI CHỜ
            if (phongHienTai == phong) {
                phongHienTai = null;
            }
            phongVuaKetThuc = phong;
            pendingO.clear(); // Xóa sạch rác của những người bấm cược mà thoát game
        }
    }

    private static long chiaThuong(CuocDat cuoc, Phong phong, String resStr) throws IOException {
        long tongNhan = 0;
        StringBuilder log = new StringBuilder();

        for (Map.Entry<O, Long> entry : cuoc.danhSachO.entrySet()) {
            O linhVat = entry.getKey();
            long tienCuoc = entry.getValue();

            int soLanXuatHien = 0;
            for (O r : phong.ketQua) {
                if (r == linhVat)
                    soLanXuatHien++;
            }

            if (soLanXuatHien > 0) {
                long win = tienCuoc + (long) (tienCuoc * soLanXuatHien);
                tongNhan += win;
                log.append("[THẮNG] ").append(linhVat.name).append(" (x").append(soLanXuatHien).append("): +")
                        .append(formatSoLon(win)).append("\n");
            } else {
                log.append("[THUA]  ").append(linhVat.name).append(": -").append(formatSoLon(tienCuoc)).append("\n");
            }
        }

        // Lưu vào hệ thống phần thưởng chờ (Tự động cộng nếu online)
        if (tongNhan > 0) {
            PendingRewardManager.addReward(cuoc.player.id, "baucua", tongNhan);
        }

        // Lợi nhuận thực tế = Tổng tiền nhận về - Tổng số vốn đã bỏ ra
        long net = tongNhan - cuoc.tongCuoc();
        cuoc.daTraThuong = true;

        String msg = "BẦU CUA - KẾT QUẢ\n" + resStr + "\n"
                + "──────────────────\n"
                + log.toString()
                + "──────────────────\n"
                + (net >= 0 ? " TỔNG LÃI: +" : " TỔNG LỖ: -") + formatSoLon(Math.abs(net)) + " Extol";

        try {
            Service.send_box_ThongBao_OK(cuoc.player, msg);
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] BauCuaManager.ketThucVan error: " + e.getMessage());
            // Nuốt lỗi nếu mạng chập chờn
        }

        return net;
    }

    public static void showHistory(Player player) throws IOException {
        StringBuilder sb = new StringBuilder("LỊCH SỬ BẦU CUA\n");
        sb.append("──────────────────\n");
        synchronized (historyList) {
            if (historyList.isEmpty()) {
                sb.append("Chưa có dữ liệu!");
            } else {
                for (Object obj : historyList) {
                    JSONObject h = (JSONObject) obj;
                    sb.append("#").append(h.get("id")).append(": ").append(h.get("result"))
                            .append(" (").append(h.get("time")).append(")\n");
                }
            }
        }
        Service.send_box_ThongBao_OK(player, sb.toString());
    }

    public static void showBXH(Player player) throws IOException {
        StringBuilder sb = new StringBuilder("BẢNG XẾP HẠNG BẦU CUA\n");
        sb.append("──────────────────\n");
        synchronized (balanceMap) {
            if (balanceMap.isEmpty()) {
                sb.append("Chưa có dữ liệu!");
            } else {
                List<JSONObject> list = new ArrayList<>();
                for (JSONObject pData : balanceMap.values()) {
                    long win = ((Number) pData.getOrDefault("win", 0L)).longValue();
                    long loss = ((Number) pData.getOrDefault("loss", 0L)).longValue();
                    long net = win - loss;
                    pData.put("net", net);
                    list.add(pData);
                }
                list.sort((a, b) -> Long.compare(
                        ((Number) b.get("net")).longValue(),
                        ((Number) a.get("net")).longValue()));

                int i = 1;
                for (JSONObject p : list) {
                    long net = ((Number) p.get("net")).longValue();
                    sb.append(i++).append(". ").append(p.get("name")).append(": ")
                            .append(net >= 0 ? "+" : "").append(formatSoLon(net)).append("\n");
                    if (i > 10)
                        break;
                }
            }
        }
        Service.send_box_ThongBao_OK(player, sb.toString());
    }

    private static void moVanMoiSauDelay() {
        scheduler.schedule(() -> {
            try {
                synchronized (BauCuaManager.class) {
                    dangNghi = false;
                }
                taoPhongMoi();
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] BauCuaManager.moVanMoiSauDelay error: " + e.getMessage());
            }
        }, THOI_GIAN_NGHI, TimeUnit.SECONDS);
    }

    public static Map<client.Player, Long> stop() {
        Map<client.Player, Long> result = new HashMap<>();
        synchronized (BauCuaManager.class) {
            if (phongHienTai != null && phongHienTai.dangDat) {
                phongHienTai.dangDat = false;
                if (phongHienTai.timerKetThuc != null) {
                    phongHienTai.timerKetThuc.cancel(false);
                }
                for (CuocDat cuoc : phongHienTai.danhSachCuoc) {
                    long refund = cuoc.tongCuoc();
                    if (refund > 0) {
                        cuoc.player.update_vnd(refund);
                        try {
                            cuoc.player.update_money();
                            result.put(cuoc.player, result.getOrDefault(cuoc.player, 0L) + refund);
                        } catch (Exception e) {
                            core.GameLogger.error("[Disconnect] BauCuaManager.stop error: " + e.getMessage());
                        }
                    }
                }
                phongHienTai = null;
            }
        }
        return result;
    }

    public static String formatSoLon(long n) {
        long a = Math.abs(n);
        if (a >= 1_000_000_000L)
            return (n / 1_000_000_000L) + "B";
        if (a >= 1_000_000L)
            return (n / 1_000_000L) + "M";
        if (a >= 1_000L)
            return (n / 1_000L) + "K";
        return String.valueOf(n);
    }
}