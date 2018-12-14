# Rebar

This is an experimental, light-weight library to connect Java POJOs 
to FoundationDB. It does so by providing an extensible layer that
sits above the raw Tuple store exposed by Foundation DB's official
Java client driver.

## Goals
The project goal is to remove the developer attention from persistence,
and rather let he or she focus on business logic. This requires
a nearly complete abstraction between the business logic and 
persistence layers, as well as exposing safe, useful hooks to
work with the abstraction.

Longer term, the layer can allow for offline optimization of requests,
including query planning, automatic index creation, etc.

The syntax to interact with the persistence layer must be simple and
flexible. The developer has a good idea of the access patterns to
the data and can advise the creation of indexes.

## Installation
To install the library:

1. Run `bindings-install.sh` to install the fdb-java client library 
locally. This step can be removed if/when the library is avaiable
in maven central.
2. Run `mvn install` to generate the `org.eatbacon.rebar` artifact
3. Add the dependency in your local project:

```
<dependency>
    <groupId>org.eatbacon</groupId>
    <artifactId>rebar</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Technique
Architecturally, there are a number of layers in the library:

* The DAO implementation layer. This is the software layer that most consumers will
interact with. Namely, the DAO layer is manually created and leverages
an abstract base class. Each DAO layer is associated with exactly one
java object, and exposes methods to create, find, update, delete
records. The DAO tier extends from an abstract base class, where the 
majority of work is done. For example, Java POJOs are converted to 
primitive maps in this tier. CRUD methods are exposed that are 
used by the DAO tier. This tier interfaces directly with the 
TupleManager, which does the actual interaction with the database.

* The Tuple Manager. This code is largely copied from the Foundation DB
documentation site, and interacts directly with the database.

* The index definition space. Indexes are defined in a metadata
space called `indexDefinitionSpace`.

* The index space. When a DAO has indexes defined, they will live
in a space called `indexSpace`

## Usage
To use the library, you first need to create a DAO object for 
a particular POJO:

```
public class MyModelDao extends AbstractDao<MyModel> {
    // .../
}
```

Next, implement some abstract methods.

1. The first method tells the class what model class the DAO is 
responsible for mapping to the database. This will ge used when
converting from the Foundation DB tuple store into a POJO.
```
@Override
protected Class<?> getObjectClass() {
    return MyModel.class
}
```

2. This method is called to return the unique identifier for the
object. For example, if your model has a `getId()` method, you would
do the following:
```
@Override
protected String getUniqueId(MyModel obj) {
    return obj.getId();
}
```

3. Finally, add the accessor methods. There are some helper classes
to make this easy--for example, say you want to find an object
by the field `name`. This is done by using the underlying `find` 
method and specifying a query, which is generated by the `Filters`
static method `eq`:

```
public List<MyModel> findByName(String name) {
    return super.find(Filters.eq("name", name));
}
```

The above will look at the `name` field from the POJO and match
it with whatever is passed in the `name` variable. For example, an
object represented (in JSON) like this:

```
{
    "id": "1234",
    "name": "Bob",
    "address": {
        "street": "12345 El Monte Blvd",
        "city": "Los Altos Hills",
        "state": "CA",
        "zip": "94022"
    }
}
```

Could be found with a query like this:

```
find(Filters.eq("name", "Bob");
```

Since multiple values could match the query, a `List` is returned.

Deep queries are supported, too. For the above JSON sample, this
would query against the `state` field for values matching `"CA"`:

```
find(Filters.eq("address", "state", "CA"));
```

### Indexes
Indexes are easy to set up and can be used to quickly find values.
The process for creating indexes is to override the `getIndexes` method:

```
@Override
protected List<Index> getIndexes() {
    // we are indexing on `name` and deep-indexing on `address.zip`
    return Arrays.asList(
        new Index("name"),
        new Index("address", "zip")
    );
}
```

This will create an index for `(name)` and `(address),(zip)` such 
that each record being saved will automatically be indexed.

## Open items
This is a quick experiement and has more unfinished items than 
finished. A quick browse of the source code would show the word
`TODO` many times, but here is a brief list.

* Connectivity information is set to default--the library assumes
access to a locally-running Foundation DB without authentication
* There is only an equality query, and it is only tested against
strings. Of course, we want multiple criteria, range checks, etc,
and full datatype support. This should not be difficult work, but
it's certainly not complete.
* Fetching values should stream results. When working with large
result sets, loading all values into memory is not great at all.
This too is just work, and also has not been started.
* References. Pojos may be complex and deeply nested, however the
entire object is stored in a single tuple set. We don't always
want persistence to follow the object model directly--in many cases
storing references between objects and keeping a shallow tuple-set
may be ideal. 
* Optimizations. Too many to list, some have been documented in 
souce with TODOs.

## Future Work
Here are some thoughts on future work with this library.

* We can support some very interesting queries. For example,
finding all users who match a complex object. We don't need to
compare field-by-field--as a developer, it would be very 
convenient to match complex objects.

* Wildcard indexes. Queries may include a wildcard like this:

```
find(Filters.eq("employer", Query.WILDCARD, "job", "developer"));
```

this would allow matching all objects that have `job: developer`
regardless of the value where the wildcard is placed (consider that
a placeholder for `employer`). We can still use the index but only
partially. This can have much better performance than a table scan
would.

* (More to come)