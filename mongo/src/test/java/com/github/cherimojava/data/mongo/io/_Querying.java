/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo.io;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.cherimojava.data.mongo.CommonInterfaces;
import com.github.cherimojava.data.mongo.MongoBase;
import com.github.cherimojava.data.mongo.entity.Entity;
import com.github.cherimojava.data.mongo.entity.annotation.Id;
import com.github.cherimojava.data.mongo.entity.annotation.Reference;
import com.github.cherimojava.data.mongo.query.OngoingQuery;
import com.github.cherimojava.data.mongo.query.QuerySort;
import com.github.cherimojava.data.mongo.query.QueryStart;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCursor;

public class _Querying
    extends MongoBase
{

    private List<CommonInterfaces.PrimitiveEntity> entityList;

    @Before
    public void prepareData()
    {
        entityList = Lists.newArrayList();
        entityList.add( createSaveEntity( 1, "one" ) );
        entityList.add( createSaveEntity( 2, "two" ) );
        entityList.add( createSaveEntity( 3, "three" ) );
    }

    private CommonInterfaces.PrimitiveEntity createEntity( int i, String s )
    {
        return (CommonInterfaces.PrimitiveEntity) factory.create( CommonInterfaces.PrimitiveEntity.class )
            .setString( s ).setInteger( i );
    }

    private CommonInterfaces.PrimitiveEntity createSaveEntity( int i, String s )
    {
        CommonInterfaces.PrimitiveEntity pe = createEntity( i, s );
        pe.save();
        return pe;
    }

    @Test
    public void eqInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query(CommonInterfaces.PrimitiveEntity.class);
        MongoCursor<CommonInterfaces.PrimitiveEntity> result = query.where( query.e().getInteger() ).is( 1 ).iterator();
        assertTrue( result.hasNext() );
        assertEquals( result.next(), entityList.get( 0 ) );
        assertFalse(result.hasNext());
    }

    @Test
    public void eqString()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getString() ).is("two").iterator();
        assertThat(Lists.newArrayList(result), containsInAnyOrder(entityList.get(1)));
    }

    @Test
    public void betweenInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).between(1, 2).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 0 ), entityList.get( 1 ) ) );
    }

    @Test
    public void lessThanInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).lessThan(2).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 0 ) ) );
    }

    @Test
    public void lessThanEqualInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).lessThanEqual(2).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 0 ), entityList.get( 1 ) ) );
    }

    @Test
    public void greaterThanInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).greaterThan(2).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 2 ) ) );
    }

    @Test
    public void greaterThanEqualInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).greaterThanEqual(2).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 1 ), entityList.get( 2 ) ) );
    }

    @Test
    public void inInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getInteger() ).in(1, 3).iterator();
        assertThat( Lists.newArrayList( result ), containsInAnyOrder( entityList.get( 0 ), entityList.get( 2 ) ) );
    }

    @Test
    public void inString()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> result =
            query.where( query.e().getString() ).in( "one", "two" ).iterator();
        assertThat(Lists.newArrayList(result), containsInAnyOrder(entityList.get(0), entityList.get(1)));
    }

    @Test
    public void countString()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query(CommonInterfaces.PrimitiveEntity.class);
        assertEquals(2, query.where(query.e().getString()).in("one", "two").count());
    }

    @Test
    public void countInt()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query(CommonInterfaces.PrimitiveEntity.class);
        assertEquals(2, query.where(query.e().getInteger()).lessThan(3).count());
    }

    @Test
    public void limitString()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> cursor =
            query.where( query.e().getInteger() ).lessThan( 3 ).limit( 1 ).iterator();
        assertThat( cursor.next(), isIn( Lists.newArrayList( entityList.get( 0 ), entityList.get( 1 ) ) ) );
        assertFalse(cursor.hasNext());
    }

    @Test
    public void queryEndMeansEnd()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        try
        {
            ( (OngoingQuery<CommonInterfaces.PrimitiveEntity>) query.where( query.e().getInteger() ).lessThan( 3 )
                .limit( 1 ) ).and( query.e().getInteger() ).is( 2 ).iterator();
            fail( "should throw an exception" );
        }
        catch ( ClassCastException e )
        {
            assertThat( e.getMessage(), containsString( "OngoingQuery" ) );
        }
    }

    @Test
    public void sorting()
    {
        fillSortingList();
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> cursor = query.where( query.e().getString() ).in( "a", "b" )
            .sort().asc( query.e().getInteger() ).desc( query.e().getString() ).iterator();
        assertThat( Lists.newArrayList( cursor ),
            equalTo( Lists.newArrayList( entityList.get( 1 ), entityList.get( 2 ), entityList.get( 0 ) ) ) );
    }

    @Test
    public void sortingSkip()
    {
        fillSortingList();
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> cursor = query.where( query.e().getString() ).in("a", "b")
            .skip(1).sort().desc(query.e().getString()).asc(query.e().getInteger()).iterator();
        assertThat( Lists.newArrayList( cursor ),
            equalTo( Lists.newArrayList( entityList.get( 2 ), entityList.get( 0 ) ) ) );
    }

    @Test
    public void sortBy()
    {
        fillSortingList();
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query( CommonInterfaces.PrimitiveEntity.class );
        MongoCursor<CommonInterfaces.PrimitiveEntity> cursor =
            query.where( query.e().getString() ).in( "a", "b" ).sort().by( QuerySort.Sort.ASC, query.e().getInteger() )
                .by(QuerySort.Sort.DESC, query.e().getString()).iterator();
        assertThat( Lists.newArrayList( cursor ),
            equalTo( Lists.newArrayList( entityList.get( 1 ), entityList.get( 2 ), entityList.get( 0 ) ) ) );
    }

    @Test
    public void noRepeatedSorting()
    {
        QueryStart<CommonInterfaces.PrimitiveEntity> query = factory.query(CommonInterfaces.PrimitiveEntity.class);
        try
        {
            query.where( query.e().getString() ).in( "a", "b" ).skip( 1 ).sort().desc( query.e().getString() )
                .asc( query.e().getInteger() ).limit( 3 ).sort().iterator();
            fail( "should throw an exception" );
        }
        catch ( IllegalStateException e )
        {
            assertThat( e.getMessage(), containsString( "Sorting can be specified only once" ) );
        }
    }

    @Test
    public void nestedSearchForId()
    {
        Inner inner = factory.create( Inner.class ).setName("something");
        inner.save();
        Outer one = factory.create( Outer.class ).setInner( inner );
        Outer two = factory.create( Outer.class );
        two.save();
        one.save();
        QueryStart<Outer> query = factory.query( Outer.class );
        MongoCursor<Outer> res = query.where( query.e().getInner().getName() ).is( "something" ).iterator();
        assertTrue( res.hasNext() );
        assertEquals( res.next(), one );
        assertFalse( res.hasNext() );
    }

    @Test
    public void nestedSearchForDBRefId() {
        Inner inner = factory.create( Inner.class ).setName("something");
        inner.save();
        Outer one = factory.create( Outer.class ).setDBRefInner(inner);
        Outer two = factory.create( Outer.class );
        two.save();
        one.save();
        QueryStart<Outer> query = factory.query(Outer.class);
        try {
            MongoCursor<Outer> res = query.where(query.e().getDBRefInner().getName()).is("something").iterator();
            fail("should throw an exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),containsString("nested search is currently not working on DBRef-References."));
        }
    }

    @Test
    public void nestedSearchExceptionOnNonId()
    {
        QueryStart<Outer> query = factory.query(Outer.class);
        try
        {
            query.where( query.e().getInner().getOther() ).is( "something" ).iterator();
            fail( "should throw an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertThat( e.getMessage(), containsString( "Only can perform nested query on id field" ) );
        }
    }

    @Test
    public void enumSearching()
    {
        EnumEntity ee = factory.create( EnumEntity.class );
        ee.setEnum( TestEnum.A );
        ee.save();
        factory.create( EnumEntity.class ).setEnum( TestEnum.C ).save();

        QueryStart<EnumEntity> q = factory.query( EnumEntity.class );
        assertEquals( 1, q.where( q.e().getEnum() ).is( TestEnum.A ).count() );

        q = factory.query( EnumEntity.class );
        assertEquals( 2, q.where( q.e().getEnum() ).in( TestEnum.A, TestEnum.C ).count() );
    }

    @Test
    public void primitiveEntitiesWorking() {
        QueryStart<Inner> i = factory.query(Inner.class);
        i.where(i.e().getInt());
    }

    private void fillSortingList()
    {
        factory.getCollection( CommonInterfaces.PrimitiveEntity.class ).drop();
        entityList = Lists.newArrayList();
        entityList.add( createSaveEntity( 3, "a" ) );
        entityList.add( createSaveEntity( 1, "b" ) );
        entityList.add( createSaveEntity( 1, "a" ) );
    }

    private static interface Outer
        extends Entity<Outer>
    {
        @Reference
        public Inner getInner();

        public Outer setInner( Inner inner );

        @Reference(asDBRef = true)
        public Inner getDBRefInner();

        public Outer setDBRefInner( Inner inner );

    }

    private static interface Inner
        extends Entity<Inner>
    {
        @Id
        public String getName();

        public Inner setName( String name );

        public String getOther();

        public Inner setOther( String other );

        public int getInt();

        public Inner setInt(int i);
    }

    private static interface EnumEntity
        extends Entity<EnumEntity>
    {
        public TestEnum getEnum();

        public EnumEntity setEnum( TestEnum e );
    }

    private static enum TestEnum
    {
        A, B, C
    }
}
