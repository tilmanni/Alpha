package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.*;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.terms.ArithmeticTerm;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.transformation.SignSetTransformation;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import org.antlr.v4.runtime.misc.Array2DHashSet;
import org.stringtemplate.v4.ST;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.asSet;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

public class ReplacementEncoder {


    private static final ST COUNT_CANDIDATE_TEMPLATE = new ST("<id>_cnt_candidate");
    private static final ST COUNT_SORTING_GRID_RESULT_TEMPLATE = new ST("<id>_sorting_grid_result");

    private static final ST MAX_ELEMENT_TUPLE_TEMPLATE = new ST("<id>_element_tuple");

    private static final ST MAX_MAX_ELEMENT_TUPLE_TEMPLATE = new ST("<id>_max_element_tuple");

    private static final ST AGGREGATE_RESULT_TEMPLATE = new ST("<id>_result");

    private static final Set<ThriceTruth> SIGNS_T = asSet(TRUE);

    private static final Set<ThriceTruth> SET_F = asSet(FALSE);

    private static final Set<ThriceTruth> DEFAULT_BODY_SIGNS = asSet(TRUE, MBT);






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
                } else if (operator == ComparisonOperator.LE) {
                    return encodeMinLe(aggregateArguments, resultTerm, id);
                }
                break;
            case MAX:
                if (operator == ComparisonOperator.EQ) {
                    return encodeMaxEq(aggregateArguments, resultTerm, id);
                } else if (operator == ComparisonOperator.LE) {
                    return encodeMaxLe(aggregateArguments, resultTerm, id);
                }
                break;
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
        return null;
    }


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

    private static Map<String, List<HeuristicDirectiveAtom>> encodeMinEq(Term argumentTerm, Term resultTerm, String id) {
        return null;
    }

    private static Map<String, List<HeuristicDirectiveAtom>> encodeMinLe(Term argumentTerm, Term resultTerm, String id) {
        return null;
    }

    private static Map<String, List<HeuristicDirectiveAtom>> encodeMaxEq(Term argumentTerm, Term resultTerm, String id) {
        List<HeuristicDirectiveAtom> positiveAtoms = new ArrayList<>();
        List<HeuristicDirectiveAtom> negativeAtoms = new ArrayList<>();

        String maxElementTupleName = new ST(MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();
        String maxMaxElementTupleName = new ST(MAX_MAX_ELEMENT_TUPLE_TEMPLATE).add("id", id).render();

        positiveAtoms.add(HeuristicDirectiveAtom.body(DEFAULT_BODY_SIGNS, new BasicAtom(Predicate.getInstance(maxElementTupleName, 2, true), argumentTerm, resultTerm)));

        negativeAtoms.add(HeuristicDirectiveAtom.body(SET_F, new BasicAtom(Predicate.getInstance(maxMaxElementTupleName, 2, true), argumentTerm, resultTerm)));

        Map<String, List<HeuristicDirectiveAtom>> map = new HashMap<>();
        map.put("positive", positiveAtoms);
        map.put("negative", negativeAtoms);

        return map;
    }

    private static Map<String, List<HeuristicDirectiveAtom>> encodeMaxLe(Term argumentTerm, Term resultTerm, String id) {
        return null;
    }

    private static Map<String, List<HeuristicDirectiveAtom>> encodeSumEq(Term argumentTerm, Term resultTerm, String id) {
        return null;
    }

    private static Map<String, List<HeuristicDirectiveAtom>> encodeSumLe(Term argumentTerm, Term resultTerm, String id) {
        return null;
    }
}
