package com.groupsync.backend.badminton.pairing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
public class RandomPairingStrategy implements PairingStrategy {
    @Override public PairingSuggestion create(List<PairingPlayer> players, long seed) { List<PairingPlayer> copy = new ArrayList<>(players); Collections.shuffle(copy, new Random(seed)); int usable = copy.size() - copy.size() % 4; List<PairingPlayer> a = new ArrayList<>(), b = new ArrayList<>(); for (int i = 0; i < usable; i += 4) { a.add(copy.get(i)); a.add(copy.get(i + 1)); b.add(copy.get(i + 2)); b.add(copy.get(i + 3)); } return new PairingSuggestion(a, b, new ArrayList<>(copy.subList(usable, copy.size()))); }
}
