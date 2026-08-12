package com.groupsync.backend.badminton.pairing;
import java.util.List;
public class ManualPairingStrategy implements PairingStrategy { @Override public PairingSuggestion create(List<PairingPlayer> players, long seed) { throw new UnsupportedOperationException("Manual pairing is supplied by the organizer."); } }
