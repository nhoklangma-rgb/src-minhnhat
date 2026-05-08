package template;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import io.Message;

public class ItemBoat {
	public static List<ItemBoat> ENTRYS = new ArrayList<>();
	static {
		FileInputStream fis = null;
		DataInputStream dis = null;
		try {
			fis = new FileInputStream("data/msg/boat");
			dis = new DataInputStream(fis);
			dis.readByte();
			dis.readUTF();
			dis.readByte();
			int size = dis.readShort();
			for (int i = 0; i < size; i++) {
				ItemBoat itboat = ItemBoat.readUpdateItemBoat(dis);
				dis.readInt();
				dis.readShort();
				ItemBoat.ENTRYS.add(itboat);
			}
		} catch (IOException e) {
			core.GameLogger.error("[Disconnect] ItemBoat.load error: " + e.getMessage());
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					core.GameLogger.error("[Disconnect] ItemBoat.load error: " + e.getMessage());
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					core.GameLogger.error("[Disconnect] ItemBoat.load error: " + e.getMessage());
				}
			}
		}
	}
	public byte id;
	public String name;
	public byte type;
	public short idimg;
	public short icon;

	private static ItemBoat readUpdateItemBoat(DataInputStream dis) throws IOException {
		ItemBoat temp = new ItemBoat();
		temp.id = dis.readByte();
		temp.name = dis.readUTF();
		temp.type = dis.readByte();
		temp.idimg = dis.readShort();
		temp.icon = dis.readShort();
		return temp;
	}

	public static void update_part_boat_when_shopping(Player p) throws IOException {
		Message m = new Message(-62);
		m.writer().writeShort(p.index_map);
		m.writer().writeByte(0);
		m.writer().writeByte(4);
		short[] part_boat = p.get_part_boat();
		m.writer().writeShort(part_boat[0]);
		m.writer().writeShort(part_boat[1]);
		m.writer().writeShort(part_boat[2]);
		m.writer().writeShort(part_boat[3]);
		p.addmsg(m);
		m.cleanup();
	}

	public static ItemBoat get_item(int id) {
		for (int i = 0; i < ItemBoat.ENTRYS.size(); i++) {
			if (ItemBoat.ENTRYS.get(i).id == id) {
				return ItemBoat.ENTRYS.get(i);
			}
		}
		return null;
	}
}
