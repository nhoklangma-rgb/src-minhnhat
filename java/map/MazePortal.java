package map;

public class MazePortal {
    public String dir;
    public short x, y;
    public int targetNode;
    public short teleX, teleY;

    public MazePortal(String dir, short x, short y, int targetNode, short teleX, short teleY) {
        this.dir = dir;
        this.x = x;
        this.y = y;
        this.targetNode = targetNode;
        this.teleX = teleX;
        this.teleY = teleY;
    }
}
