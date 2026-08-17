package com.groupsync.backend.calendar.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.calendar.aggregation.CalendarAggregatorService;
import com.groupsync.backend.calendar.dto.BusyEventResponse;
import com.groupsync.backend.calendar.dto.CalendarItemResponse;
import com.groupsync.backend.calendar.dto.ConflictResponse;
import com.groupsync.backend.calendar.dto.CreateBusyEventRequest;
import com.groupsync.backend.calendar.dto.CreateWeeklyScheduleRequest;
import com.groupsync.backend.calendar.dto.WeeklyScheduleResponse;
import com.groupsync.backend.calendar.personal.model.BusyEvent;
import com.groupsync.backend.calendar.personal.model.WeeklySchedule;
import com.groupsync.backend.calendar.personal.repository.BusyEventRepository;
import com.groupsync.backend.calendar.personal.repository.WeeklyScheduleRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class CalendarService {
    private final BusyEventRepository busyEventRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final UserAccountRepository userRepository;
    private final CalendarAggregatorService aggregator;

    public CalendarService(BusyEventRepository busyEventRepository, WeeklyScheduleRepository weeklyScheduleRepository, UserAccountRepository userRepository, CalendarAggregatorService aggregator) {
        this.busyEventRepository = busyEventRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.userRepository = userRepository;
        this.aggregator = aggregator;
    }

    @Transactional(readOnly = true)
    public List<CalendarItemResponse> getItems(AuthenticatedUser actor, Instant from, Instant to) {
        return aggregator.getItems(actor.getId(), from, to).stream().map(CalendarItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BusyEventResponse> listEvents(AuthenticatedUser actor) { return busyEventRepository.findByUserIdOrderByStartAtAsc(actor.getId()).stream().map(BusyEventResponse::from).toList(); }

    @Transactional(readOnly = true)
    public ConflictResponse findConflicts(AuthenticatedUser actor, Instant start, Instant end) {
        List<CalendarItemResponse> items = getItems(actor, start, end).stream()
            .filter(item -> item.start().isBefore(end) && start.isBefore(item.end()))
            .toList();
        return new ConflictResponse(!items.isEmpty(), items);
    }

    @Transactional
    public BusyEventResponse createEvent(AuthenticatedUser actor, CreateBusyEventRequest request) {
        validateInterval(request.start(), request.end());
        BusyEvent event = busyEventRepository.save(new BusyEvent(findUser(actor.getId()), request.title().trim(), request.start(), request.end(), clean(request.description()), clean(request.category()), clean(request.location()), normalizeVisibility(request.visibility()), request.reminderMinutes()));
        return BusyEventResponse.from(event);
    }

    @Transactional
    public BusyEventResponse updateEvent(AuthenticatedUser actor, Long eventId, CreateBusyEventRequest request) {
        validateInterval(request.start(), request.end());
        BusyEvent event = busyEventRepository.findByIdAndUserId(eventId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Busy event not found."));
        event.update(request.title().trim(), request.start(), request.end(), clean(request.description()), clean(request.category()), clean(request.location()), normalizeVisibility(request.visibility()), request.reminderMinutes());
        return BusyEventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(AuthenticatedUser actor, Long eventId) {
        busyEventRepository.delete(busyEventRepository.findByIdAndUserId(eventId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Busy event not found.")));
    }

    @Transactional
    public BusyEventResponse duplicateEvent(AuthenticatedUser actor, Long eventId) {
        BusyEvent original = busyEventRepository.findByIdAndUserId(eventId, actor.getId()).orElseThrow(() -> new NotFoundException("Busy event not found."));
        return BusyEventResponse.from(busyEventRepository.save(new BusyEvent(findUser(actor.getId()), original.getTitle() + " (copy)", original.getStartAt(), original.getEndAt(), original.getDescription(), original.getCategory(), original.getLocation(), original.getVisibility(), original.getReminderMinutes())));
    }

    @Transactional(readOnly = true)
    public List<WeeklyScheduleResponse> listSchedules(AuthenticatedUser actor) {
        return weeklyScheduleRepository.findByUserId(actor.getId()).stream().map(WeeklyScheduleResponse::from).toList();
    }

    @Transactional
    public WeeklyScheduleResponse createSchedule(AuthenticatedUser actor, CreateWeeklyScheduleRequest request) {
        validateSchedule(request);
        WeeklySchedule schedule = weeklyScheduleRepository.save(new WeeklySchedule(findUser(actor.getId()), request.title().trim(), request.weekdays(), request.startTime(), request.endTime(), request.validFrom(), request.validUntil(), request.timezone(), clean(request.description()), clean(request.category()), clean(request.location()), normalizeVisibility(request.visibility()), request.reminderMinutes(), normalizeFrequency(request.frequency())));
        return WeeklyScheduleResponse.from(schedule);
    }

    @Transactional
    public WeeklyScheduleResponse updateSchedule(AuthenticatedUser actor, Long scheduleId, CreateWeeklyScheduleRequest request) {
        validateSchedule(request);
        WeeklySchedule schedule = weeklyScheduleRepository.findByIdAndUserId(scheduleId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Weekly schedule not found."));
        schedule.update(request.title().trim(), request.weekdays(), request.startTime(), request.endTime(), request.validFrom(), request.validUntil(), request.timezone(), clean(request.description()), clean(request.category()), clean(request.location()), normalizeVisibility(request.visibility()), request.reminderMinutes(), normalizeFrequency(request.frequency()));
        return WeeklyScheduleResponse.from(schedule);
    }

    @Transactional
    public void deleteSchedule(AuthenticatedUser actor, Long scheduleId) {
        weeklyScheduleRepository.delete(weeklyScheduleRepository.findByIdAndUserId(scheduleId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Weekly schedule not found.")));
    }

    @Transactional
    public WeeklyScheduleResponse duplicateSchedule(AuthenticatedUser actor, Long scheduleId) {
        WeeklySchedule original = weeklyScheduleRepository.findByIdAndUserId(scheduleId, actor.getId()).orElseThrow(() -> new NotFoundException("Weekly schedule not found."));
        return WeeklyScheduleResponse.from(weeklyScheduleRepository.save(new WeeklySchedule(findUser(actor.getId()), original.getTitle() + " (copy)", original.getWeekdays(), original.getStartTime(), original.getEndTime(), original.getValidFrom(), original.getValidUntil(), original.getTimezone(), original.getDescription(), original.getCategory(), original.getLocation(), original.getVisibility(), original.getReminderMinutes(), original.getFrequency())));
    }

    public List<com.groupsync.backend.calendar.model.CalendarItem> getItemsForUser(Long userId, Instant from, Instant to) {
        return aggregator.getItems(userId, from, to);
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found."));
    }

    private void validateInterval(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) throw new BadRequestException("Event end must be after start.");
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeVisibility(String value) { String normalized = clean(value); if (normalized == null) return "PRIVATE"; if (!normalized.equals("PRIVATE") && !normalized.equals("SHARED")) throw new BadRequestException("Visibility must be PRIVATE or SHARED."); return normalized; }
    private String normalizeFrequency(String value) { String normalized = clean(value); if (normalized == null) return "WEEKLY"; if (!normalized.equals("WEEKLY") && !normalized.equals("DAILY")) throw new BadRequestException("Frequency must be DAILY or WEEKLY."); return normalized; }

    private void validateSchedule(CreateWeeklyScheduleRequest request) {
        if (!request.startTime().isBefore(request.endTime())) throw new BadRequestException("Schedule end time must be after start time.");
        if (request.validFrom().isAfter(request.validUntil())) throw new BadRequestException("Schedule valid-from must not be after valid-until.");
        try { java.time.ZoneId.of(request.timezone()); } catch (java.time.DateTimeException exception) { throw new BadRequestException("Timezone is not valid."); }
        if (request.reminderMinutes() != null && request.reminderMinutes() < 0) throw new BadRequestException("Reminder minutes cannot be negative.");
    }
}
