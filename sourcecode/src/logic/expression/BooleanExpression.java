package logic.expression;

import java.util.List;
import java.util.stream.Collectors;

/** An evaluable expression tree, not a display string pretending to be a result. */
public sealed interface BooleanExpression {
    boolean evaluate(int row, int variableCount);
    String text();

    record Literal(int variable, boolean negated) implements BooleanExpression {
        public boolean evaluate(int row, int variables) {
            boolean bit = (row & (1 << (variables - variable - 1))) != 0;
            return negated != bit;
        }
        public String text() { return "" + (char) ('A' + variable) + (negated ? "'" : ""); }
    }

    record Constant(boolean value) implements BooleanExpression {
        public boolean evaluate(int row, int variables) { return value; }
        public String text() { return value ? "1" : "0"; }
    }

    record And(List<BooleanExpression> children) implements BooleanExpression {
        public And { children = List.copyOf(children); }
        public boolean evaluate(int row, int variables) {
            return children.stream().allMatch(child -> child.evaluate(row, variables));
        }
        public String text() {
            return children.stream().map(child -> child instanceof Or ? "(" + child.text() + ")" : child.text())
                    .collect(Collectors.joining(" · "));
        }
    }

    record Or(List<BooleanExpression> children) implements BooleanExpression {
        public Or { children = List.copyOf(children); }
        public boolean evaluate(int row, int variables) {
            return children.stream().anyMatch(child -> child.evaluate(row, variables));
        }
        public String text() { return children.stream().map(BooleanExpression::text).collect(Collectors.joining(" + ")); }
    }
}
