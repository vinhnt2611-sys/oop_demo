package logic.model;

import java.util.List;

/** Complete snapshot of one tabulation column; no mutable UI state is stored here. */
public record CombinationRound(int index, List<Entry> entries, List<Merge> merges) {
    public CombinationRound {
        entries = List.copyOf(entries);
        merges = List.copyOf(merges);
    }
    public record Entry(Implicant implicant, boolean combined) {}
    public record Merge(Implicant left, Implicant right, Implicant result) {}
}
