package logic.model;

import logic.algorithm.CoverSolver;
import logic.expression.BooleanExpression;
import logic.expression.NormalFormStrategy;
import java.util.List;

public record MinimizationResult(TruthTable input, NormalFormStrategy form,
        List<CombinationRound> rounds, PrimeImplicantChart chart,
        CoverSolver.Solution solution, BooleanExpression expression) {
    public MinimizationResult { rounds = List.copyOf(rounds); }
    public List<Implicant> selectedPrimes() {
        return solution.selectedIndices().stream().map(chart.primes()::get).toList();
    }
    public int literalCount() { return selectedPrimes().stream().mapToInt(Implicant::literalCount).sum(); }
    public boolean equivalentToInput() {
        for (int row = 0; row < input.rowCount(); row++)
            if (expression.evaluate(row, input.variableCount()) != input.output(row)) return false;
        return true;
    }
}
