package com.github.nvd.util;

public class FileUtil {

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, true);
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String makeFileName(String channel, String startDateTime, String endDateTime) {
        return ("ch-" + channel + "-" + startDateTime + "-" + startDateTime).replace(" ", "-").replace(":", "-");
    }

    public static String nvrHomeDir() {
        return System.getProperty("user.home") + "/nvr";
    }
    

    public static void main(String[] args) {
        System.out.println(makeFileName("2", "2018-12-05 10:00:00", "2018-13-05 12:00:00"));
    }
}