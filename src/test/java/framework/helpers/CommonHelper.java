package test.java.framework.helpers;

import org.apache.commons.io.FileUtils;
import test.java.framework.SessionPrototype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class CommonHelper {

    private static boolean isAllowedToDeleteScreenshots = true;
    private static boolean isAllowedToDeleteDownloads = true;

    private static final Object downloadsDirLock = new Object();
    private static final Object screenshotsDirLock = new Object();

    private static String downloadsDir = "target/downloads/";
    private static String screenshotsDir = "target/screenshots/";

    public static String getScreenshotsDir() {
        return screenshotsDir;
    }

    public static String getDownloadsDir() {
        return downloadsDir;
    }

    /**
     * Gets current date (for random name of new item)
     *
     * @return current date in yyMMdd_HHmmss format
     */
    public static String getCurrentTimeForName() {
        SimpleDateFormat f = new SimpleDateFormat("yyMMddHHmmss_");
        return f.format(Calendar.getInstance().getTime());
    }

    /**
     * Gets fully qualified hostname
     *
     * @return fully qualified hostname
     * @throws java.net.SocketException
     */
    public static String getIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    /**
     * Gets Name of the month by its number
     *
     * @param month month number, starting 0
     * @return number of month in a year starting 0
     */
    public static String getMonth(Integer month) {
        return new DateFormatSymbols().getMonths()[month];
    }

    /**
     * Returns month number starting 0
     *
     * @param month month name, e.g. November
     * @return number of month in a year starting 0
     */
    public static Integer getMonth(String month) {
        return Arrays.asList(new DateFormatSymbols().getMonths()).indexOf(month);
    }

    /**
     * Converts Double to string leaving 2 decimal points
     *
     * @param doubleValue double number to convert
     * @return formatted string
     */
    public static String formatDouble(Double doubleValue) {
        return new DecimalFormat("#,##0.00").format(doubleValue);
    }

    /**
     * Generates random alpha-numeric string of specified length
     *
     * @param len length of string to generate
     * @return random string (A-Z, 0-9)
     */
    public static String randomString(Integer len) {
        final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    /**
     * Generates random alpha-numeric string with default length of 16
     *
     * @return random alpha-numeric string with default length of 16
     */
    public static String randomString() {
        return randomString(16);
    }

    /**
     * Random string + date time for new item random name
     *
     * @param len length of String to return.
     * @return current date in yyMMdd_HHmmss format + random string for length > 14
     */
    public static String randomStringTime(int len) {
        if (len < 14) {
            return getCurrentTimeForName();
        } else {
            String cTime = getCurrentTimeForName();
            return cTime + randomString(len - cTime.length());
        }
    }

    /**
     * Chooses random string from those provided in list
     *
     * @param diffValues list of string to select from
     * @return random value from list
     */
    public static String randomString(List<String> diffValues) {
        int select = new Random().nextInt(diffValues.size());
        return diffValues.get(select);
    }

    /**
     * Generates random double value in range of 1.00 to 100.00 rounded to 2 decimal digits
     *
     * @return random double value in range of 1.00 to 100.00
     */
    public static double randomDouble() {
        return Math.round(Math.random() * 10000d) / 100d;
    }

    /**
     * Generates random int value from the specified range of int numbers
     *
     * @param min minimum number in range
     * @param max maximum number in range
     * @return random int value from range of [min, max]
     */
    public static int randomIntFromRange(int min, int max) {
        return min + (int) (Math.random() * ((max - min)));
    }

    /**
     * Generates random month name from the specified range of month String values
     *
     * @param startMonth start month name in range
     * @param endMonth   end month name in range
     * @return random month name between specified start and end month names
     */
    protected String randomMonthFromRange(String startMonth, String endMonth) {
        return getMonth(randomIntFromRange(getMonth(startMonth), getMonth(endMonth)));
    }

    /*
     * http://stackoverflow.com/questions/3537706/howto-unescape-a-java-string-literal-in-java
     * In contrast to fixing Java's broken regex charclasses,
     * this one need be no bigger, as unescaping shrinks the string
     * here, where in the other one, it grows it.
     */
    public static String unescape(String oldStr) throws IllegalArgumentException {

        StringBuilder newStr = new StringBuilder(oldStr.length());

        boolean backSlashPresent = false;

        for (int i = 0; i < oldStr.length(); i++) {
            int cp = oldStr.codePointAt(i);
            if (oldStr.codePointAt(i) > Character.MAX_VALUE) {
                i++;
            }

            if (!backSlashPresent) {
                if (cp == '\\') {
                    backSlashPresent = true;
                } else {
                    newStr.append(Character.toChars(cp));
                }
                continue;
            }

            if (cp == '\\') {
                backSlashPresent = false;
                newStr.append('\\');
                newStr.append('\\');
                continue;
            }

            switch (cp) {

                case 'r':
                    newStr.append('\r');
                    break;

                case 'n':
                    newStr.append('\n');
                    break;

                case 'f':
                    newStr.append('\f');
                    break;

                case 'b':
                    newStr.append("\\b");
                    break;

                case 't':
                    newStr.append('\t');
                    break;

                case 'a':
                    newStr.append('\007');
                    break;

                case 'e':
                    newStr.append('\033');
                    break;

                //* A "control" character is what you get when you xor its
                //* codepoint with '@'==64.  This only makes sense for ASCII,
                //* and may not yield a "control" character after all.
                //* Strange but true: "\c{" is ";", "\c}" is "=", etc.
                case 'c': {
                    if (++i == oldStr.length()) {
                        throw new IllegalArgumentException("trailing \\c");
                    }
                    cp = oldStr.codePointAt(i);

                    if (cp > 0x7f) {
                        throw new IllegalArgumentException("expected ASCII after \\c");
                    }
                    newStr.append(Character.toChars(cp ^ 64));
                    break;
                }

                case '8':
                case '9':
                    throw new IllegalArgumentException("illegal octal digit");

                    //* may be 0 to 2 octal digits following this one
                    //* so back up one for fallthrough to next case;
                    //* unread this digit and fall through to next case.
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    --i;

                    //* Can have 0, 1, or 2 octal digits following a 0
                    //* this permits larger values than octal 377, up to octal 777.
                case '0': {
                    if (i + 1 == oldStr.length()) {
                    /* found \0 at end of string */
                        newStr.append(Character.toChars(0));
                        break;
                    }
                    i++;
                    int digits = 0;
                    int j;
                    for (j = 0; j <= 2; j++) {
                        if (i + j == oldStr.length()) {
                            break;//for
                        }
                    /* safe because will unread surrogate */
                        int ch = oldStr.charAt(i + j);
                        if (ch < '0' || ch > '7') {
                            break;//for
                        }
                        digits++;
                    }
                    if (digits == 0) {
                        --i;
                        newStr.append('\0');
                        break;
                    }
                    int value;
                    try {
                        value = Integer.parseInt(
                                oldStr.substring(i, i + digits), 8);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("invalid octal value for \\0 escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += digits - 1;
                    break;
                } /* end case '0' */

                case 'x': {
                    if (i + 2 > oldStr.length()) {
                        throw new IllegalArgumentException("string too short for \\x escape");
                    }
                    i++;
                    boolean saw_brace = false;
                    if (oldStr.charAt(i) == '{') {
                        //ok to ignore surrogates here
                        i++;
                        saw_brace = true;
                    }
                    int j;
                    for (j = 0; j < 8; j++) {

                        if (!saw_brace && j == 2) {
                            break;  /* for */
                        }

                    /*
                     * ASCII test also catches surrogates
                     */
                        int ch = oldStr.charAt(i + j);
                        if (ch > 127) {
                            throw new IllegalArgumentException("illegal non-ASCII hex digit in \\x escape");
                        }

                        if (saw_brace && ch == '}') {
                            break;
                        }

                        if (!((ch >= '0' && ch <= '9')
                                ||
                                (ch >= 'a' && ch <= 'f')
                                ||
                                (ch >= 'A' && ch <= 'F')
                        )
                                ) {
                            throw new IllegalArgumentException(String.format(
                                    "illegal hex digit #%d '%c' in \\x", ch, ch));
                        }

                    }
                    if (j == 0) {
                        throw new IllegalArgumentException("empty braces in \\x{} escape");
                    }
                    int value;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("invalid hex value for \\x escape");
                    }
                    newStr.append(Character.toChars(value));
                    if (saw_brace) {
                        j++;
                    }
                    i += j - 1;
                    break;
                }

                case 'u': {
                    if (i + 4 > oldStr.length()) {
                        throw new IllegalArgumentException("string too short for \\u escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 4; j++) {
                    /* this also handles the surrogate issue */
                        if (oldStr.charAt(i + j) > 127) {
                            throw new IllegalArgumentException("illegal non-ASCII hex digit in \\u escape");
                        }
                    }
                    int value;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("invalid hex value for \\u escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += j - 1;
                    break;
                }

                case 'U': {
                    if (i + 8 > oldStr.length()) {
                        throw new IllegalArgumentException("string too short for \\U escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 8; j++) {
                        //this also handles the surrogate issue
                        if (oldStr.charAt(i + j) > 127) {
                            throw new IllegalArgumentException("illegal non-ASCII hex digit in \\U escape");
                        }
                    }
                    int value;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("invalid hex value for \\U escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += j - 1;
                    break;
                }

                default:
                    newStr.append('\\');
                    newStr.append(Character.toChars(cp));
                    break;

            }
            backSlashPresent = false;
        }

        //weird to leave one at the end
        if (backSlashPresent) {
            newStr.append('\\');
        }

        return newStr.toString();
    }

    /**
     * Read mobile dependent locators
     */
    public static Properties readLocators() {

        Properties locators = new Properties();
        String fileName = String.format(
                "src/test/resources/locators/%s.locators",
                SessionPrototype.getClient().name().toLowerCase()
        );

        try {
            locators.load(new FileInputStream(fileName));
        } catch (Exception ioe) {
            System.err.println("I/O Exception on loading " + fileName + " file:\n" + ioe.getMessage());
            System.exit(1);
        }
        return locators;
    }

    /**
     * Delete screenshots directory and all it's contents
     */
    public static void clearScreenShotsDir() {
        synchronized (screenshotsDirLock) {
            try {
                if (isAllowedToDeleteScreenshots) {
                    FileUtils.deleteDirectory(new File(getScreenshotsDir()));
                }
                isAllowedToDeleteScreenshots = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Delete downloads directory and all it's contents
     */
    public static void clearDownloadsDir() {
        synchronized (downloadsDirLock) {
            try {
                if (isAllowedToDeleteDownloads) {
                    FileUtils.deleteDirectory(new File(getDownloadsDir()));
                }
                isAllowedToDeleteDownloads = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads config file
     *
     * @return properties object
     */
    public static Properties readConfig() throws IOException {
        Properties props = new Properties();
        String fileName = "Config.Properties";
        try {
            props.load(new FileInputStream(fileName));
        } catch (IOException ioe) {
            System.err.println("I/O Exception on loading " + fileName + " file:\n" + ioe.getMessage());
            throw ioe;
        }
        return props;
    }

    /**
     * Runs virtual screen on a linux machine. Xvfb should be installed.
     *
     * @return port
     * @throws IOException
     */
    public static String runXvfb() throws IOException {
        String command = "Xvfb :%s -ac -screen %s 1920x1080x24 -extension RANDR";

        String screen = String.valueOf(randomIntFromRange(0, 15));
        String name = String.valueOf(Thread.currentThread().getId()).concat(screen);

        Runtime.getRuntime().exec(String.format(command, name, screen));
        return System.getProperty("lmportal.xvfb.id", ":" + name);
    }

    /**
     * Verifies whether file is present
     *
     * @param fileName filename + path relevant to the project root or fully qualified filename
     * @return true if present false if not
     */
    public static boolean isFilePresent(String fileName) {
        return new File(fileName).exists();
    }

    public static String getFileUrl(String path) {

        String url = "No file URL!";
        try {
            File file = new File(path);
            url = SessionPrototype.isDebug() ?
                    "file:///" + file.getCanonicalPath() :
                    "http://" + getIP() + SessionPrototype.getProjectPath() + path;
        } catch (Exception ignored) {
        }
        return String.format("<a href=\"%s\" target=\"_blank\">Click to open in a new tab</a>", url);
    }

    public static String makeDownloadsDir() throws IOException {
        String downloadDir = CommonHelper.getDownloadsDir();
        new File(downloadDir).mkdirs();
        try {
            downloadDir = new File(downloadDir).getCanonicalPath();
        } catch (IOException ioe) {
            System.err.println("Unable to get downloads dir path!\n" + ioe.getMessage());
            throw ioe;
        }
        return downloadDir;
    }

    /**
     * Reads system variable (cmd option) or config file value on fail
     *
     * @param props        config file
     * @param propertyName config/cmd property name
     * @return value
     */
    public static String getProperty(Properties props, String propertyName) {
        return System.getProperty(propertyName, props.getProperty(propertyName));
    }

    /**
     * Reads system variable (cmd option) or config file value on fail
     *
     * @param props        config file to get value from if not specified via cmd
     * @param propertyName config/cmd property name to get value from
     * @param defaultValue value to use if not specified via cmd and config
     * @return value
     */
    public static String getProperty(Properties props, String propertyName, String defaultValue) {
        return System.getProperty(propertyName, props.getProperty(propertyName, defaultValue));
    }

}
