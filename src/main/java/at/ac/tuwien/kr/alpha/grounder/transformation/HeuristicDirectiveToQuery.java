package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.Directive;
import at.ac.tuwien.kr.alpha.common.HeuristicDirective;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.prolog.QueryInformation;
import at.ac.tuwien.kr.alpha.common.prolog.QueryInformationStorage;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;

import java.util.Collection;


/**
 * Class to represent a Program Transformation, in which heuristic directives are removed, and the corresponding
 * Prolog queries generated.
 *
 * Extends ProgramTransformation for this purpose.
 */
public class HeuristicDirectiveToQuery extends ProgramTransformation<InputProgram, InputProgram> {

    private final boolean respectDomspecHeuristics;
    private final boolean useQueryHeuristics;

    public HeuristicDirectiveToQuery(boolean respectDomspecHeuristics, boolean useQueryHeuristics) {
        this.respectDomspecHeuristics = respectDomspecHeuristics;
        this.useQueryHeuristics = useQueryHeuristics;
    }

    @Override
    public InputProgram apply(InputProgram inputProgram) {
        Collection<Directive> heuristicDirectives = inputProgram.getInlineDirectives().getDirectives(InlineDirectives.DIRECTIVE.heuristic);
        if (heuristicDirectives == null) {
            return inputProgram;
        }

        final InputProgram.Builder prgBuilder = InputProgram.builder().addFacts(inputProgram.getFacts()).addRules(inputProgram.getRules());
        final InlineDirectives copiedDirectives = new InlineDirectives();
        copiedDirectives.accumulate(inputProgram.getInlineDirectives());
        if (respectDomspecHeuristics) {
            if (useQueryHeuristics) {
                for (Directive directive : copiedDirectives.getDirectives(InlineDirectives.DIRECTIVE.heuristic)) {
                    QueryInformationStorage.addQueryInformation(new QueryInformation((HeuristicDirective) directive));
                }
                copiedDirectives.getDirectives(InlineDirectives.DIRECTIVE.heuristic).clear();
            }
        } else {
            copiedDirectives.getDirectives(InlineDirectives.DIRECTIVE.heuristic).clear();
        }
        prgBuilder.addInlineDirectives(copiedDirectives);

        return prgBuilder.build();
    }
}
