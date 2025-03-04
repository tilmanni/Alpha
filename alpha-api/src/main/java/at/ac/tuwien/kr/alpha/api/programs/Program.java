package at.ac.tuwien.kr.alpha.api.programs;

import java.util.List;

import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.rules.Rule;
import at.ac.tuwien.kr.alpha.api.programs.rules.heads.Head;

/**
 * An ASP program as accepted by Alpha.
 * 
 * @param <R> the type of rule this program may consist of (Normal-, Choice-, Disjunctive Rules)
 *            Copyright (c) 2021, the Alpha Team.
 */
public interface Program<R extends Rule<? extends Head>> {

	/**
	 * The facts, i.e. rule heads without body, in the program.
	 */
	List<Atom> getFacts();

	/**
	 * The {@link InlineDirectives} (i.e. meta-statements for the solver) that are part of this program.
	 */
	InlineDirectives getInlineDirectives();

	/**
	 * The rules in the program.
	 */
	List<R> getRules();

}
