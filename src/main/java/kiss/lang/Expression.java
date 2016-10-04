package kiss.lang;

import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentHashSet;
import kiss.lang.impl.KissUtils;

/**
 * Abstract base class for immutable Kiss Expression nodes
 * 
 * Design intent:
 * - Represent Kiss AST
 * - Can be optimised
 * - Can be evaluated / interpreted given an execution environment
 * - Can be compiled, given satisfaction of all external dependencies
 * - Immutable
 * 
 * @author Mike
 *
 */
public abstract class Expression {

	/**
	 * Gets the result type of the expression. Evaluation of an expression is guaranteed to return
	 * a result of this type.
	 */
	public abstract Type getType(); 
	
	/**
	 * Specialises an expression to guarantee returning the given type, or a strict subset
	 * 
	 * Returns null if this specialisation is impossible 
	 * Must return the same expression if no additional specialisation can be performed.
	 */
	public abstract Expression specialise(Type type);
	
	/**
	 * Specialises an expression using the given Symbol -> Value substitution map
	 * 
	 * @param bindings A map of symbols to values
	 * @return
	 */
	public abstract Expression substitute(IPersistentMap bindings);
	
	/**
	 * Optimises this expression. Performs constant folding, etc.
	 * @return An optimised Expression
	 */
	public Expression optimise() {
		return this;
	}
	
	/**
	 * Evaluate an expression within an environment, interpreter style.
	 * 
	 * Any changes to the Environment are discarded.
	 * 
	 * @param e Any Environment in which to evaluate the expression
	 * @return The result of the expression.
	 */
	public Object eval(Environment e) {
		return interpret(KissUtils.ret1(e,e=null), PersistentHashMap.EMPTY).getResult();
	}
	
	/**
	 * Evaluates this expression in an empty environment.
	 * 
	 * Any changes to the Environment are discarded.
	 * 
	 * @return
	 */
	public Object eval() {
		return interpret(Environment.EMPTY, PersistentHashMap.EMPTY).getResult();
	}
	
	/**
	 * Compute the effect of this expression, returning a new Environment
	 * @param bindings TODO
	 */
	public abstract Result interpret(Environment d, IPersistentMap bindings);

	/**
	 * Computes the result of this expression in a given Environment. 
	 * 
	 * Returns a new Environment, use Environment.getResult() to see the result of the expression.
	 * 
	 * @param e
	 * @return
	 */
	public final Result interpret(Environment e) {
		return interpret(KissUtils.ret1(e,e=null),PersistentHashMap.EMPTY);
	}
	
	/**
	 * Computes the result of the expression in the environmnet contained by a previous EvalResult
	 * 
	 * The old evaluation result is discarded
	 */
	public final Result interpret(Result e) {
		return interpret(e.getEnvironment());
	}
	
	/**
	 * Returns true if this expression is a constant value
	 * @return
	 */
	public boolean isConstant() {
		return false;
	}
	
	/**
	 * Returns true if this expression is a macro
	 * @return
	 */
	public boolean isMacro() {
		return false;
	}

	/**
	 * Returns true if the expression is pure (no side effects) with respect to all free symbols.
	 * 
	 * A pure expression can be safely replaced with its evaluation result
	 * @return
	 */
	public boolean isPure() {
		return false;
	}

	/**
	 * Gets the free symbols in an Expression, conj'ing them onto a given persistent set
	 * @param s
	 * @return
	 */
	public abstract IPersistentSet accumulateFreeSymbols(IPersistentSet s);
	
	public IPersistentSet getFreeSymbols() {
		return accumulateFreeSymbols(PersistentHashSet.EMPTY);
	}

	/**
	 * Validates the structure of the expression. Checks that all invariants are satisfied
	 */
	public abstract void validate();
}
