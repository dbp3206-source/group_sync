package com.groupsync.backend.calendar.dto;

import java.util.List;

public record ConflictResponse(boolean conflict, List<CalendarItemResponse> items) {
}
