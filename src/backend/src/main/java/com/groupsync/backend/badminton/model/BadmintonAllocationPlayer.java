package com.groupsync.backend.badminton.model;

import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "badminton_allocation_players")
@IdClass(BadmintonAllocationPlayerId.class)
public class BadmintonAllocationPlayer {
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "allocation_id", nullable = false) private BadmintonAllocation allocation;
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(nullable = false) private int position;
    protected BadmintonAllocationPlayer() { }
    public BadmintonAllocationPlayer(BadmintonAllocation allocation, UserAccount user, int position) { this.allocation = allocation; this.user = user; this.position = position; }
    public BadmintonAllocation getAllocation() { return allocation; }
    public UserAccount getUser() { return user; }
    public int getPosition() { return position; }
}
