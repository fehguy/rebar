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

        MyModel existing = dao.findById("1002");
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
        assertTrue(multi.size() > 0);
    }

    @Test
    public void testFindByName() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModel model = new MyModel()
                .id("1004")
                .name("Fred")
                .createdAt(new Date())
                .address(new Address()
                        .street("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022")
                ).childNames(Arrays.asList(15, 16));
        dao.insert(model);

        List<MyModel> filtered = dao.findByName("Fred");
        assertTrue(filtered.size() == 1);


        model = new MyModel()
                .id("1005")
                .name("Grant")
                .createdAt(new Date())
                .address(new Address()
                        .street("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022")
                ).childNames(Arrays.asList(15, 16));
        dao.insert(model);

        filtered = dao.findByName("Grant");
        assertTrue(filtered.size() == 1);
    }

    @Test
    public void testFindByIndexedValue() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModel model = new MyModel()
                .id("1005")
                .name("Grant")
                .createdAt(new Date())
                .address(new Address()
                        .street("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022-1993")
                ).childNames(Arrays.asList(15, 16));
        dao.insert(model);

        List<MyModel> filtered = dao.findByZip("94022-1993");
        assertTrue(filtered.size() == 1);

        filtered = dao.findByState("CA");
        assertTrue(filtered.size() >= 1);

        filtered = dao.findByState("ZZZ");
        assertTrue(filtered.size() == 0);
    }

    @Test
    public void testFindByNonIndexedValue() {
        MyModelDao dao = MyModelDao.getInstance();

        MyModel model = new MyModel()
                .id("1006")
                .name("Grant")
                .createdAt(new Date())
                .address(new Address()
                        .street("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022-1993")
                ).childNames(Arrays.asList(15, 16));
        dao.insert(model);

        List<MyModel> filtered = dao.findByState("CA");
        assertTrue(filtered.size() >= 1);

        filtered = dao.findByState("ZZZ");
        assertTrue(filtered.size() == 0);
    }

    @Test
    public void testCreateIndexes() {
        MyModelDao.getInstance().ensureIndex(new Index("name"));
    }
}
