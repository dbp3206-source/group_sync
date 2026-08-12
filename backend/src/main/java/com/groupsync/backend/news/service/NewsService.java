package com.groupsync.backend.news.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.MatchResponses.News;
import com.groupsync.backend.badminton.model.NewsType;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.GroupRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.news.model.GroupNews;
import com.groupsync.backend.news.repository.GroupNewsRepository;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class NewsService {
    private final GroupNewsRepository newsRepository; private final GroupRepository groupRepository; private final MembershipRepository membershipRepository; private final UserAccountRepository userRepository;
    public NewsService(GroupNewsRepository newsRepository, GroupRepository groupRepository, MembershipRepository membershipRepository, UserAccountRepository userRepository) { this.newsRepository = newsRepository; this.groupRepository = groupRepository; this.membershipRepository = membershipRepository; this.userRepository = userRepository; }
    @Transactional(readOnly = true) public List<News> list(AuthenticatedUser actor, Long groupId) { requireMember(groupId, actor.getId()); return newsRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(n -> new News(n.getId(), n.getType().name(), n.getTitle(), n.getContent(), n.getSourceKey() == null ? null : n.getId(), n.getCreatedAt())).toList(); }
    @Transactional public News announce(AuthenticatedUser actor, Long groupId, String title, String content) { Membership membership = requireMember(groupId, actor.getId()); if (membership.getRole() == GroupRole.MEMBER) throw new ForbiddenException("Only the owner or organizer can publish announcements."); Group group = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found.")); GroupNews news = newsRepository.save(new GroupNews(group, userRepository.findById(actor.getId()).orElseThrow(), NewsType.ANNOUNCEMENT, title.trim(), content.trim(), null)); return new News(news.getId(), news.getType().name(), news.getTitle(), news.getContent(), null, news.getCreatedAt()); }
    @Transactional public void createSystem(Long groupId, String title, String content, String sourceKey) { if (newsRepository.findBySourceKey(sourceKey).isEmpty()) { Group group = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group not found.")); newsRepository.save(new GroupNews(group, null, NewsType.MATCH_RESULT, title, content, sourceKey)); } }
    private Membership requireMember(Long groupId, Long userId) { return membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
}
