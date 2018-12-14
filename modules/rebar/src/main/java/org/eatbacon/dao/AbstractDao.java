package org.eatbacon.dao;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.tuple.Tuple;
import io.swagger.util.Json;
import org.eatbacon.query.Index;
import org.eatbacon.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is an abstract parameterized class that provides persistence functionality for a single class
 *
 * @param <T>
 */
public abstract class AbstractDao<T> {
    static final Logger LOGGER = LoggerFactory.getLogger(AbstractDao.class);

    private static final FDB fdb;
    private static final Database db;

    protected abstract Class<?> getObjectClass();
    protected abstract String getUniqueId(T obj);

    // the uniqueIdName is never exposed to the caller, and is used only internally. You would only need to
    // override this only if there is a conflict with an existing field name
    protected String uniqueIdName = "_id";

    // indexes are defined here and are optional. Override to add indexes in your implementation
    protected List<Index> getIndexes() {
        return new ArrayList<>();
    }

    static {
        // TODO: Use a configuration when opening the DB
        fdb = FDB.selectAPIVersion(600);
        db = fdb.open();
    }

    public AbstractDao() {
        // make sure all indexes are configured for this DAO
        for(Index index : getIndexes()) {
            ensureIndex(index);
        }
    }

    /**
     * returns the TupleManager for a specified class
     *
     * @return
     */
    protected TupleManager getTupleManager() {
        return TupleManager.getInstance(getObjectClass(), uniqueIdName);
    }

    /**
     * Ensures indexes are created for this class and creates them if they are not.
     *
     * @param index
     */
    public void ensureIndex(Index index) {
        List<Index> existingIndexes = db.run((Transaction tr) -> getTupleManager().getIndexes(tr));

        if(!existingIndexes.contains(index)) {
            db.run((Transaction tr) -> {
                getTupleManager().ensureIndex(tr, index);
                return null;
            });
        }
        else {
            LOGGER.info("index already exists");
        }
    }

    /**
     * Converts an object into a Map and passes it on to the tuple manager for storage
     *
     * @param obj
     */
    public void insert(T obj) {
        Map<Object, Object> map = Json.mapper().convertValue(obj, Map.class);
        map.put(uniqueIdName, getUniqueId(obj));

        db.run((Transaction tr) -> {
            String id = getUniqueId(obj);
            if(id == null) {
                id = getTupleManager().getNewID(tr);
            }

            map.put("_id", id);
            getTupleManager().insertDoc(tr, map);
            return null;
        });
    }

    /**
     * Find an object based on unique ID
     *
     * @param id
     * @return
     */
    public T findById(String id) {
        return db.run((Transaction tr) -> {
            Object obj = getTupleManager().getDoc(tr, id);
            return (T)Json.mapper().convertValue(obj, getObjectClass());
        });
    }

    /**
     * finds all values and returns in a list.
     * TODO: should stream values to a processor instead of putting in a single map
     * TODO: should take a SKIP and LIMIT parameter to avoid getting everything and allow range queries
     *
     * @return
     */
    public List<T> findAll() {
        return db.run((Transaction tr) -> {
            List<Object> obj = getTupleManager().getAll(tr);
            List<?> output = obj.stream().map(item ->
                    Json.mapper().convertValue(item, getObjectClass())).collect(Collectors.toList());
            return (List<T>) output;
        });
    }

    /**
     * Finds items based on a query
     *
     * TODO: should stream results to client
     * TODO: should have skip/limit on this as well
     *
     * @param query
     * @return
     */
    public List<T> find(Query query) {
        TupleManager md = getTupleManager();
        return db.run((Transaction tr) -> {
            List<T> output = new ArrayList<>();
            Tuple queryPath = query.getPathAsTuple();
            if(md.canUseIndex(tr, queryPath)) {
                md.getIdsFromIndex(tr, query.build()).forEach(idTuple -> {
                    LOGGER.debug("Looking up object by id " + idTuple);
                    T val = findById(String.valueOf(idTuple.get(0)));
                    if(val != null) {
                        output.add(val);
                    }
                });
                return output;
            }
            else {
                List<Object> objs = md.getByQuery(tr, query);
                return (List<T> ) objs.stream().map(item ->
                        Json.mapper().convertValue(item, getObjectClass())).collect(Collectors.toList());
            }
        });
    }
}
