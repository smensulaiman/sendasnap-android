package com.sendajapan.sendasnap.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormatter {

    private static final String API_DATE_FORMAT = "yyyy-MM-dd";
    private static final String API_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
    private static final String DISPLAY_DATE_FORMAT = "MMMM dd, yyyy";
    private static final String DISPLAY_DATETIME_FORMAT = "MMMM dd, yyyy 'at' HH:mm";

    public static String formatDateForDisplay(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            Date date = apiFormat.parse(apiDate);
            if (date != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
                return displayFormat.format(date);
            }
        } catch (ParseException e) {
        }

        return apiDate;
    }

    public static String formatDateTimeForDisplay(String apiDateTime) {
        if (apiDateTime == null || apiDateTime.isEmpty()) {
            return "";
        }

        try {
            String dateTimeStr = apiDateTime;
            if (dateTimeStr.contains("T")) {
                String datePart = dateTimeStr.split("T")[0];
                SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
                Date date = apiFormat.parse(datePart);
                if (date != null) {
                    SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
                    return displayFormat.format(date);
                }
            } else {
                return formatDateForDisplay(dateTimeStr);
            }
        } catch (ParseException e) {
        }

        return apiDateTime;
    }

    public static String formatDateForApi(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
        return apiFormat.format(date);
    }

    public static Date parseApiDate(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
            return apiFormat.parse(apiDate);
        } catch (ParseException e) {
            return null;
        }
    }
}
