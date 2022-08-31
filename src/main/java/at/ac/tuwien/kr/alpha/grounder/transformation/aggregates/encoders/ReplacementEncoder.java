package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.*;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.stringtemplate.v4.ST;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

/**
 * Class which generates replacement atoms for dynamic aggregates, used in aggregate rewriting to replace the aggregate atom
 */
public class ReplacementEncoder {


    private static final ST COUNT_CANDIDATE_TEMPLATE = new ST("<id>_cnt_candidate");
    private static final ST COUNT_SORTING_GRID_RESULT_TEMPLATE = new ST("<id>_sorting_grid_result");

    private static final ST MIN_MAX_ELEMENT_TUPLE_TEMPLATE = new ST("<id>_element_tuple");

    private static final ST MIN_MIN_ELEMENT_TUPLE_TEMPLATE = new ST("<id>_min_element_tuple");

    private static final ST MAX_MAX_ELEMENT_TUPLE_TEMPLATE = new ST("<id>_max_element_tuple");

    private static final ST SUM_PARTIAL_SUM_TEMPLATE = new ST("<id>_<tag>_partial_sum_at_index");

    private static final ST SUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE = new ST("<id>_<tag>_partial_sum_has_greater");

    private static final ST SUM_FULLSUM_PARTIAL_SUM_TEMPLATE = new ST("<id>_fullsum_<tag>_partial_sum_at_index");

    private static final ST SUM_FULLSUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE = new ST("<id>_fullsum_<tag>_partial_sum_has_greater");

    private static final ST AGGREGATE_RESULT_TEMPLATE = new ST("<id>_result");

    private static final Set<ThriceTruth> SIGNS_T = asSet(TRUE);

    private static final Set<ThriceTruth> SIGNS_F = asSet(FALSE);

    private static final Set<ThriceTruth> DEFAULT_BODY_SIGNS = asSet(TRUE, MBT);


    /**
     * Method which returns the appropriate replacement for the aggregate, split into two lists of HeuristicDirectiveAtoms, one for the
     * positive atoms in the replacement, titled "positive", and one for the negative atoms, titled "negative".
     * @param aggregateAtom the atom for which to get the dynamic replacement
     * @param aggregateArguments the arguments respective to the aggregate Atom
     * @param id the aggregate atoms id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    public static Map<String, List<HeuristicDirectiveAtom>> getReplacements(AggregateAtom aggregateAtom, Term aggregateArguments, String id) {
        AggregateAtom.AggregateFunctionSymbol function = aggregateAtom.getAggregatefunction();
        ComparisonOperator operator = aggregateAtom.getLowerBoundOperator();
        Term resultTerm = aggregateAtom.getLowerBoundTerm();
        switch (function) {
            case COUNT:
                if (operator == ComparisonOperator.EQ) {
                    return encodeCountEq(aggregateArguments, resultTerm, id);
                } else if (operator == ComparisonOperator.LE) {
                    return encodeCountLe(aggregateArguments, resultTerm, id);
                } else {
                    throw new UnsupportedOperationException("No fitting encoder for aggregate function " + function + "and operator " + operator + "!");
                }
            case MIN:
                if (operator == ComparisonOperator.EQ) {
                    return encodeMinEq(aggregateArguments, resultTerm, id);
                } else {
                    return encodeMinOp(aggregateArguments, resultTerm, id, operator);
                }
            case MAX:
                if (operator == ComparisonOperator.EQ) {
                    return encodeMaxEq(aggregateArguments, resultTerm, id);
                } else {
                    return encodeMaxOp(aggregateArguments, resultTerm, id, operator);
                }
            case SUM:
                if (operator == ComparisonOperator.EQ) {
                    return encodeSumEq(aggregateArguments, resultTerm, id);
                } else if (operator == ComparisonOperator.LE) {
                    return encodeSumLe(aggregateArguments, resultTerm, id);
                } else {
                    throw new UnsupportedOperationException("No fitting encoder for aggregate function " + function + "and operator " + operator + "!");
                }
            default:
                throw new UnsupportedOperationException("Unsupported aggregate function/comparison operator: " + function + ", " + operator);
        }
    }

    /**
     * Replaces e.g. CNT = #count{{X : b(X)}} with
     * $id$_cnt_candidate(ARGS, CNT), $id$_sorting_grid_result(ARGS, CNT), CNT1 = CNT + 1, not T $id$_sorting_grid_result(ARGS, CNT1)
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeCountEq(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String countCandidateName = new ST(COUNT_CANDIDATE_TEMPLATE).add("id", id).render();
        String sortingGridResultName = new ST(COUNT_SORTING_GRID_RESULT_TEMPLATE).add("id", id).render();

        Term resultPlusOne = VariableTerm.getAnonymousInstance();
        Integer one = 1;
        Term arithmeticTerm = ArithmeticTerm.getInstance(resultTerm, ArithmeticTerm.ArithmeticOperator.PLUS, ConstantTerm.getInstance(one));

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(countCandidateName, 2, true), argumentTerm, resultTerm)));
        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(sortingGridResultName, 2, true), argumentTerm, resultTerm)));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(resultPlusOne, arithmeticTerm, ComparisonOperator.EQ)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_T, new BasicAtom(Predicate.getInstance(sortingGridResultName, 2, true), argumentTerm, resultPlusOne)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);
        return map;
    }

    /**
     * Replaces e.g. CNT <= #count{{X : b(X)}} with
     * $id$_result(ARGS, CNT)
     * in the heuristic directive.
     * Here, no special rewriting is required, as the original aggregate encoding is already dynamic.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeCountLe(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String outputPredicateName = new ST(AGGREGATE_RESULT_TEMPLATE).add("id", id).render();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(outputPredicateName, 2, true), argumentTerm, resultTerm)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. MIN = #min{{X : b(X)}} with
     * $id$_element_tuple(ARGS, MIN), not F $id$_min_element_tuple(ARGS, MIN)
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeMinEq(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String minElementTupleName = new ST(MIN_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();
        String minMinElementTupleName = new ST(MIN_MIN_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(minElementTupleName, 2, true), argumentTerm, resultTerm)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_F, new BasicAtom(Predicate.getInstance(minMinElementTupleName, 2, true), argumentTerm, resultTerm)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. VALUE $op$ #min{{X : b(X)}} with
     * $id$_element_tuple(ARGS, MIN), not F $id$_min_element_tuple(ARGS, MIN), VALUE $op$ MIN
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @param operator The operator between aggregate and result variable
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeMinOp(Term argumentTerm, Term resultTerm, String id, ComparisonOperator operator) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String minElementTupleName = new ST(MIN_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();
        String minMinElementTupleName = new ST(MIN_MIN_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();

        Term currentMin = VariableTerm.getAnonymousInstance();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(minElementTupleName, 2, true), argumentTerm, currentMin)));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(resultTerm, currentMin, operator)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_F, new BasicAtom(Predicate.getInstance(minMinElementTupleName, 2, true), argumentTerm, currentMin)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. MAX = #max{{X : b(X)}} with
     * $id$_element_tuple(ARGS, MAX), not F $id$_max_element_tuple(ARGS, MAX)
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeMaxEq(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String maxElementTupleName = new ST(MIN_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();
        String maxMaxElementTupleName = new ST(MAX_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(maxElementTupleName, 2, true), argumentTerm, resultTerm)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_F, new BasicAtom(Predicate.getInstance(maxMaxElementTupleName, 2, true), argumentTerm, resultTerm)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. VALUE $op$ #max{{X : b(X)}} with
     * $id$_element_tuple(ARGS, MAX), not F $id$_max_element_tuple(ARGS, MAX), VALUE $op$ MAX
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @param operator The operator between aggregate and result variable
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeMaxOp(Term argumentTerm, Term resultTerm, String id, ComparisonOperator operator) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String maxElementTupleName = new ST(MIN_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();
        String maxMaxElementTupleName = new ST(MAX_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();

        Term currentMax = VariableTerm.getAnonymousInstance();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(maxElementTupleName, 2, true), argumentTerm, currentMax)));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(resultTerm, currentMax, operator)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_F, new BasicAtom(Predicate.getInstance(maxMaxElementTupleName, 2, true), argumentTerm, currentMax)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. SUM = #sum{{X : b(X)}} with
     * $id$_pos_partial_sum_at_index(ARGS, PSUM, _), not T $id$_pos_partial_sum_has_greater(ARGS, PSUM),
     * $id$_neg_partial_sum_at_index(ARGS, NSUM, _), not T $id$_neg_partial_sum_has_greater(ARGS, NSUM),
     * SUM = PSUM - NSUM
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeSumEq(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String positivePartialSumName = new ST(SUM_PARTIAL_SUM_TEMPLATE).add("id", id).add("tag", "pos").render();
        String positivePartialSumHasGreaterName = new ST(SUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE).add("id", id).add("tag", "pos").render();

        String negativePartialSumName = new ST(SUM_PARTIAL_SUM_TEMPLATE).add("id", id).add("tag", "neg").render();
        String negativePartialSumHasGreaterName = new ST(SUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE).add("id", id).add("tag", "neg").render();

        Term positiveSum = VariableTerm.getAnonymousInstance();
        Term negativeSum = VariableTerm.getAnonymousInstance();

        Term arithmeticTerm = ArithmeticTerm.getInstance(positiveSum, ArithmeticTerm.ArithmeticOperator.MINUS, negativeSum);

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(positivePartialSumName, 3, true), argumentTerm, positiveSum, VariableTerm.getAnonymousInstance())));
        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(negativePartialSumName, 3, true), argumentTerm, negativeSum, VariableTerm.getAnonymousInstance())));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(resultTerm, arithmeticTerm, ComparisonOperator.EQ)));


        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_T, new BasicAtom(Predicate.getInstance(positivePartialSumHasGreaterName, 2, true), argumentTerm, positiveSum)));
        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_T, new BasicAtom(Predicate.getInstance(negativePartialSumHasGreaterName, 2, true), argumentTerm, negativeSum)));


        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    /**
     * Replaces e.g. VALUE <= #sum{{X : b(X)}} with
     * $id$_pos_partial_sum_at_index(ARGS, PSUM, _), not T $id$_pos_partial_sum_has_greater(ARGS, PSUM),
     * $id$_neg_partial_sum_at_index(ARGS, NSUM, _), not T $id$_neg_partial_sum_has_greater(ARGS, NSUM),
     * SUM = PSUM - NSUM, VALUE <= SUM
     * in the heuristic directive.
     * @param argumentTerm The aggregates arguments
     * @param resultTerm The variable term for the result
     * @param id The aggregates id
     * @return A map containing both the positive and negative replacement atoms in two separate lists
     */
    private static Map<String, List<HeuristicDirectiveAtom>> encodeSumLe(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String positivePartialSumName = new ST(SUM_FULLSUM_PARTIAL_SUM_TEMPLATE).add("id", id).add("tag", "pos").render();
        String positivePartialSumHasGreaterName = new ST(SUM_FULLSUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE).add("id", id).add("tag", "pos").render();

        String negativePartialSumName = new ST(SUM_FULLSUM_PARTIAL_SUM_TEMPLATE).add("id", id).add("tag", "neg").render();
        String negativePartialSumHasGreaterName = new ST(SUM_FULLSUM_PARTIAL_SUM_HAS_GREATER_TEMPLATE).add("id", id).add("tag", "neg").render();

        Term positiveSum = VariableTerm.getAnonymousInstance();
        Term negativeSum = VariableTerm.getAnonymousInstance();
        Term sum = VariableTerm.getAnonymousInstance();

        Term arithmeticTerm = ArithmeticTerm.getInstance(positiveSum, ArithmeticTerm.ArithmeticOperator.MINUS, negativeSum);

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(positivePartialSumName, 3, true), argumentTerm, positiveSum, VariableTerm.getAnonymousInstance())));
        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(negativePartialSumName, 3, true), argumentTerm, negativeSum, VariableTerm.getAnonymousInstance())));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(sum, arithmeticTerm, ComparisonOperator.EQ)));
        positiveAtoms.add(HeuristicDirectiveAtom.body(null, new ComparisonAtom(resultTerm, sum, ComparisonOperator.LE)));


        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_T, new BasicAtom(Predicate.getInstance(positivePartialSumHasGreaterName, 2, true), argumentTerm, positiveSum)));
        negativeAtoms.add(HeuristicDirectiveAtom.body(SIGNS_T, new BasicAtom(Predicate.getInstance(negativePartialSumHasGreaterName, 2, true), argumentTerm, negativeSum)));


        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }
}
