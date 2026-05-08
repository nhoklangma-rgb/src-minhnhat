package template;

import java.util.ArrayList;
import java.util.List;

public class ShopExtol {
    public static List<ShopExtol> ENTRY = new ArrayList<>();
    public int id;
    public short id_title;
    public long price;
    public short icon;
    public String info;

    public static ShopExtol get_temp_id(int id) {
        for (int i = 0; i < ENTRY.size(); i++) {
            if (ENTRY.get(i).id == id) {
                return ENTRY.get(i);
            }
        }
        return null;
    }
    
    public static ShopExtol get_by_title_id(int title_id) {
        for (int i = 0; i < ENTRY.size(); i++) {
            if (ENTRY.get(i).id_title == title_id) {
                return ENTRY.get(i);
            }
        }
        return null;
    }
}
