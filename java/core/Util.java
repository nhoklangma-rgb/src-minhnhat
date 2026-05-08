package core;

import org.joda.time.DateTime;
import template.Top_Dame;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Util {
    private static final Random random = new Random();

    public static byte[] loadfile(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4096)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    public static String number_to_words(long number) {
        if (number >= 1_000_000_000) {
            long billions = number / 1_000_000_000; // Số Tỷ
            long millions = (number % 1_000_000_000) / 100_000_000; // Lấy phần triệu làm 1 chữ số
            if (millions > 0) {
                return billions + " Tỷ " + millions;
            } else {
                return billions + " Tỷ";
            }
        } else if (number >= 1_000_000) {
            long millions = number / 1_000_000; // Số Triệu
            return millions + " Triệu";
        } else if (number >= 1_000) {
            return (number / 1_000) + " Nghìn";
        } else {
            return String.valueOf(number);
        }
    }

    public static String get_time_str_by_sec2(long time_ship) {
        time_ship /= 1000;
        int input = (int) time_ship;
        int numberOfDays;
        int numberOfHours;
        int numberOfMinutes;
        int numberOfSeconds;
        numberOfDays = input / 86400;
        numberOfHours = (input % 86400) / 3600;
        numberOfMinutes = ((input % 86400) % 3600) / 60;
        numberOfSeconds = ((input % 86400) % 3600) % 60;
        return String.format("%s Ngày %s Giờ %s Phút %s Giây", numberOfDays, numberOfHours, numberOfMinutes,
                numberOfSeconds);
    }

    public static boolean is_DayofWeek(int day) {
        // thu2 = 1 ->
        // thu3 = 2->
        // thu4 = 3 ->
        // thu5 = 4 ->
        // thu6 = 5 ->
        // thu7 = 6 ->
        // chu nhat = 7
        DateTime dateTime = DateTime.now();
        return dateTime.getDayOfWeek() == day;
    }

    public static int random(int a1, int a2) {
        return random.nextInt(a1, a2);
    }

    public static int random(int a2) {
        return random.nextInt(a2);
    }

    public static boolean isnumber(String txt) {
        try {
            Long.valueOf(txt);
            return true;
        } catch (NumberFormatException e) {
            core.GameLogger.error("Util.isnumber: NumberFormatException for input: " + txt, e);
            return false;
        }
    }

    public static boolean is_same_day(DateTime now, DateTime d) {
        if (now == null || d == null)
            return false;
        String strDate_1 = now.toString().split("T")[0];
        String strDate_2 = d.toString().split("T")[0];
        return strDate_1.equals(strDate_2);
    }

    public synchronized static List<Top_Dame> sort(List<Top_Dame> list_select) {
        return new ArrayList<Top_Dame>();
    }

    public static String number_format(long n) {
        return (NumberFormat.getInstance(Locale.ITALY).format(n));
    }

    public static String toRoman(int n) {
        if (n <= 0) {
            return "";
        }
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] romanNumerals = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                n -= values[i];
                sb.append(romanNumerals[i]);
            }
        }
        return sb.toString();
    }

    public static String getRawName(String name) {
        if (name == null) {
            return null;
        }
        String res = name.trim();
        if (res.startsWith("「")) {
            int idx = res.indexOf("」");
            if (idx != -1) {
                res = res.substring(idx + 1).trim();
            }
        }
        int spaceIdx = res.lastIndexOf(' ');
        if (spaceIdx != -1) {
            String suffix = res.substring(spaceIdx + 1);
            if (isRoman(suffix)) {
                res = res.substring(0, spaceIdx).trim();
            }
        }
        return res;
    }

    private static boolean isRoman(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        return s.matches("^[IVXLCDM]+$");
    }

    /**
     * Calc stat with diminishing returns.
     * Linear up to 20,000 (2000%).
     * Asymptotic to 30,000 (3000%) for values > 20,000.
     * Formula for x > 20k: y = 30,000 - (100,000,000 / (x - 10,000))
     *
     * @param value raw stat
     * @return diminished stat
     */
    public static long getStatDiminishing(long value) {
        if (value <= 20000) {
            return value;
        }
        // y = 30000 - 10^8 / (x - 10000)
        long denominator = value - 10000;
        if (denominator <= 0) {
            return 20000; // Safety, should not happen for value > 20000
        }
        return 30000 - (100_000_000L / denominator);
    }
}
