package at.ac.tuwien.kr.alpha.common.heuristics;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.List;

import static at.ac.tuwien.kr.alpha.Util.join;
import static at.ac.tuwien.kr.alpha.Util.oops;

public class HeuristicAggregateAtom extends AggregateAtom {

    public HeuristicAggregateAtom(ComparisonOperator lowerBoundOperator, Term lowerBoundTerm, ComparisonOperator upperBoundOperator, Term upperBoundTerm, AggregateFunctionSymbol aggregatefunction, List<AggregateElement> aggregateElements) {
        super(lowerBoundOperator, lowerBoundTerm, upperBoundOperator, upperBoundTerm, aggregatefunction, aggregateElements);
    }

    public HeuristicAggregateAtom(ComparisonOperator lowerBoundOperator, Term lowerBoundTerm, AggregateFunctionSymbol aggregatefunction, List<AggregateElement> aggregateElements) {
        super(lowerBoundOperator, lowerBoundTerm, aggregatefunction, aggregateElements);
    }

    @Override
    public AggregateAtom substitute(Substitution substitution) {
        return new HeuristicAggregateAtom (
                getLowerBoundOperator(),
                substitution.substituteIfNotNull(getLowerBoundTerm()),
                getUpperBoundOperator(),
                substitution.substituteIfNotNull(getUpperBoundTerm()),
                getAggregatefunction(),
                substitution.substituteAll(getAggregateElements())
        );
    }

    @Override
    public String toString() {
        String lowerBound = getLowerBoundTerm() == null ? "" : (getLowerBoundTerm().toString() + getLowerBoundOperator());
        String upperBound = getUpperBoundTerm() == null ? "" : (getUpperBoundOperator().toString() + getUpperBoundTerm());
        return lowerBound + "#" + getAggregatefunction() + "{{ " + join("", getAggregateElements(), "; ", "") + " }}" + upperBound;
    }


    //TODO Maybe override hashCode() as well
}
