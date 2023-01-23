package at.ac.tuwien.kr.alpha.common.prolog;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.program.Facts;

public interface PrologModule {


    void addFacts(Facts factsFromProgram);

    void setAtomStore(AtomStore atomStore);

    void addAtom(Integer atomToAdd);

    void removeAtom(Integer atomToRemove);

    boolean poseQuery(String query);

    String poseQueryGetResult(String query, String result);

    long getAddTime();

    long getRemoveTime();

    long getQTime();
}
