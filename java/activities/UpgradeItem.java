package activities;

import client.Item;
import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.*;

public class UpgradeItem {
    public static List<DataUpgrade> DATA = new ArrayList<>();
    static {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Util.loadfile("data/msg/login/request/msg-7_12"));
                DataInputStream dis = new DataInputStream(bais)) {
            int n = dis.readByte();
            for (int i = 0; i < n; i++) {
                DataUpgrade temp = new DataUpgrade();
                temp.level = dis.readByte();
                temp.per = dis.readShort();
                temp.prelevel = dis.readByte();
                temp.beri = dis.readInt();
                temp.beri_white = dis.readInt();
                temp.ruby = dis.readShort();
                temp.att = dis.readShort();
                int n2 = dis.readByte();
                temp.material = new UpgradeMaterialTemplate[n2];
                for (int j = 0; j < n2; j++) {
                    temp.material[j] = new UpgradeMaterialTemplate(dis.readByte(), dis.readByte(),
                            dis.readShort());
                }
                UpgradeItem.DATA.add(temp);
            }
        } catch (IOException e) {
            core.GameLogger.error("UpgradeItem.load: Error loading upgrade data", e);
        }
    }

    public static void show_table_upgrade(Player p) throws IOException {
        Message m = new Message(-48);
        m.writer().writeByte(7);
        p.addmsg(m);
        m.cleanup();
        p.tool_upgrade = new int[] { -1, -1 };
    }

    public static void process(Player p, Message m2) throws IOException {
        byte type = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte bery_gem = m2.reader().readByte();
        if (type == 8 && id == 0 && bery_gem == 0) {
            Service.Send_UI_Shop(p, 6);
        } else if (type == 7 && id == 0 && bery_gem == 0) {
            UpgradeItem.show_table_upgrade(p);
        } else if (type == 13 && id == 0 && bery_gem == 0) {
            Rebuild_Item.show_table(p, 4);
        } else if (type == 10 && id == 0 && bery_gem == 0) {
            Rebuild_Item.show_table(p, 2);
        } else if (type == 12 && id == 0 && bery_gem == 0) {
            Rebuild_Item.show_table(p, 1);
        } else if (type == 9 && id == 0 && bery_gem == 0) {
            Rebuild_Item.show_table(p, 3);
        } else if (type == 11 && id == 0 && bery_gem == 0) {
            Service.Send_UI_Shop(p, 111);
        } else if (type == 14 && bery_gem == 0) { // bo item3 vao
            Item_wear it = p.item.bag3[id];
            if (it != null) {
                if (it.template.typeEquip > 7 || (it.template.tb2 != 0 && it.template.tb2 != 2)) {
                    Service.send_box_ThongBao_OK(p,
                            "Vật phẩm không hợp lệ\ntype=" + it.template.typeEquip + " tb2=" + it.template.tb2);
                    return;
                }
                if (it.levelup < 15) {
                    Message m = new Message(-48);
                    m.writer().writeByte(4);
                    m.writer().writeShort(id);
                    p.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_box_ThongBao_OK(p, "Trang bị đã cường hóa cấp tối đa");
                }
            }
        } else if (type == 4 && bery_gem == 0) { // bo item3 vao
            Item_wear it = p.item.bag3[id];
            if (it != null) {
                if (it.template.typeEquip > 7 || (it.template.tb2 != 0 && it.template.tb2 != 2)) {
                    Service.send_box_ThongBao_OK(p,
                            "Vật phẩm không hợp lệ\ntype=" + it.template.typeEquip + " tb2=" + it.template.tb2);
                    return;
                }
                if (it.levelup < 10) {
                    Message m = new Message(-48);
                    m.writer().writeByte(4);
                    m.writer().writeShort(id);
                    p.addmsg(m);
                    m.cleanup();
                } else {
                    Service.send_box_ThongBao_OK(p, "Trang bị đã cường hóa cấp tối đa");
                }
            }
        } else if (type == 1 && bery_gem == 0) { // ask upgrade
            Item_wear it = p.item.bag3[id];
            if (it != null) {
                if (it.levelup > 9) {
                    Service.send_box_ThongBao_OK(p, "Đã nâng cấp tối đa!");
                    return;
                }
                Message m = new Message(-48);
                m.writer().writeByte(1);
                m.writer().writeUTF("Bạn có chắc chắn muốn nâng\n"
                        + it.template.name + " lên cấp " + (it.levelup + 1));
                m.writer().writeInt(UpgradeItem.DATA.get(it.levelup).beri);
                m.writer().writeShort(-1);
                m.writer().writeShort(id);
                p.addmsg(m);
                m.cleanup();
            }
        } else if (type == 2) { // start upgrade (beri gem 1 : beri, 2: ruby)
            Item_wear it = p.item.bag3[id];
            if (it != null) {
                if (it.levelup > 9) {
                    Service.send_box_ThongBao_OK(p, "Đã nâng cấp tối đa!");
                    return;
                }
                if (it.template.typeEquip > 7) {
                    Service.send_box_ThongBao_OK(p, "Trang bị này không thể nâng cấp!");
                    return;
                }
                int[] material_req = get_material(it.levelup, it.template.color);
                if (material_req[0] > -1) {
                    if (p.item.total_item_bag_by_id(7, material_req[0]) < material_req[1]) {
                        Service.send_box_ThongBao_OK(p, "Không đủ " + material_req[1] + " Bột cường hóa");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(7, material_req[2]) < material_req[3]) {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ " + material_req[3] + " Bột vàng");
                        return;
                    }
                } else {
                    Service.send_box_ThongBao_OK(p, "Đã có lỗi xảy ra");
                    return;
                }
                if (bery_gem == 1) {
                    if (p.get_vang() < UpgradeItem.DATA.get(it.levelup).beri) {
                        Service.send_box_ThongBao_OK(p,
                                "Không đủ " + UpgradeItem.DATA.get(it.levelup).beri + " Beri");
                        return;
                    }
                    p.update_vang(-UpgradeItem.DATA.get(it.levelup).beri);
                    p.update_money();
                }
                p.item.remove_item47(7, material_req[0], material_req[1]);
                p.item.remove_item47(7, material_req[2], material_req[3]);

                if (p.tool_upgrade[0] != -1) {
                    p.item.remove_item47(7, p.tool_upgrade[0], 1);
                }
                if (p.tool_upgrade[1] != -1) {
                    p.item.remove_item47(7, p.tool_upgrade[1], 1);
                }
                // check item tool
                for (int i = 0; i < 2; i++) {
                    if (p.tool_upgrade[i] != -1
                            && p.item.total_item_bag_by_id(7, p.tool_upgrade[i]) < 1) {
                        p.tool_upgrade[i] = -1;
                    }
                }
                //
                int percent = UpgradeItem.DATA.get(it.levelup).per;
                if (p.tool_upgrade[1] != -1) {
                    if (p.tool_upgrade[1] == 5) {
                        percent = (percent * 15) / 10;
                    } else if (p.tool_upgrade[1] == 11) {
                        percent = (percent * 2);
                    }
                }
                boolean suc = ((percent >= 1000) ? 1200 : percent) > Util.random(1200);
                if (suc) {
                    it.levelup++;
                    if (it.levelup == 10) {
                        UpgradeItem.show_table_upgrade(p);
                    }
                    notice_upgrade(p, 2, "Cường hóa thành công \n" + it.template.name
                            + " lên cấp " + it.levelup);
                } else {
                    if (p.tool_upgrade[0] == -1) {
                        it.levelup = UpgradeItem.DATA.get(it.levelup).prelevel;
                    }
                    notice_upgrade(p, 3, "Cường hóa thất bại về cấp " + it.levelup);
                }
                //
                if (p.item.total_item_bag_by_id(7, p.tool_upgrade[0]) < 1) {
                    p.tool_upgrade[0] = -1;
                }
                if (p.item.total_item_bag_by_id(7, p.tool_upgrade[1]) < 1) {
                    p.tool_upgrade[1] = -1;
                }
                //
                p.item.update_Inventory(-1, false);
            }
        } else if (type == 6 && (bery_gem == 1 || bery_gem == 0)) { // mai rua
            if (bery_gem == 0 || p.item.total_item_bag_by_id(7, id) > 0) {
                Message m5 = new Message(-48);
                m5.writer().writeByte(6);
                m5.writer().writeByte(bery_gem); // is use
                m5.writer().writeShort(id);
                p.addmsg(m5);
                m5.cleanup();
                p.tool_upgrade[0] = (bery_gem == 0) ? -1 : id;
            }
        } else if (type == 5 && (bery_gem == 1 || bery_gem == 0)) { // ngoi sao may man
            if (bery_gem == 0 || p.item.total_item_bag_by_id(7, id) > 0) {
                Message m5 = new Message(-48);
                m5.writer().writeByte(5);
                m5.writer().writeByte(bery_gem); // is use
                m5.writer().writeShort(id);
                p.addmsg(m5);
                m5.cleanup();
                p.tool_upgrade[1] = (bery_gem == 0) ? -1 : id;
            }
        } else if (type == 15 && id == -1 && bery_gem == 0) { // heart upgrade
        }
    }

    private static int[] get_material(int level, int color) {
        int[] result = new int[] { -1, -1, -1, -1 };
        DataUpgrade temp = UpgradeItem.DATA.get(level);
        for (int i = 0; i < temp.material.length; i++) {
            if (temp.material[i].type == -1) {
                result[0] = temp.material[i].id;
                result[1] = temp.material[i].quant;
            }
            if (temp.material[i].type == color) {
                result[2] = 4;
                result[3] = temp.material[i].quant;
            }
        }
        return result;
    }

    private static void notice_upgrade(Player p, int type, String s) throws IOException {
        Message m = new Message(-48);
        m.writer().writeByte(type);
        m.writer().writeUTF(s);
        p.addmsg(m);
        m.cleanup();
    }
}
