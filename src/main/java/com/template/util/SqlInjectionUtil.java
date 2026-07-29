package com.template.util;

import java.util.regex.Pattern;

public class SqlInjectionUtil {

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE|UNION|SELECT|FROM|WHERE|OR\\s+1=1|--|;|'.*'.*OR)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_SPECIAL_CHARS = Pattern.compile(
        "[';\\-]{2,}|/\\*|\\*/|%00|\\\\x"
    );

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String cleaned = SQL_SPECIAL_CHARS.matcher(input).replaceAll("");
        cleaned = SQL_KEYWORDS.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    public static void validate(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        if (SQL_KEYWORDS.matcher(input).find()) {
            throw new IllegalArgumentException("Input contains prohibited SQL keywords");
        }
        if (SQL_SPECIAL_CHARS.matcher(input).find()) {
            throw new IllegalArgumentException("Input contains prohibited SQL characters");
        }
    }

    public static String escapeLike(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    }
}
