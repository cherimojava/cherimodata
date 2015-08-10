/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.entity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.bson.Document;
import org.bson.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cherimojava.data.mongo.entity.annotation.Collection;
import com.github.cherimojava.data.mongo.entity.annotation.Index;
import com.github.cherimojava.data.mongo.entity.annotation.IndexField;
import com.github.cherimojava.data.mongo.io.EntityCodec;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.operation.OrderBy;

/**
 * Utility class for working with Entity based Proxies.
 *
 * @author philnate
 * @since 1.0.0
 */
public class EntityFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( EntityFactory.class );

    /**
     * Where all entity for this factory will be stored. Each entity goes into it's own collection, but within the same
     * DB
     */
    private final MongoDatabase db;

    /**
     * holds to a given Entity class the corresponding MongoCollection backing it
     */
    private LoadingCache<Class<? extends Entity>, MongoCollection<? extends Entity>> preparedEntites =
        CacheBuilder.newBuilder().build( new CacheLoader<Class<? extends Entity>, MongoCollection<? extends Entity>>()
        {
            @Override
            public MongoCollection<? extends Entity> load( Class<? extends Entity> clazz )
                throws Exception
            {
                LOG.debug( "First usage of Entity class {}, checking MongoDB setup", clazz );
                return prepareEntity( defFactory.create( clazz ) );
            }

            /**
             * Prepares the data structure for this Entity class in the given database, this means creating declared
             * indexes etc.
             */
            private MongoCollection<? extends Entity> prepareEntity( final EntityProperties properties )
            {
                // TODO need to add verification that index field matches existing property
                Class<? extends Entity> clazz = properties.getEntityClass();
                Collection c = clazz.getAnnotation( Collection.class );
                MongoCollection<? extends Entity> coll = EntityCodec.getCollectionFor( db, properties );
                if ( c != null && c.indexes() != null )
                {
                    LOG.debug( "Entity class {} has indexes, ensuring that MongoDB is setup",
                        properties.getEntityClass() );
                    for ( Index index : c.indexes() )
                    {
                        IndexOptions options = new IndexOptions();
                        if ( index.unique() )
                        {
                            options.unique( true );
                        }
                        if ( isNotEmpty( index.name() ) )
                        {
                            options.name( index.name() );
                        }

                        Document indxFields = new Document();
                        for ( IndexField field : index.value() )
                        {
                            checkNotNull( properties.getProperty( field.field() ),
                                "Index field '%s' for index '%s' does not exist for %s", field.field(), index.name(),
                                clazz );
                            indxFields.put( field.field(),
                                ( field.order() == IndexField.Ordering.ASC ) ? OrderBy.ASC.getIntRepresentation()
                                                : OrderBy.DESC.getIntRepresentation() );
                        }
                        LOG.debug( "Creating index {} for Entity class {}", options.getName(),
                            properties.getEntityClass() );
                        coll.createIndex( indxFields, options );
                    }
                }
                return coll;
            }
        } );

    /**
     * get the mongo collection belonging to the given entity class
     *
     * @param clazz
     * @return
     */
    public <T extends Entity> MongoCollection<T> getCollection( Class<T> clazz )
    {
        try
        {
            return (MongoCollection<T>) preparedEntites.get( clazz );
        }
        catch ( UncheckedExecutionException | ExecutionException e )
        {
            throw Throwables.propagate( e.getCause() );
        }
    }

    /**
     * contains information about Default Implementations used when property defines a interface
     */
    private static Map<Class, Class> interfaceImpls;

    private static final EntityPropertiesFactory defFactory = new EntityPropertiesFactory();

    static
    {
        interfaceImpls = Maps.newConcurrentMap();
        interfaceImpls.put( List.class, ArrayList.class );
        interfaceImpls.put( Set.class, HashSet.class );
        interfaceImpls.put( Queue.class, LinkedBlockingQueue.class );
    }

    /**
     * creates a new EntityFactory, with the given Database for storage. Instances created through create will be linked
     * to a collection within the given Database.
     *
     * @param db MongoDatabase into which Entities will be saved if created throught create method
     */
    public EntityFactory( MongoDatabase db )
    {
        this.db = db;
    }

    /**
     * Creates a new Instance of a given Entity class, which holds a reference to the collection where it will be stored
     * to. This means an invocation of Entity.save() will store the entity in the given Collection of the MongoDatabase
     * supplied on creation time of EntityFactory
     *
     * @param clazz Entity class to create a new Instance from
     * @param <T> Entity type
     * @return new Entity instance of the given class, capable of using methods accessing MongoDB (like Entity.save(),
     *         Entity.drop())
     */
    public <T extends Entity> T create( Class<T> clazz )
    {
        EntityInvocationHandler handler =
            new EntityInvocationHandler( defFactory.create( clazz ), getCollection( clazz ) );
        return instantiate( clazz, handler );
    }

    public <T extends Entity> T createLazy( Class<T> clazz, Object id )
    {
        EntityInvocationHandler handler =
            new EntityInvocationHandler( defFactory.create( clazz ), getCollection( clazz ), id );
        T t = instantiate( clazz, handler );
        return t;
    }

    /**
     * allows to load an Entity which is identified by the given id, or null if no such entity was found.
     *
     * @param id of the document to load
     * @return Entity matching this id or null if no such entity was found
     */
    @SuppressWarnings( "unchecked" )
    public <T extends Entity> T load( Class<T> clazz, Object id )
    {
        return EntityInvocationHandler.find( (MongoCollection<T>) getCollection( clazz ), id );
    }

    /**
     * Creates a new Instance of the given Entity based class, this Entity itself has no knowledge of MongoDB, so it
     * can't be stored/dropped through it's own methods (e.g. Entity.save()). As the Entity is created static there's no
     * DB information, thus Indexes, etc. can't be created if needed
     *
     * @param clazz entity class to instantiate from
     * @return new instance of this class
     */
    public static <T extends Entity> T instantiate( Class<T> clazz )
    {
        return instantiate( clazz, new EntityInvocationHandler( defFactory.create( clazz ) ) );
    }

    /**
     * Returns the EntityProperties representing the given Entity. EntityProperty information generated here is
     * statically generated and shared across all EntityFactory instances
     *
     * @param clazz entity class of which the EntityProperties shall be looked up
     * @return EntityProperties belonging to the given class
     */
    public static EntityProperties getProperties( Class<? extends Entity> clazz )
    {
        return defFactory.create( clazz );
    }

    /**
     * Creates a new instance of the given Entity based Class, allowing to provide a custom EntityInvocationHandler
     *
     * @param clazz entity class to instantiate from
     * @param handler which is used to provide the functionality of this proxy
     * @return new instance of this class
     */
    @SuppressWarnings( "unchecked" )
    static <T extends Entity> T instantiate( Class<T> clazz, EntityInvocationHandler handler )
    {
        T proxy = (T) Proxy.newProxyInstance( EntityInvocationHandler.class.getClassLoader(), new Class<?>[] { clazz },
            handler );
        handler.setProxy( proxy );
        return proxy;
    }

    /**
     * gets the MongoDatabase which is used by this EntityFactory, meaning entities created through create will be
     * stored into
     *
     * @return MongoDatabase of this instance
     */
    public MongoDatabase getDb()
    {
        return db;
    }

    /**
     * creates an Entity from the given JSON String which is of the given Entity class
     *
     * @param clazz entity class the string reflects an instance from
     * @param json String representation of an instance of the given Entity class
     * @param <T> Entity class
     * @return Entity instance representing the given String, with MongoDB reference. Thus being able to perform e.g.
     *         Entity.save()
     */
    public <T extends Entity> T readEntity( Class<T> clazz, String json )
    {
        return new EntityCodec<T>( db, defFactory.create( clazz ) ).decode( new JsonReader( json ), null );
    }

    /**
     * creates a list of Entities from the given JSON String, which is of the given Entity class
     * 
     * @param clazz entity class of the lists string representation
     * @param json String representation of the entity list
     * @param <T> Entity class
     * @return List of entities represented by the given String, with MongoDB reference. This means each entity is able
     *         to be saved, e.g. Entity.save()
     */
    public <T extends Entity> List<T> readList( Class<T> clazz, String json )
    {
        return (List<T>) new EntityCodec<T>( db, defFactory.create( clazz ) ).getCodec( List.class )
            .decode( new JsonReader( json ), null );
    }

    /**
     * Sets a default class for a given Interface, which will be used if a Add method is invoked but no underlying
     * object is existing yet. Supplied class must provide a parameterless public constructor and must be not abstract
     *
     * @param interfaze on for which the given implementation will be invoked
     * @param implementation concrete implementation of the given interface of which a new instance might be created
     *            must have a empty constructor and can't be abstract
     * @param <T>
     */
    public static <T> void setDefaultClass( Class<T> interfaze, Class<? extends T> implementation )
    {
        checkArgument( interfaze.isInterface(), "Can set default Classes only for interfaces" );
        checkArgument( !implementation.isInterface(), "Default class can't be an interface itself" );
        checkArgument( !Modifier.isAbstract( implementation.getModifiers() ), "Implementation can't be abstract" );
        try
        {
            implementation.getConstructor();
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalArgumentException(
                "Supplied implementation does not provide a parameterless constructor." );
        }
        interfaceImpls.put( interfaze, implementation );
    }

    public static Class getDefaultClass( Class interfaze )
    {
        return interfaceImpls.get( interfaze );
    }

    /**
     * saves the entity into the collection for this factory. This allows to save an entity into a different database
     * than it would be otherwise stored
     * 
     * @param e
     */
    public void save( Entity e )
    {
        EntityInvocationHandler.save( EntityInvocationHandler.getHandler( e ), getCollection( e.entityClass() ) );
    }
}
