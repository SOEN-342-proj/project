package model;

import model.enums.Status;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class SearchCriteria {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String nameMatch;
    private DayOfWeek dayOfWeek;
    private Status status;

    public SearchCriteria() {}

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public String getNameMatch() { return nameMatch; }
    public void setNameMatch(String nameMatch) { this.nameMatch = nameMatch; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isEmpty() {
        return periodStart == null && periodEnd == null
                && (nameMatch == null || nameMatch.trim().isEmpty())
                && dayOfWeek == null && status == null;
    }
}