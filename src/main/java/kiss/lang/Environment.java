package kiss.lang;

import java.util.Iterator;
import java.util.Map;

import clojure.lang.APersistentMap;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ISeq;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;

/**
 * This is the immutable environment used by the Kiss compiler
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

	private Environment() {
		this(PersistentHashMap.EMPTY);
	}
	
	private Environment(IPersistentMap map) {
		this.map=map;
	}
	
	@Override
	public IPersistentMap assoc(Object key, Object val) {
		Mapping m=getMapping(key);
		if (m!=null) {
			if (m.getValue()==val) return this;
		}
		return new Environment(map.assoc(key, Mapping.create(val)));
	}

	@Override
	public IPersistentMap assocEx(Object key, Object val) {
		Mapping m=getMapping(key);
		if (m!=null) {
			if (m.getValue()==val) return this;
		}
		return new Environment(map.assoc(key, Mapping.create(val)));
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

	@Override
	public Iterator iterator() {
		return new EnvioronmentIterator(map.iterator());
	}
	
	private static final class EnvioronmentIterator implements Iterator {
		final Iterator source;
		
		private EnvioronmentIterator(Iterator vs) {
			this.source=vs;
		}
		
		@Override
		public boolean hasNext() {
			return source.hasNext();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object next() {
			Map.Entry entry=(Entry) source.next();
			Mapping m=(Mapping)entry.getValue();
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

}