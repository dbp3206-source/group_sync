package com.groupsync.backend.tournament.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.badminton.repository.SeasonRepository;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.tournament.dto.TournamentRequests;
import com.groupsync.backend.tournament.dto.TournamentResponses;
import com.groupsync.backend.tournament.model.Tournament;
import com.groupsync.backend.tournament.model.TournamentCompetitionMode;
import com.groupsync.backend.tournament.model.TournamentEntry;
import com.groupsync.backend.tournament.model.TournamentMatch;
import com.groupsync.backend.tournament.model.TournamentStage;
import com.groupsync.backend.tournament.model.TournamentStatus;
import com.groupsync.backend.tournament.repository.TournamentEntryRepository;
import com.groupsync.backend.tournament.repository.TournamentMatchRepository;
import com.groupsync.backend.tournament.repository.TournamentRepository;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final TournamentMatchRepository matchRepository;
    private final MembershipRepository membershipRepository;
    private final SeasonRepository seasonRepository;
    private final BadmintonSessionRepository sessionRepository;
    private final UserAccountRepository userRepository;

    public TournamentService(
        TournamentRepository tournamentRepository,
        TournamentEntryRepository entryRepository,
        TournamentMatchRepository matchRepository,
        MembershipRepository membershipRepository,
        SeasonRepository seasonRepository,
        BadmintonSessionRepository sessionRepository,
        UserAccountRepository userRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.membershipRepository = membershipRepository;
        this.seasonRepository = seasonRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TournamentResponses.Tournament> list(AuthenticatedUser actor, Long groupId) {
        requireMember(groupId, actor.getId());
        return tournamentRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::summary).toList();
    }

    @Transactional
    public TournamentResponses.Tournament create(AuthenticatedUser actor, Long groupId, TournamentRequests.Create request) {
        requireOrganizer(groupId, actor.getId());
        Season season = seasonRepository.findById(request.seasonId())
            .filter(value -> value.getGroup().getId().equals(groupId))
            .orElseThrow(() -> new NotFoundException("Season not found."));
        BadmintonSession session = sessionRepository.findById(request.sessionId())
            .filter(value -> value.getGroup().getId().equals(groupId) && value.getSeason().getId().equals(season.getId()))
            .orElseThrow(() -> new NotFoundException("Badminton session not found."));
        TournamentCompetitionMode mode = request.competitionMode();
        Tournament tournament = new Tournament(session.getGroup(), season, session, request.name().trim(), "KNOCKOUT", mode, request.maxEntries());
        return summary(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponses.Tournament open(AuthenticatedUser actor, Long tournamentId) {
        Tournament tournament = requireOrganizerTournament(actor, tournamentId);
        tournament.open();
        return summary(tournament);
    }

    @Transactional
    public TournamentResponses.Entry addEntry(AuthenticatedUser actor, Long tournamentId, TournamentRequests.AddEntry request) {
        Tournament tournament = requireOrganizerTournament(actor, tournamentId);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new ConflictException("Tournament registration is closed.");
        }
        if (entryRepository.countByTournamentId(tournamentId) >= tournament.getMaxParticipants()) {
            throw new ConflictException("Tournament is full.");
        }
        int expectedMembers = tournament.getCompetitionMode() == TournamentCompetitionMode.SINGLES ? 1 : 2;
        if (request.memberIds().size() != expectedMembers || request.memberIds().stream().distinct().count() != expectedMembers) {
            throw new BadRequestException(expectedMembers == 1 ? "Singles needs exactly one player." : "Doubles needs exactly two different players.");
        }
        List<UserAccount> members = request.memberIds().stream().map(userId -> {
            requireMember(tournament.getGroup().getId(), userId);
            if (entryRepository.existsByTournamentIdAndMembersUserId(tournamentId, userId)) {
                throw new ConflictException("A player can only belong to one entry in this tournament.");
            }
            return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
        }).toList();
        String displayName = request.displayName() == null || request.displayName().isBlank()
            ? members.stream().map(UserAccount::getDisplayName).reduce((left, right) -> left + " / " + right).orElseThrow()
            : request.displayName().trim();
        TournamentEntry entry = new TournamentEntry(tournament, displayName, request.seedNumber());
        members.forEach(entry::addMember);
        return TournamentResponses.Entry.from(entryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<TournamentResponses.Entry> entries(AuthenticatedUser actor, Long tournamentId) {
        Tournament tournament = findTournament(tournamentId);
        requireMember(tournament.getGroup().getId(), actor.getId());
        return entryRepository.findByTournamentIdOrderBySeedNumberAscCreatedAtAsc(tournamentId).stream().map(TournamentResponses.Entry::from).toList();
    }

    @Transactional
    public TournamentResponses.Tournament start(AuthenticatedUser actor, Long tournamentId) {
        Tournament tournament = requireOrganizerTournament(actor, tournamentId);
        List<TournamentEntry> entries = entryRepository.findByTournamentIdOrderBySeedNumberAscCreatedAtAsc(tournamentId);
        if (entries.size() < 2) throw new ConflictException("Tournament needs at least two entries.");
        tournament.start();
        generateKnockout(tournament, entries);
        resolveByes(tournament);
        return summary(tournament);
    }

    @Transactional
    public TournamentResponses.Bracket recordWinner(AuthenticatedUser actor, Long tournamentId, Long tournamentMatchId, TournamentRequests.RecordWinner request) {
        Tournament tournament = requireOrganizerTournament(actor, tournamentId);
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) throw new ConflictException("Tournament is not in progress.");
        TournamentMatch match = matchRepository.findById(tournamentMatchId)
            .filter(value -> value.getTournament().getId().equals(tournamentId))
            .orElseThrow(() -> new NotFoundException("Tournament match not found."));
        if (match.getWinnerEntry() != null || match.getEntryA() == null || match.getEntryB() == null) {
            throw new ConflictException("This tournament match is not ready to record.");
        }
        TournamentEntry winner = entryRepository.findById(request.winnerEntryId())
            .filter(entry -> entry.getId().equals(match.getEntryA().getId()) || entry.getId().equals(match.getEntryB().getId()))
            .orElseThrow(() -> new BadRequestException("Winner must be one of the two entries in this match."));
        match.setWinnerEntry(winner);
        advanceWinner(tournament, match);
        resolveByes(tournament);
        return TournamentResponses.Bracket.from(match);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponses.Bracket> bracket(AuthenticatedUser actor, Long tournamentId) {
        Tournament tournament = findTournament(tournamentId);
        requireMember(tournament.getGroup().getId(), actor.getId());
        return matchRepository.findByTournamentIdOrderByStageAscMatchNumberAsc(tournamentId).stream().map(TournamentResponses.Bracket::from).toList();
    }

    // Package-visible so the deterministic bracket rules can be tested without HTTP setup.
    void generateKnockout(Tournament tournament, List<TournamentEntry> unsortedEntries) {
        List<TournamentEntry> entries = new ArrayList<>(unsortedEntries);
        entries.sort(Comparator.comparing(TournamentEntry::getSeedNumber, Comparator.nullsLast(Integer::compareTo)).thenComparing(TournamentEntry::getCreatedAt));
        int bracketSize = nextPowerOfTwo(entries.size());
        List<TournamentEntry> slots = new ArrayList<>(entries);
        while (slots.size() < bracketSize) slots.add(null);
        int firstRoundMatches = bracketSize / 2;
        for (int index = 0; index < firstRoundMatches; index++) {
            TournamentEntry entryA = slots.get(index);
            TournamentEntry entryB = slots.get(bracketSize - 1 - index);
            TournamentStage stage = firstRoundMatches == 1 ? TournamentStage.FINAL : TournamentStage.KNOCKOUT;
            Integer nextMatchNumber = firstRoundMatches == 1 ? null : firstRoundMatches + (index / 2) + 1;
            matchRepository.save(new TournamentMatch(tournament, stage, index + 1, nextMatchNumber, entryA, entryB));
        }
        int matchNumber = firstRoundMatches + 1;
        for (int matchesInRound = firstRoundMatches / 2; matchesInRound >= 1; matchesInRound /= 2) {
            int roundStart = matchNumber;
            for (int index = 0; index < matchesInRound; index++) {
                TournamentStage stage = matchesInRound == 1 ? TournamentStage.FINAL : TournamentStage.KNOCKOUT;
                Integer nextMatchNumber = matchesInRound == 1 ? null : roundStart + matchesInRound + (index / 2);
                matchRepository.save(new TournamentMatch(tournament, stage, matchNumber++, nextMatchNumber, null, null));
            }
        }
    }

    void resolveByes(Tournament tournament) {
        boolean progressed;
        do {
            progressed = false;
            List<TournamentMatch> matches = matchRepository.findByTournamentIdOrderByStageAscMatchNumberAsc(tournament.getId());
            for (TournamentMatch match : matches) {
                if (match.getWinnerEntry() != null || !match.hasExactlyOneEntry()) continue;
                List<TournamentMatch> feeders = matchRepository.findByTournamentIdAndNextMatchNumber(tournament.getId(), match.getMatchNumber());
                boolean feedersAreResolved = feeders.isEmpty() || feeders.stream().allMatch(feeder -> feeder.getWinnerEntry() != null);
                if (!feedersAreResolved) continue;
                match.setWinnerEntry(match.getOnlyEntry());
                advanceWinner(tournament, match);
                progressed = true;
            }
        } while (progressed && tournament.getStatus() == TournamentStatus.IN_PROGRESS);
    }

    private void advanceWinner(Tournament tournament, TournamentMatch match) {
        if (match.getNextMatchNumber() == null) {
            tournament.complete(match.getWinnerEntry());
            return;
        }
        TournamentMatch next = matchRepository.findByTournamentIdAndMatchNumber(tournament.getId(), match.getNextMatchNumber())
            .orElseThrow(() -> new IllegalStateException("Next tournament match is missing."));
        next.receiveWinner(match.getWinnerEntry());
    }

    private int nextPowerOfTwo(int value) { int result = 1; while (result < value) result *= 2; return result; }
    private TournamentResponses.Tournament summary(Tournament tournament) { return TournamentResponses.Tournament.from(tournament, (int) entryRepository.countByTournamentId(tournament.getId())); }
    private Tournament requireOrganizerTournament(AuthenticatedUser actor, Long tournamentId) { Tournament tournament = findTournament(tournamentId); requireOrganizer(tournament.getGroup().getId(), actor.getId()); return tournament; }
    private Tournament findTournament(Long tournamentId) { return tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("Tournament not found.")); }
    private void requireOrganizer(Long groupId, Long userId) { if (requireMember(groupId, userId).getRole() == GroupRole.MEMBER) throw new ForbiddenException("Only the owner or an organizer can manage tournaments."); }
    private com.groupsync.backend.group.model.Membership requireMember(Long groupId, Long userId) { return membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
}
