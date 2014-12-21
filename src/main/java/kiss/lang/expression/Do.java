package kiss.lang.expression;

import java.util.Arrays;

import kiss.lang.Environment;
import kiss.lang.Expression;
import kiss.lang.Result;
import kiss.lang.Type;
import kiss.lang.impl.EvalResult;
import kiss.lang.impl.KissException;
import kiss.lang.type.Nothing;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;

/**
 * A Kiss "do" expression.
 * 
 * All subexpressions are evaluated in sequence, and the result is the result of the final 
 * subexpression.
 * 
 * @author Mike
 *
 */
public class Do extends kiss.lang.Expression {
	private final Expression[] exps;
	private final int length;
	
	private Do(Expression[] exps) {
		this.exps=exps;
		length=exps.length;
	}
	
	public static Do create(Expression... exps) {
		return new Do(exps);
	}
	
	@Override
	public Type getType() {
		if (length==0) return Nothing.INSTANCE;
		return exps[length-1].getType();
	}
	
	@Override
	public Expression optimise() {
		Expression[] es=new Expression[length];
		int j=0;
		boolean found=false;
		for (int i=0; i<length; i++) {
			Expression old=exps[i];
			Expression x=old.optimise();
			if (x!=old) found=true;
			// we can eliminate pure expressions in non-ending position
			if ((i==(length-1))||(!x.isPure())) {
				es[j++]=x;
			}
		}
		if ((!found)&&(j==length)) return this;
		if (j==1) return es[0];
		return create(Arrays.copyOf(es, j));
	}
	
	@Override
	public boolean isPure() {
		for (Expression e:exps) {
			if (!e.isPure()) return false;
		}
		return true;
	}

	@Override
	public Expression specialise(Type type) {
		// specialise based on the last expression in the do block, since this defines the return value
		if (length==0) return null;
		Expression end=exps[length-1];
		Expression send=end.specialise(type);
		if (send==null) return null;
		if (send==end) return this;
		Expression[] nexps=exps.clone();
		nexps[length-1]=send;
		return create(nexps);
	}
	
	@Override
	public Expression substitute(IPersistentMap bindings) {
		int i=0;
		Expression nx=null;
		for (;i<length; i++) {
			Expression x=exps[i];
			nx=x.substitute(bindings);
			if (nx==null) return null;
			if (nx!=x) break;
		}
		if (i==length) return this; // no changes
		Expression[] nexps=exps.clone();
		nexps[i++]=nx;
		for (;i<length; i++) {
			Expression x=exps[i];
			nx=x.substitute(bindings);
			if (nx==null) return null;
			nexps[i]=nx;
		}
		return create(nexps);
	}

	@Override
	public Result interpret(Environment e, IPersistentMap bindings) {
		if (length==0) return new EvalResult(e);
		Result r=null;
		for (int i=0; i<length; i++) {
			r=exps[i].interpret(e,bindings);
			e=r.getEnvironment();
			if (r.isExiting()) return r;
		}
		return r;
	}

	@Override
	public IPersistentSet accumulateFreeSymbols(IPersistentSet s) {
		for (int i=0; i<length; i++) {
			s=exps[i].accumulateFreeSymbols(s);
		}
		return s;
	}
	
	@Override
	public void validate() {
		if (length!=exps.length) throw new KissException("Mismatched length!");
	}


}
