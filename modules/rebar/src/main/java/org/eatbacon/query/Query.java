package org.eatbacon.query;

import com.apple.foundationdb.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Query {
    public static final String WILDCARD = "*";
    private List<String> path = new ArrayList<>();
    private Object value;

    public Query path(String part) {
        path.add(part);
        return this;
    }

    public String[] getPathSegments() {
        return path.toArray(new String[path.size()]);
    }

    public Tuple getPathAsTuple() {
        return new Tuple().addAll(path);
    }

    public Query isEqualTo(Object value) {
        this.value = value;
        return this;
    }

    public Object getValue() {
        return value;
    }

    /**
     * The query object is responsible for checking to see if a tuple matches the criteria. This lets
     * development of query types live in this class
     *
     * @param tuple
     * @return
     */
    public boolean isSatisfiedBy(Tuple tuple) {
        int i;
        // TODO: this is very inefficient to do over-and-over. Should revise to make it smarter
        for(i = 0; i < path.size(); i++) {
            if(tuple.size() <= i) {
                return false;
            }
            Object val = tuple.get(i);
            String segment = path.get(i);

            if(!segment.equals(String.valueOf(val)) && !WILDCARD.equals(segment)) {
                return false;
            }
        }

        if(tuple.size() > i) {
            // TODO: lots to add here for different type support
            if(tuple.get(i).equals(getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return path.stream().collect(Collectors.joining(".")) + "=" + value;
    }

    public Tuple build() {
        // TODO: not really safe to just `toString` the value
        return getPathAsTuple().add(String.valueOf(value));
    }
}
