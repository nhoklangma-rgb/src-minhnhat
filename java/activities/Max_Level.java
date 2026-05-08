package activities;

import client.Player;
import core.Service;
import tool.ItemDropTool;
import io.Message;
import java.io.IOException;
import template.Option;

public class Max_Level {
    private static final String[] name = new String[] { "Sức Mạnh", "Phòng Thủ", "Thể Lực", "Tinh Thần", "Nhanh Nhẹn" };
    private static final short[] id_op = new short[] { 5, 6, 7, 8, 9 };

    public static void addThongThao(Player p, int indexOp, int diemcong) throws IOException {
        if (indexOp < 0 || indexOp >= id_op.length)
            return;
        Option op_add = null;
        for (Option op : p.list_op_thongthao) {
            if (op.id == id_op[indexOp]) {
                op_add = op;
                break;
            }
        }
        int currentValue = (op_add != null) ? op_add.getParam() : 0;
        if (diemcong <= 0 || p.pointAttributeThongThao < diemcong) {
            Service.send_box_ThongBao_OK(p, "Không đủ điểm thông thạo");
            return;
        }
        if (currentValue >= ItemDropTool.MAX_THONG_THAO_POINT) {
            Service.send_box_ThongBao_OK(p, "Chỉ được cộng tối đa " + ItemDropTool.MAX_THONG_THAO_POINT + " điểm");
            return;
        }
        int diemThucCong = Math.min(diemcong, ItemDropTool.MAX_THONG_THAO_POINT - currentValue);
        p.pointAttributeThongThao -= diemThucCong;
        if (op_add != null) {
            op_add.setParam(currentValue + diemThucCong);
        } else {
            p.list_op_thongthao.add(new Option(id_op[indexOp], diemThucCong));
        }
        p.update_info_to_all();
        show_table(p);
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        if (act == 0) {
            for (int i = 0; i < id_op.length; i++) {
                if (id_op[i] == id) {
                    p.data_yesno = new int[] { i };
                    Service.send_box_yesno(p, 41, "Thông báo",
                            ("Hiện có " + p.pointAttributeThongThao +
                                    " điểm\nBạn muốn cộng bao nhiêu điểm vào\nTiềm năng " + name[i] + " ?"),
                            new String[] { "100", "1000", "All", "Đóng" }, new byte[] { 3, 3, 3, 1 });
                    break;
                }
            }
        }
    }

    public static void show_table(Player p) throws IOException {
        Max_Level.set_pointMaxLevelAttri(p);
        Message m = new Message(49);
        m.writer().writeByte(2);
        m.writer().writeShort((short) Math.min(32000, Math.max(0, p.pointAttributeThongThao)));
        p.addmsg(m);
        m.cleanup();
    }

    private static void set_pointMaxLevelAttri(Player p) throws IOException {
        Message m = new Message(49);
        m.writer().writeByte(0);
        m.writer().writeShort((short) Math.min(32000, p.thongthao));
        m.writer().writeByte(name.length);
        for (int i = 0; i < name.length; i++) {
            m.writer().writeShort(id_op[i]);
            m.writer().writeUTF(name[i]);
            int value = 0;
            for (int j = 0; j < p.list_op_thongthao.size(); j++) {
                if (p.list_op_thongthao.get(j).id == id_op[i]) {
                    value += p.list_op_thongthao.get(j).getParam();
                }
            }
            m.writer().writeShort((short) Math.min(32000, value));
            m.writer().writeShort((short) ItemDropTool.MAX_THONG_THAO_POINT);
        }
        p.addmsg(m);
        m.cleanup();
    }
}
