package com.mycelium.wallet.activity.rmc;

import java.util.Calendar;

public class Keys {
    public static Calendar getActiveStartDay() {
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(2017, 6, 12);
        return calendarStart;
    }

    public static Calendar getActiveEndDay() {
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.set(2018, 5, 10);
        return calendarEnd;
    }
}
