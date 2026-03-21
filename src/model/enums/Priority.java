package model.enums;

public enum Priority {
    LOW, MEDIUM, HIGH;

    public static Priority fromString(String s) {
        if (s == null || s.trim().isEmpty()) return LOW;

        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}