package org.eatbacon.example.controllers;

import io.swagger.inflector.models.ApiError;
import io.swagger.inflector.models.RequestContext;
import io.swagger.inflector.models.ResponseContext;

import javax.ws.rs.core.Response.Status;

import io.swagger.inflector.utils.ApiException;
import org.eatbacon.example.dao.PersonDao;
import org.eatbacon.example.models.Person;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public class PeopleController {
    public ResponseContext findById(RequestContext request, String id) {
        Person person = PersonDao.getInstance().findById(id);
        if(person == null) {
            throw new ApiException(new ApiError()
                    .code(404)
                    .message(String.format("user %s not found", id)));
        }
        return new ResponseContext().entity(person);
    }

    public ResponseContext addPerson(RequestContext request, Person person) {
        try {
            PersonDao.getInstance().insert(person);
            return new ResponseContext().status(201).entity(person);
        }
        catch (Exception e) {
            return new ResponseContext().status(Status.CONFLICT).entity(new ApiError()
                    .code(409)
                    .message("unable to add person"));
        }
    }
}

