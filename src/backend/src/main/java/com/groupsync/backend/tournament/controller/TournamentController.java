package com.groupsync.backend.tournament.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.tournament.dto.TournamentRequests;
import com.groupsync.backend.tournament.dto.TournamentResponses;
import com.groupsync.backend.tournament.service.TournamentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/groups/{groupId}")
    public List<TournamentResponses.Tournament> list(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return tournamentService.list(actor, groupId); }
    @PostMapping("/groups/{groupId}") @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponses.Tournament create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody TournamentRequests.Create request) { return tournamentService.create(actor, groupId, request); }
    @PostMapping("/{tournamentId}/open")
    public TournamentResponses.Tournament open(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId) { return tournamentService.open(actor, tournamentId); }
    @PostMapping("/{tournamentId}/start")
    public TournamentResponses.Tournament start(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId) { return tournamentService.start(actor, tournamentId); }
    @PostMapping("/{tournamentId}/entries") @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponses.Entry addEntry(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId, @Valid @RequestBody TournamentRequests.AddEntry request) { return tournamentService.addEntry(actor, tournamentId, request); }
    @GetMapping("/{tournamentId}/entries")
    public List<TournamentResponses.Entry> entries(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId) { return tournamentService.entries(actor, tournamentId); }
    @GetMapping("/{tournamentId}/bracket")
    public List<TournamentResponses.Bracket> bracket(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId) { return tournamentService.bracket(actor, tournamentId); }
    @PostMapping("/{tournamentId}/matches/{tournamentMatchId}/winner")
    public TournamentResponses.Bracket recordWinner(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long tournamentId, @PathVariable Long tournamentMatchId, @Valid @RequestBody TournamentRequests.RecordWinner request) { return tournamentService.recordWinner(actor, tournamentId, tournamentMatchId, request); }
}
