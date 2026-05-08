package core;

import tool.ItemDropTool;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalTime;

import activities.*;
import client.*;
import database.SQL;
import io.Message;
import map.*;
import template.*;

public class MenuController {

    private static long lastPrayTime = 0;
    private static boolean sknoel = false;
    private static boolean sktet = false;

    public static int[] ID_MAP_LANG = new int[] { 1, 17, 33, 113, 191 };

    public static void send_menu(Player p, Message m) throws IOException {
        try {
            if (!p.isdie) {
                short type = m.reader().readShort();
                // core.GameLogger.info("npc id " + type);
                boolean in_map = false;
                for (int i = 0; i < p.map.template.npcs.size(); i++) {
                    if (type == p.map.template.npcs.get(i).iditem) {
                        in_map = true;
                        break;
                    }
                }
                if (!in_map) {
                    return;
                }
                switch (type) {
                    case -666: {
                        send_dynamic_menu(p, type, "Dịch chuyển",
                                new String[] { "Quay trở về" }, new short[] { 157 });
                        break;
                    }
                    case -781: { // trang trí
                        if (!sknoel) {
                            Service.send_box_ThongBao_OK(p, "Sự kiện Noel chưa diễn ra.");
                            return;
                        }
                        UpgradeDevil.show_table(p, 8);
                        break;
                    }
                    case -98: {
                        if (p.map.template.id == 994) { // bang thong tin map tran chien lon
                            send_dynamic_menu(p, type, "", new String[] { "Thông tin" });
                        } else if (p.map.template.id == 260) { // bang thong tin phong cho pvp bang
                            send_dynamic_menu(p, type, "", new String[] { "Thông tin", "Xem thi đấu" });
                        }
                        break;
                    }
                    case -99: {
                        send_dynamic_menu(p, type, get_name_npc(type), new String[] { "Quán ăn", "Rời khỏi đây" },
                                new short[] { 104, 157 });
                        break;
                    }
                    case -780: {
                        if (!sknoel) {
                            Service.send_box_ThongBao_OK(p, "Sự kiện Noel chưa diễn ra.");
                            return;
                        }
                        send_dynamic_menu(p, type, "Ông già Noel",
                                new String[] { "Lãnh địa Noel", "Vũ khí Noel", "Hướng dẫn" },
                                new short[] { 142, 153, 148 });
                        break;
                    }
                    case -784: { // tu tien
                        send_dynamic_menu(p, type, "Tu Tiên",
                                new String[] { "Tu Tiên", "Bãi Farm Đá", "Boss Xà Nữ", "Mảnh Đất Trữ Tình",
                                        "Bãi Farm Đá 2" },
                                null);
                        break;
                    }
                    case -778: {
                        int itemCount = 0;
                        try (Connection conn2 = SQL.gI().getCon();
                                PreparedStatement ps = conn2.prepareStatement(
                                        "SELECT `box` FROM `accounts` WHERE `user` = ? LIMIT 1")) {
                            ps.setString(1, p.conn.user);
                            ResultSet rs2 = ps.executeQuery();
                            if (rs2.next()) {
                                String boxData = rs2.getString("box");
                                if (boxData != null && !boxData.trim().isEmpty()) {
                                    int count = 0;
                                    for (int i = 0; i < boxData.length(); i++) {
                                        if (boxData.charAt(i) == '[')
                                            count++;
                                    }
                                    itemCount = Math.max(0, count - 1);
                                }
                            }
                            rs2.close();
                        } catch (SQLException e) {
                            GameLogger.error("MenuController: SQL error loading box for " + p.conn.user, e);
                        }
                        String[] menuOptions = new String[] {
                                "Cầu nguyện",
                                "Điểm danh",
                                "Hòm Quà (" + itemCount + " VP)",
                                "Danh hiệu"
                        };
                        short[] menuIds = new short[] { 140, 110, 135, 165, 134 };
                        send_dynamic_menu(p, type, "Lãnh chúa", menuOptions, menuIds);
                        break;
                    }

                    case -777: {
                        send_dynamic_menu(p, type, "Zack",
                                new String[] { "Chế Tạo Trang Bị", "Đảo Bánh Kem", "Boss Siêu Trùm", "Boss Thế Giới",
                                        "Boss Cao Nguyên", "Đảo Extol", "Chế tạo trang bị GR", "Đảo Bánh Kem 2" },
                                new short[] { 131, 153, 167, 171, 171, 167, 172, 153 });
                        break;
                    }
                    case -776: {
                        send_dynamic_menu(p, type, "Dofi",
                                new String[] { "Siêu Hang Động", "Tạo Trang Bị 2", "Tạo Trứng Pet VIP",
                                        "Tạo Lõi Năng Lượng", "Cường hóa Trang bị 2", "Top Hang Động", "Ghép Dial GR" },
                                new short[] { 154, 126, 145, 127, 163, 102, 172 });
                        break;
                    }
                    case -443: {
                        send_dynamic_menu(p, type, "Vận Buôn",
                                new String[] { "Lấy Hàng", "Chọn Phe", "Huỷ Hàng" },
                                new short[] { 107, 110, 113 });
                        break;
                    }
                    case -444: {
                        send_dynamic_menu(p, type, "Croket",
                                new String[] { "Trả Hàng" },
                                new short[] { 109 });
                        break;
                    }
                    case -140: {
                        send_dynamic_menu(p, type, "WIPPER",
                                new String[] { "Chế tạo Dial", "Cường hoá Dial", "Đục lỗ vật phẩm", "Dòng sông mây",
                                        "Thử thách vệ thần", "Lên Thiên Đàng", "Xuống Địa Ngục" },
                                null);
                        break;
                    }
                    case -86: {
                        send_dynamic_menu(p, type, "Phó bản",
                                new String[] { "Đá đít Mr3", "Phó bản khổng lồ", "Hướng dẫn Phó bản khổng lồ" },
                                new short[] { 150, 142, 148 });
                        break;
                    }
                    case -77: {
                        send_dynamic_menu(p, type, "Ms Gym", new String[] { "BXH Đấu trường", "Hướng dẫn" }, null);
                        break;
                    }
                    case -73: {
                        send_dynamic_menu(p, type, "Croket", new String[] { "Hướng dẫn" }, null);
                        break;
                    }
                    case -84: {
                        if (p.clan != null) {
                            if (p.clan.members.get(0).name.equals(p.name)) {
                                if (p.clan.allowRequest == 1) {
                                    send_dynamic_menu(p, type, "Băng hải tặc",
                                            new String[] { "Nhiệm vụ băng", "Huy hiệu hành trình", "Phó bản băng",
                                                    "Cửa hàng biểu tượng", "Cửa hàng vật phẩm", "Khóa xin vào băng",
                                                    "Đổi tên băng",
                                                    "Nhường băng",
                                                    "Xóa băng" },
                                            new short[] { 141, 171, 146, 143, 144, 118, 110, 128, 113 });
                                } else {
                                    send_dynamic_menu(p, type, "Băng hải tặc",
                                            new String[] { "Nhiệm vụ băng", "Huy hiệu hành trình", "Phó bản băng",
                                                    "Cửa hàng biểu tượng", "Cửa hàng vật phẩm", "Mở xin vào băng",
                                                    "Đổi tên băng",
                                                    "Nhường băng",
                                                    "Xóa băng" },
                                            new short[] { 141, 171, 146, 143, 144, 118, 110, 128, 113 });
                                }
                            } else {
                                send_dynamic_menu(p, type, "Băng hải tặc",
                                        new String[] { "Nhiệm vụ băng", "Huy hiệu hành trình", "Phó bản băng" },
                                        new short[] { 141, 171, 146 });
                            }
                        } else {
                            send_dynamic_menu(p, type, "Băng hải tặc",
                                    new String[] { "Tạo Băng (50.000 Coin)", "Hướng dẫn" }, null);
                        }
                        break;
                    }
                    case -138: { // npc law
                        send_dynamic_menu(p, type, "Law",
                                new String[] { "Phẫu Thuật Tim", "Cường Hoá Tim", "Vòng Xoay May Mắn", "Tài xỉu",
                                        "Bầu Cua", "Blackjack", "Slot Machine", "Ghép Tim GR" },
                                null);
                        break;
                    }
                    case -106:
                    case -91:
                    case -71:
                    case -48:

                    case 9:

                    case 191:
                    case 189:
                    case 113:
                    case 93:
                    case 69:
                    case 33:
                    case 17:
                    case 25:
                    case 41:
                    case 49:
                    case 83: {
                        send_dynamic_menu(p, type, "Zosaku",
                                new String[] { "Thách đấu", "Truy nã", "Đồ sát vương", "Lệnh truy nã" },
                                new short[] { 137, 160, 159, 111 });
                        break;
                    }

                    case -72: { // npc nami
                        send_dynamic_menu(p, type, "Nami",
                                new String[] { "Quy Đổi Tiền", "Nhiệm vụ hằng ngày", "Tích lũy nạp", "Tích tiêu ruby",
                                        "Cửa hàng Coin", "Chợ đấu giá", "Chợ mua bán", "Thông tin bản thân",
                                        "Cửa hàng Danh Hiệu" },
                                new short[] { 128, 134, 110, 132, 170, 169, 152, 136, 111 });
                        break;
                    }
                    case -997: {
                        switch (p.map.template.id) {
                            case 1: { // lang coi xay gio
                                send_dynamic_menu(p, type, "Hướng dẫn", new String[] { "Đăng ký tài khoản",
                                        "Nhiệm vụ tân thủ", "Vật phẩm", "Vận buôn", "Trang bị", "Kỹ năng" }, null);
                                break;
                            }
                            case 9: { // thi tran vo so
                                send_dynamic_menu(p, type, "Hướng dẫn",
                                        new String[] { "Bảng xếp hạng", "Nhiệm vụ hàng ngày", "Cường hóa trang bị",
                                                "Khảm đá", "Chuyển hóa", "Săn trùm", "Phó bản liên tầng", "Phó bản PvP",
                                                "Khóa bảo vệ", "Nạp tiền" },
                                        null);
                                break;
                            }
                            case 17: { // thi tran orange
                                send_dynamic_menu(p, type, "Hướng dẫn",
                                        new String[] { "Chợ mua bán", "Vòng xoay kho báu", "Hoàn mỹ", "Kích ẩn",
                                                "Thuộc tính kích ẩn (1-4)", "Thuộc tính kích ẩn (5-8)",
                                                "Thuộc tính kích ẩn (9-13)" },
                                        null);
                                break;
                            }
                            case 25: { // sirup
                                send_dynamic_menu(p, type, "Hướng dẫn", new String[] { "Cường hóa ác quỷ" }, null);
                                break;
                            }
                            case 33: { // barati
                                send_dynamic_menu(p, type, "Hướng dẫn", new String[] { "Băng hải tặc", "Phó bản băng",
                                        "Phó bản khổng lồ", "Bảo vệ pháo đài" }, null);
                                break;
                            }
                            case 41: { // hat de
                                send_dynamic_menu(p, type, "Hướng dẫn",
                                        new String[] { "Bảo vệ kho báu Namie", "Siêu boss" }, null);
                                break;
                            }
                            case 49: { // khoi dau
                                send_dynamic_menu(p, type, "Hướng dẫn", new String[] { "Lệnh truy nã", "Siêu boss" },
                                        null);
                                break;
                            }
                            case 66: { // mom sinh doi
                                send_dynamic_menu(p, type, "Hướng dẫn", new String[] { "Vượt Redline" }, null);
                                break;
                            }
                            case 69: { // whiskey
                                send_dynamic_menu(p, type, "Hướng dẫn",
                                        new String[] { "Trái ác quỷ", "Đấu trường tự do", "Siêu boss" }, null);
                                break;
                            }
                            case 79: { // little grand
                                send_dynamic_menu(p, type, "Hướng dẫn",
                                        new String[] { "Đá đít Mr.3", "Phó bản khổng lồ" }, null);
                                break;
                            }
                        }
                        break;
                    }
                    case -100: {
                        send_dynamic_menu(p, type, "Sự kiện",
                                new String[] { "T/g khóa exp", "Hủy t/g khóa exp", "Tài xỉu" },
                                new short[] { 118, 118, 133 });
                        break;
                    }
                    case -4: {
                        List<String> list = new ArrayList<>();
                        list.add("Học Skill");
                        list.add("Tẩy tiềm năng");
                        list.add("Thông thạo");
                        list.add("Chỉ số ước ngọc rồng");
                        list.add("Mục tiêu săn ngọc");
                        if (activities.Mastery2.canUnlock(p)) {
                            list.add("Thăng Giai");
                        }
                        if (p.hasUnlockedMastery2) {
                            list.add("Thông Thạo 2");
                        }
                        send_dynamic_menu(p, type, "Gap", list.toArray(new String[0]), null);
                        break;
                    }

                    // case -144: // kinh do nuoc
                    case -153:
                    case -152:
                    case -151:
                    case -150:
                    case -149:
                    case -148: {
                        Show_List_Map_Tele(p, 0, -144);
                        break;
                    }
                    // case -124: // thi tran thien su
                    case -131:
                    case -130:
                    case -129:
                    case -128:
                    case -127:
                    case -126:
                    case -125:
                    case -123: {
                        Show_List_Map_Tele(p, 0, -124);
                        break;
                    }
                    case -115:
                    case -114:
                    case -113:
                    case -112:
                    case -111:
                    case -110:
                    case -109:
                    case -108: {
                        Show_List_Map_Tele(p, 0, -107);
                        break;
                    }
                    case -96:
                    case -94:
                    case -93:
                    case -92: {
                        Show_List_Map_Tele(p, 0, -85);
                        break;
                    }
                    case -83:
                    case -81:
                    case -80:
                    case -79: {
                        Show_List_Map_Tele(p, 0, 0);
                        break;
                    }
                    case -59:
                    case -63:
                    case -62:
                    case -61: {
                        Show_List_Map_Tele(p, 0, -60);
                        break;
                    }
                    case -58:
                    case -51:
                    case -50:
                    case -49: {
                        Show_List_Map_Tele(p, 0, -44);
                        break;
                    }
                    case -57:
                    case -42:
                    case -41:
                    case -40: {
                        Show_List_Map_Tele(p, 0, -36);
                        break;
                    }
                    case -56:
                    case -34:
                    case -33:
                    case -32: {
                        Show_List_Map_Tele(p, 0, -28);
                        break;
                    }
                    case -55:
                    case -26:
                    case -25:
                    case -24: {
                        Show_List_Map_Tele(p, 0, -20);
                        break;
                    }
                    case -54:
                    case -18:
                    case -17:
                    case -16: {
                        Show_List_Map_Tele(p, 0, -12);
                        break;
                    }
                    case -53:
                    case -10:
                    case -9:
                    case -8: {
                        // send_dynamic_menu(p, type, "Nhiệm vụ", new String[] {"Nhiệm vụ chính", "Nhiệm
                        // vụ lặp"}, null);
                        Show_List_Map_Tele(p, 0, -5);
                        break;
                    }
                    case -6: {
                        Service.Send_UI_Shop(p, 99);
                        break;
                    }
                    case -145:
                    case -122:
                    case -118:
                    case -103:
                    case -87:
                    case -74:
                    case -67:
                    case -45:
                    case -37:
                    case -31:
                    case -21:
                    case -13:
                    case -1: {
                        send_dynamic_menu(p, type, get_name_npc(type), new String[] { "Thách đấu", "Cao thủ",
                                "Băng hải tặc", "Truy nã", "Quà tặng Giftcode", "TOP Săn Boss", "Thức tỉnh bản thân",
                                "Trùng sinh", "Vô Hạn Liên Tầng", "Bảng xếp hạng VHLT" },
                                new short[] { 101, 102, 103, 103, 135, 103, 102, 102, 167, 102 });
                        break;
                    }
                    case -133: {
                        send_dynamic_menu(p, type, "Kho Báu",
                                new String[] { "Vòng quay kho báu", "Quay nhanh (Nhập SL)", "Hoàn mỹ - Kích ẩn" },
                                null);
                        break;
                    }
                    case -105:
                    case -90:
                    case -70:
                    case -47: {
                        send_dynamic_menu(p, type, "Johny",
                                new String[] { "Kích khuôn trang bị", "Cửa hàng nguyên liệu", "Ghép nguyên liệu",
                                        "Đá khảm trang bị",
                                        "Cường hoá trang bị", "Cường hóa thời trang", "Cường hóa kỹ năng",
                                        "Sổ tay Trái Ác Quỷ" },
                                new short[] { 171, 152, 132, 127, 126, 163, 156, 148 });
                        break;
                    }
                    case -147:
                    case -120:
                    case -116:
                    case -102:
                    case -89:
                    case -76:
                    case -68:
                    case -46:
                    case -39:
                    case -29:
                    case -22:
                    case -14:
                    case -2: {
                        send_dynamic_menu(
                                p, type, get_name_npc(type), new String[] { "Quán ăn", "Tiệm tóc",
                                        "Đóng thuyền", "Thời trang", "Thẩm mỹ viện" },
                                new short[] { 104, 106, 105, 108, 158 });
                        break;
                    }
                    case -144: // kinh do nuoc
                    case -124: // thi tran thien su
                    case -132: // dao jaza
                    case -107: // thi tran nanohano
                    case -85: // thi tran horn
                    case -97: // dao little grand
                    case 0: // thi tran whiskey
                    case -82: // mom sinh doi
                    case -60: // thi tran khoi dau
                    case -44: // lang hat de
                    case -36: // nha hang barati
                    case -28: // lang sirup
                    case -20: // thi tran orang
                    case -12: // thi tran vo so
                    case -5: { // lang coi xay gio
                        send_dynamic_menu(p, type, "", new String[] { "Trong làng", "Thế giới" });
                        break;
                    }
                    case -7: {
                        Menu_Change_Zone(p);
                        break;
                    }
                    case -146:
                    case -121:
                    case -117:
                    case -101:
                    case -88:
                    case -75:
                    case -69:
                    case -38:
                    case -30:
                    case -23:
                    case -15:
                    case -3: {
                        send_dynamic_menu(p, type, get_name_npc(type),
                                new String[] {
                                        (!p.is_show_hat ? "Bật hiển thị nón" : "Tắt hiển thị nón"),
                                        (!p.isPetVisible ? "Bật hiển thị pet" : "Tắt hiển thị pet"),
                                        "Tháo trang bị 2", "Tháo trang bị 3", "Dọn hành trang", "Thùng rác" },
                                new short[] { 117, 145, 109, 109, 125, 113 });
                        break;
                    }

                    case -119: {
                        break;
                    }
                    case -137: {
                        break;
                    }
                    default: {
                        send_dynamic_menu(p, type, (get_name_npc(type) + " id " + type), new String[] { "Chưa có" },
                                new short[] { 117 });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("MenuController.send_menu Exception for User: " + (p != null ? p.name : "unknown"), e);
        }
    }

    private static String get_name_npc(int type) {
        switch (type) {
            case -145:
                return "Icebug";
            case -122:
                return "Gan";
            case -118:
                return "Cricket";
            case -103:
                return "Cobran";
            case -87:
                return "Daltont";
            case -74:
                return "Mr Opera";
            case -67:
                return "Mastersun";
            case -45:
                return "Genzo";
            case -31:
                return "Băng hải tặc nhí";
            case -1:
                return "Trưởng làng";
            case -146:
                return "Paule";
            case -147:
                return "kookoroo";
            case -120:
                return "Conic";
            case -121:
                return "Pagada";
            case -117:
                return "Spect";
            case -116:
                return "Terri";
            case -101:
                return "Kohzak";
            case -102:
                return "Yoshi moto";
            case -89:
                return "Dr Kure";
            case -88:
                return "Stook";
            case -75:
                return "Ms Vivi";
            case -76:
                return "Mr Acrobatic";
            case -68:
                return "Sapie";
            case -46:
                return "Noziko";
            case -39:
                return "Cami";
            case -29:
                return "Kaiya";
            case -21:
                return "Thị Trưởng";
            case -13:
                return "Cobi";
            case -69:
                return "Masu";
            case -3:
                return "Guru";
            case -15:
                return "Mẹ Rita";
            case -23:
                return "Poroy";
            case -30:
                return "Merri";
            case -38:
                return "Partty";
            case -2:
                return "Machiko";
            case -14:
                return "Rita";
            case -22:
                return "Cho Cho";
        }
        return "NPC";
    }

    public static void process_menu(Player p, Message m2) throws IOException {
        try {
            if (!p.isdie) {
                short idNPC = m2.reader().readShort();
                // byte idMenu =
                m2.reader().readByte();
                byte index = m2.reader().readByte();
                // core.GameLogger.info("idNPC " + idNPC);
                // core.GameLogger.info("idMenu " + idMenu);
                // core.GameLogger.info("index " + index);
                switch (idNPC) {
                    case -999: { // Menu De Tu
                        if (p.myDisciple == null)
                            return;
                        if (index == 0) { // Thong tin
                            String clazzName = "Võ sĩ";
                            if (p.myDisciple.clazz == 2)
                                clazzName = "Kiếm khách";
                            else if (p.myDisciple.clazz == 3)
                                clazzName = "Đầu bếp";
                            else if (p.myDisciple.clazz == 4)
                                clazzName = "Hoa tiêu";
                            else if (p.myDisciple.clazz == 5)
                                clazzName = "Xạ thủ";
                            // Tính toán phần trăm level (XX.X%)
                            int lvPercent = p.myDisciple.get_level_percent();
                            String lvPercentStr = (lvPercent / 10) + "." + (lvPercent % 10) + "%";

                            // Lấy kinh nghiệm tối đa của level hiện tại
                            long maxExp = (p.myDisciple.level <= 100)
                                    ? template.Level.ENTRYS[p.myDisciple.level - 1].exp
                                    : template.Level.LEVEL_THONGTHAO[p.myDisciple.thongthao];
                            // Load Reincarnation & Awakening info
                            String info = "--- Đệ tử: " + p.myDisciple.name + " ---\n"
                                    + "Phái: " + clazzName + "\n"
                                    + "Level: " + p.myDisciple.level + " [" + lvPercentStr + "]\n"
                                    + "Trùng sinh: " + p.myDisciple.reincarnation + "\n"
                                    + "Thức tỉnh: " + p.myDisciple.levelAwaken + "\n"
                                    + "Kinh nghiệm: " + Util.number_format(p.myDisciple.exp) + "/"
                                    + Util.number_format(maxExp) + "\n"
                                    + "Tiềm năng: " + Util.number_format(p.myDisciple.pointAttribute) + "\n"
                                    + "HP: " + Util.number_format(p.myDisciple.hp) + "/"
                                    + Util.number_format(p.myDisciple.body.get_hp_max(true)) + "\n"
                                    + "Dame: " + Util.number_format(p.myDisciple.body.get_dame(true)) + "\n"
                                    + "Sức mạnh: " + p.myDisciple.point1 + "\n"
                                    + "Phòng thủ: " + p.myDisciple.point2 + "\n"
                                    + "Thể lực: " + p.myDisciple.point3 + "\n"
                                    + "Tinh thần: " + p.myDisciple.point4 + "\n"
                                    + "Nhanh nhẹn: " + p.myDisciple.point5 + "\n"
                                    + "Kỹ năng: " + p.myDisciple.skill_point.size() + "\n";
                            if (p.myDisciple.skill_point.size() > 0) {
                                info += "--- Danh sách kỹ năng ---\n";
                                for (template.Skill_info sk : p.myDisciple.skill_point) {
                                    if (sk.temp != null) {
                                        // Tính toán phần trăm kỹ năng
                                        int skPercent = sk.get_percent();
                                        String skPercentStr = (skPercent / 10) + "." + (skPercent % 10) + "%";
                                        info += "- " + sk.temp.name + " (Cấp: " + sk.realLevel + "): " + skPercentStr
                                                + "\n";
                                    }
                                }
                            }
                            Service.send_box_ThongBao_OK(p, info);
                        } else if (index == 1) { // Trang bị (UI Mới)
                            p.tempPlayer = p.myDisciple;
                            send_dynamic_menu(p, 9997, "Trang bị đệ tử",
                                    new String[] { "Trang bị 1", "Trang bị 2", "Trang bị 3" }, null);
                        } else if (index == 2) { // Doi trang thai
                            String[] statusNames = new String[] { "Đi theo", "Tấn công quái", "Bảo vệ sư phụ" };
                            send_dynamic_menu(p, -998, "Chọn trạng thái", statusNames);
                        } else if (index == 3) { // Ve nha
                            if (p.myDisciple.status != 2) {
                                p.myDisciple.status = 2;
                                Message m3 = new Message(3);
                                m3.writer().writeShort(p.myDisciple.index_map);
                                m3.writer().writeByte(0);
                                p.map.send_msg_all_p(m3, null, true);
                                m3.cleanup();
                                Service.send_box_ThongBao_OK(p, "Đệ tử đã về nhà nghỉ ngơi.");
                            }
                        } else if (index == 4) { // Trùng sinh đệ tử
                            long currentCost = 1_000_000L + (250_000L * p.myDisciple.reincarnation);
                            Service.send_box_yesno(p, 8892, "XÁC NHẬN",
                                    "BẠN MUỐN TRÙNG SINH CHO ĐỆ TỬ?\nYêu cầu: Cấp 100.\nChi phí: "
                                            + Util.number_format(currentCost)
                                            + " Extol.\nReset cấp độ về 1 và tăng tiềm năng vĩnh viễn.",
                                    new String[] { "Đồng ý", "Đóng" }, new byte[] { 1, -1 });
                        } else if (index == 5) { // Thức tỉnh đệ tử
                            if (p.myDisciple != null) {
                                if (p.myDisciple.levelAwaken >= 2) {
                                    Service.send_box_ThongBao_OK(p, "Đệ tử đã đạt đỉnh cao Thức tỉnh Giai đoạn 2.");
                                    break;
                                }
                                String reqStr = "[YÊU CẦU THỨC TỈNH ĐỆ TỬ]\n\n"
                                        + "Sư phụ cần chuẩn bị:\n"
                                        + "- 1. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[0]) + "\n"
                                        + "- 2. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[1]) + "\n"
                                        + "- 3. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[2]) + "\n"
                                        + "- 4. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[3]) + "\n"
                                        + "- 5. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[4]) + "\n"
                                        + "- 6. " + ItemTemplate4.get_item_name(p.myDisciple.idDF_required[5]) + "\n\n"
                                        + "Điều kiện khác:\n";
                                if (p.myDisciple.levelAwaken == 0) {
                                    reqStr += "• Cần ít nhất 1 bộ trang bị đạt cấp +100.\n• Tỉ lệ: 5%";
                                } else {
                                    reqStr += "• Cần cả 2 bộ trang bị đạt cấp +100.\n• Tỉ lệ: 1%";
                                }
                                Service.send_box_yesno(p, 8891, "XÁC NHẬN", reqStr,
                                        new String[] { "Đồng ý", "Đóng" }, new byte[] { 1, -1 });
                            }
                        } else if (index == 6) { // Dạy đệ dùng TAQ
                            java.util.List<template.ItemTemplate4> taqs = new java.util.ArrayList<>();
                            java.util.List<Integer> taqIds = new java.util.ArrayList<>();
                            for (int i = 0; i < p.item.bag47.size(); i++) {
                                template.ItemBag47 it = p.item.bag47.get(i);
                                if (it != null && it.category == 4) {
                                    // Danh sách ID Trái Ác Quỷ hợp lệ (32-34, 86-88, 90-93, 160-161, 219, 220, 240,
                                    // 316-318, 427, 684)
                                    int tid = it.id;
                                    if (tid == 86 || tid == 87 || tid == 32 || tid == 33 || tid == 34 || tid == 88 ||
                                            tid == 90 || tid == 91 || tid == 92 || tid == 93 || tid == 160 || tid == 161
                                            ||
                                            tid == 219 || tid == 220 || tid == 240 || tid == 316 || tid == 317
                                            || tid == 318 ||
                                            tid == 427 || tid == 684) {
                                        template.ItemTemplate4 it4 = template.ItemTemplate4.get_it_by_id(tid);
                                        if (it4 != null) {
                                            taqs.add(it4);
                                            taqIds.add(tid);
                                        }
                                    }
                                }
                            }
                            if (taqs.isEmpty()) {
                                Service.send_box_ThongBao_OK(p, "Bạn không có Trái Ác Quỷ nào trong túi đồ!");
                            } else {
                                String[] names = new String[taqs.size()];
                                short[] icons = new short[taqs.size()];
                                for (int i = 0; i < taqs.size(); i++) {
                                    names[i] = taqs.get(i).name;
                                    icons[i] = taqs.get(i).icon;
                                }
                                p.tempIdList = taqIds; // Lưu danh sách ID tạm thời
                                send_dynamic_menu(p, 8895, "Chọn Trái Ác Quỷ cho đệ ăn", names, icons);
                            }
                        } else if (index == 7) { // Khôi phục tiềm năng
                            Service.send_box_yesno(p, 8893, "XÁC NHẬN",
                                    "BẠN MUỐN KHÔI PHỤC TIỀM NĂNG ĐỆ TỬ?\n\n"
                                            + "Hệ thống sẽ dọn dẹp các chỉ số bị lỗi, tính toán lại tổng điểm chuẩn dựa trên Cấp độ và Trùng sinh hiện tại để phân bổ lại từ đầu.\n"
                                            + "(Theo chuẩn: " + p.myDisciple.getPotentialPerLevel() + " điểm/cấp)\n\n"
                                            + "Lưu ý: Tổng số điểm có thể giảm nếu đệ tử cày cuốc bằng mốc điểm 1600 cũ.",
                                    new String[] { "Đồng ý", "Đóng" }, new byte[] { 1, -1 });
                        } else if (index == 8) { // Chỉ số chi tiết
                            String info = p.myDisciple.get_detailed_info();
                            Service.send_box_ThongBao_OK(p, info);
                        }
                        break;
                    }

                    case -998: { // Submenu doi trang thai
                        if (p.myDisciple == null)
                            return;
                        if (index == 0)
                            p.myDisciple.status = 0; // Theo
                        else if (index == 1)
                            p.myDisciple.status = 1; // Danh
                        else if (index == 2)
                            p.myDisciple.status = 3; // Bao ve
                        Service.send_box_ThongBao_OK(p, "Đã thay đổi trạng thái đệ tử.");
                        break;
                    }

                    case 9876: {
                        if (index >= 0 && index < devilfruit.DevilFruitManager.SYNERGIES.size()) {
                            devilfruit.DevilFruitManager.DevilFruitSynergy syn = devilfruit.DevilFruitManager.SYNERGIES
                                    .get(index);
                            StringBuilder sb = new StringBuilder();
                            sb.append(syn.synergyName).append("\n\n");
                            sb.append("Yêu cầu khảm:");
                            for (short fid : syn.requiredFruits) {
                                template.ItemTemplate4 it4 = template.ItemTemplate4.get_it_by_id(fid);
                                if (it4 != null) {
                                    sb.append("\n- ").append(it4.name);
                                } else {
                                    sb.append("\n- Trái #").append(fid);
                                }
                            }
                            sb.append("\n\nChỉ số thưởng:");
                            for (devilfruit.DevilFruitManager.DevilFruitOption opt : syn.bonusOptions) {
                                String optName = "Chỉ số #" + opt.optId;
                                if (template.ItemOptionTemplate.ENTRYS != null) {
                                    for (template.ItemOptionTemplate io : template.ItemOptionTemplate.ENTRYS) {
                                        if (io.id == opt.optId) {
                                            optName = io.name;
                                            break;
                                        }
                                    }
                                }
                                sb.append("\n+ ").append(optName).append(": +").append(opt.optValue);
                            }
                            Service.send_box_ThongBao_OK(p, sb.toString());
                        }
                        break;
                    }

                    case 9875: { // Menu lựa chọn: Thông tin trái hay Combo
                        if (index == 0) {
                            // Lựa chọn 1: Thông tin trái ác quỷ
                            java.util.List<devilfruit.DevilFruitManager.DevilFruitData> fruits = new java.util.ArrayList<>(
                                    devilfruit.DevilFruitManager.FRUITS.values());
                            if (fruits.isEmpty()) {
                                Service.send_box_ThongBao_OK(p, "Không tải được dữ liệu trái ác quỷ.");
                                break;
                            }
                            String[] fruitNames = new String[fruits.size()];
                            for (int i = 0; i < fruits.size(); i++) {
                                fruitNames[i] = fruits.get(i).name;
                            }
                            p.tempFruitList = fruits;
                            send_dynamic_menu(p, 9874, "Chọn trái ác quỷ để xem chi tiết", fruitNames, null);
                        } else if (index == 1) {
                            // Lựa chọn 2: Xem Combo ác quỷ (logic cũ)
                            int size = devilfruit.DevilFruitManager.SYNERGIES.size();
                            String[] synergyNames = new String[size];
                            for (int i = 0; i < size; i++) {
                                synergyNames[i] = devilfruit.DevilFruitManager.SYNERGIES.get(i).synergyName;
                            }
                            send_dynamic_menu(p, 9876, "Sổ tay Trái Ác Quỷ (" + size + " Combos)", synergyNames, null);
                        }
                        break;
                    }

                    case 8895: { // Xử lý chọn TAQ từ list cho đệ
                        if (p.myDisciple == null || p.tempIdList == null || index < 0 || index >= p.tempIdList.size())
                            return;
                        int itId = p.tempIdList.get(index);
                        p.tempIdList = null; // Clear
                        // Tái sử dụng logic YesNo 8896 để xác nhận hoặc thực thi
                        p.data_yesno = new int[] { itId };
                        p.pendingFruitItemId = itId;

                        Service.send_box_yesno(p, 8896, "XÁC NHẬN",
                                "Bạn có chắc muốn dùng " + template.ItemTemplate4.get_it_by_id(itId).name
                                        + " cho đệ tử?",
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, 1 });
                        break;
                    }

                    case 1106: { // Xử lý chọn trang bị GR để nâng cấp
                        if (p.tempIndexMapping != null && index >= 0 && index < p.tempIndexMapping.size()) {
                            int realIndex = p.tempIndexMapping.get(index);
                            Item_wear it = p.item.bag3[realIndex];
                            if (it != null) {
                                p.data_yesno = new int[] { realIndex };
                                Service.send_box_yesno(p, 1106, "XÁC NHẬN ĐỔI",
                                        "Bạn có chắc muốn hy sinh " + it.template.name + " (+" + it.levelup + ") " +
                                                "và 20 loại đá để nhận Rương SGR?\n" +
                                                "(Rương sẽ mang theo +" + it.levelup + "% tỉ lệ buff dòng ẩn)",
                                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, -1 });
                            }
                        }
                        break;
                    }

                    case 9874: { // Hiện chi tiết chỉ số của từng trái ác quỷ
                        @SuppressWarnings("unchecked")
                        java.util.List<devilfruit.DevilFruitManager.DevilFruitData> fruitsD = (java.util.List<devilfruit.DevilFruitManager.DevilFruitData>) p.tempFruitList;
                        if (fruitsD != null && index >= 0 && index < fruitsD.size()) {
                            devilfruit.DevilFruitManager.DevilFruitData fruit = fruitsD.get(index);
                            StringBuilder sb = new StringBuilder();
                            sb.append(fruit.name).append("\n\n");
                            sb.append("Chỉ số khi ép vào trang bị:\n");
                            for (devilfruit.DevilFruitManager.DevilFruitOption opt : fruit.options) {
                                String optName = "Chỉ số #" + opt.optId;
                                String unit = "";
                                if (template.ItemOptionTemplate.ENTRYS != null) {
                                    for (template.ItemOptionTemplate io : template.ItemOptionTemplate.ENTRYS) {
                                        if (io.id == opt.optId) {
                                            optName = io.name;
                                            unit = (io.percent != 0) ? "%" : "";
                                            break;
                                        }
                                    }
                                }
                                sb.append("+ ").append(optName).append(": +").append(opt.optValue).append(unit)
                                        .append("\n");
                            }
                            Service.send_box_ThongBao_OK(p, sb.toString());
                        }
                        break;
                    }

                    case 8888: {
                        if (p.tempIndexMapping != null && index >= 0 && index < p.tempIndexMapping.size()) {
                            int realIndex = p.tempIndexMapping.get(index);
                            Item_wear it = p.item.bag3[realIndex];
                            if (it != null) {
                                p.data_yesno = new int[] { realIndex };
                                Service.send_box_yesno(p, 8887, "Thông báo",
                                        "Xác nhận dùng Bùa cường hóa nâng cấp " + it.template.name
                                                + "?\n(Đảm bảo thành công 100%)",
                                        new String[] { "Có", "Không" }, new byte[] { -1, -1 });
                            }
                        }
                        break;
                    }

                    case 9878: {

                        if (p.tempIndexMapping != null && index >= 0 && index < p.tempIndexMapping.size()) {
                            int realIndex = p.tempIndexMapping.get(index);
                            Item_wear it = p.item.bag3[realIndex];
                            if (it != null) {
                                p.item_to_kham_ngoc = it;
                                Service.send_box_yesno(p, 9877, "Thông báo",
                                        "Xác nhận Kích khuôn " + it.template.name
                                                + "?\nChi phí: "
                                                + Util.number_format(activities.Rebuild_Item.COST_BERRY_KICH_KHUON)
                                                + " Beri, "
                                                + Util.number_format(activities.Rebuild_Item.COST_RUBY_KICH_KHUON)
                                                + " Ruby, "
                                                + Util.number_format(activities.Rebuild_Item.COST_EXTOL_KICH_KHUON)
                                                + " Extol",
                                        new String[] { "Có", "Không" }, new byte[] { -1, -1 });
                            }
                        }
                        break;
                    }

                    case -99: {
                        if (index == 0) {
                            Menu_Machiko(p, index);
                        } else {
                            Vgo vgo = new Vgo();
                            try {
                                vgo.map_go = Map.get_map_by_id(p.id_map_save);
                                for (int i = 0; i < vgo.map_go[0].template.npcs.size(); i++) {
                                    Npc npc_temp = vgo.map_go[0].template.npcs.get(i);
                                    if (npc_temp.namegt.contains("Zosaku")) {
                                        vgo.xnew = npc_temp.x;
                                        if (npc_temp.y < 250) {
                                            vgo.ynew = (short) (npc_temp.y + 20);
                                        } else {
                                            vgo.ynew = (short) (npc_temp.y - 40);
                                        }
                                        break;
                                    }
                                }
                                if (vgo.xnew == 0 || vgo.ynew == 0) {
                                    vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                                    vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                                }
                            } catch (Exception e) {
                                core.GameLogger.error("MenuController.Menu_Machiko: Error finding Zosaku NPC", e);
                                vgo.map_go = Map.get_map_by_id(1);
                                vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                                vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                            }
                            p.goto_map(vgo);
                            p.type_pk = -1;
                        }
                        break;
                    }

                    case -776: {
                        switch (index) {
                            case 0: {
                                p.data_yesno = new int[] { 64 };
                                Service.send_box_yesno(p, 64, "Thông báo", "Bạn có muốn vào phó bản hang động ?",
                                        new String[] { "Đồng ý", "Huỷ" }, new byte[] { 2, 1 });
                                break;
                            }
                            case 1: {
                                UpgradeDevil.show_table(p, 6);
                                break;
                            }
                            case 2: {
                                UpgradeDevil.show_table(p, 8);
                                break;
                            }
                            case 3: {
                                send_dynamic_menu(p, -783, "Tạo lõi năng lượng",
                                        new String[] { "Tạo lõi ánh sáng", "Tạo lõi tình yêu", "Tạo lõi cao su" },
                                        null);
                                break;
                            }
                            case 4: {
                                UpgradeDial.show_table(p);
                                break;
                            }
                            case 5: {
                                BXH.send(p, 10, 0);
                                break;
                            }
                            case 6: { // Ghép Dial GR
                                Service.send_box_yesno(p, 882, "Ghép Dial GR",
                                        "Bạn có muốn ghép Dial GR?\n"
                                                + "- 100 Khoáng thạch vàng\n"
                                                + "- Chi phí: 5.000.000 Extol\n"
                                                + "Tỉ lệ thành công: 100%",
                                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, -1 });
                                break;
                            }
                        }
                        break;
                    }

                    case -666: {
                        if (p.oldMapId != -1) {
                            Map[] map_go = Map.get_map_by_id(p.oldMapId);
                            Vgo vgo = new Vgo();
                            vgo.map_go = map_go;
                            vgo.xnew = p.oldX;
                            vgo.ynew = p.oldY;
                            p.goto_map(vgo);
                        } else {
                            Map[] map_go = Map.get_map_by_id(1);
                            Vgo vgo = new Vgo();
                            vgo.map_go = map_go;
                            vgo.xnew = (short) Util.random(200, 700);
                            vgo.ynew = (short) Util.random(200, 280);
                            p.goto_map(vgo);
                        }
                        break;
                    }

                    case -777: {
                        switch (index) {
                            case 0: {
                                int[] id = new int[] { 1051, 1052, 1053, 1054, 1055 };
                                String[] name = new String[id.length];
                                short[] icon = new short[id.length];
                                for (int i = 0; i < id.length; i++) {
                                    ItemTemplate4 temp = ItemTemplate4.get_it_by_id(id[i]);
                                    name[i] = temp.name;
                                    icon[i] = (short) id[i];
                                }
                                send_dynamic_menu(p, -782, "Chế Tạo Trang Bị", name, icon, 4);
                                break;
                            }
                            case 1: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(307);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 2: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(308);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 3: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(300);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 4: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(309);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 5: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(304);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 350;
                                vgo.ynew = 250;
                                p.goto_map(vgo);
                                break;
                            }
                            case 6: { // Chế tạo trang bị GR
                                Service.send_box_yesno(p, 881, "Chế tạo Trang bị GR",
                                        "Bạn có muốn chế tạo Trang bị GR?\n"
                                                + "- 100 Đá sao hỏa\n"
                                                + "- 100 Đá mặt trăng\n"
                                                + "- 100 Đá khổng tước\n"
                                                + "- Chi phí: 1.000.000 Extol\n"
                                                + "Tỉ lệ thành công: 100%",
                                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, -1 });
                                break;
                            }
                            case 7: { // Đảo Bánh Kem 2
                                int countSkillSkill300 = 0;
                                for (Skill_info sk : p.skill_point) {
                                    if (sk != null && sk.temp != null && sk.temp.ID >= 0 && sk.temp.ID <= 2
                                            && sk.temp.Lv_RQ >= 300) {
                                        countSkillSkill300++;
                                    }
                                }
                                if (countSkillSkill300 >= 3) {
                                    p.saveOldMap();
                                    Map[] map_go = Map.get_map_by_id(315);
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = map_go;
                                    vgo.xnew = 200;
                                    vgo.ynew = 200;
                                    p.goto_map(vgo);
                                } else {
                                    Service.send_box_ThongBao_OK(p,
                                            "Bạn cần cả 3 kỹ năng đạt cấp 300 trở lên để vào đây!");
                                }
                                break;
                            }
                        }
                        break;
                    }

                    case -782: {
                        switch (index) {
                            case 0: {
                                UpgradeDevil.show_table(p, 11);
                                break;
                            }
                            case 1: {
                                UpgradeDevil.show_table(p, 12);
                                break;
                            }
                            case 2: {
                                UpgradeDevil.show_table(p, 13);
                                break;
                            }
                            case 3: {
                                UpgradeDevil.show_table(p, 14);
                                break;
                            }
                            case 4: {
                                UpgradeDevil.show_table(p, 15);
                                break;
                            }
                        }
                        break;
                    }

                    case -783: {
                        switch (index) {
                            case 0: {
                                UpgradeDevil.show_table(p, 16);
                                break;
                            }
                            case 1: {
                                UpgradeDevil.show_table(p, 17);
                                break;
                            }
                            case 2: {
                                UpgradeDevil.show_table(p, 18);
                                break;
                            }
                        }
                        break;
                    }

                    case -784: {
                        switch (index) {
                            case 0: {
                                ItemDropTool.showTuTienMenu(p);
                                break;
                            }

                            case 1: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(310);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 200;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 2: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(311);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 200;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 3: {
                                long remaining = BossEvent.BossEvent.getInstance().getRemainingCooldown();
                                if (remaining > 0) {
                                    Service.send_box_ThongBao_OK(p, "Kaido đang hồi phục sức mạnh... Quay lại sau "
                                            + (remaining / 60000 + 1) + " phút nữa!");
                                    break;
                                }
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(BossEvent.BossEvent.BOSS_EVENT_MAP_ID);
                                if (map_go != null && map_go.length > 0) {
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = map_go;
                                    vgo.xnew = 200;
                                    vgo.ynew = 200;
                                    p.goto_map(vgo);
                                }
                                break;
                            }
                            case 4: { // Bãi Farm Đá 2
                                boolean has0 = false, has1 = false, has2 = false;
                                for (Skill_info sk : p.skill_point) {
                                    if (sk != null && sk.temp != null && sk.temp.Lv_RQ >= 500) {
                                        if (sk.temp.ID == 0)
                                            has0 = true;
                                        else if (sk.temp.ID == 1)
                                            has1 = true;
                                        else if (sk.temp.ID == 2)
                                            has2 = true;
                                    }
                                }
                                if (has0 && has1 && has2) {
                                    p.saveOldMap();
                                    Map[] map_go = Map.get_map_by_id(316);
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = map_go;
                                    vgo.xnew = 200;
                                    vgo.ynew = 200;
                                    p.goto_map(vgo);
                                } else {
                                    Service.send_box_ThongBao_OK(p,
                                            "Bạn cần cả 3 kỹ năng đạt cấp 500 trở lên để vào đây!");
                                }
                                break;
                            }
                        }
                        break;
                    }

                    case -888: {
                        switch (index) {
                            case 0: {
                                LenhTruyNa.sendList(p);
                                break;
                            }
                            case 1: {
                                Service.input_text(p, 24, "Phát lệnh truy nã",
                                        new String[] { "Tên truy nã", "Treo thưởng (Beri)" });
                                break;
                            }
                            case 2: {
                                LenhTruyNa.showTargetInfo(p);
                                break;
                            }
                        }
                        break;
                    }

                    case -443: {
                        switch (index) {
                            case 0: {
                                if (p.typePirate == 0) {
                                    if (p.ship_pet == null) {
                                        Service.send_box_yesno(p, 50, "Thông báo",
                                                "Giao hàng đến cho Croket\n- Bãi biển hoang sơ -\nPhần thưởng 10,000,000 Beri",
                                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 6, -1 });
                                    } else {
                                        Service.send_box_ThongBao_OK(p, "Bạn đang có hàng rồi");
                                    }
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Bạn không phải là người lái buôn");
                                }
                                break;
                            }
                            case 1: {
                                Message m = new Message(-20);
                                m.writer().writeByte(1);
                                m.writer().writeShort(985); // id npc
                                m.writer().writeByte(0); // id menu
                                m.writer().writeUTF("Đăng Ký Phe");
                                m.writer().writeByte(4);
                                String[] name = new String[] { "Buôn Hàng", "Bảo Vệ Hàng", "Cướp Hàng", "Không Chọn" };
                                for (int i = 0; i < 4; i++) {
                                    m.writer().writeUTF(name[i]);
                                    m.writer().writeByte(0);
                                    if (p.typePirate == i) {
                                        m.writer().writeByte(3);
                                    } else {
                                        m.writer().writeByte(7);
                                    }
                                }
                                p.addmsg(m);
                                m.cleanup();
                                break;
                            }
                            case 2: {
                                if (p.ship_pet != null) {
                                    if (p.ship_pet.map != null) {
                                        p.ship_pet.map.remove_obj(p.ship_pet.index_map, 0);
                                    }
                                    Ship_pet.remv(p.ship_pet);
                                    p.ship_pet = null;
                                    Service.send_box_ThongBao_OK(p, "Hủy hàng thành công");
                                }
                                break;
                            }
                        }
                        break;
                    }

                    case -444: {
                        switch (index) {
                            case 0: {
                                if (p.ship_pet == null) {
                                    Service.send_box_ThongBao_OK(p, "Hãy lấy hàng ở Thương nhân\n- Rừng làng -");
                                    return;
                                }
                                if (!p.ship_pet.map.equals(p.map)
                                        || !(Math.abs(p.ship_pet.x - p.x) < 150
                                                && Math.abs(p.ship_pet.y - p.y) < 150)) {
                                    Service.send_box_ThongBao_OK(p, "Ta không thấy hàng của ngươi");
                                    return;
                                }
                                Message m = new Message(3);
                                m.writer().writeShort(p.ship_pet.index_map);
                                m.writer().writeByte(2);
                                for (int i = 0; i < p.map.players.size(); i++) {
                                    Player p0 = p.map.players.get(i);
                                    p0.addmsg(m);
                                    if (p0.typePirate == 1) {
                                        List<GiftBox> listGiftP0 = new ArrayList<>();
                                        GiftBox.addGiftBox(listGiftP0, 0, 4, 2_000_000);
                                        Service.send_gift(p0, 0, "Bảo Vệ Hàng", "Bảo Vệ " + p.name, listGiftP0, true);
                                        p0.updateRuby0(2);
                                    }
                                }
                                m.cleanup();
                                Ship_pet.remv(p.ship_pet);
                                p.ship_pet = null;
                                List<GiftBox> listGiftP = new ArrayList<>();
                                GiftBox.addGiftBox(listGiftP, 0, 4, 10_000_000);
                                GiftBox.addGiftBox(listGiftP, 1106, 4, 1);
                                Service.send_gift(p, 0, "Vận Chuyển Hàng", "Trả Hàng Thành Công", listGiftP, true);
                                p.updateRuby0(10);
                                break;
                            }
                        }
                        break;
                    }

                    case -778: {
                        switch (index) {
                            case 0: {
                                synchronized (MenuController.class) {
                                    long currentTime = System.currentTimeMillis();
                                    if (currentTime - lastPrayTime < 60_000) {
                                        long remainingTime = (60_000 - (currentTime - lastPrayTime)) / 1000;
                                        Message m = new Message(17);
                                        m.writer().writeShort(-778);
                                        m.writer().writeByte(2);
                                        m.writer().writeUTF(
                                                "Bạn phải chờ " + remainingTime + " giây nữa để tiếp tục cầu nguyện");
                                        p.addmsg(m);
                                        m.cleanup();
                                        break;
                                    }
                                    lastPrayTime = currentTime;
                                    List<GiftBox> listGift = new ArrayList<>();
                                    int soberi = Util.random(100_000, 500_000);
                                    int soruby = Util.random(200, 500);
                                    {
                                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(0);
                                        GiftBox giftBox = new GiftBox();
                                        giftBox.id = (short) 0;
                                        giftBox.type = 4;
                                        giftBox.name = itemTemplate4.name;
                                        giftBox.icon = itemTemplate4.icon;
                                        giftBox.num = soberi;
                                        giftBox.color = 0;
                                        listGift.add(giftBox);
                                    }
                                    {
                                        ItemTemplate4 itemTemplate4 = ItemTemplate4.get_it_by_id(1);
                                        GiftBox giftBox = new GiftBox();
                                        giftBox.id = (short) 1;
                                        giftBox.type = 4;
                                        giftBox.name = itemTemplate4.name;
                                        giftBox.icon = itemTemplate4.icon;
                                        giftBox.num = soruby;
                                        giftBox.color = 0;
                                        listGift.add(giftBox);
                                    }
                                    Service.send_gift(p, 0, "Cầu nguyện", "", listGift, true);
                                    Message m = new Message(-15);
                                    m.writer().writeByte(20); // thả đèn
                                    m.writer().writeShort(-778); // id npc
                                    m.writer().writeByte(2); // byte npc
                                    m.writer().writeShort(50);
                                    p.map.send_msg_all_p(m, p, true);
                                    m.cleanup();
                                    Map.sendChatNpc((short) -778, "Cảm ơn " + p.name + " đã cầu nguyện cho cả làng", 1);
                                    break;
                                }
                            }
                            case 1: {
                                DiemDanh.showTable(p);
                                break;
                            }
                            case 2: {
                                String box = null;
                                try (Connection conn = SQL.gI().getCon();
                                        PreparedStatement ps = conn.prepareStatement(
                                                "SELECT `box` FROM `accounts` WHERE `user` = ? LIMIT 1")) {
                                    ps.setString(1, p.conn.user);
                                    ResultSet rs = ps.executeQuery();
                                    if (rs.next()) {
                                        box = rs.getString("box");
                                    }
                                    rs.close();
                                } catch (SQLException e) {
                                    GameLogger.error("MenuController: SQL error querying box items for " + p.name, e);
                                    Service.send_box_ThongBao_OK(p, "Đã xảy ra lỗi khi truy vấn dữ liệu.");
                                    return;
                                }
                                if (box == null || box.trim().isEmpty() || box.length() < 5) {
                                    Service.send_box_ThongBao_OK(p, "Bạn không có quà nào để nhận.");
                                    return;
                                }
                                List<GiftBox> fullGiftList = new ArrayList<>();
                                try {
                                    box = box.replace("],[", "]~[");
                                    String[] items = box.substring(2, box.length() - 2).split("~");

                                    for (String item : items) {
                                        String[] data = item.replace("[", "").replace("]", "").split(",");
                                        int type = Integer.parseInt(data[0]);
                                        int id = Integer.parseInt(data[1]);
                                        int quantity = Integer.parseInt(data[2]);

                                        GiftBox gb = new GiftBox();
                                        gb.type = (byte) type;
                                        gb.id = (short) id;
                                        gb.num = quantity;

                                        if (type == 3) {
                                            ItemTemplate3 it = ItemTemplate3.get_it_by_id(id);
                                            if (it != null) {
                                                gb.name = it.name;
                                                gb.icon = it.icon;
                                                gb.color = it.color;
                                            }
                                        } else if (type == 4) {
                                            ItemTemplate4 it = ItemTemplate4.get_it_by_id(id);
                                            if (it != null) {
                                                gb.name = it.name;
                                                gb.icon = it.icon;
                                                gb.color = 0;
                                            }
                                        } else if (type == 7) {
                                            ItemTemplate7 it = ItemTemplate7.get_it_by_id(id);
                                            if (it != null) {
                                                gb.name = it.name;
                                                gb.icon = it.icon;
                                                gb.color = 0;
                                            }
                                        }

                                        fullGiftList.add(gb);
                                    }
                                } catch (Exception e) {
                                    GameLogger.error("MenuController: Error parsing gift box data for " + p.name, e);
                                    Service.send_box_ThongBao_OK(p, "Dữ liệu quà không hợp lệ.");
                                    return;
                                }
                                if (fullGiftList.isEmpty()) {
                                    Service.send_box_ThongBao_OK(p, "Không tìm thấy vật phẩm hợp lệ.");
                                    return;
                                }
                                int takeCount = Math.min(5, fullGiftList.size());
                                List<GiftBox> giftToSend = fullGiftList.subList(0, takeCount);
                                List<GiftBox> remainingGift = fullGiftList.subList(takeCount, fullGiftList.size());
                                if (p.item.able_bag() < giftToSend.size()) {
                                    Service.send_box_ThongBao_OK(p,
                                            "Hành trang phải chừa ít nhất " + giftToSend.size() + " ô trống.");
                                    return;
                                }
                                Service.send_gift(p, 1, "Hòm Quà", "", giftToSend, true);
                                if (remainingGift.isEmpty()) {
                                    try (Connection conn = SQL.gI().getCon();
                                            PreparedStatement ps = conn.prepareStatement(
                                                    "UPDATE `accounts` SET `box` = '' WHERE `user` = ?")) {
                                        ps.setString(1, p.conn.user);
                                        ps.executeUpdate();
                                    } catch (SQLException e) {
                                        GameLogger.error("MenuController: SQL error clearing gift box for " + p.name,
                                                e);
                                    }
                                } else {
                                    StringBuilder sb = new StringBuilder("[");
                                    for (GiftBox gb : remainingGift) {
                                        sb.append("[").append(gb.type).append(",").append(gb.id).append(",")
                                                .append(gb.num)
                                                .append("],");
                                    }
                                    sb.setLength(sb.length() - 1);
                                    sb.append("]");
                                    try (Connection conn = SQL.gI().getCon();
                                            PreparedStatement ps = conn
                                                    .prepareStatement(
                                                            "UPDATE `accounts` SET `box` = ? WHERE `user` = ?")) {
                                        ps.setString(1, sb.toString());
                                        ps.setString(2, p.conn.user);
                                        ps.executeUpdate();
                                    } catch (SQLException e) {
                                        GameLogger.error(
                                                "MenuController: SQL error updating partial gift box for " + p.name, e);
                                    }
                                }
                                break;
                            }

                            case 3: {
                                List<String> danhHieuList = new ArrayList<>();
                                List<Integer> indexMapping = new ArrayList<>();
                                p.danh_hieu.sort((dh1, dh2) -> Integer.compare(dh1.id, dh2.id));
                                for (int i = 0; i < p.danh_hieu.size(); i++) {
                                    MyDanhHieu myDanhHieu = p.danh_hieu.get(i);
                                    DanhHieu danhHieuTemplate = DanhHieu.getTemplate(myDanhHieu.id);
                                    if (danhHieuTemplate != null) {
                                        String name = danhHieuTemplate.name;
                                        String status = myDanhHieu.isUsed ? "[Tắt]" : "[Bật]";
                                        danhHieuList.add(name + " " + status);
                                        indexMapping.add(i);
                                    }
                                }
                                if (danhHieuList.isEmpty()) {
                                    danhHieuList.add("Bạn chưa có danh hiệu nào.");
                                }
                                send_dynamic_menu(p, -890, "Danh Hiệu", danhHieuList.toArray(new String[0]), null);
                                p.tempIndexMapping = indexMapping;
                                break;
                            }
                        }
                        break;
                    }

                    case -890: { // Click vào danh hiệu trong danh sách sở hữu
                        if (index >= 0 && index < p.tempIndexMapping.size()) {
                            int originalIndex = p.tempIndexMapping.get(index);
                            p.id_select_temp = originalIndex; // Lưu index để xử lý tiếp
                            send_dynamic_menu(p, -891, "Tùy chọn Danh Hiệu",
                                    new String[] { "Xem thông tin", "Bật / Tắt", "Đóng" }, null);
                        } else {
                            Service.send_box_ThongBao_OK(p, "Lựa chọn không hợp lệ.");
                        }
                        break;
                    }

                    case -891: { // Xử lý menu Tùy chọn Danh Hiệu
                        if (p.id_select_temp >= 0 && p.id_select_temp < p.danh_hieu.size()) {
                            MyDanhHieu selectedDanhHieu = p.danh_hieu.get(p.id_select_temp);
                            if (selectedDanhHieu != null) {
                                switch (index) {
                                    case 0: { // Xem thông tin
                                        DanhHieu template = DanhHieu.getTemplate(selectedDanhHieu.id);
                                        if (template != null) {
                                            StringBuilder sb = new StringBuilder("Tên: " + template.name);
                                            if (!template.options.isEmpty()) {
                                                sb.append("\nChỉ số:");
                                                for (Option op : template.options) {
                                                    ItemOptionTemplate opTemp = ItemOptionTemplate.get_temp(op.id);
                                                    if (opTemp != null) {
                                                        sb.append("\n- ").append(opTemp.name).append(": +")
                                                                .append(op.getParam());
                                                        if (opTemp.percent == 1)
                                                            sb.append("%");
                                                    }
                                                }
                                            } else {
                                                sb.append("\nKhông có chỉ số cộng thêm.");
                                            }
                                            Service.send_box_ThongBao_OK(p, sb.toString());
                                        }
                                        break;
                                    }
                                    case 1: { // Bật / Tắt
                                        if (selectedDanhHieu.isUsed) {
                                            selectedDanhHieu.isUsed = false;
                                            Service.send_box_ThongBao_OK(p, "Danh hiệu đã được tắt.");
                                        } else {
                                            for (MyDanhHieu myDanhHieu : p.danh_hieu) {
                                                if (myDanhHieu.isUsed) {
                                                    myDanhHieu.isUsed = false;
                                                }
                                            }
                                            selectedDanhHieu.isUsed = true;
                                            Service.send_box_ThongBao_OK(p, "Đã bật danh hiệu.");
                                        }
                                        p.body.setDirty();
                                        p.update_info_to_all();
                                        break;
                                    }
                                }
                            }
                        }
                        p.id_select_temp = -1;
                        p.tempIndexMapping.clear();
                        break;
                    }

                    case 973: {
                        if (index == 0) {
                            BXH.send(p, 8, 0);
                        } else if (index == 1) {
                            Player.NhanQuaTopBoss(p);
                        }
                        break;
                    }

                    case 978: {
                        if (index == 0) {
                            HanhTrinh.show_table(p, 1);
                        } else if (index == 1) {
                            HanhTrinh.show_table(p, 0);
                        } else if (index == 2) {
                            Service.send_box_ThongBao_OK(p,
                                    "Mỗi khi bạn tiêu diệt boss cuối mỗi làng sẽ có 10% cơ hội nhận được đá hành trình. "
                                            + "Gặp trưởng làng xem đá kiếm được");
                        }
                        break;
                    }
                    case 979: {
                        Set<Integer> iconsSold = Clan.get_icons_sold(); // Lấy danh sách các biểu tượng đã được mua
                        if (index == 0) {
                            Message m = new Message(-19); // hiển thị biểu tượng chọn bảng
                            m.writer().writeByte(97);
                            m.writer().writeUTF("Cửa hàng biểu tượng thường");
                            m.writer().writeByte(107);

                            // Danh sách các ID biểu tượng cần hiển thị
                            int[] iconIds = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                                    22,
                                    23,
                                    24, 26, 27,
                                    28, 29, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 76, 78, 79, 80, 81,
                                    82,
                                    83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99,
                                    100, 101, 102, 103,
                                    104, 126, 136, 137,
                                    138, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 169, 171,
                                    178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193,
                                    194, 204, 235, 248, 268, 269, 270, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313,
                                    314, 315, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 405, 406, 407, 408, 409,
                                    410, 412, 413, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509,
                                    510, 511, 512, 513, 514, 515, 516, 517, 518, 519,
                                    520, 521, 522, 523, 524, 525, 526, 527, 528, 529,
                                    530, 531, 532, 533, 534, 535, 536, 537, 538, 539,
                                    540, 541, 542, 543, 544, 545, 546, 547, 548, 549,
                                    550, 551, 552 };

                            int availableCount = 0; // Đếm số biểu tượng có sẵn để hiển thị
                            for (int id : iconIds) {
                                if (iconsSold.contains(id)) {
                                    continue; // Bỏ qua các biểu tượng đã được bán
                                }
                                availableCount++;
                            }

                            m.writer().writeShort(availableCount); // Số lượng biểu tượng có sẵn để hiển thị

                            for (int id : iconIds) {
                                if (iconsSold.contains(id)) {
                                    continue; // Bỏ qua các biểu tượng đã được bán
                                }
                                m.writer().writeShort(id);
                                m.writer().writeShort(id);
                                m.writer().writeUTF("Huy hiệu " + (id + 1));
                                m.writer().writeUTF("Được làm từ gì đấy không biết nữa, mua đeo vào rất đẹp");
                                if (id < 10) {
                                    m.writer().writeShort(0);
                                } else {
                                    m.writer().writeShort(2000);
                                }
                            }
                            p.addmsg(m);
                            m.cleanup();
                        } else if (index == 1) {
                            Message m = new Message(-19); // hiển thị biểu tượng chọn bảng
                            m.writer().writeByte(97);
                            m.writer().writeUTF("Cửa hàng biểu tượng cao cấp");
                            m.writer().writeByte(107);

                            // Danh sách các ID biểu tượng cần hiển thị
                            int[] iconIds = { 273, 275, 276, 296, 297, 298, 299, 300, 301, 302, 383,
                                    385, 387, 388, 389, 390, 391, 393, 395, 396, 397, 398, 316, 317, 318, 319, 320, 321,
                                    322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 350, 351, 352, 353,
                                    354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365,
                                    366, 367, 368, 369, 400, 401, 402, 403, 404, 411, 414, 415, 416, 417, 418, 419 };

                            int availableCount = 0; // Đếm số biểu tượng có sẵn để hiển thị
                            for (int id : iconIds) {
                                if (iconsSold.contains(id)) {
                                    continue; // Bỏ qua các biểu tượng đã được bán
                                }
                                availableCount++;
                            }

                            m.writer().writeShort(availableCount); // Số lượng biểu tượng có sẵn để hiển thị

                            for (int id : iconIds) {
                                if (iconsSold.contains(id)) {
                                    continue; // Bỏ qua các biểu tượng đã được bán
                                }
                                m.writer().writeShort(id);
                                m.writer().writeShort(id);
                                m.writer().writeUTF("Huy hiệu " + (id + 1));
                                m.writer().writeUTF("Được làm từ gì đấy không biết nữa, mua đeo vào rất đẹp");
                                if (id < 10) {
                                    m.writer().writeShort(0);
                                } else {
                                    // Xác định giá dựa trên id
                                    int price = 200;
                                    if (id >= 273 && id < 304) {
                                        price = 5000;
                                    } else if (id >= 380 && id < 399) {
                                        price = 12000;
                                    } else if ((id >= 316 && id <= 334) || (id >= 350 && id <= 369)
                                            || (id >= 400 && id <= 404) || id == 411 || (id >= 414 && id <= 419)) {
                                        price = 30000;
                                    }

                                    m.writer().writeShort((short) price);
                                }
                            }
                            p.addmsg(m);
                            m.cleanup();
                        }
                        break;

                    }

                    case 980: {
                        switch (index) {
                            case 0:
                            case 1:
                            case 2: {
                                UpgradeDevil.show_table(p, index + 3);
                                break;
                            }
                            case 3: {
                                UpgradeDial.show_table(p);
                                break;
                            }
                            case 4: {
                                Rebuild_Item.show_table(p, 10);
                                break;
                            }
                        }
                        break;
                    }
                    case 982: {
                        if (p.ship_pet != null && p.name_ThoSanHaiTac != null
                                && index < p.name_ThoSanHaiTac.length) {
                            Player p0 = Map.get_player_by_name_allmap(p.name_ThoSanHaiTac[index]);
                            if (p0 != null && p0.name_ThoSanHaiTac == null) {
                                p0.name_ThoSanHaiTac = new String[] { p.name };
                                Service.send_box_yesno(p0, 53, "Thông báo",
                                        p.name + " muốn mời bạn bảo vệ hàng, bạn hãy trả lời?",
                                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                                Service.send_box_ThongBao_OK(p, "Gửi yêu cầu thành công, đợi đối phương xác nhận");
                            } else {
                                Service.send_box_ThongBao_OK(p, "Đối phương đã rời đi, hãy thử lại");
                            }
                        } else {
                            Service.send_box_ThongBao_OK(p, "Hãy nhận hàng trước");
                        }
                        break;
                    }

                    case 965: {
                        if (index == 0) {
                            boolean check_tt_tp = false;
                            for (int i = 0; i < p.clan.members.size(); i++) {
                                if (p.clan.members.get(i).name.equals(p.name)
                                        && (p.clan.members.get(i).levelInclan == 0
                                                || p.clan.members.get(i).levelInclan == 1)) {
                                    check_tt_tp = true;
                                    break;
                                }
                            }

                            if (!WorldWar.isRegisted(p.clan)) {
                                if (check_tt_tp) {
                                    int time_h = LocalTime.now().getHourOfDay();
                                    // if (time_h == 17 || time_h == 18 || (time_h == 19 && time_m < 30)) {
                                    if (!(time_h == 21)) {
                                        int flagRd = WorldWar.getFlagRd();
                                        WorldWar.register(p.clan, flagRd);
                                        Service.send_box_ThongBao_OK(p,
                                                "Đăng ký thành công. Kết quả: " + WorldWar.getFlagName(p.clan));
                                        for (int i = 0; i < p.clan.members.size(); i++) {
                                            WorldWar.registerP(p.clan.members.get(i).name, flagRd);
                                            // Player p0 = Map.get_player_by_name_allmap(p.clan.members.get(i).name);
                                            // if (p0 != null) {
                                            // p0.map.change_flag(p0, -1); // reset flag world war
                                            // }
                                        }
                                    } else {
                                        Service.send_box_ThongBao_OK(p,
                                                "Thời gian đăng ký phải trước 21h đêm mỗi ngày");
                                    }
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Bạn không phải thuyền trưởng");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p,
                                        "Đã đăng ký phe đại chiến rồi! Phe đăng ký là " + WorldWar.getFlagName(p.clan));
                            }
                        } else if (index == 1) {
                            int time_h = LocalTime.now().getHourOfDay();

                            if (p.worldWarInfo != null) {
                                if (time_h == 21) {
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = new Map[1];
                                    vgo.map_go[0] = WorldWar.getMap(p.level / 10);
                                    vgo.xnew = 350;
                                    vgo.ynew = 260;
                                    p.goto_map(vgo);
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Đại chiến thế giới chưa bắt đầu");
                                }
                            } else {
                                Service.send_box_ThongBao_OK(p, "Băng của bạn chưa đăng ký");
                            }
                        } else if (index == 2) {
                        } else if (index == 3) {
                            Service.send_box_yesno(p, 67, "Thông báo", "Xác nhận dùng 5 ruby để hồi 5 điểm hạ gục?",
                                    new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                        }
                        break;
                    }

                    case 984: {
                        if (p.clan != null) {
                            switch (index) {
                                case 0: { // dang ky pho ban khong lo
                                    if (p.clan.map_create != null) { // enter map
                                        //
                                        if (p.clan.map_create.map_little_garden.clan1.equals(p.clan)) {
                                            p.type_pk = 4;
                                        } else {
                                            p.type_pk = 5;
                                        }
                                        //
                                        Vgo vgo = new Vgo();
                                        vgo.map_go = new Map[1];
                                        vgo.map_go[0] = p.clan.map_create;
                                        vgo.xnew = 350;
                                        vgo.ynew = 260;
                                        p.goto_map(vgo);
                                    } else {
                                        boolean check_tt_tp = false;
                                        for (int i = 0; i < p.clan.members.size(); i++) {
                                            if (p.clan.members.get(i).name.equals(p.name)
                                                    && (p.clan.members.get(i).levelInclan == 0)) {
                                                check_tt_tp = true;
                                                break;
                                            }
                                        }
                                        if (check_tt_tp) {
                                            if (p.tableTickOption == null) {
                                                int time_h = LocalTime.now().getHourOfDay();
                                                if (time_h == 20 || time_h == 21) {
                                                    p.tableTickOption = new TableTickOption();
                                                    p.tableTickOption.listP = new ArrayList<>();
                                                    p.tableTickOption.idDialog = 0;
                                                    p.tableTickOption.listP.add(p);
                                                    for (int i = 0; i < p.clan.members.size(); i++) {
                                                        Player p0 = Map
                                                                .get_player_by_name_allmap(p.clan.members.get(i).name);
                                                        if (p0 != null && p0.index_map != p.index_map
                                                                && p0.map.equals(p.map)) {
                                                            p.tableTickOption.listP.add(p0);
                                                        }
                                                    }
                                                    p.tableTickOption.list_check = new byte[p.tableTickOption.listP
                                                            .size()];
                                                    p.tableTickOption.list_check[0] = 1;
                                                    for (int i = 1; i < p.tableTickOption.list_check.length; i++) {
                                                        p.tableTickOption.list_check[i] = 0;
                                                    }
                                                    TableTickOption.show_table(p, "Phó bản khổng lồ");
                                                } else {
                                                    Service.send_box_ThongBao_OK(p,
                                                            "Phó Bản Khổng Lồ \n(Từ 20:00 đến 22:00 mỗi ngày)");
                                                }
                                            } else {
                                                Service.send_box_ThongBao_OK(p, "Băng đã đăng ký - đang chờ ghép");
                                            }
                                        } else {
                                            Service.send_box_ThongBao_OK(p, "Bạn không phải thuyền trưởng");
                                        }
                                    }
                                    break;
                                }
                                case 1: { // pvp bang
                                    boolean check_tt_tp = false;
                                    for (int i = 0; i < p.clan.members.size(); i++) {
                                        if (p.clan.members.get(i).name.equals(p.name)
                                                && (p.clan.members.get(i).levelInclan == 0
                                                        || p.clan.members.get(i).levelInclan == 1)) {
                                            check_tt_tp = true;
                                            break;
                                        }
                                    }
                                    if (check_tt_tp) {
                                        if (p.tableTickOption == null) {
                                            int time_h = LocalTime.now().getHourOfDay();
                                            if (time_h == 22 || time_h == 23) {
                                                p.tableTickOption = new TableTickOption();
                                                p.tableTickOption.listP = new ArrayList<>();
                                                p.tableTickOption.idDialog = 1;
                                                p.tableTickOption.listP.add(p);
                                                for (int i = 0; i < p.clan.members.size(); i++) {
                                                    Player p0 = Map
                                                            .get_player_by_name_allmap(p.clan.members.get(i).name);
                                                    if (p0 != null && p0.index_map != p.index_map
                                                            && p0.map.equals(p.map)) {
                                                        p.tableTickOption.listP.add(p0);
                                                    }
                                                }
                                                p.tableTickOption.list_check = new byte[p.tableTickOption.listP.size()];
                                                p.tableTickOption.list_check[0] = 1;
                                                for (int i = 1; i < p.tableTickOption.list_check.length; i++) {
                                                    p.tableTickOption.list_check[i] = 0;
                                                }
                                                TableTickOption.show_table(p, "Phó bản PVP băng");
                                            } else {
                                                Service.send_box_ThongBao_OK(p,
                                                        "Phó Bản PVP Băng \n(Từ 22:00 đến 23:00 mỗi ngày)");
                                            }
                                        } else {
                                            Service.send_box_ThongBao_OK(p, "Băng đã đăng ký - đang chờ ghép đội");
                                        }
                                    } else {
                                        Service.send_box_ThongBao_OK(p, "Bạn không phải thuyền trưởng");
                                    }
                                    break;
                                }
                                case 2: { // chiếm đảo
                                    p.saveOldMap();
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = Map.get_map_by_id(303);
                                    vgo.xnew = (short) Util.random(120, 380);
                                    vgo.ynew = (short) Util.random(230, 330);
                                    p.goto_map(vgo);
                                    p.type_pk = 3;
                                    break;
                                }
                                case 3: { // boss lãnh địa
                                    p.saveOldMap();
                                    Map[] map_go = Map.get_map_by_id(302);
                                    Vgo vgo = new Vgo();
                                    vgo.map_go = map_go;
                                    vgo.xnew = 100;
                                    vgo.ynew = 200;
                                    p.goto_map(vgo);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case 985: {
                        if (p.ship_pet == null) {
                            if (index == 0 || index == 1 || index == 2) {
                                p.typePirate = index;
                            } else {
                                p.typePirate = -1;
                            }
                            p.update_info_to_all();
                        } else {
                            Service.send_box_ThongBao_OK(p, "Bạn đang vận chuyển hàng");
                        }
                        break;
                    }
                    case 989: { // taixiu
                        break;
                    }

                    case 990: {
                        break;
                    }
                    case -140: { // wipper
                        switch (index) {
                            case 0: {
                                UpgradeDevil.show_table(p, 5);
                                break;
                            }
                            case 1: {
                                UpgradeDial.show_table(p);
                                break;
                            }
                            case 2: {
                                Rebuild_Item.show_table(p, 10);
                                break;
                            }
                            case 3: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(800);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 4: {
                                Service.send_box_yesno(p, 56, "Thông báo",
                                        "Bạn muốn vào Thử Thách Vệ Thần ?",
                                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                                break;
                            }
                            case 5: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(312);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                            case 6: {
                                p.saveOldMap();
                                Map[] map_go = Map.get_map_by_id(313);
                                Vgo vgo = new Vgo();
                                vgo.map_go = map_go;
                                vgo.xnew = 100;
                                vgo.ynew = 200;
                                p.goto_map(vgo);
                                break;
                            }
                        }
                        break;
                    }
                    case -86: { // miss pho ban
                        break;
                    }
                    case -77: { // ms gym
                        break;
                    }
                    case -73: { // croket
                        break;
                    }
                    case -84: { // npc mihaw
                        if (p.clan != null) {
                            Menu_Clan(p, index);
                        } else {
                            if (index == 0) {
                                Service.input_text(p, 10, "Tạo Băng Mới", new String[] { "Tên băng" });
                            } else if (index == 1) {
                                String txt = "Băng Hải Tặc\n"
                                        + " Giờ đây bạn có thể tập trung các người bạn của mình lại để tạo thành 1 nhóm cùng nhau luyện tập "
                                        + "và tham gia các hoạt động dành riêng cho nhóm rồi. \bNếu là thuyền Trưởng bạn sẽ có mọi quyền hành trong "
                                        + "Băng của mình. Từ thay đổi Icon Băng cho tới quản lý thành viên, mua sử dụng vật phẩm, cộng điểm tiềm năng , "
                                        + "hay thông báo toàn Băng.\bThuyền phó bạn sẽ được Thông báo toàn Clan và quản lý thành viên. Còn Hoa tiêu thì "
                                        + "Quản lý thành viên là việc bạn có thể làm.\bNgoài ra thì tặng quà, làm nhiệm vụ, đóng góp hay nhận các lợi ích "
                                        + "từ các vật phẩm Băng thì tất cả mọi người đều có.\bHãy tham gia vào Băng ngay sẽ có rất nhiều hoạt động hay và "
                                        + "thú vị mà chỉ dành cho Băng Hải tặc thôi nhé. ";
                                Service.Help_From_Server(p, -84, txt);
                            }
                        }
                        break;
                    }
                    case -138: {
                        Menu_Law(p, index);
                        break;
                    }
                    case 991: { // hoan my kich an
                        switch (index) {
                            case 0: {
                                Rebuild_Item.show_table(p, 6);
                                break;
                            }
                            case 1: {
                                Rebuild_Item.show_table(p, 7);
                                break;
                            }
                            case 2: {
                                Rebuild_Item.show_table(p, 8);
                                break;
                            }
                        }
                        break;
                    }
                    case -106:
                    case -91:
                    case -71:
                    case -48: {
                        Menu_Zosaku(p, index);
                        break;
                    }
                    case 992: {
                        Join_Item.show_table(p, index);
                        break;
                    }
                    case 993: {
                        switch (index) {
                            case 0: {
                                // Service.input_text(p, 4, "Đổi Coin Sang Extol", new String[] {"Nhập số Coin
                                // (1 Coin : 1 Extol)"});
                                Service.send_box_ThongBao_OK(p,
                                        "Chức năng đổi Coin sang Extol đã bị đóng để đảm bảo môi trường game lành mạnh!");
                                break;
                            }
                            case 1: {
                                Service.input_text(p, 18, "Đổi Extol Sang Búa",
                                        new String[] { "Nhập số Extol (1K Extol : 1 Búa)" });
                                break;
                            }
                            case 2: {
                                int allbua = p.get_bua();
                                int extolnhan = allbua * 1_000;
                                if (allbua <= 0) {
                                    Service.send_box_ThongBao_OK(p, "Bạn không có Búa để rút");
                                    return;
                                }
                                Service.send_box_yesno(p, 80, "Thông báo",
                                        "Bạn có muốn rút hết toàn bộ \n"
                                                + Util.number_format(allbua) + " Búa về "
                                                + Util.number_format(extolnhan) + " Extol",
                                        new String[] { Util.number_format(allbua), "Đóng" }, new byte[] { 4, -1 });
                                break;
                            }
                            case 3: {
                                Service.input_text(p, 19, "Đổi Coin Sang Beri",
                                        new String[] { "Nhập số Coin (1 Coin : 10,000 Beri)" });
                                break;
                            }
                            case 4: {
                                Service.input_text(p, 20, "Đổi Coin Sang Ruby",
                                        new String[] { "Nhập số Coin (1 Coin : 100 Ruby)" });
                                break;
                            }
                        }
                        break;
                    }
                    case -72: { // npc nami
                        Menu_Nami(p, index);
                        break;
                    }
                    case 994: {
                        if (index == 0) {
                        } else if (index == 1) {
                            String txt = "Khóa bảo vệ\nAi cũng có những món đồ mình rất quý trọng và không muốn mất nó.\nChức năng khóa bảo vệ sẽ giúp "
                                    + "bạn làm điều đó.\b"
                                    + "Sau khi đăng ký đặt khóa thì các thao tác có ảnh hưởng đến tài khoản của bạn sẽ phải nhập đúng mã để xác nhận đó "
                                    + "chính là bạn chứ không phải ai khác.\b"
                                    + "Trong một lần đăng nhập bạn chỉ cần mở khóa duy nhất 1 lần. Nếu cần biết thêm chi tiết vui lòng liên hệ admin"
                                    + " đẹp trai.";
                            switch (p.map.template.id) {
                                case 1: {
                                    Service.Help_From_Server(p, -3, txt);
                                    break;
                                }
                                case 9: {
                                    Service.Help_From_Server(p, -15, txt);
                                    break;
                                }
                                case 17: {
                                    Service.Help_From_Server(p, -23, txt);
                                    break;
                                }
                                case 25: {
                                    Service.Help_From_Server(p, -30, txt);
                                    break;
                                }
                                case 33: {
                                    Service.Help_From_Server(p, -38, txt);
                                    break;
                                }
                                case 49: {
                                    Service.Help_From_Server(p, -69, txt);
                                    break;
                                }
                                case 69: {
                                    Service.Help_From_Server(p, -75, txt);
                                    break;
                                }
                                case 83: {
                                    Service.Help_From_Server(p, -88, txt);
                                    break;
                                }
                            }
                        } else if (index == 2) {
                        }
                        break;
                    }
                    case 995: {
                        Select_Map_Tele_world(p, index);
                        break;
                    }
                    case 996: {
                        if (p.ship_pet != null) {
                            Service.send_box_ThongBao_OK(p, "Không thể dịch chuyển khi đang vận chuyển hàng");
                        } else {
                            Select_Map_Tele(p, index);
                        }
                        break;
                    }

                    case 791: {
                        if (p.data_topPkOnline != null && index >= 0 && index < p.data_topPkOnline.length) {
                            String name = p.data_topPkOnline[index].split(";")[0];
                            Player target = Map.get_player_by_name_allmap(name);
                            if (target != null) {
                                p.data_yesno = new int[] { index };
                                Service.send_box_yesno(p, 44, "Thông báo",
                                        "Bạn có muốn dịch chuyển đến " + name,
                                        new String[] { "Có", "Không" }, new byte[] { 2, 1 });
                            } else {
                                Service.send_box_ThongBao_OK(p, "Người chơi này đã offline.");
                            }
                        }
                        break;
                    }

                    case -98: {
                        core.GameLogger.info("1312");
                        if (p.map.template.id == 994) { // bang thong tin map tran chien lon
                            if (p.worldWarInfo != null) {
                                // Service.send_box_ThongBao_OK(p, "Điểm hiện tại là " + p.pointTranChienLon + "
                                // điểm");
                                Service.send_box_ThongBao_OK(p, "Điểm hiện tại là: " + p.worldWarInfo.kill
                                        + " giết - " + p.worldWarInfo.dead + " chết");
                            }
                        } else if (p.map.template.id == 260) { // bang thong tin phong cho pvp bang
                            // send_dynamic_menu(p, type, "", new String[] {"Thông tin", "Xem thi đấu"});
                            // if (index == 0) {
                            // if (p.map.pvpBang != null && p.map.pvpBang.state == 1
                            // && p.map.pvpBang.mapFight != null
                            // && p.map.pvpBang.mapFight.pvpBangMapFight != null) {
                            // String notice = "Băng đối thủ: ";
                            // if (p.map.pvpBang.mapFight.pvpBangMapFight.clan1.equals(p.clan)) {
                            // notice += p.map.pvpBang.mapFight.pvpBangMapFight.clan2.name;
                            // } else {
                            // notice += p.map.pvpBang.mapFight.pvpBangMapFight.clan1.name;
                            // }
                            // notice +=
                            // ("\nHình thức thi đấu giáp lá cà. Tất cả các thành viên có mặt đánh 1 trận
                            // duy nhất
                            // phân thắng thua.");
                            // Service.send_box_ThongBao_OK(p, notice);
                            // } else {
                            // Service.send_box_ThongBao_OK(p, "Đang trong thời gian tìm ghép đối thủ");
                            // }
                            // } else if (index == 1) {
                            // if (p.map.pvpBang != null && p.map.pvpBang.state == 2) {
                            // // Service.send_box_ThongBao_OK(p, "Bang đối thủ: ");
                            // } else {
                            // Service.send_box_ThongBao_OK(p, "Hiện tại chưa diễn ra");
                            // }
                            // }
                        }
                        break;
                    }

                    case -997: {
                        Menu_HuongDan(p, index);
                        break;
                    }
                    case -100: {
                        Menu_Robin(p, index);
                        break;
                    }
                    case 997: {
                        Menu_Remove_Skill(p, index);
                        break;
                    }
                    case 998: {
                        Menu_Learn_Skill(p, index);
                        break;
                    }
                    // case -37: {
                    // if (index < 3) {
                    // Menu_Gap(p, index);
                    // } else if (index == 3) {
                    // send_dynamic_menu(p, 978, "Đá hành trình",
                    // new String[] { "Kho hành trình", "Bản đồ hành trình", "Hướng dẫn" }, null);
                    // }
                    // break;
                    // }
                    case -4: {
                        Menu_Gap(p, index);
                        break;
                    }
                    case -145:
                    case -122:
                    case -118:
                    case -103:
                    case -87:
                    case -74:
                    case -67:
                    case -45:
                    case -37:
                    case -31:
                    case -21:
                    case -13:
                    case -1: {
                        Menu_TruongLang(p, index);
                        break;
                    }
                    case 120: { // bhx
                        break;
                    }
                    case -133: {
                        Menu_Buggi(p, index);
                        break;
                    }
                    case 9999: {
                        if (p.use_item_3 != -1 && p.use_item_3 < p.item.bag3.length) {
                            Item_wear it = p.item.bag3[p.use_item_3];
                            if (it != null) {
                                if (index == 0) { // Bản thân
                                    if (it.typelock == 1 || it.valueKichAn == 12) {
                                        p.wear_item(it);
                                        p.use_item_3 = -1;
                                    } else {
                                        // Lưu index vào data_yesno và hiển thị bảng hỏi khóa đồ
                                        p.data_yesno = new int[] { p.use_item_3 };
                                        Service.send_box_yesno(p, 9998, "Thông báo",
                                                "Khi trang bị lên người vật phẩm "
                                                        + template.ItemTemplate3.get_it_by_id(it.template.id).name
                                                        + " sẽ chuyển sang trạng thái khóa không thể giao dịch. Bạn có muốn trang bị?",
                                                new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, 1 });
                                    }
                                } else if (index == 1) { // Đệ tử
                                    if (p.myDisciple != null) {
                                        p.myDisciple.wear_item_disciple(it, p);
                                    }
                                    p.use_item_3 = -1;
                                }
                            }
                        }
                        break;
                    }
                    case 9997: {
                        Menu_TB(p, index);
                        break;
                    }
                    case 9996: {
                        // if (index == 0) {
                        // Service.send_view_second_body(p, p);
                        // } else if (index == 1) {
                        // Service.send_view_third_body(p, p);
                        // } else {
                        // Menu_Thanh_Vien(p, (byte) index);
                        // }
                        Menu_Thanh_Vien(p, (byte) index);
                        break;
                    }
                    case 9995: {
                        Menu_Uoc_Rong(p, index);
                        break;
                    }
                    case 8899: {
                        Menu_Don_Hanh_Trang(p, index);
                        break;
                    }
                    case 8889: { // Fashion choice (Master/Disciple)
                        if (p.data_yesno != null && p.data_yesno.length == 2) {
                            int id_fas = p.data_yesno[0];
                            int cat_fas = p.data_yesno[1];
                            if (index == 0) { // Sư phụ
                                p.data_yesno = new int[] { id_fas };
                                if (cat_fas == 105) {
                                    Service.send_box_yesno(p, 81, "", template.ItemFashion.get_item(id_fas).name
                                            + "\n Bạn muốn mặc hay đổi chỉ số ?",
                                            new String[] { "Mặc", "Đổi CS", "Đóng" },
                                            new byte[] { -1, -1, -1 });
                                } else {
                                    ItemFashionP temp = p.check_itfashionP(id_fas, cat_fas);
                                    if (temp != null) {
                                        p.update_itfashionP(temp, cat_fas);
                                        for (Player p0 : p.map.players) {
                                            Service.charWearing(p, p0, false);
                                        }
                                        Service.UpdateInfoMaincharInfo(p);
                                        Service.send_box_ThongBao_OK(p,
                                                "Sư phụ đã mặc " + template.ItemHair.get_item(id_fas, cat_fas).name);
                                    }
                                }
                            } else if (index == 1) { // Đệ tử
                                if (p.myDisciple != null) {
                                    String name = (cat_fas == 105) ? template.ItemFashion.get_item(id_fas).name
                                            : template.ItemHair.get_item(id_fas, cat_fas).name;
                                    Service.UpdateInfoMaincharInfo(p);
                                    Service.send_box_yesno(p, 8897, "XÁC NHẬN",
                                            "Bạn muốn cho đệ tử sử dụng " + name + "?",
                                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, 1 });
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Đệ tử chưa được triệu hồi!");
                                }
                            }
                        }
                        break;
                    }
                    case 8890: { // TAQ choice (Master/Disciple)
                        if (p.data_yesno != null && p.data_yesno.length == 1) {
                            int itId = p.data_yesno[0];
                            if (index == 0) { // Sư phụ
                                if (itId == 86) {
                                    Service.send_box_yesno(p, 35, "Thông báo", "Bạn có muốn sử dụng Trái Ác Quỷ?",
                                            new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                                } else if (itId == 87) {
                                    Service.send_box_yesno(p, 37, "Thông báo",
                                            "Bạn có muốn sử dụng Trái Ác Quỷ trung cấp?",
                                            new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                                } else {
                                    template.ItemTemplate4 it4 = template.ItemTemplate4.get_it_by_id(itId);
                                    Service.send_box_yesno(p, (itId + 4000), "Thông báo",
                                            "Bạn có muốn sử dụng " + (it4 != null ? it4.name : "Trái Ác Quỷ") + "?",
                                            new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                                }
                            } else if (index == 1) { // Đệ tử
                                if (p.myDisciple != null) {
                                    template.ItemTemplate4 it4 = template.ItemTemplate4.get_it_by_id(itId);
                                    Service.send_box_yesno(p, 8896, "XÁC NHẬN",
                                            "Bạn muốn cho đệ tử sử dụng " + (it4 != null ? it4.name : "Trái Ác Quỷ")
                                                    + "?",
                                            new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, 1 });
                                } else {
                                    Service.send_box_ThongBao_OK(p, "Đệ tử chưa được triệu hồi!");
                                }
                            }
                        }
                        break;
                    }
                    case 32003: {
                        int[] shop_pos = new int[4];
                        int pos = 0;
                        for (int i = 0; i < 5; i++) {
                            if ((i + 1) == p.clazz) {
                                continue;
                            }
                            shop_pos[pos++] = (int) i;
                        }
                        Service.Send_UI_Shop(p, shop_pos[index]);
                        break;
                    }
                    case 32002: {
                        switch (index) {
                            case 0: {
                                Service.Send_UI_Shop(p, 6);
                                break;
                            }
                            case 1:
                            case 2: {
                                UpgradeDevil.show_table(p, index);
                                break;
                            }
                        }
                        break;
                    }
                    case 32001: {
                        Menu_KhamNgoc(p, index);
                        break;
                    }
                    case 32000: {
                        Menu_Rebuilt_Item(p, index);
                        break;
                    }
                    case -105:
                    case -90:
                    case -70:
                    case -47: {
                        Menu_Johny(p, index);
                        break;
                    }
                    case -147:
                    case -120:
                    case -116:
                    case -102:
                    case -89:
                    case -76:
                    case -68:
                    case -46:
                    case -39:
                    case -29:
                    case -22: // menu cho cho
                    case -14: // menu rita
                    case -2: {
                        Menu_Machiko(p, index);
                        break;
                    }
                    case -146:
                    case -121:
                    case -117:
                    case -101:
                    case -88:
                    case -75:
                    case -69: // masu
                    case -38: // menu partty
                    case -30: // menu merri
                    case -23: // menu poroy
                    case -15: // Menu_MomRiTa
                    case -3: {
                        Menu_Guru(p, index);
                        break;
                    }
                    case -144: // kinh do nuoc
                    case -124: // thi tran thien su
                    case -132: // dao jaza
                    case -107: // thi tran nanohano
                    case -85: // thi tran horn
                    case -97: // dao little grand
                    case 0: // thi tran whiskey
                    case -82: // mom sinh doi
                    case -60: // thi tran khoi dau
                    case -44: // lang hat de
                    case -36: // nha hang barati
                    case -28: // lang sirup
                    case -20: // thi tran orang
                    case -12: // thi tran vo so
                    case -5: { // lang coi xay gio
                        Show_List_Map_Tele(p, index, idNPC);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            GameLogger.error("MenuController.process_menu Exception for User: " + (p != null ? p.name : "unknown"), e);
        }
    }

    public static String formatBeri(long beri) {
        if (beri >= 1_000_000_000L) {
            return String.format("%dB", beri / 1_000_000_000);
        } else {
            return String.format("%dM", beri / 1_000_000);
        }
    }

    private static void Menu_Clan(Player p, byte index) throws IOException {
        if (p.clan == null || index > 2 && !p.clan.members.get(0).name.equals(p.name)) {
            return;
        }
        switch (index) {
            case 0: {
                Clan_member clan_mem = null;
                for (int i = 0; i < p.clan.members.size(); i++) {
                    if (p.clan.members.get(i).name.equals(p.name)) {
                        clan_mem = p.clan.members.get(i);
                        break;
                    }
                }
                if (clan_mem != null) {
                    if (clan_mem.numquest >= 3) {
                        Service.send_box_ThongBao_OK(p, "Hôm nay đã hết nhiệm vụ, hãy quay lại vào ngày mai");
                        return;
                    }
                    //
                    QuestP questP = null;
                    for (int i = 0; i < p.list_quest.size(); i++) {
                        if (p.list_quest.get(i).template.id < -2000) {
                            questP = p.list_quest.get(i);
                            break;
                        }
                    }
                    if (questP == null) {
                        Service.send_box_yesno(p, 42, "Thông báo",
                                ("Bạn muốn nhận nhiệm vụ Băng hải tặc cấp " + (clan_mem.numquest + 1)),
                                new String[] { "Đồng ý", "Hủy" }, new byte[] { -1, -1 });
                    } else {
                        Service.send_box_ThongBao_OK(p, "Nhiệm vụ hiện tại chưa hoàn thành");
                    }
                }
                break;
            }
            case 1: {
                UpgradeDevil.show_table(p, 7);
                break;
            }
            case 2: {
                send_dynamic_menu(p, 984, "Phó bản băng",
                        new String[] { "Phó Bản Khổng Lồ", "Phó Bản PVP Băng", "Chiếm Đảo Băng", "Lãnh Địa Băng" },
                        new short[] { 146, 137, 142, 105 });
                break;
            }
            case 3: {
                send_dynamic_menu(p, 979, "Icon băng", new String[] { "Cửa hàng thường", "Cửa hàng cao cấp" },
                        new short[] { 164, 166 });
                break;
            }
            case 4: {
                try {
                    Message m = new Message(-19);
                    m.writer().writeByte(110);
                    m.writer().writeUTF("Cửa hàng vật phẩm băng");
                    m.writer().writeByte(8);
                    m.writer().writeShort(ItemTemplate8.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate8.ENTRYS.size(); i++) {
                        ItemTemplate8 temp = ItemTemplate8.ENTRYS.get(i);
                        m.writer().writeShort(temp.id);
                        m.writer().writeShort(1);
                    }
                    p.addmsg(m);
                    m.cleanup();
                } catch (IOException e) {
                    GameLogger.warn("MenuController: IO error sending Item_info47 for " + p.name, e);
                }
                break;
            }

            case 5: {
                if (p.clan.allowRequest == 1) {
                    p.clan.allowRequest = 0;
                    Service.send_box_ThongBao_OK(p, "Khóa mọi người xin vào băng thành công");
                } else {
                    p.clan.allowRequest = 1;
                    Service.send_box_ThongBao_OK(p, "Cho phép mọi người xin vào băng thành công");
                }
                break;
            }

            case 6: {
                if (p.clan.members.get(0).name.equals(p.name)) {
                    Service.input_text(p, 21, "Đổi tên băng",
                            new String[] { "Nhập tên băng mới (50,000 Coin)" });
                } else {
                    Service.send_box_ThongBao_OK(p, "Thuyền Trưởng mới được phép đổi tên băng");
                }
                break;
            }

            case 7: {
                if (p.clan.members.get(0).name.equals(p.name)) {
                    Service.input_text(p, 12, "Nhường băng", new String[] { "Nhập tên người muốn nhường" });
                } else {
                    Service.send_box_ThongBao_OK(p, "Thuyền Trưởng mới được phép nhường băng");
                }
                break;
            }

            case 8: {
                if (p.clan.members.get(0).name.equals(p.name)) {
                    Service.input_text(p, 13, "Xóa băng", new String[] { "Nhập 'xoabang' để xác nhận xóa" });

                } else {
                    Service.send_box_ThongBao_OK(p, "Thuyền Trưởng mới được phép xóa băng");
                }
                break;
            }
        }
    }

    private static void Menu_Law(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                UpgradeDevil.show_table(p, 10);
                break;
            }
            case 1: {
                UpgradeDial.show_table(p);
                break;
            }
            case 2: {
                if (p.getTongNap() == 0 && !Manager.gI().server_test) {
                    Service.send_box_ThongBao_OK(p, "Bạn chưa nạp lần đầu.");
                    return;
                }
                VongXoayMayManManager.showSpinMenu(p);
                break;
            }
            case 3: {
                taixiu.TaiXiuManager.guiHuongDanUI(p);
                break;
            }
            case 4: {
                taixiu.BauCuaManager.guiHuongDan(p);
                break;
            }
            case 5: {
                taixiu.BlackjackManager.showMainMenu(p);
                break;
            }
            case 6: { // Slot Machine
                taixiu.SlotMachineManager.showMainMenu(p);
                break;
            }
            case 7: {
                Service.send_box_yesno(p, 883, "Ghép Tim GR",
                        "Bạn có muốn ghép Tim GR?\n"
                                + "- 100 Ngọc đế quang\n"
                                + "- Chi phí: 5.000.000 Extol\n"
                                + "Tỉ lệ thành công: 100%",
                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 0, -1 });
                break;
            }
        }
    }

    private static void Menu_Nami(Player p, byte index) throws IOException {

        switch (index) {
            case 0: {
                send_dynamic_menu(p, 993, "Quy Đổi Tiền",
                        new String[] { "Đổi Coin -> Extol", "Đổi Extol -> Búa", "Rút Búa -> Extol", "Đổi Coin -> Beri",
                                "Đổi Coin -> Ruby" },
                        new short[] { 170, 172, 169, 170, 170 });
                break;
            }
            case 1: {
                ArchiDaily.show_table(p);
                break;
            }
            case 2: {
                if (Manager.gI().server_test && p.getAdmin() == 0) {
                    Service.send_box_ThongBao_OK(p, "Chưa Open");
                    return;
                }
                ListTichNap.showTable(p);
                break;
            }
            case 3: {
                if (Manager.gI().server_test && p.getAdmin() == 0) {
                    Service.send_box_ThongBao_OK(p, "Chưa Open");
                    return;
                }
                ListTichTieu.showTable(p);
                break;
            }
            case 4: {
                Message m = new Message(-19);
                m.writer().writeByte(118);
                m.writer().writeUTF("Cửa hàng Coin");
                m.writer().writeByte(11);
                m.writer().writeShort(ShopTichLuy.ENTRY.size());
                for (int i = 0; i < ShopTichLuy.ENTRY.size(); i++) {
                    ShopTichLuy temp = ShopTichLuy.ENTRY.get(i);
                    m.writer().writeShort(temp.id);
                    m.writer().writeByte(temp.type);
                    switch (temp.type) {
                        case 4: {
                            ItemTemplate4 temp4 = ItemTemplate4.get_it_by_id(temp.id);
                            m.writer().writeUTF(temp4.name);
                            m.writer().writeShort(temp4.icon);
                            break;
                        }
                        case 7: {
                            ItemTemplate7 temp7 = ItemTemplate7.get_it_by_id(temp.id);
                            m.writer().writeUTF(temp7.name);
                            m.writer().writeShort(temp7.icon);
                            break;
                        }
                        case 105: { // thoi trang
                            ItemFashion temp105 = ItemFashion.get_item(temp.id);
                            m.writer().writeUTF(temp105.name);
                            m.writer().writeShort(temp105.idIcon);
                            break;
                        }
                    }
                    NumberFormat nf = NumberFormat.getInstance();
                    String formattedPoint = nf.format(temp.point);
                    String info = temp.info + "\n" + formattedPoint + " Coin.";
                    if (temp.limit > 0) {
                        int time = 0;
                        if (temp.limit_data.containsKey(p.name)) {
                            time = temp.limit_data.get(p.name);
                        }
                        info += "\nĐổi tối đa: " + temp.limit + " lần.\nHiện tại đã đổi: " + time + "/"
                                + temp.limit + ".";
                    }
                    m.writer().writeUTF(info);
                }
                p.addmsg(m);
                m.cleanup();

                break;
            }
            case 5: {
                DauGia.show_table(p);
                break;
            }
            case 6: {
                Market.show_table(p);
                break;
            }
            case 7: {
                DecimalFormat formatter = new DecimalFormat("#,###");
                String trangThai = p.getKichHoat() == 0 ? "Chưa kích hoạt" : "Đã kích hoạt";
                String info = String.format( //
                        "\n---------------------------\n" //
                                + "Ruby: %s\n" //
                                + "Beli: %s\n" //
                                + "Extol: %s\n" //
                                + "Búa: %s\n" //
                                + "Coin: %s\n" //
                                + "Tổng Nạp: %s\n" //
                                + "Trạng thái: %s\n" //
                                + "Lần trùng sinh: %d\n" //
                                + "---------------------------",
                        formatter.format(p.get_ngoc()),
                        formatter.format(p.get_vang()),
                        formatter.format(p.get_vnd()),
                        formatter.format(p.get_bua()),
                        formatter.format(p.get_coin()),
                        formatter.format(p.getTongNap()),
                        trangThai,
                        p.reincarnation);
                Chat.send_chat(p, "Hệ thống", info, false);
                Service.send_box_ThongBao_OK(p, "Vui lòng kiểm tra hộp thư");
                break;
            }
            case 8: { // Shop Danh Hiệu
                Message m = new Message(-19);
                m.writer().writeByte(121); // TypeShop cho Danh Hiệu
                m.writer().writeUTF("Cửa hàng Danh Hiệu");
                m.writer().writeByte(11); // Byte hiển thị tooltip
                m.writer().writeShort(ShopExtol.ENTRY.size());
                for (int i = 0; i < ShopExtol.ENTRY.size(); i++) {
                    ShopExtol temp = ShopExtol.ENTRY.get(i);
                    DanhHieu template = DanhHieu.getTemplate(temp.id_title);
                    if (template != null) {
                        m.writer().writeShort(temp.id); // ID trong shop_extol
                        m.writer().writeByte(106); // Type client cho Danh hiệu (giả định)
                        m.writer().writeUTF(template.name);
                        m.writer().writeShort(temp.icon);

                        StringBuilder sb = new StringBuilder(temp.info != null ? temp.info : "");
                        if (!template.options.isEmpty()) {
                            sb.append("\nChỉ số:");
                            for (Option op : template.options) {
                                ItemOptionTemplate opTemp = ItemOptionTemplate.get_temp(op.id);
                                if (opTemp != null) {
                                    sb.append("\n- ").append(opTemp.name).append(": +").append(op.getParam());
                                    if (opTemp.percent == 1)
                                        sb.append("%");
                                }
                            }
                        }
                        DecimalFormat df = new DecimalFormat("#,###");
                        sb.append("\nGiá: ").append(df.format(temp.price)).append(" Extol.");
                        m.writer().writeUTF(sb.toString());
                    }
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }

        }

    }

    private static void Menu_Zosaku(Player p, byte index) throws IOException {
        switch (index) {
            case 0: { // map pvp
                Vgo vgo = new Vgo();
                vgo.map_go = Map.get_map_by_id(1000);
                if (vgo.map_go != null) {
                    boolean full = false;
                    if (!full) {
                        vgo.xnew = (short) Util.random(150, 300);
                        vgo.ynew = (short) Util.random(200, 300);
                        p.goto_map(vgo);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Map đầy hãy quay lại sau");
                    }
                }
                break;
            }
            case 1: { // truy na
                Vgo vgo = new Vgo();
                vgo.map_go = Map.get_map_by_id(119);
                if (vgo.map_go != null) {
                    boolean full = false;
                    if (!full) {
                        vgo.xnew = (short) Util.random(120, 380);
                        vgo.ynew = (short) Util.random(230, 330);
                        p.goto_map(vgo);
                    } else {
                        Service.send_box_ThongBao_OK(p, "Map đầy hãy quay lại sau");
                    }
                }
                break;
            }
            case 2: {
                List<String> topList = new ArrayList<>();
                try {
                    List<Player> onlinePlayers = Map.get_all_players();
                    for (Player pOnline : onlinePlayers) {
                        if (pOnline != null && pOnline.pointPk > 0) {
                            topList.add(pOnline.name + ";" + pOnline.pointPk);
                        }
                    }
                    topList.sort((a, b) -> Integer.compare(
                            Integer.parseInt(b.split(";")[1]),
                            Integer.parseInt(a.split(";")[1])));
                    int limit = Math.min(20, topList.size());
                    String[] menu = new String[limit];
                    for (int i = 0; i < limit; i++) {
                        String[] data = topList.get(i).split(";");
                        menu[i] = (i + 1) + ". " + data[0] + " (" + formatPoint(Integer.parseInt(data[1])) + ")";
                    }
                    send_dynamic_menu(p, 791, "Đồ Sát Vương (Online)", menu, null);
                    p.data_topPkOnline = topList.toArray(new String[0]);
                } catch (Exception e) {
                    GameLogger.error("MenuController: Error loading Top PK Online", e);
                }
                break;
            }
            case 3: { // lenh truy na
                send_dynamic_menu(p, -888, "Lệnh truy nã",
                        new String[] { "Danh sách truy nã", "Phát lệnh truy nã", "Đối tượng truy nã" }, null);
                break;
            }

        }
    }

    private static String formatPoint(int point) {
        if (point >= 1_000_000_000)
            return (point / 1_000_000_000) + "B";
        if (point >= 1_000_000)
            return (point / 1_000_000) + "M";
        if (point >= 1_000)
            return (point / 1_000) + "K";
        return String.valueOf(point);
    }

    private static void Select_Map_Tele(Player p, byte index) throws IOException {
        if (p.map_tele != null) {
            Map[] map_go = Map.get_map_by_id(p.map_tele[index]);
            if (p.map.template.id == map_go[0].template.id) {
                Service.send_box_ThongBao_OK(p, "Đang ở map này rồi!");
            } else {
                Vgo vgo = new Vgo();
                vgo.map_go = map_go;
                for (int i = 0; i < vgo.map_go[0].template.npcs.size(); i++) {
                    Npc npc_temp = vgo.map_go[0].template.npcs.get(i);
                    if (npc_temp.namegt.equals("Bản đồ")) {
                        vgo.xnew = npc_temp.x;
                        if (npc_temp.y < 250) {
                            vgo.ynew = (short) (npc_temp.y + 20);
                        } else {
                            vgo.ynew = (short) (npc_temp.y - 20);
                        }
                        break;
                    }
                }
                if (vgo.xnew == 0 || vgo.ynew == 0) {
                    vgo.xnew = (short) (vgo.map_go[0].template.maxW / 2);
                    vgo.ynew = (short) (vgo.map_go[0].template.maxH / 2);
                }
                p.goto_map(vgo);
            }
        }
        p.map_tele = null;
    }

    private static void Select_Map_Tele_world(Player p, byte index) throws IOException {
        if (p.map_tele != null && index < p.map_tele.length) {
            p.data_yesno = new int[] { index };
            Service.send_box_yesno(
                    p, 5, "", "Bạn có muốn dịch chuyển qua "
                            + Map.get_map_by_id(p.map_tele[index])[0].template.name + " ?",
                    new String[] { "200", "Hủy" }, new byte[] { 6, -1 });
        }
    }

    private static void Menu_HuongDan(Player p, byte index) throws IOException {
        switch (p.map.template.id) {
            case 1: {
                HelpDialog.show_LangCoiXayGio(p, index);
                break;
            }
            case 9: {
                HelpDialog.show_ThiTranVoSo(p, index);
                break;
            }
            case 17: {
                HelpDialog.show_ThiTranOrange(p, index);
                break;
            }
            case 25: {
                HelpDialog.show_LangSiRup(p, index);
                break;
            }
            case 33: {
                HelpDialog.show_ThuyenBarati(p, index);
                break;
            }
            case 41: {
                HelpDialog.show_LangHatDe(p, index);
                break;
            }
            case 49: {
                HelpDialog.show_ThiTranKhoiDau(p, index);
                break;
            }
            case 66: {
                HelpDialog.show_MomSinhDoi(p, index);
                break;
            }
            case 69: {
                HelpDialog.show_ThiTranWhiskey(p, index);
                break;
            }
            case 79: {
                HelpDialog.show_DaoLittleGrand(p, index);
                break;
            }
        }
    }

    private static void Menu_Remove_Skill(Player p, byte index) throws IOException {
        for (int i = 0; i < p.skill_point.size(); i++) {
            Skill_info temp = p.skill_point.get(i);
            if (temp.temp.ID >= 1000 && temp.temp.ID < 2000 && temp.temp.Lv_RQ > 0) {
                index--;
                if (index == -1) {
                    p.data_yesno = new int[] { i };
                    Service.send_box_yesno(p, 4, "Thông báo",
                            ("Bạn có chắc muốn xóa kỹ năng " + temp.temp.name
                                    + " này không? Phí xóa kỹ năng này là 2 ruby"),
                            new String[] { "2", "Không" }, new byte[] { 7, -1 });
                    break;
                }
            }
        }
    }

    private static void Menu_Robin(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                EffTemplate eff = p.get_eff(8);
                if (eff != null) {
                    Service.send_box_ThongBao_OK(p,
                            "Thời gian khóa EXP còn lại "
                                    + Util.get_time_str_by_sec2(eff.time - System.currentTimeMillis())
                                    + "\nLưu ý cộng dồn tối đa 30 ngày");
                } else {
                    Service.send_box_ThongBao_OK(p, "Thời gian khóa EXP còn lại 0 s.");
                }
                break;
            }
            case 1: {
                EffTemplate effTemplate = p.get_eff(8);
                if (effTemplate != null) {
                    effTemplate.time = 0;
                    Service.send_box_ThongBao_OK(p, "Hủy thành công");
                }
                break;
            }

        }
    }

    private static void Menu_Learn_Skill(Player p, byte index) throws IOException {
        for (int i = 0; i < p.skill_point.size(); i++) {
            Skill_info temp = p.skill_point.get(i);
            if ((temp.temp.ID < 3 && temp.temp.Lv_RQ == -1)
                    || (temp.temp.ID > 3 && temp.temp.ID < 2000 && temp.temp.Lv_RQ < 5)) {
                index--;
                if (index == -1) {
                    p.data_yesno = new int[] { i };
                    if (temp.temp.ID < 3) {
                        Service.send_box_yesno(p, 3, "Thông báo",
                                ("Bạn có muốn học kỹ năng " + temp.temp.name + "?"),
                                new String[] { "10.000", "Không" }, new byte[] { 6, -1 });
                    } else {
                        Skill_Template sk_temp = null;
                        if (temp.temp.ID > 3 && temp.temp.ID < 2000 && temp.temp.Lv_RQ > -1) {
                            sk_temp = Skill_Template.get_temp((temp.temp.indexSkillInServer + 1), 0);
                        }
                        Service.send_box_yesno(p, 3, "Thông báo",
                                ("Bạn có muốn học chiêu nội tại "
                                        + (sk_temp != null ? sk_temp.name : temp.temp.name) + "?"),
                                new String[] { "10.000", "Không" }, new byte[] { 6, -1 });
                    }
                    break;
                }
            }
        }
    }

    private static void Menu_Gap(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                List<String> str_ = new ArrayList<>();
                List<Integer> icon_ = new ArrayList<>();
                for (int i = 0; i < p.skill_point.size(); i++) {
                    Skill_info temp = p.skill_point.get(i);
                    if ((temp.temp.ID < 3 && temp.temp.Lv_RQ == -1)
                            || (temp.temp.ID > 3 && temp.temp.ID < 2000 && temp.temp.Lv_RQ < 5)) {
                        if (temp.temp.ID > 3 && temp.temp.ID < 2000 && temp.temp.Lv_RQ > -1) {
                            Skill_Template sk_temp = Skill_Template.get_temp((temp.temp.indexSkillInServer + 1), 0);
                            str_.add(sk_temp.name);
                            icon_.add((int) (sk_temp.idIcon));
                        } else {
                            str_.add(temp.temp.name);
                            icon_.add((int) (temp.temp.idIcon));
                        }
                    }
                }
                if (str_.size() == 0) {
                    str_.add("Bạn đã học hết các kỹ năng");
                    icon_.add(-1);
                }
                send_dynamic_menu(p, 998, "Học kỹ năng - Gap", str_, icon_);
                break;
            }
            case 1: {
                Service.send_box_yesno(p, 2, "",
                        "Bạn có thật sự muốn hồi điểm tiềm năng?\nMức phí là 500 Ruby.",
                        new String[] { "500", "Hủy" }, new byte[] { 7, -1 });
                break;
            }
            case 2: {
                Max_Level.show_table(p);
                break;
            }
            case 3: { // Chỉ số ước ngọc rồng
                if (p.wishStats == null) {
                    activities.WishManager.gI().loadWishData(p);
                }
                String msg = "Chỉ số tích lũy từ Rồng Thần:\n";
                msg += "- Extol tôn xưng: " + Util.number_format(p.wishStats.extol) + "\n";
                msg += "- Né đòn: " + (p.wishStats.dodge * 10) + "%\n";
                msg += "- Miễn thương: " + (p.wishStats.reduction * 10) + "%\n";
                msg += "- Phản đòn: " + (p.wishStats.react * 10) + "%\n";
                msg += "- Xuyên giáp: " + (p.wishStats.pierce * 10) + "%\n";
                msg += "- Sát thương chí mạng: " + (p.wishStats.critDame * 10) + "%\n";
                msg += "- Giảm né đối phương: " + (p.wishStats.decDodge * 10) + "%\n";
                msg += "- Giảm phản đối phương: " + (p.wishStats.decReact * 10) + "%\n";
                msg += "- Giảm miễn đối phương: " + (p.wishStats.decReduc * 10) + "%\n";
                msg += "- Giảm xuyên đối phương: " + (p.wishStats.decPierce * 10) + "%\n";
                msg += "- Sát thương cuối: " + (p.wishStats.finalDame * 5) + "%\n";
                msg += "- Máu cuối: " + (p.wishStats.finalHp * 5) + "%";
                Service.send_box_ThongBao_OK(p, msg);
                break;
            }
            case 4: { // Mục tiêu săn ngọc
                if (p.huntTarget == null) {
                    Service.send_box_ThongBao_OK(p, "Bạn hiện không có mục tiêu săn ngọc nào.");
                } else {
                    activities.WishManager.gI().sendHuntNotice(p);
                }
                break;
            }
            default: {
                List<String> dynamicOptions = new ArrayList<>();
                if (activities.Mastery2.canUnlock(p)) {
                    dynamicOptions.add("Thăng Giai");
                }
                if (p.hasUnlockedMastery2) {
                    dynamicOptions.add("Thông Thạo 2");
                }

                int dynamicIndex = index - 5; // Updated index for dynamic options
                if (dynamicIndex >= 0 && dynamicIndex < dynamicOptions.size()) {
                    String choice = dynamicOptions.get(dynamicIndex);
                    if (choice.equals("Thăng Giai")) {
                        activities.Mastery2.doUpgrade(p);
                    } else if (choice.equals("Thông Thạo 2")) {
                        activities.Mastery2.showMenuAttribute(p);
                    }
                }
                break;
            }
        }
    }

    private static void Menu_TruongLang(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                BXH.send(p, 7, 0);
                break;
            }
            case 1: {
                BXH.send(p, 4, 0);
                break;
            }
            case 2: {
                BXH.send(p, 6, 0);
                break;
            }
            case 3: {
                BXH.send(p, 9, 0);
                break;
            }
            case 4: {
                Service.input_text(p, 1, "Quà tặng máy chủ", new String[] { "Nhập Giftcode" });
                break;
            }
            case 5: {
                send_dynamic_menu(p, 973, "TOP Săn Boss",
                        new String[] { "Xem Bảng TOP", "Nhận Quà" }, null);
                break;
            }
            case 6: {
                if (p.levelAwaken >= 2) {
                    Service.send_box_ThongBao_OK(p, "Bạn đã đạt đỉnh cao Thức tỉnh Giai đoạn 2.");
                    break;
                }
                String reqStr = "[YÊU CẦU THỨC TỈNH]\n\n"
                        + "Để tiến hành thức tỉnh, bạn cần chuẩn bị:\n"
                        + "- Trái 1: " + ItemTemplate4.get_item_name(p.idDF_required[0]) + "\n"
                        + "- Trái 2: " + ItemTemplate4.get_item_name(p.idDF_required[1]) + "\n"
                        + "- Trái 3: " + ItemTemplate4.get_item_name(p.idDF_required[2]) + "\n\n";

                if (p.levelAwaken == 0) {
                    reqStr += "• Cần ít nhất 1 bộ trang bị đạt cấp +100.\n• Tỉ lệ thành công: 5%";
                } else if (p.levelAwaken == 1) {
                    reqStr += "• Cần cả 2 bộ trang bị đạt cấp +100.\n• Tỉ lệ thành công: 1%";
                }

                reqStr += "\n\n(Lưu ý: Nếu thất bại, yêu cầu về Trái ác quỷ sẽ thay đổi!)";

                Service.send_box_yesno(p, 974, "THÔNG BÁO", reqStr, new String[] { "Tiến hành", "Đóng" },
                        new byte[] { 0, -1 });
                break;
            }
            case 7: {
                if (p.level < 100) {
                    Service.send_box_ThongBao_OK(p, "Bạn cần đạt cấp 100 để có thể trùng sinh.");
                    break;
                }
                long cost = 200_000L + (50_000L * p.reincarnation);
                String msg = "[TRÙNG SINH]\n\n"
                        + "Lần trùng sinh hiện tại: " + p.reincarnation + "\n"
                        + "Chi phí lần tới: " + Util.number_format(cost) + " Extol\n"
                        + "Lợi ích: Tăng tiềm năng nhận được mỗi khi thăng cấp.\n\n"
                        + "Lưu ý: Sau khi trùng sinh, cấp độ sẽ reset về 1 và điểm tiềm năng sẽ được đặt lại!";

                Service.send_box_yesno(p, 975, "THÔNG BÁO", msg, new String[] { "Đồng ý", "Đóng" },
                        new byte[] { 0, -1 });
                break;
            }
            case 8: {
                // Khởi tạo phó bản Vô Hạn Liên Tầng
                Map mapTemplate = Map.get_map_by_id(301)[0]; // Sử dụng template Đảo Sét (ID 301)
                Map map_boss = new Map();
                map_boss.template = mapTemplate.template;
                map_boss.zone_id = (byte) 0;
                map_boss.list_mob = new int[0];
                map_boss.map_VoHanLienTang = new template.Map_Vo_Han_Lien_Tang();
                map_boss.map_VoHanLienTang.time = System.currentTimeMillis() + (60_000L * 2); // 2 Phút
                map_boss.map_VoHanLienTang.floor = 0; // Bắt đầu từ 0
                map_boss.map_VoHanLienTang.mobs = new java.util.ArrayList<>();
                map_boss.map_VoHanLienTang.time_next_floor = System.currentTimeMillis() + 1000L;

                // Dịch chuyển người chơi vào map
                p.map.leave_map(p, 2);
                p.map = map_boss;
                p.x = 100;
                p.y = 300;
                p.xold = p.x;
                p.yold = p.y;
                p.map.goto_map(p);
                Service.send_time_cool_down(p, map_boss.map_VoHanLienTang.time,
                        "VHT tầng " + (map_boss.map_VoHanLienTang.floor + 1), 0);

                map_boss.start_map();
                Map.add_map_plus(map_boss);
                break;
            }
            case 9: {
                // Hiển thị BXH VHLT
                try {
                    VoHanLienTangRanking.show(p);
                } catch (Exception e) {
                    core.GameLogger.error("MenuController.Menu_TruongLang: Error showing VHLT ranking", e);
                }
                break;
            }
        }
    }

    private static void Menu_Buggi(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                VongQuay.show_table(p);
                break;
            }
            case 1: {
                Service.input_text(p, 100, "Quay nhanh", new String[] { "Nhập số lần quay (x3 vật phẩm/lần)" });
                break;
            }
            case 2: {
                send_dynamic_menu(p, 991, "Hoàn mỹ - Kích ẩn",
                        new String[] { "Hoàn mỹ", "Kích ẩn" }, null);
                break;
            }
        }
    }

    private static void Menu_KhamNgoc(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {

                Service.Send_UI_Shop(p, 111);
                break;
            }
            case 1:
            case 2:
            case 3:
            case 4:
            case 5: {
                Rebuild_Item.show_table(p, index);
                break;
            }
            case 6: { // ngoc than thoai
                Message m = new Message(-19);
                m.writer().write(DaThanThoai.data_shop);
                p.addmsg(m);
                m.cleanup();
                break;
            }

        }
    }

    private static void Menu_TB(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                if (p.tempPlayer != null) {
                    Service.send_view_other_player(p.tempPlayer, p);
                }
                break;
            }
            case 1: {
                if (p.tempPlayer != null) {
                    Service.send_view_second_body(p.tempPlayer, p);
                }
                break;
            }
            case 2: {
                if (p.tempPlayer != null) {
                    Service.send_view_third_body(p.tempPlayer, p);
                }
                break;
            }
        }
    }

    private static void Menu_Thanh_Vien(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_view_second_body(p, p);
                break;
            }
            case 1: {
                Service.send_view_third_body(p, p);
                break;
            }
            case 2: {
                if (p.soluongmua == 0) {
                    p.soluongmua = 100;
                } else if (p.soluongmua == 100) {
                    p.soluongmua = 500;
                } else if (p.soluongmua == 500) {
                    p.soluongmua = 2000;
                } else if (p.soluongmua == 2000) {
                    p.soluongmua = 9999;
                } else {
                    p.soluongmua = 0;
                }
                String message = p.soluongmua == 0 ? "Số lượng mua: Mặc định" : "Số lượng mua: x" + p.soluongmua;
                Service.send_box_ThongBao_OK(p, message);
                break;
            }
            case 3: {
                if (p.solannang == 10) {
                    p.solannang = 50;
                } else if (p.solannang == 50) {
                    p.solannang = 200;
                } else if (p.solannang == 200) {
                    p.solannang = 1000;
                } else {
                    p.solannang = 10;
                }
                Service.send_box_ThongBao_OK(p, "Số Lần Auto Nâng : " + p.solannang + " Lần");
                break;
            }
            case 4: {
                p.is_show_gift = !p.is_show_gift;
                Service.send_box_ThongBao_OK(p,
                        p.is_show_gift ? "Đã Bật Hiển Thị Quà" : "Đã Tắt Hiển Thị Quà");
                break;
            }
            case 5: {
                if (p.myDisciple == null) {
                    Service.send_box_ThongBao_OK(p, "Bạn không có đệ tử!");
                } else {
                    send_disciple_menu(p);
                }
                break;
            }
        }
    }

    private static void Menu_Uoc_Rong(Player p, byte index) throws IOException {

        // Final check for Dragon Balls to prevent exploits
        boolean hasAll = true;
        for (int id = 1087; id <= 1093; id++) {
            if (p.item.total_item_bag_by_id(4, id) < 1) {
                hasAll = false;
                break;
            }
        }
        if (!hasAll) {
            Service.send_box_ThongBao_OK(p, "Bạn không đủ 7 viên ngọc rồng vô cực để thực hiện điều ước.");
            return;
        }

        if (p.wishStats == null) {
            activities.WishManager.gI().loadWishData(p);
        }

        String wishMsg = "";
        switch (index) {
            case 0: { // 1M Extol
                p.wishStats.extol += 1_000_000;
                p.update_vnd(1_000_000);
                wishMsg = "1 triệu Extol";
                break;
            }
            case 1: { // 10% Dodge
                p.wishStats.dodge += 1;
                wishMsg = "10% Né đòn";
                break;
            }
            case 2: { // 10% Reduction
                p.wishStats.reduction += 1;
                wishMsg = "10% Miễn thương";
                break;
            }
            case 3: { // 10% Reaction
                p.wishStats.react += 1;
                wishMsg = "10% Phản đòn";
                break;
            }
            case 4: { // 10% Pierce
                p.wishStats.pierce += 1;
                wishMsg = "10% Xuyên giáp";
                break;
            }
            case 5: { // 10% Crit Dame
                p.wishStats.critDame += 1;
                wishMsg = "10% Sát thương chí mạng";
                break;
            }
            case 6: { // 10% decDodge
                p.wishStats.decDodge += 1;
                wishMsg = "10% Giảm né đối phương";
                break;
            }
            case 7: { // 10% decReact
                p.wishStats.decReact += 1;
                wishMsg = "10% Giảm phản đối phương";
                break;
            }
            case 8: { // 10% decReduc
                p.wishStats.decReduc += 1;
                wishMsg = "10% Giảm miễn đối phương";
                break;
            }
            case 9: { // 10% decPierce
                p.wishStats.decPierce += 1;
                wishMsg = "10% Giảm xuyên đối phương";
                break;
            }
            case 10: { // 5% Final Damage
                p.wishStats.finalDame += 1;
                wishMsg = "5% Sát thương cuối";
                break;
            }
            case 11: { // 5% Final HP
                p.wishStats.finalHp += 1;
                wishMsg = "5% Máu cuối";
                break;
            }
        }

        // Consume Dragon Balls
        for (int id = 1087; id <= 1093; id++) {
            p.item.remove_item47(4, id, 1);
        }
        p.item.update_Inventory(-1, false);

        // Set cooldown and save
        activities.WishManager.gI().setGlobalCooldown();
        activities.WishManager.gI().saveWishData(p);
        p.setin4();
        p.update_money();

        // Broadcast success
        try {
            String announce = "Người chơi [" + p.name + "] đã ước thành công [" + wishMsg + "] từ Rồng Thần!";
            Manager.gI().chatKTG(0, announce, 5);
        } catch (Exception e) {
            core.GameLogger.error("MenuController.Menu_UocRong: Error broadcasting wish success", e);
        }

        Service.send_box_ThongBao_OK(p, "Điều ước của bạn đã thành hiện thực: " + wishMsg);
    }

    private static void Menu_Rebuilt_Item(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                UpgradeItem.show_table_upgrade(p);
                break;
            }
            case 1: {
                UpgradeSuperItem.show_table(p);
                break;
            }
        }
    }

    private static void Menu_Johny(Player p, byte index) throws IOException {

        switch (index) {
            case 0: {
                // Kích khuôntrang bị: Use the Rebuild Table UI (similar to Hoàn Mỹ)
                Rebuild_Item.show_table(p, 11);
                break;
            }
            case 1: {
                Service.Send_UI_Shop(p, 6);
                break;
            }
            case 2: {
                int[] id = new int[] { 18, 19 };
                String[] name = new String[id.length];
                short[] icon = new short[id.length];
                for (int i = 0; i < id.length; i++) {
                    ItemTemplate7 temp = ItemTemplate7.get_it_by_id(id[i]);
                    name[i] = temp.name;
                    icon[i] = temp.icon;
                }
                send_dynamic_menu(p, 992, "Ghép nguyên liệu", name, icon, 7);
                break;
            }
            case 3: {
                send_dynamic_menu(p, 32001, "Khảm đá trang bị",
                        new String[] { "Cửa hàng đá khảm", "Ghép đá khảm", "Đục lỗ vật phẩm", "Khảm vật phẩm",
                                "Lấy đá khảm", "Đá Siêu Cấp", "Đá Thần Thoại" },
                        new short[] { 129, 127, 133, 126, 130, 141, 132, 148 });
                break;
            }
            case 4: {
                send_dynamic_menu(
                        p, 32000, "Cường hoá trang bị", new String[] { "Cường hóa thường", "Cường hóa cao cấp" },
                        new short[] { 131, 163 });
                break;
            }
            case 5: {
                int ver_ = p.getVersion();
                if (ver_ >= 115) {
                    Upgrade_Skin.show_table(p);
                } else {
                    Service.send_box_ThongBao_OK(p, "Hãy sử dụng phiên bản từ 1.1.5 trở lên");
                }
                break;
            }
            case 6: {
                UpgradeDevil.show_table(p, 2);
                break;
            }
            case 7: {
                // Hiện menu 2 lựa chọn: thông tin trái ác quỷ hoặc xem combo
                send_dynamic_menu(p, 9875, "Số Tay Trái Ác Quỷ",
                        new String[] {
                                "Thông tin trái ác quỷ",
                                "Xem Combo Ác Quỷ"
                        }, null);
                break;
            }
        }

    }

    private static void Menu_Machiko(Player p, byte index) throws IOException {
        switch (index) {
            case 0: {
                Service.Send_UI_Shop(p, 20);
                break;
            }

            case 1: {
                ItemFashionP.show_table(p, 103);
                break;
            }
            case 2: {
                ItemFashionP.show_table(p, 102);
                break;
            }
            case 3: {
                ItemFashionP.show_table(p, 105);
                break;
            }
            case 4: {
                ItemFashionP.show_table(p, 108);
                break;
            }
        }
    }

    private static void Menu_Guru(Player p, int index) throws IOException {
        switch (index) {
            case 0: {
                p.is_show_hat = !p.is_show_hat;
                Service.charWearing(p, p, false);
                Service.update_PK(p, p, false);
                p.update_info_to_all();
                Service.send_box_ThongBao_OK(p,
                        p.is_show_hat ? "Đã bật hiển thị nón" : "Đã tắt hiển thị nón");
                break;
            }
            case 1: {
                // Bật/tắt hiển thị pet
                p.isPetVisible = !p.isPetVisible;
                Service.charWearing(p, p, false);
                Service.update_PK(p, p, false);
                p.update_info_to_all();
                p.update_info_all_to_me();
                Service.send_box_ThongBao_OK(p,
                        p.isPetVisible ? "Đã bật hiển thị Pet" : "Đã tắt hiển thị Pet");
                break;
            }
            case 2: {
                p.remove_all_second_body_items();
                Service.send_box_ThongBao_OK(p, "Đã tháo toàn bộ trang bị 2");
                break;
            }
            case 3: {
                p.remove_all_third_body_items();
                Service.send_box_ThongBao_OK(p, "Đã tháo toàn bộ trang bị 3");
                break;
            }
            case 4: {
                send_dynamic_menu(p, 8899, "Dọn hành trang",
                        new String[] { "Xóa Vật Phẩm", "Xóa Trang Bị" }, null);
                break;
            }
            case 5: {
                Service.Send_UI_Shop(p, 119);
                break;
            }
        }
    }

    private static void Menu_Don_Hanh_Trang(Player p, int index) throws IOException {
        switch (index) {
            case 0: {
                Service.send_box_yesno(p, 76, "Thông báo",
                        ("Bạn có muốn xóa hết Vật phẩm trong hành trang không ?. Lưu ý : hãy cất giữ những món đồ quan trọng mà bạn không muốn xóa vào trong rương đồ . Khi xóa sẽ không thể lấy lại nữa"),
                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, -1 });
                break;
            }
            case 1: {
                Service.send_box_yesno(p, 77, "Thông báo",
                        ("Bạn có muốn xóa hết Trang bị trong hành trang không ?. Lưu ý : hãy cất giữ những món đồ quan trọng mà bạn không muốn xóa vào trong rương đồ . Khi xóa sẽ không thể lấy lại nữa"),
                        new String[] { "Đồng ý", "Hủy" }, new byte[] { 2, -1 });
                break;
            }
        }
    }

    private static void Menu_Change_Zone(Player p) throws IOException {
        Message m = new Message(23);
        m.writer().writeByte(p.map.template.max_zone);
        Map[] map_ = Map.get_map_by_id(p.map.template.id);
        for (int i = 0; i < map_.length; i++) {
            int s = map_[i].players.size();
            int max = map_[i].template.max_player;
            // 0
            // green, 1 orange, 2 red, 3 violet, other green
            m.writer().writeByte((s >= max) ? 2 : ((s > (max / 2)) ? 1 : 0));
        }
        p.addmsg(m);
        m.cleanup();
    }

    private static void Show_List_Map_Tele(Player p, int index, int idNPC) throws IOException {
        if (index == 1) {
            p.map_tele = MenuController.ID_MAP_LANG;
            send_dynamic_menu(p, 995, "Dịch chuyển", p.map_tele);
        } else if (index == 0) { // trong lang
            switch (idNPC) {
                case -5: {
                    p.map_tele = new int[] { 1, 2 };
                    break;
                }
                case -144: {
                    p.map_tele = new int[] { 191, 192, 194 };
                    break;
                }
                case -124: {
                    p.map_tele = new int[] { 113, 189 };
                    break;
                }
                case -36: { // nha hang barite
                    p.map_tele = new int[] { 33, 34 };
                    break;
                }
                case -20: { // thi tran orang
                    p.map_tele = new int[] { 17 };
                    break;
                }
            }
            if (p.map_tele != null) {
                send_dynamic_menu(p, 996, "Dịch chuyển", p.map_tele);
            }
        }
    }

    public static void send_dynamic_menu(Player p, int id_npc, String name_npc, String[] list_menu,
            short[] list_icon) throws IOException {
        if (!p.isdie) {
            Message m = new Message(-20);
            if (list_icon == null) {
                m.writer().writeByte(0);
            } else {
                m.writer().writeByte(5);
            }
            m.writer().writeShort(id_npc);
            m.writer().writeByte(0);
            m.writer().writeUTF(name_npc);
            m.writer().writeByte(list_menu.length);
            for (int i = 0; i < list_menu.length; i++) {
                m.writer().writeUTF(list_menu[i]);
                if (list_icon != null) {
                    m.writer().writeShort(list_icon[i]);
                }
            }
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_dynamic_menu(Player p, int id_npc, String name_npc, String[] list_menu,
            short[] list_icon, int b) throws IOException {
        if (!p.isdie) {
            Message m = new Message(-20);
            m.writer().writeByte(3);
            m.writer().writeShort(id_npc);
            m.writer().writeByte(1);
            m.writer().writeUTF(name_npc);
            m.writer().writeByte(list_menu.length);
            for (int i = 0; i < list_menu.length; i++) {
                // m.writer().writeUTF(list_menu[i]);
                // if (list_icon != null) {
                // m.writer().writeShort(list_icon[i]);
                // } else {
                // m.writer().writeShort(-1);
                // }
                // m.writer().writeByte(b);
                m.writer().writeUTF(list_menu[i]);
                m.writer().writeShort(list_icon[i]);
                m.writer().writeByte(b);
            }
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_disciple_menu(Player p) throws IOException {
        if (p.myDisciple == null)
            return;
        String[] menu_names = new String[] { "Thông tin", "Trang bị", "Đổi trạng thái", "Về nhà", "Trùng sinh đệ tử",
                "Thức tỉnh đệ tử", "Dạy đệ dùng TAQ", "Khôi phục tiềm năng", "Chỉ số chi tiết" };
        send_dynamic_menu(p, -999, "Đệ tử: " + p.myDisciple.name, menu_names, null);
    }

    private static void send_dynamic_menu(Player p, int id_npc, String name_npc,
            List<String> list_menu, List<Integer> list_icon) throws IOException {
        if (!p.isdie) {
            if (list_icon == null) {
                core.GameLogger.error("MenuController send_dynamic_menu error: list_icon is null for NPC " + id_npc);
                return;
            }
            Message m = new Message(-20);
            m.writer().writeByte(4);
            m.writer().writeShort(id_npc);
            m.writer().writeByte(0);
            m.writer().writeUTF(name_npc);
            m.writer().writeByte(list_menu.size());
            for (int i = 0; i < list_menu.size(); i++) {
                m.writer().writeUTF(list_menu.get(i));
                m.writer().writeShort(list_icon.get(i).shortValue());
            }
            p.addmsg(m);
            m.cleanup();
        }
    }

    public static void send_dynamic_menu(Player p, int idNPC, String title, String[] name)
            throws IOException {
        send_dynamic_menu(p, idNPC, title, name, null);
    }

    public static void send_dynamic_menu(Player p, int idNPC, String title, int[] name)
            throws IOException {
        if (!p.isdie) {
            int mapIDCanGo = p.checkQuest();
            Message m = new Message(-20);
            m.writer().writeByte(1);
            m.writer().writeShort(idNPC);
            m.writer().writeByte(0);
            m.writer().writeUTF(title);
            m.writer().writeByte(name.length);
            for (int i = 0; i < name.length; i++) {
                Map map = Map.get_map_by_id(name[i])[0];
                m.writer().writeUTF(map.template.name);
                if (Manager.gI().server_test) {
                    m.writer().writeByte(map.template.id == p.map.template.id ? 4 : 2);
                    m.writer().writeByte(7);
                } else {
                    m.writer().writeByte(map.template.id == p.map.template.id ? 4 : (name[i] >= mapIDCanGo ? 0 : 2));
                    m.writer().writeByte(name[i] >= mapIDCanGo ? 3 : 7);
                }
            }
            p.addmsg(m);
            m.cleanup();
        }
    }
}
