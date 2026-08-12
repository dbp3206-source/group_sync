package com.groupsync.backend.badminton.dto;
import java.util.List;
public final class AllocationResponses {
    private AllocationResponses() { }
    public record Player(Long userId, String displayName, int position) { }
    public record Allocation(Long id, Long courtId, String courtName, int roundNumber, String status, List<Player> players) { }
    public record PairingPlayer(Long userId, String displayName) { }
    public record Pairing(Long courtId, String courtName, int roundNumber, String strategy, List<PairingPlayer> sideA, List<PairingPlayer> sideB, List<PairingPlayer> unassigned) { }
}
