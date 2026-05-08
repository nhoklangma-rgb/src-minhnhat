package activities;

import java.io.IOException;
import client.Player;
import core.Service;
import io.Message;
import template.Skill_Template;
import template.Skill_info;

public class Learn_Skill {
	public static void process(Player p, Message m2) throws IOException {
		byte act = m2.reader().readByte();
		short id = m2.reader().readShort();
		// core.GameLogger.info("act " + act);
		// core.GameLogger.info("id " + id);
		switch (act) {
			case 0: {
				Skill_info sk_temp = null;
				for (int i = 0; i < p.skill_point.size(); i++) {
					if (p.skill_point.get(i).temp.Lv_RQ == -1
							&& p.skill_point.get(i).temp.indexSkillInServer == id) {
						sk_temp = p.skill_point.get(i);
						break;
					}
				}
				if (sk_temp == null) {
					for (int i = 0; i < p.skill_point.size(); i++) {
						if (p.skill_point.get(i).temp.Lv_RQ != -1
								&& p.skill_point.get(i).temp.indexSkillInServer == (id - 1)) {
							sk_temp = p.skill_point.get(i);
							break;
						}
					}
				}
				if (sk_temp != null) {
					if (p.get_vang() < 10_000) {
						Service.send_box_ThongBao_OK(p,
								"Bạn không đủ 10k Beri. Phí học kỹ năng này là 10k Beri!");
						return;
					}
					if (Skill_Template.learn_skill(sk_temp)) {
						p.update_vang(-10_000);
						p.update_money();
						p.send_skill();
						p.update_info_to_all();
						Service.send_box_ThongBao_OK(p, "Học Thành công " + sk_temp.temp.name);
					} else {
						Service.send_box_ThongBao_OK(p,
								"Có lỗi xảy ra khi học skill, hãy báo cho admin fix ngay");
					}
				} else {
					Service.send_box_ThongBao_OK(p, "Có lỗi xảy ra, hãy thử lại");
				}
				break;
			}
		}
	}

	public static void request_learn_new_skill(Player p, Skill_info temp) throws IOException {
		Message m = new Message(-28);
		m.writer().writeByte(0);
		p.write_data_skill(m.writer(), temp);
		p.addmsg(m);
		m.cleanup();
	}

	public static void send_skill_percent(Player p, Skill_info sk_info) throws IOException {
		Message m = new Message(-28);
		m.writer().writeByte(2);
		m.writer().writeShort(sk_info.temp.ID);
		m.writer().writeShort(sk_info.get_percent());
		p.addmsg(m);
		m.cleanup();
	}

	public static void remove_skill(Player p, Skill_info sk_info) throws IOException {
		Message m = new Message(-28);
		m.writer().writeByte(3);
		m.writer().writeShort(sk_info.temp.ID);
		p.addmsg(m);
		m.cleanup();
	}
}
