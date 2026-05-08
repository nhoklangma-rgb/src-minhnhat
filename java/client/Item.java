package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import io.Message;
import map.f;
import template.*;

public class Item {

    public final byte max_bag = 126;
    public final byte max_box = 126;
    private final Player p;
    public Item_wear[] bag3;
    public Item_wear[] box3;
    public Item_wear[] it_body;
    public Item_wear[] second_body;
    public Item_wear[] third_body;
    public List<ItemBag47> bag47;
    public List<ItemBag47> box47;
    public Item_wear it_heart;
    public List<Item_wear> save_item_wear;
    public List<ItemBag47> save_item_47;

    public Item(Player p) {
        this.p = p;
        this.it_body = new Item_wear[8];
        this.second_body = new Item_wear[8];
        this.third_body = new Item_wear[8];
        this.bag3 = new Item_wear[max_bag];
        this.box3 = new Item_wear[max_box];
        this.bag47 = new ArrayList<>();
        this.box47 = new ArrayList<>();
        this.save_item_wear = new ArrayList<>();
        this.save_item_47 = new ArrayList<>();
    }

    public void send_maxbag_Inventory() throws IOException {
        Message m = new Message(-12);
        m.writer().writeByte(6);
        m.writer().writeByte(3);
        m.writer().writeShort(max_bag); // max bag
        p.addmsg(m);
        m.cleanup();
    }

    public void send_maxbox_Inventory() throws IOException {
        Message m = new Message(-32);
        m.writer().writeByte(6);
        m.writer().writeByte(3);
        m.writer().writeShort(max_box); // max box
        p.addmsg(m);
        m.cleanup();
    }

    public void update_Inventory(int type, boolean b) throws IOException {
        update_bag(4, b);
        update_bag(7, b);
        update_bag(3, b);
        update_bag(5, b);
    }

    private void update_bag(int type, boolean b) throws IOException {
        Message m = new Message(-12);
        m.writer().writeByte(0);
        m.writer().writeByte(type);
        switch (type) {
            case 3: {
                m.writer().writeByte(this.quant_item_inbag(3));
                for (int i = 0; i < bag3.length; i++) {
                    if (bag3[i] != null) {
                        Item.readUpdateItem(m.writer(), bag3[i], p);
                    }
                }
                break;
            }
            case 4:
            case 8: {
                m.writer().writeByte(this.quant_item_inbag(4));
                for (int i = 0; i < bag47.size(); i++) {
                    if (bag47.get(i).category == 4) {
                        m.writer().writeShort(bag47.get(i).id);
                        m.writer().writeShort(bag47.get(i).quant);
                    }
                }
                break;
            }
            case 5: {
                m.writer().writeByte(this.quant_item_inbag(5));
                for (int i = 0; i < bag47.size(); i++) {
                    if (bag47.get(i).category == 5) {
                        m.writer().writeShort(bag47.get(i).id);
                        m.writer().writeUTF(DataTemplate.NamePotionquest[bag47.get(i).id]);
                        m.writer().writeShort(bag47.get(i).quant);
                    }
                }
                break;
            }
            case 7: {
                m.writer().writeByte(this.quant_item_inbag(7));
                for (int i = 0; i < bag47.size(); i++) {
                    if (bag47.get(i).category == 7) {
                        m.writer().writeByte(bag47.get(i).id);
                        m.writer().writeShort(bag47.get(i).quant);
                    }
                }
                break;
            }
        }
        if (b) {
            p.list_msg_cache.add(m);
        } else if (p.conn != null) {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public void update_Inventory_box(int type, boolean b) throws IOException {
        if (type == -1) {
            update_box(4, b);
            update_box(7, b);
            update_box(3, b);
        } else {
            update_box(type, b);
        }
    }

    private void update_box(int type, boolean b) throws IOException {
        Message m = new Message(-32);
        m.writer().writeByte(0);
        m.writer().writeByte(type);
        switch (type) {
            case 3: {
                m.writer().writeByte(this.quant_item_inbox(3));
                for (int i = 0; i < box3.length; i++) {
                    if (box3[i] != null) {
                        Item.readUpdateItem(m.writer(), box3[i], p);
                    }
                }
                break;
            }
            case 4:
            case 8: {
                m.writer().writeByte(this.quant_item_inbox(4));
                for (int i = 0; i < box47.size(); i++) {
                    if (box47.get(i).category == 4) {
                        m.writer().writeShort(box47.get(i).id);
                        m.writer().writeShort(box47.get(i).quant);
                    }
                }
                break;
            }
            case 5: {
                m.writer().writeByte(0);
                break;
            }
            case 7: {
                m.writer().writeByte(this.quant_item_inbox(7));
                for (int i = 0; i < box47.size(); i++) {
                    if (box47.get(i).category == 7) {
                        m.writer().writeByte(box47.get(i).id);
                        m.writer().writeShort(box47.get(i).quant);
                    }
                }
                break;
            }
        }
        if (b) {
            p.list_msg_cache.add(m);
        } else if (p.conn != null) {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public void update_assets_Inventory(boolean b) throws IOException {
        Message m = new Message(-12);
        m.writer().writeByte(3);
        m.writer().writeByte(6);
        //
        m.writer().writeLong(p.get_vang());
        m.writer().writeInt(f.setInteger(p.get_ngoc()));
        m.writer().writeShort(p.get_ticket_max()); // ticket
        m.writer().writeShort(p.get_ticket_max()); // max ticket
        m.writer().writeByte((byte) p.get_pvp_ticket_max());
        m.writer().writeByte(p.get_pvp_ticket_max()); // max pvp ticket
        m.writer().writeByte((byte) p.get_key_boss_max());
        m.writer().writeByte(p.get_key_boss_max()); // max key boss
        m.writer().writeInt(f.setInteger(p.get_vnd())); // vnd
        m.writer().writeInt(p.get_bua()); // bua
        m.writer().writeInt(p.get_coin()); // diem nap
        if (b) {
            p.list_msg_cache.add(m);
        } else if (p.conn != null) {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public void update_assets_Box(boolean b) throws IOException {
        Message m = new Message(-32);
        m.writer().writeByte(3);
        m.writer().writeByte(6);
        //
        m.writer().writeLong(p.get_vang());
        m.writer().writeInt(f.setInteger(p.get_ngoc()));
        m.writer().writeInt(f.setInteger(p.get_vnd())); // vnd
        m.writer().writeInt(p.get_coin()); // bua
        m.writer().writeInt(p.get_bua()); // diem nap
        if (b) {
            p.list_msg_cache.add(m);
        } else if (p.conn != null) {
            p.addmsg(m);
        }
        m.cleanup();
    }

    public static void readUpdateItem(DataOutputStream dos, Item_wear it, Player p)
            throws IOException {
        if (it == null) {
            core.GameLogger.error("Item.readUpdateItem Error: Item is null!");
            return;
        }

        // ALWAYS write the index FIRST to maintain protocol alignment
        dos.writeShort(it.index);

        if (it.template == null) {
            System.err.println(
                    "Item.readUpdateItem DataOutputStream Error: template is null for Item Index: " + it.index);
            dos.writeUTF("Unknown Item");
            dos.writeByte(0); // clazz
            dos.writeByte(0); // typeEquip
            dos.writeShort(0); // icon
            dos.writeShort(0); // level
            dos.writeByte(it.levelup);
            dos.writeByte(0); // color
            dos.writeByte(0); // 0
            dos.writeByte(it.typelock);
            dos.writeByte(it.numHoleDaDuc);
            dos.writeInt(it.timeUse);
            dos.writeShort(it.valueChetac);
            dos.writeByte(it.isHoanMy);
            dos.writeByte(it.valueKichAn);
            // Options
            dos.writeByte(0); // totalOptions
            // SocKet Options
            dos.writeByte(0); // socket options count
            dos.writeByte(it.numLoKham);
            if (it.mdakham == null) {
                it.mdakham = new short[0];
            }
            dos.writeByte(it.mdakham.length);
            for (int i = 0; i < it.mdakham.length; i++) {
                dos.writeShort(it.mdakham[i]);
            }
            return;
        }
        String nameDisplay = it.template.name;
        if (it.combo_pattern != null && !it.combo_pattern.isEmpty()) {
            nameDisplay += " " + devilfruit.DevilFruitManager
                    .buildComboDisplayString(it.mdakham, it.combo_pattern);
        }
        dos.writeUTF(nameDisplay);
        dos.writeByte(it.template.clazz);
        dos.writeByte(it.template.typeEquip);
        dos.writeShort(it.template.icon);
        dos.writeShort(it.template.level);
        dos.writeByte(it.levelup);
        dos.writeByte(it.template.color);
        dos.writeByte(0);
        dos.writeByte(it.typelock);
        dos.writeByte(it.numHoleDaDuc);
        dos.writeInt(it.timeUse);
        dos.writeShort(it.valueChetac);
        dos.writeByte(it.isHoanMy);
        dos.writeByte(it.valueKichAn);
        //
        int totalOptions = it.option_item.size();
        totalOptions += 1;
        if (it.template.typeEquip < 6 && it.levelup > 10) {
            totalOptions += 1;
        }
        dos.writeByte(totalOptions);
        int colorOptionId = 80 + it.template.color;
        if (it.template.color == 8 && it.template.tb2 == 0) {
            colorOptionId = 90;
            // Phẩm cao hơn (New Tier) từ 144 đến 153
            if (it.template.id >= 144 && it.template.id <= 153) {
                colorOptionId = 97;
            }
        }
        dos.writeByte(colorOptionId);
        dos.writeShort(1);
        for (int i = 0; i < it.option_item.size(); i++) {
            dos.writeByte(it.option_item.get(i).id);
            dos.writeShort((short) Math.max(0, Math.min(
                    it.option_item.get(i).getParam(it.template.typeEquip, it.levelup, it.isHoanMy),
                    32000)));
        }
        if (it.template.typeEquip < 6 && it.levelup > 10) {
            switch (it.template.typeEquip) {
                case 0: {
                    dos.writeByte(46);
                    dos.writeShort((50 * (it.levelup - 10)));
                    break;
                }
                case 2: {
                    dos.writeByte(53);
                    dos.writeShort((30 * (it.levelup - 10)));
                    break;
                }
                case 1:
                case 3:
                case 5: {
                    dos.writeByte(56);
                    dos.writeShort((100 * (it.levelup - 10)));
                    break;
                }
                default: { // 4
                    dos.writeByte(47);
                    dos.writeShort((20 * (it.levelup - 10)));
                    break;
                }
            }
        }
        java.util.List<Option> allSocketOptions = new java.util.ArrayList<>();
        for (int i = 0; i < it.option_item_2.size(); i++) {
            Option o = it.option_item_2.get(i);
            allSocketOptions.add(new Option(o.id, o.getParam()));
        }

        java.util.List<Option> synOpts = devilfruit.DevilFruitManager.getSynergyOptions(it.mdakham, it.combo_pattern);
        for (int i = 0; i < synOpts.size(); i++) {
            Option synOpt = synOpts.get(i);
            boolean found = false;
            for (int j = 0; j < allSocketOptions.size(); j++) {
                Option exist = allSocketOptions.get(j);
                if (exist.id == synOpt.id) {
                    exist.setParam(exist.getParam() + synOpt.getParam());
                    found = true;
                    break;
                }
            }
            if (!found) {
                allSocketOptions.add(new Option(synOpt.id, synOpt.getParam()));
            }
        }

        dos.writeByte(allSocketOptions.size());
        for (int i = 0; i < allSocketOptions.size(); i++) {
            dos.writeByte(allSocketOptions.get(i).id);
            dos.writeShort((short) Math.max(0, Math.min(allSocketOptions.get(i).getParam(), 32000)));
        }
        dos.writeByte(it.numLoKham);
        dos.writeByte(it.mdakham.length);
        for (int i = 0; i < it.mdakham.length; i++) {
            dos.writeShort(it.mdakham[i]);
        }
    }

    public static void readUpdateItem(String jsdata, Item_wear it) {
        JSONArray js = (JSONArray) JSONValue.parse(jsdata);
        if (js == null || js.isEmpty())
            return;

        int itId = Integer.parseInt(js.get(0).toString());
        it.template = ItemTemplate3.get_it_by_id(itId);

        // Safety check if template is still null after lookup
        if (it.template == null) {
            // Try to recover template if it's a Badge (IDs 183-188)
            if (itId >= 183 && itId <= 188) {
                template.ItemTemplate4 temp4 = template.ItemTemplate4.get_it_by_id(itId);
                if (temp4 != null) {
                    template.ItemTemplate3 temp3 = new template.ItemTemplate3();
                    temp3.id = (short) itId;
                    temp3.name = temp4.name;
                    temp3.icon = temp4.icon;
                    temp3.option_item = new java.util.ArrayList<>();
                    temp3.option_item_2 = new java.util.ArrayList<>();
                    temp3.mdakham = new short[0];
                    temp3.tb2 = 2; // Trang bị 3
                    temp3.typeEquip = (byte) (itId - 183);
                    it.template = temp3;
                }
            } else {
                // [RESILIENCE] Create a dummy template to prevent data loss if the real
                // template is missing/not loaded
                template.ItemTemplate3 dummyTemp = new template.ItemTemplate3();
                dummyTemp.id = (short) itId;
                dummyTemp.name = "Vật phẩm [" + itId + "]";
                dummyTemp.option_item = new java.util.ArrayList<>();
                dummyTemp.option_item_2 = new java.util.ArrayList<>();
                dummyTemp.mdakham = new short[0];
                dummyTemp.typeEquip = -1; // Unknown position
                it.template = dummyTemp;
                core.GameLogger.warn(
                        "Item.readUpdateItem: Using DUMMY template for unknown item ID " + itId + " to prevent loss.");
            }
        }

        if (it.template == null) {
            core.GameLogger.error("Item.readUpdateItem JSON Error: Template not found for item ID " + itId);
            return;
        }

        it.levelup = Byte.parseByte(js.get(1).toString());
        it.typelock = Byte.parseByte(js.get(2).toString());
        it.numHoleDaDuc = Byte.parseByte(js.get(3).toString());
        it.timeUse = Integer.parseInt(js.get(4).toString());
        it.valueChetac = Short.parseShort(js.get(5).toString());
        it.isHoanMy = Byte.parseByte(js.get(6).toString());
        it.valueKichAn = Byte.parseByte(js.get(7).toString());
        //
        it.option_item = new java.util.ArrayList<>();
        JSONArray js_op = (JSONArray) js.get(8);
        for (int i = 0; i < js_op.size(); i++) {
            JSONArray js_3 = (JSONArray) js_op.get(i);
            it.option_item.add(new Option(Integer.parseInt(js_3.get(0).toString()),
                    Integer.parseInt(js_3.get(1).toString())));
        }
        //
        it.option_item_2 = new java.util.ArrayList<>();
        JSONArray js_op_2 = (JSONArray) js.get(9);
        for (int i = 0; i < js_op_2.size(); i++) {
            JSONArray js_3 = (JSONArray) js_op_2.get(i);
            it.option_item_2.add(new Option(Integer.parseInt(js_3.get(0).toString()),
                    Integer.parseInt(js_3.get(1).toString())));
        }
        //
        it.numLoKham = Byte.parseByte(js.get(10).toString());
        JSONArray js_da = (JSONArray) js.get(11);
        it.mdakham = new short[js_da.size()];
        for (int i = 0; i < js_da.size(); i++) {
            it.mdakham[i] = Short.parseShort(js_da.get(i).toString());
        }
        it.index = Byte.parseByte(js.get(12).toString());
        if (js.size() > 13 && js.get(13) != null) {
            it.combo_pattern = js.get(13).toString();
        }
    }

    public synchronized boolean add_item_bag3(Item_wear it_add) {
        if (able_bag() > 0) {
            for (int i = 0; i < bag3.length; i++) {
                if (bag3[i] == null) {
                    bag3[i] = it_add;
                    it_add.index = (byte) i;
                    return true;
                }
            }
        }
        //
        if (it_add != null) {
            save_item_wear.add(it_add);
            while (save_item_wear.size() > 90) {
                save_item_wear.remove(0);
            }
        }
        //
        return false;
    }

    public synchronized boolean add_item_box3(Item_wear it_add) {
        if (able_box() > 0) {
            for (int i = 0; i < box3.length; i++) {
                if (box3[i] == null) {
                    box3[i] = it_add;
                    it_add.index = (byte) i;
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized int able_bag() {
        return this.max_bag - this.quant_item_inbag(3) - this.quant_item_inbag(4)
                - this.quant_item_inbag(5) - this.quant_item_inbag(7);
    }

    public synchronized int able_box() {
        return this.max_box - this.quant_item_inbox(3) - this.quant_item_inbox(4)
                - this.quant_item_inbox(7);
    }

    private int quant_item_inbag(int type) {
        int par = 0;
        switch (type) {
            case 3: {
                for (int i = 0; i < bag3.length; i++) {
                    if (bag3[i] != null) {
                        par++;
                    }
                }
                break;
            }
            case 4:
            case 5:
            case 7: {
                for (int i = 0; i < bag47.size(); i++) {
                    if (bag47.get(i).category == type) {
                        par++;
                    }
                }
                break;
            }
        }
        return par;
    }

    private int quant_item_inbox(int type) {
        int par = 0;
        switch (type) {
            case 3: {
                for (int i = 0; i < box3.length; i++) {
                    if (box3[i] != null) {
                        par++;
                    }
                }
                break;
            }
            case 4:
            case 7: {
                for (int i = 0; i < box47.size(); i++) {
                    if (box47.get(i).category == type) {
                        par++;
                    }
                }
                break;
            }
        }
        return par;
    }

    @SuppressWarnings("unchecked")
    public static JSONArray it_data_to_json(Item_wear it) {
        JSONArray js = new JSONArray();
        if (it == null || it.template == null) {
            return js;
        }
        js.add(it.template.id);
        js.add(it.levelup);
        js.add(it.typelock);
        js.add(it.numHoleDaDuc);
        js.add(it.timeUse);
        js.add(it.valueChetac);
        js.add(it.isHoanMy);
        js.add(it.valueKichAn);
        JSONArray js_2 = new JSONArray();
        for (int i = 0; i < it.option_item.size(); i++) {
            JSONArray js_3 = new JSONArray();
            js_3.add(it.option_item.get(i).id);
            js_3.add(it.option_item.get(i).getParam());
            js_2.add(js_3);
        }
        js.add(js_2);
        JSONArray js_4 = new JSONArray();
        for (int i = 0; i < it.option_item_2.size(); i++) {
            JSONArray js_3 = new JSONArray();
            js_3.add(it.option_item_2.get(i).id);
            js_3.add(it.option_item_2.get(i).getParam());
            js_4.add(js_3);
        }
        js.add(js_4);
        js.add(it.numLoKham);
        JSONArray js_5 = new JSONArray();
        for (int i = 0; i < it.mdakham.length; i++) {
            js_5.add(it.mdakham[i]);
        }
        js.add(js_5);
        js.add(it.index);
        js.add(it.combo_pattern);
        return js;
    }

    public synchronized boolean add_item_bag47(int type, int id, int num) {
        if ((total_item_bag_by_id(type, id) + num) > DataTemplate.MAX_ITEM_IN_BAG) {
            if ((type == 4 && id > 28) || (type == 7)) {
                ItemBag47 it_select = new ItemBag47();
                it_select.category = (byte) type;
                it_select.id = (short) id;
                it_select.quant = (short) num;

                save_item_47.add(it_select);
                if (save_item_47.size() > 90) {
                    save_item_47.remove(0);
                }
            }
            return false;
        }
        ItemBag47 it_select = null;
        for (int i = 0; i < bag47.size(); i++) {
            if (bag47.get(i).category == type && bag47.get(i).id == id) {
                it_select = bag47.get(i);
                break;
            }
        }
        if (it_select != null) {
            if ((it_select.quant + num) <= DataTemplate.MAX_ITEM_IN_BAG) {
                it_select.quant += num;
                return true;
            }
        } else {
            it_select = new ItemBag47();
            it_select.category = (byte) type;
            it_select.id = (short) id;
            it_select.quant = (short) num;
            this.bag47.add(it_select);
            return true;
        }
        return false;
    }

    public synchronized boolean add_item_box47(int type, int id, int num) {
        if (num > DataTemplate.MAX_ITEM_IN_BAG) {
            return false;
        }
        ItemBag47 it_select = null;
        for (int i = 0; i < box47.size(); i++) {
            if (box47.get(i).category == type && box47.get(i).id == id) {
                it_select = box47.get(i);
                break;
            }
        }
        if (it_select != null) {
            if ((it_select.quant + num) <= DataTemplate.MAX_ITEM_IN_BAG) {
                it_select.quant += num;
                return true;
            }
        } else {
            it_select = new ItemBag47();
            it_select.category = (byte) type;
            it_select.id = (short) id;
            it_select.quant = (short) num;
            this.box47.add(it_select);
            return true;
        }
        return false;
    }

    public synchronized boolean hasItemBag3(int id) {
        for (Item_wear it : bag3) {
            if (it != null && it.template.id == id) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean remove_ItemBag3(int id) {
        for (int i = 0; i < bag3.length; i++) {
            if (bag3[i] != null && bag3[i].template.id == id) {
                bag3[i] = null;
                return true;
            }
        }
        return false;
    }

    public synchronized int total_item_bag_by_id(int type, int id) {
        int par = 0;
        switch (type) {
            case 4:
            case 5:
            case 7: {
                for (int i = 0; i < bag47.size(); i++) {
                    if (bag47.get(i).category == type && bag47.get(i).id == id) {
                        par += bag47.get(i).quant;
                    }
                }
                break;
            }
        }
        return par;
    }

    public synchronized int total_item_box_by_id(int type, int id) {
        int par = 0;
        switch (type) {
            case 4:
            case 7: {
                for (int i = 0; i < box47.size(); i++) {
                    if (box47.get(i).category == type && box47.get(i).id == id) {
                        par += box47.get(i).quant;
                    }
                }
                break;
            }
        }
        return par;
    }

    public synchronized void remove_item47(int type, int id, int num) {
        ItemBag47 it_select = null;
        for (int i = 0; i < bag47.size(); i++) {
            if (bag47.get(i).category == type && bag47.get(i).id == id) {
                it_select = bag47.get(i);
                break;
            }
        }
        if (it_select != null) {
            it_select.quant -= num;
            if (it_select.quant <= 0) {
                bag47.remove(it_select);
            }
        }
    }

    public synchronized void remove_item47_box(int type, int id, int num) {
        ItemBag47 it_select = null;
        for (int i = 0; i < box47.size(); i++) {
            if (box47.get(i).category == type && box47.get(i).id == id) {
                it_select = box47.get(i);
                break;
            }
        }
        if (it_select != null) {
            it_select.quant -= num;
            if (it_select.quant <= 0) {
                box47.remove(it_select);
            }
        }
    }

    public synchronized void remove_item_wear(Item_wear item_wear) {
        for (int i = 0; i < bag3.length; i++) {
            if (bag3[i] != null && bag3[i].equals(item_wear)) {
                bag3[i] = null;
                break;
            }
        }
    }

    public synchronized void add_item_save(Item_wear item_wear) {
        if (item_wear != null) {
            save_item_wear.add(item_wear);
            if (save_item_wear.size() > 20) {
                save_item_wear.remove(0);
            }
        }
    }
}
