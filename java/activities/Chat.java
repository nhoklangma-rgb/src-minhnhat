package activities;

import java.io.IOException;
import client.Player;
import core.Service;
import io.Message;

public class Chat {
	public static void process(Player p, Message m2, int type) throws IOException {
		if (type == 0) {
			String tab_name = m2.reader().readUTF();
			String text = m2.reader().readUTF();

			if (text.equalsIgnoreCase("slot") || text.equalsIgnoreCase("quay")) {
				taixiu.SlotMachineManager.showMainMenu(p);
				return;
			}

			switch (tab_name) {
				case "Nhóm": {
					if (p.party != null) {
						for (int i = 0; i < p.party.list.size(); i++) {
							Player p0 = p.party.list.get(i);
							if (p0.index_map != p.index_map) {
								send_chat(p0, tab_name, "@" + p.getNameShow() + " : " + text, false);
							}
						}
					} else {
						Service.send_box_ThongBao_OK(p, "Nhóm không tồn tại");
					}
					break;
				}
				case "Băng":
				case "Clan": {
					if (p.clan != null) {
						template.Clan_chat chat = new template.Clan_chat();
						chat.name = p.getNameShow();
						chat.str = text;
						chat.time = System.currentTimeMillis();

						byte levelInclan = 10;
						for (template.Clan_member mem : p.clan.members) {
							if (mem.name.equals(p.name)) {
								levelInclan = mem.levelInclan;
								chat.idMem = mem.id;
								break;
							}
						}
						chat.typeChat = (byte) (levelInclan == 0 ? -1 : -4);

						p.clan.add_chat(chat);
						p.clan.send_chat(chat, null);
					}
					break;
				}
			}
		}
	}

	public static void send_chat(Player p, String tab_name, String text, boolean cache)
			throws IOException {
		if (p.conn != null) {
			Message m = new Message(18);
			m.writer().writeUTF(tab_name);
			m.writer().writeUTF(text);
			if (cache) {
				p.list_msg_cache.add(m);
			} else {
				p.addmsg(m);
			}
			m.cleanup();
		}
	}
}
