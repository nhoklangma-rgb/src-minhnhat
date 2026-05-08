package template;



public class MobTemplate {
	public static java.util.List<MobTemplate> ENTRYS = new java.util.ArrayList<>();
	public static java.util.Map<Integer, MobTemplate> MAP = new java.util.HashMap<>();

	public static MobTemplate get_it_by_id(int id) {
		return MAP.get(id);
	}

	public short mob_id;
	public String name;
	public short level;
	public short hOne;
	public long hp_max;
	public byte typemove;
	public byte ishuman;
	public byte typemonster;
	public short[] wearing;
	public short icon;
	public short head;
	public short hair;
	public short[] skill;
}
