package logic.expression;

import logic.model.Implicant;
import java.util.List;

/** Minimize F' and complement each product using De Morgan, including the literals. */
public final class PosStrategy extends NormalFormStrategy {
    public String name() { return "POS"; }
    public boolean targetValue() { return false; }
    public BooleanExpression expressionFor(Implicant term) {
        var parts = literals(term, true);
        return parts.isEmpty() ? new BooleanExpression.Constant(false)
                : parts.size() == 1 ? parts.get(0) : new BooleanExpression.Or(parts);
    }
    public BooleanExpression build(List<Implicant> chosen) {
        var parts = chosen.stream().map(this::expressionFor).toList();
        return parts.isEmpty() ? new BooleanExpression.Constant(true)
                : parts.size() == 1 ? parts.get(0) : new BooleanExpression.And(parts);
    }
}
