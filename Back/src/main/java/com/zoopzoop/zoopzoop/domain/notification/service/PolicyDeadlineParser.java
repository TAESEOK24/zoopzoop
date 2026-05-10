package com.zoopzoop.zoopzoop.domain.notification.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PolicyDeadlineParser {

    private static final Pattern NUMERIC_DATE = Pattern.compile("(20\\d{2})[.\\-/](\\d{1,2})[.\\-/](\\d{1,2})");
    private static final Pattern KOREAN_DATE = Pattern.compile("(20\\d{2})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final DateTimeFormatter FLEXIBLE_DATE = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .toFormatter(Locale.KOREA);

    public Optional<LocalDate> parse(String deadline) {
        if (deadline == null || deadline.trim().isEmpty()) {
            return Optional.empty();
        }

        List<LocalDate> dates = new ArrayList<>();
        collectDates(NUMERIC_DATE, deadline, dates);
        collectDates(KOREAN_DATE, deadline, dates);

        if (dates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dates.get(dates.size() - 1));
    }

    private void collectDates(Pattern pattern, String value, List<LocalDate> dates) {
        Matcher matcher = pattern.matcher(value);

        while (matcher.find()) {
            String normalized = "%s-%s-%s".formatted(matcher.group(1), matcher.group(2), matcher.group(3));
            try {
                dates.add(LocalDate.parse(normalized, FLEXIBLE_DATE));
            } catch (DateTimeParseException ignored) {
                // Ignore malformed dates embedded in free-form deadline text.
            }
        }
    }
}
