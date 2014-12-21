package kiss.lang.expression;

import kiss.lang.Environment;
import kiss.lang.EvalResult;
import kiss.lang.Expression;
import kiss.lang.Type;
import kiss.lang.impl.KissException;
import kiss.lang.impl.KissUtils;
import kiss.lang.type.FunctionType;
import kiss.lang.type.Reference;
import clojure.lang.ArraySeq;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.RT;

/**
 * Expression representing a function application
 * 
 * @author Mike
 *
 */
public class Application extends Expression {
	private final Expression func;
	private final Expression[] params;
	private final int arity;
	
	private Application(Expression func, Expression[] params) {
		this.func=func;
		this.params=params;
		this.arity=params.length;
	}
	
	public static Expression create(Expression func, Expression... params) {
		return new Application(func,params.clone());
	}
	
	private Expression update(Expression nFunc, Expression[] nParams) {
		if (nFunc!=func) return Application.create(nFunc, nParams);
		for (int i=0; i<arity; i++) {
			if (params[i]!=nParams[i]) return Application.create(nFunc, nParams);
		}
		return this;
	}
	
	public Expression optimise() {
		Expression nFunc=func.optimise();
		
		if (nFunc.isConstant()) {
			// IFn fn=(IFn)nFunc.eval();
			// TODO: macro expansion here??
		}
		
		Expression[] nParams=new Expression[arity];
		boolean maybeApply=true;
		for (int i=0; i<arity; i++) {
			Expression on=params[i];
			on=on.optimise();
			nParams[i]=on;
			if (on.isConstant()) {
				// OK
			} else {
				maybeApply=false;
			}
		}
		if (maybeApply&&nFunc.isConstant()) {
			IFn fn=(IFn) ((Constant<?>)nFunc).getValue();
			if (KissUtils.isPureFn(fn)) {
				// compute result
				Object[] ps=new Object[arity];
				for (int i=0; i<arity; i++) {
					ps[i]=((Constant<?>)nParams[i]).getValue();
				}
				return Constant.create(fn.applyTo(RT.seq(ps)));
			}
		}
		return update(nFunc,nParams);
	}

	@Override
	public Type getType() {
		Type ft=func.getType();
		if (ft instanceof FunctionType) {
			return ((FunctionType)ft).getReturnType();
		}
		return Reference.INSTANCE;
	}

	@Override
	public EvalResult interpret(Environment d, IPersistentMap bindings) {
		EvalResult r=func.interpret(d, bindings);
		Object o=r.getResult();
		if (!(o instanceof IFn)) throw new KissException("Not a function: "+o);
		IFn fn=(IFn)o;
		
		int n=params.length;
		Object[] args=new Object[n];
		for (int i=0; i<n; i++) {
			r=params[i].interpret(d, bindings);
			if (r.isExiting()) return r;
			args[i]=r.getResult();
		}
		
		return r.withResult(fn.applyTo(ArraySeq.create(args)));
	}
	
	@Override
	public boolean isPure() {
		if (!func.isPure()) return false;
		for (Expression e:params) {
			if (!e.isPure()) return false;
		}
		return true;
	}

	@Override
	public Expression specialise(Type type) {
		if (type.contains(this.getType())) return this;
		return Cast.create(type, this);
	}
	
	@Override
	public IPersistentSet accumulateFreeSymbols(IPersistentSet s) {
		s=func.accumulateFreeSymbols(s);
		for (int i=0; i<arity; i++) {
			s=params[i].accumulateFreeSymbols(s);
		}
		return s;
	}

	@Override
	public Expression substitute(IPersistentMap bindings) {
		Expression nfunc=func.substitute(bindings);
		Expression[] nParams=params.clone();
		for (int i=0; i<arity; i++) {
			nParams[i]=params[i].substitute(bindings);
		}
		return update(nfunc,nParams);
	}

	@Override
	public void validate() {
		if (params.length!=arity) throw new KissException("Mismatched arity!");
	}

}
