package logic.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PrimeImplicantChart {
    private final List<Integer> targets;
    private final List<Implicant> primes;

    public PrimeImplicantChart(List<Integer> targets, List<Implicant> primes) {
        this.targets = List.copyOf(targets);
        this.primes = List.copyOf(primes);
    }
    public List<Integer> targets() { return targets; }
    public List<Implicant> primes() { return primes; }
    public boolean covers(int primeIndex, int row) { return primes.get(primeIndex).covers(row); }
    public int coverageMask(int primeIndex) {
        int mask = 0;
        for (int row : targets) if (covers(primeIndex, row)) mask |= 1 << row;
        return mask;
    }
    public int targetMask() {
        int mask = 0;
        for (int row : targets) mask |= 1 << row;
        return mask;
    }
    public Set<Integer> essentialIndices() {
        Set<Integer> result = new LinkedHashSet<>();
        for (int row : targets) {
            int count = 0, only = -1;
            for (int i = 0; i < primes.size(); i++) if (covers(i, row)) { count++; only = i; }
            if (count == 0) throw new IllegalStateException("Uncovered target row " + row);
            if (count == 1) result.add(only);
        }
        return Set.copyOf(result);
    }
}
