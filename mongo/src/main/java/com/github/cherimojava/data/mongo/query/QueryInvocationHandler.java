/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.query;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.bson.conversions.Bson;

import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.EntityFactory;
import com.github.cherimojava.data.mongo.entity.EntityProperties;
import com.github.cherimojava.data.mongo.entity.ParameterProperty;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Outer invocation Handler joining all parts of the query building together like:
 * 
 * <pre>
 * Query q = QueryBuilder.query(Entity.class);
 * q.where(q.e().getProperty()).is(something).and(q.e().getProperty2()).between(val1,val2).orderBy(desc(getDesc()),asc(getAsc()).limit(x).skip(y).getAll()
 * </pre>
 */
public class QueryInvocationHandler
    implements InvocationHandler
{
    private final MongoCollection<? extends Entity> coll;

    Class<? extends Entity> clazz;

    Supplier<Entity> entityProxy;

    Supplier<QuerySpecifier> specifier;

    Supplier<QueryEnd> queryEnd;

    Supplier<QuerySort> querySort;

    OngoingQuery proxy;

    EntityProperties properties;

    List<ParameterProperty> curQueriedProperty = Lists.newArrayList();

    List<Bson> filters = Lists.newArrayList();

    Integer limit = null;

    Integer skip = null;

    List<Bson> sorts = Lists.newArrayList();

    List<String> curSorts = Lists.newArrayList();

    private boolean sortSet = false;

    public ParameterProperty getProperty( Method m )
    {
        return properties.getProperty( m );
    }

    public void addCurQueriedProperty( ParameterProperty curQueriedProperty )
    {
        this.curQueriedProperty.add( curQueriedProperty );
    }

    /**
     * Creates a new Invocation handler for the given Entity class and collection, along with the related
     * EntityProperties
     * 
     * @param clazz Entity class the query is based for
     * @param coll Collection against which the query will be performed
     * @param properties Entities properties of the entity class
     * @param <E>
     */
    public <E extends Entity> QueryInvocationHandler( Class<E> clazz, MongoCollection<E> coll,
                                                      EntityProperties properties )
    {
        this.coll = coll;
        this.properties = properties;
        this.clazz = clazz;
        entityProxy = Suppliers.memoize( () -> (Entity) Proxy.newProxyInstance( getClass().getClassLoader(),
            new Class[] { clazz }, new EntityQueryInvocationHandler( this ) ) );
        specifier = Suppliers.memoize( () -> (QuerySpecifier) Proxy.newProxyInstance( getClass().getClassLoader(),
            new Class[] { QuerySpecifier.class }, new QuerySpecifierInvocationHandler( this ) ) );
        queryEnd = Suppliers.memoize( () -> (QueryEnd) Proxy.newProxyInstance( this.getClass().getClassLoader(),
            new Class[] { QueryEnd.class }, this ) );
        querySort = Suppliers.memoize( () -> (QuerySort) Proxy.newProxyInstance( this.getClass().getClassLoader(),
            new Class[] { QuerySort.class }, this ) );
    }

    /**
     * sets the proxy this QueryInvocationHandler represents
     * 
     * @param proxy this QueryInvocationHandler is representing
     */
    public void setProxy( OngoingQuery proxy )
    {
        this.proxy = proxy;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        String methodName = method.getName();
        switch ( methodName )
        {
            case "e":
                // this is just a handout for the entity to get knowledge of what to check
                return this.entityProxy.get();
            case "where":/* fallthrough */
            case "and":
                return this.specifier.get();
            case "iterator":
                FindIterable it = coll.find( Filters.and( filters.toArray( new Bson[] {} ) ) );
                if ( limit != null )
                {
                    it.limit( limit );
                }
                if ( skip != null )
                {
                    it.skip( skip );
                }
                if ( sorts.size() > 0 )
                {
                    it.sort( Sorts.orderBy( sorts ) );
                }
                return it.iterator();
            case "count":
                return coll.count( Filters.and( filters.toArray( new Bson[] {} ) ) );
            case "limit":
                limit = (Integer) args[0];
                return queryEnd.get();
            case "skip":
                skip = (Integer) args[0];
                return queryEnd.get();
            case "sort":
                checkState( !sortSet, "Sorting can be specified only once" );
                sortSet = true;
                return querySort.get();
            case "desc":/* fallthrough */
            case "asc":
                curQueriedProperty.forEach( parameterProperty -> curSorts.add( parameterProperty.getMongoName() ) );
                if ( "desc".equals( methodName ) )
                {
                    sorts.add( Sorts.descending( curSorts ) );
                }
                else
                {
                    sorts.add( Sorts.ascending( curSorts ) );
                }
                curSorts.clear();
                curQueriedProperty.clear();
                return proxy;
        }
        throw new IllegalStateException( "Unknown method found: " + methodName );
    }

    /**
     * retrieves the mongodb name currently queried against and clears it to avoid illegal states
     * 
     * @return mongodb name queried against
     */
    private String getCurrentMongoName()
    {
        checkState( curQueriedProperty.size() == 1, "can't get mongo name from null property" );
        String name = curQueriedProperty.get( 0 ).getMongoName();
        // after this was set change back to null, as this invocation is exhausted
        curQueriedProperty.clear();
        return name;
    }

    /**
     * InvocationHandler capturing which property is about to be queried
     */
    private class EntityQueryInvocationHandler
        implements InvocationHandler
    {

        private final QueryInvocationHandler parent;

        EntityQueryInvocationHandler( QueryInvocationHandler parent )
        {
            this.parent = parent;
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            if ( Entity.class.isAssignableFrom( method.getReturnType() ) )
            {
                checkArgument( !getProperty( method ).isDBRef(),
                    "nested search is currently not working on DBRef-References." );
                // the remembering is done nested, first remember the parent element
                parent.curQueriedProperty.add( getProperty( method ) );
                return Proxy.newProxyInstance( this.getClass().getClassLoader(), new Class[] { method.getReturnType() },
                    new InnerEntityQueryIdOnlyInvocationHandler(
                        EntityFactory.getProperties( (Class<? extends Entity>) method.getReturnType() ) ) );
            }
            else
            {
                // remember which property was invoked so we can build the query condition based on it
                parent.addCurQueriedProperty( parent.getProperty( method ) );
                return null;
            }
        }

        /**
         * InvocationHandler ensuring that only on Id fields a query can be performed
         */
        private class InnerEntityQueryIdOnlyInvocationHandler
            implements InvocationHandler
        {

            private final EntityProperties properties;

            public InnerEntityQueryIdOnlyInvocationHandler( EntityProperties properties )
            {
                this.properties = properties;
            }

            @Override
            public Object invoke( Object proxy, Method method, Object[] args )
                throws Throwable
            {
                ParameterProperty curProperty = properties.getProperty( method );
                if ( properties.getIdProperty() == curProperty )
                {
                    // We actually don't need to do anything special as we're not explicitly saving the id field
                    return null;
                }
                else
                {
                    throw new IllegalArgumentException(
                        "Only can perform nested query on id field, but was " + curProperty );
                }
            }
        }
    }


    /**
     * InvocationHandler creating the query condition
     */
    private class QuerySpecifierInvocationHandler
        implements InvocationHandler
    {

        private final QueryInvocationHandler parent;

        QuerySpecifierInvocationHandler( QueryInvocationHandler parent )
        {
            this.parent = parent;
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            // depending on the method add given filters to the query
            String property = getCurrentMongoName();
            switch ( method.getName() )
            {
                case "is":
                    filters.add( Filters.eq( property, args[0] ) );
                    break;
                case "between":
                    filters.add( Filters.gte( property, args[0] ) );
                    filters.add( Filters.lte( property, args[1] ) );
                    break;
                case "lessThan":
                    filters.add( Filters.lt( property, args[0] ) );
                    break;
                case "lessThanEqual":
                    filters.add( Filters.lte( property, args[0] ) );
                    break;
                case "greaterThan":
                    filters.add( Filters.gt( property, args[0] ) );
                    break;
                case "greaterThanEqual":
                    filters.add( Filters.gte( property, args[0] ) );
                    break;
                case "in":
                    filters.add( Filters.in( property, (Object[]) args[0] ) );
                    break;
            }
            return parent.proxy;
        }
    }
}
