package com.groupsync.backend.badminton.model;

import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity @Table(name = "badminton_match_participants") @IdClass(BadmintonMatchParticipantId.class)
public class BadmintonMatchParticipant {
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "side_id", nullable = false) private BadmintonMatchSide side;
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    protected BadmintonMatchParticipant() { }
    public BadmintonMatchParticipant(BadmintonMatchSide side, UserAccount user) { this.side = side; this.user = user; }
    public BadmintonMatchSide getSide() { return side; } public UserAccount getUser() { return user; }
}
