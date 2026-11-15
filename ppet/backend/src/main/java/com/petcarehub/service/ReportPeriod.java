package com.petcarehub.service;
import java.time.*;
import java.time.temporal.*;
public record ReportPeriod(LocalDate from, LocalDate to) {
    public ReportPeriod {
        if(from==null||to==null||from.isAfter(to)||from.getYear()<2||to.getYear()>9998||ChronoUnit.DAYS.between(from,to)>=366)
        throw CatalogInput.bad("Kỳ tối đa 366 ngày, năm từ 2 đến 9998; ngày bắt đầu không sau ngày kết thúc");
    }
    public static ReportPeriod of(String period,LocalDate from,LocalDate to,Clock clock){
        var today=LocalDate.now(clock);
        return switch(period){
            case "today"->new ReportPeriod(today,today);
            case "week"->new ReportPeriod(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),today);
            case "month"->new ReportPeriod(today.withDayOfMonth(1),today);
            case "custom"->new ReportPeriod(from,to);
            default->throw CatalogInput.bad("Kỳ không hợp lệ");
        }
        ;
    }
    public LocalDateTime start(){
        return from.atStartOfDay();
    }
    public LocalDateTime end(){
        return to.plusDays(1).atStartOfDay();
    }
    public ReportPeriod previous(){
        long days=ChronoUnit.DAYS.between(from,to)+1;
        return new ReportPeriod(from.minusDays(days),from.minusDays(1));
    }
}
