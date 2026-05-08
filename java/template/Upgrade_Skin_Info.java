package template;

public class Upgrade_Skin_Info {
    public ItemFashionP2 skin;
    public short[] upgrade_skin_data;

    public static String get_op_level(byte level) {
        float percent = level * 1.0f; // mỗi level +1%
        return String.format("%.1f", percent);
    }
}
