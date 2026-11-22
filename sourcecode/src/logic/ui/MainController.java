package logic.ui;

import logic.algorithm.QuineMcCluskeyEngine;
import logic.expression.NormalFormStrategy;
import logic.model.MinimizationResult;
import logic.model.TruthTable;

/** Thin application controller. Domain classes never import Swing. */
public final class MainController {
    private final QuineMcCluskeyEngine engine = new QuineMcCluskeyEngine();
    public MinimizationResult minimize(TruthTable input, NormalFormStrategy form) {
        return engine.minimize(input, form);
    }
}
