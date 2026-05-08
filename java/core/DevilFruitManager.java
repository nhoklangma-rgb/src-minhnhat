package core;

import java.util.ArrayList;
import java.util.List;

public class DevilFruitManager {
    
    /**
     * Returns a list of skill IDs (template) based on the Devil Fruit ID.
     * @param fruitId ID of the devil fruit (with 4000 offset applied if needed)
     * @return Array of skill IDs
     */
    public static int[] getSkillsByFruit(int fruitId) {
        switch (fruitId) {
            case 4032: // Mera Mera (Lửa)
                return new int[] { 480, 479, 477 };
            case 4033: // Gomu Gomu (Cao su)
                return new int[] { 478, 476, 475 };
            case 4034: // Hito Hito (Chopper/Tuần lộc)
                return new int[] { 521, 520, 518 };
            case 4088: // Moku Moku (Khói)
                return new int[] { 484, 485, 486 };
            case 4090: // Ushi Ushi (Bò tót)
                return new int[] { 514, 513, 512 };
            case 4091: // Nét vẽ
                return new int[] { 517, 516, 515 };
            case 4092: // Hie Hie (Băng)
                return new int[] { 523, 522, 519 };
            case 4093: // Suna Suna (Cát)
                return new int[] { 55, 54, 52 }; // Matches buggy source logic
            case 4160: // Goro Goro (Sét)
                return new int[] { 527, 526, 525, 524 };
            case 4161: // Magu Magu (Nham thạch)
                return new int[] { 531, 530, 529, 528 };
            case 4219: // Mochi Mochi
                return new int[] { 538, 537, 536 };
            case 4220: // Tori Tori (Báo/Falcon) - Logic check needed but using source values
                return new int[] { 535, 534, 533 };
            case 4240: // Gura Gura (Chấn động)
                return new int[] { 542, 541, 539, 540 };
            case 4316: // Doru Doru (Sáp)
                return new int[] { 548, 547, 546 };
            case 4317: // Ope Ope or Dice? (Buggy says 4317 is Phượng Hoàng)
                return new int[] { 545, 544, 543 };
            case 4318: // Kilo Kilo (Trọng lực)
                return new int[] { 551, 550, 549 };
            case 4427: // Yami Yami (Bóng đêm)
                return new int[] { 656, 657, 658, 659 };
            case 4684: // Pika Pika (Ánh sáng)
                return new int[] { 761, 762, 763, 764 };
            default:
                return new int[0];
        }
    }

    public static List<Short> getAvailableFruitIds() {
        List<Short> list = new ArrayList<>();
        int[] ids = new int[] { 32, 33, 34, 88, 90, 91, 92, 93, 160, 161, 219, 220, 240, 316, 317, 318, 427, 684 };
        for (int id : ids) {
            list.add((short) id);
        }
        return list;
    }
}
