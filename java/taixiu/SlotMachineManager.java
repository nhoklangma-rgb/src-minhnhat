package taixiu;

import client.Player;
import core.Service;
import core.Util;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SlotMachineManager {
    public static final int MENU_ID = 510;
    public static final int INPUT_ID = 511;
    public static final int RESULT_MENU_ID = 512;

    // Bộ ký tự Đá Quý (Tất cả đều 5 ký tự để khung hình vuông vức)
    private static final String JACKPOT_SYMBOL = "[ 7 ]";
    private static final String[] SYMBOLS = {
            "[✦]", "[◈]", "[❂]", "[ ◉ ]",
            "[ ♥ ]", "[ ♦ ]", "[ ♣ ]", "[ ♠ ]"
    };

    public static final AtomicLong jackpotPool = new AtomicLong(1000000); // Khởi tạo 1M
    private static final ConcurrentHashMap<Integer, Long> playerBets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ScheduledFuture<?>> activeSpins = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5); // Tăng thread lên 5
                                                                                                   // cho mượt

    public static void showMainMenu(Player p) throws IOException {
        if (p == null)
            return;
        long currentBet = playerBets.getOrDefault(p.id, 10000L);
        String msg = " MÁY QUAY HŨ ĐÁ QUÝ \n"
                + "Quỹ Jackpot: " + Util.number_format(jackpotPool.get()) + " Extol\n"
                + "Mức cược hiện tại: " + Util.number_format(currentBet) + " Extol\n"
                + "--------------------------\n"
                + "Hướng dẫn: Quay ra 3, 4 hoặc 5 hình giống nhau để nhận thưởng lớn!\n"
                + "Nổ hũ khi quay ra 5 hình " + JACKPOT_SYMBOL;

        Service.send_box_yesno(p, MENU_ID, "Slot Machine", msg,
                new String[] { "Quay ngay", "Đổi mức cược", "Lịch sử", "Đóng" },
                new byte[] { 0, 1, 2, -1 });
    }

    public static void handleYesNo(Player p, int index, int id) throws IOException {
        if (id == MENU_ID) {
            switch (index) {
                case 0:
                    startSpin(p);
                    break;
                case 1:
                    Service.input_text(p, INPUT_ID, "Nhập mức cược (Min: 1.000, Max: 2.000.000.000)",
                            new String[] { "Số tiền" });
                    break;
                case 2:
                    Service.send_box_ThongBao_OK(p, "Tính năng lịch sử đang được cập nhật!");
                    break;
            }
        } else if (id == RESULT_MENU_ID) {
            switch (index) {
                case 0:
                    startSpin(p);
                    break; // Quay tiếp
                case 1:
                    Service.input_text(p, INPUT_ID, "Nhập mức cược (Min: 1.000, Max: 2.000.000.000)",
                            new String[] { "Số tiền" });
                    break;
                case 2:
                    Service.send_box_ThongBao_OK(p, "Tính năng lịch sử đang được cập nhật!");
                    break;
            }
        }
    }

    public static void handleInput(Player p, String text) throws IOException {
        if (!Util.isnumber(text)) {
            Service.send_box_ThongBao_OK(p, "Vui lòng nhập số hợp lệ!");
            return;
        }
        long bet = Long.parseLong(text);
        if (bet < 1000) {
            Service.send_box_ThongBao_OK(p, "Mức cược tối thiểu là 1.000 Extol!");
            return;
        }
        if (bet > 2000000000L) {
            bet = 2000000000L;
        }
        playerBets.put(p.id, bet);
        Service.send_box_ThongBao_OK(p, "Đã đổi mức cược thành: " + Util.number_format(bet) + " Extol");
        showMainMenu(p);
    }

    // Gọi hàm này trong Player.java ở đoạn xử lý disconnect / logout
    public static void clearPlayerSession(int playerId) {
        playerBets.remove(playerId);
        cancelSpin(playerId); // Đề phòng họ out mạng lúc đang quay
    }

    public static void startSpin(Player p) throws IOException {
        if (activeSpins.containsKey(p.id)) {
            Service.send_box_ThongBao_OK(p, "Bạn đang có một lượt quay đang diễn ra!");
            return;
        }

        long bet = playerBets.getOrDefault(p.id, 10000L);
        if (p.get_vnd() < bet) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ Extol! Hiện có: " + Util.number_format(p.get_vnd()));
            return;
        }

        // Trừ tiền cược
        p.update_vnd(-bet);
        p.update_money();

        // 5% tiền cược vào Jackpot
        jackpotPool.addAndGet(bet * 5 / 100);

        // Random kết quả trước khi animation chạy để chốt hạ từng cột
        Random rand = new Random();
        final String[] finalResult = new String[5];
        for (int i = 0; i < 5; i++) {
            if (rand.nextInt(500) == 0)
                finalResult[i] = JACKPOT_SYMBOL;
            else
                finalResult[i] = SYMBOLS[rand.nextInt(SYMBOLS.length)];
        }

        final int[] iteration = { 0 };
        final boolean[] lockedReels = new boolean[5]; // Dùng để khóa từng cột lại

        final String[][] grid = new String[6][5];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 5; c++)
                grid[r][c] = getRandomSymbol();
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                // FIX BUG NUỐT TIỀN: Nếu rớt mạng, vẫn phải âm thầm trả thưởng rồi mới hủy quay
                if (p == null || p.conn == null || !p.conn.connected) {
                    if (p != null) {
                        processFinalResult(p, bet, finalResult, grid, true); // true = đang offline (âm thầm trả thưởng)
                    }
                    cancelSpin(p != null ? p.id : -1);
                    return;
                }

                iteration[0]++;
                int tick = iteration[0];

                // THUẬT TOÁN DỪNG TỪNG CỘT (Khóa ở các nhịp 6, 10, 14, 18, 24)
                if (tick == 6) {
                    lockedReels[0] = true;
                    grid[2][0] = finalResult[0];
                }
                if (tick == 10) {
                    lockedReels[1] = true;
                    grid[2][1] = finalResult[1];
                }
                if (tick == 14) {
                    lockedReels[2] = true;
                    grid[2][2] = finalResult[2];
                }
                if (tick == 18) {
                    lockedReels[3] = true;
                    grid[2][3] = finalResult[3];
                }
                if (tick == 24) {
                    lockedReels[4] = true;
                    grid[2][4] = finalResult[4];
                } // Cột cuối delay lâu hơn

                if (tick <= 25) {
                    // Dịch chuyển thác nước (Chỉ dịch những cột chưa bị khóa)
                    for (int c = 0; c < 5; c++) {
                        if (!lockedReels[c]) {
                            for (int r = 5; r > 0; r--) {
                                grid[r][c] = grid[r - 1][c];
                            }
                            grid[0][c] = getRandomSymbol();
                        }
                    }

                    // VẼ GIAO DIỆN CHỐNG LỆCH 100%
                    StringBuilder sb = new StringBuilder("🎰 ĐANG QUAY...\n\n");
                    for (int r = 0; r < 6; r++) {
                        if (r == 2)
                            sb.append(" >-----------------------< \n");

                        sb.append(" | ");
                        for (int c = 0; c < 5; c++) {
                            sb.append(grid[r][c]);
                        }
                        sb.append(" | \n");

                        if (r == 2)
                            sb.append(" >-----------------------< \n");
                    }
                    sb.append("\nĐang chờ kết quả...");
                    Service.send_box_ThongBao_OK(p, sb.toString());
                } else {
                    processFinalResult(p, bet, finalResult, grid, false); // false = đang online (hiện UI bình thường)
                    cancelSpin(p.id);
                }
            } catch (Exception e) {
                core.GameLogger.error("[Disconnect] SlotMachineManager.startSpin error: " + e.getMessage());
                if (p != null)
                    cancelSpin(p.id);
            }
        }, 0, 150, TimeUnit.MILLISECONDS); // Tốc độ 150ms cực mượt

        activeSpins.put(p.id, future);
    }

    private static String getRandomSymbol() {
        Random rand = new Random();
        if (rand.nextInt(100) < 2)
            return JACKPOT_SYMBOL;
        return SYMBOLS[rand.nextInt(SYMBOLS.length)];
    }

    // Đã thêm tham số boolean isOffline
    private static void processFinalResult(Player p, long bet, String[] result, String[][] grid, boolean isOffline)
            throws IOException {
        int maxMatch = 0;
        String matchSymbol = "";
        for (int i = 0; i < 5; i++) {
            int count = 0;
            for (int j = 0; j < 5; j++) {
                if (result[i].equals(result[j]))
                    count++;
            }
            if (count > maxMatch) {
                maxMatch = count;
                matchSymbol = result[i];
            }
        }

        long winAmount = 0;
        boolean isJackpot = false;

        if (maxMatch == 5 && matchSymbol.equals(JACKPOT_SYMBOL)) {
            winAmount = jackpotPool.getAndSet(1000000);
            isJackpot = true;
        } else if (maxMatch == 5) {
            winAmount = bet * 50;
        } else if (maxMatch == 4) {
            winAmount = bet * 10;
        } else if (maxMatch == 3) {
            winAmount = bet * 4;
        }

        // 1. CỘNG TIỀN VÀ THÔNG BÁO JACKPOT (Kể cả khi người chơi rớt mạng)
        if (winAmount > 0) {
            p.update_vnd(winAmount);
            p.update_money();
        }
        if (isJackpot) {
            core.Manager.gI().chatKTG(p, "Chúc mừng người chơi [" + p.name + "] đã NỔ HŨ Slot Machine, nhận "
                    + Util.number_format(winAmount) + " Extol!");
        }

        // 2. NẾU RỚT MẠNG RỒI THÌ DỪNG LẠI, KHÔNG GỬI GIAO DIỆN NỮA ĐỂ TRÁNH LỖI
        if (isOffline) {
            return;
        }

        // 3. NẾU ĐANG ONLINE, VẼ BẢNG KẾT QUẢ VÀ GỬI VỀ
        StringBuilder sb = new StringBuilder();
        if (isJackpot) {
            sb.append("CHÚC MỪNG: NỔ HŨ JACKPOT!!!\n");
        } else if (winAmount > 0) {
            sb.append("CHÚC MỪNG: BẠN ĐÃ THẮNG!\n");
        } else {
            sb.append("RẤT TIẾC: CHÚC MAY MẮN LẦN SAU!\n");
        }

        sb.append("\n");
        for (int r = 0; r < 6; r++) {
            if (r == 2)
                sb.append(" >-----------------------< \n");

            sb.append(" | ");
            for (int c = 0; c < 5; c++) {
                sb.append(grid[r][c]);
            }
            sb.append(" | \n");

            if (r == 2)
                sb.append(" >-----------------------< \n");
        }

        if (winAmount > 0) {
            sb.append("Tiền thưởng: +").append(Util.number_format(winAmount)).append(" Extol");
        } else {
            sb.append("Bạn đã mất ").append(Util.number_format(bet)).append(" Extol");
        }

        Service.send_box_yesno(p, RESULT_MENU_ID, "Kết Quả Slot", sb.toString(),
                new String[] { "Quay tiếp", "Đổi mức cược", "Lịch sử", "Đóng" },
                new byte[] { 0, 1, 2, -1 });
    }

    private static void cancelSpin(int playerId) {
        ScheduledFuture<?> future = activeSpins.remove(playerId);
        if (future != null)
            future.cancel(false);
    }
}