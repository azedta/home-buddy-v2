package com.azedcods.home_buddy_v2.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnumLabelUtil {

    private EnumLabelUtil() {}

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w");

    public static String getLabel(Enum<?> value) {
        if (value == null) return null;

        String name = value.name();

        // Remove ROLE_ prefix (Spring Security enums)
        if (name.startsWith("ROLE_")) {
            name = name.substring(5);
        }

        name = name
                .toLowerCase(Locale.ENGLISH)
                .replace('_', ' ');

        // Capitalize each word
        Matcher matcher = WORD_PATTERN.matcher(name);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(
                    sb,
                    matcher.group().toUpperCase(Locale.ENGLISH)
            );
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
