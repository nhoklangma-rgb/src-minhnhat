package template;



public class ItemTemplate8 {
    public static java.util.List<ItemTemplate8> ENTRYS = new java.util.ArrayList<>();
    public static java.util.Map<Integer, ItemTemplate8> MAP = new java.util.HashMap<>();

    public static ItemTemplate8 get_it_by_id(int id) {
        return MAP.get(id);
    }
    public short icon;
    public short id;
    public String name;
    public byte type;
    public short ruby;
    public int beri;
    public byte istrade;
    public short timedelay;
    public short value;
    public short timeactive;
    public String nameuse;
    public String info;
}
