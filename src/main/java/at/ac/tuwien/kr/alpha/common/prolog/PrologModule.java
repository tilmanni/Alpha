package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.program.Facts;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;

import java.util.List;
import java.util.Map;

public interface PrologModule {


    void addFacts(Facts factsFromProgram);

    void setAtomStore(AtomStore atomStore);

    void addAtom(Integer atomToAdd);

    void removeAtom(Integer atomToRemove);

    boolean poseQuery(String query);

    String poseQueryGetResult(String query, String result);

    List<Map<String, String>> poseQueryGetResults(String query, VariableTerm[] results);

    long getAddTime();

    long getRemoveTime();

    long getQTime();
}
