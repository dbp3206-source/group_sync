package com.groupsync.backend.badminton.responsibility;

import java.util.List;
import com.groupsync.backend.badminton.model.SessionResponsibility;
import com.groupsync.backend.user.model.UserAccount;

public class RoundRobinResponsibilityAssignmentStrategy implements ResponsibilityAssignmentStrategy {
    @Override public void assign(List<SessionResponsibility> responsibilities, List<UserAccount> members) { if (members.isEmpty()) return; for (int i = 0; i < responsibilities.size(); i++) responsibilities.get(i).assign(members.get(i % members.size())); }
}
