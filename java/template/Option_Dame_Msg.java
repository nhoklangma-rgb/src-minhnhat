package template;

import map.f;

public class Option_Dame_Msg {
    public short type;
    public short hp;
    public short time;

    public Option_Dame_Msg(int type, int hp, int time) {
        this.type = f.setShort((long) type);
        this.hp = f.setShort((long) hp);
        this.time = f.setShort((long) time);
    }
}
