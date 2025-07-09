package com.milosz.podsiadly.common.util;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateUtil {

    public static Date convertToDateViaInstant(LocalDateTime dateToConvert) {
        return Date.from(dateToConvert.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static boolean isBeforeNow(Date date) {
        return date != null && date.before(new Date());
    }

    public static boolean isAfterNow(Date date) {
        return date != null && date.after(new Date());
    }

    public static Date addMinutes(Date date, int minutes) {
        return Date.from(date.toInstant().plusSeconds(minutes * 60L));
    }

    public static Date addHours(Date date, int hours) {
        return Date.from(date.toInstant().plusSeconds(hours * 3600L));
    }
}