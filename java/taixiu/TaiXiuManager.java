package taixiu;

import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.*;
import java.util.stream.*;
import client.Player;
import core.Manager;
import core.PendingRewardManager;
import core.Service;
import core.Util;
import io.Message;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * TaiXiuManager.java
 * ─────────────────────────────────────────────────────────────────
 * Hệ thống Tài Xỉu Multiplayer — Đặt cược bằng EXP Skill
 * ─────────────────────────────────────────────────────────────────
 */
public class TaiXiuManager {

    // ══════════════════════════════════════════════════════════════
    // CONSTANTS
    // ══════════════════════════════════════════════════════════════

    public static final int INPUT_DAT_CUOC = 110;
    public static final int YESNO_MENU = 200;
    public static final int YESNO_XAC_NHAN_BAY = 201;
    public static final int YESNO_BXH = 202;
    public static final int YESNO_HUONG_DAN_UI = 203;
    public static final int YESNO_CHON_CUA = 204;
    public static final int YESNO_CHON_SLOT = 205;
    public static final int YESNO_ALL_IN_OR_INPUT = 206;
    public static final int INPUT_TIEN_CUOC_UI = 111;

    private static final int THOI_GIAN_DAT = 30;
    private static final int THOI_GIAN_NGHI = 10;
    private static final long BIG_WIN_THRESHOLD = 50_000L;
    private static final double TY_LE_TAI_XIU = 1.95;
    private static final double TY_LE_CUM = 5.0;
    private static final double MAX_CUOC_RATIO = 0.5;

    // ══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════════════════

    public static class CuocDat {
        public Player player;

        public long expCua1;
        public String loaiCua1;

        public long expCua2;
        public String loaiCua2;

        public boolean daTraThuong = false;

        public boolean laCuocDoi() {
            return expCua2 > 0 && loaiCua2 != null;
        }

        public long tongExp() {
            return expCua1 + expCua2;
        }
    }

    public static class Phong {
        public int id;
        public List<CuocDat> danhSachCuoc = new CopyOnWriteArrayList<>();
        public boolean dangDat = true;
        public long thoiGianBatDau;
        public ScheduledFuture<?> timerKetThuc;
        public ScheduledFuture<?> timerCanhBao;

        public int[] xucXac = new int[3];
        public int tong;
        public boolean laCum;
        public boolean laTai;
    }

    public static class BXHEntry {
        public int rank;
        public int playerId;
        public String playerName;
        public long tongExpThang;
        public int soVanThang;
        public int tongVan;
    }

    // ══════════════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════════════

    private static Phong phongHienTai = null;
    private static volatile Phong phongVuaKetThuc = null;
    private static volatile boolean dangNghi = false;
    private static long thoiGianBatDauNghi = 0;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Luồng chuyên dụng để ghi file (Async I/O)
    private static final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private static final Map<Integer, CuocDat> pendingCuoc = new ConcurrentHashMap<>();
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
    private static final String DATA_FILE = "data/taixiu_data.json";
    private static final Map<Integer, String> pendingCuaUI = new ConcurrentHashMap<>();

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
            core.GameLogger.error("[Disconnect] TaiXiuManager.init error: " + e.getMessage());
        }
    }

    public static void saveData() {
        // Chụp (snapshot) dữ liệu hiện tại để luồng game không bị block
        final int currentId = idCounter;
        final JSONArray historyClone = new JSONArray();
        synchronized (historyList) {
            historyClone.addAll(historyList);
        }

        final JSONObject balanceClone = new JSONObject();
        synchronized (balanceMap) {
            balanceClone.putAll(balanceMap);
        }

        // Đẩy tiến trình ghi file ra luồng I/O chạy nền
        ioExecutor.submit(() -> {
            JSONObject data = new JSONObject();
            data.put("idCounter", currentId);
            data.put("history", historyClone);
            data.put("balance", balanceClone);

            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write(data.toJSONString());
                writer.flush();
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] TaiXiuManager.saveData error: " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ══════════════════════════════════════════════════════════════

    public static void guiHuongDan(Player player) throws IOException {
        Service.send_box_ThongBao_OK(player,
                "LUẬT TÀI XỈU\n" +
                        "Tài(11-18) / Xỉu(3-10): x1.95\n" +
                        "Cụm(3 giống nhau): x5.0\n" +
                        "Ra Cụm -> Tài/Xỉu đều THUA!\n" +
                        "Đặt: [slot] [exp] [cua]\n" +
                        "VD: 1 5000 tai\n" +
                        "Đôi: 1 8000 tai 2000 cum\n" +
                        "Giới hạn: tối đa 50% EXP skill.\n" +
                        "Thời gian: 30s đặt cược, 10s nghỉ.");
    }

    public static void guiHuongDanUI(Player player) throws IOException {
        Service.send_box_yesno(player, YESNO_HUONG_DAN_UI, "LUẬT TÀI XỈU",
                "Tài(11-18) / Xỉu(3-10): x1.95." +
                        "Cụm(3 giống nhau): x5.0." +
                        "Ra Cụm -> Tài/Xỉu đều THUA!." +
                        "Thời gian: 30s đặt cược, 10s nghỉ. Thoát game giữa chừng sẽ tính là thua",
                new String[] { "Vào chơi", "Lịch sử", "BXH", "Đóng" }, new byte[] { 2, 2, 2, 1 });
    }

    public static void xuLyLenhUI(Player player) throws IOException {
        synchronized (TaiXiuManager.class) {
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

        Phong phong = phongHienTai;
        int conLai = THOI_GIAN_DAT - (int) ((System.currentTimeMillis() - phong.thoiGianBatDau) / 1000);
        boolean daDat = phong.danhSachCuoc.stream().anyMatch(c -> c.player.id == player.id);

        StringBuilder sb = new StringBuilder();
        sb.append("TÀI XỈU - VÁN #").append(phong.id).append("\n");
        sb.append("Còn: ").append(conLai).append("s\n");
        sb.append("──────────────────\n");

        if (daDat) {
            sb.append("TRẠNG THÁI: [ĐÃ ĐẶT]\n");
            long soNguoi = phong.danhSachCuoc.stream().map(c -> c.player.id).distinct().count();
            sb.append("Số người tham gia: ").append(soNguoi).append("\n");
            sb.append("──────────────────\n");
            sb.append("Bạn đã đặt cược thành công. Chờ kết quả!");

            Service.send_box_yesno(player, YESNO_CHON_CUA, "TÀI XỈU", sb.toString(),
                    new String[] { "Cập nhật", "Lịch sử", "BXH", "Đóng" }, new byte[] { 2, 2, 2, 1 });
        } else {
            sb.append("TRẠNG THÁI: [CHƯA ĐẶT]\n");
            sb.append("Chọn cửa muốn đặt:");
            Service.send_box_yesno(player, YESNO_CHON_CUA, "TÀI XỈU", sb.toString(),
                    new String[] { "Tài", "Xỉu", "Cụm", "Lịch sử", "BXH", "Đóng" }, new byte[] { 2, 2, 2, 2, 2, 1 });
        }
    }

    public static void xuLyLenh(Player player) throws IOException {
        synchronized (TaiXiuManager.class) {
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

        long soNguoi = phong.danhSachCuoc.stream().map(c -> c.player.id).distinct().count();
        boolean daDat = phong.danhSachCuoc.stream().anyMatch(c -> c.player.id == player.id);

        StringBuilder sb = new StringBuilder();
        sb.append("TÀI XỈU — VÁN #").append(phong.id).append("\n");
        sb.append("──────────────────\n");
        sb.append("EXTOL: ").append(formatSoLon(player.get_vnd())).append("\n");
        sb.append("──────────────────\n");
        sb.append("Người: ").append(soNguoi).append(" | Cần: ").append(conLai).append("s\n");
        sb.append("──────────────────\n");

        if (daDat) {
            sb.append("TRẠNG THÁI: [ĐÃ ĐẶT]\n");
        } else {
            sb.append("TRẠNG THÁI: [CHƯA ĐẶT]\n");
        }

        if (!phong.danhSachCuoc.isEmpty()) {
            sb.append("Đã đặt:\n");
            Map<Integer, List<CuocDat>> theo_player = phong.danhSachCuoc.stream()
                    .collect(Collectors.groupingBy(c -> c.player.id));

            for (Map.Entry<Integer, List<CuocDat>> entry : theo_player.entrySet()) {
                CuocDat cuoc = entry.getValue().get(0);
                boolean laMinh = cuoc.player.id == player.id;
                String tag = cuoc.laCuocDoi() ? " [ĐÔI]" : "";
                String expStr;
                if (laMinh) {
                    expStr = formatSoLon(cuoc.expCua1) + " EXT";
                    if (cuoc.laCuocDoi())
                        expStr += " + " + formatSoLon(cuoc.expCua2) + " EXT";
                } else {
                    expStr = "???";
                }

                sb.append(emojiCua(cuoc.loaiCua1));
                if (cuoc.laCuocDoi())
                    sb.append(emojiCua(cuoc.loaiCua2));
                sb.append(" ").append(cuoc.player.name).append(tag).append(": ").append(expStr).append("\n");
            }
            sb.append("──────────────────\n");
        }

        if (daDat) {
            sb.append("\nBạn đã đặt! Chờ kết quả...");
            Service.send_box_yesno(player, YESNO_MENU, "TÀI XỈU", sb.toString(),
                    new String[] { "Cập nhật", "Lịch sử", "BXH", "Đóng" },
                    new byte[] { 2, 2, 2, 1 });
        } else {
            sb.append("Tài 11-18 | Xỉu 3-10 (x1.95)\n");
            sb.append("Cụm (3 xúc xắc giống) x5.0\n");
            sb.append("Cụm thắng -> Tài/Xỉu THUA!");

            Service.send_box_yesno(player, YESNO_MENU, "TÀI XỈU", sb.toString(),
                    new String[] { "Đặt cược", "Lịch sử", "BXH", "Đóng" },
                    new byte[] { 2, 2, 2, 1 });
        }
    }

    public static void hienInputDatCuoc(Player player) throws IOException {
        Service.input_text(player, INPUT_DAT_CUOC, "ĐẶT CƯỢC",
                new String[] {
                        "VD: 1 5000 tai | ext 1000 xiu"
                });
    }

    public static void xuLyInput(Player player, int id, String text) throws IOException {
        if (id == INPUT_TIEN_CUOC_UI) {
            String door = pendingCuaUI.remove(player.id);
            if (door == null) {
                Service.send_box_ThongBao_OK(player, "Lỗi dữ liệu đặt cược, vui lòng thử lại!");
                return;
            }

            if (!Util.isnumber(text)) {
                Service.send_box_ThongBao_OK(player, "Số lượng nhập không hợp lệ!");
                return;
            }

            String cmd = text + " " + door;
            processBatchChat(player, cmd);
            return;
        }

        if (id != INPUT_DAT_CUOC)
            return;

        processBatchChat(player, text);
    }

    public static void xuLyYesNo(Player player, int id, int selection) throws IOException {
        switch (id) {
            case YESNO_MENU: {
                boolean daDat = phongHienTai != null
                        && phongHienTai.danhSachCuoc.stream().anyMatch(c -> c.player.id == player.id);
                if (daDat) {
                    if (selection == 0)
                        xuLyLenh(player); // Cập nhật
                    else if (selection == 1)
                        showHistory(player);
                    else if (selection == 2)
                        showBXH(player);
                } else {
                    if (selection == 0) {
                        hienInputDatCuoc(player);
                    } else if (selection == 1)
                        showHistory(player);
                    else if (selection == 2)
                        showBXH(player);
                }
                break;
            }

            case YESNO_XAC_NHAN_BAY:
                if (selection == 0) {
                    CuocDat cuoc = pendingCuoc.remove(player.id);
                    if (cuoc != null) {
                        dangKyCuoc(player, cuoc);
                    }
                }
                break;

            case YESNO_HUONG_DAN_UI:
                if (selection == 0) { // Vào chơi
                    xuLyLenhUI(player);
                } else if (selection == 1) { // Lịch sử
                    showHistory(player);
                } else if (selection == 2) { // BXH
                    showBXH(player);
                }
                break;

            case YESNO_CHON_CUA: {
                boolean daDat = phongHienTai != null
                        && phongHienTai.danhSachCuoc.stream().anyMatch(c -> c.player.id == player.id);
                if (daDat) {
                    if (selection == 0) { // Cập nhật
                        xuLyLenhUI(player);
                    } else if (selection == 1) { // Lịch sử
                        showHistory(player);
                    } else if (selection == 2) { // BXH
                        showBXH(player);
                    }
                } else {
                    if (selection >= 0 && selection <= 2) {
                        String[] doors = { "tai", "xiu", "cum" };
                        String door = doors[selection];
                        pendingCuaUI.put(player.id, door);

                        String msg = "Bạn muốn đặt [" + door.toUpperCase() + "] bằng cách nào?\n"
                                + "- Extol hiện có: " + formatSoLon(player.get_vnd()) + "\n"
                                + "- Tất tay: Cược toàn bộ số tiền đang có.";

                        Service.send_box_yesno(player, YESNO_ALL_IN_OR_INPUT, "CHỌN CÁCH CƯỢC", msg,
                                new String[] { "Nhập số", "Tất tay", "Hủy" }, new byte[] { 2, 2, 1 });
                    } else if (selection == 3) { // Lịch sử
                        showHistory(player);
                    } else if (selection == 4) { // BXH
                        showBXH(player);
                    }
                }
                break;
            }

            case YESNO_ALL_IN_OR_INPUT: {
                String door = pendingCuaUI.get(player.id);
                if (door == null)
                    return;

                if (selection == 0) { // Nhập số
                    Service.input_text(player, INPUT_TIEN_CUOC_UI, "ĐẶT " + door.toUpperCase(),
                            new String[] { "Nhập số lượng Extol muốn cược:" });
                } else if (selection == 1) { // Tất tay
                    pendingCuaUI.remove(player.id);
                    long amount = player.get_vnd();
                    if (amount <= 0) {
                        Service.send_box_ThongBao_OK(player, "Bạn không còn Extol để Tất tay!");
                        return;
                    }

                    CuocDat cuoc = new CuocDat();
                    cuoc.player = player;
                    cuoc.expCua1 = amount;
                    cuoc.loaiCua1 = door;
                    dangKyCuoc(player, cuoc);
                } else { // Hủy
                    pendingCuaUI.remove(player.id);
                }
                break;
            }
        }
    }

    private static void dangKyCuoc(Player player, CuocDat cuoc) throws IOException {
        synchronized (TaiXiuManager.class) {
            if (phongHienTai == null || !phongHienTai.dangDat) {
                Service.send_box_ThongBao_OK(player, "Ván đấu đã kết thúc, mời bạn đợi ván sau!");
                return;
            }
            if (!player.payVnd(cuoc.tongExp())) {
                Service.send_box_ThongBao_OK(player, "Extol không đủ!");
                return;
            }
            player.update_money();
            phongHienTai.danhSachCuoc.add(cuoc);
        }
        Service.send_box_ThongBao_OK(player, buildXacNhanMessage(cuoc));
        thongBaoPhong(player.name + (cuoc.laCuocDoi() ? " [ĐÔI]" : "") + " đã vào bàn!");
        Player.flush(player, false);
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

        Manager.gI().chatKTG(1, "[TÀI XỈU] Ván #" + phong.id + " vừa mở! Gõ taixiu để tham gia!", 0);

        phong.timerCanhBao = scheduler.schedule(() -> {
            try {
                if (phong.dangDat && !phong.danhSachCuoc.isEmpty()) {
                    Manager.gI().chatKTG(1, "[TÀI XỈU] Còn 10 giây! Đặt cược nhanh lên!", 0);
                }
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] TaiXiuManager.taoPhongMoi error: " + e.getMessage());
            }
        }, THOI_GIAN_DAT - 10, TimeUnit.SECONDS);

        phong.timerKetThuc = scheduler.schedule(() -> {
            try {
                ketThucVan(phong);
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] TaiXiuManager.taoPhongMoi error: " + e.getMessage());
            }
        }, THOI_GIAN_DAT, TimeUnit.SECONDS);
    }

    private static void ketThucVan(Phong phong) throws IOException {
        phong.dangDat = false;

        synchronized (TaiXiuManager.class) {
            dangNghi = true;
            thoiGianBatDauNghi = System.currentTimeMillis();
        }

        try {
            if (phong.danhSachCuoc.isEmpty()) {
                Manager.gI().chatKTG(1, "[TÀI XỈU] Không có ai đặt cược, hệ thống tạm dừng. Gõ taixiu để mở ván mới!",
                        0);
                synchronized (TaiXiuManager.class) {
                    dangNghi = false;
                }
                return;
            }

            Random rng = new Random();
            phong.xucXac[0] = rng.nextInt(6) + 1;
            phong.xucXac[1] = rng.nextInt(6) + 1;
            phong.xucXac[2] = rng.nextInt(6) + 1;
            phong.tong = phong.xucXac[0] + phong.xucXac[1] + phong.xucXac[2];
            phong.laCum = (phong.xucXac[0] == phong.xucXac[1] && phong.xucXac[1] == phong.xucXac[2]);
            phong.laTai = phong.tong >= 11;

            String xucXacStr = diceFace(phong.xucXac[0]) + " " + diceFace(phong.xucXac[1]) + " "
                    + diceFace(phong.xucXac[2]);
            String ketQuaStr = (phong.laCum ? "CỤM " + phong.xucXac[0] : (phong.laTai ? "TÀI" : "XỈU")) + " ("
                    + phong.tong + ")";

            // Lưu lịch sử
            JSONObject hObj = new JSONObject();
            hObj.put("id", phong.id);
            hObj.put("result", ketQuaStr);
            hObj.put("dice", xucXacStr);
            hObj.put("time", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            synchronized (historyList) {
                historyList.add(0, hObj);
                while (historyList.size() > 20)
                    historyList.remove(historyList.size() - 1);
            }

            Manager.gI().chatKTG(1, "[TÀI XỈU] #" + phong.id + " | " + xucXacStr + " → " + ketQuaStr, 0);

            long tongThangMax = 0;
            Player winner = null;

            for (CuocDat cuoc : phong.danhSachCuoc) {
                try {
                    // Tính thưởng (Bao gồm cả người offline và online)
                    long net = chiaThuong(cuoc, phong, xucXacStr, ketQuaStr);

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

                    if (net > tongThangMax) {
                        tongThangMax = net;
                        winner = cuoc.player;
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi trả thưởng Tài Xỉu cho user ID: "
                            + (cuoc.player != null ? cuoc.player.id : "Unknown"));
                    core.GameLogger.printStackTrace(e);
                }
            } // kết thúc vòng lặp

            if (tongThangMax >= BIG_WIN_THRESHOLD && winner != null) {
                Manager.gI().chatKTG(1,
                        "[TÀI XỈU] " + winner.name + " vừa thắng " + formatSoLon(tongThangMax) + " EXTOL!", 0);
            }

            saveData();
            moVanMoiSauDelay();

        } catch (Exception e) {
            core.GameLogger.error("Lỗi nghiêm trọng tại ketThucVan Tài Xỉu!");
            core.GameLogger.printStackTrace(e);
            moVanMoiSauDelay(); // Cứu vớt: Vẫn mở ván mới nếu có lỗi nặng
        } finally {
            // LỚP BẢO VỆ CUỐI CÙNG: LUÔN DỌN PHÒNG KHÔNG CẦN BIẾT LỖI HAY KHÔNG
            if (phongHienTai == phong) {
                phongHienTai = null;
            }
            phongVuaKetThuc = phong;
            pendingCuoc.clear();
            pendingCuaUI.clear(); // Quét rác bộ nhớ
        }
    }

    private static long chiaThuong(CuocDat cuoc, Phong phong, String xucXacStr, String ketQuaStr) throws IOException {
        long nhanVe = 0;
        long matDi = 0;
        StringBuilder log = new StringBuilder();

        if (kiemTraCuaThang(cuoc.loaiCua1, phong)) {
            long win = (long) (cuoc.expCua1 * tyLeThang(cuoc.loaiCua1));
            nhanVe += win;
            log.append("[THANG] ").append(cuoc.loaiCua1.toUpperCase())
                    .append(": +").append(formatSoLon(win)).append("\n");
        } else {
            matDi += cuoc.expCua1;
            log.append("[THUA]  ").append(cuoc.loaiCua1.toUpperCase())
                    .append(": -").append(formatSoLon(cuoc.expCua1)).append("\n");
        }

        if (cuoc.laCuocDoi()) {
            if (kiemTraCuaThang(cuoc.loaiCua2, phong)) {
                long win = (long) (cuoc.expCua2 * tyLeThang(cuoc.loaiCua2));
                nhanVe += win;
                log.append("[THANG] ").append(cuoc.loaiCua2.toUpperCase())
                        .append(": +").append(formatSoLon(win)).append("\n");
            } else {
                matDi += cuoc.expCua2;
                log.append("[THUA]  ").append(cuoc.loaiCua2.toUpperCase())
                        .append(": -").append(formatSoLon(cuoc.expCua2)).append("\n");
            }
        }

        long net = nhanVe - matDi;

        // Lưu vào hệ thống phần thưởng chờ (Tự động cộng nếu online)
        if (nhanVe > 0) {
            PendingRewardManager.addReward(cuoc.player.id, "taixiu", nhanVe);
        }

        cuoc.daTraThuong = true;

        String skillInfo = "Tiền tệ: Extol\n";
        String resMsg = "TÀI XỈU - KẾT QUẢ\n" + xucXacStr + " → " + ketQuaStr + "\n"
                + "──────────────────\n"
                + skillInfo + log.toString()
                + "──────────────────\n"
                + (net >= 0 ? " TỔNG THẮNG: +" : " TỔNG THUA: -") + formatSoLon(Math.abs(net)) + " EXTOL";

        try {
            // Gửi thông báo nổi
            Message m = new Message(17);
            m.writer().writeShort(cuoc.player.index_map);
            m.writer().writeByte(1);
            m.writer().writeUTF(resMsg);
            cuoc.player.addmsg(m);
            m.cleanup();

            // Gửi thông báo hộp hội thoại
            Service.send_box_ThongBao_OK(cuoc.player, resMsg);
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] TaiXiuManager.ketThucVan error: " + e.getMessage());
            // Nuốt lỗi nếu mạng chập chờn lúc nhận gói tin
        }

        return net;
    }

    private static String buildXacNhanMessage(CuocDat cuoc) {
        StringBuilder sb = new StringBuilder("ĐÃ ĐẶT CƯỢC!\n");
        sb.append("---").append("\n");
        sb.append(cuoc.loaiCua1.toUpperCase()).append(": ")
                .append(formatSoLon(cuoc.expCua1)).append(" EXTOL\n");
        if (cuoc.laCuocDoi()) {
            sb.append(cuoc.loaiCua2.toUpperCase()).append(": ")
                    .append(formatSoLon(cuoc.expCua2)).append(" EXTOL\n");
            sb.append("---\nKịch bản:\n").append(tinhKichBan(cuoc));
        }
        return sb.toString();
    }

    private static String tinhKichBan(CuocDat cuoc) {
        StringBuilder sb = new StringBuilder("Kịch bản:\n");
        for (String k : new String[] { "tai", "xiu", "cum" }) {
            long net = 0;
            net += cuoc.loaiCua1.equals(k)
                    ? (long) (cuoc.expCua1 * (tyLeThang(cuoc.loaiCua1) - 1))
                    : -cuoc.expCua1;
            if (cuoc.laCuocDoi()) {
                net += cuoc.loaiCua2.equals(k)
                        ? (long) (cuoc.expCua2 * (tyLeThang(cuoc.loaiCua2) - 1))
                        : -cuoc.expCua2;
            }
            sb.append(k.toUpperCase()).append(": ")
                    .append(net >= 0 ? "+" : "")
                    .append(formatSoLon(net)).append("\n");
        }
        return sb.toString();
    }

    private static boolean kiemTraCuaThang(String loai, Phong p) {
        return switch (loai) {
            case "tai" -> !p.laCum && p.laTai;
            case "xiu" -> !p.laCum && !p.laTai;
            case "cum" -> p.laCum;
            default -> false;
        };
    }

    private static boolean isValidLoai(String s) {
        return s.equals("tai") || s.equals("xiu") || s.equals("cum");
    }

    private static double tyLeThang(String l) {
        return l.equals("cum") ? TY_LE_CUM : TY_LE_TAI_XIU;
    }

    private static String emojiCua(String l) {
        return switch (l) {
            case "tai" -> "[T]";
            case "xiu" -> "[X]";
            case "cum" -> "[C]";
            default -> "[?]";
        };
    }

    private static String diceFace(int n) {
        return String.valueOf(n);
    }

    private static String formatSoLon(long n) {
        long a = Math.abs(n);
        if (a >= 1_000_000_000L)
            return (n / 1_000_000_000L) + "B";
        if (a >= 1_000_000L)
            return (n / 1_000_000L) + "M";
        if (a >= 1_000L)
            return (n / 1_000L) + "K";
        return String.valueOf(n);
    }

    private static void thongBaoPhong(String msg) throws IOException {
        if (phongHienTai == null)
            return;
        for (CuocDat c : phongHienTai.danhSachCuoc)
            Service.send_box_ThongBao_OK(c.player, msg);
    }

    private static void moVanMoiSauDelay() {
        scheduler.schedule(() -> {
            try {
                Manager.gI().chatKTG(1, "[TÀI XỈU] Ván mới sắp bắt đầu!", 0);
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] TaiXiuManager.moVanMoiSauDelay error: " + e.getMessage());
            }
        }, THOI_GIAN_NGHI - 3, TimeUnit.SECONDS);
        scheduler.schedule(() -> {
            try {
                synchronized (TaiXiuManager.class) {
                    dangNghi = false;
                }
                taoPhongMoi();
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] TaiXiuManager.moVanMoiSauDelay error: " + e.getMessage());
            }
        }, THOI_GIAN_NGHI, TimeUnit.SECONDS);
    }

    public static Map<client.Player, Long> stop() {
        Map<client.Player, Long> result = new HashMap<>();
        synchronized (TaiXiuManager.class) {
            if (phongHienTai != null && phongHienTai.dangDat) {
                phongHienTai.dangDat = false;
                if (phongHienTai.timerKetThuc != null) {
                    phongHienTai.timerKetThuc.cancel(false);
                }
                if (phongHienTai.timerCanhBao != null) {
                    phongHienTai.timerCanhBao.cancel(false);
                }
                for (CuocDat cuoc : phongHienTai.danhSachCuoc) {
                    long refund = cuoc.tongExp();
                    if (refund > 0) {
                        cuoc.player.update_vnd(refund);
                        try {
                            cuoc.player.update_money();
                            result.put(cuoc.player, result.getOrDefault(cuoc.player, 0L) + refund);
                        } catch (Exception e) {
                            core.GameLogger.error("[Disconnect] TaiXiuManager.stop error: " + e.getMessage());
                        }
                    }
                }
                phongHienTai = null;
            }
        }
        return result;
    }

    private static void processBatchChat(Player player, String text) throws IOException {
        if (phongHienTai == null || !phongHienTai.dangDat) {
            Service.send_box_ThongBao_OK(player, "Phòng đã đóng! Gõ taixiu để vào ván mới.");
            return;
        }

        boolean daDat = phongHienTai.danhSachCuoc.stream().anyMatch(c -> c.player.id == player.id);
        if (daDat) {
            Service.send_box_ThongBao_OK(player, "Bạn đã đặt cược trong ván này rồi!");
            return;
        }

        String[] parts = text.trim().toLowerCase().split("\\s+");

        // hỗ trợ cú pháp cũ: nếu có prefix "ext", bỏ nó đi
        if (parts[0].equalsIgnoreCase("ext")) {
            String[] newParts = new String[parts.length - 1];
            System.arraycopy(parts, 1, newParts, 0, parts.length - 1);
            parts = newParts;
        }

        if (parts.length != 2 && parts.length != 4) {
            Service.send_box_ThongBao_OK(player, "Sai định dạng!\nĐơn: 5000 tai\nĐôi: 8000 tai 2000 cum");
            return;
        }

        long expCua1;
        String loaiCua1 = parts[1];
        try {
            expCua1 = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            Service.send_box_ThongBao_OK(player, "Số lượng cược không hợp lệ!");
            return;
        }

        if (!isValidLoai(loaiCua1)) {
            Service.send_box_ThongBao_OK(player, "Cửa 1: chỉ nhập tai / xiu / cum");
            return;
        }
        if (expCua1 <= 0) {
            Service.send_box_ThongBao_OK(player, "Tiền cược phải lớn hơn 0!");
            return;
        }

        long expCua2 = 0;
        String loaiCua2 = null;
        if (parts.length == 4) {
            try {
                expCua2 = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                Service.send_box_ThongBao_OK(player, "Tiền cược cửa 2 không hợp lệ!");
                return;
            }
            loaiCua2 = parts[3];
            if (!isValidLoai(loaiCua2)) {
                Service.send_box_ThongBao_OK(player, "Cửa 2: chỉ nhập tai / xiu / cum");
                return;
            }
            if (expCua2 <= 0) {
                Service.send_box_ThongBao_OK(player, "Tiền cược cửa 2 phải lớn hơn 0!");
                return;
            }
            if (loaiCua1.equals(loaiCua2)) {
                Service.send_box_ThongBao_OK(player, "Không thể đặt 2 cửa giống nhau!");
                return;
            }
        }

        CuocDat cuoc = new CuocDat();
        cuoc.player = player;
        cuoc.expCua1 = expCua1;
        cuoc.loaiCua1 = loaiCua1;
        cuoc.expCua2 = expCua2;
        cuoc.loaiCua2 = loaiCua2;

        long tongExp = cuoc.tongExp();
        if (tongExp > player.get_vnd()) {
            Service.send_box_ThongBao_OK(player, "Không đủ Extol! Bạn có: " + formatSoLon(player.get_vnd()));
            return;
        }

        if (cuoc.laCuocDoi()) {
            boolean laBay = (loaiCua1.equals("tai") && loaiCua2.equals("xiu"))
                    || (loaiCua1.equals("xiu") && loaiCua2.equals("tai"));
            if (laBay) {
                pendingCuoc.put(player.id, cuoc);
                Service.send_box_yesno(player, YESNO_XAC_NHAN_BAY, "CẢNH BÁO",
                        "CƯỢC ĐÔI TÀI + XỈU!\nNếu ra CỤM - MẤT TRẮNG\n" + formatSoLon(tongExp) + " EXTOL!\n\n"
                                + tinhKichBan(cuoc) + "\nVẫn đặt?",
                        new String[] { "Xác nhận", "Hủy" }, new byte[] { 2, 1 });
                return;
            }
        }

        dangKyCuoc(player, cuoc);
    }

    public static void showHistory(Player player) throws IOException {
        StringBuilder sb = new StringBuilder("LỊCH SỬ TÀI XỈU\n");
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
        StringBuilder sb = new StringBuilder("BẢNG XẾP HẠNG TÀI XỈU\n");
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
}