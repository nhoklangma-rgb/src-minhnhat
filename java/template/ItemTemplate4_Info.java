package template;

import java.util.HashMap;
import java.util.Map;

public class ItemTemplate4_Info {
    public static Map<Short, ItemTemplate4_Info> ENTRY = new HashMap<>();

    public short id;
    public String info;

    public static ItemTemplate4_Info get_by_id(short id) {
        return ENTRY.get(id);
    }
}
