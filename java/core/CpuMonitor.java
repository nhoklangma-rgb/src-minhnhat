package core;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.sun.management.OperatingSystemMXBean;

/**
 * CpuMonitor - Periodically monitors CPU load and logs thread dumps on spikes.
 * Also performs deadlock detection.
 */
public class CpuMonitor {

    private static CpuMonitor instance;
    private ScheduledExecutorService scheduler;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;

    private CpuMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    public static CpuMonitor gI() {
        if (instance == null) {
            instance = new CpuMonitor();
        }
        return instance;
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CpuMonitor-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::check, 30, 30, TimeUnit.SECONDS);
        GameLogger.info("[CpuMonitor] Service started (Interval: 30s)");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
            GameLogger.info("[CpuMonitor] Service stopped");
        }
    }

    private void check() {
        try {
            // 1. Check Deadlocks
            long[] deadlockedIds = threadBean.findDeadlockedThreads();
            if (deadlockedIds != null && deadlockedIds.length > 0) {
                StringBuilder sb = new StringBuilder("DEADLOCK|threads=");
                ThreadInfo[] infos = threadBean.getThreadInfo(deadlockedIds);
                for (int i = 0; i < infos.length; i++) {
                    if (infos[i] != null) {
                        sb.append(infos[i].getThreadName());
                        if (i < infos.length - 1) sb.append(", ");
                    }
                }
                GameLogger.error(sb.toString());
            }

            // 2. Check CPU Load
            double load = osBean.getSystemCpuLoad();
            if (load < 0) {
                return; // Measurement not available
            }

            double percent = load * 100;
            if (percent > 80) {
                int threadCount = threadBean.getThreadCount();
                GameLogger.warn(String.format("CPU_SPIKE|load=%.2f|threads=%d", percent, threadCount));

                // Dump all threads with stack traces
                ThreadInfo[] threadInfos = threadBean.dumpAllThreads(false, false);
                for (ThreadInfo ti : threadInfos) {
                    if (ti == null) continue;

                    StackTraceElement[] stack = ti.getStackTrace();
                    String topFrame = (stack.length > 0) ? stack[0].toString() : "N/A";

                    GameLogger.error(String.format("THREAD|name=%s|state=%s|at=%s",
                            ti.getThreadName(), ti.getThreadState(), topFrame));

                    // Log up to 5 stack frames total (including the top one)
                    for (int i = 1; i < Math.min(stack.length, 5); i++) {
                        GameLogger.error("  at " + stack[i].toString());
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("[CpuMonitor] Error in monitoring job: " + e.getMessage());
        }
    }
}
