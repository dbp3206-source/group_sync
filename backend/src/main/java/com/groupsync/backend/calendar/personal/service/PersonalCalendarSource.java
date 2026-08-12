package com.groupsync.backend.calendar.personal.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.calendar.aggregation.CalendarSource;
import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.model.CalendarSourceType;
import com.groupsync.backend.calendar.personal.model.BusyEvent;
import com.groupsync.backend.calendar.personal.repository.BusyEventRepository;
import com.groupsync.backend.calendar.personal.repository.WeeklyScheduleRepository;

@Component
public class PersonalCalendarSource implements CalendarSource {
    private final BusyEventRepository busyEventRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final RecurrenceExpander recurrenceExpander;

    public PersonalCalendarSource(BusyEventRepository busyEventRepository, WeeklyScheduleRepository weeklyScheduleRepository, RecurrenceExpander recurrenceExpander) {
        this.busyEventRepository = busyEventRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.recurrenceExpander = recurrenceExpander;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarItem> getItems(Long userId, Instant from, Instant to) {
        List<CalendarItem> items = new ArrayList<>();
        for (BusyEvent event : busyEventRepository.findByUserIdAndStartAtLessThanAndEndAtGreaterThan(userId, to, from)) {
            items.add(new CalendarItem(CalendarSourceType.MANUAL, event.getId(), event.getTitle(), event.getStartAt(), event.getEndAt(), true));
        }
        weeklyScheduleRepository.findByUserId(userId).forEach(schedule -> items.addAll(recurrenceExpander.expand(schedule, from, to)));
        return items;
    }
}
