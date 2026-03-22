package model;

import model.enums.RecurrenceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecurrencePattern {
    private int patternId;
    private RecurrenceType type;
    private Set<DayOfWeek> weekdays;
    private int dayOfMonth;
    private int interval;
    private LocalDate startDate;
    private LocalDate endDate;

    public RecurrencePattern(int patternId, RecurrenceType type, LocalDate startDate, LocalDate endDate, int interval) {
        this.patternId = patternId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.interval = interval;
        this.weekdays = new HashSet<>();
    }

    public int getPatternId() { return patternId; }

    public RecurrenceType getType() { return type; }
    public void setType(RecurrenceType type) { this.type = type; }

    public Set<DayOfWeek> getWeekdays() { return weekdays; }
    public void setWeekdays(Set<DayOfWeek> weekdays) { this.weekdays = weekdays; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<LocalDate> computeDueDates() {
        List<LocalDate> dates = new ArrayList<>();
        if (startDate == null || endDate == null) return dates;
        switch (type) {
            case DAILY:
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(interval > 0 ? interval : 1)) {
                    dates.add(d);
                }
                break;
            case WEEKLY:
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    if (weekdays.isEmpty() || weekdays.contains(d.getDayOfWeek())) dates.add(d);
                }
                break;
            case MONTHLY:
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusMonths(1)) {
                    try {
                        LocalDate candidate = d.withDayOfMonth(dayOfMonth > 0 ? dayOfMonth : d.getDayOfMonth());
                        if (!candidate.isAfter(endDate)) dates.add(candidate);
                    } catch (Exception ignored) {}
                }
                break;
            case CUSTOM:
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(interval > 0 ? interval : 1)) {
                    dates.add(d);
                }
                break;
        }
        return dates;
    }
}