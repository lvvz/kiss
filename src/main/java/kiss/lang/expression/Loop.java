package kiss.lang.expression;

import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.Symbol;
import kiss.lang.Environment;
import kiss.lang.Expression;
import kiss.lang.Result;
import kiss.lang.Type;
import kiss.lang.impl.EvalResult;
import kiss.lang.impl.RecurResult;

/**
 * A let expression, creates a local lexical binding
 * 
 * @author Mike
 */
public class Loop extends Expression {

	private final Symbol[] syms;
	private final Expression[] initials;
	private final Expression body;

	public Loop(Symbol[] syms, Expression[] initials, Expression body) {
		this.syms=syms;
		this.initials=initials;
		this.body=body;
	}

	public static Loop create(Symbol[] syms, Expression[] initials, Expression body) {
		return new Loop(syms,initials,body);
	}
	
	public Loop update(Symbol[] syms, Expression[] initials, Expression body) {
		Expression[] nis =this.initials;
		for (int i=0; i<initials.length; i++) {
			if (nis[i]!=initials[i]) {
				nis=initials;
				break;
			}
		}
		if ((this.syms==syms)&&(this.body==body)&&(this.initials==nis)) return this;
		return create(syms, nis,body);
	}
	
	@Override
	public Expression optimise() {
		Expression b=body.optimise();
		Expression[] is=initials.clone();
		for (int i=0; i<is.length; i++) {
			is[i]=is[i].optimise();
		}
		
		return update(syms,is,b);
	}
	
	@Override
	public Type getType() {
		return body.getType();
	}
	
	@Override
	public boolean isPure() {
		if (!body.isPure()) return false;
		for (Expression i:initials) {
			if (!i.isPure()) return false;
		}
		return true;
	}
	
	@Override
	public Result interpret(Environment d, IPersistentMap bindings) {
		int n=syms.length;
		for (int i=0; i<n; i++) {
			Result t=initials[i].interpret(d, bindings);
			if (t.isExiting()) return t;
			Object result=t.getResult();
			bindings=bindings.assoc(syms[i], result);
		}
		while (true) {
			Result r=body.interpret(d, bindings);
			if (!(r instanceof RecurResult)) {
				return r;
			}
			
			RecurResult rr=(RecurResult) r;
			for (int i=0; i<n; i++) {
				bindings=bindings.assoc(syms[i], rr.values[i]);
			}
		}		
	}
	
	@Override
	public Expression specialise(Type type) {
		Expression newBody=body.specialise(type);
		return update(syms,initials,newBody);
	}
		
	@Override
	public Expression substitute(IPersistentMap bindings) {
		Expression[] nis=initials.clone();
		for (int i=0; i<initials.length; i++) {
			nis[i]=initials[i].substitute(bindings);
			bindings=bindings.without(syms[i]);
		}
		Expression nbody=body.substitute(bindings);
		if (nbody==null) return null;
		
		return update(syms,nis,nbody);
	}
	
	@Override
	public IPersistentSet accumulateFreeSymbols(IPersistentSet s) {
		s=body.accumulateFreeSymbols(s);
		s=s.disjoin(syms);
		for (Expression i: initials) {
			s=i.accumulateFreeSymbols(s);
		}
		return s;
	}
	
	@Override
	public void validate() {
		// OK?
	}


}
