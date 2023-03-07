package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.WeightAtLevel;
import at.ac.tuwien.kr.alpha.common.atoms.*;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveBody;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static org.apache.commons.lang3.StringUtils.join;


/**
 * Utility Class to translate Heuristic directives to their respective Prolog Queries.
 * Only has one method with public access, translateHeuristicDirective.
 */
public class ASPtoQueryTranslator {


    /**
     * Pivotal Method for this class, translating a HeuristicDirective to its respective Prolog Query, by splitting it
     * into its components, and translating the components through their respective methods.
     * Weight and Level VariableTerms must be given as parameters as well, as the weight and level calculation is included
     * in the query together with heuristic directive body.The value of these variables must also be known to the calling
     * method, in order to extract solutions later.
     *
     * @param heuristicDirectiveToTranslate the heuristic directive to translate.
     * @param weight the variable term to represent the weight of the heuristic directive in the query
     * @param level the variable term to represent the level of the heuristic directive in the query
     * @return A String representing the heuristic directive as a prolog query.
     */
    public static String translateHeuristicDirective(HeuristicDirective heuristicDirectiveToTranslate, VariableTerm weight, VariableTerm level) {
        return translateHeuristicDirectiveBody(heuristicDirectiveToTranslate.getBody()) + ", "
                + translateWeightAtLevel(heuristicDirectiveToTranslate.getWeightAtLevel(), weight, level) + ", "
                + translateHeuristicDirectiveHeadAtom(heuristicDirectiveToTranslate.getHead());

    }

    /**
     * Method to translate the heuristic directive's head atom.
     * Serves to include the head atom in negated form in the final query, in order to not derive solutions
     * which already exist.
     *
     * @param heuristicDirectiveHeadAtom the heuristic directive head atom to translate.
     * @return A String containing the negation of the head atom.
     */
    private static String translateHeuristicDirectiveHeadAtom(HeuristicDirectiveAtom heuristicDirectiveHeadAtom) {
        return "\\+ " + translateHeuristicDirectiveAtom(heuristicDirectiveHeadAtom);
    }


    private static String translateHeuristicDirectiveBody(HeuristicDirectiveBody heuristicDirectiveBodyToTranslate) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (HeuristicDirectiveAtom positiveHeuristicDirectiveAtom : heuristicDirectiveBodyToTranslate.getBodyAtomsPositive()) {
            stringJoiner.add(translateHeuristicDirectiveAtom(positiveHeuristicDirectiveAtom));
        }
        for (HeuristicDirectiveAtom negativeHeuristicDirectiveAtom : heuristicDirectiveBodyToTranslate.getBodyAtomsNegative()) {
            stringJoiner.add("\\+ " + translateHeuristicDirectiveAtom(negativeHeuristicDirectiveAtom));
        }
        return stringJoiner.toString();
    }

    private static String translateHeuristicDirectiveAtom(HeuristicDirectiveAtom heuristicDirectiveAtomToTranslate) {
        Atom atom = heuristicDirectiveAtomToTranslate.getAtom();
        if (atom instanceof BasicAtom) {
            return translateBasicAtom((BasicAtom) atom);
        } else if (atom instanceof ComparisonAtom) {
            return translateComparisonAtom((ComparisonAtom) atom);
        } else if (atom instanceof AggregateAtom) {
            return translateAggregateAtom((AggregateAtom) atom);
        }
        throw new IllegalArgumentException("Unsupported Atom for Prolog Heuristics " + heuristicDirectiveAtomToTranslate);
    }

    private static String translateBasicAtom(BasicAtom basicAtomToTranslate) {
        return basicAtomToTranslate.toString();
    }
    private static String translateComparisonAtom(ComparisonAtom comparisonAtomToTranslate) {
        ComparisonOperator comparisonOperator = comparisonAtomToTranslate.getOperator();
        if (comparisonOperator.toString().equals("=") || comparisonOperator.toString().equals("<=")) {
            return comparisonAtomToTranslate.toString().replace(comparisonOperator.toString(), translateComparisonOperator(comparisonOperator));
        } else if (comparisonOperator.toString().equals("!=")) {
            return "\\+ " + comparisonAtomToTranslate.toString().replace("!=", "is");
        }
        return comparisonAtomToTranslate.toString();
    }

    private static String translateComparisonOperator(ComparisonOperator comparisonOperatorToTranslate) {
        if (comparisonOperatorToTranslate.toString().equals("=") || comparisonOperatorToTranslate.toString().equals("!=")) {
            return "is";
        } else if (comparisonOperatorToTranslate.toString().equals("<=")) {
            return "=<";
        }
        return comparisonOperatorToTranslate.toString();
    }


    private static String translateAggregateAtom(AggregateAtom aggregateAtomToTranslate) {
        String aggregateOutputVariable = VariableTerm.getAnonymousInstance().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("aggregate_all(");
        if (aggregateAtomToTranslate.getAggregatefunction() == AggregateAtom.AggregateFunctionSymbol.COUNT) {
            sb.append("count, ");
        } else {
            sb.append(aggregateAtomToTranslate.getAggregatefunction().toString().toLowerCase()).append("(");
            sb.append(aggregateAtomToTranslate.getAggregateElements().get(0).getElementTerms().get(0).toString());
            sb.append("), ");
        }
        /*
            Assumes all aggregate Elements use the same term before the colon, like so:
                #sum{X : a(X); X : b(X)}
            instead of
                #sum{X : a(X); Y : b(Y)}
            for simplicity. If the latter case is used, only the first aggregate element (or any using X in this example)
            will be evaluated.
            Does also not evaluate #sum{X, Y : a(X), b(Y)} properly, just evaluates for the first variable in front of
            colon.
         */
        //TODO Fix this trough normalization.
        sb.append("(");
        List<String> aggregateElementsTranslated = new ArrayList<>();
        for (AggregateAtom.AggregateElement element : aggregateAtomToTranslate.getAggregateElements()) {
            aggregateElementsTranslated.add(translateAggregateElement(element));
        }
        sb.append(join(aggregateElementsTranslated, ";"));
        sb.append("), ");
        sb.append(aggregateOutputVariable);
        sb.append(")");
        ComparisonOperator lowerBoundOperator = aggregateAtomToTranslate.getLowerBoundOperator();
        ComparisonOperator upperBoundOperator = aggregateAtomToTranslate.getUpperBoundOperator();
        if (lowerBoundOperator != null) {
            sb.append(", ");
            if (lowerBoundOperator.toString().equals("!=")) {
                sb.append("\\+ ");
            }
            sb.append(aggregateAtomToTranslate.getLowerBoundTerm());
            sb.append(" ");
            sb.append(translateComparisonOperator(lowerBoundOperator));
            sb.append(" ");
            sb.append(aggregateOutputVariable);
        }
        if (upperBoundOperator != null) {
            sb.append(", ");
            if (upperBoundOperator.toString().equals("!=")) {
                sb.append("\\+ ");
            }
            sb.append(aggregateAtomToTranslate.getUpperBoundTerm());
            sb.append(" ");
            if (upperBoundOperator.toString().equals("!=") || upperBoundOperator.toString().equals("=")) {
                upperBoundOperator = upperBoundOperator.getNegation();
            }
            sb.append(translateComparisonOperator(upperBoundOperator));
            sb.append(" ");
            sb.append(aggregateOutputVariable);
        }
        return sb.toString();
    }

    private static String translateAggregateElement(AggregateAtom.AggregateElement aggregateElementToTranslate) {
        List<String> aggregateElementLiteralsTranslated = new ArrayList<>();
        for (Literal aggregateLiteral : aggregateElementToTranslate.getElementLiterals()) {
            aggregateElementLiteralsTranslated.add(translateLiteral(aggregateLiteral));
        }
        return join(aggregateElementLiteralsTranslated, ",");
    }

    private static String translateLiteral(Literal literal) {
        Atom atom = literal.getAtom();
        String atomTranslated;
        if (atom instanceof ComparisonAtom) {
            atomTranslated = translateComparisonAtom((ComparisonAtom) atom);
        } else if (atom instanceof BasicAtom) {
            atomTranslated = translateBasicAtom((BasicAtom) atom);
        } else {
            throw new IllegalArgumentException("Unsupported Literal for Prolog Heuristics " + literal);
        }
        if (literal.isNegated()) {
            atomTranslated = "\\+ "+ atomTranslated;
        }
        return atomTranslated;
    }


    private static String translateWeightAtLevel(WeightAtLevel weightAtLevelToTranslate, VariableTerm weight, VariableTerm level) {
        return weight.toString() +
                " is " +
                weightAtLevelToTranslate.getWeight().toString() +
                ", " +
                level.toString() +
                " is " +
                weightAtLevelToTranslate.getLevel().toString();
    }

    /*
     External atoms not supported.
     */

}
