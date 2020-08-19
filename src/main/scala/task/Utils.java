package task;

public class Utils {

    private Utils() {
        throw new UnsupportedOperationException("No way.");
    }

    /**
     * Faster than {@link Integer#parseInt(String)}, but with limitation: parses only positive decimal integers
     *
     * @param s positive decimal integer in string representation
     * @return positive decimal integer
     */
    public static int parseInt(final String s) {
        int len = s.length();
        int num = '0' - s.charAt(0);

        int i = 1;
        while (i < len) {
            num = num * 10 + '0' - s.charAt(i++);
        }
        return -1 * num;
    }
}
