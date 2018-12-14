package org.eatbacon.test.impl.models;

import java.util.Date;
import java.util.List;

public class MyModel {
    private String id;
    private String name;
    private List<Integer> childNames;
    private Date createdAt;
    private Address address;

    public MyModel id(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MyModel name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MyModel childNames(List<Integer> childNames) {
        this.childNames = childNames;
        return this;
    }

    public List<Integer> getChildNames() {
        return childNames;
    }

    public void setChildNames(List<Integer> childNames) {
        this.childNames = childNames;
    }

    public MyModel createdAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }


    public MyModel address(Address address) {
        this.address = address;
        return this;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
