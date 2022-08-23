package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates;

import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicAggregateAtom;
import at.ac.tuwien.kr.alpha.common.heuristics.HeuristicDirectiveAtom;
import at.ac.tuwien.kr.alpha.common.rule.head.Head;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.atoms.HeuristicAtom;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.grounder.transformation.ProgramTransformation;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewritingContext.AggregateInfo;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders.AbstractAggregateEncoder;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders.CountEncoder;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders.MinMaxEncoder;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders.SumEncoder;

/**
 * Rewrites {@link AggregateLiteral}s in programs to semantically equivalent, aggregate-free sub-programs.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public class AggregateRewriting extends ProgramTransformation<InputProgram, InputProgram> {

	private final AbstractAggregateEncoder countEqualsEncoder;
	private final AbstractAggregateEncoder countLessOrEqualEncoder;
	private final AbstractAggregateEncoder sumEqualsEncoder;
	private final AbstractAggregateEncoder sumLessOrEqualEncoder;
	private final AbstractAggregateEncoder minEncoder;
	private final AbstractAggregateEncoder maxEncoder;

	/**
	 * Creates a new {@link AggregateRewriting} transformation.
	 * 
	 * @param useSortingCircuit       if true, literals of form "X <= #count{...}" will be rewritten using a sorting
	 *                                grid-based
	 *                                encoding.
	 * @param supportNegativeIntegers if true, use ASP encodings for "#sum{...}" literals that support summation over all
	 *                                (including negative) integers. Note that these encodings are less performant than
	 *                                their simpler counterparts that only support positive integers (eused when flag set to false)
	 */
	public AggregateRewriting(boolean useSortingCircuit, boolean supportNegativeIntegers) {
		this.countLessOrEqualEncoder = CountEncoder.buildCountLessOrEqualEncoder(useSortingCircuit);
		this.sumLessOrEqualEncoder = SumEncoder.buildSumLessOrEqualEncoder(supportNegativeIntegers);
		this.sumEqualsEncoder = SumEncoder.buildSumEqualsEncoder(supportNegativeIntegers);
		this.countEqualsEncoder = CountEncoder.buildCountEqualsEncoder();
		this.minEncoder = new MinMaxEncoder(AggregateFunctionSymbol.MIN);
		this.maxEncoder = new MinMaxEncoder(AggregateFunctionSymbol.MAX);
	}

	/**
	 * Rewrites all {@link AggregateLiteral}s in the given program.
	 * The transformation workflow is split into a preprocessing- and an encoding phase.
	 * During preprocessing, all aggregate literals with two comparison operators are split into two aggregate literals with
	 * only a "lower bound" term and operator. After literal splitting, operators are normalized, i.e. all "count" and "sum"
	 * literals are rewritten to use either "<=" or "=" as comparison operator.
	 * 
	 * Rules containing {@link AggregateLiteral}s are registered in an {@link AggregateRewritingContext} during
	 * preprocessing. After preprocessing, for each rule in the context, aggregate literals in the rule body are substituted
	 * with normal literals of form "$id$_aggregate_result(...)". For each literal substituted this way, a set of rules
	 * deriving the result literal is added that is semantically equivalent to the replaced aggregate literal.
	 */
	@Override
	public InputProgram apply(InputProgram inputProgram) {
		AggregateRewritingContext ctx = new AggregateRewritingContext();
		List<BasicRule> outputRules = new ArrayList<>();
		for (BasicRule inputRule : inputProgram.getRules()) {
			// Split literals with two operators.
			for (BasicRule splitRule : AggregateLiteralSplitting.split(inputRule)) {
				// Normalize operators on aggregate literals after splitting.
				BasicRule operatorNormalizedRule = AggregateOperatorNormalization.normalize(splitRule);
				boolean hasAggregate = ctx.registerRule(operatorNormalizedRule);
				// Only keep rules without aggregates. The ones with aggregates are registered in the context and taken care of later.
				if (!hasAggregate) {
					outputRules.add(operatorNormalizedRule);
				}
			}
		}
		// Substitute AggregateLiterals with generated result literals.
		//TODO Add different Substitution for heuristic aggregates
		outputRules.addAll(rewriteRulesWithAggregates(ctx));
		InputProgram.Builder resultBuilder = InputProgram.builder().addRules(outputRules).addFacts(inputProgram.getFacts())
				.addInlineDirectives(inputProgram.getInlineDirectives());

		// Add sub-programs deriving respective aggregate literals.
		for (Map.Entry<ImmutablePair<AggregateFunctionSymbol, ComparisonOperator>, Set<AggregateInfo>> aggToRewrite : ctx.getAggregateFunctionsToRewrite()
				.entrySet()) {
			ImmutablePair<AggregateFunctionSymbol, ComparisonOperator> func = aggToRewrite.getKey();
			AbstractAggregateEncoder encoder = getEncoderForAggregateFunction(func.left, func.right);
			resultBuilder.accumulate(encoder.encodeAggregateLiterals(aggToRewrite.getValue()));
		}
		return resultBuilder.build();
	}

	private AbstractAggregateEncoder getEncoderForAggregateFunction(AggregateFunctionSymbol function, ComparisonOperator operator) {
		switch (function) {
			case COUNT:
				if (operator == ComparisonOperator.EQ) {
					return countEqualsEncoder;
				} else if (operator == ComparisonOperator.LE) {
					return countLessOrEqualEncoder;
				} else {
					throw new UnsupportedOperationException("No fitting encoder for aggregate function " + function + "and operator " + operator + "!");
				}
			case MIN:
				return minEncoder;
			case MAX:
				return maxEncoder;
			case SUM:
				if (operator == ComparisonOperator.EQ) {
					return sumEqualsEncoder;
				} else if (operator == ComparisonOperator.LE) {
					return sumLessOrEqualEncoder;
				} else {
					throw new UnsupportedOperationException("No fitting encoder for aggregate function " + function + "and operator " + operator + "!");
				}
			default:
				throw new UnsupportedOperationException("Unsupported aggregate function/comparison operator: " + function + ", " + operator);
		}
	}

	/**
	 * Transforms (restricted) aggregate literals of format "VAR OP #AGG_FN{...}" into literals of format
	 * "<result_predicate>(ARGS, VAR)" where ARGS is a function term wrapping the aggregate's global variables.
	 *
	 * @param ctx the {@link AggregateRewritingContext} containing information about all aggregates.
	 * @return for each rule, its rewritten version where aggregates are replaced with output atoms of the encoding.
	 */
	private static List<BasicRule> rewriteRulesWithAggregates(AggregateRewritingContext ctx) {
		List<BasicRule> rewrittenRules = new ArrayList<>();
		//TODO Maybe check if in Heuristic
		for (BasicRule rule : ctx.getRulesWithAggregates()) {
			List<Literal> rewrittenBody = new ArrayList<>();
			Head rewrittenHead = rule.getHead();
			for (Literal lit : rule.getBody()) {
				if (lit instanceof AggregateLiteral) {
					AggregateInfo aggregateInfo = ctx.getAggregateInfo((AggregateLiteral) lit);
					if (aggregateInfo.getIsDynamic()) {
						Head temp = rule.getHead();
						if (temp instanceof NormalHead) {
							HeuristicAtom old = (HeuristicAtom) ((NormalHead) temp).getAtom();;

							List<HeuristicDirectiveAtom> oldPositive = old.getOriginalPositiveCondition();
							List<HeuristicDirectiveAtom> oldNegative = old.getOriginalNegativeCondition();


							oldPositive.addAll(aggregateInfo.getPositiveConditions());
							oldNegative.addAll(aggregateInfo.getNegativeConditions());

							FunctionTerm newPositiveCondition = HeuristicAtom.conditionToFunctionTerm(oldPositive, old.getPositiveCondition().getSymbol());
							FunctionTerm newNegativeCondition = HeuristicAtom.conditionToFunctionTerm(oldNegative, old.getNegativeCondition().getSymbol());

							HeuristicAtom newHeadAtom = new HeuristicAtom(old.getWeightAtLevel(), old.getHeadSign(), old.getHeadAtom(), newPositiveCondition, newNegativeCondition);
							rewrittenHead = new NormalHead(newHeadAtom);
						}
						Set<Atom> bodyReplacementAtoms = aggregateInfo.getOutputAtoms();
						for (Atom atom : bodyReplacementAtoms) {
							rewrittenBody.add(atom.toLiteral(true));
						}
						//TODO Change from BasicLiteral back to BasicAtom
						//TODO Add functionality to allow for lit.isNegated()
						//Currently, negation is not allowed
					}
					else {
						rewrittenBody.add(new BasicLiteral(aggregateInfo.getOutputAtom(), !lit.isNegated()));
					}
				} else {
					rewrittenBody.add(lit);
				}
			}
			rewrittenRules.add(new BasicRule(rewrittenHead, rewrittenBody));
		}
		return rewrittenRules;
	}
}
