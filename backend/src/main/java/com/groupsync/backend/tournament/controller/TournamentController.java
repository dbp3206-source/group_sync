package com.groupsync.backend.tournament.controller;
import java.util.List; import org.springframework.http.HttpStatus; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import com.groupsync.backend.auth.security.AuthenticatedUser; import com.groupsync.backend.tournament.dto.*; import com.groupsync.backend.tournament.service.TournamentService; import jakarta.validation.Valid;
@RestController @RequestMapping("/api/tournaments") public class TournamentController {
 private final TournamentService service; public TournamentController(TournamentService service){this.service=service;}
 @GetMapping("/groups/{groupId}") public List<TournamentResponses.Tournament> list(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long groupId){return service.list(a,groupId);}
 @PostMapping("/groups/{groupId}") @ResponseStatus(HttpStatus.CREATED) public TournamentResponses.Tournament create(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long groupId,@Valid @RequestBody TournamentRequests.Create r){return service.create(a,groupId,r);}
 @PostMapping("/{id}/open") public TournamentResponses.Tournament open(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id){return service.open(a,id);}
 @PostMapping("/{id}/start") public TournamentResponses.Tournament start(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id){return service.start(a,id);}
 @PostMapping("/{id}/complete") public TournamentResponses.Tournament complete(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id,@RequestParam Long championId){return service.complete(a,id,championId);}
 @PostMapping("/{id}/participants") public TournamentResponses.Participant register(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id,@Valid @RequestBody TournamentRequests.AddParticipant r){return service.register(a,id,r.userId());}
 @GetMapping("/{id}/participants") public List<TournamentResponses.Participant> participants(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id){return service.participants(a,id);}
 @PostMapping("/{id}/matches") public TournamentResponses.Bracket match(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id,@Valid @RequestBody TournamentRequests.CreateTournamentMatch r){return service.createMatch(a,id,r);}
 @GetMapping("/{id}/bracket") public List<TournamentResponses.Bracket> bracket(@AuthenticationPrincipal AuthenticatedUser a,@PathVariable Long id){return service.bracket(a,id);}
}
