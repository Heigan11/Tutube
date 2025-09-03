package com.tutube.core.utils;

import java.time.LocalDate;

public class Utils {
    public static Double calculateExactAge(LocalDate birthDate) {
        if (birthDate == null) {
            //ToDO add exception
            return null;
        }

        LocalDate now = LocalDate.now();

        // Если дата рождения в будущем - возвращаем null
        if (birthDate.isAfter(now)) {
            //ToDO add exception
            return null;
        }

        // Вычисляем общее количество дней
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(birthDate, now);

        // Среднее количество дней в году (учитывая високосные годы)
        double exactAge = totalDays / 365.2425;

        return Math.round(exactAge * 100.0) / 100.0;
    }
}
