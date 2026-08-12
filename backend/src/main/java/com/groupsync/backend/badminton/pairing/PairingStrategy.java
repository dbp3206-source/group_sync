package com.groupsync.backend.badminton.pairing;
import java.util.List;
public interface PairingStrategy { PairingSuggestion create(List<PairingPlayer> players, long seed); }
