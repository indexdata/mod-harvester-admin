package org.folio.harvesteradmin.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Period;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MiscellaneousTest {

  static Stream<Arguments> getPeriod() {
    return Stream.of(
        arguments(null, 4, "DAYS", Period.ofDays(4)),
        arguments(null, 6, "WEEKS", Period.ofWeeks(6)),
        arguments(null, 21, "MONTHS", Period.ofMonths(21)),
        arguments("11 eternities", 8, "WEEKS", Period.ofWeeks(8)),
        arguments("2 day", 7, "MONTHS", Period.ofDays(2)),
        arguments("3 days", 7, "MONTHS", Period.ofDays(3)),
        arguments("4 tag", 7, "MONTHS", Period.ofDays(4)),
        arguments("5 tage", 7, "MONTHS", Period.ofDays(5)),
        arguments("12 week", 7, "MONTHS", Period.ofWeeks(12)),
        arguments("13 weeks", 7, "MONTHS", Period.ofWeeks(13)),
        arguments("14 woche", 7, "MONTHS", Period.ofWeeks(14)),
        arguments("15 wochen", 7, "MONTHS", Period.ofWeeks(15)),
        arguments("22 month", 7, "DAYS", Period.ofMonths(22)),
        arguments("23 months", 7, "DAYS", Period.ofMonths(23)),
        arguments("24 monat", 7, "DAYS", Period.ofMonths(24)),
        arguments("25 monate", 7, "DAYS", Period.ofMonths(25))
        );
  }

  @ParameterizedTest
  @MethodSource
  void getPeriod(String periodAsText, int defaultAmount, String defaultUnit, Period expected) {
    assertThat(Miscellaneous.getPeriod(periodAsText, defaultAmount, defaultUnit), is(expected));
  }

}
