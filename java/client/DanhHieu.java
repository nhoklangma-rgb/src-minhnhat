package client;

import io.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import core.Service;
import template.Option;

public class DanhHieu {
    public static List<DanhHieu> ENTRY = new ArrayList<>();
    public int id;
    public String name;
    public int eff;
    public List<Option> options = new ArrayList<>();

    public static DanhHieu getTemplate(int id) {
        for (int i = 0; i < ENTRY.size(); i++) {
            if (ENTRY.get(i).id == id) {
                return ENTRY.get(i);
            }
        }
        return null;
    }
}
