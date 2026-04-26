package ru.dobrovichek.user.util;

public final class HelpRequestDescriptionParser {

    private HelpRequestDescriptionParser() {
    }

    public static String category(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        for (String raw : description.split("\\R")) {
            String line = raw.trim();
            if (line.startsWith("Категория:")) {
                return line.substring("Категория:".length()).trim();
            }
        }
        return null;
    }

    public static String address(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        for (String raw : description.split("\\R")) {
            String line = raw.trim();
            if (line.startsWith("Адрес:")) {
                return line.substring("Адрес:".length()).trim();
            }
        }
        return null;
    }
}
