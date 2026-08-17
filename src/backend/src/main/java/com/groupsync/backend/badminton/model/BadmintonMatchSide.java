package com.groupsync.backend.badminton.model;

import java.util.LinkedHashSet;
import java.util.Set;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity @Table(name = "badminton_match_sides")
public class BadmintonMatchSide {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id", nullable = false) private BadmintonMatch match;
    @Enumerated(EnumType.STRING) @Column(name = "side_code", nullable = false, length = 2) private MatchSideCode code;
    @OneToMany(mappedBy = "side", cascade = CascadeType.ALL, orphanRemoval = true) private Set<BadmintonMatchParticipant> participants = new LinkedHashSet<>();
    protected BadmintonMatchSide() { }
    public BadmintonMatchSide(BadmintonMatch match, MatchSideCode code) { this.match = match; this.code = code; }
    public Long getId() { return id; } public BadmintonMatch getMatch() { return match; } public MatchSideCode getCode() { return code; } public Set<BadmintonMatchParticipant> getParticipants() { return participants; }
    public void addParticipant(BadmintonMatchParticipant participant) { participants.add(participant); }
}
