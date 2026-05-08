package client;

import core.Util;
import database.SQL;
import io.Session;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import map.Map;
import map.Vgo;
import org.json.simple.parser.ParseException;
import template.Skill_info;//

public class playerClone {

    public static final List<playerClone> IIIIIIIlllllIIIIllIIIll = new ArrayList<>();
    private static boolean isUpdating = false;
    public Player pClone;
    Player playerTarget;
    public Map m;

    public static Player findPlayerPvP(Player currentPlayer) {
        Player pcc = null;
        List<String> lName = new ArrayList<>();
        Connection c = null;
        ResultSet r = null;
        PreparedStatement p = null;

        int currentPvPPoint = currentPlayer.get_pvpPoint();

        try {
            c = SQL.gI().getCon();
            // Tìm người chơi có điểm PvP gần nhất
            p = c.prepareStatement(
                    "SELECT name, ABS(pvppoint - ?) as point_diff FROM players "
                            + "WHERE id != ? "
                            + "ORDER BY point_diff ASC LIMIT 1");
            p.setInt(1, currentPvPPoint);
            p.setInt(2, currentPlayer.id);
            r = p.executeQuery();

            if (r.next()) {
                lName.add(r.getString("name"));
            }
        } catch (Exception e) {
            core.GameLogger.error("playerClone.findPlayerPvP: Error finding player", e);
            Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (p != null) {
                    p.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            if (!lName.isEmpty()) {
                playerClone pcl = new playerClone(lName.get(0));
                pcl.pClone.conn.p = pcl.pClone;
                pcc = pcl.pClone;
                pcl.pClone.pcl = pcl;
            }
        } catch (Exception ex) {
            core.GameLogger.error("playerClone.findPlayerPvP: Error creating player clone", ex);
            Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex);
        }
        return pcc;
    }

    public static Player findPlayerTruyNa(Player currentPlayer) {
        Player pcc = null;
        List<String> lName = new ArrayList<>();
        Connection c = null;
        ResultSet r = null;
        PreparedStatement p = null;

        long currentWantedPoint = currentPlayer.get_wanted_point();

        try {
            c = SQL.gI().getCon();
            p = c.prepareStatement(
                    "SELECT name, ABS(wanted_point - ?) as point_diff FROM players "
                            + "WHERE id != ? "
                            + "ORDER BY point_diff ASC LIMIT 1");
            p.setLong(1, currentWantedPoint);
            p.setInt(2, currentPlayer.id);
            r = p.executeQuery();

            if (r.next()) {
                lName.add(r.getString("name"));
            }
        } catch (Exception e) {
            core.GameLogger.error("playerClone.findPlayerTruyNa: Error finding player", e);
            Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (p != null) {
                    p.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException ex) {
                core.GameLogger.error("playerClone.findPlayerTruyNa: Error closing resources", ex);
                Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            if (!lName.isEmpty()) {
                playerClone pcl = new playerClone(lName.get(0));
                pcl.pClone.conn.p = pcl.pClone;
                pcc = pcl.pClone;
                pcl.pClone.pcl = pcl;
            }
        } catch (Exception ex) {
            core.GameLogger.error("playerClone.findPlayerTruyNa: Error creating player clone", ex);
            Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex);
        }
        return pcc;
    }

    public static void ud() {
        synchronized (IIIIIIIlllllIIIIllIIIll) {
            if (isUpdating)
                return;
            isUpdating = true;
        }
        new Thread(() -> {
            while (true) {
                List<playerClone> copy = new ArrayList<>();
                synchronized (IIIIIIIlllllIIIIllIIIll) {
                    copy.addAll(IIIIIIIlllllIIIIllIIIll);
                }
                for (playerClone pc : copy) {
                    if (pc != null) {
                        try {
                            pc.update();
                        } catch (Exception ex) {
                            core.GameLogger.error("playerClone.ud: Error updating player clone", ex);
                            try {
                                pc.dis();
                            } catch (IOException ex1) {
                                core.GameLogger.error("playerClone.ud: Error disconnecting player clone", ex1);
                                Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    core.GameLogger.error("playerClone.ud: Thread interrupted", ex);
                }
            }
        }, "PlayerClone-Update").start();
    }

    public playerClone(String name) throws IOException, ParseException {
        pClone = new Player(new Session(), name);
        pClone.clone = true;
        pClone.setupClone();
        pClone.setin4();
        pClone.hp = pClone.body.get_hp_max(false);
        Vgo vgo = new Vgo();
        vgo.map_go = Map.get_map_by_id(1000);
        vgo.xnew = 350;
        vgo.ynew = 260;
        pClone.goto_map(vgo);
    }

    public void batDau() {
        if (!IIIIIIIlllllIIIIllIIIll.contains(this)) {
            IIIIIIIlllllIIIIllIIIll.add(this);
            pClone.timeSetPK = System.currentTimeMillis();
            pClone.timeSua = System.currentTimeMillis() + 5000;
        }
    }

    public static void pvp(Player p0, Player p1) throws IOException, InterruptedException {
        if (p0 != null && p1 != null && p0.map != null && p1.map != null && p0.map.equals(p1.map)) {
            // p0.map.send_chat_popup(0, p0.index_map, "Ngươi đã sẵn sàng chưa");
            p0.typePKClone = true;
        }
    }

    public void move(Player p, short x, short y) throws IOException {
        if (pClone.map != null) {
            pClone.map.send_move(pClone, y, y);
        }
    }

    long timeTonTai = System.currentTimeMillis();

    public void update() throws Exception {
        if (System.currentTimeMillis() - timeTonTai > 60 * 1000 * 5) {
            dis();
        }
        if (timeTonTai != 0 && System.currentTimeMillis() - timedis > 10000
                && System.currentTimeMillis() - timedis < 20000) {
            dis();
        }
        playerTarget = pClone.pvp_target;
        if (playerTarget == null || !pClone.map.players.contains(playerTarget)) {
            dis();
            return;
        }
        if (pClone.map != null) {
            m = pClone.map;
        }
        if (pClone.typePKClone) {
            // pClone.map.change_flag(pClone, 0);
            // playerTarget.map.change_flag(playerTarget, 0);
            attackPlayer(pClone, playerTarget);

            // String[] arrayTextSua = { "Má mày", "Tao ỉa lên người mày", "Chết đi", "Thằng
            // gay" };
            // if (System.currentTimeMillis() - pClone.timeSua > 5000) {// 5000 la thoi gian
            // duco phep sua
            // pClone.timeSua = System.currentTimeMillis();
            // pClone.map.send_chat_popup(0, pClone.index_map,
            // arrayTextSua[Util.random(arrayTextSua.length)]);
            // }
        }
        if (!pClone.typePKClone && System.currentTimeMillis() - pClone.timeSetPK > 0) {
            pClone.timeSetPK = System.currentTimeMillis();
            pvp(pClone, playerTarget);
        }
        /*
         * doan tren la thang loz clone sua bay thi thay text di nha
         */
    }

    long timedis = 0;

    public void attackPlayer(Player p0, Player p1) {
        try {
            {
                if (p0 == null || p1 == null || p1.isdie || pClone.isdie || !p1.map.equals(p0.map)) {
                    timedis = System.currentTimeMillis();
                } else {
                    timedis = 0;
                }
            }
            Skill_info s = p0.skill_point.get(Util.random(p0.skill_point.size()));
            if (!p0.map.use_skill(p0, (short) Util.random(10), (byte) 0, (byte) 1, Arrays.asList(playerTarget.id))) {
            }

        } catch (IOException ex) {
            core.GameLogger.error("playerClone.attackPlayer: Error attacking player", ex);
            Logger.getLogger(playerClone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // public void dis() throws IOException {
    // core.GameLogger.info("Clone disc " + pClone + " " + pClone.conn);
    // pClone.isclose = true;
    // pClone.x = -9999;
    // pClone.y = -9999;
    // if (pClone.conn.p != null && pClone.conn.p.map != null) {
    // pClone.conn.p.map.leave_map(pClone.conn.p, 0);
    // if (pClone.conn.p.ship_pet != null && pClone.conn.p.ship_pet.map == null) {
    // pClone.conn.p.ship_pet.map = pClone.conn.p.map;
    // }
    // }
    // if (pClone.map != null) {
    // pClone.map.players.remove(pClone);
    // }
    // pClone.conn.clear_network(pClone.conn);
    // IIIIIIIlllllIIIIllIIIll.remove(this);
    // }

    public void dis() throws IOException {
        if (pClone.pcl == null) {
            return;
        }
        pClone.pcl = null; // Ngắt liên kết ngay lập tức để tránh đệ quy
        pClone.isclose = true;
        pClone.x = -9999;
        pClone.y = -9999;

        if (pClone.conn != null && pClone.conn.p != null && pClone.conn.p.map != null) {
            pClone.conn.p.map.leave_map(pClone.conn.p, 0);
            if (pClone.conn.p.ship_pet != null && pClone.conn.p.ship_pet.map == null) {
                pClone.conn.p.ship_pet.map = pClone.conn.p.map;
            }
        }

        // ĐÃ XÓA: pClone.map.players.remove(pClone); -> Chỗ này gây sập server

        if (pClone.conn != null) {
            pClone.conn.clear_network(pClone.conn);
        }

        // Bọc synchronized cho list quản lý bot cục bộ
        synchronized (IIIIIIIlllllIIIIllIIIll) {
            IIIIIIIlllllIIIIllIIIll.remove(this);
        }
    }
}
