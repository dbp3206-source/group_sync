package com.groupsync.backend.tournament.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TournamentEntryMemberId implements Serializable {
    @Column(name = "entry_id") private Long entryId;
    @Column(name = "user_id") private Long userId;
    @Override public boolean equals(Object object) { if (this == object) return true; if (!(object instanceof TournamentEntryMemberId other)) return false; return Objects.equals(entryId, other.entryId) && Objects.equals(userId, other.userId); }
    @Override public int hashCode() { return Objects.hash(entryId, userId); }
}
