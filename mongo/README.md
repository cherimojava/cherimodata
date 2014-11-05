Cherimodata mongo [![Build Status](https://travis-ci.org/cherimojava/cherimodata.png?branch=master)](https://travis-ci.org/cherimojava/cherimodata)
=================
Cherimodata mongo is a work in Progress Object Document Mapper for MongoDB and Java. It allows you to define POJOs which can be easily stored within mongodb. At the moment there's no maven artefact, but this will be added at a later time, until then you need to checkout, import and build the artifact yourself.

## POJO declaration
To get started you need to define your POJO as an Interface extending *com.github.cherimojava.data.mongo.Entity* (I'll explain the difference later)

```java
    static interface SimpleEntity extends Entity {

        public SimpleEntity setMyId(String myId);

        @Id
        public String getMyId();
    }
```

The above will give you some Entity with a property called MyId, which can be accessed through Java just like a normal POJO property.

## MongoDB representation
In MongoDB the previously shown POJO will be persisted within a collection called _simpleEntity_ like this:

    {_id:"value"}

to instantiate a new Entity you need to create an Instance of Entities, supplying a DB where all the entities will be stored to. Once this is done you can create new Entities, calling entities.create(SimpleEntity.class). You can as well create Entities statically through an EntityFactory, but these entities lack the capability to be self sufficient, meaning that load, delete, save, etc. won't work for them.
After this you can access all the common Entity properties as well as the MyEntity specific properties.

## Method/Property Naming convention
Per property you need a setter and getter method, fulfilling:

* Set method named set&lt;PROPERTY&gt;, with exactly one param
* Get method named get&lt;PROPERTY&gt;, with exactly no param, return value type must match Setter param
* You can't have methods named _h or _v, as these are reserved names

## Possible Annotations
Annotations need to be placed on Getter methods, and not on setter to keep it consistent with JSR-301 validation

### Class Level
* @Named, allows to use a different collection name than the one the framework would choose
* @Collection currently only serves as container for the Index declaration
* @Index, used within @Collection defines an index name (optional), if it shall be unique and the order and
fields to be included in the index
* @IndexField, fieldname and ordering of the field within the index

### Property Level
* @Id, tells that this property shall be treated as the Id field (will map to mongodb _\_id_). If not declared
implicit id will be used. Can't be used more than once per Entity
* @Named, set the MongoDB name to be different than the java name, might use \_id, converting the field into \_id
(can't be done more than once, not combined with @Id).
* All Java Validation annotations like @Size, @NotNull, @DecimalMin, @DecimalMax, @Future, etc.
* @Transient, tells that this field isn't persisted
* @Computed tells that this property is being computed. Once must not declare a setter for those fields
* @Reference only on Entity type properties. Tells that the Entity supplied with this property will not be inlined. By default other Entities will be inlined
* @Final denotes that this property after first written can't be changed ongoing. Only working for primitives and their Object representives and org.bson.types.ObjectId

### Property MongoDB name mapping
The Java to MongoDB name resolution follows these rules:

* CamelCase is preserved except the first letter (if capital) will be turned to lowerCase
* If all Letter are capital, nothing will be transformed
* If there's only one letter it will be converted to lower case (no matter if it's upper case or not)

## TODO

* On the fly loading for subdocuments.
* Transactional Support as described in MongoDBs 2PC document
* Search functionality based on method names
