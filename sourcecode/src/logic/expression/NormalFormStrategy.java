package logic.expression;

import logic.model.Implicant;
import logic.model.TruthTable;
import java.util.List;

/** Shared abstraction used polymorphically by the engine and the GUI. */
public abstract class NormalFormStrategy {
    public abstract String name();
    public abstract boolean targetValue();
    public abstract BooleanExpression expressionFor(Implicant implicant);
    public abstract BooleanExpression build(List<Implicant> chosen);
    public List<Integer> targetRows(TruthTable table) { return table.rowsWithValue(targetValue()); }
    @Override public String toString() { return name(); }

    protected List<BooleanExpression> literals(Implicant term, boolean complement) {
        java.util.ArrayList<BooleanExpression> result = new java.util.ArrayList<>();
        for (int variable = 0; variable < term.variableCount(); variable++) {
            int bit = 1 << (term.variableCount() - variable - 1);
            if ((term.mask() & bit) != 0) continue;
            boolean isZero = (term.value() & bit) == 0;
            result.add(new BooleanExpression.Literal(variable, isZero ^ complement));
        }
        return List.copyOf(result);
    }
}
