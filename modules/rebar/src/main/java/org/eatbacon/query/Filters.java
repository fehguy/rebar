package org.eatbacon.query;

public class Filters {
    // TODO: This class should be expanded substantially to include common filters such as `in`, `less than`, etc.
    public static Query eq(String path, Object value) {
        return new Query().path(path).isEqualTo(value);
    }

    /**
     * Creates an equality query. The variable items allows for a path to be supplied in segments, with the last
     * being the value
     *
     * @param items
     * @return
     */
    public static Query eq(Object ... items) {
        Query query = new Query();
        int i;
        for(i = 0; i < items.length - 1; i++) {
            query.path(items[i].toString());
        }

        // TODO we need to check the type of value we're equaling to make sure it's supported
        return query.isEqualTo(items[i]);
    }
}
