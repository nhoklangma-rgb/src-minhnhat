package map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import client.Player;
import template.MobTemplate;

public class Mob {
    public final static HashMap<Integer, Mob> ENTRYS = new HashMap<>();
    public final static int TIME_RESPAWN = 7;
    public short x, y;
    public long hp, hp_max;
    public int level;
    public MobTemplate mob_template;
    public boolean isdie;
    public int id_target;
    public int index;
    public long time_skill;
    public long time_refresh;
    public Map map;
    public byte type_pk;
    public boolean isDynamic;
}
