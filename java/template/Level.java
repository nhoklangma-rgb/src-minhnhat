package template;

public class Level {
    public static Level[] ENTRYS;
    public static long[] LEVEL_THONGTHAO;
    public long exp;
    public int tiemnang;

    public Level() {
    }

    public Level(long a, int tn) {
        this.exp = a;
        this.tiemnang = tn;
    }

    static {
        // long tong = 0;
        ENTRYS = new Level[250];
        int tn = Skill_info.POTENTIAL_PER_LEVEL;
        ENTRYS[0] = new Level(10150, tn); // 1
        ENTRYS[1] = new Level(20860, tn); // 2
        ENTRYS[2] = new Level(30575, tn); // 3
        ENTRYS[3] = new Level(40780, tn); // 4
        ENTRYS[4] = new Level(60760, tn); // 5
        ENTRYS[5] = new Level(100130, tn); // 6
        ENTRYS[6] = new Level(106040, tn); // 7
        ENTRYS[7] = new Level(208000, tn); // 8
        ENTRYS[8] = new Level(409000, tn); // 9
        ENTRYS[9] = new Level(606400, tn); // 10
        ENTRYS[10] = new Level(709450, tn); // 11
        ENTRYS[11] = new Level(1002170, tn); // 12
        ENTRYS[12] = new Level(1025400, tn); // 13
        ENTRYS[13] = new Level(1038000, tn); // 14
        ENTRYS[14] = new Level(1050220, tn); // 15
        for (int i = 15; i < 250; i++) {
            Level temp = ENTRYS[i - 1];
            long value = temp.exp;
            if (i < 200) {
                value = value + (value * ((i - 1) + 2)) / 712;
            }
            ENTRYS[i] = new Level(value, tn);
        }
        LEVEL_THONGTHAO = new long[250];
        LEVEL_THONGTHAO[0] = ENTRYS[98].exp;
        for (int i = 1; i < LEVEL_THONGTHAO.length; i++) {
            LEVEL_THONGTHAO[i] = (LEVEL_THONGTHAO[i - 1] * 11) / 10;
        }
    }

    public static int get_total_point_by_level(int level) {
        int tn = Skill_info.POTENTIAL_PER_LEVEL;
        int par = tn;
        for (int i = 0; i < level; i++) {
            par += tn;
        }
        return par;
    }
}
