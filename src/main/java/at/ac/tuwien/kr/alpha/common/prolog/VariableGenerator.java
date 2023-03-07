package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.IntIdGenerator;

/**
 * Simple Utility Class to generate Weight and Level Variable names, these should be protected, and not accessible to
 * an End User.
 * They are currently unprotected, with weight variables being "WEIGHT[number]" and level being "LEVEL[number].
 */
public class VariableGenerator {

    private static final String WEIGHT_VARIABLE_PREFIX = "WEIGHT";
    private static final String LEVEL_VARIABLE_PREFIX = "LEVEL";
    public static final IntIdGenerator WEIGHT_VARIABLE_COUNTER = new IntIdGenerator();
    public static final IntIdGenerator LEVEL_VARIABLE_COUNTER = new IntIdGenerator();
    public static VariableTerm getNextWeightVariable() {
        return VariableTerm.getInstance(WEIGHT_VARIABLE_PREFIX + WEIGHT_VARIABLE_COUNTER.getNextId());
    }

    public static VariableTerm getNextLevelVariable() {
        return VariableTerm.getInstance(LEVEL_VARIABLE_PREFIX + LEVEL_VARIABLE_COUNTER.getNextId());
    }


}
