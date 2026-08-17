package com.groupsync.backend.badminton.responsibility;

import java.util.List;
import com.groupsync.backend.badminton.model.SessionResponsibility;
import com.groupsync.backend.user.model.UserAccount;

public interface ResponsibilityAssignmentStrategy {
    void assign(List<SessionResponsibility> responsibilities, List<UserAccount> members);
}
