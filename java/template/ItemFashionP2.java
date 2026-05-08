package template;

import java.util.ArrayList;
import java.util.List;

import core.Util;

public class ItemFashionP2 {
    public short id;
    public boolean is_use;
    public byte level;
    public List<Option> op;
    public long expirationTime;
    // private static final int[] POSSIBLE_OPTION_IDS = { 1, 2, 10, 11, 12, 13, 14,
    // 17, 49, 50, 51, 52, 53, 56, 63 }; // id random
    private static final int[] POSSIBLE_OPTION_IDS = { 2, 12, 13, 14, 17, 49, 50, 51, 52, 53, 56, 63 }; // Phép thuật,
                                                                                                        // né tránh,
                                                                                                        // xuyên giáp,
                                                                                                        // phản đòn,
                                                                                                        // máu, giảm chí
                                                                                                        // mạng, giảm
                                                                                                        // xuyên giáp,
                                                                                                        // giảm né
                                                                                                        // tránh, giảm
                                                                                                        // phản đòn,
                                                                                                        // miễn thương,
                                                                                                        // máu cuối,
                                                                                                        // giảm miễn
                                                                                                        // thương

    public static String formatOption(Option op) {
        String name = getOptionNameById(op.id);
        if (op.id >= 91 && op.id <= 95) {
            return "+" + op.getParam() + " " + name;
        }
        int displayParam = op.getParam() / 10;
        return "+" + displayParam + "% " + name;
    }

    public static String getOptionNameById(int id) {
        switch (id) {
            case 1:
                return "Sát thương";
            case 2:
                return "Phép thuật";
            case 10:
                return "Chí mạng";
            case 11:
                return "Sát thương chí mạng";
            case 12:
                return "Né tránh";
            case 13:
                return "Xuyên giáp";
            case 14:
                return "Phản đòn";
            case 17:
                return "Máu";
            case 46:
                return "Sát thương cuối";
            case 49:
                return "Giảm chí mạng";
            case 50:
                return "Giảm xuyên giáp";
            case 51:
                return "Giảm né tránh";
            case 52:
                return "Giảm phản đòn";
            case 53:
                return "Miễn thương";
            case 56:
                return "Máu cuối";
            case 63:
                return "Giảm miễn thương";
            case 68:
                return "Tăng % exp skill khi đánh quái";
            case 91:
                return "Giảm tiềm năng sức mạnh đối thủ";
            case 92:
                return "Giảm tiềm năng phòng thủ đối thủ";
            case 93:
                return "Giảm tiềm năng thể lực đối thủ";
            case 94:
                return "Giảm tiềm năng tinh thần đối thủ";
            case 95:
                return "Giảm tiềm năng nhanh nhẹn đối thủ";
            default:
                return "Chỉ số chưa rõ";
        }
    }

    public static List<Option> randomOptionsForFashion(short fashionId) {
        // Tạo một list chung để chứa tất cả các chỉ số (cả cố định và ngẫu nhiên)
        List<Option> newOptions = new ArrayList<>();

        // Nếu id = 1, 31, 32 thì thêm các dòng chỉ số cố định vào trước
        if (fashionId == 1 || fashionId == 31 || fashionId == 32) {
            newOptions.add(new Option(11, 30000)); // 3000% Sát thương chí mạng
            newOptions.add(new Option(46, 3000)); // 300% Sát thương cuối
            newOptions.add(new Option(91, 10000)); // Giảm sức mạnh
            newOptions.add(new Option(92, 10000)); // Giảm phòng thủ
            newOptions.add(new Option(93, 10000)); // Giảm thể lực
            newOptions.add(new Option(94, 10000)); // Giảm tinh thần
            newOptions.add(new Option(95, 10000)); // Giảm nhanh nhẹn
        } else if (fashionId == 41) {
            newOptions.add(new Option(11, 15000)); // 1500% Sát thương chí mạng
            newOptions.add(new Option(46, 1500)); // 150% Sát thương cuối
            newOptions.add(new Option(91, 5000)); // Giảm sức mạnh
            newOptions.add(new Option(92, 5000)); // Giảm phòng thủ
            newOptions.add(new Option(93, 5000)); // Giảm thể lực
            newOptions.add(new Option(94, 5000)); // Giảm tinh thần
            newOptions.add(new Option(95, 5000)); // Giảm nhanh nhẹn
            newOptions.add(new Option(68, 2000)); // 200% Tăng Exp Skill
        }

        // Tiếp tục chạy phần tạo chỉ số ngẫu nhiên cho mọi id (bao gồm cả id = 1)
        FashionStatRange range = getRangeById(fashionId);
        int numOptions = Util.random(range.minLines, range.maxLines);
        int[] pool = range.possibleOptionIds != null ? range.possibleOptionIds : POSSIBLE_OPTION_IDS;

        for (int i = 0; i < numOptions; i++) {
            int optionId = pool[Util.random(0, pool.length)];
            int param = Util.random(range.minPercent, range.maxPercent) * 10; // lưu x10
            newOptions.add(new Option(optionId, param));
        }

        // Trả về list tổng hợp
        return newOptions;
    }

    public static class FashionStatRange {
        public short id;
        public int minPercent, maxPercent;
        public int minLines, maxLines;
        public int[] possibleOptionIds;

        public FashionStatRange(short id, int minPercent, int maxPercent, int minLines, int maxLines) {
            this.id = id;
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
            this.minLines = minLines;
            this.maxLines = maxLines;
        }

        public FashionStatRange(short id, int minPercent, int maxPercent, int minLines, int maxLines,
                int[] customOptionIds) {
            this(id, minPercent, maxPercent, minLines, maxLines);
            this.possibleOptionIds = customOptionIds;
        }

        public String getInfoText() {
            String text = "";
            if (id == 1 || id == 31 || id == 32) {
                text += "Chỉ số đặc biệt:\n"
                        + "+3000% Sát thương chí mạng\n"
                        + "+300% Sát thương cuối\n"
                        + "+10000 Giảm tiềm năng sức mạnh đối thủ\n"
                        + "+10000 Giảm tiềm năng phòng thủ đối thủ\n"
                        + "+10000 Giảm tiềm năng thể lực đối thủ\n"
                        + "+10000 Giảm tiềm năng tinh thần đối thủ\n"
                        + "+10000 Giảm tiềm năng nhanh nhẹn đối thủ\n\n";
            } else if (id == 41) {
                text += "Chỉ số đặc biệt:\n"
                        + "+1500% Sát thương chí mạng\n"
                        + "+150% Sát thương cuối\n"
                        + "+5000 Giảm tiềm năng sức mạnh đối thủ\n"
                        + "+5000 Giảm tiềm năng phòng thủ đối thủ\n"
                        + "+5000 Giảm tiềm năng thể lực đối thủ\n"
                        + "+5000 Giảm tiềm năng tinh thần đối thủ\n"
                        + "+5000 Giảm tiềm năng nhanh nhẹn đối thủ\n"
                        + "+200% Tăng EXP kỹ năng khi đánh quái\n\n";
            }
            text += "Ngẫu nhiên chỉ số \n"
                    + "(+" + minPercent + "% đến " + maxPercent + "%)\n"
                    + "Ngẫu nhiên từ " + minLines + " đến " + maxLines + " dòng";
            return text;
        }
    }

    public static final List<FashionStatRange> RANGE_CONFIG = new ArrayList<>();
    private static final FashionStatRange DEFAULT_RANGE = new FashionStatRange((short) -1, 10, 100, 1, 10);

    static {
        RANGE_CONFIG.add(new FashionStatRange((short) 126, 10, 100, 1, 10)); // Dragon
        RANGE_CONFIG.add(new FashionStatRange((short) 102, 10, 100, 1, 10)); // Roger
        RANGE_CONFIG.add(new FashionStatRange((short) 122, 10, 100, 1, 10)); // Kaido

        // Cấu hình cho ID 152
        // RANGE_CONFIG.add(new FashionStatRange((short) 1, 10, 100, 1, 10));

        addGroup(new short[] { 98, 99, 100, 101, 120, 121 }, 10, 100, 1, 10);
        addGroup(new short[] { 123, 124, 125, 127 }, 10, 100, 1, 10);
    }

    private static void addGroup(short[] ids, int minPercent, int maxPercent, int minLines, int maxLines) {
        for (short id : ids) {
            RANGE_CONFIG.add(new FashionStatRange(id, minPercent, maxPercent, minLines, maxLines));
        }
    }

    public static FashionStatRange getRangeById(short id) {
        for (FashionStatRange r : RANGE_CONFIG) {
            if (r.id == id) {
                return r;
            }
        }
        return DEFAULT_RANGE;
    }

}
