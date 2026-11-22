package logic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A cube: mask bit 1 means that variable was eliminated (x), NOT input don't-care. */
public record Implicant(int variableCount, int value, int mask) {
    public Implicant {
        if (variableCount != 3 && variableCount != 4) throw new IllegalArgumentException("Invalid size");
        int full = (1 << variableCount) - 1;
        if ((value & ~full) != 0 || (mask & ~full) != 0)
            throw new IllegalArgumentException("Bits outside input range");
        value &= ~mask;
    }

    public Optional<Implicant> combine(Implicant other) {
        if (variableCount != other.variableCount || mask != other.mask) return Optional.empty();
        int difference = value ^ other.value;
        if (Integer.bitCount(difference) != 1) return Optional.empty();
        return Optional.of(new Implicant(variableCount, value, mask | difference));
    }

    public boolean covers(int row) {
        return row >= 0 && row < 1 << variableCount && ((row ^ value) & ~mask) == 0;
    }
    public int onesCount() { return Integer.bitCount(value); }
    public int literalCount() { return variableCount - Integer.bitCount(mask); }
    public List<Integer> coveredRows() {
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < 1 << variableCount; row++) if (covers(row)) rows.add(row);
        return List.copyOf(rows);
    }
    public String pattern() {
        StringBuilder text = new StringBuilder();
        for (int bit = variableCount - 1; bit >= 0; bit--)
            text.append((mask & (1 << bit)) != 0 ? 'x' : (value & (1 << bit)) != 0 ? '1' : '0');
        return text.toString();
    }
}
