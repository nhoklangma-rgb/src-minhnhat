package template;

import activities.Market;
import org.json.simple.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ItemMarket {
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
    public short index;
    public long time_market;
    public int price_market;
    public String seller;
    public String combo_pattern;
    public byte type_market;
    public final AtomicBoolean is_processing = new AtomicBoolean(false);

    public void clone_from_item_wear(Item_wear it_temp) {
        this.index = -1;
        if (it_temp.template != null) {
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
                this.option_item.add(new Option(it_temp.option_item.get(i).id,
                        it_temp.option_item.get(i).getParam()));
            }
            this.option_item_2 = new ArrayList<>();
            for (int i = 0; i < it_temp.option_item_2.size(); i++) {
                this.option_item_2.add(new Option(it_temp.option_item_2.get(i).id,
                        it_temp.option_item_2.get(i).getParam()));
            }
            this.numLoKham = it_temp.numLoKham;
            this.mdakham = new short[it_temp.mdakham.length];
            for (int i = 0; i < it_temp.mdakham.length; i++) {
                this.mdakham[i] = it_temp.mdakham[i];
            }
            this.combo_pattern = it_temp.combo_pattern;
            this.type_market = 1;
            this.index = Market.get_index();
        }
    }

    public void load_json(JSONArray js) {
        this.index = -1;
        ItemTemplate3 template = ItemTemplate3.get_it_by_id(Integer.parseInt(js.get(0).toString()));
        if (template != null) {
            this.template = template;
            this.levelup = Byte.parseByte(js.get(1).toString());
            this.typelock = Byte.parseByte(js.get(2).toString());
            this.numHoleDaDuc = Byte.parseByte(js.get(3).toString());
            this.timeUse = Integer.parseInt(js.get(4).toString());
            this.valueChetac = Short.parseShort(js.get(5).toString());
            this.isHoanMy = Byte.parseByte(js.get(6).toString());
            this.valueKichAn = Byte.parseByte(js.get(7).toString());
            this.option_item = new ArrayList<>();
            JSONArray js2 = (JSONArray) js.get(8);
            for (int i = 0; i < js2.size(); i++) {
                JSONArray js3 = (JSONArray) js2.get(i);
                this.option_item.add(new Option(Byte.parseByte(js3.get(0).toString()),
                        Integer.parseInt(js3.get(1).toString())));
            }
            js2.clear();
            this.option_item_2 = new ArrayList<>();
            js2 = (JSONArray) js.get(9);
            for (int i = 0; i < js2.size(); i++) {
                JSONArray js3 = (JSONArray) js2.get(i);
                this.option_item_2.add(new Option(Byte.parseByte(js3.get(0).toString()),
                        Integer.parseInt(js3.get(1).toString())));
            }
            js2.clear();
            this.numLoKham = Byte.parseByte(js.get(10).toString());
            js2 = (JSONArray) js.get(11);
            this.mdakham = new short[js2.size()];
            for (int i = 0; i < this.mdakham.length; i++) {
                this.mdakham[i] = Short.parseShort(js2.get(i).toString());
            }
            this.time_market = System.currentTimeMillis() + Long.parseLong(js.get(12).toString());
            this.price_market = Integer.parseInt(js.get(13).toString());
            this.seller = js.get(14).toString();
            if (js.size() > 16 && js.get(16) != null) {
                this.combo_pattern = js.get(16).toString();
            }
            this.type_market = Byte.parseByte(js.get(15).toString());
            this.index = Market.get_index();
        }
    }
}
