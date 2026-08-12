package com.groupsync.backend.badminton.model;

import com.groupsync.backend.user.model.UserAccount;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "badminton_responsibilities", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_responsibility_session_item", columnNames = {"session_id", "item_name"}))
public class SessionResponsibility {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @Column(name = "item_name", nullable = false, length = 100) private String itemName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ResponsibilityStatus status = ResponsibilityStatus.NEEDED;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") private UserAccount assignee;
    @Column(length = 300) private String note;

    protected SessionResponsibility() { }
    public SessionResponsibility(BadmintonSession session, String itemName, String note) { this.session = session; this.itemName = itemName; this.note = note; }
    public Long getId() { return id; }
    public BadmintonSession getSession() { return session; }
    public String getItemName() { return itemName; }
    public ResponsibilityStatus getStatus() { return status; }
    public UserAccount getAssignee() { return assignee; }
    public String getNote() { return note; }
    public void assign(UserAccount user) { assignee = user; status = ResponsibilityStatus.ASSIGNED; }
    public void unassign() { assignee = null; status = ResponsibilityStatus.NEEDED; }
}
