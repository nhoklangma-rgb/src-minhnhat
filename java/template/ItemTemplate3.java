package template;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemTemplate3 {
	public static List<ItemTemplate3> ENTRYS = new ArrayList<>();
	public static java.util.Map<Integer, ItemTemplate3> MAP = new java.util.HashMap<>();
	public short id;
	public String name;
	public byte clazz;
	public byte typeEquip;
	public short icon;
	public short level;
	public byte color;
	public byte typelock;
	public byte numHoleDaDuc;
	public short valueChetac;
	public byte isHoanMy;
	public byte valueKichAn;
	public List<Option> option_item;
	public List<Option> option_item_2;
	public byte numLoKham;
	public short[] mdakham;
	public short part;
	public int beri;
	public int ruby;
	public int tb2;

	public static ItemTemplate3 get_it_by_id(int id) {
		return MAP.get(id);
	}

	public static void readUpdateItem(DataOutputStream dos, ItemTemplate3 it) throws IOException {
		dos.writeShort(it.id);
		dos.writeUTF(it.name != null ? it.name : "");
		dos.writeByte(it.clazz);
		dos.writeByte(it.typeEquip);
		dos.writeShort(it.icon);
		dos.writeShort(it.level);
		dos.writeByte(0); // level up
		dos.writeByte(it.color);
		dos.writeByte(0); // is trade
		dos.writeByte(it.typelock);
		dos.writeByte(it.numHoleDaDuc);
		dos.writeInt(0); // time use
		dos.writeShort(it.valueChetac);
		dos.writeByte(it.isHoanMy);
		dos.writeByte(it.valueKichAn);
		dos.writeByte(it.option_item.size());
		for (int i = 0; i < it.option_item.size(); i++) {
			dos.writeByte(it.option_item.get(i).id);
			dos.writeShort(Math.min(it.option_item.get(i).getParam(), 32000));
		}
		dos.writeByte(it.option_item_2.size());
		for (int i = 0; i < it.option_item_2.size(); i++) {
			dos.writeByte(it.option_item_2.get(i).id);
			dos.writeShort(it.option_item_2.get(i).getParam());
		}
		dos.writeByte(it.numLoKham);
		dos.writeByte(it.mdakham.length);
		for (int i = 0; i < it.mdakham.length; i++) {
			dos.writeShort(it.mdakham[i]);
		}
	}

	public static ItemTemplate3 get_item_random(byte typeEquip, byte clazz, int bound1, int bound2) {
		for (int i = bound1; i < bound2; i++) {
			if (ItemTemplate3.ENTRYS.get(i).typeEquip == typeEquip && ItemTemplate3.ENTRYS.get(i).clazz == clazz) {
				return ItemTemplate3.ENTRYS.get(i);
			}
		}
		return null;
	}
}
