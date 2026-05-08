package template;

import java.util.List;

public class Part {
    public static List<Part> ENTRY;
    public byte type;
    public PartImg[] pi;
    public short id;

    public Part(byte type) {
        this.type = type;
        switch (type) {
            case 0: {
                pi = new PartImg[5];
                break;
            }
            case 1: {
                pi = new PartImg[20];
                break;
            }
            case 2: {
                pi = new PartImg[15];
                break;
            }
            case 3: {
                pi = new PartImg[24];
                break;
            }
            case 4: {
                pi = new PartImg[2];
                break;
            }
            case 5: {
                pi = new PartImg[2];
                break;
            }
        }
    }

    public static Part get_part(short index) {
        for (int i = 0; i < ENTRY.size(); i++) {
            if (ENTRY.get(i).id == index) {
                return ENTRY.get(i);
            }
        }
        return null;
    }
}
