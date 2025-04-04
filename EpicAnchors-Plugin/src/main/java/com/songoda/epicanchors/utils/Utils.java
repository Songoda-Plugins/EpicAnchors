package com.songoda.epicanchors.utils;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isInt(String s) {
        if (s != null && !s.isEmpty()) {
            try {
                Integer.parseInt(s);
                return true;
            } catch (NumberFormatException ignore) {
            }
        }

        return false;
    }

    /**
     * Get all values of a String[] which start with a given String
     *
     * @param value The String to search for
     * @param list  The array to iterate
     *
     * @return A list with all the matches
     */
    public static List<String> getMatches(String value, Collection<String> list, boolean caseInsensitive) {
        List<String> result = new LinkedList<>();

        for (String str : list) {
            if (str.startsWith(value.toLowerCase())
                    || (caseInsensitive && str.toLowerCase().startsWith(value.toLowerCase()))) {
                result.add(str);
            }
        }

        return result;
    }

    public static void logException(@Nullable Plugin plugin, @NotNull Throwable th) {
        logException(plugin, th, null);
    }

    public static void logException(@Nullable Plugin plugin, @NotNull Throwable th, @Nullable String type) {
        Logger logger = plugin != null ? plugin.getLogger() : Logger.getGlobal();

        logger.log(Level.SEVERE, th, () -> "A " + (type == null ? "critical" : type) + " error occurred");
    }
}
