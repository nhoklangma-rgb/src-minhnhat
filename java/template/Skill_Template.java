package template;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Skill_Template {
    // skill id buff 10 trung doc, 11 bat tu, 12 crit lien tuc
    // 1 choang, 2 chay mau
    public static List<Skill_Template> ENTRYS;
    public int ID;
    public int indexSkillInServer;
    public short idIcon;
    public byte typeSkill;
    public byte typeBuff;
    public String name;
    public short range;
    private short typeEffSkill;
    public List<Option> op;
    public byte idEffSpec;
    public short perEffSpec;
    public short timeEffSpec;
    public short Lv_RQ;
    public byte nTarget;
    public short rangeLan;
    public int damage;
    public short manaLost;
    public int timeDelay;
    public byte nKick;
    public String info;
    public byte typeDevil;
    public int percentDame = 100;

    public String getInfo(byte level, int clazz) {
        String result = info;
        switch (clazz) {
            case 2: {
                result = result.replace("Quả đấm tốc độ", "Nhất kiếm");
                break;
            }
            case 3: {
                result = result.replace("Quả đấm tốc độ", "Hắc cước");
                break;
            }
            case 4: {
                result = result.replace("Quả đấm tốc độ", "Gậy chong chóng");
                break;
            }
            case 5: {
                result = result.replace("Quả đấm tốc độ", "Double Shot");
                break;
            }
        }
        String percent = "";
        Pattern pattern = Pattern.compile("[\\d]+");
        Matcher matcher = pattern.matcher(info);
        while (matcher.find()) {
            percent = matcher.group();
        }
        if (!percent.isBlank()) {
            int value = Integer.parseInt(percent);
            switch (level) {
                case 1: {
                    value = (value * 11) / 10;
                    break;
                }
                case 2: {
                    value = (value * 125) / 100;
                    break;
                }
                case 3: {
                    value = (value * 145) / 100;
                    break;
                }
                case 4: {
                    value = (value * 17) / 10;
                    break;
                }
                case 5: {
                    value *= 2;
                    break;
                }
                default: {
                    if (level > 5) {
                        // Generic scaling for level > 5: Base * 2 + (Level - 5) * 0.2
                        // This implies 20% increase perc level after level 5 relative to base value?
                        // Or just continue the pattern.
                        // Let's use a simple multiplier: value = value * (2 + (level - 5) * 0.15)
                        // value = (int) (value * (2 + (level - 5) * 0.15));
                        if (level > 5) {
                            if (this.ID <= 2) {
                                value = (int) (value * (1 + Math.sqrt(level - 5) * 0.08));
                            } else {
                                value = (int) (value * (2 + (level - 5) * 0.03));
                            }
                        }
                    }
                    break;
                }
            }
            if (!percent.isBlank()) {
                if (this.ID >= 2003 && this.ID <= 2063 && this.ID != 2059) {
                    value = this.damage; // Use the scaled damage/percentage from DB
                }
                result = result.replace(percent, (value + ""));
                percentDame = value;
            }
        }
        return result;
    }

    public Skill_Template(int Index, int Id, short IdImage, byte type, byte typeBuff, String name, short typeEff,
            short range) {
        this.indexSkillInServer = Index;
        this.ID = (int) Id;
        this.idIcon = IdImage;
        this.typeSkill = type;
        this.typeBuff = typeBuff;
        this.name = name;
        this.range = range;
        this.typeEffSkill = typeEff;
    }

    public void getData(byte nTarget, short rangeLan, int Damage, short Manacost, int CoolDown, byte nkick,
            String Description, short LvCur, byte typeDevil) {
        this.nTarget = nTarget;
        this.rangeLan = rangeLan;
        this.damage = Damage;
        this.manaLost = Manacost;
        this.timeDelay = CoolDown;
        this.nKick = nkick;
        this.info = Description;
        this.Lv_RQ = LvCur;
        this.typeDevil = typeDevil;
    }

    public static Skill_Template get_temp(int index, long exp) {
        for (int i = 0; i < Skill_Template.ENTRYS.size(); i++) {
            Skill_Template temp = Skill_Template.ENTRYS.get(i);
            if (temp.indexSkillInServer == index) {
                if (exp == -1 && temp.Lv_RQ == -1) {
                    return temp;
                } else if (exp > -1 && temp.Lv_RQ > -1) {
                    return temp;
                }
            }
        }
        return null;
    }

    public static boolean upgrade_skill(Skill_info sk_info, byte clazz) {
        Skill_Template result = null;
        int nextLv = sk_info.temp.Lv_RQ + 1;

        // SỬA: Tìm kiếm dựa trên Group ID (ID) và Tên, thay vì indexSkillInServer liền
        // kề
        for (int i = 0; i < Skill_Template.ENTRYS.size(); i++) {
            Skill_Template temp_ss = Skill_Template.ENTRYS.get(i);

            // Logic mới:
            // 1. Cùng tên (để đảm bảo đúng skill)
            // 2. Cùng Group ID (temp_ss.ID == sk_info.temp.ID)
            // 3. Đúng level tiếp theo (Lv_RQ == nextLv)
            if (temp_ss.name.equals(sk_info.temp.name)
                    && temp_ss.ID == sk_info.temp.ID
                    && temp_ss.Lv_RQ == nextLv) {
                result = temp_ss;
                break;
            }
        }

        // Logic cũ cho Haki/Dial (giữ nguyên để tương thích ngược nếu cần)
        if (result == null) {
            String[] skillNames = {
                    "Chế tạo DIAL",
                    "Haki Quan Sát",
                    "Haki Vũ Trang",
                    "Haki Bá Vương"
            };
            for (String groupName : skillNames) {
                if (sk_info.temp.name.equals(groupName)) {
                    for (Skill_Template temp_ss : Skill_Template.ENTRYS) {
                        if (temp_ss.name.equals(groupName)
                                && temp_ss.ID == sk_info.temp.ID
                                && temp_ss.Lv_RQ == nextLv) {
                            result = temp_ss;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (result != null && result.Lv_RQ > 0) {
            // Kiểm tra Max Level (đã được SkillTool inject là 100)
            if (result.Lv_RQ > Skill_info.MAX_LEVEL) {
                return false;
            } else {
                sk_info.temp = result;
                sk_info.realLevel++; // Increment real level on upgrade
                return true;
            }
        }
        return false;
    }

    public static boolean learn_skill(Skill_info sk_info) {
        if (sk_info.temp.Lv_RQ == -1) {
            Skill_Template result = null;
            for (int i = 0; i < Skill_Template.ENTRYS.size(); i++) {
                if (sk_info.temp.indexSkillInServer == Skill_Template.ENTRYS.get(i).indexSkillInServer
                        && sk_info.temp.ID == Skill_Template.ENTRYS.get(i).ID
                        && Skill_Template.ENTRYS.get(i).Lv_RQ == 1) {
                    result = Skill_Template.ENTRYS.get(i);
                    break;
                }
            }
            if (result != null) {
                sk_info.temp = result;
                sk_info.exp = 0;
                sk_info.realLevel = 1; // Learned new skill, level 1
                return true;
            }
        } else {
            Skill_Template result = null;
            for (int i = 0; i < Skill_Template.ENTRYS.size(); i++) {
                if (sk_info.temp.indexSkillInServer == (Skill_Template.ENTRYS.get(i).indexSkillInServer - 1)) {
                    result = Skill_Template.ENTRYS.get(i);
                    break;
                }
            }
            if (result != null) {
                sk_info.temp = result;
                sk_info.exp = 0;
                return true;
            }
        }
        return false;
    }

    public static void reset_skill(Skill_info sk_info) {
        if (sk_info.temp.Lv_RQ == -1) {
            return;
        }
        for (int i = 0; i < Skill_Template.ENTRYS.size(); i++) {
            if (sk_info.temp.ID == Skill_Template.ENTRYS.get(i).ID && Skill_Template.ENTRYS.get(i).Lv_RQ == -1) {
                sk_info.temp = Skill_Template.ENTRYS.get(i);
                sk_info.exp = -1;
                break;
            }
        }
    }

    public short getTypeEffSkill() {
        return typeEffSkill;
    }

    public static Skill_Template getSkillTemplateLv25(Skill_Template base) {
        for (Skill_Template tempLv25 : ENTRYS) {
            if (tempLv25.name.equals(base.name) && tempLv25.ID == base.ID && tempLv25.Lv_RQ == 25) {
                return tempLv25;
            }
        }
        return null;
    }

}
