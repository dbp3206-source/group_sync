package com.groupsync.backend.badminton.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSessionStatus;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.calendar.aggregation.CalendarSource;
import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.model.CalendarSourceType;

@Component
public class BadmintonCalendarSource implements CalendarSource {
    private final BadmintonRegistrationRepository registrationRepository;
    public BadmintonCalendarSource(BadmintonRegistrationRepository registrationRepository) { this.registrationRepository = registrationRepository; }
    @Override @Transactional(readOnly = true)
    public List<CalendarItem> getItems(Long userId, Instant from, Instant to) {
        return registrationRepository.findByUserId(userId).stream()
            .filter(r -> r.getStatus() == RegistrationStatus.REGISTERED || r.getStatus() == RegistrationStatus.CHECKED_IN)
            .map(BadmintonRegistration::getSession)
            .filter(s -> s.getStatus() == BadmintonSessionStatus.CONFIRMED || s.getStatus() == BadmintonSessionStatus.PLAYING || s.getStatus() == BadmintonSessionStatus.COMPLETED)
            .filter(s -> s.getStartAt().isBefore(to) && from.isBefore(s.getEndAt()))
            .map(s -> new CalendarItem(CalendarSourceType.BADMINTON, s.getId(), "Badminton: " + s.getTitle(), s.getStartAt(), s.getEndAt(), true))
            .toList();
    }
}
