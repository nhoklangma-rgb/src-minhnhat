package core;

import activities.ArchiDaily;
import activities.VongXoayMayManManager;
import activities.VoHanLienTangRanking;
import client.playerClone;
import database.SQL;
import io.Session;
import io.SessionManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import com.sun.management.OperatingSystemMXBean;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import map.Map;

public class ServerManager implements Runnable {
    private static ServerManager instance;
    private final Thread mythread;
    private ServerEventManager serverEventManager;
    private boolean running;
    private ServerSocket server;
    private io.WSServer wsServer;
    private io.HttpDataServer httpDataServer;
    private final long time;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public ServerManager() {
        this.time = System.currentTimeMillis();
        this.mythread = new Thread(this, "ServerManager-Thread");
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public void init() {
        try {
            // System.setErr(new DualPrintStream(System.err, "error_log.txt")); // Legacy
            // approach
            GameLogger.info("Log system initialized via GameLogger.");
        } catch (Exception e) {
            GameLogger.error("Could not initialize logging foundation.");
        }
        Manager.gI().init();
        VongXoayMayManManager.gI().start();
        ArchiDaily.init();
        VoHanLienTangRanking.init();
        taixiu.TaiXiuManager.init();
        taixiu.BauCuaManager.init();
        taixiu.BlackjackManager.init();
        this.running = true;
        this.mythread.start();
        serverEventManager = new ServerEventManager();
        serverEventManager.init();
        playerClone.ud();
        scheduler = Executors.newScheduledThreadPool(3);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                SaveData.process();
            } catch (Exception e) {
                GameLogger.error("[SAVE][FATAL] Scheduler Error :", e);
            }
        }, 0, 2, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BXH.update();
                activities.VoHanLienTangRanking.save();
            } catch (Exception e) {
                GameLogger.error("[Update][FATAL] BXH/VHLT Error :", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        scheduleAutoBaoTri();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                List<Session> snapshot;
                synchronized (SessionManager.CLIENT_ENTRYS) {
                    snapshot = new ArrayList<>(SessionManager.CLIENT_ENTRYS);
                }
                for (Session ss : snapshot) {
                    if (ss != null && ss.user == null && (now - ss.connectTime > 60000)) {
                        String ip = ss.ipAddress != null ? ss.ipAddress : "unknown";
                        GameLogger.session("[Watchdog] Force killed zombie session: IP " + ip
                                + " (Connected for " + (now - ss.connectTime) / 1000 + "s, User: null)");
                        ss.disconnect();
                    }
                }
            } catch (Exception e) {
                GameLogger.warn("[Watchdog] Lỗi trong tiến trình dọn dẹp zombie: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
        showServerInfoPanel();

        // Add Shutdown Hook to ensure data is saved on abrupt termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            performShutdown(false);
        }));
    }

    public void showServerInfoPanel() {
        // ===== Frame =====
        JFrame frame = new JFrame("Panel Gay");
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(28, 28, 30));
        // ===== TabbedPane =====
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(34, 34, 36));
        tabbedPane.setForeground(Color.WHITE);
        // ===== TAB Tổng quan =====
        JPanel overview = new JPanel(new GridBagLayout());
        overview.setBackground(new Color(28, 28, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.BOTH;
        String[] titles = { "Threads", "Online", "Sessions", "CPU", "RAM", "Uptime" };
        JLabel[] vals = new JLabel[titles.length];
        Color cardColor = new Color(50, 50, 60);
        Color valColor = new Color(255, 255, 255);
        Color titleColor = new Color(180, 180, 180);
        for (int i = 0; i < titles.length; i++) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(cardColor);
            card.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80), 1, true));
            JLabel t = new JLabel(titles[i], SwingConstants.CENTER);
            t.setForeground(titleColor);
            t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            vals[i] = new JLabel("-", SwingConstants.CENTER);
            vals[i].setForeground(valColor);
            vals[i].setFont(new Font("Segoe UI", Font.BOLD, 18));
            card.add(t, BorderLayout.NORTH);
            card.add(vals[i], BorderLayout.CENTER);
            gbc.gridx = i % 3;
            gbc.gridy = i / 3;
            gbc.weightx = 1;
            gbc.weighty = 1;
            overview.add(card, gbc);
        }
        tabbedPane.addTab("Dashboard", overview);
        // ===== TAB Threads =====
        String[] thCols = { "Tên Thread", "Trạng thái", "CPU (ms)" };
        DefaultTableModel thModel = new DefaultTableModel(thCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable thTable = new JTable(thModel);
        thTable.setRowHeight(26);
        thTable.getTableHeader().setReorderingAllowed(false);
        thTable.setBackground(new Color(34, 34, 36));
        thTable.setForeground(Color.WHITE);
        thTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        thTable.setGridColor(new Color(50, 50, 60));
        JScrollPane thScroll = new JScrollPane(thTable);
        thScroll.getViewport().setBackground(new Color(34, 34, 36));
        tabbedPane.addTab("Threads", thScroll);
        // ===== TAB Maps =====
        String[] mpCols = { "Tên Map", "Zone", "Số Player" };
        DefaultTableModel mpModel = new DefaultTableModel(mpCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable mpTable = new JTable(mpModel);
        mpTable.setRowHeight(26);
        mpTable.getTableHeader().setReorderingAllowed(false);
        mpTable.setBackground(new Color(34, 34, 36));
        mpTable.setForeground(Color.WHITE);
        mpTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mpTable.setGridColor(new Color(50, 50, 60));
        JScrollPane mpScroll = new JScrollPane(mpTable);
        mpScroll.getViewport().setBackground(new Color(34, 34, 36));
        tabbedPane.addTab("Maps", mpScroll);
        // ===== TAB IP Monitor =====
        String[] ipCols = { "IP Address", "Connections" };
        DefaultTableModel ipModel = new DefaultTableModel(ipCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable ipTable = new JTable(ipModel);
        ipTable.setRowHeight(26);
        ipTable.getTableHeader().setReorderingAllowed(false);
        ipTable.setBackground(new Color(34, 34, 36));
        ipTable.setForeground(Color.WHITE);
        ipTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ipTable.setGridColor(new Color(50, 50, 60));
        ipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane ipScroll = new JScrollPane(ipTable);
        ipScroll.getViewport().setBackground(new Color(34, 34, 36));
        tabbedPane.addTab("IP Monitor", ipScroll);

        frame.add(tabbedPane, BorderLayout.CENTER);
        // ===== Nút Bảo Trì =====
        JButton btn = new JButton("BẢO TRÌ");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0, 122, 204));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 162, 255), 2, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        btn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Bạn có muốn bảo trì?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                performMaintenance(false);
            }
        });
        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(28, 28, 30));
        bottom.add(btn);
        frame.add(bottom, BorderLayout.SOUTH);
        // ===== Timer cập nhật realtime =====
        Timer timer = new Timer(1500, e -> {
            ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
            int threads = tBean.getThreadCount();
            int sessions = SessionManager.CLIENT_ENTRYS.size();
            int online = 0;
            synchronized (SessionManager.CLIENT_ENTRYS) {
                for (Session s : SessionManager.CLIENT_ENTRYS)
                    if (s != null && s.p != null)
                        online++;
            }
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpu = os.getSystemCpuLoad() * 100;
            int ram = (int) ((os.getTotalPhysicalMemorySize() - os.getFreePhysicalMemorySize()) * 100
                    / os.getTotalPhysicalMemorySize());
            long up = System.currentTimeMillis() - this.time;
            long h = up / 3600000, m = (up % 3600000) / 60000, s = (up % 60000) / 1000;
            vals[0].setText("" + threads);
            vals[1].setText("" + online);
            vals[2].setText("" + sessions);
            vals[3].setText(String.format("%.1f%%", cpu));
            vals[4].setText(ram + "%");
            vals[5].setText(String.format("%dh %dm %ds", h, m, s));
            thModel.setRowCount(0);
            List<Object[]> thData = new ArrayList<>();
            for (long tid : tBean.getAllThreadIds()) {
                ThreadInfo threadInfoObj = tBean.getThreadInfo(tid);
                if (threadInfoObj != null) {
                    long cpuTime = tBean.isThreadCpuTimeSupported() ? tBean.getThreadCpuTime(tid) / 1_000_000 : -1;
                    thData.add(new Object[] { threadInfoObj.getThreadName(), threadInfoObj.getThreadState(), cpuTime });
                }
            }
            thData.sort((a, b) -> Long.compare((long) b[2], (long) a[2]));
            thData.forEach(thModel::addRow);
            mpModel.setRowCount(0);
            List<Object[]> mpData = new ArrayList<>();
            synchronized (Map.ENTRYS) {
                for (Map[] arr : Map.ENTRYS) {
                    for (Map mapObj : arr) {
                        if (mapObj != null)
                            mpData.add(new Object[] { mapObj.template.name, "Khu " + (mapObj.zone_id + 1),
                                    mapObj.players.size() });
                    }
                }
            }
            mpData.sort((a, b) -> Integer.compare((int) b[2], (int) a[2]));
            mpData.forEach(mpModel::addRow);
            ipModel.setRowCount(0);
            List<String> ipListLocal = new ArrayList<>();
            List<Integer> ipCountListLocal = new ArrayList<>();
            synchronized (SessionManager.CLIENT_ENTRYS) {
                for (Session session : SessionManager.CLIENT_ENTRYS) {
                    if (session != null && session.ipAddress != null) {
                        String ip = session.ipAddress;
                        int index = ipListLocal.indexOf(ip);
                        if (index == -1) {
                            ipListLocal.add(ip);
                            ipCountListLocal.add(1);
                        } else {
                            ipCountListLocal.set(index, ipCountListLocal.get(index) + 1);
                        }
                    }
                }
            }
            // Sắp xếp giảm dần số lượng connection
            for (int i = 0; i < ipListLocal.size() - 1; i++) {
                for (int j = i + 1; j < ipListLocal.size(); j++) {
                    if (ipCountListLocal.get(j) > ipCountListLocal.get(i)) {
                        String tempIp = ipListLocal.get(i);
                        Integer tempCount = ipCountListLocal.get(i);
                        ipListLocal.set(i, ipListLocal.get(j));
                        ipCountListLocal.set(i, ipCountListLocal.get(j));
                        ipListLocal.set(j, tempIp);
                        ipCountListLocal.set(j, tempCount);
                    }
                }
            }
            // Thêm vào bảng hiển thị
            for (int i = 0; i < ipListLocal.size(); i++) {
                ipModel.addRow(new Object[] { ipListLocal.get(i), ipCountListLocal.get(i) });
            }
        });
        timer.start();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timer.stop();
            }
        });
        frame.setVisible(true);
    }

    private void scheduleAutoBaoTri() {
        scheduler.scheduleAtFixedRate(() -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            if (hour == 5 && minute == 30 && second == 0) {
                try {
                    performMaintenance(true);
                } catch (Exception e) {
                    GameLogger.error("Maintenance error", e);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void performMaintenance(boolean restart) {
        GameLogger.info("START SERVER MAINTENANCE");
        try {
            java.util.Map<client.Player, Long> txRefunds = taixiu.TaiXiuManager.stop();
            java.util.Map<client.Player, Long> bcRefunds = taixiu.BauCuaManager.stop();

            java.util.Set<client.Player> allPlayers = new java.util.HashSet<>();
            allPlayers.addAll(txRefunds.keySet());
            allPlayers.addAll(bcRefunds.keySet());

            for (client.Player p : allPlayers) {
                long txAmount = txRefunds.getOrDefault(p, 0L);
                long bcAmount = bcRefunds.getOrDefault(p, 0L);

                StringBuilder sb = new StringBuilder("Hệ thống bảo trì, hoàn trả:\n");
                if (txAmount > 0) {
                    sb.append("- Tài Xỉu: ").append(taixiu.BauCuaManager.formatSoLon(txAmount)).append(" Extol\n");
                }
                if (bcAmount > 0) {
                    sb.append("- Bầu Cua: ").append(taixiu.BauCuaManager.formatSoLon(bcAmount)).append(" Extol");
                }
                try {
                    Service.send_box_ThongBao_OK(p, sb.toString());
                } catch (Exception e) {
                    GameLogger.warn("[Maintenance] Lỗi gửi thông báo bảo trì cho " + (p != null ? p.name : "unknown")
                            + ": " + e.getMessage());
                }
            }

            for (int i = 5; i >= 0; i--) {
                Manager.gI().chatKTG(0, "Server sẽ tiến hành bảo trì sau " + i + " giây nữa", 1);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            GameLogger.error("Maintenance error", e);
        } finally {
            performShutdown(restart);
        }
    }

    public void performShutdown(boolean restart) {
        if (!isShuttingDown.compareAndSet(false, true)) {
            return;
        }

        core.GameLogger.info("[SHUTDOWN] Starting shutdown sequence...");
        CpuMonitor.gI().stop();

        // Step 1: Dừng nhận kết nối mới
        runWithTimeout("Step 1: Dừng nhận kết nối mới", () -> {
            try {
                this.close();
            } catch (IOException e) {
                GameLogger.error("[SHUTDOWN] Error closing server socket: " + e.getMessage());
            }
        }, 10);

        // Step 2: Disconnect và save tất cả player
        runWithTimeout("Step 2: Disconnect và save tất cả player", () -> {
            GameLogger.info("[SHUTDOWN] Step 2: CLIENT_ENTRYS size = " + SessionManager.CLIENT_ENTRYS.size());
            synchronized (SessionManager.CLIENT_ENTRYS) {
                for (Session ss : new HashSet<>(SessionManager.CLIENT_ENTRYS)) {
                    if (ss != null) {
                        try {
                            if (ss.p != null)
                                Service.send_box_ThongBao_OK(ss.p, "Máy chủ bảo trì!");
                            ss.disconnect();
                        } catch (Exception e) {
                            GameLogger.warn("[SHUTDOWN] Lỗi khi ngắt kết nối session "
                                    + (ss != null ? ss.ipAddress : "unknown") + ": " + e.getMessage());
                        }
                    }
                }
            }
            // Chờ đến khi CLIENT_ENTRYS không còn session nào có player, tối đa 5 giây
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                boolean allDisconnected;
                synchronized (SessionManager.CLIENT_ENTRYS) {
                    allDisconnected = SessionManager.CLIENT_ENTRYS.stream()
                            .allMatch(ss -> ss == null || ss.p == null);
                }
                if (allDisconnected)
                    break;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    core.GameLogger.error("[SHUTDOWN] Step 2: Thread interrupted", e);
                    break;
                }
            }
        }, 10);

        // Step 3: Dừng scheduled tasks
        runWithTimeout("Step 3: Dừng scheduled tasks", () -> {
            Manager.MAP_POOL.shutdown();
            try {
                if (!Manager.MAP_POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                    Manager.MAP_POOL.shutdownNow();
                }
            } catch (InterruptedException e) {
                core.GameLogger.error("[SHUTDOWN] Step 3: Map pool interrupted", e);
                Manager.MAP_POOL.shutdownNow();
            }

            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    core.GameLogger.error("[SHUTDOWN] Step 3: Scheduler interrupted", e);
                    scheduler.shutdownNow();
                }
            }

            SaveData.shutdown();

            taixiu.TaiXiuManager.stop();
            taixiu.BauCuaManager.stop();
            taixiu.BlackjackManager.stop();
            VongXoayMayManManager.gI().stop();
            BXH.shutdown();
        }, 10);


        // Step 4: Save toàn bộ data còn lại
        runWithTimeout("Step 4: Save toàn bộ data còn lại", () -> {
            long start = System.currentTimeMillis();
            SaveData.process(); // SaveData.process() calls Market.update() and Clan.update()
            VoHanLienTangRanking.save();
            GameLogger.info("[SHUTDOWN] Step 4: SaveData took " + (System.currentTimeMillis() - start) + "ms");
        }, 30);

        // Step 5: Shutdown HikariCP
        runWithTimeout("Step 5: Shutdown HikariCP", () -> {
            SQL.gI().close();
        }, 10);

        // Step 6: Exit
        GameLogger.info("[SHUTDOWN] Step 6: Finalizing...");
        if (restart) {
            runRestartScript();
        }
        GameLogger.info("[SHUTDOWN] Server stopped successfully.");
        System.exit(0);
    }

    private void runWithTimeout(String stepName, Runnable runnable, int timeoutSeconds) {
        GameLogger.info("[SHUTDOWN] " + stepName + "...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {

            Future<?> future = executor.submit(runnable);
            future.get(timeoutSeconds, TimeUnit.SECONDS);
            GameLogger.info("[SHUTDOWN] " + stepName + " completed.");
        } catch (TimeoutException e) {
            System.err.println(
                    "[SHUTDOWN] " + stepName + " TIMEOUT (waited " + timeoutSeconds + "s) - Force continuing...");
        } catch (Exception e) {
            GameLogger.error("[SHUTDOWN] " + stepName + " ERROR", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void runRestartScript() {
        try {
            Runtime.getRuntime().exec("cmd /c start run.bat");
        } catch (IOException e) {
            GameLogger.error("Error running restart script: " + e.getMessage());
        }
    }

    public void run() {
        try {
            this.server = new ServerSocket(Manager.gI().server_port);
            activeCommandLine();
            GameLogger.info("LISTEN PORT " + Manager.gI().server_port + "...");
            if (Manager.gI().enable_websocket) {
                this.wsServer = new io.WSServer(Manager.gI().websocket_port);
                this.wsServer.start();
            }
            this.httpDataServer = new io.HttpDataServer();
            this.httpDataServer.start(Manager.gI().http_data_port);
            GameLogger.info("Started in " + (System.currentTimeMillis() - this.time) + "ms");
            List<String> ipList = new ArrayList<>();
            List<Deque<Long>> ipTimes = new ArrayList<>();
            List<String> blockedIPs = new ArrayList<>();
            final int MAX_CONN_PER_IP = 20;
            final long WINDOW_MS = 10000;
            final int MAX_TOTAL_CONNECTIONS = 1000;
            while (this.running) {
                Socket client = this.server.accept();
                String ip = ((InetSocketAddress) client.getRemoteSocketAddress())
                        .getAddress().getHostAddress();
                long now = System.currentTimeMillis();
                if (blockedIPs.contains(ip)) {
                    client.close();
                    continue;
                }
                int activeConnections = 0;
                synchronized (SessionManager.CLIENT_ENTRYS) {
                    for (Session s : SessionManager.CLIENT_ENTRYS)
                        if (s != null)
                            activeConnections++;
                }
                if (activeConnections >= MAX_TOTAL_CONNECTIONS) {
                    GameLogger.session("Server reject connection : " + ip);
                    client.close();
                    continue;
                }
                int idx = ipList.indexOf(ip);
                if (idx == -1) {
                    ipList.add(ip);
                    Deque<Long> times = new ArrayDeque<>();
                    times.addLast(now);
                    ipTimes.add(times);
                } else {
                    Deque<Long> times = ipTimes.get(idx);
                    while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
                        times.pollFirst();
                    }
                    times.addLast(now);
                    if (times.size() > MAX_CONN_PER_IP) {
                        GameLogger.session("BLOCK IP : " + ip);
                        blockedIPs.add(ip);
                        client.close();
                        continue;
                    }
                }
                Session ss = new Session(client);
                if (!SessionManager.client_connect(ss)) {
                    client.close();
                }
            }
        } catch (java.net.SocketException e) {
            if (this.running) {
                GameLogger.error("Server socket error: ", e);
            } else {
                GameLogger.info("Server socket closed during shutdown.");
            }
        } catch (IOException e) {
            GameLogger.error("Server loop error: ", e);
        }

    }

    private void activeCommandLine() {
        new Thread(() -> {
            try (Scanner sc = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    String line = sc.nextLine();
                    if (line.equals("baotri")) {
                        performMaintenance(false);
                    } else {
                        core.GameLogger.info("Unknown command: " + line);
                    }
                }
            }
        }).start();
    }

    public void close() throws IOException {
        running = false;
        server.close();
        if (wsServer != null) {
            wsServer.stopServer();
        }
        if (httpDataServer != null) {
            httpDataServer.stop();
        }
        instance = null;
    }

    public ServerSocket get_server() {
        return this.server;
    }
}
