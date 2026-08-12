package com.groupsync.backend.badminton.pairing;
import java.util.List;
public record PairingSuggestion(List<PairingPlayer> sideA, List<PairingPlayer> sideB, List<PairingPlayer> unassigned) { }
