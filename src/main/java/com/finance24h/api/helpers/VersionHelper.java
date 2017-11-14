package com.finance24h.api.helpers;

/**
 * Created by ait on 10/10/17.
 */
public class VersionHelper {
    public static boolean versionGreaterThan(String currentVersion, String compareVersion) {
        return toInt(currentVersion) > toInt(compareVersion);

    }
    public static boolean versionLessThan(String currentVersion, String compareVersion) {
        return toInt(currentVersion) < toInt(compareVersion);

    }
    public static boolean versionGreaterThanEqual(String currentVersion, String compareVersion) {
        return toInt(currentVersion) >= toInt(compareVersion);

    }
    public static boolean versionLessThanEqual(String currentVersion, String compareVersion) {
        return toInt(currentVersion) <= toInt(compareVersion);

    }

    private static int toInt(String version) {
        byte[] bytes = version.getBytes();
        int result = 0;
        for (byte oneByte : bytes) {
            result += oneByte;
        }
        return result;
    }
}
