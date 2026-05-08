package core;

import client.Player;
import map.Map;

/**
 * BalanceGateway - Trung tâm điều phối cân bằng chỉ số và sát thương.
 * Thiết kế theo kiến trúc "Chokepoint" để quản lý tập trung Caps và PvP Scaling.
 */
public class BalanceGateway {

    /**
     * Cổng 1: Kiểm soát giới hạn chỉ số (Stat Gate)
     * Được gọi tại cuối hàm Body.total_param_item()
     * 
     * @param p Đối tượng người chơi sở hữu chỉ số
     * @param id Option ID của chỉ số (vd: 1 cho ATK%)
     * @param value Giá trị đã cộng dồn từ mọi nguồn
     * @return Giá trị sau khi áp dụng Cap
     */
    public static long applyStatLimit(Player p, int id, long value) {
        // Đệ tử không bị giới hạn bởi Cổng 1 để đảm bảo sức mạnh vượt trội cho AI
        if (p == null || p.isDisciple()) {
            return value;
        }

        // TỐI ƯU O(1): Truy cập trực tiếp mảng statCaps và statRates đã nạp sẵn
        if (id >= 0 && id < 200) {
            // 1. Áp dụng Hệ số % (Rate Scaling) - Mặc định 100
            int rate = Manager.gI().statRates[id];
            if (rate != 100) {
                value = (value * rate) / 100;
            }

            // 2. Áp dụng Soft Cap Đa tầng (Diminishing Returns)
            java.util.List<Manager.SoftCapStage> stages = Manager.gI().statSoftCaps[id];
            if (stages != null && !stages.isEmpty()) {
                double currentVal = (double) value;
                for (Manager.SoftCapStage stage : stages) {
                    if (currentVal > stage.threshold) {
                        // Công thức Logarit: Result = T + S * ln(1 + (v - T) / S)
                        currentVal = stage.threshold + stage.factor * Math.log(1.0 + (currentVal - stage.threshold) / stage.factor);
                    }
                }
                value = (long) currentVal;
            }

            // 3. Kiểm tra Chặn cứng (Cap Limit) - Mặc định -1
            int cap = Manager.gI().statCaps[id];
            if (cap != -1 && value > cap) {
                return cap;
            }
        }

        return value;
    }

    /**
     * Cổng 2: Kiểm soát sát thương cuối (Combat Gate)
     * Được gọi tại Map.java trước khi thực hiện update_hp
     * 
     * @param attacker Người tấn công
     * @param target Đối tượng bị tấn công
     * @param damage Sát thương dự kiến ban đầu
     * @return Sát thương sau khi cân bằng
     */
    public static long applyCombatScaling(Object attacker, Object target, long damage) {
        // 1. Loại biên: Nếu không có sát thương hoặc đệ tử tham gia, giữ nguyên
        if (damage <= 0) return damage;
        
        boolean isAttackerPlayer = (attacker instanceof Player) && !((Player) attacker).isDisciple();
        boolean isTargetPlayer = (target instanceof Player) && !((Player) target).isDisciple();

        // 2. Xử lý PvP (Người chơi thực vs Người chơi thực)
        if (isAttackerPlayer && isTargetPlayer) {
            // TỐI ƯU: Sử dụng pvpRate đã nạp sẵn
            float pvpRate = Manager.gI().pvpRate;
            long finalDamage = (long) (damage * pvpRate);
            
            // Đảm bảo dame tối thiểu là 1 nếu sát thương gốc > 0
            return (finalDamage <= 0) ? 1 : finalDamage;
        }

        // 3. Xử lý PvE hoặc các trường hợp liên quan đến Đệ Tử
        return damage;
    }
}
