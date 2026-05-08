package map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import client.Player;
import core.Util;
import io.Message;
import template.*;

public class LeaveItemMap {
    public static short[] ITEM_POTION = new short[] { 0 };

    public static void leave_item4(Map map, Mob mob_target, Player p) throws IOException {
        ItemMap itm = new ItemMap();
        itm.id = LeaveItemMap.ITEM_POTION[Util.random(LeaveItemMap.ITEM_POTION.length)];
        ItemTemplate4 temp4 = ItemTemplate4.get_it_by_id(itm.id);
        itm.category = 4;
        itm.icon = temp4.icon;
        itm.color = 0;
        if (itm.id == 0) {
            long vang_random = mob_target.level * (itm.id == 1 ? 2 : 12);
            vang_random -= (vang_random * Util.random(10)) / 100;
            //
            // Injected by StatsTool
            vang_random = (long) ((vang_random * (1000 + p.body.get_percent_beri_train())) / 1000
                    * Skill_info.GLOBAL_GOLD_RATE);
            //
            itm.quant = f.setInteger(vang_random);
            itm.name = vang_random + " " + temp4.name;
        }
        itm.id_master = p.index_map;
        itm.time_exist = System.currentTimeMillis() + 50_000L;
        itm.index = (short) map.get_index_item_map();
        if (itm.index > -1
                && p.item.total_item_bag_by_id(4, itm.id) < DataTemplate.MAX_ITEM_IN_BAG) {
            map.list_it_map[itm.index] = itm;
            //
            List<ItemMap> list_show = new ArrayList<>();
            list_show.add(itm);
            LeaveItemMap.show_item_map(map, list_show, mob_target, p);
        }
    }

    public static void leave_item4_little_garden(Map map, Mob mob_target, Player p)
            throws IOException {
        ItemMap itm = new ItemMap();
        switch (mob_target.mob_template.mob_id) {
            case 81: {
                itm.id = Util.random(94, 97);
                break;
            }
            default: { // 79 + 80
                itm.id = (2 > Util.random(180)) ? ((15 > Util.random(120)) ? 98 : 100)
                        : ((15 > Util.random(120)) ? 97 : 99);
                break;
            }
        }
        ItemTemplate4 temp4 = ItemTemplate4.get_it_by_id(itm.id);
        itm.category = 4;
        itm.icon = temp4.icon;
        itm.color = 0;
        itm.quant = 1;
        itm.name = temp4.name;
        itm.id_master = p.index_map;
        itm.time_exist = System.currentTimeMillis() + 50_000L;
        itm.index = (short) map.get_index_item_map();
        if (itm.index > -1
                && p.item.total_item_bag_by_id(4, itm.id) < DataTemplate.MAX_ITEM_IN_BAG) {
            map.list_it_map[itm.index] = itm;
            //
            List<ItemMap> list_show = new ArrayList<>();
            list_show.add(itm);
            LeaveItemMap.show_item_map(map, list_show, mob_target, p);
        }
    }

    public static void leave_item7(Map map, Mob mob_target, Player p) throws IOException {
        int nDrop = (int) Skill_info.GLOBAL_DROP_RATE;
        if (core.Util.random(100) < (Skill_info.GLOBAL_DROP_RATE - nDrop) * 100)
            nDrop++;
        for (int k = 0; k < nDrop; k++)
            leave_item7_internal(map, mob_target, p);
    }

    private static void leave_item7_internal(Map map, Mob mob_target, Player p) throws IOException {
        ItemMap itm = new ItemMap();
        itm.id = 0;
        ItemTemplate7 temp7 = ItemTemplate7.get_it_by_id(itm.id);
        itm.category = 7;
        itm.icon = temp7.icon;
        itm.color = 0;
        itm.quant = Util.random(1, 2);
        itm.name = temp7.name;
        itm.id_master = p.index_map;
        itm.time_exist = System.currentTimeMillis() + 50_000L;
        itm.index = (short) map.get_index_item_map();
        if (itm.index > -1
                && p.item.total_item_bag_by_id(7, itm.id) < DataTemplate.MAX_ITEM_IN_BAG) {
            map.list_it_map[itm.index] = itm;
            //
            List<ItemMap> list_show = new ArrayList<>();
            list_show.add(itm);
            LeaveItemMap.show_item_map(map, list_show, mob_target, p);
        }
    }

    public static void leave_item_quest(Map map, Mob mob_target, Player p) throws IOException {
        List<QuestP> questCheck = new ArrayList<>();
        for (int j = 0; j < p.list_quest.size(); j++) {
            QuestP tempP = p.list_quest.get(j);
            if (tempP.template.statusQuest == 1) {
                for (int k = 0; k < tempP.data.length; k++) {
                    if (tempP.data[k].length == 4 && tempP.data[k][0] == 2) {
                        if (!questCheck.contains(tempP)) {
                            questCheck.add(tempP);
                        }
                    }
                }
            }
        }
        ItemMap itm = new ItemMap();
        itm.id = -1;
        itm.category = 5;
        itm.icon = -1;
        itm.color = 0;
        itm.quant = 1;
        itm.name = "";
        itm.id_master = p.index_map;
        itm.time_exist = System.currentTimeMillis() + 50_000L;
        itm.index = (short) map.get_index_item_map();
        for (int j = 0; j < questCheck.size(); j++) {
            QuestP tempP = questCheck.get(j);
            for (int i = 0; i < tempP.data.length; i++) {
                if (tempP.data[i][1] == 0 && mob_target.mob_template.mob_id == 1) { // thit heo
                    itm.id = 0;
                    itm.icon = 0;
                } else if (tempP.data[i][1] == 2 && mob_target.mob_template.mob_id == 46) { // rang chuot cong
                    itm.id = 2;
                    itm.icon = 2;
                } else if (tempP.data[i][1] == 1 && mob_target.mob_template.mob_id == 6) { // kiem sat
                    itm.id = 1;
                    itm.icon = 1;
                } else if (tempP.data[i][1] == 3 && mob_target.mob_template.mob_id == 47) { // bach tuot
                    itm.id = 3;
                    itm.icon = 3;
                } else if (tempP.data[i][1] == 4 && mob_target.mob_template.mob_id == 48) { // long chuot
                    itm.id = 4;
                    itm.icon = 4;
                } else if (tempP.data[i][1] == 5 && mob_target.mob_template.mob_id == 49) { // vay ca map
                    itm.id = 5;
                    itm.icon = 5;
                } else if (tempP.data[i][1] == 6 && mob_target.mob_template.mob_id == 50) { // phan hoa
                    itm.id = 6;
                    itm.icon = 6;
                } else if (tempP.data[i][1] == 7 && mob_target.mob_template.mob_id == 51) { // thit ca
                    itm.id = 7;
                    itm.icon = 7;
                } else if (tempP.data[i][1] == 8 && mob_target.mob_template.mob_id == 52) { // muc tuoi
                    itm.id = 8;
                    itm.icon = 8;
                } else if (tempP.data[i][1] == 9 && mob_target.mob_template.mob_id == 54) { // noc ran
                    itm.id = 9;
                    itm.icon = 9;
                } else if (tempP.data[i][1] == 10 && mob_target.mob_template.mob_id == 55) { // vay rong
                    itm.id = 10;
                    itm.icon = 10;
                } else if (tempP.data[i][1] == 11 && mob_target.mob_template.mob_id == 60) { // sung bo
                    itm.id = 11;
                    itm.icon = 11;
                } else if (tempP.data[i][1] == 12 && mob_target.mob_template.mob_id == 61) { // thit heo nui
                    itm.id = 12;
                    itm.icon = 12;
                } else if (tempP.data[i][1] == 13 && mob_target.mob_template.mob_id == 71) { // gat huu
                    itm.id = 13;
                    itm.icon = 13;
                } else if (tempP.data[i][1] == 14 && mob_target.mob_template.mob_id == 72) { // tai tho
                    itm.id = 14;
                    itm.icon = 14;
                } else if (tempP.data[i][1] == 15 && mob_target.mob_template.mob_id == 95) { // long co trang
                    itm.id = 15;
                    itm.icon = 15;
                } else if (tempP.data[i][1] == 16 && mob_target.mob_template.mob_id == 113) { // mang ca map
                    itm.id = 16;
                    itm.icon = 16;
                } else if (tempP.data[i][1] == 17 && mob_target.mob_template.mob_id == 100) { // rang thuong luong
                    itm.id = 17;
                    itm.icon = 17;
                }
                if (itm.id > -1) {
                    break;
                }
            }
        }
        if (itm.id > -1 && itm.index > -1) {
            itm.name = DataTemplate.NamePotionquest[itm.id];
            map.list_it_map[itm.index] = itm;
            //
            List<ItemMap> list_show = new ArrayList<>();
            list_show.add(itm);
            LeaveItemMap.show_item_map(map, list_show, mob_target, p);
        }
    }

    public static void show_item_map(Map map, List<ItemMap> list_show, Mob mob_target, Player p)
            throws IOException {
        Message m = new Message(11);
        m.writer().writeByte(list_show.size()); // size item
        for (int i = 0; i < list_show.size(); i++) {
            ItemMap itm = list_show.get(i);
            m.writer().writeShort(itm.index); // index item
            m.writer().writeByte(itm.category); // item category
            m.writer().writeShort(itm.icon); // icon
            m.writer().writeByte(itm.color); // color
            m.writer().writeUTF(itm.name); // name
            //
            m.writer().writeShort(mob_target != null ? mob_target.index : -1); // index mob leave item
            m.writer().writeByte(1); // type obj
            if (p == null) {
                p = map.get_player_by_id_inmap(itm.id_master);
            }
            m.writer().writeShort(p != null ? p.index_map : -1);
        }
        map.send_msg_all_p(m, null, true);
        m.cleanup();
    }

    public static void leave_item3(Map map, Mob mob_target, Player p) throws IOException {
        int nDrop = (int) Skill_info.GLOBAL_DROP_RATE;
        if (core.Util.random(100) < (Skill_info.GLOBAL_DROP_RATE - nDrop) * 100)
            nDrop++;
        for (int k = 0; k < nDrop; k++)
            leave_item3_internal(map, mob_target, p);
    }

    private static void leave_item3_internal(Map map, Mob mob_target, Player p) throws IOException {
        int dem = 0;
        byte it_color = (byte) ((80 > Util.random(120)) ? 0 : 1);
        List<ItemSell> list_sell = ItemSell.get_it_sell(p.level, Util.random(5));
        ItemSell it_sell_temp = list_sell.get(Util.random(list_sell.size()));
        ItemTemplate3 template3 = ItemTemplate3.get_it_by_id(it_sell_temp.id);
        while (dem < 100 && it_color != template3.color) {
            it_sell_temp = list_sell.get(Util.random(list_sell.size()));
            template3 = ItemTemplate3.get_it_by_id(it_sell_temp.id);
            dem++;
        }
        ItemMap itm = new ItemMap();
        itm.id = template3.id;
        itm.category = 3;
        itm.icon = template3.icon;
        itm.color = template3.color;
        itm.quant = 1;
        itm.name = template3.name;
        itm.id_master = p.index_map;
        itm.time_exist = System.currentTimeMillis() + 50_000L;
        itm.index = (short) map.get_index_item_map();
        if (itm.index > -1) {
            map.list_it_map[itm.index] = itm;
            //
            List<ItemMap> list_show = new ArrayList<>();
            list_show.add(itm);
            LeaveItemMap.show_item_map(map, list_show, mob_target, p);
        }
    }
}
