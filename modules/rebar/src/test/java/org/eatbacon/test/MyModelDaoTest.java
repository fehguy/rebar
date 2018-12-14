package org.eatbacon.test;

import org.eatbacon.test.impl.dao.MyModelDao;
import org.eatbacon.test.impl.models.Address;
import org.eatbacon.test.impl.models.MyModel;
import org.eatbacon.query.Index;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class MyModelDaoTest {
    @Test
    public void testInsert() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModel model = new MyModel()
                .id("1001")
                .name("Tony")
                .createdAt(new Date())
                .childNames(Arrays.asList(12, 12));
        dao.insert(model);
    }

    @Test
    public void testFindById() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModel existing = dao.findById("1001");
        assertNotNull(existing);

        MyModel model = new MyModel()
                .id("1002")
                .name("Bob")
                .createdAt(new Date())
                .address(new Address()
                        .street("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022")
                ).childNames(Arrays.asList(15, 16));
        dao.insert(model);

        existing = dao.findById("1002");
        assertNotNull(existing);
    }

    @Test
    public void testFindWithNoMatch() {
        MyModelDao dao = MyModelDao.getInstance();

        List<MyModel> filtered = dao.findByName("none");
        assertTrue(filtered.size() == 0);
    }

    @Test
    public void testFindAll() {
        MyModelDao dao = MyModelDao.getInstance();

        List<MyModel> multi = dao.findAll();
        assertTrue(multi.size() == 2);
    }

    @Test
    public void testFindByName() {
        MyModelDao dao = MyModelDao.getInstance();

        List<MyModel> filtered = dao.findByName("Bob");
        assertTrue(filtered.size() == 1);

        filtered = dao.findByName("Tony");
        assertTrue(filtered.size() == 1);
    }

    @Test
    public void testFindByZip() {
        MyModelDao dao = MyModelDao.getInstance();
        List<MyModel> filtered = dao.findByZip("94022");
        assertTrue(filtered.size() == 1);
    }

    @Test
    public void testCreateIndexes() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModelDao.getInstance().ensureIndex(new Index("name"));
    }
}
