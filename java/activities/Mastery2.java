package activities;

import client.Player;
import core.GameLogger;
import core.Service;
import core.Util;
import io.Message;
import template.Option;
import java.io.IOException;

public class Mastery2 {

    // Mastery 1 Max point constant
    private static final int MAX_POINT_M1 = 32000;
    private static final int MAX_POINT_M2 = 32000;

    // Item IDs
    public static final int ITEM_BOI_TU_TIEN_1 = 1037;
    public static final int ITEM_BOI_TU_MA_1 = 1038;
    public static final int ITEM_BOI_TU_TIEN_2 = 1080;
    public static final int ITEM_BOI_TU_MA_2 = 1081;

    // Attribute Option IDs (Matching Mastery 1 for logic consistency)
    private static final short[] ID_OP = new short[] { 5, 6, 7, 8, 9 };

    /**
     * Kiểm tra điều kiện mở khóa Mastery 2
     * Điều kiện: point1-5 đều đạt 32000 và chưa mở khóa
     */
    public static boolean canUnlock(Player p) {
        if (p.hasUnlockedMastery2) {
            return false;
        }
        for (short id : ID_OP) {
            int value = 0;
            for (Option op : p.list_op_thongthao) {
                if (op.id == id) {
                    value = op.getParam();
                    break;
                }
            }
            if (value < MAX_POINT_M1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Thực hiện Thăng Giai (Unlock Mastery 2)
     * Tốn 500 bội tu ma + 500 bội tu tiên, tỉ lệ 20%
     */
    public static void doUpgrade(Player p) throws IOException {
        if (!canUnlock(p)) {
            Service.send_box_ThongBao_OK(p,
                    "Bạn chưa đủ điều kiện thăng giai (Cần đạt 32,000 điểm ở tất cả 5 thuộc tính Bảng 1)");
            return;
        }

        int countTiên = p.item.total_item_bag_by_id(4, ITEM_BOI_TU_TIEN_1);
        int countMa = p.item.total_item_bag_by_id(4, ITEM_BOI_TU_MA_1);

        if (countTiên < 500 || countMa < 500) {
            Service.send_box_ThongBao_OK(p, "Bạn cần 500 Bội Tu Tiên và 500 Bội Tu Ma để thăng giai!");
            return;
        }

        // Consume items
        p.item.remove_item47(4, ITEM_BOI_TU_TIEN_1, 500);
        p.item.remove_item47(4, ITEM_BOI_TU_MA_1, 500);
        p.item.update_Inventory(-1, false);

        // Success rate 20%
        if (Util.random(100) < 20) {
            p.hasUnlockedMastery2 = true;

            // Anti-bypass: clear pointAttributeThongThao upon success
            int oldPoints = p.pointAttributeThongThao;
            p.pointAttributeThongThao = 0;

            Service.send_box_ThongBao_OK(p, "Chúc mừng! Bạn đã Thăng Giai thành công!\n"
                    + "Toàn bộ điểm tiềm năng Bảng 1 chưa phân bổ (" + oldPoints + ") đã được xóa về 0.");
        } else {
            Service.send_box_ThongBao_OK(p, "Thăng Giai thất bại! Bạn đã mất 500 Bội mỗi loại.");
        }
    }

    /**
     * Dùng bội cho Bảng 1 hoặc Bảng 2
     */
    public static void useBoi(Player p, boolean isBảng2, boolean isTuMa, boolean isCapHai) throws IOException {
        int id = isCapHai ? (isTuMa ? ITEM_BOI_TU_MA_2 : ITEM_BOI_TU_TIEN_2)
                : (isTuMa ? ITEM_BOI_TU_MA_1 : ITEM_BOI_TU_TIEN_1);

        if (p.item.total_item_bag_by_id(4, id) < 1) {
            Service.send_box_ThongBao_OK(p, "Bạn không có vật phẩm này!");
            return;
        }

        // Check for Bội cấp 2 usage in Bảng 1
        if (!isBảng2 && isCapHai) {
            Service.send_box_ThongBao_OK(p, "Bội cấp 2 chỉ dùng cho Thông Thạo 2!");
            return;
        }

        processUseBoi(p, isBảng2, id);
    }

    /**
     * Thực hiện cộng điểm tiềm năng từ Bội
     * 
     * @param isBảng2 dùng cho bảng 2 hay bảng 1
     * @param id      id vật phẩm để trừ
     */
    public static void processUseBoi(Player p, boolean isBảng2, int id) throws IOException {
        int gain = 0;
        if (!isBảng2) {
            gain = 10;
            p.pointAttributeThongThao += gain;
        } else {
            if (!p.hasUnlockedMastery2) {
                Service.send_box_ThongBao_OK(p, "Bạn chưa mở khóa Thông Thạo 2!");
                return;
            }
            gain = 5;
            p.pointAttributeThongThao2 += gain;
        }

        p.item.remove_item47(4, id, 1);
        p.item.update_Inventory(-1, false);

        Service.send_box_ThongBao_OK(p,
                "Sử dụng Bội thành công! Bạn nhận được " + gain + " điểm tiềm năng bảng " + (isBảng2 ? "2" : "1"));

        Message m = new Message(-13);
        m.writer().writeShort(id);
        m.writer().writeShort(p.item.total_item_bag_by_id(4, id));
        p.addmsg(m);
        m.cleanup();
    }

    /**
     * Tẩy tiềm năng Bảng 2
     */
    public static void reset(Player p) throws IOException {
        if (!p.hasUnlockedMastery2)
            return;

        int refundPoints = calculateRefund(p);
        p.pointAttributeThongThao2 += refundPoints;

        p.thongthao2 = 0;
        p.point6 = 0;
        p.point7 = 0;
        p.point8 = 0;
        p.point9 = 0;
        p.point10 = 0;
        p.list_op_thongthao2.clear();

        p.update_info_to_all();
        Service.send_box_ThongBao_OK(p,
                "Tẩy tiềm năng Bảng 2 thành công! Bạn nhận lại " + refundPoints + " điểm tiềm năng.");
    }

    public static int calculateRefund(Player p) {
        return p.point6 + p.point7 + p.point8 + p.point9 + p.point10 + p.thongthao2 * 10;
    }

    /**
     * Ghép bội tại NPC
     */
    public static void craftBoi(Player p, boolean isTuMa) throws IOException {
        craftBoiBulk(p, isTuMa, 1);
    }

    /**
     * Ghép bội số lượng lớn
     */
    public static void craftBoiBulk(Player p, boolean isTuMa, int quantity) throws IOException {
        if (quantity <= 0)
            return;

        int idSource = isTuMa ? ITEM_BOI_TU_MA_1 : ITEM_BOI_TU_TIEN_1;
        int idResult = isTuMa ? ITEM_BOI_TU_MA_2 : ITEM_BOI_TU_TIEN_2;
        int totalRequired = quantity * 10;

        if (p.item.total_item_bag_by_id(4, idSource) < totalRequired) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ " + totalRequired + " Bội thường để ghép!");
            return;
        }

        int successCount = 0;
        for (int i = 0; i < quantity; i++) {
            if (Util.random(100) < 50) {
                successCount++;
            }
        }

        p.item.remove_item47(4, idSource, totalRequired);
        if (successCount > 0) {
            p.item.add_item_bag47(4, idResult, successCount);
        }

        p.item.update_Inventory(-1, false);

        String itemName = (idResult == ITEM_BOI_TU_TIEN_2) ? "Bội Tu Tiên Cấp 2" : "Bội Tu Ma Cấp 2";
        Service.send_box_ThongBao_OK(p, "Kết quả ghép " + quantity + " lần:\n"
                + "- Thành công: " + successCount + "\n"
                + "- Thất bại: " + (quantity - successCount) + "\n"
                + "Bạn nhận được " + successCount + " " + itemName + ".");
    }

    /**
     * Hiển thị bảng cộng điểm Mastery 2
     */
    public static void showMenuAttribute(Player p) throws IOException {
        if (!p.hasUnlockedMastery2) {
            Service.send_box_ThongBao_OK(p, "Bạn chưa mở khóa Thông Thạo 2!");
            return;
        }

        String msg = "Thông Thạo 2\n"
                + "Tiềm năng: " + p.pointAttributeThongThao2;

        String[] menu = {
                "Sức Mạnh (" + p.point6 + ")",
                "Phòng Thủ (" + p.point7 + ")",
                "Thể Lực (" + p.point8 + ")",
                "Tinh Thần (" + p.point9 + ")",
                "Nhanh Nhẹn (" + p.point10 + ")",
                "Tẩy Tiềm Năng",
                "Đóng"
        };
        Service.send_box_yesno(p, 3333, "Thông Thạo 2", msg, menu, new byte[] { -1, -1, -1, -1, -1, -1, 1 });
    }

    /**
     * Xử lý cộng điểm (Mở input nhập số lượng)
     */
    public static void requestAllocation(Player p, int index) throws IOException {
        if (index < 0 || index >= 5)
            return;
        p.data_yesno = new int[] { index }; // Save index for next step
        Service.input_text(p, 3333, "Cộng " + getAttrName(index), new String[] { "Nhập số điểm muốn cộng:" });
    }

    /**
     * Thực hiện cộng điểm sau khi nhập số lượng
     */
    public static void handleAllocation(Player p, String text) throws IOException {
        if (p.data_yesno == null || p.data_yesno.length < 1)
            return;
        int index = p.data_yesno[0];
        int amount = 0;
        try {
            amount = Integer.parseInt(text);
        } catch (Exception e) {
            GameLogger.error("Mastery2.handleAllocation: Invalid amount input for player " + p.name, e);
        }

        if (amount <= 0) {
            Service.send_box_ThongBao_OK(p, "Số lượng không hợp lệ!");
            return;
        }
        if (p.pointAttributeThongThao2 < amount) {
            amount = p.pointAttributeThongThao2;
        }
        if (amount <= 0) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ điểm tiềm năng!");
            return;
        }

        int actualAdd = 0; // Biến lưu số điểm thực sự có thể cộng
        switch (index) {
            case 0:
                actualAdd = Math.min(amount, MAX_POINT_M2 - p.point6);
                p.point6 += actualAdd;
                break;
            case 1:
                actualAdd = Math.min(amount, MAX_POINT_M2 - p.point7);
                p.point7 += actualAdd;
                break;
            case 2:
                actualAdd = Math.min(amount, MAX_POINT_M2 - p.point8);
                p.point8 += actualAdd;
                break;
            case 3:
                actualAdd = Math.min(amount, MAX_POINT_M2 - p.point9);
                p.point9 += actualAdd;
                break;
            case 4:
                actualAdd = Math.min(amount, MAX_POINT_M2 - p.point10);
                p.point10 += actualAdd;
                break;
        }

        // Nếu điểm thực tế được cộng <= 0 (tức là chỉ số đã max từ trước)
        if (actualAdd <= 0) {
            Service.send_box_ThongBao_OK(p, "Thuộc tính này đã đạt cấp tối đa (32,000)!");
            return;
        }

        // Chỉ trừ đi số điểm thực tế đã được cộng vào
        p.pointAttributeThongThao2 -= actualAdd;

        updateOptions(p);
        p.update_info_to_all();
        showMenuAttribute(p);
    }

    private static String getAttrName(int index) {
        switch (index) {
            case 0:
                return "Sức Mạnh";
            case 1:
                return "Phòng Thủ";
            case 2:
                return "Thể Lực";
            case 3:
                return "Tinh Thần";
            case 4:
                return "Nhanh Nhẹn";
            default:
                return "";
        }
    }

    /**
     * Cập nhật stats vào list_op_thongthao2
     */
    public static void updateOptions(Player p) {
        p.list_op_thongthao2.clear();
        p.list_op_thongthao2.add(new Option(ID_OP[0], p.point6));
        p.list_op_thongthao2.add(new Option(ID_OP[1], p.point7));
        p.list_op_thongthao2.add(new Option(ID_OP[2], p.point8));
        p.list_op_thongthao2.add(new Option(ID_OP[3], p.point9));
        p.list_op_thongthao2.add(new Option(ID_OP[4], p.point10));
    }
}
