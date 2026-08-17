package com.groupsync.backend.calendar.aggregation;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.shared.exception.BadRequestException;

@Service
public class CalendarAggregatorService {
    private final List<CalendarSource> sources;

    public CalendarAggregatorService(List<CalendarSource> sources) {
        this.sources = sources;
    }

    @Transactional(readOnly = true)
    public List<CalendarItem> getItems(Long userId, Instant from, Instant to) {
        validateRange(from, to);
        return sources.stream()
            .flatMap(source -> source.getItems(userId, from, to).stream())
            .distinct()
            .sorted(Comparator.comparing(CalendarItem::start).thenComparing(CalendarItem::end))
            .toList();
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BadRequestException("Calendar query end must be after start.");
        }
        if (Duration.between(from, to).toDays() > 31) {
            throw new BadRequestException("Calendar query range cannot exceed 31 days.");
        }
    }
}
