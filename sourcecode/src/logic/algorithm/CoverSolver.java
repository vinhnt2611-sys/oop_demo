package logic.algorithm;

import logic.model.PrimeImplicantChart;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Exhaustive set cover: fewest PIs first, then fewest literals, then deterministic order. */
public final class CoverSolver {
    public record Solution(List<Integer> selectedIndices, Set<Integer> essentialIndices,
                           int subsetsChecked, int optimalCoverCount, int remainingMask) {
        public Solution {
            selectedIndices = List.copyOf(selectedIndices);
            essentialIndices = Set.copyOf(essentialIndices);
        }
    }

    public Solution solve(PrimeImplicantChart chart) {
        Set<Integer> essential = chart.essentialIndices();
        int covered = 0;
        for (int index : essential) covered |= chart.coverageMask(index);
        int remaining = chart.targetMask() & ~covered;
        if (remaining == 0) return new Solution(essential.stream().sorted().toList(), essential, 0, 1, 0);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < chart.primes().size(); i++)
            if (!essential.contains(i) && (chart.coverageMask(i) & remaining) != 0) candidates.add(i);
        int best = -1, bestSize = Integer.MAX_VALUE, bestLiterals = Integer.MAX_VALUE, ties = 0;
        int checked = 0;
        for (int subset = 0; subset < (1 << candidates.size()); subset++) {
            checked++;
            int count = Integer.bitCount(subset);
            if (count > bestSize) continue;
            int coverage = 0, literals = 0;
            for (int bit = 0; bit < candidates.size(); bit++) if ((subset & (1 << bit)) != 0) {
                int index = candidates.get(bit);
                coverage |= chart.coverageMask(index);
                literals += chart.primes().get(index).literalCount();
            }
            if ((coverage & remaining) != remaining) continue;
            if (count < bestSize || (count == bestSize && literals < bestLiterals)) {
                best = subset; bestSize = count; bestLiterals = literals; ties = 1;
            } else if (count == bestSize && literals == bestLiterals) ties++;
        }
        if (best < 0) throw new IllegalStateException("No complete PI cover");
        List<Integer> selected = new ArrayList<>(essential);
        for (int bit = 0; bit < candidates.size(); bit++) if ((best & (1 << bit)) != 0) selected.add(candidates.get(bit));
        selected.sort(Integer::compareTo);
        return new Solution(selected, essential, checked, ties, remaining);
    }
}
