package io;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import core.GameLogger;

public class WSServer extends WebSocketServer {

    private final ConcurrentHashMap<WebSocket, WSSession> sessions = new ConcurrentHashMap<>();

    public WSServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        WSSession session = new WSSession(conn);
        if (SessionManager.client_connect(session)) {
            sessions.put(conn, session);
            GameLogger.session("[WS] New connection from " + conn.getRemoteSocketAddress());
        } else {
            conn.close();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WSSession session = sessions.remove(conn);
        if (session != null) {
            session.disconnect();
            GameLogger.session("[WS] Closed connection: " + conn.getRemoteSocketAddress() + " (Reason: " + reason + ")");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // We expect binary frames, but if we receive string, we ignore or log
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        WSSession session = sessions.get(conn);
        if (session != null) {
            byte[] data = new byte[message.remaining()];
            message.get(data);
            session.onReceive(data);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            WSSession session = sessions.remove(conn);
            if (session != null) {
                session.disconnect();
            }
        }
        GameLogger.error("[WS] Error on connection " + (conn != null ? conn.getRemoteSocketAddress() : "unknown") + ": " + ex.getMessage());
    }

    @Override
    public void onStart() {
        GameLogger.info("[WS] WebSocket Server started on port " + getPort());
        setConnectionLostTimeout(30); // 30 seconds heartbeat for production stability
    }
    
    public void stopServer() {
        try {
            this.stop(5000);
            GameLogger.info("[WS] WebSocket Server stopped.");
        } catch (InterruptedException e) {
            GameLogger.error("[WS] Error stopping WebSocket Server: " + e.getMessage());
        }
    }
}
