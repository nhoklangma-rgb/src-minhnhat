package template;

import java.util.ArrayList;
import java.util.List;

public class ItemTemplate4 {
	public static List<ItemTemplate4> ENTRYS = new ArrayList<>();
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
	public short indexInfoPotion;

	public static java.util.Map<Integer, ItemTemplate4> MAP = new java.util.HashMap<>();

	public static ItemTemplate4 get_it_by_id(int id) {
		return MAP.get(id);
	}

	public static ItemTemplate4 get_it_by_name(String name) {
		for (int i = 0; i < ItemTemplate4.ENTRYS.size(); i++) {
			if (ItemTemplate4.ENTRYS.get(i).name.equals(name)) {
				return ItemTemplate4.ENTRYS.get(i);
			}
		}
		return null;
	}

	public static String get_item_name(int id) {
		String s = "";
		for (int i = 0; i < ItemTemplate4.ENTRYS.size(); i++) {
			if (ItemTemplate4.ENTRYS.get(i).id == id) {
				s = ItemTemplate4.ENTRYS.get(i).name;
				break;
			}
		}
		return s;
	}
}
