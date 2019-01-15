package org.eatbacon.example.dao;


import org.eatbacon.dao.AbstractDao;
import org.eatbacon.example.models.Person;
import org.eatbacon.query.Filters;

import java.util.List;
import java.util.UUID;

public class PersonDao extends AbstractDao<Person> {
    private static PersonDao INSTANCE = new PersonDao();

    public static PersonDao getInstance() {
        return INSTANCE;
    }

    @Override
    protected Class<?> getObjectClass() {
        return Person.class;
    }

    @Override
    protected String getUniqueId(Person obj) {
        return obj.getId().toString();
    }

    @Override
    public void insert(Person person) {
        if(person.getId() == null) {
            person.id(UUID.randomUUID());
        }
        super.insert(person);
    }

    public Person findById(UUID id) {
        return super.findById(id.toString());
    }

    public List<Person> findByLastName(String name) {
        return super.find(Filters.eq("lastName", name));
    }

    /**
     * Looks into the address field of the Person object, if exists, and does a case-sensitive match
     *
     * {
     *     "address": {
     *         "street": "",
     *         "zip": "zip"
     *     }
     * }
     *
     * @param zip
     * @return List of matching users, empty if none are found
     */
    public List<Person> findByZip(String zip) {
        return super.find(Filters.eq("address", "zip", zip));
    }
}
