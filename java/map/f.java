
package map;

/**
 *
 * @author Admin
 */
public class f {

    public static int setInteger(long point) {
        if (point < 0) {
            point = Math.abs(point);
        }
        if (point > 2000000000) {
            point = 2000000000;
        }
        return (int) point;
    }

    public static short setShort(long point) {
        if (point > 32767) {
            point = 32767;
        }
        if (point < -32768) {
            point = -32768;
        }
        return (short) point;
    }
}
