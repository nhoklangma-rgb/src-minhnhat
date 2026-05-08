package template;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GiftTemplate {
	public static List<GiftTemplate> ENTRY = new ArrayList<>();

	public String giftname;
	public int luotnhap;
	public int gioihan;
	public String notice;
	public long beri;
	public long ruby;
	public long extol;
	public long coin;
	public byte[] type;
	public short[] id;
	public short[] quant;
	public String used;
	public String special;
	public boolean visible;

	public GiftTemplate(String giftname, int luotnhap, int gioihan, String notice, long beri, long ruby, long extol,
			long coin,
			String item, String used, String special, boolean visible) {
		this.giftname = giftname;
		this.luotnhap = luotnhap;
		this.gioihan = gioihan;
		this.notice = (notice != null) ? notice : "";
		this.beri = beri;
		this.ruby = ruby;
		this.extol = extol;
		this.coin = coin;
		JSONArray js = (JSONArray) JSONValue.parse(item);
		if (js != null && js.size() > 0) {
			this.type = new byte[js.size()];
			this.id = new short[js.size()];
			this.quant = new short[js.size()];
			for (int i = 0; i < id.length; i++) {
				JSONArray js2 = (JSONArray) JSONValue.parse(js.get(i).toString());
				this.type[i] = Byte.parseByte(js2.get(0).toString());
				this.id[i] = Short.parseShort(js2.get(1).toString());
				this.quant[i] = Short.parseShort(js2.get(2).toString());
			}
		}
		this.used = (used != null) ? used : "";
		this.special = (special != null) ? special : "";
		this.used = this.used.replace(" ", "");
		this.special = this.special.replace(" ", "");
		this.visible = visible;
	}

	public static void load() {
		ENTRY.clear();
		try {
			byte[] ab = core.Util.loadfile("data/gift_codes.json");
			if (ab == null)
				return;
			String data = new String(ab, "UTF-8");
			JSONArray arr = (JSONArray) JSONValue.parse(data);
			if (arr == null)
				return;
			for (int i = 0; i < arr.size(); i++) {
				JSONObject obj = (JSONObject) arr.get(i);
				boolean visible = obj.containsKey("visible") ? Boolean.parseBoolean(obj.get("visible").toString())
						: true;
				GiftTemplate temp = new GiftTemplate(
						obj.get("name").toString(),
						Integer.parseInt(obj.get("luotnhap").toString()),
						Integer.parseInt(obj.get("limit").toString()),
						"",
						Long.parseLong(obj.get("beri").toString()),
						Long.parseLong(obj.get("ruby").toString()),
						obj.containsKey("extol") ? Long.parseLong(obj.get("extol").toString()) : 0L,
						obj.containsKey("coin") ? Long.parseLong(obj.get("coin").toString()) : 0L,
						obj.get("items").toString(),
						obj.get("used").toString(),
						obj.containsKey("special") ? obj.get("special").toString() : "",
						visible);
				ENTRY.add(temp);
			}
			core.GameLogger.info("Load " + ENTRY.size() + " giftcodes");
		} catch (Exception e) {
			core.GameLogger.error("[Disconnect] GiftTemplate.load error: " + e.getMessage());
		}
	}

	public static synchronized void update_used(GiftTemplate temp, String name) {
		temp.used += (name + ",");
		temp.luotnhap++;
		save();
	}

	public static void save() {
		JSONArray arr = new JSONArray();
		for (GiftTemplate temp : ENTRY) {
			JSONObject obj = new JSONObject();
			obj.put("name", temp.giftname);
			obj.put("luotnhap", temp.luotnhap);
			obj.put("limit", temp.gioihan);
			obj.put("beri", temp.beri);
			obj.put("ruby", temp.ruby);
			obj.put("extol", temp.extol);
			obj.put("coin", temp.coin);
			obj.put("used", temp.used);
			obj.put("special", temp.special);
			obj.put("visible", temp.visible);

			JSONArray items = new JSONArray();
			if (temp.type != null) {
				for (int i = 0; i < temp.type.length; i++) {
					JSONArray it = new JSONArray();
					it.add((int) temp.type[i]);
					it.add((int) temp.id[i]);
					it.add((int) temp.quant[i]);
					items.add(it);
				}
			}
			obj.put("items", items.toJSONString());
			arr.add(obj);
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/gift_codes.json"))) {
			writer.write(arr.toJSONString());
		} catch (IOException e) {
			core.GameLogger.error("[Disconnect] GiftTemplate.save error: " + e.getMessage());
		}
	}
}
