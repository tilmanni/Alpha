package at.ac.tuwien.kr.alpha.grounder.atoms;

import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.WeightAtLevel;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;

import java.util.List;

import static at.ac.tuwien.kr.alpha.common.AtomToFunctionTermConverter.toFunctionTerm;

public class DynamicHeuristicAtom extends HeuristicAtom {

    private static final String FUNCTION_POSITIVE_CONDITION = "condpos";
    private static final String FUNCTION_NEGATIVE_CONDITION = "condneg";

    public DynamicHeuristicAtom(WeightAtLevel weightAtLevel, ThriceTruth headSign, FunctionTerm headAtom, FunctionTerm positiveCondition, FunctionTerm negativeCondition) {
        super(weightAtLevel, headSign, headAtom, positiveCondition, negativeCondition);
    }

    @Override
    public HeuristicAtom withTerms(List<Term> terms) {
        if (terms.size() != 6) {
            throw new IllegalArgumentException("Length of terms list does not fit " + this.getClass().getSimpleName() + ": " + terms);
        }
        final WeightAtLevel weightAtLevel = new WeightAtLevel(terms.get(0), terms.get(1));
        final ThriceTruth headSign = ((ConstantTerm<Boolean>) terms.get(2)).getObject() ? ThriceTruth.TRUE : ThriceTruth.FALSE;
        final FunctionTerm headAtom = (FunctionTerm) terms.get(3);
        final FunctionTerm positiveCondition = (FunctionTerm) terms.get(4);
        final FunctionTerm negativeCondition = (FunctionTerm) terms.get(5);
        return new DynamicHeuristicAtom(weightAtLevel, headSign, headAtom, positiveCondition, negativeCondition);
    }

    @Override
    public HeuristicAtom substitute(Substitution substitution) {
        return new DynamicHeuristicAtom(
                getWeightAtLevel().substitute(substitution),
                getHeadSign(),
                getHeadAtom().substitute(substitution),
                getPositiveCondition().substitute(substitution),
                getNegativeCondition().substitute(substitution)
        );
    }


    public static HeuristicAtom fromHeuristicDirective(HeuristicDirective heuristicDirective) {
        return new DynamicHeuristicAtom(
                heuristicDirective.getWeightAtLevel(),
                heuristicDirective.getHead().getSigns().iterator().next(),
                toFunctionTerm((BasicAtom) heuristicDirective.getHead().getAtom()),
                conditionToFunctionTerm(heuristicDirective.getBody().getBodyAtomsPositive(), FUNCTION_POSITIVE_CONDITION),
                conditionToFunctionTerm(heuristicDirective.getBody().getBodyAtomsNegative(), FUNCTION_NEGATIVE_CONDITION)
        );
    }
}
