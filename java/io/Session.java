package io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import activities.UpgradeItem;
import client.Clan;
import client.Item;
import client.MessageHandler;
import client.Player;
import core.GameLogger;
import core.Manager;
import core.Service;
import database.SQL;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import map.Map;
import map.f;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import template.*;

public class Session implements Runnable {

    // private static final byte[] KEYS = "Il1|lI1|1Il|l1I".getBytes();
    protected static final byte[] KEYS = "9#mK2$pL!5vN@8xQ*1zA&4cD^7fG(0hJ)3kL-6oP+9rS=2uV_5yX/8bE".getBytes();
    protected final Socket socket;
    protected DataInputStream dis;
    protected DataOutputStream dos;
    protected Thread sendd;
    protected Thread receiv;
    public boolean connected;
    protected final List<Message> list_msg;
    // private Queue<Message> list_msg = new ConcurrentLinkedQueue<>();
    protected boolean sendKeyComplete;
    protected byte curR;
    protected byte curW;
    public String user;
    public String pass;
    protected final MessageHandler controller;
    public Player p;
    public List<String> list_char;
    public byte zoomlv;
    public String version = "1.2.9"; // Khởi tạo mặc định để tránh NPE cho Bot/Offline
    public byte lock;
    public boolean isDummy = false;
    private boolean getImgAPK = false;
    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
    public byte check;
    public String ipAddress;
    public int id; // Session ID provided by SessionManager
    public long tong_nap = 0;
    public int iconRequestCount = 0;
    public long lastIconCountReset = System.currentTimeMillis();
    public final long connectTime = System.currentTimeMillis();
    private volatile boolean disconnected = false; // ngăn chặn race condition: chỉ disconnect 1 lần
    private long lastMovePacketTime = 0;
    private static final long MOVE_THROTTLE_MS = 100; // tối đa 10 packet move/giây

    public Session(Socket socket) {
        this.socket = socket;
        this.list_msg = Collections.synchronizedList(new LinkedList<Message>());
        this.sendKeyComplete = false;
        this.connected = false;
        this.controller = new MessageHandler(this);
    }

    public Session() throws IOException {
        this.socket = new Socket("localhost", Manager.gI().server_port);
        this.list_msg = Collections.synchronizedList(new LinkedList<Message>());
        this.sendKeyComplete = false;
        this.connected = false;
        this.controller = new MessageHandler(this);
    }

    public Session(boolean dummy) {
        this.isDummy = dummy;
        this.socket = null;
        this.list_msg = Collections.synchronizedList(new LinkedList<Message>());
        this.sendKeyComplete = false;
        this.connected = false;
        this.controller = new MessageHandler(this);
    }

    public void init() {
        try {
            this.dis = new DataInputStream(socket.getInputStream());
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.socket.setSoTimeout(0);
            this.sendd = new Thread(() -> {
                try {
                    while (connected) {
                        while (!list_msg.isEmpty()) {
                            Message m = list_msg.remove(0);
                            if (m != null) {
                                send_msg(m);
                                m.cleanup();
                            }
                        }
                        if (dos != null) {
                            dos.flush();
                        }
                        Thread.sleep(20);
                    }
                } catch (InterruptedException e) {
                    GameLogger.warn("[Session] Thread interrupted during disconnect sleep: " + e.getMessage());
                } catch (IOException e) {
                    core.GameLogger.session(
                            "Session.run: IOException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
                } catch (Exception e) {
                    core.GameLogger.error(
                            "Session.run: Unexpected error for user " + user + " IP " + ipAddress + ": "
                                    + e.getMessage());
                } finally {
                    this.disconnect();
                }
            }, "Session-Send");
            this.connected = true;
            this.ipAddress = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();
            this.receiv = new Thread(this, "Session-" + this.ipAddress);
            this.receiv.start();
            this.sendd.start();
        } catch (IOException e) {
            core.GameLogger.session(
                    "Session.run: IOException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
        }
    }
    // public void init() {
    // try {
    // this.dis = new DataInputStream(socket.getInputStream());
    // this.dos = new DataOutputStream(socket.getOutputStream());
    // this.socket.setSoTimeout(0);

    // this.connected = true;
    // this.ipAddress = ((InetSocketAddress)
    // socket.getRemoteSocketAddress()).getAddress().getHostAddress();

    // this.sendd = Thread.ofVirtual().name("Session-Send-" +
    // this.ipAddress).unstarted(() -> {
    // try {
    // while (connected) {
    // Message m = list_msg.poll();
    // if (m != null) {
    // send_msg(m);
    // m.cleanup();
    // } else {
    // Thread.sleep(20);
    // }
    // }
    // } catch (InterruptedException e) {
    // GameLogger.warn("[Session] Thread interrupted during disconnect sleep: " +
    // e.getMessage());
    // } catch (IOException e) {
    // core.GameLogger.error(
    // "Session.run: IOException for user " + user + " IP " + ipAddress + ": " +
    // e.getMessage());
    // System.err.println(
    // "[Session-Send] IOException for user " + user + " IP " + ipAddress + ": " +
    // e.getMessage());
    // } catch (Exception e) {
    // core.GameLogger.error("Session.run: Unexpected error for user " + user + " IP
    // " + ipAddress + ": "
    // + e.getMessage());
    // System.err.println(
    // "[Session-Send] Unexpected error for user " + user + " IP " + ipAddress + ":
    // "
    // + e.getMessage());
    // } finally {
    // GameLogger.info("[Session-Send] Virtual thread terminated for " + ipAddress);
    // this.disconnect();
    // }
    // });

    // this.receiv = Thread.ofVirtual().name("Session-Recv-" +
    // this.ipAddress).unstarted(this);

    // this.receiv.start();
    // this.sendd.start();
    // } catch (IOException e) {
    // core.GameLogger
    // .error("Session.run: IOException for user " + user + " IP " + ipAddress + ":
    // " + e.getMessage());
    // }
    // }

    public void disconnect() {
        if (isDummy) {
            connected = false;
            return;
        }
        // Đảm bảo chỉ chạy disconnect 1 lần duy nhất
        // Tránh race condition khi cả receiv + sendd đều gọi disconnect
        synchronized (this) {
            if (disconnected) {
                return;
            }
            disconnected = true;
        }
        try {
            SessionManager.client_disconnect(this);
        } catch (Exception e) {
            this.connected = false;
            core.GameLogger
                    .error("Session.disconnect: Exception for user " + user + " IP " + ipAddress + ": "
                            + e.getMessage());
        }
    }

    // public void disconnect() {
    // if (isDummy) {
    // connected = false;
    // return;
    // }
    // // Đảm bảo chỉ chạy disconnect 1 lần duy nhất
    // // Tránh race condition khi cả receiv + sendd đều gọi disconnect
    // synchronized (this) {
    // if (disconnected) {
    // return;
    // }
    // disconnected = true;
    // }

    // this.connected = false;

    // try {
    // SessionManager.client_disconnect(this);
    // } catch (Exception e) {
    // // this.connected = false;
    // core.GameLogger
    // .error("Session.disconnect: Exception for user " + user + " IP " + ipAddress
    // + ": "
    // + e.getMessage());
    // } finally {
    // if (sendd != null)
    // sendd.interrupt();
    // try {
    // if (socket != null && !socket.isClosed())
    // socket.close();
    // } catch (IOException ignored) {
    // }
    // }
    // }

    // public void addmsg(Message m) throws IOException {
    // if (this.connected) {
    // int size = this.list_msg.size();
    // if (size > 10000) {
    // core.GameLogger.error("Disconnect user " + user + " - message queue overflow
    // (" + size + ")");
    // SessionTracker.logKick(this, "queue_overflow");
    // this.disconnect();
    // return;
    // }
    // // Intelligent drop: if queue > 3000, drop icon messages (-51, -101) to
    // // prioritize gameplay
    // if (size > 3000 && (m.cmd == -51 || m.cmd == -101)) {
    // return;
    // }
    // m.writer().flush();
    // this.list_msg.add(m);
    // }
    // }

    public void addmsg(Message m) throws IOException {
        if (!this.connected)
            return;
            
        if (this instanceof WSSession && m.cmd == -7) {
            // Block static data packets on WS for WebGL clients.
            // These are handled via HTTP port 8181.
            return;
        }

        int size = this.list_msg.size();

        if (size > 15000) {
            core.GameLogger.error("Disconnect user " + user + " - message queue overflow (" + size + ")");
            SessionTracker.logKick(this, "queue_overflow");
            this.disconnect();
            return;
        }

        // Throttle move khi queue bắt đầu tích tụ
        if (size > 500 && m.cmd == 1) {
            if (m.source_index != -1 && m.source_index != this.p.index_map) {
                long now = System.currentTimeMillis();
                if (now - lastMovePacketTime < MOVE_THROTTLE_MS)
                    return;
                lastMovePacketTime = now;
            }
        }

        // Drop effect/animation khi queue căng
        if (size > 2000 && isLowPriority(m.cmd))
            return;

        // Drop combat khi queue rất căng (giữ hp/map/mob attack)
        if (size > 6000 && (m.cmd == 3 || m.cmd == 4))
            return;

        m.writer().flush();
        this.list_msg.add(m);
    }

    private boolean isLowPriority(int cmd) {
        return cmd == 74 || cmd == -51 || cmd == -101 || cmd == 20;
    }

    @Override
    public void run() {
        long lastMessageTime = System.currentTimeMillis();
        int messageCount = 0;

        while (this.connected) {
            Message m = null;
            try {
                m = read_msg();
                if (m == null)
                    continue;

                // Packet flood protection (Max 200 packets/sec)
                long now = System.currentTimeMillis();
                if (now - lastMessageTime < 1000) {
                    messageCount++;
                    if (messageCount > 1000) {
                        String warning = "[DDOS/SPAM] Kicked IP " + this.ipAddress
                                + (this.user != null ? " (User: " + this.user + ")" : "") + " for spamming "
                                + messageCount + " packets/sec.";
                        core.GameLogger.error(warning);
                        SessionTracker.logKick(this, "ddos_spam");
                        core.Manager.gI().logDDOS(warning); // Add to a custom log file
                        break;
                    }
                } else {
                    lastMessageTime = now;
                    messageCount = 1;
                }

                // Pre-Login Guard: 15 seconds to send handshake (-27)
                if (!sendKeyComplete && now - this.connectTime > 15000) {
                    GameLogger.session("[Guard] Handshake timeout for IP " + this.ipAddress);
                    SessionTracker.logTimeout(this, "handshake_timeout");
                    break;
                }

                // Login Timeout: 30 seconds to login after handshake
                if (this.user == null && this.p == null && now - this.connectTime > 30000) {
                    GameLogger.session("[Guard] Login timeout for IP " + this.ipAddress);
                    SessionTracker.logTimeout(this, "login_timeout");
                    break;
                }

                if (check == 0) {
                    if (m.cmd != -27)
                        break;
                    check++;
                }
                if (m.cmd == -27)
                    sendkeys();
                else if (sendKeyComplete)
                    controller.process_msg(m);
            } catch (SocketTimeoutException e) {
                core.GameLogger.error(
                        "Session.run: Socket timeout for user " + user + " IP " + ipAddress + ": " + e.getMessage());
                System.err.println("[Session-Recv] Socket timeout for IP " + this.ipAddress
                        + (this.user != null ? " (User: " + this.user + ")" : ""));
                SessionTracker.logTimeout(this, "socket_timeout");
                break;
            } catch (EOFException | SocketException e) {
                // Client mất kết nối (Standard disconnect)
                if (this.user != null) {
                    core.GameLogger.session(
                            "Session.run: Connection lost for user " + user + " IP " + ipAddress + ": "
                                    + e.getMessage());
                }
                break;
            } catch (Exception e) {
                if (this.user != null) {
                    core.GameLogger.error(
                            "Session.run: Unexpected error for user " + user + " IP " + ipAddress + ": "
                                    + e.getMessage());
                }
                break;
            } finally {
                if (m != null) {
                    try {
                        m.cleanup();
                    } catch (Exception e) {
                        GameLogger.error("Message cleanup error: ", e);
                    }
                }
            }
        }
        disconnect();
    }

    protected void send_msg(Message msg) throws IOException {
        if (isDummy || dos == null) {
            msg.cleanup();
            return;
        }
        byte[] data = msg.getData();
        if (data != null && data.length > 1048576) {
            msg.cleanup();
            return;
        }
        if (sendKeyComplete) {
            byte b = writeKey(msg.cmd);
            dos.writeByte(b);
        } else {
            dos.writeByte(msg.cmd);
        }
        if (data != null) {
            int size = data.length;
            if (sendKeyComplete) {
                if ((msg.cmd == -39) || msg.cmd == -101 || msg.cmd == -93 || msg.cmd == 76) {
                    dos.writeByte(writeKey((byte) (size >> 24)));
                    dos.writeByte(writeKey((byte) (size >> 16)));
                    dos.writeByte(writeKey((byte) (size >> 8)));
                    dos.writeByte(writeKey((byte) (size)));
                } else {
                    int byte1 = writeKey((byte) (size >> 8));
                    dos.writeByte(byte1);
                    int byte2 = writeKey((byte) (size));
                    dos.writeByte(byte2);
                }
            } else if (msg.cmd == -39) {
                dos.writeInt(size);
            } else {
                this.dos.writeByte((size >> 8) & 0xFF);
                this.dos.writeByte(size & 0xFF);

            }
            if (sendKeyComplete) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = writeKey(data[i]);
                }
            }
            dos.write(data);
        } else {
            this.dos.writeShort(0);
        }
        // dos.flush(); // Moved to batch flush in send thread
        msg.cleanup();
    }

    private Message read_msg() throws IOException {
        byte cmd = dis.readByte();
        if (sendKeyComplete)
            cmd = readKey(cmd);
        int size = sendKeyComplete
                ? ((readKey(dis.readByte()) & 0xFF) << 8 | (readKey(dis.readByte()) & 0xFF))
                : dis.readUnsignedShort();
        if (size < 0 || size > 65535) {
            throw new IOException("Invalid message size: " + size +
                    " from IP: " + socket.getInetAddress().getHostAddress());
        }
        byte[] data = new byte[size];
        dis.readFully(data);
        if (sendKeyComplete)
            for (int i = 0; i < data.length; i++)
                data[i] = readKey(data[i]);
        return new Message(cmd, data);
    }

    protected byte readKey(final byte b) {
        final byte curR = this.curR;
        this.curR = (byte) (curR + 1);
        final byte i = (byte) ((KEYS[curR] & 0xFF) ^ (b & 0xFF));
        if (this.curR >= KEYS.length) {
            this.curR %= (byte) KEYS.length;
        }
        return i;
    }

    protected byte writeKey(final byte b) {
        final byte curW = this.curW;
        this.curW = (byte) (curW + 1);
        final byte i = (byte) ((KEYS[curW] & 0xFF) ^ (b & 0xFF));
        if (this.curW >= KEYS.length) {
            this.curW %= (byte) KEYS.length;
        }
        return i;
    }

    public void sendkeys() throws IOException {
        Message msg = new Message(-27);
        msg.writer().writeByte(KEYS.length);
        msg.writer().writeByte(KEYS[0]);
        for (int i = 1; i < KEYS.length; i++) {
            msg.writer().writeByte(KEYS[i] ^ KEYS[i - 1]);
        }
        send_msg(msg);
        msg.cleanup();
        sendKeyComplete = true;
    }

    public void request_data_update(Message m) throws IOException {
        if (this instanceof WSSession) {
            // Skip large data updates over WebSocket for WebGL
            return;
        }
        byte type = m.reader().readByte();
        if (type == 3 && p != null && p.conn != null) {
            p.send_skill();
        } else {
            Message m2 = new Message(-7);
            switch (type) {
                case 2: {
                    m2.writer().writeByte(2);
                    m2.writer().writeShort(ItemOptionTemplate.ENTRYS.size());
                    for (int i = 0; i < ItemOptionTemplate.ENTRYS.size(); i++) {
                        m2.writer().writeUTF(ItemOptionTemplate.ENTRYS.get(i).name);
                        m2.writer().writeByte(ItemOptionTemplate.ENTRYS.get(i).color);
                        m2.writer().writeByte(ItemOptionTemplate.ENTRYS.get(i).percent);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataAttri);
                    break;
                }
                case 4: {
                    m2.writer().writeByte(4);
                    m2.writer().writeByte(DataTemplate.mLockMap.length);
                    m2.writer().write(DataTemplate.mLockMap);
                    break;
                }
                case 6: {
                    m2.writer().writeByte(6);
                    byte[] ab = null;
                    try (FileInputStream fis = new FileInputStream("data/msg/login/request/msg-7_6")) {
                        ab = new byte[fis.available() - 2];
                        fis.read(ab, 0, ab.length);
                    } catch (IOException e) {
                        core.GameLogger.error("Session.run: IOException for user " + user + " IP " + ipAddress + ": "
                                + e.getMessage());
                        ab = null;
                    }
                    if (ab == null) {
                        core.GameLogger
                                .error("Session.run: Error reading data type 6 for user " + user + " IP " + ipAddress);
                        return;
                    }
                    m2.writer().write(ab);
                    m2.writer().writeShort(DataTemplate.VerdataNameMap);
                    break;
                }
                case 7: {
                    m2.writer().writeByte(7);
                    m2.writer().writeShort(DataTemplate.NamePotionquest.length);
                    for (int i = 0; i < DataTemplate.NamePotionquest.length; i++) {
                        m2.writer().writeUTF(DataTemplate.NamePotionquest[i]);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataNamePotionquest);
                    break;
                }
                case 8: {
                    m2.writer().writeByte(8);
                    m2.writer().writeShort(DataTemplate.TabInventory_ItemSell[0]);
                    m2.writer().writeShort(DataTemplate.TabInventory_ItemSell[1]);
                    m2.writer().writeShort(DataTemplate.TabInventory_ItemSell[2]);
                    break;
                }
                case 10: {
                    m2.writer().writeByte(10);
                    m2.writer().writeByte(DataTemplate.mMapLang.length);
                    for (int i = 0; i < DataTemplate.mMapLang.length; i++) {
                        m2.writer().writeShort(DataTemplate.mMapLang[i]);
                    }
                    break;
                }
                case 11: {
                    m2.writer().writeByte(11);
                    m2.writer().writeByte(ItemTemplate7.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
                        ItemTemplate7 temp = ItemTemplate7.ENTRYS.get(i);
                        m2.writer().writeByte(temp.id);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeByte(temp.icon);
                        m2.writer().writeInt(temp.price);
                        m2.writer().writeShort(temp.priceruby);
                        m2.writer().writeByte(temp.istrade);
                    }
                    break;
                }
                case 12: {
                    m2.writer().writeByte(12);
                    m2.writer().writeByte(UpgradeItem.DATA.size());
                    for (int i = 0; i < UpgradeItem.DATA.size(); i++) {
                        DataUpgrade temp = UpgradeItem.DATA.get(i);
                        m2.writer().writeByte(temp.level);
                        m2.writer().writeShort(temp.per);
                        m2.writer().writeByte(temp.prelevel);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeInt(temp.beri_white);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeShort(temp.att);
                        m2.writer().writeByte(temp.material.length);
                        for (int j = 0; j < temp.material.length; j++) {
                            m2.writer().writeByte(temp.material[j].type);
                            m2.writer().writeByte(temp.material[j].id);
                            m2.writer().writeShort(temp.material[j].quant);
                        }
                    }
                    m2.writer().writeShort(DataTemplate.VerdataUpgradeSave);
                    break;
                }
                case 13: {
                    m2.writer().writeByte(13);
                    m2.writer().writeByte(DataTemplate.mSea.length);
                    for (int i = 0; i < DataTemplate.mSea.length; i++) {
                        for (int j = 0; j < DataTemplate.mSea[i].length; j++) {
                            m2.writer().writeShort(DataTemplate.mSea[i][j]);
                        }
                    }
                    break;
                }
                case 15: {
                    m2.writer().writeByte(15);
                    m2.writer().writeShort(MobTemplate.ENTRYS.size());
                    for (int i = 0; i < MobTemplate.ENTRYS.size(); i++) {
                        MobTemplate temp = MobTemplate.ENTRYS.get(i);
                        m2.writer().writeShort(temp.mob_id);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeShort(temp.level);
                        m2.writer().writeShort(temp.hOne);
                        m2.writer().writeInt(f.setInteger(temp.hp_max));
                        m2.writer().writeByte(temp.typemove);
                        m2.writer().writeByte(temp.ishuman);
                        m2.writer().writeByte(temp.typemonster);
                        if (temp.ishuman == 1) {
                            m2.writer().writeShort(temp.head);
                            m2.writer().writeShort(temp.hair);
                            m2.writer().writeByte(temp.wearing.length);
                            for (int j = 0; j < temp.wearing.length; j++) {
                                if (temp.wearing[j] != -1) {
                                    m2.writer().writeByte(1);
                                    m2.writer().writeShort(temp.wearing[j]);
                                } else {
                                    m2.writer().writeByte(-1);
                                }
                            }
                        } else {
                            m2.writer().writeShort(temp.icon);
                        }
                    }
                    m2.writer().writeShort(DataTemplate.VerdataMon);
                    break;
                }
                case 17: {
                    m2.writer().writeByte(17);
                    m2.writer().writeLong(System.currentTimeMillis());
                    break;
                }
                case 19: {
                    m2.writer().writeByte(19);
                    m2.writer().writeByte(DataTemplate.mTileUpdate.length);
                    for (int i = 0; i < DataTemplate.mTileUpdate.length; i++) {
                        m2.writer().writeShort(DataTemplate.mTileUpdate[i]);
                    }
                    m2.writer().writeByte(DataTemplate.mTileGhepĐa.length);
                    for (int i = 0; i < DataTemplate.mTileGhepĐa.length; i++) {
                        m2.writer().writeShort(DataTemplate.mTileGhepĐa[i]);
                    }
                    break;
                }
                case 21: {
                    m2.writer().writeByte(21);
                    m2.writer().writeByte(0); // h12plus = 0;
                    break;
                }
                case 26: {
                    m2.writer().writeByte(26);
                    m2.writer().writeByte(DataTemplate.AttriKichAn.length);
                    for (int i = 0; i < DataTemplate.AttriKichAn.length; i++) {
                        m2.writer().writeUTF(DataTemplate.AttriKichAn[i]);
                    }
                    m2.writer().writeShort(-31525);
                    break;
                }
                case 28: {
                    m2.writer().writeByte(28);
                    m2.writer().writeShort(ItemTemplate4.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate4.ENTRYS.size(); i++) {
                        ItemTemplate4 temp = ItemTemplate4.ENTRYS.get(i);
                        m2.writer().writeShort(temp.id);
                        m2.writer().writeShort(temp.icon);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeShort(temp.indexInfoPotion);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeByte(temp.istrade);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeShort(temp.timedelay);
                        m2.writer().writeShort(temp.value);
                        m2.writer().writeShort(temp.timeactive);
                        m2.writer().writeUTF(temp.nameuse);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataPotion);
                    break;
                }
                case 27: {
                    m2.writer().writeByte(27);
                    m2.writer().writeByte(0); // isopenDao
                    break;
                }
                case 18:
                case 29: {
                    m2.writer().writeByte(18);
                    m2.writer().writeShort(ItemTemplate8.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate8.ENTRYS.size(); i++) {
                        ItemTemplate8 temp = ItemTemplate8.ENTRYS.get(i);
                        m2.writer().writeShort(temp.id);
                        m2.writer().writeShort(temp.icon);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeUTF(temp.info);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeByte(temp.istrade);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeShort(temp.timedelay);
                        m2.writer().writeShort(temp.value);
                        m2.writer().writeShort(temp.timeactive);
                        m2.writer().writeUTF(temp.nameuse);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataPotionClan);
                    break;
                }
                case 30: {
                    m2.writer().writeByte(30);
                    m2.writer().writeByte(DataTemplate.mEffSpec.length);
                    for (int i = 0; i < DataTemplate.mEffSpec.length; i++) {
                        m2.writer().writeUTF(DataTemplate.mEffSpec[i]);
                    }
                    m2.writer().writeShort(-7547);
                    break;
                }
            }
            if (m2.writer().size() > 0) {
                this.addmsg(m2);
            }
            m2.cleanup();
        }
    }

    public void send_data_from_server(Message m) throws IOException {
        if (this.getImgAPK) {
            return;
        } else {
            getImgAPK = true;
        }
        this.zoomlv = m.reader().readByte();
        Thread send = Thread.ofVirtual().name("SendData-zoomlv-" + this.zoomlv).unstarted(() -> {
            try {
                String path = "data/datafromsver/x" + this.zoomlv;
                File folder = new File(path);
                if (folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    Arrays.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            int name1 = solve_name(o1.getName());
                            int name2 = solve_name(o2.getName());
                            return (name1 > name2) ? 1 : -1;
                        }

                        private int solve_name(String name) {
                            String num = "";
                            for (int i = 0; i < name.length(); i++) {
                                if (name.charAt(i) == '_') {
                                    break;
                                }
                                num += name.charAt(i);
                            }
                            return Integer.parseInt(num);
                        }
                    });
                    for (int i = 0; i < files.length; i++) {
                        int cmd = Integer.parseInt(files[i].getName().substring(
                                (files[i].getName().length() - 3), files[i].getName().length()));
                        Service.send_msg_data(this, cmd, files[i].getAbsolutePath(), false);
                    }
                }
            } catch (IOException e) {
                core.GameLogger.error(
                        "Session.run: IOException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            }
        });
        send.start();
    }

    public void login(Message m) throws IOException {
        byte type = m.reader().readByte();
        String user_ = m.reader().readUTF().replace(" ", "");
        String pass_ = m.reader().readUTF().replace(" ", "");
        this.lock = 0;
        list_char = new ArrayList<>();
        long time_can_login_again = 0;
        if (SessionManager.CLIENT_LOGIN_TIME.containsKey(user_)) {
            long time_can_login = SessionManager.CLIENT_LOGIN_TIME.get(user_);
            if (time_can_login > System.currentTimeMillis()) {
                SessionManager.CLIENT_LOGIN_TIME.replace(user_, time_can_login, time_can_login);
                time_can_login_again = (time_can_login - System.currentTimeMillis()) / 1000;
            } else {
                SessionManager.CLIENT_LOGIN_TIME.remove(user_);
            }
        }
        if (type == 0) {
            // Kiểm tra ký tự hợp lệ
            Pattern p = Pattern.compile("^[a-zA-Z0-9@.]{1,30}$");
            if (!p.matcher(user_).matches() || !p.matcher(pass_).matches()) {
                login_notice("Ký tự không hợp lệ");
                return;
            }
            if (Manager.gI().server_admin) {
                SessionManager.time_login.clear();
            }
            if (SessionManager.time_login.containsKey(user_) && !user_.equals("admin")) {
                long time_login = SessionManager.time_login.get(user_);
                if (time_login > System.currentTimeMillis()) {
                    long time_login_after = (time_login - System.currentTimeMillis()) / 1000;
                    if (time_login_after < 120) {
                        login_after_time(time_login_after);
                    }
                    return;
                }
            }
        } else {
            // Xử lý type khác 0
            if (user_.equals("") && pass_.equals("")) {
                login_notice("Truy cập " + Manager.gI().server_domain + " để đăng ký");
                return;
            } else {
                pass_ = "1"; // Type khác 0 thì gán pass là "1"
            }
        }
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = SQL.gI().getCon();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT * FROM `accounts` WHERE BINARY `user` = '" + user_
                    + "' AND BINARY `pass` = '" + pass_ + "' LIMIT 1;");

            if (!rs.next()) {
                String timeMessage = time_can_login_again > 0 ? ".\nThử lại sau " + time_can_login_again + "s" : "";
                login_notice("Tài khoản mật khẩu không chính xác" + timeMessage);

                if (!SessionManager.CLIENT_LOGIN_TIME.containsKey(user_)) {
                    SessionManager.CLIENT_LOGIN_TIME.put(user_, (System.currentTimeMillis() + 1_000L));
                }
                return;
            }

            this.lock = rs.getByte("lock");
            tong_nap = rs.getLong("tongnap");
            if (this.lock != 0) {
                login_notice("Tài khoản bị khóa, liên hệ admin để biết thêm chi tiết");
                return;
            }
            JSONArray js = (JSONArray) JSONValue.parse(rs.getString("char"));
            for (int i = 0; i < js.size(); i++) {
                list_char.add(js.get(i).toString());
            }
        } catch (SQLException e) {
            GameLogger.error("Login SQL error: ", e);
            return;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                core.GameLogger.error(
                        "Session.run: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            }
        }
        zoomlv = m.reader().readByte();
        this.version = m.reader().readUTF();
        try {
            int ver_ = Integer.parseInt(this.version.replace(".", ""));
            if (ver_ < 124) {
                login_notice("Vui lòng sử dụng phiên bản 1.2.4 trở lên");
                return;
            }
        } catch (NumberFormatException e) {
            core.GameLogger.error(
                    "Session.run: NumberFormatException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            login_notice("Phiên bản client không hợp lệ");
            return;
        }
        m.reader().readByte();
        byte IndexCharSelected = m.reader().readByte();
        this.user = user_;
        this.pass = pass_;

        // [SYNCHRONIZATION] Dùng Lock Striping để tuần tự hóa Login/Logout của cùng một
        // User
        synchronized (SessionManager.getUserLock(this.user)) {
            // Kiểm tra IP Limit: Giới hạn 17 acc/IP
            int count = SessionManager.IP_COUNT.getOrDefault(this.ipAddress, 0);

            int maxAllow = 17;
            if (count >= maxAllow) {
                login_notice("Tối đa " + maxAllow + " acc online trên 1 IP.");
                return;
            }

            if (!this.check_onl()) {
                this.update_onl(1);
            } else {
                GameLogger.session("[Login] User '" + this.user + "' IP " + this.ipAddress
                        + " already online (DB onl=1). Kicking old sessions...");
                List<Session> snapshot;
                synchronized (SessionManager.CLIENT_ENTRYS) {
                    snapshot = new ArrayList<>(SessionManager.CLIENT_ENTRYS);
                }
                for (Session ss : snapshot) {
                    if (ss != null && ss != this && ss.user != null && ss.user.equals(this.user)) {
                        GameLogger
                                .session("[Login] Kicking old session for user '" + this.user + "' IP " + ss.ipAddress);
                        SessionTracker.logKick(ss, "double_login");
                        ss.disconnect();
                    }
                }
                // [CẢI TIẾN] Sau khi kick các session cũ, đánh dấu session mới này là online và
                // TIẾP TỤC luồng vào game.
                this.update_onl(1);
            }

            for (int i = 0; i < list_char.size(); i++) {
                Player p0 = Map.get_player_by_name_allmap(list_char.get(i));
                if (p0 != null) {
                    GameLogger.session(
                            "[Login] Stale player '" + list_char.get(i) + "' found in map for user '" + this.user
                                    + "'. Cleaning up...");
                    if (p0.conn != null && p0.conn != this) {
                        try {
                            p0.conn.disconnect();
                        } catch (Exception e) {
                            GameLogger.warn("[Session] Lỗi ngắt kết nối session cũ khi login: " + e.getMessage());
                        }
                    }
                    // [OFFLINE TRAINING GUARD]
                    // Nếu nhân vật cũ đang treo máy, KHÔNG được xóa khỏi Map/Registry
                    // để họ tiếp tục cày. Họ sẽ được Hijack sau nếu người chơi chọn chính họ.
                    if (p0.isOfflineTraining) {
                        GameLogger.session("[Login] Player '" + p0.name + "' is offline training. Skipping cleanup to maintain AI.");
                        continue; 
                    }

                    if (p0.map != null) {
                        try {
                            p0.map.leave_map(p0, 0);
                        } catch (Exception e) {
                            GameLogger.warn("[Session] Lỗi rời map cho player cũ khi login: " + e.getMessage());
                        }
                    }
                    SessionManager.unregisterPlayer(p0);
                }
            }

            // Gửi dữ liệu cần thiết
            Service.send_lechhead_data(this);
            Service.send_item4_cansell(this);
            Service.send_msg_data(this, 72, "data/msg/login/x2msg_72_638026480840808702", false);
            if (this.zoomlv < 2) {
                Message m22 = new Message(-7);
                try {
                    m22.writer().writeByte(15);
                    m22.writer().writeShort(MobTemplate.ENTRYS.size());

                    for (int i = 0; i < MobTemplate.ENTRYS.size(); i++) {
                        MobTemplate temp = MobTemplate.ENTRYS.get(i);
                        m22.writer().writeShort(temp.mob_id);
                        m22.writer().writeUTF(temp.name);
                        m22.writer().writeShort(temp.level);
                        m22.writer().writeShort(temp.hOne);
                        m22.writer().writeInt(f.setInteger(temp.hp_max));
                        m22.writer().writeByte(temp.typemove);
                        m22.writer().writeByte(temp.ishuman);
                        m22.writer().writeByte(temp.typemonster);

                        if (temp.ishuman == 1) {
                            m22.writer().writeShort(temp.head);
                            m22.writer().writeShort(temp.hair);
                            m22.writer().writeByte(temp.wearing.length);
                            for (int j = 0; j < temp.wearing.length; j++) {
                                if (temp.wearing[j] != -1) {
                                    m22.writer().writeByte(1);
                                    m22.writer().writeShort(temp.wearing[j]);
                                } else {
                                    m22.writer().writeByte(-1);
                                }
                            }
                        } else {
                            m22.writer().writeShort(temp.icon);
                        }
                    }
                    m22.writer().writeShort(DataTemplate.VerdataMon);
                    this.addmsg(m22);
                } catch (IOException e) {
                    core.GameLogger.error(
                            "Session.run: IOException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
                } finally {
                    m22.cleanup();
                }
            }
            log_ip(this.user, this.ipAddress);
            send_list_char();
            Message m2 = new Message(-2);
            addmsg(m2);
            m2.cleanup();
            if (IndexCharSelected != -1) {
                controller.login_into_char_select(IndexCharSelected);
            }
            SessionTracker.logLoginOk(this);
        }
    }

    private void login_after_time(long l) throws IOException {
        Message m = new Message(-69);
        m.writer().writeUTF("Mời bạn đăng nhập lại sau thời gian");
        m.writer().writeShort((int) l);
        SessionTracker.logLoginFail(this, "wait_" + l + "s");
        addmsg(m);
        m.cleanup();
    }

    // private void send_list_char() throws IOException {
    // Message m2 = new Message(-4);
    // m2.writer().writeByte(list_char.size());
    // for (int i = 0; i < list_char.size(); i++) {
    // String name = list_char.get(i);
    // Connection connection = null;
    // PreparedStatement ps = null;
    // ResultSet rs = null;
    // try {
    // connection = SQL.gI().getCon();
    // ps = connection.prepareStatement(
    // "SELECT `clazz`, `level`, `body`, `it_body`, `fashion`, `site`, `tocSuper`
    // FROM `players` WHERE `name` = '"
    // + name + "' LIMIT 1;");
    // rs = ps.executeQuery();
    // while (rs.next()) {
    // List<ItemFashionP2> fashion = new ArrayList<>();
    // List<ItemFashionP> itfashionP = new ArrayList<>();
    // JSONArray js0 = (JSONArray) JSONValue.parse(rs.getString("fashion"));
    // JSONArray js_temp_2 = (JSONArray) JSONValue.parse(js0.get(0).toString());
    // for (int i0 = 0; i0 < js_temp_2.size(); i0++) {
    // JSONArray js_temp = (JSONArray)
    // JSONValue.parse(js_temp_2.get(i0).toString());
    // ItemFashionP tempf = new ItemFashionP();
    // tempf.category = Byte.parseByte(js_temp.get(0).toString());
    // tempf.id = Short.parseShort(js_temp.get(1).toString());
    // tempf.icon = Short.parseShort(js_temp.get(2).toString());
    // tempf.is_use = Byte.parseByte(js_temp.get(3).toString()) == 1;
    // itfashionP.add(tempf);
    // }
    // js_temp_2.clear();
    // js_temp_2 = (JSONArray) JSONValue.parse(js0.get(1).toString());
    // for (int i0 = 0; i0 < js_temp_2.size(); i0++) {
    // JSONArray js_temp = (JSONArray)
    // JSONValue.parse(js_temp_2.get(i0).toString());
    // ItemFashionP2 tempf = new ItemFashionP2();
    // tempf.id = Short.parseShort(js_temp.get(0).toString());
    // tempf.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
    // fashion.add(tempf);
    // }
    // js0.clear();
    // short hair_ = -1;
    // short head_ = -1;
    // short[] fashion_ = null;
    // for (int i0 = 0; i0 < fashion.size(); i0++) {
    // if (fashion.get(i0).is_use) {
    // ItemFashion temp = ItemFashion.get_item(fashion.get(i0).id);
    // if (temp != null) {
    // fashion_ = temp.mWearing;
    // break;
    // }
    // }
    // }
    // if (fashion_ != null && fashion_[6] != -1) {
    // // hair_ = -2;
    // hair_ = fashion_[7];
    // head_ = fashion_[6];
    // } else {
    // for (int i0 = 0; i0 < itfashionP.size(); i0++) {
    // if (itfashionP.get(i0).category == 103 && itfashionP.get(i0).is_use) {
    // hair_ = itfashionP.get(i0).icon;
    // }
    // }
    // for (int i0 = 0; i0 < itfashionP.size(); i0++) {
    // if (itfashionP.get(i0).category == 108 && itfashionP.get(i0).is_use) {
    // head_ = itfashionP.get(i0).icon;
    // }
    // }
    // }
    // if (hair_ == 772) {
    // hair_ += rs.getInt("tocSuper");
    // }
    // //
    // m2.writer().writeShort(i);
    // m2.writer().writeUTF(name);
    // m2.writer().writeByte(rs.getByte("clazz"));
    // JSONArray js_level = (JSONArray) JSONValue.parse(rs.getString("level"));
    // m2.writer().writeShort(Short.parseShort(js_level.get(0).toString()));
    // JSONArray js = (JSONArray) JSONValue.parse(rs.getString("body"));
    // m2.writer().writeShort(
    // (head_ != -1) ? head_ : Short.parseShort(js.get(0).toString()));
    // m2.writer().writeShort(
    // (hair_ != -1) ? hair_ : Short.parseShort(js.get(1).toString()));
    // js.clear();
    // m2.writer().writeShort(Clan.get_icon_clan(name)); // clan
    // m2.writer().writeByte(6);
    // //
    // Item_wear[] it = new Item_wear[8];
    // js = (JSONArray) JSONValue.parse(rs.getString("it_body"));
    // for (int i1 = 0; i1 < js.size(); i1++) {
    // JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i1).toString());
    // Item_wear temp = new Item_wear();
    // Item.readUpdateItem(js2.toString(), temp);
    // it[temp.index] = temp;
    // }
    // js.clear();
    // js = (JSONArray) JSONValue.parse(rs.getString("site"));
    // boolean is_show_hat = Byte.parseByte(js.get(6).toString()) == 1;
    // js.clear();
    // //
    // for (int j = 0; j < 6; j++) {
    // if (it[j] == null) {
    // m2.writer().writeByte(0);
    // } else {
    // m2.writer().writeByte(1);
    // if (j == 1 && !is_show_hat) {
    // m2.writer().writeShort(-1);
    // } else if (fashion_ != null && fashion_[j] != -1) {
    // m2.writer().writeShort(fashion_[j]);
    // } else {
    // m2.writer().writeShort(
    // ItemTemplate3.get_it_by_id(it[j].template.id).part);
    // }
    // }
    // }
    // m2.writer().writeByte(0);
    // }
    // } catch (SQLException e) {
    // core.GameLogger.printStackTrace(e);
    // } finally {
    // try {
    // if (rs != null) {
    // rs.close();
    // }
    // if (ps != null) {
    // ps.close();
    // }
    // if (connection != null) {
    // connection.close();
    // }
    // } catch (SQLException e) {
    // core.GameLogger.printStackTrace(e);
    // }
    // }
    // }
    // addmsg(m2);
    // m2.cleanup();
    // }
    private void send_list_char() throws IOException {
        Message m2 = new Message(-4);
        m2.writer().writeByte(list_char.size());

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = SQL.gI().getCon();
            ps = connection.prepareStatement(
                    "SELECT `clazz`, `level`, `body`, `it_body`, `fashion`, `site`, `tocSuper` " +
                            "FROM `players` WHERE `name` = ? LIMIT 1");

            for (int i = 0; i < list_char.size(); i++) {
                String name = list_char.get(i);
                rs = null;
                try {
                    ps.setString(1, name); // Tránh SQL Injection
                    rs = ps.executeQuery();

                    if (!rs.next())
                        continue; // Dùng if thay vì while vì LIMIT 1

                    List<ItemFashionP2> fashion = new ArrayList<>();
                    List<ItemFashionP> itfashionP = new ArrayList<>();

                    JSONArray js0 = (JSONArray) JSONValue.parse(rs.getString("fashion"));
                    JSONArray js_temp_2 = (JSONArray) JSONValue.parse(js0.get(0).toString());
                    for (int i0 = 0; i0 < js_temp_2.size(); i0++) {
                        JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i0).toString());
                        ItemFashionP tempf = new ItemFashionP();
                        tempf.category = Byte.parseByte(js_temp.get(0).toString());
                        tempf.id = Short.parseShort(js_temp.get(1).toString());
                        tempf.icon = Short.parseShort(js_temp.get(2).toString());
                        tempf.is_use = Byte.parseByte(js_temp.get(3).toString()) == 1;
                        itfashionP.add(tempf);
                    }

                    js_temp_2 = (JSONArray) JSONValue.parse(js0.get(1).toString());
                    for (int i0 = 0; i0 < js_temp_2.size(); i0++) {
                        JSONArray js_temp = (JSONArray) JSONValue.parse(js_temp_2.get(i0).toString());
                        ItemFashionP2 tempf = new ItemFashionP2();
                        tempf.id = Short.parseShort(js_temp.get(0).toString());
                        tempf.is_use = Byte.parseByte(js_temp.get(1).toString()) == 1;
                        fashion.add(tempf);
                    }

                    short hair_ = -1;
                    short head_ = -1;
                    short[] fashion_ = null;

                    for (int i0 = 0; i0 < fashion.size(); i0++) {
                        if (fashion.get(i0).is_use) {
                            ItemFashion temp = ItemFashion.get_item(fashion.get(i0).id);
                            if (temp != null) {
                                fashion_ = temp.mWearing;
                                break;
                            }
                        }
                    }

                    // Kiểm tra null và độ dài mảng trước khi truy cập index
                    if (fashion_ != null && fashion_.length > 7 && fashion_[6] != -1) {
                        hair_ = fashion_[7];
                        head_ = fashion_[6];
                    } else {
                        for (int i0 = 0; i0 < itfashionP.size(); i0++) {
                            if (itfashionP.get(i0).category == 103 && itfashionP.get(i0).is_use) {
                                hair_ = itfashionP.get(i0).icon;
                            }
                        }
                        for (int i0 = 0; i0 < itfashionP.size(); i0++) {
                            if (itfashionP.get(i0).category == 108 && itfashionP.get(i0).is_use) {
                                head_ = itfashionP.get(i0).icon;
                            }
                        }
                    }

                    if (hair_ == 772) {
                        hair_ += rs.getInt("tocSuper");
                    }

                    m2.writer().writeShort(i);
                    m2.writer().writeUTF(name);
                    m2.writer().writeByte(rs.getByte("clazz"));

                    JSONArray js_level = (JSONArray) JSONValue.parse(rs.getString("level"));
                    m2.writer().writeShort(Short.parseShort(js_level.get(0).toString()));

                    JSONArray js = (JSONArray) JSONValue.parse(rs.getString("body"));
                    m2.writer().writeShort((head_ != -1) ? head_ : Short.parseShort(js.get(0).toString()));
                    m2.writer().writeShort((hair_ != -1) ? hair_ : Short.parseShort(js.get(1).toString()));
                    js.clear();
                    m2.writer().writeShort(Clan.get_icon_clan(name));
                    m2.writer().writeByte(6);

                    Item_wear[] it = new Item_wear[8];
                    js = (JSONArray) JSONValue.parse(rs.getString("it_body"));
                    for (int i1 = 0; i1 < js.size(); i1++) {
                        JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i1).toString());
                        Item_wear temp = new Item_wear();
                        Item.readUpdateItem(js2.toString(), temp);
                        it[temp.index] = temp;
                    }
                    js.clear();
                    js = (JSONArray) JSONValue.parse(rs.getString("site"));
                    boolean is_show_hat = Byte.parseByte(js.get(6).toString()) == 1;
                    js.clear();
                    for (int j = 0; j < 6; j++) {
                        if (it[j] == null) {
                            m2.writer().writeByte(0);
                        } else {
                            m2.writer().writeByte(1);
                            if (j == 1 && !is_show_hat) {
                                m2.writer().writeShort(-1);
                            } else if (fashion_ != null && fashion_.length > j && fashion_[j] != -1) {
                                m2.writer().writeShort(fashion_[j]);
                            } else {
                                m2.writer().writeShort(
                                        ItemTemplate3.get_it_by_id(it[j].template.id).part);
                            }
                        }
                    }
                    m2.writer().writeByte(0);

                } catch (SQLException e) {
                    core.GameLogger.error(
                            "Session.run: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException e) {
                            core.GameLogger.error("Session.run: SQLException for user " + user + " IP " + ipAddress
                                    + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            core.GameLogger
                    .error("Session.run: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                core.GameLogger.error(
                        "Session.run: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            }
        }

        addmsg(m2);
        m2.cleanup();
    }

    private void login_notice(String s) throws IOException {
        Message m = new Message(-11);
        m.writer().writeShort(0);
        m.writer().writeByte(0);
        m.writer().writeUTF("Thông báo");
        m.writer().writeUTF(s);
        m.writer().writeByte(0);
        SessionTracker.logLoginFail(this, s);
        addmsg(m);
        m.cleanup();
    }

    private void log_ip(String user, String ip) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = SQL.gI().getCon();
            ps = conn.prepareStatement("UPDATE `accounts` SET `ip_log` = ? WHERE `user` = ? LIMIT 1");
            ps.setString(1, ip);
            ps.setString(2, user);
            ps.executeUpdate();
        } catch (SQLException e) {
            core.GameLogger
                    .error("Session.log_ip: SQLException for user " + user + " IP " + ip + ": " + e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                core.GameLogger
                        .error("Session.log_ip: SQLException for user " + user + " IP " + ip + ": " + e.getMessage());
            }
        }
    }

    public void ReadPartNew(Message m2) throws IOException {
        short index = m2.reader().readShort();
        Part part = Part.get_part(index);
        if (part != null) {
            Message m = new Message(-82);
            m.writer().writeShort(index);
            m.writer().writeByte(part.type);
            for (int i = 0; i < part.pi.length; i++) {
                m.writer().writeShort(part.pi[i].id);
                m.writer().writeByte(part.pi[i].dx);
                m.writer().writeByte(part.pi[i].dy);
            }
            addmsg(m);
            m.cleanup();
        }
    }

    public void create_char(Message m2) throws IOException {
        if (list_char.size() >= 1) {
            login_notice("Chỉ có thể tạo tối đa 1 nhân vật!");
            return;
        }
        String name = m2.reader().readUTF().replace(" ", "");
        name = name.toLowerCase();
        Pattern p = Pattern.compile("^[a-zA-Z0-9]{6,10}$");
        if (!p.matcher(name).matches()) {
            if (name.length() < 6 || name.length() > 10) {
                login_notice("Tên có độ dài từ 6 đến 10 ký tự");
            } else if (!name.matches("^[a-zA-Z0-9]+$")) {
                login_notice("Tên chỉ được chứa chữ cái và số");
            } else {
                login_notice("Tên không hợp lệ, hãy thử lại.");
            }
            return;
        }

        byte clazz = m2.reader().readByte();
        int head = m2.reader().readShort();
        int hair = m2.reader().readShort();
        Connection connection = null;
        Statement st = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            String query = "INSERT INTO `players` (`name`, `body`, `level`, `clazz`, `point_inven`, `site`, `bag3`, `it_body`, `potential`,"
                    + " `bag47`, `rms`, `skill`, `friend`, `enemy`, `fashion`, `eff`, `box3`, `box47`, `quest`, `date`,"
                    + " `pvppoint`, `save_it3`, `save_it47`, `hanhtrinh`, `wanted_point`, `wanted_chest`, `mypet`, `second_body`, `danhhieu`, `tu_hanh`,"
                    + " `third_body`, `disciple_data`) "
                    + "VALUES ('%s', '%s', '%s', %s, '%s', '%s', '%s','%s','%s','%s','%s','%s','%s','%s','%s','%s',"
                    + "'%s','%s','%s','%s', %s, '%s', '%s', '%s', %s, '%s', '%s', '%s', '%s', '%s', '%s', 'null')";
            String body_wear = "";
            String skill_by_clazz = "";
            String fashion_ = "";
            switch (clazz) {
                case 1: {
                    body_wear = "[[0,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[5,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[10,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]";
                    skill_by_clazz = "[[0,0,0,0],[20,0,0,0],[40,0,0,0],[487,-1,0,0],[300,-1,0,0],[305,-1,0,0],[310,-1,0,0],[315,-1,0,0],[320,-1,0,0],[325,-1,0,0],[552,-1,0,0],[557,-1,0,0],[667,1,0,0],[673,1,0,0],[679,1,0,0]]";
                    fashion_ = "[[[103,5,1,1],[108,0,0,1]],[],[[1,1],[5,1],[3,1],[7,1]]]";
                    break;
                }
                case 2: {
                    body_wear = "[[1,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[6,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[11,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]";
                    skill_by_clazz = "[[60,0,0,0],[80,0,0,0],[100,0,0,0],[492,-1,0,0],[300,-1,0,0],[305,-1,0,0],[310,-1,0,0],[315,-1,0,0],[320,-1,0,0],[325,-1,0,0],[552,-1,0,0],[557,-1,0,0],[667,1,0,0],[673,1,0,0],[679,1,0,0]]";
                    fashion_ = "[[[103,1,24,1],[108,0,0,1]],[],[[1,1],[5,1],[3,1],[7,1]]]";
                    break;
                }
                case 3: {
                    body_wear = "[[2,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[7,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[12,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]";
                    skill_by_clazz = "[[120,0,0,0],[140,0,0,0],[160,0,0,0],[497,-1,0,0],[300,-1,0,0],[305,-1,0,0],[310,-1,0,0],[315,-1,0,0],[320,-1,0,0],[325,-1,0,0],[552,-1,0,0],[557,-1,0,0],[667,1,0,0],[673,1,0,0],[679,1,0,0]]";
                    fashion_ = "[[[103,2,28,1],[108,0,0,1]],[],[[1,1],[5,1],[3,1],[7,1]]]";
                    break;
                }
                case 4: {
                    body_wear = "[[3,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[8,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[13,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]";
                    skill_by_clazz = "[[180,0,0,0],[200,0,0,0],[220,0,0,0],[502,-1,0,0],[300,-1,0,0],[305,-1,0,0],[310,-1,0,0],[315,-1,0,0],[320,-1,0,0],[325,-1,0,0],[552,-1,0,0],[557,-1,0,0],[667,1,0,0],[673,1,0,0],[679,1,0,0]]";
                    fashion_ = "[[[103,3,32,1],[108,0,0,1]],[],[[1,1],[5,1],[3,1],[7,1]]]";
                    break;
                }
                case 5: {
                    body_wear = "[[4,0,1,-1,0,1,0,-1,[[5,10],[6,10],[7,10]],[],0,[],0],[9,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],3],[14,0,1,-1,0,0,0,-1,[[5,10],[6,10],[7,10]],[],0,[],5]]";
                    skill_by_clazz = "[[240,0,0,0],[260,0,0,0],[280,0,0,0],[507,-1,0,0],[300,-1,0,0],[305,-1,0,0],[310,-1,0,0],[315,-1,0,0],[320,-1,0,0],[325,-1,0,0],[552,-1,0,0],[557,-1,0,0],[667,1,0,0],[673,1,0,0],[679,1,0,0]]";
                    fashion_ = "[[[103,4,36,1],[108,0,0,1]],[],[[1,1],[5,1],[3,1],[7,1]]]";
                    break;
                }
            }
            query = String.format(query, name, "[" + head + "," + hair + "]", "[1,0,0]", clazz,
                    "[1000000,5000,0,0,0,0,0,0,7,0,0,0]", "[0,0,-1,-1,300,300,1,0,20,0,3,0,3,0]",
                    "[]", body_wear, "[5,1,1,1,1,1,0,[]]", "[[4,1007,1],[4,1046,1]]",
                    "[[],[],[],[],[0,18],[],[],[],[],[],[]]", skill_by_clazz, "[]", "[]", fashion_,
                    "[]", "[]", "[]", "[[1,[]]]", DateTime.now(), 0, "[]", "[]", "[]", 0, "[]",
                    "[]", "[]", "[]", "[0,0]", "[]");
            st.execute(query);
        } catch (SQLException e) {
            core.GameLogger.error(
                    "Session.create_char: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            login_notice("Tên đã tồn tại");
            return;
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                core.GameLogger.error("Session.create_char: SQLException for user " + user + " IP " + ipAddress + ": "
                        + e.getMessage());
            }
        }
        list_char.add(name);
        flush();
        send_list_char();
    }

    @SuppressWarnings("unchecked")
    public void flush() {
        JSONArray js = new JSONArray();
        for (int i = 0; i < list_char.size(); i++) {
            js.add(list_char.get(i));
        }
        Connection conn = null;
        Statement st = null;
        try {
            conn = SQL.gI().getCon();
            st = conn.createStatement();
            st.executeUpdate("UPDATE `accounts` SET `char` = '" + js.toJSONString()
                    + "' WHERE BINARY `user` = '" + this.user + "' AND BINARY `pass` = '"
                    + this.pass + "';");
        } catch (SQLException e) {
            core.GameLogger
                    .error("Session.flush: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                core.GameLogger.error(
                        "Session.flush: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            }
        }
    }

    public synchronized void update_onl(int type) {
        Connection connection = null;
        Statement st = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            st.executeUpdate(
                    "UPDATE `accounts` SET `onl` = " + type + " WHERE `user` = '" + this.user + "' AND `pass` = '"
                            + this.pass + "' LIMIT 1;");
        } catch (SQLException e) {
            core.GameLogger.error(
                    "Session.update_onl: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                core.GameLogger.error("Session.update_onl: SQLException for user " + user + " IP " + ipAddress + ": "
                        + e.getMessage());
            }
        }
    }

    public boolean check_onl() {
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = SQL.gI().getCon();
            st = connection.createStatement();
            rs = st.executeQuery("SELECT `onl` FROM `accounts` WHERE BINARY `user` = '" + this.user
                    + "' AND BINARY `pass` = '" + this.pass + "' LIMIT 1;");
            if (!rs.next()) {
                return false;
            }
            if (rs.getBoolean("onl")) {
                return true;
            }
        } catch (SQLException e) {
            core.GameLogger.error(
                    "Session.check_onl: SQLException for user " + user + " IP " + ipAddress + ": " + e.getMessage());
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                core.GameLogger.error("Session.check_onl: SQLException for user " + user + " IP " + ipAddress + ": "
                        + e.getMessage());
            }
        }
        return false;
    }

    public void clear_network(Session ss) {
        if (ss == null || ss.isDummy || ss.socket == null) {
            return;
        }
        if (ss.sendd != null) {
            ss.sendd.interrupt();
            ss.sendd = null;
        }
        if (ss.receiv != null) {
            ss.receiv.interrupt();
            ss.receiv = null;
        }
        try {
            if (!ss.socket.isClosed()) {
                ss.socket.close();
            }
        } catch (IOException e) {
            core.GameLogger.error("Session.clear_network: IOException for session " + ss.user + " IP " + ss.ipAddress
                    + ": " + e.getMessage());
        }
    }

    public void Check_Data_Ver() throws IOException {
        Message m = new Message(-6);
        m.writer().writeShort(DataTemplate.VerdataMon);
        m.writer().writeShort(DataTemplate.VerdataPotion);
        m.writer().writeShort(DataTemplate.VerdataAttri);
        m.writer().writeShort(-1);
        m.writer().writeShort(DataTemplate.VerdataNameMap);
        m.writer().writeShort(DataTemplate.VerdataNamePotionquest);
        m.writer().writeShort(-1);
        m.writer().writeShort(DataTemplate.VerdataImageSave);
        m.writer().writeShort(DataTemplate.VerdataUpgradeSave);
        m.writer().writeShort(DataTemplate.VerdataPotionClan);
        m.writer().writeShort(-1);
        addmsg(m);
        m.cleanup();
        if (this instanceof WSSession) {
            // WebGL client loads -7 data via HTTP, so we skip sending it over WebSocket
            return;
        }
        // send item7
        m = new Message(-7);
        m.writer().writeByte(11);
        m.writer().writeByte(ItemTemplate7.ENTRYS.size());
        for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
            ItemTemplate7 temp = ItemTemplate7.ENTRYS.get(i);
            m.writer().writeByte(temp.id);
            m.writer().writeUTF(temp.name);
            m.writer().writeByte(temp.type);
            m.writer().writeByte(temp.icon);
            m.writer().writeInt(temp.price);
            m.writer().writeShort(temp.priceruby);
            m.writer().writeByte(temp.istrade);
        }
        addmsg(m);
        m.cleanup();
    }
}