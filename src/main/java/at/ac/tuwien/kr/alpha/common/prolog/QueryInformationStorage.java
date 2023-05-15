package at.ac.tuwien.kr.alpha.common.prolog;


import at.ac.tuwien.kr.alpha.common.AtomStore;

import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveValues;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;

import java.util.*;


/**
 * Class with only static methods (for easy global access, not really OO) to store all queries and get their result.
 * Could probably be made non-static, if OO becomes requirements.
 *
 *
 */
public class QueryInformationStorage {

    /*
     * Sadly, the plural of "information" is "information".
     */
    private static final List<QueryInformation> QUERY_INFORMATION = new ArrayList<>();

    private static AtomStore atomStore;

    private static final Set<String> OCCURRING_PREDICATES = new HashSet<>();

    private static boolean dynamicPredicatesInitialized;





    public static List<QueryInformation> getQueryInformation() {
        return QUERY_INFORMATION;
    }

    public static void addQueryInformation(QueryInformation queryInformationToAdd) {
        QUERY_INFORMATION.add(queryInformationToAdd);
        OCCURRING_PREDICATES.addAll(queryInformationToAdd.getOccurringPredicates());
    }

    public static void updateInformation() {
        if (atomStore == null) {
            return;
        }
        if (!dynamicPredicatesInitialized) {
            initializeDynamicPredicates();
        }
        for (QueryInformation queryInformationToUpdate : QUERY_INFORMATION) {
            queryInformationToUpdate.updateResults(atomStore);
        }
    }

    /*
        Could be solved more efficiently if not using PriorityQueue and returning a Sorted Set.
        The workaround with the results ArrayList is (currently) necessary, as the PriorityQueue must have a fixed capacity,
        which we might not know beforehand.
    */
    public static List<HeuristicDirectiveValues> getQueryResults() {
        if (atomStore == null) {
            return null;
        }
        ArrayList<HeuristicDirectiveValues> results = new ArrayList<>();
        for (QueryInformation queryInformationToGetResultsFrom : QUERY_INFORMATION) {
            queryInformationToGetResultsFrom.updateResults(atomStore);
            results.addAll(queryInformationToGetResultsFrom.getQueryResults());
        }
        results.sort(new HeuristicDirectiveValues.PriorityComparator());
        Collections.reverse(results);
        return results;
    }

    public static HeuristicDirectiveValues getTopQueryResult() {
        if (atomStore == null) {
            return null;
        }
        PriorityQueue<HeuristicDirectiveValues> results = new PriorityQueue<>(2000, new HeuristicDirectiveValues.PriorityComparator()); //TODO Set more natural capacity limit, not just magic number
        for (QueryInformation queryInformationToGetResultsFrom : QUERY_INFORMATION) {
            queryInformationToGetResultsFrom.updateResults(atomStore);
            results.addAll(queryInformationToGetResultsFrom.getQueryResults());
        }
        return results.poll();
    }

    public static void setAtomStore(AtomStore atomStore) {
        QueryInformationStorage.atomStore = atomStore;
    }

    private static void initializeDynamicPredicates() {
        PrologModule prologModule = NaiveGrounder.getPrologModuleInstance();
        for (String predicate : OCCURRING_PREDICATES) {
            prologModule.poseQuery("dynamic " + predicate);
        }
        dynamicPredicatesInitialized = true;
    }
}
