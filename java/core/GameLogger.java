package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GameLogger - Central logging utility for HTTH Server.
 * Routes logs to three distinct streams: SYSTEM, GAME, and SESSION.
 */
public class GameLogger {

    private static final Logger systemLogger = LoggerFactory.getLogger(GameLogger.class);
    private static final Logger gameLogger = LoggerFactory.getLogger("GAME");
    private static final Logger sessionLogger = LoggerFactory.getLogger("SESSION");

    // === SYSTEM LOGS (Startup, Errors, Core Logic) ===
    
    public static void info(String message) {
        if (Manager.gI().enable_system_log) {
            systemLogger.info(message);
        }
    }

    public static void info(String message, Throwable t) {
        if (Manager.gI().enable_system_log) {
            systemLogger.info(message, t);
        }
    }

    public static void warn(String message) {
        if (Manager.gI().enable_warn_log) {
            systemLogger.warn(message);
        }
    }

    public static void warn(String message, Throwable t) {
        if (Manager.gI().enable_warn_log) {
            systemLogger.warn(message, t);
        }
    }

    public static void error(String message) {
        systemLogger.error(message);
    }

    public static void error(String message, Throwable t) {
        systemLogger.error(message, t);
    }

    /**
     * Replacement for legacy e.printStackTrace()
     */
    public static void printStackTrace(Throwable t) {
        systemLogger.error("Unhandle exception: ", t);
    }

    // === GAME DATA LOGS (Combat, Item usage, Logic updates) ===

    public static void game(String message) {
        if (Manager.gI().enable_game_log) {
            gameLogger.info(message);
        }
    }

    public static void gameError(String message, Throwable t) {
        gameLogger.error(message, t);
    }

    // === SESSION LOGS (Connection, Packets, Auth) ===

    public static void session(String message) {
        if (Manager.gI().enable_session_log) {
            sessionLogger.info(message);
        }
    }

    public static void sessionError(String message, Throwable t) {
        sessionLogger.error(message, t);
    }

    /**
     * Logs a structured session event.
     * Format: SESSION|{EVENT}|sid={sid}|pid={pid}|{extra}
     */
    public static void sessionEvent(String event, int sid, int pid, String extra) {
        if (Manager.gI().enable_session_log) {
            String log = String.format("SESSION|%s|sid=%d|pid=%d|%s", event, sid, pid, extra != null ? extra : "");
            sessionLogger.info(log);
        }
    }
}
