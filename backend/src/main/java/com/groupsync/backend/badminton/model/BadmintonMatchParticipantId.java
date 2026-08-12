package com.groupsync.backend.badminton.model;
import java.io.Serializable;
public class BadmintonMatchParticipantId implements Serializable {
    private Long side; private Long user;
    public BadmintonMatchParticipantId() { }
    @Override public boolean equals(Object o) { if (!(o instanceof BadmintonMatchParticipantId other)) return false; return java.util.Objects.equals(side, other.side) && java.util.Objects.equals(user, other.user); }
    @Override public int hashCode() { return java.util.Objects.hash(side, user); }
}
