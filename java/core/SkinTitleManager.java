package core;

import client.DanhHieu;
import client.Player;
import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý logic Skin thông qua Danh hiệu (Title).
 * Khi một nhân vật có skinId khác -1, họ sẽ được hiển thị bằng Danh hiệu
 * thay vì mô hình nhân vật gốc.
 * Đã tối ưu hóa lưu trữ O(1) cache.
 */
public class SkinTitleManager {
    
    // CÔNG TẮC BẬT/TẮT TÍNH NĂNG SKIN
    public static boolean ENABLE_SKIN = false; 

    public static class SkinRecord {
        public int idleTitleId;
        public int attackTitleId;
        public int moveTitleId;

        // Cached values for O(1) lookup
        public int idleEffId = -1;
        public int attackEffId = -1;
        public int moveEffId = -1;

        public SkinRecord(int idle, int attack, int move) {
            this.idleTitleId = idle;
            this.attackTitleId = attack;
            this.moveTitleId = move;
        }
    }

    // Map tĩnh lưu trữ cấu hình Skin: skinId -> SkinRecord
    private static final Map<Short, SkinRecord> SKIN_MAP = new HashMap<>();

    // Thời gian duy trì trạng thái tấn công/di chuyển (ms)
    private static final long ATTACK_DURATION = 1000;
    private static final long MOVE_DURATION = 50;

    static {
        // Cấu hình mẫu: Skin ID 1503
        // idleTitleId: 1503, attackTitleId: 1503, moveTitleId: 1505
        SKIN_MAP.put((short) 1503, new SkinRecord(1503, 1504, 1505));
    }

    /**
     * Khởi tạo cache (gọi sau khi đã load dữ liệu DanhHieu xong).
     */
    public static void init() {
        for (SkinRecord record : SKIN_MAP.values()) {
            DanhHieu idleTemp = DanhHieu.getTemplate(record.idleTitleId);
            record.idleEffId = (idleTemp != null) ? idleTemp.eff : record.idleTitleId;

            DanhHieu attackTemp = DanhHieu.getTemplate(record.attackTitleId);
            record.attackEffId = (attackTemp != null) ? attackTemp.eff : record.attackTitleId;

            DanhHieu moveTemp = DanhHieu.getTemplate(record.moveTitleId);
            record.moveEffId = (moveTemp != null) ? moveTemp.eff : record.moveTitleId;

            System.out.println("SkinTitleManager Loaded Cache: IdleEff=" + record.idleEffId + " AttackEff="
                    + record.attackEffId + " MoveEff=" + record.moveEffId);
        }
    }

    /**
     * Kích hoạt trạng thái tấn công cho Skin.
     */
    public static void onAttack(Player p) {
        if (!ENABLE_SKIN) return;
        if (p != null && p.skinId != -1) {
            p.lastAttackTime = System.currentTimeMillis();
        }
    }

    /**
     * Lấy ID hiệu ứng (eff) cần hiển thị dựa trên trạng thái hiện tại của Skin.
     * Sử dụng O(1) lookup sau khi đã init cache.
     * Thứ tự ưu tiên: Attack > Move > Idle.
     */
    public static int getActiveSkinEffId(Player p) {
        if (!ENABLE_SKIN || p == null || p.skinId == -1) {
            return -1;
        }

        SkinRecord record = SKIN_MAP.get(p.skinId);
        if (record == null) {
            return -1;
        }

        long now = System.currentTimeMillis();
        if (now - p.lastAttackTime < ATTACK_DURATION) {
            return record.attackEffId;
        }
        if (now - p.lastMoveTime < MOVE_DURATION) {
            return record.moveEffId;
        }
        return record.idleEffId;
    }

    /**
     * Kiểm tra xem một Danh hiệu có đang được sử dụng bởi hệ thống Skin hay không.
     */
    public static boolean isSkinTitle(Player p, int titleId) {
        if (!ENABLE_SKIN || p == null || p.skinId == -1) {
            return false;
        }
        SkinRecord record = SKIN_MAP.get(p.skinId);
        if (record == null) {
            return false;
        }
        return record.idleTitleId == titleId || record.attackTitleId == titleId || record.moveTitleId == titleId;
    }
}
