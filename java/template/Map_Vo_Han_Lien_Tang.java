package template;

import java.util.List;
import java.util.HashMap;
import map.Mob;

public class Map_Vo_Han_Lien_Tang {
    public int floor;
    public long time_next_floor;
    public List<Mob> mobs;
    public boolean floor_cleared;
    public long time;
    public HashMap<Mob, Long> listRemoveMap;

    public Map_Vo_Han_Lien_Tang() {
        this.floor = 1;
        this.time_next_floor = -1;
        this.floor_cleared = false;
        this.listRemoveMap = new HashMap<>();
    }

    /**
     * Lấy giá trị Option theo tầng dựa trên ID (12, 14, 53, 51, 63)
     */
    public int getOptionValue(int id, int currentFloor) {
        switch (id) {
            case 12: // % Né tránh (Miss) - +10% mỗi tầng
                return currentFloor * 10;
            case 14: // % Phản sát thương - +10% mỗi tầng
                return currentFloor * 10;
            case 53: // Miễn thương (Damage Skip) - +10% mỗi tầng (100/1000)
                return currentFloor * 100;
            case 51: // Giảm né tránh đối thủ - +10% mỗi tầng
                return currentFloor * 100;
            case 63: // Giảm miễn thương đối thủ - +10% mỗi tầng
                return currentFloor * 100;
            case 64: // Giảm phản đòn đối thủ - +10% mỗi tầng
                return currentFloor * 10;
            case 15: // Giảm phòng thủ đối thủ (Phá giáp) - +10% mỗi tầng
                return currentFloor * 10;
            default:
                return 0;
        }
    }
}
