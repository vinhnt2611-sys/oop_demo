package logic;

import logic.algorithm.QuineMcCluskeyEngine;
import logic.expression.*;
import logic.model.*;
import logic.ui.TruthTableModel;
import java.util.*;

/** No test libraries. An independent ternary-cube oracle checks global minimum cost. */
public final class EngineTest {
    private static long assertions;
    private static final QuineMcCluskeyEngine ENGINE = new QuineMcCluskeyEngine();
    private static final NormalFormStrategy SOP = new SopStrategy(), POS = new PosStrategy();
    private record Cube(int rows, int literals, String pattern) {}
    private record Cost(int terms, int literals) implements Comparable<Cost> {
        public int compareTo(Cost other) {
            int primary = Integer.compare(terms, other.terms);
            return primary != 0 ? primary : Integer.compare(literals, other.literals);
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        checkModelAndExamples();
        int functions = 0;
        for (int variables : new int[] {3, 4}) {
            int rows = 1 << variables, full = (1 << rows) - 1;
            List<Cube> cubes = enumerateCubes(variables);
            Map<Integer, Cost> costs = new HashMap<>();
            for (int mask = 0; mask <= full; mask++) {
                boolean[] values = new boolean[rows];
                for (int row = 0; row < rows; row++) values[row] = (mask & (1 << row)) != 0;
                TruthTable table = new TruthTable(variables, values);
                for (NormalFormStrategy form : new NormalFormStrategy[] {SOP, POS}) {
                    int target = form.targetValue() ? mask : full ^ mask;
                    MinimizationResult result = ENGINE.minimize(table, form);
                    for (int row = 0; row < rows; row++)
                        require(result.expression().evaluate(row, variables) == values[row], "Truth-table mismatch");
                    List<Cube> valid = cubes.stream().filter(c -> (c.rows & ~target) == 0).toList();
                    Set<String> expectedPrimes = new HashSet<>();
                    for (Cube cube : valid) {
                        boolean maximal = true;
                        for (Cube larger : valid) if (cube.rows != larger.rows && (cube.rows & larger.rows) == cube.rows) {
                            maximal = false; break;
                        }
                        if (maximal) expectedPrimes.add(cube.pattern);
                    }
                    Set<String> actualPrimes = new HashSet<>();
                    for (Implicant prime : result.chart().primes()) actualPrimes.add(prime.pattern());
                    require(expectedPrimes.equals(actualPrimes), "PI completeness mismatch");
                    Cost expected = costs.computeIfAbsent(target, ignored -> minimumCost(target, valid, new HashMap<>()));
                    require(expected.equals(new Cost(result.selectedPrimes().size(), result.literalCount())), "Not globally minimal");
                    checkTrace(result, target);
                }
                functions++;
                if (variables == 4 && (mask + 1) % 8192 == 0) System.out.println("4 variables: " + (mask + 1) + "/65536 truth tables checked");
            }
            System.out.println("PASS: all " + (full + 1) + " functions with " + variables + " variables, SOP and POS");
        }
        System.out.printf(Locale.ROOT, "PASS: %,d functions / %,d minimizations / %,d assertions, %.2fs%n",
                functions, functions * 2, assertions, (System.nanoTime() - start) / 1e9);
    }

    private static void checkModelAndExamples() {
        boolean[] original = new boolean[8];
        TruthTable immutable = new TruthTable(3, original); original[0] = true;
        require(!immutable.output(0), "TruthTable must copy input");
        boolean rejected = false;
        try { new TruthTable(2, new boolean[4]); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "Reject unsupported size");
        require(new Implicant(3, 3, 0).combine(new Implicant(3, 7, 0)).orElseThrow().pattern().equals("x11"), "Basic merge");
        require(new Implicant(3, 0, 1).combine(new Implicant(3, 0, 2)).isEmpty(), "Reject different x positions");
        require(ENGINE.minimize(TruthTable.ofMinterms(3, 1, 3, 5, 7), SOP).expression().text().equals("C"), "Known SOP");
        require(ENGINE.minimize(TruthTable.ofMinterms(3, 1, 3, 5, 7), POS).expression().text().equals("C"), "Known POS");
        MinimizationResult cycle = ENGINE.minimize(TruthTable.ofMinterms(3, 0, 1, 2, 5, 6, 7), SOP);
        require(cycle.solution().essentialIndices().isEmpty(), "Cycle has no essential PI");
        require(cycle.solution().subsetsChecked() == 64, "Cycle enumerates all 64 subsets");
        require(cycle.solution().optimalCoverCount() == 2, "Cycle has two optimal covers");
        require(cycle.selectedPrimes().size() == 3 && cycle.literalCount() == 6, "Cycle minimum cost");
        TruthTableModel model = new TruthTableModel();
        model.toggle(5); require(model.snapshot().output(5), "Toggle output");
        model.setTable(TruthTable.ofMinterms(4, 15));
        require(model.getRowCount() == 16 && model.getColumnCount() == 6, "Switch to four variables");
        require(model.getValueAt(15, 1).equals(1) && model.getValueAt(15, 5).equals(1), "Bit/column mapping");
        model.setTable(TruthTable.ofMinterms(3));
        require(model.getRowCount() == 8 && !model.snapshot().output(5), "Reset when switching back");
    }

    private static void checkTrace(MinimizationResult result, int target) {
        for (int index = 0; index < result.rounds().size(); index++) {
            CombinationRound round = result.rounds().get(index);
            require(round.index() == index, "Round numbering");
            Set<Implicant> seen = new HashSet<>(), mergedParents = new HashSet<>(), outputs = new HashSet<>();
            for (var merge : round.merges()) {
                require(merge.left().mask() == merge.right().mask(), "Merge x positions");
                require(Integer.bitCount(merge.left().value() ^ merge.right().value()) == 1, "Merge bit difference");
                mergedParents.add(merge.left()); mergedParents.add(merge.right()); outputs.add(merge.result());
            }
            for (var entry : round.entries()) {
                Implicant term = entry.implicant();
                require(seen.add(term), "Duplicate term in round");
                require(entry.combined() == mergedParents.contains(term), "Incorrect combined tick");
                for (int row : term.coveredRows()) require((target & (1 << row)) != 0, "Cube covers forbidden row");
            }
            if (index + 1 < result.rounds().size()) {
                Set<Implicant> next = new HashSet<>();
                for (var entry : result.rounds().get(index + 1).entries()) next.add(entry.implicant());
                require(next.equals(outputs), "Next column does not match merge results");
            } else require(outputs.isEmpty(), "Stopped too soon");
        }
    }

    /** Enumerate all 3^n cubes independently using ternary digits: 0, 1, or absent. */
    private static List<Cube> enumerateCubes(int variables) {
        int count = (int) Math.pow(3, variables);
        List<Cube> cubes = new ArrayList<>();
        for (int code = 0; code < count; code++) {
            int rest = code, literalCount = 0, rows = 0;
            int[] digits = new int[variables]; StringBuilder pattern = new StringBuilder();
            for (int variable = 0; variable < variables; variable++) {
                digits[variable] = rest % 3; rest /= 3;
                if (digits[variable] != 2) literalCount++;
                pattern.append(digits[variable] == 2 ? 'x' : (char) ('0' + digits[variable]));
            }
            for (int row = 0; row < 1 << variables; row++) {
                boolean match = true;
                for (int variable = 0; variable < variables; variable++)
                    if (digits[variable] != 2 && digits[variable] != ((row >> (variables - variable - 1)) & 1)) match = false;
                if (match) rows |= 1 << row;
            }
            cubes.add(new Cube(rows, literalCount, pattern.toString()));
        }
        return cubes;
    }

    /** Set-cover DP over uncovered rows, using ALL valid cubes, not the engine's PIs. */
    private static Cost minimumCost(int remaining, List<Cube> valid, Map<Integer, Cost> memo) {
        if (remaining == 0) return new Cost(0, 0);
        if (memo.containsKey(remaining)) return memo.get(remaining);
        int first = Integer.lowestOneBit(remaining);
        Cost best = new Cost(100, 100);
        for (Cube cube : valid) if ((cube.rows & first) != 0) {
            Cost suffix = minimumCost(remaining & ~cube.rows, valid, memo);
            Cost proposal = new Cost(suffix.terms + 1, suffix.literals + cube.literals);
            if (proposal.compareTo(best) < 0) best = proposal;
        }
        memo.put(remaining, best); return best;
    }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
