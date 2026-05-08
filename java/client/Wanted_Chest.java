package client;

import core.Service;
import core.Util;
import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import template.GiftBox;
import template.ItemTemplate4;

public class Wanted_Chest {
    public static final String[] NAME = new String[] { "Rương gỗ", "Rương vàng", "Rương ma thuật",
            "Rương khổng lồ", "Rương siêu ma thuật", "Rương thần thoại" };
    public static final byte[] TYPE = new byte[] {
            10, // id 0 -> OPEN_BOX_WANTED_GO
            11, // id 1 -> OPEN_BOX_WANTED_VANG
            12, // id 2 -> OPEN_BOX_WANTED_MATHUAT
            13, // id 3 -> OPEN_BOX_WANTED_KHONGL0
            14, // id 4 -> OPEN_BOX_WANTED_SIEUMATHUAT
            15 // id 5 -> OPEN_BOX_WANTED_THANTHOAI
    };

    public short maxTimeUse, Ruby, id;
    public long timeUse;

    public static void send_box(Player p) throws IOException {
        for (int i = 0; i < p.wanted_chest.length; i++) {
            if (p.wanted_chest[i] != null) {
                Wanted_Chest temp = p.wanted_chest[i];
                Message m = new Message(-86);
                m.writer().writeByte(0);
                m.writer().writeByte(i);
                //
                m.writer().writeShort(temp.id);
                m.writer().writeShort(500 + temp.id);
                m.writer().writeByte(109);
                m.writer().writeUTF(NAME[temp.id]);
                m.writer().writeShort(temp.maxTimeUse);
                long time_minute = temp.timeUse - System.currentTimeMillis();
                if (time_minute < 0) {
                    time_minute = 0;
                }
                time_minute /= 60_000L;
                m.writer().writeShort((short) time_minute);
                m.writer().writeShort(temp.Ruby);
                //
                p.addmsg(m);
                m.cleanup();
            } else {
                Message m = new Message(-86);
                m.writer().writeByte(1);
                m.writer().writeByte(i);
                p.addmsg(m);
                m.cleanup();
            }
        }
    }

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        short id = m2.reader().readShort();
        if (act == 0) {
            for (int i = 0; i < p.wanted_chest.length; i++) {
                if (p.wanted_chest[i] != null && p.wanted_chest[i].id == id) {
                    long time = (p.wanted_chest[i].timeUse - System.currentTimeMillis());
                    if (time < 0) {
                        List<GiftBox> list_gift = new ArrayList<>();
                        int min = (p.wanted_chest[i].id + 1) * 100_000;
                        int max = (p.wanted_chest[i].id + 1) * 1_000_000;
                        int beri_receiv = Util.random(min, max);
                        GiftBox gb_beri = new GiftBox();
                        ItemTemplate4 it_temp4 = ItemTemplate4.get_it_by_id(0);
                        if (it_temp4 != null) {
                            gb_beri.id = it_temp4.id;
                            gb_beri.type = 4;
                            gb_beri.name = it_temp4.name;
                            gb_beri.icon = it_temp4.icon;
                            gb_beri.num = beri_receiv;
                            gb_beri.color = 0;
                            list_gift.add(gb_beri);
                        }
                        byte typeGift = Wanted_Chest.TYPE[p.wanted_chest[i].id];
                        Service.send_gift(p, typeGift, Wanted_Chest.NAME[p.wanted_chest[i].id], "", list_gift, true);
                        p.wanted_chest[i] = null;
                        Wanted_Chest.send_box(p);
                    } else {
                        Service.send_box_ThongBao_OK(p,
                                "Mở sau " + Util.get_time_str_by_sec2(time) + " nữa");
                    }
                    break;
                }
            }
        }
    }

    public static void receiv_ruong(Player p) throws IOException {
        for (int i = 0; i < p.wanted_chest.length; i++) {
            if (p.wanted_chest[i] == null) {
                p.wanted_chest[i] = new Wanted_Chest();
                p.wanted_chest[i].id = (short) Util.random(6);
                p.wanted_chest[i].timeUse = System.currentTimeMillis() + 60_000L;
                p.wanted_chest[i].Ruby = 1;
                p.wanted_chest[i].maxTimeUse = 0;
                Wanted_Chest.send_box(p);
                break;
            }
        }
    }

}
