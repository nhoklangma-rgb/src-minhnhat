package core;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import map.Map;
import map.MazePortal;
import client.Player;

public class OceanMazeManager {

    public static class MazeNode {
        public int id;
        public int templateId;
        public String customName;
        public List<MazePortal> portals = new ArrayList<>();
        public Map mazeMap;

        public boolean isTrap() {
            return id >= 9900;
        }
    }

    private static java.util.Map<Integer, MazeNode> nodes = new HashMap<>();

    public static void init() {
        try {
            byte[] data = Util.loadfile("data/ocean_maze.json");
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONObject root = (JSONObject) JSONValue.parse(jsonStr);

            for (Object keyObj : root.keySet()) {
                String key = (String) keyObj;
                int nodeId = Integer.parseInt(key);
                JSONObject nodeData = (JSONObject) root.get(key);

                MazeNode node = new MazeNode();
                node.id = nodeId;
                node.templateId = Integer.parseInt(nodeData.get("templateId").toString());
                node.customName = (String) nodeData.get("customName");

                JSONArray portalsData = (JSONArray) nodeData.get("portals");
                for (Object pObj : portalsData) {
                    JSONObject pData = (JSONObject) pObj;
                    MazePortal p = new MazePortal(
                            (String) pData.get("dir"),
                            Short.parseShort(pData.get("x").toString()),
                            Short.parseShort(pData.get("y").toString()),
                            Integer.parseInt(pData.get("targetNode").toString()),
                            Short.parseShort(pData.get("teleX").toString()),
                            Short.parseShort(pData.get("teleY").toString()));
                    node.portals.add(p);
                }

                // Initialize persistent Map instances (Flyweight)
                node.mazeMap = Map.createMazeMap(node.templateId, node.customName, nodeId, node.portals);
                nodes.put(nodeId, node);
            }
            core.GameLogger.info("Loaded " + nodes.size() + " Ocean Maze nodes.");
        } catch (Exception e) {
            GameLogger.error("OceanMazeManager: Error loading ocean_maze.json", e);
        }
    }

    public static String getNodeName(int nodeId) {
        MazeNode node = nodes.get(nodeId);
        return (node != null) ? node.customName : "Biển Lạ";
    }

    public static Map getMazeNode(int nodeId) {
        MazeNode node = nodes.get(nodeId);
        return (node != null) ? node.mazeMap : null;
    }

    public static void useSouthBird(Player p) {
        if (p.map == null || !p.map.isMazeMap) {
            try {
                Service.send_box_ThongBao_OK(p, "Chim Phương Nam chỉ ríu rít khi bạn ở ngoài biển khơi!");
            } catch (Exception e) {
                GameLogger.warn("[Maze] Lỗi gửi thông báo Chim Phương Nam (map null)", e);
            }
            return;
        }

        StringBuilder sb = new StringBuilder("Chim Phương Nam bắt đầu chỉ hướng:\n");
        for (MazePortal portal : p.map.mazePortals) {
            int danger = calculateDanger(portal.targetNode, p.clazz);
            sb.append("- ").append(portal.dir).append(": ").append(danger).append("% tử địa\n");
        }
        try {
            Service.send_box_ThongBao_OK(p, sb.toString());
        } catch (Exception e) {
            GameLogger.warn("[Maze] Lỗi gửi thông báo hướng Chim Phương Nam", e);
        }
    }

    private static int calculateDanger(int targetNodeId, int clazz) {
        MazeNode target = nodes.get(targetNodeId);
        int realDanger = (target != null && target.isTrap()) ? 100 : 1;

        switch (clazz) {
            case 3: // Hoa Tiêu (Nami)
                return realDanger;
            case 1: // (Zoro) - Lost
                return 1000 - realDanger;
            case 0: // Kiếm Khách (Luffy)
                return Math.max(0, Math.min(100, realDanger + (Util.random(100) - 50)));
            case 2: // Đầu Bếp (Sanji)
            case 4: // Xạ Thủ (Usopp)
                return Math.max(0, Math.min(100, realDanger + (Util.random(40) - 20)));
            default:
                return realDanger;
        }
    }

    public static void requestEnterMaze(Player p) {
        // Enforce party leader check
        if (p.party == null || p.party.list.isEmpty() ||
                !p.party.list.get(0).equals(p)) {
            try {
                Service.send_box_ThongBao_OK(p, "Yêu cầu phải có đội nhóm đi cùng!");
            } catch (Exception e) {
                GameLogger.warn("[Maze] Lỗi gửi yêu cầu vào mê cung (party check)", e);
            }
            return;
        }

        Map entranceNode = getMazeNode(999);
        if (entranceNode != null) {
            map.Vgo vgo = new map.Vgo();
            vgo.map_go = new Map[] { entranceNode };
            vgo.xnew = 400; // Default start coord
            vgo.ynew = 400;
            p.lastMazeTeleport = System.currentTimeMillis();
            try {
                p.goto_map(vgo);
            } catch (Exception e) {
                GameLogger.warn("[Maze] Lỗi dịch chuyển vào mê cung node 999", e);
            }
        }
    }
}
