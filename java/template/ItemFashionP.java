package template;

import client.Player;
import io.Message;
import java.io.IOException;

public class ItemFashionP {
    public byte category;
    public short id;
    public short icon;
    public boolean is_use;

    public static void show_table(Player p, int type) throws IOException {
        switch (type) {
            case 103: {
                int ver_ = p.getVersion();
                Message m = new Message(-19);
                m.writer().writeByte(103);
                m.writer().writeUTF("Tiệm tóc");
                m.writer().writeByte(103);
                m.writer().writeShort(ItemHair.get_size_type(103));
                for (int i = 0; i < ItemHair.ENTRYS.size(); i++) {
                    ItemHair temp = ItemHair.ENTRYS.get(i);
                    if (temp.type == 103) {
                        // Gửi ID dựa trên phiên bản client
                        if (ver_ >= 129) { // Client mới (ver 129 trở lên)
                            m.writer().writeShort(temp.ID); // 2 bytes
                        } else { // Client cũ (ver 127 hoặc thấp hơn)
                            m.writer().writeByte(temp.ID); // 1 byte
                        }
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(0);
                        m.writer().writeShort(temp.idIcon);
                        m.writer().writeShort(0);
                        boolean hasFashion = p.check_itfashionP(temp.ID, 103) != null;
                        if (hasFashion) {
                            m.writer().writeInt(0); // Ghi beri nếu đã có
                            m.writer().writeShort(0); // Ghi ruby nếu đã có
                        } else {
                            m.writer().writeInt(temp.beri); // Ghi beri nếu chưa có
                            m.writer().writeShort(temp.ruby); // Ghi ruby nếu chưa có
                        }
                    }
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 102: {
                Message m = new Message(-19);
                m.writer().writeByte(102);
                m.writer().writeUTF("Đóng thuyền");
                m.writer().writeByte(102);
                m.writer().writeShort(ItemBoat.ENTRYS.size());
                for (int i = 0; i < ItemBoat.ENTRYS.size(); i++) {
                    m.writer().writeByte(ItemBoat.ENTRYS.get(i).id);
                    m.writer().writeUTF(ItemBoat.ENTRYS.get(i).name);
                    m.writer().writeByte(ItemBoat.ENTRYS.get(i).type);
                    m.writer().writeShort(ItemBoat.ENTRYS.get(i).idimg);
                    m.writer().writeShort(ItemBoat.ENTRYS.get(i).icon);
                    //
                    m.writer().writeInt(0);
                    ItemBoatP my_boat = p.check_itboat(ItemBoat.ENTRYS.get(i).id);
                    m.writer().writeShort(my_boat == null ? 5 : 0);
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 105: {
                int ver_ = p.getVersion();
                Message m = new Message(-19);
                m.writer().writeByte(105);
                m.writer().writeUTF("Thời trang");
                m.writer().writeByte(105);
                m.writer().writeShort(ItemFashion.ENTRYS.size());
                for (int i = 0; i < ItemFashion.ENTRYS.size(); i++) {
                    if (ver_ >= 129) { // Client mới (ver 129 trở lên)
                        m.writer().writeShort(ItemFashion.ENTRYS.get(i).ID); // 2 bytes cho ver 129
                    } else { // Client cũ (ver 127 hoặc thấp hơn)
                        m.writer().writeByte(ItemFashion.ENTRYS.get(i).ID); // 1 byte cho ver 127
                    }
                    m.writer().writeUTF(ItemFashion.ENTRYS.get(i).name);
                    ItemFashionP2 myFashion = p.check_fashion(ItemFashion.ENTRYS.get(i).ID);
                    String info = ItemFashion.ENTRYS.get(i).info;
                    if (myFashion != null && myFashion.op != null && !myFashion.op.isEmpty()) {
                        for (Option op : myFashion.op) {
                            info += "\n " + ItemFashionP2.formatOption(op);
                        }
                    } else {
                        ItemFashionP2.FashionStatRange range = ItemFashionP2.getRangeById(ItemFashion.ENTRYS.get(i).ID);
                        if (range != null) {
                            info += "\n" + range.getInfoText();
                        }
                    }
                    if (myFashion != null) {
                        if (myFashion.level > 0) {
                            info += "\n (+" + myFashion.level + ") Toàn bộ chỉ số + "
                                    + Upgrade_Skin_Info.get_op_level(myFashion.level) + "%";
                        }
                        if (myFashion.expirationTime == -1) {
                            info += "\n Hạn sử dụng vĩnh viễn";
                        } else {
                            long currentTime = System.currentTimeMillis(); // Thời gian hiện tại
                            long remainingTime = myFashion.expirationTime - currentTime;
                            if (remainingTime > 0) {
                                long days = remainingTime / (1000 * 60 * 60 * 24); // Tính số ngày
                                long hours = (remainingTime / (1000 * 60 * 60)) % 24; // Tính số giờ
                                long minutes = (remainingTime / (1000 * 60)) % 60; // Tính số phút
                                if (days > 0) {
                                    info += "\n Hạn sử dụng " + days + " ngày";
                                } else if (hours > 0) {
                                    info += "\n Hạn sử dụng " + hours + " giờ";
                                } else if (minutes > 0) {
                                    info += "\n Hạn sử dụng " + minutes + " phút";
                                }
                            } else {
                                info += "\n Đã hết hạn sử dụng ";
                                p.check_and_remove_expired_fashion();
                            }
                        }
                    }

                    m.writer().writeUTF(info);
                    m.writer().writeShort(ItemFashion.ENTRYS.get(i).idIcon);
                    m.writer().writeByte(ItemFashion.ENTRYS.get(i).mWearing.length);
                    for (int j = 0; j < ItemFashion.ENTRYS.get(i).mWearing.length; j++) {
                        m.writer().writeShort(ItemFashion.ENTRYS.get(i).mWearing[j]);
                    }
                    if (myFashion == null && ItemFashion.ENTRYS.get(i).price == -1) { // not sale
                        m.writer().writeInt(-1);
                        m.writer().writeShort(0);
                    } else {
                        m.writer().writeInt(0);
                        if (myFashion != null) {
                            m.writer().writeShort(0);
                        } else {
                            m.writer().writeShort(ItemFashion.ENTRYS.get(i).price);
                        }
                    }
                    if (ver_ >= 115) {
                        if (myFashion != null) {
                            m.writer().writeByte(myFashion.level);
                        } else {
                            m.writer().writeByte(0);
                        }
                    }
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }
            case 108: {
                int ver_ = p.getVersion();
                Message m = new Message(-19);
                m.writer().writeByte(112);
                m.writer().writeUTF("Thẩm mỹ viện");
                m.writer().writeByte(108);
                m.writer().writeShort(ItemHair.get_size_type(108));
                for (int i = 0; i < ItemHair.ENTRYS.size(); i++) {
                    ItemHair temp = ItemHair.ENTRYS.get(i);
                    if (temp.type == 108) {
                        // Gửi ID dựa trên phiên bản client
                        if (ver_ >= 129) { // Client mới (ver 129 trở lên)
                            m.writer().writeShort(temp.ID); // 2 bytes
                        } else { // Client cũ (ver 127 hoặc thấp hơn)
                            m.writer().writeByte(temp.ID); // 1 byte
                        }
                        m.writer().writeUTF(temp.name);
                        m.writer().writeByte(0);
                        m.writer().writeShort(temp.idIcon);
                        m.writer().writeShort(0);
                        m.writer().writeInt(0);
                        if (p.check_itfashionP(temp.ID, 108) != null) {
                            m.writer().writeShort(0);
                        } else {
                            m.writer().writeShort(500);
                        }
                    }
                }
                p.addmsg(m);
                m.cleanup();
                break;
            }
        }
    }
}
