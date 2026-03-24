package model.enums;

public enum Status {
    OPEN, COMPLETED, CANCELLED;

    public static Status fromString(String s) {
        if (s == null || s.trim().isEmpty()) return OPEN;

        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}