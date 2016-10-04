package kiss.lang.expression;

import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import kiss.lang.Environment;
import kiss.lang.Expression;
import kiss.lang.Result;
import kiss.lang.Type;
import kiss.lang.impl.KissUtils;

/**
 * Expression for a standard "if" conditional
 * 
 * @author Mike
 */
public class If extends Expression {
	private final Type type;
	private final Expression cond;
	private final Expression doThen;
	private final Expression doElse;
	
	private If(Expression cond, Expression doThen, Expression doElse) {
		this.cond=cond;
		this.doThen=doThen;
		this.doElse=doElse;
		this.type=doThen.getType().union(doElse.getType());
	}
	
	public static Expression create(Expression cond,Expression doThen, Expression doElse) {
		return new If(cond,doThen,doElse);
	}
	
	public Expression update(Expression cond,Expression doThen, Expression doElse) {
		if (cond.isConstant()) {
			return (KissUtils.truthy(cond.eval()))?doThen:doElse;
		} 
		if (cond.isPure()) {
			// we can optimise away the test only if cond is pure (i.e. no side effects)
			Type t=cond.getType();
			if (t.cannotBeFalsey()) return doThen;
			if (t.cannotBeTruthy()) return doElse;
		}
		
		if ((cond==this.cond)&&(doThen==this.doThen)&&(doElse==this.doElse)) return this;

		return new If(cond,doThen,doElse);
	}
	
	@Override
	public Expression optimise() {
		Expression cond=this.cond.optimise();
		Expression doThen=this.doThen.optimise();
		Expression doElse=this.doElse.optimise();
		return update(cond,doThen,doElse);
	}
	
	@Override
	public boolean isPure() {
		return cond.isPure()&&doThen.isPure()&&doElse.isPure();
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Expression specialise(Type type) {
		Expression newThen=doThen.specialise(type);
		Expression newElse=doElse.specialise(type);
		if ((newThen==null)&&(newElse==null)) return null;
		return update(cond,newThen,newElse);
	}
	
	@Override
	public Expression substitute(IPersistentMap bindings) {
		Expression ncond=cond.substitute(bindings);
		if (ncond==null) return null;
		Expression nthen=doThen.substitute(bindings);
		if (nthen==null) return null;
		Expression nelse=doElse.substitute(bindings);
		if (nelse==null) return null;
		
		return update(ncond,nthen,nelse);
	}

	@Override
	public Result interpret(Environment d, IPersistentMap bindings) {
		Result r=cond.interpret(d, bindings);
		if (r.isExiting()) return r;
		if (KissUtils.truthy(r.getResult())) {
			return doThen.interpret(d, bindings);
		} else {
			return doElse.interpret(d, bindings);
		}
	}
	
	@Override
	public IPersistentSet accumulateFreeSymbols(IPersistentSet s) {
		s=cond.accumulateFreeSymbols(s);
		s=doThen.accumulateFreeSymbols(s);
		s=doElse.accumulateFreeSymbols(s);
		return s;
	}

	@Override
	public void validate() {
		// OK?
	}

}
