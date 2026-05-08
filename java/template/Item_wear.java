package template;

import java.util.ArrayList;
import java.util.List;

import core.Util;

public class Item_wear {
    public ItemTemplate3 template;
    public byte levelup;
    public byte typelock;
    public byte numHoleDaDuc;
    public int timeUse;
    public short valueChetac;
    public byte isHoanMy;
    public byte valueKichAn;
    public List<Option> option_item;
    public List<Option> option_item_2;
    public byte numLoKham;
    public short[] mdakham;
    public byte index;
    public String combo_pattern = "";

    @Override
    public String toString() {
        return "Item_wear [Name=" + (template != null ? template.name : "Unknown") +
                ", ID=" + (template != null ? template.id : "Unknown") +
                ", Levelup=" + levelup + "]";
    }

    public void setup_template_by_id(ItemTemplate3 it_temp) {
        if (it_temp != null) {
            this.template = it_temp;
            this.levelup = 0;
            this.typelock = it_temp.typelock;
            this.numHoleDaDuc = it_temp.numHoleDaDuc;
            this.timeUse = 0;
            this.valueChetac = (short) Util.random(50, 100);
            this.isHoanMy = it_temp.isHoanMy;
            this.valueKichAn = it_temp.valueKichAn;
            this.option_item = new ArrayList<>();
            for (int i = 0; i < it_temp.option_item.size(); i++) {
                int param_random = it_temp.option_item.get(i).getParam();
                if (param_random > 5 && param_random <= 10) {
                    param_random -= Util.random(5);
                    if (param_random < 5) {
                        param_random = 5;
                    }
                } else if (param_random > 10) {
                    param_random = (param_random * Util.random(90, 101)) / 100;
                }
                this.option_item.add(new Option(it_temp.option_item.get(i).id, param_random));
            }
            this.option_item_2 = new ArrayList<>();
            this.numLoKham = it_temp.numLoKham;
            this.mdakham = new short[it_temp.mdakham.length];
            for (int i = 0; i < it_temp.mdakham.length; i++) {
                this.mdakham[i] = it_temp.mdakham[i];
            }
            this.index = 0;
        }
    }

    public void setup_template_by_id(int id) {
        ItemTemplate3 it_temp = ItemTemplate3.get_it_by_id(id);
        if (it_temp != null) {
            this.template = it_temp;
            this.levelup = 0;
            this.typelock = it_temp.typelock;
            this.numHoleDaDuc = it_temp.numHoleDaDuc;
            this.timeUse = 0;
            this.valueChetac = (short) Util.random(50, 100);
            this.isHoanMy = it_temp.isHoanMy;
            this.valueKichAn = it_temp.valueKichAn;
            this.option_item = new ArrayList<>();
            for (int i = 0; i < it_temp.option_item.size(); i++) {
                int param_random = it_temp.option_item.get(i).getParam();
                if (param_random > 5 && param_random <= 10) {
                    param_random -= Util.random(5);
                    if (param_random < 5) {
                        param_random = 5;
                    }
                } else if (param_random > 10) {
                    param_random = (param_random * Util.random(90, 101)) / 100;
                }
                this.option_item.add(new Option(it_temp.option_item.get(i).id, param_random));
            }
            this.option_item_2 = new ArrayList<>();
            this.numLoKham = it_temp.numLoKham;
            this.mdakham = new short[it_temp.mdakham.length];
            for (int i = 0; i < it_temp.mdakham.length; i++) {
                this.mdakham[i] = it_temp.mdakham[i];
            }
            this.index = 0;
        }
    }

    public Item_wear clone_obj() {
        Item_wear clone = new Item_wear();
        clone.clone_obj(this);
        return clone;
    }

    public void clone_obj(Item_wear it_temp) {
        if (it_temp != null) {
            this.template = it_temp.template;
            this.levelup = it_temp.levelup;
            this.typelock = it_temp.typelock;
            this.numHoleDaDuc = it_temp.numHoleDaDuc;
            this.timeUse = it_temp.timeUse;
            this.valueChetac = it_temp.valueChetac;
            this.isHoanMy = it_temp.isHoanMy;
            this.valueKichAn = it_temp.valueKichAn;
            this.option_item = new ArrayList<>();
            for (int i = 0; i < it_temp.option_item.size(); i++) {
                this.option_item.add(new Option(it_temp.option_item.get(i).id, it_temp.option_item.get(i).getParam()));
            }
            this.option_item_2 = new ArrayList<>();
            for (int i = 0; i < it_temp.option_item_2.size(); i++) {
                this.option_item_2
                        .add(new Option(it_temp.option_item_2.get(i).id,
                                it_temp.option_item_2.get(i).getParam()));
            }
            this.numLoKham = it_temp.numLoKham;
            this.mdakham = new short[it_temp.mdakham.length];
            for (int i = 0; i < it_temp.mdakham.length; i++) {
                this.mdakham[i] = it_temp.mdakham[i];
            }
            this.combo_pattern = it_temp.combo_pattern;
            this.index = it_temp.index;
        }
    }

    public void clone_obj(ItemMarket it) {
        this.template = it.template;
        this.levelup = it.levelup;
        this.typelock = it.typelock;
        this.numHoleDaDuc = it.numHoleDaDuc;
        this.timeUse = it.timeUse;
        this.valueChetac = it.valueChetac;
        this.isHoanMy = it.isHoanMy;
        this.valueKichAn = it.valueKichAn;
        this.option_item = new ArrayList<>();
        for (int i = 0; i < it.option_item.size(); i++) {
            this.option_item.add(new Option(it.option_item.get(i).id, it.option_item.get(i).getParam()));
        }
        this.option_item_2 = new ArrayList<>();
        for (int i = 0; i < it.option_item_2.size(); i++) {
            this.option_item_2.add(new Option(it.option_item_2.get(i).id,
                    it.option_item_2.get(i).getParam()));
        }
        this.numLoKham = it.numLoKham;
        this.mdakham = new short[it.mdakham.length];
        for (int i = 0; i < it.mdakham.length; i++) {
            this.mdakham[i] = it.mdakham[i];
        }
        this.combo_pattern = it.combo_pattern;
        this.index = 0;
    }
}