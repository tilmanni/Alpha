package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.grounder.Unifier;

import java.util.*;


/**
 * Class to represent a Query and its corresponding "Information", derived from a Heuristic Directive.
 * Also has the vitally important updateResults Method, which writes the results of the query when given
 * to the prolog module into the queryResults attribute as HeuristicDirectiveValues, which can then be used to
 * make  a Choice.
 */
public class QueryInformation {
    private final HeuristicDirectiveAtom headAtom;

    private final String weightVariable;

    private final String levelVariable;

    private final VariableTerm[] variables;

    private final Set<String> occurringPredicates;

    private final String stringQuery;

    private List<HeuristicDirectiveValues> queryResults;

    public QueryInformation(HeuristicDirective heuristicDirective) {
        this.queryResults = new ArrayList<>();

        this.headAtom = heuristicDirective.getHead();

        Set<VariableTerm> headAtomVariables = headAtom.getAtom().getOccurringVariables();
        VariableTerm weightVariableTerm = VariableGenerator.getNextWeightVariable();
        VariableTerm levelVariableTerm = VariableGenerator.getNextLevelVariable();

        this.weightVariable = weightVariableTerm.toString();
        this.levelVariable = levelVariableTerm.toString();

        this.variables = new VariableTerm[headAtomVariables.size() + 2];
        int counter = 0;
        this.variables[counter++] = weightVariableTerm;
        this.variables[counter++] = levelVariableTerm;
        for (VariableTerm variableTerm : headAtomVariables) {
            this.variables[counter++] = variableTerm;
        }

        this.occurringPredicates = new HashSet<>();
        this.occurringPredicates.add(heuristicDirective.getHead().getAtom().getPredicate().toString());
        initializeOccurringPredicates(heuristicDirective.getBody().getBodyAtomsPositive());
        initializeOccurringPredicates(heuristicDirective.getBody().getBodyAtomsNegative());

        this.stringQuery = ASPtoQueryTranslator.translateHeuristicDirective(heuristicDirective, weightVariableTerm, levelVariableTerm);

    }

    private void initializeOccurringPredicates(Collection<HeuristicDirectiveAtom> heuristicDirectiveAtoms) {
        for (HeuristicDirectiveAtom heuristicDirectiveAtom : heuristicDirectiveAtoms) {
            if (heuristicDirectiveAtom.getAtom() instanceof BasicAtom) {
                this.occurringPredicates.add(heuristicDirectiveAtom.getAtom().getPredicate().toString());
            } else if (heuristicDirectiveAtom.getAtom() instanceof AggregateAtom) {
                AggregateAtom aggregateAtom = (AggregateAtom) heuristicDirectiveAtom.getAtom();
                for (Predicate occurringPredicate : aggregateAtom.getAggregateBodyPredicates()) {
                    this.occurringPredicates.add(occurringPredicate.toString());
                }
            }
        }
    }

    /**
     * Method to update the queryResults attribute of this class, poses the actual query to the prolog module,
     * gets all solutions, and stores them as a heuristic directive values.
     *
     * Central function here is to connect the solution returned by the prolog module, where the values are only strings,
     * and converting them into ALPHA's atoms, thus connecting them to the atoms present in ALPHA's atom store.
     *
     * @param atomStore The atom store in which the solution's corresponding atoms should be registered.
     */
    public void updateResults(AtomStore atomStore) {
        if (atomStore == null) {
            return;
        }
        PrologModule prologModule = NaiveGrounder.getPrologModuleInstance();
        List<Map<String, String>> results = prologModule.poseQueryGetResults(stringQuery, variables);
        List<HeuristicDirectiveValues> tempQueryResults = new ArrayList<>();
        for (Map<String, String> solution : results) {
            Unifier unifier = new Unifier();
            for (int i = 2; i < variables.length; i++) {
                String str = solution.get(variables[i].toString());
                if (str.matches("-?\\d+")){
                    unifier.put(variables[i], ConstantTerm.getInstance(Integer.parseInt(str)));
                } else {
                    unifier.put(variables[i], ConstantTerm.getSymbolicInstance(str));
                }
            }
            BasicAtom groundHeadAtom = (BasicAtom) headAtom.substitute(unifier).getAtom();
            int groundHeadId = atomStore.putIfAbsent(groundHeadAtom);
            int weight = Integer.parseInt(solution.get(weightVariable));
            int level = Integer.parseInt(solution.get(levelVariable));
            tempQueryResults.add(new HeuristicDirectiveValues(groundHeadId, (BasicAtom) atomStore.get(groundHeadId), weight, level, true));
        }
        this.queryResults = tempQueryResults;

    }

    public List<HeuristicDirectiveValues> getQueryResults() {
        return this.queryResults;
    }
    public Set<String> getOccurringPredicates() {
        return this.occurringPredicates;
    }

    public HeuristicDirectiveAtom getHeadAtom() {
        return this.headAtom;
    }
}
