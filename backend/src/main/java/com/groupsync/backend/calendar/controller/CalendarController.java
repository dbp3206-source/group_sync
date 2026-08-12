package com.groupsync.backend.calendar.controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.calendar.dto.BusyEventResponse;
import com.groupsync.backend.calendar.dto.CalendarItemResponse;
import com.groupsync.backend.calendar.dto.ConflictResponse;
import com.groupsync.backend.calendar.dto.CreateBusyEventRequest;
import com.groupsync.backend.calendar.dto.CreateWeeklyScheduleRequest;
import com.groupsync.backend.calendar.dto.WeeklyScheduleResponse;
import com.groupsync.backend.calendar.service.CalendarService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) { this.calendarService = calendarService; }

    @GetMapping("/items")
    public List<CalendarItemResponse> items(@AuthenticationPrincipal AuthenticatedUser actor, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        return calendarService.getItems(actor, from.toInstant(), to.toInstant());
    }

    @GetMapping("/conflicts")
    public ConflictResponse conflicts(@AuthenticationPrincipal AuthenticatedUser actor, @RequestParam OffsetDateTime start, @RequestParam OffsetDateTime end) {
        return calendarService.findConflicts(actor, start.toInstant(), end.toInstant());
    }

    @PostMapping("/events")
    public ResponseEntity<BusyEventResponse> createEvent(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody CreateBusyEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.createEvent(actor, request));
    }

    @PatchMapping("/events/{eventId}")
    public BusyEventResponse updateEvent(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long eventId, @Valid @RequestBody CreateBusyEventRequest request) {
        return calendarService.updateEvent(actor, eventId, request);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long eventId) {
        calendarService.deleteEvent(actor, eventId); return ResponseEntity.noContent().build();
    }

    @GetMapping("/recurring")
    public List<WeeklyScheduleResponse> schedules(@AuthenticationPrincipal AuthenticatedUser actor) { return calendarService.listSchedules(actor); }

    @PostMapping("/recurring")
    public ResponseEntity<WeeklyScheduleResponse> createSchedule(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody CreateWeeklyScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.createSchedule(actor, request));
    }

    @PatchMapping("/recurring/{scheduleId}")
    public WeeklyScheduleResponse updateSchedule(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long scheduleId, @Valid @RequestBody CreateWeeklyScheduleRequest request) {
        return calendarService.updateSchedule(actor, scheduleId, request);
    }

    @DeleteMapping("/recurring/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long scheduleId) {
        calendarService.deleteSchedule(actor, scheduleId); return ResponseEntity.noContent().build();
    }
}
