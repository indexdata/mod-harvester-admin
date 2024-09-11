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
                    if (!unit.endsWith("S")) {
                        unit += "S";
                    }
                    switch (unit) {
                        case "DAYS":
                            return Period.ofDays(amount);
                        case "WEEKS":
                            return Period.ofWeeks(amount);
                        case "MONTHS":
                            return Period.ofMonths(amount);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            logger.error("Could not resolve period from [" + periodAsText + "]. Expected string on the format: <number> DAYS|WEEKS|MONTHS");
        }
        return switch (defaultUnit) {
            case "DAYS" -> Period.ofDays(defaultAmount);
            case "WEEKS" -> Period.ofWeeks(defaultAmount);
            case "MONTHS" -> Period.ofMonths(defaultAmount);
            default -> null;
        };
    }

}
