package org.eatbacon.example.test;

import org.eatbacon.example.dao.PersonDao;
import org.eatbacon.example.models.Address;
import org.eatbacon.example.models.Person;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PersonDaoTest {
    @Test
    public void testPersonWithoutAddress() {
        Person person = new Person()
                .firstName("Tony")
                .lastName("Tam");

        PersonDao.getInstance().insert(person);

        UUID id = person.getId();
        assertNotNull(id);

        Person existing = PersonDao.getInstance().findById(id);
        assertEquals(existing, person);
    }

    @Test
    public void testPersonWithAddress() {
        Person person = new Person()
                .firstName("Phil")
                .lastName("Dirt")
                .address(new Address()
                        .state("12345 El Monte Blvd")
                        .city("Los Altos Hills")
                        .state("CA")
                        .zip("94022"));

        PersonDao.getInstance().insert(person);

        UUID id = person.getId();
        assertNotNull(id);

        Person existing = PersonDao.getInstance().findById(id);
        assertEquals(existing, person);

        List<Person> peopleInCalifornia = PersonDao.getInstance().findByZip("94022");

        assertTrue(peopleInCalifornia.size() > 0);
    }
}
