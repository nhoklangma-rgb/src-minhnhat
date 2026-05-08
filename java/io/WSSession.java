package io;

import org.java_websocket.WebSocket;
import java.io.IOException;
import java.nio.ByteBuffer;
import core.GameLogger;

public class WSSession extends Session {
    private final WebSocket ws;
    private java.util.List<Byte> receiveBuffer = new java.util.ArrayList<>();

    public WSSession(WebSocket ws) {
        super(true); // isDummy = true
        this.ws = ws;
    }

    @Override
    public void init() {
        this.connected = true;
        this.ipAddress = ws.getRemoteSocketAddress().getAddress().getHostAddress();
        
        // Start the send thread (reusing Session's send logic)
        this.sendd = new Thread(() -> {
            try {
                while (connected) {
                    while (!list_msg.isEmpty()) {
                        Message m = list_msg.remove(0);
                        if (m != null) {
                            send_msg_ws(m);
                            m.cleanup();
                        }
                    }
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                // Ignore
            } catch (Exception e) {
                GameLogger.error("WSSession-Send error: " + e.getMessage());
            } finally {
                this.disconnect();
            }
        }, "WSSession-Send-" + this.ipAddress);
        this.sendd.start();
    }

    @Override
    public void disconnect() {
        synchronized (this) {
            if (!connected) return;
            this.connected = false;
        }
        try {
            if (ws != null && ws.isOpen()) {
                ws.close();
            }
        } catch (Exception e) {}
        super.disconnect();
    }

    private void send_msg_ws(Message msg) throws IOException {
        byte[] data = msg.getData();
        int totalLen = 1 + (data != null ? (msg.cmd == -39 || msg.cmd == -101 || msg.cmd == -93 || msg.cmd == 76 ? 4 : 2) + data.length : 2);
        ByteBuffer buf = ByteBuffer.allocate(totalLen);

        // Opcode
        if (sendKeyComplete && msg.cmd != -27) {
            buf.put(writeKey(msg.cmd));
        } else {
            buf.put(msg.cmd);
        }

        // Length & Data
        if (data != null) {
            int size = data.length;
            if (sendKeyComplete) {
                if (msg.cmd == -39 || msg.cmd == -101 || msg.cmd == -93 || msg.cmd == 76) {
                    buf.put(writeKey((byte) (size >> 24)));
                    buf.put(writeKey((byte) (size >> 16)));
                    buf.put(writeKey((byte) (size >> 8)));
                    buf.put(writeKey((byte) (size)));
                } else {
                    buf.put(writeKey((byte) (size >> 8)));
                    buf.put(writeKey((byte) (size)));
                }
                
                if (sendKeyComplete && msg.cmd != -27) {
                    byte[] encData = new byte[size];
                    for (int i = 0; i < size; i++) {
                        encData[i] = writeKey(data[i]);
                    }
                    buf.put(encData);
                } else {
                    buf.put(data);
                }
            } else {
                if (msg.cmd == -39) {
                    buf.putInt(size);
                } else {
                    buf.putShort((short) size);
                }
                buf.put(data);
            }
        } else {
            buf.putShort((short) 0);
        }

        buf.flip();
        ws.send(buf);
    }

    // Called by WSServer when a binary frame is received
    public void onReceive(byte[] rawData) {
        for (byte b : rawData) {
            receiveBuffer.add(b);
        }
        
        while (receiveBuffer.size() > 0) {
            int pos = 0;
            int savedCurR = this.curR;
            
            try {
                if (receiveBuffer.size() < pos + 1) break;
                byte cmd = receiveBuffer.get(pos++);
                if (sendKeyComplete && cmd != -27) cmd = readKey(cmd);
                
                int size = 0;
                if (sendKeyComplete && cmd != -27) {
                    if (receiveBuffer.size() < pos + 2) { this.curR = (byte)savedCurR; break; }
                    size = ((readKey(receiveBuffer.get(pos++)) & 0xFF) << 8 | (readKey(receiveBuffer.get(pos++)) & 0xFF));
                } else {
                    if (receiveBuffer.size() < pos + 2) break;
                    size = ((receiveBuffer.get(pos++) & 0xFF) << 8 | (receiveBuffer.get(pos++) & 0xFF));
                }
                
                if (receiveBuffer.size() < pos + size) {
                    if (sendKeyComplete && cmd != -27) this.curR = (byte)savedCurR;
                    break;
                }
                
                byte[] data = new byte[size];
                for (int i = 0; i < size; i++) {
                    byte b = receiveBuffer.get(pos++);
                    if (sendKeyComplete && cmd != -27) b = readKey(b);
                    data[i] = b;
                }
                
                // Remove processed data
                for (int i = 0; i < pos; i++) {
                    receiveBuffer.remove(0);
                }
                
                Message m = new Message(cmd, data);
                if (m.cmd == -27) {
                    sendkeys();
                } else if (sendKeyComplete) {
                    if (controller != null) {
                        controller.process_msg(m);
                    }
                }
            } catch (Exception e) {
                GameLogger.error("WSSession onReceive error: " + e.getMessage());
                this.disconnect();
                break;
            }
        }
    }
    
    @Override
    public void sendkeys() throws IOException {
        Message msg = new Message(-27);
        msg.writer().writeByte(KEYS.length);
        msg.writer().writeByte(KEYS[0]);
        for (int i = 1; i < KEYS.length; i++) {
            msg.writer().writeByte(KEYS[i] ^ KEYS[i - 1]);
        }
        send_msg_ws(msg);
        msg.cleanup();
        sendKeyComplete = true;
    }
}
