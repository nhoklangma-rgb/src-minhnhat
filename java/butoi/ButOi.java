package butoi;

import client.Player;
import core.Service;
import core.Util;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.Item_wear;

import java.io.IOException;

/**
 * Tính năng "Bụt ơi" - Chat "but oi" để nhận vật phẩm.
 */
public class ButOi {

    public static final int YES_NO_ID = 201;
    public static final int INPUT_ID = 202;

    /**
     * Được gọi khi người chơi chat "but oi".
     * Hiện hộp thoại Yes/No cho người chơi.
     */
    public static void processChat(Player p) throws IOException {
        Service.send_box_yesno(p, YES_NO_ID, "Bụt",
                "Bụt đây, con có bị Gay không?",
                new String[] { "Dạ có! Con là Gay", "Dạ không!" },
                new byte[] { 3, 1 });
    }

    /**
     * Xử lý khi người chơi chọn một lựa chọn trong hộp thoại Yes/No.
     * value == 0 => "Dạ có! Con là Gay" => hiện bảng nhập vật phẩm.
     * value != 0 => "Dạ không!" => đóng.
     */
    public static void processYesNo(Player p, byte value) throws IOException {
        if (value == 0) {
            // Người chơi chọn "Dạ có! Con là Gay" -> hiện input
            Service.input_text(p, INPUT_ID, "Bụt Gay Lỏ",
                    new String[] { "Loại (3, 4, 7)", "ID vật phẩm", "Số lượng" });
        }
        // value != 0 => Đóng, không làm gì thêm
    }

    /**
     * Xử lý khi người chơi nhập dữ liệu vật phẩm từ input box.
     * inputs[0] = Loại (3, 4, 7)
     * inputs[1] = ID vật phẩm
     * inputs[2] = Số lượng
     */
    public static void processInput(Player p, String[] inputs) throws IOException {
        if (inputs.length < 3) {
            Service.send_box_ThongBao_OK(p, "Thằng Gay này nhập đủ 3 trường: Loại, ID, Số lượng");
            return;
        }

        // Validate inputs
        for (int i = 0; i < 3; i++) {
            if (!Util.isnumber(inputs[i])) {
                Service.send_box_ThongBao_OK(p, "Giá trị nhập không hợp lệ, chỉ được nhập số");
                return;
            }
        }

        int type = Integer.parseInt(inputs[0]);
        int itemId = Integer.parseInt(inputs[1]);
        int quantity = Integer.parseInt(inputs[2]);

        // Validate type
        if (type != 3 && type != 4 && type != 7) {
            Service.send_box_ThongBao_OK(p, "Loại phải là 3, 4 hoặc 7");
            return;
        }

        if (quantity <= 0 || quantity > 32000) {
            Service.send_box_ThongBao_OK(p, "Số lượng phải từ 1 đến 32000");
            return;
        }

        if (p.item.able_bag() < 1) {
            Service.send_box_ThongBao_OK(p, "Hành trang không đủ chỗ trống!");
            return;
        }

        if (type == 3) {
            // Trang bị (ItemTemplate3)
            ItemTemplate3 it3 = ItemTemplate3.get_it_by_id(itemId);
            if (it3 == null) {
                Service.send_box_ThongBao_OK(p, "Không tìm thấy trang bị với ID: " + itemId);
                return;
            }
            for (int i = 0; i < quantity; i++) {
                if (p.item.able_bag() < 1) {
                    Service.send_box_ThongBao_OK(p, "Hành trang đầy! Đã nhận " + i + "/" + quantity);
                    break;
                }
                Item_wear itw = new Item_wear();
                itw.setup_template_by_id(it3);

                // KHÓA BƯỚC 1: Không thêm trang bị vào RAM Server nữa
                // p.item.add_item_bag3(itw);
            }

            // KHÓA BƯỚC 2: Không gửi packet làm mới túi đồ cho Client
            // p.item.update_Inventory(-1, false);

            // KHÓA BƯỚC 3: Không báo tin nhắn Bụt hiện lên nữa
            // Service.send_box_ThongBao_OK(p, "Bụt đã ban cho con " + quantity + " " +
            // it3.name + "!");
            Service.send_box_ThongBao_OK(p, "Bụt đã ban cho con ... cái nịt, hết test rồi thằng Gay à!");

        } else {
            // Vật phẩm (ItemTemplate4 hoặc ItemTemplate7)
            String itemName;
            if (type == 4) {
                ItemTemplate4 it4 = ItemTemplate4.get_it_by_id(itemId);
                itemName = (it4 != null) ? it4.name : ("Item#" + itemId);
            } else {
                itemName = "Item7#" + itemId;
            }

            // KHÓA BƯỚC 1: Không thêm vật phẩm/nguyên liệu vào RAM Server nữa
            // p.item.add_item_bag47(type, itemId, quantity);

            // KHÓA BƯỚC 2: Không gửi packet làm mới túi đồ cho Client
            // p.item.update_Inventory(-1, false);

            // KHÓA BƯỚC 3: Không báo tin nhắn Bụt hiện lên nữa
            // Service.send_box_ThongBao_OK(p, "Bụt đã ban cho con " + quantity + " " +
            // itemName + "!");
            Service.send_box_ThongBao_OK(p, "Bụt đã ban cho con ... cái nịt, hết test rồi thằng Gay à!");
        }
    }
}
