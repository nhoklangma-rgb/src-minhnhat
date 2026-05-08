package template;

import java.util.List;

public class ItemOptionTemplate {
	public static List<ItemOptionTemplate> ENTRYS;
	public short id;
	public String name;
	public byte color;
	public byte percent;

	public static ItemOptionTemplate get_temp(int id) {
		for (ItemOptionTemplate entry : ENTRYS) {
			if (entry.id == id) {
				return entry;
			}
		}
		return null;
	}
}
