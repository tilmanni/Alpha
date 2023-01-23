package at.ac.tuwien.kr.alpha.common.prolog;


import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.Facts;
import at.ac.tuwien.kr.alpha.grounder.Instance;
import org.jpl7.*;

import java.lang.Integer;
import java.util.Map;
import java.util.Set;

//TODO Class is  (for convenience) should be changed to be instance later.

/**
 * Simple Utility class that provides interface for making queries, adding and removing facts from prolog.
 * The class uses swipl (SWI Prolog), with the jpl7 package.
 * In order for this to work, swipl has to be installed on your device, and environment variables have to be set, like so:
 * CLASSPATH=</usr/local/swipl-git/lib/swipl/lib/jpl.jar>;
 * SWI_HOME_DIR=/usr/local/swipl-git/lib/swipl/;
 * LD_LIBRARY_PATH=/usr/local/swipl-git/lib/swipl/lib/x86_64-linux/;
 * LD_PRELOAD=/usr/local/swipl-git/lib/swipl/lib/x86_64-linux/libswipl.so
 * (substitute these paths with your own installation)
 *  This must also be added amongst dependencies in build.gradle  (once again substituted with your own path).
 * 	implementation files('/usr/local/swipl-git/lib/swipl/lib/jpl.jar')
 *
 */
public class SWIPLPrologModule implements PrologModule {
    private  AtomStore atomStore;

    public  long removeTime = 0;
    public  long addTime = 0;

    //queryTime as name lead to inexplicable problems
    public  long qTime = 0;

    public SWIPLPrologModule() {
    }


    public  void addFacts(Facts factsFromProgram) {
        long startTime = System.nanoTime();
        for (Map.Entry<Predicate, ? extends Set<Instance>> facts : factsFromProgram.entrySet()) {
            Predicate factPredicate = facts.getKey();
            poseQuery("dynamic " + factPredicate);
            for (Instance factInstance : facts.getValue()) {
                addAtom(new BasicAtom(factPredicate, factInstance.terms));
            }
        }
        addTime += System.nanoTime() - startTime;
    }

    public  void setAtomStore(AtomStore atomStore) {
        this.atomStore = atomStore;
    }



    public  void addAtom(Integer atomToAdd) {
        addAtom(atomStore.get(atomToAdd));
    }

    public  void addAtom(at.ac.tuwien.kr.alpha.common.atoms.Atom atomToAdd) {
        long startTime = System.nanoTime();
        if (atomToAdd.getPredicate().isInternal()) {
            return;
        }
        try {
            Query q =
                    new Query(
                            "asserta",
                            new Term[]{Term.textToTerm(atomToAdd.toString())}
                    );
            if (!q.hasSolution()) {
                System.out.println("Failed assertion at " + atomToAdd);
            }
        } catch (PrologException prologException) {
            System.out.println("Prolog Exception while asserting " + atomToAdd);
            System.out.println(prologException.getMessage());
        }
        addTime += System.nanoTime() - startTime;
    }


    public  void addAtoms(Set<at.ac.tuwien.kr.alpha.common.atoms.Atom> atomsToAdd) {

        for (at.ac.tuwien.kr.alpha.common.atoms.Atom atom : atomsToAdd) {
            addAtom(atom);
        }
    }

    public  void removeAtom(Integer atomToRemove) {
        this.removeAtom(atomStore.get(atomToRemove));
    }

    public  void removeAtom(at.ac.tuwien.kr.alpha.common.atoms.Atom atomToRemove) {
        long startTime = System.nanoTime();
        if (atomToRemove.getPredicate().isInternal()) {
            return;
        }
        try {
            Query q =
                    new Query(
                            "retract",
                            new Term[]{Term.textToTerm(atomToRemove.toString())}
                    );
            if (!q.hasSolution()) {
                System.out.println("Failed retracting at " + atomToRemove);
            }
        } catch (PrologException prologException) {
            System.out.println("Prolog Exception while removing " + atomToRemove);
        }
        removeTime += System.nanoTime() - startTime;
    }


    public  void removeAtoms(Set<at.ac.tuwien.kr.alpha.common.atoms.Atom> atomsToRemove) {
        for (at.ac.tuwien.kr.alpha.common.atoms.Atom atom : atomsToRemove) {
            removeAtom(atom);
        }
    }




    //PARAMETERS SUBJECT TO CHANGE
    public  boolean poseQuery(String query) {
        long startTime = System.nanoTime();
        Term term = Term.textToTerm(query);
        try {
            Query q = new Query(term);
            qTime += System.nanoTime() - startTime;
            return q.hasSolution();
        } catch (PrologException prologException) {
            return false;
        }

    }

    public  String poseQueryGetResult(String query, String result) {
        long startTime = System.nanoTime();
        Term term = Term.textToTerm(query);
        Query q = new Query(term);
        if (q.hasNext()) {
            Map<String, Term> binding = q.next();
            qTime += System.nanoTime() - startTime;
            return binding.get(result).toString();
        }
        return null;
    }

    @Override
    public long getAddTime() {
        return this.addTime;
    }

    @Override
    public long getRemoveTime() {
        return this.removeTime;
    }

    @Override
    public long getQTime() {
        return this.qTime;
    }

}
