package com.groupsync.backend.study.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.calendar.aggregation.CalendarSource;
import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.model.CalendarSourceType;
import com.groupsync.backend.study.model.AttendanceStatus;
import com.groupsync.backend.study.model.StudyParticipant;
import com.groupsync.backend.study.model.StudySessionStatus;
import com.groupsync.backend.study.repository.StudyParticipantRepository;

@Component
public class StudyCalendarSource implements CalendarSource {
    private final StudyParticipantRepository participantRepository;

    public StudyCalendarSource(StudyParticipantRepository participantRepository) { this.participantRepository = participantRepository; }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarItem> getItems(Long userId, Instant from, Instant to) {
        return participantRepository.findByUserId(userId).stream()
            .filter(participant -> participant.getAttendance() != AttendanceStatus.ABSENT)
            .map(StudyParticipant::getSession)
            .filter(session -> session.getStatus() == StudySessionStatus.CONFIRMED || session.getStatus() == StudySessionStatus.COMPLETED)
            .filter(session -> session.getStartAt().isBefore(to) && from.isBefore(session.getEndAt()))
            .map(session -> new CalendarItem(CalendarSourceType.STUDY, session.getId(), "Study: " + session.getTopic(), session.getStartAt(), session.getEndAt(), true))
            .toList();
    }
}
