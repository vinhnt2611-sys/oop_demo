package logic.expression;

import logic.model.Implicant;
import java.util.List;

public final class SopStrategy extends NormalFormStrategy {
    public String name() { return "SOP"; }
    public boolean targetValue() { return true; }
    public BooleanExpression expressionFor(Implicant term) {
        var parts = literals(term, false);
        return parts.isEmpty() ? new BooleanExpression.Constant(true)
                : parts.size() == 1 ? parts.get(0) : new BooleanExpression.And(parts);
    }
    public BooleanExpression build(List<Implicant> chosen) {
        var parts = chosen.stream().map(this::expressionFor).toList();
        return parts.isEmpty() ? new BooleanExpression.Constant(false)
                : parts.size() == 1 ? parts.get(0) : new BooleanExpression.Or(parts);
    }
}
