package com.groupsync.backend.badminton.model;

import java.io.Serializable;

public class BadmintonAllocationPlayerId implements Serializable {
    private Long allocation;
    private Long user;
    public BadmintonAllocationPlayerId() { }
    public BadmintonAllocationPlayerId(Long allocation, Long user) { this.allocation = allocation; this.user = user; }
    @Override public boolean equals(Object o) { if (!(o instanceof BadmintonAllocationPlayerId other)) return false; return java.util.Objects.equals(allocation, other.allocation) && java.util.Objects.equals(user, other.user); }
    @Override public int hashCode() { return java.util.Objects.hash(allocation, user); }
}
