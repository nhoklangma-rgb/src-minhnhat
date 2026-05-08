package io;

import core.GameLogger;

/**
 * SessionTracker - High-level utility for logging session lifecycle events.
 */
public class SessionTracker {

    public static final String CONNECT = "CONNECT";
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String DISCONNECT = "DISCONNECT";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String KICK = "KICK";

    public static void logConnect(Session ss) {
        if (ss == null) return;
        GameLogger.sessionEvent(CONNECT, ss.id, -1, "ip=" + ss.ipAddress);
    }

    public static void logLoginOk(Session ss) {
        if (ss == null) return;
        int pid = (ss.p != null) ? ss.p.id : -1;
        GameLogger.sessionEvent(LOGIN_OK, ss.id, pid, "user=" + ss.user);
    }

    public static void logLoginFail(Session ss, String reason) {
        if (ss == null) return;
        GameLogger.sessionEvent(LOGIN_FAIL, ss.id, -1, "reason=" + reason);
    }

    public static void logDisconnect(Session ss, String reason) {
        if (ss == null) return;
        int pid = (ss.p != null) ? ss.p.id : -1;
        GameLogger.sessionEvent(DISCONNECT, ss.id, pid, "reason=" + reason);
    }

    public static void logTimeout(Session ss, String reason) {
        if (ss == null) return;
        int pid = (ss.p != null) ? ss.p.id : -1;
        GameLogger.sessionEvent(TIMEOUT, ss.id, pid, "reason=" + reason);
    }

    public static void logKick(Session ss, String reason) {
        if (ss == null) return;
        int pid = (ss.p != null) ? ss.p.id : -1;
        GameLogger.sessionEvent(KICK, ss.id, pid, "reason=" + reason);
    }
}
