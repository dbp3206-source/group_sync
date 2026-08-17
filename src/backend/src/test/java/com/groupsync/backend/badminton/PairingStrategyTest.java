package com.groupsync.backend.badminton;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.badminton.pairing.BalancedPairingStrategy;
import com.groupsync.backend.badminton.pairing.PairingPlayer;
import com.groupsync.backend.badminton.pairing.RandomPairingStrategy;

class PairingStrategyTest {
    private final List<PairingPlayer> players = List.of(
        new PairingPlayer(1L, "A", 3), new PairingPlayer(2L, "B", 2),
        new PairingPlayer(3L, "C", 1), new PairingPlayer(4L, "D", 3),
        new PairingPlayer(5L, "E", 2), new PairingPlayer(6L, "F", 1)
    );

    @Test
    void randomPairingIsRepeatableAndNeverDuplicatesPlayers() {
        var first = new RandomPairingStrategy().create(players, 42L);
        var second = new RandomPairingStrategy().create(players, 42L);
        assertThat(first).isEqualTo(second);
        assertThat(ids(first.sideA(), first.sideB(), first.unassigned())).doesNotHaveDuplicates();
        assertThat(first.sideA()).hasSize(2);
        assertThat(first.sideB()).hasSize(2);
    }

    @Test
    void balancedPairingKeepsFourPlayerDoublesAndLeavesRemainderUnassigned() {
        var result = new BalancedPairingStrategy().create(players, 7L);
        assertThat(result.sideA()).hasSize(2);
        assertThat(result.sideB()).hasSize(2);
        assertThat(result.unassigned()).hasSize(2);
        assertThat(ids(result.sideA(), result.sideB(), result.unassigned())).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void largeCourtPoolStillProducesOneValidDoublesSuggestion() {
        var result = new BalancedPairingStrategy().create(java.util.stream.IntStream.rangeClosed(1, 8)
            .mapToObj(id -> new PairingPlayer((long) id, "P" + id, 1)).toList(), 7L);

        assertThat(result.sideA()).hasSize(2);
        assertThat(result.sideB()).hasSize(2);
        assertThat(result.unassigned()).hasSize(4);
        assertThat(ids(result.sideA(), result.sideB(), result.unassigned())).doesNotHaveDuplicates();
    }

    @SafeVarargs
    private final List<Long> ids(List<PairingPlayer>... groups) { return java.util.Arrays.stream(groups).flatMap(List::stream).map(PairingPlayer::userId).collect(Collectors.toList()); }
}
