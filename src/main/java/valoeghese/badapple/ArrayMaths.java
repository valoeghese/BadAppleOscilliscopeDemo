package valoeghese.badapple;

public class ArrayMaths {
    public static int[] clampMin(int[] dest, int[] min) {
        for (int i = 0; i < dest.length; i++) {
            if (dest[i] < min[i])
                dest[i] = min[i];
        }
        return dest;
    }
}
