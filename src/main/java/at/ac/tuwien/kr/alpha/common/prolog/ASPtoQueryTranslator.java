package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.WeightAtLevel;
import at.ac.tuwien.kr.alpha.common.atoms.*;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveBody;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.join;


/**
 * Utility Class to translate Heuristic directives to their respective Prolog Queries.
 * Only has one method with public access, translateHeuristicDirective.
 */
public class ASPtoQueryTranslator {

    /**
     * Public option for multiset support, is true when uqhms flag is set.
     */
    public static boolean multiSetEnabled;


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
        String translatedHeuristicDirectiveHeadAtom = translateHeuristicDirectiveAtom(heuristicDirectiveHeadAtom);
        return "\\+ " + translatedHeuristicDirectiveHeadAtom + ", " + "\\+ " + addNegativePredicate(translatedHeuristicDirectiveHeadAtom);
    }


    private static String translateHeuristicDirectiveBody(HeuristicDirectiveBody heuristicDirectiveBodyToTranslate) {
        StringJoiner stringJoiner = new StringJoiner(", ");
        Collection<HeuristicDirectiveAtom> positiveBodySorted = sortHeuristicDirectiveBody(heuristicDirectiveBodyToTranslate.getBodyAtomsPositive());
        for (HeuristicDirectiveAtom positiveHeuristicDirectiveAtom : positiveBodySorted) {
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
        return addPrologPrefix(basicAtomToTranslate.toString());
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
        AggregateAtom.AggregateFunctionSymbol aggregateFunctionSymbol = aggregateAtomToTranslate.getAggregatefunction();

        StringBuilder sb;
        List<String> aggregateElementsTranslated = new ArrayList<>();
        List<String> aggregateElementResultVariables = new ArrayList<>();
        for (AggregateAtom.AggregateElement element : aggregateAtomToTranslate.getAggregateElements()) {
            sb = new StringBuilder();
            String aggregateElementResultVariable = VariableTerm.getAnonymousInstance().toString();
            aggregateElementResultVariables.add(aggregateElementResultVariable);
            sb.append("aggregate_all(");
            if (aggregateFunctionSymbol == AggregateAtom.AggregateFunctionSymbol.COUNT) {
                sb.append("count, ");
            } else {
                sb.append(aggregateFunctionSymbol.toString().toLowerCase()).append("(");
                sb.append(element.getElementTerms().get(0).toString());
                sb.append("), ");
            }
            sb.append(translateAggregateElement(element));
            sb.append(", ");
            sb.append(aggregateElementResultVariable);
            sb.append(")");
            aggregateElementsTranslated.add(sb.toString());
        }
        sb = new StringBuilder();
        sb.append(join(aggregateElementsTranslated, ", "));
        sb.append(", ");
        if (aggregateFunctionSymbol == AggregateAtom.AggregateFunctionSymbol.COUNT || aggregateFunctionSymbol == AggregateAtom.AggregateFunctionSymbol.SUM) {
            sb.append(aggregateOutputVariable);
            sb.append(" is ");
            sb.append(join(aggregateElementResultVariables, " + "));
        }
        else {
            sb.append(aggregateFunctionSymbol == AggregateAtom.AggregateFunctionSymbol.MAX ? "max_member(" : "min_member(");
            sb.append(aggregateOutputVariable);
            sb.append(", [");
            sb.append(join(aggregateElementResultVariables, ", "));
            sb.append("])");
        }
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
            upperBoundOperator = getOpposite(upperBoundOperator);
            sb.append(translateComparisonOperator(upperBoundOperator));
            sb.append(" ");
            sb.append(aggregateOutputVariable);
        }
        return sb.toString();
    }

    private static String translateAggregateElement(AggregateAtom.AggregateElement aggregateElementToTranslate) {
        List<String> aggregateElementTermsString = new ArrayList<>();
        List<String> aggregateElementLiteralsTranslated = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<Term> aggregateElementTerms = aggregateElementToTranslate.getElementTerms();
        if (!multiSetEnabled) {
            sb.append("(");
            for (Term aggregateTerm : aggregateElementTerms) {      //exclude first one for multiset
                aggregateElementTermsString.add(aggregateTerm.toString());
            }
            sb.append(join(aggregateElementTermsString, ","));
            sb.append("), ");
        }
        sb.append("(");
        for (Literal aggregateLiteral : aggregateElementToTranslate.getElementLiterals()) {
            aggregateElementLiteralsTranslated.add(translateLiteral(aggregateLiteral));
        }
        sb.append(join(aggregateElementLiteralsTranslated, ","));
        sb.append(")");
        return sb.toString();
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
            atomTranslated = "\\+ " + atomTranslated;
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

    private static List<HeuristicDirectiveAtom> sortHeuristicDirectiveBody(Collection<HeuristicDirectiveAtom> heuristicDirectiveBodyToSort) {
        List<HeuristicDirectiveAtom> orderedBody = new ArrayList<>();
        LinkedList<HeuristicDirectiveAtom> queue = new LinkedList<>();
        Set<VariableTerm> globalVariables = new HashSet<>();

        Set<VariableTerm> positivelyOccurringGlobalVariables = new HashSet<>();



        for (HeuristicDirectiveAtom heuristicDirectiveAtom : heuristicDirectiveBodyToSort) {
            if (heuristicDirectiveAtom.getAtom() instanceof BasicAtom) {
                positivelyOccurringGlobalVariables.addAll(heuristicDirectiveAtom.getAtom().getOccurringVariables());
                orderedBody.add(heuristicDirectiveAtom);
            } else {
                queue.add(heuristicDirectiveAtom);
            }
            globalVariables.addAll(getGlobalVariables(heuristicDirectiveAtom));
        }

        while (!queue.isEmpty()) {
            HeuristicDirectiveAtom heuristicDirectiveAtom = queue.poll();
            IsCoveredPair isCoveredPair = isCovered(heuristicDirectiveAtom, positivelyOccurringGlobalVariables, globalVariables);
            if (isCoveredPair.isCovered) {
                if (isCoveredPair.assignedVariable != null) {
                    positivelyOccurringGlobalVariables.add(isCoveredPair.assignedVariable);
                }
                orderedBody.add(heuristicDirectiveAtom);
            } else {
                queue.add(heuristicDirectiveAtom);
            }
        }
        return orderedBody;
    }

    private static Set<VariableTerm> getGlobalVariables(HeuristicDirectiveAtom heuristicDirectiveAtom) {
        if (heuristicDirectiveAtom.getAtom() instanceof AggregateAtom) {
            Set<VariableTerm> globalVariables = new HashSet<>();
            AggregateAtom aggregateAtom = (AggregateAtom) heuristicDirectiveAtom.getAtom();
            if (aggregateAtom.getLowerBoundTerm() != null) {
                globalVariables.addAll(aggregateAtom.getLowerBoundTerm().getOccurringVariables());
            }
            if (aggregateAtom.getUpperBoundTerm() != null) {
                globalVariables.addAll(aggregateAtom.getUpperBoundTerm().getOccurringVariables());
            }
            return globalVariables;
        }
        return heuristicDirectiveAtom.getAtom().getOccurringVariables();
    }

    private static IsCoveredPair isCovered(HeuristicDirectiveAtom heuristicDirectiveAtom, Set<VariableTerm> positivelyOccurringVariables, Set<VariableTerm> globalVariables) {
        Atom atom = heuristicDirectiveAtom.getAtom();
        if (atom instanceof ComparisonAtom) {
            return isCoveredCompAtom((ComparisonAtom) atom, positivelyOccurringVariables);
        } else if (atom instanceof AggregateAtom) {
            return isCoveredAggAtom((AggregateAtom) atom, positivelyOccurringVariables, globalVariables);
        }
        return new IsCoveredPair(false, null);
    }
    private static IsCoveredPair isCoveredCompAtom(ComparisonAtom comparisonAtom, Set<VariableTerm> positivelyOccurringVariables) {
        VariableTerm assignedVariable = null;
        for (VariableTerm variableTerm : comparisonAtom.getOccurringVariables()) {
            if (!positivelyOccurringVariables.contains(variableTerm)) {
                if (assignedVariable == null && comparisonAtom.getOperator().equals(ComparisonOperator.EQ)) {
                    assignedVariable = variableTerm;
                } else {
                    return new IsCoveredPair(false, null);
                }
            }
        }
        return new IsCoveredPair(true, assignedVariable);
    }

    private static IsCoveredPair isCoveredAggAtom(AggregateAtom aggregateAtom, Set<VariableTerm> positivelyOccurringVariables, Set<VariableTerm> globalVariables) {
        VariableTerm assignedVariable = null;
        for (VariableTerm variableTerm : aggregateAtom.getAggregateVariables()) {
            if (globalVariables.contains(variableTerm) && !positivelyOccurringVariables.contains(variableTerm)) {
                return new IsCoveredPair(false, null);
            }
        }
        if (aggregateAtom.getUpperBoundTerm() != null) {
            for (VariableTerm variableTerm : aggregateAtom.getUpperBoundTerm().getOccurringVariables()) {
                if (!positivelyOccurringVariables.contains(variableTerm)) {
                    if (assignedVariable == null && aggregateAtom.getUpperBoundOperator().equals(ComparisonOperator.EQ)) {
                        assignedVariable = variableTerm;
                    } else {
                        return new IsCoveredPair(false, null);
                    }
                }
            }
        }
        if (aggregateAtom.getLowerBoundTerm() != null) {
            for (VariableTerm variableTerm : aggregateAtom.getLowerBoundTerm().getOccurringVariables()) {
                if (!positivelyOccurringVariables.contains(variableTerm)) {
                    if (assignedVariable == null && aggregateAtom.getLowerBoundOperator().equals(ComparisonOperator.EQ)) {
                        assignedVariable = variableTerm;
                    } else {
                        return new IsCoveredPair(false, null);
                    }
                }
            }
        }
        return new IsCoveredPair(true, assignedVariable);

    }


    private static class IsCoveredPair {
        final boolean isCovered;
        final VariableTerm assignedVariable;
        public IsCoveredPair(boolean isCovered, VariableTerm assignedVariable) {
            this.isCovered = isCovered;
            this.assignedVariable = assignedVariable;
        }
    }

    private static ComparisonOperator getOpposite(ComparisonOperator comparisonOperator) {
        switch (comparisonOperator) {
            case EQ:
                return ComparisonOperator.NE;
            case NE:
                return ComparisonOperator.EQ;
            case LT:
                return ComparisonOperator.GT;
            case GT:
                return ComparisonOperator.LT;
            case LE:
                return ComparisonOperator.GE;
            case GE:
                return ComparisonOperator.LE;

        }
        return null;
    }

    private static String addPrologPrefix(String string) {
        return PrologModule.PROLOG_PREFIX + string;
    }

    private static String addNegativePredicate(String string) {return "f(" + string + ")";}
}
