package template;

import client.Player;
import map.Map;

import java.io.DataOutputStream;
import java.io.IOException;

public class InfoMemList {
    public int id;
    public String name;
    public short level;
    public short head;
    public short hair;
    public short hat;
    public String info;
    public long thongthao;
    public short rank;

    public static void WriteInfoMemList(DataOutputStream dos, InfoMemList temp) throws IOException {
        Player p0 = Map.get_player_by_name_allmap(temp.name);
        dos.writeInt(temp.id);
        dos.writeUTF(temp.name);
        dos.writeShort(temp.level);
        dos.writeShort(temp.head);
        dos.writeShort(temp.hair);
        dos.writeShort(temp.hat);
        dos.writeByte(p0 != null ? 1 : 0);
        dos.writeUTF(temp.info);
        dos.writeShort(temp.rank);
    }
    public short[] part;
}
