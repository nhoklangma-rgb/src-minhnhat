package template;

import client.Player;
import tool.StatsTool;
import tool.SkillTool;

public class Skill_info {
    // === Config from htth.conf (Loads dynamically at startup via Manager) ===
    public static float GLOBAL_DAME_RATE = core.Manager.gI().global_dame_rate;
    public static float GLOBAL_MOB_HP_RATE = core.Manager.gI().mob_hp_rate;
    public static float GLOBAL_EXP_RATE = core.Manager.gI().global_exp_rate;
    public static float GLOBAL_GOLD_RATE = core.Manager.gI().global_gold_rate;
    public static float GLOBAL_DROP_RATE = core.Manager.gI().global_drop_rate;
    public static float GLOBAL_MANA_RATE = core.Manager.gI().mana_cost_rate;
    public static float GLOBAL_COOLDOWN_RATE = core.Manager.gI().cooldown_rate;
    public static int GLOBAL_MOB_RESPAWN_TIME = core.Manager.gI().mob_respawn_time;
    public static int POTENTIAL_PER_LEVEL = core.Manager.gI().potential_per_level;
    public static int STAT_POINT_MAX = core.Manager.gI().stat_point_max;
    // Scaling rates for potential points (Referencing Manager)
    public static float ATK_SCALING = core.Manager.gI().atk_scaling;
    public static float DEF_SCALING = core.Manager.gI().def_scaling;
    public static float HP_SCALING = core.Manager.gI().hp_scaling;
    public static float MP_SCALING = core.Manager.gI().mp_scaling;

    public static void updateConfig() {
        GLOBAL_DAME_RATE = core.Manager.gI().global_dame_rate;
        GLOBAL_MOB_HP_RATE = core.Manager.gI().mob_hp_rate;
        GLOBAL_EXP_RATE = core.Manager.gI().global_exp_rate;
        GLOBAL_GOLD_RATE = core.Manager.gI().global_gold_rate;
        GLOBAL_DROP_RATE = core.Manager.gI().global_drop_rate;
        GLOBAL_MANA_RATE = core.Manager.gI().mana_cost_rate;
        GLOBAL_COOLDOWN_RATE = core.Manager.gI().cooldown_rate;
        GLOBAL_MOB_RESPAWN_TIME = core.Manager.gI().mob_respawn_time;
        POTENTIAL_PER_LEVEL = core.Manager.gI().potential_per_level;
        STAT_POINT_MAX = core.Manager.gI().stat_point_max;
        ATK_SCALING = core.Manager.gI().atk_scaling;
        DEF_SCALING = core.Manager.gI().def_scaling;
        HP_SCALING = core.Manager.gI().hp_scaling;
        MP_SCALING = core.Manager.gI().mp_scaling;
        POINT4_EXTRA = new float[][] { { 16, core.Manager.gI().spirit_extra_mana } };
        MAX_LEVEL = core.Manager.gI().max_skill_level;
        SKILL_EXP_RATE = core.Manager.gI().skill_exp_rate;
    }

    // Extra bonus per point column
    public static float[][] POINT1_EXTRA = StatsTool.getPoint1Extra();
    public static float[][] POINT2_EXTRA = StatsTool.getPoint2Extra();
    public static float[][] POINT3_EXTRA = StatsTool.getPoint3Extra();
    public static float[][] POINT4_EXTRA = new float[][] { { 16, core.Manager.gI().spirit_extra_mana } };
    public static float[][] POINT5_EXTRA = StatsTool.getPoint5Extra();
    // === Config from SkillTool ===
    public static int MAX_LEVEL = 1000;
    public static float SKILL_EXP_RATE = 1.0E9f;
    public int realLevel = 0; // Tracks the *actual* level of the skill, independent of the template Lv_RQ
    // idEff 1: gay choang
    //
    // idEff 2: gay chay mau
    // idEff 3: gay giam cong
    // idEff 4: gay giam thu
    // idEff 5: gay hoa mat
    // idEff 6: gay dien giat
    // idEff 7: gay lua chay
    //
    // idEff 8: gay troi chan
    // idEff 9: gay hut nang luong
    // idEff 10: gay trung doc
    // idEff 11: gay bat tu
    // idEff 12: gay chi mang lien tuc
    // idEff 13: gay tru bat tu
    // idEff 14: gay tru bat tu (co dau)
    // idEff 15: gay hut suc manh
    // idEff 16: gay hoang loan
    //

    // REGION_EXP_DEVIL_START
    public static int[] EXP_DEVIL = new int[] { 100, 125, 167, 200, 250 };
    // REGION_EXP_DEVIL_END
    public long exp;
    public Skill_Template temp;
    public byte lvdevil;
    public byte devilpercent;
    public int versionClient;
    public int fruitId;
    // REGION_EXP_START
    public static long[] EXP = new long[1010];
    static {
        Skill_info.EXP[0] = 3400;
        Skill_info.EXP[1] = 10000;
        Skill_info.EXP[2] = 30000;
        Skill_info.EXP[3] = 60000;
        Skill_info.EXP[4] = 120000;
        for (int i = 5; i < Skill_info.EXP.length; i++) {
            if (Skill_info.EXP[i - 1] > Long.MAX_VALUE / 102) {
                Skill_info.EXP[i] = Skill_info.EXP[i - 1];
            } else {
                long nextExp = (Skill_info.EXP[i - 1] * 102) / 100;
                Skill_info.EXP[i] = nextExp;
            }
        }
    }
    // REGION_EXP_END

    public int get_percent() {
        if (exp <= 0) {
            return 0;
        }
        long exp_total = Skill_info.EXP[temp.Lv_RQ - 1];
        if (exp_total == 0) {
            return 0;
        }
        int percent = (int) ((exp * 1000L) / exp_total);
        if (percent == 999 && exp >= exp_total - 1) {
            percent = 1000;
        }
        return Math.min(percent, 1000); // UI cap at 100% (1000 in client logic)
    }

    public long get_dame(Player p) {
        long result = this.temp.damage;
        if (this.temp.ID >= 2000) {
            // [SAFETY] Nhân Dame cơ bản với % Dame chiêu thức bằng double
            result = (long) ((double) p.skill_point.get(0).get_dame(p) * this.temp.percentDame / 100.0);
        }
        if (this.temp.ID == 0 || this.temp.ID == 1 || this.temp.ID == 2) {
            switch (this.lvdevil) {
                case 1: {
                    result = (long) ((double) result * 1.1);
                    break;
                }
                case 2: {
                    result = (long) ((double) result * 1.25);
                    break;
                }
                case 3: {
                    result = (long) ((double) result * 1.45);
                    break;
                }
                case 4: {
                    result = (long) ((double) result * 1.7);
                    break;
                }
                case 5: {
                    result *= 2;
                    break;
                }
            }
        } else if (this.temp.ID >= 2000 && this.temp.ID < 3000) {
            // switch (this.lvdevil) {
            // case 1: {
            // result = (result * 121) / 100;
            // break;
            // }
            // case 2: {
            // result = (result * 1567) / 1000;
            // break;
            // }
            // case 3: {
            // result = (result * 2106) / 1000;
            // break;
            // }
            // case 4: {
            // result = (result * 2889) / 1000;
            // break;
            // }
            // case 5: {
            // result *= 4;
            // break;
            // }
            // }
        }
        // Applied Global Damage Rate
        long finalDamage = (long) (result * GLOBAL_DAME_RATE);
        if (finalDamage == 0 && result > 0) {
            finalDamage = 1;
        }
        return finalDamage;
    }

    public void update_exp_devil(int exp_devil) {
        int exp = (this.devilpercent * Skill_info.EXP_DEVIL[this.lvdevil]) / 100;
        exp += exp_devil;
        while (this.lvdevil < 5 && exp >= Skill_info.EXP_DEVIL[this.lvdevil]) {
            exp -= Skill_info.EXP_DEVIL[this.lvdevil];
            this.lvdevil++;
        }
        if (this.lvdevil == 5) {
            this.devilpercent = 0;
        } else {
            this.devilpercent = (byte) ((exp * 100) / Skill_info.EXP_DEVIL[this.lvdevil]);
        }
    }

    public short get_eff_skill() {
        if (versionClient < 129) {
            if (temp.Lv_RQ >= 30) {
                Skill_Template tempLv25 = Skill_Template.getSkillTemplateLv25(temp);
                if (tempLv25 != null) {
                    return tempLv25.getTypeEffSkill();
                }
            }
        }
        if (temp.ID >= 2000 && lvdevil == 5) {
            switch (temp.indexSkillInServer) {
                case 658:
                    return 402;
                case 659:
                    return 403;
                case 475:
                    return 228;
                case 476:
                    return 229;
                case 477:
                    return 227;
                case 485:
                    return 232;
                case 486:
                    return 234;
                case 520:
                    return 236;
                case 521:
                    return 235;
                case 522:
                    return 230;
                case 523:
                    return 231;
                case 526:
                    return 237;
                case 527:
                    return 238;
                case 530:
                    return 239;
                case 531:
                    return 240;
                case 535:
                    return 241;
                case 538:
                    return 242;
                case 541:
                    return 244;
                case 542:
                    return 243;
                case 543:
                    return 251;
                case 544:
                    return 252;
                case 546:
                    return 254;
                case 547:
                    return 253;
                case 549:
                    return 255;
            }
        }
        return temp.getTypeEffSkill();
    }
}
