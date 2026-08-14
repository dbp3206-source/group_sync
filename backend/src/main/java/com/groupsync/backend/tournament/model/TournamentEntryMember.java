package com.groupsync.backend.tournament.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_entry_members")
public class TournamentEntryMember {
    @EmbeddedId
    private TournamentEntryMemberId id = new TournamentEntryMemberId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("entryId")
    @JoinColumn(name = "entry_id", nullable = false)
    private TournamentEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private com.groupsync.backend.user.model.UserAccount user;

    protected TournamentEntryMember() {
    }

    public TournamentEntryMember(TournamentEntry entry, com.groupsync.backend.user.model.UserAccount user) {
        this.entry = entry;
        this.user = user;
    }

    public com.groupsync.backend.user.model.UserAccount getUser() { return user; }
}
