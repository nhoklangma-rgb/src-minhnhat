package taixiu;

import client.Player;
import core.PendingRewardManager;
import core.Service;
import core.Util;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * BlackjackManager.java
 * Quản lý hệ thống game Blackjack (Xì Dách) tích hợp NPC Law.
 */
public class BlackjackManager {

    // Các ID giao diện để tránh xung đột
    public static final int YESNO_MAIN_MENU = 210;
    public static final int YESNO_GAME_ACTION = 211;
    public static final int YESNO_HISTORY = 212;
    public static final int INPUT_BET = 120;

    private static final String DATA_PATH = "data/blackjack_data.json";
    private static final ConcurrentHashMap<Integer, BlackjackGame> activeGames = new ConcurrentHashMap<>();
    private static final List<String> history = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Long> playerStats = new ConcurrentHashMap<>();
    private static ScheduledExecutorService saveTask;

    // Tự động dọn rác đơn lẻ khi logout
    public static void clearPlayerSession(int playerId) {
        activeGames.remove(playerId);
    }

    // --- CẤU TRÚC DỮ LIỆU ---

    public enum Suit {
        HEARTS("♥", "Cơ"), DIAMONDS("♦", "Rô"), CLUBS("♣", "Nhép"), SPADES("♠", "Bích");

        public final String symbol;
        public final String name;

        Suit(String s, String n) {
            this.symbol = s;
            this.name = n;
        }
    }

    public enum Rank {
        TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), SIX("6", 6),
        SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
        JACK("J", 10), QUEEN("Q", 10), KING("K", 10), ACE("A", 11);

        public final String label;
        public final int value;

        Rank(String l, int v) {
            this.label = l;
            this.value = v;
        }
    }

    public static class Card {
        public final Suit suit;
        public final Rank rank;
        private static final Card[] ALL_CARDS = new Card[52];

        static {
            int i = 0;
            for (Suit s : Suit.values()) {
                for (Rank r : Rank.values()) {
                    ALL_CARDS[i++] = new Card(s, r);
                }
            }
        }

        private Card(Suit s, Rank r) {
            this.suit = s;
            this.rank = r;
        }

        public static Card get(Suit s, Rank r) {
            return ALL_CARDS[s.ordinal() * 13 + r.ordinal()];
        }

        @Override
        public String toString() {
            return rank.label + suit.symbol;
        }
    }

    public static class Deck {
        private final List<Card> cards = new ArrayList<>();
        private final int numDecks;

        public Deck(int numDecks) {
            this.numDecks = numDecks;
            reset();
        }

        public void reset() {
            cards.clear();
            for (int i = 0; i < numDecks; i++) {
                for (Suit s : Suit.values()) {
                    for (Rank r : Rank.values()) {
                        cards.add(Card.get(s, r));
                    }
                }
            }
            Collections.shuffle(cards);
        }

        public Card draw() {
            if (cards.size() < (52 * numDecks * 0.25)) { // Tự động xáo bài khi còn 25%
                reset();
            }
            return cards.remove(0);
        }
    }

    public static class Hand {
        public final List<Card> cards = new ArrayList<>();

        public void add(Card c) {
            cards.add(c);
        }

        public int getValue() {
            int value = 0;
            int aces = 0;
            for (Card c : cards) {
                value += c.rank.value;
                if (c.rank == Rank.ACE)
                    aces++;
            }
            while (value > 21 && aces > 0) {
                value -= 10;
                aces--;
            }
            return value;
        }

        public boolean isBust() {
            return getValue() > 21;
        }

        public boolean isXiBang() {
            return cards.size() == 2 && cards.get(0).rank == Rank.ACE && cards.get(1).rank == Rank.ACE;
        }

        public boolean isXiDach() {
            if (cards.size() != 2)
                return false;
            boolean hasAce = cards.get(0).rank == Rank.ACE || cards.get(1).rank == Rank.ACE;
            boolean hasTenValue = cards.get(0).rank.value == 10 || cards.get(1).rank.value == 10;
            return hasAce && hasTenValue;
        }

        public boolean isNguLinh() {
            return cards.size() == 5 && getValue() <= 21;
        }

        public boolean isBlackjack() {
            return isXiDach() || isXiBang();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Card c : cards)
                sb.append(c).append(" ");
            return sb.toString().trim() + " (" + getValue() + ")";
        }
    }

    public enum GameState {
        BETTING, PLAYER_TURN, DEALER_TURN, SETTLED
    }

    public static class BlackjackGame {
        public final int playerId;
        public final String playerName;
        public long bet;
        public final Hand playerHand = new Hand();
        public final Hand dealerHand = new Hand();
        public final Deck deck = new Deck(6);
        public GameState state = GameState.BETTING;
        public long lastActive;
        public String resultMsg = "";

        public BlackjackGame(Player p) {
            this.playerId = p.id;
            this.playerName = p.name;
            this.lastActive = System.currentTimeMillis();
        }

        public void updateActive() {
            this.lastActive = System.currentTimeMillis();
        }

        public void start(long amount) throws IOException {
            updateActive();
            this.bet = amount;
            playerHand.cards.clear();
            dealerHand.cards.clear();

            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());

            state = GameState.PLAYER_TURN;

            // Kiểm tra thắng nhanh (Xì Bàng/Xì Dách) ngay khi khởi tạo
            if (playerHand.isBlackjack()) {
                state = GameState.DEALER_TURN;
                processDealer();
            }
        }

        public void hit() throws IOException {
            updateActive();
            playerHand.add(deck.draw());
            if (playerHand.isBust()) {
                settle(false, "Quá điểm (Bust)!");
            } else if (playerHand.isNguLinh()) {
                settleWin(1.0, "Ngũ Linh!");
            }
        }

        public void stand() throws IOException {
            updateActive();
            state = GameState.DEALER_TURN;
            processDealer();
        }

        public void doubleDown(Player p) throws IOException {
            updateActive();

            // FIX BUG LẠM DỤNG: Chỉ được gấp đôi khi trên tay có đúng 2 lá bài
            if (playerHand.cards.size() > 2) {
                Service.send_box_ThongBao_OK(p, "Chỉ được Gấp đôi ở lượt đầu tiên (khi có 2 lá)!");
                return;
            }

            if (p.get_vnd() < bet) {
                Service.send_box_ThongBao_OK(p, "Không đủ Extol để Gấp đôi!");
                return;
            }
            p.payVnd(bet);
            p.update_money();
            bet *= 2;
            playerHand.add(deck.draw());
            state = GameState.DEALER_TURN;
            processDealer();
        }

        private void processDealer() throws IOException {
            // FIX BUG FARM TIỀN: Kiểm tra ngay xem người chơi có bị Bust do rút lá Gấp đôi
            // không
            if (playerHand.isBust()) {
                settle(false, "Quá 21 điểm (Bust)!");
                return; // Dừng luôn, Dealer khỏi cần rút
            }

            // Dealer rút bài nếu dưới 17 điểm
            while (dealerHand.getValue() < 17 && dealerHand.cards.size() < 5) {
                dealerHand.add(deck.draw());
            }

            // Thứ tự ưu tiên: Xì Bàng > Xì Dách > Ngũ Linh > Điểm
            // 1. Kiểm tra Xì Bàng
            if (playerHand.isXiBang()) {
                if (dealerHand.isXiBang())
                    settlePush("Cùng Xì Bàng!");
                else
                    settleWin(2.0, "Xì Bàng!");
                return;
            }
            if (dealerHand.isXiBang()) {
                settle(false, "Dealer có Xì Bàng!");
                return;
            }

            // 2. Kiểm tra Xì Dách
            if (playerHand.isXiDach()) {
                if (dealerHand.isXiDach())
                    settlePush("Cùng Xì Dách!");
                else
                    settleWin(1.5, "Xì Dách!");
                return;
            }
            if (dealerHand.isXiDach()) {
                settle(false, "Dealer có Xì Dách!");
                return;
            }

            // 3. Kiểm tra Ngũ Linh
            if (playerHand.isNguLinh()) {
                if (dealerHand.isNguLinh()) {
                    if (dealerHand.getValue() < playerHand.getValue())
                        settle(false, "Dealer có Ngũ Linh điểm thấp hơn!");
                    else if (dealerHand.getValue() > playerHand.getValue())
                        settleWin(1.0, "Ngũ Linh điểm thấp hơn Dealer!");
                    else
                        settlePush("Cùng Ngũ Linh!");
                } else
                    settleWin(1.0, "Ngũ Linh!");
                return;
            }
            if (dealerHand.isNguLinh()) {
                settle(false, "Dealer có Ngũ Linh!");
                return;
            }

            int pVal = playerHand.getValue();
            int dVal = dealerHand.getValue();

            // 4. So sánh điểm bình thường
            if (dealerHand.isBust()) {
                settleWin(1.0, "Dealer Bust!");
            } else if (dVal > pVal) {
                settle(false, "Thua điểm Dealer!");
            } else if (dVal < pVal) {
                settleWin(1.0, "Thắng điểm Dealer!");
            } else {
                settlePush("Hòa điểm!");
            }
        }

        private void settleWin(double multiplier, String msg) throws IOException {
            long winAmount = (long) (bet * (1 + multiplier));
            PendingRewardManager.addReward(playerId, "blackjack", winAmount);
            resultMsg = "THẮNG: " + msg + " (+" + Util.number_format(winAmount - bet) + ")";
            addHistory(playerName + " thắng " + Util.number_format(winAmount - bet) + " (" + msg + ")");
            state = GameState.SETTLED;
        }

        private void settlePush(String msg) {
            PendingRewardManager.addReward(playerId, "blackjack", bet);
            resultMsg = "HÒA: " + msg;
            state = GameState.SETTLED;
        }

        private void settle(boolean win, String msg) throws IOException {
            if (win)
                settleWin(1.0, msg);
            else {
                resultMsg = "THUA: " + msg;
                addHistory(playerName + " thua " + Util.number_format(bet) + " (" + msg + ")");
                state = GameState.SETTLED;
            }
        }

        public void surrender() throws IOException {
            updateActive();

            // FIX BUG LẠM DỤNG: Chỉ được đầu hàng khi chưa rút thêm lá nào
            if (playerHand.cards.size() > 2) {
                // Trả lại menu lượt chơi cho người chơi, không cho đầu hàng
                resultMsg = "LỖI: Chỉ được đầu hàng ở lượt đầu tiên!";
                // state vẫn giữ là PLAYER_TURN để lát hiện lại menu Game
                return;
            }

            long refund = bet / 2;
            PendingRewardManager.addReward(playerId, "blackjack", refund);
            resultMsg = "ĐẦU HÀNG: Nhận lại 50% tiền cược.";
            state = GameState.SETTLED;
        }
    }

    // --- QUẢN LÝ TỔNG THỂ ---

    public static void init() {
        cleanupSessions(); // Dọn dẹp session cũ
        try {
            File f = new File(DATA_PATH);
            if (!f.exists())
                return;

            try (FileReader fr = new FileReader(f)) {
                JSONObject data = (JSONObject) JSONValue.parse(fr);
                if (data == null)
                    return;

                if (data.containsKey("jackpotSlot")) {
                    taixiu.SlotMachineManager.jackpotPool.set(((Number) data.get("jackpotSlot")).longValue());
                }

                JSONArray hArr = (JSONArray) data.get("history");
                if (hArr != null) {
                    synchronized (history) {
                        history.clear();
                        for (Object o : hArr)
                            history.add(o.toString());
                    }
                }

                JSONObject sObj = (JSONObject) data.get("stats");
                if (sObj != null) {
                    playerStats.clear();
                    for (Object key : sObj.keySet()) {
                        playerStats.put(key.toString(), ((Number) sObj.get(key)).longValue());
                    }
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] BlackjackManager.init error: " + e.getMessage());
        }

        // SỬA LẠI ĐOẠN NÀY: Gom cleanupSessions vào chạy tự động mỗi 5 phút
        if (saveTask == null || saveTask.isShutdown()) {
            saveTask = Executors.newSingleThreadScheduledExecutor();
            saveTask.scheduleAtFixedRate(() -> {
                cleanupSessions(); // Tự động dọn rác
                saveData(); // Sau đó mới lưu data
            }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
        }
    }

    public static void cleanupSessions() {
        long limit = System.currentTimeMillis() - 1800000; // 30 phút
        activeGames.entrySet().removeIf(entry -> {
            BlackjackGame game = entry.getValue();
            if (game.state != GameState.SETTLED && game.lastActive < limit) {
                // FIX BUG: Xử thua mất tiền nếu cố tình treo game (bỏ hoàn tiền)
                addHistory(game.playerName + " bị xử thua " + Util.number_format(game.bet) + " (Treo game)");
                return true; // Xóa game khỏi bộ nhớ
            }
            return game.state == GameState.SETTLED;
        });
    }

    @SuppressWarnings("unchecked")
    private static void saveData() {
        try {
            JSONObject obj = new JSONObject();
            JSONArray historyArray = new JSONArray();
            synchronized (history) {
                historyArray.addAll(history);
            }
            obj.put("history", historyArray);

            JSONObject statsObj = new JSONObject();
            statsObj.putAll(playerStats);
            obj.put("stats", statsObj);

            obj.put("jackpotSlot", taixiu.SlotMachineManager.jackpotPool.get());

            try (FileWriter fw = new FileWriter(DATA_PATH)) {
                fw.write(obj.toJSONString());
            }
        } catch (IOException e) {
            core.GameLogger.error("[Disconnect] BlackjackManager.saveData error: " + e.getMessage());
        }
    }

    public static void stop() {
        for (BlackjackGame game : activeGames.values()) {
            if (game.state != GameState.SETTLED && game.bet > 0) {
                PendingRewardManager.addReward(game.playerId, "blackjack", game.bet);
            }
        }
        activeGames.clear();
        saveData();
    }

    public static void showMainMenu(Player p) throws IOException {
        String msg = "CHÀO MỪNG ĐẾN VỚI SÒNG BÀI BLACKJACK\n"
                + "- Extol hiện tại: " + Util.number_format(p.get_vnd()) + "\n"
                + "- Trải nghiệm Xì Dách đỉnh cao cùng NPC Law!";

        Service.send_box_yesno(p, YESNO_MAIN_MENU, "Blackjack", msg,
                new String[] { "Chơi mới", "Tất tay", "Lịch sử", "Thoát" }, new byte[] { 0, 1, 2, 3 });
    }

    public static void handleYesNo(Player p, byte index, int id) throws IOException {
        cleanupSessions(); // Tự động dọn dẹp khi có action mới
        if (id == YESNO_MAIN_MENU) {
            if (index == 0) { // Chơi mới
                if (activeGames.containsKey(p.id)) {
                    showGameScreen(p, activeGames.get(p.id));
                } else {
                    Service.input_text(p, INPUT_BET, "Đặt cược", new String[] { "Nhập số Extol muốn cược:" });
                }
            } else if (index == 1) { // Tất tay (All-in)
                if (activeGames.containsKey(p.id)) {
                    showGameScreen(p, activeGames.get(p.id));
                } else {
                    long amount = p.get_vnd();
                    if (amount < 1000) {
                        Service.send_box_ThongBao_OK(p, "Cần tối thiểu 1.000 Extol để chơi!");
                        return;
                    }
                    if (p.payVnd(amount)) {
                        p.update_money();
                        BlackjackGame game = new BlackjackGame(p);
                        game.start(amount);
                        activeGames.put(p.id, game);
                        showGameScreen(p, game);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Không đủ Extol để Tất tay!");
                    }
                }
            } else if (index == 2) { // Lịch sử
                StringBuilder sb = new StringBuilder("LỊCH SỬ GẦN ĐÂY:\n");
                for (int i = Math.max(0, history.size() - 10); i < history.size(); i++) {
                    sb.append(history.get(i)).append("\n");
                }
                Service.send_box_ThongBao_OK(p, sb.toString());
            }
        } else if (id == YESNO_GAME_ACTION) {
            BlackjackGame game = activeGames.get(p.id);
            if (game == null)
                return;

            if (game.state == GameState.SETTLED) {
                activeGames.remove(p.id);
                showMainMenu(p);
                return;
            }

            switch (index) {
                case 0:
                    game.hit();
                    break;
                case 1:
                    game.stand();
                    break;
                case 2:
                    game.doubleDown(p);
                    break;
                case 3:
                    game.surrender();
                    break;
            }
            showGameScreen(p, game);
        }
    }

    public static void handleInput(Player p, String text) throws IOException {
        try {
            // FIX BUG SPAM: Chặn đặt cược nếu ván cũ chưa kết thúc
            if (activeGames.containsKey(p.id)) {
                Service.send_box_ThongBao_OK(p, "Bạn đang có ván bài chưa kết thúc!");
                showGameScreen(p, activeGames.get(p.id));
                return;
            }

            long amount = Long.parseLong(text);
            if (amount < 1000) {
                Service.send_box_ThongBao_OK(p, "Cược tối thiểu 1.000 Extol!");
                return;
            }
            if (amount > p.get_vnd() * 0.3) {
                Service.send_box_ThongBao_OK(p, "Chỉ được cược tối đa 30% tài sản ("
                        + Util.number_format((long) (p.get_vnd() * 0.3)) + " Extol)!");
                return;
            }

            if (p.payVnd(amount)) {
                p.update_money();
                BlackjackGame game = new BlackjackGame(p);
                game.start(amount);
                activeGames.put(p.id, game);
                showGameScreen(p, game);
            } else {
                Service.send_box_ThongBao_OK(p, "Không đủ Extol!");
            }
        } catch (Exception e) {
            Service.send_box_ThongBao_OK(p, "Số tiền không hợp lệ!");
        }
    }

    public static void showGameScreen(Player p, BlackjackGame game) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--- BLACKJACK SESSION ---\n");
        sb.append("Dealer: ");
        if (game.state == GameState.PLAYER_TURN) {
            sb.append(game.dealerHand.cards.get(0)).append(" [?]");
        } else {
            sb.append(game.dealerHand.toString());
        }
        sb.append("\nBạn: ").append(game.playerHand.toString());
        sb.append("\nCược: ").append(Util.number_format(game.bet)).append(" Extol");

        if (game.state == GameState.SETTLED) {
            sb.append("\n\n").append(game.resultMsg);
            Service.send_box_yesno(p, YESNO_GAME_ACTION, "Kết quả", sb.toString(),
                    new String[] { "Trở về" }, new byte[] { 0 });
        } else {
            Service.send_box_yesno(p, YESNO_GAME_ACTION, "Lượt của bạn", sb.toString(),
                    new String[] { "Rút (Hit)", "Dừng (Stand)", "Gấp đôi", "Đầu hàng" },
                    new byte[] { 0, 1, 2, 3 });
        }
    }

    private static void addHistory(String entry) {
        synchronized (history) {
            history.add("[" + new SimpleDateFormat("dd/MM HH:mm").format(new Date()) + "] " + entry);
            if (history.size() > 50)
                history.remove(0);
        }
    }
}
