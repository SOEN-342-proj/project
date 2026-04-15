package model.enums;

public enum RecurrenceType {
    DAILY, WEEKLY, MONTHLY;

    public static RecurrenceType fromString(String s) {
        if (s == null || s.trim().isEmpty()) return DAILY;

        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DAILY;
        }
    }
}