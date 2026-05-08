package core;

public class Start {
    public static void main(String[] args) {
        ServerManager.gI().init();
        CpuMonitor.gI().start();
    }
}
