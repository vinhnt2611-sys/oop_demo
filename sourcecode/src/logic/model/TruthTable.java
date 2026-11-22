package logic.model;

import java.util.ArrayList;
import java.util.List;

/** Immutable truth table. A is the most significant input bit. */
public final class TruthTable {
    private final int variableCount;
    private final boolean[] outputs;

    public TruthTable(int variableCount, boolean[] outputs) {
        if (variableCount != 3 && variableCount != 4)
            throw new IllegalArgumentException("Only 3 or 4 variables are supported");
        if (outputs == null || outputs.length != 1 << variableCount)
            throw new IllegalArgumentException("Wrong number of truth-table rows");
        this.variableCount = variableCount;
        this.outputs = outputs.clone();
    }

    public static TruthTable ofMinterms(int variables, int... minterms) {
        if (variables != 3 && variables != 4) throw new IllegalArgumentException("Invalid variables");
        boolean[] values = new boolean[1 << variables];
        for (int row : minterms) {
            if (row < 0 || row >= values.length) throw new IllegalArgumentException("Invalid minterm");
            values[row] = true;
        }
        return new TruthTable(variables, values);
    }

    public int variableCount() { return variableCount; }
    public int rowCount() { return outputs.length; }
    public boolean output(int row) { return outputs[row]; }
    public List<Integer> rowsWithValue(boolean value) {
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < outputs.length; row++) if (outputs[row] == value) rows.add(row);
        return List.copyOf(rows);
    }
}
