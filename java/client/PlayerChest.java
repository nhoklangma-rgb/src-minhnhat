package client;

import java.io.IOException;
import core.Service;
import io.Message;
import template.DataTemplate;
import template.Item_wear;

public class PlayerChest {
    public static void process(Player p, Message m2) throws IOException {
        p.isBox = true; // [Dupe Prevention] Set box state
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        int num = m2.reader().readInt();
        if ((act == 1 || act == 2) && cat == 4 && (id == 180 || id == 181 || id == 182)) {
            return;
        }
        if (act == 1) { // cat vao
            if (p.item.able_box() < 1) {
                Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                return;
            }
            switch (cat) {
                case 3: {
                    Item_wear it_select = p.item.bag3[id];
                    if (it_select != null) {
                        Item_wear it_add = new Item_wear();
                        it_add.clone_obj(it_select);
                        p.item.bag3[id] = null;
                        if (!p.item.add_item_box3(it_add)) {
                            p.item.bag3[id] = it_select;
                            Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                        }
                    }
                    break;
                }
                case 4:
                case 7: {
                    if (id == 1007) {
                        Service.send_box_ThongBao_OK(p, "Không thể cất vật phẩm này!");
                        return;
                    }
                    if (p.item.total_item_bag_by_id(cat, id) >= num) {
                        int num_in_box = p.item.total_item_box_by_id(cat, id);
                        if ((num + num_in_box) > DataTemplate.MAX_ITEM_IN_BAG) {
                            num = DataTemplate.MAX_ITEM_IN_BAG - num_in_box;
                        }
                        if (num > 0) {
                            p.item.remove_item47(cat, id, num);
                            p.item.add_item_box47(cat, id, num);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                        }
                    }
                    break;
                }
                default: {
                    return;
                }
            }
            p.item.update_Inventory(-1, false);
            p.item.update_Inventory_box(-1, false);
        } else if (act == 2) { // lay ra
            if (p.item.able_bag() < 1) {
                Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                return;
            }
            switch (cat) {
                case 3: {
                    Item_wear it_select = p.item.box3[id];
                    if (it_select != null) {
                        Item_wear it_add = new Item_wear();
                        it_add.clone_obj(it_select);
                        p.item.box3[id] = null;
                        if (!p.item.add_item_bag3(it_add)) {
                            p.item.box3[id] = it_select;
                            Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                        }
                    }
                    break;
                }
                case 4:
                case 7: {
                    if (p.item.total_item_box_by_id(cat, id) >= num) {
                        int num_in_bag = p.item.total_item_bag_by_id(cat, id);
                        if ((num + num_in_bag) > DataTemplate.MAX_ITEM_IN_BAG) {
                            num = DataTemplate.MAX_ITEM_IN_BAG - num_in_bag;
                        }
                        if (num > 0) {
                            p.item.remove_item47_box(cat, id, num);
                            p.item.add_item_bag47(cat, id, num);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Rương đã đầy!");
                        }
                    }
                    break;
                }
                default: {
                    return;
                }
            }
            p.item.update_Inventory(-1, false);
            p.item.update_Inventory_box(-1, false);
        } else if (act == 5) {
            Service.send_box_ThongBao_OK(p, "Đã mở rộng tối đa");
        }
    }
}
