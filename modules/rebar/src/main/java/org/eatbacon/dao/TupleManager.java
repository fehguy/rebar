package org.eatbacon.dao;

import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.TransactionContext;
import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;
import org.eatbacon.query.Index;
import org.eatbacon.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;

public class TupleManager {
    static final Logger LOGGER = LoggerFactory.getLogger(TupleManager.class);

    private static final long EMPTY_OBJECT = -2;
    private static final long EMPTY_ARRAY = -1;
    private static final Map<String, TupleManager> processors = new HashMap<>();

    private String uniqueIdName = "_id";
    private Subspace docSpace;
    private Subspace indexSpace;
    private Subspace indexDefinitionSpace;
    private List<Index> indexes;
    private Class<?> cls;

    protected TupleManager(Class<?> cls, String uniqueIdName) {
        this.docSpace = new Subspace(Tuple.from(cls.getName()));
        this.indexSpace = new Subspace(Tuple.from("index_" + cls.getName()));
        this.indexDefinitionSpace = new Subspace(Tuple.from("index_definition_" + cls.getName()));
        this.cls = cls;
        this.uniqueIdName = uniqueIdName;
    }

    public static String getKeySpace(Class<?> cls) {
        return cls.getName();
    }

    /**
     * Ensures the specified index is created. Does not re-index any values
     *
     * @param tcx
     * @param index
     */
    public void ensureIndex(TransactionContext tcx, Index index) {
        if(indexes == null) {
            this.indexes = getIndexes(tcx);
        }
        if(!indexes.contains(index)) {
            tcx.run(tr -> {
                Tuple tuple = new Tuple();
                for (String item : index.getValues()) {
                    tuple = tuple.add(item);
                }

                tr.set(indexDefinitionSpace.pack(tuple),
                        Tuple.from("").pack());
                return null;
            });
        }
    }

    /**
     * Adds the supplied object to an index. Assumes the value can be stringified
     *
     * @param tcx
     * @param path
     * @param value
     */
    public void addToIndex(TransactionContext tcx, Tuple path, Object value) {
        tcx.run(tr -> {
            String id = String.valueOf(value);
            LOGGER.info("indexing path " + path + " with value " + value);
            tr.set(indexSpace.pack(path),
                    Tuple.from(String.valueOf(id)).pack());
            return null;
        });
    }

    /**
     * Returns all indexes in the `indexDefinitionSpace`
     *
     * @param tcx
     * @return List of indexes
     */
    public List<Index> getIndexes(TransactionContext tcx) {
        List<Index> output = new ArrayList<>();
        tcx.run(tr -> {
            try {
                Future<byte[]> v = tr.get(indexDefinitionSpace.pack());
                for (KeyValue kv : tr.getRange(indexDefinitionSpace.range(), 1000)) {
                    Tuple key = indexDefinitionSpace.unpack(kv.getKey());
                    List<String> path = new ArrayList<>();
                    for (Iterator<?> x = key.iterator(); x.hasNext();) {
                        path.add(x.next().toString());
                    }
                    output.add(new Index(path.toArray(new String[path.size()])));
                }
                return output;
            }
            catch (Exception e) {
                return output;
            }
        });
        return output;
    }

    /**
     * Checks to see if a tuple can use an index for lookup
     *
     * @param tcx
     * @param tuple
     * @return true if the tuple is indexed
     */
    protected boolean canUseIndex(TransactionContext tcx, Tuple tuple) {
        if(indexes == null) {
            this.indexes = getIndexes(tcx);
        }
        return this.indexes.contains(new Index(tuple.getItems()));
    }

    /**
     * Gets an instance of the tuple-to-fdb logic for the specified class
     *
     * @param cls
     * @return
     */
    public static TupleManager getInstance(Class<?> cls, String uniqueIdName) {
        String keyspace = getKeySpace(cls);

        synchronized (processors) {
            TupleManager processor = processors.get(keyspace);
            if(processor == null) {
                processor = new TupleManager(cls, uniqueIdName);
                processors.put(keyspace, processor);
            }
            return processor;
        }
    }

    /**
     * Returns all objects for the docSpace. Used for exporting and debugging.
     *
     * TODO: Should stream values back to caller instead of creating a huge list of
     * objects, which is a huge memory issue
     *
     * @param tcx
     * @return
     */
    public List<Object> getAll(TransactionContext tcx) {
        List<Object> output = new ArrayList<>();

        return tcx.run(tr -> {
            try {
                Future<byte[]> v = tr.get(docSpace.pack());
                if (v.get() != null) {
                    // One single item.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    output.add(fromTuples(vals));
                } else {
                    // Multiple items.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    String currentKey = null;
                    for (KeyValue kv : tr.getRange(docSpace.range())) {
                        Tuple key = docSpace.unpack(kv.getKey());
                        Tuple value = Tuple.fromBytes(kv.getValue());

                        // since the tuple is the unique ID, when it changes, we have a new object
                        if(key.get(0) != null && key.get(0) instanceof String) {
                            String tupleKey = String.valueOf(key.get(0));
                            if(currentKey == null) {
                                currentKey = tupleKey;
                            }
                            if(tupleKey.equals(currentKey)) {
                                vals.add(key.popFront().addAll(value));
                            }
                            else {
                                output.add(fromTuples(vals));
                                currentKey = tupleKey;
                                vals.clear();
                            }
                        }
                    }
                    // convert the list of tuples into a Map
                    output.add(fromTuples(vals));
                }
                return output;
            }
            catch (Exception e) {
                e.printStackTrace();
                return output;
            }
        });
    }

    /**
     * Returns a list of values based on the specified query. If no values match the query, an empty
     * list is returned
     *
     * @param tcx
     * @param query
     * @return List of Objects described in a primitive map
     */
    public List<Object> getByQuery(Transaction tcx, Query query) {
        List<Object> output = new ArrayList<>();

        return tcx.run(tr -> {
            try {
                Future<byte[]> v = tr.get(docSpace.pack());
                if (v.get() != null) {
                    // One single item.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    output.add(fromTuples(vals));
                } else {
                    // Multiple items.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    String currentKey = null;
                    for (KeyValue kv : tr.getRange(docSpace.range(), 1000)) {
                        Tuple key = docSpace.unpack(kv.getKey());
                        Tuple value = Tuple.fromBytes(kv.getValue());

                            if(key.get(0) != null && key.get(0) instanceof String) {
                            String tupleKey = String.valueOf(key.get(0));
                            if(currentKey == null) {
                                currentKey = tupleKey;
                            }
                            if(tupleKey.equals(currentKey)) {
                                vals.add(key.popFront().addAll(value));
                            }
                            else {
                                boolean matches = false;
                                for(Tuple tuple : vals) {
                                    if(query.isSatisfiedBy(tuple)) {
                                        matches = true;
                                    }
                                }
                                if(matches) {
                                    output.add(fromTuples(vals));
                                }
                                currentKey = tupleKey;
                                vals.clear();
                            }
                        }
                    }
                    // check the query
                    boolean matches = false;
                    for(Tuple tuple : vals) {
                        if(query.isSatisfiedBy(tuple)) {
                            matches = true;
                        }
                    }
                    if(matches) {
                        output.add(fromTuples(vals));
                    }
                }
                return output;
            }
            catch (Exception e) {
                e.printStackTrace();
                return output;
            }
        });
    }

    /**
     * Returns a single document based on unique id
     * @param tcx
     * @param uniqueId
     * @return
     */
    public Object getDoc(TransactionContext tcx, final Object uniqueId){
        return getDoc(tcx, uniqueId, Tuple.from());
    }

    /**
     * Returns a list of unique identifiers as tuples based on a tuple query.
     *
     * @param tcx
     * @param queryPath
     * @return
     */
    public List<Tuple> getIdsFromIndex(TransactionContext tcx, Tuple queryPath) {
        List<Tuple> output = new ArrayList<>();

        return tcx.run(tr -> {
            try {
                LOGGER.info("querying index for " + queryPath);
                Future<byte[]> v = tr.get(indexSpace.pack(queryPath));
                if(v.get() != null) {
                    Tuple value = Tuple.fromBytes(v.get());
                    output.add(value);
                    return output;
                }
                for (KeyValue kv : tr.getRange(indexSpace.range(queryPath))) {
                    Tuple value = Tuple.fromBytes(kv.getValue());
                    output.add(value);
                }
                return output;
            }
            catch (Exception e) {
                e.printStackTrace();
                return output;
            }
        });
    }

    /**
     * Methods below were copied from https://apple.github.io/foundationdb/hierarchical-documents-java.html
     */
    public String getNewID(TransactionContext tcx){
        return tcx.run(tr -> {
            boolean found = false;
            int newID;
            do {
                newID = (int)(Math.random()*100000000);
                found = true;
                for(KeyValue kv : tr.getRange(docSpace.range(Tuple.from(newID)))){
                    // If not empty, this is false.
                    found = false;
                    break;
                }
            } while(!found);
            return String.valueOf(newID);
        });
    }

    public Object getDoc(TransactionContext tcx, final Object ID, final Tuple prefix){
        return tcx.run(tr -> {
            try {
                Future<byte[]> v = tr.get(docSpace.pack(Tuple.from(ID).addAll(prefix)));
                if (v.get() != null) {
                    // One single item.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    vals.add(prefix.addAll(Tuple.fromBytes(v.get())));
                    return fromTuples(vals);
                } else {
                    // Multiple items.
                    ArrayList<Tuple> vals = new ArrayList<>();
                    for (KeyValue kv : tr.getRange(docSpace.range(Tuple.from(ID).addAll(prefix)))) {
                        vals.add(docSpace.unpack(kv.getKey()).popFront().addAll(Tuple.fromBytes(kv.getValue())));
                    }
                    return fromTuples(vals);
                }
            }
            catch (Exception e) {
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Tuple> toTuplesSwitch(Object o){
        if(o instanceof ArrayList){
            return toTuples((ArrayList<Object>) o);
        } else if(o instanceof Map){
            return toTuples((Map<Object,Object>) o);
        } else {
            return toTuples(o);
        }
    }

    private static ArrayList<Tuple> toTuples(ArrayList<Object> item){
        if(item.isEmpty()){
            ArrayList<Tuple> val = new ArrayList<>();
            val.add(Tuple.from(EMPTY_ARRAY, null));
            return val;
        } else {
            ArrayList<Tuple> val = new ArrayList<>();
            for(int i = 0; i < item.size(); i++){
                for(Tuple sub : toTuplesSwitch(item.get(i))){
                    val.add(Tuple.from(i).addAll(sub));
                }
            }
            return val;
        }
    }

    private static ArrayList<Tuple> toTuples(Map<Object,Object> item){
        if(item.isEmpty()){
            ArrayList<Tuple> val = new ArrayList<>();
            val.add(Tuple.from(EMPTY_OBJECT, null));
            return val;
        } else {
            ArrayList<Tuple> val = new ArrayList<>();
            for(Entry<Object,Object> e : item.entrySet()){
                for(Tuple sub : toTuplesSwitch(e.getValue())){
                    val.add(Tuple.from(e.getKey()).addAll(sub));
                }
            }
            return val;
        }
    }

    private static ArrayList<Tuple> toTuples(Object item){
        ArrayList<Tuple> val = new ArrayList<>();
        val.add(Tuple.from(item));
        return val;
    }

    private static ArrayList<Tuple> getTruncated(ArrayList<Tuple> vals){
        ArrayList<Tuple> list = new ArrayList<>();
        for(Tuple val : vals){
            list.add(val.popFront());
        }
        return list;
    }

    public Object insertDoc(TransactionContext tcx, Map<Object,Object> doc){
        return tcx.run(tr -> {
            for(Tuple t : toTuples(doc)){
                Object uniqueId = doc.get(uniqueIdName);
                tr.set(docSpace.pack(Tuple.from(uniqueId).addAll(t.popBack())),
                        Tuple.from(t.get(t.size() - 1)).pack());
                Tuple path = t.popBack();
                if(canUseIndex(tcx, path)) {
                    LOGGER.debug("Using index for query");
                    addToIndex(tcx, t, uniqueId);
                }
            }
            return doc.get(uniqueIdName);
        });
    }

    private static Object fromTuples(ArrayList<Tuple> tuples){
        if(tuples == null || tuples.size() == 0){
            return null;
        }

        Tuple first = tuples.get(0); // Determine kind of object from
        // first tuple.
        if(first.size() == 1){
            return first.get(0); // Primitive type.
        }

        if(first.equals(Tuple.from(EMPTY_OBJECT, null))){
            return new HashMap<>(); // Empty map.
        }

        if(first.equals(Tuple.from(EMPTY_ARRAY))){
            return new ArrayList<>(); // Empty list.
        }

        HashMap<Object,ArrayList<Tuple>> groups = new HashMap<>();
        for(Tuple t : tuples){
            if(groups.containsKey(t.get(0))){
                groups.get(t.get(0)).add(t);
            } else {
                ArrayList<Tuple> list = new ArrayList<>();
                list.add(t);
                groups.put(t.get(0),list);
            }
        }

        if(first.get(0).equals(0l)){
            // Array.
            ArrayList<Object> array = new ArrayList<>();
            for(Entry<Object,ArrayList<Tuple>> g : groups.entrySet()){
                array.add(fromTuples(getTruncated(g.getValue())));
            }
            return array;
        } else {
            // Object.
            HashMap<Object,Object> map = new HashMap<>();
            for(Entry<Object,ArrayList<Tuple>> g : groups.entrySet()){
                map.put(g.getKey(), fromTuples(getTruncated(g.getValue())));
            }
            return map;
        }
    }
}
