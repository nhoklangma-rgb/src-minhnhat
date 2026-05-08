package client;

import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import core.Service;

public class Pet {
    public static List<Pet> ENTRY = new ArrayList<>();
    public short id, icon, frame;
    public String name;
    public byte type;

    public static void process(Player p, Message m2) throws IOException {
        byte act = m2.reader().readByte();
        if (act == 3) { // show table
            Pet.show_inven(p);
        } else if (act == 4) {
            try {
                byte type = m2.reader().readByte();
                short id = m2.reader().readShort();
                if (type == 1) { // mac
                    MyPet pet_select = null;
                    for (int i = 0; i < p.my_pet.size(); i++) {
                        p.my_pet.get(i).isUse = false;
                        if (p.my_pet.get(i).id == id && !p.my_pet.get(i).isUse) {
                            pet_select = p.my_pet.get(i);
                        }
                    }
                    if (pet_select != null && !pet_select.isUse) {
                        pet_select.isUse = true;
                        Pet.show_inven(p);
                        Service.send_box_ThongBao_OK(p,
                                "Trang bị " + pet_select.template.name + " thành công");
                        p.update_info_to_all();
                    }
                } else if (type == 0) { // thao
                    MyPet pet_select = null;
                    for (int i = 0; i < p.my_pet.size(); i++) {
                        if (p.my_pet.get(i).id == id && p.my_pet.get(i).isUse) {
                            pet_select = p.my_pet.get(i);
                            break;
                        }
                    }
                    if (pet_select != null && pet_select.isUse) {
                        pet_select.isUse = false;
                        Pet.show_inven(p);
                        Service.send_box_ThongBao_OK(p,
                                "Tháo " + pet_select.template.name + " thành công");
                        p.update_info_to_all();
                    }
                }
            } catch (IOException e) {
                core.GameLogger.error("Pet.process: Error processing pet action", e);
            }
        }
    }

    private static void show_inven(Player p) throws IOException {
        Message m = new Message(-80);
        m.writer().writeByte(3);
        m.writer().writeShort(p.my_pet.size());
        for (int i = 0; i < p.my_pet.size(); i++) {
            p.my_pet.get(i).id = (short) i;
            m.writer().writeShort(p.my_pet.get(i).id);
            m.writer().writeUTF(p.my_pet.get(i).template.name);
            m.writer().writeUTF(p.my_pet.get(i).template.name);
            m.writer().writeShort(p.my_pet.get(i).template.icon);
            m.writer().writeByte(110);
            m.writer().writeByte(p.my_pet.get(i).isUse ? 1 : 0);
            m.writer().writeByte(0);
        }
        p.addmsg(m);
        m.cleanup();
        //
        for (int i = 0; i < p.map.players.size(); i++) {
            Player p0 = p.map.players.get(i);
            Service.pet(p, p0, false);
        }
    }

    public static Pet getTemplate(int id) {
        for (int i = 0; i < ENTRY.size(); i++) {
            if (ENTRY.get(i).id == id) {
                return ENTRY.get(i);
            }
        }
        return null;
    }
}
