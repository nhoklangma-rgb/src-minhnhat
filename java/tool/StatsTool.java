package tool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import template.Skill_info;

public class StatsTool {

    // =================================================================================================
    // STAT ID REFERENCE (dùng cho POINTX_EXTRA config)
    // =================================================================================================
    // ID | Tên chỉ số | Ghi chú
    // ---|------------|--------------------
    // 1 | % Sát thương (ATK%) | Từ Sức mạnh
    // 2 | Sát thương AP |
    // 3 | Phòng thủ (flat) |
    // 4 | % Phòng thủ (DEF%) | Từ Phòng thủ
    // 5 | Điểm cộng Sức mạnh | point_plus
    // 6 | Điểm cộng Phòng thủ | point_plus
    // 7 | Điểm cộng Thể lực | point_plus
    // 8 | Điểm cộng Tinh thần | point_plus
    // 9 | Điểm cộng Nhanh nhẹn | point_plus
    // 10 | % Chí mạng (Crit) | Từ Sức mạnh
    // 11 | % Sát thương chí mạng | Từ Tinh thần
    // 12 | % Né tránh (Miss) | Từ Nhanh nhẹn
    // 13 | % Xuyên giáp (Pierce) | Từ Sức mạnh
    // 14 | % Phản sát thương | Từ Tinh thần (dùng bảng resist_magic)
    // 15 | HP cộng thêm (flat) | Từ Thể lực
    // 16 | MP cộng thêm (flat) | Từ Tinh thần
    // 17 | % HP tăng thêm |
    // 18 | % MP tăng thêm |
    // 19 | Tự hồi HP |
    // 20 | Tự hồi MP |
    // 21 | Hút HP khi đánh |
    // 22 | Hút MP khi đánh |
    // 23 | % Hiệu quả thuốc HP | Từ Thể lực
    // 24 | % Hiệu quả thuốc MP |
    // 25 | Nhanh nhẹn (Agility) | Từ Nhanh nhẹn
    // 26 | Kháng sát thương vật lý | Từ Phòng thủ
    // 27 | Kháng sát thương phép | Từ Phòng thủ
    // 46 | % Sát thương cuối cùng |
    // 47 | Giảm hiệu ứng |
    // 48 | % ST theo HP mục tiêu | Max 350
    // 49 | Giảm chí mạng đối thủ |
    // 50 | Giảm xuyên giáp đối thủ |
    // 51 | Giảm né tránh đối thủ |
    // 52 | Giảm phản dame đối thủ |
    // 53 | Bỏ qua sát thương |
    // 55 | Từ chối tử thần |
    // 56 | % HP cuối (final HP%) | Nhân cuối cùng
    // 57 | Sát thương thực |
    // 59 | Hấp thụ HP |
    // 63 | Giảm bỏ qua sát thương |
    // 67 | % EXP thêm |
    // 68 | % EXP skill thêm |
    // 69 | Giảm nhân sát thương |
    // 70 | Giảm DEF mục tiêu |
    // 72 | % Beri thêm khi train |
    // =================================================================================================

    // =================================================================================================
    // CONFIGURATION AREA
    // =================================================================================================

    /**
     * Skill Damage Multiplier (Global).
     * Example: 1.0f = Normal, 0.1f = 10% Damage
     */
    public static float getGlobalDamageCoefficient() {
        return Skill_info.GLOBAL_DAME_RATE;
    }

    /**
     * Mob HP Multiplier (Global).
     * Example: 1.0f = Normal, 0.1f = 10% HP
     */
    public static float getGlobalMobHpCoefficient() {
        return Skill_info.GLOBAL_MOB_HP_RATE;
    }

    /**
     * EXP Multiplier (Global).
     * Example: 1.0f = Normal, 2.0f = Double EXP
     */
    public static float getGlobalExpCoefficient() {
        return Skill_info.GLOBAL_EXP_RATE;
    }

    /**
     * Gold (Beri) Drop Multiplier (Global).
     * Example: 1.0f = Normal, 2.0f = Double Gold
     */
    public static float getGlobalGoldCoefficient() {
        return Skill_info.GLOBAL_GOLD_RATE;
    }

    /**
     * Item Drop Rate Multiplier (Global).
     * Example: 1.0f = Normal, 2.0f = Double Items (loops drop logic)
     */
    public static float getGlobalDropRateCoefficient() {
        return Skill_info.GLOBAL_DROP_RATE;
    }

    /**
     * Skill Mana Cost Multiplier.
     * Example: 1.0f = Normal, 0.5f = Half Mana Cost
     */
    public static float getGlobalManaCostCoefficient() {
        return Skill_info.GLOBAL_MANA_RATE;
    }

    /**
     * Skill Cooldown Multiplier.
     * Example: 1.0f = Normal, 0.0f = No Cooldown
     */
    public static float getGlobalCoolDownCoefficient() {
        return Skill_info.GLOBAL_COOLDOWN_RATE;
    }

    /**
     * Monster Respawn Time (seconds).
     * Example: 7 = Default, 1 = Very fast
     */
    public static int getGlobalMobRespawnTime() {
        return Skill_info.GLOBAL_MOB_RESPAWN_TIME;
    }

    /**
     * Potential Points per Level.
     * Example: 2 = Default, 5 = 5 points per level
     */
    public static int getPotentialPerLevel() {
        return Skill_info.POTENTIAL_PER_LEVEL;
    }

    /**
     * Max points per stat attribute.
     * Example: 80 = Default, 200 = Allow up to 200 per stat
     */
    public static int getStatPointMax() {
        return Skill_info.STAT_POINT_MAX;
    }

    /**
     * ATK Scaling Rate (from potential points).
     * Now loaded from htth.conf
     */
    public static float getAtkScaling() {
        return Skill_info.ATK_SCALING;
    }

    /**
     * DEF Scaling Rate (from potential points).
     * Now loaded from htth.conf
     */
    public static float getDefScaling() {
        return Skill_info.DEF_SCALING;
    }

    /**
     * HP Scaling Rate (from potential points).
     * Now loaded from htth.conf
     */
    public static float getHpScaling() {
        return Skill_info.HP_SCALING;
    }

    /**
     * MP Scaling Rate (from potential points).
     * Now loaded from htth.conf
     */
    public static float getMpScaling() {
        return Skill_info.MP_SCALING;
    }

    // =================================================================================================
    // EXTRA BONUS PER POINT COLUMN
    // Mỗi entry = {stat_id, giá_trị_mỗi_điểm}
    // Ví dụ: {{56, 1.0f}} = mỗi điểm cộng thêm 1 (‰) vào stat 56 (% hp cuối)
    // Nhiều bonus: {{56, 1.0f}, {18, 5.0f}}
    // Để trống: {} = không bonus thêm
    // =================================================================================================

    /** Sức mạnh — Extra bonus */
    public static float[][] getPoint1Extra() {
        return new float[][] {};
    }

    /** Phòng thủ — Extra bonus */
    public static float[][] getPoint2Extra() {
        return new float[][] {};
    }

    /** Thể lực — Extra bonus */
    public static float[][] getPoint3Extra() {
        return new float[][] {};
    }

    /** Tinh thần — Extra bonus */
    public static float[][] getPoint4Extra() {
        return Skill_info.POINT4_EXTRA;
    }

    /** Nhanh nhẹn — Extra bonus */
    public static float[][] getPoint5Extra() {
        return new float[][] {};
    }

    /** Helper: convert float[][] to Java source string */
    private static String toArraySource(float[][] arr) {
        if (arr.length == 0)
            return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("{").append((int) arr[i][0]).append(", ").append(arr[i][1]).append("f}");
        }
        sb.append("}");
        return sb.toString();
    }

    // =================================================================================================
    // END
    // =================================================================================================

    public static void main(String[] args) {
        core.GameLogger.info("Initializing StatsTool...");
        core.GameLogger.info("--------------------------------------------------");
        core.GameLogger.info("Dame Rate:      " + getGlobalDamageCoefficient());
        core.GameLogger.info("Mob HP Rate:    " + getGlobalMobHpCoefficient());
        core.GameLogger.info("EXP Rate:       " + getGlobalExpCoefficient());
        core.GameLogger.info("Gold Rate:      " + getGlobalGoldCoefficient());
        core.GameLogger.info("Drop Rate:      " + getGlobalDropRateCoefficient());
        core.GameLogger.info("Mana Rate:      " + getGlobalManaCostCoefficient());
        core.GameLogger.info("Cooldown Rate:  " + getGlobalCoolDownCoefficient());
        core.GameLogger.info("Respawn Time:   " + getGlobalMobRespawnTime() + "s");
        core.GameLogger.info("Potential/Lv:   " + getPotentialPerLevel());
        core.GameLogger.info("Stat Max:       " + getStatPointMax());
        core.GameLogger.info("--- Extra Bonus per Point ---");
        core.GameLogger.info("  Sức mạnh:     " + toArraySource(getPoint1Extra()));
        core.GameLogger.info("  Phòng thủ:    " + toArraySource(getPoint2Extra()));
        core.GameLogger.info("  Thể lực:      " + toArraySource(getPoint3Extra()));
        core.GameLogger.info("  Tinh thần:    " + toArraySource(getPoint4Extra()));
        core.GameLogger.info("  Nhanh nhẹn:   " + toArraySource(getPoint5Extra()));
        core.GameLogger.info("--- Potential Scaling ---");
        core.GameLogger.info("  ATK Scaling:   " + getAtkScaling());
        core.GameLogger.info("  DEF Scaling:   " + getDefScaling());
        core.GameLogger.info("  HP Scaling:    " + getHpScaling());
        core.GameLogger.info("  MP Scaling:    " + getMpScaling());
        core.GameLogger.info("--------------------------------------------------");

        boolean autoConfirm = args.length > 0 && args[0].equals("-y");
        if (autoConfirm) {
            applyConfig();
            core.GameLogger.info("Configuration applied successfully (Auto-confirmed)!");
            core.GameLogger.info("Please Re-Compile and Restart the Server.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        core.GameLogger.info("Apply changes to source? (y/n)");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            applyConfig();
        }
        scanner.close();
    }

    private static void applyConfig() {
        updateManager();
        updateMap();
        updateLeaveItemMap();
        core.GameLogger.info("DONE. Please Re-Compile and Restart the Server.");
    }

    private static File getFile(String path) {
        File file = new File(path);
        if (!file.exists())
            file = new File("../" + path);
        if (!file.exists())
            file = new File("HTTH_DROv3/" + path);
        return file;
    }

    private static void updateManager() {
        File file = getFile("src/main/java/core/Manager.java");
        if (!file.exists())
            return;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("temp.hp_max = Long.parseLong") && !line.contains("GLOBAL_MOB_HP_RATE")) {
                    newLines.add("                // Injected by StatsTool");
                    newLines.add(
                            "                temp.hp_max = (long)(Long.parseLong(rs.getString(\"hp\")) * Skill_info.GLOBAL_MOB_HP_RATE);");
                } else if (line.contains("rs.getByte(\"typeDevil\"));")) {
                    newLines.add(line);
                    // Check if subsequent lines already have the injection
                    boolean alreadyInjected = false;
                    for (int j = 1; j <= 5 && (i + j) < lines.size(); j++) {
                        if (lines.get(i + j).contains("GLOBAL_MANA_RATE")) {
                            alreadyInjected = true;
                            break;
                        }
                    }
                    if (!alreadyInjected) {
                        newLines.add("                // Injected by StatsTool");
                        newLines.add(
                                "                temp_add.manaLost = (short)(temp_add.manaLost * Skill_info.GLOBAL_MANA_RATE);");
                        newLines.add(
                                "                temp_add.timeDelay = (int)(temp_add.timeDelay * Skill_info.GLOBAL_COOLDOWN_RATE);");
                    }
                } else {
                    newLines.add(line);
                }
            }
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            core.GameLogger.info("[OK] Manager.java");
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] StatsTool.updateManager error: " + e.getMessage());
        }
    }

    private static void updateMap() {
        File file = getFile("src/main/java/map/Map.java");
        if (!file.exists())
            return;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            for (String line : lines) {
                if (line.contains("exp_up_add = 20 + (dame_exp / 100);") && !line.contains("GLOBAL_EXP_RATE")) {
                    newLines.add("                // Injected by StatsTool");
                    newLines.add(
                            "                long exp_up_add = (long)((20 + (dame_exp / 100)) * Skill_info.GLOBAL_EXP_RATE);");
                } else
                    newLines.add(line);
            }
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            core.GameLogger.info("[OK] Map.java");
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] StatsTool.updateMap error: " + e.getMessage());
        }
    }

    private static void updateLeaveItemMap() {
        File file = getFile("src/main/java/map/LeaveItemMap.java");
        if (!file.exists())
            return;
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();

            for (String line : lines) {
                if (line.contains("vang_random = (vang_random * (1000") && !line.contains("GLOBAL_GOLD_RATE")) {
                    newLines.add("            // Injected by StatsTool");
                    newLines.add(
                            "            vang_random = (long)((vang_random * (1000 + p.body.get_percent_beri_train())) / 1000 * Skill_info.GLOBAL_GOLD_RATE);");
                } else if (line.contains("public static void leave_item3")
                        && !content.contains("leave_item3_internal")) {
                    newLines.add(
                            "    public static void leave_item3(Map map, Mob mob_target, Player p) throws IOException { // Injected by StatsTool");
                    newLines.add("        int nDrop = (int)Skill_info.GLOBAL_DROP_RATE;");
                    newLines.add(
                            "        if (core.Util.random(100) < (Skill_info.GLOBAL_DROP_RATE - nDrop) * 100) nDrop++;");
                    newLines.add("        for (int k=0; k<nDrop; k++) leave_item3_internal(map, mob_target, p);");
                    newLines.add("    }");
                    newLines.add("");
                    newLines.add(
                            "    private static void leave_item3_internal(Map map, Mob mob_target, Player p) throws IOException {");
                } else if (line.contains("public static void leave_item7")
                        && !content.contains("leave_item7_internal")) {
                    newLines.add(
                            "    public static void leave_item7(Map map, Mob mob_target, Player p) throws IOException { // Injected by StatsTool");
                    newLines.add("        int nDrop = (int)Skill_info.GLOBAL_DROP_RATE;");
                    newLines.add(
                            "        if (core.Util.random(100) < (Skill_info.GLOBAL_DROP_RATE - nDrop) * 100) nDrop++;");
                    newLines.add("        for (int k=0; k<nDrop; k++) leave_item7_internal(map, mob_target, p);");
                    newLines.add("    }");
                    newLines.add("");
                    newLines.add(
                            "    private static void leave_item7_internal(Map map, Mob mob_target, Player p) throws IOException {");
                } else {
                    newLines.add(line);
                }
            }
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
            core.GameLogger.info("[OK] LeaveItemMap.java");
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] StatsTool.updateLeaveItemMap error: " + e.getMessage());
        }
    }
}
