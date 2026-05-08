package devilfruit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import template.Item_wear;

public class DevilFruitManager {

    public static class DevilFruitOption {
        public int optId;
        public int optValue;

        public DevilFruitOption(int id, int val) {
            this.optId = id;
            this.optValue = val;
        }
    }

    public static class DevilFruitData {
        public short id;
        public String name;
        public List<Byte> allowEquipTypes = new ArrayList<>();
        public List<DevilFruitOption> options = new ArrayList<>();
    }

    public static class DevilFruitSynergy {
        public String synergyName;
        public List<Short> requiredFruits = new ArrayList<>();
        public List<DevilFruitOption> bonusOptions = new ArrayList<>();
    }

    public static HashMap<Short, DevilFruitData> FRUITS = new HashMap<>();
    public static List<DevilFruitSynergy> SYNERGIES = new ArrayList<>();

    public static void load() {
        FRUITS.clear();
        SYNERGIES.clear();
        try {
            byte[] ab = core.Util.loadfile("data/devil_fruits.json");
            if (ab != null) {
                String data = new String(ab, "UTF-8");
                JSONArray arr = (JSONArray) JSONValue.parse(data);
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject obj = (JSONObject) arr.get(i);
                        DevilFruitData fd = new DevilFruitData();
                        fd.id = Short.parseShort(obj.get("id").toString());
                        fd.name = obj.get("name").toString();

                        JSONArray equips = (JSONArray) obj.get("allow_equip_types");
                        for (int e = 0; e < equips.size(); e++) {
                            fd.allowEquipTypes.add(Byte.parseByte(equips.get(e).toString()));
                        }

                        JSONArray opts = (JSONArray) obj.get("options");
                        for (int o = 0; o < opts.size(); o++) {
                            JSONObject optObj = (JSONObject) opts.get(o);
                            int optId = Integer.parseInt(optObj.get("id").toString());
                            int optValue = Integer.parseInt(optObj.get("value").toString());
                            fd.options.add(new DevilFruitOption(optId, optValue));
                        }
                        FRUITS.put(fd.id, fd);
                    }
                    core.GameLogger.info("Load " + FRUITS.size() + " Devil Fruits Khuyen Khich");
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("DevilFruitManager.load: Error loading devil fruits", e);
        }

        try {
            byte[] abSyn = core.Util.loadfile("data/devil_fruit_synergies.json");
            if (abSyn != null) {
                String dataSyn = new String(abSyn, "UTF-8");
                JSONArray arrSyn = (JSONArray) JSONValue.parse(dataSyn);
                if (arrSyn != null) {
                    for (int i = 0; i < arrSyn.size(); i++) {
                        JSONObject obj = (JSONObject) arrSyn.get(i);
                        DevilFruitSynergy syn = new DevilFruitSynergy();
                        syn.synergyName = obj.get("synergy_name").toString();

                        JSONArray reqs = (JSONArray) obj.get("required_fruits");
                        for (int r = 0; r < reqs.size(); r++) {
                            syn.requiredFruits.add(Short.parseShort(reqs.get(r).toString()));
                        }

                        JSONArray opts = (JSONArray) obj.get("bonus_options");
                        for (int o = 0; o < opts.size(); o++) {
                            JSONObject optObj = (JSONObject) opts.get(o);
                            int optId = Integer.parseInt(optObj.get("id").toString());
                            int optValue = Integer.parseInt(optObj.get("value").toString());
                            syn.bonusOptions.add(new DevilFruitOption(optId, optValue));
                        }
                        SYNERGIES.add(syn);
                    }
                    core.GameLogger.info("Load " + SYNERGIES.size() + " Devil Fruit Synergies");
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("DevilFruitManager.load: Error loading devil fruit synergies", e);
        }
    }

    public static boolean isDevilFruit(short id) {
        return FRUITS.containsKey(id);
    }

    public static boolean canSocketToItem(Item_wear it_select, short id_ngoc) {
        DevilFruitData fd = FRUITS.get(id_ngoc);
        if (fd != null && it_select != null && it_select.template != null) {
            // Allow socketing into any drillable item as requested by user
            return true;
        }
        return false;
    }

    public static int[] getOptionIds(short id_ngoc) {
        DevilFruitData fd = FRUITS.get(id_ngoc);
        if (fd != null) {
            int[] ids = new int[fd.options.size()];
            for (int i = 0; i < fd.options.size(); i++) {
                ids[i] = fd.options.get(i).optId;
            }
            return ids;
        }
        return new int[] {};
    }

    public static int[] getOptionValues(short id_ngoc) {
        DevilFruitData fd = FRUITS.get(id_ngoc);
        if (fd != null) {
            int[] vals = new int[fd.options.size()];
            for (int i = 0; i < fd.options.size(); i++) {
                vals[i] = fd.options.get(i).optValue;
            }
            return vals;
        }
        return new int[] {};
    }

    private static List<DevilFruitSynergy> getMatchedSynergies(short[] mdakham, String combo_pattern) {
        List<DevilFruitSynergy> matched = new ArrayList<>();
        if (mdakham == null || mdakham.length == 0 || combo_pattern == null || combo_pattern.isEmpty()) {
            return matched;
        }

        // 1. Parse pattern slots
        List<Integer> slots = new ArrayList<>();
        try {
            for (String part : combo_pattern.split("-")) {
                if (!part.trim().isEmpty()) {
                    slots.add(Integer.parseInt(part.trim()));
                }
            }
        } catch (Exception e) {
            core.GameLogger.error("DevilFruitManager.getMatchedSynergies: Error parsing combo pattern", e);
            return matched;
        }

        // 2. Segment-based matching
        int currentFruitIdx = 0;
        for (int slotSize : slots) {
            if (currentFruitIdx >= mdakham.length) {
                break;
            }

            // Get fruit counts for THIS segment only
            java.util.Map<Short, Integer> segmentCounts = new java.util.HashMap<>();
            int endIdx = Math.min(currentFruitIdx + slotSize, mdakham.length);
            for (int i = currentFruitIdx; i < endIdx; i++) {
                short fid = mdakham[i];
                if (fid > 0) {
                    segmentCounts.put(fid, segmentCounts.getOrDefault(fid, 0) + 1);
                }
            }

            // Find the best synergy for THIS specific segment
            // Priority is determined by the order in the SYNERGIES list (higher tier
            // generally comes first or is matched first)
            DevilFruitSynergy bestMatched = null;
            for (DevilFruitSynergy syn : SYNERGIES) {
                // Synergy must match the slot size perfectly
                if (syn.requiredFruits.size() == slotSize) {
                    java.util.Map<Short, Integer> requiredCounts = new java.util.HashMap<>();
                    for (short fid : syn.requiredFruits) {
                        requiredCounts.put(fid, requiredCounts.getOrDefault(fid, 0) + 1);
                    }

                    boolean hasAll = true;
                    for (java.util.Map.Entry<Short, Integer> entry : requiredCounts.entrySet()) {
                        if (segmentCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                            hasAll = false;
                            break;
                        }
                    }

                    if (hasAll) {
                        bestMatched = syn;
                        break; // Found the highest priority match for this segment
                    }
                }
            }

            if (bestMatched != null) {
                matched.add(bestMatched);
            }

            // Advance pointer to the start of the next slot
            currentFruitIdx += slotSize;
        }
        return matched;
    }

    public static List<String> getActiveSynergyNames(short[] mdakham, String combo_pattern) {
        List<String> names = new ArrayList<>();
        List<DevilFruitSynergy> matched = getMatchedSynergies(mdakham, combo_pattern);
        for (DevilFruitSynergy syn : matched) {
            names.add(syn.synergyName);
        }
        return names;
    }

    /**
     * Trả về chuỗi hiển thị đầy đủ cho tên trang bị, ví dụ:
     * "[Sức mạnh biển sâu | Khuôn trống: 2, 6]"
     * - Nếu tất cả combo đều kích hoạt: "[Combo A, Combo B]"
     * - Nếu có slot chưa lấp: "[Combo A | Khuôn trống: 2, 6]"
     * - Nếu chưa có combo nào khớp: "[Khuôn: 4-2-6]"
     */
    public static String buildComboDisplayString(short[] mdakham, String combo_pattern) {
        if (combo_pattern == null || combo_pattern.isEmpty())
            return "";

        // Parse pattern slots for display of unfilled slots
        List<Integer> remainingSlots = new ArrayList<>();
        try {
            for (String part : combo_pattern.split("-")) {
                if (!part.trim().isEmpty())
                    remainingSlots.add(Integer.parseInt(part.trim()));
            }
        } catch (Exception e) {
            core.GameLogger.error("DevilFruitManager.buildComboDisplayString: Error parsing combo pattern", e);
            return "[Khuôn: " + combo_pattern + "]";
        }

        // Get matched synergies using consumption logic
        List<DevilFruitSynergy> matched = getMatchedSynergies(mdakham, combo_pattern);
        List<String> activeNames = new ArrayList<>();
        for (DevilFruitSynergy syn : matched) {
            activeNames.add(syn.synergyName);
            // Remove the corresponding slot from display list
            int synSize = syn.requiredFruits.size();
            for (int i = 0; i < remainingSlots.size(); i++) {
                if (remainingSlots.get(i) == synSize) {
                    remainingSlots.remove(i);
                    break;
                }
            }
        }

        // Build display string
        StringBuilder sb = new StringBuilder("[");
        if (!activeNames.isEmpty()) {
            sb.append(String.join(", ", activeNames));
            if (!remainingSlots.isEmpty()) {
                sb.append(" | Khuôn trống: ");
                for (int i = 0; i < remainingSlots.size(); i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(remainingSlots.get(i));
                }
            }
        } else {
            // No combo activated at all
            sb.append("Khuôn: ").append(combo_pattern);
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<template.Option> getSynergyOptions(short[] mdakham, String combo_pattern) {
        List<template.Option> synergyOpts = new ArrayList<>();
        List<DevilFruitSynergy> matched = getMatchedSynergies(mdakham, combo_pattern);
        for (DevilFruitSynergy syn : matched) {
            for (DevilFruitOption bOpt : syn.bonusOptions) {
                synergyOpts.add(new template.Option(bOpt.optId, bOpt.optValue));
            }
        }
        return synergyOpts;
    }
}
