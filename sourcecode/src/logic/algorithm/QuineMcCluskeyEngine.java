package logic.algorithm;

import logic.expression.NormalFormStrategy;
import logic.model.*;
import java.util.*;

public final class QuineMcCluskeyEngine {
    private final CoverSolver coverSolver;
    public QuineMcCluskeyEngine() { this(new CoverSolver()); }
    public QuineMcCluskeyEngine(CoverSolver coverSolver) { this.coverSolver = Objects.requireNonNull(coverSolver); }

    public MinimizationResult minimize(TruthTable input, NormalFormStrategy form) {
        List<Integer> targets = form.targetRows(input);
        List<Implicant> current = targets.stream().map(row -> new Implicant(input.variableCount(), row, 0)).toList();
        List<CombinationRound> rounds = new ArrayList<>();
        Set<Implicant> primes = new LinkedHashSet<>();
        Comparator<Implicant> order = Comparator.comparingInt(Implicant::onesCount).thenComparing(Implicant::pattern);
        while (true) {
            current = current.stream().sorted(order).toList();
            Set<Implicant> combined = new HashSet<>(), next = new LinkedHashSet<>();
            List<CombinationRound.Merge> merges = new ArrayList<>();
            for (int i = 0; i < current.size(); i++) for (int j = i + 1; j < current.size(); j++) {
                Implicant left = current.get(i), right = current.get(j);
                Optional<Implicant> pair = left.combine(right);
                if (pair.isPresent()) {
                    Implicant result = pair.get();
                    combined.add(left); combined.add(right); next.add(result);
                    merges.add(new CombinationRound.Merge(left, right, result));
                }
            }
            List<CombinationRound.Entry> entries = new ArrayList<>();
            for (Implicant term : current) {
                boolean wasCombined = combined.contains(term);
                entries.add(new CombinationRound.Entry(term, wasCombined));
                if (!wasCombined) primes.add(term);
            }
            rounds.add(new CombinationRound(rounds.size(), entries, merges));
            if (next.isEmpty()) break;
            current = new ArrayList<>(next);
        }
        List<Implicant> sortedPrimes = primes.stream().sorted(Comparator.comparing(Implicant::pattern)).toList();
        PrimeImplicantChart chart = new PrimeImplicantChart(targets, sortedPrimes);
        CoverSolver.Solution solution = coverSolver.solve(chart);
        var expression = form.build(solution.selectedIndices().stream().map(sortedPrimes::get).toList());
        MinimizationResult result = new MinimizationResult(input, form, rounds, chart, solution, expression);
        if (!result.equivalentToInput()) throw new IllegalStateException("Result failed truth-table verification");
        return result;
    }
}
