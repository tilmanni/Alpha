package at.ac.tuwien.kr.alpha.core.solver;

import static at.ac.tuwien.kr.alpha.core.test.util.TestUtils.assertRegressionTestAnswerSet;
import static at.ac.tuwien.kr.alpha.core.test.util.TestUtils.assertRegressionTestAnswerSetsWithBase;

/**
 * Tests ASP programs containing arithmetic terms at arbitrary positions.
 *
 * Copyright (c) 2020, the Alpha Team.
 */
public class ArithmeticTermsTest {

	@RegressionTest
	public void testArithmeticTermInHead(RegressionTestConfig cfg) {
		String program = "dom(1). dom(2)."
			+ "p(X+3) :- dom(X).";
		assertRegressionTestAnswerSet(cfg, program, "dom(1),dom(2),p(4),p(5)");
	}

	@RegressionTest
	public void testArithmeticTermInRule(RegressionTestConfig cfg) {
		String program = "dom(1). dom(2)."
			+ "p(Y+4) :- dom(X+1), dom(X), Y=X, X=Y.";
		assertRegressionTestAnswerSet(cfg, program, "dom(1),dom(2),p(5)");
	}

	@RegressionTest
	public void testArithmeticTermInChoiceRule(RegressionTestConfig cfg) {
		String program = "cycle_max(4). cycle(1)." +
			"{ cycle(N+1) } :- cycle(N), cycle_max(K), N<K.";
		assertRegressionTestAnswerSetsWithBase(cfg, program, "cycle_max(4),cycle(1)", "", "cycle(2)", "cycle(2),cycle(3)", "cycle(2),cycle(3),cycle(4)");
	}

	@RegressionTest
	public void testMultipleArithmeticTermsInRules(RegressionTestConfig cfg) {
		String program = "q(1). q(3). r(f(40),6)." +
			"p(X+1) :- q(Y/2), r(f(X*2),Y), X-2 = Y*3, X = 0..20." +
			"bar(X,Y) :- q(Y/2), r(f(X*2),Y), X-2 = Y*3, X = 20.";
		assertRegressionTestAnswerSet(cfg, program, "q(1),q(3),r(f(40),6),p(21),bar(20,6)");
	}

	@RegressionTest
	public void testMultipleArithmeticTermsInFunctionTermsInHead(RegressionTestConfig cfg) {
		String program = "dom(1). dom(2)."
			+ "p(f(X+1),g(X+3)) :- dom(X).";
		assertRegressionTestAnswerSet(cfg, program, "dom(1),dom(2),p(f(2),g(4)),p(f(3),g(5))");
	}

	@RegressionTest
	public void testMultipleNestedArithmeticTermsInRules(RegressionTestConfig cfg) {
		String program = "domx(1). domx(2). domy(6)."
			+ "p(f(X+(Y/2))) :- domx(X), domy(Y).";
		assertRegressionTestAnswerSet(cfg, program, "domx(1),domx(2),domy(6),p(f(4)),p(f(5))");
	}
}
