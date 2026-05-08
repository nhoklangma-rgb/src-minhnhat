package tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.Manager;
import database.SQL;
import template.Skill_Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.Option;
import template.Skill_info;

public class SkillTool {

    // =================================================================================================
    // CONFIGURATION AREA - EDIT THESE METHODS TO CHANGE SKILL FORMULAS
    // =================================================================================================

    /**
     * Maximum Level for Skills (N).
     * Default: 1000
     */
    public static int getMaxSkillLevel() {
        return Skill_info.MAX_LEVEL;
    }

    /**
     * Danh sách id_2 của skill ác quỷ bị loại khỏi generate.
     */
    private static final Set<Integer> DEVIL_SKILL_BLACKLIST = new HashSet<>(Arrays.asList(
            2059));

    /**
     * Maximum Level for Devil Fruit Skills.
     * Default: 100
     */
    public static int getMaxDevilSkillLevel() {
        return 100;
    }

    /**
     * Skill Experience Gain Coefficient (Multiplier).
     * This multiplies the EXP received when hitting monsters.
     * Default: 1.0 (Normal rate)
     */
    public static float getSkillExpCoefficient() {
        return Skill_info.SKILL_EXP_RATE;
    }

    /**
     * Calculate Damage for a specific level.
     */
    private static int calculateSkillDamage(int skillId, int level, int previousDamage) {
        if (skillId >= 2003 && skillId <= 2063) {
            // Devil Fruit damage scaling: +1% per level
            return (int) (previousDamage * 1.01);
        }
        // Default scaling: +0.5% per level
        return (int) (previousDamage * 1.005);
    }

    /**
     * Calculate Mana Cost for a specific level.
     */
    private static short calculateSkillMana(int skillId, int level, int previousMana) {
        // Default: +5 mana per level
        return (short) (previousMana + 5);
    }

    /**
     * Calculate Cooldown for a specific level.
     */
    private static int calculateSkillCoolDown(int skillId, int level, int previousCoolDown) {
        // Default: No change
        return previousCoolDown;
    }

    /**
     * Calculate Effect (Animation ID) for a specific level.
     * Use -1 to keep the base effect (from Level 30).
     */
    private static short calculateSkillEffect(int skillId, int level, short baseEffectId) {
        return -1;
    }

    /**
     * Calculate Options (Passives/Bonuses) for a specific level.
     */
    private static List<Option> calculateSkillOptions(int skillId, int level, List<Option> baseOptions) {
        List<Option> newOptions = new ArrayList<Option>();
        if (baseOptions == null)
            return newOptions;

        for (Option o : baseOptions) {
            int newValue = o.getParam();
            newOptions.add(new Option(o.id, newValue));
        }
        return newOptions;
    }

    /**
     * CONFIGURATION: The Java Code String for EXP Calculation formula.
     */
    private static String getExpFormulaString() {
        return "if (Skill_info.EXP[i - 1] > Long.MAX_VALUE / 102) { Skill_info.EXP[i] = Skill_info.EXP[i - 1]; } else { long nextExp = (Skill_info.EXP[i - 1] * 102) / 100; Skill_info.EXP[i] = nextExp; }";
    }

    /**
     * Calculate EXP for Devil Fruit Levels (Index 0 to 4 correspond to Lv 1-5).
     */
    private static int calculateExpDevil(int levelIndex, int previousValue) {
        int[] defaultValues = { 100, 125, 167, 200, 250 };
        if (levelIndex >= 0 && levelIndex < defaultValues.length) {
            return defaultValues[levelIndex];
        }
        return 250;
    }

    /**
     * Define custom animation IDs for specific skills and levels.
     * Trả về -1 để giữ nguyên effect hiện tại.
     */
    private static short getCustomEff(int skillId, int level, short currentEff) {
        return -1;
    }

    /**
     * Lấy max id_index từ DB chỉ của các skill Lv1-30.
     */
    private static int getMaxIdIndex() {
        try (Connection conn = SQL.gI().getCon();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT MAX(id_index) FROM `skill` WHERE `Lv_RQ` <= 30 AND `id_index` IS NOT NULL");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            core.GameLogger.error("[Disconnect] SkillTool.getMaxIdIndex error: " + e.getMessage());
        }
        return 870; // fallback
    }

    // =================================================================================================
    // END OF CONFIGURATION AREA
    // =================================================================================================

    public static void main(String[] args) {
        core.GameLogger.info("Initializing SkillTool...");
        Manager.gI().init();

        if (SQL.gI().getCon() != null) {
            generateSkillsLv31toMax();
            printExpTable();
            core.GameLogger.info("Values have been generated and inserted.");
        } else {
            core.GameLogger.error("Failed to connect to database!");
        }
    }

    private static void loadSkillsFromDB() {
        core.GameLogger.info("Loading existing skills from database...");
        Skill_Template.ENTRYS = new ArrayList<>();
        try (Connection conn = SQL.gI().getCon();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM `skill` ORDER BY `id_index`");
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Skill_Template temp_add = new Skill_Template(
                        rs.getInt("id_index"),
                        rs.getInt("id_2"),
                        rs.getShort("icon"),
                        rs.getByte("typeSkill"),
                        rs.getByte("typeBuff"),
                        rs.getString("name"),
                        rs.getShort("typeEffSkill"),
                        rs.getShort("range"));

                temp_add.getData(
                        rs.getByte("nTarget"),
                        rs.getShort("rangeLan"),
                        rs.getInt("damage"),
                        rs.getShort("manaLost"),
                        rs.getInt("timeDelay"),
                        rs.getByte("nKick"),
                        rs.getString("info"),
                        rs.getShort("Lv_RQ"),
                        rs.getByte("typeDevil"));

                temp_add.op = new ArrayList<>();
                JSONArray js = (JSONArray) JSONValue.parse(rs.getString("option"));
                if (js != null) {
                    for (int j = 0; j < js.size(); j++) {
                        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(j).toString());
                        temp_add.op.add(new Option(Byte.parseByte(js2.get(0).toString()),
                                Integer.parseInt(js2.get(1).toString())));
                    }
                }

                js = (JSONArray) JSONValue.parse(rs.getString("EffSpec"));
                if (js != null) {
                    temp_add.idEffSpec = Byte.parseByte(js.get(0).toString());
                    temp_add.perEffSpec = Short.parseShort(js.get(1).toString());
                    temp_add.timeEffSpec = Short.parseShort(js.get(2).toString());
                }

                Skill_Template.ENTRYS.add(temp_add);
            }
            core.GameLogger.info("Loaded " + Skill_Template.ENTRYS.size() + " skills.");
        } catch (Exception e) {
            core.GameLogger.error("[Disconnect] SkillTool.loadSkillsFromDB error: " + e.getMessage());
        }
    }

    public static void generateSkillsLv31toMax() {
        int MAX_LEVEL = getMaxSkillLevel();
        core.GameLogger.info("Starting generation of skills level 31 to " + MAX_LEVEL + "...");

        // 0. AUTOMATIC SCHEMA UPDATE
        try (Connection conn = SQL.gI().getCon()) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE `skill` MODIFY COLUMN `id_index` INT")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE `skill` MODIFY COLUMN `Lv_RQ` SMALLINT")) {
                ps.executeUpdate();
            }
            core.GameLogger.info("Database Schema Updated: id_index=INT, Lv_RQ=SMALLINT.");
        } catch (SQLException e) {
            core.GameLogger.error("Note: Schema upgrade check completed (maybe already updated).");
        }

        // 1. CLEANUP: Xóa skill lv > 30 thuộc cả 2 nhóm id_2
        try (Connection conn = SQL.gI().getCon()) {
            // Xóa nhóm id_2 <= 2 (skill thường)
            try (PreparedStatement psDelete = conn
                    .prepareStatement("DELETE FROM `skill` WHERE `Lv_RQ` > 30 AND id_2 <= 2")) {
                int deleted = psDelete.executeUpdate();
                core.GameLogger.info("Cleaned up " + deleted + " generated skill records (id_2 <= 2).");
            }
            // Xóa nhóm id_2 2003-2063 (skill trái ác quỷ, base là lv1)
            try (PreparedStatement psDelete = conn
                    .prepareStatement("DELETE FROM `skill` WHERE `Lv_RQ` > 1 AND id_2 >= 2003 AND id_2 <= 2063")) {
                int deleted = psDelete.executeUpdate();
                core.GameLogger.info("Cleaned up " + deleted + " generated skill records (id_2 2003-2063).");
            }
        } catch (SQLException e) {
            core.GameLogger.error("[Disconnect] SkillTool.generateSkillsLv31toMax error: " + e.getMessage());
        }

        // 2. Reload DB sau cleanup
        loadSkillsFromDB();

        int generatedCount = 0;
        int globalIdCounter = getMaxIdIndex();
        core.GameLogger.info("Starting id_index counter from: " + (globalIdCounter + 1));

        // Nhóm id_2 hợp lệ để generate
        Set<Integer> normalIds = new HashSet<>(Arrays.asList(0, 1, 2));

        Set<String> processedBaseSkills = new HashSet<>();

        for (Skill_Template baseSkill : Skill_Template.ENTRYS) {
            boolean isNormalGroup = normalIds.contains((int) baseSkill.ID);
            boolean isDevilGroup = baseSkill.ID >= 2003 && baseSkill.ID <= 2063;

            // Bỏ qua nếu không thuộc nhóm nào
            if (!isNormalGroup && !isDevilGroup) {
                continue;
            }

            // Skill thường: base là lv30
            if (isNormalGroup && baseSkill.Lv_RQ != 30) {
                continue;
            }

            // Skill ác quỷ: base là lv1
            if (isDevilGroup && baseSkill.Lv_RQ != 1) {
                continue;
            }

            // Nhóm trái ác quỷ: chỉ generate nếu typeSkill <= 2 (bỏ qua nội tại = 3)
            if (isDevilGroup && baseSkill.typeSkill > 2) {
                core.GameLogger.info("Skipping (nội tại/typeSkill=" + baseSkill.typeSkill + "): " + baseSkill.name);
                continue;
            }

            // Bỏ qua skill ác quỷ trong blacklist
            if (isDevilGroup && DEVIL_SKILL_BLACKLIST.contains(baseSkill.ID)) {
                core.GameLogger.info("Skipping (blacklist/id_2=" + baseSkill.ID + "): " + baseSkill.name);
                continue;
            }

            // DEDUPLICATION
            String key = baseSkill.name + "_" + baseSkill.ID;
            if (processedBaseSkills.contains(key)) {
                System.out.println("Skipping duplicate base template: " + baseSkill.name
                        + " (id_index=" + baseSkill.indexSkillInServer + ")");
                continue;
            }
            processedBaseSkills.add(key);

            final short fixedEffectId = baseSkill.getTypeEffSkill();

            // Skill thường: generate lv31 → MAX_LEVEL
            // Skill ác quỷ: generate lv2 → MAX_DEVIL_LEVEL
            int startLv = isDevilGroup ? 2 : 31;
            int endLv = isDevilGroup ? getMaxDevilSkillLevel() : MAX_LEVEL;

            System.out.println(
                    "Processing Base Skill Lv" + baseSkill.Lv_RQ + ": [id_index=" + baseSkill.indexSkillInServer
                            + ", Name=" + baseSkill.name
                            + ", id_2=" + baseSkill.ID
                            + ", typeSkill=" + baseSkill.typeSkill
                            + ", typeEffSkill=" + fixedEffectId
                            + ", generate lv" + startLv + " → " + endLv + "]");

            try (Connection conn = SQL.gI().getCon()) {
                Skill_Template current = baseSkill;

                for (int lv = startLv; lv <= endLv; lv++) {
                    globalIdCounter++;
                    int newIndex = globalIdCounter;

                    if (newIndex > 2_000_000) {
                        core.GameLogger.error("CRITICAL: id_index " + newIndex + " quá lớn! Dừng generation.");
                        break;
                    }

                    int newDamage = calculateSkillDamage(baseSkill.ID, lv, current.damage);
                    short newMana = calculateSkillMana(baseSkill.ID, lv, current.manaLost);
                    int newCoolDown = calculateSkillCoolDown(baseSkill.ID, lv, current.timeDelay);
                    List<Option> newOptions = calculateSkillOptions(baseSkill.ID, lv, baseSkill.op);

                    short newEff = fixedEffectId;
                    short calcEff = calculateSkillEffect(baseSkill.ID, lv, fixedEffectId);
                    if (calcEff != -1)
                        newEff = calcEff;
                    short customEff = getCustomEff(baseSkill.ID, lv, newEff);
                    if (customEff != -1)
                        newEff = customEff;

                    Skill_Template nextLevel = new Skill_Template(
                            newIndex,
                            baseSkill.ID,
                            baseSkill.idIcon,
                            baseSkill.typeSkill,
                            baseSkill.typeBuff,
                            baseSkill.name,
                            newEff,
                            baseSkill.range);

                    // Skill trái ác quỷ (id_2 2003-2063) luôn set typeDevil = 1
                    // Skill thường kế thừa typeDevil từ base
                    byte typeDevilFinal = isDevilGroup ? (byte) 1 : baseSkill.typeDevil;

                    nextLevel.getData(
                            baseSkill.nTarget,
                            baseSkill.rangeLan,
                            newDamage,
                            newMana,
                            newCoolDown,
                            baseSkill.nKick,
                            baseSkill.info,
                            (short) lv,
                            typeDevilFinal);

                    nextLevel.op = newOptions;
                    nextLevel.idEffSpec = baseSkill.idEffSpec;
                    nextLevel.perEffSpec = baseSkill.perEffSpec;
                    nextLevel.timeEffSpec = baseSkill.timeEffSpec;

                    insertSkillToDB(conn, nextLevel);

                    current = nextLevel;
                    generatedCount++;

                    if (generatedCount % 1000 == 0) {
                        core.GameLogger.info("... generated " + generatedCount + " entries");
                    }
                }
            } catch (SQLException e) {
                core.GameLogger.error("[Disconnect] SkillTool.generateSkillsLv31toMax error: " + e.getMessage());
            }
        }

        core.GameLogger.info("Generated " + generatedCount + " new skill entries.");
        core.GameLogger.info("Final id_index counter: " + globalIdCounter);

        updateSkillInfoSourceCode();
        updateGameLogicSourceCode();
    }

    private static void printExpTable() {
        int maxLevel = getMaxSkillLevel();
        int arraySize = maxLevel + 10;

        long[] debugExp = new long[arraySize];
        debugExp[0] = 3400;
        debugExp[1] = 10000;
        debugExp[2] = 30000;
        debugExp[3] = 60000;
        debugExp[4] = 120000;
        for (int i = 5; i < debugExp.length; i++) {
            long nextExp = (debugExp[i - 1] * 102) / 100;
            if (nextExp < debugExp[i - 1] || nextExp > Long.MAX_VALUE - 100000) {
                debugExp[i] = debugExp[i - 1]; // Cap at previous valid value if overflow occurs
            } else {
                debugExp[i] = nextExp;
            }
        }

        core.GameLogger.info("\n[EXP TABLE CHECK]");
        core.GameLogger.info("EXP tại Lv 100  : " + debugExp[99]);
        core.GameLogger.info("EXP tại Lv 500  : " + debugExp[499]);
        core.GameLogger.info("EXP tại Lv " + maxLevel + " : " + debugExp[maxLevel - 1]);
        if (debugExp[maxLevel - 1] < 0) {
            core.GameLogger.error("[CẢNH BÁO] EXP tại Lv " + maxLevel + " bị âm (Long Overflow)! Giảm hệ số tăng EXP.");
        } else {
            core.GameLogger.info("[OK] EXP không bị âm tại Lv " + maxLevel + ".");
        }
    }

    private static void updateSkillInfoSourceCode() {
        String filePath = "src/main/java/template/Skill_info.java";
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                core.GameLogger.error("Cannot find Skill_info.java at: " + file.getAbsolutePath());
                return;
            }

            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            List<String> newLines = new ArrayList<>();
            boolean inRegion = false;

            int maxLevel = getMaxSkillLevel();
            int arraySize = maxLevel + 10;

            StringBuilder expCodeBlock = new StringBuilder();
            expCodeBlock.append("    public static long[] EXP = new long[" + arraySize + "];\n");
            expCodeBlock.append("    static {\n");
            expCodeBlock.append("        Skill_info.EXP[0] = 3400;\n");
            expCodeBlock.append("        Skill_info.EXP[1] = 10000;\n");
            expCodeBlock.append("        Skill_info.EXP[2] = 30000;\n");
            expCodeBlock.append("        Skill_info.EXP[3] = 60000;\n");
            expCodeBlock.append("        Skill_info.EXP[4] = 120000;\n");
            expCodeBlock.append("        for (int i = 5; i < Skill_info.EXP.length; i++) {\n");
            expCodeBlock.append("            ").append(getExpFormulaString()).append("\n");
            expCodeBlock.append("        }\n");
            expCodeBlock.append("    }");

            StringBuilder devilCodeBlock = new StringBuilder();
            devilCodeBlock.append("    public static int[] EXP_DEVIL = new int[] { ");
            for (int i = 0; i < 5; i++) {
                devilCodeBlock.append(calculateExpDevil(i, 0));
                if (i < 4)
                    devilCodeBlock.append(", ");
            }
            devilCodeBlock.append(" };");

            for (String line : lines) {
                String normalized = line.trim().replace(" ", "");

                if (normalized.equals("//REGION_EXP_START")) {
                    newLines.add(line);
                    newLines.add(expCodeBlock.toString());
                    inRegion = true;
                    continue;
                }
                if (normalized.equals("//REGION_EXP_END")) {
                    inRegion = false;
                    newLines.add(line);
                    continue;
                }
                if (normalized.equals("//REGION_EXP_DEVIL_START")) {
                    newLines.add(line);
                    newLines.add(devilCodeBlock.toString());
                    inRegion = true;
                    continue;
                }
                if (normalized.equals("//REGION_EXP_DEVIL_END")) {
                    inRegion = false;
                    newLines.add(line);
                    continue;
                }

                if (!inRegion) {
                    newLines.add(line);
                }
            }

            boolean hasMaxLevel = lines.stream().anyMatch(l -> l.contains("public static int MAX_LEVEL ="));
            boolean hasExpRate = lines.stream().anyMatch(l -> l.contains("public static float SKILL_EXP_RATE ="));

            List<String> finalLines = new ArrayList<>();
            if (!hasMaxLevel || !hasExpRate) {
                for (String line : newLines) {
                    finalLines.add(line);
                    if (line.contains("public class Skill_info") && !line.contains("//")) {
                        if (!hasMaxLevel) {
                            finalLines.add("    // INJECTED BY SkillTool");
                            finalLines.add("    public static int MAX_LEVEL = " + getMaxSkillLevel() + ";");
                        }
                        if (!hasExpRate) {
                            finalLines.add("    // INJECTED BY SkillTool");
                            finalLines
                                    .add("    public static float SKILL_EXP_RATE = " + getSkillExpCoefficient() + "f;");
                        }
                    }
                }
                newLines = finalLines;
            } else {
                for (String line : newLines) {
                    if (line.contains("public static int MAX_LEVEL =")) {
                        finalLines.add("    public static int MAX_LEVEL = " + getMaxSkillLevel() + ";");
                    } else if (line.contains("public static float SKILL_EXP_RATE =")) {
                        finalLines.add("    public static float SKILL_EXP_RATE = " + getSkillExpCoefficient() + "f;");
                    } else {
                        finalLines.add(line);
                    }
                }
                newLines = finalLines;
            }

            java.nio.file.Files.write(file.toPath(), newLines);
            core.GameLogger.info("[SUCCESS] Updated Skill_info.java.");

        } catch (java.io.IOException e) {
            core.GameLogger.error("[Disconnect] SkillTool.updateSkillInfoSourceCode error: " + e.getMessage());
        }
    }

    private static void updateGameLogicSourceCode() {
        java.io.File templateFile = new java.io.File("src/main/java/template/Skill_Template.java");
        if (templateFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(templateFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                if (content.contains("if (result.Lv_RQ > 100)")) {
                    String newContent = content.replace("if (result.Lv_RQ > 100)",
                            "if (result.Lv_RQ > Skill_info.MAX_LEVEL)");
                    java.nio.file.Files.write(templateFile.toPath(),
                            newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    core.GameLogger.info("[SUCCESS] Updated Skill_Template.java to use Skill_info.MAX_LEVEL.");
                }
            } catch (java.io.IOException e) {
                core.GameLogger.error("[Disconnect] SkillTool.updateGameLogicSourceCode error: " + e.getMessage());
            }
        }

        java.io.File playerFile = new java.io.File("src/main/java/client/Player.java");
        if (playerFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(playerFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                String oldCode = "exp *= Manager.gI().exp;";
                String newCode = "exp = (long)(exp * Manager.gI().exp * Skill_info.SKILL_EXP_RATE);";
                if (content.contains(oldCode)) {
                    String newContent = content.replace(oldCode, newCode);
                    java.nio.file.Files.write(playerFile.toPath(),
                            newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    core.GameLogger.info("[SUCCESS] Updated Player.java to use Skill_info.SKILL_EXP_RATE.");
                }
            } catch (java.io.IOException e) {
                core.GameLogger.error("[Disconnect] SkillTool.updateGameLogicSourceCode error: " + e.getMessage());
            }
        }
    }

    private static void insertSkillToDB(Connection conn, Skill_Template skill) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `skill` (`id`, `name`, `typeSkill`, `typeBuff`, `typeEffSkill`, `range`, `rangeLan`, `nTarget`, `nKick`, `manaLost`, `timeDelay`, `damage`, `Lv_RQ`, `icon`, `info`, `typeDevil`, `option`, `EffSpec`, `id_index`, `id_2`, `percentLv`, `LvDevilSkill`, `phanTramDevilSkill`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            ps.setInt(1, 0);
            ps.setString(2, skill.name);
            ps.setByte(3, skill.typeSkill);
            ps.setByte(4, skill.typeBuff);
            ps.setShort(5, skill.getTypeEffSkill());
            ps.setShort(6, skill.range);
            ps.setShort(7, skill.rangeLan);
            ps.setByte(8, skill.nTarget);
            ps.setByte(9, skill.nKick);
            ps.setShort(10, skill.manaLost);
            ps.setInt(11, skill.timeDelay);
            ps.setInt(12, skill.damage);
            ps.setShort(13, skill.Lv_RQ);
            ps.setShort(14, skill.idIcon);
            ps.setString(15, skill.info);
            ps.setByte(16, skill.typeDevil);

            JSONArray jsOp = new JSONArray();
            if (skill.op != null) {
                for (Option o : skill.op) {
                    JSONArray jsO = new JSONArray();
                    jsO.add(o.id);
                    jsO.add(o.getParam());
                    jsOp.add(jsO);
                }
            }
            ps.setString(17, jsOp.toJSONString());

            JSONArray jsEff = new JSONArray();
            jsEff.add(skill.idEffSpec);
            jsEff.add(skill.perEffSpec);
            jsEff.add(skill.timeEffSpec);
            ps.setString(18, jsEff.toJSONString());

            ps.setInt(19, skill.indexSkillInServer);
            ps.setInt(20, skill.ID);
            ps.setInt(21, 0); // percentLv
            ps.setInt(22, 0); // LvDevilSkill
            ps.setInt(23, 0); // phanTramDevilSkill

            ps.executeUpdate();
        } catch (SQLException e) {
            core.GameLogger.error("[Disconnect] SkillTool.insertSkillToDB error: " + e.getMessage());
        }
    }
}