package client;

import java.util.List;
import template.*;

public class Body {

    private static int level = 600000;
    public static String[] NameAttribute = new String[] { "Sức mạnh", "Phòng thủ", "Thể lực", "Tinh thần",
            "Nhanh nhẹn" };
    // public static byte[][] Id = new byte[][] { //
    // new byte[] { 1, 13, 10 }, // 1
    // new byte[] { 4, 26, 27 }, // 2
    // new byte[] { 15, 23 }, // 3
    // new byte[] { 16, 11, 14 }, // 4
    // new byte[] { 25, 12 } }; // 5
    // Phân bổ ID hợp lý theo đúng bản chất từng chỉ số:
    public static byte[][] Id = new byte[][] {
            // new byte[] { 1, 10, 13 }, // Sức mạnh: ATK%, Crit%, Pierce%
            // new byte[] { 4, 3, 26, 27, 50, 14 }, // Phòng thủ: DEF%, DEF flat, Kháng vật
            // lý, Kháng phép, Giảm xuyên
            // // giáp, % Phản đòn
            // new byte[] { 15, 17, 19, 21, 23 }, // Thể lực: HP flat, %HP, Hồi HP, Hút máu,
            // Thuốc HP
            // new byte[] { 16, 18, 20, 22, 24, 11 }, // Tinh thần: MP flat, %MP, Hồi MP,
            // Hút MP, Thuốc MP, Dame Crit%
            // new byte[] { 25, 12, 67, 72 } // Nhanh nhẹn: cooldown, Né tránh, EXP%, Beri%
            new byte[] { 1, 10, 13 }, // SỨC MẠNH: ATK%, Crit, Pierce
            new byte[] { 4, 3, 26, 27 }, // PHÒNG THỦ: DEF%, DEF flat, Kháng vật lý, Kháng phép (Bỏ Counter, Reduce
                                         // Pierce)
            new byte[] { 15, 17, 19 }, // THỂ LỰC: HP flat, %HP, Hồi HP (Bỏ Lifesteal, Potion)
            new byte[] { 16, 18, 20 }, // TINH THẦN: MP flat, %MP, Hồi MP (Bỏ Steal, Potion, Crit Dame)
            new byte[] { 25, 12 } // NHANH NHẸN: cooldown, Né tránh (Bỏ EXP, Beri)
    };

    // ===================== POINT 1 - SỨC MẠNH =====================
    public static long[] Point1_Template_atk; // ID 1 - ATK%
    public static long[] Point1_Template_crit; // ID 10 - Crit%
    public static long[] Point1_Template_pierce; // ID 13 - Pierce%

    // ===================== POINT 2 - PHÒNG THỦ =====================
    public static long[] Point2_Template_def_percent; // ID 4 - DEF%
    public static long[] Point2_Template_def; // ID 3 - DEF flat
    public static long[] Point2_Template_resist_physical; // ID 26 - Kháng vật lý
    public static long[] Point2_Template_resist_magic; // ID 27 - Kháng phép
    public static long[] Point2_Template_reduce_pierce; // ID 50 - Giảm xuyên giáp đối thủ
    public static long[] Point2_Template_counter_attack; // ID 14 - % Phản đòn
    // ===================== POINT 3 - THỂ LỰC =====================
    public static long[] Point3_Template_hp; // ID 15 - HP flat
    public static long[] Point3_Template_hp_percent; // ID 17 - %HP
    public static long[] Point3_Template_hp_regen; // ID 19 - Tự hồi HP
    public static long[] Point3_Template_lifesteal; // ID 21 - Hút máu
    public static long[] Point3_Template_hp_potion; // ID 23 - Thuốc HP%

    // ===================== POINT 4 - TINH THẦN =====================
    public static long[] Point4_Template_mp; // ID 16 - MP flat
    public static long[] Point4_Template_mp_percent; // ID 18 - %MP
    public static long[] Point4_Template_mp_regen; // ID 20 - Tự hồi MP
    public static long[] Point4_Template_mp_steal; // ID 22 - Hút MP
    public static long[] Point4_Template_mp_potion; // ID 24 - Thuốc MP%
    public static long[] Point4_Template_dame_crit; // ID 11 - Dame Crit%

    // ===================== POINT 5 - NHANH NHẸN =====================
    public static long[] Point5_Template_miss; // ID 12 - Né tránh%
    public static long[] Point5_Template_cooldown; // ID 25 - Hồi chiêu
    public static long[] Point5_Template_exp_bonus; // ID 67 - EXP% thêm
    public static long[] Point5_Template_beri_bonus; // ID 72 - Beri% thêm

    static {
        load_point_1();
        load_point_2();
        load_point_3();
        load_point_4();
        load_point_5();
    }
    private final Player p;
    private final long[] aggregated_stats = new long[200];
    private boolean stats_dirty = true;

    public Body(Player p) {
        this.p = p;
    }

    public void setDirty() {
        this.stats_dirty = true;
        // Also invalidate Player-level primitive caches
        p.hp_max_dirty = true;
        p.mp_max_dirty = true;
    }

    public synchronized void refreshStats() {
        if (!stats_dirty)
            return;

        java.util.Arrays.fill(aggregated_stats, 0);

        // 1. Base potential points bonuses
        for (int i = 0; i < 200; i++) {
            aggregated_stats[i] += get_extra_bonus_from_points(i);
        }

        // 2. Equipment (it_body)
        aggregateGears(p.item.it_body);

        // 3. Second Equipment (second_body)
        aggregateGears(p.item.second_body);

        // 4. Third Equipment (third_body)
        aggregateGears(p.item.third_body);

        // 4. Fashion
        for (int i = 0; i < p.fashion.size(); i++) {
            ItemFashionP2 temp = p.fashion.get(i);
            if (temp != null && temp.is_use) {
                ItemFashion tempF = ItemFashion.get_item(temp.id);
                if (tempF != null && tempF.op != null) {
                    for (int j = 0; j < tempF.op.size(); j++) {
                        addOpToAggregated(tempF.op.get(j).id, tempF.op.get(j).getParam(), temp.level);
                    }
                }
                if (temp.op != null) {
                    for (int j = 0; j < temp.op.size(); j++) {
                        addOpToAggregated(temp.op.get(j).id, temp.op.get(j).getParam(), temp.level);
                    }
                }
            }
        }

        // 5. Skills (Passive/Buff)
        for (int i = 0; i < p.skill_point.size(); i++) {
            Skill_info temp = p.skill_point.get(i);
            if (temp != null && temp.temp != null && temp.temp.Lv_RQ > 0
                    && (temp.temp.typeSkill == 3 || temp.temp.typeSkill == 6)) {
                if (temp.temp.op != null) {
                    for (int j = 0; j < temp.temp.op.size(); j++) {
                        int id = temp.temp.op.get(j).id;
                        if (id >= 0 && id < 200 && id != 25) {
                            aggregated_stats[id] += temp.temp.op.get(j).getParam();
                        }
                    }
                }
            }
        }

        // 6. Party Buffs
        if (p.party != null) {
            List<Option> op_select = p.party.get_list_buff_now(p);
            if (op_select != null) {
                for (int i = 0; i < op_select.size(); i++) {
                    int id = op_select.get(i).id;
                    if (id >= 0 && id < 200) {
                        aggregated_stats[id] += op_select.get(i).getParam();
                    }
                }
            }
        }

        // 7. Wish Stats
        if (p.wishStats != null) {
            for (int i = 0; i < 200; i++) {
                aggregated_stats[i] += p.wishStats.getBonus(i);
            }
        }

        // 8. Danh hiệu Stats
        if (p.danh_hieu != null) {
            for (int i = 0; i < p.danh_hieu.size(); i++) {
                MyDanhHieu myDH = p.danh_hieu.get(i);
                if (myDH != null && myDH.isUsed) {
                    DanhHieu tempDH = DanhHieu.getTemplate(myDH.id);
                    if (tempDH != null && tempDH.options != null) {
                        for (Option op : tempDH.options) {
                            if (op.id >= 0 && op.id < 200) {
                                aggregated_stats[op.id] += op.getParam();
                            }
                        }
                    }
                    break; // Chỉ dùng tối đa 1 danh hiệu tại một thời điểm
                }
            }
        }

        // 9. Fusion Bonus (Dynamic % of Disciple's total stats and base points)
        if (p.fusionType != 0 && p.myDisciple != null) {
            Body dBody = p.myDisciple.body;
            // Ensure disciple stats are calculated and fresh
            dBody.refreshStats();

            // Tỉ lệ kế thừa: ID 1 (Fusion 30p) = 20%, ID 2 (Fusion No Limit) = 30%
            int multiplier = (p.fusionType == 2) ? 3 : 2;

            for (int i = 0; i < 200; i++) {
                // Kế thừa chỉ số từ Option/Trang bị của đệ tử
                this.aggregated_stats[i] += dBody.aggregated_stats[i] * multiplier / 10;
            }
            // Kế thừa Điểm Tiềm năng cơ bản của Đệ tử
            this.aggregated_stats[5] += dBody.p.point1 * multiplier / 10;
            this.aggregated_stats[6] += dBody.p.point2 * multiplier / 10;
            this.aggregated_stats[7] += dBody.p.point3 * multiplier / 10;
            this.aggregated_stats[8] += dBody.p.point4 * multiplier / 10;
            this.aggregated_stats[9] += dBody.p.point5 * multiplier / 10;
        }

        stats_dirty = false;
        // Invalidate specific Player caches
        p.hp_max_dirty = true;
        p.mp_max_dirty = true;
    }

    private void aggregateGears(Item_wear[] gears) {
        if (gears == null)
            return;
        for (Item_wear gear : gears) {
            if (gear == null)
                continue;

            // Options 1
            if (gear.option_item != null) {
                for (Option op : gear.option_item) {
                    if (op.id >= 0 && op.id < 200 && gear.template != null) {
                        aggregated_stats[op.id] += op.getParam(gear.template.typeEquip, gear.levelup, gear.isHoanMy);
                    }
                }
            }
            // Options 2
            if (gear.option_item_2 != null) {
                for (Option op : gear.option_item_2) {
                    if (op.id >= 0 && op.id < 200) {
                        aggregated_stats[op.id] += op.getParam();
                    }
                }
            }
            // Devil Fruit Synergy - Optimized by aggregating all synergy options once
            if (gear.mdakham != null && gear.mdakham.length > 0) {
                List<Option> synOpts = devilfruit.DevilFruitManager.getSynergyOptions(gear.mdakham, gear.combo_pattern);
                if (synOpts != null) {
                    for (Option syn : synOpts) {
                        if (syn.id >= 0 && syn.id < 200) {
                            aggregated_stats[syn.id] += syn.getParam();
                        }
                    }
                }
            }
        }
    }

    private void addOpToAggregated(int id, int value, int fashionLevel) {
        if (id < 0 || id >= 200)
            return;
        int op_value = value;
        if (ItemOptionTemplate.ENTRYS.get(id).percent == 1) {
            op_value += fashionLevel * 10;
        }
        aggregated_stats[id] += op_value;
    }

    private long total_param_item(int id, boolean have_eff) {
        return total_param_item(id, have_eff, null);
    }

    private long total_param_item(int id, boolean have_eff, long[] p_reduction) {
        refreshStats();
        long par = (id >= 0 && id < 200) ? aggregated_stats[id] : 0;

        // Nếu có giảm tiềm năng, ta trừ đi phần bonus từ tiềm năng bị giảm trong cache
        if (p_reduction != null) {
            par -= get_reduction_bonus(id, p_reduction);
        }

        // Maintain original hardcoded index-based level bonuses
        if (id == 46) {
            if (p.item.it_body[0] != null && p.item.it_body[0].levelup > 10)
                par += (50 * (p.item.it_body[0].levelup - 10));
            if (p.item.second_body[0] != null && p.item.second_body[0].levelup > 10)
                par += (50 * (p.item.second_body[0].levelup - 10));
            if (p.item.third_body[0] != null && p.item.third_body[0].levelup > 10)
                par += (50 * (p.item.third_body[0].levelup - 10));
        } else if (id == 53) {
            if (p.item.it_body[2] != null && p.item.it_body[2].levelup > 10)
                par += (30 * (p.item.it_body[2].levelup - 10));
            if (p.item.second_body[2] != null && p.item.second_body[2].levelup > 10)
                par += (30 * (p.item.second_body[2].levelup - 10));
            if (p.item.third_body[2] != null && p.item.third_body[2].levelup > 10)
                par += (30 * (p.item.third_body[2].levelup - 10));
        } else if (id == 56) {
            for (int k : new int[] { 1, 3, 5 }) {
                if (p.item.it_body[k] != null && p.item.it_body[k].levelup > 10)
                    par += (100 * (p.item.it_body[k].levelup - 10));
                if (p.item.second_body[k] != null && p.item.second_body[k].levelup > 10)
                    par += (100 * (p.item.second_body[k].levelup - 10));
                if (p.item.third_body[k] != null && p.item.third_body[k].levelup > 10)
                    par += (100 * (p.item.third_body[k].levelup - 10));
            }
        } else if (id == 47) {
            if (p.item.it_body[4] != null && p.item.it_body[4].levelup > 10)
                par += (20 * (p.item.it_body[4].levelup - 10));
            if (p.item.second_body[4] != null && p.item.second_body[4].levelup > 10)
                par += (20 * (p.item.second_body[4].levelup - 10));
            if (p.item.third_body[4] != null && p.item.third_body[4].levelup > 10)
                par += (20 * (p.item.third_body[4].levelup - 10));
        }

        if (have_eff) {
            EffTemplate temp = p.get_eff(id + 100);
            if (temp != null) {
                par += temp.param;
            }
        }

        EffTemplate effZombie = p.get_eff(21);
        if (par > 1 && effZombie != null && effZombie.param == id) {
            par /= 2;
        }

        return par;
    }

    private long get_reduction_bonus(int stat_id, long[] reductions) {
        long red = 0;
        red += calcExtraBonus(Skill_info.POINT1_EXTRA, (short) reductions[1], stat_id);
        red += calcExtraBonus(Skill_info.POINT2_EXTRA, (short) reductions[2], stat_id);
        red += calcExtraBonus(Skill_info.POINT3_EXTRA, (short) reductions[3], stat_id);
        red += calcExtraBonus(Skill_info.POINT4_EXTRA, (short) reductions[4], stat_id);
        red += calcExtraBonus(Skill_info.POINT5_EXTRA, (short) reductions[5], stat_id);
        return red;
    }

    private long get_extra_bonus_from_points(int stat_id) {
        long bonus = 0;
        bonus += calcExtraBonus(Skill_info.POINT1_EXTRA, p.point1, stat_id);
        bonus += calcExtraBonus(Skill_info.POINT2_EXTRA, p.point2, stat_id);
        bonus += calcExtraBonus(Skill_info.POINT3_EXTRA, p.point3, stat_id);
        bonus += calcExtraBonus(Skill_info.POINT4_EXTRA, p.point4, stat_id);
        bonus += calcExtraBonus(Skill_info.POINT5_EXTRA, p.point5, stat_id);
        return bonus;
    }

    private long calcExtraBonus(float[][] config, short points, int stat_id) {
        for (float[] entry : config) {
            if (entry.length >= 2 && (int) entry[0] == stat_id) {
                return (long) (points * entry[1]);
            }
        }
        return 0;
    }

    public long get_total_point(int type) {
        return get_total_point(type, 0);
    }

    public long get_total_point(int type, long reduction) {
        long param = 0;
        long total_reduction = reduction + p.total_badge_reductions[type];

        switch (type) {
            case 1: {
                param += (long) p.point1 + get_point_plus(1);
                break;
            }
            case 2: {
                param += (long) p.point2 + get_point_plus(2);
                break;
            }
            case 3: {
                param += (long) p.point3 + get_point_plus(3);
                break;
            }
            case 4: {
                param += (long) p.point4 + get_point_plus(4);
                break;
            }
            case 5: {
                param += (long) p.point5 + get_point_plus(5);
                break;
            }
        }
        param -= total_reduction;
        if (type != 3 && param > 10 && p.get_eff(21) != null) {
            param = (param * 8L) / 10L;
        }
        if (param > level) {
            param = level;
        }
        if (param < 1) {
            param = 1;
        }
        return param;
    }

    public long get_dame(boolean have_eff) {
        Skill_info sk_temp = p.get_skill_temp(0);
        if (sk_temp == null) {
            return 0;
        }
        long dame = sk_temp.temp.damage;
        long percent = get_dame_percent(have_eff);
        // [SAFETY] Sử dụng double để tính toán % Dame, tránh tràn số tích (dame *
        // percent)
        dame = (long) ((double) dame * percent / 1000.0);
        dame += sk_temp.get_dame(p);

        // [SAFETY] Sát thương cuối (Option 46) thường rất lớn, dùng double để bảo vệ
        long final_dame_percent = get_percent_final_dame(have_eff);
        dame = (long) ((double) dame * (100000.0 + final_dame_percent) / 100000.0);
        return core.BalanceGateway.applyStatLimit(this.p, 0, dame); // ID 0 cho Sat thuong tong
    }

    public long get_dame_percent(boolean have_eff) {
        return get_dame_percent(have_eff, null);
    }

    public long get_dame_percent(boolean have_eff, long[] p_reduction) {
        long par = total_param_item(1, have_eff, p_reduction);
        par += Body.Point1_Template_atk[(int) get_total_point(1, p_reduction != null ? p_reduction[1] : 0) - 1];
        return core.BalanceGateway.applyStatLimit(this.p, 1, par); // % Sat thuong
    }

    public long get_def(boolean have_eff) {
        return get_def(have_eff, null);
    }

    public long get_def(boolean have_eff, long[] p_reduction) {
        long t_point = this.get_total_point(2, p_reduction != null ? p_reduction[2] : 0);
        long def = t_point;
        // [GATE] Áp rate/cap cho DEF flat từ trang bị (ID 3) trước khi cộng vào tổng
        long def_flat_item = core.BalanceGateway.applyStatLimit(this.p, 3, total_param_item(3, have_eff, p_reduction));
        def += def_flat_item;
        def += Body.Point2_Template_def[(int) t_point - 1]; // ✅ Thêm DEF flat từ Phòng thủ (điểm tiềm năng, không Gate
                                                            // riêng)
        return core.BalanceGateway.applyStatLimit(this.p, 3, def); // Phong thu vat ly — Gate tổng cuối
    }

    public long get_def_percent(boolean have_eff) {
        return get_def_percent(have_eff, null);
    }

    public long get_def_percent(boolean have_eff, long[] p_reduction) {
        long par = total_param_item(4, have_eff, p_reduction);
        par += Body.Point2_Template_def_percent[(int) this.get_total_point(2, p_reduction != null ? p_reduction[2] : 0)
                - 1]; // ✅ DEF% từ Phòng thủ
        return core.BalanceGateway.applyStatLimit(this.p, 4, par); // % Phong thu
    }

    public long get_hp_max(boolean have_eff) {
        return get_hp_max(have_eff, null);
    }

    public long get_hp_max(boolean have_eff, long[] p_reduction) {
        if (have_eff && !p.hp_max_dirty && p_reduction == null) {
            return p.hp_max_cache;
        }

        long t_point = this.get_total_point(3, p_reduction != null ? p_reduction[3] : 0);
        long hp = Body.Point3_Template_hp[(int) t_point - 1];
        hp += total_param_item(15, have_eff, p_reduction);
        long percent = total_param_item(17, have_eff, p_reduction);
        percent += Body.Point3_Template_hp_percent[(int) t_point - 1]; // ✅ %HP từ Thể lực
        EffTemplate eff = p.get_eff(4);
        if (eff != null) {
            percent += eff.param;
        }
        // [GATE] Áp rate/cap cho %HP trước khi dùng làm nhân tử — đảm bảo config
        // rate-17 có hiệu lực thực chiến
        percent = core.BalanceGateway.applyStatLimit(this.p, 17, percent);
        // [SAFETY] HP Max có thể lên tới hàng triệu tỷ, nhân % bằng double
        hp = (long) ((double) hp * (1000.0 + percent) / 1000.0);
        // [GATE] Áp rate/cap cho %HP cuối (ID 56) trước khi nhân — đảm bảo config
        // rate-56 có hiệu lực thực chiến
        long hp_final_percent = core.BalanceGateway.applyStatLimit(this.p, 56, total_param_item(56, true, p_reduction));
        hp = (long) ((double) hp * (1000.0 + hp_final_percent) / 1000.0); // % hp cuoi
        long result = Math.max(1, (long) ((double) hp * 200.0));

        if (have_eff && p_reduction == null) {
            p.hp_max_cache = result;
            p.hp_max_dirty = false;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 15, result); // ID 15 cho HP Tong
    }

    public long get_mp_max(boolean have_eff) {
        if (have_eff && !p.mp_max_dirty) {
            return p.mp_max_cache;
        }

        long mp = Body.Point4_Template_mp[(int) this.get_total_point(4) - 1];
        mp += total_param_item(16, have_eff);
        long mp_percent = total_param_item(18, have_eff);
        mp_percent += Body.Point4_Template_mp_percent[(int) this.get_total_point(4) - 1]; // ✅ %MP từ Tinh thần
        // [GATE] Áp rate/cap cho %MP trước khi dùng làm nhân tử — đảm bảo config
        // rate-18 có hiệu lực thực chiến
        mp_percent = core.BalanceGateway.applyStatLimit(this.p, 18, mp_percent);
        // [SAFETY] MP Max dùng double trung gian
        mp = (long) ((double) mp * (1000.0 + mp_percent) / 1000.0);
        long result = Math.max(1, mp);

        if (have_eff) {
            p.mp_max_cache = result;
            p.mp_max_dirty = false;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 16, result); // ID 16 cho MP Tong
    }

    public long get_agility(boolean have_eff) {
        return get_agility(have_eff, null);
    }

    public long get_agility(boolean have_eff, long[] p_reduction) {
        long par = total_param_item(25, have_eff, p_reduction);
        long t_point = this.get_total_point(5, p_reduction != null ? p_reduction[5] : 0);
        if (t_point > 0) {
            par += Body.Point5_Template_cooldown[(int) t_point - 1];
        }
        if (p.get_eff(13) != null) {
            par = (par * 15) / 10;
        }
        if (have_eff && p.party != null) {
            List<Option> op_select = p.party.get_list_buff_now(p);
            for (int i = 0; i < op_select.size(); i++) {
                if (op_select.get(i).id == 25) {
                    par += op_select.get(i).getParam();
                    break;
                }
            }
        }
        if (par > 450) {
            par = 450;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 25, par); // Giam cooldown
    }

    public long view_in4(int b) {
        switch (b) {
            case 0:
                return get_dame(true);
            case 1:
                return get_dame_percent(false);
            case 2:
                return get_dame_ap();
            case 3:
                return get_def(true);
            case 4:
                return get_def_percent(false);
            case 15:
                // [SYNC] Gọi get_hp_max để UI luôn khớp với combat — tránh hiển thị số khác
                // thực tế
                return get_hp_max(false);
            case 16:
                // [SYNC] Gọi get_mp_max để UI luôn khớp với combat — tránh hiển thị số khác
                // thực tế
                return get_mp_max(false);
            case 25:
                return get_agility(false);
            case 26:
                return get_dame_resist(false);
            case 27:
                return get_dame_resist_ap(false);
            case 17:
                return core.BalanceGateway.applyStatLimit(this.p, 17, total_param_item(17, false)
                        + Body.Point3_Template_hp_percent[(int) this.get_total_point(3) - 1]); // ✅ Hiện tổng %HP
            case 18:
                return core.BalanceGateway.applyStatLimit(this.p, 18, total_param_item(18, false)
                        + Body.Point4_Template_mp_percent[(int) this.get_total_point(4) - 1]); // ✅ Hiện tổng %MP
            case 13:
                return core.Util.getStatDiminishing(get_pierce(false));
            case 10:
                return get_crit(false);
            case 12:
                return core.BalanceGateway.applyStatLimit(this.p, 12, core.Util.getStatDiminishing(get_miss(false)));
            case 14:
                return core.BalanceGateway.applyStatLimit(this.p, 14,
                        core.Util.getStatDiminishing(get_dame_react(false)));
            case 23:
                return core.BalanceGateway.applyStatLimit(this.p, 23, get_hp_potion_use_percent(false));
            case 24:
                return core.BalanceGateway.applyStatLimit(this.p, 24, get_mp_potion_use_percent(false));
            case 19:
                return get_hp_auto_buff(false);
            case 20:
                return get_mp_auto_buff(false);
            case 21:
                return get_hp_atk_absorb(false);
            case 22:
                return get_mp_atk_absorb(false);
            case 11:
                return get_multi_dame_when_crit(false);
            case 53:
                return core.Util.getStatDiminishing(get_dame_skip(false));
            case 63:
                return get_dame_skip_reduce();
            case 49:
                return get_crit_reduce();
            case 50:
                return get_pierce_reduce();
            case 51:
                return get_miss_reduce();
            case 52:
                return get_dame_react_reduce();
            case 55:
                return get_TuChoiTuThan();
            case 57:
                return get_true_dame();
            case 58:
            case 59:
                return get_HapThu_Hp();
            case 46:
                return get_percent_final_dame();
            case 67:
                return get_xp_more();
            case 68:
                return get_xp_skill_more();
            case 69:
                return get_multi_dame_decrease();
            case 72:
                return get_percent_beri_train();
            case 91:
                return get_giam_point1_doi_thu();
            case 92:
                return get_giam_point2_doi_thu();
            case 93:
                return get_giam_point3_doi_thu();
            case 94:
                return get_giam_point4_doi_thu();
            case 95:
                return get_giam_point5_doi_thu();
        }
        return core.BalanceGateway.applyStatLimit(this.p, b, total_param_item(b, false));
    }

    public long get_param_by_id(int id) {
        long par = total_param_item(id, true);
        return core.BalanceGateway.applyStatLimit(this.p, id, par);
    }

    public long get_multi_dame_decrease() {
        long par = total_param_item(69, false);
        return core.BalanceGateway.applyStatLimit(this.p, 69, par);
    }

    public long get_dame_skip_reduce() {
        long par = total_param_item(63, true);
        return core.BalanceGateway.applyStatLimit(this.p, 63, par);
    }

    public long get_TuChoiTuThan() {
        long result = total_param_item(55, true);
        int index_full_set = p.get_index_full_set();
        if (index_full_set > 3) {
            result += 100;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 55, result);
    }

    public long get_mp_atk_absorb(boolean have_eff) {
        long par = total_param_item(22, have_eff);
        par += Body.Point4_Template_mp_steal[(int) this.get_total_point(4) - 1]; // ✅ thêm hút MP từ Tinh thần
        return core.BalanceGateway.applyStatLimit(this.p, 22, par); // Hut MP
    }

    public long get_hp_atk_absorb(boolean have_eff) {
        long par = total_param_item(21, have_eff);
        par += Body.Point3_Template_lifesteal[(int) this.get_total_point(3) - 1]; // ✅ thêm hút máu từ Thể lực
        return core.BalanceGateway.applyStatLimit(this.p, 21, par); // Hut HP
    }

    public long get_dame_react(boolean have_eff) {
        if (this.p.map == null || this.p.map.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
            return -BossEvent.BossEvent.REACT_REDUCE;
        }
        long dame_react = total_param_item(14, have_eff);
        dame_react += Body.Point2_Template_counter_attack[(int) this.get_total_point(2) - 1]; // ✅ thêm phản đòn từ
                                                                                              // Phòng thủ
        return core.BalanceGateway.applyStatLimit(this.p, 14, dame_react); // Phan don
    }

    public long get_giam_point1_doi_thu() {
        long par = total_param_item(91, false);
        return core.BalanceGateway.applyStatLimit(this.p, 91, par);
    }

    public long get_giam_point2_doi_thu() {
        long par = total_param_item(92, false);
        return core.BalanceGateway.applyStatLimit(this.p, 92, par);
    }

    public long get_giam_point3_doi_thu() {
        long par = total_param_item(93, false);
        return core.BalanceGateway.applyStatLimit(this.p, 93, par);
    }

    public long get_giam_point4_doi_thu() {
        long par = total_param_item(94, false);
        return core.BalanceGateway.applyStatLimit(this.p, 94, par);
    }

    public long get_giam_point5_doi_thu() {
        long par = total_param_item(95, false);
        return core.BalanceGateway.applyStatLimit(this.p, 95, par);
    }

    public long[] get_reductions() {
        long[] red = new long[6];
        red[1] = get_giam_point1_doi_thu();
        red[2] = get_giam_point2_doi_thu();
        red[3] = get_giam_point3_doi_thu();
        red[4] = get_giam_point4_doi_thu();
        red[5] = get_giam_point5_doi_thu();
        return red;
    }

    public long get_pierce(boolean have_eff) {
        return get_pierce(have_eff, null);
    }

    public long get_pierce(boolean have_eff, long[] p_reduction) {
        long par = total_param_item(13, have_eff, p_reduction);
        par += Body.Point1_Template_pierce[(int) this.get_total_point(1, p_reduction != null ? p_reduction[1] : 0) - 1];
        return core.BalanceGateway.applyStatLimit(this.p, 13, par); // Xuyen giap
    }

    public long get_miss(boolean have_eff) {
        return get_miss(have_eff, null);
    }

    public long get_miss(boolean have_eff, long[] p_reduction) {
        long miss = total_param_item(12, have_eff, p_reduction);
        long t_point = this.get_total_point(5, p_reduction != null ? p_reduction[5] : 0);
        if (t_point > 0) {
            miss += Body.Point5_Template_miss[(int) t_point - 1];
        }
        return core.BalanceGateway.applyStatLimit(this.p, 12, miss); // Ne tranh
    }

    public long get_crit(boolean have_eff) {
        return get_crit(have_eff, null);
    }

    public long get_crit(boolean have_eff, long[] p_reduction) {
        long par = total_param_item(10, have_eff, p_reduction);
        par += Body.Point1_Template_crit[(int) this.get_total_point(1, p_reduction != null ? p_reduction[1] : 0) - 1];
        return core.BalanceGateway.applyStatLimit(this.p, 10, par); // Chi mang
    }

    public long get_multi_dame_when_crit(boolean have_eff) {
        long par = total_param_item(11, have_eff);
        par += Body.Point4_Template_dame_crit[(int) this.get_total_point(4) - 1];
        return core.BalanceGateway.applyStatLimit(this.p, 11, par); // Sat thuong Chi mang
    }

    public long get_dame_resist(boolean have_eff) {
        long par = total_param_item(26, have_eff);
        par += Body.Point2_Template_resist_physical[(int) this.get_total_point(2) - 1];
        if (p.get_eff(11) != null) { // skill buff luffy
            par *= 2;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 26, par); // Khang vat ly
    }

    public long get_dame_resist_ap(boolean have_eff) {
        long par = total_param_item(27, have_eff);
        par += Body.Point2_Template_resist_magic[(int) this.get_total_point(2) - 1];
        if (p.get_eff(11) != null) { // skill buff luffy
            par *= 2;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 27, par); // Khang phep
    }

    public long get_hp_potion_use_percent(boolean have_eff) {
        long par = total_param_item(23, have_eff);
        par += Body.Point3_Template_hp_potion[(int) this.get_total_point(3) - 1];
        return core.BalanceGateway.applyStatLimit(this.p, 23, par); // Dung binh HP
    }

    public long get_point_plus(int type) {
        long par = 0L;
        switch (type) {
            case 1: {
                par += (long) total_param_item(5, true);
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    if (p.list_op_thongthao.get(i).id == 5) {
                        par += (long) p.list_op_thongthao.get(i).getParam();
                    }
                }
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    if (p.list_op_thongthao2.get(i).id == 5) {
                        par += (long) p.list_op_thongthao2.get(i).getParam();
                    }
                }
                par = core.BalanceGateway.applyStatLimit(this.p, 5, par); // Gate Thể lực cộng thêm
                break;
            }
            case 2: {
                par += (long) total_param_item(6, true);
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    if (p.list_op_thongthao.get(i).id == 6) {
                        par += (long) p.list_op_thongthao.get(i).getParam();
                    }
                }
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    if (p.list_op_thongthao2.get(i).id == 6) {
                        par += (long) p.list_op_thongthao2.get(i).getParam();
                    }
                }
                par = core.BalanceGateway.applyStatLimit(this.p, 6, par); // Gate Tinh thần cộng thêm
                break;
            }
            case 3: {
                par += (long) total_param_item(7, true);
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    if (p.list_op_thongthao.get(i).id == 7) {
                        par += (long) p.list_op_thongthao.get(i).getParam();
                    }
                }
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    if (p.list_op_thongthao2.get(i).id == 7) {
                        par += (long) p.list_op_thongthao2.get(i).getParam();
                    }
                }
                par = core.BalanceGateway.applyStatLimit(this.p, 7, par); // Gate Sức mạnh cộng thêm
                break;
            }
            case 4: {
                par += (long) total_param_item(8, true);
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    if (p.list_op_thongthao.get(i).id == 8) {
                        par += (long) p.list_op_thongthao.get(i).getParam();
                    }
                }
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    if (p.list_op_thongthao2.get(i).id == 8) {
                        par += (long) p.list_op_thongthao2.get(i).getParam();
                    }
                }
                par = core.BalanceGateway.applyStatLimit(this.p, 8, par); // Gate Phòng thủ cộng thêm
                break;
            }
            case 5: {
                par += (long) total_param_item(9, true);
                for (int i = 0; i < p.list_op_thongthao.size(); i++) {
                    if (p.list_op_thongthao.get(i).id == 9) {
                        par += (long) p.list_op_thongthao.get(i).getParam();
                    }
                }
                for (int i = 0; i < p.list_op_thongthao2.size(); i++) {
                    if (p.list_op_thongthao2.get(i).id == 9) {
                        par += (long) p.list_op_thongthao2.get(i).getParam();
                    }
                }
                par = core.BalanceGateway.applyStatLimit(this.p, 9, par); // Gate Thân pháp cộng thêm
                break;
            }
        }
        if (p.clan != null) {
            par += (long) p.clan.opAttri[type - 1] + (long) Clan.get_point_trungsinh_plus(p.clan);
        }
        int bonusPercent = p.getFullSetBonusPercent();
        if (bonusPercent > 0) {
            par += par * bonusPercent / 100;
        }
        return Math.max(0, Math.min(par, 999_999));
    }

    public long get_dame_skip(boolean have_eff) {
        if (this.p.map == null || this.p.map.template.id == BossEvent.BossEvent.BOSS_EVENT_MAP_ID) {
            return 0;
        }
        long par = total_param_item(53, have_eff);
        return core.BalanceGateway.applyStatLimit(this.p, 53, par); // Ne dame
    }

    public long get_mp_potion_use_percent(boolean have_eff) {
        long par = total_param_item(24, have_eff);
        par += Body.Point4_Template_mp_potion[(int) this.get_total_point(4) - 1]; // ✅ thêm thuốc MP từ Tinh thần
        return core.BalanceGateway.applyStatLimit(this.p, 24, par); // Dung binh MP
    }

    public long get_hp_auto_buff(boolean have_eff) {
        long par = total_param_item(19, have_eff);
        par += Body.Point3_Template_hp_regen[(int) this.get_total_point(3) - 1]; // ✅ thêm hồi HP từ Thể lực
        return core.BalanceGateway.applyStatLimit(this.p, 19, par); // Tu dong hoi HP
    }

    public long get_mp_auto_buff(boolean have_eff) {
        long par = total_param_item(20, have_eff);
        par += Body.Point4_Template_mp_regen[(int) this.get_total_point(4) - 1]; // ✅ thêm hồi MP từ Tinh thần
        return core.BalanceGateway.applyStatLimit(this.p, 20, par); // Tu dong hoi MP
    }

    public long get_dame_ap() {
        return core.BalanceGateway.applyStatLimit(this.p, 2, total_param_item(2, true)); // Dame AP
    }

    public long get_dame_percent_hp_target() {
        long result = total_param_item(48, true);
        if (result >= 350) {
            result = 350;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 48, result);
    }

    public int get_kich_an(int id) {
        int result = 0;
        for (int i = 0; i < p.item.it_body.length; i++) {
            if (p.item.it_body[i] != null && p.item.it_body[i].valueKichAn == id) {
                result++;
            }
        }
        return result;
    }

    public long get_percent_beri_train() {
        long par = total_param_item(72, true);
        par += Body.Point5_Template_beri_bonus[(int) this.get_total_point(5) - 1]; // ✅ thêm Beri% từ Nhanh nhẹn
        for (int i = 0; i < p.fashion.size(); i++) {
            if (p.fashion.get(i).id == 77 && p.fashion.get(i).is_use) {
                par += 300;
                break;
            }
        }
        return core.BalanceGateway.applyStatLimit(this.p, 72, par);
    }


    public long get_crit_reduce() {
        long par = total_param_item(49, true);
        int index_full_set = p.get_index_full_set();
        if (index_full_set == 5) {
            par += (80 * 1);
        } else if (index_full_set > 2) {
            par += (50 * 1);
        }
        return core.BalanceGateway.applyStatLimit(this.p, 49, par);
    }

    public long get_dame_react_reduce() {
        long par = total_param_item(52, true);
        int index_full_set = p.get_index_full_set();
        if (index_full_set == 5) {
            par += (80 * 1);
        } else if (index_full_set > 1) {
            par += (50 * 1);
        }
        return core.BalanceGateway.applyStatLimit(this.p, 52, par);
    }

    public long get_pierce_reduce() {
        return get_pierce_reduce(null);
    }

    public long get_pierce_reduce(long[] p_reduction) {
        long par = total_param_item(50, true, p_reduction);
        par += Body.Point2_Template_reduce_pierce[(int) this.get_total_point(2,
                p_reduction != null ? p_reduction[2] : 0)
                - 1]; // ✅ thêm giảm xuyên giáp từ Phòng
                      // thủ
        int index_full_set = p.get_index_full_set();
        if (index_full_set == 5) {
            par += (80 * 1);
        } else if (index_full_set > 2) {
            par += (50 * 1);
        }
        return core.BalanceGateway.applyStatLimit(this.p, 50, par);
    }

    public long get_miss_reduce() {
        long par = total_param_item(51, true);
        int index_full_set = p.get_index_full_set();
        if (index_full_set == 5) {
            par += (80 * 1);
        } else if (index_full_set > 0) {
            par += (50 * 1);
        }
        return core.BalanceGateway.applyStatLimit(this.p, 51, par);
    }

    public long get_true_dame() {
        long par = total_param_item(57, true);
        int index_full_set = p.get_index_full_set();
        if (index_full_set == 5) {
            par += 100;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 57, par);
    }

    public byte get_level_perfect() {
        byte result = 0;
        for (int i = 0; i < p.item.it_body.length; i++) {
            if (p.item.it_body[i] != null && p.item.it_body[i].isHoanMy == 1
                    && p.item.it_body[i].valueKichAn > -1) {
                result++;
            }
        }
        return result;
    }

    public long get_percent_final_dame() {
        return get_percent_final_dame(true);
    }

    public long get_percent_final_dame(boolean have_eff) {
        long par = total_param_item(46, have_eff);
        return core.BalanceGateway.applyStatLimit(this.p, 46, par); // Sat thuong cuoi %
    }

    public long get_xp_more() {
        long par = total_param_item(67, true);
        par += Body.Point5_Template_exp_bonus[(int) this.get_total_point(5) - 1]; // ✅ thêm EXP% từ Nhanh nhẹn
        return core.BalanceGateway.applyStatLimit(this.p, 67, par); // % EXP
    }

    public long get_xp_skill_more() {
        return core.BalanceGateway.applyStatLimit(this.p, 68, total_param_item(68, true));
    }

    public int get_dame_devil_percent() {
        int par = 100;
        Skill_info sk = p.skill_point.get(0);
        switch (sk.lvdevil) {
            case 1: {
                par += 10;
                break;
            }
            case 2: {
                par += 25;
                break;
            }
            case 3: {
                par += 45;
                break;
            }
            case 4: {
                par += 70;
                break;
            }
            case 5: {
                par += 100;
                break;
            }
        }
        return (int) core.BalanceGateway.applyStatLimit(this.p, 75, par);
    }

    public long get_reduce_Eff() {
        long par = total_param_item(47, true);
        return core.BalanceGateway.applyStatLimit(this.p, 47, par);
    }

    public long get_HapThu_Hp() {
        long par = total_param_item(59, true);
        ItemFashionP2 itF = p.check_fashion(124);
        if (itF != null && itF.is_use) {
            par *= 2;
        }
        return core.BalanceGateway.applyStatLimit(this.p, 59, par);
    }

    public long get_def_target_reduce() {
        long par = total_param_item(70, true);
        return core.BalanceGateway.applyStatLimit(this.p, 70, par);
    }

    // private static void load_point_1() {
    // Point1_Template_atk = new long[level];
    // Point1_Template_crit = new long[level];
    // Point1_Template_pierce = new long[level];
    // for (int i = 0; i < Point1_Template_crit.length; i++) {
    // if (i == 19) {
    // Point1_Template_crit[i] = 10;
    // Point1_Template_pierce[i] = 10;
    // } else if (i > 19) {
    // Point1_Template_crit[i] = Point1_Template_crit[i - 1] + 1;
    // Point1_Template_pierce[i] = Point1_Template_pierce[i - 1] + 2;
    // } else {
    // Point1_Template_crit[i] = 0;
    // Point1_Template_pierce[i] = 0;
    // }
    // }
    // //
    // int[] add_per_level = new int[] { 2, 4, 2, 2, 4 };
    // int par_add = 20;
    // int index = 0;
    // Point1_Template_atk[0] = 40;
    // Point1_Template_atk[1] = Point1_Template_atk[0] + par_add;
    // for (int i = 2; i < Point1_Template_atk.length; i++) {
    // par_add += add_per_level[index++];
    // if (index >= add_per_level.length) {
    // index = 0;
    // }
    // Point1_Template_atk[i] = Point1_Template_atk[i - 1] + par_add;
    // }
    // }

    // private static void load_point_2() {
    // Point2_Template_def = new long[level];
    // Point2_Template_resist_magic = new long[level];
    // Point2_Template_resist_physical = new long[level];
    // for (int i = 0; i < Point2_Template_resist_magic.length; i++) {
    // if (i == 19) {
    // Point2_Template_resist_magic[i] = 10;
    // Point2_Template_resist_physical[i] = 10;
    // } else if (i > 19) {
    // Point2_Template_resist_magic[i] = Point2_Template_resist_magic[i - 1] + 5;
    // Point2_Template_resist_physical[i] = Point2_Template_resist_physical[i - 1] +
    // 5;
    // } else {
    // Point2_Template_resist_magic[i] = 0;
    // Point2_Template_resist_physical[i] = 0;
    // }
    // }
    // //
    // int par_add = 20;
    // Point2_Template_def[0] = 80;
    // Point2_Template_def[1] = (Point2_Template_def[0] * 10 + par_add) / 10;
    // int change = 3;
    // for (int i = 2; i < Point2_Template_def.length; i++) {
    // if (((i - 2 + 1) % (10)) == 0) {
    // change = 2;
    // }
    // if (((i - 2) % (change)) == 0) {
    // par_add += 20;
    // change = 3;
    // }
    // Point2_Template_def[i] = (Point2_Template_def[i - 1] * 10 + par_add) / 10;
    // }
    // }

    // private static void load_point_3() {
    // Point3_Template_hp = new long[level];
    // Point3_Template_hp_potion = new long[level];
    // for (int i = 0; i < Point3_Template_hp_potion.length; i++) {
    // if (i == 19) {
    // Point3_Template_hp_potion[i] = 12;
    // } else if (i > 19) {
    // Point3_Template_hp_potion[i] = Point3_Template_hp_potion[i - 1] + 12;
    // } else {
    // Point3_Template_hp_potion[i] = 0;
    // }
    // }
    // //
    // int par_add = 15;
    // Point3_Template_hp[0] = 1001;
    // Point3_Template_hp[1] = Point3_Template_hp[0] + par_add;
    // for (int i = 2; i < Point3_Template_hp.length; i++) {
    // par_add += 10;
    // Point3_Template_hp[i] = Point3_Template_hp[i - 1] + par_add;
    // }
    // }

    // private static void load_point_4() {
    // Point4_Template_mp = new long[level];
    // Point4_Template_dame_crit = new long[level];
    // for (int i = 0; i < Point4_Template_dame_crit.length; i++) {
    // if (i == 19) {
    // Point4_Template_dame_crit[i] = 20;
    // } else if (i > 19) {
    // Point4_Template_dame_crit[i] = Point4_Template_dame_crit[i - 1] + 20;
    // } else {
    // Point4_Template_dame_crit[i] = 0;
    // }
    // }
    // //
    // int par_add = 1;
    // Point4_Template_mp[0] = 20;
    // Point4_Template_mp[1] = Point4_Template_mp[0] + par_add;
    // for (int i = 2; i < Point4_Template_mp.length; i++) {
    // if (((i - 2) % 2) == 0) {
    // par_add += 2;
    // }
    // Point4_Template_mp[i] = Point4_Template_mp[i - 1] + par_add;
    // }
    // }

    // private static void load_point_5() {
    // Point5_Template_cooldown = new long[level];
    // Point5_Template_miss = new long[level];
    // for (int i = 0; i < Point5_Template_miss.length; i++) {
    // if (i == 19) {
    // Point5_Template_miss[i] = 10;
    // } else if (i > 19) {
    // Point5_Template_miss[i] = Point5_Template_miss[i - 1] + 1;
    // } else {
    // Point5_Template_miss[i] = 0;
    // }
    // }
    // //
    // int par_add = 3;
    // Point5_Template_cooldown[0] = 13;
    // Point5_Template_cooldown[1] = Point5_Template_cooldown[0] + par_add;
    // for (int i = 2; i < Point5_Template_cooldown.length; i++) {
    // Point5_Template_cooldown[i] = Point5_Template_cooldown[i - 1] + par_add;
    // }
    // }
    // private static void load_point_1() {
    // Point1_Template_atk = new long[level];
    // Point1_Template_crit = new long[level];
    // Point1_Template_pierce = new long[level];

    // for (int i = 0; i < level; i++) {
    // if (i < 19) {
    // Point1_Template_crit[i] = 0;
    // Point1_Template_pierce[i] = 0;
    // } else if (i == 19) {
    // Point1_Template_crit[i] = 10;
    // Point1_Template_pierce[i] = 10;
    // } else {
    // Point1_Template_crit[i] = Point1_Template_crit[i - 1] + 1;
    // Point1_Template_pierce[i] = Point1_Template_pierce[i - 1] + 2;
    // }
    // // ATK tuyến tính: bắt đầu 40, +22 mỗi level
    // Point1_Template_atk[i] = 40 + 22L * i;
    // }
    // }

    // private static void load_point_2() {
    // Point2_Template_def = new long[level];
    // Point2_Template_resist_magic = new long[level];
    // Point2_Template_resist_physical = new long[level];

    // for (int i = 0; i < level; i++) {
    // if (i < 19) {
    // Point2_Template_resist_magic[i] = 0;
    // Point2_Template_resist_physical[i] = 0;
    // } else if (i == 19) {
    // Point2_Template_resist_magic[i] = 10;
    // Point2_Template_resist_physical[i] = 10;
    // } else {
    // Point2_Template_resist_magic[i] = Point2_Template_resist_magic[i - 1] + 2;
    // Point2_Template_resist_physical[i] = Point2_Template_resist_physical[i - 1] +
    // 2;
    // }
    // // DEF tuyến tính: bắt đầu 80, +11 mỗi level (~50% ATK)
    // Point2_Template_def[i] = 80 + 11L * i;
    // }
    // }

    // private static void load_point_3() {
    // Point3_Template_hp = new long[level];
    // Point3_Template_hp_potion = new long[level];

    // for (int i = 0; i < level; i++) {
    // if (i < 19) {
    // Point3_Template_hp_potion[i] = 0;
    // } else if (i == 19) {
    // Point3_Template_hp_potion[i] = 12;
    // } else {
    // Point3_Template_hp_potion[i] = Point3_Template_hp_potion[i - 1] + 12;
    // }
    // // HP tuyến tính: bắt đầu 1001, +200 mỗi level
    // Point3_Template_hp[i] = 1001 + 200L * i;
    // }
    // }

    // private static void load_point_4() {
    // Point4_Template_mp = new long[level];
    // Point4_Template_dame_crit = new long[level];

    // for (int i = 0; i < level; i++) {
    // if (i < 19) {
    // Point4_Template_dame_crit[i] = 0;
    // } else if (i == 19) {
    // Point4_Template_dame_crit[i] = 20;
    // } else {
    // Point4_Template_dame_crit[i] = Point4_Template_dame_crit[i - 1] + 20;
    // }
    // // MP tuyến tính: bắt đầu 20, +8 mỗi level
    // Point4_Template_mp[i] = 20 + 8L * i;
    // }
    // }

    // private static void load_point_5() {
    // Point5_Template_cooldown = new long[level];
    // Point5_Template_miss = new long[level];

    // for (int i = 0; i < level; i++) {
    // if (i < 19) {
    // Point5_Template_miss[i] = 0;
    // } else if (i == 19) {
    // Point5_Template_miss[i] = 10;
    // } else {
    // Point5_Template_miss[i] = Point5_Template_miss[i - 1] + 1;
    // }
    // // Cooldown tuyến tính: bắt đầu 13, +3 mỗi level
    // Point5_Template_cooldown[i] = 10 + 1L * i;
    // }
    // }
    // ============================================================
    // POINT 1 - SỨC MẠNH: ATK%, Crit%, Pierce%, Dame Crit%
    // Thêm dame_crit vào đây vì nó thuộc hệ tấn công
    // ============================================================
    /**
     * Xây dựng mảng chỉ số theo bảng cấu hình mốc.
     * Mỗi hàng segs[k] = { endIndex, addPerStep, stepDivisor, milestoneBonus }
     *
     * endIndex : chỉ số cuối của đoạn này (inclusive)
     * addPerStep : lượng cộng mỗi bước hợp lệ
     * stepDivisor : cứ bao nhiêu bước mới cộng 1 lần
     * (0 = khoá/không tăng, 1 = mỗi bước)
     * milestoneBonus: thưởng thêm đúng tại endIndex
     */
    private static long[] buildStat(int size, long initVal, long[][] segs) {
        long[] arr = new long[size];
        arr[0] = initVal;
        int s = 0;
        for (int i = 1; i < size; i++) {
            while (s < segs.length - 1 && i > segs[s][0])
                s++;
            long div = segs[s][2];
            long add = div == 0 ? 0
                    : div == 1 ? segs[s][1]
                            : (i % div == 0 ? segs[s][1] : 0);
            long bonus = (i == segs[s][0]) ? segs[s][3] : 0;
            arr[i] = arr[i - 1] + add + bonus;
        }
        return arr;
    }

    // ════════════════════════════════════════════════════════════════════════
    // POINT 1 — SỨC MẠNH: ATK%(1) | Crit%(10) | Pierce%(13)
    // ════════════════════════════════════════════════════════════════════════
    private static void load_point_1() {
        // ATK%: tăng liên tục nhưng chậm dần, thưởng mốc lớn
        // ≈300 @100đ | ≈650 @500đ | ≈900 @1000đ | ≈1700 @5000đ | ≈2400 @10000đ
        Point1_Template_atk = buildStat(level, 3, new long[][] {
                // { endIdx, add, div, milestone }
                { 0, 1, 1, 100 },
                { 99, 80, 1, 5 }, // ★ 100đ (từ 1 - 99, cứ 1 tn sẽ cộng 3, cuối mốc thưởng
                { 499, 75, 1, 10 }, // ★ 500đ
                { 999, 80, 1, 10 }, // ★ 1000đ
                { 1999, 80, 1, 20 }, // ★ 2000đ
                { 4999, 60, 1, 35 }, // ★ 5000đ
                { 9999, 55, 1, 50 }, // ★ 10000đ
                { 19999, 50, 1, 80 }, // ★ 20000đ
                { 29999, 45, 1, 300 }, // ★ 30000đ
        });

        // Crit%: unlock 500đ, softcap ~88% ở 32000đ
        // =10 @500đ | ≈25 @1000đ | ≈42 @2000đ | ≈60 @5000đ | ≈73 @10000đ
        Point1_Template_crit = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 99, 10, 1, 500 }, // ★ 100đ
                { 499, 10, 5, 500 }, // ★ 500đ unlock: +50
                { 999, 10, 10, 300 }, // ★ 1000đ +50 rate + 30 bonus
                { 1999, 10, 33, 300 }, // ★ 2000đ +30 rate + 40 bonus
                { 4999, 10, 60, 500 }, // ★ 5000đ +50 rate + 50 bonus
                { 9999, 10, 50, 1000 }, // ★ 10000đ +100 rate + 100 bonus
                { 19999, 10, 67, 1000 }, // ★ 20000đ +149 rate + 100 bonus
                { 29999, 10, 67, 500 }, // ★ 30000đ +149 rate + 50 bonus
                { 31999, 10, 38, 0 }, // → ~1000 tại 32000đ
        });

        // Pierce%: unlock muộn 2000đ, phần thưởng cho build tấn công sâu
        // =15 @2000đ | ≈35 @5000đ | ≈55 @10000đ | ≈70 @20000đ | ≈78 @30000đ
        Point1_Template_pierce = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 9, 0, 0, 0 }, // 0–9đ khoá
                { 499, 10, 50, 60 }, // ★ 500đ (+9 rate + 6 bonus ≈ 15)
                { 999, 10, 20, 10 }, // ★ 1000đ (+25 ≈ 40)
                { 1999, 10, 25, 10 }, // ★ 2000đ (+40 ≈ 80)
                { 4999, 10, 30, 10 }, // ★ 5000đ (+100 ≈ 180)
                { 9999, 10, 30, 10 }, // ★ 10000đ (+170 ≈ 350)
                { 19999, 10, 33, 10 }, // ★ 20000đ (+300 ≈ 650)
                { 29999, 10, 40, 10 }, // ★ 30000đ (+250 ≈ 900)
                { 31999, 10, 20, 10 }, // → ~1000 tại 32000đ
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // POINT 2 — PHÒNG THỦ: DEF%(4) | DEF flat(3) | Kháng vật lý(26) | Kháng
    // phép(27) | Giảm pierce(50)
    // ════════════════════════════════════════════════════════════════════════
    private static void load_point_2() {
        // DEF flat: ~1.000.000 tại 32000đ
        // ≈33k @100đ | ≈121k @500đ | ≈208k @1000đ | ≈548k @5000đ | ≈778k @10000đ
        Point2_Template_def = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 99, 3, 1, 30 }, // ★ 100đ → ~3,300
                { 499, 2, 1, 80 }, // ★ 500đ → ~12,000
                { 999, 1, 1, 120 }, // ★ 1000đ → ~20,800
                { 4999, 1, 2, 200 }, // ★ 5000đ → ~54,800
                { 9999, 1, 3, 300 }, // ★ 10000đ→ ~77,800
                { 19999, 1, 5, 300 }, // ★ 20000đ→ ~95,800
                { 29999, 1, 10, 80 }, // ★ 30000đ→ ~99,600
                { 31999, 1, 20, 0 }, // → ~100,000
        });

        // DEF%: ~200 tại 32000đ
        // =10 @500đ | ≈20 @1000đ | ≈37 @2000đ | ≈61 @5000đ
        // ≈101 @10000đ | ≈161 @20000đ | ≈191 @30000đ | ≈201 @32000đ
        Point2_Template_def_percent = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 499, 1, 0, 100 }, // ★ 500đ unlock → 10
                { 999, 1, 10, 0 }, // ★ 1000đ → ~20
                { 1999, 1, 12, 0 }, // ★ 2000đ → ~37
                { 4999, 1, 25, 0 }, // ★ 5000đ → ~61
                { 9999, 1, 25, 0 }, // ★ 10000đ→ ~101
                { 19999, 1, 33, 0 }, // ★ 20000đ→ ~161
                { 29999, 1, 67, 0 }, // ★ 30000đ→ ~191
                { 31999, 1, 40, 0 }, // → ~201
        });

        // Kháng vật lý: ~1000 tại 32000đ
        // =30 @1000đ | ≈80 @2000đ | ≈200 @5000đ | ≈400 @10000đ
        // ≈703 @20000đ | ≈953 @30000đ | ≈1003 @32000đ
        Point2_Template_resist_physical = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 999, 10, 0, 300 }, // ★ 1000đ unlock
                { 1999, 10, 20, 0 }, // ★ 2000đ → ~80
                { 4999, 10, 25, 0 }, // ★ 5000đ → ~200
                { 9999, 10, 25, 0 }, // ★ 10000đ → ~400
                { 19999, 10, 33, 0 }, // ★ 20000đ → ~703
                { 29999, 10, 40, 0 }, // ★ 30000đ → ~953
                { 31999, 10, 40, 0 }, // → ~1003
        });

        // Kháng phép: ~1000 tại 32000đ
        // =30 @2000đ | ≈130 @5000đ | ≈302 @10000đ | ≈647 @20000đ
        // ≈941 @30000đ | ≈998 @32000đ
        Point2_Template_resist_magic = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 1999, 10, 0, 300 }, // ★ 2000đ unlock
                { 4999, 10, 30, 0 }, // ★ 5000đ → ~130
                { 9999, 10, 29, 0 }, // ★ 10000đ → ~302
                { 19999, 10, 29, 0 }, // ★ 20000đ → ~647
                { 29999, 10, 34, 0 }, // ★ 30000đ → ~941
                { 31999, 10, 35, 0 }, // → ~998
        });

        // Giảm xuyên giáp: ~1000 tại 32000đ
        // =30 @5000đ | ≈155 @10000đ | ≈500 @20000đ | ≈900 @30000đ | ≈1000 @32000đ
        Point2_Template_reduce_pierce = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 4999, 10, 0, 300 }, // ★ 5000đ unlock
                { 9999, 10, 40, 0 }, // ★ 10000đ → ~155
                { 19999, 10, 29, 0 }, // ★ 20000đ → ~500
                { 29999, 10, 25, 0 }, // ★ 30000đ → ~900
                { 31999, 10, 20, 0 }, // → ~1000
        });

        // Phản đòn: ~1000 tại 32000đ
        // =30 @1000đ | ≈80 @2000đ | ≈200 @5000đ | ≈400 @10000đ
        // ≈703 @20000đ | ≈953 @30000đ | ≈1003 @32000đ
        Point2_Template_counter_attack = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 999, 10, 0, 300 }, // ★ 1000đ unlock
                { 1999, 10, 20, 0 }, // ★ 2000đ → ~80
                { 4999, 10, 25, 0 }, // ★ 5000đ → ~200
                { 9999, 10, 25, 0 }, // ★ 10000đ → ~400
                { 19999, 10, 33, 0 }, // ★ 20000đ → ~703
                { 29999, 10, 40, 0 }, // ★ 30000đ → ~953
                { 31999, 10, 40, 0 }, // → ~1003
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // POINT 3 — THỂ LỰC: HP flat(15) | %HP(17) | Hồi HP(19) | Hút máu(21) | Thuốc
    // HP(23)
    // ════════════════════════════════════════════════════════════════════════
    private static void load_point_3() {
        // HP flat: ~5.000.000 tại 32000đ
        // ≈150k @100đ | ≈480k @500đ | ≈800k @1000đ | ≈2M @5000đ | ≈3M @10000đ | ≈4.2M
        // @20000đ | ≈4.8M @30000đ
        Point3_Template_hp = buildStat(level, 3000, new long[][] {
                { 0, 1, 1, 100 },
                { 99, 150, 1, 100 }, // ★ 100đ → ~152k
                { 499, 80, 1, 1000 }, // ★ 500đ → ~482k
                { 999, 60, 1, 2000 }, // ★ 1000đ → ~802k
                { 4999, 28, 1, 8000 }, // ★ 5000đ → ~2M
                { 9999, 18, 1, 10000 }, // ★ 10000đ → ~3M
                { 19999, 10, 1, 20000 }, // ★ 20000đ → ~4.2M
                { 29999, 50, 1, 10000 }, // ★ 30000đ → ~4.8M
                { 31999, 10, 1, 0 }, // → ~5.000.000
        });

        // %HP: ~300% tại 32000đ
        // =15 @1000đ | ≈35 @2000đ | ≈80 @5000đ | ≈140 @10000đ | ≈220 @20000đ | ≈280
        // @30000đ
        Point3_Template_hp_percent = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 999, 10, 0, 150 }, // ★ 1000đ unlock
                { 1999, 10, 50, 0 }, // ★ 2000đ → ~35
                { 4999, 10, 67, 0 }, // ★ 5000đ → ~80
                { 9999, 10, 83, 0 }, // ★ 10000đ → ~140
                { 19999, 10, 125, 0 }, // ★ 20000đ → ~220
                { 29999, 10, 167, 0 }, // ★ 30000đ → ~280
                { 31999, 10, 100, 0 }, // → ~300
        });

        // Hồi HP: unlock 500đ
        // =500 @500đ | ≈2000 @1000đ | ≈8000 @5000đ | ≈18000 @10000đ | ≈38000 @20000đ
        Point3_Template_hp_regen = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 499, 0, 0, 500 }, // ★ 500đ unlock
                { 999, 30, 1, 500 }, // ★ 1000đ
                { 4999, 20, 1, 2000 }, // ★ 5000đ
                { 9999, 15, 1, 3000 }, // ★ 10000đ
                { 19999, 10, 1, 5000 }, // ★ 20000đ
                { 29999, 5, 1, 3000 }, // ★ 30000đ
                { level - 1, 3, 1, 0 },
        });

        // Hút máu: ~10000 tại 32000đ (tăng dần, thưởng mạnh khi đầu tư sâu)
        // =50 @5000đ | ≈550 @10000đ | ≈3050 @20000đ | ≈8050 @30000đ | ≈10050 @32000đ
        Point3_Template_lifesteal = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 4999, 0, 0, 50 }, // ★ 5000đ unlock
                { 9999, 1, 10, 0 }, // ★ 10000đ → ~550
                { 19999, 1, 4, 0 }, // ★ 20000đ → ~3050
                { 29999, 1, 2, 0 }, // ★ 30000đ → ~8050
                { 31999, 1, 1, 0 }, // → ~10050
        });

        // Thuốc HP: unlock 2000đ
        // =10 @2000đ | ≈30 @5000đ | ≈60 @10000đ | ≈100 @20000đ | ≈130 @30000đ
        Point3_Template_hp_potion = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 1999, 100, 0, 1000 }, // ★ 2000đ unlock
                { 4999, 100, 150, 800 }, // ★ 5000đ
                { 9999, 100, 200, 1000 }, // ★ 10000đ
                { 19999, 100, 300, 1000 }, // ★ 20000đ
                { 29999, 100, 500, 1000 }, // ★ 30000đ
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // POINT 4 — TINH THẦN: MP flat(16) | %MP(18) | Hồi MP(20) | Hút MP(22) | Thuốc
    // MP(24) | Dame Crit%(11)
    // ════════════════════════════════════════════════════════════════════════
    private static void load_point_4() {
        // MP flat: luôn tăng
        // ≈5k @100đ | ≈30k @1000đ | ≈120k @5000đ | ≈230k @10000đ
        Point4_Template_mp = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 99, 50, 1, 1000 }, // ★ 100đ
                { 499, 40, 1, 3000 }, // ★ 500đ
                { 999, 30, 1, 5000 }, // ★ 1000đ
                { 4999, 20, 1, 20000 }, // ★ 5000đ
                { 9999, 15, 1, 30000 }, // ★ 10000đ
                { 19999, 10, 1, 50000 }, // ★ 20000đ
                { 29999, 5, 1, 20000 }, // ★ 30000đ
        });

        // %MP: ~300 tại 32000đ
        // =15 @500đ | ≈35 @1000đ | ≈80 @5000đ | ≈140 @10000đ | ≈220 @20000đ | ≈280
        // @30000đ
        Point4_Template_mp_percent = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 499, 10, 0, 150 }, // ★ 500đ unlock → 15
                { 999, 10, 25, 0 }, // ★ 1000đ → ~35
                { 4999, 10, 90, 0 }, // ★ 5000đ → ~79
                { 9999, 10, 80, 0 }, // ★ 10000đ → ~141
                { 19999, 10, 125, 0 }, // ★ 20000đ → ~221
                { 29999, 10, 170, 0 }, // ★ 30000đ → ~279
                { 31999, 10, 100, 0 }, // → ~299
        });

        // Hồi MP: unlock 500đ
        // =100 @500đ | ≈500 @1000đ | ≈3000 @5000đ | ≈7000 @10000đ | ≈15000 @20000đ
        Point4_Template_mp_regen = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 499, 0, 0, 100 }, // ★ 500đ unlock
                { 999, 8, 1, 200 }, // ★ 1000đ
                { 4999, 5, 1, 800 }, // ★ 5000đ
                { 9999, 4, 1, 1200 }, // ★ 10000đ
                { 19999, 3, 1, 2000 }, // ★ 20000đ
                { 29999, 2, 1, 1000 }, // ★ 30000đ
                { level - 1, 1, 1, 0 },
        });

        // Hút MP: ~5000 tại 32000đ (tăng tốc mạnh ở giai đoạn sâu)
        // =50 @5000đ | ≈300 @10000đ | ≈1550 @20000đ | ≈4050 @30000đ | ≈5050 @32000đ
        Point4_Template_mp_steal = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 4999, 0, 0, 50 }, // ★ 5000đ unlock → 50
                { 9999, 1, 20, 0 }, // ★ 10000đ → +250, ~300
                { 19999, 1, 8, 0 }, // ★ 20000đ → +1250, ~1550
                { 29999, 1, 4, 0 }, // ★ 30000đ → +2500, ~4050
                { 31999, 1, 2, 0 }, // → +1000, ~5050
        });

        // Thuốc MP: unlock 2000đ
        // =10 @2000đ | ≈28 @5000đ | ≈55 @10000đ | ≈90 @20000đ | ≈120 @30000đ
        Point4_Template_mp_potion = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 1999, 100, 0, 1000 }, // ★ 2000đ unlock
                { 4999, 100, 150, 800 }, // ★ 5000đ
                { 9999, 100, 200, 1000 }, // ★ 10000đ
                { 19999, 100, 300, 1200 }, // ★ 20000đ
                { 29999, 100, 500, 1000 }, // ★ 30000đ
        });

        // // Dame Crit%: ~30000 tại 32000đ — phần thưởng độc quyền ALL-IN Tinh thần
        // // =1000 @10000đ | ≈10000 @20000đ | ≈25000 @30000đ | ≈30000 @32000đ
        // Point4_Template_dame_crit = buildStat(level, 0, new long[][] {
        // { 10, 25, 1, 0 }, // ★ 9đ unlock → 1000
        // { 9999, 25, 1, 10000 }, // ★ 10000đ unlock → 1000
        // { 19999, 45, 10, 0 }, // ★ 20000đ → +9000, ~10000
        // { 29999, 30, 2, 0 }, // ★ 30000đ → +15000, ~25000
        // { 31999, 25, 2, 0 }, // → +5000, ~30000
        // });
        Point4_Template_dame_crit = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 10, 10, 1, 0 }, // 1-10đ → 1%/điểm
                { 9999, 10, 1, 0 }, // ~1000% tại 10000đ
                { 19999, 15, 2, 0 }, // ~1750% tại 20000đ
                { 29999, 8, 2, 0 }, // ~2800% tại 30000đ
                { 31999, 5, 2, 0 }, // ~3000% tại 32000đ
                { 180000, 1, 1, 0 }, // +1% mỗi 1 điểm sau 32000 tại 180000đ → ~3000 +
                                     // (180000-32000)/1 = ~14800%
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // POINT 5 — NHANH NHẸN: Agility(25) | Né tránh%(12) | EXP%(67) | Beri%(72)
    // ════════════════════════════════════════════════════════════════════════
    private static void load_point_5() {
        // Hồi chiêu (Agility): ~1000 tại 32000đ, bắt đầu tăng từ sớm
        // ≈36 @100đ | ≈86 @500đ | ≈136 @1000đ | ≈302 @5000đ | ≈502 @10000đ | ≈752
        // @20000đ | ≈952 @30000đ
        Point5_Template_cooldown = buildStat(level, 3, new long[][] {
                { 0, 1, 1, 100 },
                { 99, 10, 3, 0 }, // → ~36
                { 499, 10, 8, 0 }, // → ~86
                { 999, 10, 10, 0 }, // → ~136
                { 4999, 10, 24, 0 }, // → ~302
                { 9999, 10, 25, 0 }, // → ~502
                { 19999, 10, 40, 0 }, // → ~752
                { 29999, 10, 50, 0 }, // → ~952
                { 31999, 10, 40, 0 }, // → ~1002
        });

        // ~25 @500đ | ~192 @1000đ | ~342 @4000đ | ~422 @8000đ | ~472 @15000đ | ~492
        // @25000đ | ~500 @32000đ
        Point5_Template_miss = buildStat(level, 0, new long[][] {
                { 0, 1, 10, 0 } // Mỗi 10 level +1, tăng hoàn toàn tuyến tính
        });

        // EXP%: ~500 tại 32000đ, unlock 5000đ
        // =20 @5000đ | ≈80 @10000đ | ≈246 @20000đ | ≈446 @30000đ | ≈501 @32000đ
        Point5_Template_exp_bonus = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 4999, 10, 0, 200 }, // ★ 5000đ unlock
                { 9999, 10, 83, 0 }, // → ~80
                { 19999, 10, 60, 0 }, // → ~246
                { 29999, 10, 50, 0 }, // → ~446
                { 31999, 10, 36, 0 }, // → ~501
        });

        // Beri%: ~200 tại 32000đ, unlock 10000đ
        // =20 @10000đ | ≈79 @20000đ | ≈159 @30000đ | ≈199 @32000đ
        Point5_Template_beri_bonus = buildStat(level, 0, new long[][] {
                { 0, 1, 1, 100 },
                { 9999, 10, 0, 200 }, // ★ 10000đ unlock
                { 19999, 10, 167, 0 }, // → ~79
                { 29999, 10, 125, 0 }, // → ~159
                { 31999, 10, 50, 0 }, // → ~199
        });
    }

}