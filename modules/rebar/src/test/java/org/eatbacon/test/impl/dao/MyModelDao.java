package org.eatbacon.test.impl.dao;

import org.eatbacon.dao.AbstractDao;
import org.eatbacon.query.Filters;
import org.eatbacon.query.Index;
import org.eatbacon.test.impl.models.MyModel;

import java.util.Arrays;
import java.util.List;

public class MyModelDao extends AbstractDao<MyModel> {
    private static MyModelDao INSTANCE = new MyModelDao();

    @Override
    protected List<Index> getIndexes() {
        // we are indexing on `name` and deep-indexing on `address.zip`
        return Arrays.asList(
                new Index("name"),
                new Index("address", "zip")
        );
    }

    public static MyModelDao getInstance() {
        return INSTANCE;
    }

    protected MyModelDao(){
        super();
    }

    @Override
    protected Class<?> getObjectClass() {
        return MyModel.class;
    }

    @Override
    protected String getUniqueId(MyModel obj) {
        return obj.getId();
    }

    /**
     * Performs an indexed find based on name
     *
     * @param name
     * @return
     */
    public List<MyModel> findByName(String name) {
        return super.find(Filters.eq("name", name));
    }

    /**
     * finds objects based on the zip code, like such:
     * {
     *     "address": {
     *         "zip": "94022"
     *     }
     * }
     * @param zip
     * @return
     */
    public List<MyModel> findByZip(String zip) {
        return super.find(Filters.eq("address", "zip", zip));
    }
}
