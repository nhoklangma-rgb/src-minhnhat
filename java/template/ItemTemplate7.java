package template;

import java.util.ArrayList;
import java.util.List;

public class ItemTemplate7 {
	public static List<ItemTemplate7> ENTRYS = new ArrayList<>();
	public short id;
	public String name;
	public byte type;
	public byte icon;
	public int price;
	public short priceruby;
	public byte istrade;

	public static java.util.Map<Integer, ItemTemplate7> MAP = new java.util.HashMap<>();

	public static ItemTemplate7 get_it_by_id(int id) {
		return MAP.get(id);
	}

	public static String get_item_name(int id) {
		String s = "";
		for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
			if (ItemTemplate7.ENTRYS.get(i).id == id) {
				s = ItemTemplate7.ENTRYS.get(i).name;
				break;
			}
		}
		return s;
	}
	public static ItemTemplate7 get_it_by_name(String name) {
		for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
			if (ItemTemplate7.ENTRYS.get(i).name.equals(name)) {
				return ItemTemplate7.ENTRYS.get(i);
			}
		}
		return null;
	}
}
