package model.enums;

public enum Category {
    JUNIOR, INTERMEDIATE, SENIOR;

    public int getTaskLimit() {
        switch (this) {
            case JUNIOR:       return 10;
            case INTERMEDIATE: return 5;
            case SENIOR:       return 2;
            default:           return Integer.MAX_VALUE;
        }
    }

    public static Category fromString(String s) {
        if (s == null || s.trim().isEmpty()) return JUNIOR;

        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return JUNIOR;
        }
    }
}