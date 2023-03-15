package at.ac.tuwien.kr.alpha.solver.heuristics.domspec;

import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.common.prolog.QueryInformationStorage;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;

import java.util.List;

/**
 * Class to integrate the results of query domain specific heuristics into the alpha heuristic structure.
 * Implements DomainSpecificHeuristicStore, in order to be seamlessly accessed by the DomainSpecific class.
 * Does not really "store" the results, but gets them from the QueryInformation Storage if polled.
 * As such many of the required methods only have stub implementations.
 */
public class QueryDomainSpecificHeuristicsStore implements DomainSpecificHeuristicsStore {

    List<HeuristicDirectiveValues> priorityList = null;
    @Override
    public void addInfo(int heuristicId, HeuristicDirectiveValues values) {
        return;
    }

    @Override
    public HeuristicDirectiveValues poll() {
        this.priorityList = QueryInformationStorage.getQueryResults();
        return priorityList.size() != 0 ? priorityList.get(0) : null;
    }

    @Override
    public void setChoiceManager(ChoiceManager choiceManager) {
        return;
    }

    @Override
    public void growForMaxAtomId(int maxAtomId) {
        return;
    }


    public void updateInformation() {
        this.priorityList = QueryInformationStorage.getQueryResults();
    }


}
