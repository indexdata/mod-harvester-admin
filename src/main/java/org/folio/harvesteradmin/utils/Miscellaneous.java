package org.folio.harvesteradmin.utils;

import java.time.Period;

import static org.folio.harvesteradmin.service.HarvestAdminService.logger;

public class Miscellaneous {
    /**
     * Takes a period in the form of "3 WEEKS", for example, and turns it into a temporal amount.
     * @param periodAsText a string with an integer followed by DAYS, WEEKS, or MONTHS
     * @return temporal amount representing the period.
     */
    public static Period getPeriod(String periodAsText, int defaultAmount, String defaultUnit) {
        if (periodAsText != null) {
            String[] periodAsArray = periodAsText.trim().toUpperCase().split(" ");
            if (periodAsArray.length == 2) {
                try {
                    int amount = Integer.parseInt(periodAsArray[0]);
                    String unit = periodAsArray[1];
                    switch (unit) {
                        case "DAY":
                        case "DAYS":
                        case "TAG":
                        case "TAGE":
                            return Period.ofDays(amount);
                        case "WEEK":
                        case "WEEKS":
                        case "WOCHE":
                        case "WOCHEN":
                            return Period.ofWeeks(amount);
                        case "MONTH":
                        case "MONTHS":
                        case "MONAT":
                        case "MONATE":
                            return Period.ofMonths(amount);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            logger.error("Could not resolve period from [" + periodAsText + "]. Expected string on the format: <number> DAY(S)|TAG(E)|WEEK(S)|WOCHE(N)|MONTH(S)|MONAT(E)");
        }
        return switch (defaultUnit) {
            case "DAYS" -> Period.ofDays(defaultAmount);
            case "WEEKS" -> Period.ofWeeks(defaultAmount);
            case "MONTHS" -> Period.ofMonths(defaultAmount);
            default -> null;
        };
    }

}
