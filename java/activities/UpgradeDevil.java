package activities;

import client.Player;
import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import template.GiftBox;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.Skill_info;

public class UpgradeDevil {
    private static class BoxConfig {
        String name;
        int inputId1, inputQuantity1, inputIcon1;
        int inputId2, inputQuantity2, inputIcon2;
        int outputId, outputIcon;
        int berryCost, rubyCost, extolCost;
        int successRate;

        BoxConfig(String name, int inputId1, int inputQuantity1, int inputIcon1,
                int inputId2, int inputQuantity2, int inputIcon2,
                int outputId, int outputIcon,
                int berryCost, int rubyCost, int extolCost, int successRate) {
            this.name = name;
            this.inputId1 = inputId1;
            this.inputQuantity1 = inputQuantity1;
            this.inputIcon1 = inputIcon1;
            this.inputId2 = inputId2;
            this.inputQuantity2 = inputQuantity2;
            this.inputIcon2 = inputIcon2;
            this.outputId = outputId;
            this.outputIcon = outputIcon;
            this.berryCost = berryCost;
            this.rubyCost = rubyCost;
            this.extolCost = extolCost;
            this.successRate = successRate;
        }
    }

    private static final BoxConfig[] BOX_CONFIGS = {
            // Rương U
            new BoxConfig("Rương Trang Bị U", 1056, 10, 537, 0, 0, 0,
                    1051, 7, 10_000, 500, 0, 90),
            // Rương SS
            new BoxConfig("Rương Trang Bị SS", 1051, 10, 7, 0, 0, 0,
                    1052, 8, 20_000, 1_000, 0, 90),
            // Rương SR
            new BoxConfig("Rương Trang Bị SR", 1052, 10, 8, 0, 0, 0,
                    1053, 110, 30_000, 2_000, 0, 90),
            // Rương SSS
            new BoxConfig("Rương Trang Bị SSS", 1053, 10, 110, 0, 0, 0,
                    1054, 99, 40_000, 3_000, 0, 90),
            // Rương SSR
            new BoxConfig("Rương Trang Bị SSR", 1054, 10, 99, 0, 0, 0,
                    1055, 127, 50_000, 4_000, 0, 90)
    };

    private static void showBoxTable(Player p, int configIndex) throws IOException {
        if (configIndex < 0 || configIndex >= BOX_CONFIGS.length)
            return;
        BoxConfig config = BOX_CONFIGS[configIndex];
        Message m = new Message(45);
        m.writer().writeByte(20);
        m.writer().writeUTF(config.name);
        int ingredientCount = (config.inputId2 > 0) ? 2 : 1;
        m.writer().writeByte(ingredientCount);
        m.writer().writeShort(config.inputId1);
        m.writer().writeShort(config.inputQuantity1);
        m.writer().writeByte(4);
        m.writer().writeShort(config.inputIcon1);
        if (config.inputId2 > 0) {
            m.writer().writeShort(config.inputId2);
            m.writer().writeShort(config.inputQuantity2);
            m.writer().writeByte(4);
            m.writer().writeShort(config.inputIcon2);
        }
        m.writer().writeInt(config.berryCost);
        m.writer().writeShort(config.rubyCost);
        m.writer().writeInt(config.extolCost);
        m.writer().writeShort(config.outputId);
        m.writer().writeShort(1);
        m.writer().writeByte(4);
        m.writer().writeShort(config.outputIcon);
        m.writer().writeByte(config.successRate);
        p.addmsg(m);
        m.cleanup();
    }

    public static void show_table(Player p, int index) throws IOException {
        switch (index) {
            case 1: {
                Message m = new Message(45);
                m.writer().writeByte(13);
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 2: {
                Message m = new Message(45);
                m.writer().writeByte(8);
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 5: { // che tao dial
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Tạo rương Dial");
                m.writer().writeByte(2);

                m.writer().writeShort(452);
                m.writer().writeShort(10);
                m.writer().writeByte(4);
                m.writer().writeShort(403);

                m.writer().writeShort(453);
                m.writer().writeShort(10);
                m.writer().writeByte(4);
                m.writer().writeShort(404);
                //
                m.writer().writeInt(10_000_000);
                m.writer().writeShort(5000);
                m.writer().writeInt(5000);
                m.writer().writeShort(455);
                m.writer().writeShort(1);
                m.writer().writeByte(4);
                m.writer().writeShort(407);
                m.writer().writeByte(50);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 6: { // che tao ruong tinh tu
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Rương Tinh Tú");
                m.writer().writeByte(2);
                //
                m.writer().writeShort(1021);
                m.writer().writeShort(10); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(659);
                //
                m.writer().writeShort(1020);
                m.writer().writeShort(10); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(653);
                //
                m.writer().writeInt(10_000_000);
                m.writer().writeShort(5_000);
                m.writer().writeInt(5000);
                m.writer().writeShort(1019);
                m.writer().writeShort(1);
                m.writer().writeByte(4);
                m.writer().writeShort(658);
                m.writer().writeByte(80);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 7: { // che tao xu hanh trinh
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Xu hành trình");
                m.writer().writeByte(1); // số ô ghép
                //
                m.writer().writeShort(579); // id
                m.writer().writeShort(100); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(551); // icon
                //
                m.writer().writeInt(200_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(20000);
                m.writer().writeShort(580);
                m.writer().writeShort(1);
                m.writer().writeByte(4);
                m.writer().writeShort(552);
                m.writer().writeByte(20); // ty le
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 8: { // tao trung pet vip
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Trứng Pet VIP");
                m.writer().writeByte(2);
                //
                m.writer().writeShort(1018);
                m.writer().writeShort(3); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(652);
                //
                m.writer().writeShort(1019);
                m.writer().writeShort(3); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(658);
                //
                m.writer().writeInt(20_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(10000);
                m.writer().writeShort(1040);
                m.writer().writeShort(1);
                m.writer().writeByte(4);
                m.writer().writeShort(661);
                m.writer().writeByte(50);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 10: { // che tao tim
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Phẫu Thuật Tim");
                m.writer().writeByte(3);
                //
                m.writer().writeShort(160); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(114); // icon
                //
                m.writer().writeShort(161); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(115); // icon
                //
                m.writer().writeShort(240); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(176); // icon
                //
                m.writer().writeInt(100_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(10000);
                m.writer().writeShort(13001);
                m.writer().writeShort(1);
                m.writer().writeByte(3);
                m.writer().writeShort(242);
                m.writer().writeByte(80);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: {
                showBoxTable(p, index - 11);
                break;
            }
            case 16: {
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Tạo lõi ánh sáng");
                m.writer().writeByte(2);
                //
                m.writer().writeShort(11037); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(3);
                m.writer().writeShort(307); // icon
                //
                m.writer().writeShort(226); // id
                m.writer().writeShort(10); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(182); // icon
                //
                m.writer().writeInt(100_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(10000);
                m.writer().writeShort(11038);
                m.writer().writeShort(1);
                m.writer().writeByte(3);
                m.writer().writeShort(370);
                m.writer().writeByte(50);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 17: {
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Tạo lõi tình yêu");
                m.writer().writeByte(2);
                //
                m.writer().writeShort(11037); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(3);
                m.writer().writeShort(307); // icon
                //
                m.writer().writeShort(226); // id
                m.writer().writeShort(10); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(182); // icon
                //
                m.writer().writeInt(100_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(10000);
                m.writer().writeShort(11039);
                m.writer().writeShort(1);
                m.writer().writeByte(3);
                m.writer().writeShort(371);
                m.writer().writeByte(50);
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 18: {
                Message m = new Message(45);
                m.writer().writeByte(20);
                //
                m.writer().writeUTF("Tạo lõi cao su");
                m.writer().writeByte(2);
                //
                m.writer().writeShort(11037); // id
                m.writer().writeShort(1); // so luong
                m.writer().writeByte(3);
                m.writer().writeShort(307); // icon
                //
                m.writer().writeShort(226); // id
                m.writer().writeShort(10); // so luong
                m.writer().writeByte(4);
                m.writer().writeShort(182); // icon
                //
                m.writer().writeInt(100_000_000);
                m.writer().writeShort(10_000);
                m.writer().writeInt(10000);
                m.writer().writeShort(11040);
                m.writer().writeShort(1);
                m.writer().writeByte(3);
                m.writer().writeShort(372);
                m.writer().writeByte(50); // ty le
                //
                p.addmsg(m);
                m.cleanup();
                break;
            }
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        byte cat = m2.reader().readByte();
        short num = m2.reader().readShort();
        if (act == 9 && cat == 104 && (num == 0 || num == 1)) { // bo skill vao (num =0) va sau khi
                                                                // xong bo vao lai (num = 1)
            Skill_info sk_temp = p.get_skill_temp(id);
            if (sk_temp != null) {
                Message m = new Message(45);
                m.writer().writeByte(9);
                m.writer().writeByte(0);
                m.writer().writeShort(id);
                m.writer().writeByte(104);
                m.writer().writeShort(1);
                p.addmsg(m);
                m.cleanup();
                //
                m = new Message(45);
                m.writer().writeByte(9);
                m.writer().writeByte(1);
                m.writer().writeShort(9);
                m.writer().writeByte(7);
                m.writer().writeShort(50);
                p.addmsg(m);
                m.cleanup();
            }
        } else if (act == 12 && cat == 104 && num == 0) { // bat dau cuong hoa skill
            Skill_info sk_temp = p.get_skill_temp(id);
            if (sk_temp != null) {
                if (sk_temp.lvdevil > 4) {
                    Service.send_box_ThongBao_OK(p,
                            sk_temp.temp.name + " đã được cường hóa tối đa");
                    return;
                }
                int percent = (sk_temp.lvdevil == 0) ? 10 //
                        : ((sk_temp.lvdevil == 1) ? 8 //
                                : ((sk_temp.lvdevil == 2) ? 6 //
                                        : ((sk_temp.lvdevil == 3) ? 5 : 4)));
                p.data_yesno = new int[] { id };
                Service.send_box_yesno(p, 33, "Thông báo",
                        ("Bạn có thật sự muốn cường hóa " + sk_temp.temp.name
                                + " không? Thành công sẽ tăng thêm " + percent
                                + "% vào cấp ác quỷ"),
                        new String[] { "200", "Auto", "Đóng" }, new byte[] { 7, 3, 1 });
            }
        } else if (act == 14 && (id == 29 || id == 158) && cat == 4 && num == 1) { // bo ruong ac quy vao

            Message m = new Message(45);
            m.writer().writeByte(14);
            m.writer().writeByte(0);
            m.writer().writeShort(id);
            m.writer().writeByte(4);
            m.writer().writeShort(1);
            p.addmsg(m);
            m.cleanup();
            //
            m = new Message(45);
            m.writer().writeByte(14);
            m.writer().writeByte(1);
            m.writer().writeShort(9);
            m.writer().writeByte(7);
            m.writer().writeShort(id == 158 ? 100 : 50); // so luong da
            p.addmsg(m);
            m.cleanup();
            // percent
            m = new Message(45);
            m.writer().writeByte(19);
            m.writer().writeByte(id == 158 ? 2 : 5); // ty le
            p.addmsg(m);
            m.cleanup();

        } else if (act == 17 && (id == 29 || id == 158) && cat == 4 && num == 1) { // bat dau upgrade ruong ac quy
            p.data_yesno = new int[] { id };
            Service.send_box_yesno(p, 34, "Thông báo",
                    ("Bạn có muốn nâng " + ItemTemplate4.get_it_by_id(id).name),
                    new String[] { "100", "Auto", "Đóng" }, new byte[] { 7, 3, 1 });

        } else if (act == 20 && id == 455 && cat == 4 && (num == 0 || num == 1)) { // ghep dial
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 452) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Sách công thức");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, 453) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Vỏ ốc");
                    return;
                }
                if (p.get_vang() < 10_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000.000 Beri");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 5_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                    return;
                }
                if (p.get_ngoc() < 5_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Ruby");
                    return;
                }
                boolean suc = 50 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo Rương Dial thành công")
                        : ("Tạo Rương Dial thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-10_000_000);
                p.update_vnd(-5_000);
                p.update_ngoc(-5_000);
                p.update_money();
                p.item.remove_item47(4, 452, 10);
                p.item.remove_item47(4, 453, 10);
                if (suc) {
                    p.item.add_item_bag47(4, 455, 1);
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 1019 && cat == 4 && num == 0) { // ghep ruong tinh tu
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 1020) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Nước tinh tú");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, 1021) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Đá tinh tú");
                    return;
                }
                if (p.get_vang() < 10_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000.000 Beri");
                    return;
                }
                if (p.get_vnd() < 5_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Extol");
                    return;
                }
                if (p.get_ngoc() < 5_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 5.000 Ruby");
                    return;
                }
                boolean suc = 80 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo Rương Tinh Tú thành công")
                        : ("Tạo Rương Tinh Tú thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-10_000_000);
                p.update_ngoc(-5_000);
                p.update_vnd(-5_000);
                p.update_money();
                p.item.remove_item47(4, 1020, 10);
                p.item.remove_item47(4, 1021, 10);
                if (suc) {
                    p.item.add_item_bag47(4, 1019, 1);
                }
                p.item.update_Inventory(-1, false);

            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 580 && cat == 4 && (num == 0 || num == 1)) { // ghep xu hanh trinh
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 579) < 100) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 100 Mảnh xu hành trình");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 20_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 20.000 Extol");
                    return;
                }
                if (p.get_vang() < 200_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 200.000.000 Beri");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 20 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Ghép Xu Hành Trình thành công")
                        : ("Ghép Xu Hành Trình thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-200_000_000);
                p.update_vnd(-20_000);
                p.update_ngoc(-10_000);
                p.update_money();
                p.item.remove_item47(4, 579, 100);
                if (suc) {
                    p.item.add_item_bag47(4, 580, 1);
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 1040 && cat == 4 && (num == 0 || num == 1)) { // trung pet vip
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 1018) < 3) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 3 Trứng Pet");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, 1019) < 3) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 3 Rương Tinh Tú");
                    return;
                }
                if (p.get_vang() < 20_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 20.000.000 Beri");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Extol");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 50 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo Trứng Pet VIP thành công")
                        : ("Tạo Trứng Pet VIP thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-20_000_000);
                p.update_ngoc(-10_000);
                p.update_vnd(-10_000);
                p.update_money();
                p.item.remove_item47(4, 1018, 3);
                p.item.remove_item47(4, 1019, 3);
                if (suc) {
                    p.item.add_item_bag47(4, 1040, 1);
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 13001 && cat == 3 && (num == 0 || num == 1)) {
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 160) < 1) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 1 Trái Sét");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, 161) < 1) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 1 Trái Nham Thạch");
                    return;
                }
                if (p.item.total_item_bag_by_id(4, 240) < 1) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 1 Trái Chấn Thiên");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Extol");
                    return;
                }
                if (p.get_vang() < 100_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 100.000.000 Beri");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 80 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Phẫu thuật tim thành công")
                        : ("Phẫu thuật tim thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-100_000_000);
                p.update_vnd(-10_000);
                p.update_ngoc(-10_000);
                p.update_money();
                p.item.remove_item47(4, 160, 1);
                p.item.remove_item47(4, 161, 1);
                p.item.remove_item47(4, 240, 1);
                if (suc) {
                    int[] itemIds = { 13001, 13002, 13003, 13004, 13005 };
                    int[] chances = { 68, 17, 8, 5, 2 };
                    int rand = Util.random(101);
                    int id_random = itemIds[itemIds.length - 1];
                    int cumulative = 0;
                    for (int i = 0; i < itemIds.length; i++) {
                        cumulative += chances[i];
                        if (rand <= cumulative) {
                            id_random = itemIds[i];
                            break;
                        }
                    }
                    List<GiftBox> list = new ArrayList<>();
                    ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(id_random);
                    if (itemTemplate3 != null) {
                        GiftBox gb = new GiftBox();
                        gb.id = (short) id_random;
                        gb.type = 3;
                        gb.name = itemTemplate3.name;
                        gb.icon = itemTemplate3.icon;
                        gb.num = 1;
                        gb.color = itemTemplate3.color;
                        list.add(gb);
                    }
                    if (!list.isEmpty()) {
                        Service.send_gift(p, 1, "", "", list, false);
                    }
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && (id >= 1051 && id <= 1055) && cat == 4 && num == 0) {
            for (int i = 0; i < BOX_CONFIGS.length; i++) {
                if (id == BOX_CONFIGS[i].outputId) {
                    processBox(p, i);
                    break;
                }
            }
        } else if (act == 20 && id == 11038 && cat == 3 && (num == 0 || num == 1)) {
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 226) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Đá Hải Thạch cấp 6");
                    return;
                }
                if (!p.item.hasItemBag3(11037)) {
                    Service.send_box_ThongBao_OK(p, "Không đủ Lõi năng lượng");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Extol");
                    return;
                }
                if (p.get_vang() < 100_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 100.000.000 Beri");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 50 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo lõi áng sáng thành công")
                        : ("Tạo lõi áng sáng thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-100_000_000);
                p.update_vnd(-10_000);
                p.update_ngoc(-10_000);
                p.update_money();
                p.item.remove_item47(4, 226, 10);
                p.item.remove_ItemBag3(11037);
                if (suc) {
                    List<GiftBox> list = new ArrayList<>();
                    ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(11038);
                    if (itemTemplate3 != null) {
                        GiftBox gb = new GiftBox();
                        gb.id = (short) itemTemplate3.id;
                        gb.type = 3;
                        gb.name = itemTemplate3.name;
                        gb.icon = itemTemplate3.icon;
                        gb.num = 1;
                        gb.color = itemTemplate3.color;
                        list.add(gb);
                    }
                    if (!list.isEmpty()) {
                        Service.send_gift(p, 1, "", "", list, false);
                    }
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 11039 && cat == 3 && (num == 0 || num == 1)) {
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 226) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Đá Hải Thạch cấp 6");
                    return;
                }
                if (!p.item.hasItemBag3(11037)) {
                    Service.send_box_ThongBao_OK(p, "Không đủ Lõi năng lượng");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Extol");
                    return;
                }
                if (p.get_vang() < 100_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 100.000.000 Beri");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 50 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo lõi tình yêu thành công")
                        : ("Tạo lõi tình yêu thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-100_000_000);
                p.update_vnd(-10_000);
                p.update_ngoc(-10_000);
                p.update_money();
                p.item.remove_item47(4, 226, 10);
                p.item.remove_ItemBag3(11037);

                if (suc) {
                    List<GiftBox> list = new ArrayList<>();
                    ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(11039);
                    if (itemTemplate3 != null) {
                        GiftBox gb = new GiftBox();
                        gb.id = (short) itemTemplate3.id;
                        gb.type = 3;
                        gb.name = itemTemplate3.name;
                        gb.icon = itemTemplate3.icon;
                        gb.num = 1;
                        gb.color = itemTemplate3.color;
                        list.add(gb);
                    }
                    if (!list.isEmpty()) {
                        Service.send_gift(p, 1, "", "", list, false);
                    }
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        } else if (act == 20 && id == 11040 && cat == 3 && (num == 0 || num == 1)) {
            if (p.item.able_bag() > 0) {
                if (p.item.total_item_bag_by_id(4, 226) < 10) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10 Đá Hải Thạch cấp 6");
                    return;
                }
                if (!p.item.hasItemBag3(11037)) {
                    Service.send_box_ThongBao_OK(p, "Không đủ Lõi năng lượng");
                    return;
                }
                p.refresh_vnd();
                if (p.get_vnd() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Extol");
                    return;
                }
                if (p.get_vang() < 100_000_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 100.000.000 Beri");
                    return;
                }
                if (p.get_ngoc() < 10_000) {
                    Service.send_box_ThongBao_OK(p, "Không đủ 10.000 Ruby");
                    return;
                }
                boolean suc = 50 > Util.random(120);
                Message m = new Message(45);
                m.writer().writeByte(21);
                m.writer().writeByte(suc ? 1 : 0);
                m.writer().writeUTF(suc ? ("Tạo lõi cao su thành công")
                        : ("Tạo lõi cao su thất bại"));
                p.addmsg(m);
                m.cleanup();
                //
                p.update_vang(-100_000_000);
                p.update_vnd(-10_000);
                p.update_ngoc(-10_000);
                p.update_money();
                p.item.remove_item47(4, 226, 10);
                p.item.remove_ItemBag3(11037);
                if (suc) {
                    List<GiftBox> list = new ArrayList<>();
                    ItemTemplate3 itemTemplate3 = ItemTemplate3.get_it_by_id(11040);
                    if (itemTemplate3 != null) {
                        GiftBox gb = new GiftBox();
                        gb.id = (short) itemTemplate3.id;
                        gb.type = 3;
                        gb.name = itemTemplate3.name;
                        gb.icon = itemTemplate3.icon;
                        gb.num = 1;
                        gb.color = itemTemplate3.color;
                        list.add(gb);
                    }
                    if (!list.isEmpty()) {
                        Service.send_gift(p, 1, "", "", list, false);
                    }
                }
                p.item.update_Inventory(-1, false);
            } else {
                Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            }
        }
        //
    }

    private static void processBox(Player p, int configIndex) throws IOException {
        if (configIndex < 0 || configIndex >= BOX_CONFIGS.length)
            return;
        BoxConfig cfg = BOX_CONFIGS[configIndex];
        if (p.item.able_bag() <= 0) {
            Service.send_box_ThongBao_OK(p, "Chừa ít nhất một ô trống để ghép vật phẩm này");
            return;
        }
        if (p.item.total_item_bag_by_id(4, cfg.inputId1) < cfg.inputQuantity1) {
            Service.send_box_ThongBao_OK(p, "Không đủ " + cfg.inputQuantity1 + " " +
                    ItemTemplate4.get_it_by_id(cfg.inputId1).name);
            return;
        }
        if (cfg.inputId2 > 0 && p.item.total_item_bag_by_id(4, cfg.inputId2) < cfg.inputQuantity2) {
            Service.send_box_ThongBao_OK(p, "Không đủ " + cfg.inputQuantity2 + " " +
                    ItemTemplate4.get_it_by_id(cfg.inputId2).name);
            return;
        }
        if (p.get_vang() < cfg.berryCost) {
            Service.send_box_ThongBao_OK(p, "Không đủ " + cfg.berryCost + " Beri");
            return;
        }
        if (p.get_ngoc() < cfg.rubyCost) {
            Service.send_box_ThongBao_OK(p, "Không đủ " + cfg.rubyCost + " Ruby");
            return;
        }
        boolean suc = Util.random(100) < cfg.successRate;
        Message m = new Message(45);
        m.writer().writeByte(21);
        m.writer().writeByte(suc ? 1 : 0);
        m.writer().writeUTF(suc ? ("Tạo " + cfg.name + " thành công") : "Tạo " + cfg.name + " thất bại");
        p.addmsg(m);
        m.cleanup();
        p.update_vang(-cfg.berryCost);
        p.update_ngoc(-cfg.rubyCost);
        p.update_money();
        p.item.remove_item47(4, cfg.inputId1, cfg.inputQuantity1);
        if (cfg.inputId2 > 0)
            p.item.remove_item47(4, cfg.inputId2, cfg.inputQuantity2);
        if (suc)
            p.item.add_item_bag47(4, cfg.outputId, 1);
        p.item.update_Inventory(-1, false);
    }

}
