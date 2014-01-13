package kiss.lang;

import java.util.Iterator;
import java.util.Set;

import clojure.lang.APersistentMap;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.ISeq;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentHashSet;
import clojure.lang.Symbol;

/**
 * This is the immutable environment used by the Kiss compiler
 * 
 * Design intent: 
 *  - Behaves like an immutable map of Symbols -> Values
 *  - Maintains a dependency graph for recompilation
 *  - Maintains a "result" value, so it can include an expression return value
 *  
 * It is a first class object, but probably shouldn't be messed with outside of the kiss.core functions.
 * 
 * @author Mike
 *
 */
public final class Environment extends APersistentMap {
	private static final long serialVersionUID = -2048617052932290067L;
	public static final Environment EMPTY = new Environment();
	
	public final IPersistentMap map;
	public final Object result;

	private Environment() {
		this(PersistentHashMap.EMPTY);
	}
	
	private Environment(IPersistentMap map) {
		this(map,null);
	}
	
	private Environment(IPersistentMap map, Object result) {
		this.map=map;
		this.result=result;
	}
	
	public Environment withResult(Object value) {
		if (value==result) return this;
		return new Environment(map,value);
	}
	
	public Environment withAssoc(Symbol key, Object value) {
		return new Environment(map.assoc(key, Mapping.create(value)),value);
	}
	
	public Environment withAssocResult(Symbol key, Object value, Object result) {
		return new Environment(map.assoc(key, Mapping.create(value)),result);
	}
	
	public Environment define(Symbol key, Expression body, IPersistentMap bindings) {
		@SuppressWarnings("unused")
		IPersistentSet free=body.getFreeSymbols(PersistentHashSet.EMPTY);
		
		// TODO: dependencies on free vars
		
		Object value=body.compute(this, bindings);
		
		return withAssocResult(key,value,this);
	}
	
	@Override
	public Environment assoc(Object key, Object val) {
		Mapping m=getMapping(key);
		if (m!=null) {
			if (m.getValue()==val) return this;
		}
		return withAssoc((Symbol) key,val);
	}
	

	@Override
	public Environment assocEx(Object key, Object val) {
		Mapping m=getMapping(key);
		if (m!=null) {
			if (m.getValue()==val) return this;
		}
		return withAssoc((Symbol) key,val);
	}

	@Override
	public IPersistentMap without(Object key) {
		Mapping m=getMapping(key);
		if (m==null) return this;
		return new Environment(map.without(key));
	}
	
	public Mapping getMapping(Object key) {
		return (Mapping)map.valAt(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<?> iterator() {
		return new EnvioronmentIterator(map.iterator());
	}
	
	private static final class EnvioronmentIterator implements Iterator<Entry<Symbol,Object>> {
		final Iterator<Entry<Symbol,Mapping>> source;
		
		private EnvioronmentIterator(Iterator<Entry<Symbol,Mapping>> vs) {
			this.source=vs;
		}
		
		@Override
		public boolean hasNext() {
			return source.hasNext();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Entry<Symbol,Object> next() {
			Entry<Symbol,Mapping> entry=source.next();
			Mapping m=entry.getValue();
			return m.toMapEntry(entry.getKey());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Immutable!");
		}		
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public IMapEntry entryAt(Object key) {
		Mapping m=getMapping(key);
		if (m==null) return null;
		return m.toMapEntry(key);
	}

	@Override
	public int count() {
		return map.count();
	}

	@Override
	public IPersistentCollection empty() {
		return EMPTY;
	}

	@Override
	public ISeq seq() {
		return clojure.lang.IteratorSeq.create(iterator());
	}

	@Override
	public Object valAt(Object key) {
		Mapping m=getMapping(key);
		if (m==null) return null;
		return m.getValue();
	}

	@Override
	public Object valAt(Object key, Object notFound) {
		Mapping m=getMapping(key);
		if (m==null) return notFound;
		return m.getValue();
	}

	public Object getResult() {
		return result;
	}


}
